/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.NamedCache;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
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

        m_clusterMember = startCacheServer("SimpleMetricsTests", "metrics", FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(m_clusterMember).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("SimpleMetricsTests");
        }

    @Test
    public void testViewCache()
            throws Exception
        {
        if (m_clusterMember == null)    // no metrics server, return
            {
            return;
            }

        NamedCache cache = m_clusterMember.getCache("view-cache");

        cache.put(1, "hello");
        cache.put(2, "world");
        cache.put(3, "bye");

        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("name", "view-cache");
        Eventually.assertThat(invoking(this).getCacheMetric(s_nMetricsHttpPort, "Coherence.View.Size", tags), is(3L));
        cache.destroy();
        }

    // ----- member variables -----------------------------------------------

    private static CoherenceClusterMember m_clusterMember;
    }
