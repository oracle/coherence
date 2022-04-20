/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.persistence;

import java.io.Serializable;

import java.util.Objects;

public class Person
        implements Serializable
    {
    public Person() {}

    public Person(int nId, String sName)
        {
        this.m_nId   = nId;
        this.m_sName = sName;
        }

    public int getId()
        {
        return m_nId;
        }

    public void setId(int nId)
        {
        m_nId = nId;
        }

    public String getName()
        {
        return m_sName;
        }

    public void setName(String sName)
        {
        m_sName = sName;
        }

    public String getAddress()
        {
        return m_sAddress;
        }

    public void setAddress(String sAddress)
        {
        m_sAddress = sAddress;
        }

    public String getState()
        {
        return m_sState;
        }

    public void setState(String sState)
        {
        m_sState = sState;
        }

    public int getSalary()
        {
        return m_nSalary;
        }

    public void setSalary(int nSalary)
        {
        m_nSalary = nSalary;
        }

    public int getAge()
        {
        return m_nAge;
        }

    public void setAge(int nAge)
        {
        m_nAge = nAge;
        }

    @Override
    public String toString()
        {
        return "Person(" + m_nId + ", " + m_sName + ")";
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
        return m_nId == person.m_nId && Objects.equals(m_sName, person.m_sName);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_nId, m_sName);
        }

    int    m_nId;
    String m_sName;
    String m_sAddress;
    String m_sState;
    int    m_nSalary;
    int    m_nAge;
    }
