/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.v0;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.grpc.messages.cache.v0.AddIndexRequest;
import com.oracle.coherence.grpc.messages.cache.v0.AggregateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ClearRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsEntryRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsKeyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsValueRequest;
import com.oracle.coherence.grpc.messages.cache.v0.DestroyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.oracle.coherence.grpc.messages.cache.v0.EntryResult;
import com.oracle.coherence.grpc.messages.cache.v0.GetAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.GetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeRequest;
import com.oracle.coherence.grpc.messages.cache.v0.IsEmptyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.IsReadyRequest;
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


import com.oracle.coherence.grpc.client.common.AsyncNamedCacheClient;
import com.oracle.coherence.grpc.client.common.SingleValueStreamObserver;
import com.oracle.coherence.grpc.client.common.StreamStreamObserver;

import com.oracle.coherence.grpc.services.cache.v0.NamedCacheServiceGrpc;

import com.tangosol.net.PriorityTask;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * A gRPC NamedCache service.
 *
 * @author Jonathan Knight  2020.09.21
 * @since 20.06
 */
public class NamedCacheGrpcClient
    {
    // ----- constructors ---------------------------------------------------

    public NamedCacheGrpcClient(AsyncNamedCacheClient.Dependencies dependencies)
        {
        f_dependencies = dependencies;
        }

    // ----- NamedCacheService methods --------------------------------------

    /**
     * Clear a cache.
     *
     * @param request  the {{@link ClearRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete when the cache
     * has been cleared
     *
     * @see com.tangosol.net.NamedCache#clear()
     */
    CompletionStage<Empty> clear(ClearRequest request)
        {
        SingleValueStreamObserver<Empty> observer = new SingleValueStreamObserver<>();
        createStub().clear(request, observer);
        return observer.completionStage();
        }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key to the specified value.
     *
     * @param request  the request which contains the key and value whose presence
     *                 in this map is to be tested
     *
     * @return a {@link CompletionStage} that will complete with {code true} if this map contains
     *         a mapping for the specified key to the specified value
     *
     * @see com.tangosol.net.NamedCache#containsKey(Object)
     */
    CompletionStage<BoolValue> containsEntry(ContainsEntryRequest request)
        {
        SingleValueStreamObserver<BoolValue> observer = new SingleValueStreamObserver<>();
        createStub().containsEntry(request, observer);
        return observer.completionStage();
        }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @param request  the request which contains the key whose presence
     *                 in this map is to be tested
     *
     * @return a {@link CompletionStage} that will complete with {code true}
     *         if this map contains a mapping for the specified key
     *
     * @see com.tangosol.net.NamedCache#containsKey(Object)
     */
    CompletionStage<BoolValue> containsKey(ContainsKeyRequest request)
        {
        SingleValueStreamObserver<BoolValue> observer = new SingleValueStreamObserver<>();
        createStub().containsKey(request, observer);
        return observer.completionStage();
        }

    /**
     * Returns {@code true} if this map contains a mapping for the specified value.
     *
     * @param request  the request which contains the value whose presence
     *                 in this map is to be tested
     *
     * @return a {@link CompletionStage} that will complete with {code true}
     *         if this map contains a mapping for the specified value
     *
     * @see com.tangosol.net.NamedCache#containsKey(Object)
     */
    CompletionStage<BoolValue> containsValue(ContainsValueRequest request)
        {
        SingleValueStreamObserver<BoolValue> observer = new SingleValueStreamObserver<>();
        createStub().containsValue(request, observer);
        return observer.completionStage();
        }

    /**
     * Determine whether a cache is empty.
     *
     * @param request  the {@link IsEmptyRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the result
     *         of the is empty request
     *
     * @see com.tangosol.net.NamedCache#isEmpty()
     */
    CompletionStage<BoolValue> isEmpty(IsEmptyRequest request)
        {
        SingleValueStreamObserver<BoolValue> observer = new SingleValueStreamObserver<>();
        createStub().isEmpty(request, observer);
        return observer.completionStage();
        }

    /**
     * Determine the cache's ready status.
     *
     * @param request  the {@link IsReadyRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the result
     *         of the ready request.
     *
     * @see com.tangosol.net.NamedCache#size()
     * @since 14.1.1.2206.5
     */
    CompletionStage<BoolValue> isReady(IsReadyRequest request)
        {
        SingleValueStreamObserver<BoolValue> observer = new SingleValueStreamObserver<>();
        createStub().isReady(request, observer);
        return observer.completionStage();
        }

    /**
     * Determine the number of entries in a cache.
     *
     * @param request  the {@link SizeRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the result
     *         of the size request
     *
     * @see com.tangosol.net.NamedCache#size()
     */
    CompletionStage<Int32Value> size(SizeRequest request)
        {
        SingleValueStreamObserver<Int32Value> observer = new SingleValueStreamObserver<>();
        createStub().size(request, observer);
        return observer.completionStage();
        }

    /**
     * Get a value for a given key from a cache.
     *
     * @param request  the {@link GetRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with an
     *         {@link OptionalValue} containing the result of the get request
     *
     * @see com.tangosol.net.NamedCache#get(Object)
     */
    CompletionStage<OptionalValue> get(GetRequest request)
        {
        SingleValueStreamObserver<OptionalValue> observer = new SingleValueStreamObserver<>();
        createStub().get(request, observer);
        return observer.completionStage();
        }

    /**
     * Obtain a stream of mappings of keys to values for all the specified keys.
     *
     * @param request  the {@link GetAllRequest} request containing the cache name
     *                 and collection of keys to obtain the mappings for
     *
     * @return the {@link Stream} of {@link Entry} instances
     */
    Stream<Entry> getAll(GetAllRequest request)
        {
        try
            {
            StreamStreamObserver<Entry> observer = new StreamStreamObserver<>();
            createStub().getAll(request, observer);
            return observer.future().get().stream();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Associate the specified value with the specified key in this cache.
     * If the cache previously contained a mapping for the key, the old value
     * is replaced by the specified value.
     *
     * @param request  the {@link PutRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the result of
     *         the put request
     *
     * @see com.tangosol.net.NamedCache#put(Object, Object)
     */
    CompletionStage<BytesValue> put(PutRequest request)
        {
        SingleValueStreamObserver<BytesValue> observer = new SingleValueStreamObserver<>();
        createStub().put(request, observer);
        return observer.completionStage();
        }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associate it with the given value and returns
     * {@code null}, else return the current value.
     *
     * @param request  the {@link PutIfAbsentRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with
     *         the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     *
     * @see com.tangosol.net.NamedCache#putIfAbsent(Object, Object)
     */
    CompletionStage<BytesValue> putIfAbsent(PutIfAbsentRequest request)
        {
        SingleValueStreamObserver<BytesValue> observer = new SingleValueStreamObserver<>();
        createStub().putIfAbsent(request, observer);
        return observer.completionStage();
        }

    /**
     * Add the specified key value pair mappings to this cache.
     * If the cache previously contained a mappings for the keys, the old value
     * is replaced by the specified value.
     *
     * @param request  the {@link PutAllRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the result of
     *         the {@link com.tangosol.net.NamedCache#putAll(java.util.Map)}
     *
     * @see com.tangosol.net.NamedCache#putAll(java.util.Map)
     */
    CompletionStage<Empty> putAll(PutAllRequest request)
        {
        SingleValueStreamObserver<Empty> observer = new SingleValueStreamObserver<>();
        createStub().putAll(request, observer);
        return observer.completionStage();
        }

    /**
     * Remove the mapping that is associated with the specified key.
     *
     * @param request  the {@link RemoveRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the
     *         previous value associated with specified key, or null
     *         if there was no mapping for key
     *
     * @see com.tangosol.net.NamedCache#remove(Object)
     */
    CompletionStage<BytesValue> remove(RemoveRequest request)
        {
        SingleValueStreamObserver<BytesValue> observer = new SingleValueStreamObserver<>();
        createStub().remove(request, observer);
        return observer.completionStage();
        }

    /**
     * Remove the mapping that is associated with the specified key only
     * if the mapping exists in the cache.
     *
     * @param request  the {@link RemoveMappingRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with {@code true}
     *         if the remove was successful, {@code false} otherwise
     *
     * @see com.tangosol.net.NamedCache#remove(Object, Object)
     */
    CompletionStage<BoolValue> removeMapping(RemoveMappingRequest request)
        {
        SingleValueStreamObserver<BoolValue> observer = new SingleValueStreamObserver<>();
        createStub().removeMapping(request, observer);
        return observer.completionStage();
        }

    /**
     * Replace the entry for the specified key only if it is currently
     * mapped to some value.
     *
     * @param request  the {@link ReplaceRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the
     *         previous value associated with specified key, or null
     *         if there was no mapping for key
     *
     * @see com.tangosol.net.NamedCache#replace(Object, Object)
     */
    CompletionStage<BytesValue> replace(ReplaceRequest request)
        {
        SingleValueStreamObserver<BytesValue> observer = new SingleValueStreamObserver<>();
        createStub().replace(request, observer);
        return observer.completionStage();
        }

    /**
     * Replace the mapping for the specified key only if currently mapped
     * to the specified value.
     *
     * @param request the {@link ReplaceMappingRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with
     *         the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     *
     * @see com.tangosol.net.NamedCache#replace(Object, Object, Object)
     */
    CompletionStage<BoolValue> replaceMapping(ReplaceMappingRequest request)
        {
        SingleValueStreamObserver<BoolValue> observer = new SingleValueStreamObserver<>();
        createStub().replaceMapping(request, observer);
        return observer.completionStage();
        }

    /**
     * Obtain the next page of a paged key set request.
     * <p>
     * The first element in the returned {@link Stream} will be the opaque
     * cookie to be used in subsequent page requests. The rest of the elements
     * in the stream will be the keys.
     *
     * @param request  the {@link PageRequest} to execute
     *
     * @return a {@link Stream} of {@link BytesValue} representing the keys
     */
    Stream<BytesValue> nextKeySetPage(PageRequest request)
        {
        try
            {
            StreamStreamObserver<BytesValue> observer = new StreamStreamObserver<>();
            createStub().nextKeySetPage(request, observer);
            return observer.future().get().stream();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Obtain the next page of a paged entry set request.
     * <p>
     * The first element in the returned {@link Stream} will be the opaque
     * cookie to be used in subsequent page requests. The rest of the
     * elements in the stream will be the entries.
     *
     * @param request  the {@link PageRequest} to execute
     *
     * @return a {@link Stream} of {@link EntryResult} instances
     */
    Stream<EntryResult> nextEntrySetPage(PageRequest request)
        {
        try
            {
            StreamStreamObserver<EntryResult> observer = new StreamStreamObserver<>();
            createStub().nextEntrySetPage(request, observer);
            return observer.future().get().stream();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Removes all mappings from this map.
     * <p>
     * Note: the removal of entries caused by this truncate operation will
     * not be observable. This includes any registered {@link com.tangosol.util.MapListener
     * listeners}, {@link com.tangosol.util.MapTrigger triggers}, or {@link
     * com.tangosol.net.events.EventInterceptor interceptors}. However, a
     * {@link com.tangosol.net.events.partition.cache.CacheLifecycleEvent CacheLifecycleEvent}
     * is raised to notify subscribers of the execution of this operation.
     *
     * @param request  the {@link TruncateRequest} containing the name of the cache to truncate
     *
     * @return a {@link CompletionStage} that completes when the truncate operation
     *         has completed
     */
    CompletionStage<Empty> truncate(TruncateRequest request)
        {
        SingleValueStreamObserver<Empty> observer = new SingleValueStreamObserver<>();
        createStub().truncate(request, observer);
        return observer.completionStage();
        }

    /**
     * Invoke an {@link com.tangosol.util.InvocableMap.EntryProcessor} against an entry in a cache.
     *
     * @param request    the {@link InvokeRequest} containing the serialized key of the entry and the
     *                   serialized {@link com.tangosol.util.InvocableMap.EntryProcessor}
     * @param nDeadline  the deadline to apply to the request
     *
     * @return the serialized result of the {@link com.tangosol.util.InvocableMap.EntryProcessor} invocation
     */
    CompletionStage<BytesValue> invoke(InvokeRequest request, long nDeadline)
        {
        SingleValueStreamObserver<BytesValue> observer = new SingleValueStreamObserver<>();
        createStubWithDeadline(nDeadline).invoke(request, observer);
        return observer.completionStage();
        }

    /**
     * Invoke an {@link com.tangosol.util.InvocableMap.EntryProcessor} against multiple entries in a cache.
     *
     * @param request    the {@link InvokeRequest} containing the serialized keys or serialized
     *                   {@link com.tangosol.util.Filter} to use to identify the entries and the
     *                   serialized {@link com.tangosol.util.InvocableMap.EntryProcessor}
     * @param observer   the {@link io.grpc.stub.StreamObserver} to observe the invocation results
     * @param nDeadline  the deadline to apply to the request
     */
    void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer, long nDeadline)
        {
        createStubWithDeadline(nDeadline).invokeAll(request, observer);
        }

    /**
     * Destroy a cache.
     *
     * @param request  the {@link DestroyRequest} containing the name of the cache to destroy
     *
     * @return a {@link CompletionStage} that will complete when the cache is destroyed
     */
    CompletionStage<Empty> destroy(DestroyRequest request)
        {
        SingleValueStreamObserver<Empty> observer = new SingleValueStreamObserver<>();
        createStub().destroy(request, observer);
        return observer.completionStage();
        }

    /**
     * Add an index to a cache.
     *
     * @param request  the {@link AddIndexRequest} containing the name of the cache to add the index
     *                 to, the serialized {@link com.tangosol.util.ValueExtractor} to use to create
     *                 the index and the optional serialized {@link java.util.Comparator} to sort the
     *                 index
     *
     * @return a {@link CompletionStage} that will complete when the index is created
     */
    CompletionStage<Empty> addIndex(AddIndexRequest request)
        {
        SingleValueStreamObserver<Empty> observer = new SingleValueStreamObserver<>();
        createStub().addIndex(request, observer);
        return observer.completionStage();
        }

    /**
     * Remove an index from a cache.
     *
     * @param request  the {@link RemoveIndexRequest} containing the name of the cache to remove the
     *                 index from, the serialized {@link com.tangosol.util.ValueExtractor} that was
     *                 used to create the index
     *
     * @return a {@link CompletionStage} that will complete when the index is removed
     */
    CompletionStage<Empty> removeIndex(RemoveIndexRequest request)
        {
        SingleValueStreamObserver<Empty> observer = new SingleValueStreamObserver<>();
        createStub().removeIndex(request, observer);
        return observer.completionStage();
        }

    /**
     * Execute an {@link AggregateRequest} against a cache and return the result
     * serialized in a {@link BytesValue}.
     * <p/>
     * A deadline value of {@link PriorityTask#TIMEOUT_DEFAULT TIMEOUT_DEFAULT} indicates
     * a default timeout value configured for the corresponding service; the value of
     * {@link PriorityTask#TIMEOUT_NONE TIMEOUT_NONE} indicates that the client thread
     * is willing to wait indefinitely until the task execution completes or is
     *
     * @param request    the {@link AggregateRequest} to execute
     * @param nDeadline  the deadline the client should apply to the request
     *
     * @return the serialized aggregation result
     */
    CompletionStage<BytesValue> aggregate(AggregateRequest request, long nDeadline)
        {
        SingleValueStreamObserver<BytesValue> observer = new SingleValueStreamObserver<>();
        createStubWithDeadline(nDeadline).aggregate(request, observer);
        return observer.completionStage();
        }

    /**
     * Stream a set of cache values to a {@link StreamObserver}.
     *
     * @param request  the {@link ValuesRequest} to execute
     *
     * @return a {@link Stream} of {@link BytesValue} instances
     *
     * @see com.tangosol.net.NamedCache#values(com.tangosol.util.Filter)
     * @see com.tangosol.net.NamedCache#values(com.tangosol.util.Filter, java.util.Comparator)
     */
    Stream<BytesValue> values(ValuesRequest request)
        {
        try
            {
            StreamStreamObserver<BytesValue> observer = new StreamStreamObserver<>();
            createStub().values(request, observer);
            return observer.future().get().stream();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Add a {@link com.tangosol.util.MapListener} to a cache and stream
     * the events received to the {@link io.grpc.stub.StreamObserver}.
     *
     * @param observer  the {@link io.grpc.stub.StreamObserver} to receive events
     *
     * @return a {@link io.grpc.stub.StreamObserver} that will be closed by the
     * client to end event subscription
     */
    StreamObserver<MapListenerRequest> events(StreamObserver<MapListenerResponse> observer)
        {
        return createStubWithoutDeadline().events(observer);
        }

    /**
     * Create the default {@link NamedCacheServiceGrpc.NamedCacheServiceStub}
     * to use for requests.
     *
     * @return the default {@link NamedCacheServiceGrpc.NamedCacheServiceStub}
     */
    private NamedCacheServiceGrpc.NamedCacheServiceStub createStub()
        {
        long nDeadline = f_dependencies.getDeadline();
        return createStubWithoutDeadline()
                .withDeadlineAfter(nDeadline, TimeUnit.MILLISECONDS);
        }

    /**
     * Create the default {@link NamedCacheServiceGrpc.NamedCacheServiceStub}
     * to use for requests.
     * <p/>
     * The value of {@link PriorityTask#TIMEOUT_DEFAULT TIMEOUT_DEFAULT} indicates
     * a default timeout value configured for the corresponding service; the value of
     * {@link PriorityTask#TIMEOUT_NONE TIMEOUT_NONE} indicates that the client thread
     * is willing to wait indefinitely until the task execution completes or is
     *
     * @param nDeadline  the deadline to set on the stub
     *
     * @return the default {@link NamedCacheServiceGrpc.NamedCacheServiceStub}
     */
    private NamedCacheServiceGrpc.NamedCacheServiceStub createStubWithDeadline(long nDeadline)
        {
        if (nDeadline == PriorityTask.TIMEOUT_NONE)
            {
            return createStubWithoutDeadline();
            }
        else if (nDeadline <= PriorityTask.TIMEOUT_DEFAULT)
            {
            return createStub();
            }
        return createStubWithoutDeadline().withDeadlineAfter(nDeadline, TimeUnit.MILLISECONDS);
        }

    /**
     * Create the default {@link NamedCacheServiceGrpc.NamedCacheServiceStub}
     * to use for requests.
     *
     * @return the default {@link NamedCacheServiceGrpc.NamedCacheServiceStub}
     */
    private NamedCacheServiceGrpc.NamedCacheServiceStub createStubWithoutDeadline()
        {
        Channel channel   = f_dependencies.getChannel();
        return NamedCacheServiceGrpc.newStub(channel);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The service dependencies.
     */
    private final AsyncNamedCacheClient.Dependencies f_dependencies;
    }
