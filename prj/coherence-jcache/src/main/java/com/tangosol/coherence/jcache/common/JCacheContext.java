/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Base;
import com.tangosol.util.Builder;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.util.concurrent.atomic.AtomicReference;

import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Factory;

import javax.cache.expiry.ExpiryPolicy;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheWriter;

/**
 * Coherence JCache Context on each storage member node for JCache Cache.
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class JCacheContext
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Constructs a context for a JCache cache on each coherence storage node.
     *
     *
     * @param id      JCache cache unique identifier
     * @param config  configuration for JCache {code}id{/code}
     */
    protected JCacheContext(JCacheIdentifier id, CompleteConfiguration config)
        {
        m_id = id;

        if (config == null)
            {
            Logger.warn("JCacheContext constructor called with null configuration\n" + "Stack Trace: "
                        + Base.printStackTrace(new Exception("stack trace")));

            throw new NullPointerException("configuration passed to JCacheContext can not be null");
            }

        m_cfgComplete = config;

        // create independent if statistics enabled or disabled.
        m_stats = new ContextJCacheStatistics(id);
        }

    // ----- JCacheContext methods ---------------------------------

    /**
     * Get ExpiryPolicy
     *
     * @return the contexts ExpiryPolicy
     */
    public ExpiryPolicy getExpiryPolicy()
        {
        if (m_refExpiryPolicy.get() == null)
            {
            Factory<ExpiryPolicy> factory = null;

            // tracking down NPE reported in COH-11383
            if (m_cfgComplete == null)
                {
                throw new NullPointerException("JCacheContext.getExpiryPolicy: configuration unexpectedly is null");
                }
            else
                {
                factory = m_cfgComplete.getExpiryPolicyFactory();

                if (factory == null)
                    {
                    Logger.warn("JcacheContext:getExpiryPolicy: ExpiryPolicyFactory unexpectedly null, using default ExpiryPolicy\n"
                             + " cacheid=" + m_id + " configuration=" + m_cfgComplete);

                    throw new NullPointerException("JCacheContext.getExpiryPolicy: factory unexpectedly is null");
                    }
                }

            m_refExpiryPolicy.compareAndSet(null, factory.create());
            }

        return m_refExpiryPolicy.get();
        }

    /**
     * Is read-through enabled.
     *
     * @return true if readThrough is enabled
     */
    public boolean isReadThrough()
        {
        return m_cfgComplete.isReadThrough();
        }

    /**
     * Return the configured CacheLoader independent if read-through is enabled or not.
     * Note: JCache loadAll works independent of read-through being enabled.
     *
     * @return the configured CacheLoader
     */
    public CacheLoader getCacheLoader()
        {
        if (m_refCacheLoader.get() == null)
            {
            Factory<CacheLoader> factoryLoader = m_cfgComplete.getCacheLoaderFactory();

            if (factoryLoader == null)
                {
                return null;
                }
            else
                {
                m_refCacheLoader.compareAndSet(null, (CacheLoader) factoryLoader.create());
                }
            }

        return m_refCacheLoader.get();
        }

    /**
     * If write-through is enabled, return the CacheWriter.
     *
     * @return configured CacheWriter
     */
    public CacheWriter getCacheWriter()
        {
        if (m_cfgComplete.isWriteThrough() && m_refCacheWriter.get() == null)
            {
            Factory<CacheWriter> factoryWriter = m_cfgComplete.getCacheWriterFactory();

            if (factoryWriter == null)
                {
                return null;
                }
            else
                {
                m_refCacheWriter.compareAndSet(null, (CacheWriter) factoryWriter.create());
                }
            }

        return m_refCacheWriter.get();
        }

    /**
     * Return the JCache cache configuration
     *
     * @return the JCache cache configuration
     */
    public CompleteConfiguration getConfiguration()
        {
        return m_cfgComplete;
        }

    /**
     * Return the statistics for this storage node.
     *
     * @return statistics
     */
    public JCacheStatistics getStatistics()
        {
        return m_stats;
        }

    /**
     * Get a JCacheContext for <code>cacheId</code> in {@link ResourceRegistry}
     * @param reg  Resource Registry to lookup up or create JCacheContext within.
     * @param cacheId identifier for JCacheContext
     * @return {@link JCacheContext} for cache identified by <code>cacheId</code> or null if not registered yet.
     */
    public static JCacheContext getContext(ResourceRegistry reg, JCacheIdentifier cacheId)
        {
        return reg.getResource(JCacheContext.class, cacheId.getCanonicalCacheName());
        }

    /**
     * Get or Create a JCacheContext for <code>cacheId</code> in {@link ResourceRegistry}
     * @param reg  Resource Registry to lookup up or create JCacheContext within.
     * @param cacheId identifier for JCacheContext
     * @param config JCache Configuration to associate with a new JCacheContext.
     * @return {@link JCacheContext} for cache identified by <code>cacheId</code>
     */
    public static JCacheContext getContext(ResourceRegistry reg, JCacheIdentifier cacheId, CompleteConfiguration config)
        {
        JCacheContext ctx = getContext(reg, cacheId);

        if (ctx == null)
            {
            reg.registerResource(JCacheContext.class, cacheId.getCanonicalCacheName(),
                                 new JCacheContextBuilder(cacheId, config), RegistrationBehavior.IGNORE, null);

            ctx = getContext(reg, cacheId);

            }

        return ctx;
        }

    public static void unregister(ResourceRegistry reg, JCacheIdentifier cacheId)
        {
        reg.unregisterResource(JCacheContext.class, cacheId.getCanonicalCacheName());
        }

    static class JCacheContextBuilder
            implements Builder<JCacheContext>
        {
        /**
         * Constructs ...
         *
         *
         * @param cacheId
         * @param config
         */
        JCacheContextBuilder(JCacheIdentifier cacheId, CompleteConfiguration config)
            {
            m_id     = cacheId;
            m_config = config;
            }

        @Override
        public JCacheContext realize()
            {
            return new JCacheContext(m_id, m_config);
            }

        // ----- data members------------------------------------------------

        private JCacheIdentifier      m_id;
        private CompleteConfiguration m_config;
        }

    // ----- data member ----------------------------------------------------

    // static data per storage node
    private final JCacheIdentifier      m_id;
    private final CompleteConfiguration m_cfgComplete;

    // dynamic data per storage node
    private AtomicReference<ExpiryPolicy> m_refExpiryPolicy = new AtomicReference<ExpiryPolicy>();
    private AtomicReference<CacheLoader>  m_refCacheLoader  = new AtomicReference<CacheLoader>();
    private AtomicReference<CacheWriter>  m_refCacheWriter  = new AtomicReference<CacheWriter>();
    private final JCacheStatistics        m_stats;
    }
