/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.coherence.jcache.CoherenceBasedCachingProvider;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

import javax.cache.configuration.Configuration;

/**
 * Provide an abstraction to enable testing multiple JCache configurations of an jcache implementation and
 * to easily test an alternative jcache implementation.
 *
 * Implementation config files and properties are provided by each implementation of this class.
 *
 * @author jfialli
 *
 */
public abstract class AbstractJcacheTestContext
        implements JcacheTestContext
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public CacheManager getCacheManager(URI uri, ClassLoader cl, Properties props)
        {
        return Caching.getCachingProvider(CoherenceBasedCachingProvider.class.getCanonicalName()).getCacheManager(uri,
                                          cl, props);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cache configureCache(CacheManager mgr, String cacheName, Configuration config)
        {
        return mgr.createCache(cacheName, config);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract URI getDistributedCacheConfigURI();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract URI getServerCacheConfigURI();

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getInvalidCacheConfigURI()
        {
        return getURI("junit-client-invalid-cache-config.xml");
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getURI(String s)
        {
        URI uri = null;

        try
            {
            uri = new URI(s);
            }
        catch (URISyntaxException e)
            {
            e.printStackTrace();
            }

        return uri;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean supportsPof();
    }
