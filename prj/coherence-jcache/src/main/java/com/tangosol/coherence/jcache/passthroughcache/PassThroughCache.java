/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.passthroughcache;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;
import com.tangosol.coherence.jcache.common.MapEntryIteratorToCacheEntryIteratorAdapter;
import com.tangosol.coherence.jcache.passthroughcache.processors.GetAndReplaceProcessor;
import com.tangosol.coherence.jcache.passthroughcache.processors.InvokeProcessor;
import com.tangosol.coherence.jcache.passthroughcache.processors.ReplaceIfExistsProcessor;
import com.tangosol.coherence.jcache.passthroughcache.processors.ReplaceWithProcessor;

import com.tangosol.net.NamedCache;

import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalRemove;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import javax.cache.Cache;

import javax.cache.configuration.CacheEntryListenerConfiguration;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

import javax.cache.integration.CompletionListener;

import javax.cache.management.CacheMXBean;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

/**
 * A {@link Cache} that delegates (ie: passed-through) requests directly
 * onto an existing Coherence {@link NamedCache}.
 *
 * @param <K>  the type of the {@link Cache} keys
 * @param <V>  the type of the {@link Cache} values
 *
 * @author bo  2013.10.23
 * @since Coherence 12.1.3
 */
public class PassThroughCache<K, V>
        extends AbstractCoherenceBasedCache<K, V, PassThroughCacheConfiguration<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PassThroughCache}
     *
     * @param manager        the {@link com.tangosol.coherence.jcache.CoherenceBasedCacheManager} that owns the {@link Cache}
     * @param sJCacheName    the name of the JCache {@link Cache}
     * @param configuration  the {@link PassThroughCacheConfiguration} for the {@link Cache}
     */
    public PassThroughCache(CoherenceBasedCacheManager manager, String sJCacheName,
                            PassThroughCacheConfiguration<K, V> configuration)
        {
        super(manager, sJCacheName, new PassThroughCacheConfiguration<K, V>(configuration));

        m_mapListenerRegistrations = new ConcurrentHashMap<CacheEntryListenerConfiguration<? super K, ? super V>,
            PassThroughListenerRegistration<K, V>>();

        // determine the name of the underlying NamedCache
        String sNamedCacheName = m_configuration.getNamedCacheName() == null
                                 ? sJCacheName : m_configuration.getNamedCacheName();

        // determine the underlying NamedCache
        m_namedCache = manager.getConfigurableCacheFactory().ensureCache(sNamedCacheName, manager.getClassLoader());

        // TODO: register all of the cache listeners specified in the configuration
        }

    // ----- Cache interface ------------------------------------------------

    @Override
    public V get(K key)
        {
        ensureOpen();

        return (V) m_namedCache.get(key);
        }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys)
        {
        ensureOpen();

        return m_namedCache.getAll(keys);
        }

    @Override
    public boolean containsKey(K key)
        {
        ensureOpen();

        return m_namedCache.containsKey(key);
        }

    @Override
    public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener)
        {
        throw new UnsupportedOperationException("loadAll not supported by " + this.getClass().getName());
        }

    @Override
    public void put(K key, V value)
        {
        ensureOpen();

        m_namedCache.putAll(Collections.singletonMap(key, value));
        }

    @Override
    public V getAndPut(K key, V value)
        {
        ensureOpen();

        return (V) m_namedCache.put(key, value);
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        ensureOpen();

        m_namedCache.putAll(map);
        }

    @Override
    public boolean putIfAbsent(K key, V value)
        {
        ensureOpen();

        return m_namedCache.invoke(key, new ConditionalPut(new NotFilter(PresentFilter.INSTANCE), value, true)) == null;
        }

    @Override
    public boolean remove(K key)
        {
        ensureOpen();

        return m_namedCache.remove(key) != null;
        }

    @Override
    public boolean remove(K key, V oldValue)
        {
        ensureOpen();

        return m_namedCache.invoke(key,
                                   new ConditionalRemove(new EqualsFilter(IdentityExtractor.INSTANCE, oldValue),
                                       true)) == null;
        }

    @Override
    public V getAndRemove(K key)
        {
        ensureOpen();

        return (V) m_namedCache.remove(key);
        }

    @Override
    public boolean replace(K key, V oldValue, V newValue)
        {
        ensureOpen();

        return (Boolean) m_namedCache.invoke(key, new ReplaceWithProcessor<K, V>(oldValue, newValue));
        }

    @Override
    public boolean replace(K key, V value)
        {
        ensureOpen();

        return (Boolean) m_namedCache.invoke(key, new ReplaceIfExistsProcessor<K, V>(value));
        }

    @Override
    public V getAndReplace(K key, V value)
        {
        ensureOpen();

        return (V) m_namedCache.invoke(key, new GetAndReplaceProcessor<K, V>(value));
        }

    @Override
    public void removeAll(Set<? extends K> keys)
        {
        ensureOpen();

        m_namedCache.invokeAll(keys, new ConditionalRemove(PresentFilter.INSTANCE, false));
        }

    @Override
    public void removeAll()
        {
        ensureOpen();

        m_namedCache.clear();
        }

    @Override
    public void clear()
        {
        ensureOpen();

        // NOTE: This is definitely non-compliant (as we will raise events to listeners)
        m_namedCache.clear();
        }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
            throws EntryProcessorException
        {
        ensureOpen();

        EntryProcessorResult<T> result;

        try
            {
            result = (EntryProcessorResult<T>) m_namedCache.invoke(key,
                new InvokeProcessor<K, V, T>(entryProcessor, arguments));

            return result == null ? null : result.get();
            }
        catch (EntryProcessorException e)
            {
            throw e;
            }
        catch (Exception e)
            {
            throw new EntryProcessorException(e);
            }
        }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor,
        Object... arguments)
        {
        ensureOpen();

        return (Map<K, EntryProcessorResult<T>>) m_namedCache.invokeAll(keys,
            new InvokeProcessor<K, V, T>(entryProcessor, arguments));
        }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> configuration)
        {
        ensureOpen();

        if (!m_mapListenerRegistrations.containsKey(configuration))
            {
            CacheEntryListener<? super K, ? super V> cacheEntryListener =
                configuration.getCacheEntryListenerFactory().create();

            // establish a suitable filter for the listener
            int nMask = 0;

            if (cacheEntryListener instanceof CacheEntryCreatedListener)
                {
                nMask = nMask | MapEventFilter.E_INSERTED;
                }

            if (cacheEntryListener instanceof CacheEntryUpdatedListener)
                {
                nMask = nMask | MapEventFilter.E_UPDATED;
                }

            if (cacheEntryListener instanceof CacheEntryRemovedListener)
                {
                nMask = nMask | MapEventFilter.E_DELETED;
                }

            MapEventFilter filter = configuration.getCacheEntryEventFilterFactory() == null
                                    ? (nMask == MapEventFilter.E_ALL ? null : new MapEventFilter(nMask))
                                    : new MapEventFilter(nMask,
                                        new PassThroughFilterAdapter<K,
                                            V>(configuration.getCacheEntryEventFilterFactory()));

            MapListener listener = new PassThroughMapListenerAdapter<K, V>(this, cacheEntryListener);

            if (configuration.isSynchronous())
                {
                listener = new MapListenerSupport.WrapperSynchronousListener(listener);
                }

            PassThroughListenerRegistration registration = new PassThroughListenerRegistration(configuration, listener,
                                                               filter);

            if (m_mapListenerRegistrations.putIfAbsent(configuration, registration) == null)
                {
                m_namedCache.addMapListener(listener, filter, !configuration.isOldValueRequired());
                }
            }
        }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> configuration)
        {
        ensureOpen();

        PassThroughListenerRegistration<K, V> registration = m_mapListenerRegistrations.remove(configuration);

        if (registration != null)
            {
            MapListener    listener = registration.getMapListener();
            MapEventFilter filter   = registration.getMapEventFilter();

            m_namedCache.removeMapListener(listener, filter);
            }
        }

    @Override
    public Iterator<Entry<K, V>> iterator()
        {
        ensureOpen();

        return new MapEntryIteratorToCacheEntryIteratorAdapter<K, V>(m_namedCache.entrySet().iterator());
        }

    // ----- CoherenceBasedCache interface ----------------------------------

    @Override
    public void destroy()
        {
        throw new UnsupportedOperationException("PassThroughCaches can't be destroyed.  They may only be closed.");
        }

    // ----- AbstractCoherenceBasedCache methods ---------------------------------

    @Override
    public void onBeforeClosing()
        {
        // release listeners from the underlying NamedCache
        for (PassThroughListenerRegistration<K, V> registration : m_mapListenerRegistrations.values())
            {
            MapListener    listener = registration.getMapListener();
            MapEventFilter filter   = registration.getMapEventFilter();

            if (filter == null)
                {
                m_namedCache.removeMapListener(listener);
                }
            else
                {
                m_namedCache.removeMapListener(listener, filter);
                }
            }
        }

    @Override
    public CacheMXBean getMBean()
        {
        throw new UnsupportedOperationException("not implemented");
        }

    @Override
    public JCacheStatistics getStatistics()
        {
        throw new UnsupportedOperationException("not implemented");
        }

    @Override
    public void setManagementEnabled(boolean fEnabled)
        {
        throw new UnsupportedOperationException("not implemented");
        }

    @Override
    public void setStatisticsEnabled(boolean fEnabled)
        {
        throw new UnsupportedOperationException("not implemented");
        }

    @Override
    public boolean isStatisticsEnabled()
        {
        throw new UnsupportedOperationException("not implemented");
        }

    @Override
    public JCacheIdentifier getIdentifier()
        {
        throw new UnsupportedOperationException("not implemented");
        }

    // ------ data members --------------------------------------------------

    /**
     * The {@link PassThroughListenerRegistration}s by {@link CacheEntryListenerConfiguration}.
     */
    private ConcurrentHashMap<CacheEntryListenerConfiguration<? super K, ? super V>, PassThroughListenerRegistration<K, V>> m_mapListenerRegistrations;
    }
