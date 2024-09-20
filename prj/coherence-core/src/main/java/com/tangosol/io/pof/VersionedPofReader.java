/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
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
 * A wrapper {@code PofReader} implementation that will only attempt to
 * read properties from the wrapped {@code PofReader} if the data version of
 * the wrapped reader is greater or equal to the specified implementation version.
 * Otherwise, each {@code read*} method will simply return default value for
 * the corresponding data type.
 * <p/>
 * This allows us to initialize potentially final fields in a newer version of the
 * class when deserializing POF stream created from an earlier version of the class.
 *
 * @author Aleks Seovic  2024.04.17
 * @since 24.09
 */
class VersionedPofReader implements PofReader
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@code PofReader} that will only attempt to
     * read properties from the wrapped {@code PofReader} if the data version of
     * the wrapped reader is greater or equal to the specified implementation
     * version.
     *
     * @param in            the PofReader to delegate to
     * @param nImplVersion  the maximum implementation version to read
     */
    public VersionedPofReader(PofReader in, int nImplVersion)
        {
        f_in       = in;
        f_nVersion = nImplVersion;
        f_fRead    = in.getVersionId() >= nImplVersion;
        }

    // ---- PofReader interface ---------------------------------------------

    public boolean readBoolean(int iProp) throws IOException
        {
        return f_fRead && f_in.readBoolean(iProp);
        }

    public byte readByte(int iProp) throws IOException
        {
        return f_fRead ? f_in.readByte(iProp) : 0;
        }

    public char readChar(int iProp) throws IOException
        {
        return f_fRead ? f_in.readChar(iProp) : 0;
        }

    public short readShort(int iProp) throws IOException
        {
        return f_fRead ? f_in.readShort(iProp) : 0;
        }

    public int readInt(int iProp) throws IOException
        {
        return f_fRead ? f_in.readInt(iProp) : 0;
        }

    public long readLong(int iProp) throws IOException
        {
        return f_fRead ? f_in.readLong(iProp) : 0L;
        }

    public float readFloat(int iProp) throws IOException
        {
        return f_fRead ? f_in.readFloat(iProp) : 0.0f;
        }

    public double readDouble(int iProp) throws IOException
        {
        return f_fRead ? f_in.readDouble(iProp) : 0.0d;
        }

    public boolean[] readBooleanArray(int iProp) throws IOException
        {
        return f_fRead ? f_in.readBooleanArray(iProp) : null;
        }

    public byte[] readByteArray(int iProp) throws IOException
        {
        return f_fRead ? f_in.readByteArray(iProp) : null;
        }

    public char[] readCharArray(int iProp) throws IOException
        {
        return f_fRead ? f_in.readCharArray(iProp) : null;
        }

    public short[] readShortArray(int iProp) throws IOException
        {
        return f_fRead ? f_in.readShortArray(iProp) : null;
        }

    public int[] readIntArray(int iProp) throws IOException
        {
        return f_fRead ? f_in.readIntArray(iProp) : null;
        }

    public long[] readLongArray(int iProp) throws IOException
        {
        return f_fRead ? f_in.readLongArray(iProp) : null;
        }

    public float[] readFloatArray(int iProp) throws IOException
        {
        return f_fRead ? f_in.readFloatArray(iProp) : null;
        }

    public double[] readDoubleArray(int iProp) throws IOException
        {
        return f_fRead ? f_in.readDoubleArray(iProp) : null;
        }

    public BigInteger readBigInteger(int iProp) throws IOException
        {
        return f_fRead ? f_in.readBigInteger(iProp) : null;
        }

    public RawQuad readRawQuad(int iProp) throws IOException
        {
        return f_fRead ? f_in.readRawQuad(iProp) : null;
        }

    public BigDecimal readBigDecimal(int iProp) throws IOException
        {
        return f_fRead ? f_in.readBigDecimal(iProp) : null;
        }

    public Binary readBinary(int iProp) throws IOException
        {
        return f_fRead ? f_in.readBinary(iProp) : null;
        }

    public String readString(int iProp) throws IOException
        {
        return f_fRead ? f_in.readString(iProp) : null;
        }

    public Date readDate(int iProp) throws IOException
        {
        return f_fRead ? f_in.readDate(iProp) : null;
        }

    public LocalDate readLocalDate(int iProp) throws IOException
        {
        return f_fRead ? f_in.readLocalDate(iProp) : null;
        }

    public LocalDateTime readLocalDateTime(int iProp) throws IOException
        {
        return f_fRead ? f_in.readLocalDateTime(iProp) : null;
        }

    public LocalTime readLocalTime(int iProp) throws IOException
        {
        return f_fRead ? f_in.readLocalTime(iProp) : null;
        }

    public OffsetDateTime readOffsetDateTime(int iProp) throws IOException
        {
        return f_fRead ? f_in.readOffsetDateTime(iProp) : null;
        }

    public OffsetTime readOffsetTime(int iProp) throws IOException
        {
        return f_fRead ? f_in.readOffsetTime(iProp) : null;
        }

    public ZonedDateTime readZonedDateTime(int iProp) throws IOException
        {
        return f_fRead ? f_in.readZonedDateTime(iProp) : null;
        }

    public RawDate readRawDate(int iProp) throws IOException
        {
        return f_fRead ? f_in.readRawDate(iProp) : null;
        }

    public RawTime readRawTime(int iProp) throws IOException
        {
        return f_fRead ? f_in.readRawTime(iProp) : null;
        }

    public RawDateTime readRawDateTime(int iProp) throws IOException
        {
        return f_fRead ? f_in.readRawDateTime(iProp) : null;
        }

    public RawYearMonthInterval readRawYearMonthInterval(int iProp)
            throws IOException
        {
        return f_fRead ? f_in.readRawYearMonthInterval(iProp) : null;
        }

    public RawTimeInterval readRawTimeInterval(int iProp) throws IOException
        {
        return f_fRead ? f_in.readRawTimeInterval(iProp) : null;
        }

    public RawDayTimeInterval readRawDayTimeInterval(int iProp)
            throws IOException
        {
        return f_fRead ? f_in.readRawDayTimeInterval(iProp) : null;
        }

    public <T> T readObject(int iProp) throws IOException
        {
        return f_fRead ? f_in.readObject(iProp) : null;
        }

    @Deprecated
    public Object[] readObjectArray(int iProp, Object[] ao) throws IOException
        {
        return f_fRead ? f_in.readObjectArray(iProp, ao) : null;
        }

    public <T> T[] readArray(int iProp, IntFunction<T[]> supplier)
            throws IOException
        {
        return f_fRead ? f_in.readArray(iProp, supplier) : null;
        }

    public <T> LongArray<T> readLongArray(int iProp, LongArray<T> array)
            throws IOException
        {
        return f_fRead ? f_in.readLongArray(iProp, array) : null;
        }

    public <T, C extends Collection<T>> C readCollection(int iProp, C coll)
            throws IOException
        {
        return f_fRead ? f_in.readCollection(iProp, coll) : null;
        }

    public <K, V, M extends Map<K, V>> M readMap(int iProp, M map)
            throws IOException
        {
        return f_fRead ? f_in.readMap(iProp, map) : null;
        }

    public PofContext getPofContext()
        {
        return f_in.getPofContext();
        }

    public void setPofContext(PofContext ctx)
        {
        f_in.setPofContext(ctx);
        }

    public int getUserTypeId()
        {
        return f_in.getUserTypeId();
        }

    public int getVersionId()
        {
        return f_in.getVersionId();
        }

    public void registerIdentity(Object o)
        {
        f_in.registerIdentity(o);
        }

    public PofReader createNestedPofReader(int iProp) throws IOException
        {
        return new VersionedPofReader(f_in.createNestedPofReader(iProp), f_nVersion);
        }

    public Binary readRemainder() throws IOException
        {
        return f_in.readRemainder();
        }

    private final PofReader f_in;
    private final int f_nVersion;
    private final boolean f_fRead;
    }
