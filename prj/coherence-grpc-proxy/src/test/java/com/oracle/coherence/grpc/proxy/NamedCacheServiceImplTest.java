/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;

import com.oracle.coherence.grpc.AggregateRequest;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.CacheRequestHolder;
import com.oracle.coherence.grpc.ClearRequest;
import com.oracle.coherence.grpc.ContainsEntryRequest;
import com.oracle.coherence.grpc.ContainsKeyRequest;
import com.oracle.coherence.grpc.ContainsValueRequest;
import com.oracle.coherence.grpc.DestroyRequest;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.EntrySetRequest;
import com.oracle.coherence.grpc.GetAllRequest;
import com.oracle.coherence.grpc.GetRequest;
import com.oracle.coherence.grpc.InvokeAllRequest;
import com.oracle.coherence.grpc.InvokeRequest;
import com.oracle.coherence.grpc.IsEmptyRequest;
import com.oracle.coherence.grpc.KeySetRequest;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.PutAllRequest;
import com.oracle.coherence.grpc.PutIfAbsentRequest;
import com.oracle.coherence.grpc.PutRequest;
import com.oracle.coherence.grpc.Requests;
import com.oracle.coherence.grpc.ValuesRequest;

import com.tangosol.internal.util.processor.BinaryProcessors;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.net.management.Registry;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.Count;

