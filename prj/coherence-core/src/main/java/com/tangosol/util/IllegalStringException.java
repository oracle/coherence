/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* A IllegalStringException is thrown when there is a name collision.
*
* @author  Cameron Purdy
* @version 1.00, 12/03/97
*/
public class IllegalStringException
        extends Exception
    {
    /**
    * Constructs a IllegalStringException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public IllegalStringException()
        {
        super();
        }

    /**
    * Constructs a IllegalStringException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    * @param s the String that contains a detailed message
    */
    public IllegalStringException(String s)
        {
        super(s);
        }
    }
