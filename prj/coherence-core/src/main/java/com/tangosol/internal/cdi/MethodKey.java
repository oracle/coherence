/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.cdi;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;

/**
 * Key class that represents the key parameters of the method.
 */
public final class MethodKey
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public MethodKey()
        {
        }

    /**
     * Construct a method key.
     *
     * @param paramValues  the array of parameters
     * @param indices      the array of indexes of key parameters
     */
    public MethodKey(Object[] paramValues, Integer[] indices)
        {
        m_aKeys = new Object[indices.length];
        for (int i = 0; i < m_aKeys.length; i++)
            {
            m_aKeys[i] = paramValues[indices[i]];
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_aKeys = ExternalizableHelper.readObjectArray(in);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeInt(m_aKeys.length);
        for (int i = 0; i < m_aKeys.length; i++)
            {
            ExternalizableHelper.writeObject(out, m_aKeys[i]);
            }
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aKeys = in.readArray(0, Object[]::new);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObjectArray(0, m_aKeys);
        }

    // ----- Object methods -------------------------------------------------

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
        MethodKey that = (MethodKey) o;
        return Arrays.deepEquals(m_aKeys, that.m_aKeys);
        }

    @Override
    public int hashCode()
        {
        return Arrays.deepHashCode(m_aKeys);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The array of Object parameters.
     */
    private Object[] m_aKeys;
    }
