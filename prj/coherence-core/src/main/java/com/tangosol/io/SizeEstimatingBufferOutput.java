/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * SizeEstimatingBufferOutput is a WriteBuffer.BufferOutput implementation which writes nothing, and simply maintains
 * an estimated byte count of how much would have been written.
 *
 * @since Coherence 12.1.3
 *
 * @author mf  2013.05.16
 */
public class SizeEstimatingBufferOutput
        implements WriteBuffer.BufferOutput
    {
    @Override
    public void write(int b)
            throws IOException
        {
        ++m_cb;
        }

    @Override
    public void write(byte[] ab)
            throws IOException
        {
        m_cb += ab.length;
        }

    @Override
    public void write(byte[] ab, int of, int cb)
            throws IOException
        {
        m_cb += cb;
        }

    @Override
    public void flush()
            throws IOException
        {
        }

    @Override
    public void close()
            throws IOException
        {
        }

    @Override
    public void writeBoolean(boolean f)
            throws IOException
        {
        ++m_cb;
        }

    @Override
    public void writeByte(int b)
            throws IOException
        {
        ++m_cb;
        }

    @Override
    public void writeShort(int n)
            throws IOException
        {
        m_cb += 2;
        }

    @Override
    public void writeChar(int ch)
            throws IOException
        {
        m_cb += 2;
        }

    @Override
    public void writeInt(int n)
            throws IOException
        {
        m_cb += 4;
        }

    @Override
    public void writeLong(long l)
            throws IOException
        {
        m_cb += 8;
        }

    @Override
    public void writeFloat(float fl)
            throws IOException
        {
        m_cb += 4;
        }

    @Override
    public void writeDouble(double dfl)
            throws IOException
        {
        m_cb += 8;
        }

    @Override
    public void writeBytes(String s)
            throws IOException
        {
        m_cb += s.length();
        }

    @Override
    public void writeChars(String s)
            throws IOException
        {
        m_cb += s.length() * 2;
        }

    @Override
    public void writeUTF(String s)
            throws IOException
        {
        m_cb += 4 + s.length(); // estimate assuming ascii
        }

    @Override
    public WriteBuffer getBuffer()
        {
        throw new UnsupportedOperationException();
        }

    public ByteBuffer getByteBuffer(int cb)
        {
        m_cb += cb;
        return ByteBuffer.allocate(cb);
        }

    @Override
    public void writeSafeUTF(String s)
            throws IOException
        {
        m_cb += s == null ? 5 : 5 + s.length(); // estimate assuming ascii
        }

    @Override
    public void writePackedInt(int n)
            throws IOException
        {
        m_cb += 5; // worst case
        }

    @Override
    public void writePackedLong(long l)
            throws IOException
        {
        m_cb += 10; // worst case
        }

    @Override
    public void writeBuffer(ReadBuffer buf)
            throws IOException
        {
        m_cb += buf.length();
        }

    @Override
    public void writeBuffer(ReadBuffer buf, int of, int cb)
            throws IOException
        {
        m_cb += cb;
        }

    @Override
    public void writeStream(InputStreaming stream)
            throws IOException
        {
        m_cb += stream.available();
        }

    @Override
    public void writeStream(InputStreaming stream, int cb)
            throws IOException
        {
        m_cb += cb;
        }

    @Override
    public int getOffset()
        {
        return m_cb;
        }

    @Override
    public void setOffset(int of)
        {
        m_cb = of;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The byte count.
     */
    protected int m_cb;
    }
