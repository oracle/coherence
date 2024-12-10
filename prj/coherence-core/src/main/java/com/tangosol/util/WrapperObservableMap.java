/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheStatistics;
import com.tangosol.net.cache.SimpleCacheStatistics;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


/**
* A simple implementation of ObservableMap interface built as a wrapper
* around any Map implementation. It also provides an implementation
* of CacheStatistics interface.
* <p>
* <b>Note:</b> as of Coherence 3.0 the CacheStatistics implementation has to be
*    turned on explicitly by calling the {@link #setCollectStats} method.
* <p>
* <b>Note:</b> if the underlying (wrapped) Map is an ObservableMap by itself,
*    as of Coherence 3.2 the WrapperObservableMap implementation does not
*    translate events generated the wrapped map by default. The translation can
*    be turned on explicitly by calling the {@link #setTranslateEvents} method.
*
* @author gg 2003.10.01
* @since Coherence 2.3
*/
public class WrapperObservableMap<K, V>
        extends AbstractKeySetBasedMap<K, V>
        implements ObservableMap<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an ObservableMap wrapper based on the specified map.
    * <p>
    * <b>Note:</b> it is assumed that while the WrapperObservableMap exists,
    * the contents of the underlying wrapped map are not directly manipulated.
    *
    * @param map  the Map that will be wrapped by this WrapperObservableMap
    */
    public WrapperObservableMap(Map<K, V> map)
        {
        this(map, false);
        }

    /**
    * Construct an ObservableMap wrapper based on the specified map.
    * <p>
    * <b>Note:</b> it is assumed that while the WrapperObservableMap exists,
    * the contents of the underlying wrapped map are not directly manipulated.
    *
    * @param map             the Map that will be wrapped by this WrapperObservableMap
    * @param fDeferredEvent  true iff if the value contained in the fabricated
    *                        cache events could be lazily populated. Deferred
    *                        events should only be raised to listeners that will
    *                        process events synchronously
    */
    public WrapperObservableMap(Map<K, V> map, boolean fDeferredEvent)
        {
        if (map == null)
            {
            throw new IllegalArgumentException("Map must be specified");
            }

        m_map            = map;
        m_fDeferredEvent = fDeferredEvent;
        }


    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void clear()
        {
        f_lockEvents.lock();
        try
            {
            if (isEventFabricator())
                {
                for (Iterator<K> iter = getInternalKeySet().iterator(); iter.hasNext(); )
                    {
                    // unlike some CacheEvent cases, this event gets fired BEFORE
                    // the entry is removed; moreover, deferring the event
                    // processing (e.g. processing it on a different thread)
                    // may yield the OldValue inaccessible or plain invalid
                    dispatchPendingEvent(iter.next(), MapEvent.ENTRY_DELETED, null, false);
                    }
                }

            getMap().clear();
            }
        finally
            {
            f_lockEvents.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsValue(Object oValue)
        {
        return getMap().containsValue(oValue);
        }

    /**
    * {@inheritDoc}
    */
    public V get(Object oKey)
        {
        Map<K, V> mapInner = getMap();
        if (isCollectStats())
            {
            long    ldtStart  = getSafeTimeMillis();
            V       oValue    = mapInner.get(oKey);
            boolean fContains = oValue != null || mapInner.containsKey(oKey);

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
        else
            {
            return mapInner.get(oKey);
            }
        }

    /**
    * {@inheritDoc}
    */
    public V put(K oKey, V oValue)
        {
        V oOrig;
        Map<K, V> mapInner = getMap();
        boolean fStats   = isCollectStats();
        long    ldtStart = fStats ? getSafeTimeMillis() : 0L;

        if (isEventFabricator())
            {
            int nEvent = mapInner.containsKey(oKey) ? MapEvent.ENTRY_UPDATED
                                                    : MapEvent.ENTRY_INSERTED;
            oOrig = mapInner.put(oKey, oValue);
            dispatchEvent(new CacheEvent(this, nEvent, oKey, oOrig, oValue, false));
            }
        else
            {
            oOrig = mapInner.put(oKey, oValue);
            }

        if (fStats)
            {
            // update statistics
            m_stats.registerPut(ldtStart);
            }

        return oOrig;
        }

    /**
    * {@inheritDoc}
    */
    public void putAll(Map<? extends K, ? extends V> map)
        {
        Map<K, V> mapInner = getMap();
        boolean   fStats   = isCollectStats();
        long      ldtStart = fStats ? getSafeTimeMillis() : 0L;

        // issue events
        if (isEventFabricator())
            {
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
                {
                K   oKey   = entry.getKey();
                int nEvent = mapInner.containsKey(oKey)
                             ? MapEvent.ENTRY_UPDATED
                             : MapEvent.ENTRY_INSERTED;
                dispatchPendingEvent(oKey, nEvent, entry.getValue(), false);
                }
            }

        mapInner.putAll(map);

        if (fStats)
            {
            // update statistics
            m_stats.registerPuts(map.size(), ldtStart);
            }
        }

    /**
    * {@inheritDoc}
    */
    public V remove(Object oKey)
        {
        Map<K, V> mapInner = getMap();
        if (isEventFabricator())
            {
            boolean fContained = mapInner.containsKey(oKey);
            V       oOrig      = fContained ? mapInner.remove(oKey) : null;
            if (fContained)
                {
                dispatchEvent(new CacheEvent(this, MapEvent.ENTRY_DELETED,
                                             oKey, oOrig, null, false));
                }
            return oOrig;
            }
        else
            {
            return mapInner.remove(oKey);
            }
        }


    // ----- AbstractKeySetBasedMap methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    protected Set<K> getInternalKeySet()
        {
        return getMap().keySet();
        }

    /**
    * {@inheritDoc}
    */
    protected boolean isInternalKeySetIteratorMutable()
        {
        // this wrapper only needs to do the mutations itself if the inner
        // map doesn't raise the necessary events
        return !isEventFabricator();
        }

    /**
    * {@inheritDoc}
    */
    protected boolean removeBlind(Object oKey)
        {
        if (isEventFabricator())
            {
            boolean fRemoved = false;
            if (getMap().containsKey(oKey))
                {
                dispatchPendingEvent((K) oKey, MapEvent.ENTRY_DELETED, null, false);
                getMap().keySet().remove(oKey);
                fRemoved = true;
                }
            return fRemoved;
            }
        else
            {
            return getMap().keySet().remove(oKey);
            }
        }


    // ----- ObservableMap methods ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void addMapListener(MapListener<? super K, ? super V> listener)
        {
        f_lockEvents.lock();
        try
            {
            addMapListener(listener, (Filter) null, false);
            }
        finally
            {
            f_lockEvents.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener)
        {
        f_lockEvents.lock();
        try
            {
            removeMapListener(listener, (Filter) null);
            }
        finally
            {
            f_lockEvents.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void addMapListener(MapListener<? super K, ? super V> listener, K oKey, boolean fLite)
        {
        azzert(listener != null);

        f_lockEvents.lock();
        try
            {
            Map<K, V> map = getMap();
            if (m_fEventBypass || (!isTranslateEvents() && map instanceof ObservableMap))
                {
                // it's possible to completely by-pass the WrapperObservableMap
                // for event generation by allowing the underlying map to send
                // the events directly to the listeners
                ((ObservableMap<K, V>) map).addMapListener(listener, oKey, fLite);
                m_fEventBypass = true;
                }
            else
                {
                MapListenerSupport support = ensureMapListenerSupport();

                boolean fWasEmpty = support.isEmpty(oKey);
                boolean fWasLite  = !fWasEmpty && !support.containsStandardListeners(oKey);

                support.addListener(listener, oKey, fLite);

                if ((fWasEmpty || (fWasLite && !fLite))
                        && map instanceof ObservableMap)
                    {
                    ObservableMap<K, V> mapSource        = (ObservableMap) map;
                    MapListener<K, V>   listenerInternal = ensureInternalListener();
                    if (fWasLite && !fLite)
                        {
                        // previously registered a lite listener
                        mapSource.removeMapListener(listenerInternal, oKey);
                        }
                    mapSource.addMapListener(listenerInternal, oKey, fLite);
                    }
                }
            }
        finally
            {
            f_lockEvents.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener, K oKey)
        {
        azzert(listener != null);
        f_lockEvents.lock();
        try
            {
            Map<K, V> map = getMap();
            if (m_fEventBypass)
                {
                // the event delivery was set up to by-pass this map, so
                // unregister accordingly
                ((ObservableMap<K, V>) map).removeMapListener(listener, oKey);
                }
            else
                {
                MapListenerSupport support = m_listenerSupport;
                if (support != null)
                    {
                    boolean fWasStandard = support.containsStandardListeners(oKey);

                    support.removeListener(listener, oKey);

                    MapListener<K, V> listenerInternal = m_listenerInternal;
                    if (listenerInternal != null)
                        {
                        ObservableMap<K, V> mapSource = (ObservableMap) getMap();
                        if (support.isEmpty(oKey))
                            {
                            mapSource.removeMapListener(listenerInternal, oKey);
                            if (support.isEmpty())
                                {
                                m_listenerSupport  = null;
                                m_listenerInternal = null;
                                }
                            }
                        else if (fWasStandard && !support.containsStandardListeners(oKey))
                            {
                            // replace standard with lite
                            mapSource.removeMapListener(listenerInternal, oKey);
                            mapSource.addMapListener(listenerInternal, oKey, true);
                            }
                        }
                    }
                }
            }
        finally
            {
            f_lockEvents.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void addMapListener(MapListener<? super K, ? super V> listener, Filter filter, boolean fLite)
        {
        azzert(listener != null);

        f_lockEvents.lock();
        try
            {
            Map<K, V> map = getMap();
            if (m_fEventBypass || (!isTranslateEvents() && map instanceof ObservableMap))
                {
                // it's possible to completely by-pass the WrapperObservableMap
                // for event generation by allowing the underlying map to send
                // the events directly to the listeners
                ((ObservableMap<K, V>) map).addMapListener(listener, filter, fLite);
                m_fEventBypass = true;
                }
            else
                {
                MapListenerSupport support = ensureMapListenerSupport();

                boolean fWasEmpty = support.isEmpty(filter);
                boolean fWasLite  = !fWasEmpty && !support.containsStandardListeners(filter);

                support.addListener(listener, filter, fLite);

                if ((fWasEmpty || (fWasLite && !fLite))
                        && map instanceof ObservableMap)
                    {
                    ObservableMap<K, V> mapSource        = (ObservableMap) map;
                    MapListener<K, V>   listenerInternal = ensureInternalListener();
                    if (fWasLite && !fLite)
                        {
                        // previously registered a lite listener
                        mapSource.removeMapListener(listenerInternal, filter);
                        }
                    mapSource.addMapListener(listenerInternal, filter, fLite);
                    }
                }
            }
        finally
            {
            f_lockEvents.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener, Filter filter)
        {
        azzert(listener != null);

        f_lockEvents.lock();
        try
            {
            Map<K, V> map = getMap();
            if (m_fEventBypass)
                {
                // the event delivery was set up to by-pass this map, so
                // unregister accordingly
                ((ObservableMap<K, V>) map).removeMapListener(listener, filter);
                }
            else
                {
                MapListenerSupport support = m_listenerSupport;
                if (support != null)
                    {
                    boolean fWasStandard = support.containsStandardListeners(filter);

                    support.removeListener(listener, filter);

                    MapListener<K, V> listenerInternal = m_listenerInternal;
                    if (listenerInternal != null)
                        {
                        ObservableMap<K, V> mapSource = (ObservableMap) getMap();
                        if (support.isEmpty(filter))
                            {
                            mapSource.removeMapListener(listenerInternal, filter);
                            if (support.isEmpty())
                                {
                                m_listenerSupport  = null;
                                m_listenerInternal = null;
                                }
                            }
                        else if (fWasStandard && !support.containsStandardListeners(filter))
                            {
                            // replace standard with lite
                            mapSource.removeMapListener(listenerInternal, filter);
                            mapSource.addMapListener(listenerInternal, filter, true);
                            }
                        }
                    }
                }
            }
        finally
            {
            f_lockEvents.unlock();
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the Map that is wrapped by this wrapper.
    * <p>
    * <b>Note: direct modifications of the returned map may cause
    *  an unpredictable behavior of the wrapper map.</b>
    *
    * @return the wrapped Map
    */
    public Map<K, V> getMap()
        {
        return m_map;
        }

    /**
    * Return the CacheStatistics for this cache.
    *
    * @return a CacheStatistics object
    */
    public CacheStatistics getCacheStatistics()
        {
        return m_stats;
        }

    /**
    * Check whether or not statistics are collected by the wrapper.
    *
    * @return true if this wrapper collects cache statistics; false otherwise
    *
    * @since Coherence 3.0
    */
    public boolean isCollectStats()
        {
        return m_fCollectStats;
        }

    /**
    * Specify whether or not statistics are to be collected by the wrapper.
    *
    * @param fCollectStats  true if this wrapper should collect cache
    *                       statistics; false otherwise
    *
    * @since Coherence 3.0
    */
    public void setCollectStats(boolean fCollectStats)
        {
        if (fCollectStats != m_fCollectStats)
            {
            if (fCollectStats && m_stats == null)
                {
                m_stats = new SimpleCacheStatistics();
                }
            m_fCollectStats = fCollectStats;
            }
        }

    /**
    * Check whether or not an event source has to be translated by the wrapper.
    * <p>
    * Note: this setting is only meaningful if the underlying map is an
    * ObservableMap itself.
    *
    * @return true if this wrapper translates an event source; false otherwise
    *
    * @since Coherence 3.3
    */
    public boolean isTranslateEvents()
        {
        return m_fTranslateEvents;
        }

    /**
    * Specify whether or not an event source has to be translated by the
    * wrapper.
    * <p>
    * Note: this setting is only meaningful if the underlying map is an
    * ObservableMap itself.
    *
    * @param fTranslate  true if this wrapper should translate an event
    *                    source; false otherwise
    *
    * @since Coherence 3.3
    */
    public void setTranslateEvents(boolean fTranslate)
        {
        if (fTranslate != m_fTranslateEvents)
            {
            // once listeners have been added, this setting must not be
            // changed
            azzert(!hasListeners() && !m_fEventBypass);

            m_fTranslateEvents = fTranslate;
            }
        }

    /**
    * Assemble a human-readable description.
    *
    * @return a description of this Map
    */
    protected String getDescription()
        {
        Map map = getMap();
        return "Map {class=" + map.getClass().getName()
                + ", size=" + map.size()
                + ", observable=" + (map instanceof ObservableMap)
                + "}, CollectStats=" + isCollectStats()
                + ", CacheStatistics=" + getCacheStatistics()
                + ", hasListeners=" + hasListeners()
                + ", EventFabricator=" + isEventFabricator();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "WrapperObservableMap {" + getDescription() + "}";
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
    * Obtain the MapListenerSupport, creating it if necessary.
    *
    * @return the MapListenerSupport; never null
    */
    protected MapListenerSupport ensureMapListenerSupport()
        {
        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            m_listenerSupport = support = new MapListenerSupport();
            }
        return support;
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
    * Determine if this ObservableMap has to fabricate events itself.
    *
    * @return true if events are expected, but the wrapped Map does not
    *         generate any events itself
    */
    protected boolean isEventFabricator()
        {
        // if the wrapped Map doesn't issue its own events, then this Map
        // has to generate them itself
        return hasListeners() && m_listenerInternal == null;
        }

    /**
    * Helper method to determine if an event is synthetic.
    *
    * @param <K>  the key type
    * @param <V>  the value type
    * @param evt  a Map Event
    *
    * @return true if the event is a synthetic cache event
    */
    protected static <K, V> boolean isSynthetic(MapEvent<K, V> evt)
        {
        return evt instanceof CacheEvent && ((CacheEvent) evt).isSynthetic();
        }

    /**
    * Dispatch an event that has not yet occurred, allowing the cache to
    * potentially avoid reading of the "original value" information.
    *
    * @param oKey        the key which the event is related to
    * @param nId         the event ID
    * @param oNewValue   the new value
    * @param fSynthetic  true if the event is synthetic
    */
    protected void dispatchPendingEvent(K oKey, int nId, V oNewValue, boolean fSynthetic)
        {
        CacheEvent<K, V> event = m_fDeferredEvent
            ? new CacheEvent<K, V>(this, nId, oKey, null, oNewValue, fSynthetic)
                {
                public V getOldValue()
                    {
                    if (isInsert())
                        {
                        return null;
                        }

                    V oOldValue = m_oOldValue;
                    if (oOldValue == null)
                        {
                        m_oOldValue = oOldValue = WrapperObservableMap.this.get(getKey());
                        }
                    return oOldValue;
                    }

                private V m_oOldValue;
                }
            : new CacheEvent<>(this, nId, oKey, get(oKey), oNewValue, fSynthetic);

        dispatchEvent(event);
        }

    /**
    * Dispatch the passed event.
    *
    * @param evt   a CacheEvent object
    */
    protected void dispatchEvent(MapEvent<? extends K, ? extends V> evt)
        {
        MapListenerSupport listenerSupport = getMapListenerSupport();
        if (listenerSupport != null)
            {
            if (isTranslateEvents() && evt.getMap() != this)
                {
                final MapEvent<? extends K, ? extends V> evtOrig = evt;
                evt = new CacheEvent<K, V>(this, evt.getId(), evt.getKey(),
                                     null, null, false)
                    {
                    public V getOldValue()
                        {
                        return evtOrig.getOldValue();
                        }

                    public V getNewValue()
                        {
                        return evtOrig.getNewValue();
                        }

                    public boolean isSynthetic()
                        {
                        return WrapperObservableMap.isSynthetic(evtOrig);
                        }
                    };
                }

            // the events can only be generated while the current thread
            // holds the monitor on this map
            f_lockEvents.lock();
            try
                {
                listenerSupport.fireEvent(evt, false);
                }
            finally
                {
                f_lockEvents.unlock();
                }
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public int hashCode()
        {
        return getMap().hashCode();
        }

    @Override
    public boolean equals(Object o)
        {
        return getMap().equals(o);
        }

    // ----- inner class: InternalListener ----------------------------------

    /**
    * Obtain the internal MapListener, creating one if necessary.
    *
    * @return  an instance of MapListener; never null
    */
    protected MapListener<K, V> ensureInternalListener()
        {
        MapListener<K, V> listenerInternal = m_listenerInternal;
        if (listenerInternal == null)
            {
            m_listenerInternal = listenerInternal = instantiateInternalListener();
            }
        return listenerInternal;
        }

    /**
    * Instantiate a MapListener to listen to the wrapped map.
    *
    * @return  an instance of MapListener
    */
    protected MapListener<K, V> instantiateInternalListener()
        {
        return new InternalListener();
        }

    /**
    * An internal MapListener that listens to the wrapped map.
    */
    protected class InternalListener
            extends MultiplexingMapListener<K, V>
        {
        /**
        * {@inheritDoc}
        */
        protected void onMapEvent(MapEvent<K, V> evt)
            {
            WrapperObservableMap.this.dispatchEvent(evt);
            }
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The (wrapped) map containing all the resources.
    */
    protected Map<K, V> m_map;

    /**
    * The event listeners.
    */
    protected MapListenerSupport m_listenerSupport;

    /**
    * The MapListener used to listen to the wrapped ObservableMap.
    */
    protected MapListener<K, V> m_listenerInternal;

    /**
    * The CacheStatistics object maintained by this wrapper.
    */
    protected SimpleCacheStatistics m_stats;

    /**
    * Specifies whether statistics are to be collected by this wrapper.
    */
    private boolean m_fCollectStats;

    /**
    * Specifies whether or not events are translated by this wrapper.
    */
    protected boolean m_fTranslateEvents;

    /**
    * Specifies whether or not fabricated events could be deferred.
    */
    protected boolean m_fDeferredEvent;

    /**
    * Tracks whether the WrapperObservableMap has explicitly decided to count
    * on the underlying map to bypass this map when delivering events.
    */
    private boolean m_fEventBypass;

    /**
     * The lock to control event access.
     */
    private final ReentrantLock f_lockEvents = new ReentrantLock();
    }
