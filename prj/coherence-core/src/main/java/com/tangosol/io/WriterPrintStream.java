/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.NullImplementation;

import java.io.PrintStream;
import java.io.PrintWriter;

import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;


/**
* Overrides PrintStream to delegate to a PrintWriter.
*
* @author cp  2000.11.01
*/
public class WriterPrintStream
        extends PrintStream
        implements OutputStreaming
    {
    // ------ constructors --------------------------------------------------

    public WriterPrintStream(PrintWriter out)
        {
        super(NullImplementation.getOutputStream(), true);
        m_out = out;
        }


    // ----- PrintStream methods --------------------------------------------

    /**
    * Flush the stream.  This is done by writing any buffered output bytes to
    * the underlying output stream and then flushing that stream.
    *
    * @see        java.io.OutputStream#flush()
    */
    public void flush()
        {
        m_out.flush();
        }

    /**
    * Close the stream.  This is done by flushing the stream and then closing
    * the underlying output stream.
    *
    * @see        java.io.OutputStream#close()
    */
    public void close()
        {
        m_out.close();
        }

    /**
    * Flush the stream and check its error state.  The internal error state
    * is set to <code>true</code> when the underlying output stream throws an
    * <code>IOException</code> other than <code>InterruptedIOException</code>,
    * and when the <code>setError</code> method is invoked.  If an operation
    * on the underlying output stream throws an
    * <code>InterruptedIOException</code>, then the <code>PrintStream</code>
    * converts the exception back into an interrupt by doing:
    * <pre>
    *     Thread.currentThread().interrupt();
    * </pre>
    * or the equivalent.
    *
    * @return True if and only if this stream has encountered an
    *         <code>IOException</code> other than
    *         <code>InterruptedIOException</code>, or the
    *         <code>setError</code> method has been invoked
    */
    public boolean checkError()
        {
        return m_out.checkError();
        }

    /**
    * Set the error state of the stream to <code>true</code>.
    *
    * @since JDK1.1
    */
    protected void setError()
        {
        throw new UnsupportedOperationException();
        }

    /**
    * Write the specified byte to this stream.  If the byte is a newline and
    * automatic flushing is enabled then the <code>flush</code> method will be
    * invoked.
    *
    * <p> Note that the byte is written as given; to write a character that
    * will be translated according to the platform's default character
    * encoding, use the <code>print(char)</code> or <code>println(char)</code>
    * methods.
    *
    * @param  b  The byte to be written
    * @see #print(char)
    * @see #println(char)
    */
    public void write(int b)
        {
        m_out.write(b);
        }

    /**
    * Write <code>len</code> bytes from the specified byte array starting at
    * offset <code>off</code> to this stream.  If automatic flushing is
    * enabled then the <code>flush</code> method will be invoked.
    *
    * <p> Note that the bytes will be written as given; to write characters
    * that will be translated according to the platform's default character
    * encoding, use the <code>print(char)</code> or <code>println(char)</code>
    * methods.
    *
    * @param  ab   A byte array
    * @param  of   Offset from which to start taking bytes
    * @param  cb   Number of bytes to write
    */
    public void write(byte ab[], int of, int cb)
        {
        while (of < cb)
            {
            write(ab[of++]);
            }
        }

    /**
    * Print a boolean value.  The string produced by <code>{@link
    * java.lang.String#valueOf(boolean)}</code> is translated into bytes
    * according to the platform's default character encoding, and these bytes
    * are written in exactly the manner of the
    * <code>{@link #write(int)}</code> method.
    *
    * @param      f   The <code>boolean</code> to be printed
    */
    public void print(boolean f)
        {
        m_out.print(f);
        }

    /**
    * Print a character.  The character is translated into one or more bytes
    * according to the platform's default character encoding, and these bytes
    * are written in exactly the manner of the
    * <code>{@link #write(int)}</code> method.
    *
    * @param      ch   The <code>char</code> to be printed
    */
    public void print(char ch)
        {
        m_out.print(ch);
        }

    /**
    * Print an integer.  The string produced by <code>{@link
    * java.lang.String#valueOf(int)}</code> is translated into bytes
    * according to the platform's default character encoding, and these bytes
    * are written in exactly the manner of the
    * <code>{@link #write(int)}</code> method.
    *
    * @param      i   The <code>int</code> to be printed
    * @see        Integer#toString(int)
    */
    public void print(int i)
        {
        m_out.print(i);
        }

    /**
    * Print a long integer.  The string produced by <code>{@link
    * java.lang.String#valueOf(long)}</code> is translated into bytes
    * according to the platform's default character encoding, and these bytes
    * are written in exactly the manner of the
    * <code>{@link #write(int)}</code> method.
    *
    * @param      l   The <code>long</code> to be printed
    * @see        Long#toString(long)
    */
    public void print(long l)
        {
        m_out.print(l);
        }

    /**
    * Print a floating-point number.  The string produced by <code>{@link
    * java.lang.String#valueOf(float)}</code> is translated into bytes
    * according to the platform's default character encoding, and these bytes
    * are written in exactly the manner of the
    * <code>{@link #write(int)}</code> method.
    *
    * @param      fl   The <code>float</code> to be printed
    * @see        Float#toString(float)
    */
    public void print(float fl)
        {
        m_out.print(fl);
        }

    /**
    * Print a double-precision floating-point number.  The string produced by
    * <code>{@link java.lang.String#valueOf(double)}</code> is translated into
    * bytes according to the platform's default character encoding, and these
    * bytes are written in exactly the manner of the <code>{@link
    * #write(int)}</code> method.
    *
    * @param      dfl   The <code>double</code> to be printed
    * @see        Double#toString(double)
    */
    public void print(double dfl)
        {
        m_out.print(dfl);
        }

    /**
    * Print an array of characters.  The characters are converted into bytes
    * according to the platform's default character encoding, and these bytes
    * are written in exactly the manner of the
    * <code>{@link #write(int)}</code> method.
    *
    * @param      ach   The array of chars to be printed
    * 
    * @throws  NullPointerException  If <code>s</code> is <code>null</code>
    */
    public void print(char[] ach)
        {
        m_out.print(ach);
        }

    /**
    * Print a string.  If the argument is <code>null</code> then the string
    * <code>"null"</code> is printed.  Otherwise, the string's characters are
    * converted into bytes according to the platform's default character
    * encoding, and these bytes are written in exactly the manner of the
    * <code>{@link #write(int)}</code> method.
    *
    * @param      s   The <code>String</code> to be printed
    */
    public void print(String s)
        {
        m_out.print(s);
        }

    /**
    * Print an object.  The string produced by the <code>{@link
    * java.lang.String#valueOf(Object)}</code> method is translated into bytes
    * according to the platform's default character encoding, and these bytes
    * are written in exactly the manner of the
    * <code>{@link #write(int)}</code> method.
    *
    * @param      o   The <code>Object</code> to be printed
    * @see        Object#toString()
    */
    public void print(Object o)
        {
        m_out.print(o);
        }

    /**
    * Terminate the current line by writing the line separator string.  The
    * line separator string is defined by the system property
    * <code>line.separator</code>, and is not necessarily a single newline
    * character (<code>'\n'</code>).
    */
    public void println()
        {
        m_out.println();
        }

    /**
    * Print a boolean and then terminate the line.  This method behaves as
    * though it invokes <code>{@link #print(boolean)}</code> and then
    * <code>{@link #println()}</code>.
    *
    * @param f  The <code>boolean</code> to be printed
    */
    public void println(boolean f)
        {
        m_out.println(f);
        }

    /**
    * Print a character and then terminate the line.  This method behaves as
    * though it invokes <code>{@link #print(char)}</code> and then
    * <code>{@link #println()}</code>.
    *
    * @param ch  The <code>char</code> to be printed.
    */
    public void println(char ch)
        {
        m_out.println(ch);
        }

    /**
    * Print an integer and then terminate the line.  This method behaves as
    * though it invokes <code>{@link #print(int)}</code> and then
    * <code>{@link #println()}</code>.
    *
    * @param i  The <code>int</code> to be printed.
    */
    public void println(int i)
        {
        m_out.println(i);
        }

    /**
    * Print a long and then terminate the line.  This method behaves as
    * though it invokes <code>{@link #print(long)}</code> and then
    * <code>{@link #println()}</code>.
    *
    * @param l  a The <code>long</code> to be printed.
    */
    public void println(long l)
        {
        m_out.println(l);
        }

    /**
    * Print a float and then terminate the line.  This method behaves as
    * though it invokes <code>{@link #print(float)}</code> and then
    * <code>{@link #println()}</code>.
    *
    * @param fl  The <code>float</code> to be printed.
    */
    public void println(float fl)
        {
        m_out.println(fl);
        }

    /**
    * Print a double and then terminate the line.  This method behaves as
    * though it invokes <code>{@link #print(double)}</code> and then
    * <code>{@link #println()}</code>.
    *
    * @param dfl  The <code>double</code> to be printed.
    */
    public void println(double dfl)
        {
        m_out.println(dfl);
        }

    /**
    * Print an array of characters and then terminate the line.  This method
    * behaves as though it invokes <code>{@link #print(char[])}</code> and
    * then <code>{@link #println()}</code>.
    *
    * @param ach  an array of chars to print.
    */
    public void println(char[] ach)
        {
        m_out.println(ach);
        }

    /**
    * Print a String and then terminate the line.  This method behaves as
    * though it invokes <code>{@link #print(String)}</code> and then
    * <code>{@link #println()}</code>.
    *
    * @param s  The <code>String</code> to be printed.
    */
    public void println(String s)
        {
        m_out.println(s);
        }

    /**
    * Print an Object and then terminate the line.  This method behaves as
    * though it invokes <code>{@link #print(Object)}</code> and then
    * <code>{@link #println()}</code>.
    *
    * @param o  The <code>Object</code> to be printed.
    */
    public void println(Object o)
        {
        m_out.println(o);
        }


    // ----- data members ---------------------------------------------------

    private PrintWriter m_out;
    }
