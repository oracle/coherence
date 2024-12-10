/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.cache;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.io.nio.BinaryMap;
import com.tangosol.net.NamedCache;
import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.Base;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterEnumerator;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.Gate;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.RecyclingLinkedList;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.ThreadGateLite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


/**
* A non-observable Map implementation that wraps two maps - a front map
* (assumed to be fast but limited in its maximum size) and a back map
* (assumed to be slower but much less limited in its maximum size).
* <p>
* The SimpleOverflowMap is not observable, and as such it is assumed to be
* a passive data structure. In other words, it does not support item
* expiration or other types of "self-generating" events. As such, it may
* be more efficient for many common use cases that would benefit from the
* complete avoidance of event handling. As a second effect of being a passive
* data structure, the implementation is able to avoid tracking all of its
* entries; instead it tracks only entries that are in the front map and those
* that currently have a pending event or an in-flight operation occurring.
* This means that huge key sets are possible, since only keys in the front
* map will be managed in memory by the overflow map, but it means that some
* operations that would benefit from knowing the entire set of keys will be
* more expensive. Examples of operations that would be less efficient as a
* result include {@link #containsKey containsKey()}, {@link #size()},
* {@link com.tangosol.util.AbstractKeyBasedMap.KeySet#iterator
* keySet().iterator()}, etc.
*
* @since Coherence 3.1
* @author cp  2005.07.11
*/
public class SimpleOverflowMap
        extends AbstractKeyBasedMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SimpleOverflowMap using two specified maps:
    * <ul>
    * <li> <i>FrontMap</i> (aka "cache" or "shallow") and
    * <li> <i>BackMap</i>  (aka "file" or "deep")
    * </ul>
    *
    * @param mapBack   back map
    * @param mapFront  front map
    */
    public SimpleOverflowMap(ObservableMap mapFront, Map mapBack)
        {
        this(mapFront, mapBack, null);
        }

    /**
    * Construct a SimpleOverflowMap using three specified maps:
    * <ul>
    * <li> <i>Front Map</i> (aka "cache" or "shallow") and
    * <li> <i>Back Map</i>  (aka "file" or "deep")
    * <li> <i>Miss Cache</i>
    * </ul>
    *
    * @param mapFront  front map
    * @param mapBack   back map
    * @param mapMiss   an optional miss cache
    */
    public SimpleOverflowMap(ObservableMap mapFront, Map mapBack, Map mapMiss)
        {
        azzert(mapFront != null && mapBack != null);

        m_mapFront = mapFront;
        m_mapBack  = mapBack;

        // get the keys in the front map (i.e. handle the case in which the
        // front map already has something in it); unfortunately, it is
        // [theoretically] possible for changes to occur in between this step
        // and when we put the listener on, but the listener can't be added
        // until the status map is prepared
        ImmutableArrayList setInitial = new ImmutableArrayList(mapFront.keySet());
        if (!setInitial.isEmpty())
            {
            Map mapStatus = getStatusMap();
            for (Iterator iter = setInitial.iterator(); iter.hasNext(); )
                {
                Object oKey   = iter.next();
                Status status = new Status();
                status.setEntryInFront(true);
                status.setBackUpToDate(false);
                mapStatus.put(oKey, status);
                }
            }

        setFrontMapListener(instantiateFrontMapListener());

        // store the miss cache
        if (mapMiss != null && !mapMiss.isEmpty())
            {
            mapMiss.clear();
            }
        m_mapMiss = mapMiss;

        // double-check that everything is ready to go
        if (!setInitial.isEmpty())
            {
            verifyNoNulls(mapFront.keySet(), "The front map contains a null key");
            if (!isNullValuesAllowed())
                {
                verifyNoNulls(mapFront.values(), "NullValuesAllowed is false"
                    + " but the front map contains at least one null value");
                }

            // double check that the front map didn't change in the meantime
            azzert(mapFront.keySet().equals(setInitial));
            }

        // come up with a good default for the FrontPutBlind setting; for
        // each of these classes, it is assumed that the cost of using a temp
        // map with a putAll() is less than the cost of returning the orig
        // value from a put() call
        if (   mapFront instanceof NamedCache
               || mapFront instanceof CachingMap
               || mapFront instanceof SerializationMap
               || mapFront instanceof BinaryMap
               || mapFront instanceof OverflowMap
               || mapFront instanceof SimpleOverflowMap)
            {
            setFrontPutBlind(true);
            }
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Clear all key/value mappings.
    */
    @Override
    public void clear()
        {
        beginMapProcess();
        try
            {
            // kill the listener temporarily (we don't have any desire to
            // listen to everything getting cleared out of the front cache)
            MapListener listener = getFrontMapListener();
            setFrontMapListener(null);
            try
                {
                // clear both the front and back, which will leave the
                // overflow map in an empty state
                getFrontMap().clear();
                getBackMap().clear();

                // clear the statistics
                getCacheStatistics().resetHitStatistics();

                // clear the miss map; while this is not necessary, we are
                // clearing everything else so it only makes sense
                Map mapMiss = getMissCache();
                if (mapMiss != null)
                    {
                    mapMiss.clear();
                    }

                // clean up the status objects
                Map mapStatus = getStatusMap();
                synchronized (mapStatus)
                    {
                    for (Iterator iter = mapStatus.values().iterator(); iter.hasNext(); )
                        {
                        Status status = (Status) iter.next();
                        synchronized (status)
                            {
                            // since no one else is processing, the status
                            // should be available
                            assert status.isOwnedByCurrentThread();

                            // clear the status
                            status.setEntryInFront(false);
                            status.setBackUpToDate(true);

                            // clear any pending event
                            status.takeEvent();
                            }
                        }
                    }

                // clear out the list of statuses that have deferred events
                getDeferredList().clear();
                }
            finally
                {
                setFrontMapListener(listener);
                }
            }
        finally
            {
            endMapProcess();
            }
        }

    /**
    * Returns <tt>true</tt> if this map contains a mapping for the specified
    * key.
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key, <tt>false</tt> otherwise.
    */
    @Override
    public boolean containsKey(Object oKey)
        {
        Status status = beginKeyProcess(oKey);
        try
            {
            if (status.isEntryInFront())
                {
                return true;
                }

            Map mapMiss = getMissCache();
            if (mapMiss != null && mapMiss.containsKey(oKey))
                {
                return false;
                }

            boolean fContains = getBackMap().containsKey(oKey);
            if (!fContains && mapMiss != null)
                {
                mapMiss.put(oKey, null);
                }
            return fContains;
            }
        finally
            {
            endKeyProcess(oKey, status);
            }
        }

    /**
    * Returns the value to which this map maps the specified key.
    *
    * @param oKey  the key object
    *
    * @return the value to which this map maps the specified key,
    *         or null if the map contains no mapping for this key
    */
    @Override
    public Object get(Object oKey)
        {
        Object  oValue    = null;
        boolean fContains = false;
        long    ldtStart  = getSafeTimeMillis();
        Status  status    = beginKeyProcess(oKey);
        try
            {
            if (status.isEntryInFront())
                {
                fContains = true;

                Map mapFront = getFrontMap();
                oValue = mapFront.get(oKey);
                MapEvent evt;
                synchronized (mapFront)
                    {
                    evt = status.closeProcessing();
                    }

                if (evt != null)
                    {
                    switch (evt.getId())
                        {
                        case ENTRY_INSERTED:
                        case ENTRY_UPDATED:
                            if (oValue == null)
                                {
                                oValue = evt.getNewValue();
                                }
                            break;

                        case ENTRY_DELETED:
                            if (oValue == null)
                                {
                                oValue = evt.getOldValue();
                                }
                            break;
                        }
                    processFrontEvent(status, evt);
                    }
                }
            else
                {
                Map mapMiss = getMissCache();
                if (mapMiss == null || !mapMiss.containsKey(oKey))
                    {
                    Map mapBack = getBackMap();
                    oValue    = mapBack.get(oKey);
                    fContains = oValue != null
                            || isNullValuesAllowed() && mapBack.containsKey(oKey);

                    if (fContains)
                        {
                        Map mapFront = getFrontMap();
                        putOne(mapFront, oKey, oValue, isFrontPutBlind());

                        status.setEntryInFront(true);
                        status.setBackUpToDate(true);

                        MapEvent evt;
                        synchronized (mapFront)
                            {
                            evt = status.closeProcessing();
                            }

                        if (evt != null)
                            {
                            // we're expecting an inserted event, and so it can be
                            // safely ignored
                            if (evt.getId() != ENTRY_INSERTED)
                                {
                                processFrontEvent(status, evt);
                                }
                            }
                        }
                    else if (mapMiss != null)
                        {
                        mapMiss.put(oKey, null);
                        }
                    }
                }
            }
        finally
            {
            endKeyProcess(oKey, status);
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
    * Returns <tt>true</tt> if this map contains no key-value mappings.
    *
    * @return <tt>true</tt> if this map contains no key-value mappings
    */
    @Override
    public boolean isEmpty()
        {
        // from a concurrency point of view, it is possible to conceive of a
        // situation in which isEmpty could return false when the map is not
        // actually empty, but the converse is not true
        if (getFrontMap().isEmpty() && getBackMap().isEmpty())
            {
            // the expense of synchronization etc. is therefore only incurred
            // when the map appears to be empty, which is unlikely to occur
            // often, and when it does, the expense of obtaining exclusive
            // ownership of all the status objects is minimized (since there
            // are none)
            beginMapProcess();
            try
                {
                return getFrontMap().isEmpty() && getBackMap().isEmpty();
                }
            finally
                {
                endMapProcess();
                }
            }
        else
            {
            return false;
            }
        }

    /**
    * Associates the specified value with the specified key in this map.
    *
    * @param oKey    key with which the specified value is to be associated
    * @param oValue  value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key
    */
    @Override
    public Object put(Object oKey, Object oValue)
        {
        if (oValue == null && !isNullValuesAllowed())
            {
            throw new IllegalArgumentException("null value is unsupported"
                    + "(key=\"" + oKey + "\")");
            }

        long   ldtStart  = getSafeTimeMillis();
        Object oOldValue = null;
        Status status    = beginKeyProcess(oKey);
        try
            {
            Map     mapFront         = getFrontMap();
            Map     mapBack          = getBackMap();
            Map     mapMiss          = getMissCache();
            boolean fEntryWasInFront = status.isEntryInFront();
            boolean fBackWasUpToDate = status.isBackUpToDate();
            boolean fOldValue        = false;
            boolean fEvicted         = false;

            // store the new value in the front map
            if (fEntryWasInFront)
                {
                oOldValue = mapFront.put(oKey, oValue);
                if (oOldValue != null)
                    {
                    fOldValue = true;
                    }
                }
            else
                {
                putOne(mapFront, oKey, oValue, isFrontPutBlind());
                }
            status.setEntryInFront(true);
            status.setBackUpToDate(false);

            // handle any events from the front
            MapEvent evt = status.closeProcessing();
            if (evt == null)
                {
                // unless we can grab the old value from the back, it's
                // been lost (since we didn't get any event); just assume
                // it was null
                if (!fOldValue && fEntryWasInFront && !fBackWasUpToDate)
                    {
                    fOldValue = true;
                    }
                }
            else
                {
                switch (evt.getId())
                    {
                    case ENTRY_INSERTED:
                    case ENTRY_UPDATED:
                        // just what we expected
                        if (fEntryWasInFront && !fOldValue)
                            {
                            oOldValue = evt.getOldValue();
                            fOldValue = true;
                            }
                        break;

                    case ENTRY_DELETED:
                        if (fEntryWasInFront && !fOldValue)
                            {
                            oOldValue = evt.getOldValue();
                            fOldValue = true;
                            }
                        status.setEntryInFront(false);
                        fEvicted = true;
                        break;
                    }
                }

            // no longer a miss (we just put it into the overflow map)
            if (mapMiss != null && mapMiss.containsKey(oKey))
                {
                mapMiss.remove(oKey);

                // old value is null and thus cannot be obtained
                fOldValue = true;
                }

            // back map processing (unless we already have the return value
            // and the entry is safely stored in the front)
            if (fEvicted && !fOldValue)
                {
                oOldValue = mapBack.put(oKey, oValue);
                }
            else if (fEvicted)
                {
                putOne(mapBack, oKey, oValue, true);
                status.setBackUpToDate(true);
                }
            else if (!fOldValue)
                {
                assert !fEntryWasInFront;
                oOldValue = mapBack.get(oKey);
                }
            }
        finally
            {
            endKeyProcess(oKey, status);
            }

        // update statistics
        m_stats.registerPut(ldtStart);

        return oOldValue;
        }

    /**
    * Copies all of the mappings from the specified map to this map. The
    * effect of this call is equivalent to that of calling {@link #put}
    * on this map once for each mapping in the passed map. The behavior of
    * this operation is unspecified if the passed map is modified while the
    * operation is in progress.
    *
    * @param map  the Map containing the key/value pairings to put into this
    *             Map
    */
    @Override
    public void putAll(Map map)
        {
        long ldtStart = getSafeTimeMillis();

        Object[] aoKey;
        if (isNullValuesAllowed())
            {
            aoKey = map.keySet().toArray();
            }
        else
            {
            // perhaps this is overkill, and it isn't even provably correct
            // since we can't prevent modifications from occurring to the
            // passed map on a different thread while we're processing here,
            // but some functionality counts on there being no null values if
            // the option for null values has not been enabled
            int i = 0, c = map.size();
            aoKey = new Object[c];
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry = (Map.Entry) iter.next();
                try
                    {
                    aoKey[i++] = entry.getKey();
                    }
                catch (ArrayIndexOutOfBoundsException e)
                    {
                    throw Base.ensureRuntimeException(e,
                            "a modification was detected in the passed map during iteration");
                    }
                if (entry.getValue() == null)
                    {
                    throw new IllegalArgumentException("null value is unsupported"
                            + "(putAll: " + map + ")");
                    }
                }
            if (i != c)
                {
                throw new IllegalStateException("a modification was"
                    + " detected in the passed map during iteration");
                }
            }

        Status[] astatus = beginBulkKeyProcess(aoKey);
        try
            {
            Map mapFront = getFrontMap();
            mapFront.putAll(map);

            // process the inserts/updates
            for (int i = 0, c = astatus.length; i < c; ++i)
                {
                Status status = astatus[i];

                // handle any events from the front
                MapEvent evt;
                synchronized (mapFront)
                    {
                    evt = status.closeProcessing();
                    }
                if (evt != null)
                    {
                    switch (evt.getId())
                        {
                        case ENTRY_INSERTED:
                        case ENTRY_UPDATED:
                            status.setEntryInFront(true);
                            status.setBackUpToDate(false);
                            break;

                        case ENTRY_DELETED:
                            Object oKey = evt.getKey();
                            putOne(getBackMap(), oKey, map.get(oKey), true);
                            status.setEntryInFront(false);
                            status.setBackUpToDate(true);
                            break;
                        }
                    }
                }

            // all the keys we just put in are no longer misses
            Map mapMiss = getMissCache();
            if (mapMiss != null)
                {
                mapMiss.keySet().removeAll(map.keySet());
                }
            }
        finally
            {
            endBulkKeyProcess(aoKey, astatus);
            }

        // update statistics
        m_stats.registerPuts(map.size(), ldtStart);
        }

    /**
    * Removes the mapping for this key from this map if present.
    * Expensive: updates both the underlying cache and the local cache.
    *
    * @param oKey key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *           if there was no mapping for key.  A <tt>null</tt> return can
    *           also indicate that the map previously associated <tt>null</tt>
    *           with the specified key, if the implementation supports
    *           <tt>null</tt> values.
    */
    @Override
    public Object remove(Object oKey)
        {
        Object oValue = null;
        Status status = beginKeyProcess(oKey);
        try
            {
            if (status.isEntryInFront())
                {
                Map mapFront = getFrontMap();
                mapFront.keySet().remove(oKey);

                MapEvent evt;
                synchronized (mapFront)
                    {
                    evt = status.closeProcessing();
                    }

                if (evt != null)
                    {
                    if (evt.getId() == ENTRY_DELETED)
                        {
                        oValue = evt.getOldValue();

                        // clean up the status so it is discardable
                        status.setEntryInFront(false);
                        status.setBackUpToDate(true);
                        }
                    else
                        {
                        processFrontEvent(status, evt);
                        }
                    }

                // remove the entry from the back map as well
                getBackMap().keySet().remove(oKey);
                }
            else
                {
                Map mapMiss = getMissCache();
                if (mapMiss == null || !mapMiss.containsKey(oKey))
                    {
                    Map     mapBack   = getBackMap();
                    boolean fContains = mapBack.containsKey(oKey);
                    if (fContains)
                        {
                        oValue = mapBack.remove(oKey);
                        }
                    else if (mapMiss != null)
                        {
                        mapMiss.put(oKey, null);
                        }
                    }
                }
            }
        finally
            {
            endKeyProcess(oKey, status);
            }

        return oValue;
        }

    /**
    * Returns the number of key-value mappings in this map.
    *
    * @return the number of key-value mappings in this map
    */
    @Override
    public int size()
        {
        try
            {
            Map mapBack = getBackMap();
            int c = mapBack.size();
            for (Iterator iter = getStatusMap().entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry  = (Map.Entry) iter.next();
                Status    status = (Status) entry.getValue();
                if (status.isValid() && status.isEntryInFront()
                        && !mapBack.containsKey(entry.getKey()))
                    {
                    ++c;
                    }
                }
            return c;
            }
        catch (ConcurrentModificationException e)
            {
            return super.size();
            }
        }


    // ----- AbstractKeyBasedMap methods ------------------------------------

    /**
    * Create an iterator over the keys in this Map.
    *
    * @return a new instance of an Iterator over the  keys in this Map
    */
    @Override
    protected Iterator iterateKeys()
        {
        return new KeyIterator();
        }

    /**
    * Removes the mapping for this key from this map if present. This method
    * exists to allow sub-classes to optmiize remove functionalitly for
    * situations in which the original value is not required.
    *
    * @param oKey key whose mapping is to be removed from the map
    *
    * @return true iff the Map changed as the result of this operation
    */
    @Override
    protected boolean removeBlind(Object oKey)
        {
        boolean fDidContain = false;
        Status status = beginKeyProcess(oKey);
        try
            {
            if (status.isEntryInFront())
                {
                Map mapFront = getFrontMap();
                mapFront.keySet().remove(oKey);

                MapEvent evt;
                synchronized (mapFront)
                    {
                    evt = status.closeProcessing();
                    }

                if (evt != null)
                    {
                    if (evt.getId() == ENTRY_DELETED)
                        {
                        // clean up the status so it is discardable
                        status.setEntryInFront(false);
                        status.setBackUpToDate(true);
                        }
                    else
                        {
                        processFrontEvent(status, evt);
                        }
                    }

                // remove the entry from the back map as well
                getBackMap().keySet().remove(oKey);

                fDidContain = true;
                }
            else
                {
                Map mapMiss = getMissCache();
                if (mapMiss == null || !mapMiss.containsKey(oKey))
                    {
                    fDidContain = getBackMap().keySet().remove(oKey);
                    if (!fDidContain && mapMiss != null)
                        {
                        mapMiss.put(oKey, null);
                        }
                    }
                }
            }
        finally
            {
            endKeyProcess(oKey, status);
            }

        return fDidContain;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Returns the front Map.
    * <p>
    * <b>Warning: Direct modifications of the returned map may cause
    * unpredictable behavior of the Overflow Map.</b>
    *
    * @return the front Map
    */
    public ObservableMap getFrontMap()
        {
        return m_mapFront;
        }

    /**
    * Returns the back Map.
    * <p>
    * <b>Warning: Direct modifications of the returned map may cause
    * unpredictable behavior of the Overflow Map.</b>
    *
    * @return the back Map
    */
    public Map getBackMap()
        {
        return m_mapBack;
        }

    /**
    * Returns the optional miss cache.
    * <p>
    * <b>Warning: Direct modifications of the returned map may cause
    * unpredictable behavior of the Overflow Map.</b>
    *
    * @return the miss cache, or null if there is no miss cache
    */
    public Map getMissCache()
        {
        return m_mapMiss;
        }

     /**
      * Set Miss Cache.
      *
      * Note: do not use other than in OverflowScheme#realizeMap(...)
      *
      * @param mapMiss miss cache.
      */
    public void setMissCache(Map mapMiss)
        {
        m_mapMiss = mapMiss;
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
    * Obtain the Gate for managing key-level and Collection-level
    * operations against the Map, versus Map-level operations themselves.
    *
    * @return the Gate for the OverflowMap
    */
    protected Gate getGate()
        {
        return m_gate;
        }

    /**
    * Obtain the Map of Status objects for entries managed by this
    * Overflow Map. There will be a Status object for each entry in
    * the front map, and a Status object for each key that has a current
    * operation underway.
    *
    * @return the Map of Status objects
    */
    protected Map getStatusMap()
        {
        return m_mapStatus;
        }

    /**
    * Obtain the List of keys that may have deferred events.
    *
    * @return the List of keys of deferred-event Status objects
    */
    protected List getDeferredList()
        {
        return m_listDeferred;
        }

    /**
    * Determine if null values are permitted. By default, they are not.
    *
    * @return true iff null values are permitted
    */
    public boolean isNullValuesAllowed()
        {
        return m_fNullValuesAllowed;
        }

    /**
    * Specify whether null values are permitted.
    *
    * @param fAllowNulls  pass true to allow null values; false to
    *        disallow
    */
    public synchronized void setNullValuesAllowed(boolean fAllowNulls)
        {
        beginMapProcess();
        try
            {
            synchronized (this)
                {
                if (fAllowNulls != isNullValuesAllowed())
                    {
                    if (!fAllowNulls && !getStatusMap().isEmpty())
                        {
                        azzert(!values().contains(null),
                               "NullValuesAllowed cannot be set to false because"
                               + " the SimpleOverflowMap contains null values");
                        }

                    m_fNullValuesAllowed = fAllowNulls;
                    }
                }
            }
        finally
            {
            endMapProcess();
            }
        }

    /**
    * Determine if the front Map is more efficiently updated using putAll.
    * (The use of <tt>putAll</tt> instead of <tt>put</tt> is called put-blind
    * because it does not return a value.)
    *
    * @return true iff the use of the <tt>put</tt> method should be avoided
    *         when updating the front Map, and <tt>putAll</tt> should be used
    *         instead
    */
    public boolean isFrontPutBlind()
        {
        return m_fUseFrontPutAll;
        }

    /**
    * Specify whether the front Map is more efficiently updated using putAll.
    *
    * @param fUseFrontPutAll  pass true to allow null values; false to
    *        disallow
    */
    public void setFrontPutBlind(boolean fUseFrontPutAll)
        {
        m_fUseFrontPutAll = fUseFrontPutAll;
        }


    // ----- front map listener ---------------------------------------------

    /**
    * Get the MapListener for the front map.
    *
    * @return the MapListener for the front map
    */
    protected MapListener getFrontMapListener()
        {
        return m_listenerFront;
        }

    /**
    * Specify the MapListener for the front map.
    * <p>
    * The caller is required to manage all of the thread concurrency issues
    * associated with modifying the listener.
    *
    * @param listener  the MapListener for the front map
    */
    protected void setFrontMapListener(MapListener listener)
        {
        ObservableMap mapFront = getFrontMap();
        azzert(mapFront != null);

        MapListener listenerOld = m_listenerFront;
        if (listener != listenerOld)
            {
            if (listenerOld != null)
                {
                mapFront.removeMapListener(listenerOld);
                m_listenerFront = null;
                }

            if (listener != null)
                {
                mapFront.addMapListener(listener);
                m_listenerFront = listener;
                }
            }
        }

    /**
    * Factory pattern: Front Map Listener.
    *
    * @return a new instance of the listener for the front map
    */
    protected MapListener instantiateFrontMapListener()
        {
        return new FrontMapListener();
        }

    /**
    * A listener for the front map that moves evictions to the back map.
    */
    protected class FrontMapListener
            extends MultiplexingMapListener
        {
        /**
        * {@inheritDoc}
        */
        @Override
        protected void onMapEvent(MapEvent evt)
            {
            onFrontEvent(evt);
            }
        }

    /**
    * Either handle an event by turning it over to another thread that is
    * processing the key specified by the event, or take responsibility on
    * this thread for deferring the event and registering its immediate
    * side-effects.
    *
    * @param evt  the event
    */
    protected void onFrontEvent(MapEvent evt)
        {
        // an event will always be a re-entrant condition unless one of the
        // two simple contracts are broken:
        // 1) daemon threads evicting data
        // 2) someone modifying the front map directly
        Gate       gate       = getGate();
        boolean    fReentrant = gate.isEnteredByCurrentThread();
        if (!fReentrant)
            {
            // could issue a warning here, but Coherence internally uses
            // OverflowMap for a graveyard implementation and directly evicts
            // from the front map
            gate.enter(-1);
            }

        try
            {
            Map    mapStatus = getStatusMap();
            Object oKey      = evt.getKey();
            Status status;
            while (true)
                {
                synchronized (mapStatus)
                    {
                    status = (Status) mapStatus.get(oKey);
                    if (status == null)
                        {
                        status = instantiateStatus();
                        mapStatus.put(oKey, status);
                        }
                    } // must exit sync for Map before entering sync for Status

                synchronized (status)
                    {
                    // verify that this Status is *the* Status for that key
                    if (status.isValid())
                        {
                        // determine if the event may be deferred, which
                        // implies that we need to queue the status to be
                        // processed
                        if (status.registerFrontEvent(evt))
                            {
                            getDeferredList().add(oKey);
                            }
                        return;
                        }
                    }
                }
            }
        finally
            {
            if (!fReentrant)
                {
                gate.exit();
                }
            }
        }

    /**
    * Process an event. The Status must already be owned by this thread and
    * in the processing or committing state.
    *
    * @param status  the status
    * @param evt     the event to process; may be null
    */
    protected void processFrontEvent(Status status, MapEvent evt)
        {
        assert status.isOwnedByCurrentThread()
               && (status.isProcessing() || status.isCommitting());

        if (evt != null)
            {
            // it is necessary to synchronize on the status at this point to
            // prevent the registration of further events against the same entry
            synchronized (status)
                {
                switch (evt.getId())
                    {
                    case ENTRY_INSERTED:
                        status.setEntryInFront(true);
                        status.setBackUpToDate(false);

                        // remove from miss cache; this is only necessary if
                        // front map is being manipulated directly
                        Map mapMiss = getMissCache();
                        if (mapMiss != null)
                            {
                            mapMiss.remove(evt.getKey());
                            }
                        break;

                    case ENTRY_UPDATED:
                        status.setEntryInFront(true);
                        status.setBackUpToDate(false);
                        break;

                    case ENTRY_DELETED:
                        status.setEntryInFront(false);
                        if (!status.isBackUpToDate())
                            {
                            getBackMap().put(evt.getKey(), evt.getOldValue());
                            status.setBackUpToDate(true);
                            }
                        break;
                    }
                }
            }
        }


    // ----- process registration and management ----------------------------

    /**
    * Block until a key is available for processing, and return the Status
    * object for that key. The caller is then expected to perform its
    * processing, during which time events against the Map Entry may occur.
    * Note that those events can occur on threads other than the current
    * thread; when this current thread can no longer handle events from other
    * threads, it must call {@link Status#closeProcessing()} and then perform
    * its final adjustments to the Status data structure, handling any events
    * that were queued in the meantime on the Status. After completing the
    * processing during this "quiet period" in which all other threads are
    * prevented from accessing this entry or handling events for this entry,
    * then the caller must call {@link #endKeyProcess} passing the Status
    * object returned from this method.
    *
    * @param oKey  the key to process
    *
    * @return the Status object for the specified key
    */
    protected Status beginKeyProcess(Object oKey)
        {
        // check for re-entrancy
        Gate    gate = getGate();
        boolean fReentrant = gate.isEnteredByCurrentThread();
        gate.enter(-1);

        // handle up to one deferred event for any key (other than the key
        // we've been asked to process)
        if (!fReentrant)
            {
            // check for and process deferred events from the front Map
            processDeferredEvents();
            }

        // obtain the key that we've been asked to process
        while (true)
            {
            Status status;

            Map mapStatus = getStatusMap();
            synchronized (mapStatus)
                {
                status = (Status) mapStatus.get(oKey);
                if (status == null)
                    {
                    status = instantiateStatus();
                    mapStatus.put(oKey, status);
                    }
                } // must exit sync for Map before entering sync for Status

            synchronized (status)
                {
                // verify that this Status is *the* Status for that key
                if (status.isValid())
                    {
                    MapEvent evtDeferred = status.waitForAvailable();
                    if (evtDeferred != null)
                        {
                        processFrontEvent(status, evtDeferred);
                        }
                    return status;
                    }
                }
            }
        }

    /**
    * Finish the processing of a single key.
    *
    * @param oKey    the key
    * @param status  the Status object returned from the call to
    *                {@link #beginKeyProcess}
    */
    protected void endKeyProcess(Object oKey, Status status)
        {
        Map mapStatus = getStatusMap();
        synchronized (mapStatus)
            {
            synchronized (status)
                {
                if (status.isProcessing())
                    {
                    processFrontEvent(status, status.closeProcessing());
                    }

                if (status.commitAndMaybeInvalidate())
                    {
                    mapStatus.remove(oKey);
                    }
                }
            }

        getGate().exit();
        }

    /**
    * Begin key-level procecessing for any number of keys.
    *
    * @param aoKey  an array of zero or more keys; note that this array may
    *               be re-ordered by this method
    *
    * @return an array of Status objects corresponding to the passed keys
    *
    * @see #beginKeyProcess(Object)
    */
    protected Status[] beginBulkKeyProcess(Object[] aoKey)
        {
        int      cKeys   = aoKey.length;
        Status[] aStatus = new Status[cKeys];

        switch (cKeys)
            {
            case 0:
                break;

            case 1:
                aStatus[0] = beginKeyProcess(aoKey[0]);
                break;

            default:
                {
                getGate().enter(-1);

                // check for and process deferred events from the front Map
                processDeferredEvents();

                // obtain the ownership of the entire set of keys passed in
                Map     mapStatus = getStatusMap();
                boolean fDone     = false;
                int     cIters    = 0;
                while (!fDone)
                    {
                    boolean fSuccess  = true;
                    int     cReserved = 0;
                    while (fSuccess && cReserved < cKeys)
                        {
                        // reserve one
                        Object oKey   = aoKey[cReserved];
                        Status status;
                        synchronized (mapStatus)
                            {
                            status = (Status) mapStatus.get(oKey);
                            if (status == null)
                                {
                                status = instantiateStatus();
                                mapStatus.put(oKey, status);
                                }
                            } // must exit sync for Map before entering sync for Status

                        synchronized (status)
                            {
                            // verify that this Status is *the* Status for that key
                            if (status.isValid())
                                {
                                if (status.requestReservation())
                                    {
                                    aStatus[cReserved++] = status;
                                    }
                                else
                                    {
                                    fSuccess = false;
                                    }
                                }
                            else
                                {
                                fSuccess = false;
                                }
                            }
                        }

                    if (fSuccess)
                        {
                        // use the reservations
                        for (int i = 0; i < cKeys; ++i)
                            {
                            MapEvent evtDeferred = aStatus[i].useReservation();
                            if (evtDeferred != null)
                                {
                                processFrontEvent(aStatus[i], evtDeferred);
                                }
                            }
                        fDone = true;
                        }
                    else
                        {
                        // cancel reservations
                        synchronized (mapStatus)
                            {
                            for (int i = 0; i < cReserved; ++i)
                                {
                                Status status = aStatus[i];
                                synchronized (status)
                                    {
                                    if (status.commitAndMaybeInvalidate())
                                        {
                                        mapStatus.remove(aoKey[i]);
                                        }
                                    }
                                aStatus[i] = null;
                                }
                            }

                        switch (cIters++)
                            {
                            case 0:
                                {
                                // re-order the keys to minimize deadlock
                                boolean fComparable = true;
                                for (int i = 0; i < cKeys; ++i)
                                    {
                                    if (!(aoKey[i] instanceof Comparable))
                                        {
                                        fComparable = false;
                                        }
                                    }

                                if (fComparable)
                                    {
                                    Arrays.sort(aoKey);
                                    }
                                else
                                    {
                                    Arrays.sort(aoKey, HashcodeComparator.INSTANCE);
                                    }
                                }
                                break;

                            case 1:
                                Thread.yield();
                                break;

                            default:
                                try
                                    {
                                    Blocking.sleep(cIters);
                                    }
                                catch (InterruptedException e)
                                    {
                                    Thread.interrupted();

                                    // at this point, this overflow map
                                    // hasn't done anything except enter the
                                    // gate, so it is possible to abort out
                                    // of this processing
                                    getGate().exit();
                                    throw ensureRuntimeException(e);
                                    }
                                break;
                            }
                        }
                    }
                }
                break;
            }

        return aStatus;
        }

    /**
    * Finish the processing of any number of keys.
    *
    * @param aoKey    the same array of keys as were passed to
    *                 {@link #beginBulkKeyProcess}
    * @param aStatus  the array of Status objects returned from the call to
    *                 {@link #beginBulkKeyProcess}
    */
    protected void endBulkKeyProcess(Object[] aoKey, Status[] aStatus)
        {
        int cStatus = aStatus.length;
        switch (cStatus)
            {
            case 0:
                break;

            case 1:
                endKeyProcess(aoKey[0], aStatus[0]);
                break;

            default:
                {
                Map mapStatus = getStatusMap();
                synchronized (mapStatus)
                    {
                    for (int i = 0; i < cStatus; ++i)
                        {
                        Status status = aStatus[i];
                        synchronized (status)
                            {
                            if (status.isProcessing())
                                {
                                processFrontEvent(status, status.closeProcessing());
                                }

                            if (status.commitAndMaybeInvalidate())
                                {
                                mapStatus.remove(aoKey[i]);
                                }
                            }
                        }
                    }

                getGate().exit();
                }
                break;
            }
        }

    /**
    * Block until this thread has exclusive access to perform operations
    * against the OverflowMap.
    */
    protected void beginMapProcess()
        {
        Gate gate = getGate();
        gate.close(-1);
        gate.enter(-1); // by entering, we can later determine re-entrancy

        // take ownership of all of the Status objects
        Map mapStatus = getStatusMap();
        synchronized (mapStatus)
            {
            for (Iterator iter = mapStatus.values().iterator(); iter.hasNext(); )
                {
                Status status = (Status) iter.next();
                synchronized (status)
                    {
                    // since no one else is processing, the status should be
                    // available
                    assert status.isAvailable() || status.isOwnedByCurrentThread();

                    // so this should not block
                    MapEvent evtDeferred = status.waitForAvailable();
                    if (evtDeferred != null)
                        {
                        processFrontEvent(status, evtDeferred);
                        }
                    }
                }
            }
        }

    /**
    * Release exclusive access for the OverflowMap.
    */
    protected void endMapProcess()
        {
        // commit all of the Status objects
        Map mapStatus = getStatusMap();
        synchronized (mapStatus)
            {
            for (Iterator iter = mapStatus.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry  = (Map.Entry) iter.next();
                Status    status = (Status) entry.getValue();
                synchronized (status)
                    {
                    if (status.isProcessing())
                        {
                        processFrontEvent(status, status.closeProcessing());
                        }

                    if (status.commitAndMaybeInvalidate())
                        {
                        mapStatus.remove(entry.getKey());
                        }
                    }
                }
            }

        Gate gate = getGate();
        gate.exit();
        gate.open();
        }

    /**
    * Process deferred events, if there are any. This implementation
    * processes only the first deferred event that it encounters.
    */
    protected void processDeferredEvents()
        {
        // process a deferred event, if there is one
        List listDeferred = getDeferredList();
        if (!listDeferred.isEmpty())
            {
            // try to process at least ~1% of the pending events, but not
            // less than 10 events and not more than 100 events
            int cTarget    = Math.max(10, Math.min(100, listDeferred.size() >>> 7));
            int cProcessed = 0;
            Map mapStatus  = getStatusMap();
            do
                {
                Object oKey = null;
                try
                    {
                    oKey = listDeferred.remove(0);
                    }
                catch (Exception e) {}

                if (oKey != null)
                    {
                    Status status = (Status) mapStatus.get(oKey);
                    if (status != null)
                        {
                        boolean fOwned = false;
                        synchronized (status)
                            {
                            // verify that this Status is *the* Status for that key
                            if (status.isValid())
                                {
                                if (status.isAvailable())
                                    {
                                    MapEvent evtDeferred = status.waitForAvailable();
                                    fOwned = true;
                                    if (evtDeferred != null)
                                        {
                                        processFrontEvent(status, evtDeferred);
                                        }
                                    }
                                }
                            }

                        if (fOwned)
                            {
                            MapEvent evtJustHappened = status.closeProcessing();
                            if (evtJustHappened != null)
                                {
                                processFrontEvent(status, evtJustHappened);
                                }

                            synchronized (mapStatus)
                                {
                                synchronized (status)
                                    {
                                    if (status.commitAndMaybeInvalidate())
                                        {
                                        mapStatus.remove(oKey);
                                        }
                                    }
                                }

                            ++cProcessed;
                            }
                        else
                            {
                            // not available; defer it
                            listDeferred.add(oKey);
                            }
                        }
                    }
                }
            while (!listDeferred.isEmpty() && cProcessed < cTarget);
            }
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Check the passed collection for nulls, and fail if it contains any.
    *
    * @param collection  a Collection
    * @param sAssert     the human-readable description of the error if any
    *                    nulls are found in the passed Collection
    */
    protected static void verifyNoNulls(Collection collection, String sAssert)
        {
        boolean fHasNull = false;
        try
            {
            fHasNull = collection.contains(null);
            }
        catch (RuntimeException e) {}

        azzert(!fHasNull, sAssert);
        }

    /**
    * Helper to put a value into a map using either the <tt>put</tt> or
    * <tt>putAll</tt> method.
    *
    * @param map        the Map to put into
    * @param oKey       the key to put
    * @param oValue     the value to put
    * @param fPutBlind  true to use the putBlind optimization
    */
    protected static void putOne(Map map, Object oKey, Object oValue, boolean fPutBlind)
        {
        try
            {
            if (fPutBlind)
                {
                map.putAll(Collections.singletonMap(oKey, oValue));
                }
            else
                {
                map.put(oKey, oValue);
                }
            }
        catch (NullPointerException e)
            {
            out("null pointer exception occurred during putOne()"
                + "\nMap=" + map
                + "\nKey=" + oKey
                + "\nValue=" + oValue
                + "\nPutBlind=" + fPutBlind
                + "\nException=" + e);
            throw e;
            }
        }

    /**
    * Issue a one-time warning that events are being received in an order
    * than cannot be explained by normal operation according to the
    * contracts expected by this OverflowMap.
    *
    * @param evtOld  the previous (potentially amalgamated) event
    * @param evtNew  the new event
    */
    protected static void warnEventSequence(MapEvent evtOld, MapEvent evtNew)
        {
        if (!s_fWarnedEventSequence)
            {
            s_fWarnedEventSequence = true;

            log("Overflow Map has detected"
                + " an illegal event sequence:"
                + "\nEvent 1: " + evtOld
                + "\nEvent 2: " + evtNew
                + "\nThis warning will not be repeated."
                + " Stack trace follows:\n" + getStackTrace());
            }
        }


    // ----- inner class: Status --------------------------------------------

    /**
    * Factory method: Instantiate a Status object.
    *
    * @return a new Status object
    */
    protected Status instantiateStatus()
        {
        return new Status();
        }

    /**
    * The Status object is used to manage concurrency at the key level for
    * the key-level operations against the Map, to track all the items in
    * the front Map, to manage the state transition for operations occurring
    * against the Map, and to coordinate events across multiple threads.
    */
    protected static class Status
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a Status object for a specific key.
        */
        public Status()
            {
            }

        // ----- accessors ----------------------------------------------

        /**
        * Determine the enumerated status of the Status object. This value
        * is intended for internal and debugging use only, and should have
        * no meaning to any external consumer.
        *
        * @return a number corresponding to the enumeration of states as
        *         represented by the STATUS_*
        */
        protected int getStatus()
            {
            return extractState(STATE_MASK_STATUS);
            }

        /**
        * Determine the enumerated state of the Status object. This value
        * is intended for internal and debugging use only, and should have
        * no meaning to any external consumer.
        *
        * @param nStatus  a number corresponding to one of the enumeration
        *                 represented by the STATUS_* constants
        */
        protected void setStatus(int nStatus)
            {
            updateState(STATE_MASK_STATUS, nStatus);
            }

        /**
        * Determine if the Status object is valid. A Status object can be
        * discarded (no longer used), in which case it will not be valid.
        *
        * @return true iff the Status object is still valid
        */
        public boolean isValid()
            {
            return getStatus() != STATUS_INVALIDATED;
            }

        /**
        * Determine if the Status object is available. A Status object is
        * available if it is valid and no thread is currently processing the
        * entry for which this Status object exists.
        *
        * @return true iff the Status object is available
        */
        public boolean isAvailable()
            {
            return getStatus() == STATUS_AVAILABLE;
            }

        /**
        * Determine if the Status object is reserved.
        *
        * @return true iff the Status object is available
        */
        public boolean isReserved()
            {
            return getStatus() == STATUS_RESERVED;
            }

        /**
        * Determine if the Status object represents an Entry that is
        * currently being processed.
        *
        * @return true iff the entry represented by this Status object is
        *         being processed
        */
        public boolean isProcessing()
            {
            return getStatus() == STATUS_PROCESSING;
            }

        /**
        * Determine if the Status object represents an Entry that has been
        * processed and the results of that processing are being committed.
        * The "committing" status implies that no other thread is allowed to
        * do <b>anything</b> related to the entry that this Status
        * represents.
        *
        * @return true iff the entry represented by this Status object is
        *         being committed
        */
        public boolean isCommitting()
            {
            return getStatus() == STATUS_COMMITTING;
            }

        /**
        * Determine the thread that owns the Status object, if the Status
        * object is processing or committing.
        *
        * @return the owning thread, or null
        */
        public Thread getOwnerThread()
            {
            return m_threadOwner;
            }

        /**
        * Specify the thread that owns the Status object. For internal use
        * only.
        *
        * @param thread  the owning thread, or null
        */
        protected void setOwnerThread(Thread thread)
            {
            m_threadOwner = thread;
            }

        /**
        * Determine if the current thread owns this Status object.
        *
        * @return true iff the current thread owns this Status object
        */
        public boolean isOwnedByCurrentThread()
            {
            Thread thread = getOwnerThread();
            return thread != null && thread == Thread.currentThread();
            }

        /**
        * Determine if the entry for which this Status exists is present in
        * the front map.
        *
        * @return true iff  the entry is stored in the front map
        */
        public boolean isEntryInFront()
            {
            return extractFlag(STATE_MASK_FRONT);
            }

        /**
        * Specify whether the entry for which this Status exists is present
        * in the front map.
        *
        * @param fEntryInFront  pass true if the entry is stored in the front
        *                       map, false if not
        */
        public void setEntryInFront(boolean fEntryInFront)
            {
            updateFlag(STATE_MASK_FRONT, fEntryInFront);
            }

        /**
        * Determine if the entry for which this Status exists has the same
        * value in the front Map as in the back Map.
        *
        * @return true iff the value exists in the back Map, and the value in
        *         the front Map is the same as the value in the back Map
        */
        public boolean isBackUpToDate()
            {
            return extractFlag(STATE_MASK_INSYNC);
            }

        /**
        * Specify that the value stored in the back Map is known to be up to
        * date (not needing to be written to the back if evicted from the
        * front).
        *
        * @param fUpToDate  whether the stored value is up to date
        */
        public void setBackUpToDate(boolean fUpToDate)
            {
            updateFlag(STATE_MASK_INSYNC, fUpToDate);
            }

        /**
        * For internal use only, return the current event from the front
        * Map. All handling of synchronization etc. is the responsibility of
        * the sub-class.
        *
        * @return the cummulative front Map event for the Entry represented
        *         by this Status object, or null if there were no events
        */
        protected MapEvent getFrontEvent()
            {
            return m_evtFront;
            }

        /**
        * For internal use only, store the current event from the front Map.
        * All handling of synchronization etc. is the responsibility of the
        * sub-class.
        *
        * @param evt  the cummulative front Map event for the Entry
        *             represented by this Status object, or null to clear
        *             the event
        */
        protected void setFrontEvent(MapEvent evt)
            {
            m_evtFront = evt;
            }

        /**
        * Determine if an event has occurred against the Entry for which this
        * Status exists.
        *
        * @return true iff an event is held by the Status
        */
        public boolean hasEvent()
            {
            return getFrontEvent() != null;
            }

        /**
        * Obtain the most recent front Map event that has occurred against
        * the Entry for which this Status exists.
        *
        * @return the cummulative front Map event for the Entry represented
        *         by this Status object, or null if there were no events
        */
        public synchronized MapEvent takeEvent()
            {
            assert isProcessing() || isCommitting();

            MapEvent evt = getFrontEvent();
            if (evt != null)
                {
                setFrontEvent(null);
                }

            return evt;
            }

        /**
        * Determine if this Status object can be discarded.
        * <p>
        * This is an internal method.
        *
        * @return true iff this Status object can be discarded
        */
        protected boolean isDiscardable()
            {
            // this is the same as isAvailable() && !isEntryInFront()
            // && isBackUpToDate() && !hasEvent();
            return extractState(STATE_MASK_RETAIN) == STATE_VALUE_RETAIN
                    && !hasEvent();
            }

        /**
        * Assemble a human-readable description.
        *
        * @return a description of this Status object
        */
        public String getDescription()
            {
            return "Valid=" + isValid()
                    + ", Available=" + isAvailable()
                    + ", Processing=" + isProcessing()
                    + ", Committing=" + isCommitting()
                    + ", OwnerThread=" + getOwnerThread()
                    + ", OwnedByCurrentThread=" + isOwnedByCurrentThread()
                    + ", EntryInFront=" + isEntryInFront()
                    + ", BackUpToDate=" + isBackUpToDate()
                    + ", hasEvent=" + hasEvent()
                    + ", FrontEvent=" + getFrontEvent()
                    + ", Discardable=" + isDiscardable();
            }

        // ----- Object methods -----------------------------------------

        /**
        * Returns a string representation of the object.
        *
        * @return a string representation of the object
        */
        @Override
        public String toString()
            {
            return "Status{" + getDescription() + '}';
            }

        // ----- status management --------------------------------------

        /**
        * Wait for the Entry that this Status represents to become available.
        * Once it becomes available, the current thread will automatically
        * become the owner and the status will be changed to "processing".
        * <p>
        * This is an internal method. It requires the caller to have
        * synchronized on the Status object before calling this method.
        *
        * @return whatever event was deferred for this Status which the
        *         caller must handle
        */
        protected MapEvent waitForAvailable()
            {
            boolean fRegisteredWaiting = false;

            try
                {
                while (!isAvailable())
                    {
                    assert isValid();

                    // check for re-entrancy
                    if (isOwnedByCurrentThread())
                        {
                        throw new IllegalStateException("Re-entrancy is not supported"
                            + " (State=" + getState() + ")");
                        }

                    if (!fRegisteredWaiting)
                        {
                        int cWaiting = m_cWaiting & 0x000000FF;
                        if (cWaiting == 0x000000FF)
                            {
                            throw new IllegalStateException(
                                "Exceeded maximum number of waiting threads"
                                + " (Status=" + getStatus() + ")");
                            }
                        m_cWaiting = (byte) (cWaiting + 1);
                        fRegisteredWaiting = true;
                        }

                    Blocking.wait(this);
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }
            finally
                {
                if (fRegisteredWaiting)
                    {
                    --m_cWaiting;
                    }
                }

            setStatus(STATUS_PROCESSING);
            setOwnerThread(Thread.currentThread());

            return takeEvent();
            }

        /**
        * Attempt to reserve the Status by marking it as committing if it is
        * available. If successful, the caller must subsequently either call
        * {@link #useReservation()} or {@link #commitAndMaybeInvalidate()}
        * (to cancel the reservation).
        * <p>
        * This is an internal method.
        *
        * @return true if the reservation was made successfull, false if
        *         another thread already owns this Status object
        */
        protected synchronized boolean requestReservation()
            {
            boolean fSuccess = false;

            if (isAvailable())
                {
                setStatus(STATUS_RESERVED);
                setOwnerThread(Thread.currentThread());
                fSuccess = true;
                }
            else if (isOwnedByCurrentThread())
                {
                // re-entrancy is not permitted
                throw new IllegalStateException("Re-entrancy is not supported"
                    + " (State=" + getState() + ")");
                }

            return fSuccess;
            }

        /**
        * Wait for the Entry that this Status represents to no longer be
        * reserved.
        * <p>
        * This is an internal method. It requires the caller to have
        * synchronized on the Status object before calling this method.
        */
        protected void waitForReservationDecision()
            {
            boolean fRegisteredWaiting = false;

            try
                {
                while (isReserved())
                    {
                    assert isValid();

                    if (!fRegisteredWaiting)
                        {
                        int cWaiting = m_cWaiting & 0x000000FF;
                        if (cWaiting == 0x000000FF)
                            {
                            throw new IllegalStateException(
                                "Exceeded maximum number of waiting threads"
                                + " (Status=" + getStatus() + ")");
                            }
                        m_cWaiting = (byte) (cWaiting + 1);
                        fRegisteredWaiting = true;
                        }

                    Blocking.wait(this);
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }
            finally
                {
                if (fRegisteredWaiting)
                    {
                    --m_cWaiting;
                    }
                }
            }

        /**
        * After having successfully made a reservation, this method completes
        * the reservation process by setting the status to processing for the
        * thread that made the reservation.
        * <p>
        * This is an internal method.
        *
        * @return whatever event was deferred for this Status which the
        *         caller must handle
        */
        protected synchronized MapEvent useReservation()
            {
            assert isReserved();
            assert getOwnerThread() == Thread.currentThread();

            setStatus(STATUS_PROCESSING);

            if (m_cWaiting != 0)
                {
                // must wake up any thread that was waiting on the
                // reservation status to change
                notifyAll();
                }

            return takeEvent();
            }

        /**
        * Finish the processing of the entry for which this Status exists and
        * proceed to the commit phase. The act of closing processing collects
        * any side-effects to the corresponding front Map Entry (either from
        * this or other threads) as a single event.
        *
        * @return all events that have occurred on this or other threads for
        *         the front Map Entry represented by this Status object while
        *         this Status object was "processing", or null if there were
        *         no events
        */
        public synchronized MapEvent closeProcessing()
            {
            assert isProcessing();
            assert getOwnerThread() == Thread.currentThread();

            setStatus(STATUS_COMMITTING);

            return takeEvent();
            }

        /**
        * Finish the commit for the entry for which this Status exists. If
        * there are any threads waiting on the entry for which this Status
        * exists, one will be notified that the Status is now available.
        * If the entry for which this Status object exists is contained in
        * the front Map, then it will proceed to the available phase.
        * Otherwise, the Status will be invalidated and discarded from the
        * Status registry.
        * <p>
        * This is an internal method. It requires the caller to have first
        * synchronized on the registry (Map) that contains the Status objects
        * and then to have synchronized on this Status object itself, in that
        * explicit order. Failure to follow this rule will result in deadlock
        * and/or exceptional conditions.
        *
        * @return true iff this Status object has invalidated itself
        */
        protected boolean commitAndMaybeInvalidate()
            {
            assert isReserved() || isCommitting();
            assert isOwnedByCurrentThread();

            boolean fInvalidated = false;
            boolean fWasReserved = isReserved();
            setStatus(STATUS_AVAILABLE);
            if (m_cWaiting != 0)
                {
                if (fWasReserved)
                    {
                    // notify any thread that is waiting on the reservation
                    // status to change
                    notifyAll();
                    }
                else
                    {
                    // notify the next thread in line
                    notify();
                    }
                }
            else if (isDiscardable())
                {
                setStatus(STATUS_INVALIDATED);
                fInvalidated = true;
                }
            setOwnerThread(null);

            return fInvalidated;
            }

        // ----- event management ---------------------------------------

        /**
        * Register a MapEvent that has been raised by the front Map against
        * the same key for which this Status object exists. If an event has
        * previously been registered, the previous and new event are merged
        * into a single merged event that incorporates the data from both
        * events.
        * <p>
        * For truly predictable behavior, this requires that the front Map
        * implementation be synchronized during the raising of events, such
        * that an event will only be raised from the front Map on one thread
        * at a time. The event listener (for the overflow map) must then
        * synchronize on this Status object (to verify that it is indeed
        * valid before registering the event) and the register the event
        * while holding that synchronization. If the registration returns
        * a deferred indicator, then the
        *
        * @param evt  the event that has occurred against an entry in the
        *             front Map with the same key which this Status object
        *             represents
        *
        * @return true iff the event processing has been deferred, implying
        *         that this Status object should be registered in a list of
        *         Status objects that have events that need to be handled
        */
        public synchronized boolean registerFrontEvent(MapEvent evt)
            {
            assert isValid();

            MapEvent evtOld = getFrontEvent();
            if (evtOld != null)
                {
                evt = mergeEvents(evtOld, evt);
                }
            setFrontEvent(evt);

            // not deferred if the Status is owned and in the processing
            // stage (since the event will be handled by the owning thread at
            // the end of the processing stage)
            return !isProcessing();
            }

        /**
        * Merge two events that have been raised in sequence from a given
        * map.
        *
        * @param evtOld  the first event
        * @param evtNew  the second event
        *
        * @return the merged event
        */
        protected MapEvent mergeEvents(MapEvent evtOld, MapEvent evtNew)
            {
            MapEvent evtResult   = evtNew;

            boolean  fCreate     = true;
            int      nId         = evtNew.getId();
            switch ((evtOld.getId() << 2) | nId)
                {
                case (MapEvent.ENTRY_INSERTED << 2) |  MapEvent.ENTRY_UPDATED:
                    nId = ENTRY_INSERTED;
                    break;

                case (MapEvent.ENTRY_INSERTED << 2) |  MapEvent.ENTRY_DELETED:
                    // while these technically cancel each other out, it
                    // could lead to a loss of data if an evict of a key
                    // from another thread occurs during a put of that
                    // object that inserted that key, for example
                    fCreate = false;
                    if (!(evtNew instanceof CacheEvent && ((CacheEvent) evtNew).isSynthetic()))
                        {
                        evtResult = null;
                        }
                    break;

                case (MapEvent.ENTRY_UPDATED << 2) |  MapEvent.ENTRY_UPDATED:
                    break;

                case (MapEvent.ENTRY_UPDATED << 2) |  MapEvent.ENTRY_DELETED:
                    break;

                case (MapEvent.ENTRY_DELETED << 2) |  MapEvent.ENTRY_INSERTED:
                    // delete and insert cancel each other out and just
                    // become an update
                    nId = ENTRY_UPDATED;
                    break;

                case (MapEvent.ENTRY_INSERTED << 2) |  MapEvent.ENTRY_INSERTED:
                case (MapEvent.ENTRY_UPDATED << 2) |  MapEvent.ENTRY_INSERTED:
                case (MapEvent.ENTRY_DELETED << 2) |  MapEvent.ENTRY_UPDATED:
                case (MapEvent.ENTRY_DELETED << 2) |  MapEvent.ENTRY_DELETED:
                default:
                    // this is very strange ... and indicates a sequencing
                    // problem caused by a lack of synchronization within the
                    // map from which the event originated
                    warnEventSequence(evtOld, evtNew);

                    // since the result is already non-deterministic,
                    // just use the newer event (there is no correct
                    // answer)
                    fCreate = false;
                    break;
                }

            if (fCreate)
                {
                ObservableMap map         = evtNew.getMap();
                Object        oKey        = evtNew.getKey();
                Object        oValueOld   = evtOld.getOldValue();
                Object        oValueNew   = evtNew.getNewValue();
                boolean       fSynthetic  = evtNew instanceof CacheEvent
                                            && ((CacheEvent) evtNew).isSynthetic();
                boolean       fPriming    = evtNew instanceof CacheEvent
                                            && ((CacheEvent) evtNew).isPriming();
                evtResult = new CacheEvent(map, nId, oKey,
                                           oValueOld, oValueNew, fSynthetic, fPriming);
                }

            return evtResult;
            }

        // ----- internal accessors ------------------------------------

        /**
        * Determine the state of the Status object. This value is intended
        * for internal and debugging use only, and should have no meaning to
        * any external consumer.
        *
        * @return the bit-packed state of the Status object
        */
        protected int getState()
            {
            return m_nState;
            }

        /**
        * Specify the state of the Status object. This value is intended
        * for internal and debugging use only, and should have no meaning to
        * any external consumer.
        *
        * @param nState  the new bit-packed state for the Status object
        */
        protected void setState(int nState)
            {
            m_nState = (byte) nState;
            }

        /**
        * Extract a particular masked value from the state of the Status
        * object.
        *
        * @param nMask  the mask identifying the value
        *
        * @return the extracted value
        */
        protected int extractState(int nMask)
            {
            return getState() & nMask;
            }

        /**
        * Update a particular masked value within the state of the Status
        * object.
        *
        * @param nMask   the mask of bits to store the value within
        * @param nValue  the value to store inside that mask
        */
        protected void updateState(int nMask, int nValue)
            {
            assert (nValue & ~nMask) == 0;

            int nStateOld = getState();
            int nStateNew = (nStateOld & ~nMask) | nValue;
            if (nStateNew != nStateOld)
                {
                setState((byte) nStateNew);
                }
            }

        /**
        * Extract a particular masked flag from the state of the Status
        * object.
        *
        * @param nMask  the mask identifying the flag
        *
        * @return the extracted flag as a boolean
        */
        protected boolean extractFlag(int nMask)
            {
            return extractState(nMask) != 0;
            }

        /**
        * Update a particular masked flag within the state of the Status
        * object.
        *
        * @param nMask  the mask of flag bit to store the flag within
        * @param f      the boolean value to store within that mask
        */
        protected void updateFlag(int nMask, boolean f)
            {
            updateState(nMask, f ? nMask : 0);
            }

        // ----- constants ----------------------------------------------

        /**
        * Bitmask for status (least significant three bits reserved).
        */
        protected static final int STATE_MASK_STATUS    = 0x07;
        /**
        * Bitmask for entry in front.
        */
        protected static final int STATE_MASK_FRONT     = 0x08;
        /**
        * Bitmask for value in front and back being in sync.
        */
        protected static final int STATE_MASK_INSYNC    = 0x10;

        /**
        * Status: The Status object exists and no thread is currently
        * performing processing against the associated entry.
        */
        protected static final int STATUS_AVAILABLE     = 0x00;
        /**
        * Status: The Status object has been reserved for processing by a
        * thread but is not yet processing.
        */
        protected static final int STATUS_RESERVED      = 0x01;
        /**
        * Status: The Status object represents an Entry that is currently
        * being processed.
        */
        protected static final int STATUS_PROCESSING    = 0x02;
        /**
        * Status: The Status object represents an Entry that was very
        * recently being processed, and is currently finalizing the results
        * of that processing.
        */
        protected static final int STATUS_COMMITTING    = 0x03;
        /**
        * Status: The Status object has been discarded.
        */
        protected static final int STATUS_INVALIDATED   = 0x04;

        /**
        * Bitmask for fields that would indicate that the Status must not be
        * discarded.
        */
        protected static final int STATE_MASK_RETAIN    =
                STATE_MASK_STATUS | STATE_MASK_FRONT | STATE_MASK_INSYNC;

        /**
        * Bit values for fields that would indicate that the Status can be
        * discarded.
        */
        protected static final int STATE_VALUE_RETAIN   =
                STATUS_AVAILABLE | STATE_MASK_INSYNC;

        // ----- data members -------------------------------------------

        /**
        * The Thread that currently owns the Status object.
        */
        private Thread m_threadOwner;

        /**
        * The number of other threads waiting on this Status to become
        * available.
        */
        private byte m_cWaiting;

        /**
        * Current state, including status and various flags.
        */
        private volatile byte m_nState = STATUS_AVAILABLE;

        /**
        * The event (if any) that has been received for the front Map entry
        * for which this Status exists. If multiple events are received,
        * they are merged; for truly predictable behavior, this requires that
        * the front Map implementation be synchronized on the portions that
        * are raising events, such that an event will only be raised from the
        * front Map on one thread at a time. The event listener must then
        * synchronize on this Status object (to verify that it is indeed
        * valid before registering the event) and the register the event
        * while holding that synchronization.
        */
        private volatile MapEvent m_evtFront;
        }


    // ----- inner class: KeyIterator ---------------------------------------

    /**
    * An Iterator implementation that attempts to provide the most resilient
    * and most up-to-date view of the keys in the OverflowMap. This means
    * that it will avoid throwing a ConcurrentModificationException, and that
    * it will attempt to directly use the underlying iterators available for
    * the front and back maps.
    */
    protected class KeyIterator
            implements Iterator
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public KeyIterator()
            {
            }

        // ----- Iterator interface -------------------------------------

        /**
        * Returns <tt>true</tt> if the iteration has more elements. (In other
        * words, returns <tt>true</tt> if <tt>next</tt> would return an
        * element rather than throwing an exception.)
        *
        * @return <tt>true</tt> if the iterator has more elements
        */
        public boolean hasNext()
            {
            if (m_fNextKeyReady)
                {
                return true;
                }

            return advance();
            }

        /**
        * Returns the next element in the iteration.
        *
        * @return the next element in the iteration
        *
        * @throws java.util.NoSuchElementException  if the Iterator has no more
        *         elements
        */
        public Object next()
            {
            if (!m_fNextKeyReady && !advance())
                {
                throw new NoSuchElementException();
                }

            Object oKey = m_oNextKey;

            m_fNextKeyReady = false;
            m_fCanDelete    = true;
            m_oPrevKey      = oKey;

            return oKey;
            }

        /**
        * Removes from the underlying collection the last element returned by
        * the iterator.
        *
        * @throws IllegalStateException  if the <tt>next</tt> method has not
        *         yet been called, or the <tt>remove</tt> method has already
        *         been called after the last call to the <tt>next</tt> method
        */
        public void remove()
            {
            if (m_fCanDelete)
                {
                m_fCanDelete = false;

                // cannot call the underlying iterator to remove the key
                // because the remove is logically against the overflow map
                // itself, not the front or back map
                SimpleOverflowMap.this.remove(m_oPrevKey);
                }
            else
                {
                throw new IllegalStateException();
                }
            }

        // ----- internal -----------------------------------------------

        /**
        * Advance to the next key.
        *
        * @return true if there is a next key
        */
        protected boolean advance()
            {
            assert !m_fNextKeyReady;

            while (true)
                {
                Iterator iter = m_iter;
                boolean  fNext;
                Object   oKey;
                try
                    {
                    fNext = iter.hasNext();
                    oKey  = fNext ? iter.next() : null;
                    }
                catch (ConcurrentModificationException e)
                    {
                    int nMode = m_nMode;
                    if (nMode == ITERATE_FRONT || nMode == ITERATE_BACK)
                        {
                        // switch to an Iterator that doesn't throw a
                        // ConcurrentModificationException, and try again
                        useSnapshotIterator();
                        continue;
                        }
                    else
                        {
                        throw e;
                        }
                    }

                if (fNext)
                    {
                    // if there is a previous-keys collection, then add the
                    // key to it and at the same time verify that the key was
                    // not already iterated
                    Collection collPrevKeys = m_collPrevKeys;
                    if (collPrevKeys == null || collPrevKeys.add(oKey))
                        {
                        m_oNextKey      = oKey;
                        m_fNextKeyReady = true;
                        return true;
                        }
                    }
                else
                    {
                    switch (m_nMode)
                        {
                        case ITERATE_INITIAL:
                            useFrontIterator();
                            break;

                        case ITERATE_FRONT:
                            useBackIterator();
                            break;

                        case ITERATE_BACK:
                        case ITERATE_SNAPSHOT:
                            // nothing else to iterate; clean up
                            useDoneIterator();
                            // fall through
                        case ITERATE_DONE:
                            return false;
                        }
                    }
                }
            }

        /**
        * Switch to a snapshot iterator.
        */
        protected void useFrontIterator()
            {
            assert m_nMode == ITERATE_INITIAL;

            // iterate through all the status objects
            Iterator  iter   = SimpleOverflowMap.this.getStatusMap().entrySet().iterator();

            // but filter out all the ones that aren't in the front map
            Filter    filter = FrontFilterConverter.INSTANCE;

            // and convert the remaining status objects into their keys
            Converter conv   = FrontFilterConverter.INSTANCE;

            m_nMode        = ITERATE_FRONT;
            m_iter         = new ConverterEnumerator(
                    (Iterator) new FilterEnumerator(iter, filter), conv);
            m_collPrevKeys = new ArrayList();
            }

        /**
        * Switch to an iterator over the back map.
        */
        protected void useBackIterator()
            {
            assert m_nMode == ITERATE_FRONT;

            // iterate through all the keys in the back map
            Iterator iter = SimpleOverflowMap.this.getBackMap().keySet().iterator();

            m_nMode        = ITERATE_BACK;
            m_iter         = iter;
            // we need to switch from a List (used on the front) to a Set
            // because we need to both add what we're iterating *and* check
            // for uniqueness
            m_collPrevKeys = new HashSet(m_collPrevKeys);
            }

        /**
        * Switch to a snapshot iterator.
        */
        protected void useSnapshotIterator()
            {
            Map mapStatus = SimpleOverflowMap.this.getStatusMap();
            Map mapBack   = SimpleOverflowMap.this.getBackMap();

            // we're going to build a list of all keys
            HashSet setKeys = new HashSet(mapStatus.size() + mapBack.size());

            // add front keys
            Map.Entry[] aEntry = (Map.Entry[]) mapStatus.entrySet().toArray(ENTRY_ARRAY);
            for (int i = 0, c = aEntry.length; i < c; ++i)
                {
                Map.Entry entry  = aEntry[i];
                Status    status = (Status) entry.getValue();
                if (status.isValid() && status.isEntryInFront())
                    {
                    setKeys.add(entry.getKey());
                    }
                }
            aEntry = null; // clear it out; it could be a big array

            // add back keys
            try
                {
                setKeys.addAll(mapBack.keySet());
                }
            catch (ConcurrentModificationException e)
                {
                // synchronizing totally sucks, but this should never happen
                // in the first place with any of the built-in Coherence map
                // implementations, and the whole point of this is to have
                // as close to no chance of ConcurrentModificationException
                // as is possible
                Object[] aoKey;
                synchronized (mapBack)
                    {
                    aoKey = mapBack.keySet().toArray();
                    }
                setKeys.addAll(new ImmutableArrayList(aoKey));
                aoKey = null; // clear it out; it could be a big array
                }

            // remove any previously iterated keys
            Collection collPrevKeys = m_collPrevKeys;
            if (collPrevKeys != null)
                {
                setKeys.removeAll(collPrevKeys);
                }

            m_nMode        = ITERATE_SNAPSHOT;
            m_iter         = setKeys.iterator();
            m_collPrevKeys = null;
            }

        /**
        * Switch to an iterator over nothing.
        */
        protected void useDoneIterator()
            {
            m_nMode        = ITERATE_DONE;
            m_iter         = NullImplementation.getIterator();
            m_collPrevKeys = null;
            m_oNextKey     = null;
            }

        // ----- constants ----------------------------------------------

        /**
        * Unitialized iteration mode.
        */
        private static final int ITERATE_INITIAL = 0;
        /**
        * Iteration mode that iterates only keys in the FrontMap. For this
        * mode, <tt>m_iter</tt> is an Iterator of Status objects in the
        * StatusMap.
        */
        private static final int ITERATE_FRONT = 1;
        /**
        * Iteration mode that iterates only keys in the BackMap. For this
        * mode, <tt>m_iter</tt> is an Iterator of keys in the BackMap.
        */
        private static final int ITERATE_BACK = 2;
        /**
        * Iteration mode that iterates over a snap-shot of all keys in the
        * OverflowMap.
        */
        private static final int ITERATE_SNAPSHOT = 3;
        /**
        * Nothing is left to iterate.
        */
        private static final int ITERATE_DONE = 4;

        // ----- data members -------------------------------------------

        /**
        * The current iteration mode; one of the <tt>ITERATE_*</tt>
        * constants.
        */
        private int m_nMode = ITERATE_INITIAL;

        /**
        * The current underlying iterator of keys.
        */
        private Iterator m_iter = NullImplementation.getIterator();

        /**
        * The Collection of keys already iterated. If this is null, then
        * the keys being iterated are not collected.
        */
        private Collection m_collPrevKeys;

        /**
        * Set to true when <tt>m_oNextKey</tt> is the next key to return
        * from the iterator. If there is no next key, or if the next key is
        * not determined yet, then this will be false. Set up by
        * {@link #advance} and reset by {@link #next}.
        */
        private boolean m_fNextKeyReady;

        /**
        * The next key to return from this iterator.  Set up by
        * {@link #advance} and reset by {@link #next}.
        */
        private Object m_oNextKey;

        /**
        * Set to true when the <tt>m_oPrevKey</tt> key has been returned but
        * not yet removed. Set up by {@link #next} and reset by
        * {@link #remove()}.
        */
        private boolean m_fCanDelete;

        /**
        * The key that can be deleted, if any. Set up by {@link #next}.
        */
        private Object m_oPrevKey;
        }


    // ----- inner class: FrontFilterConverter ------------------------------

    /**
    * A combination Filter and Converter used to iterate through the
    * status map in order to iterate through the front keys.
    */
    protected static class FrontFilterConverter
            implements Filter, Converter
        {
        /**
        * Filters keys out that are not in the front map.
        */
        public boolean evaluate(Object o)
            {
            Status status = (Status) ((Map.Entry) o).getValue();
            return status.isValid() && status.isEntryInFront();
            }

        /**
        * Extracts the key from the Status object.
        */
        public Object convert(Object o)
            {
            return ((Map.Entry) o).getKey();
            }

        public static final FrontFilterConverter
                INSTANCE = new FrontFilterConverter();
        }


    // ----- inner class: HashcodeComparator --------------------------------

    /**
    * A stateless Comparator that compares {@link Object#hashCode} values.
    */
    protected static class HashcodeComparator
            implements Comparator
        {
        public int compare(Object o1, Object o2)
            {
            int n1 = o1 == null ? 0 : o1.hashCode();
            int n2 = o2 == null ? 0 : o2.hashCode();
            return n1 > n2 ?  1 : n1 == n2 ? 0 : -1;
            }

        @Override
        public boolean equals(Object obj)
            {
            return obj == this;
            }

        public static final HashcodeComparator
                INSTANCE = new HashcodeComparator();
        }


    // ----- constants ------------------------------------------------------

    /**
    * This event indicates that an entry has been added to the map.
    */
    public static final int ENTRY_INSERTED = MapEvent.ENTRY_INSERTED;

    /**
    * This event indicates that an entry has been updated in the map.
    */
    public static final int ENTRY_UPDATED  = MapEvent.ENTRY_UPDATED;

    /**
    * This event indicates that an entry has been removed from the map.
    */
    public static final int ENTRY_DELETED  = MapEvent.ENTRY_DELETED;

    /**
    * An empty array of type Map Entry.
    */
    static final Map.Entry[] ENTRY_ARRAY = new Map.Entry[0];


    // ----- data fields ----------------------------------------------------

    /**
    * The "front" map, which is size-limited.
    */
    protected ObservableMap m_mapFront;

    /**
    * The "back" map, which the front overflows to.
    */
    protected Map m_mapBack;

    /**
    * The miss cache; may be null.
    */
    protected Map m_mapMiss;

    /**
    * An option to allow null values.
    */
    private boolean m_fNullValuesAllowed;

    /**
    * An option to use putAll (no return value) to update the front Map.
    */
    private boolean m_fUseFrontPutAll;

    /**
    * A Map for maintaining Status information on the entries that are being
    * managed by this Overflow Map.
    */
    private Map m_mapStatus = new SafeHashMap();

    /**
    * A list of keys that may have deferred events.
    */
    private List m_listDeferred = new RecyclingLinkedList();

    /**
    * A ThreadGate to coordinate key-level versus Map-level operations, and
    * potentially to detect re-entrancy.
    */
    private Gate m_gate = new ThreadGateLite();

    /**
    * The listener for the "front" map.
    */
    private MapListener m_listenerFront;

    /**
    * The CacheStatistics object maintained by this cache.
    */
    protected SimpleCacheStatistics m_stats = new SimpleCacheStatistics();

    /**
    * Static flag used to make sure the same warning message (a missing event
    * or events out of order) is only issued once.
    */
    private static boolean s_fWarnedEventSequence;
    }
