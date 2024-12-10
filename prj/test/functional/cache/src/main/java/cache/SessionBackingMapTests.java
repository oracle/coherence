/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;


import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Member;

import com.tangosol.net.internal.SessionLocalBackingMap;
import com.tangosol.net.internal.SessionLocalHelper;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.*;

import java.io.Serializable;

import java.util.Arrays;


/**
* A collection of functional tests for {@link SessionLocalBackingMap}.
*
* @author gg  2011.05.17
*/
public class SessionBackingMapTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SessionBackingMapTests()
        {
        super(FILE_CFG_CACHE);
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
        System.setProperty("coherence.distribution.2server", "false");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test the ability to access SessionLocalBackingMap directly.
    */
    @Test
    public void directAccess() throws Exception
        {
        NamedCache cache = getNamedCache("sessions");

        // put some data through the front door
        for (int i = 0; i < 20; i++)
            {
            cache.put(i, i);
            }

        SessionLocalBackingMap mapBacking = SessionLocalHelper.getBackingMap(cache);
        assertTrue("No storage for "+ cache.getCacheName(), mapBacking != null);

        // retrieve data using the backing map
        for (int i = 0; i < 20; i++)
            {
            Integer I = (Integer) mapBacking.getObject(i);
            assertTrue(I + "!=" + i, I.intValue() == i);
            }

        // update data using the backing map
        for (int i = 0; i < 20; i++)
            {
            mapBacking.putObject(i, i*i);

            Integer I = (Integer) mapBacking.getObject(i);
            assertTrue(I + "!=" + i*i, I.intValue() == i*i);
            }
        }

    /**
    * Test the ability to access SessionLocalBackingMap via SessionLocalHelper.
    */
    @Test
    public void helperAccessSingle() throws Exception
        {
        NamedCache cache = getNamedCache("sessions");

        // put some data through the front door
        for (int i = 0; i < 20; i++)
            {
            SessionLocalHelper.put(cache, i, i);
            }

        // retrieve data using the backing map
        for (int i = 0; i < 20; i++)
            {
            Integer I = (Integer) SessionLocalHelper.get(cache, i);
            assertTrue(I + "!=" + i, I.intValue() == i);
            }

        // update data using the backing map
        for (int i = 0; i < 20; i++)
            {
            SessionLocalHelper.put(cache, i, i*i);

            Integer I = (Integer) SessionLocalHelper.get(cache, i);
            assertTrue(I + "!=" + i*i, I.intValue() == i*i);
            }
        }

    /**
    * Test the ability to access SessionLocalBackingMap via SessionLocalHelper
    * when multiple servers own the data.
    */
    @Test
    public void helperAccessMulti() throws Exception
        {
        NamedCache cache = getNamedCache("sessions");

        startCacheServer("second", "cache", FILE_CFG_CACHE, PROPS_SEONE);
        try
            {
            PartitionedService service = (PartitionedService) cache.getCacheService();
            Member             member  = service.getCluster().getLocalMember();

            // wait for some re-distribution
            Eventually.assertThat(invoking(service).getOwnedPartitions(member).cardinality(), isIn(Arrays.asList(128, 129)));

            // put some data through the front door
            for (int i = 0; i < 20; i++)
                {
                cache.put(i, new Session(i));
                }

            // update data using the backing map
            for (int i = 0; i < 20; i++)
                {
                Session session = (Session) SessionLocalHelper.get(cache, i);
                assertFalse("Transient value must not be set", session.m_fLocal);

                SessionLocalHelper.put(cache, i, new Session(i));
                session = (Session) SessionLocalHelper.get(cache, i);
                if (!session.m_fLocal)
                    {
                    assertFalse("local put failed", service.getKeyOwner(i).equals(member));
                    }
                }

            cache.clear();
            }
        finally
            {
            stopCacheServer("second");
            }
        }

    /**
    * A semi-persistent data structure used to validate the locality of access.
    */
    public static class Session
            implements Serializable
        {
        public Session(int id)
            {
            m_id = id;
            m_fLocal = true;
            }

        public final int m_id;
        public transient boolean m_fLocal; // should be true only for local copies
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "session-cache-config.xml";
    }