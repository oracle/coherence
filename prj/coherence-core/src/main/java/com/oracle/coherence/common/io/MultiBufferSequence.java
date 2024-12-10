/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * MultiBufferSequence is a thread-safe BufferSequence implementation based on
 * an array of ByteBuffers.
 * <p>
 * For BufferSequences of just a few elements it is recommended to either directly
 * instantate a {@link SingleBufferSequence}, {@link DoubleBufferSequence},
 * {@link TripleBufferSequence}, or to use the
 * {@link Buffers#createBufferSequence} method, as these will produce more space optimized
 * alternatives to the MultiBufferSequence.
 * </p>
 *
 * @author mf  2010.12.04
 */
public class MultiBufferSequence
        extends AtomicInteger  // cheaper then having it as a data member on this short lived object
        implements BufferSequence
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a MultiBufferSequence from an array of ByteBuffers.
     * <p>
     * The BufferSequence will directly reference the supplied array and
     * buffers, subsequent modifications to the array, buffers and buffer
     * positions are not allowed.
     *
     * It is recommended that {@link Buffers#createBufferSequence} be used as an alternative
     * to this constructor as it can produce a more optimized result for
     * small array sizes.
     *
     * @param manager  the BufferManager responsible for the ByteBuffers
     * @param aBuffer  the ByteBuffer array
     */
    public MultiBufferSequence(BufferManager manager, ByteBuffer[] aBuffer)
        {
        this(manager, aBuffer, 0, aBuffer.length);
        }

    /**
     * Construct a MultiBufferSequence from an array of ByteBuffers.
     * <p>
     * The BufferSequence will directly reference the supplied array and
     * buffers, subsequent modifications to the specified array portion, its
     * buffers and buffer positions are not allowed.
     *
     * @param manager  the BufferManager responsible for the ByteBuffers
     * @param aBuffer  the ByteBuffer array
     * @param of       the offset into the array at which to start the sequence
     * @param cBuffer  the number of buffers in the sequence
     */
    public MultiBufferSequence(BufferManager manager, ByteBuffer[] aBuffer, int of, int cBuffer)
        {
        this(manager, aBuffer, of, cBuffer, computeLength(aBuffer, of, cBuffer));
        }

    /**
     * Construct a MultiBufferSequence from an array of ByteBuffers.
     * <p>
     * The BufferSequence will directly reference the supplied array and
     * buffers, subsequent modifications to the specified array portion, its
     * buffers and buffer positions are not allowed.
     *
     * @param manager   the BufferManager responsible for the ByteBuffers
     * @param aBuffer   the ByteBuffer array
     * @param of        the offset into the array at which to start the sequence
     * @param cBuffer   the number of buffers in the sequence
     * @param cbBuffer  the number of bytes in the sequence
     */
    public MultiBufferSequence(BufferManager manager, ByteBuffer[] aBuffer, int of, int cBuffer, long cbBuffer)
        {
        if (aBuffer == null)
            {
            throw new IllegalArgumentException("buffer array cannot be null");
            }

        int[] aPosLimit = new int[cBuffer * 2];
        for (int i = 0; i < cBuffer; ++i)
            {
            ByteBuffer buff = aBuffer[of + i];
            aPosLimit[i]           = buff.position();
            aPosLimit[cBuffer + i] = buff.limit();
            }

        f_manager   = manager;
        m_aBuffer   = aBuffer;
        f_ofBuffer  = of;
        f_cBuffer   = cBuffer;
        f_cbBuffer  = cbBuffer;
        f_aPosLimit = aPosLimit;
        }

    // ----- BufferSequence interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public long getLength()
        {
        return f_cbBuffer;
        }

    /**
     * {@inheritDoc}
     */
    public int getBufferCount()
        {
        return f_cBuffer;
        }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer getBuffer(int iBuffer)
        {
        if (iBuffer < 0 || iBuffer >= f_cBuffer)
            {
            throw new IndexOutOfBoundsException();
            }
        return getSafeBuffer(iBuffer);
        }

    @Override
    public ByteBuffer getUnsafeBuffer(int iBuffer)
        {
        if (iBuffer < 0 || iBuffer >= f_cBuffer)
            {
            throw new IndexOutOfBoundsException();
            }
        return m_aBuffer[f_ofBuffer + iBuffer];
        }

    @Override
    public int getBufferPosition(int iBuffer)
        {
        if (iBuffer < 0 || iBuffer >= f_cBuffer)
            {
            throw new IndexOutOfBoundsException();
            }
        return f_aPosLimit[f_ofBuffer + iBuffer];
        }

    @Override
    public int getBufferLimit(int iBuffer)
        {
        if (iBuffer < 0 || iBuffer >= f_cBuffer)
            {
            throw new IndexOutOfBoundsException();
            }
        return f_aPosLimit[f_ofBuffer + f_cBuffer + iBuffer];
        }

    @Override
    public int getBufferLength(int iBuffer)
        {
        return f_aPosLimit[f_ofBuffer + f_cBuffer + iBuffer] - f_aPosLimit[f_ofBuffer + iBuffer];
        }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer[] getBuffers()
        {
        int          cBuffer    = f_cBuffer;
        ByteBuffer[] aBufferNew = new ByteBuffer[cBuffer];

        for (int i = 0; i < cBuffer; ++i)
            {
            aBufferNew[i] = getSafeBuffer(i);
            }

        return aBufferNew;
        }

    /**
     * {@inheritDoc}
     */
    public void getBuffers(int iBuffer, int cBuffers, ByteBuffer[] abufDest,
            int iDest)
        {
        int cBufferSrc = f_cBuffer;

        if (iBuffer < 0 || iBuffer + cBuffers > cBufferSrc)
            {
            throw new IndexOutOfBoundsException();
            }

        for (int eDest = iDest + cBuffers; iDest < eDest; ++iDest, ++iBuffer)
            {
            abufDest[iDest] = getSafeBuffer(iBuffer);
            }
        }


    // ----- Disposable interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
        {
        ByteBuffer[] aBuffer = m_aBuffer;
        if (aBuffer == null)
            {
            throw new IllegalStateException("already disposed (for location use -D" +
                    BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
            }
        else if (TRACK_DISPOSE)
            {
            m_stackDispose = new IOException("disposed at");
            }
        m_aBuffer = null;

        BufferManager manager = f_manager;
        if (manager != null)
            {
            for (int of = f_ofBuffer, eof = of + f_cBuffer; of < eof; ++of)
                {
                manager.release(aBuffer[of]);
                }
            }
        }


    // ----- Object interface -----------------------------------------------

    @Override
    public String toString()
        {
        return Buffers.toString(this);
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Return true if the BufferSequence has been disposed.
     *
     * @return true if the BufferSequence has been disposed.
     */
    public boolean isDisposed()
        {
        return m_aBuffer == null;
        }

    /**
     * Return the number of remaning bytes in the supplied ByteBuffer array.
     *
     * @param aBuffer  the buffer array
     * @param of       the start offset
     * @param cBuffer  the number of buffers to consider
     *
     * @return the byte count
     */
    protected static long computeLength(ByteBuffer[] aBuffer, int of, int cBuffer)
        {
        long cb = 0;
        for (int e = of + cBuffer; of < e; ++of)
            {
            cb += aBuffer[of].remaining();
            }
        return cb;
        }

    /**
     * Return a view of the ByteBuffer which has it's position and limit set
     * to their original values.
     *
     * @return a view of the ByteBuffer
     */
    private final ByteBuffer getSafeBuffer(int i)
        {
        ByteBuffer[] aBuffer = m_aBuffer;
        if (aBuffer == null)
            {
            throw new IllegalStateException("disposed (for location use -D" +
                    BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
            }
        ByteBuffer buff = aBuffer[f_ofBuffer + i];

        // we use a CAS on an updater rather then sync as typically this is only called
        // once per BufferSequence and the cost of the first sync can be high
        if (compareAndSet(i, i + 1))
            {
            return buff; // common path
            }

        buff = buff.duplicate();
        buff.limit(f_aPosLimit[f_cBuffer + i]).position(f_aPosLimit[i]);
        return buff;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The BufferManager
     */
    protected final BufferManager f_manager;

    /**
     * The ByteBuffer array.
     */
    protected ByteBuffer[] m_aBuffer;

    /**
     * The offset of the first buffer.
     */
    protected final int f_ofBuffer;

    /**
     * The number of ByteBuffers in the sequence.
     */
    protected final int f_cBuffer;

    /**
     * The number of bytes in the sequence.
     */
    protected final long f_cbBuffer;

    /**
     * An array of the original positions and limits of each buffer.
     */
    protected final int[] f_aPosLimit;

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
