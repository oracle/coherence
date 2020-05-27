/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cache.grpc.client;

import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapIndex;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.ReducerAggregator;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.MapEventFilter;

import com.tangosol.util.processor.ExtractorProcessor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;

import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * An abstract class of integration tests to verify {@link NamedCacheClient}.
 * <p>
 * These tests are run by sub-classes that can configure the {@link NamedCacheClient}
 * in different ways.
 *
 * @author Jonathan Knight  2019.11.25
 * @since 14.1.2
 */
@SuppressWarnings("rawtypes")
abstract class BaseNamedCacheClientIT
    {
    /**
     * Create an instance of the {@link NamedCacheClient} to use for testing.
     *
     * @param sCacheName       the nam eof the underlying cache
     * @param sSerializerName  the name of the serialization format to use
     * @param serializer       the serializer to use
     *
     * @return an instance of the {@link NamedCacheClient} to use for testing
     */
    protected abstract <K, V> NamedCacheClient<K, V> createClient(String sCacheName, String sSerializerName,
                                                                  Serializer serializer);

    /**
     * Obtain the specified {@link NamedCache}.
     *
     * @param sName   the cache name
     * @param loader  the {@link ClassLoader} to use to obtain the cache
     * @param <K>     the type of the cache keys
     * @param <V>     the type of the cache values
     *
     * @return the specified {@link NamedCache}
     */
    protected abstract <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader);

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldAddIndex(String sSerializerName, Serializer serializer)
        {
        String                        cacheName = "add-index-cache";
        NamedCache                    cache     = ensureCache(cacheName);
        Map<ValueExtractor, MapIndex> indexMap  = removeIndexes(cache);

        ValueExtractor<String, Integer> extractor = new UniversalExtractor<>("length()");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        service.addIndex(extractor, false, null);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldAddSortedIndex(String sSerializerName, Serializer serializer)
        {
        String                        cacheName = "add-index-cache";
        NamedCache                    cache     = ensureCache(cacheName);
        Map<ValueExtractor, MapIndex> indexMap  = removeIndexes(cache);

        ValueExtractor<String, Integer> extractor = new UniversalExtractor<>("length()");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        service.addIndex(extractor, true, null);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldAddSortedIndexWithComparator(String sSerializerName, Serializer serializer)
        {
        String                        cacheName = "add-index-cache";
        NamedCache                    cache     = ensureCache(cacheName);
        Map<ValueExtractor, MapIndex> indexMap  = removeIndexes(cache);

        ValueExtractor<String, Integer> extractor  = new UniversalExtractor<>("length()");
        Comparator<Integer>             comparator = IdentityExtractor.INSTANCE();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        service.addIndex(extractor, true, comparator);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(true));
        assertThat(indexMap.get(extractor).getComparator(), is(comparator));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldClearEmptyCache(String sSerializerName, Serializer serializer)
        {
        String     cacheName = "test-cache";
        NamedCache cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        service.clear();

        assertThat(cache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldClearPopulatedCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 10);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        service.clear();

        assertThat(cache.isEmpty(), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnTrueForContainsKeyWithExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.containsKey("key-2");
        assertThat(result, is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnFalseForContainsKeyWithNonExistentMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.containsKey("missing-key");
        assertThat(result, is(false));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldContainValueWhenValuePresent(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 3);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.containsValue("value-2");
        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldContainValueWhenValuePresentMultipleTimes(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");
        cache.put("key-11", "value-1");
        cache.put("key-22", "value-2");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.containsValue("value-2");
        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotContainValueWhenMappingNotPresent(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 3);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.containsValue("value-100");
        assertThat(result, is(false));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldDestroyCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);

        DeactivationListener<AsyncNamedCacheClient<? super String, ? super String>> listener = mock(DeactivationListener.class);

        NamedCacheClient<String, String>      service     = createClient(cacheName, sSerializerName, serializer);
        AsyncNamedCacheClient<String, String> asyncClient = service.getAsyncClient();
        asyncClient.addDeactivationListener(listener);

        service.destroy();

        assertThat(cache.isDestroyed(), is(true));
        assertThat(service.isDestroyed(), is(true));
        assertThat(service.isReleased(), is(cache.isReleased()));
        assertThat(service.isActive(), is(false));
        verify(listener).destroyed(same(asyncClient));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldReleaseCache(String sSerializerName, Serializer serializer)
        {
        String     cacheName = "test-cache";
        NamedCache cache     = ensureCache(cacheName);

        DeactivationListener<AsyncNamedCacheClient<? super String, ? super String>> listener = mock(DeactivationListener.class);

        NamedCacheClient<String, String>      service     = createClient(cacheName, sSerializerName, serializer);
        AsyncNamedCacheClient<String, String> asyncClient = service.getAsyncClient();
        asyncClient.addDeactivationListener(listener);

        service.release();

        assertThat(cache.isDestroyed(), is(false));
        assertThat(cache.isReleased(), is(false));
        assertThat(service.isReleased(), is(true));
        assertThat(service.isDestroyed(), is(false));
        assertThat(service.isActive(), is(false));
        verify(listener).released(same(asyncClient));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetExistingKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.get("key-2");
        assertThat(result, is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetExistingKeyMappedToNull(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);
        cache.put("key-2", null);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.get("key-2");
        assertThat(result, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.get("missing-key");
        assertThat(result, is(nullValue()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetOrDefaultForExistingKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.getOrDefault("key-2", "default-value");
        assertThat(result, is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetOrDefaultForExistingKeyMappedToNull(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);
        cache.put("key-2", null);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.getOrDefault("key-2", "default-value");
        assertThat(result, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetOrDefaultForNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.getOrDefault("missing-key", "default-value");
        assertThat(result, is("default-value"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetAllForEmptyKeyCollection(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        assertGetAll(cache, sSerializerName, serializer, Collections.emptyList());
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetAllWhenNoKeysMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 4);

        Collection<String> keys = Arrays.asList("key-5", "key-6");

        assertGetAll(cache, sSerializerName, serializer, keys);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetAllWhenAllKeysMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 4);

        Collection<String> keys = Arrays.asList("key-2", "key-4");

        assertGetAll(cache, sSerializerName, serializer, keys);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetAllWhenAllSomeKeysMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 4);

        Collection<String> keys = Arrays.asList("key-0", "key-2", "key-4", "key-5");

        assertGetAll(cache, sSerializerName, serializer, keys);
        }

    void assertGetAll(NamedCache<String, String> cache, String sSerializerName, Serializer serializer, Collection<String> keys)
        {
        Map<String, String> expected = cache.getAll(keys);

        NamedCacheClient<String, String> service = createClient(cache.getCacheName(), sSerializerName, serializer);
        Map<String, String>              results = service.getAll(keys);
        assertThat(results, is(expected));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldBeEmpty(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean result = service.isEmpty();
        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotBeEmpty(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 10);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean result = service.isEmpty();
        assertThat(result, is(false));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldInsertNewEntry(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.put("key-1", "value-1");
        assertThat(result, is(nullValue()));

        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldInsertNewEntryWithExpiry(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.put("key-1", "value-1", 500000L);
        assertThat(result, is(nullValue()));

        assertThat(cache.get("key-1"), is("value-1"));

        // update the entry with a short TTL
        service.put("key-1", "value-1", 1000L);
        Thread.sleep(2000L);
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldUpdateEntry(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.put("key-1", "value-2");
        assertThat(result, is("value-1"));

        assertThat(cache.get("key-1"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldUpdateEntryPreviouslyMappedToNull(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", null);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.put("key-1", "value-2");
        assertThat(result, is(nullValue()));

        assertThat(cache.get("key-1"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldUpdateEntryWithNullValue(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.put("key-1", null);
        assertThat(result, is("value-1"));

        assertThat(cache.get("key-1"), is(nullValue()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldPutIfAbsentForNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.putIfAbsent("key-1", "value-1");

        assertThat(result, is(nullValue()));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldPutIfAbsentForExistingKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.putIfAbsent("key-1", "value-2");

        assertThat(result, is("value-1"));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldPutIfAbsentForExistingKeyMappedToNullValue(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", null);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.putIfAbsent("key-1", "value-2");

        assertThat(result, is(nullValue()));
        assertThat(cache.get("key-1"), is(nullValue()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldPutAllEntries(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        Map<String, String> map = new HashMap<>();
        map.put("key-1", "value-1");
        map.put("key-2", "value-2");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);

        service.putAll(map);

        assertThat(cache.size(), is(2));
        assertThat(cache.get("key-1"), is("value-1"));
        assertThat(cache.get("key-2"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldPutAllWithZeroEntries(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);

        service.putAll(new HashMap<>());

        assertThat(cache.isEmpty(), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveOnNonExistentEntry(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        int                        count     = 10;
        clearAndPopulate(cache, count);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.remove("key-100");
        assertThat(result, is(nullValue()));
        assertThat(cache.size(), is(count));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnPreviousValueForRemoveOnExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        int                        count     = 10;
        clearAndPopulate(cache, count);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.remove("key-1");
        assertThat(result, is("value-1"));
        assertThat(cache.get("key-1"), is(nullValue()));
        assertThat(cache.size(), is(count - 1));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnFalseForRemoveMappingOnNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result = service.remove("key-123", "value-123");
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnFalseForRemoveMappingOnNonMatchingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.remove("key-1", "value-123");
        assertThat(result, is(false));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnTrueForRemoveMappingOnMatchingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.remove("key-1", "value-1");
        assertThat(result, is(true));
        assertThat(cache.containsKey("key-1"), is(false));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnNullValueForReplaceOnNonExistentMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.replace("key-1", "value-123");
        assertThat(result, is(nullValue()));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnNonNullForReplaceOnExistentMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        String                           result  = service.replace("key-1", "value-123");
        assertThat(result, is("value-1"));
        assertThat(cache.get("key-1"), is("value-123"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnFalseForReplaceMappingOnNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.replace("key-1", "value-1", "value-123");
        assertThat(result, is(false));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnFalseForReplaceMappingOnNonMatchingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.replace("key-1", "value-123", "value-456");
        assertThat(result, is(false));
        assertThat(cache.get("key-1"), is("value-1"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnTrueForReplaceMappingOnMatchingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = service.replace("key-1", "value-1", "value-123");
        assertThat(result, is(true));
        assertThat(cache.get("key-1"), is("value-123"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReplaceAllWithKeySet(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        List<String> keys           = new ArrayList<>(cache.keySet());
        Object[]     expectedValues = new ArrayList<>(cache.values()).stream().map(v -> v + '1').toArray();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);

        service.replaceAll(keys, (k, v) ->
           {
           v = v + "1";
           return v;
           });

        Collection<String> newValues = cache.values();

        assertThat(newValues, containsInAnyOrder(expectedValues));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReplaceAllWithFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        Filter<Person> filter = Filters.equal("age", 25);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        service.replaceAll(filter, (k, v) ->
            {
            v.setAge(v.getAge() + 10);
            return v;
            });

        assertThat(cache.get("Dent").getAge(), is(35));
        assertThat(cache.get("Gently").getAge(), is(35));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetSizeOfEmptyCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        int                              size    = service.size();
        assertThat(size, is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetSizeOfPopulatedCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 10);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        int                              size    = service.size();
        assertThat(size, is(cache.size()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldTruncate(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        service.truncate();

        assertThat(cache.isEmpty(), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveIndexWhenIndexExists(String sSerializerName, Serializer serializer)
        {
        String     cacheName = "add-index-cache";
        NamedCache cache     = ensureCache(cacheName);
        cache.clear();

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor<String, Integer> extractor = new UniversalExtractor<>("length()");

        // Add the index using the normal cache
        cache.addIndex(extractor, false, null);

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        service.removeIndex(extractor);

        assertThat(indexMap.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveIndexWhenIndexDoesNotExist(String sSerializerName, Serializer serializer)
        {
        String     cacheName = "add-index-cache";
        NamedCache cache     = ensureCache(cacheName);
        cache.clear();

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor<String, Integer> extractor = new UniversalExtractor<>("length()");

        NamedCacheClient<String, String> service = createClient(cacheName, sSerializerName, serializer);
        service.removeIndex(extractor);

        assertThat(indexMap.isEmpty(), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnKeySetWithFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        Filter<Person>                   filter  = Filters.equal("age", 25);
        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Set<String> keys = service.keySet(filter);

        assertThat(keys.contains("Dent"), is(true));
        assertThat(keys.contains("Gently"), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnEntrySetWithFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        Filter<Person>                   filter  = Filters.equal("age", 25);
        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Set<Map.Entry<String, Person>> entries = service.entrySet(filter);

        assertThat(entries.size(), is(2));
        assertThat(entries.contains(new AbstractMap.SimpleEntry<>("Dent", person1)), is(true));
        assertThat(entries.contains(new AbstractMap.SimpleEntry<>("Gently", person2)), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldComputeAndUpdateEntry(String sSerializerName, Serializer serializer)
        {
        String                      cacheName = "test-cache";
        NamedCache<String, Integer> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("k1", 1);
        cache.put("k2", 2);

        NamedCacheClient<String, Integer> service = createClient(cacheName, sSerializerName, serializer);

        //noinspection ConstantConditions
        int newValue = service.compute("k1", (k, v) -> v + v);
        assertThat(newValue, is(2));

        service.compute("k2", (k, v) -> null);

        assertThat(cache.get("k2"), is(nullValue()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallInvoke(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        String key    = "bb";
        Person person = new Person("bob", "builder", 25, "male");
        cache.put(key, person);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("lastName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        String lastName = service.invoke(key, processor);

        assertThat(lastName, is(notNullValue()));
        assertThat(lastName, is("builder"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallInvokeAllWithFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("firstName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);
        Filter<Person>                                      filter    = Filters.equal("age", 25);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> map = service.invokeAll(filter, processor);

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallInvokeAllWithAlwaysFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 25, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("firstName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> map = service.invokeAll(AlwaysFilter.INSTANCE, processor);

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        assertThat(map, hasEntry(person3.getLastName(), person3.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallInvokeAllWithKeys(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 25, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("firstName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);
        List<String>                                        keys      = Arrays.asList(person1.getLastName(),
                                                                                      person2.getLastName());

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> map = service.invokeAll(keys, processor);

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallAggregateWithFilterExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();
        Filter<Person>                                        filter     = Filters.equal("age", 25);

        int expected = cache.aggregate(filter, aggregator);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        int result = service.aggregate(filter, aggregator);

        assertThat(result, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallAggregateWithFilterMatchingNoEntriesExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();
        Filter<Person>                                        filter     = Filters.equal("age", 100);

        int expected = cache.aggregate(filter, aggregator);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);
        int                              result  = service.aggregate(filter, aggregator);

        assertThat(result, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallAggregateWithKeysExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();
        List<String>                                          keys       = Arrays.asList(person1.getLastName(),
                                                                                         person2.getLastName());

        int expected = cache.aggregate(keys, aggregator);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        int result = service.aggregate(keys, aggregator);

        assertThat(result, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallAggregateWithKeysMatchingNoEntriesExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();
        List<String>                                          keys       = Arrays.asList("foo", "bar");

        int expected = cache.aggregate(keys, aggregator);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        int result = service.aggregate(keys, aggregator);

        assertThat(result, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallAggregateWithNoKeysOrFilterExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();

        int expected = cache.aggregate(aggregator);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        int result = service.aggregate(aggregator);
        assertThat(result, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallAggregateWithFilterExpectingMapResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String> extractor = Extractors.extract("getFirstName()");
        Filter<Person>                 filter    = Filters.equal("age", 25);

        InvocableMap.EntryAggregator<String, Person, Map<String, String>> aggregator =
                new ReducerAggregator<>(extractor);


        Map<String, String> expected = cache.aggregate(filter, aggregator);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> result = service.aggregate(filter, aggregator);

        assertThat(result, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallAggregateWithKeysExpectingMapResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String> extractor = Extractors.extract("getFirstName()");
        List<String>                   keys      = Arrays.asList(person1.getLastName(), person2.getLastName());

        InvocableMap.EntryAggregator<String, Person, Map<String, String>> aggregator = new ReducerAggregator<>(extractor);

        Map<String, String> expected = cache.aggregate(keys, aggregator);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> result = service.aggregate(keys, aggregator);

        assertThat(result, is(expected));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallValuesWithFilterWhenSomeEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        Filter<Person> filter = new EqualsFilter<>("getAge", 25);

        Collection<Person> expected = cache.values(filter);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Collection<Person> result = service.values(filter);

        assertThat(result, containsInAnyOrder(expected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallValuesWithFilterWhenAllEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        Filter<Person> filter = Filters.always();

        Collection<Person> expected = cache.values(filter);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Collection<Person> result = service.values(filter);

        assertThat(result, containsInAnyOrder(expected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallValuesWithFilterWhenNoEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "people";
        NamedCache<String, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        Filter<Person> filter = new EqualsFilter<>("getAge", 100);

        Collection<Person> expected = cache.values(filter);

        NamedCacheClient<String, Person> service = createClient(cacheName, sSerializerName, serializer);

        Collection<Person> result = service.values(filter);

        assertThat(result, containsInAnyOrder(expected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldCallValuesWithFilterAndComparatorWhenSomeEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                      cacheName = "numbers";
        NamedCache<String, Integer> cache     = ensureCache(cacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer>     filter     = Filters.greater(Extractors.identity(), 50);
        Comparator<Integer> comparator = IdentityExtractor.INSTANCE();

        Collection<Integer> expected = cache.values(filter, comparator);

        NamedCacheClient<String, Integer> service = createClient(cacheName, sSerializerName, serializer);

        Collection<Integer> result = service.values(filter, comparator);

        assertThat(result, containsInAnyOrder(expected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldCallValuesWithFilterAndComparatorWhenAllEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                      cacheName = "numbers";
        NamedCache<String, Integer> cache     = ensureCache(cacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer>     filter     = Filters.always();
        Comparator<Integer> comparator = IdentityExtractor.INSTANCE();

        Collection<Integer> expected = cache.values(filter, comparator);

        NamedCacheClient<String, Integer> service = createClient(cacheName, sSerializerName, serializer);

        Collection<Integer> result = service.values(filter, comparator);

        assertThat(result, containsInAnyOrder(expected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldCallValuesWithFilterAndComparatorWhenNoEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                      cacheName = "numbers";
        NamedCache<String, Integer> cache     = ensureCache(cacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer>     filter     = Filters.greater(Extractors.identity(), 500);
        Comparator<Integer> comparator = IdentityExtractor.INSTANCE();

        Collection<Integer> expected = cache.values(filter, comparator);

        NamedCacheClient<String, Integer> service = createClient(cacheName, sSerializerName, serializer);

        Collection<Integer> result = service.values(filter, comparator);

        assertThat(result, containsInAnyOrder(expected.toArray()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForSingleKey(String sSerializerName, Serializer serializer) throws Exception
        {
        String                                cacheName        = "test-events-01";
        CollectingMapListener<String, String> listenerExpected = new CollectingMapListener<>(3);
        NamedCache<String, String>            cache            = ensureCache(cacheName);
        cache.clear();
        cache.addMapListener(listenerExpected, "key-2", false);

        NamedCacheClient<String, String>      service      = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingMapListener<>(3);

        service.addMapListener(listenerTest, "key-2", true);

        // update the cache to generate events
        for (int i = 0; i < 10; i++)
            {
            cache.put("key-" + i, "value-" + i);
            }
        for (int i = 0; i < 10; i++)
            {
            cache.put("key-" + i, "value-" + i + "-updated");
            }
        for (int i = 0; i < 10; i++)
            {
            cache.remove("key-" + i);
            }

        assertThat(listenerTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerExpected.getInsertCount(), is(listenerTest.getInsertCount()));
        assertThat(listenerExpected.getUpdateCount(), is(listenerTest.getUpdateCount()));
        assertThat(listenerExpected.getDeleteCount(), is(listenerTest.getDeleteCount()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForMultipleKeys(String sSerializerName, Serializer serializer) throws Exception
        {
        String                                cacheName        = "test-events-02";
        CollectingMapListener<String, String> listenerExpected = new CollectingMapListener<>(6);
        NamedCache<String, String>            cache            = ensureCache(cacheName);
        cache.clear();
        cache.addMapListener(listenerExpected, "key-2", false);
        cache.addMapListener(listenerExpected, "key-4", false);

        NamedCacheClient<String, String>      service      = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingMapListener<>(6);

        service.addMapListener(listenerTest, "key-2", true);
        service.addMapListener(listenerTest, "key-4", true);

        // update the cache to generate events
        for (int i = 0; i < 10; i++)
            {
            cache.put("key-" + i, "value-" + i);
            }
        for (int i = 0; i < 10; i++)
            {
            cache.put("key-" + i, "value-" + i + "-updated");
            }
        for (int i = 0; i < 10; i++)
            {
            cache.remove("key-" + i);
            }

        assertThat(listenerTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerExpected.getInsertCount(), is(listenerTest.getInsertCount()));
        assertThat(listenerExpected.getUpdateCount(), is(listenerTest.getUpdateCount()));
        assertThat(listenerExpected.getDeleteCount(), is(listenerTest.getDeleteCount()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForAllKeys(String sSerializerName, Serializer serializer) throws Exception
        {
        String                                cacheName        = "test-events-03";
        CollectingMapListener<String, String> listenerExpected = new CollectingMapListener<>(30);
        NamedCache<String, String>            cache            = ensureCache(cacheName);
        cache.clear();
        cache.addMapListener(listenerExpected);

        NamedCacheClient<String, String>      service      = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingMapListener<>(30);

        service.addMapListener(listenerTest);

        // update the cache to generate events
        for (int i = 0; i < 10; i++)
            {
            cache.put("key-" + i, "value-" + i);
            }
        for (int i = 0; i < 10; i++)
            {
            cache.put("key-" + i, "value-" + i + "-updated");
            }
        for (int i = 0; i < 10; i++)
            {
            cache.remove("key-" + i);
            }

        assertThat(listenerTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerExpected.getInsertCount(), is(listenerTest.getInsertCount()));
        assertThat(listenerExpected.getUpdateCount(), is(listenerTest.getUpdateCount()));
        assertThat(listenerExpected.getDeleteCount(), is(listenerTest.getDeleteCount()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForFilter(String sSerializerName, Serializer serializer) throws Exception
        {
        String                                 cacheName        = "test-events-04";
        CollectingMapListener<String, Integer> listenerExpected = new CollectingMapListener<>(20);
        NamedCache<String, Integer>            cache            = ensureCache(cacheName);
        cache.clear();

        MapEventFilter<String, Integer> filter = new MapEventFilter<>(Filters.less(Extractors.identity(), 10));
        cache.addMapListener(listenerExpected, filter, false);

        NamedCacheClient<String, Integer>      service      = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, Integer> listenerTest = new CollectingMapListener<>(20);

        service.addMapListener(listenerTest, filter, false);

        // update the cache to generate events
        for (int i = 0; i < 20; i++)
            {
            cache.put("key-" + i, i);
            }
        for (int i = 0; i < 20; i++)
            {
            cache.put("key-" + i, i + 100);
            }
        for (int i = 0; i < 20; i++)
            {
            cache.remove("key-" + i);
            }

        assertThat(listenerTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerExpected.getInsertCount(), is(listenerTest.getInsertCount()));
        assertThat(listenerExpected.getUpdateCount(), is(listenerTest.getUpdateCount()));
        assertThat(listenerExpected.getDeleteCount(), is(listenerTest.getDeleteCount()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Disabled
    void shouldSubscribeToEventsForKeyAndFilter(String sSerializerName, Serializer serializer) throws Exception
        {
        String                                 cacheName      = "test-events-04";
        CollectingMapListener<String, Integer> listenerFilter = new CollectingMapListener<>(20);
        CollectingMapListener<String, Integer> listenerKey    = new CollectingMapListener<>(3);
        NamedCache<String, Integer>            cache          = ensureCache(cacheName);
        cache.clear();

        MapEventFilter<String, Integer> filter = new MapEventFilter<>(Filters.less(Extractors.identity(), 10));
        cache.addMapListener(listenerFilter, filter, false);
        cache.addMapListener(listenerKey, "key-2", false);

        NamedCacheClient<String, Integer>      service            = createClient(cacheName, sSerializerName,
                                                                                 serializer);
        CollectingMapListener<String, Integer> listenerFilterTest = new CollectingMapListener<>(20);
        CollectingMapListener<String, Integer> listenerKeyTest    = new CollectingMapListener<>(3);

        service.addMapListener(listenerFilterTest, filter, false);
        service.addMapListener(listenerKeyTest, "key-2", false);

        // update the cache to generate events
        for (int i = 0; i < 20; i++)
            {
            cache.put("key-" + i, i);
            }
        for (int i = 0; i < 20; i++)
            {
            cache.put("key-" + i, i + 100);
            }
        for (int i = 0; i < 20; i++)
            {
            cache.remove("key-" + i);
            }

        assertThat(listenerFilter.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerKey.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerFilterTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerFilter.getInsertCount(), is(listenerFilterTest.getInsertCount()));
        assertThat(listenerFilter.getUpdateCount(), is(listenerFilterTest.getUpdateCount()));
        assertThat(listenerFilter.getDeleteCount(), is(listenerFilterTest.getDeleteCount()));
        assertThat(listenerKey.getInsertCount(), is(listenerKeyTest.getInsertCount()));
        assertThat(listenerKey.getUpdateCount(), is(listenerKeyTest.getUpdateCount()));
        assertThat(listenerKey.getDeleteCount(), is(listenerKeyTest.getDeleteCount()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldReceiveDeactivationEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        TestDeactivationListener         listener = new TestDeactivationListener(1);
        NamedCacheClient<String, String> service  = createClient(cacheName, sSerializerName, serializer);
        service.addMapListener(listener);

        cache.destroy();

        assertThat(listener.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listener.isDestroyed(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldReceiveTruncateEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("foo", "bar");

        TestDeactivationListener         listener = new TestDeactivationListener(1);
        NamedCacheClient<String, String> service  = createClient(cacheName, sSerializerName, serializer);
        service.addMapListener(listener);

        cache.truncate();

        assertThat(listener.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listener.isTruncated(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldReceiveTruncateAndDeactivationEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "val-1");

        TestDeactivationListener         listener = new TestDeactivationListener(2);
        NamedCacheClient<String, String> service  = createClient(cacheName, sSerializerName, serializer);
        service.addMapListener(listener);

        cache.truncate();
        cache.put("key-2", "val-2");
        cache.destroy();

        assertThat(listener.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listener.isTruncated(), is(true));
        assertThat(listener.isDestroyed(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldAddPrimingListenerForExistingKey(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "val-1");

        CollectingMapListener<String, String> listenerExpected = new CollectingPrimingListener<>(2);
        cache.addMapListener(listenerExpected, "key-1", true);

        cache.put("key-2", "val-2");

        NamedCacheClient<String, String>      service      = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingPrimingListener<>(2);
        service.addMapListener(listenerTest, "key-2", true);

        assertThat(listenerExpected.getUpdateCount(), is(1));
        assertThat(listenerTest.getUpdateCount(), is(1));

        cache.put("key-2", "val-22");
        cache.put("key-1", "val-11");

        assertThat(listenerExpected.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerExpected.getUpdateCount(), is(2));
        assertThat(listenerTest.getUpdateCount(), is(2));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldAddPrimingListenerForNonExistingKey(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        CollectingMapListener<String, String> listenerExpected = new CollectingPrimingListener<>(2);
        cache.addMapListener(listenerExpected, "key-1", true);

        NamedCacheClient<String, String>      service      = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingPrimingListener<>(2);
        service.addMapListener(listenerTest, "key-2", true);

        assertThat(listenerExpected.getUpdateCount(), is(1));
        assertThat(listenerTest.getUpdateCount(), is(1));

        cache.put("key-1", "val-11");
        cache.put("key-2", "val-22");

        assertThat(listenerExpected.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listenerExpected.getUpdateCount(), is(1));
        assertThat(listenerTest.getUpdateCount(), is(1));
        assertThat(listenerExpected.getInsertCount(), is(1));
        assertThat(listenerTest.getInsertCount(), is(1));
        }

    @SuppressWarnings({"unused", "unchecked"})
    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldAddMapTrigger(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-trigger";
        NamedCache<String, Person> cache     = ensureCache(cacheName);

        MapTriggerListener listener = new MapTriggerListener(new PersonMapTrigger());

        cache.addMapListener(listener);

        // with the trigger removed the person's last name should be converted to upper case
        Person person = new Person("Tony", "Stark", 53, "male");
        cache.put("key", person);
        Person cached = cache.get("key");

        assertThat(cached.getLastName(), is(person.getLastName().toUpperCase()));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the {@link Serializer} instances to use for parameterized
     * test {@link org.junit.jupiter.params.provider.Arguments}.
     *
     * @return the {@link Serializer} instances to use for test
     * {@link org.junit.jupiter.params.provider.Arguments}
     */
    protected static Stream<Arguments> serializers()
        {
        ClassLoader loader = Base.getContextClassLoader();
        TreeMap<String, Serializer> map = new TreeMap<>();

        map.put("", new ConfigurablePofContext());
        map.put("json", new JsonSerializer());

        OperationalContext ctx = (OperationalContext) CacheFactory.getCluster();
        for (Map.Entry<String, SerializerFactory> entry : ctx.getSerializerMap().entrySet())
            {
            map.put(entry.getKey(), entry.getValue().createSerializer(loader));
            }

        return map.entrySet().stream()
                .map(e -> Arguments.of(e.getKey(), e.getValue()));
        }

    /**
     * Obtain the specified {@link NamedCache}.
     *
     * @param name  the cache name
     * @param <K>   the type of the cache keys
     * @param <V>   the type of the cache values
     *
     * @return the specified {@link NamedCache}
     */
    protected <K, V> NamedCache<K, V> ensureCache(String name)
        {
        return ensureCache(name, Base.getContextClassLoader());
        }

    /**
     * Clear the specified cache and populate it with entries.
     *
     * @param cache  the cache to clear and populate
     * @param count  the number of entries to add to the cache
     */
    protected void clearAndPopulate(NamedCache<String, String> cache, int count)
        {
        cache.clear();
        for (int i = 1; i <= count; i++)
            {
            cache.put("key-" + i, "value-" + i);
            }
        }

    /**
     * Remove all of the indexes from the cache and return its index map.
     *
     * @param cache  the cache to remove indexes from
     *
     * @return the cache's index map
     */
    protected Map<ValueExtractor, MapIndex> removeIndexes(NamedCache cache)
        {
        cache.clear();

        BackingMapContext ctx = cache.getCacheService()
                .getBackingMapManager()
                .getContext()
                .getBackingMapContext(cache.getCacheName());

        Map<ValueExtractor, MapIndex> indexMap = ctx.getIndexMap();
        for (Map.Entry<ValueExtractor, MapIndex> entry : indexMap.entrySet())
            {
            cache.removeIndex(entry.getKey());
            }
        return indexMap;
        }

    // ----- inner class: CollectingMapListener -----------------------------

    /**
     * A test listener that collects the received events.
     *
     * @param <K>  the type of the event key
     * @param <V>  the type of the event value
     */
    protected static class CollectingMapListener<K, V>
            implements MapListener<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link CollectingMapListener}.
         */
        public CollectingMapListener()
            {
            this(0);
            }

        /**
         * Create a {@link CollectingMapListener} that can wait for a specific number of events.
         *
         * @param nCount  the number of events to wait for
         */
        public CollectingMapListener(int nCount)
            {
            this.f_latch = new CountDownLatch(nCount);
            }

        // ----- MapListener interface --------------------------------------

        @Override
        public void entryInserted(MapEvent<K, V> mapEvent)
            {
            m_cInsert++;
            f_latch.countDown();
            }

        @Override
        public void entryUpdated(MapEvent<K, V> mapEvent)
            {
            m_cUpdate++;
            f_latch.countDown();
            }

        @Override
        public void entryDeleted(MapEvent<K, V> mapEvent)
            {
            m_cDelete++;
            f_latch.countDown();
            }

        // ----- public methods ---------------------------------------------

        /**
         * Wait for this listener to receive the required number of events
         * that was set when it was created.
         *
         * @param cTimeout  the amount of time to wait
         * @param units     the time units for the timeout value
         *
         * @return true if the number of events was received
         *
         * @throws InterruptedException if the thread is interrupted
         * @see java.util.concurrent.CountDownLatch#await(long, java.util.concurrent.TimeUnit)
         */
        public boolean awaitEvents(long cTimeout, TimeUnit units) throws InterruptedException
            {
            return f_latch.await(cTimeout, units);
            }

        // ----- accessors --------------------------------------------------

        public int getInsertCount()
            {
            return m_cInsert;
            }

        public int getUpdateCount()
            {
            return m_cUpdate;
            }

        public int getDeleteCount()
            {
            return m_cDelete;
            }

        // ----- data members -----------------------------------------------

        protected int m_cInsert;

        protected int m_cUpdate;

        protected int m_cDelete;

        protected final CountDownLatch f_latch;
        }

    // ----- inner class CollectingPrimingListener --------------------------

    protected static class CollectingPrimingListener<K, V>
            extends CollectingMapListener<K, V>
            implements MapListenerSupport.PrimingListener<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link CollectingPrimingListener}.
         */
        @SuppressWarnings("unused")
        public CollectingPrimingListener()
            {
            super();
            }

        /**
         * Create a {@link CollectingPrimingListener} that can wait for a specific number of events.
         *
         * @param nCount  the number of events to wait for
         */
        @SuppressWarnings("unused")
        public CollectingPrimingListener(int nCount)
            {
            super(nCount);
            }
        }

    // ----- inner class: TestDeactivationListener --------------------------

    protected static class TestDeactivationListener
            extends AbstractMapListener
            implements NamedCacheDeactivationListener
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link TestDeactivationListener}.
         */
        @SuppressWarnings("unused")
        public TestDeactivationListener()
            {
            this(0);
            }

        /**
         * Create a {@link TestDeactivationListener} that can wait for destroy/trucate events.
         *
         * @param count  the number of events to wait for
         */
        public TestDeactivationListener(int count)
            {
            this.f_latch = new CountDownLatch(count);
            }

        // ----- AbstractMapListener methods --------------------------------

        @Override
        public void entryDeleted(MapEvent evt)
            {
            // destroy/disconnect event
            m_destroyed = true;
            f_latch.countDown();
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            // "truncate" event
            m_truncated = true;
            f_latch.countDown();
            }

        // ----- public methods ---------------------------------------------

        /**
         * Wait for this listener to receive the required number of events
         * that was set when it was created.
         *
         * @param timeout  the amount of time to wait
         * @param units    the time units for the timeout value
         *
         * @return true if the number of events was received
         *
         * @throws InterruptedException if the thread is interrupted
         * @see java.util.concurrent.CountDownLatch#await(long, java.util.concurrent.TimeUnit)
         */
        public boolean awaitEvents(long timeout, TimeUnit units) throws InterruptedException
            {
            return f_latch.await(timeout, units);
            }

        // ----- accessors --------------------------------------------------

        public boolean isDestroyed()
            {
            return m_destroyed;
            }

        public boolean isTruncated()
            {
            return m_truncated;
            }

        // ----- data members -----------------------------------------------

        protected boolean m_destroyed;

        protected boolean m_truncated;

        protected final CountDownLatch f_latch;
        }
    }
