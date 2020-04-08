/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.internal.util.stream.StreamSupport;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian.GuardContext;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.function.Remote;
import com.tangosol.util.stream.RemoteStream;

import java.io.Serializable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 * An InvocableMap is a Map against which both entry-targeted processing and
 * aggregating operations can be invoked. While a traditional model for working
 * with a Map is to have an operation access and mutate the Map directly through
 * its API, the InvocableMap allows that model of operation to be inverted such
 * that the operations against the Map contents are executed by (and thus within
 * the localized context of) a Map. This is particularly useful in a distributed
 * environment, because it enables the processing to be moved to the location at
 * which the entries-to-be-processed are being managed, thus providing
 * efficiency by localization of processing.
 * <p>
 * <b>Note: When using the Coherence Enterprise Edition or Grid Edition, the
 * Partitioned Cache implements the InvocableMap interface by partitioning and
 * localizing the invocations, resulting in extremely high throughput and low
 * latency. When using Coherence Standard Edition, the InvocableMap processes
 * the invocations on the originating node, typically resulting in higher
 * network, memory and CPU utilization, which translates to lower performance,
 * and particularly when processing large data sets.</b>
 *
 * @param <K> the type of the Map entry keys
 * @param <V> the type of the Map entry values
 *
 * @author cp/gg/jh  2005.07.19
 * @author as        2014.06.14
 *
 * @since Coherence 3.1
 */
