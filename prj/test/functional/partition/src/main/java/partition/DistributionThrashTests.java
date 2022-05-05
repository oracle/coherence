/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package partition;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.testing.util.IsDefaultCacheServerRunning;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.SimplePartitionKey;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.oracle.coherence.testing.AbstractRollingRestartTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.*;


/**
 * Tests that intentionally thrash the distribution.
 */
public class DistributionThrashTests
        extends AbstractFunctionalTest
    {
    // ----- test methods ----------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("DistributionThrashTests-1", "partition");
        startCacheServer("DistributionThrashTests-2", "partition");
        startCacheServer("DistributionThrashTests-3", "partition");
        startCacheServer("DistributionThrashTests-4", "partition");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistributionThrashTests-1");
        stopCacheServer("DistributionThrashTests-2");
        stopCacheServer("DistributionThrashTests-3");
        stopCacheServer("DistributionThrashTests-4");
        }

    /**
     * Test the distribution thrashing of very frequent storage membership
     * change (short-lived storage members rapidly joining/leaving).
     */
    @Test
    public void testThrashing()
        {
        // Note: only test centralized distribution here.  Legacy distribution
        //       will fail this test due to COH-2620
        NamedCache cacheSimple = getNamedCache("simple-assignment");

        populateCache(cacheSimple);
        for (int i = 0; i < THRASH_ITERATIONS; i++)
            {
            CoherenceClusterMember member = startCacheServer("DistributionThrashTests-dyn-" + i, "partition", null, null, true);
            Eventually.assertThat(member, IsDefaultCacheServerRunning.INSTANCE, is(true));

            AbstractRollingRestartTest.waitForNodeSafe(cacheSimple.getCacheService());
            stopCacheServer("DistributionThrashTests-dyn-" + i);
            }

        // check the contents of the caches
        for (int i = 1; i <= 4; i++)
            {
            checkCache(cacheSimple);

            if (i < 4)
                {
                AbstractRollingRestartTest.waitForNodeSafe(cacheSimple.getCacheService());
                }
            stopCacheServer("DistributionThrashTests-" + i);
            }
        }


    // ----- helpers ---------------------------------------------------------

    /**
     * Populate the specified cache with one key per partition.
     *
     * @param cache  the cache to populate
     */
    protected void populateCache(NamedCache cache)
        {
        PartitionedService service = (PartitionedService) cache.getCacheService();
        int cParts = service.getPartitionCount();
        Map map    = new HashMap();
        for (int i = 0; i < cParts; i++)
            {
            map.put(SimplePartitionKey.getPartitionKey(i), i);
            }

        cache.putAll(map);
        }

    /**
     * Verify the contents of the cache.
     *
     * @param cache  the cache to verify
     */
    protected void checkCache(NamedCache cache)
        {
        PartitionedService service = (PartitionedService) cache.getCacheService();
        int cParts       = service.getPartitionCount();
        Set setMissing   = null;
        Set setIncorrect = null;
        for (int i = 0; i < cParts; i++)
            {
            Integer IValue = (Integer) cache.get(SimplePartitionKey.getPartitionKey(i));
            if (IValue == null)
                {
                setMissing = setMissing == null ? new HashSet() : setMissing;
                setMissing.add(i);
                }
            else if (IValue.intValue() != i)
                {
                setIncorrect = setIncorrect == null ? new HashSet() : setIncorrect;
                setIncorrect.add(i);
                }
            }

        assertEquals(null, setMissing);
        assertEquals(null, setIncorrect);
        }


    // ----- constants ------------------------------------------------------

    /**
     * The number of iterations to run.
     */
    public static final int THRASH_ITERATIONS = 10;
    }