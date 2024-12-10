/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_1;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.messages.cache.v1.MapEventMessage;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.oracle.coherence.grpc.messages.cache.v1.ResponseType;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.tangosol.io.Serializer;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.WrapperObservableMap;
import grpc.proxy.TestStreamObserver;
import io.reactivex.rxjava3.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class ListenableStreamObserver
        extends TestStreamObserver<ProxyResponse>
    {

    @Override
    public void onNext(@NonNull ProxyResponse response)
        {
        super.onNext(response);
        lock.lock();
        try
            {
            for (Consumer<ProxyResponse> listener : listeners)
                {
                try
                    {
                    listener.accept(response);
                    }
                catch (Throwable e)
                    {
                    Logger.err(e);
                    }
                }
            }
        finally
            {
            lock.unlock();
            }
        }

    public List<ProxyResponse> safeValues()
        {
        lock.lock();
        try
            {
            return new ArrayList<>(values);
            }
        finally
            {
            lock.unlock();
            }
        }

    public <K, V> void addListener(MapListener<K, V> listener, Serializer serializer)
        {
        addListener(new WrapperObservableMap<K, V>(Map.of()), listener, serializer);
        }

    public <K, V> void addListener(ObservableMap<K, V> map, MapListener<K, V> listener, Serializer serializer)
        {
        addListener(new MapListenerConsumer<>(map, listener, serializer));
        }

    public <K, V> void removeListener(MapListener<K, V> listener, Serializer serializer)
        {
        removeListener(new WrapperObservableMap<K, V>(Map.of()), listener, serializer);
        }

    public <K, V> void removeListener(ObservableMap<K, V> map, MapListener<K, V> listener, Serializer serializer)
        {
        removeListener(new MapListenerConsumer<>(map, listener, serializer));
        }

    public void addListener(Consumer<ProxyResponse> listener)
        {
        lock.lock();
        try
            {
            listeners.add(listener);
            }
        finally
            {
            lock.unlock();
            }
        }

    public void removeListener(Consumer<ProxyResponse> listener)
        {
        lock.lock();
        try
            {
            listeners.remove(listener);
            }
        finally
            {
            lock.unlock();
            }
        }

    public static class MapListenerConsumer<K, V>
            implements Consumer<ProxyResponse>
        {
        public MapListenerConsumer(ObservableMap<K, V> map, MapListener<K, V> mapListener, Serializer serializer)
            {
            this.map         = map;
            this.mapListener = mapListener;
            this.serializer  = serializer;
            }

        @Override
        public void accept(ProxyResponse response)
            {
            try
                {
                NamedCacheResponse cacheResponse = response.getMessage().unpack(NamedCacheResponse.class);
                if (cacheResponse.getType() == ResponseType.MapEvent)
                    {
                    try
                        {
                        MapEventMessage event = cacheResponse.getMessage().unpack(MapEventMessage.class);
                        CacheEvent.TransformationState transformState = CacheEvent.TransformationState.valueOf(event.getTransformationState().toString());

                        K oKey = BinaryHelper.fromByteString(event.getKey(), serializer);
                        V oOldValue = BinaryHelper.fromByteString(event.getOldValue(), serializer);
                        V oNewValue = BinaryHelper.fromByteString(event.getNewValue(), serializer);

                        CacheEvent<K, V> cacheEvent = new CacheEvent<>(map, event.getId(), oKey, oOldValue, oNewValue, event.getSynthetic(), transformState, event.getPriming(), event.getExpired());
                        if (cacheEvent.isInsert())
                            {
                            mapListener.entryInserted(cacheEvent);
                            }
                        else if (cacheEvent.isUpdate())
                            {
                            mapListener.entryUpdated(cacheEvent);
                            }
                        else if (cacheEvent.isDelete())
                            {
                            mapListener.entryDeleted(cacheEvent);
                            }
                        }
                    catch (InvalidProtocolBufferException e)
                        {
                        Logger.err(e);
                        }
                    }
                }
            catch (InvalidProtocolBufferException e)
                {
                // ignored
                }
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapListenerConsumer<?, ?> that = (MapListenerConsumer<?, ?>) o;
            return Objects.equals(mapListener, that.mapListener);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(mapListener);
            }

        private final ObservableMap<K, V> map;

        private final MapListener<K, V> mapListener;

        private final Serializer serializer;
        }

    private final Lock lock = new ReentrantLock();

    private final List<Consumer<ProxyResponse>> listeners = new ArrayList<>();
    }
