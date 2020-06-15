/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.io.AbstractReadBuffer;
import com.tangosol.io.ReadBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;


/**
* A ReadBuffer implementation on top of a Java NIO ByteBuffer.
*
* @author cp  2006.04.05
*/
public class ByteBufferReadBuffer
        extends AbstractReadBuffer
        implements ReadBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ByteBufferReadBuffer on an NIO ByteBuffer.
    *
    * @param buf  the underlying NIO ByteBuffer
    */
    public ByteBufferReadBuffer(ByteBuffer buf)
        {
        if (buf.order() != ByteOrder.BIG_ENDIAN)
            {
            // the stream view requires big-endian encoding (i.e. Java format)
            buf = buf.slice();
            buf.order(ByteOrder.BIG_ENDIAN);
            }
        else if (buf.position() != 0)
            {
            buf = buf.slice();
            }

        m_buf = buf;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the ByteBuffer that this ReadBuffer is based on.
    *
    * @return the underlying ByteBuffer
    */
    public ByteBuffer getByteBuffer()
        {
        return m_buf;
        }


    // ----- ReadBuffer methods ---------------------------------------------
    /**
    * {@inheritDoc}
    */
    public void writeTo(OutputStream out)
            throws IOException
        {
        toBinary().writeTo(out);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(OutputStream out, int of, int cb)
            throws IOException
        {
        toBinary(of, cb).writeTo(out);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(DataOutput out)
            throws IOException
        {
        toBinary().writeTo(out);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(DataOutput out, int of, int cb)
            throws IOException
        {
        toBinary(of, cb).writeTo(out);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(ByteBuffer buf)
        {
        toBinary().writeTo(buf);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(ByteBuffer buf, int of, int cb)
            throws IOException
        {
        toBinary(of, cb).writeTo(buf);
        }

    /**
    * {@inheritDoc}
    */
    public int length()
        {
        return getByteBuffer().limit();
        }

    /**
    * {@inheritDoc}
    */
    public byte byteAt(int of)
        {
        return getByteBuffer().get(of);
        }

    /**
    * {@inheritDoc}
    */
    public void copyBytes(int ofBegin, int ofEnd, byte abDest[], int ofDest)
        {
        ByteBuffer buf = getByteBuffer().duplicate();
        buf.position(ofBegin);
        buf.get(abDest, ofDest, ofEnd - ofBegin);
        }

    /**
    * {@inheritDoc}
    */
    public byte[] toByteArray(int of, int cb)
        {
        checkBounds(of, cb);

        // optimization: array of zero length
        if (cb == 0)
            {
            return NO_BYTES;
            }

        // optimization: the ByteBuffer has an array
        byte[]     abNew = new byte[cb];
        ByteBuffer buf   = getByteBuffer();
        if (buf.hasArray() && !buf.isReadOnly())
            {
            // adjust offset based on what part of the underlying byte[] this
            // buffer is "over"
            System.arraycopy(buf.array(), buf.arrayOffset() + of, abNew, 0, cb);
            }
        else
            {
            buf = buf.duplicate();
            buf.position(of);
            buf.get(abNew, 0, cb);
            }

        return abNew;
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary(int of, int cb)
        {
        // optimization: the ByteBuffer has an array
        ByteBuffer buf = getByteBuffer();
        if (buf.hasArray() && !buf.isReadOnly())
            {
            checkBounds(of, cb);
            return new Binary(buf.array(), buf.arrayOffset() + of, cb);
            }
        else
            {
            BinaryWriteBuffer bwb = new BinaryWriteBuffer(cb, cb);
            bwb.write(0, this, of, cb);
            return bwb.toBinary();
            }
        }

    /**
    * {@inheritDoc}
    */
    public ByteBuffer toByteBuffer()
        {
        return getByteBuffer().asReadOnlyBuffer();
        }

    /**
    * {@inheritDoc}
    */
    public ByteBuffer toByteBuffer(int of, int cb)
        {
        checkBounds(of, cb);

        ByteBuffer buffer = getByteBuffer().asReadOnlyBuffer();

        buffer.limit(of + cb)
              .position(of);

        return buffer.slice();
        }

    /**
    * {@inheritDoc}
    */
    public Object clone()
        {
        // NOTE: since a ReadBuffer may be wrapping a pooled ByteBuffer, it is not safe
        // to 'duplicate' the ByteBuffer, we must do a full byte-copy into a new buffer
        ByteBuffer  buf = getByteBuffer();
        int         cb  = buf.capacity();

        ByteBuffer bufDup = (ByteBuffer) buf.duplicate()
                .limit(cb)
                .rewind();

        ByteBuffer bufClone = (ByteBuffer) (buf.isDirect()
                                    ? ByteBuffer.allocateDirect(cb) : ByteBuffer.allocate(cb))
                .put(bufDup)
                .limit(buf.limit())
                .position(buf.position());

        return new ByteBufferReadBuffer(buf.isReadOnly() ? bufClone.asReadOnlyBuffer() : bufClone);
        }


    // ----- factory methods ------------------------------------------------

    /**
    * Factory method: Instantiate a ReadBuffer for a portion of this
    * ReadBuffer.
    *
    * @param of  the beginning index, inclusive
    * @param cb  the number of bytes to include in the resulting ReadBuffer
    *
    * @return a ReadBuffer that represents a portion of this ReadBuffer
    */
    protected ReadBuffer instantiateReadBuffer(int of, int cb)
        {
        // create a "sub" buffer
        ByteBuffer buf = getByteBuffer().duplicate();
        buf.position(of);
        buf.limit(of + cb);
        return new ByteBufferReadBuffer(buf);
        }

    /**
    * Factory method: Instantiate a BufferInput object to read data from the
    * ReadBuffer.
    *
    * @return a new BufferInput reading from this ReadBuffer
    */
    protected BufferInput instantiateBufferInput()
        {
        ByteBuffer buf = getByteBuffer().duplicate();

        buf.rewind(); // sets buf.position(0) and mark

        // the stream view requires big-endian encoding (i.e. Java format)
        buf.order(ByteOrder.BIG_ENDIAN);

        return new ByteBufferInput(buf);
        }


    // ----- inner class: BufferInput implementation ------------------------

    /**
    * This is a simple implementation of the BufferInput interface on top of
    * a ByteBuffer.
    *
    * @author cp  2006.04.06
    */
    public final class ByteBufferInput
            extends AbstractBufferInput
            implements BufferInput
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        *
        * @param buf  the NIO ByteBuffer that this BufferInput is backed by
        */
        public ByteBufferInput(ByteBuffer buf)
            {
            m_buf = buf;
            }

        // ----- accessors ------------------------------------------------------

        /**
        * Obtain the NIO ByteBuffer that this BufferInput is based on.
        *
        * @return the underlying ByteBuffer
        */
        public ByteBuffer getByteBuffer()
            {
            return m_buf;
            }

        // ----- InputStream methods ------------------------------------

        /**
        * {@inheritDoc}
        */
        public int read()
                throws IOException
            {
            ByteBuffer buf = getByteBuffer();
            return buf.hasRemaining() ? (buf.get() & 0xFF) : -1;
            }

        /**
        * {@inheritDoc}
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

            ByteBuffer buf      = getByteBuffer();
            int        cbMaxSrc = buf.remaining();
            if (cbDest > cbMaxSrc)
                {
                if (cbMaxSrc == 0)
                    {
                    return -1;
                    }

                cbDest = cbMaxSrc;
                }

            buf.get(abDest, ofDest, cbDest);
            return cbDest;
            }

        /**
        * {@inheritDoc}
        */
        public void mark(int cbReadLimit)
            {
            getByteBuffer().mark();
            super.mark(cbReadLimit);
            }

        /**
        * {@inheritDoc}
        */
        public void reset()
                throws IOException
            {
            try
                {
                getByteBuffer().reset();
                }
            catch (InvalidMarkException e)
                {
                throw new IOException("not marked");
                }
            }

        /**
        * {@inheritDoc}
        */
        public int available()
                throws IOException
            {
            return getByteBuffer().remaining();
            }

        // ----- DataInput methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        public int skipBytes(int cb)
                throws IOException
            {
            ByteBuffer buf    = getByteBuffer();
            int        cbSkip = Math.min(cb, buf.remaining());
            adjustOffsetInternal(cbSkip);
            return cbSkip;
            }

        /**
        * {@inheritDoc}
        */
        public byte readByte()
                throws IOException
            {
            try
                {
                return getByteBuffer().get();
                }
            catch (BufferUnderflowException e)
                {
                throw new EOFException();
                }
            }

        /**
        * {@inheritDoc}
        */
        public short readShort()
                throws IOException
            {
            try
                {
                return getByteBuffer().getShort();
                }
            catch (BufferUnderflowException e)
                {
                throw new EOFException();
                }
            }

        /**
        * {@inheritDoc}
        */
        public int readUnsignedShort()
                throws IOException
            {
            try
                {
                return getByteBuffer().getShort() & 0xFFFF;
                }
            catch (BufferUnderflowException e)
                {
                throw new EOFException();
                }
            }

        /**
        * {@inheritDoc}
        */
        public char readChar()
                throws IOException
            {
            try
                {
                return getByteBuffer().getChar();
                }
            catch (BufferUnderflowException e)
                {
                throw new EOFException();
                }
            }

        /**
        * {@inheritDoc}
        */
        public int readInt()
                throws IOException
            {
            try
                {
                return getByteBuffer().getInt();
                }
            catch (BufferUnderflowException e)
                {
                throw new EOFException();
                }
            }

        /**
        * {@inheritDoc}
        */
        public long readLong()
                throws IOException
            {
            try
                {
                return getByteBuffer().getLong();
                }
            catch (BufferUnderflowException e)
                {
                throw new EOFException();
                }
            }

        /**
        * {@inheritDoc}
        */
        public float readFloat()
                throws IOException
            {
            try
                {
                return getByteBuffer().getFloat();
                }
            catch (BufferUnderflowException e)
                {
                throw new EOFException();
                }
            }

        /**
        * {@inheritDoc}
        */
        public double readDouble()
                throws IOException
            {
            try
                {
                return getByteBuffer().getDouble();
                }
            catch (BufferUnderflowException e)
                {
                throw new EOFException();
                }
            }

        // ----- BufferInput methods ------------------------------------

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return getByteBuffer().position();
            }


        // ----- internal -----------------------------------------------

        /**
        * {@inheritDoc}
        */
        public void setOffsetInternal(int of)
            {
            getByteBuffer().position(of);
            }

        /**
        * {@inheritDoc}
        */
        protected void adjustOffsetInternal(int cb)
            {
            ByteBuffer buf = getByteBuffer();
            buf.position(buf.position() + cb);
            }

        /**
        * {@inheritDoc}
        */
        protected String convertUTF(int of, int cb)
                throws IOException
            {
            byte[] ab;

            ByteBuffer buf = getByteBuffer();
            if (buf.hasArray() && !buf.isReadOnly())
                {
                // optimization: direct access to the underlying byte array
                ab  = buf.array();
                of += buf.arrayOffset();
                }
            else
                {
                ab = toByteArray(of, cb);
                of = 0;
                }

            return ExternalizableHelper.convertUTF(ab, of, cb, getCharBuf(cb));
            }

        // ----- data members -------------------------------------------

        /**
        * The ByteBuffer object from which data is read.
        */
        protected ByteBuffer m_buf;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying NIO ByteBuffer.
    */
    private final ByteBuffer m_buf;
    }
