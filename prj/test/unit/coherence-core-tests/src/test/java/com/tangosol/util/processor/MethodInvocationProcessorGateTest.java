/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.LambdaBytecodeGate;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.OperationReason;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link MethodInvocationProcessor} security gates.
 *
 * @author Aleks Seovic  2026.05.08
 * @since 26.04
 */
public class MethodInvocationProcessorGateTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        SerializationTelemetry.resetForTesting();
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        SerializationTelemetry.resetForTesting();
        }

    @Test
    public void mipIsFinal()
        {
        assertTrue(Modifier.isFinal(MethodInvocationProcessor.class.getModifiers()));
        }

    @Test
    public void allowsMipInDevByDefault()
        {
        setMode("dev", null);

        assertEquals(Integer.valueOf(3), new MethodInvocationProcessor<String, String, Integer>("length", false)
                .process(new SimpleEntry<>("key", "foo", true)));

        assertPolicyCounter("dev", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertModeGateAbsent("dev");
        }

    @Test
    public void deniesMipInProdByDefault()
        {
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, String, Integer>("length", false)
                        .process(new SimpleEntry<>("key", "foo", true)));

        assertEquals("method-invocation-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void allowsMipInProdWhenPropertyAllow()
        {
        setMode("prod", "allow");

        assertEquals(Integer.valueOf(3), new MethodInvocationProcessor<String, String, Integer>("length", false)
                .process(new SimpleEntry<>("key", "foo", true)));

        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertModeGateAbsent("prod");
        }

    @Test
    public void deniesMipInProdWhenPropertyDeny()
        {
        setMode("prod", "deny");

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, String, Integer>("length", false)
                        .process(new SimpleEntry<>("key", "foo", true)));

        assertEquals("method-invocation-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void rejectsMipTargetingDenyListedTargetClass()
        {
        setMode("prod", "allow");

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Runtime, Integer>("availableProcessors", false)
                        .process(new SimpleEntry<>("key", Runtime.getRuntime(), true)));

        assertEquals(LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, e.getMessage());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_DENYLIST, 1L);
        assertBytecodeCounter("prod", LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, 1L);
        }

    @Test
    public void rejectsMipTargetingDenyListedMethod()
            throws Exception
        {
        setMode("prod", "allow");
        Method method = String.class.getDeclaredMethod("length");

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Method, Void>("setAccessible", false, true)
                        .process(new SimpleEntry<>("key", method, true)));

        assertEquals(LambdaBytecodeGate.REASON_METHOD_ON_DENYLIST, e.getMessage());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_DENYLIST, 1L);
        assertBytecodeCounter("prod", LambdaBytecodeGate.REASON_METHOD_ON_DENYLIST, 1L);
        }

    @Test
    public void rejectsMipTargetingDenyListedSupplierClass()
        {
        setMode("prod", "allow");
        DeniedSupplier supplier = new DeniedSupplier();

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Value, Integer>(supplier, "answer", false)
                        .process(new SimpleEntry<>("key", new Value(), true)));

        assertEquals(LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, e.getMessage());
        assertFalse(supplier.wasCalled());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_DENYLIST, 1L);
        assertBytecodeCounter("prod", LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, 1L);
        }

    @Test
    public void allowsMipTargetingUserClassNotOnDenyList()
        {
        setMode("prod", "allow");

        assertEquals(Integer.valueOf(42), new MethodInvocationProcessor<String, Value, Integer>("answer", false)
                .process(new SimpleEntry<>("key", new Value(), true)));
        }

    @Test
    public void allowsMipTargetingNonAllowlistedNonDenyListedClass()
        {
        setMode("prod", "allow");

        assertEquals(Integer.valueOf(43), new MethodInvocationProcessor<String, NonAllowlistedValue, Integer>(
                "answer", false).process(new SimpleEntry<>("key", new NonAllowlistedValue(43), true)));

        assertEquals(Integer.valueOf(44), new MethodInvocationProcessor<String, NonAllowlistedValue, Integer>(
                new AllowedSupplier(), "answer", false).process(new SimpleEntry<>("key", null, false)));
        }

    @Test
    public void absentEntrySupplierDoesNotRunBeforeModeGate()
        {
        setMode("prod", null);
        ObservableSupplier<Value> supplier = new ObservableSupplier<>(new Value());
        SimpleEntry<String, Value> entry = new SimpleEntry<>("key", null, false);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Value, Integer>(supplier, "answer", false)
                        .process(entry));

        assertEquals("method-invocation-denied-by-mode", e.getMessage());
        assertFalse(supplier.wasCalled());
        assertFalse(entry.isPresent());
        assertNull(entry.getValue());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void absentEntryDenyListedSupplierRunsBeforeModeGate()
        {
        setMode("prod", null);
        DeniedSupplier supplier = new DeniedSupplier();
        SimpleEntry<String, Value> entry = new SimpleEntry<>("key", null, false);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Value, Integer>(supplier, "answer", false)
                        .process(entry));

        assertEquals(LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, e.getMessage());
        assertFalse(supplier.wasCalled());
        assertFalse(entry.isPresent());
        assertNull(entry.getValue());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_DENYLIST, 1L);
        assertPolicyCounterAbsent("prod", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertBytecodeCounter("prod", LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, 1L);
        }

    @Test
    public void absentEntrySupplierRunsAfterModeAllows()
        {
        setMode("prod", "allow");
        Value value = new Value();
        ObservableSupplier<Value> supplier = new ObservableSupplier<>(value);
        SimpleEntry<String, Value> entry = new SimpleEntry<>("key", null, false);

        assertEquals(Integer.valueOf(42), new MethodInvocationProcessor<String, Value, Integer>(
                supplier, "answer", false).process(entry));

        assertTrue(supplier.wasCalled());
        assertTrue(entry.isPresent());
        assertSame(value, entry.getValue());
        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertModeGateAbsent("prod");
        }

    @Test
    public void absentEntrySupplierTargetDenyListRejectsBeforeMutation()
        {
        setMode("prod", "allow");
        Runtime runtime = Runtime.getRuntime();
        ObservableSupplier<Runtime> supplier = new ObservableSupplier<>(runtime);
        SimpleEntry<String, Runtime> entry = new SimpleEntry<>("key", null, false);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Runtime, Integer>(
                        supplier, "availableProcessors", false).process(entry));

        assertEquals(LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, e.getMessage());
        assertTrue(supplier.wasCalled());
        assertFalse(entry.isPresent());
        assertNull(entry.getValue());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_DENYLIST, 1L);
        assertBytecodeCounter("prod", LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, 1L);
        }

    @Test
    public void enforceRunsBeforeModeGate()
        {
        setMode("prod", null);

        assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, String, Integer>("length", false)
                        .process(new SimpleEntry<>("key", "foo", true)));

        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void denyListRunsBeforeModeGate()
        {
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Runtime, Integer>("availableProcessors", false)
                        .process(new SimpleEntry<>("key", Runtime.getRuntime(), true)));

        assertEquals(LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, e.getMessage());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_DENYLIST, 1L);
        assertPolicyCounterAbsent("prod", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertBytecodeCounter("prod", LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, 1L);
        }

    @Test
    public void methodDenyListRunsBeforeModeGate()
            throws Exception
        {
        setMode("prod", null);
        Method method = String.class.getDeclaredMethod("length");

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Method, Void>("setAccessible", false, true)
                        .process(new SimpleEntry<>("key", method, true)));

        assertEquals(LambdaBytecodeGate.REASON_METHOD_ON_DENYLIST, e.getMessage());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_DENYLIST, 1L);
        assertPolicyCounterAbsent("prod", SerializationTelemetry.SUB_REASON_MODE_GATE);
        assertBytecodeCounter("prod", LambdaBytecodeGate.REASON_METHOD_ON_DENYLIST, 1L);
        }

    @Test
    public void legacyMipDenyListIsHardFloor()
        {
        setMode("legacy", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new MethodInvocationProcessor<String, Runtime, Integer>("availableProcessors", false)
                        .process(new SimpleEntry<>("key", Runtime.getRuntime(), true)));

        assertEquals(LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, e.getMessage());
        assertPolicyCounter("legacy", "rejected", SerializationTelemetry.SUB_REASON_DENYLIST, 1L);
        assertBytecodeCounter("legacy", LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST, 1L);
        }

    @Test
    public void legacyModeGateBehavesLikeDev()
        {
        setMode("legacy", null);

        assertEquals(Integer.valueOf(3), new MethodInvocationProcessor<String, String, Integer>("length", false)
                .process(new SimpleEntry<>("key", "foo", true)));

        assertModeGateAbsent("legacy");
        }

    private static void assertPolicyCounter(String sMode, String sResult, String sSubReason, long cExpected)
        {
        String sKey = "coh.executable.policy_check{reason=" + OperationReason.PROCESS_ENTRY.name()
                + ",role=" + SerializationRole.UNCLASSIFIED.name()
                + ",result=" + sResult
                + ",mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static void assertModeGateAbsent(String sMode)
        {
        String sKey = "coh.executable.policy_check{reason=" + OperationReason.PROCESS_ENTRY.name()
                + ",role=" + SerializationRole.UNCLASSIFIED.name()
                + ",result=rejected,mode=" + sMode
                + ",sub_reason=" + SerializationTelemetry.SUB_REASON_MODE_GATE + "}";
        assertFalse(SerializationTelemetry.snapshot().containsKey(sKey));
        }

    private static void assertPolicyCounterAbsent(String sMode, String sSubReason)
        {
        String sKey = "coh.executable.policy_check{reason=" + OperationReason.PROCESS_ENTRY.name()
                + ",role=" + SerializationRole.UNCLASSIFIED.name()
                + ",result=rejected,mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        assertFalse("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                SerializationTelemetry.snapshot().containsKey(sKey));
        }

    private static void assertBytecodeCounter(String sMode, String sReason, long cExpected)
        {
        String sKey = "coh.serialization.lambda_bytecode_check{result=rejected,reason=" + sReason
                + ",mode=" + sMode
                + ",route=" + SerializationRole.UNCLASSIFIED.name()
                + ",site=mip_reflection}";
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
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

    public static class Value
        {
        public int answer()
            {
            return 42;
            }
        }

    public static class DeniedSupplier
            implements Remote.Supplier<Value>
        {
        @Override
        public Value get()
            {
            m_fCalled = true;
            return new Value();
            }

        public boolean wasCalled()
            {
            return m_fCalled;
            }

        private boolean m_fCalled;
        }

    public static class NonAllowlistedValue
        {
        public NonAllowlistedValue(int nValue)
            {
            m_nValue = nValue;
            }

        public int answer()
            {
            return m_nValue;
            }

        private final int m_nValue;
        }

    public static class AllowedSupplier
            implements Remote.Supplier<NonAllowlistedValue>
        {
        @Override
        public NonAllowlistedValue get()
            {
            return new NonAllowlistedValue(44);
            }
        }

    public static class ObservableSupplier<V>
            implements Remote.Supplier<V>
        {
        public ObservableSupplier(V value)
            {
            m_value = value;
            }

        @Override
        public V get()
            {
            m_fCalled = true;
            return m_value;
            }

        public boolean wasCalled()
            {
            return m_fCalled;
            }

        private final V m_value;
        private boolean m_fCalled;
        }

    private static class SimpleEntry<K, V>
            implements InvocableMap.Entry<K, V>
        {
        SimpleEntry(K key, V value, boolean fPresent)
            {
            m_key      = key;
            m_value    = value;
            m_fPresent = fPresent;
            }

        @Override
        public K getKey()
            {
            return m_key;
            }

        @Override
        public V getValue()
            {
            return m_value;
            }

        @Override
        public V setValue(V value)
            {
            V valueOld = m_value;
            m_value    = value;
            m_fPresent = true;
            return valueOld;
            }

        @Override
        public void setValue(V value, boolean fSynthetic)
            {
            setValue(value);
            }

        @Override
        public <T> void update(ValueUpdater<V, T> updater, T value)
            {
            updater.update(m_value, value);
            }

        @Override
        public boolean isPresent()
            {
            return m_fPresent;
            }

        @Override
        public boolean isSynthetic()
            {
            return false;
            }

        @Override
        public void remove(boolean fSynthetic)
            {
            m_value    = null;
            m_fPresent = false;
            }

        @Override
        public <T, E> E extract(ValueExtractor<T, E> extractor)
            {
            return extractor.extract((T) m_value);
            }

        private final K m_key;
        private V       m_value;
        private boolean m_fPresent;
        }

    private String m_sModeOld;
    private String m_sDynamicRemoteOld;
    }
