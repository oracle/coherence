/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotActiveException;

import java.util.Arrays;


/**
* A static {@link PofNavigator} implementation which uses an array of integer
* indices to navigate the PofValue hierarchy.
*
* @author as  2009.02.14
* @since Coherence 3.5
*/
public class SimplePofPath
        extends AbstractPofPath
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the PortableObject interface).
    */
    public SimplePofPath()
        {
        }

    /**
    * Construct a SimplePofPath using a single index as a path.
    *
    * @param nIndex  an index
    */
    public SimplePofPath(int nIndex)
        {
        m_aiElements = new int[] {nIndex};
        }

    /**
    * Construct a SimplePofPath using an array of indices as a path.
    *
    * @param anIndices  an array of indices
    */
    public SimplePofPath(int[] anIndices)
        {
        azzert(anIndices != null, "Indices array must not be null");
        m_aiElements = anIndices;
        }


    // ----- internal -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected int[] getPathElements()
        {
        return m_aiElements;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the SimplePofPath with another object to determine equality.
    * Two SimplePofPath objects are considered equal iff their indices are
    * equal.
    *
    * @return true iff this SimplePofPath and the passed object are equivalent
    */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof SimplePofPath)
            {
            SimplePofPath that = (SimplePofPath) o;
            return Arrays.equals(m_aiElements, that.m_aiElements);
            }
        return false;
        }

    /**
    * Determine a hash value for the SimplePofPath object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this SimplePofPath object
    */
    public int hashCode()
        {
        int[] ai = m_aiElements;
        return HashHelper.hash(ai, ai.length);
        }

    /**
    * Return a human-readable description for this SimplePofPath.
    *
    * @return a String description of the SimplePofPath
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            "(indices=" + toDelimitedString(m_aiElements, ".") + ')';
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aiElements = in.readIntArray(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        int[] aiElements = m_aiElements;
        if (aiElements == null)
            {
            throw new NotActiveException(
                    "SimplePofPath was constructed without indices");
            }
        out.writeIntArray(0, aiElements);
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_aiElements = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        int[] aiElements = m_aiElements;
        if (aiElements == null)
            {
            throw new NotActiveException(
                    "SimplePofPath was constructed without indices");
            }
        ExternalizableHelper.writeObject(out, aiElements);
        }

    // ----- data members ---------------------------------------------------

    /**
    * Path elements.
    */
    private int[] m_aiElements;
    }
