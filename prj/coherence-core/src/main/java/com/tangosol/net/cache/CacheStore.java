/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import java.util.Collection;
import java.util.Map;


/**
* A JCache cache store.
*
* @since Coherence 2.2
* @author cp 2003.05.29
*/
public interface CacheStore<K, V>
        extends CacheLoader<K, V>
    {
    /**
    * Store the specified value under the specified key in the underlying
    * store. This method is intended to support both key/value creation
    * and value update for a specific key.
    *
    * @param key    key to store the value under
    * @param value  value to be stored
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void store(K key, V value);

    /**
    * Store the specified values under the specified keys in the underlying
    * store. This method is intended to support both key/value creation
    * and value update for the specified keys.
    * <p>
    * If this operation fails (by throwing an exception) after a partial
    * success, the convention is that entries which have been stored
    * successfully are to be removed from the specified <tt>mapEntries</tt>,
    * indicating that the store operation for the entries left in the map has
    * failed or has not been attempted.
    *
    * @param mapEntries   a Map of any number of keys and values to store
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void storeAll(Map<? extends K, ? extends V> mapEntries);

    /**
    * Remove the specified key from the underlying store if present.
    *
    * @param key key whose mapping is being removed from the cache
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void erase(K key);

    /**
    * Remove the specified keys from the underlying store if present.
    * <p>
    * If this operation fails (by throwing an exception) after a partial
    * success, the convention is that keys which have been erased successfully
    * are to be removed from the specified <tt>colKeys</tt>, indicating that
    * the erase operation for the keys left in the collection has failed or has
    * not been attempted.
    *
    * @param colKeys  keys whose mappings are being removed from the cache
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void eraseAll(Collection<? extends K> colKeys);
    }
