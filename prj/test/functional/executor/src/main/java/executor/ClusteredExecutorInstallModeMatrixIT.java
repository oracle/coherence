/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.function.Predicates;
import com.oracle.coherence.concurrent.executor.options.Member;
import com.oracle.coherence.concurrent.executor.processors.LocalOnlyProcessor;
import com.oracle.coherence.concurrent.executor.util.Caches;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import com.tangosol.util.InvocableMap;

import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Functional install-mode coverage for clustered executor callback gates.
 */
public class ClusteredExecutorInstallModeMatrixIT
    {
    @Before
    public void setUp() throws Exception
        {
        m_sModeOld          = System.getProperty("coherence.mode");
        m_sSecurityModeOld  = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sExtendAddressOld = System.getProperty("coherence.concurrent.extend.address");
        m_sExtendPortOld    = System.getProperty("coherence.concurrent.extend.port");
        m_sExtendEnabledOld = System.getProperty("coherence.concurrent.extend.enabled");

        CoherenceModeHelper.restore("prod");
        CoherenceModeHelper.restoreSecurityMode(CoherenceMode.SECURITY_MODE_HARDENED);
        RemoteExecutionMode.resetForTesting();

        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.concurrent.extend.address", "127.0.0.1");
        System.setProperty("coherence.concurrent.extend.port", String.valueOf(m_nExtendPort));
        System.setProperty("coherence.concurrent.extend.enabled", "true");
        System.setProperty("coherence.executor.trace.logging", "true");

        m_coherence = Coherence.clusterMember(CoherenceConfiguration.builder().discoverSessions().build());
        m_coherence.start().get(5, TimeUnit.MINUTES);

        m_session = m_coherence.getSession(ConcurrentServicesSessionConfiguration.SESSION_NAME);
        m_coherence.addSession(SessionConfiguration.builder()
                .named(REMOTE_SESSION_NAME)
                .withConfigUri("coherence-concurrent-client-config.xml")
                .withScopeName("$SYS")
                .withMode(Coherence.Mode.Client)
                .withParameter("coherence.client", "remote-fixed")
                .withParameter("coherence.concurrent.extend.address", "127.0.0.1")
                .withParameter("coherence.concurrent.extend.port", String.valueOf(m_nExtendPort))
                .build());
        m_remoteSession = m_coherence.getSession(REMOTE_SESSION_NAME);

        m_service  = new ClusteredExecutorService(m_session);
        m_executor = Executors.newSingleThreadExecutor();
        String sExecutorId = m_service.register(m_executor).getId();

        NamedCache<String, ClusteredExecutorInfo> executors = Caches.executors(m_session);
        Eventually.assertDeferred(() -> executors.containsKey(sExecutorId), is(true));
        Eventually.assertDeferred(() -> executors.get(sExecutorId).getState(),
                                  is(TaskExecutorService.ExecutorInfo.State.RUNNING));
        }

    @After
    public void tearDown()
        {
        if (m_executor != null)
            {
            m_executor.shutdownNow();
            m_executor = null;
            }
        if (m_service != null)
            {
            m_service.shutdown();
            m_service = null;
            }
        Coherence.closeAll();
        CacheFactory.shutdown();

        if (m_sModeOld == null)
            {
            System.clearProperty("coherence.mode");
            }
        else
            {
            System.setProperty("coherence.mode", m_sModeOld);
            }
        CoherenceModeHelper.restoreSecurityMode(m_sSecurityModeOld);
        restore("coherence.concurrent.extend.address", m_sExtendAddressOld);
        restore("coherence.concurrent.extend.port", m_sExtendPortOld);
        restore("coherence.concurrent.extend.enabled", m_sExtendEnabledOld);

        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void shouldAllowAnnotatedTaskThroughClusteredExecutorSubmission()
        {
        AnnotatedTask.COUNT.set(0);

        m_service.orchestrate(new AnnotatedTask("storage-ok"))
                .limit(1)
                .submit();

        Eventually.assertDeferred(AnnotatedTask.COUNT::get, is(1));
        }

    @Test
    public void shouldRejectPlainTaskThroughClusteredExecutorSubmission()
        {
        PlainTask.COUNT.set(0);

        assertThrows(SecurityException.class, () -> m_service.submit(new PlainTask("storage-denied")));
        assertEquals(0, PlainTask.COUNT.get());
        }

    @Test
    public void shouldRejectNestedPlainPredicateWrapperThroughClusteredExecutorSubmission()
        {
        AnnotatedTask.COUNT.set(0);
        AtomicInteger cPredicateInvoked = new AtomicInteger();

        assertThrows(SecurityException.class,
                () -> m_service.orchestrate(new AnnotatedTask("storage-denied"))
                        .filter(Predicates.not(new PlainExecutorPredicate(cPredicateInvoked)))
                        .limit(1)
                        .submit());

        assertEquals(0, cPredicateInvoked.get());
        assertEquals(0, AnnotatedTask.COUNT.get());
        }

    @Test
    public void shouldAllowMemberRegistrationOptionPredicateThroughClusteredExecutorSubmission()
        {
        AnnotatedTask.COUNT.set(0);

        m_service.orchestrate(new AnnotatedTask("member-ok"))
                .filter(Predicates.has(Member.of(CacheFactory.getCluster().getLocalMember())))
                .limit(1)
                .submit();

        Eventually.assertDeferred(AnnotatedTask.COUNT::get, is(1));
        }

    @Test
    public void shouldRejectNestedPlainLocalOnlyProcessorThroughTaskCacheInvoke()
        {
        PlainTaskProcessor.COUNT.set(0);

        Throwable e = assertThrows(Throwable.class,
                () -> Caches.tasks(m_remoteSession).invoke("local-only-denied",
                        LocalOnlyProcessor.of(new PlainTaskProcessor())));
        Throwable security = findCause(e, SecurityException.class);

        assertTrue("expected SecurityException cause in " + e, security != null);
        assertTrue(security.getMessage().contains(PlainTaskProcessor.class.getName()));
        assertEquals(0, PlainTaskProcessor.COUNT.get());
        }

    private void restore(String sProperty, String sValue)
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

    private Throwable findCause(Throwable e, Class<?> clz)
        {
        while (e != null)
            {
            if (clz.isInstance(e))
                {
                return e;
                }
            e = e.getCause();
            }
        return null;
        }

    @Remote.Executable
    public static class AnnotatedTask
            extends AbstractStringTask
        {
        public AnnotatedTask()
            {
            }

        AnnotatedTask(String sValue)
            {
            super(sValue);
            }

        @Override
        public String execute(Task.Context<String> context)
            {
            COUNT.incrementAndGet();
            return m_sValue;
            }

        static final AtomicInteger COUNT = new AtomicInteger();
        }

    public static class PlainTask
            extends AbstractStringTask
        {
        public PlainTask()
            {
            }

        PlainTask(String sValue)
            {
            super(sValue);
            }

        @Override
        public String execute(Task.Context<String> context)
            {
            COUNT.incrementAndGet();
            return m_sValue;
            }

        static final AtomicInteger COUNT = new AtomicInteger();
        }

    public static class PlainExecutorPredicate
            implements Remote.Predicate<TaskExecutorService.ExecutorInfo>
        {
        public PlainExecutorPredicate()
            {
            }

        PlainExecutorPredicate(AtomicInteger cInvoked)
            {
            m_cInvoked = cInvoked;
            }

        @Override
        public boolean test(TaskExecutorService.ExecutorInfo info)
            {
            if (m_cInvoked != null)
                {
                m_cInvoked.incrementAndGet();
                }
            return true;
            }

        private AtomicInteger m_cInvoked;
        }

    public static class PlainTaskProcessor
            implements InvocableMap.EntryProcessor<String, com.oracle.coherence.concurrent.executor.ClusteredTaskManager, Void>,
                       java.io.Serializable
        {
        @Override
        public Void process(InvocableMap.Entry<String, com.oracle.coherence.concurrent.executor.ClusteredTaskManager> entry)
            {
            COUNT.incrementAndGet();
            return null;
            }

        static final AtomicInteger COUNT = new AtomicInteger();
        }

    public abstract static class AbstractStringTask
            implements Task<String>
        {
        AbstractStringTask()
            {
            }

        AbstractStringTask(String sValue)
            {
            m_sValue = sValue;
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

        protected String m_sValue;
        }

    private static final String CLUSTER_NAME = System.getProperty("coherence.cluster",
            ClusteredExecutorInstallModeMatrixIT.class.getSimpleName());

    private static final String REMOTE_SESSION_NAME = "remote-concurrent";

    private String                   m_sModeOld;
    private String                   m_sSecurityModeOld;
    private String                   m_sExtendAddressOld;
    private String                   m_sExtendPortOld;
    private String                   m_sExtendEnabledOld;
    private Coherence                m_coherence;
    private Session                  m_session;
    private Session                  m_remoteSession;
    private ClusteredExecutorService m_service;
    private ExecutorService          m_executor;
    private final int                m_nExtendPort = LocalPlatform.get().getAvailablePorts().next();
    }
