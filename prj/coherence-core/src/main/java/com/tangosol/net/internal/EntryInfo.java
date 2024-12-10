/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.net.events.partition.cache.EntryEvent.Type;
import com.tangosol.util.BinaryEntry;

/**
 * EntryInfo is a simple representation of an entry associated
 * with corresponding event.
 *
 * @author bbc 2018.11.06
 * @version 12.2.1.3
 */
public class EntryInfo
    {
    /**
     * Construct an EntryInfo.
     */
    public EntryInfo()
        {
        }

    /**
     * Create a new EntryInfo with the given event type and binary entry.
     *
     * @param evenType   the event type
     * @param binEntry   the binaryEntry associated to the event
     *
     */
    public EntryInfo(Type evenType, BinaryEntry binEntry)
        {
        m_eventType  = evenType;
        m_binEntry   = binEntry;
        }

    // ----- accessors -------------------------------------------------------

    /**
     * Return the event type.
     *
     * @return the event type
     */
    public Type getEventType()
        {
        return m_eventType;
        }

    /**
     * Return the binary entry.
     *
     * @return the binary entry
     */
    public BinaryEntry getBinaryEntry()
        {
        return m_binEntry;
        }


    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
        {
        if (o instanceof EntryInfo)
            {
            EntryInfo that = (EntryInfo) o;
            return this == that
                    || this.getEventType().equals(that.getEventType()) &&
                    (m_binEntry != null && m_binEntry.equals(that.m_binEntry) ||
                               m_binEntry == null && that.m_binEntry == null);
            }

        return false;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        return (m_binEntry == null ? 0 : m_binEntry.hashCode()) ^
               (m_eventType == null ? 0 : m_eventType.hashCode());
        }


    // ----- data members ---------------------------------------------------

    /**
     * The event type.
     */
    private Type m_eventType;

    /**
     * The binary entry.
     */
    private BinaryEntry m_binEntry;
    }
