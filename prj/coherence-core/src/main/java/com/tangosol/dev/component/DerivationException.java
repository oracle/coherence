/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


/**
* A derivation exception is thrown when an error occurs resolving or
* extracting a derivation or modification.
*
* @author  Cameron Purdy
* @version 1.00, 12/01/97
*/
public class DerivationException
        extends ComponentException
    {
    /**
    * Constructs a DerivationException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public DerivationException()
        {
        super();
        }

    /**
    * Constructs a DerivationException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    * @param s the String that contains a detailed message
    */
    public DerivationException(String s)
        {
        super(s);
        }
    }
