/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;


import com.tangosol.util.Filter;


/**
 * QueryResult is a structure that holds the state of the query execution
 * (see PartitionedCache$Storage#query)
 *
 * @author gg 2015.04.13
 */
public class QueryResult
    {
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
