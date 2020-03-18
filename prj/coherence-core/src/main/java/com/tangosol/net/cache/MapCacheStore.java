/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.SimpleEnumerator;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;


/**
* A CacheStore that sits directly on top of a Map.
*
* @since Coherence 2.5
* @author cp 2004.09.24
*/
public class MapCacheStore<K, V>
        extends AbstractCacheStore<K, V>
        implements CacheStore<K, V>, IterableCacheLoader<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a CacheStore that delegates to a Map.
    *
    * @param map  the Map to use as the underlying store for this CacheStore
    */
    public MapCacheStore(Map<K, V> map)
        {
        setMap(map);
        }

    // ----- CacheStore interface -------------------------------------------

    /**
    * Return the value associated with the specified key, or null if the
    * key does not have an associated value in the underlying store.
    *
    * @param key  key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or
    *         <tt>null</tt> if no value is available for that key
    */
    public V load(K key)
        {
        return getMap().get(key);
        }

    /**
    * Store the specified value under the specified key in the underlying
    * store. This method is intended to support both key/value creation
    * and value update for a specific key.
    *
    * @param key    key to store the value under
    * @param value  value to be stored
    */
    public void store(K key, V value)
        {
        // "put blind" optimization
        getMap().putAll(Collections.singletonMap(key, value));
        }

    /**
    * Store the specified values under the specified keys in the underlying
    * store. This method is intended to support both key/value creation
    * and value update for the specified keys.
    *
    * @param mapEntries   a Map of any number of keys and values to store
    */
    public void storeAll(Map<? extends K, ? extends V> mapEntries)
        {
        getMap().putAll(mapEntries);
        }

    /**
    * Remove the specified key from the underlying store if present.
    *
    * @param key  key to remove from the store
    */
    public void erase(K key)
        {
        // "remove blind" optimization
        getMap().keySet().remove(key);
        }

    /**
    * Remove the specified keys from the underlying store if present.
    *
    * @param colKeys  keys whose mappings are being removed from the cache
    */
    public void eraseAll(Collection<? extends K> colKeys)
        {
        getMap().keySet().removeAll(colKeys);
        }


    // ----- IterableCacheLoader interface ----------------------------------

    /**
    * Iterate all keys in the underlying store.
    *
    * @return a read-only iterator of the keys in the underlying store
    */
    public Iterator<K> keys()
        {
        // use SimpleEnumerator to avoid possible
        // ConcurrentModificationException exceptions
        return new SimpleEnumerator<>(getMap().keySet());
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Returns the Map that this CacheStore uses for its storage.
    *
    * @return the Map that this CacheStore uses
    */
    public Map<K, V> getMap()
        {
        return m_map;
        }

    /**
    * Configures the Map that this CacheStore uses for its storage.
    *
    * @param map  the Map that this CacheStore will use
    */
    protected void setMap(Map<K, V> map)
        {
        m_map = map;
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The underlying Map.
    */
    private Map<K, V> m_map;
    }
