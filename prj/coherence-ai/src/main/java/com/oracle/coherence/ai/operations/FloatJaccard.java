/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.operations;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A base {@link com.oracle.coherence.ai.VectorOp} that executes a Jaccard
 * similarity algorithm against a vector of floats.
 */
public abstract class FloatJaccard
        extends BaseVectorOp<Float>
    {
    /**
     * Default constructor for serialization
     */
    protected FloatJaccard()
        {
        }

    /**
     * Create a {@link FloatJaccard} to find similarities between
     * vectors and the specified target vector.
     *
     * @param target  the vector to find similarities to
     */
    public FloatJaccard(float[] target)
        {
        this.target = target == null ? EMPTY : target;
        }

    @Override
    public String id()
        {
        return ID;
        }

    @Override
    public int getImplVersion()
        {
        return 0;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        target = in.readFloatArray(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeFloatArray(0, target);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        target = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, target);
        }

    // ----- constants ------------------------------------------------------

    public static final String ID = "float-jaccard";

    // ----- constants ------------------------------------------------------

    public static final float[] EMPTY = new float[0];

    // ----- data members ---------------------------------------------------

    protected float[] target = EMPTY;
    }
