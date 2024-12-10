/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.io.BinaryStore;

import com.tangosol.util.AbstractKeySetBasedMap;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.WrapperCollections;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* Map implementation that stores its contents in a BinaryStore.
* <p>
* This implementation is mostly thread safe. To be certain, it is suggested
* that access to this cache is either single-threaded or gated through an
* object like WrapperConcurrentMap.
*
* @since Coherence 2.2
* @author cp  2003.05.26
* @author cp  2005.11.25 updating for 3.1 using AbstractKeySetBasedMap
*/
public class SerializationMap
        extends AbstractKeySetBasedMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SerializationMap on top of a BinaryStore.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    */
    public SerializationMap(BinaryStore store)
        {
        m_mapKeys = instantiateKeyMap();
        setBinaryStore(store);
        }

    /**
    * Construct a SerializationMap on top of a BinaryStore, using the passed
    * ClassLoader for deserialization.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    * @param loader the ClassLoader to use for deserialization
    */
    public SerializationMap(BinaryStore store, ClassLoader loader)
        {
        // configure the serialization map, setting up the loader first so
        // it can be used when the BinaryStore is configured
        setClassLoader(loader);
        m_mapKeys = instantiateKeyMap();
        setBinaryStore(store);
        }

    /**
    * Construct a SerializationMap on top of a BinaryStore, optionally
    * storing only Binary keys and values.
    *
    * @param store       the BinaryStore to use to write the serialized
    *                    objects to
    * @param fBinaryMap  true indicates that this map will only manage
    *                    binary keys and values
    * @since Coherence 2.4
    */
    public SerializationMap(BinaryStore store, boolean fBinaryMap)
        {
        setBinaryMap(fBinaryMap);
        m_mapKeys = instantiateKeyMap();
        setBinaryStore(store);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void clear()
        {
        try
            {
            eraseStore();
            }
        finally
            {
            getKeyMap().clear();
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsValue(Object oValue)
        {
        BinaryStore store    = getBinaryStore();
        Binary      binValue = toBinary(oValue);
        for (Iterator iter = keySet().iterator(); iter.hasNext(); )
            {
            if (equals(binValue, store.load(toBinary(iter.next()))))
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
        Object  oValue    = null;
        long    ldtStart  = getSafeTimeMillis();
        boolean fContains = false;
        if (getKeyMap().containsKey(oKey))
            {
            Binary binValue = getBinaryStore().load(toBinary(oKey));
            if (binValue != null)
                {
                oValue    = fromBinary(binValue);
                fContains = true;
                }
            }

        // update statistics
        if (fContains)
            {
            m_stats.registerHit(ldtStart);
            }
        else
            {
            m_stats.registerMiss(ldtStart);
            }

        return oValue;
        }

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue)
        {
        Object      oOrig     = null;
        long        ldtStart  = getSafeTimeMillis();
        Binary      binKey    = toBinary(oKey);
        Binary      binValue  = toBinary(oValue);
        BinaryStore store     = getBinaryStore();
        Binary      binOrig   = null;
        boolean     fContains = getKeyMap().containsKey(oKey);

        // load the original value
        if (fContains)
            {
            binOrig = store.load(binKey);
            }

        // check for no change (avoids the write)
        if (equals(binValue, binOrig))
            {
            oOrig = oValue;
            }
        else
            {
            // store the new value
            store.store(binKey, binValue);

            // deserialize the old value
            if (binOrig != null)
                {
                oOrig = fromBinary(binOrig);
                }
            }

        // register the update
        registerKey(oKey, binKey, binValue);

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
            Entry  entry    = (Entry) iter.next();
            Object oKey     = entry.getKey();
            Binary binKey   = toBinary(oKey);
            Binary binValue = toBinary(entry.getValue());
            store.store(binKey, binValue);
            registerKey(oKey, binKey, binValue);
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
        Object oOrig = null;
        if (getKeyMap().containsKey(oKey))
            {
            BinaryStore store   = getBinaryStore();
            Binary      binKey  = toBinary(oKey);
            Binary      binOrig = store.load(binKey);
            if (binOrig != null)
                {
                store.erase(binKey);
                oOrig = fromBinary(binOrig);
                }
            unregisterKey(oKey);
            }

        return oOrig;
        }


    // ----- AbstractKeySetBasedMap methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    protected Set getInternalKeySet()
        {
        return getKeyMap().keySet();
        }

    /**
    * {@inheritDoc}
    */
    protected boolean removeBlind(Object oKey)
        {
        boolean fRemoved = false;
        if (getKeyMap().containsKey(oKey))
            {
            getBinaryStore().erase(toBinary(oKey));
            unregisterKey(oKey);
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
        return "SerializationMap {" + getDescription() + "}";
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
        BinaryStore storeOrig = m_store;
        if (store != storeOrig)
            {
            m_store = store;

            // initialize the serialization map with whatever keys are already in
            // the store (typically none, but just in case ..)
            getKeyMap().clear();
            if (store != null)
                {
                Iterator iter;
                try
                    {
                    iter = store.keys();
                    }
                catch (UnsupportedOperationException e)
                    {
                    iter = NullImplementation.getIterator();
                    }

                while (iter.hasNext())
                    {
                    Binary binKey = (Binary) iter.next();
                    registerKey(fromBinary(binKey), binKey, null);
                    }
                }
            }
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
    * @since Coherence 2.4
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
    * Returns the cache of keys that are in the SerializationMap.
    *
    * @return the cache of keys that are in the SerializationMap
    */
    protected Map getKeyMap()
        {
        return m_mapKeys;
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

    /**
    * Assemble a human-readable description.
    *
    * @return a description of this Map
    */
    protected String getDescription()
        {
        return "BinaryStore=" + getBinaryStore()
                + ", size=" + size()
                + ", CacheStatistics=" + getCacheStatistics();
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Register a new key for the SerializationMap. This method maintains the
    * internal key Set for the SerializationMap.
    *
    * @param oKey      the key that has been added to the map
    * @param binKey    the binary form of the key
    * @param binValue  the binary form of the value
    */
    protected synchronized void registerKey(Object oKey, Binary binKey, Binary binValue)
        {
        getKeyMap().put(oKey, EXISTS);
        }

    /**
    * Unregister a key from the SerializationMap. This method maintains the
    * internal key Set for the SerializationMap.
    *
    * @param oKey  the key that has been removed from the map
    */
    protected synchronized void unregisterKey(Object oKey)
        {
        getKeyMap().remove(oKey);
        }

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

    /**
    * Erase all entries from the underlying store.
    */
    protected void eraseStore()
        {
        BinaryStore store = getBinaryStore();

        // try bulk erase
        try
            {
            store.eraseAll();
            return;
            }
        catch (UnsupportedOperationException e) {}

        // iterate keys and erase each
        try
            {
            for (Iterator iter = store.keys(); iter.hasNext(); )
                {
                store.erase((Binary) iter.next());
                }
            return;
            }
        catch (UnsupportedOperationException e) {}

        // iterate object keys and erase the persistent version of each
        for (Iterator iter = getKeyMap().keySet().iterator(); iter.hasNext(); )
            {
            store.erase(toBinary(iter.next()));
            }
        }

    /**
    * Instantiate a key-map.
    *
    * @return a Map to hold the keys managed by this SerializationMap
    */
    protected Map instantiateKeyMap()
        {
        return new SafeHashMap();
        }


    // ----- constants ------------------------------------------------------

    /**
    * A constant Long instance representing the existence of a value.
    */
    private static final Long EXISTS = Long.valueOf(1L);


    // ----- data fields ----------------------------------------------------

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
    * Map that caches the collection of keys managed by this Map.
    */
    private final Map m_mapKeys;

    /**
    * The CacheStatistics object maintained by this cache.
    */
    private final SimpleCacheStatistics m_stats = new SimpleCacheStatistics();
    }
