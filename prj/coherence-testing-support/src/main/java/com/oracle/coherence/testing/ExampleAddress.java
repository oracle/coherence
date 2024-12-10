/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;

import java.io.IOException;

/**
* ExampleAddress is a place where a contact is located.
* <p/>
* The type implements PortableObject for efficient cross-platform
* serialization.
*
* @since Coherence 3.7.1.10
*
* @author dag  2009.02.17
*/
public class ExampleAddress
        implements PortableObject, Comparable<ExampleAddress>
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Default constructor (necessary for PortableObject implementation).
    */
    public ExampleAddress()
        {
        }

    /**
    * Construct an ExampleAddress.
    *
    * @param sStreet1  first line of the street address
    * @param sStreet2  second line of the street address
    * @param sCity     city name
    * @param sState    State name
    * @param sZip      Zip (postal) code
    * @param sCountry  country name
    */
    public ExampleAddress(String sStreet1, String sStreet2, String sCity,
            String sState, String sZip, String sCountry)
        {
        m_sStreet1 = sStreet1;
        m_sStreet2 = sStreet2;
        m_sCity    = sCity;
        m_sState   = sState;
        m_sZip     = sZip;
        m_sCountry = sCountry;
        }

    // ----- accessors -------------------------------------------------------

    /**
    * Return the first line of the street address.
    *
    * @return the first line of the street address
    */
    public String getStreet1()
        {
        return m_sStreet1;
        }

    /**
    * Set the first line of the street address.
    *
    * @param sStreet1 the first line of the street address
    */
    public void setStreet1(String sStreet1)
        {
        m_sStreet1 = sStreet1;
        }

    /**
    * Return the second line of the street address.
    *
    * @return the second line of the street address
    */
    public String getStreet2()
        {
        return m_sStreet2;
        }

    /**
    * Set the second line of the street address.
    *
    * @param sStreet2 the second line of the street address
    */
    public void setStreet2(String sStreet2)
        {
        m_sStreet2 = sStreet2;
        }

    /**
    * Return the city name.
    *
    * @return the city name
    */
    public String getCity()
        {
        return m_sCity;
        }

    /**
    * Set the city name.
    *
    * @param sCity the city name
    */
    public void setCity(String sCity)
        {
        m_sCity = sCity;
        }

    /**
    * Return the state or territory name.
    *
    * @return the state or territory name
    */
    public String getState()
        {
        return m_sState;
        }

    /**
    * Set the State or Province name.
    *
    * @param sState the State or Province name
    */
    public void setState(String sState)
        {
        m_sState = sState;
        }

    /**
    * Return the Zip code.
    *
    * @return the Zip code
    */
    public String getZipCode()
        {
        return m_sZip;
        }

    /**
    * Set the Zip code.
    *
    * @param sZip the Zip code
    */
    public void setZipCode(String sZip)
        {
        m_sZip = sZip;
        }

    /**
    * Return the Country name.
    *
    * @return the Country name
    */
    public String getCountry()
        {
        return m_sCountry;
        }

    /**
    * Set the country name.
    *
    * @param sCountry the country name
    */
    public void setCountry(String sCountry)
        {
        m_sCountry = sCountry;
        }

    // ----- PortableObject interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sStreet1 = reader.readString(STREET_1);
        m_sStreet2 = reader.readString(STREET_2);
        m_sCity    = reader.readString(CITY);
        m_sState   = reader.readString(STATE);
        m_sZip     = reader.readString(ZIP);
        m_sCountry = reader.readString(COUNTRY);
        }


    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(STREET_1, m_sStreet1);
        writer.writeString(STREET_2, m_sStreet2);
        writer.writeString(CITY,     m_sCity);
        writer.writeString(STATE,    m_sState);
        writer.writeString(ZIP,      m_sZip);
        writer.writeString(COUNTRY,  m_sCountry);
        }

    // ----- Comparable interface ---------------------------------------

    public int compareTo(ExampleAddress obj)
        {
        int retValue = 0;
        if (obj == null)
            {
            return 1;
            }


        if (this.getStreet1() == null)
            {
            retValue = (obj.getStreet1() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getStreet1() == null)
                {
                    return 1;
                }
                else {
                    retValue = this.getStreet1().compareTo(obj.getStreet1());
                }
            }
        if (retValue != 0)
            {
            return retValue;
            }


        if (this.getStreet2() == null)
            {
            retValue = (obj.getStreet2() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getStreet2() == null)
                {
                    return 1;
                }
                else {
                    retValue = this.getStreet2().compareTo(obj.getStreet2());
                }
            }
        if (retValue != 0)
            {
            return retValue;
            }

        if (this.getCity() == null)
            {
            return (obj.getCity() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getCity() == null)
                {
                    return 1;
                }
                else {
                    retValue = this.getCity().compareTo(obj.getCity());
                }
            }
        if (retValue != 0)
            {
            return retValue;
            }

        if (this.getState() == null)
            {
            return (obj.getState() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getState() == null)
                {
                    return 1;
                }
                else {
                    retValue = this.getState().compareTo(obj.getState());
                }
            }
        if (retValue != 0)
            {
            return retValue;
            }

        if (this.getZipCode() == null)
            {
            return (obj.getZipCode() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getZipCode() == null)
                {
                    return 1;
                }
                else {
                    retValue = this.getZipCode().compareTo(obj.getZipCode());
                }
            }
        if (retValue != 0)
            {
            return retValue;
            }

        if (this.getCountry() == null)
            {
            return (obj.getCountry() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getCountry() == null)
                {
                    return 1;
                }
            }
        return this.getCountry().compareTo(obj.getCountry());

        }

    // ----- Object methods --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object oThat)
        {
        if (this == oThat)
            {
            return true;
            }
        if (oThat == null)
            {
            return false;
            }

        ExampleAddress that = (ExampleAddress) oThat;
        return Base.equals(getStreet1(), that.getStreet1()) &&
               Base.equals(getStreet2(), that.getStreet2()) &&
               Base.equals(getCity(),    that.getCity())    &&
               Base.equals(getState(),   that.getState())   &&
               Base.equals(getZipCode(), that.getZipCode()) &&
               Base.equals(getCountry(), that.getCountry());
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        return (getStreet1() == null ? 0 : getStreet1().hashCode()) ^
               (getStreet2() == null ? 0 : getStreet2().hashCode()) ^
               (getZipCode() == null ? 0 : getZipCode().hashCode());
        }

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return  getStreet1() + "\n" +
                getStreet2() + "\n" +
                getCity() + ", " + getState() + " "  + getZipCode() + "\n" +
                getCountry();
        }

    // ----- constants -------------------------------------------------------

    /**
    * The POF index for the Street1 property
    */
    public static final int STREET_1 = 0;

    /**
    * The POF index for the Street2 property
    */
    public static final int STREET_2 = 1;

    /**
    * The POF index for the City property
    */
    public static final int CITY = 2;

    /**
    * The POF index for the State property
    */
    public static final int STATE = 3;

    /**
    * The POF index for the Zip property
    */
    public static final int ZIP = 4;

    /**
    * The POF index for the Country property
    */
    public static final int COUNTRY = 5;


    // ----- data members ----------------------------------------------------

    /**
    * First line of street address
    */
    private String m_sStreet1;

    /**
    * Second line of street address
    */
    private String m_sStreet2;

    /**
    * City.
    */
    private String m_sCity;

    /**
    * State or Province
    */
    private String m_sState;

    /**
    * Zip or other postal code
    */
    private String m_sZip;

    /**
    * Country
    */
    private String m_sCountry;
    }
