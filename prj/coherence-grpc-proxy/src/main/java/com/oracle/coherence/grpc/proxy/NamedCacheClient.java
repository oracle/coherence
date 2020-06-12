/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.BoolValue;
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
import com.oracle.coherence.grpc.SizeRequest;
import com.oracle.coherence.grpc.TruncateRequest;
import com.oracle.coherence.grpc.ValuesRequest;

import io.grpc.stub.StreamObserver;

import io.helidon.microprofile.grpc.core.Bidirectional;
import io.helidon.microprofile.grpc.core.Grpc;
import io.helidon.microprofile.grpc.core.ServerStreaming;
import io.helidon.microprofile.grpc.core.Unary;

import java.util.concurrent.CompletionStage;

/**
 * A gRPC NamedCache service.
 *
 * @author Mahesh Kannan    2019.11.01
 * @author Jonathan Knight  2019.11.07
 * @since 14.1.2
 */
@Grpc(name = "coherence.NamedCacheService")
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public interface NamedCacheClient
    {
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
    @Unary
    CompletionStage<Empty> addIndex(AddIndexRequest request);

    /**
     * Execute an {@link AggregateRequest} against a cache and return the result serialized in a {@link BytesValue}.
     *
     * @param request  the {@link AggregateRequest} to execute
     *
     * @return the serialized aggregation result
     */
    @Unary
    CompletionStage<BytesValue> aggregate(AggregateRequest request);

    /**
     * Clear a cache.
     *
     * @param request  the {@link ClearRequest} to execute
     *
     * @return a {@link java.util.concurrent.CompletionStage} that will complete when the cache
     *         has been cleared.
     *
     * @see com.tangosol.net.NamedCache#clear()
     */
    @Unary
    CompletionStage<Empty> clear(ClearRequest request);

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
    @Unary
    CompletionStage<BoolValue> containsEntry(ContainsEntryRequest request);

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
    @Unary
    CompletionStage<BoolValue> containsKey(ContainsKeyRequest request);

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
    @Unary
    CompletionStage<BoolValue> containsValue(ContainsValueRequest request);

    /**
     * Destroy a cache.
     *
     * @param request  the {@link DestroyRequest} containing the name of the cache to destroy
     *
     * @return a {@link CompletionStage} that will complete when the cache is destroyed
     */
    @Unary
    CompletionStage<Empty> destroy(DestroyRequest request);

    /**
     * Stream a set of cache entries to a {@link StreamObserver}.
     *
     * @param request   the {@link EntrySetRequest} to execute
     * @param observer  the {@link StreamObserver} to stream the entries to
     *
     * @see com.tangosol.net.NamedCache#entrySet(com.tangosol.util.Filter)
     * @see com.tangosol.net.NamedCache#entrySet(com.tangosol.util.Filter, java.util.Comparator)
     */
    @ServerStreaming
    void entrySet(EntrySetRequest request, StreamObserver<Entry> observer);

    /**
     * Add a {@link com.tangosol.util.MapListener} to a cache and stream
     * the events received to the {@link io.grpc.stub.StreamObserver}.
     *
     * @param observer  the {@link io.grpc.stub.StreamObserver} to receive events
     *
     * @return a {@link io.grpc.stub.StreamObserver} that will be closed by the
     *         client to end event subscription
     */
    @Bidirectional
    StreamObserver<MapListenerRequest> events(StreamObserver<MapListenerResponse> observer);

    /**
     * Get a value for a given key from a cache.
     *
     * @param request  the {@link GetRequest} to execute
     *
     * @return a {@link java.util.concurrent.CompletionStage} that will complete with the result of
     *         the {@link com.tangosol.net.NamedCache#get(Object)}
     *
     * @see com.tangosol.net.NamedCache#get(Object)
     */
    @Unary
    CompletionStage<OptionalValue> get(GetRequest request);

    /**
     * Obtain a stream of mappings of keys to values for all of the
     * specified keys.
     *
     * @param request   the {@link GetAllRequest} request containing the cache name
     *                  and collection of keys to obtain the mappings for
     * @param observer  the {@link StreamObserver} to stream the results back to
     */
    @ServerStreaming
    void getAll(GetAllRequest request, StreamObserver<Entry> observer);

    /**
     * Invoke an {@link com.tangosol.util.InvocableMap.EntryProcessor} against an entry in a cache.
     *
     * @param request  the {@link InvokeRequest} containing the serialized key of the entry and the
     *                 serialized {@link com.tangosol.util.InvocableMap.EntryProcessor}
     *
     * @return the serialized result of the {@link com.tangosol.util.InvocableMap.EntryProcessor} invocation
     */
    @Unary
    CompletionStage<BytesValue> invoke(InvokeRequest request);

    /**
     * Invoke an {@link com.tangosol.util.InvocableMap.EntryProcessor} against multiple entries in a cache.
     *
     * @param request   the {@link InvokeRequest} containing the serialized keys or serialized
     *                  {@link com.tangosol.util.Filter} to use to identify the entries and the
     *                  serialized {@link com.tangosol.util.InvocableMap.EntryProcessor}
     * @param observer  the {@link io.grpc.stub.StreamObserver} to observer the invocation results
     */
    @ServerStreaming
    void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer);

    /**
     * Determine whether a cache is empty.
     *
     * @param request  the {@link IsEmptyRequest} to execute
     *
     * @return a {@link java.util.concurrent.CompletionStage} that will complete with the result
     *         of the {@link com.tangosol.net.NamedCache#isEmpty()}
     *
     * @see com.tangosol.net.NamedCache#isEmpty()
     */
    @Unary
    CompletionStage<BoolValue> isEmpty(IsEmptyRequest request);

    /**
     * Stream a set of cache keys to a {@link StreamObserver}.
     *
     * @param request   the {@link KeySetRequest} to execute
     * @param observer  the {@link StreamObserver} to stream the keys to
     *
     * @see com.tangosol.net.NamedCache#keySet(com.tangosol.util.Filter)
     */
    @ServerStreaming
    void keySet(KeySetRequest request, StreamObserver<BytesValue> observer);

    /**
     * Obtain the next page of a paged entry set request.
     *
     * @param request   the {@link PageRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     */
    @ServerStreaming
    void nextEntrySetPage(PageRequest request, StreamObserver<EntryResult> observer);

    /**
     * Obtain the next page of a paged key set request.
     *
     * @param request   the {@link PageRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     */
    @ServerStreaming
    void nextKeySetPage(PageRequest request, StreamObserver<BytesValue> observer);

    /**
     * Associate the specified value with the specified key in this cache.
     * If the cache previously contained a mapping for the key, the old value
     * is replaced by the specified value.
     *
     * @param request  the {@link PutRequest} to execute
     *
     * @return a {@link java.util.concurrent.CompletionStage} that will complete with the result of
     *         the {@link com.tangosol.net.NamedCache#put(Object, Object)}
     *
     * @see com.tangosol.net.NamedCache#put(Object, Object)
     */
    @Unary
    CompletionStage<BytesValue> put(PutRequest request);

    /**
     * Add the specified key value pair mappings to this cache.
     * If the cache previously contained a mappings for the keys, the old value
     * is replaced by the specified value.
     *
     * @param request  the {@link PutAllRequest} to execute
     *
     * @return a {@link java.util.concurrent.CompletionStage} that will complete with the result of
     *         the {@link com.tangosol.net.NamedCache#putAll(java.util.Map)}
     *
     * @see com.tangosol.net.NamedCache#putAll(java.util.Map)
     */
    @Unary
    CompletionStage<Empty> putAll(PutAllRequest request);

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associate it with the given value and returns
     * {@code null}, else return the current value.
     *
     * @param request  the {@link PutIfAbsentRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the previous value associated
     *         with the specified key, or {@code null} if there was no mapping for the key.
     *         A {@code null} return can also indicate that the map previously associated
     *         {@code null} with the key, if the implementation supports {@code null} values
     *
     * @see com.tangosol.net.NamedCache#putIfAbsent(Object, Object)
     */
    @Unary
    CompletionStage<BytesValue> putIfAbsent(PutIfAbsentRequest request);

    /**
     * Remove the mapping that is associated with the specified key.
     *
     * @param request  the {@link RemoveRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the
     *         previous value associated with specified key, or null
     *         if there was no mapping for key.
     *
     * @see com.tangosol.net.NamedCache#remove(Object)
     */
    @Unary
    CompletionStage<BytesValue> remove(RemoveRequest request);

    /**
     * Remove an index from a cache.
     *
     * @param request  the {@link RemoveIndexRequest} containing the name of the cache to remove the
     *                 index from, the serialized {@link com.tangosol.util.ValueExtractor} that was
     *                 used to create the index
     *
     * @return a {@link CompletionStage} that will complete when the index is removed
     */
    @Unary
    CompletionStage<Empty> removeIndex(RemoveIndexRequest request);

    /**
     * Remove the mapping that is associated with the specified key only
     * if the mapping exists in the cache.
     *
     * @param request  the {@link RemoveMappingRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with {@code true}
     *         if the removal was successful, {@code false} otherwise
     *
     * @see com.tangosol.net.NamedCache#remove(Object, Object)
     */
    @Unary
    CompletionStage<BoolValue> removeMapping(RemoveMappingRequest request);

    /**
     * Replace the entry for the specified key only if it is currently
     * mapped to some value.
     *
     * @param request  the {@link ReplaceRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the
     *         previous value associated with specified key, or null
     *         if there was no mapping for key.
     *
     * @see com.tangosol.net.NamedCache#replace(Object, Object)
     */
    @Unary
    CompletionStage<BytesValue> replace(ReplaceRequest request);

    /**
     * Replace the mapping for the specified key only if currently mapped
     * to the specified value.
     *
     * @param request  the {@link ReplaceMappingRequest} to execute
     *
     * @return a {@link CompletionStage} that will complete with the previous value associated
     *         with the specified key, or {@code null} if there was no mapping for the key.
     *         A {@code null} return can also indicate that the map previously associated
     *         {@code null} with the key, if the implementation supports null values
     *
     * @see com.tangosol.net.NamedCache#replace(Object, Object, Object)
     */
    @Unary
    CompletionStage<BoolValue> replaceMapping(ReplaceMappingRequest request);

    /**
     * Determine the number of entries in a cache.
     *
     * @param request the {@link SizeRequest} to execute
     *
     * @return a {@link java.util.concurrent.CompletionStage} that will complete with the result
     *         of the {@link com.tangosol.net.NamedCache#size()}
     *
     * @see com.tangosol.net.NamedCache#size()
     */
    @Unary
    CompletionStage<Int32Value> size(SizeRequest request);

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
    @Unary
    CompletionStage<Empty> truncate(TruncateRequest request);

    /**
     * Stream a set of cache values to a {@link StreamObserver}.
     *
     * @param request   the {@link ValuesRequest} to execute
     * @param observer  the {@link StreamObserver} to stream the values to
     *
     * @see com.tangosol.net.NamedCache#values(com.tangosol.util.Filter)
     * @see com.tangosol.net.NamedCache#values(com.tangosol.util.Filter, java.util.Comparator)
     */
    @ServerStreaming
    void values(ValuesRequest request, StreamObserver<BytesValue> observer);
    }
