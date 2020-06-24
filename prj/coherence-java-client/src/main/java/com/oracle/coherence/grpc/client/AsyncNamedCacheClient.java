/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.UnsafeByteOperations;

import com.oracle.coherence.cdi.Scope;

import com.oracle.coherence.cdi.SerializerProducer;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.ContainsEntryRequest;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.EntryResult;
import com.oracle.coherence.grpc.InvokeAllRequest;
import com.oracle.coherence.grpc.MapEventResponse;
import com.oracle.coherence.grpc.MapListenerErrorResponse;
import com.oracle.coherence.grpc.MapListenerRequest;
import com.oracle.coherence.grpc.MapListenerResponse;
import com.oracle.coherence.grpc.MapListenerSubscribedResponse;
import com.oracle.coherence.grpc.MapListenerUnsubscribedResponse;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.Requests;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.io.Serializer;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheEvent.TransformationState;
import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.Listeners;
import com.tangosol.util.LongArray;
import com.tangosol.util.LongArray.Iterator;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.SparseArray;
import com.tangosol.util.SynchronousListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import io.grpc.Channel;

import io.grpc.stub.StreamObserver;

import io.helidon.config.Config;

import io.helidon.grpc.client.GrpcChannelsProvider;

import io.helidon.microprofile.grpc.client.GrpcProxyBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

/**
 * Implementation of a {@link NamedCache}.
 *
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 *
 * @author Jonathan Knight  2019.11.22
 * @since 20.06
 */
