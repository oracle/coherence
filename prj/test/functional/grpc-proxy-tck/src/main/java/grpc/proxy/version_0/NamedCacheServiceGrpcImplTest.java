/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.grpc.v0.Requests;
import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.messages.cache.v0.AddIndexRequest;
import com.oracle.coherence.grpc.messages.cache.v0.AggregateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ClearRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsEntryRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsKeyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsValueRequest;
import com.oracle.coherence.grpc.messages.cache.v0.DestroyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.oracle.coherence.grpc.messages.cache.v0.EntryResult;
import com.oracle.coherence.grpc.messages.cache.v0.EntrySetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.GetAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.GetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeRequest;
import com.oracle.coherence.grpc.messages.cache.v0.IsEmptyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.KeySetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerResponse;
import com.oracle.coherence.grpc.messages.cache.v0.OptionalValue;
import com.oracle.coherence.grpc.messages.cache.v0.PageRequest;
import com.oracle.coherence.grpc.messages.cache.v0.PutAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.PutIfAbsentRequest;
import com.oracle.coherence.grpc.messages.cache.v0.PutRequest;
import com.oracle.coherence.grpc.messages.cache.v0.RemoveIndexRequest;
import com.oracle.coherence.grpc.messages.cache.v0.RemoveMappingRequest;
import com.oracle.coherence.grpc.messages.cache.v0.RemoveRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ReplaceMappingRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ReplaceRequest;
import com.oracle.coherence.grpc.messages.cache.v0.SizeRequest;
import com.oracle.coherence.grpc.messages.cache.v0.TruncateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ValuesRequest;

import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheServiceGrpcImpl;

