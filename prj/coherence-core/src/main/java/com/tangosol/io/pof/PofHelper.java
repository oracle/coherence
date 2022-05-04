/*
 * Copyright (c) 2000, 2020, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.LiteMap;
import com.tangosol.util.LiteSet;
import com.tangosol.util.LongArray;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.RecyclingLinkedList;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SafeHashSet;
import com.tangosol.util.SafeLinkedList;
import com.tangosol.util.SimpleMapEntry;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.UTFDataFormatException;

import java.lang.reflect.Array;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Time;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;


/**
* Collection of helper methods for POF streams.
*
* @author cp/jh  2006.07.17
*
* @since Coherence 3.2
*/
public abstract class PofHelper
        extends Base
        implements PofConstants
    {
    // ----- type conversion ------------------------------------------------

    /**
    * Return an identifier that represents the Java type of the specified
    * object.
    *
    * @param o    an object to determine the type of
    * @param ctx  the PofContext used to determine if the object is an
    *             instance of a valid user type; must not be null
    *
    * @return one of the {@link PofConstants} class <tt>J_*</tt> constants
    */
    public static int getJavaTypeId(Object o, PofContext ctx)
        {
        assert ctx != null;

        return  o == null                               ? J_NULL
                : o instanceof PortableObject           ? J_USER_TYPE
                : o instanceof String                   ? J_STRING
                : o instanceof Number                   ?
                      o instanceof Integer              ? J_INTEGER
                    : o instanceof Long                 ? J_LONG
                    : o instanceof Double               ? J_DOUBLE
                    : o instanceof BigInteger           ?
                         ctx.isUserType(o)              ? J_USER_TYPE
                        :                                 J_BIG_INTEGER
                    : o instanceof BigDecimal           ?
                         ctx.isUserType(o)              ? J_USER_TYPE
                        :                                 J_BIG_DECIMAL
                    : o instanceof Short                ? J_SHORT
                    : o instanceof Float                ? J_FLOAT
                    : o instanceof Byte                 ? J_BYTE
                    :                                     J_USER_TYPE
                : o instanceof Boolean                  ? J_BOOLEAN
                : o instanceof Character                ? J_CHARACTER
                : o instanceof ReadBuffer               ? J_BINARY
                // as of JDK 1.5.0_09 this is very expensive:
                //: o.getClass().isArray()              ?
                    : o instanceof byte[]               ? J_BYTE_ARRAY
                    : o instanceof int[]                ? J_INT_ARRAY
                    : o instanceof long[]               ? J_LONG_ARRAY
                    : o instanceof double[]             ? J_DOUBLE_ARRAY
                    : o instanceof char[]               ? J_CHAR_ARRAY
                    : o instanceof boolean[]            ? J_BOOLEAN_ARRAY
                    : o instanceof short[]              ? J_SHORT_ARRAY
                    : o instanceof float[]              ? J_FLOAT_ARRAY
                // final POF-primitive types MUST be checked before customizable
                // serialization (isUserType); additionally, isUserType is a
                // potentially expensive call, and it is thus desirable to avoid
                // it where possible
                : ctx.isUserType(o)                     ? J_USER_TYPE
                : o instanceof Map                      ? J_MAP
                : o instanceof Collection               ? J_COLLECTION
                : o instanceof LocalDate                ? J_LOCAL_DATE
                : o instanceof java.sql.Date            ? J_DATE
                : o instanceof LocalTime                ? J_LOCAL_TIME
                : o instanceof OffsetTime               ? J_OFFSET_TIME
                : o instanceof Time                     ? J_TIME
                : o instanceof Timestamp                ? J_TIMESTAMP
                : o instanceof Date                     ? J_DATETIME
                : o instanceof LocalDateTime            ? J_LOCAL_DATETIME
                : o instanceof OffsetDateTime           ? J_OFFSET_DATETIME
                : o instanceof ZonedDateTime            ? J_ZONED_DATETIME
                : o instanceof LongArray                ? J_SPARSE_ARRAY
                : o instanceof RawQuad                  ? J_QUAD
                : o instanceof PofHelper                ?
                      o instanceof RawDate              ? J_RAW_DATE
                    : o instanceof RawTime              ? J_RAW_TIME
                    : o instanceof RawDateTime          ? J_RAW_DATETIME
                    : o instanceof RawYearMonthInterval ? J_RAW_YEAR_MONTH_INTERVAL
                    : o instanceof RawTimeInterval      ? J_RAW_TIME_INTERVAL
                    : o instanceof RawDayTimeInterval   ? J_RAW_DAY_TIME_INTERVAL
                    :                                     J_USER_TYPE
                : o instanceof Object[]                 ? J_OBJECT_ARRAY
                :                                         J_USER_TYPE;
        }

    /**
    * Return an identifier that represents the POF type of the specified
    * class.
    *
    * @param clz  the class; must not be null
    * @param ctx  the PofContext used to determine the type identifier of a
    *             user type; must not be null
    *
    * @return one of the {@link PofConstants} class <tt>T_*</tt> constants
    *
    * @throws IllegalArgumentException if the user type associated with the
    *         given object is unknown to the specified PofContext
    */
    public static int getPofTypeId(Class clz, PofContext ctx)
        {
        assert clz != null;
        assert ctx != null;

        Integer I = (Integer) JAVA_TO_POF_TYPE.get(clz);
        return I != null                                           ? I.intValue()
                : PortableObject      .class.isAssignableFrom(clz) ? ctx.getUserTypeIdentifier(clz)
                : ctx.isUserType(clz)                              ? ctx.getUserTypeIdentifier(clz)
                : BigInteger          .class.isAssignableFrom(clz) ? T_INT128
                : BigDecimal          .class.isAssignableFrom(clz) ? T_DECIMAL128
                : Map                 .class.isAssignableFrom(clz) ? T_MAP
                : Collection          .class.isAssignableFrom(clz) ? T_COLLECTION
                : Date                .class.isAssignableFrom(clz) ? T_DATETIME
                : RawQuad             .class.isAssignableFrom(clz) ? T_FLOAT128
                : RawDate             .class.isAssignableFrom(clz) ? T_DATE
                : RawTime             .class.isAssignableFrom(clz) ? T_TIME
                : RawDateTime         .class.isAssignableFrom(clz) ? T_DATETIME
                : RawYearMonthInterval.class.isAssignableFrom(clz) ? T_YEAR_MONTH_INTERVAL
                : RawTimeInterval     .class.isAssignableFrom(clz) ? T_TIME_INTERVAL
                : RawDayTimeInterval  .class.isAssignableFrom(clz) ? T_DAY_TIME_INTERVAL
                : clz.isArray()                                    ? T_ARRAY
                :                                                    ctx.getUserTypeIdentifier(clz);
        }

    /**
    * Determine if the given class can be represented as an intrinsic POF
    * type.
    *
    * @param clz  the class; must not be null
    *
    * @return true if the given class can be represented as an intrinsic POF
    *         type; false, otherwise
    */
    public static boolean isIntrinsicPofType(Class clz)
        {
        assert clz != null;

        Integer I = (Integer) JAVA_TO_POF_TYPE.get(clz);
        return I != null
                || BigInteger          .class.isAssignableFrom(clz)
                || BigDecimal          .class.isAssignableFrom(clz)
                || Map                 .class.isAssignableFrom(clz)
                || Collection          .class.isAssignableFrom(clz)
                || Date                .class.isAssignableFrom(clz)
                || RawDate             .class.isAssignableFrom(clz)
                || RawTime             .class.isAssignableFrom(clz)
                || RawDateTime         .class.isAssignableFrom(clz)
                || RawYearMonthInterval.class.isAssignableFrom(clz)
                || RawTimeInterval     .class.isAssignableFrom(clz)
                || RawDayTimeInterval  .class.isAssignableFrom(clz)
                || clz.isArray();
        }

    /**
    * Convert the passed number to the specified type.
    *
    * @param number       the number to convert
    * @param nJavaTypeId  the Java type ID to convert to, one of the J_*
    *                     enumerated values
    *
    * @return the number converted to the specified type
    */
    public static Number convertNumber(Number number, int nJavaTypeId)
        {
        if (number == null)
            {
            return null;
            }

        switch (nJavaTypeId)
            {
            case J_BYTE:
                return number instanceof Byte
                       ? number
                       : Byte.valueOf(number.byteValue());

            case J_SHORT:
                return number instanceof Short
                       ? number
                       : Short.valueOf(number.shortValue());

            case J_INTEGER:
                return number instanceof Integer
                       ? number
                       : Integer.valueOf(number.intValue());

            case J_LONG:
                return number instanceof Long
                       ? number
                       : Long.valueOf(number.longValue());

            case J_BIG_INTEGER:
                if (number instanceof BigInteger)
                    {
                    return number;
                    }
                else if (number instanceof BigDecimal)
                    {
                    // BigDecimal can provide a BigInteger directly without
                    // loss of precision (other than to the right of the
                    // decimal point)
                    return ((BigDecimal) number)
                            .setScale(0, BigDecimal.ROUND_DOWN)
                            .unscaledValue();
                    }
                else if (number instanceof Float || number instanceof Double)
                    {
                    // base-2 floating point value: go through BigDecimal to
                    // avoid losing precision
                    return new BigDecimal(number.doubleValue())
                            .setScale(0, BigDecimal.ROUND_DOWN)
                            .unscaledValue();
                    }
                else
                    {
                    // the number is likely an integer type
                    return BigInteger.valueOf(number.longValue());
                    }

            case J_FLOAT:
                return number instanceof Float
                       ? number
                       : Float.valueOf(number.floatValue());

            case J_DOUBLE:
                return number instanceof Double
                       ? number
                       : Double.valueOf(number.doubleValue());

            case J_QUAD:
                return number instanceof RawQuad
                       ? number
                       : new RawQuad(number.doubleValue());

            case J_BIG_DECIMAL:
                if (number instanceof BigDecimal)
                    {
                    return number;
                    }
                else if (number instanceof BigInteger)
                    {
                    BigInteger n = (BigInteger) number;

                    // COH-3194: if the BigInteger fits into a long, use the
                    // valueOf() method, thus eliminating a BigInteger object
                    // and leveraging flyweight instances for frequently used
                    // values
                    if (n.compareTo(BIGINTEGER_MIN_LONG) >= 0 &&
                        n.compareTo(BIGINTEGER_MAX_LONG) <= 0)
                        {
                        return BigDecimal.valueOf(n.longValue());
                        }

                    // BigInteger can be directly converted without loss of
                    // precision
                    return new BigDecimal(n);
                    }
                else if (number instanceof Float || number instanceof Double)
                    {
                    // base-2 floating point value
                    return BigDecimal.valueOf(number.doubleValue());
                    }
                else
                    {
                    // the number is likely an integer type
                    return BigDecimal.valueOf(number.longValue());
                    }

            default:
                throw new IllegalArgumentException("Java type ID "
                        + nJavaTypeId + " is not a number type");
            }
        }

    /**
    * Convert a date, time or date/time value to a Java Date.
    *
    * @param o an Object of type Date, RawDate, RawTime or RawDateTime
    *
    * @return a Java Date Object
    */
    public static Date convertToDate(Object o)
        {
        if (o == null)
            {
            return null;
            }

        if (o instanceof Date)
            {
            return (Date) o;
            }

        if (o instanceof RawDate)
            {
            return ((RawDate) o).toJavaDate();
            }

        if (o instanceof RawTime)
            {
            return ((RawTime) o).toJavaDate();
            }

        if (o instanceof RawDateTime)
            {
            return ((RawDateTime) o).toJavaDate();
            }

        throw new IllegalArgumentException("Unable to convert "
                            + o.getClass().getName() + " to a Date value");
        }

    /**
    * Expand the passed array to contain the specified number of elements.
    *
    * @param <T>    the array type
    * @param aoOld  the "template" array or null
    * @param cNew   the number of desired elements in the new array
    *
    * @return the old array, if it was big enough, or a new array of the same
    *         type
    */
    public static <T> T[] resizeArray(T[] aoOld, int cNew)
        {
        T[] aoNew;

        if (aoOld == null)
            {
            aoNew = (T[]) new Object[cNew];
            }
        else
            {
            if (cNew > aoOld.length)
                {
                aoNew = (T[]) Array.newInstance(
                        aoOld.getClass().getComponentType(), cNew);
                }
            else
                {
                aoNew = aoOld;
                }
            }

        return aoNew;
        }


    // ----- parsing --------------------------------------------------------

    /**
    * Decode an integer value from one of the reserved single-byte combined
    * type and value indicators.
    *
    * @param n  the integer value that the integer is encoded as
    *
    * @return an integer between -1 and 22 inclusive
    */
    public static int decodeTinyInt(int n)
        {
        assert n <= V_INT_NEG_1 && n >= V_INT_22;
        return V_INT_0 - n;
        }

    /**
    * Read a "char" value from the passed DataInput.
    *
    * @param in  the DataInput object to read from
    *
    * @return a char value
    *
    * @throws IOException on read error
    */
    public static char readChar(DataInput in)
            throws IOException
        {
        char ch;

        int b = in.readUnsignedByte();
        switch ((b & 0xF0) >>> 4)
            {
            case 0x0: case 0x1: case 0x2: case 0x3:
            case 0x4: case 0x5: case 0x6: case 0x7:
                // 1-byte format:  0xxx xxxx
                ch = (char) b;
                break;

            case 0xC: case 0xD:
                {
                // 2-byte format:  110x xxxx, 10xx xxxx
                int b2 = in.readUnsignedByte();
                if ((b2 & 0xC0) != 0x80)
                    {
                    throw new UTFDataFormatException();
                    }
                ch = (char) (((b  & 0x1F) << 6) |
                             b2 & 0x3F);
                break;
                }

            case 0xE:
                {
                // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                int n  = in.readUnsignedShort();
                int b2 = n >>> 8;
                int b3 = n & 0xFF;
                if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80)
                    {
                    throw new UTFDataFormatException();
                    }
                ch = (char) (((b  & 0x0F) << 12) |
                             ((b2 & 0x3F) <<  6) |
                             b3 & 0x3F);
                break;
                }

            default:
                throw new UTFDataFormatException(
                        "illegal leading UTF byte: " + b);
            }

        return ch;
        }

    /**
    * Read a BigInteger value from the passed DataInput.
    *
    * @param in  the DataInput object to read from
    *
    * @return the BigInteger value
    *
    * @throws IOException on read error
    */
    public static BigInteger readBigInteger(DataInput in)
            throws IOException
        {
        int     cb    = 16;
        byte[]  ab    = new byte[cb];
        int     b     = in.readUnsignedByte();
        boolean fNeg  = (b & 0x40) != 0;
        boolean fTiny = true;

        int of = cb - 1;
        ab[of] = (byte) (b & 0x3F);
        int cBits = 6;

        while ((b & 0x80) != 0)
            {
            b = in.readUnsignedByte();
            ab[of] = (byte) ((ab[of] & 0xFF) | ((b & 0x7F) << cBits));
            cBits += 7;
            if (cBits >= 8)
                {
                cBits -= 8;
                --of;

                if (cBits > 0 && of >= 0)
                    {
                    // some bits left over
                    ab[of] = (byte) ((b & 0x7F) >>> (7 - cBits));
                    }
                }
            fTiny = false;
            }

        if (fNeg)
            {
            for (of = 0; of < 16; ++of)
                {
                ab[of] = (byte) ~ab[of];
                }
            }

        // COH-3194: leverage the flyweight BigInteger instances for values
        // between -16 and 16
        if (fTiny)
            {
            b = ab[ab.length - 1];
            if (b >= -16 && b <= 16)
                {
                return BigInteger.valueOf(b);
                }
            }
        return new BigInteger(ab);
        }

    /**
    * Read a quad (a 128-bit base-2 IEEE754 value) from the passed DataInput
    * and convert it to a RawQuad.
    *
    * @param in      the DataInput object to read from
    *
    * @return the quad value as a RawQuad
    *
    * @throws IOException on read error
    */
    public static RawQuad readQuad(ReadBuffer.BufferInput in)
            throws IOException
        {
        int of = in.getOffset();
        int cb = 16;
        in.skipBytes(16);
        return new RawQuad(in.getBuffer().toBinary(of, cb));
        }

    /**
    * Read an IEEE754 value from the passed DataInput and convert it to a
    * Java BigDecimal.
    *
    * @param in      the DataInput object to read from
    * @param cBytes  the number of bytes for the IEEE754 value: 4, 8 or 16
    *
    * @return the BigDecimal value
    *
    * @throws IOException on read error
    */
    public static BigDecimal readBigDecimal(ReadBuffer.BufferInput in, int cBytes)
            throws IOException
        {
        assert cBytes == 4 || cBytes == 8 || cBytes == 16;

        BigInteger n      = readBigInteger(in);
        int        nScale = in.readPackedInt();

        // COH-3194: if the BigInteger fits into a long, use the valueOf()
        // method, thus eliminating a BigInteger object and leveraging
        // flyweight instances for frequently used values
        if (n.compareTo(BIGINTEGER_MIN_LONG) >= 0 &&
            n.compareTo(BIGINTEGER_MAX_LONG) <= 0)
            {
            return BigDecimal.valueOf(n.longValue(), nScale);
            }
        return new BigDecimal(n, nScale);
        }

    /**
    * Read a LocalDate value from a POF stream.
    *
    * @param in  the stream containing the POF date value
    *
    * @return a LocalDate object
    *
    * @throws IOException on read error
    */
    public static LocalDate readLocalDate(ReadBuffer.BufferInput in)
            throws IOException
        {
        int nYear  = in.readPackedInt();
        int nMonth = in.readPackedInt();
        int nDay   = in.readPackedInt();

        return LocalDate.of(nYear, nMonth, nDay);
        }

    /**
    * Read a LocalDateTime value from a POF stream.
    *
    * @param in  the stream containing the POF date value
    *
    * @return a LocalDateTime object
    *
    * @throws IOException on read error
    */
    public static LocalDateTime readLocalDateTime(ReadBuffer.BufferInput in)
            throws IOException
        {
        return LocalDateTime.of(readLocalDate(in), readLocalTime(in));
        }

    /**
    * Read a LocalTime value from a POF stream.
    *
    * @param in  the stream containing the POF date value
    *
    * @return a LocalTime object
    *
    * @throws IOException on read error
    */
    public static LocalTime readLocalTime(ReadBuffer.BufferInput in)
            throws IOException
        {
        int nHour     = in.readPackedInt();
        int nMinute   = in.readPackedInt();
        int nSecond   = in.readPackedInt();
        int nFraction = in.readPackedInt();
        int nNanos    = nFraction <= 0 ? -nFraction : nFraction * 1000000;

        byte nZoneType = in.readByte();
        if (nZoneType == 0)
            {
            return LocalTime.of(nHour, nMinute, nSecond, nNanos);
            }
        else
            {
            throw new IOException("Attempted to read time with zone information as LocalTime");
            }
        }

    /**
    * Read a OffsetDateTime value from a POF stream.
    *
    * @param in  the stream containing the POF date value
    *
    * @return a OffsetDateTime object
    *
    * @throws IOException on read error
    */
    public static OffsetDateTime readOffsetDateTime(ReadBuffer.BufferInput in)
            throws IOException
        {
        LocalDate  date = readLocalDate(in);
        OffsetTime time = readOffsetTime(in);

        return OffsetDateTime.of(date, time.toLocalTime(), time.getOffset());
        }

    /**
    * Read a OffsetTime value from a POF stream.
    *
    * @param in  the stream containing the POF date value
    *
    * @return a OffsetTime object
    *
    * @throws IOException on read error
    */
    public static OffsetTime readOffsetTime(ReadBuffer.BufferInput in)
            throws IOException
        {
        int nHour     = in.readPackedInt();
        int nMinute   = in.readPackedInt();
        int nSecond   = in.readPackedInt();
        int nFraction = in.readPackedInt();
        int nNanos    = nFraction <= 0 ? -nFraction : nFraction * 1000000;

        byte nZoneType = in.readByte();
        switch (nZoneType)
            {
            case 1:
                return OffsetTime.of(nHour, nMinute, nSecond, nNanos, ZoneOffset.UTC);

            case 2:
                int nOfHours = in.readPackedInt();
                int nOfMin   = in.readPackedInt();
                return OffsetTime.of(nHour, nMinute, nSecond, nNanos, ZoneOffset.ofHoursMinutes(nOfHours, nOfMin));

            default:
                throw new IOException("Attempted to read time without zone information as OffsetTime");
            }
        }

    /**
    * Read a RawDate value from a POF stream.
    *
    * @param in  the stream containing the POF date value
    *
    * @return a RawDate object
    *
    * @throws IOException on read error
    */
    public static RawDate readRawDate(ReadBuffer.BufferInput in)
            throws IOException
        {
        int nYear  = in.readPackedInt();
        int nMonth = in.readPackedInt();
        int nDay   = in.readPackedInt();
        return new RawDate(nYear, nMonth, nDay);
        }

    /**
    * Read a RawTime value from a POF stream.
    *
    * @param in  the stream containing the POF time value
    *
    * @return a RawTime object
    *
    * @throws IOException on read error
    */
    public static RawTime readRawTime(ReadBuffer.BufferInput in)
            throws IOException
        {
        RawTime time;

        int nHour     = in.readPackedInt();
        int nMinute   = in.readPackedInt();
        int nSecond   = in.readPackedInt();
        int nFraction = in.readPackedInt();
        int nNanos    = nFraction <= 0 ? -nFraction : nFraction * 1000000;

        byte nZoneType = in.readByte();
        if (nZoneType == 2)
            {
            int nHourOffset   = in.readPackedInt();
            int nMinuteOffset = in.readPackedInt();
            time = new RawTime(nHour, nMinute, nSecond,
                           nNanos, nHourOffset, nMinuteOffset);
            }
        else
            {
            assert nZoneType == 0 || nZoneType == 1;
            boolean fUTC = nZoneType == 1;
            time = new RawTime(nHour, nMinute, nSecond,
                           nNanos, fUTC);
            }

        return time;
        }

    /**
    * Read a value of the specified encoding from the POF stream and convert
    * it to a char.
    *
    * @param in     the POF stream containing the value
    * @param nType  the POF type of the value
    *
    * @return the POF value as an char
    *
    * @throws IOException if an I/O error occurs reading the POF stream, or
    *         the POF value cannot be coerced to a char value
    */
    public static char readAsChar(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        switch (nType)
            {
            case T_OCTET:
                return (char) in.readUnsignedByte();

            case T_CHAR:
                return readChar(in);

            default:
                return (char) readAsInt(in, nType);
            }
        }

    /**
    * Read a value of the specified encoding from the POF stream and convert
    * it to an integer.
    *
    * @param in     the POF stream containing the value
    * @param nType  the POF type of the value
    *
    * @return the POF value as an integer
    *
    * @throws IOException if an I/O error occurs reading the POF stream, or
    *         the POF value cannot be coerced to an integer value
    */
    public static int readAsInt(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        switch (nType)
            {
            case T_BOOLEAN:
            case T_INT16:
            case T_INT32:
            case T_INT64:
            case T_INT128:
                return in.readPackedInt();

            case T_FLOAT32:
                return (int) in.readFloat();

            case T_FLOAT64:
                return (int) in.readDouble();

            case T_FLOAT128:
                return readQuad(in).intValue();

            case T_DECIMAL32:
                return readBigDecimal(in, 4).intValue();

            case T_DECIMAL64:
                return readBigDecimal(in, 8).intValue();

            case T_DECIMAL128:
                return readBigDecimal(in, 16).intValue();

            case T_OCTET:
                return in.readUnsignedByte();

            case T_CHAR:
                return readChar(in);

            case V_REFERENCE_NULL:
            case V_BOOLEAN_FALSE:
            case V_INT_0:
                return 0;

            case V_BOOLEAN_TRUE:
            case V_INT_1:
                return 1;

            case V_INT_NEG_1:
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
                return decodeTinyInt(nType);

            default:
                throw new IOException("unable to convert type " + nType
                    + " to a numeric type");
            }

        }

    /**
    * Read a value of the specified encoding from the POF stream and convert
    * it to a long.
    *
    * @param in     the POF stream containing the value
    * @param nType  the POF type of the value
    *
    * @return the POF value as a long
    *
    * @throws IOException if an I/O error occurs reading the POF stream, or
    *         the POF value cannot be coerced to a long value
    */
    public static long readAsLong(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        switch (nType)
            {
            case T_INT64:
            case T_INT128:
                return in.readPackedLong();

            case T_FLOAT32:
                return (long) in.readFloat();

            case T_FLOAT64:
                return (long) in.readDouble();

            case T_FLOAT128:
                return readQuad(in).longValue();

            case T_DECIMAL32:
                return readBigDecimal(in, 4).longValue();

            case T_DECIMAL64:
                return readBigDecimal(in, 8).longValue();

            case T_DECIMAL128:
                return readBigDecimal(in, 16).longValue();

            default:
                return readAsInt(in, nType);
            }
        }

    /**
    * Read a value of the specified encoding from the POF stream and convert
    * it to a float.
    *
    * @param in     the POF stream containing the value
    * @param nType  the POF type of the value
    *
    * @return the POF value as a float
    *
    * @throws IOException if an I/O error occurs reading the POF stream, or
    *         the POF value cannot be coerced to a float value
    */
    public static float readAsFloat(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        switch (nType)
            {
            case T_INT64:
                return (float) in.readPackedLong();

            case T_FLOAT32:
                return in.readFloat();

            case T_FLOAT64:
                return (float) in.readDouble();

            case T_FLOAT128:
                return readQuad(in).floatValue();

            case T_INT128:
                return readBigInteger(in).floatValue();

            case T_DECIMAL32:
                return readBigDecimal(in, 4).floatValue();

            case T_DECIMAL64:
                return readBigDecimal(in, 8).floatValue();

            case T_DECIMAL128:
                return readBigDecimal(in, 16).floatValue();

            case V_FP_NEG_INFINITY:
                return Float.NEGATIVE_INFINITY;

            case V_FP_POS_INFINITY:
                return Float.POSITIVE_INFINITY;

            case V_FP_NAN:
                return Float.NaN;

            default:
                return readAsInt(in, nType);
            }
        }

    /**
    * Read a value of the specified encoding from the POF stream and convert
    * it to a double.
    *
    * @param in     the POF stream containing the value
    * @param nType  the POF type of the value
    *
    * @return the POF value as a double
    *
    * @throws IOException if an I/O error occurs reading the POF stream, or
    *         the POF value cannot be coerced to a double value
    */
    public static double readAsDouble(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        switch (nType)
            {
            case T_INT64:
                return (double) in.readPackedLong();

            case T_FLOAT32:
                return (double) in.readFloat();

            case T_FLOAT64:
                return in.readDouble();

            case T_FLOAT128:
                return readQuad(in).doubleValue();

            case T_INT128:
                return readBigInteger(in).doubleValue();

            case T_DECIMAL32:
            case T_DECIMAL64:
            case T_DECIMAL128:
                return readAsBigDecimal(in, nType).doubleValue();

            case V_FP_NEG_INFINITY:
                return Double.NEGATIVE_INFINITY;

            case V_FP_POS_INFINITY:
                return Double.POSITIVE_INFINITY;

            case V_FP_NAN:
                return Double.NaN;

            default:
                return readAsInt(in, nType);
            }
        }

    /**
    * Read a value of the specified encoding from the POF stream and convert
    * it to a quad.
    *
    * @param in     the POF stream containing the value
    * @param nType  the POF type of the value
    *
    * @return the POF value as a RawQuad
    *
    * @throws IOException if an I/O error occurs reading the POF stream, or
    *         the POF value cannot be coerced to a double value
    */
    public static RawQuad readAsQuad(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        if (nType == T_FLOAT128)
            {
            return readQuad(in);
            }
        else
            {
            return new RawQuad(readAsDouble(in, nType));
            }
        }

    /**
    * Read a value of the specified encoding from the POF stream and convert
    * it to a BigInteger.
    *
    * @param in     the POF stream containing the value
    * @param nType  the POF type of the value
    *
    * @return the POF value as a BigInteger
    *
    * @throws IOException if an I/O error occurs reading the POF stream, or
    *         the POF value cannot be coerced to a BigInteger value
    */
    public static BigInteger readAsBigInteger(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        switch (nType)
            {
            case T_INT128:
                return readBigInteger(in);

            case T_FLOAT32:
            case T_FLOAT64:
            case T_FLOAT128:
            case T_DECIMAL32:
            case T_DECIMAL64:
            case T_DECIMAL128:
                return (BigInteger) convertNumber(
                        readAsBigDecimal(in, nType), J_BIG_INTEGER);

            default:
                return BigInteger.valueOf(readAsLong(in, nType));
            }
        }

    /**
    * Read a value of the specified encoding from the POF stream and convert
    * it to a BigDecimal.
    *
    * @param in     the POF stream containing the value
    * @param nType  the POF type of the value
    *
    * @return the POF value as a BigDecimal
    *
    * @throws IOException if an I/O error occurs reading the POF stream, or
    *         the POF value cannot be coerced to a BigDecimal value
    */
    public static BigDecimal readAsBigDecimal(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        switch (nType)
            {
            case T_INT128:
                return (BigDecimal) convertNumber(readBigInteger(in), J_BIG_DECIMAL);

            case T_FLOAT32:
                return BigDecimal.valueOf((double) in.readFloat());

            case T_FLOAT64:
                return BigDecimal.valueOf(in.readDouble());

            case T_FLOAT128:
                return BigDecimal.valueOf(readQuad(in).doubleValue());

            case T_DECIMAL32:
                return readBigDecimal(in, 4);

            case T_DECIMAL64:
                return readBigDecimal(in, 8);

            case T_DECIMAL128:
                return readBigDecimal(in, 16);

            default:
                return BigDecimal.valueOf(readAsLong(in, nType));
            }
        }

    /**
    * Within the POF stream, skip the next POF value.
    *
    * @param in  the BufferInput containing the POF stream
    *
    * @throws IOException on read error
    */
    public static void skipValue(ReadBuffer.BufferInput in)
            throws IOException
        {
        int nType = in.readPackedInt();
        if (nType == T_IDENTITY)
            {
            skipPackedInts(in, 1);
            nType = in.readPackedInt();
            }

        skipUniformValue(in, nType);
        }

    /**
    * Within the POF stream, skip the next POF value of the specified type.
    *
    * @param in  the BufferInput containing the POF stream
    * @param nType  the type of the value to skip
    *
    * @throws IOException on read error
    */
    public static void skipUniformValue(ReadBuffer.BufferInput in, int nType)
            throws IOException
        {
        switch (nType)
            {
            case T_INT16:                  // int16
            case T_INT32:                  // int32
            case T_INT64:                  // int64
            case T_INT128:                 // int128
            case T_REFERENCE:              // reference
            case T_BOOLEAN:                // boolean
                skipPackedInts(in, 1);
                break;

            case T_YEAR_MONTH_INTERVAL:    // year-month-interval
                skipPackedInts(in, 2);
                break;

            case T_DATE:                   // date
                skipPackedInts(in, 3);
                break;

            case T_TIME_INTERVAL:          // time-interval
                skipPackedInts(in, 4);
                break;

            case T_DAY_TIME_INTERVAL:      // day-time-interval
                skipPackedInts(in, 5);
                break;

            case T_FLOAT32:                // float32
                in.skipBytes(4);
                break;

            case T_FLOAT64:                // float64
                in.skipBytes(8);
                break;

            case T_FLOAT128:               // float128*
                in.skipBytes(16);
                break;

            case T_DECIMAL32:              // decimal32
            case T_DECIMAL64:              // decimal64
            case T_DECIMAL128:             // decimal128
                skipPackedInts(in, 2);
                break;

            case T_OCTET:                  // octet
                in.skipBytes(1);
                break;

            case T_CHAR:                   // char
                switch (in.readUnsignedByte() & 0xF0)
                    {
                    case 0xC0: case 0xD0:
                        in.skipBytes(1);
                        break;

                    case 0xE0:
                        in.skipBytes(2);
                        break;
                    }
                break;

            case T_OCTET_STRING:           // octet-string
            case T_CHAR_STRING:            // char-string
                int cb = in.readPackedInt();

                if (cb == V_REFERENCE_NULL)
                    {
                    break;
                    }

                in.skipBytes(cb);
                break;

            case T_DATETIME:               // datetime
                skipPackedInts(in, 3);
                // fall through (datetime ends with a time)
            case T_TIME:                   // time
                {
                skipPackedInts(in, 4);
                int nZoneType = in.readPackedInt();
                if (nZoneType == 2)
                    {
                    skipPackedInts(in, 2);
                    }
                }
                break;

            case T_COLLECTION:             // collection
            case T_ARRAY:                  // array
                for (int i = 0, c = in.readPackedInt(); i < c; ++i)
                    {
                    skipValue(in);
                    }
                break;

            case T_UNIFORM_COLLECTION:     // uniform-collection
            case T_UNIFORM_ARRAY:          // uniform-array
                for (int i = 0, nElementTypeId = in.readPackedInt(),
                        c = in.readPackedInt(); i < c; ++i)
                    {
                    skipUniformValue(in, nElementTypeId);
                    }
                break;

            case T_SPARSE_ARRAY:           // sparse-array
                for (int i = 0, c = in.readPackedInt(); i < c; ++i)
                    {
                    int iPos = in.readPackedInt();
                    if (iPos < 0)
                        {
                        break;
                        }
                    skipValue(in);
                    }
                break;

            case T_UNIFORM_SPARSE_ARRAY:   // uniform-sparse-array
                for (int i = 0, nElementTypeId = in.readPackedInt(),
                        c = in.readPackedInt(); i < c; ++i)
                    {
                    int iPos = in.readPackedInt();
                    if (iPos < 0)
                        {
                        break;
                        }
                    skipUniformValue(in, nElementTypeId);
                    }
                break;

            case T_MAP:                    // map
                for (int i = 0, c = in.readPackedInt(); i < c; ++i)
                    {
                    skipValue(in); // key
                    skipValue(in); // value
                    }
                break;

            case T_UNIFORM_KEYS_MAP:       // uniform-keys-map
                for (int i = 0, nKeyTypeId = in.readPackedInt(),
                        c = in.readPackedInt(); i < c; ++i)
                    {
                    skipUniformValue(in, nKeyTypeId);
                    skipValue(in);
                    }
                break;

            case T_UNIFORM_MAP:            // uniform-map
                for (int i = 0, nKeyTypeId = in.readPackedInt(),
                        nValueTypeId = in.readPackedInt(),
                        c = in.readPackedInt(); i < c; ++i)
                    {
                    skipUniformValue(in, nKeyTypeId);
                    skipUniformValue(in, nValueTypeId);
                    }
                break;

            case V_BOOLEAN_FALSE:          // boolean:false
            case V_BOOLEAN_TRUE:           // boolean:true
            case V_STRING_ZERO_LENGTH:     // string:zero-length
            case V_COLLECTION_EMPTY:       // collection:empty
            case V_REFERENCE_NULL:         // reference:null
            case V_FP_POS_INFINITY:        // floating-point:+infinity
            case V_FP_NEG_INFINITY:        // floating-point:-infinity
            case V_FP_NAN:                 // floating-point:NaN
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
                break;

            default:
                if (nType >= 0)
                    {
                    // user type
                    // version id, reference id, or T_IDENTITY
                    if (in.readPackedInt() == T_IDENTITY)
                        {
                        // TODO: see COH-11347
                        throw new UnsupportedOperationException("Detected object identity/reference"
                                + " in uniform collection, which is not currently supported");
                        }
                    while (in.readPackedInt() >= 0)
                        {
                        skipValue(in);
                        }
                    }
                else
                    {
                    throw new StreamCorruptedException("type=" + nType);
                    }
                break;
            }
        }

    /**
    * Skip the specified number of packed integers in the passed POF stream.
    *
    * @param in  the BufferInput containing the POF stream
    * @param c   the number of packed integers to skip over
    *
    * @throws IOException on read error
    */
    public static void skipPackedInts(ReadBuffer.BufferInput in, int c)
            throws IOException
        {
        int n = 0;
        while (c-- > 0)
            {
            for (n = in.read(); (n & 0xFFFFFF80) == 0x80; n = in.read()) {}
            }

        if (n == -1)
            {
            throw new EOFException();
            }
        }


    // ----- encoding -------------------------------------------------------

    /**
    * Write a BigInteger as a packed int.
    *
    * @param out  the DataOutput to write to
    * @param n    the BigInteger value
    *
    * @throws IOException on write error
    */
    public static void writeBigInteger(WriteBuffer.BufferOutput out, BigInteger n)
            throws IOException
        {
        int cBits = n.bitLength();
        if (cBits <= 63)
            {
            out.writePackedLong(n.longValue());
            }
        else
            {
            byte[] ab = n.toByteArray();
            int    cb = ab.length;
            if (cb > 16)
                {
                throw new IllegalStateException(
                        "too many bits for 128-bit integer: " + (cBits + 1));
                }

            int b = 0;

            // check for negative
            if ((ab[0] & 0x80) != 0)
                {
                b = 0x40;
                for (int of = 0; of < cb; ++of)
                    {
                    ab[of] = (byte) ~ab[of];
                    }
                }

            // trim off the leading zeros
            int    ofMSB = 0;
            while (ofMSB < cb && ab[ofMSB] == 0)
                {
                ++ofMSB;
                }

            if (ofMSB < cb)
                {
                int of    = cb - 1;
                int nBits = ab[of] & 0xFF;

                b |= (byte) (nBits & 0x3F);
                nBits >>>= 6;
                cBits    = 2;  // only 2 data bits left in nBits

                while (nBits != 0 || of > ofMSB)
                    {
                    b |= 0x80;
                    out.writeByte(b);

                    // load more data bits if necessary
                    if (cBits < 7)
                        {
                        nBits |= (--of < 0 ? 0 : ab[of] & 0xFF) << cBits;
                        cBits += 8;
                        }

                    b = (nBits & 0x7F);
                    nBits >>>= 7;
                    cBits   -= 7;
                    }
                }

            out.writeByte(b);
            }
        }

    /**
    * Write a BigDecimal to the passed DataOutput stream as a decimal value.
    *
    * @param out     the DataOutput to write to
    * @param dec     the BigDecimal value
    * @param cBytes  the number of bytes for the IEEE754 value: 4, 8 or 16
    *
    * @throws IOException on write error
    */
    public static void writeBigDecimal(WriteBuffer.BufferOutput out, BigDecimal dec, int cBytes)
            throws IOException
        {
        checkDecimalRange(dec, cBytes);

        writeBigInteger(out, dec.unscaledValue());
        out.writePackedInt(dec.scale());
        }

    /**
    * Encode an integer value into one of the reserved single-byte combined
    * type and value indicators.
    *
    * @param n  an integer between -1 and 22 inclusive
    *
    * @return the integer value that the integer is encoded as
    */
    public static int encodeTinyInt(int n)
        {
        assert n >= -1 && n <= 22;
        return V_INT_0 - n;
        }

    /**
    * Write a date value to a BufferOutput object.
    *
    * @param out     the BufferOutput to write to
    * @param nYear   the year number as defined by ISO8601; note the
    *                difference with the Java Date class, whose year is
    *                relative to 1900
    * @param nMonth  the month number between 1 and 12 inclusive as defined
    *                by ISO8601; note the difference from the Java Date
    *                class, whose month value is 0-based (0-11)
    * @param nDay    the day number between 1 and 31 inclusive as defined by
    *                ISO8601
    *
    * @throws IOException thrown if the passed BufferOutput object throws an
    *                     IOException while the value is being written to it
    */
    public static void writeDate(WriteBuffer.BufferOutput out, int nYear, int nMonth, int nDay)
            throws IOException
        {
        out.writePackedInt(nYear);
        out.writePackedInt(nMonth);
        out.writePackedInt(nDay);
        }

    /**
    * Write a time value to a BufferOutput object.
    *
    * @param out           the BufferOutput to write to
    * @param nHour         the hour between 0 and 23 inclusive
    * @param nMinute       the minute value between 0 and 59 inclusive
    * @param nSecond       the second value between 0 and 59 inclusive (and
    *                      theoretically 60 for a leap-second)
    * @param nNano         the nanosecond value between 0 and 999999999
    *                      inclusive
    * @param nTimeZoneType 0 if the time value does not have an explicit time
    *                      zone, 1 if the time value is UTC and 2 if the time
    *                      zone has an explicit hour and minute offset
    * @param nHourOffset   the timezone offset in hours from UTC, for example
    *                      0 for BST, -5 for EST and 1 for CET
    * @param nMinuteOffset the timezone offset in minutes, for example 0 (in
    *                      most cases) or 30
    *
    * @throws IOException thrown if the passed BufferOutput object throws an
    *                     IOException while the value is being written to it
    */
    public static void writeTime(WriteBuffer.BufferOutput out, int nHour, int nMinute,
            int nSecond, int nNano, int nTimeZoneType, int nHourOffset,
            int nMinuteOffset)
            throws IOException
        {
        int nFraction = 0;
        if (nNano != 0)
            {
            nFraction = nNano % 1000000 == 0 ? nNano / 1000000 : -nNano;
            }

        out.writePackedInt(nHour);
        out.writePackedInt(nMinute);
        out.writePackedInt(nSecond);
        out.writePackedInt(nFraction);
        out.writeByte(nTimeZoneType);

        if (nTimeZoneType == 2)
            {
            out.writePackedInt(nHourOffset);
            out.writePackedInt(nMinuteOffset);
            }
        }


    // ----- validation -----------------------------------------------------

    /**
    * Validate a type identifier.
    *
    * @param nType  the type identifier
    */
    public static void checkType(int nType)
        {
        // user types are all values >= 0
        if (nType < 0)
            {
            // all types < 0 are pre-defined
            switch (nType)
                {
                case T_INT16:
                case T_INT32:
                case T_INT64:
                case T_INT128:
                case T_FLOAT32:
                case T_FLOAT64:
                case T_FLOAT128:
                case T_DECIMAL32:
                case T_DECIMAL64:
                case T_DECIMAL128:
                case T_BOOLEAN:
                case T_OCTET:
                case T_OCTET_STRING:
                case T_CHAR:
                case T_CHAR_STRING:
                case T_DATE:
                case T_YEAR_MONTH_INTERVAL:
                case T_TIME:
                case T_TIME_INTERVAL:
                case T_DATETIME:
                case T_DAY_TIME_INTERVAL:
                case T_COLLECTION:
                case T_UNIFORM_COLLECTION:
                case T_ARRAY:
                case T_UNIFORM_ARRAY:
                case T_SPARSE_ARRAY:
                case T_UNIFORM_SPARSE_ARRAY:
                case T_MAP:
                case T_UNIFORM_KEYS_MAP:
                case T_UNIFORM_MAP:
                case T_IDENTITY:
                case T_REFERENCE:
                    break;

                default:
                    throw new IllegalStateException("unknown type: " + nType);
                }
            }
        }

    /**
    * Verify that the number of elements is valid.
    *
    * @param cElements  the number of elements in a complex data structure
    */
    public static void checkElementCount(int cElements)
        {
        if (cElements < 0)
            {
            throw new IllegalStateException("illegal element count: " + cElements);
            }
        }

    /**
    * Validate a reference identifier to make sure it is in a valid range.
    *
    * @param nId  the reference identity
    */
    public static void checkReferenceRange(int nId)
        {
        if (nId < 0)
            {
            throw new IllegalStateException("illegal reference identity: " + nId);
            }
        }

    /**
    * Verify that the specified decimal value will fit in the specified
    * number of bits.
    *
    * @param dec     the decimal value
    * @param cBytes  the number of bytes (4, 8 or 16)
    */
    public static void checkDecimalRange(BigDecimal dec, int cBytes)
        {
        BigInteger nUnscaled = dec.unscaledValue();
        int        nScale    = dec.scale();

        switch (cBytes)
            {
            case 4:
                if (nUnscaled.abs().compareTo(MAX_DECIMAL32_UNSCALED) > 0
                        || nScale < MIN_DECIMAL32_SCALE
                        || nScale > MAX_DECIMAL32_SCALE)
                    {
                    throw new IllegalStateException(
                        "decimal value exceeds IEEE754r 32-bit range: " + dec);
                    }
                break;

            case 8:
                if (nUnscaled.abs().compareTo(MAX_DECIMAL64_UNSCALED) > 0
                        || nScale < MIN_DECIMAL64_SCALE
                        || nScale > MAX_DECIMAL64_SCALE)
                    {
                    throw new IllegalStateException(
                        "decimal value exceeds IEEE754r 64-bit range: " + dec);
                    }
                break;

            case 16:
                if (nUnscaled.abs().compareTo(MAX_DECIMAL128_UNSCALED) > 0
                        || nScale < MIN_DECIMAL128_SCALE
                        || nScale > MAX_DECIMAL128_SCALE)
                    {
                    throw new IllegalStateException(
                        "decimal value exceeds IEEE754r 128-bit range: " + dec);
                    }
                break;

            default:
                throw new IllegalArgumentException("byte count (" + cBytes
                        + ") must be 4, 8 or 16");
            }
        }

    /**
    * Determine the minimum size (in bytes) of the IEEE754 decimal type that
    * would be capable of holding the passed value.
    *
    * @param dec     the decimal value
    *
    * @return the number of bytes (4, 8 or 16)
    */
    public static int calcDecimalSize(BigDecimal dec)
        {
        BigInteger nUnscaled = dec.unscaledValue();
        int        nScale    = dec.scale();

        if (nUnscaled.abs().compareTo(MAX_DECIMAL32_UNSCALED) <= 0
                && nScale >= MIN_DECIMAL32_SCALE
                && nScale <= MAX_DECIMAL32_SCALE)
            {
            return 4;
            }

        if (nUnscaled.abs().compareTo(MAX_DECIMAL64_UNSCALED) <= 0
                && nScale >= MIN_DECIMAL64_SCALE
                && nScale <= MAX_DECIMAL64_SCALE)
            {
            return 8;
            }

        if (nUnscaled.abs().compareTo(MAX_DECIMAL128_UNSCALED) <= 0
                && nScale >= MIN_DECIMAL128_SCALE
                && nScale <= MAX_DECIMAL128_SCALE)
            {
            return 16;
            }

        throw new IllegalStateException(
            "decimal value exceeds IEEE754r 128-bit range: " + dec);
        }

    /**
    * Validate date information.
    *
    * @param nYear   the year number
    * @param nMonth  the month number
    * @param nDay    the day number
    */
    public static void checkDate(int nYear, int nMonth, int nDay)
        {
        if (nMonth < 1 || nMonth > 12)
            {
            throw new IllegalStateException("month is out of range: " + nMonth);
            }

        if (nDay < 1 || nDay > MAX_DAYS_PER_MONTH[nMonth-1])
            {
            throw new IllegalStateException("day is out of range: " + nDay);
            }

        if (nMonth == 2 && nDay == 29 && (nYear % 4 != 0
                || (nYear % 100 == 0 && nYear % 400 != 0)))
            {
            throw new IllegalStateException("not a leap year: " + nYear);
            }
        }

    /**
    * Validate time information.
    *
    * @param nHour    the hour number
    * @param nMinute  the minute number
    * @param nSecond  the second number
    * @param nNano    the nanosecond number
    */
    public static void checkTime(int nHour, int nMinute, int nSecond, int nNano)
        {
        if (nHour < 0 || nHour > 23)
            {
            if (nHour == 24 && nMinute == 0 && nSecond == 0 && nNano == 0)
                {
                throw new IllegalStateException(
                        "end-of-day midnight (24:00:00.0) is supported by ISO8601"
                        + ", but use 00:00:00.0 instead");
                }
            else
                {
                throw new IllegalStateException("hour is out of range: " + nHour);
                }
            }

        if (nMinute < 0 || nMinute > 59)
            {
            throw new IllegalStateException("minute is out of range: " + nMinute);
            }

        // 60 is allowed for a leap-second
        if (nSecond < 0 || (nSecond == 60 && nNano > 0) || nSecond > 60)
            {
            throw new IllegalStateException("second is out of range: " + nSecond);
            }

        if (nNano < 0 || nNano > 999999999)
            {
            throw new IllegalStateException("nanosecond is out of range: " + nNano);
            }
        }

    /**
    * Check the specified timezone offset.
    *
    * @param nHourOffset    the hour offset
    * @param nMinuteOffset  the minute offset
    */
    public static void checkTimeZone(int nHourOffset, int nMinuteOffset)
        {
        // technically this is reasonable, but in reality iw should never be
        // over 14; unfortunately, countries keep changing theirs for silly
        // reasons; see http://www.worldtimezone.com/faq.html
        if (nHourOffset < -23 || nHourOffset > 23)
            {
            throw new IllegalStateException("invalid hour offset: " + nHourOffset);
            }

        // The minute offset should be 0, 15, 30 or 45 for standard timezones, but for
        // non-standard timezones, the minute offset could be any number between 0 and 59.
        // For example, Hong Kong switched from local mean time to standard time in 1904,
        // so prior to 1904, the minute offset was 36.See http://en.wikipedia.org/wiki/Standard_time
        if (nMinuteOffset < 0 || nMinuteOffset > 59)
            {
            throw new IllegalStateException("invalid minute offset: " + nMinuteOffset);
            }
        }

    /**
    * Validate a year-month interval.
    *
    * @param cYears   the number of years
    * @param cMonths  the number of months
    */
    public static void checkYearMonthInterval(int cYears, int cMonths)
        {
        if (cYears == 0)
            {
            if (cMonths < -11 || cMonths > 11)
                {
                throw new IllegalStateException(
                        "month interval is out of range: " + cMonths);
                }
            }
        }

    /**
    * Validate a time interval.
    *
    * @param cHours    the number of hours
    * @param cMinutes  the number of minutes
    * @param cSeconds  the number of seconds
    * @param cNanos    the number of nanoseconds
    */
    public static void checkTimeInterval(int cHours, int cMinutes,
            int cSeconds, int cNanos)
        {
        // duration is allowed to be negative
        if (cHours == 0)
            {
            if (cMinutes == 0)
                {
                if (cSeconds == 0)
                    {
                    cNanos = Math.abs(cNanos);
                    }
                else
                    {
                    cSeconds = Math.abs(cSeconds);
                    }
                }
            else
                {
                cMinutes = Math.abs(cMinutes);
                }
            }
        else
            {
            cHours = Math.abs(cHours);
            }

        // apply the same rules as limit the time values themselves
        checkTime(cHours, cMinutes, cSeconds, cNanos);
        }

    /**
    * Validate a day-time interval.
    * <p>
    * See http://www.builderau.com.au/architect/database/soa/SQL_basics_Datetime_and_interval_data_types/0,39024547,20269031,00.htm
    *
    * @param cDays     the number of days
    * @param cHours    the number of hours
    * @param cMinutes  the number of minutes
    * @param cSeconds  the number of seconds
    * @param cNanos    the number of nanoseconds
    */
    public static void checkDayTimeInterval(int cDays, int cHours, int cMinutes,
            int cSeconds, int cNanos)
        {
        if (cDays == 0)
            {
            checkTimeInterval(cHours, cMinutes, cSeconds, cNanos);
            }
        else
            {
            // number of days is permitted to be any value

            // apply the same rules as limit the time values themselves
            checkTime(cHours, cMinutes, cSeconds, cNanos);
            }
        }


    // ----- String formatting ----------------------------------------------

    /**
    * Format a date in the form YYYY-MM-DD.
    *
    * @param nYear   the year number
    * @param nMonth  the month number
    * @param nDay    the day number
    *
    * @return the formatted string
    */
    public static String formatDate(int nYear, int nMonth, int nDay)
        {
        return toDecString(nYear,  Math.max(4, getMaxDecDigits(nYear))) + "-"
             + toDecString(nMonth, Math.max(2, getMaxDecDigits(nMonth))) + "-"
             + toDecString(nDay,   Math.max(2, getMaxDecDigits(nDay)));
        }

    /**
    * Format a time using the simplest applicable of the following formats:
    * <ol>
    * <li><tt>HH:MM</tt></li>
    * <li><tt>HH:MM:SS</tt></li>
    * <li><tt>HH:MM:SS.MMM</tt></li>
    * <li><tt>HH:MM:SS.NNNNNNNNN</tt></li>
    * </ol>
    *
    * @param nHour    the hour number
    * @param nMinute  the minute number
    * @param nSecond  the second number
    * @param nNano    the nanosecond number
    * @param fUTC     true for UTC, false for no time zone
    *
    * @return a time String
    */
    public static String formatTime(int nHour, int nMinute, int nSecond,
            int nNano, boolean fUTC)
        {
        StringBuilder sb = new StringBuilder();

        sb.append(toDecString(nHour,  Math.max(2, getMaxDecDigits(nHour))))
          .append(":")
          .append(toDecString(nMinute, Math.max(2, getMaxDecDigits(nMinute))));

        if (nSecond != 0 || nNano != 0)
            {
            sb.append(":")
              .append(toDecString(nSecond, Math.max(2, getMaxDecDigits(nSecond))));

            if (nNano != 0)
                {
                sb.append('.');
                if (nNano % 1000000 == 0)
                    {
                    sb.append(toDecString(nNano / 1000000, 3));
                    }
                else
                    {
                    sb.append(toDecString(nNano, 9));
                    }
                }
            }

        if (fUTC)
            {
            sb.append('Z');
            }

        return sb.toString();
        }

    /**
    * Format a time using the simplest applicable of the following formats:
    * <ol>
    * <li><tt>HH:MM(+|-)HH:MM</tt></li>
    * <li><tt>HH:MM:SS(+|-)HH:MM</tt></li>
    * <li><tt>HH:MM:SS.MMM(+|-)HH:MM</tt></li>
    * <li><tt>HH:MM:SS.NNNNNNNNN(+|-)HH:MM</tt></li>
    * </ol>
    *
    * @param nHour          the hour number
    * @param nMinute        the minute number
    * @param nSecond        the second number
    * @param nNano          the nanosecond number
    * @param nHourOffset    the timezone offset in hours
    * @param nMinuteOffset  the timezone offset in minutes
    *
    * @return a time String
    */
    public static String formatTime(int nHour, int nMinute, int nSecond,
            int nNano, int nHourOffset, int nMinuteOffset)
        {
        StringBuilder sb = new StringBuilder();

        sb.append(formatTime(nHour, nMinute, nSecond, nNano, false));

        if (nHourOffset < 0)
            {
            sb.append('-');
            nHourOffset = -nHourOffset;
            }
        else
            {
            sb.append('+');
            }

        sb.append(toDecString(nHourOffset,  Math.max(2, getMaxDecDigits(nHourOffset))))
          .append(":")
          .append(toDecString(nMinuteOffset, Math.max(2, getMaxDecDigits(nMinuteOffset))));

        return sb.toString();
        }


    // ----- inner class: WriteableEntrySetMap ------------------------------

    /**
    * Immutable Map implementation backed by a Set of Map.Entry objects.
    */
    public static class WriteableEntrySetMap
            extends AbstractMap
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a new WriteableEntrySetMap that is backed by a single
        * Map.Entry object.
        *
        * @param entry  the single Map.Entry in the new WriteableEntrySetMap
        */
        public WriteableEntrySetMap(Map.Entry entry)
            {
            m_setEntries = Collections.singleton(entry);
            }

        /**
        * Construct a new WriteableEntrySetMap that is backed by the given
        * Set of Map.Entry objects.
        *
        * @param setEntries  a Set of Map.Entry objects in the new
        *                    WriteableEntrySetMap
        */
        public WriteableEntrySetMap(Set setEntries)
            {
            m_setEntries = Collections.unmodifiableSet(setEntries);
            }

        // ----- Map interface ------------------------------------------

        /**
        * Returns a set view of the mappings contained in this map.  Each element
        * in the returned set is a <tt>Map.Entry</tt>.  The set is backed by the
        * map, so changes to the map are reflected in the set, and vice-versa.
        * If the map is modified while an iteration over the set is in progress,
        * the results of the iteration are undefined.  The set supports element
        * removal, which removes the corresponding mapping from the map, via the
        * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
        * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not support
        * the <tt>add</tt> or <tt>addAll</tt> operations.
        *
        * @return a set view of the mappings contained in this map.
        */
        public Set entrySet()
            {
            return m_setEntries;
            }

        // ----- data members -------------------------------------------

        /**
        * The backing Set of Map.Entry objects.
        */
        private Set m_setEntries;
        }


    // ----- inner class: ReadableEntrySetMap -------------------------------

    /**
    * Map implementation backed by a List of Map.Entry objects.
    */
    public static class ReadableEntrySetMap
            extends AbstractMap
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a new ReadableEntrySetMap.
        */
        public ReadableEntrySetMap()
            {
            clear();
            }

        // ----- Map interface ------------------------------------------

        /**
        * Associates the specified value with the specified key in this map.
        * If the map previously contained a mapping for this key, the old
        * value is replaced.
        * <p>
        * This method is not synchronized; it only synchronizes internally if
        * it has to add a new Entry.  To ensure that the value does not change
        * (or the Entry is not removed) before this method returns, the caller
        * must synchronize on the map before calling this method.
        *
        * @param oKey key with which the specified value is to be associated
        *
        * @param oValue value to be associated with the specified key
        *
        * @return previous value associated with specified key, or <tt>null</tt>
        *          if there was no mapping for key.  A <tt>null</tt> return can
        *          also indicate that the map previously associated <tt>null</tt>
        *          with the specified key, if the implementation supports
        *          <tt>null</tt> values
        */
        public Object put(Object oKey, Object oValue)
            {
            m_listEntries.add(new SimpleMapEntry(oKey, oValue));
            return null;
            }

        /**
        * Removes all mappings from this map.
        */
        public void clear()
            {
            m_listEntries = new ArrayList();
            }

        /**
        * Returns a set view of the mappings contained in this map.  Each element
        * in the returned set is a <tt>Map.Entry</tt>.  The set is backed by the
        * map, so changes to the map are reflected in the set, and vice-versa.
        * If the map is modified while an iteration over the set is in progress,
        * the results of the iteration are undefined.  The set supports element
        * removal, which removes the corresponding mapping from the map, via the
        * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
        * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not support
        * the <tt>add</tt> or <tt>addAll</tt> operations.
        *
        * @return a set view of the mappings contained in this map.
        */
        public Set entrySet()
            {
            return new ImmutableArrayList(m_listEntries);
            }

        // ----- data members -------------------------------------------

        /**
        * The backing List of Map.Entry objects.
        */
        private List m_listEntries;
        }


    // ----- constants ------------------------------------------------------

    private final static Map JAVA_TO_POF_TYPE;
    static
        {
        Map map = new HashMap();
        map.put(Short.TYPE                , T_INT16);
        map.put(Short.class               , T_INT16);
        map.put(Integer.TYPE              , T_INT32);
        map.put(Integer.class             , T_INT32);
        map.put(Long.TYPE                 , T_INT64);
        map.put(Long.class                , T_INT64);
        map.put(Float.TYPE                , T_FLOAT32);
        map.put(Float.class               , T_FLOAT32);
        map.put(Double.TYPE               , T_FLOAT64);
        map.put(Double.class              , T_FLOAT64);
        map.put(RawQuad.class             , T_FLOAT128);
        map.put(Boolean.TYPE              , T_BOOLEAN);
        map.put(Boolean.class             , T_BOOLEAN);
        map.put(Byte.TYPE                 , T_OCTET);
        map.put(Byte.class                , T_OCTET);
        map.put(Binary.class              , T_OCTET_STRING);
        map.put(Character.TYPE            , T_CHAR);
        map.put(Character.class           , T_CHAR);
        map.put(String.class              , T_CHAR_STRING);
        map.put(Date.class                , T_DATETIME);
        map.put(java.sql.Date.class       , T_DATE);
        map.put(Time.class                , T_TIME);
        map.put(Timestamp.class           , T_DATETIME);
        map.put(RawDate.class             , T_DATE);
        map.put(RawTime.class             , T_TIME);
        map.put(RawDateTime.class         , T_DATETIME);
        map.put(RawYearMonthInterval.class, T_YEAR_MONTH_INTERVAL);
        map.put(RawTimeInterval.class     , T_TIME_INTERVAL);
        map.put(RawDayTimeInterval.class  , T_DAY_TIME_INTERVAL);
        map.put(boolean[].class           , T_UNIFORM_ARRAY);
        map.put(byte[].class              , T_UNIFORM_ARRAY);
        map.put(char[].class              , T_CHAR_STRING);
        map.put(short[].class             , T_UNIFORM_ARRAY);
        map.put(int[].class               , T_UNIFORM_ARRAY);
        map.put(long[].class              , T_UNIFORM_ARRAY);
        map.put(float[].class             , T_UNIFORM_ARRAY);
        map.put(double[].class            , T_UNIFORM_ARRAY);
        map.put(ArrayList.class           , T_COLLECTION);
        map.put(ImmutableArrayList.class  , T_COLLECTION);
        map.put(Vector.class              , T_COLLECTION);
        map.put(LinkedList.class          , T_COLLECTION);
        map.put(SafeLinkedList.class      , T_COLLECTION);
        map.put(RecyclingLinkedList.class , T_COLLECTION);
        map.put(LiteSet.class             , T_COLLECTION);
        map.put(HashSet.class             , T_COLLECTION);
        map.put(SafeHashSet.class         , T_COLLECTION);
        map.put(TreeSet.class             , T_COLLECTION);
        map.put(LiteMap.class             , T_MAP);
        map.put(Hashtable.class           , T_MAP);
        map.put(HashMap.class             , T_MAP);
        map.put(SafeHashMap.class         , T_MAP);
        map.put(ObservableHashMap.class   , T_MAP);
        map.put(TreeMap.class             , T_MAP);
        JAVA_TO_POF_TYPE = map;
        }

    /**
    * The maximum number of days in each month. Note February.
    */
    private static final int[] MAX_DAYS_PER_MONTH =
            {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    /**
    * The BigInteger representation of Long.MAX_VALUE.
    */
    public static final BigInteger BIGINTEGER_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    /**
    * The BigInteger representation of Long.MIN_VALUE.
    */
    public static final BigInteger BIGINTEGER_MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);

    /**
    * The default BigDecimal value.
    */
    public static final BigDecimal BIGDECIMAL_ZERO = BigDecimal.valueOf(0L);

    /**
    * An empty array of bytes.
    */
    public static final boolean[] BOOLEAN_ARRAY_EMPTY = new boolean[0];

    /**
    * An empty array of bytes.
    */
    public static final byte[] BYTE_ARRAY_EMPTY = new byte[0];

    /**
    * An empty array of chars.
    */
    public static final char[] CHAR_ARRAY_EMPTY = new char[0];

    /**
    * An empty array of shorts.
    */
    public static final short[] SHORT_ARRAY_EMPTY = new short[0];

    /**
    * An empty array of ints.
    */
    public static final int[] INT_ARRAY_EMPTY = new int[0];

    /**
    * An empty array of longs.
    */
    public static final long[] LONG_ARRAY_EMPTY = new long[0];

    /**
    * An empty array of floats.
    */
    public static final float[] FLOAT_ARRAY_EMPTY = new float[0];

    /**
    * An empty array of doubles.
    */
    public static final double[] DOUBLE_ARRAY_EMPTY = new double[0];

    /**
    * An empty array of objects.
    */
    public static final Object[] OBJECT_ARRAY_EMPTY = new Object[0];

    /**
    * An empty (and immutable) collection.
    */
    public static final Collection COLLECTION_EMPTY = new ImmutableArrayList(OBJECT_ARRAY_EMPTY);

    /**
    * An empty Binary value.
    */
    public static final Binary BINARY_EMPTY = new Binary(BYTE_ARRAY_EMPTY);
    }
