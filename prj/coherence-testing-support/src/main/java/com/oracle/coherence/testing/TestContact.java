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

import java.io.IOException;

import java.lang.Comparable;


/**
 * TestContact represents information needed to contact a person.
 *
 * The type implements PortableObject for efficient cross-platform
 * serialization..
 *
 * @since Coherence 3.7.1.10
 *
 * @author dag  2009.02.17
 */
public class TestContact
        implements PortableObject, Comparable<TestContact>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for PortableObject implementation).
     */
    public TestContact()
        {
        }

    /**
     * Construct TestContact
     *
     * @param sFirstName      the first name
     * @param sLastName       the last name
     * @param addrHome        the home address
     */
    public TestContact(String sFirstName, String sLastName, ExampleAddress addrHome)
        {
        m_sFirstName     = sFirstName;
        m_sLastName      = sLastName;
        m_addrHome       = addrHome;
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Return the first name.
     *
     * @return the first name
     */
    public String getFirstName()
        {
        return m_sFirstName;
        }

    /**
     * Set the first name.
     *
     * @param sFirstName  the first name
     */
    public void setFirstName(String sFirstName)
        {
        m_sFirstName = sFirstName;
        }

    /**
     * Return the last name.
     *
     * @return the last name
     */
    public String getLastName()
        {
        return m_sLastName;
        }

    /**
     * Set the last name.
     *
     * @param sLastName  the last name
     */
    public void setLastName(String sLastName)
        {
        m_sLastName = sLastName;
        }

   /**
     * Return the home address.
     *
     * @return the home address
     */
    public ExampleAddress getHomeAddress()
        {
        return m_addrHome;
        }

    /**
     * Set the home address.
     *
     * @param addrHome  the home address
     */
    public void setHomeAddress(ExampleAddress addrHome)
        {
        m_addrHome = addrHome;
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sFirstName     = reader.readString(FIRSTNAME);
        m_sLastName      = reader.readString(LASTNAME);
        m_addrHome       = (ExampleAddress) reader.readObject(HOME_ADDRESS);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(FIRSTNAME, m_sFirstName);
        writer.writeString(LASTNAME, m_sLastName);
        writer.writeObject(HOME_ADDRESS, m_addrHome);
        }


    // ----- Comparable interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public int compareTo(TestContact obj)
        {
        int retValue = 0;
        if (obj == null)
            {
            return 1;
            }


        if (this.getFirstName() == null)
            {
            retValue = (obj.getFirstName() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getFirstName() == null)
                {
                    return 1;
                }
                else {
                    retValue = this.getFirstName().compareTo(obj.getFirstName());
                }
            }
        if (retValue != 0)
            {
            return retValue;
            }


        if (this.getLastName() == null)
            {
            retValue = (obj.getLastName() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getLastName() == null)
                {
                    return 1;
                }
                else {
                    retValue = this.getLastName().compareTo(obj.getLastName());
                }
            }
        if (retValue != 0)
            {
            return retValue;
            }

        if (this.getHomeAddress() == null)
            {
            return (obj.getHomeAddress() == null ? 0 : -1);
            }
        else 
            {
            if (obj.getHomeAddress() == null)
                {
                    return 1;
                }
            }
        return this.getHomeAddress().compareTo(obj.getHomeAddress());

        }


    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        StringBuffer sb = new StringBuffer(getFirstName())
                .append(" ")
                .append(getLastName())
                .append("\nAddresses")
                .append("\nHome: ").append(getHomeAddress());
        return sb.toString();
        }


    // ----- constants -------------------------------------------------------

    /**
     * The POF index for the FirstName property
     */
    public static final int FIRSTNAME = 0;

    /**
     * The POF index for the LastName property
     */
    public static final int LASTNAME = 1;

    /**
     * The POF index for the HomeAddress property
     */
    public static final int HOME_ADDRESS = 2;

    // ----- data members ---------------------------------------------------

    /**
     * First name.
     */
    private String m_sFirstName;

    /**
     * Last name.
     */
    private String m_sLastName;

    /**
     * Home address.
     */
    private ExampleAddress m_addrHome;
    }
