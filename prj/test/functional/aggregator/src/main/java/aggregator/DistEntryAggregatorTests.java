/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package aggregator;


import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.NullImplementation;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import data.Trade;
import java.util.Collections;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
* A collection of functional tests for the various
* {@link InvocableMap.EntryAggregator} implementations that use the
* "dist-test1" cache.
*
* @author jh  2005.12.21
*
* @see InvocableMap
*/
public class DistEntryAggregatorTests
        extends AbstractEntryAggregatorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistEntryAggregatorTests()
        {
        super("dist-test1");
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

        AbstractEntryAggregatorTests._startup();
        }

    @Test
    public void testCoh27651()
        {
        AbstractFunctionalTest._shutdown();
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("test.thread.count", "0");
        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("test.thread.count", "0");

        // initial cluster of 4
        for (int i = 0; i < 4; i++)
            {
            String sServerName = "storage" + i;
            CoherenceClusterMember clusterMember = startCacheServer(sServerName, "testIndexMap", "coherence-cache-config.xml", props);
            waitForServer(clusterMember);
            }

        NamedCache cache = getNamedCache();
        waitForBalanced(cache.getCacheService());

        cache.addIndex(new ReflectionExtractor("toString"));

        // initial load
        for (int i = 0; i < 1000; i++)
            {
            cache.put("a" + i, "b" + i);
            }

        // aggregate at various points in restart
        for (int i = 0; i < 10; i++)
            {
            cache.aggregate(AlwaysFilter.INSTANCE, new IndexCheckAggregator("toString"));

            String sServerNameX = "storage-" + i;

            CoherenceClusterMember clusterMemberX = startCacheServer(sServerNameX, "testIndexMap", "coherence-cache-config.xml", props);
            waitForServer(clusterMemberX);

            cache.aggregate(AlwaysFilter.INSTANCE, new IndexCheckAggregator("toString"));

            waitForBalanced(cache.getCacheService());

            cache.aggregate(AlwaysFilter.INSTANCE, new IndexCheckAggregator("toString"));

            stopCacheServer(sServerNameX);

            cache.aggregate(AlwaysFilter.INSTANCE, new IndexCheckAggregator("toString"));
            }

        // reset test
        for (int i = 0; i < 4; i++)
            {
            String sServerName = "storage" + i;
            stopCacheServer(sServerName);
            }

        AbstractFunctionalTest._shutdown();
        System.setProperty("coherence.distributed.localstorage", "true");
        AbstractFunctionalTest._startup();
        }


    @Test
    public void testCoh27651Double()
        {
        AbstractFunctionalTest._shutdown();
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("test.thread.count", "0");
        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("test.thread.count", "0");

        // initial cluster
        for (int i = 0; i < 4; i++)
            {
            String sServerName = "storage" + i;
            CoherenceClusterMember clusterMember = startCacheServer(sServerName, "testIndexMap", "coherence-cache-config.xml", props);
            waitForServer(clusterMember);
            }

        NamedCache cache = getNamedCache();
        waitForBalanced(cache.getCacheService());

        DoubleSum agent = new DoubleSum("getPrice");
        cache.addIndex(new ReflectionExtractor("getPrice"));
        cache.addIndex(new ReflectionExtractor("getSymbol"));

        // initial load
        for (int i = 1; i <= NENTRIES; ++i)
            {
            cache.put(String.valueOf(i), new Trade(i, PRICE, "IBM", 100));
            }

        // aggregate at various points in restart
        for (int i = 0; i < 10; i++)
            {
            doAggregate(cache, agent);

            String sServerNameX = "storage-" + i;
            CoherenceClusterMember clusterMemberX = startCacheServer(sServerNameX, "testIndexMap", "coherence-cache-config.xml", props);
            waitForServer(clusterMemberX);

            doAggregate(cache, agent);
            waitForBalanced(cache.getCacheService());
            doAggregate(cache, agent);
            stopCacheServer(sServerNameX);
            doAggregate(cache, agent);
            }

        // reset test
        for (int i = 0; i < 4; i++)
            {
            String sServerName = "storage" + i;
            stopCacheServer(sServerName);
            }

        AbstractFunctionalTest._shutdown();
        System.setProperty("coherence.distributed.localstorage", "true");
        AbstractFunctionalTest._startup();
        }

    protected void doAggregate(NamedCache cache, DoubleSum agent)
        {
        Object oResult = cache.aggregate(new EqualsFilter("getSymbol", "IBM"), agent);

        if ((Double) oResult != NENTRIES * PRICE)
            {
            cache.aggregate(AlwaysFilter.INSTANCE, new IndexCheckAggregator("getPrice"));
            cache.aggregate(AlwaysFilter.INSTANCE, new IndexDumpAggregator(cache.size()));
            }

        assertTrue("Result=" + oResult, equals(oResult, NENTRIES * PRICE));
        }

    private static int    NENTRIES = 200;
    private static double PRICE    = 5.0;
    }
