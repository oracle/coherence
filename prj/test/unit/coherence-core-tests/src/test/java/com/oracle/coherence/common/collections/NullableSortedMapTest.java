/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.collections;

import com.google.common.collect.testing.ConcurrentNavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapEntrySetTester;

import java.util.Map.Entry;
import java.util.SortedMap;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link NullableSortedMap}.
 */
public final class NullableSortedMapTest
        extends TestCase
    {
    public static TestSuite suite() throws NoSuchMethodException
        {
        return ConcurrentNavigableMapTestSuiteBuilder
                .using(generator())
                .named("NullableSortedMap")
                .withFeatures(
                        CollectionSize.ANY,
                        MapFeature.GENERAL_PURPOSE,
                        MapFeature.ALLOWS_ANY_NULL_QUERIES,
                        CollectionFeature.SUPPORTS_REMOVE,
                        CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
                .suppressing(MapEntrySetTester.class.getDeclaredMethod("testSetValue"))
                .createTestSuite();
        }

    private static TestStringSortedMapGenerator generator()
        {
        return new TestStringSortedMapGenerator()
            {
            protected SortedMap<String, String> create(Entry<String, String>[] entries)
                {
                SortedMap<String, String> map = new NullableSortedMap<>();
                for (var entry : entries)
                    {
                    map.put(entry.getKey(), entry.getValue());
                    }
                return map;
                }
            };
        }
    }
