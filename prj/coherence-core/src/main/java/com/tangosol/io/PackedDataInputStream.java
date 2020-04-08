/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;

import java.io.DataInput;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;


/**
* This is an imitation DataInputStream class that reads from streams that
* were produced by a corresponding {@link PackedDataOutputStream}.
*
* @author cp  2004.09.09
*/
public class PackedDataInputStream
        extends WrapperInputStream
        implements DataInput
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperDataInputStream that will read from the specified
    * InputStream object.
    *
    * @param in  an InputStream to read from
    */
    public PackedDataInputStream(InputStream in)
        {
        super(in);
        }


    // ----- DataInput methods ----------------------------------------------

    /**
    * Read <code>ab.length</code> bytes and store them in <code>ab</code>.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @param ab  the array to store the bytes which are read from the stream
    *
    * @exception NullPointerException  if the passed array is null
    * @exception EOFException  if the stream is exhausted before the number
    *            of bytes indicated by the array length could be read
    * @exception IOException  if an I/O error occurs
    */
    public void readFully(byte ab[])
            throws IOException
        {
        readFully(ab, 0, ab.length);
        }

    /**
    * Read <code>cb</code> bytes and store them in <code>ab</code> starting
    * at offset <code>of</code>.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @param ab  the array to store the bytes which are read from the stream
    * @param of  the offset into the array that the read bytes will be stored
    * @param cb  the maximum number of bytes to read
    *
    * @exception NullPointerException  if the passed array is null
    * @exception IndexOutOfBoundsException  if <code>of</code> or
    *            <code>cb</code> is negative, or <code>of+cb</code> is
    *            greater than the length of the <code>ab</code>
    * @exception EOFException  if the stream is exhausted before the number
    *            of bytes indicated by the array length could be read
    * @exception IOException  if an I/O error occurs
    */
    public void readFully(byte ab[], int of, int cb)
            throws IOException
        {
        if (of < 0 || cb < 0 || of + cb > ab.length)
            {
            throw new IndexOutOfBoundsException("ab.length=" + ab.length
                    + ", of=" + of + ", cb=" + cb);
            }

        while (cb > 0)
            {
            int cbRead = read(ab, of, cb);
            if (cbRead < 0)
                {
                throw new EOFException(cb + " bytes remaining to be read");
                }

            of += cbRead;
            cb -= cbRead;
            }
        }

    /**
    * Skips over up to the specified number of bytes of data. The number of
    * bytes actually skipped over may be fewer than the number specified to
    * skip, and may even be zero; this can be caused by an end-of-file
    * condition, but can also occur even when there is data remaining to
    * be read. As a result, the caller should check the return value from
    * this method, which indicates the actual number of bytes skipped.
    *
    * @param cb  the maximum number of bytes to skip over
    *
    * @return  the actual number of bytes that were skipped over
    *
    * @exception IOException  if an I/O error occurs
    */
    public int skipBytes(int cb)
            throws IOException
        {
        return (int) skip(cb);
        }

    /**
    * Read a boolean value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeBoolean} method.
    *
    * @return either <code>true</code> or <code>false</code>
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public boolean readBoolean()
            throws IOException
        {
        return readUnsignedByte() != 0;
        }

    /**
    * Read a byte value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeByte} method.
    *
    * @return a <code>byte</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public byte readByte()
            throws IOException
        {
        int n = read();
        if (n < 0)
            {
            throw new EOFException();
            }
        return (byte) n;
        }

    /**
    * Read an unsigned byte value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeByte} method when it is used with
    * unsigned 8-bit values.
    *
    * @return an <code>int</code> value in the range 0x00 to 0xFF
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public int readUnsignedByte()
            throws IOException
        {
        int n = read();
        if (n < 0)
            {
            throw new EOFException();
            }
        return n;
        }

    /**
    * Read a short value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeShort} method.
    *
    * @return a <code>short</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public short readShort()
            throws IOException
        {
        return (short) readInt();
        }

    /**
    * Read an unsigned short value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeShort} method when it is used with
    * unsigned 16-bit values.
    *
    * @return an <code>int</code> value in the range of 0x0000 to 0xFFFF
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public int readUnsignedShort()
            throws IOException
        {
        return readInt() & 0xFFFF;
        }

    /**
    * Read a char value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeChar} method.
    *
    * @return a <code>char</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public char readChar()
            throws IOException
        {
        int ch = readUnsignedByte();
        switch ((ch & 0xF0) >>> 4)
            {
            case 0x0: case 0x1: case 0x2: case 0x3:
            case 0x4: case 0x5: case 0x6: case 0x7:
                // 1-byte format:  0xxx xxxx
                return (char) ch;

            case 0xC: case 0xD:
                {
                // 2-byte format:  110x xxxx, 10xx xxxx
                int ch2 = readUnsignedByte();
                if ((ch2 & 0xC0) != 0x80)
                    {
                    throw new UTFDataFormatException(); 
                    }
                return (char) (((ch & 0x1F) << 6) | ch2 & 0x3F);
                }

            case 0xE:
                {
                // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                int ch2 = readUnsignedByte();
                int ch3 = readUnsignedByte();
                if ((ch2 & 0xC0) != 0x80 || (ch3 & 0xC0) != 0x80)
                    {
                    throw new UTFDataFormatException(); 
                    }
                return (char) (((ch & 0x0F) << 12) | ((ch2 & 0x3F) << 6) | ch3 & 0x3F);
                }

            default:
                throw new UTFDataFormatException("illegal leading UTF byte: " + ch);
            }
        }

    /**
    * Read an int value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeInt} method.
    *
    * @return an <code>int</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public int readInt()
            throws IOException
        {
        int     b     = readUnsignedByte();
        int     n     = b & 0x3F;           // only 6 bits of data in first byte
        int     cBits = 6;
        boolean fNeg  = (b & 0x40) != 0;    // seventh bit is a sign bit

        while ((b & 0x80) != 0)             // eighth bit is the continuation bit
            {
            b      = readUnsignedByte();
            n     |= ((b & 0x7F) << cBits);
            cBits += 7;
            }

        if (fNeg)
            {
            n = ~n;
            }

        return n;
        }

    /**
    * Read a long value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeLong} method.
    *
    * @return a <code>long</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public long readLong()
            throws IOException
        {
        int     b     = readUnsignedByte();
        long    l     = b & 0x3F;           // only 6 bits of data in first byte
        int     cBits = 6;
        boolean fNeg  = (b & 0x40) != 0;    // seventh bit is a sign bit

        while ((b & 0x80) != 0)             // eighth bit is the continuation bit
            {
            b      = readUnsignedByte();
            l     |= (((long) (b & 0x7F)) << cBits);
            cBits += 7;
            }

        if (fNeg)
            {
            l = ~l;
            }

        return l;
        }

    /**
    * Read a float value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeFloat} method.
    *
    * @return a <code>float</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public float readFloat()
            throws IOException
        {
        byte[] ab = m_abBuf;
        readFully(ab, 0, 4);
        int n = ((ab[0]       ) << 24)
              | ((ab[1] & 0xFF) << 16)
              | ((ab[2] & 0xFF) <<  8)
              | ((ab[3] & 0xFF));
        return Float.intBitsToFloat(n);
        }

    /**
    * Read a double value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeDouble} method.
    *
    * @return a <code>double</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public double readDouble()
            throws IOException
        {
        byte[] ab = m_abBuf;
        readFully(ab, 0, 8);
        int nMSB = ((ab[0]       ) << 24)
                 | ((ab[1] & 0xFF) << 16)
                 | ((ab[2] & 0xFF) <<  8)
                 | ((ab[3] & 0xFF));
        int nLSB = ((ab[4]       ) << 24)
                 | ((ab[5] & 0xFF) << 16)
                 | ((ab[6] & 0xFF) <<  8)
                 | ((ab[7] & 0xFF));
        long l   = (((long) nMSB) << 32) | (nLSB & 0xFFFFFFFFL);
        return Double.longBitsToDouble(l);
        }

    /**
    * Reads the next "line" of text.
    * <p>
    * This method does not have a counterpart in the
    * {@link java.io.DataOutput} interface. Furthermore, this method is
    * defined as operating on bytes and not on characters, and thus it should
    * be selected for use only after careful consideration, as if it were
    * deprecated.
    *
    * @return a line of text as a String
    * @exception  IOException  if an I/O error occurs.
    */
    public String readLine()
            throws IOException
        {
        throw new IOException("unsupported");
        }

    /**
    * Reads a String value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeUTF} method.
    *
    * @return a String value
    *
    * @exception UTFDataFormatException  if the bytes that were read were not
    *            a valid UTF-8 encoded string
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public String readUTF()
            throws IOException
        {
        int cch = readInt();
        if (cch == 0)
            {
            // this is a constant, and thus is NOT allocated dynamically
            return "";
            }

        int    cb  = readInt();
        byte[] ab  = (cb <= MAX_BUF ? m_abBuf : new byte[cb]);
        readFully(ab, 0, cb);

        char[] ach = new char[cch];
        for (int of = 0, ofb = 0; of < cch; ++of)
            {
            int ch = ab[ofb++];
            switch ((ch & 0xF0) >>> 4)
                {
                case 0x0: case 0x1: case 0x2: case 0x3:
                case 0x4: case 0x5: case 0x6: case 0x7:
                    // 1-byte format:  0xxx xxxx
                    ach[of] = (char) ch;
                    break;
    
                case 0xC: case 0xD:
                    {
                    // 2-byte format:  110x xxxx, 10xx xxxx
                    int ch2 = ab[ofb++];
                    if ((ch2 & 0xC0) != 0x80)
                        {
                        throw new UTFDataFormatException(); 
                        }
                    ach[of] = (char) (((ch & 0x1F) << 6) | ch2 & 0x3F);
                    }
                    break;
    
                case 0xE:
                    {
                    // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                    int ch2 = ab[ofb++];
                    int ch3 = ab[ofb++];
                    if ((ch2 & 0xC0) != 0x80 || (ch3 & 0xC0) != 0x80)
                        {
                        throw new UTFDataFormatException(); 
                        }
                    ach[of] = (char) (((ch & 0x0F) << 12) | ((ch2 & 0x3F) << 6) | ch3 & 0x3F);
                    }
                    break;
    
                default:
                    throw new UTFDataFormatException("illegal leading UTF byte: " + ch);
                }
            }

        return new String(ach);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The size of the internal buffer.
    */
    static final int MAX_BUF = 32;

    /**
    * An internal buffer to use for reading data.
    */
    private byte[] m_abBuf = new byte[MAX_BUF];
    }
