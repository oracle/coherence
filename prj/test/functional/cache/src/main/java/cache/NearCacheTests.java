/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.coherence.testing.CustomBackMap;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.NearCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The {@link NearCacheTests} class contains test that uses custom back map
 * configuration to verify that near cache internals work as expected.
 *
 * @author hr, lh 2021.01.27
 */
public class NearCacheTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
     public NearCacheTests()
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
     * Test to ensure we don't encounter bug 32415967.
     */
    @Test
    public void testNearCacheCustom()
        {
        NamedCache    cache     = getNamedCache("near-cache-custom");
        CustomBackMap mapCustom = (CustomBackMap) ((NearCache) cache).getBackCache();

        cache.clear();
        assertTrue(cache.isEmpty());

        cache.put("Key", "Value");
        assertEquals("Value", cache.get("Key"));
        assertEquals(0, mapCustom.f_atomicCounter.get());
        }

    // ----- constants and data members -------------------------------------

    /**
     * The path to the cache configuration.
     */
    public final static String FILE_CFG_CACHE = "near-cache-config.xml";
    }
