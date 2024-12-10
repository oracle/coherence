/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;


/**
* This is an imitation DataOutputStream class that packs its data tighter
* using variable-length integers and supports UTF longer than 64KB.
* <p>
* <b>Warning!</b> This class is not intended to be thread-safe!
*
* @author cp  2004.09.09
*/
public class PackedDataOutputStream
        extends WrapperOutputStream
        implements DataOutput
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PackedDataOutputStream that will output to the specified
    * OutputStream object.
    *
    * @param out  an OutputStream to write to
    */
    public PackedDataOutputStream(OutputStream out)
        {
        super(out);
        }


    // ----- DataOutput methods ---------------------------------------------

    /**
    * Writes the boolean value <code>f</code>.
    *
    * @param f  the boolean to be written
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeBoolean(boolean f)
            throws IOException
        {
        getOutputStream().write(f ? 1 : 0);
        }

    /**
    * Writes the eight low-order bits of the argument <code>b</code>. The 24
    * high-order bits of <code>b</code> are ignored.
    *
    * @param b  the byte to write (passed as an integer)
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeByte(int b)
            throws IOException
        {
        getOutputStream().write(b);
        }

    /**
    * Writes a short value, comprised of the 16 low-order bits of the
    * argument <code>n</code>; the 16 high-order bits of <code>n</code> are
    * ignored.
    *
    * @param n  the short to write (passed as an integer)
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeShort(int n)
            throws IOException
        {
        writeInt((short) n);
        }

    /**
    * Writes a char value, comprised of the 16 low-order bits of the
    * argument <code>ch</code>; the 16 high-order bits of <code>ch</code> are
    * ignored.
    *
    * @param ch  the char to write (passed as an integer)
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeChar(int ch)
            throws IOException
        {
        OutputStream out = getOutputStream();
        if (ch >= 0x0001 && ch <= 0x007F)
            {
            // 1-byte format:  0xxx xxxx
            out.write(ch);
            }
        else if (ch <= 0x07FF)
            {
            // 2-byte format:  110x xxxx, 10xx xxxx
            byte[] ab = m_abBuf;
            ab[0] = (byte) (0xC0 | ((ch >>> 6) & 0x1F));
            ab[1] = (byte) (0x80 | ((ch      ) & 0x3F));
            out.write(ab, 0, 2);
            }
        else
            {
            // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
            byte[] ab = m_abBuf;
            ab[0] = (byte) (0xE0 | ((ch >>> 12) & 0x0F));
            ab[1] = (byte) (0x80 | ((ch >>>  6) & 0x3F));
            ab[2] = (byte) (0x80 | ((ch       ) & 0x3F));
            out.write(ab, 0, 3);
            }
        }

    /**
    * Writes an int value.
    *
    * @param n  the int to write
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeInt(int n)
            throws IOException
        {
        // build the int image in the buffer
        byte[] ab = m_abBuf;
        int    cb = 0;

        // first byte contains sign bit (bit 7 set if neg)
        int b = 0;
        if (n < 0)
            {
            b = 0x40;
            n = ~n;
            }

        // first byte contains only 6 data bits
        b |= (byte) (n & 0x3F);
        n >>>= 6;

        while (n != 0)
            {
            b |= 0x80;          // bit 8 is a continuation bit
            ab[cb++] = (byte) b;

            b = (n & 0x7F);
            n >>>= 7;
            }

        if (cb == 0)
            {
            getOutputStream().write(b);
            }
        else
            {
            // remaining byte
            ab[cb++] = (byte) b;

            getOutputStream().write(ab, 0, cb);
            }
        }

    /**
    * Writes a long value.
    *
    * @param l  the long to write
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeLong(long l)
            throws IOException
        {
        // build the long image in the buffer
        byte[] ab = m_abBuf;
        int    cb = 0;

        // first byte contains sign bit (bit 7 set if neg)
        int b = 0;
        if (l < 0)
            {
            b = 0x40;
            l = ~l;
            }

        // first byte contains only 6 data bits
        b |= (byte) (((int) l) & 0x3F);
        l >>>= 6;

        while (l != 0)
            {
            b |= 0x80;          // bit 8 is a continuation bit
            ab[cb++] = (byte) b;

            b = (((int) l) & 0x7F);
            l >>>= 7;
            }

        if (cb == 0)
            {
            getOutputStream().write(b);
            }
        else
            {
            // remaining byte
            ab[cb++] = (byte) b;

            getOutputStream().write(ab, 0, cb);
            }
        }

    /**
    * Writes a float value.
    *
    * @param fl  the float to write
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeFloat(float fl)
            throws IOException
        {
        byte[] ab = m_abBuf;
        int n = Float.floatToIntBits(fl);
        ab[0] = (byte)(n >>> 24);
        ab[1] = (byte)(n >>> 16);
        ab[2] = (byte)(n >>> 8);
        ab[3] = (byte)(n);
        getOutputStream().write(ab, 0, 4);
        }

    /**
    * Writes a double value.
    *
    * @param dfl  the double to write
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeDouble(double dfl)
            throws IOException
        {
        byte[] ab = m_abBuf;
        long   l  = Double.doubleToLongBits(dfl);
        int n = (int) (l >>> 32);
        ab[0] = (byte)(n >>> 24);
        ab[1] = (byte)(n >>> 16);
        ab[2] = (byte)(n >>> 8);
        ab[3] = (byte)(n);
        n     = (int) l;
        ab[4] = (byte)(n >>> 24);
        ab[5] = (byte)(n >>> 16);
        ab[6] = (byte)(n >>>  8);
        ab[7] = (byte)(n);
        getOutputStream().write(ab, 0, 8);
        }

    /**
    * Writes the String <code>s</code>, but only the low-order byte from each
    * character of the String is written.
    *
    * @param s  the String to write
    *
    * @exception IOException  if an I/O error occurs
    * @exception NullPointerException  if <code>s</code> is <code>null</code>
    */
    public void writeBytes(String s)
            throws IOException
        {
        int    cb = s.length();
        byte[] ab = new byte[cb];
        s.getBytes(0, cb, ab, 0);
        write(ab);
        }

    /**
    * Writes the String <code>s</code> as a sequence of characters.
    *
    * @param s  the String to write
    *
    * @exception IOException  if an I/O error occurs
    * @exception NullPointerException  if <code>s</code> is <code>null</code>
    */
    public void writeChars(String s)
            throws IOException
        {
        char[] ach = s.toCharArray();
        for (int of = 0, cch = ach.length; of < cch; ++of)
            {
            writeChar(ach[of]);
            }
        }

    /**
    * Writes the String <code>s</code> as a sequence of characters, but using
    * UTF-8 encoding for the characters, and including the String length data
    * so that the corresponding {@link java.io.DataInput#readUTF} method can
    * reconstitute a String from the written data.
    *
    * @param s  the String to write
    *
    * @exception IOException  if an I/O error occurs
    * @exception NullPointerException  if <code>s</code> is <code>null</code>
    */
    public void writeUTF(String s)
            throws IOException
        {
        // get the chars
        char[] ach = s.toCharArray();
        int    cch = ach.length;
        writeInt(cch);

        if (cch > 0)
            {
            // figure out how many bytes it will use to hold those chars
            int cb = cch;
            for (int of = 0; of < cch; ++of)
                {
                int ch = ach[of];
                if (ch <= 0x007F)
                    {
                    // all bytes in this range use the 1-byte format except
                    // for 0, which uses a 2-byte format (so that none of the
                    // bytes in the UTF stream have a value of 0x00)
                    if (ch == 0)
                        {
                        ++cb;
                        }
                    }
                else
                    {
                    // either a 2-byte format up to 0x07FF
                    // or a 3-byte format if over 0x07FF
                    cb += (ch <= 0x07FF ? 1 : 2);
                    }
                }
            writeInt(cb);

            byte[] ab = (cb <= MAX_BUF ? m_abBuf : new byte[cb]);
            for (int of = 0, ofb = 0; of < cch; ++of)
                {
                int ch = ach[of];
                if (ch >= 0x0001 && ch <= 0x007F)
                    {
                    // 1-byte format:  0xxx xxxx
                    ab[ofb++] = (byte) ch;
                    }
                else if (ch <= 0x07FF)
                    {
                    // 2-byte format:  110x xxxx, 10xx xxxx
                    ab[ofb++] = (byte) (0xC0 | ((ch >>> 6) & 0x1F));
                    ab[ofb++] = (byte) (0x80 | ((ch      ) & 0x3F));
                    }
                else
                    {
                    // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                    ab[ofb++] = (byte) (0xE0 | ((ch >>> 12) & 0x0F));
                    ab[ofb++] = (byte) (0x80 | ((ch >>>  6) & 0x3F));
                    ab[ofb++] = (byte) (0x80 | ((ch       ) & 0x3F));
                    }
                }

            getOutputStream().write(ab, 0, cb);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The size of the internal buffer.
    */
    static final int MAX_BUF = 32;

    /**
    * An internal buffer to use for building the data to write.
    */
    private byte[] m_abBuf = new byte[MAX_BUF];
    }
