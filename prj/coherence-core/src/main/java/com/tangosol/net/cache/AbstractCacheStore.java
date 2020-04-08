/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


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
    * Store the specified values under the specified keys in the underlying
    * store. This method is intended to support both key/value creation
    * and value update for the specified keys.
    * <p>
    * The implementation of this method calls {@link #store} for each entry in
    * the supplied Map. Once stored successfully, an entry is removed from
    * the Map (if possible).
    * <p>
    * <b>Note:</b>
    * For many types of persistent stores, a single store operation is as
    * expensive as a bulk store operation; therefore, subclasses should
    * override this method if possible.
    *
    * @param mapEntries   a Map of any number of keys and values to store
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void storeAll(Map<? extends K, ? extends V> mapEntries)
        {
        boolean fRemove = true;

        for (Iterator<? extends Map.Entry<? extends K, ? extends V>> iter = mapEntries.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry<? extends K, ? extends V> entry = iter.next();
            store(entry.getKey(), entry.getValue());
            if (fRemove)
                {
                try
                    {
                    iter.remove();
                    }
                catch (UnsupportedOperationException e)
                    {
                    fRemove = false;
                    }
                }
            }
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

    /**
    * Remove the specified keys from the underlying store if present.
    * <p>
    * The implementation of this method calls {@link #erase} for each key in
    * the supplied Collection. Once erased successfully, a key is removed from
    * the Collection (if possible).
    * <p>
    * <b>Note:</b>
    * For many types of persistent stores, a single erase operation is as
    * expensive as a bulk erase operation; therefore, subclasses should
    * override this method if possible.
    *
    * @param colKeys  keys whose mappings are being removed from the cache
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void eraseAll(Collection<? extends K> colKeys)
        {
        boolean fRemove = true;

        for (Iterator<? extends K> iter = colKeys.iterator(); iter.hasNext(); )
            {
            erase(iter.next());
            if (fRemove)
                {
                try
                    {
                    iter.remove();
                    }
                catch (UnsupportedOperationException e)
                    {
                    fRemove = false;
                    }
                }
            }
        }
    }
