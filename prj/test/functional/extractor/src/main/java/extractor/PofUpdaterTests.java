/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extractor;


import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.net.NamedCache;

import com.tangosol.util.extractor.PofUpdater;

import com.tangosol.util.processor.UpdaterProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.pof.Address;
import data.pof.PortablePerson;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
* Functional tests for the  {@link PofUpdater} implementation.
*
* @author as 02/10/2009
*/
public class PofUpdaterTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("PofUpdaterTests-1", "extractor");
        startCacheServer("PofUpdaterTests-2", "extractor");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("PofUpdaterTests-1");
        stopCacheServer("PofUpdaterTests-2");
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test of how the PofUpdater works with UpdaterProcessor.
    */
    @Test
    public void pofUpdaterIntegrationTest()
        {
        NamedCache     cache    = getNamedCache();
        PortablePerson original = PortablePerson.create();

        cache.put("p1", original);

        PofUpdater updName = new PofUpdater(PortablePerson.NAME);
        PofUpdater updCity = new PofUpdater(
                new SimplePofPath(new int[] {PortablePerson.ADDRESS, Address.CITY}));

        cache.invoke("p1", new UpdaterProcessor(updName, "Novak Seovic"));
        cache.invoke("p1", new UpdaterProcessor(updCity, "Lutz"));

        PortablePerson modified = (PortablePerson) cache.get("p1");
        assertEquals("Novak Seovic", modified.m_sName);
        assertEquals("Lutz", modified.getAddress().m_sCity);

        cache.release();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Return the cache used by all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        return getNamedCache("dist-test");
        }
    }