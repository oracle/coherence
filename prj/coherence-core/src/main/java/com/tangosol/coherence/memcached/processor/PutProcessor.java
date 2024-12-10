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
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * PutProcessor is an EntryProcessor that will store the value and the decorations
 * (flag and version) in the binary entry.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class PutProcessor
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public PutProcessor()
        {
        }

    /**
     * Constructor.
     *
     * @param abValue          byte[] value
     * @param nFlag            flag sent in the memcached request
     * @param lVersion         data version sent in the memcached request
     * @param nExpiry          expiry for the entry
     * @param fBinaryPassThru  binary pass-thru flag needed to deserialize the binary entry
     */
    public PutProcessor(byte[] abValue, int nFlag, long lVersion, int nExpiry, boolean fBinaryPassThru)
        {
        m_abValue         = abValue;
        m_nFlag           = nFlag;
        m_lVersion        = lVersion;
        m_nExpiry         = nExpiry;
        m_fBinaryPassThru = fBinaryPassThru;
        }

    // ----- EntryProcessor methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(Entry entry)
        {
        /*
         * Memcached binary protocol constraints:
         * If the Data Version Check (CAS) is nonzero, the requested operation
         * MUST only succeed if the item exists and has a CAS value identical to the provided value.
         */
        BinaryEntry              binaryEntry = MemcachedHelper.getBinaryEntry(entry);
        Binary                   binValue    = binaryEntry.getBinaryValue();
        BackingMapManagerContext ctxMgr      = binaryEntry.getBackingMapContext().getManagerContext();
        long                     lVersion    = m_lVersion;
        DataHolder               oHolder     = (binValue != null)
                                                ? MemcachedHelper.convertToDataHolder(binValue, ctxMgr, m_fBinaryPassThru)
                                                : null;
        if (lVersion != 0)
            {
            if (oHolder == null)
                {
                return ResponseCode.KEYNF;
                }
            else
                {
                if (oHolder.getVersion() != lVersion)
                    {
                    return ResponseCode.KEYEXISTS;
                    }
                }
            }
        lVersion = (oHolder != null) ? oHolder.getVersion() + 1 : ++lVersion;
        binaryEntry.updateBinaryValue(getDecoratedBinary(ctxMgr, lVersion));
        if (m_nExpiry > 0)
            {
            binaryEntry.expire(m_nExpiry);
            }
        return lVersion;
        }

    // ----- ExternalizableLite methods -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_abValue         = ExternalizableHelper.readByteArray(in);
        m_nFlag           = in.readInt();
        m_lVersion        = in.readLong();
        m_nExpiry         = in.readInt();
        m_fBinaryPassThru = in.readBoolean();
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
        out.writeInt(m_nExpiry);
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
        m_abValue         = in.readByteArray(0);
        m_nFlag           = in.readInt(1);
        m_lVersion        = in.readLong(2);
        m_nExpiry         = in.readInt(3);
        m_fBinaryPassThru = in.readBoolean(4);
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
        out.writeInt(3, m_nExpiry);
        out.writeBoolean(4, m_fBinaryPassThru);
        }

    // ----- internal helper methods ----------------------------------------

    /**
     * Get the decorated binary.
     *
     * @param mgrCtx    Backing map manager context
     * @param lVersion  Entry version
     *
     * @return decorated binary
     */
    protected Binary getDecoratedBinary(BackingMapManagerContext mgrCtx, long lVersion)
        {
        Binary bin = m_fBinaryPassThru
                       ? new Binary(m_abValue)
                       : (Binary) mgrCtx.getValueToInternalConverter().convert(m_abValue);
        WriteBuffer bufDeco = new BinaryWriteBuffer(12, 12);
        try
            {
            BufferOutput out = bufDeco.getBufferOutput();
            out.writeInt(m_nFlag);
            out.writeLong(lVersion);

            return (Binary) mgrCtx.addInternalValueDecoration(
                bin, ExternalizableHelper.DECO_MEMCACHED, bufDeco.toBinary());
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The byte[] value.
     */
    protected byte[] m_abValue;

    /**
     * Flag sent in the memcached request.
     */
    protected int m_nFlag;

    /**
     * Data version sent in the memcached request.
     */
    protected long m_lVersion;

    /**
     * Entry expiry time.
     */
    protected int m_nExpiry;

    /**
     * Flag to indicate if binary pass thru is enabled.
     */
    protected boolean m_fBinaryPassThru;
    }