/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.GuardSupport;

import com.tangosol.net.cache.CacheStore;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;


/**
* {@link CacheStore} implementation for testing {@link ReadWriteBackingMap}
* functionality.
* <p>
* The TestCacheStore stores all items in an {@link ObservableMap} that can be
* retrieved using the {@link #getStorageMap} method.
*/
public class TestCacheStore
        extends AbstractTestStore
        implements CacheStore
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public TestCacheStore()
        {
        this(new ObservableHashMap());
        }

    /**
    * Create a new TestCacheStore that will use the NamedCache with the
    * specified name to store items.
    *
    * @param sName  the name of the NamedCache to use for storage
    */
    public TestCacheStore(String sName)
        {
        this(CacheFactory.getCache(sName));
        }

    /**
    * Create a new TestCacheStore that will use the specified ObservableMap to
    * store items.
    *
    * @param mapStorage  the ObservableMap used for the underlying storage
    */
    public TestCacheStore(ObservableMap mapStorage)
        {
        super(mapStorage);
        }


    // ----- CacheStore interface -------------------------------------------

    /**
    * Return the value associated with the specified key, or null if the key
    * does not have an associated value in the underlying store.
    *
    * @param oKey key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or <tt>null</tt> if
    *         no value is available for that key
    */
    public Object load(Object oKey)
        {
        log(isVerboseLoad(), "load(" + oKey + ")");
        logMethodInvocation("load");

        delay(getDurationLoad());

        checkForFailure(getFailureKeyLoad(), oKey);
        return getStorageMap().get(oKey);
        }

    /**
    * Return the values associated with each the specified keys in the passed
    * collection. If a key does not have an associated value in the underlying
    * store, then the return map will not have an entry for that key.
    *
    * @param colKeys a collection of keys to load
    *
    * @return a Map of keys to associated values for the specified keys
    */
    public Map loadAll(Collection colKeys)
        {
        log(isVerboseLoadAll(), "loadAll(" + colKeys + ")");
        logMethodInvocation("loadAll");

        delay(getDurationLoadAll());

        if (Thread.currentThread().getName().startsWith("ReadThread:"))
            {
            Assert.assertNotNull(GuardSupport.getThreadContext());
            }

        Map map = new HashMap();
        for (Iterator iterKeys = colKeys.iterator(); iterKeys.hasNext(); )
            {
            Object oKey = iterKeys.next();
            Object oValue;

            checkForFailure(getFailureKeyLoadAll(), oKey);
            oValue = getStorageMap().get(oKey);

            if (oValue != null)
                {
                map.put(oKey, oValue);
                }
            }

        return map;
        }

    /**
    * Store the specified value under the specified key in the underlying
    * store. This method is intended to support both key/value creation and
    * value update for a specific key.
    *
    * @param oKey   key to store the value under
    * @param oValue value to be stored
    *
    * @throws UnsupportedOperationException if this implementation or the
    *                                       underlying store is read-only
    */
    public void store(Object oKey, Object oValue)
        {
        log(isVerboseStore(), "store(" + oKey + ", " + oValue + ")");
        logMethodInvocation("store");

        delay(getDurationStore());

        checkForFailure(getFailureKeyStore(), oKey);
        getStorageMap().put(oKey, oValue);
        }

    /**
    * Store the specified values under the specified keys in the underlying
    * store. This method is intended to support both key/value creation and
    * value update for the specified keys.
    *
    * @param mapEntries a Map of any number of keys and values to store
    *
    * @throws UnsupportedOperationException if this implementation or the
    *                                       underlying store is read-only
    */
    public void storeAll(Map mapEntries)
        {
        log(isVerboseStoreAll(), "storeAll(" + mapEntries + ")");
        logMethodInvocation("storeAll");

        delay(getDurationStoreAll());

        Map     mapStorage = getStorageMap();
        boolean fRemove    = true;

        for (Iterator iterEntries = mapEntries.entrySet().iterator();
             iterEntries.hasNext(); )
            {
            Map.Entry entry  = (Map.Entry) iterEntries.next();
            Object    oKey   = entry.getKey();
            Object    oValue = entry.getValue();

            checkForFailure(getFailureKeyStoreAll(), oKey);
            mapStorage.put(oKey, oValue);

            if (fRemove)
                {
                try
                    {
                    iterEntries.remove();
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
    * @param oKey key whose mapping is being removed from the cache
    *
    * @throws UnsupportedOperationException if this implementation or the
    *                                       underlying store is read-only
    */
    public void erase(Object oKey)
        {
        log(isVerboseErase(), "erase(" + oKey + ")");
        logMethodInvocation("erase");

        delay(getDurationErase());

        checkForFailure(getFailureKeyErase(), oKey);
        getStorageMap().remove(oKey);
        }

    /**
    * Remove the specified keys from the underlying store if present.
    *
    * @param colKeys keys whose mappings are being removed from the cache
    *
    * @throws UnsupportedOperationException if this implementation or the
    *                                       underlying store is read-only
    */
    public void eraseAll(Collection colKeys)
        {
        log(isVerboseEraseAll(), "eraseAll(" + colKeys + ")");
        logMethodInvocation("eraseAll");

        delay(getDurationEraseAll());

        Map     mapStorage = getStorageMap();
        boolean fRemove    = true;

        for (Iterator iterKeys = colKeys.iterator(); iterKeys.hasNext(); )
            {
            Object oKey = iterKeys.next();

            checkForFailure(getFailureKeyEraseAll(), oKey);
            mapStorage.remove(oKey);

            if (fRemove)
                {
                try
                    {
                    iterKeys.remove();
                    }
                catch (UnsupportedOperationException e)
                    {
                    fRemove = false;
                    }
                }
            }
        }
    }
