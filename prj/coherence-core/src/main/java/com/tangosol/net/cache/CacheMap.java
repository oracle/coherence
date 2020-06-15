/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import java.util.function.BiConsumer;


/**
* A CacheMap is a Map that supports caching. This interface will be
* eventually replaced by the javax.cache.Cache interface.
*
* @param <K>  the type of the Map entry keys
* @param <V>  the type of the Map entry values
*
* @author gg  2004.01.05
*
* @since Coherence 2.3
*/
public interface CacheMap<K, V>
        extends ObservableMap<K, V>
    {
    /**
    * Get all the specified keys, if they are in the cache. For each key
    * that is in the cache, that key and its corresponding value will be
    * placed in the map that is returned by this method. The absence of
    * a key in the returned map indicates that it was not in the cache,
    * which may imply (for caches that can load behind the scenes) that
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
    * @param colKeys  a collection of keys that may be in the named cache
    *
    * @return a Map of keys to values for the specified keys passed in
    *         <tt>colKeys</tt>
    *
    * @since Coherence 2.5
    */
    public Map<K, V> getAll(Collection<? extends K> colKeys);

    /**
    * Associates the specified value with the specified key in this cache.
    * If the cache previously contained a mapping for this key, the old
    * value is replaced.
    * <p>
    * Invoking this method is equivalent to the following call:
    * <pre>
    *     put(oKey, oValue, EXPIRY_DEFAULT);
    * </pre>
    *
    * @param key     key with which the specified value is to be associated
    * @param value   value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values
    */
    public V put(K key, V value);

    /**
    * Associates the specified value with the specified key in this cache.
    * If the cache previously contained a mapping for this key, the old
    * value is replaced.
    * This variation of the {@link #put(Object oKey, Object oValue)}
    * method allows the caller to specify an expiry (or "time to live")
    * for the cache entry.
    *
    * @param key      key with which the specified value is to be associated
    * @param value    value to be associated with the specified key
    * @param cMillis  the number of milliseconds until the cache entry will
    *                 expire, also referred to as the entry's "time to live";
    *                 pass {@link #EXPIRY_DEFAULT} to use the cache's default
    *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
    *                 indicate that the cache entry should never expire; this
    *                 milliseconds value is <b>not</b> a date/time value, such
    *                 as is returned from System.currentTimeMillis()
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values
    *
    * @throws UnsupportedOperationException if the requested expiry is a
    *         positive value and the implementation does not support expiry
    *         of cache entries
    */
    public V put(K key, V value, long cMillis);

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
    * A special time-to-live value that can be passed to the extended
    * {@link #put(Object, Object, long) put} method to indicate that the
    * cache's default expiry should be used.
    */
    public static final long EXPIRY_DEFAULT = 0L;

    /**
    * A special time-to-live value that can be passed to the extended
    * {@link #put(Object, Object, long) put} method to indicate that the
    * cache entry should never expire.
    */
    public static final long EXPIRY_NEVER   = -1L;
    }
