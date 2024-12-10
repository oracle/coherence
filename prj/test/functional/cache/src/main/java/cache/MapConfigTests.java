/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.caffeine.CaffeineCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.cache.AbstractEvictionPolicy;
import com.tangosol.net.cache.ConfigurableCacheMap.Entry;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.SimpleMemoryCalculator;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.NamedCache;

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.Matchers.is;

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
          System.setProperty("coherence.distributed.localstorage", "true");
          System.setProperty("coherence.management.refresh.expiry", "1s");

          AbstractFunctionalTest._startup();
          }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that the default unit calculator for a partitioned backing map is FIXED.
     */
    @Test
    public void testUnitCalculator()
        {
        verifyCalculator("dist-default",  LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("near-test",     LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("repl-test",     LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("local-test",    LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("caffeine-test", LocalCache.INSTANCE_FIXED.getClass());
        }

    /**
     * Test that the custom unit calculator can be configured.
     */
    @Test
    public void testExplicitUnitCalculator()
        {
        verifyCalculator("dist-calculator-binary", LocalCache.INSTANCE_BINARY.getClass());
        verifyCalculator("dist-caffeine-calculator-binary", LocalCache.INSTANCE_BINARY.getClass());
        verifyCalculator("dist-calculator-fixed",  LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("dist-caffeine-calculator-fixed",  LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("dist-calculator-custom", CustomUnitCalculator.class);
        verifyCalculator("dist-caffeine-calculator-custom", CustomUnitCalculator.class);
        }

    /**
     * Test that the unit calculator defaults to BINARY is high-units is a memory string (e.g. 20M)
     */
    @Test
    public void testDefaultBinaryUnitCalculator()
        {
        verifyCalculator("dist-units-memorysize", LocalCache.INSTANCE_BINARY.getClass());
        verifyCalculator("dist-caffeine-units-memorysize", LocalCache.INSTANCE_BINARY.getClass());
        verifyCalculator("dist-ext-units-memorysize", LocalCache.INSTANCE_BINARY.getClass());
        }

    /**
     * Test that the unit calculator defaults to FIXED is high-units is NOT a memory string (e.g. 2000)
     */
    @Test
    public void testDefaultFixedUnitCalculator()
        {
        verifyCalculator("dist-units-fixed", LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("dist-caffeine-units-fixed", LocalCache.INSTANCE_FIXED.getClass());
        verifyCalculator("dist-ext-units-fixed", LocalCache.INSTANCE_FIXED.getClass());
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

    @Test
    public void testUnitBytesIntegerOverflow()
        {
        String                      sCacheName = "dist-calculator-large-1";
        NamedCache<Integer, String> cacheDist  = CacheFactory.getCache(sCacheName);
        Map<Integer, String>        map        = new HashMap<>();

        for (int i = 0 ; i < 1000 ; i++)
            {
            map.put(i, "value-" + i);
            }
        cacheDist.putAll(map);
        assertEquals(1000, cacheDist.size());

        MBeanServer mbs = MBeanHelper.findMBeanServer();
        Eventually.assertDeferred(() -> isUnitsBytesCorrect(mbs, sCacheName), is(true));
        }

    @Test
    public void testNearFrontSchemeCaffeine()
        {
        String                       sCacheName = "near-caffeine-cache1";
        NamedCache<Integer,Integer>  cacheNear  = CacheFactory.getCache(sCacheName);

        verifyCaffeineFrontMap(cacheNear);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Returns true if the UnitsBytes attribute is greater than Integer.MAX_VALUE.
     *
     * @param mbs         {@link MBeanServer}
     * @param sCacheName  cache name
     *
     * @return the UnitsBytes
     * @throws Exception if any JMX errors
     */
    public boolean isUnitsBytesCorrect(MBeanServer mbs, String sCacheName)
        {
        try
            {
            Set<ObjectName> setValues = mbs.queryNames(
                    new ObjectName("Coherence:type=Cache,tier=back,name=" + sCacheName + ",*"), null);
            for (ObjectName on : setValues)
                {
                return (Long) mbs.getAttribute(on, "UnitsBytes") > (long) Integer.MAX_VALUE;
                }
            }
        catch (Exception eIgnore)
            {
            }
        return false;
        }

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
        try
            {
            Method getUnitCalculator = backingMap.getClass().getMethod("getUnitCalculator");
            assertEquals(clzCalculator, getUnitCalculator.invoke(backingMap).getClass());
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
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

    /**
     * Verify that front map is Caffeine
     *
     * @param cache  the cache
     */
    protected void verifyCaffeineFrontMap(NamedCache cache)
        {
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.put(4, 4);

        Map cacheFrontMap = ((NearCache) cache).getFrontMap();

        assertNotNull(cacheFrontMap);
        assertEquals(3, cache.get(3));
        assertEquals(3, cacheFrontMap.get(3));

        for (int k = 1; k <= 4; k++)
            {
            // prime front cache
            cache.get(k);
            assertThat(cacheFrontMap.get(k), is(k));
            }

        assertThat(cache.size(), is(4));
        assertThat(cacheFrontMap.size(), is(4));
        assertThat(cacheFrontMap.getClass(), is(CaffeineCache.class));
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
     * A unit calculator that will return a value equal to Integer.MAX_VALUE to
     * test possible integer overflow.
     */
    public static class LargeIntegerValueUnitCalculator
            extends SimpleMemoryCalculator
        {
        @Override
        public int calculateUnits(Object oKey, Object oValue)
            {
            return Integer.MAX_VALUE / 10;
            }
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
