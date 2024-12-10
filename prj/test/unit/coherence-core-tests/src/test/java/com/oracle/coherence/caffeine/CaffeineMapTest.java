/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.caffeine;

import com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Guava testlib map tests.
 */
public final class CaffeineMapTest
        extends TestCase
    {
    public static TestSuite suite()
        {
        return ConcurrentMapTestSuiteBuilder
                .using(generator())
                .named("Caffeine")
                .withFeatures(
                        CollectionSize.ANY,
                        MapFeature.GENERAL_PURPOSE,
                        MapFeature.ALLOWS_NULL_ENTRY_QUERIES,
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
                var cache = new CaffeineCache();
                for (var entry : entries)
                    {
                    cache.put(entry.getKey(), entry.getValue());
                    }
                return cache;
                }
            };
        }
    }
