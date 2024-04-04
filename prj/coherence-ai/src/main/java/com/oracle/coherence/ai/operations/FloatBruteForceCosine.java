/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.operations;

import com.tangosol.io.ReadBuffer;

import java.nio.FloatBuffer;

/**
 * A {@link com.oracle.coherence.ai.VectorOp} that executes a Cosine
 * similarity algorithm against a vector of floats.
 * <p>
 * This implementation uses a brute force non-optimized approach to
 * work out the Cosine similarity.
 */
public class FloatBruteForceCosine
        extends FloatCosine
    {
    /**
     * Default constructor for serialization.
     */
    public FloatBruteForceCosine()
        {
        }

    /**
     * Create a {@link FloatBruteForceCosine} operation.
     *
     * @param target  the vector to find similarities to
     */
    public FloatBruteForceCosine(float[] target)
        {
        super(target);
        }

    @Override
    public Float apply(ReadBuffer binary)
        {
        FloatBuffer buffer = binary.toByteBuffer().asFloatBuffer();
        return getCosineSimilarity(buffer);
        }

    /**
     * Calculate the Cosine similarity for the specified float vector with
     * this operation's target vector.
     *
     * @param buffer  a {@link FloatBuffer} containing the vector to
     *                calculate the similarity to the target vector
     *
     * @return  the Cosine similarity for the specified vector to
     *          this operation's target vector
     */
    public float getCosineSimilarity(FloatBuffer buffer)
        {
        double magVector = calculateMagnitude(buffer);
        double magTarget = ensureTargetMagnitude();
        if (magVector == 0.0f || magTarget == 0.0f)
            {
            return magTarget == magVector ? 1.0f : 0.0f;
            }
        double dot = calculateDotProduct(buffer);
        return (float) (dot / (magVector * magTarget));
        }

    protected double calculateDotProduct(FloatBuffer buffer)
        {
        int    size  = m_target.length;
        double total = 0.0d;
        for (int i = 0; i < size; i++)
            {
            total += (m_target[i] * buffer.get(i));
            }
        return total;
        }

    protected double calculateMagnitude(FloatBuffer buffer)
        {
        int    size  = buffer.limit();
        double total = 0.0f;
        for (int i = 0; i < size; i++)
            {
            double f = buffer.get(i);
            total += (f * f);
            }
        return (float) Math.sqrt(total);
        }

    protected double ensureTargetMagnitude()
        {
        double m = magnitude;
        if (Double.isNaN(m))
            {
            m = magnitude = calculateMagnitude(FloatBuffer.wrap(m_target));
            }
        return m;
        }

    // ----- data members ---------------------------------------------------

    protected double magnitude = Double.NaN;
    }
