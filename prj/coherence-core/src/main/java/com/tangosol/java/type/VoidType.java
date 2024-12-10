/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.java.type;


/**
* The void type.
*
* @author cp  2000.10.13
*/
public class VoidType
        extends Type
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct a void type object.
    */
    VoidType()
        {
        super("V");
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the type into its source code form.
    *
    * @return a source code representation of the type
    */
    public String toString()
        {
        return "void";
        }
    }