/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.oracle.coherence.common.base.Disposable;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;

import java.io.IOException;

import java.nio.ByteBuffer;

/**
* The MultiBufferWriteBuffer is used to present a single WriteBuffer that
* collects together a sequence of underlying WriteBuffer objects, and which
* grows by allocating additional WriteBuffer objects as necessary.
*
* @author cp  2006.04.10
*/
public final class MultiBufferWriteBuffer
        extends AbstractWriteBuffer
        implements Disposable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MultiBufferWriteBuffer that will use the passed
    * WriteBufferPool to allocate WriteBuffer objects from.
    *
    * @param bufferpool  a {@link WriteBufferPool} from which the
    *        MultiBufferWriteBuffer will allocate WriteBuffer objects
    */
    public MultiBufferWriteBuffer(WriteBufferPool bufferpool)
        {
        this(bufferpool, 0);
        }

    /**
     * Construct a MultiBufferWriteBuffer that will use the passed
     * WriteBufferPool to allocate WriteBuffer objects from.
     *
     * @param bufferpool  a {@link WriteBufferPool} from which the
     *        MultiBufferWriteBuffer will allocate WriteBuffer objects
     * @param cbEstimate  an estimate as to the final size
     */
    public MultiBufferWriteBuffer(WriteBufferPool bufferpool, int cbEstimate)
        {
        m_bufferpool = bufferpool;
        m_bufLast    = bufferpool.allocate(cbEstimate);
        m_cBuffers   = 1;
        }

    // ----- WriteBuffer interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte b)
        {
        try
            {
            ensureBufferOutput(ofDest).write(b);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte[] abSrc, int ofSrc, int cbSrc)
        {
        try
            {
            ensureBufferOutput(ofDest).write(abSrc, ofSrc, cbSrc);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, ReadBuffer bufSrc, int ofSrc, int cbSrc)
        {
        int cbTotal = length();
        if (ofDest != cbTotal)
            {
            if (ofDest < cbTotal)
                {
                // figure out if this write will go beyond the current end of
                // this buffer
                int cbAppend = 0;
                int ofAppend = 0;
                int ofEnd    = ofDest + cbSrc;
                if (ofEnd > cbTotal)
                    {
                    cbAppend = ofEnd - cbTotal;
                    ofAppend = ofSrc + cbSrc - cbAppend;
                    cbSrc   -= cbAppend;
                    }

                // the data is intended to be written within this buffer's
                // current length (or at least the first part of it); find
                // the underlying buffer that the first part will be written
                // to, and write as much as will fit into that buffer up to
                // its length
                int         iBuf    = getBufferIndexByOffset(ofDest);
                WriteBuffer buf     = getBuffer(iBuf);
                int         ofWrite = ofDest - getBufferOffset(iBuf);
                int         cbWrite = Math.min(buf.length() - ofWrite, cbSrc);
                buf.write(ofWrite, bufSrc, ofSrc, cbWrite);
                ofDest += cbWrite;
                ofSrc  += cbWrite;
                cbSrc  -= cbWrite;

                // if there is anything that didn't fit into the first
                // underlying buffer, keep writing until the data to write is
                // exhausted
                while (cbSrc > 0)
                    {
                    buf     = getBuffer(++iBuf);
                    cbWrite = Math.min(buf.length(), cbSrc);
                    buf.write(0, bufSrc, ofSrc, cbWrite);
                    ofDest += cbWrite;
                    ofSrc  += cbWrite;
                    cbSrc  -= cbWrite;
                    }

                // if there is nothing to append to this buffer, then the
                // write is complete
                if (cbAppend == 0)
                    {
                    return;
                    }

                // set up to write any extra amount that starts where this
                // buffer currently stops
                ofSrc = ofAppend;
                cbSrc = cbAppend;
                assert ofDest == cbTotal;
                // fall through to the appending implementation
                }
            else // if (ofDest > cbTotal)
                {
                // fill in up to the point that the write should occur
                ensureBufferOutput(ofDest);
                // fall through to the appending implementation
                }
            }

        // append the data to this buffer
        WriteBuffer buf      = m_bufLast;
        int         cbWrite  = Math.min(cbSrc, getCurrentBufferRemaining());
        if (cbWrite > 0)
            {
            buf.write(buf.length(), bufSrc, ofSrc, cbWrite);
            ofSrc += cbWrite;
            cbSrc -= cbWrite;
            }

        while (cbSrc > 0)
            {
            buf      = addBuffer();
            cbWrite  = Math.min(cbSrc, buf.getCapacity());
            buf.write(0, bufSrc, ofSrc, cbWrite);
            ofSrc += cbWrite;
            cbSrc -= cbWrite;
            }
        }

    /**
    * {@inheritDoc}
    */
    public int length()
        {
        WriteBuffer bufLast = m_bufLast;
        return m_ofLastBuffer + (bufLast == null ? 0 : bufLast.length());
        }

    /**
    * {@inheritDoc}
    */
    public void retain(int of, int cb)
        {
        // optimization: only one buffer to manage
        int cBuffers = m_cBuffers;
        if (cBuffers == 1)
            {
            m_bufLast.retain(of, cb);
            }
        else // more than one buffer
            {
            WriteBufferPool pool = getBufferPool();
            if (cb == 0)
                {
                // clear() functionality
                WriteBuffer[] abuf = m_aBuffer;
                int[]         aof  = m_aofBuffer;

                // retain the first buffer
                WriteBuffer buf = abuf[0];
                buf.clear();
                m_bufLast      = buf;
                m_ofLastBuffer = 0;
                m_cBuffers     = 1;

                WriteBuffer bufOverflow = m_bufOverflow;
                if (cBuffers > 1 && bufOverflow != null)
                    {
                    abuf[cBuffers - 1] = bufOverflow;
                    m_bufOverflow      = null;
                    }

                // release all other buffers
                for (int i = 0; i < cBuffers; ++i)
                    {
                    if (i > 0)
                        {
                        pool.release(abuf[i]);
                        }

                    abuf[i] = null;
                    aof [i] = 0;
                    }
                }
            else
                {
                // get a temporary read-only view of this
                // MultiBufferWriteBuffer
                ReadBuffer bufSrc = getUnsafeReadBuffer();

                // hold on to the underlying WriteBuffer objects (they will
                // need to be released)
                WriteBuffer[] abuf        = m_aBuffer;
                WriteBuffer   bufOverflow = m_bufOverflow;

                if (bufOverflow != null)
                    {
                    abuf[cBuffers - 1] = bufOverflow;
                    }

                // clear out this MultiBufferWriteBuffer
                m_bufLast      = null;
                m_bufOverflow  = null;
                m_ofLastBuffer = 0;
                m_cBuffers     = 0;
                m_aBuffer      = null;
                m_aofBuffer    = null;
                addBuffer();

                // copy whatever portion of the data needs to be retained
                write(0, bufSrc, of, cb);

                // release the original underlying WriteBuffer objects
                for (int i = 0; i < cBuffers; ++i)
                    {
                    pool.release(abuf[i]);
                    }
                }
            }

        // reset miscellaneous state
        m_ofLastLookup   = 0;
        m_iBufLastAnswer = 0;
        m_outInternal    = null;
        m_bufUnsafe      = null;
        }

    /**
    * {@inheritDoc}
    */
    public int getCapacity()
        {
        return m_ofLastBuffer + m_bufLast.getCapacity();
        }

    /**
    * {@inheritDoc}
    */
    public int getMaximumCapacity()
        {
        return getBufferPool().getMaximumCapacity();
        }

    /**
    * {@inheritDoc}
    */
    public ReadBuffer getReadBuffer()
        {
        return new ByteArrayReadBuffer(toByteArray());
        }

    /**
    * {@inheritDoc}
    */
    public ReadBuffer getUnsafeReadBuffer()
        {
        int cBuffers = m_cBuffers;
        if (cBuffers == 1)
            {
            return getBuffer(0).getUnsafeReadBuffer();
            }
        else
            {
            MultiBufferReadBuffer buf = m_bufUnsafe;
            int cb = length();
            if (buf == null || buf.length() != cb)
                {
                ReadBuffer[] abuf     = new ReadBuffer[cBuffers];
                int[]        aof      = new int[cBuffers];
                for (int i = 0; i < cBuffers; ++i)
                    {
                    abuf[i] = getBuffer(i).getUnsafeReadBuffer();
                    aof [i] = getBufferOffset(i);
                    }
                m_bufUnsafe = buf = new MultiBufferReadBuffer(abuf, aof, 0, cb);
                }
            return buf;
            }
        }

    /**
    * {@inheritDoc}
    */
    public byte[] toByteArray()
        {
        int cBuffers = m_cBuffers;
        if (cBuffers == 1)
            {
            return m_bufLast.toByteArray();
            }

        int    cb = length();
        byte[] ab = new byte[cb];

        WriteBuffer[] abuf = m_aBuffer;
        int[]         aof  = m_aofBuffer;
        for (int i = 0, ofThis = aof[i]; i < cBuffers; ++i)
            {
            int ofNext = i + 1 >= cBuffers ? cb : aof[i + 1];
            abuf[i].getUnsafeReadBuffer().copyBytes(0, ofNext - ofThis, ab, ofThis);
            ofThis = ofNext;
            }

        return ab;
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        int cBuffers = m_cBuffers;
        if (cBuffers == 1)
            {
            return m_bufLast.toBinary();
            }

        int               cb     = length();
        BinaryWriteBuffer bufbin = new BinaryWriteBuffer(cb, cb);

        WriteBuffer[] abuf = m_aBuffer;
        int[]         aof  = m_aofBuffer;
        for (int i = 0; i < cBuffers; ++i)
            {
            bufbin.write(aof[i], abuf[i].getUnsafeReadBuffer());
            }

        return bufbin.toBinary();
        }


    /**
    * {@inheritDoc}
    */
    public BufferOutput getBufferOutput(int of)
        {
        return new MultiBufferOutput(of);
        }

    /**
    * {@inheritDoc}
    */
    public Object clone()
        {
        MultiBufferWriteBuffer that = new MultiBufferWriteBuffer(getBufferPool());
        if (this.length() > 0)
            {
            that.write(0, this.getUnsafeReadBuffer());
            }
        return that;
        }

    /**
    * {@inheritDoc}
    */
    public void dispose()
        {
        int cBuffers = m_cBuffers;
        if (cBuffers > 0)
            {
            // release the original underlying WriteBuffer objects
            WriteBuffer     bufSingle = m_bufLast;
            WriteBuffer[]   abuf      = m_aBuffer;
            WriteBufferPool pool      = getBufferPool();
            if (cBuffers == 1)
                {
                bufSingle.clear();
                pool.release(bufSingle);
                }
            else
                {
                for (int i = 0; i < cBuffers; ++i)
                    {
                    WriteBuffer buf = abuf[i];
                    buf.clear();
                    pool.release(buf);
                    }
                }
            }

        // clear out this MultiBufferWriteBuffer
        m_bufLast        = null;
        m_ofLastBuffer   = 0;
        m_cBuffers       = 0;
        m_aBuffer        = null;
        m_aofBuffer      = null;

        // reset miscellaneous state
        m_ofLastLookup   = 0;
        m_iBufLastAnswer = 0;
        m_outInternal    = null;
        m_bufUnsafe      = null;

        // release the pool (to force an NPE if someone uses the buffer)
        m_bufferpool     = null;
        }


    // ----- inner class: AbstractBufferOutput ------------------------------

    /**
    * The MultiBufferOutput implementation extends the AbstractBufferOutput
    * to provide "pass through" operations to the underlying buffer if the
    * operation is guaranteed to fit in the underlying buffer; otherwise,
    * it virtualizes the operation onto the MultiBufferWriteBuffer itself so
    * that the over-run of one underlying WriteBuffer will end up being
    * written to the next underlying WriteBuffer.
    * <p>
    * This implementation is fairly tightly bound to the super-class
    * implementation; changes to AbstractBufferOutput should be carefully
    * evaluated for potential impacts on this class.
    */
    public final class MultiBufferOutput
            extends AbstractBufferOutput
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct an AbstractBufferOutput that will begin writing at the
        * specified offset within the containing WriteBuffer.
        *
        * @param of  the offset at which to begin writing
        */
        public MultiBufferOutput(int of)
            {
            setOffset(of);
            sync();
            }

        // ----- BufferOutput methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public ByteBuffer getByteBuffer(int cb)
            {
            if (m_ofNextBuffer - m_ofWrite < cb)
                {
                advance();
                }

            m_ofWrite += cb;
            return m_out.getByteBuffer(cb);
            }

        /**
        * {@inheritDoc}
        */
        public void write(int b)
            throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < 1)
                {
                advance();
                }

            m_out.write(b);
            m_ofWrite += 1;
            }

        /**
        * {@inheritDoc}
        */
        public void write(byte ab[])
            throws IOException
            {
            write(ab, 0, ab.length);
            }

        /**
        * {@inheritDoc}
        */
        public void write(byte ab[], int of, int cb)
            throws IOException
            {
            int ofWrite = m_ofWrite;
            int cbMax   = m_ofNextBuffer - ofWrite;
            while (cb > cbMax)
                {
                m_out.write(ab, of, cbMax);
                ofWrite += cbMax;
                of      += cbMax;
                cb      -= cbMax;

                advance();
                cbMax = m_ofNextBuffer - ofWrite;
                }

            m_out.write(ab, of, cb);
            m_ofWrite = ofWrite + cb;
            }

        /**
        * {@inheritDoc}
        */
        public void writeShort(int n)
            throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < 2)
                {
                super.writeShort(n);
                }
            else
                {
                m_out.writeShort(n);
                m_ofWrite += 2;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeChar(int ch)
            throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < 2)
                {
                super.writeChar(ch);
                }
            else
                {
                m_out.writeChar(ch);
                m_ofWrite += 2;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeInt(int n)
            throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < 4)
                {
                super.writeInt(n);
                }
            else
                {
                m_out.writeInt(n);
                m_ofWrite += 4;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writePackedInt(int n)
            throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < MAX_PACKED_INT_SIZE)
                {
                super.writePackedInt(n);
                }
            else
                {
                BufferOutput out = m_out;
                int of = out.getOffset();
                out.writePackedInt(n);
                m_ofWrite += out.getOffset() - of;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeLong(long l)
                throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < 8)
                {
                super.writeLong(l);
                }
            else
                {
                m_out.writeLong(l);
                m_ofWrite += 8;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writePackedLong(long n)
            throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < MAX_PACKED_LONG_SIZE)
                {
                super.writePackedLong(n);
                }
            else
                {
                BufferOutput out = m_out;
                int of = out.getOffset();
                out.writePackedLong(n);
                m_ofWrite += out.getOffset() - of;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeFloat(float fl)
            throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < 4)
                {
                super.writeFloat(fl);
                }
            else
                {
                m_out.writeFloat(fl);
                m_ofWrite += 4;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeDouble(double dfl)
                throws IOException
            {
            if (m_ofNextBuffer - m_ofWrite < 8)
                {
                super.writeDouble(dfl);
                }
            else
                {
                m_out.writeDouble(dfl);
                m_ofWrite += 8;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeBuffer(ReadBuffer buf)
                throws IOException
            {
            writeBuffer(buf, 0, buf.length());
            }

        /**
        * {@inheritDoc}
        */
        public void writeBuffer(ReadBuffer buf, int of, int cb)
                throws IOException
            {
            while (true)
                {
                int cbMax   = m_ofNextBuffer - m_ofWrite;
                int cbWrite = Math.min(cb, cbMax);
                if (cbWrite > 0)
                    {
                    m_out.writeBuffer(buf, of, cbWrite);
                    m_ofWrite += cbWrite;
                    of += cbWrite;
                    cb -= cbWrite;
                    }

                if (cb > 0)
                    {
                    advance();
                    }
                else
                    {
                    break;
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeStream(InputStreaming stream)
            throws IOException
            {
            super.writeStream(stream);
            sync();
            }

        /**
        * {@inheritDoc}
        */
        public void writeStream(InputStreaming stream, int cb)
            throws IOException
            {
            while (true)
                {
                int cbMax   = m_ofNextBuffer - m_ofWrite;
                int cbWrite = Math.min(cb, cbMax);
                if (cbWrite > 0)
                    {
                    m_out.writeStream(stream, cbWrite);
                    m_ofWrite += cbWrite;
                    cb -= cbWrite;
                    }

                if (cb > 0)
                    {
                    advance();
                    }
                else
                    {
                    break;
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public void setOffset(int of)
            {
            int ofPrev = m_ofWrite;
            if (of != ofPrev)
                {
                if (ofPrev < 0)
                    {
                    throw new IllegalStateException("BufferOutput is closed");
                    }

                // it is possible to set the offset beyond the bounds of the
                // buffer, which forces us to advance to that point (so that
                // the underlying buffer will exist for the next byte to write)
                MultiBufferWriteBuffer bufMulti = MultiBufferWriteBuffer.this;
                int                    cb       = bufMulti.length();
                super.setOffset(Math.min(of, cb));

                if (of > bufMulti.length())
                    {
                    // fill with zeros
                    final byte[] abFill = FILL;
                    final int    cbFill = abFill.length;
                    while (cb < of)
                        {
                        int cbWrite = Math.min(of - cb, cbFill);
                        try
                            {
                            write(abFill, 0, cbWrite);
                            }
                        catch (IOException e)
                            {
                            throw Base.ensureRuntimeException(e);
                            }
                        cb += cbWrite;
                        }

                    // don't stop at the end of a full buffer
                    if (!hasRemaining(1))
                        {
                        advance();
                        }
                    }
                else if (of >= m_ofNextBuffer
                        || (of < ofPrev && of < bufMulti.getBufferOffset(m_iBuffer)))
                    {
                    // the offset has gone outside of the bounds of the
                    // current underlying buffer as represented by m_out
                    sync();
                    }
                else
                    {
                    // the offset is within the current buffer
                    m_out.setOffset(of - bufMulti.getBufferOffset(m_iBuffer));
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public void close()
                throws IOException
            {
            super.close();

            m_out          = null;
            m_iBuffer      = -1;
            m_ofWrite      = -1;
            m_ofNextBuffer = -1;
            }



        // ----- internal -----------------------------------------------

        /**
        * Obtain the underlying BufferOutput.
        *
        * @return the underlying BufferOutput
        */
        protected BufferOutput getOut()
            {
            return m_out;
            }

        /**
        * Determine if it is possible to write something of a specified
        * length to the underlying buffer.
        *
        * @param cb  the length to write
        *
        * @return true if there are at least <tt>cb</tt> bytes remaining to
        *         be written in the underlying buffer; always returns false
        *         after the BufferOutput has been closed
        */
        protected boolean hasRemaining(int cb)
            {
            return m_ofNextBuffer - m_ofWrite >= cb;
            }

        /**
        * Adjust the offset of this BufferOutput based on a write that
        * by-passed this BufferOutput's own super-class implementation that
        * is responsible for maintaining the offset.
        *
        * @param cb  the number of bytes that were just written directly to
        *            the underlying BufferOutput
        */
        protected void adjust(int cb)
            {
            m_ofWrite += cb;
            }

        /**
        * Advance past the end of the current underlying BufferOutput by
        * switching to the BufferOutput of the next underlying WriteBuffer,
        * creating one if necessary.
        */
        protected void advance()
            {
            MultiBufferWriteBuffer bufMulti = MultiBufferWriteBuffer.this;

            int iBuffer  = m_iBuffer + 1;
            if (iBuffer <= 0)
                {
                throw new IllegalStateException("BufferOutput is closed");
                }
            else if (iBuffer >= bufMulti.getBufferCount())
                {
                bufMulti.addBuffer();
                }

            int         of  = bufMulti.getBufferOffset(iBuffer);
            WriteBuffer buf = bufMulti.getBuffer(iBuffer);

            m_iBuffer      = iBuffer;
            m_out          = buf.getBufferOutput();
            m_ofWrite      = of;
            m_ofNextBuffer = of + buf.getCapacity();
            }

        /**
        * After traversing an underlying WriteBuffer boundary, or otherwise
        * changing the offset significantly, sync between this BufferOutput's
        * absolute position and an underlying BufferOutput's relative
        * position.
        */
        protected void sync()
            {
            MultiBufferWriteBuffer bufMulti = MultiBufferWriteBuffer.this;

            // absolute offset of this BufferOutput
            int of = m_ofWrite;

            // find the underlying WriteBuffer for that offset
            int         iBuf  = bufMulti.getBufferIndexByOffset(of);
            WriteBuffer buf   = bufMulti.getBuffer(iBuf);
            int         ofBuf = bufMulti.getBufferOffset(iBuf);

            // convert the absolute offset to the underlying buffer's
            // relative offset
            of -= ofBuf;

            BufferOutput outPrev = m_out;
            if (outPrev != null && buf == outPrev.getBuffer())
                {
                // still inside the previous underlying WriteBuffer
                outPrev.setOffset(of);
                }
            else
                {
                // traversed to the next (or some subsequent) underlying
                // WriteBuffer
                m_iBuffer      = iBuf;
                m_out          = buf.getBufferOutput(of);
                m_ofNextBuffer = ofBuf + buf.getCapacity();
                }
            }

        // ----- data members -------------------------------------------

        /**
        * The index of the current WriteBuffer.
        */
        private int m_iBuffer;

        /**
        * The current underlying BufferOutput.
        */
        private BufferOutput m_out;

        /**
        * The offset of the first byte of the BufferOutput that will follow
        * the current BufferOutput.
        */
        private int m_ofNextBuffer;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the factory used to create WriteBuffer objects.
    *
    * @return the WriteBufferPool used by this MultiBufferWriteBuffer
    */
    public WriteBufferPool getBufferPool()
        {
        return m_bufferpool;
        }

    /**
    * Determine the number of WriteBuffer objects allocated by this
    * MultiBufferWriteBuffer from the WriteBufferPool.
    *
    * @return the count of WriteBuffer objects allocated
    */
    public int getBufferCount()
        {
        return m_cBuffers;
        }

    /**
    * Determine the offset of the specified buffer. The offset of a buffer
    * is the absolute offset of the first byte stored in the buffer.
    *
    * @param iBuffer  an index <tt>0 &lt;= iBuffer &lt; getBufferCount()</tt>
    *
    * @return the absolute offset of the first byte of the specified
    *         WriteBuffer
    */
    public int getBufferOffset(int iBuffer)
        {
        int cBuffers = m_cBuffers;
        if (iBuffer < cBuffers)
            {
            return cBuffers == 1 ? m_ofLastBuffer : m_aofBuffer[iBuffer];
            }
        else
            {
            throw new IndexOutOfBoundsException("iBuffer=" + iBuffer
                    + ", BufferCount=" + cBuffers);
            }
        }

    /**
    * Obtain the specified buffer.
    *
    * @param iBuffer  an index <tt>0 &lt;= iBuffer &lt; getBufferCount()</tt>
    *
    * @return the specified WriteBuffer
    */
    public WriteBuffer getBuffer(int iBuffer)
        {
        int cBuffers = m_cBuffers;
        if (iBuffer < cBuffers)
            {
            return cBuffers == 1 ? m_bufLast : m_aBuffer[iBuffer];
            }
        else
            {
            throw new IndexOutOfBoundsException("iBuffer=" + iBuffer
                    + ", BufferCount=" + cBuffers);
            }
        }


    // ----- internal -------------------------------------------------------

    /**
    * Determine which underlying WriteBuffer contains the specified offset.
    *
    * @param of  an offset into the MultiBufferWriteBuffer,
    *            <tt>0 &lt;= of &lt;= length()</tt>
    *
    * @return the index of the WriteBuffer containing the specified offset
    */
    protected int getBufferIndexByOffset(int of)
        {
        int cBuffers = m_cBuffers;
        if (cBuffers == 1 || of == 0)
            {
            // if there is only one buffer, the offset occurs within it; the
            // zero offset always occurs with the first buffer as well
            return 0;
            }

        // optimization: check last buffer
        if (of >= m_ofLastBuffer)
            {
            assert of <= length();
            return cBuffers - 1;
            }

        // optimization: use previous "cached" result
        int[] aof = m_aofBuffer;
        if (of >= m_ofLastLookup)
            {
            int iBuf = m_iBufLastAnswer;
            if (iBuf + 1 >= cBuffers || of < aof[iBuf + 1])
                {
                return iBuf;
                }
            }

        // binary search through the array of offsets
        int iBuf  = 0;
        int iLow  = 0;
        int iHigh = cBuffers - 2;   // don't check the last one
        while (iLow <= iHigh)
            {
            // pick a buffer to act as the root of the tree (or sub-tree)
            // that is being searched
            int iRoot = (iLow + iHigh) >> 1;

            // absolute offset of the first byte of the buffer
            int ofRoot = aof[iRoot];

            if (of == ofRoot)
                {
                // exact hit
                iBuf = iRoot;
                break;
                }
            else if (of < ofRoot)
                {
                iHigh = iRoot - 1;
                }
            else // if (of > ofRoot)
                {
                // go "right" in the binary tree ..
                iLow = iRoot + 1;

                // .. but remember this is the closest we've come so far ..
                iBuf  = iRoot;
                }
            }

        // update "cache"
        m_ofLastLookup   = of;
        m_iBufLastAnswer = iBuf;

        return iBuf;
        }

    /**
    * Once the current buffer is full, allocate a new one and make it the
    * current buffer.
    *
    * @return the new WriteBuffer object
    */
    protected WriteBuffer addBuffer()
        {
        WriteBufferPool bufferfactory = getBufferPool();
        WriteBuffer     bufNew;

        int cBuffers = m_cBuffers;
        if (cBuffers == 0)
            {
            m_bufLast  = bufNew = bufferfactory.allocate(0);
            m_cBuffers = 1;
            }
        else if (getCapacity() == Integer.MAX_VALUE)
            {
            throw new UnsupportedOperationException("buffer has reached its max capacity of 2GB");
            }
        else
            {
            WriteBuffer   buf       = m_bufLast;
            int           of        = m_ofLastBuffer;
            WriteBuffer[] abuf      = m_aBuffer;
            int[]         aof       = m_aofBuffer;
            int           cElements = abuf == null ? 0 : abuf.length;

            // ensure that the arrays are large enough to hold the new buffer
            if (cBuffers >= cElements)
                {
                int           cNew    = Math.max(16, cElements << 1);
                WriteBuffer[] abufNew = new WriteBuffer[cNew];
                int[]         aofNew  = new int[cNew];
                if (abuf == null)
                    {
                    abufNew[0] = m_bufLast;
                    aofNew [0] = m_ofLastBuffer;
                    }
                else
                    {
                    System.arraycopy(abuf, 0, abufNew, 0, cBuffers);
                    System.arraycopy(aof , 0, aofNew , 0, cBuffers);
                    }
                m_aBuffer   = abuf = abufNew;
                m_aofBuffer = aof  = aofNew;
                }

            // allocate the new buffer
            of    += buf.length();
            bufNew = bufferfactory.allocate(of);

            if (of + bufNew.getCapacity() < of)
                {
                // integer overflow
                m_bufOverflow = bufNew;
                bufNew        = bufNew.getWriteBuffer(0, Integer.MAX_VALUE - of);
                }

            // store the new buffer
            abuf[cBuffers] = bufNew;
            aof [cBuffers] = of;

            m_bufLast      = bufNew;
            m_ofLastBuffer = of;
            m_cBuffers     = ++cBuffers;
            }

        // clear the cached readbuffer
        m_bufUnsafe = null;

        return bufNew;
        }

    /**
    * Obtain the current buffer.
    *
    * @return the current underlying WriteBuffer
    */
    protected WriteBuffer getCurrentBuffer()
        {
        return m_bufLast;
        }

    /**
    * Determine the offset of the first byte of the current buffer.
    *
    * @return the offset of the first byte written to the current
    *         underlying WriteBuffer
    */
    protected int getCurrentBufferAbsoluteOffset()
        {
        return m_ofLastBuffer;
        }

    /**
    * Determine the maximum number of bytes that can still be written to the
    * current underlying WriteBuffer.
    *
    * @return the number of bytes of capacity that remain on the current
    *         underlying WriteBuffer object
    */
    protected int getCurrentBufferRemaining()
        {
        WriteBuffer buf = m_bufLast;
        return buf.getCapacity() - buf.length();
        }

    /**
    * Obtain the internal MultiBufferOutput, creating it if necessary.
    *
    * @param of  the desired offset of the MultiBufferOutput
    *
    * @return a MultiBufferOutput set to the specified offest
    */
    protected MultiBufferOutput ensureBufferOutput(int of)
        {
        MultiBufferOutput outInternal = m_outInternal;
        if (outInternal == null)
            {
            m_outInternal = outInternal = (MultiBufferOutput) getBufferOutput(of);
            }
        else
            {
            if (of != outInternal.m_ofWrite)
                {
                outInternal.setOffset(of);
                }
            }
        return outInternal;
        }


    // ----- inner interface: WriteBufferPool ----------------------------

    /**
    * A WriteBufferPool is used to dynamically allocate WriteBuffer
    * objects as the MultiBufferWriteBuffer requires them. It is expected
    * that implementations may use pooling, or may create WriteBuffer objects
    * as necessary.
    */
    public interface WriteBufferPool
        {
        /**
        * Determine the largest amount of aggregate WriteBuffer capacity
        * that this factory can provide.
        *
        * @return the number of bytes that can be stored in the WriteBuffer
        *         objects that may be returned from this factory
        */
        public int getMaximumCapacity();

        /**
        * Allocate a WriteBuffer for use by the MultiBufferWriteBuffer. The
        * MultiBufferWriteBuffer calls this factory method when it exhausts
        * the storage capacity of previously allocated WriteBuffer objects.
        * <p>
        * Note that the returned WriteBuffer is expected to be empty, and
        * its capacity is expected to be identical to its maximum capacity,
        * i.e. it is not expected to resize itself, since the purpose of the
        * MultiBufferWriteBuffer is to act as a dynamically-sized
        * WriteBuffer.
        *
        * @param cbPreviousTotal  the total number of bytes of capacity of
        *        the WriteBuffer objects that the MultiBufferWriteBuffer has
        *        thus far consumed
        *
        * @return an empty WriteBuffer suitable for writing to
        */
        public WriteBuffer allocate(int cbPreviousTotal);

        /**
        * Returns a WriteBuffer to the pool. This can happen when
        * {@link MultiBufferWriteBuffer#clear()}, or
        * {@link MultiBufferWriteBuffer#retain(int)}, or
        * {@link MultiBufferWriteBuffer#retain(int, int)} is called.
        *
        * @param buffer  the WriteBuffer that is no longer being used
        */
        public void release(WriteBuffer buffer);
        }


    // ----- constants ------------------------------------------------------

    /**
    * Empty fill.
    */
    static final byte[] FILL = new byte[1024];


    // ----- data members ---------------------------------------------------

    /**
    * The factory for obtaining underlying WriteBuffer objects from.
    */
    private WriteBufferPool m_bufferpool;

    /**
    * The last buffer. The "last" buffer is the "right-most"
    * buffer in the left-to-right (0..capacity) sequence of buffers.
    */
    private WriteBuffer m_bufLast;

    /**
     * In the case that the MultiBuffer grows to greater then 2GB, this is the "real" last allocated buffer, whereas
     * m_bufLast is a slice of this buffer ensuring that there is no writeable space beyond 2GB.  This reference is
     * retained in order to be able to release the real buffer
     */
    private WriteBuffer m_bufOverflow;

    /**
    * The absolute offset of the first byte stored in the last buffer.
    */
    private int m_ofLastBuffer;

    /**
    * The number of allocated WriteBuffer objects.
    */
    private int m_cBuffers;

    /**
    * The array of all WriteBuffer objects allocated to store the contents
    * of this MultiBufferWriteBuffer.
    */
    private WriteBuffer[] m_aBuffer;

    /**
    * An array of absolute offsets, each corresponding to the first byte
    * stored in the corresponding WriteBuffer object.
    */
    private int[] m_aofBuffer;

    /**
    * Cached "last offset looked up" value.
    */
    private transient int m_ofLastLookup;

    /**
    * Cached "last buffer index answer" value.
    */
    private transient int m_iBufLastAnswer;

    /**
    * The WriteBuffer internally delegates to a MultiBufferOutput.
    */
    private MultiBufferOutput m_outInternal;

    /**
    * A cached "unsafe" MultiBufferReadBuffer instance.
    */
    private MultiBufferReadBuffer m_bufUnsafe;
    }
