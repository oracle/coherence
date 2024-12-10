/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.coherence.component.util.SafeNamedCache;
import com.tangosol.coherence.config.CacheConfig;
import com.oracle.coherence.testing.CustomClasses;

import com.tangosol.io.nio.BinaryMap;

import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.OverflowMap;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.cache.SerializationMap;
import com.tangosol.net.cache.SerializationPagedCache;
import com.tangosol.net.partition.ObservableSplittingBackingCache;
import com.tangosol.net.partition.ReadWriteSplittingBackingMap;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A collection of functional tests that test the selection of a single scheme.
 *
 * @author pfm 2012.04.17
 */
public class SchemeSelectionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SchemeSelectionTests()
        {
        this(FILE_CFG_CACHE);

        }

    /**
     * Instantiate test with this coherence config file.
     *
     * @param sPath coherence config file to use for default ECCF.
     */
    protected SchemeSelectionTests(String sPath)
        {
        super(sPath);
        ConfigurableCacheFactory factory = getFactory();
        m_config = (factory instanceof ExtensibleConfigurableCacheFactory)
                ? ((ExtensibleConfigurableCacheFactory) factory).getCacheConfig()
                : null;
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
    public void distBackingClass() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-class", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomLocalCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void distBackingExternal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-external", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, BinaryMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void distBackingLocal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-local", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, LocalCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void distBackingPagedExternal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-paged-external", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationPagedCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void distBackingRwbmClass() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-rwbm-class", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, ReadWriteBackingMap.class);
        releaseNamedCache(cache);
        }

    //@Test
    // this does not work with ECCF or DCCF since the external scheme may return
    // a non-Observable map, which is a problem
    public void distBackingRwbmExternal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-rwbm-external", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, ReadWriteBackingMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void distBackingRwbmLocal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-rwbm-local", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, ReadWriteBackingMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void distBackingRwbmPagedExternal() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-rwbm-paged-external", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, ReadWriteBackingMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void distBackingOverflow() throws Exception
        {
        NamedCache cache = validateNamedCache("dist-backing-overflow", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, OverflowMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void externalAsyncBdb() throws Exception
        {
        NamedCache cache = validateNamedCache("external-async-bdb", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void externalAsyncCustom() throws Exception
        {
        NamedCache cache = validateNamedCache("external-async-custom", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void externalAsyncNioFile() throws Exception
        {
        NamedCache cache = validateNamedCache("external-async-nio-file", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void externalBdb() throws Exception
        {
        NamedCache cache = validateNamedCache("external-bdb", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void externalCustom() throws Exception
        {
        NamedCache cache = validateNamedCache("external-custom", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void externalNioFile() throws Exception
        {
        NamedCache cache = validateNamedCache("external-nio-file", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void localTest() throws Exception
        {
        NamedCache cache = validateNamedCache("local-simple1", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        releaseNamedCache(cache);
        }

    @Test
    public void localClassTest() throws Exception
        {
        NamedCache cache = validateNamedCache("local-class1", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, CustomClasses.CustomLocalCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void nearLocalTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-local", CacheService.TYPE_LOCAL);
        assertTrue(cache.getFrontMap() instanceof LocalCache);
        assertTrue(cache.getBackMap() instanceof SafeNamedCache);
        releaseNamedCache(cache);
        }

    @Test
    public void nearFrontClassTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-front-class", CacheService.TYPE_LOCAL);
        assertTrue(cache.getFrontMap() instanceof CustomClasses.CustomLocalCache);
        releaseNamedCache(cache);
        }

    @Test
    public void nearBackClassTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-back-class", CacheService.TYPE_LOCAL);
        assertTrue(cache.getFrontMap() instanceof LocalCache);
        assertTrue(cache.getBackCache() instanceof CustomClasses.CustomNamedCache);
        releaseNamedCache(cache);
        }

    @Test
    public void nearBackDistTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-back-dist", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache.getFrontMap() instanceof LocalCache);
        assertTrue(cache.getBackCache() instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache.getBackCache(), LocalCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void nearBackExternalTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-back-external", CacheService.TYPE_LOCAL);
        assertTrue(cache.getBackCache() instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache.getBackCache(), SerializationMap.class);
        releaseNamedCache(cache);
        }

    @Test
    public void nearBackOptTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-back-opt", CacheService.TYPE_OPTIMISTIC);
        assertTrue(cache.getFrontMap() instanceof LocalCache);
        assertTrue(cache.getBackCache() instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache.getBackCache(), LocalCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void nearBackPagedExternalTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-back-paged-external", CacheService.TYPE_LOCAL);
        assertTrue(cache.getBackCache() instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache.getBackCache(), SerializationPagedCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void nearBackReplTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-back-repl", CacheService.TYPE_REPLICATED);
        assertTrue(cache.getFrontMap() instanceof LocalCache);
        assertTrue(cache.getBackCache() instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache.getBackCache(), LocalCache.class);
        releaseNamedCache(cache);
        }

    //@Test
    // waiting on COH-7078
    public void nearBackTransTest() throws Exception
        {
        NearCache cache = (NearCache) validateNamedCache("near-back-trans", CacheService.TYPE_DISTRIBUTED);
        assertTrue(cache.getFrontMap() instanceof LocalCache);
        assertTrue(cache.getBackCache() instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache.getBackCache(), LocalCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void pagedExternalAsyncBdb() throws Exception
        {
        NamedCache cache = validateNamedCache("paged-external-async-bdb", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationPagedCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void pagedExternalAsyncCustom() throws Exception
        {
        NamedCache cache = validateNamedCache("paged-external-async-custom", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationPagedCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void pagedExternalAsyncNioFile() throws Exception
        {
        NamedCache cache = validateNamedCache("paged-external-async-nio-file", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationPagedCache.class);
        //sleep to let the async write to go through
        Blocking.sleep(1000);
        releaseNamedCache(cache);
        }

    @Test
    public void pagedExternalBdb() throws Exception
        {
        NamedCache cache = validateNamedCache("paged-external-bdb", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationPagedCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void pagedExternalCustom() throws Exception
        {
        NamedCache cache = validateNamedCache("paged-external-custom", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationPagedCache.class);
        releaseNamedCache(cache);
        }

    @Test
    public void pagedExternalNioFile() throws Exception
        {
        NamedCache cache = validateNamedCache("paged-external-nio-file", CacheService.TYPE_LOCAL);
        assertTrue(cache instanceof SafeNamedCache);
        TestHelper.validateBackingMapType(cache, SerializationPagedCache.class);
        releaseNamedCache(cache);
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
        assertTrue(cache != null);
        cache.put(1,"One");
        assertTrue(cache.get(1).equals("One"));

        // validate the service type
        assertTrue(cache.getCacheService().getInfo().getServiceType().equals(sServiceType));

        return cache;
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "scheme-selection-cache-config.xml";

    // ----- data members ---------------------------------------------------

    /**
     * The ECCF cache config.
     */
    protected CacheConfig m_config;
    }
