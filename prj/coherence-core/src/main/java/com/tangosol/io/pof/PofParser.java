/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ReadBuffer;

import java.io.IOException;


/**
* A "push" parser (event-based parser) for ripping through a POF stream and
* delivering the contents as events to a PofHandler object.
*
* @author cp  2006.07.12
*
* @since Coherence 3.2
*/
public class PofParser
        extends PofHelper
        implements PofConstants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a POF parser that will push events to the specified handler.
    *
    * @param handler  a POF handler object
    */
    public PofParser(PofHandler handler)
        {
        m_handler = handler;
        }


    // ----- public API -----------------------------------------------------

    /**
    * Parse a POF value that is in the passed BufferInput.
    *
    * @param in  the BufferInput to read the POF value from
    */
    public void parse(ReadBuffer.BufferInput in)
        {
        try
            {
            parseValue(in, -1);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- internal -------------------------------------------------------

    /**
    * Within the POF stream, parse a POF value that is in the passed
    * BufferInput.
    *
    * @param in    the BufferInput to read from
    * @param iPos  the position of the value that is about to be read, which
    *              is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseValue(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int nType = in.readPackedInt();

        if (nType == T_IDENTITY)
            {
            m_handler.registerIdentity(in.readPackedInt());
            nType = in.readPackedInt();
            }

        parseUniformValue(in, iPos, nType);
        }

    /**
    * Within the POF stream, parse a POF value of the specified type that is
    * in the passed BufferInput.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    * @param nType  the Type ID to parse
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseUniformValue(ReadBuffer.BufferInput in, int iPos, int nType)
            throws IOException
        {
        if (nType >= 0)
            {
            parseUserType(in, iPos, nType);
            }
        else
            {
            PofHandler handler = m_handler;
            switch (nType)
                {
                case T_INT16:                  // int16
                    handler.onInt16(iPos, (short) in.readPackedInt());
                    break;

                case T_INT32:                  // int32
                    handler.onInt32(iPos, in.readPackedInt());
                    break;

                case T_INT64:                  // int64
                    handler.onInt64(iPos, in.readPackedLong());
                    break;

                case T_INT128:                 // int128
                    handler.onInt128(iPos, readBigInteger(in));
                    break;

                case T_FLOAT32:                // float32
                    handler.onFloat32(iPos, in.readFloat());
                    break;

                case T_FLOAT64:                // float64
                    handler.onFloat64(iPos, in.readDouble());
                    break;

                case T_FLOAT128:               // float128*
                    handler.onFloat128(iPos, new RawQuad(in.readBuffer(16).toBinary()));

                case T_DECIMAL32:              // decimal32
                    handler.onDecimal32(iPos, readBigDecimal(in, 4));
                    break;

                case T_DECIMAL64:              // decimal64
                    handler.onDecimal64(iPos, readBigDecimal(in, 8));
                    break;

                case T_DECIMAL128:             // decimal128
                    handler.onDecimal128(iPos, readBigDecimal(in, 16));
                    break;

                case T_BOOLEAN:                // boolean
                    handler.onBoolean(iPos, in.readPackedInt() != 0);
                    break;

                case T_OCTET:                  // octet
                    handler.onOctet(iPos, in.readUnsignedByte());
                    break;

                case T_OCTET_STRING:           // octet-string
                    {
                    int cb = in.readPackedInt();
                    int of = in.getOffset();
                    in.skipBytes(cb);
                    handler.onOctetString(iPos, in.getBuffer().toBinary(of, cb));
                    }
                    break;

                case T_CHAR:                   // char
                    {
                    handler.onChar(iPos, readChar(in));
                    }
                    break;

                case T_CHAR_STRING:            // char-string
                    handler.onCharString(iPos, in.readSafeUTF());
                    break;

                case T_DATE:                   // date
                    {
                    int nYear  = in.readPackedInt();
                    int nMonth = in.readPackedInt();
                    int nDay   = in.readPackedInt();
                    handler.onDate(iPos, nYear, nMonth, nDay);
                    }
                    break;

                case T_YEAR_MONTH_INTERVAL:    // year-month-interval
                    {
                    int cYears  = in.readPackedInt();
                    int dMonths = in.readPackedInt();
                    handler.onYearMonthInterval(iPos, cYears, dMonths);
                    }
                    break;

                case T_TIME:                   // time
                    {
                    int nHour     = in.readPackedInt();
                    int nMinute   = in.readPackedInt();
                    int nSecond   = in.readPackedInt();
                    int nFraction = in.readPackedInt();
                    int nNanos    = nFraction <= 0 ? -nFraction : nFraction * 1000000;

                    int nZoneType = in.readPackedInt();
                    if (nZoneType == 2)
                        {
                        int nHourOffset   = in.readPackedInt();
                        int nMinuteOffset = in.readPackedInt();
                        handler.onTime(iPos, nHour, nMinute, nSecond,
                                       nNanos, nHourOffset, nMinuteOffset);
                        }
                    else
                        {
                        assert nZoneType == 0 || nZoneType == 1;
                        boolean fUTC = nZoneType == 1;
                        handler.onTime(iPos, nHour, nMinute, nSecond,
                                       nNanos, fUTC);
                        }
                    }
                    break;

                case T_TIME_INTERVAL:          // time-interval
                    {
                    int cHours   = in.readPackedInt();
                    int cMinutes = in.readPackedInt();
                    int cSeconds = in.readPackedInt();
                    int cNanos   = in.readPackedInt();
                    handler.onTimeInterval(iPos, cHours, cMinutes, cSeconds, cNanos);
                    }
                    break;

                case T_DATETIME:               // datetime
                    {
                    int nYear     = in.readPackedInt();
                    int nMonth    = in.readPackedInt();
                    int nDay      = in.readPackedInt();
                    int nHour     = in.readPackedInt();
                    int nMinute   = in.readPackedInt();
                    int nSecond   = in.readPackedInt();
                    int nFraction = in.readPackedInt();
                    int nNano     = nFraction <= 0 ? -nFraction : nFraction * 1000000;

                    int nZoneType = in.readPackedInt();
                    if (nZoneType == 2)
                        {
                        int nHourOffset   = in.readPackedInt();
                        int nMinuteOffset = in.readPackedInt();
                        handler.onDateTime(iPos, nYear, nMonth, nDay, nHour,
                                           nMinute, nSecond, nNano, nHourOffset, nMinuteOffset);
                        }
                    else
                        {
                        assert nZoneType == 0 || nZoneType == 1;
                        boolean fUTC = nZoneType == 1;
                        handler.onDateTime(iPos, nYear, nMonth, nDay,
                                           nHour, nMinute, nSecond, nNano, fUTC);
                        }
                    }
                    break;

                case T_DAY_TIME_INTERVAL:      // day-time-interval
                    {
                    int cDays    = in.readPackedInt();
                    int cHours   = in.readPackedInt();
                    int cMinutes = in.readPackedInt();
                    int cSeconds = in.readPackedInt();
                    int cNanos   = in.readPackedInt();
                    handler.onDayTimeInterval(iPos, cDays, cHours, cMinutes,
                                              cSeconds, cNanos);
                    }
                    break;

                case T_COLLECTION:             // collection
                    parseCollection(in, iPos);
                    break;

                case T_UNIFORM_COLLECTION:     // uniform-collection
                    parseUniformCollection(in, iPos);
                    break;

                case T_ARRAY:                  // array
                    parseArray(in, iPos);
                    break;

                case T_UNIFORM_ARRAY:          // uniform-array
                    parseUniformArray(in, iPos);
                    break;

                case T_SPARSE_ARRAY:           // sparse-array
                    parseSparseArray(in, iPos);
                    break;

                case T_UNIFORM_SPARSE_ARRAY:   // uniform-sparse-array
                    parseUniformSparseArray(in, iPos);
                    break;

                case T_MAP:                    // map
                    parseMap(in, iPos);
                    break;

                case T_UNIFORM_KEYS_MAP:       // uniform-keys-map
                    parseUniformKeysMap(in, iPos);
                    break;

                case T_UNIFORM_MAP:            // uniform-map
                    parseUniformMap(in, iPos);
                    break;

                case T_IDENTITY:               // identity
                    throw azzert();

                case T_REFERENCE:              // reference
                    handler.onIdentityReference(iPos, in.readPackedInt());
                    break;

                case V_BOOLEAN_FALSE:          // boolean:false
                case V_BOOLEAN_TRUE:           // boolean:true
                    handler.onBoolean(iPos, nType == V_BOOLEAN_TRUE);
                    break;

                case V_STRING_ZERO_LENGTH:     // string:zero-length
                    handler.onOctetString(iPos, BINARY_EMPTY);
                    break;

                case V_COLLECTION_EMPTY:       // collection:empty
                    handler.beginCollection(iPos, 0);
                    handler.endComplexValue();
                    break;

                case V_REFERENCE_NULL:         // reference:null
                    handler.onNullReference(iPos);
                    break;

                case V_FP_POS_INFINITY:        // floating-point:+infinity
                    handler.onFloat32(iPos, Float.POSITIVE_INFINITY);
                    break;

                case V_FP_NEG_INFINITY:        // floating-point:-infinity
                    handler.onFloat32(iPos, Float.NEGATIVE_INFINITY);
                    break;

                case V_FP_NAN:                 // floating-point:NaN
                    handler.onFloat32(iPos, Float.NaN);
                    break;

                case V_INT_NEG_1:              // int:-1
                case V_INT_0:                  // int:0
                case V_INT_1:                  // int:1
                case V_INT_2:                  // int:2
                case V_INT_3:                  // int:3
                case V_INT_4:                  // int:4
                case V_INT_5:                  // int:5
                case V_INT_6:                  // int:6
                case V_INT_7:                  // int:7
                case V_INT_8:                  // int:8
                case V_INT_9:                  // int:9
                case V_INT_10:                 // int:10
                case V_INT_11:                 // int:11
                case V_INT_12:                 // int:12
                case V_INT_13:                 // int:13
                case V_INT_14:                 // int:14
                case V_INT_15:                 // int:15
                case V_INT_16:                 // int:16
                case V_INT_17:                 // int:17
                case V_INT_18:                 // int:18
                case V_INT_19:                 // int:19
                case V_INT_20:                 // int:20
                case V_INT_21:                 // int:21
                case V_INT_22:                 // int:22
                    handler.onInt32(iPos, V_INT_0 - nType);
                    break;

                default:
                    azzert();
                }
            }
        }

    /**
    * Parse a User Type from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    * @param nType  the Type ID for the User Type
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseUserType(ReadBuffer.BufferInput in, int iPos, int nType)
            throws IOException
        {
        PofHandler handler = m_handler;
        handler.beginUserType(iPos, nType, in.readPackedInt());

        while (true)
            {
            int iProp = in.readPackedInt();
            if (iProp < 0)
                {
                handler.endComplexValue();
                break;
                }
            else
                {
                parseValue(in, iProp);
                }
            }
        }

    /**
    * Parse a Collection from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseCollection(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginCollection(iPos, cElements);

        for (int i = 0; i < cElements; ++i)
            {
            parseValue(in, i);
            }

        handler.endComplexValue();
        }

    /**
    * Parse a Uniform Collection from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseUniformCollection(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int nType     = in.readPackedInt();
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginUniformCollection(iPos, cElements, nType);

        for (int i = 0; i < cElements; ++i)
            {
            parseUniformValue(in, i, nType);
            }

        handler.endComplexValue();
        }

    /**
    * Parse an Array from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseArray(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginArray(iPos, cElements);

        for (int i = 0; i < cElements; ++i)
            {
            parseValue(in, i);
            }

        handler.endComplexValue();
        }

    /**
    * Parse a Uniform Array from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseUniformArray(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int nType     = in.readPackedInt();
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginUniformArray(iPos, cElements, nType);

        for (int i = 0; i < cElements; ++i)
            {
            parseUniformValue(in, i, nType);
            }

        handler.endComplexValue();
        }

    /**
    * Parse a Sparse Array from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseSparseArray(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginSparseArray(iPos, cElements);

        for (int i = 0; i < cElements; ++i)
            {
            int iArrayIndex = in.readPackedInt();
            if (iArrayIndex < 0)
                {
                break;
                }

            parseValue(in, iArrayIndex);
            }

        handler.endComplexValue();
        }

    /**
    * Parse a Uniform Sparse Array from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseUniformSparseArray(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int nType     = in.readPackedInt();
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginUniformSparseArray(iPos, cElements, nType);

        for (int i = 0; i < cElements; ++i)
            {
            int iArrayIndex = in.readPackedInt();
            if (iArrayIndex < 0)
                {
                break;
                }

            parseUniformValue(in, iArrayIndex, nType);
            }

        handler.endComplexValue();
        }

    /**
    * Parse a Map from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseMap(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginMap(iPos, cElements);

        for (int i = 0; i < cElements; ++i)
            {
            parseValue(in, i);
            parseValue(in, i);
            }

        handler.endComplexValue();
        }

    /**
    * Parse a Uniform-Keys Map from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseUniformKeysMap(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int nTypeKeys = in.readPackedInt();
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginUniformKeysMap(iPos, cElements, nTypeKeys);

        for (int i = 0; i < cElements; ++i)
            {
            parseUniformValue(in, i, nTypeKeys);
            parseValue(in, i);
            }

        handler.endComplexValue();
        }

    /**
    * Parse a Uniform Map from the POF stream.
    *
    * @param in     the BufferInput to read from
    * @param iPos   the position of the value that is about to be read, which
    *               is a property index, an array index, or -1
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void parseUniformMap(ReadBuffer.BufferInput in, int iPos)
            throws IOException
        {
        int nTypeKeys = in.readPackedInt();
        int nTypeVals = in.readPackedInt();
        int cElements = in.readPackedInt();

        PofHandler handler = m_handler;
        handler.beginUniformMap(iPos, cElements, nTypeKeys, nTypeVals);

        for (int i = 0; i < cElements; ++i)
            {
            parseUniformValue(in, i, nTypeKeys);
            parseUniformValue(in, i, nTypeVals);
            }

        handler.endComplexValue();
        }


    // ----- unit test ------------------------------------------------------

    /**
    * Unit test:
    * <pre>
    * java PofParser &lt;hex string&gt;
    * </pre>
    *
    * @param asArg  command line arguments
    *
    * @throws Exception if an error occurs
    */
    public static void main(String[] asArg) throws Exception
        {
        if (asArg.length > 0)
            {
            ReadBuffer.BufferInput in = new ByteArrayReadBuffer(
                    parseHex(asArg[0])).getBufferInput();

            new PofParser(new LoggingPofHandler()).parse(in);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The PofHandler to deliver events to.
    */
    private PofHandler m_handler;
    }
