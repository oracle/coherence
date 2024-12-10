/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import java.net.URI;

import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheManager;

import javax.cache.configuration.Configuration;

/**
 * Provide an abstraction for junit test to be configured to run in different configuration modes and/or different
 * implementations of jcache api.
 *
 * @version        1.0, 13/06/04
 * @author         jfialli
 */
public interface JcacheTestContext
    {
    /**
     * Provide a means for other implementation of jcache to inject implementation specific properties.
     *
     * @param uri should be one of the getURI methods from this class.  Each context will defines its configruations.
     * @param cl
     * @param props pass null if no shared common properties, implementation specific properties can be added
     *              in implementation of this method.
     * @return context and implementation specific configured CacheManager.
     */
    public CacheManager getCacheManager(URI uri, ClassLoader cl, Properties props);

    /**
     * provide a means for other implementations to inject their configuration into cache creation.
     *
     * @param mgr  a CacheManager returned by {@link #getCacheManager(URI,ClassLoader, Properties)}
     * @param cacheName application name for cache
     * @param config  cache configuration.
     *
     * @return a cache configured by the parameters of this method and the implementation of this method.
     */
    public Cache configureCache(CacheManager mgr, String cacheName, Configuration config);

    /**
     * @return default cache config used by most simple test environments.
     */
    public URI getDistributedCacheConfigURI();

    /**
     * Used to configure Extended Client testing mode.
     *
     * @return server cache config URI
     */
    public URI getServerCacheConfigURI();

        /**
     * Used for junit test on handling invalid configuration file.  Wanted to see if user was given sufficient
     * feedback to know that a configuration issue exists and where to fix it.
     *
     * @return a cache config file that is invalid.
     */
    public URI getInvalidCacheConfigURI();

    /**
     * create a URI from a string.
     *
     * @param s  a well-formed uri
     *
     * @return a URI
     */
    public URI getURI(String s);

    /**
     * @return true iff current test configuration supports Pof.
     */
    public boolean supportsPof();
    }
