/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.internal;

/**
 * An enumeration to represent the possible causes of backing map events.
 *
 * @author bo
 * @since 21.12
 */
public enum Cause
    {
    /**
     * <code>REGULAR</code> is for regular inserts, updates and delete events.
     */
    REGULAR,

    /**
     * <code>EVICTION</code> is for deletes that are due to cache eviction.
     */
    EVICTION,

    /**
     * <code>PARTITIONING</code> is used for inserts and deletes that
     * have occurred due to cache partitions being load-balanced or recovered.
     */
    PARTITIONING,

    /**
     * <code>STORE_COMPLETED</code> is for update events due to a storage decoration
     * change on an entry. Coherence updates a decoration after a successful store
     * operation on a write-behind store. ie: an asynchronous store has completed.
     */
    STORE_COMPLETED;
    }
