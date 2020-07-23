/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.rest.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Sample Department class used for REST examples.
 *
 * @author  tam 2015.07.06
 * @since 12.2.1
 */
@XmlRootElement(name="department")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Department
        implements Serializable
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Department()
        {
        }

    /**
     * Create a new department.
     *
     * @param sDeptCode  department code
     * @param sName      name of the department
     */
    public Department(String sDeptCode, String sName)
        {
        m_sDeptCode = sDeptCode;
        m_sName     = sName;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the department code.
     *
     * @return  the department code
     */
    public String getDeptCode()
        {
        return m_sDeptCode;
        }

    /**
     * Set the department code.
     *
     * @param sDeptCode  the department code
     */
    public void setDeptCode(String sDeptCode)
        {
        m_sDeptCode = sDeptCode;
        }

    /**
     * Set the department name.
     *
     * @param sName  the department name
     */
    public void setName(String sName)
        {
        m_sName = sName;
        }

    /**
     * Return the department name.
     *
     * @return  the department name
     */
    public String getName()
        {
        return m_sName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Department code.
     */
    private String m_sDeptCode;

    /**
     * Department name.
     */
    private String m_sName;
    }
