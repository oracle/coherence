/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

import com.oracle.coherence.concurrent.executor.RemoteExecutor;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.config.xml.preprocessor.ConcurrentProxyPreprocessor;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;

import com.tangosol.util.Base;
import com.tangosol.util.OperationReason;

import com.tangosol.util.function.Remote;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Functional install-mode coverage for RemoteExecutor client paths.
 */
public class RemoteExecutorInstallModeMatrixIT
    {
    private static final String CLUSTER_NAME = System.getProperty("coherence.cluster",
            RemoteExecutorInstallModeMatrixIT.class.getSimpleName());

    @BeforeEach
    public void setUp()
        {
        m_sModeOld        = System.getProperty("coherence.mode");
        m_sSecurityOld    = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sDynamicOld     = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        m_sClusterOld     = System.getProperty("coherence.cluster");
        m_sExtendOld      = System.getProperty(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED);
        m_sLocalhostOld   = System.getProperty("coherence.localhost");
        m_sWkaOld         = System.getProperty("coherence.wka");
        m_sTraceOld       = System.getProperty("coherence.executor.trace.logging");

        CoherenceModeHelper.restore("prod");
        CoherenceModeHelper.restoreSecurityMode(CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        restore(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, null);
        RemoteExecutionMode.resetForTesting();
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.executor.trace.logging", "true");
        System.setProperty(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED, "true");

        m_clientMember = Coherence.client().start().join();
        Base.sleep(4000);
        storageMember().invoke(new ResetTelemetry());
        }

    @AfterEach
    public void tearDown()
        {
        Coherence.closeAll();
        CacheFactory.shutdown();
        m_clientMember = null;

        restore("coherence.mode", m_sModeOld);
        restore(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityOld);
        restore(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicOld);
        restore("coherence.cluster", m_sClusterOld);
        restore(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED, m_sExtendOld);
        restore("coherence.localhost", m_sLocalhostOld);
        restore("coherence.wka", m_sWkaOld);
        restore("coherence.executor.trace.logging", m_sTraceOld);
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void shouldAllowAnnotatedCallableThroughRemoteExecutorSubmission()
            throws Exception
        {
        RemoteExecutor def = RemoteExecutor.getDefault();
        Future<String> future = def.submit(new AnnotatedCallable("remote-ok"));

        assertThat(future.get(1, TimeUnit.MINUTES), is("remote-ok"));
        }

    @Test
    public void shouldShadowPlainCallableThroughRemoteExecutorSubmissionInCompatibility()
            throws Exception
        {
        RemoteExecutor def = RemoteExecutor.getDefault();
        Future<String> future = def.submit(new PlainCallable("remote-shadowed"));

        assertThat(future.get(1, TimeUnit.MINUTES), is("remote-shadowed"));
        assertWouldRejectCounter(PlainCallable.class);
        }

    @Test
    public void shouldShadowNestedPlainPredicateWrapperThroughRemoteExecutorSubmissionInCompatibility()
        {
        AtomicInteger cPredicateInvoked = new AtomicInteger();

        RemoteExecutor def = RemoteExecutor.getDefault();
        Task.Coordinator<String> coordinator = def.orchestrate(new AnnotatedTask("remote-shadowed"))
                .filter(Predicates.not(new PlainExecutorPredicate(cPredicateInvoked, false)))
                .limit(1)
                .submit();

        Eventually.assertDeferred(coordinator::isDone, is(true));
        assertWouldRejectCounter(PlainExecutorPredicate.class);
        }

    @Test
    public void shouldAllowExecutableNamedFilterThroughRemoteExecutorSubmission()
        {
        RemoteExecutor def = RemoteExecutor.getDefault();
        Task.Coordinator<String> coordinator = def.orchestrate(new AnnotatedTask("named-filter-ok"))
                .filter(new AnnotatedExecutorPredicate())
                .limit(1)
                .submit();

        Eventually.assertDeferred(coordinator::isDone, is(true));
        }

    private static void restore(String sName, String sValue)
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

    private static void assertWouldRejectCounter(Class<?> clz)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + OperationReason.CONCURRENT_TASK.name()
                + ",role=" + SerializationRole.CONCURRENT.name() + "}";
        Map<String, Long> mapTelemetry = storageMember().invoke(new GetTelemetrySnapshot());
        long              cActual      = mapTelemetry.getOrDefault(sKey, 0L);

        assertThat("counter " + sKey + " in " + mapTelemetry, cActual > 0L, is(true));
        }

    private static CoherenceClusterMember storageMember()
        {
        return coherenceResource.getCluster().get("storage-1");
        }

    @Remote.Executable
    public static class AnnotatedCallable
            implements Remote.Callable<String>
        {
        public AnnotatedCallable(String sValue)
            {
            m_sValue = sValue;
            }

        @Override
        public String call()
            {
            return m_sValue;
            }

        private final String m_sValue;
        }

    public static class PlainCallable
            implements Remote.Callable<String>
        {
        public PlainCallable(String sValue)
            {
            m_sValue = sValue;
            }

        @Override
        public String call()
            {
            return m_sValue;
            }

        private final String m_sValue;
        }

    @Remote.Executable
    public static class AnnotatedTask
            implements Task<String>
        {
        public AnnotatedTask()
            {
            }

        AnnotatedTask(String sValue)
            {
            m_sValue = sValue;
            }

        @Override
        public String execute(Task.Context<String> context)
            {
            return m_sValue;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sValue = in.readUTF();
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeUTF(m_sValue == null ? "" : m_sValue);
            }

        private String m_sValue;
        }

    public static class PlainExecutorPredicate
            implements Remote.Predicate<TaskExecutorService.ExecutorInfo>
        {
        public PlainExecutorPredicate()
            {
            }

        PlainExecutorPredicate(AtomicInteger cInvoked)
            {
            this(cInvoked, true);
            }

        PlainExecutorPredicate(AtomicInteger cInvoked, boolean fResult)
            {
            m_cInvoked = cInvoked;
            m_fResult  = fResult;
            }

        @Override
        public boolean test(TaskExecutorService.ExecutorInfo info)
            {
            if (m_cInvoked != null)
                {
                m_cInvoked.incrementAndGet();
                }
            return m_fResult;
            }

        private AtomicInteger m_cInvoked;
        private boolean       m_fResult = true;
        }

    @Remote.Executable
    public static class AnnotatedExecutorPredicate
            extends PlainExecutorPredicate
        {
        }

    // ----- inner class: ResetTelemetry -----------------------------------

    public static class ResetTelemetry
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            SerializationTelemetry.resetForTesting();
            return null;
            }
        }

    // ----- inner class: GetTelemetrySnapshot -----------------------------

    public static class GetTelemetrySnapshot
            implements RemoteCallable<Map<String, Long>>
        {
        @Override
        public Map<String, Long> call()
            {
            return SerializationTelemetry.snapshot();
            }
        }

    /**
     * A Bedrock JUnit5 extension that starts a Coherence cluster of a single
     * storage-enabled member with ConcurrentProxy enabled for this
     * compatibility-shadow boundary test.
     */
    @RegisterExtension
    static CoherenceClusterExtension coherenceResource =
            new CoherenceClusterExtension()
                    .using(LocalPlatform.get())
                    .with(ClassName.of(Coherence.class),
                          Logging.at(9),
                          LocalHost.only(),
                          Multicast.ttl(0),
                          IPv4Preferred.yes(),
                          ClusterName.of(CLUSTER_NAME),
                          ClusterPort.automatic(),
                          SystemProperty.of("coherence.mode", "prod"),
                          SystemProperty.of(CoherenceMode.PROP_SECURITY_MODE, CoherenceMode.SECURITY_MODE_COMPATIBILITY),
                          SystemProperty.of("coherence.executor.trace.logging", "true"),
                          SystemProperty.of("coherence.lambdas", "dynamic"),
                          SystemProperty.of(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED, "true"),
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(1,
                             DisplayName.of("storage"),
                             RoleName.of("storage"),
                             LocalStorage.enabled());

    private String    m_sModeOld;
    private String    m_sSecurityOld;
    private String    m_sDynamicOld;
    private String    m_sClusterOld;
    private String    m_sExtendOld;
    private String    m_sLocalhostOld;
    private String    m_sWkaOld;
    private String    m_sTraceOld;
    private Coherence m_clientMember;
    }
