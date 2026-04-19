/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.persistence.PersistenceManager;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.SimplePartitionKey;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.CompositeKey;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.processor.AbstractProcessor;

import com.tangosol.persistence.bdb.BerkeleyDBManager;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import javax.management.MBeanException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for simple cache persistence and recovery using the
 * BerkeleyDBPersistenceManager.
 *
 * @author jh  2012.10.18
 */
public class BerkeleyDBSimplePersistenceTests
        extends AbstractSimplePersistenceTests
    {

    // ----- AbstractSimplePersistenceTests methods -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException
        {
        return new BerkeleyDBManager(file, null, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPersistenceManagerName()
        {
        return "BDB";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheConfigPath()
        {
        return "simple-persistence-bdb-cache-config.xml";
        }

    /**
     * Test multiple restarts with ensure cache for active persistence.
     */
    @Test
    public void testRestartsWithEnsureCacheForActiveMode()
            throws IOException, MBeanException
        {
        testMultipleRestartsWithClientEnsureCache("active");
        }

    /**
     * Test multiple restarts with ensure cache for async-active persistence.
     */
    @Test
    public void testRestartsWithEnsureCacheForAsyncActiveMode()
            throws IOException, MBeanException
        {
        testMultipleRestartsWithClientEnsureCache("active-async");
        }

    /**
     * Test multiple restarts with ensure cache for active-backup persistence.
     */
    @Test
    public void testRestartsWithEnsureCacheForActiveBackupMode()
            throws IOException, MBeanException
        {
        testMultipleRestartsWithClientEnsureCache("active-backup");
        }

    /**
     * Regression test for COH-33056: stress index rebuild after persistence
     * recovery with a larger partition count.
     */
    @Test
    public void testIndexConsistencyAfterRecoveryWithManyPartitions()
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();
        File fileActive1  = FileHelper.createTempDir();
        File fileActive2  = FileHelper.createTempDir();
        File fileActive3  = FileHelper.createTempDir();

        final String sTestName             = "testIndexConsistencyAfterRecoveryWithManyPartitions";
        final String sPersistentCache      = "simple-persistent";
        final String sCluster              = sTestName + "-" + Base.getSafeTimeMillis();
        final int    cPartitions           = Integer.getInteger("test.index.recovery.partition.count", 1021);
        final int    cEntriesPerPartition  = Integer.getInteger("test.index.recovery.entries.per.partition", 8);
        final int    cRestartCount         = Integer.getInteger("test.index.recovery.restart.count", 3);
        final int    cThreads              = Integer.getInteger("test.index.recovery.thread.count", 8);
        final int    cEntries              = cPartitions * cEntriesPerPartition;
        final String sServer1              = sTestName + "-1";
        final String sServer2              = sTestName + "-2";
        final String sServer3              = sTestName + "-3";

        String sPrevCluster    = System.getProperty("coherence.cluster");
        String sPrevPartitions = System.getProperty("coherence.distributed.partitions");

        Properties props1 = createRecoveryProperties(fileActive1, fileSnapshot, fileTrash, sCluster, cPartitions, cThreads);
        Properties props2 = createRecoveryProperties(fileActive2, fileSnapshot, fileTrash, sCluster, cPartitions, cThreads);
        Properties props3 = createRecoveryProperties(fileActive3, fileSnapshot, fileTrash, sCluster, cPartitions, cThreads);

        NamedCache cache = null;

        try
            {
            CacheFactory.shutdown();

            System.setProperty("coherence.cluster", sCluster);
            System.setProperty("coherence.distributed.partitions", String.valueOf(cPartitions));

            ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                    .getConfigurableCacheFactory("client-cache-config.xml", null);
            setFactory(factory);

            cache = getNamedCache(sPersistentCache);

            DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
            Cluster                 cluster = CacheFactory.ensureCluster();

            startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props1);
            startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props2);
            startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props3);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
            waitForBalanced(service);

            ValueExtractor extractor = new ReflectionExtractor("length");
            cache.addIndex(extractor, false, null);

            populateEveryPartition(cache, cPartitions, cEntriesPerPartition);

            assertEquals(cEntries, cache.size());
            assertNoIndexInconsistencies(cache, cPartitions, 1, "initial population");

            String sService = service.getInfo().getServiceName();

            for (int i = 1; i <= cRestartCount; i++)
                {
                cluster.suspendService(sService);

                try
                    {
                    stopCacheServer(sServer1);
                    stopCacheServer(sServer2);
                    stopCacheServer(sServer3);

                    Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
                    }
                finally
                    {
                    cluster.resumeService(sService);
                    }

                service.shutdown();

                startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props1);
                startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props2);
                startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props3);

                cache   = getNamedCache(sPersistentCache);
                service = (DistributedCacheService) cache.getCacheService();

                Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
                waitForBalanced(service);

                assertEquals("Unexpected cache size after restart " + i, cEntries, cache.size());
                assertNoIndexInconsistencies(cache, cPartitions, 1, "restart " + i);
                }
            }
        finally
            {
            if (cache != null && getFactory() != null)
                {
                getFactory().destroyCache(cache);
                }

            stopAllApplications();
            CacheFactory.shutdown();

            restoreSystemProperty("coherence.cluster", sPrevCluster);
            restoreSystemProperty("coherence.distributed.partitions", sPrevPartitions);

            FileHelper.deleteDirSilent(fileActive1);
            FileHelper.deleteDirSilent(fileActive2);
            FileHelper.deleteDirSilent(fileActive3);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    /**
     * Test 2 server storage, 1 server restart with backup persistence.
     */
    @Test
    public void testBackupPersistence2()
            throws IOException, MBeanException
        {
        testBackupPersistence("active-backup", 2, "simple-persistent", false);
        }

    /**
     * Test 3 server storage, 1 server restart with backup persistence.
     */
    @Test
    public void testBackupPersistence3()
            throws IOException, MBeanException
        {
        testBackupPersistence3("active-backup");
        }

    /**
     * Test 4 server storage, 1 server restart with backup persistence and
     * backup count at 2.
     */
    @Test
    public void testBackupPersistence4()
            throws IOException, MBeanException
        {
        testBackupPersistence("active-backup", 4, "simple-persistent", false);
        }

    /**
     * Test 4 server storage, 1 server restart with backup persistence and
     * backup count at 2, and after rolling restarts.
     */
    @Test
    public void testBackupPersistence4Rolling()
            throws IOException, MBeanException
        {
        testBackupPersistence("active-backup", 4, "simple-persistent", true);
        }

    /**
     * Test multiple restarts with ensure cache before storage nodes start.
     */
    public void testMultipleRestartsWithClientEnsureCache(String sMode)
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", sMode);
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.threads", "5");
        props.setProperty("test.persistence.members", "3");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final String            sServer          = "testMultipleRestartsWithClientEnsureCache" +
                                                   Character.toUpperCase(sMode.charAt(0)) + sMode.substring(1);
        final String            sPersistentCache = "simple-persistent";
        NamedCache              cache            = getNamedCache(sPersistentCache);
        DistributedCacheService service          = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster          = CacheFactory.ensureCluster();

        String sServer1;
        String sServer2;
        String sServer3;

        int i             = 0;
        int nRestartCount = 3;
        try
            {
            while (++i <= nRestartCount)
                {
                System.out.println("**** Iteration: " + i + " of " + nRestartCount);
                sServer1 = sServer + "-" + (i*3 - 1);
                sServer2 = sServer + "-" + (i*3 - 2);
                sServer3 = sServer + "-" + (i*3);
                startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props);
                startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props);
                startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props);

                Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
                waitForBalanced(service);

                // populate with some data if first time only
                if (i == 1)
                    {
                    PersistenceTestHelper.populateData(cache, 5000);
                    }
                else
                    {
                    service = (DistributedCacheService) cache.getCacheService();
                    }

                // always assert the size to ensure we have not lost data
                assertEquals(cache.size(), 5000);

                String  sService = service.getInfo().getServiceName();

                cluster.suspendService(sService);

                try
                    {
                    // abruptly shutdown
                    stopCacheServer(sServer1);
                    stopCacheServer(sServer2);
                    stopCacheServer(sServer3);

                    Eventually.assertThat(cluster.getMemberSet().size(), is(1));
                    }
                finally
                    {
                    cluster.resumeService(sService);
                    }

                service.shutdown();

                try
                    {
                    cache = getNamedCache(sPersistentCache);
                    cache.size();
                    }
                catch (Throwable t)
                    {
                    CacheFactory.log("got Exception: " + t);
                    }
                }
            }
        finally
            {
            getFactory().destroyCache(cache);
            stopAllApplications();
            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    /**
     * Test 3 server restart with backup persistence.
     */
    public void testBackupPersistence3(String sMode)
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive1 = FileHelper.createTempDir();
        File fileActive2 = FileHelper.createTempDir();
        File fileBackup1 = FileHelper.createTempDir();
        File fileBackup2 = FileHelper.createTempDir();
        File fileTrash = FileHelper.createTempDir();

        Properties props1 = new Properties();
        props1.setProperty("test.persistence.mode", sMode);
        props1.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props1.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props1.setProperty("test.threads", "5");
        props1.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        Properties props2 = new Properties();
        props2.setProperty("test.persistence.mode", sMode);
        props2.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props2.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props2.setProperty("test.threads", "5");
        props2.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        // use 2 different locations
        props1.setProperty("test.persistence.active.dir", fileActive1.getAbsolutePath());
        props1.setProperty("test.persistence.backup.dir", fileBackup1.getAbsolutePath());
        props2.setProperty("test.persistence.active.dir", fileActive2.getAbsolutePath());
        props2.setProperty("test.persistence.backup.dir", fileBackup2.getAbsolutePath());

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final String sServer = "testBackupPersistence3";
        final String sPersistentCache = "simple-persistent";
        NamedCache cache = getNamedCache(sPersistentCache);
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        Cluster cluster = CacheFactory.ensureCluster();

        try
            {
            System.out.println("**** Backup Persistence Start");
            String sServer1 = sServer + "-1";
            String sServer2 = sServer + "-2";
            String sServer3 = sServer + "-3";
            String sServer4 = sServer + "-4";

            // servers 1 and 2 use the same location
            startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props1);
            startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props1);
            startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props2);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
            waitForBalanced(service);

            // populate with some data
            PersistenceTestHelper.populateData(cache, 5000);

            // always assert the size to ensure we have not lost data
            assertEquals(cache.size(), 5000);

            // backups persistence is async., need some time to complete
            Base.sleep(10_000);

            String sService = service.getInfo().getServiceName();

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

            cluster.suspendService(sService);

            try
                {
                // abruptly shutdown
                stopCacheServer(sServer1);
                stopCacheServer(sServer2);
                stopCacheServer(sServer3);

                Eventually.assertThat(cluster.getMemberSet().size(), is(1));
                }
            finally
                {
                cluster.resumeService(sService);
                }

            service.shutdown();

            try
                {
                cache = getNamedCache(sPersistentCache);
                cache.size();
                }
            catch (Throwable t)
                {
                CacheFactory.log("got Exception: " + t);
                }

            // start fourth server, recovering from stores location 1
            startCacheServer(sServer4, getProjectName(), getCacheConfigPath(), props1);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));
            waitForBalanced(service);

            Eventually.assertDeferred(cache::size, is(5000));

            stopCacheServer(sServer4);
            }
        finally
            {
            getFactory().destroyCache(cache);

            stopAllApplications();

            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileActive1);
            FileHelper.deleteDirSilent(fileActive2);
            FileHelper.deleteDirSilent(fileBackup1);
            FileHelper.deleteDirSilent(fileBackup2);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    /**
     * Test N server restart with backup persistence.
     */
    public void testBackupPersistence(String sMode, int nServers, String sCacheName, boolean fRolling)
            throws IOException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        File[]       fileActive = new File[nServers + 1];
        File[]       fileBackup = new File[nServers + 1];
        Properties[] props      = new Properties[nServers + 1];

        for (int i = 0; i < nServers + 1; i++)
            {
            fileActive[i] = FileHelper.createTempDir();
            fileBackup[i] = FileHelper.createTempDir();

            props[i] = new Properties();
            props[i].setProperty("test.persistence.mode", sMode);
            props[i].setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
            props[i].setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
            props[i].setProperty("coherence.distributed.partitions", "257");
            props[i].setProperty("test.threads", "5");
            if (nServers == 4)
                {
                props[i].setProperty("test.backupcount", "2");
                }
            props[i].setProperty("coherence.override", "common-tangosol-coherence-override.xml");

            props[i].setProperty("test.persistence.active.dir", fileActive[i].getAbsolutePath());
            props[i].setProperty("test.persistence.backup.dir", fileBackup[i].getAbsolutePath());
            }

        if (nServers == 4)
            {
            System.setProperty("coherence.distributed.backupcount", "2");
            }

        System.setProperty("coherence.distributed.partitions", "257");

        // reset so service senior picks up BC count/partition count
        AbstractFunctionalTest.stopAllApplications();

        AbstractFunctionalTest._startup();

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final String sServer = "testBackupPersistence" + nServers;
        NamedCache cache = getNamedCache(sCacheName);
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        Cluster cluster = CacheFactory.ensureCluster();

        try
            {
            System.out.println("**** Backup Persistence Start");
            String sServer1 = sServer + "-1";
            String sServer2 = sServer + "-2";
            String sServer3 = sServer + "-3";
            String sServer4 = sServer + "-4";

            String sServer5 = sServer + "-5";

            startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props[0]);
            startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props[1]);
            if (nServers == 4)
                {
                startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props[2]);
                startCacheServer(sServer4, getProjectName(), getCacheConfigPath(), props[3]);
                }

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(nServers));
            waitForBalanced(service);

            if (fRolling)
                {
                for (int i = 0; i < 5; i++)
                    {
                    String sServerExtra = sServer + "-X-" + i;

                    startCacheServer(sServerExtra, getProjectName(), getCacheConfigPath(), props[nServers]);

                    Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(nServers + 1));
                    waitForBalanced(service);

                    // populate with some data
                    PersistenceTestHelper.populateData(cache, 5000);
                    assertEquals(cache.size(), 5000);

                    cache.clear();

                    stopCacheServer(sServerExtra);

                    Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(nServers));
                    waitForBalanced(service);
                    }
                }

            // populate with some data
            PersistenceTestHelper.populateData(cache, 5000);

            // always assert the size to ensure we have not lost data
            assertEquals(cache.size(), 5000);

            // populate with some data, on non-default cache to trigger extents init
            PersistenceTestHelper.populateData(getNamedCache("simple-persistent-new"), 5000);

            // backups persistence is async., need some time to complete
            Base.sleep(10_000);

            String sService = service.getInfo().getServiceName();

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

            cluster.suspendService(sService);

            try
                {
                // abruptly shutdown
                stopCacheServer(sServer1);
                stopCacheServer(sServer2);
                if (nServers == 4)
                    {
                    stopCacheServer(sServer3);
                    stopCacheServer(sServer4);
                    }

                Eventually.assertThat(cluster.getMemberSet().size(), is(1));
                }
            finally
                {
                cluster.resumeService(sService);
                }

            service.shutdown();

            try
                {
                cache = getNamedCache(sCacheName);
                cache.size();
                }
            catch (Throwable t)
                {
                CacheFactory.log("got Exception: " + t);
                }

            if (nServers == 4)
                {
                // provide enough partitions saved
                FileHelper.copyDir(fileActive[2], fileActive[0]);
                FileHelper.copyDir(fileBackup[2], fileBackup[0]);
                }

            // re-start recovering from half the original stores
            startCacheServer(sServer5, getProjectName(), getCacheConfigPath(), props[0]);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));
            waitForBalanced(service);

            assertEquals(5000, cache.size());

            stopCacheServer(sServer5);
            }
        finally
            {
            System.clearProperty("coherence.distributed.backupcount");

            getFactory().destroyCache(cache);

            stopAllApplications();

            CacheFactory.shutdown();

            for (int i = 0; i < nServers; i++)
                {
                FileHelper.deleteDirSilent(fileActive[i]);
                FileHelper.deleteDirSilent(fileBackup[i]);
                }

            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    static Properties createRecoveryProperties(File fileActive, File fileSnapshot, File fileTrash,
                                               String sCluster, int cPartitions, int cThreads)
        {
        Properties props = new Properties();

        props.setProperty("test.persistence.mode", "active");
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.members", "3");
        props.setProperty("test.distribution.members", "3");
        props.setProperty("test.threads", String.valueOf(cThreads));
        props.setProperty("coherence.cluster", sCluster);
        props.setProperty("coherence.distributed.partitions", String.valueOf(cPartitions));
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        return props;
        }

    static void populateEveryPartition(NamedCache cache, int cPartitions, int cEntriesPerPartition)
        {
        Map mapBatch = new HashMap();

        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            for (int iEntry = 0; iEntry < cEntriesPerPartition; iEntry++)
                {
                mapBatch.put(new CompositeKey(SimplePartitionKey.getPartitionKey(iPart), iPart + "-" + iEntry),
                        String.format("value-%05d-%03d", iPart, iEntry));

                if (mapBatch.size() >= 1024)
                    {
                    cache.putAll(mapBatch);
                    mapBatch.clear();
                    }
                }
            }

        if (!mapBatch.isEmpty())
            {
            cache.putAll(mapBatch);
            }
        }

    static void assertNoIndexInconsistencies(NamedCache cache, int cPartitions, int cExpectedIndexes, String sStage)
        {
        int  cTimeoutSeconds    = Integer.getInteger("test.index.recovery.consistency.timeout.seconds", 120);
        long ldtTimeout         = Base.getSafeTimeMillis() + TimeUnit.SECONDS.toMillis(cTimeoutSeconds);
        Map  mapInconsistencies = collectIndexInconsistencies(cache, cPartitions, cExpectedIndexes);

        while (!mapInconsistencies.isEmpty() && Base.getSafeTimeMillis() < ldtTimeout)
            {
            Base.sleep(500L);
            mapInconsistencies = collectIndexInconsistencies(cache, cPartitions, cExpectedIndexes);
            }

        assertTrue("Expected no index inconsistencies after " + sStage + " but found " + mapInconsistencies,
                mapInconsistencies.isEmpty());
        }

    private static Map collectIndexInconsistencies(NamedCache cache, int cPartitions, int cExpectedIndexes)
        {
        Map mapResults         = cache.invokeAll(createPartitionKeySet(cPartitions),
                new PartitionIndexCheckProcessor(cExpectedIndexes));
        Map mapInconsistencies = new LinkedHashMap();

        for (Iterator iter = mapResults.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry         entry            = (Map.Entry) iter.next();
            SimplePartitionKey key             = (SimplePartitionKey) entry.getKey();
            Map               mapPartitionInfo = (Map) entry.getValue();

            if (mapPartitionInfo != null && !mapPartitionInfo.isEmpty())
                {
                mapInconsistencies.put(key.getPartitionId(), mapPartitionInfo);
                }
            }

        return mapInconsistencies;
        }

    private static Set createPartitionKeySet(int cPartitions)
        {
        Set setKeys = new LinkedHashSet(cPartitions);

        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            setKeys.add(SimplePartitionKey.getPartitionKey(iPart));
            }

        return setKeys;
        }

    static void restoreSystemProperty(String sProperty, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sProperty);
            }
        else
            {
            System.setProperty(sProperty, sValue);
            }
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(90, TimeUnit.MINUTES);

    // ----- helper processor ----------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class PartitionIndexCheckProcessor
            extends AbstractProcessor
            implements ExternalizableLite
        {
        public PartitionIndexCheckProcessor()
            {
            }

        public PartitionIndexCheckProcessor(int cExpectedIndexes)
            {
            m_cExpectedIndexes = cExpectedIndexes;
            }

        @Override
        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry              binEntry = entry.asBinaryEntry();
            BackingMapContext        bmc      = binEntry.getBackingMapContext();
            BackingMapManagerContext bmmc     = bmc.getManagerContext();
            PartitionedService       service  = (PartitionedService) bmmc.getCacheService();
            PartitionedCache         cacheSvc = (PartitionedCache) service;
            Storage                  storage  = cacheSvc.getStorage(bmc.getCacheName());
            int                      nPart    = binEntry.getKeyPartition();

            if (storage == null)
                {
                return new LinkedHashMap();
                }

            Set setBackingKeys = storage.collectKeySet(nPart);
            int cBackingKeys   = setBackingKeys.size();
            int cRegistered    = storage.getIndexExtractorMap().size();

            Map mapIssues = new LinkedHashMap();

            if (cBackingKeys == 0)
                {
                return mapIssues;
                }

            if (cRegistered < m_cExpectedIndexes)
                {
                mapIssues.put("<registration>",
                        "backing=" + cBackingKeys + ", registered=" + cRegistered
                        + ", expected=" + m_cExpectedIndexes);

                if (cRegistered == 0)
                    {
                    return mapIssues;
                    }
                }

            Map mapPartitioned = storage.getPartitionedIndexMap();
            Map mapPartIndex   = mapPartitioned == null
                                 ? null
                                 : (Map) mapPartitioned.get(Integer.valueOf(nPart));

            for (Iterator iter = storage.getIndexExtractorMap().keySet().iterator(); iter.hasNext(); )
                {
                ValueExtractor extractor = (ValueExtractor) iter.next();
                MapIndex       index     = mapPartIndex == null
                                           ? null
                                           : (MapIndex) mapPartIndex.get(extractor);

                if (index == null)
                    {
                    mapIssues.put(String.valueOf(extractor),
                            "backing=" + cBackingKeys + ", index=0, missing=" + cBackingKeys);
                    continue;
                    }

                if (index instanceof SimpleMapIndex)
                    {
                    Set setIndexKeys = new HashSet();

                    for (Iterator iterContents = ((SimpleMapIndex) index).getIndexContents().values().iterator();
                         iterContents.hasNext(); )
                        {
                        setIndexKeys.addAll((Collection) iterContents.next());
                        }

                    if (!setBackingKeys.equals(setIndexKeys))
                        {
                        Set setMissing = new HashSet(setBackingKeys);
                        setMissing.removeAll(setIndexKeys);

                        if (!setMissing.isEmpty())
                            {
                            Set setExtra = new HashSet(setIndexKeys);
                            setExtra.removeAll(setBackingKeys);

                            StringBuilder sb = new StringBuilder()
                                    .append("backing=").append(cBackingKeys)
                                    .append(", index=").append(setIndexKeys.size())
                                    .append(", missing=").append(setMissing.size());

                            if (!setExtra.isEmpty())
                                {
                                sb.append(", extra=").append(setExtra.size());
                                }

                            mapIssues.put(String.valueOf(extractor), sb.toString());
                            }
                        }
                    }
                }

            return mapIssues;
            }

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_cExpectedIndexes = in.readInt();
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            out.writeInt(m_cExpectedIndexes);
            }

        private int m_cExpectedIndexes;
        }
    }
