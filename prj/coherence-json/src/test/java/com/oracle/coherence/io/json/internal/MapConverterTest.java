/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.GenericType;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.reflect.VisibilityFilter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link MapConverter}.
 *
 * @author Aleks Seovic  2018.05.30
* @since 20.06
 */
class MapConverterTest
    {
    // ----- test configuration ---------------------------------------------

    @BeforeAll
    static void configure()
        {
        s_genson = new GensonBuilder()
                .useClassMetadata(true)
                .useClassMetadataWithStaticType(false)
                .useFields(true, VisibilityFilter.PRIVATE)
                .useMethods(false)
                .useIndentation(true)
                .withConverterFactory(MapConverter.Factory.INSTANCE)
                .create();
        }

    // ----- test cases -----------------------------------------------------

    @Test
    void testMapSerialization()
        {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        String json = s_genson.serialize(map);
        System.out.println(json);

        Map<String, Integer> map2 = s_genson.deserialize(json, MAP_GENERIC_TYPE);
        assertTrue(map2 instanceof HashMap);
        assertEquals(map, map2);
        }

    @Test
    void testSortedMapSerialization()
        {
        Map<String, Integer> map = new TreeMap<>();
        map.put("one", 1);
        map.put("two", 2);
        String json = s_genson.serialize(map);
        System.out.println(json);

        Map<String, Integer> map2 = s_genson.deserialize(json, MAP_GENERIC_TYPE);
        assertTrue(map2 instanceof TreeMap);
        assertEquals(map, map2);
        }

    @Test
    void testOrderedMapSerialization()
        {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("two", 2);
        map.put("one", 1);
        String json = s_genson.serialize(map);
        System.out.println(json);

        Map<String, Integer> map2 = s_genson.deserialize(json, MAP_GENERIC_TYPE);
        assertTrue(map2 instanceof LinkedHashMap);
        assertEquals(map, map2);
        }

    // ----- inner class: MapType -------------------------------------------

    public static class MapType
            extends GenericType<Map<String, Integer>>
        {
        }

    // ----- constants ------------------------------------------------------

    protected static final GenericType<Map<String, Integer>> MAP_GENERIC_TYPE = new MapType();

    protected static Genson s_genson;
    }
