/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.java.type;


/**
* The array type implementation.
*
* @author cp  2000.10.13
*/
public class ArrayType
        extends ReferenceType
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct an array type object.
    *
    * @param typeElement  the type of the elements in the array
    */
    public ArrayType(Type typeElement)
        {
        super('[' + typeElement.getSignature());
        m_typeElement = typeElement;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the element type of the array.  For example, if the type is
    * an array of int, the element type is int.
    *
    * @return the element type
    */
    public Type getElementType()
        {
        return m_typeElement;
        }

    /**
    * Determine the innermost element type of the array.  For example, if the
    * type is an array of arrays  of arrays of arrays of int, the "innermost"
    * type is int.
    *
    * @return the innermost element type
    */
    public Type getInnermostElementType()
        {
        Type type = this;
        while (type instanceof ArrayType)
            {
            type = ((ArrayType) type).getElementType();
            }
        return type;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the type into its source code form.
    *
    * @return a source code representation of the type
    */
    public String toString()
        {
        return m_typeElement.toString() + "[]";
        }


    // ----- data members ---------------------------------------------------

    private Type m_typeElement;
    }