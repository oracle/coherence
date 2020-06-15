/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition.cache;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap.EntryProcessor;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;


/**
 * An EntryProcessorEvent captures information relating to the execution of
 * {@link EntryProcessor}s.
 *
 * @author bo, nsa, rhan, mwj  2011.03.29
 * @since Coherence 12.1.2
 */
@SuppressWarnings("rawtypes")
public interface EntryProcessorEvent
        extends Event<EntryProcessorEvent.Type>, Iterable<BinaryEntry>
    {
    /**
     * Return a Set of {@link BinaryEntry entries} being processed by the
     * entry processor.
     *
     * @return the Set of entries represented by this event
     */
    public Set<BinaryEntry> getEntrySet();

    /**
     * Return the {@link EntryProcessor} associated with this {@link
     * EntryProcessorEvent}.
     *
     * @return the entry processor associated with this event
     */
    public EntryProcessor getProcessor();

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

    // ----- constants ------------------------------------------------------

    /**
     * The {@link EntryProcessorEvent} types.
     */
    public static enum Type
        {
        /**
         * This {@link EntryProcessorEvent} is raised prior to executing an
         * {@link EntryProcessor} on a set of entries.
         * <p>
         * The following holds:
         * <ul>
         *   <li>The {@link EntryProcessor} provided for this event type is
         *       mutable. The processor could be shared across threads, so
         *       processor implementations modified in this fashion must ensure
         *       thread-safety</li>
         *   <li>A lock will be held for each Entry during the processing of
         *       this event preventing concurrent updates</li>
         *   <li>Throwing an exception from this event will prevent the
         *       operation from being committed</li>
         * </ul>
         */
        EXECUTING,

        /**
         * This {@link EntryProcessorEvent} is dispatched after an {@link
         * EntryProcessor} has been executed.
         */
        EXECUTED
        }
    }
