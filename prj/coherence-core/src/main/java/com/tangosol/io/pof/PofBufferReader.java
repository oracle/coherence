/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.ReadBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import java.io.IOException;
import java.io.StreamCorruptedException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.nio.ByteBuffer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.function.IntFunction;

/**
* {@link PofReader} implementation that reads POF-encoded data from a
* {@link com.tangosol.io.ReadBuffer.BufferInput BufferInput}.
*
* @author cp  2006.07.14
*
* @since Coherence 3.2
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class PofBufferReader
        extends PofHelper
        implements PofReader
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new PofBufferReader that will read a POF stream from the
    * passed BufferInput object.
    *
    * @param in   a BufferInput object
    * @param ctx  the PofContext
    */
    public PofBufferReader(ReadBuffer.BufferInput in, PofContext ctx)
        {
        m_in  = in;
        m_ctx = ctx;
        }

    protected PofBufferReader()
        {
        }


    // ----- PofReader interface --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean readBoolean(int iProp)
            throws IOException
        {
        return readInt(iProp) != 0;
        }

    /**
    * {@inheritDoc}
    */
    public byte readByte(int iProp)
            throws IOException
        {
        return (byte) readInt(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public char readChar(int iProp)
            throws IOException
        {
        return (char) readInt(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public short readShort(int iProp)
            throws IOException
        {
        return (short) readInt(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public int readInt(int iProp)
            throws IOException
        {
        int n = 0;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    n = readInt(iProp);
                    registerIdentity(nId, n);
                    }
                    break;

                case T_REFERENCE:
                    Number number = (Number) lookupIdentity(in.readPackedInt());
                    if (number != null)
                        {
                        n = number.intValue();
                        }
                    break;

                case V_REFERENCE_NULL:
                case V_INT_0:
                    break;

                default:
                    n = readAsInt(in, nType);
                    break;
                }
            }
        complete(iProp);

        return n;
        }

    /**
    * {@inheritDoc}
    */
    public long readLong(int iProp)
            throws IOException
        {
        long n = 0L;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    n = readLong(iProp);
                    registerIdentity(nId, n);
                    }
                    break;

                case T_REFERENCE:
                    Number number = (Number) lookupIdentity(in.readPackedInt());
                    if (number != null)
                        {
                        n = number.longValue();
                        }
                    break;

                case V_REFERENCE_NULL:
                case V_INT_0:
                    break;

                default:
                    n = readAsLong(in, nType);
                    break;
                }
            }
        complete(iProp);

        return n;
        }

    /**
    * {@inheritDoc}
    */
    public float readFloat(int iProp)
            throws IOException
        {
        float fl = 0.0F;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    fl = readFloat(iProp);
                    registerIdentity(nId, fl);
                    }
                    break;

                case T_REFERENCE:
                    Number number = (Number) lookupIdentity(in.readPackedInt());
                    if (number != null)
                        {
                        fl = number.floatValue();
                        }
                    break;

                case V_REFERENCE_NULL:
                case V_INT_0:
                    break;

                default:
                    fl = readAsFloat(in, nType);
                    break;
                }
            }
        complete(iProp);

        return fl;
        }

    /**
    * {@inheritDoc}
    */
    public double readDouble(int iProp)
            throws IOException
        {
        double dfl = 0.0;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    dfl = readDouble(iProp);
                    registerIdentity(nId, dfl);
                    }
                    break;

                case T_REFERENCE:
                    Number number = (Number) lookupIdentity(in.readPackedInt());
                    if (number != null)
                        {
                        dfl = number.doubleValue();
                        }
                    break;

                case V_REFERENCE_NULL:
                case V_INT_0:
                    break;

                default:
                    dfl = readAsDouble(in, nType);
                    break;
                }
            }
        complete(iProp);

        return dfl;
        }

    /**
    * {@inheritDoc}
    */
    public boolean[] readBooleanArray(int iProp)
            throws IOException
        {
        boolean[] af = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    af = readBooleanArray(iProp);
                    registerIdentity(nId, af);
                    }
                    break;

                case T_REFERENCE:
                    af = (boolean[]) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    af = BOOLEAN_ARRAY_EMPTY;
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    af = new boolean[cElements];
                    for (int i = 0; i < cElements; ++i)
                        {
                        af[i] = readAsInt(in, in.readPackedInt()) != 0;
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    af = new boolean[cElements];
                    switch (nElementType)
                        {
                        case T_BOOLEAN:
                        case T_INT16:
                        case T_INT32:
                        case T_INT64:
                        case T_INT128:
                            for (int i = 0; i < cElements; ++i)
                                {
                                af[i] = in.readPackedInt() != 0;
                                }
                            break;

                        default:
                            for (int i = 0; i < cElements; ++i)
                                {
                                af[i] = readAsInt(in, nElementType) != 0;
                                }
                            break;
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    af = new boolean[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        af[iElement] = readAsInt(in, in.readPackedInt()) != 0;
                        }
                    while (--cElements >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    af = new boolean[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        af[iElement] = readAsInt(in, nElementType) != 0;
                        }
                    while (--cElements >= 0);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return af;
        }

    /**
    * {@inheritDoc}
    */
    public byte[] readByteArray(int iProp)
            throws IOException
        {
        byte[] ab = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    ab = readByteArray(iProp);
                    registerIdentity(nId, ab);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    ab = o instanceof Binary
                            ? ((Binary) o).toByteArray()
                            : (byte[]) o;
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    ab = BYTE_ARRAY_EMPTY;
                    break;

                case T_OCTET_STRING:
                    ab = new byte[in.readPackedInt()];
                    in.readFully(ab);
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    ab = new byte[cElements];
                    for (int i = 0; i < cElements; ++i)
                        {
                        ab[i] = (byte) readAsInt(in, in.readPackedInt());
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();

                    ab = new byte[cElements];
                    if (nElementType == T_OCTET)
                        {
                        in.readFully(ab);
                        }
                    else
                        {
                        for (int i = 0; i < cElements; ++i)
                            {
                            ab[i] = (byte) readAsInt(in, nElementType);
                            }
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    ab = new byte[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        ab[iElement] = (byte) readAsInt(in, in.readPackedInt());
                        }
                    while (--cElements >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    ab = new byte[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        ab[iElement] = nElementType == T_OCTET
                                       ? in.readByte()
                                       : (byte) readAsInt(in, nElementType);
                        }
                    while (--cElements >= 0);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return ab;
        }

    /**
    * {@inheritDoc}
    */
    public char[] readCharArray(int iProp)
            throws IOException
        {
        char[] ach = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    ach = readCharArray(iProp);
                    registerIdentity(nId, ach);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    ach = o instanceof String
                            ? ((String) o).toCharArray()
                            : (char[]) o;
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    ach = CHAR_ARRAY_EMPTY;
                    break;

                case T_OCTET_STRING:
                    {
                    int    cb = in.readPackedInt();
                    byte[] ab = new byte[cb];
                    in.readFully(ab);

                    ach = new char[cb];
                    for (int of = 0; of < cb; ++cb)
                        {
                        ach[of] = (char) (ab[of] & 0xFF);
                        }
                    }
                    break;

                case T_CHAR_STRING:
                    ach = in.readSafeUTF().toCharArray();
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    ach = new char[cElements];
                    for (int i = 0; i < cElements; ++i)
                        {
                        ach[i] = (char) readAsInt(in, in.readPackedInt());
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    ach = new char[cElements];

                    if (nElementType == T_OCTET)
                        {
                        // raw encoding (since 24.09)
                        int        cb = cElements * 2;
                        ByteBuffer bb = in.readBuffer(cb).toByteBuffer();
                        bb.asCharBuffer().get(ach, 0, cElements);
                        }
                    else
                        {
                        for (int i = 0; i < cElements; ++i)
                            {
                            ach[i] = nElementType == T_CHAR
                                     ? readChar(in)
                                     : (char) readAsInt(in, nElementType);
                            }
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    ach = new char[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        ach[iElement] = (char) readAsInt(in, in.readPackedInt());
                        }
                    while (--cElements >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    ach = new char[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        ach[iElement] = nElementType == T_CHAR
                                        ? readChar(in)
                                        : (char) readAsInt(in, nElementType);
                        }
                    while (--cElements >= 0);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return ach;
        }

    /**
    * {@inheritDoc}
    */
    public short[] readShortArray(int iProp)
            throws IOException
        {
        short[] an = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    an = readShortArray(iProp);
                    registerIdentity(nId, an);
                    }
                    break;

                case T_REFERENCE:
                    an = (short[]) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_COLLECTION_EMPTY:
                    an = SHORT_ARRAY_EMPTY;
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    an = new short[cElements];
                    for (int i = 0; i < cElements; ++i)
                        {
                        an[i] = (short) readAsInt(in, in.readPackedInt());
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    an = new short[cElements];
                    switch (nElementType)
                        {
                        case T_INT16:
                        case T_INT32:
                        case T_INT64:
                        case T_INT128:
                            for (int i = 0; i < cElements; ++i)
                                {
                                an[i] = (short) in.readPackedInt();
                                }
                            break;

                        case T_OCTET:  // raw encoding (since 24.09)
                            int        cb = cElements * 2;
                            ByteBuffer bb = in.readBuffer(cb).toByteBuffer();
                            bb.asShortBuffer().get(an, 0, cElements);
                            break;

                        default:
                            for (int i = 0; i < cElements; ++i)
                                {
                                an[i] = (short) readAsInt(in, nElementType);
                                }
                            break;
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    an = new short[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        an[iElement] = (short) readAsInt(in, in.readPackedInt());
                        }
                    while (--cElements >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    an = new short[cElements];
                    switch (nElementType)
                        {
                        case T_INT16:
                        case T_INT32:
                        case T_INT64:
                        case T_INT128:
                            do
                                {
                                int iElement = in.readPackedInt();
                                if (iElement < 0)
                                    {
                                    break;
                                    }
                                an[iElement] = (short) in.readPackedInt();
                                }
                            while (--cElements >= 0);
                            break;

                        default:
                            do
                                {
                                int iElement = in.readPackedInt();
                                if (iElement < 0)
                                    {
                                    break;
                                    }
                                an[iElement] = (short) readAsInt(in, nElementType);
                                }
                            while (--cElements >= 0);
                            break;
                        }
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return an;
        }

    /**
    * {@inheritDoc}
    */
    public int[] readIntArray(int iProp)
            throws IOException
        {
        int[] an = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    an = readIntArray(iProp);
                    registerIdentity(nId, an);
                    }
                    break;

                case T_REFERENCE:
                    an = (int[]) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_COLLECTION_EMPTY:
                    an = INT_ARRAY_EMPTY;
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    an = new int[cElements];
                    for (int i = 0; i < cElements; ++i)
                        {
                        an[i] = readAsInt(in, in.readPackedInt());
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    an = new int[cElements];
                    switch (nElementType)
                        {
                        case T_INT16:
                        case T_INT32:
                        case T_INT64:
                        case T_INT128:
                            for (int i = 0; i < cElements; ++i)
                                {
                                an[i] = in.readPackedInt();
                                }
                            break;

                        case T_OCTET:  // raw encoding (since 24.09)
                            int        cb = cElements * 4;
                            ByteBuffer bb = in.readBuffer(cb).toByteBuffer();
                            bb.asIntBuffer().get(an, 0, cElements);
                            break;

                        default:
                            for (int i = 0; i < cElements; ++i)
                                {
                                an[i] = readAsInt(in, nElementType);
                                }
                            break;
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    an = new int[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        an[iElement] = readAsInt(in, in.readPackedInt());
                        }
                    while (--cElements >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    an = new int[cElements];
                    switch (nElementType)
                        {
                        case T_INT16:
                        case T_INT32:
                        case T_INT64:
                        case T_INT128:
                            do
                                {
                                int iElement = in.readPackedInt();
                                if (iElement < 0)
                                    {
                                    break;
                                    }
                                an[iElement] = in.readPackedInt();
                                }
                            while (--cElements >= 0);
                            break;

                        default:
                            do
                                {
                                int iElement = in.readPackedInt();
                                if (iElement < 0)
                                    {
                                    break;
                                    }
                                an[iElement] = readAsInt(in, nElementType);
                                }
                            while (--cElements >= 0);
                            break;
                        }
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return an;
        }

    /**
    * {@inheritDoc}
    */
    public long[] readLongArray(int iProp)
            throws IOException
        {
        long[] an = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    an = readLongArray(iProp);
                    registerIdentity(nId, an);
                    }
                    break;

                case T_REFERENCE:
                    an = (long[]) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_COLLECTION_EMPTY:
                    an = LONG_ARRAY_EMPTY;
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    an = new long[cElements];
                    for (int i = 0; i < cElements; ++i)
                        {
                        an[i] = readAsLong(in, in.readPackedInt());
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    an = new long[cElements];
                    switch (nElementType)
                        {
                        case T_INT16:
                        case T_INT32:
                        case T_INT64:
                        case T_INT128:
                            for (int i = 0; i < cElements; ++i)
                                {
                                an[i] = in.readPackedLong();
                                }
                            break;

                        case T_OCTET:  // raw encoding (since 24.09)
                            int        cb = cElements * 8;
                            ByteBuffer bb = in.readBuffer(cb).toByteBuffer();
                            bb.asLongBuffer().get(an, 0, cElements);
                            break;

                        default:
                            for (int i = 0; i < cElements; ++i)
                                {
                                an[i] = readAsLong(in, nElementType);
                                }
                            break;
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    an = new long[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        an[iElement] = readAsLong(in, in.readPackedInt());
                        }
                    while (--cElements >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    an = new long[cElements];
                    switch (nElementType)
                        {
                        case T_INT16:
                        case T_INT32:
                        case T_INT64:
                        case T_INT128:
                            do
                                {
                                int iElement = in.readPackedInt();
                                if (iElement < 0)
                                    {
                                    break;
                                    }
                                an[iElement] = in.readPackedLong();
                                }
                            while (--cElements >= 0);
                            break;

                        default:
                            do
                                {
                                int iElement = in.readPackedInt();
                                if (iElement < 0)
                                    {
                                    break;
                                    }
                                an[iElement] = readAsLong(in, nElementType);
                                }
                            while (--cElements >= 0);
                            break;
                        }
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return an;
        }

    /**
    * {@inheritDoc}
    */
    public float[] readFloatArray(int iProp)
            throws IOException
        {
        float[] afl = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    afl = readFloatArray(iProp);
                    registerIdentity(nId, afl);
                    }
                    break;

                case T_REFERENCE:
                    afl = (float[]) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_COLLECTION_EMPTY:
                    afl = FLOAT_ARRAY_EMPTY;
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    afl = new float[cElements];
                    for (int i = 0; i < cElements; ++i)
                        {
                        afl[i] = readAsFloat(in, in.readPackedInt());
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    afl = new float[cElements];

                    if (nElementType == T_OCTET)
                        {
                        // raw encoding (since 24.09)
                        int        cb = cElements * 4;
                        ByteBuffer bb = in.readBuffer(cb).toByteBuffer();
                        bb.asFloatBuffer().get(afl, 0, cElements);
                        }
                    else
                        {
                        for (int i = 0; i < cElements; ++i)
                            {
                            afl[i] = nElementType == T_FLOAT32
                                     ? in.readFloat()
                                     : readAsFloat(in, nElementType);
                            }
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    afl = new float[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        afl[iElement] = readAsFloat(in, in.readPackedInt());
                        }
                    while (--cElements >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    afl = new float[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        afl[iElement] = nElementType == T_FLOAT32
                                 ? in.readFloat()
                                 : readAsFloat(in, nElementType);
                        }
                    while (--cElements >= 0);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return afl;
        }

    /**
    * {@inheritDoc}
    */
    public double[] readDoubleArray(int iProp)
            throws IOException
        {
        double[] adfl = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    adfl = readDoubleArray(iProp);
                    registerIdentity(nId, adfl);
                    }
                    break;

                case T_REFERENCE:
                    adfl = (double[]) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_COLLECTION_EMPTY:
                    adfl = DOUBLE_ARRAY_EMPTY;
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    adfl = new double[cElements];
                    for (int i = 0; i < cElements; ++i)
                        {
                        adfl[i] = readAsDouble(in, in.readPackedInt());
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    adfl = new double[cElements];

                    if (nElementType == T_OCTET)
                        {
                        // raw encoding (since 24.09)
                        int        cb = cElements * 8;
                        ByteBuffer bb = in.readBuffer(cb).toByteBuffer();
                        bb.asDoubleBuffer().get(adfl, 0, cElements);
                        }
                    else
                        {
                        for (int i = 0; i < cElements; ++i)
                            {
                            adfl[i] = nElementType == T_FLOAT64
                                      ? in.readDouble()
                                      : readAsDouble(in, nElementType);
                            }
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int cElements = in.readPackedInt();
                    adfl = new double[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        adfl[iElement] = readAsDouble(in, in.readPackedInt());
                        }
                    while (--cElements >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int cElements    = in.readPackedInt();
                    adfl = new double[cElements];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        adfl[iElement] = nElementType == T_FLOAT64
                                  ? in.readDouble()
                                  : readAsDouble(in, nElementType);
                        }
                    while (--cElements >= 0);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return adfl;
        }

    /**
    * {@inheritDoc}
    */
    public BigInteger readBigInteger(int iProp)
            throws IOException
        {
        BigInteger n = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    n = readBigInteger(iProp);
                    registerIdentity(nId, n);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Number number = (Number) lookupIdentity(in.readPackedInt());
                    n = (BigInteger) convertNumber(number, J_BIG_INTEGER);
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_INT_0:
                    n = BigInteger.ZERO;
                    break;

                default:
                    n = readAsBigInteger(in, nType);
                    break;
                }
            }
        complete(iProp);

        return n;
        }

    /**
    * {@inheritDoc}
    */
    public RawQuad readRawQuad(int iProp)
            throws IOException
        {
        RawQuad qfl = RawQuad.ZERO;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    qfl = readRawQuad(iProp);
                    registerIdentity(nId, qfl);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Number number = (Number) lookupIdentity(in.readPackedInt());
                    qfl = (RawQuad) convertNumber(number, J_QUAD);
                    }
                    break;

                case V_REFERENCE_NULL:
                    qfl = null;
                    break;

                case V_INT_0:
                    break;

                default:
                    qfl = readAsQuad(in, nType);
                    break;
                }
            }
        complete(iProp);

        return qfl;
        }

    /**
    * {@inheritDoc}
    */
    public BigDecimal readBigDecimal(int iProp)
            throws IOException
        {
        BigDecimal dec = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    dec = readBigDecimal(iProp);
                    registerIdentity(nId, dec);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Number number = (Number) lookupIdentity(in.readPackedInt());
                    dec = (BigDecimal) convertNumber(number, J_BIG_DECIMAL);
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_INT_0:
                    dec = BigDecimal.ZERO;
                    break;

                default:
                    dec = readAsBigDecimal(in, nType);
                    break;
                }
            }
        complete(iProp);

        return dec;
        }

    /**
    * {@inheritDoc}
    */
    public Binary readBinary(int iProp)
            throws IOException
        {
        Binary bin = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    bin = readBinary(iProp);
                    registerIdentity(nId, bin);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    bin = o instanceof byte[]
                            ? new Binary((byte[]) o)
                            : (Binary) o;
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    bin = BINARY_EMPTY;
                    break;

                case T_OCTET_STRING:
                    bin = readBinary(in);
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int    cb = in.readPackedInt();
                    byte[] ab = new byte[cb];
                    for (int i = 0; i < cb; ++i)
                        {
                        ab[i] = (byte) readAsInt(in, in.readPackedInt());
                        }
                    bin = new Binary(ab);
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int    cb = in.readPackedInt();
                    byte[] ab = new byte[cb];

                    if (nElementType == T_OCTET)
                        {
                        in.readFully(ab);
                        }
                    else
                        {
                        for (int i = 0; i < cb; ++i)
                            {
                            ab[i] = (byte) readAsInt(in, nElementType);
                            }
                        }
                    bin = new Binary(ab);
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int    cb = in.readPackedInt();
                    byte[] ab = new byte[cb];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        ab[iElement] = (byte) readAsInt(in, in.readPackedInt());
                        }
                    while (--cb >= 0);
                    bin = new Binary(ab);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int    cb = in.readPackedInt();
                    byte[] ab = new byte[cb];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        ab[iElement] = nElementType == T_OCTET
                                       ? in.readByte()
                                       : (byte) readAsInt(in, nElementType);
                        }
                    while (--cb >= 0);
                    bin = new Binary(ab);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Binary type");
                }
            }
        complete(iProp);

        return bin;
        }

    /**
    * {@inheritDoc}
    */
    public String readString(int iProp)
            throws IOException
        {
        String s = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    s = readString(iProp);
                    registerIdentity(nId, s);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    if (o instanceof byte[])
                        {
                        s = new String((byte[]) o, 0);
                        }
                    else if (o instanceof Binary)
                        {
                        s = new String(((Binary) o).toByteArray(), 0);
                        }
                    else
                        {
                        s = (String) o;
                        }
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    s = "";
                    break;

                case T_OCTET_STRING:
                    {
                    int cb = in.readPackedInt();
                    int of = in.getOffset();

                    ReadBuffer buf = in.getBuffer();
                    if (buf == null)
                        {
                        s = new String(in.readBuffer(cb).toByteArray(), 0);
                        }
                    else
                        {
                        in.skipBytes(cb);
                        Binary bin = in.getBuffer().toBinary(of, cb);
                        s = new String(bin.toByteArray(), 0);
                        }
                    }
                    break;

                case T_CHAR_STRING:
                    s = in.readSafeUTF();
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    int    cch = in.readPackedInt();
                    char[] ach = new char[cch];
                    for (int i = 0; i < cch; ++i)
                        {
                        ach[i] = readAsChar(in, in.readPackedInt());
                        }
                    s = new String(ach);
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int    cch = in.readPackedInt();
                    char[] ach = new char[cch];
                    for (int i = 0; i < cch; ++i)
                        {
                        ach[i] = readAsChar(in, nElementType);
                        }
                    s = new String(ach);
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int    cch = in.readPackedInt();
                    char[] ach = new char[cch];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        ach[iElement] = readAsChar(in, in.readPackedInt());
                        }
                    while (--cch >= 0);
                    s = new String(ach);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int    cch = in.readPackedInt();
                    char[] ach = new char[cch];
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        ach[iElement] = readAsChar(in, nElementType);
                        }
                    while (--cch >= 0);
                    s = new String(ach);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a String type");
                }
            }
        complete(iProp);

        return s;
        }

    /**
    * {@inheritDoc}
    */
    public Date readDate(int iProp)
            throws IOException
        {
        Date date = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    date = readDate(iProp);
                    registerIdentity(nId, date);
                    }
                    break;

                case T_REFERENCE:
                    date = convertToDate(lookupIdentity(in.readPackedInt()));
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_DATE:
                    date = convertToDate(readRawDate(in));
                    break;

                case T_TIME:
                    date = convertToDate(readRawTime(in));
                    break;

                case T_DATETIME:
                    date = convertToDate(new RawDateTime(readRawDate(in), readRawTime(in)));
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Java Date type");
                }
            }
        complete(iProp);

        return date;
        }

    @Override
    public LocalDate readLocalDate(int iProp) throws IOException
        {
        LocalDate date = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    date = readLocalDate(iProp);
                    registerIdentity(nId, date);
                    }
                    break;

                case T_REFERENCE:
                    date = (LocalDate) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_DATE:
                    date = readLocalDate(in);
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Java LocalDate type");
                }
            }
        complete(iProp);

        return date;
        }

    @Override
    public LocalDateTime readLocalDateTime(int iProp) throws IOException
        {
        LocalDateTime dt = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    dt = readLocalDateTime(iProp);
                    registerIdentity(nId, dt);
                    }
                    break;

                case T_REFERENCE:
                    dt = (LocalDateTime) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_DATETIME:
                    dt = readLocalDateTime(in);
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Java LocalDateTime type");
                }
            }
        complete(iProp);

        return dt;
        }

    @Override
    public LocalTime readLocalTime(int iProp) throws IOException
        {
        LocalTime time = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    time = readLocalTime(iProp);
                    registerIdentity(nId, time);
                    }
                    break;

                case T_REFERENCE:
                    time = (LocalTime) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_TIME:
                    time = readLocalTime(in);
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Java LocalTime type");
                }
            }
        complete(iProp);

        return time;
        }

    @Override
    public OffsetDateTime readOffsetDateTime(int iProp) throws IOException
        {
        OffsetDateTime dt = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    dt = readOffsetDateTime(iProp);
                    registerIdentity(nId, dt);
                    }
                    break;

                case T_REFERENCE:
                    dt = (OffsetDateTime) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_DATETIME:
                    dt = readOffsetDateTime(in);
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Java OffsetDateTime type");
                }
            }
        complete(iProp);

        return dt;
        }

    @Override
    public OffsetTime readOffsetTime(int iProp) throws IOException
        {
        OffsetTime time = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    time = readOffsetTime(iProp);
                    registerIdentity(nId, time);
                    }
                    break;

                case T_REFERENCE:
                    time = (OffsetTime) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_TIME:
                    time = readOffsetTime(in);
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Java OffsetTime type");
                }
            }
        complete(iProp);

        return time;
        }

    /**
    * {@inheritDoc}
    */
    public RawDate readRawDate(int iProp)
            throws IOException
        {
        RawDate date = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    date = readRawDate(iProp);
                    registerIdentity(nId, date);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    if (o instanceof Date)
                        {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime((Date) o);
                        date = new RawDate(
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH) + 1,
                                calendar.get(Calendar.DAY_OF_MONTH));
                        }
                    else if (o instanceof RawDateTime)
                        {
                        date = ((RawDateTime) o).getRawDate();
                        }
                    else
                        {
                        date = (RawDate) o;
                        }
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_DATE:
                    date = readRawDate(in);
                    break;

                case T_DATETIME:
                    {
                    // read the date portion
                    date = readRawDate(in);

                    // skip the time portion
                    skipPackedInts(in, 4);
                    int nZoneType = in.readPackedInt();
                    if (nZoneType == 2)
                        {
                        skipPackedInts(in, 2);
                        }
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a RawDate type");
                }
            }
        complete(iProp);

        return date;
        }

    /**
    * {@inheritDoc}
    */
    public RawTime  readRawTime(int iProp)
            throws IOException
        {
        RawTime time = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    time = readRawTime(iProp);
                    registerIdentity(nId, time);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    if (o instanceof Date)
                        {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime((Date) o);
                        time = new RawTime(
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                calendar.get(Calendar.SECOND),
                                calendar.get(Calendar.MILLISECOND) * 1000000,
                                false);
                        }
                    else if (o instanceof RawDateTime)
                        {
                        time = ((RawDateTime) o).getRawTime();
                        }
                    else
                        {
                        time = (RawTime) o;
                        }
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_DATETIME:
                    // skip the date portion
                    skipPackedInts(in, 3);
                    // fall through
                case T_TIME:
                    time = readRawTime(in);
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a RawTime type");
                }
            }
        complete(iProp);

        return time;
        }

    /**
    * {@inheritDoc}
    */
    public RawDateTime readRawDateTime(int iProp)
            throws IOException
        {
        RawDateTime dt = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    dt = readRawDateTime(iProp);
                    registerIdentity(nId, dt);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    if (o instanceof Date)
                        {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime((Date) o);
                        dt = new RawDateTime(
                            new RawDate(
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH) + 1,
                                calendar.get(Calendar.DAY_OF_MONTH)),
                            new RawTime(
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                calendar.get(Calendar.SECOND),
                                calendar.get(Calendar.MILLISECOND) * 1000000,
                                false));
                        }
                    else if (o instanceof RawDate)
                        {
                        dt = new RawDateTime((RawDate) o, new RawTime(0, 0, 0, 0, false));
                        }
                    else
                        {
                        dt = (RawDateTime) o;
                        }
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_DATE:
                    dt = new RawDateTime(readRawDate(in), new RawTime(0, 0, 0, 0, false));
                    break;

                case T_DATETIME:
                    dt = new RawDateTime(readRawDate(in), readRawTime(in));
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a RawDateTime type");
                }
            }
        complete(iProp);

        return dt;
        }

    /**
    * {@inheritDoc}
    */
    public RawYearMonthInterval readRawYearMonthInterval(int iProp)
            throws IOException
        {
        RawYearMonthInterval interval = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    interval = readRawYearMonthInterval(iProp);
                    registerIdentity(nId, interval);
                    }
                    break;

                case T_REFERENCE:
                    interval = (RawYearMonthInterval) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_YEAR_MONTH_INTERVAL:
                    {
                    int cYears  = in.readPackedInt();
                    int cMonths = in.readPackedInt();
                    interval = new RawYearMonthInterval(cYears, cMonths);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a RawYearMonthInterval type");
                }
            }
        complete(iProp);

        return interval;
        }

    /**
    * {@inheritDoc}
    */
    public RawTimeInterval readRawTimeInterval(int iProp)
            throws IOException
        {
        RawTimeInterval interval = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    interval = readRawTimeInterval(iProp);
                    registerIdentity(nId, interval);
                    }
                    break;

                case T_REFERENCE:
                    interval = (RawTimeInterval) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_TIME_INTERVAL:
                    int cHours   = in.readPackedInt();
                    int cMinutes = in.readPackedInt();
                    int cSeconds = in.readPackedInt();
                    int cNanos   = in.readPackedInt();
                    interval = new RawTimeInterval(cHours, cMinutes, cSeconds, cNanos);
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a RawTimeInterval type");
                }
            }
        complete(iProp);

        return interval;
        }

    /**
    * {@inheritDoc}
    */
    public RawDayTimeInterval readRawDayTimeInterval(int iProp)
            throws IOException
        {
        RawDayTimeInterval interval = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    interval = readRawDayTimeInterval(iProp);
                    registerIdentity(nId, interval);
                    }
                    break;

                case T_REFERENCE:
                    interval = (RawDayTimeInterval) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                case T_DAY_TIME_INTERVAL:
                    int cDays    = in.readPackedInt();
                    int cHours   = in.readPackedInt();
                    int cMinutes = in.readPackedInt();
                    int cSeconds = in.readPackedInt();
                    int cNanos   = in.readPackedInt();
                    interval = new RawDayTimeInterval(
                            cDays, cHours, cMinutes, cSeconds, cNanos);
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a RawDayTimeInterval type");
                }
            }
        complete(iProp);

        return interval;
        }

    /**
    * {@inheritDoc}
    */
    public Object[] readObjectArray(int iProp, Object[] ao)
            throws IOException
        {
        Object[] aoResult = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    aoResult = readObjectArray(iProp, ao);
                    registerIdentity(nId, aoResult);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    if (o instanceof Collection)
                        {
                        aoResult = ((Collection) o).toArray(ao);
                        }
                    else if (o instanceof LongArray)
                        {
                        LongArray array     = (LongArray) o;
                        long      cElements = array.getLastIndex() + 1;

                        if (array.getFirstIndex() < 0L)
                            {
                            throw new ArrayIndexOutOfBoundsException(
                                    "index=" + array.getFirstIndex());
                            }
                        if (cElements > Integer.MAX_VALUE)
                            {
                            throw new ArrayIndexOutOfBoundsException(
                                    "index=" + array.getLastIndex());
                            }

                        aoResult = resizeArray(ao, (int) cElements);
                        for (LongArray.Iterator iter = array.iterator(); iter.hasNext(); )
                            {
                            Object oValue = iter.next();
                            aoResult[(int) iter.getIndex()] = oValue;
                            }
                        }
                    else
                        {
                        aoResult = (Object[]) o;
                        }
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    aoResult = OBJECT_ARRAY_EMPTY;
                    break;

                default:
                    aoResult = readAsObjectArray(iProp, nType, ao);
                    break;
                }
            }
        complete(iProp);

        return aoResult;
        }

    /**
    * {@inheritDoc}
    */
    public <T> T[] readArray(int iProp, IntFunction<T[]> supplier)
            throws IOException
        {
        T[] aoResult = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    aoResult = readArray(iProp, supplier);
                    registerIdentity(nId, aoResult);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    if (o instanceof Collection)
                        {
                        Collection<T> col = (Collection<T>) o;
                        aoResult = col.toArray(supplier.apply(col.size()));
                        }
                    else if (o instanceof LongArray)
                        {
                        LongArray array     = (LongArray) o;
                        long      cElements = array.getLastIndex() + 1;

                        if (array.getFirstIndex() < 0L)
                            {
                            throw new ArrayIndexOutOfBoundsException(
                                    "index=" + array.getFirstIndex());
                            }
                        if (cElements > Integer.MAX_VALUE)
                            {
                            throw new ArrayIndexOutOfBoundsException(
                                    "index=" + array.getLastIndex());
                            }

                        aoResult = supplier.apply((int) cElements);
                        for (LongArray.Iterator iter = array.iterator(); iter.hasNext(); )
                            {
                            T value = (T) iter.next();
                            aoResult[(int) iter.getIndex()] = value;
                            }
                        }
                    else
                        {
                        aoResult = (T[]) o;
                        }
                    }
                    break;

                case V_REFERENCE_NULL:
                    break;

                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    aoResult = (T[]) OBJECT_ARRAY_EMPTY;
                    break;

                default:
                    aoResult = readAsTypedObjectArray(iProp, nType, supplier);
                    break;
                }
            }
        complete(iProp);

        return aoResult;
        }

    /**
    * {@inheritDoc}
    */
    public LongArray readLongArray(int iProp, LongArray array)
            throws IOException
        {
        // do not default to null, since the caller is passing in a mutable
        // LongArray

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    array = readLongArray(iProp, array);
                    registerIdentity(nId, array);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    if (o instanceof Collection)
                        {
                        if (array == null)
                            {
                            array = new SparseArray();
                            }
                        int i = 0;
                        for (Iterator iter = ((Collection) o).iterator(); iter.hasNext(); )
                            {
                            array.set(++i, iter.next());
                            }
                        }
                    else
                        {
                        array = (LongArray) o;
                        }
                    }
                    break;

                case V_REFERENCE_NULL:
                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    if (array == null)
                        {
                        array = new SparseArray();
                        }

                    int co = in.readPackedInt();
                    for (int i = 0; i < co; ++i)
                        {
                        array.set(i, readAsObject(in.readPackedInt()));
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    if (array == null)
                        {
                        array = new SparseArray();
                        }

                    int nElementType = in.readPackedInt();
                    int co           = in.readPackedInt();
                    for (int i = 0; i < co; ++i)
                        {
                        array.set(i, readAsUniformObject(nElementType));
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    if (array == null)
                        {
                        array = new SparseArray();
                        }

                    int co = in.readPackedInt();
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        array.set(iElement, readAsObject(in.readPackedInt()));
                        }
                    while (--co >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    if (array == null)
                        {
                        array = new SparseArray();
                        }

                    int nElementType = in.readPackedInt();
                    int co           = in.readPackedInt();
                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        array.set(iElement, readAsUniformObject(nElementType));
                        }
                    while (--co >= 0);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                }
            }
        complete(iProp);

        return array;
        }

    /**
    * {@inheritDoc}
    */
    public <T, C extends Collection<T>> C readCollection(int iProp, C coll)
            throws IOException
        {
        // do not default to null, since the caller is passing in a mutable
        // Collection

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    coll = readCollection(iProp, coll);
                    registerIdentity(nId, coll);
                    }
                    break;

                case T_REFERENCE:
                    {
                    Object o = lookupIdentity(in.readPackedInt());
                    if (o instanceof Object[])
                        {
                        Collection collData =
                            new ImmutableArrayList((Object[]) o).getList();
                        if (coll == null)
                            {
                            coll = (C) collData;
                            }
                        else
                            {
                            coll.addAll(collData);
                            }
                        }
                    else
                        {
                        coll = (C) o;
                        }
                    }
                    break;

                case V_REFERENCE_NULL:
                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    break;

                case T_COLLECTION:
                case T_ARRAY:
                    {
                    if (coll == null)
                        {
                        Object[] ao = readAsObjectArray(iProp, nType, null);
                        coll = (C) new ImmutableArrayList(ao).getList();
                        }
                    else
                        {
                        int co = in.readPackedInt();
                        for (int i = 0; i < co; ++i)
                            {
                            coll.add((T) readAsObject(in.readPackedInt()));
                            }
                        }
                    }
                    break;

                case T_UNIFORM_COLLECTION:
                case T_UNIFORM_ARRAY:
                    {
                    if (coll == null)
                        {
                        Object[] ao = readAsObjectArray(iProp, nType, null);
                        coll = (C) new ImmutableArrayList(ao).getList();
                        }
                    else
                        {
                        int nElementType = in.readPackedInt();
                        int co           = in.readPackedInt();
                        for (int i = 0; i < co; ++i)
                            {
                            coll.add((T) readAsUniformObject(nElementType));
                            }
                        }
                    }
                    break;

                case T_SPARSE_ARRAY:
                    {
                    int co = in.readPackedInt();
                    if (coll == null)
                        {
                        coll = (C) new ArrayList<>(co);
                        }

                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        coll.add((T) readAsObject(in.readPackedInt()));
                        }
                    while (--co >= 0);
                    }
                    break;

                case T_UNIFORM_SPARSE_ARRAY:
                    {
                    int nElementType = in.readPackedInt();
                    int co           = in.readPackedInt();
                    if (coll == null)
                        {
                        coll = (C) new ArrayList<>(co);
                        }

                    do
                        {
                        int iElement = in.readPackedInt();
                        if (iElement < 0)
                            {
                            break;
                            }
                        coll.add((T) readAsUniformObject(nElementType));
                        }
                    while (--co >= 0);
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Collection type");
                }
            }
        complete(iProp);

        return coll;
        }

    /**
    * {@inheritDoc}
    */
    public <K, V, M extends Map<K, V>> M readMap(int iProp, M map)
            throws IOException
        {
        // do not default to null, since the caller is passing in a mutable
        // Map

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    map = readMap(iProp, map);
                    registerIdentity(nId, map);
                    }
                    break;

                case T_REFERENCE:
                    map = (M) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                case V_STRING_ZERO_LENGTH:
                case V_COLLECTION_EMPTY:
                    break;

                case T_MAP:
                    {
                    int cEntries = in.readPackedInt();
                    if (map == null)
                        {
                        map = (M) new HashMap(cEntries);
                        }

                    for (int i = 0; i < cEntries; ++i)
                        {
                        K key = (K) readAsObject(in.readPackedInt());
                        V val = (V) readAsObject(in.readPackedInt());
                        map.put(key, val);
                        }
                    }
                    break;

                case T_UNIFORM_KEYS_MAP:
                    {
                    int nKeyType = in.readPackedInt();
                    int cEntries = in.readPackedInt();
                    if (map == null)
                        {
                        map = (M) new HashMap(cEntries);
                        }

                    for (int i = 0; i < cEntries; ++i)
                        {
                        K key = (K) readAsUniformObject(nKeyType);
                        V val = (V) readAsObject(in.readPackedInt());
                        map.put(key, val);
                        }
                    }
                    break;

                case T_UNIFORM_MAP:
                    {
                    int nKeyType = in.readPackedInt();
                    int nValType = in.readPackedInt();
                    int cEntries = in.readPackedInt();
                    if (map == null)
                        {
                        map = (M) new HashMap(cEntries);
                        }

                    for (int i = 0; i < cEntries; ++i)
                        {
                        K key = (K) readAsUniformObject(nKeyType);
                        V val = (V) readAsUniformObject(nValType);
                        map.put(key, val);
                        }
                    }
                    break;

                default:
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to a Map type");
                }
            }
        complete(iProp);

        return map;
        }

    /**
    * {@inheritDoc}
    */
    public PofContext getPofContext()
        {
        return m_ctx;
        }

    /**
    * {@inheritDoc}
    */
    public void setPofContext(PofContext ctx)
        {
        if (ctx == null)
            {
            throw new IllegalArgumentException("PofContext cannot be null");
            }
        m_ctx = ctx;
        }

    /**
    * {@inheritDoc}
    */
    public <T> T readObject(int iProp)
            throws IOException
        {
        T o = null;

        if (advanceTo(iProp))
            {
            ReadBuffer.BufferInput in = m_in;
            int nType = in.readPackedInt();
            switch (nType)
                {
                case T_IDENTITY:
                    {
                    int nId = in.readPackedInt();
                    IdentityHolder.set(this, nId);
                    o = readObject(iProp);
                    IdentityHolder.reset(this, nId, o);
                    }
                    break;

                case T_REFERENCE:
                    o = (T) lookupIdentity(in.readPackedInt());
                    break;

                case V_REFERENCE_NULL:
                    break;

                default:
                    o = (T) readAsObject(nType);
                    break;
                }
            }
        complete(iProp);

        return o;
        }

    /**
    * {@inheritDoc}
    */
    public int getUserTypeId()
        {
        return -1;
        }

    /**
    * {@inheritDoc}
    */
    public int getVersionId()
        {
        throw new IllegalStateException("not in a user type");
        }

    /**
    * {@inheritDoc}
    */
    public void registerIdentity(Object o)
        {
        throw new IllegalStateException("not in a user type");
        }

    /**
    * {@inheritDoc}
    */
    public PofReader createNestedPofReader(int iProp)
            throws IOException
        {
        throw new IllegalStateException("not in a user type");
        }

    /**
    * {@inheritDoc}
    */
    public Binary readRemainder()
            throws IOException
        {
        throw new IllegalStateException("not in a user type");
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Advance through the POF stream until the specified property is found.
    * If the property is found, return true, otherwise return false and
    * advance to the first property that follows the specified property.
    *
    * @param iProp  the index of the property to advance to
    *
    * @return true if the property is found
    *
    * @throws IllegalStateException if the POF stream has already advanced
    *         past the desired property
    * @throws IOException  if an I/O error occurs
    */
    protected boolean advanceTo(int iProp)
            throws IOException
        {
        if (iProp > 0)
            {
            throw new IllegalStateException();
            }

        return true;
        }

    /**
    * Register the completion of the parsing of a value.
    *
    * @param iProp  the property index
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void complete(int iProp)
            throws IOException
        {
        }

    /**
    * If this parser is contextually within a user type, obtain the parser
    * which created this parser in order to parse the user type.
    *
    * @return the parser for the context within which this parser is
    *         operating
    */
    protected PofBufferReader getParentParser()
        {
        return null;
        }

    /**
    * Obtain the registry for identity-reference pairs, creating it if
    * necessary.
    *
    * @return the identity-reference registry, never null
    */
    protected LongArray ensureReferenceRegistry()
        {
        LongArray array = m_arrayRefs;

        if (array == null)
            {
            PofBufferReader parent = getParentParser();
            m_arrayRefs = array = parent == null
                    ? new SparseArray()
                    : parent.ensureReferenceRegistry();
            }

        return array;
        }

    /**
    * Register the passed value with the passed identity.
    *
    * @param nId     the identity within the POF stream of the object
    * @param oValue  the object to associate with the passed identity
    *
    * @throws IllegalArgumentException  if the specified identity is already
    *                                   registered with a different object
    */
    protected void registerIdentity(int nId, Object oValue)
        {
        if (nId >= 0)
            {
            LongArray array = ensureReferenceRegistry();
            Object    o     = array.get(nId);
            if (o != null && o != oValue)
                {
                throw new IllegalArgumentException("duplicate identity: " + nId);
                }

            array.set(nId, oValue);
            }
        }

    /**
    * Look up the specified identity and return the object to which it
    * refers.
    *
    * @param nId  the identity
    *
    * @return the object registered under that identity
    *
    * @throws IOException  if the requested identity is not registered
    */
    protected Object lookupIdentity(int nId)
            throws IOException
        {
        LongArray array = ensureReferenceRegistry();
        if (!array.exists(nId))
            {
            throw new IOException("missing identity: " + nId);
            }

        return array.get(nId);
        }

    /**
    * Read a POF value as an Object.
    *
    * @param nType  the type identifier of the value
    *
    * @return an Object value
    *
    * @throws IOException  if an I/O error occurs
    */
    protected Object readAsObject(int nType)
            throws IOException
        {
        Object o = null;

        ReadBuffer.BufferInput in = m_in;
        switch (nType)
            {
            case T_INT16:
                o = (short) in.readPackedInt();
                break;

            case T_INT32:
            case V_INT_NEG_1:
            case V_INT_0:
            case V_INT_1:
            case V_INT_2:
            case V_INT_3:
            case V_INT_4:
            case V_INT_5:
            case V_INT_6:
            case V_INT_7:
            case V_INT_8:
            case V_INT_9:
            case V_INT_10:
            case V_INT_11:
            case V_INT_12:
            case V_INT_13:
            case V_INT_14:
            case V_INT_15:
            case V_INT_16:
            case V_INT_17:
            case V_INT_18:
            case V_INT_19:
            case V_INT_20:
            case V_INT_21:
            case V_INT_22:
                o = readAsInt(in, nType);
                break;

            case T_INT64:
                o = in.readPackedLong();
                break;

            case T_INT128:
                o = readBigInteger(in);
                break;

            case T_FLOAT32:
                o = in.readFloat();
                break;

            case T_FLOAT64:
                o = in.readDouble();
                break;

            case T_FLOAT128:
                o = readQuad(in);
                break;

            case V_FP_POS_INFINITY:
                o = Double.POSITIVE_INFINITY;
                break;

            case V_FP_NEG_INFINITY:
                o = Double.NEGATIVE_INFINITY;
                break;

            case V_FP_NAN:
                o = Double.NaN;
                break;

            case T_DECIMAL32:
                o = readBigDecimal(in, 4);
                break;

            case T_DECIMAL64:
                o = readBigDecimal(in, 8);
                break;

            case T_DECIMAL128:
                o = readBigDecimal(in, 16);
                break;

            case T_BOOLEAN:
                o = in.readPackedInt() == 0 ? Boolean.FALSE : Boolean.TRUE;
                break;

            case T_OCTET:
                o = in.readByte();
                break;

            case T_OCTET_STRING:
                o = readBinary(in);
                break;

            case T_CHAR:
                o = readChar(in);
                break;

            case T_CHAR_STRING:
                o = in.readSafeUTF();
                break;

            case T_DATE:
                {
                RawDate rawDate = readRawDate(in);
                o = getPofContext().isPreferJavaTime()
                    ? rawDate.toLocalDate()
                    : rawDate.toSqlDate();
                }
                break;

            case T_TIME:
                {
                RawTime rawTime = readRawTime(in);
                o = getPofContext().isPreferJavaTime()
                    ? rawTime.hasTimezone() ? rawTime.toOffsetTime() : rawTime.toLocalTime()
                    : rawTime.toSqlTime();
                }
                break;

            case T_DATETIME:
                {
                RawDateTime rawDateTime = new RawDateTime(readRawDate(in), readRawTime(in));
                o = getPofContext().isPreferJavaTime()
                    ? rawDateTime.getRawTime().hasTimezone()
                      ? rawDateTime.toOffsetDateTime() : rawDateTime.toLocalDateTime()
                    : rawDateTime.toSqlTimestamp();
                }
                break;

            case T_YEAR_MONTH_INTERVAL:
                {
                int cYears  = in.readPackedInt();
                int dMonths = in.readPackedInt();
                o = new RawYearMonthInterval(cYears, dMonths);
                }
                break;

            case T_TIME_INTERVAL:
                {
                int cHours   = in.readPackedInt();
                int cMinutes = in.readPackedInt();
                int cSeconds = in.readPackedInt();
                int cNanos   = in.readPackedInt();
                o = new RawTimeInterval(cHours, cMinutes, cSeconds, cNanos);
                }
                break;

            case T_DAY_TIME_INTERVAL:
                {
                int cDays    = in.readPackedInt();
                int cHours   = in.readPackedInt();
                int cMinutes = in.readPackedInt();
                int cSeconds = in.readPackedInt();
                int cNanos   = in.readPackedInt();
                o = new RawDayTimeInterval(cDays, cHours, cMinutes, cSeconds, cNanos);
                }
                break;

            case T_COLLECTION:
            case T_UNIFORM_COLLECTION:
                o = new ImmutableArrayList(readAsObjectArray(-1, nType, null)).getList();
                break;

            case T_ARRAY:
                o = readAsObjectArray(-1, nType, null);
                break;

            case T_UNIFORM_ARRAY:
                {
                int nElementType = in.readPackedInt();
                int cElements    = in.readPackedInt();
                switch (nElementType)
                    {
                    case T_BOOLEAN:
                        {
                        boolean[] af = new boolean[cElements];
                        for (int i = 0; i < cElements; ++i)
                            {
                            af[i] = in.readPackedInt() != 0;
                            }
                        o = af;
                        }
                        break;

                    case T_OCTET:
                        {
                        byte[] ab = new byte[cElements];
                        in.readFully(ab);
                        o = ab;
                        }
                        break;

                    case T_CHAR:
                        {
                        char[] ach = new char[cElements];
                        for (int i = 0; i < cElements; ++i)
                            {
                            ach[i] = readChar(in);
                            }
                        o = ach;
                        }
                        break;

                    case T_INT16:
                        {
                        short[] an = new short[cElements];
                        for (int i = 0; i < cElements; ++i)
                            {
                            an[i] = (short) in.readPackedInt();
                            }
                        o = an;
                        }
                        break;

                    case T_INT32:
                        {
                        int[] an = new int[cElements];
                        for (int i = 0; i < cElements; ++i)
                            {
                            an[i] = in.readPackedInt();
                            }
                        o = an;
                        }
                        break;

                    case T_INT64:
                        {
                        long[] an = new long[cElements];
                        for (int i = 0; i < cElements; ++i)
                            {
                            an[i] = in.readPackedLong();
                            }
                        o = an;
                        }
                        break;

                    case T_FLOAT32:
                        {
                        float[] afl = new float[cElements];
                        for (int i = 0; i < cElements; ++i)
                            {
                            afl[i] = in.readFloat();
                            }
                        o = afl;
                        }
                        break;

                    case T_FLOAT64:
                        {
                        double[] adfl = new double[cElements];
                        for (int i = 0; i < cElements; ++i)
                            {
                            adfl[i] = in.readDouble();
                            }
                        o = adfl;
                        }
                        break;

                    default:
                        {
                        Object[] ao = new Object[cElements];
                        for (int i = 0; i < cElements; ++i)
                            {
                            ao[i] = readAsUniformObject(nElementType);
                            }
                        o = ao;
                        }
                    }
                }
                break;

            case T_SPARSE_ARRAY:
                {
                LongArray array     = new SparseArray();
                int       cElements = in.readPackedInt();
                do
                    {
                    int iElement = in.readPackedInt();
                    if (iElement < 0)
                        {
                        break;
                        }
                    array.set(iElement, readAsObject(in.readPackedInt()));
                    }
                while (--cElements >= 0);
                o = array;
                }
                break;

            case T_UNIFORM_SPARSE_ARRAY:
                {
                int nElementType = in.readPackedInt();
                int cElements    = in.readPackedInt();
                switch (nElementType)
                    {
                    case T_BOOLEAN:
                        {
                        boolean[] af = new boolean[cElements];
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            af[iElement] = in.readPackedInt() != 0;
                            }
                        while (--cElements >= 0);
                        o = af;
                        }
                        break;

                    case T_OCTET:
                        {
                        byte[] ab = new byte[cElements];
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            ab[iElement] = in.readByte();
                            }
                        while (--cElements >= 0);
                        o = ab;
                        }
                        break;

                    case T_CHAR:
                        {
                        char[] ach = new char[cElements];
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            ach[iElement] = readChar(in);
                            }
                        while (--cElements >= 0);
                        o = ach;
                        }
                        break;

                    case T_INT16:
                        {
                        short[] an = new short[cElements];
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            an[iElement] = (short) in.readPackedInt();
                            }
                        while (--cElements >= 0);
                        o = an;
                        }
                        break;

                    case T_INT32:
                        {
                        int[] an = new int[cElements];
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            an[iElement] = in.readPackedInt();
                            }
                        while (--cElements >= 0);
                        o = an;
                        }
                        break;

                    case T_INT64:
                        {
                        long[] an = new long[cElements];
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            an[iElement] = in.readPackedLong();
                            }
                        while (--cElements >= 0);
                        o = an;
                        }
                        break;

                    case T_FLOAT32:
                        {
                        float[] afl = new float[cElements];
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            afl[iElement] = in.readFloat();
                            }
                        while (--cElements >= 0);
                        o = afl;
                        }
                        break;

                    case T_FLOAT64:
                        {
                        double[] adfl = new double[cElements];
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            adfl[iElement] = in.readDouble();
                            }
                        while (--cElements >= 0);
                        o = adfl;
                        }
                        break;

                    default:
                        {
                        LongArray array = new SparseArray();
                        do
                            {
                            int iElement = in.readPackedInt();
                            if (iElement < 0)
                                {
                                break;
                                }
                            array.set(iElement, readAsUniformObject(nElementType));
                            }
                        while (--cElements >= 0);
                        o = array;
                        }
                    }
                }
                break;

            case T_MAP:
                {
                Map map      = new HashMap();
                int cEntries = in.readPackedInt();
                for (int i = 0; i < cEntries; ++i)
                    {
                    Object oKey = readAsObject(in.readPackedInt());
                    Object oVal = readAsObject(in.readPackedInt());
                    map.put(oKey, oVal);
                    }
                o = map;
                }
                break;

            case T_UNIFORM_KEYS_MAP:
                {
                Map map      = new HashMap();
                int nKeyType = in.readPackedInt();
                int cEntries = in.readPackedInt();
                for (int i = 0; i < cEntries; ++i)
                    {
                    Object oKey = readAsUniformObject(nKeyType);
                    Object oVal = readAsObject(in.readPackedInt());
                    map.put(oKey, oVal);
                    }
                o = map;
                }
                break;

            case T_UNIFORM_MAP:
                {
                Map map      = new HashMap();
                int nKeyType = in.readPackedInt();
                int nValType = in.readPackedInt();
                int cEntries = in.readPackedInt();
                for (int i = 0; i < cEntries; ++i)
                    {
                    Object oKey = readAsUniformObject(nKeyType);
                    Object oVal = readAsUniformObject(nValType);
                    map.put(oKey, oVal);
                    }
                o = map;
                }
                break;

            case T_IDENTITY:
                {
                int nId = in.readPackedInt();
                nType = in.readPackedInt();
                IdentityHolder.set(this, nId);
                o = readAsObject(nType);
                IdentityHolder.reset(this, nId, o);
                }
                break;

            case T_REFERENCE:
                o = lookupIdentity(in.readPackedInt());
                break;

            case V_BOOLEAN_FALSE:
                o = Boolean.FALSE;
                break;

            case V_BOOLEAN_TRUE:
                o = Boolean.TRUE;
                break;

            case V_STRING_ZERO_LENGTH:
                o = "";
                break;

            case V_COLLECTION_EMPTY:
                o = COLLECTION_EMPTY;
                break;

            case V_REFERENCE_NULL:
                break;

            default:
                {
                if (nType < 0)
                    {
                    throw new StreamCorruptedException("illegal type " 
                            + PofConstants.getTypeName(nType));
                    }

                PofContext    ctx = getPofContext();
                PofSerializer ser;
                try
                    {
                    ser = ctx.getPofSerializer(nType);
                    }
                catch (IllegalArgumentException e)
                    {
                    throw new StreamCorruptedException(e.getMessage());
                    }
                PofReader reader = new PofBufferReader.UserTypeReader(
                        this, in, ctx, nType, in.readPackedInt());

                o = ExternalizableHelper.realize(ser.deserialize(reader), m_ctx);
                }
                break;
            }

        return o;
        }

    /**
    * Read a POF value in a uniform array/map as an Object.
    *
    * @param nType  the type identifier of the value
    *
    * @return an Object value
    *
    * @throws IOException  if an I/O error occurs
    */
    protected Object readAsUniformObject(int nType)
            throws IOException
        {
        if (nType < 0)
            {
            return readAsObject(nType);
            }

        ReadBuffer.BufferInput in  = m_in;
        int                    of  = in.getOffset();
        int                    nId = -1;
        if (of == 0)
            {
            return readAsObject(nType);
            }

        int nValue = in.readPackedInt();
        if (nValue == T_IDENTITY)
            {
            nId = in.readPackedInt();
            IdentityHolder.set(this, nId);
            }
        else
            {
            // it can only be reference if its data type supports reference
            // (a user defined object)
            if (nValue > 0 && nType >= 0)
                {
                Object o = ensureReferenceRegistry().get(nValue);
                if (o != null)
                    {
                    // double check the object type
                    int nTypeId = PofHelper.getPofTypeId(o.getClass(), getPofContext());
                    if (nTypeId == nType)
                        {
                        return o;
                        }
                    }
                }
            in.setOffset(of);
            }

        Object o = readAsObject(nType);
        if (nValue == T_IDENTITY)
            {
            IdentityHolder.reset(this, nId, o);
            }
        return o;
        }

    /**
    * Read a POF value as an Object array.
    *
    * @param nType  the type identifier of the value
    * @param ao     the optional array to use to store the values, or to use
    *               as a typed template for creating an array to store the
    *               values, following the documentation for
    *               {@link java.util.Collection#toArray(Object[]) Collection.toArray()}
    *
    * @return an Object array
    *
    * @throws IOException  if an I/O error occurs
    */
    protected Object[] readAsObjectArray(int iProp, int nType, Object[] ao)
            throws IOException
        {
        Object[] aoResult = null;

        ReadBuffer.BufferInput in = m_in;
        switch (nType)
            {
            case V_REFERENCE_NULL:
                break;

            case V_STRING_ZERO_LENGTH:
            case V_COLLECTION_EMPTY:
                aoResult = OBJECT_ARRAY_EMPTY;
                break;

            case T_COLLECTION:
            case T_ARRAY:
                {
                int co = in.readPackedInt();
                aoResult = resizeArray(ao, co);
                for (int i = 0; i < co; ++i)
                    {
                    aoResult[i] = readAsObject(in.readPackedInt());
                    }
                }
                break;

            case T_UNIFORM_COLLECTION:
            case T_UNIFORM_ARRAY:
                {
                int nElementType = in.readPackedInt();
                int co           = in.readPackedInt();
                aoResult = resizeArray(ao, co);
                for (int i = 0; i < co; ++i)
                    {
                    aoResult[i] = readAsUniformObject(nElementType);
                    }
                }
                break;

            case T_SPARSE_ARRAY:
                {
                int co = in.readPackedInt();
                aoResult = resizeArray(ao, co);
                do
                    {
                    int iElement = in.readPackedInt();
                    if (iElement < 0)
                        {
                        break;
                        }
                    aoResult[iElement] = readAsObject(in.readPackedInt());
                    }
                while (--co >= 0);
                }
                break;

            case T_UNIFORM_SPARSE_ARRAY:
                {
                int nElementType = in.readPackedInt();
                int co           = in.readPackedInt();
                aoResult = resizeArray(ao, co);
                do
                    {
                    int iElement = in.readPackedInt();
                    if (iElement < 0)
                        {
                        break;
                        }
                    aoResult[iElement] = readAsUniformObject(nElementType);
                    }
                while (--co >= 0);
                }
                break;

            default:
                if (iProp != -1)
                    {
                    throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                          + " to an array type");
                    }
                throw new IOException("unable to convert type " 
                            + PofConstants.getTypeName(nType)
                                      + " to an array type");
            }

        return aoResult;
        }

    /**
    * Read a POF value as an Object array.
    *
    * @param <T>      the identifier type
    * @param iProp    the property index to read
    * @param nType    the type identifier of the value
    * @param factory  the optional factory to use to initialize the array
    *
    * @return an Object array
    *
    * @throws IOException  if an I/O error occurs
    */
    protected <T> T[] readAsTypedObjectArray(int iProp, int nType, IntFunction<T[]> factory)
            throws IOException
        {
        T[] aoResult = null;

        ReadBuffer.BufferInput in = m_in;
        switch (nType)
            {
            case V_REFERENCE_NULL:
                break;

            case V_STRING_ZERO_LENGTH:
            case V_COLLECTION_EMPTY:
                aoResult = (T[]) OBJECT_ARRAY_EMPTY;
                break;

            case T_COLLECTION:
            case T_ARRAY:
                {
                int co = in.readPackedInt();
                aoResult = factory.apply(co);
                for (int i = 0; i < co; ++i)
                    {
                    aoResult[i] = (T) readAsObject(in.readPackedInt());
                    }
                }
                break;

            case T_UNIFORM_COLLECTION:
            case T_UNIFORM_ARRAY:
                {
                int nElementType = in.readPackedInt();
                int co           = in.readPackedInt();
                aoResult = factory.apply(co);
                for (int i = 0; i < co; ++i)
                    {
                    aoResult[i] = (T) readAsUniformObject(nElementType);
                    }
                }
                break;

            case T_SPARSE_ARRAY:
                {
                int co = in.readPackedInt();
                aoResult = factory.apply(co);
                do
                    {
                    int iElement = in.readPackedInt();
                    if (iElement < 0)
                        {
                        break;
                        }
                    aoResult[iElement] = (T) readAsObject(in.readPackedInt());
                    }
                while (--co >= 0);
                }
                break;

            case T_UNIFORM_SPARSE_ARRAY:
                {
                int nElementType = in.readPackedInt();
                int co           = in.readPackedInt();
                aoResult = factory.apply(co);
                do
                    {
                    int iElement = in.readPackedInt();
                    if (iElement < 0)
                        {
                        break;
                        }
                    aoResult[iElement] = (T) readAsUniformObject(nElementType);
                    }
                while (--co >= 0);
                }
                break;

            default:
                throw new IOException("unable to convert field " + iProp + " of type " 
                            + PofConstants.getTypeName(nType)
                                      + " to an array type");
            }

        return aoResult;
        }

    /**
    * Read a Binary object from the specified BufferInput in an optimal way,
    * depending on the existence of an enclosing ReadBuffer.
    *
    * @param in  a BufferInput to read from
    *
    * @return a Binary object
    *
    * @throws IOException  if an I/O error occurs
    */
    protected static Binary readBinary(ReadBuffer.BufferInput in)
            throws IOException
        {
        int cb = in.readPackedInt();

        ReadBuffer buf = in.getBuffer();
        if (buf == null)
            {
            return in.readBuffer(cb).toBinary();
            }
        else
            {
            int    of  = in.getOffset();
            Binary bin = buf.toBinary(of, cb);
            in.skipBytes(cb); // skip after read to prevent pre-destruction
            return bin;
            }
        }


    // ----- inner class: UserTypeReader ------------------------------------

    /**
    * The UserTypeReader implementation is a contextually-aware PofReader
    * whose purpose is to advance through the properties of a value of a
    * specified user type. The "contextual awareness" refers to the fact that
    * the UserTypeReader maintains state about the type identifier and
    * version of the user type, the parser's property index position within
    * the user type value, and a PofContext that may differ from the
    * PofContext that provided the PofSerializer which is using this
    * UserTypeReader to parse a user type.
    */
    public static class UserTypeReader
            extends PofBufferReader
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a parser for parsing the property values of a user type.
        *
        * @param in          the BufferInput that contains the user type
        *                    data, except for the user type id itself (which
        *                    is passed passed as a constructor argument)
        * @param ctx         the PofContext to use for parsing the user type
        *                    property values within the user type that this
        *                    parser will be parsing
        * @param nTypeId     the type id of the user type
        * @param nVersionId  the version id of the user type
        *
        * @throws IOException  if an I/O error occurs
        */
        public UserTypeReader(
                ReadBuffer.BufferInput in, PofContext ctx,
                int nTypeId, int nVersionId)
                throws IOException
            {
            this(null, in, ctx, nTypeId, nVersionId);
            }

        /**
        * Construct a parser for parsing the property values of a user type.
        *
        * @param parent      the parent (ie the containing) PofBufferReader
        * @param in          the BufferInput that contains the user type
        *                    data, except for the user type id itself (which
        *                    is passed passed as a constructor argument)
        * @param ctx         the PofContext to use for parsing the user type
        *                    property values within the user type that this
        *                    parser will be parsing
        * @param nTypeId     the type id of the user type
        * @param nVersionId  the version id of the user type
        *
        * @throws IOException  if an I/O error occurs
        */
        public UserTypeReader(PofBufferReader parent,
                ReadBuffer.BufferInput in, PofContext ctx,
                int nTypeId, int nVersionId)
                throws IOException
            {
            super(in, ctx);

            assert nTypeId >= 0;
            assert nVersionId >= 0;

            m_parent     = parent;
            m_nTypeId    = nTypeId;
            m_nVersionId = nVersionId;

            // prime the property reader by knowing the offset of index of
            // the next property to read
            m_ofNextProp = in.getOffset();
            int iProp    = in.readPackedInt();
            m_iNextProp  = iProp < 0 ? EOPS : iProp;
            }

        /**
        * Create a nested UserTypeReader, which will be initiated with the
        * information found in the nested buffer.
        *
        * @param parent  the parent (ie the containing) PofBufferReader
        * @param in      the BufferInput that contains the user type data
        * @param ctx     the PofContext to use for parsing the user type property
        *                values within the user type that this parser will be
        *                parsing
        *
        * @throws IOException  if an I/O error occurs
        */
        private UserTypeReader(PofBufferReader parent,
                ReadBuffer.BufferInput in, PofContext ctx) throws IOException
            {
            super(in, ctx);

            m_parent     = parent;


            // read the type and version directly from the buffer
            m_nTypeId    = in.readPackedInt();
            m_nVersionId = in.readPackedInt();

            // prime the property reader by knowing the offset of index of
            // the next property to read
            m_ofNextProp = in.getOffset();

            int iProp    = in.readPackedInt();
            m_iNextProp  = iProp < 0 ? EOPS : iProp;
            }

        /**
        * Construct a parser for parsing a nested property that does not exist.
        * In other words, this is a "no-op" user type reader.
        *
        * @param parent      the parent (ie the containing) PofBufferReader
        * @param in          the BufferInput that contains the user type
        *                    data, except for the user type id itself (which
        *                    is passed passed as a constructor argument)
        * @param ctx         the PofContext to use for parsing the user type
        *                    property values within the user type that this
        *                    parser will be parsing
        * @param nTypeId     the type id of the user type
        *
        * @throws IOException  if an I/O error occurs
        */
        private UserTypeReader(PofBufferReader parent, ReadBuffer.BufferInput in,
                              PofContext ctx, int nTypeId)
                throws IOException
            {
            super(in, ctx);

            assert nTypeId >= 0;

            m_parent     = parent;
            m_nTypeId    = nTypeId;
            m_nVersionId = 0;

            // prime the property reader by knowing the offset of index of
            // the next property to read
            m_ofNextProp = in.getOffset();
            m_iNextProp  = EOPS;
            }

        // ----- PofReader interface ------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override public int getUserTypeId()
            {
            return m_nTypeId;
            }

        /**
        * {@inheritDoc}
        */
        @Override public int getVersionId()
            {
            return m_nVersionId;
            }

        /**
        * {@inheritDoc}
        */
        @Override public void registerIdentity(Object o)
            {
            PofBufferReader.IdentityHolder.reset(this, -1, o);
            }

        /**
        * {@inheritDoc}
        */
        @Override public PofReader createNestedPofReader(int iProp)
                throws IOException
            {
            UserTypeReader reader;
            PropertyInfo prop = m_propertyMap == null ? null : m_propertyMap.get(iProp);
            if (prop != null)
                {
                // ensure that the existing nested stream is closed before creating one for a skipped property
                closeNested();

                // create new buffer to read skipped property from
                ReadBuffer.BufferInput in = m_in.getBuffer().getReadBuffer(prop.offset(), prop.length()).getBufferInput();

                reader = new PofBufferReader.UserTypeReader(this, in, getPofContext());
                
                // note: there is no complete() call at this point, since the
                //       property has yet to be read
                }
            else if (advanceTo(iProp))
                {
                reader = new PofBufferReader.UserTypeReader(this, m_in, getPofContext());

                // note: there is no complete() call at this point, since the
                //       property has yet to be read
                }
            else
                {
                // nothing to read for that property
                complete(iProp);

                // return a "fake" reader that contains no data
                reader = new PofBufferReader.UserTypeReader(this, m_in, getPofContext(), iProp);
                }

            m_readerNested = reader;
            m_iNestedProp  = iProp;

            return reader;
            }

        /**
        * {@inheritDoc}
        */
        @Override public Binary readRemainder()
                throws IOException
            {
            // close nested buffer if it exists
            closeNested();

            // check if the property stream is already exhausted
            int iNextProp = m_iNextProp;
            if (iNextProp == EOPS)
                {
                return null;
                }

            // skip over all the remaining properties
            ReadBuffer.BufferInput in = m_in;
            ReadBuffer buf = in.getBuffer();
            if (buf == null)
                {
                // if the BufferInput does not have an underlying ReadBuffer,
                // we must use mark() and reset() to read the remaining
                // properties; in this case, assume we need to reread as much
                // data as theoretically possible
                in.mark(Integer.MAX_VALUE);
                }
            int ofBegin = m_ofNextProp;
            int ofEnd;
            do
                {
                skipValue(in);
                ofEnd     = in.getOffset();
                iNextProp = in.readPackedInt();
                }
            while (iNextProp != -1);

            m_iNextProp  = EOPS;
            m_ofNextProp = ofEnd;

            // return all the properties that were skipped
            int cb = ofEnd - ofBegin;
            if (buf == null)
                {
                in.reset();
                return in.readBuffer(cb).toBinary();
                }
            else
                {
                return buf.toBinary(ofBegin, cb);
                }
            }

        // ----- internal methods ---------------------------------------

        /**
        * Return the index of the most recent property read or (if it were
        * missing) requested.
        *
        * @return  the index of the most recent property read
        */
        public int getPreviousPropertyIndex()
            {
            return m_iPrevProp;
            }

        /**
        * Return the index of the next property in the POF stream.
        *
        * @return  the index of the next property in the POF stream
        *
        * @throws IOException  if an I/O error occurs
        */
        public int getNextPropertyIndex()
                throws IOException
            {
            closeNested();
            return m_iNextProp == EOPS ? -1 : m_iNextProp;
            }

        /**
        * {@inheritDoc}
        */
        @Override protected boolean advanceTo(int iProp)
                throws IOException
            {
            // if a nested writer is still open, then "end" that property
            closeNested();

            // the terminating index is -1; if searching for -1, re-order the
            // goal to come after all other properties (which assumes that
            // there is no valid property index Integer.MAX_VALUE)
            if (iProp == -1)
                {
                iProp = EOPS;
                }

            // check for backwards movement
            if (iProp <= m_iPrevProp)
                {
                throw new IllegalStateException("previous property index="
                        + m_iPrevProp + ", requested property index=" + iProp
                        + " while reading user type " + getUserTypeId());
                }

            // check if the stream is already in the correct location
            // (common case)
            int iNextProp = m_iNextProp;
            if (iProp == iNextProp)
                {
                return true;
                }

            ReadBuffer.BufferInput in = m_in;
            int ofNextProp = m_ofNextProp;
            while (iNextProp < iProp)
                {
                int ofCurrentProp = in.getOffset();
                int iCurrentProp  = iNextProp;

                skipValue(in);

                ofNextProp = in.getOffset();
                iNextProp  = in.readPackedInt();

                ensurePropertyMap().put(iCurrentProp, new PropertyInfo(ofCurrentProp, in.getOffset() - ofCurrentProp));

                if (iNextProp < 0)
                    {
                    iNextProp = EOPS;
                    }
                }

            m_ofNextProp = ofNextProp;
            m_iNextProp  = iNextProp;

            return iProp == iNextProp;
            }

        /**
        * {@inheritDoc}
        */
        @Override protected void complete(int iProp)
                throws IOException
            {
            if (m_iNextProp == iProp)
                {
                ReadBuffer.BufferInput in = m_in;
                m_ofNextProp  = in.getOffset();
                int iNextProp = in.readPackedInt();
                m_iNextProp   = iNextProp < 0 ? EOPS : iNextProp;
                }

            m_iPrevProp = iProp;
            }

        /**
        * Notify the UserTypeReader that it is being "closed".
        *
        * @throws IOException  if an I/O error occurs
        */
        protected void closeNested()
                throws IOException
            {
            // check if a nested PofReader is open
            UserTypeReader readerNested = m_readerNested;
            if (readerNested != null)
                {
                // check if there is some remainder that haven't been skipped
                if (readerNested.m_iNextProp != EOPS)
                    {
                    readerNested.readRemainder();
                    }
                // close it
                readerNested.closeNested();

                // finish reading the property that the nested PofReader was
                // reading from; this is the "complete()" call that was
                // deferred when the nested stream was opened
                complete(m_iNestedProp);

                m_readerNested = null;
                m_iNestedProp  = -1;
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override protected PofBufferReader getParentParser()
            {
            return m_parent;
            }

        /**
         * Lazily creates a map of skipped properties, if necessary.
         *
         * @return a map of skipped properties
         */
        private Map<Integer, PropertyInfo> ensurePropertyMap()
            {
            Map<Integer, PropertyInfo> map = m_propertyMap;
            if (map == null)
                {
                map = m_propertyMap = new LinkedHashMap<>();
                }
            return map;
            }

        private record PropertyInfo(int offset, int length) {}

        // ----- constants ----------------------------------------------

        /**
        * Fake End-Of-Property-Stream indicator.
        */
        private static final int EOPS = Integer.MAX_VALUE;

        // ----- data members -------------------------------------------

        /**
        * The parent (ie containing) PofBufferReader.
        */
        private PofBufferReader m_parent;

        /**
        * The type identifier of the user type that is being parsed.
        */
        private int m_nTypeId;

        /**
        * The version identifier of the user type that is being parsed.
        */
        private int m_nVersionId;

        /**
        * Most recent property read or (if it were missing) requested. This
        * is used to determine if the client is attempting to read properties
        * in the wrong order.
        */
        private int m_iPrevProp = -1;

        /**
        * The index of the next property in the POF stream.
        */
        private int m_iNextProp;

        /**
        * The offset of the index of the next property to read.
        */
        private int m_ofNextProp;

        /**
        * The currently open nested reader, if any.
        */
        private UserTypeReader m_readerNested;

        /**
        * The property index of the property from which the currently open
        * nested reader is reading from.
        */
        private int m_iNestedProp;

        /**
         * Map of property indexes to their offset and length.
         */
        private Map<Integer, PropertyInfo> m_propertyMap;
        }


    // ----- inner class: IdentityHolder ------------------------------------

    public static class IdentityHolder
        {
        private static ThreadLocal s_mapId = new ThreadLocal()
            {
            @Override protected synchronized Object initialValue()
                {
                return new HashMap();
                }
            };

        public static void set(PofBufferReader reader, int nId)
            {
            Map mapId = (Map) s_mapId.get();
            mapId.put(reader, nId);
            }

        public static void reset(PofBufferReader reader, int nId, Object o)
            {
            Map mapId = (Map) s_mapId.get();
            if (!mapId.isEmpty())
                {
                while (reader != null)
                    {
                    Object oValue = mapId.get(reader);
                    if (oValue != null)
                        {
                        int nValue = (Integer) oValue;
                        if (nId == -1 || nValue == nId)
                            {
                            reader.registerIdentity(nValue, o);
                            mapId.remove(reader);
                            }
                        break;
                        }
                    reader = reader.getParentParser();
                    }
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The BufferInput containing the POF stream.
    */
    protected ReadBuffer.BufferInput m_in;

    /**
    * The PofContext to use to realize user data types as Java objects.
    */
    protected PofContext m_ctx;

    /**
    * Lazily-constructed mapping of identities to references.
    */
    protected LongArray m_arrayRefs;
    }
