/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCompleteConfiguration;
import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import javax.cache.configuration.CompleteConfiguration;

/**
 * A {@link javax.cache.configuration.Configuration} for a
 * {@link javax.cache.Cache} based on a Coherence &lt;distributed-scheme&gt;
 * or more specifically a partitioned (aka: distributed)
 * {@link com.tangosol.net.NamedCache}.
 *
 * @param <K>  the type of the keys
 * @param <V>  the type of the values
 *
 * @author bo  2013.10.23
 * @since Coherence 12.1.3
 */
public class PartitionedCacheConfiguration<K, V>
        extends AbstractCoherenceBasedCompleteConfiguration<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a default {@link PartitionedCacheConfiguration}.
     */
    public PartitionedCacheConfiguration()
        {
        super();
        }

    /**
     * Constructs a {@link PartitionedCacheConfiguration} based on a
     * {@link CompleteConfiguration}.
     *
     * @param configuration  the {@link CompleteConfiguration}
     */
    public PartitionedCacheConfiguration(CompleteConfiguration<K, V> configuration)
        {
        super(configuration);

        validate();
        }

    // ----- PartitionedCacheConfiguration interface ------------------------

    /**
     * Validates the {@link PartitionedCacheConfiguration}.
     */
    protected void validate()
        {
        if (!isStoreByValue())
            {
            throw new UnsupportedOperationException("Store by reference is not supported");
            }
        }

    // ----- CoherenceCacheConfiguration interface --------------------------

    @Override
    public CoherenceBasedCache<K, V> createCache(CoherenceBasedCacheManager manager, String sJCacheName)
            throws IllegalArgumentException
        {
        return new PartitionedCache<K, V>(manager, sJCacheName, this);
        }

    @Override
    public void destroyCache(CoherenceBasedCacheManager manager, String sJcacheName)
        {
        PartitionedCache.destroyCache(manager, new JCacheIdentifier(manager.getURI().toString(), sJcacheName));
        }
    }
