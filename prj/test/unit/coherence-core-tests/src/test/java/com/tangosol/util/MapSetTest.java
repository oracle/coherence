/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link MapSet}.
 */
public final class MapSetTest
        extends TestCase
    {
    public static TestSuite suite() throws NoSuchMethodException
        {
        return SetTestSuiteBuilder
                .using(generator())
                .named("MapSet")
                .withFeatures(
                        CollectionSize.ANY,
                        CollectionFeature.ALLOWS_NULL_VALUES,
                        CollectionFeature.SERIALIZABLE,
                        CollectionFeature.GENERAL_PURPOSE,
                        CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
                .createTestSuite();
        }

    private static TestStringSetGenerator generator()
        {
        return new TestStringSetGenerator()
            {
            protected Set<String> create(String[] elements)
                {
                Set set = new MapSet();
                for (int i = 0; i < elements.length; i++)
                    {
                    set.add(elements[i]);
                    }
                return set;
                }
            };
        }
    }
