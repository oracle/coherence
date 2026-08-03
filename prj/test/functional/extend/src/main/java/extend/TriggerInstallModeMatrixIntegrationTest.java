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

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;
import com.tangosol.internal.util.security.RemoteInstallGate;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.OperationReason;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Extend roundtrip coverage for remote MapTrigger install and removal gates.
 * This trigger-only suite reflects the round-1 Outcome B interceptor audit.
 * Generated$$LambdaTrigger exercises the allowlisted-and-mode-gated axis,
 * Synthetic$$LambdaShapedTrigger exercises the unallowlisted-and-mode-gated
 * axis, and MapTriggerInstallGateTest uses real lambdas for the unit-level
 * install-gate composition corner.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.07
 */
public class TriggerInstallModeMatrixIntegrationTest
        extends AbstractFunctionalTest
    {
    public TriggerInstallModeMatrixIntegrationTest()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }

    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        m_sClusterOld       = System.getProperty(PROP_COHERENCE_CLUSTER);
        }

    @After
    public void cleanup()
        {
        if (m_cache != null)
            {
            releaseNamedCache(m_cache);
            m_cache = null;
            }
        CacheFactory.shutdown();
        setFactory(null);
        if (m_sServerName != null)
            {
            stopCacheServer(m_sServerName);
            m_sServerName = null;
            }
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        restoreProperty(PROP_COHERENCE_CLUSTER, m_sClusterOld);
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void annotatedTriggerInstallsInProd()
        {
        startProxy("prod", null);

        assertTriggerInstalled(new AnnotatedTrigger());
        assertCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        }

    @Test
    public void annotatedTriggerInstallsInDev()
        {
        startProxy("dev", null);

        assertTriggerInstalled(new AnnotatedTrigger());
        assertCounter("dev", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        }

    @Test
    public void annotatedTriggerInstallsInLegacy()
        {
        startProxy("legacy", null);

        assertTriggerInstalled(new AnnotatedTrigger());
        assertWouldRejectCounterAbsent(AnnotatedTrigger.class);
        }

    @Test
    public void unannotatedTriggerRejectedInProd()
        {
        startProxy("prod", null);

        assertInstallRejected(new PlainTrigger(), "Remote execution denied");
        assertCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void unannotatedTriggerRejectedInDev()
        {
        startProxy("dev", null);

        assertInstallRejected(new PlainTrigger(), "Remote execution denied");
        assertCounter("dev", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void dynamicTriggerRejectedInProd()
        {
        startProxy("prod", null);

        assertInstallRejected(new Generated$$LambdaTrigger(), "map-trigger-install-denied-by-mode");
        assertCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        assertCounterAbsent("prod", "rejected", SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void dynamicTriggerInstallsInDev()
        {
        startProxy("dev", null);

        assertTriggerInstalled(new Generated$$LambdaTrigger());
        assertCounter("dev", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        assertCounterAbsent("dev", "allowed", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterAbsent("dev", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void legacyShadowsUnannotatedTrigger()
        {
        startProxy("legacy", null);

        assertTriggerInstalled(new PlainTrigger());
        assertWouldRejectCounter(PlainTrigger.class, 2L);
        }

    @Test
    public void legacyShadowsDynamicTrigger()
        {
        startProxy("legacy", null);

        assertTriggerInstalled(new Generated$$LambdaTrigger());
        assertWouldRejectCounter(Generated$$LambdaTrigger.class, 2L);
        }

    @Test
    public void unallowlistedDynamicTriggerRejectedInProd()
        {
        startProxy("prod", null);

        assertInstallRejected(new Synthetic$$LambdaShapedTrigger(), "map-trigger-install-denied-by-mode");
        assertCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        assertCounterAbsent("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void unallowlistedDynamicTriggerInstallsInDev()
        {
        startProxy("dev", null);

        assertTriggerInstalled(new Synthetic$$LambdaShapedTrigger());
        assertCounter("dev", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        assertCounterAbsent("dev", "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        assertCounterAbsent("dev", "allowed", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterAbsent("dev", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void unallowlistedDynamicTriggerShadowedInLegacy()
        {
        startProxy("legacy", null);

        assertTriggerInstalled(new Synthetic$$LambdaShapedTrigger());
        assertWouldRejectCounter(Synthetic$$LambdaShapedTrigger.class, 4L);
        assertCounterAbsent("legacy", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void annotatedTriggerRemovalAllowedInProd()
        {
        startProxy("prod", null);

        NamedCache<String, String> cache    = getCache();
        MapTriggerListener        listener = new MapTriggerListener(new AnnotatedTrigger());

        cache.clear();
        cache.addMapListener(listener);
        cache.put("key", "value");
        assertEquals("triggered", cache.get("key"));

        cache.removeMapListener(listener);
        cache.put("key", "after");

        assertEquals("after", cache.get("key"));
        assertCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        }

    @Test
    public void unannotatedTriggerRemovalRejectedBeforeCallbacksInProd()
        {
        startProxy("prod", null);

        NamedCache<String, String> cache    = getCache();
        MapTriggerListener        listener = new MapTriggerListener(new AnnotatedTrigger());

        cache.clear();
        cache.addMapListener(listener);
        try
            {
            resetRemovalSideEffects();
            assertRemoveRejected(cache, new ObservableRemovalTrigger(), "Remote execution denied");
            assertEquals(0, removalSideEffects());
            assertCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
            }
        finally
            {
            cache.removeMapListener(listener);
            }
        }

    @Test
    public void unannotatedKeyTriggerRemovalRejectedBeforeCallbacksInProd()
        {
        startProxy("prod", null);

        NamedCache<String, String> cache    = getCache();
        MapTriggerListener        listener = new MapTriggerListener(new AnnotatedTrigger());

        cache.clear();
        cache.addMapListener(listener);
        try
            {
            resetRemovalSideEffects();
            assertKeyRemoveRejected(cache, new ObservableRemovalTrigger(), "key", "Remote execution denied");
            assertEquals(0, removalSideEffects());
            assertCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
            }
        finally
            {
            cache.removeMapListener(listener);
            }
        }

    @Test
    public void legacyShadowsUnannotatedTriggerRemoval()
        {
        startProxy("legacy", null);

        NamedCache<String, String> cache    = getCache();
        MapTriggerListener        listener = new MapTriggerListener(new PlainTrigger());

        cache.clear();
        cache.addMapListener(listener);
        cache.removeMapListener(listener);

        assertWouldRejectCounter(PlainTrigger.class, 2L);
        }

    @Test
    public void cacheConfigDeclaredTriggerInstallsAndFiresInProd()
        {
        startProxy("prod", null);

        m_memberProxy.invoke(new ResetDeclaredAdvisoryRecorder());
        NamedCache<String, String> cache = getCache(CACHE_DECLARED_TRIGGER);
        cache.clear();
        cache.put("key", "value");

        assertEquals("declared", cache.get("key"));
        Eventually.assertDeferred(() -> m_memberProxy.invoke(new GetDeclaredAdvisoryCount()), is(1));
        assertNoRejectedTriggerCounters();
        }

    private void assertTriggerInstalled(MapTrigger<String, String> trigger)
        {
        NamedCache<String, String> cache = getCache();
        MapTriggerListener listener = new MapTriggerListener(trigger);

        cache.clear();
        cache.addMapListener(listener);
        try
            {
            cache.put("key", "value");
            assertEquals("triggered", cache.get("key"));
            }
        finally
            {
            cache.removeMapListener(listener);
            }
        }

    private void assertInstallRejected(MapTrigger<String, String> trigger, String sMessage)
        {
        NamedCache<String, String> cache = getCache();
        try
            {
            cache.addMapListener(new MapTriggerListener(trigger));
            fail("Expected trigger install to fail");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, sMessage));
            }
        }

    private void assertRemoveRejected(NamedCache<String, String> cache, MapTrigger<String, String> trigger,
                                      String sMessage)
        {
        try
            {
            cache.removeMapListener(new MapTriggerListener(trigger));
            fail("Expected trigger removal to fail");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, sMessage));
            }
        }

    private void assertKeyRemoveRejected(NamedCache<String, String> cache, MapTrigger<String, String> trigger,
                                         String sKey, String sMessage)
        {
        try
            {
            cache.removeMapListener(new MapTriggerListener(trigger), sKey);
            fail("Expected trigger removal to fail");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, sMessage));
            }
        }

    private void startProxy(String sMode, String sDynamicRemote)
        {
        String sCluster = SERVER_NAME + '-' + sMode + '-' + (sDynamicRemote == null ? "default" : sDynamicRemote)
                + '-' + System.nanoTime();

        CoherenceModeHelper.restore(sMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
        restoreProperty(PROP_COHERENCE_CLUSTER, sCluster);
        RemoteExecutionMode.resetForTesting();

        Properties props = new Properties();
        props.setProperty("coherence.mode", sMode);
        props.setProperty(PROP_COHERENCE_CLUSTER, sCluster);
        props.setProperty("test.extend.enabled", "true");
        if (sDynamicRemote != null)
            {
            props.setProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
            }

        m_sServerName = sCluster;
        m_memberProxy = startCacheServer(m_sServerName, "extend",
                AbstractExtendTests.FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(m_memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        m_memberProxy.invoke(new ResetTelemetry());
        }

    private NamedCache<String, String> getCache()
        {
        return getCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);
        }

    private NamedCache<String, String> getCache(String sName)
        {
        m_cache = getNamedCache(sName);
        return m_cache;
        }

    private void assertCounter(String sMode, String sResult, String sSubReason, long cExpected)
        {
        String sKey = "coh.executable.policy_check{reason=" + OperationReason.TRIGGER.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=" + sResult + ",mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        assertEquals("counter " + sKey + " in " + telemetry(),
                Long.valueOf(cExpected), telemetry().get(sKey));
        }

    private void assertCounterAbsent(String sMode, String sResult, String sSubReason)
        {
        String sKey = "coh.executable.policy_check{reason=" + OperationReason.TRIGGER.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=" + sResult + ",mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        assertTrue("counter " + sKey + " in " + telemetry(), !telemetry().containsKey(sKey));
        }

    private void assertWouldRejectCounter(Class<?> clz, long cExpected)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + OperationReason.TRIGGER.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}";
        assertEquals("counter " + sKey + " in " + telemetry(),
                Long.valueOf(cExpected), telemetry().get(sKey));
        }

    private void assertWouldRejectCounterAbsent(Class<?> clz)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + OperationReason.TRIGGER.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}";
        assertTrue("counter " + sKey + " in " + telemetry(), !telemetry().containsKey(sKey));
        }

    private void assertNoRejectedTriggerCounters()
        {
        for (String sKey : telemetry().keySet())
            {
            assertTrue("unexpected trigger rejection counter " + sKey + " in " + telemetry(),
                    !(sKey.contains("reason=" + OperationReason.TRIGGER.name()) && sKey.contains("result=rejected")));
            }
        }

    private Map<String, Long> telemetry()
        {
        return m_memberProxy.invoke(new GetTelemetrySnapshot());
        }

    private void resetRemovalSideEffects()
        {
        m_memberProxy.invoke(new ResetRemovalSideEffects());
        }

    private int removalSideEffects()
        {
        return m_memberProxy.invoke(new GetRemovalSideEffects());
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

    // ----- trigger classes -----------------------------------------------

    @Remote.Executable
    public static class AnnotatedTrigger
            implements MapTrigger<String, String>, PortableObject, Serializable
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            entry.setValue("triggered");
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        @Override
        public boolean equals(Object o)
            {
            return o != null && o.getClass() == getClass();
            }

        @Override
        public int hashCode()
            {
            return getClass().getName().hashCode();
            }
        }

    public static class PlainTrigger
            implements MapTrigger<String, String>, PortableObject, Serializable
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            entry.setValue("triggered");
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        @Override
        public boolean equals(Object o)
            {
            return o != null && o.getClass() == getClass();
            }

        @Override
        public int hashCode()
            {
            return getClass().getName().hashCode();
            }
        }

    public static class ObservableRemovalTrigger
            implements MapTrigger<String, String>, PortableObject, Serializable
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            entry.setValue("triggered");
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            SIDE_EFFECTS.incrementAndGet();
            }

        @Override
        public boolean equals(Object o)
            {
            SIDE_EFFECTS.incrementAndGet();
            return o != null && o.getClass() == getClass();
            }

        @Override
        public int hashCode()
            {
            SIDE_EFFECTS.incrementAndGet();
            return getClass().getName().hashCode();
            }

        private static final AtomicInteger SIDE_EFFECTS = new AtomicInteger();
        }

    public static class Generated$$LambdaTrigger
            implements MapTrigger<String, String>, PortableObject, Serializable
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            entry.setValue("triggered");
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        @Override
        public boolean equals(Object o)
            {
            return o != null && o.getClass() == getClass();
            }

        @Override
        public int hashCode()
            {
            return getClass().getName().hashCode();
            }
        }

    // the $$Lambda substring is intentional and pairs with RemoteInstallGate's lambda heuristic.
    /**
     * Deliberately exercises the unannotated + unallowlisted + lambda-shaped
     * FQN combination that models a remote attacker payload after the
     * install gate composes policy enforcement with the dynamic mode gate.
     */
    public static class Synthetic$$LambdaShapedTrigger
            implements MapTrigger<String, String>, PortableObject, Serializable
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            entry.setValue("triggered");
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        @Override
        public boolean equals(Object o)
            {
            return o != null && o.getClass() == getClass();
            }

        @Override
        public int hashCode()
            {
            return getClass().getName().hashCode();
            }
        }

    public static class CacheConfigDeclaredTrigger
            implements MapTrigger<String, String>, Serializable
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            entry.setValue("declared");
            }
        }

    public static MapTriggerListener createDeclaredTriggerListener()
        {
        return new MapTriggerListener(new CacheConfigDeclaredTrigger());
        }

    // ----- inner class: ResetDeclaredAdvisoryRecorder -----------------------

    public static class ResetDeclaredAdvisoryRecorder
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            DECLARED_ADVISORIES.set(0);
            RemoteInstallGate.setAdvisoryLoggerForTesting(sMessage ->
                {
                if (sMessage.contains("Declared MapTrigger class " + CacheConfigDeclaredTrigger.class.getName()))
                    {
                    DECLARED_ADVISORIES.incrementAndGet();
                    }
                });
            return null;
            }
        }

    // ----- inner class: GetDeclaredAdvisoryCount ----------------------------

    public static class GetDeclaredAdvisoryCount
            implements RemoteCallable<Integer>
        {
        @Override
        public Integer call()
            {
            return DECLARED_ADVISORIES.get();
            }
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

    // ----- inner class: ResetRemovalSideEffects --------------------------

    public static class ResetRemovalSideEffects
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            ObservableRemovalTrigger.SIDE_EFFECTS.set(0);
            return null;
            }
        }

    // ----- inner class: GetRemovalSideEffects ----------------------------

    public static class GetRemovalSideEffects
            implements RemoteCallable<Integer>
        {
        @Override
        public Integer call()
            {
            return ObservableRemovalTrigger.SIDE_EFFECTS.get();
            }
        }

    private static final String SERVER_NAME = "TriggerGateIT";

    private static final String PROP_COHERENCE_CLUSTER = "coherence.cluster";

    private static final String CACHE_DECLARED_TRIGGER = "dist-extend-declared-trigger";

    private static final AtomicInteger DECLARED_ADVISORIES = new AtomicInteger();

    private CoherenceClusterMember m_memberProxy;
    private NamedCache<String, String> m_cache;
    private String m_sServerName;
    private String m_sModeOld;
    private String m_sDynamicRemoteOld;
    private String m_sClusterOld;
    }
