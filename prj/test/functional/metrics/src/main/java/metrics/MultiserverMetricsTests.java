/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.deferred.DeferredHelper;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.partition.SimpleStrategyMBean;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.UUID;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.filter.AlwaysFilter;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.tangosol.internal.net.metrics.MetricsHttpHelper.PROP_METRICS_ENABLED;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * Functional tests for prometheus metrics end point on a cluster with multiple cache servers.
 *
 * @author jf  2018.07.03
 * @since 12.2.1.4.0
 */
@SuppressWarnings("unchecked")
public class MultiserverMetricsTests
    extends AbstractMetricsFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public MultiserverMetricsTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // ----- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup() throws IOException
        {
        Properties props = new Properties();

        props.put("coherence.management", "dynamic");

        fileActiveDir   = FileHelper.createTempDir();
        fileSnapshotDir = FileHelper.createTempDir();
        fileArchiveDir  = FileHelper.createTempDir();

        props.put("test.persistence.mode","active");
        props.put("test.persistence.active.dir", fileActiveDir.getAbsolutePath());
        props.put("test.persistence.snapshot.dir", fileSnapshotDir.getAbsolutePath());
        props.put("test.persistence.archive.dir", fileArchiveDir.getAbsolutePath());
        props.put("test.extend.address.local", "0.0.0.0");
        props.put("test.extend.multiservertests.enabled", "true");
        props.put(OperationalOverride.PROPERTY, "common-tangosol-coherence-override.xml");

        AvailablePortIterator ports      = new com.oracle.bedrock.runtime.network.AvailablePortIterator(9613, 10100);
        AvailablePortIterator proxyPorts = new com.oracle.bedrock.runtime.network.AvailablePortIterator(30100, 30200);

        for (int i = 0; i < N_SERVERS; i++)
            {
            String sMemberName = "MultiserverMetricsTests" + (i + 1);
            cacheServerMetricsPorts[i] = ports.next();

            props.put(PROP_METRICS_ENABLED, "true");
            props.put("coherence.metrics.http.port", Integer.toString(cacheServerMetricsPorts[i]));
            props.put("coherence.member", sMemberName);
            props.put("test.extend.port", Integer.toString(proxyPorts.next()));
            props.put("test.extend.port2", Integer.toString(proxyPorts.next()));

            CoherenceClusterMember clusterMember = startCacheServer(sMemberName, "metrics", FILE_SERVER_CFG_CACHE, props);

            anNodeIds[i] = clusterMember.getLocalMemberId();

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
            stopCacheServer("MultiserverMetricsTests" + (i + 1), true);
            }

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

    /**
     * Multiple cache server test.
     */
    @Test
    public void testCollectionOverMultipleCacheServers() throws Exception
        {
        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config-metrics.xml", null);
        setFactory(factory);

        // wait for partition assignments to complete.
        Map<String, String> serviceTags = new LinkedHashMap<>();

        serviceTags.put("name", "DistributedCacheService");

        try
            {
            final long ALL_SERVERS = N_SERVERS * N_SERVERS;

            Eventually.assertThat(invoking(this).getCacheMetric("Coherence.Service.StorageEnabledCount", serviceTags),
                    is(ALL_SERVERS), DeferredHelper.within(8L, TimeUnit.SECONDS));
            }
        catch (Throwable t)
            {
            getCacheMetric("Coherence.Service.StorageEnabledCount", serviceTags, true);
            throw t;
            }

        NamedCache cache = getNamedCache("dist-ms-test1");
        if (cache.size() > 0)
            {
            cache.truncate();
            }
        Eventually.assertThat(invoking(cache).size(), is(0));

        for (int i = 0; i < 1000; i++)
            {
            cache.put("key" + i, "value" + i);
            }

        for (int i = 0; i < 1100; i++)
            {
            cache.get("key" + i);
            }

        List<Map<String, Object>> listMetric;
        Map<String, String>       clusterTags  = new LinkedHashMap<>();

        clusterTags.put("cluster", cache.getCacheService().getCluster().getClusterName());

        listMetric = getMetrics(cacheServerMetricsPorts[0], "Coherence.Cluster.Size", clusterTags);

        assertThat("Failed to get Cluster Size metric", listMetric.size(), is(1));

        Map<String, Object> clusterSizeMetric = listMetric.get(0);

        assertThat("expected cluster size check", clusterSizeMetric.get("value"), is(N_SERVERS + 1));

        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("name", "dist-ms-test1");

        Map<String, String>   partitionAssignmentTags   = new LinkedHashMap<>();
        partitionAssignmentTags.put("cluster", cache.getCacheService().getCluster().getClusterName());
        partitionAssignmentTags.put("coherence_service", "DistributedCacheService");

        Map<String, String>   mapServiceTags   = new LinkedHashMap<>();
        mapServiceTags.put("name", "DistributedCacheService");
        mapServiceTags.put("type", "DistributedCache");

        String currentMetric = null;

        try
            {
            currentMetric = "Coherence.Cache.Size";
            Eventually.assertThat(invoking(this).getCacheMetric(currentMetric, tags), is(1000L), DeferredHelper.within(5L, TimeUnit.SECONDS));

            currentMetric = "Coherence.Cache.Misses";
            Eventually.assertThat(invoking(this).getCacheMetric(currentMetric, tags), is(100L), DeferredHelper.within(5L, TimeUnit.SECONDS));

            currentMetric = "Coherence.PartitionAssignment.HAStatusCode";
            Eventually.assertThat(invoking(this).getCacheMetric(currentMetric, partitionAssignmentTags),
                                  is((long) SimpleStrategyMBean.HAStatus.NODE_SAFE.getCode()), DeferredHelper.within(5L, TimeUnit.SECONDS));

            // check each cache server member (by iterating over nodeIds of storage-enabled members) has StatusHACode.
            for (int nodeId : anNodeIds)
                {
                currentMetric = "Coherence.Service.StatusHACode";
                mapServiceTags.put("nodeId", Integer.toString(nodeId));
                Eventually.assertThat(invoking(this).getCacheMetric(currentMetric, mapServiceTags),
                                      is((long) SimpleStrategyMBean.HAStatus.NODE_SAFE.getCode()), DeferredHelper.within(5L, TimeUnit.SECONDS));
                }
            }
        catch (Throwable t)
            {
            getCacheMetric(currentMetric, tags, true);
            throw t;
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Only used for live prometheus demo purposes.
     */
    @Test
    public void testDemoClient()
            throws InterruptedException
        {
        Assume.assumeTrue(Boolean.getBoolean("test.demo.enabled"));

        NamedCache cache = getNamedCache("dist-test2");

        NamedCache ncPersistence1 = getNamedCache("persistence-test1");
        NamedCache ncPersistence2 = getNamedCache("persistence-test2");
        NamedCache ncNear          = getNamedCache("near-test1");
        NamedCache ncRepl          = getNamedCache("repl-test1");

        cache.addIndex(ValueExtractor.identity(),false, null);
        ncPersistence1.addIndex(ValueExtractor.identity(),false, null);
        ncPersistence2.addIndex(ValueExtractor.identity(),false, null);

        int ITERS = 100000;
        int LOOP  = 100;

        // do some random stuff until we die
        for (int j = 0; j < ITERS; j++)
            {
            System.err.println("Iteration: " + j + " of " + ITERS);
            addData(cache, 100, LOOP, j * ITERS);

            cache.aggregate(new Count());
            cache.invokeAll(AlwaysFilter.INSTANCE(), upperCase());
            removeData(cache, LOOP, j * ITERS);

            addData(ncPersistence1, 100, LOOP, j * ITERS);
            ncPersistence1.aggregate(AlwaysFilter.INSTANCE, new Count());
            ncPersistence1.invokeAll(upperCase());
            removeData(ncPersistence1, LOOP, j * ITERS);

            addData(ncPersistence2, 100, LOOP, j * ITERS);
            ncPersistence2.aggregate(AlwaysFilter.INSTANCE, new Count());
            ncPersistence2.invokeAll(upperCase());
            removeData(ncPersistence2, LOOP, j * ITERS);

            // near
            addData(ncNear, 100, LOOP, j * ITERS);
            ncNear.aggregate(new Count());
            ncNear.invokeAll(upperCase());
            removeData(ncNear, LOOP, j * ITERS);

            // repl
            addData(ncRepl, 1, LOOP / 2, j * ITERS);
            removeData(ncRepl, LOOP, j * ITERS);

            int randomInt =  s_random.nextInt(10);
            // some random removes

            if (randomInt == 1)
                {
                ncNear.invokeAll(new CacheProcessors.Remove());
                }
            else if (randomInt == 2)
                {
                ncPersistence1.invokeAll(new CacheProcessors.Remove());
                }
            else if (randomInt == 3)
                {
                cache.clear();
                }
            else if (randomInt == 4)
                {
                ncPersistence2.invokeAll(new CacheProcessors.Remove());
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    private static InvocableMap.EntryProcessor<String, String, Void> upperCase()
        {
        return entry ->
            {
            String s = entry.getValue();

            s.toUpperCase();
            entry.setValue(s);
            return null;
            };
        }

    private void addData(NamedCache cache, int dataSize, int count, int offset)  throws InterruptedException
        {
        String data = getData(dataSize);

        Map map = new HashMap();
        for (int i = 0; i < count; i++)
            {
            Object key = Integer.valueOf(offset + i).toString();
            map.put(key, data);
            cache.get(key);
             }
        cache.putAll(map);
        Thread.sleep(500);

        for (int i = 0; i < count; i++)
            {
            cache.get(Integer.valueOf(offset + i).toString());
            Thread.sleep(50);
            }

        System.err.println("Cache size for " + cache.getCacheName() + " is: " + cache.size());
        }

    private void removeData(NamedCache cache, int count, int offset)  throws InterruptedException
        {
        int size = cache.size();
        // randomize the removes a bit so we get some variation in data
        int realCount = (s_random.nextInt(count)) / 2 + 2;
        System.err.println("Removing " + realCount + " entries from " + cache.getCacheName());
        for (int i = 0; i < realCount; i++)
            {
            cache.remove(s_random.nextInt(size + 2) + offset);
            Thread.sleep(50);
            }
        System.err.println("After Delete: Cache size for " + cache.getCacheName() + " is: " + cache.size());
        }

    private String getData(int dataSize) {
        byte[] data = new byte[dataSize];
        for (int i =0; i < dataSize; i++)
            {
            data[i] = 'X';
            }
        return new String(data);
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
    private long getCacheMetric(String metric, Map<String, String> tags, boolean debug) throws Exception
        {
        long result = 0L;
        for (int port : cacheServerMetricsPorts)
            {
            long partialResult = getCacheMetric(port, metric, tags);
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
                System.out.println("Metric response from port: " + port + "\n" + getMetricsResponse(port));
                }
            }
        return result;
        }

    // Must be public - used in Eventually.assertThat
    public long getCacheMetric(String metric, Map<String, String> tags) throws Exception
        {
        return getCacheMetric(metric, tags, false);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    private static String FILE_SERVER_CFG_CACHE = "server-cache-config-metrics.xml";

    /**
     * Number of cache servers in cluster
     */
    private static final int N_SERVERS = 3;

    /**
     * Prometheus port for each cache server by index starting at 1.
     */
    private static int[]   cacheServerMetricsPorts = new int[N_SERVERS];

    /**
     * Node Ids for cache servers.
     */
    private static int[] anNodeIds = new int[N_SERVERS];

    private static File   fileActiveDir;
    private static File   fileSnapshotDir;
    private static File   fileArchiveDir;
    private static Random s_random = new Random();
    }
