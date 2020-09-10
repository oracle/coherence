/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheDetailData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ClusterData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationDestinationDetailsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationOriginDetailsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MemberData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Pair;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.PersistenceData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ProxyData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.RamJournalData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceMemberData;
import com.tangosol.coherence.component.application.console.Coherence;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.net.URL;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Assert;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
        setModel(VisualVMModel.getInstance());
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
            if (isCommercial())
                {
                testFederationData();
                }
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

        VisualVMModel model = getModel();
        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(getRequestSender());
        clusterData = model.getData(VisualVMModel.DataType.CLUSTER);

        validateData(VisualVMModel.DataType.CLUSTER, clusterData, 1);

        setCurrentDataType(VisualVMModel.DataType.CLUSTER);

        // ensure we have correct values
        for (Map.Entry<Object, Data> entry : clusterData)
            {
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

        VisualVMModel model = getModel();
        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(getRequestSender());
        nodeStorageData = model.getData(VisualVMModel.DataType.NODE_STORAGE);
        assertThat(nodeStorageData.size(), is(2));
        }

    /**
     * Test the retrieval of MachineData via the VisualVMModel.
     */
    private void testMachineData()
        {
        List<Map.Entry<Object, Data>> machineData;

        VisualVMModel model = getModel();
        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(getRequestSender());
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

        VisualVMModel model = getModel();
        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(getRequestSender());
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

        VisualVMModel model         = getModel();
        RequestSender requestSender = getRequestSender();
        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(requestSender);
        cacheData = model.getData(VisualVMModel.DataType.CACHE);
        setCurrentDataType(VisualVMModel.DataType.CACHE);

        setClientProperties();
        ExtensibleConfigurableCacheFactory eccf = getECCF();

        // now add 2 caches on 2 different services - note we are connecting via Extend
        NamedCache nc1 = eccf.ensureCache(DIST1_CACHE, null);
        NamedCache nc2 = eccf.ensureCache(DIST2_CACHE, null);
        NamedCache nc3 = eccf.ensureCache(REPL_CACHE, null);

        populateRandomData(nc1, INSERT1_COUNT, DATA_SIZE);
        populateRandomData(nc2, INSERT2_COUNT, DATA_SIZE);
        populateRandomData(nc3, INSERT3_COUNT, DATA_SIZE);

        assertTrue(nc1.size() == INSERT1_COUNT);
        assertTrue(nc2.size() == INSERT2_COUNT);
        assertTrue(nc3.size() == INSERT3_COUNT);

        waitForRefresh();

        model.refreshStatistics(requestSender);
        cacheData = model.getData(VisualVMModel.DataType.CACHE);

        Map.Entry<Object, Data> distCache1 = getData(cacheData, new Pair<>(DIST1_SERVICE, DIST1_CACHE));
        Map.Entry<Object, Data> distCache2 = getData(cacheData, new Pair<>(DIST2_SERVICE, DIST2_CACHE));
        Map.Entry<Object, Data> replCache1 = getData(cacheData, new Pair<>(REPLICATED_SERVICE, REPL_CACHE));

        // validate the data returned where its deterministic
        validateColumnNotNull(CacheData.SIZE, distCache1);
        validateColumn(CacheData.CACHE_NAME, distCache1, getCacheName(DIST1_SERVICE, DIST1_CACHE));

        validateColumnNotNull(CacheData.SIZE, distCache2);
        validateColumn(CacheData.CACHE_NAME, distCache2, getCacheName(DIST2_SERVICE, DIST2_CACHE));

        validateColumnNotNull(CacheData.SIZE, replCache1);
        validateColumn(CacheData.CACHE_NAME, replCache1, getCacheName(REPLICATED_SERVICE, REPL_CACHE));

        validateColumn(CacheData.UNIT_CALCULATOR, distCache1, "BINARY");

        // select the first service, which should then generate both CacheDetailData and
        // CacheStorageManagerData on next refresh
        model.setSelectedCache(new Pair<>(DIST1_SERVICE, DIST1_CACHE));

        // do 2 gets
        nc1.get(0);
        nc1.get(INSERT1_COUNT - 1);

        waitForRefresh();
        model.refreshStatistics(getRequestSender());

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
        if (isCommercial())
            {
            testElasticData();
            }
        }

    /**
     * Test the retrieval of ServiceData via the VisualVMModel.
     */
    public void testServiceData()
        {
        List<Map.Entry<Object, Data>> serviceData;
        List<Map.Entry<Object, Data>> serviceMemberData;

        VisualVMModel model = getModel();
        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(getRequestSender());
        serviceData = model.getData(VisualVMModel.DataType.SERVICE);
        setCurrentDataType(VisualVMModel.DataType.SERVICE);

        // the services will be ordered as above, alphabetically
        Map.Entry<Object, Data> distService1       = getData(serviceData, DIST1_SERVICE);
        Map.Entry<Object, Data> distService2       = getData(serviceData, DIST2_SERVICE);
        Map.Entry<Object, Data> extendProxyService = getData(serviceData, PROXY_SERVICE);
        Map.Entry<Object, Data> federatedService   = getData(serviceData, FEDERATED_SERVICE);
        Map.Entry<Object, Data> replicatedService  = getData(serviceData, REPLICATED_SERVICE);

        Assert.assertNotNull(distService1);
        Assert.assertNotNull(distService2);
        Assert.assertNotNull(extendProxyService);

        // test Federation
        if (isCommercial())
            {
            Assert.assertNotNull(federatedService);
            validateColumn(ServiceData.STATUS_HA, federatedService, "NODE-SAFE");
            validateColumn(ServiceData.STORAGE_MEMBERS, federatedService, 2);
            }

        setCurrentDataType(VisualVMModel.DataType.SERVICE);

        // validate distributed caches
        validateDistributedCacheService(distService1, DIST1_SERVICE);
        validateDistributedCacheService(distService2, DIST2_SERVICE);

        // validate proxy server
        validateColumn(ServiceData.STATUS_HA, extendProxyService, "n/a");
        validateColumn(ServiceData.MEMBERS, extendProxyService, 2);
        validateColumn(ServiceData.STORAGE_MEMBERS, extendProxyService, 0);

        // validate replicated scheme
        validateColumn(ServiceData.STATUS_HA, replicatedService, "NODE-SAFE");
        validateColumn(ServiceData.STORAGE_MEMBERS, replicatedService, 2);

        // set the selected service and refresh to get ServiceMemberData
        model.setSelectedService(DIST1_SERVICE);

        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(getRequestSender());
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
     * Test the retrieval of FederationData via the VisualVMModel.
     */
    public void testFederationData() throws Exception
        {
        if (Boolean.getBoolean("com.oracle.coherence.jvisualvm.reporter.disabled"))
            {
            return;
            }

        final int                     INSERT_COUNT  = 100;
        final int                     INSERT_COUNT2 = 2 * INSERT_COUNT;
        final int                     DATA_SIZE     = 250;
        List<Map.Entry<Object, Data>> federationOriginData;
        List<Map.Entry<Object, Data>> federationOriginDetailsData;
        List<Map.Entry<Object, Data>> federationDestinationData;
        List<Map.Entry<Object, Data>> federationDestinationDetailsData;

        VisualVMModel model = getModel();
        assertClusterReady();

        NamedCache ncA = s_memberA1.getCache(FED_CACHE);
        populateRandomData(ncA, INSERT_COUNT, DATA_SIZE);

        assertThat(ncA.size(), is(INSERT_COUNT));

        NamedCache ncB = s_memberB1.getCache(FED_CACHE);
        populateRandomData(ncB, INSERT_COUNT2, DATA_SIZE);

        assertThat(ncB.size(),is(INSERT_COUNT2));

        waitForRefresh();

        // select a service participant
        model.setSelectedServiceParticipant(new Pair<String, String>(FEDERATED_SERVICE, "ClusterB"));
        // refresh the statistics
        model.refreshStatistics(getRequestSender());
        federationDestinationData = model.getData(VisualVMModel.DataType.FEDERATION_DESTINATION);
        setCurrentDataType(VisualVMModel.DataType.FEDERATION_DESTINATION);

        validateColumn(FederationData.Column.PARTICIPANT.getColumn(), federationDestinationData.get(0), "ClusterB");

        federationDestinationDetailsData = model.getData(VisualVMModel.DataType.FEDERATION_DESTINATION_DETAILS);
        setCurrentDataType(VisualVMModel.DataType.FEDERATION_DESTINATION_DETAILS);

        Assert.assertNotNull(federationDestinationDetailsData);
        List<Long> listDestEntriesSent = federationDestinationDetailsData.stream().
                                         map(e -> (Long) getColumn(
                                                 FederationDestinationDetailsData.Column.TOTAL_ENTRIES_SENT.getColumn() - 1, e)).
                                         collect(Collectors.toList());
        long lTotalDestEntriesSent     = listDestEntriesSent.stream().mapToLong(Long::longValue).sum();

        assertThat("Incorrect total destination entries sent with " + listDestEntriesSent,
                            lTotalDestEntriesSent, is(INSERT_COUNT * 1L));

        federationOriginData = model.getData(VisualVMModel.DataType.FEDERATION_ORIGIN);
        setCurrentDataType(VisualVMModel.DataType.FEDERATION_ORIGIN);
        String sParticipant = (String)getColumn(FederationData.Column.PARTICIPANT.getColumn(), federationOriginData.get(0));

        assertThat("Expected ClusterB, but got " + sParticipant, sParticipant, is("ClusterB"));

        federationOriginDetailsData = model.getData(VisualVMModel.DataType.FEDERATION_ORIGIN_DETAILS);
        setCurrentDataType(VisualVMModel.DataType.FEDERATION_ORIGIN_DETAILS);

        long lTotalOrigEntriesSent = federationOriginDetailsData.stream().
                                     map(e -> (Long) getColumn(
                                             FederationOriginDetailsData.Column.TOTAL_ENTRIES_RECEIVED.getColumn() - 1, e)).
                                     reduce(0L, Long::sum);

        assertThat(federationOriginDetailsData, is(notNullValue()));
        assertThat("Total origin entries sent should be positive.", lTotalOrigEntriesSent, is(greaterThan(0L)));
        }

    /**
     * Test the retrieval of RamJournalData and FlashJournalData via the VisualVMModel.
     * Note: Called from testCacheData().
     */
    public void testElasticData() throws Exception
        {
        final int                     RAM_INSERT_COUNT   = 1000;
        final int                     FLASH_INSERT_COUNT = 2000;
        final int                     DATA_SIZE          = 1000;

        List<Map.Entry<Object, Data>> ramJournalData;
        List<Map.Entry<Object, Data>> flashJournalData;

        // refresh the statistics
        VisualVMModel model = getModel();
        RequestSender requestSender = getRequestSender();
        model.refreshStatistics(requestSender);
        ramJournalData = model.getData(VisualVMModel.DataType.RAMJOURNAL);
        setCurrentDataType(VisualVMModel.DataType.RAMJOURNAL);

        // should have two entries from federation
        assertTrue("RamJournalData should be empty but size is "
                          + (ramJournalData == null ? null : ramJournalData.size()), ramJournalData == null
                              || ramJournalData.size() == 2);

        setClientProperties();
        ExtensibleConfigurableCacheFactory eccf = getECCF();

        NamedCache nc = eccf.ensureCache(RAM_CACHE, null);

        populateRandomData(nc, RAM_INSERT_COUNT, DATA_SIZE);
        assertTrue(nc.size() == RAM_INSERT_COUNT);

        waitForRefresh();
        model.refreshStatistics(requestSender);
        ramJournalData = model.getData(VisualVMModel.DataType.RAMJOURNAL);
        validateData(VisualVMModel.DataType.RAMJOURNAL, ramJournalData, 2);

        Map.Entry<Object, Data> entryRamJournal1 = ramJournalData.get(0);

        assertTrue(entryRamJournal1 != null
                          && ((Integer) entryRamJournal1.getValue().getColumn(RamJournalData.NODE_ID)) != 0);

        NamedCache nc2 = eccf.ensureCache(FLASH_CACHE, null);

        populateRandomData(nc2, FLASH_INSERT_COUNT, DATA_SIZE);
        assertThat(nc2.size(), is(FLASH_INSERT_COUNT));

        waitForRefresh();
        model.refreshStatistics(getRequestSender());
        flashJournalData = model.getData(VisualVMModel.DataType.FLASHJOURNAL);
        setCurrentDataType(VisualVMModel.DataType.FLASHJOURNAL);
        validateData(VisualVMModel.DataType.FLASHJOURNAL, flashJournalData, 2);

        Map.Entry<Object, Data> entryFlashJournal1 = flashJournalData.get(0);

        assertTrue(entryFlashJournal1 != null
                          && ((Integer) entryFlashJournal1.getValue().getColumn(RamJournalData.NODE_ID)) != 0);

        }

    /**
     * Test the retrieval of ProxyData via the VisualVMModel.
     * Note: Called from testCacheData().
     */
    public void testProxyData()
        {
        List<Map.Entry<Object, Data>> proxyData;

        VisualVMModel model = getModel();
        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.setIncludeNameService(false);
        model.refreshStatistics(getRequestSender());
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

        assertThat(cConnection, is(1));
        assertThat(cTotalBytesRec, is(greaterThan(0L)));
        assertThat( cTotalBytesSent, is(greaterThan(0L)));
        assertThat(cTotalMsgRec, is(greaterThan(0L)));
        assertThat(cTotalMsgSent, is(greaterThan(0L)));

        // Now include the name service
        // refresh the statistics
        model.setIncludeNameService(true);
        waitForRefresh();

        model.refreshStatistics(getRequestSender());
        proxyData = model.getData(VisualVMModel.DataType.PROXY);
        setCurrentDataType(VisualVMModel.DataType.PROXY);

        validateData(VisualVMModel.DataType.PROXY, proxyData, 2);

        for (Map.Entry<Object, Data> entry : proxyData)
            {
            String sServiceName = (String) entry.getValue().getColumn(ProxyData.SERVICE_NAME);

            assertThat(sServiceName, is(PROXY_SERVICE));
            }
        }

    /**
     * Test the retrieval of PersistenceData via the VisualVMModel.
     * Note: Called from testCacheData().
     */
    public void testPersistenceData()
        {
        List<Map.Entry<Object, Data>> persistenceData;

        VisualVMModel model = getModel();
        assertClusterReady();
        waitForRefresh();

        // refresh the statistics
        model.refreshStatistics(getRequestSender());
        persistenceData = model.getData(VisualVMModel.DataType.PERSISTENCE);

        assertTrue("VisualVMModel does not report Coherence as 12.1.3 or above",
                          model.is1213AndAbove() != null && model.is1213AndAbove());
        setCurrentDataType(VisualVMModel.DataType.PERSISTENCE);

        // need to account for difference in coherence versions as persistence is
        // not on by default in 12.1.3 but is in 12.2.1.
        int cCount = model.getClusterVersion().startsWith("12.1.3") ? 3 : 5;

        validateData(VisualVMModel.DataType.PERSISTENCE, persistenceData, cCount);

        // the services will be ordered as above, alphabetically
        Map.Entry<Object, Data> entryPersistence1 = persistenceData.get(0);
        Map.Entry<Object, Data> entryPersistence2 = persistenceData.get(1);

        assertThat(entryPersistence1, is(notNullValue()));
        assertThat(entryPersistence2, is(notNullValue()));

        validateColumn(PersistenceData.SERVICE_NAME, entryPersistence1, DIST1_SERVICE);
        validateColumn(PersistenceData.SERVICE_NAME, entryPersistence2, DIST2_SERVICE);
        validateColumn(PersistenceData.SNAPSHOT_COUNT, entryPersistence1, 0);
        validateColumn(PersistenceData.SNAPSHOT_COUNT, entryPersistence2, 0);

        int cSnapshots = (Integer) entryPersistence1.getValue().getColumn(PersistenceData.SNAPSHOT_COUNT);

        assertThat(cSnapshots, is(0));

        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate the required dist cache service
     * @param entry         entry to validate
     * @param sServiceName  the expected service name
     */
    private void validateDistributedCacheService(Map.Entry<Object, Data> entry, String sServiceName)
        {
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

        RequestSender requestSender = getRequestSender();
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

    /**
     * Retrieve a {@link ExtensibleConfigurableCacheFactory} instance using the
     * tangosol.coherence.cacheconfig system property.
     *
     * @return a {@link ExtensibleConfigurableCacheFactory}
     */
    private ExtensibleConfigurableCacheFactory getECCF()
         {
         return (ExtensibleConfigurableCacheFactory)
                 CacheFactory.getCacheFactoryBuilder()
                             .getConfigurableCacheFactory(System.getProperty("tangosol.coherence.cacheconfig"), null);
        }

    /**
     * Return the data for the given entry or null if none exists.
     * @param data  the data to query
     * @param oEntry the entry to look for
     * @return the data for the given entry or null if none exists.
     */
    private Map.Entry<Object, Data> getData(List<Map.Entry<Object, Data>> data, Object oEntry)
        {
        for (Map.Entry<Object, Data> v : data)
            {
            if (v.getKey().equals(oEntry))
                {
                return v;
                }
            }
            return null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Service and cache details.
     */
    private static final String DIST1_SERVICE      = "DistributedScheme1";
    private static final String DIST2_SERVICE      = "DistributedScheme2";
    private static final String PROXY_SERVICE      = "ExtendTcpProxyService";
    private static final String REPLICATED_SERVICE = "ReplicatedScheme";
    private static final String FEDERATED_SERVICE  = "FederatedPartitionedPofCache";
    private static final String NAME_SERVICE       = "NameService";
    private static final String DIST2_CACHE        = "dist2-test";
    private static final String DIST1_CACHE        = "dist1-test";
    private static final String REPL_CACHE         = "repl1";
    private static final String RAM_CACHE          = "ram1";
    private static final String FLASH_CACHE        = "flash1";
    private static final String FED_CACHE          = "fed";
    }
