/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.discovery.NSLookup;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.tangosol.internal.net.metrics.MetricsHttpHelper.PROP_METRICS_ENABLED;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * Functional test using NSLookup to collect HTTP Metrics endpoints
 *
 * @author jf  2018.07.28
 * @since 12.2.1.4.0
 */
public class NSLookUpMetricsTests
    extends AbstractMetricsFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public NSLookUpMetricsTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup() throws IOException
        {
        Properties props = new Properties();

        for (int i = 0; i < N_SERVERS; i++)
            {
            String sMemberName = "NSLookUpMetricsTests" + (i + 1);

            props.put("coherence.member", sMemberName);
            props.put(PROP_METRICS_ENABLED, "true");
            props.put("coherence.metrics.http.port", "0");
            props.put("coherence.override", "common-tangosol-coherence-override.xml");

            CoherenceClusterMember clusterMember = startCacheServer(sMemberName, "metrics", FILE_SERVER_CFG_CACHE, props);
            Eventually.assertThat(invoking(clusterMember).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
            }
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        for (int i = 0; i < N_SERVERS; i++)
            {
            stopCacheServer("NSLookUpMetricsTests" + (i + 1), true);
            }
        }

    // ----- test -----------------------------------------------------------

    @Test
    public void testLookupMetricsURLS()
        throws IOException
        {
        Cluster cluster = CacheFactory.getCluster();

        Collection<URL> colMetricsURL = NSLookup.lookupHTTPMetricsURL(cluster.getClusterName(),
            new InetSocketAddress("127.0.0.1", cluster.getDependencies().getGroupPort()));
        assertThat("validate a HTTP metrics url returned for each server by lookupHTTPMetricsURL",
            colMetricsURL.size(), is(N_SERVERS));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String     FILE_SERVER_CFG_CACHE = "server-cache-config-metrics.xml";

    /**
     * Number of cache servers in cluster
     */
    private static final int N_SERVERS = 3;
    }
