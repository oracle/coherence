/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition;

import com.tangosol.util.BinaryEntry;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;


/**
 * A TransactionEvent captures information pertaining to all mutations
 * performed within the context of a single request. All modified
 * entries are passed to the interceptor(s) of this event. All entries are
 * bound to the same {@link com.tangosol.net.PartitionedService}, but may
 * belong to different caches.
 *
 * @author rhl/hr/gg  2012.09.21
 * @since Coherence 12.1.2
 */
@SuppressWarnings("rawtypes")
public interface TransactionEvent
        extends Event<TransactionEvent.Type>, Iterable<BinaryEntry>
    {
    /**
     * A set of {@link BinaryEntry entries} enlisted within this
     * transaction.
     *
     * @return a set of entries enlisted within this transaction
     */
    public Set<BinaryEntry> getEntrySet();

    /**
     * Returns an iterator over the {@link BinaryEntry entries} in this event.
     *
     * @return an iterator over the {@link BinaryEntry entries} in this event.
     */
    @Override
    public default Iterator<BinaryEntry> iterator()
        {
        return getEntrySet().iterator();
        }

    /**
     * The TransactionEvent types.
     */
    public static enum Type
        {
        /**
         * A COMMITTING event is raised prior to any updates to the
         * underlying backing map. This event will contain all modified
         * entries which may span multiple backing maps.
         * <p>
         * The following holds:
         * <ul>
         *   <li>The BinaryEntry instances passed for this event type are mutable.</li>
         *   <li>A lock will be held for each entry during the processing of
         *       this event, preventing concurrent updates.</li>
         *   <li>Throwing an exception from this event will prevent the
         *       operation from being committed.</li>
         * </ul>
         */
        COMMITTING,

        /**
         * A COMMITTED event is raised after any mutations have been
         * committed to the underlying backing maps. This event will contain
         * all modified entries which may span multiple backing maps.
         * The BinaryEntry instances passed for this event type
         * are immutable.
         */
        COMMITTED
        }
    }
