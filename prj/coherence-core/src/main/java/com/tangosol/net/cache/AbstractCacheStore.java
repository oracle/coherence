/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


/**
* An abstract base class for the JCache CacheLoader.
*
* @author cp 2003.05.29
* @author jh 2005.09.01
*/
public abstract class AbstractCacheStore<K, V>
        extends AbstractCacheLoader<K, V>
        implements CacheStore<K, V>
    {
    // ----- CacheStore interface -------------------------------------------

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
    public void store(K key, V value)
        {
        throw new UnsupportedOperationException();
        }

    /**
    * Remove the specified key from the underlying store if present.
    *
    * @param key key whose mapping is being removed from the cache
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void erase(K key)
        {
        throw new UnsupportedOperationException();
        }
    }
