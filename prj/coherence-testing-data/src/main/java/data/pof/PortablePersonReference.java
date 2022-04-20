/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.Date;


public class PortablePersonReference
        extends Person
        implements PortableObject, Serializable
    {
    public PortablePersonReference()
        {
        }

    public PortablePersonReference(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sName = reader.readString(NAME);
        setAddress((Address) reader.readObject(ADDRESS));
        m_dtDOB = reader.readDate(DOB);
        setSpouse((Person) reader.readObject(SPOUSE));
        setChildren((Person[]) reader.readObjectArray(CHILDREN,
                new PortablePerson[0]));
        setSiblings((PortablePersonReference[]) reader.readObjectArray(
                SIBLINGS, new PortablePersonReference[0]));
        setFriend((PortablePersonReference) reader.readObject(FRIEND));
        }

    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(NAME, m_sName);
        writer.writeObject(ADDRESS, getAddress());
        writer.writeDateTime(DOB, m_dtDOB);
        writer.writeObject(SPOUSE, getSpouse());
        writer.writeObjectArray(CHILDREN, getChildren(), PortablePerson.class);
        writer.writeObjectArray(SIBLINGS, getSiblings(),
                PortablePersonReference.class);
        writer.writeObject(FRIEND, getFriend());
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

    public PortablePersonReference getFriend()
        {
        return m_friend;
        }

    public void setFriend(PortablePersonReference friend)
        {
        m_friend = friend;
        }

    public PortablePersonReference[] getSiblings()
        {
        return m_siblings;
        }

    public void setSiblings(PortablePersonReference[] siblings)
        {
        m_siblings = siblings;
        }

    public static final int NAME     = PersonLite.NAME;
    public static final int ADDRESS  = Person.ADDRESS;
    public static final int DOB      = PersonLite.DOB;
    public static final int SPOUSE   = Person.SPOUSE;
    public static final int CHILDREN = Person.CHILDREN;
    public static final int SIBLINGS = 5;
    public static final int FRIEND   = 6;

    PortablePersonReference[] m_siblings;
    PortablePersonReference   m_friend;
    }