/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
* A JCache CacheLoader.
*
* @since Coherence 2.2
* @author cp 2003.05.29
*/
public interface CacheLoader<K, V>
    {
    /**
    * Return the value associated with the specified key, or null if the
    * key does not have an associated value in the underlying store.
    *
    * @param key  key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or
    *         <tt>null</tt> if no value is available for that key
    */
    public V load(K key);

    /**
    * Return the values associated with each the specified keys in the
    * passed collection. If a key does not have an associated value in
    * the underlying store, then the return map will not have an entry
    * for that key.
    * <p>
    * The default implementation of this method calls {@link #load} for each
    * key in the supplied Collection. Implementations that can optimize multi-key
    * operations <code>should</code> override this default implementation.
    *
    * @param colKeys  a collection of keys to load
    *
    * @return a Map of keys to associated values for the specified keys
    */
    public default Map<K, V> loadAll(Collection<? extends K> colKeys)
        {
        Map<K, V> map = new HashMap<>();
        for (K key : colKeys)
            {
            V value = load(key);
            if (value != null)
                {
                map.put(key, value);
                }
            }
        return map;
        }
    }
