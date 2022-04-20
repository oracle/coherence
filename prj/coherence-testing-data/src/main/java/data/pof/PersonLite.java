/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.pof;

import com.tangosol.util.Base;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


@XmlAccessorType(XmlAccessType.PROPERTY)
public class PersonLite
        extends Base
    {
    public PersonLite()
        {
        }

    public PersonLite(String sName, Date dtDOB)
        {
        m_sName = sName;
        m_dtDOB = dtDOB;
        }

    public String getName()
        {
        return m_sName;
        }

    public void setName(String sName)
        {
        m_sName = sName;
        }

    @XmlJavaTypeAdapter(DateXmlAdapter.class)
    public Date getDateOfBirth()
        {
        return m_dtDOB;
        }

    public void setDateOfBirth(Date dtDOB)
        {
        m_dtDOB = dtDOB;
        }

    public String m_sName;
    public Date   m_dtDOB;

    public static final int NAME = 0;
    public static final int DOB  = 2;
    }