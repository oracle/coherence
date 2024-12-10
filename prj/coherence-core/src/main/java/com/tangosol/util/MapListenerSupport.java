/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.cache.CacheEvent;

import com.tangosol.net.partition.DefaultVersionedPartitions;
import com.tangosol.net.partition.VersionAwareMapListener;
import com.tangosol.net.partition.VersionedPartitions;
import com.tangosol.net.partition.VersionedPartitions.VersionedIterator;

import com.tangosol.util.filter.InKeySetFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
* This class provides support for advanced MapListener functionality.
*
* @author gg  2003.09.16
* @since Coherence 2.3
*/
public class MapListenerSupport
        extends Base
    {
    /**
    * Constructs a new MapListenerSupport object.
    */
    public MapListenerSupport()
        {
        }

    /**
    * Add a map listener that receives events based on a filter evaluation.
    *
    * @param listener  the listener to add
    * @param filter    a filter that will be passed MapEvent objects to
    *                  select from; a MapEvent will be delivered to the
    *                  listener only if the filter evaluates to true for
    *                  that MapEvent; null is equivalent to a filter
    *                  that always returns true
    * @param fLite     true to indicate that the MapEvent objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    */
    public synchronized void addListener(MapListener listener, Filter filter, boolean fLite)
        {
        if (listener != null)
            {
            Map mapListeners = m_mapListeners;
            if (mapListeners == null)
                {
                mapListeners = m_mapListeners = new LiteMap();
                }
            addSafeListener(mapListeners, filter, listener);

            Map mapStandard = m_mapStandardListeners;
            if (mapStandard == null)
                {
                mapStandard = m_mapStandardListeners = new LiteMap();
                }
            addListenerState(mapStandard, filter, listener, fLite);

            m_nOptimizationPlan = PLAN_NONE;
            m_listenersCached   = null;
            }
        }

    /**
    * Add a map listener that receives events based on a filter evaluation.
    *
    * @param listener  the listener to add
    * @param filter    a filter that will be passed MapEvent objects to
    *                  select from; a MapEvent will be delivered to the
    *                  listener only if the filter evaluates to true for
    *                  that MapEvent; null is equivalent to a filter
    *                  that always returns true
    * @param fLite     true to indicate that the MapEvent objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    *
    * @return false iff there already existed a "covering" listener for that filter
    *              (either standard or lite for a lite call and standard otherwise)
    */
    public synchronized boolean addListenerWithCheck(MapListener listener, Filter filter, boolean fLite)
        {
        boolean fCovered = !isEmpty(filter) && !isVersionAware(listener) &&
                (fLite || containsStandardListeners(filter));

        addListener(listener, filter, fLite);

        return !fCovered;
        }

    /**
    * Add a map listener for a specific key.
    *
    * @param listener  the listener to add
    * @param oKey      the key that identifies the entry for which to register
    *                  the event listener
    * @param fLite     true to indicate that the MapEvent objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    */
    public synchronized void addListener(MapListener listener, Object oKey, boolean fLite)
        {
        if (listener != null)
            {
            Map mapListeners = m_mapKeyListeners;
            if (mapListeners == null)
                {
                mapListeners = m_mapKeyListeners = new HashMap();
                }
            addSafeListener(mapListeners, oKey, listener);

            Map mapStandard = m_mapStandardKeyListeners;
            if (mapStandard == null)
                {
                mapStandard = m_mapStandardKeyListeners = new LiteMap();
                }
            addListenerState(mapStandard, oKey, listener, fLite);

            // if the optimization plan was already to optimize for key
            // listeners, and the key listener that we just added is the
            // same as was already present, then keep the current plan,
            // otherwise reset it
            boolean fKeepPlan = false;
            if (m_nOptimizationPlan == PLAN_KEY_LISTENER)
                {
                EventListener[] alistener = m_listenersCached.listeners();
                if (alistener != null && alistener.length == 1 && alistener[0] == listener)
                    {
                    fKeepPlan = true;
                    }
                }

            if (!fKeepPlan)
                {
                m_nOptimizationPlan = PLAN_NONE;
                m_listenersCached   = null;
                }
            }
        }

    /**
    * Add a map listener for a specific key.
    *
    * @param listener  the listener to add
    * @param oKey      the key that identifies the entry for which to register
    *                  the event listener
    * @param fLite     true to indicate that the MapEvent objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    *
    * @return false iff there already existed a "covering" listener for that key
    *              (either standard or lite for a lite call and standard otherwise)
    */
    public synchronized boolean addListenerWithCheck(MapListener listener, Object oKey, boolean fLite)
        {
        boolean fCovered = !isEmpty(oKey) && !isVersionAware(listener) &&
                (fLite || containsStandardListeners(oKey));

        addListener(listener, oKey, fLite);

        return !fCovered;
        }

    /**
    * Add a map listener for a set of keys.
    *
    * @param listener  the listener to add
    * @param setKey    the key set for which to register the event listener
    * @param fLite     true to indicate that the MapEvent objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    */
    public synchronized void addListener(MapListener listener, Set setKey, boolean fLite)
        {
        for (Object oKey : setKey)
            {
            addListener(listener, oKey, fLite);
            }
        }

    /**
    * Add a map listener for a set of keys. This method will modify the passed
    * set by removing the keys that already had existing "covering" listeners
    * for them.
    *
    * @param listener  the listener to add
    * @param setKey    the key set for which to register the event listener
    * @param fLite     true to indicate that the MapEvent objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    */
    public synchronized void addListenerWithCheck(MapListener listener, Set setKey, boolean fLite)
        {
        for (Iterator iter = setKey.iterator(); iter.hasNext(); )
            {
            Object oKey = iter.next();

            if (!addListenerWithCheck(listener, oKey, fLite))
                {
                iter.remove();
                }
            }
        }

    /**
    * Remove a map listener that previously signed up for events based on a
    * filter evaluation.
    *
    * @param listener  the listener to remove
    * @param filter    a filter used to evaluate events
    */
    public synchronized void removeListener(MapListener listener, Filter filter)
        {
        if (listener != null)
            {
            Map mapListeners = m_mapListeners;
            if (mapListeners != null)
                {
                removeSafeListener(mapListeners, filter, listener);
                if (mapListeners.isEmpty())
                    {
                    m_mapListeners = null;
                    }

                Map mapStandard = m_mapStandardListeners;
                if (mapStandard != null)
                    {
                    removeListenerState(mapStandard, filter, listener);
                    if (mapStandard.isEmpty())
                        {
                        m_mapStandardListeners = null;
                        }
                    }
                }

            m_nOptimizationPlan = PLAN_NONE;
            m_listenersCached   = null;
            }
        }

    /**
    * Remove a map listener that previously signed up for events based on a
    * filter evaluation.
    *
    * @param listener  the listener to remove
    * @param filter    a filter used to evaluate events
    *
    * @return true iff there are no longer any listeners for that filter
    */
    public synchronized boolean removeListenerWithCheck(MapListener listener, Filter filter)
        {
        removeListener(listener, filter);

        return isEmpty(filter);
        }

    /**
    * Remove a map listener that previously signed up for events about a
    * specific key.
    *
    * @param listener  the listener to remove
    * @param oKey      the key that identifies the entry for which to unregister
    *                  the event listener
    */
    public synchronized void removeListener(MapListener listener, Object oKey)
        {
        if (listener != null)
            {
            Map mapListeners = m_mapKeyListeners;
            if (mapListeners != null)
                {
                removeSafeListener(mapListeners, oKey, listener);
                if (mapListeners.isEmpty())
                    {
                    m_mapKeyListeners = null;
                    }

                Map mapStandard = m_mapStandardKeyListeners;
                if (mapStandard != null)
                    {
                    removeListenerState(mapStandard, oKey, listener);
                    if (mapStandard.isEmpty())
                        {
                        m_mapStandardKeyListeners = null;
                        }
                    }
                }

            // if the optimization plan was already to optimize for key
            // listeners, and the cached set of key listeners is a set of
            // exactly one listener, and there are still other keys with
            // that same listener registered, then keep the current plan,
            // otherwise reset it
            boolean fKeepPlan = false;
            if (m_nOptimizationPlan == PLAN_KEY_LISTENER)
                {
                EventListener[] alistener = m_listenersCached.listeners();
                if (alistener != null && alistener.length == 1 && alistener[0] == listener)
                    {
                    // keep the plan if there are any keys still being
                    // listened to
                    fKeepPlan = (m_mapKeyListeners != null);
                    }
                }

            if (!fKeepPlan)
                {
                m_nOptimizationPlan = PLAN_NONE;
                m_listenersCached   = null;
                }
            }
        }

    /**
    * Remove a map listener that previously signed up for events about a
    * specific key.
    *
    * @param listener  the listener to remove
    * @param oKey      the key that identifies the entry for which to unregister
    *                  the event listener
    *
    * @return true iff there are no longer any listeners for that key
    */
    public synchronized boolean removeListenerWithCheck(MapListener listener, Object oKey)
        {
        removeListener(listener, oKey);

        return isEmpty(oKey);
        }

    /**
    * Remove a map listener that previously signed up for events about
    * specific keys.
    *
    * @param listener  the listener to remove
    * @param setKey    the set of keys for which to unregister the event listener
    */
    public synchronized void removeListener(MapListener listener, Set setKey)
        {
        for (Object oKey : setKey)
            {
            removeListener(listener, oKey);
            }
        }

    /**
    * Remove a map listener that previously signed up for events about specific
    * keys. This method will modify the passed set by removing the keys that
    * still have existing "covering" listeners for them, so the keys that are
    * retained in the set no longer have any listeners for them.
    *
    * @param listener  the listener to remove
    * @param setKey    the set of keys for which to unregister the event listener
    */
    public synchronized void removeListenerWithCheck(MapListener listener, Set setKey)
        {
        for (Iterator iter = setKey.iterator(); iter.hasNext(); )
            {
            if (!removeListenerWithCheck(listener, iter.next()))
                {
                iter.remove();
                }
            }
        }

    /**
    * Remove all signed up listeners.
    */
    public synchronized void clear()
        {
        m_mapListeners = null;
        m_mapKeyListeners = null;
        m_mapStandardListeners = null;
        m_mapStandardKeyListeners = null;

        m_nOptimizationPlan = PLAN_NO_LISTENERS;
        m_listenersCached   = null;
        }

    /**
    * Checks whether or not this MapListenerSupport object contains
    * any listeners.
    *
    * @return true iff there are no listeners encapsulated by this
    *         MapListenerSupport object
    */
    public boolean isEmpty()
        {
        return m_mapListeners == null && m_mapKeyListeners == null;
        }

    /**
    * Return the number of listeners registered.
    *
    * @return the number of listeners registered
    */
    @SuppressWarnings("unchecked")
    public int getListenerCount()
        {
        int cListener = 0;
        if (m_mapListeners != null)
            {
            Map<?, Listeners> map = m_mapListeners;
            cListener += map.values().stream().mapToInt(Listeners::getListenerCount).sum();
            }
        if (m_mapKeyListeners != null)
            {
            Map<?, Listeners> map = m_mapKeyListeners;
            cListener += map.values().stream().mapToInt(Listeners::getListenerCount).sum();
            }
        return cListener;
        }

    /**
    * Checks whether or not this MapListenerSupport object contains
    * any listeners for a given filter.
    *
    * @param filter  the filter
    *
    * @return true iff there are no listeners for the specified filter
    *              encapsulated by this MapListenerSupport object
    */
    public boolean isEmpty(Filter filter)
        {
        Map mapListeners = m_mapListeners;
        return mapListeners == null || !mapListeners.containsKey(filter);
        }

    /**
    * Checks whether or not this MapListenerSupport object contains
    * any listeners for a given key.
    *
    * @param oKey  the key
    *
    * @return true iff there are no listeners for the specified filter
    *              encapsulated by this MapListenerSupport object
    */
    public boolean isEmpty(Object oKey)
        {
        Map mapListeners = m_mapKeyListeners;
        return mapListeners == null || !mapListeners.containsKey(oKey);
        }

    /**
    * Checks whether or not this MapListenerSupport object contains
    * any standard (not lite) listeners for a given filter.
    *
    * @param filter  the filter
    *
    * @return true iff there are no standard listeners for the specified filter
    *              encapsulated by this MapListenerSupport object
    */
    public boolean containsStandardListeners(Filter filter)
        {
        Map mapStandard = m_mapStandardListeners;
        if (mapStandard == null)
            {
            return false;
            }
        Set setStandard = (Set) mapStandard.get(filter);
        return setStandard != null && !setStandard.isEmpty();
        }

    /**
    * Checks whether or not this MapListenerSupport object contains
    * any standard (not lite) listeners for a given key.
    *
    * @param oKey  the key
    *
    * @return true iff there are no standard listeners for the specified filter
    *              encapsulated by this MapListenerSupport object
    */
    public boolean containsStandardListeners(Object oKey)
        {
        Map mapStandard = m_mapStandardKeyListeners;
        if (mapStandard == null)
            {
            return false;
            }
        Set setStandard = (Set) mapStandard.get(oKey);
        return setStandard != null && !setStandard.isEmpty();
        }

    /**
    * Obtain a set of all filters that have associated global listeners.
    * <p>
    * <b>Note</b>: The returned value must be treated as an immutable.
    *
    * @return a set of all filters that have associated global listeners
    */
    public Set getFilterSet()
        {
        Map mapListeners = m_mapListeners;
        return mapListeners == null ? NullImplementation.getSet() : mapListeners.keySet();
        }

    /**
    * Obtain a set of all keys that have associated key listeners.
    * <p>
    * <b>Note</b>: The returned value must be treated as an immutable.
    *
    * @return a set of all keys that have associated key listeners
    */
    public Set getKeySet()
        {
        Map mapListeners = m_mapKeyListeners;
        return mapListeners == null ? NullImplementation.getSet() : mapListeners.keySet();
        }

    /**
    * Obtain the Listeners object for a given filter.
    * <p>
    * <b>Note</b>: The returned value must be treated as an immutable.
    *
    * @param filter  the filter
    *
    * @return the Listeners object for the filter; null if none exists
    */
    public synchronized Listeners getListeners(Filter filter)
        {
        // this method is synchronized because the underlying map implementation
        // is not thread safe for "get" operations: it could blow up (LiteMap)
        // or return null (HashMap) while there is a valid entry
        Map mapListeners = m_mapListeners;
        return mapListeners == null ? null : (Listeners) mapListeners.get(filter);
        }

    /**
    * Obtain the Listeners object for a given key.
    * <p>
    * <b>Note</b>: The returned value must be treated as an immutable.
    *
    * @param oKey  the key
    *
    * @return the Listeners object for the key; null if none exists
    */
    public synchronized Listeners getListeners(Object oKey)
        {
        // this method is synchronized because the underlying map implementation
        // is not thread safe for "get" operations: it could blow up (LiteMap)
        // or return null (HashMap) while there is a valid entry
        Map mapListeners = m_mapKeyListeners;
        return mapListeners == null ? null : (Listeners) mapListeners.get(oKey);
        }

    /**
    * Collect all Listeners that should be notified for a given event.
    * <p>
    * <b>Note</b>: The returned value must be treated as an immutable.
    *
    * @param event the MapEvent object
    *
    * @return the Listeners object containing the relevant listeners
    */
    public Listeners collectListeners(MapEvent event)
        {
        synchronized (this)
            {
            switch (m_nOptimizationPlan)
                {
                case PLAN_NONE:
                default:
                    // put a plan together
                    Map mapAllListeners = m_mapListeners;
                    Map mapKeyListeners = m_mapKeyListeners;
                    if (mapAllListeners == null || mapAllListeners.isEmpty())
                        {
                        // no standard listeners; check for key listeners
                        if (mapKeyListeners == null || mapKeyListeners.isEmpty())
                            {
                            m_nOptimizationPlan = PLAN_NO_LISTENERS;
                            m_listenersCached   = null;
                            }
                        else
                            {
                            // can only do key optimization if all keys have
                            // the same set of listeners registered
                            EventListener[] alistenerPrev = null;
                            for (Iterator iter = mapKeyListeners.values().iterator(); iter.hasNext(); )
                                {
                                Listeners listeners = (Listeners) iter.next();
                                if (alistenerPrev == null)
                                    {
                                    // assume that they are all the same
                                    m_nOptimizationPlan = PLAN_KEY_LISTENER;
                                    m_listenersCached   = listeners;

                                    alistenerPrev = listeners.listeners();
                                    }
                                else
                                    {
                                    EventListener[] alistenerCur   = listeners.listeners();
                                    int             cListenersCur  = alistenerCur.length;
                                    int             cListenersPrev = alistenerPrev.length;
                                    boolean         fOptimize      = cListenersCur == cListenersPrev;
                                    if (fOptimize)
                                        {
                                        for (int i = 0; i < cListenersCur; ++i)
                                            {
                                            if (alistenerCur[i] != alistenerPrev[i])
                                                {
                                                // assumption was incorrect -- some
                                                // keys have different listeners
                                                fOptimize = false;
                                                break;
                                                }
                                            }
                                        }
                                    if (!fOptimize)
                                        {
                                        m_nOptimizationPlan = PLAN_NO_OPTIMIZE;
                                        m_listenersCached   = null;
                                        break;
                                        }
                                    }
                                }
                            }
                        }
                    else // there are "all" listeners
                        {
                        // assume no optimizations
                        m_nOptimizationPlan = PLAN_NO_OPTIMIZE;
                        m_listenersCached   = null;

                        // it is possible to optimize if there are no key
                        // listeners AND no filtered listeners
                        if (mapKeyListeners == null || mapKeyListeners.isEmpty())
                            {
                            // check if there is only one listener and it has
                            // no filter
                            if (mapAllListeners.size() == 1)
                                {
                                Listeners listeners = (Listeners) mapAllListeners.get(null);
                                if (listeners != null)
                                    {
                                    m_nOptimizationPlan = PLAN_ALL_LISTENER;
                                    m_listenersCached   = listeners;
                                    }
                                }
                            }
                        }

                    azzert(m_nOptimizationPlan != PLAN_NONE);
                    return collectListeners(event);

                case PLAN_NO_LISTENERS:
                    return NO_LISTENERS;

                case PLAN_ALL_LISTENER:
                    return m_listenersCached;

                case PLAN_KEY_LISTENER:
                    return m_mapKeyListeners.containsKey(event.getKey()) || isVersionUpdate(event)
                            ? m_listenersCached
                            : NO_LISTENERS;

                case PLAN_NO_OPTIMIZE:
                    // fall through to the full implementation
                }
            }

        Listeners listeners = new Listeners();

        // add global listeners
        Map mapListeners = m_mapListeners;
        if (mapListeners != null)
            {
            MapEvent evt = unwrapEvent(event);
            if (evt instanceof FilterEvent)
                {
                FilterEvent evtFilter = (FilterEvent) evt;
                Filter[]    aFilter   = evtFilter.getFilter();

                listeners.setFilters(aFilter);
                }

            Filter[] aFilters = listeners.getFilters();
            if (aFilters == null)
                {
                // the server sent an event without a specified filter list;
                // attempt to match it to any registered filter-based listeners
                Object[] aEntry;
                synchronized (this)
                    {
                    aEntry = mapListeners.entrySet().toArray();
                    }

                List<Filter> listFilters = null;
                for (Object o : aEntry)
                    {
                    Map.Entry entry = (Map.Entry) o;

                    Filter filter = (Filter) entry.getKey();
                    if (filter == null || evaluateEvent(filter, event))
                        {
                        listeners.addAll((Listeners) entry.getValue());

                        if (filter != null)
                            {
                            if (listFilters == null)
                                {
                                listFilters = new ArrayList<>();
                                }
                            listFilters.add(filter);
                            }
                        }
                    }

                if (listFilters != null)
                    {
                    listeners.setFilters(listFilters.toArray(new Filter[listFilters.size()]));
                    }
                }
            else
                {
                synchronized (this)
                    {
                    for (Filter filter : aFilters)
                        {
                        listeners.addAll((Listeners) mapListeners.get(filter));
                        }
                    }
                }
            }

        // add key listeners, only if the event is not transformed (COH-9355)
        Map mapKeyListeners = m_mapKeyListeners;
        if (mapKeyListeners != null && !isTransformedEvent(event))
            {
            Collection<Listeners> colListeners = isVersionUpdate(event)
                    ? mapKeyListeners.values()
                    : Collections.singleton((Listeners) mapKeyListeners.get(event.getKey()));

            for (Listeners lsnrs : colListeners)
                {
                listeners.addAll(lsnrs);
                }
            }
        return listeners;
        }

    /**
    * Fire the specified map event.
    *
    * @param event    the map event
    * @param fStrict  if true then any RuntimeException thrown by event
    *                 handlers stops all further event processing and the
    *                 exception is re-thrown; if false then all exceptions
    *                 are logged and the process continues
    */
    public void fireEvent(MapEvent event, boolean fStrict)
        {
        Listeners listeners = collectListeners(event);
        enrichEvent(event, listeners).dispatch(listeners, fStrict);
        }

    /**
    * Return the minimum {@link VersionedPartitions versions} for the provided
    * {@link Filter}.
    *
    * @param filter  the filter the listeners were registered with
    *
    * @return the minimum {@link VersionedPartitions versions} for the provided
    *         {@link Filter}
    */
    public VersionedPartitions getMinVersions(Filter filter)
        {
        Listeners listeners = getListeners(filter);

        return min(getMinVersions(listeners.getAsynchronousListeners()),
                   getMinVersions(listeners.getSynchronousListeners()));
        }

    /**
    * Return the minimum {@link VersionedPartitions versions} for the provided
    * key.
    *
    * @param oKey  the key the listeners were registered with
    *
    * @return the minimum {@link VersionedPartitions versions} for the provided
    *         key
    */
    public VersionedPartitions getMinVersions(Object oKey)
        {
        Listeners listeners = getListeners(oKey);

        return min(getMinVersions(listeners.getAsynchronousListeners()),
                   getMinVersions(listeners.getSynchronousListeners()));
        }

    /**
    * Convert the specified map event into another MapEvent that ensures the
    * lazy event data conversion using the specified converters.
    *
    * @param event    the map event
    * @param mapConv  the source for the converted event
    * @param convKey  (optional) the key Converter
    * @param convVal  (optional) the value converter
    *
    * @return the converted MapEvent object
    */
    public static MapEvent convertEvent(MapEvent event, ObservableMap mapConv,
                                 Converter convKey, Converter convVal)
        {
        if (convKey == null)
            {
            convKey = NullImplementation.getConverter();
            }
        if (convVal == null)
            {
            convVal = NullImplementation.getConverter();
            }

        return ConverterCollections.getMapEvent(mapConv, event, convKey, convVal);
        }

    /**
    * Transform the given MapEvent into a FilterEvent if it is not already a
    * FilterEvent and there are matching filters associated with the
    * specified Listeners object.
    *
    * @param event      the MapEvent to transform, if necessary
    * @param listeners  the Listeners object
    *
    * @return a FilterEvent if the given MapEvent is not a FilterEvent and
    *         the specified Listeners object has associated filters;
    *         otherwise, the given MapEvent
    */
    public static MapEvent enrichEvent(MapEvent event, Listeners listeners)
        {
        if (!(event instanceof FilterEvent))
            {
            Filter[] aFilters = listeners.getFilters();
            if (aFilters != null)
                {
                event = new FilterEvent(event, aFilters);
                }
            }
        return event;
        }

    /**
    * Unwrap the specified map event and return the underlying source event.
    *
    * @param evt  the event to unwrap
    *
    * @return the unwrapped event
    */
    public static MapEvent unwrapEvent(MapEvent evt)
        {
        while (evt instanceof ConverterCollections.ConverterMapEvent)
            {
            evt = ((ConverterCollections.ConverterMapEvent) evt).getMapEvent();
            }

        return evt;
        }

    /**
    * Return the inner MapListener skipping any wrapper listeners.
    *
    * @param listener  the listener to check against
    *
    * @param <K> key type for the map
    * @param <V> value type for the map
    *
    * @return the inner MapListener skipping any wrapper listeners
    */
    public static <K, V> MapListener<K, V> unwrap(MapListener<K, V> listener)
        {
        while (listener instanceof WrapperListener)
            {
            listener = ((WrapperListener<K, V>) listener).getMapListener();
            }
        return listener;
        }

    /**
    * Check if the given listener is a PrimingListener or if it wraps one.
    *
    * @param listener  Map listener to check
    *
    * @return true iff the listener is a PrimingListener or wraps one
    */
    public static boolean isPrimingListener(MapListener listener)
        {
        while (true)
            {
            if (listener instanceof PrimingListener)
                {
                return true;
                }

            if (listener instanceof WrapperListener)
                {
                listener = ((WrapperListener) listener).getMapListener();
                }
            else
                {
                return false;
                }
            }
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Return true if the provided listener is version aware.
    *
    * @param listener  the listener to check
    *
    * @return true if the provided listener is version aware
    */
    protected boolean isVersionAware(MapListener listener)
        {
        return listener.isVersionAware();
        }

    /**
    * Return true if the provided MapEvent is due to a synthetic version
    * update.
    *
    * @param event  the event to test
    *
    * @return true if the provided MapEvent is due to a synthetic version
    *         update
    */
    protected boolean isVersionUpdate(MapEvent event)
        {
        if (!(event instanceof CacheEvent))
            {
            return false;
            }
        return ((CacheEvent<?, ?>) event).isVersionUpdate();
        }

    /**
    * Evaluate whether or not the specified event should be delivered to the
    * listener associated with the specified filter.
    *
    * @param filter  the filter
    * @param event   the event
    *
    * @return true iff the event should be delivered to the corresponding listener
    */
    protected boolean evaluateEvent(Filter filter, MapEvent event)
        {
        if (event instanceof CacheEvent &&
            ((CacheEvent) event).getTransformationState() == CacheEvent.TransformationState.NON_TRANSFORMABLE &&
            filter instanceof MapEventTransformer)
            {
            // if the event is marked as non-transformable, ensure that it does not
            // get delivered to listeners associated with transformer-filters
            return false;
            }

        if (filter instanceof InKeySetFilter)
            {
            // explicit support for the InKeySetFilter
            return ((InKeySetFilter) filter).getKeys().contains(event.getKey());
            }

        return filter.evaluate(event);
        }

    /**
    * Return true iff the specified event represents a transformed CacheEvent.
    *
    * @param event  the event to test
    *
    * @return true iff the event has been transformed
    */
    protected boolean isTransformedEvent(MapEvent event)
        {
        return event instanceof CacheEvent &&
               ((CacheEvent) event).getTransformationState() == CacheEvent.TransformationState.TRANSFORMED;
        }

    /**
    * Ensure that the specified map has a Listeners object associated
    * with the specified key and add the specified listener to it.
    */
    protected static void addSafeListener(Map mapListeners, Object anyKey, MapListener listener)
        {
        Listeners listeners = (Listeners) mapListeners.get(anyKey);
        if (listeners == null)
            {
            listeners = new Listeners();
            mapListeners.put(anyKey, listeners);
            }
        listeners.add(listener);
        }

    /**
    * Ensure that the specified map has a Listeners object associated
    * with the specified Filter and add the specified listener to it.
    */
    protected static void addSafeListener(Map mapListeners, Filter anyFilter, MapListener listener)
        {
        Listeners listeners = (Listeners) mapListeners.get(anyFilter);
        if (listeners == null)
            {
            listeners = new Listeners();
            if (anyFilter != null)
                {
                listeners.setFilters(new Filter[] {anyFilter});
                }
            mapListeners.put(anyFilter, listeners);
            }
        listeners.add(listener);
        }

    /**
    * Remove the specified listener from the Listeners object associated
    * with the specified key.
    */
    protected static void removeSafeListener(Map mapListeners, Object anyKey, MapListener listener)
        {
        Listeners listeners = (Listeners) mapListeners.get(anyKey);
        if (listeners != null)
            {
            listeners.remove(listener);
            if (listeners.isEmpty())
                {
                mapListeners.remove(anyKey);
                }
            }
        }

    /**
    * Add a state information (lite or standard) associated with
    * specified key and listener.
    */
    protected static void addListenerState(Map mapStandardListeners,
                                           Object anyKey, MapListener listener, boolean fLite)
        {
        Set setStandard = (Set) mapStandardListeners.get(anyKey);
        if (fLite)
            {
            if (setStandard != null)
                {
                setStandard.remove(listener);
                }
            }
        else
            {
            if (setStandard == null)
                {
                setStandard = new LiteSet();
                mapStandardListeners.put(anyKey, setStandard);
                }
            setStandard.add(listener);
            }
        }

    /**
    * Remove a state information (lite or standard) associated with
    * specified key and listener.
    */
    protected static void removeListenerState(Map mapStandardListeners,
                                              Object anyKey, MapListener listener)
        {
        Set setStandard = (Set) mapStandardListeners.get(anyKey);
        if (setStandard != null)
            {
            setStandard.remove(listener);
            if (setStandard.isEmpty())
                {
                mapStandardListeners.remove(anyKey);
                }
            }
        }

    /**
    * Return the minimum versions received for all partitions for the provided
    * listeners.
    *
    * @param aListeners  the listeners to interrogate
    *
    * @return the minimum versions received for all partitions for the provided
    *         listeners
    */
    protected static VersionedPartitions getMinVersions(EventListener[] aListeners)
        {
        VersionedPartitions versionsMin = null;

        for (int i = 0, c = aListeners.length; i < c; ++i)
            {
            MapListener listener = (MapListener) aListeners[i];
            if (listener.isVersionAware())
                {
                versionsMin = min(versionsMin, ((VersionAwareMapListener) listener).getVersions());
                }
            }

        return versionsMin;
        }

    /**
    * Return the minimum versions for the provided two partition versioned
    * data structures.
    *
    * @param versionsLHS  the first partition versioned object
    * @param versionsRHS  the second partition versioned object
    *
    * @return the minimum versions for the provided two partition versioned
    *         data structures
    */
    protected static VersionedPartitions min(VersionedPartitions versionsLHS, VersionedPartitions versionsRHS)
        {
        if (versionsLHS == null || versionsRHS == null || versionsLHS.equals(versionsRHS))
            {
            return versionsLHS == null ? versionsRHS : versionsLHS;
            }

        // generally it is expected that all MapListeners registered to the same
        // Filter / Key will receive and process all the same events thus should
        // have the same version for all partitions; this deriving the min version
        // is purely a safety measure

        DefaultVersionedPartitions versions = new DefaultVersionedPartitions();

        for (VersionedIterator iter = versionsLHS.iterator(); iter.hasNext(); )
            {
            long lVersion = iter.nextVersion();
            int  iPart    = iter.getPartition();
            versions.setPartitionVersion(iPart, Math.min(lVersion, versionsRHS.getVersion(iter.getPartition())));
            }

        return versions;
        }

    /**
    * Return a minimum version for the provided listeners.
    *
    * @param aListeners  the listeners to interrogate
    *
    * @return a minimum version for the provided listeners
    */
    protected static long getMinVersion(EventListener[] aListeners)
        {
        long lMinVersion = 0L;

        for (int i = 0, c = aListeners.length; i < c; ++i)
            {
            MapListener listener = (MapListener) aListeners[i];
            if (listener instanceof VersionAwareMapListener)
                {
                lMinVersion = lMinVersion == 0L
                        ? ((VersionAwareMapListener) listener).getCurrentVersion()
                        : Math.min(lMinVersion, ((VersionAwareMapListener) listener).getCurrentVersion());
                }
            }

        return lMinVersion;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Provide a string representation of the MapListenerSupport object.
    *
    * @return a human-readable description of the MapListenerSupport instance
    */
    public synchronized String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append("Global listeners:");
        if (m_mapListeners == null)
            {
            sb.append(" none");
            }
        else
            {
            for (Iterator iter = m_mapListeners.keySet().iterator(); iter.hasNext();)
                {
                Filter filter = (Filter) iter.next();

                sb.append("\n  Filter=")
                  .append(filter)
                  .append("; lite=")
                  .append(!containsStandardListeners(filter));
                }
            }

        sb.append("\nKey listeners:");
        if (m_mapKeyListeners == null)
            {
            sb.append(" none");
            }
        else
            {
            for (Iterator iter = m_mapKeyListeners.keySet().iterator(); iter.hasNext();)
                {
                Object oKey = iter.next();

                sb.append("\n  Key=")
                  .append(oKey)
                  .append("; lite=")
                  .append(!containsStandardListeners(oKey));
                }
            }

        return sb.toString();
        }


    // ----- inner classes --------------------------------------------------

    /**
    * An extension of the CacheEvent which may carry no values (old or new), but
    * instead holds on an array of Filter objects being the "cause" of the event.
    */
    public static class FilterEvent
            extends CacheEvent
        {
        /**
        * Constructs a new lite (no values are specified) FilterEvent.
        *
        * @param map         the ObservableMap object that fired the event
        * @param nId         this event's id
        * @param oKey        the key into the map
        * @param fSynthetic  true iff the event is caused by the cache
        *                    internal processing such as eviction or loading
        * @param aFilter     an array of filters that caused this event
        */
        public FilterEvent(ObservableMap map, int nId, Object oKey,
                           boolean fSynthetic, Filter[] aFilter)
            {
            this(map, nId, oKey, null, null, fSynthetic, false, aFilter);
            }

        /**
        * Constructs a new lite (no values are specified) FilterEvent.
        *
        * @param map         the ObservableMap object that fired the event
        * @param nId         this event's id
        * @param oKey        the key into the map
        * @param fSynthetic  true iff the event is caused by the cache
        *                    internal processing such as eviction or loading
        * @param fPriming    a flag indicating whether or not the event is a priming event
        * @param aFilter     an array of filters that caused this event
        */
        public FilterEvent(ObservableMap map, int nId, Object oKey,
                           boolean fSynthetic, boolean fPriming, Filter[] aFilter)
            {
            this(map, nId, oKey, null, null, fSynthetic, fPriming, aFilter);
            }

        /**
        * Constructs a new FilterEvent.
        *
        * @param map         the ObservableMap object that fired the event
        * @param nId         this event's id
        * @param oKey        the key into the map
        * @param oValueOld   the old value
        * @param oValueNew   the new value
        * @param fSynthetic  true iff the event is caused by the cache
        *                    internal processing such as eviction or loading
        * @param fPriming    a flag indicating whether or not the event is a priming event
        * @param aFilter     an array of filters that caused this event
        */
        public FilterEvent(ObservableMap map, int nId, Object oKey,
                           Object oValueOld, Object oValueNew,
                           boolean fSynthetic, boolean fPriming, Filter[] aFilter)
            {
            this(map, nId, oKey, oValueOld, oValueNew, fSynthetic,
                 TransformationState.TRANSFORMABLE, fPriming, aFilter);
            }

        /**
        * Constructs a new FilterEvent.
        *
        * @param map             the ObservableMap object that fired the event
        * @param nId             this event's id
        * @param oKey            the key into the map
        * @param oValueOld       the old value
        * @param oValueNew       the new value
        * @param fSynthetic      true iff the event is caused by the cache
        *                        internal processing such as eviction or loading
        * @param transformState  the {@link TransformationState state} describing
        *                        how this event has been or should be transformed
        * @param aFilter         an array of filters that caused this event
        */
        public FilterEvent(ObservableMap map, int nId, Object oKey,
                           Object oValueOld, Object oValueNew,
                           boolean fSynthetic, TransformationState transformState,
                           Filter[] aFilter)
            {
            super(map, nId, oKey, oValueOld, oValueNew, fSynthetic, transformState, false);

            azzert(aFilter != null);
            f_aFilter = aFilter;
            f_event   = null;
            }

        /**
        * Constructs a new FilterEvent.
        *
        * @param map             the ObservableMap object that fired the event
        * @param nId             this event's id
        * @param oKey            the key into the map
        * @param oValueOld       the old value
        * @param oValueNew       the new value
        * @param fSynthetic      true iff the event is caused by the cache
        *                        internal processing such as eviction or loading
        * @param transformState  the {@link TransformationState state} describing
        *                        how this event has been or should be transformed
        * @param fPriming        a flag indicating whether or not the event is a priming event
        * @param aFilter         an array of filters that caused this event
        */
        public FilterEvent(ObservableMap map, int nId, Object oKey,
                           Object oValueOld, Object oValueNew,
                           boolean fSynthetic, TransformationState transformState,
                           boolean fPriming,
                           Filter[] aFilter)
            {
            this(map, nId, oKey, oValueOld, oValueNew, fSynthetic, transformState, fPriming, false, aFilter);
            }

        /**
        * Constructs a new FilterEvent.
        *
        * @param map             the ObservableMap object that fired the event
        * @param nId             this event's id
        * @param oKey            the key into the map
        * @param oValueOld       the old value
        * @param oValueNew       the new value
        * @param fSynthetic      true iff the event is caused by the cache
        *                        internal processing such as eviction or loading
        * @param transformState  the {@link TransformationState state} describing
        *                        how this event has been or should be transformed
        * @param fPriming        a flag indicating whether or not the event is a priming event
        * @param fExpired        true iff the event results from an eviction due to time
        * @param aFilter         an array of filters that caused this event
        *
        * @since 22.06
        */
        public FilterEvent(ObservableMap map, int nId, Object oKey,
                           Object oValueOld, Object oValueNew,
                           boolean fSynthetic, TransformationState transformState,
                           boolean fPriming, boolean fExpired,
                           Filter[] aFilter)
            {
            super(map, nId, oKey, oValueOld, oValueNew, fSynthetic, transformState, fPriming, fExpired);

            azzert(aFilter != null);
            f_aFilter = aFilter;
            f_event   = null;
            }

        /**
        * Constructs a new FilterEvent that wraps the given MapEvent.
        *
        * @param event    the wrapped MapEvent
        * @param aFilter  an array of filters that caused this event
        */
        public FilterEvent(MapEvent event, Filter[] aFilter)
            {
            super(event.getMap(), event.getId(), null, null, null,
                    event instanceof CacheEvent && ((CacheEvent) event).isSynthetic(),
                    event instanceof CacheEvent
                            ? ((CacheEvent) event).getTransformationState()
                            : TransformationState.TRANSFORMABLE,
                  event instanceof CacheEvent && ((CacheEvent) event).isPriming(),
                  event instanceof CacheEvent && ((CacheEvent) event).isExpired());

            with(event.getPartition(), event.getVersion());

            azzert(aFilter != null);
            f_aFilter = aFilter;
            f_event   = event;
            }

        /**
        * Return an array of filters that are the cause of this event.
        *
        * @return an array of filters
        */
        public Filter[] getFilter()
            {
            return f_aFilter;
            }

        /**
        * Return the wrapped event.
        *
        * @return the underlying {@link MapEvent}
        *
        * @since 12.2.1.4
        */
        public MapEvent getMapEvent()
            {
            return f_event;
            }

        /**
        * Get the event's description.
        *
        * @return this event's description
        */
        protected String getDescription()
            {
            return super.getDescription()
                + ", filters=" + new ImmutableArrayList(f_aFilter);
            }

        // ----- MapEvent methods ---------------------------------------

        /**
        * {@inheritDoc}
        */
        public Object getKey()
            {
            return f_event == null ? super.getKey() : f_event.getKey();
            }

        /**
        * {@inheritDoc}
        */
        public Object getOldValue()
            {
            return f_event == null ? super.getOldValue() : f_event.getOldValue();
            }

        /**
        * {@inheritDoc}
        */
        public Object getNewValue()
            {
            return f_event == null ? super.getNewValue() : f_event.getNewValue();
            }

        // ----- data members -------------------------------------------

        /**
        * Filters that caused the event.
        */
        protected final Filter[] f_aFilter;

        /**
        * Optional wrapped MapEvent.
        */
        protected final MapEvent f_event;
        }

    /**
    * A tag interface indicating that tagged MapListener implementation
    * has to receive the MapEvent notifications in a synchronous manner.
    * <p>
    * Consider a MapListener that subscribes to receive notifications for
    * distributed (partitioned) cache. All events notifications are received
    * by the service thread and immediately queued to be processed by the
    * dedicated event dispatcher thread. This makes it impossible to
    * differentiate between the event caused by the updates made by this
    * thread and any other thread (possibly in a different VM).
    * Forcing the events to be processed on the service thread guarantees
    * that by the time "put" or "remove" requests return to the caller
    * all relevant MapEvent notifications raised on the same member as
    * the caller have been processed (due to the "in order delivery" rule
    * enforced by the TCMP).
    * <p>
    * This interface should be considered as a very advanced feature, so
    * a MapListener implementation that is tagged as a SynchronousListener
    * must exercise extreme caution during event processing since any delay
    * with return or unhandled exception will cause a delay or complete
    * shutdown of the corresponding cache service.
    * <p>
    * <b>Note:</b> The contract by the event producer in respect to the
    * SynchronousListener is somewhat weaker then the general one.
    * First, the SynchronousListener implementation should make no assumptions
    * about the event source obtained by {@link MapEvent#getMap()}.
    * Second, in the event of [automatic] service restart, the listener has
    * to be re-registered manually (for example, in response to the
    * {@link com.tangosol.net.MemberEvent#MEMBER_JOINED MEMBER_JOINED} event).
    * Third, and the most important, no calls against the NamedCache are
    * allowed during the synchronous event processing (the only exception
    * being a call to remove the listener itself).
    */
    public interface SynchronousListener<K, V>
            extends com.tangosol.util.SynchronousListener<K, V>, MapListener<K, V>
        {
        @Override
        default int characteristics()
            {
            return SYNCHRONOUS;
            }
        }

    /**
    * A tag interface indicating that this listener is registered as a
    * synchronous listener for lite events (carrying only a key) and generates
    * a "priming" event when registered.
    */
   public interface PrimingListener<K, V>
            extends SynchronousListener<K, V>
       {
       }

    /**
    * A base class for various wrapper listener classes.
    */
    public abstract static class WrapperListener<K, V>
            extends MultiplexingMapListener<K, V>
        {
        /**
        * Construct WrapperSynchronousListener.
        *
        * @param listener  the wrapped MapListener
        */
        public WrapperListener(MapListener<K, V> listener)
            {
            azzert(listener != null);

            f_listener = listener;
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void onMapEvent(MapEvent<K, V> evt)
            {
            evt.dispatch(f_listener);
            }

        @Override
        public int characteristics()
            {
            return f_listener.characteristics();
            }

        /**
        * Return the underlying MapListener.
        *
        * @return the underlying MapListener
        */
        public MapListener<K, V> getMapListener()
            {
            return f_listener;
            }

        // ----- Object methods -----------------------------------------

        /**
        * Determine a hash value for the WrapperListener object
        * according to the general {@link Object#hashCode()} contract.
        *
        * @return an integer hash value for this WrapperListener
        */
        public int hashCode()
            {
            return f_listener.hashCode();
            }

        /**
        * Compare the WrapperListener with another object to
        * determine equality.
        *
        * @return true iff this WrapperListener and the passed
        *          object are equivalent listeners
        */
        public boolean equals(Object o)
            {
            return o != null &&
                    getClass() == o.getClass() &&
                    Base.equals(f_listener, ((WrapperListener) o).f_listener);
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return ClassHelper.getSimpleName(getClass()) + "{listener=" + f_listener + "}";
            }

        // ----- data members -------------------------------------------

        /**
        * Wrapped MapListener.
        */
        protected final MapListener<K, V> f_listener;
        }

    /**
    * A wrapper class that turns the specified MapListener into
    * a synchronous listener.
    */
    public static class WrapperSynchronousListener<K, V>
            extends    WrapperListener<K, V>
            implements SynchronousListener<K, V>
        {
        /**
        * Construct WrapperSynchronousListener.
        *
        * @param listener  the wrapped MapListener
        */
        public WrapperSynchronousListener(MapListener<K, V> listener)
            {
            super(listener);
            }
        }

    /**
    * A wrapper class that turns the specified MapListener into
    * a priming listener.
    */
    public static class WrapperPrimingListener
            extends    WrapperSynchronousListener
            implements PrimingListener
        {
        public WrapperPrimingListener(MapListener listener)
            {
            super(listener);
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void onMapEvent(MapEvent evt)
            {
            int nId = evt.getId();

            switch (nId)
                {
                case MapEvent.ENTRY_INSERTED:
                    getMapListener().entryInserted(evt);
                    break;
                case MapEvent.ENTRY_UPDATED:
                    getMapListener().entryUpdated(evt);
                    break;
                case MapEvent.ENTRY_DELETED:
                    getMapListener().entryDeleted(evt);
                    break;
                default:
                    throw new RuntimeException(
                            "Unknown map event id: " + nId);
                }
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * A plan has not yet been formed.
    */
    protected static final int PLAN_NONE         = 0;

    /**
    * There are no listeners.
    */
    protected static final int PLAN_NO_LISTENERS = 1;

    /**
    * There is one all-keys non-filtered listener.
    */
    protected static final int PLAN_ALL_LISTENER = 2;

    /**
    * There is one key listener (even if for multiple keys).
    */
    protected static final int PLAN_KEY_LISTENER = 3;

    /**
    * There is no optimized plan, so just use the default approach.
    */
    protected static final int PLAN_NO_OPTIMIZE  = 4;

    /**
    * An empty set of Listeners. Because this is a theoretically mutable
    * object that is used as a return value, it is purposefully not static.
    */
    protected final Listeners NO_LISTENERS = new Listeners();


    // ----- data members ---------------------------------------------------

    /**
    * The collections of MapListener objects that have signed up for
    * notifications from an ObservableMap implementation keyed by the
    * corresponding Filter objects.
    */
    protected Map m_mapListeners;

    /**
    * The collections of MapListener objects that have signed up for key based
    * notifications from an ObservableMap implementation keyed by the
    * corresponding key objects.
    */
    protected Map m_mapKeyListeners;

    // consider adding listener tag support to Listeners class, which
    // would allow getting rid of the following data structures ...
    /**
    * The subset of standard (not lite) global listeners. The keys are the
    * Filter objects, the values are sets of corresponding standard listeners.
    */
    protected Map m_mapStandardListeners;

    /**
    * The subset of standard (not lite) key listeners. The keys are the
    * key objects, the values are sets of corresponding standard listeners.
    */
    protected Map m_mapStandardKeyListeners;

    /**
    * The optimization plan which indicates the fastest way to put together a
    * set of listeners.
    */
    protected int m_nOptimizationPlan;

    /**
    * A cached set of Listeners.
    */
    protected Listeners m_listenersCached;
    }
