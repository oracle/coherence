/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;
import com.tangosol.coherence.jcache.common.CoherenceCacheEntry;
import com.tangosol.coherence.jcache.common.CoherenceCacheEntryListenerRegistration;
import com.tangosol.coherence.jcache.common.CoherenceCacheMXBean;
import com.tangosol.coherence.jcache.common.InternalConverter;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;
import com.tangosol.coherence.jcache.common.MBeanServerRegistrationUtility;
import com.tangosol.coherence.jcache.common.SerializingInternalConverter;
import com.tangosol.coherence.jcache.partitionedcache.processors.ClearProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.ConditionalRemoveProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.ConditionalReplaceProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.ContainsKeyProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.GetAndPutProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.GetAndRemoveProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.GetAndReplaceProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.GetPartitionCountEP;
import com.tangosol.coherence.jcache.partitionedcache.processors.GetProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.InvokeProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.PutAllProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.PutIfAbsentProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.PutProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.RemoveProcessor;
import com.tangosol.coherence.jcache.partitionedcache.processors.ReplaceProcessor;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.WrapperCollections;
import com.tangosol.util.WrapperException;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import java.io.Closeable;
import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.cache.Cache;
import javax.cache.CacheException;

import javax.cache.configuration.CacheEntryListenerConfiguration;

import javax.cache.event.CacheEntryListener;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import javax.cache.integration.CompletionListener;

import javax.cache.management.CacheMXBean;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

