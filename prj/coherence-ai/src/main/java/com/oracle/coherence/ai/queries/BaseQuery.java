/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.queries;

import com.oracle.coherence.ai.SimilarityQuery;

import com.oracle.coherence.ai.aggregators.SimilarityAggregator;

import com.tangosol.util.Filter;

/**
 * A base class for {@link SimilarityQuery} classes.
 *
 * @param <R>  the type of the vector to be queried
 */
public abstract class BaseQuery<R>
        implements SimilarityQuery<R>
    {
    /**
     * Default constructor for serialization.
     */
    protected BaseQuery()
        {
        }

    /**
     * Create a {@link BaseQuery}.
     *
     * @param builder     the {@link Builder} containing the query state
     * @param aggregator  the aggregator to use to execute the query
     */
    protected BaseQuery(Builder<R, ? extends Builder<R, ?>> builder, SimilarityAggregator aggregator)
        {
        m_filter          = builder.m_filter;
        m_includeVector   = builder.m_includeVector;;
        m_includeMetadata = builder.m_includeMetadata;
        m_aggregator      = aggregator;
        }

    @Override
    public Filter<?> getFilter()
        {
        return m_filter;
        }

    @Override
    public SimilarityAggregator getAggregator()
        {
        return m_aggregator;
        }

    /**
     * Returns {@code true} if the query result should contain
     * the corresponding vector.
     *
     * @return {@code true} if the query result should contain
     *         the corresponding vector
     */
    public boolean isIncludeVector()
        {
        return m_includeVector;
        }

    /**
     * Returns {@code true} if the query result should contain
     * the corresponding vector metadata.
     *
     * @return {@code true} if the query result should contain
     *         the corresponding vector metadata
     */
    public boolean isIncludeMetadata()
        {
        return m_includeMetadata;
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A base class for query builders.
     *
     * @param <R>  the type of the vector to be queried
     * @param <B>   they type of the builder subclass
     */
    @SuppressWarnings("unchecked")
    protected static abstract class Builder<R, B extends Builder<R, B>>
            implements SimilarityQuery.Builder<R>
        {
        /**
         * Create a query builder.
         *
         * @param vector  the vector to be matched by the query
         */
        protected Builder(R vector)
            {
            this.m_vector = vector;
            }

        @Override
        public B includeMetadata(boolean f)
            {
            return (B) this;
            }

        @Override
        public B includeVector(boolean f)
            {
            return (B) this;
            }

        @Override
        public B withMaxResults(int max)
            {
            if (max <= 0 || max > 10000)
                {
                throw new IllegalArgumentException("Max results must be in the range 1 .. 10000");
                }
            m_maxResults = max;
            return (B) this;
            }

        @Override
        public B withFilter(Filter<?> filter)
            {
            m_filter = filter;
            return (B) this;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Filter} to apply to the vector metadata.
         */
        protected Filter<?> m_filter;

        /**
         * The maximum number of results to return.
         */
        protected int m_maxResults = 100;

        /**
         * {@code true} if the result should contain the corresponding vector.
         */
        protected boolean m_includeVector = true;

        /**
         * {@code true} if the result should contain the corresponding vector metadata.
         */
        protected boolean m_includeMetadata = true;

        /**
         * The vector to use in the query.
         */
        protected R m_vector;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Filter} to apply to the vector metadata.
     */
    private Filter<?> m_filter;

    /**
     * The {@link SimilarityAggregator} to use to execute the query.
     */
    private SimilarityAggregator m_aggregator;

    /**
     * {@code true} if the result should contain the corresponding vector.
     */
    protected boolean m_includeVector;

    /**
     * {@code true} if the result should contain the corresponding vector metadata.
     */
    protected boolean m_includeMetadata;
    }
