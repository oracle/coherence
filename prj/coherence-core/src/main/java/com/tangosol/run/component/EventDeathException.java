/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.component;


/**
* The EventDeathException exception is used to prevent
* delivering an event to the registered listeners
*
* @version 1.00, 09/03/98
* @author 	Gene Gleyzer
*/
public class EventDeathException extends RuntimeException
    {
    /**
    * Constructs a EventDeathException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public EventDeathException()
        {
        super();
        }

    /**
    * Constructs a EventDeathException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    *
    * @param s the String that contains a detailed message
    */
    public EventDeathException(String s)
        {
        super(s);
        }
    }
