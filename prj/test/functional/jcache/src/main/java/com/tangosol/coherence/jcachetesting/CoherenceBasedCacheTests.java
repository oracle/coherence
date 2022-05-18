/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.net.CacheFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.List;
import java.util.Map;

import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;

/**
 * Run Coherence Cache junit test with JCache default configuration.
 * This tests Coherence JCache defaulting from generic JCache default configuration
 * to one of the Coherence JCache configurations such as LocalCache or PartitionedCache.
 *
 * @version        1.0
 * @author         jfialli
 */
public class CoherenceBasedCacheTests
        extends AbstractCoherenceCacheTests
    {
    @BeforeClass
    public static void setup()
        {
        Caching.getCachingProvider().close();
        CacheFactory.shutdown();
        beforeClassSetup();
        }

    @Override
    protected <K, V> CompleteConfiguration<K, V> getConfiguration()
        {
        return new MutableConfiguration<K, V>();
        }

    /**
     * Setup testing with generic JCache javax.cache.configuration.MutableConfiguration
     *
     */
    @Before
    public void setupTest()
        {
        // test Coherence JCache Adapter defaulting by using the JCache standard configuration.
        // default is to default to Local implementation.  partitioned is another configuration.
        super.setupTest();
        lsConfiguration = new MutableConfiguration<Long, String>();
        ((MutableConfiguration) lsConfiguration).setTypes(Long.class, String.class);
        spConfiguration = new MutableConfiguration<String, Point>();
        ((MutableConfiguration) spConfiguration).setTypes(String.class, Point.class);
        ;
        snpConfiguration = new MutableConfiguration<String, NonPofPoint>();
        ((MutableConfiguration) snpConfiguration).setTypes(String.class, NonPofPoint.class);
        iiConfiguration = new MutableConfiguration<Integer, Integer>();
        ((MutableConfiguration) iiConfiguration).setTypes(Integer.class, Integer.class);
        ssConfiguration = new MutableConfiguration<String, String>();
        ((MutableConfiguration) ssConfiguration).setTypes(String.class, String.class);

        slConfiguration = new MutableConfiguration<String, List>();
        ((MutableConfiguration) slConfiguration).setTypes(String.class, List.class);
        smConfiguration = new MutableConfiguration<String, Map>();
        ((MutableConfiguration) smConfiguration).setTypes(String.class, Map.class);

        }

    @After
    public void cleanupAfterTest()
        {
        super.cleanupAfterTest();
        }

    }
