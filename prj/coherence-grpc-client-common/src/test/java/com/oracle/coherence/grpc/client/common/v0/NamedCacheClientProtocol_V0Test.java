/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.v0;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Jonathan Knight  2019.11.26
 * @since 20.06
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class NamedCacheClientProtocol_V0Test
    {
//    // ----- helper methods -------------------------------------------------
//
//    <K, V> LegacyAsyncNamedCacheClient<K, V> createClient()
//        {
//        NamedCacheGrpcClient client = createMockService();
//        return createClient(client);
//        }
//
//    private <K, V> LegacyAsyncNamedCacheClient<K, V> createClient(NamedCacheGrpcClient client)
//        {
//        Channel                           channel    = mock(Channel.class);
//        GrpcCacheLifecycleEventDispatcher dispatcher = mock(GrpcCacheLifecycleEventDispatcher.class);
//
//        AsyncNamedCacheClient.DefaultDependencies deps
//                = new  AsyncNamedCacheClient.DefaultDependencies("test", channel, dispatcher);
//
//        deps.setScope(GrpcDependencies.DEFAULT_SCOPE);
//        deps.setSerializer(SERIALIZER, FORMAT);
//        deps.setClient(client);
//
//        return new LegacyAsyncNamedCacheClient<>(deps);
//        }
//
//    // ----- test methods ---------------------------------------------------
//
//    @Test
//    public void shouldHandleUnsupportedStreamMethods()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new StatusRuntimeException(Status.UNIMPLEMENTED);
//        observer.onError(error);
//
//        assertFutureErrorType(future, UnsupportedOperationException.class);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    public void shouldHandleInitFailure()
//        {
//        GrpcCacheLifecycleEventDispatcher dispatcher = mock(GrpcCacheLifecycleEventDispatcher.class);
//        Channel                           channel    = mock(Channel.class);
//
//        AsyncNamedCacheClient.DefaultDependencies deps
//                = new AsyncNamedCacheClient.DefaultDependencies("test", channel, dispatcher);
//
//        deps.setScope(GrpcDependencies.DEFAULT_SCOPE);
//        deps.setSerializer(SERIALIZER, FORMAT);
//
//        NamedCacheGrpcClient client = mock(NamedCacheGrpcClient.class);
//        RuntimeException     ex     = new RuntimeException("Computer says No!");
//
//        when(client.events(any(StreamObserver.class))).thenAnswer(invocation ->
//            {
//            StreamObserver observer = invocation.getArgument(0);
//
//            return new StreamObserver<MapListenerRequest>()
//                {
//                @Override
//                public void onNext(MapListenerRequest request)
//                    {
//                    observer.onError(ex);
//                    }
//
//                @Override
//                public void onError(Throwable throwable)
//                    {
//                    }
//
//                @Override
//                public void onCompleted()
//                    {
//                    }
//                };
//            });
//
//        deps.setClient(client);
//
//        Exception result = assertThrows(Exception.class, () -> new LegacyAsyncNamedCacheClient<>(deps));
//        assertThat(result, causedBy(is(ex)));
//        }
//
//    @Test
//    public void shouldHandleInitCompletionWithoutSubscription()
//        {
//        GrpcCacheLifecycleEventDispatcher dispatcher = mock(GrpcCacheLifecycleEventDispatcher.class);
//        Channel                           channel    = mock(Channel.class);
//
//        AsyncNamedCacheClient.DefaultDependencies deps
//                = new AsyncNamedCacheClient.DefaultDependencies("test", channel, dispatcher);
//
//        deps.setScope(GrpcDependencies.DEFAULT_SCOPE);
//        deps.setSerializer(SERIALIZER, FORMAT);
//
//        NamedCacheGrpcClient client = mock(NamedCacheGrpcClient.class);
//
//        when(client.events(any(StreamObserver.class))).thenAnswer(invocation ->
//            {
//            StreamObserver observer = invocation.getArgument(0);
//
//            return new StreamObserver<MapListenerRequest>()
//                {
//                @Override
//                public void onNext(MapListenerRequest request)
//                    {
//                    observer.onCompleted();
//                    }
//
//                @Override
//                public void onError(Throwable throwable)
//                    {
//                    }
//
//                @Override
//                public void onCompleted()
//                    {
//                    }
//                };
//            });
//
//        deps.setClient(client);
//
//        Exception result = assertThrows(Exception.class, () -> new LegacyAsyncNamedCacheClient<>(deps));
//        assertThat(result, causedBy(instanceOf(IllegalStateException.class)));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorOnly() throws Exception
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = future.get();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorOnlyWithNoResults() throws Exception
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = future.get();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorOnlyWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future = client.invokeAll(processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//        assertFutureError(future, error);
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorOnlyWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(processor);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//        }
//
//    @Test
//    void shouldInvokeAllWithKeys() throws Exception
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(keys, processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture(), eq(PriorityTask.TIMEOUT_DEFAULT));
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = future.get();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysWithNoResults() throws Exception
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(keys, processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture(), eq(PriorityTask.TIMEOUT_DEFAULT));
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = future.get();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(keys, processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture(), eq(PriorityTask.TIMEOUT_DEFAULT));
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//
//        assertFutureError(future, error);
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class), eq(PriorityTask.TIMEOUT_DEFAULT));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(keys, processor);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//        }
//
//    @Test
//    void shouldInvokeAllWithFilter() throws Exception
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(filter, processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = future.get();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterWithNoResults() throws Exception
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(filter, processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = future.get();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(filter, processor);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//
//        assertFutureError(future, error);
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        CompletableFuture<Map<String, String>>              future    = client.invokeAll(filter, processor);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndConsumer()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndConsumerWithNoResults()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndConsumerWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndConsumerWithCallbackError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        BadConsumer<String, String>                         consumer  = new BadConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertFutureError(future, consumer.getError());
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndConsumerWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndConsumer()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request = reqCaptor.getValue();
//        StreamObserver observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndConsumerWithNoResults()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer = new TestConsumer<>();
//        CompletableFuture<Void>                             future = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndConsumerWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndConsumerWithCallbackError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        BadConsumer<String, String>                         consumer = new BadConsumer<>();
//        CompletableFuture<Void>                             future = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<StreamObserver> obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(any(InvokeAllRequest.class), obsCaptor.capture());
//
//        StreamObserver observer = obsCaptor.getValue();
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertFutureError(future, consumer.getError());
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndConsumerWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndConsumer()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndConsumerWithNoResults()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndConsumerWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndConsumerWithCallbackError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        BadConsumer<String, String>                         consumer  = new BadConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<StreamObserver> obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(any(InvokeAllRequest.class), obsCaptor.capture());
//
//        StreamObserver observer = obsCaptor.getValue();
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertFutureError(future, consumer.getError());
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndConsumerWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestConsumer<String, String>                        consumer  = new TestConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndBiConsumer()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndBiConsumerWithNoResults()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndBiConsumerWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndBiConsumerWithCallbackError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        BadBiConsumer<String, String>                       consumer  = new BadBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(instanceOf(AlwaysFilter.class)));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertFutureError(future, consumer.getError());
//        }
//
//    @Test
//    void shouldInvokeAllWithProcessorAndBiConsumerWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndBiConsumer()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndBiConsumerWithNoResults()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndBiConsumerWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(nullValue()));
//        assertThat(request.getKeysCount(), is(keys.size()));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(0), SERIALIZER), is("key-1"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(1), SERIALIZER), is("key-2"));
//        assertThat(BinaryHelper.fromByteString(request.getKeys(2), SERIALIZER), is("key-3"));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndBiConsumerWithCallbackError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        BadBiConsumer<String, String>                       consumer  = new BadBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<StreamObserver> obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(any(InvokeAllRequest.class), obsCaptor.capture());
//
//        StreamObserver observer = obsCaptor.getValue();
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertFutureError(future, consumer.getError());
//        }
//
//    @Test
//    void shouldInvokeAllWithKeysAndBiConsumerWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        List<String>                                        keys      = Arrays.asList("key-1", "key-2", "key-3");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future = client.invokeAll(keys, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndBiConsumer()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(2));
//        assertThat(map, hasEntry("key-1", "value-1"));
//        assertThat(map, hasEntry("key-2", "value-2"));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndBiConsumerWithNoResults()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        observer.onCompleted();
//
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(false));
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndBiConsumerWithError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<InvokeAllRequest> reqCaptor = ArgumentCaptor.forClass(InvokeAllRequest.class);
//        ArgumentCaptor<StreamObserver>   obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(reqCaptor.capture(), obsCaptor.capture());
//
//        InvokeAllRequest request  = reqCaptor.getValue();
//        StreamObserver   observer = obsCaptor.getValue();
//
//        assertThat(request.getKeysCount(), is(0));
//        assertThat(BinaryHelper.fromByteString(request.getFilter(), SERIALIZER), is(filter));
//        assertThat(BinaryHelper.fromByteString(request.getProcessor(), SERIALIZER), is(processor));
//
//        Throwable error = new RuntimeException("Computer says No!");
//        observer.onError(error);
//
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndBiConsumerWithCallbackError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        doNothing().when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        BadBiConsumer<String, String>                       consumer  = new BadBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertThat(future.isDone(), is(false));
//
//        ArgumentCaptor<StreamObserver> obsCaptor = ArgumentCaptor.forClass(StreamObserver.class);
//        verify(client).invokeAllInternal(any(InvokeAllRequest.class), obsCaptor.capture());
//
//        StreamObserver observer = obsCaptor.getValue();
//
//        observer.onNext(createEntry("key-1", "value-1"));
//        observer.onNext(createEntry("key-2", "value-2"));
//        observer.onCompleted();
//
//        assertFutureError(future, consumer.getError());
//        }
//
//    @Test
//    void shouldInvokeAllWithFilterAndBiConsumerWithInvokeError()
//        {
//        LegacyAsyncNamedCacheClient<String, String> realClient = createClient();
//        LegacyAsyncNamedCacheClient<String, String> client     = Mockito.spy(realClient);
//
//        Throwable error = new RuntimeException("Computer says No!");
//        doThrow(error).when(client).invokeAllInternal(any(InvokeAllRequest.class), any(StreamObserver.class));
//
//        Filter<String>                                      filter    = new EqualsFilter<>("foo", "bar");
//        InvocableMap.EntryProcessor<String, String, String> processor = new ProcessorStub<>();
//        TestBiConsumer<String, String>                      consumer  = new TestBiConsumer<>();
//        CompletableFuture<Void>                             future    = client.invokeAll(filter, processor, consumer);
//
//        assertThat(future, is(notNullValue()));
//        assertFutureError(future, error);
//
//        Map<String, String> map = consumer.getResults();
//        assertThat(map.size(), is(0));
//        }
//
//    // ----- helper methods -------------------------------------------------
//
//    /**
//     * Create a mock {@link NamedCacheGrpcClient} ensuring that the
//     * mock will return a {@link StreamObserver} from the
//     * {@link NamedCacheGrpcClient#events(StreamObserver)}
//     * method.
//     *
//     * @return a mock {@link NamedCacheGrpcClient}
//     */
//    protected static NamedCacheGrpcClient createMockService()
//        {
//        NamedCacheGrpcClient service = mock(NamedCacheGrpcClient.class);
//        when(service.events(any(StreamObserver.class))).thenAnswer(invocation ->
//            {
//            StreamObserver observer = invocation.getArgument(0);
//
//            return new StreamObserver<MapListenerRequest>()
//                {
//                @Override
//                public void onNext(MapListenerRequest request)
//                    {
//                    MapListenerSubscribedResponse subscribed = MapListenerSubscribedResponse.newBuilder()
//                            .setUid(request.getUid())
//                            .build();
//
//                    observer.onNext(MapListenerResponse.newBuilder().setSubscribed(subscribed).build());
//                    }
//
//                @Override
//                public void onError(Throwable throwable)
//                    {
//                    }
//
//                @Override
//                public void onCompleted()
//                    {
//                    }
//                };
//            });
//
//        return service;
//        }
//
//    protected Entry createEntry(String key, String value)
//        {
//        return Entry.newBuilder()
//                .setKey(BinaryHelper.toByteString(ExternalizableHelper.toBinary(key, SERIALIZER)))
//                .setValue(BinaryHelper.toByteString(ExternalizableHelper.toBinary(value, SERIALIZER)))
//                .build();
//        }
//
//    /**
//     * Assert that a {@link CompletableFuture} completed exceptionally.
//     *
//     * @param future    the {@link CompletableFuture} to test
//     * @param expected  the expected exception
//     */
//    protected void assertFutureError(CompletableFuture<?> future, Throwable expected)
//        {
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(true));
//
//        try
//            {
//            future.get();
//            fail("Expected exception " + expected);
//            }
//        catch (InterruptedException | ExecutionException e)
//            {
//            assertThat(rootCause(e), is(sameInstance(expected)));
//            }
//        }
//
//    /**
//     * Assert that a {@link CompletableFuture} completed exceptionally.
//     *
//     * @param future  the {@link CompletableFuture} to test
//     * @param clazz   the expected throwable type
//     */
//    protected void assertFutureErrorType(CompletableFuture<?> future, Class<? extends Throwable> clazz)
//    {
//        assertThat(future.isDone(), is(true));
//        assertThat(future.isCompletedExceptionally(), is(true));
//
//        try
//            {
//            future.get();
//            fail("Expected exception of type" + clazz.getName());
//            }
//        catch (InterruptedException | ExecutionException e)
//            {
//            assertThat(rootCause(e).getClass(), is(clazz));
//            }
//    }
//
//    protected Throwable rootCause(Throwable t)
//        {
//        Throwable cause = t.getCause();
//        if (cause == null)
//            {
//            return t;
//            }
//        return rootCause(cause);
//        }
//
//    // ----- inner class: ProcessorStub -------------------------------------
//
//    /**
//     * A stub entry processor to verify serialization.
//     */
//    public static class ProcessorStub<K, V, R>
//            implements InvocableMap.EntryProcessor<K, V, R>, ExternalizableLite
//        {
//
//        // ----- EntryProcessor interface -----------------------------------
//
//        @Override
//        public R process(InvocableMap.Entry<K, V> entry)
//            {
//            return null;
//            }
//
//        // ----- ExternalizableLite interface -------------------------------
//
//        @Override
//        public void readExternal(DataInput in) throws IOException
//            {
//            m_sUid = ExternalizableHelper.readUTF(in);
//            }
//
//        @Override
//        public void writeExternal(DataOutput out) throws IOException
//            {
//            ExternalizableHelper.writeUTF(out, m_sUid);
//            }
//
//        // ----- Object methods ---------------------------------------------
//
//        @Override
//        public boolean equals(Object o)
//            {
//            if (this == o)
//                {
//                return true;
//                }
//            if (o == null || getClass() != o.getClass())
//                {
//                return false;
//                }
//            ProcessorStub<?, ?, ?> that = (ProcessorStub<?, ?, ?>) o;
//            return Objects.equals(m_sUid, that.m_sUid);
//            }
//
//        @Override
//        public int hashCode()
//            {
//            return Objects.hash(m_sUid);
//            }
//
//        // ----- data members -----------------------------------------------
//
//        protected String m_sUid = new UID().toString();
//        }
//
//    // ----- inner class TestBiConsumer -------------------------------------
//
//    /**
//     * A test {@link BiConsumer}.
//     *
//     * @param <K>  the type of the key consumed
//     * @param <R>  the type of the result consumed
//     */
//    protected static class TestBiConsumer<K, R>
//            implements BiConsumer<K, R>
//        {
//        // ----- BiConsumer interface ---------------------------------------
//
//        @Override
//        public void accept(K k, R r)
//            {
//            f_mapResults.put(k, r);
//            }
//
//        // ----- helper methods ---------------------------------------------
//
//        /**
//         * Obtain the consumed results.
//         *
//         * @return the consumed results
//         */
//        protected Map<K, R> getResults()
//            {
//            return f_mapResults;
//            }
//
//        // ----- data members -----------------------------------------------
//
//        protected final Map<K, R> f_mapResults = new LinkedHashMap<>();
//        }
//
//    // ----- inner class: TestConsumer --------------------------------------
//
//    /**
//     * A test {@link BiConsumer}.
//     *
//     * @param <K>  the type of the key consumed
//     * @param <R>  the type of the result consumed
//     */
//    protected static class TestConsumer<K, R>
//            implements Consumer<Map.Entry<? extends K, ? extends R>>
//        {
//        // ----- Consumer interface -----------------------------------------
//
//        @Override
//        public void accept(Map.Entry<? extends K, ? extends R> entry)
//            {
//            f_mapResults.put(entry.getKey(), entry.getValue());
//            }
//
//        // ----- helper methods ---------------------------------------------
//
//        /**
//         * Obtain the consumed results.
//         *
//         * @return the consumed results
//         */
//        protected Map<K, R> getResults()
//            {
//            return f_mapResults;
//            }
//
//        // ----- data members -----------------------------------------------
//
//        protected final Map<K, R> f_mapResults = new LinkedHashMap<>();
//        }
//
//    // ----- inner class BadBiConsumer --------------------------------------
//
//    /**
//     * A test {@link BiConsumer} that throws
//     * an exception from it's accept method.
//     *
//     * @param <K>  the type of the key consumed
//     * @param <R>  the type of the result consumed
//     */
//    private static class BadBiConsumer<K, R>
//            implements BiConsumer<K, R>
//        {
//        // ----- BiConsumer interface ---------------------------------------
//
//        @Override
//        public void accept(K k, R r)
//            {
//            throw f_error;
//            }
//
//        // ----- helper methods ---------------------------------------------
//
//        /**
//         * Obtain the error thrown.
//         *
//         * @return the error thrown by the accept method
//         */
//        protected RuntimeException getError()
//            {
//            return f_error;
//            }
//
//        // ----- data members -----------------------------------------------
//
//        protected final RuntimeException f_error = new RuntimeException("Computer says No!");
//        }
//
//    // ----- inner class: BadConsumer ---------------------------------------
//
//    /**
//     * A test {@link Consumer} that throws
//     * an exception from it's accept method.
//     *
//     * @param <K>  the type of the key consumed
//     * @param <R>  the type of the result consumed
//     */
//    private static class BadConsumer<K, R>
//            implements Consumer<Map.Entry<? extends K, ? extends R>>
//        {
//        // ----- Consumer interface -----------------------------------------
//
//        @Override
//        public void accept(Map.Entry<? extends K, ? extends R> entry)
//            {
//            throw f_error;
//            }
//
//        // ----- helper methods ---------------------------------------------
//
//        /**
//         * Obtain the error thrown.
//         *
//         * @return the error thrown by the accept method
//         */
//        protected RuntimeException getError()
//            {
//            return f_error;
//            }
//
//        // ----- data members -----------------------------------------------
//
//        protected final RuntimeException f_error = new RuntimeException("Computer says No!");
//        }
//
//    // ----- constants ------------------------------------------------------
//
//    private static final Serializer SERIALIZER = new DefaultSerializer();
//
//    // ----- data members ---------------------------------------------------
//
//    private static final String FORMAT = "java";
    }
