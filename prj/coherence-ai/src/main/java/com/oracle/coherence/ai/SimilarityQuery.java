/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;


import com.oracle.coherence.ai.aggregators.SimilarityAggregator;

import com.tangosol.util.Filter;

/**
 * The configuration for executing a similarity query
 * on a {@link VectorStore}.
 *
 * @param <R>  the type of the vector to be queried
 */
public interface SimilarityQuery<R>
    {
    /**
     * Return the {@link Filter} that will be applied to the vector
     * metadata to restrict the vectors to be queried
     *
     * @return the {@link Filter} that will be applied to the vector metadata
     */
    Filter<?> getFilter();

    /**
     * Return the {@link SimilarityAggregator} that will execute the similarity query.
     *
     * @return the {@link SimilarityAggregator} that will execute the similarity query
     */
    SimilarityAggregator getAggregator();

    /**
     * Return {@code true} if the results of the query should contain the matching vector.
     *
     * @return {@code true} if the results of the query should contain the matching vector
     */
    boolean isIncludeVector();

    /**
     * Return {@code true} if the results of the query should contain the matching vector metadata.
     *
     * @return {@code true} if the results of the query should contain the matching vector metadata
     */
    boolean isIncludeMetadata();

    // ----- inner interface Builder<R> -------------------------------------

    /**
     * A builder that builds a {@link SimilarityQuery}.
     *
     * @param <R>  the type of the vector to be queried
     */
    interface Builder<R>
        {
        /**
         * Set whether the query should return the matching vector metadata.
         *
         * @param f  {@code true} if the query should return the matching vector metadata
         *
         * @return this {@link Builder}
         */
        Builder<R> includeMetadata(boolean f);

        /**
         * Set whether the query should return the matching vector data.
         *
         * @param f  {@code true} if the query should return the matching vector data
         *
         * @return this {@link Builder}
         */
        Builder<R> includeVector(boolean f);

        /**
         * Set the maximum number of results to return.
         *
         * @param max  the maximum number of results to return
         *
         * @return this {@link Builder}
         */
        Builder<R> withMaxResults(int max);

        /**
         * Set the {@link Filter} to apply to the vector metadata to restrict the vectors
         * used in the query.
         *
         * @param filter  set the {@link Filter} to apply to the vector metadata to restrict
         *                the vectors used in the query
         *
         * @return this {@link Builder}
         */
        Builder<R> withFilter(Filter<?> filter);

        /**
         * Build the {@link SimilarityQuery}.
         *
         * @return  a {@link SimilarityQuery} built from the state in this builder
         */
        SimilarityQuery<R> build();
        }
    }
