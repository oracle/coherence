/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.coherence.jcache.localcache.LocalCacheConfiguration;
import com.tangosol.coherence.jcache.CoherenceBasedCompleteConfiguration;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.Base;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

/**
 * Junit test for Coherence adapter impl of jcache running against LocalCache.
 *
 * @version Coherence 12.1.3
 * @author jf 2014.02.18
 */
public class LocalCacheTests
        extends AbstractCoherenceCacheTests
    {
    @Override
    protected String getTestCacheName()
        {
        return getClass().getName();
        }

    @Override
    protected <K, V> CoherenceBasedCompleteConfiguration<K, V> getConfiguration()
        {
        return new LocalCacheConfiguration<K, V>();
        }

    @BeforeClass
    static public void setup()
        {
        Caching.getCachingProvider().close();
        CacheFactory.shutdown();
        AbstractCoherenceCacheTests.beforeClassSetup();
        }

    @Override
    @Before
    public void setupTest()
        {
        super.setupTest();

        lsConfiguration = new LocalCacheConfiguration<Long, String>();
        ((LocalCacheConfiguration) lsConfiguration).setTypes(Long.class, String.class);
        spConfiguration = new LocalCacheConfiguration<String, Point>();
        ((LocalCacheConfiguration) spConfiguration).setTypes(String.class, Point.class);
        snpConfiguration = new LocalCacheConfiguration<String, NonPofPoint>();
        ((LocalCacheConfiguration) snpConfiguration).setTypes(String.class, NonPofPoint.class);
        iiConfiguration = new LocalCacheConfiguration<Integer, Integer>();
        ((LocalCacheConfiguration) iiConfiguration).setTypes(Integer.class, Integer.class);
        ssConfiguration = new LocalCacheConfiguration<String, String>();
        ((LocalCacheConfiguration) ssConfiguration).setTypes(String.class, String.class);
        slConfiguration = new LocalCacheConfiguration<String, List>();
        ((LocalCacheConfiguration) slConfiguration).setTypes(String.class, List.class);
        smConfiguration = new LocalCacheConfiguration<String, Map>();
        ((LocalCacheConfiguration) smConfiguration).setTypes(String.class, Map.class);

        final boolean STORE_BY_REFERENCE_TESTING = true;

        if (STORE_BY_REFERENCE_TESTING)
            {
            ((LocalCacheConfiguration) lsConfiguration).setStoreByValue(false);
            ((LocalCacheConfiguration) spConfiguration).setStoreByValue(false);
            ((LocalCacheConfiguration) snpConfiguration).setStoreByValue(false);
            ((LocalCacheConfiguration) iiConfiguration).setStoreByValue(false);
            ((LocalCacheConfiguration) ssConfiguration).setStoreByValue(false);
            ((LocalCacheConfiguration) slConfiguration).setStoreByValue(false);
            ((LocalCacheConfiguration) slConfiguration).setStoreByValue(false);
            }
        }

    @Override
    @After
    public void cleanupAfterTest()
        {
        super.cleanupAfterTest();
        }

    @Test
    public void detectBadJCacheConfiguration_COH11926RegressionTest()
            throws URISyntaxException
        {
        CacheManager cmgr = Caching.getCachingProvider().getCacheManager(new URI("coherence-cache-config.xml"), null,
                                null);
        Cache      cache = null;

        try {
            // next line should throw IllegalStateException.
            cache = cmgr.createCache("localCacheTest", new LocalCacheConfiguration());

            NamedCache ncache = (NamedCache) cache.unwrap(NamedCache.class);

            CacheFactory.log("ncache cache service is " + ncache.getCacheService(), Base.LOG_INFO);

            assertFalse(PartitionedService.class.isAssignableFrom(ncache.getCacheService().getClass()));


            try {
                // threw a NotSerializableException for LocalCacheValue when NamedCache within cache was
                // for distributed rather than local cache.
                cache.put("key", "value");
            } catch (Throwable t) {
                assertFalse("IllegalStateException should have been thrown during LocalCache creation. During put, unexpected exception " +
                        Base.printStackTrace(t), true);
            }
        }
        catch (IllegalStateException e) {
            // ignore.  this is expected exception thrown when there is misconfiguration.
            CacheFactory.log("handled expected IllegalStateException due to misconfiguration", Base.LOG_INFO);
        }
        finally
            {
            if (cmgr != null && cache != null)
                {
                cmgr.destroyCache(cache.getName());
                cmgr.close();
                }
            }
        }
    }
