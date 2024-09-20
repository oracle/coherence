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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import java.util.function.IntFunction;


/**
* The PofReader interface provides the capability of reading a set of
* non-primitive Java types ("user types") from a POF stream as an ordered
* sequence of indexed properties.
* <p>
* See {@link PofWriter} for a complete description of the POF user type
* serialization format.
*
* @author cp/jh  2006.07.13
*
* @see PofContext
* @see PofWriter
*
* @since Coherence 3.2
*/
public interface PofReader
    {
    // ----- versioning helper ----------------------------------------------

    /**
     * Return a {@code PofReader} that will only attempt to read properties
     * from the wrapped {@code PofReader} if the data version of this reader is
     * greater or equal to the specified implementation version.
     *
     * @param nImplVersion  the maximum implementation version to read
     *
     * @return a version-aware {@code PofReader}
     */
    default PofReader version(int nImplVersion)
        {
        return new VersionedPofReader(this, nImplVersion);
        }

    // ----- primitive value support ----------------------------------------

    /**
    * Read a <tt>boolean</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>boolean</tt> property value, or zero if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public boolean readBoolean(int iProp)
            throws IOException;

    /**
    * Read a <tt>byte</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>byte</tt> property value, or zero if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public byte readByte(int iProp)
            throws IOException;

    /**
    * Read a <tt>char</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>char</tt> property value, or zero if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public char readChar(int iProp)
            throws IOException;

    /**
    * Read a <tt>short</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>short</tt> property value, or zero if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public short readShort(int iProp)
            throws IOException;

    /**
    * Read a <tt>int</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>int</tt> property value, or zero if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public int readInt(int iProp)
            throws IOException;

    /**
    * Read a <tt>long</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>long</tt> property value, or zero if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public long readLong(int iProp)
            throws IOException;

    /**
    * Read a <tt>float</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>float</tt> property value, or zero if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public float readFloat(int iProp)
            throws IOException;

    /**
    * Read a <tt>double</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>double</tt> property value, or zero if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public double readDouble(int iProp)
            throws IOException;


    // ----- primitive array support ----------------------------------------

    /**
    * Read a <tt>boolean[]</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>boolean[]</tt> property value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public boolean[] readBooleanArray(int iProp)
            throws IOException;

    /**
    * Read a <tt>byte[]</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>byte[]</tt> property value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public byte[] readByteArray(int iProp)
            throws IOException;

    /**
    * Read a <tt>char[]</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>char[]</tt> property value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public char[] readCharArray(int iProp)
            throws IOException;

    /**
    * Read a <tt>short[]</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>short[]</tt> property value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public short[] readShortArray(int iProp)
            throws IOException;

    /**
    * Read a <tt>int[]</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>int[]</tt> property value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public int[] readIntArray(int iProp)
            throws IOException;

    /**
    * Read a <tt>long[]</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>long[]</tt> property value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public long[] readLongArray(int iProp)
            throws IOException;

    /**
    * Read a <tt>float[]</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>float[]</tt> property value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public float[] readFloatArray(int iProp)
            throws IOException;

    /**
    * Read a <tt>double[]</tt> property from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>double[]</tt> property value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public double[] readDoubleArray(int iProp)
            throws IOException;


    // ----- object value support -------------------------------------------

    /**
    * Read a <tt>BigInteger</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>BigInteger</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public BigInteger readBigInteger(int iProp)
            throws IOException;

    /**
    * Read a <tt>RawQuad</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>RawQuad</tt> property value, or null if no value
    *         was available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public RawQuad readRawQuad(int iProp)
            throws IOException;

    /**
    * Read a <tt>BigDecimal</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>BigDecimal</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public BigDecimal readBigDecimal(int iProp)
            throws IOException;

    /**
    * Read a <tt>Binary</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>Binary</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public Binary readBinary(int iProp)
            throws IOException;

    /**
    * Read a <tt>String</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>String</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public String readString(int iProp)
            throws IOException;

    /**
    * Read a <tt>java.util.Date</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>Date</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public Date readDate(int iProp)
            throws IOException;

    /**
    * Read a <tt>java.time.LocalDate</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>LocalDate</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public LocalDate readLocalDate(int iProp)
            throws IOException;

    /**
    * Read a <tt>java.time.LocalDateTime</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>LocalDateTime</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public LocalDateTime readLocalDateTime(int iProp)
            throws IOException;

    /**
    * Read a <tt>java.time.LocalTime</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>LocalTime</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public LocalTime readLocalTime(int iProp)
            throws IOException;

    /**
    * Read a <tt>java.time.OffsetDateTime</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>OffsetDateTime</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public OffsetDateTime readOffsetDateTime(int iProp)
            throws IOException;

    /**
    * Read a <tt>java.time.OffsetTime</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>OffsetTime</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public OffsetTime readOffsetTime(int iProp)
            throws IOException;

    /**
    * Read a <tt>java.time.ZonedDateTime</tt> from the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>ZonedDateTime</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public default ZonedDateTime readZonedDateTime(int iProp)
            throws IOException
        {
        return readOffsetDateTime(iProp).toZonedDateTime();
        }

    /**
    * Read a <tt>RawDate</tt> from the POF stream. The {@link RawDate} class
    * contains the raw date information that was carried in the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>RawDate</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public RawDate readRawDate(int iProp)
            throws IOException;

    /**
    * Read a <tt>RawTime</tt> from the POF stream. The {@link RawTime} class
    * contains the raw time information that was carried in the POF stream,
    * including raw timezone information.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>RawTime</tt> property value, or null if no value was
    *         available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public RawTime  readRawTime(int iProp)
            throws IOException;

    /**
    * Read a <tt>RawDateTime</tt> from the POF stream.  The {@link
    * RawDateTime} class contains the raw date and time information that was
    * carried in the POF stream, including raw timezone information.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>RawDateTime</tt> property value, or null if no value
    *         was available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public RawDateTime readRawDateTime(int iProp)
            throws IOException;

    /**
    * Read a <tt>RawYearMonthInterval</tt> from the POF stream. The {@link
    * RawYearMonthInterval} class contains the raw year-month interval
    * information that was carried in the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>RawYearMonthInterval</tt> property value, or null if no
    *         value was available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public RawYearMonthInterval readRawYearMonthInterval(int iProp)
            throws IOException;

    /**
    * Read a <tt>RawTimeInterval</tt> from the POF stream. The {@link
    * RawTimeInterval} class contains the raw time interval information that
    * was carried in the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>RawTimeInterval</tt> property value, or null if no
    *         value was available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public RawTimeInterval readRawTimeInterval(int iProp)
            throws IOException;

    /**
    * Read a <tt>RawDayTimeInterval</tt> from the POF stream. The {@link
    * RawDayTimeInterval} class contains the raw year-month interval
    * information that was carried in the POF stream.
    *
    * @param iProp  the property index to read
    *
    * @return the <tt>RawDayTimeInterval</tt> property value, or null if no
    *         value was available in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public RawDayTimeInterval readRawDayTimeInterval(int iProp)
            throws IOException;

    /**
    * Read a property of any type, including a user type, from the POF
    * stream.
    *
    * @param <T>    the object type
    * @param iProp  the property index to read
    *
    * @return the <tt>Object</tt> value; may be null
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public <T> T readObject(int iProp)
            throws IOException;

    // ----- collections support --------------------------------------------

    /**
    * Read an array of object values.
    *
    * @param iProp  the property index to read
    * @param ao     the optional array to use to store the values, or to use
    *               as a typed template for creating an array to store the
    *               values, following the documentation for
    *               {@link java.util.Collection#toArray(Object[]) Collection.toArray()}
    *
    * @return an array of object values, or null if no array is passed and
    *         there is no array data in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    @Deprecated
    public Object[] readObjectArray(int iProp, Object[] ao)
            throws IOException;

    /**
    * Read an array of values.
    *
    * @param <T>       the value type
    * @param iProp     the property index to read
    * @param supplier  the supplier to use to create the array, typically an
     *                 array constructor reference (i.e. {@code String[]::new})
    *
    * @return an array of object values, or an empty array if there
    *         is no array data in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public <T> T[] readArray(int iProp, IntFunction<T[]> supplier)
            throws IOException;

    /**
    * Read a LongArray of object values.
    *
    * @param <T>    the object value type
    * @param iProp  the property index to read
    * @param array  the optional LongArray object to use to store the values
    *
    * @return a LongArray of object values, or null if no LongArray is passed
    *         and there is no array data in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public <T> LongArray<T> readLongArray(int iProp, LongArray<T> array)
            throws IOException;

    /**
    * Read a <tt>Collection</tt> of object values from the POF stream.
    *
    * @param <T>    the object value type
    * @param <C>    the collection type
    * @param iProp  the property index to read
    * @param coll   the optional Collection to use to store the values
    *
    * @return a Collection of object values, or null if no Collection is
    *         passed and there is no collection data in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public <T, C extends Collection<T>> C readCollection(int iProp, C coll)
            throws IOException;

    /**
    * Read a <tt>Map</tt> of key/value pairs from the POF stream.
    *
    * @param <K>    the key type
    * @param <V>    the value type
    * @param <M>    the map type
    * @param iProp  the property index to read
    * @param map    the optional Map to initialize
    *
    * @return a Map of key/value pairs object values, or null if no Map is
    *         passed and there is no key/value data in the POF stream
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property
    * @throws IOException  if an I/O error occurs
    */
    public <K, V, M extends Map<K, V>> M readMap(int iProp, M map)
            throws IOException;

    // ----- POF user type support ------------------------------------------

    /**
    * Return the PofContext object used by this PofReader to deserialize user
    * types from a POF stream.
    *
    * @return the PofContext object that contains user type meta-data
    */
    public PofContext getPofContext();

    /**
    * Configure the PofContext object used by this PofReader to deserialize
    * user types from a POF stream.
    * <p>
    * Note: this is an advanced method that should be used with care. For
    * example, if this method is being used to switch to another PofContext
    * mid-POF stream, it is important to eventually restore the original
    * PofContext. For example:
    * <pre>
    * PofContext ctxOrig = reader.getPofContext();
    * try
    *     {
    *     // switch to another PofContext
    *     PofContext ctxNew = ...;
    *     reader.setContext(ctxNew);
    *
    *     // read POF data using the reader
    *     }
    * finally
    *     {
    *     // restore the original PofContext
    *     reader.setPofContext(ctxOrig);
    *     }
    * </pre>
    *
    * @param ctx  the new PofContext; must not be null
    */
    public void setPofContext(PofContext ctx);

    /**
    * Determine the user type that is currently being parsed.
    *
    * @return the user type information, or -1 if the PofReader is not
    *         currently parsing a user type
    */
    public int getUserTypeId();

    /**
    * Determine the version identifier of the user type that is currently
    * being parsed.
    *
    * @return  the integer version ID read from the POF stream; always
    *          non-negative
    *
    * @throws IllegalStateException  if no user type is being parsed
    */
    public int getVersionId();

    /**
    * Register an identity for a newly created user type instance.
    * <p>
    * If identity/reference types are enabled, an identity is used to
    * uniquely identify a user type instance within a POF stream. The
    * identity immediately proceeds the instance value in the POF stream and
    * can be used later in the stream to reference the instance.
    * <p>
    * PofSerializer implementations must call this method with the user
    * type instance instantiated during deserialization prior to reading any
    * properties of the instance which are user type instances themselves.
    *
    * @param o  the object to register the identity for
    *
    * @see PofSerializer#deserialize(PofReader)
    *
    * @since Coherence 3.7.1
    */
    public void registerIdentity(Object o);

    /**
    * Obtain a PofReader that can be used to read a set of properties from a
    * single property of the current user type. The returned PofReader is
    * only valid from the time that it is returned until the next call is
    * made to this PofReader.
    *
    * @param iProp  the property index to read from
    *
    * @return a PofReader that reads its contents from  a single property of
    *         this PofReader
    *
    * @throws IllegalStateException  if the POF stream has already
    *         advanced past the desired property or if no user
    *         type is being parsed.
    * @throws IOException  if an I/O error occurs
    *
    * @since Coherence 3.6
    */
    public PofReader createNestedPofReader(int iProp)
            throws IOException;

    /**
    * Read all remaining indexed properties of the current user type from the
    * POF stream. As part of reading in a user type, this method must be
    * called by the PofSerializer that is reading the user type, or the read
    * position within the POF stream will be corrupted.
    * <p>
    * Subsequent calls to the various <tt>readXYZ</tt> methods of this
    * interface will fail after this method is called.
    *
    * @return a Binary object containing zero or more indexed properties in
    *         binary POF encoded form
    *
    * @throws IllegalStateException  if no user type is being parsed
    * @throws IOException  if an I/O error occurs
    */
    public Binary readRemainder()
            throws IOException;
    }
