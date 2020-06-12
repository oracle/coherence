/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.util.Options;

import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.AsynchronousAgent;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.comparator.EntryComparator;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.function.Remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Asynchronous {@link NamedMap}.
 *
 * @param <K>  the type of the map entry keys
 * @param <V>  the type of the map entry values
 *
 * @author Aleks Seovic  2020.06.06
 *
 * @since 20.06
 */
public interface AsyncNamedMap<K, V>
    {
    /**
     * Return the {@link NamedCache} instance this {@code AsyncNamedCache} is
     * based on.
     *
     * @return the {@link NamedCache} instance this {@code AsyncNamedCache} is
     *         based on
     */
    public NamedMap<K, V> getNamedMap();

    // ---- Asynchronous Map methods ----------------------------------------

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     *
     * @param key  the key whose associated value is to be returned
     *
     * @return a {@link CompletableFuture} for the value to which the specified
     *         key is mapped
     */
    default CompletableFuture<V> get(K key)
        {
        return invoke(key, CacheProcessors.get());
        }

    /**
     * Get all the specified keys, if they are in the map. For each key that
     * is in the map, that key and its corresponding value will be placed in
     * the map that is returned by this method. The absence of a key in the
     * returned map indicates that it was not in the map, which may imply (for
     * maps that can load behind the scenes) that the requested data could not
     * be loaded.
     *
     * @param colKeys  a collection of keys that may be in the named map
     *
     * @return a {@link CompletableFuture} for a Map of keys to values for the
     *         specified keys passed in <tt>colKeys</tt>
     */
    default CompletableFuture<Map<K, V>> getAll(Collection<? extends K> colKeys)
        {
        return invokeAll(colKeys, CacheProcessors.get());
        }

    /**
     * Get all the entries that satisfy the specified filter. For each entry
     * that satisfies the filter, the key and its corresponding value will be
     * placed in the map that is returned by this method.
     *
     * @param filter  a Filter that determines the set of entries to return
     *
     * @return a {@link CompletableFuture} for a Map of keys to values for the
     *         specified filter
     */
    default CompletableFuture<Map<K, V>> getAll(Filter filter)
        {
        return invokeAll(filter, CacheProcessors.get());
        }

    /**
     * Stream the entries associated with the specified keys to the provided
     * callback.
     *
     * @param colKeys   a collection of keys that may be in the named map
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> getAll(Collection<? extends K> colKeys,
                                           BiConsumer<? super K, ? super V> callback)
        {
        return invokeAll(colKeys, CacheProcessors.get(),
                entry -> callback.accept(entry.getKey(), entry.getValue()));
        }

    /**
     * Stream the entries associated with the specified keys to the provided
     * callback.
     *
     * @param colKeys   a collection of keys that may be in the named map
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> getAll(Collection<? extends K> colKeys,
                                           Consumer<? super Map.Entry<? extends K, ? extends V>> callback)
        {
        return invokeAll(colKeys, CacheProcessors.get(), callback);
        }

    /**
     * Associates the specified value with the specified key in this map. If
     * the map previously contained a mapping for this key, the old value is
     * replaced.
     * <p>
     * Invoking this method is equivalent to the following call:
     * <pre>
     *     put(oKey, oValue, CacheMap.EXPIRY_DEFAULT);
     * </pre>
     *
     * @param key    key with which the specified value is to be associated
     * @param value  value to be associated with the specified key
     *
     * @return a {@link CompletableFuture}
     */
    default CompletableFuture<Void> put(K key, V value)
        {
        return invoke(key, CacheProcessors.put(value, CacheMap.EXPIRY_NEVER));
        }

    /**
     * Copies all of the mappings from the specified map to this map.
     *
     * @param map  mappings to be added to this map
     *
     * @return a {@link CompletableFuture}
     */
    default CompletableFuture<Void> putAll(Map<? extends K, ? extends V> map)
        {
        return invokeAll(map.keySet(), CacheProcessors.putAll(map)).thenAccept(nil -> {});
        }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key  key whose mapping is to be removed from the map
     *
     * @return a {@link CompletableFuture} for the previous value associated
     *         with the <tt>key</tt>
     */
    default CompletableFuture<V> remove(K key)
        {
        return invoke(key, CacheProcessors.remove());
        }

    /**
     * Removes all of the mappings from the specified keys from this map, if
     * they are present in the map.
     *
     * @param colKeys  a collection of keys that may be in the named map
     *
     * @return a {@link CompletableFuture}
     */
    default CompletableFuture<Void> removeAll(Collection<? extends K> colKeys)
        {
        return invokeAll(colKeys, CacheProcessors.removeBlind()).thenAccept(ANY);
        }

    /**
     * Removes all of the mappings that satisfy the specified filter from this map.
     *
     * @param filter  a Filter that determines the set of entries to remove
     *
     * @return a {@link CompletableFuture}
     */
    default CompletableFuture<Void> removeAll(Filter filter)
        {
        return invokeAll(filter, CacheProcessors.removeBlind()).thenAccept(ANY);
        }

    /**
     * Return a set view of all the keys contained in this map.
     *
     * @return a complete set of keys for this map
     */
    default CompletableFuture<Set<K>> keySet()
        {
        return keySet(AlwaysFilter.INSTANCE);
        }

    // ---- Asynchronous QueryMap methods -----------------------------------
    /**
     * Return a set view of the keys contained in this map for entries that
     * satisfy the criteria expressed by the filter.
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return a set of keys for entries that satisfy the specified criteria
     */
    default CompletableFuture<Set<K>> keySet(Filter filter)
        {
        return invokeAll(filter, CacheProcessors.nop())
                .thenApply(Map::keySet);
        }

    /**
     * Stream the keys of all the entries contained in this map to the provided
     * callback.
     *
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> keySet(Consumer<? super K> callback)
        {
        return keySet(AlwaysFilter.INSTANCE, callback);
        }

    /**
     * Stream the keys for the entries that satisfy the specified filter to the
     * provided callback.
     *
     * @param filter    the Filter object representing the criteria that the
     *                  entries of this map should satisfy
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> keySet(Filter filter,
                                           Consumer<? super K> callback)
        {
        return invokeAll(filter, CacheProcessors.nop(),
                entry -> callback.accept(entry.getKey()));
        }

    /**
     * Return a set view of all the entries contained in this map. Each element
     * in the returned set is a {@link Map.Entry}.
     *
     * @return a set of all entries in this map
     */
    default CompletableFuture<Set<Map.Entry<K, V>>> entrySet()
        {
        return entrySet(AlwaysFilter.INSTANCE);
        }

    /**
     * Return a set view of the entries contained in this map that satisfy the
     * criteria expressed by the filter.  Each element in the returned set is a
     * {@link Map.Entry}.
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return a set of entries that satisfy the specified criteria
     */
    default CompletableFuture<Set<Map.Entry<K, V>>> entrySet(Filter filter)
        {
        return invokeAll(filter, CacheProcessors.get())
                .thenApply(Map::entrySet);
        }

    /**
     * Return a set view of the entries contained in this map that satisfy the
     * criteria expressed by the filter.  Each element in the returned set is a
     * {@link Map.Entry}.  It is further guaranteed that its iterator
     * will traverse the set in such a way that the entry values come up in
     * ascending order, sorted by the specified Comparator or according to the
     * <i>natural ordering</i> (see {@link Comparable}).
     *
     * @param filter     the Filter object representing the criteria that the
     *                   entries of this map should satisfy
     * @param comparator the Comparator object which imposes an ordering on
     *                   entries in the resulting set; or <tt>null</tt> if the
     *                   entries' values natural ordering should be used
     *
     * @return a set of entries that satisfy the specified criteria
     */
    default CompletableFuture<Set<Map.Entry<K, V>>> entrySet(Filter filter, Comparator comparator)
        {
        return invokeAll(filter, CacheProcessors.get())
                .thenApply(mapResult ->
                    {
                    int         cEntries = mapResult.size();
                    Map.Entry[] aEntries = mapResult.entrySet().toArray(new Map.Entry[cEntries]);

                    Arrays.sort(aEntries, new EntryComparator(
                            comparator == null ? SafeComparator.INSTANCE : comparator));
                    return new ImmutableArrayList(aEntries, 0, cEntries).getSet();
                    });
        }

    /**
     * Stream all the entries contained in this map to the provided callback.
     *
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> entrySet(BiConsumer<? super K, ? super V> callback)
        {
        return entrySet(AlwaysFilter.INSTANCE, callback);
        }

    /**
     * Stream all the entries contained in this map to the provided callback.
     *
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> entrySet(Consumer<? super Map.Entry<? extends K, ? extends V>> callback)
        {
        return entrySet(AlwaysFilter.INSTANCE, callback);
        }

    /**
     * Stream the entries that satisfy the specified filter to the provided
     * callback.
     *
     * @param filter    the Filter object representing the criteria that the
     *                  entries of this map should satisfy
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> entrySet(Filter filter, BiConsumer<? super K, ? super V> callback)
        {
        return invokeAll(filter, CacheProcessors.get(),
                entry -> callback.accept(entry.getKey(), entry.getValue()));
        }

    /**
     * Stream the entries that satisfy the specified filter to the provided
     * callback.
     *
     * @param filter    the Filter object representing the criteria that the
     *                  entries of this map should satisfy
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> entrySet(Filter filter,
                                             Consumer<? super Map.Entry<? extends K, ? extends V>> callback)
        {
        return invokeAll(filter, CacheProcessors.get(), callback);
        }

    /**
      * Return a collection of all the values contained in this map.
      *
      * @return a collection of all the values in this map
      */
     default CompletableFuture<Collection<V>> values()
         {
         return values(AlwaysFilter.INSTANCE);
         }

    /**
     * Return a collection of the values contained in this map that satisfy the
     * criteria expressed by the filter.
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return a collection of values for entries that satisfy the specified
     *         criteria
     */
    default CompletableFuture<Collection<V>> values(Filter filter)
        {
        return invokeAll(filter, CacheProcessors.get())
                .thenApply(Map::values);
        }

    /**
     * Return a collection of the values contained in this map that satisfy the
     * criteria expressed by the filter.
     *
     * @param filter     the Filter object representing the criteria that the
     *                   entries of this map should satisfy
     * @param comparator the Comparator object which imposes an ordering on
     *                   entries in the resulting set; or <tt>null</tt> if the
     *                   entries' values natural ordering should be used
     *
     * @return a collection of values for entries that satisfy the specified
     *         criteria
     */
    default CompletableFuture<Collection<V>> values(Filter filter, Comparator<? super V> comparator)
        {
        return values(filter)
                .thenApply(colValues ->
                    {
                    List<V> values = new ArrayList<>(colValues);
                    values.sort(comparator);
                    return values;
                    });
        }

    /**
     * Stream the values of all the entries contained in this map to the provided
     * callback.
     *
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> values(Consumer<? super V> callback)
        {
        return values(AlwaysFilter.INSTANCE, callback);
        }

    /**
     * Stream the values for the entries that satisfy the specified filter to the
     * provided callback.
     *
     * @param filter    the Filter object representing the criteria that the
     *                  entries of this map should satisfy
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    default CompletableFuture<Void> values(Filter filter, Consumer<? super V> callback)
        {
        return invokeAll(filter, CacheProcessors.get(),
                entry -> callback.accept(entry.getValue()));
        }

    // ---- Asynchronous InvocableMap methods -------------------------------
    /**
     * Invoke the passed EntryProcessor against the Entry specified by the
     * passed key asynchronously, returning a {@link CompletableFuture} that can
     * be used to obtain the result of the invocation.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param key        the key to process; it is not required to exist within
     *                   the Map
     * @param processor  the EntryProcessor to use to process the specified key
     *
     * @return a {@link CompletableFuture} that can be used to obtain the result
     *         of the invocation
     */
    <R> CompletableFuture<R> invoke(K key, InvocableMap.EntryProcessor<K, V, R> processor);

    /**
     * Invoke the passed EntryProcessor against all the entries asynchronously,
     * returning a {@link CompletableFuture} that can be used to obtain the
     * result of the invocation for each entry.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param processor  the EntryProcessor to use to process the specified keys
     *
     * @return a {@link CompletableFuture} that can be used to obtain the result
     *         of the invocation for each entry
     */
    default <R> CompletableFuture<Map<K, R>> invokeAll(InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return invokeAll(AlwaysFilter.INSTANCE, processor);
        }

    /**
     * Invoke the passed EntryProcessor against the entries specified by the
     * passed keys asynchronously, returning a {@link CompletableFuture} that
     * can be used to obtain the result of the invocation for each entry.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param collKeys   the keys to process; these keys are not required to
     *                   exist within the Map
     * @param processor  the EntryProcessor to use to process the specified keys
     *
     * @return a {@link CompletableFuture} that can be used to obtain the result
     *         of the invocation for each entry
     */
    <R> CompletableFuture<Map<K, R>> invokeAll(Collection<? extends K> collKeys,
                                               InvocableMap.EntryProcessor<K, V, R> processor);

    /**
     * Invoke the passed EntryProcessor against the set of entries that are
     * selected by the given Filter asynchronously, returning a {@link
     * CompletableFuture} that can be used to obtain the result of the
     * invocation for each entry.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param filter     a Filter that results in the set of keys to be
     *                   processed
     * @param processor  the EntryProcessor to use to process the specified keys
     *
     * @return a {@link CompletableFuture} that can be used to obtain the result
     *         of the invocation for each entry
     */
    <R> CompletableFuture<Map<K, R>> invokeAll(Filter filter,
                                               InvocableMap.EntryProcessor<K, V, R> processor);

    /**
     * Invoke the passed EntryProcessor against all the entries asynchronously,
     * returning a {@link CompletableFuture} that can be used to determine if
     * the operation completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param processor  the EntryProcessor to use to process the specified keys
     * @param callback   a user-defined callback that will be called for each
     *                   partial result
     *
     * @return a {@link CompletableFuture} that can be used to determine if the
     *         operation completed successfully
     */
    default <R> CompletableFuture<Void> invokeAll(InvocableMap.EntryProcessor<K, V, R> processor,
                                                  Consumer<? super Map.Entry<? extends K, ? extends R>> callback)
        {
        return invokeAll(AlwaysFilter.INSTANCE, processor, callback);
        }

    /**
     * Invoke the passed EntryProcessor against all the entries asynchronously,
     * returning a {@link CompletableFuture} that can be used to determine if
     * the operation completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param processor  the EntryProcessor to use to process the specified keys
     * @param callback   a user-defined callback that will be called for each
     *                   partial result
     *
     * @return a {@link CompletableFuture} that can be used to determine if the
     *         operation completed successfully
     */
    default <R> CompletableFuture<Void> invokeAll(InvocableMap.EntryProcessor<K, V, R> processor,
                                                  BiConsumer<? super K, ? super R> callback)
        {
        return invokeAll(AlwaysFilter.INSTANCE, processor,
                (entry -> callback.accept(entry.getKey(), entry.getValue())));
        }

    /**
     * Invoke the passed EntryProcessor against the entries specified by the
     * passed keys asynchronously, returning a {@link CompletableFuture} that
     * can be used to determine if the operation completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param collKeys   the keys to process; these keys are not required to
     *                   exist within the Map
     * @param processor  the EntryProcessor to use to process the specified keys
     * @param callback   a user-defined callback that will be called for each
     *                   partial result
     *
     * @return a {@link CompletableFuture} that can be used to determine if the
     *         operation completed successfully
     */
    <R> CompletableFuture<Void> invokeAll(Collection<? extends K> collKeys,
                                          InvocableMap.EntryProcessor<K, V, R> processor,
                                          Consumer<? super Map.Entry<? extends K, ? extends R>> callback);

    /**
     * Invoke the passed EntryProcessor against the entries specified by the
     * passed keys asynchronously, returning a {@link CompletableFuture} that
     * can be used to determine if the operation completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param collKeys   the keys to process; these keys are not required to
     *                   exist within the Map
     * @param processor  the EntryProcessor to use to process the specified keys
     * @param callback   a user-defined callback that will be called for each
     *                   partial result
     *
     * @return a {@link CompletableFuture} that can be used to determine if the
     *         operation completed successfully
     */
    default <R> CompletableFuture<Void> invokeAll(Collection<? extends K> collKeys,
                                                  InvocableMap.EntryProcessor<K, V, R> processor, BiConsumer<? super K, ? super R> callback)
        {
        return invokeAll(collKeys, processor,
                entry -> callback.accept(entry.getKey(), entry.getValue()));
        }

    /**
     * Invoke the passed EntryProcessor against the set of entries that are
     * selected by the given Filter asynchronously, returning a {@link
     * CompletableFuture} that can be used to determine if the operation
     * completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param filter     a Filter that results in the set of keys to be
     *                   processed
     * @param processor  the EntryProcessor to use to process the specified keys
     * @param callback   a user-defined callback that will be called for each
     *                   partial result
     *
     * @return a {@link CompletableFuture} that can be used to determine if the
     *         operation completed successfully
     */
    <R> CompletableFuture<Void> invokeAll(Filter filter,
                                          InvocableMap.EntryProcessor<K, V, R> processor,
                                          Consumer<? super Map.Entry<? extends K, ? extends R>> callback);

    /**
     * Invoke the passed EntryProcessor against the set of entries that are
     * selected by the given Filter asynchronously, returning a {@link
     * CompletableFuture} that can be used to determine if the operation
     * completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     *
     * @param <R>        the type of value returned by the EntryProcessor
     * @param filter     a Filter that results in the set of keys to be
     *                   processed
     * @param processor  the EntryProcessor to use to process the specified keys
     * @param callback   a user-defined callback that will be called for each
     *                   partial result
     *
     * @return a {@link CompletableFuture} that can be used to determine if the
     *         operation completed successfully
     */
    default <R> CompletableFuture<Void> invokeAll(Filter filter,
                                                  InvocableMap.EntryProcessor<K, V, R> processor, BiConsumer<? super K, ? super R> callback)
        {
        return invokeAll(filter, processor,
                entry -> callback.accept(entry.getKey(), entry.getValue()));
        }

    /**
     * Perform an aggregating operation asynchronously against all the entries.
     *
     * @param <R>         the type of value returned by the EntryAggregator
     * @param aggregator  the EntryAggregator that is used to aggregate across
     *                    the specified entries of this Map
     *
     * @return a {@link CompletableFuture} that can be used to obtain the result
     *         of the aggregation
     */
    default <R> CompletableFuture<R> aggregate(InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return aggregate(AlwaysFilter.INSTANCE, aggregator);
        }

    /**
     * Perform an aggregating operation asynchronously against the entries
     * specified by the passed keys.
     *
     * @param <R>         the type of value returned by the EntryAggregator
     * @param collKeys    the Collection of keys that specify the entries within
     *                    this Map to aggregate across
     * @param aggregator  the EntryAggregator that is used to aggregate across
     *                    the specified entries of this Map
     *
     * @return a {@link CompletableFuture} that can be used to obtain the result
     *         of the aggregation
     */
    <R> CompletableFuture<R> aggregate(Collection<? extends K> collKeys,
                                       InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator);

    /**
     * Perform an aggregating operation asynchronously against the set of
     * entries that are selected by the given Filter.
     *
     * @param <R>         the type of value returned by the EntryAggregator
     * @param filter      the Filter that is used to select entries within this
     *                    Map to aggregate across
     * @param aggregator  the EntryAggregator that is used to aggregate across
     *                    the selected entries of this Map
     *
     * @return a {@link CompletableFuture} that can be used to obtain the result
     *         of the aggregation
     */
    <R> CompletableFuture<R> aggregate(Filter filter,
                                       InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator);

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    default CompletableFuture<Integer> size()
        {
        return aggregate(new Count<>());
        }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    default CompletableFuture<Boolean> isEmpty()
        {
        return size().thenApply(size -> size == 0);
        }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this operation completes.
     */
    default CompletableFuture<Void> clear()
        {
        return removeAll(AlwaysFilter.INSTANCE);
        }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this map contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
     * at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested
     *
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     */
    default CompletableFuture<Boolean> containsKey(K key)
        {
        return invoke(key, InvocableMap.Entry::isPresent);
        }

    /**
     * Returns the value to which the specified key is mapped, or {@code
     * valueDefault} if this map contains no mapping for the key.
     *
     * @param key           the key whose associated value is to be returned
     * @param valueDefault  the default mapping of the key
     *
     * @return the value to which the specified key is mapped, or {@code
     *         valueDefault} if this map contains no mapping for the key
     */
    @SuppressWarnings("unchecked")
    default CompletableFuture<V> getOrDefault(K key, V valueDefault)
        {
        return invoke(key, entry ->
            {
            if (entry.isPresent())
                {
                return new Object[]{true, entry.getValue()};
                }
            return new Object[]{false};
            })
            .thenApply(aoResult ->
                {
                if (Boolean.TRUE.equals(aoResult[0]))
                    {
                    return (V) aoResult[1];
                    }
                else
                    {
                    return valueDefault;
                    }
                });
        }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associates it with the given value and returns
     * {@code null}, else returns the current value.
     *
     * @param key    key with which the specified value is to be associated
     * @param value  value to be associated with the specified key
     *
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     */
    default CompletableFuture<V> putIfAbsent(K key, V value)
        {
        return invoke(key, CacheProcessors.putIfAbsent(value));
        }

    /**
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * @param key    key with which the specified value is associated
     * @param value  value expected to be associated with the specified key
     *
     * @return {@code true} if the value was removed
     */
    default CompletableFuture<Boolean> remove(K key, V value)
        {
        return invoke(key, CacheProcessors.remove(value));
        }

    /**
     * Replaces the entry for the specified key only if it is
     * currently mapped to some value.
     *
     * @param key    key with which the specified value is associated
     * @param value  value to be associated with the specified key
     *
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     */
    default CompletableFuture<V> replace(K key, V value)
        {
        return invoke(key, CacheProcessors.replace(value));
        }

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     *
     * @param key       key with which the specified value is associated
     * @param oldValue  value expected to be associated with the specified key
     * @param newValue  value to be associated with the specified key
     *
     * @return {@code true} if the value was replaced
     */
    default CompletableFuture<Boolean> replace(K key, V oldValue, V newValue)
        {
        return invoke(key, CacheProcessors.replace(oldValue, newValue));
        }

    /**
     * Compute the value using the given mapping function and enter it into this
     * map (unless {@code null}), if the specified key is not already associated
     * with a value (or is mapped to {@code null}).
     * <p>
     * If the mapping function returns {@code null} no mapping is recorded. If
     * the function itself throws an (unchecked) exception, the exception is
     * rethrown, and no mapping is recorded.
     * <p>
     * The most common usage is to construct a new object serving as an initial
     * mapped value or memoized result, as in:
     * <pre>
     *   {@code map.computeIfAbsent(key, k -> new Value(f(k)));}
     * </pre>
     *
     * <p>Or to implement a multi-value map, {@code Map<K, Collection<V>>},
     * supporting multiple values per key:
     * <pre>
     *   {@code map.computeIfAbsent(key, k -> new HashSet<V>()).add(v);}
     * </pre>
     *
     * Note that the previous example will not work as expected if this method
     * is called on a distributed map, as the <code>add</code> method will be called
     * on the client-side copy of the collection stored on the server.
     *
     * @param key              key with which the specified value is to be associated
     * @param mappingFunction  the function to compute a value
     *
     * @return the current (existing or computed) value associated with
     *         the specified key, or null if the computed value is null
     */
    default CompletableFuture<V> computeIfAbsent(
            K key, Remote.Function<? super K, ? extends V> mappingFunction)
        {
        return invoke(key, CacheProcessors.computeIfAbsent(mappingFunction));
        }

    /**
     * Compute a new mapping given the key and its current mapped value, if the
     * value for the specified key is present and non-null.
     * <p>
     * If the function returns {@code null}, the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     *
     * @param key               the key with which the specified value is to be
     *                          associated
     * @param remappingFunction the function to compute a value
     *
     * @return the new value associated with the specified key, or null if none
     */
    default CompletableFuture<V> computeIfPresent(
            K key, Remote.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.computeIfPresent(remappingFunction));
        }

    /**
     * Compute a new mapping for the specified key and its current value.
     * <p>
     * If the function returns {@code null}, the mapping is removed (or remains
     * absent if initially absent). If the function itself throws an (unchecked)
     * exception, the exception is rethrown, and the current mapping is left
     * unchanged.
     *
     * @param key               the key with which the computed value is to be
     *                          associated
     * @param remappingFunction the function to compute a value
     *
     * @return the new value associated with the specified key, or null if none
     */
    default CompletableFuture<V> compute(K key,
                                         Remote.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.compute(remappingFunction));
        }

    /**
     * If the specified key is not already associated with a value or is
     * associated with null, associates it with the given non-null value.
     * Otherwise, replaces the associated value with the results of the given
     * remapping function, or removes if the result is {@code null}.
     * <p>
     * This method may be of use when combining multiple mapped values for a
     * key. For example, to either create or append a {@code String msg} to a
     * value mapping:
     * <pre>
     *   {@code map.merge(key, msg, String::concat)}
     * </pre>
     *
     * If the function returns {@code null} the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     *
     * @param key               key with which the resulting value is to be
     *                          associated
     * @param value             the non-null value to be merged with the
     *                          existing value associated with the key or, if no
     *                          existing value or a null value is associated
     *                          with the key, to be associated with the key
     * @param remappingFunction the function to recompute a value if present
     *
     * @return the new value associated with the specified key, or null if no
     *         value is associated with the key
     */
    default CompletableFuture<V> merge(K key, V value,
                                       Remote.BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.merge(value, remappingFunction));
        }

    /**
     * Replace each entry's value with the result of invoking the given function
     * on that entry until all entries have been processed or the function
     * throws an exception.
     *
     * @param function the function to apply to each entry
     */
    default CompletableFuture<Map<K, Void>> replaceAll(
            Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        return replaceAll(AlwaysFilter.INSTANCE, function);
        }

    /**
     * Replace each entry's value with the result of invoking the given function
     * on that entry until all entries for the specified key set have been
     * processed or the function throws an exception.
     *
     * @param collKeys the keys to process; these keys are not required to exist
     *                 within the Map
     * @param function the function to apply to each entry
     */
    default CompletableFuture<Map<K, Void>> replaceAll(Collection<? extends K> collKeys,
                                                       Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        return invokeAll(collKeys, CacheProcessors.replace(function));
        }

    /**
     * Replace each entry's value with the result of invoking the given function
     * on that entry until all entries selected by the specified filter have
     * been processed or the function throws an exception.
     *
     * @param filter   the filter that should be used to select entries
     * @param function the function to apply to each entry
     */
    default CompletableFuture<Map<K, Void>> replaceAll(Filter filter,
                                                       Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        return invokeAll(filter, CacheProcessors.replace(function));
        }

    // ----- AsyncNamedMap.Option interface -------------------------------

    /**
     * An immutable option for creating and configuring {@link AsyncNamedMap}s.
     */
    interface Option
        {
        }

    // ---- option: OrderBy -------------------------------------------------

    /**
     * A configuration option which determines the ordering of async operations.
     */
    public class OrderBy
            implements Option
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct OrderBy instance.
         *
         * @param supplierOrderId  the function that should be used to determine
         *                         order id
         */
        protected OrderBy(IntSupplier supplierOrderId)
            {
            m_supplierOrderId = supplierOrderId;
            }

        // ---- accessors ---------------------------------------------------

        /**
         * Return unit-of-order id.
         *
         * @return the unit-of-order id
         */
        public int getOrderId()
            {
            return m_supplierOrderId.getAsInt();
            }

        // ---- static factory methods --------------------------------------

        /**
         * Return the order that will ensure that all operations invoked from
         * the same client thread are executed on the server sequentially, using
         * the same {@link AsynchronousAgent#getUnitOfOrderId unit-of-order}.
         * <p>
         * This tends to provide best performance for fast, non-blocking
         * operations and is the default order if the order is not specified.
         *
         * @return the default, thread-based ordering
         */
        @Options.Default
        public static OrderBy thread()
            {
            return THREAD;
            }

        /**
         * Return the order that will allow the server to parallelize the
         * execution of async operations across multiple threads, regardless of
         * whether they were invoked from the same client thread or not.
         *
         * @return the order which allows the server to parallelize the
         *         execution of async operations across multiple threads
         */
        public static OrderBy none()
            {
            return NONE;
            }

        /**
         * Return the order that will use the specified unit-of-order for all
         * async operations, regardless of whether they were invoked from the
         * same client thread or not.
         *
         * @param nOrderId  the unit-of-order to use for all operations
         *
         * @return the order which will use specified {@code nOrderId} for all
         *         operations
         */
        public static OrderBy id(int nOrderId)
            {
            return new OrderBy(() -> nOrderId);
            }

        /**
         * Return the order that will use the specified supplier to provide
         * unit-of-order for each async operation.
         *
         * @param supplierOrderId  the function that should be used to determine
         *                         order id
         *
         * @return the order which will use specified function to provide
         *         unit-of-order for each async operation
         */
        public static OrderBy custom(IntSupplier supplierOrderId)
            {
            return new OrderBy(supplierOrderId);
            }

        // ---- static members ----------------------------------------------

        /**
         * Random number generator.
         */
        protected static final Random RANDOM = new Random();

        /**
         * The thread-order singleton.
         */
        protected static final OrderBy THREAD = new OrderBy(() -> Thread.currentThread().hashCode());

        /**
         * The none-order singleton.
         */
        protected static final OrderBy NONE   = new OrderBy(RANDOM::nextInt);

        // ---- data members ------------------------------------------------

        /**
         * A function that should be used to determine order id.
         */
        protected final IntSupplier m_supplierOrderId;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A trivial consumer.
     */
    Consumer<Object> ANY = (any) -> {};
    }
