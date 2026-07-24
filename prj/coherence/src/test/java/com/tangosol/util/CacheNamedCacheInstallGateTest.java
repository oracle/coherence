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

import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for remote NamedCache data-plane install gates.
 *
 * @author Aleks Seovic  2026.05.12
 * @since 26.04
 */
public class CacheNamedCacheInstallGateTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sSecurityModeOld  = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        SerializationTelemetry.resetForTesting();
        resetSecurityConfig();
        RemoteExecutablePolicy.resetForTesting();
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        resetSecurityConfig();
        SerializationTelemetry.resetForTesting();
        RemoteExecutablePolicy.resetForTesting();
        }

    @Test
    public void allowsAnnotatedInstallInAllModes()
        {
        for (GateCase gate : GATES)
            {
            for (String sMode : new String[] {"prod", "dev"})
                {
                setMode(sMode, null);

                gate.enforce(gate.annotated());

                assertPolicyCounter(gate.reason(), sMode, "allowed",
                        SerializationTelemetry.SUB_REASON_POLICY, 1L);
                assertCounterAbsent(gate.reason(), sMode, "rejected",
                        SerializationTelemetry.SUB_REASON_MODE_GATE);
                }

            setMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

            gate.enforce(gate.annotated());

            assertCounterAbsent(gate.reason(), "prod", "allowed",
                    SerializationTelemetry.SUB_REASON_POLICY);
            assertCounterAbsent(gate.reason(), "prod", "rejected",
                    SerializationTelemetry.SUB_REASON_MODE_GATE);
            }
        }

    @Test
    public void rejectsUnannotatedInstallInProdAndDev()
        {
        for (GateCase gate : GATES)
            {
            for (String sMode : new String[] {"prod", "dev"})
                {
                setMode(sMode, null);

                assertThrows(SecurityException.class, () -> gate.enforce(gate.plain()));

                assertPolicyCounter(gate.reason(), sMode, "rejected",
                        SerializationTelemetry.SUB_REASON_POLICY, 1L);
                assertCounterAbsent(gate.reason(), sMode, "rejected",
                        SerializationTelemetry.SUB_REASON_MODE_GATE);
                }
            }
        }

    @Test
    public void compatibilityShadowsUnannotatedInstall()
        {
        for (GateCase gate : GATES)
            {
            Object oPlain = gate.plain();
            setMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

            gate.enforce(oPlain);

            assertWouldRejectCounter(oPlain.getClass(), gate.reason(), 1L);
            assertCounterAbsent(gate.reason(), "prod", "rejected",
                    SerializationTelemetry.SUB_REASON_POLICY);
            }
        }

    @Test
    public void rejectsDynamicInstallInProd()
        {
        for (GateCase gate : GATES)
            {
            Object oDynamic = gate.dynamic();
            assertTrue(oDynamic.getClass().isSynthetic());
            setMode("prod", null);

            SecurityException e = assertThrows(SecurityException.class, () -> gate.enforce(oDynamic));

            assertEquals(gate.modeMessage(), e.getMessage());
            assertPolicyCounter(gate.reason(), "prod", "rejected",
                    SerializationTelemetry.SUB_REASON_POLICY, 1L);
            assertPolicyCounter(gate.reason(), "prod", "rejected",
                    SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
            }
        }

    @Test
    public void allowsDynamicInstallInDevCompatibility()
        {
        for (GateCase gate : GATES)
            {
            Object oDynamic = gate.dynamic();
            assertTrue(oDynamic.getClass().isSynthetic());
            setMode("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

            gate.enforce(oDynamic);

            assertWouldRejectCounter(oDynamic.getClass(), gate.reason(), 2L);
            assertCounterAbsent(gate.reason(), "dev", "rejected",
                    SerializationTelemetry.SUB_REASON_MODE_GATE);
            }
        }

    @Test
    public void compatibilityShadowsDynamicInstall()
        {
        for (GateCase gate : GATES)
            {
            Object oDynamic = gate.dynamic();
            assertTrue(oDynamic.getClass().isSynthetic());
            setMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

            gate.enforce(oDynamic);

            assertWouldRejectCounter(oDynamic.getClass(), gate.reason(), 2L);
            assertCounterAbsent(gate.reason(), "prod", "rejected",
                    SerializationTelemetry.SUB_REASON_MODE_GATE);
            }
        }

    @Test
    public void compatibilityWithExplicitDenyRejectsDynamicInstall()
        {
        for (GateCase gate : GATES)
            {
            Object oDynamic = gate.dynamic();
            assertTrue(oDynamic.getClass().isSynthetic());
            setMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "deny");

            SecurityException e = assertThrows(SecurityException.class, () -> gate.enforce(oDynamic));

            assertEquals(gate.modeMessage(), e.getMessage());
            assertWouldRejectCounter(oDynamic.getClass(), gate.reason(), 1L);
            assertPolicyCounter(gate.reason(), "prod", "rejected",
                    SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
            }
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
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}";
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static String key(OperationReason reason, String sMode, String sResult, String sSubReason)
        {
        return "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=" + sResult
                + ",mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        }

    private static void setMode(String sMode, String sDynamicRemote)
        {
        setMode(sMode, CoherenceMode.SECURITY_MODE_HARDENED, sDynamicRemote);
        }

    private static void setMode(String sMode, String sSecurityMode, String sDynamicRemote)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
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
            java.lang.reflect.Method method = SecurityConfig.class.getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError(e);
            }
        }

    @Remote.Executable
    public static class AnnotatedProcessor
            implements InvocableMap.EntryProcessor<String, String, String>
        {
        @Override
        public String process(InvocableMap.Entry<String, String> entry)
            {
            return entry.getValue();
            }
        }

    public static class PlainProcessor
            implements InvocableMap.EntryProcessor<String, String, String>
        {
        @Override
        public String process(InvocableMap.Entry<String, String> entry)
            {
            return entry.getValue();
            }
        }

    @Remote.Executable
    public static class AnnotatedAggregator
            implements InvocableMap.EntryAggregator<String, String, Integer>
        {
        @Override
        public Integer aggregate(java.util.Set<? extends InvocableMap.Entry<? extends String, ? extends String>> entries)
            {
            return entries.size();
            }
        }

    public static class PlainAggregator
            implements InvocableMap.EntryAggregator<String, String, Integer>
        {
        @Override
        public Integer aggregate(java.util.Set<? extends InvocableMap.Entry<? extends String, ? extends String>> entries)
            {
            return entries.size();
            }
        }

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return value != null;
            }
        }

    public static class PlainFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return value != null;
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

    @Remote.Executable
    public static class AnnotatedComparator
            implements Comparator<String>
        {
        @Override
        public int compare(String left, String right)
            {
            return 0;
            }
        }

    public static class PlainComparator
            implements Comparator<String>
        {
        @Override
        public int compare(String left, String right)
            {
            return 0;
            }
        }

    private abstract static class GateCase
        {
        GateCase(OperationReason reason, String sModeMessage)
            {
            m_reason       = reason;
            m_sModeMessage = sModeMessage;
            }

        abstract void enforce(Object executable);

        abstract Object annotated();

        abstract Object plain();

        abstract Object dynamic();

        OperationReason reason()
            {
            return m_reason;
            }

        String modeMessage()
            {
            return m_sModeMessage;
            }

        private final OperationReason m_reason;
        private final String          m_sModeMessage;
        }

    private static final GateCase[] GATES = new GateCase[]
        {
        new GateCase(OperationReason.PROCESS_ENTRY, "cache-processor-install-denied-by-mode")
            {
            @Override
            void enforce(Object executable)
                {
                RemoteInstallGate.enforceCacheProcessorInstall(
                        (InvocableMap.EntryProcessor<?, ?, ?>) executable, SerializationRole.EXTEND_PROXY, null);
                }

            @Override
            Object annotated()
                {
                return new AnnotatedProcessor();
                }

            @Override
            Object plain()
                {
                return new PlainProcessor();
                }

            @Override
            Object dynamic()
                {
                return (InvocableMap.EntryProcessor<String, String, String>) entry -> entry.getValue();
                }
            },
        new GateCase(OperationReason.AGGREGATE, "cache-aggregator-install-denied-by-mode")
            {
            @Override
            void enforce(Object executable)
                {
                RemoteInstallGate.enforceCacheAggregatorInstall(
                        (InvocableMap.EntryAggregator<?, ?, ?>) executable, SerializationRole.EXTEND_PROXY, null);
                }

            @Override
            Object annotated()
                {
                return new AnnotatedAggregator();
                }

            @Override
            Object plain()
                {
                return new PlainAggregator();
                }

            @Override
            Object dynamic()
                {
                return (InvocableMap.EntryAggregator<String, String, Integer>) entries -> entries.size();
                }
            },
        new GateCase(OperationReason.EVALUATE_FILTER, "cache-filter-install-denied-by-mode")
            {
            @Override
            void enforce(Object executable)
                {
                RemoteInstallGate.enforceCacheFilterInstall((Filter<?>) executable, SerializationRole.EXTEND_PROXY, null);
                }

            @Override
            Object annotated()
                {
                return new AnnotatedFilter();
                }

            @Override
            Object plain()
                {
                return new PlainFilter();
                }

            @Override
            Object dynamic()
                {
                return (Filter<String>) value -> value != null;
                }
            },
        new GateCase(OperationReason.EXTRACT, "cache-extractor-install-denied-by-mode")
            {
            @Override
            void enforce(Object executable)
                {
                RemoteInstallGate.enforceCacheExtractorInstall(
                        (ValueExtractor<?, ?>) executable, SerializationRole.EXTEND_PROXY, null);
                }

            @Override
            Object annotated()
                {
                return new AnnotatedExtractor();
                }

            @Override
            Object plain()
                {
                return new PlainExtractor();
                }

            @Override
            Object dynamic()
                {
                return (ValueExtractor<String, String>) value -> value;
                }
            },
        new GateCase(OperationReason.COMPARE, "cache-comparator-install-denied-by-mode")
            {
            @Override
            void enforce(Object executable)
                {
                RemoteInstallGate.enforceCacheComparatorInstall((Comparator<?>) executable,
                        SerializationRole.EXTEND_PROXY, null);
                }

            @Override
            Object annotated()
                {
                return new AnnotatedComparator();
                }

            @Override
            Object plain()
                {
                return new PlainComparator();
                }

            @Override
            Object dynamic()
                {
                return (Comparator<String>) (left, right) -> 0;
                }
            }
        };

    private String m_sModeOld;
    private String m_sSecurityModeOld;
    private String m_sDynamicRemoteOld;
    }
