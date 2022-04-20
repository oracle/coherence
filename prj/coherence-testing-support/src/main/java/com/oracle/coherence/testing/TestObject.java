/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.tangosol.util.Base;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;


public class TestObject 
        implements PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default Constructor.
     */
    public TestObject()
        {	
        }

    /**
     * Constructor.
     *
     * @param iD    identifier
     * @param name  object name
     */
    public TestObject(int iD, String name)
        {
        this.m_ID = iD;
        this.m_name = name;
        }


    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader)
            throws IOException
        {
        this.m_ID   = reader.readInt(0);
        this.m_name = reader.readString(1);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeInt(0, m_ID);
        writer.writeString(1, m_name);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * ID accessor.
     *
     * @returns object id
     */
    public int getID()
        {
        return m_ID;
        }

    /**
     * ID accessor.
     *
     * @param iD  object id
     */
    public void setID(int iD)
        {
        this.m_ID = iD;
        }

    /**
     * Name accessor.
     *
     * @returns object name
     */
    public String getName()
        {
        return m_name;
        }

    /**
     * ID accessor.
     *
     * @param name  object name
     */
    public void setName(String name)
        {
        this.m_name = name;
        }

    // ----- Object --------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "TestObject [ID=" + m_ID + ", name=" + m_name + "]";
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_ID;
        result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
        return result;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
        {
        TestObject other = (TestObject) obj;
        if (m_ID != other.m_ID)
            {
            return false;
            }
        return Base.equals(m_name, other.m_name);
	}

    // ----- data members ---------------------------------------------------

    /**
     * object id.
     */
    private int    m_ID;

    /**
     * object name.
     */
    private String m_name;
    }
