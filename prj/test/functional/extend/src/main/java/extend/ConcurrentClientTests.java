/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;


/**
* A collection of functional tests for concurrent Coherence*Extend clients.
*
* @author welin  2013.02.14
*/
public class ConcurrentClientTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ConcurrentClientTests()
        {
        super("client-cache-config-concurrent.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("ConcurrentClientTests", "extend",
                                                "server-cache-config.xml");
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ConcurrentClientTests");
        }

    // ----- Concurrent Extend client tests ---------------------------------

    /**
     * COH-8758 Concurrent Requests for a Local Cache by Different Clients Can Corrupt the Cache
     */
    @Test
    public void testConcurrentRequests()
        {
        CacheService service1 = (CacheService) getFactory().ensureService("ExtendTcpCacheService1");
        CacheService service2 = (CacheService) getFactory().ensureService("ExtendTcpCacheService2");

        try
            {
            NamedCache cache1 = service1.ensureCache(LOCAL_CACHE_NAME, null);
            NamedCache cache2 = service2.ensureCache(LOCAL_CACHE_NAME, null);

            cache1.clear();
            cache1.put("key1", "value1");
            assertEquals(1, cache1.size());
            assertEquals(1, cache2.size());

            cache2.clear();

            assertEquals(0, cache1.size());
            assertEquals(0, cache2.size());
            }
        finally
            {
            service1.shutdown();
            service2.shutdown();
            }
        }

    /**
     * COH-8696 Cache Destroy/recreation Cause IllegalStateException Accessing Cache Via Extend
     */
    @Test
    public void testConcurrentDestroyCache()
        {
        CacheService service1 = (CacheService) getFactory().ensureService("ExtendTcpCacheService1");
        CacheService service2 = (CacheService) getFactory().ensureService("ExtendTcpCacheService2");

        try
            {
            NamedCache cache1 = service1.ensureCache(DIST_CACHE_NAME, null);
            NamedCache cache2 = service2.ensureCache(DIST_CACHE_NAME, null);

            cache1.clear();
            cache1.put("key1", "value1");
            cache2.put("key2", "value2");

            assertEquals(2, cache1.size());
            assertEquals(2, cache2.size());

            service1.destroyCache(cache1);

            // cache destroy is async.  Wait for cache2's status to update
//            for (long ldtStartTime = getSafeTimeMillis();
//                    cache2.isActive() && (getSafeTimeMillis() - ldtStartTime < 30000); )
//                {
//                sleep(250);
//                }
//            assertFalse(cache2.isActive());
            Eventually.assertThat(invoking(cache2).isActive(), is(false));

            cache2 = service2.ensureCache(DIST_CACHE_NAME, null);
            cache2.put("key3", "value3");
            assertEquals(1, cache2.size());
            }
        finally
            {
            service1.shutdown();
            service2.shutdown();
            }
        }

    // ----- fields and constants -------------------------------------------

    static final String LOCAL_CACHE_NAME = "local-test";
    static final String DIST_CACHE_NAME  = "dist-test";
    }
