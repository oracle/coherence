/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib;

/**
 * Query Tuple that represents the results of a knn query. It contains two
 * arrays: ids and coefficients.
 */
public class QueryTuple
    {
    /**
     * The identifiers of the nearest matches.
     */
    int[] ids;

    /**
     * The similarity coefficients of the nearest matches.
     */
    float[] coefficients;

    /**
     * The number of actual matches (which may be less than the size of the id array).
     * This only applied if querying with filters
     */
    private int count;

    /**
     * Create a {@link QueryTuple} to hold a specified number of results.
     *
     * @param k  the number of results to hold
     */
    QueryTuple(int k)
        {
        this(new int[k]);
        }

    /**
     * Create a {@link QueryTuple} to hold the specified ids.
     *
     * @param ids the result ids
     */
    QueryTuple(int[] ids)
        {
        this.ids          = ids;
        this.coefficients = new float[ids.length];
        this.count        = ids.length;
        }

    /**
     * Return the vector ids.
     *
     * @return the vector ids
     */
    public int[] getIds()
        {
        return ids;
        }

    /**
     * Return the similarity coefficients.
     *
     * @return the similarity coefficients
     */
    public float[] getCoefficients()
        {
        return coefficients;
        }

    /**
     * Return {@code true} if there are no results.
     *
     * @return {@code true} if there are no results
     */
    public boolean empty()
        {
        return count == 0;
        }

    /**
     * Set this {@link QueryTuple} to be empty
     */
    void setEmpty()
        {
        this.count = 0;
        }

    /**
     * Return the count of the results.
     *
     * @return the count of the results
     */
    public int count()
        {
        return count;
        }

    /**
     * Set the count of the results.
     *
     * @param count  the count of the results
     */
    void count(int count)
        {
        this.count = count;
        }

    /**
     * An empty {@link QueryTuple}.
     */
    public static final QueryTuple EMPTY = new QueryTuple(0);
    }