public interface InvocableMap<K, V>
        extends Map<K, V>
    {
    /**
     * Invoke the passed EntryProcessor against the Entry specified by the
     * passed key, returning the result of the invocation.
     *
     * @param <R>       the type of value returned by the EntryProcessor
     * @param key       the key to process; it is not required to exist within
     *                  the Map
     * @param processor the EntryProcessor to use to process the specified key
     *
     * @return the result of the invocation as returned from the EntryProcessor
     */
    public <R> R invoke(K key, EntryProcessor<K, V, R> processor);

    /**
     * Invoke the passed EntryProcessor against all the entries, returning the
     * result of the invocation for each.
     *
     * @param <R>       the type of value returned by the EntryProcessor
     * @param processor the EntryProcessor to use to process the specified keys
     *
     * @return a Map containing the results of invoking the EntryProcessor
     * against each of the specified keys
     */
    public default <R> Map<K, R> invokeAll(EntryProcessor<K, V, R> processor)
        {
        return invokeAll(AlwaysFilter.INSTANCE, processor);
        }

    /**
     * Invoke the passed EntryProcessor against the entries specified by the
     * passed keys, returning the result of the invocation for each.
     *
     * @param <R>       the type of value returned by the EntryProcessor
     * @param collKeys  the keys to process; these keys are not required to
     *                  exist within the Map
     * @param processor the EntryProcessor to use to process the specified keys
     *
     * @return a Map containing the results of invoking the EntryProcessor
     * against each of the specified keys
     */
    public <R> Map<K, R> invokeAll(Collection<? extends K> collKeys, EntryProcessor<K, V, R> processor);

    /**
     * Invoke the passed EntryProcessor against the set of entries that are
     * selected by the given Filter, returning the result of the invocation for
     * each.
     * <p>
     * Unless specified otherwise, InvocableMap implementations will perform
     * this operation in two steps: (1) use the filter to retrieve a matching
     * entry set; (2) apply the agent to every filtered entry. This algorithm
     * assumes that the agent's processing does not affect the result of the
     * specified filter evaluation, since the filtering and processing could be
     * performed in parallel on different threads. If this assumption does not
     * hold, the processor logic has to be idempotent, or at least re-evaluate
     * the filter. This could be easily accomplished by wrapping the processor
     * with the {@link com.tangosol.util.processor.ConditionalProcessor}.
     *
     * @param <R>       the type of value returned by the EntryProcessor
     * @param filter    a Filter that results in the set of keys to be
     *                  processed
     * @param processor the EntryProcessor to use to process the specified keys
     *
     * @return a Map containing the results of invoking the EntryProcessor
     * against the keys that are selected by the given Filter
     */
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V, R> processor);

    /**
     * Perform an aggregating operation against all the entries.
     *
     * @param <R>        the type of value returned by the EntryAggregator
     * @param aggregator the EntryAggregator that is used to aggregate across
     *                   the specified entries of this Map
     *
     * @return the result of the aggregation
     */
    public default <R> R aggregate(EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return aggregate(AlwaysFilter.INSTANCE, aggregator);
        }

    /**
     * Perform an aggregating operation against the entries specified by the
     * passed keys.
     *
     * @param <R>        the type of value returned by the EntryAggregator
     * @param collKeys   the Collection of keys that specify the entries within
     *                   this Map to aggregate across
     * @param aggregator the EntryAggregator that is used to aggregate across
     *                   the specified entries of this Map
     *
     * @return the result of the aggregation
     */
    public <R> R aggregate(Collection<? extends K> collKeys, EntryAggregator<? super K, ? super V, R> aggregator);

    /**
     * Perform an aggregating operation against the set of entries that are
     * selected by the given Filter.
     *
     * @param <R>        the type of value returned by the EntryAggregator
     * @param filter     the Filter that is used to select entries within this
     *                   Map to aggregate across
     * @param aggregator the EntryAggregator that is used to aggregate across
     *                   the selected entries of this Map
     *
     * @return the result of the aggregation
     */
    public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V, R> aggregator);

    // ----- Map interface --------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public default V getOrDefault(Object key, V defaultValue)
        {
        Object[] aoResult = invoke((K) key, entry -> {
            if (entry.isPresent())
                {
                return new Object[]{true, entry.getValue()};
                }
            return new Object[]{false};
        });

        if (Boolean.TRUE.equals(aoResult[0]))
            {
            return (V) aoResult[1];
            }
        else
            {
            return defaultValue;
            }
        }

    @Override
    public default V putIfAbsent(K key, V value)
        {
        return invoke(key, CacheProcessors.putIfAbsent(value));
        }

    @Override
    public default boolean remove(Object key, Object value)
        {
        return invoke((K) key, CacheProcessors.remove(value));
        }

    @Override
    public default boolean replace(K key, V oldValue, V newValue)
        {
        return invoke(key, CacheProcessors.replace(oldValue, newValue));
        }

    @Override
    public default V replace(K key, V value)
        {
        return invoke(key, CacheProcessors.replace(value));
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
     *
     * <pre> {@code
     * map.computeIfAbsent(key, k -> new Value(f(k)));
     * }</pre>
     *
     * <p>Or to implement a multi-value map, {@code Map<K, Collection<V>>},
     * supporting multiple values per key:
     *
     * <pre> {@code
     * map.computeIfAbsent(key, k -> new HashSet<V>()).add(v);
     * }</pre>
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
    public default V computeIfAbsent(K key, Remote.Function<? super K, ? extends V> mappingFunction)
        {
        return invoke(key, CacheProcessors.computeIfAbsent(mappingFunction));
        }

    /**
     * {@inheritDoc}
     *
     * Note that the previous example will not work as expected if this method
     * is called on a distributed map, as the <code>add</code> method will be called
     * on a client-side copy of the collection stored on the server.
     */
    @Override
    public default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        {
        return invoke(key, CacheProcessors.computeIfAbsent(mappingFunction));
        }

    /**
     * Compute a new mapping given the key and its current mapped value,
     * if the value for the specified key is present and non-null.
     * <p>
     * If the function returns {@code null}, the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     *
     * @param key                the key with which the specified value is to be
     *                           associated
     * @param remappingFunction  the function to compute a value
     *
     * @return the new value associated with the specified key, or null if none
     */
    public default V computeIfPresent(K key, Remote.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.computeIfPresent(remappingFunction));
        }

    @Override
    public default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
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
     * @param key                the key with which the computed value is to be
     *                           associated
     * @param remappingFunction  the function to compute a value
     *
     * @return the new value associated with the specified key, or null if none
     */
    public default V compute(K key, Remote.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.compute(remappingFunction));
        }

    @Override
    public default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.compute(remappingFunction));
        }

    /**
     * If the specified key is not already associated with a value or is
     * associated with null, associates it with the given non-null value.
     * Otherwise, replaces the associated value with the results of the given
     * remapping function, or removes if the result is {@code null}.
     * <p>
     * This method may be of use when combining multiple mapped values for a key.
     * For example, to either create or append a {@code String msg} to a
     * value mapping:
     *
     * <pre> {@code
     * map.merge(key, msg, String::concat)
     * }</pre>
     *
     * If the function returns {@code null} the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     *
     * @param key    key with which the resulting value is to be associated
     * @param value  the non-null value to be merged with the existing value
     *               associated with the key or, if no existing value or a null
     *               value is associated with the key, to be associated with the key
     * @param remappingFunction  the function to recompute a value if present
     *
     * @return the new value associated with the specified key, or null if no
     *         value is associated with the key
     */
    public default V merge(K key, V value, Remote.BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.merge(value, remappingFunction));
        }

    @Override
    public default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.merge(value, remappingFunction));
        }

    /**
     * Replace each entry's value with the result of invoking the given
     * function on that entry until all entries have been processed or the
     * function throws an exception.
     * <p>
     * Exceptions thrown by the function are relayed to the caller.
     *
     * @param function  the function to apply to each entry
     */
    public default void replaceAll(Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        invokeAll(AlwaysFilter.INSTANCE, CacheProcessors.replace(function));
        }

    @Override
    public default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
        {
        invokeAll(AlwaysFilter.INSTANCE, CacheProcessors.replace(function));
        }

    /**
     * Replace each entry's value with the result of invoking the given
     * function on that entry until all entries for the specified key set have
     * been processed or the function throws an exception.
     * <p>
     * Exceptions thrown by the function are relayed to the caller.
     *
     * @param collKeys  the keys to process; these keys are not required to
     *                  exist within the Map
     * @param function  the function to apply to each entry
     */
    public default void replaceAll(Collection<? extends K> collKeys, Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        invokeAll(collKeys, CacheProcessors.replace(function));
        }

    /**
     * Replace each entry's value with the result of invoking the given
     * function on that entry until all entries selected by the specified filter
     * have been processed or the function throws an exception.
     * <p>
     * Exceptions thrown by the function are relayed to the caller.
     *
     * @param filter    the filter that should be used to select entries
     * @param function  the function to apply to each entry
     */
    public default void replaceAll(Filter filter, Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        invokeAll(filter, CacheProcessors.replace(function));
        }

    // ----- Stream support -------------------------------------------------

    /**
     * Return a stream of all entries in this map.
     *
     * @return  a stream of all entries in this map
     */
    public default RemoteStream<Entry<K, V>> stream()
        {
        return stream(AlwaysFilter.INSTANCE);
        }

    /**
     * Return a stream of entries with the specified keys.
     *
     * @param collKeys  the keys to process; these keys are not required to
     *                  exist within the Map
     *
     * @return  a stream of entries for the specified keys
     */
    public default RemoteStream<Entry<K, V>> stream(Collection<? extends K> collKeys)
        {
        return StreamSupport.entryStream(this, true, collKeys, null);
        }

    /**
     * Return a filtered stream of entries in this map.
     *
     * @param filter  filter to apply to this map's entries before creating
     *                the stream
     *
     * @return  a stream of entries that satisfy the specified filter
     */
    public default RemoteStream<Entry<K, V>> stream(Filter filter)
        {
        return StreamSupport.entryStream(this, true, null, filter);
        }

    /**
     * Return a stream of values extracted from the entries of this map.
     * <p>
     * This method is highly recommended when the intention is to work against
     * a stream of {@link Entry} attributes, and is functionally equivalent to:
     * <pre>
     *     cache.stream().map(entry -&gt; entry.extract(extractor))
     * </pre>
     * The use of this method over the {@link #stream()} method, allows relevant
     * indices to be used avoiding potential deserialization and significantly
     * improving performance of value extraction.
     * <p>
     * The same goal can be achieved by simply using multiple {@link
     * RemoteStream#map(ValueExtractor)} calls against the entry stream, such
     * as:
     * <pre>
     *     cache.stream()
     *          .map(Map.Entry::getValue)
     *          .map(MyValue::getAttribute)
     * </pre>
     * However, this will only be efficient if you have a {@link
     * com.tangosol.util.extractor.DeserializationAccelerator DeserializationAccelerator}
     * configured for the cache. Otherwise, it will result in deserialization of
     * all cache entries, which is likely to have significant negative impact
     * on performance.
     *
     * @param <T>        the type of the value to extract from
     * @param <E>        the type of value that will be extracted
     * @param extractor  the extractor to use
     *
     * @return  a stream of extracted values from the entries of this map
     */
    public default <T, E> RemoteStream<E> stream(ValueExtractor<T, ? extends E> extractor)
        {
        Objects.requireNonNull(extractor, "The extractor cannot be null");

        ValueExtractor<T, ? extends E> ex = Lambdas.ensureRemotable(extractor);
        return stream().map(entry -> entry.extract(ex));
        }

    /**
     * Return a stream of values extracted from the entries with the specified keys.
     * <p>
     * This method is highly recommended when the intention is to work against
     * a stream of {@link Entry} attributes, and is functionally equivalent to:
     * <pre>
     *     cache.stream(collKeys).map(entry -&gt; entry.extract(extractor))
     * </pre>
     * The use of this method over the {@link #stream(Collection)} method, allows relevant
     * indices to be used avoiding potential deserialization and significantly
     * improving performance of value extraction.
     * <p>
     * The same goal can be achieved by simply using multiple {@link
     * RemoteStream#map(ValueExtractor)} calls against the entry stream, such
     * as:
     * <pre>
     *     cache.stream(collKeys)
     *          .map(Map.Entry::getValue)
     *          .map(MyValue::getAttribute)
     * </pre>
     * However, this will only be efficient if you have a {@link
     * com.tangosol.util.extractor.DeserializationAccelerator DeserializationAccelerator}
     * configured for the cache. Otherwise, it will result in deserialization of
     * all selected entries, which is likely to have significant negative impact
     * on performance.
     *
     * @param <T>        the type of the value to extract from
     * @param <E>        the type of value that will be extracted
     * @param collKeys   the keys to process; these keys are not required to
     *                   exist within the Map
     * @param extractor  the extractor to use
     *
     * @return a stream of values extracted from all the entries with the
     *         specified keys
     */
    public default <T, E> RemoteStream<E> stream(Collection<? extends K> collKeys, ValueExtractor<T, ? extends E> extractor)
        {
        Objects.requireNonNull(extractor, "The extractor cannot be null");

        ValueExtractor<T, ? extends E> ex = Lambdas.ensureRemotable(extractor);
        return stream(collKeys).map(entry -> entry.extract(ex));
        }

    /**
     * Return a stream of values extracted from all the entries that satisfy
     * the specified filter.
     * <p>
     * This method is highly recommended when the intention is to work against
     * a stream of {@link Entry} attributes, and is functionally equivalent to:
     * <pre>
     *     cache.stream(filter).map(entry -&gt; entry.extract(extractor))
     * </pre>
     * The use of this method over the {@link #stream(Filter)} method, allows relevant
     * indices to be used avoiding potential deserialization and significantly
     * improving performance of value extraction.
     * <p>
     * The same goal can be achieved by simply using multiple {@link
     * RemoteStream#map(ValueExtractor)} calls against the entry stream, such
     * as:
     * <pre>
     *     cache.stream(filter)
     *          .map(Map.Entry::getValue)
     *          .map(MyValue::getAttribute)
     * </pre>
     * However, this will only be efficient if you have a {@link
     * com.tangosol.util.extractor.DeserializationAccelerator DeserializationAccelerator}
     * configured for the cache. Otherwise, it will result in deserialization of
     * all selected entries, which is likely to have significant negative impact
     * on performance.
     *
     * @param <T>        the type of the value to extract from
     * @param <E>        the type of value that will be extracted
     * @param filter     filter to apply to this map's entries before creating
     *                   the stream
     * @param extractor  the extractor to use
     *
     * @return  a stream of values extracted from all the entries that satisfy
     *          the specified filter
     */
    public default <T, E> RemoteStream<E> stream(Filter filter, ValueExtractor<T, ? extends E> extractor)
        {
        Objects.requireNonNull(extractor, "The extractor cannot be null");

        ValueExtractor<T, ? extends E> ex = Lambdas.ensureRemotable(extractor);
        return stream(filter).map(entry -> entry.extract(ex));
        }

    // ----- InvocableMap.Entry interface -----------------------------------

    /**
     * An InvocableMap.Entry contains additional information and exposes
     * additional operations that the basic Map.Entry does not. It allows
     * non-existent entries to be represented, thus allowing their optional
     * creation. It allows existent entries to be removed from the Map. It
     * supports a number of optimizations that can ultimately be mapped through
     * to indexes and other data structures of the underlying Map.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public interface Entry<K, V>
            extends QueryMap.Entry<K, V>
        {
        // ----- Map.Entry interface ------------------------------------

        /**
         * Return the key corresponding to this entry. The resultant key does
         * not necessarily exist within the containing Map, which is to say that
         * <tt>InvocableMap.this.containsKey(getKey())</tt> could return false.
         * To test for the presence of this key within the Map, use {@link
         * #isPresent}, and to create the entry for the key, use {@link
         * #setValue}.
         *
         * @return the key corresponding to this entry; may be null if the
         * underlying Map supports null keys
         */
        public K getKey();

        /**
         * Return the value corresponding to this entry. If the entry does not
         * exist, then the value will be null. To differentiate between a null
         * value and a non-existent entry, use {@link #isPresent}.
         * <p>
         * <b>Note:</b> any modifications to the value retrieved using this
         * method are not guaranteed to persist unless followed by a {@link
         * #setValue} or {@link #update} call.
         *
         * @return the value corresponding to this entry; may be null if the
         * value is null or if the Entry does not exist in the Map
         */
        public V getValue();

        /**
         * Store the value corresponding to this entry. If the entry does not
         * exist, then the entry will be created by invoking this method, even
         * with a null value (assuming the Map supports null values).
         *
         * @param value the new value for this Entry
         *
         * @return the previous value of this Entry, or null if the Entry did
         * not exist
         */
        public V setValue(V value);

        // ----- InvocableMap.Entry interface ---------------------------

        /**
         * Return the value corresponding to this entry, or the specified
         * {@code defaultValue}, if the entry value is {@code null}.
         * To differentiate between a null value and a non-existent entry,
         * use {@link #isPresent}.
         * <p>
         * <b>Note:</b> any modifications to the value retrieved using this
         * method are not guaranteed to persist unless followed by a {@link
         * #setValue} or {@link #update} call.
         *
         * @param defaultValue  the default value to return if the entry
         *                      value is {@code null}
         *
         * @return the value corresponding to this entry, or the specified
         *         {@code defaultValue}, if the entry value is {@code null}
         */
        public default V getValue(V defaultValue)
            {
            V value = getValue();
            return value == null ? defaultValue : value;
            }

        /**
         * Store the value corresponding to this entry. If the entry does not
         * exist, then the entry will be created by invoking this method, even
         * with a null value (assuming the Map supports null values).
         * <p>
         * Unlike the other form of {@link #setValue(Object) setValue}, this
         * form does not return the previous value, and as a result may be
         * significantly less expensive (in terms of cost of execution) for
         * certain Map implementations.
         *
         * @param value      the new value for this Entry
         * @param fSynthetic pass true only if the insertion into or
         *                   modification of the Map should be treated as a
         *                   synthetic event
         */
        public void setValue(V value, boolean fSynthetic);

        /**
         * Update the Entry's value. Calling this method is semantically
         * equivalent to:
         * <pre>
         *   V target = entry.getValue();
         *   updater.update(target, value);
         *   entry.setValue(target, false);
         * </pre>
         * The benefit of using this method is that it may allow the Entry
         * implementation to significantly optimize the operation, such as for
         * purposes of delta updates and backup maintenance.
         *
         * @param <T>     the class of the value
         * @param updater a ValueUpdater used to modify the Entry's value
         * @param value   the new value for this Entry
         */
        public <T> void update(ValueUpdater<V, T> updater, T value);

        /**
         * Determine if this Entry exists in the Map. If the Entry is not
         * present, it can be created by calling {@link #setValue(Object)} or
         * {@link #setValue(Object, boolean)}. If the Entry is present, it can
         * be destroyed by calling {@link #remove(boolean)}.
         *
         * @return true iff this Entry is existent in the containing Map
         */
        public boolean isPresent();

        /**
         * Determine if this Entry has been synthetically mutated. This method
         * returns {@code false} if either a non-synthetic update was made or
         * the entry has not been modified.
         *
         * @return true if the Entry has been synthetically mutated
         */
        public boolean isSynthetic();

        /**
         * Remove this Entry from the Map if it is present in the Map.
         * <p>
         * This method supports both the operation corresponding to {@link
         * Map#remove} as well as synthetic operations such as eviction. If the
         * containing Map does not differentiate between the two, then this
         * method will always be identical to <tt>InvocableMap.this.remove(getKey())</tt>.
         *
         * @param fSynthetic pass true only if the removal from the Map should
         *                   be treated as a synthetic event
         */
        public void remove(boolean fSynthetic);
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
     * An invocable agent that operates against the Entry objects within a Map.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     * @param <R> the type of value returned by the EntryProcessor
     */
    @FunctionalInterface
    public interface EntryProcessor<K, V, R>
            extends Serializable
        {
        /**
         * Process a Map.Entry object.
         * <p>
         * Note: if this method throws an exception, all modifications to the supplied
         * entry or any other entries retrieved via the {@link BackingMapContext#getBackingMapEntry}
         * API will be rolled back leaving all underlying values unchanged.
         *
         *  @param entry  the Entry to process
         *
         * @return the result of the processing, if any
         */
        public R process(Entry<K, V> entry);

        /**
         * Process a Set of InvocableMap.Entry objects. This method is
         * semantically equivalent to:
         * <pre>
         *   Map mapResults = new ListMap();
         *   for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
         *       {
         *       Entry entry = (Entry) iter.next();
         *       mapResults.put(entry.getKey(), process(entry));
         *       }
         *   return mapResults;
         * </pre>
         * <p>
         * Note: if processAll() call throws an exception, only the entries that
         * were removed from the setEntries would be considered successfully
         * processed and the corresponding changes made to the underlying Map;
         * changes made to the remaining entries or any other entries obtained
         * from {@link BackingMapContext#getBackingMapEntry} will not be
         * processed.
         *
         * @param setEntries  a Set of InvocableMap.Entry objects to process
         *
         * @return a Map containing the results of the processing, up to one
         *         entry for each InvocableMap.Entry that was processed, keyed
         *         by the keys of the Map that were processed, with a corresponding
         *         value being the result of the processing for each key
         */
        public default Map<K, R> processAll(Set<? extends Entry<K, V>> setEntries)
            {
            Map<K, R>    mapResults = new LiteMap<>();
            GuardContext ctxGuard   = GuardSupport.getThreadContext();
            long         cMillis    = ctxGuard == null ? 0L : ctxGuard.getTimeoutMillis();

            for (Iterator<? extends Entry<K, V>> iter = setEntries.iterator(); iter.hasNext(); )
                {
                Entry<K, V> entry = iter.next();

                mapResults.put(entry.getKey(), process(entry));

                iter.remove();

                if (ctxGuard != null)
                    {
                    ctxGuard.heartbeat(cMillis);
                    }
                }
            return mapResults;
            }
        }


    // ----- EntryAggregator interface --------------------------------------

    /**
     * An EntryAggregator represents processing that can be directed to occur
     * against some subset of the entries in an InvocableMap, resulting in a
     * aggregated result. Common examples of aggregation include functions such
     * as min(), max() and avg(). However, the concept of aggregation applies to
     * any process that needs to evaluate a group of entries to come up with a
     * single answer.
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     * @param <R> the type of the value returned by the EntryAggregator
     */
    public interface EntryAggregator<K, V, R>
            extends Serializable
        {
        /**
         * Process a set of InvocableMap.Entry objects in order to produce an
         * aggregated result.
         *
         * @param setEntries a Set of read-only InvocableMap.Entry objects to
         *                   aggregate
         *
         * @return the aggregated result from processing the entries
         */
        public R aggregate(Set<? extends Entry<? extends K, ? extends V>> setEntries);
        }

    // ----- StreamingAggregator interface ----------------------------------

    /**
     * A StreamingAggregator is an extension of {@link EntryAggregator} that
     * processes entries in a streaming fashion and provides better control
     * over {@link #characteristics() execution characteristics}.
     * <p>
     * It is strongly recommended that all new custom aggregator implementations
     * implement this interface directly and override default implementation of
     * the {@link #characteristics()} method which intentionally errs on a
     * conservative side.
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     * @param <P> the type of the partial result
     * @param <R> the type of the final result
     *
     * @see EntryAggregator
     */
    public interface StreamingAggregator<K, V, P, R>
            extends EntryAggregator<K, V, R>
        {
        /**
         * Create a new instance of this aggregator.
         *
         * @return a StreamAggregator
         */
        public StreamingAggregator<K, V, P, R> supply();

        /**
         * Accumulate multiple entries into the result.
         * <p>
         * <b>Important note:</b> The default implementation of this method provides
         * necessary logic for aggregation short-circuiting and should rarely
         * (if ever) be overridden by the custom aggregator implementation.
         *
         * @param streamer  a {@link Streamer} that can be used to iterate over
         *                  entries to add
         *
         * @return <code>true</code> to continue the aggregation, and <code>false</code> to
         *         signal to the caller that the result is ready and the
         *         aggregation can be short-circuited
         */
        public default boolean accumulate(Streamer<? extends Entry<? extends K, ? extends V>> streamer)
            {
            while (streamer.hasNext())
                {
                if (!accumulate(streamer.next()))
                    {
                    return false;
                    }
                }

            return true;
            }

        /**
         * Accumulate one entry into the result.
         *
         * @param entry the entry to accumulate into the aggregation result
         *
         * @return <code>true</code> to continue the aggregation, and <code>false</code> to
         *         signal to the caller that the result is ready and the
         *         aggregation can be short-circuited
         */
        public boolean accumulate(Entry<? extends K, ? extends V> entry);

        /**
         * Merge another partial result into the result.
         *
         * @param partialResult  the partial result to merge
         *
         * @return <code>true</code> to continue the aggregation, and <code>false</code> to
         *         signal to the caller that the result is ready and the
         *         aggregation can be short-circuited
         */
        public boolean combine(P partialResult);

        /**
         * Return the partial result of the aggregation.
         *
         * @return the partial result of the aggregation
         */
        public P getPartialResult();

        /**
         * Return the final result of the aggregation.
         *
         * @return the final result of the aggregation
         */
        public R finalizeResult();

        /**
         * A bit mask representing the set of characteristics of this aggregator.
         * <p>
         * Be default, characteristics are a combination of {@link #PARALLEL}
         * and {@link #RETAINS_ENTRIES}, which is sub-optimal and should be
         * overridden by the aggregator implementation if the aggregator does not
         * need to retain entries (which is often the case).
         *
         * @return a bit mask representing the set of characteristics of this aggregator
         *
         * @see #PARALLEL
         * @see #SERIAL
         * @see #BY_MEMBER
         * @see #BY_PARTITION
         * @see #RETAINS_ENTRIES
         * @see #PRESENT_ONLY
         */
        public default int characteristics()
            {
            return PARALLEL | RETAINS_ENTRIES;
            }

        /**
         * A convenience accessor to check if this streamer is {@link #PARALLEL}.
         *
         * @return <code>true</code> if this streamer is {@link #PARALLEL}, false otherwise
         */
        public default boolean isParallel()
            {
            return (characteristics() & PARALLEL) != 0;
            }

        /**
         * A convenience accessor to check if this streamer is {@link #SERIAL}.
         *
         * @return <code>true</code> if this streamer is {@link #SERIAL}, false otherwise
         */
        public default boolean isSerial()
            {
            return (characteristics() & SERIAL) != 0;
            }

        /**
         * A convenience accessor to check if this streamer is {@link #BY_MEMBER}.
         *
         * @return <code>true</code> if this streamer is {@link #BY_MEMBER}, false otherwise
         */
        public default boolean isByMember()
            {
            return (characteristics() & BY_MEMBER) != 0;
            }

        /**
         * A convenience accessor to check if this streamer is {@link #BY_PARTITION}.
         *
         * @return <code>true</code> if this streamer is {@link #BY_PARTITION}, false otherwise
         */
        public default boolean isByPartition()
            {
            return (characteristics() & BY_PARTITION) != 0;
            }

        /**
         * A convenience accessor to check if this streamer is {@link #RETAINS_ENTRIES}.
         *
         * @return <code>true</code> if this streamer is {@link #RETAINS_ENTRIES}, false otherwise
         */
        public default boolean isRetainsEntries()
            {
            return (characteristics() & RETAINS_ENTRIES) != 0;
            }

        /**
         * A convenience accessor to check if this streamer is {@link #PRESENT_ONLY}.
         *
         * @return <code>true</code> if this streamer is {@link #PRESENT_ONLY}, false otherwise
         */
        public default boolean isPresentOnly()
            {
            return (characteristics() & PRESENT_ONLY) != 0;
            }

        // ----- EntryAggregator interface ----------------------------------

        @Override
        public default R aggregate(Set<? extends Entry<? extends K, ? extends V>> setEntries)
            {
            StreamingAggregator<K, V, P, R> aggregator = supply();

            if (isPresentOnly())
                {
                Stream<? extends Entry<? extends K, ? extends V>> stream =
                        setEntries.stream().filter(entry -> entry.isPresent());
                aggregator.accumulate(new SimpleStreamer<>(stream));
                }
            else
                {
                aggregator.accumulate(new SimpleStreamer<>(setEntries));
                }

            return aggregator.finalizeResult();
            }

        // ----- constants --------------------------------------------------

        /**
         * A flag specifying that this aggregator should be executed in parallel.
         * <p>
         * An additional hint can be provided by combining this flag with {@link
         * #BY_PARTITION} flag, which would suggest to further parallelize the
         * server side aggregation by splitting it per-partitions. In the absence
         * of this flag, Coherence is free to decide which strategy to use based
         * on the internal metrics.
         */
        public static int PARALLEL        = 0x00000001;

        /**
         * A flag specifying that this aggregator should be executed serially.
         * <p>
         * An additional hint can be provided by combining this flag with either
         * {@link #BY_MEMBER} or {@link #BY_PARTITION} flag, which are mutually
         * exclusive. In the absence of either, Coherence is free to decide which
         * strategy to use based on the internal metrics.
         */
        public static int SERIAL          = 0x00000002;

        /**
         * A flag specifying that it might be beneficial to execute this aggregator
         * member-by-member.
         * <p>
         * This can be beneficial when there is a high chance for the aggregation
         * to compute the result based solely on the one member worth set of entries.
         * <p>
         * Note: this flag is meaningful only for {@link #SERIAL serial execution}.
         */
        public static int BY_MEMBER       = 0x00000004;

        /**
         * A flag specifying that it might be beneficial to execute this aggregator
         * partition-by-partition. This implies that the entries from each partition
         * will be processed independently and a partial result will be created
         * for each partition.
         * <p>
         * This can be beneficial when accumulation of individual entries is
         * computationally intensive and would benefit from additional parallelization
         * within each storage-enabled member. In this case, the partial results
         * for all the partitions on a given member will be combined into a single
         * partial result, which will then be sent back to the client for further
         * aggregation.
         */
        public static int BY_PARTITION    = 0x00000008;

        /**
         * A flag specifying that this aggregator retains {@link Entry} instances
         * passed to {@link StreamingAggregator#accumulate(Entry)} method, which will force creation
         * of a separate {@link Entry} instance for each cache entry.
         * <p>
         * Please note that this flag is specified by default for backwards
         * compatibility reasons, but if the aggregator does not retain entries
         * it should be "unset" by overriding {@link #characteristics()} method
         * in order to reduce the amount of garbage that is created during the
         * aggregation.
         */
        public static int RETAINS_ENTRIES = 0x00000010;

        /**
         * A flag specifying that this aggregator is only interested in entries
         * that are present in the cache and that the entries which are not present
         * should never be passed to the {@link StreamingAggregator#accumulate(Entry)} method.
         */
        public static int PRESENT_ONLY    = 0x00000020;
        }

    // ----- ParallelAwareAggregator interface ------------------------------

    /**
     * A ParallelAwareAggregator is an advanced extension to EntryAggregator
     * that is explicitly capable of being run in parallel, for example in a
     * distributed environment.
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     * @param <P> the type of the intermediate result during the parallel stage
     * @param <R> the type of the value returned by the ParallelAwareAggregator
     *
     * @deprecated This interface was deprecated in Coherence 12.2.1 and might be
     *             removed in a future release. Use {@link StreamingAggregator}
     *             instead.
     */
    @Deprecated
    public interface ParallelAwareAggregator<K, V, P, R>
            extends EntryAggregator<K, V, R>
        {
        /**
         * Get an aggregator that can take the place of this aggregator in
         * situations in which the InvocableMap can aggregate in parallel.
         * <p>
         * If the returned aggregator is a {@link PartialResultAggregator}, the
         * partial results of the aggregation <em>may</em> be further
         * {@link PartialResultAggregator#aggregatePartialResults aggregated}
         * where optimal.
         *
         * @return the aggregator that can be run in parallel
         */
        public EntryAggregator<K, V, P> getParallelAggregator();

        /**
         * Aggregate the results of the partial aggregations into a
         * final result.
         *
         * @param collResults the partial aggregation results
         *
         * @return the aggregation of the partial aggregation results
         */
        public R aggregateResults(Collection<P> collResults);

        // ----- PartialResultAggregator interface --------------------------

        /**
         * PartialResultAggregator allows for the intermediate {@link
         * #aggregatePartialResults aggregation} of the partial results of a
         * {@link ParallelAwareAggregator parallel aggregation}.
         *
         * @deprecated This interface was deprecated in Coherence 12.2.1 and might be
         *             removed in a future release. Use {@link StreamingAggregator}
         *             instead.
         */
        @Deprecated
        public interface PartialResultAggregator<P>
            {
            /**
             * Aggregate the results of the parallel aggregations, producing a
             * partial result logically representing the partial aggregation.
             * The returned partial result will be further {@link
             * ParallelAwareAggregator#aggregateResults aggregated} to produce
             * the final result.
             *
             * @param colPartialResults the partial results
             *
             * @return an aggregation of the collection of partial results
             */
            public P aggregatePartialResults(Collection<P> colPartialResults);
            }
        }
    }