import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import com.tangosol.util.processor.ExtractorProcessor;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsMapContaining.hasEntry;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.nullable;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2019.11.27
 * @since 20.06
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class NamedCacheServiceImplTest
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        s_bytes1 = BinaryHelper.toByteString("one",   SERIALIZER);
        s_bytes2 = BinaryHelper.toByteString("two",   SERIALIZER);
        s_bytes3 = BinaryHelper.toByteString("three", SERIALIZER);
        s_bytes4 = BinaryHelper.toByteString("four",  SERIALIZER);
        s_bytes5 = BinaryHelper.toByteString("five",  SERIALIZER);

        s_byteStringList = Arrays.asList(s_bytes1, s_bytes2, s_bytes3, s_bytes4, s_bytes5);

        s_filterBytes = BinaryHelper.toByteString(new EqualsFilter<>("foo", "bar"), SERIALIZER);

        s_serializerProducer = mock(NamedSerializerFactory.class);
        when(s_serializerProducer.getNamedSerializer(eq(JAVA_FORMAT), any(ClassLoader.class))).thenReturn(SERIALIZER);
        when(s_serializerProducer.getNamedSerializer(eq(POF_FORMAT),  any(ClassLoader.class))).thenReturn(POF_SERIALIZER);
        }

    @BeforeEach
    void setupEach()
        {
        NamedCache testCache = new CacheStub<>(TEST_CACHE_NAME, false);
        m_testAsyncCache = testCache.async();

        m_testCCF = mock(ConfigurableCacheFactory.class);
        when(m_testCCF.ensureCache(eq(TEST_CACHE_NAME), any(ClassLoader.class))).thenReturn(testCache);
        when(m_testCCF.getScopeName()).thenReturn(Requests.DEFAULT_SCOPE);

        m_ccfSupplier = ConfigurableCacheFactorySuppliers.fixed(m_testCCF);

        Registry registry = mock(Registry.class);
        when(registry.ensureGlobalName(anyString())).thenReturn("foo");

        m_dependencies = new NamedCacheServiceImpl.DefaultDependencies();
        m_dependencies.setSerializerFactory(s_serializerProducer);
        m_dependencies.setRegistry(registry);
        m_dependencies.setExecutor(ForkJoinPool.commonPool());
        m_dependencies.setConfigurableCacheFactorySupplier(m_ccfSupplier); 
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldCreateRequestHolder() throws Exception
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);

        CompletionStage<CacheRequestHolder<String, Void>> stage = 
                service.createHolderAsync("foo", Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, POF_FORMAT);
        assertThat(stage, is(notNullValue()));

        CacheRequestHolder<String, Void> holder = stage.toCompletableFuture().get(1, TimeUnit.MINUTES);
        assertThat(holder,                      is(notNullValue()));
        assertThat(holder.getRequest(),         is("foo"));
        assertThat(holder.getCache(),           is(sameInstance(m_testAsyncCache.getNamedCache())));
        assertThat(holder.getAsyncCache(),      is(sameInstance(m_testAsyncCache)));
        assertThat(holder.getSerializer(),      is(sameInstance(POF_SERIALIZER)));
        assertThat(holder.getCacheSerializer(), is(sameInstance(CACHE_SERIALIZER)));
        assertThat(holder.getResult(),          is(nullValue()));
        }

    @Test
    public void shouldNotCreateRequestHolderIfRequestIsNull()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);

        CompletionStage<CacheRequestHolder<String, Void>> stage =
                service.createHolderAsync(null, Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, POF_FORMAT);
        assertThat(stage, is(notNullValue()));

        Throwable error = assertThrows(Throwable.class, () ->
                stage.toCompletableFuture().get(1, TimeUnit.MINUTES));
        Throwable cause = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is("invalid request, the request cannot be null"));
        }

    @Test
    public void shouldNotCreateRequestHolderIfCacheNameIsNull()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);

        CompletionStage<CacheRequestHolder<String, Void>> stage =
                service.createHolderAsync(Requests.DEFAULT_SCOPE, "foo", null, POF_FORMAT);
        assertThat(stage, is(notNullValue()));

        Throwable error = assertThrows(Throwable.class, () -> stage.toCompletableFuture().get(1, TimeUnit.MINUTES));
        Throwable cause = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(
                INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldNotCreateRequestHolderIfCacheNameIsBlank()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);

        CompletionStage<CacheRequestHolder<String, Void>> stage =
                service.createHolderAsync(Requests.DEFAULT_SCOPE, "foo", "", POF_FORMAT);
        assertThat(stage, is(notNullValue()));

        Throwable error = assertThrows(Throwable.class, () ->
                stage.toCompletableFuture().get(1, TimeUnit.MINUTES));
        Throwable cause = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldNotCreateRequestHolderIfRequestSerializerNotFound()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);

        CompletionStage<CacheRequestHolder<String, Void>> stage =
                service.createHolderAsync("foo", Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, "BAD");
        assertThat(stage, is(notNullValue()));

        Throwable error = assertThrows(Throwable.class, () ->
                stage.toCompletableFuture().get(1, TimeUnit.MINUTES));
        Throwable cause = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        Status status = ((StatusRuntimeException) cause).getStatus();
        assertThat(status.getCode(),        is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(status.getDescription(), is("invalid request format, cannot find serializer with name 'BAD'"));
        }

    @Test
    public void shouldEnsureValueExtractor()
        {
        ValueExtractor<?, ?>  extractor = new UniversalExtractor("foo");
        ByteString            bytes     = BinaryHelper.toByteString(extractor, SERIALIZER);
        NamedCacheServiceImpl service   = new NamedCacheServiceImpl(m_dependencies);
        ValueExtractor<?, ?>  result    = service.ensureValueExtractor(bytes, SERIALIZER);
        assertThat(result, is(notNullValue()));
        assertThat(result, is(extractor));
        }

    @Test
    public void shouldEnsureValueExtractorWhenByteStringIsNull()
        {
        NamedCacheServiceImpl  service = new NamedCacheServiceImpl(m_dependencies);
        StatusRuntimeException error   = assertThrows(StatusRuntimeException.class,
                                                      () -> service.ensureValueExtractor(null, SERIALIZER));
        Status                 status  = error.getStatus();
        assertThat(status.getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(status.getDescription(), is("the request does not contain a serialized ValueExtractor"));
        }

    @Test
    public void shouldEnsureValueExtractorWhenByteStringIsEmpty()
        {
        NamedCacheServiceImpl  service = new NamedCacheServiceImpl(m_dependencies);
        StatusRuntimeException error   = assertThrows(StatusRuntimeException.class,
                                                      () -> service.ensureValueExtractor(ByteString.EMPTY, SERIALIZER));
        Status                 status  = error.getStatus();
        assertThat(status.getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(status.getDescription(), is("the request does not contain a serialized ValueExtractor"));
        }

    @Test
    public void shouldEnsureFilter()
        {
        Filter                filter  = new EqualsFilter("foo", "bar");
        ByteString            bytes   = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);
        Filter                result  = service.ensureFilter(bytes, SERIALIZER);
        assertThat(result, is(filter));
        }

    @Test
    public void shouldEnsureFilterWhenByteStringIsNull()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);
        Filter                result  = service.ensureFilter(null, SERIALIZER);
        assertThat(result, is(instanceOf(AlwaysFilter.class)));
        }

    @Test
    public void shouldEnsureFilterWhenByteStringIsEmpty()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);
        Filter                result  = service.ensureFilter(ByteString.EMPTY, SERIALIZER);
        assertThat(result, is(instanceOf(AlwaysFilter.class)));
        }


    @Test
    public void shouldDeserializeComparator()
        {
        Comparator            comparator = new UniversalExtractor("foo");
        ByteString            bytes      = BinaryHelper.toByteString(comparator, SERIALIZER);
        NamedCacheServiceImpl service    = new NamedCacheServiceImpl(m_dependencies);
        Comparator            result     = service.deserializeComparator(bytes, SERIALIZER);
        assertThat(result, is(comparator));
        }

    @Test
    public void shouldDeserializeComparatorWhenByteStringIsNull()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);
        Comparator            result  = service.deserializeComparator(null, SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    @Test
    public void shouldDeserializeComparatorWhenByteStringIsEmpty()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);
        Comparator            result  = service.deserializeComparator(ByteString.EMPTY, SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    // ----- aggregate() tests ----------------------------------------------

    @Test
    public void shouldNotExecuteAggregateWithoutCacheName()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);
        AggregateRequest      request = AggregateRequest.newBuilder()
                .setFormat(JAVA_FORMAT)
                .setAggregator(s_bytes1)
                .build();

        CompletionStage<BytesValue>   stage  = service.aggregate(request);
        CompletableFuture<BytesValue> future = stage.toCompletableFuture();
        ExecutionException            error  = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldNotExecuteAggregateWithoutAggregator()
        {
        NamedCacheServiceImpl service = new NamedCacheServiceImpl(m_dependencies);
        AggregateRequest      request = AggregateRequest.newBuilder()
                .setScope(Requests.DEFAULT_SCOPE)
                .setCache(TEST_CACHE_NAME)
                .setFormat(JAVA_FORMAT)
                .build();

        CompletionStage<BytesValue>   stage  = service.aggregate(request);
        CompletableFuture<BytesValue> future = stage.toCompletableFuture();
        ExecutionException            error  = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(),
                   is("the request does not contain a serialized entry aggregator"));
        }

    @Test
    public void shouldHandleAggregateWithFilterError()
        {
        when(m_testAsyncCache.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenThrow(ERROR);

        Filter                       filter               = new EqualsFilter();
        ByteString                   serializedFilter     = BinaryHelper.toByteString(filter, SERIALIZER);
        InvocableMap.EntryAggregator aggregator           = new Count();
        ByteString                   serializedAggregator = BinaryHelper.toByteString(aggregator, SERIALIZER);
        NamedCacheServiceImpl        service              = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>  stage                = service.aggregate(Requests.aggregate(Requests.DEFAULT_SCOPE,
                                                                              TEST_CACHE_NAME, JAVA_FORMAT,
                                                                              serializedFilter,
                                                                              serializedAggregator));
        CompletableFuture<BytesValue> future    = stage.toCompletableFuture();
        ExecutionException            exception = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleAggregateWithFilterAsyncError()
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);
        when(m_testAsyncCache.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenReturn(failed);

        Filter                       filter               = new EqualsFilter();
        ByteString                   serializedFilter     = BinaryHelper.toByteString(filter, SERIALIZER);
        InvocableMap.EntryAggregator aggregator           = new Count();
        ByteString                   serializedAggregator = BinaryHelper.toByteString(aggregator, SERIALIZER);
        NamedCacheServiceImpl        service              = new NamedCacheServiceImpl(m_dependencies);

        AggregateRequest             request              = Requests.aggregate(Requests.DEFAULT_SCOPE,
                                                                               TEST_CACHE_NAME,
                                                                               JAVA_FORMAT,
                                                                               serializedFilter,
                                                                               serializedAggregator);

        CompletionStage<BytesValue>   stage               = service.aggregate(request);
        CompletableFuture<BytesValue> future              = stage.toCompletableFuture();
        ExecutionException            exception           = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause               = rootCause(exception);

        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleAggregateWithKeysError()
        {
        when(m_testAsyncCache.aggregate(any(Collection.class),
                                        any(InvocableMap.EntryAggregator.class))).thenThrow(ERROR);

        InvocableMap.EntryAggregator aggregator           = new Count();
        ByteString                   serializedAggregator = BinaryHelper.toByteString(aggregator, SERIALIZER);
        NamedCacheServiceImpl        service              = new NamedCacheServiceImpl(m_dependencies);
        AggregateRequest             request              = Requests.aggregate(Requests.DEFAULT_SCOPE,
                                                                               TEST_CACHE_NAME,
                                                                               JAVA_FORMAT,
                                                                               s_byteStringList,
                                                                               serializedAggregator);
        CompletionStage<BytesValue>   stage               = service.aggregate(request);
        CompletableFuture<BytesValue> future              = stage.toCompletableFuture();
        ExecutionException            exception           = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause               = rootCause(exception);

        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleAggregateWithKeysAsyncError()
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);
        when(m_testAsyncCache.aggregate(any(Collection.class),
                                        any(InvocableMap.EntryAggregator.class))).thenReturn(failed);

        InvocableMap.EntryAggregator  aggregator           = new Count();
        ByteString                    serializedAggregator = BinaryHelper.toByteString(aggregator, SERIALIZER);
        NamedCacheServiceImpl         service              = new NamedCacheServiceImpl(m_dependencies);
        AggregateRequest              request              = Requests.aggregate(Requests.DEFAULT_SCOPE,
                                                                                TEST_CACHE_NAME,
                                                                                JAVA_FORMAT,
                                                                                s_byteStringList,
                                                                                serializedAggregator);
        CompletionStage<BytesValue>   stage                = service.aggregate(request);
        CompletableFuture<BytesValue> future               = stage.toCompletableFuture();
        ExecutionException            exception            = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause                = rootCause(exception);

        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- clear() tests --------------------------------------------------

    @Test
    public void shouldNotExecuteClearWithoutCacheName()
        {
        NamedCacheServiceImpl    service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<Empty>   stage   = service.clear(ClearRequest.newBuilder().build());
        CompletableFuture<Empty> future  = stage.toCompletableFuture();
        ExecutionException       error   = assertThrows(ExecutionException.class, future::get);
        Throwable                cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleClearError()
        {
        when(m_testAsyncCache.invokeAll(isA(AlwaysFilter.class),
                                        isA(BinaryProcessors.BinarySyntheticRemoveBlindProcessor.class))).thenThrow(ERROR);

        NamedCacheServiceImpl    service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<Empty>   stage     = service.clear(Requests.clear(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME));
        CompletableFuture<Empty> future    = stage.toCompletableFuture();
        ExecutionException       exception = assertThrows(ExecutionException.class, future::get);
        Throwable                cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleClearAsyncError()
        {
        CompletableFuture<Map<Binary, Void>> failed = failedFuture(ERROR);

        when(m_testAsyncCache.invokeAll(isA(AlwaysFilter.class),
                                        isA(BinaryProcessors.BinarySyntheticRemoveBlindProcessor.class))).thenReturn(failed);

        NamedCacheServiceImpl    service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<Empty>   stage     = service.clear(Requests.clear(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME));
        CompletableFuture<Empty> future    = stage.toCompletableFuture();
        ExecutionException       exception = assertThrows(ExecutionException.class, future::get);
        Throwable                cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- containsEntry() tests -----------------------------------

    @Test
    public void shouldNotExecuteContainsEntryWithoutCacheName()
        {
        NamedCacheServiceImpl        service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage   = service.containsEntry(ContainsEntryRequest.newBuilder().build());
        CompletableFuture<BoolValue> future  = stage.toCompletableFuture();
        ExecutionException           error   = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleContainsEntryError()
        {
        when(m_testAsyncCache.invoke(any(Binary.class), isA(BinaryProcessors.BinaryContainsValueProcessor.class)))
                .thenThrow(ERROR);

        NamedCacheServiceImpl        service   = new NamedCacheServiceImpl(m_dependencies);
        ContainsEntryRequest         request   = Requests.containsEntry(Requests.DEFAULT_SCOPE,
                                                                        TEST_CACHE_NAME,
                                                                        JAVA_FORMAT,
                                                                        s_bytes1,
                                                                        s_bytes2);
        CompletionStage<BoolValue>   stage     = service.containsEntry(request);
        CompletableFuture<BoolValue> future    = stage.toCompletableFuture();
        ExecutionException           exception = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleContainsEntryAsyncError()
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);

        when(m_testAsyncCache.invoke(any(Binary.class), isA(BinaryProcessors.BinaryContainsValueProcessor.class))).thenReturn(failed);

        NamedCacheServiceImpl        service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage     = service.containsEntry(Requests.containsEntry(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME,
                                                                                              JAVA_FORMAT, s_bytes1,
                                                                                              s_bytes2));
        CompletableFuture<BoolValue> future    = stage.toCompletableFuture();
        ExecutionException           exception = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- containsKey() tests --------------------------------------------

    @Test
    public void shouldNotExecuteContainsKeyWithoutCacheName()
        {
        NamedCacheServiceImpl        service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage   = service.containsKey(ContainsKeyRequest.newBuilder().build());
        CompletableFuture<BoolValue> future  = stage.toCompletableFuture();
        ExecutionException           error   = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleContainsKeyError()
        {
        when(m_testAsyncCache.containsKey(any(Binary.class))).thenThrow(ERROR);

        NamedCacheServiceImpl        service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage     = service.containsKey(Requests.containsKey(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                          s_bytes1));
        CompletableFuture<BoolValue> future    = stage.toCompletableFuture();
        ExecutionException           exception = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleContainsKeyAsyncError()
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);

        when(m_testAsyncCache.containsKey(any(Binary.class))).thenReturn(failed);

        NamedCacheServiceImpl        service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage     = service.containsKey(Requests.containsKey(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                          s_bytes1));
        CompletableFuture<BoolValue> future    = stage.toCompletableFuture();
        ExecutionException           exception = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- containsValue() tests ------------------------------------------

    @Test
    public void shouldNotExecuteContainsValueWithoutCacheName()
        {
        NamedCacheServiceImpl        service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage   = service.containsValue(ContainsValueRequest.newBuilder().build());
        CompletableFuture<BoolValue> future  = stage.toCompletableFuture();
        ExecutionException           error   = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleContainsValueError()
        {
        when(m_testAsyncCache.aggregate(any(Filter.class), isA(Count.class))).thenThrow(ERROR);

        NamedCacheServiceImpl        service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage     = service.containsValue(Requests.containsValue(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME,
                                                                                              JAVA_FORMAT,
                                                                                              s_bytes1));
        CompletableFuture<BoolValue> future    = stage.toCompletableFuture();
        ExecutionException           exception = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleContainsValueAsyncError()
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);

        when(m_testAsyncCache.aggregate(any(Filter.class), isA(Count.class))).thenReturn(failed);

        NamedCacheServiceImpl        service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage     = service.containsValue(Requests.containsValue(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME,
                                                                                             JAVA_FORMAT, s_bytes1));
        CompletableFuture<BoolValue> future    = stage.toCompletableFuture();
        ExecutionException           exception = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- destroy() tests ------------------------------------------------

    @Test
    public void shouldNotExecuteDestroyWithoutCacheName()
        {
        NamedCacheServiceImpl    service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<Empty>   stage   = service.destroy(DestroyRequest.newBuilder().build());
        CompletableFuture<Empty> future  = stage.toCompletableFuture();
        ExecutionException       error   = assertThrows(ExecutionException.class, future::get);
        Throwable                cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleDestroyError()
        {
        NamedCache cache = m_testAsyncCache.getNamedCache();
        doThrow(ERROR).when(cache).destroy();

        NamedCacheServiceImpl    service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<Empty>   stage     = service.destroy(Requests.destroy(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME));
        CompletableFuture<Empty> future    = stage.toCompletableFuture();
        ExecutionException       exception = assertThrows(ExecutionException.class, future::get);
        Throwable                cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- entrySet(Filter) tests -----------------------------------------

    @Test
    public void shouldNotExecuteEntrySetWithoutCacheName() throws Exception
        {
        Filter                    filter      = new EqualsFilter("foo", "bar");
        ByteString                filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        TestStreamObserver<Entry> observer    = new TestStreamObserver<>();
        NamedCacheServiceImpl     service     = new NamedCacheServiceImpl(m_dependencies);

        service.entrySet(EntrySetRequest.newBuilder().setFormat(JAVA_FORMAT).setFilter(filterBytes).build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleEntrySetError() throws Exception
        {
        when(m_testAsyncCache.entrySet(any(Filter.class), nullable(Comparator.class))).thenThrow(ERROR);

        Filter<Binary>            filter      = new EqualsFilter<>("foo", "bar");
        ByteString                filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheServiceImpl     service     = new NamedCacheServiceImpl(m_dependencies);
        TestStreamObserver<Entry> observer    = new TestStreamObserver<>();

        service.entrySet(Requests.entrySet(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleEntrySetAsyncError() throws Exception
        {
        CompletableFuture<Set<Map.Entry<Binary, Binary>>> failed = failedFuture(ERROR);
        when(m_testAsyncCache.entrySet(any(Filter.class), nullable(Comparator.class))).thenReturn(failed);

        Filter                    filter      = new EqualsFilter("foo", "bar");
        ByteString                filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheServiceImpl     service     = new NamedCacheServiceImpl(m_dependencies);
        TestStreamObserver<Entry> observer    = new TestStreamObserver<>();

        service.entrySet(Requests.entrySet(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- get() tests ----------------------------------------------------

    @Test
    public void shouldNotExecuteGetWithoutCacheName()
        {
        NamedCacheServiceImpl            service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<OptionalValue>   stage   = service.get(GetRequest.newBuilder().build());
        CompletableFuture<OptionalValue> future  = stage.toCompletableFuture();
        ExecutionException               error   = assertThrows(ExecutionException.class, future::get);
        Throwable                        cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleGetError()
        {
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryGetProcessor.class))).thenThrow(ERROR);

        NamedCacheServiceImpl            service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<OptionalValue>   stage     = service.get(Requests.get(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1));
        CompletableFuture<OptionalValue> future    = stage.toCompletableFuture();
        ExecutionException               exception = assertThrows(ExecutionException.class, future::get);
        Throwable                        cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleGetAsyncError()
        {
        CompletableFuture<Binary> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryGetProcessor.class))).thenReturn(failed);

        NamedCacheServiceImpl            service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<OptionalValue>   stage     = service.get(Requests.get(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1));
        CompletableFuture<OptionalValue> future    = stage.toCompletableFuture();
        ExecutionException               exception = assertThrows(ExecutionException.class, future::get);
        Throwable                        cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- getAll() tests -------------------------------------------------

    @Test
    public void shouldExecuteGetAllWithNoKeys() throws Exception
        {
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheServiceImpl     service  = new NamedCacheServiceImpl(m_dependencies);

        service.getAll(GetAllRequest.newBuilder()
                .setScope(Requests.DEFAULT_SCOPE)
                .setCache(TEST_CACHE_NAME).build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoValues()
                .assertNoErrors();
        }

    @Test
    public void shouldExecuteGetAllWithResults() throws Exception
        {
        // The values returned from an invoke all on a pass-thru cache are double serialized
        Map<Binary, Binary> mapResults = new HashMap<>();
        mapResults.put(serialize("one"),   doubleSerialize("value-1"));
        mapResults.put(serialize("two"),   doubleSerialize("value-2"));
        mapResults.put(serialize("three"), doubleSerialize("value-3"));
        mapResults.put(serialize("four"),  doubleSerialize("value-4"));
        mapResults.put(serialize("five"),  doubleSerialize("value-5"));

        CompletableFuture<Map<Binary, Binary>> future = CompletableFuture.completedFuture(mapResults);
        when(m_testAsyncCache.invokeAll(any(Collection.class), any(BinaryProcessors.BinaryGetProcessor.class))).thenReturn(future);

        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheServiceImpl     service  = new NamedCacheServiceImpl(m_dependencies);

        service.getAll(Requests.getAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_byteStringList), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertValueCount(mapResults.size())
                .assertNoErrors();

        Map<String, String> results = observer.values().stream()
                .collect(Collectors.toMap(e -> BinaryHelper.fromByteString(e.getKey(), SERIALIZER),
                                          e -> BinaryHelper.fromByteString(e.getValue(), SERIALIZER)));

        assertThat(results, hasEntry("one",   "value-1"));
        assertThat(results, hasEntry("two",   "value-2"));
        assertThat(results, hasEntry("three", "value-3"));
        assertThat(results, hasEntry("four",  "value-4"));
        assertThat(results, hasEntry("five",  "value-5"));
        }

    @Test
    public void shouldNotExecuteGetAllWithoutCacheName() throws Exception
        {
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheServiceImpl     service  = new NamedCacheServiceImpl(m_dependencies);

        service.getAll(GetAllRequest.newBuilder().addAllKey(s_byteStringList).build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleGetAllError() throws Exception
        {
        when(m_testAsyncCache.invokeAll(any(Collection.class), any(BinaryProcessors.BinaryGetProcessor.class))).thenThrow(ERROR);

        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheServiceImpl     service  = new NamedCacheServiceImpl(m_dependencies);

        service.getAll(Requests.getAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_byteStringList), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleGetAllAsyncError() throws Exception
        {
        CompletableFuture<Map<Binary, Binary>> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invokeAll(any(Collection.class), any(BinaryProcessors.BinaryGetProcessor.class))).thenReturn(failed);

        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheServiceImpl     service  = new NamedCacheServiceImpl(m_dependencies);

        service.getAll(Requests.getAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_byteStringList), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- invoke() tests -------------------------------------------------

    @Test
    public void shouldNotExecuteInvokeWithoutCacheName()
        {
        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheServiceImpl       service             = new NamedCacheServiceImpl(m_dependencies);

        CompletionStage<BytesValue>   stage  =
                service.invoke(InvokeRequest.newBuilder().setProcessor(serializedProcessor).build());
        CompletableFuture<BytesValue> future = stage.toCompletableFuture();
        ExecutionException            error  = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldNotExecuteInvokeWithoutEntryProcessor()
        {
        InvokeRequest request  = InvokeRequest.newBuilder()
                .setScope(Requests.DEFAULT_SCOPE)
                .setCache(TEST_CACHE_NAME).build();

        NamedCacheServiceImpl         service  = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage    = service.invoke(request);
        CompletableFuture<BytesValue> future   = stage.toCompletableFuture();
        ExecutionException            error    = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause    = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(MISSING_PROCESSOR_MESSAGE));
        }

    @Test
    public void shouldHandleInvokeError()
        {
        when(m_testAsyncCache.invoke(any(Binary.class), any(ExtractorProcessor.class))).thenThrow(ERROR);

        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        InvokeRequest               request             = Requests.invoke(Requests.DEFAULT_SCOPE,
                                                                          TEST_CACHE_NAME,
                                                                          JAVA_FORMAT,
                                                                          s_bytes1,
                                                                          serializedProcessor);

        NamedCacheServiceImpl         service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage   = service.invoke(request);
        CompletableFuture<BytesValue> future  = stage.toCompletableFuture();
        ExecutionException            error   = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause   = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleInvokeAsyncError()
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invoke(any(Binary.class), any(ExtractorProcessor.class))).thenReturn(failed);

        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        InvokeRequest               request             = Requests.invoke(Requests.DEFAULT_SCOPE,
                                                                          TEST_CACHE_NAME,
                                                                          JAVA_FORMAT,
                                                                          s_bytes1,
                                                                          serializedProcessor);

        NamedCacheServiceImpl         service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage   = service.invoke(request);
        CompletableFuture<BytesValue> future  = stage.toCompletableFuture();
        ExecutionException            error   = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause   = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- invokeAll() tests ----------------------------------------------

    @Test
    public void shouldNotExecuteInvokeAllWithoutCacheName() throws Exception
        {
        TestStreamObserver<Entry>   observer            = new TestStreamObserver<>();
        NamedCacheServiceImpl       service             = new NamedCacheServiceImpl(m_dependencies);
        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        InvokeAllRequest            request             = InvokeAllRequest.newBuilder()
                                                                          .setProcessor(serializedProcessor)
                                                                          .build();

        service.invokeAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldNotExecuteInvokeAllWithoutEntryProcessor() throws Exception
        {
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheServiceImpl     service  = new NamedCacheServiceImpl(m_dependencies);
        InvokeAllRequest          request  = InvokeAllRequest.newBuilder()
                                                            .setScope(Requests.DEFAULT_SCOPE)
                                                            .setCache(TEST_CACHE_NAME)
                                                            .build();
        service.invokeAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(MISSING_PROCESSOR_MESSAGE));
        }

    @Test
    public void shouldHandleInvokeAllWithFilterError() throws Exception
        {
        when(m_testAsyncCache.invokeAll(any(Filter.class), any(ExtractorProcessor.class))).thenThrow(ERROR);

        TestStreamObserver<Entry>   observer            = new TestStreamObserver<>();
        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheServiceImpl       service             = new NamedCacheServiceImpl(m_dependencies);

        service.invokeAll(Requests.invokeAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                             s_filterBytes, serializedProcessor), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleInvokeWithFilterAllAsyncError() throws Exception
        {
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        CompletableFuture<Map>    failed   = failedFuture(ERROR);
        when(m_testAsyncCache.invokeAll(any(Filter.class), any(ExtractorProcessor.class))).thenReturn(failed);

        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheServiceImpl       service             = new NamedCacheServiceImpl(m_dependencies);

        service.invokeAll(Requests.invokeAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                             s_filterBytes, serializedProcessor), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleInvokeAllWithKeysError() throws Exception
        {
        when(m_testAsyncCache.invokeAll(any(Collection.class), any(ExtractorProcessor.class))).thenThrow(ERROR);

        TestStreamObserver<Entry>   observer            = new TestStreamObserver<>();
        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheServiceImpl       service             = new NamedCacheServiceImpl(m_dependencies);

        service.invokeAll(Requests.invokeAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                             s_byteStringList, serializedProcessor), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleInvokeAllWithKeysAsyncError() throws Exception
        {
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        CompletableFuture<Map>    failed   = failedFuture(ERROR);
        when(m_testAsyncCache.invokeAll(any(Collection.class), any(ExtractorProcessor.class))).thenReturn(failed);

        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheServiceImpl       service             = new NamedCacheServiceImpl(m_dependencies);

        service.invokeAll(Requests.invokeAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                             s_byteStringList, serializedProcessor), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- isEmpty() tests ------------------------------------------------

    @Test
    public void shouldNotExecuteIsEmptyWithoutCacheName()
        {
        NamedCacheServiceImpl        service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage   = service.isEmpty(IsEmptyRequest.newBuilder().build());
        CompletableFuture<BoolValue> future  = stage.toCompletableFuture();
        ExecutionException           error   = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleIsEmptyError()
        {
        when(m_testAsyncCache.isEmpty()).thenThrow(ERROR);

        NamedCacheServiceImpl        service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage     = service.isEmpty(Requests.isEmpty(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME));
        CompletableFuture<BoolValue> future    = stage.toCompletableFuture();
        ExecutionException           exception = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleIsEmptyAsyncError()
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);
        when(m_testAsyncCache.isEmpty()).thenReturn(failed);

        NamedCacheServiceImpl        service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BoolValue>   stage     = service.isEmpty(Requests.isEmpty(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME));
        CompletableFuture<BoolValue> future    = stage.toCompletableFuture();
        ExecutionException           exception = assertThrows(ExecutionException.class, future::get);
        Throwable                    cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- keySet(Filter) tests -------------------------------------------

    @Test
    public void shouldNotExecuteKeySetWithoutCacheName() throws Exception
        {
        Filter<Binary>                 filter      = new EqualsFilter<>("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();
        NamedCacheServiceImpl          service     = new NamedCacheServiceImpl(m_dependencies);

        service.keySet(KeySetRequest.newBuilder().setFormat(JAVA_FORMAT).setFilter(filterBytes).build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleKeySetError() throws Exception
        {
        when(m_testAsyncCache.keySet(any(Filter.class))).thenThrow(ERROR);

        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheServiceImpl          service     = new NamedCacheServiceImpl(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.keySet(Requests.keySet(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleKeySetAsyncError() throws Exception
        {
        CompletableFuture<Set<Binary>> failed = failedFuture(ERROR);
        when(m_testAsyncCache.keySet(any(Filter.class))).thenReturn(failed);

        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheServiceImpl          service     = new NamedCacheServiceImpl(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.keySet(Requests.keySet(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- put tests ------------------------------------------------------

    @Test
    public void shouldNotExecutePutWithoutCacheName()
        {
        PutRequest request = PutRequest.newBuilder()
                .setFormat(JAVA_FORMAT)
                .setKey(s_bytes1)
                .setValue(s_bytes2).build();

        NamedCacheServiceImpl         service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage   = service.put(request);
        CompletableFuture<BytesValue> future  = stage.toCompletableFuture();
        ExecutionException            error   = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandlePutError()
        {
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryPutProcessor.class))).thenThrow(ERROR);

        PutRequest request = Requests.put(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1, s_bytes2);

        NamedCacheServiceImpl         service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage     = service.put(request);
        CompletableFuture<BytesValue> future    = stage.toCompletableFuture();
        ExecutionException            exception = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePutAsyncError()
        {
        CompletableFuture<Binary> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryPutProcessor.class))).thenReturn(failed);

        PutRequest request = Requests.put(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                          s_bytes2);

        NamedCacheServiceImpl         service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage     = service.put(request);
        CompletableFuture<BytesValue> future    = stage.toCompletableFuture();
        ExecutionException            exception = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause     = rootCause(exception);
        cause.printStackTrace();
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- putAll tests ---------------------------------------------------

    @Test
    public void shouldNotExecutePutAllWithoutCacheName()
        {

        Entry         entry   = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>   entries = Collections.singletonList(entry);
        PutAllRequest request = PutAllRequest.newBuilder().setFormat(JAVA_FORMAT).addAllEntry(entries).build();

        NamedCacheServiceImpl    service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<Empty>   stage   = service.putAll(request);
        CompletableFuture<Empty> future  = stage.toCompletableFuture();
        ExecutionException       error   = assertThrows(ExecutionException.class, future::get);
        Throwable                cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandlePutAllError()
        {
        Entry         entry   = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>   entries = Collections.singletonList(entry);
        PutAllRequest request = Requests.putAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, entries);

        when(m_testAsyncCache.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenThrow(ERROR);

        NamedCacheServiceImpl    service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<Empty>   stage     = service.putAll(request);
        CompletableFuture<Empty> future    = stage.toCompletableFuture();
        ExecutionException       exception = assertThrows(ExecutionException.class, future::get);
        Throwable                cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePartitionedPutAllError()
        {
        String          sCacheName = "test-partitioned";
        NamedCache      testCache  = new CacheStub<>(sCacheName, true);
        AsyncNamedCache asyncCache = testCache.async();

        when(m_testCCF.ensureCache(eq(sCacheName), any(ClassLoader.class))).thenReturn(testCache);
        when(asyncCache.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenThrow(ERROR);

        NamedCacheServiceImpl    service   = new NamedCacheServiceImpl(m_dependencies);
        Entry                    entry     = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>              entries   = Collections.singletonList(entry);
        CompletionStage<Empty>   stage     = service.putAll(Requests.putAll(Requests.DEFAULT_SCOPE, sCacheName, JAVA_FORMAT, entries));
        CompletableFuture<Empty> future    = stage.toCompletableFuture();
        ExecutionException       exception = assertThrows(ExecutionException.class, future::get);
        Throwable                cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePutAllAsyncError()
        {
        CompletableFuture<Map<Binary, Binary>> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenReturn(failed);

        NamedCacheServiceImpl    service   = new NamedCacheServiceImpl(m_dependencies);
        Entry                    entry     = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>              entries   = Collections.singletonList(entry);
        CompletionStage<Empty>   stage     = service.putAll(Requests.putAll(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, entries));
        CompletableFuture<Empty> future    = stage.toCompletableFuture();
        ExecutionException       exception = assertThrows(ExecutionException.class, future::get);
        Throwable                cause     = rootCause(exception);
        cause.printStackTrace();
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePartitionedPutAllAsyncError()
        {
        String          sCacheName = "test-partitioned";
        NamedCache      testCache  = new CacheStub<>(sCacheName, true);
        AsyncNamedCache asyncCache = testCache.async();

        when(m_testCCF.ensureCache(eq(sCacheName), any(ClassLoader.class))).thenReturn(testCache);
        when(asyncCache.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenThrow(ERROR);

        NamedCacheServiceImpl    service   = new NamedCacheServiceImpl(m_dependencies);
        Entry                    entry     = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>              entries   = Collections.singletonList(entry);
        CompletionStage<Empty>   stage     = service.putAll(Requests.putAll(Requests.DEFAULT_SCOPE, sCacheName, JAVA_FORMAT, entries));
        CompletableFuture<Empty> future    = stage.toCompletableFuture();
        ExecutionException       exception = assertThrows(ExecutionException.class, future::get);
        Throwable                cause     = rootCause(exception);
        cause.printStackTrace();
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- putIfAbsent tests ----------------------------------------------

    @Test
    public void shouldNotExecutePutIfAbsentWithoutCacheName()
        {
        PutIfAbsentRequest  request = PutIfAbsentRequest
                .newBuilder()
                .setFormat(JAVA_FORMAT)
                .setKey(s_bytes1).setValue(s_bytes2)
                .build();

        NamedCacheServiceImpl         service = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage   = service.putIfAbsent(request);
        CompletableFuture<BytesValue> future  = stage.toCompletableFuture();
        ExecutionException            error   = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause   = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandlePutIfAbsentError()
        {
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryPutIfAbsentProcessor.class)))
                .thenThrow(ERROR);

        PutIfAbsentRequest request = Requests.putIfAbsent(Requests.DEFAULT_SCOPE,
                                                          TEST_CACHE_NAME,
                                                          JAVA_FORMAT,
                                                          s_bytes1,
                                                          s_bytes2);

        NamedCacheServiceImpl         service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage     = service.putIfAbsent(request);
        CompletableFuture<BytesValue> future    = stage.toCompletableFuture();
        ExecutionException            exception = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause     = rootCause(exception);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePutIfAbsentAsyncError()
        {
        CompletableFuture<Binary> failed = failedFuture(ERROR);
        PutIfAbsentRequest        request = Requests.putIfAbsent(Requests.DEFAULT_SCOPE,
                                                                 TEST_CACHE_NAME,
                                                                 JAVA_FORMAT,
                                                                 s_bytes1,
                                                                 s_bytes2);

        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryPutIfAbsentProcessor.class)))
                .thenReturn(failed);

        NamedCacheServiceImpl         service   = new NamedCacheServiceImpl(m_dependencies);
        CompletionStage<BytesValue>   stage     = service.putIfAbsent(request);
        CompletableFuture<BytesValue> future    = stage.toCompletableFuture();
        ExecutionException            exception = assertThrows(ExecutionException.class, future::get);
        Throwable                     cause     = rootCause(exception);
        cause.printStackTrace();
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- values(Filter) tests -------------------------------------------

    @Test
    public void shouldNotExecuteValuesWithoutCacheName() throws Exception
        {
        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();
        NamedCacheServiceImpl          service     = new NamedCacheServiceImpl(m_dependencies);

        service.values(ValuesRequest.newBuilder().setFormat(JAVA_FORMAT).setFilter(filterBytes).build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleValuesError() throws Exception
        {
        when(m_testAsyncCache.values(any(Filter.class), nullable(Comparator.class))).thenThrow(ERROR);

        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheServiceImpl          service     = new NamedCacheServiceImpl(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.values(Requests.values(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        cause.printStackTrace();
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleValuesAsyncError() throws Exception
        {
        CompletableFuture<Map> failed = failedFuture(ERROR);
        when(m_testAsyncCache.values(any(Filter.class), nullable(Comparator.class))).thenReturn(failed);

        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheServiceImpl          service     = new NamedCacheServiceImpl(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.values(Requests.values(Requests.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- helper methods -------------------------------------------------

    protected <T> CompletableFuture<T> failedFuture(Throwable t)
        {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
        }

    protected Throwable rootCause(Throwable t)
        {
        Throwable cause = t.getCause();
        if (cause == null)
            {
            return t;
            }
        return rootCause(cause);
        }

    protected <T> Binary serialize(T value)
        {
        return ExternalizableHelper.toBinary(value, SERIALIZER);
        }

    protected <T> Binary doubleSerialize(T value)
        {
        Binary binary = ExternalizableHelper.toBinary(value, SERIALIZER);
        return ExternalizableHelper.toBinary(binary, SERIALIZER);
        }

    // ----- inner class: CacheStub -----------------------------------------

    private static class CacheStub<K, V>
            extends WrapperNamedCache<K, V>
        {
        // ----- constructors -----------------------------------------------

        private CacheStub(String sName, boolean partitioned)
            {
            super(new HashMap<>(), sName, partitioned ? mock(DistributedCacheService.class) : mock(CacheService.class));
            NamedCache mockCache = spy(this);
            f_async = mock(AsyncNamedCache.class);
            when(f_async.getNamedCache()).thenReturn(mockCache);
            CacheService             cacheService = getCacheService();
            BackingMapManager        bmm          = mock(BackingMapManager.class);
            BackingMapManagerContext ctx          = mock(BackingMapManagerContext.class);
            when(cacheService.getSerializer()).thenReturn(CACHE_SERIALIZER);
            when(cacheService.getContextClassLoader()).thenReturn(Base.getContextClassLoader());
            when(cacheService.getBackingMapManager()).thenReturn(bmm);
            when(bmm.getContext()).thenReturn(ctx);
            when(ctx.getKeyFromInternalConverter()).thenReturn(new ConverterUp());
            when(ctx.getValueFromInternalConverter()).thenReturn(new ConverterUp());
            when(ctx.getKeyToInternalConverter()).thenReturn(new ConverterDown());
            when(ctx.getValueToInternalConverter()).thenReturn(new ConverterDown());
            }

        // ----- NamedCache interface ---------------------------------------

        @Override
        public AsyncNamedCache<K, V> async()
            {
            return f_async;
            }

        @Override
        public AsyncNamedCache<K, V> async(AsyncNamedCache.Option... options)
            {
            return f_async;
            }

        // ----- data members -----------------------------------------------

        private final AsyncNamedCache<K, V> f_async;
        }

    // ----- inner class: ConverterDown -------------------------------------

    protected static class ConverterDown
            implements Converter<Object, Binary>
        {
        @Override
        public Binary convert(Object o)
            {
            return ExternalizableHelper.toBinary(o, CACHE_SERIALIZER);
            }
        }

    // ----- inner class: ConverterUp ---------------------------------------

    private static class ConverterUp
            implements Converter<Binary, Object>
        {
        @Override
        public Object convert(Binary bin)
            {
            return ExternalizableHelper.fromBinary(bin, CACHE_SERIALIZER);
            }
        }

    // ----- constants ------------------------------------------------------

    private static final String INVALID_CACHE_NAME_MESSAGE = "invalid request, cache name cannot be null or empty";

    private static final String MISSING_PROCESSOR_MESSAGE = "the request does not contain a serialized entry processor";

    private static final String TEST_CACHE_NAME = "test-cache";

    private static final String JAVA_FORMAT = "java";

    private static final String POF_FORMAT = "pof";

    private static final Serializer SERIALIZER = new DefaultSerializer();

    private static final Serializer CACHE_SERIALIZER = new DefaultSerializer();

    private static final Serializer POF_SERIALIZER = new ConfigurablePofContext("test-pof-config.xml");

    private static final RuntimeException ERROR = new RuntimeException("Computer says No!");

    // ----- data members ---------------------------------------------------

    private static NamedSerializerFactory s_serializerProducer;

    private Function<String, ConfigurableCacheFactory> m_ccfSupplier;
    
    private NamedCacheServiceImpl.DefaultDependencies m_dependencies;

    private static ByteString s_bytes1;

    private static ByteString s_bytes2;

    private static ByteString s_bytes3;

    private static ByteString s_bytes4;

    private static ByteString s_bytes5;

    private static ByteString s_filterBytes;

    private static List<ByteString> s_byteStringList;

    private AsyncNamedCache<Binary, Binary> m_testAsyncCache;

    private ConfigurableCacheFactory m_testCCF;
    }
