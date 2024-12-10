/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import javax.cache.Cache;

/**
 * An Coherence-based implementation of a {@link javax.cache.Cache}.
 * <p>
 * All Coherence-based implementations of {@link javax.cache.Cache}
 * should implement this interface.
 *
 * @author bo  2013.11.04
 * @since Coherence 12.1.3
 *
 * @param <K>  the type of keys for the {@link javax.cache.Cache}
 * @param <V>  the type of values for the {@link javax.cache.Cache}
 */
public interface CoherenceBasedCache<K, V>
        extends Cache<K, V>
    {
    // ------ CoherenceBasedCache methods -----------------------------------

    /**
     * Obtain the internal identifier used for JCache caches.
     *
     * @return  the internal {@link JCacheIdentifier}
     */
    public JCacheIdentifier getIdentifier();

    /**
     * Destroys a {@link CoherenceBasedCache} due to a request from a
     * {@link CoherenceBasedCacheManager}.
     */
    public void destroy();

    // ------ constants -----------------------------------------------------

    /**
     * The name of the Coherence NamedCache that will hold JCache {@link javax.cache.configuration.Configuration}s.
     */
    public static final String JCACHE_CONFIG_CACHE_NAME = "jcache-configurations";

    /**
     * The name of the Coherence Service that will manage {@link #JCACHE_CONFIG_CACHE_NAME} caches.
     */
    public static final String JCACHE_CONFIG_SERVICE_NAME = "jcache-configurations-service";

    /**
     * The name of the Coherence Scheme that will manage {@link #JCACHE_CONFIG_CACHE_NAME} caches.
     */
    public static final String JCACHE_CONFIG_SCHEME_NAME = "jcache-configurations-scheme";

    /**
     * The name of the Coherence Scheme that will manage {@link #JCACHE_CONFIG_CACHE_NAME} back-scheme caches.
     */
    public static final String JCACHE_CONFIG_BACK_SCHEME_NAME = "jcache-configurations-distributed-scheme";

    /**
     * The name of the Coherence Scheme that will manage {@link #JCACHE_CONFIG_CACHE_NAME} back-scheme service.
     */
    public static final String JCACHE_CONFIG_BACK_SCHEME_SERVICE_NAME = "jcache-configurations-distributed-service";

    /**
     * The Coherence NamedCache name prefix that JCache Partitioned Caches use internally.
     */
    public static final String JCACHE_PARTITIONED_CACHE_NAME_PREFIX = "jcache-partitioned-";

    /**
     * The name of the Coherence NamedCache onto which Partitioned JCaches will be mapped.
     */
    public static final String JCACHE_PARTITIONED_CACHE_NAME_PATTERN = JCACHE_PARTITIONED_CACHE_NAME_PREFIX + "*";

    /**
     * The name of the Coherence Scheme that will manage partitioned caches.
     */
    public static final String JCACHE_PARTITIONED_SCHEME_NAME = "jcache-partitioned-scheme";

    /**
     * The name of the Coherence Service that will manage partitioned caches.
     */
    public static final String JCACHE_PARTITIONED_SERVICE_NAME = "jcache-partitioned-service";

    /**
     * The Coherence NamedCache name prefix that JCache Local Caches will use.
     */
    public static final String JCACHE_LOCAL_CACHE_NAME_PREFIX = "jcache-local-";

    /**
     * The name of the Coherence NamedCache onto which Local JCaches will be mapped.
     */
    public static final String JCACHE_LOCAL_CACHE_NAME_PATTERN = JCACHE_LOCAL_CACHE_NAME_PREFIX + "*";

    /**
     * The name of the Coherence Scheme that will manage Local caches.
     */
    public static final String JCACHE_LOCAL_SCHEME_NAME = "jcache-local-scheme";

    /**
     * The name of the Coherence Service that will manage Local caches.
     */
    public static final String JCACHE_LOCAL_SERVICE_NAME = "jcache-local-service";

    public static final String JCACHE_EXTEND_SCHEME_NAME = "jcache-extend-tcp";

    public static final String JCACHE_EXTEND_SERVICE_NAME = "JCacheTCPProxyServiceName";

    public static final String JCACHE_EXTEND_PROXY_SERVICE_NAME = "TCPProxyService";

    /**
     * The name of the Coherence remote scheme for JCache.
     */
    public static final String JCACHE_REMOTE_SCHEME = "jcache-extend-tcp";
    }
