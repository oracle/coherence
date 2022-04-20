/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.Date;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.PROPERTY)
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include= JsonTypeInfo.As.PROPERTY, property="@type")
public class PortablePerson
        extends Person
        implements PortableObject, Serializable
    {
    public PortablePerson()
        {
        }

    public PortablePerson(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    public PortablePerson(String sName, Date dtDOB, int nAge)
        {
        super(sName, dtDOB, nAge, new PortablePerson[0]);
        }

    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sName = reader.readString(NAME);
        setAddress((Address) reader.readObject(ADDRESS));
        m_dtDOB = reader.readDate(DOB);
        setSpouse((Person) reader.readObject(SPOUSE));
        setChildren(reader.readArray(CHILDREN, PortablePerson[]::new));
        m_nAge         = reader.readInt(AGE);
        m_phoneNumbers = reader.readMap(PHONE, new TreeMap());
        }

    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(NAME, m_sName);
        writer.writeObject(ADDRESS, getAddress());
        writer.writeDateTime(DOB, m_dtDOB);
        writer.writeObject(SPOUSE, getSpouse());
        writer.writeObjectArray(CHILDREN, getChildren(), PortablePerson.class);
        writer.writeInt(AGE, m_nAge);
        writer.writeMap(PHONE, m_phoneNumbers);
        }

    private synchronized void writeObject(ObjectOutputStream out)
            throws IOException
        {
        super.serializePerson(out);
        }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        super.deserializePerson(in);
        }

    public static final int NAME     = PersonLite.NAME;
    public static final int ADDRESS  = Person.ADDRESS;
    public static final int DOB      = PersonLite.DOB;
    public static final int SPOUSE   = Person.SPOUSE;
    public static final int CHILDREN = Person.CHILDREN;
    public static final int AGE      = Person.AGE;
    public static final int PHONE    = Person.PHONE;


    // ----- factory methods ------------------------------------------------

    /**
     * Creates a populated instance of a PortablePerson class to be used in
     * tests.
     *
     * @return a populated instance of a PortablePerson class to be used in
     *         tests
     */
    public static PortablePerson create()
        {
        PortablePerson p = new PortablePerson("Aleksandar Seovic", new Date(74, 7, 24), 36);
        initPerson(p);
        return p;
        }

    /**
     * Creates a populated instance of a PortablePerson class with no children
     * to be used in tests.
     *
     * @return a populated instance of a PortablePerson class to be used in
     *         tests
     */
    public static PortablePerson createNoChildren()
        {
        PortablePerson p = new PortablePerson("Aleksandar Seovic", new Date(74, 7, 24), 36);
        p.setAddress(new Address("123 Main St", "Tampa", "FL", "12345"));
        p.setSpouse(new PortablePerson("Marija Seovic", new Date(78, 1, 20), 33));
        return p;
        }
    }