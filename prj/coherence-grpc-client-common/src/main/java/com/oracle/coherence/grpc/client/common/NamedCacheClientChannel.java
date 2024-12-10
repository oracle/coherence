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

import com.oracle.coherence.grpc.MaybeByteString;

import com.oracle.coherence.grpc.client.common.v0.NamedCacheClientChannel_V0;
import com.oracle.coherence.grpc.client.common.v1.NamedCacheClientChannel_V1;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapTriggerListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import java.util.stream.Stream;

/**
 * A channel that sends requests and handles responses for a specific
 * {@link NamedCache} over a {@link GrpcConnection}.
 */
public interface NamedCacheClientChannel
        extends ClientProtocol
    {
    /**
     * Create an instance of a {@link NamedCacheClientChannel}.
     *
     * @param dependencies the {@link AsyncNamedCacheClient.Dependencies} to use
     * @param connection   the {@link GrpcConnection} to use
     *
     * @return an instance of a {@link NamedCacheClientChannel} supporting
     *         the requested version
     */
    static NamedCacheClientChannel createProtocol(AsyncNamedCacheClient.Dependencies dependencies, GrpcConnection connection)
        {
        int nVersion = connection.getProtocolVersion();
        if (nVersion >= 1)
            {
            return new NamedCacheClientChannel_V1(dependencies, connection);
            }
        return new NamedCacheClientChannel_V0(dependencies, connection);
        }

    /**
     * Return the dependencies for this channel.
     *
     * @return the dependencies for this channel
     */
    AsyncNamedCacheClient.Dependencies getDependencies();

    /**
     * Asynchronous implementation of {@link NamedCache#addIndex}.
     *
     * @param extractor  the ValueExtractor object that is used to extract an
     *                   indexable Object from a value stored in the indexed
     *                   Map.  Must not be {@code null}
     * @param fOrdered   true iff the contents of the indexed information should
     *                   be ordered; false otherwise
     * @param comparator the Comparator object which imposes an ordering on
     *                   entries in the indexed map; or <tt>null</tt> if the
     *                   entries' values natural ordering should be used
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    CompletableFuture<Void> addIndex(ByteString extractor, boolean fOrdered, ByteString comparator);

    /**
     * Sends the serialized {@link Filter} for registration with the cache server.
     *
     * @param key           the serialized bytes of the {@link Filter}
     * @param fLite         {@code true} to indicate that the {@link MapEvent} objects do
     *                      not have to include the OldValue and NewValue
     *                      property values in order to allow optimizations
     * @param fPriming      {@code true} if this is a priming listener
     * @param fSynchronous  {@code true} if this is a synchronous listener
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    CompletableFuture<Void> addMapListener(ByteString key, boolean fLite, boolean fPriming, boolean fSynchronous);

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
     * @param fSynchronous  {@code true} if this is a synchronous listener
     *
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    CompletableFuture<Void> addMapListener(ByteString filterBytes, long nFilterId, boolean fLite,
                                           ByteString triggerBytes, boolean fSynchronous);

    /**
     * Perform an aggregating operation asynchronously against the entries
     * specified by the passed keys.
     *
     * @param collKeys   the Collection of keys that specify the entries within
     *                   this Map to aggregate across
     * @param aggregator the EntryAggregator that is used to aggregate across
     *                   the specified entries of this Map
     * @return a {@link CompletableFuture} that can be used to obtain the result
     * of the aggregation
     */
    CompletableFuture<BytesValue> aggregate(List<ByteString> collKeys, ByteString aggregator, long nDeadline);

    /**
     * Perform an aggregating operation asynchronously against the set of
     * entries that are selected by the given Filter.
     *
     * @param filter     the Filter that is used to select entries within this
     *                   Map to aggregate across
     * @param aggregator the EntryAggregator that is used to aggregate across
     *                   the selected entries of this Map
     * @return a {@link CompletableFuture} that can be used to obtain the result
     * of the aggregation
     */
    CompletableFuture<BytesValue> aggregate(ByteString filter, ByteString aggregator, long nDeadline);

    /**
     * Removes all the mappings from this map.
     * The map will be empty after this operation completes.
     *
     * @return a {@link CompletableFuture} that is complete when the cache has been cleared
     */
    CompletableFuture<Void> clear();

    /**
     * Determine whether en entry exists in the cache with the specified key and value.
     *
     * @param key   the cache key
     * @param value the cache value
     * @return a page of cache keys
     */
    CompletionStage<BoolValue> containsEntry(ByteString key, ByteString value);

    /**
     * Determine whether a mapping to the specified key exists in the cache.
     *
     * @param key the key to check
     * @return a {@link CompletableFuture} returning {@code true} if the cache contains the given key
     */
    CompletionStage<BoolValue> containsKey(ByteString key);

    /**
     * Helper method for confirm presence of the value within the cache.
     *
     * @param value the value to check
     * @return a {@link CompletableFuture} returning {@code true} if the cache contains the given value
     */
    CompletionStage<BoolValue> containsValue(ByteString value);

    /**
     * Release and destroy this instance of NamedCollection.
     * <p>
     * <b>Warning:</b> This method is used to completely destroy the specified
     * collection across the cluster. All references in the entire cluster to this
     * collection will be invalidated, the collection data will be cleared, and all
     * internal resources will be released.
     */
    CompletableFuture<Void> destroy();

    /**
     * Obtain the value mapped to the specified key.
     *
     * @param key the key to obtain the value for
     * @return a {@link CompletableFuture} that can be used to obtain the result
     * of the aggregation
     */
    CompletionStage<MaybeByteString> get(ByteString key);

    /**
     * Return a {@link Stream} of the values mapped to the specified keys.
     *
     * @param keys the keys to obtain mappings for
     * @return a stream of entries for the specified keys
     */
    Stream<Map.Entry<ByteString, ByteString>> getAll(Iterable<ByteString> keys);

    /**
     * Obtain the next page of a cache entry set.
     *
     * @param cookie  the cookie to use to locate the next page (or {@code null} for the first page)
     *
     * @return a the {@link EntrySetPage}
     */
    EntrySetPage getEntriesPage(ByteString cookie);

    /**
     * Obtain a page of cache keys.
     *
     * @param cookie an opaque cooke used to determine the current page
     * @return a page of cache keys
     */
    Stream<BytesValue> getKeysPage(ByteString cookie);

    /**
     * Invoke the passed EntryProcessor against the Entry specified by the
     * passed key asynchronously, returning a {@link CompletableFuture} that can
     * be used to obtain the result of the invocation.
     *
     * @param key       the key to process; it is not required to exist within
     *                  the Map
     * @param processor the EntryProcessor to use to process the specified key
     * @return a {@link CompletableFuture} that can be used to obtain the result
     * of the invocation
     */
    CompletionStage<BytesValue> invoke(ByteString key, ByteString processor, long nDeadline);

    /**
     * Invoke the passed EntryProcessor against the entries specified by the
     * passed keys asynchronously, returning a {@link CompletableFuture} that
     * can be used to obtain the result of the invocation for each entry.
     *
     * @param collKeys  the keys to process; these keys are not required to
     *                  exist within the Map
     * @param processor the EntryProcessor to use to process the specified keys
     * @return a {@link CompletableFuture} that can be used to obtain the result
     * of the invocation for each entry
     */
    CompletableFuture<Map<ByteString, ByteString>> invokeAll(Collection<ByteString> collKeys,
                                                             ByteString processor, long nDeadline);

    /**
     * Invoke the passed EntryProcessor against the set of entries that are
     * selected by the given Filter asynchronously, returning a {@link
     * CompletableFuture} that can be used to obtain the result of the
     * invocation for each entry.
     *
     * @param filter    a Filter that results in the set of keys to be
     *                  processed
     * @param processor the EntryProcessor to use to process the specified keys
     * @return a {@link CompletableFuture} that can be used to obtain the result
     * of the invocation for each entry
     */
    CompletableFuture<Map<ByteString, ByteString>> invokeAll(ByteString filter, ByteString processor);

    /**
     * Invoke the passed EntryProcessor against the entries specified by the
     * passed keys asynchronously, returning a {@link CompletableFuture} that
     * can be used to determine if the operation completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     * <p/>
     * Note: the callback implementation must be thread-safe as it may be called
     * by multiple worker threads in cases where Coherence splits the operation
     * over multiple partitions.
     *
     * @param colKeys   the keys to process; these keys are not required to
     *                  exist within the Map
     * @param processor the EntryProcessor to use to process the specified keys
     * @param callback  a user-defined callback that will be called for each
     *                  partial result
     * @return a {@link CompletableFuture} that can be used to determine if the
     * operation completed successfully
     */
    CompletableFuture<Void> invokeAll(Collection<ByteString> colKeys, ByteString processor,
                                      Consumer<Map.Entry<ByteString, ByteString>> callback);

    /**
     * Invoke the passed EntryProcessor against the set of entries that are
     * selected by the given Filter asynchronously, returning a {@link
     * CompletableFuture} that can be used to determine if the operation
     * completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     * <p/>
     * Note: the callback implementation must be thread-safe as it may be called
     * by multiple worker threads in cases where Coherence splits the operation
     * over multiple partitions.
     *
     * @param filter    a Filter that results in the set of keys to be
     *                  processed
     * @param processor the EntryProcessor to use to process the specified keys
     * @param callback  a user-defined callback that will be called for each
     *                  partial result
     * @return a {@link CompletableFuture} that can be used to determine if the
     * operation completed successfully
     */
    CompletableFuture<Void> invokeAll(ByteString filter, ByteString processor,
                                      Consumer<Map.Entry<ByteString, ByteString>> callback);

    /**
     * Invoke the passed EntryProcessor against all the entries asynchronously,
     * returning a {@link CompletableFuture} that can be used to determine if
     * the operation completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     * <p/>
     * Note: the callback implementation must be thread-safe as it may be called
     * by multiple worker threads in cases where Coherence splits the operation
     * over multiple partitions.
     *
     * @param processor the EntryProcessor to use to process the specified keys
     * @param callback  a user-defined callback that will be called for each
     *                  partial result
     * @return a {@link CompletableFuture} that can be used to determine if the
     * operation completed successfully
     */
    CompletableFuture<Void> invokeAll(ByteString filter, ByteString processor,
                                      BiConsumer<ByteString, ByteString> callback);

    /**
     * Invoke the passed EntryProcessor against the entries specified by the
     * passed keys asynchronously, returning a {@link CompletableFuture} that
     * can be used to determine if the operation completed successfully.
     * <p>
     * Instead of collecting and returning the complete result, this method
     * will stream partial results of the processor execution to the specified
     * partial result callback, which allows for a much lower memory overhead.
     * <p/>
     * Note: the callback implementation must be thread-safe as it may be called
     * by multiple worker threads in cases where Coherence splits the operation
     * over multiple partitions.
     *
     * @param colKeys   the keys to process; these keys are not required to
     *                  exist within the Map
     * @param processor the EntryProcessor to use to process the specified keys
     * @param callback  a user-defined callback that will be called for each
     *                  partial result
     * @return a {@link CompletableFuture} that can be used to determine if the
     * operation completed successfully
     */
    CompletableFuture<Void> invokeAll(Collection<ByteString> colKeys, ByteString processor,
                                      BiConsumer<ByteString, ByteString> callback);

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    CompletionStage<BoolValue> isEmpty();

    /**
     * Returns whether this cache is ready to be used.
     * </p>
     * An example of when this method would return {@code false} would
     * be where a partitioned cache service that owns this cache has no
     * store-enabled members.
     *
     * @return {@code true} if the cache is ready
     */
    CompletionStage<BoolValue> isReady();

    /**
     * Storing the contents of the provided map within the cache.
     *
     * @param map     the {@link Map} of key/value pairs to store in the cache.
     * @param cMillis the expiry delay t apply to the entries
     * @return a {@link CompletableFuture}
     */
    CompletableFuture<Empty> putAll(Map<ByteString, ByteString> map, long cMillis);

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associates it with the given value and returns
     * {@code null}, else returns the current value.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     * {@code null} if there was no mapping for the key.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with the key,
     * if the implementation supports null values.)
     */
    CompletionStage<BytesValue> putIfAbsent(ByteString key, ByteString value);

    /**
     * Helper method for storing a key/value pair within the cache.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   the time-to-live (may not be supported)
     * @return a {@link CompletableFuture} returning the value previously associated
     * with the specified key, if any
     */
    CompletionStage<BytesValue> put(ByteString key, ByteString value, long ttl);

    /**
     * Remove the mapping for the given key, returning the associated value.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the value associated with the given key, or {@code null} if no mapping existed
     */
    CompletionStage<BytesValue> remove(ByteString key);

    /**
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * @param key   key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return a {@link CompletableFuture} returning a {@code boolean} indicating
     * whether the mapping was removed
     */
    CompletionStage<BoolValue> remove(ByteString key, ByteString value);

    /**
     * Remove an index from this QueryMap.
     *
     * @param extractor the ValueExtractor object that is used to extract an
     *                  indexable Object from a value stored in the Map.
     */
    CompletionStage<Empty> removeIndex(ByteString extractor);

    /**
     * Removes the {@link MapListener} associated with the specified key.
     *
     * @param key      the key the listener is associated with
     * @param fPriming {@code true} if the listener is a priming listener
     * @return a {@link CompletableFuture} returning {@link Void}
     */
    CompletableFuture<Void> removeMapListener(ByteString key, boolean fPriming);

    /**
     * Sends the serialized {@link Filter} for un-registration with the cache server.
     *
     * @param filterBytes  the serialized bytes of the {@link Filter}
     * @param nFilterId    the ID of the {@link Filter}
     * @param triggerBytes the serialized bytes of the {@link MapTriggerListener}; pass {@link ByteString#EMPTY}
     *                     if there is no trigger listener.
     * @return {@link CompletableFuture} returning type {@link Void}
     */
    CompletableFuture<Void> removeMapListener(ByteString filterBytes, long nFilterId, ByteString triggerBytes);

    /**
     * Replaces the entry for the specified key only if it is
     * currently mapped to some value.
     *
     * @param key   key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     * {@code null} if there was no mapping for the key.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with the key,
     * if the implementation supports null values.)
     */
    CompletionStage<BytesValue> replace(ByteString key, ByteString value);

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     *
     * @param key      key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     */
    CompletionStage<BoolValue> replaceMapping(ByteString key, ByteString oldValue, ByteString newValue);

    /**
     * Initialize the event dispatcher.
     *
     * @param dispatcher  the {@link EventDispatcher} to use
     */
    void setEventDispatcher(EventDispatcher dispatcher);

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    CompletionStage<Int32Value> size();

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
     * @return a {@link CompletableFuture} returning {@link Empty}
     */
    CompletionStage<Empty> truncate();

    // ----- inner class: EventDispatcher -----------------------------------

    /**
     * A dispatcher of cache events.
     */
    interface EventDispatcher
        {
        /**
         * Dispatch an event to the registered listeners.
         *
         * @param listFilterIds   the list of filter identifiers that the event is for
         * @param nEventId        the event id
         * @param binKey          the serialized key
         * @param binOldValue     the serialized old value
         * @param binNewValue     the serialized new value
         * @param fSynthetic      {@code true} if this is a synthetic event
         * @param fPriming        {@code true} if this is a priming event
         * @param transformState  the event transformation state
         */
        void dispatch(List<Long> listFilterIds, int nEventId, ByteString binKey, ByteString binOldValue,
                                ByteString binNewValue, boolean fSynthetic, boolean fPriming,
                                CacheEvent.TransformationState transformState);

        /**
         * The cache has been destroyed.
         */
        void onDestroy();

        /**
         * The cache has been truncated.
         */
        void onTruncate();

        /**
         * Increment the listener count.
         */
        void incrementListeners();

        /**
         * Decrement the listener count.
         */
        void decrementListeners();
        }

    // ----- inner class: EntrySetPage --------------------------------------

    /**
     * A set of pages returned by a cache page query.
     *
     * @param cookie   the cookie to use to obtain the next page
     * @param entries  the set of entries in the page
     */
    record EntrySetPage(ByteString cookie, List<Map.Entry<ByteString, ByteString>> entries)
        {
        public boolean isEmpty()
            {
            return entries.isEmpty();
            }
        }
    }
