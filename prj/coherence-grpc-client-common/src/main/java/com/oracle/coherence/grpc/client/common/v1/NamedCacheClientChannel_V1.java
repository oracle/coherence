/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.v1;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.MaybeByteString;
import com.oracle.coherence.grpc.SafeStreamObserver;
import com.oracle.coherence.grpc.client.common.AsyncNamedCacheClient;
import com.oracle.coherence.grpc.client.common.BaseNamedCacheClientChannel;
import com.oracle.coherence.grpc.client.common.FutureStreamObserver;
import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.oracle.coherence.grpc.client.common.NamedCacheClientChannel;
import com.oracle.coherence.grpc.client.common.StreamStreamObserver;
import com.oracle.coherence.grpc.messages.cache.v1.EnsureCacheRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ExecuteRequest;
import com.oracle.coherence.grpc.messages.cache.v1.IndexRequest;
import com.oracle.coherence.grpc.messages.cache.v1.KeyOrFilter;
import com.oracle.coherence.grpc.messages.cache.v1.KeysOrFilter;
import com.oracle.coherence.grpc.messages.cache.v1.MapEventMessage;
import com.oracle.coherence.grpc.messages.cache.v1.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequest;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequestType;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.oracle.coherence.grpc.messages.cache.v1.PutAllRequest;
import com.oracle.coherence.grpc.messages.cache.v1.PutRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ReplaceMappingRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ResponseType;
import com.oracle.coherence.grpc.messages.common.v1.BinaryKeyAndValue;
import com.oracle.coherence.grpc.messages.common.v1.CollectionOfBytesValues;
import com.oracle.coherence.grpc.messages.common.v1.OptionalValue;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.util.SimpleMapEntry;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The version 1 implementation of a {@link NamedCacheClientChannel}.
 */
