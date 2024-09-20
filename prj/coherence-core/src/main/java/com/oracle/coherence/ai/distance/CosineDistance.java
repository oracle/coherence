/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.distance;

import com.oracle.coherence.ai.DistanceAlgorithm;
import com.oracle.coherence.ai.util.Vectors;

import java.util.BitSet;

import static com.oracle.coherence.ai.util.Vectors.EPSILON;

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
        double dotProduct = 0.0;
        double normA      = 0.0;
        double normB      = 0.0;

        for (int i = 0; i < v1.size(); i++)
            {
            int a = v1.get(i) ? 1 : 0;
            int b = v2.get(i) ? 1 : 0;
            normA += a * a;
            normB += b * b;
            dotProduct += a * b;
            }

        // Avoid division by zero.
        return 1.0f - (float) (dotProduct / Math.max(Math.sqrt(normA) * Math.sqrt(normB), EPSILON));
        }

    @Override
    protected double distance(byte[] v1, byte[] v2)
        {
        double dotProduct = 0.0;
        double normA      = 0.0;
        double normB      = 0.0;

        for (int i = 0; i < v1.length; i++)
            {
            int a = v1[i];
            int b = v2[i];
            normA += a * a;
            normB += b * b;
            dotProduct += a * b;
            }

        // Avoid division by zero.
        return 1.0f - (float) (dotProduct / Math.max(Math.sqrt(normA) * Math.sqrt(normB), EPSILON));
        }

    @Override
    protected double distance(float[] v1, float[] v2)
        {
        // we require that all float vectors are normalized ahead of time
        // when using cosine distance, so we can simply use dot product
        return 1.0f - (float) Vectors.dotProduct(v1, v2);
        }
    }
