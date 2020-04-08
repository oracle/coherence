/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.io;


import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.collections.ConcurrentLinkedStack;
import com.oracle.coherence.common.collections.Stack;
import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferManagers;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.MemorySize;

import java.nio.ByteOrder;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.ByteBuffer;


/**
 * The SegmentedBufferManager performs buffer managment by dividing
 * the buffers into a number of segments, such as small, medium, and large.
 * <p>
 * Each segment contains a number of generations, where generations are created
 * on demand and lazily collected.
 *
 * @author ch  2010.03.04
 */
public class SegmentedBufferManager
        implements BufferManager, Disposable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a SegmentedBufferManager.
     *
     * @param allocator    the BufferAllocator to use to fill the pool
     * @param cbBufferMin  the minimum buffer size
     * @param cbMax        the total amount of memory to pool
     */
    public SegmentedBufferManager(BufferAllocator allocator, int cbBufferMin, long cbMax)
        {
        this(allocator, DEFAULT_SEGMENT_COUNT, cbMax / DEFAULT_SEGMENT_COUNT, cbBufferMin, DEFAULT_GROWTH_FACTOR);
        }

    /**
     * Creates a SegmentedBufferManager.
     *
     * @param allocator  the BufferAllocator to use to fill the pool
     * @param cbMax      the total amount of memory to pool
     */
    public SegmentedBufferManager(BufferAllocator allocator, long cbMax)
        {
        this(allocator, DEFAULT_SEGMENT_COUNT, cbMax / DEFAULT_SEGMENT_COUNT, DEFAULT_BUF_SIZE, DEFAULT_GROWTH_FACTOR);
        }

    /**
     * Creates a SegmentedBufferManager.
     *
     * @param allocator      the BufferAllocator to use to fill the pool
     * @param cSegments      the number of segments
     * @param cbSegment      the maximum number bytes each segment will consume
     * @param cbBufferMin    the smallest buffer size
     * @param nGrowthFactor  the segment growth factor
     */
    public SegmentedBufferManager(BufferAllocator allocator, int cSegments,
            long cbSegment, int cbBufferMin, int nGrowthFactor)
        {
        m_allocator = allocator;
        final int cbDefaultBuf = SegmentedBufferManager.DEFAULT_BUF_SIZE;

        cbSegment = Math.min(cbSegment, (long) Integer.MAX_VALUE * GEN_ID_UNPOOLED); // max gen size is 2GB

        if ((cbBufferMin & (DEFAULT_BUF_SIZE - 1)) != 0)
            {
            // ensure that the size doesn't touch our bits
            // TODO cbBufferMin = (cbBufferMin + DEFAULT_BUF_SIZE - 1) & ~(DEFAULT_BUF_SIZE - 1);
            cbBufferMin = (cbBufferMin / cbDefaultBuf) * cbDefaultBuf
                    + (cbBufferMin % cbDefaultBuf == 0 ? 0 : cbDefaultBuf);
            }

        final int cHighestBit  = Integer.SIZE - 1;
        final int cLowestBit   = Integer.numberOfTrailingZeros(cbBufferMin);
        final int nMaxSegments = cHighestBit - (cLowestBit  * nGrowthFactor);

        if (nMaxSegments < 1)
            {
            throw new IllegalArgumentException("Growthfactor is to aggressive: "
                    + nGrowthFactor);
            }
        else if (cSegments > nMaxSegments)
            {
            throw new IllegalArgumentException("The number of segments exceeded: "
                    + nMaxSegments);
            }

        int       cbBuff   = m_cbMin     = cbBufferMin;
        Segment[] aSegment = f_aSegments = new Segment[cSegments];
        for (int i = 0; i < cSegments; i++)
            {
            long cBuf = cbSegment / cbBuff;
            aSegment[i] = allocateSegment(cbBuff, (int) (cBuf > Integer.MAX_VALUE ? Integer.MAX_VALUE : cBuf));
            cbBuff      = cbBuff << nGrowthFactor;
            }

        m_cbMax                = cbBuff >> nGrowthFactor;
        m_nSegmentGrowthFactor = nGrowthFactor;
        }

    // ----- SegmentedBufferManager interface -------------------------------

    /**
     * Set the name of this BufferManager
     *
     * @param sName the name
     */
    public void setName(String sName)
        {
        m_sName = sName;
        }

    /**
     * Return the name of this BufferManager
     *
     * @return the name
     */
    public String getName()
        {
        return m_sName;
        }

    // ----- BufferManager interface ----------------------------------------


    @Override
    public long getCapacity()
        {
        long cbCapacity  = 0;

        for (Segment seg : f_aSegments)
            {
            long cbBuf = seg.getBufferSize();
            long cbGen = cbBuf * seg.getGenerationSize();

            cbCapacity  += cbGen * GEN_ID_UNPOOLED;
            }

        return cbCapacity;
        }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer acquire(final int cbMin)
        {
        ByteBuffer buff = ensureMinBuffer(cbMin, Integer.MAX_VALUE);

        if (buff.capacity() > cbMin)
            {
            buff.limit(cbMin);
            }

        return buff;
        }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer acquirePref(int cbPref)
        {
        ByteBuffer buff = ensureBuffer(cbPref);

        if (buff.capacity() > cbPref)
            {
            buff.limit(cbPref);
            }

        return buff;
        }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer acquireSum(int cbSum)
        {
        return ensureBuffer(cbSum);
        }

    /**
     * {@inheritDoc}
     */
    public void release(ByteBuffer buffer)
        {
        getSegment(buffer.capacity()).release(buffer);
        }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer truncate(ByteBuffer buff)
        {
        int nGen = decodeGeneration(buff.capacity());
        return nGen < GEN_ID_TRUNCATE
            ? buff // common path
            : truncateComplex(buff);
        }

    /**
     * Truncate the specified buffer as described by {@link #truncate truncate}.
     *
     * @param buff  the buffer to truncate
     *
     * @return the replacement buffer
     */
    protected ByteBuffer truncateComplex(ByteBuffer buff)
        {
        int cbSeg    = decodeSize(buff.capacity());
        int cbSegPre = cbSeg >> m_nSegmentGrowthFactor;
        int cbUsed   = buff.remaining();

        if (cbSeg > m_cbMin && cbUsed <= cbSegPre)
            {
            // the used buffer could fit in the previous segment
            ByteBuffer buffNew;
            try
                {
                buffNew = ensureMinBuffer(/*cbMin*/ cbUsed, /*cbMax*/ cbSeg - 1);
                }
            catch (OutOfMemoryError e)
                {
                return buff; // use the original
                }

            buffNew.put(buff).flip();
            release(buff);
            return buffNew;
            }
        return buff;
        }

    // ----- Disposable interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void dispose()
        {
        for (Segment pool : f_aSegments)
            {
            pool.dispose();
            }
        }


    // ----- Object interface ------------------------------------------------

    @Override
    public String toString()
        {
        long cbCapacity  = 0;
        long cbUsed      = 0;
        long cAlloc      = 0;
        long cNonPooled  = 0;
        long cbAvailable = 0;
        long cbPeakHist  = 0;

        StringBuilder sbSeg = new StringBuilder();
        for (Segment seg : f_aSegments)
            {
            long cbBuf = seg.getBufferSize();
            long cbGen = cbBuf * seg.getGenerationSize();

            cbAvailable += seg.f_stackBuf.size() * cbBuf;
            cbCapacity  += cbGen * GEN_ID_UNPOOLED;
            cbUsed      += seg.getAcquired() * cbBuf;
            cAlloc      += seg.getAllocationCount();
            cNonPooled  += seg.getNonPooledAllocationCount();
            cbPeakHist  += seg.m_cMaxBuffersHistoric * cbBuf; // peak since last full GC

            sbSeg.append(seg).append(", ");
            }
        return getName() + "(capacity=" + new MemorySize(cbCapacity) +
                ", usage=" + new MemorySize(cbUsed) + ".." + new MemorySize(cbPeakHist) + "/" + new MemorySize(cbAvailable) +
                ", hit rate=" + ((cAlloc - cNonPooled) * 100/ (cAlloc == 0 ? 1 : cAlloc)) + "%" +
                ", segment utilization=" + sbSeg.toString() +
                "allocator=" + m_allocator + ")";
        }


    // ----- helpers ---------------------------------------------------------

    /**
     * Return the allocator used by this manager.
     *
     * @return the allocator
     */
    protected BufferAllocator getAllocator()
        {
        return m_allocator;
        }

    // ----- private members -------------------------------------------------

    /**
    * Given a buffer allocation size, extract the generation id of that
    * buffer.
    *
    * @param cbBuffer  the buffer allocation size
    *
    * @return the generation id
    */
    protected int decodeGeneration(final int cbBuffer)
        {
        return (cbBuffer & GEN_ID_MASK) >> GEN_ID_SHIFT;
        }

    /**
     * Given an allocated buffer size, determine the configured buffer size
     * of that buffer.
     *
     * @param cb  the allocated buffer size
     *
     * @return  the configured buffer size
     */
    private int decodeSize(final int cb)
        {
        return cb & ~GEN_ID_MASK;
        }

    /**
     * Return a ByteBuffer from the optimal PoolSegment for a specific count of bytes.
     *
     * @param cb  the size to match
     *
     * @return  the buffer
     */
    private ByteBuffer ensureBuffer(int cb)
        {
        Segment[] aSegments = f_aSegments;
        int           cSegments = aSegments.length;
        int iSeg = 0;
        if (cb >= m_cbMax)
            {
            iSeg = cSegments - 1;
            }
        else
            {
            for (int cbMin = m_cbMin; cb > cbMin; ++iSeg)
                {
                cb = cb >> m_nSegmentGrowthFactor;
                }
            }

        // try to find the closest matching segment by incrementally checking neighboring segments
        // this as apposed to simply looping around showed 50% performance gaines in some tests
        ByteBuffer buff = null;
        for (int i = 0; i < cSegments && buff == null; ++i)
            {
            if (iSeg + i < cSegments)
                {
                buff = aSegments[iSeg + i].acquire(/*fEnsure*/ false);
                }

            if (buff == null && i > 0 && iSeg - i >= 0)
                {
                buff = aSegments[iSeg - i].acquire(/*fEnsure*/ false);
                }
            // no space in segment; try next
            }

        // fall back on non-pooled allocation if necessary
        return buff == null ? aSegments[iSeg].acquire(/*fEnsure*/ true) : buff;
        }

    /**
     * Return a ByteBuffer of at least the specified size.
     *
     * @param cbMin  the minimum required size
     * @param cbMax  the maximum desired size
     *
     * @return  the buffer
     */
    private ByteBuffer ensureMinBuffer(int cbMin, int cbMax)
        {
        Segment[] aSegments  = f_aSegments;
        int           cSegments = aSegments.length;
        if (cbMin > m_cbMax)
            {
            throw new OutOfMemoryError("requested buffer size exceeds pool maximum");
            }

        int iSeg = 0;
        for (int cSeg = aSegments.length;
             iSeg < cSeg && aSegments[iSeg].f_cbBuffer < cbMin;
             ++iSeg)
            {}

        for (int i = iSeg; i < cSegments; ++i)
            {
            Segment segment = aSegments[i];
            if (segment.f_cbUnpooledBuffer > cbMax)
                {
                break;
                }

            ByteBuffer buf = segment.acquire(/*fEnsure*/ false);
            if (buf != null)
                {
                return buf;
                }
            // no space in segment; try next
            }

        // all suitable segments are full; allow non-pooled from best fit
        return aSegments[iSeg].acquire(/*fEnsure*/ true);
        }

    /**
     * Return the matching PoolSegment for a specific count of bytes.
     *
     * @param cb  the size to match
     *
     * @return the PoolSegment that matches the size
     *
     * @throws IllegalArgumentException if no PoolSegment matches the size
     *                                  exactly
     */
    private Segment getSegment(int cb)
        throws IllegalArgumentException
        {
        Segment[] aSegments = f_aSegments;
        final int     cbDecoded = cb = decodeSize(cb);
        final int     cSeg      = aSegments.length;
        int           iSeg      = 0;

        for (int cbMin = m_cbMin; cb > cbMin && iSeg < cSeg; ++iSeg)
            {
            cb = cb >> m_nSegmentGrowthFactor;
            }

        if (iSeg < cSeg && cbDecoded == aSegments[iSeg].getBufferSize())
            {
            return aSegments[iSeg];
            }

        throw new IllegalArgumentException("No pool segment for size: "
                + cbDecoded + " in " + cSeg + " segment(s) between "
                + aSegments[0].getBufferSize() + " .. "
                + aSegments[cSeg - 1].getBufferSize());
        }


    // ----- inner class: Segment -------------------------------------------

    /**
     * Allocate a segment for buffers of the specified size.
     *
     * @param cbBuffer      the size of the individual ByteBuffers
     * @param cBuffers      the number of ByteBuffers for the pool
     *
     * @return the allocated segment
     */
    protected Segment allocateSegment(int cbBuffer, int cBuffers)
        {
        return new Segment(cbBuffer, cBuffers);
        }


    /**
     * PoolSegment defines the pools which buffers can be allocated from
     * and released to. The implementation provide highly concurrent access so
     * that elements can be acquired and released to the pool without
     * synchronization.
     * <p>
     * A PoolSegment is shrunk when its capacity remains greater than the actual
     * usage for a period of time. The capacity will be evaluated periodically
     * based on the number of buffer releases (<tt>cReevalFreq</tt>) and at
     * specific time (<tt>PERIODIC_CLEANUP_FREQUENCY</tt>) intervals.
     * When a pool shrinks, buffers that belong to purged generations will no
     * longer be retained when released back to the pool.
     */
    protected class Segment
            implements Disposable
        {
        // ----- constructors---------------------------------------------

        /**
        * Construct a pool for buffers of the specified size.
        *
        * @param cbBuffer      the size of the individual ByteBuffers
        * @param cBuffers      the number of ByteBuffers for the pool
        */
        protected Segment(int cbBuffer, int cBuffers)
            {
            f_cbBuffer = cbBuffer;
            // COH-4231: there should be at least one buffer per generation
            f_cBufferGen = Math.max(cBuffers / GEN_ID_UNPOOLED, 1);
            f_stackBuf              = new ConcurrentLinkedStack<ByteBuffer>();
            f_cGeneration = new AtomicInteger(GEN_ID_EMPTY);
            f_cReleased = new AtomicLong(0L);
            f_cNonPooledReleased = new AtomicLong(0L);
            f_cAcquired = new AtomicLong(0L);
            f_cNonPooledAllocations = new AtomicLong(0L);
            f_cbUnpooledBuffer = encodeGeneration(GEN_ID_UNPOOLED);
            }

        // ----- public methods ------------------------------------------

        /**
        * Acquire the next available buffer from the pool. If the pool is
        * empty, either grow the pool or as the last resort return a
        * non-pooled buffer.
        *
        * @return a ByteBuffer
        */
        public ByteBuffer acquire()
            {
            return acquire(/*fEnsure*/ true);
            }

        /**
        * Acquire the next available buffer from the pool. If the pool is
        * empty, either grow the pool or as the last resort return a
        * non-pooled buffer.
        *
        * @param fEnsure  true iff non-pooled allocation is allowable
         *
        * @return a ByteBuffer or null if fEnsure == false and no suitable buffer is available
        */
        public ByteBuffer acquire(boolean fEnsure)
            {
            // optimized for most common scenario: there's something in the pool
            // (otherwise it wouldn't be a pool)
            ByteBuffer buffer = f_stackBuf.pop();
            if (buffer == null)
                {
                buffer = acquireComplex(fEnsure);
                if (buffer == null)
                    {
                    return null;
                    }
                }

            f_cAcquired.incrementAndGet();
            return buffer;
            }

        /**
         * Return true iff the segment is currently allowed to shrink
         *
         * @return true iff the segment is currently allowed to shrink
         */
        protected boolean isShrinkable()
            {
            return true;
            }

        /**
        * Release a buffer back to the pool, and occasionally check if it is
        * possible to shrink the pool.
        *
        * @param buffer  the buffer to release
        */
        public void release(ByteBuffer buffer)
            {
            int nGeneration = decodeGeneration(buffer.capacity());

            if ((f_cReleased.incrementAndGet() & STATS_FREQUENCY) == 0)
                {
                recordUsage();
                evaluateCapacity(/*fOthers*/ true);
                }

           if (nGeneration == GEN_ID_UNPOOLED &&
                f_cNonPooledReleased.incrementAndGet() % UNPOOLED_RECLAIM_INTERVAL != 0 && // only for so long
                f_stackBuf.size() < f_cBufferGen * GEN_ID_TRUNCATE) // only so much
                {
                // we've been given an unpooled buffer to release, which indicates that at some "recent" point
                // we were out of pool buffers and had to allocate an unpooled one which is expensive.  If this
                // happens frequently then either the pool is undersized or the application has a leak.  In
                // the case of a leak, overtime it could completely drain the pool of poolable buffers, which
                // if not refilled would entirely negate the pooling benefits.  In an effort to avoid penalizing
                // good shared pool users we will allow pooling of the "unpooled" buffers, but not all of them.
                // But we do limit the number of re-uses so that if there is no leak we won't overinflate our
                // pool. Also we don't allow ourselves to hold unpooled buffers if the pool is fairly full
                --nGeneration; // treat it like the max gen, so it may still be released now if pool has "shrunk"
                }

            // always store in default java byte order BIG_ENDIAN
            // so that acquired buffers are as good as freshly allocated
            // ones.
            buffer.order(ByteOrder.BIG_ENDIAN).clear(); // just resets position and limit
            if (BufferManagers.ZERO_ON_RELEASE)
                {
                Buffers.zero(buffer);
                }

            // Release the buffer back into the pool if:
            // 1) buffer is not of the non-pooled generation
            // 2) buffer belongs to a generation that is  less or equal to the
            //    current generation
            // 3) the current generation is 'LOCKED' (meaning at least one
            //    other thread is in need of a buffer, or is shrinking
            //    capacity at this instant)
            final int cCurrentGen = getGenerationId();
            if (nGeneration <= cCurrentGen || (cCurrentGen == GEN_ID_LOCKED && nGeneration != GEN_ID_UNPOOLED) || !isShrinkable())
                {
                f_stackBuf.push(buffer);
                }
            else
                {
                dropBuffer(buffer);
                }
            }

        /**
         * Release the specified buffer from the segment.
         *
         * @param buffer the buffer
         */
        protected void dropBuffer(ByteBuffer buffer)
            {
            m_allocator.release(buffer);
            }

        // ----- Disposable implementation ------------------------------

        /**
        * {@inheritDoc}
        */
        public void dispose()
            {
            trim(0);
            }

        // ----- accessors ----------------------------------------------

        /**
        * Get the default size of buffers in this pool. Note that the
        * actual size of buffers may be larger, since each generation's
        * size is incrementally larger to differentiate it from other
        * generations.
        *
        * @return the default size of the buffers in this pool
        */
        public int getBufferSize()
            {
            return f_cbBuffer;
            }

        /**
        * Get the number of buffers that are allocated per generation.
        *
        * @return the number of buffers in each generation
        */
        public int getGenerationSize()
            {
            return f_cBufferGen;
            }

        // ----- private accessors --------------------------------------

        /**
        * Get the current number of generations for this pool.
        *
        * @return the number of generations for this pool
        */
        private int getGenerationId()
            {
            return f_cGeneration.get();
            }

        /**
        * Return the number of currently acquired buffers.
        *
        * @return the number of currently acquired buffers
        */
        protected int getAcquired()
            {
            return Math.max(0, (int) (f_cAcquired.get() - f_cReleased.get()));
            }

        /**
         * Return the number of allocations performed on this segment.
         *
         * @return the number of allocations performed on this segment
         */
        private long getAllocationCount()
            {
            return f_cAcquired.get();
            }

        /**
         * Return the number of releases performed on this segment.
         *
         * @return the number of releases performed on this segment
         */
        private long getReleaseCount()
            {
            return f_cReleased.get();
            }

        /**
         * Return the number of non-pooled allocations performed on this segment.
         *
         * @return the number of non-pooled allocations performed on this segment
         */
        private long getNonPooledAllocationCount()
            {
            return f_cNonPooledAllocations.get();
            }

        // ----- internal ---------------------------------------------------

        /**
        * Return a buffer, potentially grow the PoolSegment or returning a non
        * pooled buffer.
        *
        * @param fEnsure  true if a non-pooled buffer should be allocated if necessary
         *
        * @return a buffer
        */
        protected ByteBuffer acquireComplex(boolean fEnsure)
            {
            final AtomicInteger atomicGen = f_cGeneration;
            while (true)
                {
                // we poll after obtaining cGen to avoid accidently growing by
                // two generations
                int cGen = atomicGen.get();

                ByteBuffer buffer = f_stackBuf.pop();
                if (buffer != null)
                    {
                    return buffer;
                    }

                switch (cGen)
                    {
                    case GEN_ID_UNPOOLED - 1:
                        // no more generations; allocate a "throw away" buffer
                        return fEnsure ? allocateNonPooledBuffer() : null;

                    case GEN_ID_LOCKED:
                        // this spin is limited as it will end once there are
                        // available buffers in the queue
                        break;

                    case GEN_ID_EMPTY:
                        // fall through

                    default:
                        // attempt to allocate a generation
                        if (atomicGen.compareAndSet(cGen, GEN_ID_LOCKED))
                            {
                            int     nGen     = cGen + 1;
                            boolean fSuccess = false;
                            try
                                {
                                // allocate an entire generation
                                recordUsage();
                                fSuccess = allocateGeneration(nGen);
                                }
                            finally
                                {
                                atomicGen.set(fSuccess ? nGen : cGen);
                                }
                            }
                        // else spin to obtain growth lock
                        break;
                    }
                }
            }

        /**
        * Allocate a new buffer which will not be returned to the pool
        * after it has been released.
        *
        * @return a buffer that will not be pooled when it is released
        */
        protected ByteBuffer allocateNonPooledBuffer()
            {
            f_cNonPooledAllocations.incrementAndGet();
            return m_allocator.allocate(f_cbUnpooledBuffer);
            }

        /**
        * Allocate a generation of ByteBuffers.
        *
        * @param nGeneration  the generation id of the block
        *
        * @return true iff the generation was successfully allocated
        */
        protected boolean allocateGeneration(int nGeneration)
            {
            int cbBuffer = encodeGeneration(nGeneration);

            LOGGER.log(Level.FINE, getName() + " growing segment '"
                    + getBufferSize() + "' to " + (nGeneration + 1)
                    + " generations");

            // record when this happened (do this first so we won't bother
            // checking the capacity to see if it needs to shrink on another
            // thread)
            m_ldtNextEvaluation = System.currentTimeMillis()
                + CLEANUP_FREQUENCY_MILLIS;

            try
                {
                return allocateGenerationBuffers(nGeneration, cbBuffer);
                }
            catch (OutOfMemoryError e)
                {
                return false;
                }
            }

        /**
         * Allocate a series of buffers.
         *
         * @param nGen      the generation id
         * @param cbBuffer  the size of each buffer
         *
         * @return true iff the generation was allocated
         */
        protected boolean allocateGenerationBuffers(int nGen, int cbBuffer)
            {
            for (int i = 0, c = getGenerationSize(); i < c; ++i)
                {
                try
                    {
                    f_stackBuf.push(m_allocator.allocate(cbBuffer));
                    }
                catch (OutOfMemoryError e)
                    {
                    return i > 0; // return true if any buffers for this generation were allocated
                    }
                }

            return true;
            }

        /**
        * Evaluate if and how much the pool should shrink. If the capacity
        * is twice the amount needed in the pool. The pool will be cut in
        * half.
        *
        * @param fEvalPeers  true if all segments should be evaluated
        */
        private void evaluateCapacity(final boolean fEvalPeers)
            {
            final long ldtNow = System.currentTimeMillis();
            final int  nGen   = getGenerationId();

            if (nGen > GEN_ID_EMPTY
                    && ldtNow > m_ldtNextEvaluation
                    && f_cGeneration.compareAndSet(nGen, GEN_ID_LOCKED))
                {
                // record next check time
                m_ldtNextEvaluation = ldtNow + CLEANUP_FREQUENCY_MILLIS;
                int nDesiredGen = nGen;
                try
                    {
                    if (isShrinkable())
                        {
                        int nInUse      = nGen - (f_stackBuf.size() / f_cBufferGen); // we certainly need whatever the app is currently using
                        int nRecent     = GEN_ID_EMPTY + (m_cMaxBuffers / f_cBufferGen) + (m_cMaxBuffers % f_cBufferGen == 0 ? 0 : 1); // recent high water mark

                        nDesiredGen = Math.min(nGen, Math.max(nInUse, nRecent));

                        int cCapacity = Math.min(nGen + 1, GEN_ID_UNPOOLED);
                        if (nDesiredGen == GEN_ID_EMPTY || (nDesiredGen < cCapacity && nDesiredGen != nGen))
                            {
                            m_cMaxBuffersHistoric = m_cMaxBuffers;

                            LOGGER.log(Level.FINE, getName() + " shrinking segment '" + getBufferSize()
                                    + "' by " + (nGen - nDesiredGen)
                                    + " generation(s) to " + (nDesiredGen + 1) + ", based on recent high water mark of "
                                    + new MemorySize(m_cMaxBuffers * f_cbBuffer));

                            // since we hand out buffers in LIFO order we must actively trim buffers now; rather then
                            // let them bleed off over time otherwise we could hold them "forever"
                            int cTrimmed = trim(nDesiredGen);

                            LOGGER.log(Level.FINEST, getName() + " scavenged " + cTrimmed + " buffers; "
                                    + new MemorySize(f_cbBuffer * cTrimmed));
                            }
                        }

                   if (fEvalPeers)
                        {
                        for (Segment pool : SegmentedBufferManager.this.f_aSegments)
                            {
                            if (pool != this)
                                {
                                pool.evaluateCapacity(/*fOthers*/ false);
                                }
                            }
                        }
                    }
                finally
                    {
                    m_cMaxBuffers /= 2; // don't completely blow out stats
                    f_cGeneration.set(nDesiredGen);
                    }
                }
            }

        /**
        * Record the current buffer usage.
        */
        private void recordUsage()
            {
            m_cMaxBuffers         = Math.max(m_cMaxBuffers, getAcquired());
            m_cMaxBuffersHistoric = Math.max(m_cMaxBuffersHistoric, m_cMaxBuffers);
            }

        /**
        * Release the buffers from later generations.
        *
        * @param nGeneration  the minimum generation to retain
        *
        * @return the number of trimmed buffers
        */
        private int trim(final int nGeneration)
            {
            int cbCutoff = encodeGeneration(nGeneration + 1);
            int cRemove  = 0;

            // in case the application has a leak, and we've been replenishing the
            // pool with "unpoolable" buffers, refuse to reclaim all of them

            ConcurrentLinkedStack<ByteBuffer> stackTmp = new ConcurrentLinkedStack<ByteBuffer>();
            for (ByteBuffer buf = f_stackBuf.pop(); buf != null; buf = f_stackBuf.pop())
                {
                if (buf.capacity() >= cbCutoff)
                    {
                    ++cRemove;
                    dropBuffer(buf);
                    }
                else
                    {
                    stackTmp.push(buf);
                    }
                }

            for (ByteBuffer buf : stackTmp)
                {
                f_stackBuf.push(buf);
                }

            return cRemove;
            }

        /**
        * Return the buffer allocation size for a given generation.
        *
        * @param nGenId  the generation id
        *=
        * @return the buffer allocation size
        */
        protected int encodeGeneration(final int nGenId)
            {
            return getBufferSize() | (nGenId << GEN_ID_SHIFT);
            }

        @Override
        public String toString()
            {
            return new MemorySize(f_cbBuffer).toString() + "(" + (getAcquired() * 100) / (f_cBufferGen * GEN_ID_UNPOOLED) + "%)";
            }

        // ----- constants ----------------------------------------------

        /**
         * Generation id that indicates that the segment has yet to be initialized.
         */
        private static final int GEN_ID_EMPTY = -1;

        /**
         * Generation id that indicates that the generation lock has been
         * taken, which means that a thread is either growing or shrinking
         * the pool.
         */
        private static final int GEN_ID_LOCKED = -2;

        // ----- data members--------------------------------------------

        /**
         * The configured size of the buffers in the pool.
         */
        protected final int f_cbBuffer;

        /**
         * The number of buffers allocated in a generation.
         */
        protected final int f_cBufferGen;

        /**
         * The stack which pools all the buffers that are not acquired.
         * <p>
         * Maintained as a stack improves performance especially for the RDMA extended allocator as
         * keys for recently used buffers may still be in the HCA cache.
         */
        protected final Stack<ByteBuffer> f_stackBuf;

        /**
         * The current generation id. This is a counter that starts at zero
         * and goes up to <tt>GEN_ID_UNPOOLED</tt>. Buffers that belong to
         * generation id zero through <tt>(GEN_ID_UNPOOLED - 1)</tt> may be
         * pooled, and buffers that belong to generation id
         * <tt>GEN_ID_UNPOOLED</tt> are never pooled. By altering the
         * current generation id, only buffers of that (or older) generation
         * are returned to the pool when they are released.
         */
        protected final AtomicInteger f_cGeneration;

        /**
         * The next the segment should be evaluated for shrinkage.
         */
        private volatile long m_ldtNextEvaluation;

        /**
         * This is the count of pooled buffers that have been handed out by
         * this pool.
         */
        protected final AtomicLong f_cAcquired;

        /**
         * The count of non-pooled allocations.
         */
        protected final AtomicLong f_cNonPooledAllocations;

        /**
         * This is the count of buffers that have been released back to this
         * pool.
         */
        protected final AtomicLong f_cReleased;

        /**
         * The count of the number of unpooled releases.
         */
        protected final AtomicLong f_cNonPooledReleased;

        /**
         * The size of a non-pooled buffer. Since the size of a buffer
         * indicates its generation id, we pre-calculate the size of the
         * generation of buffers that are not pooled.
         */
        protected final int f_cbUnpooledBuffer;

        /**
         * The peak recorded sample usage during the pending cleanup interval.
         */
        private int m_cMaxBuffers;

        /**
         * The peak recorded sample usage since the last shrinkage.
         */
        private int m_cMaxBuffersHistoric;
        }


    // ----- inner class: BufferAllocator -----------------------------------

    /**
     * A BufferAllocator is provides a mean for allocating ByteBuffers.
     */
    public interface BufferAllocator
        {
        /**
         * Allocate and return buffer of the specified size.
         *
         * @param cb  the required buffer size
         *
         * @return the buffer
         *
         * @throws OutOfMemoryError if the request cannot be satisified
         */
        public ByteBuffer allocate(int cb);

        /**
         * Release a ByteBuffer back to the allocator.
         *
         * @param buff  the buffer to release
         */
        public void release(ByteBuffer buff);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The generation id is stored in bits 6-9 (bits 0-5 are reserved to
     * enforce a 64 byte paragraph boundary).
     */
    private static final int GEN_ID_SHIFT = 6;

    /**
     * The number of bits reserved to store the generation id.
     */
    protected static final int GEN_ID_BITS = 4;

    /**
     * The bit mask of the bits used to store the generation id inside a size.
     */
    private static final int GEN_ID_MASK = ((1 << GEN_ID_BITS) - 1)
            << GEN_ID_SHIFT;

    /**
     * The ID of the generation that is freely allocated and un-pooled. In
     * other words, when we reach this generation, the buffers that are
     * allocated will not be returned to the pool when they are released.
     */
    protected static final int GEN_ID_UNPOOLED = (1 << GEN_ID_BITS) - 1;

    /**
     * The ID of the first generation at which truncation will be considered necessary.
     * Exempting lower generations from truncation allows for the truncate logic to very
     * efficiently avoid truncating buffers when there is significant space available
     * within the segment.
     */
    private static final int GEN_ID_TRUNCATE = (GEN_ID_UNPOOLED * 2) / 3;

    /**
     * The number of times an unpooled buffer is reused before being released.
     */
    public static final long UNPOOLED_RECLAIM_INTERVAL = 1024;

    /**
     * The default release frequency at which to records statistics.
     *
     * Note this value must be a power of two - 1.
     */
    public static final int STATS_FREQUENCY = 255;

    /**
     * The default size of the smallest ByteBuffer. Note that the actual size
     * of the ByteBuffer may be larger, since the size includes an implicit
     * generation ID. Also note that the default buffer size cannot use any of
     * the bits 0-9.
     */
    public static final int DEFAULT_BUF_SIZE = 1
            << (GEN_ID_BITS + GEN_ID_SHIFT); // 1024

    /**
     * The growth factor between each pool segment. The factor describes the
     * number of left shifts.
     */
    public static final short DEFAULT_GROWTH_FACTOR = 1;

    /**
     * The default number of segments.
     */
    public static final int DEFAULT_SEGMENT_COUNT = 7; // sufficiently large to allow for 1KB..64KB segments, covering standard MTU sizes

    /**
     * Reevaluation of capacity does not occur more than once every
     * period, as defined here. Furthermore, reevaluation of one pool
     * (i.e. a pool of one buffer size) will ensure that the other pools
     * (i.e. the pools that hold buffers of the other sizes) have also
     * reevaluated, since it is possible that they have no activity that
     * would cause them to reevaluate on their own.
     */
    protected static final long CLEANUP_FREQUENCY_MILLIS = new Duration(System.getProperty(
            SegmentedBufferManager.class.getName() + ".cleanup.frequency", "1s")).as(Duration.Magnitude.MILLI);

    /**
     * The logger.
     */
    protected static final Logger LOGGER = Logger.getLogger(
            SegmentedBufferManager.class.getName());

    // ----- data members ---------------------------------------------------

    /**
     * The BufferAllocator to use to grow the pool.
     */
    private final BufferAllocator m_allocator;

    /**
     * The pool is composed of several sub-ordinate pools represented by the
     * PoolSegment inner class, each of a specific allocation size. This array
     * holds the various PoolSegment instances, starting with the smallest
     * allocation size and proceeding to the largest.
     */
    private final Segment[] f_aSegments;

    /**
     * The buffer size of the smallest buffer.
     */
    private final int m_cbMin;

    /**
     * The buffer size of the largest buffer.
     */
    private final int m_cbMax;

    /**
     * The segment growth factor.
     */
    private final int m_nSegmentGrowthFactor;

    /**
     * The name of the allocator
     */
    protected String m_sName = getClass().getSimpleName() + "(" + System.identityHashCode(this) + ")";
    }
