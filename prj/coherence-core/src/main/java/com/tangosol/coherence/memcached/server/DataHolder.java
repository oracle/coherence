/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * DataHolder is a holder for the value and memcached decorations - flag and version.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class DataHolder
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default Constructor
     */
    public DataHolder()
        {
        }

    /**
     * Construct a DataHolder.
     *
     * @param abValue   the value
     * @param nFlag     the flag
     * @param lVersion  the version
     */
    public DataHolder(byte[] abValue, int nFlag, long lVersion)
        {
        m_abValue  = abValue;
        m_nFlag    = nFlag;
        m_lVersion = lVersion;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the value
     *
     * @return value
     */
    public byte[] getValue()
        {
        return m_abValue;
        }

    /**
     * Return the flag.
     *
     * @return flag
     */
    public int getFlag()
        {
        return m_nFlag;
        }

    /**
     * Return the version.
     *
     * @return version
     */
    public long getVersion()
        {
        return m_lVersion;
        }

    // ----- ExternalizableLite methods -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_abValue  = ExternalizableHelper.readByteArray(in);
        m_nFlag    = in.readInt();
        m_lVersion = in.readLong();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        if (m_abValue == null)
            {
            out.writeInt(0);
            }
        else
            {
            out.writeInt(m_abValue.length);
            out.write(m_abValue);
            }

        out.writeInt(m_nFlag);
        out.writeLong(m_lVersion);
        }

    // ----- PortableObject methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_abValue  = in.readByteArray(0);
        m_nFlag    = in.readInt(1);
        m_lVersion = in.readLong(2);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeByteArray(0, m_abValue);
        out.writeInt(1, m_nFlag);
        out.writeLong(2, m_lVersion);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The value.
     */
    protected byte[] m_abValue;

    /**
     * The flag.
     */
    protected int m_nFlag;

    /**
     * The version.
     */
    protected long m_lVersion;
    }