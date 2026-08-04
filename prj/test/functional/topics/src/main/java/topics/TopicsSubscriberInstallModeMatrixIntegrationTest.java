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
import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.Filter;
import com.tangosol.util.OperationReason;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import java.lang.reflect.Field;

import java.util.Map;
import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Extend roundtrip coverage for remote topic subscriber filter and extractor
 * install mode gates.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.07
 */
public class TopicsSubscriberInstallModeMatrixIntegrationTest
        extends AbstractFunctionalTest
    {
    public TopicsSubscriberInstallModeMatrixIntegrationTest()
        {
        super(CLIENT_CACHE_CONFIG);
        }

    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sSecurityModeOld  = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        m_sLambdasOld       = System.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY);
        m_sClusterOld       = System.getProperty(PROP_COHERENCE_CLUSTER);
        m_sLocalStorageOld  = System.getProperty(PROP_LOCAL_STORAGE);
        m_sCacheConfigOld   = System.getProperty(PROP_CACHE_CONFIG);
        }

    @After
    public void cleanup()
        {
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
        restoreProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, m_sLambdasOld);
        restoreProperty(PROP_COHERENCE_CLUSTER, m_sClusterOld);
        restoreProperty(PROP_LOCAL_STORAGE, m_sLocalStorageOld);
        restoreProperty(PROP_CACHE_CONFIG, m_sCacheConfigOld);
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        resetLambdasForTesting();
        }

    @Test
    public void annotatedFilterInstallsInProd()
        {
        startProxy("prod");

        assertFilterInstalled(new AnnotatedFilter());
        assertCounterAbsent(OperationReason.EVALUATE_FILTER, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void annotatedFilterInstallsInDev()
        {
        startProxy("dev");

        assertFilterInstalled(new AnnotatedFilter());
        assertCounterAbsent(OperationReason.EVALUATE_FILTER, "dev", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void annotatedFilterInstallsInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertFilterInstalled(new AnnotatedFilter());
        assertWouldRejectCounterAbsent(AnnotatedFilter.class, OperationReason.EVALUATE_FILTER);
        }

    @Test
    public void unannotatedFilterShadowedInProd()
        {
        startProxy("prod");

        assertFilterInstalled(new PlainFilter());
        assertWouldRejectCounter(PlainFilter.class, OperationReason.EVALUATE_FILTER, 1L);
        }

    @Test
    public void wrappedUnannotatedExtractorFilterShadowedInProd()
        {
        startProxy("prod");

        assertFilterInstalled(new EqualsFilter<>(new PlainExtractor(), "value"));
        assertWouldRejectCounter(PlainExtractor.class, OperationReason.EXTRACT, 1L);
        }

    @Test
    public void unannotatedFilterShadowedInDev()
        {
        startProxy("dev");

        assertFilterInstalled(new PlainFilter());
        assertWouldRejectCounter(PlainFilter.class, OperationReason.EVALUATE_FILTER, 1L);
        }

    @Test
    public void unannotatedFilterShadowedInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertFilterInstalled(new PlainFilter());
        assertWouldRejectCounter(PlainFilter.class, OperationReason.EVALUATE_FILTER, 1L);
        }

    @Test
    public void dynamicFilterShadowedInProd()
        {
        startProxy("prod", "static");

        Filter<String> filter = dynamicFilter();
        assertTrue(filter.getClass().isSynthetic());

        assertFilterInstalled(filter);
        assertWouldRejectLambdaCounter(OperationReason.EVALUATE_FILTER, 2L);
        assertCounterAbsent(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void dynamicFilterRejectedWithExplicitDenyInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "static", "deny");

        Filter<String> filter = dynamicFilter();
        assertTrue(filter.getClass().isSynthetic());

        assertFilterRejected(filter, "topic-subscriber-install-denied-by-mode");
        assertCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void dynamicFilterInstallsInDevCompatibility()
        {
        startProxy("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "static");

        Filter<String> filter = dynamicFilter();
        assertTrue(filter.getClass().isSynthetic());

        assertFilterInstalled(filter);
        assertWouldRejectLambdaCounter(OperationReason.EVALUATE_FILTER, 2L);
        assertCounterAbsent(OperationReason.EVALUATE_FILTER, "dev", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void dynamicFilterShadowedInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "static");

        Filter<String> filter = dynamicFilter();
        assertTrue(filter.getClass().isSynthetic());

        assertFilterInstalled(filter);
        assertWouldRejectLambdaCounter(OperationReason.EVALUATE_FILTER, 2L);
        }

    @Test
    public void annotatedExtractorInstallsInProd()
        {
        startProxy("prod");

        assertExtractorInstalled(new AnnotatedExtractor());
        assertCounterAbsent(OperationReason.EXTRACT, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void annotatedExtractorInstallsInDev()
        {
        startProxy("dev");

        assertExtractorInstalled(new AnnotatedExtractor());
        assertCounterAbsent(OperationReason.EXTRACT, "dev", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void annotatedExtractorInstallsInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertExtractorInstalled(new AnnotatedExtractor());
        assertWouldRejectCounterAbsent(AnnotatedExtractor.class, OperationReason.EXTRACT);
        }

    @Test
    public void unannotatedExtractorShadowedInProd()
        {
        startProxy("prod");

        assertExtractorInstalled(new PlainExtractor());
        assertWouldRejectCounter(PlainExtractor.class, OperationReason.EXTRACT, 1L);
        }

    @Test
    public void unannotatedExtractorShadowedInDev()
        {
        startProxy("dev");

        assertExtractorInstalled(new PlainExtractor());
        assertWouldRejectCounter(PlainExtractor.class, OperationReason.EXTRACT, 1L);
        }

    @Test
    public void unannotatedExtractorShadowedInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertExtractorInstalled(new PlainExtractor());
        assertWouldRejectCounter(PlainExtractor.class, OperationReason.EXTRACT, 1L);
        }

    @Test
    public void dynamicExtractorShadowedInProd()
        {
        startProxy("prod", "static");

        ValueExtractor<String, String> extractor = dynamicExtractor();
        assertTrue(extractor.getClass().isSynthetic());

        assertExtractorInstalled(extractor);
        assertWouldRejectLambdaCounter(OperationReason.EXTRACT, 2L);
        assertCounterAbsent(OperationReason.EXTRACT, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void dynamicExtractorRejectedWithExplicitDenyInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "static", "deny");

        ValueExtractor<String, String> extractor = dynamicExtractor();
        assertTrue(extractor.getClass().isSynthetic());

        assertExtractorRejected(extractor, "topic-subscriber-install-denied-by-mode");
        assertCounter(OperationReason.EXTRACT, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void dynamicExtractorInstallsInDevCompatibility()
        {
        startProxy("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "static");

        ValueExtractor<String, String> extractor = dynamicExtractor();
        assertTrue(extractor.getClass().isSynthetic());

        assertExtractorInstalled(extractor);
        assertWouldRejectLambdaCounter(OperationReason.EXTRACT, 2L);
        assertCounterAbsent(OperationReason.EXTRACT, "dev", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void dynamicExtractorShadowedInCompatibility()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "static");

        ValueExtractor<String, String> extractor = dynamicExtractor();
        assertTrue(extractor.getClass().isSynthetic());

        assertExtractorInstalled(extractor);
        assertWouldRejectLambdaCounter(OperationReason.EXTRACT, 2L);
        }

    private void assertFilterInstalled(Filter<String> filter)
        {
        NamedTopic<String> topic = getTopic();
        topic.ensureSubscriberGroup(groupName(), filter, null);
        }

    private void assertFilterRejected(Filter<String> filter, String sMessage)
        {
        try
            {
            assertFilterInstalled(filter);
            fail("Expected filter install to fail");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, sMessage));
            }
        }

    private void assertExtractorInstalled(ValueExtractor<String, String> extractor)
        {
        NamedTopic<String> topic = getTopic();
        topic.ensureSubscriberGroup(groupName(), null, extractor);
        }

    private void assertExtractorRejected(ValueExtractor<String, String> extractor, String sMessage)
        {
        try
            {
            assertExtractorInstalled(extractor);
            fail("Expected extractor install to fail");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, sMessage));
            }
        }

    @SuppressWarnings("unchecked")
    private NamedTopic<String> getTopic()
        {
        return getFactory().ensureTopic("java-topic-subscriber-install-" + m_sServerName);
        }

    private String groupName()
        {
        return "group-" + m_sServerName;
        }

    private void startProxy(String sMode)
        {
        startProxy(sMode, null);
        }

    private void startProxy(String sMode, String sLambdas)
        {
        startProxy(sMode, null, sLambdas);
        }

    private void startProxy(String sMode, String sSecurityMode, String sLambdas)
        {
        startProxy(sMode, sSecurityMode, sLambdas, null);
        }

    private void startProxy(String sMode, String sSecurityMode, String sLambdas, String sDynamicRemote)
        {
        String sCluster = SERVER_NAME + '-' + sMode + '-' + System.nanoTime();

        CacheFactory.shutdown();
        setFactory(null);
        CoherenceModeHelper.restore(sMode);
        CoherenceModeHelper.restoreSecurityMode(sSecurityMode);
        restoreProperty(PROP_COHERENCE_CLUSTER, sCluster);
        restoreProperty(PROP_LOCAL_STORAGE, "false");
        restoreProperty(PROP_CACHE_CONFIG, CLIENT_CACHE_CONFIG);
        restoreProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, sLambdas);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
        resetLambdasForTesting();
        RemoteExecutionMode.resetForTesting();

        Properties props = new Properties();
        props.setProperty("coherence.mode", sMode);
        props.setProperty(PROP_COHERENCE_CLUSTER, sCluster);
        props.setProperty("coherence.proxy.enabled", "true");
        if (sSecurityMode != null)
            {
            props.setProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
            }
        if (sLambdas != null)
            {
            props.setProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, sLambdas);
            }
        if (sDynamicRemote != null)
            {
            props.setProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
            }

        m_sServerName = sCluster;
        m_memberProxy = startCacheServer(m_sServerName, "topics", SERVER_CACHE_CONFIG, props);
        Eventually.assertThat(invoking(m_memberProxy).isServiceRunning("JavaProxy"), is(true));
        m_memberProxy.invoke(new ResetTelemetry());
        }

    private void assertCounter(OperationReason reason, String sMode, String sResult, String sSubReason, long cExpected)
        {
        String sKey = key(reason, sMode, sResult, sSubReason);
        assertEquals("counter " + sKey + " in " + telemetry(), Long.valueOf(cExpected),
                Long.valueOf(telemetry().getOrDefault(sKey, 0L)));
        }

    private void assertCounterAbsent(OperationReason reason, String sMode, String sResult, String sSubReason)
        {
        String sKey = key(reason, sMode, sResult, sSubReason);
        assertTrue("counter " + sKey + " in " + telemetry(), !telemetry().containsKey(sKey));
        }

    private void assertWouldRejectCounter(Class<?> clz, OperationReason reason, long cExpected)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + reason.name()
                + ",role=" + SerializationRole.TOPICS.name() + "}";
        assertEquals("counter " + sKey + " in " + telemetry(), Long.valueOf(cExpected), telemetry().get(sKey));
        }

    private void assertWouldRejectLambdaCounter(OperationReason reason, long cExpected)
        {
        String sPrefix = "coh.executable.policy_check{result=would_reject,class="
                + TopicsSubscriberInstallModeMatrixIntegrationTest.class.getName() + "$$Lambda";
        String sSuffix = ",reason=" + reason.name()
                + ",role=" + SerializationRole.TOPICS.name() + "}";
        long cActual = telemetry().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sPrefix) && entry.getKey().endsWith(sSuffix))
                .mapToLong(Map.Entry::getValue)
                .sum();
        assertEquals("lambda would_reject counter in " + telemetry(), cExpected, cActual);
        }

    private void assertWouldRejectCounterAbsent(Class<?> clz, OperationReason reason)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + reason.name()
                + ",role=" + SerializationRole.TOPICS.name() + "}";
        assertTrue("counter " + sKey + " in " + telemetry(), !telemetry().containsKey(sKey));
        }

    private String key(OperationReason reason, String sMode, String sResult, String sSubReason)
        {
        return "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.TOPICS.name()
                + ",result=" + sResult
                + ",mode=" + sMode
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

    private static void resetLambdasForTesting()
        {
        try
            {
            Field field = Lambdas.class.getDeclaredField("LAMBDAS_SERIALIZATION_MODE");
            field.setAccessible(true);
            field.set(null, null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new RuntimeException(e);
            }
        }

    // ----- filter and extractor fixtures ---------------------------------

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<String>, Serializable
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    public static class PlainFilter
            implements Filter<String>, Serializable
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    private static Filter<String> dynamicFilter()
        {
        return value -> value != null;
        }

    @Remote.Executable
    public static class AnnotatedExtractor
            implements ValueExtractor<String, String>, Serializable
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    public static class PlainExtractor
            implements ValueExtractor<String, String>, Serializable
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    private static ValueExtractor<String, String> dynamicExtractor()
        {
        return value -> value;
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

    private static final String SERVER_NAME = "TopicsSubscriberGateIT";

    private static final String PROP_COHERENCE_CLUSTER = "coherence.cluster";

    private static final String PROP_LOCAL_STORAGE = "coherence.distributed.localstorage";

    private static final String PROP_CACHE_CONFIG = "coherence.cacheconfig";

    private static final String CLIENT_CACHE_CONFIG = "client-cache-config.xml";

    private static final String SERVER_CACHE_CONFIG = "topic-cache-config.xml";

    private CoherenceClusterMember m_memberProxy;
    private String m_sServerName;
    private String m_sModeOld;
    private String m_sSecurityModeOld;
    private String m_sDynamicRemoteOld;
    private String m_sLambdasOld;
    private String m_sClusterOld;
    private String m_sLocalStorageOld;
    private String m_sCacheConfigOld;
    }
