/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package quorum;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.coherence.common.base.Blocking;
import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.AddressProvider;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.InvocationService;
import com.tangosol.net.IsDefaultCacheServerRunning;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.RequestPolicyException;
import com.tangosol.net.Service;

import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.net.partition.VersionedOwnership;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;

import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.util.InvocableMap.EntryProcessor;

import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.DoubleSum;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.NeverFilter;

import com.tangosol.util.processor.NumberIncrementor;

import common.AbstractFunctionalTest;

import common.TestInfrastructureHelper;

import data.Trade;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import java.io.Serializable;
import java.net.InetSocketAddress;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
* Test service Quorum.
*
* @author rhl  2009.10.05
*/
public class QuorumTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public QuorumTests()
        {
        super(FILE_CFG_CACHE);
        }

    /**
    * Constructor for subclassing.
    *
    * @param sCacheConfig  the cache config
    */
    protected QuorumTests(String sCacheConfig)
        {
        super(sCacheConfig);
        }


    // ----- test lifecycle -----------------------------------------------

    @After
    public void cleanup()
        {
        if (!f_CoherenceClusterMembers.isEmpty())
            {
            for (CoherenceClusterMember member : f_CoherenceClusterMembers)
                {
                member.close();
                }

            f_CoherenceClusterMembers.clear();

            NamedCache cacheEvents = getNamedCache("distribution-events");
            cleanupPartitionedCacheTest(cacheEvents);
            }
        CacheFactory.shutdown();
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Start a cache server for partitioned cache tests.
    *
    * @param sServer        the name of the server to start
    * @param fDistribution  is distribution allowed after starting the server?
    * @param fBackup        are backups allowed after starting the server?
    * @param cacheEvents    the events cache
    * @param props          the properties to use to start the server
    */
    protected void startPartitionedCacheServer(String sServer, boolean fDistribution,
            boolean fBackup, NamedCache cacheEvents, Properties props)
        {
        cacheEvents.put("DISTRIBUTION_ALLOWED", Boolean.valueOf(fDistribution));
        cacheEvents.put("BACKUP_ALLOWED", Boolean.valueOf(fBackup));

        props.setProperty("tangosol.coherence.override", getOverrideConfig());

        PartitionedService svc      = (PartitionedService) CacheFactory.getService("PartitionedCacheWithQuorum");
        int                cServers = svc.getOwnershipEnabledMembers().size();

        CoherenceClusterMember member = startCacheServer(sServer, "quorum", getCacheConfig(), props);

        // wait for the service used to be started on the new member
        Eventually.assertDeferred(() -> svc.getOwnershipEnabledMembers().size(), is(cServers + 1));

        registerConfigListener(sServer, "PartitionedCacheWithQuorum");
        }

    /**
    * Stop a cache server for partitioned cache tests
    *
    * @param sServer        the name of the server to stop
    * @param fDistribution  is distribution allowed after stopping the server?
    * @param fBackup        are backups allowed after stopping the server?
    * @param cacheEvents    the events cache
    * @param fGraceful      perform a graceful shutdown?
    */
    protected void stopPartitionedCacheServer(String sServer, boolean fDistribution,
            boolean fBackup, NamedCache cacheEvents, boolean fGraceful)
        {
        PartitionedService svc      = (PartitionedService) CacheFactory.getService("PartitionedCacheWithQuorum");
        int                cServers = svc.getOwnershipEnabledMembers().size();

        stopCacheServer(sServer, fGraceful);
        cacheEvents.put("DISTRIBUTION_ALLOWED", Boolean.valueOf(fDistribution));
        cacheEvents.put("BACKUP_ALLOWED", Boolean.valueOf(fBackup));

        // wait for death to be detected
        Eventually.assertDeferred(() -> svc.getOwnershipEnabledMembers().size(),
            is(cServers - 1));
        }

    /**
    * Return the partition assignments array for the partitioned service of
    * specified name.
    *
    * @param sServiceName  the name of the partitioned service
    *
    * @return the partition assignments array
    */
    protected int[][] getPartitionAssignments(String sServiceName)
        {
        try
            {
            Service          service     = CacheFactory.ensureCluster().getService(sServiceName);
            SafeService      serviceSafe = (SafeService) service;
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

            return serviceReal.getPartitionAssignments();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Debugging routine to print the partition ownership for the specified
    * partitioned service.
    *
    * @param sServiceName  the name of the partitioned service
    */
    protected void debugPrintOwnership(String sServiceName)
        {
        try
            {
            Service          service     = CacheFactory.ensureCluster().getService(sServiceName);
            SafeService      serviceSafe = (SafeService) service;
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

            Base.out(serviceReal.reportOwnership(Boolean.TRUE));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Clean up test resources used by a partitioned cache test.
    *
    * @param cacheEvents  the events cache
    */
    protected void cleanupPartitionedCacheTest(NamedCache cacheEvents)
        {
        cacheEvents.remove("DISTRIBUTION_ALLOWED");
        cacheEvents.remove("BACKUP_ALLOWED");

        String sExceptions = "";
        for (Iterator iter = cacheEvents.entrySet().iterator(); iter.hasNext(); )
            {
            Entry     entry = (Entry) iter.next();
            Exception e     = (Exception) entry.getValue();

            sExceptions += "\n" + printStackTrace(e);
            }
        assertEquals("Detected policy exceptions:\n" + sExceptions, 0, cacheEvents.size());
        cacheEvents.clear();
        }

    /**
    * Test partitioned-cache quorum.
    * <p/>
    * The test process joins as an ownership-disabled service member,
    * and the various cache read and write operations are tested as
    * the service membership changes.
    */
    @Test
    public void testPartitionedCache0()
            throws InterruptedException
        {
        NamedCache cache       = getNamedCache("dist-quorum0");
        NamedCache cacheEvents = getNamedCache("distribution-events");
        Properties props       = new Properties();

        props.setProperty("test.quorum.test.partitioned0", "true");

        try
            {
            // there are 0 cache servers running
            startPartitionedCacheServer(
                    "testPartitionedCache0-0", false, false, cacheEvents, props);

            // there is 1 cache server running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/false);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            startPartitionedCacheServer(
                    "testPartitionedCache0-1", false, false, cacheEvents, props);

            // there are 2 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/false);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            startPartitionedCacheServer(
                    "testPartitionedCache0-2", false, false, cacheEvents, props);

            // there are 3 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            startPartitionedCacheServer(
                    "testPartitionedCache0-3", true, true, cacheEvents, props);

            // there are 4 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            startPartitionedCacheServer(
                    "testPartitionedCache0-4", true, true, cacheEvents, props);

            // there are 5 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            startPartitionedCacheServer(
                    "testPartitionedCache0-5", true, true, cacheEvents, props);

            // there are 6 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            stopPartitionedCacheServer("testPartitionedCache0-0", true, true, cacheEvents, true);

            // there are 5 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            stopPartitionedCacheServer("testPartitionedCache0-1", true, true, cacheEvents, true);

            // there are 4 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            stopPartitionedCacheServer("testPartitionedCache0-2", false, false, cacheEvents, true);

            // there are 3 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            stopPartitionedCacheServer("testPartitionedCache0-3", false, false, cacheEvents, true);

            // there are 2 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/false);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            startPartitionedCacheServer(
                    "testPartitionedCache0-6", false, false, cacheEvents, props);

            // there are 3 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            startPartitionedCacheServer(
                    "testPartitionedCache0-7", true, true, cacheEvents, props);

            // there are 4 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/false);

            startPartitionedCacheServer(
                    "testPartitionedCache0-8", true, true, cacheEvents, props);

            // there are 5 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            startPartitionedCacheServer(
                    "testPartitionedCache0-9", true, true, cacheEvents, props);

            // there are 6 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);
            }
        finally
            {
            // cleanup
            stopCacheServer("testPartitionedCache0-9");
            stopCacheServer("testPartitionedCache0-8");
            stopCacheServer("testPartitionedCache0-7");
            stopCacheServer("testPartitionedCache0-6");
            stopCacheServer("testPartitionedCache0-5");
            stopCacheServer("testPartitionedCache0-4");
            stopCacheServer("testPartitionedCache0-3"); // no-op in the common case
            stopCacheServer("testPartitionedCache0-2"); // no-op in the common case
            stopCacheServer("testPartitionedCache0-1"); // no-op in the common case
            stopCacheServer("testPartitionedCache0-0"); // no-op in the common case
            }
        }

    /**
    * Test partitioned-cache quorum.
    * <p/>
    * The test process joins as an ownership-disabled service member,
    * and the various cache read and write operations are tested as
    * the service membership changes.
    */
    @Test
    public void testPartitionedCache1()
            throws InterruptedException
        {
        NamedCache cache       = getNamedCache("dist-quorum1");
        NamedCache cacheEvents = getNamedCache("distribution-events");
        Properties props = new Properties();

        props.setProperty("test.quorum.test.partitioned1", "true");

        // there are 0 cache servers running
        startPartitionedCacheServer(
            "testPartitionedCache1-0", true, true, cacheEvents, props);

        try
            {
            // there is 1 cache server running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            startPartitionedCacheServer(
                    "testPartitionedCache1-1", true, true, cacheEvents, props);

            // there are 2 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            stopPartitionedCacheServer("testPartitionedCache1-0", true, true, cacheEvents, true);

            // there is 1 cache server running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);
            }
        finally
            {
            // cleanup
            stopCacheServer("testPartitionedCache1-1");
            stopCacheServer("testPartitionedCache1-0"); // no-op in the common case
            }
        }

    /**
    * Test partitioned-cache quorum.
    * <p/>
    * The test process joins as an ownership-disabled service member,
    * and the various cache read and write operations are tested as
    * the service membership changes.
    */
    @Test
    public void testPartitionedCache2()
            throws InterruptedException
        {
        NamedCache cache       = getNamedCache("dist-quorum2");
        NamedCache cacheEvents = getNamedCache("distribution-events");
        Properties props = new Properties();

        props.setProperty("test.quorum.test.partitioned2", "true");

        try
            {
            // there are 0 cache servers running
            startPartitionedCacheServer(
                    "testPartitionedCache2-0", false, false, cacheEvents, props);

            // there is 1 cache server running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            startPartitionedCacheServer(
                    "testPartitionedCache2-1", false, false, cacheEvents, props);

            // there are 2 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            startPartitionedCacheServer(
                    "testPartitionedCache2-2", true, true, cacheEvents, props);

            // there are 3 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);

            stopPartitionedCacheServer("testPartitionedCache2-0", false, false, cacheEvents, true);

            // there are 2 cache servers running
            // test that read/write quorums are enforced
            testCacheReadOperations(cache, /*fAllowed*/true);
            testCacheWriteOperations(cache, /*fAllowed*/true);
            }
        finally
            {
            // cleanup
            stopCacheServer("testPartitionedCache2-2");
            stopCacheServer("testPartitionedCache2-1");
            stopCacheServer("testPartitionedCache2-0");
            }
        }

    /**
    * Test partitioned-cache restore-quorum.
    */
    //@Test
    public void testPartitionedCache3()
            throws InterruptedException
        {
        NamedCache cache       = getNamedCache("dist-quorum3");
        NamedCache cacheEvents = getNamedCache("distribution-events");
        Properties props       = new Properties();
        int[][]    aiOwners;
        int        cOrphans;

        props.setProperty("test.quorum.test.partitioned3", "true");

        // there are 0 cache servers running
        startPartitionedCacheServer(
            "testPartitionedCache3-0", true, true, cacheEvents, props);

        // there is 1 cache server running
        startPartitionedCacheServer(
            "testPartitionedCache3-1", true, true, cacheEvents, props);

        // there are 2 cache servers running
        startPartitionedCacheServer(
            "testPartitionedCache3-2", true, true, cacheEvents, props);

        // there are 3 cache servers running
        startPartitionedCacheServer(
            "testPartitionedCache3-3", true, true, cacheEvents, props);

        // there are 4 cache servers running
        // check that there are no orphans
        aiOwners = getPartitionAssignments("PartitionedCacheWithQuorum");
        for (int i = 0; i < aiOwners.length; i++)
            {
            assertTrue(0 != aiOwners[i][0]);
            }

        stopPartitionedCacheServer("testPartitionedCache3-0", true, true, cacheEvents, false);

        // there are 3 cache servers running
        // check that there are no orphans
        aiOwners = getPartitionAssignments("PartitionedCacheWithQuorum");
        for (int i = 0; i < aiOwners.length; i++)
            {
            assertTrue(0 != aiOwners[i][0]);
            }

        stopPartitionedCacheServer("testPartitionedCache3-1", true, true, cacheEvents, false);

        // there are 2 cache servers running
        // check that there are orphans
        aiOwners = getPartitionAssignments("PartitionedCacheWithQuorum");
        cOrphans = 0;
        for (int i = 0; i < aiOwners.length; i++)
            {
            if (aiOwners[i][0] == 0)
                {
                ++cOrphans;
                }
            }

        // Note: cOrphans here should logically match the primary ownership of
        // testPartitionedCache3-1 before it was killed, however we don't
        // test for that here because of the possibility of in-flight transfers
        // at the point in time that testPartitionedCache3-1 was killed.  Instead,
        // just test for the presence of some orphans
        assertTrue(0 != cOrphans);

        stopPartitionedCacheServer("testPartitionedCache3-2", true, true, cacheEvents, true);
        // there is 1 cache server running
        // check that there are orphans
        aiOwners = getPartitionAssignments("PartitionedCacheWithQuorum");
        cOrphans = 0;
        for (int i = 0; i < aiOwners.length; i++)
            {
            if (aiOwners[i][0] == 0)
                {
                ++cOrphans;
                }
            }

        // Note: cOrphans here should logically match the primary ownership of
        // testPartitionedCache3-1 and testPartitionedCache3-2 before they were
        // killed, however we don't test for that here because of the
        // possibility of in-flight transfers at the point in time that
        // testPartitionedCache3-1 was killed.  Instead, just test for the
        // presence of some orphans
        assertTrue(0 != cOrphans);

        // cleanup
        stopCacheServer("testPartitionedCache3-3");
        }

    /**
    * Test proxy service quorum.
    * <p/>
    * The test process attempts to initiate TcpExtend connections as
    * the proxy service membership changes.
    */
    @Test
    public void testProxy0()
            throws Exception
        {
        String                     hostName = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        OptionsByType optionsByType   = createCacheServerOptions()
                .add(CacheConfig.of(getCacheConfig()))
                .add(SystemProperty.of("test.quorum.proxy", true))
                .add(SystemProperty.of("tangosol.coherence.extend.address", hostName))
                .add(SystemProperty.of("test.unicast.address", hostName))
                .add(SystemProperty.of("tangosol.coherence.extend.port", LocalPlatform.get().getAvailablePorts()));

        CoherenceCacheServer proxy0 = startCoherenceClusterMember(optionsByType, "testProxy0-0");
        Eventually.assertDeferred(() -> proxy0.isServiceRunning("TcpProxyService"), is(true));

        // 1 proxy server is running
        testExtendConnect(proxy0, /*fAllowed*/false);

        CoherenceCacheServer proxy1 = startCoherenceClusterMember(optionsByType, "testProxy0-1");
        Eventually.assertDeferred(() -> proxy1.isServiceRunning("TcpProxyService"), is(true));

        // 2 proxy servers are running
        testExtendConnect(proxy0, /*fAllowed*/false);
        testExtendConnect(proxy1, /*fAllowed*/false);

        CoherenceCacheServer proxy2 = startCoherenceClusterMember(optionsByType, "testProxy0-2");
        Eventually.assertDeferred(() -> proxy2.isServiceRunning("TcpProxyService"), is(true));

        // 3 proxy servers are running
        testExtendConnect(proxy0, /*fAllowed*/true);
        testExtendConnect(proxy1, /*fAllowed*/true);
        testExtendConnect(proxy2, /*fAllowed*/true);

        proxy0.close();  // 2 cache servers remaining

        Eventually.assertDeferred(() -> proxy1.getClusterSize(), is(2));
        Eventually.assertDeferred(() -> proxy1.invoke(new GetServiceMemberCount("TcpProxyService")), is(2));

        // 2 proxy servers are running
        testExtendConnect(proxy1, /*fAllowed*/false);
        testExtendConnect(proxy2, /*fAllowed*/false);

        CoherenceClusterMember proxy3 = startCoherenceClusterMember(optionsByType, "testProxy0-3");
        Eventually.assertDeferred(() -> proxy3.isServiceRunning("TcpProxyService"), is(true));

        // 3 proxy servers are running
        testExtendConnect(proxy1, /*fAllowed*/true);
        testExtendConnect(proxy2, /*fAllowed*/true);
        testExtendConnect(proxy3, /*fAllowed*/true);

        proxy1.close();
        proxy2.close();
        proxy3.close();
        }

    /**
    * Test member based cluster quorum policy.
    * <p/>
    * The test process attempts to artificially pause a server and verify that
    * it does not get disconnected from the cluster due to the
    * "timeout-survivor-quorum" setting.
    */
    @Test
    public void testMemberQuorum()
        {
        testClusterQuorum(
            new ServerConfig("testMemberQuorum-0", new String[]
                {
                "test.member.quorum", "3",
                }),
            new ServerConfig("testMemberQuorum-1", new String[]
                {
                "test.member.quorum", "3",
                }),
            new ServerConfig("testMemberQuorum-2", new String[]
                {
                "test.member.quorum", "3",
                })
            );
        }

    /**
    * Test member and role based cluster quorum policy.
    * <p/>
    * The test process attempts to artificially pause a server and verify that
    * it does not get disconnected from the cluster due to the
    * "timeout-survivor-quorum" setting and roles.
    */
    @Test
    public void testMemberRoleQuorum()
        {
        testClusterQuorum(
            new ServerConfig("testMemberQuorum-0q", new String[]
                {
                "test.member.role.quorum", "3",
                "coherence.role", "qualified",
                }),
            new ServerConfig("testMemberQuorum-1q", new String[]
                {
                "test.member.role.quorum", "3",
                "coherence.role", "qualified",
                }),
            new ServerConfig("testMemberQuorum-2q", new String[]
                {
                "test.member.role.quorum", "3",
                "coherence.role", "qualified",
                }),
            new ServerConfig("testMemberQuorum-1u", new String[]
                {
                "test.member.role.quorum", "3",
                "coherence.role", "unqualified",
                }),
            new ServerConfig("testMemberQuorum-2u", new String[]
                {
                "test.member.role.quorum", "3",
                "coherence.role", "unqualified",
                })
            );
        }

    /**
    * Test machine based cluster quorum policy.
    * <p/>
    * The test process attempts to artificially pause a server and verify that
    * it does not get disconnected from the cluster due to the
    * "timeout-machine-quorum" setting.
    */
    @Test
    public void testMachineQuorum()
        {
        testClusterQuorum(
            new ServerConfig("testMachineQuorum-0-1", new String[]
                {
                "test.machine.quorum", "3",
                "coherence.machine", "machine-0",
                }),
            new ServerConfig("testMemberQuorum-1-1", new String[]
                {
                "test.machine.quorum", "3",
                "coherence.machine", "machine-1",
                }),
            new ServerConfig("testMemberQuorum-2-1", new String[]
                {
                "test.machine.quorum", "3",
                "coherence.machine", "machine-2",
                })
            );
        }

    /**
    * Test site based cluster quorum policy.
    * <p/>
    * The test process attempts to artificially pause a server and verify that
    * it does not get disconnected from the cluster due to the
    * "timeout-site-quorum" setting.
    */
    @Test
    public void testSiteQuorum()
        {
        testClusterQuorum(
            new ServerConfig("testSiteQuorum-0-1", new String[]
                {
                "test.site.quorum", "3",
                "coherence.site", "site-0",
                }),
            new ServerConfig("testSiteQuorum-1-1", new String[]
                {
                "test.site.quorum", "3",
                "coherence.site", "site-1",
                }),
            new ServerConfig("testSiteQuorum-2-1", new String[]
                {
                "test.site.quorum", "3",
                "coherence.site", "site-2",
                })
            );
        }

    /**
    * Test site and role based cluster quorum policy.
    * <p/>
    * The test process attempts to artificially pause a server and verify that
    * it does not get disconnected from the cluster due to the
    * "timeout-site-quorum" setting.
    */
    @Test
    public void testSiteRoleQuorum()
        {
        testClusterQuorum(
            new ServerConfig("testSiteQuorum-0-1q", new String[]
                {
                "test.site.role.quorum", "3",
                "coherence.site", "site-0",
                "coherence.role", "qualified",
                }),
            new ServerConfig("testSiteQuorum-1-1q", new String[]
                {
                "test.site.role.quorum", "3",
                "coherence.site", "site-1",
                "coherence.role", "qualified",
                }),
            new ServerConfig("testSiteQuorum-2-1q", new String[]
                {
                "test.site.role.quorum", "3",
                "coherence.site", "site-2",
                "coherence.role", "qualified",
                }),
            new ServerConfig("testSiteQuorum-0-1u", new String[]
                {
                "test.site.role.quorum", "3",
                "coherence.site", "site-0",
                "coherence.role", "unqualified",
                }),
            new ServerConfig("testSiteQuorum-1-2u", new String[]
                {
                "test.site.role.quorum", "3",
                "coherence.site", "site-1",
                "coherence.role", "unqualified",
                })
            );
        }

    /**
    * Helper struct.
    */
    private class ServerConfig
        {
        public ServerConfig(String sName, String[] asProp)
            {
            Name  = sName;
            Props = new Properties();
            Props.setProperty("tangosol.coherence.override", getOverrideConfig());
            Props.setProperty("test.log.level", "3");
            for (int i = 0; i < asProp.length; i+=2)
                {
                Props.setProperty(asProp[i], asProp[i+1]);
                }
            }
        public final String Name;
        public final Properties Props;

        }

    /**
    * Test cluster quorum policy.
    * <p/>
    * The test process attempts to artificially pause a server and verify that
    * it does not get disconnected from the cluster due to the quorum setting.
    */
    private void testClusterQuorum(ServerConfig... aConfig)
        {
        CacheFactory.shutdown();

        // the operational override sets the timeout quorum to 3, which means
        // that a third server should no be "disconnected" if it times out;
        // also, and the publisher timeout is set to 5 seconds
        Properties propsThis = aConfig[0].Props; // this node
        System.getProperties().putAll(propsThis);

        SafeCluster clusterSafe = (SafeCluster) CacheFactory.ensureCluster();
        assertEquals("Invalid operational override",
            5000, clusterSafe.getDependencies().getPublisherResendTimeoutMillis());

        int cMembers = aConfig.length;
        try
            {
            CoherenceClusterMember[] aMember = new CoherenceClusterMember[cMembers];

            for (int i = 1; i < cMembers; i++)
                {
                aMember[i] = startCacheServer(aConfig[i].Name, "quorum", getCacheConfig(), aConfig[i].Props, false);
                }

            for (int i = 1; i < cMembers; i++)
                {
                final CoherenceClusterMember member = aMember[i];
                Eventually.assertDeferred(() -> m_helper.submitBool(member, IsDefaultCacheServerRunning.INSTANCE), is(true));
                }
            Eventually.assertDeferred(() -> clusterSafe.getMemberSet().size(), is(cMembers));

            com.tangosol.coherence.component.net.Cluster clusterReal = clusterSafe.getCluster();

            // turn off the publisher over unicast
            clusterReal.getPublisher().getUdpSocketUnicast().setTxDebugDropRate(100000);

            Eventually.assertDeferred(() -> clusterReal.getClusterService().getQuorumControl().getConvictedMembers().isEmpty(), is(false));

            assertTrue("Quorum policy failed",
                    clusterReal.isRunning() && clusterSafe.getMemberSet().size() == cMembers);

            clusterReal.getPublisher().getUdpSocketUnicast().setTxDebugDropRate(0);

            Eventually.assertDeferred(() -> clusterReal.getClusterService().getQuorumControl().getConvictedMembers().isEmpty(), is(true));

            assertTrue("Quorum policy failed",
                    clusterReal.isRunning() && clusterSafe.getMemberSet().size() == cMembers);
            }
        catch (Exception e)
            {
            out("QuorumTests.testClusterQuorum() Failed with exception: " +
                    e.getMessage() + "\n" + getStackTrace(e));
            }
        finally
            {
            // cleanup
            for (int i = 1; i < cMembers; i++)
                {
                stopCacheServer(aConfig[i].Name);
                }

            // clean-up changed system properties not to affect other tests
            System.getProperties().keySet().removeAll(propsThis.keySet());
            CacheFactory.shutdown();
            }
        }


    // ----- helpers ------------------------------------------------------

    protected CoherenceCacheServer startCoherenceClusterMember(OptionsByType optionsByType, String sName)
            throws IOException
        {
        optionsByType.add(DisplayName.of(sName));

        CoherenceCacheServer member = LocalPlatform.get().launch(CoherenceCacheServer.class, optionsByType.asArray());

        f_CoherenceClusterMembers.add(member);

        return member;
        }

    protected OptionsByType createCacheServerOptions()
        {
        OptionsByType optionsByType = OptionsByType.empty();

        optionsByType.add(ClassName.of(DefaultCacheServer.class));
        optionsByType.add(OperationalOverride.of(getOverrideConfig()));
        optionsByType.add(LocalHost.only());
        optionsByType.add(LocalStorage.enabled());

        return optionsByType;
        }

    /**
    * Re-populate the specified named cache
    *
    * @param cache     the cache to repopulate
    * @param fAllowed  are cache writes allowed?
    */
    protected void reloadCache(NamedCache cache, boolean fAllowed)
        {
        String sMsgNotEnforced = "Expected RequestPolicyException was not thrown; "
            + "cache writes should be disallowed";
        String sMsgUnexpected  = "Unexpected exception; cache writes should be allowed";
        try
            {
            Trade.fillRandom(cache, 64);
            azzert(fAllowed, sMsgNotEnforced);
            azzert(cache.size() >= 64);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        }

    /**
    * Test the write operations on the specified cache to ensure that the quorum
    * policy is properly enforced.
    *
    * @param cache     the cache to test
    * @param fAllowed  true iff read operations should be allowed on the cache
    */
    protected void testCacheWriteOperations(final NamedCache cache, final boolean fAllowed)
        {
        final String sMsgNotEnforced = "Expected RequestPolicyException was not thrown; "
            + "cache writes should be disallowed";
        final String sMsgUnexpected  = "Unexpected exception; cache writes should be allowed";

        // inner class used for testing mutable operations of entrySet()/keySet()/values()
        class ComponentCollectionProvider
            {
            public Collection getCollection()
                {
                return null;
                }
            }

        // inner class used for testing mutable operations of entrySet()/keySet()/values()
        class ComponentCollectionTester
            {
            public void testCollection(ComponentCollectionProvider provider)
                {
                Collection col;

                //
                // col.add(Object)
                //
                try
                    {
                    col = provider.getCollection();
                    col.add(new AbstractMap.SimpleEntry("String","String"));
                    azzert(fAllowed, sMsgNotEnforced);
                    }
                catch (UnsupportedOperationException e)
                    {
                    // ok
                    }
                catch (RequestPolicyException e)
                    {
                    azzert(!fAllowed, sMsgUnexpected + "; " + e);
                    }

                reloadCache(cache, fAllowed);

                //
                // col.clear()
                //
                try
                    {
                    col = provider.getCollection();
                    if (!col.isEmpty())
                        {
                        col.clear();
                        azzert(fAllowed, sMsgNotEnforced);
                        }
                    }
                catch (RequestPolicyException e)
                    {
                    azzert(!fAllowed, sMsgUnexpected + "; " + e);
                    }

                reloadCache(cache, fAllowed);

                //
                // col.remove(Object)
                //
                try
                    {
                    col = provider.getCollection();
                    Iterator iter = col.iterator();
                    if (iter.hasNext())
                        {
                        Object o = iter.next();
                        col.remove(o);
                        azzert(fAllowed, sMsgNotEnforced);
                        }
                    }
                catch (UnsupportedOperationException e)
                    {
                    // ok
                    }
                catch (RequestPolicyException e)
                    {
                    azzert(!fAllowed, sMsgUnexpected + "; " + e);
                    }

                reloadCache(cache, fAllowed);

                //
                // col.removeAll(Collection)
                //
                try
                    {
                    col = provider.getCollection();
                    Iterator iter = col.iterator();
                    if (iter.hasNext())
                        {
                        Object o = iter.next();
                        col.removeAll(Collections.singletonList(o));
                        azzert(fAllowed, sMsgNotEnforced);
                        }
                    }
                catch (UnsupportedOperationException e)
                    {
                    // ok
                    }
                catch (RequestPolicyException e)
                    {
                    azzert(!fAllowed, sMsgUnexpected + "; " + e);
                    }

                reloadCache(cache, fAllowed);

                try
                    {
                    col = provider.getCollection();
                    if (!col.isEmpty())
                        {
                        HashSet setAll = new HashSet();
                        for (Iterator iter = col.iterator(); iter.hasNext(); )
                            {
                            setAll.add(iter.next());
                            }
                        col.removeAll(setAll);
                        azzert(fAllowed, sMsgNotEnforced);
                        }
                    }
                catch (UnsupportedOperationException e)
                    {
                    // ok
                    }
                catch (RequestPolicyException e)
                    {
                    azzert(!fAllowed, sMsgUnexpected + "; " + e);
                    }

                reloadCache(cache, fAllowed);

                //
                // col.retainAll(Collection)
                //
                try
                    {
                    col = provider.getCollection();
                    Iterator iter = col.iterator();
                    if (iter.hasNext())
                        {
                        Object o = iter.next();
                        col.retainAll(Collections.singletonList(o));
                        azzert(fAllowed, sMsgNotEnforced);
                        }
                    }
                catch (UnsupportedOperationException e)
                    {
                    // ok
                    }
                catch (RequestPolicyException e)
                    {
                    azzert(!fAllowed, sMsgUnexpected + "; " + e);
                    }

                reloadCache(cache, fAllowed);

                try
                    {
                    col = provider.getCollection();
                    if (!col.isEmpty())
                        {
                        col.retainAll(new HashSet());
                        azzert(fAllowed, sMsgNotEnforced);
                        }
                    }
                catch (UnsupportedOperationException e)
                    {
                    // ok
                    }
                catch (RequestPolicyException e)
                    {
                    azzert(!fAllowed, sMsgUnexpected + "; " + e);
                    }

                reloadCache(cache, fAllowed);

                //
                // col.iterator().remove()
                //
                try
                    {
                    col = provider.getCollection();
                    Iterator iter = col.iterator();
                    if (iter.hasNext())
                        {
                        iter.next();
                        iter.remove();
                        azzert(fAllowed, sMsgNotEnforced);
                        }
                    }
                catch (UnsupportedOperationException e)
                    {
                    // ok
                    }
                catch (RequestPolicyException e)
                    {
                    azzert(!fAllowed, sMsgUnexpected + "; " + e);
                    }

                reloadCache(cache, fAllowed);
                }
            }

        // test helpers
        ValueExtractor extractor = new ReflectionExtractor("getPrice");
        EntryProcessor processor = new NumberIncrementor("Price", Integer.valueOf(1), true);
        MapListener    listener  = new AbstractMapListener() {};

        ComponentCollectionTester colTester = new ComponentCollectionTester();
        HashSet setKeys = new HashSet();
        // setup a set of keys
        for (int i = 0; i < 10; i++)
            {
            setKeys.add(Integer.valueOf(i));
            }


        //
        // Test the mutable operations of the cache
        //

        //
        // addIndex(ValueExtractor, boolean, Comparator)
        //
        try
            {
            cache.addIndex(extractor, false, null);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // addMapListener(MapListener, Filter, boolean)
        //
        try
            {
            cache.addMapListener(listener, AlwaysFilter.INSTANCE, false);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.addMapListener(listener, NeverFilter.INSTANCE, false);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // addMapListener(MapListener, Object, boolean)
        //
        try
            {
            cache.addMapListener(listener, Integer.valueOf(1), false);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // addMapListener(MapListener)
        //
        try
            {
            cache.addMapListener(listener);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // clear()
        //
        try
            {
            cache.clear();
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        reloadCache(cache, fAllowed);

        //
        // entrySet() mutable operations
        //
        colTester.testCollection(new ComponentCollectionProvider()
            {
            public Collection getCollection()
                {
                return cache.entrySet();
                }
            });

        reloadCache(cache, fAllowed);

        //
        // entrySet(Filter, Comparator).remove()
        //
        // rhl: not tested, as the set returned may not be backed by
        //      the underlying map.

        //
        // entrySet(Filter).remove()
        //
        // rhl: not tested, as the set returned may not be backed by
        //      the underlying map.

        //
        // invoke(Object, InvocableMap$EntryProcessor)
        //
        try
            {
            cache.invoke(Integer.valueOf(5), processor);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // invokeAll(Collection, InvocableMap$EntryProcessor)
        //
        try
            {
            cache.invoke(setKeys, processor);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // invokeAll(Filter, InvocableMap$EntryProcessor)
        //
        try
            {
            cache.invokeAll(NeverFilter.INSTANCE, processor);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // keySet() mutable operations
        //
        colTester.testCollection(new ComponentCollectionProvider()
            {
            public Collection getCollection()
                {
                return cache.keySet();
                }
            });

        reloadCache(cache, fAllowed);

        colTester.testCollection(new ComponentCollectionProvider()
            {
            public Collection getCollection()
                {
                return cache.keySet();
                }
            });

        reloadCache(cache, fAllowed);

        //
        // keySet(Filter) mutable operations
        //
        // rhl: not tested, as the set returned may not be backed by
        //      the underlying map.

        //
        // lock(Object, long)
        //
        try
            {
            cache.lock(Integer.valueOf(1), 200);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // lock(Object)
        //
        try
            {
            cache.lock(Integer.valueOf(2));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // put(Object, Object, long)
        //
        try
            {
            cache.put(Integer.valueOf(4), Trade.makeRandomTrade(5), 5000);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // put(Object, Object)
        //
        try
            {
            cache.put(Integer.valueOf(5), Trade.makeRandomTrade(5));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // putAll(Map)
        //
        try
            {
            Map mapUpdate = new HashMap();
            Trade.fillRandom(mapUpdate, 32);
            cache.putAll(mapUpdate);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // remove(Object)
        //
        try
            {
            cache.remove(Integer.valueOf(2));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.remove("non-existent-key");
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // removeIndex(ValueExtractor)
        //
        try
            {
            cache.removeIndex(extractor);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // removeMapListener(MapListener, Filter)
        //
        try
            {
            cache.removeMapListener(listener, NeverFilter.INSTANCE);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.removeMapListener(listener, AlwaysFilter.INSTANCE);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // removeMapListener(MapListener, Object)
        //
        try
            {
            cache.removeMapListener(listener, Integer.valueOf(1));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // removeMapListener(MapListener)
        //
        try
            {
            cache.removeMapListener(listener);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // unlock(Object)
        //
        try
            {
            cache.unlock(Integer.valueOf(1));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.unlock(Integer.valueOf(2));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // values() mutable operations
        //
        colTester.testCollection(new ComponentCollectionProvider()
            {
            public Collection getCollection()
                {
                return cache.values();
                }
            });
        }

    /**
    * Test the read operations on the specified cache to ensure that the quorum
    * policy is properly enforced.
    *
    * @param cache     the cache to test
    * @param fAllowed  true iff read operations should be allowed on the cache
    */
    protected void testCacheReadOperations(NamedCache cache, boolean fAllowed)
        {
        String sMsgNotEnforced = "Expected RequestPolicyException was not thrown; "
            + "cache reads should be disallowed";
        String sMsgUnexpected  = "Unexpected exception; cache reads should be allowed";

        DoubleSum  aggregator = new DoubleSum("getPrice");
        HashSet    setKeys    = new HashSet();
        Comparator comparator = (o1, o2) -> {
            Trade t1 = (Trade) o1;
            Trade t2 = (Trade) o2;
            return Integer.valueOf(t1.getId()).compareTo(Integer.valueOf(t2.getId()));
            };

        // setup a set of keys
        for (int i = 0; i < 10; i++)
            {
            setKeys.add(Integer.valueOf(i));
            }

        //
        // aggregate(Collection, InvocableMap$EntryAggregator)
        //
        try
            {
            cache.aggregate(Collections.singletonList(Integer.valueOf(1)), aggregator);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.aggregate(setKeys, aggregator);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // aggregate(Filter, InvocableMap$EntryAggregator)
        //
        try
            {
            cache.aggregate(NeverFilter.INSTANCE, aggregator);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.aggregate(new BetweenFilter("getId", 2, 10), aggregator);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // containsKey()
        //
        try
            {
            cache.containsKey(Integer.valueOf(1));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // containsValue()
        //
        try
            {
            cache.containsValue(Trade.makeRandomTrade(1));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // entrySet()
        //
        try
            {
            // just fetching the entrySet isn't considered a "read"
            Set setEntries = cache.entrySet();
            setEntries.size();
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // entrySet(Filter, Comparator)
        //
        try
            {
            cache.entrySet(NeverFilter.INSTANCE, comparator);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.entrySet(AlwaysFilter.INSTANCE, comparator);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // entrySet(Filter)
        //
        try
            {
            cache.entrySet(NeverFilter.INSTANCE);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.entrySet(AlwaysFilter.INSTANCE);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // get()
        //
        try
            {
            cache.get(null);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.get(Integer.valueOf(1));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // getAll(Collection)
        //

        try
            {
            cache.getAll(Collections.singletonList(Integer.valueOf(1)));
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.getAll(setKeys);
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // isEmpty()
        //
        try
            {
            cache.isEmpty();
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // keySet()
        //
        try
            {
            cache.keySet().size();
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // keySet(Filter)
        //
        try
            {
            cache.keySet(NeverFilter.INSTANCE).size();
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        try
            {
            cache.keySet(AlwaysFilter.INSTANCE).size();
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // size()
        //
        try
            {
            cache.size();
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }

        //
        // values()
        //
        try
            {
            cache.size();
            azzert(fAllowed, sMsgNotEnforced);
            }
        catch (RequestPolicyException e)
            {
            azzert(!fAllowed, sMsgUnexpected + "; " + e);
            }
        }

    /**
    * Test making an extend connection to the specified server.
    *
    * @param proxy     the proxy server to connect to
    * @param fAllowed  true if connections should be allowed
    */
    protected void testExtendConnect(CoherenceClusterMember proxy, boolean fAllowed)
        {
        int port = Integer.parseInt(proxy.getSystemProperty("tangosol.coherence.extend.port"));

        getInitiatorAddressProvider().setNextPort(port);

        try
            {
            Service    service = getFactory().ensureService("ExtendService");
            NamedCache cache   = ((CacheService) service).ensureCache("dist-no-quorum", null);

            cache.put("test", "test");

            // close the service so that the next test is forced to reconnect
            service.stop();

            azzert(fAllowed, "Extend client connections are not allowed, but a connection was established");
            }
        catch (ConnectionException e)
            {
            azzert(!fAllowed, "Extend client connections are allowed, but an exception was thrown: "
                   + e + "\n" + printStackTrace(e));
            Throwable cause = e.getCause();
            while (cause != null)
                {
                if (cause instanceof RequestPolicyException)
                    {
                    break;
                    }
                cause = cause.getCause();
                }
            if (!fAllowed)
                {
                azzert(cause instanceof RequestPolicyException,
                       "Expected cause to be instanceof RequestPolicyException, but was: " + e.getCause().toString());
                }
            }
        }


    // ----- inner class: ConfigListenerRegistrar -------------------------

    /**
    * Invocable sent to the Management invocation service to register
    * a listener on the partition-config ownership objects.
    */
    protected static class ConfigListenerRegistrar
            extends AbstractInvocable
        {
        /**
        * Construct a ConfigListenerRegistrar for the specified service.
        *
        * @param sServiceName  the name of the service to configure a listener for
        */
        ConfigListenerRegistrar(String sServiceName)
            {
            m_sServiceName = sServiceName;
            }

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            Service          service     = CacheFactory.getService(m_sServiceName);
            SafeService      serviceSafe = (SafeService) service;
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

            ObservableMap      mapConfig;
            Member             memberThis;
            int                nMemberThis;

            try
                {
                mapConfig   = serviceReal.getPartitionConfigMap();
                memberThis  = serviceReal.getThisMember();
                nMemberThis = memberThis.getId();
                synchronized (mapConfig)
                    {
                    for (Iterator iter = mapConfig.entrySet().iterator(); iter.hasNext(); )
                        {
                        Entry              entry      = (Entry) iter.next();
                        int                iPartition = ((Integer) entry.getKey()).intValue();
                        VersionedOwnership owners     = (VersionedOwnership) entry.getValue();

                        entry.setValue(new ObservableOwnership(nMemberThis, iPartition, owners));
                        }
                    }
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        /**
        * The name of the service.
        */
        protected String m_sServiceName;
        }

    /**
    * Register a configuration listener on the specified service on the
    * specified server.
    *
    * @param sServerName   the name of the server
    * @param sServiceName  the name of the service
    */
    protected void registerConfigListener(String sServerName, final String sServiceName)
        {
        Member            member  = findCacheServer(sServerName);
        InvocationService service = (InvocationService)
            CacheFactory.ensureCluster().getService("Management");
        service.query(new ConfigListenerRegistrar(sServiceName),
                      Collections.singleton(member));
        }

    /**
    * Observable Ownership objects that are used to validate that partition
    * transfer and backup operations are only conducted when allowed by the
    * quorum policy being tested.
    *
    * ObservableOwnership relies on status events being posted to the
    * "distribution-events" cache in order to determine what operations are
    * allowable.  Any exceptions are posted to the same cache, for deferred
    * handling by the test client.
    */
    protected static class ObservableOwnership
            extends VersionedOwnership
        {
        /**
        * Construct an ObservableOwnership.
        *
        * @param nMemberThis  the id of this member
        * @param nPartition   the partition id associated with this Ownership
        * @param owners       the existing ownership being replaced
        */
        ObservableOwnership(int nMemberThis, int nPartition, VersionedOwnership owners)
            {
            super(owners.getBackupCount(), owners.getVersion());
            m_nPartition = nPartition;
            m_aiOwner[0] = owners.getPrimaryOwner();
            m_aiOwner[1] = owners.getOwner(1);

            m_cacheEvents = CacheFactory.getCache("distribution-events");
            m_nMemberThis = nMemberThis;
            }

        /**
        * {@inheritDoc}
        */
        public void setPrimaryOwner(int iOwner)
            {
            int iOwnerOld = getPrimaryOwner();
            super.setPrimaryOwner(iOwner);

            try
                {
                if (iOwnerOld == 0)
                    {
                    // ok
                    }
                else
                    {
                    // somebody transferred ownership to us
                    azzert(isDistributionAllowed(),
                           "Distribution was not allowed, but primary owner for partition " +
                           m_nPartition + " changed from " + iOwnerOld + " to " + iOwner +
                           ".  Backup owner is " + getOwner(1));
                    }
                }
            catch (Exception e)
                {
                err("caught: " + e + ":" + Base.getStackTrace(e));
                m_cacheEvents.put(m_nMemberThis + ":" + m_nPartition, e);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void setOwner(int iStore, int iOwner)
            {
            int iOwnerOld = getOwner(iStore);
            super.setOwner(iStore, iOwner);

            try
                {
                if (iOwner == 0)
                    {
                    // ok
                    }
                else if (iOwnerOld == 0)
                    {
                    // if iOwner == getPrimaryOwner(), this is not really
                    // backup creation but rather primary partition transfer.
                    // For example, ownership is (4, 0), but then 4 transfers
                    // ownership to 5, retaining a backup copy.  Now ownership
                    // becomes (5, 4).
                    azzert(isBackupAllowed() || iOwner == getPrimaryOwner(),
                           "Backup was not allowed, but backup owner for partition " +
                           m_nPartition + " changed from " + iOwnerOld + " to " + iOwner +
                           ".  PrimaryOwner is " + getPrimaryOwner());
                    }
                else
                    {
                    azzert(isDistributionAllowed(),
                           "Distribution was not allowed, but backup owner for partition " +
                           m_nPartition + " changed from " + iOwnerOld + " to " + iOwner +
                           ".  PrimaryOwner is " + getPrimaryOwner());
                    }
                }
            catch (Exception e)
                {
                err("caught: " + e + ":" + Base.getStackTrace(e));
                m_cacheEvents.put(m_nMemberThis + ":" + m_nPartition, e);
                }
            }

        /**
        * Return true iff backup operations are allowed
        *
        * @return true iff backup operations are allowed
        */
        protected boolean isBackupAllowed()
            {
            return ((Boolean) m_cacheEvents.get("BACKUP_ALLOWED")).booleanValue();
            }

        /**
        * Return true iff distribution operations are allowed
        *
        * @return true iff distribution operations are allowed
        */
        protected boolean isDistributionAllowed()
            {
            return ((Boolean) m_cacheEvents.get("DISTRIBUTION_ALLOWED")).booleanValue();
            }

        /**
        * The partition id that this Ownership corresponds to.
        */
        protected int        m_nPartition;

        /**
        * The id of this member.
        */
        protected int        m_nMemberThis;

        /**
        * The events cache.
        */
        protected NamedCache m_cacheEvents;
        }


    // ----- inner class: QuorumAddressProvider ---------------------------

    /**
    * Return the singleton address provider instance used by the TcpInitiator to
    * initiate extend connections.
    *
    * @return the initiator address provider
    */
    public synchronized static QuorumAddressProvider getInitiatorAddressProvider()
        {
        QuorumAddressProvider provider = m_addressProvider;
        if (provider == null)
            {
            m_addressProvider = provider = new QuorumAddressProvider();
            }

        return provider;
        }

    /**
    * Test address provider.
    */
    protected static class QuorumAddressProvider
            implements AddressProvider
        {
        // ----- accessors ------------------------------------------------

        /**
        * Set the port of the next address to be returned by this AddressProvider.
        *
        * @param nPort  the port
        */
        protected void setNextPort(int nPort)
            {
            try
                {
                m_addressNext = new InetSocketAddress(LocalPlatform.get().getLoopbackAddress().getHostAddress(), nPort);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }


        // ----- AddressProvider methods ----------------------------------

        /**
        * {@inheritDoc}
        */
        public InetSocketAddress getNextAddress()
            {
            InetSocketAddress address = m_addressNext;
            m_addressNext = null;
            return address;
            }

        /**
        * {@inheritDoc}
        */
        public void accept()
            {
            }

        /**
        * {@inheritDoc}
        */
        public void reject(Throwable eCause)
            {
            }


        // ----- data members ---------------------------------------------

        /**
        * The next socket address to return, or null.
        */
        protected InetSocketAddress m_addressNext;
        }


    // ----- inner class: GetServiceMemberCount -----------------------------

    /**
     * A Callable that may be submitted to a CoherenceClusterMember to
     * obtain a count of the members of a Service.
     */
    public static class GetServiceMemberCount
            implements RemoteCallable<Integer>, Serializable
        {

        /**
         * Create a GetServiceMemberCount to return the
         * number of members of the given service.
         *
         * @param sServiceName  the name of the service
         */
        public GetServiceMemberCount(String sServiceName)
            {
            m_sServiceName = sServiceName;
            }

        @Override
        public Integer call() throws Exception
            {
            Service service = CacheFactory.getService(m_sServiceName);
            return service.getInfo().getServiceMembers().size();
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the service to get the membership count for
         */
        protected String m_sServiceName;
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Return the cache configuration under test.
     */
    protected String getCacheConfig()
        {
        return FILE_CFG_CACHE;
        }
    /**
     * Return the operational configuration under test.
     *
     * @return the operational configuration under test
     */
    protected String getOverrideConfig()
        {
        return FILE_OPERATIONAL_CONFIG;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The singleton address provider instance used to target extend connections
    * for the proxy tests.
    */
    protected static QuorumAddressProvider m_addressProvider;

    protected final List<CoherenceClusterMember> f_CoherenceClusterMembers = new ArrayList<>();

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static final String FILE_CFG_CACHE  = "quorum-cache-config.xml";

    /**
    * Constant for the operational override under test.
    */
    public static final String FILE_OPERATIONAL_CONFIG = "quorum-coherence-override.xml";

    /*
     * A {@link TestInfrastructureHelper} instance that we can pass to Bedrock on an invoking().
     */
    protected static TestInfrastructureHelper m_helper = new TestInfrastructureHelper();
    }
