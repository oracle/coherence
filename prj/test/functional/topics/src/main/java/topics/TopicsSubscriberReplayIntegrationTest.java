/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteInstallGate;
import com.tangosol.internal.util.security.TopicsPersistedPolicyDrift;

import com.tangosol.io.FileHelper;
import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.persistence.CachePersistenceHelper;

import com.tangosol.util.Filter;
import com.tangosol.util.OperationReason;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.Rule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Persistence-recovery coverage for persisted topic subscriber replay drift.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.04
 */
public class TopicsSubscriberReplayIntegrationTest
        extends AbstractFunctionalTest
    {
    public TopicsSubscriberReplayIntegrationTest()
        {
        super(CLIENT_CACHE_CONFIG);
        }

    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sPolicyDriftOld   = System.getProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT);
        m_sClusterOld       = System.getProperty(PROP_COHERENCE_CLUSTER);
        m_sLocalStorageOld  = System.getProperty(PROP_LOCAL_STORAGE);
        m_sCacheConfigOld   = System.getProperty(PROP_CACHE_CONFIG);
        m_sOverrideOld      = System.getProperty(PROP_COHERENCE_OVERRIDE);
        m_sManagementOld    = System.getProperty(PROP_COHERENCE_MANAGEMENT);
        m_sManagementRemote = System.getProperty(PROP_COHERENCE_MANAGEMENT_REMOTE);
        m_sPartitionOld     = System.getProperty(PROP_PARTITION_COUNT);
        m_sChannelOld       = System.getProperty(PROP_CHANNEL_COUNT);
        }

    @After
    public void cleanup()
        {
        CacheFactory.shutdown();
        setFactory(null);
        stopServer();
        if (m_fileBase != null)
            {
            FileHelper.deleteDirSilent(m_fileBase);
            m_fileBase = null;
            }
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT, m_sPolicyDriftOld);
        restoreProperty(PROP_COHERENCE_CLUSTER, m_sClusterOld);
        restoreProperty(PROP_LOCAL_STORAGE, m_sLocalStorageOld);
        restoreProperty(PROP_CACHE_CONFIG, m_sCacheConfigOld);
        restoreProperty(PROP_COHERENCE_OVERRIDE, m_sOverrideOld);
        restoreProperty(PROP_COHERENCE_MANAGEMENT, m_sManagementOld);
        restoreProperty(PROP_COHERENCE_MANAGEMENT_REMOTE, m_sManagementRemote);
        restoreProperty(PROP_PARTITION_COUNT, m_sPartitionOld);
        restoreProperty(PROP_CHANNEL_COUNT, m_sChannelOld);
        CoherenceModeHelper.reset();
        TopicsPersistedPolicyDrift.resetForTesting();
        }

    @Test
    public void executableFilterReplaysWithSameConfig() throws Exception
        {
        ReplayResult result = runReplay("prod", TopicsPersistedPolicyDrift.VALUE_REJECT, true, false);

        assertNoReplayDriftCounter(result.m_mapTelemetry);
        }

    @Test
    public void rejectPolicyDriftRefusesRecovery() throws Exception
        {
        ReplayResult result = runReplay("prod", TopicsPersistedPolicyDrift.VALUE_REJECT, false, true);

        assertTrue(String.valueOf(result.m_error), containsMessage(result.m_error,
                "topic-subscriber-replay-drift-rejected"));
        assertCounter(result.m_mapTelemetry, OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(result.m_mapTelemetry, OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_REPLAY_DRIFT, 1L);
        }

    @Test
    public void warnAllowPolicyDriftRecovers() throws Exception
        {
        ReplayResult result = runReplay("prod", TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW, false, false);

        assertCounter(result.m_mapTelemetry, OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(result.m_mapTelemetry, OperationReason.EVALUATE_FILTER, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_REPLAY_DRIFT, 1L);
        }

    @Test
    public void legacyPolicyDriftShadowsRecovery() throws Exception
        {
        ReplayResult result = runReplay("legacy", TopicsPersistedPolicyDrift.VALUE_REJECT, false, false);

        assertWouldRejectCounter(result.m_mapTelemetry, DriftFilter.class, OperationReason.EVALUATE_FILTER, 1L);
        assertNoReplayDriftCounter(result.m_mapTelemetry);
        }

    private ReplayResult runReplay(String sMode, String sPolicyDrift, boolean fExecutableOnReplay,
                                   boolean fExpectFailure) throws Exception
        {
        String sCluster  = "TSR-" + Integer.toUnsignedString(m_testName.getMethodName().hashCode(), 36)
                + '-' + Long.toString(System.nanoTime(), 36);
        String sTopic    = "simple-persistent-topic-replay-" + sCluster;
        String sGroup    = "group-" + sCluster;
        String sSnapshot = "snapshot-" + sCluster;

        configureClient(sCluster, sMode, sPolicyDrift);
        createDirectories();

        String sExecutableClassPath = executableConfigClassPath();
        startServer(sCluster + "-initial", sMode, sPolicyDrift, sExecutableClassPath);

        String sServiceName = m_member.invoke(new EnsureSubscriberGroup(sTopic, sGroup));
        m_member.invoke(new SnapshotOperation("create", sServiceName, sSnapshot));

        stopServer();
        CacheFactory.shutdown();
        setFactory(null);
        FileHelper.deleteDirSilent(m_fileActive);
        m_fileActive.mkdirs();

        startServer(sCluster + "-replay", sMode, sPolicyDrift,
                fExecutableOnReplay ? sExecutableClassPath : null);
        m_member.invoke(new WaitForPersistenceIdle(sServiceName));
        m_member.invoke(new ResetTelemetry());

        Throwable error = null;
        try
            {
            m_member.invoke(new SnapshotOperation("recover", sServiceName, sSnapshot));
            }
        catch (RuntimeException e)
            {
            error = e;
            if (!fExpectFailure)
                {
                throw e;
                }
            }

        Map<String, Long> mapTelemetry = awaitReplayOutcome(sMode, sPolicyDrift, fExecutableOnReplay);
        if (fExpectFailure && error == null
                && mapTelemetry.getOrDefault(key(OperationReason.EVALUATE_FILTER, sMode, "rejected",
                        SerializationTelemetry.SUB_REASON_REPLAY_DRIFT), 0L) > 0L)
            {
            error = new SecurityException("topic-subscriber-replay-drift-rejected");
            }

        if (fExpectFailure && error == null)
            {
            fail("Expected snapshot recovery to fail");
            }

        return new ReplayResult(mapTelemetry, error);
        }

    private Map<String, Long> awaitReplayOutcome(String sMode, String sPolicyDrift, boolean fExecutableOnReplay)
        {
        Eventually.assertThat(invoking(m_member).isServiceRunning("DistributedTopicPersistence"), is(true),
                within(2, TimeUnit.MINUTES));

        if (fExecutableOnReplay)
            {
            return m_member.invoke(new GetTelemetrySnapshot());
            }

        String sKey;
        if ("legacy".equals(sMode))
            {
            sKey = wouldRejectKey(DriftFilter.class, OperationReason.EVALUATE_FILTER);
            }
        else if (TopicsPersistedPolicyDrift.VALUE_REJECT.equals(sPolicyDrift))
            {
            sKey = key(OperationReason.EVALUATE_FILTER, sMode, "rejected",
                    SerializationTelemetry.SUB_REASON_REPLAY_DRIFT);
            }
        else
            {
            sKey = key(OperationReason.EVALUATE_FILTER, sMode, "allowed",
                    SerializationTelemetry.SUB_REASON_REPLAY_DRIFT);
            }

        Eventually.assertDeferred("counter " + sKey,
                () -> m_member.invoke(new GetTelemetrySnapshot()).getOrDefault(sKey, 0L),
                is(1L), within(2, TimeUnit.MINUTES));
        return m_member.invoke(new GetTelemetrySnapshot());
        }

    private void configureClient(String sCluster, String sMode, String sPolicyDrift)
        {
        CacheFactory.shutdown();
        setFactory(null);
        restoreProperty(PROP_COHERENCE_CLUSTER, sCluster);
        restoreProperty(PROP_LOCAL_STORAGE, "false");
        restoreProperty(PROP_CACHE_CONFIG, CLIENT_CACHE_CONFIG);
        restoreProperty(PROP_COHERENCE_OVERRIDE, OVERRIDE_CONFIG);
        restoreProperty(PROP_COHERENCE_MANAGEMENT, "all");
        restoreProperty(PROP_COHERENCE_MANAGEMENT_REMOTE, "true");
        restoreProperty(PROP_PARTITION_COUNT, "1");
        restoreProperty(PROP_CHANNEL_COUNT, "1");
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT, sPolicyDrift);
        CoherenceModeHelper.restore(sMode);
        TopicsPersistedPolicyDrift.resetForTesting();
        }

    private void createDirectories() throws IOException
        {
        m_fileBase     = FileHelper.createTempDir();
        m_fileActive   = new File(m_fileBase, "active");
        m_fileSnapshot = new File(m_fileBase, "snapshot");
        m_fileTrash    = new File(m_fileBase, "trash");
        m_fileActive.mkdirs();
        m_fileSnapshot.mkdirs();
        m_fileTrash.mkdirs();
        }

    private String executableConfigClassPath() throws IOException
        {
        File fileConfig = new File(m_fileBase, "executable-security-config.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(fileConfig)))
            {
            out.putNextEntry(new JarEntry("META-INF/coherence/security-config.xml"));
            out.write(executableSecurityConfig().getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            }

        String sModulePath = System.getProperty("jdk.module.path", System.getProperty("java.class.path"));
        return fileConfig.getAbsolutePath() + File.pathSeparator + sModulePath;
        }

    private static String executableSecurityConfig()
        {
        return "<?xml version=\"1.0\"?>\n"
                + "<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\" "
                + "version=\"1.0\">\n"
                + "  <allowed-classes>\n"
                + "    <class name=\"" + DriftFilter.class.getName()
                + "\" source=\"manual\" executable=\"true\"/>\n"
                + "  </allowed-classes>\n"
                + "</security-config>\n";
        }

    private void startServer(String sName, String sMode, String sPolicyDrift, String sClassPath)
        {
        Properties props = new Properties();
        props.setProperty(PROP_COHERENCE_CLUSTER, System.getProperty(PROP_COHERENCE_CLUSTER));
        props.setProperty(PROP_COHERENCE_OVERRIDE, OVERRIDE_CONFIG);
        props.setProperty(PROP_COHERENCE_MANAGEMENT, "all");
        props.setProperty(PROP_COHERENCE_MANAGEMENT_REMOTE, "true");
        props.setProperty(PROP_PARTITION_COUNT, "1");
        props.setProperty(PROP_CHANNEL_COUNT, "1");
        props.setProperty("coherence.proxy.enabled", "true");
        props.setProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        props.setProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT, sPolicyDrift);
        props.setProperty("test.persistence.active.dir", m_fileActive.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", m_fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", m_fileTrash.getAbsolutePath());

        m_sServerName = sName;
        m_member = startCacheServer(m_sServerName, "topics", SERVER_CACHE_CONFIG, props, true, sClassPath);
        Eventually.assertThat(invoking(m_member).isServiceRunning("PofProxy"), is(true));
        Eventually.assertThat(invoking(m_member).isServiceRunning("DistributedTopicPersistence"), is(true));
        }

    private void stopServer()
        {
        if (m_sServerName != null)
            {
            stopCacheServer(m_sServerName);
            m_sServerName = null;
            m_member = null;
            }
        }

    private static void assertCounter(Map<String, Long> map, OperationReason reason, String sMode, String sResult,
                                      String sSubReason, long cExpected)
        {
        String sKey = key(reason, sMode, sResult, sSubReason);
        assertEquals("counter " + sKey + " in " + map, Long.valueOf(cExpected),
                Long.valueOf(map.getOrDefault(sKey, 0L)));
        }

    private static void assertWouldRejectCounter(Map<String, Long> map, Class<?> clz, OperationReason reason,
                                                 long cExpected)
        {
        String sKey = wouldRejectKey(clz, reason);
        assertEquals("counter " + sKey + " in " + map, Long.valueOf(cExpected),
                Long.valueOf(map.getOrDefault(sKey, 0L)));
        }

    private static void assertNoReplayDriftCounter(Map<String, Long> map)
        {
        assertFalse(String.valueOf(map), map.keySet().stream()
                .anyMatch(s -> s.contains("sub_reason=" + SerializationTelemetry.SUB_REASON_REPLAY_DRIFT)));
        }

    private static String key(OperationReason reason, String sMode, String sResult, String sSubReason)
        {
        return "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.PERSISTENCE.name()
                + ",result=" + sResult
                + ",mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        }

    private static String wouldRejectKey(Class<?> clz, OperationReason reason)
        {
        return "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + reason.name()
                + ",role=" + SerializationRole.PERSISTENCE.name() + "}";
        }

    private static boolean containsMessage(Throwable t, String sMessage)
        {
        while (t != null)
            {
            if (String.valueOf(t).contains(sMessage)
                    || String.valueOf(t.getMessage()).contains(sMessage))
                {
                return true;
                }
            t = t.getCause();
            }
        return false;
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    @Rule
    public TestName m_testName = new TestName();

    public static class DriftFilter
            implements Filter<String>, Serializable
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    public static class ResetTelemetry
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            SerializationTelemetry.resetForTesting();
            RemoteInstallGate.resetReplayDedupForTesting();
            return null;
            }
        }

    public static class EnsureSubscriberGroup
            implements RemoteCallable<String>
        {
        public EnsureSubscriberGroup(String sTopic, String sGroup)
            {
            m_sTopic = sTopic;
            m_sGroup = sGroup;
            }

        @Override
        public String call()
            {
            NamedTopic<String> topic = CacheFactory.getConfigurableCacheFactory().ensureTopic(m_sTopic);
            topic.ensureSubscriberGroup(m_sGroup, new DriftFilter(), null);
            return topic.getService().getInfo().getServiceName();
            }

        private final String m_sTopic;
        private final String m_sGroup;
        }

    public static class GetTelemetrySnapshot
            implements RemoteCallable<Map<String, Long>>
        {
        @Override
        public Map<String, Long> call()
            {
            return SerializationTelemetry.snapshot();
            }
        }

    public static class SnapshotOperation
            implements RemoteCallable<Void>
        {
        public SnapshotOperation(String sOperation, String sService, String sSnapshot)
            {
            m_sOperation = sOperation;
            m_sService   = sService;
            m_sSnapshot  = sSnapshot;
            }

        @Override
        public Void call() throws Exception
            {
            PersistenceTestHelper helper = new PersistenceTestHelper();
            if ("create".equals(m_sOperation))
                {
                helper.createSnapshot(m_sService, m_sSnapshot);
                }
            else if ("recover".equals(m_sOperation))
                {
                helper.invokeOperationWithReturn("recoverSnapshot", m_sService,
                        new String[] {m_sSnapshot}, new String[] {"java.lang.String"});
                waitForPersistenceIdle(m_sService);
                }
            else
                {
                throw new IllegalArgumentException("Unknown snapshot operation " + m_sOperation);
                }
            return null;
            }

        private static void waitForPersistenceIdle(String sService) throws Exception
            {
            Registry         registry = CacheFactory.ensureCluster().getManagement();
            MBeanServerProxy proxy    = registry.getMBeanServerProxy();
            String           sBean    = registry.ensureGlobalName(CachePersistenceHelper.getMBeanName(sService));
            long             ldtStop  = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2);

            Thread.sleep(250L);
            while (System.currentTimeMillis() < ldtStop)
                {
                if (proxy.isMBeanRegistered(sBean))
                    {
                    try
                        {
                        if (Boolean.TRUE.equals(proxy.getAttribute(sBean, "Idle")))
                            {
                            return;
                            }
                        }
                    catch (RuntimeException ignored)
                        {
                        // the service can briefly unregister while snapshot recovery restarts it
                        }
                    }
                Thread.sleep(250L);
                }

            throw new IllegalStateException("Timed out waiting for persistence operation on " + sService);
            }

        private final String m_sOperation;
        private final String m_sService;
        private final String m_sSnapshot;
        }

    public static class WaitForPersistenceIdle
            implements RemoteCallable<Void>
        {
        public WaitForPersistenceIdle(String sService)
            {
            m_sService = sService;
            }

        @Override
        public Void call() throws Exception
            {
            SnapshotOperation.waitForPersistenceIdle(m_sService);
            return null;
            }

        private final String m_sService;
        }

    private static class ReplayResult
        {
        private ReplayResult(Map<String, Long> mapTelemetry, Throwable error)
            {
            m_mapTelemetry = mapTelemetry;
            m_error        = error;
            }

        private final Map<String, Long> m_mapTelemetry;
        private final Throwable         m_error;
        }

    private static final String SERVER_NAME = "TopicsSubscriberReplayIT";

    private static final String PROP_COHERENCE_CLUSTER = "coherence.cluster";

    private static final String PROP_LOCAL_STORAGE = "coherence.distributed.localstorage";

    private static final String PROP_CACHE_CONFIG = "coherence.cacheconfig";

    private static final String PROP_COHERENCE_OVERRIDE = "coherence.override";

    private static final String PROP_COHERENCE_MANAGEMENT = "coherence.management";

    private static final String PROP_COHERENCE_MANAGEMENT_REMOTE = "coherence.management.remote";

    private static final String PROP_PARTITION_COUNT = "coherence.distributed.partitioncount";

    private static final String PROP_CHANNEL_COUNT = "coherence.channel.count";

    private static final String CLIENT_CACHE_CONFIG = "simple-persistence-bdb-client-config.xml";

    private static final String SERVER_CACHE_CONFIG = "simple-persistence-bdb-cache-config.xml";

    private static final String OVERRIDE_CONFIG = "common-tangosol-coherence-override.xml";

    private CoherenceClusterMember m_member;
    private String                 m_sServerName;
    private File                   m_fileBase;
    private File                   m_fileActive;
    private File                   m_fileSnapshot;
    private File                   m_fileTrash;
    private String                 m_sModeOld;
    private String                 m_sPolicyDriftOld;
    private String                 m_sClusterOld;
    private String                 m_sLocalStorageOld;
    private String                 m_sCacheConfigOld;
    private String                 m_sOverrideOld;
    private String                 m_sManagementOld;
    private String                 m_sManagementRemote;
    private String                 m_sPartitionOld;
    private String                 m_sChannelOld;
    }
