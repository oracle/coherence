/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.CompositeKey;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.pof.PortablePerson;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * <pre>
 * The {@link PofReferenceTest} class contains tests that use the POF object
 * reference feature of POF serializer.
 * </pre>
 */
public class PofReferenceTest
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
     public PofReferenceTest()
         {
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
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void _shutdown()
        {
        // no-op since each test shutdowns the cluster in a finally block
        }

    // ----- test methods ---------------------------------------------------

    /**
    * Regression test for COH-8911.
    */
    @Test
    public void testCompositeKey() throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", "pof-reference-cache-config.xml");
            AbstractFunctionalTest._startup();
            CacheFactory.getCache("dist-testCache");

            CompositeKey   key0    = new CompositeKey(new PortablePerson("Joe Smith", new Date(78, 4, 25)),
                                         new PortablePerson("Joe Smith", new Date(78, 4, 25)));
            PortablePerson person1 = new PortablePerson("Joe Smith", new Date(78, 4, 25));
            CompositeKey   key1    = new CompositeKey(person1, person1);
            NamedCache     cache   = CacheFactory.getCache("dist-testCache");
            cache.put(key0, "value0");
            cache.put(key1, "value1");

            assertEquals(1, cache.entrySet().size());
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }
    }
