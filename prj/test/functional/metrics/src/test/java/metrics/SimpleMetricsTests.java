/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.tangosol.internal.metrics.MetricSupport;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.NamedCache;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.HttpURLConnection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.tangosol.internal.net.metrics.MetricsHttpHelper.PROP_METRICS_ENABLED;
import static java.net.HttpURLConnection.HTTP_OK;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Functional tests for prometheus metrics end point.
 *
 * @author jf  2018.07.03
 * @since 12.2.1.4.0
 */
@SuppressWarnings("unchecked")
public class SimpleMetricsTests
    extends AbstractMetricsFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public SimpleMetricsTests()
        {
        super("client-cache-config-metrics.xml");
        }

    // ----- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        metricsHttpPort = s_portIterator.next();

        Properties props = new Properties();

        props.put(PROP_METRICS_ENABLED, "true");
        props.put(Logging.PROPERTY_LEVEL, "9");
        props.put(LocalStorage.PROPERTY, "true");
        props.put("coherence.metrics.http.port", Integer.toString(metricsHttpPort));
        props.put("test.persistence.enabled", false);
        props.put("coherence.management.extendedmbeanname", "true");

        CoherenceClusterMember clusterMember = startCacheServer("SimpleMetricsTests", "metrics", FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("SimpleMetricsTests", true);
        }

    // ----- test -----------------------------------------------------------

    /**
     * Simplistic one cache server test.
     */
    @Test
    public void testSimpleCollection()
            throws Exception
        {
        NamedCache cache = getNamedCache("dist-test1");
        cache.put(1, "hello");
        cache.put(2, "world");
        cache.put(3, "bye");
        cache.get(1);
        cache.get(4);

        assertClusterSize(cache);

        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("name", "dist-test1");

        Eventually.assertThat(invoking(this).getCacheMetric(metricsHttpPort, "Coherence.Cache.Size", tags), is(3L));
        Eventually.assertThat(invoking(this).getCacheMetric(metricsHttpPort, "Coherence.Cache.Misses", tags), is(1L));
        Eventually.assertThat(invoking(this).getCacheMetric(metricsHttpPort, "Coherence.Cache.Hits", tags), is(1L));
        Eventually.assertThat(invoking(this).getCacheMetric(metricsHttpPort, "Coherence.Cache.UnitsBytes", tags), greaterThanOrEqualTo(15L));
        cache.destroy();
        }

    @Test
    public void testUnitsBytes()
        throws Exception
        {
        NamedCache cache = getNamedCache("dist-test4");

        cache.put((byte) 1,  (byte) 2);

        assertClusterSize(cache);

        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("name", "dist-test4");
        Eventually.assertThat(invoking(this).getCacheMetric(metricsHttpPort, "Coherence.Cache.Size", tags), is(1L));
        Eventually.assertThat(invoking(this).getCacheMetric(metricsHttpPort, "Coherence.Cache.UnitsBytes", tags), greaterThanOrEqualTo(2L));
        cache.destroy();
        }

    @Test
    public void testMetricsRequestWithAcceptEncodingGzip()
        throws Exception
        {
        NamedCache cache = getNamedCache("dist-test3");
        cache.put(1, "hello");
        cache.put(2, "world");
        cache.put(3, "bye");
        cache.get(1);
        cache.get(4);

        HttpURLConnection conn = getMetricsResponseConnectionWithAcceptEncodingGzip(metricsHttpPort);

        assertEquals(HTTP_OK, conn.getResponseCode());
        assertEquals("gzip", conn.getHeaderField("Content-Encoding"));
        conn.disconnect();
        cache.destroy();
        }

    // ----- helper methods -------------------------------------------------

    private void assertClusterSize(NamedCache cache) throws Exception
        {
        String                   sClusterName = cache.getCacheService().getCluster().getClusterName();
        Map<String, String>      mapTags      = Collections.singletonMap(MetricSupport.GLOBAL_TAG_CLUSTER, sClusterName);
        List<Map<String,Object>> listMetric   = getMetrics(metricsHttpPort, "Coherence.Cluster.Size", mapTags);

        assertThat(listMetric.size(), is(1));

        Map<String,Object> metric = listMetric.get(0);
        Number             nValue = (Number) metric.get("value");

        assertThat(nValue, is(notNullValue()));
        assertThat("expect cluster size to be 2", nValue.longValue(), is(2L));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    static String FILE_SERVER_CFG_CACHE = "server-cache-config-metrics.xml";

   // ----- data members ----------------------------------------------------

    /**
     * Default port for ManagementHttpProxy.
     */
    static int metricsHttpPort;

    static AvailablePortIterator s_portIterator = new AvailablePortIterator(9999, 10000);
    }
