/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.persistence;


import com.tangosol.util.Base;

import java.io.Serializable;


public class PersonId
        implements Serializable
    {
    public PersonId() {}

    public PersonId(int nId, String sId)
        {
        this.m_nId = nId;
        this.m_sId = sId;
        }

    public int getId()
        {
        return m_nId;
        }

    public String getIdString()
        {
        return m_sId;
        }

    public boolean equals(Object o)
        {
        if (o instanceof PersonId)
            {
            PersonId that = (PersonId) o;
            return this.m_nId == that.m_nId &&
                   Base.equals(this.m_sId, that.m_sId);
            }

        return false;
        }

    public int hashCode()
        {
        return m_nId;
        }

    public String toString()
        {
        return "PersonId(" + m_nId + ", " + m_sId + ")";
        }

    int    m_nId;
    String m_sId;
    }