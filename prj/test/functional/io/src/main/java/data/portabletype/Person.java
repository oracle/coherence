/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package data.portabletype;

import java.util.Objects;
import com.tangosol.io.pof.schema.annotation.PortableType;

/**
 * Test class for ConfigurablePofContextPortableTypeTest.
 *
 * @author tam  2020.08.21
 */
@PortableType(id = 1000)
public class Person
    {

    public Person(int nId, String sName, Address homeAddress, Address workAddress)
        {
        m_nId = m_nId;
        m_sName = sName;
        m_homeAddress = homeAddress;
        m_workAddress = workAddress;
        }

    public void setId(int nId)
        {
        m_nId = nId;
        }

    public int getId()
        {
        return m_nId;
        }

    public void setName(String sName)
        {
        m_sName = sName;
        }

    public String getName()
        {
        return m_sName;
        }

    public void setHomeAddress(Address homeAddress)
        {
        m_homeAddress = homeAddress;
        }

    public Address getHomeAddres()
        {
        return m_homeAddress;
        }

    public void setWorkAddress(Address workAddress)
        {
        m_workAddress = workAddress;
        }

    public Address getWorkAddress()
        {
        return m_workAddress;
        }

    @Override
    public String toString()
        {
        return "Person{" +
               "m_nId=" + m_nId +
               ", m_sName='" + m_sName + '\'' +
               ", m_homeAddress=" + m_homeAddress +
               ", m_workAddress=" + m_workAddress +
               '}';
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return m_nId == person.m_nId &&
               Objects.equals(m_sName, person.m_sName) &&
               Objects.equals(m_homeAddress, person.m_homeAddress) &&
               Objects.equals(m_workAddress, person.m_workAddress);
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_nId, m_sName, m_homeAddress, m_workAddress);
        }

        private int     m_nId;
        private String  m_sName;
        private Address m_homeAddress;
        private Address m_workAddress;

    }
