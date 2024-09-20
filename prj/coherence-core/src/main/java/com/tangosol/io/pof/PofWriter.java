/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;

import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import java.util.Collection;
import java.util.Date;
import java.util.Map;


/**
* The PofWriter interface provides the capability of writing a set of
* non-primitive Java types ("user types") to a POF stream as an ordered
* sequence of indexed properties.
* <p>
* The serialized format of a POF user type is as follows:
* <ul>
*   <li>Type Identifier</li>
*   <li>Version Identifier</li>
*   <li>[Property Index, Property Value]*</li>
*   <li>-1</li>
* </ul>
* The type identifier is an integer value greater than or equal to zero that
* identifies the non-primitive Java type. The type identifier has no explicit
* or self-describing meaning within the POF stream itself; in other words,
* the type identifier does not contain the actual class definition. Instead,
* the PofWriter and corresponding {@link PofReader} share a
* {@link PofContext} which contains the necessary meta-data, including type
* identifier to Java type mappings.
* <p>
* The version identifier is used to support both backwards and forwards
* compatibility of serialized POF user types. Versioning of user types allows
* the addition of new properties to a user type, but not the replacement or
* removal of properties that existed in a previous version of the user type.
* <p>
* When a version <i>v1</i> of a user type written by a PofWriter is read by
* a PofReader that supports version <i>v2</i> of the same user type, the
* PofReader returns default values for the additional properties of the User
* Type that exist in <i>v2</i> but do not exist in <i>v1</i>. Conversely,
* when a version <i>v2</i> of a user type written by a PofWriter is read by
* a PofReader that supports version <i>v1</i> of the same user type, the
* instance of user type <i>v1</i> must store those additional opaque
* properties for later encoding. The PofReader enables the user type to store
* off the opaque properties in binary form (see
* {@link PofReader#readRemainder}). When the user type is re-encoded, it must
* be done so using the version identifier <i>v2</i>, since it is including
* the unaltered <i>v2</i> properties. The opaque properties are subsequently
* included in the POF stream using the {@link #writeRemainder} method.
* <p>
* Following the version identifier is an ordered sequence of index/value
* pairs, each of which is composed of a property index encoded as
* non-negative integer value whose value is greater than the previous
* property index, and a property value encoded as a POF value. The user type
* is finally terminated with an illegal property index of -1.
* <p>
* <b>Note: To read a property that was written using a PofWriter method,
* the corresponding read method on {@link PofReader} must be used.
* For example, if a property was written using {@link #writeByteArray},
* {@link PofReader#readByteArray} must be used to read the property.</b>
*
* @author cp/jh  2006.07.13
*
* @see PofContext
* @see PofReader
*
* @since Coherence 3.2
*/
public interface PofWriter
    {
    // ----- primitive value support ----------------------------------------

    /**
    * Write a <tt>boolean</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param f      the <tt>boolean</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeBoolean(int iProp, boolean f)
            throws IOException;

    /**
    * Write a <tt>byte</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param b      the <tt>byte</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeByte(int iProp, byte b)
            throws IOException;

    /**
    * Write a <tt>char</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param ch     the <tt>char</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeChar(int iProp, char ch)
            throws IOException;

    /**
    * Write a <tt>short</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param n      the <tt>short</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeShort(int iProp, short n)
            throws IOException;

    /**
    * Write a <tt>int</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param n      the <tt>int</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeInt(int iProp, int n)
            throws IOException;

    /**
    * Write a <tt>long</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param n      the <tt>long</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeLong(int iProp, long n)
            throws IOException;

    /**
    * Write a <tt>float</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param fl     the <tt>float</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeFloat(int iProp, float fl)
            throws IOException;

    /**
    * Write a <tt>double</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param dfl    the <tt>double</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDouble(int iProp, double dfl)
            throws IOException;


    // ----- primitive array support ----------------------------------------

    /**
    * Write a <tt>boolean[]</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param af     the <tt>boolean[]</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeBooleanArray(int iProp, boolean[] af)
            throws IOException;

    /**
    * Write a <tt>byte[]</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param ab     the <tt>byte[]</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public default void writeByteArray(int iProp, byte[] ab)
            throws IOException
        {
        writeByteArray(iProp, ab, 0, ab == null ? 0 : ab.length);
        }

    /**
     * Write {@code cb} bytes of the specified <tt>byte[]</tt> to the POF stream,
     * starting from offset {@code of}.
     *
     * @param iProp  the property index
     * @param ab     the <tt>byte[]</tt> property value to write
     * @param of     the offset to write from
     * @param cb     the number of bytes to write
     *
     * @throws IllegalArgumentException  if the property index is invalid, or
     *         is less than or equal to the index of the previous property
     *         written to the POF stream
     * @throws IOException  if an I/O error occurs
     *
     * @since 24.09
     */
    public void writeByteArray(int iProp, byte[] ab, int of, int cb)
            throws IOException;

    /**
    * Write a <tt>char[]</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param ach    the <tt>char[]</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public default void writeCharArray(int iProp, char[] ach)
            throws IOException
        {
        writeCharArray(iProp, ach, false);
        }

    /**
     * Write a <tt>char[]</tt> property to the POF stream.
     *
     * @param iProp  the property index
     * @param ach    the <tt>char[]</tt> property value to write
     * @param fRaw   the flag specifying whether to write raw bytes for the array
     *               instead of encoding individual elements; raw encoding will
     *               typically perform better, but it may not be portable, so it
     *               should only be used if serialization format portability is
     *               not required
     *
     * @throws IllegalArgumentException  if the property index is invalid, or
     *         is less than or equal to the index of the previous property
     *         written to the POF stream
     * @throws IOException  if an I/O error occurs
     *
     * @since 24.09
     */
    public void writeCharArray(int iProp, char[] ach, boolean fRaw)
            throws IOException;

    /**
    * Write a <tt>short[]</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param an     the <tt>short[]</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public default void writeShortArray(int iProp, short[] an)
            throws IOException
        {
        writeShortArray(iProp, an, false);
        }

    /**
     * Write a <tt>short[]</tt> property to the POF stream.
     *
     * @param iProp  the property index
     * @param an     the <tt>short[]</tt> property value to write
     * @param fRaw   the flag specifying whether to write raw bytes for the array
     *               instead of encoding individual elements; raw encoding will
     *               typically perform better, but it may not be portable, so it
     *               should only be used if serialization format portability is
     *               not required
     *
     * @throws IllegalArgumentException  if the property index is invalid, or
     *         is less than or equal to the index of the previous property
     *         written to the POF stream
     * @throws IOException  if an I/O error occurs
     *
     * @since 24.09
     */
    public void writeShortArray(int iProp, short[] an, boolean fRaw)
            throws IOException;

    /**
    * Write a <tt>int[]</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param an     the <tt>int[]</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public default void writeIntArray(int iProp, int[] an)
            throws IOException
        {
        writeIntArray(iProp, an, false);
        }

    /**
     * Write a <tt>int[]</tt> property to the POF stream.
     *
     * @param iProp  the property index
     * @param an     the <tt>int[]</tt> property value to write
     * @param fRaw   the flag specifying whether to write raw bytes for the array
     *               instead of encoding individual elements; raw encoding will
     *               typically perform better, but it may not be portable, so it
     *               should only be used if serialization format portability is
     *               not required
     *
     * @throws IllegalArgumentException  if the property index is invalid, or
     *         is less than or equal to the index of the previous property
     *         written to the POF stream
     * @throws IOException  if an I/O error occurs
     *
     * @since 24.09
     */
    public void writeIntArray(int iProp, int[] an, boolean fRaw)
            throws IOException;

    /**
    * Write a <tt>long[]</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param an     the <tt>long[]</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public default void writeLongArray(int iProp, long[] an)
            throws IOException
        {
        writeLongArray(iProp, an, false);
        }

    /**
     * Write a <tt>long[]</tt> property to the POF stream.
     *
     * @param iProp  the property index
     * @param al     the <tt>long[]</tt> property value to write
     * @param fRaw   the flag specifying whether to write raw bytes for the array
     *               instead of encoding individual elements; raw encoding will
     *               typically perform better, but it may not be portable, so it
     *               should only be used if serialization format portability is
     *               not required
     *
     * @throws IllegalArgumentException  if the property index is invalid, or
     *         is less than or equal to the index of the previous property
     *         written to the POF stream
     * @throws IOException  if an I/O error occurs
     *
     * @since 24.09
     */
    public void writeLongArray(int iProp, long[] al, boolean fRaw)
            throws IOException;

    /**
    * Write a <tt>float[]</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param afl    the <tt>float[]</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public default void writeFloatArray(int iProp, float[] afl)
            throws IOException
        {
        writeFloatArray(iProp, afl, false);
        }

    /**
     * Write a <tt>float[]</tt> property to the POF stream.
     *
     * @param iProp  the property index
     * @param afl    the <tt>float[]</tt> property value to write
     * @param fRaw   the flag specifying whether to write raw bytes for the array
     *               instead of encoding individual elements; raw encoding will
     *               typically perform better, but it may not be portable, so it
     *               should only be used if serialization format portability is
     *               not required
     *
     * @throws IllegalArgumentException  if the property index is invalid, or
     *         is less than or equal to the index of the previous property
     *         written to the POF stream
     * @throws IOException  if an I/O error occurs
     *
     * @since 24.09
     */
    public void writeFloatArray(int iProp, float[] afl, boolean fRaw)
            throws IOException;

    /**
    * Write a <tt>double[]</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param adfl   the <tt>double[]</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public default void writeDoubleArray(int iProp, double[] adfl)
            throws IOException
        {
        writeDoubleArray(iProp, adfl, false);
        }

    /**
     * Write a <tt>double[]</tt> property to the POF stream.
     *
     * @param iProp  the property index
     * @param adfl   the <tt>double[]</tt> property value to write
     * @param fRaw   the flag specifying whether to write raw bytes for the array
     *               instead of encoding individual elements; raw encoding will
     *               typically perform better, but it may not be portable, so it
     *               should only be used if serialization format portability is
     *               not required
     *
     * @throws IllegalArgumentException  if the property index is invalid, or
     *         is less than or equal to the index of the previous property
     *         written to the POF stream
     * @throws IOException  if an I/O error occurs
     *
     * @since 24.09
     */
    public void writeDoubleArray(int iProp, double[] adfl, boolean fRaw)
            throws IOException;


    // ----- object value support -------------------------------------------

    /**
    * Write a <tt>BigInteger</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param n      the <tt>BigInteger</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IllegalStateException  if the given BigInteger does not fit
    *         into 128 bits
    * @throws IOException  if an I/O error occurs
    */
    public void writeBigInteger(int iProp, BigInteger n)
            throws IOException;

    /**
    * Write a <tt>RawQuad</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param qfl    the <tt>RawQuad</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeRawQuad(int iProp, RawQuad qfl)
            throws IOException;

    /**
    * Write a <tt>BigDecimal</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param dec    the <tt>BigDecimal</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IllegalStateException  if the signed unscaled form of the given
    *         BigDecimal does not fit into 128 bits
    * @throws IOException  if an I/O error occurs
    */
    public void writeBigDecimal(int iProp, BigDecimal dec)
            throws IOException;

    /**
    * Write a <tt>{@link Binary}</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param bin    the <tt>Binary</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeBinary(int iProp, Binary bin)
            throws IOException;

    /**
    * Write a <tt>String</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param s      the <tt>String</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeString(int iProp, String s)
            throws IOException;

    /**
    * Write a <tt>Date</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the year, month and day information of the
    * specified <tt>Date</tt> object. No time or timezone information is
    * encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Date</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDate(int iProp, Date dt)
            throws IOException;

    /**
    * Write a <tt>LocalDate</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the year, month and day information of the
    * specified <tt>LocalDate</tt> object. No time or timezone information is
    * encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>LocalDate</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDate(int iProp, LocalDate dt)
            throws IOException;

    /**
    * Write a <tt>Date</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the year, month, day, hour, minute, second and
    * millisecond information of the specified <tt>Date</tt> object. No
    * timezone information is encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Date</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDateTime(int iProp, Date dt)
            throws IOException;

    /**
    * Write a <tt>LocalDateTime</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the year, month, day, hour, minute, second and
    * millisecond information of the specified <tt>LocalDateTime</tt> object. No
    * timezone information is encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>LocalDateTime</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDateTime(int iProp, LocalDateTime dt)
            throws IOException;

    /**
    * Write a <tt>Timestamp</tt> property to the POF stream in ISO8601
    * format.
    * <p>
    * This method encodes the year, month, day, hour, minute, second,
    * millisecond and nanosecond information of the specified
    * <tt>Timestamp</tt> object. No timezone information is encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Timestamp</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDateTime(int iProp, Timestamp dt)
            throws IOException;

    /**
    * Write a <tt>Date</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the year, month, day, hour, minute, second,
    * millisecond and timezone information of the specified <tt>Date</tt>
    * object.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Date</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDateTimeWithZone(int iProp, Date dt)
            throws IOException;

    /**
    * Write a <tt>OffsetDateTime</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the year, month, day, hour, minute, second,
    * millisecond and timezone information of the specified <tt>OffsetDateTime</tt>
    * object.
    *
    * @param iProp  the property index
    * @param dt     the <tt>OffsetDateTime</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDateTimeWithZone(int iProp, OffsetDateTime dt)
            throws IOException;

    /**
    * Write a <tt>ZonedDateTime</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the year, month, day, hour, minute, second,
    * millisecond and timezone information of the specified <tt>ZonedDateTime</tt>
    * object.
    *
    * @param iProp  the property index
    * @param dt     the <tt>ZonedDateTime</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public default void writeDateTimeWithZone(int iProp, ZonedDateTime dt)
            throws IOException
        {
        writeDateTimeWithZone(iProp, dt.toOffsetDateTime());
        }

    /**
    * Write a <tt>Timestamp</tt> property to the POF stream in ISO8601
    * format.
    * <p>
    * This method encodes the year, month, day, hour, minute, second,
    * millisecond, nanosecond and timezone information of the specified
    * <tt>Timestamp</tt> object.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Timestamp</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeDateTimeWithZone(int iProp, Timestamp dt)
            throws IOException;

    /**
    * Write a <tt>Date</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the hour, minute, second and millisecond
    * information of the specified <tt>Date</tt> object. No year, month, day
    * or timezone information is encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Date</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeTime(int iProp, Date dt)
            throws IOException;

    /**
    * Write a <tt>LocalTime</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the hour, minute, second and millisecond
    * information of the specified <tt>LocalTime</tt> object. No year, month, day
    * or timezone information is encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>LocalTime</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeTime(int iProp, LocalTime dt)
            throws IOException;

    /**
    * Write a <tt>Timestamp</tt> property to the POF stream in ISO8601
    * format.
    * <p>
    * This method encodes the hour, minute, second, millisecond and
    * nanosecond information of the specified <tt>Timestamp</tt> object.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Timestamp</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeTime(int iProp, Timestamp dt)
            throws IOException;

    /**
    * Write a <tt>Date</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the hour, minute, second, millisecond and timezone
    * information of the specified <tt>Date</tt> object. No year, month or
    * day information is encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Date</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeTimeWithZone(int iProp, Date dt)
            throws IOException;

    /**
    * Write a <tt>OffsetTime</tt> property to the POF stream in ISO8601 format.
    * <p>
    * This method encodes the hour, minute, second, millisecond and timezone
    * information of the specified <tt>OffsetTime</tt> object. No year, month or
    * day information is encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>OffsetTime</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeTimeWithZone(int iProp, OffsetTime dt)
            throws IOException;

    /**
    * Write a <tt>Timestamp</tt> property to the POF stream in ISO8601
    * format.
    * <p>
    * This method encodes the hour, minute, second, millisecond, nanosecond
    * and timezone information of the specified <tt>Timestamp</tt> object. No
    * year, month or day information is encoded.
    *
    * @param iProp  the property index
    * @param dt     the <tt>Timestamp</tt> property value to write in ISO8601
    *               format
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeTimeWithZone(int iProp, Timestamp dt)
            throws IOException;

    /**
    * Write a <tt>RawDate</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param date   the <tt>RawDate</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeRawDate(int iProp, RawDate date)
            throws IOException;

    /**
    * Write a <tt>RawTime</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param time   the <tt>RawTime</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeRawTime(int iProp, RawTime time)
            throws IOException;

    /**
    * Write a <tt>RawDateTime</tt> property to the POF stream.
    *
    * @param iProp  the property index
    * @param dt     the <tt>RawDateTime</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeRawDateTime(int iProp, RawDateTime dt)
            throws IOException;

    /**
    * Write a <tt>RawYearMonthInterval</tt> property to the POF stream.
    *
    * @param iProp     the property index
    * @param interval  the <tt>RawYearMonthInterval</tt> property value to
    *                  write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeRawYearMonthInterval(int iProp, RawYearMonthInterval interval)
            throws IOException;

    /**
    * Write a <tt>RawTimeInterval</tt> property to the POF stream.
    *
    * @param iProp     the property index
    * @param interval  the <tt>RawTimeInterval</tt> property value to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeRawTimeInterval(int iProp, RawTimeInterval interval)
            throws IOException;

    /**
    * Write a <tt>RawDayTimeInterval</tt> property to the POF stream.
    *
    * @param iProp     the property index
    * @param interval  the <tt>RawDayTimeInterval</tt> property value to
    *                  write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeRawDayTimeInterval(int iProp, RawDayTimeInterval interval)
            throws IOException;

    /**
    * Write an <tt>Object</tt> property to the POF stream.
    * <p>
    * The given object must be an instance of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for the object must be obtainable
    * from the {@link PofContext} associated with this PofWriter.
    *
    * @param iProp  the property index
    * @param o      the <tt>Object</tt> property to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; or if the given property cannot be
    *         encoded into a POF stream
    * @throws IOException  if an I/O error occurs
    */
    public void writeObject(int iProp, Object o)
            throws IOException;

    // ----- collection support ---------------------------------------------

    /**
    * Write an <tt>Object[]</tt> property to the POF stream.
    * <p>
    * Each element of the given array must be an instance (or an array of
    * instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each element of the array must
    * be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    *
    * @param <T>    type of the elements in the array
    * @param iProp  the property index
    * @param ao     the <tt>Object[]</tt> property to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; or  if the given property cannot be
    *         encoded into a POF stream
    * @throws IOException  if an I/O error occurs
    */
    public <T> void writeObjectArray(int iProp, T[] ao)
            throws IOException;

    /**
    * Write a uniform <tt>Object[]</tt> property to the POF stream.
    * <p>
    * Each element of the given array must be an instance (or an array of
    * instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each element of the array must
    * be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    * <p>
    * Additionally, the type of each element must be equal to the specified
    * class.
    *
    * @param <T>    the type of the elements in the array
    * @param iProp  the property index
    * @param ao     the <tt>Object[]</tt> property to write
    * @param clz    the class of all elements; must not be null
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; if the given property cannot be
    *         encoded into a POF stream; or if the type of one or more
    *         elements of the array is not equal to the specified class
    * @throws IOException  if an I/O error occurs
    */
    public <T> void writeObjectArray(int iProp, T[] ao, Class<? extends T> clz)
            throws IOException;

    /**
    * Write a <tt>Collection</tt> property to the POF stream.
    * <p>
    * Each element of the given Collection must be an instance (or an array
    * of instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each element of the Collection
    * must be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    *
    * @param <T>    the type of elements in <tt>Collection</tt>
    * @param iProp  the property index
    * @param coll   the <tt>Collection</tt> property to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; or if the given property cannot be
    *         encoded into a POF stream
    * @throws IOException  if an I/O error occurs
    */
    public <T> void writeCollection(int iProp, Collection<? extends T> coll)
            throws IOException;

    /**
    * Write a uniform <tt>Collection</tt> property to the POF stream.
    * <p>
    * Each element of the given Collection must be an instance (or an array
    * of instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each element of the Collection
    * must be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    * <p>
    * Additionally, the type of each element must be equal to the specified
    * class.
    *
    * @param <T>    the type of elements in the <tt>Collection</tt>
    * @param iProp  the property index
    * @param coll   the <tt>Collection</tt> property to write
    * @param clz    the class of all elements; must not be null
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; if the given property cannot be
    *         encoded into a POF stream; or if the type of one or more
    *         elements of the Collection is not equal to the specified class
    * @throws IOException  if an I/O error occurs
    */
    public <T> void writeCollection(int iProp, Collection<? extends T> coll, Class<? extends T> clz)
            throws IOException;

    /**
    * Write a <tt>LongArray</tt> property to the POF stream.
    * <p>
    * Each element of the given LongArray must be an instance (or an array of
    * instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each element of the LongArray
    * must be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    *
    * @param <T>    the type of elements in <tt>LongArray</tt>
    * @param iProp  the property index
    * @param la     the <tt>LongArray</tt> property to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; or if the given property cannot be
    *         encoded into a POF stream
    * @throws IOException  if an I/O error occurs
    */
    public <T> void writeLongArray(int iProp, LongArray<? extends T> la)
            throws IOException;

    /**
    * Write a uniform <tt>LongArray</tt> property to the POF stream.
    * <p>
    * Each element of the given LongArray must be an instance (or an array of
    * instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each element of the LongArray
    * must be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    * <p>
    * Additionally, the type of each element must be equal to the specified
    * class.
    *
    * @param <T>    the type of elements in <tt>LongArray</tt>
    * @param iProp  the property index
    * @param la     the <tt>LongArray</tt> property to write
    * @param clz    the class of all elements; must not be null
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; if the given property cannot be
    *         encoded into a POF stream; or if the type of one or more
    *         elements of the LongArray is not equal to the specified class
    * @throws IOException  if an I/O error occurs
    */
    public <T> void writeLongArray(int iProp, LongArray<T> la, Class<T> clz)
            throws IOException;

    /**
    * Write a <tt>Map</tt> property to the POF stream.
    * <p>
    * Each key and value of the given Map must be an instance (or an array of
    * instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each key and value of the Map
    * must be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    *
    * @param <K>    the key type
    * @param <V>    the value type
    * @param iProp  the property index
    * @param map    the <tt>Map</tt> property to write
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; or  if the given property cannot be
    *         encoded into a POF stream
    * @throws IOException  if an I/O error occurs
    */
    public <K, V> void writeMap(int iProp, Map<? extends K, ? extends V> map)
            throws IOException;

    /**
    * Write a uniform key <tt>Map</tt> property to the POF stream.
    * <p>
    * Each key and value of the given Map must be an instance (or an array of
    * instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each key and value of the Map
    * must be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    * <p>
    * Additionally, the type of each key must be equal to the specified
    * class.
    *
    * @param <K>     the key type
    * @param <V>     the value type
    * @param iProp   the property index
    * @param map     the <tt>Map</tt> property to write
    * @param clzKey  the class of all keys; must not be null
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; if the given property cannot be
    *         encoded into a POF stream; or if the type of one or more keys
    *         is not equal to the specified class
    * @throws IOException  if an I/O error occurs
    */
    public <K, V> void writeMap(int iProp, Map<K, ? extends V> map, Class<? extends K> clzKey)
            throws IOException;

    /**
    * Write a uniform <tt>Map</tt> property to the POF stream.
    * <p>
    * Each key and value of the given Map must be an instance (or an array of
    * instances) of one of the following:
    * <ul>
    *   <li>boolean[]</li>
    *   <li>byte[]</li>
    *   <li>char[]</li>
    *   <li>short[]</li>
    *   <li>int[]</li>
    *   <li>long[]</li>
    *   <li>float[]</li>
    *   <li>double[]</li>
    *   <li>Boolean</li>
    *   <li>Byte</li>
    *   <li>Char</li>
    *   <li>Short</li>
    *   <li>Integer</li>
    *   <li>Long</li>
    *   <li>BigInteger</li>
    *   <li>Float</li>
    *   <li>Double</li>
    *   <li>{@link RawQuad}</li>
    *   <li>BigDecimal</li>
    *   <li>{@link Binary}</li>
    *   <li>String</li>
    *   <li>Date</li>
    *   <li>{@link RawDate}</li>
    *   <li>{@link RawTime}</li>
    *   <li>{@link RawDateTime}</li>
    *   <li>{@link RawYearMonthInterval}</li>
    *   <li>{@link RawTimeInterval}</li>
    *   <li>{@link RawDayTimeInterval}</li>
    *   <li>Collection</li>
    *   <li>{@link LongArray}</li>
    *   <li>{@link PortableObject}</li>
    * </ul>
    * <p>
    * Otherwise, a {@link PofSerializer} for each key and value of the Map
    * must be obtainable from the {@link PofContext} associated with this
    * PofWriter.
    * <p>
    * Additionally, the type of each key and value must be equal to the
    * specified key class and value class respectively.
    *
    * @param <K>       the key type
    * @param <V>       the value type
    * @param iProp     the property index
    * @param map       the <tt>Map</tt> property to write
    * @param clzKey    the class of all keys; must not be null
    * @param clzValue  the class of all values; must not be null
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream; if the given property cannot be
    *         encoded into a POF stream; or if the type of one or more keys
    *         or values of the Map is not equal to the specified key or value
    *         class
    * @throws IOException  if an I/O error occurs
    */
    public <K, V> void writeMap(int iProp, Map<K, V> map, Class<? extends K> clzKey, Class<? extends V> clzValue)
            throws IOException;


    // ----- POF user type support ------------------------------------------

    /**
    * Return the PofContext object used by this PofWriter to serialize user
    * types to a POF stream.
    *
    * @return the PofContext object that contains user type meta-data
    */
    public PofContext getPofContext();

    /**
    * Configure the PofContext object used by this PofWriter to serialize
    * user types to a POF stream.
    * <p>
    * Note: this is an advanced method that should be used with care. For
    * example, if this method is being used to switch to another PofContext
    * mid-POF stream, it is important to eventually restore the original
    * PofContext. For example:
    * <pre>
    * PofContext ctxOrig = writer.getPofContext();
    * try
    *     {
    *     // switch to another PofContext
    *     PofContext ctxNew = ...;
    *     writer.setContext(ctxNew);
    *
    *     // output POF data using the writer
    *     }
    * finally
    *     {
    *     // restore the original PofContext
    *     writer.setPofContext(ctxOrig);
    *     }
    * </pre>
    *
    * @param ctx  the new PofContext; must not be null
    */
    public void setPofContext(PofContext ctx);

    /**
    * Determine the user type that is currently being written.
    *
    * @return the user type identifier, or -1 if the PofWriter is not
    *         currently writing a user type
    */
    public int getUserTypeId();

    /**
    * Determine the version identifier of the user type that is currently
    * being written.
    *
    * @return  the integer version ID of the user type; always non-negative
    *
    * @throws IllegalStateException  if no user type is being written
    */
    public int getVersionId();

    /**
    * Set the version identifier of the user type that is currently being
    * written.
    *
    * @param nVersionId  the user type identifier; must be non-negative
    *
    * @throws IllegalArgumentException  if the given version ID is negative
    * @throws IllegalStateException  if no user type is being written
    */
    public void setVersionId(int nVersionId);

    /**
    * Obtain a PofWriter that can be used to write a set of properties into
    * a single property of the current user type. The returned PofWriter is
    * only valid from the time that it is returned until the next call is
    * made to this PofWriter.
    *
    * @param iProp  the property index
    *
    * @return a PofWriter whose contents are nested into a single property
    *         of this PofWriter
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IllegalStateException  if no user type is being written
    * @throws IOException  if an I/O error occurs
    *
    * @since Coherence 3.6
    */
    public PofWriter createNestedPofWriter(int iProp)
            throws IOException;

    /**
    * Obtain a PofWriter that can be used to write a set of properties into
    * a single property of the current user type. The returned PofWriter is
    * only valid from the time that it is returned until the next call is
    * made to this PofWriter.
    *
    * @param iProp    the property index
    * @param nTypeId  the type identifier of the nested property
    *
    * @return a PofWriter whose contents are nested into a single property
    *         of this PofWriter
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IllegalStateException  if no user type is being written
    * @throws IOException  if an I/O error occurs
    *
    * @since Coherence 12.2.1
    */
    public PofWriter createNestedPofWriter(int iProp, int nTypeId)
            throws IOException;

    /**
    * Write the remaining properties to the POF stream, terminating the
    * writing of the current user type. As part of writing out a user type,
    * this method must be called by the PofSerializer that is writing out the
    * user type, or the POF stream will be corrupted.
    * <p>
    * Calling this method terminates the current user type by writing a -1 to
    * the POF stream after the last indexed property. Subsequent calls to the
    * various <tt>writeXYZ</tt> methods of this interface will fail after
    * this method is called.
    *
    * @param binProps  a Binary object containing zero or more indexed
    *                  properties in binary POF encoded form; may be null
    *
    * @throws IllegalStateException  if no user type is being written
    * @throws IOException  if an I/O error occurs
    */
    public void writeRemainder(Binary binProps)
            throws IOException;
    }
