/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.v0;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.grpc.proxy.common.BaseGrpcServiceImpl;
import com.oracle.coherence.grpc.proxy.common.GrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;
import com.oracle.coherence.grpc.v0.CacheRequestHolder;

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
import com.oracle.coherence.grpc.messages.cache.v0.IsReadyRequest;
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

import com.tangosol.io.Serializer;

import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.Filter;

import com.tangosol.util.filter.AlwaysFilter;

import io.grpc.stub.StreamObserver;

/**
 * A NamedCache service.
 *
 * @author Mahesh Kannan    2019.11.01
 * @author Jonathan Knight  2019.11.07
 * @since 20.06
 */
public interface NamedCacheService
        extends GrpcProxyService
    {
    /**
     * Add an index to a cache.
     *
     * @param request   the {@link AddIndexRequest} containing the name of the cache to add the index
     *                  to, the serialized {@link com.tangosol.util.ValueExtractor} to use to create
     *                  the index and the optional serialized {@link java.util.Comparator} to sort the
     *                  index
     * @param observer  the {@link StreamObserver} to receive the response
     */
    void addIndex(AddIndexRequest request, StreamObserver<Empty> observer);

    /**
     * Execute an {@link AggregateRequest} against a cache and return the result serialized in a {@link BytesValue}.
     *
     * @param request   the {@link AggregateRequest} to execute
     * @param observer  the {@link StreamObserver} to receive the response
     */
    void aggregate(AggregateRequest request, StreamObserver<BytesValue> observer);

    /**
     * Clear a cache.
     *
     * @param request   the {@link ClearRequest} to execute
     * @param observer  the {@link StreamObserver} to receive the response
     *
     * @see com.tangosol.net.NamedCache#clear()
     */
    void clear(ClearRequest request, StreamObserver<Empty> observer);

    /**
     * Returns {@code true} if this map contains a mapping for the specified key to the specified value.
     *
     * @param request   the request which contains the key and value whose presence
     *                  in this map is to be tested
     * @param observer  the {@link StreamObserver} to receive the response
     *
     * @see com.tangosol.net.NamedCache#containsKey(Object)
     */
    void containsEntry(ContainsEntryRequest request, StreamObserver<BoolValue> observer);

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @param request   the request which contains the key whose presence
     *                  in this map is to be tested
     * @param observer  the {@link StreamObserver} to receive the response
     *
     * @see com.tangosol.net.NamedCache#containsKey(Object)
     */
    void containsKey(ContainsKeyRequest request, StreamObserver<BoolValue> observer);

    /**
     * Returns {@code true} if this map contains a mapping for the specified value.
     *
     * @param request   the request which contains the value whose presence
     *                  in this map is to be tested
     * @param observer  the {@link StreamObserver} to receive the response
     *
     * @see com.tangosol.net.NamedCache#containsKey(Object)
     */
    void containsValue(ContainsValueRequest request, StreamObserver<BoolValue> observer);

    /**
     * Destroy a cache.
     *
     * @param request  the {@link DestroyRequest} containing the name of the cache to destroy
     * @param observer  the {@link StreamObserver} to receive the response
     */
    void destroy(DestroyRequest request, StreamObserver<Empty> observer);

    /**
     * Stream a set of cache entries to a {@link StreamObserver}.
     *
     * @param request   the {@link EntrySetRequest} to execute
     * @param observer  the {@link StreamObserver} to stream the entries to
     *
     * @see com.tangosol.net.NamedCache#entrySet(com.tangosol.util.Filter)
     * @see com.tangosol.net.NamedCache#entrySet(com.tangosol.util.Filter, java.util.Comparator)
     */
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
    StreamObserver<MapListenerRequest> events(StreamObserver<MapListenerResponse> observer);

    /**
     * Get a value for a given key from a cache.
     *
     * @param request   the {@link GetRequest} to execute
     * @param observer  the {@link io.grpc.stub.StreamObserver} to receive events
     *
     * @see com.tangosol.net.NamedCache#get(Object)
     */
    void get(GetRequest request, StreamObserver<OptionalValue> observer);

    /**
     * Obtain a stream of mappings of keys to values for all the
     * specified keys.
     *
     * @param request   the {@link GetAllRequest} request containing the cache name
     *                  and collection of keys to obtain the mappings for
     * @param observer  the {@link StreamObserver} to stream the results back to
     */
    void getAll(GetAllRequest request, StreamObserver<Entry> observer);

    /**
     * Invoke an {@link com.tangosol.util.InvocableMap.EntryProcessor} against an entry in a cache.
     *
     * @param request   the {@link InvokeRequest} containing the serialized key of the entry and the
     *                  serialized {@link com.tangosol.util.InvocableMap.EntryProcessor}
     * @param observer  the {@link StreamObserver} to stream the results back to
     */
    void invoke(InvokeRequest request, StreamObserver<BytesValue> observer);

    /**
     * Invoke an {@link com.tangosol.util.InvocableMap.EntryProcessor} against multiple entries in a cache.
     *
     * @param request   the {@link InvokeRequest} containing the serialized keys or serialized
     *                  {@link com.tangosol.util.Filter} to use to identify the entries and the
     *                  serialized {@link com.tangosol.util.InvocableMap.EntryProcessor}
     * @param observer  the {@link io.grpc.stub.StreamObserver} to observer the invocation results
     */
    void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer);

    /**
     * Determine whether a cache is empty.
     *
     * @param request   the {@link IsEmptyRequest} to execute
     * @param observer  the {@link io.grpc.stub.StreamObserver} to observer the invocation results
     *
     * @see com.tangosol.net.NamedCache#isEmpty()
     */
    void isEmpty(IsEmptyRequest request, StreamObserver<BoolValue> observer);

    /**
     * Determine whether a cache is Ready.
     *
     * @param request   the {@link IsReadyRequest} to execute
     * @param observer  the {@link io.grpc.stub.StreamObserver} to observer the invocation results
     *
     * @see com.tangosol.net.NamedCache#isReady()
     * @since 14.1.1.2206.5
     */
    void isReady(IsReadyRequest request, StreamObserver<BoolValue> observer);

    /**
     * Stream a set of cache keys to a {@link StreamObserver}.
     *
     * @param request   the {@link KeySetRequest} to execute
     * @param observer  the {@link StreamObserver} to stream the keys to
     *
     * @see com.tangosol.net.NamedCache#keySet(com.tangosol.util.Filter)
     */
    void keySet(KeySetRequest request, StreamObserver<BytesValue> observer);

    /**
     * Obtain the next page of a paged entry set request.
     *
     * @param request   the {@link PageRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     */
    void nextEntrySetPage(PageRequest request, StreamObserver<EntryResult> observer);

    /**
     * Obtain the next page of a paged key set request.
     *
     * @param request   the {@link PageRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     */
    void nextKeySetPage(PageRequest request, StreamObserver<BytesValue> observer);

    /**
     * Associate the specified value with the specified key in this cache.
     * If the cache previously contained a mapping for the key, the old value
     * is replaced by the specified value.
     *
     * @param request   the {@link PutRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     *
     * @see com.tangosol.net.NamedCache#put(Object, Object)
     */
    void put(PutRequest request, StreamObserver<BytesValue> observer);

    /**
     * Add the specified key value pair mappings to this cache.
     * If the cache previously contained a mappings for the keys, the old value
     * is replaced by the specified value.
     *
     * @param request   the {@link PutAllRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     *
     * @see com.tangosol.net.NamedCache#putAll(java.util.Map)
     */
    void putAll(PutAllRequest request, StreamObserver<Empty> observer);

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associate it with the given value and returns
     * {@code null}, else return the current value.
     *
     * @param request   the {@link PutIfAbsentRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     *
     * @see com.tangosol.net.NamedCache#putIfAbsent(Object, Object)
     */
    void putIfAbsent(PutIfAbsentRequest request, StreamObserver<BytesValue> observer);

    /**
     * Remove the mapping that is associated with the specified key.
     *
     * @param request  the {@link RemoveRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     *
     * @see com.tangosol.net.NamedCache#remove(Object)
     */
    void remove(RemoveRequest request, StreamObserver<BytesValue> observer);

    /**
     * Remove an index from a cache.
     *
     * @param request   the {@link RemoveIndexRequest} containing the name of the cache to remove the
     *                  index from, the serialized {@link com.tangosol.util.ValueExtractor} that was
     *                  used to create the index
     * @param observer  the {@link StreamObserver} that will receive the responses
     */
    void removeIndex(RemoveIndexRequest request, StreamObserver<Empty> observer);

    /**
     * Remove the mapping that is associated with the specified key only
     * if the mapping exists in the cache.
     *
     * @param request   the {@link RemoveMappingRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     *
     * @see com.tangosol.net.NamedCache#remove(Object, Object)
     */
    void removeMapping(RemoveMappingRequest request, StreamObserver<BoolValue> observer);

    /**
     * Replace the entry for the specified key only if it is currently
     * mapped to some value.
     *
     * @param request  the {@link ReplaceRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     *
     * @see com.tangosol.net.NamedCache#replace(Object, Object)
     */
    void replace(ReplaceRequest request, StreamObserver<BytesValue> observer);

    /**
     * Replace the mapping for the specified key only if currently mapped
     * to the specified value.
     *
     * @param request   the {@link ReplaceMappingRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     *
     * @see com.tangosol.net.NamedCache#replace(Object, Object, Object)
     */
    void replaceMapping(ReplaceMappingRequest request, StreamObserver<BoolValue> observer);

    /**
     * Determine the number of entries in a cache.
     *
     * @param request   the {@link SizeRequest} to execute
     * @param observer  the {@link StreamObserver} that will receive the responses
     *
     * @see com.tangosol.net.NamedCache#size()
     */
    void size(SizeRequest request, StreamObserver<Int32Value> observer);

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
     * @param request   the {@link TruncateRequest} containing the name of the cache to truncate
     * @param observer  the {@link StreamObserver} that will receive the responses
     */
    void truncate(TruncateRequest request, StreamObserver<Empty> observer);

    /**
     * Stream a set of cache values to a {@link StreamObserver}.
     *
     * @param request   the {@link ValuesRequest} to execute
     * @param observer  the {@link StreamObserver} to stream the values to
     *
     * @see com.tangosol.net.NamedCache#values(com.tangosol.util.Filter)
     * @see com.tangosol.net.NamedCache#values(com.tangosol.util.Filter, java.util.Comparator)
     */
    void values(ValuesRequest request, StreamObserver<BytesValue> observer);

    /**
     * Create a {@link CacheRequestHolder} for a given request.
     *
     * @param <Req>       the type of the request
     * @param request     the request object to add to the holder
     * @param sScope      the scope name to use to identify the CCF to obtain the cache from
     * @param sCacheName  the name of the cache that the request executes against
     * @param format      the optional serialization format used by requests that contain a payload
     *
     * @return the {@link CacheRequestHolder} holding the request
     */
    <Req> CacheRequestHolder<Req, Void> createRequestHolder(Req    request,
                                                            String sScope,
                                                            String sCacheName,
                                                            String format);

    /**
     * Obtain a {@link Filter} from the serialized data in a {@link ByteString}.
     * <p>
     * If the {@link ByteString} is {@code null} or {@link ByteString#EMPTY} then
     * an {@link AlwaysFilter} is returned.
     *
     * @param bytes       the {@link ByteString} containing the serialized {@link Filter}
     * @param serializer  the serializer to use
     * @param <T>         the {@link Filter} type
     *
     * @return a deserialized {@link Filter}
     */
    <T> Filter<T> ensureFilter(ByteString bytes, Serializer serializer);

    /**
     * Obtain a {@link Filter} from the serialized data in a {@link ByteString}.
     *
     * @param bytes       the {@link ByteString} containing the serialized {@link Filter}
     * @param serializer  the serializer to use
     *  @param <T>        the {@link Filter} type
     *
     * @return a deserialized {@link Filter} or {@code null} if no filter is set
     */
    <T> Filter<T> getFilter(ByteString bytes, Serializer serializer);

    // ----- inner interface: Dependencies ----------------------------------

    /**
     * The dependencies to configure a {@link NamedCacheService}.
     */
    interface Dependencies
            extends BaseGrpcServiceImpl.Dependencies
        {
        /**
         * Returns the frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional events channel.
         *
         * @return the frequency in millis that heartbeats should be sent by the
         *         proxy to the client bidirectional events channel
         */
        long getEventsHeartbeat();

        /**
         * The default heartbeat frequency value representing no heartbeats to be sent.
         */
        long NO_EVENTS_HEARTBEAT = 0L;
        }

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * The default {@link NamedCacheService.Dependencies} implementation.
     */
    class DefaultDependencies
            extends BaseGrpcServiceImpl.DefaultDependencies
            implements NamedCacheService.Dependencies
        {
        public DefaultDependencies(GrpcDependencies.ServerType serverType)
            {
            super(serverType);
            }

        public DefaultDependencies(GrpcServiceDependencies deps)
            {
            super(deps);
            }

        public DefaultDependencies(NamedCacheService.Dependencies deps)
            {
            super(deps);
            }

        @Override
        public long getEventsHeartbeat()
            {
            return m_nEventsHeartbeat;
            }

        /**
         * Set the frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional events channel.
         * <p/>
         * If the frequency is set to zero or less, then no heartbeats will be sent.
         *
         * @param nEventsHeartbeat the heartbeat frequency in millis
         */
        public void setEventsHeartbeat(long nEventsHeartbeat)
            {
            m_nEventsHeartbeat = Math.max(NO_EVENTS_HEARTBEAT, nEventsHeartbeat);
            }

        // ----- data members -----------------------------------------------

        /**
         * The frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional events channel
         */
        private long m_nEventsHeartbeat = NO_EVENTS_HEARTBEAT;
        }
    }
