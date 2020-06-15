/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * SingleBufferSequence is a thread-safe BufferSequence implementation which
 * wraps a single ByteBuffer.
 *
 * @author mf  2010.12.04
 */
public class SingleBufferSequence
        extends AtomicInteger // cheaper then having it as a data member on this short lived object
        implements BufferSequence
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SingleBufferSequence around a single ByteBuffer.
     * <p>
     * The BufferSequence will directly reference the supplied buffer,
     * subsequent modifications to the buffer or its positions are not allowed.
     *
     * @param manager  the manager responsible for the buffer if it is to be released during dispose
     * @param buffer   the buffer
     */
    public SingleBufferSequence(BufferManager manager, ByteBuffer buffer)
        {
        if (buffer == null)
            {
            throw new IllegalArgumentException("buffer cannot be null");
            }

        f_manager   = manager;
        m_buffer    = buffer;
        f_nPosition = buffer.position();
        f_nLimit    = buffer.limit();
        }

    /**
     * Construct an "unsafe" SingleBufferSequence around a single ByteBuffer.
     * <p>
     * The BufferSequence will directly reference the supplied buffer,
     * subsequent modifications to the buffer are not allowed.  The buffer's positions will
     * not be relied upon and thus are safe to update externally.
     *
     * @param manager  the manager responsible for the buffer if it is to be released during dispose
     * @param buffer   the buffer
     * @param nPos     the position within the buffer
     * @param cb       the number of bytes
     */
    public SingleBufferSequence(BufferManager manager, ByteBuffer buffer, int nPos, int cb)
        {
        super(1); // mark the BufferSequence as immediately unsafe
        if (buffer == null)
            {
            throw new IllegalArgumentException("buffer cannot be null");
            }
        else if (nPos < 0 || cb < 0 || nPos + cb > buffer.capacity())
            {
            throw new IllegalArgumentException("specified position and length exceed buffer capacity");
            }

        f_manager   = manager;
        m_buffer    = buffer;
        f_nPosition = nPos;
        f_nLimit    = nPos + cb;
        }


    // ----- BufferSequence interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLength()
        {
        return f_nLimit - f_nPosition;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBufferCount()
        {
        return 1;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer getBuffer(int iBuffer)
        {
        if (iBuffer == 0)
            {
            return getSafeBuffer();
            }
        throw new IndexOutOfBoundsException();
        }

    @Override
    public ByteBuffer getUnsafeBuffer(int iBuffer)
        {
        if (iBuffer == 0)
            {
            return m_buffer;
            }
        throw new IndexOutOfBoundsException();
        }

    @Override
    public int getBufferPosition(int iBuffer)
        {
        if (iBuffer == 0)
            {
            return f_nPosition;
            }
        throw new IndexOutOfBoundsException();
        }

    @Override
    public int getBufferLimit(int iBuffer)
        {
        if (iBuffer == 0)
            {
            return f_nLimit;
            }
        throw new IndexOutOfBoundsException();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBufferLength(int iBuffer)
        {
        if (iBuffer == 0)
            {
            return f_nLimit - f_nPosition;
            }
        throw new IndexOutOfBoundsException();
        }


    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer[] getBuffers()
        {
        return new ByteBuffer[] {getSafeBuffer()};
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getBuffers(int iBuffer, int cBuffers, ByteBuffer[] abufDest,
            int iDest)
        {
        if (iBuffer != 0 || cBuffers > 1)
            {
            throw new IndexOutOfBoundsException();
            }

        if (cBuffers == 1)
            {
            abufDest[iDest] = getSafeBuffer();
            }
        }


    // ----- Disposable interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
        {
        ByteBuffer buff = m_buffer;
        if (buff == null)
            {
            throw new IllegalStateException("already disposed (for location use -D" +
                    BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
            }
        else if (TRACK_DISPOSE)
            {
            m_stackDispose = new IOException("disposed at");
            }
        m_buffer = null;

        BufferManager manager = f_manager;
        if (manager != null)
            {
            manager.release(buff);
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
        return m_buffer == null;
        }

    /**
     * Return a view of the ByteBuffer which has it's position and limit set
     * to their original values.
     *
     * @return a view of the ByteBuffer
     */
    protected final ByteBuffer getSafeBuffer()
        {
        // we use a CAS on an updater rather then sync as typically this is only called
        // once per BufferSequence and the cost of the first sync can be high
        ByteBuffer buff = m_buffer;
        if (buff == null)
            {
            throw new IllegalStateException("disposed (for location use -D" +
                    BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
            }
        else if (compareAndSet(0, 1))
            {
            return buff; // common path
            }

        buff = buff.duplicate();
        buff.limit(f_nLimit).position(f_nPosition);
        return buff;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The BufferManager
     */
    protected final BufferManager f_manager;

    /**
     * The ByteBuffer.
     */
    protected ByteBuffer m_buffer;

    /**
     * The buffer's original position.
     */
    protected final int f_nPosition;

    /**
     * The buffer's original limit.
     */
    protected final int f_nLimit;

    /**
     * The stack at which the sequence was destroyed, if trackDestroy is enabled.
     */
    protected IOException m_stackDispose;

    /**
     * True iff dispose locations should be tracked (for debugging)
     */
    private static final boolean TRACK_DISPOSE = Boolean.getBoolean(
            BufferSequence.class.getName() + ".trackDispose");
    }
