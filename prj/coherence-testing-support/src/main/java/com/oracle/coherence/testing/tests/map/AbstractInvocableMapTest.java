/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.tests.map;

import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.GreaterFilter;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for InvocableMap.
 *
 * @author as  2014.06.13
 */
@SuppressWarnings("unchecked")
public abstract class AbstractInvocableMapTest
    {
    protected InvocableMap getInvocableMap()
        {
        return new WrapperNamedCache(new HashMap(), "test");
        }

    @Test
    public void testInvoke()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        EntryProcessor<String, Integer, Integer> processor =
            (e) -> e.getValue() * e.getValue();

        assertEquals(4, (int) map.invoke("2", processor));
        }

    @Test
    public void testInvokeAllWithKeySet()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("1", 1);
        expected.put("3", 9);

        assertEquals(expected, map.invokeAll(Arrays.asList("1", "3"), AbstractInvocableMapTest::square));
        }

    @Test
    public void testInvokeAllWithFilter()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("2", 4);
        expected.put("3", 9);

        assertEquals(expected, map.invokeAll(GREATER_THAN_1, AbstractInvocableMapTest::square));
        }

    // ---- Default Map methods ---------------------------------------------

    @Test
    public void testGetOrDefaultWhenKeyMappedToValue()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        assertThat(map.getOrDefault("1", 100), is(1));
        }

    @Test
    public void testGetOrDefaultWhenKeyMappedToNull()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", null);
        assertThat(map.getOrDefault("1", 100), is(nullValue()));
        }

    @Test
    public void testGetOrDefaultWhenKeyNotPresent()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.remove("1");
        assertThat(map.getOrDefault("1", 100), is(100));
        }

    @Test
    public void testPutIfAbsent()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        Integer result = map.putIfAbsent("1", 1);
        assertNull(result);
        assertEquals(1, (int) map.putIfAbsent("1", 100));
        map.put("2", null);
        assertNull(map.putIfAbsent("2", 2));
        assertEquals(2, map.size());
        assertEquals(2, (int) map.get("2"));
        }

    @Test
    public void testRemove()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);

        assertFalse(map.remove("1", 2));
        assertTrue(map.remove("2", 2));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("1"));
        assertFalse(map.containsKey("2"));
        }

    @Test
    public void testReplace()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", null);

        assertEquals(1, (int) map.replace("1", 100));
        assertNull(map.replace("2", 200));
        assertNull(map.replace("3", 300));
        assertEquals(2, map.size());
        assertEquals(200, (int) map.get("2"));
        assertFalse(map.containsKey("3"));
        }

    @Test
    public void testReplaceWithValueCheck()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", null);
        map.put("3", null);

        assertTrue(map.replace("1", 1, 100));
        assertFalse(map.replace("2", 2, 200));
        assertTrue(map.replace("3", null, 300));
        assertFalse(map.replace("4", 4, 400));
        assertEquals(100, (int) map.get("1"));
        assertNull(map.get("2"));
        assertEquals(300, (int) map.get("3"));
        assertFalse(map.containsKey("4"));
        }

    @Test
    public void testComputeIfAbsent()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("five", 5);
        assertEquals(1, (int) map.computeIfAbsent("1", Integer::parseInt));
        assertEquals(5, (int) map.computeIfAbsent("five", Integer::parseInt));
        assertNull(map.computeIfAbsent("null", (k) -> null));
        }

    @Test
    public void testComputeIfPresent()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);

        assertEquals(2, (int) map.computeIfPresent("1", (k, v) -> v + v));
        assertEquals(4, (int) map.computeIfPresent("2", (k, v) -> v * v));
        assertEquals(4, (int) map.get("2"));
        assertNull(map.computeIfPresent("1", (k, v) -> null));
        assertEquals(1, map.size());
        assertFalse(map.containsKey("1"));
        assertNull(map.computeIfPresent("3", (k, v) -> v * v));
        }

    @Test
    public void testCompute()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);

        assertEquals(2, (int) map.compute("1", (k, v) -> v + v));
        assertNull(map.compute("2", (k, v) -> null));
        assertFalse(map.containsKey("2"));

        assertEquals(3, (int) map.compute("3", (k, v) -> Integer.parseInt(k)));
        assertTrue(map.containsKey("3"));
        }

    @Test
    public void testMerge()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);

        assertEquals(2, (int) map.merge("1", 1, (v1, v2) -> v1 + v2));
        assertEquals(3, (int) map.merge("2", 1, (v1, v2) -> v1 + v2));
        assertEquals(1, (int) map.merge("3", 1, (v1, v2) -> v1 + v2));
        assertNull(map.merge("1", 1, (v1, v2) -> null));
        assertFalse(map.containsKey("1"));
        assertTrue(map.containsKey("3"));
        }

    @Test
    public void testReplaceAll()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        map.replaceAll((k, v) -> v * v);
        assertEquals(1, (int) map.get("1"));
        assertEquals(4, (int) map.get("2"));
        assertEquals(9, (int) map.get("3"));
        }

    @Test
    public void testReplaceAllWithKeySet()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        map.replaceAll(Arrays.asList("1", "3"), (k, v) -> v * v);
        assertEquals(1, (int) map.get("1"));
        assertEquals(2, (int) map.get("2"));
        assertEquals(9, (int) map.get("3"));

        // make sure that the missing keys can be replaced as well
        map.replaceAll(Arrays.asList("5", "6"), (k, v) -> Integer.parseInt(k));
        assertEquals(5, (int) map.get("5"));
        assertEquals(6, (int) map.get("6"));
        }

    @Test
    public void testReplaceAllWithFilter()
        {
        InvocableMap<String, Integer> map = getInvocableMap();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        map.replaceAll(GREATER_THAN_2, (k, v) -> v * v);
        assertEquals(1, (int) map.get("1"));
        assertEquals(2, (int) map.get("2"));
        assertEquals(9, (int) map.get("3"));
        }

    public static Integer square(InvocableMap.Entry<?, Integer> entry)
        {
        return entry.getValue() * entry.getValue();
        }

    public static final GreaterFilter GREATER_THAN_1 = new GreaterFilter<>(IdentityExtractor.INSTANCE, 1);
    public static final GreaterFilter GREATER_THAN_2 = new GreaterFilter<>(IdentityExtractor.INSTANCE, 2);
    }
