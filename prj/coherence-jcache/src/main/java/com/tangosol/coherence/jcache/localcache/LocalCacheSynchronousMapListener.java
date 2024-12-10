/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache;

import com.tangosol.coherence.jcache.common.CoherenceCacheEntryListenerRegistration;

import com.tangosol.util.MapListenerSupport;

/**
 * Synchronous version of LocalCacheAsynchronousMapListener.
 *
 * @param <K>  key type
 * @param <V>  value type
 *
 * @author bo  2013.12.19
 * @since Coherence 12.1.3
 */
public class LocalCacheSynchronousMapListener<K, V>
        extends LocalCacheAsynchronousMapListener<K, V>
        implements MapListenerSupport.SynchronousListener
    {
    // ------ constructors --------------------------------------------------

    LocalCacheSynchronousMapListener(String sDescription, LocalCache cache)
        {
        super(sDescription, cache);
        }

    // ----- LocalCacheSynchronousMapListener methods -----------------------

    @Override
    protected Iterable<CoherenceCacheEntryListenerRegistration<K, V>> getEventListeners()
        {
        return m_cache.getRegisteredSynchronousEventListeners();
        }
    }
