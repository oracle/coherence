/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.OutputStreaming;

import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;


/**
* An ObjectOutput implementation suitable for writing Externalizable and
* ExternalizableLite objects to a POF stream, although without support for
* schema evolution and other advanced POF features.
*
* @author cp  2006.07.29
*/
public class PofOutputStream
        extends OutputStream
        implements OutputStreaming, DataOutput, ObjectOutput
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PofOutputStream that will write its information to an
    * underlying {@link PofWriter PofWriter}.
    *
    * @param out  the {@link PofWriter PofWriter} to write to
    */
    public PofOutputStream(PofWriter out)
        {
        m_out = out;
        }


    // ----- OutputStreaming interface --------------------------------------

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
        m_out.writeByte(nextIndex(), (byte) b);
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
        m_out.writeByteArray(nextIndex(), ab);
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
        if (of > 0 || cb < ab.length)
            {
            byte[] abOrig = ab;
            ab = new byte[cb];
            System.arraycopy(abOrig, of, ab, 0, cb);
            }
        m_out.writeByteArray(nextIndex(), ab);
        }

    /**
    * Flushes this OutputStream and forces any buffered output bytes to be
    * written.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void flush()
            throws IOException
        {
        }

    /**
    * Closes this OutputStream and releases any associated system resources.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void close()
            throws IOException
        {
        }


    // ----- DataOutput interface -------------------------------------------

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
        m_out.writeBoolean(nextIndex(), f);
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
        m_out.writeByte(nextIndex(), (byte) b);
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
        m_out.writeShort(nextIndex(), (short) n);
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
        m_out.writeChar(nextIndex(), (char) ch);
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
        m_out.writeInt(nextIndex(), n);
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
        m_out.writeLong(nextIndex(), l);
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
        m_out.writeFloat(nextIndex(), fl);
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
        m_out.writeDouble(nextIndex(), dfl);
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
        m_out.writeByteArray(nextIndex(), ab);
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
        m_out.writeCharArray(nextIndex(), s.toCharArray());
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
        m_out.writeString(nextIndex(), s);
        }


    // ----- ObjectOutput interface -----------------------------------------

    /**
    * Writes the Object <code>o</code> so that the corresponding
    * {@link java.io.ObjectInput#readObject} method can reconstitute an
    * Object from the written data.
    *
    * @param o  the Object to write
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeObject(Object o)
            throws IOException
        {
        m_out.writeObject(nextIndex(), o);
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Obtain the underlying PofWriter.
    *
    * @return the PofWriter
    */
    public PofWriter getPofWriter()
        {
        return m_out;
        }

    /**
    * Determine the next property index to write to.
    *
    * @return the next property index to write to
    */
    public int nextIndex()
        {
        return m_nProp++;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying PofWriter.
    */
    private PofWriter m_out;

    /**
    * The next property index.
    */
    private int m_nProp;
    }
