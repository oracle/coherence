/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
* Test harness for the various Overflow configurations.
*
* @author Naveen Gangam 2012-05-08
*/
public class BDBCacheTests
        extends AbstractFunctionalTest
    {

    /**
    * Default constructor.
    */
    public BDBCacheTests()
        {
        super(FILE_CFG_CACHE);
        }


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


    /**
    * Test.
    */
    @Test
    public void testBDBCache()
        {
        out("BDBCacheTests.testBDBCache starting");

        Person person    = null;
        NamedCache cache = getNamedCache("test");

        // there is nothing to test. The fact that getNamedCache() works is
        // proof that we are able to initialize Berkley DB.
        for (int i=0; i < 10; i++)
            {
            person = new Person("" + i);
            cache.put(i, person);
            }

        assertEquals(cache.size(), 10);
        cache.clear();
        cache.release();
        out("BDBCacheTests.testBDBCache completed");
        }

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "external-bdb-cache-config.xml";

    }
