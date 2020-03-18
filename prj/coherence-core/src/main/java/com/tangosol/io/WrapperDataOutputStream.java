/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;


/**
* This is an imitation DataOutputStream class that provides the DataOutput
* interface by delegating to an object that implements the DataOutput
* interface. Primarily, this is intended as a base class for building
* specific-purpose DataOutput wrappers.
*
* @author cp  2004.08.20
*/
public class WrapperDataOutputStream
        extends OutputStream
        implements DataOutput, OutputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperDataOutputStream that will output to the specified
    * object implementing the DataOutput interface.
    *
    * @param out  an object implementing DataOutput to write to
    */
    public WrapperDataOutputStream(DataOutput out)
        {
        m_out = out;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying object providing the DataOutput interface that
    * this object is delegating to.
    *
    * @return the underlying DataOutput
    */
    public DataOutput getDataOutput()
        {
        return m_out;
        }

    /**
    * Return the total number of bytes written to the wrapped DataOutput object.
    *
    * @return the total number of bytes written
    */
    public long getBytesWritten()
        {
        return m_cbWritten;
        }

    /**
    * Increment the count of total number of bytes written to the wrapped
    * DataOutput object by the specified number of bytes.
    * <p>
    * If the count has reached Long.MAX_VALUE, the count is not incremented.
    *
    * @param cb the number of bytes to increment the count by
    */
    protected void incBytesWritten(int cb)
        {
        long cbWritten = m_cbWritten;
        long cbTotal   = cbWritten + cb;

        if (cbTotal >= 0)
            {
            m_cbWritten = cbTotal;
            }
        else if (cbWritten != Long.MAX_VALUE)
            {
            m_cbWritten = Long.MAX_VALUE;
            }
        }


    // ----- DataOutput methods ---------------------------------------------

    /**
    * Writes the eight low-order bits of the argument <code>b</code>. The 24
    * high-order bits of <code>b</code> are ignored.
    *
    * @param b  the byte to write (passed as an integer)
    *
    * @exception IOException  if an I/O error occurs
    */
    public void write(int b)
            throws IOException
        {
        m_out.write(b);
        incBytesWritten(1);
        }

    /**
    * Writes all the bytes in the array <code>ab</code>.
    *
    * @param ab  the byte array to write
    *
    * @exception IOException  if an I/O error occurs
    * @exception NullPointerException  if <code>ab</code> is
    *            <code>null</code>
    */
    public void write(byte ab[])
            throws IOException
        {
        m_out.write(ab);
        incBytesWritten(ab.length);
        }

    /**
    * Writes <code>cb</code> bytes starting at offset <code>of</code> from
    * the array <code>ab</code>.
    *
    * @param ab  the byte array to write from
    * @param of  the offset into <code>ab</code> to start writing from
    * @param cb  the number of bytes from <code>ab</code> to write
    *
    * @exception IOException  if an I/O error occurs
    * @exception NullPointerException  if <code>ab</code> is
    *            <code>null</code>
    * @exception IndexOutOfBoundsException  if <code>of</code> is negative,
    *            or <code>cb</code> is negative, or <code>of+cb</code> is
    *            greater than <code>ab.length</code>
    */
    public void write(byte ab[], int of, int cb)
            throws IOException
        {
        m_out.write(ab, of, cb);
        incBytesWritten(cb);
        }

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
        m_out.writeBoolean(f);
        incBytesWritten(1);
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
        m_out.writeByte(b);
        incBytesWritten(1);
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
        m_out.writeShort(n);
        incBytesWritten(2);
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
        m_out.writeChar(ch);
        incBytesWritten(2);
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
        m_out.writeInt(n);
        incBytesWritten(4);
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
        m_out.writeLong(l);
        incBytesWritten(8);
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
        m_out.writeFloat(fl);
        incBytesWritten(4);
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
        m_out.writeDouble(dfl);
        incBytesWritten(8);
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
        m_out.writeBytes(s);
        incBytesWritten(s.length());
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
        m_out.writeChars(s);
        incBytesWritten(s.length() * 2);
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
        m_out.writeUTF(s);
        }


    // ----- OutputStream methods -------------------------------------------

    /**
    * Flushes this OutputStream and forces any buffered output bytes to be
    * written.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void flush()
            throws IOException
        {
        DataOutput out = m_out;
        if (out instanceof OutputStreaming)
            {
            ((OutputStreaming) out).flush();
            }
        else if (out instanceof OutputStream)
            {
            ((OutputStream) out).flush();
            }
        else if (out instanceof ObjectOutput)
            {
            ((ObjectOutput) out).flush();
            }
        }

    /**
    * Closes this OutputStream and releases any associated system resources.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void close()
            throws IOException
        {
        DataOutput out = m_out;
        if (out instanceof OutputStreaming)
            {
            ((OutputStreaming) out).close();
            }
        else if (out instanceof OutputStream)
            {
            ((OutputStream) out).close();
            }
        else if (out instanceof ObjectOutput)
            {
            ((ObjectOutput) out).close();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying DataOutput object to use.
    */
    private DataOutput m_out;

    /**
    * The total number of bytes written to the wrapped DataOutput object.
    */
    private long m_cbWritten;
    }
