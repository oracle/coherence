/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.PROPERTY)
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include= JsonTypeInfo.As.PROPERTY, property="@type")
public class Person
        extends PersonLite
    {
    public Person()
        {
        }

    public Person(String sName, Date dtDOB)
        {
        this(sName, dtDOB, 0);
        }

    public Person(String sName, Date dtDOB, int nAge)
        {
        this(sName, dtDOB, nAge, new Person[0]);
        }
    public Person(String sName, Date dtDOB, int nAge, Person[] children)
        {
        super(sName, dtDOB);
        m_nAge = nAge;
        m_aChildren = children;
        }

    @XmlAttribute
    public int getAge()
        {
        return m_nAge;
        }
    
    public void setAge(int nAge)
        {
        m_nAge = nAge;
        }
               
    public Address getAddress()
        {
        return m_addr;
        }

    public void setAddress(Address addr)
        {
        m_addr = addr;
        }

    public Person getSpouse()
        {
        return m_spouse;
        }

    public void setSpouse(Person spouse)
        {
        m_spouse = spouse;
        }

    @JsonProperty
    @XmlElementWrapper(name = "children")
    @XmlElement(name = "child")
    public Person[] getChildren()
        {
        return m_aChildren;
        }

    public void setChildren(Person[] aChildren)
        {
        m_aChildren = aChildren;
        }

    public void addPhoneNumber(String sType, PhoneNumber phoneNumber)
        {
        m_phoneNumbers.put(sType, phoneNumber);
        }

    public PhoneNumber getPhoneNumber(String sType)
        {
        return m_phoneNumbers.get(sType);
        }

    public Map getPhoneNumbers()
        {
        return m_phoneNumbers;
        }

    protected void serializePerson(ObjectOutputStream out)
            throws IOException
        {
        out.defaultWriteObject();
        out.writeUTF(m_sName);
        out.writeObject(m_dtDOB);
        out.writeObject(m_addr);
        out.writeObject(m_spouse);

        int cChildren = m_aChildren == null ? 0 : m_aChildren.length;
        out.writeInt(cChildren);

        for (int i = 0; i < cChildren; i++)
            {
            out.writeObject(m_aChildren[i]);
            }
        out.writeInt(m_nAge);
        out.writeObject(m_phoneNumbers);
        }

    protected void deserializePerson(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        in.defaultReadObject();
        m_sName       = in.readUTF();
        m_dtDOB       = (Date) in.readObject();
        m_addr        = (Address) in.readObject();
        m_spouse      = (Person) in.readObject();
        int cChildren = in.readInt();
        m_aChildren   = new Person[cChildren];

        if (cChildren > 0)
            {
            for (int i = 0; i < m_aChildren.length; i++)
                {
                m_aChildren[i] = (Person) in.readObject();
                }
            }
        m_nAge = in.readInt();
        m_phoneNumbers = (Map) in.readObject();
        }

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

        final Person that = (Person) o;

        return  equals(this.m_sName, that.m_sName)
                && equals(this.m_dtDOB, that.m_dtDOB)
                && equals(this.m_nAge, that.m_nAge)
                && equalsDeep(this.m_aChildren, that.m_aChildren)
                && equals(this.m_addr, that.m_addr)
                && equals(this.m_spouse, that.m_spouse)
                && equals(this.m_phoneNumbers, that.m_phoneNumbers);
        }

    public String toString()
        {
        return "Person{"
               + "name=" + m_sName
               + ", dob=" + m_dtDOB
               + ", age=" + m_nAge
               + ", address=" + m_addr
               + ", spouse=" + (m_spouse == null
                                               ? null
                                               : m_spouse.m_sName)
               + ", children=" + (m_aChildren == null
                                               ? null
                                               : m_aChildren.length)
               + ", phoneNumbers=" + m_phoneNumbers
               + '}';
        }

    public int      m_nAge;
    public Address  m_addr;
    public Person   m_spouse;
    public Person[] m_aChildren;

    public Map<String,PhoneNumber> m_phoneNumbers = new TreeMap();

    public static final int ADDRESS  = 1;
    public static final int SPOUSE   = 3;
    public static final int CHILDREN = 4;
    public static final int AGE      = 5;
    public static final int PHONE    = 6;


    // ----- factory methods ------------------------------------------------

    /**
    * Creates a populated instance of a Person class to be used in
    * tests.
    *
    * @return a populated instance of a Person class to be used in
    * tests
    */
    public static Person create()
        {
        Person p = new PortablePerson("Aleksandar Seovic", new Date(74, 7, 24), 36);
        initPerson(p);
        return p;
        }

    /**
    * Initialize the given Person with some sample data.
    *
    * @param p  the Person to initialize
    */
    protected static void initPerson(Person p)
        {
        p.setAddress(new Address("123 Main St", "Tampa", "FL", "12345"));
        p.setSpouse(new PortablePerson("Marija Seovic", new Date(78, 1, 20), 33));
        p.setChildren(new PortablePerson[] {
                new PortablePerson("Ana Maria Seovic", new Date(104, 7, 14), 6),
                new PortablePerson("Novak Seovic", new Date(108, 11, 28), 3)
            });
        }
    }