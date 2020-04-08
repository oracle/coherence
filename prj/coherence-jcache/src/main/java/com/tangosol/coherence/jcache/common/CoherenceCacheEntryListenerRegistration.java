/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import java.util.concurrent.atomic.AtomicReference;

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;

import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;

/**
 * A runtime representation of a {@link CacheEntryListener} registration, including
 * its {@link javax.cache.configuration.CacheEntryListenerConfiguration}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class CoherenceCacheEntryListenerRegistration<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CoherenceCacheEntryListenerRegistration} based on
     * a {@link CacheEntryListenerConfiguration}.
     *
     * @param configuration  the {@link CacheEntryListenerConfiguration}
     */
    public CoherenceCacheEntryListenerRegistration(CacheEntryListenerConfiguration<K, V> configuration)
        {
        m_configuration = configuration;
        }

    // ----- CacheEntryListenerRegistration methods -------------------------

    /**
     * Obtains an instance of the {@link CacheEntryEventFilter} for the
     * registration.  If no instance has been previously instantiated,
     * one is created.
     *
     * @return  the {@link CacheEntryEventFilter} for the registration
     */
    public CacheEntryEventFilter<? super K, ? super V> getCacheEntryFilter()
        {
        Factory<CacheEntryEventFilter<? super K, ? super V>> factoryEventFilter =
            m_configuration.getCacheEntryEventFilterFactory();

        if (m_refFilter.get() == null && factoryEventFilter != null)
            {
            m_refFilter.compareAndSet(null, factoryEventFilter.create());
            }

        return m_refFilter.get();
        }

    /**
     * Obtains an instance of the {@link CacheEntryListener} for the
     * registration.  If no instance has been previously instantiated,
     * one is created.
     *
     * @return  the {@link CacheEntryListener} for the registration
     */
    public CacheEntryListener<? super K, ? super V> getCacheEntryListener()
        {
        Factory<CacheEntryListener<? super K, ? super V>> factoryEventListener =
            m_configuration.getCacheEntryListenerFactory();

        if (m_refListener.get() == null && factoryEventListener != null)
            {
            m_refListener.compareAndSet(null, factoryEventListener.create());
            }

        return m_refListener.get();
        }

    /**
     * Obtains the underlying {@link CacheEntryListenerConfiguration} used to
     * create the registration
     *
     * @return the {@link CacheEntryListenerConfiguration}
     */
    public CacheEntryListenerConfiguration<K, V> getConfiguration()
        {
        return m_configuration;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object object)
        {
        if (this == object)
            {
            return true;
            }

        if (object == null)
            {
            return false;
            }

        if (!(object instanceof CoherenceCacheEntryListenerRegistration))
            {
            return false;
            }

        CoherenceCacheEntryListenerRegistration<?, ?> other = (CoherenceCacheEntryListenerRegistration<?, ?>) object;

        if (getCacheEntryFilter() == null)
            {
            if (other.getCacheEntryFilter() != null)
                {
                return false;
                }
            }
        else if (!getCacheEntryFilter().equals(other.getCacheEntryFilter()))
            {
            return false;
            }

        if (m_configuration.isOldValueRequired() != other.getConfiguration().isOldValueRequired())
            {
            return false;
            }

        if (m_configuration.isSynchronous() != other.getConfiguration().isSynchronous())
            {
            return false;
            }

        if (getCacheEntryListener() == null)
            {
            if (other.getCacheEntryListener() != null)
                {
                return false;
                }
            }
        else if (!getCacheEntryListener().equals(other.getCacheEntryListener()))
            {
            return false;
            }

        return true;
        }

    @Override
    public int hashCode()
        {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + ((getCacheEntryFilter() == null) ? 0 : getCacheEntryFilter().hashCode());
        result = prime * result + (m_configuration.isOldValueRequired() ? 1231 : 1237);
        result = prime * result + (m_configuration.isSynchronous() ? 1231 : 1237);
        result = prime * result + ((getCacheEntryListener() == null) ? 0 : getCacheEntryListener().hashCode());

        return result;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link CacheEntryListenerConfiguration} for the {@link CoherenceCacheEntryListenerRegistration}.
     */
    private final CacheEntryListenerConfiguration<K, V> m_configuration;

    /**
     * The {@link CacheEntryListener}.
     */
    private final AtomicReference<CacheEntryListener<? super K, ? super V>> m_refListener =
        new AtomicReference<CacheEntryListener<? super K, ? super V>>();

    /**
     * The {@link CacheEntryEventFilter}.
     */
    private final AtomicReference<CacheEntryEventFilter<? super K, ? super V>> m_refFilter =
        new AtomicReference<CacheEntryEventFilter<? super K, ? super V>>();
    }
