/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import com.tangosol.net.options.WithConfiguration;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
* A functional test for Coherence*Extend that has multiple client sessions,
* each has its own remote service connecting to different proxy services that
* started by different cache factories.
*
* @author lh  2021.08.27
*
* @since 14.1.2.0.0
*/
public class DistExtendMultiFactoriesTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Start a test server with multiple cache factories.
    */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "MultiProxiesTests");

        SessionConfiguration sessionConfig1 = SessionConfiguration.builder()
                .named("session1")
                .withConfigUri(SERVER_CACHE_CFG_CACHE1)
                .build();
        SessionConfiguration sessionConfig2 = SessionConfiguration.builder()
                .named("session2")
                .withConfigUri(SERVER_CACHE_CFG_CACHE2)
                .build();
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .withSession(sessionConfig1)
                .withSession(sessionConfig2)
                .build();

        // Create the Coherence instance from the configuration
        Coherence coherence = Coherence.clusterMember(cfg);

        // Start Coherence
        coherence.start().join();
        }

    /**
    * Shutdown the server and test class.
    */
    @AfterClass
    public static void shutdown()
        {
        Coherence.closeAll();
        CacheFactory.getCacheFactoryBuilder().releaseAll(null);
        CacheFactory.shutdown();
        }

    @Test
    public void testMultipleSessions()
            throws Exception
        {
        try (Session session1 = Session.create(WithConfiguration.using(CLIENT_CACHE_CFG_CACHE1));
             Session session2 = Session.create(WithConfiguration.using(CLIENT_CACHE_CFG_CACHE2)))
            {
            assertNotNull(session1);
            assertNotNull(session2);

            NamedCache<String, String> namedCache1 = session1.getCache("dist-cache");
            NamedCache<String, String> namedCache2 = session2.getCache("dist-cache");

            assertNotNull(namedCache1);
            assertTrue(namedCache1.isActive());
            namedCache1.put("key1", "value1");
            assertEquals(namedCache1.get("key1"), "value1");

            assertNotNull(namedCache2);
            assertTrue(namedCache2.isActive());
            namedCache2.put("key2", "value2");
            assertEquals(namedCache2.get("key2"), "value2");

            assertEquals(1, namedCache1.size());
            assertEquals(1, namedCache2.size());
            }
        }

    // ----- helper methods -------------------------------------

    /**
     * The file name of the default cache configuration file used by the tests
     */
    public static String CLIENT_CACHE_CFG_CACHE1            = "client-cache-config-factory1.xml";
    public static String CLIENT_CACHE_CFG_CACHE2            = "client-cache-config-factory2.xml";

    /**
     * The file name of the default cache configuration file used by cache
     * servers launched by this test.
     */
    public static String SERVER_CACHE_CFG_CACHE1 = "server-cache-config-factory1.xml";
    public static String SERVER_CACHE_CFG_CACHE2 = "server-cache-config-factory2.xml";

    // ----- data members ---------------------------------------------------

    }
