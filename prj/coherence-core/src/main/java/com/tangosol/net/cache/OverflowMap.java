/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.collections.AbstractStableIterator;

import com.tangosol.io.nio.BinaryMap;
import com.tangosol.net.NamedCache;
import com.tangosol.util.AbstractKeySetBasedMap;
import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.Gate;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.LiteMap;
import com.tangosol.util.LiteSet;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.RecyclingLinkedList;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.SparseArray;
import com.tangosol.util.ThreadGateLite;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
* An Observable Map implementation that wraps two maps - a front map
* (assumed to be fast but limited in its maximum size) and a back map
* (assumed to be slower but much less limited in its maximum size).
* <p>
* The OverflowMap is observable, and as such it is allowed to be an active
* data structure. In other words, it supports item expiration, eviction, and
* other types of "self-generating" events. (The {@link SimpleOverflowMap}) is
* an alternative that may be more efficient if the overflow map is passive.)
* As a consequence of the requirements for managing the overflow map as an
* active data structure, the OverflowMap maintains information about each
* entry in the map; in practice, this will limit the size of the OverflowMap
* since all keys and some additional information for each key will be
* maintained in memory. On the other hand, by maintaining this information
* in memory, certain operations will be more efficient, such as the methods
* {@link #containsKey containsKey()}, {@link #size()},
* {@link KeySet#iterator keySet().iterator()}, etc.
* (Again, for an alternative, see {@link SimpleOverflowMap}, which does not
* maintain the entire set of keys in memory.)
* <p>
* The primary reason why OverflowMap is based on the AbstractKeySetBasedMap
* is that the set of keys within the OverflowMap is considered to be "known",
* although not altogether stable due to concurrency issues (the OverflowMap
* is designed to support a high level of concurrency). AbstractKeySetBasedMap
* conceptually delegates many operations down to its key Set, which the
* OverflowMap implementation reverses (by having the implementations on the
* OverflowMap itself). However, the key and entry Sets and the values
* Collection benefit significantly in terms of their ability to optimize
* the {@link Collection#iterator()} and {@link Collection#toArray()} methods.
* <p>
* Further, as a result (and sometimes as a consequence) of its observability,
* the OverflowMap supports (and has to support) a number of capabilities that
* the SimpleOverflowMap does not:
* <ul>
* <li>Expiry - the entries in the OverflowMap can each have their own expiry,
*     which is respected regardless of whether the entry is being managed in
*     the front and/or back map.</li>
* <li>Reentrancy - since an event listener on the OverflowMap can respond to
*     an event in an unpredictable manner, including by attempting to undo
*     or compensate for or react to the event, the OverflowMap is forced to
*     support reentrancy.</li>
* </ul>
*
* @since Coherence 2.2
* @author cp  2003.05.24
* @author cp  2005.08.11 re-architected and split out SimpleOverflowMap
*/
public class OverflowMap
        extends AbstractKeySetBasedMap
        implements CacheMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a OverflowMap using two specified maps:
    * <ul>
    * <li> <i>FrontMap</i> (aka "cache" or "shallow") and
    * <li> <i>BackMap</i>  (aka "file" or "deep")
    * </ul>
    * If the BackMap implements the ObservableMap interface a listener
    * will be added to the BackMap to invalidate items updated [externally]
    * in the back map.
    *
    * @param mapBack   back map
    * @param mapFront  front map
    */
    public OverflowMap(ObservableMap mapFront, Map mapBack)
        {
        azzert(mapFront != null && mapBack != null);

        m_mapFront = mapFront;
        m_mapBack  = mapBack;

        // register whatever data already exists in the front and back maps
        // unfortunately, it is [theoretically] possible for changes to occur
        // in between this step and when we put the listener on, but the
        // listener can't be added until the status map is prepared
        ImmutableArrayList setFront = new ImmutableArrayList(mapFront.keySet());
        if (!setFront.isEmpty())
            {
            Map mapStatus = getStatusMap();
            for (Iterator iter = setFront.iterator(); iter.hasNext(); )
                {
                Object oKey   = iter.next();
                Status status = instantiateStatus();
                status.setEntryInFront(true);
                status.setBackUpToDate(false);
                mapStatus.put(oKey, status);
                }
            setSize(mapStatus.size());
            }

        ImmutableArrayList setBack = new ImmutableArrayList(mapBack.keySet());
        if (!setBack.isEmpty())
            {
            Map mapStatus = getStatusMap();
            for (Iterator iter = setBack.iterator(); iter.hasNext(); )
                {
                Object oKey   = iter.next();
                Status status = (Status) mapStatus.get(oKey);
                if (status == null)
                    {
                    status = instantiateStatus();
                    mapStatus.put(oKey, status);
                    }
                status.setEntryInBack(true);
                }
            setSize(mapStatus.size());
            }

        // put the listeners onto the front and back maps
        setFrontMapListener(instantiateFrontMapListener());
        if (mapBack instanceof ObservableMap)
            {
            setBackMapListener(instantiateBackMapListener());
            }

        // double-check that everything is ready to go
        if (!setFront.isEmpty())
            {
            verifyNoNulls(mapFront.keySet(), "The front map contains a null key");
            if (!isNullValuesAllowed())
                {
                verifyNoNulls(mapFront.values(), "NullValuesAllowed is false"
                    + " but the front map contains at least one null value");
                }

            // double check that the front map didn't change in the meantime
            azzert(mapFront.keySet().equals(setFront));
            }

        if (!setBack.isEmpty())
            {
            verifyNoNulls(mapBack.keySet(), "The back map contains a null key");
            if (!isNullValuesAllowed())
                {
                verifyNoNulls(mapBack.values(), "NullValuesAllowed is false"
                    + " but the back map contains at least one null value");
                }

            // double check that the back map didn't change in the meantime
            azzert(mapBack.keySet().equals(setBack));
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
    * {@inheritDoc}
    */
    @Override
    public void clear()
        {
        if (getGate().isEnteredByCurrentThread() || hasListeners())
            {
            // in the case of re-entrancy:
            //   a re-entrant clear cannot be optimized or deadlock could
            //   occur
            // in the case of listeners:
            //   we are forced to remove the items one by one since
            //   the actions of the event listeners are unknown, and
            //   they must see a stable view of the OverflowMap, which
            //   they would not see if (for example) we cleared the
            //   entire overflow before and/or after issuing the events
            for (Iterator iter = keySet().iterator(); iter.hasNext(); )
                {
                iter.next();
                iter.remove();
                }
            }
        else
            {
            beginMapProcess();
            try
                {
                synchronized (this)
                    {
                    // kill the listeners temporarily
                    MapListener listenerFront = getFrontMapListener();
                    MapListener listenerBack  = getBackMapListener();
                    setFrontMapListener(null);
                    setBackMapListener(null);
                    try
                        {
                        // clear both the front and back, which will leave the
                        // overflow map in an empty state
                        getFrontMap().clear();
                        getBackMap().clear();
                        setSize(0);

                        // clean up the status objects
                        Map mapStatus = getStatusMap();
                        synchronized (mapStatus)
                            {
                            for (Iterator iter = mapStatus.values().iterator();
                                 iter.hasNext(); )
                                {
                                Status status = (Status) iter.next();
                                synchronized (status)
                                    {
                                    // since no one else is processing, the
                                    // status should be available
                                    assert status.isOwnedByCurrentThread();

                                    // clear the status
                                    status.setEntryInFront(false);
                                    status.setEntryInBack(false);

                                    // clear any pending event
                                    status.takeEvent();
                                    }
                                }
                            }

                        // clear out the list of statuses that have deferred
                        // events; since there are no even listeners right
                        // now on the front or the back, no events should
                        // have accumulated during the clear() processing
                        getDeferredList().clear();

                        LongArray laExpiry = getExpiryArray();
                        if (laExpiry != null)
                            {
                            laExpiry.clear();
                            }

                        // clear the statistics
                        getCacheStatistics().resetHitStatistics();
                        }
                    finally
                        {
                        setFrontMapListener(listenerFront);
                        setBackMapListener(listenerBack);
                        }
                    }
                }
            finally
                {
                endMapProcess();
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean containsKey(Object oKey)
        {
        boolean fContains = false;

        if (getStatusMap().containsKey(oKey))
            {
            Status status = beginKeyProcess(oKey);
            try
                {
                fContains = status.isEntryExistent();
                }
            finally
                {
                endKeyProcess(oKey, status);
                }
            }

        return fContains;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Object get(Object oKey)
        {
        Object oResult;

        if (getStatusMap().containsKey(oKey))
            {
            oResult = getInternal(oKey, true, null);
            }
        else
            {
            // if there is no status object for it, then it doesn't exist
            oResult = null;
            m_stats.registerMiss();
            }

        return oResult;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isEmpty()
        {
        if (getStatusMap().isEmpty())
            {
            return true;
            }

        return size() == 0;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Object put(Object oKey, Object oValue)
        {
        return putInternal(oKey, oValue, false, EXPIRY_DEFAULT);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public void putAll(Map map)
        {
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();
            putInternal(entry.getKey(), entry.getValue(), true, EXPIRY_DEFAULT);
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Object remove(Object oKey)
        {
        return getStatusMap().containsKey(oKey)
               ? removeInternal(oKey, false, false)
               : null;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public int size()
        {
        evict();
        return getSize();
        }


    // ----- CacheMap methods -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return putInternal(oKey, oValue, false, cMillis);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Map getAll(Collection colKeys)
        {
        Map  mapResult = new HashMap();
        long ldtStart  = getSafeTimeMillis();
        int  cKeys     = colKeys.size();
        for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
            {
            Object oKey = iter.next();
            if (getStatusMap().containsKey(oKey))
                {
                getInternal(oKey, false, mapResult);
                }
            }

        // update stats
        int cHits = mapResult.size();
        if (cHits > 0)
            {
            m_stats.registerHits(cHits, ldtStart);
            }
        int cMisses = cKeys - cHits;
        if (cMisses > 0)
            {
            m_stats.registerMisses(cMisses, ldtStart);
            }

        return mapResult;
        }


    // ----- ObservableMap methods ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener)
        {
        addMapListener(listener, (Filter) null, false);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener)
        {
        removeMapListener(listener, (Filter) null);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener, Object oKey, boolean fLite)
        {
        azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            support = m_listenerSupport = new MapListenerSupport();
            }

        support.addListener(listener, oKey, fLite);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener, Object oKey)
        {
        azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support != null)
            {
            support.removeListener(listener, oKey);
            if (support.isEmpty())
                {
                m_listenerSupport = null;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener, Filter filter, boolean fLite)
        {
        azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            support = m_listenerSupport = new MapListenerSupport();
            }

        support.addListener(listener, filter, fLite);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener, Filter filter)
        {
        azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support != null)
            {
            support.removeListener(listener, filter);
            if (support.isEmpty())
                {
                m_listenerSupport = null;
                }
            }
        }


    // ----- AbstractKeySetBasedMap methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected boolean removeBlind(Object oKey)
        {
        return ((Boolean) removeInternal(oKey, true, false)).booleanValue();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected Set getInternalKeySet()
        {
        Set set = m_setKeysInternal;
        if (set == null)
            {
            m_setKeysInternal = set = instantiateInternalKeySet();
            }
        return set;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected boolean isInternalKeySetIteratorMutable()
        {
        // avoids double-wrapping of the underlying iterator, but the
        // internal key set iterator has to implement remove itself by
        // calling back to removeBlind
        return true;
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * A combined implementation of {@link #get} and {@link #getAll} that
    * eliminates duplicate (and very complex) code.
    *
    * @param oKey       the key to access
    * @param fStats     pass true to update statistics
    * @param mapResult  pass a map into which the key and its value will be
    *                   put iff the key is contained within the overflow map
    *
    * @return the value (which may be null) is returned from the entry if it
    *         exists, otherwise null is returned
    */
    protected Object getInternal(Object oKey, boolean fStats, Map mapResult)
        {
        Object   oValue    = null;
        boolean  fContains = false;
        MapEvent evtRaise  = null;
        long     ldtStart  = fStats ? getSafeTimeMillis() : 0L;
        Status   status    = beginKeyProcess(oKey);
        boolean  fExists   = status.isEntryExistent();
        try
            {
            Map mapFront = getFrontMap();
            Map mapBack  = getBackMap();

            // check the front
            if (status.isEntryInFront())
                {
                fContains = true;
                oValue    = mapFront.get(oKey);

                // may have been evicted
                MapEvent evtFront;
                synchronized (mapFront)
                    {
                    synchronized (status)
                        {
                        // can't close processing because we may need to
                        // re-insert the value into the front if it just got
                        // evicted
                        evtFront = status.takeFrontEvent();

                        // we want to close the processing on the front
                        // before we release the monitor, unless it was a
                        // delete event (see delete processing below)
                        if (evtFront == null || evtFront.getId() != ENTRY_DELETED)
                            {
                            status.closeProcessing();
                            }
                        }
                    }

                if (evtFront != null)
                    {
                    switch (evtFront.getId())
                        {
                        case ENTRY_INSERTED:
                            // this is completely un-expected, since the
                            // entry was supposedly already in the front
                            warnUnfathomable(evtFront);

                            if (oValue == null)
                                {
                                oValue = evtFront.getNewValue();
                                }
                            status.setBackUpToDate(false);

                            // convert the event to an update, since the
                            // OverflowMap already thought it contained the
                            // entry
                            evtRaise = new CacheEvent(this, ENTRY_UPDATED,
                                    oKey, null, oValue, isSynthetic(evtFront));
                            break;

                        case ENTRY_UPDATED:
                            // an update event could be caused by something
                            // like an eviction followed by a load in the
                            // front map
                            if (oValue == null)
                                {
                                oValue = evtFront.getNewValue();
                                }
                            status.setBackUpToDate(false);

                            // this event will be raised, since it represents
                            // a change to the contents of the overflow map
                            evtRaise = evtFront;
                            break;

                        case ENTRY_DELETED:
                            // "contains" is still true (since we have an
                            // event that provided us with a value) but the
                            // value is no longer in the front map
                            if (oValue == null)
                                {
                                oValue = getLatestOldValue(evtFront);
                                }
                            status.setEntryInFront(false);
                            break;
                        }
                    }
                }
            else if (status.isEntryInBack())
                {
                fContains = true;
                oValue    = mapBack.get(oKey);

                // it's possible that looking at the back Map caused the
                // desired entry to be evicted, etc.
                MapEvent evtBack = status.takeBackEvent();
                if (evtBack != null)
                    {
                    switch (evtBack.getId())
                        {
                        case ENTRY_INSERTED:
                            // this is completely un-expected, since the
                            // entry was supposedly already in the back
                            warnUnfathomable(evtBack);

                            if (oValue == null)
                                {
                                oValue = evtBack.getNewValue();
                                }

                            // convert the event to an update, since the
                            // OverflowMap already thought it contained the
                            // entry
                            evtRaise = new CacheEvent(this, ENTRY_UPDATED,
                                    oKey, null, oValue, isSynthetic(evtBack));
                            break;

                        case ENTRY_UPDATED:
                            if (oValue == null)
                                {
                                oValue = evtBack.getNewValue();
                                }

                            // this event will be raised, since it represents
                            // a change to the contents of the overflow map
                            evtRaise = evtBack;
                            break;

                        case ENTRY_DELETED:
                            if (oValue == null)
                                {
                                oValue = getLatestOldValue(evtBack);
                                }

                            // the entry was evicted from the back map
                            // when we tried to check for it, but it's being
                            // put into the front anyhow, so this not a loss
                            status.setEntryInBack(false);
                            status.setBackUpToDate(false);
                            break;
                        }
                    }
                }

            // move the item to the front map if it isn't there
            if (fContains && !status.isEntryInFront())
                {
                putOne(mapFront, oKey, oValue, isFrontPutBlind());

                // assume it worked
                status.setEntryInFront(true);

                MapEvent evtFront;
                synchronized (mapFront)
                    {
                    evtFront = status.closeProcessing();
                    }

                // we're expecting an inserted event on the front, so
                // it can be safely ignored; other front events
                // should be handled, and back events can be ignored
                // (since the value is now in the front)
                if (evtFront != null && evtFront.getMap() == getFrontMap())
                    {
                    switch (evtFront.getId())
                        {
                        case ENTRY_INSERTED:
                            // this was expected, since we just
                            // inserted the entry into the front map;
                            // just verify that nothing else has
                            // occurred; for example, an event
                            // handler could have modified the
                            // inserted value
                            Object oValueNew = evtFront.getNewValue();
                            if (oValueNew != oValue)
                                {
                                // the value has changed from what we
                                // inserted; mark back out-of-date
                                status.setBackUpToDate(false);

                                // issue an update event
                                int     nId        = ENTRY_UPDATED;
                                Object  oOldValue  = oValue;
                                if (evtRaise != null)
                                    {
                                    nId       = evtRaise.getId();
                                    oOldValue = evtRaise.getOldValue();
                                    }
                                evtRaise = new CacheEvent(this, nId, oKey,
                                        oOldValue, oValueNew,
                                        isSynthetic(evtFront));

                                // return the event-modified value
                                oValue = oValueNew;
                                }
                            break;

                        case ENTRY_UPDATED:
                            // this is bizarre, since the entry was not even
                            // in the front map as far as we knew
                            warnUnfathomable(evtFront);
                            status.setBackUpToDate(false);
                            oValue   = evtFront.getNewValue();
                            evtRaise = evtFront;
                            break;

                        case ENTRY_DELETED:
                            // the event indicates the exact opposite of what
                            // we would expect, but it could just be that the
                            // front map cannot hold the data (a synthetic
                            // event)
                            if (!isSynthetic(evtFront))
                                {
                                warnUnfathomable(evtFront);
                                }

                            // either way, the entry is not in the front
                            status.setEntryInFront(false);

                            boolean fIsInBack = status.isEntryInBack();
                            if (!(fIsInBack && status.isBackUpToDate()))
                                {
                                // try to keep it by puttting it into the
                                // back map
                                putOne(mapBack, oKey, oValue, true);

                                // assume that the write to the back
                                // succeeded ..
                                status.setEntryInBack(true);
                                status.setBackUpToDate(true);

                                // .. unless an event tells us differently
                                MapEvent evtBack = status.takeBackEvent();
                                if (evtBack != null)
                                    {
                                    switch (evtBack.getId())
                                        {
                                        case ENTRY_INSERTED:
                                        case ENTRY_UPDATED:
                                            Object oNewValue = evtBack.getNewValue();
                                            if (oNewValue != oValue && hasListeners())
                                                {
                                                // somehow when we put the value
                                                // into the back map, it changed,
                                                // so dispatch an updated event
                                                evtRaise = new CacheEvent(this,
                                                        ENTRY_UPDATED, oKey,
                                                        oValue, oNewValue,
                                                        isSynthetic(evtBack));
                                                oValue = oNewValue;
                                                }
                                            break;

                                        case ENTRY_DELETED:
                                            if (!isSynthetic(evtBack))
                                                {
                                                warnUnfathomable(evtBack);
                                                }

                                            // we've lost the value from the
                                            // front and the back, so it's
                                            // been lost from the OverflowMap
                                            evtRaise  = new CacheEvent(this,
                                                    ENTRY_DELETED, oKey,
                                                    oValue, null, true);

                                            // at any rate, the behavior of
                                            // the get() on the OverflowMap
                                            // is as if the entry was deleted
                                            // *before* the value could be
                                            // gotten
                                            oValue    = null;
                                            fContains = false;
                                            break;
                                        }
                                    }
                                }
                            break;
                        }
                    }
                }
            }
        finally
            {
            // adjust the size if the size changed
            if (fExists != status.isEntryExistent())
                {
                // fExists tells us that it used to exist
                adjustSize(fExists ? -1 : +1);
                }

            // raise the event, if any
            if (evtRaise != null)
                {
                dispatchEvent(status, evtRaise);
                }

            endKeyProcess(oKey, status);
            }

        // update statistics
        if (fStats)
            {
            if (fContains)
                {
                m_stats.registerHit(ldtStart);
                }
            else
                {
                m_stats.registerMiss(ldtStart);
                }
            }

        // update result map
        if (fContains && mapResult != null)
            {
            mapResult.put(oKey, oValue);
            }

        return oValue;
        }

    /**
    * A combined implementation of {@link #put(Object, Object)} and
    * "void put(Object, Object)" that eliminates duplicate (and muchos
    * complex) code between the {@link #put} and {@link #putAll} methods.
    * Additionally implements the put-with-expiry option.
    *
    * @param oKey        the key to store
    * @param oValue      the value to store
    * @param fStoreOnly  pass true to simply store the new value, or false to
    *                    both store the new value and return the previous
    *                    value
    * @param cMillis     the time-to-live for the entry as defined by
    *                    {@link CacheMap#put(Object, Object, long)}
    *
    * @return the previous value if fStoreOnly is false and the entry existed
    *         in the overflow map, otherwise undefined
    */
    protected Object putInternal(Object oKey, Object oValue, boolean fStoreOnly, long cMillis)
        {
        if (oKey == null)
            {
            throw new IllegalArgumentException("null keys are unsupported");
            }

        boolean fNullAllowed = isNullValuesAllowed();
        if (oValue == null && !fNullAllowed)
            {
            throw new IllegalArgumentException("null values are unsupported"
                                               + " (key=\"" + oKey + "\")");
            }

        if (isExpiryEnabled())
            {
            // the parameter
            if (cMillis == EXPIRY_DEFAULT)
                {
                cMillis = getExpiryDelay();
                }
            }
        else if (cMillis > 0)
            {
            throw new IllegalArgumentException("expiry is not enabled"
                    + " (key=\"" + oKey + "\", expiry=" + cMillis + ")");
            }

        Object   oOldValue = null;
        MapEvent evtRaise  = null;
        long     ldtStart  = getSafeTimeMillis();
        Status   status    = beginKeyProcess(oKey);
        boolean  fExists   = status.isEntryExistent();
        try
            {
            boolean fInFront      = status.isEntryInFront();
            boolean fInBack       = status.isEntryInBack();
            boolean fBackUpToDate = status.isBackUpToDate();
            boolean fOldExpiry    = status.hasExpiry();
            boolean fNewExpiry    = cMillis > 0L;
            boolean fEvtReq       = hasListeners();
            boolean fOldValue     = false;
            boolean fOldValueReq  = fEvtReq || !fStoreOnly;
            Object  oNewValue     = oValue;

            Map mapFront = getFrontMap();
            if (fInFront && fOldValueReq)
                {
                oOldValue = mapFront.put(oKey, oValue);
                fOldValue = oOldValue != null;
                }
            else
                {
                putOne(mapFront, oKey, oValue, isFrontPutBlind());
                }
            status.setEntryInFront(true);
            status.setBackUpToDate(false);

            MapEvent evtFront = status.closeProcessing();
            if (evtFront == null || evtFront.getSource() != mapFront)
                {
                warnMissingEvent(oKey, fInFront ? ENTRY_UPDATED
                                                : ENTRY_INSERTED, true);

                if (!fOldValue)
                    {
                    // unless we can grab the old value from the back, it's
                    // been lost (since we didn't get any event); just assume
                    // it was null
                    fOldValue = !(fInBack && fBackUpToDate);
                    }
                }
            else
                {
                switch (evtFront.getId())
                    {
                    case ENTRY_INSERTED:
                        if (fExists)
                            {
                            // since the entry already exists, we'll have to
                            // issue an update event
                            oNewValue = evtFront.getNewValue();

                            if (fInFront)
                                {
                                // how could we get an insert if the entry
                                // was supposedly already in the front?
                                warnUnfathomable(evtFront);
                                }
                            }
                        else
                            {
                            // insert event is the correct one to raise from
                            // the overflow map
                            fOldValue = true;       // i.e. it was null
                            evtRaise  = evtFront;
                            }
                        break;

                    case ENTRY_UPDATED:
                        // if we get an update event from the front, then an
                        // update event is the correct one to raise (shows
                        // old value from the front and new value in the
                        // front)
                        evtRaise = evtFront;

                        if (!fOldValue)
                            {
                            if (fOldValueReq)
                                {
                                oOldValue = getLatestOldValue(evtFront);
                                }
                            fOldValue = true;
                            }

                        if (!fInFront)
                            {
                            // how could we get an update if the entry
                            // was supposedly missing from the front?
                            warnUnfathomable(evtFront);
                            }
                        break;

                    case ENTRY_DELETED:
                        status.setEntryInFront(false);

                        // this is not as unbelievable as it first appears,
                        // since the front could be full and reject the
                        // insertion, which will appear as a synthetic
                        // deletion
                        if (!isSynthetic(evtFront))
                            {
                            warnUnfathomable(evtFront);
                            }

                        // it's our job to move the new value to the back
                        Map mapBack = getBackMap();
                        oNewValue   = getLatestOldValue(evtFront);
                        if (fInFront || !fInBack || !fOldValueReq)
                            {
                            if (!fOldValue && fOldValueReq)
                                {
                                oOldValue = fInFront ? evtFront.getOldValue()
                                                     : null;
                                fOldValue = true;
                                }
                            putOne(mapBack, oKey, oNewValue, true);
                            }
                        else
                            {
                            oOldValue = mapBack.put(oKey, oNewValue);
                            fOldValue = oOldValue != null
                                    || fNullAllowed && mapBack.containsKey(oKey);
                            }

                        // assume that the write to the back succeeded ..
                        status.setEntryInBack(true);
                        status.setBackUpToDate(true);

                        // .. unless an event tells us differently
                        MapEvent evtBack = status.takeBackEvent();
                        if (evtBack != null)
                            {
                            boolean fSynthetic = isSynthetic(evtBack);
                            switch (evtBack.getId())
                                {
                                case ENTRY_UPDATED:
                                case ENTRY_INSERTED:
                                    oNewValue = evtBack.getNewValue();
                                    if (!fOldValue && fOldValueReq && !fInFront && fInBack)
                                        {
                                        oOldValue = getLatestOldValue(evtBack);
                                        }
                                    break;

                                case ENTRY_DELETED:
                                    status.setEntryInBack(false);

                                    if (!fSynthetic)
                                        {
                                        warnUnfathomable(evtBack);
                                        }

                                    if (fEvtReq)
                                        {
                                        evtRaise = new CacheEvent(this,
                                                ENTRY_DELETED, oKey, oOldValue,
                                                null, fSynthetic);
                                        }

                                    // prevent registerExpiry()
                                    fNewExpiry = false;
                                    break;
                                }
                            }

                        // no matter what, we've overridden the value in the
                        // back and thus we can't go back in later trying to
                        // get the old value that was there previously
                        fOldValue = true;
                        break;
                    }
                }

            // get the original value if necessary
            if (!fOldValue && (fEvtReq && evtRaise == null || !fStoreOnly)
                && fInBack && (!fInFront || fBackUpToDate))
                {
                // need to get the back value as the original value if there
                // is an event or a return value
                oOldValue = getBackMap().get(oKey);
                MapEvent evtBack = status.takeBackEvent();
                if (evtBack != null)
                    {
                    switch (evtBack.getId())
                        {
                        case ENTRY_INSERTED:
                            warnUnfathomable(evtBack);
                            break;
                        case ENTRY_UPDATED:
                            // update is hidden by the value in the front
                            oOldValue = getLatestOldValue(evtBack);
                            break;
                        case ENTRY_DELETED:
                            // delete is hidden by the value in the front
                            status.setEntryInBack(false);
                            oOldValue = getLatestOldValue(evtBack);
                            break;
                        }
                    }
                }

            // create the event if we don't already have one
            if (fEvtReq && evtRaise == null)
                {
                int nId = fExists ? ENTRY_UPDATED : ENTRY_INSERTED;
                evtRaise = new CacheEvent(this, nId, oKey, oOldValue, oNewValue, false);
                }

            if (fOldExpiry)
                {
                unregisterExpiry(oKey, status.getExpiry());
                status.setExpiry(0L);
                }

            if (fNewExpiry)
                {
                long ldtExpires = ldtStart + cMillis;
                status.setExpiry(ldtExpires);
                registerExpiry(oKey, ldtExpires);
                }
            }
        finally
            {
            // adjust the size of the OverflowMap if it changed
            if (fExists != status.isEntryExistent())
                {
                // fExists tells us that it used to exist
                adjustSize(fExists ? -1 : +1);
                }

            // raise the event, if any
            if (evtRaise != null)
                {
                dispatchEvent(status, evtRaise);
                }

            endKeyProcess(oKey, status);
            }

        // update statistics
        m_stats.registerPut(ldtStart);

        return oOldValue;
        }

    /**
    * A combined implementation of remove() and removeBlind() that eliminates
    * duplicate (and muchos complex) code
    *
    * @param oKey               the key to remove
    * @param fCheckRemovedOnly  pass true to only check for entry existence
    * @param fEviction          pass true if the removal is an eviction and
    *                           if the status is already owned by this thread
    *
    * @return if only checking for existence, then a Boolean value is
    *         returned, otherwise the value (which may be null) is returned
    *         from the entry if it exists, otherwise null is returned
    */
    protected Object removeInternal(Object oKey, boolean fCheckRemovedOnly, boolean fEviction)
        {
        Object   oValue        = null;
        boolean  fFrontRemoved = false;
        boolean  fBackRemoved  = false;
        MapEvent evtRaise      = null;
        boolean  fEvtReq       = hasListeners();
        Status   status        = fEviction
                                 ? (Status) getStatusMap().get(oKey)
                                 : beginKeyProcess(oKey);
        boolean  fExists       = status.isEntryExistent();
        try
            {
            // the assumption is that we will remove the entry from the back
            // after we remove it from the front, but that plan could change
            boolean fRemoveFromBack = true;

            // remove the entry from the front
            if (status.isEntryInFront())
                {
                Map mapFront = getFrontMap();
                fFrontRemoved = mapFront.keySet().remove(oKey);
                synchronized (mapFront)
                    {
                    evtRaise = status.closeProcessing();
                    }

                if (evtRaise == null || evtRaise.getSource() != mapFront)
                    {
                    if (mapFront.containsKey(oKey))
                        {
                        // no idea why the entry was not removed, but there
                        // is no reason to continue trying to remove it
                        fFrontRemoved   = false;
                        fRemoveFromBack = false;
                        }
                    else
                        {
                        if (fFrontRemoved)
                            {
                            warnMissingEvent(oKey, ENTRY_DELETED, true);
                            }

                        // it's possible that the status somehow got out of
                        // sync, and the entry wasn't actually in the front;
                        // either way, it's gone now
                        fFrontRemoved = true;
                        status.setEntryInFront(false);

                        // the removed value has been lost, so create a
                        // replacement event
                        if (fEvtReq)
                            {
                            evtRaise = new CacheEvent(this, ENTRY_DELETED,
                                    oKey, null, null, fEviction);
                            }
                        }
                    }
                else
                    {
                    switch (evtRaise.getId())
                        {
                        case ENTRY_INSERTED:
                            warnUnfathomable(evtRaise);
                            // fall through
                        case ENTRY_UPDATED:
                            status.setBackUpToDate(false);
                            fFrontRemoved   = false;
                            fRemoveFromBack = false;
                            break;

                        case ENTRY_DELETED:
                            fFrontRemoved = true;
                            status.setEntryInFront(false);

                            // obtain the removed value if we need it
                            if (!fCheckRemovedOnly)
                                {
                                oValue = getLatestOldValue(evtRaise);
                                }
                            break;
                        }
                    }
                }

            // remove the entry from the back
            if (fRemoveFromBack && status.isEntryInBack())
                {
                Map mapBack = getBackMap();
                // do i need an event? do i need the old value?
                if (fFrontRemoved                        // already have orig + event
                    || fCheckRemovedOnly && !fEvtReq     // only need true/false
                    || mapBack instanceof ObservableMap) // back map will gen its own event
                    {
                    // remove the entry from the back map
                    fBackRemoved = mapBack.keySet().remove(oKey);

                    MapEvent evtBack = status.takeBackEvent();
                    if (evtBack == null)
                        {
                        if (fBackRemoved)
                            {
                            if (getBackMapListener() != null)
                                {
                                warnMissingEvent(oKey, ENTRY_DELETED, false);
                                }
                            }
                        else
                            {
                            // it appears that the back map did not remove
                            // the entry, so we will assume that it was
                            // already removed (even if it is still there)
                            fBackRemoved = true;
                            }
                        status.setEntryInBack(false);

                        if (fEvtReq && !fFrontRemoved)
                            {
                            // generate the missing event (but we don't know
                            // what the value was)
                            evtRaise = new CacheEvent(this, ENTRY_DELETED,
                                                      oKey, null, null, fEviction);
                            }
                        }
                    else
                        {
                        switch (evtBack.getId())
                            {
                            case ENTRY_INSERTED:
                                warnUnfathomable(evtBack);
                                // fall through
                            case ENTRY_UPDATED:
                                fBackRemoved = false;
                                // may need to raise an event to explain what
                                // happened when we tried to delete the entry
                                // from the back and failed to do so
                                if (fEvtReq)
                                    {
                                    evtRaise = new CacheEvent(this,
                                            ENTRY_UPDATED, oKey,
                                            (evtRaise == null ? evtBack : evtRaise).getOldValue(),
                                            evtBack.getNewValue(),
                                            fEviction || isSynthetic(evtBack));
                                    }
                                break;

                            case ENTRY_DELETED:
                                fBackRemoved = true;
                                status.setEntryInBack(false);

                                if (!fFrontRemoved)
                                    {
                                    // obtain the removed value if we need it
                                    if (!fCheckRemovedOnly)
                                        {
                                        oValue = getLatestOldValue(evtBack);
                                        }

                                    // use the back event ifan event is expected
                                    // and we don't have an event from the front
                                    if (fEvtReq)
                                        {
                                        evtRaise = evtBack;
                                        }
                                    }
                                break;
                            }
                        }
                    }
                else
                    {
                    // have to explicitly get the value from the back map
                    // and generate our own event
                    oValue       = mapBack.remove(oKey);
                    fBackRemoved = true;
                    if (fEvtReq)
                        {
                        evtRaise = new CacheEvent(this, ENTRY_DELETED,
                                oKey, oValue, null, fEviction);
                        }
                    status.setEntryInBack(false);
                    }
                }

            if (fCheckRemovedOnly)
                {
                boolean fRemoved = (fFrontRemoved || fBackRemoved)
                                   && !status.isEntryExistent();
                oValue = Boolean.valueOf(fRemoved);
                }
            }
        finally
            {
            // adjust the size of the OverflowMap if it changed
            if (fExists != status.isEntryExistent())
                {
                // fExists tells us that it used to exist
                adjustSize(fExists ? -1 : +1);
                }

            // clean up the expiry information
            if (!status.isEntryExistent())
                {
                long ldtExpire = status.getExpiry();
                if (ldtExpire != 0L)
                    {
                    unregisterExpiry(oKey, ldtExpire);
                    }
                }

            // raise the appropriate event (if any)
            if (fEvtReq && evtRaise != null)
                {
                // make sure the eviction event is synthetic
                if (fEviction && evtRaise.getId() == ENTRY_DELETED
                        && !isSynthetic(evtRaise))
                    {
                    final MapEvent evtOrig = evtRaise;
                    evtRaise = new CacheEvent(this, ENTRY_DELETED,
                            evtRaise.getKey(), null, null, true)
                        {
                        @Override
                        public Object getOldValue()
                            {
                            return evtOrig.getOldValue();
                            }
                        };
                    }

                dispatchEvent(status, evtRaise);
                }

            // release the status (except for the eviction processing, which
            // manages the status on its own)
            if (!fEviction)
                {
                endKeyProcess(oKey, status);
                }
            }

        return oValue;
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
    * Determine the "time to live" for each individual cache entry.
    *
    * @return the number of milliseconds that a cache entry value will live,
    *         or {@link CacheMap#EXPIRY_NEVER EXPIRY_NEVER} if the entries
    *         never expire by default
    */
    public int getExpiryDelay()
        {
        int cMillis = m_cExpiryDelay;
        return cMillis <= 0 ? (int) EXPIRY_NEVER : cMillis;
        }

    /**
    * Specify the "time to live" for cache entries. This does not affect
    * the already-scheduled expiry of existing entries.
    *
    * @param cMillis  the number of milliseconds to specify that cache
    *                 entries subsequently added without an expiry will
    *                 live, {@link CacheMap#EXPIRY_DEFAULT EXPIRY_DEFAULT} to
    *                 indicate that the OverflowMap's default expiry be used
    *                 (which is to never automatically expire values), or
    *                 {@link CacheMap#EXPIRY_NEVER EXPIRY_NEVER} to specify
    *                 that cache entries subsequently added without an expiry
    *                 will never expire
    */
    public void setExpiryDelay(int cMillis)
        {
        if (!isExpiryEnabled() && cMillis > 0)
            {
            setExpiryEnabled(true);
            }

        m_cExpiryDelay = Math.max(cMillis, 0);
        }

    /**
    * Obtain the number of entries in the OverflowMap.
    *
    * @return the cached size of the OverflowMap
    */
    protected int getSize()
        {
        return m_countItems.get();
        }

    /**
    * Update the number of entries in the OverflowMap.
    *
    * @param cItems the cached size of the OverflowMap
    */
    protected void setSize(int cItems)
        {
        m_countItems.set(cItems);
        }

    /**
    * Adjust the number of entries in the OverflowMap.
    *
    * @param cItems  the number of items to adjust the cached size of the
    *                OverflowMap by, for example +1 or -1
    */
    protected void adjustSize(int cItems)
        {
        m_countItems.addAndGet(cItems);
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
    * Obtain the array of keys that have an expiration, indexed by expiration
    * times.
    *
    * @return a sparse LongArray keyed by expiry time with a corresponding
    *         value of a set of keys that expire at that time
    */
    protected LongArray getExpiryArray()
        {
        return m_laExpiry;
        }

    /**
    * Specify the array of keys that have an expiration, indexed by
    * expiration times.
    *
    * @param laExpiry  a LongArray to use to keep track of what expires when
    */
    protected void setExpiryArray(LongArray laExpiry)
        {
        m_laExpiry = laExpiry;
        }

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
        assert mapFront != null;

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
    * Get the MapListener for the back map.
    *
    * @return the MapListener for the back map
    */
    protected MapListener getBackMapListener()
        {
        return m_listenerBack;
        }

    /**
    * Specify the MapListener for the back map.
    * <p>
    * The caller is required to manage all of the thread concurrency issues
    * associated with modifying the listener.
    *
    * @param listener  the MapListener for the back map
    */
    protected void setBackMapListener(MapListener listener)
        {
        Map mapBack = getBackMap();
        assert mapBack != null;

        if (mapBack instanceof ObservableMap)
            {
            ObservableMap mapBackObs = (ObservableMap) mapBack;
            MapListener listenerOld = m_listenerBack;
            if (listener != listenerOld)
                {
                if (listenerOld != null)
                    {
                    mapBackObs.removeMapListener(listenerOld);
                    m_listenerBack = null;
                    }

                if (listener != null)
                    {
                    mapBackObs.addMapListener(listener);
                    m_listenerBack = listener;
                    }
                }
            }
        else if (listener != null)
            {
            throw new UnsupportedOperationException("back map is not"
                    + " observable: map=" + toString(mapBack.getClass())
                    + ", listener=" + toString(listener.getClass()));
            }
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
    public void setNullValuesAllowed(boolean fAllowNulls)
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
                               + " the OverflowMap contains null values");
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
    * Determine if this OverflowMap supports entry expiry.
    *
    * @return true iff the entry expiry feature is supported
    */
    public boolean isExpiryEnabled()
        {
        return m_fExpiryEnabled;
        }

    /**
    * Specify whether or not entry expiry is supported. Since there is a
    * per-entry (memory) cost just to support the expiry feature, and since
    * there is additional memory cost (and a small performance cost) to
    * keep track of entries that are set to expire, this feature is made
    * optional.
    * <p>
    * Note that this feature must be enabled <b>before</b> populating the
    * cache.
    *
    * @param fEnableExpiry  pass true to enable entry expiry, false to
    *                       disable it
    */
    public void setExpiryEnabled(boolean fEnableExpiry)
        {
        beginMapProcess();
        try
            {
            synchronized (this)
                {
                if (fEnableExpiry != isExpiryEnabled() && !getStatusMap().isEmpty())
                    {
                    throw new IllegalStateException("ExpiryEnabled must be"
                            + " configured before populating the OverflowMap");
                    }

                m_fExpiryEnabled = fEnableExpiry;

                // expiry requires a secondary data structure for keeping
                // track of what expires when; either create one or discard
                // the previously existing one as appropriate
                if (fEnableExpiry != (getExpiryArray() != null))
                    {
                    setExpiryArray(fEnableExpiry ? new SparseArray() : null);
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
        Gate    gate       = getGate();
        boolean fReentrant = gate.isEnteredByCurrentThread();
        gate.enter(-1);

        try
            {
            // handle up to one deferred event for any key (other than the key
            // we've been asked to process)
            if (!fReentrant)
                {
                // check for and process deferred events from the front Map
                processDeferredEvents(false);
                }

            // obtain the status for the key that we've been asked to process
            Status status = null;
            int cAttempts = 0;
            while (++cAttempts < 0xFF)
                {
                Map mapStatus = getStatusMap();
                synchronized (mapStatus)
                    {
                    status = (Status) mapStatus.get(oKey);
                    if (status == null || !status.isValid())
                        {
                        status = instantiateStatus();
                        mapStatus.put(oKey, status);
                        }
                    } // must exit sync for Map before entering sync for Status

                if (prepareStatus(oKey, status))
                    {
                    return status;
                    }
                }

            // assertion: apparent infinite loop
            throw new IllegalStateException("Overflow Map was unable to"
                    + " obtain and prepare the Status for \"" + oKey
                    + "\" for processing (Status=" + status + ")");
            }
        catch (Error e)
            {
            gate.exit();
            throw e;
            }
        catch (RuntimeException e)
            {
            gate.exit();
            throw e;
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
        closeStatus(status);
        releaseClosedStatus(oKey);
        getGate().exit();
        }

    /**
    * Block until this thread has exclusive access to perform operations
    * against the OverflowMap.
    */
    protected void beginMapProcess()
        {
        // prevent re-entrancy (which would cause deadlock)
        Gate gate = getGate();
        if (gate.isEnteredByCurrentThread())
            {
            throw new IllegalStateException("OverflowMap map-level operations"
                    + " are not permitted from re-entrant code");
            }

        gate.close(-1);
        gate.enter(-1); // by entering, we can later determine re-entrancy

        try
            {
            // take ownership of all of the Status objects
            Map mapStatus = getStatusMap();
            Map mapEvents = new HashMap();
            synchronized (mapStatus)
                {
                for (Iterator iter = mapStatus.entrySet().iterator(); iter.hasNext(); )
                    {
                    Map.Entry entry  = (Map.Entry) iter.next();
                    Object    oKey   = entry.getKey();
                    Status    status = (Status) entry.getValue();
                    synchronized (status)
                        {
                        if (status.isValid())
                            {
                            MapEvent evt = status.waitForAvailable();
                            if (evt != null || status.isExpired() && status.isEntryExistent())
                                {
                                // defer processing until we get out of the
                                // synchronized block
                                mapEvents.put(oKey, evt);
                                }
                            }
                        else
                            {
                            iter.remove();
                            }
                        }
                    }
                }

            // process deferred events and expirations
            for (Iterator iter = mapEvents.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry  = (Map.Entry) iter.next();
                Object    oKey   = entry.getKey();
                Status    status = (Status) mapStatus.get(oKey);
                MapEvent  evt    = (MapEvent) entry.getValue();
                do
                    {
                    boolean fEvict = false;
                    if (status != null)
                        {
                        synchronized (status)
                             {
                             MapEvent evtJustHappened = status.closeProcessing();
                             if (evtJustHappened != null)
                                 {
                                 evt = evt == null
                                        ? evtJustHappened
                                        : mergeEvents(evt, evtJustHappened);
                                 }

                             if (evt == null && status.isExpired() && status.isEntryExistent())
                                 {
                                 fEvict = true;
                                 }
                             }
                        }

                    // handle the eviction of the entry (which must occur outside a sync
                    // on Status)
                    if (fEvict)
                        {
                        removeInternal(oKey, true, true);
                        synchronized (status)
                            {
                            if (status.isProcessing())
                                {
                                evt = status.closeProcessing();
                                }
                            }
                        }

                    // process any deferred or new event
                    if (evt != null)
                        {
                        processEvent(status, evt);
                        evt = null;
                        }

                    synchronized (mapStatus)
                        {
                        status = (Status) mapStatus.get(oKey);
                        if (status != null)
                            {
                            synchronized (status)
                                {
                                if (!status.isValid() ||
                                        status.commitAndMaybeInvalidate())
                                    {
                                    mapStatus.remove(oKey);
                                    }
                                else
                                    {
                                    evt = status.waitForAvailable();
                                    }
                                }
                            }
                        }
                    }
                while (evt != null);
                }
            }
        catch (Throwable e)
            {
            try
                {
                // try to undo any damage; unfortunately, this will have
                // side-effects if the exception occurs on a recursive
                // map-wide operation, because it will "unlock" all Status
                // objects, and not just those processed immediately above
                endMapProcess();
                }
            catch (Exception eUnhandleable)
                {
                err("Overflow Map encountered an unhandleable "
                        + " exception while releasing exclusive ownership of"
                        + " the Overflow Map for the current thread (\""
                        + Thread.currentThread().getName() + "\"); exception:"
                        + "\n" + getStackTrace(eUnhandleable));
                }

            gate.exit();
            gate.open();

            if (e instanceof Error)
                {
                throw (Error) e;
                }
            else
                {
                throw ensureRuntimeException(e);
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
        Map mapEvents = new HashMap();
        synchronized (mapStatus)
            {
            for (Iterator iter = mapStatus.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry  = (Map.Entry) iter.next();
                Status    status = (Status) entry.getValue();
                // only clean up status objects owned by this thread (just in
                // case this is being called from the exception handler in
                // the beginMapProcess() method)
                if (!status.isValid())
                    {
                    iter.remove();
                    }
                else if (status.isOwnedByCurrentThread())
                    {
                    synchronized (status)
                        {
                        boolean fCommit = true;
                        if (status.isProcessing())
                            {
                            if (status.hasEvent())
                                {
                                mapEvents.put(entry.getKey(), status);
                                fCommit = false;
                                }
                            else
                                {
                                status.closeProcessing();
                                }
                            }

                        if (fCommit && status.commitAndMaybeInvalidate())
                            {
                            iter.remove();
                            }
                        }
                    }
                }
            }

        // process all pending events (now that we're outside of the nested
        // synchronization blocks)
        for (Iterator iter = mapEvents.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry   = (Map.Entry) iter.next();
            Status    status  = (Status) entry.getValue();
            if (status.isValid() && status.isOwnedByCurrentThread())
                {
                closeStatus(status);
                releaseClosedStatus(entry.getKey());
                }
            }

        Gate gate = getGate();
        gate.exit();
        gate.open();
        }

    /**
    * Validate the status and process any deferred events or pending expiry.
    *
    * @param oKey    the entry key
    * @param status  the Status object for the entry
    *
    * @return true iff the Status is in the processing state and oready to be
    *         used
    */
    protected boolean prepareStatus(Object oKey, Status status)
        {
        boolean  fPrepared;
        boolean  fOwned = false;
        boolean  fEvict = false;
        MapEvent evt    = null;
        synchronized (status)
             {
             // verify that this Status is *the* Status for that key
             fPrepared = status.isValid();
             if (fPrepared)
                 {
                 // check for a deferred event
                 evt = status.waitForAvailable();
                 fOwned = true;
                 if (evt != null)
                     {
                     // close the processing (to allow for re-entrancy); the
                     // additional event could have occurred while this
                     // thread was waiting
                     MapEvent evtJustHappened = status.closeProcessing();
                     if (evtJustHappened != null)
                         {
                         evt = mergeEvents(evt, evtJustHappened);
                         }
                     fPrepared = false;
                     }
                 else if (status.isExpired() && status.isEntryExistent())
                     {
                     fEvict    = true;
                     fPrepared = false;
                     }
                 }
             }

        // handle the eviction of the entry (which must occur outside a sync
        // on Status)
        if (fEvict)
            {
            removeInternal(oKey, true, true);
            synchronized (status)
                {
                if (status.isProcessing())
                    {
                    evt = status.closeProcessing();
                    }
                }
            }

        // process any deferred or new event
        try
            {
            if (evt != null)
                {
                processEvent(status, evt);
                }
            }
        finally
            {
            if (fOwned && !fPrepared)
                {
                // we were asked to validate the status, which means
                // to return it in a processing state, but we've
                // closed it, so the entire thing has to be repeated
                releaseClosedStatus(oKey);
                }
            }

        return fPrepared;
        }


    // ----- event handling -------------------------------------------------

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
                    if (status == null || !status.isValid())
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
    * Handle an event occuring against the "back" map.
    *
    * @param evtBack  an event from the back map
    */
    protected void onBackEvent(MapEvent evtBack)
        {
        // an event will always be a re-entrant condition unless one of the
        // two simple contracts are broken:
        // 1) daemon threads evicting data
        // 2) someone modifying the back map directly
        Gate       gate       = getGate();
        boolean    fReentrant = gate.isEnteredByCurrentThread();
        if (!fReentrant)
            {
            gate.enter(-1);
            }

        try
            {
            Map    mapStatus = getStatusMap();
            Object oKey      = evtBack.getKey();
            Status status;
            while (true)
                {
                synchronized (mapStatus)
                    {
                    status = (Status) mapStatus.get(oKey);
                    if (status == null || !status.isValid())
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
                        if (status.registerBackEvent(evtBack))
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
    * Merge two events that have been raised in sequence from a given
    * map.
    *
    * @param evtOld  the first event
    * @param evtNew  the second event
    *
    * @return the merged event
    */
    protected static MapEvent mergeEvents(MapEvent evtOld, MapEvent evtNew)
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
                // while these technically cancel each other out, it could
                // lead to a loss of data, for example if an evict of a key
                // from another thread occurs during a put of that object
                // that inserted that key
                if (!isSynthetic(evtNew))
                    {
                    fCreate   = false;
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
            ObservableMap map          = evtNew.getMap();
            Object        oKey         = evtNew.getKey();
            Object        oValueOld    = evtOld.getOldValue();
            Object        oValueNew    = evtNew.getNewValue();
            boolean       fSynthetic   = isSynthetic(evtNew);

            // use the old value from the new event to prevent the most
            // recent value from being lost if an entry is in flux because it
            // has disappeared from both the front and back maps
            evtResult = nId == MapEvent.ENTRY_DELETED
                    ? new HistoricCacheEvent(map, nId, oKey, oValueOld,
                            oValueNew, fSynthetic, evtNew.getOldValue())
                    : new CacheEvent(map, nId, oKey, oValueOld, oValueNew,
                            fSynthetic);
            }

        return evtResult;
        }

    /**
    * Obtain the latest old value from the passed event. The latest old value
    * is the value that must be retained upon the eviction of data from the
    * front map.
    *
    * @param evt  an event (which may be a HistoricCacheEvent)
    *
    * @return the latest old value from the event
    */
    protected static Object getLatestOldValue(MapEvent evt)
        {
        return evt instanceof HistoricCacheEvent
                ? ((HistoricCacheEvent) evt).getLatestOldValue()
                : evt.getOldValue();
        }

    /**
    * Process an arbitrary event that has occurred against either the front
    * or back map.
    *
    * @param status  the Status object representing the Overflow Map's
    *                entry against which the event was raised
    * @param evt     the MapEvent to process
    */
    protected void processEvent(Status status, MapEvent evt)
        {
        if (evt != null)
            {
            Map map = evt.getMap();
            if (map == getFrontMap())
                {
                processFrontEvent(status, evt);
                }
            else
                {
                assert map == getBackMap();
                processBackEvent(status, evt);
                }
            }
        }

    /**
    * Process an event. The Status must already be owned by this thread and
    * in the processing or committing state.
    *
    * @param status    the status
    * @param evtFront  the event to process; may be null
    */
    protected void processFrontEvent(Status status, MapEvent evtFront)
        {
        assert status.isOwnedByCurrentThread();
        assert status.isProcessing() || status.isCommitting();

        if (evtFront != null)
            {
            MapEvent evtRaise = null;
            boolean  fExists  = status.isEntryExistent();

            // originally, the entire following switch was enclosed in a
            // synchronized block, but that causes deadlock to be possible,
            // and the explanation for the synchronization appears to have
            // been incorrect:
            //      "it is necessary to synchronize on the status at this
            //      point to prevent the registration of further events
            //      against the same entry"
            // the reason that the comment appears to be incorrect is that
            // subsequent events will be not be lost, and will be processed
            // correctly by this thread (or deferred for another); remember
            // that the Status is currently owned by this thread, so no
            // other thread can legally take action against this entry
            switch (evtFront.getId())
                {
                case ENTRY_INSERTED:
                    status.setEntryInFront(true);

                    // the data in the overflow map has changed; it is
                    // either an insert (no entry in back map) or an
                    // update (if there is an entry in the back map)
                    if (status.isEntryInBack())
                        {
                        Object  oKey      = evtFront.getKey();
                        Object  oNewValue = evtFront.getNewValue();
                        Object  oOldValue = getBackMap().get(oKey);
                        boolean fUpToDate = oNewValue == oOldValue;

                        // check for an even on the back map, since it is
                        // possible that by accessing the back map, it
                        // evicted out its value, etc.
                        MapEvent evtBack = status.takeBackEvent();
                        if (evtBack != null)
                            {
                            if (!isSynthetic(evtBack))
                                {
                                warnUnfathomable(evtBack);
                                }

                            switch (evtBack.getId())
                                {
                                case ENTRY_UPDATED:
                                    oOldValue = evtBack.getOldValue();
                                    // fall through
                                case ENTRY_INSERTED:
                                    // the new value in the back map is
                                    // hidden by the new value in the
                                    // front map, but it does affect
                                    // whether or not the back is "up to
                                    // date"; the only way the back is up
                                    // to date is it happens to have the
                                    // same exact reference in it
                                    fUpToDate = (oNewValue == evtBack.getNewValue());
                                    break;

                                case ENTRY_DELETED:
                                    // the entry has been inserted into
                                    // the front and evicted out of the
                                    // back; it's a net update (at most)
                                    // from the point of view of a client
                                    // of the OverflowMap
                                    status.setEntryInBack(false);
                                    fUpToDate = false;

                                    // the previous value that the client
                                    // of the OverflowMap would have seen
                                    // is the value that has been evicted
                                    // from the back
                                    oOldValue = evtBack.getOldValue();
                                    break;
                                }
                            }

                        status.setBackUpToDate(fUpToDate);

                        if (oNewValue != oOldValue && hasListeners())
                            {
                            evtRaise = new CacheEvent(this, ENTRY_UPDATED,
                                    oKey, oOldValue, oNewValue,
                                    isSynthetic(evtFront));
                            }
                        }
                    else
                        {
                        // dispatch an insert event from the overflow map
                        evtRaise = evtFront;
                        }
                    break;

                case ENTRY_UPDATED:
                    status.setEntryInFront(true);

                    // we could compare the old and new values to
                    // determine if a change has actually occurred, and
                    // if it hasn't, then we could choose to NOT mark the
                    // back as out of date, but a mutable value could
                    // change in the front without the reference actually
                    // changing
                    status.setBackUpToDate(false);

                    // an update to the front map is an update to the
                    // overflow map, since both the previous and new
                    // values are visible through the OverflowMap
                    evtRaise = evtFront;
                    break;

                case ENTRY_DELETED:
                    status.setEntryInFront(false);

                    // if the back is up to date, then the deletion from
                    // the front is not visible (since the OverflowMap
                    // still contains the same value)
                    if (!status.isBackUpToDate())
                        {
                        // on the other hand, if the back is not up to
                        // date, then the eviction from the front must
                        // force a write to the back map
                        Object oKey      = evtFront.getKey();
                        Object oOldValue = getLatestOldValue(evtFront);
                        putOne(getBackMap(), oKey, oOldValue, true);

                        // assume that the write to the back succeeded ..
                        status.setEntryInBack(true);
                        status.setBackUpToDate(true);

                        // .. unless an event tells us differently
                        MapEvent evtBack = status.takeBackEvent();
                        if (evtBack != null)
                            {
                            switch (evtBack.getId())
                                {
                                case ENTRY_INSERTED:
                                case ENTRY_UPDATED:
                                    Object oNewValue = evtBack.getNewValue();
                                    if (oNewValue != oOldValue && hasListeners())
                                        {
                                        // somehow when we put the value
                                        // into the back map, it changed,
                                        // so dispatch an updated event
                                        evtRaise = new CacheEvent(this,
                                                ENTRY_UPDATED, oKey,
                                                oOldValue, oNewValue,
                                                isSynthetic(evtBack));
                                        }
                                    break;

                                case ENTRY_DELETED:
                                    if (!isSynthetic(evtBack))
                                        {
                                        warnUnfathomable(evtBack);
                                        }

                                    // COH-1061: entry was evicted from the back
                                    status.setEntryInBack(false);

                                    // we've lost the value from the
                                    // front and the back, so it's been
                                    // lost from the OverflowMap
                                    evtRaise = evtFront;
                                    break;
                                }
                            }
                        }
                    break;
                }

            // adjust the size of the OverflowMap if it changed
            if (fExists != status.isEntryExistent())
                {
                // fExists tells us that it used to exist
                adjustSize(fExists ? -1 : +1);
                }

            // issue an event from the OverflowMap if there one resulted
            if (evtRaise != null)
                {
                dispatchEvent(status, evtRaise);
                }
            }
        }

    /**
    * Process an event. The Status must already be owned by this thread and
    * in the processing or committing state.
    *
    * @param status   the status
    * @param evtBack  the event to process; may be null
    */
    protected void processBackEvent(Status status, MapEvent evtBack)
        {
        if (evtBack != null)
            {
            // the event changed the value in the back, therefore it must
            // be assumed to be out of date now
            status.setBackUpToDate(false);

            boolean fIsInFront = status.isEntryInFront();
            boolean fWasInBack = status.isEntryInBack();
            boolean fNowInBack = evtBack.getId() != ENTRY_DELETED;
            if (fNowInBack != fWasInBack)
                {
                status.setEntryInBack(fNowInBack);

                // check if the size of the OverflowMap has changed; if the
                // entry is not in the front, then a change to the existence
                // in the back signifies a change to the existence in the
                // OverflowMap itself
                if (!fIsInFront)
                    {
                    adjustSize(fWasInBack ? -1 : +1);
                    }
                }

            if (!fIsInFront)
                {
                // the event from the back map is going to be visible
                // "through" the overflow map because it is not hidden
                // by an entry in the front map
                dispatchEvent(status, evtBack);
                }
            }
        }

    /**
    * Process deferred events, if there are any.
    *
    * @param fProcessAll  pass true to process all pending events, false to
    *                     process just a fair portion of them
    */
    protected void processDeferredEvents(boolean fProcessAll)
        {
        List listDeferred = getDeferredList();

        // check to see if there are any keys to expire
        if (isExpiryEnabled())
            {
            // transfer any expired keys to the deferred list
            LongArray laExpiry = getExpiryArray();
            synchronized (laExpiry)
                {
                long ldtCurrent = getSafeTimeMillis();
                long ldtFirst   = laExpiry.getFirstIndex();
                if (ldtCurrent >= ldtFirst)
                    {
                    for (LongArray.Iterator iter = laExpiry.iterator(); iter.hasNext(); )
                        {
                        Set setKeys = (Set) iter.next();
                        if (ldtCurrent >= iter.getIndex())
                            {
                            listDeferred.addAll(setKeys);
                            iter.remove();
                            }
                        }
                    }
                }
            }

        // process a deferred event, if there is one
        if (!listDeferred.isEmpty())
            {
            // try to process at least ~1% of the pending events, but not
            // less than 10 events and not more than 100 events
            int cDeferred  = listDeferred.size();
            int cTarget    = fProcessAll ? cDeferred
                             : Math.max(10, Math.min(100, cDeferred >>> 7));
            int cProcessed = 0;
            Map mapStatus  = getStatusMap();

            // if anything has to be deferred, remember the first deferred
            // item to avoid looping indefinitely waiting for it to become
            // available
            Object oFirstDeferredKey = null;

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
                        boolean fRequeue = true;
                        if (status.isAvailable())
                            {
                            // process events, etc.
                            if (prepareStatus(oKey, status))
                                {
                                closeStatus(status);
                                releaseClosedStatus(oKey);
                                }

                            fRequeue = status.isValid() &&
                                       (status.hasEvent() || status.isExpired());

                            ++cProcessed;
                            }

                        if (fRequeue)
                            {
                            if (oFirstDeferredKey == null)
                                {
                                oFirstDeferredKey = oKey;
                                }
                            else if (oKey == oFirstDeferredKey)
                                {
                                // in an apparent indefinite loop
                                break;
                                }

                            // defer processing
                            listDeferred.add(oKey);
                            }
                        }
                    }
                }
            while (!listDeferred.isEmpty() && cProcessed < cTarget);
            }
        }

    /**
    * Helper method to close a Status, processing any pending events as part
    * of the process.
    *
    * @param status  the Status object to close
    */
    protected void closeStatus(Status status)
        {
        MapEvent evt;
        do
            {
            evt = null;

            synchronized (status)
                {
                if (status.isProcessing())
                    {
                    if (status.hasEvent())
                        {
                        evt = status.takeEvent();
                        }
                    else
                        {
                        status.closeProcessing();
                        }
                    }
                }

            // this has to occur outside the synchronization on the Status
            if (evt != null)
                {
                processEvent(status, evt);
                }
            }
        while (evt != null);
        }

    /**
    * Helper method to encapsulate the release of a Status object on which
    * closeProcessing() has already been called.
    *
    * @param oKey  the entry key
    */
    protected void releaseClosedStatus(Object oKey)
        {
        Map mapStatus  = getStatusMap();
        synchronized (mapStatus)
            {
            Status status = (Status) mapStatus.get(oKey);
            if (status != null)
                {
                synchronized (status)
                    {
                    if (!status.isValid() ||
                            status.commitAndMaybeInvalidate())
                        {
                        mapStatus.remove(oKey);
                        }
                    }
                }
            }
        }


    // ----- event dispatching ----------------------------------------------

    /**
    * Accessor for the MapListenerSupport for sub-classes.
    *
    * @return the MapListenerSupport, or null if there are no listeners
    */
    protected MapListenerSupport getMapListenerSupport()
        {
        return m_listenerSupport;
        }

    /**
    * Determine if the OverflowMap has any listeners at all.
    *
    * @return true iff this OverflowMap has at least one MapListener
    */
    protected boolean hasListeners()
        {
        // m_listenerSupport defaults to null, and it is reset to null when
        // the last listener unregisters
        return m_listenerSupport != null;
        }

    /**
    * Dispatch an event containing the passed event information.
    *
    * @param status      the Status for the entry
    * @param nId         this event's id
    * @param oKey        the key into the map
    * @param oValueOld   the old value
    * @param oValueNew   the new value
    * @param fSynthetic  true iff the event is caused by the cache
    *                    internal processing such as eviction or loading
    */
    protected void dispatchEvent(Status status, int nId, Object oKey, Object oValueOld, Object oValueNew, boolean fSynthetic)
        {
        if (hasListeners())
            {
            MapEvent evt = new CacheEvent(this, nId, oKey, oValueOld, oValueNew, fSynthetic);
            dispatchEvent(status, evt);
            }
        }

    /**
    * Dispatch the passed event.
    *
    * @param status  the Status for the entry
    * @param evt     a MapEvent object
    */
    protected void dispatchEvent(Status status, MapEvent evt)
        {
        if (evt != null)
            {
            MapListenerSupport listenerSupport = getMapListenerSupport();
            if (listenerSupport != null)
                {
                // make sure the event reports this overflow map as its
                // source, just in case the event is from one of the front or
                // back Maps and is being reported to the listeners directly
                // (e.g. eviction from the back when there is nothing in the
                // front is the same as eviction from the overflow map itself)
                if (evt.getMap() != this)
                    {
                    // build a wrapper that delegates back to the original
                    // event; this allows us to avoid explicitly calling
                    // getOldValue() and getNewValue(), each of which may
                    // be expensive operations if the events are backed
                    // by the underlying maps; the reason that we don't have
                    // to call them is that the event being raised may not
                    // need all that information if the listener is a "lite"
                    // listener, for example
                    final MapEvent evtOrig = evt;
                    evt = new CacheEvent(this, evt.getId(), evt.getKey(),
                                         null, null, false)
                        {
                        @Override
                        public Object getOldValue()
                            {
                            return evtOrig.getOldValue();
                            }

                        @Override
                        public Object getNewValue()
                            {
                            return evtOrig.getNewValue();
                            }

                        @Override
                        public boolean isSynthetic()
                            {
                            return OverflowMap.isSynthetic(evtOrig);
                            }
                        };
                    }

                // for re-entrancy, the Status cannot still be in the
                // processing state; commit the processing, rolling up any
                // pending events, then register that event (if any) to be
                // processed later
                synchronized(status)
                    {
                    if (status.isProcessing())
                        {
                        MapEvent evtDefer = status.closeProcessing();
                        if (evtDefer != null)
                            {
                            if (evt.getMap() == getFrontMap())
                                {
                                status.registerFrontEvent(evtDefer);
                                }
                            else
                                {
                                status.registerBackEvent(evtDefer);
                                }
                            }
                        }
                    }

                // the events can only be generated while the current thread
                // holds the monitor on this overflow map; this will guarantee
                // stable behavior by supporting the same guarantee that the
                // overflow map requires from its own front map; in other
                // words, an overflow map can theoretically use an overflow
                // map for its front map (not to mention for its back map)
                // and maintain its lossless guarantees
                synchronized (this)
                    {
                    listenerSupport.fireEvent(evt, false);
                    }
                }
            }
        }

    /**
    * Helper method to determine if an event is synthetic.
    *
    * @param evt  a Map Event
    *
    * @return true if the event is a synthetic cache event
    */
    protected static boolean isSynthetic(MapEvent evt)
        {
        return evt instanceof CacheEvent && ((CacheEvent) evt).isSynthetic();
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Register an expiry for the specified key.
    *
    * @param oKey       the key of the entry to expire
    * @param ldtExpire  the time to expire the entry
    */
    protected void registerExpiry(Object oKey, long ldtExpire)
        {
        LongArray laExpiry = getExpiryArray();
        synchronized (laExpiry)
            {
            Collection collKeys = (Collection) laExpiry.get(ldtExpire);
            if (collKeys == null)
                {
                collKeys = new LiteSet();
                laExpiry.set(ldtExpire, collKeys);
                }
            collKeys.add(oKey);
            }
        }

    /**
    * Unregister the specified key expiry that is registered to expire at the
    * specified time.
    *
    * @param oKey       the key that is set to expire
    * @param ldtExpire  the time that the key is set to expire
    */
    protected void unregisterExpiry(Object oKey, long ldtExpire)
        {
        LongArray laExpiry = getExpiryArray();
        synchronized (laExpiry)
            {
            Collection collKeys = (Collection) laExpiry.get(ldtExpire);
            if (collKeys != null)
                {
                collKeys.remove(oKey);
                if (collKeys.isEmpty())
                    {
                    laExpiry.remove(ldtExpire);
                    }
                }
            }
        }

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
            err("null pointer exception occurred during putOne()"
                + "\nMap=" + map
                + "\nKey=" + oKey
                + "\nValue=" + oValue
                + "\nPutBlind=" + fPutBlind
                + "\nException=" + e);
            throw e;
            }
        }

    /**
    * Flush items that have expired.
    *
    * @since Coherence 3.2
    */
    public void evict()
        {
        // check for re-entrancy
        Gate gate = getGate();
        if (!gate.isEnteredByCurrentThread())
            {
            gate.enter(-1);
            try
                {
                evict(getFrontMap());
                evict(getBackMap());
                processDeferredEvents(true);
                }
            finally
                {
                gate.exit();
                }
            }
        }

    /**
    * If the passed Map supports it, evict its expired data.
    *
    * @param map  a Map that may or may not support the requesting of an
    *             eviction of expired data
    *
    * @since Coherence 3.2
    */
    protected static void evict(Map map)
        {
        if (map instanceof LocalCache)
            {
            ((LocalCache) map).checkFlush();
            }
        else if (map instanceof AbstractSerializationCache)
            {
            ((AbstractSerializationCache) map).evict();
            }
        else if (map instanceof OverflowMap)
            {
            ((OverflowMap) map).evict();
            }
        }

    /**
    * An expected event did not get raised. Try to report it to the log.
    *
    * @param oKey    the key for which an event is missing
    * @param nId     the event type that is missing
    * @param fFront  true if the event was expected to come from the front
    *                map, false if from the back map
    */
    protected void warnMissingEvent(Object oKey, int nId, boolean fFront)
        {
        if (!m_fLoggedMissingEvent)
            {
            m_fLoggedMissingEvent = true;
            log("Overflow Map was expecting to receive an "
                + MapEvent.getDescription(nId) + " event for the key \""
                + oKey + "\", but no event was received. This indicates"
                + " that the Observable Map implementation used as the "
                + (fFront ? "front" : "back") + " map for the Overflow"
                + " Map is not implemented correctly. This Overflow Map"
                + " instance will not repeat this warning, and will"
                + " attempt to compensate for the missing events."
                + "\nStack trace follows:\n" + getStackTrace());
            }
        }

    /**
    * Something totally inexplicable has occurred. Try to report it to the
    * log.
    *
    * @param evt  the event that cannot be explained
    */
    protected void warnUnfathomable(MapEvent evt)
        {
        if (!m_fLoggedUnfathomableEvent)
            {
            m_fLoggedUnfathomableEvent = true;
            String sMap;
            if (evt.getMap() == getFrontMap())
                {
                sMap = "the front map";
                }
            else if (evt.getMap() == getBackMap())
                {
                sMap = "the back map";
                }
            else
                {
                sMap = "some unknown map";
                }

            log("Overflow Map has received an inexplicable"
                + " event from " + sMap + "; such an event should not"
                + " have been possible to occur. The Overflow Map will"
                + " arbitrarily interpret the event in order to maintain"
                + " its own internal consistency. The likely origin of"
                + " the event is direct modification of the front and/or"
                + " back maps managed by the Overflow Map, a MapListener"
                + " making re-entrant modifications to its source map, or"
                + " an ObservableMap implementation that reacts to the Map"
                + " API in a manner inconsistent with the specification"
                + " (e.g. actively modifying its contents in a manner that"
                + " differs from the sequence of API calls made against it)."
                + " This Overflow Map instance will not repeat this warning,"
                + " and will attempt to silently compensate for subsequent"
                + " similar event irregularities."
                + "\nEvent: " + evt
                + "\nStack trace follows:\n" + getStackTrace());
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
        if (!m_sWarnedEventSequence)
            {
            m_sWarnedEventSequence = true;
            log("Overflow Map has detected"
                + " an illegal event sequence:"
                + "\nEvent 1: " + evtOld
                + "\nEvent 2: " + evtNew
                + "\nThis warning will not be repeated."
                + " Stack trace follows:\n" + getStackTrace());
            }
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected Set instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries backed by this map.
    */
    public class EntrySet
            extends AbstractKeySetBasedMap.EntrySet
        {
        // ----- inner class: Entry Set Iterator ------------------------

        /**
        * Factory pattern.
        *
        * @return a new instance of an Iterator over the EntrySet
        */
        @Override
        protected Iterator instantiateIterator()
            {
            return new EntrySetIterator();
            }

        /**
        * A pessimistic Iterator over the EntrySet that is backed by the
        * OverflowMap.
        */
        protected class EntrySetIterator
                extends AbstractStableIterator
            {
            // ----- AbstractStableIterator methods -----------------

            /**
            * {@inheritDoc}
            */
            @Override
            protected void advance()
                {
                OverflowMap mapOverflow = OverflowMap.this;
                Map         mapTemp     = m_mapTemp;
                for (Iterator iter = m_iterKeys; iter.hasNext(); )
                    {
                    Object oKey = m_iterKeys.next();
                    mapOverflow.getInternal(oKey, false, mapTemp);
                    if (!mapTemp.isEmpty())
                        {
                        setNext(instantiateEntry(oKey, mapTemp.remove(oKey)));
                        break;
                        }
                    }
                }

            /**
            * {@inheritDoc}
            */
            @Override
            protected void remove(Object oPrev)
                {
                m_iterKeys.remove();
                }

            // ----- data members -----------------------------------

            /**
            * Key iterator.
            */
            protected Iterator m_iterKeys = OverflowMap.this.iterateKeys();

            /**
            * Map to use with getInternal() in order to determine differences
            * between null values and non-present values.
            */
            protected Map m_mapTemp = new LiteMap();
            }
        }


    // ----- inner class: InternalKeySet ------------------------------------

    /**
    * Factory pattern: Create a read-only Set of keys in the Overflow Map
    *
    * @return a new instance of Set that represents the keys in the Map
    */
    protected Set instantiateInternalKeySet()
        {
        return new InternalKeySet();
        }

    /**
    * A read-only set of keys backed by this map.
    */
    protected class InternalKeySet
            extends AbstractSet
        {
        // ----- Set interface ------------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        public boolean contains(Object o)
            {
            return OverflowMap.this.containsKey(o);
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public boolean isEmpty()
            {
            return OverflowMap.this.isEmpty();
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public Iterator iterator()
            {
            return new InternalKeySetIterator();
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public int size()
            {
            return OverflowMap.this.size();
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public Object[] toArray(Object ao[])
            {
            Object[] aoKey;

            // get rid of expired data
            OverflowMap mapOverflow = OverflowMap.this;
            mapOverflow.evict();

            // re-entrancy could theoretically cause deadlock if two threads
            // were trying to do the same thing, e.g. if an event listener on
            // the OverflowMap were to call this method
            Gate       gate       = mapOverflow.getGate();
            boolean    fReentrant = gate.isEnteredByCurrentThread();
            if (fReentrant)
                {
                // this is inefficient, but should avoid deadlock
                aoKey = SimpleEnumerator.toArray(iterator(),
                        ao == null ? EMPTY_ARRAY : ao);
                }
            else
                {
                // take ownership of the OverflowMap to keep the size stable
                gate.close(-1);
                try
                    {
                    // prepare the array to hold the keys
                    int cKeys = mapOverflow.size();
                    if (ao == null)
                        {
                        aoKey = new Object[cKeys];
                        }
                    else
                        {
                        int cElements = ao.length;
                        if (cKeys > cElements)
                            {
                            aoKey = (Object[]) Array.newInstance(
                                     ao.getClass().getComponentType(), cKeys);
                            }
                        else
                            {
                            aoKey = ao;
                            if (cKeys < cElements)
                                {
                                // put a "null terminator" immediately
                                // following the keys that are going to be
                                // stored in the passed array
                                aoKey[cKeys] = null;
                                }
                            }
                        }

                    // grab all the keys in the OverflowMap (using the status
                    // map, which does not look at either the front or back)
                    int iKey = 0;
                    for (Iterator iter = mapOverflow.getStatusMap()
                            .entrySet().iterator(); iter.hasNext(); )
                        {
                        Map.Entry entry  = (Map.Entry) iter.next();
                        Status    status = (Status) entry.getValue();
                        if (status.isEntryExistent())
                            {
                            aoKey[iKey++] = entry.getKey();
                            }
                        }
                    assert iKey == cKeys;
                    }
                finally
                    {
                    gate.open();
                    }
                }

            return aoKey;
            }

        // ----- inner class: KeyIterator ---------------------------

        /**
        * An Iterator implementation over the keys in the OverflowMap that
        * that is based on a concurrent Iterator over the internal status
        * map.
        */
        protected class InternalKeySetIterator
                extends AbstractStableIterator
            {
            // ----- constructors -----------------------------------

            /**
            * Default constructor.
            */
            public InternalKeySetIterator()
                {
                m_iter = OverflowMap.this.getStatusMap().entrySet().iterator();
                }

            // ----- AbstractStableIterator methods -----------------

            /**
            * {@inheritDoc}
            */
            @Override
            protected void advance()
                {
                for (Iterator iter = m_iter; iter.hasNext(); )
                    {
                    Map.Entry entry  = (Map.Entry) iter.next();
                    Status    status = (Status) entry.getValue();
                    if (status.isEntryExistent())
                        {
                        setNext(entry.getKey());
                        break;
                        }
                    }
                }

            /**
            * {@inheritDoc}
            * @see OverflowMap#isInternalKeySetIteratorMutable()
            */
            @Override
            protected void remove(Object oPrev)
                {
                OverflowMap.this.removeBlind(oPrev);
                }

            // ----- data members -----------------------------------

            /**
            * The underlying iterator of status map entries.
            */
            private Iterator m_iter;
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
        return isExpiryEnabled() ? new ExpirableStatus()
               : new Status();
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
        * Construct a Status object.
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
        * Determine if the entry for which this Status exists is present in
        * the back map.
        *
        * @return true iff  the entry is stored in the back map
        */
        public boolean isEntryInBack()
            {
            return extractFlag(STATE_MASK_BACK);
            }

        /**
        * Specify whether the entry for which this Status exists is present
        * in the back map.
        *
        * @param fEntryInBack  pass true if the entry is stored in the back
        *                      map, false if not
        */
        public void setEntryInBack(boolean fEntryInBack)
            {
            updateFlag(STATE_MASK_BACK, fEntryInBack);

            // if the entry is not in the back, then the back is definitely
            // not up to date
            if (!fEntryInBack)
                {
                setBackUpToDate(false);
                }
            }

        /**
        * Determine if the entry for which this Status exists is present in
        * the front map or the back map.
        *
        * @return true iff  the entry is stored in either the front or the
        *         back map
        */
        public boolean isEntryExistent()
            {
            return extractState(STATE_MASK_EXISTS) != 0;
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
        * @param fUpToDate  the flag indicate back up to date
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
        * For internal use only, return the current event from the back
        * Map. All handling of synchronization etc. is the responsibility of
        * the sub-class.
        *
        * @return the cummulative back Map event for the Entry represented
        *         by this Status object, or null if there were no events
        */
        protected MapEvent getBackEvent()
            {
            return m_evtBack;
            }

        /**
        * For internal use only, store the current event from the back Map.
        * All handling of synchronization etc. is the responsibility of the
        * sub-class.
        *
        * @param evt  the cummulative back Map event for the Entry
        *             represented by this Status object, or null to clear
        *             the event
        */
        protected void setBackEvent(MapEvent evt)
            {
            m_evtBack = evt;
            }

        /**
        * Determine if an event has occurred against the Entry for which this
        * Status exists.
        *
        * @return true iff an event is held by the Status
        */
        public boolean hasEvent()
            {
            return getFrontEvent() != null || getBackEvent() != null;
            }

        /**
        * Obtain the most recent front Map event that has occurred against
        * the Entry for which this Status exists.
        *
        * @return the cummulative front Map event for the Entry represented
        *         by this Status object, or null if there were no events
        */
        public synchronized MapEvent takeFrontEvent()
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
        * Obtain the most recent back Map event that has occurred against
        * the Entry for which this Status exists.
        *
        * @return the cummulative back Map event for the Entry represented
        *         by this Status object, or null if there were no events
        */
        public synchronized MapEvent takeBackEvent()
            {
            assert isProcessing() || isCommitting();

            MapEvent evt = getBackEvent();
            if (evt != null)
                {
                setBackEvent(null);
                }

            return evt;
            }

        /**
        * Obtain the most recent event that has occurred against the Entry
        * for which this Status exists. If both the front and the back Map
        * have event listeners which register events with the Status object,
        * then the event could be from either the front or the back Map. If
        * events have occurred on both the front and back Maps, the event
        * from the front Map will be returned and the event from the back Map
        * will be discarded; this can be explained by the fact that the front
        * Map overlays the back Map, and thus the back Map events can be
        * safely ignored if a front Map event has occurred. (This is possible
        * because the back Map content is not monitored strictly, but the
        * front Map content is.)
        *
        * @return the cummulative event for the Entry represented by this
        *         Status object, or null if there were no events
        */
        public synchronized MapEvent takeEvent()
            {
            MapEvent evt = takeFrontEvent();
            if (evt == null)
                {
                evt = takeBackEvent();
                }
            else
                {
                MapEvent evtBack = takeBackEvent();;
                if (evtBack != null)
                    {
                    // problem: there is both an event on the front AND on the
                    // back, but we can only return one of them; since the front
                    // hides the back, that means that the front event needs to
                    // be returned, but the results of the back event need to
                    // first be incorporated into the status
                    setEntryInBack(evtBack.getId() != MapEvent.ENTRY_DELETED);
                    setBackUpToDate(false);
                    }
                }

            return evt;
            }

        /**
        * Determine the expiry for the entry represented by this Status.
        *
        * @return the expiry, or 0L if there is no expiry
        */
        public long getExpiry()
            {
            return 0L;
            }

        /**
        * Specify the expiry for the entry represented by this Status.
        *
        * @param ldtExpires  the expiry, or 0L if the entry should not expire
        */
        public void setExpiry(long ldtExpires)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * Determine if this Status represents an entry that will
        * automatically expire.
        *
        * @return true iff the Status is for an entry that will expire
        */
        public boolean hasExpiry()
            {
            return false;
            }

        /**
        * Determine if this Status represents an entry that will
        * automatically expire.
        *
        * @return true iff the Status is for an entry that will expire
        */
        public boolean isExpired()
            {
            return false;
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
            // && !isEntryInBack() && !hasEvent();
            return extractState(STATE_MASK_RETAIN) == 0 && !hasEvent();
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
                    + ", EntryInBack=" + isEntryInBack()
                    + ", EntryExistent=" + isEntryExistent()
                    + ", BackUpToDate=" + isBackUpToDate()
                    + ", hasEvent=" + hasEvent()
                    + ", FrontEvent=" + getFrontEvent()
                    + ", BackEvent=" + getBackEvent()
                    + ", cWaiting=" + m_cWaiting
                    + ", cEnters=" + m_cEnters
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
        *
        * @return whatever event was deferred for this Status which the
        *         caller must handle
        */
        protected synchronized MapEvent waitForAvailable()
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
                        if (isCommitting())
                            {
                            break;
                            }
                        else
                            {
                            throw new IllegalStateException("Re-entrancy"
                                + " requires that the Status be Committing"
                                + " (Status=" + getStatus() + ")");
                            }
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

            // update re-entrancy count
            int cEnters = m_cEnters & 0x000000FF;
            if (cEnters == 0x000000FF)
                {
                throw new IllegalStateException(
                    "Exceeded maximum depth of re-entrancy"
                    + " (Status=" + getStatus() + ")");
                }
            m_cEnters = (byte) (cEnters + 1);

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
        * before calling this method (or otherwise synchronizing on this
        * Status object). Failure to follow this rule will result in deadlock
        * and/or exceptional conditions.
        *
        * @return true iff this Status object has invalidated itself
        */
        protected synchronized boolean commitAndMaybeInvalidate()
            {
            assert isCommitting();
            assert m_cEnters != 0;

            boolean fInvalidated = false;
            if (--m_cEnters == 0)
                {
                setStatus(STATUS_AVAILABLE);
                if (m_cWaiting != 0)
                    {
                    // notify the next thread in line
                    notify();
                    }
                else if (isDiscardable())
                    {
                    setStatus(STATUS_INVALIDATED);
                    fInvalidated = true;
                    }

                setOwnerThread(null);
                }
            else
                {
                // this is the equivalent of a recursive "exit"
                setStatus(STATUS_COMMITTING);
                }

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

            // force loading of the event's "old value", since some Map
            // implementations do not pre-populate the event with the old
            // value, choosing instead to load it lazily
            evt.getOldValue();

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
        * Register a MapEvent that has been raised by the back Map against
        * the same key for which this Status object exists. If an event has
        * previously been registered, the previous and new event are merged
        * into a single merged event that incorporates the data from both
        * events.
        *
        * @param evt  the event that has occurred against an entry in the
        *             back Map with the same key which this Status object
        *             represents
        *
        * @return true iff the event processing has been deferred, implying
        *         that this Status object should be registered in a list of
        *         Status objects that have events that need to be handled
        */
        public synchronized boolean registerBackEvent(MapEvent evt)
            {
            assert isValid();

            if (!isEntryInFront())
                {
                // force loading of the event's "old value", since some Map
                // implementations do not pre-populate the event with the old
                // value, choosing instead to load it lazily
                evt.getOldValue();
                }

            MapEvent evtOld = getBackEvent();
            if (evtOld != null)
                {
                evt = mergeEvents(evtOld, evt);
                }
            setBackEvent(evt);

            // not deferred if the Status is owned and in the processing
            // stage (since the event will be handled by the owning thread at
            // the end of the processing stage)
            return !isProcessing();
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
        * Bitmask for entry in back.
        */
        protected static final int STATE_MASK_BACK      = 0x10;
        /**
        * Bitmask for value in front and back being in sync.
        */
        protected static final int STATE_MASK_INSYNC    = 0x20;

        /**
        * Bitmask for fields that would indicate that the Status represents
        * an existent entry in the OverflowMap.
        */
        protected static final int STATE_MASK_EXISTS    =
                STATE_MASK_FRONT | STATE_MASK_BACK;

        /**
        * Bitmask for fields that would indicate that the Status must not be
        * discarded.
        */
        protected static final int STATE_MASK_RETAIN    =
                STATE_MASK_STATUS | STATE_MASK_FRONT | STATE_MASK_BACK;

        /**
        * Status: The Status object exists and no thread is currently
        * performing processing against the associated entry.
        */
        protected static final int STATUS_AVAILABLE     = 0x00;
        /**
        * Status: The Status object represents an Entry that is currently
        * being processed.
        */
        protected static final int STATUS_PROCESSING    = 0x01;
        /**
        * Status: The Status object represents an Entry that was very
        * recently being processed, and is currently finalizing the results
        * of that processing.
        */
        protected static final int STATUS_COMMITTING    = 0x02;
        /**
        * Status: The Status object has been discarded.
        */
        protected static final int STATUS_INVALIDATED   = 0x03;

        // ----- data members -------------------------------------------

        /**
        * The Thread that currently owns the Status object.
        */
        private Thread m_threadOwner;

        /**
        * The count of times that this Status has been entered by this
        * thread (m_threadOwner) but not exited.
        */
        private volatile byte m_cEnters;

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

        /**
        * The event (if any) that has been received for the back Map entry
        * for which this Status exists. See the additional notes on ordering
        * and synchronization on the {@link #m_evtFront m_evtFront} field.
        */
        private volatile MapEvent m_evtBack;
        }


    // ----- inner class: ExpirableStatus -----------------------------------

    /**
    * The ExpirableStatus adds expiry to the base Status object.
    */
    protected static class ExpirableStatus
            extends Status
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a Status object for a specific key.
        */
        public ExpirableStatus()
            {
            }

        // ----- accessors ----------------------------------------------

        /**
        * Determine the expiry for the entry represented by this Status.
        *
        * @return the expiry, or 0L if there is no expiry
        */
        @Override
        public long getExpiry()
            {
            return m_ldtExpires;
            }

        /**
        * Specify the expiry for the entry represented by this Status.
        *
        * @param ldtExpires  the expiry, or 0L if the entry should not expire
        */
        @Override
        public void setExpiry(long ldtExpires)
            {
            m_ldtExpires = ldtExpires;
            }

        /**
        * Determine if this Status represents an entry that will
        * automatically expire.
        *
        * @return true iff the Status is for an entry that will expire
        */
        @Override
        public boolean hasExpiry()
            {
            return getExpiry() != 0L;
            }

        /**
        * Determine if this Status represents an entry that will
        * automatically expire.
        *
        * @return true iff the Status is for an entry that will expire
        */
        @Override
        public boolean isExpired()
            {
            long ldtExpires = getExpiry();
            return ldtExpires != 0L && getSafeTimeMillis() >= ldtExpires;
            }

        /**
        * Assemble a human-readable description.
        *
        * @return a description of this Status object
        */
        @Override
        public String getDescription()
            {
            return super.getDescription()
                   + ", hasExpiry=" + hasExpiry()
                   + ", Expiry=" + formatDateTime(getExpiry())
                   + ", Expired=" + isExpired();
            }

        // ----- data members -------------------------------------------

        /**
        * The time at which this Status will expire.
        */
        private volatile long m_ldtExpires;
        }


    // ----- inner class: FrontMapListener ----------------------------------

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
            OverflowMap.this.onFrontEvent(evt);
            }
        }


    // ----- inner class: BackMapListener -----------------------------------

    /**
    * Factory pattern: Back Map Listener.
    *
    * @return a new instance of the listener for the back map
    */
    protected MapListener instantiateBackMapListener()
        {
        return new BackMapListener();
        }

    /**
    * A listener for the back map.
    */
    protected class BackMapListener
            extends MultiplexingMapListener
        {
        /**
        * {@inheritDoc}
        */
        @Override
        protected void onMapEvent(MapEvent evt)
            {
            OverflowMap.this.onBackEvent(evt);
            }
        }


    // ----- inner class: HistoricCacheEvent ----------------------------

    /**
    * A CacheEvent that carries a recent value (to avoid it being lost during
    * eviction).
    */
    protected static class HistoricCacheEvent
            extends CacheEvent
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a Historic CacheEvent that contains the most recent value
        * before the now-current value.
        *
        * @param map           the ObservableMap object that fired the event
        * @param nId           this event id as defined by the MapEvent class
        * @param oKey          the key into the map
        * @param oValueOld     the old value (for update and delete events)
        * @param oValueNew     the new value (for insert and update events)
        * @param fSynthetic    true iff the event is historically synthetic
        * @param oValueRecent  the most recent value before the new value
        */
        public HistoricCacheEvent(ObservableMap map, int nId, Object oKey,
                Object oValueOld, Object oValueNew, boolean fSynthetic,
                Object oValueRecent)
            {
            super(map, nId, oKey, oValueOld, oValueNew, fSynthetic);
            m_oValueLatestOld = oValueRecent;
            }

        // ----- accessors ----------------------------------------------

        /**
        * Obtain the value that needs to be saved if this event represents
        * a merged sequence of events ending with the eviction of data.
        *
        * @return the most recent data value before the new value
        */
        public Object getLatestOldValue()
            {
            return m_oValueLatestOld;
            }

        // ----- data members -------------------------------------------

        /**
        * A previous value, but the most recent of the previous values.
        */
        protected Object m_oValueLatestOld;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Empty array of objects.
    */
    static final Object[] EMPTY_ARRAY = new Object[0];

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


    // ----- data fields ----------------------------------------------------

    /**
    * The "front" map, which is size-limited.
    */
    private ObservableMap m_mapFront;

    /**
    * The "back" map, which the front overflows to.
    */
    private Map m_mapBack;

    /**
    * A Map for maintaining Status information on the entries that are being
    * managed by this Overflow Map.
    */
    private Map m_mapStatus = new SafeHashMap();

    /**
    * A virtual set of keys based on the status map.
    */
    private Set m_setKeysInternal;

    /**
    * The count of entries in the OverflowMap.
    */
    private AtomicInteger m_countItems = new AtomicInteger();

    /**
    * A list of keys that may have deferred events.
    */
    private List m_listDeferred = new RecyclingLinkedList();

    /**
    * The ordered expiration of particular keys.
    */
    private LongArray m_laExpiry;

    /**
    * An option to allow null values.
    */
    private boolean m_fNullValuesAllowed;

    /**
    * An option to support entry expiry.
    */
    private boolean m_fExpiryEnabled;

    /**
    * The number of milliseconds that a value will live in the cache.
    * Zero indicates no timeout.
    */
    private int m_cExpiryDelay;

    /**
    * An option to use putAll (no return value) to update the front Map.
    */
    private boolean m_fUseFrontPutAll;

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
    * The listener for the "back" map.
    */
    private MapListener m_listenerBack;

    /**
    * The listeners that are listening to this overflow map.
    */
    private MapListenerSupport m_listenerSupport;

    /**
    * The CacheStatistics object maintained by this cache.
    */
    private SimpleCacheStatistics m_stats = new SimpleCacheStatistics();

    /**
    * Keeps track of whether we have previously logged a "Missing Event".
    */
    private boolean m_fLoggedMissingEvent;

    /**
    * Keeps track of whether we have previously logged an "Unfathomable
    * Event".
    */
    private boolean m_fLoggedUnfathomableEvent;

    /**
    * Keeps track of whether we have previously logged a "missing event
    * or events out of order".
    */
    private static boolean m_sWarnedEventSequence;
    }
