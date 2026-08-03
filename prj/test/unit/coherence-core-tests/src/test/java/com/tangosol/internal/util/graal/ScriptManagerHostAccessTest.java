/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.processor.ScriptProcessor;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for {@link ScriptManager} host-access hardening.
 *
 * @author Aleks Seovic  2026.05.08
 * @since 26.04
 */
public class ScriptManagerHostAccessTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void denyAccessRejectsRuntimeFromGuest()
        {
        assertThrows(PolyglotException.class,
                () -> context().eval("js", "Java.type('java.lang.Runtime')"));
        }

    @Test
    public void denyAccessRejectsHostAccessOnDenyListed()
        {
        Context context = context();
        context.getBindings("js").putMember("runtimeRef", Runtime.getRuntime());

        assertThrows(PolyglotException.class,
                () -> context.eval("js", "runtimeRef.availableProcessors()"));
        }

    @Test
    public void allowsAccessOnNonDenyListedClasses()
        {
        Value value = context().eval("js", "Java.type('java.lang.String').valueOf(42)");

        assertEquals("42", value.asString());
        }

    /**
     * Canary for the {@code allowAllAccess(true)} removal: re-introducing it
     * would let {@code Java.type('java.lang.ProcessBuilder')} through and fail
     * this host-class-lookup assertion.
     */
    @Test
    public void processBuilderUnreachableViaHostClassLookup()
        {
        assertThrows(PolyglotException.class,
                () -> context().eval("js", "Java.type('java.lang.ProcessBuilder')"));
        }

    @Test
    public void legacySpHostAccessIsHardFloor()
        {
        setMode("legacy", null);

        assertThrows(PolyglotException.class,
                () -> new ScriptProcessor<String, String, Object>("js", "RuntimeProbe")
                        .process(new SimpleEntry<>("key", "value", true)));
        }

    private static Context context()
        {
        return ScriptManager.getInstance().getContext();
        }

    private static void setMode(String sMode, String sDynamicRemote)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
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
