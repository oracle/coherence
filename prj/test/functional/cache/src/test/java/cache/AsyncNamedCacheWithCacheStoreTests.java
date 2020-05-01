/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import common.AbstractFunctionalTest;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for COH-20907 AsyncNamedCache putAll causing CacheLoader
 * to read through.
 *
 * @author cp  2020.04.29
 */
public class AsyncNamedCacheWithCacheStoreTests
    {

    @Test
    public void testAsyncPutAll()
        {

        Cluster cluster = null;

        try
            {
            String cacheConfig = this.getClass().getClassLoader()
                                     .getResource(FILE_CFG_CACHE).getFile();
            System.setProperty("coherence.cacheconfig", cacheConfig);
            cluster = CacheFactory.ensureCluster();

            NamedCache cache = CacheFactory.getCache("test-coh20907");
            Map<Integer, Integer> map = new HashMap<>();

            for (int i = 0; i < 10; ++i)
                {
                map.put(i, i);
                }

            try
                {
                cache.async().putAll(map);
                }
            catch (Throwable t)
                {
                fail("No Exception should have been thrown! Got Exception: " + t);
                }
            }
        finally
            {
            if (cluster != null)
                {
                cluster.shutdown();
                }
            }
        }

    // ----- constants and data members -------------------------------------

    /**
     * The name of cache configuration file.
     */
    public static final String FILE_CFG_CACHE = "server-cache-config.xml";

    }
