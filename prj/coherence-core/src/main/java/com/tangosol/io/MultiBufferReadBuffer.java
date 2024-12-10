/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;

import com.tangosol.io.nio.ByteBufferOutputStream;

import com.tangosol.util.Binary;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;

import static com.oracle.coherence.common.base.Exceptions.ensureRuntimeException;

/**
* The MultiBufferReadBuffer is a ReadBuffer implementation that presents a
* view across any number of underlying ReadBuffer objects, as if they were
* appended end-to-end into a single ReadBuffer.
*
* @author cp  2006.04.15
*/
public class MultiBufferReadBuffer
        extends AbstractReadBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MultiBufferReadBuffer from an array of underlying
    * ReadBuffer objects.
    *
    * @param abuf  an array of ReadBuffer objects from which to construct
    *              this MultiBufferReadBuffer
    */
    public MultiBufferReadBuffer(ReadBuffer[] abuf)
        {
              abuf     = abuf.clone();
        int   cBuffers = abuf.length;
        int[] aof      = new int[cBuffers];
        int   cb       = 0;
        for (int i = 0; i < cBuffers; ++i)
            {
            aof[i] = cb;

            int cbBuf = abuf[i].length();
            if (cb + cbBuf < cb)
                {
                // integer overflow
                throw new IllegalArgumentException("cumulative buffer length exceeds 2GB");
                }
            cb += cbBuf;
            }

        m_abuf      = abuf;
        m_aofBuffer = aof;
        m_ofStart   = 0;
        m_ofEnd     = cb;
        }

    /**
    * Construct a MultiBufferReadBuffer from its constituent members. This
    * is a package-private constructor intended for use by the
    * MultiBufferWriteBuffer and the MultiBufferReadBuffer itself. Note that
    * this implementation holds onto the passed array references.
    *
    * @param abuf       an array of underlying ReadBuffer objects containing
    *                   the data that this MultiBufferReadBuffer represents
    * @param aofBuffer  the absolute offset of the first byte of each
    *                   ReadBuffer passed in <tt>abuf</tt>
    * @param ofStart    the absolute offset into the virtual ReadBuffer that
    *                   corresponds to the zero offset of this
    *                   MultiBufferReadBuffer
    * @param ofEnd      the absolute offset into the virtual ReadBuffer that
    *                   corresponds to the first byte beyond the bounds of
    *                   this MultiBufferReadBuffer
    */
    MultiBufferReadBuffer(ReadBuffer[] abuf, int[] aofBuffer, int ofStart, int ofEnd)
        {
        m_abuf      = abuf;
        m_aofBuffer = aofBuffer;
        m_ofStart   = ofStart;
        m_ofEnd     = ofEnd;
        }


    // ----- MultiBufferReadBuffer interface --------------------------------

    /**
    * Return a self-destructing BufferInput over this Buffer.
    *
    * As the BufferInput is advanced the individual buffer segments will be
    * released allowing them to potentially be garbage collected.
    *
    * @return a destructed BufferInput
    */
    public BufferInput getDestructiveBufferInput()
        {
        return instantiateBufferInput(/*fDestructive*/ true);
        }


    // ----- ReadBuffer interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void writeTo(OutputStream out)
            throws IOException
        {
        writeTo((DataOutput) new DataOutputStream(out));
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(OutputStream out, int of, int cb)
            throws IOException
        {
        writeTo((DataOutput) new DataOutputStream(out), of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(DataOutput out)
            throws IOException
        {
        writeTo(out, 0, length());
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(DataOutput out, int of, int cb)
            throws IOException
        {
        if (length() == 0 || cb == 0)
            {
            // nop
            return;
            }

        int iBufFirst = getBufferIndexByOffset(of);
        int iBufLast  = getBufferIndexByOffset(of + cb);

        for (int iBuf = iBufFirst; iBuf <= iBufLast; iBuf++)
            {
            ReadBuffer buf   = getBuffer(iBuf);
            int        cbBuf = buf.length();

            if (iBuf == iBufFirst)
                {
                int ofSrc = getBufferOffset(iBuf);  // ofSrc <= of
                buf = buf.getReadBuffer(of - ofSrc, cbBuf + ofSrc);
                cbBuf += ofSrc;
                }
            else if (iBuf == iBufLast)
                {
                buf = buf.getReadBuffer(0, cb);
                }

            buf.writeTo(out);
            cb -= cbBuf;
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(ByteBuffer buf)
        {
        try
            {
            writeTo(new ByteBufferOutputStream(buf));
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(ByteBuffer buf, int of, int cb)
            throws IOException
        {
        writeTo(new ByteBufferOutputStream(buf), of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public int length()
        {
        return m_ofEnd - m_ofStart;
        }

    /**
    * {@inheritDoc}
    */
    public byte byteAt(int of)
        {
        checkBounds(of, 1);
        int iBuf = getBufferIndexByOffset(of);
        return getBuffer(iBuf).byteAt(of - getBufferOffset(iBuf));
        }

    /**
    * {@inheritDoc}
    */
    public void copyBytes(int ofBegin, int ofEnd, byte abDest[], int ofDest)
        {
        int cbDest = ofEnd - ofBegin;
        checkBounds(ofBegin, cbDest);

        if (ofDest < 0 || ofDest + cbDest > abDest.length)
            {
            throw new IndexOutOfBoundsException("ofDest=" + ofDest
                    + ", abDest.length=" + abDest.length
                    + ", bytes requested=" + cbDest);
            }

        int        iBuf  = getBufferIndexByOffset(ofBegin);
        ReadBuffer buf   = getBuffer(iBuf);
        int        ofBuf = getBufferOffset(iBuf);
        int        ofSrc = ofBegin - ofBuf;
        int        cbSrc = Math.min(cbDest, buf.length() - ofSrc);
        buf.copyBytes(ofSrc, ofSrc + cbSrc, abDest, ofDest);
        ofDest += cbSrc;
        cbDest -= cbSrc;

        while (cbDest > 0)
            {
            buf   = getBuffer(++iBuf);
            cbSrc = Math.min(cbDest, buf.length());
            buf.copyBytes(0, cbSrc, abDest, ofDest);
            ofDest += cbSrc;
            cbDest -= cbSrc;
            }
        }

    /**
    * {@inheritDoc}
    */
    public byte[] toByteArray(int of, int cb)
        {
        checkBounds(of, cb);
        if (cb == 0)
            {
            return NO_BYTES;
            }

        int iBuf = getBufferIndexByOffset(of);
        return iBuf == getBufferIndexByOffset(of + cb - 1)
                ? getBuffer(iBuf).toByteArray(of - getBufferOffset(iBuf), cb)
                : super.toByteArray(of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary(int of, int cb)
        {
        checkBounds(of, cb);
        if (cb == 0)
            {
            return NO_BINARY;
            }

        int iBuf = getBufferIndexByOffset(of);
        return iBuf == getBufferIndexByOffset(of + cb - 1)
                ? getBuffer(iBuf).toBinary(of - getBufferOffset(iBuf), cb)
                : super.toBinary(of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public ByteBuffer toByteBuffer()
        {
        return toByteBuffer(0, length());
        }

    /**
    * {@inheritDoc}
    */
    public ByteBuffer toByteBuffer(int of, int cb)
        {
        int iBuf = getBufferIndexByOffset(of);
        if (iBuf == getBufferIndexByOffset(of + cb - 1))
            {
            return getBuffer(iBuf).toByteBuffer(of - getBufferOffset(iBuf), cb);
            }
        return ByteBuffer.wrap(toByteArray(of, cb)).asReadOnlyBuffer();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }

        if (o instanceof ReadBuffer)
            {
            ReadBuffer bufThat = (ReadBuffer) o;

            int cbThis = length();
            int cbThat = bufThat.length();
            if (cbThis != cbThat)
                {
                return false;
                }

            if (cbThat == 0)
                {
                return true;
                }

            int iBufFirst = getBufferIndexByOffset(0);
            int iBufLast  = getBufferIndexByOffset(cbThis);
            int of        = 0;
            for (int iBuf = iBufFirst; iBuf <= iBufLast; iBuf++)
                {
                ReadBuffer buf = getBuffer(iBuf);
                int        cb  = buf.length();

                if (iBuf == iBufFirst)
                    {
                    int ofSrc = getBufferOffset(iBuf);  // ofSrc <= 0
                    buf = buf.getReadBuffer(of - ofSrc, cb + ofSrc);
                    cb += ofSrc;
                    }
                else if (iBuf == iBufLast)
                    {
                    buf = buf.getReadBuffer(0, cbThis - of);
                    cb = cbThis - of;
                    }

                if (!buf.equals(bufThat.getReadBuffer(of, cb)))
                    {
                    return false;
                    }

                of += cb;
                }

            return true;
            }

        return false;
        }


    // ----- factory methods ------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected ReadBuffer instantiateReadBuffer(int of, int cb)
        {
        checkBounds(of, cb);
        if (cb == 0)
            {
            return NO_BINARY;
            }

        // calculate which underlying buffers will compose the new
        // MultiBufferReadBuffer
        int iBufFirst = getBufferIndexByOffset(of);
        int iBufLast  = getBufferIndexByOffset(of + cb - 1);

        // adjust offset to be relative to the first buffer that will compose
        // the new MultiBufferReadBuffer
        of -= getBufferOffset(iBufFirst);

        // check if the new buffer could be created without the use of the
        // MultiBufferReadBuffer implementation (i.e. it's a "single" buffer)
        if (iBufFirst == iBufLast)
            {
            ReadBuffer buf = getBuffer(iBufFirst);
            return cb == buf.length() ? buf : buf.getReadBuffer(of, cb);
            }

        // otherwise, build the list of underlying ReadBuffers that the new
        // MultiBufferReadBuffer will be composed of
        int          cBuffers = iBufLast - iBufFirst + 1;
        ReadBuffer[] abuf     = new ReadBuffer[cBuffers];
        int[]        aof      = new int[cBuffers];
        int          cbTotal  = 0;
        for (int i = 0; i < cBuffers; ++i)
            {
            ReadBuffer buf = getBuffer(iBufFirst + i);
            abuf[i]  = buf;
            aof [i]  = cbTotal;
            cbTotal += buf.length();
            }

        return new MultiBufferReadBuffer(abuf, aof, of, of + cb);
        }

    /**
    * {@inheritDoc}
    */
    protected BufferInput instantiateBufferInput()
        {
        return instantiateBufferInput(/*fDestructive*/ false);
        }

    /**
    * Factory method: Instantiate a BufferInput object to read data from the
    * ReadBuffer.
    *
    * @param fDestructive  true iff the BufferInput should self-destruct as it
    *                      is advanced
    *
    * @return a new BufferInput reading from this ReadBuffer
    */
    protected BufferInput instantiateBufferInput(boolean fDestructive)
        {
        return new MultiBufferInput(fDestructive);
        }

    // ----- inner class: MultiBufferInput ----------------------------------

    /**
    * An implementation of the BufferInput interface that is backed by a
    * series of the underlying ReadBuffer BufferInput objects.
    */
    public final class MultiBufferInput
            extends AbstractBufferInput
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public MultiBufferInput()
            {
            this(/*fDestructive*/ false);
            }

        /**
        * Default constructor.
        *
        * @param fDestructive  true iff the stream should self-destruct as it
        *                      is advanced
        */
        public MultiBufferInput(boolean fDestructive)
            {
            m_fDestructive = fDestructive;

            // initialize the stream
            sync();
            }

        // ----- InputStreaming methods ---------------------------------

        /**
        * {@inheritDoc}
        */
        public int read()
                throws IOException
            {
            int b;

            BufferInput in = m_in;
            if (in.available() >= 1)
                {
                b = in.read();
                adjustOffsetInternal(1);
                }
            else
                {
                b = super.read();
                sync();
                }

            return b;
            }

        /**
        * {@inheritDoc}
        */
        public int read(byte ab[], int of, int cb)
                throws IOException
            {
            int cbActual;

            BufferInput in = m_in;
            if (in.available() >= cb)
                {
                cbActual = in.read(ab, of, cb);
                assert cbActual == cb;
                adjustOffsetInternal(cbActual);
                }
            else
                {
                cbActual = super.read(ab, of, cb);
                sync();
                }

            return cbActual;
            }

        /**
        * {@inheritDoc}
        */
        public void reset()
                throws IOException
            {
            int ofMark = getMarkInternal();
            if (ofMark < 0)
                {
                throw new IOException("not marked");
                }

            // optimization: does the reset of the location occur within
            // the current buffer?
            BufferInput in = getIn();
            int         of = getOffset();
            if (of > ofMark)
                {
                int cbRewind  = of - ofMark;
                int ofCurrent = in.getOffset();
                if (cbRewind < ofCurrent)
                    {
                    in.setOffset(ofCurrent - cbRewind);
                    adjustOffsetInternal(-cbRewind);
                    return;
                    }
                }
            else if (of < ofMark)
                {
                int cbForward = ofMark - of;
                if (cbForward < in.available())
                    {
                    in.skipBytes(cbForward);
                    adjustOffsetInternal(cbForward);
                    return;
                    }
                }
            else
                {
                return;
                }

            super.reset();
            sync();
            }

        // ----- DataInput methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        public int skipBytes(int cb)
                throws IOException
            {
            int cbActual;

            BufferInput in = m_in;
            if (in.available() >= cb)
                {
                cbActual = in.skipBytes(cb);
                assert cbActual == cb;
                adjustOffsetInternal(cbActual);
                }
            else
                {
                cbActual = super.skipBytes(cb);
                sync();
                }

            return cbActual;
            }

        /**
        * {@inheritDoc}
        */
        public byte readByte()
                throws IOException
            {
            byte b;

            BufferInput in = m_in;
            if (in.available() >= 1)
                {
                b = in.readByte();
                adjustOffsetInternal(1);
                }
            else
                {
                b = super.readByte();
                sync();
                }

            return b;
            }

        /**
        * {@inheritDoc}
        */
        public short readShort()
                throws IOException
            {
            short n;

            BufferInput in = m_in;
            if (in.available() >= 2)
                {
                n = in.readShort();
                adjustOffsetInternal(2);
                }
            else
                {
                n = super.readShort();
                sync();
                }

            return n;
            }

        /**
        * {@inheritDoc}
        */
        public int readUnsignedShort()
                throws IOException
            {
            int n;

            BufferInput in = m_in;
            if (in.available() >= 2)
                {
                n = in.readUnsignedShort();
                adjustOffsetInternal(2);
                }
            else
                {
                n = super.readUnsignedShort();
                sync();
                }

            return n;
            }

        /**
        * {@inheritDoc}
        */
        public char readChar()
                throws IOException
            {
            char ch;

            BufferInput in = m_in;
            if (in.available() >= 2)
                {
                ch = in.readChar();
                adjustOffsetInternal(2);
                }
            else
                {
                ch = super.readChar();
                sync();
                }

            return ch;
            }

        /**
        * {@inheritDoc}
        */
        public int readInt()
                throws IOException
            {
            int n;

            BufferInput in = getIn();
            if (in.available() >= 4)
                {
                n = in.readInt();
                adjustOffsetInternal(4);
                }
            else
                {
                n = super.readInt();
                sync();
                }

            return n;
            }

        /**
        * {@inheritDoc}
        */
        public long readLong()
                throws IOException
            {
            long l;

            BufferInput in = m_in;
            if (in.available() >= 8)
                {
                l = in.readLong();
                adjustOffsetInternal(8);
                }
            else
                {
                l = super.readLong();
                sync();
                }

            return l;
            }

        /**
        * {@inheritDoc}
        */
        public float readFloat()
                throws IOException
            {
            float fl;

            BufferInput in = m_in;
            if (in.available() >= 4)
                {
                fl = in.readFloat();
                adjustOffsetInternal(4);
                }
            else
                {
                fl = super.readFloat();
                sync();
                }

            return fl;
            }

        /**
        * {@inheritDoc}
        */
        public double readDouble()
                throws IOException
            {
            double dfl;

            BufferInput in = m_in;
            if (in.available() >= 8)
                {
                dfl = in.readDouble();
                adjustOffsetInternal(8);
                }
            else
                {
                dfl = super.readDouble();
                sync();
                }

            return dfl;
            }

        /**
        * {@inheritDoc}
        */
        public String readUTF()
                throws IOException
            {
            BufferInput in      = m_in;
            int         cbAvail = in.available();
            int         cbChars;
            if (cbAvail >= 2)
                {
                int ofBefore = in.getOffset();
                cbChars = in.readUnsignedShort();
                int cbTotal = 2 + cbChars;
                if (cbAvail >= cbTotal)
                    {
                    in.setOffset(ofBefore);
                    String s = in.readUTF();
                    adjustOffsetInternal(cbTotal);
                    return s;
                    }
                else
                    {
                    // not enough bytes left to read the String, so update
                    // the offset to reflect that we read the String length
                    adjustOffsetInternal(2);
                    }
                }
            else
                {
                cbChars = readUnsignedShort();
                }

            // do a virtual read of the String itself (i.e. across a buffer
            // boundary)
            String s = readUTF(cbChars);
            sync();
            return s;
            }

        // ----- BufferInput methods ------------------------------------

        /**
        * {@inheritDoc}
        */
        public String readSafeUTF()
                throws IOException
            {
            BufferInput in      = m_in;
            int         cbAvail = in.available();
            int         cbChars;
            if (cbAvail >= 5)
                {
                int ofBefore = in.getOffset();
                cbChars  = in.readPackedInt(); // WARNING: -1 == null String
                int cbLength = in.getOffset() - ofBefore;
                int cbTotal  = cbLength + cbChars;
                if (cbChars > 0 && cbAvail >= cbTotal)
                    {
                    in.setOffset(ofBefore);
                    String s = in.readSafeUTF();
                    adjustOffsetInternal(cbTotal);
                    return s;
                    }
                else
                    {
                    // not enough bytes left to read the String, so update
                    // the offset to reflect that we read the String length
                    adjustOffsetInternal(cbLength);
                    }
                }
            else
                {
                cbChars = readPackedInt();
                }

            // do a virtual read of the String itself (i.e. across a buffer
            // boundary)
            String s = readUTF(cbChars);
            if (cbChars > 0)
                {
                sync();
                }
            return s;
            }

        /**
        * {@inheritDoc}
        */
        public int readPackedInt()
                throws IOException
            {
            int n;

            BufferInput in = m_in;
            if (in.available() >= 5)
                {
                int of = in.getOffset();
                n = in.readPackedInt();
                adjustOffsetInternal(in.getOffset() - of);
                }
            else
                {
                n = super.readPackedInt();
                sync();
                }

            return n;
            }

        /**
        * {@inheritDoc}
        */
        public long readPackedLong()
                throws IOException
            {
            long l;

            BufferInput in = m_in;
            if (in.available() >= 10)
                {
                int of = in.getOffset();
                l = in.readPackedLong();
                adjustOffsetInternal(in.getOffset() - of);
                }
            else
                {
                l = super.readPackedLong();
                sync();
                }

            return l;
            }

        /**
        * {@inheritDoc}
        */
        public ReadBuffer readBuffer(int cb)
                throws IOException
            {
            ReadBuffer buf;

            BufferInput in = m_in;
            if (in.available() >= cb)
                {
                buf = in.readBuffer(cb);
                adjustOffsetInternal(cb);
                }
            else
                {
                buf = super.readBuffer(cb);
                sync();
                }

            return buf;
            }

        /**
        * {@inheritDoc}
        */
        public void setOffset(int of)
            {
            // optimization: is the offset within the current buffer?
            BufferInput in    = getIn();
            int         ofCur = getOffset();
            if (ofCur > of)
                {
                int cbRewind  = ofCur - of;
                int ofCurrent = in.getOffset();
                if (cbRewind < ofCurrent)
                    {
                    in.setOffset(ofCurrent - cbRewind);
                    adjustOffsetInternal(-cbRewind);
                    return;
                    }
                }
            else if (ofCur < of)
                {
                int cbForward = of - ofCur;
                try
                    {
                    if (cbForward < in.available())
                        {
                        in.skipBytes(cbForward);
                        adjustOffsetInternal(cbForward);
                        return;
                        }
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            else
                {
                return;
                }

            super.setOffset(of);
            sync();
            }


        // ----- internal -----------------------------------------------

        /**
        * Obtain the underlying BufferOutput.
        *
        * @return the underlying BufferOutput
        */
        protected BufferInput getIn()
            {
            return m_in;
            }

        /**
        * After traversing an underlying WriteBuffer boundary, or otherwise
        * changing the offset significantly, sync between this BufferOutput's
        * absolute position and an underlying BufferOutput's relative
        * position.
        */
        protected void sync()
            {
            MultiBufferReadBuffer bufMulti = MultiBufferReadBuffer.this;

            // absolute offset of this BufferInput
            int of = getOffset();

            // find the underlying WriteBuffer for that offset
            int        iBuf = bufMulti.getBufferIndexByOffset(of);
            ReadBuffer buf  = bufMulti.getBuffer(iBuf);

            // convert the absolute offset to the underlying buffer's
            // relative offset
            of -= bufMulti.getBufferOffset(iBuf);

            BufferInput inPrev = m_in;
            if (inPrev != null && buf == inPrev.getBuffer())
                {
                // still inside the previous underlying ReadBuffer
                inPrev.setOffset(of);
                }
            else
                {
                // traversed to the next (or some subsequent) underlying
                // ReadBuffer; if this buffer supports destructive streaming,
                // then release any previously streamed sub-buffers
                if (m_fDestructive)
                    {
                    int ofMark = getMarkInternal();
                    if (ofMark >= 0)
                        {
                        // mark is in place; only allow destruction before
                        // the buffer containing the mark
                        iBuf = Math.min(iBuf,
                                bufMulti.getBufferIndexByOffset(ofMark) - 1);
                        }

                    // release previous buffers
                    while (--iBuf >= 0 && bufMulti.releaseBuffer(iBuf) != null)
                        {
                        }
                    }

                // store the new underlying BufferInput and adjust the offset
                BufferInput in = buf.getBufferInput();
                m_in = in;
                in.setOffset(of);
                }
            }

        // ----- data members -------------------------------------------

        /**
        * The current underlying BufferInput object.
        */
        private BufferInput m_in;

        /**
        * True if the BufferInput set to self-destruct.
        */
        protected boolean m_fDestructive;
        }


    // ----- internal -------------------------------------------------------

    /**
    * Determine the number of ReadBuffer objects that contain the data
    * presented by this MultiBufferReadBuffer.
    *
    * @return the count of underlying ReadBuffer objects
    */
    protected int getBufferCount()
        {
        return m_abuf.length;
        }

    /**
    * Determine the offset of the specified buffer. The offset of a buffer
    * is the absolute offset of the first byte stored in the buffer.
    *
    * @param iBuffer  an index <tt>0 &lt;= iBuffer &lt; getBufferCount()</tt>
    *
    * @return the absolute offset of the first byte of the specified
    *         ReadBuffer
    */
    protected int getBufferOffset(int iBuffer)
        {
        return m_aofBuffer[iBuffer] - m_ofStart;
        }

    /**
    * Obtain the specified buffer.
    *
    * @param iBuffer  an index <tt>0 &lt;= iBuffer &lt; getBufferCount()</tt>
    *
    * @return the specified ReadBuffer
    */
    protected ReadBuffer getBuffer(int iBuffer)
        {
        ReadBuffer buf = m_abuf[iBuffer];
        if (buf == null)
            {
            throw new IndexOutOfBoundsException(
                    "the requested buffer '" + iBuffer + "' has been released");
            }
        return buf;
        }

    /**
    * Release the specified buffer.
    *
    * Once released any operation requiring access to overall buffer segment
    * maintained by said buffer will result in an error.  This method allows
    * for "destructive streaming", see #getDestructiveBufferInput()
    *
    * @param iBuffer  an index <tt>0 &lt;= iBuffer &lt; getBufferCount()</tt>
    *
    * @return the released buffer
    */
    protected ReadBuffer releaseBuffer(int iBuffer)
        {
        ReadBuffer[] abuf = m_abuf;
        ReadBuffer   buf  = m_abuf[iBuffer];

        abuf[iBuffer] = null;

        return buf;
        }

    /**
    * Determine which underlying ReadBuffer contains the specified offset.
    *
    * @param of  an offset into this MultiBufferReadBuffer
    *
    * @return the index of the ReadBuffer containing the specified offset
    */
    protected int getBufferIndexByOffset(int of)
        {
        int[] aof      = m_aofBuffer;
        int   cBuffers = aof.length;
        if (cBuffers == 1)
            {
            // since there is only one buffer, the offset occurs within it
            return 0;
            }

        // adjust offset to create an absolute offset into the virtual
        // ReadBuffer composed of all the underlying ReadBuffer objects
        of += m_ofStart;

        // optimization: use previous "cached" result, and check both that
        // buffer and the buffer after it (assuming there is buffer by
        // buffer forward progress)
        int iBuf  = 0;      // "closest" node from the binary search
        int iLow  = 0;      // "left-most" node for the binary search
        boolean fFound = false;
        if (of >= m_ofLastOffset)
            {
            for (iBuf = m_iBufLastAnswer, iLow = iBuf + 2; iBuf < iLow; ++iBuf)
                {
                if (iBuf + 1 >= cBuffers || of < aof[iBuf+1])
                    {
                    fFound = true;
                    break;
                    }
                }
            }

        if (!fFound)
            {
            // brute-force binary search through the array of offsets
            int iHigh = cBuffers - 1;
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
                    while (iBuf < iHigh && ofRoot == aof[iBuf + 1])
                        {
                        // COH-5507 : skip over any empty buffers
                        iBuf += 1;
                        }
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
                    iBuf = iRoot;
                    }
                }
            }

        // update "cache"
        m_ofLastOffset   = of;
        m_iBufLastAnswer = iBuf;

        return iBuf;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The array of all ReadBuffer objects allocated to store the contents
    * of this MultiBufferReadBuffer.
    */
    private final ReadBuffer[] m_abuf;

    /**
    * An array of absolute offsets, each corresponding to the first byte
    * stored in the corresponding ReadBuffer object.
    */
    private final int[] m_aofBuffer;

    /**
    * The starting offset of this ReadBuffer. Basically, if there were a
    * virtual ReadBuffer composed of all the contents of all the underlying
    * ReadBuffers in {@link #m_abuf}, then this is the offset into that
    * virtual ReadBuffer.
    */
    private final int m_ofStart;

    /**
    * The ending offset of this ReadBuffer. Basically, if there were a
    * virtual ReadBuffer composed of all the contents of all the underlying
    * ReadBuffers in {@link #m_abuf}, then this is the offset into that
    * ReadBuffer of the first byte that this MultiBufferReadBuffer does not
    * permit access to; i.e. it is the "exclusive" ending offset.
    */
    private final int m_ofEnd;

    /**
    * Cached "last offset looked up" value.
    */
    private transient int m_ofLastOffset;

    /**
    * Cached "last buffer index answer" value.
    */
    private transient int m_iBufLastAnswer;
    }
