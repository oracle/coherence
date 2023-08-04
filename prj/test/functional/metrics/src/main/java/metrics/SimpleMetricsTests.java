/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

/**
 * Functional tests for prometheus metrics end point.
 *
 * @author jf  2018.07.03
 * @since 12.2.1.4.0
 */

public class SimpleMetricsTests
        extends AbstractMetricsTests
    {
    // ----- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        Properties props = setupProperties();

        CoherenceClusterMember clusterMember = startCacheServer("SimpleMetricsTests", "metrics", FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("SimpleMetricsTests");
        }
    }
