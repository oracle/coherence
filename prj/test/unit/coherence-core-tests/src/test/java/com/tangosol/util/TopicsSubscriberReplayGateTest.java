/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteInstallGate;
import com.tangosol.internal.util.security.SecurityConfig;
import com.tangosol.internal.util.security.TopicsPersistedPolicyDrift;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for persisted topic subscriber replay policy.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.04
 */
public class TopicsSubscriberReplayGateTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld        = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sSecurityModeOld = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sPolicyDriftOld = System.getProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT);
        m_listMessages    = new ArrayList<>();
        SerializationTelemetry.resetForTesting();
        resetSecurityConfig();
        RemoteExecutablePolicy.resetForTesting();
        RemoteInstallGate.resetAdvisoryForTesting();
        RemoteInstallGate.resetReplayDedupForTesting();
        RemoteInstallGate.setAdvisoryLoggerForTesting(m_listMessages::add);
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        restoreProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT, m_sPolicyDriftOld);
        resetMode();
        resetSecurityConfig();
        SerializationTelemetry.resetForTesting();
        RemoteExecutablePolicy.resetForTesting();
        RemoteInstallGate.resetAdvisoryForTesting();
        RemoteInstallGate.resetReplayDedupForTesting();
        }

    @Test
    public void allowsExecutableReplay()
        {
        setMode("prod", TopicsPersistedPolicyDrift.VALUE_REJECT);

        RemoteInstallGate.enforceTopicSubscriberReplay(new AnnotatedFilter(), new AnnotatedExtractor(),
                SerializationRole.PERSISTENCE, null);

        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EXTRACT, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertTrue(m_listMessages.toString(), m_listMessages.isEmpty());
        }

    @Test
    public void rejectPolicyDriftRefusesReplay()
        {
        setMode("prod", TopicsPersistedPolicyDrift.VALUE_REJECT);

        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteInstallGate.enforceTopicSubscriberReplay(new PlainFilter(), null,
                        SerializationRole.PERSISTENCE, null));

        assertEquals("topic-subscriber-replay-drift-rejected", e.getMessage());
        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_REPLAY_DRIFT, 1L);
        }

    @Test
    public void warnAllowPolicyDriftWarnsOnceAndReplays()
        {
        setMode("prod", TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW);

        HashSet<String> setDedup = new HashSet<>();
        RemoteInstallGate.enforceTopicSubscriberReplay(new PlainFilter(), null,
                SerializationRole.PERSISTENCE, null, setDedup);
        RemoteInstallGate.enforceTopicSubscriberReplay(new PlainFilter(), null,
                SerializationRole.PERSISTENCE, null, setDedup);

        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_REPLAY_DRIFT, 1L);
        assertEquals(m_listMessages.toString(), 1, m_listMessages.size());
        assertTrue(m_listMessages.get(0).contains(PlainFilter.class.getName()));
        assertTrue(m_listMessages.get(0).contains(TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW));
        }

    @Test
    public void compatibilityWithExplicitRejectRefusesReplay()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, TopicsPersistedPolicyDrift.VALUE_REJECT);

        HashSet<String> setDedup = new HashSet<>();
        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteInstallGate.enforceTopicSubscriberReplay(new PlainFilter(), null,
                        SerializationRole.PERSISTENCE, null, setDedup));

        assertEquals("topic-subscriber-replay-drift-rejected", e.getMessage());
        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_REPLAY_DRIFT, 1L);
        }

    @Test
    public void rejectPolicyDriftRefusesNestedExtractorReplay()
        {
        setMode("prod", TopicsPersistedPolicyDrift.VALUE_REJECT);

        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteInstallGate.enforceTopicSubscriberReplay(
                        new EqualsFilter<>(new PlainExtractor(), "value"), null,
                        SerializationRole.PERSISTENCE, null));

        assertEquals("topic-subscriber-replay-drift-rejected", e.getMessage());
        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EXTRACT, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EXTRACT, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_REPLAY_DRIFT, 1L);
        }

    @Test
    public void warnAllowPolicyDriftWarnsOnceForNestedExtractorReplay()
        {
        setMode("prod", TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW);

        HashSet<String> setDedup = new HashSet<>();
        RemoteInstallGate.enforceTopicSubscriberReplay(new EqualsFilter<>(new PlainExtractor(), "value"),
                null, SerializationRole.PERSISTENCE, null, setDedup);
        RemoteInstallGate.enforceTopicSubscriberReplay(new EqualsFilter<>(new PlainExtractor(), "value"),
                null, SerializationRole.PERSISTENCE, null, setDedup);

        assertPolicyCounter(OperationReason.EXTRACT, "prod", "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EXTRACT, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_REPLAY_DRIFT, 1L);
        assertEquals(m_listMessages.toString(), 1, m_listMessages.size());
        assertTrue(m_listMessages.get(0).contains(PlainExtractor.class.getName()));
        assertTrue(m_listMessages.get(0).contains(TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW));
        }

    @Test
    public void allowsNestedExecutableReplay()
        {
        setMode("prod", TopicsPersistedPolicyDrift.VALUE_REJECT);

        RemoteInstallGate.enforceTopicSubscriberReplay(
                new EqualsFilter<>(new AnnotatedExtractor(), "value"),
                new KeyExtractor<>(new AnnotatedExtractor()), SerializationRole.PERSISTENCE, null);

        assertPolicyCounter(OperationReason.EVALUATE_FILTER, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter(OperationReason.EXTRACT, "prod", "allowed",
                SerializationTelemetry.SUB_REASON_POLICY, 3L);
        assertTrue(m_listMessages.toString(), m_listMessages.isEmpty());
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
                + ",role=" + SerializationRole.PERSISTENCE.name() + "}";
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static String key(OperationReason reason, String sMode, String sResult, String sSubReason)
        {
        return "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.PERSISTENCE.name()
                + ",result=" + sResult
                + ",mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        }

    private static void setMode(String sMode, String sPolicyDrift)
        {
        setMode(sMode, CoherenceMode.SECURITY_MODE_HARDENED, sPolicyDrift);
        }

    private static void setMode(String sMode, String sSecurityMode, String sPolicyDrift)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
        restoreProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT, sPolicyDrift);
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
        TopicsPersistedPolicyDrift.resetForTesting();
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
    private String m_sSecurityModeOld;
    private String m_sPolicyDriftOld;
    private List<String> m_listMessages;
    }
