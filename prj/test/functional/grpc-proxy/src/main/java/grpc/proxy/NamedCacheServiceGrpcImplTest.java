/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.oracle.coherence.grpc.AddIndexRequest;
import com.oracle.coherence.grpc.AggregateRequest;
import com.oracle.coherence.grpc.ClearRequest;
import com.oracle.coherence.grpc.ContainsEntryRequest;
import com.oracle.coherence.grpc.ContainsKeyRequest;
import com.oracle.coherence.grpc.ContainsValueRequest;
import com.oracle.coherence.grpc.DestroyRequest;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.EntryResult;
import com.oracle.coherence.grpc.EntrySetRequest;
import com.oracle.coherence.grpc.GetAllRequest;
import com.oracle.coherence.grpc.GetRequest;
import com.oracle.coherence.grpc.InvokeAllRequest;
import com.oracle.coherence.grpc.InvokeRequest;
import com.oracle.coherence.grpc.IsEmptyRequest;
import com.oracle.coherence.grpc.KeySetRequest;
import com.oracle.coherence.grpc.MapListenerRequest;
import com.oracle.coherence.grpc.MapListenerResponse;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.PageRequest;
import com.oracle.coherence.grpc.PutAllRequest;
import com.oracle.coherence.grpc.PutIfAbsentRequest;
import com.oracle.coherence.grpc.PutRequest;
import com.oracle.coherence.grpc.RemoveIndexRequest;
import com.oracle.coherence.grpc.RemoveMappingRequest;
import com.oracle.coherence.grpc.RemoveRequest;
import com.oracle.coherence.grpc.ReplaceMappingRequest;
import com.oracle.coherence.grpc.ReplaceRequest;
import com.oracle.coherence.grpc.Requests;
import com.oracle.coherence.grpc.SafeStreamObserver;
import com.oracle.coherence.grpc.SizeRequest;
import com.oracle.coherence.grpc.TruncateRequest;
import com.oracle.coherence.grpc.ValuesRequest;

import com.oracle.coherence.grpc.proxy.NamedCacheService;
import com.oracle.coherence.grpc.proxy.NamedCacheServiceGrpcImpl;
import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.Test;

import org.mockito.invocation.InvocationOnMock;

import java.util.Collections;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.09.23
 */
