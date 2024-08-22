/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.testdata;

public abstract class PortableTypeTestBase
{
    private int m_nId;
    private String m_sString;

    public PortableTypeTestBase()
    {
    }

    public PortableTypeTestBase(int nId, String sString)
    {
        m_nId     = nId;
        m_sString = sString;
    }

    public int getId()
    {
        return m_nId;
    }

    public void setId(int nId)
    {
        this.m_nId = nId;
    }

    public String getString()
    {
        return m_sString;
    }

    public void setString(String sString)
    {
        this.m_sString = sString;
    }
}
