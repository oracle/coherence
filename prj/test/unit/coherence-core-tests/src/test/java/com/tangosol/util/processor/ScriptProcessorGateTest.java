/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.OperationReason;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.aggregator.ScriptAggregator;
import com.tangosol.util.extractor.ScriptValueExtractor;
import com.tangosol.util.filter.ScriptFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ScriptProcessor} security gates.
 *
 * @author Aleks Seovic  2026.05.08
 * @since 26.07
 */
public class ScriptProcessorGateTest
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
    public void scriptProcessorIsFinal()
        {
        assertTrue(Modifier.isFinal(ScriptProcessor.class.getModifiers()));
        }

    @Test
    public void allowsSpInDevByDefault()
        {
        setMode("dev", null);

        assertEquals("value", new ScriptProcessor<String, String, String>("js", "EntryEcho")
                .process(new SimpleEntry<>("key", "value", true)));

        assertPolicyCounter("dev", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertModeGateAbsent("dev");
        }

    @Test
    public void deniesSpInProdByDefault()
        {
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new ScriptProcessor<String, String, String>("js", "EntryEcho")
                        .process(new SimpleEntry<>("key", "value", true)));

        assertEquals("script-eval-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void allowsSpInProdWhenPropertyAllow()
        {
        setMode("prod", "allow");

        assertEquals("value", new ScriptProcessor<String, String, String>("js", "EntryEcho")
                .process(new SimpleEntry<>("key", "value", true)));

        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertModeGateAbsent("prod");
        }

    @Test
    public void deniesSpInProdWhenPropertyDeny()
        {
        setMode("prod", "deny");

        SecurityException e = assertThrows(SecurityException.class,
                () -> new ScriptProcessor<String, String, String>("js", "EntryEcho")
                        .process(new SimpleEntry<>("key", "value", true)));

        assertEquals("script-eval-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void enforceRunsBeforeModeGate()
        {
        setMode("prod", null);

        assertThrows(SecurityException.class,
                () -> new ScriptProcessor<String, String, String>("js", "EntryEcho")
                        .process(new SimpleEntry<>("key", "value", true)));

        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    public void legacyModeGateBehavesLikeDev()
        {
        setMode("legacy", null);

        assertEquals("value", new ScriptProcessor<String, String, String>("js", "EntryEcho")
                .process(new SimpleEntry<>("key", "value", true)));

        assertModeGateAbsent("legacy");
        }

    @Test
    public void scriptFilterUsesSameModeGate()
        {
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new ScriptFilter<String>("js", "ValuePresentFilter").evaluate("value"));

        assertEquals("script-eval-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);

        setMode("dev", null);
        assertTrue(new ScriptFilter<String>("js", "ValuePresentFilter").evaluate("value"));
        assertPolicyCounter("dev", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertModeGateAbsent("dev");
        }

    @Test
    public void scriptValueExtractorUsesSameModeGate()
        {
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new ScriptValueExtractor<String, String>("js", "IdentityExtractor").extract("value"));

        assertEquals("script-eval-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);

        setMode("prod", "allow");
        assertEquals("value", new ScriptValueExtractor<String, String>("js", "IdentityExtractor").extract("value"));
        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertModeGateAbsent("prod");
        }

    @Test
    public void scriptAggregatorUsesSameModeGateBeforeDelegateCreation()
        {
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class,
                () -> new ScriptAggregator<String, String, Integer, Integer>("js", "CountAggregator", 0).supply());

        assertEquals("script-eval-denied-by-mode", e.getMessage());
        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertPolicyCounter("prod", "rejected", SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);

        setMode("prod", "allow");
        new ScriptAggregator<String, String, Integer, Integer>("js", "CountAggregator", 0).supply();
        assertPolicyCounter("prod", "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertModeGateAbsent("prod");
        }

    private static void assertPolicyCounter(String sMode, String sResult, String sSubReason, long cExpected)
        {
        String sKey = "coh.executable.policy_check{reason=" + OperationReason.SCRIPT_EVAL.name()
                + ",role=" + SerializationRole.UNCLASSIFIED.name()
                + ",result=" + sResult
                + ",mode=" + sMode
                + ",sub_reason=" + sSubReason + "}";
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static void assertModeGateAbsent(String sMode)
        {
        String sKey = "coh.executable.policy_check{reason=" + OperationReason.SCRIPT_EVAL.name()
                + ",role=" + SerializationRole.UNCLASSIFIED.name()
                + ",result=rejected,mode=" + sMode
                + ",sub_reason=" + SerializationTelemetry.SUB_REASON_MODE_GATE + "}";
        assertFalse(SerializationTelemetry.snapshot().containsKey(sKey));
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
