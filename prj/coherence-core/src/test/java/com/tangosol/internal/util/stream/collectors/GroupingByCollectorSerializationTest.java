/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream.collectors;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.io.internal.SerializationAllowlist;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.function.Remote;
import com.tangosol.util.stream.RemoteCollector;
import com.tangosol.util.stream.RemoteCollectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Regression tests for serialization of grouped collector results.
 *
 * @author phf  2026.10.14
 * @since 26.10
 */
public class GroupingByCollectorSerializationTest
    {
    @Test
    public void testGroupingCollectorRoundTrip()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withSecurityMode(sSecurityMode, () ->
                {
                assertGroupingCollectorRoundTrip(RemoteCollectors.groupingBy(
                        new ReflectionExtractor<String, Integer>("length"),
                        RemoteCollectors.toSortedSet(Remote.Comparator.<String>naturalOrder())));
                });
            }
        }

    @Test
    public void testGroupingCollectorOptionalResultRoundTrip()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withSecurityMode(sSecurityMode, () ->
                {
                Map<Integer, Optional<String>> original = Arrays.asList("a", "bb", "cc").stream().collect(
                        RemoteCollectors.groupingBy(new ReflectionExtractor<String, Integer>("length"),
                                RemoteCollectors.maxBy(Remote.Comparator.<String>naturalOrder())));
                Map<Integer, Optional<String>> copy = ExternalizableHelper.fromBinary(
                        ExternalizableHelper.toBinary(original));

                Map<Integer, Optional<String>> expected = new HashMap<>();
                expected.put(1, Optional.of("a"));
                expected.put(2, Optional.of("cc"));
                assertEquals(expected, copy);
                });
            }
        }

    private static <A> void assertGroupingCollectorRoundTrip(
            RemoteCollector<String, A, Map<Integer, SortedSet<String>>> collector)
        {
        A partial = collector.supplier().get();
        for (String value : Arrays.asList("a", "cc", "bb"))
            {
            collector.accumulator().accept(partial, value);
            }

        A copiedPartial = ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(partial));
        collector.accumulator().accept(copiedPartial, "aa");
        Map<Integer, SortedSet<String>> result = collector.finisher().apply(copiedPartial);
        Map<Integer, SortedSet<String>> copy = ExternalizableHelper.fromBinary(
                ExternalizableHelper.toBinary(result));

        assertEquals(2, copy.size());
        assertEquals(Arrays.asList("a"), new ArrayList<>(copy.get(1)));
        assertEquals(Arrays.asList("aa", "bb", "cc"), new ArrayList<>(copy.get(2)));
        }

    private static void withSecurityMode(String sSecurityMode, Runnable runnable)
        {
        String sModeOld         = System.getProperty("coherence.mode");
        String sSecurityModeOld = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        String sAllowedOld      = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        try
            {
            System.setProperty("coherence.mode", "dev");
            restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
            System.clearProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
            CoherenceModeHelper.reset();
            runnable.run();
            }
        finally
            {
            restoreProperty("coherence.mode", sModeOld);
            restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityModeOld);
            restoreProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, sAllowedOld);
            CoherenceModeHelper.reset();
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
    }
