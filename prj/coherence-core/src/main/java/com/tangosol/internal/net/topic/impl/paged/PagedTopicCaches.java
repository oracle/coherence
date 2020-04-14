/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.Position;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.internal.util.Primes;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.Serializer;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.HashHelper;

import java.io.Closeable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.ThreadLocalRandom;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * This class encapsulates operations on the set of {@link NamedCache}s
 * that are used to hold the underlying data for a topic.
 *
 * @author jk 2015.06.19
 * @since Coherence 14.1.1
 */
public class PagedTopicCaches
    implements Closeable, ClassLoaderAware
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicCaches}.
     *
     * @param sName         the name of the topic
     * @param cacheService  the {@link CacheService} owning the underlying caches
     */
    public PagedTopicCaches(String sName, CacheService cacheService)
        {
        this(sName, cacheService, null);
        }

    /**
     * Create a {@link PagedTopicCaches}.
     *
     * @param sName          the name of the topic
     * @param cacheService   the {@link CacheService} owning the underlying caches
     * @param functionCache  the function to invoke to obtain each underlying cache
     */
    public PagedTopicCaches(String sName, CacheService cacheService,
                            BiFunction<String, ClassLoader, NamedCache> functionCache)
        {
        if (sName == null || sName.isEmpty())
            {
            throw new IllegalArgumentException("The name argument cannot be null or empty String");
            }

        if (cacheService == null)
            {
            throw new IllegalArgumentException("The cacheService argument cannot be null");
            }

        if (functionCache == null)
            {
            functionCache = cacheService::ensureCache;
            }

        f_sTopicName   = sName;
        f_cacheService = cacheService;

        Pages         = functionCache.apply(Names.PAGES.cacheNameForTopicName(sName), f_cacheService.getContextClassLoader());
        Data          = functionCache.apply(Names.CONTENT.cacheNameForTopicName(sName), f_cacheService.getContextClassLoader());
        Subscriptions = functionCache.apply(Names.SUBSCRIPTIONS.cacheNameForTopicName(sName), f_cacheService.getContextClassLoader());
        Notifications = functionCache.apply(Names.NOTIFICATIONS.cacheNameForTopicName(sName), f_cacheService.getContextClassLoader());
        Usages = functionCache.apply(Names.USAGE.cacheNameForTopicName(sName), f_cacheService.getContextClassLoader());

        Set<NamedCache> setCaches = f_setCaches = new HashSet<>();

        setCaches.add(Pages);
        setCaches.add(Data);
        setCaches.add(Subscriptions);
        setCaches.add(Notifications);
        setCaches.add(Usages);
        }

    // ----- TopicCaches methods --------------------------------------

    /**
     * Return the serializer.
     *
     * @return the serializer
     */
    public Serializer getSerializer()
        {
        return f_cacheService.getSerializer();
        }

    /**
     * Destory the PagedTopicCaches.
     */
    public void destroy()
        {
        close(/* destroy */ true);
        }

    /**
     * Return whether or not the caches are active,
     * specifically the page cache for the topic.
     *
     * @return true if the caches are active; false otherwise
     */
    public boolean isActive()
        {
        return Pages.isActive();
        }

    // ----- Closeable methods ----------------------------------------------

    @Override
    public void close()
        {
        close(/* destroy */ false);
        }

    // ----- ClassLoaderAware methods ---------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        return f_cacheService.getContextClassLoader();
        }

    @Override
    public void setContextClassLoader(ClassLoader classLoader)
        {
        throw new UnsupportedOperationException();
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Return the topic name.
     *
     * @return the topic name
     */
    public String getTopicName()
        {
        return f_sTopicName;
        }

    /**
     * Get the start page for this topic upon creation.
     *
     * @return the start page
     */
    public int getBasePage()
        {
        return Math.abs(f_sTopicName.hashCode() % getPartitionCount());
        }

    /**
     * Return the partition count for this topic.
     *
     * @return the partition count for this topic
     */
    public int getPartitionCount()
        {
        return ((PartitionedService) f_cacheService).getPartitionCount();
        }

    /**
     * Return the channel count for this topic.
     *
     * @return the channel count for this topic
     */
    public int getChannelCount()
        {
        return getChannelCount(getPartitionCount());
        }

    /**
     * Compute the channel count based on the supplied partition count.
     *
     * @param cPartitions the partition count
     *
     * @return the channel count based on the supplied partition count
     */
    public static int getChannelCount(int cPartitions)
        {
        return Math.min(cPartitions, Primes.next((int) Math.sqrt(cPartitions)));
        }

    /**
     * Generate a new non-zero NotifierId.
     *
     * @return the NotifierId
     */
    public int newNotifierId()
        {
        // avoid 0 as it is used to indicate that notification is disabled (on the publisher)
        return 1 + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        }

    /**
     * Return the set of NotificationKeys covering all partitions for the given notifier
     *
     * @param nNotifier  the notifier id
     *
     * @return the NotificationKeys
     */
    public Set<NotificationKey> getPartitionNotifierSet(int nNotifier)
        {
        Set<NotificationKey> setKey = new HashSet<>();
        for (int i = 0, c = getPartitionCount(); i < c; ++i)
            {
            setKey.add(new NotificationKey(i, nNotifier));
            }

        return setKey;
        }

    /**
     * Return the unit of order for a topic partition.
     *
     * @param nPartition  the partition
     *
     * @return the unit of order
     */
    public int getUnitOfOrder(int nPartition)
        {
        return f_sTopicName.hashCode() + nPartition;
        }

    /**
     * Return the associated CacheService.
     *
     * @return the cache service
     */
    public CacheService getCacheService()
        {
        return f_cacheService;
        }

    /**
     * Return the Configuration.
     *
     * @return the configuration
     */
    public Configuration getConfiguration()
        {
        return f_cacheService.getResourceRegistry()
            .getResource(Configuration.class, getTopicName());
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        PagedTopicCaches that = (PagedTopicCaches) o;

        return f_sTopicName.equals(that.f_sTopicName) && f_cacheService.equals(that.f_cacheService);
        }

    @Override
    public int hashCode()
        {
        return HashHelper.hash(f_sTopicName, 31);
        }

    @Override
    public String toString()
        {
        return "TopicCaches(name='" + f_sTopicName + ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Close the PagedTopicCaches.
     *
     * @param fDestroy  true to destroy, false to release
     */
    private void close(boolean fDestroy)
        {
        Consumer<NamedCache> function = fDestroy ? NamedCache::destroy : NamedCache::release;

        if (f_setCaches != null)
            {
            synchronized (this)
                {
                if (f_setCaches != null)
                    {
                    f_setCaches.stream().forEach(function);
                    f_setCaches = null;
                    }
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The topic name.
     */
    protected final String f_sTopicName;

    /**
     * The cache service.
     */
    protected final CacheService f_cacheService;

    /**
     * The caches which back the topic.
     */
    protected Set<NamedCache> f_setCaches;

    /**
     * The cache that holds the topic pages.
     */
    public final NamedCache<Page.Key, Page> Pages;

    /**
     * The cache that holds the topic elements.
     */
    public final NamedCache<Position, Object> Data;

    /**
     * The cache that holds the topic subscriber partitions.
     */
    public final NamedCache<Subscription.Key, Subscription> Subscriptions;

    /**
     * The cache that is used to notify blocked publishers and subscribers that they topic is no longer full/empty.
     */
    public final NamedCache<NotificationKey, int[]> Notifications;

    /**
     * The cache that holds the highest used topic pages for a cache partition.
     */
    public final NamedCache<Usage.Key, Usage> Usages;

    // ----- inner class: Names ---------------------------------------------

    /**
     * An pseudo enumeration representing the different caches used
     * by topic and topic implementations.
     *
     * @author jk 2015.06.08
     * @since Coherence 14.1.1
     */
    public static class Names<K,V>
        {
        // ----- constructors ---------------------------------------------------

        /**
         * Create a TopicCacheNames with the given cache name prefix.
         *
         * @param sName       the name of this TopicCacheNames.
         * @param sPrefix     the prefix to use to obtain the cache name from the topic name
         * @param classKey    the type of the cache keys
         * @param classValue  the type of the cache values
         */
        private Names(String sName, String sPrefix, Class<K> classKey, Class<V> classValue, Storage storage)
            {
            f_sName         = sName;
            f_sPrefix       = sPrefix;
            f_classKey      = classKey;
            f_classValue    = classValue;
            f_typeAssertion = withTypes(f_classKey, f_classValue);
            f_storage       = storage;

            s_setValues.add(this);
            }

        // ----- TopicCacheNames methods ----------------------------------------

        /**
         * Return the cache name from the specified topic name.
         *
         * @param sTopicName  the topic name
         *
         * @return the cache name
         */
        public String cacheNameForTopicName(String sTopicName)
            {
            return f_sPrefix + sTopicName;
            }

        /**
         * Return the {@link Names} that matches the specified cache name.
         *
         * @param sCacheName  the cache name;
         *
         * @return the {@link Names} that matches the specified cache name
         */
        public static Names fromCacheName(String sCacheName)
            {
            for (Names pagedTopicCacheNames : values())
                {
                if (sCacheName.startsWith(pagedTopicCacheNames.f_sPrefix))
                    {
                    return pagedTopicCacheNames;
                    }
                }

            throw new IllegalArgumentException("Cache name " + sCacheName + " is not a valid TopicCacheName");
            }

        /**
         * For a given cache name return the topic name.
         *
         * @param sCacheName  the cache name
         *
         * @return the topic name
         */
        public static String getTopicName(String sCacheName)
            {
            for (Names pagedTopicCacheNames : values())
                {
                if (sCacheName.startsWith(pagedTopicCacheNames.f_sPrefix))
                    {
                    return sCacheName.substring(pagedTopicCacheNames.f_sPrefix.length());
                    }
                }

            return sCacheName;
            }

        /**
         * Obtain the set of all possible {@link Names}.
         *
         * @return the set of all possible {@link Names}
         */
        public static Set<Names> values()
            {
            return Collections.unmodifiableSet(s_setValues);
            }

        // ----- accessor methods -----------------------------------------------

        /**
         * Obtain the {@link TypeAssertion} to use when obtaining a type safe
         * version of the cache this {@link Names} represents.
         *
         * @return the {@link TypeAssertion} to use when obtaining a type safe
         *         version of the cache this {@link Names} represents
         */
        public TypeAssertion<K,V> getTypeAssertion()
            {
            return f_typeAssertion;
            }

        /**
         * Obtain the prefix used to get the cache name from the
         * topic name.
         *
         * @return the prefix used to get the cache name from the
         *         topic name
         */
        public String getPrefix()
            {
            return f_sPrefix;
            }

        /**
         * Obtain the Class of the cache keys.
         *
         * @return the Class of the cache keys
         */
        public Class<K> getKeyClass()
            {
            return f_classKey;
            }

        /**
         * Obtain the Class of the cache values.
         *
         * @return the Class of the cache values
         */
        public Class<V> getValueClass()
            {
            return f_classValue;
            }

        /**
         * Obtain the storage location for the cache of this type.
         *
         * @return the storage location for the cache of this type
         */
        public Storage getStorage()
            {
            return f_storage;
            }

        /**
         * Return true if corresponding cache should be considered an internal submapping.
         *
         * @return true if corresponding sub cache mapping should be considered internal.
         */
        public boolean isInternal()
            {
            return Storage.MetaData.equals(getStorage());
            }

        // ----- object methods -------------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }

            Names<?, ?> names = (Names<?, ?>) o;

            return f_sName.equals(names.f_sName);

            }

        @Override
        public int hashCode()
            {
            return f_sName.hashCode();
            }

        @Override
        public String toString()
            {
            return f_sName;
            }


        // ----- constants ------------------------------------------------------

        /**
         * The {@link Set} of all legal {@link Names}.
         */
        private static final Set<Names> s_setValues = new HashSet<>();

        /**
         * The name prefix for all meta-data {@link Names} used to implement a {@link NamedTopic}.
         */
        public static final String METACACHE_PREFIX="$meta$topic";

        /**
         * The cache that holds the topic content.
         *
         * Use of no prefix rather than METACACHE_PREFIX since it is being used to filter out internal topic meta caches.
         */
        public static final Names<Position,Object> CONTENT =
            new Names<>("content", "$topic$", Position.class, Object.class, Names.Storage.Data);

        /**
         * The cache that holds the topic pages.
         */
        public static final Names<Page.Key,Page> PAGES =
            new Names<>("pages", METACACHE_PREFIX +"$pages$", Page.Key.class,
                Page.class, Names.Storage.MetaData);

        /**
         * The cache that holds the topic subscriber partitions.
         */
        public static final Names<Subscription.Key,Subscription> SUBSCRIPTIONS =
            new Names<>("subscriptions", METACACHE_PREFIX + "$subscriptions$",
                Subscription.Key.class, Subscription.class,
                Names.Storage.MetaData);

        /**
         * The cache used for notifying publishers and subscribers of full/empty events
         */
        public static final Names<NotificationKey, int[] /*channels*/> NOTIFICATIONS =
            new Names<>("notifications", METACACHE_PREFIX + "$notifications$",
                NotificationKey.class, int[].class, Names.Storage.MetaData);

        /**
         * The cache that holds usage data for a cache partition.
         */
        public static final Names<Usage.Key, Usage> USAGE =
            new Names<>("usage", METACACHE_PREFIX + "$usage$",
                Usage.Key.class, Usage.class, Names.Storage.Data);

        // ----- inner class: StorageType ---------------------------------------

        public enum Storage {Data, MetaData}

        // ----- data members ---------------------------------------------------

        /**
         * The name of this {@link Names}.
         */
        private final String f_sName;

        /**
         * The prefix to add to the topic name to obtain the cache name.
         */
        private final String f_sPrefix;

        /**
         * The key class of the cache.
         */
        private final Class<K> f_classKey;

        /**
         * The value class of the cache.
         */
        private final Class<V> f_classValue;

        /**
         * The {@link TypeAssertion} to use when obtaining the cache.
         */
        private final TypeAssertion<K,V> f_typeAssertion;

        /**
         * The storage location for the cache data.
         */
        private final Storage f_storage;
        }
    }
