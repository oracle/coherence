/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
* An implementation of PofHandler that writes a POF stream to a WriteBuffer
* using a BufferOutput object.
*
* @author cp  2006.07.11
*
* @since Coherence 3.2
*/
public class WritingPofHandler
        extends PofHelper
        implements PofHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a Writing POF Handler that will write a POF stream to the
    * passed BufferOutput object.
    *
    * @param out  the BufferOutput to write to
    */
    public WritingPofHandler(WriteBuffer.BufferOutput out)
        {
        assert out != null;

        m_out = out;
        }


    // ----- PofHandler interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void registerIdentity(int nId)
        {
        assert !m_fHasIdentity || nId < 0;

        if (nId >= 0)
            {
            WriteBuffer.BufferOutput out = m_out;
            try
                {
                out.writePackedInt(T_IDENTITY);
                out.writePackedInt(nId);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        m_fHasIdentity = true;
        }

    /**
    * {@inheritDoc}
    */
    public void onNullReference(int iPos)
        {
        if (!isSkippable())
            {
            encodePosition(iPos);

            try
                {
                m_out.writePackedInt(V_REFERENCE_NULL);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onIdentityReference(int iPos, int nId)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_REFERENCE))
                {
                out.writePackedInt(T_REFERENCE);
                }
            out.writePackedInt(nId);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onInt16(int iPos, short n)
        {
        if (n != 0 || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_INT16))
                    {
                    if (n >= -1 && n <= 22 && fCompressable)
                        {
                        out.writePackedInt(encodeTinyInt(n));
                        break output;
                        }
                    out.writePackedInt(T_INT16);
                    }
                out.writePackedInt(n);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onInt32(int iPos, int n)
        {
        if (n != 0 || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_INT32))
                    {
                    if (n >= -1 && n <= 22 && fCompressable)
                        {
                        out.writePackedInt(encodeTinyInt(n));
                        break output;
                        }
                    out.writePackedInt(T_INT32);
                    }
                out.writePackedInt(n);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onInt64(int iPos, long n)
        {
        if (n != 0L || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_INT64))
                    {
                    if (n >= -1L && n <= 22L && fCompressable)
                        {
                        out.writePackedInt(encodeTinyInt((int) n));
                        break output;
                        }
                    out.writePackedInt(T_INT64);
                    }
                out.writePackedLong(n);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onInt128(int iPos, BigInteger n)
        {
        if (!n.equals(BigInteger.ZERO) || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_INT128))
                    {
                    if (n.bitLength() <= 7 && fCompressable)
                        {
                        int nTiny = n.intValue();
                        if (nTiny >= -1 && nTiny <= 22)
                            {
                            out.writePackedInt(encodeTinyInt(nTiny));
                            break output;
                            }
                        }
                    out.writePackedInt(T_INT128);
                    }

                writeBigInteger(out, n);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat32(int iPos, float fl)
        {
        if (fl != 0.0F || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_FLOAT32))
                    {
                    int nBits = Float.floatToIntBits(fl);
                    if ((nBits & 0x0000FFFF) == 0 && fCompressable)
                        {
                        switch (nBits >>> 16)
                            {
                            case 0xFF80:
                                out.writePackedInt(V_FP_NEG_INFINITY);
                                break output;

                            case 0x7F80:
                                out.writePackedInt(V_FP_POS_INFINITY);
                                break output;

                            case 0x7FC0:
                                out.writePackedInt(V_FP_NAN);
                                break output;

                            case 0xBF80: // -1
                            case 0x0000: // 0
                            case 0x3F80: // 1
                            case 0x4000:
                            case 0x4040:
                            case 0x4080:
                            case 0x40A0:
                            case 0x40C0:
                            case 0x40E0:
                            case 0x4100:
                            case 0x4110:
                            case 0x4120:
                            case 0x4130:
                            case 0x4140:
                            case 0x4150:
                            case 0x4160:
                            case 0x4170:
                            case 0x4180:
                            case 0x4188:
                            case 0x4190:
                            case 0x4198:
                            case 0x41A0:
                            case 0x41A8:
                            case 0x41B0: // 22
                                out.writePackedInt(encodeTinyInt((int) fl));
                                break output;
                            }
                        }
                    out.writePackedInt(T_FLOAT32);
                    out.writeInt(nBits);
                    }
                else
                    {
                    out.writeFloat(fl);
                    }
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat64(int iPos, double dfl)
        {
        if (dfl != 0.0 || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_FLOAT64))
                    {
                    long nBits = Double.doubleToLongBits(dfl);
                    if ((nBits & 0x0000FFFFFFFFFFFFL) == 0L && fCompressable)
                        {
                        switch ((int) (nBits >>> 48))
                            {
                            case 0xFFF0:
                                out.writePackedInt(V_FP_NEG_INFINITY);
                                break output;

                            case 0x7FF0:
                                out.writePackedInt(V_FP_POS_INFINITY);
                                break output;

                            case 0x7FF8:
                                out.writePackedInt(V_FP_NAN);
                                break output;

                            case 0xBFF0: // -1
                            case 0x0000: // 0
                            case 0x3FF0: // 1
                            case 0x4000:
                            case 0x4008:
                            case 0x4010:
                            case 0x4014:
                            case 0x4018:
                            case 0x401C:
                            case 0x4020:
                            case 0x4022:
                            case 0x4024:
                            case 0x4026:
                            case 0x4028:
                            case 0x402A:
                            case 0x402C:
                            case 0x402E:
                            case 0x4030:
                            case 0x4031:
                            case 0x4032:
                            case 0x4033:
                            case 0x4034:
                            case 0x4035:
                            case 0x4036: // 22
                                out.writePackedInt(V_INT_0 - (int) dfl);
                                break output;
                            }
                        }
                    out.writePackedInt(T_FLOAT64);
                    out.writeLong(nBits);
                    }
                else
                    {
                    out.writeDouble(dfl);
                    }
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat128(int iPos, RawQuad qfl)
        {
        if (!qfl.equals(RawQuad.ZERO) || !isSkippable())
            {
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            try
                {
                if (isTypeIdEncoded(T_FLOAT128))
                    {
                    out.writePackedInt(T_FLOAT128);
                    }

                out.writeBuffer(qfl.getBits());
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal32(int iPos, BigDecimal dec)
        {
        if (dec.scale() != 0 || !dec.unscaledValue().equals(BigInteger.ZERO) || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_DECIMAL32))
                    {
                    if (dec.scale() == 0 && fCompressable)
                        {
                        BigInteger n = dec.unscaledValue();
                        if (n.bitLength() <= 7)
                            {
                            int nTiny = n.intValue();
                            if (nTiny >= -1 && nTiny <= 22)
                                {
                                out.writePackedInt(encodeTinyInt(nTiny));
                                break output;
                                }
                            }
                        }
                    out.writePackedInt(T_DECIMAL32);
                    }

                writeBigDecimal(out, dec, 4);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal64(int iPos, BigDecimal dec)
        {
        if (dec.scale() != 0 || !dec.unscaledValue().equals(BigInteger.ZERO) || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_DECIMAL64))
                    {
                    if (dec.scale() == 0 && fCompressable)
                        {
                        BigInteger n = dec.unscaledValue();
                        if (n.bitLength() <= 7)
                            {
                            int nTiny = n.intValue();
                            if (nTiny >= -1 && nTiny <= 22)
                                {
                                out.writePackedInt(encodeTinyInt(nTiny));
                                break output;
                                }
                            }
                        }
                    out.writePackedInt(T_DECIMAL64);
                    }

                writeBigDecimal(out, dec, 8);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal128(int iPos, BigDecimal dec)
        {
        if (dec.scale() != 0 || !dec.unscaledValue().equals(BigInteger.ZERO) || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_DECIMAL128))
                    {
                    if (dec.scale() == 0 && fCompressable)
                        {
                        BigInteger n = dec.unscaledValue();
                        if (n.bitLength() <= 7)
                            {
                            int nTiny = n.intValue();
                            if (nTiny >= -1 && nTiny <= 22)
                                {
                                out.writePackedInt(encodeTinyInt(nTiny));
                                break output;
                                }
                            }
                        }
                    out.writePackedInt(T_DECIMAL128);
                    }

                writeBigDecimal(out, dec, 16);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onBoolean(int iPos, boolean f)
        {
        if (f || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_BOOLEAN))
                    {
                    if (fCompressable)
                        {
                        out.writePackedInt(f ? V_BOOLEAN_TRUE : V_BOOLEAN_FALSE);
                        break output;
                        }
                    out.writePackedInt(T_BOOLEAN);
                    }

                out.writePackedInt(f ? 1 : 0);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onOctet(int iPos, int b)
        {
        if (b != 0x00 || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_OCTET))
                    {
                    if (fCompressable)
                        {
                        // get rid of any extra bits
                        b &= 0xFF;

                        if (b <= 22)
                            {
                            out.writePackedInt(encodeTinyInt(b));
                            break output;
                            }
                        else if (b == 0xFF)
                            {
                            out.writePackedInt(V_INT_NEG_1);
                            break output;
                            }
                        }

                    out.writePackedInt(T_OCTET);
                    }
                out.writeByte(b);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onOctetString(int iPos, Binary bin)
        {
        if (bin.length() != 0 || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_OCTET_STRING))
                    {
                    if (bin.length() == 0 && fCompressable)
                        {
                        out.writePackedInt(V_STRING_ZERO_LENGTH);
                        break output;
                        }

                    out.writePackedInt(T_OCTET_STRING);
                    }

                out.writePackedInt(bin.length());
                out.writeBuffer(bin);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onChar(int iPos, char ch)
        {
        if (ch != 0 || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_CHAR))
                    {
                    if (fCompressable)
                        {
                        if (ch <= 22)
                            {
                            out.writePackedInt(encodeTinyInt(ch));
                            break output;
                            }
                        else if (ch == 0xFFFF)
                            {
                            out.writePackedInt(V_INT_NEG_1);
                            break output;
                            }
                        }

                    out.writePackedInt(T_CHAR);
                    }

                if (ch >= 0x0000 && ch <= 0x007F)
                    {
                    // 1-byte format:  0xxx xxxx
                    out.writeByte(ch);
                    }
                else if (ch <= 0x07FF)
                    {
                    // 2-byte format:  110x xxxx, 10xx xxxx
                    int b0 = 0xC0 | ((ch >>> 6) & 0x1F);
                    int b1 = 0x80 | ((ch      ) & 0x3F);
                    out.writeShort((short) ((b0 << 8) | b1));
                    }
                else
                    {
                    // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                    out.writeByte((byte) (0xE0 | ((ch >>> 12) & 0x0F)));
                    out.writeByte((byte) (0x80 | ((ch >>>  6) & 0x3F)));
                    out.writeByte((byte) (0x80 | ((ch       ) & 0x3F)));
                    }
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onCharString(int iPos, String s)
        {
        if (s.length() != 0 || !isSkippable())
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_CHAR_STRING))
                    {
                    if (s.length() == 0 && fCompressable)
                        {
                        out.writePackedInt(V_STRING_ZERO_LENGTH);
                        break output;
                        }

                    out.writePackedInt(T_CHAR_STRING);
                    }

                out.writeSafeUTF(s);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onDate(int iPos, int nYear, int nMonth, int nDay)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_DATE))
                {
                out.writePackedInt(T_DATE);
                }

            writeDate(out, nYear, nMonth, nDay);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onYearMonthInterval(int iPos, int cYears, int cMonths)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_YEAR_MONTH_INTERVAL))
                {
                out.writePackedInt(T_YEAR_MONTH_INTERVAL);
                }

            out.writePackedInt(cYears);
            out.writePackedInt(cMonths);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, boolean fUTC)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_TIME))
                {
                out.writePackedInt(T_TIME);
                }

            writeTime(out, nHour, nMinute, nSecond, nNano, fUTC ? 1 : 0, 0, 0);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, int nHourOffset, int nMinuteOffset)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_TIME))
                {
                out.writePackedInt(T_TIME);
                }

            writeTime(out, nHour, nMinute, nSecond, nNano, 2, nHourOffset, nMinuteOffset);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onTimeInterval(int iPos, int cHours, int cMinutes,
            int cSeconds, int cNanos)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_TIME_INTERVAL))
                {
                out.writePackedInt(T_TIME_INTERVAL);
                }

            out.writePackedInt(cHours);
            out.writePackedInt(cMinutes);
            out.writePackedInt(cSeconds);
            out.writePackedInt(cNanos);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano, boolean fUTC)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_DATETIME))
                {
                out.writePackedInt(T_DATETIME);
                }

            writeDate(out, nYear, nMonth, nDay);
            writeTime(out, nHour, nMinute, nSecond, nNano, fUTC ? 1 : 0, 0, 0);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano,
            int nHourOffset, int nMinuteOffset)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_DATETIME))
                {
                out.writePackedInt(T_DATETIME);
                }

            writeDate(out, nYear, nMonth, nDay);
            writeTime(out, nHour, nMinute, nSecond, nNano, 2, nHourOffset, nMinuteOffset);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onDayTimeInterval(int iPos, int cDays, int cHours,
            int cMinutes, int cSeconds, int cNanos)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            if (isTypeIdEncoded(T_DAY_TIME_INTERVAL))
                {
                out.writePackedInt(T_DAY_TIME_INTERVAL);
                }
            out.writePackedInt(cDays);
            out.writePackedInt(cHours);
            out.writePackedInt(cMinutes);
            out.writePackedInt(cSeconds);
            out.writePackedInt(cNanos);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginCollection(int iPos, int cElements)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_COLLECTION))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        break output;
                        }

                    out.writePackedInt(T_COLLECTION);
                    }

                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new Complex(m_complex, false);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformCollection(int iPos, int cElements, int nType)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_UNIFORM_COLLECTION))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        break output;
                        }

                    out.writePackedInt(T_UNIFORM_COLLECTION);
                    }

                out.writePackedInt(nType);
                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new Complex(m_complex, false, nType);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginArray(int iPos, int cElements)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_ARRAY))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        break output;
                        }

                    out.writePackedInt(T_ARRAY);
                    }

                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new Complex(m_complex, false);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformArray(int iPos, int cElements, int nType)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_UNIFORM_ARRAY))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        break output;
                        }

                    out.writePackedInt(T_UNIFORM_ARRAY);
                    }

                out.writePackedInt(nType);
                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new Complex(m_complex, false, nType);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginSparseArray(int iPos, int cElements)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            boolean fTerminated = true;
            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_SPARSE_ARRAY))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        fTerminated = false;
                        break output;
                        }

                    out.writePackedInt(T_SPARSE_ARRAY);
                    }

                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new Complex(m_complex, fTerminated);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformSparseArray(int iPos, int cElements, int nType)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            boolean fTerminated = true;
            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_UNIFORM_SPARSE_ARRAY))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        fTerminated = false;
                        break output;
                        }

                    out.writePackedInt(T_UNIFORM_SPARSE_ARRAY);
                    }

                out.writePackedInt(nType);
                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new Complex(m_complex, fTerminated, nType);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginMap(int iPos, int cElements)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_MAP))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        break output;
                        }

                    out.writePackedInt(T_MAP);
                    }

                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new Complex(m_complex, false);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformKeysMap(int iPos, int cElements, int nTypeKeys)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_UNIFORM_KEYS_MAP))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        break output;
                        }

                    out.writePackedInt(T_UNIFORM_KEYS_MAP);
                    }

                out.writePackedInt(nTypeKeys);
                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new ComplexMap(m_complex, nTypeKeys);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformMap(int iPos, int cElements,
                                int nTypeKeys, int nTypeValues)
        {
        if (cElements == 0 && isSkippable())
            {
            // dummy complex type (no contents, no termination)
            m_complex = new Complex(m_complex, false);
            }
        else
            {
            boolean fCompressable = isCompressable();
            encodePosition(iPos);

            WriteBuffer.BufferOutput out = m_out;
            output: try
                {
                if (isTypeIdEncoded(T_UNIFORM_MAP))
                    {
                    if (cElements == 0 && fCompressable)
                        {
                        out.writePackedInt(V_COLLECTION_EMPTY);
                        break output;
                        }

                    out.writePackedInt(T_UNIFORM_MAP);
                    }

                out.writePackedInt(nTypeKeys);
                out.writePackedInt(nTypeValues);
                out.writePackedInt(cElements);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            m_complex = new ComplexMap(m_complex, nTypeKeys, nTypeValues);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void beginUserType(int iPos, int nUserTypeId, int nVersionId)
        {
        beginUserType(iPos, -1, nUserTypeId, nVersionId);
        }

    /**
    * Report that a value of a "user type" has been encountered in the POF
    * stream. A user type is analogous to a "class", and a value of a user
    * type is analogous to an "object".
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the property values in the user type, and
    * the user type will then be terminated by a call to
    * {@link #endComplexValue()}.
    *
    * @param iPos         context-sensitive position information: property
    *                     index within a user type, array index within an
    *                     array, element counter within a collection, entry
    *                     counter within a map, -1 otherwise
    * @param nId          identity of the object to encode, or -1 if identity
    *                     shouldn't be encoded in the POF stream
    * @param nUserTypeId  the user type identifier,
    *                     <tt>(nUserTypeId &gt;= 0)</tt>
    * @param nVersionId   the version identifier for the user data type data
    *                     in the POF stream, <tt>(nVersionId &gt;= 0)</tt>
    */
    public void beginUserType(int iPos, int nId, int nUserTypeId, int nVersionId)
        {
        encodePosition(iPos);

        WriteBuffer.BufferOutput out = m_out;
        try
            {
            registerIdentity(nId);
            if (isTypeIdEncoded(nUserTypeId))
                {
                out.writePackedInt(nUserTypeId);
                }

            out.writePackedInt(nVersionId);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        m_complex = new Complex(m_complex, true);
        }

    /**
    * {@inheritDoc}
    */
    public void endComplexValue()
        {
        Complex complex = m_complex;
        if (complex.isSparse())
            {
            try
                {
                m_out.writePackedInt(-1);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        m_complex = complex.pop();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the DataOutput object that this Writing POF Handler is writing
    * to.
    *
    * @return the DataOutput object that this POF handler is writing to
    */
    public WriteBuffer.BufferOutput getBufferOutput()
        {
        return m_out;
        }

    /**
    * Obtain the current Complex object that represents the complex type that
    * is being written to the POF stream.
    *
    * @return the current Complex object
    */
    protected Complex getComplex()
        {
        return m_complex;
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Determine if the value encoding can be skipped. A value can be skipped
    * if it is a default value and if it does not have an identity and if it
    * is in a sparse data structure.
    *
    * @return true iff value encoding of default values can be skipped
    *         altogether
    */
    protected boolean isSkippable()
        {
        if (m_fHasIdentity)
            {
            return false;
            }

        Complex complex = m_complex;
        return complex != null && complex.isSparse();
        }

    /**
    * Determine if the value encoding can be compressed by combining type and
    * value information in such a way that type information could be lost.
    *
    * @return true iff values can be encoded without type information
    */
    protected boolean isCompressable()
        {
        return !m_fHasIdentity;
        }

    /**
    * Called for each and every value going into the POF stream, in case the
    * value needs its position to be encoded into the stream.
    *
    * @param iPos  the position (property index, array index, etc.)
    */
    protected void encodePosition(int iPos)
        {
        Complex complex = m_complex;
        if (complex != null)
            {
            complex.onValue(iPos);

            if (iPos >= 0 && complex.isSparse())
                {
                try
                    {
                    m_out.writePackedInt(iPos);
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            }

        // once the position is encoded, the "has identity" flag is reset
        m_fHasIdentity = false;
        }

    /**
    * Determine if the type should be encoded for the current value.
    *
    * @param nTypeId  the type of the current value
    *
    * @return true if the type ID should be placed into the POF stream, and
    *         false if only the value itself should be placed into the stream
    */
    protected boolean isTypeIdEncoded(int nTypeId)
        {
        Complex complex = m_complex;

        // if the type is not being encoded, it must match the expected
        // uniform type
        assert complex == null || !complex.isUniform()
               || nTypeId == complex.getUniformType()
               || nTypeId == T_REFERENCE;

        return complex == null || !complex.isUniform();
        }


    // ----- inner class: Complex -------------------------------------------

    /**
    * A Complex object represents the current complex data structure in the
    * POF stream.
    */
    public static class Complex
            extends Base
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a Complex object for a data collection or user type.
        *
        * @param complexCurrent   the current Complex object or null
        * @param fEncodePosition  true to encode the position information
        */
        public Complex(Complex complexCurrent, boolean fEncodePosition)
            {
            m_complexOuter = complexCurrent;
            m_fSparse      = fEncodePosition;
            }

        /**
        * Construct a Complex object for a uniformly-typed data collection.
        *
        * @param complexCurrent   the current Complex object or null
        * @param fEncodePosition  true to encode the position information
        * @param nUniformTypeId   the type identifier of the uniform type
        */
        public Complex(Complex complexCurrent, boolean fEncodePosition,
                int nUniformTypeId)
            {
            this(complexCurrent, fEncodePosition);
            m_fUniform = true;
            m_nTypeId  = nUniformTypeId;
            }

        // ----- accessors ----------------------------------------------

        /**
        * Notify the Complex object that a value has been encountered.
        *
        * @param iPos  the position that accomponied the value
        */
        public void onValue(int iPos)
            {
            }

        /**
        * Determine if the object encoding within the Complex type is
        * uniform.
        *
        * @return true iff values within the Complex type are of a uniform
        *         type and are encoded uniformly
        */
        public boolean isUniform()
            {
            return m_fUniform;
            }

        /**
        * If the object encoding is using uniform encoding, obtain the type
        * id of the uniform type.
        *
        * @return the type id used for the uniform encoding
        */
        public int getUniformType()
            {
            return m_nTypeId;
            }

        /**
        * Determine if the position information is encoded with the values
        * of the complex type, and if the Complex type is terminated in the
        * POF stream with an illegal position (-1).
        *
        * @return true iff the complex value is a sparse type
        */
        public boolean isSparse()
            {
            return m_fSparse;
            }

        /**
        * Pop this Complex object off the stack, returning the outer Complex
        * object or null if there is none.
        *
        * @return the outer Complex object or null if there is none
        */
        public Complex pop()
            {
            return m_complexOuter;
            }

        // ----- data members -------------------------------------------

        /**
        * Whether or not the position information is encoded. This is true
        * for user type properties and array elements.
        */
        private boolean m_fSparse;

        /**
        * Whether or not values within the complex type are uniformly
        * encoded. This is expected for arrays of primitive types, for
        * example.
        */
        private boolean m_fUniform;

        /**
        * The type ID, if uniform encoding is used.
        */
        private int m_nTypeId;

        /**
        * The Complex within which this Complex exists, to support nesting.
        */
        private Complex m_complexOuter;
        }


    // ----- inner class: ComplexMap ----------------------------------------

    /**
    * A ComplexMap object represents a map data structure (with uniform keys
    * or with uniform keys and values) in the POF stream.
    */
    public static class ComplexMap
            extends Complex
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a ComplexMap object for maps with uniformly-typed keys.
        *
        * @param complexCurrent     the current Complex object or null
        * @param nUniformKeyTypeId  the type identifier of the uniform type
        */
        public ComplexMap(Complex complexCurrent, int nUniformKeyTypeId)
            {
            super(complexCurrent, false, nUniformKeyTypeId);
            }

        /**
        * Construct a ComplexMap object for maps with uniformly-typed keys
        * and values.
        *
        * @param complexCurrent     the current Complex object or null
        * @param nUniformKeyTypeId  the type identifier of the uniform type
        *                           for keys in the map
        * @param nUniformValTypeId  the type identifier of the uniform type
        *                           for values in the map
        */
        public ComplexMap(Complex complexCurrent,
                int nUniformKeyTypeId, int nUniformValTypeId)
            {
            this(complexCurrent, nUniformKeyTypeId);
            m_fUniformValue = true;
            m_nValueTypeId  = nUniformValTypeId;
            }

        // ----- accessors ----------------------------------------------

        /**
        * {@inheritDoc}
        */
        public void onValue(int iPos)
            {
            m_fKey = !m_fKey;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isUniform()
            {
            return m_fKey ? super.isUniform() : m_fUniformValue;
            }

        /**
        * {@inheritDoc}
        */
        public int getUniformType()
            {
            return m_fKey ? super.getUniformType() : m_nValueTypeId;
            }

        // ----- data members -------------------------------------------

        /**
        * Toggles between key and value processing every time the caller
        * invokes {@link #onValue}.
        */
        private boolean m_fKey;

        /**
        * Whether or not values within the map are uniformly encoded.
        */
        private boolean m_fUniformValue;

        /**
        * The value type ID, if uniform encoding is used for values.
        */
        private int m_nValueTypeId;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The BufferOutput to write to.
    */
    private WriteBuffer.BufferOutput m_out;

    /**
    * The current containing Complex value in the POF stream.
    */
    private Complex m_complex;

    /**
    * Set to true when the next value to write has been tagged with an
    * identity.
    */
    private boolean m_fHasIdentity;
    }
