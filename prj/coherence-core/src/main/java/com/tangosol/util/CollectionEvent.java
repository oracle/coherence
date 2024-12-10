/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


public interface CollectionEvent<V>
    {
    /**
    * Return this event's id. The event id is one of the ENTRY_*
    * enumerated constants.
    *
    * @return an id
    */
    int getId();

    /**
    * Return an old value associated with this event.
    * <p>
    * The old value represents a value deleted from or updated in a collection.
    * It is always null for "insert" notifications.
    *
    * @return an old value
    */
    V getOldValue();

    /**
    * Return a new value associated with this event.
    * <p>
    * The new value represents a new value inserted into or updated in
    * a collection. It is always null for "delete" notifications.
    *
    * @return a new value
    */
    V getNewValue();


    /**
     * Return the partition this event represents or -1 if the event source
     * is not partition aware.
     *
     * @return the partition this event represents or -1 if the event source
     *         is not partition aware
     */
    int getPartition();

    /**
     * Return the version that represents the change that caused this event.
     * The meaning of this version, and therefore causality of versions, is
     * defined by the event source.
     *
     * @return the version that represents the change that caused this event
     */
    long getVersion();


    /**
     * Determine whether this event is an insert event.
     *
     * @return  {@code true} if this event is an insert event
     */
    boolean isInsert();

    /**
     * Determine whether this event is an update event.
     *
     * @return  {@code true} if this event is an update event
     */
    boolean isUpdate();

    /**
     * Determine whether this event is a delete event.
     *
     * @return  {@code true} if this event is a delete event
     */
    boolean isDelete();
    }
