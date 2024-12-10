/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.NamedCache;

import com.tangosol.util.WrapperException;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for events over extend.
 */
public class ExtendEventTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public ExtendEventTests()
        {
        super(FILE_CLIENT_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember clusterMember = startCacheServer("ExtendEventTests", "events", FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    @Before
    public void testBefore()
        {
        }

    @After
    public void testAfter()
        {
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ExtendEventTests");
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test that we can veto an insert event.
     */
    @Test
    public void testInsertEntryEventDeath()
        {
        NamedCache cache = getNamedCache("dist-extend-direct");

        boolean fCaught  = false;
        try
            {
            cache.put(10, 10);
            }
        catch (WrapperException wEx)
            {
            if (wEx.getOriginalException() instanceof RuntimeException)
                {
                fCaught = true;
                wEx.printStackTrace();
                }
            else
                {
                throw wEx;
                }
            }

        assertTrue(fCaught);
        assertEquals(0, cache.size());
        }


    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CLIENT_CFG_CACHE = "client-cache-config.xml";

    /**
    * The file name of the default cache configuration file used by cache
    * servers launched by this test.
    */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config.xml";
    }
