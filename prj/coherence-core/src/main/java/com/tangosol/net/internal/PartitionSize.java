/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.cache.BinaryMemoryCalculator;

import com.tangosol.util.BinaryEntry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Internal class used by {@link PartitionSizeAggregator} to calculate partition size across all partitions for a cache.
 *
 * @author as 2024.01.28
 * @since 24.03
 */
public class PartitionSize
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    public PartitionSize()
        {
        }

    // ----- accessors ------------------------------------------------------

    public PartitionSize(int nPartitionId)
        {
        this.m_nPartitionId = nPartitionId;
        }

    public int getPartitionId()
        {
        return m_nPartitionId;
        }

    public int getCount()
        {
        return m_cCount;
        }

    public long getTotalSize()
        {
        return m_nTotalSize;
        }

    public long getMaxEntrySize()
        {
        return m_nMaxEntrySize;
        }

    public int getMemberId()
       {
        return m_nMemberId;
       }

    public void accumulate(BinaryEntry<?, ?> entry)
        {
        long nEntrySize = BinaryMemoryCalculator.INSTANCE.calculateUnits(entry.getBinaryKey(), entry.getBinaryValue());
        
        m_cCount++;
        m_nTotalSize += nEntrySize;

        if (nEntrySize > m_nMaxEntrySize)
            {
            m_nMaxEntrySize = nEntrySize;
            }

        if (m_nMemberId == 0)
            {
            // only set member if it's not already set
            m_nMemberId =  entry.getContext().getCacheService().getCluster().getLocalMember().getId();
            }
        }

    public void combine(PartitionSize other)
        {
        this.m_cCount       += other.m_cCount;
        this.m_nTotalSize   += other.m_nTotalSize;
        this.m_nMemberId     = other.m_nMemberId;
        this.m_nMaxEntrySize = Math.max(other.m_nMaxEntrySize, this.m_nMaxEntrySize);
        }

    // ---- Object interface ------------------------------------------------

    public String toString()
        {
        return String.format("#%,d, count=%,d, totalSize=%,d, maxEntrySize=%,d, member=%d", m_nPartitionId, m_cCount, m_nTotalSize,
                m_nMaxEntrySize, m_nMemberId);
        }

    // ---- ExternalizableLite interface ------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        m_nPartitionId  = in.readInt();
        m_cCount        = in.readInt();
        m_nTotalSize    = in.readLong();
        m_nMaxEntrySize = in.readLong();
        m_nMemberId     = in.readInt();
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(m_nPartitionId);
        out.writeInt(m_cCount);
        out.writeLong(m_nTotalSize);
        out.writeLong(m_nMaxEntrySize);
        out.writeInt(m_nMemberId);
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        m_nPartitionId  = in.readInt(0);
        m_cCount        = in.readInt(1);
        m_nTotalSize    = in.readLong(2);
        m_nMaxEntrySize = in.readLong(3);
        m_nMemberId     = in.readInt(4);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nPartitionId);
        out.writeInt(1, m_cCount);
        out.writeLong(2, m_nTotalSize);
        out.writeLong(3, m_nMaxEntrySize);
        out.writeInt(4, m_nMemberId);
        }

    // ----- data members ---------------------------------------------------

    /**
     * PartitionId for the result.
     */
    private int m_nPartitionId;

    /**
     * Count of number if entries owned.
     */
    private int m_cCount;

    /**
     * Total size of all keys and values.
     */
    private long m_nTotalSize;

    /**
     * Max single entry size out of all keys and values.
     */
    private long m_nMaxEntrySize;

    /**
     * The member owning this partition.
     */
    private int m_nMemberId;
    }