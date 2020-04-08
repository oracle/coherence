/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.component;


/**
* The IntegrationException exception is thrown when a process of integration
* cannot be completed normally.  This is a severe exception that usually
* indicates a non-recoverable (quite likely environment configuration) error
*
* @version 1.00, 08/24/98
* @author 	Gene Gleyzer
*/
public class IntegrationException extends RuntimeException
    {
    /**
    * Constructs a IntegrationException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public IntegrationException()
        {
        super();
        }

    /**
    * Constructs a IntegrationException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    *
    * @param s the String that contains a detailed message
    */
    public IntegrationException(String s)
        {
        super(s);
        }
    }
