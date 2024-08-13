/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib.exception;

/**
 * Exception that represents that results could not be returned by the query.
 */
public class QueryCannotReturnResultsException
        extends UnexpectedNativeException
    {

    private static final String MESSAGE = "Probably ef or M is too small";

    public QueryCannotReturnResultsException()
        {
        super(MESSAGE);
        }

    }
