/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.net.NamedCache;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.Versionable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
* A near cache that fronts a Distributed Cache and uses a Replicated Cache of
* version data to keep the local cache in sync.
*
* @author cp  2002.10.20
* @deprecated as of Coherence 3.2 all the functionality of this class has been
*              superceded by the NearCache implementation
*/
public class VersionedNearCache
        extends NearCache
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a VersionedNearCache.
    *
    * @param mapLocal    local cache to front the distributed cache with
    * @param mapDist     distributed cache of Versionable objects
    * @param mapVersion  replicated version cache
    */
    public VersionedNearCache(Map mapLocal, NamedCache mapDist, NamedCache mapVersion)
        {
        super(mapLocal, mapDist, LISTEN_NONE);

        Base.azzert(mapVersion != null);
        m_mapVersion = mapVersion;
        m_mapVersion.addMapListener(instantiateVersionCacheListener());
        }


    // ----- life-cycle -----------------------------------------------------

    /**
    * Invalidate the CachingMap. If the BackMap implements an ObservableMap
    * calling this method is necessary to remove the BackMap listener.
    */
    public void release()
        {
        super.release();
        m_mapVersion = null;
        }


    // ----- CacheMap interface ---------------------------------------------

    /**
    * Returns the value to which this map maps the specified key.
    *
    * @param oKey  the key object
    *
    * @return the value to which this map maps the specified key,
    *         or null if the map contains no mapping for this key
    */
    public Object get(Object oKey)
        {
        Object oValue = super.get(oKey);

        // make sure version cache is up to date
        updateVersion(oKey, oValue);

        return oValue;
        }

    /**
    * Get all the specified keys, if they are in the cache. For each key
    * that is in the cache, that key and its corresponding value will be
    * placed in the map that is returned by this method. The absence of
    * a key in the returned map indicates that it was not in the cache,
    * which may imply (for caches that can load behind the scenes) that
    * the requested data could not be loaded.
    * <p>
    * <b>Note:</b> this implementation does not differentiate between
    * missing keys or null values stored in the back map; in both cases
    * the returned map will not contain the corresponding entry.
    *
    * @param colKeys  a collection of keys that may be in the named cache
    *
    * @return a Map of keys to values for the specified keys passed in
    *         <tt>col</tt>
    * @since Coherence 2.5
    */
    public Map getAll(Collection colKeys)
        {
        Map map = super.getAll(colKeys);
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            updateVersion(entry.getKey(), entry.getValue());
            }
        return map;
        }

    /**
    * Removes the mapping for this key from this map if present.
    * Expensive: updates both the underlying cache and the local cache.
    *
    * @param oKey key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *	       if there was no mapping for key.  A <tt>null</tt> return can
    *	       also indicate that the map previously associated <tt>null</tt>
    *	       with the specified key, if the implementation supports
    *	       <tt>null</tt> values.
    */
    public Object remove(Object oKey)
        {
        getVersionCache().remove(oKey);
        return super.remove(oKey);
        }

    /**
    * Implementation of put method that optionally skips the return value
    * retrieval and allows to specify an expiry for the cache entry.
    *
    * @param oKey     the key
    * @param oValue   the value
    * @param fReturn  if true, the return value is required; otherwise
    *                 the return value will be ignored
    * @param cMillis  the number of milliseconds until the cache entry will
    *                 expire
    * @return previous value (if required)
    *
    * @throws UnsupportedOperationException if the requested expiry is a
    *         positive value and either the front map or the back map
    *         implementations do not support the expiration functionality
    *
    * @see CacheMap#put(Object oKey, Object oValue, long cMillis)
    */
    public Object put(Object oKey, Object oValue, boolean fReturn, long cMillis)
        {
        incrementVersion(oKey, oValue);
        return super.put(oKey, oValue, fReturn, cMillis);
        }

    /**
    * Copy all of the mappings from the specified map to this map.
    *
    * @param map  mappings to be stored in this map
    */
    public void putAll(Map map)
        {
        RuntimeException exception = null;
        if (map.size() > 1)
            {
            // compensate for the optimization at NearCache
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
                {
                Map.Entry entry = (Map.Entry) iter.next();
                try
                    {
                    incrementVersion(entry.getKey(), entry.getValue());
                    }
                catch (RuntimeException e)
                    {
                    exception = e;
                    iter.remove();
                    }
                }
            }

        super.putAll(map);

        if (exception != null)
            {
            throw exception;
            }
        }

    /**
    * Update value's version in the version cache.
    *
    * @param oKey    the key
    * @param oValue  the value
    */
    protected void updateVersion(Object oKey, Object oValue)
        {
        if (oValue != null)
            {
            Map        map    = getVersionCache();
            Comparable verOld = (Comparable) map.get(oKey);
            Comparable verNew = ((Versionable) oValue).getVersionIndicator();
            if (verOld == null || verNew.compareTo(verOld) > 0)
                {
                try
                    {
                    map.put(oKey, verNew);
                    }
                catch (Exception e)
                    {
                    Base.err(e);
                    }
                }
            }
        }

    /**
    * Increment value's version.
    *
    * @param oKey    the key
    * @param oValue  the value
    */
    protected void incrementVersion(Object oKey, Object oValue)
        {
        if (oValue instanceof Versionable)
            {
            Versionable oLocalValue = (Versionable) oValue;
            oLocalValue.incrementVersion();
            getVersionCache().put(oKey, oLocalValue.getVersionIndicator());
            }
        else
            {
            throw new IllegalArgumentException(
                "Value must implement Versionable interface: " + oValue);
            }
        }


    // ----- ConcurrentMap interface ----------------------------------------

    /**
    * Attempt to lock the specified item within the specified period of time.
    *
    * Expensive: Locking always occurs on the back cache, and removes the
    * value from the front cache if successful (forcing a get() from the back
    * cache on next access to the key).
    *
    * @param oKey     key being locked
    * @param lMillis  the number of milliseconds to continue trying to obtain
    *                 a lock; pass zero to return immediately; pass -1 to block
    *                 the calling thread until the lock could be obtained
    *
    * @return true if the item was successfully locked within the
    *              specified time; false otherwise
    */
    public boolean lock(Object oKey, long lMillis)
        {
        NamedCache mapVersion = getVersionCache();
        if (mapVersion.lock(oKey, lMillis))
            {
            Map         mapLocal    = getFrontMap();
            Versionable oLocalValue = (Versionable) mapLocal.get(oKey);
            if (oLocalValue != null)
                {
                Comparable verCurrent = (Comparable) mapVersion.get(oKey);
                if (verCurrent == null)
                    {
                    // if version is missing from the version cache, then
                    // the local cache should not contain the data
                    mapLocal.remove(oKey);
                    }
                else
                    {
                    Comparable verLocal = oLocalValue.getVersionIndicator();
                    if (verLocal.compareTo(verCurrent) < 0)
                        {
                        // local version is out of date
                        mapLocal.remove(oKey);
                        }
                    }
                }

            return true;
            }
        else
            {
            return false;
            }
        }

    /**
    * Unlock the specified item.
    *
    * @param oKey key being unlocked
    *
    * @return true if the item was successfully unlocked; false otherwise
    */
    public boolean unlock(Object oKey)
        {
        return getVersionCache().unlock(oKey);
        }


    // ----- VersionCacheListener -------------------------------------------

    /**
    * Factory method: Provide a MapListener that listens to the VersionCache
    * and invalidates local entries accordingly.
    *
    * @return a MapListener object to listen to the VersionCache
    */
    protected MapListener instantiateVersionCacheListener()
        {
        return new VersionCacheListener();
        }

    /**
    * A map listener that listens to the VersionCache and invalidates local
    * entries accordingly.
    *
    * @author cp 2002.10.20
    */
    public class VersionCacheListener
            extends AbstractMapListener
        {
        /**
        * {@inheritDoc}
        */
        public void entryUpdated(MapEvent evt)
            {
            Object oKey     = evt.getKey();
            Map    mapLocal = VersionedNearCache.this.getFrontMap();

            synchronized (mapLocal)
                {
                Versionable oLocalValue = (Versionable) mapLocal.get(oKey);
                if (oLocalValue != null)
                    {
                    Comparable verLocal   = oLocalValue.getVersionIndicator();
                    Comparable verCurrent = (Comparable) evt.getNewValue();
                    if (verLocal.compareTo(verCurrent) < 0)
                        {
                        // local version is out of date
                        mapLocal.remove(oKey);
                        }
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public void entryDeleted(MapEvent evt)
            {
            Object oKey     = evt.getKey();
            Map    mapLocal = VersionedNearCache.this.getFrontMap();

            synchronized (mapLocal)
                {
                mapLocal.remove(oKey);
                }
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the NamedCache object that holds the version data.
    *
    * @return the NamedCache object, which holds just keys and version data
    */
    public NamedCache getVersionCache()
        {
        return m_mapVersion;
        }


    // ----- data members ---------------------------------------------------

    /**
    * This is the NamedCache that holds the current known "transient" version
    * that indicates the only acceptable version of each cached object that a
    * near cache should hold onto.
    */
    private NamedCache m_mapVersion;
    }
