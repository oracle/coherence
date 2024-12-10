/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.net.NamedCache;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SegmentedConcurrentMap;

import com.tangosol.util.filter.CacheEventFilter;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.NotFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.atomic.AtomicBoolean;


/**
* Map implementation that wraps two maps - a front map (assumed to be
* "inexpensive" and probably "incomplete") and a back map (assumed to
* be "complete" and "correct", but more "expensive") - using a
* read-through/write-through approach.
* <p>
* If the back map implements ObservableMap interface, the CachingMap provides
* four different strategies of invalidating the front map entries that have
* changed by other processes in the back map:
* <ul>
* <li>LISTEN_NONE strategy instructs the cache not to listen for invalidation
*     events at all. This is the best choice for raw performance and
*     scalability when business requirements permit the use of data which
*     might not be absolutely current.  Freshness of data can be guaranteed
*     by use of a sufficiently brief eviction policy for the front map;
* <li>LISTEN_PRESENT strategy instructs the CachingMap to listen to the
*     back map events related <b>only</b> to the items currently present in
*     the front map. This strategy works best when each instance of a front
*     map contains distinct subset of data relative to the other front map
*     instances (e.g. sticky data access patterns);
* <li>LISTEN_ALL strategy instructs the CachingMap to listen to <b>all</b>
*     back map events. This strategy is optimal for read-heavy tiered access
*     patterns where there is significant overlap between the different
*     instances of front maps;
* <li>LISTEN_AUTO strategy instructs the CachingMap implementation to switch
*     automatically between LISTEN_PRESENT and LISTEN_ALL strategies based
*     on the cache statistics;
* <li>LISTEN_LOGICAL strategy instructs the CachingMap to listen to <b>all</b>
*     back map events that are <b>not synthetic deletes</b>. A synthetic event
*     could be emitted as a result of eviction or expiration. With this
*     invalidation strategy, it is possible for the front map to contain cache
*     entries that have been synthetically removed from the back (though any
*     subsequent re-insertion will cause the corresponding entries in the front
*     map to be invalidated).
* </ul>
* <p>
* The front map implementation is assumed to be thread safe; additionally
* any modifications to the front map are allowed only after the corresponding
* lock is acquired against the {@link #getControlMap() ControlMap}.
* <p>
* <b>Note:</b> null values are not cached in the front map and therefore this
* implementation is not optimized for maps that allow null values to be
* stored.
*
* @author ag/gg 2002.09.10
* @author gg 2003.10.16
*/
public class CachingMap<K, V>
        implements Map<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a CachingMap using two specified maps:
    * <ul>
    * <li> <i>FrontMap</i> (aka "cache", "near" or "shallow") and
    * <li> <i>BackMap</i>  (aka "actual", "real" or "deep").
    * </ul>
    * If the BackMap implements the ObservableMap interface a listener will
    * be added to the BackMap to invalidate FrontMap items updated
    * [externally] in the back map using the {@link #LISTEN_AUTO} strategy.
    *
    * @param mapBack   back map
    * @param mapFront  front map
    *
    * @see SeppukuMapListener
    */
    public CachingMap(Map<K, V> mapFront, Map<K, V> mapBack)
        {
        this(mapFront, mapBack, LISTEN_AUTO);
        }

    /**
    * Construct a CachingMap using two specified maps:
    * <ul>
    * <li> <i>FrontMap</i> (aka "cache", "near" or "shallow") and
    * <li> <i>BackMap</i>  (aka "actual", "real" or "deep")
    * </ul>
    * and using the specified front map invalidation strategy.
    *
    * @param mapFront   front map
    * @param mapBack    back map
    * @param nStrategy  specifies the strategy used for the front map
    *                   invalidation; valid values are LISTEN_* constants
    */
    public CachingMap(Map<K, V> mapFront, Map<K, V> mapBack, int nStrategy)
        {
        Base.azzert(mapFront != null && mapBack != null, "Null map");
        Base.azzert(LISTEN_NONE <= nStrategy && nStrategy <= LISTEN_LOGICAL, "Invalid strategy value");

        m_mapFront   = mapFront;
        m_mapBack    = mapBack;
        m_mapControl = new SegmentedConcurrentMap();

        if (nStrategy != LISTEN_NONE)
            {
            if (mapBack instanceof ObservableMap)
                {
                m_listener = instantiateBackMapListener(nStrategy);
                if (mapFront instanceof ObservableMap)
                    {
                    m_listenerFront = instantiateFrontMapListener();
                    }
                m_listenerDeactivation = new DeactivationListener();
                }
            else
                {
                nStrategy = LISTEN_NONE;
                }
            }

        m_nStrategyTarget  = nStrategy;
        m_nStrategyCurrent = LISTEN_NONE;
        }


    // ----- life-cycle -----------------------------------------------------

    /**
    * Release the CachingMap. If the BackMap implements an ObservableMap
    * calling this method is necessary to remove the BackMap listener.
    * Any access to the CachingMap which has been released will cause
    * IllegalStateException.
    */
    public void release()
        {
        ConcurrentMap mapControl = getControlMap();
        if (!mapControl.lock(ConcurrentMap.LOCK_ALL, 0))
            {
            // Note: we cannot do a blocking LOCK_ALL as any event which came
            // in while the ThreadGate is in the closing state would cause
            // the service thread to spin.  Unlike clear() there is no
            // benefit in sleeping/retrying here as we know that there are
            // other active threads, thus if we succeeded they would get the
            // IllegalStateException
            throw new IllegalStateException("Cache is in active use by other threads.");
            }

        try
            {
            mapControl.put(GLOBAL_KEY, IGNORE_LIST);
            switch (m_nStrategyCurrent)
                {
                case LISTEN_PRESENT:
                    unregisterFrontListener();
                    unregisterListeners(getFrontMap().keySet());
                    break;

                case LISTEN_LOGICAL:
                case LISTEN_ALL:
                    unregisterListener();
                    break;
                }

            unregisterDeactivationListener();

            m_listener             = null;
            m_mapFront             = null;
            m_mapBack              = null;
            m_filterListener       = null;
            m_listenerDeactivation = null;
            }
        catch (RuntimeException e)
            {
            // one of the following should be ignored:
            //   IllegalStateException("Cache is not active");
            //   RuntimeException("Storage is not configured");
            //   RuntimeException("Service has been terminated");
            }
        finally
            {
            mapControl.remove(GLOBAL_KEY);
            mapControl.unlock(ConcurrentMap.LOCK_ALL);
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the front map reference.
    * <p>
    * <b>Note:</b> direct modifications of the returned map may cause an
    * unpredictable behavior of the CachingMap.
    *
    * @return the front Map
    */
    public Map<K, V> getFrontMap()
        {
        Map<K, V> map = m_mapFront;
        if (map == null)
            {
            throw new IllegalStateException("Cache is not active");
            }
        return map;
        }

    /**
    * Obtain the back map reference.
    * <p>
    * <b>Note:</b> direct modifications of the returned map may cause an
    * unpredictable behavior of the CachingMap.
    *
    * @return the back Map
    */
    public Map<K, V> getBackMap()
        {
        Map<K, V> map = m_mapBack;
        if (map == null)
            {
            throw new IllegalStateException("Cache is not active");
            }
        return map;
        }

    /**
    * Obtain the invalidation strategy used by this CachingMap.
    *
    * @return one of LISTEN_* values
    */
    public int getInvalidationStrategy()
        {
        return m_nStrategyTarget;
        }

    /**
    * Obtain the ConcurrentMap that should be used to synchronize
    * the front map modification access.
    *
    * @return a ConcurrentMap controlling the front map modifications
    */
    public ConcurrentMap getControlMap()
        {
        return m_mapControl;
        }

    /**
    * Determine if changes to the back map affect the front map so that data
    * in the front map stays in sync.
    *
    * @return true if the front map has a means to stay in sync with the back
    *         map so that it does not contain stale data
    */
    protected boolean isCoherent()
        {
        return m_listener != null;
        }

    /**
    * Obtain the CacheStatistics for this cache.
    *
    * @return a CacheStatistics object
    */
    public CacheStatistics getCacheStatistics()
        {
        return m_stats;
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Clears both the front and back maps.
    */
    @Override
    public void clear()
        {
        ConcurrentMap mapControl = getControlMap();

        // Note: we cannot do a blocking LOCK_ALL as any event which came
        // in while the ThreadGate is in the closing state would cause the
        // service thread to spin.  Try for up ~1s before giving up and
        // issue the operation against the back, allowing events to perform
        // the cleanup.  We don't even risk a timed LOCK_ALL as whatever
        // time value we choose would risk a useless spin for that duration
        for (int i = 0; !mapControl.lock(ConcurrentMap.LOCK_ALL, 0); ++i)
            {
            if (i == 100)
                {
                getBackMap().clear();
                if (m_nStrategyTarget == LISTEN_NONE)
                    {
                    getFrontMap().clear();
                    }
                return;
                }
            try
                {
                Blocking.sleep(10);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }
            }

        try
            {
            mapControl.put(GLOBAL_KEY, IGNORE_LIST);

            Map mapFront = getFrontMap();
            Map mapBack  = getBackMap();

            switch (m_nStrategyCurrent)
                {
                case LISTEN_PRESENT:
                    unregisterFrontListener();
                    try
                        {
                        for (Iterator iter = mapFront.keySet().iterator(); iter.hasNext();)
                            {
                            unregisterListener(iter.next());
                            iter.remove();
                            }
                        }
                    catch (RuntimeException e)
                        {
                        // we're not going to reset the invalidation strategy
                        // so we must keep the front listener around
                        registerFrontListener();
                        throw e;
                        }
                    break;

                case LISTEN_LOGICAL:
                case LISTEN_ALL:
                    unregisterListener();
                    try
                        {
                        mapFront.clear();
                        }
                    catch (RuntimeException e)
                        {
                        // since we don't know what's left there
                        // leave the cache in a coherent state
                        registerListener();
                        throw e;
                        }
                    break;

                default:
                    mapFront.clear();
                    break;
                }
            resetInvalidationStrategy();
            mapBack.clear();
            }
        finally
            {
            mapControl.remove(GLOBAL_KEY);
            mapControl.unlock(ConcurrentMap.LOCK_ALL);
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean containsKey(Object oKey)
        {
        Map mapFront = getFrontMap();
        if (mapFront.containsKey(oKey))
            {
            m_stats.registerHit();
            return true;
            }

        ConcurrentMap mapControl = getControlMap();
        mapControl.lock(oKey, -1);
        try
            {
            if (mapFront.containsKey(oKey))
                {
                m_stats.registerHit();
                return true;
                }

            mapControl.put(oKey, IGNORE_LIST);
            m_stats.registerMiss();
            return getBackMap().containsKey(oKey);
            }
        finally
            {
            mapControl.remove(oKey);
            mapControl.unlock(oKey);
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean containsValue(Object oValue)
        {
        return getFrontMap().containsValue(oValue)
            || getBackMap().containsValue(oValue);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Set<Map.Entry<K, V>> entrySet()
        {
        Set<Map.Entry<K, V>> set = getBackMap().entrySet();
        if (!isCoherent())
            {
            set = Collections.unmodifiableSet(set);
            }
        return set;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public V get(Object oKey)
        {
        Map<K, V> mapFront = getFrontMap();
        V value = mapFront.get(oKey);
        if (value != null)
            {
            m_stats.registerHit(); // avoid calculating time for hit
            return value;
            }

        long          ldtStart   = Base.getSafeTimeMillis();
        ConcurrentMap mapControl = getControlMap();
        mapControl.lock(oKey, -1);
        try
            {
            value = mapFront.get(oKey);
            if (value != null)
                {
                m_stats.registerHit(ldtStart);
                return value;
                }

            Map<K, V> mapBack = getBackMap();

            if (m_nStrategyTarget == LISTEN_NONE)
                {
                value = mapBack.get(oKey);
                if (value != null)
                    {
                    mapFront.put((K) oKey, value);
                    }
                }
            else
                {
                List listEvents = new LinkedList();
                mapControl.put(oKey, listEvents);

                registerListener(oKey);

                boolean fPrimed;
                synchronized (listEvents)
                    {
                    int c;
                    switch (c = listEvents.size())
                        {
                        case 0:
                            fPrimed = false;
                            break;

                        default:
                            // check if the last event is a "priming" one
                            MapEvent<K, V> evt = (MapEvent) listEvents.get(c-1);

                            if (fPrimed = isPriming(evt))
                                {
                                value = evt.getNewValue();
                                listEvents.remove(c-1);
                                }
                            break;
                        }
                    }

                if (!fPrimed)
                    {
                    // this call could be a network call
                    // generating events on a service thread
                    try
                        {
                        value = mapBack.get(oKey);
                        }
                    catch (RuntimeException e)
                        {
                        unregisterListener(oKey);
                        mapControl.remove(oKey);
                        throw e;
                        }
                    }

                synchronized (listEvents)
                    {
                    if (value == null)
                        {
                        // we don't cache null values
                        unregisterListener(oKey);
                        }
                    else
                        {
                        // get operation itself can generate only
                        // a synthetic INSERT; anything else should be
                        // considered as an invalidating event
                        boolean fValid = true;
                        switch (listEvents.size())
                            {
                            case 0:
                                break;

                            case 1:
                                // it's theoretically possible (though very
                                // unlikely) that another thread caused the
                                // entry expiration, reload and the synthetic
                                // insert all while this request had already
                                // been supplied with a value;
                                // we'll take our chance here to provide
                                // greater effectiveness for the more
                                // probable situation
                                MapEvent evt = (MapEvent) listEvents.get(0);
                                fValid =
                                    evt.getId() == MapEvent.ENTRY_INSERTED &&
                                    evt instanceof CacheEvent &&
                                    ((CacheEvent) evt).isSynthetic();
                                break;

                            default:
                                fValid = false;
                                break;
                            }

                        if (fValid)
                            {
                            // Adding to the front cache could cause a large number
                            // of evictions. Instead of unregistering the listeners
                            // individually, try to collect them for a bulk unregistration.
                            Set<K> setUnregister = setKeyHolder();
                            try
                                {
                                mapFront.put((K) oKey, value);
                                }
                            finally
                                {
                                if (setUnregister != null)
                                    {
                                    try
                                        {
                                        unregisterListeners(setUnregister);
                                        }
                                    catch (UnsupportedOperationException e)
                                        {
                                        // ignore.  can only happen if back map truncated during this call.
                                        }
                                    finally
                                        {
                                        removeKeyHolder();
                                        }
                                    }
                                }
                            }
                        else
                            {
                            unregisterListener(oKey);
                            m_cInvalidationHits++;
                            }
                        }
                    // remove must occur under sync (if we're caching) otherwise we risk losing events
                    mapControl.remove(oKey);
                    }
                }

            // update miss statistics
            m_stats.registerMiss(ldtStart);
            return value;
            }
        finally
            {
            mapControl.unlock(oKey);
            }
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
    public Map<K, V> getAll(Collection<? extends K> colKeys)
        {
        long ldtStart = Base.getSafeTimeMillis();

        // Step 1: retrieve all we can from the front map first
        Map<K, V> mapResult = getAllFromFrontMap(colKeys);

        if (!mapResult.isEmpty())
            {
            m_stats.registerHits(mapResult.size(), ldtStart);
            }

        if (mapResult.size() == colKeys.size())
            {
            // all keys found in front
            return mapResult;
            }
        else
            {
            // ensure mapResult is modifiable to enable adding missing entries retrieved from backing map
            mapResult = new HashMap<K,V>(mapResult);
            }

        Set<K> setMiss = new HashSet<>(colKeys);
        setMiss.removeAll(mapResult.keySet());

        Map<K, V> mapBack = getBackMap();
        if (mapBack instanceof CacheMap)
            {
            // Step 2: Lock the missing keys without blocking
            Map<K, V>     mapFront   = getFrontMap();
            ConcurrentMap mapControl = getControlMap();
            int           nStrategy  = ensureInvalidationStrategy();
            Set<K>        setLocked  = tryLock(setMiss);
            int           cLocked    = setLocked.size();
            int           cMisses    = setMiss.size();

            try
                {
                List<MapEvent<K, V>> listEvents = new ArrayList<>(cLocked);

                if (nStrategy != LISTEN_NONE)
                    {
                    setLocked.forEach(k -> mapControl.put(k, listEvents));

                    if (nStrategy == LISTEN_PRESENT)
                        {
                        // Step 3: Register listeners and try to get the values
                        // through priming events
                        registerListeners(setLocked);

                        synchronized (listEvents)
                            {
                            for (int i = listEvents.size() - 1; i >= 0; --i)
                                {
                                MapEvent<K, V> evt = listEvents.get(i);

                                if (isPriming(evt))
                                    {
                                    K key = evt.getKey();

                                    mapResult.put(key, evt.getNewValue());
                                    setMiss.remove(key);
                                    listEvents.remove(i);
                                    }
                                }
                            }
                        }
                    }

                // Step 4: do a bulk getAll() for all the front misses
                //         that were not "primed"
                if (!setMiss.isEmpty())
                    {
                    try
                        {
                        // COH-4447: materialize the converted results to avoid
                        //           unnecessary repeated deserialization
                        mapResult.putAll(new HashMap(((CacheMap) mapBack).getAll(setMiss)));
                        }
                    catch (RuntimeException e)
                        {
                        if (nStrategy != LISTEN_NONE)
                            {
                            for (K key : setLocked)
                                {
                                if (nStrategy == LISTEN_PRESENT)
                                    {
                                    unregisterListener(key);
                                    }
                                mapControl.remove(key);
                                }
                            }
                        throw e;
                        }
                    }


                // Step 5: for the locked keys move the retrieved values to the front
                if (nStrategy == LISTEN_NONE)
                    {
                    for (K key : setLocked)
                        {
                        V value = mapResult.get(key);

                        if (value != null)
                            {
                            mapFront.put(key, value);
                            }
                        }
                    }
                else
                    {
                    Set<K> setInvalid = new HashSet<>();
                    Set<K> setAdd     = new HashSet(setLocked);

                    // remove entries invalidated during the getAll() call
                    synchronized (listEvents)
                        {
                        // getAll() operation itself can generate not more
                        // than one synthetic INSERT per key; anything else
                        // should be considered as an invalidating event
                        // (see additional comment at "get" processing)
                        for (MapEvent<K, V> evt : listEvents)
                            {
                            K key = evt.getKey();

                            // always start with removing the key from the
                            // result set, so a second event is always
                            // treated as an invalidation
                            boolean fValid = setAdd.remove(key)
                                    && evt.getId() == MapEvent.ENTRY_INSERTED
                                    && evt instanceof CacheEvent
                                    && ((CacheEvent) evt).isSynthetic();

                            if (!fValid)
                                {
                                setInvalid.add(key);
                                m_cInvalidationHits++;
                                }
                            }

                        // Adding to the front cache could cause a large number
                        // of evictions. Instead of unregistering the listeners
                        // individually, try to collect them for a bulk unregistration.
                        Set<K> setUnregister = setKeyHolder();
                        try
                            {
                            for (K key : setLocked)
                                {
                                V value = mapResult.get(key);
                                if (value != null && !setInvalid.contains(key))
                                    {
                                    mapFront.put(key, value);
                                    }
                                else // null or invalid
                                    {
                                    if (value == null)
                                        {
                                        mapResult.remove(key);
                                        }

                                    mapFront.remove(key);
                                    unregisterListener(key);
                                    }
                                // remove must occur under sync (if we're caching) otherwise we risk losing events
                                mapControl.remove(key);
                                }
                            }
                        finally
                            {
                            if (setUnregister != null)
                                {
                                try
                                    {
                                    unregisterListeners(setUnregister);
                                    }
                                catch (UnsupportedOperationException e)
                                    {
                                    // ignore.  can only happen if back map truncated during this call.
                                    }
                                finally
                                    {
                                    // ensure key holder is removed
                                    removeKeyHolder();
                                    }
                                }
                            }
                        }
                    }

                m_stats.registerMisses(cMisses, ldtStart);
                }
            finally
                {
                for (K key : setLocked)
                    {
                    mapControl.unlock(key);
                    }
                }
            }
        else
            {
            // back Map is not a CacheMap
            for (K key : setMiss)
                {
                V value = get(key);
                if (value != null)
                    {
                    mapResult.put(key, value);
                    }
                }
            }

        return mapResult;
        }

    /**
    * Retrieve entries from the front map.
    *
    * @param colKeys  a collection of keys
    *
    * @return a Map of keys to values for a subset of the passed in keys that
    *         exist in the front map
    */
    protected Map<K, V> getAllFromFrontMap(Collection<? extends K> colKeys)
        {
        Map<K, V> mapFront = getFrontMap();
        if (mapFront instanceof CacheMap)
            {
            return ((CacheMap<K, V>) mapFront).getAll(colKeys);
            }
        else
            {
            Map<K, V> mapResult = new HashMap<>(colKeys.size());
            for (K key : colKeys)
                {
                V value = mapFront.get(key);

                // we don't cache null values in the front
                if (value != null)
                    {
                    mapResult.put(key, value);
                    }
                }
            return mapResult;
            }
        }

    /**
    * Lock the keys in the given set without blocking.
    *
    * @param setKeys  keys to lock in the control map
    *
    * @return Set of keys that were successfully locked
    */
    protected Set<K> tryLock(Set<K> setKeys)
        {
        ConcurrentMap mapControl = getControlMap();
        Set<K>        setLocked  = new HashSet<>(setKeys.size());

        for (K key : setKeys)
            {
            if (mapControl.lock(key, 0L)) // don't block on lock
                {
                setLocked.add(key);
                }
            }
        return setLocked;
        }

    /**
    * Check if the specified event is a "priming" one.
    *
    * @param evt  the event
    *
    * @return {@code true} if the specified event is a "priming" one   
    */
    protected boolean isPriming(MapEvent evt)
        {
        CacheEvent cacheEvent = evt instanceof CacheEvent
                ? (CacheEvent) evt : null;

        return cacheEvent != null                           &&
               cacheEvent.getId() == MapEvent.ENTRY_UPDATED &&
               // for b/wards comparability consider a synthetic event a priming event
                (isCheckPrimingExclusively(cacheEvent.isPriming())
                    ? cacheEvent.isPriming()
                    : cacheEvent.isSynthetic());
        }

    /**
    * Return true if we can rely on the server emitting priming events (based
    * on receiving a at least one priming event from a storage node).
    *
    * @param fPriming  whether the event that instigated this check is a priming
    *                  event
    * @return true if we can rely on the server emitting priming events 
    */
    protected boolean isCheckPrimingExclusively(boolean fPriming)
        {
        boolean fPrimingOnly = f_atomicPrimingOnly.get();

        if (STRICT_PRIMING && !fPrimingOnly && fPriming)
            {
            f_atomicPrimingOnly.set(fPrimingOnly = true);
            }
        return fPrimingOnly;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isEmpty()
        {
        return getBackMap().isEmpty();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Set<K> keySet()
        {
        Set<K> set = getBackMap().keySet();
        if (!isCoherent())
            {
            set = Collections.unmodifiableSet(set);
            }
        return set;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public V put(K oKey, V oValue)
        {
        return put(oKey, oValue, true, 0L);
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
    public V put(K oKey, V oValue, boolean fReturn, long cMillis)
        {
        long      ldtStart         = Base.getSafeTimeMillis();
        Map<K, V> mapFront         = getFrontMap();
        Map<K, V> mapBack          = getBackMap();
        int       nStrategyTarget  = m_nStrategyTarget; // Use of target is intentional
        int       nStrategyCurrent = m_nStrategyCurrent;

        ConcurrentMap mapControl = getControlMap();
        mapControl.lock(oKey, -1);
        try
            {
            List listEvents = null;

            // obtain current front value; if the new value is null then
            // remove from the front map since we will ignore any changes
            V oFront = oValue == null ? mapFront.remove(oKey) :
                           mapFront.get(oKey);

            if (nStrategyTarget != LISTEN_NONE)
                {
                // NOTE: put() will not register any new key-based listeners;
                // per-key registering for new entries would double the
                // number of synchronous network operations; instead we defer
                // the registration until the first get; we are assuming that
                // "get(a), put(a)", or "put(a), put(b)" are more likely
                // sequences then "put(a), get(a)"

                if (oValue == null)
                    {
                    // we won't cache null values, so no need to listen
                    mapControl.put(oKey, listEvents = IGNORE_LIST);
                    if (oFront != null)
                        {
                        // the value was previously in the front, cleanup
                        unregisterListener(oKey);
                        }
                    }
                else if (oFront != null ||
                         nStrategyCurrent == LISTEN_ALL ||
                         nStrategyCurrent == LISTEN_LOGICAL)
                    {
                    // we are already registered for events covering this key

                    // when back map operations returns we may choose to
                    // cache the new [non-null] value into the front map.
                    // This is cheap since we already have a listener (global
                    // or key) registered for this entry
                    mapControl.put(oKey, listEvents = new LinkedList());
                    }
                else
                    {
                    // we are not registered for events covering this key

                    // we will ignore any changes; this allows us to avoid
                    // the cost of registering a listener and/or generating a
                    // questionably useful LinkedList allocation which could
                    // become tenured
                    mapControl.put(oKey, listEvents = IGNORE_LIST);
                    }
                }

            V oOrig;
            try
                {
                // the back map calls could be network calls
                // generating events on a service thread
                if (cMillis > 0 || fReturn)
                    {
                    // normal put with return value
                    oOrig = put(mapBack, oKey, oValue, cMillis);
                    }
                else
                    {
                    // optimize out the return value
                    mapBack.putAll(Collections.singletonMap(oKey, oValue));
                    oOrig = null;
                    }
                }
            catch (RuntimeException e)
                {
                // we don't know the state of the back; cleanup and
                // invalidate this key on the front
                mapControl.remove(oKey);
                try
                    {
                    invalidateFront(oKey);
                    }
                catch (RuntimeException x) {}
                throw e;
                }

            // cleanup, and update the front if possible
            finalizePut(oKey, oValue, listEvents, cMillis);

            m_stats.registerPut(ldtStart);
            return oOrig;
            }
        finally
            {
            mapControl.unlock(oKey);
            }
        }

    /**
    * Extended put implementation that respects the expiration contract.
    */
    static private <K, V> V put(Map<K, V> map, K oKey, V oValue, long cMillis)
        {
        if (map instanceof CacheMap)
            {
            return ((CacheMap<K, V>) map).put(oKey, oValue, cMillis);
            }
        else if (cMillis <= 0)
            {
            return map.put(oKey, oValue);
            }
        else
            {
            throw new UnsupportedOperationException(
                "Class \"" + map.getClass().getName() +
                "\" does not implement CacheMap interface");
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        // optimize for caller doing a single blind put
        if (map.size() == 1)
            {
            Iterator<? extends Entry<? extends K, ? extends V>> iter = map.entrySet().iterator();
            if (iter.hasNext())
                {
                Map.Entry<? extends K, ? extends V> entry = iter.next();
                put(entry.getKey(), entry.getValue(), false, 0L);
                }
            return;
            }

        int           nStrategyTarget  = m_nStrategyTarget;
        int           nStrategyCurrent = m_nStrategyCurrent;
        boolean       fAllRegistered   = nStrategyCurrent == LISTEN_ALL ||
                                         nStrategyCurrent == LISTEN_LOGICAL;
        long          ldtStart         = Base.getSafeTimeMillis();
        ConcurrentMap mapControl       = getControlMap();
        Map<K, V>     mapFront         = getFrontMap();
        Map<K, V>     mapBack          = getBackMap();
        Map<K, V>     mapLocked        = new HashMap<>();
        List          listUnlockable   = null;

        try
            {
            // lock keys where possible
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
                {
                K key   = entry.getKey();
                V value = entry.getValue();

                if (value != null && mapControl.lock(key, 0))
                    {
                    mapLocked.put(key, value);

                    if (nStrategyTarget != LISTEN_NONE)
                        {
                        // we only track keys which have registered listeners
                        // thus avoiding the synchronous network call for
                        // event registration
                        mapControl.put(key,
                            fAllRegistered || mapFront.containsKey(key) ?
                                new LinkedList() : IGNORE_LIST);
                        }
                    }
                else
                    {
                    // for null values or unlockable keys we will just push
                    // the entry to the back, any required cleanup will occur
                    // automatically during event validation or manually for
                    // LISTEN_NONE
                    if (listUnlockable == null)
                        {
                        listUnlockable = new LinkedList();
                        }
                    listUnlockable.add(key);
                    }
                }

            // update the back with all entries
            mapBack.putAll(map);

            // update front with locked keys where possible
            if (nStrategyTarget == LISTEN_NONE)
                {
                // no event based cleanup to do, simply update the front
                mapFront.putAll(mapLocked);
                for (Iterator iter = mapLocked.keySet().iterator();
                     iter.hasNext(); )
                    {
                    mapControl.unlock(iter.next());
                    iter.remove();
                    }
                // unlockable key cleanup in finally
                }
            else
                {
                // conditionally update locked keys based on event results
                for (Iterator<Map.Entry<K, V>> iter = mapLocked.entrySet().iterator();
                     iter.hasNext(); )
                    {
                    Map.Entry<K, V> entry = iter.next();
                    K key = entry.getKey();

                    finalizePut(key, entry.getValue(), (List) mapControl.get(key), 0L);
                    mapControl.unlock(key);
                    iter.remove();
                    }
                }

            m_stats.registerPuts(map.size(), ldtStart);
            }
        finally
            {
            // invalidate and unlock anything which remains locked
            for (Iterator<K> iter = mapLocked.keySet().iterator();
                 iter.hasNext(); )
                {
                K key = iter.next();
                try
                    {
                    invalidateFront(key);
                    }
                catch (RuntimeException x) {}

                mapControl.remove(key);
                mapControl.unlock(key);
                }

            // invalidate unlockable keys as needed
            if (listUnlockable != null && nStrategyTarget == LISTEN_NONE)
                {
                // not using events, do it manually
                mapFront.keySet().removeAll(listUnlockable);
                }
            }
        }

    /**
    * Invalidate the key from the front.  The caller must have the key
    * locked.
    *
    * @param oKey  the key to invalidate
    */
    protected void invalidateFront(Object oKey)
        {
        if (getFrontMap().remove(oKey) == null)
            {
            m_cInvalidationMisses++;
            }
        else
            {
            unregisterListener(oKey);
            m_cInvalidationHits++;
            }
        }

    /**
    * Helper method used by put() and putAll() to perform common maintenance
    * tasks after completing an operation against the back.  This includes
    * removing the keys from the control map, and evaluating if it is safe to
    * update the front with the "new" value.  The implementation makes use of
    * the following assumption: if listEvents == IGNORE_LIST then oKey does
    * not exist in the front, and there is no key based listener for it.  Any
    * key passed to this method must be locked in the control map by the
    * caller.
    *
    * @param oKey        the key
    * @param oValue      the new value
    * @param listEvents  the event list associated with the key
    * @param cMillis     the number of milliseconds until the cache entry
    *                    will expire
    */
    private void finalizePut(K oKey, V oValue, List listEvents,
            long cMillis)
        {
        Map<K, V>     mapFront         = getFrontMap();
        ConcurrentMap mapControl       = getControlMap();
        int           nStrategyTarget  = m_nStrategyTarget;
        int           nStrategyCurrent = m_nStrategyCurrent;

        if (nStrategyTarget == LISTEN_NONE)
            {
            // we're not validating; simply update the front
            if (oValue != null)
                {
                put(mapFront, oKey, oValue, cMillis);
                }
            }
        else if (listEvents == IGNORE_LIST)
            {
            // IGNORE_LIST indicates that the entry is not already in the
            // front; we're not going to add it
            mapControl.remove(oKey);
            }
        else if (listEvents == null)
            {
            // can only be null for LISTEN_NONE which is covered above
            throw new IllegalStateException("Encountered unexpected key "
                    + oKey + "; this may be caused by concurrent "
                    + "modification of the supplied key(s), or by an "
                    + "inconsistent hashCode() or equals() implementation.");
            }
        else
            {
            // validate events and update the front if possible
            synchronized (listEvents)
                {
                // put operation itself should generate one "natural"
                // INSERT or UPDATE; anything else should be considered
                // as an invalidating event
                boolean fValid;
                if (oValue == null)
                    {
                    fValid = false;
                    }
                else
                    {
                    switch (listEvents.size())
                        {
                        case 0:
                            if (STRICT_SYNCHRO_LISTENER &&
                                    (nStrategyCurrent == LISTEN_ALL ||
                                     nStrategyCurrent == LISTEN_LOGICAL ||
                                     mapFront.containsKey(oKey)))
                                {
                                Base.log("Expected an insert/update for " + oKey +
                                        ", but none have been received");
                                fValid = false;
                                }
                            else
                                {
                                fValid = true;
                                }
                            break;
                        case 1:
                            {
                            MapEvent<K, V> evt = (MapEvent<K, V>) listEvents.get(0);
                            int nId = evt.getId();

                            fValid = nId == MapEvent.ENTRY_INSERTED ||
                                     nId == MapEvent.ENTRY_UPDATED;
                            if (fValid)
                                {
                                V oValueNew = evt.getNewValue();
                                if (oValueNew != null)
                                    {
                                    // While we subscribed only to light events, there are
                                    // two scenarios when the event can carry the values:
                                    //   1) there is another "heavy" listener for that key;
                                    //   2) the put value was changed by a trigger or
                                    //      an interceptor (see COH-15130).
                                    // In both cases we can safely use the new value
                                    // to update the front map
                                    oValue = oValueNew;
                                    }
                                }
                            break;
                            }
                        default:
                            fValid = false;
                            break;
                        }
                    }

                if (fValid)
                    {
                    if (put(mapFront, oKey, oValue, cMillis) == null &&
                        nStrategyTarget == LISTEN_PRESENT)
                        {
                        // this entry was evicted from behind us, and thus
                        // we haven't been listening to its events for
                        // some time, so we may not have the current value
                        mapFront.remove(oKey);
                        }
                    }
                else
                    {
                    invalidateFront(oKey);
                    }

                // remove event list from the control map; in this case
                // it must be done while still under synchronization
                mapControl.remove(oKey);
                }
            }
        }

    /**
    * Validate the front map entry for the specified back map event.
    *
    * @param evt  the MapEvent from the back map
    */
    protected void validate(MapEvent evt)
        {
        ConcurrentMap mapControl = getControlMap();
        Object        oKey       = evt.getKey();
        long          ldtStart   = 0;

        for (int i = 0; true; ++i)
            {
            // after first iteration, fallback to 1 millis wait to slow down polling and fix
            // COH-26003 by checking if lock held by a non-active thread by calling lock with non-zero wait
            if (mapControl.lock(oKey, i == 0 ? 0 : 1))
                {
                try
                    {
                    List listEvents = (List) mapControl.get(oKey);
                    if (listEvents == null)
                        {
                        if (!isPriming(evt))
                            {
                             // not in use; invalidate front entry
                            invalidateFront(oKey);
                            }
                        }
                    else
                        {
                        // this can only happen if the back map fires event
                        // on the caller's thread (e.g. LocalCache)
                        listEvents.add(evt);
                        }
                    return;
                    }
                finally
                    {
                    mapControl.unlock(oKey);
                    }
                }
            else
                {
                // check for a key based action
                List listEvents = (List) mapControl.get(oKey);

                if (listEvents == null)
                    {
                    // check for a global action
                    listEvents = (List) mapControl.get(GLOBAL_KEY);
                    if (listEvents == null)
                        {
                        // has not been assigned yet, or has been just
                        // removed or switched; try again
                        Thread.yield();
                        long ldtNow = Base.getSafeTimeMillis();
                        if (ldtStart == 0)
                            {
                            ldtStart = ldtNow;
                            }
                        else if (i > 5000 && ldtNow - ldtStart > 5000)
                            {
                            // we've been spinning and have given the other
                            // thread ample time to register the event list;
                            // the control map is corrupt
                            Base.err("Detected a state corruption on the key \""
                                + oKey + "\", of class "
                                + oKey.getClass().getName()
                                + " which is missing from the active key set "
                                + mapControl.keySet()
                                + ". This could be caused by a mutating or "
                                + "inconsistent key implementation, or a "
                                + "concurrent modification to the map passed to "
                                + getClass().getName() + ".putAll()");

                            invalidateFront(oKey);
                            return;
                            }
                        continue;
                        }
                    }

                synchronized (listEvents)
                    {
                    List listKey = (List) mapControl.get(oKey);
                    if (listEvents == listKey || (listKey == null &&
                        listEvents == mapControl.get(GLOBAL_KEY)))
                        {
                        listEvents.add(evt);
                        return;
                        }
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public V remove(Object oKey)
        {
        Map<K, V> mapFront  = getFrontMap();
        Map<K, V> mapBack   = getBackMap();
        int       nStrategy = m_nStrategyTarget;

        ConcurrentMap mapControl = getControlMap();
        mapControl.lock(oKey, -1);
        try
            {
            if (nStrategy != LISTEN_NONE)
                {
                mapControl.put(oKey, IGNORE_LIST);
                }

            if (mapFront.remove(oKey) != null)
                {
                unregisterListener(oKey);
                }

            return mapBack.remove(oKey);
            }
        finally
            {
            if (nStrategy != LISTEN_NONE)
                {
                mapControl.remove(oKey);
                }
            mapControl.unlock(oKey);
            }
        }

    /**
    * Return the number of key-value mappings in this map.
    * Expensive: always reflects the contents of the underlying cache.
    *
    * @return the number of key-value mappings in this map
    */
    @Override
    public int size()
        {
        return getBackMap().size();
        }

    /**
    * Obtain an collection of the values contained in this map. If there is
    * a listener for the back map, then the collection will be mutable;
    * otherwise the returned collection will be immutable.
    *
    * The returned collection reflects the full contents of the back map.
    *
    * @return a collection view of the values contained in this map
    */
    @Override
    public Collection<V> values()
        {
        Collection<V> values = getBackMap().values();
        if (!isCoherent())
            {
            values = Collections.unmodifiableCollection(values);
            }
        return values;
        }

    /**
    * Determine the rough number of front map invalidation hits since
    * the cache statistics were last reset.
    * <p>
    * An invalidation hit is an externally induced map event for an entry
    * that exists in the front map.
    *
    * @return the number of cache invalidation hits
    */
    public long getInvalidationHits()
        {
        return m_cInvalidationHits;
        }

    /**
    * Determine the rough number of front map invalidation misses since
    * the cache statistics were last reset.
    *
    * An invalidation miss is an externally induced map event for an entry
    * that does not exists in the front map.
    *
    * @return the number of cache invalidation misses
    */
    public long getInvalidationMisses()
        {
        return m_cInvalidationMisses;
        }

    /**
    * Determine the total number of {@link #registerListener(Object oKey)}
    * operations since the cache statistics were last reset.
    *
    * @return the total number of listener registrations
    */
    public long getTotalRegisterListener()
        {
        return m_cRegisterListener;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * For debugging purposes, format the contents of the CachingMap
    * in a human readable format.
    *
    * @return a String representation of the CachingMap object
    */
    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder("CachingMap");
        try
            {
            Map mapFront = getFrontMap();
            Map mapBack  = getBackMap();

            sb.append("{FrontMap{class=")
              .append(mapFront.getClass().getName())
              .append(", size=")
              .append(mapFront.size())
              .append("}, BackMap{class=")
              .append(mapBack.getClass().getName())
              .append(", size=")
              .append(mapBack.size())
              .append("}, strategy=")
              .append(getInvalidationStrategy(getInvalidationStrategy()))
              .append(", CacheStatistics=")
              .append(getCacheStatistics())
              .append(", invalidation hits=")
              .append(getInvalidationHits())
              .append(", invalidation misses=")
              .append(getInvalidationMisses())
              .append(", listener registrations=")
              .append(getTotalRegisterListener())
              .append('}');
            }
        catch (IllegalStateException e)
            {
            sb.append(" not active");
            }
        return sb.toString();
        }


    // ----- back map listener support --------------------------------------

    /**
    * Register the global back map listener.
    */
    protected void registerListener()
        {
        ((ObservableMap) getBackMap()).
            addMapListener(m_listener, m_filterListener, true);
        }


    /**
    * Unregister the global back map listener.
    */
    protected void unregisterListener()
        {
        ((ObservableMap) getBackMap()).
            removeMapListener(m_listener, m_filterListener);
        }

    /**
    * Register the back map listener for the specified key.
    *
    * @param oKey  the key
    */
    protected void registerListener(Object oKey)
        {
        if (ensureInvalidationStrategy() == LISTEN_PRESENT)
            {
            try
                {
                ((ObservableMap) getBackMap()).
                    addMapListener(m_listener, oKey, true);
                }
            catch (UnsupportedOperationException e)
                {
                // the back is of an older version; need to reset the
                // "old" non-priming listener
                m_listener = instantiateBackMapListener(LISTEN_ALL);
                ((ObservableMap) getBackMap()).
                    addMapListener(m_listener, oKey, true);
                }
            m_cRegisterListener++;
            }
        }

    /**
    * Register the back map listeners for the specified set of keys.
    *
    * @param setKeys  the key set
    */
    protected void registerListeners(Set setKeys)
        {
        if (ensureInvalidationStrategy() == LISTEN_PRESENT)
            {
            if (m_listener instanceof CachingMap.PrimingListener)
                {
                try
                    {
                    ((ObservableMap) getBackMap()).
                        addMapListener(m_listener,
                            new InKeySetFilter(null, setKeys), true);
                    m_cRegisterListener += setKeys.size();
                    return;
                    }
                catch (UnsupportedOperationException e)
                    {
                    // the back is of an older version; need to reset the
                    // "old" non-priming listener
                    m_listener = instantiateBackMapListener(LISTEN_ALL);
                    }
                }

            // use non-optimized legacy algorithm
            for (Object oKey : setKeys)
                {
                registerListener(oKey);
                }
            }
        }

    /**
    * Unregister the back map listener for the specified key.
    *
    * @param oKey  the key
    */
    protected void unregisterListener(Object oKey)
        {
        if (m_nStrategyCurrent == LISTEN_PRESENT)
            {
            ConcurrentMap mapControl = getControlMap();

            // bumped wait to 1 millis as part of COH-26003 fix to check if lock held by
            // a terminated thread
            if (mapControl.lock(oKey, 1))
                {
                if (m_listener instanceof CachingMap.PrimingListener)
                    {
                    Set setKeys = s_tloKeys.get();
                    if (setKeys != null)
                        {
                        boolean fAdded = setKeys.add(oKey);
                        if (!fAdded)
                            {
                            // Fix COH-26224
                            // all keys in setKeys are already locked once, so release the redundant lock acquired by this method.
                            // Final unregisterListeners processing will only release one lock per key in set.
                            mapControl.unlock(oKey);
                            }

                        // the key is still locked; it will be unlocked
                        // along with other keys after bulk un-registration
                        // in the unregisterListeners(setKeys) method
                        return;
                        }
                    }

                try
                    {
                    ((ObservableMap) getBackMap()).
                            removeMapListener(m_listener, oKey);
                    }
                finally
                    {
                    mapControl.unlock(oKey);
                    }
                }
            }
        }

    /**
    * Unregister the back map listener for the specified keys.
    * <p>
    * Note: all the keys in the passed-in set must be locked and will be
    *       unlocked.
    *
    * @param setKeys  Set of keys to unregister (and unlock)
    */
    protected void unregisterListeners(Set<K> setKeys)
        {
        if (m_nStrategyCurrent == LISTEN_PRESENT &&
            m_listener instanceof CachingMap.PrimingListener)
            {
            if (!setKeys.isEmpty())
                {
                try
                    {
                    ((ObservableMap) getBackMap()).removeMapListener(
                            m_listener, new InKeySetFilter(null, setKeys));
                    }
                finally
                    {
                    ConcurrentMap mapControl = getControlMap();
                    for (K key : setKeys)
                        {
                        mapControl.unlock(key);
                        }
                    }
                }
            }
        else
            {
            throw new UnsupportedOperationException(
                "unregisterListeners can only be called with PRESENT strategy");
            }
        }

    /**
    * Set up a thread local Set to hold all the keys that might be evicted
    * from the front cache.
    *
    * @return a Set to hold all the keys in the ThreadLocal object or null
    *         if the bulk unregistering is not needed
    */
    protected Set setKeyHolder()
        {
        if (m_nStrategyCurrent == LISTEN_PRESENT &&
            m_listener instanceof CachingMap.PrimingListener)
            {
            Set setKeys = new HashSet<>();
            s_tloKeys.set(setKeys);
            return setKeys;
            }

        return null;
        }

    /**
    * Remove the key holder from the ThreadLocal object.
    */
    protected void removeKeyHolder()
        {
        s_tloKeys.set(null);
        }

    /**
    * Register the global front map listener.
    */
    protected void registerFrontListener()
        {
        FrontMapListener listener = m_listenerFront;
        if (listener != null)
            {
            listener.register();
            }
        }

    /**
    * Unregister the global front map listener.
    */
    protected void unregisterFrontListener()
        {
        FrontMapListener listener = m_listenerFront;
        if (listener != null)
            {
            listener.unregister();
            }
        }

    /**
    * Ensure that a strategy has been chosen and that any appropriate global
    * listeners have been registered.
    *
    * @return the current strategy
    */
    protected int ensureInvalidationStrategy()
        {
        // the situation in which
        //      (m_nStrategyCurrent != m_nStrategyTarget)
        // can happen either at the first map access following the
        // instantiation or after resetInvalidationStrategy() is called

        int nStrategyTarget = m_nStrategyTarget;
        switch (nStrategyTarget)
            {
            case LISTEN_AUTO:
                // as of Coherence 12.1.2, default LISTEN_AUTO to LISTEN_PRESENT
            case LISTEN_PRESENT:
                if (m_nStrategyCurrent != LISTEN_PRESENT)
                    {
                    synchronized (GLOBAL_KEY)
                        {
                        if (m_nStrategyCurrent != LISTEN_PRESENT)
                            {
                            registerFrontListener();
                            registerDeactivationListener();

                            m_nStrategyCurrent = LISTEN_PRESENT;
                            }
                        }
                    }
                return LISTEN_PRESENT;

            case LISTEN_LOGICAL:
            case LISTEN_ALL:
                if (m_nStrategyCurrent != nStrategyTarget)
                    {
                    synchronized (GLOBAL_KEY)
                        {
                        if (m_nStrategyCurrent != nStrategyTarget)
                            {
                            if (nStrategyTarget == LISTEN_LOGICAL)
                                {
                                // LOGICAL behaves like ALL, but with synthetic deletes filtered out
                                m_filterListener = new NotFilter(
                                    new CacheEventFilter(CacheEventFilter.E_DELETED,
                                        CacheEventFilter.E_SYNTHETIC));
                                }
                            registerListener();
                            registerDeactivationListener();

                            m_nStrategyCurrent = nStrategyTarget;
                            }
                        }
                    }
                return nStrategyTarget;
            }
        return LISTEN_NONE;
        }

    /**
    * Reset the "current invalidation strategy" flag.
    * <p>
    * This method should be called <b>only</b> while the access to the front
    * map is fully synchronised and the front map is empty to prevent stalled
    * data.
    */
    protected void resetInvalidationStrategy()
        {
        m_nStrategyCurrent = LISTEN_NONE;
        m_filterListener   = null;
        }

    /**
    * Factory pattern: instantiate back map listener.
    *
    * @param nStrategy the strategy to instantiate a back map listener for
    *
    * @return an instance of back map listener responsible for keeping the
    *         front map coherent with the back map
    */
    protected MapListener instantiateBackMapListener(int nStrategy)
        {
        return nStrategy == LISTEN_AUTO || nStrategy == LISTEN_PRESENT
            ? new PrimingListener()
            : new SimpleListener();
        }

    /**
    * Instantiate and register a DeactivationListener with the back cache.
    */
    protected void registerDeactivationListener()
        {
        try
            {
            NamedCacheDeactivationListener listener = m_listenerDeactivation;
            if (listener != null)
                {
                ((NamedCache) getBackMap()).addMapListener(listener);
                }
            }
        catch (RuntimeException e) {};
        }

    /**
    * Unregister back cache deactivation listener.
    */
    protected void unregisterDeactivationListener()
        {
        try
            {
            NamedCacheDeactivationListener listener = m_listenerDeactivation;
            if (listener != null)
                {
                ((NamedCache) getBackMap()).removeMapListener(listener);
                }
            }
        catch (RuntimeException e) {}
        }


    // ----- inner classes --------------------------------------------------
    /**
    * DeactivationListener for the back NamedCache.
    * <p>
    * The primary goal of that listener is invalidation of the front map
    * when the back cache is destroyed or all storage nodes are stopped.
    */
    protected class DeactivationListener
            extends AbstractMapListener
            implements NamedCacheDeactivationListener
        {
        @Override
        public void entryDeleted(MapEvent evt)
            {
            // destroy/disconnect event
            resetFrontMap();
            unregisterMBean();
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            // "truncate" event
            onTruncate();
            }
        }

    /**
    * Reset the front map.
    */
    public void resetFrontMap()
        {
        try
            {
            unregisterFrontListener();
            getFrontMap().clear();
            }
        catch (RuntimeException e) {}

        resetInvalidationStrategy();
        }

    // ----- helper ---------------------------------------------------------

    /**
     * Reset front map and control map when back map is truncated.
     * Post condition is front map is reset, current invalidationStrategy reset to NONE (will be ensured from target invalidation strategy)
     * and control map must not contain any non-synthetic lockable entries for keys.
     */
    private void onTruncate()
        {
        if (m_nStrategyTarget == LISTEN_NONE)
            {
            resetFrontMap();
            return;
            }

        long                   ldtStartTime = Base.getSafeTimeMillis();
        SegmentedConcurrentMap mapControl   = (SegmentedConcurrentMap) getControlMap();

        // block any getAll/putAll starting
        mapControl.put(GLOBAL_KEY, IGNORE_LIST);

        // Note: we cannot do a blocking LOCK_ALL as any event which came
        // in while the ThreadGate is in the closing state would cause the
        // service thread to spin.  Try for up ~3s before giving up acquiring
        // LOCK_ALL and just complete reset of front map and truncate of its control map.
        // We don't even risk a timed LOCK_ALL as whatever
        // time value we choose would risk a useless spin for that duration
        final long ldtDurationMs     = 3000;
        final long ldtWaitDeadlineMs = ldtStartTime + ldtDurationMs;

        while (!mapControl.lock(ConcurrentMap.LOCK_ALL, 0) && Base.getSafeTimeMillis() < ldtWaitDeadlineMs)
            {
            try
                {
                Blocking.sleep(10);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }
            }
        try
            {
            // All entries and registered key listeners have been truncated from backing map.
            // Perform truncate on near cache front and control map.
            // No entries should be in front and control map after exiting synchronize block.
            synchronized (GLOBAL_KEY)
                {
                resetFrontMap();
                mapControl.truncate();
                }
            }
        catch (RuntimeException e)
            {
            Logger.finer("CachingMap.onTruncate: unexpected exception " + e + " \nStack trace: " + Base.printStackTrace(e));
            Logger.finer("CachingMap.onTruncate: unexpected held key locks");
            mapControl.dumpHeldLocks();
            }
        finally
            {
            mapControl.remove(GLOBAL_KEY);
            mapControl.unlock(ConcurrentMap.LOCK_ALL);
            }
        }

    /**
     * Return string representation for invalidation strategy {@code value}.
     *
     * @param value  one of LISTEN_*
     *
     * @return string for invalidation strategy
     *
     * @since 12.2.1.4.21
     */
    public String getInvalidationStrategy(int value)
        {
        return value >= 0 && value < INVALIDATION_STRATEGY.length ? INVALIDATION_STRATEGY[value] : "<unknown invalidation strategy:" + value + ">";
        }

    /**
    * Unregister an associated CacheMBean.
    */
    protected void unregisterMBean()
        {
        // implemented by NearCache
        }

    /**
    * MapListener for back map responsible for keeping the front map
    * coherent with the back map. This listener is registered as a
    * synchronous listener for lite events (carrying only a key) and generates
    * a "priming" event when registered.
    */
    protected class PrimingListener
            extends MultiplexingMapListener
            implements MapListenerSupport.PrimingListener
        {
        @Override
        protected void onMapEvent(MapEvent evt)
            {
            validate(evt);
            }
        }

    /**
    * MapListener for back map responsible for keeping the front map
    * coherent with the back map. This listener is registered as a
    * synchronous listener for lite events (carrying only a key).
    */
    protected class SimpleListener
            extends MultiplexingMapListener
            implements MapListenerSupport.SynchronousListener
        {
        @Override
        protected void onMapEvent(MapEvent evt)
            {
            validate(evt);
            }
        }


    // ----- front map listener support -------------------------------------

    /**
    * Factory pattern: instantiate front map listener.
    *
    * @return an instance of front map listener
    */
    protected FrontMapListener instantiateFrontMapListener()
        {
        return new FrontMapListener();
        }

    /**
    * MapListener for front map responsible for deregistering back map
    * listeners upon front map eviction.
    */
    protected class FrontMapListener
            extends AbstractMapListener
            implements MapListenerSupport.SynchronousListener
        {
        @Override
        public void entryDeleted(MapEvent evt)
            {
            // explicitly removed or evicted from from map
            // unregister listener on back map
            if (evt instanceof CacheEvent)
                {
                if (((CacheEvent) evt).isSynthetic())
                    {
                    unregisterListener(evt.getKey());
                    }
                }
            else
                {
                // plain MapEvent, can't tell if it was synthetic or not
                // but it is always appropriate to unregister if we've
                // removed something from the front. This may be a double
                // unregistration as the caller may have already explicitly
                // unregistered
                unregisterListener(evt.getKey());
                }
            }

        // ---- helper registration methods -----------------------------

        /**
        * Register this listener with the "front" map.
        */
        public void register()
            {
            ((ObservableMap) getFrontMap()).addMapListener
                    (this, m_filter, true);
            }

        /**
        * Unregister this listener with the "front" map.
        */
        public void unregister()
            {
            ((ObservableMap) getFrontMap()).removeMapListener(this, m_filter);
            }

        // ----- data members -------------------------------------------

        /**
        * The filter associated with this listener.
        */
        protected Filter m_filter = new MapEventFilter
                (MapEventFilter.E_DELETED);
        }


    // ----- constants ------------------------------------------------------

    /**
    * No invalidation strategy.
    */
    public static final int LISTEN_NONE    = 0;

    /**
    * Invalidation strategy that instructs the CachingMap to listen to the
    * back map events related <b>only</b> to the items currently present
    * in the front map; this strategy serves best when the changes to the
    * back map come mostly from the CachingMap itself.
    */
    public static final int LISTEN_PRESENT = 1;

    /**
    * Invalidation strategy that instructs the CachingMap to listen to
    * <b>all</b> back map events; this strategy is preferred when updates to
    * the back map are frequent and with high probability come from the
    * outside of this CachingMap; for example multiple CachingMap instances
    * using the same back map with a large degree of key set overlap between
    * front maps.
    */
    public static final int LISTEN_ALL     = 2;

    /**
    * Invalidation strategy that instructs the CachingMap implementation to
    * switch automatically between LISTEN_PRESENT and LISTEN_ALL strategies
    * based on the cache statistics.
    */
    public static final int LISTEN_AUTO    = 3;

    /**
    * Invalidation strategy that instructs the CachingMap to listen to <b>all</b>
    * back map events that are <b>not synthetic deletes</b>. A synthetic event
    * could be emitted as a result of eviction or expiration. With this
    * invalidation strategy, it is possible for the front map to contain cache
    * entries that have been synthetically removed from the back (though any
    * subsequent re-insertion will cause the corresponding entries in the front
    * map to be invalidated).
    */
    public static final int LISTEN_LOGICAL = 4;

    /**
     * String representation for {@link #LISTEN_NONE}, {@link #LISTEN_PRESENT}, {@link #LISTEN_ALL}, {@link #LISTEN_AUTO}, {@link #LISTEN_LOGICAL}.
     *
     * @since 12.2.1.4.21
     */
    public static final String[] INVALIDATION_STRATEGY = {"NONE", "PRESENT", "ALL", "AUTO", "LOGICAL"};

    /**
    * Specifies whether the back map listener strictly adheres to the
    * MapListenerSupport.SynchronousListener contract.
    */
    private static final boolean STRICT_SYNCHRO_LISTENER = Config.getBoolean("coherence.near.strictlistener", true);

    /**
    * Specifies whether this CachingMap should infer the server's capability
    * of sending priming events. This undocumented flag allows customers to
    * disable this inferring logic.
    */
    private static final boolean STRICT_PRIMING = Config.getBoolean("coherence.near.strictpriming", true);


    // ----- data members ---------------------------------------------------

    /**
    * The "back" map, considered to be "complete" yet "expensive" to access.
    */
    private Map<K, V> m_mapBack;

    /**
    * The "front" map, considered to be "incomplete" yet "inexpensive" to
    * access.
    */
    private Map<K, V> m_mapFront;

    /**
    * The invalidation strategy that this map is to use.
    */
    protected int m_nStrategyTarget;

    /**
    * The current invalidation strategy, which at times could be different
    * from the target strategy.
    */
    protected int m_nStrategyCurrent;

    /**
    * An optional listener for the "back" map.
    */
    private MapListener m_listener;

    /**
    * An optional listener for the "front" map.
    */
    private FrontMapListener m_listenerFront;

    /**
    * A filter that selects events for the back-map listener.
    */
    private Filter m_filterListener;

    /**
     * The NamedCache deactivation listener.
     */
    protected NamedCacheDeactivationListener m_listenerDeactivation;

    /**
    * The ConcurrentMap to keep track of front map updates.
    * Values are list of events received by the listener while the
    * corresponding key was locked.  Use of LOCK_ALL is restricted to
    * non-blocking operations to prevent deadlock with the service thread.
    */
    private ConcurrentMap m_mapControl;

    /**
    * The CacheStatistics object maintained by this cache.
    */
    private SimpleCacheStatistics m_stats = new SimpleCacheStatistics();

    /**
    * The rough (ie unsynchronized) number of times the front map entries
    * that were present in the front map were invalidated by the listener.
    */
    private volatile long m_cInvalidationHits;

    /**
    * The rough (ie unsynchronized) number of times the front map entries
    * that were absent in the front map received invalidation event.
    */
    private volatile long m_cInvalidationMisses;

    /**
    * The total number of registerListener(oKey) operations.
    */
    private volatile long m_cRegisterListener;

    /**
    * The ThreadLocal to hold all the keys that are evicted while the front cache
    * is updated during get or getAll operation.
    */
    private final static ThreadLocal<Set> s_tloKeys = new ThreadLocal<>();

    /**
    * A unique Object that serves as a control key for global operations
    * such as clear and release and synchronization point for the current
    * strategy change.
    */
    private final Object GLOBAL_KEY = new Object();

    /**
    * Whether this implementation can rely on the server emitting priming events.
    */
    private final AtomicBoolean f_atomicPrimingOnly = new AtomicBoolean();

    /**
    * Empty list that ignores any add operations.
    */
    private static final List IGNORE_LIST = new ImmutableArrayList(new Object[0])
        {
        public boolean add(Object o)
            {
            return true;
            }
        };
    }
