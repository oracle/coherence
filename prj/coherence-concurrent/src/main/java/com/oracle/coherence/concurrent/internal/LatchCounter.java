/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.internal;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A data structure that holds state related to countdown latch.
 *
 * @author lh  2021.11.16
 */
public class LatchCounter
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization interfaces
     */
    public LatchCounter()
        {
        }

    /**
     * Construct a {@link LatchCounter} instance.
     *
     * @param  count of the latch
     */
    public LatchCounter(int count)
        {
        m_lCount        = count;
        f_lInitialCount = count;
        }

    // ----- public api -----------------------------------------------------

    /**
     * Returns the current count of the latch.
     *
     * @return  the current count of the latch
     */
    public long getCount()
        {
        return m_lCount;
        }

    /**
     * Returns the initial count of the latch.
     *
     * @return  the initial count of the latch
     */
    public long getInitialCount()
        {
        return f_lInitialCount;
        }

    /**
     * Count down the latch count.
     */
    public void countDown()
        {
        if (m_lCount > 0)
            {
            m_lCount--;
            }
        }

    // ----- object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "LatchCounter{" +
                "count=" + m_lCount +
                ", initialCount=" + f_lInitialCount +
                '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_lCount        = in.readLong();
        f_lInitialCount = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeLong(m_lCount);
        out.writeLong(f_lInitialCount);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_lCount        = in.readLong(1);
        f_lInitialCount = in.readLong(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(1, m_lCount);
        out.writeLong(2, f_lInitialCount);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The current latch count.
     */
    protected long m_lCount;

    /**
     * The initial latch count.
     */
    protected long f_lInitialCount;
    }
