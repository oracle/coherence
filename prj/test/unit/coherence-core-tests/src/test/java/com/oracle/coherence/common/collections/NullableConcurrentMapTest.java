/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.collections;

import com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link NullableConcurrentMap}.
 */
public final class NullableConcurrentMapTest
        extends TestCase
    {
    public static TestSuite suite() throws NoSuchMethodException
        {
        return NullableConcurrentMapTestSuiteBuilder
                .using(generator())
                .named("NullableConcurrentMap")
                .withFeatures(
                        CollectionSize.ANY,
                        MapFeature.GENERAL_PURPOSE,
                        MapFeature.ALLOWS_ANY_NULL_QUERIES,
                        CollectionFeature.SUPPORTS_REMOVE,
                        CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
                .createTestSuite();
        }

    private static TestStringMapGenerator generator()
        {
        return new TestStringMapGenerator()
            {
            @Override
            protected Map<String, String> create(Entry<String, String>[] entries)
                {
                Map<String, String> map = new NullableConcurrentMap<>();
                for (var entry : entries)
                    {
                    map.put(entry.getKey(), entry.getValue());
                    }
                return map;
                }
            };
        }

    private static class NullableConcurrentMapTestSuiteBuilder<K, V>
                extends ConcurrentMapTestSuiteBuilder<K, V>
        {
        public static <K, V> NullableConcurrentMapTestSuiteBuilder<K, V> using(TestMapGenerator<K, V> generator)
            {
            NullableConcurrentMapTestSuiteBuilder<K, V> result = new NullableConcurrentMapTestSuiteBuilder<>();
            result.usingGenerator(generator);
            return result;
            }

        protected SetTestSuiteBuilder<Entry<K, V>> createDerivedEntrySetSuite(TestSetGenerator<Entry<K, V>> entrySetGenerator)
            {
            return super.createDerivedEntrySetSuite(entrySetGenerator).withFeatures(CollectionFeature.SUPPORTS_ADD);
            }

        protected SetTestSuiteBuilder<K> createDerivedKeySetSuite(TestSetGenerator<K> keySetGenerator)
            {
            return super.createDerivedKeySetSuite(keySetGenerator).withFeatures(CollectionFeature.SUPPORTS_ADD);
            }
        }
    }
