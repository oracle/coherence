/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.IOException;
import java.io.OutputStream;


/**
* This is an OutputStream class that delegates to another OutputStream.
* Primarily, this is intended as a base class for building specific-purpose
* OutputStream wrappers.
*
* @author cp  2004.08.20
*/
public class WrapperOutputStream
        extends OutputStream
        implements OutputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an uninitialized WrapperOutputStream.
    */
    public WrapperOutputStream()
        {
        }
    
    /**
    * Construct a WrapperOutputStream that will output to the specified
    * OutputStream object.
    *
    * @param out  an OutputStream object to write to
    */
    public WrapperOutputStream(OutputStream out)
        {
        setOutputStream(out);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying OutputStream.
    *
    * @return the underlying OutputStream
    */
    public OutputStream getOutputStream()
        {
        return m_out;
        }

    /**
    * Return the underlying OutputStream.
    *
    * @return the underlying OutputStream
    *
    * @throws IllegalStateException if the underlying stream has not been
    *                               specified.
    */
    protected OutputStream ensureOutputStream()
        {
        OutputStream stream = m_out;
        if (stream == null)
            {
            throw new IllegalStateException
                    ("Uninitialized WrapperOutputStream");
            }
        return stream;
        }

    /**
    * Specify the underlying OutputStream. This method may only be called once
    * with a non-null value.
    *
    * @param out  the stream to be wrapped
    *
    *
    * @throws IllegalStateException if the underlying stream has already been
    *                               specified.
    */
    public void setOutputStream(OutputStream out)
        {
        if (m_out == null)
            {
            m_out = out;
            }
        else
            {
            throw new IllegalStateException("OutputStream already specified");
            }
        }

    // ----- OutputStream methods -------------------------------------------

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
        ensureOutputStream().write(b);
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
        ensureOutputStream().write(ab);
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
        ensureOutputStream().write(ab, of, cb);
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
        ensureOutputStream().flush();
        }

    /**
    * Closes this OutputStream and releases any associated system resources.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void close()
            throws IOException
        {
        ensureOutputStream().close();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying OutputStream object to use.
    */
    protected OutputStream m_out;
    }
