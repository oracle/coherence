/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;
import com.tangosol.internal.util.security.RemoteInstallGate;
import com.tangosol.internal.util.security.SecurityConfig;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for remote topic subscriber install gates.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.04
 */
public class TopicsSubscriberInstallGateTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        SerializationTelemetry.resetForTesting();
        resetSecurityConfig();
        RemoteExecutablePolicy.resetForTesting();
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        resetSecurityConfig();
        SerializationTelemetry.resetForTesting();
        RemoteExecutablePolicy.resetForTesting();
        }

    @Test
    public void allowsAnnotatedFilterInstall()
        {
        setMode("prod", null);

        RemoteInstallGate.enforceTopicSubscriberInstall(new AnnotatedFilter(), null,
                SerializationRole.TOPICS, null);

        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void rejectsUnannotatedFilterInstall_prod()
        {
        setMode("prod", null);

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceTopicSubscriberInstall(
                new PlainFilter(), null, SerializationRole.TOPICS, null));

        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void rejectsUnannotatedFilterInstall_dev()
        {
        setMode("dev", null);

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceTopicSubscriberInstall(
                new PlainFilter(), null, SerializationRole.TOPICS, null));

        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "dev", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void legacyShadowsUnannotatedFilterInstall()
        {
        setMode("legacy", null);

        RemoteInstallGate.enforceTopicSubscriberInstall(new PlainFilter(), null,
                SerializationRole.TOPICS, null);

        assertWouldRejectCounter(PlainFilter.class, OperationReason.EVALUATE_FILTER, 1L);
        }

    @Test
    public void rejectsDynamicFilterInstall_prod()
        {
        Filter<String> filter = dynamicFilter();
        assertTrue(filter.getClass().isSynthetic());
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteInstallGate.enforceTopicSubscriberInstall(filter, null, SerializationRole.TOPICS, null));

        assertEquals("topic-subscriber-install-denied-by-mode", e.getMessage());
        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void allowsDynamicFilterInstall_dev()
        {
        Filter<String> filter = dynamicFilter();
        assertTrue(filter.getClass().isSynthetic());
        setMode("dev", null);

        RemoteInstallGate.enforceTopicSubscriberInstall(filter, null, SerializationRole.TOPICS, null);

        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "dev", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounterAbsent(OperationReason.EVALUATE_FILTER, "dev", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void legacyShadowsDynamicFilterInstall()
        {
        Filter<String> filter = dynamicFilter();
        assertTrue(filter.getClass().isSynthetic());
        setMode("legacy", null);

        RemoteInstallGate.enforceTopicSubscriberInstall(filter, null, SerializationRole.TOPICS, null);

        assertWouldRejectCounter(filter.getClass(), OperationReason.EVALUATE_FILTER, 2L);
        }

    @Test
    public void allowsAnnotatedExtractorInstall()
        {
        setMode("prod", null);

        RemoteInstallGate.enforceTopicSubscriberInstall(null, new AnnotatedExtractor(),
                SerializationRole.TOPICS, null);

        assertPolicyCounter(OperationReason.EXTRACT, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void rejectsUnannotatedExtractorInstall_prod()
        {
        setMode("prod", null);

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceTopicSubscriberInstall(
                null, new PlainExtractor(), SerializationRole.TOPICS, null));

        assertPolicyCounter(OperationReason.EXTRACT, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void rejectsUnannotatedExtractorInstall_dev()
        {
        setMode("dev", null);

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceTopicSubscriberInstall(
                null, new PlainExtractor(), SerializationRole.TOPICS, null));

        assertPolicyCounter(OperationReason.EXTRACT, "dev", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void legacyShadowsUnannotatedExtractorInstall()
        {
        setMode("legacy", null);

        RemoteInstallGate.enforceTopicSubscriberInstall(null, new PlainExtractor(),
                SerializationRole.TOPICS, null);

        assertWouldRejectCounter(PlainExtractor.class, OperationReason.EXTRACT, 1L);
        }

    @Test
    public void rejectsDynamicExtractorInstall_prod()
        {
        ValueExtractor<String, String> extractor = dynamicExtractor();
        assertTrue(extractor.getClass().isSynthetic());
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteInstallGate.enforceTopicSubscriberInstall(null, extractor, SerializationRole.TOPICS, null));

        assertEquals("topic-subscriber-install-denied-by-mode", e.getMessage());
        assertPolicyCounter(OperationReason.EXTRACT, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EXTRACT, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void allowsDynamicExtractorInstall_dev()
        {
        ValueExtractor<String, String> extractor = dynamicExtractor();
        assertTrue(extractor.getClass().isSynthetic());
        setMode("dev", null);

        RemoteInstallGate.enforceTopicSubscriberInstall(null, extractor, SerializationRole.TOPICS, null);

        assertPolicyCounter(OperationReason.EXTRACT, "dev", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounterAbsent(OperationReason.EXTRACT, "dev", "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void legacyShadowsDynamicExtractorInstall()
        {
        ValueExtractor<String, String> extractor = dynamicExtractor();
        assertTrue(extractor.getClass().isSynthetic());
        setMode("legacy", null);

        RemoteInstallGate.enforceTopicSubscriberInstall(null, extractor, SerializationRole.TOPICS, null);

        assertWouldRejectCounter(extractor.getClass(), OperationReason.EXTRACT, 2L);
        }

    private static void assertPolicyCounter(OperationReason reason, String sMode, String sResult,
                                            String sSubReason, long cExpected)
        {
        String sKey = key(reason, sMode, sResult, sSubReason);
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static void assertCounterAbsent(OperationReason reason, String sMode, String sResult, String sSubReason)
        {
        String sKey = key(reason, sMode, sResult, sSubReason);
        assertFalse("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                SerializationTelemetry.snapshot().containsKey(sKey));
        }

    private static void assertWouldRejectCounter(Class<?> clz, OperationReason reason, long cExpected)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + reason.name()
                + ",role=" + SerializationRole.TOPICS.name() + "}";
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static String key(OperationReason reason, String sMode, String sResult, String sSubReason)
        {
        return "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.TOPICS.name()
                + ",result=" + sResult
                + ",mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        }

    private static void setMode(String sMode, String sDynamicRemote)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
        resetMode();
        SerializationTelemetry.resetForTesting();
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

    private static void resetMode()
        {
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    private static void resetSecurityConfig()
        {
        try
            {
            var method = SecurityConfig.class.getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError(e);
            }
        }

    private static Filter<String> dynamicFilter()
        {
        return value -> value != null;
        }

    private static ValueExtractor<String, String> dynamicExtractor()
        {
        return value -> value;
        }

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    public static class PlainFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    @Remote.Executable
    public static class AnnotatedExtractor
            implements ValueExtractor<String, String>
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    public static class PlainExtractor
            implements ValueExtractor<String, String>
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    private String m_sModeOld;
    private String m_sDynamicRemoteOld;
    }
