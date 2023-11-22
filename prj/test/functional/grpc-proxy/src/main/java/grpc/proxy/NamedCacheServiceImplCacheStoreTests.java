/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.proxy;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.Requests;

import com.oracle.coherence.grpc.proxy.NamedCacheService;

import com.tangosol.io.Serializer;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for COH-28911 to ensure gRPC calls trigger a CacheStore.
 */
@SuppressWarnings({"resource", "rawtypes"})
public class NamedCacheServiceImplCacheStoreTests
        extends BaseNamedCacheServiceImplIT
    {
    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void testAsyncGetCallsLoadOnly(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "store-get";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();
        TestCacheStore.clear();

        NamedCacheService              service  = createService();
        Binary                         binary   = ExternalizableHelper.toBinary("key-1", serializer);
        ByteString                     key      = BinaryHelper.toByteString(binary);
        CompletionStage<OptionalValue> response = service.get(Requests.get(sScope, sCacheName, serializerName, key));

        assertThat(response, is(notNullValue()));
        CompletableFuture<OptionalValue> future = response.toCompletableFuture();
        future.get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getErases().isEmpty(), is(true));
        assertThat(TestCacheStore.getStores().isEmpty(), is(true));
        Queue<?> loads = TestCacheStore.getLoads();
        assertThat(loads.size(), is(1));
        assertThat(loads.contains("key-1"), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void testAsyncGetAllCallsLoadOnly(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "store-getAll";
        Set<String>                setKey     = Set.of("key-1", "key-2", "key-3", "key-4");
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();
        TestCacheStore.clear();

        NamedCacheService service     = createService();
        List<ByteString>  listBinKeys = setKey.stream()
                                              .map(s -> ExternalizableHelper.toBinary(s, serializer))
                                              .map(BinaryHelper::toByteString)
                                              .collect(Collectors.toList());

        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        service.getAll(Requests.getAll(sScope, cache.getCacheName(), serializerName, listBinKeys), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertComplete()
                .assertNoErrors();

        assertThat(TestCacheStore.getErases().isEmpty(), is(true));
        assertThat(TestCacheStore.getStores().isEmpty(), is(true));
        Set<?> loads = TestCacheStore.getLoadsAsSet();
        assertThat(loads, is(setKey));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void testAsyncPutCallsStoreOnly(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "store-put";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();
        TestCacheStore.clear();

        NamedCacheService           service     = createService();
        Binary                      binaryKey   = ExternalizableHelper.toBinary("key-1", serializer);
        ByteString                  key         = BinaryHelper.toByteString(binaryKey);
        Binary                      binaryValue = ExternalizableHelper.toBinary("value-1", serializer);
        ByteString                  value       = BinaryHelper.toByteString(binaryValue);
        CompletionStage<BytesValue> response    = service.put(Requests.put(sScope, sCacheName, serializerName, key, value));

        assertThat(response, is(notNullValue()));
        CompletableFuture<BytesValue> future = response.toCompletableFuture();
        future.get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getErases().isEmpty(), is(true));
        assertThat(TestCacheStore.getLoads().isEmpty(), is(true));
        assertThat(TestCacheStore.getStores().get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void testAsyncPutAllCallsStoreOnly(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "store-putAll";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();
        TestCacheStore.clear();

        NamedCacheService service = createService();
        ByteString        key1    = toByteString("key-1", serializer);
        ByteString        value1  = toByteString("value-1", serializer);
        ByteString        key2    = toByteString("key-2", serializer);
        ByteString        value2  = toByteString("value-2", serializer);

        List<Entry> listEntries = new ArrayList<>();
        listEntries.add(Entry.newBuilder().setKey(key1).setValue(value1).build());
        listEntries.add(Entry.newBuilder().setKey(key2).setValue(value2).build());

        CompletionStage<Empty> response = service.putAll(Requests.putAll(sScope, sCacheName, serializerName, listEntries));
        assertThat(response, is(notNullValue()));
        CompletableFuture<Empty> future = response.toCompletableFuture();
        future.get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getErases().isEmpty(), is(true));
        assertThat(TestCacheStore.getLoads().isEmpty(), is(true));
        Map mapStores = TestCacheStore.getStores();
        assertThat(mapStores.size(), is(2));
        assertThat(mapStores.get("key-1"), is("value-1"));
        assertThat(mapStores.get("key-2"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void testAsyncRemoveCallsLoadAndErase(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "store-remove";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();
        TestCacheStore.clear();
        TestCacheStore.put("key-1", "value-1");

        NamedCacheService service = createService();
        ByteString        key     = toByteString("key-1", serializer);

        CompletionStage<BytesValue> response = service.remove(Requests.remove(sScope, sCacheName, serializerName, key));
        CompletableFuture<BytesValue> future = response.toCompletableFuture();
        future.get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getStores().isEmpty(), is(true));

        Queue<?> loads = TestCacheStore.getLoads();
        assertThat(loads.size(), is(1));
        assertThat(loads.contains("key-1"), is(true));

        Queue<?> erases = TestCacheStore.getErases();
        assertThat(erases.size(), is(1));
        assertThat(erases.contains("key-1"), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void testAsyncRemoveKeyAndValueCallsLoadAndErase(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "store-remove";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();
        TestCacheStore.clear();
        TestCacheStore.put("key-1", "value-1");

        NamedCacheService service = createService();
        ByteString        key     = toByteString("key-1", serializer);
        ByteString        value   = toByteString("value-1", serializer);

        CompletionStage<BoolValue>   response = service.removeMapping(Requests.remove(sScope, sCacheName, serializerName, key, value));
        CompletableFuture<BoolValue> future   = response.toCompletableFuture();
        future.get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getStores().isEmpty(), is(true));

        Queue<?> loads = TestCacheStore.getLoads();
        assertThat(loads.size(), is(1));
        assertThat(loads.contains("key-1"), is(true));

        Queue<?> erases = TestCacheStore.getErases();
        assertThat(erases.size(), is(1));
        assertThat(erases.contains("key-1"), is(true));
        }
    }
