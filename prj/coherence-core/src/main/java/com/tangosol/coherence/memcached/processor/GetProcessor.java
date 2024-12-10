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
 * GetProcessor is an EntryProcessor that will fetch the value and the decorations (flag and version)
 * of the binary entry associated with the key.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class GetProcessor
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public GetProcessor()
        {
        }

    /**
     * Constructor
     *
     * @param fBinaryPassThru  binary pass-thru flag needed to deserialize the binary entry
     */
    public GetProcessor(boolean fBinaryPassThru)
        {
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

        BackingMapManagerContext mgrCtx = binaryEntry.getBackingMapContext().getManagerContext();
        return MemcachedHelper.convertToDataHolder(binValue, mgrCtx, m_fBinaryPassThru);
        }

    // ----- ExternalizableLite methods -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_fBinaryPassThru = in.readBoolean();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
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
        m_fBinaryPassThru = in.readBoolean(0);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeBoolean(0, m_fBinaryPassThru);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Flag to indicate if binary pass thru is enabled.
     */
    protected boolean m_fBinaryPassThru;
    }