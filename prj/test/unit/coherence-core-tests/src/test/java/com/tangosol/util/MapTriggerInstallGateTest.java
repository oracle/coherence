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
 * Unit tests for remote MapTrigger install gates.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.04
 */
public class MapTriggerInstallGateTest
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
    public void allowsAnnotatedTriggerInstall()
        {
        setMode("prod", null);

        RemoteInstallGate.enforceMapTriggerInstall(new AnnotatedTrigger(),
                SerializationRole.EXTEND_PROXY, null);

        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounterAbsent("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void rejectsUnannotatedTriggerInstall_prod()
        {
        setMode("prod", null);

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceMapTriggerInstall(
                new PlainTrigger(), SerializationRole.EXTEND_PROXY, null));

        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounterAbsent("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void rejectsUnannotatedTriggerInstall_dev()
        {
        setMode("dev", null);

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceMapTriggerInstall(
                new PlainTrigger(), SerializationRole.EXTEND_PROXY, null));

        assertPolicyCounter("dev", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounterAbsent("dev", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void legacyShadowsUnannotatedTriggerInstall()
        {
        setMode("legacy", null);

        RemoteInstallGate.enforceMapTriggerInstall(new PlainTrigger(),
                SerializationRole.EXTEND_PROXY, null);

        assertWouldRejectCounter(PlainTrigger.class, 1L);
        }

    @Test
    public void rejectsDynamicTriggerInstall_prod()
            throws Exception
        {
        MapTrigger<String, String> trigger = dynamicTrigger();
        assertTrue(trigger.getClass().isSynthetic());
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteInstallGate.enforceMapTriggerInstall(trigger, SerializationRole.EXTEND_PROXY, null));

        assertEquals("map-trigger-install-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        assertCounterAbsent("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        }

    @Test
    public void allowsDynamicTriggerInstall_dev()
            throws Exception
        {
        MapTrigger<String, String> trigger = dynamicTrigger();
        assertTrue(trigger.getClass().isSynthetic());
        setMode("dev", null);

        RemoteInstallGate.enforceMapTriggerInstall(trigger, SerializationRole.EXTEND_PROXY, null);

        assertPolicyCounter("dev", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounterAbsent("dev", "allowed", SerializationTelemetry.SUB_REASON_POLICY);
        assertCounterAbsent("dev", "allowed", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertCounterAbsent("dev", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    @Test
    public void fqnSubstringHeuristicMatchesGenerated$$LambdaTrigger()
        {
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteInstallGate.enforceMapTriggerInstall(new Generated$$Lambda$Trigger(),
                        SerializationRole.EXTEND_PROXY, null));

        assertEquals("map-trigger-install-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void legacyShadowsDynamicTriggerInstall()
            throws Exception
        {
        MapTrigger<String, String> trigger = dynamicTrigger();
        assertTrue(trigger.getClass().isSynthetic());
        setMode("legacy", null);

        RemoteInstallGate.enforceMapTriggerInstall(trigger, SerializationRole.EXTEND_PROXY, null);

        assertWouldRejectCounter(trigger.getClass(), 2L);
        assertCounterAbsent("legacy", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE);
        }

    private static void assertPolicyCounter(String sMode, String sResult, String sSubReason, long cExpected)
        {
        String sKey = key(sMode, sResult, sSubReason);
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static void assertCounterAbsent(String sMode, String sResult, String sSubReason)
        {
        String sKey = key(sMode, sResult, sSubReason);
        assertFalse("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                SerializationTelemetry.snapshot().containsKey(sKey));
        }

    private static void assertWouldRejectCounter(Class<?> clz, long cExpected)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + OperationReason.TRIGGER.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}";
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static String key(String sMode, String sResult, String sSubReason)
        {
        return "coh.executable.policy_check{reason=" + OperationReason.TRIGGER.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
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

    private static MapTrigger<String, String> dynamicTrigger()
        {
        return entry -> entry.setValue("triggered");
        }

    @Remote.Executable
    public static class AnnotatedTrigger
            implements MapTrigger<String, String>
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            }
        }

    public static class PlainTrigger
            implements MapTrigger<String, String>
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            }
        }

    public static class Generated$$Lambda$Trigger
            implements MapTrigger<String, String>
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            }
        }

    private String m_sModeOld;
    private String m_sDynamicRemoteOld;
    }
