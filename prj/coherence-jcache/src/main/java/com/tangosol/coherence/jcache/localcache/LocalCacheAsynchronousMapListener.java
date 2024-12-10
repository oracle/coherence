/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache;

import com.tangosol.coherence.jcache.common.CoherenceCacheEntryEvent;
import com.tangosol.coherence.jcache.common.CoherenceCacheEntryListenerRegistration;
import com.tangosol.coherence.jcache.common.CoherenceCacheEventEventDispatcher;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.MapEvent;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;

/**
 * MapListener for Coherence-based JCache adapter implementation to generate JCache {@link CoherenceCacheEntryEvent}.
 *
 * @param <K>  the key type
 * @param <V>  the value type
 *
 * @author jf 2013.12.19
 * @version Coherence 12.1.3
 *
 */
public class LocalCacheAsynchronousMapListener<K, V>
        extends AbstractMapListener
    {
    // ------ constructors --------------------------------------------------

    /**
     * Constructs {@link LocalCacheAsynchronousMapListener}
     *
     *
     * @param sDescription description of handler
     * @param cache  source jcache
     */
    LocalCacheAsynchronousMapListener(String sDescription, LocalCache<K, V> cache)
        {
        f_sDescription = sDescription;
        m_cache        = cache;
        }

    // ------ AbstractMapListener methods -----------------------------------

    @Override
    public void entryInserted(MapEvent evt)
        {
        CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();
        CoherenceCacheEntryEvent<K, V> createdEntry = new CoherenceCacheEntryEvent<K, V>(m_cache, EventType.CREATED,
                                                          keyFromInternal(evt.getKey()),
                                                          valueFromInternal(evt.getNewValue()));

        dispatcher.addEvent(CacheEntryCreatedListener.class, createdEntry);
        dispatcher.dispatch(getEventListeners());
        }

    @Override
    public void entryUpdated(MapEvent evt)
        {
        CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();
        CoherenceCacheEntryEvent<K, V> updatedEntry = new CoherenceCacheEntryEvent<K, V>(m_cache, EventType.UPDATED,
                                                          keyFromInternal(evt.getKey()),
                                                          valueFromInternal(evt.getNewValue()),
                                                          valueFromInternal(evt.getOldValue()));

        dispatcher.addEvent(CacheEntryUpdatedListener.class, updatedEntry);
        dispatcher.dispatch(getEventListeners());
        }

    @Override
    public void entryDeleted(MapEvent evt)
        {
        CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();


        CoherenceCacheEntryEvent<K, V> deletedEntry = new CoherenceCacheEntryEvent<K, V>(m_cache, EventType.REMOVED,
                                                          keyFromInternal(evt.getKey()),
                                                          null,
                                                          valueFromInternal(evt.getOldValue()));

        dispatcher.addEvent(CacheEntryRemovedListener.class, deletedEntry);
        dispatcher.dispatch(getEventListeners());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Convert internalKey to external format
     *
     * @param internalKey key in internal format
     *
     * @return external format of internalKey
     */
    private K keyFromInternal(Object internalKey)
        {
        return (K) m_cache.getKeyConverter().fromInternal(internalKey);
        }

    /**
     * Convert internalValue to external format
     *
     * @param value internal LocalCacheValue
     *
     * @return external format of value, no accessed computation on LocalCacheValue
     */
    private V valueFromInternal(Object value)
        {
        LocalCacheValue cacheValue = (LocalCacheValue) value;

        // note: do not use getInternalValue(long ldtAccessed) since this should not count as an access to cached value
        // in StoreByReference set to true, there exist a chance that this is a reference to value in JCache entry.
        return (V) m_cache.getValueConverter().fromInternal(cacheValue.getInternalValue());
        }

    /**
     * Iterable over asynchronous CacheEntryListenerRegistrations
     *
     * @return Iterable over asynchronous cache entry event listener
     */
    protected Iterable<CoherenceCacheEntryListenerRegistration<K, V>> getEventListeners()
        {
        return m_cache.getRegisteredAsynchronousEventListeners();
        }

    @Override
    public String toString()
        {
        return this.getClass().getSimpleName() + " cacheName=" + (m_cache == null ? "" : m_cache.getName())
               + " description=" + f_sDescription;
        }

    // ----- inner class ----------------------------------------------------

    /**
     * Server side filter to filter out both coherence and jcache synthetic events.
     */
    public static class NonSyntheticEntryFilter
            extends com.tangosol.coherence.jcache.common.NonSyntheticEntryFilter
        {
        @Override
        public boolean isJCacheSynthetic(CacheEvent evt)
            {
            // GetProcessor generates a UPDATE when updating DECO_JCACHE with JCACHE meta info about entry.
            LocalCacheValue cacheValue = (LocalCacheValue) evt.getNewValue();

            return cacheValue.isSyntheticUpdate();
            }
        }

    // ------ data members --------------------------------------------------

    /**
     * Handler description.
     */
    protected final String f_sDescription;

    /**
     * Listener for this JCache.
     */
    protected final LocalCache<K, V> m_cache;
    }
