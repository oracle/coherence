/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package filter;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import util.PartitionedCacheServiceIsBalanced;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
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
        CoherenceCacheServer clusterMember1 = (CoherenceCacheServer) startCacheServer("DistMultiFilterTests-1", "filter", null, PROPS_SEONE);
        CoherenceCacheServer clusterMember = (CoherenceCacheServer) startCacheServer("DistMultiFilterTests-2", "filter", null, PROPS_SEONE);

        Eventually.assertThat(invoking(m_helper).getClusterSize(clusterMember1), is(3));
        Eventually.assertThat(invoking(m_helper).getClusterSize(clusterMember), is(3));

        NamedCache                        cache        = CacheFactory.getCache("dist-test2");
        DistributedCacheService           service      = (DistributedCacheService) cache.getCacheService();
        String                            sServiceName = service.getInfo().getServiceName();
        PartitionedCacheServiceIsBalanced isBalanced   = new PartitionedCacheServiceIsBalanced(sServiceName);

        try
            {
            Eventually.assertThat(invoking(m_helper).submitBool(clusterMember, isBalanced), is(true));
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail(e.getMessage());
            }
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


    // ----- AbstractEntryAggregatorTests methods ---------------------------

    /**
    * {@inheritDoc}
    */
    protected int getCacheServerCount()
        {
        return 2;
        }
    }
