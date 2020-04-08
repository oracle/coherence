/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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
    }