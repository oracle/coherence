/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofContext;

import java.io.IOException;


/**
* PofArray is a {@link PofValue} implementation for arrays.
*
* @author as  2009.03.06
* @since Coherence 3.5
*/
public class PofArray
        extends ComplexPofValue
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PofArray instance wrapping the supplied buffer.
    *
    * @param valueParent parent value within the POF stream
    * @param bufValue    buffer containing POF representation of this value
    * @param ctx         POF context to use when reading or writing properties
    * @param of          offset of this value from the beginning of POF stream
    * @param nType       POF type identifier for this value
    * @param ofChildren  offset of the first child element within this value
    * @param cElements   the length of this array
    */
    public PofArray(PofValue valueParent, ReadBuffer bufValue,
                    PofContext ctx, int of, int nType, int ofChildren, int cElements)
        {
        super(valueParent, bufValue, ctx, of, nType, ofChildren);

        m_cElements = cElements;
        }


    // ----- public API -----------------------------------------------------

    /**
    * Return the length of this array.
    *
    * @return the length of this array
    */
    public int getLength()
        {
        return m_cElements;
        }


    // ----- internal -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected PofValue findChildInternal(int nIndex, int ofStart, int iStart)
        {
        ReadBuffer bufValue = getValueBuffer();
        ReadBuffer.BufferInput in = bufValue.getBufferInput();
        in.setOffset(ofStart);

        try
            {
            // check array bounds
            if (nIndex < 0 || nIndex >= getLength())
                {
                throw new IndexOutOfBoundsException(
                        "Element index " + nIndex + " must be in the range [0 .. "
                        + getLength() + ").");
                }

            // skip children until we find the one we are looking for
            int iProp = iStart;
            while (iProp < nIndex)
                {
                skipChild(in);
                iProp++;
                }

            // child found. parse it and return it
            int of = in.getOffset();
            skipChild(in);
            int cb = in.getOffset() - of;

            return extractChild(bufValue, of, cb);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The length of this array.
    */
    private int m_cElements;
    }