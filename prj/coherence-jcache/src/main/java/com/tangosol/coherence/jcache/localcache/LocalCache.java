/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;
import com.tangosol.coherence.jcache.common.CoherenceCacheEntry;
import com.tangosol.coherence.jcache.common.CoherenceCacheEntryEvent;
import com.tangosol.coherence.jcache.common.CoherenceCacheEntryListenerRegistration;
import com.tangosol.coherence.jcache.common.CoherenceCacheEventEventDispatcher;
import com.tangosol.coherence.jcache.common.CoherenceCacheMXBean;
import com.tangosol.coherence.jcache.common.CoherenceEntryProcessorResult;
import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.InternalConverter;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;
import com.tangosol.coherence.jcache.common.MBeanServerRegistrationUtility;
import com.tangosol.coherence.jcache.common.ReferenceInternalConverter;
import com.tangosol.coherence.jcache.common.SerializingInternalConverter;
import com.tangosol.coherence.jcache.localcache.processors.ConditionalRemoveProcessor;
import com.tangosol.coherence.jcache.localcache.processors.GetAndReplaceProcessor;
import com.tangosol.coherence.jcache.localcache.processors.InvokeProcessor;
import com.tangosol.coherence.jcache.localcache.processors.ReplaceIfExistsProcessor;
import com.tangosol.coherence.jcache.localcache.processors.ReplaceWithProcessor;
import com.tangosol.coherence.jcache.localcache.processors.SyntheticDeleteProcessor;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.WrapperCollections;
import com.tangosol.util.WrapperException;
import com.tangosol.util.filter.AlwaysFilter;

import java.io.Closeable;
import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.cache.CacheException;

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;

import javax.cache.event.CacheEntryExpiredListener;

import javax.cache.event.CacheEntryListener;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;
import javax.cache.integration.CompletionListener;

import javax.cache.management.CacheMXBean;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

import static javax.cache.event.EventType.EXPIRED;

/**
 * An in-process local cache implementation of a {@link javax.cache.Cache} based
 * on a Coherence &lt;local-cache&gt;.
 *
 * @param <K>  key type
 * @param <V>  value type
 *
 * @author jf  2013.12.17
 * @author bko  2013.12.17
 * @since Coherence 12.1.3
 */
