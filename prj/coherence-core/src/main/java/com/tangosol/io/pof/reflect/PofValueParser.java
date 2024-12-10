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

import com.tangosol.util.ExternalizableHelper;

import java.io.EOFException;
import java.io.IOException;


/**
* Parses POF-encoded binary and returns an instance of a {@link PofValue}
* wrapper for it.
*
* @author as 2009.02.12
* @since Coherence 3.5
*/
public abstract class PofValueParser
        extends    ExternalizableHelper
        implements PofConstants
    {
    // ----- public API -----------------------------------------------------

    /**
    * Parses POF-encoded binary and returns an instance of a {@link PofValue}
    * wrapper for it.
    *
    * @param buf  POF-encoded binary value
    * @param ctx  POF context to use
    *
    * @return a {@link PofValue} instance
    */
    public static PofValue parse(ReadBuffer buf, PofContext ctx)
        {
        ReadBuffer.BufferInput in = buf.getBufferInput();
        ReadBuffer bufDeco   = null;
        long       nDecoMask = 0;
        int        of;
        int        cb;

        try
            {
            int nType = in.readUnsignedByte();
            switch (nType)
                {
                case FMT_IDO:
                    readInt(in);           // skip the decoration
                    in.readUnsignedByte(); // skip FMT_EXT byte
                    of = in.getOffset();
                    cb = buf.length() - of;
                    break;

                case FMT_BIN_DECO:
                case FMT_BIN_EXT_DECO:
                    nDecoMask = nType == FMT_BIN_DECO ? in.readByte() : in.readPackedLong();
                    if ((nDecoMask & (1 << DECO_VALUE)) == 0)
                        {
                        throw new EOFException("Decorated binary is missing a value");
                        }

                    // get length and offset for the decorated value
                    cb = readInt(in);
                    of = in.getOffset();

                    // get decorations, so we can apply them later
                    int ofDeco = of + cb;
                    bufDeco = buf.getReadBuffer(ofDeco, buf.length() - ofDeco);

                    // replace buffer with undecorated value
                    buf = buf.getReadBuffer(of, cb);

                    // fall through -- should be treated as FMT_EXT now
                case FMT_EXT:
                    of = 1;
                    cb = buf.length() - 1;
                    break;

                default:
                    of = 0;
                    cb = buf.length();
                }
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        AbstractPofValue valueRoot = (AbstractPofValue)
                parseValue(null, buf.getReadBuffer(of, cb), ctx, of);
        valueRoot.setOriginalBuffer(buf);
        valueRoot.setDecorations(nDecoMask, bufDeco);

        return valueRoot;
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Parse a POF-encoded binary and return an instance of a {@link PofValue}
    * wrapping the binary.
    *
    * @param valueParent  parent POF value
    * @param bufValue     buffer with POF-encoded binary value
    * @param ctx          POF context to use
    * @param of           offset of the parsed value from the beginning of the
    *                     POF stream
    *
    * @return a PofValue instance
    */
    protected static PofValue parseValue(PofValue valueParent, ReadBuffer bufValue,
            PofContext ctx, int of)
        {
        ReadBuffer.BufferInput in = bufValue.getBufferInput();
        try
            {
            int nType = in.readPackedInt();
            return instantiatePofValue(valueParent, nType, bufValue, ctx, of, in);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Parses a uniform POF-encoded binary and returns an instance of a
    * {@link PofValue} wrapping the binary.
    *
    * @param valueParent  parent POF value
    * @param nType        type identifier of this POF value
    * @param bufValue     POF-encoded binary value without the type identifier
    * @param ctx          POF context to use
    * @param of           offset of the parsed value from the beginning of the
    *                     POF stream
    *
    * @return a PofValue instance
    */
    protected static PofValue parseUniformValue(PofValue valueParent, int nType,
            ReadBuffer bufValue, PofContext ctx, int of)
        {
        ReadBuffer.BufferInput in = bufValue.getBufferInput();
        AbstractPofValue    value = (AbstractPofValue)
                instantiatePofValue(valueParent, nType, bufValue, ctx, of, in);
        value.setUniformEncoded();
        return value;
        }

    /**
    * Creates a PofValue instance.
    *
    * @param valueParent  parent POF value
    * @param nType        type identifier of this POF value
    * @param bufValue     POF-encoded binary value without the type identifier
    * @param ctx          POF context to use
    * @param of           offset of the parsed value from the beginning of the
    *                     POF stream
    * @param in           buffer input to read the value from
    *
    * @return a {@link PofValue} instance
    */
    protected static PofValue instantiatePofValue(PofValue valueParent, int nType,
            ReadBuffer bufValue, PofContext ctx, int of, ReadBuffer.BufferInput in)
        {
        try
            {
            int         cSize;
            int         nElementType;
            int         nId;
            int         ofChildren;
            PofUserType value;
            PofValue    valueRef;

            switch (nType)
                {
                case T_ARRAY:
                    cSize      = in.readPackedInt();
                    ofChildren = in.getOffset();
                    return new PofArray(valueParent, bufValue, ctx, of, nType,
                            ofChildren, cSize);

                case T_UNIFORM_ARRAY:
                    nElementType = in.readPackedInt();
                    cSize        = in.readPackedInt();
                    ofChildren   = in.getOffset();
                    return new PofUniformArray(valueParent, bufValue, ctx, of,
                            nType, ofChildren, cSize, nElementType);

                case T_COLLECTION:
                    cSize = in.readPackedInt();
                    ofChildren = in.getOffset();
                    return new PofCollection(valueParent, bufValue, ctx, of,
                            nType, ofChildren, cSize);

                case T_UNIFORM_COLLECTION:
                    nElementType = in.readPackedInt();
                    cSize        = in.readPackedInt();
                    ofChildren   = in.getOffset();
                    return new PofUniformCollection(valueParent, bufValue, ctx,
                            of, nType, ofChildren, cSize, nElementType);

                case T_SPARSE_ARRAY:
                                 in.readPackedInt(); // skip size
                    ofChildren = in.getOffset();
                    return new PofSparseArray(valueParent, bufValue, ctx, of,
                            nType, ofChildren);

                case T_UNIFORM_SPARSE_ARRAY:
                    nElementType = in.readPackedInt();
                                   in.readPackedInt(); // skip size
                    ofChildren   = in.getOffset();
                    return new PofUniformSparseArray(valueParent, bufValue, ctx,
                            of, nType, ofChildren, nElementType);

                case T_REFERENCE:
                    nId        = in.readPackedInt(); // reference id
                    ofChildren = in.getOffset();
                    value      = new PofUserType(valueParent, bufValue, ctx, of,
                            nType, ofChildren, 0);
                    valueRef = value.lookupIdentity(nId);
                    if (valueRef != null)
                        {
                        value.setValue(valueRef.getValue());
                        }
                    return value;

                default:
                    nId = -1;
                    if (nType == T_IDENTITY)
                        {
                        nId   = in.readPackedInt();
                        nType = in.readPackedInt();
                        if (valueParent == null)
                            {
                            of = in.getOffset();
                            }
                        }

                    if (nType >= 0)
                        {
                        int nVersionId = in.readPackedInt();
                        ofChildren     = in.getOffset();

                        value = new PofUserType(valueParent, bufValue, ctx, of,
                                nType, ofChildren, nVersionId);
                        if (nId > -1)
                            {
                            value.registerIdentity(nId, value);
                            }
                        return value;
                        }
                    else
                        {
                        return new SimplePofValue(valueParent, bufValue, ctx,
                                of, nType);
                        }
                }
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }
    }
