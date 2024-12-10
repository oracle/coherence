/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.v0;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.client.common.BaseNamedCacheClientChannel;
import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.oracle.coherence.grpc.messages.cache.v0.EntryResult;
import com.oracle.coherence.grpc.messages.cache.v0.GetAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapEventResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerErrorResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerSubscribedResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerUnsubscribedResponse;
import com.oracle.coherence.grpc.MaybeByteString;
import com.oracle.coherence.grpc.v0.Requests;
import com.oracle.coherence.grpc.SafeStreamObserver;
import com.oracle.coherence.grpc.client.common.AsyncNamedCacheClient;
import com.oracle.coherence.grpc.client.common.FutureStreamObserver;
import com.oracle.coherence.grpc.client.common.NamedCacheClientChannel;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.cache.CacheEvent.TransformationState;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.SimpleMapEntry;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The legacy version zero implementation of a {@link NamedCacheClientChannel}.
 */
@SuppressWarnings({"DuplicatedCode"})
public class NamedCacheClientChannel_V0
        extends BaseNamedCacheClientChannel
        implements NamedCacheClientChannel
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates an {@link NamedCacheClientChannel_V0} from the specified
     * {@link AsyncNamedCacheClient.Dependencies}.
     *
     * @param dependencies the {@link AsyncNamedCacheClient.Dependencies} to configure this
     *                     {@link NamedCacheClientChannel_V0}.
     * @param connection   the {@link GrpcConnection}
     */
    public NamedCacheClientChannel_V0(AsyncNamedCacheClient.Dependencies dependencies, GrpcConnection connection)
        {
        super(dependencies, connection);
        f_service = new NamedCacheGrpcClient(dependencies);
        }

    // ----- NamedCacheClientProtocol interface -----------------------------

    @Override
    public int getVersion()
        {
        return 0;
        }

    @Override
    public  CompletableFuture<BytesValue> aggregate(List<ByteString> keys, ByteString aggregator, long nDeadline)
        {
        return f_service.aggregate(Requests.aggregate(f_sScopeName, f_sName, f_sFormat, keys, aggregator), nDeadline)
                .toCompletableFuture();
        }

    @Override
    public  CompletableFuture<BytesValue> aggregate(ByteString filter, ByteString aggregator, long nDeadline)
        {
        return f_service.aggregate(Requests.aggregate(f_sScopeName, f_sName, f_sFormat, filter, aggregator), nDeadline)
                .toCompletableFuture();
        }

    @Override
    public CompletionStage<BytesValue> invoke(ByteString key, ByteString processor, long nDeadline)
        {
        return f_service.invoke(Requests.invoke(f_sScopeName, f_sName, f_sFormat, key, processor), nDeadline);
        }

    @Override
    public CompletableFuture<Map<ByteString, ByteString>> invokeAll(Collection<ByteString> serializedKeys, ByteString processor, long nDeadline)
        {
        CompletableFuture<Map<ByteString, ByteString>> future = new CompletableFuture<>();
        BiFunction<Entry, Map<ByteString, ByteString>, Map<ByteString, ByteString>> function = (e, m) ->
            {
            try
                {
                m.put(e.getKey(), e.getValue());
                return m;
                }
            catch (Throwable ex)
                {
                future.completeExceptionally(ex);
                }
            return null;
            };

        FutureStreamObserver<Entry, Map<ByteString, ByteString>> observer = new FutureStreamObserver<>(future, new HashMap<>(), function);
        InvokeAllRequest request  = Requests.invokeAll(f_sScopeName, f_sName, f_sFormat, serializedKeys, processor);
        invokeAll(request, observer, nDeadline);
        return future;
        }

    @Override
    public CompletableFuture<Map<ByteString, ByteString>> invokeAll(ByteString filter, ByteString processor)
        {
        CompletableFuture<Map<ByteString, ByteString>> future = new CompletableFuture<>();
        BiFunction<Entry, Map<ByteString, ByteString>, Map<ByteString, ByteString>> function = (e, m) ->
            {
            try
                {
                m.put(e.getKey(), e.getValue());
                return m;
                }
            catch (Throwable ex)
                {
                future.completeExceptionally(ex);
                }
            return null;
            };
        FutureStreamObserver<Entry, Map<ByteString, ByteString>> observer = new FutureStreamObserver<>(future, new HashMap<>(), function);
        invokeAll(Requests.invokeAll(f_sScopeName, f_sName, f_sFormat, filter, processor), observer);
        return future;
        }

    @Override
    public CompletableFuture<Void> invokeAll(Collection<ByteString> colKeys, ByteString processor,
                                             Consumer<Map.Entry< ByteString, ByteString>> callback)
        {
        CompletableFuture<Void> future = new CompletableFuture<>();
        BiFunction<Entry, Void, Void> function = (e, v) ->
            {
            try
                {
                callback.accept(new SimpleMapEntry<>(e.getKey(), e.getValue()));
                }
            catch (Throwable ex)
                {
                future.completeExceptionally(ex);
                }
            return null;
            };
        FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
        invokeAll(Requests.invokeAll(f_sScopeName, f_sName, f_sFormat, colKeys, processor),
                observer);
        return future;
        }

    @Override
    public CompletableFuture<Void> invokeAll(Collection<ByteString> colKeys, ByteString processor, BiConsumer<ByteString, ByteString> callback)
        {
        CompletableFuture<Void> future = new CompletableFuture<>();
        BiFunction<Entry, Void, Void> function = (e, v) ->
            {
            try
                {
                callback.accept(e.getKey(), e.getValue());
                }
            catch (Throwable ex)
                {
                future.completeExceptionally(ex);
                }
            return null;
            };
        FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
        invokeAll(Requests.invokeAll(f_sScopeName, f_sName, f_sFormat, colKeys, processor),
                observer);
        return future;
        }

    @Override
    public CompletableFuture<Void> invokeAll(ByteString filter, ByteString processor, Consumer<Map.Entry<ByteString, ByteString>> callback)
        {
        CompletableFuture<Void> future = new CompletableFuture<>();
        BiFunction<Entry, Void, Void> function = (e, v) ->
            {
            try
                {
                callback.accept(new SimpleMapEntry<>(e.getKey(), e.getValue()));
                }
            catch (Throwable ex)
                {
                future.completeExceptionally(ex);
                }
            return null;
            };
        FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
        invokeAll(Requests.invokeAll(f_sScopeName, f_sName, f_sFormat, filter, processor), observer);
        return future;
        }

    @Override
    public CompletableFuture<Void> invokeAll(ByteString filter, ByteString processor, BiConsumer<ByteString, ByteString> callback)
        {
        CompletableFuture<Void> future = new CompletableFuture<>();
        BiFunction<Entry, Void, Void> function = (e, v) ->
            {
            try
                {
                callback.accept(e.getKey(), e.getValue());
                }
            catch (Throwable ex)
                {
                future.completeExceptionally(ex);
                }
            return VOID;
            };
        FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
        invokeAll(Requests.invokeAll(f_sScopeName, f_sName, f_sFormat, filter, processor),
                observer);
        return future;
        }

    @Override
    public CompletableFuture<Void> clear()
        {
        return f_service.clear(Requests.clear(f_sScopeName, f_sName)).thenApply(e -> VOID).toCompletableFuture();
        }

    @Override
    public CompletionStage<BoolValue> isEmpty()
        {
        return f_service.isEmpty(Requests.isEmpty(f_sScopeName, f_sName));
        }

    @Override
    public CompletionStage<BytesValue> putIfAbsent(ByteString key, ByteString value)
        {
        return f_service.putIfAbsent(Requests.putIfAbsent(f_sScopeName, f_sName, f_sFormat, key, value));
        }

    @Override
    public CompletionStage<BytesValue> replace(ByteString key, ByteString value)
        {
        return f_service.replace(Requests.replace(f_sScopeName, f_sName, f_sFormat, key, value));
        }

    @Override
    public CompletionStage<BoolValue> replaceMapping(ByteString key, ByteString oldValue, ByteString newValue)
        {
        return f_service.replaceMapping(Requests.replace(f_sScopeName, f_sName, f_sFormat, key, oldValue, newValue));
        }

    @Override
    public CompletionStage<Int32Value> size()
        {
        return f_service.size(Requests.size(f_sScopeName, f_sName));
        }

    @Override
    public Stream<Map.Entry<ByteString, ByteString>> getAll(Iterable<ByteString> keys)
        {
        GetAllRequest request = Requests.getAll(f_sScopeName, f_sName, f_sFormat, keys);
        return f_service.getAll(request)
                .map(e -> new SimpleMapEntry<>(e.getKey(), e.getValue()));
        }

    @Override
    public CompletionStage<MaybeByteString> get(ByteString key)
        {
        return f_service.get(Requests.get(f_sScopeName, f_sName, f_sFormat, key))
                .thenApply(v -> v.getPresent() ? MaybeByteString.ofNullable(v.getValue()) : MaybeByteString.empty());
        }

    /**
     * Helper method to perform invokeAll operations.
     *
     * @param request  the {@link InvokeAllRequest}
     * @param observer the {@link StreamObserver}
     */
    public void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        invokeAll(request, observer, PriorityTask.TIMEOUT_DEFAULT);
        }

    /**
     * Helper method to perform invokeAll operations.
     *
     * @param request  the {@link InvokeAllRequest}
     * @param observer the {@link StreamObserver}
     */
    protected void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer, long nDeadline)
        {
        f_service.invokeAll(request, observer, nDeadline);
        }

    @Override
    public CompletionStage<BoolValue> isReady()
        {
        return f_service.isReady(Requests.ready(f_sScopeName,f_sName));
        }

    @Override
    public CompletionStage<BytesValue> put(ByteString key, ByteString value, long ttl)
        {
        return f_service.put(Requests.put(f_sScopeName, f_sName, f_sFormat, key, value, ttl));
        }

    @Override
    public CompletableFuture<Empty> putAll(Map<ByteString, ByteString> map, long cMillis)
        {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<ByteString, ByteString> entry : map.entrySet())
            {
            entries.add(Entry.newBuilder()
                    .setKey(entry.getKey())
                    .setValue(entry.getValue())
                    .build());
            }
        return f_service.putAll(Requests.putAll(f_sScopeName, f_sName, f_sFormat, entries, cMillis)).toCompletableFuture();
        }

    @Override
    public CompletionStage<Empty> removeIndex(ByteString extractor)
        {
        return f_service.removeIndex(Requests.removeIndex(f_sScopeName, f_sName, f_sFormat, extractor));
        }

    @Override
    public CompletionStage<BytesValue> remove(ByteString key)
        {
        return f_service.remove(Requests.remove(f_sScopeName, f_sName, f_sFormat, key));
        }

    @Override
    public CompletionStage<BoolValue> remove(ByteString key, ByteString value)
        {
        return f_service.removeMapping(Requests.remove(f_sScopeName, f_sName, f_sFormat, key, value));
        }

    @Override
    public CompletableFuture<Void> removeMapListener(ByteString key, boolean fPriming)
        {
        String uid = "";
        try
            {
            MapListenerRequest request = Requests.removeKeyMapListener(f_sScopeName, f_sName,
                    f_sFormat, key, fPriming, ByteString.EMPTY);

            uid = request.getUid();
            return m_evtResponseObserver.send(request);
            }
        catch (Throwable t)
            {
            m_evtResponseObserver.removeAndComplete(uid, t);
            return CompletableFuture.failedFuture(t);
            }
        }

    @Override
    public CompletableFuture<Void> removeMapListener(ByteString filterBytes,
                                                     long nFilterId, ByteString triggerBytes)
        {
        String uid = "";
        CompletableFuture<Void> future = new CompletableFuture<>();
        try
            {
            MapListenerRequest request = Requests
                    .removeFilterMapListener(f_sScopeName, f_sName, f_sFormat, filterBytes, nFilterId,
                                             false, false, triggerBytes);
            uid = request.getUid();
            future = m_evtResponseObserver.send(request);
            }
        catch (Throwable t)
            {
            m_evtResponseObserver.removeAndComplete(uid, t);
            future.completeExceptionally(t);
            }
        return future;
        }

    @Override
    public CompletionStage<Empty> truncate()
        {
        return f_service.truncate(Requests.truncate(f_sScopeName, f_sName));
        }

    @Override
    public CompletionStage<BoolValue> containsKey(ByteString key)
        {
        return f_service.containsKey(Requests.containsKey(f_sScopeName, f_sName, f_sFormat, key));
        }

    @Override
    public CompletionStage<BoolValue> containsValue(ByteString value)
        {
        return f_service.containsValue(Requests.containsValue(f_sScopeName, f_sName, f_sFormat, value));
        }

    @Override
    public void setEventDispatcher(EventDispatcher dispatcher)
        {
        m_evtResponseObserver = new EventStreamObserver(dispatcher);
        // create a future to allow us to wait for the init request to complete
        CompletableFuture<Void> future = m_evtResponseObserver.whenSubscribed().toCompletableFuture();
        // handle subscription completion errors
        future.handle((v, err) ->
            {
            if (err != null)
                {
                // close the channel if subscription failed
                m_evtResponseObserver.onCompleted();
                }
            return null;
            });

        // Wait for the init request to complete
        // The events bi-di channel has no deadline, but we need to ensure the initial
        // subscription completes in a suitable time, so we use the deadline here
        long cDeadlineMillis = f_dependencies.getDeadline();
        try
            {
            future.get(cDeadlineMillis, TimeUnit.MILLISECONDS);
            }
        catch (InterruptedException | TimeoutException e)
            {
            throw new RequestTimeoutException("Timed out waiting for event subscription after "
                    + cDeadlineMillis + " ms", e);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public Stream<BytesValue> getKeysPage(ByteString cookie)
        {
        return f_service.nextKeySetPage(Requests.page(f_sScopeName, f_sName, f_sFormat, cookie));
        }

    @Override
    public EntrySetPage getEntriesPage(ByteString cookie)
        {
        LinkedList<EntryResult> list = f_service.nextEntrySetPage(Requests.page(f_sScopeName, f_sName, f_sFormat, cookie))
                .collect(Collectors.toCollection(LinkedList::new));

        if (list.isEmpty())
            {
            return new EntrySetPage(null, List.of());
            }

        EntryResult first = list.poll();
        return new EntrySetPage(first.getCookie(), ConverterCollections.getList(list, e -> new SimpleMapEntry<>(e.getKey(), e.getValue()), e -> null));
        }

    @Override
    public CompletionStage<BoolValue> containsEntry(ByteString key, ByteString value)
        {
        return f_service.containsEntry(Requests.containsEntry(f_sScopeName, f_sName, f_sFormat, key, value));
        }

    @Override
    public CompletableFuture<Void> destroy()
        {
        // close the events bidirectional channel and any event listeners
        if (m_evtResponseObserver != null)
            {
            m_evtResponseObserver.onCompleted();
            }
        return f_service.destroy(Requests.destroy(f_sScopeName, f_sName)).thenApply(e -> VOID).toCompletableFuture();
        }

    @Override
    public CompletableFuture<Void> addIndex(ByteString extractor, boolean fOrdered, ByteString comparator)
        {
        if (comparator == null)
            {
            return f_service.addIndex(Requests.addIndex(f_sScopeName, f_sName, f_sFormat, extractor, fOrdered))
                    .thenApply(e -> VOID).toCompletableFuture();
            }
        else
            {
            return f_service.addIndex(Requests.addIndex(f_sScopeName, f_sName, f_sFormat, extractor, fOrdered, comparator))
                    .thenApply(e -> VOID).toCompletableFuture();
            }
        }

    @Override
    public CompletableFuture<Void> addMapListener(ByteString key, boolean fLite, boolean fPriming, boolean fSynchronous)
        {
        String uid = "";
        try
            {
            MapListenerRequest request = Requests
                    .addKeyMapListener(f_sScopeName, f_sName, f_sFormat, key, fLite, fPriming, ByteString.EMPTY);
            uid = request.getUid();
            return m_evtResponseObserver.send(request);
            }
        catch (Throwable t)
            {
            m_evtResponseObserver.removeAndComplete(uid, t);
            return CompletableFuture.failedFuture(t);
            }
        }

    @Override
    public CompletableFuture<Void> addMapListener(ByteString filterBytes,
                                                  long nFilterId, boolean fLite, ByteString triggerBytes, boolean fSynchronous)
        {
        String uid = "";
        CompletableFuture<Void> future;
        try
            {
            MapListenerRequest request = Requests
                    .addFilterMapListener(f_sScopeName, f_sName, f_sFormat, filterBytes, nFilterId, fLite, false, triggerBytes);
            uid = request.getUid();
            future = m_evtResponseObserver.send(request);
            }
        catch (Throwable t)
            {
            m_evtResponseObserver.removeAndComplete(uid, t);
            future = CompletableFuture.failedFuture(t);
            }
        return future;
        }

    // ----- EventStreamObserver -------------------------------------------

    /**
     * A {@code EventStreamObserver} that processes {@link MapListenerResponse}s.
     */
    @SuppressWarnings("EnhancedSwitchMigration")
    protected class EventStreamObserver
            implements StreamObserver<MapListenerResponse>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new EventStreamObserver
         */
        protected EventStreamObserver(EventDispatcher dispatcher)
            {
            f_dispatcher = dispatcher;
            f_sUid       = UUID.randomUUID().toString();
            f_future     = new CompletableFuture<>();
            StreamObserver<MapListenerRequest> observer = f_service.events(this);
            m_evtRequestObserver = (SafeStreamObserver<MapListenerRequest>) SafeStreamObserver.ensureSafeObserver(observer);
            m_evtRequestObserver.whenDone().thenAccept(v -> f_mapFuture.values().forEach(f -> f.complete(null)));

            long nDeadline = f_dependencies.getDeadline();

            // initialise the bidirectional stream so that this client will receive
            // destroy and truncate events
            MapListenerRequest request = MapListenerRequest.newBuilder()
                    .setScope(f_sScopeName)
                    .setCache(f_sName)
                    .setUid(f_sUid)
                    .setSubscribe(true)
                    .setFormat(f_sFormat)
                    .setType(MapListenerRequest.RequestType.INIT)
                    .setHeartbeatMillis(f_dependencies.getHeartbeatMillis())
                    .build();

            observer.onNext(request);
            }

        // ----- public methods ---------------------------------------------

        /**
         * Subscription {@link CompletionStage}.
         *
         * @return a {@link CompletionStage} returning {@link Void}
         */
        public CompletionStage<Void> whenSubscribed()
            {
            return f_future;
            }

        // ----- StreamObserver interface -----------------------------------

        @Override
        public void onNext(MapListenerResponse response)
            {
            switch (response.getResponseTypeCase())
                {
                case SUBSCRIBED:
                    onSubscribed(response);
                    break;
                case UNSUBSCRIBED:
                    onUnsubscribed(response);
                    break;
                case EVENT:
                    MapEventResponse    event          = response.getEvent();
                    TransformationState transformState = TransformationState.valueOf(event.getTransformationState().toString());
                    f_dispatcher.dispatch(event.getFilterIdsList(), event.getId(), event.getKey(), event.getOldValue(),
                            event.getNewValue(), event.getSynthetic(), event.getPriming(), transformState);
                    break;
                case ERROR:
                    onError(response);
                    break;
                case DESTROYED:
                    onDestroyed(response);
                    break;
                case TRUNCATED:
                    onTruncated(response);
                    break;
                case HEARTBEAT:
                    break;
                case RESPONSETYPE_NOT_SET:
                    Logger.info("Received unexpected event without a response type!");
                    break;
                default:
                    Logger.info("Received unexpected event " + response.getEvent());
                }
            }

        @Override
        public void onError(Throwable t)
            {
            f_lock.lock();
            try
                {
                m_fDone = true;
                if (!f_future.isDone())
                    {
                    f_future.completeExceptionally(t);
                    }
                f_mapFuture.values().forEach(f -> f.complete(null));
                }
            finally
                {
                f_lock.unlock();
                }
            }

        @Override
        public void onCompleted()
            {
            f_lock.lock();
            try
                {
                m_fDone = true;
                if (!f_future.isDone())
                    {
                    f_future.completeExceptionally(
                            new IllegalStateException("Event observer completed without subscription"));
                    }
                f_mapFuture.values().forEach(f -> f.complete(null));
                }
            finally
                {
                f_lock.unlock();
                }
            }

        // ----- helper methods ---------------------------------------------

        private void onSubscribed(MapListenerResponse response)
            {
            MapListenerSubscribedResponse subscribed  = response.getSubscribed();
            String                        responseUid = subscribed.getUid();
            if (f_sUid.equals(responseUid))
                {
                f_future.complete(VOID);
                }
            else
                {
                CompletableFuture<Void> future = f_mapFuture.remove(responseUid);
                if (future != null)
                    {
                    future.complete(VOID);
                    }
                f_dispatcher.incrementListeners();
                }
            }

        private void onUnsubscribed(MapListenerResponse response)
            {
            MapListenerUnsubscribedResponse unsubscribed = response.getUnsubscribed();
            CompletableFuture<Void> future = f_mapFuture.remove(unsubscribed.getUid());
            if (future != null)
                {
                future.complete(VOID);
                }
            f_dispatcher.decrementListeners();
            }

        private void onDestroyed(MapListenerResponse response)
            {
            if (response.getDestroyed().getCache().equals(f_sName))
                {
                f_dispatcher.onDestroy();
                }
            }

        private void onTruncated(MapListenerResponse response)
            {
            if (response.getTruncated().getCache().equals(f_sName))
                {
                f_dispatcher.onTruncate();
                }
            }

        private void onError(MapListenerResponse response)
            {
            MapListenerErrorResponse error       = response.getError();
            String                   responseUid = error.getUid();
            if (f_sUid.equals(responseUid))
                {
                f_future.completeExceptionally(new RuntimeException(error.getMessage()));
                }
            else
                {
                CompletableFuture<Void> future = f_mapFuture.remove(responseUid);
                if (future != null)
                    {
                    future.completeExceptionally(new RuntimeException(error.getMessage()));
                    }
                }
            }

        public CompletableFuture<Void> send(MapListenerRequest request)
            {
            if (m_fDone)
                {
                return CompletableFuture.completedFuture(null);
                }

            f_lock.lock();
            try
                {
                if (m_fDone)
                    {
                    return CompletableFuture.completedFuture(null);
                    }
                CompletableFuture<Void> future = new CompletableFuture<>();
                f_mapFuture.put(request.getUid(), future);
                m_evtRequestObserver.onNext(request);
                return future;
                }
            finally
                {
                f_lock.unlock();
                }
            }

        public void removeAndComplete(String uid, Throwable t)
            {
            CompletableFuture<Void> future = f_mapFuture.remove(uid);
            if (future != null && !future.isDone())
                {
                if (t == null)
                    {
                    future.complete(null);
                    }
                else
                    {
                    future.completeExceptionally(t);
                    }
                }
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "EventStreamObserver(" +
                    "cacheName='" + f_sName + '\'' +
                    ", uid='" + f_sUid + '\'' +
                    ')';
            }

        // ----- data members -----------------------------------------------

        private final EventDispatcher f_dispatcher;

        /**
         * The event ID.
         */
        protected final String f_sUid;

        /**
         * The {@link CompletableFuture} to notify
         */
        protected final CompletableFuture<Void> f_future;

        /**
         * A flag indicating that this observer is closed.
         */
        protected volatile boolean m_fDone;

        /**
         * The lock to control sending messages and closing the channel.
         */
        protected final Lock f_lock = new ReentrantLock();

        /**
         * The client channel for events observer.
         */
        private final SafeStreamObserver<MapListenerRequest> m_evtRequestObserver;

        /**
         * The map of event listener request futures keyed by request id.
         */
        protected final Map<String, CompletableFuture<Void>> f_mapFuture = new ConcurrentHashMap<>();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link NamedCacheGrpcClient} to delegate calls.
     */
    protected final NamedCacheGrpcClient f_service;

    /**
     * The event response observer.
     */
    protected EventStreamObserver m_evtResponseObserver;
    }
