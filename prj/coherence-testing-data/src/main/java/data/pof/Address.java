/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import java.io.IOException;
import java.io.Serializable;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;


@XmlAccessorType(XmlAccessType.PROPERTY)
public class Address
        extends Base
        implements PortableObject, Serializable
    {
    public Address()
        {
        }

    public Address(String sStreet, String sCity, String sState, String sZip)
        {
        m_sStreet = sStreet;
        m_sCity   = sCity;
        m_sState  = sState;
        m_sZip    = sZip;
        }

    public String getCity()
        {
        return m_sCity;
        }

    public void setCity(String sCity)
        {
        m_sCity = sCity;
        }

    public String getStreet()
        {
        return m_sStreet;
        }

    public void setStreet(String sStreet)
        {
        m_sStreet = sStreet;
        }

    public String getState()
        {
        return m_sState;
        }

    public void setState(String sState)
        {
        m_sState = sState;
        }

    public String getZip()
        {
        return m_sZip;
        }

    public void setZip(String sZip)
        {
        m_sZip = sZip;
        }

    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sStreet = reader.readString(STREET);
        m_sCity   = reader.readString(CITY);
        m_sState  = reader.readString(STATE);
        m_sZip    = reader.readString(ZIP);
        }

    public String toString()
        {
        return "Address {m_sCity="
               + m_sCity + ", m_sState=" + m_sState + ", m_sStreet="
               + m_sStreet + ", m_sZip=" + m_sZip + "}";
        }

    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(STREET, m_sStreet);
        writer.writeString(CITY, m_sCity);
        writer.writeString(STATE, m_sState);
        writer.writeString(ZIP, m_sZip);
        }

    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }

        if (o instanceof Address)
            {
            Address that = (Address) o;
            return equals(this.m_sStreet, that.m_sStreet) &&
                   equals(this.m_sCity,   that.m_sCity)   &&
                   equals(this.m_sState,  that.m_sState)  &&
                   equals(this.m_sZip,    that.m_sZip);
            }

        return false;
        }

    public String m_sStreet;
    public String m_sCity;
    public String m_sState;
    public String m_sZip;

    public static final int STREET = 0;
    public static final int CITY   = 1;
    public static final int STATE  = 2;
    public static final int ZIP    = 3;
    }