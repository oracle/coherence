/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package cache;

import common.AbstractFunctionalTest;
import common.TestHelper;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.AbstractEvictionPolicy;
import com.tangosol.net.cache.ConfigurableCacheMap.Entry;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.SerializationCache;
import com.tangosol.net.cache.SimpleMemoryCalculator;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The {@link MapConfigTests} class contains various tests for map configuration, such
 * as UnitCalculator and EvictionPolicy tests.
 *
 * @author pfm 2012.02.27
 */
public class MapConfigTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
     public MapConfigTests()
         {
         super(FILE_CFG_CACHE);
         }

     // ----- test lifecycle ------------------------------------------------

     /**
      * Initialize the test class.
      */
      @BeforeClass
      public static void _startup()
          {
          // this test requires local storage to be enabled
          System.setProperty("tangosol.coherence.distributed.localstorage", "true");

          AbstractFunctionalTest._startup();
          }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that the default unit calculator for a partitioned backing map is FIXED.
     */
    @Test
    public void testUnitCalculator()
        {
        verifyCalculator("dist-default", LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("near-test",    LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("repl-test",    LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("local-test",   LocalCache.INSTANCE_FIXED.getClass());
        }

    /**
     * Test that the custom unit calculator can be configured.
     */
    @Test
    public void testExplicitUnitCalculator()
        {
        verifyCalculator("dist-calculator-binary", LocalCache.INSTANCE_BINARY.getClass());
        verifyCalculator("dist-calculator-fixed",  LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("dist-calculator-custom", CustomUnitCalculator.class);
        }

    /**
     * Test that the unit calculator defaults to BINARY is high-units is a memory string (e.g. 20M)
     */
    @Test
    public void testDefaultBinaryUnitCalculator()
        {
        verifyCalculator("dist-units-memorysize", LocalCache.INSTANCE_BINARY.getClass());
        verifyExtCalculator("dist-ext-units-memorysize", LocalCache.INSTANCE_BINARY.getClass());
        }

    /**
     * Test that the unit calculator defaults to FIXED is high-units is NOT a memory string (e.g. 2000)
     */
    @Test
    public void testDefaultFixedUnitCalculator()
        {
        verifyCalculator("dist-units-fixed", LocalCache.INSTANCE_FIXED.getClass());
        verifyExtCalculator("dist-ext-units-fixed", LocalCache.INSTANCE_FIXED.getClass());
        }

    /**
     * Test that the default eviction policy for all maps is HYBRID.
     */
    @Test
    public void testDefaultEvictionPolicy()
        {
        verifyEvictionPolicy("dist-default", LocalCache.INSTANCE_HYBRID.getClass());
        verifyEvictionPolicy("near-test",    LocalCache.INSTANCE_HYBRID.getClass());
        verifyEvictionPolicy("repl-test",    LocalCache.INSTANCE_HYBRID.getClass());
        verifyEvictionPolicy("local-test",   LocalCache.INSTANCE_HYBRID.getClass());
        }

    /**
     * Test explicit eviction policies.
     */
    @Test
    public void testExplicitEvictionPolicy()
        {
        verifyEvictionPolicy("dist-eviction-custom", CustomEvictionPolicy.class);
        verifyEvictionPolicy("dist-eviction-hybrid", LocalCache.INSTANCE_HYBRID.getClass());
        verifyEvictionPolicy("dist-eviction-lfu",    LocalCache.INSTANCE_LFU.getClass());
        verifyEvictionPolicy("dist-eviction-lru",    LocalCache.INSTANCE_LRU.getClass());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Verify that the UnitCalculator is the correct class.
     *
     * @param sCacheName     the cache name
     * @param clzCalculator  the calculator class
     */
    protected void verifyCalculator(String sCacheName, Class clzCalculator)
        {
        NamedCache cache = getNamedCache(sCacheName);
        cache.put(1,1);
        Map backingMap = TestHelper.getBackingMap(cache);

        assertNotNull(backingMap);
        assertEquals(clzCalculator, ((LocalCache) backingMap).getUnitCalculator().getClass());
        }

    /**
     * Verify that the UnitCalculator is the correct class.
     *
     * @param sCacheName     the cache name
     * @param clzCalculator  the calculator class
     */
    protected void verifyExtCalculator(String sCacheName, Class clzCalculator)
        {
        NamedCache cache = getNamedCache(sCacheName);
        cache.put(1,1);
        Map backingMap = TestHelper.getBackingMap(cache);

        assertNotNull(backingMap);
        assertEquals(clzCalculator, ((SerializationCache) backingMap).getUnitCalculator().getClass());
        }

    /**
     * Verify that the EvictionPolicy is the correct class.
     *
     * @param sCacheName         the cache name
     * @param clzEvictionPolicy  the eviction policy class
     */
    protected void verifyEvictionPolicy(String sCacheName, Class clzEvictionPolicy)
        {
        NamedCache cache = getNamedCache(sCacheName);
        cache.put(1,1);
        Map backingMap = TestHelper.getBackingMap(cache);

        assertNotNull(backingMap);
        assertEquals(clzEvictionPolicy, ((LocalCache) backingMap).getEvictionPolicy().getClass());
        }

    // ----- inner classes --------------------------------------------------

    /**
     * The custom UnitCalculator class.
     */
    public static class CustomUnitCalculator
            extends SimpleMemoryCalculator
        {
        }

    /**
     * The custom EvictionPolicy class.
     */
    public static class CustomEvictionPolicy
            extends AbstractEvictionPolicy
        {

        @Override
        public void entryTouched(Entry entry)
            {
            }

        @Override
        public void requestEviction(int cMaximum)
            {
            }

        @Override
        public void entryUpdated(Entry entry)
            {
            }
        }

    // ----- constants and data members -------------------------------------

    /**
     * The path to the cache configuration.
     */
    public final static String FILE_CFG_CACHE = "map-cache-config.xml";
    }
