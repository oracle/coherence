/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Binary;

import java.util.Iterator;


/**
* A simple mapping of CacheStore operations for Binary objects into a
* Java interface.
*
* @see com.tangosol.net.cache.CacheStore
*
* @since Coherence 2.2
* @author cp 2003.05.26
*/
public interface BinaryStore
    {
    /**
    * Return the value associated with the specified key, or null if the
    * key does not have an associated value in the underlying store.
    *
    * @param binKey  key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or
    *         <tt>null</tt> if no value is available for that key
    */
    public Binary load(Binary binKey);

    /**
    * Store the specified value under the specific key in the underlying
    * store. This method is intended to support both key/value creation
    * and value update for a specific key.
    *
    * @param binKey    key to store the value under
    * @param binValue  value to be stored
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void store(Binary binKey, Binary binValue);

    /**
    * Remove the specified key from the underlying store if present.
    *
    * @param binKey key whose mapping is to be removed from the map
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void erase(Binary binKey);

    /**
    * Remove all data from the underlying store.
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void eraseAll();

    /**
    * Iterate all keys in the underlying store.
    *
    * @return a read-only iterator of the keys in the underlying store
    *
    * @throws UnsupportedOperationException  if the underlying store is not
    *         iterable
    */
    public Iterator<Binary> keys();

    /**
    * If a BinaryStore is aware of the number of keys that it stores, then it
    * should implement this optional interface in order to allow that
    * information to be efficiently communicated to an intelligent consumer of
    * the BinaryStore interface.
    *
    * @since Coherence 3.7
    */
    public interface SizeAware
            extends BinaryStore
        {
        /**
        * Determine the number of keys in the BinaryStore.
        *
        * @return the number of keys in the BinaryStore
        *
        * @since Coherence 3.7
        */
        public int size();
        }

    /**
    * If a BinaryStore is aware of which keys that it stores, then it should
    * implement this optional interface in order to allow that information
    * to be efficiently communicated to an intelligent consumer of the BinaryStore
    * interface.
    *
    * @since Coherence 12.1.2
    */
    public interface KeySetAware
            extends SizeAware
        {
        /**
        * Return <tt>true</tt> iff this BinaryStore contains a mapping for the
        * specified key.
        *
        * @param binKey  key whose presence in the BinaryStore is to be tested
        *
        * @return true iff this BinaryStore contains a mapping for the specified key
        */
        public boolean containsKey(Binary binKey);
        }
    }
