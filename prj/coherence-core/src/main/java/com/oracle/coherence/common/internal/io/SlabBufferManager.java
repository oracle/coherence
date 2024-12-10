/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.io;

import com.oracle.coherence.common.collections.WeakIdentityHashMap;

import com.oracle.coherence.common.io.BufferManagers;

import java.lang.ref.WeakReference;

import java.nio.ByteBuffer;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.oracle.coherence.common.util.SafeClock;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.logging.Level;


/**
 * The SlabBufferManager is a BufferManager which allocates buffers in large slabs which are then sliced into smaller
 * ByteBuffers.
 *
 * @author mf 2013.03.18
 */
public class SlabBufferManager
        extends SegmentedBufferManager
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a SlabBufferManager.
     *
     * @param allocator    the BufferAllocator to use to fill the pool
     * @param cbBufferMin  the minimum buffer size
     * @param cbMax        the total amount of memory to pool
     */
    public SlabBufferManager(BufferAllocator allocator, int cbBufferMin, long cbMax)
        {
        super(allocator, cbBufferMin, cbMax);
        }

    /**
     * Creates a SlabBufferManager.
     *
     * @param allocator  the BufferAllocator to use to fill the pool
     * @param cbMax      the total amount of memory to pool
     */
    @SuppressWarnings("unused")
    public SlabBufferManager(BufferAllocator allocator, long cbMax)
        {
        super(allocator, cbMax);
        }

    /**
     * Creates a SlabBufferManager.
     *
     * @param allocator      the BufferAllocator to use to fill the pool
     * @param cSegments      the number of segments
     * @param cbSegment      the maximum number bytes each segment will consume
     * @param cbBufferMin    the smallest buffer size
     * @param nGrowthFactor  the segment growth factor
     */
    @SuppressWarnings("unused")
    public SlabBufferManager(BufferAllocator allocator, int cSegments, long cbSegment, int cbBufferMin,
            int nGrowthFactor)
        {
        super(allocator, cSegments, cbSegment, cbBufferMin, nGrowthFactor);
        }



    // ----- inner class: SlabSegment ---------------------------------------

    @Override
    protected Segment allocateSegment(int cbBuffer, int cBuffers)
        {
        return new SlabSegment(cbBuffer, cBuffers);
        }

    /**
     * The SlabSegment is a Segment which manages generations as slabs rather then as
     * individual buffers.
     */
    protected class SlabSegment
            extends Segment
        {
        /**
        * Construct a pool for buffers of the specified size.
        *
        * @param cbBuffer      the size of the individual ByteBuffers
        * @param cBuffers      the number of ByteBuffers for the pool
        */
        protected SlabSegment(int cbBuffer, int cBuffers)
            {
            super(cbBuffer, cBuffers);
            for (int i = 0, c = f_aSlabs.length; i < c; ++i)
                {
                f_aSlabs[i] = instantiateSlab(encodeGeneration(i));
                }
            }

        @Override
        protected int getAcquired()
            {
            return Math.max(0, super.getAcquired() - (int) f_cReclaimed.get());
            }

        @Override
        protected boolean allocateGenerationBuffers(int nGen, int cbBuffer)
            {
            // since we're growing perhaps we've leaked; ensure lower generations
            for (int i = 0; i < nGen; ++i)
                {
                if (f_aSlabs[i].ensure())
                    {
                    f_cReclaimed.addAndGet(getGenerationSize());
                    LOGGER.log(Level.WARNING, getName() + " replenished leaked segment '" + getBufferSize()
                            + "' slab for generation " + i +
                            "; set the com.oracle.common.io.BufferManagers.checked system property to true to help identify leak suspects");
                    }
                }

            // TODO: check unpooled slabs?

            Slab slab = f_aSlabs[nGen];
            if (!slab.ensure())
                {
                // revive generation that was never fully released
                LOGGER.log(Level.FINE, getName() + " reviving segment '" + getBufferSize() + "' slab for generation "
                        + nGen);
                }
            // else; new memory allocation

            return true;
            }


        @Override
        protected ByteBuffer allocateNonPooledBuffer()
            {
            synchronized (f_mapBufSlabUnpooled)
                {
                // drop any leaked slabs
                Slab slab = m_slabUnpooledHead;
                while (slab != null)
                    {
                    Slab slabPrev = slab.m_slabPrev;
                    if (slab.isLeaked())
                        {
                        removeSlabFromList(slab);

                        if (!slab.f_afOutstanding.isEmpty())
                            {
                            LOGGER.log(Level.WARNING, getName() + " detected leaked segment '" + getBufferSize()
                                    + "' slab for generation " + GEN_ID_UNPOOLED + "; " + slab.f_afOutstanding.cardinality() +
                                    " of " + f_cBufferGen + " buffers were leaked" +
                                    "; set the com.oracle.common.io.BufferManagers.checked system property to true to help identify leak suspects");
                            }
                        // else; just a GC
                        }

                    slab = slabPrev; // progress towards tail
                    }

                ByteBuffer buff = null;
                do
                    {
                    // attempt to just revive a partially released slab, head has the most reclaimable buffers
                    Slab slabRevive = m_slabUnpooledHead;
                    if (slabRevive != null)
                        {
                        if (!slabRevive.ensure())
                            {
                            // revive generation that was never fully released
                            LOGGER.log(Level.FINE, getName() + " reviving segment '" + getBufferSize() + "' slab for generation "
                                    + GEN_ID_UNPOOLED);
                            }

                        buff = f_stackBuf.pop();
                        if (buff != null)
                            {
                            return buff;
                            }
                        }

                    // allocate an unpooled slab
                    f_cNonPooledAllocations.incrementAndGet();
                    instantiateSlab(f_cbUnpooledBuffer).ensure(); // note: ensure inserts its buffers into the segment

                    LOGGER.log(Level.FINE, getName() + " allocating unpooled slab for segment '" + getBufferSize());

                    buff = f_stackBuf.pop();
                    }
                while (buff == null); // very unlikely to require more then one pass

                return buff;
                }
            }

        @Override
        protected boolean isShrinkable()
            {
            long ldtNow = SafeClock.INSTANCE.getSafeTimeMillis();
            if (m_ldtShrinkableEval < ldtNow - CLEANUP_FREQUENCY_MILLIS)
                {
                m_ldtShrinkableEval = ldtNow;
                GcTracker[] aInfo = m_aGcInfo;
                if (aInfo != null)
                    {
                    for (GcTracker info : aInfo)
                        {
                        if (info != null && !info.hasGCd())
                            {
                            // this collector hasn't collected since our last allocation, avoid dropping for now
                            // the intent here is to ultimately avoid creating new garbage until our old garbage
                            // has been cleaned up.  This is important when dealing with DirectByteBuffers as
                            // they don't cause GC pressure and thus may go along time without being GC'd which
                            // can cause a large off-heap memory leak.
                            return m_fShrinkable = false;
                            }
                        }
                    }

                return m_fShrinkable = true;
                }

            return m_fShrinkable;
            }

        @Override
        public void dropBuffer(ByteBuffer buffer)
            {
            int nGen = decodeGeneration(buffer.capacity());

            if (nGen == GEN_ID_UNPOOLED)
                {
                synchronized (f_mapBufSlabUnpooled)
                    {
                    Slab slab = f_mapBufSlabUnpooled.remove(buffer);
                    if (slab == null)
                        {
                        // the application has apparently has now, or at some point in the past given us a buffer which
                        // we didn't produce.  We have no evidence that it is the callers fault though, so we don't throw
                        LOGGER.log(Level.WARNING, getName() + " detected release of unknown ByteBuffer for segment '"
                                + getBufferSize() + "' generation " + nGen
                                + "; set the com.oracle.common.io.BufferManagers.checked system property to true to help identify suspects");
                        // return and let GC deal with it
                        }
                    else if (slab.releaseSlice(buffer))
                        {
                        f_cNonPooledAllocations.decrementAndGet();
                        }
                    }
                }
            else
                {
                f_aSlabs[nGen].releaseSlice(buffer);
                }
            }

        @Override
        public String toString()
            {
            int cUnpooledSlabs;
            synchronized (f_mapBufSlabUnpooled)
                {
                cUnpooledSlabs = new HashSet<>(f_mapBufSlabUnpooled.values()).size();
                }
            return super.toString() + '/' + (f_cGeneration.get() + 1 + cUnpooledSlabs);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Remove the specified {@link Slab} from the linked list of buffers.
         *
         * @param slabToRemove the slab to remove
         */
        protected void removeSlabFromList(Slab slabToRemove)
            {
            Slab slabNext = slabToRemove.m_slabNext;
            Slab slabPrev = slabToRemove.m_slabPrev;

            if (slabNext == null)
                {
                m_slabUnpooledHead = slabPrev;
                }
            else
                {
                slabNext.m_slabPrev = slabPrev;
                }

            if (slabPrev == null)
                {
                m_slabUnpooledTail = slabNext;
                }
            else
                {
                slabPrev.m_slabNext = slabNext;
                }

            slabToRemove.m_slabNext = slabToRemove.m_slabPrev = null;
            }

        /**
         * Instantiate a new unallocated slab.
         *
         * @param cbBuffer  the size of each buffer within the slab
         *
         * @return the slab
         */
        protected Slab instantiateSlab(int cbBuffer)
            {
            return new Slab(cbBuffer);
            }

        // ----- inner class: Slab ------------------------------------------

        /**
         * Slab represents a single Buffer allocation.
         */
        protected class Slab
            implements Comparable<Slab>
            {
            /**
             * Allocate a slab
             *
             * @param cbBuffer   the size of each buffer
             */
            public Slab(int cbBuffer)
                {
                f_cbSlice = cbBuffer;
                }

            @Override
            public String toString()
                {
                return "Slab " + System.identityHashCode(this) + ", bufSize " + f_cbSlice + ", leaked " + isLeaked() + ", outstanding " + f_afOutstanding.cardinality() + "/" + getGenerationSize();
                }

            /**
             * Return true iff the segment is retaining no actual memory.
             *
             * @return true iff the segment is retaining no actual memory.
             */
            public synchronized boolean isLeaked()
                {
                return m_weakBuf == null || m_weakBuf.get() == null;
                }

            /**
             * Return the buffer associated with this slab, or null if none is present
             *
             * @return the buffer
             */
            public ByteBuffer getSlabBuffer()
                {
                WeakReference<ByteBuffer> weakBuf = m_weakBuf;
                return weakBuf == null ? null : m_weakBuf.get();
                }

            /**
             * Release a slice back to the slab.
             *
             * @param buffer  the buffer
             *
             * @return true the slab was destroyed as part of this operation
             */
            public synchronized boolean releaseSlice(ByteBuffer buffer)
                {
                ByteBuffer bufSlab   = getSlabBuffer();
                int        nPosition = getSlicePosition(buffer);
                if (nPosition >= 0 && f_afOutstanding.get(nPosition) && bufSlab != null)
                    {
                    // retain knowledge of which slices have been released so we can restore them later
                    // if we need to grow before we've released all slices.  Note we don't hold refs to
                    // the released slices as that would largely prevent us from being able to detect a
                    // leaked segment
                    f_afOutstanding.clear(nPosition);

                    if (f_afOutstanding.isEmpty())
                        {
                        // we've reclaimed the entire generation (slab); release it now
                        LOGGER.log(Level.FINE, getName() + " releasing segment '" + getBufferSize()
                                + "' slab for generation " + decodeGeneration(f_cbSlice));
                        getAllocator().release(bufSlab);
                        m_weakBuf.clear();
                        m_weakBuf = null;

                        if (f_cbSlice == f_cbUnpooledBuffer)
                            {
                            removeSlabFromList(this);
                            }
                        return true;
                        }
                    else if (f_cbSlice == f_cbUnpooledBuffer)
                        {
                        // one additional slice is revivable, sort order may change
                        Slab slabNext = m_slabNext;

                        if (slabNext != null && compareTo(slabNext) < 0)
                            {
                            // swap positions with next, note we can only move forward
                            Slab slabPrev = m_slabPrev;

                            m_slabNext = slabNext.m_slabNext;
                            if (m_slabNext != null)
                                {
                                m_slabNext.m_slabPrev = this;
                                }

                            slabNext.m_slabPrev = slabPrev;
                            slabNext.m_slabNext = this;

                            m_slabPrev = slabNext;

                            if (slabPrev == null)
                                {
                                m_slabUnpooledTail = slabNext;
                                }
                            else
                                {
                                slabPrev.m_slabNext = slabNext;
                                }

                            if (m_slabUnpooledHead == slabNext)
                                {
                                m_slabUnpooledHead = this;
                                }
                            }
                        }
                    }
                else
                    {
                    // double release of slice
                    LOGGER.log(Level.WARNING, getName() + " double release of '" + getBufferSize()
                            + "' buffer in generation " + decodeGeneration(f_cbSlice) +
                            "; set the com.oracle.common.io.BufferManagers.checked system property to true to help identify suspects");
                    }

                return false;
                }

            /**
             * Ensure that the slab has not been leaked, and reallocate if it has.
             *
             * @return true iff ensure performed an allocation
             */
            public synchronized boolean ensure()
                {
                int        cBufGen = getGenerationSize();
                ByteBuffer bufSlab = getSlabBuffer();
                boolean    fAlloc  = bufSlab == null;

                if (fAlloc)
                    {
                    bufSlab   = getAllocator().allocate(f_cbSlice * cBufGen);
                    m_weakBuf = new WeakReference<>(bufSlab);
                    f_afOutstanding.clear(); // outstanding were leaked and GC'd

                    // record GC counts at the time of the most recent allocation
                    List<GarbageCollectorMXBean> listGc = ManagementFactory.getGarbageCollectorMXBeans();
                    GarbageCollectorMXBean[]     aGc    = listGc.toArray(new GarbageCollectorMXBean[listGc.size()]);
                    GcTracker[]                     aInfo  = new GcTracker[aGc.length];
                    for (int i = 0; i < aGc.length && aGc[i] != null; ++i)
                        {
                        aInfo[i] = new GcTracker(aGc[i]);
                        }
                    m_aGcInfo = aInfo;
                    }
                else if (f_afOutstanding.cardinality() == cBufGen)
                    {
                    // none of the buffers were ever released
                    return false;
                    }
                // else; there are some released buffers we can reslice and return

                if (f_cbSlice == f_cbUnpooledBuffer)
                    {
                    Slab slabNext = m_slabNext;
                    Slab slabPrev = m_slabPrev;

                    // unlink self (likely from head)
                    if (slabNext != null)
                        {
                        slabNext.m_slabPrev = slabPrev;
                        }
                    else if (m_slabUnpooledHead == this)
                        {
                        m_slabUnpooledHead = slabPrev;
                        }
                    // else; not in chain

                    if (slabPrev != null)
                        {
                        slabPrev.m_slabNext = slabNext;
                        }
                    else if (m_slabUnpooledTail == this)
                        {
                        m_slabUnpooledTail = slabNext;
                        }
                    // else; not in chain

                    // (re)link at the tail
                    Slab slabTail = m_slabUnpooledTail;

                    m_slabUnpooledTail = this;
                    m_slabNext         = slabTail;
                    m_slabPrev         = null;

                    if (slabTail == null)
                        {
                        m_slabUnpooledHead = this;
                        }
                    else
                        {
                        slabTail.m_slabPrev = this;
                        }
                    }

                // slice and return
                for (int i = f_afOutstanding.nextClearBit(0); i < cBufGen; i = f_afOutstanding.nextClearBit(i))
                    {
                    int ofStart = i * f_cbSlice;
                    int ofEnd   = ofStart + f_cbSlice;
                    bufSlab.limit(ofEnd).position(ofStart);

                    ByteBuffer bufSlice = bufSlab.slice();

                    if (f_cbSlice == f_cbUnpooledBuffer)
                        {
                        // caller must hold sync on f_mapBufSlabUnpooled
                        f_mapBufSlabUnpooled.put(bufSlice, this);
                        }

                    f_afOutstanding.set(i);
                    f_stackBuf.push(bufSlice);
                    }

                bufSlab.clear(); // leave pos & limit at their extremes so we can safely invoke getSlicePosition later

                return fAlloc;
                }

            /**
             * Return the position (measured in slices) of this specified slice within the slab.  The slice must be available
             * for writes, i.e. not in use by the application.
             *
             * @param buffSlice  the slice
             *
             * @return the position, or -1 if not found
             */
            protected synchronized int getSlicePosition(ByteBuffer buffSlice)
                {
                // determine starting offset of buffer withing slab
                ByteBuffer buffSlab = getSlabBuffer();
                if (buffSlab == null)
                    {
                    return -1;
                    }
                else if (buffSlice.hasArray() && buffSlab.hasArray())
                    {
                    return buffSlice.array() == buffSlab.array() ? buffSlice.arrayOffset() / buffSlice.capacity() : -1;
                    }
                else if (buffSlice.isDirect())
                    {
                    int               cbSlice       = buffSlice.capacity(); // all slices from this slab are the same size
                    ThreadLocalRandom tlRandom      = ThreadLocalRandom.current();
                    long              lThreadId     = Thread.currentThread().getId();
                    long              lRand;

                    // obtain and write a random value to the buffer and verify the value is present within the slab
                    do
                        {
                        lRand = tlRandom.nextLong();
                        }
                    while (lRand == 0);
                    buffSlice.putLong(0, lRand); // slice is released so we can write to it

                    // probe outstanding slab positions for the random write
                    for (int nPosition = f_afOutstanding.nextSetBit(0);
                         nPosition >= 0;
                         nPosition = f_afOutstanding.nextSetBit(nPosition + 1))
                        {
                        int ofSlab = nPosition * cbSlice;
                        if (buffSlab.getLong(ofSlab) == lRand)
                            {
                            // looks like a match, verify with an additional non-random write
                            long lUnique = (((long) nPosition) << 32) | lThreadId;
                            buffSlice.putLong(0, lUnique);
                            if (buffSlab.getLong(ofSlab) == lUnique)
                                {
                                // "confirmed"
                                if (BufferManagers.ZERO_ON_RELEASE)
                                    {
                                    buffSlice.putLong(0, 0); // finish by zeroing out the value
                                    }
                                return nPosition;
                                }

                            buffSlice.putLong(0, lRand);
                            }
                        }

                    return -1;
                    }

                throw new IllegalStateException();
                }

            @Override
            public int compareTo(Slab that)
                {
                return f_afOutstanding.cardinality() - that.f_afOutstanding.cardinality();
                }

            /**
             * The size of each slice.
             */
            protected final int f_cbSlice;

            /**
             * BitSet indicating which slices are outstanding
             */
            protected final BitSet f_afOutstanding = new BitSet(getGenerationSize());

            /**
             * WeakReference tracking the slab ByteBuffer
             */
            protected WeakReference<ByteBuffer> m_weakBuf;

            /**
             * Next largest unpooled slab.
             */
            protected Slab m_slabNext;

            /**
             * Next smallest unpooled slab.
             */
            protected Slab m_slabPrev;
            }

        // ----- inner class: GcTracker ----------------------------------------

        /**
         * GcTracker provides a mechanism to detect if a GC has occurred in the supplied
         * collector since the tracker was created.
         */
        protected class GcTracker
            {
            GcTracker(GarbageCollectorMXBean bean)
                {
                this.f_cGcLast = bean.getCollectionCount();
                this.f_bean = bean;
                }

            public boolean hasGCd()
                {
                return f_cGcLast != f_bean.getCollectionCount();
                }

            private final long f_cGcLast;
            private final GarbageCollectorMXBean f_bean;
            }

        // ----- data members -----------------------------------------------

        /**
         * Array of slabs for poolable generations.
         */
        protected final Slab[] f_aSlabs = new Slab[GEN_ID_UNPOOLED];

        /**
         * Map from unpooled ByteBuffers to their corresponding unpooled slab.
         *
         * Weak to allow leaks in the application to not cause true memory leaks.
         *
         * All access must be synchronized on the map.
         */
        protected final Map<ByteBuffer, Slab> f_mapBufSlabUnpooled = new WeakIdentityHashMap<>();

        /**
         * The slab with the most available reclaimable slices
         */
        protected Slab m_slabUnpooledHead;

        /**
         * The slab with the least available reclaimable slices
         */
        protected Slab m_slabUnpooledTail;

        /**
         * The number of reclaimed buffers.
         */
        protected final AtomicLong f_cReclaimed = new AtomicLong();

        /**
         * GC counter info as of last slab allocation.
         */
        private volatile GcTracker[] m_aGcInfo;

        /**
         * The cached shrinkable state.
         */
        private volatile boolean m_fShrinkable;

        /**
         * The time at which m_fShrinkable was last calculated
         */
        private long m_ldtShrinkableEval;
        }

    // ----- inner class: DirectBufferAllocator -----------------------------

    /**
     * A buffer allocator that allow the associated memory of the direct buffer
     * to be released explicitly.
     */
    public static class DirectBufferAllocator
            implements BufferAllocator
        {

        @Override
        public ByteBuffer allocate(int cb)
            {
            return ByteBuffer.allocateDirect(cb);
            }

        @Override
        public void release(ByteBuffer buff)
            {
            if (!buff.isDirect())
                {
                throw new IllegalArgumentException();
                }

            clean(buff);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Release the associated memory of the specified direct byte buffer.
         *
         * @param buff  the buffer to be cleaned
         */
        protected static void clean(ByteBuffer buff)
            {
            Consumer<ByteBuffer> consumerClean = m_cleaner;
            if (consumerClean == null)
                {
                synchronized (DirectBufferAllocator.class)
                    {
                    if ((consumerClean = m_cleaner) == null)
                        {
                        Consumer<ByteBuffer> consumerTmp = REFLECTION_CLEANER;
                        try
                            {
                            consumerTmp.accept(buff);
                            }
                        catch (Throwable t)
                            {
                            consumerTmp = VOID_CLEANER;
                            }
                        finally
                            {
                            m_cleaner = consumerTmp;
                            }
                        }
                    }
                }

            if (consumerClean != null)
                {
                consumerClean.accept(buff);
                }
            }

        // ----- data members------------------------------------------------

        /**
         * A dummy ByteBuffer cleaner.
         */
        private static final Consumer<ByteBuffer> VOID_CLEANER = bb -> {};

        /**
         * The direct byte buffer cleaner via reflection.
         */
        private static final Consumer<ByteBuffer> REFLECTION_CLEANER = new Consumer<ByteBuffer>()
            {
            @Override
            public void accept(ByteBuffer buff)
                {
                try
                    {
                    Method methClean = m_methClean;
                    Object oCleaner;

                    if (methClean == null)
                        {
                        Method methCleaner = m_methCleaner = buff.getClass().getDeclaredMethod("cleaner", null);
                        methCleaner.setAccessible(true);
                        oCleaner = methCleaner.invoke(buff);

                        m_methClean = methClean = oCleaner.getClass().getDeclaredMethod("clean", null);
                        }
                    else
                        {
                        oCleaner = m_methCleaner.invoke(buff);
                        }

                    methClean.invoke(oCleaner, null);
                    }
                catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
                    {
                    m_methCleaner = m_methClean = null;

                    throw new RuntimeException(e);
                    }
                }

            /**
             * The Cleaner method.
             */
            private Method m_methCleaner;

            /**
             * The clean method
             */
            private Method m_methClean;
            };

        /**
         * The cached cleaner for direct byte buffer.
         */
        private static Consumer<ByteBuffer> m_cleaner;
        }
    }
