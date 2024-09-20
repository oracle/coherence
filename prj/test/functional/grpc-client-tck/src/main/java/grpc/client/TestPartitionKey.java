/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class TestPartitionKey
        implements KeyPartitioningStrategy.PartitionAwareKey, PortableObject, ExternalizableLite
    {

    public TestPartitionKey()
        {
        }

    public TestPartitionKey(String sKey, int nPartition)
        {
        m_sKey       = sKey;
        m_nPartition = nPartition;
        }

    public String getKey()
        {
        return m_sKey;
        }

    @Override
    public int getPartitionId()
        {
        return m_nPartition;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sKey       = ExternalizableHelper.readSafeUTF(in);
        m_nPartition = in.readInt();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sKey);
        out.writeInt(m_nPartition);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sKey       = in.readString(0);
        m_nPartition = in.readInt(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sKey);
        out.writeInt(1, m_nPartition);
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestPartitionKey that = (TestPartitionKey) o;
        return m_nPartition == that.m_nPartition && Objects.equals(m_sKey, that.m_sKey);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sKey, m_nPartition);
        }

    @Override
    public String toString()
        {
        return "TestAssociatedKey{" +
                "key='" + m_sKey + '\'' +
                ", partition='" + m_nPartition + '\'' +
                '}';
        }

    // ----- data members ---------------------------------------------------

    private String m_sKey;

    private int m_nPartition;
    }
