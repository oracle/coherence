/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

import com.tangosol.coherence.rest.query.QueryEngine;

/**
 * Holder for {@link QueryEngine} configuration.
 *
 * @author ic  2011/11/25
 */
public class QueryEngineConfig {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a <tt>QueryEngineConfig</tt>.
     *
     * @param sName  the query engine name
     * @param clz    the query engine implementation class
     */
    public QueryEngineConfig(String sName, Class clz)
        {
        if (QueryEngine.class.isAssignableFrom(clz))
            {
            m_sName = sName;
            m_clz   = clz;
            }
        else
            {
            throw new IllegalArgumentException("class \"" + clz.getName()
                    + "\" does not implement the QueryEngine interface");
            }
        }

    //----- accessors -------------------------------------------------------

    /**
     * Determine the name of the query engine.
     *
     * @return the query engine name
     */
    public String getQueryEngineName()
        {
        return m_sName;
        }

    /**
     * Determine the class of the query engine.
     *
     * @return the query engine class
     */
    public Class getQueryEngineClass()
        {
        return m_clz;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Query engine name.
     */
    private final String m_sName;

    /**
     * Query engine class.
     */
    private final Class m_clz;
}
