/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
* Filter functional testing using the cache "near-test2" and two cache
* servers.
*
* @author gg  2006.01.23
*/
public class NearMultiFilterTests
        extends AbstractFilterTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public NearMultiFilterTests()
        {
        super("near-test2");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember clusterMember1 = startCacheServer("NearMultiFilterTests-1", "filter", null, PROPS_SEONE);
        CoherenceClusterMember clusterMember2 = startCacheServer("NearMultiFilterTests-2", "filter", null, PROPS_SEONE);
        Eventually.assertThat(invoking(clusterMember1).getClusterSize(), is(3));
        Eventually.assertThat(invoking(clusterMember2).getClusterSize(), is(3));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NearMultiFilterTests-1");
        stopCacheServer("NearMultiFilterTests-2");
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
