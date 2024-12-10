/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import java.io.IOException;


/**
* This is the interface represented by the Java OutputStream class.
*
* @author cp  2005.01.18
*/
public interface OutputStreaming
    {
    // ----- OutputStream methods -------------------------------------------

    /**
    * Writes the eight low-order bits of the argument <code>b</code>. The 24
    * high-order bits of <code>b</code> are ignored.
    *
    * @param b  the byte to write (passed as an integer)
    *
    * @exception java.io.IOException  if an I/O error occurs
    */
    public void write(int b)
            throws IOException;

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
            throws IOException;

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
            throws IOException;

    /**
    * Flushes this OutputStream and forces any buffered output bytes to be
    * written.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void flush()
            throws IOException;

    /**
    * Closes this OutputStream and releases any associated system resources.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void close()
            throws IOException;
    }
