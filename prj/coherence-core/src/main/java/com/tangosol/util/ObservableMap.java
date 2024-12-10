/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Map;


/**
* ObservableMap interface represents an object with a model being 
* a Map that allows for pluggable notifications for occurring changes.
* <p>
* This is primarily intended for maps that have automatic pruning and
* purging strategies or maps that are asynchronously modified by different
* threads.
* <p>
* Starting from Coherence 2.3 it supports optimizations that optionally 
* do not require the map values to be included in the map events,
* allowing a "lite" event to be delivered and saving memory, processing 
* and bandwidth for distributed applications.
*
* @param <K>  the type of the Map entry keys
* @param <V>  the type of the Map entry values
*
* @see com.tangosol.net.NamedCache
* @see com.tangosol.net.cache.LocalCache
* @see com.tangosol.util.ObservableHashMap
*
* @author gg  2002.02.11
* @author cp  2003.05.21
*/
public interface ObservableMap<K, V>
        extends Map<K, V>
    {
    // ----- simple interface -----------------------------------------------

    /**
    * Add a standard map listener that will receive all events (inserts,
    * updates, deletes) that occur against the map, with the key, old-value 
    * and new-value included. This has the same result as the following call:
    * <pre>
    *   addMapListener(listener, (Filter) null, false);
    * </pre>
    *
    * @param listener the {@link MapEvent} listener to add
    */
    public void addMapListener(MapListener<? super K, ? super V> listener);

    /**
    * Remove a standard map listener that previously signed up for all 
    * events. This has the same result as the following call:
    * <pre>
    *   removeMapListener(listener, (Filter) null);
    * </pre>
    *
    * @param listener the listener to remove
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener);


    // ----- specific key listeners -----------------------------------------

    /**
    * Add a map listener for a specific key.
    * <p>
    * The listeners will receive MapEvent objects, but if fLite is passed as
    * true, they <i>might</i> not contain the OldValue and NewValue
    * properties.
    * <p>
    * To unregister the MapListener, use the
    * {@link #removeMapListener(MapListener, Object)} method.

    * @param listener  the {@link MapEvent} listener to add
    * @param key       the key that identifies the entry for which to raise
    *                  events
    * @param fLite     true to indicate that the {@link MapEvent} objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    *
    * @since Coherence 2.3
    */
    public void addMapListener(MapListener<? super K, ? super V> listener, K key, boolean fLite);

    /**
    * Remove a map listener that previously signed up for events about a
    * specific key.
    *
    * @param listener  the listener to remove
    * @param key       the key that identifies the entry for which to raise
    *                  events
    *
    * @since Coherence 2.3
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener, K key);


    // ----- advanced interface ---------------------------------------------

    /**
    * Add a map listener that receives events based on a filter evaluation.
    * <p>
    * The listeners will receive MapEvent objects, but if fLite is passed as
    * true, they <i>might</i> not contain the OldValue and NewValue
    * properties.
    * <p>
    * To unregister the MapListener, use the
    * {@link #removeMapListener(MapListener, Filter)} method.
    *
    * @param listener  the {@link MapEvent} listener to add
    * @param filter    a filter that will be passed MapEvent objects to select 
    *                  from; a MapEvent will be delivered to the listener only 
    *                  if the filter evaluates to true for that MapEvent (see
    *                  {@link com.tangosol.util.filter.MapEventFilter});
    *                  null is equivalent to a filter that alway returns true
    * @param fLite     true to indicate that the {@link MapEvent} objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    *
    * @since Coherence 2.3
    */
    public void addMapListener(MapListener<? super K, ? super V> listener, Filter filter, boolean fLite);

    /**
    * Remove a map listener that previously signed up for events based on a 
    * filter evaluation.
    *
    * @param listener  the listener to remove
    * @param filter    the filter that was passed into the corresponding
    *                   addMapListener() call
    *
    * @since Coherence 2.3
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener, Filter filter);
    }