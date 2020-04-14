/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;

/**
 * An OPToken representing the end of a token stream.
 *
 * @author jk 2014.08.07
 */
public class EndOPToken
        extends PunctuationOPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new EndOPToken
     */
    protected EndOPToken()
        {
        super("*end*");
        }

    // ----- constants ------------------------------------------------------

    public static EndOPToken INSTANCE = new EndOPToken();

    }
