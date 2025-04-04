/*
 * Copyright (c) 2000, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import com.tangosol.coherence.config.Config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * TripleBufferSequence is a thread-safe BufferSequence implementation which
 * wraps three ByteBuffers.
 *
 * @author mf  2014.10.23
 */
public class TripleBufferSequence
        extends AtomicInteger // cheaper then having it as a data member on this short lived object
        implements BufferSequence
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a TripleBufferSequence.
     * <p>
     * The BufferSequence will directly reference the supplied buffers,
     * subsequent modifications to the buffers or their positions are not allowed.
     *
     * @param manager  the manager responsible for the buffers if they are to be released during dispose
     * @param bufferA  the first buffer
     * @param bufferB  the second buffer
     * @param bufferC  the third buffer
     */
    public TripleBufferSequence(BufferManager manager, ByteBuffer bufferA, ByteBuffer bufferB, ByteBuffer bufferC)
        {
        if (bufferA == null || bufferB == null || bufferC == null)
            {
            throw new IllegalArgumentException("buffers cannot be null");
            }

        f_manager    = manager;
        m_bufferA    = bufferA;
        m_bufferB    = bufferB;
        m_bufferC    = bufferC;
        f_nPositionA = bufferA.position();
        f_nLimitA    = bufferA.limit();
        f_nPositionB = bufferB.position();
        f_nLimitB    = bufferB.limit();
        f_nPositionC = bufferC.position();
        f_nLimitC    = bufferC.limit();
        }

    // ----- BufferSequence interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLength()
        {
        return (f_nLimitA - f_nPositionA) + (f_nLimitB - f_nPositionB) + (f_nLimitC - f_nPositionC);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBufferCount()
        {
        return 3;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer getBuffer(int iBuffer)
        {
        switch (iBuffer)
            {
            case 0:
                return getFirstSafeBuffer();
            case 1:
                return getSecondSafeBuffer();
            case 2:
                return getThirdSafeBuffer();
            default:
                throw new IndexOutOfBoundsException();
            }
        }
    @Override
    public ByteBuffer getUnsafeBuffer(int iBuffer)
        {
        switch (iBuffer)
            {
            case 0:
                return m_bufferA;
            case 1:
                return m_bufferB;
            case 2:
                return m_bufferC;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

    @Override
    public int getBufferPosition(int iBuffer)
        {
        switch (iBuffer)
            {
            case 0:
                return f_nPositionA;
            case 1:
                return f_nPositionB;
            case 2:
                return f_nPositionC;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

    @Override
    public int getBufferLimit(int iBuffer)
        {
        switch (iBuffer)
            {
            case 0:
                return f_nLimitA;
            case 1:
                return f_nLimitB;
            case 2:
                return f_nLimitC;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBufferLength(int iBuffer)
        {
        switch (iBuffer)
            {
            case 0:
                return f_nLimitA - f_nPositionA;
            case 1:
                return f_nLimitB - f_nPositionB;
            case 2:
                return f_nLimitC - f_nPositionC;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer[] getBuffers()
        {
        return new ByteBuffer[] {getFirstSafeBuffer(), getSecondSafeBuffer(), getThirdSafeBuffer()};
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getBuffers(int iBuffer, int cBuffers, ByteBuffer[] abufDest,
            int iDest)
        {
        if (cBuffers-- > 0)
            {
            abufDest[iDest++] = getBuffer(iBuffer++);

            if (cBuffers-- > 0)
                {
                abufDest[iDest++] = getBuffer(iBuffer++);

                if (cBuffers > 0)
                    {
                    abufDest[iDest] = getBuffer(iBuffer);
                    }
                }
            }
        }


    // ----- Disposable interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
        {
        ByteBuffer buffA = m_bufferA;
        ByteBuffer buffB = m_bufferB;
        ByteBuffer buffC = m_bufferC;
        if (buffA == null)
            {
            throw new IllegalStateException("already disposed (for location use -D" +
                    BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
            }
        else if (TRACK_DISPOSE)
            {
            m_stackDispose = new IOException("disposed at");
            }
        m_bufferA = null;
        m_bufferB = null;
        m_bufferC = null;

        BufferManager manager = f_manager;
        if (manager != null)
            {
            manager.release(buffA);
            manager.release(buffB);
            manager.release(buffC);
            }
        }

    // ----- Object interface -----------------------------------------------

    @Override
    public String toString()
        {
        return Buffers.toString(this);
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Return true if the BufferSequence has been disposed.
     *
     * @return true if the BufferSequence has been disposed.
     */
    public boolean isDisposed()
        {
        return m_bufferA == null;
        }

    /**
     * Return a view of the first ByteBuffer which has it's position and limit set
     * to their original values.
     *
     * @return a view of the ByteBuffer
     */
    protected final ByteBuffer getFirstSafeBuffer()
        {
        // we use a CAS on an updater rather then sync as typically this is only called
        // once per BufferSequence and the cost of the first sync can be high
        ByteBuffer buff = m_bufferA;
        int        n    = get();
        if (buff == null)
            {
            throw new IllegalStateException("disposed (for location use -D" +
                    BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
            }
        else if ((n & 0x01) == 0 && compareAndSet(n, n | 0x01))
            {
            return buff; // common path
            }

        buff = buff.duplicate();
        buff.limit(f_nLimitA).position(f_nPositionA);
        return buff;
        }

    /**
     * Return a view of the second ByteBuffer which has it's position and limit set
     * to their original values.
     *
     * @return a view of the ByteBuffer
     */
    protected final ByteBuffer getSecondSafeBuffer()
        {
        // we use a CAS on an updater rather then sync as typically this is only called
        // once per BufferSequence and the cost of the first sync can be high
        ByteBuffer buff = m_bufferB;
        int        n    = get();
        if (buff == null)
            {
            throw new IllegalStateException("disposed (for location use -D" +
                    BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
            }
        else if ((n & 0x10) == 0 && compareAndSet(n, n | 0x10))
            {
            return buff; // common path
            }

        buff = buff.duplicate();
        buff.limit(f_nLimitB).position(f_nPositionB);
        return buff;
        }

    /**
     * Return a view of the third ByteBuffer which has it's position and limit set
     * to their original values.
     *
     * @return a view of the ByteBuffer
     */
    protected final ByteBuffer getThirdSafeBuffer()
        {
        // we use a CAS on an updater rather then sync as typically this is only called
        // once per BufferSequence and the cost of the first sync can be high
        ByteBuffer buff = m_bufferC;
        int        n    = get();
        if (buff == null)
            {
            throw new IllegalStateException("disposed (for location use -D" +
                    BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
            }
        else if ((n & 0x0100) == 0 && compareAndSet(n, n | 0x0100))
            {
            return buff; // common path
            }

        buff = buff.duplicate();
        buff.limit(f_nLimitC).position(f_nPositionC);
        return buff;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The BufferManager
     */
    protected final BufferManager f_manager;

    /**
     * The first ByteBuffer.
     */
    protected ByteBuffer m_bufferA;

    /**
     * The second ByteBuffer.
     */
    protected ByteBuffer m_bufferB;

    /**
     * The third ByteBuffer.
     */
    protected ByteBuffer m_bufferC;

    /**
     * The first buffer's original position.
     */
    protected final int f_nPositionA;

    /**
     * The first buffer's original limit.
     */
    protected final int f_nLimitA;

    /**
     * The second buffer's original position.
     */
    protected final int f_nPositionB;

    /**
     * The second buffer's original limit.
     */
    protected final int f_nLimitB;

    /**
     * The third buffer's original position.
     */
    protected final int f_nPositionC;

    /**
     * The thirrd buffer's original limit.
     */
    protected final int f_nLimitC;

    /**
     * The stack at which the sequence was destroyed, if trackDestroy is enabled.
     */
    protected IOException m_stackDispose;

    /**
     * True iff dispose locations should be tracked (for debugging)
     */
    private static final boolean TRACK_DISPOSE = Config.getBoolean(
            BufferSequence.class.getName() + ".trackDispose");
    }