public class NamedCacheClientChannel_V1
        extends BaseNamedCacheClientChannel
    {
    /**
     * Create a version one cache client protocol.
     *
     * @param dependencies the dependencies to use to configure the client
     * @param connection   the {@link GrpcConnection} to use to send requests
     */
    public NamedCacheClientChannel_V1(AsyncNamedCacheClient.Dependencies dependencies, GrpcConnection connection)
        {
        super(dependencies, connection);

        EnsureCacheRequest ensureCache = EnsureCacheRequest.newBuilder()
                .setCache(dependencies.getName())
                .build();

        NamedCacheRequest request = NamedCacheRequest.newBuilder()
                .setType(NamedCacheRequestType.EnsureCache)
                .setMessage(Any.pack(ensureCache))
                .build();

        NamedCacheResponse response = connection.send(request);
        f_nCacheId = response.getCacheId();
        }

    @Override
    public CompletableFuture<Void> addIndex(ByteString extractor, boolean fOrdered, ByteString comparator)
        {
        IndexRequest request = IndexRequest.newBuilder()
                .setAdd(true)
                .setExtractor(extractor)
                .setSorted(fOrdered)
                .setComparator(Objects.requireNonNullElse(comparator, ByteString.EMPTY))
                .build();

        return poll(NamedCacheRequestType.Index, request)
                .thenApply(r -> VOID);
        }

    @Override
    public CompletableFuture<Void> addMapListener(ByteString key, boolean fLite, boolean fPriming, boolean fSynchronous)
        {
        MapListenerRequest request = MapListenerRequest.newBuilder()
                .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(key).build())
                .setLite(fLite)
                .setPriming(fPriming)
                .setSubscribe(true)
                .setSynchronous(fSynchronous)
                .build();

        return poll(NamedCacheRequestType.MapListener, request)
                .thenApply(r -> VOID);
        }

    @Override
    public CompletableFuture<Void> addMapListener(ByteString filterBytes, long nFilterId, boolean fLite, ByteString triggerBytes, boolean fSynchronous)
        {
        MapListenerRequest.Builder builder = MapListenerRequest.newBuilder()
                .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytes).build())
                .setFilterId(nFilterId)
                .setLite(fLite)
                .setSynchronous(fSynchronous)
                .setSubscribe(true);

        if (triggerBytes != null)
            {
            builder.setTrigger(triggerBytes);
            }

        return poll(NamedCacheRequestType.MapListener, builder.build())
                .thenApply(r -> VOID);
        }

    @Override
    public CompletableFuture<BytesValue> aggregate(List<ByteString> collKeys, ByteString aggregator, long nDeadline)
        {
        CollectionOfBytesValues binaryKeys = CollectionOfBytesValues.newBuilder()
                .addAllValues(collKeys)
                .build();

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(aggregator)
                .setKeys(KeysOrFilter.newBuilder().setKeys(binaryKeys).build())
                .build();

        return poll(NamedCacheRequestType.Aggregate, request)
                .thenApply(this::unpackBytes);
        }

    @Override
    public CompletableFuture<BytesValue> aggregate(ByteString filter, ByteString aggregator, long nDeadline)
        {
        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(aggregator)
                .setKeys(KeysOrFilter.newBuilder().setFilter(filter).build())
                .build();

        return poll(NamedCacheRequestType.Aggregate, request)
                .thenApply(this::unpackBytes);
        }

    @Override
    public CompletableFuture<Void> clear()
        {
        return poll(NamedCacheRequestType.Clear).thenApply(r -> VOID);
        }

    @Override
    public CompletionStage<BoolValue> containsEntry(ByteString key, ByteString value)
        {
        return poll(NamedCacheRequestType.ContainsEntry, toBinaryKeyAndValue(key, value))
                .thenApply(this::unpackBoolean);
        }

    @Override
    public CompletionStage<BoolValue> containsKey(ByteString key)
        {
        return poll(NamedCacheRequestType.ContainsKey, toBinaryValue(key))
                .thenApply(this::unpackBoolean);
        }

    @Override
    public CompletionStage<BoolValue> containsValue(ByteString value)
        {
        return poll(NamedCacheRequestType.ContainsValue, toBinaryValue(value))
                .thenApply(this::unpackBoolean);
        }

    @Override
    public CompletableFuture<Void> destroy()
        {
        return poll(NamedCacheRequestType.Destroy).thenApply(r -> VOID);
        }

    @Override
    public CompletionStage<MaybeByteString> get(ByteString key)
        {
        return poll(NamedCacheRequestType.Get, toBinaryValue(key))
                .thenApply(response ->
                    {
                    OptionalValue optional = unpackMessage(response, OptionalValue.class);
                    if (optional.getPresent())
                        {
                        return MaybeByteString.ofNullable(optional.getValue());
                        }
                    return MaybeByteString.empty();
                    });
        }

    @Override
    public Stream<Map.Entry<ByteString, ByteString>> getAll(Iterable<ByteString> keys)
        {
        CollectionOfBytesValues                 values   = CollectionOfBytesValues.newBuilder().addAllValues(keys).build();
        StreamStreamObserver<NamedCacheResponse> observer = new StreamStreamObserver<>();
        poll(NamedCacheRequestType.GetAll, values, observer);
        try
            {
            return observer.future().get().stream()
                    .map(resp ->
                        {
                        BinaryKeyAndValue keyAndValue = unpackMessage(resp, BinaryKeyAndValue.class);
                        return new SimpleMapEntry<>(keyAndValue.getKey(), keyAndValue.getValue());
                        });
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public EntrySetPage getEntriesPage(ByteString cookie)
        {
        try
            {
            PageResultObserver observer    = new PageResultObserver();
            BytesValue         cookieBytes = cookie == null ? BytesValue.getDefaultInstance() : BytesValue.of(cookie);

            poll(NamedCacheRequestType.PageOfEntries, cookieBytes, observer);

            List<Any> list = observer.future().get();
            if (list.isEmpty())
                {
                return new EntrySetPage(null, List.of());
                }
            Iterator<Any>                           iterator   = list.iterator();
            BytesValue                              nextCookie = iterator.next().unpack(BytesValue.class);
            List<Map.Entry<ByteString, ByteString>> entries    = new ArrayList<>();
            while (iterator.hasNext())
                {
                BinaryKeyAndValue keyAndValue = iterator.next().unpack(BinaryKeyAndValue.class);
                entries.add(new SimpleMapEntry<>(keyAndValue.getKey(), keyAndValue.getValue()));
                }
            return new EntrySetPage(nextCookie.getValue(), entries);
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public Stream<BytesValue> getKeysPage(ByteString cookie)
        {
        try
            {
            PageResultObserver observer    = new PageResultObserver();
            BytesValue         cookieBytes = cookie == null ? BytesValue.getDefaultInstance() : BytesValue.of(cookie);

            poll(NamedCacheRequestType.PageOfKeys, cookieBytes, observer);

            List<Any> list = observer.future().get();
            return list.stream().map(this::unpackBytes);
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public CompletionStage<BytesValue> invoke(ByteString key, ByteString processor, long nDeadline)
        {
        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(processor)
                .setKeys(KeysOrFilter.newBuilder().setKey(key).build())
                .build();

        MapStreamObserver observer = new MapStreamObserver();
        poll(NamedCacheRequestType.Invoke, request, observer);

        return observer.future().thenApply(map ->
            {
            ByteString bytes = map.get(key);
            return bytes == null ? BytesValue.of(ByteString.empty()) : BytesValue.of(map.get(key));
            });
        }

    @Override
    public CompletableFuture<Map<ByteString, ByteString>> invokeAll(Collection<ByteString> colKeys, ByteString processor, long nDeadline)
        {
        CollectionOfBytesValues binaryKeys = CollectionOfBytesValues.newBuilder()
                .addAllValues(colKeys)
                .build();

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(processor)
                .setKeys(KeysOrFilter.newBuilder().setKeys(binaryKeys).build())
                .build();

        MapStreamObserver observer = new MapStreamObserver();
        poll(NamedCacheRequestType.Invoke, request, observer);
        return observer.future();
        }

    @Override
    public CompletableFuture<Void> invokeAll(Collection<ByteString> colKeys, ByteString processor, Consumer<Map.Entry<ByteString, ByteString>> callback)
        {
        CollectionOfBytesValues binaryKeys = CollectionOfBytesValues.newBuilder()
                .addAllValues(colKeys)
                .build();

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(processor)
                .setKeys(KeysOrFilter.newBuilder().setKeys(binaryKeys).build())
                .build();

        BiConsumer<ByteString, ByteString> consumer = (k, v) -> callback.accept(new SimpleMapEntry<>(k, v));
        ForwardingStreamObserver observer = new ForwardingStreamObserver(consumer);
        poll(NamedCacheRequestType.Invoke, request, observer);
        return observer.future().thenApply(r -> VOID);
        }

    @Override
    public CompletableFuture<Void> invokeAll(Collection<ByteString> colKeys, ByteString processor, BiConsumer<ByteString, ByteString> callback)
        {
        CollectionOfBytesValues binaryKeys = CollectionOfBytesValues.newBuilder()
                .addAllValues(colKeys)
                .build();

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(processor)
                .setKeys(KeysOrFilter.newBuilder().setKeys(binaryKeys).build())
                .build();

        ForwardingStreamObserver observer = new ForwardingStreamObserver(callback);
        poll(NamedCacheRequestType.Invoke, request, observer);
        return observer.future().thenApply(r -> VOID);
        }

    @Override
    public CompletableFuture<Map<ByteString, ByteString>> invokeAll(ByteString filter, ByteString processor)
        {
        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(processor)
                .setKeys(KeysOrFilter.newBuilder().setFilter(filter).build())
                .build();

        MapStreamObserver observer = new MapStreamObserver();
        poll(NamedCacheRequestType.Invoke, request, observer);
        return observer.future();
        }

    @Override
    public CompletableFuture<Void> invokeAll(ByteString filter, ByteString processor, Consumer<Map.Entry<ByteString, ByteString>> callback)
        {
        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(processor)
                .setKeys(KeysOrFilter.newBuilder().setFilter(filter).build())
                .build();

        BiConsumer<ByteString, ByteString> consumer = (k, v) -> callback.accept(new SimpleMapEntry<>(k, v));
        ForwardingStreamObserver observer = new ForwardingStreamObserver(consumer);
        poll(NamedCacheRequestType.Invoke, request, observer);
        return observer.future().thenApply(r -> VOID);
        }

    @Override
    public CompletableFuture<Void> invokeAll(ByteString filter, ByteString processor, BiConsumer<ByteString, ByteString> callback)
        {
        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(processor)
                .setKeys(KeysOrFilter.newBuilder().setFilter(filter).build())
                .build();

        ForwardingStreamObserver observer = new ForwardingStreamObserver(callback);
        poll(NamedCacheRequestType.Invoke, request, observer);
        return observer.future().thenApply(r -> VOID);
        }

    @Override
    public CompletionStage<BoolValue> isEmpty()
        {
        return poll(NamedCacheRequestType.IsEmpty)
                .thenApply(this::unpackBoolean);
        }

    @Override
    public CompletionStage<BoolValue> isReady()
        {
        return poll(NamedCacheRequestType.IsReady).thenApply(this::unpackBoolean);
        }

    @Override
    public CompletableFuture<Empty> putAll(Map<ByteString, ByteString> map, long ttl)
        {
        List<BinaryKeyAndValue> list = map.entrySet().stream()
                .map(this::toBinaryKeyAndValue)
                .toList();

        PutAllRequest request = PutAllRequest.newBuilder()
                .addAllEntries(list)
                .setTtl(ttl)
                .build();

        return poll(NamedCacheRequestType.PutAll, request)
                .thenApply(r -> Empty.getDefaultInstance());
        }

    @Override
    public CompletionStage<BytesValue> put(ByteString key, ByteString value, long ttl)
        {
        return poll(NamedCacheRequestType.Put, putRequest(key, value, ttl))
                .thenApply(this::unpackBytes);
        }

    @Override
    public CompletionStage<BytesValue> putIfAbsent(ByteString key, ByteString value)
        {
        return poll(NamedCacheRequestType.PutIfAbsent, putRequest(key, value))
                .thenApply(this::unpackBytes);
        }

    @Override
    public CompletionStage<BytesValue> remove(ByteString key)
        {
        return poll(NamedCacheRequestType.Remove, toBinaryValue(key))
                .thenApply(this::unpackBytes);
        }

    @Override
    public CompletionStage<BoolValue> remove(ByteString key, ByteString value)
        {
        return poll(NamedCacheRequestType.RemoveMapping, toBinaryKeyAndValue(key, value))
                .thenApply(this::unpackBoolean);
        }

    @Override
    public CompletionStage<Empty> removeIndex(ByteString extractor)
        {
        IndexRequest request = IndexRequest.newBuilder()
                .setAdd(false)
                .setExtractor(extractor)
                .build();

        return poll(NamedCacheRequestType.Index, request)
                .thenApply(r -> Empty.getDefaultInstance());
        }

    @Override
    public CompletableFuture<Void> removeMapListener(ByteString key, boolean fPriming)
        {
        MapListenerRequest request = MapListenerRequest.newBuilder()
                .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(key).build())
                .setPriming(fPriming)
                .setSubscribe(false)
                .build();

        return poll(NamedCacheRequestType.MapListener, request)
                .thenApply(r -> VOID);
        }

    @Override
    public CompletableFuture<Void> removeMapListener(ByteString filterBytes, long nFilterId, ByteString triggerBytes)
        {
        MapListenerRequest.Builder builder = MapListenerRequest.newBuilder()
                .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytes).build())
                .setFilterId(nFilterId)
                .setSubscribe(false);

        if (triggerBytes != null)
            {
            builder.setTrigger(triggerBytes);
            }

        return poll(NamedCacheRequestType.MapListener, builder.build())
                .thenApply(r -> VOID);
        }

    @Override
    public CompletionStage<BytesValue> replace(ByteString key, ByteString value)
        {
        return poll(NamedCacheRequestType.Replace, toBinaryKeyAndValue(key, value))
                .thenApply(this::unpackBytes);
        }

    @Override
    public CompletionStage<BoolValue> replaceMapping(ByteString key, ByteString oldValue, ByteString newValue)
        {
        ReplaceMappingRequest request = ReplaceMappingRequest.newBuilder()
                .setKey(key)
                .setPreviousValue(oldValue)
                .setNewValue(newValue)
                .build();
        return poll(NamedCacheRequestType.ReplaceMapping, request)
                .thenApply(this::unpackBoolean);
        }

    @Override
    public void setEventDispatcher(EventDispatcher dispatcher)
        {
        f_lock.lock();
        try
            {
            if (m_eventObserver == null)
                {
                m_eventObserver = new EventObserver(dispatcher);
                f_connection.addResponseObserver(new GrpcConnection.Listener<>(m_eventObserver, m -> m.getCacheId() == getCacheId()));
                }
            else
                {
                throw new IllegalStateException("Event dispatcher is already set");
                }
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    public CompletionStage<Int32Value> size()
        {
        return poll(NamedCacheRequestType.Size).thenApply(this::unpackInteger);
        }

    @Override
    public CompletionStage<Empty> truncate()
        {
        return poll(NamedCacheRequestType.Truncate).thenApply(r -> Empty.getDefaultInstance());
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the cache identifier for this protocol.
     *
     * @return the cache identifier for this protocol
     */
    protected int getCacheId()
        {
        return f_nCacheId;
        }

    /**
     * Convert a {@link ByteString} to a {@link BytesValue}.
     *
     * @param bytes  the {@link ByteString}
     *
     * @return a {@link BytesValue} wrapping the specified {@link ByteString}
     */
    protected BytesValue toBinaryValue(ByteString bytes)
        {
        return BytesValue.newBuilder().setValue(bytes).build();
        }

    protected BinaryKeyAndValue toBinaryKeyAndValue(Map.Entry<ByteString, ByteString> entry)
        {
        return toBinaryKeyAndValue(entry.getKey(), entry.getValue());
        }

    /**
     * Convert a {@link ByteString} key and value to a {@link BinaryKeyAndValue}.
     *
     * @param key    the key {@link ByteString}
     * @param value  the value {@link ByteString}
     *
     * @return a {@link BinaryKeyAndValue} wrapping the specified {@link ByteString} key and value
     */
    protected BinaryKeyAndValue toBinaryKeyAndValue(ByteString key, ByteString value)
        {
        return BinaryKeyAndValue.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
        }

    /**
     * Create a {@link PutRequest}.
     *
     * @param key    the serialized key
     * @param value  the serialized value
     *
     * @return a {@link PutRequest} for the specified key and value
     */
    protected PutRequest putRequest(ByteString key, ByteString value)
        {
        return putRequest(key, value, NamedCache.EXPIRY_DEFAULT);
        }

    /**
     * Create a {@link PutRequest}.
     *
     * @param key    the serialized key
     * @param value  the serialized value
     * @param ttl    the TTL for the cache entry
     *
     * @return a {@link PutRequest} for the specified key and value
     */
    protected PutRequest putRequest(ByteString key, ByteString value, long ttl)
        {
        return PutRequest.newBuilder().setKey(key).setValue(value).setTtl(ttl).build();
        }

    /**
     * Unpack a {@link BoolValue} from the {@link Any} value in a {@link NamedCacheResponse}
     * {@link NamedCacheResponse#getMessage() message field}.
     *
     * @param response  the {@link NamedCacheResponse}
     *
     * @return the unpacked {@link BoolValue}
     */
    protected BoolValue unpackBoolean(NamedCacheResponse response)
        {
        return unpackMessage(response, BoolValue.class);
        }

    /**
     * Unpack a {@link Int32Value} from the {@link Any} value in a {@link NamedCacheResponse}
     * {@link NamedCacheResponse#getMessage() message field}.
     *
     * @param response  the {@link NamedCacheResponse}
     *
     * @return the unpacked {@link Int32Value}
     */
    protected Int32Value unpackInteger(NamedCacheResponse response)
        {
        return unpackMessage(response, Int32Value.class);
        }

    /**
     * Unpack a {@link BytesValue} from the {@link Any} value in a {@link NamedCacheResponse}
     * {@link NamedCacheResponse#getMessage() message field}.
     *
     * @param response  the {@link NamedCacheResponse}
     *
     * @return the unpacked {@link BytesValue}
     */
    protected BytesValue unpackBytes(NamedCacheResponse response)
        {
        return unpackMessage(response, BytesValue.class);
        }


    /**
     * Unpack a {@link BytesValue} from the {@link Any} value in a {@link NamedCacheResponse}
     * {@link NamedCacheResponse#getMessage() message field}.
     *
     * @param any  the {@link Any} to unpack the {@link BytesValue} from
     *
     * @return the unpacked {@link BytesValue}
     */
    protected BytesValue unpackBytes(Any any)
        {
        try
            {
            return any.unpack(BytesValue.class);
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Unpack a value from the {@link Any} value in a {@link NamedCacheResponse}
     * {@link NamedCacheResponse#getMessage() message field}.
     *
     * @param response  the {@link NamedCacheResponse}
     * @param type      they expected type of the message field
     *
     * @return the unpacked value
     */
    protected <T extends Message> T unpackMessage(NamedCacheResponse response, Class<T> type)
        {
        try
            {
            return response.getMessage().unpack(type);
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Asynchronously Send a {@link NamedCacheRequest}.
     *
     * @param type  the request message type to send
     *
     * @return a {@link CompletableFuture} that will complete with eh message response
     */
    protected CompletableFuture<NamedCacheResponse> poll(NamedCacheRequestType type)
        {
        return poll(type, null);
        }

    /**
     * Asynchronously Send a {@link NamedCacheRequest}.
     *
     * @param type     the request message type to send
     * @param message  the message to send in the {@link NamedCacheRequest}
     *
     * @return a {@link CompletableFuture} that will complete with eh message response
     */
    protected CompletableFuture<NamedCacheResponse> poll(NamedCacheRequestType type, Message message)
        {
        NamedCacheRequest.Builder builder = NamedCacheRequest.newBuilder()
                .setCacheId(f_nCacheId)
                .setType(type);

        if (message != null)
            {
            builder.setMessage(Any.pack(message));
            }
        else
            {
            builder.setMessage(Any.pack(Empty.getDefaultInstance()));
            }

        return f_connection.poll(builder.build());
        }

    /**
     * Asynchronously Send a {@link NamedCacheRequest}.
     *
     * @param type      the request message type to send
     * @param message   the message to send in the {@link NamedCacheRequest}
     * @param observer  a {@link StreamObserver} to receive the responses
     */
    protected void poll(NamedCacheRequestType type, Message message, StreamObserver<NamedCacheResponse> observer)
        {
        NamedCacheRequest.Builder builder = NamedCacheRequest.newBuilder()
                .setCacheId(f_nCacheId)
                .setType(type);

        if (message != null)
            {
            builder.setMessage(Any.pack(message));
            }

        f_connection.poll(builder.build(), SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- inner class: PageResultObserver --------------------------------

    /**
     * A {@link FutureStreamObserver} to receive a stream of responses
     * and store the messages.
     */
    protected static class PageResultObserver
            extends FutureStreamObserver<NamedCacheResponse, List<Any>>
        {
        public PageResultObserver()
            {
            super(new CompletableFuture<>(), new ArrayList<>(), PageResultObserver::onNext);
            }

        protected static List<Any> onNext(NamedCacheResponse response, List<Any> list)
            {
            list.add(response.getMessage());
            return list;
            }
        }

    // ----- inner class: ForwardingStreamObserver --------------------------

    /**
     * A {@link FutureStreamObserver} to receive a stream of responses
     * and store the messages.
     */
    protected static class ForwardingStreamObserver
            extends FutureStreamObserver<NamedCacheResponse, BiConsumer<ByteString, ByteString>>
        {
        public ForwardingStreamObserver(BiConsumer<ByteString, ByteString> consumer)
            {
            super(new CompletableFuture<>(), consumer, ForwardingStreamObserver::onNext);
            }

        protected static BiConsumer<ByteString, ByteString> onNext(NamedCacheResponse response,
                BiConsumer<ByteString, ByteString> consumer)
            {
            try
                {
                BinaryKeyAndValue keyAndValue = response.getMessage().unpack(BinaryKeyAndValue.class);
                consumer.accept(keyAndValue.getKey(), keyAndValue.getValue());
                return consumer;
                }
            catch (InvalidProtocolBufferException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        }

    // ----- inner class: ForwardingStreamObserver --------------------------

    /**
     * A {@link FutureStreamObserver} to receive a stream of responses
     * and store the messages.
     */
    protected static class MapStreamObserver
            extends FutureStreamObserver<NamedCacheResponse, Map<ByteString, ByteString>>
        {
        public MapStreamObserver()
            {
            super(new CompletableFuture<>(), new HashMap<>(), MapStreamObserver::onNext);
            }

        protected static Map<ByteString, ByteString> onNext(NamedCacheResponse response, Map<ByteString, ByteString> map)
            {
            try
                {
                BinaryKeyAndValue keyAndValue = response.getMessage().unpack(BinaryKeyAndValue.class);
                map.put(keyAndValue.getKey(), keyAndValue.getValue());
                return map;
                }
            catch (InvalidProtocolBufferException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        }

    // ----- inner class: EventObserver -------------------------------------

    /**
     * A {@link StreamObserver} to receive {@link NamedCacheResponse}
     * messages containing events.
     */
    protected class EventObserver
            implements StreamObserver<NamedCacheResponse>
        {
        /**
         * Create an {@link EventObserver}.
         *
         * @param dispatcher  the {@link EventDispatcher} to dispatch the received events
         */
        public EventObserver(EventDispatcher dispatcher)
            {
            m_dispatcher = dispatcher;
            }

        @Override
        public void onNext(NamedCacheResponse response)
            {
            if (m_dispatcher != null)
                {
                ResponseType type = response.getType();
                switch (type)
                    {
                    case MapEvent:
                        MapEventMessage event = unpackMessage(response, MapEventMessage.class);
                        CacheEvent.TransformationState transformState = CacheEvent.TransformationState.valueOf(event.getTransformationState().toString());
                        m_dispatcher.dispatch(event.getFilterIdsList(), event.getId(), event.getKey(), event.getOldValue(),
                                event.getNewValue(), event.getSynthetic(), event.getPriming(), transformState);
                        break;
                    case Destroyed:
                        m_dispatcher.onDestroy();
                        break;
                    case Truncated:
                        m_dispatcher.onTruncate();
                        break;
                    default:
                        Logger.err("Event observer received unexpected NamedCacheResponse type: " + type);
                    }
                }
            }

        @Override
        public void onError(Throwable t)
            {
            Logger.err("Event observer received an error", t);
            }

        @Override
        public void onCompleted()
            {
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link EventDispatcher} to dispatch events.
         */
        private final EventDispatcher m_dispatcher;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The lock to control access to state.
     */
    private final Lock f_lock = new ReentrantLock();

    /**
     * The cache identifier for this protocol.
     */
    private final int f_nCacheId;

    /**
     * The {@link EventObserver} to receive event responses.
     */
    private EventObserver m_eventObserver;
    }
