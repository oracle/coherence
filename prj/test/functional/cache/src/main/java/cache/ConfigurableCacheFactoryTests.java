/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import java.util.Map;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.LocalCache;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The {@link ConfigurableCacheFactoryTests} class contains various tests that use the
 * ConfigurableCacheFactory interface only (no ECCF or DCCF explicit creation).
 *
 * @author pfm 2012.02.29
 */
public class ConfigurableCacheFactoryTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
     public ConfigurableCacheFactoryTests()
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

          AbstractFunctionalTest._startup();
          }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that we can get cache whose scheme comes after a scheme with a
     * missing scheme name.
     */
    @Test
    public void testCoh6722()
        {
        validateLocalCache("local-1-test", 1000);
        }

    /**
     * Test that we get the correct local cache when there are multiple schemes
     * using the same service name but have different cache/map configuration values.
     */
    @Test
    public void testDuplicateServiceNameLocal()
        {
        validateLocalCache("local-2-test", 2000);
        validateLocalCache("local-3-test", 3000);
        }

    /**
     * Test that we get the correct distributed cache when there are multiple schemes
     * using the same service name but have different cache/map configuration values.
     */
    @Test
    public void testDuplicateServiceNameDist()
        {
        validateLocalCache("dist-1-test", 1000);
        validateLocalCache("dist-2-test", 2000);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate the high units setting in the local cache.
     *
     * @param  sName       the cache name
     * @param  cHighUnits  the high units
     */
    protected void validateLocalCache(String sName, int cHighUnits)
        {
        NamedCache cache = getNamedCache(sName);
        assertNotNull(cache);
        cache.put(KEY,VAL);
        assertEquals(cache.get(KEY), VAL);

        Map map = TestHelper.getBackingMap(cache);
        assertTrue(map instanceof LocalCache);
        assertEquals(((LocalCache) map).getHighUnits(), cHighUnits);
        }

    // ----- constants and data members -------------------------------------

    /**
     * The path to the cache configuration.
     */
    public final static String FILE_CFG_CACHE = "ccf-cache-config.xml";

    /**
     * The key used for the test.
     */
    private final static String KEY = "key1";

    /**
     * The value used for the test.
     */
    private final static String VAL = "val1";
    }
