/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib.exception;

/**
 * Exception thrown when the index reference is not initialized on the native
 * side. (the method initialize() is not called after the object instantiation)
 */
public class IndexNotInitializedException
        extends UnexpectedNativeException
    {
    }
