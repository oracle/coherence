/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.events;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.util.BinaryEntry;

/**
 * An event that is raised for each entry from the server-side {@link EntryEvent},
 * allowing us to simplify event handling.
 *
 * @author Aleks Seovic  2020.06.08
 * @since Coherence 14.1.2
 */
public class SingleEntryEvent<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code SingleEntryEvent} instance.
     *
     * @param type   the event type
     * @param entry  the entry that caused the event to fire
     */
    public SingleEntryEvent(EntryEvent.Type type, BinaryEntry<K, V> entry)
        {
        m_type = type;
        m_entry = entry;
        }

    // ---- API -------------------------------------------------------------

    /**
     * Return the {@link Event}'s type.
     *
     * @return the Event's type
     */
    public EntryEvent.Type getType()
        {
        return m_type;
        }

    /**
     * Return the entry that caused the event to fire.
     *
     * @return the entry that caused the event to fire
     */
    public BinaryEntry<K, V> getEntry()
        {
        return m_entry;
        }

    /**
     * Return the key corresponding to this entry.
     *
     * @return the key corresponding to this entry; may be null if the
     * underlying Map supports null keys
     */
    public K getKey()
        {
        return m_entry.getKey();
        }

    /**
     * Return the value corresponding to this entry.
     *
     * @return the value corresponding to this entry; may be null if the
     * value is null or if the Entry does not exist in the Map
     */
    public V getValue()
        {
        return m_entry.getValue();
        }

    /**
     * Return an original value for this entry.
     *
     * @return an original value for this entry
     */
    public V getOldValue()
        {
        return m_entry.getOriginalValue();
        }

    /**
     * Store the value corresponding to this entry.
     *
     * @param value the new value for this Entry
     *
     * @return the previous value of this Entry, or null if the Entry did
     * not exist
     */
    public V setValue(V value)
        {
        return m_entry.setValue(value);
        }

    // --- data members -----------------------------------------------------

    /**
     * The Event's type.
     */
    private final EntryEvent.Type m_type;

    /**
     * The entry that caused the event to fire.
     */
    private final BinaryEntry<K, V> m_entry;
    }
