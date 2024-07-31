/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.oracle.coherence.ai.util.Vectors;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * A vector of bytes.
 */
public final class Int8Vector
        implements Vector<byte[]>
    {
    /**
     * Default constructor for serialization.
     */
    public Int8Vector()
        {
        this(EMPTY);
        }

    /**
     * Create a {@link Int8Vector} wrapping the specified {@code byte} array.
     * <p/>
     * Note: this method does nt make a copy of the passed in byte array.
     * Mutating the array will result in changes to the internal state of
     * this vector.
     *
     * @param array  the {@code byte} array to wrap
     */
    public Int8Vector(byte[] array)
        {
        m_array = array;
        }

    @Override
    public int dimensions()
        {
        return m_array.length;
        }

    @Override
    public byte[] get()
        {
        return m_array;
        }

    @Override
    public BitVector binaryQuant()
        {
        return Vectors.binaryQuant(m_array);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_array = in.readByteArray(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeByteArray(0, m_array);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_array = ExternalizableHelper.readByteArray(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeByteArray(out, m_array);
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Int8Vector that = (Int8Vector) o;
        return Objects.deepEquals(m_array, that.m_array);
        }

    @Override
    public int hashCode()
        {
        return Arrays.hashCode(m_array);
        }


    @Override
    public String toString()
        {
        return "Int8Vector{" +
                "vector=" + Arrays.toString(m_array) +
                '}';
        }

    // ----- constants ------------------------------------------------------

    /**
     * An empty Int8 vector.
     */
    public static final byte[] EMPTY = new byte[0];


    // ----- data members ---------------------------------------------------

    /**
     * The actual bytes of the vector.
     */
    private byte[] m_array;
    }
