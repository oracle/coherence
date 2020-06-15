/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import javax.cache.expiry.Duration;

/**
 * Constants used by the Coherence JCache Adapter.
 *
 * @author jf  2013.12.19
 * @since Coherence 12.1.3
 */
public class Constants
    {
    /**
     * The uri of the Coherence Cache Configuration file to use as the default
     * when one is not specified by an application.
     */
    public static final String DEFAULT_COHERENCE_JCACHE_CONFIGURATION_URI = "coherence-jcache-cache-config.xml";

    /**
     * The uri of the Coherence Cache Configuration file to use as the default when one is not specified by an application
     * and the property {@link #DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY} is "remote" or "extend"
     * or {@link com.tangosol.coherence.jcache.remotecache.RemoteCacheConfiguration}.
     */
    public static final String DEFAULT_COHERENCE_JCACHE_EXTEND_CLIENT_CONFIGURATION_URI = "coherence-jcache-extendclient-cache-config.xml";

    /**
     * The system property to specify an alternative {@link #DEFAULT_COHERENCE_JCACHE_CONFIGURATION_URI}.
     * <p>
     * This uri should contain a coherence configuration element with a JCache Namespace defined.
     */
    public static final String DEFAULT_COHERENCE_CONFIGURATION_URI_SYSTEM_PROPERTY = "coherence.cacheconfig";

    /**
     * The system property to specify the Coherence-based JCache Configuration class to
     * use as the default type of JCache Configuration when a standard (non-Coherence-based)
     * JCache configuration is provided.
     * <p>
     * Often this is simply an alias to actual class names.  Currently there are five
     * defined aliases; "local", "partitioned" and "extend" or "remote", "passthrough"
     * <p>
     * When not set a default of "local" is assumed.
     *
     * @see com.tangosol.coherence.jcache.localcache.LocalCacheConfiguration
     * @see com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration
     * @see com.tangosol.coherence.jcache.remotecache.RemoteCacheConfiguration
     * @see com.tangosol.coherence.jcache.passthroughcache.PassThroughCacheConfiguration
     */
    public static final String DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY =
        "coherence.jcache.configuration.classname";

    /**
     * Override default refreshtime duration for partitioned cache to aggregate cache statistics over the cluster.
     *
     * Format for string is documented at {@link com.oracle.coherence.common.util.Duration#Duration(String)}.
     *
     * @see com.tangosol.coherence.jcache.partitionedcache.PartitionedJCacheStatistics#refreshClusterJCacheStatistics()
     */
    public static final String PARTITIONED_CACHE_STATISTICS_REFRESHTIME_SYSTEM_PROPERTY =
        "coherence.jcache.statistics.refreshtime";

    /**
     * By default, throttle constant updating of JCacheStatistics across storage-enabled members of a PartitionedCache.
     *
     * Format for string is documented at {@link com.oracle.coherence.common.util.Duration#Duration(String)}.
     *
     * @see com.tangosol.coherence.jcache.partitionedcache.PartitionedJCacheStatistics#refreshClusterJCacheStatistics()
     */
    public static final String DEFAULT_PARTITIONED_CACHE_STATISTICS_REFRESHTIME = "3s";

    /**
     * The default expiry {@link Duration}.
     */
    public static final Duration DEFAULT_EXPIRY_DURATION = Duration.ETERNAL;
    }
