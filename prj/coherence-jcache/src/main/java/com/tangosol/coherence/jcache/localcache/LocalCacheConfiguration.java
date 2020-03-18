/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCompleteConfiguration;
import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;

import javax.cache.configuration.CompleteConfiguration;

/**
 * A {@link javax.cache.configuration.Configuration} for a
 * {@link javax.cache.Cache} based on a Coherence &lt;local-scheme&gt;,
 * or more specifically an in-process {@link com.tangosol.net.NamedCache}.
 *
 * @author bo  2013.10.23
 * @since Coherence 12.1.3
 *
 * @param <K>  the type of keys
 * @param <V>  the type of values
 */
public class LocalCacheConfiguration<K, V>
        extends AbstractCoherenceBasedCompleteConfiguration<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a default {@link LocalCacheConfiguration}.
     */
    public LocalCacheConfiguration()
        {
        super();
        }

    /**
     * Constructs a {@link LocalCacheConfiguration} based on a
     * {@link javax.cache.configuration.CompleteConfiguration}.
     *
     * @param configuration  the {@link javax.cache.configuration.CompleteConfiguration}
     */
    public LocalCacheConfiguration(CompleteConfiguration<K, V> configuration)
        {
        super(configuration);
        }

    // ----- CoherenceCacheConfiguration interface --------------------------

    @Override
    public CoherenceBasedCache<K, V> createCache(CoherenceBasedCacheManager manager, String sJCacheName)
            throws IllegalArgumentException
        {
        return new LocalCache<K, V>(manager, sJCacheName, this);
        }

    @Override
    public void destroyCache(CoherenceBasedCacheManager manager, String name)
        {
        // no meaningful implementation. only can be destroyed in CoherenceBasedCacheManager.destroyCache
        throw new UnsupportedOperationException("not implemented: should never be called");
        }

    }
