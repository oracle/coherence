/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.util.ArrayList;
import java.util.List;

import com.oracle.coherence.common.base.Blocking;
import java.util.concurrent.atomic.AtomicInteger;


/**
* An abstract base for processors that implement bundling strategy.
* <p>
* Assume that we receive a continuous and concurrent stream of individual
* operations on multiple threads in parallel. Let's also assume those individual
* operations have relatively high latency (network or database-related) and
* there are functionally analogous [bulk] operations that take a collection of
* arguments instead of a single one without causing the latency to grow
* linearly, as a function of the collection size. Examples of operations and
* topologies that satisfy these assumptions are:
* <ul>
*   <li> get() and getAll() methods for the {@link com.tangosol.net.NamedCache}
*        API for the partitioned cache service topology;
*   <li> put() and putAll() methods for the {@link com.tangosol.net.NamedCache}
*        API for the partitioned cache service topology;
*   <li> load() and loadAll() methods for the
*        {@link com.tangosol.net.cache.CacheLoader} API for the read-through
*        backing map topology;
*   <li> store() and storeAll() methods for the
*        {@link com.tangosol.net.cache.CacheStore} API for the write-through
*        backing map topology.
* </ul>
* <p>
* Under these assumptions, it's quite clear that the bundler could achieve a
* better utilization of system resources and better throughput if slightly
* delays the individual execution requests with a purpose of "bundling" them
* together and passing into a corresponding bulk operation. Additionally,
* the "bundled" request should be triggered if a bundle reaches a "preferred
* bundle size" threshold, eliminating a need to wait till a bundle timeout is
* reached.
* <p>
* Note: we assume that all bundle-able operations are idempotent and could be
* repeated if un-bundling is necessary due to a bundled operation failure.
*
* @author gg 2007.01.28
* @since Coherence 3.3
*/
public abstract class AbstractBundler
        extends Base
    {
    /**
    * Construct the bundler. By default, the timeout delay value is set to
    * one millisecond and the auto-adjustment feature is turned on.
    */
    public AbstractBundler()
        {
        Bundle bundle = instantiateBundle();
        bundle.setMaster();
        m_listBundle.add(bundle);
        }


    // ----- property accessors ----------------------------------------------

    /**
    * Obtain the bundle size threshold value.
    *
    * @return the bundle size threshold value expressed in the same units as the
    *         value returned by the {@link Bundle#getBundleSize()} method
    */
    public int getSizeThreshold()
        {
        return (int) m_dSizeThreshold;
        }

    /**
    * Specify the bundle size threshold value.
    *
    * @param cSize  the bundle size threshold value; must be positive value
    *        expressed in the same units as the value returned by the
    *        {@link Bundle#getBundleSize()} method
    */
    public void setSizeThreshold(int cSize)
        {
        if (cSize <= 0)
            {
            throw new IllegalArgumentException("Negative bundle size threshold");
            }
        m_dSizeThreshold = cSize;

        // reset the previous value used for auto adjustment
        m_dPreviousSizeThreshold = 0.0;
        }

    /**
    * Obtains the minimum number of threads that will trigger the bundler to
    * switch from a pass through to a bundled mode.
    *
    * @return a the number of threads threshold
    */
    public int getThreadThreshold()
        {
        return m_cThreadThreshold;
        }

    /**
    * Specify the minimum number of threads that will trigger the bundler to
    * switch from a pass through to a bundled mode.
    *
    * @param cThreads  the number of threads threshold
    */
    public void setThreadThreshold(int cThreads)
        {
        if (cThreads <= 0)
            {
            throw new IllegalArgumentException("Invalid thread threshold");
            }
        m_cThreadThreshold = cThreads;
        }

    /**
    * Obtain the timeout delay value.
    *
    * @return the timeout delay value in milliseconds
    */
    public long getDelayMillis()
        {
        return m_lDelayMillis;
        }

    /**
    * Specify the timeout delay value.
    *
    * @param lDelay the timeout delay value in milliseconds
    */
    public void setDelayMillis(long lDelay)
        {
        if (lDelay <= 0)
            {
            throw new IllegalArgumentException("Invalid delay value");
            }
        m_lDelayMillis = lDelay;
        }

    /**
    * Check whether or not the auto-adjustment is allowed.
    *
    * @return true iff the auto-adjustment is allowed
    */
    public boolean isAllowAutoAdjust()
        {
        return m_fAllowAuto;
        }

    /**
    * Specify whether or not the auto-adjustment is allowed..
    *
    * @param fAutoAdjust true if the auto-adjustment should be allowed;
    *                    false otherwise
    */
    public void setAllowAutoAdjust(boolean fAutoAdjust)
        {
        m_fAllowAuto = fAutoAdjust;
        }


    // ----- statistics ------------------------------------------------------

    /**
    * Update the statistics for this Bundle.
    */
    protected void updateStatistics()
        {
        List       listBundle = m_listBundle;
        Statistics stats      = m_stats;
        while (true)
            {
            try
                {
                long cTotalBundles = 0l;
                long cTotalSize    = 0l;
                long cTotalBurst   = 0l;
                long cTotalWait    = 0l;

                for (int i = 0, c = listBundle.size(); i < c; i++)
                    {
                    Bundle bundle = (Bundle) listBundle.get(i);

                    cTotalBundles += bundle.m_cTotalBundles;
                    cTotalSize    += bundle.m_cTotalSize;
                    cTotalBurst   += bundle.m_cTotalBurstDuration;
                    cTotalWait    += bundle.m_cTotalWaitDuration;
                    }

                long cDeltaBundles = cTotalBundles - stats.m_cBundleCountSnapshot;
                long cDeltaSize    = cTotalSize    - stats.m_cBundleSizeSnapshot;
                long cDeltaBurst   = cTotalBurst   - stats.m_cBurstDurationSnapshot;
                long cDeltaWait    = cTotalWait    - stats.m_cThreadWaitSnapshot;

                // log("DeltaBundles=" + cDeltaBundles + ", DeltaSize=" + cDeltaSize
                // + ", DeltaBurst=" + cDeltaBurst + ", DeltaWait=" + cDeltaWait);

                if (cDeltaBundles > 0 && cDeltaWait > 0)
                    {
                    stats.m_cAverageBundleSize = (int) Math.round(
                            ((double) cDeltaSize) / ((double) cDeltaBundles));
                    stats.m_cAverageBurstDuration = (int) Math.round(
                            ((double) cDeltaBurst) / ((double) cDeltaBundles));
                    stats.m_cAverageThreadWaitDuration = (int) Math.round(
                            ((double) cDeltaWait) / ((double) cDeltaBundles));
                    stats.m_nAverageThroughput = (int) Math.round(
                            ((double) cDeltaSize*1000) / (cDeltaWait));
                    }

                stats.m_cBundleCountSnapshot   = cTotalBundles;
                stats.m_cBundleSizeSnapshot    = cTotalSize;
                stats.m_cBurstDurationSnapshot = cTotalBurst;
                stats.m_cThreadWaitSnapshot    = cTotalWait;

                return;
                }
            catch (IndexOutOfBoundsException e)
                {
                // there is theoretical possibility that the Java Memory Model
                // causes the list size to be not in sync with the actual list
                // storage; try again
                }
            }
        }

    /**
    * Reset this Bundler statistics.
    */
    public void resetStatistics()
        {
        List listBundle = m_listBundle;
        while (true)
            {
            try
                {
                for (int i = 0, c = listBundle.size(); i < c; i++)
                    {
                    Bundle bundle = (Bundle) listBundle.get(i);

                    bundle.resetStatistics();
                    }
                break;
                }
            catch (IndexOutOfBoundsException e)
                {
                // there is theoretical possibility that the memory model causes
                // the list size to be not in sync with the actual list storage;
                // try again
                }
            }
        m_stats.reset();
        m_dPreviousSizeThreshold = 0.0;
        }

    /**
    * Adjust this Bundler's parameters according to the available statistical
    * information.
    */
    public void adjust()
        {
        Statistics stats = m_stats;

        double dSizePrev = m_dPreviousSizeThreshold;
        double dSizeCurr = m_dSizeThreshold;

        int nThruPrev = stats.m_nAverageThroughput;
        updateStatistics();
        int nThruCurr = stats.m_nAverageThroughput;

        // out("Size= " + (float) dSizePrev + ", Thru=" + nThruPrev + " -> "
        //  + "Size= " + (float) dSizeCurr + ", Thru=" + nThruCurr);

        if (isAllowAutoAdjust())
            {
            double dDelta = 0.0;

            if (dSizePrev == 0.0)
                {
                // the very first adjustment after reset
                dDelta = Math.max(1, 0.1*dSizeCurr);
                }
            else if (Math.abs(nThruCurr - nThruPrev) <=
                        Math.max(1, (nThruCurr + nThruPrev)/100))
                {
                // not more than 2% throughput change;
                // with a probability of 10% lets nudge the size up to 5%
                // in a random direction
                int nRandom = getRandom().nextInt(100);
                if (nRandom < 10 || Math.abs(dSizePrev - dSizeCurr) < 0.001)
                    {
                    dDelta = Math.max(1, 0.05*dSizeCurr);
                    if (nRandom < 5)
                        {
                        dDelta = -dDelta;
                        }
                    }
                }
            else if (nThruCurr > nThruPrev)
                {
                // the throughput has improved; keep moving the size threshold
                // in the same direction at the same rate
                dDelta = (dSizeCurr - dSizePrev);
                }
            else
                {
                // the throughput has dropped; reverse the direction with half
                // of the previous rate
                dDelta = (dSizePrev - dSizeCurr) / 2;
                }

            if (dDelta != 0.0)
                {
                double dSizeNew = dSizeCurr + dDelta;
                if (dSizeNew > 1.0)
                    {
                    // out("Adjusting size by: " +
                    //     (float) dDelta + " to " + (float) dSizeNew);
                    m_dPreviousSizeThreshold = dSizeCurr;
                    m_dSizeThreshold         = dSizeNew;
                    }
                }
            }
        }

    /**
    * Provide a human readable description for the Bundler object
    * (for debugging).
    *
    * @return a human readable description for the Bundler object
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
             + "{SizeThreshold="    + getSizeThreshold()
             + ", ThreadThreshold=" + getThreadThreshold()
             + ", DelayMillis="     + getDelayMillis()
             + ", AutoAdjust="      + (isAllowAutoAdjust() ? "on" : "off")
             + ", ActiveBundles="   + m_listBundle.size()
             + ", Statistics="      + m_stats
             + "}";
        }


    // ----- sublcassing support ---------------------------------------------

    /**
    * Retrieve any Bundle that is currently in the open state. This method does
    * not assume any external synchronization and as a result, a caller must
    * double check the returned bundle open state (after synchronizing on it).
    *
    * @return an open Bundle
    */
    protected Bundle getOpenBundle()
        {
        List listBundle    = m_listBundle;
        int  cBundles      = listBundle.size();
        int  iActiveBundle = m_iActiveBundle;
        try
            {
            for (int i = 0; i < cBundles; i++)
                {
                int    iBundle = (iActiveBundle + i) % cBundles;
                Bundle bundle  = (Bundle) listBundle.get(iBundle);

                if (bundle.isOpen())
                    {
                    m_iActiveBundle = iBundle;
                    return bundle;
                    }
                }
            }
        catch (IndexOutOfBoundsException e)
            {
            // there is theoretical possibility that the memory model causes
            // the list size to be not in sync with the actual list storage;
            // proceed with synchronization...
            }
        catch (NullPointerException e)
            {
            // ditto
            }

        // we may need to create a new Bundle; synchronize to prevent the
        // creation of unnecessary bundles
        synchronized (listBundle)
            {
            // double check under synchronization
            cBundles = listBundle.size();
            for (int i = 0; i < cBundles; i++)
                {
                int    iBundle = (iActiveBundle + i) % cBundles;
                Bundle bundle  = (Bundle) listBundle.get(iBundle);

                if (bundle.isOpen())
                    {
                    m_iActiveBundle = iBundle;
                    return bundle;
                    }
                }

            // nothing available; add a new one
            Bundle bundle = instantiateBundle();
            listBundle.add(bundle);
            m_iActiveBundle = cBundles;
            return bundle;
            }
        }

    /**
    * Instantiate a new Bundle object.
    *
    * @return a new Bundle object
    */
    protected abstract Bundle instantiateBundle();

    /**
    * Bundle represents a unit of optimized execution.
    */
    protected abstract class Bundle
            extends Base
        {
        /**
        * Default constructor.
        */
        protected Bundle()
            {
            m_iStatus = STATUS_OPEN;
            }

        // ----- accessors -------------------------------------------------

        /**
        * Check whether or not this bundle is open for adding request elements.
        *
        * @return true iff this Bundle is still open
        */
        protected boolean isOpen()
            {
            return m_iStatus == STATUS_OPEN;
            }

        /**
        * Check whether or not this bundle is in the "pending" state - awaiting
        * for the execution results.
        *
        * @return true iff this Bundle is in the "pending" state
        */
        protected boolean isPending()
            {
            return m_iStatus == STATUS_PENDING;
            }

        /**
        * Check whether or not this bundle is in the "processed" state -
        * ready to return the result of execution back to the client.
        *
        * @return true iff this Bundle is in the "processed" state
        */
        protected boolean isProcessed()
            {
            return m_iStatus == STATUS_PROCESSED ||
                   m_iStatus == STATUS_EXCEPTION;
            }

        /**
        * Check whether or not this bundle is in the "exception" state -
        * bundled execution threw an exception and requests have to be
        * un-bundled.
        *
        * @return true iff this Bundle is in the "exception" state
        */
        protected boolean isException()
            {
            return m_iStatus == STATUS_EXCEPTION;
            }

        /**
        * Change the status of this Bundle.
        *
        * @param iStatus  the new status value
        */
        protected synchronized void setStatus(int iStatus)
            {
            boolean fValid;
            switch (m_iStatus)
                {
                case STATUS_OPEN:
                    fValid = iStatus == STATUS_PENDING
                          || iStatus == STATUS_EXCEPTION;
                    break;

                case STATUS_PENDING:
                    fValid = iStatus == STATUS_PROCESSED
                          || iStatus == STATUS_EXCEPTION;
                    break;

                case STATUS_PROCESSED:
                case STATUS_EXCEPTION:
                    fValid = iStatus == STATUS_OPEN;
                    break;
                default:
                    fValid = false;
                }

            if (!fValid)
                {
                throw new IllegalStateException(this +
                    "; invalid transition to " + formatStatusName(iStatus));
                }

            m_iStatus = iStatus;

            if (iStatus == STATUS_PROCESSED
             || iStatus == STATUS_EXCEPTION)
                {
                m_cTotalWaitDuration +=
                    Math.max(0l, System.currentTimeMillis() - m_ldtStart);
                notifyAll();
                }
            }

        /**
        * Obtain this bundle size. The return value should be expressed in the
        * same units as the value returned by the
        * {@link AbstractBundler#getSizeThreshold getSizeThreshold} method.
        *
        * @return the bundle size
        */
        protected int getBundleSize()
            {
            return m_cThreads;
            }

        /**
        * Check whether or not this is a "master" Bundle.
        *
        * @return true iff this Bundle is a designated "master" Bundle
        */
        protected boolean isMaster()
            {
            return m_fMaster;
            }

        /**
        * Designate this Bundle as a "master" bundle.
        */
        protected void setMaster()
            {
            m_fMaster = true;
            }

        // ----- processing and subclassing support --------------------------

        /**
        * Obtain results of the bundled requests. This method should be
        * implemented by concrete Bundle implementations using the most
        * efficient mechanism.
        */
        protected abstract void ensureResults();

        /**
        * Wait until results of bundled requests are retrieved.
        * <p>
        * Note that calls to this method must be externally synchronized.
        *
        * @param fFirst  true iff this is the first thread entering the bundle
        *
        * @return true if this thread is supposed to perform an actual bundled
        *         operation (burst); false otherwise
        */
        protected boolean waitForResults(boolean fFirst)
            {
            m_cThreads++;
            try
                {
                if (fFirst)
                    {
                    m_ldtStart = System.currentTimeMillis();
                    }

                if (getBundleSize() < getSizeThreshold())
                    {
                    if (fFirst)
                        {
                        long lDelay = getDelayMillis();
                        do
                            {
                            Blocking.wait(this, lDelay);

                            // if someone has already "submitted" the bundle
                            // need to keep waiting
                            lDelay = 0l;
                            }
                        while (isPending());
                        }
                    else
                        {
                        while (true)
                            {
                            Blocking.wait(this);

                            if (isProcessed())
                                {
                                return false;
                                }
                            // spurious wake-up; continue waiting
                            }
                        }
                    }

                if (isProcessed())
                    {
                    return false;
                    }

                // this bundle should be closed and processed right away
                setStatus(STATUS_PENDING);

                // update stats
                m_cTotalSize += getBundleSize();
                long cTotal = ++m_cTotalBundles;
                if (cTotal > 1000 // allow the "hotspot" to kick in
                 && cTotal % ADJUSTMENT_FREQUENCY == 0 && isMaster())
                    {
                    // attempt to adjust for every 1000 iterations of the master
                    // bundle
                    adjust();
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                setStatus(STATUS_EXCEPTION);
                }
            catch (RuntimeException e)
                {
                // should never happen
                --m_cThreads;
                throw e;
                }
            catch (Error e)
                {
                // should never happen
                --m_cThreads;
                throw e;
                }
            return true;
            }

        /**
        * Obtain results of the bundled requests or ensure that the results
        * have already been retrieved.
        *
        * @param fBurst  specifies whether or not the actual results have to be
        *                fetched on this thread; this parameter will be true
        *                for one and only one thread per bundle
        *
        * @return true if the bundling has succeeded; false if the un-bundling
        *         has to be performed as a result of a failure
        */
        protected boolean ensureResults(boolean fBurst)
            {
            if (isException())
                {
                return false;
                }

            if (fBurst)
                {
                // bundle is closed and ready for the actual execution (burst);
                // it must be performed without holding any synchronization
                try
                    {
                    long ldtStart = System.currentTimeMillis();

                    ensureResults();

                    long cElapsedMillis = System.currentTimeMillis() - ldtStart;
                    if (cElapsedMillis > 0)
                        {
                        m_cTotalBurstDuration += cElapsedMillis;
                        }

                    setStatus(STATUS_PROCESSED);
                    }
                catch (Throwable e)
                    {
                    setStatus(STATUS_EXCEPTION);
                    return false;
                    }
                }
            else
                {
                azzert(isProcessed());
                }
            return true;
            }

        /**
        * Release all bundle resources associated with the current thread.
        *
        * @return true iff all entered threads have released
        */
        protected synchronized boolean releaseThread()
            {
            azzert(isProcessed() && m_cThreads > 0);

            if (--m_cThreads == 0)
                {
                setStatus(STATUS_OPEN);
                return true;
                }

            return false;
            }

        // ----- statistics and debugging  -----------------------------------

        /**
        * Reset statistics for this Bundle.
        */
        public void resetStatistics()
            {
            m_cTotalBundles       = 0l;
            m_cTotalSize          = 0l;
            m_cTotalBurstDuration = 0l;
            m_cTotalWaitDuration  = 0l;
            }

        /**
        * Provide a human readable description for the Bundle object
        * (for debugging).
        *
        * @return a human readable description for the Bundle object
        */
        public String toString()
            {
            return "Bundle@" + hashCode() + "{" + formatStatusName(m_iStatus)
                 + ", size=" + getBundleSize() + '}';
            }

        /**
        * Return a human readable name for the specified status value.
        *
        * @param iStatus  the status value to format
        *
        * @return a human readable status name
        */
        protected String formatStatusName(int iStatus)
            {
            switch (iStatus)
                {
                case STATUS_OPEN:
                    return "STATUS_OPEN";
                case STATUS_PENDING:
                    return "STATUS_PENDING";
                case STATUS_PROCESSED:
                    return "STATUS_PROCESSED";
                case STATUS_EXCEPTION:
                    return "STATUS_EXCEPTION";
                default:
                    return "unknown";
                }
            }

        // ----- data fields and constants ---------------------------------

        /**
        * This Bundle accepting additional items.
        */
        public static final int STATUS_OPEN      = 0;

        /**
        * This Bundle is closed for accepting additional items and awaiting
        * for the execution results.
        */
        public static final int STATUS_PENDING   = 1;

        /**
        * This Bundle is in process of returning the result of execution
        * back to the client.
        */
        public static final int STATUS_PROCESSED = 2;

        /**
        * Attempt to bundle encountered and exception; the execution has to be
        * de-optimized and performed by individual threads.
        */
        public static final int STATUS_EXCEPTION = 3;

        /**
        * This Bundle status.
        */
        private volatile int m_iStatus = STATUS_OPEN;

        /**
        * A count of threads that are using this Bundle.
        */
        private int m_cThreads;

        /**
        * A flag that differentiates the "master" bundle which is responsible
        * for all auto-adjustments. It's set to "true" for one and only one
        * Bundle object.
        */
        private boolean m_fMaster;

        // stat fields intentionally have the "package private" access to
        // prevent generation of synthetic access methods

        /**
        * Statistics: the total number of times this Bundle has been used for
        * bundled request processing.
        */
        volatile long m_cTotalBundles;

        /**
        * Statistics: the total size of individual requests processed by this
        * Bundle expressed in the same units as values returned by the
        * {@link Bundle#getBundleSize()} method.
        */
        volatile long m_cTotalSize;

        /**
        * Statistics: a timestamp of the first thread entering the bundle.
        */
        private long m_ldtStart;

        /**
        * Statistics: a total time duration this Bundle has spent in bundled
        * request processing (burst).
        */
        volatile long m_cTotalBurstDuration;

        /**
        * Statistics: a total time duration this Bundle has spent waiting for
        * bundle to be ready for processing.
        */
        volatile long m_cTotalWaitDuration;
        }


    /**
    * Statistics class contains the latest bundler statistics.
    */
    protected static class Statistics
        {
        /**
        * Reset the statistics.
        */
        protected void reset()
            {
            m_cBundleCountSnapshot   = 0l;
            m_cBundleSizeSnapshot    = 0l;
            m_cBurstDurationSnapshot = 0l;
            m_cThreadWaitSnapshot    = 0l;
            }

        /**
        * Provide a human readable description for the Statistics object.
        * (for debugging).
        *
        * @return a human readable description for the Statistics object
        */
        public String toString()
            {
            return  "(AverageBundleSize="    + m_cAverageBundleSize
                 + ", AverageBurstDuration=" + m_cAverageBurstDuration + "ms"
                 + ", AverageWaitDuration="  + m_cAverageThreadWaitDuration + "ms"
                 + ", AverageThroughput="    + m_nAverageThroughput + "/sec"
                 + ")";
            }

        // ----- running averages ------------------------------------------

        /**
        * An average time for bundled request processing (burst).
        */
        protected int m_cAverageBurstDuration;

        /**
        * An average bundle size for this Bundler.
        */
        protected int m_cAverageBundleSize;

        /**
        * An average thread waiting time caused by under-filled bundle. The
        * wait time includes the time spend in the bundled request processing.
        */
        protected int m_cAverageThreadWaitDuration;

        /**
        * An average bundled request throughput in size units per millisecond
        * (total bundle size over total processing time)
        */
        protected int m_nAverageThroughput;

        // ----- snapshots --------------------------------------------------

        /**
        * Snapshot for a total number of processed bundled.
        */
        protected long m_cBundleCountSnapshot;

        /**
        * Snapshot for a total size of processed bundled.
        */
        protected long m_cBundleSizeSnapshot;

        /**
        * Snapshot for a burst duration.
        */
        protected long m_cBurstDurationSnapshot;

        /**
        * Snapshot for a combined thread waiting time.
        */
        protected long m_cThreadWaitSnapshot;
        }


    // ----- constants and data fields ---------------------------------------

    /**
    * Frequency of the adjustment attempts. This number represents a number of
    * iterations of the master bundle usage after which an adjustment attempt
    * will be performed.
    */
    public static int ADJUSTMENT_FREQUENCY = 128;

    /**
    * The bundle size threshold. We use double for this value to allow for
    * fine-tuning of the auto-adjust algorithm.
    *
    * @see #adjust()
    */
    private double m_dSizeThreshold;

    /**
    * The previous bundle size threshold value.
    */
    protected double m_dPreviousSizeThreshold;

    /**
    * The minimum number of threads that should trigger the bundler to switch
    * from a pass through mode to a bundled mode.
    */
    private int m_cThreadThreshold;

    /**
    * Specifies whether or not auto-adjustment is on. Default value is "true".
    */
    private boolean m_fAllowAuto = true;

    /**
    * The delay timeout in milliseconds. Default value is one millisecond.
    */
    private long m_lDelayMillis = 1l;

    /**
    * A pool of Bundle objects. Note that this list never shrinks.
    */
    protected List m_listBundle = new ArrayList();

    /**
    * Last active (open) bundle position.
    */
    private volatile int m_iActiveBundle;

    /**
    * A counter for the total number of threads that have started any bundle
    * related execution. This counter is used by subclasses to reduce an impact
    * of bundled execution for lightly loaded environments.
    */
    protected AtomicInteger m_countThreads = new AtomicInteger();

    /**
    * An instance of the Statistics object containing the latest statistics.
    */
    private Statistics m_stats = new Statistics();
    }