/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;


import com.tangosol.io.BinaryStore;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.SerializationCache;

import com.tangosol.util.ClassHelper;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import static org.junit.Assert.*;


/**
* A collection of functional tests for {@link SerializationCache}.
*
* @author jh  2006.04.17
*/
public class SerializationCacheTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SerializationCacheTests()
        {
        super(FILE_CFG_CACHE);
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test the ability to create and configure a SerializationCache subclass.
    */
    @Test
    public void customImplementation() throws Exception
        {
        NamedCache cache = getNamedCache("custom-serialization-cache");

        Object oCache = ClassHelper.invoke(cache, "getNamedCache", ClassHelper.VOID);
        oCache = ClassHelper.invoke(oCache, "getActualMap", ClassHelper.VOID);

        assertTrue(oCache instanceof CustomCache);

        CustomCache cacheCustom = (CustomCache) oCache;
        assertTrue(cacheCustom.getHighUnits() == 1000);
        assertTrue(cacheCustom.getLowUnits() == 800);

        cache.release();
        }


    // ----- CustomCache inner class ----------------------------------------

    /**
    * Custom SerializationCache extension.
    */
    public static class CustomCache
            extends SerializationCache
        {
        // ----- constructors ---------------------------------------------

        public CustomCache(BinaryStore store, int cMax)
            {
            super(store, cMax);
            }

        public CustomCache(BinaryStore store, int cMax, ClassLoader loader)
            {
            super(store, cMax, loader);
            }

        public CustomCache(BinaryStore store, int cMax, boolean fBinaryMap)
            {
            super(store, cMax, fBinaryMap);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "external-cache-config.xml";
    }