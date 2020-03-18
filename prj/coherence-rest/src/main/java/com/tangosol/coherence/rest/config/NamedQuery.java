/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

/**
 * Holder for configured named query data.
 *
 * @author ic  2011.12.03
 */
public class NamedQuery
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Construct an instance of <tt>NamedQuery</tt>.
     *
     * @param sName         query name
     * @param sExpression   query expression
     * @param sQueryEngine  name of query engine responsible to execute this query
     * @param cMaxResults   max size of result set that this query can return
     */
    public NamedQuery(String sName, String sExpression, String sQueryEngine,
            int cMaxResults)
        {
        if (DirectQuery.KEY.equals(sName) && !getClass().equals(DirectQuery.class))
            {
            throw new IllegalArgumentException("illegal query name: " + sName);
            }
        m_sName        = sName;
        m_sExpression  = sExpression;
        m_sQueryEngine = sQueryEngine;
        m_cMaxResults  = cMaxResults;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the query name.
     *
     * @return query name
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Return the query expression.
     *
     * @return query expression
     */
    public String getExpression()
        {
        return m_sExpression;
        }

    /**
     * Return the name of the query engine responsible to execute this query.
     *
     * @return query engine name
     */
    public String getQueryEngineName()
        {
        return m_sQueryEngine;
        }

    /**
     * Return the max allowed result set size.
     *
     * @return max size of the result set
     */
    public int getMaxResults()
        {
        return m_cMaxResults;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Query name
     */
    private final String m_sName;

    /**
     * Query expression
     */
    private final String m_sExpression;

    /**
     * Query engine
     */
    private final String m_sQueryEngine;

    /**
     * Max size of result set
     */
    private final int m_cMaxResults;
    }
