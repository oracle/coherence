/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.grpc.client.common.AsyncNamedCacheClient;
import com.oracle.coherence.grpc.client.common.DeactivationListener;
import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.oracle.coherence.grpc.client.common.GrpcRemoteCacheService;
import com.oracle.coherence.grpc.client.common.NamedCacheClient;
import com.oracle.coherence.grpc.client.common.NamedCacheClientChannel;
import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;
import com.tangosol.coherence.component.util.SafeAsyncNamedCache;

import com.tangosol.coherence.component.util.SafeNamedCache;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.internal.net.SessionNamedCache;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.net.partition.KeyPartitioningStrategy;
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

import com.tangosol.util.listener.SimpleMapListener;
import com.tangosol.util.processor.ExtractorProcessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
public abstract class AbstractGrpcClientIT
    {
    protected AbstractGrpcClientIT()
        {
        this(GrpcDependencies.DEFAULT_SCOPE);
        }

    public AbstractGrpcClientIT(String sScopeName)
        {
        f_sScopeName = sScopeName;
        }

    @BeforeEach
    public void beforeEachTest(TestInfo info)
        {
        String sClass  = info.getTestClass().map(Class::toString).orElse("");
        String sMethod = info.getTestMethod().map(Method::toString).orElse("");
        System.err.println(">>>>>>> Starting test " + sClass + "." + sMethod + " - " + info.getDisplayName());
        m_nStart = System.currentTimeMillis();
        }

    @AfterEach
    public void afterEachTest(TestInfo info)
        {
        String sClass  = info.getTestClass().map(Class::toString).orElse("");
        String sMethod = info.getTestMethod().map(Method::toString).orElse("");
        long   cMillis = System.currentTimeMillis() - m_nStart;
        System.err.println(">>>>>>> Finished test " + sClass + "." + sMethod + " - " + info.getDisplayName() + " (" + cMillis + " ms)");
        }

    // ----- test methods ---------------------------------------------------


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldThrowException(String sSerializerName, Serializer serializer)
        {
        String     cacheName = createCacheName();
        NamedCache cache     = ensureCache(cacheName);

        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assertThrows(RequestIncompleteException.class,
                () -> grpcClient.aggregate(new UnusableAggregator<>()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldClearEmptyCache(String sSerializerName, Serializer serializer)
        {
        String     cacheName = createCacheName();
        NamedCache cache     = ensureCache(cacheName);

        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.clear();

        assertThat(cache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldBeReady(String sSerializerName, Serializer serializer)
        {
        String                     cacheName  = createCacheName();
        NamedCache                 cache      = ensureCache(cacheName);
        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assertThat(grpcClient.isReady(), is(cache.isReady()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldClearPopulatedCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);

        clearAndPopulate(cache, 10);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.clear();

        Eventually.assertDeferred(cache::isEmpty, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnTrueForContainsKeyWithExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);

        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.containsKey("key-2");

        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnTrueForContainsAssociatedKeyWithExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);

        clearAndPopulateAssociated(cache, "foo", 5);

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        TestAssociatedKey                     key        = new TestAssociatedKey("key-2", "foo");
        boolean                               result     = grpcClient.containsKey(key);

        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnFalseForContainsKeyWithNonExistentMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.containsKey("missing-key");
        assertThat(result, is(false));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldContainValueWhenValuePresent(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 3);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.containsValue("value-2");
        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldContainValueWhenValuePresentMultipleTimes(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldNotContainValueWhenMappingNotPresent(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 3);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.containsValue("value-100");
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldDestroyCache(String sSerializerName, Serializer serializer)
        {
        String                          cacheName   = createCacheName();
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
    public void shouldReleaseCache(String sSerializerName, Serializer serializer)
        {
        String     cacheName = createCacheName();
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
    public void shouldGetExistingKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.get("key-2");
        assertThat(result, is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetExistingAssociatedKey(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        clearAndPopulateAssociated(cache, "foo", 5);

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        TestAssociatedKey                     key        = new TestAssociatedKey("key-2", "foo");
        String                                result     = grpcClient.get(key);
        assertThat(result, is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetExistingKeyMappedToNull(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);
        cache.put("key-2", null);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.get("key-2");
        assertThat(result, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.get("missing-key");
        assertThat(result, is(nullValue()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetOrDefaultForExistingKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.getOrDefault("key-2", "default-value");
        assertThat(result, is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetOrDefaultForExistingKeyMappedToNull(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);
        cache.put("key-2", null);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.getOrDefault("key-2", "default-value");
        assertThat(result, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetOrDefaultForNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.getOrDefault("missing-key", "default-value");
        assertThat(result, is("default-value"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetAllForEmptyKeyCollection(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        assertGetAll(cache, sSerializerName, serializer, Collections.emptyList());
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetAllWhenNoKeysMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 4);

        Collection<String> keys = Arrays.asList("key-5", "key-6");

        assertGetAll(cache, sSerializerName, serializer, keys);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetAllWhenAllKeysMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 4);

        Collection<String> keys = Arrays.asList("key-2", "key-4");

        assertGetAll(cache, sSerializerName, serializer, keys);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetAllWhenAllAssociatedKeysMatch(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        clearAndPopulateAssociated(cache, "foo", 4);

        Collection<TestAssociatedKey> keys = Arrays.asList(
                new TestAssociatedKey("key-2", "foo"),
                new TestAssociatedKey("key-4", "foo"));

        assertGetAll(cache, sSerializerName, serializer, keys);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetAllWhenAllSomeKeysMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 4);

        Collection<String> keys = Arrays.asList("key-0", "key-2", "key-4", "key-5");

        assertGetAll(cache, sSerializerName, serializer, keys);
        }

    <K> void assertGetAll(NamedCache<K, String> cache, String sSerializerName, Serializer serializer, Collection<K> keys)
        {
        NamedCache<K, String> grpcClient = createClient(cache.getCacheName(), sSerializerName, serializer);
        assertGetAll(cache, grpcClient, keys);
        }

    <K> void assertGetAll(NamedCache<K, String> cache, NamedCache<K, String> grpcClient, Collection<K> keys)
        {
        Map<K, String> expected = cache.getAll(keys);
        Map<K, String> results    = grpcClient.getAll(keys);
        assertThat(results, is(expected));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldBeEmpty(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.isEmpty();
        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldNotBeEmpty(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 10);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.isEmpty();
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldInsertNewEntry(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.put("key-1", "value-1");
        assertThat(result, is(nullValue()));

        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldInsertNewAssociatedEntry(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        cache.clear();

        TestAssociatedKey                     key        = new TestAssociatedKey("key-1", "foo");
        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                                result     = grpcClient.put(key, "value-1");
        assertThat(result, is(nullValue()));

        assertThat(cache.get(key), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldInsertNewAssociatedEntriesToSamePartition(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        TestAssociatedKey                     key1      = new TestAssociatedKey("key-1", "foo");
        TestAssociatedKey                     key2      = new TestAssociatedKey("key-2", "foo");
        TestAssociatedKey                     key3      = new TestAssociatedKey("key-3", "foo");
        TestAssociatedKey                     key4      = new TestAssociatedKey("key-4", "foo");

        cache.clear();

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.put(key1, "value-1");
        grpcClient.put(key2, "value-2");
        grpcClient.put(key3, "value-3");
        grpcClient.put(key4, "value-4");

        GetEntryPartition<TestAssociatedKey, String> processor = new GetEntryPartition<>();
        int                                          nPart1    = cache.invoke(key1, processor);
        int                                          nPart2    = cache.invoke(key2, processor);
        int                                          nPart3    = cache.invoke(key3, processor);
        int                                          nPart4    = cache.invoke(key4, processor);

        assertThat(nPart2, is(nPart1));
        assertThat(nPart3, is(nPart1));
        assertThat(nPart4, is(nPart1));
        }



    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldUpdateNewEntryWithNewExpiry(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        long cMillisInitial = 1500000L;
        long cExpireInitial = System.currentTimeMillis() + cMillisInitial - 1L;

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.put("key-1", "value-1", cMillisInitial);
        assertThat(result, is(nullValue()));

        long nExpiryActual = cache.invoke("key-1", GetEntryExpiry.instance());
        assertThat(nExpiryActual, is(greaterThan(cExpireInitial)));

        assertThat(cache.get("key-1"), is("value-1"));

        long cMillisUpdate = 200000L;
        long cExpireUpdate = System.currentTimeMillis() + cMillisUpdate - 1L;

        // update the entry with a short TTL
        grpcClient.put("key-1", "value-1", cMillisUpdate);

        nExpiryActual = cache.invoke("key-1", GetEntryExpiry.instance());
        assertThat(nExpiryActual, is(greaterThan(cExpireUpdate)));
        assertThat(nExpiryActual, is(lessThan(cExpireInitial)));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldUpdateEntry(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldUpdateEntryPreviouslyMappedToNull(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldUpdateEntryWithNullValue(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldPutIfAbsentForNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.putIfAbsent("key-1", "value-1");

        assertThat(result, is(nullValue()));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPutIfAbsentForExistingKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldPutIfAbsentForExistingAssociatedKey(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        TestAssociatedKey                     key       = new TestAssociatedKey("key-1", "foo");
        cache.clear();
        cache.put(key, "value-1");

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                                result     = grpcClient.putIfAbsent(key, "value-2");

        assertThat(result, is("value-1"));
        assertThat(cache.get(key), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPutIfAbsentForExistingKeyMappedToNullValue(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", null);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                     result     = grpcClient.putIfAbsent("key-1", "value-2");

        assertThat(result, is(nullValue()));
        assertThat(cache.get("key-1"), is("value-2"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPutAllEntries(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldPutAllAssociatedEntries(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        cache.clear();

        Map<TestAssociatedKey, String> map = new HashMap<>();
        TestAssociatedKey              key1 = new TestAssociatedKey("key-1", "foo");
        TestAssociatedKey              key2 = new TestAssociatedKey("key-2", "foo");
        map.put(key1, "value-1");
        map.put(key2, "value-2");

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        grpcClient.putAll(map);

        assertThat(cache.size(), is(2));
        assertThat(cache.get(key1), is("value-1"));
        assertThat(cache.get(key2), is("value-2"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPutAllWithExpiry(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = "test-cache-" + sSerializerName;
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        Map<String, String> map = new HashMap<>();
        map.put("key-1", "value-1");
        map.put("key-2", "value-2");

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        long cMillis = 50000L;
        long cExpire = System.currentTimeMillis() + cMillis - 1L;

        AsyncNamedCache<String, String> async = grpcClient.async();
        async.putAll(map, cMillis).get(1, TimeUnit.MINUTES);
        long nExpiry1 = cache.invoke("key-1", GetEntryExpiry.instance());
        long nExpiry2 = cache.invoke("key-2", GetEntryExpiry.instance());
        assertThat(nExpiry1, is(greaterThan(cExpire)));
        assertThat(nExpiry2, is(greaterThan(cExpire)));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPutAllWithZeroEntries(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        grpcClient.putAll(new HashMap<>());

        assertThat(cache.isEmpty(), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveOnNonExistentEntry(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldReturnPreviousValueForRemoveOnExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldReturnPreviousAssociatedValueForRemoveOnExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        int                                   count     = 10;

        clearAndPopulateAssociated(cache, "foo", count);

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        TestAssociatedKey                     key        = new TestAssociatedKey("key-1", "foo");
        String                                result     = grpcClient.remove(key);

        assertThat(result, is("value-1"));
        assertThat(cache.get(key), is(nullValue()));
        assertThat(cache.size(), is(count - 1));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnFalseForRemoveMappingOnNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result = grpcClient.remove("key-123", "value-123");
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnFalseForRemoveMappingOnNonMatchingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldReturnTrueForRemoveMappingOnMatchingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                    result     = grpcClient.remove("key-1", "value-1");
        assertThat(result, is(true));
        assertThat(cache.containsKey("key-1"), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnTrueForRemoveMappingOnMatchingMappingWithAssociatedKey(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        TestAssociatedKey                     key       = new TestAssociatedKey("key-1", "foo");
        cache.clear();
        cache.put(key, "value-1");

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                               result     = grpcClient.remove(key, "value-1");
        assertThat(result, is(true));
        assertThat(cache.containsKey(key), is(false));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnNullValueForReplaceOnNonExistentMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.replace("key-1", "value-123");
        assertThat(result, is(nullValue()));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnNonNullForReplaceOnExistentMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                           result  = grpcClient.replace("key-1", "value-123");
        assertThat(result, is("value-1"));
        assertThat(cache.get("key-1"), is("value-123"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnNonNullForReplaceOnExistentMappingWithAssociatedKey(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);
        TestAssociatedKey                     key       = new TestAssociatedKey("key-1", "foo");

        clearAndPopulateAssociated(cache, "foo", 5);

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        String                                result     = grpcClient.replace(key, "value-123");
        assertThat(result, is("value-1"));
        assertThat(cache.get(key), is("value-123"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnFalseForReplaceMappingOnNonExistentKey(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.replace("key-1", "value-1", "value-123");
        assertThat(result, is(false));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnFalseForReplaceMappingOnNonMatchingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.replace("key-1", "value-123", "value-456");
        assertThat(result, is(false));
        assertThat(cache.get("key-1"), is("value-1"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnTrueForReplaceMappingOnMatchingMapping(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        boolean                          result  = grpcClient.replace("key-1", "value-1", "value-123");
        assertThat(result, is(true));
        assertThat(cache.get("key-1"), is("value-123"));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReplaceAllWithKeySet(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldReplaceAllWithAssociatedKeySet(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, String> cache     = ensureCache(cacheName);

        clearAndPopulateAssociated(cache, "foo", 5);

        List<TestAssociatedKey> keys           = new ArrayList<>(cache.keySet());
        Object[]                expectedValues = new ArrayList<>(cache.values()).stream().map(v -> v + '1').toArray();

        NamedCache<TestAssociatedKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

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
    public void shouldReplaceAllWithFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldGetSizeOfEmptyCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        int                              size    = grpcClient.size();
        assertThat(size, is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetSizeOfPopulatedCache(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 10);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        int                              size    = grpcClient.size();
        assertThat(size, is(cache.size()));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldTruncate(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
        NamedCache<String, String> cache     = ensureCache(cacheName);
        clearAndPopulate(cache, 5);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.truncate();

        assertThat(cache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnKeySetWithFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldReturnEntrySetWithFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldComputeAndUpdateEntry(String sSerializerName, Serializer serializer)
        {
        String                      cacheName = createCacheName();
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
    public void shouldComputeAndUpdateEntryWithAssociatedKey(String sSerializerName, Serializer serializer)
        {
        String                                 cacheName = createCacheName();
        NamedCache<TestAssociatedKey, Integer> cache     = ensureCache(cacheName);
        TestAssociatedKey                      key1      = new TestAssociatedKey("k1", "foo");
        TestAssociatedKey                      key2      = new TestAssociatedKey("k2", "foo");
        cache.clear();
        cache.put(key1, 1);
        cache.put(key2, 2);

        NamedCache<TestAssociatedKey, Integer> grpcClient = createClient(cacheName, sSerializerName, serializer);

        //noinspection ConstantConditions
        int newValue = grpcClient.compute(key1, (k, v) -> v + v);
        assertThat(newValue, is(2));

        grpcClient.compute(key2, (k, v) -> null);

        assertThat(cache.get(key2), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldCallInvoke(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName();
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
    public void shouldCallInvokeWithAssociatedKey(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName();
        NamedCache<TestAssociatedKey, Person> cache     = ensureCache(cacheName);
        TestAssociatedKey                     key       = new TestAssociatedKey("bb", "foo");

        cache.clear();

        Person person = new Person("bob", "builder", 25, "male");
        cache.put(key, person);

        ValueExtractor<Person, String>                                 extractor = new UniversalExtractor<>("lastName");
        InvocableMap.EntryProcessor<TestAssociatedKey, Person, String> processor = new ExtractorProcessor<>(extractor);

        NamedCache<TestAssociatedKey, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        String lastName = grpcClient.invoke(key, processor);

        assertThat(lastName, is(notNullValue()));
        assertThat(lastName, is("builder"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallInvokeAllWithAlwaysFilter(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallInvokeAllWithKeys(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallInvokeAllWithAssociatedKeys(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName("people");
        NamedCache<TestAssociatedKey, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 25, "male");

        TestAssociatedKey key1 = new TestAssociatedKey(person1.getLastName(), "foo");
        TestAssociatedKey key2 = new TestAssociatedKey(person2.getLastName(), "foo");
        TestAssociatedKey key3 = new TestAssociatedKey(person3.getLastName(), "foo");
        cache.put(key1, person1);
        cache.put(key2, person2);
        cache.put(key3, person3);

        ValueExtractor<Person, String>                                 extractor = new UniversalExtractor<>("firstName");
        InvocableMap.EntryProcessor<TestAssociatedKey, Person, String> processor = new ExtractorProcessor<>(extractor);
        List<TestAssociatedKey>                                        keys      = Arrays.asList(key1, key2);

        NamedCache<TestAssociatedKey, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        Map<TestAssociatedKey, String> map = grpcClient.invokeAll(keys, processor);

        assertThat(map, hasEntry(key1, person1.getFirstName()));
        assertThat(map, hasEntry(key2, person2.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldHeartbeat(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName  = createCacheName("foo");
        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        if (grpcClient instanceof SessionNamedCache)
            {
            grpcClient = ((SessionNamedCache) grpcClient).getInternalNamedCache();
            }
        if (grpcClient instanceof SafeNamedCache)
            {
            grpcClient = ((SafeNamedCache) grpcClient).getNamedCache();
            }

        assertThat(grpcClient, is(instanceOf(NamedCacheClient.class)));

        NamedCacheClient<?, ?>             client       = (NamedCacheClient) grpcClient;
        AsyncNamedCacheClient<?, ?>        asyncClient  = client.getAsyncClient();
        NamedCacheClientChannel            channel      = asyncClient.getClientProtocol();
        AsyncNamedCacheClient.Dependencies dependencies = channel.getDependencies();

        long    nMillis = dependencies.getHeartbeatMillis();
        boolean fAck    = dependencies.isRequireHeartbeatAck();
        Assumptions.assumeTrue(channel.getVersion() >= 1, "Skipping test, protocol is less then version 1");
        Assumptions.assumeTrue(nMillis > 0L, "Skipping test, heart beats are not configured");
        Assumptions.assumeTrue(fAck, "Skipping test, heart beat acks are not configured");

        GrpcConnection connection = channel.getConnection();
        long           nTimestamp = connection.getLastHeartbeatTime();
        long           cSent      = connection.getHeartbeatsSent();

        Eventually.assertDeferred(connection::getHeartbeatsSent, is(greaterThan(cSent)));
        Eventually.assertDeferred(connection::getHeartbeatsAcked, is(greaterThan(cSent)));
        assertThat(connection.getLastHeartbeatTime(), is(greaterThan(nTimestamp)));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithFilterExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallAggregateWithFilterMatchingNoEntriesExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallAggregateWithKeysExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallAggregateWithAssociatedKeysExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                                cacheName = createCacheName("people");
        NamedCache<TestAssociatedKey, Person> cache     = ensureCache(cacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");

        TestAssociatedKey key1 = new TestAssociatedKey(person1.getLastName(), "foo");
        TestAssociatedKey key2 = new TestAssociatedKey(person2.getLastName(), "foo");
        TestAssociatedKey key3 = new TestAssociatedKey(person3.getLastName(), "foo");
        cache.put(key1, person1);
        cache.put(key2, person2);
        cache.put(key3, person3);

        InvocableMap.EntryAggregator<TestAssociatedKey, Person, Integer> aggregator = new Count<>();
        List<TestAssociatedKey>                                          keys       = Arrays.asList(key1, key2);

        int expected = cache.aggregate(keys, aggregator);

        NamedCache<TestAssociatedKey, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        int result = grpcClient.aggregate(keys, aggregator);

        assertThat(result, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithKeysMatchingNoEntriesExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallAggregateWithNoKeysOrFilterExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallAggregateWithFilterExpectingMapResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallAggregateWithKeysExpectingMapResult(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallValuesWithFilterWhenSomeEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallValuesWithFilterWhenAllEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallValuesWithFilterWhenNoEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("people");
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
    public void shouldCallValuesWithFilterAndComparatorWhenSomeEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                      cacheName = createCacheName("numbers");
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
    public void shouldCallValuesWithFilterAndComparatorWhenAllEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                      cacheName = createCacheName("numbers");
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
    public void shouldCallValuesWithFilterAndComparatorWhenNoEntriesMatch(String sSerializerName, Serializer serializer)
        {
        String                      cacheName = createCacheName("numbers");
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
    public void shouldSubscribeToEventsForSingleKey(String sSerializerName, Serializer serializer) throws Exception
        {
        assertSubscribeToEventsForSingleKey("key-2", AbstractGrpcClientIT::generateCacheEventsForStringValue,
                sSerializerName, serializer);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForSingleAssociatedKey(String sSerializerName, Serializer serializer) throws Exception
        {
        String            sAssociated = "foo";
        TestAssociatedKey key         = new TestAssociatedKey("key-2", sAssociated);

        assertSubscribeToEventsForSingleKey(key, cache -> generateCacheEventsForAssociatedKey(cache, sAssociated),
                sSerializerName, serializer);
        }

    <K> void assertSubscribeToEventsForSingleKey(K key, Consumer<NamedCache<K, String>> generateEvents,
            String sSerializerName, Serializer serializer) throws Exception
        {
        String                           cacheName     = "test-events-" + System.currentTimeMillis();
        NamedCache<K, String>            cache         = ensureCache(cacheName);
        NamedCache<K, String>            grpcClient    = createClient(cacheName, sSerializerName, serializer);
        CollectingMapListener<K, String> listenerCache = new CollectingMapListener<>(9);

        if (key instanceof KeyPartitioningStrategy.PartitionAwareKey)
            {
            assumeDeferKeyAssociation(cache);
            assumeDeferKeyAssociation(grpcClient);
            }

        cache.clear();
        cache.addMapListener(listenerCache, key, true);

        CollectingMapListener<K, String> listenerOne = new CollectingMapListener<>(3);
        CollectingMapListener<K, String> listenerTwo = new CollectingMapListener<>(6);

        // Add both listeners
        grpcClient.addMapListener(listenerOne, key, true);
        grpcClient.addMapListener(listenerTwo, key, true);

        // update the cache to generate events
        generateEvents.accept(cache);

        assertThat(listenerOne.awaitEvents(1, TimeUnit.MINUTES), is(true));
        assertThat("Incorrect number of insert events", listenerOne.getInsertCount(), is(1));
        assertThat("Incorrect number of update events", listenerOne.getUpdateCount(), is(1));
        assertThat("Incorrect number of delete events", listenerOne.getDeleteCount(), is(1));

        // remove the listenerOne - listener two should still get events
        grpcClient.removeMapListener(listenerOne, key);

        // update the cache to generate events
        generateEvents.accept(cache);

        assertThat(listenerTwo.awaitEvents(1, TimeUnit.MINUTES), is(true));
        assertThat("Incorrect number of insert events", listenerOne.getInsertCount(), is(1));
        assertThat("Incorrect number of update events", listenerOne.getUpdateCount(), is(1));
        assertThat("Incorrect number of delete events", listenerOne.getDeleteCount(), is(1));
        assertThat("Incorrect number of insert events", listenerTwo.getInsertCount(), is(2));
        assertThat("Incorrect number of update events", listenerTwo.getUpdateCount(), is(2));
        assertThat("Incorrect number of delete events", listenerTwo.getDeleteCount(), is(2));

        // remove the listenerTwo
        grpcClient.removeMapListener(listenerTwo, key);

        // update the cache to generate events
        generateEvents.accept(cache);

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
    public void shouldSubscribeToEventsForMultipleKeys(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = createCacheName("test-events");
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
    public void shouldSubscribeToEventsForAllKeys(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = createCacheName("test-events");
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

    private static void generateCacheEventsForAssociatedKey(NamedCache<TestAssociatedKey, String> cache, String sAssociated)
        {
        for (int i = 0; i < 10; i++)
            {
            cache.put(new TestAssociatedKey("key-" + i, sAssociated), "value-" + i);
            }
        for (int i = 0; i < 10; i++)
            {
            cache.put(new TestAssociatedKey("key-" + i, sAssociated), "value-" + i + "-updated");
            }
        for (int i = 0; i < 10; i++)
            {
            cache.remove(new TestAssociatedKey("key-" + i, sAssociated));
            }
        }

    protected static void generateCacheEventsForPartitionedKey(NamedCache<TestPartitionKey, String> cache, int nPart)
        {
        for (int i = 0; i < 10; i++)
            {
            cache.put(new TestPartitionKey("key-" + i, nPart), "value-" + i);
            }
        for (int i = 0; i < 10; i++)
            {
            cache.put(new TestPartitionKey("key-" + i, nPart), "value-" + i + "-updated");
            }
        for (int i = 0; i < 10; i++)
            {
            cache.remove(new TestPartitionKey("key-" + i, nPart));
            }
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForFilter(String sSerializerName, Serializer serializer) throws Exception
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
    public void shouldSubscribeToEventsForKeyAndFilter(String sSerializerName, Serializer serializer) throws Exception
        {
        String                      cacheName = createCacheName("test-events");
        NamedCache<String, Integer> cache     = ensureCache(cacheName);
        cache.clear();

        MapEventFilter<String, Integer> filter     = new MapEventFilter<>(Filters.less(Extractors.identity(), 10));
        NamedCache<String, Integer>     grpcClient = createClient(cacheName, sSerializerName, serializer);

        CollectingMapListener<String, Integer> listenerFilterTest = new CollectingMapListener<>(20);
        CollectingMapListener<String, Integer> listenerKeyTest    = new CollectingMapListener<>(3);

        grpcClient.addMapListener(listenerFilterTest, filter, false);
        grpcClient.addMapListener(listenerKeyTest, "key-2", false);

        // update the cache to generate events
        generateCacheEvents(cache);

        assertThat(listenerFilterTest.awaitEvents(30, TimeUnit.SECONDS), is(true));
        assertThat("Incorrect number of insert events (filter)", listenerFilterTest.getInsertCount(), is(10));
        assertThat("Incorrect number of update events (filter)", listenerFilterTest.getUpdateCount(), is(10));
        assertThat("Incorrect number of delete events (filter)", listenerFilterTest.getDeleteCount(), is(0));

        assertThat(listenerKeyTest.awaitEvents(30, TimeUnit.SECONDS), is(true));
        assertThat("Incorrect number of insert events (key)", listenerKeyTest.getInsertCount(), is(1));
        assertThat("Incorrect number of update events (key)", listenerKeyTest.getUpdateCount(), is(1));
        assertThat("Incorrect number of delete events (key)", listenerKeyTest.getDeleteCount(), is(1));
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
    public void shouldReceiveDeactivationEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = createCacheName("test-events");
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();

        TestDeactivationListener   listener    = new TestDeactivationListener(1);
        NamedCache<String, String> grpcClient  = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addMapListener(listener);

        cache.destroy();

        assertThat(listener.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listener.isDestroyed(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldReceiveTruncateEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = createCacheName("test-events");
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("foo", "bar");

        TestDeactivationListener   listener   = new TestDeactivationListener(1);
        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addMapListener(listener);

        cache.truncate();

        assertThat(listener.awaitEvents(3, TimeUnit.SECONDS), is(true));
        assertThat(listener.isTruncated(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")                                                                                                                                                      
    @SuppressWarnings("unchecked")
    public void shouldReceiveTruncateAndDeactivationEvent(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = createCacheName("test-events");
        NamedCache<String, String> cache     = ensureCache(cacheName);
        cache.clear();
        cache.put("key-1", "val-1");

        TestDeactivationListener   listener   = new TestDeactivationListener(2);
        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addMapListener(listener);

        System.err.println("Calling truncate on cache " + cacheName);
        cache.truncate();
        Eventually.assertDeferred(listener::count, is(1));
        System.err.println("Called truncate on cache " + cacheName);
        cache.put("key-2", "val-2");
        System.err.println("Calling destroy on cache " + cacheName);
        cache.destroy();
        System.err.println("Called destroy on cache " + cacheName);

        System.err.println("Waiting for events from cache " + cacheName);
        boolean fEvents = listener.awaitEvents(1, TimeUnit.MINUTES);
        System.err.println("Finished waiting for events from cache " + cacheName);
        assertThat(fEvents, is(true));
        assertThat(listener.isTruncated(), is(true));
        assertThat(listener.isDestroyed(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldAddPrimingListenerForExistingKey(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = createCacheName("test-events");
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
    public void shouldAddPrimingListenerForNonExistingKey(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName = createCacheName("test-events");
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
    public void shouldAddMapTrigger(String sSerializerName, Serializer serializer)
        {
        String                     cacheName = createCacheName("test-trigger");
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

    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldSubscribeWithSynchronousListener(String sSerializerName, Serializer serializer) throws Exception
        {
        String                     cacheName  = "test-events-" + System.currentTimeMillis();
        NamedCache<String, String> cache      = ensureCache(cacheName);
        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        if (grpcClient instanceof SessionNamedCache)
            {
            grpcClient = ((SessionNamedCache) grpcClient).getInternalNamedCache();
            }
        if (grpcClient instanceof SafeNamedCache)
            {
            grpcClient = ((SafeNamedCache) grpcClient).getNamedCache();
            }

        assertThat(grpcClient, is(instanceOf(NamedCacheClient.class)));

        NamedCacheClient<?, ?>       client      = (NamedCacheClient) grpcClient;
        AsyncNamedCacheClient<?, ?>  asyncClient = client.getAsyncClient();
        NamedCacheClientChannel      channel     = asyncClient.getClientProtocol();

        Assumptions.assumeTrue(channel.getVersion() >= 1, "Skipping test, protocol is less then version 1");

        cache.clear();

        String        key   = "key-" + System.nanoTime();
        AtomicBoolean fDone = new AtomicBoolean(false);
        MapListener<String, String> listener = new MapListenerSupport.SynchronousListener<>()
            {
            @Override
            public void entryInserted(MapEvent<String, String> evt)
                {
                fDone.set(true);
                }

            @Override
            public void entryUpdated(MapEvent<String, String> evt)
                {
                }

            @Override
            public void entryDeleted(MapEvent<String, String> evt)
                {
                }
            };

        grpcClient.addMapListener(listener, key, true);
        grpcClient.async().put(key, "value")
                .handle((ignored, err) ->
                    {
                    if (err != null)
                        {
                        throw Exceptions.ensureRuntimeException(err);
                        }
                    // As the listener is synchronous, the event must have been
                    // received before the put request returns
                    assertThat(fDone.get(), is(true));
                    return null;
                    })
                .get(2, TimeUnit.MINUTES);

        }

    // ----- helper methods -------------------------------------------------

    protected void assumeDeferKeyAssociation(NamedCache<?, ?> cache)
        {
        CacheService service = cache.getCacheService();
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        if (service instanceof GrpcRemoteCacheService)
            {
            Assumptions.assumeTrue(((GrpcRemoteCacheService) service).isDeferKeyAssociationCheck(),
                    "Skipping test as defer-key-association-check is false");
            }
        else if (service instanceof RemoteCacheService)
            {
            Assumptions.assumeTrue(((RemoteCacheService) service).isDeferKeyAssociationCheck(),
                                "Skipping test as defer-key-association-check is false");
            }
        }

    protected String createCacheName()
        {
        return createCacheName("test-cache-");  
        }

    protected String createCacheName(String sPrefix)
        {
        return sPrefix + "-" + f_cacheId.getAndIncrement(); 
        }

    protected abstract <K, V> NamedCache<K, V> createClient(String sCacheName, String sSerializerName, Serializer serializer);

    protected abstract <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader);

    /**
     * Obtain the {@link Serializer} instances to use for parameterized
     * test {@link Arguments}.
     *
     * @return the {@link Serializer} instances to use for test
     * {@link Arguments}
     */
    public static Stream<Arguments> serializers()
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

    /**
     * Clear the specified cache and populate it with entries.
     *
     * @param cache        the cache to clear and populate
     * @param sAssociated  the associated key
     * @param count        the number of entries to add to the cache
     */
    @SuppressWarnings("SameParameterValue")
    protected void clearAndPopulateAssociated(NamedCache<TestAssociatedKey, String> cache, String sAssociated, int count)
        {
        cache.clear();
        for (int i = 1; i <= count; i++)
            {
            TestAssociatedKey key = new TestAssociatedKey("key-" + i, sAssociated);
            cache.put(key, "value-" + i);
            }
        }

    /**
     * Clear the specified cache and populate it with entries.
     *
     * @param cache  the cache to clear and populate
     * @param nPart  the partition id
     * @param count  the number of entries to add to the cache
     */
    @SuppressWarnings("SameParameterValue")
    protected void clearAndPopulatePartition(NamedCache<TestPartitionKey, String> cache, int nPart, int count)
        {
        cache.clear();
        for (int i = 1; i <= count; i++)
            {
            TestPartitionKey key = new TestPartitionKey("key-" + i, nPart);
            cache.put(key, "value-" + i);
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
            System.err.println("entryDeleted (destroy) - Received event " + evt);
            m_destroyed = true;
            f_latch.countDown();
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            // "truncate" event
            System.err.println("entryUpdated (truncate) - Received event " + evt);
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

        /**
         * Returns the current count of events.
         *
         * @return the current count of events
         */
        public int count()
            {
            return (int) f_latch.getCount();
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

    protected static final AtomicLong f_cacheId = new AtomicLong(0L);

    protected final String f_sScopeName;

    protected long m_nStart;
    }
