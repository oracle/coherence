/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.common.base.Collector;

import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistenceTools;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.persistence.GUIDHelper;
import com.tangosol.persistence.bdb.BerkeleyDBManager;

import com.tangosol.util.Base;

import common.AbstractFunctionalTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Functional tests for persistence recovery protocol.
 *
 * @author rhl 2012.07.10
 */
public class SimpleRecoveryProtocolTests
        extends AbstractFunctionalTest
    {

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        try
            {
            s_fileActive   = FileHelper.createTempDir();
            s_fileSnapshot = FileHelper.createTempDir();
            s_fileTrash    = FileHelper.createTempDir();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        testSetup();
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        try
            {
            FileHelper.deleteDir(s_fileActive);
            FileHelper.deleteDir(s_fileSnapshot);
            FileHelper.deleteDir(s_fileTrash);
            }
        catch (IOException e)
            {
            // ignore
            }
        }

    @Before
    public void beforeTests()
        {
        CacheFactory.shutdown();
        }

    // ----- tests ----------------------------------------------------------

    public static void testSetup()
        {
        try
            {
            System.setProperty("test.persistence.active.dir",    s_fileActive.getAbsolutePath());
            System.setProperty("test.persistence.snapshot.dir",  s_fileSnapshot.getAbsolutePath());
            System.setProperty("test.persistence.trash.dir",     s_fileTrash.getAbsolutePath());
            System.setProperty("tangosol.coherence.cacheconfig", CFG_FILE);

            AbstractFunctionalTest._startup();

            Properties props = new Properties();
            addTestProperties(props);
            props.setProperty("test.recover.quorum", "0");

            CoherenceClusterMember clusterMember = startCacheServer("SRPT-setup", getProjectName(), "simple-recovery-cache-config.xml", props);
            Eventually.assertThat(invoking(clusterMember).getClusterSize(), is(2));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning("DistributedCacheRecovery"), is(true));

            NamedCache         cache          = CacheFactory.getCache("simple-recovery");
            PartitionedService service        = (PartitionedService) cache.getCacheService();
            waitForRecoveryAssignments(service, 30000);

            stopCacheServer("SRPT-setup");
            clusterMember.waitFor();
            }
        finally
            {
            CacheFactory.shutdown();
            }
        }

    @Test
    public void testNoPersistentData()
        {
        try
            {
            Properties props = new Properties();
            addTestProperties(props);
            props.setProperty("test.manager.testcase", "testNoPersistentData");

            NamedCache         cache          = getNamedCache("simple-recovery");
            PartitionedService service        = (PartitionedService) cache.getCacheService();
            String             sServiceName   = service.getInfo().getServiceName();
            int          cParts               = service.getPartitionCount();
            PartitionSet partsEmpty           = new PartitionSet(cParts);
            PartitionSet partsAll             = new PartitionSet(cParts);
            partsAll.fill();

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}});

            CoherenceClusterMember member = startCacheServer("testNoPersistentData-1", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(2));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}});

            member = startCacheServer("testNoPersistentData-2", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(3));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}, {2, partsEmpty}});

            member = startCacheServer("testNoPersistentData-3", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(4));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}, {2, partsEmpty}, {3, partsEmpty}});

            member = startCacheServer("testNoPersistentData-4", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(5));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            waitForRecoveryAssignments(service, 30000);

            reportOwnership(service);
            assertPartitionOwnership(service, new Object[][] {{0, partsEmpty}});
            }
        finally
            {
            stopCacheServer("testNoPersistentData-1");
            stopCacheServer("testNoPersistentData-2");
            stopCacheServer("testNoPersistentData-3");
            stopCacheServer("testNoPersistentData-4");
            Cluster cluster = CacheFactory.ensureCluster();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            CacheFactory.shutdown();
            }
        }

    @Test
    public void testAllOnMember3()
        {
        try
            {
            Properties props = new Properties();
            addTestProperties(props);
            props.setProperty("test.manager.testcase", "testAllOnMember3");

            NamedCache         cache          = getNamedCache("simple-recovery");
            PartitionedService service        = (PartitionedService) cache.getCacheService();
            String             sServiceName   = service.getInfo().getServiceName();
            int          cParts               = service.getPartitionCount();
            PartitionSet partsEmpty           = new PartitionSet(cParts);
            PartitionSet partsAll             = new PartitionSet(cParts);
            partsAll.fill();

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}});

            CoherenceClusterMember member = startCacheServer("testAllOnMember3-1", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(2));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}});

            member = startCacheServer("testAllOnMember3-2", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(3));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}, {2, partsEmpty}});

            member = startCacheServer("testAllOnMember3-3", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(4));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}, {2, partsEmpty}, {3, partsEmpty}});

            member = startCacheServer("testAllOnMember3-4", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(5));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            waitForRecoveryAssignments(service, 30000);

            reportOwnership(service);
            assertPartitionOwnership(service, new Object[][] {{0, partsEmpty}, {1, partsEmpty}, {2, partsEmpty}, {3, partsAll}, {4, partsEmpty}, {5, partsEmpty}});
            }
        finally
            {
            stopCacheServer("testAllOnMember3-1");
            stopCacheServer("testAllOnMember3-2");
            stopCacheServer("testAllOnMember3-3");
            stopCacheServer("testAllOnMember3-4");
            Cluster cluster = CacheFactory.ensureCluster();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            CacheFactory.shutdown();
            }
        }

    @Test
    public void testHalfOn3HalfOn4()
        {
        try
            {
            Properties props = new Properties();
            addTestProperties(props);
            props.setProperty("test.manager.testcase", "testHalfOn3HalfOn4");

            NamedCache         cache          = getNamedCache("simple-recovery");
            PartitionedService service        = (PartitionedService) cache.getCacheService();
            String             sServiceName   = service.getInfo().getServiceName();
            int          cParts               = service.getPartitionCount();
            PartitionSet partsEmpty           = new PartitionSet(cParts);
            PartitionSet parts0_20            = new PartitionSet(cParts);
            PartitionSet parts21_42           = new PartitionSet(cParts);
            PartitionSet partsAll             = new PartitionSet(cParts);
            partsAll.fill();

            for (int i = 0; i <= 20; i++)
                {
                parts0_20.add(i);
                }
            for (int i = 21; i <= 42; i++)
                {
                parts21_42.add(i);
                }

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}});

            CoherenceClusterMember member = startCacheServer("testHalfOn3HalfOn4-1", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(2));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}});

            member = startCacheServer("testHalfOn3HalfOn4-2", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(3));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}, {2, partsEmpty}});

            member = startCacheServer("testHalfOn3HalfOn4-3", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(4));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}, {2, partsEmpty}, {3, partsEmpty}});

            member = startCacheServer("testHalfOn3HalfOn4-4", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(5));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            waitForRecoveryAssignments(service, 30000);

            reportOwnership(service);
            assertPartitionOwnership(service, new Object[][]
                    {{0, partsEmpty}, {1, partsEmpty}, {2, partsEmpty},
                     {3, parts0_20},  {4, parts21_42}, {5, partsEmpty}});
            }
        finally
            {
            stopCacheServer("testHalfOn3HalfOn4-1");
            stopCacheServer("testHalfOn3HalfOn4-2");
            stopCacheServer("testHalfOn3HalfOn4-3");
            stopCacheServer("testHalfOn3HalfOn4-4");
            Cluster cluster = CacheFactory.ensureCluster();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            CacheFactory.shutdown();
            }
        }

    @Test
    public void testHalfOn3HalfMissing()
        {
        try
            {
            Properties props = new Properties();
            addTestProperties(props);
            props.setProperty("test.manager.testcase", "testHalfOn3HalfMissing");

            NamedCache         cache          = getNamedCache("simple-recovery");
            PartitionedService service        = (PartitionedService) cache.getCacheService();
            String             sServiceName   = service.getInfo().getServiceName();
            int          cParts               = service.getPartitionCount();
            PartitionSet partsEmpty           = new PartitionSet(cParts);
            PartitionSet parts0_20            = new PartitionSet(cParts);
            PartitionSet parts21_42           = new PartitionSet(cParts);
            PartitionSet partsAll             = new PartitionSet(cParts);
            partsAll.fill();

            for (int i = 0; i <= 20; i++)
                {
                parts0_20.add(i);
                }
            for (int i = 21; i <= 42; i++)
                {
                parts21_42.add(i);
                }

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}});

            CoherenceClusterMember member = startCacheServer("testHalfOn3HalfMissing-1", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(2));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}});

            member = startCacheServer("testHalfOn3HalfMissing-2", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(3));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}, {2, partsEmpty}});

            member = startCacheServer("testHalfOn3HalfMissing-3", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(4));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));

            assertPartitionOwnership(service, new Object[][]
                    {{0, partsAll}, {1, partsEmpty}, {2, partsEmpty}, {3, partsEmpty}});

            member = startCacheServer("testHalfOn3HalfMissing-4", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(member).getClusterSize(), is(5));
            Eventually.assertThat(invoking(member).isServiceRunning(sServiceName), is(true));
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(4));

            reportOwnership(service);
            assertPartitionOwnership(service, new Object[][]
                    {{0, partsEmpty}, {1, partsEmpty}, {3, partsEmpty}});
            }
        finally
            {
            stopCacheServer("testHalfOn3HalfMissing-1");
            stopCacheServer("testHalfOn3HalfMissing-2");
            stopCacheServer("testHalfOn3HalfMissing-3");
            stopCacheServer("testHalfOn3HalfMissing-4");
            Cluster cluster = CacheFactory.ensureCluster();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            CacheFactory.shutdown();
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

    public static void assertPartitionOwnership(PartitionedService service, Object[][] aoOwners)
        {
        for (int i = 0; i < aoOwners.length; i++)
            {
            int nMember = (Integer) aoOwners[i][0];
            PartitionSet parts = (PartitionSet) aoOwners[i][1];

            for (int iPartition = parts.next(0); iPartition >= 0; iPartition = parts.next(iPartition + 1))
                {
                Member member = service.getPartitionOwner(iPartition);
                if (nMember != ( member == null ? 0 : member.getId()))
                    {
                    CacheFactory.log("Owner of partition " + iPartition + " is member " +
                                     ( member == null ? 0 : member.getId()) + " and not member " + nMember);
                    }
                assertEquals(nMember, member == null ? 0 : member.getId());
                }
            }
        }

    public static void waitForRecoveryAssignments(PartitionedService service, int cWaitMillis)
        {
        int cRemain = cWaitMillis;
        while (cRemain >= 0)
            {
            boolean fOrphans = false;
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
                return;
                }

            long ldtStart = Base.getSafeTimeMillis();
            Base.sleep(1000);
            long ldtEnd   = Base.getSafeTimeMillis();

            cRemain -= (ldtEnd - ldtStart);
            }

        CacheFactory.log("Waited over " + (cWaitMillis / 1000) + "sec for orphan recovery assignments");
        }

    // ----- inner class: Environment -------------------------------------

    public static class Environment
            implements PersistenceEnvironment
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized PersistenceManager openActive()
            {
            Manager manager = m_manager;
            if (manager == null)
                {
                try
                    {
                    m_manager = manager = new Manager();
                    }
                catch (IOException e)
                    {
                    throw new PersistenceException(e);
                    }
                }
            return manager;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public PersistenceManager openSnapshot(String sSnapshot)
            {
            throw new IllegalArgumentException();
            }

        @Override
        public PersistenceManager createSnapshot(String sSnapshot, PersistenceManager manager)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean removeSnapshot(String sSnapshot)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public String[] listSnapshots()
            {
            return new String[0];
            }

        @Override
        public synchronized void release()
            {
            Manager manager = m_manager;
            if (manager != null)
                {
                manager.release();
                m_manager = null;
                }
            }

        private Manager m_manager;
        }

    // ----- inner class: Manager -----------------------------------------

    public static class Manager
            implements PersistenceManager
        {
        public Manager()
                throws IOException
            {
            f_manager = new BerkeleyDBManager(
                    new File(System.getProperty("test.persistence.active.dir")),
                    null,
                    null);
            }

        // ----- PersistenceManager interface -----------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName()
            {
            return f_manager.getName();
            }

        @Override
        public PersistentStore createStore(String sId)
            {
            return f_manager.createStore(sId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public PersistentStore open(String sId, PersistentStore store)
            {
            return f_manager.open(sId, store);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public PersistentStore open(String sId, PersistentStore store, Collector collector)
            {
            return f_manager.open(sId, store, collector);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close(String sId)
            {
            f_manager.close(sId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean delete(String sId, boolean fSafe)
            {
            return f_manager.delete(sId, fSafe);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] list()
            {
            String[] asGUIDAll = f_manager.list();
            String   sTestCase = System.getProperty("test.manager.testcase", "testNoPersistentData");
            if (sTestCase.equals("testNoPersistentData"))
                {
                return new String[0];
                }
            else if (sTestCase.equals("testAllOnMember3"))
                {
                // 43 partitions, members 2-5, all partitions on member 3
                Member memberThis  = CacheFactory.getCluster().getLocalMember();
                int    nMemberThis = memberThis.getId();
                switch (nMemberThis)
                    {
                    case 3:
                        {
                        return asGUIDAll;
                        }

                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return new String[0];

                    default:
                        fail();
                    }
                }
            else if (sTestCase.equals("testHalfOn3HalfOn4"))
                {
                // 43 partitions, members 2-5, partitions 0-20 on member 3, 21-42 on member 4
                Member memberThis  = CacheFactory.getCluster().getLocalMember();
                int    nMemberThis = memberThis.getId();
                switch (nMemberThis)
                    {
                    case 3:
                        {
                        int  cGUID = asGUIDAll.length;
                        List list  = new ArrayList(cGUID);
                        for (int i = 0; i < cGUID; i++)
                            {
                            String sGUID = asGUIDAll[i];
                            if (GUIDHelper.getPartition(sGUID) <= 20)
                                {
                                list.add(sGUID);
                                }
                            }

                        return (String[]) list.toArray(new String[list.size()]);
                        }

                    case 4:
                        {
                        int  cGUID = asGUIDAll.length;
                        List list  = new ArrayList(cGUID);
                        for (int i = 0; i < cGUID; i++)
                            {
                            String sGUID = asGUIDAll[i];
                            if (GUIDHelper.getPartition(sGUID) >= 21)
                                {
                                list.add(sGUID);
                                }
                            }

                        return (String[]) list.toArray(new String[list.size()]);
                        }

                    case 1:
                    case 2:
                    case 5:
                        return new String[0];

                    default:
                        fail();
                    }
                }
            else if (sTestCase.equals("testHalfOn3HalfMissing"))
                {
                // 43 partitions, members 2-5, partitions 0-20 on member 3
                Member memberThis  = CacheFactory.getCluster().getLocalMember();
                int    nMemberThis = memberThis.getId();
                switch (nMemberThis)
                    {
                    case 3:
                        {
                        int  cGUID = asGUIDAll.length;
                        List list  = new ArrayList(cGUID);
                        for (int i = 0; i < cGUID; i++)
                            {
                            String sGUID = asGUIDAll[i];
                            if (GUIDHelper.getPartition(sGUID) <= 20)
                                {
                                list.add(sGUID);
                                }
                            }

                        return (String[]) list.toArray(new String[list.size()]);
                        }

                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return new String[0];

                    default:
                        fail();
                    }
                }
            else
                {
                fail("unknown test case");
                }

            return null;  // unreachable
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] listOpen()
            {
            return f_manager.listOpen();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void read(String sId, InputStream in)
                throws IOException
            {
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(String sId, OutputStream out)
                throws IOException
            {
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void release()
            {
            f_manager.release();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public PersistenceTools getPersistenceTools()
            {
            return (PersistenceTools) null;
            }

        // ----- data members ---------------------------------------------

        private final PersistenceManager f_manager;
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

    protected static final String CFG_FILE = "simple-recovery-cache-config.xml";

    // ----- data members ---------------------------------------------------

    private static File s_fileActive;
    private static File s_fileSnapshot;
    private static File s_fileTrash;
    }
