/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

/**
 * Query execution specific exception.
 *
 * @author ic  2011.12.04
 */
public class QueryException
        extends RuntimeException
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Construct an instance of <tt>QueryException</tt>.
     *
     * @param sMessage  detail message
     */
    public QueryException(String sMessage)
        {
        super(sMessage);
        }

    /**
     * Construct an instance of <tt>QueryException</tt>.
     *
     * @param sMessage  detail message
     * @param cause     the cause
     */
    public QueryException(String sMessage, Throwable cause)
        {
        super(sMessage, cause);
        }

    /**
     * Construct an instance of <tt>QueryException</tt>.
     *
     * @param cause  the cause
     */
    public QueryException(Throwable cause)
        {
        super(cause);
        }
    }
