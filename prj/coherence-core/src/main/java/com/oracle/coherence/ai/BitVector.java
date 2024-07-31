/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.BitSet;
import java.util.Objects;

/**
 * A vector of bits.
 */
public final class BitVector
        implements Vector<BitSet>
    {
    /**
     * Default constructor for serialization.
     */
    public BitVector()
        {
        this(new BitSet());
        }

    /**
     * Create a {@link BitVector} wrapping the specified {@code long} array.
     *
     * @param array  the {@code long} array representing the bits in the bit set
     */
    public BitVector(long[] array)
        {
        this(BitSet.valueOf(array));
        }

    /**
     * Create a {@link BitVector} wrapping the specified {@code byte} array.
     *
     * @param array  the {@code byte} array representing the bits in the bit set
     */
    public BitVector(byte[] array)
        {
        this(BitSet.valueOf(array));
        }

    /**
     * Create a {@link BitVector} wrapping the specified {@code byte} array.
     * <p/>
     * Note: this method does nt make a copy of the passed in {@link BitSet}.
     * Mutating the {@link BitSet} will result in changes to the internal
     * state of this vector.
     *
     * @param bits  the {@link BitSet} to wrap
     */
    public BitVector(BitSet bits)
        {
        m_bits = bits;
        }

    @Override
    public int dimensions()
        {
        return m_bits.size();
        }

    @Override
    @JsonbTransient
    public BitSet get()
        {
        return m_bits;
        }

    @Override
    public BitVector binaryQuant()
        {
        return this;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        long[] an = in.readLongArray(0);
        m_bits = BitSet.valueOf(an);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLongArray(0, m_bits.toLongArray());
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        long[] an = ExternalizableHelper.readLongArray(in);
        m_bits = BitSet.valueOf(an);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        long[] an = m_bits.toLongArray();
        out.writeInt(an.length);
        for(long a : an)
            {
            out.writeLong(a);
            }
        }

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
        BitVector bitVector = (BitVector) o;
        return Objects.equals(m_bits, bitVector.m_bits);
        }

    @Override
    public int hashCode()
        {
        return Objects.hashCode(m_bits);
        }

    @Override
    public String toString()
        {
        return "BitVector{" +
                "m_bits=" + m_bits +
                '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link BitSet} representing the vector's bits.
     */
    @JsonbProperty("bits")
    private BitSet m_bits;
    }
