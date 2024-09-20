/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib.exception;

/**
 * Exception raised when the method initialize() of an index has been called
 * more than once.
 */
public class IndexAlreadyInitializedException
        extends UnexpectedNativeException
    {
    }
