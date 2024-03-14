/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.util.EventListener;

/**
 * The listener interface for receiving CollectionEvents.
 */
public interface CollectionListener<V>
        extends EventListener
    {
    /**
    * Invoked when a collection entry has been inserted.
    *
    * @param evt  the CollectionEvent carrying the insert information
    */
   void entryInserted(CollectionEvent<V> evt);

    /**
    * Invoked when a collection entry has been updated.
    *
    * @param evt  the CollectionEvent carrying the update information
    */
   void entryUpdated(CollectionEvent<V> evt);

    /**
    * Invoked when a collection entry has been removed.
    *
    * @param evt  the CollectionEvent carrying the delete information
    */
   void entryDeleted(CollectionEvent<V> evt);

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
   default int characteristics()
        {
        return ASYNCHRONOUS;
        }

    /**
     * Returns true if this listener should be fired asynchronously.
     *
     * @return true if this listener should be fired asynchronously
     */
   default boolean isAsynchronous()
        {
        return !isSynchronous();
        }

    /**
     * Returns true if this listener should be fired synchronously.
     *
     * @return true if this listener should be fired synchronously
     */
   default boolean isSynchronous()
        {
        return (characteristics() & SYNCHRONOUS) != 0;
        }

    /**
     * Returns true if this listener should track versions thus allowing missed
     * versions to be replayed if supported by the event source.
     *
     * @return true if this listener should track versions
     */
   default boolean isVersionAware()
        {
        return (characteristics() & VERSION_AWARE) != 0;
        }
    
    // ----- constants ------------------------------------------------------

    /**
     * A flag that indicates this MapListener can be fired asynchronously.
     */
    int ASYNCHRONOUS   = 0x00000000;

    /**
     * A flag that indicates this MapListener should be fired synchronously.
     */
    int SYNCHRONOUS    = 0x00000001;

    /**
     * A flag that indicates this MapListener should track versions thus allow
     * missed versions to be replayed if supported by the source of events.
     */
    int VERSION_AWARE  = 0x00000002;
    }
