/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;


import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.SiteName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.tangosol.coherence.component.application.console.Coherence;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.management.AnnotatedStandardMBean;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tangosol.internal.metrics.MetricSupport.GLOBAL_TAG_CLUSTER;
import static com.tangosol.internal.metrics.MetricSupport.GLOBAL_TAG_MACHINE;
import static com.tangosol.internal.metrics.MetricSupport.GLOBAL_TAG_MEMBER;
import static com.tangosol.internal.metrics.MetricSupport.GLOBAL_TAG_ROLE;
import static com.tangosol.internal.metrics.MetricSupport.GLOBAL_TAG_SITE;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.Assert.fail;


/**
 * @author jk  2019.06.25
 */
public class MetricsSupportTests
    {
    @BeforeClass
    public static void setup()
        {
        // this test requires local storage to be enabled
        System.setProperty(CacheConfig.PROPERTY, "server-cache-config-metrics.xml");
        System.setProperty(LocalStorage.PROPERTY, "true");
        System.setProperty(JMXManagementMode.PROPERTY, "dynamic");
        System.setProperty(RoleName.PROPERTY, "ClientServer");
        System.setProperty("coherence.member", "TestClientServer");
        System.setProperty(SiteName.PROPERTY, "TestSite");
        System.setProperty(Logging.PROPERTY_LEVEL, "9");
        System.setProperty(ClusterName.PROPERTY, "MetricsSupportTestsCluster");
        System.setProperty(IPv4Preferred.JAVA_NET_PREFER_IPV4_STACK, "true");

        // ensure that all the local services are running before the tests start
        DefaultCacheServer.startServerDaemon();

        getCache(TEST_CACHE);
        getCache(TEST_NEAR_CACHE);
        getCache(TEST_WRITE_BEHIND_CACHE);
        getCache(TEST_WRITE_THROUGH_CACHE);
        getCache("persistence-1234");

        ExtensibleConfigurableCacheFactory.Dependencies deps
                = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("client-cache-config-metrics.xml");

        s_eccfClient = new ExtensibleConfigurableCacheFactory(deps);

        s_registry = CacheFactory.ensureCluster().getManagement();
        }

    @Test
    public void shouldRegisterClusterAfterNode()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(Registry.CLUSTER_TYPE);
        metricSupport.register(Registry.CLUSTER_TYPE);
        assertThat(adapter.metricCount(), is(0));

        metricSupport.register(getNodeMBeanName());

        assertThat(adapter.metricCount(), is(not(0)));
        adapter.removeMatching("Coherence.Node.");
        adapter.removeMatching("Coherence.Memory.HeapAfterGC.");
        assertThat(adapter.metricCount(), is(2));

        Map<String, String> mapTags = getClusterTags(false);

        mapTags.put("version", Coherence.VERSION);

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Cluster.Size",
                                    "Coherence.Cluster.MembersDepartureCount");
        }

    @Test
    public void shouldRegisterNode()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getNodeMBeanName());

        // The cluster is registered when the Node is registered so remove the cluster metrics before doing assertions
        adapter.removeMatching("Coherence.Cluster.");

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Node.GuardRecoverCount",
                                    "Coherence.Node.GuardTerminateCount",
                                    "Coherence.Node.PacketsReceived",
                                    "Coherence.Node.PacketsRepeated",
                                    "Coherence.Node.PacketsResent",
                                    "Coherence.Node.PacketsSent",
                                    "Coherence.Node.PublisherSuccessRate",
                                    "Coherence.Node.SendQueueSize",
                                    "Coherence.Node.ReceiverSuccessRate",
                                    "Coherence.Node.TransportBacklogDelay",
                                    "Coherence.Node.TransportReceivedBytes",
                                    "Coherence.Node.TransportReceivedMessages",
                                    "Coherence.Node.TransportRetainedBytes",
                                    "Coherence.Node.TransportSentBytes",
                                    "Coherence.Node.TransportSentMessages");
        }

    @Test
    public void shouldGetBackCacheMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getCacheMBeanName(TEST_CACHE, true));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", TEST_CACHE);
        mapTags.put("coherence_service", "DistributedCacheService");
        mapTags.put("tier", "back");

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Cache.Hits",
                                    "Coherence.Cache.HitsMillis",
                                    "Coherence.Cache.Misses",
                                    "Coherence.Cache.MissesMillis",
                                    "Coherence.Cache.Prunes",
                                    "Coherence.Cache.PrunesMillis",
                                    "Coherence.Cache.Size",
                                    "Coherence.Cache.TotalGets",
                                    "Coherence.Cache.TotalGetsMillis",
                                    "Coherence.Cache.TotalPuts",
                                    "Coherence.Cache.TotalPutsMillis",
                                    "Coherence.Cache.UnitsBytes");
        }

    @Test
    public void shouldRemoveMetrics()
        {
        String sMBeanName = getCacheMBeanName(TEST_CACHE, true);
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(sMBeanName);
        assertThat(adapter.metricCount(), is(not(0)));

        adapter.removeMatching("Coherence.Memory.HeapAfterGC.");
        metricSupport.remove(sMBeanName);
        assertThat(adapter.metricCount(), is(0));
        }


    @Test
    public void shouldGetFrontNearCacheMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getCacheMBeanName(TEST_NEAR_CACHE, false));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", TEST_NEAR_CACHE);
        mapTags.put("coherence_service", "DistributedCacheService");
        mapTags.put("tier", "front");
        mapTags.put("loader", null);

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Cache.Hits",
                                    "Coherence.Cache.HitsMillis",
                                    "Coherence.Cache.Misses",
                                    "Coherence.Cache.MissesMillis",
                                    "Coherence.Cache.Prunes",
                                    "Coherence.Cache.PrunesMillis",
                                    "Coherence.Cache.Size",
                                    "Coherence.Cache.TotalGets",
                                    "Coherence.Cache.TotalGetsMillis",
                                    "Coherence.Cache.TotalPuts",
                                    "Coherence.Cache.TotalPutsMillis",
                                    "Coherence.Cache.UnitsBytes");
        }

    @Test
    public void shouldGetBackNearCacheMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getCacheMBeanName(TEST_NEAR_CACHE, true));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", TEST_NEAR_CACHE);
        mapTags.put("coherence_service", "DistributedCacheService");
        mapTags.put("tier", "back");

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Cache.Hits",
                                    "Coherence.Cache.HitsMillis",
                                    "Coherence.Cache.Misses",
                                    "Coherence.Cache.MissesMillis",
                                    "Coherence.Cache.Prunes",
                                    "Coherence.Cache.PrunesMillis",
                                    "Coherence.Cache.Size",
                                    "Coherence.Cache.TotalGets",
                                    "Coherence.Cache.TotalGetsMillis",
                                    "Coherence.Cache.TotalPuts",
                                    "Coherence.Cache.TotalPutsMillis",
                                    "Coherence.Cache.UnitsBytes");
        }

    @Test
    public void shouldGetWriteThruBackCacheMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getCacheMBeanName(TEST_WRITE_THROUGH_CACHE, true));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", TEST_WRITE_THROUGH_CACHE);
        mapTags.put("coherence_service", "DistributedCacheService");
        mapTags.put("tier", "back");

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Cache.Hits",
                                    "Coherence.Cache.HitsMillis",
                                    "Coherence.Cache.Misses",
                                    "Coherence.Cache.MissesMillis",
                                    "Coherence.Cache.Prunes",
                                    "Coherence.Cache.PrunesMillis",
                                    "Coherence.Cache.Size",
                                    "Coherence.Cache.StoreAverageBatchSize",
                                    "Coherence.Cache.StoreFailures",
                                    "Coherence.Cache.StoreReads",
                                    "Coherence.Cache.StoreReadMillis",
                                    "Coherence.Cache.StoreWrites",
                                    "Coherence.Cache.StoreWriteMillis",
                                    "Coherence.Cache.TotalGets",
                                    "Coherence.Cache.TotalGetsMillis",
                                    "Coherence.Cache.TotalPuts",
                                    "Coherence.Cache.TotalPutsMillis",
                                    "Coherence.Cache.UnitsBytes");
        }

    @Test
    public void shouldGetWriteBehindBackCacheMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getCacheMBeanName(TEST_WRITE_BEHIND_CACHE, true));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", TEST_WRITE_BEHIND_CACHE);
        mapTags.put("coherence_service", "DistributedCacheService");
        mapTags.put("tier", "back");

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Cache.Hits",
                                    "Coherence.Cache.HitsMillis",
                                    "Coherence.Cache.Misses",
                                    "Coherence.Cache.MissesMillis",
                                    "Coherence.Cache.Prunes",
                                    "Coherence.Cache.PrunesMillis",
                                    "Coherence.Cache.QueueSize",
                                    "Coherence.Cache.Size",
                                    "Coherence.Cache.StoreAverageBatchSize",
                                    "Coherence.Cache.StoreFailures",
                                    "Coherence.Cache.StoreReads",
                                    "Coherence.Cache.StoreReadMillis",
                                    "Coherence.Cache.StoreWrites",
                                    "Coherence.Cache.StoreWriteMillis",
                                    "Coherence.Cache.TotalGets",
                                    "Coherence.Cache.TotalGetsMillis",
                                    "Coherence.Cache.TotalPuts",
                                    "Coherence.Cache.TotalPutsMillis",
                                    "Coherence.Cache.UnitsBytes");
        }

    @Test
    public void shouldGetDistributedCacheServiceMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getServiceMBeanName("DistributedCacheService"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", "DistributedCacheService");
        mapTags.put("type", "DistributedCache");

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Service.EventBacklog",
                                    "Coherence.Service.EventCount",
                                    "Coherence.Service.MemberCount",
                                    "Coherence.Service.MessagesLocal",
                                    "Coherence.Service.MessagesReceived",
                                    "Coherence.Service.MessagesSent",
                                    "Coherence.Service.OwnedPartitionsBackup",
                                    "Coherence.Service.OwnedPartitionsPrimary",
                                    "Coherence.Service.PartitionsEndangered",
                                    "Coherence.Service.PartitionsUnbalanced",
                                    "Coherence.Service.PartitionsVulnerable",
                                    "Coherence.Service.PersistenceLatencyAverage",
                                    "Coherence.Service.PersistenceLatencyMax",
                                    "Coherence.Service.PersistenceSnapshotSpaceAvailable",
                                    "Coherence.Service.PersistenceSnapshotSpaceTotal",
                                    "Coherence.Service.RequestAverageDuration",
                                    "Coherence.Service.RequestMaxDuration",
                                    "Coherence.Service.RequestPendingCount",
                                    "Coherence.Service.RequestPendingDuration",
                                    "Coherence.Service.RequestTimeoutCount",
                                    "Coherence.Service.RequestTotalCount",
                                    "Coherence.Service.StorageEnabledCount",
                                    "Coherence.Service.TaskAverageDuration",
                                    "Coherence.Service.TaskBacklog",
                                    "Coherence.Service.TaskCount",
                                    "Coherence.Service.TaskHungCount",
                                    "Coherence.Service.TaskHungDuration",
                                    "Coherence.Service.TaskMaxBacklog",
                                    "Coherence.Service.TaskTimeoutCount",
                                    "Coherence.Service.ThreadAbandonedCount",
                                    "Coherence.Service.ThreadAverageActiveCount",
                                    "Coherence.Service.ThreadCount",
                                    "Coherence.Service.ThreadIdleCount");
        }

    @Test
    public void shouldGetExtendProxyServiceMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getServiceMBeanName("ExtendTcpProxyService"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", "ExtendTcpProxyService");
        mapTags.put("type", "Proxy");

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Service.MemberCount",
                                    "Coherence.Service.MessagesLocal",
                                    "Coherence.Service.MessagesReceived",
                                    "Coherence.Service.MessagesSent",
                                    "Coherence.Service.RequestAverageDuration",
                                    "Coherence.Service.RequestMaxDuration",
                                    "Coherence.Service.RequestPendingCount",
                                    "Coherence.Service.RequestPendingDuration",
                                    "Coherence.Service.RequestTimeoutCount",
                                    "Coherence.Service.RequestTotalCount",
                                    "Coherence.Service.TaskAverageDuration",
                                    "Coherence.Service.TaskBacklog",
                                    "Coherence.Service.TaskCount",
                                    "Coherence.Service.TaskHungCount",
                                    "Coherence.Service.TaskHungDuration",
                                    "Coherence.Service.TaskMaxBacklog",
                                    "Coherence.Service.TaskTimeoutCount",
                                    "Coherence.Service.ThreadAbandonedCount",
                                    "Coherence.Service.ThreadAverageActiveCount",
                                    "Coherence.Service.ThreadCount",
                                    "Coherence.Service.ThreadIdleCount");
        }

    @Test
    public void shouldGetCacheServiceWithActivePersistence()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getServiceMBeanName("DistributedCachePersistence"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", "DistributedCachePersistence");
        mapTags.put("type", "DistributedCache");

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Service.EventBacklog",
                                    "Coherence.Service.EventCount",
                                    "Coherence.Service.MemberCount",
                                    "Coherence.Service.MessagesLocal",
                                    "Coherence.Service.MessagesReceived",
                                    "Coherence.Service.MessagesSent",
                                    "Coherence.Service.OwnedPartitionsBackup",
                                    "Coherence.Service.OwnedPartitionsPrimary",
                                    "Coherence.Service.PartitionsEndangered",
                                    "Coherence.Service.PartitionsUnbalanced",
                                    "Coherence.Service.PartitionsVulnerable",
                                    "Coherence.Service.PersistenceActiveSpaceAvailable",
                                    "Coherence.Service.PersistenceActiveSpaceTotal",
                                    "Coherence.Service.PersistenceActiveSpaceUsed",
                                    "Coherence.Service.PersistenceLatencyAverage",
                                    "Coherence.Service.PersistenceLatencyMax",
                                    "Coherence.Service.PersistenceSnapshotSpaceAvailable",
                                    "Coherence.Service.PersistenceSnapshotSpaceTotal",
                                    "Coherence.Service.RequestAverageDuration",
                                    "Coherence.Service.RequestMaxDuration",
                                    "Coherence.Service.RequestPendingCount",
                                    "Coherence.Service.RequestPendingDuration",
                                    "Coherence.Service.RequestTimeoutCount",
                                    "Coherence.Service.RequestTotalCount",
                                    "Coherence.Service.StorageEnabledCount",
                                    "Coherence.Service.TaskAverageDuration",
                                    "Coherence.Service.TaskBacklog",
                                    "Coherence.Service.TaskCount",
                                    "Coherence.Service.TaskHungCount",
                                    "Coherence.Service.TaskHungDuration",
                                    "Coherence.Service.TaskMaxBacklog",
                                    "Coherence.Service.TaskTimeoutCount",
                                    "Coherence.Service.ThreadAbandonedCount",
                                    "Coherence.Service.ThreadAverageActiveCount",
                                    "Coherence.Service.ThreadCount",
                                    "Coherence.Service.ThreadIdleCount");
        }

    @Test
    public void shouldGetPartitionAssignmentMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getPartitionAssignmentMBeanName("DistributedCacheService"));

        Map<String, String> mapTags = getClusterTags(false);

        mapTags.put("coherence_service", "DistributedCacheService");
        mapTags.put("coordinatorId", null);

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.PartitionAssignment.ServiceNodeCount",
                                    "Coherence.PartitionAssignment.ServiceMachineCount",
                                    "Coherence.PartitionAssignment.ServiceRackCount",
                                    "Coherence.PartitionAssignment.ServiceSiteCount",
                                    "Coherence.PartitionAssignment.HAStatusCode");
        }

    @Test
    public void shouldGetStorageManagerMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getStorageMBeanName(TEST_CACHE));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("cache", TEST_CACHE);
        mapTags.put("coherence_service", "DistributedCacheService");

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.StorageManager.EventsDispatched",
                                    "Coherence.StorageManager.EvictionCount",
                                    "Coherence.StorageManager.IndexTotalUnits",
                                    "Coherence.StorageManager.InsertCount",
                                    "Coherence.StorageManager.ListenerFilterCount",
                                    "Coherence.StorageManager.ListenerKeyCount",
                                    "Coherence.StorageManager.ListenerRegistrations",
                                    "Coherence.StorageManager.LocksGranted",
                                    "Coherence.StorageManager.LocksPending",
                                    "Coherence.StorageManager.MaxQueryDurationMillis",
                                    "Coherence.StorageManager.NonOptimizedQueryCount",
                                    "Coherence.StorageManager.NonOptimizedQueryTotalMillis",
                                    "Coherence.StorageManager.OptimizedQueryCount",
                                    "Coherence.StorageManager.OptimizedQueryTotalMillis",
                                    "Coherence.StorageManager.QueryContentionCount",
                                    "Coherence.StorageManager.RemoveCount");
        }

    @Test
    public void shouldGetConnectionManagerMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getMBeanName(Registry.CONNECTION_MANAGER_TYPE, "ExtendTcpProxyService"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", "ExtendTcpProxyService");
        mapTags.put("host", null);
        mapTags.put("protocol", null);

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.ConnectionManager.TotalBytesReceived",
                                    "Coherence.ConnectionManager.TotalBytesSent",
                                    "Coherence.ConnectionManager.TotalMessagesReceived",
                                    "Coherence.ConnectionManager.TotalMessagesSent",
                                    "Coherence.ConnectionManager.UnauthorizedConnectionAttempts",
                                    "Coherence.ConnectionManager.OutgoingByteBacklog",
                                    "Coherence.ConnectionManager.OutgoingMessageBacklog",
                                    "Coherence.ConnectionManager.ConnectionCount");
        }

    @Test
    public void shouldGetPlatformOSMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getMBeanName("*:type=Platform,Domain=java.lang,subType=OperatingSystem,*"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", System.getProperty("os.name"));
        mapTags.put("arch", System.getProperty("os.arch"));
        mapTags.put("version", System.getProperty("os.version"));

        List<String> listExpectedMetrics =
                new ArrayList<>(Arrays.asList("Coherence.OS.AvailableProcessors",
                                              "Coherence.OS.CommittedVirtualMemorySize",
                                              "Coherence.OS.FreePhysicalMemorySize",
                                              "Coherence.OS.FreeSwapSpaceSize",
                                              "Coherence.OS.ProcessCpuLoad",
                                              "Coherence.OS.ProcessCpuTime",
                                              "Coherence.OS.SystemCpuLoad",
                                              "Coherence.OS.TotalPhysicalMemorySize",
                                              "Coherence.OS.TotalSwapSpaceSize"));

        // the following attributes are only supported on non-Windows systems
        if (!System.getProperty("os.name").toLowerCase().contains("windows"))
            {
            listExpectedMetrics.add("Coherence.OS.MaxFileDescriptorCount");
            listExpectedMetrics.add("Coherence.OS.OpenFileDescriptorCount");
            listExpectedMetrics.add("Coherence.OS.SystemLoadAverage");
            }

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    listExpectedMetrics.toArray(new String[0]));
        }

    @Test
    public void shouldGetPlatformMemoryMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getMBeanName("*:type=Platform,Domain=java.lang,subType=Memory,*"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Memory.ObjectPendingFinalizationCount",
                                    "Coherence.Memory.HeapMemoryUsage.committed",
                                    "Coherence.Memory.HeapMemoryUsage.init",
                                    "Coherence.Memory.HeapMemoryUsage.max",
                                    "Coherence.Memory.HeapMemoryUsage.used",
                                    "Coherence.Memory.NonHeapMemoryUsage.committed",
                                    "Coherence.Memory.NonHeapMemoryUsage.init",
                                    "Coherence.Memory.NonHeapMemoryUsage.max",
                                    "Coherence.Memory.NonHeapMemoryUsage.used");
        }

    @Test
    public void shouldGetPlatformGarbageCollectorMetrics()
        {
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getMBeanName("*:type=Platform,Domain=java.lang,subType=GarbageCollector,*"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", null);

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.GC.CollectionCount",
                                    "Coherence.GC.CollectionTime",
                                    "Coherence.GC.LastGcInfo.id",
                                    "Coherence.GC.LastGcInfo.startTime",
                                    "Coherence.GC.LastGcInfo.endTime",
                                    "Coherence.GC.LastGcInfo.duration");
        }

    @Test
    public void shouldGetExtendConnectionMetrics()
        {
        // make a client connection
        s_eccfClient.ensureCache("dist-extend-1234", null);

        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getMBeanName(Registry.CONNECTION_TYPE, "ExtendTcpProxyService"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("name", "ExtendTcpProxyService");
        mapTags.put("remoteAddress", null);
        mapTags.put("remotePort", null);

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Connection.ConnectionTimeMillis",
                                    "Coherence.Connection.OutgoingByteBacklog",
                                    "Coherence.Connection.OutgoingMessageBacklog",
                                    "Coherence.Connection.TotalBytesReceived",
                                    "Coherence.Connection.TotalBytesSent",
                                    "Coherence.Connection.TotalMessagesSent",
                                    "Coherence.Connection.TotalMessagesReceived");
        }

    @Test
    public void shouldRegisterCustomMBean() throws Exception
        {
        Registry registry = CacheFactory.ensureCluster().getManagement();
        Dummy dummy = new Dummy();
        String sMBeanName = registry.ensureGlobalName("type=Dummy,foo=bar");

        registry.register(sMBeanName, new AnnotatedStandardMBean(dummy, DummyMBean.class));

        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getMBeanName("*:type=Dummy,*"));

        Map<String, String> mapTags = getCommonTagsWithNodeId();

        mapTags.put("foo", "bar");               // tag from object name registered above
        mapTags.put("custom_tag", "TagOne");     // tag from annotated interface method with name specified
        mapTags.put("tagValueTwo", "TagTwo");    // tag from annotated interface method with no name specified

        assertMetricsWithoutAfterGC(adapter.getMetrics(),
                                    mapTags,
                                    "Coherence.Dummy.custom_value",  // metric from annotated method with name specified
                                    "Coherence.Dummy.ValueTwo");                   //  metric from annotated method with no name specified
        }

    @Test
    public void shouldGetHeapAfterGCMetrics() throws Exception
        {
        Assume.assumeThat("test skipped, no memory pool MBeans", ManagementFactory.getMemoryPoolMXBeans().size(), is(not(0)));
        MetricsRegistryAdapterStub adapter = new MetricsRegistryAdapterStub();
        MetricSupport metricSupport = new MetricSupport(registrySupplier(), Collections.singletonList(adapter));

        metricSupport.register(getMBeanName("*:type=Platform,Domain=java.lang,subType=MemoryPool,*"));

        assertThat(adapter.metricCount(), is(not(0)));

        List<String> metrics = adapter.getMetrics()
                .stream()
                .map(MBeanMetric::getName)
                .distinct()
                .collect(Collectors.toList());

        assertThat(metrics.size(), is(4));
        assertThat(metrics, containsInAnyOrder("Coherence.Memory.HeapAfterGC.Initial",
                                               "Coherence.Memory.HeapAfterGC.Used",
                                               "Coherence.Memory.HeapAfterGC.Committed",
                                               "Coherence.Memory.HeapAfterGC.Max"));
        }

    // ----- helper methods -------------------------------------------------


    private void assertMetricsWithoutAfterGC(List<MBeanMetric> list, Map<String, String> mapTags, String... asMetricName)
        {
        List<MBeanMetric> filtered = list.stream()
                .filter(m -> !m.getName().startsWith("Coherence.Memory.HeapAfterGC."))
                .collect(Collectors.toList());

        assertMetrics(filtered, mapTags, asMetricName);
        }

    private void assertMetrics(List<MBeanMetric> list, Map<String, String> mapTags, String... asMetricName)
        {
        if (list.size() > asMetricName.length)
            {
            List<String> listExpected = Arrays.asList(asMetricName);
            List<String> listExtra = list.stream()
                    .filter(m -> !listExpected.contains(m.getName()))
                    .map(MBeanMetric::getName)
                    .collect(Collectors.toList());

            fail("Extra metric found " + listExtra);
            }

        List<String> listMissing = new ArrayList<>();

        for (String sMetricName : asMetricName)
            {
            if (!assertMetric(list, mapTags, sMetricName))
                {
                listMissing.add(sMetricName);
                }
            }

        if (listMissing.size() > 0)
            {
            fail("Missing metrics: " + listMissing);
            }
        }

    private boolean assertMetric(List<MBeanMetric> list, Map<String, String> mapTags, String sMetricName)
        {
        MBeanMetric metric = findMetric(sMetricName, list);

        if (metric == null)
            {
            return false;
            }

        Map<String, String> mapMetricTags = metric.getTags();

        if (mapMetricTags.size() > mapTags.size())
            {
            Set<String> setTag = new HashSet<>(mapMetricTags.keySet());
            setTag.removeAll(mapTags.keySet());
            fail("Metric " + sMetricName + " has extra tags: " + setTag);
            }

        if (mapMetricTags.size() < mapTags.size())
            {
            Set<String> setTag = new HashSet<>(mapTags.keySet());
            setTag.removeAll(mapMetricTags.keySet());
            fail("Metric " + sMetricName + " has missing tags: " + setTag);
            }

        for (Map.Entry<String, String> enryTag : mapTags.entrySet())
            {
            String sKey = enryTag.getKey();
            String sValue = enryTag.getValue();

            assertThat(mapMetricTags, hasKey(sKey));
            if (sValue != null)
                {
                assertThat("Metric " + sMetricName + " has wrong value for tag '"
                           + sKey + "'", mapMetricTags.get(sKey), is(sValue));
                }
            }

        return true;
        }

    private Supplier<Registry> registrySupplier()
        {
        return () -> s_registry;
        }

    private String getNodeMBeanName()
        {
        return getMBeanName("*:" + Registry.NODE_TYPE + ",*");
        }

    private String getCacheMBeanName(String sCacheName, boolean fBack)
        {
        String sTier = fBack ? "back" : "front";
        return getMBeanName(String.format("*:%s,name=%s,tier=%s,*", Registry.CACHE_TYPE, sCacheName, sTier));
        }

    private String getServiceMBeanName(String sName)
        {
        return getMBeanName(Registry.SERVICE_TYPE, sName);
        }

    private String getStorageMBeanName(String sCache)
        {
        return getMBeanName(String.format("*:%s,cache=%s,*", Registry.STORAGE_MANAGER_TYPE, sCache));
        }

    private String getPartitionAssignmentMBeanName(String sService)
        {
        return getMBeanName(String.format("*:%s,service=%s,*", Registry.PARTITION_ASSIGNMENT_TYPE, sService));
        }

    private String getMBeanName(String sType, String sName)
        {
        return getMBeanName(String.format("*:%s,name=%s,*", sType, sName));
        }

    private String getMBeanName(String sPattern)
        {
        Set<String> set = s_registry.getMBeanServerProxy().local().queryNames(sPattern, null);

        return set.stream()
                .findFirst()
                .map(s -> s.substring(s.indexOf(':') + 1))
                .orElseThrow(() -> new AssertionError("Did not find an MBean matching " + sPattern));
        }


    private MBeanMetric findMetric(String sName, List<MBeanMetric> list)
        {
        return list.stream()
                .filter(m -> m.getName().equals(sName))
                .findFirst()
                .orElse(null);
        }


    private Map<String, String> getClusterTags(boolean fGlobal)
        {
        Map<String, String> mapTag = new HashMap<>();
        Cluster cluster = CacheFactory.ensureCluster();
        Member member = cluster.getLocalMember();

        mapTag.put(GLOBAL_TAG_CLUSTER, cluster.getClusterName());
        if (fGlobal)
            {
            mapTag.put(GLOBAL_TAG_SITE, member.getSiteName());
            mapTag.put(GLOBAL_TAG_MACHINE, member.getMachineName());
            mapTag.put(GLOBAL_TAG_ROLE, member.getRoleName());
            }

        return mapTag;
        }

    private Map<String, String> getCommonTags()
        {
        Map<String, String> mapTag = getClusterTags(true);
        Cluster cluster = CacheFactory.ensureCluster();
        Member member = cluster.getLocalMember();

        mapTag.put(GLOBAL_TAG_MEMBER, member.getMemberName());

        return mapTag;
        }

    private Map<String, String> getCommonTagsWithNodeId()
        {
        Map<String, String> mapTag = getCommonTags();
        Cluster cluster = CacheFactory.ensureCluster();
        Member member = cluster.getLocalMember();

        mapTag.put("nodeId", String.valueOf(member.getId()));

        return mapTag;
        }

    private static <K, V> NamedCache<K, V> getCache(String sCache)
        {
        return CacheFactory.getCache(sCache);
        }


    // ----- inner class: MetricsRegistryAdapterStub  -----------------------

    /**
     * A stub {@link MetricsRegistryAdapter} to capture registered metrics.
     */
    public static class MetricsRegistryAdapterStub
            implements MetricsRegistryAdapter
        {
        // ----- MetricsRegistryAdapterStub methods -------------------------

        int metricCount()
            {
            return f_mapMetric.size();
            }

        List<MBeanMetric> getMetrics()
            {
            return new ArrayList<>(f_mapMetric.values());
            }

        void removeMatching(String sPrefix)
            {
            f_mapMetric.keySet().removeIf(identifier -> identifier.getName().startsWith(sPrefix));
            }

        // ----- MetricsRegistryAdapter methods -----------------------------

        @Override
        public void register(MBeanMetric metric)
            {
            f_mapMetric.put(metric.getIdentifier(), metric);
            }

        @Override
        public void remove(MBeanMetric.Identifier identifier)
            {
            f_mapMetric.remove(identifier);
            }

        private final Map<MBeanMetric.Identifier, MBeanMetric> f_mapMetric = new ConcurrentHashMap<>();
        }

    // ----- constants ------------------------------------------------------

    private static final String TEST_CACHE = "dist-test-1234";

    private static final String TEST_NEAR_CACHE = "near-1234";

    private static final String TEST_WRITE_THROUGH_CACHE = "write-through-1234";

    private static final String TEST_WRITE_BEHIND_CACHE = "write-behind-1234";


    // ----- data members ---------------------------------------------------

    private static Registry s_registry;

    private static ExtensibleConfigurableCacheFactory s_eccfClient;
    }
