/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.oracle.coherence.common.collections.ConcurrentLinkedQueue;

import java.util.Queue;
import java.util.concurrent.locks.LockSupport;


/**
 * SingleWaiterCooperativeNotifier is an extension of the SingleWaiterMultiNotifier which attempts to offload
 * potentially expensive "notification" work from signaling threads onto the waiting threads. This notifier
 * is beneficial when there are few signaling threads, but potentially many waiting threads, each waiting on
 * their own notifier.
 * <p>
 * Unlike the standard Notifier usage, a signaling thread must at some point invoke the static {@link #flush}
 * method, or {@link #await} on any SingleWaiterCooperativeNotifier to ensure that all deferred signals
 * are processed.
 *
 * @author mf  2014.03.12
 */
public class SingleWaiterCooperativeNotifier
        extends SingleWaiterMultiNotifier
    {
    // ----- OffloadingMultiNotifier interface ------------------------------

    /**
     * Ensure that any deferred signals will be processed.
     * <p>
     * Note it is more advantageous if the calling thread's natural idle point is
     * to sit in {@link #await}, in which case calling this method is not required.
     * </p>
     */
     public static void flush()
        {
        flush(FLUSH_AWAKE_COUNT, null);
        }

    // ----- Notifier interface ---------------------------------------------

    @Override
    public void await(long cMillis)
            throws InterruptedException
        {
        flush(cMillis <= 0 || cMillis > Integer.MAX_VALUE   // Note: using cMillis as a count just to limit iterations
                ? Integer.MAX_VALUE : (int) cMillis, this); //       relative to the allowable wait time
        try
            {
            m_threadAwait = Thread.currentThread(); // done before await's cas to ensure visibility if we wait
            super.await(cMillis);
            }
        finally
            {
            flush(2, /*self (not waiting)*/  null); // wakeup at least two other threads, forming a wakeup tree
            }
        }

    @Override
    public void signal()
        {
        if (signalInternal() != null)
            {
            s_queueDeferred.add(this);
            }
        // else; no thread to wake; signaling is completed

        if (++s_cSignal == 0) // periodically ensure things are moving
            {
            flush(1, /*self (not waiting)*/ null);
            }
        }

    // ----- Object interface -----------------------------------------------

    @Override
    public String toString()
        {
        String s      = super.toString();
        Thread thread = m_threadAwait;

        if (thread != null && !s.endsWith(")"))
            {
            // delayed signal
            s += "(" + thread + ")";
            }
        return s + "/" + s_queueDeferred.size();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Ensure that any deferred signals will be processed.
     *
     * @param cWake the number of deferrals to signal on the calling thread
     * @param self  the waiting notifier which is flushing, or null
     */
    protected static void flush(int cWake, SingleWaiterCooperativeNotifier self)
        {
        for (int i = 0; i < cWake && (self == null || self.m_oState == null); )
            {
            SingleWaiterCooperativeNotifier notifier = s_queueDeferred.poll();
            if (notifier == null)
                {
                break;
                }

            Object oState = notifier.m_oState; // read to get clean view of m_threadAwait
            Thread thread = notifier.m_threadAwait;
            if (oState == notifier && thread != null)
                {
                LockSupport.unpark(thread);
                ++i;
                }
            // else; we can't be sure the signaled thread will ever call flush(), don't rely on it
            }
        }

    @Override
    protected void consumeSignal()
        {
        m_threadAwait = null; // ensure that null is written before we'd update m_oState to null in super
        super.consumeSignal();
        }


    // ----- data members ---------------------------------------------------

    /**
     * The waiting thread, or null. Visibility ensured via super.m_oState writes.
     */
    protected Thread m_threadAwait;

    /**
     * An intentionally non-volatile signal counter to ensure periodic flushes.
     *
     * byte is used to allow for auto-flush on every 256 signals (rollover)
     */
    protected static byte s_cSignal;

    /**
     * The deferred notifiers.
     *
     * Note: this doesn't have to be a queue (though that marginally improves fairness), it just needs to
     * be some highly concurrent data set.  Being a queue could result in this being a bottleneck.
     */
    private static final Queue<SingleWaiterCooperativeNotifier> s_queueDeferred = new ConcurrentLinkedQueue<>();

    /**
     * The number of threads to awake upon a static flush call.
     */
    private static final int FLUSH_AWAKE_COUNT = Integer.parseInt(
            System.getProperty(SingleWaiterCooperativeNotifier.class.getName() + ".wakeOnFlush", "1"));
    }
