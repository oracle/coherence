/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.passthroughcache;

import com.tangosol.util.MapListener;
import com.tangosol.util.filter.MapEventFilter;

import javax.cache.configuration.CacheEntryListenerConfiguration;

/**
 * Captures the Coherence-based {@link MapListener} and {@link MapEventFilter}
 * instances used for a {@link javax.cache.configuration.CacheEntryListenerConfiguration}
 *
 * @param <K>  the type of the {@link javax.cache.Cache} keys
 * @param <V>  the type of the {@link javax.cache.Cache} values
 *
 * @author bo  2013.11.04
 * @since Coherence 12.1.3
 */
public class PassThroughListenerRegistration<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PassThroughListenerRegistration}.
     *
     * @param configuration  the {@link CacheEntryListenerConfiguration}
     * @param listener       the {@link MapListener}
     * @param filter         the {@link MapEventFilter}
     */
    public PassThroughListenerRegistration(CacheEntryListenerConfiguration<K, V> configuration, MapListener listener,
        MapEventFilter filter)
        {
        m_configuration = configuration;
        m_listener      = listener;
        m_filter        = filter;
        }

    // ----- PassThroughListenerRegistration methods ------------------------

    /**
     * Obtains the {@link CacheEntryListenerConfiguration} for the
     * registration.
     *
     * @return the {@link CacheEntryListenerConfiguration}
     */
    public CacheEntryListenerConfiguration<K, V> getCacheEntryListenerConfiguration()
        {
        return m_configuration;
        }

    /**
     * Obtains the {@link MapListener} for the registration.
     *
     * @return the {@link MapListener}
     */
    public MapListener getMapListener()
        {
        return m_listener;
        }

    /**
     * Obtains the {@link MapEventFilter} for the registration.
     * (<code>null</code> if no filtering required).
     *
     * @return the {@link MapEventFilter}
     */
    public MapEventFilter getMapEventFilter()
        {
        return m_filter;
        }

    // ------ data members --------------------------------------------------

    /**
     * The {@link CacheEntryListenerConfiguration} for which a native
     * {@link MapListener} and {@link MapEventFilter} have been created.
     */
    private CacheEntryListenerConfiguration<K, V> m_configuration;

    /**
     * The {@link MapListener} registered for the {@link CacheEntryListenerConfiguration}.
     */
    private MapListener m_listener;

    /**
     * The {@link MapEventFilter} registered for the {@link CacheEntryListenerConfiguration}.
     */
    private MapEventFilter m_filter;
    }
