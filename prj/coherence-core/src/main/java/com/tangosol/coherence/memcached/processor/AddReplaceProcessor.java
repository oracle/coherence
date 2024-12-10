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

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * AddReplaceProcessor is an EntryProcessor that will conditionally add or replace
 * the cache entry based on the memcached protocol rules.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class AddReplaceProcessor
        extends PutProcessor
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public AddReplaceProcessor()
        {
        }

    /**
     * Constructor.
     *
     * @param abValue         byte[] value to store
     * @param nFlag           flag sent in the memcached request
     * @param lVersion        data version sent in the memcached request
     * @param nExpiry         expiry for the entry
     * @param fAllowInsert    flag to indicate if it is an add or replace operation
     * @param fBinaryPassThru binary pass-thru flag needed to deserialize the binary entry
     */
    public AddReplaceProcessor(byte[] abValue, int nFlag, long lVersion, int nExpiry, boolean fAllowInsert,
            boolean fBinaryPassThru)
        {
        super(abValue, nFlag, lVersion, nExpiry, fBinaryPassThru);
        m_fAllowInsert = fAllowInsert;
        }

    // ----- EntryProcessor methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(Entry entry)
        {
        // Memcached binary protocol constraints:
        //   * Add MUST fail if the item already exist
        //   * Replace MUST fail if the item doesn't exist
        BinaryEntry binaryEntry = MemcachedHelper.getBinaryEntry(entry);
        Binary      binValue    = binaryEntry.getBinaryValue();

        if (!m_fAllowInsert && binValue == null)
            {
            return ResponseCode.KEYNF;
            }
        if (m_fAllowInsert && binValue != null)
            {
            return ResponseCode.KEYEXISTS;
            }

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
        m_fAllowInsert = in.readBoolean();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);
        out.writeBoolean(m_fAllowInsert);
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
        m_fAllowInsert = in.readBoolean(10);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);
        out.writeBoolean(10, m_fAllowInsert);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Flag indicating if its an add or replace operation.
     */
    protected boolean m_fAllowInsert;
    }