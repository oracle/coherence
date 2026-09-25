/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.AbstractRollingRestartTest;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.scheme.BackingMapScheme;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.DistributedScheme;

import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.internal.util.PartitionedCacheComponent;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.persistence.GUIDHelper;
import com.tangosol.persistence.journal.JournalPersistenceMBeanRegistry;

import java.io.File;
import java.io.Serializable;

import java.nio.file.Files;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for journal-backed backup maps.
 *
 * @author Aleks Seovic  2026.04.26
 * @since 26.04
 */
public class JournalBackupMapTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.remote", "true");
        System.setProperty("coherence.management.refresh.expiry", "1s");
        System.setProperty("coherence.management.http", "inherit");
        System.setProperty("coherence.management.metrics.port", "0");

        setupProps();
        System.setProperty("test.unicast.port", "0");
        }

    @Test
    public void testSingleBackupWriteProducesSingleJournalStoreEntry()
            throws Exception
        {
        File             fileActiveOne  = FileHelper.createTempDir();
        File             fileActiveTwo  = FileHelper.createTempDir();
        File             fileBackupOne  = FileHelper.createTempDir();
        File             fileBackupTwo  = FileHelper.createTempDir();
        File             fileSnapshot   = FileHelper.createTempDir();
        File             fileTrash      = FileHelper.createTempDir();
        String           sClusterName   = createClusterName("single-backup-write");
        PropertySnapshot snapshot       = applyClientProperties(sClusterName);
        MemberState      stateOne       = null;
        MemberState      stateTwo       = null;

        try
            {
            stateOne = startMember("JournalBackupMap-1", sClusterName, fileActiveOne, fileBackupOne, fileSnapshot, fileTrash);
            stateTwo = startMember("JournalBackupMap-2", sClusterName, fileActiveTwo, fileBackupTwo, fileSnapshot, fileTrash);

            Map<Integer, MemberState> mapMembers = memberStateMap(stateOne, stateTwo);
            ConfigurableCacheFactory  factory    = ensureFactory();
            NamedCache<String, String> cache      = factory.ensureCache(CACHE_NAME, null);
            DistributedCacheService   service    = (DistributedCacheService) cache.getCacheService();
            PartitionedCacheComponent serviceComponent = resolveServiceComponent(service);
            String                    sKey       = "single-backup-write-key";

            waitForClusterReady(service, cache, 2);

            long           lExtent   = waitForCacheExtentId(serviceComponent, cache.getCacheName());
            OwnershipState ownership = putAndWaitForBackupStoreEntry(mapMembers, cache, service,
                    sKey, "value-one", lExtent);

            assertEquals("value-one", cache.get(sKey));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            snapshot.restore();
            FileHelper.deleteDirSilent(fileActiveOne);
            FileHelper.deleteDirSilent(fileActiveTwo);
            FileHelper.deleteDirSilent(fileBackupOne);
            FileHelper.deleteDirSilent(fileBackupTwo);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    @Test
    public void testJournalSchemeDefaultsBackupStorageToJournal()
            throws Exception
        {
        File             fileActive   = FileHelper.createTempDir();
        File             fileBackup   = FileHelper.createTempDir();
        File             fileSnapshot = FileHelper.createTempDir();
        File             fileTrash    = FileHelper.createTempDir();
        String           sClusterName = createClusterName("default-backup-storage");
        PropertySnapshot snapshot     = applyClientProperties(sClusterName);

        try
            {
            snapshot.setProperty("test.persistence.mode", "active-backup");
            snapshot.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
            snapshot.setProperty("test.persistence.backup.dir", fileBackup.getAbsolutePath());
            snapshot.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
            snapshot.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
            snapshot.setProperty("test.persistent-environment", "simple-journal-environment");

            ConfigurableCacheFactory factory = ensureFactory();
            CachingScheme scheme = ((ExtensibleConfigurableCacheFactory) factory)
                    .getCacheConfig()
                    .findSchemeByCacheName(DEFAULTED_CACHE_NAME);

            assertTrue(scheme instanceof DistributedScheme);

            DistributedScheme schemeDistributed = (DistributedScheme) scheme;
            MapBuilder        bldrPrimaryMap    = schemeDistributed.getBackingMapScheme().getInnerScheme();

            assertEquals(BackingMapScheme.JOURNAL,
                    schemeDistributed.getBackupMapConfig().resolveType(new NullParameterResolver(), bldrPrimaryMap));
            }
        finally
            {
            CacheFactory.shutdown();
            snapshot.restore();
            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileBackup);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    @Test
    public void testRollingRestartRetainsJournalBackupStore()
            throws Exception
        {
        File             fileActiveOne  = FileHelper.createTempDir();
        File             fileActiveTwo  = FileHelper.createTempDir();
        File             fileBackupOne  = FileHelper.createTempDir();
        File             fileBackupTwo  = FileHelper.createTempDir();
        File             fileSnapshot   = FileHelper.createTempDir();
        File             fileTrash      = FileHelper.createTempDir();
        String           sClusterName   = createClusterName("rolling-retain-backup");
        PropertySnapshot snapshot       = applyClientProperties(sClusterName);
        MemberState      stateOne       = null;
        MemberState      stateTwo       = null;

        try
            {
            stateOne = startMember("JournalBackupMap-Rolling-1", sClusterName, fileActiveOne, fileBackupOne, fileSnapshot, fileTrash);
            stateTwo = startMember("JournalBackupMap-Rolling-2", sClusterName, fileActiveTwo, fileBackupTwo, fileSnapshot, fileTrash);

            Map<Integer, MemberState> mapMembers = memberStateMap(stateOne, stateTwo);
            ConfigurableCacheFactory  factory    = ensureFactory();
            NamedCache<String, String> cache      = factory.ensureCache(CACHE_NAME, null);
            DistributedCacheService   service    = (DistributedCacheService) cache.getCacheService();
            PartitionedCacheComponent serviceComponent = resolveServiceComponent(service);

            waitForClusterReady(service, cache, 2);

            long           lExtent   = waitForCacheExtentId(serviceComponent, cache.getCacheName());
            OwnershipState ownership = putAndWaitForBackupStoreEntry(mapMembers, cache, service,
                    "rolling-retain-key", "rolling-value", lExtent);
            String         sKey      = ownership.getKey();
            MemberState    stateBackup = ownership.getBackup();

            assertEquals("rolling-value", cache.get(sKey));

            stopCacheServer(stateBackup.getServerName());
            Eventually.assertDeferred(() -> service.getOwnershipEnabledMembers().size(), is(1));
            AbstractRollingRestartTest.waitForNoOrphans(cache.getCacheService());
            AbstractRollingRestartTest.waitForBalanced(cache.getCacheService());

            assertEquals("rolling-value", cache.get(sKey));

            stateBackup.restart();
            waitForClusterReady(service, cache, 2);

            assertEquals("rolling-value", cache.get(sKey));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            snapshot.restore();
            FileHelper.deleteDirSilent(fileActiveOne);
            FileHelper.deleteDirSilent(fileActiveTwo);
            FileHelper.deleteDirSilent(fileBackupOne);
            FileHelper.deleteDirSilent(fileBackupTwo);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    @Test
    public void testFullClusterRestartAfterBackupPromotion()
            throws Exception
        {
        File             fileActiveOne  = FileHelper.createTempDir();
        File             fileActiveTwo  = FileHelper.createTempDir();
        File             fileBackupOne  = FileHelper.createTempDir();
        File             fileBackupTwo  = FileHelper.createTempDir();
        File             fileSnapshot   = FileHelper.createTempDir();
        File             fileTrash      = FileHelper.createTempDir();
        String           sClusterName   = createClusterName("promotion-full-restart");
        PropertySnapshot snapshot       = applyClientProperties(sClusterName);
        MemberState      stateOne       = null;
        MemberState      stateTwo       = null;

        try
            {
            stateOne = startMember("JournalBackupMap-Promotion-1", sClusterName, fileActiveOne, fileBackupOne, fileSnapshot, fileTrash);
            stateTwo = startMember("JournalBackupMap-Promotion-2", sClusterName, fileActiveTwo, fileBackupTwo, fileSnapshot, fileTrash);

            Map<Integer, MemberState> mapMembers = memberStateMap(stateOne, stateTwo);
            ConfigurableCacheFactory  factory    = ensureFactory();
            NamedCache<String, String> cache      = factory.ensureCache(CACHE_NAME, null);
            DistributedCacheService   service    = (DistributedCacheService) cache.getCacheService();
            PartitionedCacheComponent serviceComponent = resolveServiceComponent(service);

            waitForClusterReady(service, cache, 2);

            long           lExtent   = waitForCacheExtentId(serviceComponent, cache.getCacheName());
            OwnershipState ownership = putAndWaitForBackupStoreEntry(mapMembers, cache, service,
                    "promotion-full-restart-key", "promoted-value", lExtent);
            String         sKey      = ownership.getKey();
            MemberState    statePrimary = ownership.getPrimary();
            MemberState    stateBackup  = ownership.getBackup();

            assertEquals("promoted-value", cache.get(sKey));

            stopCacheServer(statePrimary.getServerName());
            Eventually.assertDeferred(() -> service.getOwnershipEnabledMembers().size(), is(1));
            AbstractRollingRestartTest.waitForNoOrphans(cache.getCacheService());
            AbstractRollingRestartTest.waitForBalanced(cache.getCacheService());

            assertEquals("promoted-value", cache.get(sKey));

            stopCacheServer(stateBackup.getServerName());
            Eventually.assertDeferred(() -> service.getOwnershipEnabledMembers().size(), is(0));

            statePrimary.restart();
            stateBackup.restart();
            waitForClusterReady(service, cache, 2);

            assertEquals("promoted-value", cache.get(sKey));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            snapshot.restore();
            FileHelper.deleteDirSilent(fileActiveOne);
            FileHelper.deleteDirSilent(fileActiveTwo);
            FileHelper.deleteDirSilent(fileBackupOne);
            FileHelper.deleteDirSilent(fileBackupTwo);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    @Test
    public void testRecoveryFallsBackFromInvalidNewestActiveStoreToRetainedBackup()
            throws Exception
        {
        File             fileActiveOne  = FileHelper.createTempDir();
        File             fileActiveTwo  = FileHelper.createTempDir();
        File             fileBackupOne  = FileHelper.createTempDir();
        File             fileBackupTwo  = FileHelper.createTempDir();
        File             fileSnapshot   = FileHelper.createTempDir();
        File             fileTrash      = FileHelper.createTempDir();
        String           sClusterName   = createClusterName("invalid-active-fallback");
        PropertySnapshot snapshot       = applyClientProperties(sClusterName);
        MemberState      stateOne       = null;
        MemberState      stateTwo       = null;

        try
            {
            stateOne = startMember("JournalBackupMap-Fallback-1", sClusterName,
                    fileActiveOne, fileBackupOne, fileSnapshot, fileTrash);
            stateTwo = startMember("JournalBackupMap-Fallback-2", sClusterName,
                    fileActiveTwo, fileBackupTwo, fileSnapshot, fileTrash);

            Map<Integer, MemberState> mapMembers = memberStateMap(stateOne, stateTwo);
            ConfigurableCacheFactory  factory    = ensureFactory();
            NamedCache<String, String> cache      = factory.ensureCache(CACHE_NAME, null);
            DistributedCacheService   service    = (DistributedCacheService) cache.getCacheService();
            PartitionedCacheComponent serviceComponent = resolveServiceComponent(service);

            waitForClusterReady(service, cache, 2);

            long           lExtent   = waitForCacheExtentId(serviceComponent, cache.getCacheName());
            OwnershipState ownership = putAndWaitForBackupStoreEntry(mapMembers, cache, service,
                    "invalid-active-fallback-key", "retained-backup-value", lExtent);
            String         sKey      = ownership.getKey();
            int            nPart     = ownership.getPartition();
            MemberState    statePrimary = ownership.getPrimary();
            MemberState    stateBackup  = ownership.getBackup();

            stopCacheServer(statePrimary.getServerName());
            Eventually.assertDeferred(() -> service.getOwnershipEnabledMembers().size(), is(1));
            AbstractRollingRestartTest.waitForNoOrphans(cache.getCacheService());
            AbstractRollingRestartTest.waitForBalanced(cache.getCacheService());
            assertEquals("retained-backup-value", cache.get(sKey));

            stopCacheServer(stateBackup.getServerName());
            Eventually.assertDeferred(() -> service.getOwnershipEnabledMembers().size(), is(0));

            File dirActive = persistenceRoot(stateBackup.getActiveDirectory(),
                    sClusterName, service.getInfo().getServiceName());
            File dirBackup = persistenceRoot(stateBackup.getBackupDirectory(),
                    sClusterName, service.getInfo().getServiceName());
            String sActiveGUID = findNewestStoreGUID(dirActive, nPart);
            String sBackupGUID = findNewestStoreGUID(dirBackup, nPart);

            assertNotNull("expected promoted active store", sActiveGUID);
            assertNotNull("expected retained backup store", sBackupGUID);

            long   lVersion = Math.max(GUIDHelper.getVersion(sActiveGUID),
                    GUIDHelper.getVersion(sBackupGUID)) + 1L;
            String sInvalidGUID = String.format("%d-%x-%x-%d", nPart, lVersion,
                    GUIDHelper.getServiceJoinTime(sBackupGUID), GUIDHelper.getMemberId(sBackupGUID));
            File   dirInvalid = new File(dirActive, sInvalidGUID);

            assertTrue(dirInvalid.mkdir());
            Files.write(new File(dirInvalid, "incomplete-promotion").toPath(), new byte[] {1});

            statePrimary.restart();
            stateBackup.restart();
            waitForClusterReady(service, cache, 2);

            assertEquals("retained-backup-value", cache.get(sKey));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            snapshot.restore();
            FileHelper.deleteDirSilent(fileActiveOne);
            FileHelper.deleteDirSilent(fileActiveTwo);
            FileHelper.deleteDirSilent(fileBackupOne);
            FileHelper.deleteDirSilent(fileBackupTwo);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    @Test
    public void testJournalBackupMapMBeanAggregatesActiveAndBackup()
            throws Exception
        {
        File             fileActiveOne  = FileHelper.createTempDir();
        File             fileActiveTwo  = FileHelper.createTempDir();
        File             fileBackupOne  = FileHelper.createTempDir();
        File             fileBackupTwo  = FileHelper.createTempDir();
        File             fileSnapshot   = FileHelper.createTempDir();
        File             fileTrash      = FileHelper.createTempDir();
        String           sClusterName   = createClusterName("mbean-active-backup");
        PropertySnapshot snapshot       = applyClientProperties(sClusterName);
        Properties       propsOne       = createMemberProperties(sClusterName, fileActiveOne, fileBackupOne, fileSnapshot, fileTrash);
        Properties       propsTwo       = createMemberProperties(sClusterName, fileActiveTwo, fileBackupTwo, fileSnapshot, fileTrash);

        try
            {
            CoherenceClusterMember memberOne = startCacheServer("JournalBackupMap-MBean-1", PROJECT_NAME, CACHE_CONFIG, propsOne);
            CoherenceClusterMember memberTwo = startCacheServer("JournalBackupMap-MBean-2", PROJECT_NAME, CACHE_CONFIG, propsTwo);

            ConfigurableCacheFactory  factory = ensureFactory();
            NamedCache<String, String> cache   = factory.ensureCache(CACHE_NAME, null);
            DistributedCacheService   service = (DistributedCacheService) cache.getCacheService();
            PartitionedCacheComponent serviceComponent = resolveServiceComponent(service);
            String                    sName   = service.getInfo().getServiceName();

            Eventually.assertThat(invoking(memberOne).isServiceRunning(sName), is(true));
            Eventually.assertThat(invoking(memberTwo).isServiceRunning(sName), is(true));
            waitForClusterReady(service, cache, 2);

            String sActiveKey = findKeyOwnedBy(service, memberOne.getLocalMemberId(), true);
            String sBackupKey = findKeyOwnedBy(service, memberOne.getLocalMemberId(), false);
            int    nActivePart = service.getKeyPartitioningStrategy().getKeyPartition(sActiveKey);
            int    nBackupPart = service.getKeyPartitioningStrategy().getKeyPartition(sBackupKey);
            long   lExtent     = waitForCacheExtentId(serviceComponent, cache.getCacheName());

            cache.put(sActiveKey, "mbean-active-backup-active-value");
            cache.put(sBackupKey, "mbean-active-backup-backup-value");

            assertEquals("mbean-active-backup-active-value", cache.get(sActiveKey));
            assertEquals("mbean-active-backup-backup-value", cache.get(sBackupKey));
            waitForStoreEntry(memberOne, sName, nActivePart, lExtent, false);
            waitForStoreEntry(memberOne, sName, nBackupPart, lExtent, true);

            Eventually.assertDeferred(() -> getLocalJournalMBeanAttributes(memberOne, sName).isActiveBackupVisible(),
                    is(true));

            JournalMBeanAttributes attributes = getLocalJournalMBeanAttributes(memberOne, sName);

            assertTrue(attributes.isRegistered());
            assertTrue(attributes.getOpenExtentCount() >= 2);
            assertNotNull(attributes.getCompactionProgress());
            assertTrue(attributes.getCompactionProgress().contains("active"));
            assertTrue(attributes.getCompactionProgress().contains("backup"));
            assertNotNull(attributes.getLastRecoverySummary());
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            snapshot.restore();
            FileHelper.deleteDirSilent(fileActiveOne);
            FileHelper.deleteDirSilent(fileActiveTwo);
            FileHelper.deleteDirSilent(fileBackupOne);
            FileHelper.deleteDirSilent(fileBackupTwo);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    private Properties createMemberProperties(String sClusterName, File fileActive, File fileBackup, File fileSnapshot, File fileTrash)
        {
        Properties props = new Properties();
        props.setProperty("coherence.cluster", sClusterName);
        props.setProperty("coherence.localhost", "127.0.0.1");
        props.setProperty("coherence.ttl", "0");
        props.setProperty("coherence.distributed.localstorage", "true");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.distributed.partitions", "257");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");
        props.setProperty("test.unicast.port", "0");
        props.setProperty("test.backupcount", "1");
        props.setProperty("test.persistence.mode", "active-backup");
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.backup.dir", fileBackup.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistent-environment", "simple-journal-environment");
        return props;
        }

    private MemberState startMember(String sName, String sClusterName, File fileActive, File fileBackup, File fileSnapshot, File fileTrash)
        {
        return new MemberState(sName, createMemberProperties(sClusterName, fileActive, fileBackup, fileSnapshot, fileTrash),
                fileActive, fileBackup)
                .restart();
        }

    private File persistenceRoot(File dirBase, String sClusterName, String sServiceName)
        {
        return new File(new File(dirBase, FileHelper.toFilename(sClusterName)),
                FileHelper.toFilename(sServiceName));
        }

    private String findNewestStoreGUID(File dirPersistence, int nPartition)
        {
        File[] aFile = dirPersistence.listFiles(file -> file.isDirectory()
                && GUIDHelper.validateGUID(file.getName())
                && GUIDHelper.getPartition(file.getName()) == nPartition);

        String sNewest = null;
        if (aFile != null)
            {
            for (File file : aFile)
                {
                String sGUID = file.getName();
                if (sNewest == null || GUIDHelper.getVersion(sGUID) > GUIDHelper.getVersion(sNewest))
                    {
                    sNewest = sGUID;
                    }
                }
            }
        return sNewest;
        }

    private Map<Integer, MemberState> memberStateMap(MemberState stateOne, MemberState stateTwo)
        {
        Map<Integer, MemberState> mapMembers = new HashMap<>();
        mapMembers.put(Integer.valueOf(stateOne.getMemberId()), stateOne);
        mapMembers.put(Integer.valueOf(stateTwo.getMemberId()), stateTwo);
        return mapMembers;
        }

    private MemberState findMemberState(Map<Integer, MemberState> mapMembers, Member member)
        {
        MemberState state = mapMembers.get(Integer.valueOf(member.getId()));
        if (state == null)
            {
            throw new IllegalStateException("Unable to resolve test member state for " + member);
            }
        return state;
        }

    private PropertySnapshot applyClientProperties(String sClusterName)
        {
        PropertySnapshot snapshot = new PropertySnapshot();
        snapshot.setProperty("coherence.cluster", sClusterName);
        snapshot.setProperty("coherence.localhost", "127.0.0.1");
        snapshot.setProperty("coherence.ttl", "0");
        snapshot.setProperty("coherence.distributed.localstorage", "false");
        snapshot.setProperty("coherence.override", "common-tangosol-coherence-override.xml");
        snapshot.setProperty("test.unicast.port", "0");
        CacheFactory.shutdown();
        return snapshot;
        }

    private ConfigurableCacheFactory ensureFactory()
        {
        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(CACHE_CONFIG, null);
        setFactory(factory);
        return factory;
        }

    private void waitForClusterReady(DistributedCacheService service, NamedCache<?, ?> cache, int cStorageMembers)
        {
        Eventually.assertDeferred(() -> service.getOwnershipEnabledMembers().size(), is(cStorageMembers));
        Eventually.assertDeferred(() -> service.getPartitionOwner(0) != null, is(true));
        AbstractRollingRestartTest.waitForNoOrphans(cache.getCacheService());
        AbstractRollingRestartTest.waitForBalanced(cache.getCacheService());
        }

    private long waitForCacheExtentId(PartitionedCacheComponent serviceComponent, String sCacheName)
        {
        long[] alExtentId = new long[1];

        Eventually.assertDeferred(() ->
            {
            long lExtentId = serviceComponent.getCacheId(sCacheName);
            if (lExtentId > 0L)
                {
                alExtentId[0] = lExtentId;
                return true;
                }

            return false;
            }, is(true));

        return alExtentId[0];
        }

    private String findKeyOwnedBy(DistributedCacheService service, int nMemberId, boolean fPrimary)
        {
        for (int i = 0; i < 10000; i++)
            {
            String sKey       = "mbean-active-backup-key-" + nMemberId + '-' + fPrimary + '-' + i;
            int    nPartition = service.getKeyPartitioningStrategy().getKeyPartition(sKey);
            Member member     = fPrimary
                    ? service.getPartitionOwner(nPartition)
                    : service.getBackupOwner(nPartition, 1);

            if (member != null && member.getId() == nMemberId)
                {
                return sKey;
                }
            }

        throw new IllegalStateException("Unable to find a " + (fPrimary ? "primary" : "backup")
                + " partition owned by member " + nMemberId);
        }

    private JournalMBeanAttributes getLocalJournalMBeanAttributes(CoherenceClusterMember member, String sServiceName)
        {
        return member.invoke(new GetLocalJournalMBeanAttributes(sServiceName));
        }

    private static PartitionedCacheComponent resolveServiceComponent(DistributedCacheService service)
        {
        if (service instanceof PartitionedCacheComponent)
            {
            return (PartitionedCacheComponent) service;
            }

        if (service instanceof SafeService)
            {
            return (PartitionedCacheComponent) ((SafeService) service).getRunningService();
            }

        throw new IllegalStateException("Unable to resolve partitioned cache component from service " + service);
        }

    private OwnershipState putAndWaitForBackupStoreEntry(Map<Integer, MemberState> mapMembers,
            NamedCache<String, String> cache, DistributedCacheService service, String sKey, String sValue,
            long lExtentId)
        {
        int nPartition = service.getKeyPartitioningStrategy().getKeyPartition(sKey);

        Eventually.assertDeferred(() -> service.getPartitionOwner(nPartition) != null
                && service.getBackupOwner(nPartition, 1) != null, is(true));

        cache.put(sKey, sValue);

        OwnershipState[] aState = new OwnershipState[1];
        Eventually.assertDeferred(() ->
            {
            OwnershipState state = resolveOwnership(mapMembers, service, sKey, nPartition, lExtentId);

            if (state == null)
                {
                return false;
                }

            int cEntries = state.getBackup().invoke(new CountStoreEntries(
                    service.getInfo().getServiceName(), nPartition, lExtentId, true));

            if (cEntries != 1)
                {
                return false;
                }

            aState[0] = state;
            return true;
            }, is(true));

        return aState[0];
        }

    private OwnershipState resolveOwnership(Map<Integer, MemberState> mapMembers, DistributedCacheService service,
            String sKey, int nPartition, long lExtentId)
        {
        Member memberPrimary = service.getPartitionOwner(nPartition);
        Member memberBackup  = service.getBackupOwner(nPartition, 1);

        if (memberPrimary == null || memberBackup == null)
            {
            return null;
            }

        MemberState statePrimary = findMemberState(mapMembers, memberPrimary);
        MemberState stateBackup  = findMemberState(mapMembers, memberBackup);

        return new OwnershipState(sKey, nPartition, lExtentId, statePrimary, stateBackup);
        }

    private void waitForStoreEntry(CoherenceClusterMember member, String sServiceName, int nPartition,
            long lExtentId, boolean fBackup)
        {
        Eventually.assertDeferred(() -> member.invoke(new CountStoreEntries(
                sServiceName, nPartition, lExtentId, fBackup)), is(1));
        }

    private static class CountStoreEntries
            implements RemoteCallable<Integer>, Serializable
        {
        private CountStoreEntries(String sServiceName, int nPartition, long lExtentId, boolean fBackup)
            {
            f_sServiceName = sServiceName;
            f_nPartition   = nPartition;
            f_lExtentId    = lExtentId;
            f_fBackup      = fBackup;
            }

        @Override
        public Integer call()
            {
            DistributedCacheService   service          = (DistributedCacheService) CacheFactory.getService(f_sServiceName);
            PartitionedCacheComponent serviceComponent = resolveServiceComponent(service);
            PersistentStore<ReadBuffer> store           = f_fBackup
                    ? serviceComponent.getBackupPersistentStore(f_nPartition)
                    : serviceComponent.getPersistentStore(f_nPartition);

            if (store != null && !store.isOpen())
                {
                store = f_fBackup
                        ? serviceComponent.ensureOpenBackupPersistentStore(f_nPartition)
                        : serviceComponent.ensureOpenPersistentStore(f_nPartition);
                }

            if (store == null || !store.isOpen() || !store.containsExtent(f_lExtentId))
                {
                return 0;
                }

            int[] cEntries = new int[1];
            store.iterate((lExtentId, bufKey, bufValue) ->
                {
                if (lExtentId == f_lExtentId)
                    {
                    cEntries[0]++;
                    }
                return true;
                });

            return cEntries[0];
            }

        private static final long serialVersionUID = 1L;

        private final String f_sServiceName;

        private final int f_nPartition;

        private final long f_lExtentId;

        private final boolean f_fBackup;
        }

    private static class GetLocalJournalMBeanAttributes
            implements RemoteCallable<JournalMBeanAttributes>, Serializable
        {
        private GetLocalJournalMBeanAttributes(String sServiceName)
            {
            f_sServiceName = sServiceName;
            }

        @Override
        public JournalMBeanAttributes call()
            {
            Registry         registry = CacheFactory.getCluster().getManagement();
            MBeanServerProxy proxy    = registry.getMBeanServerProxy().local();
            String           sMBean   = registry.ensureGlobalName(
                    JournalPersistenceMBeanRegistry.getMBeanName(f_sServiceName));

            if (!proxy.isMBeanRegistered(sMBean))
                {
                return JournalMBeanAttributes.notRegistered();
                }

            return new JournalMBeanAttributes(true,
                    ((Number) proxy.getAttribute(sMBean, "OpenExtentCount")).intValue(),
                    (String) proxy.getAttribute(sMBean, "CompactionProgress"),
                    (String) proxy.getAttribute(sMBean, "LastRecoverySummary"));
            }

        private static final long serialVersionUID = 1L;

        private final String f_sServiceName;
        }

    private static class JournalMBeanAttributes
            implements Serializable
        {
        private JournalMBeanAttributes(boolean fRegistered, int cOpenExtentCount, String sCompactionProgress,
                String sLastRecoverySummary)
            {
            m_fRegistered          = fRegistered;
            m_cOpenExtentCount     = cOpenExtentCount;
            m_sCompactionProgress  = sCompactionProgress;
            m_sLastRecoverySummary = sLastRecoverySummary;
            }

        private static JournalMBeanAttributes notRegistered()
            {
            return new JournalMBeanAttributes(false, 0, "", "");
            }

        private boolean isActiveBackupVisible()
            {
            String sCompactionProgress = getCompactionProgress();
            return isRegistered()
                    && getOpenExtentCount() >= 2
                    && sCompactionProgress != null
                    && sCompactionProgress.contains("active")
                    && sCompactionProgress.contains("backup");
            }

        private boolean isRegistered()
            {
            return m_fRegistered;
            }

        private int getOpenExtentCount()
            {
            return m_cOpenExtentCount;
            }

        private String getCompactionProgress()
            {
            return m_sCompactionProgress;
            }

        private String getLastRecoverySummary()
            {
            return m_sLastRecoverySummary;
            }

        private static final long serialVersionUID = 1L;

        private final boolean m_fRegistered;
        private final int     m_cOpenExtentCount;
        private final String  m_sCompactionProgress;
        private final String  m_sLastRecoverySummary;
        }

    private String createClusterName(String sLabel)
        {
        String sSuffix = Long.toUnsignedString(ProcessHandle.current().pid(), 36)
                + "-"
                + Long.toUnsignedString(System.nanoTime(), 36);
        int    cLabel  = MAX_CLUSTER_NAME_LENGTH - CLUSTER_NAME_PREFIX.length() - sSuffix.length() - 2;

        if (sLabel.length() > cLabel)
            {
            sLabel = sLabel.substring(0, cLabel);
            }

        return CLUSTER_NAME_PREFIX + "-" + sLabel + "-" + sSuffix;
        }

    protected static class PropertySnapshot
        {
        protected void setProperty(String sName, String sValue)
            {
            if (!m_mapOriginal.containsKey(sName))
                {
                m_mapOriginal.put(sName, System.getProperty(sName));
                }

            if (sValue == null)
                {
                System.clearProperty(sName);
                }
            else
                {
                System.setProperty(sName, sValue);
                }
            }

        protected void restore()
            {
            for (Map.Entry<String, String> entry : m_mapOriginal.entrySet())
                {
                String sValue = entry.getValue();
                if (sValue == null)
                    {
                    System.clearProperty(entry.getKey());
                    }
                else
                    {
                    System.setProperty(entry.getKey(), sValue);
                    }
                }
            }

        private final Map<String, String> m_mapOriginal = new HashMap<>();
        }

    protected class OwnershipState
        {
        protected OwnershipState(String sKey, int nPartition, long lExtentId, MemberState statePrimary,
                MemberState stateBackup)
            {
            m_sKey        = sKey;
            m_nPartition  = nPartition;
            m_lExtentId   = lExtentId;
            m_statePrimary = statePrimary;
            m_stateBackup  = stateBackup;
            }

        protected String getKey()
            {
            return m_sKey;
            }

        protected int getPartition()
            {
            return m_nPartition;
            }

        protected long getExtentId()
            {
            return m_lExtentId;
            }

        protected MemberState getPrimary()
            {
            return m_statePrimary;
            }

        protected MemberState getBackup()
            {
            return m_stateBackup;
            }

        private final String m_sKey;

        private final int m_nPartition;

        private final long m_lExtentId;

        private final MemberState m_statePrimary;

        private final MemberState m_stateBackup;
        }

    protected class MemberState
        {
        protected MemberState(String sName, Properties props, File fileActive, File fileBackup)
            {
            m_sName      = sName;
            m_props      = props;
            m_fileActive = fileActive;
            m_fileBackup = fileBackup;
            }

        protected MemberState restart()
            {
            m_member = startCacheServer(m_sName, PROJECT_NAME, CACHE_CONFIG, m_props);
            Eventually.assertDeferred(() -> m_member.getLocalMemberId() > 0, is(true));
            return this;
            }

        protected File getBackupDirectory()
            {
            return m_fileBackup;
            }

        protected File getActiveDirectory()
            {
            return m_fileActive;
            }

        protected int getMemberId()
            {
            return m_member.getLocalMemberId();
            }

        protected String getServerName()
            {
            return m_sName;
            }

        protected <T> T invoke(RemoteCallable<T> callable)
            {
            return m_member.invoke(callable);
            }

        private final String     m_sName;

        private final Properties m_props;

        private final File       m_fileActive;

        private final File       m_fileBackup;

        private CoherenceClusterMember m_member;
        }

    private static final String CACHE_NAME = "journal-backup-map";

    private static final String DEFAULTED_CACHE_NAME = "journal-backup-map-defaulted";

    private static final String CACHE_CONFIG = "journal-backup-map-cache-config.xml";

    private static final String CLUSTER_NAME_PREFIX = "jbmt";

    private static final int MAX_CLUSTER_NAME_LENGTH = 66;

    private static final String PROJECT_NAME = "persistence";
    }
