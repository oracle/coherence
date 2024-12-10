/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql.api;


/**
 * An {@link Exception} to indicate that a customer was not found.
 *
 * @author Tim Middleton 2021-01-25
 */
// tag::include[]
public class CustomerNotFoundException
        extends Exception {

    /**
     * Constructs a new exception to indicate that a customer was not found.
     *
     * @param message the detail message.
     */
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
// end::include[]
