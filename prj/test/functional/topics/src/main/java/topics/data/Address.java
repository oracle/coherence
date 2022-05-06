/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics.data;

import java.io.Serializable;

/**
 * Non POF class to test sending non POF to topic.
 */
public class Address implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public Address()
        {
        }

    public Address(String street, String city, String state, int zipcode)
        {
        m_sStreet  = street;
        m_sCity    = city;
        m_sState   = state;
        m_nZipcode = zipcode;
        }

    public Address(Address address)
        {
        this(address.m_sStreet, address.m_sCity, address.m_sState, address.m_nZipcode);
        }

    // ----- Address methods ------------------------------------------------

    public String getStreet()
        {
        return m_sStreet;
        }

    public void setStreet(String street)
        {
        m_sStreet = street;
        }

    public String getCity()
        {
        return m_sCity;
        }

    public void setCity(String city)
        {
        m_sCity = city;
        }

    public String getState()
        {
        return m_sState;
        }

    public void setState(String state)
        {
        m_sState = state;
        }

    public int getZipcode()
        {
        return m_nZipcode;
        }

    public void setZipcode(int zipcode)
        {
        m_nZipcode = zipcode;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (! (o instanceof Address))
            {
            return false;
            }
        Address other = (Address) o;
        return getStreet().compareTo(other.getStreet()) == 0 &&
            getCity().compareTo(other.getCity()) == 0 &&
            getState().compareTo(other.getState()) == 0 &&
            getZipcode() == other.getZipcode();
        }

    @Override
    public String toString()
        {
        return "Address[street" + m_sStreet + ", city=" + m_sCity + " state=" + m_sState + " zipcode=" + m_nZipcode +  "]";
        }

    // ----- constants ------------------------------------------------------

    static final public Address[] arrAddress =
    {
        new Address("101 Main St", "Boston", "MA", 12340),
        new Address("105 Washingon St", "Cambridge", "MA", 14340),
        new Address("211 Broadway", "Chicago", "IL", 24340),
        new Address("311 First St", "San Francisco", "CA", 11111),
        new Address("311 Fifth St", "Hollywood", "CA", 55555)
    };

    // ----- data members ---------------------------------------------------

    private String m_sStreet;

    private String m_sCity;

    private String m_sState;

    private int    m_nZipcode;
    }
