/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.PrimitiveSparseArray;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

/**
 * Default implementation of {@link VersionedPartitions},
 *
 * @author hr  2021.02.17
 * @since 21.06
 */
public class DefaultVersionedPartitions
        implements VersionedPartitions, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default no-arg constructor.
     */
    public DefaultVersionedPartitions()
        {
        this((PrimitiveSparseArray) null);
        }

    /**
     * Copy-like constructor based on the provided {@link VersionedPartitions}.
     *
     * @param versions  the VersionedPartitions to copy / clone from
     */
    public DefaultVersionedPartitions(VersionedPartitions versions)
        {
        this((PrimitiveSparseArray) null);

        for (VersionedIterator iter = versions.iterator(); iter.hasNext(); )
            {
            long lVersion = iter.nextVersion();

            setPartitionVersion(iter.getPartition(), lVersion);
            }
        }

    /**
     * Create a DefaultVersionedPartitions instance based on the provided {@link
     * PrimitiveSparseArray}.
     *
     * @param laVersions  a PrimitiveSparseArray to base this VersionedPartitions
     *                    implementation on
     */
    protected DefaultVersionedPartitions(PrimitiveSparseArray laVersions)
        {
        m_laVersions = laVersions == null ? new PrimitiveSparseArray() : null;
        }

    // ----- VersionedPartitions interface ----------------------------------

    @Override
    public Iterator<Integer> getPartitions()
        {
        return new Iterator<Integer>()
            {
            @Override
            public int getPartition()
                {
                return (int) m_iter.getIndex();
                }

            @Override
            public int nextPartition()
                {
                m_iter.nextPrimitive();

                return (int) m_iter.getIndex();
                }

            @Override
            public boolean hasNext()
                {
                return m_iter.hasNext();
                }

            @Override
            public Integer next()
                {
                return m_iter.next().intValue();
                }

            /**
             * The iterator to base this iterator on.
             */
            final PrimitiveSparseArray.Iterator m_iter = m_laVersions.iterator();
            };
        }

    @Override
    public long getVersion(int iPartition)
        {
        PrimitiveSparseArray laVersions = m_laVersions;
        if (!laVersions.exists(iPartition))
            {
            return VersionAwareMapListener.HEAD;
            }

        return laVersions.getPrimitive(iPartition);
        }

    @Override
    public VersionedIterator iterator()
        {
        return new VersionedIterator()
            {
            @Override
            public int getPartition()
                {
                return (int) f_iter.getIndex();
                }

            @Override
            public int nextPartition()
                {
                f_iter.nextPrimitive();

                return (int) f_iter.getIndex();
                }

            @Override
            public long getVersion()
                {
                return f_iter.getPrimitiveValue();
                }

            @Override
            public long nextVersion()
                {
                return f_iter.nextPrimitive();
                }

            @Override
            public boolean hasNext()
                {
                return f_iter.hasNext();
                }

            @Override
            public Long next()
                {
                return f_iter.next();
                }

            /**
             * The iterator to base this iterator on.
             */
            final PrimitiveSparseArray.Iterator f_iter = m_laVersions.iterator();
            };
        }

    // ----- public api methods ---------------------------------------------

    /**
     * Set the partition version.
     *
     * @param iPart     the partition
     * @param lVersion  the version
     */
    public DefaultVersionedPartitions setPartitionVersion(int iPart, long lVersion)
        {
        m_laVersions.setPrimitive(iPart, lVersion);

        return this;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        return o != null && o.getClass() == getClass() &&
                Base.equals(m_laVersions, ((DefaultVersionedPartitions) o).m_laVersions);
        }

    @Override
    public int hashCode()
        {
        int nHash = m_laVersions.hashCode();

        return (nHash << 4) | ((nHash >> 28) & 0xF);
        }

    @Override
    public String toString()
        {
        StringBuffer sb      = new StringBuffer(getClass().getSimpleName()).append("[");
        String       sPrefix = "";
        for (VersionedIterator iter = iterator(); iter.hasNext(); )
            {
            long lVersion = iter.nextVersion();

            sb.append(sPrefix).append(iter.getPartition()).append("->").append(lVersion);

            sPrefix = ",";
            }
        return sb.append("]").toString();
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        PrimitiveSparseArray laVersions = m_laVersions = new PrimitiveSparseArray();

        for (int i = ExternalizableHelper.readInt(in); i > 0; --i)
            {
            laVersions.setPrimitive(ExternalizableHelper.readInt(in), ExternalizableHelper.readLong(in));
            }
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        PrimitiveSparseArray laVersions = m_laVersions;

        int cSize = laVersions.getSize();
        ExternalizableHelper.writeInt(out, cSize);

        for (PrimitiveSparseArray.Iterator iter = laVersions.iterator(); iter.hasNext(); )
            {
            long lVersion = iter.nextPrimitive();

            ExternalizableHelper.writeInt(out, (int) iter.getIndex());
            ExternalizableHelper.writeLong(out, lVersion);
            }
        }
    
    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_laVersions = (PrimitiveSparseArray) in.readLongArray(0, new PrimitiveSparseArray());
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLongArray(0, m_laVersions);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A data structure for holding partition -> version mapping.
     */
    protected PrimitiveSparseArray m_laVersions;
    }
