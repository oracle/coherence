/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder for query configuration.
 * <p>
 * Query configuration is composed of two parts:
 * <ul>
 *  <li>named queries (pre-defined queries that can be invoked using their name)</li>
 *  <li>direct query (queries submitted directly as part of URL)</li>
 * </ul>
 *
 * @see NamedQuery
 * @see DirectQuery
 *
 * @author ic 2011/11/26
 */
public class QueryConfig
    {
    // ---- public API ------------------------------------------------------

    /**
     * Add named query to this config.
     *
     * @param query  named query to add
     */
    public QueryConfig addNamedQuery(NamedQuery query)
        {
        m_mapNamedQueries.put(query.getName(), query);
        return this;
        }

    /**
     * Return the named query.
     *
     * @param sName  name of the query
     *
     * @return named query
     */
    public NamedQuery getNamedQuery(String sName)
        {
        return m_mapNamedQueries.get(sName);
        }

    /**
     * Return <tt>true</tt> if this configuration contains a query with
     * given name.
     *
     * @param sName  name of the query whose presence is to be tested
     *
     * @return <tt>true</tt> if query with given name exists in this
     *         configuration.
     */
    public boolean containsNamedQuery(String sName)
        {
        return m_mapNamedQueries.containsKey(sName);
        }

    /**
     * Return the direct query.
     *
     * @return direct query
     */
    public DirectQuery getDirectQuery()
        {
        return (DirectQuery) m_mapNamedQueries.get(DirectQuery.KEY);
        }

    /**
     * Set the direct query.
     *
     * @param directQuery  direct query
     */
    public QueryConfig setDirectQuery(DirectQuery directQuery)
        {
        m_mapNamedQueries.put(DirectQuery.KEY, directQuery);
        return this;
        }

    /**
     * Return <tt>true</tt> if direct querying is enabled.
     *
     * @return <tt>true</tt> if direct querying is enabled
     */
    public boolean isDirectQueryEnabled()
        {
        return getDirectQuery() != null;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Named query mappings.
     */
    protected Map<String, NamedQuery> m_mapNamedQueries = new HashMap<>();
    }
