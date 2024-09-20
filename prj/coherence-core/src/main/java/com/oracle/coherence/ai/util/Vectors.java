/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.util;

import com.oracle.coherence.ai.BitVector;
import com.oracle.coherence.ai.Vector;

import java.util.BitSet;

/**
 * Utility methods for supporting vectors.
 */
@SuppressWarnings("DuplicatedCode")
public class Vectors
    {
    /**
     * Calculate the magnitude of a bit vector.
     *
     * @param v  the bit vector
     *
     * @return the magnitude of the bit vector
     */
    public static double magnitude(BitSet v)
        {
        return Math.sqrt(v.cardinality());
        }

    /**
     * Calculate the magnitude of an Int8 vector.
     *
     * @param v  the Int8 vector
     *
     * @return the magnitude of the Int8 vector
     */
    public static double magnitude(byte[] v)
        {
        double magnitude = 0;
        for (byte b : v)
            {
            magnitude += b * b;
            }
        return Math.sqrt(magnitude);
        }

    /**
     * Calculate the magnitude of a float vector.
     *
     * @param v  the float vector
     *
     * @return the magnitude of the float vector
     */
    public static double magnitude(float[] v)
        {
        double magnitude = 0;
        for (float f : v)
            {
            magnitude += f * f;
            }
        return Math.sqrt(magnitude);
        }

    /**
     * Calculate the dot product of two bit vectors.
     *
     * @param v1  the first bit vector
     * @param v2  the second bit vector
     *
     * @return the dot product of the bit vectors
     */
    public static double dotProduct(BitSet v1, BitSet v2)
        {
        BitSet v = BitSet.valueOf(v1.toLongArray());
        v.and(v2);
        return v.cardinality();
        }

    /**
     * Calculate the dot product of two Int8 vectors.
     *
     * @param v1  the first Int8 vector
     * @param v2  the second Int8 vector
     *
     * @return the dot product of the bit vectors
     */
    public static double dotProduct(byte[] v1, byte[] v2)
        {
        double dotProduct = 0.0;

        for (int i = 0; i < v1.length; i++)
            {
            dotProduct += v1[i] * v2[i];
            }
        return dotProduct;
        }

    /**
     * Calculate the dot product of two float vectors.
     *
     * @param v1  the first float vector
     * @param v2  the second float vector
     *
     * @return the magnitude of the float vectors
     */
    public static double dotProduct(float[] v1, float[] v2)
        {
        double dotProduct = 0.0;

        for (int i = 0; i < v1.length; i++)
            {
            dotProduct += v1[i] * v2[i];
            }
        return dotProduct;
        }

    /**
     * Calculate the L2 Squared value for two bit vectors.
     *
     * @param v1  the first bit vector
     * @param v2  the second bit vector
     *
     * @return the L2 Squared value for the two bit vectors
     */
    public static double l2squared(BitSet v1, BitSet v2)
        {
        BitSet v = BitSet.valueOf(v1.toLongArray());
        v.xor(v2);
        return v.cardinality();
        }

    /**
     * Calculate the L2 Squared value for two Int8 vectors.
     *
     * @param v1  the first Int8 vector
     * @param v2  the second Int8 vector
     *
     * @return the L2 Squared value for the two bit vectors
     */
    public static double l2squared(byte[] v1, byte[] v2)
        {
        double l2squared = 0.0;

        for (int i = 0; i < v1.length; i++)
            {
            int n = v1[i] - v2[i];
            l2squared += n ^ 2;
            }
        return l2squared;
        }

    /**
     * Calculate the L2 Squared value for two float vectors.
     *
     * @param v1  the first float vector
     * @param v2  the second float vector
     *
     * @return the L2 Squared value for the two bit vectors
     */
    public static double l2squared(float[] v1, float[] v2)
        {
        double l2squared = 0.0;

        for (int i = 0; i < v1.length; i++)
            {
            float f = v1[i] - v2[i];
            l2squared += (f * f);
            }
        return l2squared;
        }

    /**
     * Normalize an Int8 vector.
     *
     * @param array  the Int8 vector to normalize.
     *
     * @return the Int8 vector normalized to a float vector
     */
    public static float[] normalize(byte[] array)
        {
        float norm = 0.0f;
        int   cDim = array.length;
        for (float v : array)
            {
            norm += (v * v);
            }
        norm = 1.0f / ((float) Math.sqrt(norm) + EPSILON);

        float[] aNorm = new float[cDim];
        for (int i = 0; i < cDim; i++)
            {
            aNorm[i] = array[i] * norm;
            }
        return aNorm;
        }

    /**
     * Normalize a float vector.
     * <p/>
     * Note, the vector is normalized in place, so the values
     * in the array parameter will be updated and the same
     * array returned.
     *
     * @param array  the float vector to normalize.
     *
     * @return the normalized to a float vector
     */
    public static float[] normalize(float[] array)
        {
        float norm = 0.0f;
        int   cDim = array.length;
        for (float v : array)
            {
            norm += (v * v);
            }

        norm = 1.0f / ((float) Math.sqrt(norm) + EPSILON);
        for (int i = 0; i < cDim; i++)
            {
            array[i] = array[i] * norm;
            }
        return array;
        }

    /**
     * Quantize a float vector to a bit vector.
     *
     * @param vector the float vector to quantize
     *
     * @return the result of the binary quantization of the float vector
     */
    public static BitVector binaryQuant(float[] vector)
        {
        if (vector == null)
            {
            return null;
            }

        BitSet bin = new BitSet(vector.length);
        for (int i = 0; i < vector.length; i++)
            {
            if (vector[i] > 0)
                {
                bin.set(i);
                }
            }
        return new BitVector(bin);
        }

    /**
     * Quantize an Int8 vector to a bit vector.
     *
     * @param vector the Int8 vector to quantize
     *
     * @return the result of the binary quantization of the Int8 vector
     */
    public static BitVector binaryQuant(byte[] vector)
        {
        if (vector == null)
            {
            return null;
            }

        BitSet bin = new BitSet(vector.length);
        for (int i = 0; i < vector.length; i++)
            {
            if (vector[i] > 0)
                {
                bin.set(i);
                }
            }
        return new BitVector(bin);
        }

    /**
     * Calculate the hamming distance between two bit vectors.
     *
     * @param x  the first bit vector
     * @param y  the second bit vector
     *
     * @return  the hamming distance between the two vectors
     */
    public static int hammingDistance(Vector<BitSet> x, Vector<BitSet> y)
        {
        return hammingDistance(x.get(), y.get());
        }

    /**
     * Calculate the hamming distance between two bit vectors.
     *
     * @param x  the first bit vector
     * @param y  the second bit vector
     *
     * @return  the hamming distance between the two vectors
     */
    public static int hammingDistance(BitSet x, BitSet y)
        {
        if (x.size() != y.size())
            {
            throw new IllegalArgumentException(String.format("BitSets have different length: x[%d], y[%d]", x.size(), y.size()));
            }

        long[] ax = x.toLongArray();
        long[] ay = y.toLongArray();

        int dist = 0;
        for (int i = 0; i < ax.length; i++)
            {
            dist += d(ax[i], ay[i]);
            }

        return dist;
        }

    private static int d(long x, long y)
        {
        int dist = 0;
        long val = x ^ y;

        // Count the number of set bits (Knuth's algorithm)
        while (val != 0)
            {
            ++dist;
            val &= val - 1;
            }

        return dist;
        }

    /**
     * A very small value to use to avoid divide by zero errors.
     */
    public static final float EPSILON = 1e-30f;
    }
