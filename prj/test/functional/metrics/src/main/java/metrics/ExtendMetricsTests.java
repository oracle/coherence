/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.deferred.DeferredHelper;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.tangosol.internal.metrics.DefaultMetricRegistry;
import com.tangosol.internal.net.cluster.DefaultMemberIdentity;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.InvocationService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.tangosol.coherence.component.util.SafeService;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.tangosol.internal.net.metrics.MetricsHttpHelper.PROP_METRICS_ENABLED;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * Regression test for COH-22257:
 * RFA: Memory leak in proxy node
 * Connection MBean meta data retained in MetricSupport instance for all
 * Conections that ever existed.
 *
 * @author jf  2018.07.03
 * @since 12.2.1.4.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ExtendMetricsTests
    extends AbstractMetricsFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public ExtendMetricsTests()
        {
        super("extend-client-cache-config-metrics.xml");
        }

    // ----- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        try
            {
            fileActiveDir = FileHelper.createTempDir();
            fileSnapshotDir = FileHelper.createTempDir();
            fileArchiveDir = FileHelper.createTempDir();
            }
        catch (IOException e)
            {
            //ignore
            }

        Properties props = new Properties();

        // Disable active persistence, has nothing to do with this test.
        //props.put("test.persistence.mode","active");
        props.put("test.persistence.active.dir", fileActiveDir.getAbsolutePath());
        props.put("test.persistence.snapshot.dir", fileSnapshotDir.getAbsolutePath());
        props.put("test.persistence.archive.dir", fileArchiveDir.getAbsolutePath());
        props.put("test.extend.address.local", "0.0.0.0");
        props.put("test.extend.multiservertests.enabled", "true");
        props.put("coherence.management.remote", "true");
        props.put("com.sun.management.jmxremote", "true");
        props.put("coherence.management.extendedmbeanname", "true");
        props.put("coherence.management", "all");
        props.put("coherence.role", "Server");
        props.put("coherence.override", "common-tangosol-coherence-override.xml");

        // extend client properties
        System.setProperty("coherence.management.extendedmbeanname", "true");
        System.setProperty("coherence.member", "ExtendMetricsTestsClient");
        System.setProperty("coherence.role", "TestClient");
        System.setProperty("coherence.management.remote", "true");
        System.setProperty("coherence.management.all", "true");
        System.setProperty("com.sun.management.jmxremote", "true");
        //System.setProperty("coherence.tcmp.enabled", "false");

        AvailablePortIterator ports      = new com.oracle.bedrock.runtime.network.AvailablePortIterator(9990, 10000);
        AvailablePortIterator proxyPorts = new com.oracle.bedrock.runtime.network.AvailablePortIterator(11000, 11010);

        for (int i = 0; i < N_SERVERS; i++)
            {
            String sMemberName = "ExtendMetricsTests" + (i + 1);

            props.put("coherence.member", sMemberName);

            cacheServerMetricsPorts[i] = ports.next();

            props.put(PROP_METRICS_ENABLED, "true");
            props.put("coherence.metrics.http.port", Integer.toString(cacheServerMetricsPorts[i]));
            props.put("coherence.member", sMemberName);
            int extendPort = proxyPorts.next();
            props.put("test.extend.port", Integer.toString(extendPort));
            System.setProperty("test.extend.port", Integer.toString(extendPort));

            CoherenceClusterMember clusterMember = startCacheServer(sMemberName, "metrics", FILE_SERVER_CFG_CACHE, props);
            Eventually.assertDeferred(() -> clusterMember.isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
            }

        cacheServerMetricsPorts[N_SERVERS] = ports.next();

        props.put(PROP_METRICS_ENABLED, "true");
        props.put("coherence.metrics.http.port", Integer.toString(cacheServerMetricsPorts[N_SERVERS]));
        props.put("test.proxy.enabled", "true");
        props.put("coherence.distributed.localstorage", "false");
        props.put("coherence.member", "ExtendMetricsTestsProxy");

        CoherenceClusterMember proxyMember = startCacheServer("ExtendMetricsTestsProxy", "metrics", FILE_SERVER_CFG_CACHE, props);
        Eventually.assertDeferred(() -> proxyMember.isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        for (int i = 0; i < N_SERVERS; i++)
            {
            stopCacheServer("ExtendMetricsTests" + (i + 1), true);
            }

        stopCacheServer("ExtendMetricsTestsProxy");

        if (fileActiveDir != null)
            {
            try
                {
                FileHelper.deleteDir(fileActiveDir);
                }
            catch (IOException e)
                {
                // ignored
                }
            }

        if (fileSnapshotDir != null)
            {
            try
                {
                FileHelper.deleteDir(fileSnapshotDir);
                }
            catch (IOException e)
                {
                // ignored
                }
            }

        if (fileArchiveDir != null)
            {
            try
                {
                FileHelper.deleteDir(fileArchiveDir);
                }
            catch (IOException e)
                {
                // ignored
                }
            }
        }

    // ----- test -----------------------------------------------------------

    @Test
    public void testExtendClientCollectionOverMultipleCacheServers() throws Exception
        {
        // wait for partition assignments to complete.
        Map<String, String> serviceTags = new LinkedHashMap<>();

        serviceTags.put("name", "DistributedCacheService");

        try
            {
            Eventually.assertDeferred(() -> this.getCacheMetric("Coherence.Service.StorageEnabledCount", serviceTags),
                is(2L), DeferredHelper.within(8L, TimeUnit.SECONDS));
            }
        catch (Throwable t)
            {
            getCacheMetric("Coherence.Service.StorageEnabledCount", serviceTags, true);
            throw t;
            }

        for (int k = 0; k < 5; k++)
            {
            System.out.println("Simulating new client connection: Iteration " + k);
            NamedCache cache = getFactory().ensureCache("dist-extend-test" + k, getClass().getClassLoader());
            if (cache.size() > 0)
                {
                cache.truncate();
                }
            Eventually.assertDeferred(() -> cache.size(), is(0));

            for (int i = 0; i < 1000; i++)
                {
                cache.put("key" + i, "value" + i);
                }

            for (int i = 0; i < 1100; i++)
                {
                cache.get("key" + i);
                }

            List<Map<String, Object>> listMetric;
            Map<String, String> clusterTags = new LinkedHashMap<>();

            clusterTags.put("cluster", cache.getCacheService().getCluster().getClusterName());

            listMetric = getMetrics(cacheServerMetricsPorts[0], "Coherence.Cluster.Size", clusterTags);

            assertThat("Failed to get Cluster Size metric", listMetric.size(), is(1));

            Map<String, Object> clusterSizeMetric = listMetric.get(0);

            assertThat("expected cluster size check", clusterSizeMetric.get("value"), is(N_SERVERS + 2));

            Map<String, String> tags = new LinkedHashMap<>();
            tags.put("name", cache.getCacheName());

            Map<String, String> partitionAssignmentTags = new LinkedHashMap<>();
            partitionAssignmentTags.put("cluster", cache.getCacheService().getCluster().getClusterName());
            partitionAssignmentTags.put("coherence_service", cache.getCacheService().getInfo().getServiceName());

            Map<String, String> connectionTags = new LinkedHashMap<>();
            connectionTags.put("cluster", cache.getCacheService().getCluster().getClusterName());
            connectionTags.put("member", "ExtendMetricsTestsProxy");
            connectionTags.put("clientRole", "TestClient");
            connectionTags.put("clientProcessName", new DefaultMemberIdentity().getProcessName());
            connectionTags.put("clientAddress","127.0.0.1");

            String currentMetric = null;

            try
                {
                currentMetric = "Coherence.Cache.Size";
                Eventually.assertDeferred("iteration" + k + ": Checking Coherence.Cache.Size metric",
                                          () -> getCacheMetric("Coherence.Cache.Size", tags), is(1000L), DeferredHelper.within(5L, TimeUnit.SECONDS));

                currentMetric = "Coherence.Cache.Misses";
                Eventually.assertDeferred("iteration" + k + ": Checking Coherence.Cache.Misses metric",
                                          () -> getCacheMetric("Coherence.Cache.Misses", tags), is(100L), DeferredHelper.within(5L, TimeUnit.SECONDS));

                currentMetric = "Coherence.Connection.TotalMessagesSent";
                Eventually.assertDeferred("iteration" + k + ": Checking Coherence.Connection.TotalMessagesSent metric",
                                          () -> getCacheMetric("Coherence.Connection.TotalMessagesSent", connectionTags, false, true), greaterThan(2100L), DeferredHelper.within(5L, TimeUnit.SECONDS));

                currentMetric = "Coherence.Connection.TotalMessagesReceived";
                Eventually.assertDeferred("iteration" + k + ": Checking Coherence.Connection.TotalMessagesReceived metric",
                                          () -> getCacheMetric("Coherence.Connection.TotalMessagesSent", connectionTags, false, true), greaterThan(2100L), DeferredHelper.within(5L, TimeUnit.SECONDS));

                }
            catch (Throwable t)
                {
                getCacheMetric(currentMetric, tags, true);
                throw t;
                }
            finally
                {
                cache.destroy();

                // ensure new connection per iteration to simulate multiple extend clients
                // validate via heap dump if MetricSupport instance is leaking connection metadata
                ((SafeService)cache.getCacheService()).getService().stop();

                validateMetricRegistry("iteration " + k);
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    private void validateMetricRegistry(String sDescription)
        {
        InvocationService    service = (InvocationService)CacheFactory.getService("ExtendTcpInvocationService");
        try
            {
            Map<Member, Integer> map = service.query(new MetricsAgent(), null);
            Integer count = map.values().iterator().next();

            System.out.println( sDescription + ":validateMetricRegistry: " + count);
            assertThat(map.size(), is(1));
            assertThat("assert less than or equal to 3 Connection metric in proxy server", count, lessThanOrEqualTo(3));
            }
        finally
            {
            // ensure only connection in proxy is one for cache so cache metric assertions are correct
            ((SafeService) service).getService().stop();
            }
        }

    /**
     * Executes in proxy
     */
    static public class MetricsAgent
        extends AbstractInvocable
        {
        public void run()
            {
            List<String> list = DefaultMetricRegistry.getRegistry().stream().
                filter(e -> e.getKey().toString().contains("Connection.TotalMessagesReceived")).
                map(e -> "Key: " + e.getKey() + " Value:" + e.getValue()).
                collect(Collectors.toList());

            StringBuffer sbuf = new StringBuffer("MetricsAgent: size=" + list.size() + " Details: ");
            for (String s : list)
                {
                sbuf.append(s).append(",");
                }
            System.out.println(sbuf.toString());
            setResult(list.size());
            }
        }

    /**
     * Sum <code>metric</code> value for cache <code>cacheName</code> across
     * all cache servers in the cluster.
     *
     * @param metric     the Prometheus metric
     * @param tags       tag name and value to require for match
     * @param debug      iff true log debug message
     * @return           cluster wide metric value
     *
     * @throws IOException if the request fails
     */
    private long getCacheMetric(String metric, Map<String, String> tags, boolean debug)
        {
        return getCacheMetric(metric, tags, debug, false);
        }

    /**
     * Sum <code>metric</code> value for cache <code>cacheName</code> across
     * all cache servers in the cluster.
     *
     * @param metric     the Prometheus metric
     * @param tags       tag name and value to require for match
     * @param debug      iff true log debug message
     * @param aggregate  iff true aggregate value over multiple metric instances
     * @return           cluster wide metric value
     */
    private long getCacheMetric(String metric, Map<String, String> tags, boolean debug, boolean aggregate)
        {
        long result = 0L;
        for (int port : cacheServerMetricsPorts)
            {
            if (port == 0)
                {
                continue;
                }
            long partialResult = -1;
            try
                {
                partialResult = getCacheMetric(port, metric, tags, aggregate);
                }
            catch (Throwable t)
                {
                System.out.println("getCacheMetric: handled unexpected exception " + t.getClass().getName() + ": " + t.getMessage());
                }
            if (debug)
                {
                System.out.println("Metric " + metric + " tags.name=" + tags.get("name") + "partial value=" + partialResult + " from port " + port);
                }
                
            // getCacheMetric returns -1 when a value is not found on a node, 
            // do not add not found into partial result. 
            // For looking up HA_status, the metrics is only found and returned on the coordinator node. 
            if (partialResult > 0)
                {
                result += partialResult;
                }
            }

        if (debug)
            {
            for (int port : cacheServerMetricsPorts)
                {
                if (port == 0)
                    {
                    continue;
                    }

                String sResponse = null;
                try
                    {
                    sResponse = getMetricsResponse(port);
                    }
                catch (Throwable t)
                    {}
                System.out.println("Metric response from port: " + port + "\n" + sResponse);
                }
            }
        return result;
        }

    private long getCacheMetric(String metric, Map<String, String> tags)
        {
        try
            {
            return getCacheMetric(metric, tags, false);
            }
        catch (Throwable t)
            {
            return -1;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    private static final String FILE_SERVER_CFG_CACHE = "proxy-server-cache-config-metrics.xml";

    /**
     * Number of cache servers in cluster
     */
    private static final int N_SERVERS = 1;

    /**
     * Prometheus port for each cache server by index starting at 1.
     */
    private static final int[] cacheServerMetricsPorts = new int[N_SERVERS+2];
    
    private static File   fileActiveDir;
    private static File   fileSnapshotDir;
    private static File   fileArchiveDir;
    private static final Random s_random = new Random();
    }
