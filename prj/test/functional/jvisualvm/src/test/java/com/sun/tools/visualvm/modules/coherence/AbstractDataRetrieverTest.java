/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import com.tangosol.coherence.component.application.console.Coherence;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.net.URL;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.management.ObjectName;

import org.junit.*;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;

/**
 * Tests for basic data retriever functionality using the VisualVM model API.
 *
 * Note: Not all the values returned from each different data call can be tested
 * as some are not deterministic such as a machines load average, memory available, etc.
 * as well as some cache specific values such as actual MB usage.
 *
 * @author  tam  2013.11.18
 * @since   12.1.3
 */
public abstract class AbstractDataRetrieverTest
        extends AbstractVisualVMTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new test.
     *
     */
    public AbstractDataRetrieverTest()
        {
        model = VisualVMModel.getInstance();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Shutdown tests after a test run.
     * Note: the startup is done in the class that extends this test.
     */
    @AfterClass
    public static void _shutdown()
        {
        shutdownCacheServers();
        }

    @Test
    public void runMasterTest()
        {
        ProtectionDomain domain = Coherence.class.getProtectionDomain();
        CodeSource source = domain == null ? null : domain.getCodeSource();
        URL urlSrc = source == null ? null : source.getLocation();
        System.out.println("--> Coherence component loaded from: " + urlSrc);

        try
            {
            testClusterData();
            testMachineData();
            testMemberData();
            testCacheData();
            testServiceData();
            testNodeStorageData();
            }
        catch (Exception e)
            {
            e.printStackTrace();

            throw new RuntimeException("Test failed: " + e.getMessage());
            }
        }

    /**
     * Test the retrieval of ClusterData via the VisualVMModel.
     */
    private void testClusterData()
        {
        List<Map.Entry<Object, Data>> clusterData;

        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        clusterData = model.getData(VisualVMModel.DataType.CLUSTER);

        validateData(VisualVMModel.DataType.CLUSTER, clusterData, 1);

        setCurrentDataType(VisualVMModel.DataType.CLUSTER);

        // ensure we have correct values
        for (Map.Entry<Object, Data> entry : clusterData)
            {
            output(entry.toString());
            validateColumn(ClusterData.CLUSTER_NAME, entry, CLUSTER_NAME);
            validateColumn(ClusterData.CLUSTER_SIZE, entry, 2);
            validateColumn(ClusterData.VERSION, entry, CacheFactory.VERSION);
            validateColumn(ClusterData.DEPARTURE_COUNT, entry, 0L);
            }
        }

    /**
     * Test NodeStorageData.
     */
    private void testNodeStorageData()
        {
        List<Map.Entry<Object, Data>> nodeStorageData;

        assertClusterReady();

        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        nodeStorageData = model.getData(VisualVMModel.DataType.NODE_STORAGE);
        Assert.assertThat(nodeStorageData.size(), is(2));
        }

    /**
     * Test the retrieval of MachineData via the VisualVMModel.
     */
    private void testMachineData()
        {
        List<Map.Entry<Object, Data>> machineData;

        assertClusterReady();

        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        machineData = model.getData(VisualVMModel.DataType.MACHINE);

        validateData(VisualVMModel.DataType.MACHINE, machineData, 1);

        // we don't do any further validation as all values are non-deterministic
        }

    /**
     * Test the retrieval of MemberData via the VisualVMModel.
     */
    private void testMemberData()
        {
        List<Map.Entry<Object, Data>> memberData;

        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        memberData = model.getData(VisualVMModel.DataType.MEMBER);

        setCurrentDataType(VisualVMModel.DataType.MEMBER);
        validateData(VisualVMModel.DataType.MEMBER, memberData, 2);

        Map.Entry<Object, Data> entryNode1 = memberData.get(0);
        Map.Entry<Object, Data> entryNode2 = memberData.get(1);

        Assert.assertNotNull(entryNode1);
        Assert.assertNotNull(entryNode2);

        setCurrentDataType(VisualVMModel.DataType.MEMBER);

        // validate node 1 and 2 exists and has deterministic data
        validateColumn(MemberData.NODE_ID, entryNode1, 1);
        validateColumn(MemberData.NODE_ID, entryNode2, 2);
        validateColumn(MemberData.ROLE_NAME, entryNode1, ROLE_NAME);
        validateColumn(MemberData.ROLE_NAME, entryNode2, ROLE_NAME);
        // this is always defaulted to true and only updated to false within the Members Tab
        validateColumn(MemberData.STORAGE_ENABLED, entryNode2, "true");
        }

    /**
     * Test the retrieval of CacheData via the VisualVMModel.
     */
    private void testCacheData() throws Exception
        {
        final int                     INSERT1_COUNT = 10000;
        final int                     INSERT2_COUNT = 7500;
        final int                     INSERT3_COUNT = 100;
        final int                     DATA_SIZE     = 250;

        List<Map.Entry<Object, Data>> cacheData;
        List<Map.Entry<Object, Data>> cacheDetailData;
        List<Map.Entry<Object, Data>> cacheStorageData;

        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        cacheData = model.getData(VisualVMModel.DataType.CACHE);
        setCurrentDataType(VisualVMModel.DataType.CACHE);

        setClientProperties();

        // now add 2 caches on 2 different services - note we are connecting via Extend
        NamedCache nc1 = CacheFactory.getCache(DIST1_CACHE);
        NamedCache nc2 = CacheFactory.getCache(DIST2_CACHE);
        NamedCache nc3 = CacheFactory.getCache(REPL_CACHE);

        populateRandomData(nc1, INSERT1_COUNT, DATA_SIZE);
        populateRandomData(nc2, INSERT2_COUNT, DATA_SIZE);
        populateRandomData(nc3, INSERT3_COUNT, DATA_SIZE);

        Assert.assertTrue(nc1.size() == INSERT1_COUNT);
        Assert.assertTrue(nc2.size() == INSERT2_COUNT);
        Assert.assertTrue(nc3.size() == INSERT3_COUNT);

        waitForRefresh();

        model.refreshStatistics(requestSender);
        cacheData = model.getData(VisualVMModel.DataType.CACHE);

        validateData(VisualVMModel.DataType.CACHE, cacheData, 3);

        Map.Entry<Object, Data> entryCache1 = cacheData.get(0);
        Map.Entry<Object, Data> entryCache2 = cacheData.get(1);
        Map.Entry<Object, Data> entryCache3 = cacheData.get(2);

        // validate the data returned where its deterministic
        validateColumnNotNull(CacheData.SIZE, entryCache1);
        validateColumn(CacheData.CACHE_NAME, entryCache1, getCacheName(DIST1_SERVICE, DIST1_CACHE));

        validateColumnNotNull(CacheData.SIZE, entryCache2);
        validateColumn(CacheData.CACHE_NAME, entryCache2, getCacheName(DIST2_SERVICE, DIST2_CACHE));

        validateColumnNotNull(CacheData.SIZE, entryCache3);
        validateColumn(CacheData.CACHE_NAME, entryCache3, getCacheName(REPLICATED_SERVICE, REPL_CACHE));

        validateColumn(CacheData.UNIT_CALCULATOR, entryCache1, "BINARY");

        // select the first service, which should then generate both CacheDetailData and
        // CacheStorageManagerData on next refresh
        model.setSelectedCache(new Pair<String, String>(DIST1_SERVICE, DIST1_CACHE));

        // do 2 gets
        nc1.get(0);
        nc1.get(INSERT1_COUNT - 1);

        waitForRefresh();
        model.refreshStatistics(requestSender);

        cacheDetailData  = model.getData(VisualVMModel.DataType.CACHE_DETAIL);
        cacheStorageData = model.getData(VisualVMModel.DataType.CACHE_STORAGE_MANAGER);

        setCurrentDataType(VisualVMModel.DataType.CACHE_DETAIL);
        validateData(VisualVMModel.DataType.CACHE_DETAIL, cacheDetailData, 2);

        setCurrentDataType(VisualVMModel.DataType.CACHE_STORAGE_MANAGER);
        validateData(VisualVMModel.DataType.CACHE_STORAGE_MANAGER, cacheStorageData, 2);

        // validate the CacheDetail data
        Map.Entry<Object, Data> entryDetail1 = cacheDetailData.get(0);
        Map.Entry<Object, Data> entryDetail2 = cacheDetailData.get(1);

        validateColumn(CacheDetailData.NODE_ID, entryDetail1, 1);
        validateColumn(CacheDetailData.NODE_ID, entryDetail2, 2);

        // call the dependent tests as we can't guarantee execution order in JUnit
        testPersistenceData();
        testProxyData();
        }

    /**
     * Test the retrieval of ServiceData via the VisualVMModel.
     */
    public void testServiceData()
        {
        List<Map.Entry<Object, Data>> serviceData;
        List<Map.Entry<Object, Data>> serviceMemberData;

        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        serviceData = model.getData(VisualVMModel.DataType.SERVICE);
        setCurrentDataType(VisualVMModel.DataType.SERVICE);

        // we should have nine services:
        // ManagementHttpProxy,
        // DistributedScheme1, DistributedScheme2, ExtendTcpProxyService, ReplicatedScheme, XDistributedSchemeRAM,
        // XDistributedSchemeFlash, FederatedPartitionedPofCache
        validateData(VisualVMModel.DataType.SERVICE, serviceData, 8);

        Map.Entry<Object, Data> distScheme1 = null, distScheme2 = null,
                proxyService = null, replicatedService = null;

        for (Map.Entry<Object, Data> entry : serviceData)
            {
            switch ((String) entry.getKey())
                {
                case DIST1_SERVICE:
                    distScheme1 = entry;
                    break;
                case DIST2_SERVICE:
                    distScheme2 = entry;
                    break;
                case PROXY_SERVICE:
                    proxyService = entry;
                    break;
                case REPLICATED_SERVICE:
                    replicatedService = entry;
                    break;
                }
            }

        Assert.assertNotNull(distScheme1);
        Assert.assertNotNull(distScheme2);
        Assert.assertNotNull(proxyService);
        Assert.assertNotNull(replicatedService);

        setCurrentDataType(VisualVMModel.DataType.SERVICE);

        // validate distributed caches
        validateDistributedCacheService(distScheme1, DIST1_SERVICE);
        validateDistributedCacheService(distScheme2, DIST2_SERVICE);

        // validate proxy server
        validateColumn(ServiceData.STATUS_HA, proxyService, "n/a");
        validateColumn(ServiceData.MEMBERS, proxyService, 2);
        validateColumn(ServiceData.STORAGE_MEMBERS, proxyService, 0);

        // validate replicated scheme
        validateColumn(ServiceData.STATUS_HA, distScheme1, "NODE-SAFE");
        validateColumn(ServiceData.STORAGE_MEMBERS, distScheme1, 2);

        // set the selected service and refresh to get ServiceMemberData
        model.setSelectedService(DIST1_SERVICE);

        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        serviceMemberData = model.getData(VisualVMModel.DataType.SERVICE_DETAIL);
        setCurrentDataType(VisualVMModel.DataType.SERVICE_DETAIL);

        validateData(VisualVMModel.DataType.SERVICE_DETAIL, serviceMemberData, 2);

        // ensure we have correct values
        int nNode = 1;

        for (Map.Entry<Object, Data> entry : serviceMemberData)
            {
            validateColumn(ServiceMemberData.NODE_ID, entry, nNode++);
            validateColumn(ServiceMemberData.THREAD_COUNT, entry, 10);
            validateColumn(ServiceMemberData.THREAD_IDLE_COUNT, entry, 10);
            validateColumn(ServiceMemberData.THREAD_UTILISATION_PERCENT, entry, 0.0f);
            }
        }

    /**
     * Test the retrieval of ProxyData via the VisualVMModel.
     * Note: Called from testCacheData().
     */
    public void testProxyData()
        {
        List<Map.Entry<Object, Data>> proxyData;

        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.setIncludeNameService(false);
        model.refreshStatistics(requestSender);
        proxyData = model.getData(VisualVMModel.DataType.PROXY);
        setCurrentDataType(VisualVMModel.DataType.PROXY);

        validateData(VisualVMModel.DataType.PROXY, proxyData, 2);

        // ensure we have correct values
        int  nNode           = 1;
        int  cConnection     = 0;
        long cTotalBytesRec  = 0L;
        long cTotalBytesSent = 0L;
        long cTotalMsgRec    = 0L;
        long cTotalMsgSent   = 0L;

        for (Map.Entry<Object, Data> entry : proxyData)
            {
            cConnection     += (Integer) entry.getValue().getColumn(ProxyData.CONNECTION_COUNT);
            cTotalBytesRec  += (Long) entry.getValue().getColumn(ProxyData.TOTAL_BYTES_RECEIVED);
            cTotalBytesSent += (Long) entry.getValue().getColumn(ProxyData.TOTAL_BYTES_SENT);
            cTotalMsgRec    += (Long) entry.getValue().getColumn(ProxyData.TOTAL_MSG_RECEIVED);
            cTotalMsgSent   += (Long) entry.getValue().getColumn(ProxyData.TOTAL_MSG_SENT);
            }

        Assert.assertTrue("Total number of connections should be 1 but is " + cConnection, cConnection == 1);
        Assert.assertTrue("Total bytes Rec should be > 0", cTotalBytesRec > 0L);
        Assert.assertTrue("Total bytes Sent should be > 0", cTotalBytesSent > 0L);
        Assert.assertTrue("Total msg Rec should be > 0", cTotalMsgRec > 0L);
        Assert.assertTrue("Total msg Rec should be > 0", cTotalMsgSent > 0L);

        // Now include the name service
        // refresh the statistics
        model.setIncludeNameService(true);
        waitForRefresh();

        model.refreshStatistics(requestSender);
        proxyData = model.getData(VisualVMModel.DataType.PROXY);
        setCurrentDataType(VisualVMModel.DataType.PROXY);

        validateData(VisualVMModel.DataType.PROXY, proxyData, 2);

        for (Map.Entry<Object, Data> entry : proxyData)
            {
            String sServiceName = (String) entry.getValue().getColumn(ProxyData.SERVICE_NAME);

            Assert.assertTrue("Service name should be " + PROXY_SERVICE,
                              sServiceName.equals(PROXY_SERVICE));
            }
        }

    /**
     * Test the retrieval of PersistenceData via the VisualVMModel.
     * Note: Called from testCacheData().
     */
    public void testPersistenceData()
        {
        List<Map.Entry<Object, Data>> persistenceData;

        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        persistenceData = model.getData(VisualVMModel.DataType.PERSISTENCE);

        Assert.assertTrue("VisualVMModel does not report Coherence as 12.1.3 or above",
                          model.is1213AndAbove() != null && model.is1213AndAbove());
        setCurrentDataType(VisualVMModel.DataType.PERSISTENCE);

        // need to account for difference in coherence versions as persistence is
        // not on by default in 12.1.3 but is in 12.2.1.
        int cCount = model.getClusterVersion().startsWith("12.1.3") ? 3 : 5;

        validateData(VisualVMModel.DataType.PERSISTENCE, persistenceData, cCount);

        // the services will be ordered as above, alphabetically
        Map.Entry<Object, Data> entryPersistence1 = persistenceData.get(0);
        Map.Entry<Object, Data> entryPersistence2 = persistenceData.get(1);

        Assert.assertNotNull(entryPersistence1);
        Assert.assertNotNull(entryPersistence2);

        validateColumn(PersistenceData.SERVICE_NAME, entryPersistence1, DIST1_SERVICE);
        validateColumn(PersistenceData.SERVICE_NAME, entryPersistence2, DIST2_SERVICE);
        validateColumn(PersistenceData.SNAPSHOT_COUNT, entryPersistence1, 0);
        validateColumn(PersistenceData.SNAPSHOT_COUNT, entryPersistence2, 0);

        long cSnapshots = (Integer) entryPersistence1.getValue().getColumn(PersistenceData.SNAPSHOT_COUNT);

        Assert.assertEquals(0, cSnapshots);

        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate the required dist cache service
     * @param entry         entry to validate
     * @param sServiceName  the expected service name
     */
    private void validateDistributedCacheService(Map.Entry<Object, Data> entry, String sServiceName)
        {
        output("Validating service " + sServiceName);
        validateColumn(ServiceData.SERVICE_NAME, entry, sServiceName);
        validateColumn(ServiceData.STATUS_HA, entry, "NODE-SAFE");
        validateColumn(ServiceData.PARTITION_COUNT, entry, PARTITION_COUNT);
        validateColumn(ServiceData.PARTITIONS_ENDANGERED, entry, 0);
        validateColumn(ServiceData.PARTITIONS_UNBALANCED, entry, 0);
        validateColumn(ServiceData.PARTITIONS_PENDING, entry, 0);
        validateColumn(ServiceData.PARTITIONS_VULNERABLE, entry, PARTITION_COUNT);
        validateColumn(ServiceData.MEMBERS, entry, 2);
        validateColumn(ServiceData.STORAGE_MEMBERS, entry, 2);
        }

    /**
     * Set properties for the client.
     */
    private void setClientProperties() throws Exception
        {
        // now that the cluster is started, set the properties to turn off local clustering
        // so we don't connect with the process just started
        System.setProperty("tangosol.coherence.tcmp.enabled", "false");
        System.setProperty("tangosol.coherence.override", "test-client-cache-override.xml");
        System.setProperty("tangosol.coherence.cacheconfig", "test-client-cache-config.xml");

        ObjectName objName = requestSender
                .getCompleteObjectName(new ObjectName("Coherence:type=Node,nodeId=1,*")).iterator().next();
        // get the multicast port
        int nMulticastPortNode1 = Integer.parseInt(requestSender.getAttribute(objName, "MulticastPort"));

        System.setProperty("remote.port", Integer.toString(nMulticastPortNode1));
        }

    /**
     * Return the cache name given a service and cache.
     *
     * @param sService  service name
     * @param sCache    cache name
     * @return the resultant name
     */
    private String getCacheName(String sService, String sCache)
        {
        return new String(sService + " / " + sCache);
        }

    /**
     * Wait for a certain amount of time to ensure the JMX management
     * refresh has completed.
     */
    private void waitForRefresh()
        {
        wait("Sleeping to ensure JMX stats updated for next refresh", 15000L);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Service and cache details.
     */
    private static final String DIST1_SERVICE      = "DistributedScheme1";
    private static final String DIST2_SERVICE      = "DistributedScheme2";
    private static final String PROXY_SERVICE      = "ExtendTcpProxyService";
    private static final String REPLICATED_SERVICE = "ReplicatedScheme";
    private static final String NAME_SERVICE       = "NameService";
    private static final String DIST2_CACHE        = "dist2-test";
    private static final String DIST1_CACHE        = "dist1-test";
    private static final String REPL_CACHE         = "repl1";
    private static final String RAM_CACHE          = "ram1";
    private static final String FLASH_CACHE        = "flash1";
    }
