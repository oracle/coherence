/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
* An abstract base class for the JCache CacheLoader.
*
* @author cp 2003.05.29
*/
public abstract class AbstractCacheLoader<K, V>
        extends Base
        implements CacheLoader<K, V>
    {
    // ----- CacheLoader interface ------------------------------------------

    /**
    * Return the value associated with the specified key, or null if the
    * key does not have an associated value in the underlying store.
    *
    * @param key  key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or
    *         <tt>null</tt> if no value is available for that key
    */
    public abstract V load(K key);

    /**
    * Return the values associated with each the specified keys in the
    * passed collection. If a key does not have an associated value in
    * the underlying store, then the return map will not have an entry
    * for that key.
    *
    * @param colKeys  a collection of keys to load
    *
    * @return a Map of keys to associated values for the specified keys
    */
    public Map<K, V> loadAll(Collection<? extends K> colKeys)
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
