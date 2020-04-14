/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.remotecache;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCompleteConfiguration;
import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.partitionedcache.PartitionedCache;

import javax.cache.configuration.CompleteConfiguration;

/**
 * A {@link javax.cache.configuration.Configuration} for a
 * {@link javax.cache.Cache} based on a Coherence &lt;remote-scheme&gt;,
 * or more specifically a {@link com.tangosol.net.NamedCache}.
 *
 * @author jf  2014.05.22
 * @since Coherence 12.2.1
 *
 * @param <K>  the type of keys
 * @param <V>  the type of values
 */
public class RemoteCacheConfiguration<K, V>
        extends AbstractCoherenceBasedCompleteConfiguration<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a default {@link com.tangosol.coherence.jcache.remotecache.RemoteCacheConfiguration}.
     *
     * @throws Exception  if an error occurs
     */
    public RemoteCacheConfiguration()
            throws Exception
        {
        super();
        }

    /**
     * Constructs a {@link com.tangosol.coherence.jcache.remotecache.RemoteCacheConfiguration} based on a
     * {@link javax.cache.configuration.CompleteConfiguration}.
     *
     * @param configuration the {@link javax.cache.configuration.CompleteConfiguration}
     */
    public RemoteCacheConfiguration(CompleteConfiguration<K, V> configuration)
        {
        super(configuration);
        }

    // ----- RemoteCacheConfiguration interface ------------------------

    /**
     * Validates the {@link RemoteCacheConfiguration}.
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
        return new RemoteCache<K, V>(manager, sJCacheName, this);
        }

    @Override
    public void destroyCache(CoherenceBasedCacheManager manager, String sJcacheName)
        {
        PartitionedCache.destroyCache(manager, new JCacheIdentifier(manager.getURI().toString(), sJcacheName));
        }
    }
