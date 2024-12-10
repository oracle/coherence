/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;


import com.tangosol.io.BinaryStore;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.SerializationMap;

import com.tangosol.util.ClassHelper;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import static org.junit.Assert.*;


/**
* A collection of functional tests for {@link SerializationMap}.
*
* @author jh  2006.04.17
*/
public class SerializationMapTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SerializationMapTests()
        {
        super(FILE_CFG_CACHE);
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test the ability to create and configure a SerializationMap subclass.
    */
    @Test
    public void customImplementation() throws Exception
        {
        NamedCache cache = getNamedCache("custom-serialization-map");

        Object oCache = ClassHelper.invoke(cache, "getNamedCache", ClassHelper.VOID);
        oCache = ClassHelper.invoke(oCache, "getActualMap", ClassHelper.VOID);

        assertTrue(oCache instanceof CustomMap);

        CustomMap cacheMap = (CustomMap) oCache;

        cache.release();
        }


    // ----- CustomMap inner class ------------------------------------------

    /**
    * Custom SerializationMap extension.
    */
    public static class CustomMap
            extends SerializationMap
        {
        // ----- constructors ---------------------------------------------

        public CustomMap(BinaryStore store)
            {
            super(store);
            }

        public CustomMap(BinaryStore store, ClassLoader loader)
            {
            super(store, loader);
            }

        public CustomMap(BinaryStore store, boolean fBinaryMap)
            {
            super(store, fBinaryMap);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "external-cache-config.xml";
    }
