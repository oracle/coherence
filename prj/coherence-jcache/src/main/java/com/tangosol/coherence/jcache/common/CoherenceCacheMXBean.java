/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.coherence.jcache.CoherenceBasedCache;

import javax.cache.configuration.CompleteConfiguration;

import javax.cache.management.CacheMXBean;

/**
 * The Coherence implementation of a {@link CacheMXBean}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class CoherenceCacheMXBean<K, V>
        implements CacheMXBean
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CoherenceCacheMXBean}.
     *
     * @param cache  the {@link CoherenceBasedCache}
     */
    public CoherenceCacheMXBean(CoherenceBasedCache<K, V> cache)
        {
        m_cache       = cache;
        m_cfgComplete = cache.getConfiguration(CompleteConfiguration.class);
        }

    // ----- CacheMXBean interface ------------------------------------------

    @Override
    public String getKeyType()
        {
        return m_cfgComplete.getKeyType().getCanonicalName();
        }

    @Override
    public String getValueType()
        {
        return m_cfgComplete.getValueType().getCanonicalName();
        }

    @Override
    public boolean isReadThrough()
        {
        return m_cfgComplete.isReadThrough();
        }

    @Override
    public boolean isWriteThrough()
        {
        return m_cfgComplete.isWriteThrough();
        }

    @Override
    public boolean isStoreByValue()
        {
        return m_cfgComplete.isStoreByValue();
        }

    @Override
    public boolean isStatisticsEnabled()
        {
        return m_cfgComplete.isStatisticsEnabled();
        }

    @Override
    public boolean isManagementEnabled()
        {
        return m_cfgComplete.isManagementEnabled();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link CoherenceBasedCache}.
     */
    private final CoherenceBasedCache<K, V> m_cache;

    /**
     * The {@link CompleteConfiguration}.
     */
    private final CompleteConfiguration<K, V> m_cfgComplete;
    }
