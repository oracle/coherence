/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


/**
* A ComponentException is thrown when an exception condition occurs that
* is related to a Component Definition or the Component Integration Model.
*
* @author  Cameron Purdy
* @version 1.00, 12/03/97
*/
public class ComponentException
        extends Exception
    {
    /**
    * Constructs a ComponentException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public ComponentException()
        {
        super();
        }

    /**
    * Constructs a ComponentException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    * @param s the String that contains a detailed message
    */
    public ComponentException(String s)
        {
        super(s);
        }
    }
