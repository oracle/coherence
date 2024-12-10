/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.MapTrigger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class providing various functionality related to event handling.
 *
 * @author hr  2012.09.04
 * @since Coherence 12.1.2
 */
public abstract class EventHelper
    {
    /**
     * Converts the provided {@link TransactionEvent} into a map keyed by
     * {@link EntryEvent} and with a value of a Set of {@link BinaryEntry entries}.
     *
     * @param event  the TransactionEvent to convert
     *
     * @return a map of event {@link EntryEvent.Type types} to a set of {@link
     *         BinaryEntry entries}
     */
    public static Map<EntryEvent.Type, Set<BinaryEntry>> getEntryEventsMap(TransactionEvent event)
        {
        return getEntryEventsMap(event.getEntrySet(), event.getType() == TransactionEvent.Type.COMMITTING);
        }

    /**
     * Converts the provided {@link UnsolicitedCommitEvent} into a map keyed by
     * {@link EntryEvent} and with a value of a Set of {@link BinaryEntry entries}.
     *
     * @param event  the UnsolicitedCommitEvent to convert
     *
     * @return a map of event {@link EntryEvent.Type types} to a set of {@link
     *         BinaryEntry entries}
     */
    public static Map<EntryEvent.Type, Set<BinaryEntry>> getEntryEventsMap(UnsolicitedCommitEvent event)
        {
        return getEntryEventsMap(event.getEntrySet(), false);
        }

    /**
     * Converts the provided set of {@link BinaryEntry entries} into a map keyed
     * by {@link EntryEvent} and with a value of a Set of entries.
     *
     * @param setEntries  the set of entries to categorize into EntryEvent.Types
     * @param fPreEvent   whether the entries are part of a pre-event
     *
     * @return a map of event {@link EntryEvent.Type types} to a set of {@link
     *         BinaryEntry entries}
     */
    protected static Map<EntryEvent.Type, Set<BinaryEntry>> getEntryEventsMap(Set<BinaryEntry> setEntries, boolean fPreEvent)
        {
        Map<EntryEvent.Type, Set<BinaryEntry>> mapEntries = new HashMap<>(setEntries.size());

        for (BinaryEntry binEntry : setEntries)
            {
            if (binEntry != null)
                {
                EntryEvent.Type eventType =
                    binEntry.isPresent()
                        ? ((MapTrigger.Entry) binEntry).isOriginalPresent()
                            ? fPreEvent
                                ? EntryEvent.Type.UPDATING  : EntryEvent.Type.UPDATED
                            : fPreEvent
                                ? EntryEvent.Type.INSERTING : EntryEvent.Type.INSERTED
                            : fPreEvent
                                ? EntryEvent.Type.REMOVING : EntryEvent.Type.REMOVED;

                Set<BinaryEntry> setTypedEntries = mapEntries.get(eventType);
                if (setTypedEntries == null)
                    {
                    mapEntries.put(eventType, setTypedEntries = new HashSet<>());
                    }
                setTypedEntries.add(binEntry);
                }
            }

        return mapEntries;
        }
    }
