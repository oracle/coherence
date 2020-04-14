/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;


import com.tangosol.coherence.rest.query.QueryEngine;


/**
 * Holder for direct query configuration.
 *
 * @author ic  2011.12.10
 */
public class DirectQuery
        extends NamedQuery
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Construct an instance of <tt>DirectQuery</tt>.
     *
     * @param cMaxResults   max size of result set that this query can return
     */
    public DirectQuery(int cMaxResults)
        {
        this(QueryEngine.DEFAULT, cMaxResults);
        }

    /**
     * Construct an instance of <tt>DirectQuery</tt>.
     *
     * @param sQueryEngine  name of query engine responsible to execute direct query
     * @param cMaxResults   max size of result set that this query can return
     */
    public DirectQuery(String sQueryEngine, int cMaxResults)
        {
        super(KEY, null, sQueryEngine, cMaxResults);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The key that can be used to store direct query in registry or map
     * implementation.
     */
    static final String KEY = "__DIRECT__";
    }
