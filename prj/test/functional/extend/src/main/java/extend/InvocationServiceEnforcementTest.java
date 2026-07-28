/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.util.OperationReason;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional coverage for InvocationService executable policy enforcement.
 *
 * @author Aleks Seovic  2026.05.07
 * @since 26.04
 */
public class InvocationServiceEnforcementTest
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public InvocationServiceEnforcementTest()
        {
        super("client-cache-config-invocation.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    @After
    public void cleanup()
        {
        CacheFactory.shutdown();
        setFactory(null);
        stopCacheServer(SERVER_NAME);
        CoherenceModeHelper.restore(m_sModeOld);
        restoreProperty(PROP_INVOCATION_ENABLED, m_sInvocationEnabledOld);
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void allowsAnnotatedInvocableInDev()
        {
        startProxy("dev", null);

        assertEquals(7, query(newTestInvocable(6)).intValue());
        assertCounter("dev", "allowed", 1L);
        }

    @Test
    public void allowsAnnotatedInvocableInProd()
        {
        startProxy("prod", "true");

        assertEquals(7, query(newTestInvocable(6)).intValue());
        assertCounter("prod", "allowed", 1L);
        }

    @Test
    public void allowsAnnotatedInvocableInLegacy()
        {
        startProxy("legacy", null);

        assertEquals(7, query(newTestInvocable(6)).intValue());
        assertCounterAbsent("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=allowed,mode=legacy}");
        }

    @Test
    public void rejectsUnannotatedInvocableInDev()
        {
        startProxy("dev", null);

        assertRemoteSecurityException(new PlainInvocable(6));
        assertCounter("dev", "rejected", 1L);
        }

    @Test
    public void rejectsUnannotatedInvocableInProd()
        {
        startProxy("prod", "true");

        assertRemoteSecurityException(new PlainInvocable(6));
        assertCounter("prod", "rejected", 1L);
        }

    @Test
    public void rejectsUnannotatedPriorityTaskBeforeSchedulingCallback()
        {
        startProxy("dev", null);
        resetPriorityTaskCallbacks();

        assertRemoteRejected(new PlainPriorityInvocable(6));

        assertPriorityTaskCallbacks(0, 0);
        assertCounter("dev", "rejected", 1L);
        }

    @Test
    public void allowsAnnotatedPriorityTaskCallback()
        {
        startProxy("dev", null);
        resetPriorityTaskCallbacks();

        assertEquals(7, query(new ExecutablePriorityInvocable(6)).intValue());

        assertPriorityTaskCallbacksAtLeast(1, 1);
        assertCounter("dev", "allowed", 1L);
        }

    @Test
    public void allowsUnannotatedInvocableInLegacy()
        {
        startProxy("legacy", null);

        assertEquals(7, query(new PlainInvocable(6)).intValue());
        assertCounter("coh.executable.policy_check{result=would_reject,class="
                + PlainInvocable.class.getName() + ",reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}", 1L);
        }

    /**
     * Verifies the disabled-proxy asymmetry: operators get one specific
     * startup WARN, while peers receive only the generic unknown-receiver fault.
     */
    @Test
    public void rejectsWhenProxyDisabled()
            throws IOException
        {
        startProxy("prod", "false");

        try
            {
            query(newTestInvocable(6));
            fail("Expected disabled InvocationService proxy to reject the request");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, "unknown receiver"));
            assertFalse(String.valueOf(e), containsMessage(e, "disabled by policy"));
            }

        Eventually.assertDeferred(this::serverLogDisabledWarningCountUnchecked, is(1));
        Eventually.assertDeferred(this::serverLogPerRequestWarningCountUnchecked, is(0));
        }

    @Test
    public void allowsClusterInternalInvocableWithoutAnnotation()
        {
        startServer("prod", "false", "coherence-cache-config.xml");

        assertEquals(Integer.valueOf(7), m_memberProxy.invoke(new InternalInvocationQuery()));
        assertCounterAbsent("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=rejected,mode=prod}");
        }

    // ----- helpers --------------------------------------------------------

    private void startProxy(String sMode, String sInvocationEnabled)
        {
        startServer(sMode, sInvocationEnabled, "server-cache-config-invocation.xml");
        }

    private void startServer(String sMode, String sInvocationEnabled, String sCacheConfig)
        {
        CoherenceModeHelper.restore(sMode);
        restoreProperty(PROP_INVOCATION_ENABLED, sInvocationEnabled);
        deleteServerLog();

        Properties props = new Properties();
        props.setProperty("coherence.mode", sMode);
        props.setProperty("test.extend.enabled", "true");
        if (sInvocationEnabled != null)
            {
            props.setProperty(PROP_INVOCATION_ENABLED, sInvocationEnabled);
            }

        m_memberProxy = startCacheServer(SERVER_NAME, "extend", sCacheConfig, props);
        if ("server-cache-config-invocation.xml".equals(sCacheConfig))
            {
            Eventually.assertThat(invoking(m_memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
            }
        m_memberProxy.invoke(new ResetTelemetry());
        }

    private void deleteServerLog()
        {
        try
            {
            Files.deleteIfExists(new File(ensureOutputDir("extend"), SERVER_NAME + ".out").toPath());
            }
        catch (IOException e)
            {
            throw new AssertionError(e);
            }
        }

    private static InvocationExtendTests.TestInvocable newTestInvocable(int nValue)
        {
        InvocationExtendTests.TestInvocable task = new InvocationExtendTests.TestInvocable();
        task.setValue(nValue);
        return task;
        }

    private Integer query(Invocable task)
        {
        InvocationService service = (InvocationService) getFactory().ensureService(INVOCATION_SERVICE_NAME);
        try
            {
            Map<?, ?> mapResult = service.query(task, null);
            return (Integer) mapResult.values().iterator().next();
            }
        finally
            {
            service.shutdown();
            }
        }

    private void assertRemoteSecurityException(Invocable task)
        {
        try
            {
            query(task);
            fail("Expected unannotated Invocable to be rejected");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsSecurityException(e));
            }
        }

    private void assertRemoteRejected(Invocable task)
        {
        try
            {
            query(task);
            fail("Expected unannotated Invocable to be rejected");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsSecurityException(e) || containsConnectionException(e));
            }
        }

    private void assertCounter(String sMode, String sResult, long cExpected)
        {
        assertCounter("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=" + sResult + ",mode=" + sMode + "}", cExpected);
        }

    private void assertCounter(String sKey, long cExpected)
        {
        Map<String, Long> map = m_memberProxy.invoke(new GetTelemetrySnapshot());
        assertEquals("counter " + sKey + " in " + map, Long.valueOf(cExpected), map.get(sKey));
        }

    private void assertCounterAbsent(String sKey)
        {
        Map<String, Long> map = m_memberProxy.invoke(new GetTelemetrySnapshot());
        assertFalse("counter " + sKey + " in " + map, map.containsKey(sKey));
        }

    private void resetPriorityTaskCallbacks()
        {
        m_memberProxy.invoke(new ResetPriorityTaskCallbacks());
        }

    private void assertPriorityTaskCallbacks(int cScheduling, int cRun)
        {
        int[] anCounts = m_memberProxy.invoke(new GetPriorityTaskCallbackCounts());
        assertEquals("scheduling callbacks", cScheduling, anCounts[0]);
        assertEquals("run callbacks", cRun, anCounts[1]);
        }

    private void assertPriorityTaskCallbacksAtLeast(int cScheduling, int cRun)
        {
        int[] anCounts = m_memberProxy.invoke(new GetPriorityTaskCallbackCounts());
        assertTrue("scheduling callbacks " + anCounts[0], anCounts[0] >= cScheduling);
        assertEquals("run callbacks", cRun, anCounts[1]);
        }

    private int serverLogDisabledWarningCount()
            throws IOException
        {
        File file = new File(ensureOutputDir("extend"), SERVER_NAME + ".out");
        if (!file.exists())
            {
            return 0;
            }

        String sLog = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return countOccurrences(sLog, DISABLED_STARTUP_WARNING);
        }

    private int serverLogPerRequestWarningCount()
            throws IOException
        {
        File file = new File(ensureOutputDir("extend"), SERVER_NAME + ".out");
        if (!file.exists())
            {
            return 0;
            }

        String sLog = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return countOccurrences(sLog, DISABLED_PER_REQUEST_WARNING);
        }

    private int serverLogDisabledWarningCountUnchecked()
        {
        try
            {
            return serverLogDisabledWarningCount();
            }
        catch (IOException e)
            {
            throw new AssertionError(e);
            }
        }

    private int serverLogPerRequestWarningCountUnchecked()
        {
        try
            {
            return serverLogPerRequestWarningCount();
            }
        catch (IOException e)
            {
            throw new AssertionError(e);
            }
        }

    private static int countOccurrences(String sText, String sNeedle)
        {
        int cOccurrences = 0;
        int of = sText.indexOf(sNeedle);
        while (of >= 0)
            {
            cOccurrences++;
            of = sText.indexOf(sNeedle, of + sNeedle.length());
            }
        return cOccurrences;
        }

    private static boolean containsSecurityException(Throwable t)
        {
        while (t != null)
            {
            if (t instanceof SecurityException)
                {
                return true;
                }
            if (String.valueOf(t).contains("SecurityException")
                    || String.valueOf(t.getMessage()).contains("Remote execution denied"))
                {
                return true;
                }
            t = t.getCause();
            }
        return false;
        }

    private static boolean containsConnectionException(Throwable t)
        {
        while (t != null)
            {
            if (t instanceof ConnectionException)
                {
                return true;
                }
            t = t.getCause();
            }
        return false;
        }

    private static boolean containsMessage(Throwable t, String sText)
        {
        while (t != null)
            {
            if (String.valueOf(t).contains(sText)
                    || String.valueOf(t.getMessage()).contains(sText))
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

    // ----- inner class: PlainInvocable -----------------------------------

    /**
     * Invocable allowed to deserialize but not marked executable.
     */
    public static class PlainInvocable
            implements Invocable, PortableObject, Serializable
        {
        public PlainInvocable()
            {
            }

        public PlainInvocable(int nValue)
            {
            m_nValue = nValue;
            }

        @Override
        public void init(InvocationService service)
            {
            m_service = service;
            }

        @Override
        public void run()
            {
            if (m_service != null)
                {
                m_nValue++;
                }
            }

        @Override
        public Object getResult()
            {
            return m_nValue;
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_nValue = in.readInt(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, m_nValue);
            }

        private int m_nValue;

        private transient InvocationService m_service;
        }

    // ----- inner class: PlainPriorityInvocable ---------------------------

    /**
     * PriorityTask invocable allowed to deserialize but not execute.
     */
    public static class PlainPriorityInvocable
            extends PlainInvocable
            implements PriorityTask
        {
        public PlainPriorityInvocable()
            {
            }

        public PlainPriorityInvocable(int nValue)
            {
            super(nValue);
            }

        @Override
        public int getSchedulingPriority()
            {
            SCHEDULING_CALLBACKS.incrementAndGet();
            return SCHEDULE_IMMEDIATE;
            }

        @Override
        public void run()
            {
            RUN_CALLBACKS.incrementAndGet();
            super.run();
            }

        @Override
        public long getExecutionTimeoutMillis()
            {
            return TIMEOUT_NONE;
            }

        @Override
        public long getRequestTimeoutMillis()
            {
            return TIMEOUT_NONE;
            }

        @Override
        public void runCanceled(boolean fAbandoned)
            {
            }

        static void resetCallbacks()
            {
            SCHEDULING_CALLBACKS.set(0);
            RUN_CALLBACKS.set(0);
            }

        static int[] callbackCounts()
            {
            return new int[] {SCHEDULING_CALLBACKS.get(), RUN_CALLBACKS.get()};
            }

        private static final AtomicInteger SCHEDULING_CALLBACKS = new AtomicInteger();

        private static final AtomicInteger RUN_CALLBACKS = new AtomicInteger();
        }

    // ----- inner class: ExecutablePriorityInvocable ----------------------

    /**
     * PriorityTask invocable allowed to execute.
     */
    @Remote.Executable
    public static class ExecutablePriorityInvocable
            extends PlainPriorityInvocable
        {
        public ExecutablePriorityInvocable()
            {
            }

        public ExecutablePriorityInvocable(int nValue)
            {
            super(nValue);
            }
        }

    // ----- inner class: InternalInvocationQuery ---------------------------

    /**
     * Runs an unannotated invocable through the cluster-internal service.
     */
    public static class InternalInvocationQuery
            implements RemoteCallable<Integer>
        {
        @Override
        public Integer call()
            {
            InvocationService service = (InvocationService) CacheFactory.getService("InvocationService");
            Map<?, ?> mapResult = service.query(new PlainInvocable(6), null);
            return (Integer) mapResult.values().iterator().next();
            }
        }

    // ----- inner class: ResetTelemetry -----------------------------------

    /**
     * Clears serialization telemetry on a remote member.
     */
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

    // ----- inner class: ResetPriorityTaskCallbacks -----------------------

    /**
     * Clears PriorityTask callback counters on a remote member.
     */
    public static class ResetPriorityTaskCallbacks
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            PlainPriorityInvocable.resetCallbacks();
            return null;
            }
        }

    // ----- inner class: GetPriorityTaskCallbackCounts --------------------

    /**
     * Returns PriorityTask callback counters from a remote member.
     */
    public static class GetPriorityTaskCallbackCounts
            implements RemoteCallable<int[]>
        {
        @Override
        public int[] call()
            {
            return PlainPriorityInvocable.callbackCounts();
            }
        }

    // ----- inner class: GetTelemetrySnapshot ------------------------------

    /**
     * Returns serialization telemetry from a remote member.
     */
    public static class GetTelemetrySnapshot
            implements RemoteCallable<Map<String, Long>>
        {
        @Override
        public Map<String, Long> call()
            {
            return SerializationTelemetry.snapshot();
            }
        }

    // ----- constants ------------------------------------------------------

    private static final String SERVER_NAME = "InvocationServiceEnforcementTest";

    private static final String INVOCATION_SERVICE_NAME = "ExtendTcpInvocationService";

    private static final String PROP_INVOCATION_ENABLED = "coherence.invocation.enabled";

    private static final String DISABLED_STARTUP_WARNING =
            "InvocationService proxy is DISABLED in prod mode by policy. "
            + "Remote Invocable execution will be refused with a generic "
            + "service-unavailable fault. Set coherence.invocation.enabled=true "
            + "or <invocation-service-proxy><enabled>true</enabled> to expose "
            + "the proxy.";

    private static final String DISABLED_PER_REQUEST_WARNING =
            "InvocationService request refused: proxy disabled by policy";

    // ----- data members ---------------------------------------------------

    private CoherenceClusterMember m_memberProxy;

    private final String m_sModeOld = System.getProperty("coherence.mode");

    private final String m_sInvocationEnabledOld = System.getProperty(PROP_INVOCATION_ENABLED);
    }
