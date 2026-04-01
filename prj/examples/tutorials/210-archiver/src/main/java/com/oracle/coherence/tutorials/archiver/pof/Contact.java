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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Contact represents information needed to contact a person.
 * <p>
 * The type implements PortableObject for efficient cross-platform
 * serialization..
 *
 * @author si, tm 2026.02.17
 * @since  15.1.2
 */
@XmlRootElement(name="contact")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Contact
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for PortableObject implementation).
     */
    public Contact()
        {
        }

    /**
     * Construct Contact
     *
     * @param sFirstName      the first name
     * @param sLastName       the last name
     * @param addrHome        the home address
     * @param addrWork        the work address
     * @param mapPhoneNumber  map string number type (e.g. "work") to
     *                        PhoneNumber
     * @param dtBirth         date of birth
     */
    public Contact(String sFirstName, String sLastName, Address addrHome,
            Address addrWork, Map<String, PhoneNumber> mapPhoneNumber, LocalDate dtBirth)
        {
        m_sFirstName     = sFirstName;
        m_sLastName      = sLastName;
        m_addrHome       = addrHome;
        m_addrWork       = addrWork;
        m_mapPhoneNumber = mapPhoneNumber;
        m_dtBirth        = dtBirth;
        calculateAge();
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
    public Address getHomeAddress()
        {
        return m_addrHome;
        }

    /**
     * Set the home address.
     *
     * @param addrHome  the home address
     */
    public void setHomeAddress(Address addrHome)
        {
        m_addrHome = addrHome;
        }

    /**
     * Return the work address.
     *
     * @return the work address
     */
    public Address getWorkAddress()
        {
        return m_addrWork;
        }

    /**
     * Set the work address.
     *
     * @param addrWork  the work address
     */
    public void setWorkAddress(Address addrWork)
        {
        m_addrWork = addrWork;
        }

    /**
     * Get all phone numbers.
     *
     * @return a map of phone numbers
     */
    public Map<String, PhoneNumber> getPhoneNumbers()
        {
        return m_mapPhoneNumber;
        }

    /**
     * Set the list of phone numbers.
     *
     * @param mapTelNumber  a map of phone numbers
     */
    public void setPhoneNumbers(Map<String, PhoneNumber> mapTelNumber)
        {
        m_mapPhoneNumber = mapTelNumber;
        }

    /**
     * Get the date of birth.
     *
     * @return the date of birth
     */
    public LocalDate getBirthDate()
        {
        return m_dtBirth;
        }

    /**
     * Set the date of birth.
     *
     * @param dtBirth  the date of birth
     */
    public void setBirthDate(LocalDate dtBirth)
        {
        m_dtBirth = dtBirth;
        }

    /**
     * Get age.
     *
     * @return age
     */
    public int getAge()
        {
        return m_nAge;
        }

    /**
     * set the age.
     *
     * @param nAge  the age
     */
    public void setAge(int nAge)
        {
        m_nAge = nAge;
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sFirstName     = reader.readString(FIRSTNAME);
        m_sLastName      = reader.readString(LASTNAME);
        m_addrHome       = reader.readObject(HOME_ADDRESS);
        m_addrWork       = reader.readObject(WORK_ADDRESS);
        m_mapPhoneNumber = reader.readMap(PHONE_NUMBERS, new HashMap<>());
        m_dtBirth        = reader.readLocalDate(BIRTH_DATE);
        m_nAge           = reader.readInt(AGE);
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(FIRSTNAME, m_sFirstName);
        writer.writeString(LASTNAME, m_sLastName);
        writer.writeObject(HOME_ADDRESS, m_addrHome);
        writer.writeObject(WORK_ADDRESS, m_addrWork);
        writer.writeMap(PHONE_NUMBERS, m_mapPhoneNumber);
        writer.writeDate(BIRTH_DATE, m_dtBirth);
        writer.writeInt(AGE, m_nAge);
        }

    // ----- Object methods -------------------------------------------------


    @Override
    public String toString()
        {
        StringBuffer sb = new StringBuffer(getFirstName())
                .append(" ")
                .append(getLastName())
                .append("\nAddresses")
                .append("\nHome: ").append(getHomeAddress())
                .append("\nWork: ").append(getWorkAddress())
                .append("\nPhone Numbers");

        if (m_mapPhoneNumber != null)
            {
            for (Iterator<Map.Entry<String, PhoneNumber>> iter = m_mapPhoneNumber.entrySet().iterator();
                 iter.hasNext(); )
                {
                Map.Entry<String, PhoneNumber> entry = iter.next();
                sb.append("\n")
                  .append(entry.getKey()).append(": ").append(entry.getValue());
                }
            }
        return sb.append("\nBirth Date: ")
                 .append(getBirthDate())
                 .append(" (")
                 .append(getAge())
                 .append(" years old)")
                 .toString();
        }

    // ----- helpers ---------------------------------------------------------

    /**
     * Calculate and set the age based upon date of birth.
     */
    public void calculateAge()
        {
        m_nAge = Period.between(m_dtBirth, LocalDate.now()).getYears();
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

    /**
     * The POF index for the HomeAddress property.
     */
    public static final int HOME_ADDRESS = 2;

    /**
     * The POF index for the WorkAddress property.
     */
    public static final int WORK_ADDRESS = 3;

    /**
     * The POF index for the PhoneNumbers property.
     */
    public static final int PHONE_NUMBERS = 4;

    /**
     * The POF index for the BirthDate property.
     */
    public static final int BIRTH_DATE = 5;

    /**
     * The POF index for the age property.
     */
    public static final int AGE = 6;

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
    private Address m_addrHome;

    /**
     * Work address.
     */
    private Address m_addrWork;

    /**
     * Maps phone number type (such as "work", "home") to PhoneNumber.
     */
    private Map<String, PhoneNumber> m_mapPhoneNumber;

    /**
     * Birth Date.
     */
    private LocalDate m_dtBirth;

    /**
     * Age.
     */
    private int m_nAge;
    }
