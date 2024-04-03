/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.queries;

import com.oracle.coherence.ai.VectorOp;

import com.oracle.coherence.ai.aggregators.SimilarityAggregator;

import com.oracle.coherence.ai.operations.FloatBruteForceCosine;

/**
 * A {@link com.oracle.coherence.ai.SimilarityQuery} that uses the
 * Cosine Similarity algorithm to find the nearest neighbours.
 *
 * @param <R>  the type of the vector to query
 */
public class Cosine<R>
        extends BaseQuery<R>
    {
    /**
     * Default constructor for serialization.
     */
    public Cosine()
        {
        }

    /**
     * Create a {@link Cosine} query.
     *
     * @param builder     the {@link Builder} containing the parameters for the query
     * @param aggregator  the {@link SimilarityAggregator} to use to execute the query
     */
    protected Cosine(Builder<R> builder, SimilarityAggregator aggregator)
        {
        super(builder, aggregator);
        }

    /**
     * Obtain a {@link Builder} to build a {@link Cosine} query to execute
     * against a vector of {@code float} values.
     *
     * @param vector  the vector of {@code float} values to find the closest match to
     *
     * @return a {@link Builder} to build a {@link Cosine} query to execute
     *         against a vector of {@code float} values
     */
    public static Builder<float[]> forFloats(float[] vector)
        {
        return new Builder<>(vector);
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder that builds a {@link Cosine} query.
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
        public Cosine<R> build()
            {
            VectorOp<Float> op = null;
            if (m_vector instanceof float[])
                {
                op = new FloatBruteForceCosine((float[]) m_vector);
                }
            return new Cosine<>(this, new SimilarityAggregator(op, m_maxResults, false,
                    m_includeVector, m_includeMetadata));
            }
        }
    }
