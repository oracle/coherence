/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache.processors;

import com.tangosol.coherence.jcache.common.CoherenceCacheEventEventDispatcher;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheStatistics;
import com.tangosol.coherence.jcache.localcache.LocalCache;
import com.tangosol.coherence.jcache.localcache.LocalCacheValue;

import com.tangosol.util.processor.AbstractProcessor;

import javax.cache.Cache;

import javax.cache.integration.CacheWriter;

/**
 * Common operations used in more than one Coherence Entry Processor implementation.
 *
 * @param <K> key type
 * @param <V> value type
 *
 * @version Coherence 12.1.3
 * @author jf 2013.12.19
 */
public abstract class AbstractEntryProcessor<K, V, T>
        extends AbstractProcessor<K, V, T>

    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link AbstractEntryProcessor} with {@link LocalCache} context.
     *
     *
     * @param cache source JCache of EntryProcessor
     */
    AbstractEntryProcessor(LocalCache cache)
        {
        // TODO:  would like to stop passing the LocalCache to LocalCacheEntryProcessors in future.
        // We were going to pass in JCacheContext instead.  However, to implement CacheEvent.EXPIRY
        // anytime the EntryProcessor detects an expired Entry, we manually dispatch to all CacheEventListeners
        // registered to the LocalCache.
        // Since the CacheEventListeners are dynamic and are allowed to be added
        // over time, the CacheEventListeners belong on LocalCache and not in JCacheContext.
        m_cache = cache;
        }

    // ----- AbstractEntryProcessor methods ---------------------------------

    /**
     * get cache statistics
     *
     * @return JCache statistics
     */
    public JCacheStatistics getJCacheStatistics()
        {
        return getContext().getStatistics();
        }

    /**
     * Get JCache context for entry processor operations.
     *
     * @return {@link JCacheContext} for this EntryProcessor context.
     */
    public JCacheContext getContext()
        {
        return getCache().getContext();
        }

    /**
     * update localCacheValue with internalValue with updateTime
     *
     * @param cachedValue    value with JCache meta info
     * @param internalValue  internal form of value to be stored in cachedValue
     * @param ldtUpdate      update time in milliseconds
     *
     * @return  {@link LocalCacheValue} with updated JCache MetaInfo
     */
    public LocalCacheValue updateLocalCacheValue(LocalCacheValue cachedValue, Object internalValue, long ldtUpdate)
        {
        LocalCacheValue updatedCacheValue = new LocalCacheValue(cachedValue);

        return  updatedCacheValue.updateInternalValue(internalValue, ldtUpdate, getContext().getExpiryPolicy());
        }

    /**
     * update JCache metainfo with access to value
     *
     * @param cachedValue  value with JCache meta info
     * @param ldtAccess    access time in milliseconds
     *
     * @return {@link LocalCacheValue} with updated access expiry
     */
    public LocalCacheValue accessLocalCacheValue(LocalCacheValue cachedValue, long ldtAccess)
        {
        cachedValue.accessInternalValue(ldtAccess, m_cache.getContext().getExpiryPolicy());

        return cachedValue;
        }

    /**
     * Writes the Cache Entry to the configured CacheWriter.  Does nothing if
     * write-through is not configured.
     *
     * @param entry external key value pair to write
     */
    protected void writeCacheEntry(Cache.Entry<K, V> entry)
        {
        CacheWriter cacheWriter = getContext().getCacheWriter();

        if (getContext().getConfiguration().isWriteThrough() && cacheWriter != null)
            {
            cacheWriter.write(entry);
            }
        }

    /**
     * Deletes the Cache Entry using the configued CacheWriter.  Does nothing
     * if write-through is not configued.
     *
     * @param key external form of key
     */
    protected void deleteCacheEntry(K key)
        {
        CacheWriter cacheWriter = getContext().getCacheWriter();

        if (getContext().getConfiguration().isWriteThrough() && cacheWriter != null)
            {
            cacheWriter.delete(key);
            }
        }

    /**
     * Convert key from internal to external format.
     *
     * @param internalObject key in internal format
     *
     * @return key in external format
     */
    protected K fromInternalKey(Object internalObject)
        {
        return (K) m_cache.getKeyConverter().fromInternal(internalObject);
        }

    /**
     * Covert value from internal to external format
     *
     * @param internalObject value in internal format
     *
     * @return value in external format
     */
    protected V fromInternalValue(Object internalObject)
        {
        return (V) m_cache.getValueConverter().fromInternal(internalObject);
        }

    /**
     * Convert external representation of key to internal format
     *
     * @param externalKey key in external format
     *
     * @return key in internal format
     */
    protected Object toInternalKey(K externalKey)
        {
        return m_cache.getKeyConverter().toInternal(externalKey);
        }

    /**
     * Convert external representation of value to internal format
     *
     * @param externalValue  value in external form
     *
     * @return internal representation of value
     */
    protected Object toInternalValue(V externalValue)
        {
        return m_cache.getValueConverter().toInternal(externalValue);
        }

    /**
     * State of cache statistics
     *
     * @return true if recording cache statistics is enabled
     */
    protected boolean isStatisticsEnabled()
        {
        return m_cache.isStatisticsEnabled();
        }

    /**
     * Get cache
     *
     * @return cache
     */
    protected LocalCache getCache()
        {
        return m_cache;
        }

    /**
     * Dispatch an ExpiryEvent for the entry of internalKey
     *
     * @param internalKey internal format of a key of an entry that has expired.
     */
    protected void processExpiries(Object internalKey)
        {
        CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();

        m_cache.processExpiries(m_cache.getKeyConverter().fromInternal(internalKey), dispatcher);
        m_cache.dispatch(dispatcher);
        }

    // ----- data members ---------------------------------------------------
    private LocalCache m_cache;
    }
