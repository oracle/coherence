/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* An AssertionException is thrown when an assertion fails.
*
* @author  Cameron Purdy
* @version 1.00, 04/21/99
*/
public class AssertionException
        extends RuntimeException
    {
    /**
    * Constructs a AssertionException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public AssertionException()
        {
        super();
        }

    /**
    * Constructs a AssertionException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    * @param s the String that contains a detailed message
    */
    public AssertionException(String s)
        {
        super(s);
        }
    }
