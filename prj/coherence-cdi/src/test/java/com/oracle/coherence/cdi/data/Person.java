/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi.data;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

/**
 * A simple class representing a person used in tests requiring data classes.
 *
 * @author Jonathan Knight  2019.10.25
 */
public class Person
        implements PortableObject, Serializable
    {
    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private PhoneNumber phoneNumber;

    /**
     * Default constructor for serialization.
     */
    public Person()
        {
        }

    /**
     * Create a {@link Person}.
     *
     * @param firstName   the person's first name
     * @param lastName    the person's last name
     * @param dateOfBirth the person's date of birth
     * @param phoneNumber the person's phone number
     */
    public Person(String firstName, String lastName, LocalDate dateOfBirth, PhoneNumber phoneNumber)
        {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        }

    public String getFirstName()
        {
        return firstName;
        }

    public void setFirstName(String firstName)
        {
        this.firstName = firstName;
        }

    public String getLastName()
        {
        return lastName;
        }

    public void setLastName(String lastName)
        {
        this.lastName = lastName;
        }

    public LocalDate getDateOfBirth()
        {
        return dateOfBirth;
        }

    public void setDateOfBirth(LocalDate dateOfBirth)
        {
        this.dateOfBirth = dateOfBirth;
        }

    public PhoneNumber getPhoneNumber()
        {
        return phoneNumber;
        }

    public void setPhoneNumber(PhoneNumber phoneNumber)
        {
        this.phoneNumber = phoneNumber;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        Person person = (Person) o;
        return Objects.equals(firstName, person.firstName)
               && Objects.equals(lastName, person.lastName)
               && Objects.equals(dateOfBirth, person.dateOfBirth);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(firstName, lastName, dateOfBirth);
        }

    @Override
    public String toString()
        {
        return "{firstName: \"" + firstName + "\""
               + ", lastName: \"" + lastName + "\""
               + ", dateOfBirth: " + dateOfBirth
               + ", phoneNumber: " + phoneNumber
               + '}';
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        firstName = in.readString(0);
        lastName = in.readString(1);
        dateOfBirth = in.readLocalDate(2);
        phoneNumber = in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, firstName);
        out.writeString(1, lastName);
        out.writeDate(2, dateOfBirth);
        out.writeObject(3, phoneNumber);
        }
    }
