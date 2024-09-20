/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.distance;

import com.oracle.coherence.ai.BitVector;
import com.oracle.coherence.ai.DistanceAlgorithm;
import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.ai.Int8Vector;
import com.oracle.coherence.ai.Vector;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.BitSet;

/**
 * A base class for {@link DistanceAlgorithm distance algorithm} implementations.
 *
 * @param <T>  the type of the vector the algorithm uses
 */
public abstract class AbstractDistance<T>
        implements DistanceAlgorithm<T>, ExternalizableLite, PortableObject
    {
    @Override
    public double distance(Vector<T> v1, Vector<T> v2)
        {
        if (v1.dimensions() != v2.dimensions())
            {
            throw new IllegalArgumentException("Length of vector v1 (%,d) must be equal to the length of vector v2 (%,d)"
                                                       .formatted(v1.dimensions(), v2.dimensions()));
            }

        if (v1 instanceof Float32Vector)
            {
            return distance(((Float32Vector) v1).get(), ((Float32Vector) v2).get());
            }
        else if (v1 instanceof Int8Vector)
            {
            return distance(((Int8Vector) v1).get(), ((Int8Vector) v2).get());
            }
        else if (v1 instanceof BitVector)
            {
            return distance(((BitVector) v1).get(), ((BitVector) v2).get());
            }

        // should never happen
        throw new IllegalArgumentException("Unsupported vector type " + v1.getClass().getName());
        }

    /**
     * Calculate the distance between two bit vectors.
     *
     * @param v1  the first bit vector
     * @param v2  the second bit vector
     *
     * @return the distance between the two bit vectors
     */
    protected abstract double distance(BitSet v1, BitSet v2);

    /**
     * Calculate the distance between two Int8 (byte) vectors.
     *
     * @param v1  the first Int8 vector
     * @param v2  the second Int8 vector
     *
     * @return the distance between the two Int8 vectors
     */
    protected abstract double distance(byte[] v1, byte[] v2);

    /**
     * Calculate the distance between two Float32 (float) vectors.
     *
     * @param v1  the first Float32 vector
     * @param v2  the second Float32 vector
     *
     * @return the distance between the two Float32 vectors
     */
    protected abstract double distance(float[] v1, float[] v2);

    @Override
    public void readExternal(PofReader in)
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }
    }
