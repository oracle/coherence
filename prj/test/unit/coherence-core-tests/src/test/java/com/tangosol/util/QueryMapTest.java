/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.comparator.InverseComparator;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.GreaterFilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for QueryMap.
 *
 * @author as  2014.08.05
 */
@SuppressWarnings("unchecked")
public class QueryMapTest
    {
    protected QueryMap<String, Integer> getMap()
        {
        return new WrapperNamedCache<>(new HashMap<>(), "test");
        }

    @Test
    public void testValues()
        {
        QueryMap<String, Integer> map = getMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        Collection<Integer> values = map.values(GREATER_THAN_1);
        assertEquals(2, values.size());
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        }

    @Test
    public void testValuesWithNullComparator()
        {
        QueryMap<String, Integer> map = getMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        Collection<Integer> values = map.values(GREATER_THAN_1, null);
        assertEquals(2, values.size());
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        }

    @Test
    public void testValuesWithComparator()
        {
        QueryMap<String, Integer> map = getMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        Collection<Integer> values = map.values(GREATER_THAN_1, new InverseComparator());
        assertEquals(2, values.size());
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        }

    @Test
    public void testForEach()
        {
        QueryMap<String, Integer> map = getMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        Map<String, Integer> squares = new HashMap<>();
        int[] c = new int[1];

        map.forEach((k, v) -> { squares.put(k, v * v); c[0]++; });

        assertEquals(3, squares.size());
        assertEquals(3, c[0]);
        assertEquals(1, (int) squares.get("1"));
        assertEquals(4, (int) squares.get("2"));
        assertEquals(9, (int) squares.get("3"));
        }

    @Test
    public void testForEachWithFilter()
        {
        QueryMap<String, Integer> map = getMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        Map<String, Integer> squares = new HashMap<>();
        int[] c = new int[1];

        map.forEach(GREATER_THAN_2, (k, v) -> { squares.put(k, v * v); c[0]++; });

        assertEquals(1, squares.size());
        assertEquals(1, c[0]);
        assertEquals(9, (int) squares.get("3"));
        }

    public static final GreaterFilter GREATER_THAN_1 = new GreaterFilter<>(IdentityExtractor.INSTANCE, 1);
    public static final GreaterFilter GREATER_THAN_2 = new GreaterFilter<>(IdentityExtractor.INSTANCE, 2);
    }
