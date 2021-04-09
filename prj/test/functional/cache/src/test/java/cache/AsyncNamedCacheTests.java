/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package cache;


import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleHolder;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.LongSum;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.NeverFilter;

import common.AbstractFunctionalTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;
import util.JacocoHelper;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for AsyncNamedCache.
 *
 * @author as  2015.01.21
 */
public class AsyncNamedCacheTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");
        AbstractFunctionalTest._startup();
        }

    @Test
    public void testInvoke()
            throws Exception
        {
        NamedCache<Integer, Integer> cache = getNamedCache();
        cache.put(2, 2);

        SimpleHolder<Integer> holder = new SimpleHolder<>();
        assertEquals(2, (int) cache.async().invoke(2, multiplier(2))
                .whenComplete((r, t) -> System.out.println("testInvoke: " + r))
                .whenComplete((r, t) -> holder.set(r))
                .get());

        assertEquals(4, (int) cache.get(2));
        assertEquals(2, (int) holder.get());
        }

    @Test(expected = ExecutionException.class)
    public void testInvokeWithException()
            throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(2, "two");

        cache.async().invoke(2, AsyncNamedCacheTests::error)
                .whenComplete((r, t) -> t.printStackTrace()).get();
        }

    @Test
    public void testInvokeAllWithKeySet()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("1", 1);
        expected.put("3", 9);

        assertEquals(expected, cache.async()
                .invokeAll(Arrays.asList("1", "3"), AsyncNamedCacheTests::square)
                .whenComplete((r, t) -> System.out.println("testInvokeAllWithKeySet: " + r))
                .get());
        }

    @Test
    public void testInvokeAllWithFilter()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("2", 4);
        expected.put("3", 9);

        assertEquals(expected, cache.async().invokeAll(GREATER_THAN_1, AsyncNamedCacheTests::square)
                .whenComplete((r, t) -> System.out.println("testInvokeAllWithFilter: " + r)).get());

        expected = cache.async().invokeAll(new NeverFilter(), AsyncNamedCacheTests::square)
                .whenComplete((r, t) -> System.out.println("testInvokeAllWithFilter: " + r)).get();
        assertEquals(0, expected.size());

        Set expected2 = cache.async().entrySet(new NeverFilter())
                .whenComplete((r, t) -> System.out.println("testInvokeAllWithFilter: " + r)).get();
        assertEquals(0, expected2.size());
        }

    // ---- query methods ---------------------------------------------------

    @Test
    public void testKeySet()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        assertEquals(cache.keySet(), cache.async().keySet().get());
        }

    @Test
    public void testKeySetWithFilter()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter filter = Filters.like(ValueExtractor.identity(), "A%");
        assertEquals(cache.keySet(filter), cache.async().keySet(filter).get());

        filter = Filters.like(ValueExtractor.identity(), "Z%");
        assertEquals(0, cache.async().keySet(filter).get().size());
        }

    @Test
    public void testKeySetStreaming()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Set<Integer> setResults = new HashSet<>();
        cache.async().keySet(setResults::add).get();
        assertEquals(2, setResults.size());
        assertTrue(setResults.contains(1));
        assertTrue(setResults.contains(2));

        setResults.clear();

        Filter filter = Filters.like(ValueExtractor.identity(), "A%");
        cache.async().keySet(filter, setResults::add).get();
        assertEquals(1, setResults.size());
        assertTrue(setResults.contains(1));
        }

    @Test(expected = ExecutionException.class)
    public void testKeySetWithException()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter filter = oTarget -> { throw new RuntimeException(); };
        cache.async().keySet(filter).get();
        }

    @Test
    public void testEntrySet()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        assertEquals(cache.entrySet(), cache.async().entrySet().get());
        }

    @Test
    public void testEntrySetWithFilter()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter filter = Filters.like(ValueExtractor.identity(), "A%");
        assertEquals(cache.entrySet(filter), cache.async().entrySet(filter).get());

        filter = Filters.like(ValueExtractor.identity(), "Z%");
        assertEquals(0, cache.async().entrySet(filter).get().size());
        }

    @Test
    public void testEntrySetWithComparator()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        assertEquals(cache.entrySet(AlwaysFilter.INSTANCE, null),
                     cache.async().entrySet(AlwaysFilter.INSTANCE, (Comparator) null).get());
        }

    @Test
    public void testEntrySetStreaming()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Set<String> setResults = new HashSet<>();
        cache.async().entrySet((key, value) -> setResults.add(value)).get();
        assertEquals(2, setResults.size());
        assertTrue(setResults.contains("Aleks"));
        assertTrue(setResults.contains("Marija"));

        setResults.clear();

        Filter filter = Filters.like(ValueExtractor.identity(), "A%");
        cache.async().entrySet(filter, entry -> setResults.add(entry.getValue())).get();
        assertEquals(1, setResults.size());
        assertTrue(setResults.contains("Aleks"));
        }

    @Test(expected = ExecutionException.class)
    public void testEntrySetWithException()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter filter = oTarget -> { throw new RuntimeException(); };
        cache.async().entrySet(filter).get();
        }

    @Test
    public void testValues()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        assertEquals(cache.values().size(), cache.async().values().get().size());
        assertTrue(cache.values().containsAll(cache.async().values().get()));
        }

    @Test
    public void testValuesWithFilter()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter filter = Filters.like(ValueExtractor.identity(), "A%");
        assertEquals(new ArrayList<>(cache.values(filter, null)),
                     new ArrayList<>(cache.async().values(filter, (Comparator<String>) null).get()));

        filter = Filters.like(ValueExtractor.identity(), "Z%");
        assertEquals(0, cache.async().values(filter).get().size());
        }

    @Test
    public void testValuesStreaming()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Set<String> setResults = new HashSet<>();
        cache.async().values(setResults::add).get();
        assertEquals(2, setResults.size());
        assertTrue(setResults.contains("Aleks"));
        assertTrue(setResults.contains("Marija"));

        setResults.clear();

        Filter filter = Filters.like(ValueExtractor.identity(), "A%");
        cache.async().values(filter, setResults::add).get();
        assertEquals(1, setResults.size());
        assertTrue(setResults.contains("Aleks"));
        }

    @Test(expected = ExecutionException.class)
    public void testValuesWithException()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter filter = oTarget -> { throw new RuntimeException(); };
        cache.async().values(filter).get();
        }

    // ---- Map methods -----------------------------------------------------

    @Test
    public void testSizeContainsClearIsEmpty()
            throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        assertEquals(0, (int) cache.async().size().get());
        assertTrue(cache.async().isEmpty().get());
        assertFalse(cache.async().containsKey(1).get());

        cache.put(1, "one");
        cache.put(2, "two");

        assertEquals(2, (int) cache.async().size().get());
        assertFalse(cache.async().isEmpty().get());
        assertTrue(cache.async().containsKey(1).get());

        cache.async().clear().get();
        assertEquals(0, (int) cache.async().size().get());
        assertTrue(cache.async().isEmpty().get());
        assertFalse(cache.async().containsKey(1).get());
        }
    
    @Test
    public void testGetOrDefaultWhenKeyMappedToValue() throws Exception
        {
        JacocoHelper.skipIfJacocoInstrumented();
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        assertThat(cache.async().getOrDefault("1", 100).get(), is(1));
        }

    @Test
    public void testGetOrDefaultWhenKeyMappedToNull() throws Exception
        {
        JacocoHelper.skipIfJacocoInstrumented();
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", null);
        assertThat(cache.async().getOrDefault("1", 100).get(), is(nullValue()));
        }

    @Test
    public void testGetOrDefaultWhenKeyNotPresent() throws Exception
        {
        JacocoHelper.skipIfJacocoInstrumented();
        NamedCache<String, Integer> cache = getNamedCache();
        cache.remove("1");
        assertThat(cache.async().getOrDefault("1", 100).get(), is(100));
        }

    @Test
    public void testGetOrDefault()
            throws Exception
        {
        JacocoHelper.skipIfJacocoInstrumented();
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(2, "two");

        cache.getOrDefault("one", "foo");
        assertEquals("one", cache.async().getOrDefault(1, "one")
                .whenComplete((r, t) -> System.out.println("testGetOrDefault: " + r))
                .get());
        assertEquals("two", cache.async().getOrDefault(2, "TWO")
                .whenComplete((r, t) -> System.out.println("testGetOrDefault: " + r))
                .get());
        }

    @Test
    public void testGetAllWhereAllKeysMatch()
            throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>        listKeys = Arrays.asList(2, 4, 6, 8);
        Map<Integer, String> mapCache = new HashMap<>(cache.getAll(listKeys));
        Map<Integer, String> mapAsync = new HashMap<>(cache.async().getAll(listKeys).get());

        assertThat(mapCache.size(), is(listKeys.size()));
        assertThat(mapAsync.size(), is(listKeys.size()));
        assertThat(mapCache, hasEntry(2, "value-2"));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(6, "value-6"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void testGetAllWhereNoKeysMatch()
            throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>        listKeys = Arrays.asList(22, 44, 66, 88);
        Map<Integer, String> mapCache = cache.getAll(listKeys);
        Map<Integer, String> mapAsync = cache.async().getAll(listKeys).get();

        assertThat(mapCache.isEmpty(), is(true));
        assertThat(mapAsync.isEmpty(), is(true));
        }

    @Test
    public void testGetAllWhereSomeKeysMatch()
            throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>        listKeys = Arrays.asList(22, 4, 66, 8);
        Map<Integer, String> mapCache = new HashMap<>(cache.getAll(listKeys));
        Map<Integer, String> mapAsync = new HashMap<>(cache.async().getAll(listKeys).get());

        assertThat(mapCache.size(), is(2));
        assertThat(mapAsync.size(), is(2));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void testGetAllWithConsumerWhereAllKeysMatch()
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>           listKeys = Arrays.asList(2, 4, 6, 8);
        Map<Integer, String>    mapCache = new HashMap<>(cache.getAll(listKeys));
        Map<Integer, String>    mapAsync = new HashMap<>();
        CompletableFuture<Void> future   = cache.async() .getAll(listKeys, entry ->
                mapAsync.put(entry.getKey(), entry.getValue()));

        future.join();

        assertThat(mapCache.size(), is(listKeys.size()));
        assertThat(mapAsync.size(), is(listKeys.size()));
        assertThat(mapCache, hasEntry(2, "value-2"));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(6, "value-6"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void testGetAlWithConsumerWhereNoKeysMatch()
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>           listKeys = Arrays.asList(22, 44, 66, 88);
        Map<Integer, String>    mapCache = new HashMap<>(cache.getAll(listKeys));
        Map<Integer, String>    mapAsync = new HashMap<>();
        CompletableFuture<Void> future   = cache.async() .getAll(listKeys, entry ->
                mapAsync.put(entry.getKey(), entry.getValue()));

        future.join();

        assertThat(mapCache.isEmpty(), is(true));
        assertThat(mapAsync.isEmpty(), is(true));
        }

    @Test
    public void testGetAllWithConsumerWhereSomeKeysMatch()
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>           listKeys = Arrays.asList(22, 4, 66, 8);
        Map<Integer, String>    mapCache = new HashMap<>(cache.getAll(listKeys));
        Map<Integer, String>    mapAsync = new HashMap<>();
        CompletableFuture<Void> future   = cache.async() .getAll(listKeys, entry ->
                mapAsync.put(entry.getKey(), entry.getValue()));

        future.join();

        assertThat(mapCache.size(), is(2));
        assertThat(mapAsync.size(), is(2));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void testGetAllWithBiConsumerWhereAllKeysMatch()
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>           listKeys = Arrays.asList(2, 4, 6, 8);
        Map<Integer, String>    mapCache = new HashMap<>(cache.getAll(listKeys));
        Map<Integer, String>    mapAsync = new HashMap<>();
        CompletableFuture<Void> future   = cache.async() .getAll(listKeys, mapAsync::put);

        future.join();

        assertThat(mapCache.size(), is(listKeys.size()));
        assertThat(mapAsync.size(), is(listKeys.size()));
        assertThat(mapCache, hasEntry(2, "value-2"));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(6, "value-6"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void testGetAlWithBiConsumerWhereNoKeysMatch()
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>           listKeys = Arrays.asList(22, 44, 66, 88);
        Map<Integer, String>    mapCache = new HashMap<>(cache.getAll(listKeys));
        Map<Integer, String>    mapAsync = new HashMap<>();
        CompletableFuture<Void> future   = cache.async() .getAll(listKeys, mapAsync::put);

        future.join();

        assertThat(mapCache.isEmpty(), is(true));
        assertThat(mapAsync.isEmpty(), is(true));
        }

    @Test
    public void testGetAllWithBiConsumerWhereSomeKeysMatch()
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.clear();
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, "value-" + i);
            }

        List<Integer>           listKeys = Arrays.asList(22, 4, 66, 8);
        Map<Integer, String>    mapCache = new HashMap<>(cache.getAll(listKeys));
        Map<Integer, String>    mapAsync = new HashMap<>();
        CompletableFuture<Void> future   = cache.async() .getAll(listKeys, mapAsync::put);

        future.join();

        assertThat(mapCache.size(), is(2));
        assertThat(mapAsync.size(), is(2));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void testPutIfAbsent()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        assertNull(cache.async().putIfAbsent("1", 1).get());
        assertEquals(1, (int) cache.async().putIfAbsent("1", 100).get());
        cache.put("2", null);
        assertNull(cache.async().putIfAbsent("2", 2).get());
        assertEquals(2, cache.size());
        assertEquals(2, (int) cache.get("2"));
        }

    @Test
    public void testRemove()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);

        assertFalse(cache.async().remove("1", 2).get());
        assertTrue(cache.async().remove("2", 2).get());

        assertEquals(1, cache.size());
        assertTrue(cache.containsKey("1"));
        assertFalse(cache.containsKey("2"));
        }

    @Test
    public void testReplace()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", null);

        assertEquals(1, (int) cache.async().replace("1", 100).get());
        assertNull(cache.async().replace("2", 200).get());
        assertNull(cache.async().replace("3", 300).get());

        assertEquals(2, cache.size());
        assertFalse(cache.containsKey("3"));
        }

    @Test
    public void testReplaceWithValueCheck()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", null);
        cache.put("3", null);

        assertTrue(cache.async().replace("1", 1, 100).get());
        assertFalse(cache.async().replace("2", 2, 200).get());
        assertTrue(cache.async().replace("3", null, 300).get());
        assertFalse(cache.async().replace("4", 4, 400).get());

        assertEquals(100, (int) cache.get("1"));
        assertNull(cache.get("2"));
        assertEquals(300, (int) cache.get("3"));
        assertFalse(cache.containsKey("4"));
        }

    @Test
    public void testComputeIfAbsent()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("five", 5);
        assertEquals(1, (int) cache.async().computeIfAbsent("1", Integer::parseInt).get());
        assertEquals(5, (int) cache.async().computeIfAbsent("five", Integer::parseInt).get());
        assertNull(cache.async().computeIfAbsent("null", (k) -> null).get());
        }

    @Test
    public void testComputeIfPresent()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);

        assertEquals(2, (int) cache.async().computeIfPresent("1", (k, v) -> v + v).get());
        assertEquals(4, (int) cache.async().computeIfPresent("2", (k, v) -> v * v).get());
        assertNull(cache.async().computeIfPresent("1", (k, v) -> null).get());
        assertNull(cache.async().computeIfPresent("3", (k, v) -> v * v).get());

        assertEquals(4, (int) cache.get("2"));
        assertEquals(1, cache.size());
        assertFalse(cache.containsKey("1"));
        }

    @Test
    public void testCompute()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);

        assertEquals(2, (int) cache.async().compute("1", (k, v) -> v + v).get());
        assertNull(cache.async().compute("2", (k, v) -> null).get());
        assertFalse(cache.containsKey("2"));
        }

    @Test
    public void testMerge()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);

        assertEquals(2, (int) cache.async().merge("1", 1, (v1, v2) -> v1 + v2).get());
        assertEquals(3, (int) cache.async().merge("2", 1, (v1, v2) -> v1 + v2).get());
        assertEquals(1, (int) cache.async().merge("3", 1, (v1, v2) -> v1 + v2).get());
        assertNull(cache.async().merge("1", 1, (v1, v2) -> null).get());

        assertFalse(cache.containsKey("1"));
        assertTrue(cache.containsKey("3"));
        }

    @Test
    public void testReplaceAll()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        cache.async().replaceAll((k, v) -> v * v).get();
        assertEquals(1, (int) cache.get("1"));
        assertEquals(4, (int) cache.get("2"));
        assertEquals(9, (int) cache.get("3"));
        }

    @Test
    public void testReplaceAllWithKeySet()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        cache.async().replaceAll(Arrays.asList("1", "3"), (k, v) -> v * v).get();
        assertEquals(1, (int) cache.get("1"));
        assertEquals(2, (int) cache.get("2"));
        assertEquals(9, (int) cache.get("3"));
        }

    @Test
    public void testReplaceAllWithFilter()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        cache.async().replaceAll(GREATER_THAN_2, (k, v) -> v * v).get();
        assertEquals(1, (int) cache.get("1"));
        assertEquals(2, (int) cache.get("2"));
        assertEquals(9, (int) cache.get("3"));
        }

    @Test
    public void testAggregateSet()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Count count = new Count();

        cache.async().aggregate(NullImplementation.getSet(), count)
                .thenAccept(x -> assertEquals((long) 0, (long) x));

        cache.async().aggregate(cache.keySet(), new LongSum(IdentityExtractor.INSTANCE))
                .thenAccept(x -> assertEquals(6l, x));
        }

    @Test
    public void testAggregateFilter() throws Exception
        {
        NamedCache cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Count count = new Count();

        cache.async().aggregate((Filter) null, count)
                .thenAccept(x -> assertEquals(0, x));

        cache.async().aggregate(AlwaysFilter.INSTANCE, count)
                .thenAccept(x -> assertEquals(3, x));

        cache.async().aggregate(AlwaysFilter.INSTANCE, new LongSum(IdentityExtractor.INSTANCE))
                    .thenAccept(x -> assertEquals(6l, x));

        cache.async().aggregate(
                Filters.greater(x -> ((Integer) x), 1), new LongSum(IdentityExtractor.INSTANCE))
                .thenAccept(x -> assertEquals(5L, x));
        }

    /**
     * Test of the {@link CompositeAggregator} aggregator.
     */
    @Test
    public void testComposite() throws Exception
        {
        NamedCache cache = getNamedCache();
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i <= 10; ++i)
            {
            map.put(i, i);
            }
        cache.putAll(map);
        Set setKeys = cache.keySet();

        EntryAggregator composite = CompositeAggregator.createInstance(new
                EntryAggregator[]{new Count(), new LongSum(IdentityExtractor.INSTANCE)});

        cache.async().aggregate(setKeys, composite).thenAccept(x ->
            {
            List<Number> oResult = (List<Number>) x;
            assertEquals(oResult.get(0), 10);
            assertEquals(oResult.get(1), 55L);
            });
        }

    // ---- helpers ---------------------------------------------------------

    public static Integer square(InvocableMap.Entry<?, Integer> entry)
        {
        return entry.getValue() * entry.getValue();
        }

    public static NamedCache.EntryProcessor<Integer, Integer, Integer> multiplier(int n)
        {
        return (e) -> e.setValue(e.getValue() * n);
        }

    @SuppressWarnings("NumericOverflow")
    public static String error(InvocableMap.Entry<Integer, String> e)
        {
        return e.setValue(Integer.toString(5 / 0));
        }

    protected <K, V> NamedCache<K, V> getNamedCache()
        {
        NamedCache<K, V> cache = getFactory().ensureTypedCache("dist-test",
                                                               null,
                                                               withoutTypeChecking());
        cache.clear();
        return cache;
        }

    protected static final GreaterFilter GREATER_THAN_1 = new GreaterFilter<>(IdentityExtractor.INSTANCE(), 1);
    protected static final GreaterFilter GREATER_THAN_2 = new GreaterFilter<>(IdentityExtractor.INSTANCE(), 2);
    }
