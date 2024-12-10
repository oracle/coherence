/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.common.base.SimpleHolder;

import com.oracle.bedrock.runtime.java.options.JvmOptions;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.PartitionedService;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.persistence.CachePersistenceHelper;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.AbstractRollingRestartTest;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Rolling-restart tests to validate that no data is lost as a result of the
 * simultaneous death of members exceeding the backup-count (resulting in the
 * loss of both primary and backup copies for some partitions).
 *
 * @author rhl 2012.09.07
 */
public abstract class AbstractRollingPersistenceTests
        extends AbstractRollingRestartTest
    {
    /**
     * Test backup-count=0
     */
    @Test
    public void testBC0()
            throws IOException
        {
        doTest("testBC0" + getPersistenceManagerName(), 0);
        }

    /**
     * Test backup-count=1
     */
    @Test
    public void testBC1()
            throws IOException
        {
        doTest("testBC1" + getPersistenceManagerName(), 1);
        }

    /**
     * Test backup-count=2
     */
    @Test
    public void testBC2()
            throws IOException
        {
        doTest("testBC2" + getPersistenceManagerName(), 2);
        }

    /**
     * Test the dynamic recovery algorithm.
     */
    @Test
    public void testDynamicRecovery()
            throws IOException
        {
        doRecoveryTest();
        }

    /**
     * Test Helper.
     *
     * @param sTest     the test name
     * @param cBackups  the backup-count
     */
    protected void doTest(String sTest, int cBackups)
            throws IOException
        {
        File fileActive   = FileHelper.createTempDir();
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        System.setProperty("test.heap.min", "128");
        System.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        System.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        System.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        System.setProperty("test.backupcount", "" + cBackups);
        System.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        int cServers = 4 * (cBackups + 1);
        MemberHandler memberHandler = new MemberHandler(
                CacheFactory.ensureCluster(), sTest + "-", cBackups);
        for (int i = 0; i < cServers; i++)
            {
            memberHandler.addServer(null,
                                    JvmOptions.include("-XX:+ExitOnOutOfMemoryError"));
            }

        try
            {
            ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                    .getConfigurableCacheFactory("client-cache-config.xml", null);
            setFactory(factory);

            NamedCache cache = getNamedCache("rolling-" + sTest);
            HashMap    map   = new HashMap();
            for (int i = 0; i < 5000; i++)
                {
                map.put(i, Base.getRandomBinary(100, 1024));
                }
            cache.putAll(map);

            int cIters = 5;

            doRollingRestart(memberHandler, cIters, new WaitForNoOrphansRunnable(cache.getCacheService()));

            // debugging - print the final ownership
            SafeService      serviceSafe = (SafeService) cache.getCacheService();
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();
            try
                {
                String sOwnership = serviceReal.reportOwnership(Boolean.TRUE);
                CacheFactory.log(sOwnership, LOG_INFO);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }

            // assert that we didn't lose data
            KeyPartitioningStrategy strategy  = serviceReal.getKeyPartitioningStrategy();
            PartitionSet            partsDiff = new PartitionSet(serviceReal.getPartitionCount());
            PartitionSet            partsMiss = new PartitionSet(serviceReal.getPartitionCount());

            for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry   = (Map.Entry) iter.next();
                Object    oKey    = entry.getKey();
                Object    oValue  = entry.getValue();
                Object    oActual = cache.get(oKey);

                if (!equals(oValue, oActual))
                    {
                    if (oActual == null && oValue != null)
                        {
                        partsMiss.add(strategy.getKeyPartition(oKey));
                        }
                    else
                        {
                        partsDiff.add(strategy.getKeyPartition(oKey));
                        }
                    }
                }

            assertTrue("Corrupted: " + partsDiff + ", Lost: " + partsMiss,
                    partsDiff.isEmpty() && partsMiss.isEmpty());
            }
        finally
            {
            try
                {
                System.clearProperty("test.persistence.active.dir");
                System.clearProperty("test.persistence.snapshot.dir");
                System.clearProperty("test.persistence.trash.dir");
                System.clearProperty("test.backupcount");

                memberHandler.dispose();
                }
            finally
                {
                CacheFactory.shutdown();
                try
                    {
                    FileHelper.deleteDir(fileActive);
                    FileHelper.deleteDir(fileSnapshot);
                    FileHelper.deleteDir(fileTrash);
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }
        }

    /**
     * Test dynamic recovery.
     */
    protected void doRecoveryTest()
            throws IOException
        {
        File dirActive = FileHelper.createTempDir();
        File dirTrash  = FileHelper.createTempDir();

        String sTestPrefix = "dynamic-";

        // nine servers on three machines
        List<String> listMembers = new ArrayList(Arrays.asList(
            "m1-1", "m1-2", "m1-3", "m2-1", "m2-2", "m2-3", "m3-1", "m3-2", "m3-3"));
        int cServers = listMembers.size();

        Properties props = new Properties();
        props.setProperty("test.recover.quorum", "0"); // dynamic recovery
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        try
            {
            for (String sMember : listMembers)
                {
                startServer(sTestPrefix, sMember, dirActive, dirTrash, props, null);
                }

            ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                    .getConfigurableCacheFactory("client-cache-config.xml", null);
            setFactory(factory);

            NamedCache cache = getNamedCache("rolling-" + sTestPrefix + "data");
            DistributedCacheService service = (DistributedCacheService) cache.getCacheService();

            Eventually.assertThat(
                    invoking(service).getOwnershipEnabledMembers().size(),
                    is(cServers));

            HashMap map = new HashMap();
            for (int i = 0; i < 5000; i++)
                {
                map.put(i, Base.getRandomBinary(100, 1024));
                }

            // partition stabilization could take over a minute which should not be
            // a failure condition; wait after sleep
            waitForBalanced(service, 180);

            // the following sleep should be removed once COH-19735 is done
            sleep(4000); // when COH-14809 is done, replace with an event check

            cache.putAll(map);

            // stop all servers at once (kinda)
            // Note: we do this under a suspended service to avoid restore completing
            //       as a part of the non-atomic server shutdown and breaking
            //       our assertions

            Cluster cluster  = service.getCluster();
            String  sService = service.getInfo().getServiceName();

            cluster.suspendService(sService);
            try
                {
                listMembers.forEach(sMember ->
                        stopCacheServer(sTestPrefix + sMember, false));
                }
            finally
                {
                cluster.resumeService(sService);
                }
            listMembers.clear();

            Eventually.assertThat(
                invoking(service).getOwnershipEnabledMembers().size(), is(0));

            // start m1-1 and m2-1; no recovery since m3 is not visible
            startServer(sTestPrefix, "m1-r1", dirActive, dirTrash, props, listMembers);
            Eventually.assertThat(
                invoking(service).getOwnershipEnabledMembers().size(), is(1));

            // until COH-14764 is fixed, we need to register the notification
            // listener only after the first node is up
            SimpleHolder<Notification> holderNotification = new SimpleHolder<Notification>()
                {
                @Override
                public Notification get()
                    {
                    Notification note = super.get();
                    set(null);
                    return note;
                    }
                };
            registerNotificationListener(service,
                (notification, any) -> holderNotification.set(notification));

            startServer(sTestPrefix, "m2-r1", dirActive, dirTrash, props, listMembers);

            Eventually.assertThat(
                invoking(service).getOwnershipEnabledMembers().size(), is(2));
            assertNull("Missing machine m3", service.getPartitionOwner(0));

// Uncomment all notification related logic after COH-14764 is fixed
//            Notification notification = holderNotification.get();
//            assertTrue("Missing notification", notification != null &&
//                PersistenceManagerMBean.RECOVER_DISALLOWED.equals(notification.getType()));

            // start m3-1; no recovery since there are not enough members
            startServer(sTestPrefix, "m3-r1", dirActive, dirTrash, props, listMembers);

            Eventually.assertThat(
                invoking(service).getOwnershipEnabledMembers().size(), is(3));
            assertNull("Not enough nodes", service.getPartitionOwner(0));

//            notification = holderNotification.get();
//            assertTrue("Missing notification", notification != null &&
//                PersistenceManagerMBean.RECOVER_DISALLOWED.equals(notification.getType()));

            // start m1-2 and m1-3; no recovery since there are not enough members on m2 and m3
            startServer(sTestPrefix, "m1-r2", dirActive, dirTrash, props, listMembers);
            startServer(sTestPrefix, "m1-r3", dirActive, dirTrash, props, listMembers);

            Eventually.assertThat(
                invoking(service).getOwnershipEnabledMembers().size(), is(5));
            assertNull("Not enough nodes on m2 and m3", service.getPartitionOwner(0));

//            notification = holderNotification.get();
//            assertTrue("Missing notification", notification != null &&
//                PersistenceManagerMBean.RECOVER_DISALLOWED.equals(notification.getType()));

            // stop m1-3, start m2-2 and m3-2; should recover after that
            stopCacheServer(sTestPrefix + "m1-r3", false);
            listMembers.remove("m1-r3");

            startServer(sTestPrefix, "m2-r2", dirActive, dirTrash, props, listMembers);
            startServer(sTestPrefix, "m3-r2", dirActive, dirTrash, props, listMembers);

            Eventually.assertThat(
                invoking(service).getOwnershipEnabledMembers().size(), is(6));
            Eventually.assertThat(invoking(cache).size(), is(5000));
            }
        finally
            {
            try
                {
                for (String sMember : listMembers)
                    {
                    stopCacheServer(sTestPrefix + sMember, false);
                    }
                }
            finally
                {
                CacheFactory.shutdown();
                try
                    {
                    FileHelper.deleteDir(dirActive);
                    FileHelper.deleteDir(dirTrash);
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    private void startServer(String sTestPrefix, String sMember, File dirActive, File dirTrash,
                             Properties props, List<String> listMembers)
        {
        String sMachine   = sMember.substring(0, sMember.indexOf('-')); // machine name is member's prefix

        dirActive = new File(dirActive, sMachine);
        if (!dirActive.exists())
            {
            assertTrue("Failed to create directory" + dirActive, dirActive.mkdir());
            }

        dirTrash = new File(dirTrash, sMachine);
        if (!dirTrash.exists())
            {
            assertTrue("Failed to create directory" + dirTrash, dirTrash.mkdir());
            }

        props.setProperty("coherence.machine", sMachine);
        props.setProperty("test.persistence.active.dir", dirActive.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", dirTrash.getAbsolutePath());
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        if (listMembers != null)
            {
            listMembers.add(sMember);
            }
        startCacheServer(sTestPrefix + sMember, getProjectName(), getCacheConfigPath(), props, false);
        }

    private void registerNotificationListener(PartitionedService service, NotificationListener listener)
        {
        // there is a chance that while the cache server starts, the MBean
        // registration can be a bit delayed; implementing "Eventually" approach
        for (int iTry = 0; true; iTry++)
            {
            try
                {
                MBeanServer serverMB = MBeanHelper.findMBeanServer();

                String sMBean = "Coherence:" +
                        CachePersistenceHelper.getMBeanName(
                            service.getInfo().getServiceName());

                serverMB.addNotificationListener(new ObjectName(sMBean),
                        listener, null, null);
                return;
                }
            catch (InstanceNotFoundException e)
                {
                if (iTry >= 600)
                    {
                    throw ensureRuntimeException(e);
                    }
                sleep(100);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    // ----- inner class: MemberHandler -------------------------------------

    /**
     * MemberHandler implementation that is backup-count aware.
     */
    public class MemberHandler
            extends AbstractRollingRestartTest.MemberHandler
        {
        public MemberHandler(Cluster cluster, String sPrefix, int cBackups)
            {
            super(cluster, sPrefix, true, System.getProperty("os.name").toLowerCase().contains("windows"));

            m_cBackups = cBackups;
            }

        // ----- MemberHandler methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void bounce()
            {
            int cBounce = m_cBackups + 1;
            for (int i = 0; i < cBounce; i++)
                {
                killOldestServer();
                }
            for (int i = 0; i < cBounce; i++)
                {
                Properties props = new Properties();

                props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

                addServer(props);
                }
            }

        // ----- data members -----------------------------------------------

        protected int m_cBackups;
        }

    // ----- abstract methods -----------------------------------------------

    /**
     * Return a name for the PersistenceManager being used by the tests.
     *
     * @return a name used in log files, etc.
     */
    public abstract String getPersistenceManagerName();

    // ----- AbstractRollingRestartTest methods -----------------------------

    /**
     * {@inheritDoc}
     */
    public String getBuildPath()
        {
        return "build.xml";
        }

    /**
     * {@inheritDoc}
     */
    public String getProjectName()
        {
        return "persistence";
        }
    }
