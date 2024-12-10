/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.oracle.coherence.common.base.Disposable;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.CacheStatistics;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.WrapperObservableMap;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* An observable, cache-aware PartitionAwareBackingMap implementation.
*
* @since Coherence 3.5
* @author cp  2009-01-09
*/
public class ObservableSplittingBackingMap
        extends WrapperObservableMap
        implements CacheMap, Disposable, PartitionAwareBackingMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a ObservableSplittingBackingMap that will delegate each
    * partition's data and operations to a separate backing map.
    *
    * @param bmm    a callback that knows how to create and release the
    *               backing maps that this PartitionSplittingBackingMap is
    *               responsible for
    * @param sName  the cache name for which this backing map exists
    */
    public ObservableSplittingBackingMap(BackingMapManager bmm, String sName)
        {
        this(new PartitionSplittingBackingMap(bmm, sName));
        }

    /**
    * Create a ObservableSplittingBackingMap that will delegate each
    * partition's data and operations to a separate backing map.
    *
    * @param map  the PartitionAwareBackingMap to delegate to
    */
    public ObservableSplittingBackingMap(PartitionAwareBackingMap map)
        {
        super(map);
        }


    // ----- CacheMap interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        Object  oOrig;
        Map     mapPart = getPartitionSplittingBackingMap().getBackingMap(oKey);
        if (mapPart instanceof CacheMap)
            {
            // for an understanding of this event-related code, see
            // WrapperObservableMap#put
            boolean fFabricate = isEventFabricator();
            int     nEvent     = fFabricate && mapPart.containsKey(oKey)
                               ? MapEvent.ENTRY_UPDATED
                               : MapEvent.ENTRY_INSERTED;

            prepareUpdate(mapPart, Collections.singletonMap(oKey, oValue));

            oOrig = ((CacheMap) mapPart).put(oKey, oValue, cMillis);

            if (fFabricate)
                {
                dispatchEvent(new CacheEvent(this, nEvent, oKey, oOrig, oValue, false));
                }
            }
        else if (cMillis <= 0)
            {
            oOrig = super.put(oKey, oValue);
            }
        else
            {
            throw new UnsupportedOperationException();
            }

        return oOrig;
        }


    // ----- ObservableMap interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener, Object oKey, boolean fLite)
        {
        azzert(listener != null);

        MapListenerSupport support = ensureMapListenerSupport();

        boolean fWasEmpty = support.isEmpty(oKey);
        boolean fWasLite  = !fWasEmpty && !support.containsStandardListeners(oKey);

        support.addListener(listener, oKey, fLite);

        if (m_nEventSource == EVT_SRC_BACKING && (fWasEmpty || (fWasLite && !fLite)))
            {
            PartitionSplittingBackingMap mapPSBM   = getPartitionSplittingBackingMap();
            BackingMapManagerContext     ctx       = mapPSBM.getContext();
            ObservableMap                mapSource = (ObservableMap)
                    getPartitionMap(ctx.getKeyPartition(oKey));
            if (mapSource != null)
                {
                if (fWasLite && !fLite)
                    {
                    mapSource.removeMapListener(ensureInternalListener(), oKey);
                    }
                mapSource.addMapListener(ensureInternalListener(), oKey, fLite);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener, Object oKey)
        {
        MapListenerSupport support = getMapListenerSupport();
        if (support != null)
            {
            boolean fWasStandard = support.containsStandardListeners(oKey);

            support.removeListener(listener, oKey);

            if (m_nEventSource == EVT_SRC_BACKING)
                {
                PartitionSplittingBackingMap mapPSBM   = getPartitionSplittingBackingMap();
                BackingMapManagerContext     ctx       = mapPSBM.getContext();
                ObservableMap                mapSource = (ObservableMap)
                        getPartitionMap(ctx.getKeyPartition(oKey));
                if (mapSource != null)
                    {
                    MapListener listenerInternal = ensureInternalListener();
                    if (support.isEmpty(oKey))
                        {
                        mapSource.removeMapListener(listenerInternal, oKey);
                        if (support.isEmpty())
                            {
                            m_listenerSupport = null;
                            }
                        }
                    else
                        {
                        if (fWasStandard && !support.containsStandardListeners(oKey))
                            {
                            // replace standard with lite
                            mapSource.removeMapListener(listenerInternal, oKey);
                            mapSource.addMapListener(listenerInternal, oKey, true);
                            }
                        }
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener, Filter filter, boolean fLite)
        {
        azzert(listener != null);

        MapListenerSupport support = ensureMapListenerSupport();

        boolean fWasEmpty = support.isEmpty(filter);
        boolean fWasLite  = !fWasEmpty && !support.containsStandardListeners(filter);

        support.addListener(listener, filter, fLite);

        if (m_nEventSource == EVT_SRC_BACKING && (fWasEmpty || (fWasLite && !fLite)))
            {
            MapListener listenerInternal = ensureInternalListener();
            Map[] amapSource = getPartitionSplittingBackingMap().getMapArray().getBackingMaps();
            for (int i = 0, c = amapSource.length; i < c; ++i)
                {
                ObservableMap mapSource = (ObservableMap) amapSource[i];
                if (fWasLite && !fLite)
                    {
                    mapSource.removeMapListener(listenerInternal, filter);
                    }
                mapSource.addMapListener(listenerInternal, filter, fLite);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener, Filter filter)
        {
        MapListenerSupport support = getMapListenerSupport();
        if (support != null)
            {
            boolean fWasStandard = support.containsStandardListeners(filter);

            support.removeListener(listener, filter);

            if (m_nEventSource == EVT_SRC_BACKING)
                {
                // determine what changes must occur to the event sources
                // (the actual backing maps)
                boolean fRemove  = false;
                boolean fAddLite = false;
                if (support.isEmpty(filter))
                    {
                    fRemove = true;
                    if (support.isEmpty())
                        {
                        m_listenerSupport = null;
                        }
                    }
                else
                    {
                    if (fWasStandard && !support.containsStandardListeners(filter))
                        {
                        // replace standard with lite
                        fRemove  = true;
                        fAddLite = true;
                        }
                    }

                if (fRemove || fAddLite)
                    {
                    MapListener listenerInternal = ensureInternalListener();
                    Map[] amapSource = getPartitionSplittingBackingMap().getMapArray().getBackingMaps();
                    for (int i = 0, c = amapSource.length; i < c; ++i)
                        {
                        ObservableMap mapSource = (ObservableMap) amapSource[i];
                        if (fRemove)
                            {
                            mapSource.removeMapListener(listenerInternal, filter);
                            }
                        if (fAddLite)
                            {
                            mapSource.addMapListener(listenerInternal, filter, true);
                            }
                        }
                    }
                }
            }
        }


    // ----- PartitionAwareBackingMap methods -------------------------------

    /**
    * {@inheritDoc}
    */
    public BackingMapManager getBackingMapManager()
        {
        return getPartitionSplittingBackingMap().getBackingMapManager();
        }

    /**
    * {@inheritDoc}
    */
    public String getName()
        {
        return getPartitionSplittingBackingMap().getName();
        }

    /**
    * {@inheritDoc}
    */
    public void createPartition(int nPid)
        {
        // get the partition-aware backing map
        PartitionSplittingBackingMap mapPSBM = getPartitionSplittingBackingMap();

        // tell it to create the partition (which in turn creates the backing
        // map)
        mapPSBM.createPartition(nPid);

        // get the new partition backing map
        Map     mapBacking   = mapPSBM.getPartitionMap(nPid);
        boolean fObservable  = mapBacking instanceof ObservableMap;
        int     nEventSource = m_nEventSource;
        if (nEventSource == EVT_SRC_UNKNOWN)
            {
            m_nEventSource = nEventSource = fObservable ? EVT_SRC_BACKING : EVT_SRC_THIS;
            }
        else
            {
            // all partition backing maps should be of the same type
            azzert(nEventSource == (fObservable ? EVT_SRC_BACKING : EVT_SRC_THIS));
            }

        // if the new underlying partition map is responsible for raising
        // events, then add any listeners that have already been registered
        // for those events
        synchronized (this)
            {
            if (nEventSource == EVT_SRC_BACKING && hasListeners())
                {
                ObservableMap      mapSource = (ObservableMap) mapBacking;
                MapListenerSupport support   = getMapListenerSupport();
                MapListener        listener  = ensureInternalListener();

                for (Iterator iter = support.getFilterSet().iterator(); iter.hasNext(); )
                    {
                    Filter filter = (Filter) iter.next();
                    mapSource.addMapListener(listener, filter,
                                             !support.containsStandardListeners(filter));
                    }

                BackingMapManagerContext ctx = mapPSBM.getContext();
                for (Iterator iter = support.getKeySet().iterator(); iter.hasNext(); )
                    {
                    Object oKey = iter.next();
                    if (ctx.getKeyPartition(oKey) == nPid)
                        {
                        mapSource.addMapListener(listener, oKey,
                                                 !support.containsStandardListeners(oKey));
                        }
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void destroyPartition(int nPid)
        {
        // get the partition-aware backing map
        PartitionSplittingBackingMap mapPSBM = getPartitionSplittingBackingMap();

        // if the new underlying partition map is responsible for raising
        // events, then unregister for those events
        synchronized (this)
            {
            if (m_nEventSource == EVT_SRC_BACKING && hasListeners())
                {
                ObservableMap      mapSource = (ObservableMap) mapPSBM.getPartitionMap(nPid);
                MapListenerSupport support   = getMapListenerSupport();
                MapListener        listener  = ensureInternalListener();

                for (Iterator iter = support.getFilterSet().iterator(); iter.hasNext(); )
                    {
                    Filter filter = (Filter) iter.next();
                    mapSource.removeMapListener(listener, filter);
                    }

                BackingMapManagerContext ctx = mapPSBM.getContext();
                for (Iterator iter = support.getKeySet().iterator(); iter.hasNext(); )
                    {
                    Object oKey = iter.next();
                    if (ctx.getKeyPartition(oKey) == nPid)
                        {
                        mapSource.removeMapListener(listener, oKey);
                        }
                    }
                }
            }

        mapPSBM.destroyPartition(nPid);
        }

    /**
    * {@inheritDoc}
    */
    protected Set instantiateKeySet()
        {
        return getPartitionSplittingBackingMap().instantiateKeySet();
        }

    /**
    * {@inheritDoc}
    */
    public Map getPartitionMap(int nPid)
        {
        return ((PartitionAwareBackingMap) m_map).getPartitionMap(nPid);
        }

    /**
    * {@inheritDoc}
    */
    public Map getPartitionMap(PartitionSet partitions)
        {
        return ((PartitionAwareBackingMap) m_map).getPartitionMap(partitions);
        }


    // ----- Disposable interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public void dispose()
        {
        getPartitionSplittingBackingMap().dispose();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying PartitionSplittingBackingMap.
    *
    * @return the underlying PartitionSplittingBackingMap
    */
    public PartitionSplittingBackingMap getPartitionSplittingBackingMap()
        {
        return (PartitionSplittingBackingMap) getMap();
        }

    /**
    * {@inheritDoc}
    */
    protected boolean isEventFabricator()
        {
        return hasListeners() && m_nEventSource == EVT_SRC_THIS;
        }

    /**
    * {@inheritDoc}
    */
    public void setTranslateEvents(boolean fTranslate)
        {
        // this sub-class can deal with the event translation setting being
        // modified on the fly because all events are routed through the
        // internal listener (there is no event by-pass option)
        m_fTranslateEvents = fTranslate;
        }

    /**
    * {@inheritDoc}
    */
    public CacheStatistics getCacheStatistics()
        {
        return getPartitionSplittingBackingMap().getCacheStatistics();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isCollectStats()
        {
        // stats are collected by the PartitionSplittingBackingMap
        return false;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this ObservableSplittingBackingMap.
    *
    * @return a String description of the ObservableSplittingBackingMap
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            '{' + getPartitionSplittingBackingMap() + '}';
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Prepare mapPart, a map representing a partition, for the impending changes
    * in which all of the mappings from mapUpdate will be copied to mapPart.
    *
    * @param mapPart    the map to be mutated with the contents of mapUpdate
    * @param mapUpdate  the map of changes to be applied
    */
    protected void prepareUpdate(Map mapPart, Map mapUpdate)
        {
        // this method should be overridden by any sub class that needs to
        // interrogate or manipulate the map prior to an impending mutation
        // to the map
        }


    // ----- constants ------------------------------------------------------

    /**
    * Event source: Not yet determined (no partition maps yet).
    */
    private static final int EVT_SRC_UNKNOWN = 0;

    /**
    * Event source: Events are raised by the underlying partition backing
    * maps.
    */
    private static final int EVT_SRC_BACKING = 1;

    /**
    * Event source: Events are fabricated by this map.
    */
    private static final int EVT_SRC_THIS    = 2;


    // ----- data members ---------------------------------------------------

    /**
    * The source of events; one of the EVT_SRC_* constants. This tracks
    * whether events are created (fabricated) by this map or whether the
    * underlying maps are observable.
    */
    private int m_nEventSource = EVT_SRC_UNKNOWN;
    }
