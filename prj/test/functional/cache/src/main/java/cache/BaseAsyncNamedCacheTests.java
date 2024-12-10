/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.testing.util.JacocoHelper;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.AsyncNamedMap;
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
import org.junit.Test;

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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for AsyncNamedCache.
 *
 * @author as  2015.01.21
 */
@SuppressWarnings({"CallToPrintStackTrace", "DuplicatedCode"})
public abstract class BaseAsyncNamedCacheTests
    {
    /**
     * Create a {@link BaseAsyncNamedCacheTests} instance.
     *
     * @param sCacheName  the cache name to test (should map to a valid name in the coherence-cache-config.xml file
     *                    in this module's resources/ folder).
     * @param options     the {@link AsyncNamedMap.Option options} to use to create the async maps to test
     */
    protected BaseAsyncNamedCacheTests(String sCacheName, AsyncNamedMap.Option... options)
        {
        f_sCacheName = sCacheName;
        f_options    = options;
        }

    /**
     * Return the cache used by the tests.
     * <p/>
     * This should be the plain {@link NamedCache} not an
     * {@link com.tangosol.net.AsyncNamedCache}.
     *
     * @param <K> the cache key type
     * @param <V> the cache value type
     *
     * @return the cache used by the tests
     */
    protected  <K, V> NamedCache<K, V> getNamedCache()
        {
        return getNamedCache(f_sCacheName);
        }

    /**
     * Return the cache used by the tests.
     * <p/>
     * This should be the plain {@link NamedCache} not an
     * {@link com.tangosol.net.AsyncNamedCache}.
     *
     * @param <K> the cache key type
     * @param <V> the cache value type
     *
     * @return the cache used by the tests
     */
    protected abstract  <K, V> NamedCache<K, V> getNamedCache(String sCacheName);

    @Test
    public void shouldCallInvoke()
            throws Exception
        {
        NamedCache<Integer, Integer> cache = getNamedCache();
        cache.put(2, 2);

        SimpleHolder<Integer> holder = new SimpleHolder<>();
        assertEquals(2, (int) cache.async(f_options).invoke(2, multiplier(2))
                .whenComplete((r, t) -> System.out.println("testInvoke: " + r))
                .whenComplete((r, t) -> holder.set(r))
                .get(1, TimeUnit.MINUTES));

        assertEquals(4, (int) cache.get(2));
        assertEquals(2, (int) holder.get());
        }

    @Test(expected = ExecutionException.class)
    public void shouldCallInvokeWithException()
            throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(2, "two");

        cache.async(f_options).invoke(2, BaseAsyncNamedCacheTests::error)
                .whenComplete((r, t) -> t.printStackTrace()).get(1, TimeUnit.MINUTES);
        }

    @Test
    public void shouldCallInvokeAllWithKeySet()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("1", 1);
        expected.put("3", 9);

        assertEquals(expected, cache.async(f_options)
                .invokeAll(Arrays.asList("1", "3"), BaseAsyncNamedCacheTests::square)
                .whenComplete((r, t) -> System.out.println("testInvokeAllWithKeySet: " + r))
                .get(1, TimeUnit.MINUTES));
        }

    @Test
    public void shouldCallInvokeAllWithFilter()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("2", 4);
        expected.put("3", 9);

        assertEquals(expected, cache.async(f_options).invokeAll(GREATER_THAN_1, BaseAsyncNamedCacheTests::square)
                .whenComplete((r, t) -> System.out.println("testInvokeAllWithFilter: " + r)).get(1, TimeUnit.MINUTES));

        expected = cache.async(f_options).invokeAll(NeverFilter.INSTANCE(), BaseAsyncNamedCacheTests::square)
                .whenComplete((r, t) -> System.out.println("testInvokeAllWithFilter: " + r)).get(1, TimeUnit.MINUTES);
        assertEquals(0, expected.size());

        Set<?> expected2 = cache.async(f_options).entrySet(NeverFilter.INSTANCE())
                .whenComplete((r, t) -> System.out.println("testInvokeAllWithFilter: " + r)).get(1, TimeUnit.MINUTES);
        assertEquals(0, expected2.size());
        }

    // ---- query methods ---------------------------------------------------

    @Test
    public void shouldCallKeySet()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        assertEquals(cache.keySet(), cache.async(f_options).keySet().get(1, TimeUnit.MINUTES));
        }

    @Test
    public void shouldCallKeySetWithFilter()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter<?> filter = Filters.like(ValueExtractor.identity(), "A%");
        assertEquals(cache.keySet(filter), cache.async(f_options).keySet(filter).get(1, TimeUnit.MINUTES));

        filter = Filters.like(ValueExtractor.identity(), "Z%");
        assertEquals(0, cache.async(f_options).keySet(filter).get(1, TimeUnit.MINUTES).size());
        }

    @Test
    public void shouldStreamKeySet()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Set<Integer> setResults = new HashSet<>();
        cache.async(f_options).keySet(setResults::add).get(1, TimeUnit.MINUTES);
        assertEquals(2, setResults.size());
        assertTrue(setResults.contains(1));
        assertTrue(setResults.contains(2));

        setResults.clear();

        Filter<?> filter = Filters.like(ValueExtractor.identity(), "A%");
        cache.async(f_options).keySet(filter, setResults::add).get(1, TimeUnit.MINUTES);
        assertEquals(1, setResults.size());
        assertTrue(setResults.contains(1));
        }

    @Test(expected = ExecutionException.class)
    public void shouldCallKeySetWithException()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter<?> filter = oTarget -> { throw new RuntimeException(); };
        cache.async(f_options).keySet(filter).get(1, TimeUnit.MINUTES);
        }

    @Test
    public void shouldCallEntrySet()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        assertEquals(cache.entrySet(), cache.async(f_options).entrySet().get(1, TimeUnit.MINUTES));
        }

    @Test
    public void shouldCallEntrySetWithFilter()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter<?> filter = Filters.like(ValueExtractor.identity(), "A%");
        assertThat(cache.async(f_options).entrySet(filter).get(1, TimeUnit.MINUTES), is(cache.entrySet(filter)));

        filter = Filters.like(ValueExtractor.identity(), "Z%");
        assertEquals(0, cache.async(f_options).entrySet(filter).get(1, TimeUnit.MINUTES).size());
        }

    @Test
    public void shouldCallEntrySetWithComparator()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        assertEquals(cache.entrySet(AlwaysFilter.INSTANCE, null),
                     cache.async(f_options).entrySet(AlwaysFilter.INSTANCE, (Comparator<?>) null).get(1, TimeUnit.MINUTES));
        }

    @Test
    public void shouldStreamEntrySet()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Set<String> setResults = new HashSet<>();
        cache.async(f_options).entrySet((key, value) -> setResults.add(value)).get(1, TimeUnit.MINUTES);
        assertEquals(2, setResults.size());
        assertTrue(setResults.contains("Aleks"));
        assertTrue(setResults.contains("Marija"));

        setResults.clear();

        Filter<?> filter = Filters.like(ValueExtractor.identity(), "A%");
        cache.async(f_options).entrySet(filter, entry -> setResults.add(entry.getValue())).get(1, TimeUnit.MINUTES);
        assertEquals(1, setResults.size());
        assertTrue(setResults.contains("Aleks"));
        }

    @Test(expected = ExecutionException.class)
    public void shouldCallEntrySetWithException()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter<?> filter = oTarget -> { throw new RuntimeException(); };
        cache.async(f_options).entrySet(filter).get(1, TimeUnit.MINUTES);
        }

    @Test
    public void shouldCallValues()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        assertEquals(cache.values().size(), cache.async(f_options).values().get(1, TimeUnit.MINUTES).size());
        assertTrue(cache.values().containsAll(cache.async(f_options).values().get(1, TimeUnit.MINUTES)));
        }

    @Test
    public void shouldCallValuesWithFilter()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter<?> filter = Filters.like(ValueExtractor.identity(), "A%");
        assertEquals(new ArrayList<>(cache.values(filter, null)),
                     new ArrayList<>(cache.async(f_options).values(filter, (Comparator<String>) null).get(1, TimeUnit.MINUTES)));

        filter = Filters.like(ValueExtractor.identity(), "Z%");
        assertEquals(0, cache.async(f_options).values(filter).get(1, TimeUnit.MINUTES).size());
        }

    @Test
    public void shouldStreamValues()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Set<String> setResults = new HashSet<>();
        cache.async(f_options).values(setResults::add).get(1, TimeUnit.MINUTES);
        assertEquals(2, setResults.size());
        assertTrue(setResults.contains("Aleks"));
        assertTrue(setResults.contains("Marija"));

        setResults.clear();

        Filter<?> filter = Filters.like(ValueExtractor.identity(), "A%");
        cache.async(f_options).values(filter, setResults::add).get(1, TimeUnit.MINUTES);
        assertEquals(1, setResults.size());
        assertTrue(setResults.contains("Aleks"));
        }

    @Test(expected = ExecutionException.class)
    public void shouldCallValuesWithException()
        throws Exception
        {
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(1, "Aleks");
        cache.put(2, "Marija");

        Filter<?> filter = oTarget -> { throw new RuntimeException(); };
        cache.async(f_options).values(filter).get(1, TimeUnit.MINUTES);
        }

    // ---- Map methods -----------------------------------------------------

    @Test
    public void shouldCallClear() throws Exception
        {
        NamedCache<Integer, String>      cache = getNamedCache();
        AsyncNamedCache<Integer, String> async = cache.async(f_options);

        assertThat(async.size().get(1, TimeUnit.MINUTES), is(0));
        assertThat(async.isEmpty().get(1, TimeUnit.MINUTES), is(true));

        cache.put(1, "one");
        cache.put(2, "two");
        assertThat(async.size().get(1, TimeUnit.MINUTES), is(2));
        assertThat(async.isEmpty().get(1, TimeUnit.MINUTES), is(false));

        async.clear().get(1, TimeUnit.MINUTES);
        assertThat(cache.size(), is(0));
        assertThat(async.size().get(1, TimeUnit.MINUTES), is(0));
        assertThat(async.isEmpty().get(1, TimeUnit.MINUTES), is(true));
        }

    @Test
    public void shouldCallContainsKey()
            throws Exception
        {
        NamedCache<Integer, String>      cache = getNamedCache();
        AsyncNamedCache<Integer, String> async = cache.async(f_options);

        assertThat(async.containsKey(1).get(1, TimeUnit.MINUTES), is(false));
        cache.put(1, "one");
        assertThat(async.containsKey(1).get(1, TimeUnit.MINUTES), is(true));
        cache.remove(1);
        assertThat(async.containsKey(1).get(1, TimeUnit.MINUTES), is(false));
        }

    @Test
    public void shouldCallGetOrDefaultWhenKeyMappedToValue() throws Exception
        {
        JacocoHelper.skipIfJacocoInstrumented();
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        assertThat(cache.async(f_options).getOrDefault("1", 100).get(1, TimeUnit.MINUTES), is(1));
        }

    @Test
    public void shouldCallGetOrDefaultWhenKeyMappedToNull() throws Exception
        {
        JacocoHelper.skipIfJacocoInstrumented();
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", null);
        assertThat(cache.async(f_options).getOrDefault("1", 100).get(1, TimeUnit.MINUTES), is(nullValue()));
        }

    @Test
    public void shouldCallGetOrDefaultWhenKeyNotPresent() throws Exception
        {
        JacocoHelper.skipIfJacocoInstrumented();
        NamedCache<String, Integer> cache = getNamedCache();
        cache.remove("1");
        assertThat(cache.async(f_options).getOrDefault("1", 100).get(1, TimeUnit.MINUTES), is(100));
        }

    @Test
    public void shouldCallGetOrDefault()
            throws Exception
        {
        JacocoHelper.skipIfJacocoInstrumented();
        NamedCache<Integer, String> cache = getNamedCache();
        cache.put(2, "two");

        assertEquals("one", cache.async(f_options).getOrDefault(1, "one")
                .whenComplete((r, t) -> System.out.println("testGetOrDefault: " + r))
                .get(1, TimeUnit.MINUTES));
        assertEquals("two", cache.async(f_options).getOrDefault(2, "TWO")
                .whenComplete((r, t) -> System.out.println("testGetOrDefault: " + r))
                .get(1, TimeUnit.MINUTES));
        }

    @Test
    public void shouldCallGetAllWhereAllKeysMatch()
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
        Map<Integer, String> mapAsync = new HashMap<>(cache.async(f_options).getAll(listKeys).get(1, TimeUnit.MINUTES));

        assertThat(mapCache.size(), is(listKeys.size()));
        assertThat(mapAsync.size(), is(listKeys.size()));
        assertThat(mapCache, hasEntry(2, "value-2"));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(6, "value-6"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void shouldCallGetAllWhereNoKeysMatch()
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
        Map<Integer, String> mapAsync = cache.async(f_options).getAll(listKeys).get(1, TimeUnit.MINUTES);

        assertThat(mapCache.isEmpty(), is(true));
        assertThat(mapAsync.isEmpty(), is(true));
        }

    @Test
    public void shouldCallGetAllWhereSomeKeysMatch()
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
        Map<Integer, String> mapAsync = new HashMap<>(cache.async(f_options).getAll(listKeys).get(1, TimeUnit.MINUTES));

        assertThat(mapCache.size(), is(2));
        assertThat(mapAsync.size(), is(2));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void shouldCallGetAllWithConsumerWhereAllKeysMatch()
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
        CompletableFuture<Void> future   = cache.async(f_options) .getAll(listKeys, entry ->
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
    public void shouldCallGetAlWithConsumerWhereNoKeysMatch()
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
        CompletableFuture<Void> future   = cache.async(f_options) .getAll(listKeys, entry ->
                mapAsync.put(entry.getKey(), entry.getValue()));

        future.join();

        assertThat(mapCache.isEmpty(), is(true));
        assertThat(mapAsync.isEmpty(), is(true));
        }

    @Test
    public void shouldCallGetAllWithConsumerWhereSomeKeysMatch()
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
        CompletableFuture<Void> future   = cache.async(f_options) .getAll(listKeys, entry ->
                mapAsync.put(entry.getKey(), entry.getValue()));

        future.join();

        assertThat(mapCache.size(), is(2));
        assertThat(mapAsync.size(), is(2));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void shouldCallGetAllWithBiConsumerWhereAllKeysMatch()
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
        CompletableFuture<Void> future   = cache.async(f_options) .getAll(listKeys, mapAsync::put);

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
    public void shouldCallGetAlWithBiConsumerWhereNoKeysMatch()
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
        CompletableFuture<Void> future   = cache.async(f_options) .getAll(listKeys, mapAsync::put);

        future.join();

        assertThat(mapCache.isEmpty(), is(true));
        assertThat(mapAsync.isEmpty(), is(true));
        }

    @Test
    public void shouldCallGetAllWithBiConsumerWhereSomeKeysMatch()
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
        CompletableFuture<Void> future   = cache.async(f_options) .getAll(listKeys, mapAsync::put);

        future.join();

        assertThat(mapCache.size(), is(2));
        assertThat(mapAsync.size(), is(2));
        assertThat(mapCache, hasEntry(4, "value-4"));
        assertThat(mapCache, hasEntry(8, "value-8"));
        assertThat(mapAsync, is(mapCache));
        }

    @Test
    public void shouldCallPut()
            throws Exception
        {
        NamedCache<String, Integer>      cache = getNamedCache();
        AsyncNamedCache<String, Integer> async = cache.async(f_options);

        async.put("1", 1).get(1, TimeUnit.MINUTES);
        assertThat(cache.get("1"), is(1));
        }

    @Test
    public void shouldCallPutWithExpiry()
            throws Exception
        {
        NamedCache<String, Integer>      cache = getNamedCache();
        AsyncNamedCache<String, Integer> async = cache.async(f_options);

        async.put("1", 1, 5000L).get(1, TimeUnit.MINUTES);
        assertThat(cache.get("1"), is(1));
        Eventually.assertDeferred(() -> cache.get("1"), is(nullValue()));
        }

    @Test
    public void shouldCallPutAll()
            throws Exception
        {
        NamedCache<String, Integer>      cache = getNamedCache();
        AsyncNamedCache<String, Integer> async = cache.async(f_options);

        Map<String, Integer> map = Map.of("11", 11, "22", 22);
        async.putAll(map).get(1, TimeUnit.MINUTES);
        assertThat(cache.get("11"), is(11));
        assertThat(cache.get("22"), is(22));
        }

    @Test
    public void shouldCallPutAllWithExpiry()
            throws Exception
        {
        /*
        NOTE: If this test fails with UnsupportedOperationException when running in IntelliJ make sure
        the tangosol-coherence.xml file has not had the version in the license section overwritten by the IDE.
        If you see logs without a real version like this: "Oracle Coherence GE ${project.version.official} ${project.impl.description} "
        then you need to rebuild coherence.
         */
        NamedCache<String, Integer>      cache = getNamedCache();
        AsyncNamedCache<String, Integer> async = cache.async(f_options);

        Map<String, Integer> map = Map.of("11", 11, "22", 22);
        async.putAll(map, 5000L).get(1, TimeUnit.MINUTES);
        assertThat(cache.get("11"), is(11));
        assertThat(cache.get("22"), is(22));

        Eventually.assertDeferred(cache::isEmpty, is(true));
        }

    @Test
    public void shouldCallPutIfAbsent()
            throws Exception
        {
        NamedCache<String, Integer>      cache = getNamedCache();
        AsyncNamedCache<String, Integer> async = cache.async(f_options);

        assertThat(async.putIfAbsent("1", 1).get(1, TimeUnit.MINUTES), is(nullValue()));
        assertThat(async.putIfAbsent("1", 100).get(1, TimeUnit.MINUTES), is(1));
        assertThat(cache.get("1"), is(1));
        cache.put("2", null);
        assertThat(cache.size(), is(2));
        assertThat(async.putIfAbsent("2", 2).get(1, TimeUnit.MINUTES), is(nullValue()));
        assertThat(async.putIfAbsent("2", 200).get(1, TimeUnit.MINUTES), is(2));
        assertThat(cache.get("2"), is(2));
        }

    @Test
    public void shouldCallRemove()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);

        assertFalse(cache.async(f_options).remove("1", 2).get(1, TimeUnit.MINUTES));
        assertTrue(cache.async(f_options).remove("2", 2).get(1, TimeUnit.MINUTES));

        assertEquals(1, cache.size());
        assertTrue(cache.containsKey("1"));
        assertFalse(cache.containsKey("2"));
        }

    @Test
    public void shouldCallReplace()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", null);

        assertEquals(1, (int) cache.async(f_options).replace("1", 100).get(1, TimeUnit.MINUTES));
        assertNull(cache.async(f_options).replace("2", 200).get(1, TimeUnit.MINUTES));
        assertNull(cache.async(f_options).replace("3", 300).get(1, TimeUnit.MINUTES));

        assertEquals(2, cache.size());
        assertFalse(cache.containsKey("3"));
        }

    @Test
    public void shouldCallReplaceWithValueCheck()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", null);
        cache.put("3", null);

        assertTrue(cache.async(f_options).replace("1", 1, 100).get(1, TimeUnit.MINUTES));
        assertFalse(cache.async(f_options).replace("2", 2, 200).get(1, TimeUnit.MINUTES));
        assertTrue(cache.async(f_options).replace("3", null, 300).get(1, TimeUnit.MINUTES));
        assertFalse(cache.async(f_options).replace("4", 4, 400).get(1, TimeUnit.MINUTES));

        assertEquals(100, (int) cache.get("1"));
        assertNull(cache.get("2"));
        assertEquals(300, (int) cache.get("3"));
        assertFalse(cache.containsKey("4"));
        }

    @Test
    public void shouldCallComputeIfAbsent()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("five", 5);
        assertEquals(1, (int) cache.async(f_options).computeIfAbsent("1", Integer::parseInt).get(1, TimeUnit.MINUTES));
        assertEquals(5, (int) cache.async(f_options).computeIfAbsent("five", Integer::parseInt).get(1, TimeUnit.MINUTES));
        assertNull(cache.async(f_options).computeIfAbsent("null", (k) -> null).get(1, TimeUnit.MINUTES));
        }

    @Test
    public void shouldCallComputeIfPresent()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);

        assertEquals(2, (int) cache.async(f_options).computeIfPresent("1", (k, v) -> v + v).get(1, TimeUnit.MINUTES));
        assertEquals(4, (int) cache.async(f_options).computeIfPresent("2", (k, v) -> v * v).get(1, TimeUnit.MINUTES));
        assertNull(cache.async(f_options).computeIfPresent("1", (k, v) -> null).get(1, TimeUnit.MINUTES));
        assertNull(cache.async(f_options).computeIfPresent("3", (k, v) -> v * v).get(1, TimeUnit.MINUTES));

        assertEquals(4, (int) cache.get("2"));
        assertEquals(1, cache.size());
        assertFalse(cache.containsKey("1"));
        }

    @Test
    public void shouldCallCompute()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);

        assertEquals(2, (int) cache.async(f_options).compute("1", (k, v) -> v + v).get(1, TimeUnit.MINUTES));
        assertNull(cache.async(f_options).compute("2", (k, v) -> null).get(1, TimeUnit.MINUTES));
        assertFalse(cache.containsKey("2"));
        }

    @Test
    public void shouldCallMerge()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);

        assertEquals(2, (int) cache.async(f_options).merge("1", 1, Integer::sum).get(1, TimeUnit.MINUTES));
        assertEquals(3, (int) cache.async(f_options).merge("2", 1, Integer::sum).get(1, TimeUnit.MINUTES));
        assertEquals(1, (int) cache.async(f_options).merge("3", 1, Integer::sum).get(1, TimeUnit.MINUTES));
        assertNull(cache.async(f_options).merge("1", 1, (v1, v2) -> null).get(1, TimeUnit.MINUTES));

        assertFalse(cache.containsKey("1"));
        assertTrue(cache.containsKey("3"));
        }

    @Test
    public void shouldCallReplaceAll()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        cache.async(f_options).replaceAll((k, v) -> v * v).get(1, TimeUnit.MINUTES);
        assertEquals(1, (int) cache.get("1"));
        assertEquals(4, (int) cache.get("2"));
        assertEquals(9, (int) cache.get("3"));
        }

    @Test
    public void shouldCallReplaceAllWithKeySet()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        cache.async(f_options).replaceAll(Arrays.asList("1", "3"), (k, v) -> v * v).get(1, TimeUnit.MINUTES);
        assertEquals(1, (int) cache.get("1"));
        assertEquals(2, (int) cache.get("2"));
        assertEquals(9, (int) cache.get("3"));
        }

    @Test
    public void shouldCallReplaceAllWithFilter()
            throws Exception
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        cache.async(f_options).replaceAll(GREATER_THAN_2, (k, v) -> v * v).get(1, TimeUnit.MINUTES);
        assertEquals(1, (int) cache.get("1"));
        assertEquals(2, (int) cache.get("2"));
        assertEquals(9, (int) cache.get("3"));
        }

    @Test
    public void shouldCallAggregate()
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Count<String, Integer> count = new Count<>();

        cache.async(f_options).aggregate(NullImplementation.getSet(), count)
                .thenAccept(x -> assertThat(x, is(0L)));

        cache.async(f_options).aggregate(cache.keySet(), new LongSum<>(IdentityExtractor.INSTANCE()))
                .thenAccept(x -> assertThat(x, is(6L)));
        }

    @Test
    public void shouldCallAggregateWithFilter()
        {
        NamedCache<String, Integer> cache = getNamedCache();
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Count<String, Integer> count = new Count<>();

        cache.async(f_options).aggregate((Filter<?>) null, count)
                .thenAccept(x -> assertThat(x, is(0)));

        cache.async(f_options).aggregate(AlwaysFilter.INSTANCE, count)
                .thenAccept(x -> assertThat(x, is(3)));

        cache.async(f_options).aggregate(AlwaysFilter.INSTANCE(), new LongSum<>(IdentityExtractor.INSTANCE()))
                    .thenAccept(x -> assertThat(x, is(6L)));

        cache.async(f_options).aggregate(
                Filters.greater(x -> ((Integer) x), 1), new LongSum<>(IdentityExtractor.INSTANCE()))
                .thenAccept(x -> assertThat(x, is(5L)));
        }

    /**
     * Test of the {@link CompositeAggregator} aggregator.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldCallCompositeAggregator()
        {
        NamedCache<Integer, Integer> cache = getNamedCache();
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i <= 10; ++i)
            {
            map.put(i, i);
            }
        cache.putAll(map);
        Set<Integer> setKeys = cache.keySet();

        EntryAggregator<Integer, Integer, List<Number>> composite = CompositeAggregator.createInstance(new
                EntryAggregator[]{new Count<>(), new LongSum<>(IdentityExtractor.INSTANCE())});

        cache.async(f_options).aggregate(setKeys, composite).thenAccept(x ->
            {
            assertEquals(x.get(0), 10);
            assertEquals(x.get(1), 55L);
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

    @SuppressWarnings({"NumericOverflow", "divzero"})
    public static String error(InvocableMap.Entry<Integer, String> e)
        {
        return e.setValue(Integer.toString(5 / 0));
        }

    protected static final GreaterFilter<?, ?> GREATER_THAN_1 = new GreaterFilter<>(IdentityExtractor.INSTANCE(), 1);
    protected static final GreaterFilter<?, ?> GREATER_THAN_2 = new GreaterFilter<>(IdentityExtractor.INSTANCE(), 2);

    // ----- data members ---------------------------------------------------

    /**
     * The cache name to test (should map to a valid name in the coherence-cache-config.xml file
     * in this module's resources/ folder).
     */
    protected final String f_sCacheName;

    protected AsyncNamedMap.Option[] f_options;
    }
