/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.queries;

import com.oracle.coherence.ai.VectorOp;

import com.oracle.coherence.ai.aggregators.SimilarityAggregator;

import com.oracle.coherence.ai.operations.FloatBruteForceJaccard;
import com.oracle.coherence.ai.operations.LongBruteForceJaccard;

/**
 * A {@link com.oracle.coherence.ai.SimilarityQuery} that uses the
 * Jaccard Similarity algorithm to find the nearest neighbours.
 * <p>
 * Jaccard similarity is defined as {@code J(a,b) = |a⋂b| / |a⋃b|}
 * <p>
 * That is, the size of the intersection of both vectors divided by the
 * count of the union of both vectors. A value of {@code 1} means the
 * vectors are identical.
 *
 * @param <R>  the type of the vector to query
 */
public class Jaccard<R>
        extends BaseQuery<R>
    {
    /**
     * Default constructor for serialization.
     */
    public Jaccard()
        {
        }

    /**
     * Create a {@link Jaccard} query.
     *
     * @param builder     the {@link Builder} containing the parameters for the query
     * @param aggregator  the {@link SimilarityAggregator} to use to execute the query
     */
    protected Jaccard(Builder<R> builder, SimilarityAggregator aggregator)
        {
        super(builder, aggregator);
        }

    /**
     * Obtain a {@link Builder} to build a {@link Jaccard} query to execute
     * against a vector of {@code float} values.
     *
     * @param vector  the vector of {@code float} values to find the closest match to
     *
     * @return a {@link Builder} to build a {@link Jaccard} query to execute
     *         against a vector of {@code float} values
     */
    public static Builder<float[]> forFloats(float[] vector)
        {
        return new Builder<>(vector);
        }

    /**
     * Obtain a {@link Builder} to build a {@link Jaccard} query to execute
     * against a vector of {@code long} values.
     *
     * @param vector  the vector of {@code long} values to find the closest match to
     *
     * @return a {@link Builder} to build a {@link Jaccard} query to execute
     *         against a vector of {@code long} values
     */
    public static Builder<long[]> forLongs(long[] vector)
        {
        return new Builder<>(vector);
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder that builds a {@link Jaccard} query.
     *
     * @param <R>  the type of the vector to query
     */
    public static class Builder<R>
            extends BaseQuery.Builder<R, Builder<R>>
        {
        /**
         * Create a builder.
         *
         * @param vector the vector to find similar vectors to
         */
        private Builder(R vector)
            {
            super(vector);
            }

        @Override
        public Jaccard<R> build()
            {
            VectorOp<Float> op = null;
            if (m_vector instanceof long[])
                {
                op = new LongBruteForceJaccard((long[]) m_vector);
                }
            else if (m_vector instanceof float[])
                {
                op = new FloatBruteForceJaccard((float[]) m_vector);
                }
            return new Jaccard<>(this, new SimilarityAggregator(op, m_maxResults, false,
                    m_includeVector, m_includeMetadata));
            }
        }
    }
