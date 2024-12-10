/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import java.io.OutputStream;
import java.io.IOException;
import java.io.DataOutput;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.ArrayList;


/**
 * BufferSequenceOutputStream is an implementation of an OutputStream which
 * which produces a BufferSequence.
 *
 * @author mf  2010.12.08
 */
public class BufferSequenceOutputStream
        extends OutputStream
        implements DataOutput
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a BufferSequenceOutputStream.
     *
     * @param manager  the BufferManager to acquire buffers from
     */
    public BufferSequenceOutputStream(BufferManager manager)
        {
        this (manager, 0);
        }

    /**
     * Construct a BufferSequenceOutputStream.
     *
     * @param manager  the BufferManager to acquire buffers from
     * @param cb       the anticipated sequence size, or zero
     */
    public BufferSequenceOutputStream(BufferManager manager, long cb)
        {
        if (manager == null)
            {
            throw new IllegalArgumentException("manager cannot be null");
            }

        m_manager = manager;
        if (cb > 0)
            {
            int ncb = (int) cb;
            m_buffer = (ByteBuffer) manager.acquirePref(ncb < 0 ? Integer.MAX_VALUE : ncb).clear();
            }
        }


    // ----- BufferSequenceOutputStream interface ---------------------------

    /**
     * Write the specified buffer to the stream.
     * <p>
     * The positional properties of the buffer will be modified, and thus at
     * the completion of the operation src.remaining() will be zero.
     * </p>
     *
     * @param buf  the buffer to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeBuffer(ByteBuffer buf)
            throws IOException
        {
        writeBuffer(buf, 0);
        }

    /**
     * Write the specified BufferSequence to the stream.
     *
     * @param bufseq  the BufferSequence to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeBufferSequence(BufferSequence bufseq)
            throws IOException
        {
        long cbHint = bufseq.getLength();
        for (int i = 0, c = bufseq.getBufferCount(); i < c; ++i)
            {
            cbHint = writeBuffer(bufseq.getBuffer(i), cbHint);
            }
        }

    /**
     * Close the stream and return its contents as a BufferSequence.
     * <p>
     * It is the responsibility of the caller to eventually {@link
     * BufferSequence#dispose dispose} of the returned BufferSequence.
     *
     * @return the BufferSequence
     *
     * @throws IOException if an I/O error occurs
     */
    public BufferSequence toBufferSequence()
            throws IOException
        {
        BufferManager    manager     = m_manager;
        List<ByteBuffer> listBuffers = m_listBuffers;
        ByteBuffer       bufLast     = m_buffer;

        flush();

        m_listBuffers = null;
        m_buffer      = null;
        m_manager     = null;

        if (listBuffers == null)
            {
            // never flushed; avoid creating an unnecessary List
            if (bufLast == null || bufLast.position() == 0)
                {
                if (bufLast != null)
                    {
                    manager.release(bufLast);
                    }
                return Buffers.getEmptyBufferSequence();
                }
            bufLast.flip();
            return new SingleBufferSequence(manager, manager.truncate(bufLast));
            }
        else
            {
            bufLast.flip();
            listBuffers.add(manager.truncate(bufLast));
            return Buffers.createBufferSequence(manager, listBuffers.toArray(new ByteBuffer[listBuffers.size()]));
            }
        }


    // ----- DataOutput interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBoolean(boolean v)
            throws IOException
        {
        write(v ? 1 : 0);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(int v)
            throws IOException
        {
        write(v);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(int v)
            throws IOException
        {
        flush(ensureBuffer(Short.SIZE / 8).putShort((short) v));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(int v)
            throws IOException
        {
        flush(ensureBuffer(Character.SIZE / 8).putChar((char) v));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(int v)
            throws IOException
        {
        flush(ensureBuffer(Integer.SIZE / 8).putInt(v));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLong(long v)
            throws IOException
        {
        flush(ensureBuffer(Long.SIZE / 8).putLong(v));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFloat(float v)
            throws IOException
        {
        flush(ensureBuffer(Float.SIZE / 8).putFloat(v));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDouble(double v)
            throws IOException
        {
        flush(ensureBuffer(Double.SIZE / 8).putDouble(v));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(String s)
            throws IOException
        {
        int i = 0;
        int c = s.length();

        // optimize by writing in chunks, note BSOS is always big endian
        while (i <= c - 8)
            {
            long lch;
            lch  = ((long) (0x0FF & s.charAt(i++)) << 56);
            lch |= ((long) (0x0FF & s.charAt(i++)) << 48);
            lch |= ((long) (0x0FF & s.charAt(i++)) << 40);
            lch |= ((long) (0x0FF & s.charAt(i++)) << 32);
            lch |= ((long) (0x0FF & s.charAt(i++)) << 24);
            lch |= ((long) (0x0FF & s.charAt(i++)) << 16);
            lch |= ((long) (0x0FF & s.charAt(i++)) << 8);
            lch |=         (0x0FF & s.charAt(i++));

            writeLong(lch);
            }

        if (i <= c - 4)
            {
            int nch;
            nch  = ((0x0FF & s.charAt(i++)) << 24);
            nch |= ((0x0FF & s.charAt(i++)) << 16);
            nch |= ((0x0FF & s.charAt(i++)) << 8);
            nch |=  (0x0FF & s.charAt(i++));

            writeInt(nch);
            }

        if (i <= c - 2)
            {
            int nch;
            nch  = ((0x0FF & s.charAt(i++)) << 8);
            nch |=  (0x0FF & s.charAt(i++));

            writeShort(nch);
            }

        if (i < c)
            {
            writeByte(s.charAt(i));
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChars(String s)
            throws IOException
        {
        int i = 0;
        int c = s.length();

        // optimize by writing in chunks, note BSOS is always big endian
        while (i <= c - 4)
            {
            long lch;
            lch  = ((long) (0x0FFFF & s.charAt(i++)) << 48);
            lch |= ((long) (0x0FFFF & s.charAt(i++)) << 32);
            lch |= ((long) (0x0FFFF & s.charAt(i++)) << 16);
            lch |=         (0x0FFFF & s.charAt(i++));

            writeLong(lch);
            }

        if (i <= c - 2)
            {
            int nch;
            nch  = ((0x0FFFF & s.charAt(i++)) << 16);
            nch |=  (0x0FFFF & s.charAt(i++));

            writeInt(nch);
            }

        if (i < c)
            {
            writeChar(s.charAt(i));
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUTF(String str)
            throws IOException
        {
        // Note: implementation borrowed from java.io.DataOutputStream
        int strlen   = str.length();
        int utflen   = 0;
        int c, count = 0;

        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++)
            {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F))
                {
                utflen++;
                }
            else if (c > 0x07FF)
                {
                utflen += 3;
                }
            else
                {
                utflen += 2;
                }
            }

        if (utflen > 65535)
            {
            throw new UTFDataFormatException(
                    "encoded string too long: " + utflen + " bytes");
            }

        byte[] bytearr = new byte[utflen+2];

        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

        int i;
        for (i = 0; i < strlen; i++)
            {
            c = str.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F)))
                {
                break;
                }
            bytearr[count++] = (byte) c;
            }

        for ( ; i < strlen; i++)
            {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F))
                {
                bytearr[count++] = (byte) c;
                }
            else if (c > 0x07FF)
                {
                bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
                }
            else
                {
                bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
                }
            }

        write(bytearr, 0, utflen+2);
        }


    // ----- OutputStream interface -----------------------------------------

    /**
     * {@inheritDoc}
     */
    public void write(int b)
            throws IOException
        {
        ensureBuffer().put((byte) b);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] ab, int of, int cb)
            throws IOException
        {
        if (ab == null || of < 0 || cb < 0 || of + cb > ab.length)
            {
            if (ab == null)
                {
                throw new IllegalArgumentException("null byte array");
                }
            else
                {
                throw new IllegalArgumentException(
                        "ab.length=" + ab.length +
                        ", of=" + of + ", cb=" + cb);
                }
            }

        while (cb > 0)
            {
            ByteBuffer buff   = ensureSpace(cb);
            int        cbCopy = Math.min(buff.remaining(), cb);

            buff.put(ab, of, cbCopy);
            cb -= cbCopy;
            of += cbCopy;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush()
            throws IOException
        {
        if (m_manager == null)
            {
            throw new IOException("stream closed");
            }

        // no-op
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
        {
        if (m_manager != null)
            {
            try
                {
                toBufferSequence().dispose();
                }
            catch (IOException e)
                {
                // already closed
                }
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Write the specified ByteBuffer to the stream.
     *
     * @param buf     the buffer to write out
     * @param cbHint  an optional hint as to how big to resize the stream's buffer if necessary
     *
     * @return the updated hint size after having written the buffer
     *
     * @throws IOException if an I/O error occurs
     */
    private long writeBuffer(ByteBuffer buf, long cbHint)
            throws IOException
        {
        ByteBuffer bufDst = ensureSpace(cbHint);
        int        nLimit = buf.limit();
        int        cb     = buf.remaining();

        cbHint = Math.max(cb, cbHint);
        while (cb > bufDst.remaining())
            {
            int cbDst = bufDst.remaining();

            buf.limit(buf.position() + cbDst);
            bufDst.put(buf);
            buf.limit(nLimit);

            cb     -= cbDst;
            cbHint -= cbDst;
            bufDst  = ensureSpace(cbHint);
            }

        bufDst.put(buf);
        return cbHint - cb;
        }

    /**
     * Return the current ByteBuffer with some remaining capacity.
     *
     * @return the current ByteBuffer.
     *
     * @throws IOException on I/O error
     */
    protected final ByteBuffer ensureBuffer()
            throws IOException
        {
        return ensureSpace(1);
        }

    /**
     * Return the current ByteBuffer with some remaining capacity.
     *
     * @param cbHint a hint as to how much additional space is required
     *
     * @return the current ByteBuffer.
     *
     * @throws IOException on I/O error
     */
    protected final ByteBuffer ensureSpace(long cbHint)
            throws IOException
        {
        BufferManager manager = m_manager;
        if (manager == null)
            {
            throw new IOException("stream closed");
            }

        ByteBuffer buffer = m_buffer;
        if (buffer != null && !buffer.hasRemaining())
            {
            buffer.flip();

            m_cb += buffer.remaining();
            ensureBufferList().add(buffer);
            buffer = null;
            }

        if (buffer == null)
            {
            m_buffer = buffer = manager.acquireSum((int) Math.min(
                    Integer.MAX_VALUE, Math.max(m_cb, cbHint)));
            }

        return buffer;
        }

    /**
     * Return a big-endian ByteBuffer of at least the specified size.
     *
     * The caller must "flush" any writes to the buffer via a call to {@link #flush(java.nio.ByteBuffer)}.
     *
     * @param cb  the required byte size, maximum value of 8
     *
     * @return the temp buffer
     *
     * @throws IOException if an IO error occurs
     */
    protected final ByteBuffer ensureBuffer(int cb)
        throws IOException
        {
        ByteBuffer buff = ensureSpace(/*cbHint*/ cb);
        if (buff.remaining() >= cb && buff.order() == ByteOrder.BIG_ENDIAN)
            {
            return buff;
            }
        else
            {
            ByteBuffer buffTmp = m_buffTmp;
            if (buffTmp == null)
                {
                m_buffTmp = buffTmp = ByteBuffer.allocate(8);
                }
            return buffTmp;
            }
        }

    /**
     * Write the contents of the temp buffer to the stream.
     *
     * @param buff  the temp buffer to flush
     *
     * @throws IOException if an IO error occurs
     */
    protected final void flush(ByteBuffer buff)
            throws IOException
        {
        if (buff == m_buffTmp)
            {
            write(buff.array(), 0, buff.position());
            buff.position(0);
            }
        // else; otherwise the buffer is part of the stream and doesn't need flushing
        }

    /**
     * Return the buffer list, creating it if necessary.
     *
     * @return the buffer list
     */
    protected List<ByteBuffer> ensureBufferList()
        {
        List<ByteBuffer> listBuffers = m_listBuffers;
        if (listBuffers == null)
            {
            m_listBuffers = listBuffers = new ArrayList<ByteBuffer>();
            }
        return listBuffers;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The BufferManager to use in producing the sequence, or null if closed.
     */
    protected BufferManager m_manager;

    /**
     * The current "unflushed" buffer.
     */
    protected ByteBuffer m_buffer;

    /**
     * A temporary byte buffer which can be used for encoding purposes.
     */
    protected ByteBuffer m_buffTmp;

    /**
     * The sequence length.
     */
    protected long m_cb;

    /**
     * The list of flushed buffers.
     */
    protected List<ByteBuffer> m_listBuffers;
    }
