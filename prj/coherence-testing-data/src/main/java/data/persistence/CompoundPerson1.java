/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.persistence;


import java.io.Serializable;


public class CompoundPerson1
        implements Serializable
    {
    public CompoundPerson1() {}

    public CompoundPerson1(int nId, String sId, String sName)
        {
        this.m_nId   = nId;
        this.m_sId   = sId;
        this.m_sName = sName;
        }

    public int getId()
        {
        return m_nId;
        }

    public String getIdString()
        {
        return m_sId;
        }

    public String toString()
        {
        return "CompoundPerson1(" + m_nId + ", " + m_sId + ", " + m_sName + ")";
        }

    int    m_nId;
    String m_sId;
    String m_sName;
    }