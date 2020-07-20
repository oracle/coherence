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
 * ContactId represents a key to the contact for whom information is stored in
 * the cache.
 * <p/>
 * The type implements PortableObject for efficient cross-platform
 * serialization..
 *
 * @author dag  2009.02.18
 * @author timmiddleton 2020.07.20
 */
@PortableType(id = 1001)
public class ContactId
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

    // ----- Object methods -------------------------------------------------

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

        ContactId that = (ContactId) oThat;
        return Base.equals(getFirstName(), that.getFirstName()) &&
               Base.equals(getLastName(),  that.getLastName());
        }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
        {
        return (getFirstName() == null ? 0 : getFirstName().hashCode()) ^
                (getLastName() == null ? 0 : getLastName().hashCode());

        }

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return getFirstName() + " " + getLastName();
        }


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
