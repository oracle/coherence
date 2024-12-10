/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.socketbus;


import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.base.Holder;

import com.oracle.coherence.common.io.BufferSequence;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import java.nio.ByteBuffer;


/**
 * SharedBuffer holds a ByteBuffer, and releases it back to its
 * BufferManager when its reference count reaches zero.
 * <p>
 * It is not advisable to make direct use of the ByteBuffer, but rather to
 * use segments, as obtained from {@link #getSegment}, which will be a
 * {@link ByteBuffer#duplicate} over a segment of the shared buffer.
 *
 * @author mf  2010.12.06
 */
public class SharedBuffer
        implements Holder<ByteBuffer>, Disposable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SharedBuffer from a ByteBuffer and its associated
     * BufferManager.  The SharedBuffer is reference counted, and has an
     * inital reference count of one.  The {@link #dispose} method decrements
     * the counter, and will ultimately release the buffer to the manager
     * once the count reaches zero.
     *
     * @param buffer    the buffer to share
     * @param disposer  disposer for disposing the shared buffer
     */
    public SharedBuffer(ByteBuffer buffer, Disposer disposer)
        {
        m_buffer   = buffer;
        m_disposer = disposer;
        }


    // ----- SharedBuffer interface -----------------------------------------

    /**
     * Return a segment of the shared buffer.
     * <p>
     * A successful return from this message will have also incremented the
     * reference count.
     *
     * @param of  the segment offset
     * @param cb  the segment length
     *
     * @return  the SharedBuffer.Segment.
     */
    public Segment getSegment(int of, int cb)
        {
        return new Segment(of, cb);
        }

    /**
     * Return a segment of the shared buffer covering its current position to
     * limit.
     * <p>
     * A successful return from this message will have also incremented the
     * reference count.
     *
     * @return  the SharedBuffer.Segment.
     */
    public Segment getSegment()
        {
        return new Segment();
        }

    /**
     * Increment the reference count.
     *
     * @return this SharedBuffer
     */
    public SharedBuffer attach()
        {
        safeAdjust(/*fIncrement*/ true);
        return this;
        }

    /**
     * Decrement the reference count, releaseing the buffer if the count
     * reaches zero.
     */
    public void detach()
        {
        if (safeAdjust(/*fIncrement*/ false) == 0)
            {
            m_disposer.dispose(m_buffer);
            }
        }


    // ----- Holder interface -----------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException the buffer cannot be changed
     */
    public void set(ByteBuffer value)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public final ByteBuffer get()
        {
        return m_buffer;
        }


    // ----- Disposable interface -------------------------------------------

    /**
     * Decrement the SharedBuffer's reference count, once it reaches zero
     * the shared buffer will be released to the manager.
     */
    public void dispose()
        {
        detach();
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Perform a safe increment or decrement of the reference counter.
     *
     * @param fIncrement  true for increment, false for decrement
     *
     * @return the new value
     */
    protected final int safeAdjust(boolean fIncrement)
        {
        while (true)
            {
            int cCurr = m_cRefs;

            if (cCurr <= 0)
                {
                // we start with a ref count of 1, so to see a zero value
                // we've already released the buffer, and it is then illegal
                // to be here, someone overdecremented
                throw new IllegalStateException("already disposed during " + (fIncrement ? "attach" : "detach")
                        +"; refCount=" + cCurr + " (for location use -D" +
                        BufferSequence.class.getName() + ".trackDispose=true)", m_stackDispose);
                }

            int cNew = cCurr + (fIncrement ? 1 : -1);
            if (cCurr == Integer.MAX_VALUE)
                {
                // once we reach max, we must leak the object, since we can't
                // maintain the count, ultimately the garbage collector will
                // reclaim the memory, though it will not make it back to the
                // pool
                return cCurr;
                }
            else if (REF_COUNT_UPDATER.compareAndSet(this, cCurr, cNew))
                {
                if (TRACK_DISPOSE && !fIncrement && cNew == 0)
                    {
                    m_stackDispose = new Exception("disposed at");
                    }
                return cNew;
                }
            // else; try again
            }
        }


    // ----- inner class: Segment -------------------------------------------

    /**
     * Segment represents a segment of a SharedBuffer.
     */
    public class Segment
            implements Holder<ByteBuffer>, Disposable
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct a holder for a shared buffer, around its current position
         * to limit.
         */
        public Segment()
            {
            synchronized (SharedBuffer.this)
                {
                m_bufferSegment = m_buffer.slice();
                }
            safeAdjust(/*fIncrement*/ true);
            }

        /**
         * Construct a holder for a shared buffer.
         *
         * @param of  the segment offset
         * @param cb  the segment length
         */
        public Segment(int of, int cb)
            {
            synchronized (SharedBuffer.this)
                {
                ByteBuffer buff = m_buffer;
                buff.limit(of + cb).position(of);
                m_bufferSegment = buff.slice();
                }
            safeAdjust(/*fIncrement*/ true);
            }

        // ----- Holder interface ---------------------------------------

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException the buffer cannot be changed
         */
        @Override
        public void set(ByteBuffer value)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteBuffer get()
            {
            return m_bufferSegment;
            }

        // ----- Disposable interface -----------------------------------

        /**
         * Decrement the reference count of the SharedBuffer associated with
         * this Segment.
         */
        @Override
        public void dispose()
            {
            SharedBuffer.this.detach();
            }

        // ----- data members -------------------------------------------

        /**
         * The local accessable portion of the shared buffer.
         */
        protected final ByteBuffer m_bufferSegment;
        }

    // ----- inner interface: Disposer --------------------------------------

    /**
     * Disposer used by the SharedBuffer to dispose ByteBuffer
     */
    public interface Disposer
        {

        /**
         * Dispose byte buffer
         *
         * @param buffer byte buffer to dispose
         */
        public void dispose(ByteBuffer buffer);
        }


    // ----- data members ---------------------------------------------------

    /**
     * The buffer.
     */
    protected final ByteBuffer m_buffer;

    /**
     * The associated Disposer
     */
    protected final Disposer m_disposer;

    /**
     * The reference count.
     */
    protected volatile int m_cRefs = 1;

    /**
     * The stack at which the sequence was destroyed, if trackDestroy is enabled.
     */
    protected volatile Exception m_stackDispose;

    /**
     * The reference count updater.
     */
    private static final AtomicIntegerFieldUpdater REF_COUNT_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SharedBuffer.class, "m_cRefs");

    /**
     * True iff dispose locations should be tracked (for debugging)
     */
    private static final boolean TRACK_DISPOSE = Boolean.getBoolean(
            BufferSequence.class.getName() + ".trackDispose");
    }
