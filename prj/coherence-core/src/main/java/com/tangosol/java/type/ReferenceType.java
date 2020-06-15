/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.java.type;


/**
* The base for all reference Type implementations.
*
* @author cp  2000.10.13
*/
public abstract class ReferenceType
        extends Type
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct a reference type object.
    */
    protected ReferenceType(String sSig)
        {
        super(sSig);
        }
    }