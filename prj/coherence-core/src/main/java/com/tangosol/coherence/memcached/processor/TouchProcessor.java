/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.processor;

import com.tangosol.coherence.memcached.Response.ResponseCode;

import com.tangosol.coherence.memcached.server.MemcachedHelper;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap.Entry;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * TouchProcessor is an EntryProcessor that will update the expiry time of the entry and
 * optionally return the stored entry.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class TouchProcessor
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public TouchProcessor()
        {
        }

    /**
     * Constructor.
     *
     * @param nExpiry          expiry for the entry
     * @param fBlind           flag to indicate if returned value is not required.
     * @param fBinaryPassThru  binary pass-thru flag needed to deserialize the binary entry
     */
    public TouchProcessor(int nExpiry, boolean fBlind, boolean fBinaryPassThru)
        {
        m_nExpiry         = nExpiry;
        m_fBlind          = fBlind;
        m_fBinaryPassThru = fBinaryPassThru;
        }

    // ----- EntryProcessor methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(Entry entry)
        {
        BinaryEntry binaryEntry = MemcachedHelper.getBinaryEntry(entry);
        Binary      binValue    = binaryEntry.getBinaryValue();
        if (binValue == null)
            {
            return ResponseCode.KEYNF;
            }
        if (m_nExpiry >= 0)
            {
            binaryEntry.expire(m_nExpiry);
            }

        BackingMapManagerContext mgrCtx = binaryEntry.getBackingMapContext().getManagerContext();
        return m_fBlind
               ? null
               : MemcachedHelper.convertToDataHolder(binValue, mgrCtx, m_fBinaryPassThru);
        }

    // ----- ExternalizableLite methods -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_nExpiry         = in.readInt();
        m_fBlind          = in.readBoolean();
        m_fBinaryPassThru = in.readBoolean();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeInt(m_nExpiry);
        out.writeBoolean(m_fBlind);
        out.writeBoolean(m_fBinaryPassThru);
        }

    // ----- PortableObject methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nExpiry         = in.readInt(0);
        m_fBlind          = in.readBoolean(1);
        m_fBinaryPassThru = in.readBoolean(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nExpiry);
        out.writeBoolean(1, m_fBlind);
        out.writeBoolean(2, m_fBinaryPassThru);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Expiry time
     */
    protected int m_nExpiry;

    /**
     * Flag to indicate if entry needs to be returned or not.
     */
    protected boolean m_fBlind;

    /**
     * Flag to indicate if binary pass thru is enabled.
     */
    protected boolean m_fBinaryPassThru;
    }