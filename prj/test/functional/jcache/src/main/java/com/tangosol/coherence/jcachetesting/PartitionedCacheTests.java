/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration;

import com.tangosol.coherence.jcache.CoherenceBasedCompleteConfiguration;

import com.tangosol.net.CacheFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.cache.Caching;
import java.util.List;
import java.util.Map;

/**
 * Junit test for Coherence adapter impl of jcache running against LocalCache.
 *
 * @version        1.0
 * @author         jfialli
 */
public class PartitionedCacheTests
        extends AbstractCoherenceCacheTests
    {
    // ----- AbstractCoherenceCacheTest methods -----------------------------

    protected String getTestCacheName()
        {
        return getClass().getName();
        }

    protected <K, V> CoherenceBasedCompleteConfiguration<K, V> getConfiguration()
        {
        return new PartitionedCacheConfiguration<K, V>();
        }

    // ----- PartitionedCacheTest methods -----------------------------------

    @BeforeClass
    public static void setup()
        {
        Caching.getCachingProvider().close();
        CacheFactory.shutdown();
        AbstractCoherenceCacheTests.beforeClassSetup();
        }

    /**
     * Setup jcache configurations for this implementation of CacheTest
     *
     */
    @Before
    public void setupTest()
        {
        super.setupTest();

        lsConfiguration = new PartitionedCacheConfiguration<Long, String>();
        ((PartitionedCacheConfiguration) lsConfiguration).setTypes(Long.class, String.class);
        spConfiguration = new PartitionedCacheConfiguration<String, Point>();
        ((PartitionedCacheConfiguration) spConfiguration).setTypes(String.class, Point.class);
        snpConfiguration = new PartitionedCacheConfiguration<String, NonPofPoint>();
        ((PartitionedCacheConfiguration) snpConfiguration).setTypes(String.class, NonPofPoint.class);
        iiConfiguration = new PartitionedCacheConfiguration<Integer, Integer>();
        ((PartitionedCacheConfiguration) iiConfiguration).setTypes(Integer.class, Integer.class);
        ssConfiguration = new PartitionedCacheConfiguration<String, String>();
        ((PartitionedCacheConfiguration) ssConfiguration).setTypes(String.class, String.class);
        slConfiguration = new PartitionedCacheConfiguration<String, List>();
        ((PartitionedCacheConfiguration) slConfiguration).setTypes(String.class, List.class);
        smConfiguration = new PartitionedCacheConfiguration<String, Map>();
        ((PartitionedCacheConfiguration) smConfiguration).setTypes(String.class, Map.class);
        }

    @After
    public void cleanupAfterTest()
        {
        super.cleanupAfterTest();
        }
    }
