/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.client.AsyncNamedCacheClient;
import com.oracle.coherence.client.DeactivationListener;

import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.coherence.component.util.SafeAsyncNamedCache;
import com.tangosol.coherence.component.util.SafeNamedCache;
import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.internal.net.SessionNamedCache;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
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

import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * An abstract class of integration tests to verify the gRPC client.
 * <p>
 * These tests are run by subclasses that can configure the gRPC client
 * in different ways.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 20.06
 */
@SuppressWarnings({"rawtypes", "resource"})
abstract class AbstractGrpcClientIT
    {
    protected AbstractGrpcClientIT()
        {
        this(GrpcDependencies.DEFAULT_SCOPE);
        }

    public AbstractGrpcClientIT(String sScopeName)
        {
        f_sScopeName = sScopeName;
        }

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldClearEmptyCache(String sSerializerName, Serializer serializer)
        {
        String     cacheName = "test-cache";
        NamedCache cache     = ensureCache(cacheName);

        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.clear();

        assertThat(cache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldClearPopulatedCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);

        clearAndPopulate(cache, 10);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.clear();

        assertThat(cache.isEmpty(), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnTrueForContainsKeyWithExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);

        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.containsKey("key-2");

        assertThat(result, is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnFalseForContainsKeyWithNonExistentMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.containsKey("missing-key");
        assertThat(result, is(false));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldContainValueWhenValuePresent(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 3);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.containsValue("value-2");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.containsValue("value-2");
        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotContainValueWhenMappingNotPresent(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 3);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.containsValue("value-100");
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldDestroyCache(String sSerializerName, Serializer serializer)
        {
        String                          cacheName   = "test-cache";
        NamedCache<String, String>      cache       = ensureCache(cacheName);
        DeactivationListener            listener    = mock(DeactivationListener.class);
        NamedCache<String, String>      grpcCache   = createClient(cacheName, sSerializerName, serializer);
        AsyncNamedCache<String, String> asyncClient = grpcCache.async();

        if (asyncClient instanceof SafeAsyncNamedCache)
            {
            asyncClient = ((SafeAsyncNamedCache) asyncClient).getAsyncNamedCache();
            }
        ((AsyncNamedCacheClient) asyncClient).addDeactivationListener(listener);

        grpcCache.destroy();

        Eventually.assertDeferred(cache::isDestroyed, is(true));
        Eventually.assertDeferred(grpcCache::isDestroyed, is(true));

        assertThat(grpcCache.isActive(), is(false));
        verify(listener).destroyed(same(asyncClient));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldReleaseCache(String sSerializerName, Serializer serializer)
        {
        String     cacheName = "test-cache";
        NamedCache cache     = ensureCache(cacheName);

        DeactivationListener<AsyncNamedCache<? super String, ? super String>> listener = mock(DeactivationListener.class);

        NamedCache<String, String>      grpcClient  = createClient(cacheName, sSerializerName, serializer);
        AsyncNamedCache<String, String> asyncClient = grpcClient.async();

        if (asyncClient instanceof SafeAsyncNamedCache)
            {
            asyncClient = ((SafeAsyncNamedCache) asyncClient).getAsyncNamedCache();
            }
        ((AsyncNamedCacheClient) asyncClient).addDeactivationListener(listener);

        grpcClient.release();

        assertThat(cache.isDestroyed(), is(false));
        assertThat(cache.isReleased(), is(false));
        assertThat(grpcClient.isReleased(), is(true));
        assertThat(grpcClient.isDestroyed(), is(false));
        assertThat(grpcClient.isActive(), is(false));
        verify(listener).released(same(asyncClient));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetExistingKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.get("key-2");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.get("key-2");
        assertThat(result, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.get("missing-key");
        assertThat(result, is(nullValue()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetOrDefaultForExistingKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.getOrDefault("key-2", "default-value");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.getOrDefault("key-2", "default-value");
        assertThat(result, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetOrDefaultForNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.getOrDefault("missing-key", "default-value");
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

        NamedCache<String, String> grpcClient = createClient(cache.getCacheName(), sSerializerName, serializer);
        Map<String, String>        results    = grpcClient.getAll(keys);
        assertThat(results, is(expected));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldBeEmpty(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.isEmpty();
        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotBeEmpty(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 10);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.isEmpty();
        assertThat(result, is(false));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldInsertNewEntry(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.put("key-1", "value-1");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.put("key-1", "value-1", 500000L);
        assertThat(result, is(nullValue()));

        assertThat(cache.get("key-1"), is("value-1"));

        // update the entry with a short TTL
        grpcClient.put("key-1", "value-1", 1000L);
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.put("key-1", "value-2");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.put("key-1", "value-2");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.put("key-1", null);
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.putIfAbsent("key-1", "value-1");

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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.putIfAbsent("key-1", "value-2");

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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.putIfAbsent("key-1", "value-2");

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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        grpcClient.putAll(map);

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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        grpcClient.putAll(new HashMap<>());

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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.remove("key-100");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.remove("key-1");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result = grpcClient.remove("key-123", "value-123");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.remove("key-1", "value-123");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.remove("key-1", "value-1");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.replace("key-1", "value-123");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.replace("key-1", "value-123");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.replace("key-1", "value-1", "value-123");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.replace("key-1", "value-123", "value-456");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.replace("key-1", "value-1", "value-123");
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        grpcClient.replaceAll(keys, (k, v) ->
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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        grpcClient.replaceAll(filter, (k, v) ->
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

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        int                              size    = grpcClient.size();
        assertThat(size, is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetSizeOfPopulatedCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 10);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        int                              size    = grpcClient.size();
        assertThat(size, is(cache.size()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldTruncate(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.truncate();

        assertThat(cache.isEmpty(), is(true));
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
        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Set<String> keys = grpcClient.keySet(filter);

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
        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Set<Map.Entry<String, Person>> entries = grpcClient.entrySet(filter);

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

        NamedCache<String, Integer> grpcClient = createClient(cacheName, sSerializerName, serializer);

        //noinspection ConstantConditions
        int newValue = grpcClient.compute("k1", (k, v) -> v + v);
        assertThat(newValue, is(2));

        grpcClient.compute("k2", (k, v) -> null);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        String lastName = grpcClient.invoke(key, processor);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> map = grpcClient.invokeAll(filter, processor);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> map = grpcClient.invokeAll(AlwaysFilter.INSTANCE, processor);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> map = grpcClient.invokeAll(keys, processor);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        int result = grpcClient.aggregate(filter, aggregator);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);
        int                              result  = grpcClient.aggregate(filter, aggregator);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        int result = grpcClient.aggregate(keys, aggregator);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        int result = grpcClient.aggregate(keys, aggregator);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        int result = grpcClient.aggregate(aggregator);
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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> result = grpcClient.aggregate(filter, aggregator);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Map<String, String> result = grpcClient.aggregate(keys, aggregator);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Collection<Person> result = grpcClient.values(filter);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Collection<Person> result = grpcClient.values(filter);

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

        NamedCache<String, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Collection<Person> result = grpcClient.values(filter);

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

        NamedCache<String, Integer> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Collection<Integer> result = grpcClient.values(filter, comparator);

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

        NamedCache<String, Integer> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Collection<Integer> result = grpcClient.values(filter, comparator);

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

        NamedCache<String, Integer> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Collection<Integer> result = grpcClient.values(filter, comparator);

        assertThat(result, containsInAnyOrder(expected.toArray()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForSingleKey(String sSerializerName, Serializer serializer) throws Exception
        {
        String                                cacheName     = "test-events-" + System.currentTimeMillis();
        NamedCache<String, String>            cache         = ensureCache(cacheName);
        CollectingMapListener<String, String> listenerCache = new CollectingMapListener<>(9);
        cache.clear();
        cache.addMapListener(listenerCache, "key-2", true);

        NamedCache<String, String>            grpcClient  = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerOne = new CollectingMapListener<>(3);
        CollectingMapListener<String, String> listenerTwo = new CollectingMapListener<>(6);

        // Add both listeners
        grpcClient.addMapListener(listenerOne, "key-2", true);
        grpcClient.addMapListener(listenerTwo, "key-2", true);

        // update the cache to generate events
        generateCacheEventsForStringValue(cache);

        assertThat(listenerOne.awaitEvents(1, TimeUnit.MINUTES), is(true));
        assertThat("Incorrect number of insert events", listenerOne.getInsertCount(), is(1));
        assertThat("Incorrect number of update events", listenerOne.getUpdateCount(), is(1));
        assertThat("Incorrect number of delete events", listenerOne.getDeleteCount(), is(1));

        // remove the listenerOne - listener two should still get events
        grpcClient.removeMapListener(listenerOne, "key-2");

        // update the cache to generate events
        generateCacheEventsForStringValue(cache);

        assertThat(listenerTwo.awaitEvents(1, TimeUnit.MINUTES), is(true));
        assertThat("Incorrect number of insert events", listenerOne.getInsertCount(), is(1));
        assertThat("Incorrect number of update events", listenerOne.getUpdateCount(), is(1));
        assertThat("Incorrect number of delete events", listenerOne.getDeleteCount(), is(1));
        assertThat("Incorrect number of insert events", listenerTwo.getInsertCount(), is(2));
        assertThat("Incorrect number of update events", listenerTwo.getUpdateCount(), is(2));
        assertThat("Incorrect number of delete events", listenerTwo.getDeleteCount(), is(2));

        // remove the listenerTwo
        grpcClient.removeMapListener(listenerTwo, "key-2");

        // update the cache to generate events
        generateCacheEventsForStringValue(cache);

        assertThat(listenerCache.awaitEvents(1, TimeUnit.MINUTES), is(true));
        assertThat("Incorrect number of insert events", listenerOne.getInsertCount(), is(1));
        assertThat("Incorrect number of update events", listenerOne.getUpdateCount(), is(1));
        assertThat("Incorrect number of delete events", listenerOne.getDeleteCount(), is(1));
        assertThat("Incorrect number of insert events", listenerTwo.getInsertCount(), is(2));
        assertThat("Incorrect number of update events", listenerTwo.getUpdateCount(), is(2));
        assertThat("Incorrect number of delete events", listenerTwo.getDeleteCount(), is(2));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForMultipleKeys(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String>      grpcClient      = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingMapListener<>(6);

        grpcClient.addMapListener(listenerTest, "key-2", true);
        grpcClient.addMapListener(listenerTest, "key-4", true);

        // update the cache to generate events
        generateCacheEventsForStringValue(cache);

        assertThat(listenerTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat("Incorrect number of insert events", listenerTest.getInsertCount(), is(2));
        assertThat("Incorrect number of update events", listenerTest.getUpdateCount(), is(2));
        assertThat("Incorrect number of delete events", listenerTest.getDeleteCount(), is(2));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForAllKeys(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String>            grpcClient   = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingMapListener<>(30);

        grpcClient.addMapListener(listenerTest);

        // update the cache to generate events
        generateCacheEventsForStringValue(cache);

        assertThat(listenerTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat("Incorrect number of insert events", listenerTest.getInsertCount(), is(10));
        assertThat("Incorrect number of update events", listenerTest.getUpdateCount(), is(10));
        assertThat("Incorrect number of delete events", listenerTest.getDeleteCount(), is(10));
        }

    private static void generateCacheEventsForStringValue(NamedCache<String, String> cache)
        {
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
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForFilter(String sSerializerName, Serializer serializer) throws Exception
        {
        String                                 cacheName     = "test-events-" + System.currentTimeMillis();
        NamedCache<String, Integer>            cache         = ensureCache(cacheName);
        MapEventFilter<String, Integer>        filter        = new MapEventFilter<>(Filters.less(Extractors.identity(), 10));
        CollectingMapListener<String, Integer> listenerCache = new CollectingMapListener<>(60);

        cache.clear();
        cache.addMapListener(listenerCache, filter, false);

        NamedCache<String, Integer>            grpcClient  = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, Integer> listenerOne = new CollectingMapListener<>(20);
        CollectingMapListener<String, Integer> listenerTwo = new CollectingMapListener<>(40);

        grpcClient.addMapListener(listenerOne, filter, false);
        grpcClient.addMapListener(listenerTwo, filter, false);

        // update the cache to generate events
        generateCacheEvents(cache);

        assertThat(listenerOne.awaitEvents(1, TimeUnit.MINUTES), is(true));
        assertThat("Incorrect number of insert events", listenerOne.getInsertCount(), is(10));
        assertThat("Incorrect number of update events", listenerOne.getUpdateCount(), is(10));
        assertThat("Incorrect number of delete events", listenerOne.getDeleteCount(), is(0));
        Eventually.assertDeferred(listenerTwo::getInsertCount, is(10));
        Eventually.assertDeferred(listenerTwo::getUpdateCount, is(10));
        Eventually.assertDeferred(listenerTwo::getDeleteCount, is(0));

        grpcClient.removeMapListener(listenerOne, filter);
        generateCacheEvents(cache);

        assertThat(listenerTwo.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat("Incorrect number of insert events", listenerOne.getInsertCount(), is(10));
        assertThat("Incorrect number of update events", listenerOne.getUpdateCount(), is(10));
        assertThat("Incorrect number of delete events", listenerOne.getDeleteCount(), is(0));
        assertThat("Incorrect number of insert events", listenerTwo.getInsertCount(), is(20));
        assertThat("Incorrect number of update events", listenerTwo.getUpdateCount(), is(20));
        assertThat("Incorrect number of delete events", listenerTwo.getDeleteCount(), is(0));

        grpcClient.removeMapListener(listenerTwo, filter);
        generateCacheEvents(cache);

        assertThat(listenerCache.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat("Incorrect number of insert events", listenerOne.getInsertCount(), is(10));
        assertThat("Incorrect number of update events", listenerOne.getUpdateCount(), is(10));
        assertThat("Incorrect number of delete events", listenerOne.getDeleteCount(), is(0));
        assertThat("Incorrect number of insert events", listenerTwo.getInsertCount(), is(20));
        assertThat("Incorrect number of update events", listenerTwo.getUpdateCount(), is(20));
        assertThat("Incorrect number of delete events", listenerTwo.getDeleteCount(), is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Disabled
    void shouldSubscribeToEventsForKeyAndFilter(String sSerializerName, Serializer serializer) throws Exception
        {
        String                      cacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, Integer> cache     = ensureCache(cacheName);
        cache.clear();

        MapEventFilter<String, Integer> filter = new MapEventFilter<>(Filters.less(Extractors.identity(), 10));

        NamedCache<String, Integer>      grpcClient            = createClient(cacheName, sSerializerName,
                                                                              serializer);
        CollectingMapListener<String, Integer> listenerFilterTest = new CollectingMapListener<>(20);
        CollectingMapListener<String, Integer> listenerKeyTest    = new CollectingMapListener<>(3);

        grpcClient.addMapListener(listenerFilterTest, filter, false);
        grpcClient.addMapListener(listenerKeyTest, "key-2", false);

        // update the cache to generate events
        generateCacheEvents(cache);

        assertThat(listenerFilterTest.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat("Incorrect number of insert events (filter)", listenerFilterTest.getInsertCount(), is(10));
        assertThat("Incorrect number of update events (filter)", listenerFilterTest.getUpdateCount(), is(10));
        assertThat("Incorrect number of delete events (filter)", listenerFilterTest.getDeleteCount(), is(0));

        assertThat("Incorrect number of insert events (key)", listenerKeyTest.getInsertCount(), is(3));
        assertThat("Incorrect number of update events (key)", listenerKeyTest.getUpdateCount(), is(3));
        assertThat("Incorrect number of delete events (key)", listenerKeyTest.getDeleteCount(), is(3));
        }

    private static void generateCacheEvents(NamedCache<String, Integer> cache)
        {
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
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldReceiveDeactivationEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events-" + System.currentTimeMillis();  // make cache name unique
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        TestDeactivationListener         listener = new TestDeactivationListener(1);
        NamedCache<String, String> grpcClient  = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addMapListener(listener);

        cache.destroy();

        assertThat(listener.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listener.isDestroyed(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldReceiveTruncateEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events-" + System.currentTimeMillis();  // make cache name unique
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("foo", "bar");

        TestDeactivationListener         listener = new TestDeactivationListener(1);
        NamedCache<String, String> grpcClient  = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addMapListener(listener);

        cache.truncate();

        assertThat(listener.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listener.isTruncated(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldReceiveTruncateAndDeactivationEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events-" + System.currentTimeMillis();  // make cache name unique
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "val-1");

        TestDeactivationListener         listener = new TestDeactivationListener(2);
        NamedCache<String, String> grpcClient  = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addMapListener(listener);

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
        String                     cacheName = "test-events-" + System.currentTimeMillis();  // make cache name unique
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "val-1");

        CollectingMapListener<String, String> listenerCache = new CollectingPrimingListener<>(3);
        cache.addMapListener(listenerCache, "key-1", true);

        cache.put("key-2", "val-2");

        NamedCache<String, String>            grpcClient   = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingPrimingListener<>(2);
        grpcClient.addMapListener(listenerTest, "key-2", true);

        assertThat(listenerCache.getUpdateCount(), is(1));
        assertThat(listenerTest.getUpdateCount(), is(1));

        cache.put("key-2", "val-22");
        cache.put("key-1", "val-11");

        assertThat(listenerTest.awaitEvents(1, TimeUnit.MINUTES), is(true));

        grpcClient.removeMapListener(listenerTest, "key-2");

        cache.put("key-2", "val-222");
        cache.put("key-1", "val-11");

        assertThat(listenerCache.awaitEvents(1, TimeUnit.MINUTES), is(true));
        assertThat(listenerCache.getUpdateCount(), is(3));
        assertThat(listenerTest.getUpdateCount(), is(2));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldAddPrimingListenerForNonExistingKey(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-events-" + System.currentTimeMillis();  // make cache name unique
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        CollectingMapListener<String, String> listenerExpected = new CollectingPrimingListener<>(2);
        cache.addMapListener(listenerExpected, "key-1", true);

        NamedCache<String, String>            grpcClient   = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<String, String> listenerTest = new CollectingPrimingListener<>(2);
        grpcClient.addMapListener(listenerTest, "key-2", true);

        Eventually.assertDeferred(listenerExpected::getUpdateCount, is(1));
        Eventually.assertDeferred(listenerTest::getUpdateCount, is(1));

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

        // with the trigger added the person's last name should be converted to upper case
        Person ironMan = new Person("Tony", "Stark", 53, "male");
        cache.put("iron.man", ironMan);
        Person cached = cache.get("iron.man");

        assertThat(cached.getLastName(), is(ironMan.getLastName().toUpperCase()));

        cache.removeMapListener(listener);

        // with the trigger removed the person's last name should be unchanged
        Person luke = new Person("Luke", "Skywalker", 100, "male");
        cache.put("luke", luke);
        cached = cache.get("luke");
        assertThat(cached.getLastName(), is(luke.getLastName()));
        }

    // ----- helper methods -------------------------------------------------


    protected abstract <K, V> NamedCache<K, V> createClient(String sCacheName, String sSerializerName, Serializer serializer);

    protected abstract <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader);

    /**
     * Obtain the {@link Serializer} instances to use for parameterized
     * test {@link Arguments}.
     *
     * @return the {@link Serializer} instances to use for test
     * {@link Arguments}
     */
    protected static Stream<Arguments> serializers()
        {
        ClassLoader loader = Base.getContextClassLoader();
        TreeMap<String, Serializer> map = new TreeMap<>();

        map.put("", new DefaultSerializer());
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
         * @see CountDownLatch#await(long, TimeUnit)
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
         * @see CountDownLatch#await(long, TimeUnit)
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

    // ----- data members ---------------------------------------------------

    protected final String f_sScopeName;
    }
