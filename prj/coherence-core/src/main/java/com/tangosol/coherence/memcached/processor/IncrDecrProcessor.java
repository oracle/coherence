/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.processor;

import com.tangosol.coherence.memcached.Response;
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
import com.tangosol.util.InvocableMap.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * IncrDecrProcessor is an EntryProcessor that will increment or decrement the
 * stringified value stored in the cache.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class IncrDecrProcessor
        extends PutProcessor
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public IncrDecrProcessor()
        {
        }

    /**
     * Constructor.
     *
     * @param lInitial         Initial seed value
     * @param lIncr            Increment or decrement value
     * @param fIncr            flag to indicate if its a increment or decrement operation
     * @param lVersion         data version sent in the memcached request
     * @param nExpiry          expiry for the entry
     * @param fBinaryPassThru  binary pass-thru flag needed to deserialize the binary entry
     */
    public IncrDecrProcessor(long lInitial, long lIncr, boolean fIncr, long lVersion, int nExpiry,
            boolean fBinaryPassThru)
        {
        super(null, 0, lVersion, nExpiry, fBinaryPassThru);
        m_lInitial = lInitial;
        m_lIncr    = lIncr;
        m_fIncr    = fIncr;
        }

    // ----- EntryProcessor methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(Entry entry)
        {
        // Memcached binary protocol constraints:
        // Add or remove the specified amount to the requested counter. To set
        // the value of the counter with add/set/replace, the object's data
        // must be the ASCII representation of the value and not the byte values
        // of a 64 bit integer.
        //
        // If the counter does not exist, one of two things may happen:
        // 1. If the expiration value is all one-bits (0xffffffff), the
        //    operation will fail with NOT_FOUND.
        // 2. For all other expiration values, the operation will succeed by seeding
        //    the value for this key with the provided initial value to expire with
        //    the provided expiration time and the flags will be set to zero.
        //    Decrementing a counter will never result in a "negative value" (or
        //    cause the counter to "wrap"); instead the counter is set to 0.
        //    Incrementing the counter may cause the counter to wrap.

        BinaryEntry              binaryEntry = MemcachedHelper.getBinaryEntry(entry);
        Binary                   binValue    = binaryEntry.getBinaryValue();
        BackingMapManagerContext mgrCtx      = binaryEntry.getBackingMapContext().getManagerContext();
        try
            {
            if (binValue == null)
                {
                if (m_nExpiry == 0xffffffff)
                    {
                    return ResponseCode.KEYNF;
                    }
                m_abValue = String.valueOf(m_lInitial).getBytes("utf-8");
                }
            else
                {
                DataHolder holder = MemcachedHelper.convertToDataHolder(binValue, mgrCtx, m_fBinaryPassThru);
                long       lValue = getLong(holder.getValue());

                m_nFlag   = holder.getFlag();
                m_abValue = String.valueOf(calculateNewValue(lValue)).getBytes();
                }

            Object oReturn = super.process(entry);
            return oReturn instanceof Response.ResponseCode
                ? oReturn
                : MemcachedHelper.convertToDataHolder(binaryEntry.getBinaryValue(), mgrCtx, m_fBinaryPassThru);
            }
        catch (Exception ex)
            {
            return ResponseCode.NAN;
            }
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
        m_lInitial = in.readLong();
        m_lIncr    = in.readLong();
        m_fIncr    = in.readBoolean();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);
        out.writeLong(m_lInitial);
        out.writeLong(m_lIncr);
        out.writeBoolean(m_fIncr);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);
        m_lInitial = in.readLong(10);
        m_lIncr    = in.readLong(11);
        m_fIncr    = in.readBoolean(12);
        }

    // ----- PortableObject methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);
        out.writeLong(10, m_lInitial);
        out.writeLong(11, m_lIncr);
        out.writeBoolean(12, m_fIncr);
        }

    /**
     * Calculate the updated value based on the memcached protocol rules.
     *
     * @param lValueOld  old value
     *
     * @return computed new value
     */
    protected long calculateNewValue(long lValueOld)
        {
        // decrementing cannot result in -ve value.
        return (m_fIncr)
                ? lValueOld + m_lIncr
                : Math.max(0, lValueOld - m_lIncr);
        }

    // ----- static helpers -------------------------------------------------

    /**
     * Get the long value from its stringified form.
     *
     * @param abValue  byte[] of the stringified value
     *
     * @return the long value
     */
    public static Long getLong(byte[] abValue)
        {
        return Long.valueOf(MemcachedHelper.getString(abValue));
        }

    // ----- data members ---------------------------------------------------

    /**
     * Initial seed value.
     */
    protected long m_lInitial;

    /**
     * Increment/decrement value.
     */
    protected long m_lIncr;

    /**
     * Flag to indicate if this is an increment/decrement operation.
     */
    protected boolean m_fIncr;
    }