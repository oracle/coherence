/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.distance;

import com.oracle.coherence.ai.DistanceAlgorithm;
import com.oracle.coherence.ai.util.Vectors;

import java.util.BitSet;

/**
 * A {@link DistanceAlgorithm} that performs a cosine similarity calculation between two vectors.
 * <p/>
 * Cosine similarity measures the similarity between two vectors of an inner product space.
 * It is measured by the cosine of the angle between two vectors and determines whether two
 * vectors are pointing in roughly the same direction. It is often used to measure document
 * similarity in text analysis.
 */
public class CosineDistance<T>
        extends AbstractDistance<T>
    {
    @Override
    protected double distance(BitSet v1, BitSet v2)
        {
        return 1.0d - Vectors.cosine(v1, v2);
        }

    @Override
    protected double distance(byte[] v1, byte[] v2)
        {
        return 1.0d - Vectors.cosine(v1, v2);
        }

    @Override
    protected double distance(float[] v1, float[] v2)
        {
        return 1.0d - Vectors.cosine(v1, v2);
        }
    }
