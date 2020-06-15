/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;

/**
 * An OPToken representing the end of a CohQL statement.
 *
 * @author jk 2014.08.07
 */
public class EndOfStatementOPToken
        extends PunctuationOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new EndOfStatementOPToken
     */
    protected EndOfStatementOPToken()
        {
        super(";");
        }

    // ----- constants ------------------------------------------------------

    public static EndOfStatementOPToken INSTANCE = new EndOfStatementOPToken();

    }
