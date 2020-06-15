/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.internal.util.DefaultAsyncNamedCache;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.AsynchronousAgent;
import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
* A Map-based data-structure that manages entries across one or more processes.
* Entries are typically managed in memory, and are often comprised of data
* that is also stored in an external system, for example a database, or data
* that has been assembled or calculated at some significant cost.  Such
* entries are referred to as being <i>cached</i>.
*
* @param <K>  the type of the cache entry keys
* @param <V>  the type of the cache entry values
*
* @author gg  2002.03.27
*
* @since Coherence 1.1.2
*/
public interface NamedCache<K, V>
        extends NamedMap<K, V>, CacheMap<K, V>
    {
    /**
    * Return the cache name.
    *
    * @return the cache name
    */
    public String getCacheName();

    /**
    * Return the CacheService that this NamedCache is a part of.
    *
    * @return the CacheService
    */
    public CacheService getCacheService();

    /**
    * Associates the specified value with the specified key in this cache and
    * allows to specify an expiry for the cache entry.
    * <p>
    * <b>Note: Though NamedCache interface extends {@link CacheMap},
    * not all implementations currently support this functionality.</b>
    * <p>
    * For example, if a cache is configured to be a
    * <a href="http://www.tangosol.com/UserGuide-Reference-CacheConfig.jsp#descref-cc-replicated-scheme">replicated</a>,
    * <a href="http://www.tangosol.com/UserGuide-Reference-CacheConfig.jsp#descref-cc-optimistic-scheme">optimistic</a>
    * or
    * <a href="http://www.tangosol.com/UserGuide-Reference-CacheConfig.jsp#descref-cc-distributed-scheme">distributed</a>
    * cache then its backing map must be configured as a
    * <a href="http://www.tangosol.com/UserGuide-Reference-CacheConfig.jsp#descref-cc-local-scheme">local cache</a>.
    * If a cache is configured to be a
    * <a href="http://www.tangosol.com/UserGuide-Reference-CacheConfig.jsp#descref-cc-near-scheme">near cache</a>
    * then the front map must to be configured as a local cache and the back map must support this feature as well,
    * typically by being a distributed cache backed by a local cache (as above.)
    *
    * @throws UnsupportedOperationException if the requested expiry is a
    *         positive value and the implementation does not support expiry
    *         of cache entries
    *
    * @since Coherence 2.3
    */
    public V put(K key, V value, long cMillis);

    /**
     * Perform the given action for each entry selected by the specified key set
     * until all entries have been processed or the action throws an exception.
     * <p>
     * Exceptions thrown by the action are relayed to the caller.
     * <p>
     * The implementation processes each entry on the client and should only be
     * used for read-only client-side operations (such as adding cache entries to
     * a UI widget, for example).
     * <p>
     * Any entry mutation caused by the specified action will not be propagated
     * to the server when this method is called on a distributed cache, so it
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
     * Request a specific type of reference to a {@link NamedCache} that this
     * {@link NamedCache} may additionally implement or support.
     *
     * @param clzNamedCache  the class of {@link NamedCache}
     * @param <C>            the type of {@link NamedCache}
     *
     * @return  a {@link NamedCache} of the requested type
     *
     * @throws UnsupportedOperationException  when this {@link NamedCache}
     *                                        doesn't support or implement
     *                                        the requested class
     */
    public default <C extends NamedCache<K, V>> C as(Class<C> clzNamedCache)
        {
        Base.azzert(clzNamedCache != null, "The specified Class can't be null");

        //noinspection ConstantConditions
        if (clzNamedCache.isInstance(this))
            {
            //noinspection unchecked
            return (C) this;
            }
        else
            {
            throw new UnsupportedOperationException(
                    "The NamedCache [" + this.getCacheName() +
                    "] doesn't implement or support [" + clzNamedCache + "]");
            }
        }

    // ----- Async API ------------------------------------------------------

    /**
     * Return an asynchronous wrapper for this NamedCache.
     * <p>
     * By default, the order of execution of asynchronous operation invoked on
     * the returned AsyncNamedCache will be preserved by ensuring that all
     * operations invoked from the same client thread are executed on the server
     * sequentially, using the same {@link AsynchronousAgent#getUnitOfOrderId
     * unit-of-order}. This tends to provide best performance for fast,
     * non-blocking operations.
     * <p>
     * However, when invoking CPU-intensive or blocking operations, such as
     * read- or write-through operations that access remote database or web
     * service, for example, it may be very beneficial to allow the server to
     * parallelize execution by passing {@link AsyncNamedCache.OrderBy#none()}
     * configuration option to the {@link #async(AsyncNamedCache.Option...)}
     * method. Note, that in that case there are no guarantees for the order
     * of execution.
     *
     * @return  asynchronous wrapper for this NamedCache
     */
    public default AsyncNamedCache<K, V> async()
        {
        // NOTE: while not strictly required any longer, we need to keep this
        // method in order to preserve binary compatibility with 12.2.1.0.0
        return new DefaultAsyncNamedCache<>(this, null);
        }

    /**
     * Return an asynchronous wrapper for this NamedCache.
     * <p>
     * By default, the order of execution of asynchronous operation invoked on
     * the returned AsyncNamedCache will be preserved by ensuring that all
     * operations invoked from the same client thread are executed on the server
     * sequentially, using the same {@link AsynchronousAgent#getUnitOfOrderId
     * unit-of-order}. This tends to provide best performance for fast,
     * non-blocking operations.
     * <p>
     * However, when invoking CPU-intensive or blocking operations, such as
     * read- or write-through operations that access remote database or web
     * service, for example, it may be very beneficial to allow the server to
     * parallelize execution by passing {@link AsyncNamedCache.OrderBy#none()}
     * configuration option to this method. Note, that in that case there are
     * no guarantees for the order of execution.
     *
     * @param options  the configuration options
     *
     * @return  asynchronous wrapper for this NamedCache
     */
    public default AsyncNamedCache<K, V> async(AsyncNamedCache.Option... options)
        {
        return new DefaultAsyncNamedCache<>(this, options);
        }

    // ----- View API -------------------------------------------------------

    /**
     * Construct a {@code view} of this {@link NamedCache}.
     *
     * @param <V_FRONT>  the type of the entry values in this {@code view}, which
     *                   will be the same as {@code V_BACK}, unless a {@code transformer} is specified
     *                   when creating this {@code view}
     *
     * @return a local {@code view} for this {@link NamedCache}
     *
     * @see ViewBuilder
     *
     * @since 12.2.1.4
     */
    public default <V_FRONT> ViewBuilder<K, V, V_FRONT> view()
        {
        return new ViewBuilder<>(this);
        }
    }
