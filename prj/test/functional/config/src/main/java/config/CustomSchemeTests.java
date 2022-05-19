/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.coherence.component.util.SafeNamedCache;

import com.oracle.coherence.testing.CustomClasses;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.OverflowMap;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.partition.ObservableSplittingBackingCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * A collection of functional tests that test the selection of a single scheme.
 *
 * @author pfm 2012.04.27
 */
public class CustomSchemeTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public CustomSchemeTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------
    @Test
    public void localCustom() throws Exception
        {
        NamedCache cache = validateNamedCache("custom-named-cache", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof CustomClasses.CustomNamedCache);
        cache.put("Key","Val");
        Assert.assertEquals("Val", cache.get("Key"));
        cache.release();
        }

    @Test
    public void distBackingCustom() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomLocalCache.class);
        cache.release();
        }

    @Test
    public void distBackingCustomAsyncExternal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-external-async-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomSerializationCache.class);
        cache.release();
        }

    @Test
    public void distBackingCustomExternal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-external-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomSerializationCache.class);
        cache.release();
        }

    @Test
    public void distBackingCustomOverflow() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-overflow-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomOverflowMap.class);
        cache.release();
        }

    @Test
    public void distBackingCustomOverflowBack() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-overflow-back-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        OverflowMap map = (OverflowMap) TestHelper.getBackingMap(cache);
        assertTrue(map.getFrontMap() instanceof LocalCache);
        Assert.assertEquals(map.getBackMap().getClass(), CustomClasses.CustomLocalCache.class);
        cache.release();
        }

    @Test
    public void distBackingCustomOverflowFront() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-overflow-front-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        OverflowMap map = (OverflowMap) TestHelper.getBackingMap(cache);
        Assert.assertEquals(map.getFrontMap().getClass(), CustomClasses.CustomLocalCache.class);
        assertTrue(map.getBackMap() instanceof LocalCache);
        cache.release();
        }

    @Test
    public void distBackingCustomPagedExternalAsyncStoreManager() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-paged-external-async-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomSerializationPagedCache.class);
        cache.release();
        }

    @Test
    public void distBackingCustomPagedExternalStoreManager() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-paged-external-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomSerializationPagedCache.class);
        cache.release();
        }

    @Test
    public void distBackingCustomRwbm() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-rwbm-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomReadWriteBackingMap.class);
        cache.release();
        }

    @Test
    public void distBackingCustomRwbmInternal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-rwbm-internal-custom", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        ReadWriteBackingMap map = (ReadWriteBackingMap) TestHelper.getBackingMap(cache);
        assertTrue(map.getInternalCache() instanceof CustomClasses.CustomLocalCache);
        cache.release();
        }

    @Test
    public void localCustomStore() throws Exception
        {
        NamedCache cache = validateNamedCache("local-custom-store", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomLocalCache.class);
        cache.release();
        }

    @Test
    public void caffeineCustom() throws Exception
        {
        NamedCache cache = validateNamedCache("caffeine-custom", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomCaffeineCache.class);
        cache.release();
        }

    @Test
    public void nearCustom() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-custom", CacheService.TYPE_LOCAL);
        Assert.assertEquals(cache.getClass(), CustomClasses.CustomNearCache.class);
        assertTrue(cache.getFrontMap() instanceof LocalCache);
        assertTrue(cache.getBackMap() instanceof SafeNamedCache);
        cache.release();
        }

    @Test
    public void nearCustomFront() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-front-custom", CacheService.TYPE_LOCAL);
        assertTrue(cache.getFrontMap() instanceof CustomClasses.CustomLocalCache);
        assertTrue(cache.getBackCache() instanceof NamedCache);
        cache.release();
        }

    @Test
    public void nearCustomBack() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-back-custom", CacheService.TYPE_LOCAL);
        assertTrue(cache.getFrontMap() instanceof LocalCache);
        assertTrue(cache.getBackCache() instanceof CustomClasses.CustomNamedCache);
        cache.release();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Get the cache and do a simple test to ensure that it works.
     *
     * @param sName         the cache name
     * @param sServiceType  the expected service type
     *
     * @return the named cache
     */
    protected NamedCache validateNamedCache(String sName, String sServiceType)
        {
        NamedCache cache = getNamedCache(sName);
        assertNotNull(cache);
        cache.put(1,"One");
        Assert.assertEquals(cache.get(1), "One");

        // validate the service type
        Assert.assertEquals(cache.getCacheService().getInfo().getServiceType(), sServiceType);

        return cache;
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "custom-scheme-cache-config.xml";
    }
