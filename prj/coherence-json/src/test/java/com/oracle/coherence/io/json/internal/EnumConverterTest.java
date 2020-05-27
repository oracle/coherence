/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.reflect.VisibilityFilter;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link EnumConverter}.
 *
 * @since 14.1.2
 */
class EnumConverterTest
    {
    // ----- test setup -----------------------------------------------------

    @BeforeAll
    static void configure()
        {
        s_genson = new GensonBuilder()
                .useClassMetadata(true)
                .useClassMetadataWithStaticType(false)
                .useFields(true, VisibilityFilter.PRIVATE)
                .useMethods(false)
                .useIndentation(false)
                .withConverterFactory(EnumConverter.Factory.INSTANCE)
                .create();
        }

    // ----- test cases -----------------------------------------------------

    @Test
    void testDeserializationOfEnumInMap()
        {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("minutes", TimeUnit.MINUTES);
        expected.put("seconds", TimeUnit.SECONDS);
        Map result = s_genson.deserialize(s_genson.serialize(expected), LinkedHashMap.class);
        assertEquals(expected, result);
        }

    // ----- data members ---------------------------------------------------

    protected static Genson s_genson;
    }
