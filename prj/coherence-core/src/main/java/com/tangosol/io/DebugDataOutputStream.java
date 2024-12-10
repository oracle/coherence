/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;

import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
* This is an imitation DataOutputStream class that logs the output in a
* human-readable format for debugging purposes. All output will be
*
* @author cp  2004.08.06
*/
public class DebugDataOutputStream
        extends FilterOutputStream
        implements DataOutput, OutputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a DebugDataOutputStream that will output to the specified
    * Stream object.
    *
    * @param stream  an OutputStream to write to
    */
    public DebugDataOutputStream(OutputStream stream)
        {
        super(stream);
        try
            {
            println("::DebugDataOutputStream()");
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
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
        writeByte(b);
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
        write(ab, 0, ab.length);
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
        if (cb < 0)
            {
            throw new IndexOutOfBoundsException();
            }

        int    cch = cb * 2;
        char[] ach = new char[cch];

        for (int ofch = 0; ofch < cch; ++of)
            {
            int n = ab[of] & 0xFF;
            ach[ofch++] = HEX[n >>> 4];
            ach[ofch++] = HEX[n & 0x0F];
            }

        println("byte[] 0x" + new String(ach));
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
        println("boolean " + String.valueOf(f));
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
        int n = b & 0xFF;
        println("byte 0x" + HEX[n >>> 4] + HEX[n & 0x0F]);
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
        println("short " + String.valueOf(n));
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
        println("char \'" + String.valueOf(ch) + '\'');
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
        println("int " + String.valueOf(n));
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
        println("long " + String.valueOf(l));
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
        println("float " + String.valueOf(fl));
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
        println("double " + String.valueOf(dfl));
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
        println("String as byte[] \"" + s + '\"');
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
        println("String as char[] \"" + s + '\"');
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
        println("String as UTF \"" + s + '\"');
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
        println("::flush()");

        out.flush();
        }

    /**
    * Closes this OutputStream and releases any associated system resources.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void close()
            throws IOException
        {
        println("::close()");

        try
            {
            out.flush();
            }
        catch (IOException eIgnore) {}
        out.close();
        }


    // ----- internal -------------------------------------------------------

    /**
    * Hex digits.
    */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
    * Print the passed String to the underlying stream.
    *
    * @param s  the String to print
    */
    protected void println(String s)
            throws IOException
        {
        OutputStream out = this.out;

        // print out character by character, escaping anything outside of the
        // visible ASCII range
        char[] ach = s.toCharArray();
        for (int of = 0, cch = ach.length; of < cch; ++of)
            {
            char ch = ach[of];
            switch (ch)
                {
                case '\b':
                    out.write('\\');
                    out.write('b');
                    break;
                case '\t':
                    out.write('\\');
                    out.write('t');
                    break;
                case '\n':
                    out.write('\\');
                    out.write('n');
                    break;
                case '\f':
                    out.write('\\');
                    out.write('f');
                    break;
                case '\r':
                    out.write('\\');
                    out.write('r');
                    break;
                case '\\':
                    out.write('\\');
                    out.write('\\');
                    break;
    
                case 0x00:  case 0x01: case 0x02: case 0x03:
                case 0x04:  case 0x05: case 0x06: case 0x07:
                                                  case 0x0B:
                                       case 0x0E: case 0x0F:
                case 0x10:  case 0x11: case 0x12: case 0x13:
                case 0x14:  case 0x15: case 0x16: case 0x17:
                case 0x18:  case 0x19: case 0x1A: case 0x1B:
                case 0x1C:  case 0x1D: case 0x1E: case 0x1F:
                    out.write('\\');
                    out.write('0');
                    out.write((char)(ch / 8 + '0'));
                    out.write((char)(ch % 8 + '0'));
                    break;
    
                default:
                    switch (Character.getType(ch))
                        {
                        default:
                            if (ch <= 0xFF)
                                {
                                out.write(ch);
                                break;
                                }
                            // fall through

                        case Character.CONTROL:
                        case Character.PRIVATE_USE:
                        case Character.UNASSIGNED:
                            {
                            int n = ch;
                            out.write('\\');
                            out.write('u');
                            out.write(HEX[n >> 12       ]);
                            out.write(HEX[n >>  8 & 0x0F]);
                            out.write(HEX[n >>  4 & 0x0F]);
                            out.write(HEX[n       & 0x0F]);
                            }
                            break;
                        }
                    break;
                }
            }

        // new line
        out.write('\n');
        }

    /**
    * Command line test.
    */
    public static void main(String[] asArg)
            throws Exception
        {
        DebugDataOutputStream stream = new DebugDataOutputStream(System.out);
        for (int i = 0; i <= 0xFF; i += 64)
            {
            stream.write(i);
            stream.writeChar((char) i);
            stream.writeShort((short) (i * i));
            stream.writeInt(i * i);
            stream.writeLong(i * i);
            stream.writeFloat(1.0F / i);
            stream.writeDouble(1.0 / i);
            }

        String s = "start-test \07\r\n\b\t\\\uFEFF end-test";
        stream.writeBytes(s);
        stream.writeChars(s);
        stream.writeUTF(s);
        }
    }
