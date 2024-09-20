/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.proxy;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.v0.CacheRequestHolder;
import com.oracle.coherence.grpc.v0.Requests;

import com.oracle.coherence.grpc.messages.cache.v0.AggregateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ClearRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsEntryRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsKeyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsValueRequest;
import com.oracle.coherence.grpc.messages.cache.v0.DestroyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.oracle.coherence.grpc.messages.cache.v0.EntrySetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.GetAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.GetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeRequest;
import com.oracle.coherence.grpc.messages.cache.v0.IsEmptyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.KeySetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.OptionalValue;
import com.oracle.coherence.grpc.messages.cache.v0.PutAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.PutIfAbsentRequest;
import com.oracle.coherence.grpc.messages.cache.v0.PutRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ValuesRequest;

import com.oracle.coherence.grpc.proxy.common.v0.BaseNamedCacheServiceImpl;
import com.oracle.coherence.grpc.proxy.common.ConfigurableCacheFactorySuppliers;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
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

import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.Count;

import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import com.tangosol.util.processor.ExtractorProcessor;

import grpc.proxy.TestNamedCacheServiceProvider;
import grpc.proxy.TestStreamObserver;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import static com.oracle.coherence.grpc.proxy.common.v0.BaseNamedCacheServiceImpl.INVALID_CACHE_NAME_MESSAGE;
import static com.oracle.coherence.grpc.proxy.common.v0.BaseNamedCacheServiceImpl.INVALID_REQUEST_MESSAGE;
import static com.oracle.coherence.grpc.proxy.common.v0.BaseNamedCacheServiceImpl.MISSING_EXTRACTOR_MESSAGE;
import static com.oracle.coherence.grpc.proxy.common.v0.BaseNamedCacheServiceImpl.MISSING_PROCESSOR_MESSAGE;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsMapContaining.hasEntry;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

        Optional<TestNamedCacheServiceProvider> optional = TestNamedCacheServiceProvider.getProvider();
        assertThat(optional.isPresent(), is(true));
        s_serviceProvider = optional.get();
        }

    @BeforeEach
    void setupEach()
        {
        NamedCache testCache = new CacheStub<>(TEST_CACHE_NAME, false);
        m_testAsyncCache = testCache.async();

        m_testCCF = mock(ConfigurableCacheFactory.class);
        when(m_testCCF.ensureCache(eq(TEST_CACHE_NAME), any())).thenReturn(testCache);
        when(m_testCCF.ensureCache(eq(TEST_CACHE_NAME), any(ClassLoader.class))).thenReturn(testCache);
        when(m_testCCF.getScopeName()).thenReturn(GrpcDependencies.DEFAULT_SCOPE);

        m_ccfSupplier = ConfigurableCacheFactorySuppliers.fixed(m_testCCF);

        Registry registry = mock(Registry.class);
        when(registry.ensureGlobalName(anyString())).thenReturn("foo");

        m_dependencies = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        m_dependencies.setSerializerFactory(s_serializerProducer);
        m_dependencies.setRegistry(registry);
        m_dependencies.setExecutor(ForkJoinPool.commonPool());
        m_dependencies.setConfigurableCacheFactorySupplier(m_ccfSupplier); 
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldCreateRequestHolder() throws Exception
        {
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;

        CompletionStage<CacheRequestHolder<String, Void>> stage = 
                service.createHolderAsync("foo", GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, POF_FORMAT);
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
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;

        CompletionStage<CacheRequestHolder<String, Void>> stage =
                service.createHolderAsync(null, GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, POF_FORMAT);
        assertThat(stage, is(notNullValue()));

        Throwable error = assertThrows(Throwable.class, () ->
                stage.toCompletableFuture().get(1, TimeUnit.MINUTES));
        Throwable cause = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_REQUEST_MESSAGE));
        }

    @Test
    public void shouldNotCreateRequestHolderIfCacheNameIsNull()
        {
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;

        CompletionStage<CacheRequestHolder<String, Void>> stage =
                service.createHolderAsync(GrpcDependencies.DEFAULT_SCOPE, "foo", null, POF_FORMAT);
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
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;

        CompletionStage<CacheRequestHolder<String, Void>> stage =
                service.createHolderAsync(GrpcDependencies.DEFAULT_SCOPE, "foo", "", POF_FORMAT);
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
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;

        CompletionStage<CacheRequestHolder<String, Void>> stage =
                service.createHolderAsync("foo", GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, "BAD");
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
        ByteString                 bytes   = BinaryHelper.toByteString(extractor, SERIALIZER);
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        ValueExtractor<?, ?>       result  = service.ensureValueExtractor(bytes, SERIALIZER);
        assertThat(result, is(notNullValue()));
        assertThat(result, is(extractor));
        }

    @Test
    public void shouldEnsureValueExtractorWhenByteStringIsNull()
        {
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        StatusRuntimeException error   = assertThrows(StatusRuntimeException.class,
                                                      () -> service.ensureValueExtractor(null, SERIALIZER));
        Status                 status  = error.getStatus();
        assertThat(status.getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(status.getDescription(), is(MISSING_EXTRACTOR_MESSAGE));
        }

    @Test
    public void shouldEnsureValueExtractorWhenByteStringIsEmpty()
        {
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        StatusRuntimeException error   = assertThrows(StatusRuntimeException.class,
                                                      () -> service.ensureValueExtractor(ByteString.EMPTY, SERIALIZER));
        Status                 status  = error.getStatus();
        assertThat(status.getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(status.getDescription(), is(MISSING_EXTRACTOR_MESSAGE));
        }

    @Test
    public void shouldEnsureFilter()
        {
        Filter                filter  = new EqualsFilter("foo", "bar");
        ByteString                 bytes   = BinaryHelper.toByteString(filter, SERIALIZER);
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        Filter                     result  = service.ensureFilter(bytes, SERIALIZER);
        assertThat(result, is(filter));
        }

    @Test
    public void shouldEnsureFilterWhenByteStringIsNull()
        {
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        Filter                     result  = service.ensureFilter(null, SERIALIZER);
        assertThat(result, is(instanceOf(AlwaysFilter.class)));
        }

    @Test
    public void shouldEnsureFilterWhenByteStringIsEmpty()
        {
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        Filter                     result  = service.ensureFilter(ByteString.EMPTY, SERIALIZER);
        assertThat(result, is(instanceOf(AlwaysFilter.class)));
        }


    @Test
    public void shouldDeserializeComparator()
        {
        Comparator            comparator = new UniversalExtractor("foo");
        ByteString                 bytes   = BinaryHelper.toByteString(comparator, SERIALIZER);
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        Comparator                 result  = service.deserializeComparator(bytes, SERIALIZER);
        assertThat(result, is(comparator));
        }

    @Test
    public void shouldDeserializeComparatorWhenByteStringIsNull()
        {
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        Comparator                 result  = service.deserializeComparator(null, SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    @Test
    public void shouldDeserializeComparatorWhenByteStringIsEmpty()
        {
        BaseNamedCacheServiceImpl service = s_serviceProvider.getBaseService(m_dependencies);;
        Comparator                 result  = service.deserializeComparator(ByteString.EMPTY, SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    // ----- aggregate() tests ----------------------------------------------

    @Test
    public void shouldNotExecuteAggregateWithoutCacheName() throws Exception
        {
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        AggregateRequest               request  = AggregateRequest.newBuilder()
                                                        .setFormat(JAVA_FORMAT)
                                                        .setAggregator(s_bytes1)
                                                        .build();

        service.aggregate(request, observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldNotExecuteAggregateWithoutAggregator() throws Exception
        {
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        AggregateRequest               request  = AggregateRequest.newBuilder()
                                                        .setScope(GrpcDependencies.DEFAULT_SCOPE)
                                                        .setCache(TEST_CACHE_NAME)
                                                        .setFormat(JAVA_FORMAT)
                                                        .build();

        service.aggregate(request, observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(),
                   is("the request does not contain a serialized entry aggregator"));
        }

    @Test
    public void shouldHandleAggregateWithFilterError() throws Exception
        {
        when(m_testAsyncCache.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenThrow(ERROR);

        Filter                         filter               = new EqualsFilter();
        ByteString                     serializedFilter     = BinaryHelper.toByteString(filter, SERIALIZER);
        InvocableMap.EntryAggregator   aggregator           = new Count();
        ByteString                     serializedAggregator = BinaryHelper.toByteString(aggregator, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();

        service.aggregate(Requests.aggregate(GrpcDependencies.DEFAULT_SCOPE,
                                              TEST_CACHE_NAME, JAVA_FORMAT,
                                              serializedFilter,
                                              serializedAggregator), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleAggregateWithFilterAsyncError() throws Exception
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);
        when(m_testAsyncCache.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenReturn(failed);

        Filter                       filter               = new EqualsFilter();
        ByteString                   serializedFilter     = BinaryHelper.toByteString(filter, SERIALIZER);
        InvocableMap.EntryAggregator aggregator           = new Count();
        ByteString                   serializedAggregator = BinaryHelper.toByteString(aggregator, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        AggregateRequest             request              = Requests.aggregate(GrpcDependencies.DEFAULT_SCOPE,
                                                                               TEST_CACHE_NAME,
                                                                               JAVA_FORMAT,
                                                                               serializedFilter,
                                                                               serializedAggregator);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleAggregateWithKeysError() throws Exception
        {
        when(m_testAsyncCache.aggregate(any(Collection.class),
                                        any(InvocableMap.EntryAggregator.class))).thenThrow(ERROR);

        InvocableMap.EntryAggregator aggregator           = new Count();
        ByteString                   serializedAggregator = BinaryHelper.toByteString(aggregator, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        AggregateRequest             request              = Requests.aggregate(GrpcDependencies.DEFAULT_SCOPE,
                                                                               TEST_CACHE_NAME,
                                                                               JAVA_FORMAT,
                                                                               s_byteStringList,
                                                                               serializedAggregator);

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleAggregateWithKeysAsyncError() throws Exception
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);
        when(m_testAsyncCache.aggregate(any(Collection.class),
                                        any(InvocableMap.EntryAggregator.class))).thenReturn(failed);

        InvocableMap.EntryAggregator  aggregator           = new Count();
        ByteString                 serializedAggregator = BinaryHelper.toByteString(aggregator, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        AggregateRequest              request              = Requests.aggregate(GrpcDependencies.DEFAULT_SCOPE,
                                                                                TEST_CACHE_NAME,
                                                                                JAVA_FORMAT,
                                                                                s_byteStringList,
                                                                                serializedAggregator);

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- clear() tests --------------------------------------------------

    @Test
    public void shouldNotExecuteClearWithoutCacheName() throws Exception
        {
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<Empty>  observer = new TestStreamObserver<>();

        service.clear(ClearRequest.newBuilder().build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleClearError() throws Exception
        {
        NamedCache<Binary, Binary> cache = mock(NamedCache.class);
        when(m_testAsyncCache.getNamedCache()).thenReturn(cache);
        doThrow(ERROR).when(cache).clear();

        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<Empty>  observer = new TestStreamObserver<>();

        service.clear(Requests.clear(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }


    @Test
    public void shouldHandleTruncateError() throws Exception
        {
        NamedCache<Binary, Binary> cache = mock(NamedCache.class);
        when(m_testAsyncCache.getNamedCache()).thenReturn(cache);
        doThrow(ERROR).when(cache).truncate();

        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<Empty>  observer = new TestStreamObserver<>();
        service.truncate(Requests.truncate(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- containsEntry() tests -----------------------------------

    @Test
    public void shouldNotExecuteContainsEntryWithoutCacheName() throws Exception
        {
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsEntry(ContainsEntryRequest.newBuilder().build(), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleContainsEntryError() throws Exception
        {
        when(m_testAsyncCache.invoke(any(Binary.class), isA(BinaryProcessors.BinaryContainsValueProcessor.class)))
                .thenThrow(ERROR);

        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        ContainsEntryRequest          request  = Requests.containsEntry(GrpcDependencies.DEFAULT_SCOPE,
                                                                        TEST_CACHE_NAME,
                                                                        JAVA_FORMAT,
                                                                        s_bytes1,
                                                                        s_bytes2);

        service.containsEntry(request, observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleContainsEntryAsyncError() throws Exception
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);

        when(m_testAsyncCache.invoke(any(Binary.class), isA(BinaryProcessors.BinaryContainsValueProcessor.class))).thenReturn(failed);

        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();

        service.containsEntry(Requests.containsEntry(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME,
                                                      JAVA_FORMAT, s_bytes1,
                                                      s_bytes2), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- containsKey() tests --------------------------------------------

    @Test
    public void shouldNotExecuteContainsKeyWithoutCacheName() throws Exception
        {
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();

        service.containsKey(ContainsKeyRequest.newBuilder().build(), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleContainsKeyError() throws Exception
        {
        when(m_testAsyncCache.containsKey(any(Binary.class))).thenThrow(ERROR);

        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        service.containsKey(Requests.containsKey(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                          s_bytes1), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleContainsKeyAsyncError() throws Exception
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);

        when(m_testAsyncCache.containsKey(any(Binary.class))).thenReturn(failed);

        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        service.containsKey(Requests.containsKey(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                          s_bytes1), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- containsValue() tests ------------------------------------------

    @Test
    public void shouldNotExecuteContainsValueWithoutCacheName() throws Exception
        {
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        service.containsValue(ContainsValueRequest.newBuilder().build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleContainsValueError() throws Exception
        {
        when(m_testAsyncCache.aggregate(any(Filter.class), isA(Count.class))).thenThrow(ERROR);

        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        service.containsValue(Requests.containsValue(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME,
                                                                                              JAVA_FORMAT,
                                                                                              s_bytes1), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleContainsValueAsyncError() throws Exception
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);

        when(m_testAsyncCache.aggregate(any(Filter.class), isA(Count.class))).thenReturn(failed);

        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.containsValue(Requests.containsValue(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME,
                                                                                             JAVA_FORMAT, s_bytes1), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- destroy() tests ------------------------------------------------

    @Test
    public void shouldNotExecuteDestroyWithoutCacheName() throws Exception
        {
        TestStreamObserver<Empty>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.destroy(DestroyRequest.newBuilder().build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleDestroyError() throws Exception
        {
        NamedCache cache = mock(NamedCache.class);
        when(m_testAsyncCache.getNamedCache()).thenReturn(cache);
        doThrow(ERROR).when(m_testCCF).destroyCache(any(NamedCache.class));
        doThrow(ERROR).when(cache).destroy();

        TestStreamObserver<Empty>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        service.destroy(Requests.destroy(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- entrySet(Filter) tests -----------------------------------------

    @Test
    public void shouldNotExecuteEntrySetWithoutCacheName() throws Exception
        {
        Filter                    filter      = new EqualsFilter("foo", "bar");
        ByteString                filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        TestStreamObserver<Entry>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

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
    public void shouldHandleEntrySetErrorWithoutComparator() throws Exception
        {
        when(m_testAsyncCache.entrySet(any(Filter.class), any(Consumer.class))).thenThrow(ERROR);

        Filter<Binary>            filter      = new EqualsFilter<>("foo", "bar");
        ByteString                 filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<Entry>  observer    = new TestStreamObserver<>();

        service.entrySet(Requests.entrySet(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleEntrySetErrorWithComparator() throws Exception
        {
        when(m_testAsyncCache.entrySet(any(Filter.class), nullable(Comparator.class))).thenThrow(ERROR);

        Filter<Binary>            filter          = new EqualsFilter<>("foo", "bar");
        ByteString                filterBytes     = BinaryHelper.toByteString(filter, SERIALIZER);
        Comparator<?>             comparator      = new ReflectionExtractor<>("foo");
        ByteString                 comparatorBytes = BinaryHelper.toByteString(comparator, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<Entry>  observer        = new TestStreamObserver<>();

        service.entrySet(Requests.entrySet(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes, comparatorBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleEntrySetAsyncErrorWithoutComparator() throws Exception
        {
        CompletableFuture<Set<Map.Entry<Binary, Binary>>> failed = failedFuture(ERROR);

        when(m_testAsyncCache.entrySet(any(Filter.class), any(Consumer.class))).thenReturn(failed);

        Filter                    filter      = new EqualsFilter("foo", "bar");
        ByteString                 filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<Entry>  observer    = new TestStreamObserver<>();

        service.entrySet(Requests.entrySet(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleEntrySetAsyncErrorWithComparator() throws Exception
        {
        CompletableFuture<Set<Map.Entry<Binary, Binary>>> failed = failedFuture(ERROR);
        when(m_testAsyncCache.entrySet(any(Filter.class), any(Comparator.class))).thenReturn(failed);

        Filter                    filter          = new EqualsFilter("foo", "bar");
        ByteString                filterBytes     = BinaryHelper.toByteString(filter, SERIALIZER);
        Comparator<?>             comparator      = new ReflectionExtractor<>("foo");
        ByteString                 comparatorBytes = BinaryHelper.toByteString(comparator, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<Entry>  observer        = new TestStreamObserver<>();

        service.entrySet(Requests.entrySet(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes, comparatorBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- get() tests ----------------------------------------------------

    @Test
    public void shouldNotExecuteGetWithoutCacheName() throws Exception
        {
        TestStreamObserver<OptionalValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        service.get(GetRequest.newBuilder().build(), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleGetError() throws Exception
        {
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryGetProcessor.class))).thenThrow(ERROR);

        TestStreamObserver<OptionalValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        service.get(Requests.get(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleGetAsyncError() throws Exception
        {
        CompletableFuture<Binary> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryGetProcessor.class))).thenReturn(failed);

        TestStreamObserver<OptionalValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        service.get(Requests.get(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);

        Throwable error  = observer.getError();
        Throwable cause  = rootCause(error);
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- getAll() tests -------------------------------------------------

    @Test
    public void shouldExecuteGetAllWithNoKeys() throws Exception
        {
        TestStreamObserver<Entry>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.getAll(GetAllRequest.newBuilder()
                .setScope(GrpcDependencies.DEFAULT_SCOPE)
                .setCache(TEST_CACHE_NAME).build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoValues()
                .assertNoErrors();
        }

    @Test
    public void shouldExecuteGetAllWithResults() throws Exception
        {
        Map<Binary, Binary> mapResults = new HashMap<>();
        mapResults.put(serialize("one"),   serialize("value-1"));
        mapResults.put(serialize("two"),   serialize("value-2"));
        mapResults.put(serialize("three"), serialize("value-3"));
        mapResults.put(serialize("four"),  serialize("value-4"));
        mapResults.put(serialize("five"),  serialize("value-5"));

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        when(m_testAsyncCache.invokeAll(any(Collection.class), any(BinaryProcessors.BinaryGetProcessor.class), any(Consumer.class)))
                .thenAnswer((Answer<CompletableFuture<Void>>) invocation ->
                    {
                    Consumer<Map.Entry<Binary, Binary>> consumer = invocation.getArgument(2, Consumer.class);
                    mapResults.entrySet().forEach(consumer);
                    return future;
                    });

        TestStreamObserver<Entry>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.getAll(Requests.getAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_byteStringList), observer);

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
        TestStreamObserver<Entry>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

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
        when(m_testAsyncCache.invokeAll(any(Collection.class), any(BinaryProcessors.BinaryGetProcessor.class), any(Consumer.class))).thenThrow(ERROR);

        TestStreamObserver<Entry>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.getAll(Requests.getAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_byteStringList), observer);

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
        when(m_testAsyncCache.invokeAll(any(Collection.class), any(BinaryProcessors.BinaryGetProcessor.class), any(Consumer.class))).thenReturn(failed);

        TestStreamObserver<Entry>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.getAll(Requests.getAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_byteStringList), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- invoke() tests -------------------------------------------------

    @Test
    public void shouldNotExecuteInvokeWithoutCacheName() throws Exception
        {
        InvocableMap.EntryProcessor    processor           = new ExtractorProcessor("length()");
        ByteString                     serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        TestStreamObserver<BytesValue> observer            = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.invoke(InvokeRequest.newBuilder().setProcessor(serializedProcessor).build(), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldNotExecuteInvokeWithoutEntryProcessor() throws Exception
        {
        InvokeRequest request  = InvokeRequest.newBuilder()
                .setScope(GrpcDependencies.DEFAULT_SCOPE)
                .setCache(TEST_CACHE_NAME).build();

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = s_serviceProvider.getService(m_dependencies);
        service.invoke(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(MISSING_PROCESSOR_MESSAGE));
        }

    @Test
    public void shouldHandleInvokeError() throws Exception
        {
        when(m_testAsyncCache.invoke(any(Binary.class), any(ExtractorProcessor.class))).thenThrow(ERROR);

        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        InvokeRequest               request             = Requests.invoke(GrpcDependencies.DEFAULT_SCOPE,
                                                                          TEST_CACHE_NAME,
                                                                          JAVA_FORMAT,
                                                                          s_bytes1,
                                                                          serializedProcessor);

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = s_serviceProvider.getService(m_dependencies);
        service.invoke(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleInvokeAsyncError() throws Exception
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invoke(any(Binary.class), any(ExtractorProcessor.class))).thenReturn(failed);

        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                  serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        InvokeRequest               request             = Requests.invoke(GrpcDependencies.DEFAULT_SCOPE,
                                                                          TEST_CACHE_NAME,
                                                                          JAVA_FORMAT,
                                                                          s_bytes1,
                                                                          serializedProcessor);

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = s_serviceProvider.getService(m_dependencies);
        service.invoke(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- invokeAll() tests ----------------------------------------------

    @Test
    public void shouldNotExecuteInvokeAllWithoutCacheName() throws Exception
        {
        TestStreamObserver<Entry>   observer  = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        InvocableMap.EntryProcessor processor = new ExtractorProcessor("length()");
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
        TestStreamObserver<Entry>  observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        InvokeAllRequest          request  = InvokeAllRequest.newBuilder()
                                                            .setScope(GrpcDependencies.DEFAULT_SCOPE)
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
        when(m_testAsyncCache.invokeAll(any(Filter.class), any(ExtractorProcessor.class), any(Consumer.class))).thenThrow(ERROR);

        TestStreamObserver<Entry>   observer            = new TestStreamObserver<>();
        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                 serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.invokeAll(Requests.invokeAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
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
        CompletableFuture<Void>   failed   = failedFuture(ERROR);

        when(m_testAsyncCache.invokeAll(any(Filter.class), any(ExtractorProcessor.class), any(Consumer.class))).thenReturn(failed);

        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                 serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.invokeAll(Requests.invokeAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
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
        when(m_testAsyncCache.invokeAll(any(Collection.class), any(ExtractorProcessor.class), any(Consumer.class))).thenThrow(ERROR);

        TestStreamObserver<Entry>   observer            = new TestStreamObserver<>();
        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                 serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.invokeAll(Requests.invokeAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
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

        when(m_testAsyncCache.invokeAll(any(Collection.class), any(ExtractorProcessor.class), any(Consumer.class))).thenReturn(failed);

        InvocableMap.EntryProcessor processor           = new ExtractorProcessor("length()");
        ByteString                 serializedProcessor = BinaryHelper.toByteString(processor, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

        service.invokeAll(Requests.invokeAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT,
                                             s_byteStringList, serializedProcessor), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- isEmpty() tests ------------------------------------------------

    @Test
    public void shouldNotExecuteIsEmptyWithoutCacheName() throws Exception
        {
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        BaseNamedCacheServiceImpl     service  = s_serviceProvider.getBaseService(m_dependencies);;

        service.isEmpty(IsEmptyRequest.newBuilder().build(), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandleIsEmptyError() throws Exception
        {
        when(m_testAsyncCache.isEmpty()).thenThrow(ERROR);

        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        BaseNamedCacheServiceImpl     service  = s_serviceProvider.getBaseService(m_dependencies);;
        service.isEmpty(Requests.isEmpty(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleIsEmptyAsyncError() throws Exception
        {
        CompletableFuture<Boolean> failed = failedFuture(ERROR);
        when(m_testAsyncCache.isEmpty()).thenReturn(failed);

        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        BaseNamedCacheServiceImpl service  = s_serviceProvider.getBaseService(m_dependencies);;
        service.isEmpty(Requests.isEmpty(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- keySet(Filter) tests -------------------------------------------

    @Test
    public void shouldNotExecuteKeySetWithoutCacheName() throws Exception
        {
        Filter<Binary>                 filter      = new EqualsFilter<>("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

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
        when(m_testAsyncCache.keySet(any(Filter.class), any(Consumer.class))).thenThrow(ERROR);

        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.keySet(Requests.keySet(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

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
        when(m_testAsyncCache.keySet(any(Filter.class), any(Consumer.class))).thenReturn(failed);

        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.keySet(Requests.keySet(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- put tests ------------------------------------------------------

    @Test
    public void shouldNotExecutePutWithoutCacheName() throws Exception
        {
        PutRequest request = PutRequest.newBuilder()
                .setFormat(JAVA_FORMAT)
                .setKey(s_bytes1)
                .setValue(s_bytes2).build();

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        BaseNamedCacheServiceImpl      service  = s_serviceProvider.getBaseService(m_dependencies);;
        service.put(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandlePutError() throws Exception
        {
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryPutProcessor.class))).thenThrow(ERROR);

        PutRequest request = Requests.put(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1, s_bytes2);

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = s_serviceProvider.getService(m_dependencies);;
        service.put(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePutAsyncError() throws Exception
        {
        CompletableFuture<Binary> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryPutProcessor.class))).thenReturn(failed);

        PutRequest request = Requests.put(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                          s_bytes2);

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = s_serviceProvider.getService(m_dependencies);;
        service.put(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- putAll tests ---------------------------------------------------

    @Test
    public void shouldNotExecutePutAllWithoutCacheName() throws Exception
        {

        Entry         entry   = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>   entries = Collections.singletonList(entry);
        PutAllRequest request = PutAllRequest.newBuilder().setFormat(JAVA_FORMAT).addAllEntry(entries).build();

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service  = s_serviceProvider.getService(m_dependencies);;

        service.putAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandlePutAllError() throws Exception
        {
        Entry         entry   = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>   entries = Collections.singletonList(entry);
        PutAllRequest request = Requests.putAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, entries);

        when(m_testAsyncCache.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenThrow(ERROR);

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service  = s_serviceProvider.getService(m_dependencies);;

        service.putAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePartitionedPutAllError() throws Exception
        {
        String          sCacheName = "test-partitioned";
        NamedCache      testCache  = new CacheStub<>(sCacheName, true);
        AsyncNamedCache asyncCache = testCache.async();

        when(m_testCCF.ensureCache(eq(sCacheName), any(ClassLoader.class))).thenReturn(testCache);
        when(asyncCache.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenThrow(ERROR);

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service  = s_serviceProvider.getService(m_dependencies);;
        Entry                     entry    = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>               entries  = Collections.singletonList(entry);

        service.putAll(Requests.putAll(GrpcDependencies.DEFAULT_SCOPE, sCacheName, JAVA_FORMAT, entries), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePutAllAsyncError() throws Exception
        {
        CompletableFuture<Map<Binary, Binary>> failed = failedFuture(ERROR);
        when(m_testAsyncCache.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenReturn(failed);

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service  = s_serviceProvider.getService(m_dependencies);;
        Entry                     entry    = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>               entries  = Collections.singletonList(entry);

        service.putAll(Requests.putAll(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, entries), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePartitionedPutAllAsyncError() throws Exception
        {
        String          sCacheName = "test-partitioned";
        NamedCache      testCache  = new CacheStub<>(sCacheName, true);
        AsyncNamedCache asyncCache = testCache.async();

        when(m_testCCF.ensureCache(eq(sCacheName), any(ClassLoader.class))).thenReturn(testCache);
        when(asyncCache.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenThrow(ERROR);

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service  = s_serviceProvider.getService(m_dependencies);;
        Entry                     entry    = Entry.newBuilder().setKey(s_bytes1).setValue(s_bytes2).build();
        List<Entry>               entries  = Collections.singletonList(entry);

        service.putAll(Requests.putAll(GrpcDependencies.DEFAULT_SCOPE, sCacheName, JAVA_FORMAT, entries), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- putIfAbsent tests ----------------------------------------------

    @Test
    public void shouldNotExecutePutIfAbsentWithoutCacheName() throws Exception
        {
        PutIfAbsentRequest request = PutIfAbsentRequest
                .newBuilder()
                .setFormat(JAVA_FORMAT)
                .setKey(s_bytes1).setValue(s_bytes2)
                .build();

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = s_serviceProvider.getService(m_dependencies);;

        service.putIfAbsent(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(instanceOf(StatusRuntimeException.class)));
        assertThat(((StatusRuntimeException) cause).getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));
        assertThat(((StatusRuntimeException) cause).getStatus().getDescription(), is(INVALID_CACHE_NAME_MESSAGE));
        }

    @Test
    public void shouldHandlePutIfAbsentError() throws Exception
        {
        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryPutIfAbsentProcessor.class)))
                .thenThrow(ERROR);

        PutIfAbsentRequest request = Requests.putIfAbsent(GrpcDependencies.DEFAULT_SCOPE,
                                                          TEST_CACHE_NAME,
                                                          JAVA_FORMAT,
                                                          s_bytes1,
                                                          s_bytes2);

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = s_serviceProvider.getService(m_dependencies);;

        service.putIfAbsent(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandlePutIfAbsentAsyncError() throws Exception
        {
        CompletableFuture<Binary> failed = failedFuture(ERROR);
        PutIfAbsentRequest        request = Requests.putIfAbsent(GrpcDependencies.DEFAULT_SCOPE,
                                                                 TEST_CACHE_NAME,
                                                                 JAVA_FORMAT,
                                                                 s_bytes1,
                                                                 s_bytes2);

        when(m_testAsyncCache.invoke(any(Binary.class), any(BinaryProcessors.BinaryPutIfAbsentProcessor.class)))
                .thenReturn(failed);

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = s_serviceProvider.getService(m_dependencies);;

        service.putIfAbsent(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    // ----- values(Filter) tests -------------------------------------------

    @Test
    public void shouldNotExecuteValuesWithoutCacheName() throws Exception
        {
        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);

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
        when(m_testAsyncCache.values(any(Filter.class), any(Consumer.class))).thenThrow(ERROR);

        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.values(Requests.values(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        cause.printStackTrace();
        assertThat(cause, is(sameInstance(ERROR)));
        }
    @Test
    public void shouldHandleValuesWithComparatorError() throws Exception
        {
        when(m_testAsyncCache.values(any(Filter.class), nullable(Comparator.class))).thenThrow(ERROR);

        Comparator                     comparator  = new UniversalExtractor("foo");
        ByteString                     cmpBytes    = BinaryHelper.toByteString(comparator, SERIALIZER);
        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.values(Requests.values(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes, cmpBytes), observer);

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
        when(m_testAsyncCache.values(any(Filter.class), any(Consumer.class))).thenReturn(failed);

        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.values(Requests.values(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertNoValues()
                .assertError(StatusRuntimeException.class);

        Throwable cause = rootCause(observer.getError());
        assertThat(cause, is(sameInstance(ERROR)));
        }

    @Test
    public void shouldHandleValuesWithComparatorAsyncError() throws Exception
        {
        CompletableFuture<Map> failed = failedFuture(ERROR);
        when(m_testAsyncCache.values(any(Filter.class), nullable(Comparator.class))).thenReturn(failed);

        Comparator                     comparator  = new UniversalExtractor("foo");
        ByteString                     cmpBytes    = BinaryHelper.toByteString(comparator, SERIALIZER);
        Filter                         filter      = new EqualsFilter("foo", "bar");
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, SERIALIZER);
        NamedCacheService service = s_serviceProvider.getService(m_dependencies);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();

        service.values(Requests.values(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, filterBytes, cmpBytes), observer);

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
            if (partitioned)
                {
                DistributedCacheService distributedCacheService = (DistributedCacheService) cacheService;
                when(distributedCacheService.instantiateKeyToBinaryConverter(any(), anyBoolean())).thenReturn(new ConverterDown());
                }
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

        @Override
        public void clear()
            {
            f_async.getNamedCache().clear();
            }

        @Override
        public void destroy()
            {
            f_async.getNamedCache().destroy();
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
    
    private NamedCacheService.DefaultDependencies m_dependencies;

    private static ByteString s_bytes1;

    private static ByteString s_bytes2;

    private static ByteString s_bytes3;

    private static ByteString s_bytes4;

    private static ByteString s_bytes5;

    private static ByteString s_filterBytes;

    private static List<ByteString> s_byteStringList;

    private AsyncNamedCache<Binary, Binary> m_testAsyncCache;

    private static TestNamedCacheServiceProvider s_serviceProvider;

    private ConfigurableCacheFactory m_testCCF;
    }
