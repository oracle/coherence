/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.PofConstants;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofHelper;

import com.tangosol.util.Binary;

import java.io.IOException;


/**
* PofSparseArray is {@link PofValue} implementation for sparse arrays.
*
* @author as  2009.03.06
* @since Coherence 3.5
*/
public class PofSparseArray
        extends ComplexPofValue
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PofSparseArray instance wrapping the supplied buffer.
    *
    * @param valueParent parent value within the POF stream
    * @param bufValue    buffer containing POF representation of this value
    * @param ctx         POF context to use when reading or writing properties
    * @param of          offset of this value from the beginning of POF stream
    * @param nType       POF type identifier for this value
    * @param ofChildren  offset of the first child element within this value
    */
    public PofSparseArray(PofValue valueParent, ReadBuffer bufValue,
                          PofContext ctx, int of, int nType, int ofChildren)
        {
        super(valueParent, bufValue, ctx, of, nType, ofChildren);
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
            // skip children until we either find the one we are looking for,
            // or reach the end of the sparse array (index == -1)
            int ofLastIndex = ofStart;
            int iProp       = in.readPackedInt();
            while (iProp < nIndex && iProp >= 0)
                {
                skipChild(in);
                ofLastIndex = in.getOffset();
                iProp       = in.readPackedInt();
                }

            // child found. extract it from the parent buffer
            if (iProp == nIndex)
                {
                int of = in.getOffset();
                skipChild(in);
                int cb = in.getOffset() - of;

                return extractChild(bufValue, of, cb);
                }

            // child not found
            return instantiateNilValue(ofLastIndex, nIndex);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Instantiate a {@link NilPofValue} (factory method).
    *
    * @param of      offset this value would be at if it existed
    * @param nIndex  index of this value within the parent sparse array
    *
    * @return a NilPofValue instance
    */
    protected NilPofValue instantiateNilValue(int of, int nIndex)
        {
        return new NilPofValue(this, getPofContext(), getOffset() + of,
                                PofConstants.T_UNKNOWN, nIndex);
        }


    // ----- NilPofValue inner class ------------------------------------

    /**
    * NilPofValue represents a value that does not exist in the original POF stream.
    */
    protected static class NilPofValue
            extends SimplePofValue
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a NilPofValue instance.
        *
        * @param valueParent  parent value within the POF stream
        * @param ctx          POF context to use when reading or writing properties
        * @param of           offset of this value from the beginning of POF stream
        * @param nType        POF type identifier for this value
        * @param nIndex       index of this value within the parent sparse array
        */
        public NilPofValue(PofValue valueParent, PofContext ctx, int of, int nType,
                            int nIndex)
            {
            super(valueParent, Binary.NO_BINARY, ctx, of, nType);

            m_oValue = null;
            m_nIndex = nIndex;
            }

        // ----- PofValue interface -------------------------------------

        /**
        * {@inheritDoc}
        */
        public Object getValue(int nType)
            {
            Object oValue = m_oValue;
            if (oValue == null)
                {
                // Return default value for primitives that have been
                // optimized out of the serialized binary.
                switch (nType)
                    {
                    case PofConstants.T_INT16:
                        return (short) 0;

                    case PofConstants.T_INT32:
                        return 0;

                    case PofConstants.T_INT64:
                        return 0L;

                    case PofConstants.T_FLOAT32:
                        return (float) 0;

                    case PofConstants.T_FLOAT64:
                        return (double) 0;

                    case PofConstants.T_BOOLEAN:
                        return Boolean.FALSE;

                    case PofConstants.T_OCTET:
                        return (byte) 0;

                    case PofConstants.T_CHAR:
                        return (char) 0;

                    default:
                        return null;
                    }
                }

            return PofReflectionHelper.ensureType(oValue, nType, getPofContext());
            }

        /**
        * {@inheritDoc}
        */
        public void setValue(Object oValue)
            {
            super.setValue(oValue);

            if (oValue != null)
                {
                m_nType = PofHelper.getPofTypeId(oValue.getClass(), getPofContext());
                }
            }

        // ----- internal API -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public ReadBuffer getSerializedValue()
            {
            try
                {
                ReadBuffer bufValue = super.getSerializedValue();

                WriteBuffer buf = new ByteArrayWriteBuffer(5 + bufValue.length());
                WriteBuffer.BufferOutput out = buf.getBufferOutput();

                out.writePackedInt(m_nIndex);
                out.writeBuffer(bufValue);

                return buf.getReadBuffer();
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        // ----- data members -------------------------------------------

        /**
        * Index of this value within the parent sparse array
        */
        private int m_nIndex;
        }
    }
