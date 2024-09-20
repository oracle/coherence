/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.internal.util.processor.BinaryProcessors;
import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import java.util.Collection;
import java.util.Map;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import static com.tangosol.internal.util.VersionHelper.VERSION_14_1_1_2206_6;
import static com.tangosol.internal.util.VersionHelper.VERSION_14_1_2_0;
import static com.tangosol.internal.util.VersionHelper.VERSION_23_09;
import static com.tangosol.internal.util.VersionHelper.isPatchCompatible;
import static com.tangosol.internal.util.VersionHelper.isVersionCompatible;

/**
 * An {@link com.tangosol.net.AsyncNamedCache} that wraps a distributed cache.
 * <p/>
 * This implementation uses more efficient binary processors wherever possible
 * to make use of the {@link com.tangosol.util.BinaryEntry} support in a distributed
 * cache.
 * 
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 */
public class DistributedAsyncNamedCache<K, V>
        extends DefaultAsyncNamedCache<K, V>
    {
    /**
     * Create a {@link DistributedAsyncNamedCache}.
     *
     * @param cache  the distributed {@link NamedCache} this async cache wraps
     */
    public DistributedAsyncNamedCache(NamedCache<K, V> cache)
        {
        this(cache, null);
        }

    /**
     * Create a {@link DistributedAsyncNamedCache}.
     *
     * @param cache    the distributed {@link NamedCache} this async cache wraps
     * @param options  the async cache options
     */
    @SuppressWarnings("unchecked")
    public DistributedAsyncNamedCache(NamedCache<K, V> cache, Option[] options)
        {
        super(cache, options);
        CacheService             service = cache.getCacheService();
        BackingMapManagerContext context = service.getBackingMapManager().getContext();
        f_keyFromInternalConverter   = context.getKeyFromInternalConverter();
        f_keyToInternalConverter     = context.getKeyToInternalConverter();
        f_valueFromInternalConverter = context.getValueFromInternalConverter();
        f_valueToInternalConverter   = context.getValueToInternalConverter();
        }

    // ---- AsyncNamedMap interface -----------------------------------------

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<V> get(K key)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.get();
        CompletableFuture<Binary>   future    = invoke(key, processor);
        return future.thenApply(f_valueFromInternalConverter::convert);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<Map<K, V>> getAll(Collection<? extends K> colKeys)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.get();
        return invokeAll(colKeys, processor);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<Void> getAll(Collection<? extends K> colKeys,
                                          BiConsumer<? super K, ? super V> callback)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.get();
        return invokeAll(colKeys, processor,
                entry -> callback.accept(entry.getKey(), (V) entry.getValue()));
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<Void> getAll(Collection<? extends K> colKeys,
                                          Consumer<? super Map.Entry<? extends K, ? extends V>> callback)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.get();
        return invokeAll(colKeys, processor, callback);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<Map<K, V>> getAll(Filter<?> filter)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.get();
        CompletableFuture<Map<Binary, Binary>> future = invokeAll(filter, processor);
        return future.thenApply(mapBinary ->
                ConverterCollections.getMap(mapBinary, f_keyFromInternalConverter, f_keyToInternalConverter,
                        f_valueFromInternalConverter, f_valueToInternalConverter));
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<Void> getAll(Filter<?> filter,
                                          BiConsumer<? super K, ? super V> callback)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.get();
        return invokeAll(filter, processor,
                entry -> callback.accept(entry.getKey(), (V) entry.getValue()));
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<Void> getAll(Filter<?> filter,
                                          Consumer<? super Map.Entry<? extends K, ? extends V>> callback)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.get();
        return invokeAll(filter, processor, callback);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<V> getOrDefault(K key, V valueDefault)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.get();
        return invoke(key, processor)
            .thenApply(bin ->
                {
                if (bin != null)
                    {
                    return f_valueFromInternalConverter.convert((Binary) bin);
                    }
                else
                    {
                    return valueDefault;
                    }
                });
        }

    @Override
    public CompletableFuture<Void> put(K key, V value)
        {
        return put(key, value, CacheMap.EXPIRY_DEFAULT);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<Void> put(K key, V value, long cMillis)
        {
        Binary binary = f_valueToInternalConverter.convert(value);
        InvocableMap.EntryProcessor processor;
        if (isBinaryProcessorCompatible())
            {
            processor = BinaryProcessors.blindPut(binary, cMillis);
            return invoke(key, processor);
            }
        else
            {
            processor = BinaryProcessors.put(binary, cMillis);
            return invoke(key, processor).thenAccept(ANY);
            }
        }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<Void> putAll(Map<? extends K, ? extends V> map)
        {
        Map<Binary, Binary> mapBinary = ConverterCollections.getMap((Map<K, V>) map, f_keyToInternalConverter,
                f_keyFromInternalConverter, f_valueToInternalConverter, f_valueFromInternalConverter);
        InvocableMap.EntryProcessor processor = BinaryProcessors.putAll(mapBinary);
        return invokeAll(map.keySet(), processor).thenAccept(ANY);
        }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<Void> putAll(Map<? extends K, ? extends V> map, long cMillis)
        {
        if (isBinaryProcessorCompatible())
            {
            Map<Binary, Binary> mapBinary = ConverterCollections.getMap((Map<K, V>) map, f_keyToInternalConverter,
                    f_keyFromInternalConverter, f_valueToInternalConverter, f_valueFromInternalConverter);
            InvocableMap.EntryProcessor processor = BinaryProcessors.putAll(mapBinary, cMillis);
            return invokeAll(map.keySet(), processor).thenAccept(ANY);
            }
        CacheService service  = getNamedCache().getService();
        int          nVersion = service.getMinimumServiceVersion();
        throw new UnsupportedOperationException("the whole cluster is not running a compatible version to execute " +
                "this method (version=\"" + VersionHelper.toVersionString(nVersion, true) +
                "\" encoded=" + nVersion + ")");
        }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<V> putIfAbsent(K key, V value)
        {
        Binary                      binary    = f_valueToInternalConverter.convert(value);
        InvocableMap.EntryProcessor processor = BinaryProcessors.putIfAbsent(binary, CacheMap.EXPIRY_DEFAULT);
        return invoke(key, processor)
                .thenApply(bin -> bin == null ? null : f_valueFromInternalConverter.convert((Binary) bin));
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CompletableFuture<V> remove(K key)
        {
        InvocableMap.EntryProcessor processor = BinaryProcessors.remove();
        CompletableFuture<Binary>   future    = invoke(key, processor);
        return future.thenApply(f_valueFromInternalConverter::convert);
        }

    @Override
    public CompletableFuture<Void> removeAll(Collection<? extends K> colKeys)
        {
        if (isBinaryProcessorCompatible())
            {
            return invokeAll(colKeys, CacheProcessors.removeWithoutResults()).thenAccept(ANY);
            }
        return invokeAll(colKeys, CacheProcessors.removeBlind()).thenAccept(ANY);
        }

    @Override
    public CompletableFuture<Void> removeAll(Filter<?> filter)
        {
        if (isBinaryProcessorCompatible())
            {
            return invokeAll(filter, CacheProcessors.removeWithoutResults()).thenAccept(ANY);
            }
        return invokeAll(filter, CacheProcessors.removeBlind()).thenAccept(ANY);
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Determine whether the service members are all compatible with changes
     * made in COH-28060 to use binary processors within async API.
     *
     * @return {@code true} if the service members are all compatible with changes
     *          made in COH-28060 to use binary processors within async API
     */
    protected boolean isBinaryProcessorCompatible()
        {
        return m_cache.getService().isVersionCompatible(IS_BINARY_PROCESSOR_COMPATIBLE);
        }
    
    /**
     * Determine whether the specified version is compatible with changes
     * made in COH-28060.
     *
     * @return {@code true} if the specified version is compatible with changes
     *          made in COH-28060
     */
    protected static boolean isBinaryProcessorCompatible(int nVersion)
        {
        // >= 23.09.0 or >= 14.1.1.2206.6 or >= 14.1.2
        return isVersionCompatible(VERSION_23_09, nVersion)
                || isPatchCompatible(VERSION_14_1_2_0, nVersion)
                || isPatchCompatible(VERSION_14_1_1_2206_6, nVersion);
        }

    // ----- constants ------------------------------------------------------

    /**
     * A version compatibility predicate to assert that members of a cache service are all on
     * a version that supports binary entry processors introduced in COH-28060.
     * See {@link CacheService#isVersionCompatible(IntPredicate)}
     */
    public static final IntPredicate IS_BINARY_PROCESSOR_COMPATIBLE = DistributedAsyncNamedCache::isBinaryProcessorCompatible;

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Converter} to use to convert a {@link Binary} value to a cache key.
     */
    private final Converter<Binary, K> f_keyFromInternalConverter;

    /**
     * The {@link Converter} to use to convert a cache key to a {@link Binary} value.
     */
    private final Converter<K, Binary> f_keyToInternalConverter;

    /**
     * The {@link Converter} to use to convert a {@link Binary} value to a cache value.
     */
    private final Converter<Binary, V> f_valueFromInternalConverter;

    /**
     * The {@link Converter} to use to convert a cache value to a {@link Binary} value.
     */
    private final Converter<V, Binary> f_valueToInternalConverter;
    }
