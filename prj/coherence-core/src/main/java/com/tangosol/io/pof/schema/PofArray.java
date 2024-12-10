/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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
     * @param elementType      the type of array elements
     */
    public PofArray(String elementType)
        {
        this(elementType, false);
        }

    /**
     * Construct {@code PofArray} instance.
     *
     * @param elementType      the type of array elements
     * @param fUseRawEncoding  the flag specifying whether to use raw array encoding
     *
     * @since 24.09
     */
    public PofArray(String elementType, boolean fUseRawEncoding)
        {
        m_elementType     = elementType;
        m_fUseRawEncoding = fUseRawEncoding;
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

    /**
     * Return the flag specifying whether to use raw array encoding.
     *
     * @return the flag specifying whether to use raw array encoding
     *
     * @since 24.09
     */
    public boolean isUseRawEncoding()
        {
        return m_fUseRawEncoding;
        }

    /**
     * Set the flag specifying whether to use raw array encoding.
     *
     * @param fUseRawEncoding  the flag specifying whether to use raw array encoding
     *
     * @since 24.09
     */
    public void setUseRawEncoding(boolean fUseRawEncoding)
        {
        m_fUseRawEncoding = fUseRawEncoding;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The type of array elements.
     */
    private String m_elementType = Object.class.getName();

    /**
     * The flag specifying whether to use raw array encoding.
     *
     * @since 24.09
     */
    private boolean m_fUseRawEncoding = false;
    }
