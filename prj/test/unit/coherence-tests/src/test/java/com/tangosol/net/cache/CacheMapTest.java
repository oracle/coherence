/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for CacheMap.
 *
 * @author as  2014.08.05
 */
public class CacheMapTest
    {
    protected CacheMap<String, Integer> getMap()
        {
        return new WrapperNamedCache<>(new HashMap<>(), "test");
        }

    @Test
    public void testForEachWithKeySet()
        {
        CacheMap<String, Integer> map = getMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        Map<String, Integer> squares = new HashMap<>();
        map.forEach(Arrays.asList("1", "3"), (k, v) -> squares.put(k, v * v));
        assertEquals(2, squares.size());
        assertEquals(1, (int) squares.get("1"));
        assertEquals(9, (int) squares.get("3"));
        }
    }
