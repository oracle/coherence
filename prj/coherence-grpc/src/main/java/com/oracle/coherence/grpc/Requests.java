/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.google.protobuf.ByteString;

import java.util.UUID;

/**
 * A factory to simplify creating proto-buffer requests.
 *
 * @author Jonathan Knight  2019.11.01
 * @since 20.06
 */
public final class Requests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor for utility class.
     */
    private Requests()
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a {@link AddIndexRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     * @param format     the serialization format used
     * @param extractor  the serialized ValueExtractor to use to create the index
     *
     * @return a {@link AddIndexRequest}
     */
    public static AddIndexRequest addIndex(String scope, String cacheName, String format, ByteString extractor)
        {
        return addIndex(scope, cacheName, format, extractor, false, null);
        }

    /**
     * Create a {@link AddIndexRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     * @param format     the serialization format used
     * @param extractor  the serialized ValueExtractor to use to create the index
     * @param sorted     a flag indicating whether the index will be sorted
     *
     * @return a {@link AddIndexRequest}
     */
    public static AddIndexRequest addIndex(String scope, String cacheName, String format, ByteString extractor, boolean sorted)
        {
        return addIndex(scope, cacheName, format, extractor, sorted, null);
        }

    /**
     * Create a {@link AddIndexRequest}.
     *
     * @param scope       the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache
     * @param format      the serialization format used
     * @param extractor   the serialized ValueExtractor to use to create the index
     * @param sorted      a flag indicating whether the index will be sorted
     * @param comparator  an optional serialized {@link java.util.Comparator} to use to sort the index
     *
     * @return a {@link AddIndexRequest}
     */
    public static AddIndexRequest addIndex(String scope, String cacheName, String format, ByteString extractor,
                                           boolean sorted, ByteString comparator)
        {

        validateRequest(cacheName, format);
        if (extractor == null || extractor.isEmpty())
            {
            throw new IllegalArgumentException("the serialized extractor cannot be null or empty");
            }

        return AddIndexRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setExtractor(extractor)
                .setSorted(sorted)
                .setComparator(ensureNotNull(comparator))
                .build();
        }

    /**
     * Create a {@link MapListenerRequest} that will initialise the bidirectional channel.
     * <p>
     * After this request has been sent the response observer will receive responses if the
     * underlying cache is released on the proxy, destroyed or truncated.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     * @param format     the serialization format used
     *
     * @return an {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     * for all entries in a cache
     */
    public static MapListenerRequest initListenerChannel(String scope, String cacheName, String format)
        {
        validateRequest(cacheName);

        return MapListenerRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setUid(UUID.randomUUID().toString())
                .setSubscribe(true)
                .setFormat(format)
                .setType(MapListenerRequest.RequestType.INIT)
                .build();
        }

    /**
     * Create a {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     * for a single entry in a cache.
     *
     * @param scope       the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache
     * @param format      the serialization format used
     * @param key         the serialized key that identifies the entry for which to raise events
     * @param lite        {@code true} to indicate that the {@link com.tangosol.util.MapEvent} objects
     *                    do not have to include the OldValue and NewValue property values in order to
     *                    allow optimizations
     * @param priming     a flag set to {@link true} to indicate that this is a priming listener
     * @param mapTrigger  an optional serialized {@link com.tangosol.util.MapTrigger}
     *
     * @return an {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     * for all entries in a cache
     */
    public static MapListenerRequest addKeyMapListener(String scope,
                                                       String cacheName,
                                                       String format,
                                                       ByteString key,
                                                       boolean lite,
                                                       boolean priming,
                                                       ByteString mapTrigger)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("key cannot be null or empty");
            }

        return MapListenerRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setUid(UUID.randomUUID().toString())
                .setSubscribe(true)
                .setType(MapListenerRequest.RequestType.KEY)
                .setKey(key)
                .setLite(lite)
                .setPriming(priming)
                .setTrigger(ensureNotNull(mapTrigger))
                .build();
        }

    /**
     * Create a {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     * for all entries in a cache matching a filter.
     *
     * @param scope       the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache
     * @param format      the serialization format used
     * @param filter      the serialized filter that identifies the entries for which to raise events
     * @param filterId    a unique identifier to identify this filter to the client
     * @param lite        {@code true} to indicate that the {@link com.tangosol.util.MapEvent} objects
     *                    do not have to include the OldValue and NewValue property values in order to
     *                    allow optimizations
     * @param priming     a flag set to {@link true} to indicate that this is a priming listener
     * @param mapTrigger  an optional serialized {@link com.tangosol.util.MapTrigger}
     *
     * @return an {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     * for all entries in a cache
     */
    public static MapListenerRequest addFilterMapListener(String scope,
                                                          String cacheName,
                                                          String format,
                                                          ByteString filter,
                                                          long filterId,
                                                          boolean lite,
                                                          boolean priming,
                                                          ByteString mapTrigger)
        {

        validateRequest(cacheName, format);

        return MapListenerRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setUid(UUID.randomUUID().toString())
                .setSubscribe(true)
                .setType(MapListenerRequest.RequestType.FILTER)
                .setFilter(ensureNotNull(filter))
                .setFilterId(filterId)
                .setLite(lite)
                .setPriming(priming)
                .setTrigger(ensureNotNull(mapTrigger))
                .build();
        }

    /**
     * Create a {@link AggregateRequest}.
     *
     * @param scope       the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache to clear
     * @param format      the serialization format used
     * @param filter      the serialized {@link com.tangosol.util.Filter} or
     *                    {@link com.google.protobuf.ByteString#EMPTY}
     *                    to represent an {@link com.tangosol.util.filter.AlwaysFilter}
     * @param aggregator  the serialized {@link com.tangosol.util.InvocableMap.EntryAggregator}
     *
     * @return a {@link AggregateRequest}
     */
    public static AggregateRequest aggregate(String scope, String cacheName, String format, ByteString filter, ByteString aggregator)
        {
        validateRequest(cacheName, format);
        if (aggregator == null || aggregator.isEmpty())
            {
            throw new IllegalArgumentException("the serialized aggregator cannot be null or empty");
            }

        return AggregateRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setFilter(ensureNotNull(filter))
                .setAggregator(aggregator)
                .build();
        }

    /**
     * Create a {@link AggregateRequest}.
     *
     * @param scope       the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache to clear
     * @param format      the serialization format used
     * @param keys        the serialized entry keys
     * @param aggregator  the serialized {@link com.tangosol.util.InvocableMap.EntryAggregator}
     *
     * @return a {@link AggregateRequest}
     */
    public static AggregateRequest aggregate(String scope, String cacheName, String format, Iterable<ByteString> keys,
                                             ByteString aggregator)
        {
        validateRequest(cacheName, format);
        if (keys == null)
            {
            throw new IllegalArgumentException("the keys parameter cannot be null or empty");
            }
        if (aggregator == null || aggregator.isEmpty())
            {
            throw new IllegalArgumentException("the serialized aggregator cannot be null or empty");
            }

        return AggregateRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .addAllKeys(keys)
                .setAggregator(aggregator)
                .build();
        }

    /**
     * Create a {@link ClearRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     *
     * @return a {@link ClearRequest}
     */
    public static ClearRequest clear(String scope, String cacheName)
        {
        validateRequest(cacheName);
        return ClearRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .build();
        }

    /**
     * Create a {@link ContainsEntryRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to check
     * @param format     the serialization format used
     * @param key        the key of the entry to check if the mapping exists
     * @param value      the value of the entry to check if the mapping exists
     *
     * @return a {@link ContainsEntryRequest}
     */
    public static ContainsEntryRequest containsEntry(String scope, String cacheName, String format, ByteString key, ByteString value)
        {
        return ContainsEntryRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .setValue(value)
                .build();
        }

    /**
     * Create a {@link ClearRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     * @param format     the serialization format used
     * @param key        the key of the entry to check if the mapping exists
     *
     * @return a {@link ContainsKeyRequest}
     */
    public static ContainsKeyRequest containsKey(String scope, String cacheName, String format, ByteString key)
        {
        validateRequest(cacheName, format);
        return ContainsKeyRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .build();
        }

    /**
     * Create a {@link ContainsValueRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to check
     * @param format     the serialization format used
     * @param value      the value of the entry to check if the mapping exists
     *
     * @return a {@link ContainsValueRequest}
     */
    public static ContainsValueRequest containsValue(String scope, String cacheName, String format, ByteString value)
        {
        validateRequest(cacheName, format);
        return ContainsValueRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setValue(value)
                .build();
        }

    /**
     * Create a {@link DestroyRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     *
     * @return a {@link DestroyRequest}
     */
    public static DestroyRequest destroy(String scope, String cacheName)
        {
        validateRequest(cacheName);
        return DestroyRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .build();
        }

    /**
     * Create an {@link EntrySetRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     * @param format     the serialization format used
     * @param filter     the serialized {@link com.tangosol.util.Filter}
     *
     * @return an {@link EntrySetRequest}
     */
    public static EntrySetRequest entrySet(String scope, String cacheName, String format, ByteString filter)
        {
        validateRequest(cacheName, format);
        if (filter == null || filter.isEmpty())
            {
            throw new IllegalArgumentException("the serialized filter cannot be null or empty");
            }
        return EntrySetRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setFilter(filter)
                .build();
        }

    /**
     * Create an {@link EntrySetRequest}.
     *
     * @param scope       the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache
     * @param format      the serialization format used
     * @param filter      the serialized {@link com.tangosol.util.Filter}
     * @param comparator  the serialized {@link java.util.Comparator}
     *
     * @return an {@link EntrySetRequest}
     */
    public static EntrySetRequest entrySet(String scope, String cacheName, String format,
                                           ByteString filter, ByteString comparator)
        {
        validateRequest(cacheName, format);
        if (filter == null || filter.isEmpty())
            {
            throw new IllegalArgumentException("the serialized filter cannot be null or empty");
            }
        if (comparator == null || comparator.isEmpty())
            {
            throw new IllegalArgumentException("the serialized comparator cannot be null or empty");
            }
        return EntrySetRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setFilter(filter)
                .setComparator(comparator)
                .build();
        }

    /**
     * Create a {@link GetRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     * @param format     the serialization format used
     * @param key        the key of the entry to get the value for
     *
     * @return a {@link GetRequest}
     */
    public static GetRequest get(String scope, String cacheName, String format, ByteString key)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }
        return GetRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .build();
        }

    /**
     * Create a {@link GetAllRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     * @param format     the serialization format used
     * @param keys       the keys of the entries to get the values for
     *
     * @return a {@link GetAllRequest}
     */
    public static GetAllRequest getAll(String scope, String cacheName, String format, Iterable<ByteString> keys)
        {
        validateRequest(cacheName, format);
        if (keys == null)
            {
            throw new IllegalArgumentException("the keys iterable cannot be null or empty");
            }
        return GetAllRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .addAllKey(keys)
                .build();
        }

    /**
     * Create a {@link InvokeRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     * @param format     the serialization format used
     * @param key        the serialized key of the entry to invoke the entry processor against
     * @param processor  the serialized entry processor
     *
     * @return a {@link InvokeRequest}
     */
    public static InvokeRequest invoke(String scope, String cacheName, String format, ByteString key, ByteString processor)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }
        if (processor == null || processor.isEmpty())
            {
            throw new IllegalArgumentException("the serialized processor cannot be null or empty");
            }
        return InvokeRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .setProcessor(processor)
                .build();
        }

    /**
     * Create a {@link InvokeAllRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     * @param format     the serialization format used
     * @param keys       the serialized keys of the entries to invoke the entry processor against
     * @param processor  the serialized entry processor
     *
     * @return a {@link InvokeAllRequest}
     */
    public static InvokeAllRequest invokeAll(String scope, String cacheName, String format, Iterable<ByteString> keys,
                                             ByteString processor)
        {
        validateRequest(cacheName, format);
        if (keys == null)
            {
            throw new IllegalArgumentException("the keys parameter cannot be null or empty");
            }
        if (processor == null || processor.isEmpty())
            {
            throw new IllegalArgumentException("the serialized processor cannot be null or empty");
            }
        return InvokeAllRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .addAllKeys(keys)
                .setProcessor(processor)
                .build();
        }

    /**
     * Create a {@link InvokeAllRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     * @param format     the serialization format used
     * @param filter     the serialized filter to identify of the entries to invoke the entry processor against
     * @param processor  the serialized entry processor
     *
     * @return a {@link InvokeAllRequest}
     */
    public static InvokeAllRequest invokeAll(String scope, String cacheName, String format, ByteString filter, ByteString processor)
        {
        validateRequest(cacheName, format);
        if (filter == null || filter.isEmpty())
            {
            throw new IllegalArgumentException("the serialized filter cannot be null or empty");
            }
        if (processor == null || processor.isEmpty())
            {
            throw new IllegalArgumentException("the serialized processor cannot be null or empty");
            }
        return InvokeAllRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setFilter(filter)
                .setProcessor(processor)
                .build();
        }

    /**
     * Create a {@link IsEmptyRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     *
     * @return a {@link IsEmptyRequest}
     */
    public static IsEmptyRequest isEmpty(String scope, String cacheName)
        {
        validateRequest(cacheName);
        return IsEmptyRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .build();
        }

    /**
     * Create an {@link KeySetRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     * @param format     the serialization format used
     * @param filter     the serialized {@link com.tangosol.util.Filter}
     *
     * @return an {@link KeySetRequest}
     */
    public static KeySetRequest keySet(String scope, String cacheName, String format, ByteString filter)
        {
        validateRequest(cacheName, format);
        if (filter == null || filter.isEmpty())
            {
            throw new IllegalArgumentException("the serialized filter cannot be null or empty");
            }
        return KeySetRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setFilter(filter)
                .build();
        }

    /**
     * Create a {@link PageRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     * @param format     the serialization format used
     * @param cookie     the opaque cookie used to track the page being requested
     *
     * @return a {@link PageRequest}
     */
    public static PageRequest page(String scope, String cacheName, String format, ByteString cookie)
        {
        validateRequest(cacheName, format);
        PageRequest.Builder builder = PageRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format);

        if (cookie != null && !cookie.isEmpty())
            {
            builder.setCookie(cookie);
            }
        return builder.build();
        }

    /**
     * Create a {@link PutRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to put the value into
     * @param format     the serialization format used
     * @param key        the key of the entry to update
     * @param value      the value to map to the key in the cache
     *
     * @return a {@link PutRequest} to update the value mapped to a key in a cache
     */
    public static PutRequest put(String scope, String cacheName, String format, ByteString key, ByteString value)
        {
        return put(scope, cacheName, format, key, value, 0L);
        }

    /**
     * Create a {@link PutRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to put the value into
     * @param format     the serialization format used
     * @param key        the key of the entry to update
     * @param value      the value to map to the key in the cache
     * @param ttl        the time to live for the cache entry
     *
     * @return a {@link PutRequest} to update the value mapped to a key in a cache
     */
    public static PutRequest put(String scope, String cacheName, String format, ByteString key, ByteString value, long ttl)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }
        if (value == null)
            {
            throw new IllegalArgumentException("the serialized value cannot be null");
            }
        return PutRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .setValue(value)
                .setTtl(ttl)
                .build();
        }

    /**
     * Create a {@link PutAllRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to put the values into
     * @param format     the serialization format used
     * @param entries    the entries to put into the cache
     *
     * @return a {@link PutAllRequest}
     */
    public static PutAllRequest putAll(String scope, String cacheName, String format, Iterable<Entry> entries)
        {
        validateRequest(cacheName, format);
        if (entries == null)
            {
            throw new IllegalArgumentException("the entries parameter cannot be null");
            }
        return PutAllRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .addAllEntry(entries)
                .build();
        }

    /**
     * Create a {@link PutIfAbsentRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to put the value into
     * @param format     the serialization format used
     * @param key        the key of the entry to update
     * @param value      the value to map to the key in the cache
     *
     * @return a {@link PutIfAbsentRequest} to update the value mapped to a key in a cache if no mapping
     *         exists for the key
     */
    public static PutIfAbsentRequest putIfAbsent(String scope, String cacheName, String format, ByteString key, ByteString value)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }
        if (value == null)
            {
            throw new IllegalArgumentException("the serialized value cannot be null");
            }
        return PutIfAbsentRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .setValue(value).build();
        }

    /**
     * Create a {@link RemoveRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to remove the value
     * @param format     the serialization format used
     * @param key        the key of the entry to remove
     *
     * @return a {@link RemoveRequest} to update the value mapped to a key in a cache if no mapping exists for the key
     */
    public static RemoveRequest remove(String scope, String cacheName, String format, ByteString key)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }
        return RemoveRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .build();
        }

    /**
     * Create a {@link RemoveMappingRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to remove the mapping
     * @param format     the serialization format used
     * @param key        the key of the entry to remove
     * @param value      the value of the existing mapping
     *
     * @return a {@link RemoveMappingRequest} to update the value mapped to a key in a cache if no mapping
     *         exists for the key
     */
    public static RemoveMappingRequest remove(String scope, String cacheName, String format,
                                              ByteString key, ByteString value)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }
        if (value == null)
            {
            throw new IllegalArgumentException("the serialized value cannot be null");
            }
        return RemoveMappingRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .setValue(value)
                .build();
        }

    /**
     * Create a {@link RemoveIndexRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     * @param format     the serialization format used
     * @param extractor  the serialized ValueExtractor to use to remove the index
     *
     * @return a {@link RemoveIndexRequest}
     */
    public static RemoveIndexRequest removeIndex(String scope, String cacheName, String format, ByteString extractor)
        {
        validateRequest(cacheName, format);
        if (extractor == null || extractor.isEmpty())
            {
            throw new IllegalArgumentException("the serialized extractor cannot be null or empty");
            }
        return RemoveIndexRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setExtractor(extractor)
                .build();
        }

    /**
     * Create a {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     * for a single entry in a cache.
     *
     * @param scope       the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache
     * @param format      the serialization format used
     * @param key         the serialized key that identifies the entry for which to raise events
     * @param priming     a flag set to {@link true} to indicate that this is a priming listener
     * @param mapTrigger  an optional serialized {@link com.tangosol.util.MapTrigger}
     *
     * @return an {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     *         for all entries in a cache
     */
    public static MapListenerRequest removeKeyMapListener(String scope,
                                                          String cacheName,
                                                          String format,
                                                          ByteString key,
                                                          boolean priming,
                                                          ByteString mapTrigger)
        {

        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }

        return MapListenerRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setSubscribe(false)
                .setType(MapListenerRequest.RequestType.KEY)
                .setKey(key)
                .setPriming(priming)
                .setTrigger(ensureNotNull(mapTrigger))
                .build();
        }

    /**
     * Create a {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     * for all entries in a cache matching a filter.
     *
     * @param scope       the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache
     * @param format      the serialization format used
     * @param filter      the serialized filter that identifies the entries for which to raise events
     * @param filterId    a unique identifier to identify this filter to the client
     * @param lite        {@code true} to indicate that the {@link com.tangosol.util.MapEvent} objects
     *                    do not have to include the OldValue and NewValue property values in order to
     *                    allow optimizations
     * @param priming     a flag set to {@link true} to indicate that this is a priming listener
     * @param mapTrigger  an optional serialized {@link com.tangosol.util.MapTrigger}
     *
     * @return an {@link MapListenerRequest} that will subscribe to {@link com.tangosol.util.MapEvent MapEvents}
     *         for all entries in a cache
     */
    public static MapListenerRequest removeFilterMapListener(String scope,
                                                             String cacheName,
                                                             String format,
                                                             ByteString filter,
                                                             long filterId,
                                                             boolean lite,
                                                             boolean priming,
                                                             ByteString mapTrigger)
        {

        validateRequest(cacheName, format);

        return MapListenerRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setSubscribe(false)
                .setType(MapListenerRequest.RequestType.FILTER)
                .setFilter(ensureNotNull(filter))
                .setFilterId(filterId)
                .setLite(lite)
                .setPriming(priming)
                .setTrigger(ensureNotNull(mapTrigger))
                .build();
        }

    /**
     * Create a {@link ReplaceRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to remove the mapping
     * @param format     the serialization format used
     * @param key        the key of the entry to remove
     * @param value      the value of the new mapping
     *
     * @return a {@link ReplaceRequest}to update the value mapped to a key in a cache if no mapping exists for the key
     */
    public static ReplaceRequest replace(String scope, String cacheName, String format, ByteString key, ByteString value)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }
        if (value == null)
            {
            throw new IllegalArgumentException("the serialized value cannot be null");
            }
        return ReplaceRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .setValue(value)
                .build();
        }

    /**
     * Create a {@link ReplaceMappingRequest}.
     *
     * @param scope          the scope name to use to obtain the cache from.
     * @param cacheName      the name of the cache to remove the mapping
     * @param format         the serialization format used
     * @param key            the key of the entry to remove
     * @param previousValue  the value of the existing mapping
     * @param newValue       the new value of the new mapping
     *
     * @return a {@link ReplaceMappingRequest} to update the value mapped to a key in a cache if no mapping
     *         exists for the key
     */
    public static ReplaceMappingRequest replace(String scope, String cacheName, String format, ByteString key,
                                                ByteString previousValue, ByteString newValue)
        {
        validateRequest(cacheName, format);
        if (key == null || key.isEmpty())
            {
            throw new IllegalArgumentException("the serialized key cannot be null or empty");
            }
        if (previousValue == null)
            {
            throw new IllegalArgumentException("the serialized previous value cannot be null");
            }
        if (newValue == null)
            {
            throw new IllegalArgumentException("the serialized new value cannot be null");
            }

        return ReplaceMappingRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setKey(key)
                .setPreviousValue(previousValue)
                .setNewValue(newValue)
                .build();
        }

    /**
     * Create a {@link SizeRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache to clear
     *
     * @return a {@link SizeRequest}
     */
    public static SizeRequest size(String scope, String cacheName)
        {
        validateRequest(cacheName);
        return SizeRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .build();
        }

    /**
     * Create a {@link TruncateRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     *
     * @return a {@link TruncateRequest}
     */
    public static TruncateRequest truncate(String scope, String cacheName)
        {
        validateRequest(cacheName);
        return TruncateRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .build();
        }

    /**
     * Create a {@link ValuesRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName  the name of the cache
     * @param format     the serialization format used
     * @param filter     the serialized {@link com.tangosol.util.Filter}
     *
     * @return an {@link ValuesRequest}
     */
    public static ValuesRequest values(String scope, String cacheName, String format, ByteString filter)
        {
        validateRequest(cacheName, format);
        if (filter == null || filter.isEmpty())
            {
            throw new IllegalArgumentException("the serialized filter cannot be null or empty");
            }
        return ValuesRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setFilter(filter)
                .build();
        }

    /**
     * Create a {@link ValuesRequest}.
     *
     * @param scope      the scope name to use to obtain the cache from.
     * @param cacheName   the name of the cache
     * @param format      the serialization format used
     * @param filter      the serialized {@link com.tangosol.util.Filter}
     * @param comparator  the serialized {@link java.util.Comparator}
     *
     * @return an {@link ValuesRequest}
     */
    public static ValuesRequest values(String scope, String cacheName, String format, ByteString filter, ByteString comparator)
        {
        validateRequest(cacheName, format);
        if (filter == null || filter.isEmpty())
            {
            throw new IllegalArgumentException("the serialized filter cannot be null or empty");
            }

        return ValuesRequest.newBuilder()
                .setScope(ensureScope(scope))
                .setCache(cacheName)
                .setFormat(format)
                .setFilter(filter)
                .setComparator(ensureNotNull(comparator))
                .build();
        }

    /**
     * Validate the cache name.
     *
     * @param cacheName  the cache name to validate
     *
     * @throws IllegalArgumentException if the cache name is {@code null}
     */
    private static void validateRequest(String cacheName)
        {
        validateRequest(cacheName, "");
        }

    /**
     * Validate the cache name.
     *
     * @param cacheName  the cache name to validate
     * @param format     the serialization format to validate
     *
     * @throws IllegalArgumentException if the cache name or format are {@code null}
     */
    private static void validateRequest(String cacheName, String format)
        {
        if (cacheName == null)
            {
            throw new IllegalArgumentException("the cache name cannot be null");
            }
        if (format == null)
            {
            throw new IllegalArgumentException("the serialization format cannot be null");
            }
        }

    /**
     * Return the specified {@link com.google.protobuf.ByteString} if it is not
     * {@code null} otherwise return {@link com.google.protobuf.ByteString#EMPTY}.
     *
     * @param b the  {@link com.google.protobuf.ByteString} to test
     *
     * @return the specified {@link com.google.protobuf.ByteString} if it is not
     * {@code null} otherwise return {@link com.google.protobuf.ByteString#EMPTY}
     */
    private static ByteString ensureNotNull(ByteString b)
        {
        return b == null ? ByteString.EMPTY : b;
        }

    /**
     * Ensure the scope value is not null.
     *
     * @param scope  the scope value to check
     *
     * @return the scope value if not null otherwise the default scope value
     */
    private static String ensureScope(String scope)
        {
        return scope != null ? scope : DEFAULT_SCOPE;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default scope value.
     */
    public static final String DEFAULT_SCOPE = "";
    }
