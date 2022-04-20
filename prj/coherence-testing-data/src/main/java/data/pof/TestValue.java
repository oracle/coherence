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

import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;


/**
* Test PortableObject class.
*
* @author as  2009.01.31
*/
public class TestValue
        implements PortableObject
    {
    public TestValue()
        {
        }

    public TestValue(Object[] oArray, String[] sArray,
                     Collection col, Collection colUniform,
                     LongArray oSparseArray, LongArray oUniformSparseArray)
        {
        m_oArray = oArray;
        m_sArray = sArray;
        m_col = col;
        m_colUniform = colUniform;
        m_sparseArray = oSparseArray;
        m_uniformSparseArray = oUniformSparseArray;
        }

    public void readExternal(PofReader in)
            throws IOException
        {
        m_oArray = in.readObjectArray(0, new Object[0]);
        m_sArray = (String[]) in.readObjectArray(1, new String[0]);
        m_col = in.readCollection(2, new ArrayList());
        m_colUniform = in.readCollection(3, new ArrayList());
        m_sparseArray = in.readLongArray(4, new SparseArray());
        m_uniformSparseArray = in.readLongArray(5, new SparseArray());
        }

    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObjectArray(0, m_oArray);
        out.writeObjectArray(1, m_sArray, String.class);
        out.writeCollection(2, m_col);
        out.writeCollection(3, m_colUniform, String.class);
        out.writeLongArray(4, m_sparseArray);
        out.writeLongArray(5, m_uniformSparseArray, String.class);
        }


    // ----- data members ---------------------------------------------------

    public Object[] m_oArray;
    public String[] m_sArray;
    public Collection m_col;
    public Collection m_colUniform;
    public LongArray m_sparseArray;
    public LongArray m_uniformSparseArray;


    // ----- factory methods ------------------------------------------------

    /**
    * Creates a populated instance of a TestValue class to be used in tests.
    *
    * @param fRefEnabled  a flag to indicate if POF references are enabled
    *
    * @return a populated instance of a TestValue class to be used in tests
    */
    public static TestValue create(boolean fRefEnabled)
        {
        PortablePerson person   = fRefEnabled ? PortablePerson.createNoChildren() : PortablePerson.create();
        Binary         binary   = new Binary(new byte[]{22, 23, 24});
        Object[]       aObj     = new Object[] { Integer.valueOf(1), "two", person, binary };
        String[]       aStr     = new String[] { "one", "two", "three", "four"};

        SparseArray oSparseArray = new SparseArray();
        oSparseArray.set(2, "two");
        oSparseArray.set(4, Integer.valueOf(4));
        oSparseArray.set(5, person);

        SparseArray oUnfiformSparseArray = new SparseArray();
        oUnfiformSparseArray.set(2, "two");
        oUnfiformSparseArray.set(4, "four");

        return new TestValue(aObj, aStr,
                             Arrays.asList(aObj), Arrays.asList(aStr),
                             oSparseArray, oUnfiformSparseArray);
        }
    }
