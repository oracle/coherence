/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.util;

import com.oracle.coherence.ai.BitVector;
import com.oracle.coherence.ai.Vector;

import com.tangosol.internal.lucene.util.VectorUtil;

import java.util.BitSet;

/**
 * Utility methods for vector support.
 *
 * @author Aleks Seovic  2024.07.30
 * @since 25.03
 */
@SuppressWarnings("unused")
public class Vectors
    {
    /**
     * Calculate the dot product of two bit vectors.
     *
     * @param a  the first vector
     * @param b  the second vector
     *
     * @return the dot product of the bit vectors
     */
    public static double dotProduct(BitSet a, BitSet b)
        {
        BitSet v = BitSet.valueOf(a.toLongArray());
        v.and(b);
        return v.cardinality();
        }

    /**
     * Calculate the dot product of two int8 vectors.
     *
     * @param a  the first vector
     * @param b  the second vector
     *
     * @return the dot product of the int8 vectors
     */
    public static int dotProduct(byte[] a, byte[] b)
        {
        return VectorUtil.dotProduct(a, b);
        }

    /**
     * Calculate the dot product of two float32 vectors.
     *
     * @param a  the first vector
     * @param b  the second vector
     *
     * @return the dot product of the float32 vectors
     */
    public static float dotProduct(float[] a, float[] b)
        {
        return VectorUtil.dotProduct(a, b);
        }

    /**
     * Calculate the cosine similarity of two float32 vectors.
     *
     * @param v1  the first vector
     * @param v2  the second vector
     *
     * @return the cosine similarity of the float32 vectors
     */
    public static float cosine(BitSet v1, BitSet v2)
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
        return (float) (dotProduct / Math.max(Math.sqrt(normA) * Math.sqrt(normB), EPSILON));
        }

    /**
     * Calculate the cosine similarity of two float32 vectors.
     *
     * @param a  the first vector
     * @param b  the second vector
     *
     * @return the cosine similarity of the float32 vectors
     */
    public static float cosine(byte[] a, byte[] b)
        {
        return VectorUtil.cosine(a, b);
        }

    /**
     * Calculate the cosine similarity of two int8 vectors.
     *
     * @param a  the first vector
     * @param b  the second vector
     *
     * @return the cosine similarity of the int8 vectors
     */
    public static float cosine(float[] a, float[] b)
        {
        return VectorUtil.cosine(a, b);
        }

    /**
     * Calculate the L2 Squared value for two bit vectors.
     *
     * @param a  the first vector
     * @param b  the second vector
     *
     * @return the L2 Squared value for the two bit vectors
     */
    public static double l2squared(BitSet a, BitSet b)
        {
        BitSet v = BitSet.valueOf(a.toLongArray());
        v.xor(b);
        return v.cardinality();
        }

    /**
     * Calculate the L2 Squared value for two float32 vectors.
     *
     * @param a  the first vector
     * @param b  the second vector
     *
     * @return the L2 Squared value for the two float32 vectors
     */
    public static double l2squared(float[] a, float[] b)

        {
        return VectorUtil.squareDistance(a, b);
        }

    /**
     * Calculate the L2 Squared value for two int8 vectors.
     *
     * @param a  the first vector
     * @param b  the second vector
     *
     * @return the L2 Squared value for the two int8 vectors
     */
    public static double l2squared(byte[] a, byte[] b)
        {
        return VectorUtil.squareDistance(a, b);
        }

    /**
     * Normalize an int8 vector.
     *
     * @param array  the int8 vector to normalize.
     *
     * @return the int8 vector normalized to a float32 vector
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
     * Normalize a float32 vector.
     *
     * @param v  the vector to normalize
     *
     * @return the input vector after normalization
     *
     * @throws IllegalArgumentException when the vector is all zero
     */
    public static float[] normalize(float[] v)
        {
        return VectorUtil.l2normalize(v);
        }

    /**
     * Normalize a float32 vector.
     *
     * @param v            the vector to normalize
     * @param throwOnZero  whether to throw an exception when <code>v</code> has
     *                     all zeros
     *
     * @return the input vector after normalization
     *
     * @throws IllegalArgumentException when the vector is all zero and
     *                                  throwOnZero is true
     */
    public static float[] normalize(float[] v, boolean throwOnZero)
        {
        return VectorUtil.l2normalize(v, throwOnZero);
        }

    public static boolean isUnitVector(float[] v)
        {
        return VectorUtil.isUnitVector(v);
        }

    /**
     * Adds the second argument to the first
     *
     * @param u the destination
     * @param v the vector to add to the destination
     */
    public static void add(float[] u, float[] v)
        {
        VectorUtil.add(u, v);
        }

    /**
     * Returns XOR bit count computed over signed bytes.
     *
     * @param a  bytes containing a vector
     * @param b  bytes containing another vector, of the same dimension
     *
     * @return the value of the XOR bit count of the two vectors
     */
    public static int xorBitCount(byte[] a, byte[] b)
        {
        return VectorUtil.xorBitCount(a, b);
        }

    /**
     * Returns dot product score computed over signed bytes, scaled to be in [0, 1].
     *
     * @param a  bytes containing a vector
     * @param b  bytes containing another vector, of the same dimension
     *
     * @return the value of the similarity function applied to the two vectors
     */
    public static float dotProductScore(byte[] a, byte[] b)
        {
        return VectorUtil.dotProductScore(a, b);
        }

    /**
     * Returns a scaled score for maximum-inner-product.
     *
     * @param vectorDotProductSimilarity  the raw similarity between two vectors
     *
     * @return a scaled score preventing negative scores for maximum-inner-product
     */
    public static float scaleMaxInnerProductScore(float vectorDotProductSimilarity)
        {
        return VectorUtil.scaleMaxInnerProductScore(vectorDotProductSimilarity);
        }

    /**
     * Checks if a float vector only has finite components.
     *
     * @param v  bytes containing a vector
     *
     * @return the vector for call-chaining
     *
     * @throws IllegalArgumentException if any component of vector is not finite
     */
    public static float[] checkFinite(float[] v)
        {
        return VectorUtil.checkFinite(v);
        }

    /**
     * Given an array {@code buffer} that is sorted between indexes {@code 0}
     * inclusive and {@code to} exclusive, find the first array index whose
     * value is greater than or equal to {@code target}. This index is
     * guaranteed to be at least {@code from}. If there is no such array index,
     * {@code to} is returned.
     */
    public static int findNextGEQ(int[] buffer, int target, int from, int to)
        {
        return VectorUtil.findNextGEQ(buffer, target, from, to);
        }

    /**
     * Scalar quantizes {@code vector}, putting the result into {@code dest}.
     *
     * @param vector       the vector to quantize
     * @param dest         the destination vector
     * @param scale        the scaling factor
     * @param alpha        the alpha value
     * @param minQuantile  the lower quantile of the distribution
     * @param maxQuantile  the upper quantile of the distribution
     *
     * @return the corrective offset that needs to be applied to the score
     */
    public static float minMaxScalarQuantize(float[] vector, byte[] dest, float scale, float alpha, float minQuantile, float maxQuantile)
        {
        return VectorUtil.minMaxScalarQuantize(vector, dest, scale, alpha, minQuantile, maxQuantile);
        }

    /**
     * Recalculates the offset for {@code vector}.
     *
     * @param vector          the vector to quantize
     * @param oldAlpha        the previous alpha value
     * @param oldMinQuantile  the previous lower quantile
     * @param scale           the scaling factor
     * @param alpha           the alpha value
     * @param minQuantile     the lower quantile of the distribution
     * @param maxQuantile     the upper quantile of the distribution
     *
     * @return the new corrective offset
     */
    public static float recalculateOffset(byte[] vector, float oldAlpha, float oldMinQuantile, float scale, float alpha, float minQuantile, float maxQuantile)
        {
        return VectorUtil.recalculateOffset(vector, oldAlpha, oldMinQuantile, scale, alpha, minQuantile, maxQuantile);
        }

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

    // ---- constants -------------------------------------------------------

    /**
     * Small float used to prevent division by zero.
     */
    private static final float EPSILON = 1e-4f;
    }
