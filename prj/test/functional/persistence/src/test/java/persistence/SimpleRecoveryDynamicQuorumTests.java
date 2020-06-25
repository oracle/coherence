/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.Base;

import common.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Functional tests for persistence recovery protocol.
 *
 * @author rhl 2012.07.10
 */
public class SimpleRecoveryDynamicQuorumTests
        extends AbstractFunctionalTest
    {

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("tangosol.coherence.cacheconfig", CFG_FILE);

        AbstractFunctionalTest._startup();
        }

    // ----- tests ----------------------------------------------------------


    @Test
    public void testQuorum_Default() throws Exception
        {
        Properties props = new Properties();
        addTestProperties(props);
        props.setProperty("test.manager.testcase", "testDynamic_Default");
        props.setProperty("test.recover.quorum", "0");
        testDynamic(props, "default");
        }

    @Test
    public void testQuorum_number() throws Exception
        {
        Properties props = new Properties();
        addTestProperties(props);
        props.setProperty("test.manager.testcase", "testDynamic_Number");
        props.setProperty("test.recover.quorum", "3");
        testDynamic(props, "number");
        }

    @Test
    public void testQuorum_percentage() throws Exception
        {
        Properties props = new Properties();
        addTestProperties(props);
        props.setProperty("test.manager.testcase", "testDynamic_Percentage");
        props.setProperty("test.recover.quorum", "33%");
        testDynamic(props, "percentage");
        }


    public void testDynamic(Properties props, String sTest) throws Exception
        {
        try
            {
            s_fileActive   = FileHelper.createTempDir();
            s_fileSnapshot = FileHelper.createTempDir();
            s_fileTrash    = FileHelper.createTempDir();

            props.setProperty("test.persistence.active.dir",    s_fileActive.getAbsolutePath());
            props.setProperty("test.persistence.snapshot.dir",  s_fileSnapshot.getAbsolutePath());
            props.setProperty("test.persistence.trash.dir",     s_fileTrash.getAbsolutePath());

            NamedCache              cache       = getNamedCache("dynamic-quorum");
            DistributedCacheService service     = (DistributedCacheService) cache.getCacheService();
            String                  sServerName = "testDynamic_" + sTest;
            int                     cServers    = 6;
            for (int i = 0; i < cServers; i++)
                {
                startCacheServer(sServerName + i, getProjectName(), getCacheConfigPath(), props);
                }

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cServers));

            waitForBalanced(cache.getCacheService());

            Cluster cluster  = CacheFactory.ensureCluster();
            String  sService = service.getInfo().getServiceName();
            try
                {
                cluster.suspendService(sService);
                for (int i = 0; i < cServers; i++)
                    {
                    stopCacheServer(sServerName + i);
                    }
                Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(0));
                }
            finally
                {
                cluster.resumeService(sService);
                }

            String sQuorum = props.getProperty("test.recover.quorum");
            float  fPct    = sQuorum.indexOf("%") >= 0 ? Base.parsePercentage(sQuorum) : Integer.parseInt(sQuorum) == 0 ? 2.f/3 : 1.0f;
            int    cQuorum = fPct < 1.0f ? (int) (fPct * cServers) : Integer.parseInt(sQuorum);

            for (int i = 0; i < cQuorum -1; i++)
                {
                startCacheServer(sServerName + (i + cServers), getProjectName(), getCacheConfigPath(), props);
                }

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cQuorum - 1));
            assertFalse(waitForRecoveryAssignments(service, 60000));

            startCacheServer(sServerName + (cServers + cQuorum -1), getProjectName(), getCacheConfigPath(), props);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cQuorum));
            waitForBalanced(cache.getCacheService());
            }
        finally
            {
            stopAllApplications();
            Cluster cluster = CacheFactory.ensureCluster();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(s_fileActive);
            FileHelper.deleteDirSilent(s_fileSnapshot);
            FileHelper.deleteDirSilent(s_fileTrash);
            }
        }


    // ----- helpers ------------------------------------------------------

    public static void reportOwnership(PartitionedService service)
        {
        try
            {
            SafeService      serviceSafe = (SafeService) service;
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();
            CacheFactory.log(serviceReal.reportOwnership(Boolean.TRUE));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Wait for an extended period compared with {@link AbstractFunctionalTest#waitForBalanced(CacheService)}
     * for the specified (partitioned) cache service to become "balanced".
     *
     * @param service   the partitioned cache to wait for balancing
     */
    public static void waitForBalanced(CacheService service)
        {
        SafeService      serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        Eventually.assertThat(invoking(serviceReal).calculateUnbalanced(), is(0), within(300, TimeUnit.SECONDS));
        }

    /**
     * Wait for partition recovery with the specified time.
     *
     * @param service      the distributed service
     * @param cWaitMillis  time to wait for
     *
     * @return true if recovery is successful
     */
    public static boolean waitForRecoveryAssignments(PartitionedService service, int cWaitMillis)
        {
        int     cRemain  = cWaitMillis;
        boolean fOrphans = false;
        while (cRemain >= 0)
            {
            for (int iPartition = 0, cParts = service.getPartitionCount(); iPartition < cParts; iPartition++)
                {
                if (service.getPartitionOwner(iPartition) == null)
                    {
                    fOrphans = true;
                    break;
                    }
                }

            if (!fOrphans)
                {
                return true;
                }

            long ldtStart = Base.getSafeTimeMillis();
            Base.sleep(1000);
            long ldtEnd   = Base.getSafeTimeMillis();

            cRemain -= (ldtEnd - ldtStart);
            }

        CacheFactory.log("Waited over " + (cWaitMillis / 1000) + "sec for orphan recovery assignments");

        return !fOrphans;
        }


    // ----- accessors ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getCacheConfigPath()
        {
        return CFG_FILE;
        }

    /**
     * {@inheritDoc}
     */
    public static String getProjectName()
        {
        return "persistence";
        }

    // ----- constants ------------------------------------------------------

    protected static final String CFG_FILE = "simple-persistence-bdb-cache-config.xml";

    // ----- data members ---------------------------------------------------

    private static File s_fileActive;
    private static File s_fileSnapshot;
    private static File s_fileTrash;
    }
