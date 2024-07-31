/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.distance;

import com.oracle.coherence.ai.DistanceAlgorithm;

import java.util.BitSet;

import static com.oracle.coherence.ai.util.Vectors.dotProduct;

/**
 * A {@link DistanceAlgorithm} that performs inner product distance calculation between two vectors.
 */
public class InnerProductDistance<T>
        extends AbstractDistance<T>
    {
    @Override
    protected double distance(BitSet v1, BitSet v2)
        {
        return 1.0d - dotProduct(v1, v2);
        }

    @Override
    protected double distance(byte[] v1, byte[] v2)
        {
        return 1.0d - dotProduct(v1, v2);
        }

    @Override
    protected double distance(float[] v1, float[] v2)
        {
        return 1.0d - dotProduct(v1, v2);
        }
    }
