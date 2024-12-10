/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

import java.util.Map;

/**
 * The QueryEngine interface provides a pluggable mechanism for producing
 * queries that can be executed against a NamedCache.
 *
 * @author ic  2011.11.25
 *
 * @see CoherenceQueryLanguageEngine
 */
public interface QueryEngine
    {
    /**
     * Prepares a Query for execution by replacing any parameter placeholders
     * with the appropriate values from the parameter map.
     *
     * @param sQuery     query expression
     * @param mapParams  parameter values used to resolve parameters from
     *                   query expression
     *
     * @return a Query instance ready for execution
     */
    public Query prepareQuery(String sQuery, Map<String, Object> mapParams);

    // ---- constants -------------------------------------------------------

    /**
     * The name of the default (CohQL) query engine.
     */
    public static final String DEFAULT = "DEFAULT";
    }