public class AsyncNamedCacheClient<K, V>
        implements AsyncNamedCache<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a {@link NamedCacheClient} with the specified cache name
     * and {@link NamedCacheService}.
     * 
     * @param builder  the {@link Builder} to configure this {@link NamedCacheService}.
     */
    AsyncNamedCacheClient(Builder<K, V> builder)
        {
        f_sScopeName                = builder.getScopeName().orElse(Scope.DEFAULT);
        f_sCacheName                = builder.getCacheName();
        f_service                   = builder.ensureServiceProxy();
        f_serializer                = builder.ensureSerializer();
        f_sFormat                   = builder.getFormat();
        f_executor                  = builder.ensureExecutor();
        f_synchronousCache          = new NamedCacheClient<>(this);
        f_listDeactivationListeners = new ArrayList<>();
        initEvents();
        }

    /**
     * Create an {@link AsyncNamedCacheClient} instance.
     *
     * @param sScopeName  the scope name to use to obtain the server side ConfigurableCacheFactory.
     * @param sCacheName  the name of the cache
     * @param <K>         the type of the cache keys
     * @param <V>         the type of the cache values
     *
     * @return an {@link AsyncNamedCacheClient} instance
     */
    public static <K, V> AsyncNamedCacheClient<K, V> create(String sScopeName, String sCacheName)
        {
        Builder<K, V> builder = builder(sScopeName, sCacheName);
        return builder.build();
        }

    /**
     * Create a {@link Builder} instance that will build an {@link AsyncNamedCacheClient}.
     *
     * @param sScopeName  the scope name to use to obtain the server side ConfigurableCacheFactory.
     * @param sCacheName  the name of the cache
     * @param <K>         the type of the cache keys
     * @param <V>         the type of the cache values
     *
     * @return an {@link AsyncNamedCacheClient} instance
     */
    public static <K, V> Builder<K, V> builder(String sScopeName, String sCacheName)
        {
        return builder(sScopeName, sCacheName, Config.empty());
        }

    /**
     * Create a {@link Builder} instance that will build an {@link AsyncNamedCacheClient}.
     *
     * @param sScopeName  the scope name to use to obtain the server side ConfigurableCacheFactory.
     * @param sCacheName  the name of the cache
     * @param config      the configuration to use
     * @param <K>         the type of the cache keys
     * @param <V>         the type of the cache values
     *
     * @return an {@link AsyncNamedCacheClient} instance
     */
    public static <K, V> Builder<K, V> builder(String sScopeName, String sCacheName, Config config)
        {
        return new Builder<>(sScopeName, sCacheName, config);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "AsyncNamedCacheClient{"
               + "scope: \"" + f_sScopeName + '"'
               + "name: \"" + f_sCacheName + '"'
               + " format: \"" + f_sFormat + '"'
               + '}';
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
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> aggregate(Collection<? extends K> colKeys,
                                              InvocableMap.EntryAggregator<? super K, ? super V, R> entryAggregator)
        {
        return executeIfActive(() ->
            {
            try
                {
                List<ByteString> keys = colKeys.stream()
                        .map(this::toByteString)
                        .collect(Collectors.toList());
                return f_service.aggregate(Requests.aggregate(f_sScopeName, f_sCacheName, f_sFormat, keys,
                                                              toByteString(entryAggregator)))
                        .thenApplyAsync(this::fromBytesValue)
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
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> aggregate(Filter filter,
                                              InvocableMap.EntryAggregator<? super K, ? super V, R> entryAggregator)
        {
        return executeIfActive(() ->
            {
            try
                {
                return f_service
                        .aggregate(Requests.aggregate(f_sScopeName, f_sCacheName, f_sFormat, toByteString(filter),
                                                      toByteString(entryAggregator)))
                        .thenApplyAsync(this::fromBytesValue)
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
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> invoke(K k, InvocableMap.EntryProcessor<K, V, R> entryProcessor)
        {
        return executeIfActive(() ->
            {
            try
                {
                return f_service.invoke(Requests.invoke(f_sScopeName, f_sCacheName, f_sFormat, toByteString(k),
                                                        toByteString(entryProcessor)))
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
                CompletableFuture<Map<K, R>>            future   = new CompletableFuture<>();
                BiFunction<Entry, Map<K, R>, Map<K, R>> function = (e, m) ->
                    {
                    try
                        {
                        m.put(fromByteString(e.getKey()), fromByteString(e.getValue()));
                        return m;
                        }
                    catch (Throwable ex)
                        {
                        future.completeExceptionally(ex);
                        }
                    return null;
                    };
                FutureStreamObserver<Entry, Map<K, R>> observer       = new FutureStreamObserver<>(future,
                                                                                                   new HashMap<>(),
                                                                                                   function);
                Collection<ByteString>                 serializedKeys = colKeys.stream()
                        .map(this::toByteString)
                        .collect(Collectors.toList());
                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, serializedKeys, toByteString(processor)),
                                  observer);
                return future;
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Map<K, R>> invokeAll(Filter filter, InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return executeIfActive(() ->
            {
            try
                {
                CompletableFuture<Map<K, R>>            future   = new CompletableFuture<>();
                BiFunction<Entry, Map<K, R>, Map<K, R>> function = (e, m) ->
                    {
                    try
                        {
                        m.put(fromByteString(e.getKey()), fromByteString(e.getValue()));
                        return m;
                        }
                    catch (Throwable ex)
                        {
                        future.completeExceptionally(ex);
                        }
                    return null;
                    };
                FutureStreamObserver<Entry, Map<K, R>> observer = new FutureStreamObserver<>(future, new HashMap<>(),
                                                                                             function);
                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, toByteString(filter),
                                                     toByteString(processor)), observer);
                return future;
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
                CompletableFuture<Void>       future   = new CompletableFuture<>();
                BiFunction<Entry, Void, Void> function = (e, v) ->
                    {
                    try
                        {
                        Map.Entry<K, R> entry = new SimpleMapEntry<>(fromByteString(e.getKey()),
                                                                     fromByteString(e.getValue()));
                        callback.accept(entry);
                        }
                    catch (Throwable ex)
                        {
                        future.completeExceptionally(ex);
                        }
                    return null;
                    };
                FutureStreamObserver<Entry, Void> observer       = new FutureStreamObserver<>(future, VOID, function);
                Collection<ByteString>            serializedKeys = colKeys.stream()
                        .map(this::toByteString)
                        .collect(Collectors.toList());
                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, serializedKeys, toByteString(processor)),
                                  observer);
                return future;
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(Filter filter,
                                                 InvocableMap.EntryProcessor<K, V, R> processor,
                                                 Consumer<? super Map.Entry<? extends K, ? extends R>> callback)
        {
        return executeIfActive(() ->
            {
            try
                {
                CompletableFuture<Void>       future   = new CompletableFuture<>();
                BiFunction<Entry, Void, Void> function = (e, v) ->
                    {
                    try
                        {
                        Map.Entry<K, R> entry = new SimpleMapEntry<>(fromByteString(e.getKey()),
                                                                     fromByteString(e.getValue()));
                        callback.accept(entry);
                        }
                    catch (Throwable ex)
                        {
                        future.completeExceptionally(ex);
                        }
                    return null;
                    };
                FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, toByteString(filter),
                                                     toByteString(processor)), observer);
                return future;
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    // ----- AsyncNamedCache methods ----------------------------------------

    @Override
    public CompletableFuture<Void> clear()
        {
        return executeIfActive(() ->
            {
            try
                {
                return f_service.clear(Requests.clear(f_sScopeName, f_sCacheName)).thenApply(e -> VOID).toCompletableFuture();
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
                CompletableFuture<Map<K, R>>           future   = new CompletableFuture<>();
                InvokeAllBiFunction<K, R>              function = new InvokeAllBiFunction<>(future);
                FutureStreamObserver<Entry, Map<K, R>> observer = new FutureStreamObserver<>(future, new HashMap<>(),
                                                                                             function);
                ByteString                             filter   = toByteString(AlwaysFilter.INSTANCE());
                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, filter, toByteString(processor)),
                                  observer);
                return future;
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
                CompletableFuture<Void>       future   = new CompletableFuture<>();
                BiFunction<Entry, Void, Void> function = (e, v) ->
                    {
                    try
                        {
                        Map.Entry<K, R> entry = new SimpleMapEntry<>(fromByteString(e.getKey()),
                                                                     fromByteString(e.getValue()));
                        callback.accept(entry);
                        }
                    catch (Throwable ex)
                        {
                        future.completeExceptionally(ex);
                        }
                    return null;
                    };
                FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
                ByteString                        filter   = toByteString(AlwaysFilter.INSTANCE());
                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, filter, toByteString(processor)),
                                  observer);
                return future;
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
                CompletableFuture<Void>       future   = new CompletableFuture<>();
                BiFunction<Entry, Void, Void> function = (e, v) ->
                    {
                    try
                        {
                        callback.accept(fromByteString(e.getKey()), fromByteString(e.getValue()));
                        }
                    catch (Throwable ex)
                        {
                        future.completeExceptionally(ex);
                        }
                    return null;
                    };
                FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
                ByteString                        filter   = toByteString(AlwaysFilter.INSTANCE());
                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, filter, toByteString(processor)),
                                  observer);
                return future;
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
                CompletableFuture<Void>       future   = new CompletableFuture<>();
                BiFunction<Entry, Void, Void> function = (e, v) ->
                    {
                    try
                        {
                        callback.accept(fromByteString(e.getKey()), fromByteString(e.getValue()));
                        }
                    catch (Throwable ex)
                        {
                        future.completeExceptionally(ex);
                        }
                    return null;
                    };
                FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
                Collection<ByteString>            keys     = colKeys.stream()
                        .map(this::toByteString)
                        .collect(Collectors.toList());

                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, keys, toByteString(processor)), observer);
                return future;
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            });
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(Filter filter,
                                                 InvocableMap.EntryProcessor<K, V, R> processor,
                                                 BiConsumer<? super K, ? super R> callback)
        {
        return executeIfActive(() ->
        {
            try
                {
                CompletableFuture<Void>       future   = new CompletableFuture<>();
                BiFunction<Entry, Void, Void> function = (e, v) ->
                    {
                    try
                        {
                        callback.accept(fromByteString(e.getKey()), fromByteString(e.getValue()));
                        }
                    catch (Throwable ex)
                        {
                        future.completeExceptionally(ex);
                        }
                    return VOID;
                    };
                FutureStreamObserver<Entry, Void> observer = new FutureStreamObserver<>(future, VOID, function);
                invokeAllInternal(Requests.invokeAll(f_sScopeName, f_sCacheName, f_sFormat, toByteString(filter), toByteString(processor)), observer);
                return future;
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
        return executeIfActive(() -> f_service.isEmpty(Requests.isEmpty(f_sScopeName, f_sCacheName))
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
        return executeIfActive(() -> f_service.putIfAbsent(Requests.putIfAbsent(f_sScopeName, f_sCacheName, f_sFormat,
                                                                                toByteString(key), toByteString(value)))
                .thenApplyAsync(this::valueFromBytesValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Void> putAll(Map<? extends K, ? extends V> map)
        {
        return putAllInternal(map).thenApply(v -> VOID);
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
        return executeIfActive(() -> f_service.replace(Requests.replace(f_sScopeName, f_sCacheName, f_sFormat,
                                                                        toByteString(key), toByteString(value)))
                .thenApplyAsync(this::valueFromBytesValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue)
        {
        return executeIfActive(() -> f_service.replaceMapping(
                Requests.replace(f_sScopeName, f_sCacheName, f_sFormat, toByteString(key),
                                 toByteString(oldValue), toByteString(newValue)))
                .thenApplyAsync(BoolValue::getValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Integer> size()
        {
        return executeIfActive(() -> f_service.size(Requests.size(f_sScopeName, f_sCacheName))
                .thenApply(Int32Value::getValue)
                .toCompletableFuture());
        }

    @Override
    public CompletableFuture<Collection<V>> values()
        {
        return executeIfActive(() -> CompletableFuture.completedFuture(new RemoteValues<>(this)));
        }

    @Override
    public CompletableFuture<Collection<V>> values(Filter filter, Comparator<? super V> comparator)
        {
        return executeIfActive(() -> valuesInternal(filter, comparator));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Helper method to obtain a {@link Collection} of {@link Entry} instances for the specified keys
     * as a {@link Stream}.
     *
     * @param colKeys  the keys to look up
     *
     * @return a {@link Stream} of {@link Entry} instances for the keys found in the cache
     */
    protected Stream<Entry> getAllInternal(Collection<? extends K> colKeys)
        {
        assertActive();
        if (colKeys.isEmpty())
            {
            return Stream.empty();
            }
        else
            {
            List<ByteString> keys = colKeys.stream()
                    .map(this::toByteString)
                    .collect(Collectors.toList());
            return f_service.getAll(Requests.getAll(f_sScopeName, f_sCacheName, f_sFormat, keys));
            }
        }

    /**
     * Helper method to obtain a {@link Map} of the keys/values found within the cache matching
     * the provided {@link Collection} of keys.
     *
     * @param colKeys  the keys to look up
     *
     * @return a {@link Map} containing keys/values for the keys found witin the cache
     */
    @SuppressWarnings("unchecked")
    protected Map<K, V> getAllInternalAsMap(Collection<? extends K> colKeys)
        {
        assertActive();
        return getAllInternal(colKeys)
                .map(e -> (Map.Entry<K, V>) new SimpleMapEntry<>(fromByteString(e.getKey()), fromByteString(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

    /**
     * Returns the scope name.
     *
     * @return the scope name
     */
    protected String getScopeName()
        {
        return f_sScopeName;
        }

    /**
     * Returns the cache name.
     *
     * @return the cache name
     */
    protected String getCacheName()
        {
        return f_sCacheName;
        }

    /**
     * Get the {@link CacheService} associated with this cache.
     * <p>
     * <em>NOTE: </em> this method always throws an UnsupportedOperationException();
     *
     * @return the {@link CacheService} associated wtih this cache
     */
    protected CacheService getCacheService()
        {
        // ToDo: implement this method when we know what a service will look like
        throw new UnsupportedOperationException("method not implemented");
        }

    /**
     * Helper method for getting a value from the cache.
     *
     * @param key           the key
     * @param defaultValue  the value to return if this cache doesn't contain the provided key
     *
     * @return the value within the Cache, or {@code defaultValue}
     */
    protected CompletableFuture<V> getInternal(Object key, V defaultValue)
        {
        return executeIfActive(() -> f_service.get(Requests.get(f_sScopeName, f_sCacheName, f_sFormat, toByteString(key)))
                .thenApplyAsync(optional -> this.valueFromOptionalValue(optional, defaultValue))
                .toCompletableFuture());
        }

    /**
     * Return the synchronous cache client.
     *
     * @return the the synchronous cache client
     */
    protected NamedCacheClient<K, V> getNamedCacheClient()
        {
        return f_synchronousCache;
        }

    /**
     * Helper method to perform invokeAll operations.
     *
     * @param request   the {@link InvokeAllRequest}
     * @param observer  the {@link StreamObserver}
     */
    protected void invokeAllInternal(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        assertActive();

        f_service.invokeAll(request, observer);
        }

    /**
     * Return {@code true} if the cache is still active.
     *
     * @return {@code true} if the cache is still active
     */
    protected CompletableFuture<Boolean> isActive()
        {
        return CompletableFuture.completedFuture(isActiveInternal());
        }

    /**
     * Return {@code true} if the cache has been released.
     *
     * @return {@code true} if the cache has been released
     */
    protected boolean isReleased()
        {
        return m_fReleased;
        }

    /**
     * Return {@code true} if the cache has been destroyed.
     *
     * @return {@code true} if the cache has been destroyed
     */
    protected boolean isDestroyed()
        {
        return m_fDestroyed;
        }

    /**
     * Helper method for cache active checks.
     *
     * @return {@code true} if the cache is still active
     */
    protected boolean isActiveInternal()
        {
        return !m_fReleased && !m_fDestroyed;
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
    protected CompletableFuture<V> putInternal(K key, V value, long cTtl)
        {
        return executeIfActive(() -> f_service.put(Requests.put(f_sScopeName, f_sCacheName, f_sFormat,
                                                                toByteString(key), toByteString(value), cTtl))
                .thenApplyAsync(this::valueFromBytesValue)
                .toCompletableFuture());
        }

    /**
     * Helper method for storing the contents of the provided map within the cache.
     *
     * @param map  the {@link Map} of key/value pairs to store in the cache.
     *
     * @return a {@link CompletableFuture}
     */
    protected CompletableFuture<Empty> putAllInternal(Map<? extends K, ? extends V> map)
        {
        return executeIfActive(() ->
        {
            try
                {
                List<Entry> entries = new ArrayList<>();
                for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
                    {
                    entries.add(Entry.newBuilder()
                                        .setKey(toByteString(entry.getKey()))
                                        .setValue(toByteString(entry.getValue())).build());
                    }
                return f_service.putAll(Requests.putAll(f_sScopeName, f_sCacheName, f_sFormat, entries)).toCompletableFuture();
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
    protected CompletableFuture<Void> release()
        {
        return executeIfActive(() -> releaseInternal(false));
        }

     protected <T, E> CompletableFuture<Void> removeIndex(ValueExtractor<? super T, ? extends E> valueExtractor)
        {
        return f_service.removeIndex(Requests.removeIndex(f_sScopeName, f_sCacheName, f_sFormat, toByteString(valueExtractor)))
                .thenApply(e -> VOID).toCompletableFuture();
        }

    /**
     * Remove the mapping for the given key, returning the associated value.
     *
     * @param key  key whose mapping is to be removed from the map
     *
     * @return the value associated with the given key, or {@code null} if no mapping existed
     */
    protected CompletableFuture<V> removeInternal(Object key)
        {
        return executeIfActive(() -> f_service.remove(Requests.remove(f_sScopeName, f_sCacheName, f_sFormat, toByteString(key)))
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
     *         whether or not the mapping was removed
     */
    protected CompletableFuture<Boolean> removeInternal(Object key, Object value)
        {
        return executeIfActive(() -> f_service.removeMapping(Requests.remove(f_sScopeName, f_sCacheName, f_sFormat,
                                                                             toByteString(key), toByteString(value)))
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
    protected CompletableFuture<Void> removeMapListener(MapListener<? super K, ? super V> mapListener)
        {
        return executeIfActive(() ->
            {
            // ToDo: implement this method
            throw new UnsupportedOperationException("not implemented");
            });
        }

    /**
     * Removes the {@link MapListener} associated with the specified key.
     *
     * @param mapListener  the {@link MapListener} to remove
     * @param key          the key the listener is associated with
     *
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    protected CompletableFuture<Void> removeMapListener(MapListener<? super K, ? super V> mapListener, K key)
        {
        return executeIfActive(() ->
            {
            // ToDo: implement this method
            throw new UnsupportedOperationException("not implemented");
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
    protected CompletableFuture<Void> removeMapListener(MapListener<? super K, ? super V> listener, Filter filter)
        {
        return executeIfActive(() ->
            {
            if (listener instanceof NamedCacheDeactivationListener && filter == null)
                {
                synchronized (this)
                    {
                    f_listCacheDeactivationListeners.remove(listener);
                    }
                return CompletableFuture.completedFuture(VOID);
                }

            // ToDo: implement this method
            throw new UnsupportedOperationException("not implemented");
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
     */
    protected Stream<InvocableMap.Entry<K, V>> stream(Filter<V> filter)
        {
        assertActive();
        throw new UnsupportedOperationException("method not implemented");
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
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    protected CompletableFuture<Void> truncate()
        {
        return executeIfActive(() -> f_service.truncate(Requests.truncate(f_sScopeName, f_sCacheName))
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
    protected CompletableFuture<Collection<V>> valuesInternal(Filter filter, Comparator comparator)
        {
        return this.values(filter).thenApply((colValues) ->
            {
            List<V> values = new ArrayList<>(colValues);
            values.sort(comparator);
            return values;
            });
        }

    /**
     * Helper method for {@link #containsKey(Object)} invocations.
     *
     * @param oKey  the key to check
     *
     * @return a {@link CompletableFuture} returning {@code true} if the cache contains the given key
     */
    protected CompletableFuture<Boolean> containsKeyInternal(Object oKey)
        {
        return executeIfActive(() -> f_service.containsKey(Requests.containsKey(f_sScopeName, f_sCacheName, f_sFormat,
                                                                                toByteString(oKey)))
                .thenApplyAsync(BoolValue::getValue)
                .toCompletableFuture());
        }

    /**
     * Helper method for confirm presence of the value within the cache.
     *
     * @param oValue  the value to check
     *
     * @return a {@link CompletableFuture} returning {@code true} if the cache contains the given value
     */
    protected CompletableFuture<Boolean> containsValue(Object oValue)
        {
        return executeIfActive(() -> f_service.containsValue(Requests.containsValue(f_sScopeName, f_sCacheName, f_sFormat,
                                                                                    toByteString(oValue)))
                .thenApplyAsync(BoolValue::getValue)
                .toCompletableFuture());
        }

    /**
     * Destroys the cache.
     *
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    protected CompletableFuture<Void> destroy()
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
     * Called by the constructor to initialize event support.
     */
    protected void initEvents()
        {
        MapListenerRequest request = MapListenerRequest.newBuilder()
                .setScope(f_sScopeName)
                .setCache(f_sCacheName)
                .setUid(UUID.randomUUID().toString())
                .setSubscribe(true)
                .setFormat(f_sFormat)
                .setType(MapListenerRequest.RequestType.INIT)
                .build();

        m_evtResponseObserver = new EventStreamObserver(request.getUid());
        m_evtRequestObserver  = f_service.events(m_evtResponseObserver);
        m_listenerSupport     = new MapListenerSupport();
        m_aEvtFilter          = new SparseArray<>();
        // initialise the bi-directional stream so that this client will receive
        // destroy and truncate events
        m_evtRequestObserver.onNext(request);
        // wait for the init request to complete
        m_evtResponseObserver.whenSubscribed().toCompletableFuture().join();
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
     * Obtain a page of cache keys.
     *
     * @param cookie  an opaque cooke used to determine the current page
     *
     * @return a page of cache keys
     */
    protected Stream<BytesValue> getKeysPage(BytesValue cookie)
        {
        assertActive();
        ByteString s = cookie == null ? null : cookie.getValue();
        return f_service.nextKeySetPage(Requests.page(f_sScopeName, f_sCacheName, f_sFormat, s));
        }

    /**
     * Obtain a page of cache entries.
     *
     * @param cookie  an opaque cooke used to determine the current page
     *
     * @return a page of cache entries
     */
    protected Stream<EntryResult> getEntriesPage(ByteString cookie)
        {
        assertActive();
        return f_service.nextEntrySetPage(Requests.page(f_sScopeName, f_sCacheName, f_sFormat, cookie));
        }

    /**
     * Determine whether en entry exists in the cache with the specified key and value.
     *
     * @param key    the cache key
     * @param value  the cache value
     *
     * @return a page of cache keys
     */
    protected boolean containsEntry(K key, V value)
        {
        assertActive();
        try
            {
            ContainsEntryRequest       request   = Requests.containsEntry(f_sScopeName, f_sCacheName, f_sFormat, toByteString(key),
                                                                          toByteString(value));
            CompletionStage<BoolValue> stage     = f_service.containsEntry(request);
            BoolValue                  boolValue = stage.toCompletableFuture().get();

            return boolValue != null && boolValue.getValue();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    /**
     * A utility method to serialize an Object to {@link BytesValue}.
     *
     * @param obj  the object to convert to {@link BytesValue}.
     *
     * @return the {@link BytesValue} for the specified object
     */
    protected BytesValue toBytesValue(Object obj)
        {
        return BytesValue.of(toByteString(obj));
        }

    /**
     * A utility method to serialize an Object to {@link ByteString}.
     *
     * @param obj  the object to convert to {@link ByteString}.
     *
     * @return the {@link ByteString} for the specified object
     */
    protected ByteString toByteString(Object obj)
        {
        return UnsafeByteOperations.unsafeWrap(ExternalizableHelper.toBinary(obj, f_serializer).toByteBuffer());
        }

    /**
     * A utility method to deserialize an {@link OptionalValue} to an Object.
     * <p>
     * If no value is present in the {@link OptionalValue} then {@code null}
     * is returned.
     *
     * @param optional  the {@link OptionalValue} to deserialize an Object.
     *
     * @return an object from the specified {@link BytesValue}
     */
    protected V valueFromOptionalValue(OptionalValue optional)
        {
        return valueFromOptionalValue(optional, null);
        }

    /**
     * A utility method to deserialize an {@link OptionalValue} to an Object.
     * <p>
     * If no value is present in the {@link OptionalValue} then the default
     * value is returned.
     *
     * @param optional      the {@link OptionalValue} to deserialize an Object.
     * @param defaultValue  the value to return if the {link @OptionalValue} does
     *                      not contain a value
     *
     * @return an object from the specified {@link BytesValue}
     */
    protected V valueFromOptionalValue(OptionalValue optional, V defaultValue)
        {
        if (optional.getPresent())
            {
            return fromByteString(optional.getValue());
            }
        return defaultValue;
        }

    /**
     * A utility method to deserialize a {@link BytesValue} to an Object.
     *
     * @param bv  the {@link BytesValue} to deserialize an Object.
     *
     * @return an object from the specified {@link BytesValue}
     */
    protected V valueFromBytesValue(BytesValue bv)
        {
        return fromBytesValue(bv);
        }

    /**
     * A utility method to deserialize a {@link BytesValue} to an Object.
     *
     * @param bytes  the {@link BytesValue} to deserialize an Object.
     *
     * @return an object from the specified {@link BytesValue}
     */
    protected <T> T fromBytesValue(BytesValue bytes)
        {
        return BinaryHelper.fromBytesValue(bytes, f_serializer);
        }

    /**
     * A utility method to deserialize a {@link ByteString} to an Object.
     *
     * @param bytes  the {@link ByteString} to deserialize an Object.
     *
     * @return an object from the specified {@link ByteString}
     */
    protected <T> T fromByteString(ByteString bytes)
        {
        return BinaryHelper.fromByteString(bytes, f_serializer);
        }

    protected  <T> CompletableFuture<T> failedFuture(Throwable t)
        {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
        }

    /**
     * Assert that this {@link AsyncNamedCacheClient} is active.
     *
     * @throws IllegalStateException if this {@link AsyncNamedCacheClient}
     *                               is not active
     */
    protected void assertActive()
        {
        if (m_fReleased || m_fDestroyed)
            {
            String reason = m_fDestroyed ? "destroyed" : "released";
            throw new IllegalStateException("remote cache '" + f_sCacheName + "' has been " + reason);
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
                return supplier.get();
                }
            catch (Throwable t)
                {
                return failedFuture(t);
                }
            }
        else
            {
            String reason = m_fDestroyed ? "destroyed" : "released";
            return failedFuture(new IllegalStateException("remote cache '" + f_sCacheName + "' has been " + reason));
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
    protected synchronized CompletableFuture<Void> releaseInternal(boolean destroy)
        {
        CompletableFuture<Void> future;

        // close the events bi-directional channel and any event listeners
        if (m_evtRequestObserver != null)
            {
            m_evtRequestObserver.onCompleted();
            }

        if (!m_fDestroyed && !m_fReleased)
            {
            if (destroy)
                {
                m_fDestroyed = true;
                future = f_service.destroy(Requests.destroy(f_sScopeName, f_sCacheName)).thenApply(e -> VOID).toCompletableFuture();
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

        return future.handleAsync((v, err) -> {
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
                t.printStackTrace();
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
                t.printStackTrace();
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
    protected synchronized void addDeactivationListener(DeactivationListener<AsyncNamedCacheClient<? super K, ?
            super V>> listener)
        {
        assertActive();
        if (listener != null)
            {
            f_listDeactivationListeners.add(listener);
            }
        }

    /**
     * Add a {@link NamedCacheDeactivationListener} that will be notified when this
     * {@link AsyncNamedCacheClient} is released or destroyed.
     *
     * @param listener  the listener to add
     */
    protected synchronized void addDeactivationListener(NamedCacheDeactivationListener listener)
        {
        if (listener != null)
            {
            if (m_fReleased || m_fDestroyed)
                {
                listener.entryDeleted(createDeactivationEvent(m_fDestroyed));
                }
            else
                {
                f_listCacheDeactivationListeners.add(listener);
                }
            }
        }

    /**
     * Remove a {@link DeactivationListener}.
     *
     * @param listener  the listener to remove
     */
    protected synchronized void removeDeactivationListener(DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>> listener)
        {
        if (listener != null)
            {
            f_listDeactivationListeners.remove(listener);
            }
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
    protected <T, E> CompletableFuture<Void> addIndex(ValueExtractor<? super T, ? extends E> extractor,
                                                      boolean fOrdered, Comparator<? super E> comparator)
        {
        return executeIfActive(() ->
                               {
                               ByteString serializedExtractor = toByteString(extractor);
                               if (comparator == null)
                                   {
                                   return f_service.addIndex(Requests.addIndex(f_sScopeName, f_sCacheName, f_sFormat, serializedExtractor, fOrdered))
                                           .thenApply(e -> VOID).toCompletableFuture();
                                   }
                               else
                                   {
                                   return f_service.addIndex(Requests.addIndex(f_sScopeName, f_sCacheName, f_sFormat, serializedExtractor,
                                                                               fOrdered, toByteString(comparator)))
                                           .thenApply(e -> VOID).toCompletableFuture();
                                   }
                               });
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
    protected CompletableFuture<Void> addMapListener(MapListener<? super K, ? super V> mapListener)
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
    protected CompletableFuture<Void> addMapListener(MapListener<? super K, ? super V> mapListener, K key, boolean fLite)
        {
        return executeIfActive(() ->
                               {
                               try
                                   {
                                   return addKeyMapListener(mapListener, key, fLite);
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
    protected CompletableFuture<Void> addMapListener(MapListener<? super K, ? super V> mapListener, Filter<?> filter,
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

        CompletableFuture<Void> future  = new CompletableFuture<>();
        MapListenerSupport      support = getMapListenerSupport();

        boolean first = support.addListenerWithCheck(listener, key, fLite);
        boolean priming = MapListenerSupport.isPrimingListener(listener);

        // "priming" request should be sent regardless
        if (first || priming)
            {
            String uid = "";
            try
                {
                MapListenerRequest request = Requests
                        .addKeyMapListener(f_sScopeName, f_sCacheName, f_sFormat, toByteString(key),
                                           fLite, priming, ByteString.EMPTY);
                uid = request.getUid();
                f_mapFuture.put(uid, future);
                m_evtRequestObserver.onNext(request);
                }
            catch (RuntimeException e)
                {
                synchronized (support)
                    {
                    support.removeListener(listener, key);
                    f_mapFuture.remove(uid);
                    }
                future.completeExceptionally(e);
                }
            }
        else
            {
            // we're not actually adding the listener to the server so complete the future now.
            future.complete(VOID);
            }

        return future;
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
        if (listener instanceof NamedCacheDeactivationListener)
            {
            synchronized (this)
                {
                f_listCacheDeactivationListeners.add((NamedCacheDeactivationListener) listener);
                }
            return CompletableFuture.completedFuture(VOID);
            }

        if (listener instanceof MapTriggerListener)
            {
            MapTriggerListener triggerListener = (MapTriggerListener) listener;
            return addRemoteFilterListener(ByteString.EMPTY, 0L, fLite, toByteString(triggerListener.getTrigger()));
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
            future = addRemoteFilterListener(toByteString(filter), filterId, fLite, ByteString.EMPTY);
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
     * Sends the serialized {@link Filter} for registration with the cache server.
     *
     * @param filterBytes   the serialized bytes of the {@link Filter}
     * @param nFilterId     the ID of the {@link Filter}
     * @param fLite         {@code true} to indicate that the {@link MapEvent} objects do
     *                      not have to include the OldValue and NewValue
     *                      property values in order to allow optimizations
     * @param triggerBytes  the serialized bytes of the {@link MapTriggerListener}; pass {@link ByteString#EMPTY}
     *                      if there is no trigger listener.
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    protected CompletableFuture<Void> addRemoteFilterListener(ByteString filterBytes,
                                                              long nFilterId, boolean fLite, ByteString triggerBytes)
        {
        String uid = "";
        CompletableFuture<Void> future = new CompletableFuture<>();
        try
            {
            MapListenerRequest request = Requests
                    .addFilterMapListener(f_sScopeName, f_sCacheName, f_sFormat, filterBytes, nFilterId, fLite, false, triggerBytes);
            uid = request.getUid();
            f_mapFuture.put(uid, future);
            m_evtRequestObserver.onNext(request);
            }
        catch (RuntimeException e)
            {
            f_mapFuture.remove(uid);
            future.completeExceptionally(e);
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
     * Dispatch the received {@link MapEventResponse} for processing by local listeners.
     * <p>
     * This method is taken from code in the TDE RemoteNamedCache.
     *
     * @param response  the {@link MapEventResponse} to process
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void dispatch(MapEventResponse response)
        {
        List<Long>          listFilterIds  = response.getFilterIdsList();
        int                 nEventId       = response.getId();
        Object              oKey           = fromByteString(response.getKey());
        Object              oValueOld      = fromByteString(response.getOldValue());
        Object              oValueNew      = fromByteString(response.getNewValue());
        boolean             fSynthetic     = response.getSynthetic();
        boolean             fPriming       = response.getPriming();
        MapListenerSupport  support        = getMapListenerSupport();
        int                 cFilters       = listFilterIds == null ? 0 : listFilterIds.size();
        TransformationState transformState = TransformationState.valueOf(response.getTransformationState().toString());
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
            // there is also a valid scenario of a client thread removing an asyn
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

    // ----- inner class: EventTask -----------------------------------------

    /**
     * A simple {@link Runnable} to dispatch an event to a listener.
     */
    @SuppressWarnings("rawtypes")
    static class EventTask
            implements Runnable
        {
        /**
         * Create an {@link EventTask}.
         *
         * @param event     the event to dispatch
         * @param listener  the listener to dispatch the event to
         */
        EventTask(CacheEvent<?, ?> event, MapListener listener)
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

    // ----- inner class: FutureStreamObserver ------------------------------

    /**
     * A {@link StreamObserver} that completes a {@link CompletableFuture}.
     *
     * @param <T>  the type of element observed
     * @param <R>  the type of the {@link CompletableFuture}
     */
    protected static class FutureStreamObserver<T, R>
            implements StreamObserver<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code FutureStreamObserver}.
         *
         * @param future         the {@link CompletableFuture} to notify
         * @param initialResult  the initial result
         * @param function       the function to call for each entry within the stream
         */
        protected FutureStreamObserver(CompletableFuture<R> future, R initialResult, BiFunction<T, R, R> function)
            {
            f_future = future;
            m_result = initialResult;
            f_function = function;
            }

        // ----- StreamObserver interface -----------------------------------

        @Override
        public void onNext(T t)
            {
            if (!f_future.isDone())
                {
                m_result = f_function.apply(t, m_result);
                }
            }

        @Override
        public void onError(Throwable throwable)
            {
            if (!f_future.isDone())
                {
                f_future.completeExceptionally(throwable);
                }
            }

        @Override
        public void onCompleted()
            {
            if (!f_future.isDone())
                {
                f_future.complete(m_result);
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link CompletableFuture} to notify.
         */
        protected final CompletableFuture<R> f_future;

        /**
         * Callback function for entry processing.
         */
        protected final BiFunction<T, R, R> f_function;

        /**
         * The result after the callback is applied.
         */
        protected R m_result;
        }

    // ----- inner class: InvokeAllBiFunction -------------------------------

    /**
     * A {@link BiFunction} that completes a {@link CompletableFuture}.
     *
     * @param <Kf>  key type
     * @param <Rf>  result type
     */
    protected class InvokeAllBiFunction<Kf, Rf>
            implements BiFunction<Entry, Map<Kf, Rf>, Map<Kf, Rf>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code InvokeAllBiFunction}.
         *
         * @param future  the {@link CompletableFuture} to notify
         */
        protected InvokeAllBiFunction(CompletableFuture<Map<Kf, Rf>> future)
            {
            f_future = future;
            }

        // ----- BiFunction interface ---------------------------------------

        @Override
        public Map<Kf, Rf> apply(Entry e, Map<Kf, Rf> m)
            {
            try
                {
                m.put(fromByteString(e.getKey()), fromByteString(e.getValue()));
                return m;
                }
            catch (Throwable ex)
                {
                f_future.completeExceptionally(ex);
                }
            return null;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link CompletableFuture} to notify
         */
        protected final CompletableFuture<Map<Kf, Rf>> f_future;
        }

    // ----- EventStreamObserver -------------------------------------------
    /**
     * A {@code EventStreamObserver} that processes {@link MapListenerResponse)s.
     */
    protected class EventStreamObserver
            implements StreamObserver<MapListenerResponse>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new EventStreamObserver
         *
         * @param uid  the event ID to observe
         */
        protected EventStreamObserver(String uid)
            {
            f_sUid   = Objects.requireNonNull(uid);
            f_future = new CompletableFuture<>();
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
            CompletableFuture<Void> future;
            switch (response.getResponseTypeCase())
                {
                case SUBSCRIBED:
                    MapListenerSubscribedResponse subscribed  = response.getSubscribed();
                    String                        responseUid = subscribed.getUid();
                    if (f_sUid.equals(responseUid))
                        {
                        f_future.complete(VOID);
                        }
                    future = f_mapFuture.remove(responseUid);
                    if (future != null)
                        {
                        future.complete(null);
                        }
                    break;
                case UNSUBSCRIBED:
                    MapListenerUnsubscribedResponse unsubscribed = response.getUnsubscribed();
                    future = f_mapFuture.remove(unsubscribed.getUid());
                    if (future != null)
                        {
                        future.complete(null);
                        }
                    break;
                case EVENT:
                    dispatch(response.getEvent());

                    break;
                case ERROR:
                    MapListenerErrorResponse error = response.getError();
                    future = f_mapFuture.remove(error.getUid());
                    if (future != null)
                        {
                        future.completeExceptionally(new RuntimeException(error.getMessage()));
                        }
                    break;
                case DESTROYED:
                    if (response.getDestroyed().getCache().equals(f_sCacheName))
                        {
                        synchronized (this)
                            {
                            if (isActiveInternal())
                                {
                                m_fDestroyed = true;
                                releaseInternal(true);
                                }
                            }
                        }
                    break;
                case TRUNCATED:
                    if (response.getTruncated().getCache().equals(f_sCacheName))
                        {
                        CacheEvent<?, ?> evt = createDeactivationEvent(/*destroyed*/ false);
                        for (NamedCacheDeactivationListener listener : f_listCacheDeactivationListeners)
                            {
                            try
                                {
                                listener.entryUpdated(evt);
                                }
                            catch (Throwable t)
                                {
                                t.printStackTrace();
                                }
                            }

                        }
                    break;
                case RESPONSETYPE_NOT_SET:
                    LOGGER.log(Level.INFO, "Received unexpected event without response type! ");
                    break;
                default:
                    LOGGER.log(Level.INFO, "Received unexpected event " + response.getEvent());
                }
            }

        @Override
        public void onError(Throwable t)
            {
            LOGGER.log(Level.SEVERE, t, () -> "Caught exception handling onError");
            }

        @Override
        public void onCompleted()
            {
            }

        // ----- data members -----------------------------------------------

        /**
         * The event ID.
         */
        protected final String f_sUid;

        /**
         * The {@link CompletableFuture} to notify
         */
        protected final CompletableFuture<Void> f_future;
        }

    // ----- inner class: WrapperDeactivationListener -----------------------

    /**
     * A {@link DeactivationListener} that wraps client {@link NamedCacheDeactivationListener}.
     */
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

    // ----- Builder --------------------------------------------------------

    /**
     * A {@link io.helidon.common.Builder} of AsyncNamedCacheClient instances.
     *
     * @param <K>  the type of the cache keys
     * @param <V>  the type of the cache values
     */
    public static class Builder<K, V>
            implements io.helidon.common.Builder<AsyncNamedCacheClient<K, V>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs the {@code Builder} using the provided name and configuration.
         *
         * @param sScopeName  the scope name for requests
         * @param sCacheName  the cache name
         * @param config      the client configuration
         */
        protected Builder(String sScopeName, String sCacheName, Config config)
            {
            f_sScopeName = sScopeName == null ? Scope.DEFAULT : sScopeName;
            f_sCacheName = sCacheName;
            f_config     = config;
            try
                {
                m_beanManager = CDI.current().getBeanManager();
                }
            catch (IllegalStateException e)
                {
                m_beanManager = null;
                }
            }

        /**
         * Set the {@link Channel} to use.
         *
         * @param channel  the {@link Channel} to use
         *
         * @return this {@link Builder}
         */
        public Builder<K, V> channel(Channel channel)
            {
            m_channel = channel;
            return this;
            }

        /**
         * Set the optional {@link BeanManager} to use in a CDI environment.
         *
         * @param beanManager  the optional {@link BeanManager} to use in a CDI environment
         *
         * @return this {@link Builder}
         */
        public Builder<K, V> beanManager(BeanManager beanManager)
            {
            this.m_beanManager = beanManager;
            return this;
            }

        /**
         * Set the {@link Serializer} to use.
         * <p>
         * The serialization format will be taken from the
         * {@link Serializer#getName()} value
         *
         * @param serializer  the {@link Serializer} to use
         *
         * @return this {@link Builder}
         */
        public Builder<K, V> serializer(Serializer serializer)
            {
            m_serializer = serializer;
            m_format     = serializer == null ? null : serializer.getName();
            return this;
            }

        /**
         * Set the {@link Serializer} to use.
         *
         * @param serializer  the {@link Serializer} to use
         * @param sFormat     the serialization format
         *
         * @return this {@link Builder}
         */
        public Builder<K, V> serializer(Serializer serializer, String sFormat)
            {
            m_serializer = serializer;
            m_format     = sFormat;
            return this;
            }

        /**
         * Set the {@link NamedCacheService} proxy to be used by the cache.
         * 
         * @param proxy  the {@link NamedCacheService} proxy to use
         *
         * @return this {@link Builder}
         */
        public Builder<K, V> serviceProxy(NamedCacheService proxy)
            {
            m_serviceProxy = proxy;
            return this;
            }

        /**
         * Set the {@link Executor} to be used by the cache for
         * dispatching events.
         * 
         * @param executor  the {@link Executor} to be used by the cache
         *
         * @return this {@link Builder}
         */
        public Builder<K, V> executor(Executor executor)
            {
            m_executor = executor;
            return this;
            }

        @Override
        public AsyncNamedCacheClient<K, V> build()
            {
            return new AsyncNamedCacheClient<>(this);
            }

        // ----- helper methods ---------------------------------------------

        private Serializer buildSerializer(String format)
            {
            SerializerProducer serializerProducer = SerializerProducer.builder().beanManager(m_beanManager).build();
            return serializerProducer.getNamedSerializer(format, Base.getContextClassLoader());
            }

        /**
         * Return the {@link Serializer}, if any.
         *
         * @return the {@link Serializer}, if any
         */
        protected Serializer ensureSerializer()
            {
            if (m_serializer == null)
                {
                m_serializer = buildSerializer(getFormat());
                }
            return m_serializer;
            }

        /**
         * Return the serialization format.
         *
         * @return the serialization format
         */
        protected String getFormat()
            {
            return m_format == null ? "java" : m_format;
            }

        /**
         * Return the cache name, if any.
         *
         * @return the cache name, if any
         */
        protected String getCacheName()
            {
            return f_sCacheName;
            }

        /**
         * Return the scope name, if any.
         *
         * @return the scope name, if any
         */
        protected Optional<String> getScopeName()
            {
            return Optional.ofNullable(f_sScopeName);
            }

        /**
         * Obtain the {@link Executor} to be used by the cache.
         *
         * @return  the {@link Executor} to be used by the cache
         */
        protected Executor ensureExecutor()
            {
            if (m_executor == null)
                {
                m_executor = Executors.newSingleThreadExecutor();
                }
            return m_executor;
            }

        /**
         * Return the gRPC {@link Channel} to use.
         * 
         * @return  the gRPC {@link Channel} to use
         */
        protected Channel ensureChannel() 
            {
            if (m_channel == null)
                {
                m_channel = GrpcChannelsProvider.create(f_config).channel("default");
                }
            return m_channel;
            }

        /**
         * Create the gRPC service proxy.
         * 
         * @return the gRPC service proxy
         */
        protected NamedCacheService ensureServiceProxy()
            {
            if (m_serviceProxy == null) 
                {
                m_serviceProxy = GrpcProxyBuilder.create(ensureChannel(), NamedCacheService.class).build();
                }
            return m_serviceProxy;
            }

        // ----- data members -----------------------------------------------

        /**
         * The scope name.
         */
        protected final String f_sScopeName;

        /**
         * The cache name.
         */
        protected final String f_sCacheName;

        /**
         * The client configuration.
         */
        protected final Config f_config;

        /**
         * The serialization format.
         */
        protected String m_format;

        /**
         * The {@link Serializer} for data marshalling.
         */
        protected Serializer m_serializer;

        /**
         * The gRPC communications channel.
         */
        protected Channel m_channel;

        /**
         * The CDI {@link BeanManager}.
         */
        protected BeanManager m_beanManager;

        /**
         * The {@link NamedCacheService} proxy;
         */
        protected NamedCacheService m_serviceProxy;

        /**
         * The {@link Executor} to be used by the cache.
         */
        protected Executor m_executor;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Java logging.
     */
    protected static final Logger LOGGER = Logger.getLogger(AsyncNamedCacheClient.class.getName());

    /**
     * A constant value to use for {@link Void} values.
     */
    protected static final Void VOID = null;

    // ----- data members ---------------------------------------------------

    /**
     * The scope name to use for requests.
     */
    protected final String f_sScopeName;

    /**
     * The cache name.
     */
    protected final String f_sCacheName;

    /**
     * The {@link NamedCacheService} to delegate calls.
     */
    protected final NamedCacheService f_service;

    /**
     * The {@link Serializer} to use.
     */
    protected final Serializer f_serializer;

    /**
     * The serialization format.
     */
    protected final String f_sFormat;

    /**
     * The synchronous version of this client.
     */
    protected final NamedCacheClient<K, V> f_synchronousCache;

    /**
     * A flag indicating whether this {@link AsyncNamedCacheClient} has been released.
     */
    protected boolean m_fReleased;

    /**
     * A flag indicating whether this {@link AsyncNamedCacheClient} has been destroyed.
     */
    protected boolean m_fDestroyed;

    /**
     * The list of {@link DeactivationListener} to be notified when this {@link AsyncNamedCacheClient}
     * is released or destroyed.
     */
    protected final List<DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>>> f_listDeactivationListeners;

    /**
     * The list of {@link NamedCacheDeactivationListener} instances added to this client.
     */
    protected final List<NamedCacheDeactivationListener> f_listCacheDeactivationListeners = new ArrayList<>();

    /**
     * The {@link Executor} to use to dispatch events.
     */
    protected final Executor f_executor;

    /**
     * The client channel for events observer.
     */
    protected StreamObserver<MapListenerRequest> m_evtRequestObserver;

    /**
     * The event response observer.
     */
    protected EventStreamObserver m_evtResponseObserver;

    /**
     * The map listener support.
     */
    protected MapListenerSupport m_listenerSupport;

    /**
     * The array of filter.
     */
    protected LongArray<Filter<?>> m_aEvtFilter;

    /**
     * The map of future keyed by request id.
     */
    protected final Map<String, CompletableFuture<Void>> f_mapFuture = new ConcurrentHashMap<>();
    }
