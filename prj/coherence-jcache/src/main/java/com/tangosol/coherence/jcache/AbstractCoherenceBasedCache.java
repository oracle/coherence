/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;

import com.tangosol.net.NamedCache;

import javax.cache.CacheManager;

import javax.cache.configuration.Configuration;

import javax.cache.management.CacheMXBean;

/**
 * The base implementation of a {@link CoherenceBasedCache}.
 *
 * @param <K> the type of the {@link javax.cache.Cache} keys
 * @param <V> the type of the {@link javax.cache.Cache} values
 * @param <C> the type of the {@link javax.cache.Cache} configuration
 *
 * @author bo  2013.11.04
 * @since Coherence 12.1.3
 */
public abstract class AbstractCoherenceBasedCache<K, V, C extends CoherenceBasedConfiguration<K, V>>
        implements CoherenceBasedCache<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link AbstractCoherenceBasedCache}.
     *
     * @param manager        the {@link CoherenceBasedCacheManager} that owns the {@link CoherenceBasedCache}
     * @param sJCacheName    the name of the {@link javax.cache.Cache}
     * @param configuration  the {@link CoherenceBasedConfiguration} for the {@link javax.cache.Cache}
     */
    public AbstractCoherenceBasedCache(CoherenceBasedCacheManager manager, String sJCacheName, C configuration)
        {
        m_manager       = manager;
        m_sJCacheName   = sJCacheName;
        m_configuration = configuration;

        m_fClosed       = false;
        }

    // ----- Cache interface ------------------------------------------------

    @Override
    public void close()
        {
        if (!isClosed())
            {
            m_fClosed = true;

            onBeforeClosing();

            m_manager.releaseCache(m_sJCacheName);
            }
        }

    @Override
    public <T extends Configuration<K, V>> T getConfiguration(Class<T> clz)
        {
        if (clz.isInstance(m_configuration))
            {
            return clz.cast(m_configuration);
            }
        else
            {
            throw new IllegalArgumentException("Unsupported type in getConfiguration(" + clz.getName() + ")");
            }
        }

    @Override
    public String getName()
        {
        return m_sJCacheName;
        }

    @Override
    public CacheManager getCacheManager()
        {
        return m_manager;
        }

    @Override
    public boolean isClosed()
        {
        return m_fClosed;
        }

    @Override
    public <T> T unwrap(Class<T> clz)
        {
        if (clz.isAssignableFrom(this.getClass()))
            {
            return clz.cast(this);
            }

        if (clz.isAssignableFrom(NamedCache.class))
            {
            return (T) m_namedCache;
            }

        throw new IllegalArgumentException("Class " + this.getClass().getName() + " can't be unwrapped to: "
                                           + clz.getName() + "");
        }

    // ----- AbstractCoherenceBasedCache methods ----------------------------

    /**
     * Ensures that the {@link CoherenceBasedCache} is open (not closed).
     *
     * @throws IllegalStateException  if the {@link CoherenceBasedCache} is closed
     */
    protected void ensureOpen()
            throws IllegalStateException
        {
        if (isClosed())
            {
            throw new IllegalStateException("Operation not permitted as the Cache is closed");
            }
        }

    /**
     * Closes a {@link CoherenceBasedCache} at the request of a call to
     * {@link CoherenceBasedCache#close()}.
     */
    public abstract void onBeforeClosing();

    /**
     * Determine the {@link ClassLoader} to use for the {@link CoherenceBasedCache}.
     *
     * @return  the {@link ClassLoader}
     */
    protected ClassLoader getClassLoader()
        {
        return m_manager.getClassLoader();
        }

    /**
     * Get JMX Bean associated with Cache.
     *
     * @return JMX Bean
     */
    abstract public CacheMXBean getMBean();

    /**
     * Get JCache Statistics associated with Cache.
     *
     * @return JCache Statistics if exist or null.
     */
    abstract public JCacheStatistics getStatistics();

    /**
     * Set JCache Management status.
     *
     * @param fEnabled  true to enable
     */
    abstract public void setManagementEnabled(boolean fEnabled);

    /**
     * Set JCache statistics status
     *
     * @param fEnabled true to enable
     */
    abstract public void setStatisticsEnabled(boolean fEnabled);

    /**
     * Get JCache Statistics status
     * @return true if enabled, otherwise false
     */
    abstract public boolean isStatisticsEnabled();

    @Override
    abstract public JCacheIdentifier getIdentifier();

    // ------ data members --------------------------------------------------

    /**
     * The {@link CoherenceBasedCacheManager} that owns the {@link javax.cache.Cache}.
     */
    protected CoherenceBasedCacheManager m_manager;

    /**
     * The {@link javax.cache.Cache} name of the {@link CoherenceBasedCache}.
     */
    protected String m_sJCacheName;

    /**
     * The underlying {@link NamedCache} that holds then entries for the {@link CoherenceBasedCache}.
     */
    protected NamedCache m_namedCache;

    /**
     * The {@link Configuration} for the {@link CoherenceBasedCache}.
     */
    protected C m_configuration;

    /**
     * Is the {@link CoherenceBasedCache} closed?
     */
    private volatile boolean m_fClosed;
    }
