/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.oracle.coherence.common.base.Disposable;

import com.oracle.coherence.common.internal.net.socketbus.SharedBuffer.Segment;

import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.nio.ByteBufferReadBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.ByteSequence;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;

/**
 * DisposableReadBuffer provides a ReadBuffer abstraction on top of shared
 * {@link Segment Segment}.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class DisposableReadBuffer
        implements ReadBuffer, Disposable
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DisposableReadBuffer.
     *
     * @param aSegment  underlying segments
     */
    public DisposableReadBuffer(Segment[] aSegment)
        {
        m_aSegment = aSegment;
        m_delegate = aSegment.length == 1
                     ? new ByteBufferReadBuffer(aSegment[0].get())
                     : new MultiBufferReadBuffer(createReadBuffer(aSegment));
        }

    // ----- ReadBuffer methods ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int length()
        {
        return m_delegate.length();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte byteAt(int of)
        {
        return m_delegate.byteAt(of);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyBytes(int ofBegin, int ofEnd, byte[] abDest, int ofDest)
        {
        m_delegate.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferInput getBufferInput()
        {
        return m_delegate.getBufferInput();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadBuffer getReadBuffer(int of, int cb)
        {
        return m_delegate.getReadBuffer(of, cb);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(OutputStream out)
            throws IOException
        {
        m_delegate.writeTo(out);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(OutputStream out, int of, int cb)
            throws IOException
        {
        m_delegate.writeTo(out, of, cb);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(DataOutput out)
            throws IOException
        {
        m_delegate.writeTo(out);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(DataOutput out, int of, int cb)
            throws IOException
        {
        m_delegate.writeTo(out, of, cb);
        }

    @Override
    public void writeTo(ByteBuffer buf)
        {
        m_delegate.writeTo(buf);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(ByteBuffer buf, int of, int cb)
            throws IOException
        {
        m_delegate.writeTo(buf, of, cb);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray()
        {
        return m_delegate.toByteArray();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray(int of, int cb)
        {
        return m_delegate.toByteArray(of, cb);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary toBinary()
        {
        return m_delegate.toBinary();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary toBinary(int of, int cb)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer toByteBuffer()
        {
        return m_delegate.toByteBuffer();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer toByteBuffer(int of, int cb)
        {
        return m_delegate.toByteBuffer(of, cb);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteSequence subSequence(int ofStart, int ofEnd)
        {
        return m_delegate.subSequence(ofStart, ofEnd);
        }

    /**
     * {@inheritDoc}
     */
    public Object clone()
        {
        throw new UnsupportedOperationException();
        }

    // ----- Disposable methods ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
        {
        Segment[] aSegment = m_aSegment;
        for (int i = 0, c = aSegment.length; i < c; i++)
            {
            aSegment[i].dispose();
            }
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Create read buffers from the segments.
     *
     * @param aSegment Segment array
     *
     * @return ReadBuffers
     */
    protected static ReadBuffer[] createReadBuffer(Segment[] aSegment)
        {
        int          cSegments = aSegment.length;
        ReadBuffer[] aBuf      = new ReadBuffer[cSegments];
        for (int i = 0; i < cSegments; i++)
            {
            aBuf[i] = new ByteBufferReadBuffer(aSegment[i].get());
            }

        return aBuf;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Underlying Segments
     */
    protected Segment[]  m_aSegment;

    /**
     * Delegate ReadBuffer
     */
    protected ReadBuffer m_delegate;
    }