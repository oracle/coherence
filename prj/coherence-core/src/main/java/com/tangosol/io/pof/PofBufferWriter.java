/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.Evolvable;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.WrapperException;

import java.io.EOFException;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.nio.ByteBuffer;

import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
* {@link PofWriter} implementation that writes POF-encoded data to a
* {@link com.tangosol.io.WriteBuffer.BufferOutput BufferOutput}.
*
* @author jh  2006.07.11
*
* @since Coherence 3.2
*/
public class PofBufferWriter
        extends PofHelper
        implements PofWriter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new PofBufferWriter that will write a POF stream to the
    * passed BufferOutput object.
    *
    * @param out  the BufferOutput object to write to; must not be null
    * @param ctx  the PofContext used by the new PofBufferWriter to
    *             serialize user types; must not be null
    */
    public PofBufferWriter(WriteBuffer.BufferOutput out, PofContext ctx)
        {
        azzert(out != null, "BufferOutput cannot be null");
        azzert(ctx != null, "PofContext cannot be null");

        m_out     = out;
        m_ctx     = ctx;
        m_handler = new WritingPofHandler(out);
        }

    /**
    * Construct a new PofBufferWriter that will write a POF stream using
    * the passed WritingPofHandler.
    *
    * @param handler  the WritingPofHandler used for writing; must not be null
    * @param ctx      the PofContext used by the new PofBufferWriter to
    *                 serialize user types; must not be null
    */
    public PofBufferWriter(WritingPofHandler handler, PofContext ctx)
        {
        azzert(handler != null, "WritingPofHandler cannot be null");
        azzert(ctx != null,     "PofContext cannot be null");

        m_out     = handler.getBufferOutput();
        m_ctx     = ctx;
        m_handler = handler;
        }


    // ----- PofWriter implementation ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void writeBoolean(int iProp, boolean f)
            throws IOException
        {
        writeBoolean(iProp, f, false);
        }

    /**
    * Write a <tt>boolean</tt> property to the POF stream.
    *
    * @param iProp           the property index
    * @param f               the <tt>boolean</tt> property value to write
    * @param fReferenceable  true if the property value is a referenceable
    *                        type
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void writeBoolean(int iProp, boolean f, boolean fReferenceable)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (fReferenceable)
                {
                handler.registerIdentity(-1);
                }
            handler.onBoolean(iProp, f);
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeByte(int iProp, byte b)
            throws IOException
        {
        writeByte(iProp, b, false);
        }

    /**
    * Write a <tt>byte</tt> property to the POF stream.
    *
    * @param iProp           the property index
    * @param b               the <tt>byte</tt> property value to write
    * @param fReferenceable  true if the property value is a referenceable
    *                        type
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void writeByte(int iProp, byte b, boolean fReferenceable)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (fReferenceable)
                {
                handler.registerIdentity(-1);
                }
            handler.onOctet(iProp, b);
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeChar(int iProp, char ch)
            throws IOException
        {
        writeChar(iProp, ch, false);
        }

    /**
    * Write a <tt>char</tt> property to the POF stream.
    *
    * @param iProp           the property index
    * @param ch              the <tt>char</tt> property value to write
    * @param fReferenceable  true if the property value is a referenceable
    *                        type
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void writeChar(int iProp, char ch, boolean fReferenceable)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (fReferenceable)
                {
                handler.registerIdentity(-1);
                }
            handler.onChar(iProp, ch);
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeShort(int iProp, short n)
            throws IOException
        {
        writeShort(iProp, n, false);
        }

    /**
    * Write a <tt>short</tt> property to the POF stream.
    *
    * @param iProp           the property index
    * @param n               the <tt>short</tt> property value to write
    * @param fReferenceable  true if the property value is a referenceable
    *                        type
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void writeShort(int iProp, short n, boolean fReferenceable)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (fReferenceable)
                {
                handler.registerIdentity(-1);
                }
            handler.onInt16(iProp, n);
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeInt(int iProp, int n)
            throws IOException
        {
        writeInt(iProp, n, false);
        }

    /**
    * Write a <tt>int</tt> property to the POF stream.
    *
    * @param iProp           the property index
    * @param n               the <tt>int</tt> property value to write
    * @param fReferenceable  true if the property value is a referenceable
    *                        type
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void writeInt(int iProp, int n, boolean fReferenceable)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (fReferenceable)
                {
                handler.registerIdentity(-1);
                }
            handler.onInt32(iProp, n);
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeLong(int iProp, long n)
            throws IOException
        {
        writeLong(iProp, n, false);
        }

    /**
    * Write a <tt>long</tt> property to the POF stream.
    *
    * @param iProp           the property index
    * @param n               the <tt>long</tt> property value to write
    * @param fReferenceable  true if the property value is a referenceable
    *                        type
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void writeLong(int iProp, long n, boolean fReferenceable)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (fReferenceable)
                {
                handler.registerIdentity(-1);
                }
            handler.onInt64(iProp, n);
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeFloat(int iProp, float fl)
            throws IOException
        {
        writeFloat(iProp, fl, false);
        }

    /**
    * Write a <tt>float</tt> property to the POF stream.
    *
    * @param iProp           the property index
    * @param fl              the <tt>float</tt> property value to write
    * @param fReferenceable  true if the property value is a referenceable
    *                        type
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void writeFloat(int iProp, float fl, boolean fReferenceable)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (fReferenceable)
                {
                handler.registerIdentity(-1);
                }
            handler.onFloat32(iProp, fl);
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeDouble(int iProp, double dfl)
            throws IOException
        {
        writeDouble(iProp, dfl, false);
        }

    /**
    * Write a <tt>double</tt> property to the POF stream.
    *
    * @param iProp           the property index
    * @param dfl             the <tt>double</tt> property value to write
    * @param fReferenceable  true if the property value is a referenceable
    *                        type
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void writeDouble(int iProp, double dfl, boolean fReferenceable)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (fReferenceable)
                {
                handler.registerIdentity(-1);
                }
            handler.onFloat64(iProp, dfl);
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeBooleanArray(int iProp, boolean[] af)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (af == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = af.length;

                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cElements, T_BOOLEAN);
                for (int i = 0; i < cElements; ++i)
                    {
                    handler.onBoolean(i, af[i]);
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeByteArray(int iProp, byte[] ab, int of, int cb)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (ab == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cb, T_OCTET);

                getBufferOutput().write(ab, of, cb);

                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeCharArray(int iProp, char[] ach, boolean fRaw)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (ach == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = ach.length;

                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cElements, fRaw ? T_OCTET : T_CHAR);

                if (fRaw)
                    {
                    int        cb = cElements * 2;
                    ByteBuffer bb = getBufferOutput().getByteBuffer(cb);
                    bb.asCharBuffer().put(ach);
                    }
                else
                    {
                    for (int i = 0; i < cElements; ++i)
                        {
                        handler.onChar(i, ach[i]);
                        }
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeShortArray(int iProp, short[] an, boolean fRaw)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (an == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = an.length;

                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cElements, fRaw ? T_OCTET : T_INT16);

                if (fRaw)
                    {
                    int        cb = cElements * 2;
                    ByteBuffer bb = getBufferOutput().getByteBuffer(cb);
                    bb.asShortBuffer().put(an);
                    }
                else
                    {
                    for (int i = 0; i < cElements; ++i)
                        {
                        handler.onInt16(i, an[i]);
                        }
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeIntArray(int iProp, int[] an, boolean fRaw)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (an == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = an.length;

                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cElements, fRaw ? T_OCTET : T_INT32);

                if (fRaw)
                    {
                    int        cb = cElements * 4;
                    ByteBuffer bb = getBufferOutput().getByteBuffer(cb);
                    bb.asIntBuffer().put(an);
                    }
                else
                    {
                    for (int i = 0; i < cElements; ++i)
                        {
                        handler.onInt32(i, an[i]);
                        }
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeLongArray(int iProp, long[] an, boolean fRaw)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (an == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = an.length;

                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cElements, fRaw ? T_OCTET : T_INT64);

                if (fRaw)
                    {
                    int        cb = cElements * 8;
                    ByteBuffer bb = getBufferOutput().getByteBuffer(cb);
                    bb.asLongBuffer().put(an);
                    }
                else
                    {
                    for (int i = 0; i < cElements; ++i)
                        {
                        handler.onInt64(i, an[i]);
                        }
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeFloatArray(int iProp, float[] afl, boolean fRaw)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (afl == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = afl.length;

                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cElements, fRaw ? T_OCTET : T_FLOAT32);

                if (fRaw)
                    {
                    int        cb = cElements * 4;
                    ByteBuffer bb = getBufferOutput().getByteBuffer(cb);
                    bb.asFloatBuffer().put(afl);
                    }
                else
                    {
                    for (int i = 0; i < cElements; ++i)
                        {
                        handler.onFloat32(i, afl[i]);
                        }
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeDoubleArray(int iProp, double[] adfl, boolean fRaw)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (adfl == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = adfl.length;

                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cElements, fRaw ? T_OCTET : T_FLOAT64);

                if (fRaw)
                    {
                    int        cb = cElements * 8;
                    ByteBuffer bb = getBufferOutput().getByteBuffer(cb);
                    bb.asDoubleBuffer().put(adfl);
                    }
                else
                    {
                    for (int i = 0; i < cElements; ++i)
                        {
                        handler.onFloat64(i, adfl[i]);
                        }
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeBigInteger(int iProp, BigInteger n)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (n == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onInt128(iProp, n);
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeRawQuad(int iProp, RawQuad qfl)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (qfl == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onFloat128(iProp, qfl);
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeBigDecimal(int iProp, BigDecimal dec)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dec == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                switch (calcDecimalSize(dec))
                    {
                    case 4:
                        handler.onDecimal32(iProp, dec);
                        break;
                    case 8:
                        handler.onDecimal64(iProp, dec);
                        break;
                    case 16:
                        handler.onDecimal128(iProp, dec);
                        break;
                    default:
                        throw azzert();
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeBinary(int iProp, Binary bin)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (bin == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onOctetString(iProp, bin);
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeString(int iProp, String s)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (s == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onCharString(iProp, s);
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeDate(int iProp, Date dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onDate(iProp,
                        dt.getYear() + 1900,
                        dt.getMonth() + 1,
                        dt.getDate());
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    @Override
    public void writeDate(int iProp, LocalDate dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onDate(iProp,
                        dt.getYear(),
                        dt.getMonthValue(),
                        dt.getDayOfMonth());
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeDateTime(int iProp, Date dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                dt = fixNanos(dt);
                handler.onDateTime(iProp,
                        dt.getYear() + 1900,
                        dt.getMonth() + 1,
                        dt.getDate(),
                        dt.getHours(),
                        dt.getMinutes(),
                        dt.getSeconds(),
                        getNanos(dt),
                        false); // UTC?
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    @Override
    public void writeDateTime(int iProp, LocalDateTime dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                handler.onDateTime(iProp,
                        dt.getYear(),
                        dt.getMonthValue(),
                        dt.getDayOfMonth(),
                        dt.getHour(),
                        dt.getMinute(),
                        dt.getSecond(),
                        dt.getNano(),
                        false); // UTC?
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeDateTime(int iProp, Timestamp dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onDateTime(iProp,
                        dt.getYear() + 1900,
                        dt.getMonth() + 1,
                        dt.getDate(),
                        dt.getHours(),
                        dt.getMinutes(),
                        dt.getSeconds(),
                        dt.getNanos(),
                        false); // UTC?
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeDateTimeWithZone(int iProp, Date dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                dt = fixNanos(dt);
                int cMinuteOff = -dt.getTimezoneOffset();
                if (cMinuteOff == 0)
                    {
                    handler.onDateTime(iProp,
                            dt.getYear() + 1900,
                            dt.getMonth() + 1,
                            dt.getDate(),
                            dt.getHours(),
                            dt.getMinutes(),
                            dt.getSeconds(),
                            getNanos(dt),
                            true); // UTC?
                    }
                else
                    {
                    int cHourOff = cMinuteOff / 60;
                    cMinuteOff   = Math.abs(cMinuteOff);
                    handler.onDateTime(iProp,
                            dt.getYear() + 1900,
                            dt.getMonth() + 1,
                            dt.getDate(),
                            dt.getHours(),
                            dt.getMinutes(),
                            dt.getSeconds(),
                            getNanos(dt),
                            cHourOff,
                            cHourOff == 0 ? cMinuteOff : cMinuteOff % 60);
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    @Override
    public void writeDateTimeWithZone(int iProp, OffsetDateTime dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                ZoneOffset of = dt.getOffset();
                if (ZoneOffset.UTC.equals(of))
                    {
                    handler.onDateTime(iProp,
                           dt.getYear(),
                           dt.getMonthValue(),
                           dt.getDayOfMonth(),
                           dt.getHour(),
                           dt.getMinute(),
                           dt.getSecond(),
                           dt.getNano(),
                           true); // UTC?
                    }
                else
                    {
                    int cMinuteOff = of.getTotalSeconds() / 60;
                    int cHourOff   = cMinuteOff / 60;
                    cMinuteOff     = Math.abs(cMinuteOff);

                    handler.onDateTime(iProp,
                           dt.getYear(),
                           dt.getMonthValue(),
                           dt.getDayOfMonth(),
                           dt.getHour(),
                           dt.getMinute(),
                           dt.getSecond(),
                           dt.getNano(),
                           cHourOff,
                           cHourOff == 0 ? cMinuteOff : cMinuteOff % 60);
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeDateTimeWithZone(int iProp, Timestamp dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                int cMinuteOff = -dt.getTimezoneOffset();
                if (cMinuteOff == 0)
                    {
                    handler.onDateTime(iProp,
                            dt.getYear() + 1900,
                            dt.getMonth() + 1,
                            dt.getDate(),
                            dt.getHours(),
                            dt.getMinutes(),
                            dt.getSeconds(),
                            dt.getNanos(),
                            true); // UTC?
                    }
                else
                    {
                    int cHourOff = cMinuteOff / 60;
                    cMinuteOff   = Math.abs(cMinuteOff);
                    handler.onDateTime(iProp,
                            dt.getYear() + 1900,
                            dt.getMonth() + 1,
                            dt.getDate(),
                            dt.getHours(),
                            dt.getMinutes(),
                            dt.getSeconds(),
                            dt.getNanos(),
                            cHourOff,
                            cHourOff == 0 ? cMinuteOff : cMinuteOff % 60);
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTime(int iProp, Date dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                dt = fixNanos(dt);
                handler.onTime(iProp,
                        dt.getHours(),
                        dt.getMinutes(),
                        dt.getSeconds(),
                        getNanos(dt),
                        false); // UTC?
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    @Override
    public void writeTime(int iProp, LocalTime dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                handler.onTime(iProp,
                        dt.getHour(),
                        dt.getMinute(),
                        dt.getSecond(),
                        dt.getNano(),
                        false); // UTC?
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTime(int iProp, Timestamp dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onTime(iProp,
                        dt.getHours(),
                        dt.getMinutes(),
                        dt.getSeconds(),
                        dt.getNanos(),
                        false); // UTC?
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTimeWithZone(int iProp, Date dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                dt = fixNanos(dt);
                int cMinuteOff = -dt.getTimezoneOffset();
                if (cMinuteOff == 0)
                    {
                    handler.onTime(iProp,
                            dt.getHours(),
                            dt.getMinutes(),
                            dt.getSeconds(),
                            getNanos(dt),
                            true); // UTC?
                    }
                else
                    {
                    int cHourOff = cMinuteOff / 60;
                    cMinuteOff   = Math.abs(cMinuteOff);
                    handler.onTime(iProp,
                            dt.getHours(),
                            dt.getMinutes(),
                            dt.getSeconds(),
                            getNanos(dt),
                            cHourOff,
                            cHourOff == 0 ? cMinuteOff : cMinuteOff % 60);
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    @Override
    public void writeTimeWithZone(int iProp, OffsetTime dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                ZoneOffset of = dt.getOffset();
                if (ZoneOffset.UTC.equals(of))
                    {
                    handler.onTime(iProp,
                           dt.getHour(),
                           dt.getMinute(),
                           dt.getSecond(),
                           dt.getNano(),
                           true); // UTC?
                    }
                else
                    {
                    int cMinuteOff = of.getTotalSeconds() / 60;
                    int cHourOff   = cMinuteOff / 60;
                    cMinuteOff     = Math.abs(cMinuteOff);

                    handler.onTime(iProp,
                           dt.getHour(),
                           dt.getMinute(),
                           dt.getSecond(),
                           dt.getNano(),
                           cHourOff,
                           cHourOff == 0 ? cMinuteOff : cMinuteOff % 60);
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTimeWithZone(int iProp, Timestamp dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                int cMinuteOff = -dt.getTimezoneOffset();
                if (cMinuteOff == 0)
                    {
                    handler.onTime(iProp,
                            dt.getHours(),
                            dt.getMinutes(),
                            dt.getSeconds(),
                            dt.getNanos(),
                            true); // UTC?
                    }
                else
                    {
                    int cHourOff = cMinuteOff / 60;
                    cMinuteOff   = Math.abs(cMinuteOff);
                    handler.onTime(iProp,
                            dt.getHours(),
                            dt.getMinutes(),
                            dt.getSeconds(),
                            dt.getNanos(),
                            cHourOff,
                            cHourOff == 0 ? cMinuteOff : cMinuteOff % 60);
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeRawDate(int iProp, RawDate date)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (date == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onDate(iProp,
                        date.getYear(),
                        date.getMonth(),
                        date.getDay());
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeRawTime(int iProp, RawTime time)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (time == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                if (time.hasTimezone())
                    {
                    if (time.isUTC())
                        {
                        handler.onTime(iProp,
                                time.getHour(),
                                time.getMinute(),
                                time.getSecond(),
                                time.getNano(),
                                true);
                        }
                    else
                        {
                        handler.onTime(iProp,
                                time.getHour(),
                                time.getMinute(),
                                time.getSecond(),
                                time.getNano(),
                                time.getHourOffset(),
                                time.getMinuteOffset());
                        }
                    }
                else
                    {
                    handler.onTime(iProp,
                            time.getHour(),
                            time.getMinute(),
                            time.getSecond(),
                            time.getNano(),
                            false);
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeRawDateTime(int iProp, RawDateTime dt)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (dt == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);

                RawDate date = dt.getRawDate();
                RawTime time = dt.getRawTime();

                if (time.hasTimezone())
                    {
                    if (time.isUTC())
                        {
                        handler.onDateTime(iProp,
                                date.getYear(),
                                date.getMonth(),
                                date.getDay(),
                                time.getHour(),
                                time.getMinute(),
                                time.getSecond(),
                                time.getNano(),
                                true);
                        }
                    else
                        {
                        handler.onDateTime(iProp,
                                date.getYear(),
                                date.getMonth(),
                                date.getDay(),
                                time.getHour(),
                                time.getMinute(),
                                time.getSecond(),
                                time.getNano(),
                                time.getHourOffset(),
                                time.getMinuteOffset());
                        }
                    }
                else
                    {
                    handler.onDateTime(iProp,
                            date.getYear(),
                            date.getMonth(),
                            date.getDay(),
                            time.getHour(),
                            time.getMinute(),
                            time.getSecond(),
                            time.getNano(),
                            false);
                    }
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeRawYearMonthInterval(int iProp, RawYearMonthInterval interval)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (interval == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onYearMonthInterval(iProp,
                        interval.getYears(),
                        interval.getMonths());
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeRawTimeInterval(int iProp, RawTimeInterval interval)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (interval == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onTimeInterval(iProp,
                        interval.getHours(),
                        interval.getMinutes(),
                        interval.getSeconds(),
                        interval.getNanos());
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeRawDayTimeInterval(int iProp, RawDayTimeInterval interval)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (interval == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                handler.registerIdentity(-1);
                handler.onDayTimeInterval(iProp,
                        interval.getDays(),
                        interval.getHours(),
                        interval.getMinutes(),
                        interval.getSeconds(),
                        interval.getNanos());
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeObject(int iProp, Object o)
            throws IOException
        {
        // note that in each and every case below, the handler is notified
        // (via a call to registerIdentity) that the subsequent value is of
        // an identifiable reference type
        switch (getJavaTypeId(o, getPofContext()))
            {
            case J_NULL:
                // all null values (regardless of the caller-perceived type
                // of "o") are handled here
                beginProperty(iProp);
                try
                    {
                    WritingPofHandler handler = getPofHandler();
                    handler.registerIdentity(-1);
                    handler.onNullReference(iProp);
                    }
                catch (Exception e)
                    {
                    onException(e);
                    }
                endProperty(iProp);
                break;

            case J_BOOLEAN:
                writeBoolean(iProp, ((Boolean) o).booleanValue(), true);
                break;

            case J_BYTE:
                writeByte(iProp, ((Byte) o).byteValue(), true);
                break;

            case J_CHARACTER:
                writeChar(iProp, ((Character) o).charValue(), true);
                break;

            case J_SHORT:
                writeShort(iProp, ((Short) o).shortValue(), true);
                break;

            case J_INTEGER:
                writeInt(iProp, ((Integer) o).intValue(), true);
                break;

            case J_LONG:
                writeLong(iProp, ((Long) o).longValue(), true);
                break;

            case J_BIG_INTEGER:
                writeBigInteger(iProp, (BigInteger) o);
                break;

            case J_FLOAT:
                writeFloat(iProp, ((Float) o).floatValue(), true);
                break;

            case J_DOUBLE:
                writeDouble(iProp, ((Double) o).doubleValue(), true);
                break;

            case J_QUAD:
                writeRawQuad(iProp, (RawQuad) o);
                break;

            case J_BIG_DECIMAL:
                writeBigDecimal(iProp, (BigDecimal) o);
                break;

            case J_BINARY:
                writeBinary(iProp, ((ReadBuffer) o).toBinary());
                break;

            case J_STRING:
                writeString(iProp, (String) o);
                break;

            case J_DATE:
                writeDate(iProp, (Date) o);
                break;

            case J_LOCAL_DATE:
                writeDate(iProp, (LocalDate) o);
                break;

            case J_TIME:
                writeTimeWithZone(iProp, (Date) o);
                break;

            case J_LOCAL_TIME:
                writeTime(iProp, (LocalTime) o);
                break;

            case J_OFFSET_TIME:
                writeTimeWithZone(iProp, (OffsetTime) o);
                break;

            case J_DATETIME:
                writeDateTimeWithZone(iProp, (Date) o);
                break;

            case J_LOCAL_DATETIME:
                writeDateTime(iProp, (LocalDateTime) o);
                break;

            case J_OFFSET_DATETIME:
                writeDateTimeWithZone(iProp, (OffsetDateTime) o);
                break;

            case J_ZONED_DATETIME:
                writeDateTimeWithZone(iProp, (ZonedDateTime) o);
                break;

            case J_TIMESTAMP:
                writeDateTimeWithZone(iProp, (Timestamp) o);
                break;

            case J_RAW_DATE:
                writeRawDate(iProp, (RawDate) o);
                break;

            case J_RAW_TIME:
                writeRawTime(iProp, (RawTime) o);
                break;

            case J_RAW_DATETIME:
                writeRawDateTime(iProp, (RawDateTime) o);
                break;

            case J_RAW_YEAR_MONTH_INTERVAL:
                writeRawYearMonthInterval(iProp, (RawYearMonthInterval) o);
                break;

            case J_RAW_TIME_INTERVAL:
                writeRawTimeInterval(iProp, (RawTimeInterval) o);
                break;

            case J_RAW_DAY_TIME_INTERVAL:
                writeRawDayTimeInterval(iProp, (RawDayTimeInterval) o);
                break;

            case J_BOOLEAN_ARRAY:
                writeBooleanArray(iProp, (boolean[]) o);
                break;

            case J_BYTE_ARRAY:
                writeByteArray(iProp, (byte[]) o);
                break;

            case J_CHAR_ARRAY:
                writeCharArray(iProp, (char[]) o);
                break;

            case J_SHORT_ARRAY:
                writeShortArray(iProp, (short[]) o);
                break;

            case J_INT_ARRAY:
                writeIntArray(iProp, (int[]) o);
                break;

            case J_LONG_ARRAY:
                writeLongArray(iProp, (long[]) o);
                break;

            case J_FLOAT_ARRAY:
                writeFloatArray(iProp, (float[]) o);
                break;

            case J_DOUBLE_ARRAY:
                writeDoubleArray(iProp, (double[]) o);
                break;

            case J_OBJECT_ARRAY:
                writeObjectArray(iProp, (Object[]) o);
                break;

            case J_SPARSE_ARRAY:
                writeLongArray(iProp, (LongArray) o);
                break;

            case J_COLLECTION:
                writeCollection(iProp, (Collection) o);
                break;

            case J_MAP:
                writeMap(iProp, (Map) o);
                break;

            case J_USER_TYPE:
            default:
                writeUserType(iProp, o);
            }
        }

    /**
    * Write a user-type to the POF stream.
    *
    * @param iProp  the property index
    * @param o      the object to write
    *
    * @throws IOException if an I/O error occurs
    */
    protected void writeUserType(int iProp, Object o)
            throws IOException
        {
        // replace checks for SerializationSupport
        o = ExternalizableHelper.replace(o);

        boolean fEvolvableOld = isEvolvable();
        boolean fEvolvable    = fEvolvableOld || o instanceof Evolvable;

        setEvolvable(fEvolvable);
        beginProperty(iProp);

        try
            {
            WritingPofHandler handler = getPofHandler();

            int              iRef = -1;
            boolean          fRef = false;
            ReferenceLibrary refs = m_refs;

            // COH-5065: due to the complexity of maintaining references
            // in future data, we won't support them for Evolvable objects
            if (refs != null && !fEvolvable)
                {
                iRef = refs.getIdentity(o);
                if (iRef < 0)
                    {
                    iRef = refs.registerReference(o);
                    }
                else
                    {
                    fRef = true;
                    }
                }

            if (fRef)
                {
                handler.onIdentityReference(iProp, iRef);
                }
            else
                {
                PofContext ctx = getPofContext();

                // resolve the user type identifier
                int nTypeId = ctx.getUserTypeIdentifier(o);

                // create a new PofWriter for the user type
                UserTypeWriter writer = new UserTypeWriter(this,
                        getPofHandler(), ctx, nTypeId, iProp, iRef);
                if (refs != null && !fEvolvable)
                    {
                    writer.enableReference();
                    }

                // serialize the object using a PofSerializer
                ctx.getPofSerializer(nTypeId).serialize(writer, o);

                // notify the nested PofWriter that it is closing
                writer.closeNested();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }

        endProperty(iProp);
        setEvolvable(fEvolvableOld);
        }

    /**
    * {@inheritDoc}
    */
    public <T> void writeObjectArray(int iProp, T[] ao)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (ao == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = ao.length;
                handler.registerIdentity(-1);
                handler.beginArray(iProp, cElements);
                for (int i = 0; i < cElements; ++i)
                    {
                    writeObject(i, ao[i]);
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public <T> void writeObjectArray(int iProp, T[] ao, Class<? extends T> clz)
            throws IOException
        {
        // COH-3370: uniform arrays cannot contain null values
        for (int i = 0, c = ao == null ? 0 : ao.length; i < c; ++i)
            {
            if (ao[i] == null)
                {
                writeObjectArray(iProp, ao);
                return;
                }
            }

        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (ao == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int nTypeId   = getPofTypeId(clz, getPofContext());
                int cElements = ao.length;

                handler.registerIdentity(-1);
                handler.beginUniformArray(iProp, cElements, nTypeId);
                for (int i = 0; i < cElements; ++i)
                    {
                    Object o = ao[i];
                    assertEqual(clz, o.getClass());
                    writeObject(i, o);
                    }
                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public <T> void writeCollection(int iProp, Collection<? extends T> coll)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (coll == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = coll.size();
                int cWritten  = 0;

                handler.registerIdentity(-1);
                handler.beginCollection(iProp, cElements);
                for (Iterator iter = coll.iterator(); iter.hasNext(); ++cWritten)
                    {
                    writeObject(cWritten, iter.next());
                    }

                // check for under/overflow
                if (cWritten != cElements)
                    {
                    throw new IOException("expected to write " + cElements
                        + " objects but actually wrote " + cWritten);
                    }

                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public <T> void writeCollection(int iProp, Collection<? extends T> coll, Class<? extends T> clz)
            throws IOException
        {
        // COH-3370: uniform collections cannot contain null values
        if (coll != null)
            {
            try
                {
                if (coll.contains(null))
                    {
                    writeCollection(iProp, coll);
                    return;
                    }
                }
            catch (NullPointerException e)
                {
                // according to the Collection documentation, contains() may
                // throw a NullPointerException if the Collection does not
                // support null values
                }
            }

        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (coll == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int nTypeId   = getPofTypeId(clz, getPofContext());
                int cElements = coll.size();
                int cWritten  = 0;

                handler.registerIdentity(-1);
                handler.beginUniformCollection(iProp, cElements, nTypeId);
                for (Iterator iter = coll.iterator(); iter.hasNext(); ++cWritten)
                    {
                    Object o = iter.next();
                    assertEqual(clz, o.getClass());
                    writeObject(cWritten, o);
                    }

                // check for under/overflow
                if (cWritten != cElements)
                    {
                    throw new IOException("expected to write " + cElements
                        + " objects but actually wrote " + cWritten);
                    }

                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    public void writeLongArray(int iProp, LongArray la)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (la == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                long nSize     = la.getLastIndex() + 1;
                int  cElements = la.getSize();
                int  cWritten  = 0;

                if (cElements > 0 && (la.getFirstIndex() < 0L || nSize > Integer.MAX_VALUE))
                    {
                    throw new IndexOutOfBoundsException("cannot encode LongArray["
                            + la.getFirstIndex() + ", " + la.getLastIndex()
                            + "] as a POF sparse array");
                    }

                handler.registerIdentity(-1);
                handler.beginSparseArray(iProp, (int) nSize);
                for (LongArray.Iterator iter = la.iterator(); iter.hasNext(); ++cWritten)
                    {
                    Object o = iter.next();
                    int    n = (int) iter.getIndex();
                    writeObject(n, o);
                    }

                // check for under/overflow
                if (cWritten != cElements)
                    {
                    throw new IOException("expected to write " + cElements
                        + " objects but actually wrote " + cWritten);
                    }

                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public void writeLongArray(int iProp, LongArray la, Class clz)
            throws IOException
        {
        // COH-3370: uniform arrays cannot contain null values
        if (la != null && la.contains(null))
            {
            writeLongArray(iProp, la);
            return;
            }

        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (la == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                long nSize     = la.getLastIndex() + 1;
                int  nTypeId   = getPofTypeId(clz, getPofContext());
                int  cElements = la.getSize();
                int  cWritten  = 0;

                if (cElements > 0 && (la.getFirstIndex() < 0L || nSize > Integer.MAX_VALUE))
                    {
                    throw new IndexOutOfBoundsException("cannot encode LongArray["
                            + la.getFirstIndex() + ", " + la.getLastIndex()
                            + "] as a POF sparse array");
                    }

                handler.registerIdentity(-1);
                handler.beginUniformSparseArray(iProp, (int) nSize, nTypeId);
                for (LongArray.Iterator iter = la.iterator(); iter.hasNext(); ++cWritten)
                    {
                    Object o = iter.next();
                    int    n = (int) iter.getIndex();
                    assertEqual(clz, o.getClass());
                    writeObject(n, o);
                    }

                // check for under/overflow
                if (cWritten != cElements)
                    {
                    throw new IOException("expected to write " + cElements
                        + " objects but actually wrote " + cWritten);
                    }

                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public <K, V> void writeMap(int iProp, Map<? extends K, ? extends V> map)
            throws IOException
        {
        beginProperty(iProp);
        try
            {
            WritingPofHandler handler = getPofHandler();
            if (map == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int cElements = map.size();
                int cWritten  = 0;

                handler.registerIdentity(-1);
                handler.beginMap(iProp, cElements);
                for (Iterator<? extends Map.Entry<? extends K, ? extends V>> iter = map.entrySet().iterator(); iter.hasNext();
                     ++cWritten)
                    {
                    Map.Entry entry  = iter.next();
                    Object    oKey   = entry.getKey();
                    Object    oValue = entry.getValue();

                    writeObject(cWritten, oKey);   // index is ignored
                    writeObject(cWritten, oValue); // index is ignored
                    }

                // check for under/overflow
                if (cWritten != cElements)
                    {
                    throw new IOException("expected to write " + cElements
                        + " objects but actually wrote " + cWritten);
                    }

                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public <K, V> void writeMap(int iProp, Map<K, ? extends V> map, Class<? extends K> clzKey)
            throws IOException
        {
        // COH-3370: uniform maps cannot contain null keys
        if (map != null)
            {
            try
                {
                if (map.containsKey(null))
                    {
                    writeMap(iProp, map);
                    return;
                    }
                }
            catch (NullPointerException e)
                {
                // according to the Map documentation, containsKey() may
                // throw a NullPointerException if the Map does not support
                // null keys
                }
            }

        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (map == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                int nTypeId   = getPofTypeId(clzKey, getPofContext());
                int cElements = map.size();
                int cWritten  = 0;

                handler.registerIdentity(-1);
                handler.beginUniformKeysMap(iProp, cElements, nTypeId);
                for (Iterator<? extends Map.Entry<K, ? extends V>> iter = map.entrySet().iterator(); iter.hasNext();
                     ++cWritten)
                    {
                    Map.Entry entry  = (Map.Entry) iter.next();
                    Object    oKey   = entry.getKey();
                    Object    oValue = entry.getValue();

                    assertEqual(clzKey, oKey.getClass());

                    writeObject(cWritten, oKey);   // index is ignored
                    writeObject(cWritten, oValue); // index is ignored
                    }

                // check for under/overflow
                if (cWritten != cElements)
                    {
                    throw new IOException("expected to write " + cElements
                        + " objects but actually wrote " + cWritten);
                    }

                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    /**
    * {@inheritDoc}
    */
    public <K, V> void writeMap(int iProp, Map<K, V> map, Class<? extends K> clzKey, Class<? extends V> clzValue)
            throws IOException
        {
        // COH-3370: uniform maps cannot contain null keys or values
        if (map != null)
            {
            for (Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry = iter.next();
                if (entry.getKey() == null || entry.getValue() == null)
                    {
                    writeMap(iProp, map);
                    return;
                    }
                }
            }

        beginProperty(iProp);
        try
            {
            PofHandler handler = getPofHandler();
            if (map == null)
                {
                handler.onNullReference(iProp);
                }
            else
                {
                PofContext ctx = getPofContext();

                int nTypeIdKey   = getPofTypeId(clzKey, ctx);
                int nTypeIdValue = getPofTypeId(clzValue, ctx);
                int cElements    = map.size();
                int cWritten     = 0;

                handler.registerIdentity(-1);
                handler.beginUniformMap(iProp, cElements, nTypeIdKey, nTypeIdValue);
                for (Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator(); iter.hasNext();
                     ++cWritten)
                    {
                    Map.Entry entry  = (Map.Entry) iter.next();
                    Object    oKey   = entry.getKey();
                    Object    oValue = entry.getValue();

                    assertEqual(clzKey, oKey.getClass());
                    assertEqual(clzValue, oValue.getClass());

                    writeObject(cWritten, oKey);   // index is ignored
                    writeObject(cWritten, oValue); // index is ignored
                    }

                // check for under/overflow
                if (cWritten != cElements)
                    {
                    throw new IOException("expected to write " + cElements
                        + " objects but actually wrote " + cWritten);
                    }

                handler.endComplexValue();
                }
            }
        catch (Exception e)
            {
            onException(e);
            }
        endProperty(iProp);
        }

    // ----- optional types support -----------------------------------------


    public void writeOptionalInt(int iProp, OptionalInt n) throws IOException
        {

        }

    public void writeOptionalLong(int iProp, OptionalLong n) throws IOException
        {

        }

    public void writeOptionalDouble(int iProp, OptionalDouble n)
            throws IOException
        {

        }

    public <T> void writeOptional(int iProp, Optional<T> o) throws IOException
        {

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
    public void setVersionId(int nVersionId)
        {
        throw new IllegalStateException("not in a user type");
        }

    /**
    * {@inheritDoc}
    */
    public PofWriter createNestedPofWriter(int iProp)
            throws IOException
        {
        throw new IllegalStateException("not in a user type");
        }

    /**
    * {@inheritDoc}
    */
    public PofWriter createNestedPofWriter(int iProp, int nTypeId)
            throws IOException
        {
        throw new IllegalStateException("not in a user type");
        }

    /**
    * {@inheritDoc}
    */
    public void writeRemainder(Binary binProps)
            throws IOException
        {
        throw new IllegalStateException("not in a user type");
        }


    // ----- internal methods -----------------------------------------------

    /**
    * If this writer is contextually within a user type, obtain the writer
    * which created this writer in order to write the user type.
    *
    * @return the containing writer
    */
    protected PofBufferWriter getParentWriter()
        {
        return null;
        }

    /**
    * Report that a POF property is about to be written to the POF stream.
    * <p>
    * This method call will be followed by one or more separate calls to a
    * "write" method and the property extent will then be terminated by a
    * call to {@link #endProperty}.
    *
    * @param iProp  the index of the property being written
    *
    * @throws IllegalArgumentException  if the property index is invalid, or
    *         is less than or equal to the index of the previous property
    *         written to the POF stream
    * @throws IOException  if an I/O error occurs
    */
    protected void beginProperty(int iProp)
            throws IOException
        {
        if (iProp > 0 && getPofHandler().getComplex() == null)
            {
            throw new IllegalArgumentException("not in a complex type");
            }
        }

    /**
    * Signifies the termination of the current POF property.
    *
    * @param iProp  the index of the current property
    */
    protected void endProperty(int iProp)
        {
        }

    /**
    * Called when an unexpected exception is caught while writing to the POF
    * stream.
    * <p>
    * If the given exception wraps an IOException, the IOException is
    * unwrapped and rethrown; otherwise the given exception is rethrown.
    *
    * @param e  the exception
    *
    * @throws IOException  the wrapped IOException, if the given exception is
    *         a wrapped IOException
    */
    protected void onException(Exception e)
            throws IOException
        {
        if (e instanceof WrapperException)
            {
            Throwable eOrig = ((WrapperException) e).getOriginalException();
            if (eOrig instanceof IOException)
                {
                throw (IOException) eOrig;
                }
            }

        throw ensureRuntimeException(e);
        }

    /**
    * Return a {@link Date} which is suitable for POF serialization
    * <p>
    * Similar to {@link Timestamp}, POF requires that dates have positive
    * sub-second values.
    *
    * @param dt  the Date object to check
    *
    * @return the passed in Date if no conversion is necessary; otherwise
    *         the passed in Date as a Timestamp.
    *
    * @since Coherence 3.7.1.13
    */
    protected static Date fixNanos(Date dt)
        {
        // COH-11890
        long cTime = dt.getTime();
        return (cTime < 0 && cTime % 1000 != 0)
                ? new Timestamp(cTime)
                : dt;
        }

    /**
    * Extract the nanoseconds from the date. This number represents a fraction
    * of a second in the passed-in date expressed with a nanosecond resolution.
    *
    * @param dt  the Date object to extract the nanoseconds from
    *
    * @return the number of nanoseconds in the date
    */
    protected static int getNanos(Date dt)
        {
        return dt instanceof Timestamp
                ? ((Timestamp) dt).getNanos()
                : (int) (dt.getTime() % 1000) * 1000000;
        }

    /**
    * Assert that a class is equal to another class.
    *
    * @param clz      the expected class; must not be null
    * @param clzTest  the class to test for equality; must not be null
    *
    * @throws IllegalArgumentException  if the second class is not equal to
    *         the first
    */
    protected static void assertEqual(Class clz, Class clzTest)
        {
        if (!clz.equals(clzTest))
            {
            throw new IllegalArgumentException("illegal class \""
                    + clzTest.getName() + "\"; expected \""
                    + clz.getName() + '"');
            }
        }


    // ----- inner class: UserTypeWriter ------------------------------------

    /**
    * The UserTypeWriter implementation is a contextually-aware PofWriter
    * whose purpose is to write the properties of a value of a specified user
    * type. The "contextual awareness" refers to the fact that the
    * UserTypeWriter maintains state about the type identifier, the
    * PofWriter's property index position within the user type value, and a
    * PofContext that may differ from the PofContext that provided the
    * PofSerializer which is using this UserTypeWriter to serialize a user
    * type.
    */
    public static class UserTypeWriter
            extends PofBufferWriter
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct a UserTypeWriter for writing the property values of a
        * user type.
        *
        * @param out      the BufferOutput object to write to; must not be
        *                 null
        * @param ctx      the PofContext to use for writing the user type
        *                 property values within the user type that this
        *                 writer will be writing
        * @param nTypeId  the type identifier of the user type; must be
        *                 non-negative
        * @param iProp    the index of the user type being written
        */
        public UserTypeWriter(WriteBuffer.BufferOutput out,
                PofContext ctx, int nTypeId, int iProp)
            {
            this(null, out, ctx, nTypeId, iProp);
            }

        /**
        * Construct a UserTypeWriter for writing the property values of a
        * user type.
        *
        * @param parent   the containing PofBufferWriter
        * @param out      the BufferOutput object to write to; must not be
        *                 null
        * @param ctx      the PofContext to use for writing the user type
        *                 property values within the user type that this
        *                 writer will be writing
        * @param nTypeId  the type identifier of the user type; must be
        *                 non-negative
        * @param iProp    the index of the user type being written
        */
        public UserTypeWriter(PofBufferWriter parent, WriteBuffer.BufferOutput out,
                PofContext ctx, int nTypeId, int iProp)
            {
            super(out, ctx);

            assert nTypeId >= 0;

            m_writerParent  = parent;
            m_nTypeId       = nTypeId;
            m_iProp         = iProp;
            m_refs          = parent == null ? null : parent.m_refs;
            }

        /**
        * Construct a UserTypeWriter for writing the property values of a
        * user type.
        *
        * @param handler  the WritingPofHandler used to write user type data
        *                 (except for the user type id itself, which is
        *                 passed as a constructor argument)
        * @param ctx      the PofContext to use for writing the user type
        *                 property values within the user type that this
        *                 writer will be writing
        * @param nTypeId  the type identifier of the user type; must be
        *                 non-negative
        * @param iProp    the index of the user type being written
        */
        public UserTypeWriter(WritingPofHandler handler,
                PofContext ctx, int nTypeId, int iProp)
            {
            this(null, handler, ctx, nTypeId, iProp);
            }

        /**
        * Construct a UserTypeWriter for writing the property values of a
        * user type.
        *
        * @param parent   the containing PofBufferWriter
        * @param handler  the WritingPofHandler used to write user type data
        *                 (except for the user type id itself, which is
        *                 passed as a constructor argument)
        * @param ctx      the PofContext to use for writing the user type
        *                 property values within the user type that this
        *                 writer will be writing
        * @param nTypeId  the type identifier of the user type; must be
        *                 non-negative
        * @param iProp    the index of the user type being written
        */
        public UserTypeWriter(PofBufferWriter parent, WritingPofHandler handler,
                PofContext ctx, int nTypeId, int iProp)
            {
            this(parent,  handler, ctx, nTypeId, iProp, -1);
            }

        /**
        * Construct a UserTypeWriter for writing the property values of a
        * user type.
        *
        * @param parent   the containing PofBufferWriter
        * @param handler  the WritingPofHandler used to write user type data
        *                 (except for the user type id itself, which is
        *                 passed as a constructor argument)
        * @param ctx      the PofContext to use for writing the user type
        *                 property values within the user type that this
        *                 writer will be writing
        * @param nTypeId  the type identifier of the user type; must be
        *                 non-negative
        * @param iProp    the index of the user type being written
        * @param nId      the identity of the object to encode, or -1 if the
        *                 identity shouldn't be encoded in the POF stream
        */
        public UserTypeWriter(PofBufferWriter parent, WritingPofHandler handler,
                PofContext ctx, int nTypeId, int iProp, int nId)
            {
            super(handler, ctx);

            assert nTypeId >= 0;

            m_writerParent  = parent;
            m_nTypeId       = nTypeId;
            m_iProp         = iProp;
            m_refs          = parent == null ? null : parent.m_refs;
            m_nId           = nId;
            }

        // ----- PofWriter interface ------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        public int getUserTypeId()
            {
            return m_nTypeId;
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public int getVersionId()
            {
            return m_nVersionId;
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public void setVersionId(int nVersionId)
            {
            if (nVersionId < 0)
                {
                throw new IllegalArgumentException(
                        "negative version identifier: " + nVersionId);
                }
            m_nVersionId = nVersionId;
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public PofWriter createNestedPofWriter(int iProp)
                throws IOException
            {
            return createNestedPofWriter(iProp, m_nTypeId);
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public PofWriter createNestedPofWriter(int iProp, int nTypeId)
                throws IOException
            {
            beginProperty(iProp);
            try
                {
                getPofHandler().registerIdentity(-1);

                // create a new PofWriter for the user type
                PofContext     ctx    = getPofContext();
                UserTypeWriter writer = new UserTypeWriter(this,
                        getPofHandler(), ctx, nTypeId, iProp);
                if (isReferenceEnabled())
                    {
                    writer.enableReference();
                    }

                m_writerNested = writer;
                return writer;
                }
            catch (Exception e)
                {
                onException(e);
                throw azzert();
                }
            // note: there is no endProperty() call at this point, since the
            //       property has yet to be written
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public void writeRemainder(Binary binProps)
                throws IOException
            {
            // if a nested writer is still open, then "end" that property
            closeNested();

            // write out the type and version identifiers, if necessary
            writeUserTypeInfo();

            try
                {
                if (binProps != null)
                    {
                    getBufferOutput().writeBuffer(binProps);
                    }
                getPofHandler().endComplexValue();
                }
            catch (Exception e)
                {
                onException(e);
                }

            m_fUserTypeEnd = true; // EOF
            m_complex      = null;
            }

        // ----- internal methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected PofBufferWriter getParentWriter()
            {
            return m_writerParent;
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void beginProperty(int iProp)
                throws IOException
            {
            // if a nested writer is still open, then "end" that property
            closeNested();

            // check for negative index
            if (iProp < 0)
                {
                throw new IllegalArgumentException("negative property index: "
                        + iProp);
                }

            // write out the type and version identifiers, if necessary
            writeUserTypeInfo();

            // check for backwards movement
            if (getPofHandler().getComplex() == m_complex && iProp <= m_iPrevProp)
                {
                throw new IllegalArgumentException("previous property index="
                        + m_iPrevProp + ", requested property index=" + iProp
                        + " while writing user type " + getUserTypeId());
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void endProperty(int iProp)
            {
            if (getPofHandler().getComplex() == m_complex)
                {
                m_iPrevProp = iProp;
                }
            }

        /**
        * Notify the UserTypeWriter that it is being "closed". This
        * notification allows the UserTypeWriter to write any remaining data
        * that it has pending to write.
        */
        protected void closeNested()
            {
            // check if a nested PofWriter is open
            UserTypeWriter writerNested = m_writerNested;
            if (writerNested != null)
                {
                // make sure that the entire nested is terminated
                if (!writerNested.m_fUserTypeEnd)
                    {
                    getPofHandler().endComplexValue();
                    }

                // close it
                writerNested.closeNested();

                // finish writing the property that the nested PofWriter was
                // writing into
                endProperty(writerNested.m_iProp);

                m_writerNested = null;
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void onException(Exception e)
                throws IOException
            {
            m_fUserTypeEnd = true; // EOF
            m_complex      = null;

            super.onException(e);
            }

        /**
        * Write out the type and version identifiers of the user type to the
        * POF stream, if they haven't already been written.
        *
        * @throws IOException on I/O error
        */
        protected void writeUserTypeInfo()
                throws IOException
            {
            // check for EOF
            if (m_fUserTypeEnd)
                {
                throw new EOFException("user type POF stream terminated");
                }

            if (!m_fUserTypeBegin)
                {
                WritingPofHandler handler = getPofHandler();
                try
                    {
                    handler.beginUserType(m_iProp, m_nId, getUserTypeId(), getVersionId());
                    }
                catch (Exception e)
                    {
                    onException(e);
                    }
                m_fUserTypeBegin = true;
                m_complex        = handler.getComplex();
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public void enableReference()
            {
            if (m_refs == null)
                {
                PofBufferWriter parent = m_writerParent;
                if (parent == null)
                    {
                    m_refs = new ReferenceLibrary();
                    }
                else
                    {
                    parent.enableReference();
                    m_refs = parent.m_refs;
                    }

                UserTypeWriter child = m_writerNested;
                if (child != null)
                    {
                    child.enableReference();
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected boolean isEvolvable()
            {
            if (!m_fEvolvable)
                {
                PofBufferWriter parent = m_writerParent;
                if (parent != null)
                    {
                    m_fEvolvable = parent.isEvolvable();
                    }
                }
            return m_fEvolvable;
            }

        // ----- data members ---------------------------------------------

        /**
        * The parent (ie containing) PofBufferWriter.
        */
        private PofBufferWriter m_writerParent;

        /**
        * The type identifier of the user type that is being written.
        */
        protected int m_nTypeId;

        /**
        * The version identifier of the user type that is being written.
        */
        protected int m_nVersionId;

        /**
        * The index of the user type being written.
        */
        protected int m_iProp;

        /**
        * The identity of the object to encode, or -1 if the identity
        * shouldn't be encoded in the POF stream
        */
        protected int m_nId = -1;

        /**
        * The index of the last property written to the POF stream or -1 if
        * the first property has yet to be written.
        */
        protected int m_iPrevProp = -1;

        /**
        * True iff the type and version identifier of the user type was
        * written to the POF stream.
        */
        protected boolean m_fUserTypeBegin;

        /**
        * True iff the user type was written to the POF stream.
        */
        protected boolean m_fUserTypeEnd;

        /**
        * The Complex value that corresponds to the user type that is being
        * written.
        */
        protected WritingPofHandler.Complex m_complex;

        /**
        * The currently open nested writer, if any.
        */
        protected UserTypeWriter m_writerNested;
        }


    // ----- inner class: ReferenceLibrary ----------------------------------

    /**
    * A "library" of object references and their corresponding identities in
    * the POF stream.
    */
    public static class ReferenceLibrary
        {
        /**
        * Look up an identity for an object.
        *
        * @param o  the object
        *
        * @return the identity, or -1 if the object is not registered
        */
        public int getIdentity(Object o)
            {
            Integer I = (Integer) m_mapIdentities.get(o);
            return I == null ? -1 : I.intValue();
            }

        /**
        * Register an object.
        *
        * @param o  the object
        *
        * @return the assigned identity for the object
        *
        * @throws IllegalStateException  if the object is already registered
        */
        public int registerReference(Object o)
            {
            IdentityHashMap mapIdentities = m_mapIdentities;
            int iRef = ++m_cRefs;
            Integer IOld = (Integer) mapIdentities.put(o, Integer.valueOf(iRef));
            if (IOld != null)
                {
                --m_cRefs;
                mapIdentities.put(o, IOld);
                throw new IllegalStateException("object already registered");
                }

            return iRef;
            }

        /**
        * The reference counter.
        */
        private int m_cRefs;

        /**
        * A map from objects that can be referenced to their Integer
        * identities.
        */
        protected IdentityHashMap m_mapIdentities = new IdentityHashMap();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the BufferOutput that this PofBufferWriter writes to.
    *
    * @return the BufferOutput
    */
    protected WriteBuffer.BufferOutput getBufferOutput()
        {
        return m_out;
        }

    /**
    * Return the WritingPofHandler used internally by this PofBufferWriter
    * to write the POF stream.
    *
    * @return the PofHandler
    */
    protected WritingPofHandler getPofHandler()
        {
        return m_handler;
        }

    /**
    * Ensure that reference support (necessary for cyclic dependencies) is
    * enabled.
    */
    public void enableReference()
        {
        if (m_refs == null)
            {
            m_refs = new ReferenceLibrary();
            }
        }

    /**
    * Determine if reference support is enabled.
    *
    * @return true iff reference support is enabled
    */
    public boolean isReferenceEnabled()
        {
        return m_refs != null;
        }

    /**
    * Determine if the object to be written is either Evolvable or part of an
    * Evolvable object.
    *
    * @return true iff the object to be written is Evolvable
    */
    protected boolean isEvolvable()
        {
        return m_fEvolvable;
        }

    /**
    * Set the Evolvable flag to indicate if the object to be written is
    * Evolvable or part of an Evolvable object.
    *
    * @param fEvolvable  true iff the object to be written is Evolvable
    */
    protected void setEvolvable(boolean fEvolvable)
        {
        m_fEvolvable = fEvolvable;
        }


    // ------ data members --------------------------------------------------

    /**
    * The BufferOutput object that the PofBufferWriter writes to.
    */
    protected WriteBuffer.BufferOutput m_out;

    /**
    * The PofContext used by this PofBufferWriter to serialize user types.
    */
    protected PofContext m_ctx;

    /**
    * Indicate if the object to be written is either Evolvable or part of an
    * Evolvable object.
    */
    protected boolean m_fEvolvable;

    /**
    * The WritingPofHandler used to write a POF stream.
    */
    protected WritingPofHandler m_handler;

    /**
    * If references are used, then this is the ReferenceLibrary.
    */
    protected ReferenceLibrary m_refs;

    public static void main(String[] args)
        {
        System.out.println(Integer.highestOneBit(-30 / 60));
        System.out.println(Integer.highestOneBit(30 / 60));
        }
    }
