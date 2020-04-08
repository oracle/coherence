/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;

import java.io.IOException;
import java.util.Collection;


/**
* Sample value class.
*
* @author dag  2009.02.17
*/
public class ComplexTestValue
        implements PortableObject
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Default constructor (necessary for PortableObject implementation).
    */
    public ComplexTestValue()
        {
        this("a3",null , "Burlington", "TexasFloridama", "asdfgghjklzxcvbnmqwertyu", "poiuytrewqasdfghjklmnbvcxzasdfghtrewqyuioplk");
        }

    /**
    * Construct an Address.
    *
    * @param sStreet1  first line of the street address
    * @param sAddressCollection  second line of collection of street addresses
    * @param sCity     city name
    * @param sState    state name
    * @param sZip      zip (postal) code
    * @param sCountry  country name
    */
    public ComplexTestValue(String sStreet1, Collection sAddressCollection, String sCity,
            String sState, String sZip, String sCountry)
        {
       /* Collection col = new ArrayList();
        col.add(new TestValue());
        if(sAddressCollection == null) sAddressCollection = col;*/
        m_sStreet1 = sStreet1;
        m_sAddressCollection = sAddressCollection;
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
     //   return "'"+m_sStreet1+"'";
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
     * @return the m_sAddressCollection
     */
    public Collection getAddressCollection() {
        return m_sAddressCollection;
    }

    /**
     * @param m_sAddressCollection the m_sAddressCollection to set
     */
    public void setAddressCollection(Collection m_sAddressCollection) {
        this.m_sAddressCollection = m_sAddressCollection;
    }

    /**
    * Return the city name.
    *
    * @return the city name
    */
    public String getCity()
        {
        //   return "'"+m_sCity+"'";
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
        //   return "'"+m_sState+"'";
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
        //   return "'"+m_sZip+"'";
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
        //   return "'"+m_sCountry+"'";
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
        m_sAddressCollection = reader.readCollection(ADDRESS_COLLECTION, m_sAddressCollection);
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
        writer.writeCollection(ADDRESS_COLLECTION, m_sAddressCollection);
        writer.writeString(CITY,     m_sCity);
        writer.writeString(STATE,    m_sState);
        writer.writeString(ZIP,      m_sZip);
        writer.writeString(COUNTRY,  m_sCountry);
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

        ComplexTestValue that = (ComplexTestValue) oThat;
        return Base.equals(getStreet1(), that.getStreet1()) &&
               Base.equals(getAddressCollection(), that.getAddressCollection()) &&
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
               (getAddressCollection() == null ? 0 : getAddressCollection().hashCode()) ^
               (getZipCode() == null ? 0 : getZipCode().hashCode());
        }

    /**
    * {@inheritDoc}
    */
   public String toString()
        {
        return  getStreet1() + "\n" +
                getAddressCollection() + "\n" +
                getCity() + "\n" + getState() + "\n"  + getZipCode() + "\n" +
                getCountry();
        }


    // ----- constants -------------------------------------------------------

    /**
    * The POF index for the Street1 property
    */
    public static final int STREET_1 = 0;

    /**
    * The POF index for the AddressCollection property
    */
    public static final int ADDRESS_COLLECTION = 1;

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
    private Collection m_sAddressCollection;

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
