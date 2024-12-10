/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.io.MultiBufferWriteBuffer.WriteBufferPool;

import com.tangosol.util.BitHelper;

import java.io.IOException;

import java.nio.ByteBuffer;

/**
* The MultiBufferWriteBuffer is used to present a single WriteBuffer that
* collects together a sequence of underlying WriteBuffer objects, and which
* grows by allocating additional WriteBuffer objects as necessary.
*
* @author cp  2006.04.10
*/
public class OldMultiBufferWriteBuffer
        extends AbstractWriteBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MultiBufferWriteBuffer that will use the passed
    * WriteBufferPool to allocate WriteBuffer objects from.
    *
    * @param bufferpool  a {@link WriteBufferPool} from which the
    *        MultiBufferWriteBuffer will allocate WriteBuffer objects
    */
    public OldMultiBufferWriteBuffer(WriteBufferPool bufferpool)
        {
        m_bufferpool = bufferpool;
        advance();
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
        int cBuffers = getBufferCount();
        if (iBuffer < cBuffers)
            {
            return cBuffers == 1 ? m_ofCurrent : m_aofBuffer[iBuffer];
            }
        else
            {
            throw new IndexOutOfBoundsException("iBuffer=" + iBuffer
                    + ", BufferCount=" + getBufferCount());
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
        int cBuffers = getBufferCount();
        if (iBuffer < cBuffers)
            {
            return cBuffers == 1 ? m_bufCurrent : m_abuf[iBuffer];
            }
        else
            {
            throw new IndexOutOfBoundsException("iBuffer=" + iBuffer
                    + ", BufferCount=" + getBufferCount());
            }
        }


    // ----- WriteBuffer interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte b)
        {
        int cbTotal = length();
        if (ofDest == cbTotal)
            {
            WriteBuffer buf = getCurrentBuffer();
            int of = buf.length();
            if (of == buf.getCapacity())
                {
                buf = advance();
                of  = 0;
                }
            buf.write(of, b);
            }
        else if (ofDest < cbTotal)
            {
            int         iBuf  = getBufferIndexByOffset(ofDest);
            int         ofBuf = getBufferOffset(iBuf);
            WriteBuffer buf   = getBuffer(iBuf);
            buf.write(ofDest - ofBuf, b);
            }
        else
            {
            WriteBuffer buf = advanceTo(ofDest);
            buf.write(buf.length(), b);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte[] abSrc, int ofSrc, int cbSrc)
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
                buf.write(ofWrite, abSrc, ofSrc, cbWrite);
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
                    buf.write(0, abSrc, ofSrc, cbWrite);
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
                advanceTo(ofDest);
                // fall through to the appending implementation
                }
            }

        // append the data to this buffer
        WriteBuffer buf      = getCurrentBuffer();
        int         cbWrite  = Math.min(cbSrc, getCurrentBufferRemaining());
        if (cbWrite > 0)
            {
            buf.write(buf.length(), abSrc, ofSrc, cbWrite);
            ofSrc += cbWrite;
            cbSrc -= cbWrite;
            }

        while (cbSrc > 0)
            {
            buf      = advance();
            cbWrite  = Math.min(cbSrc, buf.getCapacity());
            buf.write(0, abSrc, ofSrc, cbWrite);
            ofSrc += cbWrite;
            cbSrc -= cbWrite;
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
                advanceTo(ofDest);
                // fall through to the appending implementation
                }
            }

        // append the data to this buffer
        WriteBuffer buf      = getCurrentBuffer();
        int         cbWrite  = Math.min(cbSrc, getCurrentBufferRemaining());
        if (cbWrite > 0)
            {
            buf.write(buf.length(), bufSrc, ofSrc, cbWrite);
            ofSrc += cbWrite;
            cbSrc -= cbWrite;
            }

        while (cbSrc > 0)
            {
            buf      = advance();
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
        return getCurrentBufferAbsoluteOffset() + getCurrentBuffer().length();
        }

    /**
    * {@inheritDoc}
    */
    public void retain(int of, int cb)
        {
        // optimization: only one buffer to manage
        int cBuffers = getBufferCount();
        if (cBuffers == 1)
            {
            getCurrentBuffer().retain(of, cb);
            }
        else // more than one buffer
            {
            WriteBufferPool pool = getBufferPool();
            if (cb == 0)
                {
                // clear() functionality
                WriteBuffer[] abuf = m_abuf;
                int[]         aof  = m_aofBuffer;

                // retain the first buffer
                WriteBuffer buf = abuf[0];
                buf.clear();
                m_bufCurrent = buf;
                m_ofCurrent  = 0;
                m_cBuffers   = 1;

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
                WriteBuffer[] abuf = m_abuf;

                // clear out this MultiBufferWriteBuffer
                m_bufCurrent = null;
                m_ofCurrent  = 0;
                m_cBuffers   = 0;
                m_abuf       = null;
                m_aofBuffer  = null;
                advance();

                // copy whatever portion of the data needs to be retained
                write(0, bufSrc, of, cb);

                // release the original underlying WriteBuffer objects
                for (int i = 0; i < cBuffers; ++i)
                    {
                    pool.release(abuf[i]);
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public int getCapacity()
        {
        return getCurrentBufferAbsoluteOffset() + getCurrentBuffer().getCapacity();
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
    public ReadBuffer getUnsafeReadBuffer()
        {
        int cBuffers = getBufferCount();
        if (cBuffers == 1)
            {
            return getBuffer(0).getUnsafeReadBuffer();
            }
        else
            {
            ReadBuffer[] abuf     = new ReadBuffer[cBuffers];
            int[]        aof      = new int[cBuffers];
            for (int i = 0; i < cBuffers; ++i)
                {
                abuf[i] = getBuffer(i).getUnsafeReadBuffer();
                aof [i] = getBufferOffset(i);
                }
            return new MultiBufferReadBuffer(abuf); // , aof, 0, length());
            }
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
        OldMultiBufferWriteBuffer that = new OldMultiBufferWriteBuffer(getBufferPool());
        if (this.length() > 0)
            {
            that.write(0, this.getUnsafeReadBuffer());
            }
        return that;
        }


    // ----- inner class: AbstractBufferOutput ------------------------------

    /**
    * The MultiBufferOutput implementation extends the AbstractBufferOutput
    * to provide "pass through" operations to the underlying buffer if the
    * operation is guaranteed to fit in the underlying buffer; otherwise,
    * it vitualizes the operation onto the MultiBufferWriteBuffer itself so
    * that the over-run of one underlying WriteBuffer will end up being
    * written to the next underlying WriteBuffer.
    * <p/>
    * This implementation is fairly tightly bound to the super-class
    * implementation; changes to AbstractBufferOutput should be carefully
    * evaluated for potential impacts on this class.
    */
    public class MultiBufferOutput
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
            }

        // ----- BufferOutput methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public ByteBuffer getByteBuffer(int cb)
            {
            if (hasRemaining(cb))
                {
                adjust(cb);
                return getOut().getByteBuffer(cb);
                }

            throw new IndexOutOfBoundsException();
            }

        /**
        * {@inheritDoc}
        */
        public void write(int b)
                throws IOException
            {
            if (hasRemaining(1))
                {
                getOut().write(b);
                adjust(1);
                }
            else
                {
                super.write(b);
                sync();
                }
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
            if (hasRemaining(cb))
                {
                getOut().write(ab, of, cb);
                adjust(cb);
                }
            else
                {
                super.write(ab, of, cb);
                sync();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeShort(int n)
                throws IOException
            {
            if (hasRemaining(2))
                {
                getOut().writeShort(n);
                adjust(2);
                }
            else
                {
                super.writeShort(n);
                sync();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeChar(int ch)
                throws IOException
            {
            if (hasRemaining(2))
                {
                getOut().writeChar(ch);
                adjust(2);
                }
            else
                {
                super.writeChar(ch);
                sync();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeInt(int n)
                throws IOException
            {
            if (hasRemaining(4))
                {
                getOut().writeInt(n);
                adjust(4);
                }
            else
                {
                super.writeInt(n);
                sync();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeLong(long l)
                throws IOException
            {
            if (hasRemaining(8))
                {
                getOut().writeLong(l);
                adjust(8);
                }
            else
                {
                super.writeLong(l);
                sync();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeFloat(float fl)
                throws IOException
            {
            if (hasRemaining(4))
                {
                getOut().writeFloat(fl);
                adjust(4);
                }
            else
                {
                super.writeFloat(fl);
                sync();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeDouble(double dfl)
                throws IOException
            {
            if (hasRemaining(8))
                {
                getOut().writeDouble(dfl);
                adjust(8);
                }
            else
                {
                super.writeDouble(dfl);
                sync();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeBuffer(ReadBuffer buf)
                throws IOException
            {
            super.writeBuffer(buf);
            sync();
            }

        /**
        * {@inheritDoc}
        */
        public void writeBuffer(ReadBuffer buf, int of, int cb)
                throws IOException
            {
            super.writeBuffer(buf, of, cb);
            sync();
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
            super.writeStream(stream, cb);
            sync();
            }

        /**
        * {@inheritDoc}
        */
        public void setOffset(int of)
            {
            super.setOffset(of);

            // it is possible to set the offset beyond the bounds of the
            // buffer, which forces us to advance to that point (so that
            // the underlying buffer will exist for the next byte to write)
            OldMultiBufferWriteBuffer bufMulti = OldMultiBufferWriteBuffer.this;
            if (of > bufMulti.length())
                {
                bufMulti.advanceTo(of);
                }

            sync();
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
        * length to the underlying
        * buffer.
        *
        * @param cb  the length to write
        *
        * @return true if there are at least <tt>cb</tt> bytes remaining to
        *         be written in the underlying buffer
        */
        protected boolean hasRemaining(int cb)
            {
            BufferOutput out = getOut();
            return out.getOffset() + cb <= out.getBuffer().getCapacity();
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
        * After traversing an underlying WriteBuffer boundary, or otherwise
        * changing the offset significantly, sync between this BufferOutput's
        * absolute position and an underlying BufferOutput's relative
        * position.
        */
        protected void sync()
            {
            OldMultiBufferWriteBuffer bufMulti = OldMultiBufferWriteBuffer.this;

            // absolute offset of this BufferOutput
            int of = getOffset();

            // find the underlying WriteBuffer for that offset
            int         iBuf = bufMulti.getBufferIndexByOffset(of);
            WriteBuffer buf  = bufMulti.getBuffer(iBuf);

            // convert the absolute offset to the underlying buffer's
            // relative offset
            of -= bufMulti.getBufferOffset(iBuf);

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
                m_out = buf.getBufferOutput(of);
                }
            }

        // ----- data members -------------------------------------------

        /**
        * The current underlying BufferOutput.
        */
        private BufferOutput m_out;
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
        int cBuffers = getBufferCount();
        if (cBuffers == 1)
            {
            // since there is only one buffer, the offset occurs within it
            return 0;
            }

        // optimization: use previous "cached" result
        int[] aof = m_aofBuffer;
        if (of >= m_ofLastOffset)
            {
            int iBuf = m_iBufLastAnswer;
            if (iBuf + 1 >= cBuffers || of < aof[iBuf+1])
                {
                return iBuf;
                }
            }

        // binary search through the array of offsets
        int iBuf = 0;
        int iMSB = BitHelper.indexOfMSB(cBuffers);
        int iPos = (1 << iMSB) - 1;
        // technically, "cJmp = 1 << (iMSB - 1)", but this primes the do/while
        int   cJmp = 1 << iMSB;
        do
            {
            cJmp >>>= 1;

            // absolute offset of the first byte of the buffer
            int ofBuf = iPos >= cBuffers ? Integer.MAX_VALUE : aof[iPos];

            if (of == ofBuf)
                {
                // exact hit
                iBuf = iPos;
                break;
                }
            else if (of < ofBuf)
                {
                // go "left" in the binary tree
                iPos -= cJmp;
                }
            else // if (of > ofBuf)
                {
                // go "right" in the binary tree, but
                iBuf  = iPos; // this is the closest we've come so far ..
                iPos += cJmp;
                }
            }
        while (cJmp > 0);

        // update "cache"
        m_ofLastOffset   = of;
        m_iBufLastAnswer = iBuf;

        return iBuf;
        }

    /**
    * Once the current buffer is full, allocate a new one and make it the
    * current buffer.
    *
    * @return the new WriteBuffer object
    */
    protected WriteBuffer advance()
        {
        WriteBufferPool bufferfactory = getBufferPool();
        WriteBuffer     bufNew;

        int cBuffers = m_cBuffers;
        if (cBuffers == 0)
            {
            m_bufCurrent = bufNew = bufferfactory.allocate(0);
            m_cBuffers   = 1;
            }
        else
            {
            WriteBuffer   buf       = m_bufCurrent;
            int           of        = m_ofCurrent;
            WriteBuffer[] abuf      = m_abuf;
            int[]         aof       = m_aofBuffer;
            int           cElements = abuf == null ? 0 : abuf.length;

            // ensure that the arrays are large enough to hold the new buffer
            if (cBuffers >= cElements)
                {
                int           cNew    = cElements + 16;
                WriteBuffer[] abufNew = new WriteBuffer[cNew];
                int[]         aofNew  = new int[cNew];
                if (abuf == null)
                    {
                    abufNew[0] = m_bufCurrent;
                    aofNew [0] = m_ofCurrent;
                    }
                else
                    {
                    System.arraycopy(abuf, 0, abufNew, 0, cBuffers);
                    System.arraycopy(aof , 0, aofNew , 0, cBuffers);
                    }
                m_abuf      = abuf = abufNew;
                m_aofBuffer = aof  = aofNew;
                }

            // allocate the new buffer
            of    += buf.length();
            bufNew = bufferfactory.allocate(of);

            // store the new buffer
            abuf[cBuffers] = bufNew;
            aof [cBuffers] = of;

            m_bufCurrent = bufNew;
            m_ofCurrent  = of;
            m_cBuffers   = ++cBuffers;
            }

        return bufNew;
        }

    /**
    * Increase the MultiBufferWriteBuffer length so that the next byte to
    * write is at the specified offset.
    *
    * @param of  the offset to advance to
    *
    * @return the underlying WriteBuffer containing space for the next byte
    *         to write
    */
    protected WriteBuffer advanceTo(int of)
        {
        int cb = length();
        if (of <= cb)
            {
            throw new IllegalArgumentException("of=" + of + ", length()=" + cb);
            }

        // fill with zeros
        final byte[] abFill = FILL;
        final int    cbFill = abFill.length;
        while (cb < of)
            {
            int cbWrite = Math.min(of - cb, cbFill);
            write(cb, abFill, 0, cbWrite);
            cb += cbWrite;
            }

        // don't return a full buffer
        if (getCurrentBufferRemaining() == 0)
            {
            advance();
            }

        return getCurrentBuffer();
        }

    /**
    * Obtain the current buffer.
    *
    * @return the current underlying WriteBuffer
    */
    protected WriteBuffer getCurrentBuffer()
        {
        return m_bufCurrent;
        }

    /**
    * Determine the offset of the first byte of the current buffer.
    *
    * @return the offset of the first byte written to the current
    *         underlying WriteBuffer
    */
    protected int getCurrentBufferAbsoluteOffset()
        {
        return m_ofCurrent;
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
        WriteBuffer buf = getCurrentBuffer();
        return buf.getCapacity() - buf.length();
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
    * The current buffer (never null).
    */
    private WriteBuffer m_bufCurrent;

    /**
    * The absolute offset of the first byte stored in the current buffer.
    */
    private int m_ofCurrent;

    /**
    * The number of allocated WriteBuffer objects.
    */
    private int m_cBuffers;

    /**
    * The array of all WriteBuffer objects allocated to store the contents
    * of this MultiBufferWriteBuffer.
    */
    private WriteBuffer[] m_abuf;

    /**
    * An array of absolute offsets, each corresponding to the first byte
    * stored in the corresponding WriteBuffer object.
    */
    private int[] m_aofBuffer;

    /**
    * Cached "last offset looked up" value.
    */
    private transient int m_ofLastOffset;

    /**
    * Cached "last buffer index answer" value.
    */
    private transient int m_iBufLastAnswer;
    }
