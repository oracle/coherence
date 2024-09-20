/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib.exception;

/**
 * Exception thrown when the max number of elements into a vector space is
 * reached. This value is set during the vector space initialization.
 */
public class ItemCannotBeInsertedIntoTheVectorSpaceException
        extends UnexpectedNativeException
    {
    }
