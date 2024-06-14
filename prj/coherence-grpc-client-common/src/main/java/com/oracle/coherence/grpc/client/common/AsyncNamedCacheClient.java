/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheEvent.TransformationState;
import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Base;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.Listeners;
import com.tangosol.util.LongArray;
import com.tangosol.util.LongArray.Iterator;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.PagedIterator;
import com.tangosol.util.SparseArray;
import com.tangosol.util.SynchronousListener;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.AlwaysFilter;

import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tangosol.internal.net.grpc.RemoteGrpcCacheServiceDependencies.NO_EVENTS_HEARTBEAT;

/**
 * A base implementation of an {@link AsyncNamedCacheClient}.
 *
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 */
@SuppressWarnings({"DuplicatedCode", "PatternVariableCanBeUsed"})
public class AsyncNamedCacheClient<K, V>
        extends BaseGrpcClient<V, NamedCacheClientChannel>
        implements AsyncNamedCache<K, V>, NamedCacheClientChannel.EventDispatcher
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates an {@link AsyncNamedCacheClient} from the specified
     * {@link Dependencies}.
     *
     * @param dependencies  the {@link AsyncNamedCacheClient.Dependencies} to configure this
     *                      {@link AsyncNamedCacheClient}.
     * @param client        the {@link NamedCacheClientChannel} to use to send requests
     */
    public AsyncNamedCacheClient(Dependencies dependencies, NamedCacheClientChannel client)
        {
        super(dependencies, client);
        f_synchronousCache          = new NamedCacheClient<>(this);
        f_listDeactivationListeners = new ArrayList<>();
        m_listenerSupport           = new MapListenerSupport();
        m_aEvtFilter                = new SparseArray<>();

        client.setEventDispatcher(this);
        }

    // ----- AsyncNamedCache interface --------------------------------------

    @Override
   public NamedCache<K, V> getNamedCache()
       {
       return getNamedCacheClient();
       }

    // ----- AsyncNamedMap interface ----------------------------------------

    @Override
    public NamedMap<K, V> getNamedMap()
       {
       return getNamedCacheClient();
       }

    @Override
    public <R> CompletableFuture<R> aggregate(Collection<? extends K> colKeys, InvocableMap.EntryAggregator<? super K, ? super V, R> entryAggregator)
        {
        return executeIfActive(() ->
            {
            try
                {
                List<ByteString> keys = colKeys.stream()
                        .map(this::toKeyByteString)
                        .collect(Collectors.toList());

                long nDeadline = PriorityTask.TIMEOUT_DEFAULT;
                if (entryAggregator instanceof PriorityTask)
                    {
                    nDeadline = ((PriorityTask) entryAggregator).getRequestTimeoutMillis();
                    }

                return f_client.aggregate(keys, toByteString(entryAggregator), nDeadline)
                        .thenApply(this::fromBytesValue);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<R> aggregate(Filter<?> filter, InvocableMap.EntryAggregator<? super K, ? super V, R> entryAggregator)
        {
        return executeIfActive(() ->
            {
            try
                {
                long nDeadline = PriorityTask.TIMEOUT_DEFAULT;
                if (entryAggregator instanceof PriorityTask)
                    {
                    nDeadline = ((PriorityTask) entryAggregator).getRequestTimeoutMillis();
                    }

                return f_client.aggregate(toByteString(filter), toByteString(entryAggregator), nDeadline)
                        .thenApply(this::fromBytesValue);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> invoke(K k, InvocableMap.EntryProcessor<K, V, R> entryProcessor)
        {
        return executeIfActive(() ->
            {
            try
                {
                long nDeadline = PriorityTask.TIMEOUT_DEFAULT;
                if (entryProcessor instanceof PriorityTask)
                    {
                    nDeadline = ((PriorityTask) entryProcessor).getRequestTimeoutMillis();
                    }
                return f_client.invoke(toKeyByteString(k), toByteString(entryProcessor), nDeadline)
                        .thenApplyAsync(this::valueFromBytesValue)
                        .thenApply(r -> (R) r)
                        .toCompletableFuture();
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Map<K, R>> invokeAll(Collection<? extends K> colKeys,
                                                      InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return executeIfActive(() ->
            {
            try
                {
                long nDeadline = PriorityTask.TIMEOUT_DEFAULT;
                if (processor instanceof PriorityTask)
                    {
                    nDeadline = ((PriorityTask) processor).getRequestTimeoutMillis();
                    }

                Collection<ByteString> serializedKeys = colKeys.stream()
                        .map(this::toKeyByteString)
                        .collect(Collectors.toList());

                return f_client.invokeAll(serializedKeys, toByteString(processor), nDeadline)
                        .thenApply(this::toMap);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Map<K, R>> invokeAll(Filter<?> filter, InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return executeIfActive(() ->
            {
            try
                {
                return f_client.invokeAll(toByteString(filter), toByteString(processor))
                        .thenApply(this::toMap);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(Collection<? extends K> colKeys,
                                                 InvocableMap.EntryProcessor<K, V, R> processor,
                                                 Consumer<? super Map.Entry<? extends K, ? extends R>> callback)
        {
        return executeIfActive(() ->
            {
            try
                {
                Collection<ByteString> serializedKeys = colKeys.stream()
                        .map(this::toKeyByteString)
                        .collect(Collectors.toList());

                Consumer<Map.Entry<ByteString, ByteString>> consumer = entry -> callback.accept(toMapEntry(entry));

                return f_client.invokeAll(serializedKeys, toByteString(processor), consumer);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(Filter<?> filter,
                                                 InvocableMap.EntryProcessor<K, V, R> processor,
                                                 Consumer<? super Map.Entry<? extends K, ? extends R>> callback)
        {
        return executeIfActive(() ->
            {
            try
                {
                Consumer<Map.Entry<ByteString, ByteString>> consumer = entry -> callback.accept(toMapEntry(entry));

                return f_client.invokeAll(toByteString(filter), toByteString(processor), consumer);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public CompletableFuture<Void> clear()
        {
        return executeIfActive(() ->
            {
            try
                {
                return f_client.clear();
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public CompletableFuture<Boolean> containsKey(K key)
        {
        return executeIfActive(() -> containsKeyInternal(key));
        }

    @Override
    public CompletableFuture<Set<Map.Entry<K, V>>> entrySet()
        {
        return executeIfActive(() -> CompletableFuture.completedFuture(new RemoteEntrySet<>(this)));
        }

    @Override
    public CompletableFuture<V> get(K key)
        {
        return executeIfActive(() -> getInternal(key, null));
        }

    @Override
    public CompletableFuture<Map<K, V>> getAll(Collection<? extends K> colKeys)
        {
        return executeIfActive(() ->
            {
            if (colKeys.isEmpty())
                {
                return CompletableFuture.completedFuture(new HashMap<>());
                }
            else
                {
                return CompletableFuture.supplyAsync(() -> getAllInternalAsMap(colKeys));
                }
            });
        }

    @Override
    public CompletableFuture<V> getOrDefault(K key, V defaultValue)
        {
        return executeIfActive(() -> getInternal(key, defaultValue));
        }

    @Override
    public <R> CompletableFuture<Map<K, R>> invokeAll(InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return executeIfActive(() ->
            {
            try
                {
                return f_client.invokeAll(toByteString(AlwaysFilter.INSTANCE()), toByteString(processor))
                        .thenApply(this::toMap);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(InvocableMap.EntryProcessor<K, V, R> processor,
                                                 Consumer<? super Map.Entry<? extends K, ? extends R>> callback)
        {
        return executeIfActive(() ->
            {
            try
                {
                Consumer<Map.Entry<ByteString, ByteString>> consumer = e -> callback.accept(toMapEntry(e));
                return f_client.invokeAll(toByteString(AlwaysFilter.INSTANCE()), toByteString(processor), consumer);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }


    @Override
    public <R> CompletableFuture<Void> invokeAll(InvocableMap.EntryProcessor<K, V, R> processor,
                                                 BiConsumer<? super K, ? super R> callback)
        {
        return executeIfActive(() ->
            {
            try
                {
                return f_client.invokeAll(toByteString(AlwaysFilter.INSTANCE()), toByteString(processor),
                        (k, v) -> callback.accept(fromByteString(k), fromByteString(v)));
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(Collection<? extends K> colKeys,
                                                 InvocableMap.EntryProcessor<K, V, R> processor,
                                                 BiConsumer<? super K, ? super R> callback)
        {
        return executeIfActive(() ->
            {
            try
                {
                Collection<ByteString> keys = colKeys.stream()
                        .map(this::toKeyByteString)
                        .collect(Collectors.toList());

                return f_client.invokeAll(keys, toByteString(processor),
                        (k, v) -> callback.accept(fromByteString(k), fromByteString(v)));
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(Filter<?> filter,
                                                 InvocableMap.EntryProcessor<K, V, R> processor,
                                                 BiConsumer<? super K, ? super R> callback)
        {
        return executeIfActive(() ->
        {
            try
                {
                return f_client.invokeAll(toByteString(filter), toByteString(processor),
                        (k, v) -> callback.accept(fromByteString(k), fromByteString(v)));
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
        });
        }

    @Override
    public CompletableFuture<Boolean> isEmpty()
        {
        return executeIfActive(() -> f_client.isEmpty()
                .thenApply(BoolValue::getValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Set<K>> keySet()
        {
        return executeIfActive(() -> CompletableFuture.completedFuture(new RemoteKeySet<>(this)));
        }

    @Override
    public CompletableFuture<Void> put(K key, V value)
        {
        return putInternal(key, value, CacheMap.EXPIRY_DEFAULT).thenApply(v -> VOID);
        }

    @Override
    public CompletableFuture<Void> put(K key, V value, long ttl)
        {
        return putInternal(key, value, ttl).thenApply(v -> VOID);
        }

    @Override
    public CompletableFuture<V> putIfAbsent(K key, V value)
        {
        return executeIfActive(() -> f_client.putIfAbsent(toKeyByteString(key), toByteString(value))
                .thenApplyAsync(this::valueFromBytesValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Void> putAll(Map<? extends K, ? extends V> map)
        {
        return putAllInternal(map, CacheMap.EXPIRY_DEFAULT).thenApply(v -> VOID);
        }

    @Override
    public CompletableFuture<Void> putAll(Map<? extends K, ? extends V> map, long cMillis)
        {
        return putAllInternal(map, cMillis).thenApply(v -> VOID);
        }

    @Override
    public CompletableFuture<V> remove(K key)
        {
        return removeInternal(key);
        }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value)
        {
        return removeInternal(key, value);
        }

    @Override
    public CompletableFuture<V> replace(K key, V value)
        {
        return executeIfActive(() -> f_client.replace(toKeyByteString(key), toByteString(value))
                .thenApplyAsync(this::valueFromBytesValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue)
        {
        return executeIfActive(() -> f_client.replaceMapping(toKeyByteString(key), toByteString(oldValue), toByteString(newValue))
                .thenApplyAsync(BoolValue::getValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Integer> size()
        {
        return executeIfActive(() -> f_client.size()
                .thenApply(Int32Value::getValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Collection<V>> values()
        {
        return executeIfActive(() -> CompletableFuture.completedFuture(new RemoteValues<>(this)));
        }

    @Override
    public CompletableFuture<Collection<V>> values(Filter<?> filter, Comparator<? super V> comparator)
        {
        return executeIfActive(() -> valuesInternal(filter, comparator));
        }

    // ----- AsyncNamedCacheClient methods ----------------------------------
    
    /**
     * Helper method for {@link #containsKey(Object)} invocations.
     *
     * @param oKey  the key to check
     *
     * @return a {@link CompletableFuture} returning {@code true} if the cache contains the given key
     */
    public CompletableFuture<Boolean> containsKeyInternal(Object oKey)
        {
        return executeIfActive(() -> f_client.containsKey(toKeyByteString(oKey))
                .thenApplyAsync(BoolValue::getValue)
                .toCompletableFuture());
        }

    /**
     * Create a {@link PagedIterator.Advancer} to use to iterate over the cache contents.
     *
     * @return a {@link PagedIterator.Advancer} to use to iterate over the cache contents
     */
    public PagedIterator.Advancer createEntryAdvancer()
        {
        return new EntryAdvancer<>(this);
        }

    /**
     * Helper method to obtain a {@link Map} of the keys/values found within the cache matching
     * the provided {@link Collection} of keys.
     *
     * @param colKeys  the keys to look up
     *
     * @return a {@link Map} containing keys/values for the keys found within the cache
     */
    public Map<K, V> getAllInternalAsMap(Collection<? extends K> colKeys)
        {
        assertActive();
        if (colKeys.isEmpty())
            {
            return Map.of();
            }
        return f_client.getAll(ConverterCollections.getCollection(colKeys, this::toByteString, this::fromByteString))
                .collect(Collectors.toMap(e -> fromByteString(e.getKey()), e -> fromByteString(e.getValue())));
        }

    /**
     * Helper method for getting a value from the cache.
     *
     * @param key           the key
     * @param defaultValue  the value to return if this cache doesn't contain the provided key
     *
     * @return the value within the Cache, or {@code defaultValue}
     */
    public CompletableFuture<V> getInternal(Object key, V defaultValue)
        {
        return f_client.get(toKeyByteString(key))
                .thenApply(o -> fromByteString(o, defaultValue))
                .toCompletableFuture();
        }

    /**
     * Returns the scope name.
     *
     * @return the scope name
     */
    public String getScopeName()
        {
        return f_sScopeName;
        }

    /**
     * Return the serialization format.
     *
     * @return the serialization format
     */
    protected String getFormat()
        {
        return f_sFormat;
        }

    /**
     * Returns the cache name.
     *
     * @return the cache name
     */
    public String getCacheName()
        {
        return f_sName;
        }

    /**
     * Get the {@link CacheService} associated with this cache.
     *
     * @return the {@link CacheService} associated with this cache
     */
    public CacheService getCacheService()
        {
        return m_cacheService;
        }

    /**
     * Set the {@link CacheService} associated with this cache.
     *
     * @param cacheService  the {@link CacheService} associated with this cache
     */
    public void setCacheService(GrpcRemoteCacheService cacheService)
        {
        m_cacheService = cacheService;
        }

    /**
     * Return the synchronous cache client.
     *
     * @return the synchronous cache client
     */
    public NamedCacheClient<K, V> getNamedCacheClient()
        {
        return f_synchronousCache;
        }

    /**
     * Return {@code true} if the cache is still active.
     *
     * @return {@code true} if the cache is still active
     */
    public CompletableFuture<Boolean> isActive()
        {
        return CompletableFuture.completedFuture(isActiveInternal());
        }

    /**
     * Returns whether this cache is ready to be used.
     * </p>
     * An example of when this method would return {@code false} would
     * be where a partitioned cache service that owns this cache has no
     * store-enabled members.
     *
     * @return {@code true} if the cache is ready
     *
     * @since 14.1.1.2206.5
     */
    public CompletableFuture<Boolean> isReady()
        {
        if (isActiveInternal())
            {
            return executeIfActive(() -> f_client.isReady()
                .thenApply(BoolValue::getValue).toCompletableFuture());
            }
        return CompletableFuture.completedFuture(false);
        }

    /**
     * Helper method for storing a key/value pair within the cache.
     *
     * @param key    the key
     * @param value  the value
     * @param cTtl   the time-to-live (may not be supported)
     *
     * @return a {@link CompletableFuture} returning the value previously associated
     *         with the specified key, if any
     */
    public CompletableFuture<V> putInternal(K key, V value, long cTtl)
        {
        return executeIfActive(() -> f_client.put(toKeyByteString(key), toByteString(value), cTtl)
                .thenApplyAsync(this::valueFromBytesValue)
                .toCompletableFuture());
        }

    /**
     * Helper method for storing the contents of the provided map within the cache.
     *
     * @param map      the {@link Map} of key/value pairs to store in the cache.
     * @param cMillis  the expiry delay t apply to the entries
     *
     * @return a {@link CompletableFuture}
     */
    protected CompletableFuture<Empty> putAllInternal(Map<? extends K, ? extends V> map, long cMillis)
        {
        return executeIfActive(() ->
            {
            try
                {

                Map<ByteString, ByteString> mapBinary = new HashMap<>();
                for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
                    {
                    mapBinary.put(toKeyByteString(entry.getKey()), toByteString(entry.getValue()));
                    }
                return f_client.putAll(mapBinary, cMillis);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    /**
     * Releases the cache.
     *
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    public CompletableFuture<Void> release()
        {
        return executeIfActive(() -> releaseInternal(false));
        }

     public  <T, E> CompletableFuture<Void> removeIndex(ValueExtractor<? super T, ? extends E> valueExtractor)
        {
        return f_client.removeIndex(toByteString(valueExtractor))
                .thenApply(e -> VOID).toCompletableFuture();
        }

    /**
     * Remove the mapping for the given key, returning the associated value.
     *
     * @param key  key whose mapping is to be removed from the map
     *
     * @return the value associated with the given key, or {@code null} if no mapping existed
     */
    public CompletableFuture<V> removeInternal(Object key)
        {
        return executeIfActive(() -> f_client.remove(toKeyByteString(key))
                .thenApplyAsync(this::valueFromBytesValue)
                .toCompletableFuture());
        }

    /**
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * @param key    key with which the specified value is associated
     * @param value  value expected to be associated with the specified key
     *
     * @return a {@link CompletableFuture} returning a {@code boolean} indicating
     *         whether the mapping was removed
     */
    public CompletableFuture<Boolean> removeInternal(Object key, Object value)
        {
        return executeIfActive(() -> f_client.remove(toKeyByteString(key), toByteString(value))
                .thenApplyAsync(BoolValue::getValue)
                .toCompletableFuture());
        }

    /**
     * Removes the specified {@link MapListener}.
     *
     * @param mapListener  the {@link MapListener} to remove
     *
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    public CompletableFuture<Void> removeMapListener(MapListener<? super K, ? super V> mapListener)
        {
        return removeMapListener(mapListener, (Filter<?>) null);
        }

    /**
     * Removes the {@link MapListener} associated with the specified key.
     *
     * @param listener  the {@link MapListener} to remove
     * @param key       the key the listener is associated with
     *
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    public CompletableFuture<Void> removeMapListener(MapListener<? super K, ? super V> listener, K key)
        {
        return executeIfActive(() ->
            {
            MapListenerSupport support = getMapListenerSupport();
            synchronized (support)
                {
                support.removeListener(listener, key);
                boolean fEmpty   = support.isEmpty(key);
                boolean fPriming = MapListenerSupport.isPrimingListener(listener);

                // "priming" request should be sent regardless
                if (fEmpty || fPriming)
                    {
                    return f_client.removeMapListener(toKeyByteString(key), fPriming);
                    }
                else
                    {
                    // we're not actually adding the listener to the server so complete the future now.
                    return CompletableFuture.completedFuture(VOID);
                    }
                }
            });
        }

    /**
     * Removes the {@link MapListener} associated with the specified {@link Filter}.
     *
     * @param listener  the {@link MapListener} to remove
     * @param filter    the filter the listener is associated with
     *
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    public CompletableFuture<Void> removeMapListener(MapListener<? super K, ? super V> listener, Filter<?> filter)
        {
        return executeIfActive(() ->
            {
            if (listener instanceof NamedCacheDeactivationListener && filter == null)
                {
                f_lockDeactivationListeners.lock();
                try
                    {
                    if (f_listCacheDeactivationListeners.remove(listener))
                        {
                        f_cListener.decrementAndGet();
                        }
                    }
                finally
                    {
                    f_lockDeactivationListeners.unlock();
                    }
                return CompletableFuture.completedFuture(VOID);
                }

            if (listener instanceof MapTriggerListener)
                {
                MapTriggerListener triggerListener = (MapTriggerListener) listener;
                return f_client.removeMapListener(ByteString.EMPTY, 0L, toByteString(triggerListener.getTrigger()));
                }

            MapListenerSupport support = getMapListenerSupport();
            synchronized (support)
                {
                long nId = getFilterId(filter);
                support.removeListener(listener, filter);
                if (support.isEmpty(filter))
                    {
                    return f_client.removeMapListener(toByteString(filter), nId, ByteString.EMPTY);
                    }
                }
            return CompletableFuture.completedFuture(VOID);
            });
        }

    /**
     * {@link Stream} all entries within the cache.
     *
     * @return a {@link Stream} for processing entries within the cache.
     */
    protected Stream<InvocableMap.Entry<K, V>> stream()
        {
        assertActive();
        return stream(AlwaysFilter.INSTANCE());
        }

    /**
     * {@link Stream} which filters the cache entries.
     *
     * @param filter  the filter to apply to the {@link Stream}
     *
     * @return a {@link Stream} for processing entries within the cache.
     *
     * @throws UnsupportedOperationException this method is unsupported
     */
    @SuppressWarnings("unused")
    public Stream<InvocableMap.Entry<K, V>> stream(Filter<V> filter)
        {
        assertActive();
        throw new UnsupportedOperationException("method not implemented");
        }

    /**
     * Removes all mappings from this map.
     * <p>
     * Note: the removal of entries caused by this truncate operation will
     * not be observable. This includes any registered {@link MapListener
     * listeners}, {@link com.tangosol.util.MapTrigger triggers}, or {@link
     * com.tangosol.net.events.EventInterceptor interceptors}. However, a
     * {@link com.tangosol.net.events.partition.cache.CacheLifecycleEvent CacheLifecycleEvent}
     * is raised to notify subscribers of the execution of this operation.
     *
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    public CompletableFuture<Void> truncate()
        {
        return executeIfActive(() -> f_client.truncate()
                .thenApply(e -> VOID).toCompletableFuture());
        }

    /**
     * Helper method to return the values of the cache.
     *
     * @param filter      the {@link Filter} to apply
     * @param comparator  the {@link Comparator} for ordering
     *
     * @return a {@link Collection} of values based on the provided {@link Filter} and {@link Comparator}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<Collection<V>> valuesInternal(Filter<?> filter, Comparator comparator)
        {
        return values(filter).thenApply((colValues) ->
            {
            List<V> values = new ArrayList<>(colValues);
            values.sort(comparator);
            return values;
            });
        }

    /**
     * Helper method for confirm presence of the value within the cache.
     *
     * @param oValue  the value to check
     *
     * @return a {@link CompletableFuture} returning {@code true} if the cache contains the given value
     */
    public CompletableFuture<Boolean> containsValue(Object oValue)
        {
        return executeIfActive(() -> f_client.containsValue(toByteString(oValue))
                .thenApplyAsync(BoolValue::getValue)
                .toCompletableFuture());
        }

    /**
     * Destroys the cache.
     *
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    public CompletableFuture<Void> destroy()
        {
        return executeIfActive(() -> releaseInternal(true));
        }

    /**
     * Return the {@link MapListenerSupport}.
     *
     * @return the {@link MapListenerSupport}
     */
    protected MapListenerSupport getMapListenerSupport()
        {
        return m_listenerSupport;
        }

    /**
     * Obtain a page of cache keys.
     *
     * @param cookie  an opaque cooke used to determine the current page
     *
     * @return a page of cache keys
     */
    public Stream<BytesValue> getKeysPage(BytesValue cookie)
        {
        assertActive();
        ByteString s = cookie == null ? null : cookie.getValue();
        return f_client.getKeysPage(s);
        }

    /**
     * Determine whether en entry exists in the cache with the specified key and value.
     *
     * @param key    the cache key
     * @param value  the cache value
     *
     * @return a page of cache keys
     */
    public boolean containsEntry(K key, V value)
        {
        assertActive();
        try
            {
            BoolValue boolValue = f_client.containsEntry(toKeyByteString(key), toByteString(value))
                    .toCompletableFuture()
                    .get();
            return boolValue != null && boolValue.getValue();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    /**
     * Assert that this {@link AsyncNamedCacheClient} is active.
     *
     * @throws IllegalStateException if this {@link AsyncNamedCacheClient}
     *                               is not active
     */
    public void assertActive()
        {
        if (m_fReleased || m_fDestroyed)
            {
            String reason = m_fDestroyed ? "destroyed" : "released";
            throw new IllegalStateException("remote cache '" + f_sName + "' has been " + reason);
            }
        }

    /**
     * If this {@link AsyncNamedCacheClient} is active then return the {@link CompletableFuture}
     * supplied by the supplier otherwise return a failed {@link CompletableFuture}.
     */
    protected  <T> CompletableFuture<T> executeIfActive(Supplier<CompletableFuture<T>> supplier)
        {
        if (!m_fReleased && !m_fDestroyed)
            {
            try
                {
                return supplier.get().handle(AsyncNamedCacheClient::handleException);
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            }
        else
            {
            String reason = m_fDestroyed ? "destroyed" : "released";
            return failedFuture(new IllegalStateException("remote cache '" + f_sName + "' has been " + reason));
            }
        }

    /**
     * Release or destroy this {@link AsyncNamedCacheClient}, notifying any registered
     * {@link DeactivationListener DeactivationListeners}.
     *
     * @param destroy  {@code true} to destroy the cache, {@code false} to release
     *                 the cache
     *
     * @return a {@link CompletableFuture} that will be complete when the operation
     * is completed.
     */
    protected CompletableFuture<Void> releaseInternal(boolean destroy)
        {
        f_lock.lock();
        try
            {
            CompletableFuture<Void> future;
            f_cListener.set(0);

            if (!m_fDestroyed && !m_fReleased)
                {
                if (destroy)
                    {
                    m_fDestroyed = true;
                    future = f_client.destroy();
                    }
                else
                    {
                    m_fReleased = true;
                    future = CompletableFuture.completedFuture(VOID);
                    }
                }
            else
                {
                future = CompletableFuture.completedFuture(VOID);
                }

            getMapListenerSupport().clear();
            //f_client.close();

            return future.handleAsync((v, err) ->
                {
                for (DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>> listener : f_listDeactivationListeners)
                    {
                    try
                        {
                        if (destroy)
                            {
                            listener.destroyed(this);
                            }
                        else
                            {
                            listener.released(this);
                            }
                        }
                    catch (Throwable t)
                        {
                        Logger.err(t);
                        }
                    }
                f_listDeactivationListeners.clear();

                CacheEvent<?, ?> evt = createDeactivationEvent(/*destroyed*/true);
                for (NamedCacheDeactivationListener listener : f_listCacheDeactivationListeners)
                    {
                    try
                        {
                        listener.entryDeleted(evt);
                        }
                    catch (Throwable t)
                        {
                        Logger.err(t);
                        }
                    }
                f_listCacheDeactivationListeners.clear();

                if (err != null)
                    {
                    throw Base.ensureRuntimeException(err);
                    }
                return VOID;
                });
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Helper method to create a new {@link CacheEvent} to signal cache truncation or destruction.
     *
     * @param destroyed  {@code true} if this event should represent a cache being destroyed
     * @param <Ke>       the {@link CacheEvent} key type
     * @param <Ve>       the {@link CacheEvent} value type
     *
     * @return  a new {@link CacheEvent} to signal cache truncation or destruction
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <Ke, Ve> CacheEvent<Ke, Ve> createDeactivationEvent(boolean destroyed)
        {
        return new CacheEvent(getNamedCache(), destroyed ? CacheEvent.ENTRY_DELETED : CacheEvent.ENTRY_UPDATED,
                              null, null, null, true);
        }

    /**
     * Add a {@link DeactivationListener} that will be notified when this
     * {@link AsyncNamedCacheClient} is released or destroyed.
     *
     * @param listener  the listener to add
     */
    public void addDeactivationListener(DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>> listener)
        {
        assertActive();
        f_lockDeactivationListeners.lock();
        try
            {
            if (listener != null)
                {
                f_listDeactivationListeners.add(listener);
                }
            }
        finally
            {
            f_lockDeactivationListeners.unlock();
            }
        }

    /**
     * Add a {@link NamedCacheDeactivationListener} that will be notified when this
     * {@link AsyncNamedCacheClient} is released or destroyed.
     *
     * @param listener  the listener to add
     */
    public void addDeactivationListener(NamedCacheDeactivationListener listener)
        {
        if (listener != null)
            {
            if (m_fReleased || m_fDestroyed)
                {
                listener.entryDeleted(createDeactivationEvent(m_fDestroyed));
                }
            else
                {
                f_lockDeactivationListeners.lock();
                try
                    {
                    f_listCacheDeactivationListeners.add(listener);
                    }
                finally
                    {
                    f_lockDeactivationListeners.unlock();
                    }
                }
            }
        }

    /**
     * Remove a {@link DeactivationListener}.
     *
     * @param listener  the listener to remove
     */
    public void removeDeactivationListener(DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>> listener)
        {
        if (listener != null)
            {
            f_lockDeactivationListeners.lock();
            try
                {
                f_listDeactivationListeners.remove(listener);
                }
            finally
                {
                f_lockDeactivationListeners.unlock();
                }
            }
        }

    protected List<NamedCacheDeactivationListener> getDeactivationListeners()
        {
        return Collections.unmodifiableList(f_listCacheDeactivationListeners);
        }

    /**
     * Asynchronous implementation of {@link NamedCache#addIndex}.
     *
     * @param extractor   the ValueExtractor object that is used to extract an
     *                    indexable Object from a value stored in the indexed
     *                    Map.  Must not be {@code null}
     * @param fOrdered    true iff the contents of the indexed information should
     *                    be ordered; false otherwise
     * @param comparator  the Comparator object which imposes an ordering on
     *                    entries in the indexed map; or <tt>null</tt> if the
     *                    entries' values natural ordering should be used
     * @param <T>         the type of the value to extract from
     * @param <E>         the type of value that will be extracted
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    public  <T, E> CompletableFuture<Void> addIndex(ValueExtractor<? super T, ? extends E> extractor,
            boolean fOrdered, Comparator<? super E> comparator)
        {
        return executeIfActive(() -> f_client.addIndex(toByteString(extractor), fOrdered, toByteStringOrNull(comparator)));
        }

    /**
     * Add a standard map listener that will receive all events (inserts,
     * updates, deletes) that occur against the map, with the key, old-value
     * and new-value included
     *
     * @param mapListener  the {@link MapEvent} listener to add
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    public CompletableFuture<Void> addMapListener(MapListener<? super K, ? super V> mapListener)
        {
        return addMapListener(mapListener, (Filter<?>) null, false);
        }

    /**
     * Add a map listener for a specific key.
     * <p>
     * The listeners will receive MapEvent objects, but if fLite is passed as
     * true, they <i>might</i> not contain the OldValue and NewValue
     * properties.
     *
     * @param mapListener  the {@link MapEvent} listener to add
     * @param key          the key that identifies the entry for which to raise
     *                     events
     * @param fLite        {@code true} to indicate that the {@link MapEvent} objects do
     *                     not have to include the OldValue and NewValue
     *                     property values in order to allow optimizations
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    public CompletableFuture<Void> addMapListener(MapListener<? super K, ? super V> mapListener, K key, boolean fLite)
        {
        return executeIfActive(() ->
                               {
                               try
                                   {
                                   return addKeyMapListener(mapListener, key, fLite);
                                   }
                               catch (Exception e)
                                   {
                                   return CompletableFuture.failedFuture(e);
                                   }
                               });
        }

    /**
     * Add a map listener that receives events based on a filter evaluation.
     * <p>
     * The listeners will receive MapEvent objects, but if fLite is passed as
     * true, they <i>might</i> not contain the OldValue and NewValue
     * properties.
     *
     * @param mapListener  the {@link MapEvent} listener to add
     * @param filter       a filter that will be passed {@link MapEvent} objects to select
     *                     from; a {@link MapEvent} will be delivered to the listener only
     *                     if the filter evaluates to {@code true} for that {@link MapEvent} (see
     *                     {@link com.tangosol.util.filter.MapEventFilter});
     *                     {@code null} is equivalent to a filter that always returns {@code true}
     * @param fLite        {@code true} to indicate that the {@link MapEvent} objects do
     *                     not have to include the OldValue and NewValue
     *                     property values in order to allow optimizations
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    public CompletableFuture<Void> addMapListener(MapListener<? super K, ? super V> mapListener, Filter<?> filter,
                                                     boolean fLite)
        {
        return executeIfActive(() ->
                               {
                               try
                                   {
                                   return addFilterMapListener(mapListener, filter, fLite);
                                   }
                               catch (Exception e)
                                   {
                                   CompletableFuture<Void> future = new CompletableFuture<>();

                                   future.completeExceptionally(e);
                                   return future;
                                   }
                               });
        }

    /**
     * Add a map listener for a specific key.
     * <p>
     * The listeners will receive MapEvent objects, but if {@code fLite} is passed as
     * {@code true}, they <i>might</i> not contain the OldValue and NewValue
     * properties.
     *
     * @param listener  the {@link MapEvent} listener to add
     * @param key       the key that identifies the entry for which to raise
     *                  events
     * @param fLite     {@code true} to indicate that the {@link MapEvent}
     *                  objects do not have to include the OldValue and NewValue
     *                  property values in order to allow optimizations
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    protected CompletableFuture<Void> addKeyMapListener(MapListener<? super K, ? super V> listener, Object key,
                                                        boolean fLite)
        {
        MapListenerSupport support      = getMapListenerSupport();
        boolean            fShouldAdd   = support.addListenerWithCheck(listener, key, fLite);
        boolean            fPriming     = MapListenerSupport.isPrimingListener(listener);
        boolean            fSynchronous = listener.isSynchronous();

        // "priming" request should be sent regardless
        if (fShouldAdd || fPriming)
            {
            return f_client.addMapListener(toKeyByteString(key), fLite, fPriming, fSynchronous)
                .handle((ignored, err) ->
                {
                if (err != null)
                    {
                    synchronized (support)
                        {
                        support.removeListener(listener, key);
                        }
                    }
                return VOID;
                });
            }
        else
            {
            // we're not actually adding the listener to the server so complete the future now.
            return CompletableFuture.completedFuture(VOID);
            }
        }

    /**
     * Add a map listener that receives events based on a filter evaluation.
     * <p>
     * The listeners will receive MapEvent objects, but if {@code fLite} is passed as
     * {@code true}, they <i>might</i> not contain the OldValue and NewValue
     * properties.
     *
     * @param listener     the {@link MapEvent} listener to add
     * @param filter       a filter that will be passed {@link MapEvent} objects to select
     *                     from; a {@link MapEvent} will be delivered to the listener only
     *                     if the filter evaluates to {@code true} for that {@link MapEvent} (see
     *                     {@link com.tangosol.util.filter.MapEventFilter});
     *                     {@code null} is equivalent to a filter that always returns {@code true}
     * @param fLite        {@code true} to indicate that the {@link MapEvent} objects do
     *                     not have to include the OldValue and NewValue
     *                     property values in order to allow optimizations
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    protected CompletableFuture<Void> addFilterMapListener(MapListener<? super K, ? super V> listener,
                                                           Filter<?> filter, boolean fLite)
        {
        boolean fSynchronous = listener.isSynchronous();

        if (listener instanceof NamedCacheDeactivationListener)
            {
            f_lockDeactivationListeners.lock();
            try
                {
                f_listCacheDeactivationListeners.add((NamedCacheDeactivationListener) listener);
                f_cListener.incrementAndGet();
                }
            finally
                {
                f_lockDeactivationListeners.unlock();
                }
            return CompletableFuture.completedFuture(VOID);
            }

        if (listener instanceof MapTriggerListener)
            {
            MapTriggerListener triggerListener = (MapTriggerListener) listener;
            return f_client.addMapListener(ByteString.EMPTY, 0L, fLite, toByteString(triggerListener.getTrigger()), fSynchronous);
            }

        boolean wasEmpty;
        boolean first;
        long    filterId;

        CompletableFuture<Void> future;

        MapListenerSupport support = getMapListenerSupport();
        synchronized (support)
            {
            wasEmpty = support.isEmpty(filter);
            first    = support.addListenerWithCheck(listener, filter, fLite);
            filterId = wasEmpty ? registerFilter(filter) : getFilterId(filter);
            }

        if (wasEmpty || first)
            {
            future = f_client.addMapListener(toByteString(filter), filterId, fLite, ByteString.EMPTY, fSynchronous);
            if (future.isCompletedExceptionally())
                {
                synchronized (support)
                    {
                    if (wasEmpty)
                        {
                        m_aEvtFilter.remove(filterId);
                        }
                    support.removeListener(listener, filter);
                    }
                }
            }
        else
            {
            // we're not actually adding the listener to the server so complete the future now.
            future = CompletableFuture.completedFuture(VOID);
            }

        return future;
        }

    /**
     * Register the {@link Filter} locally.
     *
     * @param filter  the {@link Filter}
     *
     * @return the registration ID of the {@link Filter}
     */
    protected long registerFilter(Filter<?> filter)
        {
        if (m_aEvtFilter.isEmpty())
            {
            m_aEvtFilter.set(1, filter);
            return 1L;
            }
        else
            {
            return m_aEvtFilter.add(filter);
            }
        }

    /**
     * Obtain the registration ID for the provided filter.
     *
     * @param filter  the filter whose ID it is to look up
     *
     * @return the registration ID or zero if the filter hasn't been registered
     */
    @SuppressWarnings("rawtypes")
    protected long getFilterId(Filter<?> filter)
        {
        for (Iterator iter = m_aEvtFilter.iterator(); iter.hasNext(); )
            {
            Filter<?> filterThat = (Filter<?>) iter.next();
            if (Base.equals(filter, filterThat))
                {
                return iter.getIndex();
                }
            }

        return 0L;
        }

    /**
     * Dispatch a received mep event for processing by local listeners.
     * <p>
     * This method is taken from code in the TDE RemoteNamedCache.
     *
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions"})
    public void dispatch(List<Long> listFilterIds, int nEventId, ByteString binKey, ByteString binOldValue,
                            ByteString binNewValue, boolean fSynthetic, boolean fPriming,
                            TransformationState transformState)
        {
        int                 cFilters       = listFilterIds == null ? 0 : listFilterIds.size();
        Object              oKey           = fromByteString(binKey);
        Object              oValueOld      = fromByteString(binOldValue);
        Object              oValueNew      = fromByteString(binNewValue);
        MapListenerSupport  support        = getMapListenerSupport();
        CacheEvent<?, ?>    evt            = null;

        // collect key-based listeners
        Listeners listeners = transformState == TransformationState.TRANSFORMED ? null : support.getListeners(oKey);
        if (cFilters > 0)
            {
            LongArray<Filter<?>> laFilters   = m_aEvtFilter;
            List<Filter<?>>      listFilters = null;

            // collect filter-based listeners
            synchronized (support)
                {
                for (int i = 0; i < cFilters; i++)
                    {
                    long lFilterId = listFilterIds.get(i);
                    if (laFilters.exists(lFilterId))
                        {
                        Filter<?> filter = laFilters.get(lFilterId);
                        if (listFilters == null)
                            {
                            listFilters = new ArrayList<>(cFilters - i);

                            // clone the key listeners before merging filter listeners
                            Listeners listenersTemp = new Listeners();
                            listenersTemp.addAll(listeners);
                            listeners = listenersTemp;
                            }

                        listFilters.add(filter);
                        listeners.addAll(support.getListeners(filter));
                        }
                    }
                }

            if (listFilters != null)
                {
                Filter<?>[] aFilters = new Filter[listFilters.size()];
                aFilters = listFilters.toArray(aFilters);

                evt = new MapListenerSupport.FilterEvent(getNamedMap(), nEventId, oKey, oValueOld, oValueNew,
                                                         fSynthetic, transformState, fPriming, aFilters);
                }
            }

        //noinspection StatementWithEmptyBody
        if (listeners == null || listeners.isEmpty())
            {
            // we cannot safely remove the orphaned listener because of the following
            // race condition: if another thread registers a listener for the same key
            // or filter associated with the event between the time that this thread
            // detected the orphaned listener, but before either sends a message to the
            // server, it is possible for this thread to inadvertently remove the new
            // listener
            //
            // since it is only possible for synchronous listeners to be leaked (due to
            // the extra synchronization in the SafeNamedCache), let's err on the side
            // of leaking a listener than possibly incorrectly removing a listener
            //
            // there is also a valid scenario of a client thread removing an async
            // listener while the event is already on the wire; hence no logging makes sense
            }
        else
            {
            if (evt == null)
                {
                evt = new CacheEvent(getNamedMap(), nEventId, oKey, oValueOld, oValueNew, fSynthetic, transformState, fPriming);
                }


            for (EventListener listener : listeners.listeners())
                {
                EventTask task = new EventTask(evt, (MapListener) listener);
                if (listener instanceof SynchronousListener)
                    {
                    task.run();
                    }
                else
                    {
                    f_executor.execute(task);
                    }
                }
            }
        }

    @Override
    public void onDestroy()
        {
        if (isActiveInternal())
            {
            m_fDestroyed = true;
            releaseInternal(true);
            }
        f_cListener.set(0);
        }

    @Override
    public void onTruncate()
        {
        CacheEvent<?, ?> evt = createDeactivationEvent(/*destroyed*/ false);
        for (NamedCacheDeactivationListener listener : getDeactivationListeners())
            {
            try
                {
                listener.entryUpdated(evt);
                }
            catch (Throwable t)
                {
                Logger.err(t);
                }
            }
        }

    @Override
    public void incrementListeners()
        {
        f_cListener.incrementAndGet();
        }

    @Override
    public void decrementListeners()
        {
        f_cListener.decrementAndGet();
        }

    /**
     * Return the number of listeners registered with this map.
     *
     * @return the number of listeners registered with this map
     */
    public int getListenerCount()
        {
        return f_cListener.get();
        }

    /**
     * Returns the event dispatcher for this cache.
     *
     * @return the event dispatcher for this cache
     */
    public GrpcCacheLifecycleEventDispatcher getEventDispatcher()
        {
        return (GrpcCacheLifecycleEventDispatcher) f_dispatcher;
        }

    /**
     * Return this client's dependencies.
     *
     * @return this client's dependencies
     */
    protected AsyncNamedCacheClient.Dependencies getDependencies()
        {
        return (AsyncNamedCacheClient.Dependencies) f_dependencies;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "AsyncNamedCacheClient{"
                + "scope: \"" + f_sScopeName + '"'
                + "name: \"" + f_sName + '"'
                + " format: \"" + f_sFormat + '"'
                + '}';
        }

    // ----- inner class: EventTask -----------------------------------------

    /**
     * A simple {@link Runnable} to dispatch an event to a listener.
     */
    @SuppressWarnings("rawtypes")
    public static class EventTask
            implements Runnable
        {
        /**
         * Create an {@link EventTask}.
         *
         * @param event     the event to dispatch
         * @param listener  the listener to dispatch the event to
         */
        public EventTask(CacheEvent<?, ?> event, MapListener listener)
            {
            f_event    = event;
            f_listener = listener;
            }

        @Override
        @SuppressWarnings("unchecked")
        public void run()
            {
            NamedCache<?, ?> cache = (NamedCache<?, ?>) f_event.getSource();
            if (cache.isActive())
                {
                try
                    {
                    f_event.dispatch(f_listener);
                    }
                catch (Throwable thrown)
                    {
                    CacheFactory.err("Caught exception dispatching event to listener");
                    CacheFactory.err(thrown);
                    }
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The event to dispatch.
         */
        private final CacheEvent<?, ?> f_event;

        /**
         * The listener to dispatch the event to.
         */
        private final MapListener f_listener;
        }


    // ----- inner class: WrapperDeactivationListener -----------------------

    /**
     * A {@link DeactivationListener} that wraps client {@link NamedCacheDeactivationListener}.
     */
    @SuppressWarnings("unused")
    protected static class WrapperDeactivationListener<K, V>
            implements DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code WrapperDeactivationListener}.
         *
         * @param listener  the {@link MapListener} to wrap
         */
        protected WrapperDeactivationListener(MapListener<? super K, ? super V> listener)
            {
            m_listener = listener;
            }

        // ----- DeactivationListener interface -----------------------------

        @Override
        public void released(AsyncNamedCacheClient<? super K, ? super V> client)
            {
            }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void destroyed(AsyncNamedCacheClient<? super K, ? super V> client)
            {
            CacheEvent evt = client.createDeactivationEvent(true);
            m_listener.entryDeleted(evt);
            }

        // ----- data members -----------------------------------------------

        /**
         * The delegate {@link MapListener}.
         */
        protected final MapListener<? super K, ? super V> m_listener;
        }

    // ----- helper methods ---------------------------------------------

   /**
    * Returns the arguments unmodified unless the error is a {@link StatusRuntimeException}
    * with a status of {@link Status#UNIMPLEMENTED}, then an UnsupportedOperationException
    * will be raised instead.
    *
    * @param result  the result - ignored
    * @param t       the error, if any
    * @return        the result unmodified, or if the error is {@link Status#UNIMPLEMENTED}
    *                an {@link UnsupportedOperationException} will be thrown
    *
    * @param <T> the result type
    *
    * @since 14.1.1.2206.5
    */
   protected static <T> T handleException(T result, Throwable t)
       {
       if (t != null)
           {
           Throwable cause = t.getCause();
           if (cause instanceof StatusRuntimeException)
               {
               StatusRuntimeException sre = (StatusRuntimeException) cause;
               if (sre.getStatus().getCode() == Status.Code.UNIMPLEMENTED.toStatus().getCode())
                   {
                   throw new UnsupportedOperationException("This operation"
                           + " is not supported by the current gRPC proxy. "
                           + "Either upgrade the version of Coherence on the "
                           + "gRPC proxy or connect to a gRPC proxy "
                           + "that supports the operation.", sre);
                   }
               }
           throw Exceptions.ensureRuntimeException(t);
           }
       return result;
       }


    // ----- Dependencies ---------------------------------------------------

    /**
     * The dependencies used to create an {@link AsyncNamedCacheClient}.
     */
    public interface Dependencies
            extends BaseGrpcClient.Dependencies
        {
        /**
         * Returns the frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional channel.
         *
         * @return the frequency in millis that heartbeats should be sent by the
         *         proxy to the client bidirectional channel
         */
        long getHeartbeatMillis();

        /**
         * Return the flag to determine whether heart beat messages should require an
         * ack response from the server.
         *
         * @return  that is {@code true} if heart beat messages should require an
         *          ack response from the server
         */
        boolean isRequireHeartbeatAck();
        }

    // ----- DefaultDependencies ----------------------------------------

    /**
     * The dependencies used to create an {@link AsyncNamedCacheClient}.
     */
    public static class DefaultDependencies
            extends BaseGrpcClient.DefaultDependencies
            implements Dependencies
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link DefaultDependencies}.
         *
         * @param sCacheName  the name of the underlying cache that
         *                    the {@link AsyncNamedCacheClient} will
         *                    represent
         * @param channel     the gRPC {@link Channel} to use
         */
        public DefaultDependencies(String sCacheName, Channel channel, GrpcCacheLifecycleEventDispatcher dispatcher)
            {
            super(sCacheName, channel, dispatcher);
            }

        // ----- Dependencies methods ---------------------------------------

        @Override
        public long getHeartbeatMillis()
            {
            return m_nEventsHeartbeat;
            }

        @Override
        public boolean isRequireHeartbeatAck()
            {
            return m_fRequireHeartbeatAck;
            }

        // ----- setters ----------------------------------------------------

        /**
         * Set the frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional channel.
         * <p/>
         * If the frequency is set to zero or less, then no heartbeats will be sent.
         *
         * @param nEventsHeartbeat the heartbeat frequency in millis
         */
        public void setHeartbeatMillis(long nEventsHeartbeat)
            {
            m_nEventsHeartbeat = Math.max(NO_EVENTS_HEARTBEAT, nEventsHeartbeat);
            }

        /**
         * Set the flag to indicate whether heart beat messages require an
         * ack response from the server.
         *
         * @param fRequireHeartbeatAck  {@code true} to require an ack response
         */
        public void setRequireHeartbeatAck(boolean fRequireHeartbeatAck)
            {
            m_fRequireHeartbeatAck = fRequireHeartbeatAck;
            }

        // ----- data members -----------------------------------------------

        /**
         * The frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional channel
         */
        private long m_nEventsHeartbeat = NO_EVENTS_HEARTBEAT;


        private boolean m_fRequireHeartbeatAck;
        }

    // ----- inner class: EntryAdvancer -------------------------------------

    /**
     * A {@link PagedIterator.Advancer} to support a
     * {@link PagedIterator} over an entry set.
     */
    @SuppressWarnings("rawtypes")
    protected static class EntryAdvancer<K, V>
            implements PagedIterator.Advancer
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code EntryAdvancer} using the provided {@link AsyncNamedCacheClient}.
         *
         * @param client  the async client
         */
        protected EntryAdvancer(AsyncNamedCacheClient<K, V> client)
            {
            this.f_parent = client;
            }

        // ----- Advancer interface -----------------------------------------

        @Override
        public void remove(Object oCurr)
            {
            Map.Entry entry = (Map.Entry) oCurr;
            try
                {
                f_parent.removeInternal(entry.getKey())
                        .toCompletableFuture()
                        .get();
                }
            catch (InterruptedException | ExecutionException e)
                {
                throw new RequestIncompleteException(e);
                }
            }

        @Override
        public Collection nextPage()
            {
            if (m_exhausted)
                {
                return null;
                }

            NamedCacheClientChannel.EntrySetPage page = f_parent.f_client.getEntriesPage(m_cookie);
            if (!page.isEmpty())
                {
                m_cookie = page.cookie();
                }
            else
                {
                m_cookie = null;
                }

            m_exhausted = m_cookie == null || m_cookie.isEmpty();
            return ConverterCollections.getEntrySet(page.entries(), f_parent::fromByteString, f_parent::toKeyByteString,
                    f_parent::fromByteString, f_parent::toByteString);
            }

        // ----- data members -----------------------------------------------

        /**
         * A flag indicating whether this advancer has exhausted all of the pages.
         */
        protected boolean m_exhausted;

        /**
         * The opaque cookie used by the server to maintain the page location.
         */
        protected ByteString m_cookie;

        /**
         * The {@link AsyncNamedCacheClient} used to send gRPC requests.
         */
        protected final AsyncNamedCacheClient<K, V> f_parent;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A constant value to use for {@link Void} values.
     */
    protected static final Void VOID = null;

    // ----- data members ---------------------------------------------------

    /**
     * The synchronous version of this client.
     */
    private final NamedCacheClient<K, V> f_synchronousCache;

    /**
     * The list of {@link DeactivationListener} to be notified when this {@link AsyncNamedCacheClient}
     * is released or destroyed.
     */
    private final List<DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>>> f_listDeactivationListeners;

    /**
     * The list of {@link NamedCacheDeactivationListener} instances added to this client.
     */
    private final List<NamedCacheDeactivationListener> f_listCacheDeactivationListeners = new ArrayList<>();

    private final Lock f_lockDeactivationListeners = new ReentrantLock();

    private final Lock f_lock = new ReentrantLock();

    /**
     * The map listener support.
     */
    private final MapListenerSupport m_listenerSupport;

    /**
     * The array of filter.
     */
    private final LongArray<Filter<?>> m_aEvtFilter;

    /**
     * The count of successfully registered {@link MapListener map listeners}
     */
    protected final AtomicInteger f_cListener = new AtomicInteger(0);

    /**
     * The owing cache service.
     */
    private GrpcRemoteCacheService m_cacheService;
    }
