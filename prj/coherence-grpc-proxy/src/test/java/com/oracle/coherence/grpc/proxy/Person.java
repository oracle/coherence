/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

import java.util.Objects;

public class Person
        implements Serializable, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    public Person()
        {
        }

    public Person(String sFirstName, String sLastName, int nAge, String sGender)
        {
        this.m_sFirstName = sFirstName;
        this.m_lastName   = sLastName;
        this.m_nAge       = nAge;
        this.sGender      = sGender;
        }

    // ----- accessors ------------------------------------------------------

    public String getFirstName()
        {
        return m_sFirstName;
        }

    public void setFirstName(String sFirstName)
        {
        this.m_sFirstName = sFirstName;
        }

    public String getLastName()
        {
        return m_lastName;
        }

    public void setLastName(String sLastName)
        {
        this.m_lastName = sLastName;
        }

    public int getAge()
        {
        return m_nAge;
        }

    public void setAge(int nAge)
        {
        this.m_nAge = nAge;
        }

    public String getGender()
        {
        return sGender;
        }

    public void setGender(String sGender)
        {
        this.sGender = sGender;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof Person))
            {
            return false;
            }
        Person person = (Person) o;
        return getAge() == person.getAge() &&
               Objects.equals(getFirstName(), person.getFirstName()) &&
               Objects.equals(getLastName(), person.getLastName()) &&
               Objects.equals(getGender(), person.getGender());
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(getFirstName(), getLastName(), getAge(), getGender());
        }

    @Override
    public String toString()
        {
        return "Person{" +
               "firstName='" + m_sFirstName + '\'' +
               ", lastName='" + m_lastName + '\'' +
               ", age=" + m_nAge +
               ", gender='" + sGender + '\'' +
               '}';
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sFirstName = in.readString(0);
        m_lastName   = in.readString(1);
        m_nAge       = in.readInt(2);
        sGender      = in.readString(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sFirstName);
        out.writeString(1, m_lastName);
        out.writeInt(2, m_nAge);
        out.writeString(3, sGender);
        }

    // ----- data members ---------------------------------------------------

    private String m_sFirstName;

    private String m_lastName;

    private int m_nAge;

    private String sGender;
    }
