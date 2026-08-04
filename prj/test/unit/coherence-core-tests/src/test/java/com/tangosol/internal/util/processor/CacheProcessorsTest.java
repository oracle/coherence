/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.processor;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import org.junit.Test;

import java.lang.reflect.Method;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link CacheProcessors}.
 *
 * @author as  2026.05.22
 */
public class CacheProcessorsTest
    {
    @Test
    public void shouldUseLegacyCompatibleLambdaProcessorsInLegacyMode()
        {
        withCoherenceMode("legacy", () ->
            {
            assertNotSame(CacheProcessors.ReplaceFunction.class,
                    CacheProcessors.replace((BiFunction<String, String, String>) (key, value) -> key + value).getClass());
            assertNotSame(CacheProcessors.ComputeIfAbsent.class,
                    CacheProcessors.computeIfAbsent((Function<String, String>) key -> key).getClass());
            assertNotSame(CacheProcessors.ComputeIfPresent.class,
                    CacheProcessors.computeIfPresent((BiFunction<String, String, String>) (key, value) -> key).getClass());
            assertNotSame(CacheProcessors.Compute.class,
                    CacheProcessors.compute((BiFunction<String, String, String>) (key, value) -> key).getClass());
            assertNotSame(CacheProcessors.Merge.class,
                    CacheProcessors.merge("value", (BiFunction<String, String, String>) (oldValue, value) -> value).getClass());
            });
        }

    @Test
    public void shouldUseNamedClassesForFunctionProcessorsInDevMode()
        {
        assertNamedFunctionProcessors("dev");
        }

    @Test
    public void shouldUseNamedClassesForFunctionProcessorsInProdMode()
        {
        assertNamedFunctionProcessors("prod");
        }

    private static void assertNamedFunctionProcessors(String sMode)
        {
        withCoherenceMode(sMode, () ->
            {
            assertSame(CacheProcessors.ReplaceFunction.class,
                    CacheProcessors.replace((BiFunction<String, String, String>) (key, value) -> key + value).getClass());
            assertSame(CacheProcessors.ComputeIfAbsent.class,
                    CacheProcessors.computeIfAbsent((Function<String, String>) key -> key).getClass());
            assertSame(CacheProcessors.ComputeIfPresent.class,
                    CacheProcessors.computeIfPresent((BiFunction<String, String, String>) (key, value) -> key).getClass());
            assertSame(CacheProcessors.Compute.class,
                    CacheProcessors.compute((BiFunction<String, String, String>) (key, value) -> key).getClass());
            assertSame(CacheProcessors.Merge.class,
                    CacheProcessors.merge("value", (BiFunction<String, String, String>) (oldValue, value) -> value).getClass());
            });
        }

    @Test
    public void shouldRejectNullFunctionProcessorArguments()
        {
        assertThrows(NullPointerException.class,
                () -> CacheProcessors.replace((BiFunction<String, String, String>) null));
        assertThrows(NullPointerException.class,
                () -> CacheProcessors.computeIfAbsent((Function<String, String>) null));
        assertThrows(NullPointerException.class,
                () -> CacheProcessors.computeIfPresent((BiFunction<String, String, String>) null));
        assertThrows(NullPointerException.class,
                () -> CacheProcessors.compute((BiFunction<String, String, String>) null));
        assertThrows(NullPointerException.class,
                () -> CacheProcessors.<String, String>merge(null, (oldValue, value) -> value));
        assertThrows(NullPointerException.class,
                () -> CacheProcessors.merge("value", (BiFunction<String, String, String>) null));
        }

    @Test
    public void shouldProcessFunctionBasedReplace()
        {
        SimpleEntry<String, String> entry = new SimpleEntry<>("key", "value", true);

        assertNull(CacheProcessors.replace((BiFunction<String, String, String>) (key, value) -> key + ':' + value)
                .process(entry));

        assertTrue(entry.isPresent());
        assertEquals("key:value", entry.getValue());
        }

    @Test
    public void shouldProcessComputeIfAbsent()
        {
        SimpleEntry<String, String> entry = new SimpleEntry<>("key", null, false);

        assertEquals("key-value", CacheProcessors.computeIfAbsent((Function<String, String>) key -> key + "-value")
                .process(entry));

        assertTrue(entry.isPresent());
        assertEquals("key-value", entry.getValue());
        }

    @Test
    public void shouldProcessComputeIfPresent()
        {
        SimpleEntry<String, String> entry = new SimpleEntry<>("key", "value", true);

        assertEquals("key:value", CacheProcessors.computeIfPresent(
                (BiFunction<String, String, String>) (key, value) -> key + ':' + value).process(entry));

        assertTrue(entry.isPresent());
        assertEquals("key:value", entry.getValue());
        }

    @Test
    public void shouldRemoveWhenComputeReturnsNullForPresentEntry()
        {
        SimpleEntry<String, String> entry = new SimpleEntry<>("key", "value", true);

        assertNull(CacheProcessors.compute((BiFunction<String, String, String>) (key, value) -> null).process(entry));

        assertFalse(entry.isPresent());
        assertNull(entry.getValue());
        }

    @Test
    public void shouldProcessMerge()
        {
        SimpleEntry<String, String> entry = new SimpleEntry<>("key", "old", true);

        assertEquals("old-new", CacheProcessors.<String, String>merge("new",
                (BiFunction<String, String, String>) (oldValue, value) -> oldValue + '-' + value).process(entry));

        assertTrue(entry.isPresent());
        assertEquals("old-new", entry.getValue());
        }

    @Test
    public void shouldSerializeFunctionProcessorsUsingPof()
        {
        assertPofRoundTrip(new CacheProcessors.ReplaceFunction<>(), CacheProcessors.ReplaceFunction.class, 495);
        assertPofRoundTrip(new CacheProcessors.ComputeIfAbsent<>(), CacheProcessors.ComputeIfAbsent.class, 496);
        assertPofRoundTrip(new CacheProcessors.ComputeIfPresent<>(), CacheProcessors.ComputeIfPresent.class, 497);
        assertPofRoundTrip(new CacheProcessors.Compute<>(), CacheProcessors.Compute.class, 498);

        assertPofRoundTrip(new CacheProcessors.Merge<>(), CacheProcessors.Merge.class, 499);
        }

    @SuppressWarnings("unchecked")
    private static <T> T assertPofRoundTrip(T value, Class<?> clz, int nType)
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");

        assertEquals(nType, serializer.getUserTypeIdentifier(clz));

        Binary binary = ExternalizableHelper.toBinary(value, serializer);
        Object result = ExternalizableHelper.fromBinary(binary, serializer);

        assertEquals(clz, result.getClass());
        return (T) result;
        }

    private static void withCoherenceMode(String sMode, Runnable runnable)
        {
        String sPrevious = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        System.setProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        resetCoherenceMode();
        try
            {
            runnable.run();
            }
        finally
            {
            if (sPrevious == null)
                {
                System.clearProperty(CoherenceMode.PROP_COHERENCE_MODE);
                }
            else
                {
                System.setProperty(CoherenceMode.PROP_COHERENCE_MODE, sPrevious);
                }
            resetCoherenceMode();
            }
        }

    private static void resetCoherenceMode()
        {
        try
            {
            Method method = CoherenceMode.class.getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError(e);
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
        @SuppressWarnings("unchecked")
        public <T, E> E extract(ValueExtractor<T, E> extractor)
            {
            return extractor.extract((T) m_value);
            }

        private final K m_key;
        private V       m_value;
        private boolean m_fPresent;
        }
    }