public class LocalCache<K, V>
        extends AbstractCoherenceBasedCache<K, V, LocalCacheConfiguration<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link LocalCache} configured by {@link LocalCacheConfiguration}.
     *
     * @param manager        the {@link com.tangosol.coherence.jcache.CoherenceBasedCacheManager} that created the {@link LocalCache}
     * @param sJCacheName    the JCache Name for the {@link LocalCache}
     * @param configuration  the {@link LocalCacheConfiguration} for the {@link LocalCache}
     */
    public LocalCache(CoherenceBasedCacheManager manager, String sJCacheName,
                      LocalCacheConfiguration<K, V> configuration)
        {
        super(manager, sJCacheName, configuration);

        m_cacheId = new JCacheIdentifier(manager.getURI().toString(), sJCacheName);

        m_ctx = JCacheContext.getContext(manager.getConfigurableCacheFactory().getResourceRegistry(), m_cacheId,
                                         m_configuration);

        // the prefix of coherence named cache is used to ensure that named cache uses a local scheme from
        // coherence cache configuration. JCache developer never sees this name, only the one they provided to
        // their JCache cache.
        m_namedCache =
            manager.getConfigurableCacheFactory().ensureCache(CoherenceBasedCache.JCACHE_LOCAL_CACHE_NAME_PREFIX
                + getInternalCacheName(), manager.getClassLoader());

        // fix COH-11926.  Detect invalid configuration that results in the underlying Coherence NamedCache
        // supporting PartitionedService.  Without this check, the user will observe a NotSerializableException for
        // LocalCacheValue as first sign of misconfiguration.  This check catches the typical failure
        // of incorrectly using coherence-cache-config.xml as a JCache CacheManager URI.
        if (PartitionedService.class.isAssignableFrom(m_namedCache.getCacheService().getClass()))
            {
            throw new IllegalStateException("Invalid JCache LocalCache configuration: Verify that the uri used by this cache's CacheManager includes the attribute \"xmlns:jcache=class://com.tangosol.coherence.jcache.JCacheNamespace\" on "
                                            + " element cache-config");
            }

        m_fStats.set(m_configuration.isStatisticsEnabled());
        m_mxbean = new CoherenceCacheMXBean<K, V>(this);

        // establish the appropriate key and value converters
        // (based on the configured store-by-reference or store-by-value semantics)
        if (m_configuration.isStoreByValue())
            {
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
            }
        else
            {
            // when using store-by-reference we don't convert the keys or values
            m_converterKey   = new ReferenceInternalConverter<K>();
            m_converterValue = new ReferenceInternalConverter<V>();
            }

        if (m_configuration.isManagementEnabled())
            {
            setManagementEnabled(true);
            }

        if (m_configuration.isStatisticsEnabled())
            {
            setStatisticsEnabled(true);
            }

        // establish all of the listeners
        LinkedList<CoherenceCacheEntryListenerRegistration<K, V>> listSynchDef =
            new LinkedList<CoherenceCacheEntryListenerRegistration<K, V>>();

        LinkedList<CoherenceCacheEntryListenerRegistration<K, V>> listAsyncDef =
            new LinkedList<CoherenceCacheEntryListenerRegistration<K, V>>();

        for (CacheEntryListenerConfiguration<K, V> listenerConfiguration :
            m_configuration.getCacheEntryListenerConfigurations())
            {
            CoherenceCacheEntryListenerRegistration<K, V> definition = new CoherenceCacheEntryListenerRegistration<K,
                                                                           V>(listenerConfiguration);

            if (listenerConfiguration.isSynchronous())
                {
                listSynchDef.add(definition);
                }
            else
                {
                listAsyncDef.add(definition);
                }
            }

        m_setListenerRegistrationSynchronous = new CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K,
            V>>(listSynchDef);
        m_setListenerRegistrationAsynchronous = new CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K,
            V>>(listAsyncDef);

        m_filterCreateUpdateRemove = new LocalCacheAsynchronousMapListener.NonSyntheticEntryFilter();
        synchronizeCacheEntryListeners();
        }

    // ----- AbstractCoherenceBasedCache methods ----------------------------

    @Override
    public void onBeforeClosing()
        {
        // close the configured CacheLoader
        if (m_ctx.getCacheLoader() instanceof Closeable)
            {
            try
                {
                ((Closeable) m_ctx.getCacheLoader()).close();
                }
            catch (IOException e)
                {
                Logger.fine("Unexpected exception in closable CacheLoader: ", e);
                }
            }

        // close the configured CacheWriter
        if (m_ctx.getCacheWriter() instanceof Closeable)
            {
            try
                {
                ((Closeable) m_ctx.getCacheWriter()).close();
                }
            catch (IOException e)
                {
                Logger.fine("Unexpected exception in closable CacheWriter: ", e);
                }
            }

        if (m_ctx.getExpiryPolicy() instanceof Closeable)
            {
            try
                {
                ((Closeable) m_ctx.getExpiryPolicy()).close();
                }
            catch (IOException e)
                {
                Logger.fine("Unexpected exception in closable ExpiryPolicy: ", e);
                }

            }

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
            if (listener instanceof Closeable)
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

        ExecutorService exeSvc = m_refExecutorService.get();

        if (exeSvc != null)
            {
            exeSvc.shutdown();
            m_refExecutorService.compareAndSet(exeSvc, null);
            }

        // Remove MBean registrations
        setStatisticsEnabled(false);
        setManagementEnabled(false);
        }

    /**
     * Get JCache statistics.
     *
     * @return {@link JCacheStatistics} if enabled, null otherwise
     */
    @Override
    public JCacheStatistics getStatistics()
        {
        return m_configuration.isStatisticsEnabled() ? m_ctx.getStatistics() : null;
        }

    @Override
    public boolean isStatisticsEnabled()
        {
        return m_ctx.getStatistics() != null && m_fStats.get();
        }

    @Override
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

    @Override
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

    @Override
    public CacheMXBean getMBean()
        {
        return m_mxbean;
        }

    // ----- LocalCache methods ---------------------------------------------

    /**
     * Get synchronous CacheEntryEventListeners.
     *
     * @return synchronous CacheEntryEventListenerRegistrations
     */
    public CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K, V>> getRegisteredSynchronousEventListeners()
        {
        return m_setListenerRegistrationSynchronous;
        }

    /**
     * Get asynchronous CacheEntryEventListeners.
     *
     * @return asynchronous CacheEntryEventListenerRegistrations
     */
    public CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K, V>> getRegisteredAsynchronousEventListeners()
        {
        return m_setListenerRegistrationAsynchronous;
        }

    /**
     * Process expiry for entry of key. Dispatch ExpiryEvent(s) to dispatcher.
     *
     * @param key   key of entry to be expired.
     * @param dispatcher dispatcher for ExpiryEvent.
     */
    public void processExpiries(K key, CoherenceCacheEventEventDispatcher<K, V> dispatcher)
        {
        Object internalKey = m_converterKey.toInternal(key);
        LocalCacheValue cacheValue = (LocalCacheValue) m_namedCache.invoke(internalKey,
                                         new SyntheticDeleteProcessor<K, V>(this));
        Object expiredValue =
            cacheValue == null
            ? null : m_converterValue.fromInternal(cacheValue.getInternalValue(Helper.getCurrentTimeMillis()));

        dispatcher.addEvent(CacheEntryExpiredListener.class,
                            new CoherenceCacheEntryEvent<K, V>(this, EXPIRED, key, null, (V) expiredValue));
        }

    /**
     * Dispatch an ExpiryEvent to registered CacheEntryListeners.
     *
     * @param keyExpired key of expired entry
     * @param valueExpired value of expired entry
     * @param dispatcher  dispatch EXPIRED CacheEntryEvent to registered listeners of this cache.
     */
    public void processExpiries(K keyExpired, V valueExpired, CoherenceCacheEventEventDispatcher<K, V> dispatcher)
        {
        Object internalKey = m_converterKey.toInternal(keyExpired);

        m_namedCache.invoke(internalKey, new SyntheticDeleteProcessor<K, V>(this));
        dispatcher.addEvent(CacheEntryExpiredListener.class,
                            new CoherenceCacheEntryEvent<K, V>(this, EXPIRED, keyExpired, null, valueExpired));
        }

    /**
     * Get the JCache context for this JCache's operations
     *
     * @return JCacheContext
     */
    public JCacheContext getContext()
        {
        return m_ctx;
        }

    /**
     * Get key converter for this.
     *
     * @return keyConverter
     */
    public InternalConverter<K> getKeyConverter()
        {
        return m_converterKey;
        }

    /**
     * Get value converter for this.
     *
     * @return valueConverter  value converter for this.
     */
    public InternalConverter<V> getValueConverter()
        {
        return m_converterValue;
        }

    /**
     * dispatch outstanding JCache Events
     *
     * @param dispatcher outstanding JCache events
     */
    public void dispatch(final CoherenceCacheEventEventDispatcher<K, V> dispatcher)
        {
        dispatcher.dispatch(getRegisteredSynchronousEventListeners());

        submit(new Runnable()
            {
            @Override
            public void run()
                {
                dispatcher.dispatch(getRegisteredAsynchronousEventListeners());
                }


            });
        }

    // ----- Cache interface ------------------------------------------------

    @Override
    public V get(K key)
        {
        RuntimeException exception = null;

        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException();
            }

        V                                        value      = null;
        CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();

        try
            {
            value = getValue(key, dispatcher);
            }
        catch (Exception e)
            {
            // if read-through is enabled, possibility of read-through method causing failure.
            // be sure to count this as a m_cacheSource miss and only then rethrow exception.
            exception = handleException(e, CacheLoaderException.class);
            }

        dispatch(dispatcher);

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

        if (setKey == null || setKey.contains(null))
            {
            throw new NullPointerException();
            }

        try
            {
            Map<K, V>                                map        = new HashMap<K, V>(setKey.size());
            CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();

            for (K key : setKey)
                {
                V value = getValue(key, dispatcher);

                if (value != null)
                    {
                    map.put(key, value);
                    }
                }

            dispatch(dispatcher);

            return map;
            }
        catch (Exception e)
            {
            throw handleException(e, CacheException.class);
            }
        }

    @Override
    public boolean containsKey(K key)
        {
        boolean fResult = false;

        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException();
            }

        try
            {
            Object          internalKey = m_converterKey.toInternal(key);
            LocalCacheValue cacheValue  = (LocalCacheValue) m_namedCache.get(internalKey);
            boolean         fIsExpired  = cacheValue != null && cacheValue.isExpiredAt(Helper.getCurrentTimeMillis());

            if (cacheValue == null || fIsExpired)
                {
                fResult = false;

                if (fIsExpired)
                    {
                    CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K,
                                                                              V>();

                    processExpiries(key, dispatcher);
                    dispatch(dispatcher);
                    }
                }
            else
                {
                fResult = true;
                }

            return fResult;
            }
        catch (Exception e)
            {
            throw handleException(e, CacheException.class);
            }
        }

    @Override
    public void loadAll(final Set<? extends K> setKeys, final boolean fReplaceExistingValues,
                        final CompletionListener listener)
        {
        ensureOpen();

        if (setKeys == null)
            {
            throw new NullPointerException("keys");
            }

        if (m_ctx.getCacheLoader() == null)
            {
            if (listener != null)
                {
                listener.onCompletion();
                }
            }
        else
            {
            for (K key : setKeys)
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

                        for (K key : setKeys)
                            {
                            if (fReplaceExistingValues || !containsKey(key))
                                {
                                listKeysToLoad.add(key);
                                }
                            }

                        Map<? extends K, ? extends V> mapLoaded = m_ctx.getCacheLoader().loadAll(listKeysToLoad);

                        // do not load entries with null values.
                        for (K key : listKeysToLoad)
                            {
                            if (mapLoaded.get(key) == null)
                                {
                                mapLoaded.remove(key);
                                }
                            }

                        // don't write-through just read-through values.
                        final boolean USE_WRITE_THROUGH = false;

                        putAll(mapLoaded, fReplaceExistingValues, USE_WRITE_THROUGH);

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
        put(key, value, true);
        }

    @Override
    public boolean putIfAbsent(K key, V value)
        {
        return putIfAbsent(key, value, true);
        }

    @Override
    public V getAndPut(K key, V value)
        {
        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("null value specified for key " + key);
            }

        if (value == null)
            {
            throw new NullPointerException("null value specified for value " + value);
            }

        long ldtNow = Helper.getCurrentTimeMillis();
        long start  = isStatisticsEnabled() ? ldtNow : 0;

        V    result;
        int  putCount = 0;

        try
            {
            CoherenceCacheEventEventDispatcher<K, V> dispatcher    = new CoherenceCacheEventEventDispatcher<K, V>();

            Object                                   internalKey   = m_converterKey.toInternal(key);
            Object                                   internalValue = m_converterValue.toInternal(value);

            LocalCacheValue                          cachedValue   = (LocalCacheValue) m_namedCache.get(internalKey);

            boolean                                  isExpired = cachedValue != null && cachedValue.isExpiredAt(ldtNow);

            if (cachedValue == null || isExpired)
                {
                result = null;

                CoherenceCacheEntry<K, V> entry = new CoherenceCacheEntry<K, V>(key, value);

                writeCacheEntry(entry);

                if (isExpired)
                    {
                    processExpiries(key, dispatcher);
                    }

                cachedValue = LocalCacheValue.createLocalCacheValue(internalValue, ldtNow,
                    getContext().getExpiryPolicy());

                if (!cachedValue.isExpiredAt(ldtNow))
                    {
                    m_namedCache.put(internalKey, cachedValue);
                    putCount++;
                    }
                }
            else
                {
                V                         oldValue =
                    m_converterValue.fromInternal(cachedValue.getInternalValue(ldtNow));
                CoherenceCacheEntry<K, V> entry    = new CoherenceCacheEntry<K, V>(key, value, oldValue);

                writeCacheEntry(entry);
                m_namedCache.put(internalKey, updateLocalCacheValue(m_ctx, cachedValue, internalValue, ldtNow));
                putCount++;
                result = oldValue;
                }
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }

        if (isStatisticsEnabled())
            {
            if (result == null)
                {
                getStatistics().registerMisses(1, start);
                }
            else
                {
                getStatistics().registerHits(1, start);
                }

            if (putCount > 0)
                {
                getStatistics().registerPuts(putCount, start);
                }
            }

        return result;
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        putAll(map, true, m_configuration.isWriteThrough());
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
            CoherenceCacheEventEventDispatcher<K, V> dispatcher  = new CoherenceCacheEventEventDispatcher<K, V>();
            long                                     ldtNow      = Helper.getCurrentTimeMillis();
            long                                     ldtStart    = isStatisticsEnabled() ? ldtNow : 0L;
            Object                                   internalKey = m_converterKey.toInternal(key);

            deleteCacheEntry(key);

            LocalCacheValue value      = (LocalCacheValue) m_namedCache.remove(internalKey);
            boolean         fIsExpired = value != null && value.isExpiredAt(ldtNow);
            boolean         fResult    = false;

            if (value == null || fIsExpired)
                {
                if (fIsExpired)
                    {
                    processExpiries(key, dispatcher);
                    dispatch(dispatcher);
                    }

                fResult = false;

                }
            else
                {
                fResult = true;
                }

            if (fResult && isStatisticsEnabled())
                {
                m_ctx.getStatistics().registerRemoves(1, ldtStart);
                }

            return fResult;
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

        if (key == null || value == null)
            {
            throw new NullPointerException();
            }

        try
            {
            Object internalKey           = m_converterKey.toInternal(key);
            Object internalExpectedValue = m_converterValue.toInternal(value);

            return (Boolean) m_namedCache.invoke(internalKey,
                new ConditionalRemoveProcessor<K, V>(this, internalExpectedValue));

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
            CoherenceCacheEventEventDispatcher<K, V> dispatcher  = new CoherenceCacheEventEventDispatcher<K, V>();

            long ldtStart = isStatisticsEnabled() ? Helper.getCurrentTimeMillis() : 0L;
            long                                     ldtNow      = ldtStart;
            Object                                   internalKey = m_converterKey.toInternal(key);
            LocalCacheValue                          cacheValue  = (LocalCacheValue) m_namedCache.get(internalKey);
            boolean fIsExpired = cacheValue == null ? false : cacheValue.isExpiredAt(ldtNow);
            V oResult = cacheValue == null ? null : m_converterValue.fromInternal(cacheValue.get());

            deleteCacheEntry(key);

            if (cacheValue == null || fIsExpired)
                {
                // expired
                if (fIsExpired)
                    {
                    processExpiries(key, oResult, dispatcher);
                    dispatch(dispatcher);
                    }

                oResult = null;
                }
            else
                {
                m_namedCache.remove(internalKey);
                }

            if (isStatisticsEnabled())
                {
                if (oResult == null)
                    {
                    m_ctx.getStatistics().registerMisses(1, ldtStart);
                    }
                else
                    {
                    m_ctx.getStatistics().registerRemoves(1, ldtStart);
                    m_ctx.getStatistics().registerHits(1, ldtStart);
                    }
                }

            return oResult;
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
            throw new NullPointerException("key can't be null");
            }

        if (valueOrig == null)
            {
            throw new NullPointerException("expectedValue can't be null");
            }

        if (value == null)
            {
            throw new NullPointerException("newValue can't be null");
            }

        boolean fResult = false;

        try
            {
            Object internalKey       = m_converterKey.toInternal(key);
            Object internalValueOrig = m_converterValue.toInternal(valueOrig);
            Object internalNewValue  = m_converterValue.toInternal(value);

            fResult = (Boolean) m_namedCache.invoke(internalKey,
                new ReplaceWithProcessor<K, V>(this, internalValueOrig, internalNewValue));
            }
        catch (Exception e)
            {
            handleException(e, CacheWriterException.class);
            }

        return fResult;
        }

    @Override
    public boolean replace(K key, V value)
        {
        ensureOpen();

        boolean fResult = false;

        if (key == null)
            {
            throw new NullPointerException("key can't be null");
            }

        if (value == null)
            {
            throw new NullPointerException("value can't be null");
            }

        try
            {
            Object internalKey      = m_converterKey.toInternal(key);
            Object internalNewValue = m_converterValue.toInternal(value);

            fResult = (Boolean) m_namedCache.invoke(internalKey,
                new ReplaceIfExistsProcessor<K, V>(this, internalNewValue));
            }
        catch (Exception e)
            {
            handleException(e, CacheWriterException.class);
            }

        return fResult;
        }

    @Override
    public V getAndReplace(K key, V value)
        {
        V result = null;

        ensureOpen();

        if (key == null)
            {
            throw new NullPointerException("null key not allowed");
            }

        if (value == null)
            {
            throw new NullPointerException("null value specified for key " + key);
            }

        try
            {
            Object internalKey      = m_converterKey.toInternal(key);
            Object internalNewValue = m_converterValue.toInternal(value);

            result = m_converterValue.fromInternal(m_namedCache.invoke(internalKey,
                new GetAndReplaceProcessor(this, internalNewValue)));
            }
        catch (Exception e)
            {
            handleException(e, CacheWriterException.class);
            }

        return result;
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
            // TODO: implement this via entry processor
            // Map<K, Boolean> map = (Map<K, Boolean>) m_cacheSource.invokeAll(keys, new RemoveProcessor<K, V>(m_cacheId));
            for (K key : keys)
                {
                remove(key);
                }
            }
        catch (Exception e)
            {
            throw handleException(e, CacheWriterException.class);
            }
        }

    @Override
    public void removeAll()
        {
        // jcache specification expects remove events for removeAll, but not for clear.
        // coherence remove event occurs for each cleared m_entry.
        ensureOpen();

        try
            {
            Set keys = m_namedCache.keySet();

            for (Object internalKey : keys)
                {
                remove(m_converterKey.fromInternal(internalKey));
                }
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
        // the SyntheticDeleteProcessor is suppose to perform a synthetic coherence remove.
        ensureOpen();

        m_namedCache.invokeAll(AlwaysFilter.INSTANCE, new SyntheticDeleteProcessor(this));
        }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> proc, Object... arguments)
        {
        ensureOpen();

        if (key == null)
            {
            // Note: use JCache terminology in exception message to external user.
            throw new NullPointerException("parameter key");
            }

        if (proc == null)
            {
            // Note: use JCache terminology in exception message to external user.
            throw new NullPointerException("parameter entryProcessor");
            }

        Object internalKey = m_converterKey.toInternal(key);

        try
            {
            CoherenceEntryProcessorResult<T> result =
                (CoherenceEntryProcessorResult<T>) m_namedCache.invoke(internalKey,
                    new InvokeProcessor<K, V, T>(this, proc, arguments));

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
        Map<K, EntryProcessorResult<T>> result       = new HashMap<K, EntryProcessorResult<T>>();

        Set                             internalKeys = new HashSet();

        if (proc == null)
            {
            throw new NullPointerException();
            }

        for (K key : setKey)
            {
            if (key == null)
                {
                throw new NullPointerException();
                }

            internalKeys.add(getKeyConverter().toInternal(key));
            }

        try
            {
            Map<Object, EntryProcessorResult<T>> internalResult =
                (Map<Object,
                     EntryProcessorResult<T>>) m_namedCache.invokeAll(internalKeys,
                                          new InvokeProcessor<K, V, T>(this, proc, arguments));

            result = new HashMap<K, EntryProcessorResult<T>>(internalResult.size());

            for (Map.Entry<Object, EntryProcessorResult<T>> internalEntry : internalResult.entrySet())
                {
                result.put(m_converterKey.fromInternal(internalEntry.getKey()), internalEntry.getValue());
                }
            }
        catch (Exception e)
            {
            throw handleException(e, EntryProcessorException.class);
            }

        return result == null ? new HashMap<K, EntryProcessorResult<T>>() : result;
        }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> config)
        {
        m_configuration.addCacheEntryListenerConfiguration(config);
        createAndAddListener(config);
        }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> config)
        {
        removeListener(config);
        }

    @Override
    public Iterator<Entry<K, V>> iterator()
        {
        ensureOpen();

        return new EntryIterator<K, V>(m_namedCache.entrySet().iterator(), this);
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

        if (m_setListenerRegistrationSynchronous.isEmpty())
            {
            if (m_listenerSynchronous != null)
                {
                m_namedCache.removeMapListener(m_listenerSynchronous, m_filterCreateUpdateRemove);
                m_listenerSynchronous = null;
                }
            }
        else if (m_listenerSynchronous == null)
            {
            m_listenerSynchronous =
                new LocalCacheSynchronousMapListener("coherence-jcache-adapter-localcache-synchronous-listener", this);
            m_namedCache.addMapListener(m_listenerSynchronous, m_filterCreateUpdateRemove, F_LITE);
            }

        if (m_setListenerRegistrationAsynchronous.isEmpty())
            {
            if (m_listenerAsynchronous != null)
                {
                m_namedCache.removeMapListener(m_listenerAsynchronous, m_filterCreateUpdateRemove);
                m_listenerAsynchronous = null;
                }
            }
        else if (m_listenerAsynchronous == null)
            {
            m_listenerAsynchronous =
                new LocalCacheAsynchronousMapListener("coherence-jcache-adapter-localcache-asynchronous-listener",
                    this);
            m_namedCache.addMapListener(m_listenerAsynchronous, m_filterCreateUpdateRemove, F_LITE);
            }
        }

    /**
     *  Update LocalCacheValue
     *
     *  @param ctx   JCache context
     *  @param cachedValue current LocalCacheValue
     *  @param internalValue updated value
     *  @param ldtNow        modification time
     *
     *  @return updated {@link LocalCacheValue}
     */
    private LocalCacheValue updateLocalCacheValue(JCacheContext ctx, LocalCacheValue cachedValue, Object internalValue,
        long ldtNow)
        {
        LocalCacheValue updatedCacheValue = new LocalCacheValue(cachedValue);

        return updatedCacheValue.updateInternalValue(internalValue, ldtNow, ctx.getExpiryPolicy());
        }

    /**
     * Gets the value for the specified key from the underlying cache, including
     * attempting to load it if a CacheLoader is configured (with read-through).
     * <p>
     * Any events that need to be raised are added to the specified dispatcher.
     *
     * @param key        the key of the entry to get from the cache
     * @param dispatcher the dispatcher for events
     * @return the value loaded
     *
     * @throws Exception
     */
    private V getValue(K key, CoherenceCacheEventEventDispatcher<K, V> dispatcher)
            throws Exception
        {
        long            ldtNow      = Helper.getCurrentTimeMillis();
        long            ldtStart    = isStatisticsEnabled() ? ldtNow : 0;
        Object          internalKey = m_converterKey.toInternal(key);
        V               value       = null;

        LocalCacheValue cachedValue = (LocalCacheValue) m_namedCache.get(internalKey);

        boolean         isExpired   = cachedValue != null && cachedValue.isExpiredAt(ldtNow);

        if (cachedValue == null || isExpired)
            {
            V expiredValue = isExpired ? m_converterValue.fromInternal(cachedValue.get()) : null;

            if (isExpired)
                {
                processExpiries(key, expiredValue, dispatcher);
                dispatch(dispatcher);
                }

            if (isStatisticsEnabled())
                {
                getStatistics().registerMisses(1, ldtStart);
                }

            if (m_configuration.isReadThrough() && m_ctx.getCacheLoader() != null)
                {
                try
                    {
                    value = (V) m_ctx.getCacheLoader().load(key);
                    }
                catch (Exception e)
                    {
                    if (!(e instanceof CacheLoaderException))
                        {
                        throw new CacheLoaderException("Exception in CacheLoader", e);
                        }
                    else
                        {
                        throw e;
                        }
                    }
                }

            if (value == null)
                {
                return null;
                }

            Object internalValue = m_converterValue.toInternal(value);

            cachedValue = LocalCacheValue.createLoadedLocalCacheValue(internalValue, ldtNow, m_ctx.getExpiryPolicy());

            if (cachedValue.isExpiredAt(ldtNow))
                {
                return null;
                }
            else
                {
                m_namedCache.put(internalKey, cachedValue);

                // do not consider a load as a put for cache statistics.
                // this is one place where JCache Statistics differ from Coherence Statistics on underlying named cache.
                }
            }
        else
            {
            value = m_converterValue.fromInternal(cachedValue.getInternalValue(ldtNow));
            cachedValue.accessInternalValue(ldtNow, m_ctx.getExpiryPolicy());

            // update that entry was accessed.
            m_namedCache.put(internalKey, cachedValue);

            if (isStatisticsEnabled())
                {
                m_ctx.getStatistics().registerHits(1, ldtStart);
                }
            }

        return value;
        }

    /**
     * Get the coherence named cache for this JCache. Note: that this is not the same as JCache provided cache name.
     *
     * @return internal Coherence named cache for this JCache.
     */
    private String getInternalCacheName()
        {
        return m_cacheId.getCanonicalCacheName();
        }

    /**
     * Requests a {@link java.util.concurrent.FutureTask} to be performed.
     *
     * @param task the {@link java.util.concurrent.FutureTask} to be performed
     */
    private void submit(Runnable task)
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
     * Put key and value into this JCache, performing write-through only if <code>fUserWriteThrough</code>
     * is <code>true</code>.
     *
     * @param key   JCache entry key
     * @param value  JCache entry value
     * @param fUseWriteThrough if true, perform write-through if it is configured
     */
    private void put(K key, V value, boolean fUseWriteThrough)
        {
        int putCount = 0;

        ensureOpen();

        if (value == null)
            {
            throw new NullPointerException("null value specified for key " + key);
            }

        CoherenceCacheEventEventDispatcher<K, V> dispatcher    = new CoherenceCacheEventEventDispatcher<K, V>();

        long                                     ldtNow        = Helper.getCurrentTimeMillis();
        long                                     start         = isStatisticsEnabled() ? ldtNow : 0;

        Object                                   internalKey   = m_converterKey.toInternal(key);
        Object                                   internalValue = m_converterValue.toInternal(value);

        LocalCacheValue                          cachedValue   = (LocalCacheValue) m_namedCache.get(internalKey);

        boolean isOldEntryExpired                              = cachedValue != null && cachedValue.isExpiredAt(ldtNow);

        if (isOldEntryExpired)
            {
            processExpiries(key, dispatcher);
            }

        if (cachedValue == null || isOldEntryExpired)
            {
            // creation

            if (fUseWriteThrough)
                {
                writeCacheEntry(new CoherenceCacheEntry<K, V>(key, value));
                }

            cachedValue = LocalCacheValue.createLocalCacheValue(internalValue, ldtNow, getContext().getExpiryPolicy());

            if (!cachedValue.isExpiredAt(ldtNow))
                {
                // check that new entry is not already expired, in which case it should
                // not be added to the cache or listeners called or writers called.
                m_namedCache.put(internalKey, cachedValue);
                putCount++;
                }

            }
        else
            {
            // update
            V                         oldValue = m_converterValue.fromInternal(cachedValue.get());
            CoherenceCacheEntry<K, V> entry    = new CoherenceCacheEntry<K, V>(key, value, oldValue);

            if (fUseWriteThrough)
                {
                writeCacheEntry(entry);
                }

            m_namedCache.put(internalKey, updateLocalCacheValue(m_ctx, cachedValue, internalValue, ldtNow));
            putCount++;
            }

        dispatch(dispatcher);

        if (isStatisticsEnabled() && putCount > 0)
            {
            m_ctx.getStatistics().registerPuts(putCount, Helper.getCurrentTimeMillis() - start);
            }
        }

    /**
     * Internal putIfAbsent with ability to disable write-through by setting parameter fUseWriteThrough to false.
     * Used to put read-through values into this JCache without writing them right back to external resource they
     * were just loaded from.
     *
     * @param key  entry key
     * @param value entry value
     * @param fUseWriteThrough only use write-through if configured and this is true.
     *
     * @return true if put succeeded due to key being absent from this JCache.
     */
    private boolean putIfAbsent(K key, V value, boolean fUseWriteThrough)
        {
        int     putCount  = 0;
        int     missCount = 0;
        int     hitCount  = 0;
        boolean result    = false;

        ensureOpen();

        if (value == null)
            {
            throw new NullPointerException("null value specified for key " + key);
            }

        if (key == null)
            {
            throw new NullPointerException("null key provided");
            }

        CoherenceCacheEventEventDispatcher<K, V> dispatcher    = new CoherenceCacheEventEventDispatcher<K, V>();

        long                                     ldtNow        = Helper.getCurrentTimeMillis();
        long                                     start         = isStatisticsEnabled() ? ldtNow : 0;

        Object                                   internalKey   = m_converterKey.toInternal(key);
        Object                                   internalValue = m_converterValue.toInternal(value);

        LocalCacheValue                          cachedValue   = (LocalCacheValue) m_namedCache.get(internalKey);

        boolean isOldEntryExpired                              = cachedValue != null && cachedValue.isExpiredAt(ldtNow);

        if (isOldEntryExpired)
            {
            processExpiries(key, dispatcher);
            }

        if (cachedValue == null || isOldEntryExpired)
            {
            if (fUseWriteThrough)
                {
                writeCacheEntry(new CoherenceCacheEntry<K, V>(key, value));
                }

            // check that new entry is not already expired, in which case it should
            // not be added to the cache or listeners called or writers called.
            cachedValue = LocalCacheValue.createLocalCacheValue(internalValue, ldtNow, getContext().getExpiryPolicy());

            if (cachedValue.isExpiredAt(ldtNow))
                {
                result = false;
                }
            else
                {
                result = true;
                m_namedCache.put(internalKey, cachedValue);
                putCount++;
                missCount++;
                }

            }
        else
            {
            result = false;
            hitCount++;
            }

        dispatch(dispatcher);

        if (isStatisticsEnabled())
            {
            if (putCount > 0)
                {
                m_ctx.getStatistics().registerPuts(putCount, start);
                m_ctx.getStatistics().registerMisses(missCount, start);
                }
            if (hitCount > 0)
                {
                m_ctx.getStatistics().registerHits(hitCount, start);
                }
            }

        return result;
        }

    /**
     * Register a {@link CacheEntryListenerConfiguration} to synchronous or asynchronous native Coherence map listeners
     * based on value of {@link javax.cache.configuration.CacheEntryListenerConfiguration#isSynchronous()}.
     *
     * @param configuration the {@link CacheEntryListenerConfiguration}
     */
    private void createAndAddListener(CacheEntryListenerConfiguration<K, V> configuration)
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
     * Remove a {@link CacheEntryListenerConfiguration} from this JCache
     *
     * @param configListener CacheEntryListenerConfiguration to remove
     */
    private void removeListener(CacheEntryListenerConfiguration<K, V> configListener)
        {
        if (configListener == null)
            {
            throw new NullPointerException("CacheEntryListenerConfiguration can't be null");
            }

        CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K, V>> setListeners =
            configListener.isSynchronous()
            ? m_setListenerRegistrationSynchronous : m_setListenerRegistrationAsynchronous;

        for (CoherenceCacheEntryListenerRegistration<K, V> listenerRegistration : setListeners)
            {
            if (configListener.equals(listenerRegistration.getConfiguration()))
                {
                setListeners.remove(listenerRegistration);
                m_configuration.getCacheEntryListenerConfigurations().remove(configListener);
                }
            }

        synchronizeCacheEntryListeners();
        }

    /**
     * Get configured CacheLoaderFactory.
     *
     * @return CacheLoaderFactory, if configured, or null if not configured
     */
    private Factory<CacheLoader<K, V>> getCacheLoaderFactory()
        {
        return m_configuration.isReadThrough() ? m_configuration.getCacheLoaderFactory() : null;
        }

    /**
     * Get configured CacheWriterFactory.
     *
     * @return CacheWriterFactory, if configured, or null if not configured
     */
    private Factory<CacheWriter<? super K, ? super V>> getCacheWriterFactory()
        {
        return m_configuration.isWriteThrough() ? m_configuration.getCacheWriterFactory() : null;
        }

    /**
     * Get one argument constructor for clz.
     *
     * Note: candidate for AccessControlBlock due to reflection.
     *
     * @param clz   class
     * @param <T>   the type
     *
     * @return constructor if it exists, or null otherwise.
     */
    private <T> Constructor<T> getClassCtor(Class<T> clz)
        {
        Class[]        argTypes = {Throwable.class};
        Constructor<T> result   = null;

        try
            {
            result = clz.getDeclaredConstructor(argTypes);
            }
        catch (NoSuchMethodException e)
            {
            }

        return result;
        }

    /**
     * general purpose runtime exception handler
     *
     * @param exception  current exception
     * @param clzException wrapper exception
     * @param <T>          wrapper exception type
     *
     * @return exception to throw
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
     * Writes the Cache Entry to the configured CacheWriter.  Does nothing if
     * write-through is not configured.
     *
     * @param entry the Cache Entry to write
     */
    private void writeCacheEntry(Entry<K, V> entry)
        {
        if (m_configuration.isWriteThrough())
            {
            try
                {
                m_ctx.getCacheWriter().write(entry);
                }
            catch (Exception e)
                {
                throw handleException(e, CacheWriterException.class);
                }
            }
        }

    /**
     * Deletes the Cache Entry using the configured CacheWriter.  Does nothing
     * if write-through is not configured.
     *
     * @param key key
     */
    private void deleteCacheEntry(K key)
        {
        if (m_configuration.isWriteThrough())
            {
            try
                {
                m_ctx.getCacheWriter().delete(key);
                }
            catch (Exception e)
                {
                throw handleException(e, CacheWriterException.class);
                }
            }
        }

    /**
     * PutAll entries in map into this cache.  If fReplaceExistingValues is false, then entries that are already in
     * cache will not be updated. Do not perform write-through if <code>fUseWriteThrough</code> is
     * <code>false</code></code>.
     *
     * @param map  map               entries to put into this cache
     * @param fReplaceExistingValues if false, do not update existing entries in cache with value from <code>map</code>
     * @param fUseWriteThrough       is writer-through enabled
     */
    private void putAll(Map<? extends K, ? extends V> map, boolean fReplaceExistingValues, boolean fUseWriteThrough)
        {
        ensureOpen();

        if (map == null)
            {
            throw new NullPointerException();
            }

        // TODO: implement as an entry processor. tck test PutTest#putAll_NullKey that had a null key in the middle of
        // real map data expected none to be put, hence
        // one pass to check for nulls and second pass to perform puts.
        // partitionedcache has putAllWithEntryProcessor(map, fReplaceExistingValues, fUseWriteThrough, false);

        // leave this. require it when implementing putAll as an entry processor.
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            {
            if (entry.getKey() == null || entry.getValue() == null)
                {
                throw new NullPointerException();
                }
            }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            {
            if (fReplaceExistingValues)
                {
                put(entry.getKey(), entry.getValue(), fUseWriteThrough);
                }
            else
                {
                putIfAbsent(entry.getKey(), entry.getValue(), fUseWriteThrough);
                }
            }
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
        JCacheContext.unregister(m_manager.getConfigurableCacheFactory().getResourceRegistry(), m_cacheId);

        m_manager.removeCacheToConfigurationMapping(m_cacheId);
        m_manager.getConfigurableCacheFactory().destroyCache(m_namedCache);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return m_sJCacheName;
        }

    // ----- inner class ----------------------------------------------------

    /**
     * JCache Entry iterator
     *
     * @param <K> key type
     * @param <V> value type
     */
    public static class EntryIterator<K, V>
            extends WrapperCollections.AbstractWrapperIterator
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an EntryIterator backed by the specified iterator.
         *
         *
         * @param iter the underlying iterator
         * @param cache cache to iterate over
         */
        public EntryIterator(Iterator<Map.Entry<K, V>> iter, LocalCache<K, V> cache)
            {
            super(iter);
            m_cache = cache;
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

                return new CoherenceCacheEntry<K, V>(m_cache.m_converterKey.fromInternal(m_entryLast.getKey()),
                                               m_cache.m_converterValue.fromInternal(
                                                   ((LocalCacheValue) m_entryLast.getValue()).getInternalValue(
                                                       Helper.getCurrentTimeMillis())));
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
                m_cache.remove(m_cache.m_converterKey.fromInternal(m_entryLast.getKey()));
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

                // check for lazy expiration and update last Accessed via call to m_cacheSource.get().
                K key   = m_cache.m_converterKey.fromInternal(entry.getKey());
                V value = null;

                try
                    {
                    value = m_cache.getValue(key, m_dispatcher);
                    }
                catch (Exception e)
                    {
                    }

                if (value != null)
                    {
                    m_entryNext = entry;
                    }
                }

            }

        // ----- data members -----------------------------------------------
        private final LocalCache<K, V>                   m_cache;
        private Map.Entry<K, V>                          m_entryLast  = null;
        private Map.Entry<K, V>                          m_entryNext  = null;
        private CoherenceCacheEventEventDispatcher<K, V> m_dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();
        }

    // ------ data members --------------------------------------------------

    /**
     * An {@link java.util.concurrent.ExecutorService} for the purposes of performing asynchronous
     * background work.
     */
    private final AtomicReference<ExecutorService> m_refExecutorService = new AtomicReference<ExecutorService>(null);

    /**
     * JCache cache name
     */
    private final CacheMXBean          m_mxbean;
    private final InternalConverter<K> m_converterKey;
    private final InternalConverter<V> m_converterValue;
    private MapListener                m_listenerAsynchronous;
    private MapListener                m_listenerSynchronous;
    private final Filter               m_filterCreateUpdateRemove;
    private AtomicBoolean              m_fStats = new AtomicBoolean();
    private final JCacheIdentifier     m_cacheId;
    private final JCacheContext        m_ctx;

    /**
     * The {@link javax.cache.configuration.CacheEntryListenerConfiguration}s for the {@link javax.cache.Cache}.
     */
    private final CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K, V>> m_setListenerRegistrationSynchronous;
    private final CopyOnWriteArraySet<CoherenceCacheEntryListenerRegistration<K, V>> m_setListenerRegistrationAsynchronous;
    }
