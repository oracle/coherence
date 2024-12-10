/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token;

import com.tangosol.coherence.dsltools.precedence.OPScanner;

/**
 * SQLTraceOPToken is used for parsing and specifying the AST
 * used for a trace statement.
 * <p>
 * Syntax:
 * <p>
 * TRACE select stmt | update stmt | delete stmt
 *
 * @author tb  2011.06.05
 */
public class SQLTraceOPToken
        extends SQLExplainOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SQLTraceOPToken with the given parameters.
     *
     * @param id string identifier for this token
     */
    public SQLTraceOPToken(String id)
        {
        super(id);
        }

    // ----- helper methods -------------------------------------------------

    protected void advanceToStmt(OPScanner s)
        {
        }

    protected String getFunctor()
        {
        return FUNCTOR;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The functor name used to represent this node in an AST
     */
    public static final String FUNCTOR = "sqlTraceNode";
    }
