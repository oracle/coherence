/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The version information for a queue.
 */
public class QueueVersionInfo
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for POF.
     */
    public QueueVersionInfo()
        {
        }

    /**
     * Create a new QueueVersionInfo with the
     * specified version details.
     *
     * @param tailOfferVersion  the tail offer version
     * @param headPollVersion   the head poll version
     */
    public QueueVersionInfo(long tailOfferVersion, long headPollVersion)
        {
        m_tailOfferVersion = tailOfferVersion;
        m_headPollVersion  = headPollVersion;
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Obtain the current tail offer version.
     *
     * @return the current tail offer version
     */
    public long getTailOfferVersion()
        {
        return m_tailOfferVersion;
        }


    public void setTailOfferVersion(long tailOfferVersion)
        {
        m_tailOfferVersion = tailOfferVersion;
        }

    /**
     * Increment the current tail offer version
     */
    public void incrementTailOfferVersion()
        {
        m_tailOfferVersion++;
        }

    /**
     * Obtain the current head poll version.
     *
     * @return the current head poll version
     */
    public long getHeadPollVersion()
        {
        return m_headPollVersion;
        }

    public void setHeadPollVersion(long headPollVersion)
        {
        m_headPollVersion = headPollVersion;
        }

    /**
     * Increment the current head poll version
     */
    public void incrementHeadPollVersion()
        {
        m_headPollVersion++;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        QueueVersionInfo that = (QueueVersionInfo) o;

        if (m_headPollVersion != that.m_headPollVersion)
            {
            return false;
            }
        if (m_tailOfferVersion != that.m_tailOfferVersion)
            {
            return false;
            }

        return true;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        int result = Long.hashCode(m_headPollVersion);
        result = 31 * result + Long.hashCode(m_tailOfferVersion);
        return result;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "QueueVersionInfo(" +
                "headPollVersion=" + m_headPollVersion +
                ", tailOfferVersion=" + m_tailOfferVersion +
                ')';
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        PofReader reader = in.createNestedPofReader(0);
        m_headPollVersion  = reader.readLong(0);
        m_tailOfferVersion = reader.readLong(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        PofWriter writer = out.createNestedPofWriter(0);
        writer.writeLong(0, m_headPollVersion);
        writer.writeLong(1, m_tailOfferVersion);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_headPollVersion  = ExternalizableHelper.readLong(in);
        m_tailOfferVersion = ExternalizableHelper.readLong(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeLong(out, m_headPollVersion);
        ExternalizableHelper.writeLong(out, m_tailOfferVersion);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Bucket's head poll version number.
     */
    protected long m_headPollVersion = 0;

    /**
     * The Bucket's tail offer version number.
     */
    protected long m_tailOfferVersion = 0;
    }
