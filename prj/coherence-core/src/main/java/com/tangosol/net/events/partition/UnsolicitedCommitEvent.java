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

/**
 * An UnsolicitedCommitEvent captures changes pertaining to all observed
 * mutations performed against caches that were not directly caused (solicited)
 * by the partitioned service.
 * <p>
 * These events may be due to changes made internally by the backing map, such
 * as eviction, or referrers of the backing map causing changes.
 *
 * @author hr/gg  2014.04.02
 * @since Coherence 12.2.1
 */
@SuppressWarnings("rawtypes")
public interface UnsolicitedCommitEvent
        extends Event<UnsolicitedCommitEvent.Type>, Iterable<BinaryEntry>
    {
    /**
     * A set of {@link BinaryEntry entries} observed to have been modified
     * without being caused by the partitioned service.
     *
     * @return a set of entries observed to have been modified without being
     *         caused by the partitioned service
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

    // ----- inner class: Type ----------------------------------------------

    /**
     * The UnsolicitedCommitEvent types.
     */
    public static enum Type
        {
        /**
         * A COMMITTED event is the only event raised by the {@link UnsolicitedCommitEvent}
         * as the mutation has already occurred to the underlying backing map(s).
         * This event will contain all modified entries which may span multiple
         * backing maps. The BinaryEntry instances passed for this event type
         * are immutable.
         */
        COMMITTED
        }
    }
