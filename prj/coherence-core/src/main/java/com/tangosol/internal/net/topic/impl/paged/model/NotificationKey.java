/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.partition.KeyPartitioningStrategy;

import java.io.IOException;

/**
 * NotificationKeys serve as a means by which publishers and subscribers can be notified when
 * the topic exits a full or empty state.
 *
 * @author mf  2016.04.12
 * @since Coherence 14.1.1
 */
// implementation note: NotificationKey does not implement Evolvable
// because adding fields would affect the "equality"
// of a key
public class NotificationKey
    implements KeyPartitioningStrategy.PartitionAwareKey, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public NotificationKey()
        {
        }

    /**
     * Create a {@link NotificationKey}.
     *
     * @param nPartition  the partition id
     * @param nId         the notification id
     */
    public NotificationKey(int nPartition, int nId)
        {
        m_nPartition = nPartition;
        m_nId        = nId;
        }

    // ----- accessors ------------------------------------------------------

    @Override
    public int getPartitionId()
        {
        return m_nPartition;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object oThat)
        {
        if (oThat instanceof NotificationKey)
            {
            NotificationKey that = (NotificationKey) oThat;
            return that.m_nPartition == m_nPartition && that.m_nId == m_nId;
            }

        return false;
        }

    @Override
    public int hashCode()
        {
        return m_nId;
        }

    @Override
    public String toString()
        {
        return m_nPartition + "/" + m_nId;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nPartition = in.readInt(0);
        m_nId        = in.readInt(1);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nPartition);
        out.writeInt(1, m_nId);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The partition.
     */
    protected int m_nPartition;

    /**
     * Notification id.
     */
    protected int m_nId;
    }
