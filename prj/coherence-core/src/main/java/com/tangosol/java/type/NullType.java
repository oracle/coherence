/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.java.type;


/**
* The null reference type implementation, used only by compilation.
*
* @author cp  2001.04.30
*/
public class NullType
        extends ReferenceType
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct a null type object.
    */
    NullType()
        {
        super("N");
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the type into its source code form.
    *
    * @return a source code representation of the type
    */
    public String toString()
        {
        return "null";
        }
    }