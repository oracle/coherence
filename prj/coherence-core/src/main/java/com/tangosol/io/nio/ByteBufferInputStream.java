/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.io.InputStreaming;

import java.io.InputStream;
import java.io.IOException;

import java.nio.ByteBuffer;


/**
* An InputStream implementation on top of a Java NIO ByteBuffer.
*
* @author cp  2002.09.06
*
* @since Coherence 2.2
*/
public class ByteBufferInputStream
        extends InputStream
        implements InputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ByteBufferInputStream on a ByteBuffer object.
    *
    * @param buffer  the ByteBuffer to read the data from
    */
    public ByteBufferInputStream(ByteBuffer buffer)
        {
        m_buf = buffer;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the ByteBuffer that this InputStream is based on.
    *
    * @return the underlying ByteBuffer
    */
    public ByteBuffer getByteBuffer()
        {
        return m_buf;
        }


    // ----- InputStream implementation -------------------------------------

    /**
    * Reads the next byte of data from the input stream. The value byte is
    * returned as an <code>int</code> in the range <code>0</code> to
    * <code>255</code>. If no byte is available because the end of the stream
    * has been reached, the value <code>-1</code> is returned. This method
    * blocks until input data is available, the end of the stream is detected,
    * or an exception is thrown.
    *
    * @return     the next byte of data, or <code>-1</code> if the end of the
    *             stream is reached
    *
    * @exception  IOException  if an I/O error occurs
    */
    public int read()
            throws IOException
        {
        ByteBuffer buf = getByteBuffer();
        try
            {
            return buf.hasRemaining() ? (((int) buf.get()) & 0xFF) : -1;
            }
        catch (NullPointerException e)
            {
            throw potentialStreamClosedException(e);
            }
        }

    /**
    * Reads up to <code>len</code> bytes of data from the input stream into
    * an array of bytes.  An attempt is made to read as many as
    * <code>len</code> bytes, but a smaller number may be read, possibly
    * zero. The number of bytes actually read is returned as an integer.
    *
    * @param abDest  the buffer into which the data is read
    * @param ofDest  the start offset in array <code>b</code>
    *                at which the data is written
    * @param cbDest  the maximum number of bytes to read
    *
    * @return the total number of bytes read into the buffer, or
    *         <code>-1</code> if there is no more data because the end of
    *         the stream has been reached.
    */
    public int read(byte abDest[], int ofDest, int cbDest)
            throws IOException
        {
        if (abDest == null || ofDest < 0 || cbDest < 0
                || ofDest + cbDest > abDest.length)
            {
            if (abDest == null)
                {
                throw new IllegalArgumentException("null byte array");
                }
            else
                {
                throw new IllegalArgumentException(
                        "abDest.length=" + abDest.length +
                        ", ofDest=" + ofDest + ", cbDest=" + cbDest);
                }
            }

        int cbMaxSrc = available(); // note: also checks if stream is closed
        if (cbDest > cbMaxSrc)
            {
            if (cbMaxSrc == 0)
                {
                return -1;
                }

            cbDest = cbMaxSrc;
            }

        getByteBuffer().get(abDest, ofDest, cbDest);
        return cbDest;
        }

    /**
    * Skips over and discards <code>n</code> bytes of data from this input
    * stream. The <code>skip</code> method may, for a variety of reasons, end
    * up skipping over some smaller number of bytes, possibly <code>0</code>.
    * This may result from any of a number of conditions; reaching end of file
    * before <code>n</code> bytes have been skipped is only one possibility.
    * The actual number of bytes skipped is returned.  If <code>n</code> is
    * negative, no bytes are skipped.
    *
    * @param lcb  the number of bytes to be skipped
    *
    * @return the actual number of bytes skipped
    *
    * @exception IOException  if an I/O error occurs
    */
    public long skip(long lcb)
            throws IOException
        {
        int cb;
        if (lcb > (long) Integer.MAX_VALUE)
            {
            cb = Integer.MAX_VALUE;
            }
        else if (lcb < 0)
            {
            cb = 0;
            }
        else
            {
            cb = (int) lcb;
            }

        cb = Math.min(cb, available()); // note: also checks if stream is closed

        ByteBuffer buffer = getByteBuffer();
        int        of     = buffer.position();
        buffer.position(of + cb);

        return (long) cb;
        }

    /**
    * Returns the number of bytes that can be read (or skipped over) from
    * this input stream without blocking by the next caller of a method for
    * this input stream.  The next caller might be the same thread or or
    * another thread.
    *
    * @return     the number of bytes that can be read from this input stream
    *             without blocking.
    */
    public int available()
            throws IOException
        {
        try
            {
            return getByteBuffer().remaining();
            }
        catch (NullPointerException e)
            {
            throw potentialStreamClosedException(e);
            }
        }

    /**
    * Marks the current position in this input stream. A subsequent call to
    * the <code>reset</code> method repositions this stream at the last
    * marked position so that subsequent reads re-read the same bytes.
    *
    * @param readlimit  the maximum limit of bytes that can be read before
    *                   the mark position becomes invalid
    */
    public void mark(int readlimit)
        {
        try
            {
            getByteBuffer().mark();
            }
        catch (NullPointerException e)
            {
            }
        }

    /**
    * Repositions this stream to the position at the time the
    * <code>mark</code> method was last called on this input stream.
    *
    * @exception  IOException  if an I/O error occurs.
    */
    public void reset()
            throws IOException
        {
        try
            {
            getByteBuffer().reset();
            }
        catch (NullPointerException e)
            {
            throw potentialStreamClosedException(e);
            }
        }

    /**
    * Tests if this input stream supports the <code>mark</code> and
    * <code>reset</code> methods. The <code>markSupported</code> method
    * of <code>InputStream</code> returns <code>false</code>.
    *
    * @return  <code>true</code> if this true type supports the mark and
    *          reset method; <code>false</code> otherwise
    */
    public boolean markSupported()
        {
        return true;
        }

    /**
    * Close the stream, flushing any accumulated bytes.  The underlying
    * buffer is not closed.
    *
    * @exception  IOException  if an I/O error occurs.
    */
    public void close()
            throws IOException
        {
        m_buf = null;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Check if an NPE is caused by the stream being closed. Either throws
    * an IO exception if the stream is closed or throws the original NPE.
    *
    * @param e  an NPE
    *
    * @return this method never returns normally but is designed so that the
    *         developer can write "throw potentialStreamClosedException(e)"
    *         so that the compiler knows that an exception is thrown at that
    *         point in the code
    *
    * @throws IOException if the stream is closed
    */
    protected NullPointerException potentialStreamClosedException(NullPointerException e)
            throws IOException
        {
        if (getByteBuffer() == null)
            {
            throw new IOException("stream closed");
            }
        throw e;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The ByteBuffer object from which data is read.
    */
    protected ByteBuffer m_buf;
    }
