/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package rest;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tangosol.net.NamedCache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
 * A collection of functional tests for Coherence*Extend-REST that use the
 * default embedded HttpServer.
 *
 * This test specifically tests RWBM/CacheStore usage with REST, as
 * described in Bug21356685.
 *
 * @author par 2015.07.10
 */
public class DefaultRestGetPutTests
        extends AbstractRestTests
    {
    // ----- constructors ---------------------------------------------------

    public DefaultRestGetPutTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember clusterMember = startCacheServer("DefaultRestGetPutTests", "rest", FILE_SERVER_CFG_CACHE);
        Eventually.assertDeferred(() -> clusterMember.isServiceRunning("ExtendHttpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DefaultRestGetPutTests");
        }

    // ----- test methods ---------------------------------------------------

    /**
    * 1. perform a GET operation to read an entry that is not in the cache and
    * needs to be read from the CacheStore.
    *
    * Custom cache store implementation always returns "testREST" when there is
    * no entry in the cache for this key.
    */
    @Test
    public void testGetFromCacheStore()
        {
        WebTarget webTarget = getWebTarget("dist-test-getput/2015");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        assertEquals(200 /* OK */, response.getStatus());

        String actual = response.readEntity(String.class);
        assertEquals("testREST", actual);
        }

    /**
    * 2. perform a PUT operation that writes to a CacheStore. Using a write-behind
    * to delay the write operation to simulate a database is down. Ensure a PUT
    * operation on a new record should be delayed until the database is available.
    *
    * Custom cache store implementation delays 2 seconds before storing the
    * the entry.  The store is verified by getting the entry from the cache store,
    * and checking the value returned.
    */
    @Test
    public void testPutToCacheStore()
        {
        WebTarget webTarget = getWebTarget("dist-test-getput/2015");

        long cMillisStart    = System.currentTimeMillis();
        Response  response  = webTarget.request(MediaType.TEXT_PLAIN).put(Entity.text("RESTTest"));
        long cMillisEnd      = System.currentTimeMillis();
        assertEquals(200 /* OK */, response.getStatus());
        assertTrue((cMillisEnd-cMillisStart) > 1500); 

        response  = webTarget.request(MediaType.APPLICATION_XML).get();
        assertEquals(200 /* OK */, response.getStatus());

        String actual = response.readEntity(String.class);
        assertEquals("RESTTest", actual);
        }

    @Test
    public void testPutWithExpiry()
        {
        NamedCache cache = getNamedCache("dist-test1");
        assertNotNull(cache.get(1));

        WebTarget webTarget = getWebTarget("dist-test1/2;t=5s");

        Response  response  = webTarget.request(MediaType.TEXT_PLAIN).put(Entity.text("PutTest"));
        assertEquals(200 /* OK */, response.getStatus());

        assertEquals("PutTest", cache.get(2));
        sleep(5000);            // let the entry expire
        assertEquals("PutTest", cache.get(2));    // the data backed by the cachestore
        }


        // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-getput.xml";
    }