@SuppressWarnings("unchecked")
public class NamedCacheServiceGrpcImplTest
    {
    @Test
    public void shouldCallAddIndex() throws Exception
        {
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service = mock(NamedCacheService.class);

        when(service.addIndex(any(AddIndexRequest.class))).thenReturn(EMPTY_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        AddIndexRequest           request = Requests.addIndex(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.addIndex(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).addIndex(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallAggregate() throws Exception
        {
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service = mock(NamedCacheService.class);

        when(service.aggregate(any(AggregateRequest.class))).thenReturn(BYTES_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        AggregateRequest          request = Requests.aggregate(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).aggregate(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallClear() throws Exception
        {
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service = mock(NamedCacheService.class);

        when(service.clear(any(ClearRequest.class))).thenReturn(EMPTY_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ClearRequest              request = Requests.clear(SCOPE, CACHE_NAME);

        grpc.clear(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).clear(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallContainsEntry() throws Exception
        {
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService             service  = mock(NamedCacheService.class);

        when(service.containsEntry(any(ContainsEntryRequest.class))).thenReturn(BOOL_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ContainsEntryRequest      request = Requests.containsEntry(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.containsEntry(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).containsEntry(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallContainsKey()  throws Exception
        {
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService             service = mock(NamedCacheService.class);

        when(service.containsKey(any(ContainsKeyRequest.class))).thenReturn(BOOL_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ContainsKeyRequest        request = Requests.containsKey(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.containsKey(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).containsKey(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallContainsValue()  throws Exception
        {
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService             service  = mock(NamedCacheService.class);

        when(service.containsValue(any(ContainsValueRequest.class))).thenReturn(BOOL_STAGE);

        NamedCacheServiceGrpcImpl      grpc    = new NamedCacheServiceGrpcImpl(service);
        ContainsValueRequest       request = Requests.containsValue(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.containsValue(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).containsValue(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallDestroy()  throws Exception
        {
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service = mock(NamedCacheService.class);

        when(service.destroy(any(DestroyRequest.class))).thenReturn(EMPTY_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        DestroyRequest            request = Requests.destroy(SCOPE, CACHE_NAME);

        grpc.destroy(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).destroy(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallEntrySet()  throws Exception
        {
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheService         service = mock(NamedCacheService.class);
        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        EntrySetRequest           request = Requests.entrySet(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).entrySet(any(EntrySetRequest.class), any(StreamObserver.class));

        grpc.entrySet(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors();

        verify(service).entrySet(same(request), isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallEvents()
        {
        StreamObserver<MapListenerRequest> requestObserver = mock(StreamObserver.class);
        NamedCacheService                  service         = mock(NamedCacheService.class);

        when(service.events(any(StreamObserver.class))).thenReturn(requestObserver);

        StreamObserver<MapListenerResponse> observer = mock(StreamObserver.class);
        NamedCacheServiceGrpcImpl           grpc    = new NamedCacheServiceGrpcImpl(service);
        StreamObserver<MapListenerRequest>  result  = grpc.events(observer);

        assertThat(result, is(sameInstance(requestObserver)));
        verify(service).events(isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallGet()  throws Exception
        {
        TestStreamObserver<OptionalValue> observer = new TestStreamObserver<>();
        NamedCacheService                 service  = mock(NamedCacheService.class);

        when(service.get(any(GetRequest.class))).thenReturn(OPT_STAGE);

        NamedCacheServiceGrpcImpl grpc     = new NamedCacheServiceGrpcImpl(service);
        GetRequest                request  = Requests.get(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.get(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).get(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallGetAll()  throws Exception
        {
        NamedCacheService         service  = mock(NamedCacheService.class);
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        GetAllRequest             request = Requests.getAll(SCOPE, CACHE_NAME, FORMAT, Collections.singleton(BYTES));

        doAnswer(this::completeObserver).when(service).getAll(any(GetAllRequest.class), any(StreamObserver.class));

        grpc.getAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors();

        verify(service).getAll(same(request), isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallInvoke()  throws Exception
        {
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = mock(NamedCacheService.class);

        when(service.invoke(any(InvokeRequest.class))).thenReturn(BYTES_STAGE);

        NamedCacheServiceGrpcImpl grpc     = new NamedCacheServiceGrpcImpl(service);
        InvokeRequest             request  = Requests.invoke(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.invoke(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).invoke(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallInvokeAll()
        {
        NamedCacheService         service  = mock(NamedCacheService.class);
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        NamedCacheServiceGrpcImpl grpc     = new NamedCacheServiceGrpcImpl(service);
        InvokeAllRequest          request = Requests.invokeAll(SCOPE, CACHE_NAME, FORMAT, Collections.emptyList(), BYTES);

        doAnswer(this::completeObserver).when(service).invokeAll(any(InvokeAllRequest.class), any(StreamObserver.class));

        grpc.invokeAll(request, observer);

        verify(service).invokeAll(same(request), isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallIsEmpty()  throws Exception
        {
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService             service  = mock(NamedCacheService.class);

        when(service.isEmpty(any(IsEmptyRequest.class))).thenReturn(BOOL_STAGE);

        NamedCacheServiceGrpcImpl      grpc     = new NamedCacheServiceGrpcImpl(service);
        IsEmptyRequest             request  = Requests.isEmpty(SCOPE, CACHE_NAME);

        grpc.isEmpty(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).isEmpty(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallKeySet() throws Exception
        {
        NamedCacheService              service  = mock(NamedCacheService.class);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheServiceGrpcImpl      grpc     = new NamedCacheServiceGrpcImpl(service);
        KeySetRequest                  request  = Requests.keySet(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).keySet(any(KeySetRequest.class), any(StreamObserver.class));

        grpc.keySet(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors();

        verify(service).keySet(same(request), isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallNextEntrySetPage() throws Exception
        {
        NamedCacheService               service  = mock(NamedCacheService.class);
        TestStreamObserver<EntryResult> observer = new TestStreamObserver<>();
        NamedCacheServiceGrpcImpl       grpc    = new NamedCacheServiceGrpcImpl(service);
        PageRequest                     request = Requests.page(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).nextEntrySetPage(any(PageRequest.class), any(StreamObserver.class));

        grpc.nextEntrySetPage(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors();

        verify(service).nextEntrySetPage(same(request), isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallNextKeySetPage() throws Exception
        {
        NamedCacheService              service  = mock(NamedCacheService.class);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheServiceGrpcImpl      grpc    = new NamedCacheServiceGrpcImpl(service);
        PageRequest                    request = Requests.page(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).nextKeySetPage(any(PageRequest.class), any(StreamObserver.class));

        grpc.nextKeySetPage(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors();

        verify(service).nextKeySetPage(same(request), isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallPut()  throws Exception
        {
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = mock(NamedCacheService.class);

        when(service.put(any(PutRequest.class))).thenReturn(BYTES_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        PutRequest                request = Requests.put(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.put(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).put(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallPutAll()  throws Exception
        {
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service  = mock(NamedCacheService.class);

        when(service.putAll(any(PutAllRequest.class))).thenReturn(EMPTY_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        PutAllRequest             request = Requests.putAll(SCOPE, CACHE_NAME, FORMAT, Collections.emptyList());

        grpc.putAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).putAll(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallPutIfAbsent()  throws Exception
        {
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = mock(NamedCacheService.class);

        when(service.putIfAbsent(any(PutIfAbsentRequest.class))).thenReturn(BYTES_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        PutIfAbsentRequest        request = Requests.putIfAbsent(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.putIfAbsent(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).putIfAbsent(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallRemove()  throws Exception
        {
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = mock(NamedCacheService.class);

        when(service.remove(any(RemoveRequest.class))).thenReturn(BYTES_STAGE);

        NamedCacheServiceGrpcImpl grpc     = new NamedCacheServiceGrpcImpl(service);
        RemoveRequest             request  = Requests.remove(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.remove(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).remove(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallRemoveIndex()  throws Exception
        {
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service  = mock(NamedCacheService.class);

        when(service.removeIndex(any(RemoveIndexRequest.class))).thenReturn(EMPTY_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        RemoveIndexRequest        request = Requests.removeIndex(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.removeIndex(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).removeIndex(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallRemoveMapping()  throws Exception
        {
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService             service  = mock(NamedCacheService.class);

        when(service.removeMapping(any(RemoveMappingRequest.class))).thenReturn(BOOL_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        RemoveMappingRequest      request = Requests.remove(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.removeMapping(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).removeMapping(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallReplace()  throws Exception
        {
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheService              service  = mock(NamedCacheService.class);

        when(service.replace(any(ReplaceRequest.class))).thenReturn(BYTES_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ReplaceRequest            request = Requests.replace(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.replace(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).replace(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallReplaceMapping()  throws Exception
        {
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        NamedCacheService             service  = mock(NamedCacheService.class);

        when(service.replaceMapping(any(ReplaceMappingRequest.class))).thenReturn(BOOL_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ReplaceMappingRequest     request = Requests.replace(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES, BYTES);

        grpc.replaceMapping(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).replaceMapping(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallSize()  throws Exception
        {
        TestStreamObserver<Int32Value> observer = new TestStreamObserver<>();
        NamedCacheService              service  = mock(NamedCacheService.class);

        when(service.size(any(SizeRequest.class))).thenReturn(INT_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        SizeRequest               request = Requests.size(SCOPE, CACHE_NAME);

        grpc.size(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).size(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallTruncate()  throws Exception
        {
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        NamedCacheService         service  = mock(NamedCacheService.class);

        when(service.truncate(any(TruncateRequest.class))).thenReturn(EMPTY_STAGE);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        TruncateRequest           request = Requests.truncate(SCOPE, CACHE_NAME);

        grpc.truncate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        verify(service).truncate(same(request));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallValues() throws Exception
        {
        NamedCacheService              service  = mock(NamedCacheService.class);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        NamedCacheServiceGrpcImpl      grpc    = new NamedCacheServiceGrpcImpl(service);
        ValuesRequest                  request = Requests.values(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).values(any(ValuesRequest.class), any(StreamObserver.class));

        grpc.values(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors();

        verify(service).values(same(request), isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    // ----- helper methods -------------------------------------------------

    public Object completeObserver(InvocationOnMock invocation)
        {
        invocation.getArgument(1, StreamObserver.class).onCompleted();
        return null;
        }

    // ----- data members ---------------------------------------------------

    public static final String SCOPE = "foo";

    public static final String CACHE_NAME = "test";

    public static final String FORMAT = "java";

    public static final ByteString BYTES = ByteString.copyFrom("bar".getBytes());

    public static final CompletionStage<Empty> EMPTY_STAGE = CompletableFuture.completedFuture(Empty.getDefaultInstance());
    public static final CompletionStage<BytesValue> BYTES_STAGE = CompletableFuture.completedFuture(BytesValue.getDefaultInstance());
    public static final CompletionStage<BoolValue> BOOL_STAGE = CompletableFuture.completedFuture(BoolValue.getDefaultInstance());
    public static final CompletionStage<OptionalValue> OPT_STAGE = CompletableFuture.completedFuture(OptionalValue.getDefaultInstance());
    public static final CompletionStage<Int32Value> INT_STAGE = CompletableFuture.completedFuture(Int32Value.getDefaultInstance());
    }
