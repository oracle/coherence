/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.io.BinaryStore;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;

import java.util.Iterator;


/**
* A CacheStore that sits directly on top of a BinaryStore.
*
* @since Coherence 2.5
* @author cp 2004.09.24
*/
public class BinaryStoreCacheStore<K, V>
        extends AbstractCacheStore<K, V>
        implements CacheStore<K, V>, IterableCacheLoader<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a CacheStore that delegates to a BinaryStore.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    */
    public BinaryStoreCacheStore(BinaryStore store)
        {
        setBinaryStore(store);
        }

    /**
    * Create a CacheStore that delegates to a BinaryStore, using the passed
    * ClassLoader for deserialization.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    * @param loader the ClassLoader to use for deserialization
    */
    public BinaryStoreCacheStore(BinaryStore store, ClassLoader loader)
        {
        // set up the loader first so it can be used when the BinaryStore is
        // configured
        setClassLoader(loader);
        setBinaryStore(store);
        }

    /**
    * Create a CacheStore that delegates to a BinaryStore, optionally
    * storing only Binary keys and values.
    *
    * @param store        the BinaryStore to use to write the serialized
    *                     objects to
    * @param fBinaryOnly  true indicates that this CacheStore will only
    *                     manage binary keys and values
    */
    public BinaryStoreCacheStore(BinaryStore store, boolean fBinaryOnly)
        {
        m_fBinaryOnly = fBinaryOnly;
        setBinaryStore(store);
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
        Binary bin = getBinaryStore().load(toBinary(key));
        return bin == null ? null : (V) fromBinary(bin);
        }

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
        getBinaryStore().store(toBinary(key), toBinary(value));
        }

    /**
    * Remove the specified key from the underlying store if present.
    *
    * @param key  key to remove from the store
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void erase(K key)
        {
        getBinaryStore().erase(toBinary(key));
        }


    // ----- IterableCacheLoader interface ----------------------------------

    /**
    * Iterate all keys in the underlying store.
    *
    * @return a read-only iterator of the keys in the underlying store
    *
    * @throws UnsupportedOperationException  if the underlying store is not
    *         iterable
    */
    public Iterator<K> keys()
        {
        Iterator<Binary>     iter = getBinaryStore().keys();
        Converter<Binary, K> conv = bin -> (K) fromBinary(bin);

        return new ConverterCollections.ConverterEnumerator<>(iter, conv);
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Translate the passed Object object into an Binary object.
    *
    * @param o  the Object to serialize into a Binary object
    *
    * @return the Binary object
    */
    protected Binary toBinary(Object o)
        {
        return isBinaryOnly() ? (Binary) o
                              : ExternalizableHelper.toBinary(o); 
        }

    /**
    * Translate the passed Binary object into an Object object.
    *
    * @param bin  the Binary object to deserialize
    *
    * @return the deserialized object
    */
    protected Object fromBinary(Binary bin)
        {
        return isBinaryOnly()
               ? bin
               : ExternalizableHelper.fromBinary(bin, getClassLoader());
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Returns the BinaryStore that this CacheStore uses for its storage.
    *
    * @return the BinaryStore that this CacheStore uses
    */
    public BinaryStore getBinaryStore()
        {
        return m_store;
        }

    /**
    * Configures the BinaryStore that this CacheStore will use for its
    * storage.
    *
    * @param store  the BinaryStore to use
    */
    protected void setBinaryStore(BinaryStore store)
        {
        m_store = store;
        }

    /**
    * Returns the ClassLoader that this CacheStore uses for deserialization,
    * if it has one.
    * 
    * @return the ClassLoader that this CacheStore uses for deserialization;
    *         may be null
    */
    public ClassLoader getClassLoader()
        {
        return m_loader;
        }

    /**
    * Configure the ClassLoader that this CacheStore will use for
    * deserialization.
    * 
    * @param loader  the ClassLoader that this CacheStore should use for
    *                deserialization
    */
    protected void setClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        }

    /**
    * Determine if the keys and values in this CacheStore are known to be all
    * Binary.
    *
    * @return true if all keys and values will be Binary to start with, and
    *         thus will not require conversion
    */
    public boolean isBinaryOnly()
        {
        return m_fBinaryOnly;
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The BinaryStore that this BinaryStoreCacheStore uses to store data.
    */
    private BinaryStore m_store;

    /**
    * The ClassLoader that this BinaryStoreCacheStore uses to deserialize
    * data from the BinaryStore. May be null.
    */
    private ClassLoader m_loader;

    /**
    * An indicator that specifies that all keys and values will be Binary to
    * start with, and thus will not require conversion.
    */
    private boolean m_fBinaryOnly;
    }
