/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.java.type;


/**
* The unknown type.
*
* @author cp  2001.04.30
*/
public class UnknownType
        extends Type
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct a unknown type object.
    */
    UnknownType()
        {
        super("U");
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the number of stack words used to load this type.
    *
    * @return the number of words used on the stack to hold this type
    */
    public int getWordCount()
        {
        throw new UnsupportedOperationException();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the type into its source code form.
    *
    * @return a source code representation of the type
    */
    public String toString()
        {
        return "unknown";
        }
    }