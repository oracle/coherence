/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.pof;

import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.util.Base;

/**
 * Address is a place where a contact is located.
 * <p/>
 * The type implements PortableObject for efficient cross-platform
 * serialization..
 *
 * @author dag  2009.02.17
 * @author timmiddleton 2020.07.20
 */
@PortableType(id = 1003)
public class Address
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Default constructor (necessary for PortableObject implementation).
     */
    public Address()
        {
        }

    /**
     * Construct an Address.
     *
     * @param sStreet1  first line of the street address
     * @param sStreet2  second line of the street address
     * @param sCity     city name
     * @param sState    State name
     * @param sZip      Zip (postal) code
     * @param sCountry  country name
     */
    public Address(String sStreet1, String sStreet2, String sCity,
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

        Address that = (Address) oThat;
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

    // ----- data members ----------------------------------------------------

    /**
     * First line of street address.
     */
    private String m_sStreet1;

    /**
     * Second line of street address.
     */
    private String m_sStreet2;

    /**
     * City.
     */
    private String m_sCity;

    /**
     * State or Province.
     */
    private String m_sState;

    /**
     * Zip or other postal code.
     */
    private String m_sZip;

    /**
     * Country.
     */
    private String m_sCountry;
    }
