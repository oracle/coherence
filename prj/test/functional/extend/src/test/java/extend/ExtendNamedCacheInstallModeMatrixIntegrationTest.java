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

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.OperationReason;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.ScriptAggregator;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.ScriptValueExtractor;
import com.tangosol.util.filter.ScriptFilter;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import common.AbstractFunctionalTest;

/**
 * Extend roundtrip coverage for remote NamedCache data-plane install gates.
 *
 * @author Aleks Seovic  2026.05.12
 * @since 26.04
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ExtendNamedCacheInstallModeMatrixIntegrationTest
        extends AbstractFunctionalTest
    {
    public ExtendNamedCacheInstallModeMatrixIntegrationTest()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }

    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sSecurityModeOld  = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
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
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        restoreProperty(PROP_COHERENCE_CLUSTER, m_sClusterOld);
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void annotatedRequestsInstallInProd()
        {
        startProxy("prod", null);

        assertAllOperationsInstall(PayloadKind.ANNOTATED);
        assertCounterAbsent(OperationReason.PROCESS_ENTRY, "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        assertCounterAbsent(OperationReason.AGGREGATE, "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        assertCounterAbsent(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        assertCounterAbsent(OperationReason.EXTRACT, "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        assertCounterAbsent(OperationReason.COMPARE, "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void annotatedRequestsInstallInDev()
        {
        startProxy("dev", null);

        assertAllOperationsInstall(PayloadKind.ANNOTATED);
        assertCounterAbsent(OperationReason.PROCESS_ENTRY, "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void annotatedRequestsInstallInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertAllOperationsInstall(PayloadKind.ANNOTATED);
        assertNoRejectedCounters();
        }

    @Test
    public void plainRequestsShadowInProd()
        {
        startProxy("prod", null);

        assertAllOperationsInstall(PayloadKind.PLAIN);
        assertWouldRejectCounterPresent(PlainProcessor.class, OperationReason.PROCESS_ENTRY);
        assertWouldRejectCounterPresent(PlainAggregator.class, OperationReason.AGGREGATE);
        assertWouldRejectCounterPresent(PlainFilter.class, OperationReason.EVALUATE_FILTER);
        assertWouldRejectCounterPresent(PlainExtractor.class, OperationReason.EXTRACT);
        }

    @Test
    public void plainRequestsShadowInDev()
        {
        startProxy("dev", null);

        assertAllOperationsInstall(PayloadKind.PLAIN);
        assertWouldRejectCounterPresent(PlainProcessor.class, OperationReason.PROCESS_ENTRY);
        }

    @Test
    public void plainRequestsShadowInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertAllOperationsInstall(PayloadKind.PLAIN);
        assertWouldRejectCounterPresent(PlainProcessor.class, OperationReason.PROCESS_ENTRY);
        assertWouldRejectCounterPresent(PlainAggregator.class, OperationReason.AGGREGATE);
        assertWouldRejectCounterPresent(PlainFilter.class, OperationReason.EVALUATE_FILTER);
        assertWouldRejectCounterPresent(PlainExtractor.class, OperationReason.EXTRACT);
        }

    @Test
    public void dynamicRequestsShadowInProd()
        {
        startProxy("prod", null);

        assertAllOperationsInstall(PayloadKind.DYNAMIC);
        assertWouldRejectCounterPresent(Generated$$LambdaProcessor.class, OperationReason.PROCESS_ENTRY);
        assertWouldRejectCounterPresent(Generated$$LambdaAggregator.class, OperationReason.AGGREGATE);
        assertWouldRejectCounterPresent(Generated$$LambdaFilter.class, OperationReason.EVALUATE_FILTER);
        assertWouldRejectCounterPresent(Generated$$LambdaExtractor.class, OperationReason.EXTRACT);
        assertCounterAbsent(OperationReason.PROCESS_ENTRY, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterAbsent(OperationReason.AGGREGATE, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterAbsent(OperationReason.EVALUATE_FILTER, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterAbsent(OperationReason.EXTRACT, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void dynamicRequestsInstallInDevCompatibility()
        {
        startProxy("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertAllOperationsInstall(PayloadKind.DYNAMIC);
        assertWouldRejectCounterPresent(Generated$$LambdaProcessor.class, OperationReason.PROCESS_ENTRY);
        assertWouldRejectCounterPresent(Generated$$LambdaAggregator.class, OperationReason.AGGREGATE);
        assertWouldRejectCounterPresent(Generated$$LambdaFilter.class, OperationReason.EVALUATE_FILTER);
        assertWouldRejectCounterPresent(Generated$$LambdaExtractor.class, OperationReason.EXTRACT);
        assertCounterAbsent(OperationReason.PROCESS_ENTRY, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void dynamicRequestsShadowInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertAllOperationsInstall(PayloadKind.DYNAMIC);
        assertWouldRejectCounterPresent(Generated$$LambdaProcessor.class, OperationReason.PROCESS_ENTRY);
        assertWouldRejectCounterPresent(Generated$$LambdaAggregator.class, OperationReason.AGGREGATE);
        assertWouldRejectCounterPresent(Generated$$LambdaFilter.class, OperationReason.EVALUATE_FILTER);
        assertWouldRejectCounterPresent(Generated$$LambdaExtractor.class, OperationReason.EXTRACT);
        }

    @Test
    public void dynamicRequestsRejectInCompatibilityWithExplicitDeny()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "deny");

        assertAllOperationsRejected(PayloadKind.DYNAMIC, "denied-by-mode");
        assertCounterPresent(OperationReason.PROCESS_ENTRY, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterPresent(OperationReason.AGGREGATE, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterPresent(OperationReason.EVALUATE_FILTER, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterPresent(OperationReason.EXTRACT, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void scriptBackedCacheExecutablesRejectWhenPropertyDeny()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "deny");

        NamedCache<String, Integer> cacheQuery = getCache(Operation.QUERY);
        assertRemoteFailure(() -> cacheQuery.keySet(new ScriptFilter<>("js", "ValuePresentFilter")),
                "script-eval-denied-by-mode");
        assertCounterPresent(OperationReason.SCRIPT_EVAL, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);

        NamedCache<String, Integer> cacheIndex = getCache(Operation.INDEX);
        assertRemoteFailure(() -> cacheIndex.addIndex(new ScriptValueExtractor<>("js", "IdentityExtractor"),
                false, null),
                "script-eval-denied-by-mode");

        NamedCache<String, Integer> cacheAggregate = getCache(Operation.AGGREGATE_ALL);
        assertRemoteFailure(() -> cacheAggregate.aggregate(Arrays.asList("one-AGGREGATE_ALL", "two-AGGREGATE_ALL"),
                new ScriptAggregator<>("js", "CountAggregator", 0)), "script-eval-denied-by-mode");
        }

    @Test
    public void extractorBackedIndexComparatorShadowsNestedPlainExtractorInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        NamedCache<String, Integer> cache = getCache(Operation.INDEX);
        Comparator comparator = new KeyExtractor<>(new PlainExtractor());
        cache.addIndex(new AnnotatedExtractor(), true, comparator);
        cache.removeIndex(new AnnotatedExtractor());

        assertWouldRejectCounterPresent(PlainExtractor.class, OperationReason.EXTRACT);
        assertCounterAbsent(OperationReason.EXTRACT, "rejected", SerializationTelemetry.SUB_REASON_POLICY);
        assertCounterAbsent(OperationReason.COMPARE, "rejected", SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void scriptBackedCacheExecutablesInstallWhenOverrideAllowsDynamicRemote()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "allow");

        NamedCache<String, Integer> cache = getCache(Operation.QUERY);
        assertEquals(3, cache.keySet(new ScriptFilter<>("js", "ValuePresentFilter")).size());

        cache = getCache(Operation.INDEX);
        cache.addIndex(new ScriptValueExtractor<>("js", "IdentityExtractor"), false, null);
        cache.removeIndex(new ScriptValueExtractor<>("js", "IdentityExtractor"));

        cache = getCache(Operation.AGGREGATE_ALL);
        assertEquals(Integer.valueOf(2), cache.aggregate(Arrays.asList("one-AGGREGATE_ALL", "two-AGGREGATE_ALL"),
                new ScriptAggregator<>("js", "CountAggregator", 0)));

        assertCounterAbsent(OperationReason.SCRIPT_EVAL, "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterAbsent(OperationReason.SCRIPT_EVAL, SerializationRole.UNCLASSIFIED, "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    private void assertAllOperationsInstall(PayloadKind kind)
        {
        for (Operation operation : Operation.values())
            {
            operation.execute(getCache(operation), kind);
            }
        }

    private void assertAllOperationsRejected(PayloadKind kind, String sMessage)
        {
        for (Operation operation : Operation.values())
            {
            try
                {
                operation.execute(getCache(operation), kind);
                fail("Expected " + operation + " to reject " + kind);
                }
            catch (RuntimeException e)
                {
                assertTrue(operation + " " + e, containsMessage(e, sMessage));
                }
            }
        }

    private NamedCache<String, Integer> getCache(Operation operation)
        {
        NamedCache<String, Integer> cache = getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);
        cache.clear();
        cache.put("one-" + operation.name(), 1);
        cache.put("two-" + operation.name(), 2);
        cache.put("three-" + operation.name(), 3);
        m_cache = cache;
        return cache;
        }

    private void startProxy(String sMode, String sDynamicRemote)
        {
        startProxy(sMode, null, sDynamicRemote);
        }

    private void startProxy(String sMode, String sSecurityMode, String sDynamicRemote)
        {
        String sCluster = SERVER_NAME + '-' + sMode + '-' + System.nanoTime();

        CoherenceModeHelper.restore(sMode);
        CoherenceModeHelper.restoreSecurityMode(sSecurityMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
        restoreProperty(PROP_COHERENCE_CLUSTER, sCluster);
        RemoteExecutionMode.resetForTesting();

        m_sServerName = sCluster;
        m_sMode       = sMode;

        java.util.Properties props = new java.util.Properties();
        props.setProperty("coherence.mode", sMode);
        props.setProperty(PROP_COHERENCE_CLUSTER, sCluster);
        props.setProperty("test.extend.enabled", "true");
        if (sSecurityMode != null)
            {
            props.setProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
            }
        if (sDynamicRemote != null)
            {
            props.setProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
            }

        m_memberProxy = startCacheServer(m_sServerName, "extend",
                AbstractExtendTests.FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(m_memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        m_memberProxy.invoke(new ResetTelemetry());
        }

    private void assertCounterPresent(OperationReason reason, String sResult, String sSubReason)
        {
        assertCounterPresent(reason, SerializationRole.EXTEND_PROXY, sResult, sSubReason);
        }

    private void assertCounterPresent(OperationReason reason, SerializationRole role, String sResult,
                                      String sSubReason)
        {
        String sKey = key(reason, role, sResult, sSubReason);
        assertTrue("counter " + sKey + " in " + telemetry(),
                telemetry().getOrDefault(sKey, 0L) > 0L);
        }

    private void assertCounterAbsent(OperationReason reason, String sResult, String sSubReason)
        {
        assertCounterAbsent(reason, SerializationRole.EXTEND_PROXY, sResult, sSubReason);
        }

    private void assertCounterAbsent(OperationReason reason, SerializationRole role, String sResult,
                                     String sSubReason)
        {
        String sKey = key(reason, role, sResult, sSubReason);
        assertTrue("counter " + sKey + " in " + telemetry(), !telemetry().containsKey(sKey));
        }

    private void assertWouldRejectCounterPresent(Class<?> clz, OperationReason reason)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + reason.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}";
        assertTrue("counter " + sKey + " in " + telemetry(),
                telemetry().getOrDefault(sKey, 0L) > 0L);
        }

    private void assertNoRejectedCounters()
        {
        for (String sKey : telemetry().keySet())
            {
            assertTrue("unexpected rejected counter " + sKey + " in " + telemetry(),
                    !sKey.contains("result=rejected"));
            }
        }

    private String key(OperationReason reason, String sResult, String sSubReason)
        {
        return key(reason, SerializationRole.EXTEND_PROXY, sResult, sSubReason);
        }

    private String key(OperationReason reason, SerializationRole role, String sResult, String sSubReason)
        {
        return "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + role.name()
                + ",result=" + sResult
                + ",mode=" + m_sMode
                + ",sub_reason=" + sSubReason + "}";
        }

    private Map<String, Long> telemetry()
        {
        return m_memberProxy.invoke(new GetTelemetrySnapshot());
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

    private static void assertRemoteFailure(Runnable runnable, String sMessage)
        {
        try
            {
            runnable.run();
            fail("Expected remote failure containing " + sMessage);
            }
        catch (RuntimeException e)
            {
            assertTrue(e.toString(), containsMessage(e, sMessage));
            }
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

    private enum Operation
        {
        INVOKE
            {
            @Override
            void execute(NamedCache<String, Integer> cache, PayloadKind kind)
                {
                assertEquals(Integer.valueOf(1), cache.invoke("one-" + name(), kind.processor()));
                }
            },
        INVOKE_ALL
            {
            @Override
            void execute(NamedCache<String, Integer> cache, PayloadKind kind)
                {
                assertEquals(2, cache.invokeAll(Arrays.asList("one-" + name(), "two-" + name()),
                        kind.processor()).size());
                }
            },
        INVOKE_FILTER
            {
            @Override
            void execute(NamedCache<String, Integer> cache, PayloadKind kind)
                {
                assertEquals(3, cache.invokeAll(kind.filter(), kind.processor()).size());
                }
            },
        AGGREGATE_ALL
            {
            @Override
            void execute(NamedCache<String, Integer> cache, PayloadKind kind)
                {
                assertEquals(Integer.valueOf(2), cache.aggregate(Arrays.asList("one-" + name(), "two-" + name()),
                        kind.aggregator()));
                }
            },
        AGGREGATE_FILTER
            {
            @Override
            void execute(NamedCache<String, Integer> cache, PayloadKind kind)
                {
                assertEquals(Integer.valueOf(3), cache.aggregate(kind.filter(), kind.aggregator()));
                }
            },
        INDEX
            {
            @Override
            void execute(NamedCache<String, Integer> cache, PayloadKind kind)
                {
                cache.addIndex(kind.extractor(), true, kind.comparator());
                cache.removeIndex(kind.extractor());
                }
            },
        QUERY
            {
            @Override
            void execute(NamedCache<String, Integer> cache, PayloadKind kind)
                {
                assertEquals(3, cache.keySet(kind.filter()).size());
                }
            };

        abstract void execute(NamedCache<String, Integer> cache, PayloadKind kind);
        }

    private enum PayloadKind
        {
        ANNOTATED
            {
            @Override InvocableMap.EntryProcessor processor() { return new AnnotatedProcessor(); }
            @Override InvocableMap.EntryAggregator aggregator() { return new AnnotatedAggregator(); }
            @Override Filter filter() { return new AnnotatedFilter(); }
            @Override ValueExtractor extractor() { return new AnnotatedExtractor(); }
            @Override Comparator comparator() { return new AnnotatedComparator(); }
            },
        PLAIN
            {
            @Override InvocableMap.EntryProcessor processor() { return new PlainProcessor(); }
            @Override InvocableMap.EntryAggregator aggregator() { return new PlainAggregator(); }
            @Override Filter filter() { return new PlainFilter(); }
            @Override ValueExtractor extractor() { return new PlainExtractor(); }
            @Override Comparator comparator() { return new PlainComparator(); }
            },
        DYNAMIC
            {
            @Override InvocableMap.EntryProcessor processor() { return new Generated$$LambdaProcessor(); }
            @Override InvocableMap.EntryAggregator aggregator() { return new Generated$$LambdaAggregator(); }
            @Override Filter filter() { return new Generated$$LambdaFilter(); }
            @Override ValueExtractor extractor() { return new Generated$$LambdaExtractor(); }
            @Override Comparator comparator() { return new Generated$$LambdaComparator(); }
            };

        abstract InvocableMap.EntryProcessor processor();
        abstract InvocableMap.EntryAggregator aggregator();
        abstract Filter filter();
        abstract ValueExtractor extractor();
        abstract Comparator comparator();
        }

    // ----- executable fixtures ------------------------------------------

    @Remote.Executable
    public static class AnnotatedProcessor
            implements InvocableMap.EntryProcessor<Object, Object, Integer>, PortableObject, Serializable
        {
        @Override
        public Integer process(InvocableMap.Entry<Object, Object> entry)
            {
            return 1;
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    public static class PlainProcessor
            implements InvocableMap.EntryProcessor<Object, Object, Integer>, PortableObject, Serializable
        {
        @Override
        public Integer process(InvocableMap.Entry<Object, Object> entry)
            {
            return 1;
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    @Remote.Executable
    public static class Generated$$LambdaProcessor
            extends AnnotatedProcessor
        {
        }

    @Remote.Executable
    public static class AnnotatedAggregator
            implements InvocableMap.EntryAggregator<Object, Object, Integer>, PortableObject, Serializable
        {
        @Override
        public Integer aggregate(Set<? extends InvocableMap.Entry<? extends Object, ? extends Object>> setEntries)
            {
            return setEntries.size();
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    public static class PlainAggregator
            implements InvocableMap.EntryAggregator<Object, Object, Integer>, PortableObject, Serializable
        {
        @Override
        public Integer aggregate(Set<? extends InvocableMap.Entry<? extends Object, ? extends Object>> setEntries)
            {
            return setEntries.size();
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    @Remote.Executable
    public static class Generated$$LambdaAggregator
            extends AnnotatedAggregator
        {
        }

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<Object>, PortableObject, Serializable
        {
        @Override
        public boolean evaluate(Object value)
            {
            return true;
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    public static class PlainFilter
            implements Filter<Object>, PortableObject, Serializable
        {
        @Override
        public boolean evaluate(Object value)
            {
            return true;
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    @Remote.Executable
    public static class Generated$$LambdaFilter
            extends AnnotatedFilter
        {
        }

    @Remote.Executable
    public static class AnnotatedExtractor
            implements ValueExtractor<Object, Integer>, PortableObject, Serializable
        {
        @Override
        public Integer extract(Object target)
            {
            return 1;
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    public static class PlainExtractor
            implements ValueExtractor<Object, Integer>, PortableObject, Serializable
        {
        @Override
        public Integer extract(Object target)
            {
            return 1;
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    @Remote.Executable
    public static class Generated$$LambdaExtractor
            extends AnnotatedExtractor
        {
        }

    @Remote.Executable
    public static class AnnotatedComparator
            implements Comparator<Object>, PortableObject, Serializable
        {
        @Override
        public int compare(Object o1, Object o2)
            {
            return String.valueOf(value(o1)).compareTo(String.valueOf(value(o2)));
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    public static class PlainComparator
            implements Comparator<Object>, PortableObject, Serializable
        {
        @Override
        public int compare(Object o1, Object o2)
            {
            return String.valueOf(value(o1)).compareTo(String.valueOf(value(o2)));
            }

        @Override public void readExternal(PofReader in) throws IOException {}
        @Override public void writeExternal(PofWriter out) throws IOException {}
        }

    @Remote.Executable
    public static class Generated$$LambdaComparator
            extends AnnotatedComparator
        {
        }

    private static Object value(Object o)
        {
        return o instanceof Map.Entry ? ((Map.Entry<?, ?>) o).getValue() : o;
        }

    // ----- telemetry callables ------------------------------------------

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

    public static class GetTelemetrySnapshot
            implements RemoteCallable<Map<String, Long>>
        {
        @Override
        public Map<String, Long> call()
            {
            return SerializationTelemetry.snapshot();
            }
        }

    private static final String SERVER_NAME = "ExtendNamedCacheGateIT";

    private static final String PROP_COHERENCE_CLUSTER = "coherence.cluster";

    private CoherenceClusterMember m_memberProxy;
    private NamedCache<String, Integer> m_cache;
    private String m_sServerName;
    private String m_sMode;
    private String m_sModeOld;
    private String m_sSecurityModeOld;
    private String m_sDynamicRemoteOld;
    private String m_sClusterOld;
    }
