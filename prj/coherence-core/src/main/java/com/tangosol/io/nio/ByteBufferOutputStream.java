/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.io.OutputStreaming;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;


/**
* An OutputStream implementation on top of a Java NIO ByteBuffer.
*
* @author cp  2002.09.06
*
* @since Coherence 2.2
*/
public class ByteBufferOutputStream
        extends OutputStream
        implements OutputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ByteBufferOutputStream on a ByteBuffer object.
    *
    * @param buffer  the ByteBuffer to write the data to
    */
    public ByteBufferOutputStream(ByteBuffer buffer)
        {
        m_buf = buffer;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the ByteBuffer that this OutputStream is based on.
    *
    * @return the underlying ByteBuffer
    */
    public ByteBuffer getByteBuffer()
        {
        return m_buf;
        }


    // ----- OutputStream implementation -------------------------------------

    /**
    * Writes the specified byte to this output stream.
    *
    * @param b  the <code>byte</code>
    *
    * @exception IOException  if an I/O error occurs
    */
    public void write(int b)
            throws IOException
        {
        try
            {
            getByteBuffer().put((byte) b);
            }
        catch (NullPointerException e)
            {
            throw potentialStreamClosedException(e);
            }
        catch (BufferOverflowException e)
            {
            throw new IOException("stream capacity exceeded" + includeMessage(e));
            }
        catch (ReadOnlyBufferException e)
            {
            throw new IOException("stream is read-only" + includeMessage(e));
            }
        }

    /**
    * Writes <code>len</code> bytes from the specified byte array
    * starting at offset <code>off</code> to this output stream.
    * <p>
    * If <code>b</code> is <code>null</code>, a
    * <code>NullPointerException</code> is thrown.
    * <p>
    * If <code>off</code> is negative, or <code>len</code> is negative, or
    * <code>off+len</code> is greater than the length of the array
    * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
    *
    * @param abSrc  the data
    * @param ofSrc  the start offset in the data
    * @param cbSrc  the number of bytes to write
    *
    * @exception IOException  if an I/O error occurs
    */
    public void write(byte[] abSrc, int ofSrc, int cbSrc)
            throws IOException
        {
        if (abSrc == null || ofSrc < 0 || cbSrc < 0 || ofSrc + cbSrc > abSrc.length)
            {
            throw new IllegalArgumentException(abSrc == null ? "null buffer"
                : "abSrc.length=" + abSrc.length + ", ofSrc=" + ofSrc + ", cbSrc=" + cbSrc);
            }

        try
            {
            getByteBuffer().put(abSrc, ofSrc, cbSrc);
            }
        catch (NullPointerException e)
            {
            throw potentialStreamClosedException(e);
            }
        catch (BufferOverflowException e)
            {
            throw new IOException("stream capacity exceeded" + includeMessage(e));
            }
        catch (ReadOnlyBufferException e)
            {
            throw new IOException("stream is read-only" + includeMessage(e));
            }
        }

    /**
    * Flush any accumulated bytes.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void flush()
            throws IOException
        {
        ByteBuffer buf = getByteBuffer();
        if (buf instanceof MappedByteBuffer)
            {
            try
                {
                ((MappedByteBuffer) buf).force();
                }
            catch (UnsupportedOperationException e) {}
            }
        }

    /**
    * Close the stream, flushing any accumulated bytes.  The underlying
    * buffer is not closed.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void close()
            throws IOException
        {
        flush();
        m_buf = null;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Provide a "suffix" containing the exception message (if any).
    *
    * @param e  an exception (any Throwable object)
    *
    * @return either an empty string (no message) or a suitable suffix for
    *         an error message
    */
    protected static String includeMessage(Throwable e)
        {
        String s = e.getMessage();
        return s == null || s.length() == 0 ? "" : ": \"" + s + '\"';
        }

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
    * The ByteBuffer object to which data is written.
    */
    private ByteBuffer m_buf;
    }