/**
 * JCache of Key, Value pairs implemented over distributed cache.
 *
 * @param <K>  key type
 * @param <V>  value type
 *
 * @author jf  2013.07.07
 * @since Coherence 12.1.3
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PartitionedCache<K, V>
        extends AbstractCoherenceBasedCache<K, V, PartitionedCacheConfiguration<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PartitionedCache} configured by {@link PartitionedCacheConfiguration}
     * in context of {@link CoherenceBasedCacheManager} with JCache name <code>sJCacheName</code>.
     *
     * @param manager       CacheManager manages the created PartitionedCache.
     * @param sJCacheName   JCache cache name that must be unique within {@link CoherenceBasedCacheManager} context
     * @param configuration JCache cache configuration
     */
    public PartitionedCache(CoherenceBasedCacheManager manager, String sJCacheName,
                            PartitionedCacheConfiguration<K, V> configuration)
        {
        super(manager, sJCacheName, configuration);

        m_cacheId = new JCacheIdentifier(manager.getURI().toString(), sJCacheName);
        Logger.finest(() -> "PartitionedCache ctor cacheName=" + m_cacheId + " configuration=" + configuration);

        m_manager.putCacheToConfigurationMapping(m_cacheId, configuration);
        m_namedCache = getNamedCache(manager, m_cacheId);
        m_stats      = new PartitionedJCacheStatistics(this);

        m_fStats.set(configuration.isStatisticsEnabled());

        m_loader = getContext().getCacheLoader();
        m_mxbean = new CoherenceCacheMXBean<K, V>(this);

        // establish the required Serializer using the SerializerFactory in the ResourceRegistry
        ClassLoader              classLoader       = manager.getClassLoader();
        ConfigurableCacheFactory ccf               = manager.getConfigurableCacheFactory();
        ResourceRegistry         registryResources = ccf.getResourceRegistry();

        SerializerFactory factorySerializer = registryResources.getResource(SerializerFactory.class, "serializer");
        Serializer               serializer;

        if (factorySerializer == null)
            {
            // when there's no default serializer in the ResourceRegistry we use the
            // ExternalizableHelper to determine a suitable serializer
            serializer = ExternalizableHelper.ensureSerializer(classLoader);
            }
        else
            {
            serializer = factorySerializer.createSerializer(classLoader);
            }

        m_converterKey   = new SerializingInternalConverter<K>(serializer);
        m_converterValue = new SerializingInternalConverter<V>(serializer);

        if (configuration.isManagementEnabled())
            {
            setManagementEnabled(true);
            }

        if (configuration.isStatisticsEnabled())
            {
            setStatisticsEnabled(true);
            }

        // establish all of the Coherence MapListeners that will generate the JCache CacheEntryEvents
        // and deliver them to the registered JCache CacheEntryListenerRegistrations.
        LinkedList<CoherenceCacheEntryListenerRegistration<K, V>> listSynchDef =
            new LinkedList<CoherenceCacheEntryListenerRegistration<K, V>>();
        LinkedList<CoherenceCacheEntryListenerRegistration<K, V>> listAsyncDef =
            new LinkedList<CoherenceCacheEntryListenerRegistration<K, V>>();
        List<CacheEntryListenerConfiguration<K, V>> listListenerConfigurations =
            configuration.getCacheEntryListenerConfigurations();

        for (CacheEntryListenerConfiguration<K, V> listenerConfiguration : listListenerConfigurations)
            {
            CoherenceCacheEntryListenerRegistration<K, V> registration = new CoherenceCacheEntryListenerRegistration<K,
                                                                             V>(listenerConfiguration);

            if (listenerConfiguration.isSynchronous())
                {
                listSynchDef.add(registration);
                }
            else
                {
                listAsyncDef.add(registration);
                }
            }

        m_setListenerRegistrationSynchronous = new CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K,
            V>>(listSynchDef);
        m_setListenerRegistrationAsynchronous = new CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K,
            V>>(listAsyncDef);

        // TODO: possible optimization that introduces more complexity in number of mapListeners on named m_namedCache.
        // should CoherenceCacheEntryListenerDefinition filter be run on server-side and passed through
        // to coherence addMapListener.  Currently just using jcache filter on client side.

        // TODO: consider if worth honoring isOldValueRequired in coherence implementation.
        // currently disregarding possible optimization if JCache CacheEntryListenerConfiguration.isOldValueRequired()
        // returns false.
        // Always requiring OldValue in Coherence CacheEntryListeners to be able to honor
        // whether JCache Event Listeners requires it or not.

        m_filterCreateUpdateRemove = new PartitionedCacheAsynchronousMapListener.NonSyntheticEntryFilter();
        m_filterExpiryEvent        = new PartitionedCacheSyntheticDeleteMapListener.JCacheExpiryEntryFilter();
        synchronizeCacheEntryListeners();
        }

    // ----- CoherenceBasedCache interface ----------------------------------

    @Override
    public JCacheIdentifier getIdentifier()
        {
        return m_cacheId;
        }

    @Override
    public void destroy()
        {
        destroyCache(m_manager, m_cacheId, m_namedCache);
        }

    // ----- Cache interface --------------------------------------------------

    @Override
    public V get(K key)
        {
        RuntimeException exception = null;

        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException();
            }

        V value = null;

        try
            {
            value = m_converterValue.fromInternal(m_namedCache.invoke(key, new GetProcessor(m_cacheId)));
            }
        catch (Exception e)
            {
            // if read-through is enabled, possibility of read-through method causing failure.
            exception = handleException(e, CacheLoaderException.class);
            }

        if (exception != null)
            {
            throw exception;
            }

        return value;
        }

    @Override
    public Map<K, V> getAll(Set<? extends K> setKey)
        {
        ensureOpen();

        if (setKey.contains(null))
            {
            throw new NullPointerException();
            }

        try
            {
            return m_namedCache.invokeAll(setKey, new GetProcessor(m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheException.class);
            }
        }

    @Override
    public boolean containsKey(K key)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException();
            }

        try
            {
            return (Boolean) m_namedCache.invoke(key, new ContainsKeyProcessor(m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheException.class);
            }
        }

    @Override
    public void loadAll(final Set<? extends K> setKey, final boolean fReplaceExistingValues,
                        final CompletionListener listener)
        {
        ensureOpen();

        if (setKey == null)
            {
            throw new NullPointerException("setKey");
            }

        if (m_loader == null)
            {
            if (listener != null)
                {
                listener.onCompletion();
                }
            }
        else
            {
            for (K key : setKey)
                {
                if (key == null)
                    {
                    throw new NullPointerException("keys contains a null");
                    }
                }

            submit(new Runnable()
                {
                @Override
                public void run()
                    {
                    try
                        {
                        ArrayList<K> listKeysToLoad = new ArrayList<K>();

                        for (K key : setKey)
                            {
                            if (fReplaceExistingValues || !containsKey(key))
                                {
                                listKeysToLoad.add(key);
                                }
                            }

                        Map<? extends K, ? extends V> mapLoaded = m_loader.loadAll(listKeysToLoad);

                        // do not load entries with null values.
                        for (K key : listKeysToLoad)
                            {
                            if (mapLoaded.get(key) == null)
                                {
                                mapLoaded.remove(key);
                                }
                            }

                        putAllWithEntryProcessor(mapLoaded, fReplaceExistingValues, false, true);

                        if (listener != null)
                            {
                            listener.onCompletion();
                            }
                        }
                    catch (Exception e)
                        {
                        if (listener != null)
                            {
                            listener.onException(handleException(e, CacheLoaderException.class));
                            }
                        }
                    }


                });
            }
        }

    @Override
    public void put(K key, V value)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("key parameter");
            }

        if (value == null)
            {
            throw new NullPointerException("value parameter");
            }

        try
            {
            Binary binValue = (Binary) m_converterValue.toInternal(value);

            m_namedCache.invoke(key, new PutProcessor(binValue, m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public V getAndPut(K key, V value)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("key parameter");
            }

        if (value == null)
            {
            throw new NullPointerException("value parameter");
            }

        try
            {
            Binary binValue  = (Binary) m_converterValue.toInternal(value);
            Binary binResult = (Binary) m_namedCache.invoke(key, new GetAndPutProcessor(binValue, m_cacheId));

            return m_converterValue.fromInternal(binResult);
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        putAll(map, true, m_configuration.isWriteThrough());
        }

    /**
     *
     * @param map  map
     * @param fReplaceExistingValues if false, do not update existing values
     * @param fUseWriteThrough       is writer-through enabled
     */
    public void putAll(Map<? extends K, ? extends V> map, boolean fReplaceExistingValues, boolean fUseWriteThrough)
        {
        ensureOpen();

        if (map == null)
            {
            throw new NullPointerException();
            }

        putAllWithEntryProcessor(map, fReplaceExistingValues, fUseWriteThrough, false);

        }

    @Override
    public boolean putIfAbsent(K key, V value)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("parameter m_key");
            }

        if (value == null)
            {
            throw new NullPointerException("parameter m_value");
            }

        try
            {
            Binary binValue = (Binary) m_converterValue.toInternal(value);

            return (Boolean) m_namedCache.invoke(key, new PutIfAbsentProcessor(binValue, m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public boolean remove(K key)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException();
            }

        try
            {
            return (Boolean) m_namedCache.invoke(key, new RemoveProcessor<K, V>(m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public boolean remove(K key, V value)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("parameter key");
            }

        if (value == null)
            {
            throw new NullPointerException("parameter value");
            }

        try
            {
            Binary binValue = (Binary) m_converterValue.toInternal(value);

            return (Boolean) m_namedCache.invoke(key, new ConditionalRemoveProcessor<K, V>(binValue, m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public V getAndRemove(K key)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException();
            }

        try
            {
            return m_converterValue.fromInternal(m_namedCache.invoke(key, new GetAndRemoveProcessor(m_cacheId)));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public boolean replace(K key, V valueOrig, V value)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("parameter key");
            }

        if (valueOrig == null)
            {
            throw new NullPointerException("parameter valueOrig");
            }

        if (value == null)
            {
            throw new NullPointerException("parameter newValue");
            }

        try
            {
            Binary binValueOrig = (Binary) m_converterValue.toInternal(valueOrig);
            Binary binValue     = (Binary) m_converterValue.toInternal(value);

            return (Boolean) m_namedCache.invoke(key,
                new ConditionalReplaceProcessor(binValueOrig, binValue, m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public boolean replace(K key, V value)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("parameter key");
            }

        if (value == null)
            {
            throw new NullPointerException("parameter value");
            }

        try
            {
            Object iValue = m_converterValue.toInternal(value);

            return (Boolean) m_namedCache.invoke(key, new ReplaceProcessor((Binary) iValue, m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public V getAndReplace(K key, V value)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("parameter key");
            }

        if (value == null)
            {
            throw new NullPointerException("parameter value");
            }

        try
            {
            Object iValue = m_converterValue.toInternal(value);

            return m_converterValue.fromInternal(m_namedCache.invoke(key,
                new GetAndReplaceProcessor((Binary) iValue, m_cacheId)));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public void removeAll(Set<? extends K> keys)
        {
        ensureOpen();

        if (keys.contains(null))
            {
            throw new NullPointerException("keys set contains a null");
            }

        try
            {
            m_namedCache.invokeAll(keys, new RemoveProcessor<K, V>(m_cacheId));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public void removeAll()
        {
        ensureOpen();

        try
            {
            removeAll(m_namedCache.keySet());
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public void clear()
        {
        // jcache specification expects remove events for removeAll, but not for clear.
        // the SyntheticDeleteProcessor performs a synthetic coherence remove.
        ensureOpen();

        Set<K> keys = m_namedCache.keySet();

        if (!keys.isEmpty())
            {
            int cPartitions = getPartitionCount(keys);

            if (cPartitions == -1)
                {
                // not backed by a partitioned service; do it all at once
                m_namedCache.invoke(AlwaysFilter.INSTANCE, ClearProcessor.INSTANCE);
                }
            else
                {
                for (int i = 0; i < cPartitions; i++)
                    {
                    PartitionSet parts = new PartitionSet(cPartitions);

                    parts.add(i);
                    m_namedCache.invokeAll(new PartitionedFilter(AlwaysFilter.INSTANCE, parts),
                                           ClearProcessor.INSTANCE);
                    }
                }
            }
        }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
        {
        ensureOpen();

        if (key == null)
            {
            // Note: use JCache terminology in exception message to external user.
            throw new NullPointerException("parameter key");
            }

        if (entryProcessor == null)
            {
            // Note: use JCache terminology in exception message to external user.
            throw new NullPointerException("parameter entryProcessor");
            }

        try
            {
            EntryProcessorResult<T> result = (EntryProcessorResult<T>) m_namedCache.invoke(key,
                                                 new InvokeProcessor(m_cacheId, entryProcessor, arguments));

            return result == null ? null : result.get();
            }
        catch (Exception e)
            {
            throw handleException(e, EntryProcessorException.class);
            }
        }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> setKey, EntryProcessor<K, V, T> proc,
        Object... arguments)
        {
        Map<K, EntryProcessorResult<T>> result;

        if (setKey == null || proc == null)
            {
            throw new NullPointerException();
            }

        try
            {
            result = (Map<K, EntryProcessorResult<T>>) m_namedCache.invokeAll(setKey,
                new InvokeProcessor(m_cacheId, proc, arguments));
            }
        catch (Exception e)
            {
            throw handleException(e, EntryProcessorException.class);
            }

        return result;
        }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> config)
        {
        m_configuration.addCacheEntryListenerConfiguration(config);
        createAndAddListenerConfiguration(config);
        }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> config)
        {
        removeListenerConfiguration(config);
        }

    @Override
    public Iterator<Entry<K, V>> iterator()
        {
        ensureOpen();

        return new EntryIterator<K, V>(m_namedCache.entrySet().iterator(), this);
        }

    // ----- AbstractCoherenceBasedCache methods ----------------------------

    /**
     * Get JMX Management Bean.
     *
     * @return JMX Management bean
     */
    @Override
    public CacheMXBean getMBean()
        {
        return m_mxbean;
        }

    /**
     * Get JCache statistics
     *
     * @return JCache statistics when enabled, return null when not enabled.
     */
    public JCacheStatistics getStatistics()
        {
        return m_configuration.isStatisticsEnabled() ? m_stats : null;
        }

    @Override
    public void onBeforeClosing()
        {
        // close the configured CacheLoader
        if (m_loader instanceof Closeable)
            {
            try
                {
                ((Closeable) m_loader).close();
                }
            catch (IOException e)
                {
                Logger.fine("Unexpected exception in closable CacheLoader: ", e);
                }
            }

        // TODO: close ExpiryPolicy in all distributed members.

        // close the configured CacheEntryListeners
        for (CoherenceCacheEntryListenerRegistration registration : m_setListenerRegistrationAsynchronous)
            {
            CacheEntryListener listener = registration.getCacheEntryListener();
            if (listener instanceof Closeable)
                {
                try
                    {
                    ((Closeable) listener).close();
                    }
                catch (IOException e)
                    {
                    Logger.fine("Unexpected exception in closable Asynchronous CacheEntryListener: ", e);
                    }
                }
            }

        // close the configured CacheEntryListeners
        for (CoherenceCacheEntryListenerRegistration registration : m_setListenerRegistrationSynchronous)
            {
            CacheEntryListener listener = registration.getCacheEntryListener();
            if (registration.getCacheEntryListener() instanceof Closeable)
                {
                try
                    {
                    ((Closeable) listener).close();
                    }
                catch (IOException e)
                    {
                    Logger.fine("Unexpected exception in closable Synchronous CacheEntryListener: ", e);
                    }
                }
            }

        if (m_listenerSynchronous != null)
            {
            m_namedCache.removeMapListener(m_listenerSynchronous, m_filterCreateUpdateRemove);
            }

        if (m_listenerAsynchronous != null)
            {
            m_namedCache.removeMapListener(m_listenerAsynchronous, m_filterCreateUpdateRemove);
            }

        m_namedCache.removeMapListener(m_listenerExpiryEvent, m_filterExpiryEvent);

        ExecutorService exeSvc = m_refExecutorService.get();

        if (exeSvc != null)
            {
            exeSvc.shutdown();
            m_refExecutorService.compareAndSet(exeSvc, null);
            }
        }

    @Override
    public boolean isStatisticsEnabled()
        {
        return m_stats != null && m_fStats.get();
        }

    /**
     * get class loader for this Cache
     *
     * @return a classloader
     */
    protected ClassLoader getClassLoader()
        {
        return m_manager.getClassLoader();
        }

    /**
     * Requests a {@link FutureTask} to be performed.
     *
     * @param task the {@link FutureTask} to be performed
     */
    protected void submit(Runnable task)
        {
        if (isClosed())
            {
            return;
            }

        if (m_refExecutorService.get() == null)
            {
            // lazy creation of thread pool.
            ExecutorService newExeSvc  = Executors.newFixedThreadPool(20);
            boolean         fSetResult = m_refExecutorService.compareAndSet(null, newExeSvc);

            if (!fSetResult)
                {
                // concurrent creation of thread pool. this one lost so shut it down.
                newExeSvc.shutdown();
                }
            }

        m_refExecutorService.get().submit(task);
        }

    /**
     * get synchronous CacheEntryListenerRegistrations
     *
     * @return a thread-safe set of CacheEntryListenerRegistrations that are all synchronous listeners.
     */
    protected CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K,
        V>> getRegisteredSynchronousEventListeners()
        {
        return m_setListenerRegistrationSynchronous;
        }

    /**
     * get asynchronous CacheEntryListenerRegistrations
     *
     * @return a thread-safe set of CacheEntryListenerRegistrations that are all asynchronous listeners.
     */
    protected CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K,
        V>> getRegisteredAsynchronousEventListeners()
        {
        return m_setListenerRegistrationAsynchronous;
        }

    /**
     * Sets m_stats
     */
    public void setStatisticsEnabled(boolean fEnabled)
        {
        m_configuration.setStatisticsEnabled(fEnabled);
        m_fStats.set(fEnabled);

        if (fEnabled)
            {
            MBeanServerRegistrationUtility.registerCacheObject(this,
                MBeanServerRegistrationUtility.ObjectNameType.Statistics);
            }
        else
            {
            MBeanServerRegistrationUtility.unregisterCacheObject(this,
                MBeanServerRegistrationUtility.ObjectNameType.Statistics);
            }
        }

    /**
     * Sets management enablement
     * @param fEnabled true if management should be fEnabled
     */
    public void setManagementEnabled(boolean fEnabled)
        {
        m_configuration.setManagementEnabled(fEnabled);

        if (fEnabled)
            {
            MBeanServerRegistrationUtility.registerCacheObject(this,
                MBeanServerRegistrationUtility.ObjectNameType.Configuration);
            }
        else
            {
            MBeanServerRegistrationUtility.unregisterCacheObject(this,
                MBeanServerRegistrationUtility.ObjectNameType.Configuration);
            }
        }

    // ----- PartitionedCache methods ---------------------------------------

    /**
     * Return current {@link JCacheContext}
     *
     * @return {@link JCacheContext}
     */
    public JCacheContext getContext()
        {
        ResourceRegistry reg = m_manager.getConfigurableCacheFactory().getResourceRegistry();

        return JCacheContext.getContext(reg, m_cacheId, m_configuration);
        }

    /**
     * Destroy a partitioned cache.
     *
     * @param mgr  {@link CoherenceBasedCacheManager}
     * @param id  PartitionedCache external cache name
     * @param namedCache Coherence {@link NamedCache}
     */
    static public void destroyCache(CoherenceBasedCacheManager mgr, JCacheIdentifier id, NamedCache namedCache)
        {
        ConfigurableCacheFactory ccf = mgr.getConfigurableCacheFactory();

        mgr.removeCacheToConfigurationMapping(id);
        JCacheContext.unregister(ccf.getResourceRegistry(), id);
        try
            {
            MBeanServerRegistrationUtility.unregisterCacheObject(mgr, id, MBeanServerRegistrationUtility.ObjectNameType.Statistics);
            MBeanServerRegistrationUtility.unregisterCacheObject(mgr, id, MBeanServerRegistrationUtility.ObjectNameType.Configuration);
            }
        catch(Throwable t)
            {
            // ignore if unable to unregister without exception.
            }
        ccf.destroyCache(namedCache);

        // allow PartitionedCacheConfigurationMapListener.entryDeleted() to run.
        try
            {
            Thread.sleep(1000);
            }
        catch (InterruptedException e)
            {
            // ignore
            }
        }

    /**
     * Destroy a partitioned cache when there is no PartitionedCache object.
     * This method removes leftover context such as mapping from JCache name to configuration
     * and destroys outstanding NamedCache.
     *
     * Handles scenario where cache is created and closed in one jvm and discovered
     * not destroyed in another jvm.
     *
     * @param mgr  {@link CoherenceBasedCacheManager}
     * @param id  PartitionedCache external cache name
     */
    static public void destroyCache(CoherenceBasedCacheManager mgr, JCacheIdentifier id)
        {
        try
            {
            MBeanServerRegistrationUtility.unregisterCacheObject(mgr, id, MBeanServerRegistrationUtility.ObjectNameType.Statistics);
            MBeanServerRegistrationUtility.unregisterCacheObject(mgr, id, MBeanServerRegistrationUtility.ObjectNameType.Configuration);
            }
        catch(Throwable t)
            {
            // ignore if unable to unregister without exception.
            }
        destroyCache(mgr, id, getNamedCache(mgr, id));
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Register or unregister Coherence CacheEntryListeners on internal cache that dispatches JCache CacheEntryListener
     * based on current state of JCache CacheEntryListeners. If there are no JCache CacheEntryListeners
     * unregister the Coherence CacheEntryListener. If there are JCacheCacheEntryListeners and no Coherence CacheEntryListener,
     * register the appropriate CacheEntryListener with underlying Coherence namedCache.
     *
     * This capability is needed due to dynamic registration and remove of JCache CacheEntryListeners.
     */
    private void synchronizeCacheEntryListeners()
        {
        boolean F_LITE = false;

        if (m_setListenerRegistrationSynchronous.isEmpty() && m_setListenerRegistrationAsynchronous.isEmpty())
            {
            if (m_listenerExpiryEvent != null)
                {
                // no JCache CacheEntryListeners registered so unregister Coherence CacheEntryListener
                // for ExpiryEvents.
                m_namedCache.removeMapListener(m_listenerExpiryEvent, m_filterExpiryEvent);
                m_listenerExpiryEvent = null;
                }
            }
        else if (m_listenerExpiryEvent == null)
            {
            m_listenerExpiryEvent =
                new PartitionedCacheSyntheticDeleteMapListener("coherence-jcache-adapter-expiry-event-listener", this);
            m_namedCache.addMapListener(m_listenerExpiryEvent, m_filterExpiryEvent, F_LITE);
            }

        if (m_setListenerRegistrationSynchronous.isEmpty())
            {
            if (m_listenerSynchronous != null)
                {
                // no JCache Synchronous CacheEntryListeners registered so unregister Coherence Synchronous CacheEntryListener.
                m_namedCache.removeMapListener(m_listenerSynchronous, m_filterCreateUpdateRemove);
                m_listenerSynchronous = null;
                }
            }
        else if (m_listenerSynchronous == null)
            {
            m_listenerSynchronous =
                new PartitionedCacheSynchronousMapListener("coherence-jcache-adapter-synchronous-listener", this);
            m_namedCache.addMapListener(m_listenerSynchronous, m_filterCreateUpdateRemove, F_LITE);
            }

        if (m_setListenerRegistrationAsynchronous.isEmpty())
            {
            if (m_listenerAsynchronous != null)
                {
                // no JCache Asynchronous CacheEntryListeners registered so unregister Coherence Aynchronous CacheEntryListener
                m_namedCache.removeMapListener(m_listenerAsynchronous, m_filterCreateUpdateRemove);
                m_listenerAsynchronous = null;
                }
            }
        else if (m_listenerAsynchronous == null)
            {
            m_listenerAsynchronous =
                new PartitionedCacheAsynchronousMapListener("coherence-jcache-adapter-asynchronous-listener", this);
            m_namedCache.addMapListener(m_listenerAsynchronous, m_filterCreateUpdateRemove, F_LITE);
            }
        }

    /**
     * Given CacheManager and JCacheIdentifier, ensure the corresponding named cache.
     *
     * @param mgr CacheManager
     * @param id  unique JCacheIdentifier
     *
     * @return corresponding {@link NamedCache}
     */
    static private NamedCache getNamedCache(CoherenceBasedCacheManager mgr, JCacheIdentifier id)
        {
        return mgr.getConfigurableCacheFactory().ensureCache(CoherenceBasedCache.JCACHE_PARTITIONED_CACHE_NAME_PREFIX
            + id.getCanonicalCacheName(), mgr.getClassLoader());
        }

    /**
     * Add cache entry listener configuration
     *
     * @param configuration  configuration
     */
    private void createAndAddListenerConfiguration(CacheEntryListenerConfiguration<K, V> configuration)
        {
        CoherenceCacheEntryListenerRegistration<K, V> registration = new CoherenceCacheEntryListenerRegistration<K,
                                                                         V>(configuration);

        if (configuration.isSynchronous())
            {
            m_setListenerRegistrationSynchronous.add(registration);
            }
        else
            {
            m_setListenerRegistrationAsynchronous.add(registration);

            }

        synchronizeCacheEntryListeners();
        }

    /**
     * Remove cache entry listener configuration
     *
     * @param configListenerConfiguration  configuration
     */
    private void removeListenerConfiguration(CacheEntryListenerConfiguration<K, V> configListenerConfiguration)
        {
        if (configListenerConfiguration == null)
            {
            throw new NullPointerException("CacheEntryListenerConfiguration can't be null");
            }

        CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K, V>> setListeners =
            configListenerConfiguration.isSynchronous()
            ? m_setListenerRegistrationSynchronous : m_setListenerRegistrationAsynchronous;

        for (CoherenceCacheEntryListenerRegistration<K, V> listenerRegistration : setListeners)
            {
            if (configListenerConfiguration.equals(listenerRegistration.getConfiguration()))
                {
                setListeners.remove(listenerRegistration);
                m_configuration.getCacheEntryListenerConfigurations().remove(configListenerConfiguration);
                }
            }

        synchronizeCacheEntryListeners();
        }

    /**
     * PutAll Entry Processor
     *
     * @param map  key, value map to putAll into this cache
     * @param fReplaceExistingValues true if an entry in map should replace an existing entry in cache
     * @param fUseWriteThrough true if write-through should be performed for each entry in map
     * @param fLoadAll if true, disable write-through since being called from loadAll context.
     */
    private void putAllWithEntryProcessor(Map<? extends K, ? extends V> map, boolean fReplaceExistingValues,
        boolean fUseWriteThrough, boolean fLoadAll)
        {
        Map<Object, Binary> binMap = new HashMap<Object, Binary>(map.size());

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            {
            if (entry.getKey() == null)
                {
                throw new NullPointerException("parameter map had a null key with a value of " + entry.getValue());
                }

            if (entry.getValue() == null)
                {
                throw new NullPointerException("parameter map had a null value for key " + entry.getKey());
                }

            // TODO: figure out why binKey not working.
            // Commented out. This was not working.  CompositeKey key worked when in external form.
            // Binary bKey   = (Binary) m_converterKey.toInternal(m_entry.getKey());
            Binary bValue = (Binary) m_converterValue.toInternal(entry.getValue());

            binMap.put(entry.getKey(), bValue);

            // imap.put(bKey, bValue);
            }

        try
            {
            m_namedCache.invokeAll(PutAllProcessor.setOf(binMap),
                                   new PutAllProcessor(m_cacheId, fReplaceExistingValues, fUseWriteThrough, fLoadAll));
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    /**
     * Get a Class Constructor for clzz.
     *
     * @param clzz class to lookup constructor via reflection
     * @param <T>  type of this class
     *
     * @return a constructor that takes one parameter that is Throwable or null, if none exist.
     */
    private <T> Constructor<T> getClassCtor(Class<T> clzz)
        {
        Class[]        argTypes = {Throwable.class};
        Constructor<T> result   = null;

        try
            {
            result = clzz.getDeclaredConstructor(argTypes);
            }
        catch (NoSuchMethodException e)
            {
            }

        return result;
        }

    /**
     * common exception handling for all JCache PartitionedCache operations
     *
     * @param exception  exception
     * @param clzException class to wrapper exception in
     * @param <T>          the type of wrapper exception
     *
     * @return a RuntimeException
     */
    private <T extends RuntimeException> RuntimeException handleException(Exception exception, Class<T> clzException)
        {
        Throwable t = exception;

        if (exception instanceof WrapperException)
            {
            t = exception.getCause();

            if (t instanceof Error)
                {
                throw(Error) t;
                }
            }

        if (clzException.isInstance(t))
            {
            // avoid multiple wrappers
            return (T) t;
            }
        else
            {
            Constructor<T> ctor    = getClassCtor(clzException);
            Object[]       args    = {t};
            T              eResult = null;

            if (ctor != null)
                {
                try
                    {
                    eResult = ctor.newInstance(args);
                    }
                catch (InstantiationException e)
                    {
                    }
                catch (IllegalAccessException e)
                    {
                    }
                catch (InvocationTargetException e)
                    {
                    }
                }

            return eResult;
            }
        }

    /**
     * Performance optimization: m_namedCache partition count for this m_namedCache.
     * @param setKey non-empty set of keys
     * @return the partition count
     */
    private int getPartitionCount(Set<K> setKey)
        {
        if (m_ref_c_partition.get() == UNKNOWN_PARTITION_COUNT)
            {
            ensureOpen();

            Iterator<K> iter = setKey.iterator();

            if (iter.hasNext())
                {
                K   key         = iter.next();

                int cPartitions = (Integer) m_namedCache.invoke(key, new GetPartitionCountEP());

                m_ref_c_partition.compareAndSet(UNKNOWN_PARTITION_COUNT, cPartitions);
                }
            }

        return m_ref_c_partition.get();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return m_sJCacheName;
        }

    // ----- innner class ---------------------------------------------------

    /**
     * JCache Entry iterator
     *
     * @param <K> key type
     * @param <V> value type
     *
     */
    public static class EntryIterator<K, V>
            extends WrapperCollections.AbstractWrapperIterator
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an {@link EntryIterator} backed by the specified iterator.
         *
         *
         * @param iter the underlying iterator
         * @param cache iterate over entries of this cache
         */
        public EntryIterator(Iterator<Map.Entry<K, V>> iter, PartitionedCache<K, V> cache)
            {
            super(iter);
            m_namedCache = cache;
            }

        // ----- Iterator interface -----------------------------------------

        @Override
        public boolean hasNext()
            {
            if (m_entryNext == null)
                {
                fetch();
                }

            return m_entryNext != null;
            }

        @Override
        public Entry<K, V> next()
            {
            if (hasNext())
                {
                // remember the m_entryLast (so that we call allow for removal)
                m_entryLast = m_entryNext;

                // reset m_entryNext to force fetching the next available m_entry
                m_entryNext = null;

                return new CoherenceCacheEntry<K, V>(m_entryLast);
                }
            else
                {
                throw new NoSuchElementException();
                }
            }

        @Override
        public void remove()
            {
            if (m_entryLast == null)
                {
                throw new IllegalStateException("Must progress to the next m_entry to remove");
                }
            else
                {
                m_namedCache.remove(m_entryLast.getKey());
                }
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Fetches the next available, non-expired m_entry from the underlying
         * iterator.
         */
        private void fetch()
            {
            while (m_entryNext == null && super.hasNext())
                {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) super.next();

                // check for lazy expiration and update last Accessed via call to m_namedCache.get().
                V value = m_namedCache.get(entry.getKey());

                if (value != null)
                    {
                    m_entryNext = entry;
                    }
                }
            }

        // ----- data members -----------------------------------------------
        private final PartitionedCache<K, V> m_namedCache;
        private Map.Entry<K, V>              m_entryLast = null;
        private Map.Entry<K, V>              m_entryNext = null;
        }

    // ------ constants -----------------------------------------------------

    static private final int UNKNOWN_PARTITION_COUNT = -2;

    // ------ data members --------------------------------------------------

    /**
     * An {@link java.util.concurrent.ExecutorService} for the purposes of performing asynchronous
     * background work.
     */
    private final AtomicReference<ExecutorService> m_refExecutorService = new AtomicReference<ExecutorService>(null);
    private final CacheLoader<K, V>                m_loader;
    private final CacheMXBean                      m_mxbean;
    private final JCacheStatistics                 m_stats;
    private final InternalConverter<K>             m_converterKey;
    private final InternalConverter<V>             m_converterValue;
    private MapListener                            m_listenerAsynchronous;
    private MapListener                            m_listenerSynchronous;
    private final Filter                           m_filterCreateUpdateRemove;
    private MapListener                            m_listenerExpiryEvent;
    private final Filter                           m_filterExpiryEvent;
    private final AtomicInteger                    m_ref_c_partition = new AtomicInteger(UNKNOWN_PARTITION_COUNT);
    private AtomicBoolean                          m_fStats          = new AtomicBoolean();
    private final JCacheIdentifier                 m_cacheId;

    /**
     * The {@link javax.cache.configuration.CacheEntryListenerConfiguration}s for the {@link Cache}.
     */
    private final CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K, V>> m_setListenerRegistrationSynchronous;
    private final CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K, V>> m_setListenerRegistrationAsynchronous;
    }
