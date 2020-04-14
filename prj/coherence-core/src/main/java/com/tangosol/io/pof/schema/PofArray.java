/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

/**
 * Schema representation of POF array.
 *
 * @author as  2013.11.18
 */
public class PofArray
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PofArray} instance.
     */
    public PofArray()
        {
        }

    /**
     * Construct {@code PofArray} instance.
     *
     * @param elementType  the type of array elements
     */
    public PofArray(String elementType)
        {
        m_elementType = elementType;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the type of array elements.
     *
     * @return the type of array elements
     */
    public String getElementClass()
        {
        return m_elementType;
        }

    /**
     * Set the type of array elements.
     *
     * @param elementType  the type of array elements
     */
    public void setElementClass(String elementType)
        {
        m_elementType = elementType;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The type of array elements.
     */
    private String m_elementType = Object.class.getName();
    }
