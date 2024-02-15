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
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class TestAssociatedKey
        implements KeyAssociation<String>, PortableObject, ExternalizableLite
    {
    public TestAssociatedKey()
        {
        }

    public TestAssociatedKey(String sKey, String sAssociated)
        {
        m_sKey        = sKey;
        m_sAssociated = sAssociated;
        }

    public String getKey()
        {
        return m_sKey;
        }

    @Override
    public String getAssociatedKey()
        {
        return m_sAssociated;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sKey        = ExternalizableHelper.readSafeUTF(in);
        m_sAssociated = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sKey);
        ExternalizableHelper.writeSafeUTF(out, m_sAssociated);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sKey        = in.readString(0);
        m_sAssociated = in.readString(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sKey);
        out.writeString(1, m_sAssociated);
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestAssociatedKey that = (TestAssociatedKey) o;
        return Objects.equals(m_sKey, that.m_sKey) && Objects.equals(m_sAssociated, that.m_sAssociated);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sKey, m_sAssociated);
        }

    @Override
    public String toString()
        {
        return "TestAssociatedKey{" +
                "key='" + m_sKey + '\'' +
                ", associated='" + m_sAssociated + '\'' +
                '}';
        }

    // ----- data members ---------------------------------------------------

    private String m_sKey;

    private String m_sAssociated;
    }
