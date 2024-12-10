/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.CopyOnWriteLongArray;
import com.tangosol.util.LongArray;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.util.LongArray.Iterator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * StorageVersion is used to ensure that a submitted modification to the
 * backing map is committed to all dependent data structures; such as an index.
 * When a modification is made the mutator calls{@link #submit(int)} which
 * increments the submitted version to indicate there is a change that needs
 * to be committed.
 * When the associated data structures has been updated the version is committed
 * by calling {@link #commit(int)}.
 * <p>
 * A storage is considered stable when the committed and submitted versions
 * are equal. A call to {@link #getModifiedPartitions(long, PartitionSet)} can be
 * used to check  if there are pending submissions since the last {@link #commit}
 * call for the specified partitions.
 *
 * @author coh 2011.02.08
 * @since  Coherence 3.7
 */
public class StorageVersion
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new StorageVersion representing one PartitionCache$Storage instance.
     */
    public StorageVersion()
        {
        m_laPartitionVersion     = new CopyOnWriteLongArray<>();
        m_atomicCommittedVersion = new AtomicLong(0L);
        m_atomicSubmittedVersion = new AtomicLong(0L);
        m_cWaitingThreads = 0;
        }

    // ----- methods ---------------------------------------------------------

    /**
     * Get the committed version.
     *
     * @return the committed version
     */
    public long getCommittedVersion()
        {
        return m_atomicCommittedVersion.get();
        }

    /**
     * Provide a human readable description for this StorageVersion.
     *
     * @return a human readable description for this StorageVersion
     */
    @Override
    public String toString()
        {
        StringBuilder sbMap = new StringBuilder();

        for (Iterator iter = m_laPartitionVersion.iterator(); iter.hasNext(); )
            {
            PartitionVersion version = (PartitionVersion) iter.next();

            if (version.f_atomicSubmission.get() > 0)
                {
                sbMap.append("\n{Partition ")
                   .append(iter.getIndex())
                   .append(' ')
                   .append(version)
                   .append('}');
                }
            }

        return "StorageVersion{SubmittedVersion=" + m_atomicSubmittedVersion.get() + ", CommittedVersion="
                + m_atomicCommittedVersion.get() + ", WaitingThreads=" + m_cWaitingThreads
                + ", PartitionVersions={" + sbMap + "\n}}";
        }

    /**
     * Get the submitted version.
     *
     * @return the submitted version
     */
    public long getSubmittedVersion()
        {
        return m_atomicSubmittedVersion.get();
        }

    /**
     * Get the submitted version.
     *
     * @return the submitted version
     */
    public long getSubmittedVersion(int nPart)
        {
        PartitionVersion version = m_laPartitionVersion.get(nPart);
        return version == null ? 0 : version.f_atomicSubmitted.get();
        }

    /**
     * Check if the specified partition has submitted any modifications since
     * <code>lCommittedVersion</code>.
     *
     * @param lCommittedVersion  the committed version to use as a threshold
     * @param nPart              a partition to test
     *
     * @return true if the partition has been modified or is in the process of being modified
     */
    public boolean isPartitionModified(long lCommittedVersion, int nPart)
        {
        PartitionVersion version = (PartitionVersion) m_laPartitionVersion.get(nPart);

        return version != null && version.isNewerThan(lCommittedVersion);
        }

    /**
     * Return the partitions what have submitted any modifications since
     * <code>lCommittedVersion</code>.
     *
     * @param lCommittedVersion  the committed version to use as a threshold
     * @param partsCheck         a {@link PartitionSet} that contains partitions to test
     *
     * @return the partitions that have been modified or are in the process of being modified
     */
    public PartitionSet getModifiedPartitions(long lCommittedVersion, PartitionSet partsCheck)
        {
        LongArray    laPartitionVersion = m_laPartitionVersion;
        PartitionSet partsSuspect       = new PartitionSet(partsCheck.getPartitionCount());

        for (int nPart = partsCheck.next(0); nPart >= 0; nPart = partsCheck.next(nPart + 1))
            {
            PartitionVersion version = (PartitionVersion) laPartitionVersion.get(nPart);

            if (version != null && version.isNewerThan(lCommittedVersion))
                {
                partsSuspect.add(nPart);
                }
            }
        return partsSuspect;
        }

    /**
     * Increment the submitted version counter also increment the pending submission
     * counter for the specified partition.
     * <p>
     * This method should be called while holding synchronization on the
     * corresponding backing map (see PartitionedCache.Storage#checkIndexConsistency).
     *
     * @param nPartition  the partition to update
     */
    public long submit(int nPartition)
        {
        LongArray laPartitionVersion = m_laPartitionVersion;

        PartitionVersion version = ensurePartitionVersion(nPartition);
        version.f_atomicSubmission.incrementAndGet();

        m_atomicSubmittedVersion.incrementAndGet();

        return version.f_atomicSubmitted.incrementAndGet();
        }

    /**
     * Increment committed version and decrement the outstanding submission counter of the
     * specified partition (see PartitionedCache.ResourceCoordinator#processEvent).
     *
     * @param nPartition  the partition to update
     */
    public void commit(int nPartition)
        {
        PartitionVersion version = m_laPartitionVersion.get(nPartition);
        boolean          fNotify;

        // make sure that two updating threads don't step on each other
        synchronized (version)
            {
            long lVersion = m_atomicCommittedVersion.incrementAndGet();
            version.f_atomicCommitted.set(lVersion);

            long cPendingUpdates = version.f_atomicSubmission.decrementAndGet();
            assert cPendingUpdates >= 0;

            fNotify = cPendingUpdates == 0 && m_cWaitingThreads > 0;
            }

        if (fNotify)
            {
            // the index for the partition is now available for querying threads;
            // since we don't have the fidelity to know what threads are waiting
            // on what partitions, notify all threads now
            synchronized (this)
                {
                notifyAll();
                }
            }
        }

    /**
     * Check if there are any pending commits (i.e. SubmittedVersion &gt; CommittedVersion)
     * and if so, wait for any.
     */
    public void waitForPendingCommit()
        {
        long lSubmitted = m_atomicSubmittedVersion.get();
        long lCommitted = m_atomicCommittedVersion.get();

        if (lSubmitted == lCommitted)
            {
            // we are already in a stable state
            return;
            }

        waitForNotify();
        }

    /**
     * Wait for commit notification.
     */
    private void waitForNotify()
        {
        synchronized (this)
            {
            m_cWaitingThreads++;
            try
                {
                // while the timed wait is not strictly speaking necessary, the
                // cost of our mistake to wake up is huge (basically meaning a
                // service termination), and the cost of doing an extra iteration
                // is negligible - let's play it safe
                Blocking.wait(this, 50);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            finally
                {
                m_cWaitingThreads--;
                }
            }
        }

    /**
     * Wait for all pending submission to be prcessed and return the
     * current committed version.
     * <p>
     * This method is simply an optimization that allows a caller to avoid
     * retrieving a submit counter while there is known to be a submission
     * that hasn't yet had a correposnding commit.
     * However, since another submission cna come at any time after this
     * method returns, a caller is supposed to re-check again to guarantee
     * the correctness of the data covered by this StorageVersion.
     *
     * @param nPartition  the partition to wait for
     *
     * @return the current committed version
     */
    public long waitForPendingCommit(int nPartition)
        {
        PartitionVersion version = m_laPartitionVersion.get(nPartition);
        if (version == null)
            {
            return 0;
            }

        while (version.f_atomicSubmission.get() > 0)
            {
            waitForNotify();
            }

        return version.f_atomicCommitted.get();
        }

    /**
     * Drop the specified partition from the partition version map.
     * <p>
     * This method should only be called when a partition is no longer
     * owned by the corresponding storage.
     *
     * @param nPartition  the partition to drop from the partition version map
     */
    public void dropCommittedVersion(int nPartition)
        {
        m_laPartitionVersion.remove(nPartition);
        }

    /**
     * Reset the submitted version to be the provided version iff that version
     * is greater than the current submitted version.
     *
     * @param nPartition   the partition
     * @param lNewVersion  the new version
     */
    public void resetSubmitted(int nPartition, long lNewVersion)
        {
        LongArray laPartitionVersion = m_laPartitionVersion;

        PartitionVersion version = ensurePartitionVersion(nPartition);

        long lCurrentVersion;
        do
            {
            lCurrentVersion = version.f_atomicSubmitted.get();
            }
        while (lCurrentVersion < lNewVersion && !version.f_atomicSubmitted.compareAndSet(lCurrentVersion, lNewVersion));
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a PartitionVersion data structure that represents the provided
     * partition
     *
     * @param nPartition  the partition
     *
     * @return a PartitionVersion data structure that represents the provided
     *         partition
     */
    protected PartitionVersion ensurePartitionVersion(int nPartition)
        {
        LongArray laPartitionVersion = m_laPartitionVersion;

        PartitionVersion version = (PartitionVersion) laPartitionVersion.get(nPartition);
        if (version == null)
            {
            // partition not registered, allocate and add to the map
            laPartitionVersion.set(nPartition, version = new PartitionVersion());
            }

        return version;
        }

    /**
     * PartitionVersion encapsulates the partition last commit version and
     * the "pending" update counter used to determine whether or not there is
     * a thread within the "critical section" between the submit and commit.
     * This class is used as a "struct", so we claim an exception from the
     * standard coding practices.
     */
    private static class PartitionVersion
        {

        /**
         * Check whether or not any modifications have been submitted to
         * this PartitionVersion since {code}lCommittedVersion{code}.
         *
         * @param lCommittedVersion the committed version to test
         *
         * @return true if this PartitionVersion has been modified
         */
        public boolean isNewerThan(long lCommittedVersion)
          {
          return f_atomicSubmission.get() > 0                 // there is a pending change
              || f_atomicCommitted.get() > lCommittedVersion; // the partition version is newer
          }

        // ----- object methods ---------------------------------------------

        /**
         * Provide a human readable description for this PartitionVersion.
         *
         * @return a human readable description for this PartitionVersion
         */
        public String toString()
            {
            return "PartitionVersion{CommittedVersion=" + f_atomicCommitted.get()
                 + ", SubmissionCounter=" + f_atomicSubmission.get() + "}";
            }

        // ----- data members -----------------------------------------------

        /**
         * A monotonically increasing count of the changes made to a partition
         * slice.
         */
        final AtomicLong f_atomicSubmitted = new AtomicLong();

        /**
         * The last index update made to the partition.
         */
        final AtomicLong f_atomicCommitted = new AtomicLong();

        /**
         * The pending index update counter. A positive value indicates that
         * there are updates made to the backing map which have not yet been
         * processed.
         */
        final AtomicLong f_atomicSubmission = new AtomicLong();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The global submitted version counter.
     */
    protected final AtomicLong m_atomicSubmittedVersion;

    /**
     * The global committed version counter.
     */
    protected final AtomicLong m_atomicCommittedVersion;

    /**
     * The array containing PartitionVersions keyed by the partition id.
     */
    protected final LongArray<PartitionVersion> m_laPartitionVersion;

    /**
     * The number of threads that are currently waiting for pending commits.
     */
    protected volatile int m_cWaitingThreads;
    }
