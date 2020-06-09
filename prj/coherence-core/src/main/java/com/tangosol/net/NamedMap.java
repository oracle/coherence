/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.internal.util.DefaultAsyncNamedCache;

import com.tangosol.util.AsynchronousAgent;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.QueryMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A Map-based data-structure that manages entries across one or more processes.
 * Entries are typically managed in memory, and are often comprised of data
 * that is also stored persistently, on disk.
 *
 * @param <K>  the type of the map entry keys
 * @param <V>  the type of the map entry values
 *
 * @author Aleks Seovic  2020.06.06
 *
 * @since Coherence 14.1.2
 */
public interface NamedMap<K, V>
        extends NamedCollection, ObservableMap<K, V>, ConcurrentMap<K, V>,
                QueryMap<K, V>, InvocableMap<K, V>
    {
    @Override
    default String getName()
        {
        return ((NamedCache<K, V>) this).getCacheName();
        }

    @Override
    default CacheService getService()
        {
        return ((NamedCache<K, V>) this).getCacheService();
        }

    /**
    * Get all the specified keys, if they are in the map. For each key
    * that is in the map, that key and its corresponding value will be
    * placed in the map that is returned by this method. The absence of
    * a key in the returned map indicates that it was not in the map,
    * which may imply (for maps that can load behind the scenes) that
    * the requested data could not be loaded.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation, without regards to threading issues:
    *
    * <pre>
    * Map map = new AnyMap(); // could be a HashMap (but does not have to)
    * for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
    *     {
    *     Object oKey = iter.next();
    *     Object oVal = get(oKey);
    *     if (oVal != null || containsKey(oKey))
    *         {
    *         map.put(oKey, oVal);
    *         }
    *     }
    * return map;
    * </pre>
    *
    * @param colKeys  a collection of keys that may be in the named map
    *
    * @return a Map of keys to values for the specified keys passed in
    *         <tt>colKeys</tt>
    */
    Map<K, V> getAll(Collection<? extends K> colKeys);

    /**
    * Removes all mappings from this map.
    * <p>
    * <b>Note: invoking the {@code clear()} operation against a distributed map
    * can be both a memory and CPU intensive task and therefore is generally
    * not recommended. Either {@link #truncate()} or {@link #destroy()} operations
    * may be suitable alternatives.</b>
    */
    void clear();

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
    * @throws UnsupportedOperationException if the server does not support the truncate operation
    */
    default void truncate()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Perform the given action for each entry selected by the specified key set
     * until all entries have been processed or the action throws an exception.
     * <p>
     * Exceptions thrown by the action are relayed to the caller.
     * <p>
     * The implementation processes each entry on the client and should only be
     * used for read-only client-side operations (such as adding map entries to
     * a UI widget, for example).
     * <p>
     * Any entry mutation caused by the specified action will not be propagated
     * to the server when this method is called on a distributed map, so it
     * should be avoided. The mutating operations on a subset of entries
     * should be implemented using one of {@link InvocableMap#invokeAll},
     * {@link #replaceAll}, {@link #compute}, or {@link #merge} methods instead.
     *
     * @param collKeys  the keys to process; these keys are not required to
     *                  exist within the Map
     * @param action    the action to be performed for each entry
     *
     * @since 12.2.1
     */
    public default void forEach(Collection<? extends K> collKeys, BiConsumer<? super K, ? super V> action)
        {
        Objects.requireNonNull(action);
        getAll(collKeys).forEach(action);
        }

    /**
     * Return an asynchronous wrapper for this NamedMap.
     * <p>
     * By default, the order of execution of asynchronous operation invoked on
     * the returned AsyncNamedMap will be preserved by ensuring that all
     * operations invoked from the same client thread are executed on the server
     * sequentially, using the same {@link AsynchronousAgent#getUnitOfOrderId
     * unit-of-order}. This tends to provide best performance for fast,
     * non-blocking operations.
     * <p>
     * However, when invoking CPU-intensive or blocking operations, such as
     * read- or write-through operations that access remote database or web
     * service, for example, it may be very beneficial to allow the server to
     * parallelize execution by passing {@link AsyncNamedMap.OrderBy#none()}
     * configuration option to this method. Note, that in that case there are
     * no guarantees for the order of execution.
     *
     * @param options  the configuration options
     *
     * @return  asynchronous wrapper for this NamedMap
     */
    default AsyncNamedMap<K, V> async(AsyncNamedMap.Option... options)
        {
        return new DefaultAsyncNamedCache<>((NamedCache<K, V>) this, options);
        }

    /**
     * Construct a {@code view} of this {@link NamedMap}.
     *
     * @param <V_FRONT>  the type of the entry values in this {@code view}, which
     *                   will be the same as {@code V_BACK}, unless a {@code transformer} is specified
     *                   when creating this {@code view}
     *
     * @return a local {@code view} for this {@link NamedMap}
     *
     * @see ViewBuilder
     *
     * @since 12.2.1.4
     */
    public default <V_FRONT> MapViewBuilder<K, V, V_FRONT> view()
        {
        return new MapViewBuilder<>(this);
        }

    // ----- NamedMap.Option interface --------------------------------------

    /**
     * An immutable option for requesting and configuring {@link NamedMap}s.
     */
    interface Option
        {
        }
    }
