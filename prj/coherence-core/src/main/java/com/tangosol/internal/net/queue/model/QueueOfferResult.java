/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.model;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import javax.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The result of offering an element to a queue.
 */
public class QueueOfferResult
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public QueueOfferResult()
        {
        }

    /**
     * Create a result.
     *
     * @param idMSB    the least significant bits of the long id value
     * @param idLSB    the most significant bits of the long id value
     * @param nResult  the result value
     */
    public QueueOfferResult(int idMSB, int idLSB, int nResult)
        {
        this((((long) idMSB) << 32) | (idLSB & 0xffffffffL), nResult);
        }

    /**
     * Create a result.
     *
     * @param id       the result identifier
     * @param nResult  the result value
     */
    public QueueOfferResult(long id, int nResult)
        {
        m_nId     = id;
        m_nResult = nResult;
        }

    /**
     * Get the result identifier.
     *
     * @return  the result identifier
     */
    public long getId()
        {
        return m_nId;
        }

    /**
     * Get the actual result of the offer.
     *
     * @return the actual result of the offer
     */
    public int getResult()
        {
        return m_nResult;
        }

    // ----- EvolvablePortableObject methods --------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nId     = in.readLong(0);
        m_nResult = in.readInt(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_nId);
        out.writeInt(1, m_nResult);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nId     = in.readLong();
        m_nResult = in.readInt();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(m_nId);
        out.writeInt(m_nResult);
        }

    @Override
    public String toString()
        {
        return "QueueOfferResult{" +
                "id=" + m_nId +
                ", result=" + m_nResult +
                '}';
        }

    // ----- constants ------------------------------------------------------

    public static final int RESULT_SUCCESS = 1;

    public static final int RESULT_FAILED_CAPACITY = 2;

    public static final int RESULT_FAILED_RETRY = 3;

    // ----- data members ---------------------------------------------------

    public static final int IMPL_VERSION = 1;

    @JsonbProperty("id")
    private long m_nId;

    @JsonbProperty("result")
    private int m_nResult;
    }
