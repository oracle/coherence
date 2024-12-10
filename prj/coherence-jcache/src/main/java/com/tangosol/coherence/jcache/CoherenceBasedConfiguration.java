/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import javax.cache.configuration.Configuration;

/**
 * A {@link Configuration} for a {@link CoherenceBasedCache}.
 * <p>
 * {@link javax.cache.Cache}s produced by an implementation of this configuration
 * are not guaranteed to be compliant to the JCache specification.
 * <p>
 * Only configurations that additionally implement the appropriate JCache
 * configuration interface(s), namely
 * the {@link javax.cache.configuration.CompleteConfiguration} interface,
 * are guaranteed to be compliant.
 *
 * @author bo  2013.10.22
 * @since Coherence 12.1.3
 *
 * @see CoherenceBasedCompleteConfiguration
 *
 * @param <K>  the type of the {@link javax.cache.Cache} keys
 * @param <V>  the type of the {@link javax.cache.Cache} values
 */
public interface CoherenceBasedConfiguration<K, V>
        extends Configuration<K, V>
    {
    /**
     * Creates a {@link CoherenceBasedCache} based on the current state of
     * the {@link CoherenceBasedConfiguration}, to be owned by
     * the specified {@link CoherenceBasedCacheManager}.
     *
     * @param manager  the owning {@link CoherenceBasedCacheManager}
     * @param name     the name of the {@link javax.cache.Cache} to be configured
     *
     * @return a {@link CoherenceBasedCache}
     * @throws IllegalArgumentException  when a {@link javax.cache.Cache} can't be created
     *                                   based on the current {@link CoherenceBasedConfiguration}
     */
    public CoherenceBasedCache<K, V> createCache(CoherenceBasedCacheManager manager, String name)
            throws IllegalArgumentException;

    /**
     * Destroy a {@link CoherenceBasedCache} implementation.
     * @param manager  the owning {@link CoherenceBasedCacheManager}
     * @param name     the name of the {@link javax.cache.Cache} to be destroyed
     */
    public void destroyCache(CoherenceBasedCacheManager manager, String name);
    }
