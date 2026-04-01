/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.archiver.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;

import java.io.IOException;

/**
 * ContactId represents a key to the contact for whom information is stored in
 * the cache.
 * <p>
 * The type implements PortableObject for efficient cross-platform
 * serialization..
 *
 * @author si, tm 2026.02.17
 * @since  15.1.2
 */
public class ContactId
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for PortableObject implementation).
     */
    public ContactId()
        {
        }

    /**
     * Construct a contact key.
     *
     * @param sFirstName  first name
     * @param sLastName   last name
     */
    public ContactId(String sFirstName, String sLastName)
        {
        m_sFirstName = sFirstName;
        m_sLastName  = sLastName;
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
     * Return the last name.
     *
     * @return the last name
     */
    public String getLastName()
        {
        return m_sLastName;
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sFirstName = reader.readString(FIRSTNAME);
        m_sLastName = reader.readString(LASTNAME);
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(FIRSTNAME, m_sFirstName);
        writer.writeString(LASTNAME, m_sLastName);
        }

    // ----- Object methods -------------------------------------------------

    @Override
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

        ContactId that = (ContactId) oThat;
        return Base.equals(getFirstName(), that.getFirstName()) &&
               Base.equals(getLastName(),  that.getLastName());
        }

    @Override
    public int hashCode()
        {
        return (getFirstName() == null ? 0 : getFirstName().hashCode()) ^
                (getLastName() == null ? 0 : getLastName().hashCode());

        }

    @Override
    public String toString()
        {
        return getFirstName() + " " + getLastName();
        }

    // ----- constants -------------------------------------------------------

    /**
     * The POF index for the FirstName property.
     */
    public static final int FIRSTNAME = 0;

    /**
     * The POF index for the LastName property.
     */
    public static final int LASTNAME = 1;

    // ----- data members ---------------------------------------------------

    /**
     * First name.
     */
    private String m_sFirstName;

    /**
     * Last name.
     */
    private String m_sLastName;
    }
