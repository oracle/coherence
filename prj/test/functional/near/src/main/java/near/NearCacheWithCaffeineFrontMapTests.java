/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package near;

import com.oracle.bedrock.runtime.java.options.JvmOptions;

import com.oracle.coherence.caffeine.CaffeineCache;

import com.tangosol.net.cache.CacheStatistics;
import com.tangosol.net.cache.NearCache;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * NearCacheWithCaffeineFrontMap tests
 *
 * @author jf 2024.01.17
 */
public class NearCacheWithCaffeineFrontMapTests
        extends NearCacheTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public NearCacheWithCaffeineFrontMapTests()
        {
        super();
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        FILE_CFG_CACHE = "near-cache-caffeine-front-map-config.xml";

        startCacheServer("NearCacheWithCaffeineFrontMapTests-1", "near", FILE_CFG_CACHE,
                null, true, null,
                JvmOptions.include("-XX:+ExitOnOutOfMemoryError"));
        startCacheServer("NearCacheWithCaffeineFrontMapTests-2", "near", FILE_CFG_CACHE, null, true, null,
                JvmOptions.include("-XX:+ExitOnOutOfMemoryError"));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NearCacheWithCaffeineFrontMapTests-1");
        stopCacheServer("NearCacheWithCaffeineFrontMapTests-2");
        }

    // ----- helpers ---------------------------------------------------------

    @Override
    protected CacheStatistics getFrontMapCacheStatistics(NearCache cache)
        {
        return ((CaffeineCache) cache.getFrontMap()).getCacheStatistics();
        }
    }