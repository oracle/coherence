/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.persistence;


import java.io.Serializable;


public class CompoundPerson2
        implements Serializable
    {
    public CompoundPerson2() {}

    public CompoundPerson2(int nId, String sId, String sName)
        {
        this.m_pid   = new PersonId(nId, sId);
        this.m_sName = sName;
        }

    public PersonId getPid()
        {
        return m_pid;
        }

    public int getId()
        {
        return m_pid.getId();
        }

    public String getIdString()
        {
        return m_pid.getIdString();
        }

    public String toString()
        {
        return "CompoundPerson2(" + m_pid + ", " + m_sName + ")";
        }

    PersonId m_pid;
    String   m_sName;
    }