/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import java.io.InputStream;
import java.io.IOException;
import java.io.DataInput;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.UTFDataFormatException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
* An InputStream implementation on top of a BufferSequence.
*
* @author cp/mf  2010.12.08
*/
public class BufferSequenceInputStream
        extends InputStream
        implements DataInput
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a BufferSequenceInputStream over a BufferSequence object.
    *
    * If it is desired for the BufferSequence to be disposed of when the stream
    * is closed then {@link #BufferSequenceInputStream(BufferSequence, boolean)}
    * should be used.
    *
    * @param bufseq  the BufferSequence to read the data from
    */
    public BufferSequenceInputStream(BufferSequence bufseq)
        {
        this (bufseq, false);
        }

    /**
     * Construct a BufferSequenceInputStream over a BufferSequence object.
     *
     * @param bufseq        the BufferSequence to read the data from
     * @param fAutoDispose  true if the sequence should be disposed of when the stream is closed,
     *                      or an unmarked stream is fully consumed
     */
    public BufferSequenceInputStream(BufferSequence bufseq, boolean fAutoDispose)
        {
        m_bufseq       = bufseq;
        m_cb           = bufseq.getLength();
        m_cbMark       = m_cb;
        f_fAutoDispose = fAutoDispose;
        }

    // ----- BufferSequenceInputStream interface ----------------------------

    /**
     * Fill the supplied buffer using the contents of the stream.
     *
     * @param bufDst  the buffer to fill, the position will be updated per {@link ByteBuffer#put}
     */
    public void read(ByteBuffer bufDst)
        {
        for (ByteBuffer bufUnsafe = ensureBuffer(); bufDst.hasRemaining() && bufUnsafe != null; bufUnsafe = ensureBuffer())
            {
            int nPosSrc = m_nPosBuf;
            int cbCopy  = Math.min(m_nLimBuf - nPosSrc,  bufDst.remaining());

            consumeBytes(cbCopy);
            Buffers.copy(bufUnsafe, nPosSrc, cbCopy, bufDst);
            m_nPosBuf += cbCopy;
            }
        }

    /**
     * Return the current buffer which is being read from.
     * <p>
     * Note that the validity of the returned ByteBuffer is tied to that of the BufferSequence.
     * As this is the buffer currently being consumed by the stream any positional changes made
     * to this buffer may invalidate the stream, thus in general it should either be used for
     * "peeking", or just before closing the stream.
     *
     * @return the current buffer which is being read from.
     */
    public ByteBuffer getCurrentBuffer()
        {
        ByteBuffer bufUnsafe = m_bufUnsafe;
        if (bufUnsafe == null)
            {
            bufUnsafe = ensureBuffer();
            }
        bufUnsafe = bufUnsafe.duplicate();
        bufUnsafe.position(m_nPosBuf).limit(m_nLimBuf);

        return bufUnsafe;
        }

    /**
     * Reset the stream to operate on a new BufferSequence.
     * <p>
     * This is the equivalent of closing this stream and opening a new one.
     * </p>
     *
     * @param bufseq the new sequence to operate against
     *
     * @return this stream
     */
    public BufferSequenceInputStream reset(BufferSequence bufseq)
        {
        close();

        m_bufseq    = bufseq;
        m_cb        = bufseq.getLength();
        m_cbMark    = m_cb;
        m_ofNext    = 0;
        m_ofMark    = 0;
        m_posMark   = 0;
        m_bufUnsafe = Buffers.getEmptyBuffer();
        m_cbLimit   = 0;
        m_nPosBuf   = 0;
        m_nLimBuf   = 0;

        return this;
        }


    // ----- DataInput interface --------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFully(byte[] ab)
            throws IOException
        {
        readFully(ab, 0, ab.length);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFully(byte[] ab, int off, int len)
            throws IOException
        {
        if (read(ab, off, len) < len)
            {
            throw new EOFException();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int skipBytes(int n)
            throws IOException
        {
        return (int) skip(n);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean readBoolean()
            throws IOException
        {
        int n = read();
        if (n < 0)
            {
            throw new EOFException();
            }
        return n != 0;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte readByte()
            throws IOException
        {
        int n = read();
        if (n < 0)
            {
            throw new EOFException();
            }
        return (byte) n;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUnsignedByte()
            throws IOException
        {
        int n = read();
        if (n < 0)
            {
            throw new EOFException();
            }
        return n;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort()
            throws IOException
        {
        ByteBuffer buf = ensureBuffer(2);
        return buf.getShort(buf == m_bufTmp ? 0 : m_nPosBuf - 2);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUnsignedShort()
            throws IOException
        {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + ch2;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar()
            throws IOException
        {
        ByteBuffer buf = ensureBuffer(2);
        return buf.getChar(buf == m_bufTmp ? 0 : m_nPosBuf - 2);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt()
            throws IOException
        {
        ByteBuffer buf = ensureBuffer(4);
        return buf.getInt(buf == m_bufTmp ? 0 : m_nPosBuf - 4);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong()
            throws IOException
        {
        ByteBuffer buf = ensureBuffer(8);
        return buf.getLong(buf == m_bufTmp ? 0 : m_nPosBuf - 8);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloat()
            throws IOException
        {
        ByteBuffer buf = ensureBuffer(4);
        return buf.getFloat(buf == m_bufTmp ? 0 : m_nPosBuf - 4);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDouble()
            throws IOException
        {
        ByteBuffer buf = ensureBuffer(8);
        return buf.getDouble(buf == m_bufTmp ? 0 : m_nPosBuf - 8);
        }

    /**
     * The <tt>readLine</tt> functionality was {@link DataInputStream#readLine
     * deprecated} as of JDK 1.1.  This implementation will always throw an
     * UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    @Deprecated
    public String readLine()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readUTF()
            throws IOException
        {
        // Note: implementation borrowed from java.io.DataInputStream

        int utflen = readUnsignedShort();
        byte[] bytearr = new byte[utflen];
        char[] chararr = new char[utflen];

        int c, char2, char3;
        int count = 0;
        int chararr_count = 0;

        readFully(bytearr, 0, utflen);

        while (count < utflen)
            {
            c = (int) bytearr[count] & 0xff;
            if (c > 127)
                {
                break;
                }
            count++;
            chararr[chararr_count++] = (char) c;
            }

        while (count < utflen)
            {
            c = (int) bytearr[count] & 0xff;
            switch (c >> 4)
                {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    /* 0xxxxxxx*/
                    count++;
                    chararr[chararr_count++] = (char) c;
                    break;
                case 12:
                case 13:
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if (count > utflen)
                        {
                        throw new UTFDataFormatException(
                                "malformed input: partial character at end");
                        }
                    char2 = (int) bytearr[count - 1];
                    if ((char2 & 0xC0) != 0x80)
                        {
                        throw new UTFDataFormatException(
                                "malformed input around byte " + count);
                        }
                    chararr[chararr_count++] = (char) (((c & 0x1F) << 6) |
                            (char2 & 0x3F));
                    break;
                case 14:
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if (count > utflen)
                        {
                        throw new UTFDataFormatException(
                                "malformed input: partial character at end");
                        }
                    char2 = (int) bytearr[count - 2];
                    char3 = (int) bytearr[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        {
                        throw new UTFDataFormatException(
                                "malformed input around byte " + (count - 1));
                        }
                    chararr[chararr_count++] = (char) (((c & 0x0F) << 12) |
                            ((char2 & 0x3F) << 6) | (char3 & 0x3F));
                    break;
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new UTFDataFormatException(
                            "malformed input around byte " + count);
                }
            }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
        }


    // ----- InputStream implementation -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int read()
            throws IOException
        {
        ByteBuffer buf = ensureBuffer();
        if (m_nPosBuf < m_nLimBuf)
            {
            consumeBytes(1);
            return ((int) buf.get(m_nPosBuf++)) & 0xFF;
            }

        return -1; // for read() and read(byte[]) we don't throw
        }

    /**
    * {@inheritDoc}
    */
    public int read(byte abDest[], int ofDest, int cbDest)
            throws IOException
        {
        if (abDest == null || ofDest < 0 || cbDest < 0 || ofDest + cbDest > abDest.length)
            {
            if (abDest == null)
                {
                throw new IllegalArgumentException("null byte array");
                }
            else
                {
                throw new IllegalArgumentException(
                        "abDest.length=" + abDest.length + ", ofDest=" + ofDest + ", cbDest=" + cbDest);
                }
            }

        int cbResult = 0;
        do
            {
            ByteBuffer bufDupe = ensureBuffer().duplicate(); // duplicate so we can use bulk-get below
            bufDupe.position(m_nPosBuf).limit(m_nLimBuf);

            int posStart = bufDupe.position();
            bufDupe.get(abDest, ofDest, Math.min(cbDest, bufDupe.remaining()));

            int cbRead = bufDupe.position() - posStart;
            if (cbRead == 0 && m_cb == 0)
                {
                break; // EOS
                }

            consumeBytes(cbRead); // must be in loop to allow for above check m_cb check to work

            m_nPosBuf += cbRead;
            cbResult  += cbRead;
            ofDest    += cbRead;
            cbDest    -= cbRead;
            }
        while (cbDest > 0);

        return cbResult == 0 && m_cb == 0 ? -1 : cbResult;
        }

    /**
    * {@inheritDoc}
    */
    public long skip(long lcb)
            throws IOException
        {
        if (lcb <= 0)
            {
            return 0;
            }

        int cb       = (int) Math.min(Integer.MAX_VALUE, lcb);
        int cbResult = 0;
        while (cb > 0)
            {
            ensureBuffer(); // we don't actually use the buffer, but this advances it if empty
            int cbSkip = Math.min(cb, m_nLimBuf - m_nPosBuf);

            if (cbSkip == 0 && m_cb == 0)
                {
                break;
                }

            consumeBytes(cbSkip); // must be in loop for above m_cb check

            m_nPosBuf += cbSkip;
            cb        -= cbSkip;
            cbResult  += cbSkip;
            }

        return cbResult;
        }

    /**
    * {@inheritDoc}
    */
    public int available()
            throws IOException
        {
        return (int) Math.min(Integer.MAX_VALUE, m_cb); // 0 on EOS
        }

    /**
    * {@inheritDoc}
    */
    public void mark(int readlimit)
        {
        ensureBuffer();
        m_cbLimit = readlimit;
        m_cbMark  = m_cb;
        m_ofMark  = m_ofNext - 1;
        m_posMark = m_nPosBuf;
        }

    /**
    * {@inheritDoc}
    */
    public void reset()
            throws IOException
        {
        long           cbMark = m_cbMark;
        BufferSequence bufseq = m_bufseq;
        long           cb     = m_cb;
        long           cbBack = cbMark - cb;

        if (cbBack == 0)
            {
            // noop, we only do this check in case we are doing a mark/reset on an auto-closed stream
            // we want to avoid it looking any different then having done it on an EOS
            return;
            }
        else if (cbBack > m_cbLimit)
            {
            // Note we could just treat the limit as a hint, but since we can auto-dispose from it we treat it
            // as a true limit in order to provide consistent behavior
            throw new IOException("Invalid mark, limit exceeded");
            }
        else if (bufseq == null) // explicitly closed stream
            {
            throw new IOException("Closed stream");
            }

        int of      = m_ofMark;
        m_bufUnsafe = bufseq.getUnsafeBuffer(of);
        m_nLimBuf   = bufseq.getBufferLimit(of);
        m_nPosBuf   = m_posMark;
        m_ofNext    = of + 1;
        m_cb        = cbMark;
        }

    /**
    * {@inheritDoc}
    */
    public boolean markSupported()
        {
        return true;
        }

    /**
    * {@inheritDoc}
    */
    public void close()
        {
        BufferSequence bufSeq = m_bufseq;

        m_bufUnsafe = null;
        m_bufseq    = null;
        m_bufTmp    = null;
        m_cb        = 0; // for subsequent read(byte[]) calls
        m_nLimBuf   = 0;
        m_nPosBuf   = 0;

        if (bufSeq != null && f_fAutoDispose)
            {
            bufSeq.dispose();
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Consume the specified number of bytes.
     *
     * @param cb the number of bytes consumed.
     */
    protected final void consumeBytes(int cb)
        {
        if ((m_cb -= cb) == 0 && f_fAutoDispose && m_cbMark > m_cbLimit)
            {
            close();
            }
        }

    /**
    * Obtain the next ByteBuffer that this InputStream is based on.
    *
     * The returned buffer will be big endian, and the caller must not modify the buffers positional properties, and use/update m_nPosBuf and m_nLimBuf instead.
    *
    * @return the underlying ByteBuffer
    */
    protected final ByteBuffer ensureBuffer()
        {
        BufferSequence bufseq = m_bufseq;
        if (bufseq == null)
            {
            m_nPosBuf = m_nLimBuf = 0;
            return Buffers.getEmptyBuffer();
            }

        ByteBuffer bufUnsafe  = m_bufUnsafe;
        int        of         = m_ofNext;
        int        cBuf       = bufseq.getBufferCount();
        while (m_nPosBuf == m_nLimBuf && of < cBuf)
            {
            bufUnsafe   = bufseq.getUnsafeBuffer(of);
            m_bufUnsafe = bufUnsafe.order() == ByteOrder.BIG_ENDIAN ? bufUnsafe : bufUnsafe.duplicate().order(ByteOrder.BIG_ENDIAN);
            m_nPosBuf   = bufseq.getBufferPosition(of);
            m_nLimBuf   = bufseq.getBufferLimit(of);
            ++of;
            }
        m_ofNext = of;

        return bufUnsafe;
        }


    /**
     * Helper function which returns a buffer containing at least the specified number of readable bytes which
     * will have already been logically consumed from the stream.
     *
     * If the returned buffer == m_bufTmp then a read may be performed from position 0, otherwise the read
     * must be at location m_nPos - cb.
     *
     * @param cb  the number of bytes to read, maximum of 8
     *
     * @return the temp buffer
     */
    private final ByteBuffer ensureBuffer(int cb)
        throws IOException
        {
        int        cbUnsafe  = m_nLimBuf - m_nPosBuf;
        ByteBuffer bufUnsafe = m_bufUnsafe;

        if (cbUnsafe == 0)
            {
            bufUnsafe = ensureBuffer();
            cbUnsafe  = m_nLimBuf - m_nPosBuf;
            }

        if (cbUnsafe >= cb)
            {
            consumeBytes(cb);
            m_nPosBuf += cb;
            return bufUnsafe;
            }
        else
            {
            ByteBuffer buffTmp = m_bufTmp;
            if (buffTmp == null)
                {
                m_bufTmp = buffTmp = ByteBuffer.allocate(8);
                }

            buffTmp.position(0).limit(cb); // will throw if cb > 8

            readFully(buffTmp.array(), 0, cb); // updates m_nPosBuf

            return buffTmp;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The BufferSequence object from which data is read.
     */
    protected BufferSequence m_bufseq;

    /**
     * A temporary byte buffer which can be used for decoding purposes.
     */
    private ByteBuffer m_bufTmp;

    /**
     * The number of bytes remaining in the stream.
     */
    protected long m_cb;

    /**
     * The offset of the next ByteBuffer to read from.
     */
    protected int m_ofNext;

    /**
     * The offset of the buffer associated with the mark.
     */
    protected int m_ofMark;

    /**
     * The buffer position of the buffer associated with the mark.
     */
    protected int m_posMark;

    /**
     * The number of bytes remaining in the stream after the mark.
     */
    protected long m_cbMark;

    /**
     * The limit associated with the mark.
     */
    protected int m_cbLimit;

    /**
     * True if the sequence should be disposed of when the stream is closed.
     */
    protected final boolean f_fAutoDispose;

    /**
     * The ByteBuffer object from which data is read.
     */
    protected ByteBuffer m_bufUnsafe = Buffers.getEmptyBuffer();

    /**
     * The position in the m_bufUnsafe
     */
    protected int m_nPosBuf;

    /**
     * the limit in m_bufUnsafe;
     */
    protected int m_nLimBuf;
    }
