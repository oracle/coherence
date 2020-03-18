/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;


/**
* The AbstractWriteBuffer is a partial implementation of the WriteBuffer
* interface intended to be used as a base class for easily creating concrete
* WriteBuffer implementations.
* <p>
* This implementation is explicitly not thread-safe.
*
* @author cp  2005.03.23 created
*/
public abstract class AbstractWriteBuffer
        implements WriteBuffer
    {
    // ----- buffer write operations ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public abstract void write(int ofDest, byte b);

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, byte[] abSrc)
        {
        write(ofDest, abSrc, 0, abSrc.length);
        }

    /**
    * {@inheritDoc}
    */
    public abstract void write(int ofDest, byte[] abSrc, int ofSrc, int cbSrc);

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, ReadBuffer bufSrc)
        {
        write(ofDest, bufSrc, 0, bufSrc.length());
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, ReadBuffer bufSrc, int ofSrc, int cbSrc)
        {
        // this is inefficient; sub-classes should override this
        write(ofDest, bufSrc.toByteArray(ofSrc, cbSrc), 0, cbSrc);
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, InputStreaming stream)
            throws IOException
        {
        copyStream(ofDest, stream, getMaximumCapacity() - ofDest);
        }

    /**
    * {@inheritDoc}
    */
    public void write(int ofDest, InputStreaming stream, int cbSrc)
            throws IOException
        {
        // see if it is a known implementation that we can optimize for
        if (stream instanceof ReadBuffer.BufferInput)
            {
            copyBufferInputPortion(ofDest, (ReadBuffer.BufferInput) stream, cbSrc);
            return;
            }

        byte[] abTmp = tmpbuf(cbSrc);
        int    cbTmp = abTmp.length;

        int    cbWritten = 0;
        while (cbWritten < cbSrc)
            {
            int cbActual = stream.read(abTmp, 0, Math.min(cbSrc - cbWritten, cbTmp));
            if (cbActual < 0)
                {
                throw new EOFException("requested to read " + cbSrc
                        + " bytes but only " + cbWritten + " bytes were available");
                }

            write(ofDest + cbWritten, abTmp, 0, cbActual);
            cbWritten += cbActual;
            }
        }


    // ----- buffer maintenance ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public abstract int length();

    /**
    * {@inheritDoc}
    */
    public void retain(int of)
        {
        retain(of, length() - of);
        }

    /**
    * {@inheritDoc}
    */
    public abstract void retain(int of, int cb);

    /**
    * {@inheritDoc}
    */
    public void clear()
        {
        retain(0, 0);
        releaseBuffers();
        }

    /**
    * {@inheritDoc}
    */
    public abstract int getCapacity();

    /**
    * {@inheritDoc}
    */
    public int getMaximumCapacity()
        {
        // assume non-resizing; sub-classes that support resizable buffers
        // must override this
        return getCapacity();
        }


    // ----- obtaining different "write views" to the buffer ----------------

    /**
    * {@inheritDoc}
    */
    public WriteBuffer getWriteBuffer(int of)
        {
        return getWriteBuffer(of, getMaximumCapacity() - of);
        }

    /**
    * {@inheritDoc}
    */
    public WriteBuffer getWriteBuffer(int of, int cb)
        {
        if (of < 0 || cb < 0 || of + cb > getMaximumCapacity())
            {
            throw new IndexOutOfBoundsException("of=" + of + ", cb=" + cb
                    + ", max=" + getMaximumCapacity());
            }

        return new DelegatingWriteBuffer(this, of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public BufferOutput getBufferOutput()
        {
        return getBufferOutput(0);
        }

    /**
    * {@inheritDoc}
    */
    public BufferOutput getBufferOutput(int of)
        {
        return new AbstractBufferOutput(of);
        }

    /**
    * {@inheritDoc}
    */
    public BufferOutput getAppendingBufferOutput()
        {
        return getBufferOutput(length());
        }


    // ----- accessing the buffered data ------------------------------------

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
    public abstract ReadBuffer getUnsafeReadBuffer();

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
        WriteBuffer buf = new ByteArrayWriteBuffer(getCapacity(), getMaximumCapacity());
        if (length () > 0)
            {
            buf.write(0, getUnsafeReadBuffer());
            }
        return buf;
        }


    // ----- internal -------------------------------------------------------

    /**
    * Read a portion of the specified BufferInput and write it to the
    * specified offset within this buffer.
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param inSrc   the BufferInput to read data from to copy to this buffer
    * @param cbSrc   the exact number of bytes to read from the passed
    *                BufferInput
    *
    * @exception IOException  if an IOException occurs reading from the
    *            passed stream
    */
    protected void copyBufferInputPortion(int ofDest, ReadBuffer.BufferInput inSrc, int cbSrc)
            throws IOException
        {
        int cbMax  = inSrc.available();
        int ofSrc  = inSrc.getOffset();
        int cbCopy = Math.min(cbSrc, cbMax);
        write(ofDest, inSrc.getBuffer(), ofSrc, cbCopy);
        inSrc.setOffset(ofSrc + cbCopy);
        if (cbSrc > cbMax)
            {
            throw new EOFException("instructed to copy " + cbSrc
                    + " bytes, but only " + cbMax + " were available");
            }
        }

    /**
    * Read the remaining contents of the specified BufferInput and write it
    * to the specified offset within this buffer.
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param inSrc   the BufferInput to read and store in this buffer
    * @param cbMax   the maximum number of bytes to copy
    *
    * @return the actual number of bytes read from the BufferInput and
    *         written to this buffer
    *
    * @exception IOException  if an IOException occurs reading from the
    *            passed stream or if the limit is reached without emptying
    *            the source stream
    */
    protected int copyBufferInputRemainder(int ofDest, ReadBuffer.BufferInput inSrc, int cbMax)
            throws IOException
        {
        int cbSrc  = inSrc.available();
        int cbCopy = Math.min(cbSrc, cbMax);
        copyBufferInputPortion(ofDest, inSrc, cbCopy);
        if (cbSrc > cbMax)
            {
            throw new IOException("Overflow: attempted to write " + cbSrc
                    + " bytes, but limited to " + cbMax + " bytes");
            }
        return cbCopy;
        }

    /**
    * Store the remaining contents of the specified InputStreaming object at
    * the specified offset within this buffer.
    *
    * @param ofDest   the offset within this buffer to store the passed data
    * @param stream   the stream of bytes to read and store in this buffer
    * @param cbMax    the maximum number of bytes to copy
    *
    * @return the actual number of bytes read from the InputStreaming object
    *         and written to this buffer
    *
    * @exception IOException  if an IOException occurs reading from the
    *            passed stream or if the limit is reached without emptying
    *            the source stream
    */
    protected int copyStream(int ofDest, InputStreaming stream, int cbMax)
            throws IOException
        {
        // see if it is a known implementation that we can optimize for
        if (stream instanceof ReadBuffer.BufferInput)
            {
            return copyBufferInputRemainder(ofDest, (ReadBuffer.BufferInput) stream, cbMax);
            }

        // remember where we started to be able to determine the number of
        // bytes that were copied
        int ofOrig   = ofDest;
        int cbRemain = cbMax;

        // we have to treat it as a generic stream;
        // first, grab whatever can be read without blocking
        int cbAvail = stream.available();
        if (cbAvail > 0)
            {
            byte[] abTmp = tmpbuf(cbAvail);
            int    cbTmp = abTmp.length;
            do
                {
                int cbActual = stream.read(abTmp, 0, Math.min(cbAvail, cbTmp));
                if (cbActual > 0)
                    {
                    write(ofDest, abTmp, 0, Math.min(cbActual, cbRemain));
                    ofDest   += cbActual;
                    cbRemain -= cbActual;

                    if (cbRemain < 0)
                        {
                        throw new IOException("Overflow: attempted to copy "
                                + cbActual + " available bytes, but limited to "
                                + cbMax + " bytes");
                        }
                    }

                cbAvail = stream.available();
                }
            while (cbAvail > 0);
            }

        // test if there is anything remaining in the stream
        int b = stream.read();
        if (b >= 0)
            {
            // there was at least one byte; write it to the WriteBuffer
            if (cbRemain > 0)
                {
                write(ofDest, (byte) b);
                }
            ++ofDest;
            --cbRemain;

            if (cbRemain < 0)
                {
                throw new IOException("Overflow: attempted to copy at least "
                        + (ofDest - ofOrig) + " bytes, but limited to "
                        + cbMax + " bytes");
                }

            // allocate a read buffer if we don't have a suitable one already
            byte[] abBlock = tmpbuf(MAX_BUF);
            int    cbBlock = abBlock.length;

            // drop into a default read/write loop to drain the stream
            while (true)
                {
                try
                    {
                    int cbActual = stream.read(abBlock, 0, cbBlock);
                    if (cbActual < 0)
                        {
                        break;
                        }
                    else if (cbActual > 0)
                        {
                        write(ofDest, abBlock, 0, Math.min(cbActual, cbRemain));
                        ofDest   += cbActual;
                        cbRemain -= cbActual;

                        if (cbRemain < 0)
                            {
                            throw new IOException(
                                    "Overflow: attempted to copy at least "
                                    + (ofDest - ofOrig)
                                    + " bytes, but limited to "
                                    + cbMax + " bytes");
                            }
                        }
                    }
                catch (EOFException e)
                    {
                    break;
                    }
                }
            }

        return ofDest - ofOrig;
        }

    /**
    * Get a small buffer for formating data to bytes.
    *
    * @return a byte array that is at least <tt>{@link #MIN_BUF}</tt>
    *         bytes long
    */
    protected byte[] tmpbuf()
        {
        byte[] ab = m_abBuf;
        if (ab == null)
            {
            m_abBuf = ab = new byte[MIN_BUF];
            }
        return ab;
        }

    /**
    * Get a buffer for formating data to bytes. Note that the resulting buffer
    * may be shorter than the requested size.
    *
    * @param cb  the requested size for the buffer
    *
    * @return a byte array that is at least <tt>cb</tt> bytes long, but not
    *         shorter than {@link #MIN_BUF} <b>and (regardless of the value of
    *         <tt>cb</tt>) not longer than {@link #MAX_BUF}</b>
    */
    protected byte[] tmpbuf(int cb)
        {
        byte[] ab = m_abBuf;
        if (ab == null || ab.length < cb)
            {
            int cbOld = ab == null ? 0 : ab.length;
            int cbNew = Math.max(MIN_BUF, Math.min(MAX_BUF , cb));
            if (cbNew > cbOld)
                {
                m_abBuf = ab = new byte[cbNew > (MAX_BUF >>> 1) ? MAX_BUF : cbNew];
                }
            }
        return ab;
        }

    /**
    * Release the internal buffers held by the WriteBuffer.
    */
    protected final void releaseBuffers()
        {
        m_abBuf  = null;
        m_achBuf = null;
        }


    // ----- inner class: AbstractBufferOutput ------------------------------

    /**
    * AbstractBufferOutput is a concrete implementation of BufferOutput for
    * the non-concrete AbstractWriteBuffer implementation.
    *
    * @author cp  2005.03.24
    */
    public class AbstractBufferOutput
            extends OutputStream
            implements WriteBuffer.BufferOutput
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct an AbstractBufferOutput that will begin writing at the
        * start of the containing WriteBuffer.
        */
        public AbstractBufferOutput()
            {
            }

        /**
        * Construct an AbstractBufferOutput that will begin writing at the
        * specified offset within the containing WriteBuffer.
        *
        * @param of  the offset at which to begin writing
        */
        public AbstractBufferOutput(int of)
            {
            setOffset(of);
            }

        // ----- OutputStreaming methods --------------------------------

        /**
        * {@inheritDoc}
        */
        public void write(int b)
                throws IOException
            {
            int ofWrite = m_ofWrite;
            AbstractWriteBuffer.this.write(ofWrite, (byte) b);
            m_ofWrite = ofWrite + 1;
            }

        /**
        * {@inheritDoc}
        */
        public void write(byte ab[])
                throws IOException
            {
            int ofWrite = m_ofWrite;
            AbstractWriteBuffer.this.write(ofWrite, ab);
            m_ofWrite = ofWrite + ab.length;
            }

        /**
        * {@inheritDoc}
        */
        public void write(byte ab[], int of, int cb)
                throws IOException
            {
            int ofWrite = m_ofWrite;
            AbstractWriteBuffer.this.write(ofWrite, ab, of, cb);
            m_ofWrite = ofWrite + cb;
            }

        /**
        * {@inheritDoc}
        */
        public void flush()
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void close()
                throws IOException
            {
            releaseBuffers();
            }

        // ----- DataOutput methods -------------------------------------

        /**
        * {@inheritDoc}
        */
        public void writeBoolean(boolean f)
                throws IOException
            {
            write(f ? 1 : 0);
            }

        /**
        * {@inheritDoc}
        */
        public void writeByte(int b)
                throws IOException
            {
            write(b);
            }

        /**
        * {@inheritDoc}
        */
        public void writeShort(int n)
                throws IOException
            {
            byte[] ab = tmpbuf();
            ab[0] = (byte) (n >>>  8);
            ab[1] = (byte) (n);
            write(ab, 0, 2);
            }

        /**
        * {@inheritDoc}
        */
        public void writeChar(int ch)
                throws IOException
            {
            writeShort(ch);
            }

        /**
        * {@inheritDoc}
        */
        public void writeInt(int n)
                throws IOException
            {
            byte[] ab = tmpbuf();
            ab[0] = (byte) (n >>> 24);
            ab[1] = (byte) (n >>> 16);
            ab[2] = (byte) (n >>>  8);
            ab[3] = (byte) (n);
            write(ab, 0, 4);
            }

        /**
        * {@inheritDoc}
        */
        public void writeLong(long l)
                throws IOException
            {
            byte[] ab = ExternalizableHelper.toByteArray(l, tmpbuf());

            write(ab, 0, 8);
            }

        /**
        * {@inheritDoc}
        */
        public void writeFloat(float fl)
                throws IOException
            {
            writeInt(Float.floatToIntBits(fl));
            }

        /**
        * {@inheritDoc}
        */
        public void writeDouble(double dfl)
                throws IOException
            {
            writeLong(Double.doubleToLongBits(dfl));
            }

        /**
        * {@inheritDoc}
        */
        public void writeBytes(String s)
                throws IOException
            {
            int    of    = 0;
            int    cb    = s.length();
            byte[] abTmp = tmpbuf(cb);
            int    cbTmp = abTmp.length;
            while (cb > 0)
                {
                int cbChunk = Math.min(cb, cbTmp);

                s.getBytes(of, of + cbChunk, abTmp, 0);
                write(abTmp, 0, cbChunk);

                of += cbChunk;
                cb -= cbChunk;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeChars(String s)
                throws IOException
            {
            int    cch    = s.length();
            char[] achTmp = getCharBuf();
            int    cchTmp = achTmp.length;
            int    cbTmp  = cchTmp << 1;
            byte[] abTmp  = tmpbuf(cbTmp);
            assert abTmp.length >= cbTmp;

            for (int of = 0; of < cch; of += cchTmp)
                {
                int cchChunk = Math.min(of + cchTmp, cch);
                s.getChars(of, cchChunk, achTmp, 0);
                for (int ofch = 0, ofb = 0; ofch < cchChunk; ++ofch)
                    {
                    int ch = achTmp[ofch];
                    abTmp[ofb++] = (byte) (ch >>>  8);
                    abTmp[ofb++] = (byte) (ch);
                    }
                write(abTmp, 0, cchChunk << 1);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeUTF(String s)
                throws IOException
            {
            int cch = s.length();
            if (cch == 0)
                {
                // 0-length UTF (Java UTF has a 2-byte length indicator)
                writeShort(0);
                }
            else
                {
                // calculate the length (in bytes) of the resulting UTF
                int cb = calcUTF(s);

                // Java UTF binary format has only 2 bytes for length
                if (cb > 0xFFFF)
                    {
                    throw new UTFDataFormatException("UTF binary length="
                            + cb + ", max=65535");
                    }

                // write the UTF header (the length as a 2-byte value)
                writeShort(cb);

                // write the characters
                writeUTF(s, cch, cb);
                }
            }

        // ----- BufferOutput methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public WriteBuffer getBuffer()
            {
            return AbstractWriteBuffer.this;
            }

        /**
        * {@inheritDoc}
        */
        public void writeSafeUTF(String s)
                throws IOException
            {
            if (s == null)
                {
                writePackedInt(-1);
                }
            else
                {
                int cch = s.length();
                if (cch == 0)
                    {
                    writePackedInt(0);
                    }
                else
                    {
                    // calculate the length (in bytes) of the resulting UTF
                    int cb = calcUTF(s);

                    // write the UTF header (the length)
                    writePackedInt(cb);

                    // write the characters
                    writeUTF(s, cch, cb);
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writePackedInt(int n)
                throws IOException
            {
            byte[] ab = tmpbuf();
            int    cb = 0;

            // first byte contains sign bit (bit 7 set if neg)
            int b = 0;
            if (n < 0)
                {
                b = 0x40;
                n = ~n;
                }

            // first byte contains only 6 data bits
            b |= (byte) (n & 0x3F);
            n >>>= 6;

            while (n != 0)
                {
                b |= 0x80;          // bit 8 is a continuation bit
                ab[cb++] = (byte) b;

                b = (n & 0x7F);
                n >>>= 7;
                }

            if (cb == 0)
                {
                // one-byte format
                write(b);
                }
            else
                {
                ab[cb++] = (byte) b;
                write(ab, 0, cb);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writePackedLong(long l)
                throws IOException
            {
            byte[] ab = tmpbuf();
            int    cb = 0;

            // first byte contains sign bit (bit 7 set if neg)
            int b = 0;
            if (l < 0)
                {
                b = 0x40;
                l = ~l;
                }

            // first byte contains only 6 data bits
            b |= (byte) (((int) l) & 0x3F);
            l >>>= 6;

            while (l != 0)
                {
                b |= 0x80;          // bit 8 is a continuation bit
                ab[cb++] = (byte) b;

                b = (((int) l) & 0x7F);
                l >>>= 7;
                }

            if (cb == 0)
                {
                // one-byte format
                write(b);
                }
            else
                {
                ab[cb++] = (byte) b;
                write(ab, 0, cb);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeBuffer(ReadBuffer buf)
                throws IOException
            {
            int ofWrite = m_ofWrite;
            AbstractWriteBuffer.this.write(ofWrite, buf);
            m_ofWrite = ofWrite + buf.length();
            }

        /**
        * {@inheritDoc}
        */
        public void writeBuffer(ReadBuffer buf, int of, int cb)
                throws IOException
            {
            int ofWrite = m_ofWrite;
            AbstractWriteBuffer.this.write(ofWrite, buf, of, cb);
            m_ofWrite = ofWrite + cb;
            }

        /**
        * {@inheritDoc}
        */
        public void writeStream(InputStreaming stream)
                throws IOException
            {
            int ofWrite = m_ofWrite;
            int cb = AbstractWriteBuffer.this.copyStream(ofWrite, stream,
                    getMaximumCapacity() - ofWrite);
            m_ofWrite = ofWrite + cb;
            }

        /**
        * {@inheritDoc}
        */
        public void writeStream(InputStreaming stream, int cb)
                throws IOException
            {
            int ofWrite = m_ofWrite;
            AbstractWriteBuffer.this.write(ofWrite, stream, cb);
            m_ofWrite = ofWrite + cb;
            }

        /**
        * {@inheritDoc}
        */
        public final int getOffset()
            {
            return m_ofWrite;
            }

        /**
        * {@inheritDoc}
        */
        public void setOffset(int of)
            {
            if (of < 0 || of > AbstractWriteBuffer.this.getMaximumCapacity())
                {
                throw new IndexOutOfBoundsException("of=" + of + ", min=0, max="
                        + AbstractWriteBuffer.this.getMaximumCapacity());
                }

            m_ofWrite = of;
            }

        // ----- internal -----------------------------------------------

        /**
        * Write out the characters of the passed String as UTF-8 data.
        *
        * @param s    the String
        * @param cch  the number of characters
        * @param cb   the number of bytes as returned from calcUTF
        *
        * @exception IOException  if an I/O error occurs
        */
        protected void writeUTF(String s, int cch, int cb)
                throws IOException
            {
            if (cb == cch)
                {
                // length of UTF-8 format is the same as the UTF-16 format
                // iff the characters are all in the range 0x01-0x7F
                writeBytes(s);
                }
            else
                {
                // get a temp buffer to load characters into from the String
                char[]  ach = getCharBuf();

                // get a temp buffer to write the UTF binary into
                byte[] abTmp = tmpbuf(cb);

                // go through the string, a chunk at a time
                for (int ofch = 0; ofch < cch; ofch += CHAR_BUF_SIZE)
                    {
                    int cchChunk = Math.min(CHAR_BUF_SIZE, cch - ofch);

                    // for small strings, i.e. those smaller than the
                    // CHAR_BUF_SIZE, the characters would already be loaded
                    // into the character buffer by calcUTF; otherwise load
                    // the next chunk
                    if (cch > CHAR_BUF_SIZE)
                        {
                        s.getChars(ofch, ofch + cchChunk, ach, 0);
                        }

                    int cbChunk = formatUTF(abTmp, 0, ach, cchChunk);
                    write(abTmp, 0, cbChunk);
                    }
                }
            }

        /**
        * Obtain a temp buffer used to avoid allocations from
        * {@link String#toCharArray()} and repeated calls to
        * {@link String#charAt(int)}.
        *
        * @return a char buffer of CHAR_BUF_SIZE characters long
        */
        protected final char[] getCharBuf()
            {
            // "partial" (i.e. windowed) char buffer just for writeUTF
            char[] ach = m_achBuf;
            if (ach == null)
                {
                m_achBuf = ach = new char[CHAR_BUF_SIZE];
                }
            return ach;
            }

        /**
        * Figure out how many bytes it will take to hold the passed String.
        * <p>
        * This method is tightly bound to formatUTF.
        *
        * @param s  the String
        *
        * @return the binary UTF length
        */
        protected final int calcUTF(String s)
            {
            int     cch    = s.length();
            int     cb     = cch;
            char[]  ach    = getCharBuf();
            boolean fSmall = (cch <= CHAR_BUF_SIZE);
            if (fSmall)
                {
                s.getChars(0, cch, ach, 0);
                }

            for (int ofch = 0; ofch < cch; ++ofch)
                {
                int ch;
                if (fSmall)
                    {
                    ch = ach[ofch];
                    }
                else
                    {
                    int ofBuf = ofch & CHAR_BUF_MASK;
                    if (ofBuf == 0)
                        {
                        s.getChars(ofch, Math.min(ofch + CHAR_BUF_SIZE, cch), ach, 0);
                        }
                    ch = ach[ofBuf];
                    }

                if (ch <= 0x007F)
                    {
                    // all bytes in this range use the 1-byte format
                    // except for 0
                    if (ch == 0)
                        {
                        ++cb;
                        }
                    }
                else
                    {
                    // either a 2-byte format or a 3-byte format (if over
                    // 0x07FF)
                    cb += (ch <= 0x07FF ? 1 : 2);
                    }
                }

            return cb;
            }

        /**
        * Format the passed String as UTF into the passed byte array.
        * <p>
        * This method is tightly bound to calcUTF.
        *
        * @param ab   the byte array to format into
        * @param ofb  the offset into the byte array to write the first byte
        * @param cb   the number of bytes that the UTF will take as returned
        *             by calcUTF
        * @param s    the String
        */
        protected final void formatUTF(byte[] ab, int ofb, int cb, String s)
            {
            int cch = s.length();
            if (cb == cch)
                {
                // ask the string to convert itself to ascii bytes
                // straight into the WriteBuffer
                s.getBytes(0, cch, ab, ofb);
                }
            else
                {
                char[]  ach = getCharBuf();
                if (cch <= CHAR_BUF_SIZE)
                    {
                    /*
                    * The following is unnecessary, because it would already
                    * have been performed by calcUTF:
                    *
                    *   if (fSmall)
                    *       {
                    *       s.getChars(0, cch, ach, 0);
                    *       }
                    */
                    formatUTF(ab, ofb, ach, cch);
                    }
                else
                    {
                    for (int ofch = 0; ofch < cch; ofch += CHAR_BUF_SIZE)
                        {
                        int cchChunk = Math.min(CHAR_BUF_SIZE, cch - ofch);
                        s.getChars(ofch, ofch + cchChunk, ach, 0);
                        ofb += formatUTF(ab, ofb, ach, cchChunk);
                        }
                    }
                }
            }

        /**
        * Format the passed characters as UTF into the passed byte array.
        *
        * @param ab    the byte array to format into
        * @param ofb   the offset into the byte array to write the first byte
        * @param ach   the array of characters to format
        * @param cch   the number of characters to format
        *
        * @return cb  the number of bytes written to the array
        */
        protected final int formatUTF(byte[] ab, int ofb, char[] ach, int cch)
            {
            int ofbOrig = ofb;
            for (int ofch = 0; ofch < cch; ++ofch)
                {
                char ch = ach[ofch];
                if (ch >= 0x0001 && ch <= 0x007F)
                    {
                    // 1-byte format:  0xxx xxxx
                    ab[ofb++] = (byte) ch;
                    }
                else if (ch <= 0x07FF)
                    {
                    // 2-byte format:  110x xxxx, 10xx xxxx
                    ab[ofb++] = (byte) (0xC0 | ((ch >>> 6) & 0x1F));
                    ab[ofb++] = (byte) (0x80 | ((ch      ) & 0x3F));
                    }
                else
                    {
                    // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                    ab[ofb++] = (byte) (0xE0 | ((ch >>> 12) & 0x0F));
                    ab[ofb++] = (byte) (0x80 | ((ch >>>  6) & 0x3F));
                    ab[ofb++] = (byte) (0x80 | ((ch       ) & 0x3F));
                    }
                }
            return ofb - ofbOrig;
            }

        // ----- data members -------------------------------------------

        /**
        * Current write offset within the containing WriteBuffer.
        */
        protected int m_ofWrite;
        }


    // ----- constants ------------------------------------------------------

    /**
    * An empty byte array (by definition immutable).
    */
    public static final byte[] NO_BYTES = AbstractByteArrayReadBuffer.NO_BYTES;

    /**
    * An empty Binary object.
    */
    public static final Binary NO_BINARY = AbstractByteArrayReadBuffer.NO_BINARY;

    /**
    * The minimum size of the temp buffer.
    */
    private static final int MIN_BUF = 0x40;

    /**
    * The maximum size of the temp buffer. The maximum size must be at least
    * <tt>(3 * CHAR_BUF_SIZE)</tt> to accomodate the worst-case UTF
    * formatting length.
    */
    private static final int MAX_BUF = 0x400;

    /**
    * Size of the temporary character buffer. Must be a power of 2.
    * <p>
    * Size is: 256 characters (.25 KB).
    */
    protected static final int CHAR_BUF_SIZE = 0x100;

    /**
    * Bitmask used against a raw offset to determine the offset within
    * the temporary character buffer.
    */
    protected static final int CHAR_BUF_MASK = (CHAR_BUF_SIZE - 1);


    // ----- data members ---------------------------------------------------

    /**
    * A temp buffer to use for building the data to write.
    */
    private transient byte[] m_abBuf;

    /**
    * A lazily instantiated temp buffer used to avoid allocations from
    * {@link String#toCharArray()} and repeated calls to
    * {@link String#charAt(int)}.
    */
    protected transient char[] m_achBuf;
    }
