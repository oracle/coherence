/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

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
 * Guava testlib SegmentedConcurrentMap tests.
 */
public final class SegmentedConcurrentMapTest
        extends TestCase
    {
    public static TestSuite suite()
        {
        return ConcurrentMapTestSuiteBuilder
                .using(generator())
                .named("SegmentedConcurrentMap")
                .withFeatures(
                        CollectionSize.ANY,
                        MapFeature.GENERAL_PURPOSE,
                        MapFeature.ALLOWS_ANY_NULL_QUERIES,
                        CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                        CollectionFeature.SUPPORTS_REMOVE,
                        CollectionFeature.NON_STANDARD_TOSTRING)
                .createTestSuite();
        }

    private static TestStringMapGenerator generator()
        {
        return new TestStringMapGenerator()
            {
            @Override
            protected Map<String, String> create(Entry<String, String>[] entries)
                {
                Map<String, String> map = new SegmentedConcurrentMap();
                for (Entry<String, String> entry : entries)
                    {
                    map.put(entry.getKey(), entry.getValue());
                    }
                return map;
                }
            };
        }
    }
