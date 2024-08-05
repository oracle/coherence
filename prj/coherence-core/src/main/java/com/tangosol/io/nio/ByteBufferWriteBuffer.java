/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.io.AbstractWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Binary;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
* A WriteBuffer implementation on top of a Java NIO ByteBuffer.
*
* @author cp  2006.04.05
*/
public final class ByteBufferWriteBuffer
        extends AbstractWriteBuffer
        implements WriteBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ByteBufferWriteBuffer on an NIO ByteBuffer.
    *
    * @param buf  the underlying NIO ByteBuffer
    */
    public ByteBufferWriteBuffer(ByteBuffer buf)
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

    /**
     * Perform a shallow clone of the supplied ByteBufferWriteBuffer.
     *
     * @param that  the buffer to shallow clone
     */
    protected ByteBufferWriteBuffer(ByteBufferWriteBuffer that)
        {
        m_buf = that.m_buf.duplicate();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the ByteBuffer that this WriteBuffer is based on.
    *
    * @return the underlying ByteBuffer
    */
    public ByteBuffer getByteBuffer()
        {
        return m_buf;
        }


    // ----- buffer write operations ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte b)
        {
        ByteBuffer buf   = getByteBuffer();
        int        ofCur = buf.position();
        if (ofDest == ofCur)
            {
            buf.put(b);
            }
        else
            {
            buf.put(ofDest, b);
            if (ofDest > ofCur)
                {
                buf.position(ofDest + 1);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte[] abSrc, int ofSrc, int cbSrc)
        {
        ByteBuffer bufDest = getByteBuffer();
        int        ofCur   = bufDest.position();
        int        ofEnd   = ofDest + cbSrc;

        if (ofEnd > bufDest.limit())
            {
            throw new IndexOutOfBoundsException("ofDest=" + ofDest
                    + ", cbSrc=" + cbSrc
                    + ", getCapacity()=" + bufDest.limit());
            }

        if (ofDest != ofCur)
            {
            bufDest.position(ofDest);
            }

        try
            {
            bufDest.put(abSrc, ofSrc, cbSrc);
            }
        catch (RuntimeException e)
            {
            bufDest.position(ofCur);
            throw e;
            }

        // make sure the current position is always greater or equal to the
        // position prior to this call (i.e. never move to the left)
        if (ofEnd < ofCur)
            {
            bufDest.position(ofCur);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, ReadBuffer bufSrc, int ofSrc, int cbSrc)
        {
        if (bufSrc instanceof Binary)
            {
            Binary     binSrc  = bufSrc.toBinary(ofSrc, cbSrc); // essentially no-op
            ByteBuffer bufDest = getByteBuffer();
            int        ofCur   = bufDest.position();
            int        ofEnd   = ofDest + cbSrc;

            if (ofEnd > bufDest.limit())
                {
                throw new IndexOutOfBoundsException("ofDest=" + ofDest
                        + ", cbSrc=" + cbSrc
                        + ", getCapacity()=" + bufDest.limit());
                }

            if (ofDest != ofCur)
                {
                bufDest.position(ofDest);
                }

            try
                {
                binSrc.writeTo(bufDest);
                }
            catch (RuntimeException e)
                {
                bufDest.position(ofCur);
                throw e;
                }

            // make sure the current position is always greater or equal to the
            // position prior to this call (i.e. never move to the left)
            if (ofEnd < ofCur)
                {
                bufDest.position(ofCur);
                }
            }
        else
            {
            super.write(ofDest, bufSrc, ofSrc, cbSrc);
            }
        }


    // ----- buffer maintenance ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int length()
        {
        return getByteBuffer().position();
        }

    /**
    * Reconfigure the length of the buffer. The length must not be longer than
    * the available capacity.
    *
    * @param cb the new length of the buffer
    */
    public void setLength(int cb)
        {
        int cbMax = getCapacity();
        if (cb < 0 || cb > cbMax)
            {
            throw new IndexOutOfBoundsException("cb=" + cb
                    + ", getCapacity()=" + cbMax);
            }

        getByteBuffer().position(cb);
        }

    /**
    * {@inheritDoc}
    */
    public void retain(int of, int cb)
        {
        ByteBuffer buf   = getByteBuffer();
        int        cbBuf = buf.limit();

        // validate parameters
        if (of < 0 || cb < 0 || of + cb > cbBuf)
            {
            throw new IndexOutOfBoundsException("of=" + of + ", cb=" + cb
                                                + ", getCapacity()=" + cbBuf);
            }

        // compact copies the contents from position to limit to the start
        // of the ByteBuffer
        buf.position(of);
        buf.limit(of+cb);
        buf.compact();

        // reset the buffer limit to where it was
        buf.limit(cbBuf);

        // set the length to the retained data
        buf.position(cb);
        }

    /**
    * {@inheritDoc}
    */
    public int getCapacity()
        {
        return getByteBuffer().limit();
        }


    // ----- obtaining different "write views" to the buffer ----------------

    /**
    * {@inheritDoc}
    */
    public BufferOutput getBufferOutput(int of)
        {
        return new ByteBufferOutput(of);
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
        ByteBufferReadBuffer bufUnsafe = m_bufUnsafe;
        if (bufUnsafe == null)
            {
            m_bufUnsafe = bufUnsafe = new ByteBufferReadBuffer(
                    (ByteBuffer) getByteBuffer().duplicate().flip());
            }
        else
            {
            // read domain is from zero to our position, which is the length of
            // the write buffer (i.e. the amount available to read)
            bufUnsafe.getByteBuffer().limit(getByteBuffer().position());
            }
        return bufUnsafe;
        }

    /**
    * {@inheritDoc}
    */
    public byte[] toByteArray()
        {
        return getUnsafeReadBuffer().toByteArray();
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        return getUnsafeReadBuffer().toBinary();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object clone()
        {
        // create a new NIO buffer for the clone
        ByteBuffer bufThis = getByteBuffer();
        int        cbCur   = bufThis.position();
        int        cbMax   = bufThis.limit();
        ByteBuffer bufThat = bufThis.isDirect()
                             ? ByteBuffer.allocateDirect(cbMax)
                             : ByteBuffer.allocate(cbMax);

        // copy over the buffer's data
        bufThis.limit(cbCur).position(0);
        bufThat.put(bufThis);
        bufThis.limit(cbMax).position(cbCur);

        // create the clone ByteBufferWriteBuffer
        return new ByteBufferWriteBuffer(bufThat);
        }


    // ----- ByteBufferWriteBuffer-specific methods -------------------------

    /**
    * Create a "shallow clone" of the ByteBufferWriteBuffer that uses the same
    * underlying memory but through a different (a duplicate) ByteBuffer. This
    * method is primarily intended to allow multiple threads to be able to
    * write to the same block of NIO memory, by creating shallow clones of the
    * ByteBufferWriteBuffer, one for each thread, since the ByteBuffer itself
    * is not thread safe.
    *
    * @return a ByteBufferWriteBuffer that shares the same underlying memory,
    *         but does not share the same NIO ByteBuffer
    */
    public ByteBufferWriteBuffer getUnsafeWriteBuffer()
        {
        return new ByteBufferWriteBuffer(this);
        }


    // ----- inner class: AbstractBufferOutput ------------------------------

    /**
    * This is a simple implementation of the BufferOutput interface on top of
    * a ByteBuffer.
    *
    * @author cp  2006.04.07
    */
    public final class ByteBufferOutput
            extends AbstractWriteBuffer.AbstractBufferOutput
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a ByteBufferOutput on top of an NIO ByteBuffer.
        *
        * @param of  the offset at which to begin writing
        */
        public ByteBufferOutput(int of)
            {
            super(of);
            }

        // ----- BufferOutput methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public ByteBuffer getByteBuffer(int cb)
            {
            ByteBuffer buf      = getByteBuffer();
            int        ofStream = m_ofWrite;
            int        ofFinal  = ofStream + cb;

            buf.position(ofFinal);
            m_ofWrite = ofFinal;

            return buf.slice(ofStream, cb);
            }

        // ----- DataOutput methods -------------------------------------

        /**
        * {@inheritDoc}
        */
        public void writeShort(int n)
                throws IOException
            {
            ByteBuffer buf      = getByteBuffer();
            int        ofBuf    = buf.position();
            int        ofStream = m_ofWrite;
            int        ofFinal  = ofStream + 2;

            if (ofBuf == ofStream) // this is the common case
                {
                buf.putShort((short) n);
                }
            else if (ofFinal > ofBuf)
                {
                buf.position(ofStream);
                buf.putShort((short) n);
                }
            else
                {
                buf.putShort(ofStream, (short) n);
                }
            m_ofWrite = ofFinal;
            }

        /**
        * {@inheritDoc}
        */
        public void writeChar(int ch)
                throws IOException
            {
            ByteBuffer buf      = getByteBuffer();
            int        ofBuf    = buf.position();
            int        ofStream = m_ofWrite;
            int        ofFinal  = ofStream + 2;

            if (ofBuf == ofStream) // this is the common case
                {
                buf.putChar((char) ch);
                }
            else if (ofFinal > ofBuf)
                {
                buf.position(ofStream);
                buf.putChar((char) ch);
                }
            else
                {
                buf.putChar(ofStream, (char) ch);
                }
            m_ofWrite = ofFinal;
            }

        /**
        * {@inheritDoc}
        */
        public void writeInt(int n)
                throws IOException
            {
            ByteBuffer buf      = getByteBuffer();
            int        ofBuf    = buf.position();
            int        ofStream = m_ofWrite;
            int        ofFinal  = ofStream + 4;

            if (ofBuf == ofStream) // this is the common case
                {
                buf.putInt(n);
                }
            else if (ofFinal > ofBuf)
                {
                buf.position(ofStream);
                buf.putInt(n);
                }
            else
                {
                buf.putInt(ofStream, n);
                }
            m_ofWrite = ofFinal;
            }

        /**
        * {@inheritDoc}
        */
        public void writeLong(long l)
                throws IOException
            {
            ByteBuffer buf      = getByteBuffer();
            int        ofBuf    = buf.position();
            int        ofStream = m_ofWrite;
            int        ofFinal  = ofStream + 8;

            if (ofBuf == ofStream) // this is the common case
                {
                buf.putLong(l);
                }
            else if (ofFinal > ofBuf)
                {
                buf.position(ofStream);
                buf.putLong(l);
                }
            else
                {
                buf.putLong(ofStream, l);
                }
            m_ofWrite = ofFinal;
            }

        /**
        * {@inheritDoc}
        */
        public void writeFloat(float fl)
                throws IOException
            {
            ByteBuffer buf      = getByteBuffer();
            int        ofBuf    = buf.position();
            int        ofStream = m_ofWrite;
            int        ofFinal  = ofStream + 4;

            if (ofBuf == ofStream) // this is the common case
                {
                buf.putFloat(fl);
                }
            else if (ofFinal > ofBuf)
                {
                buf.position(ofStream);
                buf.putFloat(fl);
                }
            else
                {
                buf.putFloat(ofStream, fl);
                }
            m_ofWrite = ofFinal;
            }

        /**
        * {@inheritDoc}
        */
        public void writeDouble(double dfl)
                throws IOException
            {
            ByteBuffer buf      = getByteBuffer();
            int        ofBuf    = buf.position();
            int        ofStream = m_ofWrite;
            int        ofFinal  = ofStream + 8;

            if (ofBuf == ofStream) // this is the common case
                {
                buf.putDouble(dfl);
                }
            else if (ofFinal > ofBuf)
                {
                buf.position(ofStream);
                buf.putDouble(dfl);
                }
            else
                {
                buf.putDouble(ofStream, dfl);
                }
            m_ofWrite = ofFinal;
            }

        // ---- helpers -----------------------------------------------------

        private ByteBuffer getByteBuffer()
            {
            return ByteBufferWriteBuffer.this.getByteBuffer();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying NIO ByteBuffer.
    */
    private final ByteBuffer m_buf;

    /**
    * A cached unsafe ReadBuffer.
    */
    private transient ByteBufferReadBuffer m_bufUnsafe;
    }
