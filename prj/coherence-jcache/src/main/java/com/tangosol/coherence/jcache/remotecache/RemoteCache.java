/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.remotecache;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;
import com.tangosol.coherence.jcache.partitionedcache.PartitionedCache;
import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration;

import com.tangosol.net.NamedCache;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;

import javax.cache.integration.CompletionListener;

import javax.cache.management.CacheMXBean;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

/**
 * A remote partitioned-cache implementation of a {@link javax.cache.Cache} based
 * on a Coherence &lt;partitioned-cache&gt;.
 *
 * @param <K>  key type
 * @param <V>  value type
 *
 * @author jf  2014.05.21
 * @since  12.2.1
 */
public class RemoteCache<K, V>
        extends AbstractCoherenceBasedCache<K, V, RemoteCacheConfiguration<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link com.tangosol.coherence.jcache.remotecache.RemoteCache} configured by {@link com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration}
     * in context of {@link com.tangosol.coherence.jcache.CoherenceBasedCacheManager} with JCache name <code>sJCacheName</code>.
     *
     * @param manager       CacheManager manages the created RemoteCache.
     * @param sJCacheName   JCache cache name that must be unique within {@link com.tangosol.coherence.jcache.CoherenceBasedCacheManager} context
     * @param configuration remote JCache cache configuration
     */
    public RemoteCache(CoherenceBasedCacheManager manager, String sJCacheName, RemoteCacheConfiguration configuration)
        {
        super(manager, sJCacheName, new RemoteCacheConfiguration<K, V>(configuration));

        // delegate to a PartitionedCache since that is all that is supported for now.
        // Potentially, could support FederatedCache in future.
        m_cache = new PartitionedCache<K, V>(manager, sJCacheName,
                                       new PartitionedCacheConfiguration<K, V>(configuration));
        }

    @Override
    public void onBeforeClosing()
        {
        m_cache.onBeforeClosing();
        }

    @Override
    public CacheMXBean getMBean()
        {
        return m_cache.getMBean();
        }

    @Override
    public JCacheStatistics getStatistics()
        {
        return m_cache.getStatistics();
        }

    @Override
    public void setManagementEnabled(boolean fEnabled)
        {
        m_cache.setManagementEnabled(fEnabled);
        }

    @Override
    public void setStatisticsEnabled(boolean fEnabled)
        {
        m_cache.setStatisticsEnabled(fEnabled);
        }

    @Override
    public boolean isStatisticsEnabled()
        {
        return m_cache.isStatisticsEnabled();
        }

    @Override
    public JCacheIdentifier getIdentifier()
        {
        return m_cache.getIdentifier();
        }

    @Override
    public void destroy()
        {
        m_cache.destroy();
        }

    @Override
    public V get(K key)
        {
        return m_cache.get(key);
        }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys)
        {
        return m_cache.getAll(keys);
        }

    @Override
    public boolean containsKey(K key)
        {
        return m_cache.containsKey(key);
        }

    @Override
    public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener)
        {
        m_cache.loadAll(keys, replaceExistingValues, completionListener);
        }

    @Override
    public void put(K key, V value)
        {
        m_cache.put(key, value);
        }

    @Override
    public V getAndPut(K key, V value)
        {
        return m_cache.getAndPut(key, value);
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        m_cache.putAll(map);
        }

    @Override
    public boolean putIfAbsent(K key, V value)
        {
        return m_cache.putIfAbsent(key, value);
        }

    @Override
    public boolean remove(K key)
        {
        return m_cache.remove(key);
        }

    @Override
    public boolean remove(K key, V oldValue)
        {
        return m_cache.remove(key, oldValue);
        }

    @Override
    public V getAndRemove(K key)
        {
        return m_cache.getAndRemove(key);
        }

    @Override
    public boolean replace(K key, V oldValue, V newValue)
        {
        return m_cache.replace(key, oldValue, newValue);
        }

    @Override
    public boolean replace(K key, V value)
        {
        return m_cache.replace(key, value);
        }

    @Override
    public V getAndReplace(K key, V value)
        {
        return m_cache.getAndReplace(key, value);
        }

    @Override
    public void removeAll(Set<? extends K> keys)
        {
        m_cache.removeAll(keys);
        }

    @Override
    public void removeAll()
        {
        m_cache.removeAll();
        }

    @Override
    public void clear()
        {
        m_cache.clear();
        }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
            throws EntryProcessorException
        {
        return m_cache.invoke(key, entryProcessor, arguments);
        }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor,
        Object... arguments)
        {
        return m_cache.invokeAll(keys, entryProcessor, arguments);
        }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration)
        {
        m_cache.registerCacheEntryListener(cacheEntryListenerConfiguration);
        }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration)
        {
        m_cache.deregisterCacheEntryListener(cacheEntryListenerConfiguration);
        }

    @Override
    public Iterator<Entry<K, V>> iterator()
        {
        return m_cache.iterator();
        }

    @Override
    public void close()
        {
        super.close();
        m_cache.close();
        }

    @Override
    public <T extends Configuration<K, V>> T getConfiguration(Class<T> clz)
        {
        return m_cache.getConfiguration(clz);
        }

    @Override
    public <T> T unwrap(Class<T> clz)
        {
        return m_cache.unwrap(clz);
        }

    // ----- data members ---------------------------------------------------
    private PartitionedCache<K, V> m_cache;
    }
