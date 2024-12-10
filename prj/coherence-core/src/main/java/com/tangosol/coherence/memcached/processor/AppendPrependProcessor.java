/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.processor;

import com.tangosol.coherence.memcached.Response.ResponseCode;

import com.tangosol.coherence.memcached.server.DataHolder;
import com.tangosol.coherence.memcached.server.MemcachedHelper;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.InvocableMap.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.nio.ByteBuffer;

/**
 * AppendPrependProcessor is an EntryProcessor that will append or prepend the
 * passed in value to the existing value in the cache.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class AppendPrependProcessor
        extends PutProcessor
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public AppendPrependProcessor()
        {
        }

    /**
     * Constructor.
     *
     * @param obDelta         delta value to be prepended or appended.
     * @param lVersion        data version sent in the memcached request
     * @param fAppend         flag to indicate if the delta has to be appended or prepended.
     * @param fBinaryPassThru binary pass-thru flag needed to deserialize the binary entry
     */
    public AppendPrependProcessor(byte[] obDelta, long lVersion, boolean fAppend, boolean fBinaryPassThru)
        {
        super(null, 0, lVersion, 0, fBinaryPassThru);
        m_abDelta = obDelta;
        m_fAppend = fAppend;
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

        BackingMapManagerContext mgrCtx     = binaryEntry.getBackingMapContext().getManagerContext();
        DataHolder               holder     = MemcachedHelper.convertToDataHolder(binValue, mgrCtx, m_fBinaryPassThru);
        byte[]                   abValueOld = holder.getValue();
        ByteBuffer               buf        = ByteBuffer.allocate(abValueOld.length + m_abDelta.length);

        if (m_fAppend)
            {
            buf.put(abValueOld);
            buf.put(m_abDelta);
            }
        else
            {
            buf.put(m_abDelta);
            buf.put(abValueOld);
            }
        m_abValue = buf.array();
        m_nFlag   = holder.getFlag();

        return super.process(entry);
        }

    // ----- ExternalizableLite methods -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_abDelta = ExternalizableHelper.readByteArray(in);
        m_fAppend = in.readBoolean();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);
        if (m_abDelta == null)
            {
            out.writeInt(0);
            }
        else
            {
            out.writeInt(m_abDelta.length);
            out.write(m_abDelta);
            }
        out.writeBoolean(m_fAppend);
        }

    // ----- PortableObject methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);
        m_abDelta = in.readByteArray(10);
        m_fAppend = in.readBoolean(11);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);
        out.writeByteArray(10, m_abDelta);
        out.writeBoolean(11, m_fAppend);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Delta value to be appended or prepended.
     */
    protected byte[] m_abDelta;

    /**
     * Flag to indicate if its an append or prepend operation.
     */
    protected boolean m_fAppend;
    }