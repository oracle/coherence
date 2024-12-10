/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.examples;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

/**
 * Example 34.1 A Simple Person Object
 *
 * Added POF support that is not in documentation.
 *
 * @version 12.1.3.0.0
 * @author  jf 2014.5.8
 */
public class Person
        implements Serializable, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link Person}
     */
    public Person()
        {
        // support PoF with a no-arg ctor.
        }

    /**
     * Constructs {@link Person}
     *
     * @param sFirstName first name
     * @param sLastName  last name
     * @param nAge       age
     */
    public Person(String sFirstName, String sLastName, int nAge)
        {
        m_sFirstName = sFirstName;
        m_sLastName  = sLastName;
        m_nAge       = nAge;
        }

    public String getFirstName()
        {
        return m_sFirstName;
        }

    public String getLastName()
        {
        return m_sLastName;
        }

    public int getAge()
        {
        return m_nAge;
        }

    public String toString()
        {
        return "Person( " + m_sFirstName + " " + m_sLastName + " : " + m_nAge + ")";
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sFirstName = in.readString(0);
        m_sLastName  = in.readString(1);
        m_nAge       = in.readInt(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sFirstName);
        out.writeString(1, m_sLastName);
        out.writeInt(2, m_nAge);
        }

    // ----- constants ------------------------------------------------------

    /**
     * serialization
     */
    public static final long serialVersionUID = 1L;

    // ----- data members ---------------------------------------------------

    private String m_sFirstName;
    private String m_sLastName;
    private int    m_nAge;
    }
