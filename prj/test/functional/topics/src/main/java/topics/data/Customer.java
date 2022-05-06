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
public class Customer implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public Customer()
        {
        }

    public Customer(String sName, int nId, Address address)
        {
        m_sName   = sName;
        m_nId     = nId;
        m_address = address;
        }

    // ----- Customer methods -----------------------------------------------

    public void setName(String sName)
        {
        m_sName = sName;
        }

    public String getName()
        {
        return m_sName;
        }

    public void setId(int nId)
        {
        m_nId = nId;
        }

    public int getId()
        {
        return m_nId;
        }

    public void setAddress(Address address)
        {
        m_address = address;
        }

    public Address getAddress()
        {
        return m_address;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (! (o instanceof Customer))
            {
            return false;
            }
        Customer other = (Customer) o;
        return getId() == other.getId() && getName().compareTo(other.getName()) == 0 &&
            getAddress().equals(other.getAddress());
        }

    @Override
    public int hashCode()
        {
        return getId() + getName().hashCode() + getAddress().hashCode();
        }

    @Override
    public String toString()
        {
        return "Customer[name=" + m_sName + ", id=" + m_nId + " address=" + getAddress() + "]";
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -1L;

    // ----- data -----------------------------------------------------------

    private String  m_sName;
    private int     m_nId;
    private Address m_address;
    }
