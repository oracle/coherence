/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;

import java.util.Collection;
import java.util.Map;


/**
* A simple extension of the WrapperNamedCache implementation that shields all
* content mutating operations such as put(), remove(), lock() etc.
*
* @param <K>  the type of the cache entry keys
* @param <V>  the type of the cache entry values
*
* @author gg 2006.08.06
*/
public class ReadonlyNamedCache<K, V>
        extends WrapperNamedCache<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a NamedCache wrapper based on the specified map.
    *
    * @param cache    the NamedCache object that will be wrapped by this
    *                 read-only wrapper
    * @param fStrict  if true, calls to mutating operations will throw
    *                 the UnsupportedOperationException; otherwise those calls
    *                 will have no effect whatsoever
    */
    public ReadonlyNamedCache(NamedCache<K, V> cache, boolean fStrict)
        {
        this(cache, cache.getCacheName(), fStrict);
        }

    /**
    * Construct a NamedCache wrapper based on the specified map.
    *
    * @param map      the Map that will be wrapped by this read-only wrapper
    * @param sName    the cache name
    * @param fStrict  if true, calls to mutating operations will throw
    *                 the UnsupportedOperationException; otherwise those calls
    *                 will have no effect whatsoever
    */
    public ReadonlyNamedCache(Map<K, V> map, String sName, boolean fStrict)
        {
        super(map, sName);
        m_fStrict = fStrict;
        }


    // ----- mutating WrapperNamedCache methods -----------------------------

    /**
    * Should not be called.
    */
    public void destroy()
        {
        checkStrict();
        }

    /**
     * Should not be called.
     */
    public void truncate()
        {
        checkStrict();
        }

    /**
    * Should not be called.
    */
    public <R> R invoke(K key, EntryProcessor<K, V, R> agent)
        {
        checkStrict();
        return null;
        }

    /**
    * Should not be called.
    */
    public <R> Map<K, R> invokeAll(Collection<? extends K> collKeys, EntryProcessor<K, V, R> agent)
        {
        checkStrict();
        return null;
        }

    /**
    * Should not be called.
    */
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V, R> agent)
        {
        checkStrict();
        return null;
        }

    /**
    * Should not be called.
    */
    public V put(K oKey, V oValue)
        {
        checkStrict();
        return null;
        }

    /**
    * Should not be called.
    */
    public void putAll(Map<? extends K, ? extends V> map)
        {
        checkStrict();
        }

    /**
    * Should not be called.
    */
    public V remove(Object oKey)
        {
        checkStrict();
        return null;
        }

    /**
    * Should not be called.
    */
    public void clear()
        {
        checkStrict();
        }

    /**
    * Should not be called.
    */
    public boolean lock(Object oKey, long cWait)
        {
        checkStrict();
        return false;
        }

    /**
    * Should not be called.
    */
    public boolean lock(Object oKey)
        {
        checkStrict();
        return false;
        }

    /**
    * Should not be called.
    */
    public boolean unlock(Object oKey)
        {
        checkStrict();
        return false;
        }

    /**
    * Should not be called.
    */
    protected boolean removeBlind(Object oKey)
        {
        checkStrict();
        return false;
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Enforce the "strict" read-only policy.
    */
    protected void checkStrict()
        {
        if (m_fStrict)
            {
            throw new UnsupportedOperationException();
            }
        }


    // ----- data fields ----------------------------------------------------

    /**
    * Specifies whether or not the "read-only" nature of this NamedCache
    * is strictly enforced.
    */
    protected boolean m_fStrict;
    }