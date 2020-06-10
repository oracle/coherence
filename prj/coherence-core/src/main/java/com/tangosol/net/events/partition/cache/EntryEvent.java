/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition.cache;

import com.tangosol.util.BinaryEntry;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An EntryEvent captures information relating to actions performed
 * on {@link BinaryEntry entries}.
 * <p>
 * Note that the semantics for interceptors handling pre-commit events
 * (e.g. INSERTING, UPDATING, REMOVING) are analogous to the semantics for
 * triggers.  Interceptors are permitted to change the entry content before
 * the operation is committed.  Such interceptors execute after triggers and
 * become the final arbiter of the entry value.
 * <p>
 * Post-commit EntryEvents (e.g. INSERTED, UPDATED, REMOVED) do not
 * allow interceptors to modify the entries' content.
 *
 * @author bo, nsa, rhan, mwj  2011.03.29
 * @since Coherence 12.1.2
 */
public interface EntryEvent<K, V>
        extends Event<EntryEvent.Type>, Iterable<BinaryEntry<K, V>>
    {
    /**
     * Return the immutable Set of {@link BinaryEntry entries} on which the
     * action represented by this {@link EntryEvent} occurred.
     *
     * @return the Set of entries represented by this event
     */
    public Set<BinaryEntry<K, V>> getEntrySet();

    /**
     * Returns an iterator over the {@link BinaryEntry entries} in this event.
     *
     * @return an iterator over the {@link BinaryEntry entries} in this event.
     */
    @Override
    public default Iterator<BinaryEntry<K, V>> iterator()
        {
        return getEntrySet().iterator();
        }

    // ----- constants --------------------------------------------------

    /**
     * The {@link EntryEvent} types.
     */
    public static enum Type
        {
        /**
         * This {@link EntryEvent} is dispatched before one or more
         * {@link BinaryEntry}(s) are inserted into the backing-map.
         * <p>
         * The following holds:
         * <ul>
         *   <li>The BinaryEntry(s) provided for this event type are mutable</li>
         *   <li>A lock will be held for each entry during the processing of
         *       this event, preventing concurrent updates</li>
         *   <li>Throwing an exception from this event will prevent the
         *       operation from being committed</li>
         * </ul>
         */
        INSERTING,

        /**
         * This {@link EntryEvent} is dispatched after one or more {@link
         * BinaryEntry}(s) have been inserted into the backing-map.  This
         * event is raised after the insert has been committed.  The
         * BinaryEntry(s) provided for this event type are immutable. For a
         * given BinaryEntry, events will be raised in the same order as the
         * changes occurred.
         */
        INSERTED,

        /**
         * This {@link EntryEvent} is dispatched before a {@link
         * BinaryEntry} is updated in backing-map.
         * <p>
         * The following holds:
         * <ul>
         *   <li>The BinaryEntry provided for this event type is mutable</li>
         *   <li>A lock will be held for each Entry during the processing of
         *       this event preventing concurrent updates</li>
         *   <li>Throwing an exception from this type of event will prevent the
         *       operation from being committed</li>
         * </ul>
         */
        UPDATING,

        /**
         * This {@link EntryEvent} is dispatched after one or more {@link
         * BinaryEntry}(s) have been updated in the backing-map.  This
         * event is raised after the update has been committed.  The
         * BinaryEntry(s) provided for this event type are immutable. For a
         * given BinaryEntry, events will be raised in the same order as the
         * changes occurred.
         */
        UPDATED,

        /**
         * This {@link EntryEvent} is dispatched before a {@link
         * BinaryEntry} has been removed from backing-map.
         * <p>
         * The following holds:
         * <ul>
         *   <li>The BinaryEntry provided for this event type is mutable</li>
         *   <li>A lock will be held for each Entry during the processing of
         *       this event preventing concurrent updates</li>
         *   <li>Throwing an exception from this type of event will prevent the
         *       operation from being committed</li>
         * </ul>
         */
        REMOVING,

        /**
         * This {@link EntryEvent} is dispatched after one or more {@link
         * BinaryEntry}(s) have been removed from the backing-map.  This
         * event is raised after the removal has been committed.  The
         * BinaryEntry(s) provided for this event type are immutable. For a
         * given BinaryEntry, events will be raised in the same order as the
         * changes occurred.
         */
        REMOVED
        }
    }
