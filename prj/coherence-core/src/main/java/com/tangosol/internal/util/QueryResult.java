/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.util.Filter;

/**
 * QueryResult is a structure that holds the state of the query execution
 * (see PartitionedCache.Storage#query)
 *
 * @author gg 2015.04.13
 */
public class QueryResult
    {
    /**
     * Construct {@link QueryResult} instance.
     */
    public QueryResult()
        {
        }

    /**
     * Construct {@link QueryResult} instance.
     *
     * @param aoResult         the array of results (keys or entries) that match the query
     * @param cResults         the number of results in the array
     * @param filterRemaining  the filter that still needs to be evaluated
     */
    public QueryResult(Object[] aoResult, int cResults, Filter filterRemaining)
        {
        this.aoResult = aoResult;
        this.cResults = cResults;
        this.filterRemaining = filterRemaining;
        }

    /**
     * The array of results (keys or entries) that match the query.
     */
    public Object[] aoResult;

    /**
     * The number of results in the array.
     */
    public int cResults;

    /**
     * The filter that still needs to be evaluated.
     */
    public Filter filterRemaining;
    }
