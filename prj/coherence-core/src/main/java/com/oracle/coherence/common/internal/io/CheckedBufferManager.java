/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.io;


import com.oracle.coherence.common.base.InverseComparator;
import com.oracle.coherence.common.internal.util.HeapDump;
import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferManagers;
import com.oracle.coherence.common.util.MemorySize;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * CheckedBufferManager is a BufferManager wrapper which adds on safety checks
 * to detect improper re-use of ByteBuffers.
 *
 * @author mf  2011.03.29
 */
public class CheckedBufferManager
        extends WrapperBufferManager
    {
    // ----- constructor --------------------------------------------

    /**
     * Construct a CheckedBufferManager around the specified manager.
     *
     * @param mgr the manager to delegate to
     */
    public CheckedBufferManager(BufferManager mgr)
        {
        super(mgr);
        }

    // ----- BufferManager interface --------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer acquire(int cbMin)
        {
        ByteBuffer buff;
        int cAttempts = 0;
        AcquisitionSite errorSite, site = null;
        do
            {
            errorSite = site;
            buff = f_delegate.acquire(cbMin);
            ++cAttempts;
            }
        while (!zeroed(buff) || (site = m_mapAllocated.put(buff, new AcquisitionSite())) != null);

        checkLeaks(m_cbBufferAllocated.addAndGet(buff.capacity()));

        if (cAttempts > 1)
            {
            // this should only happen do to either an internal manager
            // error, or because another thread is using the delegate
            // manager directly and it is the one messed up, just log
            // here
            LOGGER.log(Level.WARNING, "Compensating for unaccounted for" +
                    " ByteBuffer re-use of " + (cAttempts - 1) +
                    " buffers for request size of " + cbMin + " from " +
                    f_delegate + (errorSite == null ? "" : (" last " + errorSite)));
            }

        return buff;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer acquirePref(int cbPref)
        {
        ByteBuffer buff;
        int cAttempts = 0;
        AcquisitionSite errorSite, site = null;
        do
            {
            errorSite = site;
            buff = f_delegate.acquirePref(cbPref);
            ++cAttempts;
            }
        while (!zeroed(buff) || (site = m_mapAllocated.put(buff, new AcquisitionSite())) != null);

        checkLeaks(m_cbBufferAllocated.addAndGet(buff.capacity()));

        if (cAttempts > 1)
            {
            // this should only happen do to either an internal manager
            // error, or because another thread is using the delegate
            // manager directly and it is the one messed up, just log
            // here
            LOGGER.log(Level.WARNING, "Compensating for unaccounted for" +
                    " ByteBuffer re-use of " + (cAttempts - 1) +
                    " buffers for prefered size of " + cbPref + " from " +
                    f_delegate + (errorSite == null ? "" : (" last " + errorSite)));
            }

        return buff;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer acquireSum(int cbSum)
        {
        ByteBuffer buff;
        int cAttempts = 0;
        AcquisitionSite errorSite, site = null;

        do
            {
            errorSite = site;
            buff = f_delegate.acquireSum(cbSum);
            ++cAttempts;
            }
        while (!zeroed(buff) || (site = m_mapAllocated.put(buff, new AcquisitionSite())) != null);

        checkLeaks(m_cbBufferAllocated.addAndGet(buff.capacity()));

        if (cAttempts > 1)
            {
            // this should only happen do to either an internal manager
            // error, or because another thread is using the delegate
            // manager directly and it is the one messed up, just log
            // here
            LOGGER.log(Level.WARNING, "Compensating for unaccounted for" +
                    " ByteBuffer re-use of " + (cAttempts - 1) +
                    " buffers for accumulated size of " + cbSum +
                    f_delegate + (errorSite == null ? "" : (" last " + errorSite)));
            }

        return buff;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer truncate(ByteBuffer buff)
        {
        Map<ByteBuffer, AcquisitionSite> mapAllocated = m_mapAllocated;
        RuntimeException e;

        AcquisitionSite site = mapAllocated.remove(buff);
        if (site == null)
            {
            e = new IllegalArgumentException(
                    "Rejecting attempt to truncate" +
                            " unknown buffer of size " + buff.capacity() +
                            " from " + f_delegate);
            }
        else
            {
            ByteBuffer buffNew = f_delegate.truncate(buff);

            checkLeaks(m_cbBufferAllocated.addAndGet(buffNew.capacity() - buff.capacity()));

            AcquisitionSite errorSite;
            if ((errorSite = mapAllocated.put(buffNew, buff == buffNew ? site : new AcquisitionSite())) == null)
                {
                return buffNew;
                }

            e = new IllegalStateException(
                    "Unable to safely compensate for unaccounted " +
                            " ByteBuffer re-use of truncated buffer size of " +
                            buff.limit() + " to " + f_delegate + " prior " + errorSite);
            }

        LOGGER.log(Level.SEVERE, e.getMessage(), e);
        throw e;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(ByteBuffer buff)
        {
        if (m_mapAllocated.remove(buff) == null)
            {
            IllegalArgumentException e = new IllegalArgumentException(
                    "Rejecting attempt to release" +
                            " unknown buffer of size " + buff.capacity() +
                            " to " + f_delegate);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw e;
            }
        else
            {
            m_cbBufferAllocated.addAndGet(-buff.capacity());
            f_delegate.release(buff);
            }
        }

    // ----- Object interface ---------------------------------------

    @Override
    public String toString()
        {
        int                    cBuff      = m_mapAllocated.size();
        long                   cbNow      = m_cbBufferAllocated.get();
        long                   cbHigh     = m_cbHigh.get();
        long                   cbLow      = m_cbLow.get();
        Set<LeakSuspect> setSuspect = getSuspects(BYTE_WARNING_INTERVAL / 4);

        StringBuilder sb = new StringBuilder()
                .append("CheckedBufferManager(oustanding buffers=").append(cBuff)
                .append(", bytes(low=").append(new MemorySize(cbLow))
                .append(", allocated=").append(new MemorySize(cbNow))
                .append(", high=").append(new MemorySize(cbHigh))
                .append("), suspects=").append(setSuspect.size())
                .append(", delegate=").append(f_delegate)
                .append(")");

        if (setSuspect.size() > 0)
            {
            sb.append("\nSuspects:");
            }
        for (LeakSuspect record : setSuspect)
            {
            sb.append("\n").append(record);
            }

        return sb.toString();
        }


    // ----- Disposable interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
        {
        f_delegate.dispose();
        }


    // ----- CheckedBufferManager helpers ---------------------------

    /**
     * Verify that the specified buffer contains all zero content.
     *
     * @param buffer  the buffer to verify
     *
     * @return true iff the buffer is zeroed
     */
    protected static boolean zeroed(ByteBuffer buffer)
        {
        if (BufferManagers.ZERO_ON_RELEASE)
            {
            int cb    = buffer.capacity();
            int ofLim = buffer.limit();
            buffer.limit(cb);

            for (int i = 0; i < cb; ++i)
                {
                if (buffer.get(i) != 0)
                    {
                    return false;
                    }
                }

            buffer.limit(ofLim);
            }
        // else; underlying manager isn't zeroing so we can't ensure zeros, note that we can't
        // zero within the checked manager because of how truncate works

        return true;
        }

    /**
     * Called on each allocation in order to check for leaks.
     *
     * @param cbAllocated  the current allocation count
     */
    protected void checkLeaks(long cbAllocated)
        {
        long cbWarnNext = m_cbWarnNext.get();
        if (cbAllocated > cbWarnNext && m_cbWarnNext.compareAndSet(cbWarnNext,
                Math.max(cbAllocated, cbWarnNext) + BYTE_WARNING_INTERVAL))
            {
            String sDump   = DUMP_ON_WARNING ? HeapDump.dumpHeap() : null;
            String sSuffix = sDump == null ? "" : ("; " + sDump + " has been collected for analysis");

            LOGGER.warning("Passing new allocation limit: " + this + sSuffix);

            // reset low/high at start of cycle
            m_cbLow.set(cbAllocated);
            m_cbHigh.set(cbAllocated);
            }
        else
            {
            // update low
            for (long cbLow = m_cbLow.get();
                 cbAllocated < cbLow && !m_cbLow.compareAndSet(cbLow, cbAllocated);
                 cbLow = m_cbLow.get());

            // update lifetime high
            for (long cbHigh = m_cbHigh.get();
                 cbAllocated > cbHigh && !m_cbHigh.compareAndSet(cbHigh, cbAllocated);
                 cbHigh = m_cbHigh.get());
            }
        }

    /**
     * AcquisitionSite represents an acquisition call site.
     */
    public static class AcquisitionSite
        {
        final StackTraceElement[] f_aStackTraceElement;

        public AcquisitionSite()
            {
            if (SAMPLING_RATIO == 1 || ThreadLocalRandom.current().nextInt(SAMPLING_RATIO) == 0)
                {
                StackTraceElement[] stack = new Throwable().getStackTrace();

                if (stack.length > 1)
                    {
                    // trim this frame
                    StackTraceElement[] stackTrim = new StackTraceElement[stack.length - 1];

                    for (int i = 0; i < stackTrim.length; ++i)
                        {
                        stackTrim[i] = stack[i + 1];
                        }

                    stack = stackTrim;
                    }

                f_aStackTraceElement = stack;
                }
            else
                {
                f_aStackTraceElement = null;
                }
            }

        /**
         * Return the stack trace associated with this site.
         *
         * @return the stack trace associated with this site
         */
        public StackTraceElement[] getStackTrace()
            {
            return f_aStackTraceElement;
            }

        @Override
        public String toString()
            {
            StringBuilder       sb    = new StringBuilder("AcquisitionSite");
            StackTraceElement[] stack = getStackTrace();

            if (stack == null)
                {
                return sb.append(" - unsampled").toString();
                }

            sb.append('\n');
            for (StackTraceElement e : stack)
                {
                sb.append("\tat ").append(e).append('\n');
                }

            return sb.toString();
            }

        @Override
        public boolean equals(Object obj)
            {
            if (obj == this)
                {
                return true;
                }
            else if (obj == null)
                {
                return false;
                }

            return Arrays.equals(getStackTrace(), ((AcquisitionSite) obj).getStackTrace());
            }

        @Override
        public int hashCode()
            {
            return Arrays.hashCode(getStackTrace());
            }
        }

    /**
     * AcquisitionRecord represents statistics for unrelased memory associated with a single allocate site.
     */
    public static class LeakSuspect
        implements Comparable<LeakSuspect>
        {
        AcquisitionSite site;
        long            cOccurances;
        long            cb;

        /**
         * Return the site at which the acquisitions were made.
         *
         * @return the site at which the acquisitions were made
         */
        public AcquisitionSite getAcquisitionSite()
            {
            return site;
            }

        /**
         * Return the number of unreleased bytes for the associated allocation site.
         *
         * @return the number of unreleased bytes for the associated allocation site
         */
        public long getByteCount()
            {
            return cb;
            }

        /**
         * Return the number of unreleased buffers for the associated allocation site.
         *
         * @return the number of unreleased buffers for the associated allocation site
         */
        public long getBufferCount()
            {
            return cOccurances;
            }

        @Override
        public int compareTo(LeakSuspect that)
            {
            long cbDiff = cb - that.cb;
            long cDiff  = cOccurances - that.cOccurances;
            return (int) (cbDiff == 0
                          ? cDiff == 0
                            ? hashCode() - that.hashCode()
                            : cDiff
                          : cbDiff);
            }

        @Override
        public String toString()
            {
            return new StringBuilder()
                    .append(cOccurances).append(" acquisitions consuming a total of ").append(new MemorySize(cb))
                    .append(" from ").append(site).toString();
            }
        }

    /**
     * Return a set of leak suspects describing suspect leaks.
     *
     * @param cbSuspect the cumulative unreleased memory for an leak to be suspected.
     *
     * @return a set of leak suspects sorted by the size of the potential leak
     */
    public SortedSet<LeakSuspect> getSuspects(long cbSuspect)
        {
        Map.Entry<ByteBuffer, AcquisitionSite>[] aEntry = new Map.Entry[0];
        synchronized (m_mapAllocated)
            {
            aEntry = m_mapAllocated.entrySet().toArray(aEntry);
            }

        SortedSet<LeakSuspect> setSuspect = new TreeSet<LeakSuspect>(InverseComparator.INSTANCE);

        // group retained buffers by their acquisition site
        Map<AcquisitionSite, LeakSuspect> mapRecords = new HashMap<>();
        for (Map.Entry<ByteBuffer, AcquisitionSite> entry : aEntry)
            {
            ByteBuffer buff = entry.getKey();
            AcquisitionSite site = entry.getValue();
            LeakSuspect record = mapRecords.get(site);
            if (record == null)
                {
                record = new LeakSuspect();
                record.site = entry.getValue();
                mapRecords.put(site, record);
                }
            record.cOccurances++;
            record.cb += buff.capacity();
            if (record.cb > cbSuspect)
                {
                setSuspect.add(record);
                }
            }

        return setSuspect;
        }

    // ----- constants ----------------------------------------------

    /**
     * The Logger to use.
     */
    protected static final Logger LOGGER = Logger.getLogger(CheckedBufferManager
            .class.getName());

    /**
     * The interval at which to log warning messages.
     */
    protected static final long BYTE_WARNING_INTERVAL = new MemorySize(System.getProperty(
            CheckedBufferManager.class.getName() + ".limit", String.valueOf(Runtime.getRuntime().maxMemory() / 10)))
            .getByteCount();

    /**
     * The interval at which to log warning messages.
     */
    protected static final boolean DUMP_ON_WARNING = Boolean.getBoolean(CheckedBufferManager.class.getName() + ".dump");

    /**
     * The ratio at which acquisitions are sampled.
     */
    protected static final int SAMPLING_RATIO = Integer.parseInt(System.getProperty(
            CheckedBufferManager.class.getName() + ".samplingRatio", "1"));

    // ----- data members -------------------------------------------

    /**
     * Map of buffers which this manager has handed out but which have
     * yet to be released, to Site containing the stack of where
     * there were acquired.
     */
    protected final Map<ByteBuffer, AcquisitionSite> m_mapAllocated =
            Collections.synchronizedMap(
                    new IdentityHashMap<ByteBuffer, AcquisitionSite>());

    /**
     * The total number of bytes currently handed out.
     */
    protected final AtomicLong m_cbBufferAllocated = new AtomicLong();

    /**
     * The next size at which to warn.
     */
    protected final AtomicLong m_cbWarnNext = new AtomicLong(BYTE_WARNING_INTERVAL);

    /**
     * The low water mark since the last warning.
     */
    protected final AtomicLong m_cbLow = new AtomicLong();

    /**
     * The all time high water mark.
     */
    protected final AtomicLong m_cbHigh = new AtomicLong();
    }