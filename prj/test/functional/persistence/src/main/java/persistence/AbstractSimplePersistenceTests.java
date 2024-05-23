/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Timeout;

import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.deferred.atomic.DeferredAtomicInteger;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.coherence.persistence.PersistentStoreInfo;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.Action;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.PartitionedService.PartitionRecoveryAction;
import com.tangosol.net.Service;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.net.partition.SimplePartitionKey;

import com.tangosol.persistence.CachePersistenceHelper;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapIndex;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.Count;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.GreaterEqualsFilter;

import com.tangosol.persistence.GUIDHelper;
import com.tangosol.util.listener.SimpleMapListener;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.AbstractRollingRestartTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.Assert.*;

/**
 * Functional tests for simple cache persistence and recovery.
 *
 * @author jh  2012.07.12
 */
public abstract class AbstractSimplePersistenceTests
        extends AbstractFunctionalTest
    {

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.remote", "true");
        System.setProperty("coherence.management.refresh.expiry", "1s");
        System.setProperty("coherence.management.http", "inherit");
        System.setProperty("coherence.management.metrics.port", "0");

        AbstractFunctionalTest._startup();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Tests persistence and recovery of a single cache server.
     */
    @Test
    public void testSingleServer()
            throws IOException
        {
        testBasicPersistence("testSingleServer" + getPersistenceManagerName(),
                "simple-persistent",
                "simple-transient");
        }

    /**
     * Tests persistence and recovery of a 2 cache servers with async backups with SEONE enabled.
     */
    @Test
    public void testSingleServerAsyncSEONE()
            throws IOException
        {
        testBasicPersistence2("test1ServerAsyncSEONE" + getPersistenceManagerName(), "simple-persistent", true, false, true);
        }

    /**
     * Tests persistence and recovery of a 2 cache servers with async backups with SEONE disabled.
     */
    @Test
    public void testSingleServerAsync()
            throws IOException
        {
        testBasicPersistence2("test1ServerAsync" + getPersistenceManagerName(), "simple-persistent", true, false, false);
        }

    /**
     * Tests persistence and recovery of a 2 cache servers with worker threads
     * with SEONE enabled.
     */
    @Test
    public void testSingleServerWorkersSEONE()
            throws IOException
        {
        testBasicPersistence2("test1ServerWorkersSEONE" + getPersistenceManagerName(),
                "simple-persistent", false, true, true);
        }

    /**
     * Tests persistence and recovery of a 2 cache servers with worker threads
     * with SEONE disabled.
     */
    @Test
    public void testSingleServerWorkers()
            throws IOException
        {
        testBasicPersistence2("test1ServerWorkers" + getPersistenceManagerName(),
                "simple-persistent", false, true, false);
        }

    /**
     * Tests persistence and recovery of a 2 cache servers with async backups
     * and worker threads with SEONE enabled.
     */
    @Test
    public void testSingleServerAsyncWorkersSEONE()
            throws IOException
        {
        testBasicPersistence2("test1ServerAsyncWorkersSEONE" + getPersistenceManagerName(),
                "simple-persistent", true, true, true);
        }

    /**
     * Tests persistence and recovery of a 2 cache servers with async backups
     * and worker threads.
     */
    @Test
    public void testSingleServerAsyncWorkers()
            throws IOException
        {
        testBasicPersistence2("test1ServerAsyncWorkers" + getPersistenceManagerName(),
                "simple-persistent", true, true, false);
        }

    /**
     * Tests the create and recover snapshot functionality.
     */
    @Test
    public void testPassiveSnapshot()
            throws IOException, MBeanException
        {
        testBasicSnapshot("testPassiveSnapshot"+ getPersistenceManagerName(), "simple-persistent", false);
        }

    /**
     * Tests the create and recover snapshot functionality.
     */
    @Test
    public void testActiveSnapshot()
            throws IOException, MBeanException
        {
        testBasicSnapshot("testActiveSnapshot" + getPersistenceManagerName(), "simple-persistent", true);
        }

    /**
     * Test archivers while in active mode for up to 4 servers.
     */
    @Test
    public void testActiveArchiver()
            throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, MBeanException
        {
        testBasicArchiver("testActiveArchiver" + getPersistenceManagerName(), "simple-archiver", true, 4);
        }

    /**
     * Test for a regression in Bug 25522362 - Retrieval of Archived Snapshot fails in 2 member cluster setup.
     */
    @Test
    public void testBug25522362Regression() throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, MBeanException
        {
        testSEOneArchiver("testBug25522362", "simple-archiver");
        }

     /**
      * Test archivers while in passive (on-demand) mode for up to 3 servers.
      */
    @Test
    public void testPassiveArchiver()
            throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, MBeanException
        {
        testBasicArchiver("testPassiveArchiver" + getPersistenceManagerName(), "simple-archiver", false, 3);
        }

    /**
     * Test tools API using MBeanServerProxy.
     */
    @Test
    public void testToolsAPIWithProxy()
            throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
                   MBeanException, ExecutionException
        {
        testToolsAPI("testToolsAPIProxy" + getPersistenceManagerName(), "simple-archiver", true);
        }

    /**
     * Test multiple restarts and wait for balanced between each one.
     */
    @Test
    public void testMultipleRestartsBalanced()
            throws IOException, MBeanException
        {
        testMultipleRestarts("testMultipleBalanced", "active", "simple-persistent", 10, true);
        }

    /**
     * Test multiple restarts and wait for balanced between each one.
     */
    @Test
    public void testMultipleRestartsBalancedBackup()
            throws IOException, MBeanException
        {
        testMultipleRestarts("testMultipleBalancedBackup", "active-backup", "simple-persistent", 10, true);
        }

    /**
     * Test truncate removes persisted data.
     */
    @Test
    public void testTruncate()
            throws IOException
        {
        testTruncate("testTruncate" + getPersistenceManagerName(),
                "simple-persistent", "simple-persistent-truncate");
        }

    /**
     * Test CQC cache recovery.
     * @throws IOException
     * @throws MBeanException
     */
    @Test
    public void testCqcSnapshotRecovery()
            throws IOException, MBeanException
        {
        testCqcSnapshotRecovery("testCqcSnapshot" + getPersistenceManagerName(), "simple-persistent");
        }

    // ----- helpers --------------------------------------------------------

    private void testBasicPersistence(String sServer, String sPersistentCache, String sTransientCache)
            throws IOException
        {
        File fileActive   = FileHelper.createTempDir();
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        Properties props  = new Properties();
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        Cluster cluster = CacheFactory.getCluster();

        CoherenceClusterMember clusterMember = startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        NamedCache         cache   = getNamedCache(sPersistentCache);
        PartitionedService service = (PartitionedService) cache.getCacheService();

        try
            {
            final String KEY   = "key";
            final String VALUE = "value";

            Eventually.assertThat(invoking(clusterMember).isServiceRunning(service.getInfo().getServiceName()), is(true));
            AbstractRollingRestartTest.waitForNoOrphans(cache.getCacheService());

            // assert that the persistence root was created
            File dir = new File(fileActive,
                    FileHelper.toFilename(CacheFactory.getCluster().getClusterName()));
            dir = new File(dir,
                    FileHelper.toFilename(service.getInfo().getServiceName()));
            assertTrue(dir.exists());

            cache.put(KEY, VALUE);
            getNamedCache(sTransientCache).put(KEY, VALUE);

            int cPart = service.getPartitionCount();
            int nPart = service.getKeyPartitioningStrategy().getKeyPartition(KEY);

            stopCacheServer(sServer + "-1");
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));

            // assert that the persistence root still exists
            assertTrue(dir.exists());

            // validate the contents of the persistence root
            PersistenceManager<ReadBuffer> manager = createPersistenceManager(dir);
            try
                {
                PersistentStoreInfo[] aInfo = manager.listStoreInfo();
                assertEquals(cPart, aInfo.length);

                // assert that each partition is valid and holds the cache
                // name as well as find the GUID for the partition that holds
                // the persisted data during the scan
                String sGUID = null;
                for (int i = 0; i < aInfo.length; ++i)
                    {
                    String s = aInfo[i].getId();

                    if (manager.isEmpty(s))
                        {
                        continue;
                        }

                    PersistentStore<ReadBuffer> store = manager.open(s, null);
                    try
                        {
                        LongArray la = CachePersistenceHelper.getCacheNames(store);
                        if (GUIDHelper.getPartition(s) == nPart)
                            {
                            assertEquals(1, la.getSize());
                            assertEquals(sPersistentCache, la.iterator().next());
                            }
                        }
                    finally
                        {
                        manager.close(s);
                        }

                    if (s.startsWith(nPart + "-"))
                        {
                        sGUID = s;
                        }
                    }
                assertNotNull(sGUID);

                // assert that data was persisted
                final Map<Long, Map.Entry<ReadBuffer, ReadBuffer>> map =
                        new HashMap<>();
                PersistentStore<ReadBuffer> store = manager.open(sGUID, null);
                try
                    {
                    store.iterate((lExtentId, bufKey, bufValue) ->
                        {
                        if (lExtentId > 0L)
                            {
                            map.put(Long.valueOf(lExtentId),
                                    new SimpleMapEntry(bufKey, bufValue));
                            }
                        return true;
                        });
                    }
                finally
                    {
                    manager.close(sGUID);
                    }
                assertEquals(1, map.size());

                // validate the persisted data
                Map.Entry<ReadBuffer, ReadBuffer> entry = map.values().iterator().next();
                assertEquals(KEY,   ExternalizableHelper.fromBinary(entry.getKey().toBinary()));
                assertEquals(VALUE, ExternalizableHelper.fromBinary(entry.getValue().toBinary()));
                }
            finally
                {
                manager.release();
                }

            // restart the server and assert that all (and only) persisted
            // data was recovered
            clusterMember = startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning(service.getInfo().getServiceName()), is(true));
            AbstractRollingRestartTest.waitForNoOrphans(cache.getCacheService());

            assertEquals(VALUE, cache.get(KEY));
            assertNull(getNamedCache(sTransientCache).get(KEY));

            stopCacheServer(sServer + "-2");
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    private void testBasicPersistence2(String sServer, String sPersistentCache,
                                       boolean fAsyncBackup, boolean fWorkers,
                                       boolean fisSEONE )
            throws IOException
        {
        File fileActive   = FileHelper.createTempDir();
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        Properties props = fisSEONE ? new Properties() : PROPS_SEONE;

        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.threads", fWorkers ? "5" : "0");
        props.setProperty("test.asyncbackup", fAsyncBackup ? "true" : "false");
        props.setProperty("test.persistence.members", "2");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final NamedCache        cache   = getNamedCache(sPersistentCache);
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster = service.getCluster();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
        try
            {
            // add a bunch of data; do this over multiple client threads to
            // exercise the worker and persistence thread pools
            cache.put("foo", "bar");
            cache.put("biz", "baz");

            int      cThreads = 5;
            int      cPuts    = 10000;
            int      cMax     = cThreads * cPuts;
            Thread[] aThreads = new Thread[cThreads];
            for (int i = 0; i < cThreads; i++)
                {
                final int iThread = i;
                aThreads[i] = new Thread()
                    {
                    public void run()
                        {
                        HashMap map = new HashMap();
                        for (int i = 0; i < cPuts; i++)
                            {
                            int nVal = iThread * cPuts + i;
                            map.put(nVal, nVal);
                            }

                        cache.putAll(map);
                        }
                    };
                aThreads[i].start();
                }
            for (int i = 0; i < cThreads; i++)
                {
                aThreads[i].join();
                }

            // add some indices and triggers
            ValueExtractor ext1 = new ReflectionExtractor("toString");
            ValueExtractor ext2 = new ReflectionExtractor("hashCode");
            MapTrigger trigger1 = new FilterTrigger(AlwaysFilter.INSTANCE());
            MapTrigger trigger2 = new FilterTrigger(new GreaterEqualsFilter<>(ValueExtractor.identity(), Integer.valueOf(cThreads * cPuts)));

            cache.addIndex(ext1, false, null);
            cache.addIndex(ext2, true, SafeComparator.INSTANCE);
            cache.addMapListener(new MapTriggerListener(trigger1));
            cache.addMapListener(new MapTriggerListener(trigger2));

            AtomicInteger atomicListenerCalls = new AtomicInteger();
            MapListener<?, ?> listener = new SimpleMapListener<>().addInsertHandler(
                    mapEvent -> atomicListenerCalls.incrementAndGet());

            Set<Integer> setKeys = new HashSet<>(10);
            for (int i = cMax; i < cMax + 10; ++i)
                {
                setKeys.add(i);
                cache.addMapListener(listener, i, false);
                }

            // validate that the added indices and triggers exist
            validateIndexTrigger(cache, new Object[]{ext1, ext2}, new Object[]{trigger1, trigger2});

            stopCacheServer(sServer + "-1", true);
            stopCacheServer(sServer + "-2", true);

            int cRestarts = 2;
            for (int i = 3, c = i + cRestarts * 2; i < c; i += 2)
                {
                Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));

                startCacheServer(sServer + "-" +  i     , getProjectName(), getCacheConfigPath(), props);
                startCacheServer(sServer + "-" + (i + 1), getProjectName(), getCacheConfigPath(), props);

                Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
                waitForBalanced(service);

                // validate that the data were restored
                assertEquals("bar", cache.get("foo"));
                assertEquals("baz", cache.get("biz"));

                for (int j = 0; j < cThreads * cPuts; j++)
                    {
                    assertEquals(j, cache.get(j));
                    }

                // validate that the indices and triggers were recreated
                validateIndexTrigger(cache, new Object[]{ext1, ext2}, new Object[]{trigger1, trigger2});

                // validate that the listener continues to fire after the restart
                int cCalls   = atomicListenerCalls.get();

                setKeys.forEach(j -> cache.put(j, j));

                Eventually.assertThat("Expected " + setKeys.size() + " updates to be seen by a registered MapListener",
                        new DeferredAtomicInteger(atomicListenerCalls),
                        is(Integer.valueOf(cCalls + setKeys.size())));

                cache.keySet().removeAll(setKeys);

                stopCacheServer(sServer + "-" + i);
                stopCacheServer(sServer + "-" + (i + 1));
                }
            }
        catch (InterruptedException e)
            {
            fail();
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

    private void testTruncate(String sServer, String sPersistentCache, String sTruncateCache)
            throws IOException
        {
        File fileActive   = FileHelper.createTempDir();
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        Properties props  = new Properties();
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.threads", "5");
        props.setProperty("test.persistence.members", "2");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        NamedCache cache         = getNamedCache(sPersistentCache);
        NamedCache cacheTruncate = getNamedCache(sTruncateCache);

        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster = service.getCluster();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
        waitForBalanced(service);
        try
            {
            // add a bunch of data; do this over multiple client threads to
            // exercise the worker and persistence thread pools
            cache.put("foo", "bar");
            cache.put("biz", "baz");

            int      cThreads = 5;
            int      cPuts    = 1000;
            Thread[] aThreads = new Thread[cThreads];
            for (int i = 0; i < cThreads; i++)
                {
                final int iThread = i;
                aThreads[i] = new Thread()
                    {
                    public void run()
                        {
                        HashMap map = new HashMap();
                        for (int i = 0; i < cPuts; i++)
                            {
                            int nVal = iThread * cPuts + i;
                            map.put(nVal, nVal);
                            }

                        cache.putAll(map);
                        cacheTruncate.putAll(map);
                        }
                    };
                aThreads[i].start();
                }
            for (int i = 0; i < cThreads; i++)
                {
                aThreads[i].join();
                }

            cacheTruncate.truncate();

            stopCacheServer(sServer + "-1");
            stopCacheServer(sServer + "-2");

            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));

            startCacheServer(sServer + "-3", getProjectName(), getCacheConfigPath(), props);
            startCacheServer(sServer + "-4", getProjectName(), getCacheConfigPath(), props);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
            waitForBalanced(service);

            // validate that the data were restored
            assertEquals("bar", cache.get("foo"));
            assertEquals("baz", cache.get("biz"));

            for (int j = 0; j < cThreads * cPuts; j++)
                {
                assertEquals(j, cache.get(j));
                }

            if (!cacheTruncate.isEmpty())
                {
                fail(String.format("cache %s did not truncate; size: %s\n",
                        cacheTruncate.getCacheName(), cacheTruncate.size()));
                }

            stopCacheServer(sServer + "-3");
            stopCacheServer(sServer + "-4");
            }
        catch (InterruptedException e)
            {
            fail();
            }
        finally
            {
            getFactory().destroyCache(cache);
            getFactory().destroyCache(cacheTruncate);

            stopAllApplications();
            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    /**
     * Test for basic snapshot create, recover and remove functionality.
     *
     * @param sServer           the prefix name of the servers to create
     * @param sPersistentCache  the name of the cache
     * @param fActive           true iff the servers should be in active persistence mode
     */
    private void testBasicSnapshot(String sServer, String sPersistentCache, boolean fActive)
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = fActive ? FileHelper.createTempDir() : null;
        File fileTrash    = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", fActive ? "active" : "on-demand");
        props.setProperty("test.persistence.active.dir", fActive ? fileActive.getAbsolutePath() : "");
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("test.threads", "1");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final NamedCache        cache    = getNamedCache(sPersistentCache);
        DistributedCacheService service  = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster  = service.getCluster();
        String                  sService = service.getInfo().getServiceName();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
        waitForBalanced(service);

        PersistenceTestHelper helper = new PersistenceTestHelper();

        try
            {
            // add a bunch of data
            cache.put("foo", "bar");
            cache.put("biz", "baz");

            HashMap mapTemp = new HashMap();
            for (int i = 0; i < 10000; i++)
                {
                mapTemp.put(i, i);
                }
            cache.putAll(mapTemp);

            // add some indices and triggers
            ValueExtractor ext1 = new ReflectionExtractor("toString");
            ValueExtractor ext2 = new ReflectionExtractor("hashCode");
            MapTrigger trigger1 = new FilterTrigger(AlwaysFilter.INSTANCE);

            cache.addIndex(ext1, false, null);
            cache.addIndex(ext2, true, SafeComparator.INSTANCE);
            cache.addMapListener(new MapTriggerListener(trigger1));

            // validate that the added indices and triggers exist
            validateIndexTrigger(cache, new Object[] {ext1, ext2}, new Object[] {trigger1});

            cluster.suspendService(sService);
            helper.createSnapshot(sService, "snapshot-A");
            cluster.resumeService(sService);

            mapTemp = new HashMap();
            for (int i = 10000; i < 20000; i++)
                {
                mapTemp.put(i, i);
                }
            cache.putAll(mapTemp);

            // if we do not call suspend service, then the snapshot is created
            // with partitioned consistency
            Thread[] aReaders = ensureContinuousReaders(cache, 8);
            Base.sleep(100L);  // allow for some successful reads
            cache.put("foo", "BAR");
            cache.put("biz", "BAZ");
            helper.createSnapshot(sService, "snapshot-B");
            destroyContinuousReaders(aReaders);

            // intentionally lose all data
            stopCacheServer(sServer + "-1");
            stopCacheServer(sServer + "-2");
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(0));

            startCacheServer(sServer + "-3", getProjectName(), getCacheConfigPath(), props);
            startCacheServer(sServer + "-4", getProjectName(), getCacheConfigPath(), props);
            startCacheServer(sServer + "-5", getProjectName(), getCacheConfigPath(), props);
            startCacheServer(sServer + "-6", getProjectName(), getCacheConfigPath(), props);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(4));
            waitForBalanced(service);

            if (fActive)
                {
                // if in active persistence mode, we should not have lost anything,
                // and automatic recovery should have restored the logical state
                // of "snapshot-B"

                // validate that the data were recovered
                assertEquals("BAR", cache.get("foo"));
                assertEquals("BAZ", cache.get("biz"));
                for (int i = 0; i < 20000; i++)
                    {
                    assertEquals(i, cache.get(i));
                    }

                // validate that the indices and triggers were recreated
                validateIndexTrigger(cache, new Object[] {ext1, ext2}, new Object[] {trigger1});
                }
            else
                {
                // nothing should have survived
                assertEquals(0, cache.size());
                }

            // if we do not call suspend service, then the service will be
            // automatically suspended and then resumed
            helper.recoverSnapshot(sService, "snapshot-A");
            waitForBalanced(service);

            // validate that the data were recovered
            assertEquals("bar", cache.get("foo"));
            assertEquals("baz", cache.get("biz"));
            for (int i = 0; i < 10000; i++)
                {
                assertEquals(i, cache.get(i));
                }

            // validate that the indices and triggers were recreated
            validateIndexTrigger(cache, new Object[] {ext1, ext2}, new Object[] {trigger1});

            helper.recoverSnapshot(sService, "snapshot-B");
            waitForBalanced(service);

            // validate that the data were recovered
            assertEquals("BAR", cache.get("foo"));
            assertEquals("BAZ", cache.get("biz"));
            for (int i = 0; i < 20000; i++)
                {
                assertEquals(i, cache.get(i));
                }

            // validate that the indices and triggers were recreated
            validateIndexTrigger(cache, new Object[] {ext1, ext2}, new Object[] {trigger1});

            // test partial failure
            stopCacheServer(sServer + "-3");
            stopCacheServer(sServer + "-4");
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
            waitForBalanced(service);

            helper.ensurePersistenceMBean(sService);
            helper.recoverSnapshot(sService, "snapshot-B");

            waitForBalanced(service);

            // validate that the data were recovered
            assertEquals("BAR", cache.get("foo"));
            assertEquals("BAZ", cache.get("biz"));
            for (int i = 0; i < 20000; i++)
                {
                assertEquals(i, cache.get(i));
                }

            // validate that the indices and triggers were recreated
            validateIndexTrigger(cache, new Object[] {ext1, ext2}, new Object[] {trigger1});

            // validate that "snapshot-B" is gone when we issue a removeSnapshot
            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(2));

            helper.removeSnapshot(sService, "snapshot-B");

            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(1));
            assertThat(listSnapshots(helper, sService).get(0), is("snapshot-A"));

            // validate that "snapshot-A" is gone when we issue a removeSnapshot
            helper.removeSnapshot(sService, "snapshot-A");

            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(0));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();

            if (fActive)
                {
                FileHelper.deleteDirSilent(fileActive);
                }
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    /**
     * Start {@code cThreads} that continuously execute read (aggregation) requests,
     * and return references to the created threads.
     *
     * @param cache     the cache to perform the requests against
     * @param cThreads  the number of threads to create
     *
     * @return references to the created threads
     */
    private Thread[] ensureContinuousReaders(NamedCache cache, int cThreads)
        {
        Runnable r = () ->
            {
            while (true)
                {
                try
                    {
                    cache.aggregate(new Count());
                    }
                catch (RuntimeException e)
                    {
                    break;
                    }
                }
            };

        Thread[] aReaders = new Thread[cThreads];
        for (int i = 0; i < cThreads; ++i)
            {
            (aReaders[i] = new Thread(r)).start();
            }
        return aReaders;
        }

    /**
     * Destroy the provided threads via interruption.
     *
     * @param aReaders  the threads to destroy
     */
    private void destroyContinuousReaders(Thread[] aReaders)
        {
        for (int i = 0, c = aReaders == null ? 0 : aReaders.length; i < c; ++i)
            {
            aReaders[i].interrupt();
            try
                {
                aReaders[i].join();
                }
            catch (InterruptedException e)
                {
                }
            }
        }

    /**
     * Test basic archiver functionality.
     *
     * @param sServer           the prefix name of the servers to create
     * @param sPersistentCache  the name of the cache
     * @param fActive           true iff the servers should be in active persistence mode
     */
    private void testBasicArchiver(String sServer, String sPersistentCache, boolean fActive, int nMaxServers)
            throws IOException, NoSuchMethodException, IllegalAccessException,
                   InvocationTargetException, MBeanException {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = fActive ? FileHelper.createTempDir() : null;
        File fileTrash    = FileHelper.createTempDir();
        File fileArchive  = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", fActive ? "active" : "on-demand");
        props.setProperty("test.persistence.active.dir", fActive ? fileActive.getAbsolutePath() : "");
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.archive.dir", fileArchive.getAbsolutePath());
        props.setProperty("test.start.archiver", "true");
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.refresh.expiry", "1s");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final NamedCache        cache    = getNamedCache(sPersistentCache);
        DistributedCacheService service  = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster  = service.getCluster();
        String                  sService = service.getInfo().getServiceName();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));

        final String sEmptySnapshot = "empty-cluster";
        final String sSnapshot10000 = "snapshot-10000";
        try
            {
            PersistenceToolsHelper helper = new PersistenceToolsHelper();
            helper.setPrintWriter(new PrintWriter(System.out));

            for (int i = 2; i <= nMaxServers; i++)
                {
                System.out.println("Iteration: " + i);
                // each iteration start another cache server to test out
                // slightly different scenarios with the archival
                // iteration 1:  server-1 & server-2
                // iteration 2:  server-1, server-2 & server-3
                // iteration 3:  server-1, server-2, server-3 & server-4

                startCacheServer(sServer + "-" + i, getProjectName(), getCacheConfigPath(), props);
                Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(i));
                waitForBalanced(service);

                cache.clear();
                assertEquals(0, cache.size());

                // create an empty-cluster snapshot
                helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, sEmptySnapshot, sService);

                // create a second snapshot with 10,000 entries
                PersistenceTestHelper.populateData(cache, 10000);
                assertEquals(10000, cache.size());

                helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, sSnapshot10000, sService);

                // archive the snapshots
                helper.invokeOperationWithWait(PersistenceToolsHelper.ARCHIVE_SNAPSHOT, sEmptySnapshot, sService);
                Eventually.assertThat(invoking(this).getArchivedSnapshotCount(helper, sService), is(1));

                String[] asArchivedSnapshots = helper.listArchivedSnapshots(sService);
                assertTrue(asArchivedSnapshots != null & asArchivedSnapshots.length == 1);
                assertEquals(asArchivedSnapshots[0], sEmptySnapshot);

                helper.invokeOperationWithWait(PersistenceToolsHelper.ARCHIVE_SNAPSHOT, sSnapshot10000, sService);
                Eventually.assertThat(invoking(this).getArchivedSnapshotCount(helper, sService), is(2));

                asArchivedSnapshots = helper.listArchivedSnapshots(sService);
                assertTrue(asArchivedSnapshots != null && asArchivedSnapshots.length == 2 &&
                        (sSnapshot10000.equals(asArchivedSnapshots[0]) ||
                         sSnapshot10000.equals(asArchivedSnapshots[1])));

                // remove the local snapshots
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sEmptySnapshot, sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sSnapshot10000, sService);

                Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(0));

                // retrieve and recover the empty cluster snapshot
                helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sEmptySnapshot, sService);

                Eventually.assertThat(listSnapshots(helper, sService), is(containsInAnyOrder(sEmptySnapshot)));

                // if we do not call suspend service, then the service will be
                // automatically suspended and then resumed
                helper.invokeOperationWithWait(PersistenceToolsHelper.RECOVER_SNAPSHOT, sEmptySnapshot, sService);
                assertEquals(0, cache.size());

                // retrieve and recover the 10000 object snapshot
                helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sSnapshot10000, sService);
                Eventually.assertThat(listSnapshots(helper, sService),
                                      is(containsInAnyOrder(sEmptySnapshot, sSnapshot10000)));

                cluster.suspendService(sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.RECOVER_SNAPSHOT, sSnapshot10000, sService);
                cluster.resumeService(sService);
                assertEquals(10000, cache.size());

                // Test to ensure COH-11028 does not re-appear
                for (int j = 0; j < 10 ; j++)
                    {
                    System.out.println("Iteration " + j);
                    helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sSnapshot10000, sService);
                    Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(1));
                    helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, sSnapshot10000, sService);
                    Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(2));
                    }

                // purge the archived snapshots
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_ARCHIVED_SNAPSHOT, sEmptySnapshot, sService);

                // try and retrieve, this should fail with exception
                try (Timeout timeout = Timeout.after(240, TimeUnit.SECONDS))
                    {
                    helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sEmptySnapshot, sService);
                    fail("MBeanException should have been raised for retrieve snapshot " + sEmptySnapshot);
                    }
                catch (Exception e)
                    {
                    System.out.println("Expected exception: Ignore");
                    // expected, just ignore
                    }

                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_ARCHIVED_SNAPSHOT, sSnapshot10000, sService);

                // try and retrieve, this should fail with exception
                try
                    {
                    helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sSnapshot10000, sService);
                    fail("MBeanException should have been raised for retrieve snapshot " + sSnapshot10000);
                    }
                catch (Exception e)
                    {
                    // expected, just ignore
                    System.out.println("Expected exception: Ignore");
                    }

                // Cleanup
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sEmptySnapshot, sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sSnapshot10000, sService);
                Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(0));
                }
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();

            if (fActive)
                {
                FileHelper.deleteDirSilent(fileActive);
                }
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            FileHelper.deleteDirSilent(fileArchive);
            }
        }

    /**
     * Test for Bug 25522362 regression where retreive archived snapshot does not work under SEO One.
     *
     * @param sServer           the prefix name of the servers to create
     * @param sPersistentCache  the name of the cache
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws MBeanException
     */
    private void testSEOneArchiver(String sServer, String sPersistentCache)
            throws IOException, NoSuchMethodException, IllegalAccessException,
                   InvocationTargetException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();
        File fileArchive  = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode",  "active");
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.archive.dir", fileArchive.getAbsolutePath());
        props.setProperty("test.start.archiver", "true");
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.refresh.expiry", "1s");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final NamedCache        cache    = getNamedCache(sPersistentCache);
        DistributedCacheService service  = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster  = service.getCluster();
        String                  sService = service.getInfo().getServiceName();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);
        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));

        final String sEmptySnapshot = "empty-cluster";
        PersistenceToolsHelper helper = new PersistenceToolsHelper();

        LastMessageNotificationListener listener = new LastMessageNotificationListener();

        try
            {
            registerNotificationListener(sService, listener);

            cache.clear();
            assertEquals(0, cache.size());

            // create an empty-cluster snapshot
            helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, sEmptySnapshot, sService);
            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(1));

            // archive the snapshots
            helper.invokeOperationWithWait(PersistenceToolsHelper.ARCHIVE_SNAPSHOT, sEmptySnapshot, sService);
            Eventually.assertThat(invoking(this).getArchivedSnapshotCount(helper, sService), is(1));

            // remove the local snapshot
            helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sEmptySnapshot, sService);
            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(0));

            // retrieve archived snapshot
            helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sEmptySnapshot, sService);

            // If we get the following this means that the Bug has reappeared
            //    "Failed to retrieve  archived snapshot "empty-cluster"
            // Successful message is:
            //    "Successfully retrieved  archived snapshot "empty-cluster"

            Eventually.assertThat(invoking(listener).getLastMessage(), startsWith("Successfully retrieved"));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            FileHelper.deleteDirSilent(fileArchive);
            }
        }


    /**
     * Test the ToolsAPI functionality which is utilized by CohQL.
     *
     * @param sServer           the prefix name of the servers to create
     * @param sPersistentCache  the name of the cache
     * @param fActive           true iff the servers should be in active persistence mode
     *
     * @throws ExecutionException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws MBeanException
     * @throws NoSuchMethodException
     */
    private void testToolsAPI(String sServer, String sPersistentCache, boolean fActive)
            throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
                   MBeanException, ExecutionException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = fActive ? FileHelper.createTempDir() : null;
        File fileTrash    = FileHelper.createTempDir();
        File fileArchive  = FileHelper.createTempDir();

        Properties props = new Properties();

        props.setProperty("test.persistence.mode", fActive ? "active" : "on-demand");
        props.setProperty("test.persistence.active.dir", fActive ? fileActive.getAbsolutePath() : "");
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.archive.dir", fileArchive.getAbsolutePath());
        props.setProperty("test.start.archiver", "true");
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        NamedCache              cache    = getNamedCache(sPersistentCache);
        DistributedCacheService service  = (DistributedCacheService) cache.getCacheService();
        String                  sService = service.getInfo().getServiceName();
        PersistenceToolsHelper  helper;

        String sEmptyClusterSnapshot = "empty-cluster";
        String sSnapshot10000        = "snapshot-10000";

        try
            {
            startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
            startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);
            startCacheServer(sServer + "-3", getProjectName(), getCacheConfigPath(), props);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
            waitForBalanced(service);

            helper = new PersistenceToolsHelper();
            helper.setPrintWriter(new PrintWriter(System.out));

            cache.clear();
            assertEquals(0, cache.size());

            // create an empty snapshot
            helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, sEmptyClusterSnapshot, sService);

            // list the current snapshots
            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(1));

            String[] asSnapshots = helper.listSnapshots(sService);

            assertEquals(sEmptyClusterSnapshot, asSnapshots[0]);

            // create a second snapshot with 10,000 entries
            PersistenceTestHelper.populateData(cache, 10000);
            assertEquals(10000, cache.size());

            helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, sSnapshot10000, sService);

            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(2));

            // archive both of the snapshots
            helper.invokeOperationWithWait(PersistenceToolsHelper.ARCHIVE_SNAPSHOT, sEmptyClusterSnapshot, sService);
            helper.invokeOperationWithWait(PersistenceToolsHelper.ARCHIVE_SNAPSHOT, sSnapshot10000, sService);

            // remove the local snapshots
            helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sEmptyClusterSnapshot, sService);
            helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sSnapshot10000, sService);

            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(0));

            // make sure we can see the archived snapshots
            Eventually.assertThat(invoking(this).getArchivedSnapshotCount(helper, sService), is(2));

            // retrieve the snapshots
            helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sEmptyClusterSnapshot, sService);
            helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sSnapshot10000, sService);

            // recover the empty snapshot
            helper.invokeOperationWithWait(PersistenceToolsHelper.RECOVER_SNAPSHOT, sEmptyClusterSnapshot, sService);
            assertEquals(0, cache.size());

            // recover the 10000 snapshot
            helper.invokeOperationWithWait(PersistenceToolsHelper.RECOVER_SNAPSHOT, sSnapshot10000, sService);
            assertEquals(10000, cache.size());

            // purge the archived snapshot 10000
            helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_ARCHIVED_SNAPSHOT, sSnapshot10000, sService);

            Eventually.assertThat(invoking(this).getArchivedSnapshotCount(helper, sService), is(1));

            // purge the other snapshot
            helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_ARCHIVED_SNAPSHOT, sEmptyClusterSnapshot, sService);

            Eventually.assertThat(invoking(this).getArchivedSnapshotCount(helper, sService), is(0));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            if (fActive)
                {
                FileHelper.deleteDirSilent(fileActive);
                }

            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            FileHelper.deleteDirSilent(fileArchive);
            }
        }


    /**
     * Test for multiple restarts of cache servers in quick succession and
     * ensure there is no data loss.
     *
     * @param sServer           the prefix name of the servers to create
     * @param sMode             the persistence mode
     * @param sPersistentCache  the name of the cache
     * @param nRestartCount     the number of restarts to issue
     * @param fWaitForBalanced  indicates if we should wait for balanced cluster
     */
    private void testMultipleRestarts(String sServer, String sMode, String sPersistentCache,int nRestartCount,
                                      boolean fWaitForBalanced)
                throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        File fileActive1  = FileHelper.createTempDir();
        File fileBackup1  = FileHelper.createTempDir();
        File fileActive2  = FileHelper.createTempDir();
        File fileBackup2  = FileHelper.createTempDir();
        File fileActive3  = FileHelper.createTempDir();
        File fileBackup3  = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", sMode);
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.threads", "5");
        props.setProperty("test.persistence.members", "3");
        props.setProperty("test.distribution.members", "3");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        Properties props1 = new Properties();
        props1.putAll(props);
        props1.setProperty("test.persistence.active.dir", fileActive1.getAbsolutePath());
        props1.setProperty("test.persistence.backup.dir", fileBackup1.getAbsolutePath());

        Properties props2 = new Properties();
        props2.putAll(props);
        props2.setProperty("test.persistence.active.dir", fileActive2.getAbsolutePath());
        props2.setProperty("test.persistence.backup.dir", fileBackup2.getAbsolutePath());

        Properties props3 = new Properties();
        props3.putAll(props);
        props3.setProperty("test.persistence.active.dir", fileActive3.getAbsolutePath());
        props3.setProperty("test.persistence.backup.dir", fileBackup3.getAbsolutePath());

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final NamedCache        cache    = getNamedCache(sPersistentCache);
        DistributedCacheService service  = (DistributedCacheService) cache.getCacheService();

        String sServer1;
        String sServer2;
        String sServer3;

        int i = 0;
        try
            {
            while (++i <= nRestartCount)
                {
                System.out.println("**** Iteration: " + i + " of " + nRestartCount);
                sServer1 = sServer + "-" + (i*3 - 2);
                sServer2 = sServer + "-" + (i*3 - 1);
                sServer3 = sServer + "-" + (i*3);

                startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props1);
                startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props2);
                startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props3);

                if (fWaitForBalanced)
                    {
                    Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
                    waitForBalanced(service);
                    }

                // always assert the size to ensure we have not lost data
                assertEquals((i - 1)*10000, cache.size());

                // populate with some data
                PersistenceTestHelper.populateData(cache, i*10000, 10000);

                // leave some time for backups to persist, they are async
                Base.sleep(10*1000);

                // abruptly shutdown
                stopCacheServer(sServer1);
                Base.sleep(1000L);
                stopCacheServer(sServer2);
                Base.sleep(1000L);
                stopCacheServer(sServer3);
                }
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileActive1);
            FileHelper.deleteDirSilent(fileBackup1);
            FileHelper.deleteDirSilent(fileActive2);
            FileHelper.deleteDirSilent(fileBackup2);
            FileHelper.deleteDirSilent(fileActive3);
            FileHelper.deleteDirSilent(fileBackup3);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    public void testCqcSnapshotRecovery(String sServer, String sPersistentCache)
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", "on-demand");
        props.setProperty("test.persistence.active.dir", "");
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("test.threads", "1");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final NamedCache        cache    = getNamedCache(sPersistentCache);
        DistributedCacheService service  = (DistributedCacheService) cache.getCacheService();
        String                  sService = service.getInfo().getServiceName();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
        waitForBalanced(service);

        PersistenceTestHelper      helper = new PersistenceTestHelper();
        final ContinuousQueryCache cqc    = new ContinuousQueryCache(cache, (Filter) o -> o instanceof String
                                                                                          || (o instanceof Integer
                                                                                              && ((Integer) o) >= 9500
                                                                                              && ((Integer) o) < 10500), true);

        try
            {
            // add a bunch of data
            cache.put("foo", "bar");
            cache.put("biz", "baz");
            HashMap mapTemp = new HashMap();
            for (int i = 0; i < 10000; i++)
                {
                mapTemp.put(i, i);
                }
            cache.putAll(mapTemp);

            helper.createSnapshot(sService, "snapshot-A");

            mapTemp = new HashMap();
            for (int i = 10000; i < 20000; i++)
                {
                mapTemp.put(i, i);
                }
            cache.putAll(mapTemp);

            cache.put("foo", "BAR");
            cache.put("biz", "BAZ");
            cqc.put(9999, "X");
            helper.createSnapshot(sService, "snapshot-B");

            cache.clear();

            assertEquals(0, cqc.size());
            assertEquals(0, cache.size());

            helper.recoverSnapshot(sService, "snapshot-A");

            // validate that the data were recovered
            assertEquals(502, cqc.size());
            assertEquals(10002, cache.size());
            assertEquals("bar", cqc.get("foo"));
            assertEquals("baz", cqc.get("biz"));
            for (int i = 0; i < 10000; i++)
                {
                assertEquals(i, cache.get(i));
                assertEquals(i < 9500 ? null : i, cqc.get(i));
                }

            helper.recoverSnapshot(sService, "snapshot-B");

            // validate that the data were recovered
            assertEquals("BAR", cqc.get("foo"));
            assertEquals("BAZ", cqc.get("biz"));
            for (int i = 0; i < 20000; i++)
                {
                assertEquals(i == 9999 ? "X" : i, cache.get(i));
                assertEquals(i < 9500 || i >= 10500
                             ? null
                             : (i == 9999 ? "X" : i), cqc.get(i));
                }
            assertEquals(1002, cqc.size());
            assertEquals(20002, cache.size());
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }
    /**
     * A helper method to call the static {@link PersistenceTestHelper#listSnapshots(String)}
     * method. This allows us to use this method in Eventually.assertThat(Deferred, Matcher)
     * tests.
     */
    public List<String> listSnapshots(PersistenceTestHelper helper, String sService)
        {
        String[] asSnapshots = helper.listSnapshots(sService);
        return Arrays.asList(asSnapshots);
        }

    /**
     * A helper method to call the static {@link PersistenceToolsHelper#listSnapshots(String)}
     * method. This allows us to use this method in Eventually.assertThat(Deferred, Matcher)
     * tests.
     */
    public List<String> listSnapshots(PersistenceToolsHelper helper, String sService)
        {
        String[] asSnapshots = helper.listSnapshots(sService);
        return Arrays.asList(asSnapshots);
        }

    /**
     * Return the count of archived snapshots
     *
     * @param helper        helper to query
     * @param sServiceName  service name to query
     *
     * @return the number of snapshots
     */
    // TODO Revisit when new Oracle tools is available
    public int getArchivedSnapshotCount(PersistenceToolsHelper helper, String sServiceName)
        {
        String[] asSnapshots = helper.listArchivedSnapshots(sServiceName);
        return asSnapshots == null ||  asSnapshots.length == 0 ? 0: asSnapshots.length;
        }

    /**
     * Validate that the specified index extractors and triggers are registered
     * in the specified cache.
     *
     * @param cache         the cache
     * @param aoExtractors  the array of index extractors to verify
     * @param aoTriggers    the array of triggers to verify
     */
    private static void validateIndexTrigger(NamedCache cache, Object[] aoExtractors, Object[] aoTriggers)
        {
        PartitionedService service = (PartitionedService) cache.getCacheService();
        int                cParts  = service.getPartitionCount();

        for (int iPart = 0; iPart < cParts; iPart++)
            {
            Object[] ao = (Object[]) cache.invoke(
                    SimplePartitionKey.getPartitionKey(iPart), new IndexTriggerCheckProcessor());
            Collection colExtractors = (Collection) ao[0];
            Collection colTriggers   = (Collection) ao[1];

            assertEquals(aoExtractors.length, colExtractors.size());
            assertEquals(aoTriggers.length, colTriggers.size());

            for (int i = 0, c = aoExtractors.length; i < c; i++)
                {
                assertTrue(colExtractors.contains(aoExtractors[i]));
                }

            for (int i = 0, c = aoTriggers.length; i < c; i++)
                {
                assertTrue(colTriggers.contains(aoTriggers[i]));
                }
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
     * Register a listener on the Persistence coordinator MBean.
     *
     * @param sServiceName  service name
     * @param listener      {@link NotificationListener} to register
     */
    public void registerNotificationListener(String sServiceName, NotificationListener listener)
    {
        // there is a chance that while the cache server starts, the MBean
        // registration can be a bit delayed; implementing "Eventually" approach
        for (int iTry = 0; true; iTry++)
            {
            try
                {
                MBeanServer serverMB = MBeanHelper.findMBeanServer();

                String sMBean = "Coherence:" + CachePersistenceHelper.getMBeanName(sServiceName);

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

                try
                    {
                    Blocking.sleep(100);
                    }
                catch (InterruptedException e1)
                    {
                    // ignore
                    }
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    // ----- inner class: LastMessageNotificationListener -------------------
    /**
     * Class to store the last notification message received.
     */
    public static class LastMessageNotificationListener implements NotificationListener
        {
        // ----- constructors -----------------------------------------------

        public LastMessageNotificationListener()
            {
            }

        @Override
        public void handleNotification(Notification notification, Object handback)
            {
            m_sLastMessage = notification.getMessage();
            System.out.println("*** Received Message: " + m_sLastMessage);
            }

        // ----- accessors --------------------------------------------------

        public String getLastMessage()
            {
            return m_sLastMessage;
            }

        // ----- data members -----------------------------------------------

        private String m_sLastMessage;
        }

    // ----- inner class: IndexTriggerCheckProcessor ------------------------

    public static class IndexTriggerCheckProcessor
            extends AbstractProcessor
            implements ExternalizableLite
        {
        /**
         * {@inheritDoc}
         */
        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapContext        bmc      = binEntry.getBackingMapContext();
            BackingMapManagerContext bmmc     = bmc.getManagerContext();
            PartitionedService       service  = (PartitionedService) bmmc.getCacheService();

            try
                {
                Object oStorage   = ClassHelper.invoke(service, "getStorage", new Object[] { bmc.getCacheName() });
                Map    mapIndices = (Map) ClassHelper.invoke(oStorage, "getIndexExtractorMap", new Object[0]);
                Set setExtractors = mapIndices.keySet();

                // triggers are recovered by being sent to the service senior in a
                // ListenerRequest; on slow platforms this can take some time.
                // poll for 10s if necessary
                long ldtStart = CacheFactory.getSafeTimeMillis();
                long ldtNow   = 0L;
                Set setTriggers = null;
                do
                    {
                    if (ldtNow > 0L)
                        {
                        Base.sleep(500L);
                        }
                    setTriggers = (Set) ClassHelper.invoke(oStorage, "getTriggerSet", new Object[0]);
                    ldtNow      = CacheFactory.getSafeTimeMillis();
                    } while (setTriggers == null && ldtNow < ldtStart + 10000L);

                return new Object[] { setExtractors, setTriggers };
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in) throws IOException
            {
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out) throws IOException
            {
            }
        }

    // ----- inner class: QuorumPolicy --------------------------------------

    /**
     * Simple quorum policy that disallows partition recovery action until
     * a configurable number of storage-enabled members have joined the
     * service.
     */
    public static class QuorumPolicy
            implements ActionPolicy
        {
        /**
         * {@inheritDoc}
         */
        public void init(Service service)
            {
            }

        /**
         * {@inheritDoc}
         */
        public boolean isAllowed(Service service, Action action)
            {
            if (action instanceof PartitionRecoveryAction)
                {
                int nMembers    = ((PartitionedService) service).getOwnershipEnabledMembers().size();
                int nCheckValue = Integer.getInteger("test.persistence.members", 1);
                CacheFactory.log("Checking Quorum Policy: Service = " + service.getInfo().getServiceName() + ", Action="
                        + action + ", CheckValue=" + nCheckValue + ", Members=" + nMembers, CacheFactory.LOG_INFO);
                return (nMembers >= nCheckValue);
                }
            else if (action == PartitionedService.PartitionedAction.DISTRIBUTE)
                {
                int nMembers    = ((PartitionedService) service).getOwnershipEnabledMembers().size();
                int nCheckValue = Integer.getInteger("test.distribution.members", 1);

                return (nMembers >= nCheckValue);
                }
            return true;
            }
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a PersistenceManager to validate results of tests.
     *
     * @param file  the persistence root
     *
     * @return a new PersistenceManager for the given root directory
     */
    protected abstract PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException;

    // ----- accessors ------------------------------------------------------

    /**
     * Return a name for the PersistenceManager being used by the tests.
     *
     * @return a name used in log files, etc.
     */
    public abstract String getPersistenceManagerName();

    /**
     * {@inheritDoc}
     */
    public abstract String getCacheConfigPath();

    /**
     * Return the project name.
     */
    public static String getProjectName()
        {
        return "persistence";
        }
    }
