/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofConstants;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofHelper;

import com.tangosol.util.SparseArray;

import java.util.Iterator;
import java.io.IOException;


/**
* An abstract base class for complex POF types, such as collections, arrays,
* maps, and user types.
*
* @author as 2009.02.12
* @since Coherence 3.5
*/
public abstract class ComplexPofValue
        extends AbstractPofValue
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PofValue instance wrapping the supplied buffer.
    *
    * @param valueParent parent value within the POF stream
    * @param bufValue    buffer containing POF representation of this value
    * @param ctx         POF context to use when reading or writing properties
    * @param of          offset of this value from the beginning of POF stream
    * @param nType       POF type identifier for this value
    * @param ofChildren  offset of the first child element within this value
    */
    public ComplexPofValue(PofValue valueParent, ReadBuffer bufValue,
                           PofContext ctx, int of, int nType, int ofChildren)
        {
        super(valueParent, bufValue, ctx, of, nType);

        m_aChildren  = new SparseArray();
        m_ofChildren = ofChildren;
        }


    // ----- PofValue interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public PofValue getChild(int nIndex)
        {
        PofValue valueChild = (PofValue) m_aChildren.get(nIndex);
        if (valueChild == null)
            {
            valueChild = findChild(nIndex);
            m_aChildren.set(nIndex, valueChild);
            }
        return valueChild;
        }


    // ----- public members -------------------------------------------------

    /**
    * Return an iterator over all parsed child values.
    *
    * @return a children iterator
    */
    public Iterator getChildrenIterator()
        {
        return m_aChildren.iterator();
        }


    // ----- internal members -----------------------------------------------

    /**
    * Return the uniform element type (if this is a uniform collection).
    *
    * @return uniform element type, or {@link PofConstants#T_UNKNOWN}
    *         if this is not a uniform collection
    */
    protected int getUniformElementType()
        {
        return m_nElementType;
        }

    /**
    * Set the uniform element type for this collection.
    *
    * @param nElementType uniform element type
    */
    protected void setUniformElementType(int nElementType)
        {
        m_nElementType = nElementType;
        }

    /**
    * Find the child value with the specified index.
    *
    * @param nIndex  index of the child value to find
    *
    * @return the child value
    */
    protected PofValue findChild(int nIndex)
        {
        int ofStart = m_ofChildren;
        int iStart  = getLastChildIndex(nIndex);
        if (iStart >= 0)
            {
            AbstractPofValue lastChild = (AbstractPofValue) m_aChildren.get(iStart);
            ofStart = lastChild.getOffset() - getOffset() + lastChild.getSize();
            iStart  = iStart + 1;
            }
        else
            {
            iStart  = 0;
            }

        return findChildInternal(nIndex, ofStart, iStart);
        }

    /**
    * Return index of the last parsed child with an index lower than the
    * specified one.
    *
    * @param nIndex  index to find the preceding child index for
    *
    * @return index of the last parsed child, or -1 if one does not exist
    */
    protected int getLastChildIndex(int nIndex)
        {
        SparseArray aChildren = m_aChildren;
        int         nLast     = (int) aChildren.getLastIndex();

        if (nIndex < nLast)
            {
            nLast = nIndex;
            while (nLast >= 0 && !aChildren.exists(nLast))
                {
                nLast--;
                }
            }
        return nLast;
        }

    /**
    * Return <tt>true</tt> if this complex value is encoded as one of uniform
    * collection types.
    *
    * @return <tt>true</tt> if this is a uniform collection
    */
    protected boolean isUniformCollection()
        {
        return m_nElementType != PofConstants.T_UNKNOWN;
        }

    /**
    * Skip a single child value.
    *
    * @param in  buffer input containing child values
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void skipChild(ReadBuffer.BufferInput in)
            throws IOException
        {
        if (isUniformCollection())
            {
            PofHelper.skipUniformValue(in, m_nElementType);
            }
        else
            {
            PofHelper.skipValue(in);
            }
        }

    /**
    * Extract child PofValue from a buffer.
    *
    * @param buf  parent buffer to extract the child from
    * @param of   offset of the child within the parent buffer
    * @param cb   length of the child in bytes
    *
    * @return extracted PofValue
    */
    protected PofValue extractChild(ReadBuffer buf, int of, int cb)
        {
        return isUniformCollection()
               ? PofValueParser.parseUniformValue(this, m_nElementType,
                        buf.getReadBuffer(of, cb), getPofContext(),  getOffset() + of)
               : PofValueParser.parseValue(this, buf.getReadBuffer(of, cb),
                        getPofContext(), getOffset() + of);
        }


    // ----- abstract members -----------------------------------------------

    /**
    * Find the child value with the specified index.
    *
    * @param nIndex   index of the child value to find
    * @param ofStart  offset within the parent buffer to start search from
    * @param iStart   index of the child value to start search from
    *
    * @return the child value
    */
    protected abstract PofValue findChildInternal(int nIndex, int ofStart, int iStart);


    // ----- data members ---------------------------------------------------

    /**
    * Sparse array of child values.
    */
    private SparseArray m_aChildren;

    /**
    * Offset of the first child element within this value.
    */
    private int m_ofChildren;

    /**
    * Type of the child values, if this is a uniform collection.
    */
    private int m_nElementType = PofConstants.T_UNKNOWN;
    }
