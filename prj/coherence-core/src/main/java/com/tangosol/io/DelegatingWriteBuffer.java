/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Binary;

import java.io.IOException;

import java.nio.ByteBuffer;

/**
* A DelegatingWriteBuffer is a WriteBuffer that writes through to an
* underlying (or "containing") WriteBuffer. Basically, it allows a process
* that is writing to a WriteBuffer to ask for a "protected" sub-portion of
* that WriteBuffer to hand to a second process, such that the second process
* can not affect (or even read from) the WriteBuffer outside of the portion
* that the first process explicitly designated as viewable and modifiable.
* <p>
* This implementation is explicitly not thread-safe.
*
* @author cp  2005.03.24 created
*/
public final class DelegatingWriteBuffer
        extends AbstractWriteBuffer
    {
    // ----- constructors -------------------------------------------

    /**
    * Construct a DelegatingWriteBuffer that will delegate to the
    * containing WriteBuffer.
    *
    * @param buf  the containing WriteBuffer
    * @param of   the offset within the containing WriteBuffer that this
    *             WriteBuffer is starting at
    * @param cb   the maximum capacity for this WriteBuffer
    */
    public DelegatingWriteBuffer(WriteBuffer buf, int of, int cb)
        {
        m_buf     = buf;
        m_ofStart = of;
        m_cbMax   = cb;
        }

    // ----- buffer write operations ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte b)
        {
        checkBounds(ofDest, 1);
        m_buf.write(m_ofStart + ofDest, b);
        updateLength(ofDest + 1);
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte[] abSrc, int ofSrc, int cbSrc)
        {
        checkBounds(ofDest, cbSrc);
        m_buf.write(m_ofStart + ofDest, abSrc, ofSrc, cbSrc);
        updateLength(ofDest + cbSrc);
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, ReadBuffer bufSrc, int ofSrc, int cbSrc)
        {
        checkBounds(ofDest, cbSrc);
        m_buf.write(m_ofStart + ofDest, bufSrc, ofSrc, cbSrc);
        updateLength(ofDest + cbSrc);
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, InputStreaming stream)
            throws IOException
        {
        int cb = copyStream(ofDest, stream, m_cbMax - ofDest);
        updateLength(cb);
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, InputStreaming stream, int cbSrc)
            throws IOException
        {
        checkBounds(ofDest, cbSrc);
        m_buf.write(m_ofStart + ofDest, stream, cbSrc);
        updateLength(ofDest + cbSrc);
        }


    // ----- buffer maintenance ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int length()
        {
        return m_cb;
        }

    /**
    * {@inheritDoc}
    */
    public void retain(int of, int cb)
        {
        if (of < 0 || cb < 0 || of + cb > length())
            {
            throw new IndexOutOfBoundsException("of=" + of + ", cb="
                    + cb + ", length()=" + length());
            }
        else if (cb == 0)
            {
            clear();
            }
        else
            {
            // "slide" the desired bytes to the start of this buffer
            if (of > 0)
                {
                WriteBuffer buf     = m_buf;
                int         ofStart = m_ofStart;
                buf.write(ofStart, buf.getUnsafeReadBuffer(), ofStart + of, cb);
                }

            // update length
            m_cb = cb;
            }
        }

    /**
    * {@inheritDoc}
    */
    public void clear()
        {
        m_cb = 0;
        }

    /**
    * {@inheritDoc}
    */
    public int getCapacity()
        {
        // figure out the capacity by asking the delegatee for its capacity
        // and then subtracting this buffer's offset, but if that is beyond
        // our max capacity then the capacity is actually our max capacity
        return Math.min(Math.max(0, m_buf.getCapacity() - m_ofStart), m_cbMax);
        }

    /**
    * {@inheritDoc}
    */
    public int getMaximumCapacity()
        {
        return m_cbMax;
        }


    // ----- obtaining different "write views" to the buffer ----------------

    /**
    * {@inheritDoc}
    */
    public BufferOutput getBufferOutput(int of)
        {
        return new DelegatingBufferOutput(of);
        }


    // ----- accessing the buffered data ------------------------------------

    /**
    * {@inheritDoc}
    */
    public ReadBuffer getReadBuffer()
        {
        return toBinary();
        }

    /**
    * {@inheritDoc}
    */
    public ReadBuffer getUnsafeReadBuffer()
        {
        int cb = m_cb;
        return cb == 0 ? NO_BINARY
                       : m_buf.getUnsafeReadBuffer().getReadBuffer(m_ofStart, cb);
        }

    /**
    * {@inheritDoc}
    */
    public byte[] toByteArray()
        {
        int cb = m_cb;
        return cb == 0 ? NO_BYTES
                       : m_buf.getUnsafeReadBuffer().toByteArray(m_ofStart, cb);
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        int cb = m_cb;
        return cb == 0 ? NO_BINARY
                       : m_buf.getUnsafeReadBuffer().toBinary(m_ofStart, cb);
        }


    // ----- internal -------------------------------------------------------

    /**
    * Test an offset and length of data to write to see if it can be
    * written to this buffer.
    *
    * @param of  offset to write data at
    * @param cb  length in bytes of data
    */
    protected void checkBounds(int of, int cb)
        {
        if (of < 0 || cb < 0 || of + cb > m_cbMax)
            {
            throw new IndexOutOfBoundsException("of=" + of + ", cb="
                    + cb + ", max=" + m_cbMax);
            }
        }

    /**
    * Update the length if the passed length is greater than the current
    * buffer length.
    *
    * @param cb  the possible new length
    */
    protected void updateLength(int cb)
        {
        if (cb > m_cb)
            {
            m_cb = cb;
            }
        }

    /**
    * {@inheritDoc}
    */
    protected int copyStream(int ofDest, InputStreaming stream, int cbMax)
            throws IOException
        {
        WriteBuffer buf = m_buf;
        if (buf instanceof AbstractWriteBuffer)
            {
            // ask the delegatee to do the size-limited stream copy itself,
            // so that it can optimize it
            AbstractWriteBuffer awbuf = (AbstractWriteBuffer) buf;
            return awbuf.copyStream(m_ofStart + ofDest, stream, cbMax);
            }
        else
            {
            // since the delegatee doesn't know how to copy the stream in a
            // size-limited manner, we have to do the copy ourselves
            return super.copyStream(ofDest, stream, cbMax);
            }
        }


    // ----- inner interface: BufferOutput ----------------------------------

    /**
    * A BufferOutput implementation that delegates to a BufferOutput
    * implementation, except that its offset range is shifted and limited.
    *
    * @author cp  2005.03.24
    */
    public final class DelegatingBufferOutput
            extends AbstractBufferOutput
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct an AbstractBufferOutput that will begin writing at the
        * start of the containing WriteBuffer.
        *
        * @param of  the offset within the DelegatingWriteBuffer that this
        *            BufferOutput will begin writing to
        */
        public DelegatingBufferOutput(int of)
            {
            m_out = m_buf.getBufferOutput();
            setOffset(of);
            }

        // ----- OutputStreaming methods --------------------------------

        /**
        * {@inheritDoc}
        */
        public void write(int b)
                throws IOException
            {
            checkBounds(m_ofWrite, 1);
            m_out.write(b);
            moveOffset(1);
            }

        /**
        * {@inheritDoc}
        */
        public void write(byte ab[], int of, int cb)
                throws IOException
            {
            checkBounds(m_ofWrite, cb);
            m_out.write(ab, of, cb);
            moveOffset(cb);
            }

        // ----- DataOutput methods -------------------------------------

        /**
        * {@inheritDoc}
        */
        public void writeBoolean(boolean f)
                throws IOException
            {
            checkBounds(m_ofWrite, 1);
            m_out.writeBoolean(f);
            moveOffset(1);
            }

        /**
        * {@inheritDoc}
        */
        public void writeByte(int b)
                throws IOException
            {
            checkBounds(m_ofWrite, 1);
            m_out.writeByte(b);
            moveOffset(1);
            }

        /**
        * {@inheritDoc}
        */
        public void writeShort(int n)
                throws IOException
            {
            checkBounds(m_ofWrite, 2);
            m_out.writeShort(n);
            moveOffset(2);
            }

        /**
        * {@inheritDoc}
        */
        public void writeChar(int ch)
                throws IOException
            {
            checkBounds(m_ofWrite, 2);
            m_out.writeChar(2);
            moveOffset(2);
            }

        /**
        * {@inheritDoc}
        */
        public void writeInt(int n)
                throws IOException
            {
            checkBounds(m_ofWrite, 4);
            m_out.writeInt(n);
            moveOffset(4);
            }

        /**
        * {@inheritDoc}
        */
        public void writeLong(long l)
                throws IOException
            {
            checkBounds(m_ofWrite, 8);
            m_out.writeLong(l);
            moveOffset(8);
            }

        /**
        * {@inheritDoc}
        */
        public void writeFloat(float fl)
                throws IOException
            {
            checkBounds(m_ofWrite, 4);
            m_out.writeFloat(fl);
            moveOffset(4);
            }

        /**
        * {@inheritDoc}
        */
        public void writeDouble(double dfl)
                throws IOException
            {
            checkBounds(m_ofWrite, 8);
            m_out.writeDouble(dfl);
            moveOffset(8);
            }

        /**
        * {@inheritDoc}
        */
        public void writeBytes(String s)
                throws IOException
            {
            int cb = s.length();
            checkBounds(m_ofWrite, cb);
            m_out.writeBytes(s);
            moveOffset(cb);
            }

        /**
        * Writes the String <tt>s</tt> as a sequence of characters.
        *
        * @param s  the String to write
        *
        * @exception IOException  if an I/O error occurs
        * @exception NullPointerException  if <tt>s</tt> is <tt>null</tt>
        */
        public void writeChars(String s)
                throws IOException
            {
            // length is the string length times two
            int cb = s.length() * 2;

            checkBounds(m_ofWrite, cb);
            m_out.writeChars(s);
            moveOffset(cb);
            }

        /**
        * {@inheritDoc}
        */
        public void writeUTF(String s)
                throws IOException
            {
            // at most the string will consume 2 bytes for the header and
            // three bytes for each character
            int cbMaxUtf = 2 + 3 * s.length();
            int cbRemain = m_cbMax - m_ofWrite;
            if (cbMaxUtf < cbRemain)
                {
                // capture the current offset before and after the string
                // write
                BufferOutput out = m_out;
                int ofBefore = out.getOffset();
                out.writeUTF(s);
                int ofAfter  = out.getOffset();
                moveOffset(ofAfter - ofBefore);
                }
            else
                {
                // use the default implementation, which will put the UTF
                // binary together in a temp buffer and then write it as
                // a byte array, thus going through the bounds-checking and
                // offset-fixing code in the write(ab, of, cb) method
                super.writeUTF(s);
                }
            }

        // ----- BufferOutput methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public ByteBuffer getByteBuffer(int cb)
            {
            checkBounds(m_ofWrite, cb);
            moveOffset(cb);
            return m_out.getByteBuffer(cb);
            }

        /**
        * {@inheritDoc}
        */
        public void writeBuffer(ReadBuffer buf)
                throws IOException
            {
            int cb = buf.length();
            checkBounds(m_ofWrite, cb);
            m_out.writeBuffer(buf);
            moveOffset(cb);
            }

        /**
        * {@inheritDoc}
        */
        public void writeBuffer(ReadBuffer buf, int of, int cb)
                throws IOException
            {
            checkBounds(m_ofWrite, cb);
            m_out.writeBuffer(buf, of, cb);
            moveOffset(cb);
            }

        /**
        * {@inheritDoc}
        */
        public void writeStream(InputStreaming stream)
                throws IOException
            {
            int cb;

            if (stream instanceof ReadBuffer.BufferInput)
                {
                // it is a known implementation that we can optimize for
                cb = stream.available();
                checkBounds(m_ofWrite, cb);
                m_out.writeStream(stream);
                }
            else
                {
                int of = m_ofWrite;
                cb = copyStream(of, stream, m_cbMax - of);
                }

            moveOffset(cb);
            }

        /**
        * {@inheritDoc}
        */
        public void writeStream(InputStreaming stream, int cb)
                throws IOException
            {
            checkBounds(m_ofWrite, cb);
            m_out.writeStream(stream, cb);
            moveOffset(cb);
            }

        // ----- internal -----------------------------------------------

        /**
        * Move the offset within the stream forward.
        *
        * @param cb  the number of bytes to advance the offset
        */
        protected void moveOffset(int cb)
            {
            int of = m_ofWrite + cb;
            m_ofWrite = of;
            updateLength(of);
            }

        // ----- data members -------------------------------------------

        /**
        * The BufferOutput to delegate to.
        */
        protected BufferOutput m_out;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The WriteBuffer to delegate to; the "containing" WriteBuffer.
    */
    protected WriteBuffer m_buf;

    /**
    * Offset into the containing WriteBuffer where this WriteBuffer starts.
    */
    protected int m_ofStart;

    /**
    * Length in bytes of this WriteBuffer.
    */
    protected int m_cb;

    /**
    * Maximum number of bytes in this WriteBuffer.
    */
    protected int m_cbMax;
    }
