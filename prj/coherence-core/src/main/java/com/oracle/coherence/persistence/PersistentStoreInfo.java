/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * PersistentStoreInfo is a simple representation of a persistent directory.
 *
 * @since 24.09
 * @author mg 2024.14.18
 */
public class PersistentStoreInfo
        implements Comparable, PortableObject, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an PersistentStoreInfo.
     */
    public PersistentStoreInfo()
        {
        }

    /**
     * Create a new PersistentStoreInfo with the given store identifier.
     *
     * @param sId     the identifiers of the PersistentStore
     * @param fEmpty  true if the store is empty
     */
    public PersistentStoreInfo(String sId, boolean fEmpty)
        {
        m_sId    = sId;
        m_fEmpty = fEmpty;
        }

    // ----- accessors -------------------------------------------------------

    /**
     * Return the name of the PersistentStore identifier.
     *
     * @return the name of the PersistentStore identifier
     */
    public String getId()
        {
        return m_sId;
        }

    /**
     * Return true if the store is empty.
     *
     * @return true if the store is empty
     */
    public boolean isEmpty()
        {
        return m_fEmpty;
        }


    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sId    = in.readUTF();
        m_fEmpty = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeUTF(m_sId);
        out.writeBoolean(m_fEmpty);
        }

    // ----- PofSerializer interface -----------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sId    = in.readString(0);
        m_fEmpty = in.readBoolean(1);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sId);
        out.writeBoolean(1, m_fEmpty);
        }

    // ----- Comparable interface -------------------------------------------

    @Override
    public int compareTo(Object o)
        {
        PersistentStoreInfo info    = (PersistentStoreInfo) o;
        int                 nResult = getId().compareTo(info.getId());
        if (nResult == 0)
            {
            nResult = -(Boolean.valueOf(isEmpty()).compareTo(Boolean.valueOf(info.isEmpty())));
            }
        return nResult;
        }

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        return "PersistenceStoreInfo [id=" + m_sId + ", isEmpty=" + m_fEmpty + "]";
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the PersistentStore identifier.
     */
    private String m_sId;

    /**
     * The flag indicating if the store is empty.
     */
    private boolean m_fEmpty;
    }
