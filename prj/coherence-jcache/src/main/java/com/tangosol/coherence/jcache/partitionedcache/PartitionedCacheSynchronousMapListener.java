/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache;

import com.tangosol.coherence.jcache.common.CoherenceCacheEntryListenerRegistration;

import com.tangosol.util.MapListenerSupport;

/**
 * Synchronous version of PartitionedCacheAsynchronousMapListener.
 *
 * @param <K> type of key
 * @param <V> type of value
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 */
public class PartitionedCacheSynchronousMapListener<K, V>
        extends PartitionedCacheAsynchronousMapListener<K, V>
        implements MapListenerSupport.SynchronousListener
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link PartitionedCacheSynchronousMapListener} to propagate CacheEntryEvents
     * from native Coherence Cache to JCache registered CacheEntyEvents.
     *
     *
     * @param sDescription description for MapListener
     * @param cache        JCache source of CacheEntryEvents
     */
    PartitionedCacheSynchronousMapListener(String sDescription, PartitionedCache cache)
        {
        super(sDescription, cache);
        }

    // ----- PartitionedCacheSynchronousMapListener methods -----------------

    /**
     * Iterate over synchronous JCache CacheEntry Listener Registrations.
     *
     * @return {@link Iterable} over {@link CoherenceCacheEntryListenerRegistration}.
     */
    protected Iterable<CoherenceCacheEntryListenerRegistration<K, V>> getEventListeners()
        {
        return m_cacheSource.getRegisteredSynchronousEventListeners();
        }
    }
