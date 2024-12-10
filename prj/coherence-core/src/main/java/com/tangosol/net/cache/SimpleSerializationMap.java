/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.common.collections.AbstractStableIterator;

import com.tangosol.io.BinaryStore;
import com.tangosol.io.BinaryStore.KeySetAware;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.util.Iterator;
import java.util.Map;


/**
* Map implementation that stores and accesses its contents through an
* underlying BinaryStore. The Map does not maintain additional state, such
* as the keys that it contains, which allows it to manage very large sets
* of data. However, a number of operations that would normally be "free" are
* potentially very expensive with this implementation. For example,
* {@link #size} has to iterate through all the keys provided by the
* underlying BinaryStore, and {@link #containsKey} has to read the value
* from the underlying BinaryStore to prove its existence.
*
* @since Coherence 3.1
* @author cp  2005.11.23
*/
public class SimpleSerializationMap
        extends AbstractKeyBasedMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SimpleSerializationMap on top of a BinaryStore.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    */
    public SimpleSerializationMap(BinaryStore store)
        {
        setBinaryStore(store);
        }

    /**
    * Construct a SimpleSerializationMap on top of a BinaryStore, using the
    * passed ClassLoader for deserialization.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    * @param loader the ClassLoader to use for deserialization
    */
    public SimpleSerializationMap(BinaryStore store, ClassLoader loader)
        {
        // configure the serialization map, setting up the loader first so
        // it can be used when the BinaryStore is configured
        setClassLoader(loader);
        setBinaryStore(store);
        }

    /**
    * Construct a SimpleSerializationMap on top of a BinaryStore, optionally
    * storing only Binary keys and values.
    *
    * @param store       the BinaryStore to use to write the serialized
    *                    objects to
    * @param fBinaryMap  true indicates that this map will only manage
    *                    binary keys and values
    */
    public SimpleSerializationMap(BinaryStore store, boolean fBinaryMap)
        {
        setBinaryMap(fBinaryMap);
        setBinaryStore(store);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void clear()
        {
        BinaryStore store = getBinaryStore();
        try
            {
            store.eraseAll();
            }
        catch (UnsupportedOperationException e)
            {
            for (Iterator iter = store.keys(); iter.hasNext(); )
                {
                store.erase((Binary) iter.next());
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsKey(Object oKey)
        {
        BinaryStore binaryStore = getBinaryStore();

        return binaryStore instanceof KeySetAware
               ? ((KeySetAware) binaryStore).containsKey(toBinary(oKey))
               : binaryStore.load(toBinary(oKey)) != null;
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsValue(Object oValue)
        {
        BinaryStore store    = getBinaryStore();
        Binary      binValue = toBinary(oValue);
        for (Iterator iter = store.keys(); iter.hasNext(); )
            {
            if (equals(binValue, store.load((Binary) iter.next())))
                {
                return true;
                }
            }
        return false;
        }

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        Object  oValue   = null;
        long    ldtStart = getSafeTimeMillis();
        Binary  binValue = getBinaryStore().load(toBinary(oKey));
        if (binValue == null)
            {
            m_stats.registerMiss(ldtStart);
            }
        else
            {
            oValue = fromBinary(binValue);
            m_stats.registerHit(ldtStart);
            }
        return oValue;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isEmpty()
        {
        BinaryStore store = getBinaryStore();
        return store instanceof BinaryStore.SizeAware
                ? ((BinaryStore.SizeAware) store).size() == 0
                : !store.keys().hasNext();
        }

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue)
        {
        Object      oOrig    = null;
        long        ldtStart = getSafeTimeMillis();
        BinaryStore store    = getBinaryStore();
        Binary      binKey   = toBinary(oKey);
        Binary      binOrig  = store.load(binKey);
        Binary      binValue = toBinary(oValue);
        if (equals(binValue, binOrig))
            {
            oOrig = oValue;
            }
        else
            {
            store.store(binKey, binValue);

            if (binOrig != null)
                {
                oOrig = fromBinary(binOrig);
                }
            }

        // update statistics
        m_stats.registerPut(ldtStart);

        return oOrig;
        }

    /**
    * {@inheritDoc}
    */
    public void putAll(Map map)
        {
        long        ldtStart = getSafeTimeMillis();
        BinaryStore store    = getBinaryStore();
        int         cPuts    = 0;
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
            {
            Entry  entry  = (Entry) iter.next();
            store.store(toBinary(entry.getKey()), toBinary(entry.getValue()));
            ++cPuts;
            }

        // update statistics
        m_stats.registerPuts(cPuts, ldtStart);
        }

    /**
    * {@inheritDoc}
    */
    public Object remove(Object oKey)
        {
        Object      oOrig   = null;
        BinaryStore store   = getBinaryStore();
        Binary      binKey  = toBinary(oKey);
        Binary      binOrig = store.load(binKey);
        if (binOrig != null)
            {
            store.erase(binKey);
            oOrig = fromBinary(binOrig);
            }
        return oOrig;
        }

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        BinaryStore store = getBinaryStore();
        if (store instanceof BinaryStore.SizeAware)
            {
            return ((BinaryStore.SizeAware) store).size();
            }

        // brute force: count the keys
        int c = 0;
        for (Iterator iter = store.keys(); iter.hasNext(); )
            {
            iter.next();
            ++c;
            }
        return c;
        }


    // ----- AbstractKeyBasedMap methods ------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Iterator iterateKeys()
        {
        return new AbstractStableIterator()
            {
            protected void advance()
                {
                Iterator iter = m_iter;
                if (iter.hasNext())
                    {
                    Binary bin = (Binary) m_iter.next();
                    m_binPrev = bin;
                    setNext(fromBinary(bin));
                    }
                }

            protected void remove(Object oPrev)
                {
                getBinaryStore().erase(m_binPrev);
                }

            Iterator m_iter = getBinaryStore().keys();
            Binary   m_binPrev;
            };
        }

    /**
    * {@inheritDoc}
    */
    protected boolean removeBlind(Object oKey)
        {
        boolean fRemoved = false;

        // unfortunately if the store is not a KeySetAware, this is not
        // a completely blind implementation, as it has to verify whether
        // or not something is actually being deleted, which requires the
        // original value to be loaded (but not deserialized)
        BinaryStore store  = getBinaryStore();
        Binary      binKey = toBinary(oKey);

        if (store instanceof KeySetAware
                ? ((KeySetAware) store).containsKey(binKey)
                : store.load(binKey) != null)
            {
            store.erase(binKey);
            fRemoved = true;
            }

        return fRemoved;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "SimpleSerializationMap {BinaryStore=" + getBinaryStore()
                + ", CacheStatistics=" + getCacheStatistics() + '}';
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Returns the BinaryStore that this map uses for its storage.
    * <p>
    * Note: This implementation assumes that the BinaryStore is only being
    * modified by this Map instance. If you modify the BinaryStore contents,
    * the behavior of this Map is undefined.
    *
    * @return the BinaryStore
    */
    public BinaryStore getBinaryStore()
        {
        return m_store;
        }

    /**
    * Configures the BinaryStore that this map will use for its storage.
    *
    * @param store  the BinaryStore to use
    */
    protected void setBinaryStore(BinaryStore store)
        {
        m_store = store;
        }

    /**
    * Returns the ClassLoader that this map uses for deserialization, if it
    * has one.
    *
    * @return the ClassLoader that this map uses for deserialization; may be
    *         null
    */
    public ClassLoader getClassLoader()
        {
        return m_loader;
        }

    /**
    * Configure the ClassLoader that this map will use for deserialization.
    *
    * @param loader  the ClassLoader that this map should use for
    *                deserialization
    */
    protected void setClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        }

    /**
    * Determine if the keys and values in this map are known to be all
    * Binary.
    *
    * @return true if all keys and values will be Binary to start with, and
    *         thus will not require conversion
    */
    public boolean isBinaryMap()
        {
        return m_fBinaryMap;
        }

    /**
    * Configure the Map to be aware that all the keys and values in the map
    * are known to be Binary or not.
    *
    * @param fBinary  pass true if all keys and values will be Binary
    */
    protected void setBinaryMap(boolean fBinary)
        {
        m_fBinaryMap = fBinary;
        }

    /**
    * Returns the CacheStatistics for this cache.
    *
    * @return a CacheStatistics object
    */
    public CacheStatistics getCacheStatistics()
        {
        return m_stats;
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
        return isBinaryMap() ? (Binary) o
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
        return isBinaryMap() ? bin
                             : ExternalizableHelper.fromBinary(bin, getClassLoader());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The BinaryStore that this SerializationMap uses to store data.
    */
    private BinaryStore m_store;

    /**
    * The ClassLoader that this SerializationMap uses to deserialize data
    * from the BinaryStore. May be null.
    */
    private ClassLoader m_loader;

    /**
    * An indicator that specifies that all keys and values will be Binary to
    * start with, and thus will not require conversion.
    */
    private boolean m_fBinaryMap;

    /**
    * The CacheStatistics object maintained by this cache.
    */
    private SimpleCacheStatistics m_stats = new SimpleCacheStatistics();
    }
