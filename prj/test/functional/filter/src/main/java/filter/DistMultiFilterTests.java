/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.filter.EqualsFilter;

import com.oracle.coherence.testing.TestMapListener;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.DataInput;
import java.io.IOException;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
* Filter functional testing using the cache "dist-test2" and two cache
* servers.
*
* @author gg  2006.01.23
*/
public class DistMultiFilterTests
        extends AbstractFilterTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistMultiFilterTests()
        {
        super("dist-test2");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        s_server1 = (CoherenceCacheServer) startCacheServer("DistMultiFilterTests-1", "filter", null, PROPS_SEONE);
        s_server2 = (CoherenceCacheServer) startCacheServer("DistMultiFilterTests-2", "filter", null, PROPS_SEONE);

        NamedCache              cache        = CacheFactory.getCache("dist-test2");
        DistributedCacheService service      = (DistributedCacheService) cache.getCacheService();
        String                  sServiceName = service.getInfo().getServiceName();

        s_serviceIsBalanced  = new PartitionedCacheServiceIsBalanced(sServiceName);

        waitForBalancedPartitions();
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistMultiFilterTests-1");
        stopCacheServer("DistMultiFilterTests-2");
        }

    /*
     * Test that a request with bad filter is handled.
     * See bug 32152844.
     */
    @Test
    public void testFilter()
        {
        NamedCache cache = getNamedCache();

        // fill the cache with some data
        for (int i = 0; i < 5000; i++)
            {
            cache.put(i, i);
            }

        Filter filter = new CustomFilter();
        try
            {
            TestMapListener listener = new TestMapListener();
            cache.addMapListener(listener, filter, true);
            fail("Expected Exception, but got none");
            }
        catch (Exception e)
            {
            }

        try
            {
            cache.entrySet(filter);
            fail("Expected Exception, but got none");
            }
        catch (Exception e)
            {
            }

        CoherenceCacheServer clusterMember3 = (CoherenceCacheServer) startCacheServer("DistMultiFilterTests-3",
                                      "filter", null, PROPS_SEONE);
        Eventually.assertThat(invoking(m_helper).getClusterSize(clusterMember3), is(4));
        stopCacheServer("DistMultiFilterTests-3");

        // post-condition.  ensure partitioned service is rebalanced after clusterMember3 started/left.
        // Fixes failure running AbstractFilterTests#test. Details at COH-23048.
        waitForBalancedPartitions();
        }

    // ----- inner class: CustomFilter --------------------------------

    class CustomFilter extends EqualsFilter
        {
        // ----- constructors -----------------------------------------

        /**
         * Default constructor (necessary for the ExternalizableLite interface).
         */
        public CustomFilter()
            {
            }

        /**
         * Construct a IsNullFilter for testing equality to null.
         *
         * @param sMethod  the name of the method to invoke via reflection
         */
        public CustomFilter(String sMethod)
            {
            super(sMethod, null);
            }

        // ----- ExternalizableLite interface -----------------------------------

        /**
         * Throw StackOverflowError to simulate a bad filter.
         */
        public void readExternal(DataInput in)
                throws IOException
            {
            throw new StackOverflowError();
            }
        }


    // ----- AbstractEntryAggregatorTests methods ---------------------------

    /**
    * {@inheritDoc}
    */
    protected int getCacheServerCount()
        {
        return 2;
        }

    // ----- helpers -----------------------------------------------------------

    /**
     * Preconditions for filter tests.
     *
     * Ensure 2 servers and one client are member of cluster.
     * Ensure the 2 storage enabled servers have balanced partitions for s_serviceIsBalanced.
     *
     * Unreliable limit filter results causing intermittent failure when see
     * log message "Repeating QueryRequest due to the re-distribution of PartitionSet{X..N)"
     */
    private static void waitForBalancedPartitions()
        {
        long ldtStart = System.currentTimeMillis();

        Eventually.assertThat(invoking(m_helper).getClusterSize(s_server1), is(3));
        Eventually.assertThat(invoking(m_helper).getClusterSize(s_server2), is(3));
        try
            {
            Eventually.assertThat(invoking(m_helper).submitBool(s_server1, s_serviceIsBalanced), is(true));
            Eventually.assertThat(invoking(m_helper).submitBool(s_server2, s_serviceIsBalanced), is(true));
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail(e.getMessage());
            }
        System.out.println("Waited " + (System.currentTimeMillis() - ldtStart) + " ms for partition rebalancing");
        }

    // ----- data members ------------------------------------------------------

    private static CoherenceCacheServer               s_server1;

    private static CoherenceCacheServer               s_server2;

    private static PartitionedCacheServiceIsBalanced  s_serviceIsBalanced;
    }
