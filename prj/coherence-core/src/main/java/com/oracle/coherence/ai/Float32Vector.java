/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.oracle.coherence.ai.util.Vectors;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;
import jakarta.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * A vector of floats.
 */
public final class Float32Vector
        implements Vector<float[]>
    {
    /**
     * Default constructor for serialization.
     */
    public Float32Vector()
        {
        this(EMPTY);
        }

    /**
     * Create a {@link Float32Vector} wrapping the specified {@code float} array.
     * <p/>
     * Note: this method does not make a copy of the passed in float array.
     * Mutating the array will result in changes to the internal state of
     * this vector.
     *
     * @param array  the {@code float} array to wrap
     */
    public Float32Vector(float[] array)
        {
        m_array = array;
        }

    @Override
    public int dimensions()
        {
        return m_array.length;
        }

    @Override
    public float[] get()
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
        m_array = in.readFloatArray(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeFloatArray(0, m_array, true);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_array = ExternalizableHelper.readFloatArray(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeFloatArray(out, m_array);
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Float32Vector that = (Float32Vector) o;
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
        return "Float32Vector{" +
                "vector=" + Arrays.toString(m_array) +
                '}';
        }

    // ----- constants ------------------------------------------------------

    /**
     * An empty float vector.
     */
    public static final float[] EMPTY = new float[0];

    // ----- data members ---------------------------------------------------

    /**
     * The actual float vector.
     */
    @JsonbProperty("array")
    private float[] m_array;
    }
