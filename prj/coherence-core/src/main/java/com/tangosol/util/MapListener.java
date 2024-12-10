/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.EventListener;


/**
* The listener interface for receiving MapEvents.
*
* @param <K>  the type of the cache entry keys
* @param <V>  the type of the cache entry values
*
* @author gg  2002.02.11
* @author hr  2020.11.07
*
* @see com.tangosol.util.ObservableMap
* @see com.tangosol.util.MapEvent
*/
public interface MapListener<K, V>
        extends EventListener
    {
    /**
    * Invoked when a map entry has been inserted.
    *
    * @param evt  the MapEvent carrying the insert information
    */
    public void entryInserted(MapEvent<K, V> evt);

    /**
    * Invoked when a map entry has been updated.
    *
    * @param evt  the MapEvent carrying the update information
    */
    public void entryUpdated(MapEvent<K, V> evt);

    /**
    * Invoked when a map entry has been removed.
    *
    * @param evt  the MapEvent carrying the delete information
    */
    public void entryDeleted(MapEvent<K, V> evt);

    /**
     * Make this MapListener synchronous.
     * <p>
     * This should be considered as a very advanced feature, as a synchronous
     * MapListener must exercise extreme caution during event processing since
     * any delay with return or unhandled exception will cause a delay or
     * complete shutdown of the corresponding service.
     *
     * @return a synchronous MapListener
     *
     * @see SynchronousListener
     * @see MapListenerSupport.SynchronousListener
     */
    public default MapListener<K, V> synchronous()
        {
        return new MapListenerSupport.WrapperSynchronousListener(this);
        }

    /**
     * A bit mask representing the set of characteristics of this listener.
     * <p>
     * By default, characteristics suggest this listener is {@link #ASYNCHRONOUS}.
     *
     * @return a bit mask representing the set of characteristics of this listener
     *
     * @see #ASYNCHRONOUS
     * @see #SYNCHRONOUS
     * @see #VERSION_AWARE
     */
    public default int characteristics()
        {
        return ASYNCHRONOUS;
        }

    /**
     * Returns true if this listener should be fired asynchronously.
     *
     * @return true if this listener should be fired asynchronously
     */
    public default boolean isAsynchronous()
        {
        return !isSynchronous();
        }

    /**
     * Returns true if this listener should be fired synchronously.
     *
     * @return true if this listener should be fired synchronously
     */
    public default boolean isSynchronous()
        {
        return (characteristics() & SYNCHRONOUS) != 0;
        }

    /**
     * Returns true if this listener should track versions thus allowing missed
     * versions to be replayed if supported by the event source.
     *
     * @return true if this listener should track versions
     */
    public default boolean isVersionAware()
        {
        return (characteristics() & VERSION_AWARE) != 0;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A flag that indicates this MapListener can be fired asynchronously.
     */
    public static final int ASYNCHRONOUS   = CollectionListener.ASYNCHRONOUS;

    /**
     * A flag that indicates this MapListener should be fired synchronously.
     */
    public static final int SYNCHRONOUS    = CollectionListener.SYNCHRONOUS;

    /**
     * A flag that indicates this MapListener should track versions thus allow
     * missed versions to be replayed if supported by the source of events.
     */
    public static final int VERSION_AWARE  = CollectionListener.VERSION_AWARE;
    }