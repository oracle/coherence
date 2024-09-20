/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib.exception;

/**
 * General exception for errors that happened on the native implementation.
 */
public class UnexpectedNativeException
        extends RuntimeException
    {
    /**
     * Construct {@code UnexpectedNativeException} instance.
     */
    public UnexpectedNativeException()
        {
        }

    /**
     * Construct {@code UnexpectedNativeException} instance.
     *
     * @param message  the error message
     */
    public UnexpectedNativeException(String message)
        {
        super(message);
        }
    }