import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
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
    public void shouldCallAddIndex()
        {
        StreamObserver<Empty> observer = mock(StreamObserver.class);
        NamedCacheService     service = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        AddIndexRequest           request = Requests.addIndex(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.addIndex(request, observer);

        ArgumentCaptor<StreamObserver<Empty>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).addIndex(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Empty> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallAggregate()
        {
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheService          service = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        AggregateRequest          request = Requests.aggregate(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.aggregate(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).aggregate(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallClear()
        {
        StreamObserver<Empty> observer = mock(StreamObserver.class);
        NamedCacheService     service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ClearRequest              request = Requests.clear(SCOPE, CACHE_NAME);

        grpc.clear(request, observer);

        ArgumentCaptor<StreamObserver<Empty>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).clear(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Empty> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallContainsEntry()
        {
        StreamObserver<BoolValue> observer = mock(StreamObserver.class);
        NamedCacheService             service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ContainsEntryRequest      request = Requests.containsEntry(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.containsEntry(request, observer);

        ArgumentCaptor<StreamObserver<BoolValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).containsEntry(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BoolValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallContainsKey() 
        {
        StreamObserver<BoolValue> observer = mock(StreamObserver.class);
        NamedCacheService             service = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ContainsKeyRequest        request = Requests.containsKey(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.containsKey(request, observer);

        ArgumentCaptor<StreamObserver<BoolValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).containsKey(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BoolValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallContainsValue() 
        {
        StreamObserver<BoolValue> observer = mock(StreamObserver.class);
        NamedCacheService             service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl      grpc    = new NamedCacheServiceGrpcImpl(service);
        ContainsValueRequest       request = Requests.containsValue(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.containsValue(request, observer);

        ArgumentCaptor<StreamObserver<BoolValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).containsValue(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BoolValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallDestroy() 
        {
        StreamObserver<Empty> observer = mock(StreamObserver.class);
        NamedCacheService         service = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        DestroyRequest            request = Requests.destroy(SCOPE, CACHE_NAME);

        grpc.destroy(request, observer);

        ArgumentCaptor<StreamObserver<Empty>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).destroy(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Empty> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallEntrySet() 
        {
        StreamObserver<Entry> observer = mock(StreamObserver.class);
        NamedCacheService         service = mock(NamedCacheService.class);
        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        EntrySetRequest           request = Requests.entrySet(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).entrySet(any(EntrySetRequest.class), any(StreamObserver.class));

        grpc.entrySet(request, observer);

        ArgumentCaptor<StreamObserver<Entry>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).entrySet(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Entry> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
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
    public void shouldCallGet() 
        {
        StreamObserver<OptionalValue> observer = mock(StreamObserver.class);
        NamedCacheService                 service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc     = new NamedCacheServiceGrpcImpl(service);
        GetRequest                request  = Requests.get(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.get(request, observer);

        ArgumentCaptor<StreamObserver<OptionalValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).get(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<OptionalValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallGetAll() 
        {
        NamedCacheService         service  = mock(NamedCacheService.class);
        StreamObserver<Entry> observer = mock(StreamObserver.class);
        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        GetAllRequest             request = Requests.getAll(SCOPE, CACHE_NAME, FORMAT, Collections.singleton(BYTES));

        doAnswer(this::completeObserver).when(service).getAll(any(GetAllRequest.class), any(StreamObserver.class));

        grpc.getAll(request, observer);

        ArgumentCaptor<StreamObserver<Entry>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).getAll(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Entry> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallInvoke() 
        {
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheService              service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc     = new NamedCacheServiceGrpcImpl(service);
        InvokeRequest             request  = Requests.invoke(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.invoke(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).invoke(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallInvokeAll()
        {
        NamedCacheService         service  = mock(NamedCacheService.class);
        StreamObserver<Entry> observer = mock(StreamObserver.class);
        NamedCacheServiceGrpcImpl grpc     = new NamedCacheServiceGrpcImpl(service);
        InvokeAllRequest          request = Requests.invokeAll(SCOPE, CACHE_NAME, FORMAT, Collections.emptyList(), BYTES);

        doAnswer(this::completeObserver).when(service).invokeAll(any(InvokeAllRequest.class), any(StreamObserver.class));

        grpc.invokeAll(request, observer);

        verify(service).invokeAll(same(request), isA(SafeStreamObserver.class));
        verifyNoMoreInteractions(service);
        }

    @Test
    public void shouldCallIsEmpty() 
        {
        StreamObserver<BoolValue> observer = mock(StreamObserver.class);
        NamedCacheService             service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl      grpc     = new NamedCacheServiceGrpcImpl(service);
        IsEmptyRequest             request  = Requests.isEmpty(SCOPE, CACHE_NAME);

        grpc.isEmpty(request, observer);

        ArgumentCaptor<StreamObserver<BoolValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).isEmpty(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BoolValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallKeySet()
        {
        NamedCacheService              service  = mock(NamedCacheService.class);
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheServiceGrpcImpl      grpc     = new NamedCacheServiceGrpcImpl(service);
        KeySetRequest                  request  = Requests.keySet(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).keySet(any(KeySetRequest.class), any(StreamObserver.class));

        grpc.keySet(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).keySet(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallNextEntrySetPage()
        {
        NamedCacheService               service  = mock(NamedCacheService.class);
        StreamObserver<EntryResult> observer = mock(StreamObserver.class);
        NamedCacheServiceGrpcImpl       grpc    = new NamedCacheServiceGrpcImpl(service);
        PageRequest                     request = Requests.page(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).nextEntrySetPage(any(PageRequest.class), any(StreamObserver.class));

        grpc.nextEntrySetPage(request, observer);

        ArgumentCaptor<StreamObserver<EntryResult>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).nextEntrySetPage(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<EntryResult> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallNextKeySetPage()
        {
        NamedCacheService              service  = mock(NamedCacheService.class);
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheServiceGrpcImpl      grpc    = new NamedCacheServiceGrpcImpl(service);
        PageRequest                    request = Requests.page(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).nextKeySetPage(any(PageRequest.class), any(StreamObserver.class));

        grpc.nextKeySetPage(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).nextKeySetPage(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallPut() 
        {
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheService              service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        PutRequest                request = Requests.put(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.put(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).put(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallPutAll() 
        {
        StreamObserver<Empty> observer = mock(StreamObserver.class);
        NamedCacheService         service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        PutAllRequest             request = Requests.putAll(SCOPE, CACHE_NAME, FORMAT, Collections.emptyList());

        grpc.putAll(request, observer);

        ArgumentCaptor<StreamObserver<Empty>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).putAll(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Empty> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallPutIfAbsent() 
        {
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheService              service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        PutIfAbsentRequest        request = Requests.putIfAbsent(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.putIfAbsent(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).putIfAbsent(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallRemove() 
        {
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheService              service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc     = new NamedCacheServiceGrpcImpl(service);
        RemoveRequest             request  = Requests.remove(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.remove(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).remove(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallRemoveIndex() 
        {
        StreamObserver<Empty> observer = mock(StreamObserver.class);
        NamedCacheService         service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        RemoveIndexRequest        request = Requests.removeIndex(SCOPE, CACHE_NAME, FORMAT, BYTES);

        grpc.removeIndex(request, observer);

        ArgumentCaptor<StreamObserver<Empty>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).removeIndex(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Empty> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallRemoveMapping() 
        {
        StreamObserver<BoolValue> observer = mock(StreamObserver.class);
        NamedCacheService             service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        RemoveMappingRequest      request = Requests.remove(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.removeMapping(request, observer);

        ArgumentCaptor<StreamObserver<BoolValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).removeMapping(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BoolValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallReplace() 
        {
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheService              service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ReplaceRequest            request = Requests.replace(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES);

        grpc.replace(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).replace(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallReplaceMapping() 
        {
        StreamObserver<BoolValue> observer = mock(StreamObserver.class);
        NamedCacheService             service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        ReplaceMappingRequest     request = Requests.replace(SCOPE, CACHE_NAME, FORMAT, BYTES, BYTES, BYTES);

        grpc.replaceMapping(request, observer);

        ArgumentCaptor<StreamObserver<BoolValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).replaceMapping(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BoolValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallSize() 
        {
        StreamObserver<Int32Value> observer = mock(StreamObserver.class);
        NamedCacheService              service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        SizeRequest               request = Requests.size(SCOPE, CACHE_NAME);

        grpc.size(request, observer);

        ArgumentCaptor<StreamObserver<Int32Value>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).size(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Int32Value> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallTruncate() 
        {
        StreamObserver<Empty> observer = mock(StreamObserver.class);
        NamedCacheService         service  = mock(NamedCacheService.class);

        NamedCacheServiceGrpcImpl grpc    = new NamedCacheServiceGrpcImpl(service);
        TruncateRequest           request = Requests.truncate(SCOPE, CACHE_NAME);

        grpc.truncate(request, observer);

        ArgumentCaptor<StreamObserver<Empty>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).truncate(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<Empty> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
        }

    @Test
    public void shouldCallValues()
        {
        NamedCacheService              service  = mock(NamedCacheService.class);
        StreamObserver<BytesValue> observer = mock(StreamObserver.class);
        NamedCacheServiceGrpcImpl      grpc    = new NamedCacheServiceGrpcImpl(service);
        ValuesRequest                  request = Requests.values(SCOPE, CACHE_NAME, FORMAT, BYTES);

        doAnswer(this::completeObserver).when(service).values(any(ValuesRequest.class), any(StreamObserver.class));

        grpc.values(request, observer);

        ArgumentCaptor<StreamObserver<BytesValue>> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(service).values(same(request), captor.capture());
        verifyNoMoreInteractions(service);
        StreamObserver<BytesValue> actual = captor.getValue();
        assertThat(actual, is(instanceOf(SafeStreamObserver.class)));
        assertThat(((SafeStreamObserver<?>) actual).delegate(), is(sameInstance(observer)));
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
    }
