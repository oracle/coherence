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

import com.tangosol.util.ObservableMap;
import com.tangosol.util.listener.SimpleMapListener;

import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link ObservableConcurrentMap}.
 */
public final class ObservableConcurrentMapTest
        extends TestCase
    {
    public static TestSuite suite() throws NoSuchMethodException
        {
        return ObservableConcurrentMapTestSuiteBuilder
                .using(generator())
                .named("ObservableConcurrentMap")
                .withFeatures(
                        CollectionSize.ANY,
                        MapFeature.GENERAL_PURPOSE,
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
                ObservableMap<String, String> map = new ObservableConcurrentMap<>();

                for (var entry : entries)
                    {
                    map.put(entry.getKey(), entry.getValue());
                    }

                // register listener *after* the map is populated with test data,
                // to avoid printing initial population events
                map.addMapListener(new SimpleMapListener<>().addEventHandler(System.out::println));

                return map;
                }
            };
        }

    private static class ObservableConcurrentMapTestSuiteBuilder<K, V>
                extends ConcurrentMapTestSuiteBuilder<K, V>
        {
        public static <K, V> ObservableConcurrentMapTestSuiteBuilder<K, V> using(TestMapGenerator<K, V> generator)
            {
            ObservableConcurrentMapTestSuiteBuilder<K, V> result = new ObservableConcurrentMapTestSuiteBuilder<>();
            result.usingGenerator(generator);
            return result;
            }

        protected SetTestSuiteBuilder<Entry<K, V>> createDerivedEntrySetSuite(TestSetGenerator<Entry<K, V>> entrySetGenerator)
            {
            return super.createDerivedEntrySetSuite(entrySetGenerator).withFeatures(CollectionFeature.SUPPORTS_ADD);
            }
        }
    }
