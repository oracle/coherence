/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;

import java.io.IOException;


/**
* This is the interface represented by the Java InputStream class.
*
* @author cp  2005.01.18
*/
public interface InputStreaming
    {
    // ----- InputStream methods --------------------------------------------

    /**
    * Read the next byte of data from the InputStream. The value byte is
    * returned as an <code>int</code> in the range <code>0</code> to
    * <code>255</code>. If the end of the stream has been reached, the value
    * <code>-1</code> is returned.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @return  the next byte of data, or <code>-1</code> if the end of the
    *          stream has been reached
    *
    * @exception java.io.IOException  if an I/O error occurs
    */
    public int read()
            throws IOException;

    /**
    * Read some number of bytes from the input stream and store them into the
    * passed array <code>ab</code>. The number of bytes actually read is
    * returned.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @param ab  the array to store the bytes which are read from the stream
    *
    * @return  the number of bytes read from the stream, or <code>-1</code>
    *          if no bytes were read from the stream because the end of the
    *          stream had been reached
    *
    * @exception NullPointerException  if the passed array is null
    * @exception IOException  if an I/O error occurs
    */
    public int read(byte ab[])
            throws IOException;

    /**
    * Read up to <code>cb</code> bytes from the input stream and store them
    * into the passed array <code>ab</code> starting at offset
    * <code>of</code>. The number of bytes actually read is returned.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @param ab  the array to store the bytes which are read from the stream
    * @param of  the offset into the array that the read bytes will be stored
    * @param cb  the maximum number of bytes to read
    *
    * @return  the number of bytes read from the stream, or <code>-1</code>
    *          if no bytes were read from the stream because the end of the
    *          stream had been reached
    *
    * @exception NullPointerException  if the passed array is null
    * @exception IndexOutOfBoundsException  if <code>of</code> or
    *            <code>cb</code> is negative, or <code>of+cb</code> is
    *            greater than the length of the <code>ab</code>
    * @exception IOException  if an I/O error occurs
    */
    public int read(byte ab[], int of, int cb)
            throws IOException;

    /**
    * Skips over up to the specified number of bytes of data from this
    * InputStream. The number of bytes actually skipped over may be fewer
    * than the number specified to skip, and may even be zero; this can be
    * caused by an end-of-file condition, but can also occur even when there
    * is data remaining in the InputStream. As a result, the caller should
    * check the return value from this method, which indicates the actual
    * number of bytes skipped.
    *
    * @param cb  the maximum number of bytes to skip over
    *
    * @return  the actual number of bytes that were skipped over
    *
    * @exception IOException  if an I/O error occurs
    */
    public long skip(long cb)
            throws IOException;

    /**
    * Returns the number of bytes that can be read (or skipped over) from
    * this input stream without causing a blocking I/O condition to occur.
    * This method reflects the assumed implementation of various buffering
    * InputStreams, which can guarantee non-blocking reads up to the extent
    * of their buffers, but beyond that the read operations will have to read
    * from some underlying (and potentially blocking) source.
    *
    * @return  the number of bytes that can be read from this InputStream
    *          without blocking
    *
    * @exception IOException  if an I/O error occurs
    */
    public int available()
            throws IOException;

    /**
    * Close the InputStream and release any system resources associated with
    * it.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void close()
            throws IOException;

    /**
    * Marks the current read position in the InputStream in order to support
    * the stream to be later "rewound" (using the {@link #reset} method) to
    * the current position. The caller passes in the maximum number of bytes
    * that it expects to read before calling the {@link #reset} method, thus
    * indicating the upper bounds of the responsibility of the stream to be
    * able to buffer what it has read in order to support this functionality.
    *
    * @param cbReadLimit  the maximum number of bytes that caller expects the
    *                     InputStream to be able to read before the mark
    *                     position becomes invalid
    */
    public void mark(int cbReadLimit);

    /**
    * Rewind this stream to the position at the time the {@link #mark} method
    * was last called on this InputStream. If the InputStream cannot fulfill
    * this contract, it should throw an IOException.
    *
    * @exception IOException  if an I/O error occurs, for example if this
    *                         has not been marked or if the mark has been
    *                         invalidated
    */
    public void reset()
            throws IOException;

    /**
    * Determine if this InputStream supports the {@link #mark} and
    * {@link #reset} methods.
    *
    * @return  <code>true</code> if this InputStream supports the mark and
    *          reset method; <code>false</code> otherwise
    */
    public boolean markSupported();
    }
