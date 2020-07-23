/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.rest.model;

import java.io.Serializable;

/**
 * An abstract implementation of a standard ReferenceCode class that must be
 * extended to implement other reference codes such as Country and State.
 *
 * @author tam  2015.07.21
 * @since 12.2.1
 */
public abstract class ReferenceCode
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for Reference Codes.
     *
     * @param sCode the code to identify the entry
     * @param sName the descriptive name of the entry
     */
    public ReferenceCode(String sCode, String sName)
        {
        m_sCode = sCode;
        m_sName = sName;
        }

    /**
     * Default no-args constructor
     */
    public ReferenceCode()
        {
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the code.
     *
     * @return the code.
     */
    public String getCode()
        {
        return m_sCode;
        }

    /**
     * Set the code.
     *
     * @param sCode the code
     */
    public void setCode(String sCode)
        {
        m_sCode = sCode;
        }

    /**
     * Return the name.
     *
     * @return the name
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Set the name.
     *
     * @param sName the name.
     */
    public void setName(String sName)
        {
        m_sName = sName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The code to identify a reference code entry.
     */
    private String m_sCode;

    /**
     * The descriptive name of the entry.
     */
    private String m_sName;
    }
