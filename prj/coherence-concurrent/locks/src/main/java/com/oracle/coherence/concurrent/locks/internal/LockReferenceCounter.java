/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal;

import com.tangosol.util.LongArray;
import com.tangosol.util.LongArray.Iterator;
import com.tangosol.util.SparseArray;

import java.io.Serializable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * A data structure that holds state related to read and write locks.
 *
 * @author hr  2021.06.15
 */
public class LockReferenceCounter
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default no-arg constructor.
     */
    public LockReferenceCounter()
        {
        }

    // ----- public api -----------------------------------------------------

    /**
     * Attempt to acquire a read lock for the given member and lock holder,
     * returning true if it was successfully acquired.
     *
     * @param nMemberId    the member id that requests the read lock
     * @param lLockHolder  the lock holder id; generally thread id
     *
     * @return true if the read lock was successfully acquired
     */
    public boolean tryReadLock(int nMemberId, long lLockHolder)
        {
        if (hasWriters())
            {
            return false;
            }

        long       lTicket = f_atomicTicketor.incrementAndGet();
        LockHolder holder  = new LockHolder(nMemberId, lLockHolder, /*fWrite*/ false);

        f_laHolders.set(lTicket, holder);

        f_atomicReaders.incrementAndGet();

        return true;
        }

    /**
     * Unlock the read lock that was successfully acquired for the given member
     * and lock holder id. Return true if there are no more readers.
     *
     * @param nMemberId    the member id that request the read unlock
     * @param lLockHolder  the lock holder id; generally thread id
     *
     * @return true if there are no more readers for this lock
     */
    public boolean readUnlock(int nMemberId, long lLockHolder)
        {
        LockHolder holder = new LockHolder(nMemberId, lLockHolder, /*fWrite*/ false);

        // TODO: for now we'll do a simple head first walk, however we really
        //       should pass the ticket back to the client during the lock and
        //       therefore expect it on the unlock

        long lTicket = f_laHolders.indexOf(holder);
        if (lTicket == LongArray.NOT_FOUND)
            {
            throw new IllegalMonitorStateException("Read lock was not acquired by " + holder);
            }

        if (f_laHolders.remove(lTicket) != null)
            {
            return f_atomicReaders.decrementAndGet() == 0 && f_laHolders.isEmpty();
            }
        return false;
        }

    /**
     * Attempt to acquire a write lock for the given member and lock holder,
     * returning true if it was successfully acquired.
     * <p>
     * If the caller passed {@code true} for {@code fWait}, the request to lock
     * will be tracked and therefore subsequent read attempts will be rejected.
     *
     * @param nMemberId    the member id that requests the read lock
     * @param lLockHolder  the lock holder id; generally thread id
     *
     * @return true if the read lock was successfully acquired
     */
    public boolean tryWriteLock(int nMemberId, long lLockHolder, boolean fWait)
        {
        LockHolder holder = new LockHolder(nMemberId, lLockHolder, /*fWrite*/ true);

        boolean fAcquired = !hasReaders() &&
                (f_laHolders.isEmpty() || f_laHolders.get(f_laHolders.getFirstIndex()).equals(holder));

        if (fAcquired || !fWait)
            {
            return fAcquired;
            }
        // biased towards writers by holding a reference to the writer which
        // will disallow read locks

        long lTicket = f_atomicTicketor.incrementAndGet();

        f_laHolders.set(lTicket, holder);

        return false;
        }

    /**
     * Unlock the write lock that was successfully acquired for the given member
     * and lock holder id. Return true if there are no more writers.
     *
     * @param nMemberId    the member id that request the read unlock
     * @param lLockHolder  the lock holder id; generally thread id
     *
     * @return true if there are no more readers for this lock
     */
    public boolean writeUnlock(int nMemberId, long lLockHolder)
        {
        LockHolder holder = new LockHolder(nMemberId, lLockHolder, /*fWrite*/ true);

        long       lTicket    = f_laHolders.getFirstIndex();
        LockHolder holderHead = f_laHolders.get(lTicket);

        if (!holder.equals(holderHead))
            {
            throw new IllegalMonitorStateException("Write lock was not acquired by " + holder);
            }

        f_laHolders.remove(lTicket);

        return f_laHolders.isEmpty();
        }

    /**
     * Called when the given member was removed from the service.
     * <p>
     * Return true if this data structure was modified.
     *
     * @param nMemberId  the member that left the service
     *
     * @return true if this data structure was modified
     */
    public boolean onMemberLeft(int nMemberId)
        {
        return checkLockHolders(nHolderId -> nHolderId == nMemberId);
        }

    /**
     * Ensures lock holders are for valid member ids only (determined by the
     * provided predicate). Lock holders with invalid member ids (no longer
     * in the service) are removed.
     * <p>
     * Return true if this data structure was modified.
     *
     * @param condition  a condition
     *
     * @return true if this data structure was modified
     */
    public boolean checkLockHolders(Predicate<Integer> condition)
        {
        boolean fModified = false;
        for (Iterator<LockHolder> iter = f_laHolders.iterator(); iter.hasNext(); )
            {
            LockHolder holder = iter.next();

            if (condition.test(holder.getMemberId()))
                {
                if (holder.isReadHolder())
                    {
                    f_atomicReaders.decrementAndGet();
                    }

                iter.remove();
                fModified = true;
                }
            }
        return fModified;
        }

    /**
     * Return true if the current lock holder is a write lock.
     *
     * @return true if the current lock holder is a write lock
     */
    public boolean isWriteLocked()
        {
        return isWriteLocked(holder -> true);
        }

    /**
     * Return true if the current lock holder is a write lock and the given
     * predicate passes.
     *
     * @param condition  a predicate based on the current lock holder
     *
     * @return true if the current lock holder is a write lock and the given
     *         predicate passes
     */
    public boolean isWriteLocked(Predicate<LockHolder> condition)
        {
        if (f_laHolders.isEmpty())
            {
            return false;
            }
        LockHolder holder = f_laHolders.get(f_laHolders.getFirstIndex());

        return holder.isWriteHolder() && condition.test(holder);
        }

    /**
     * Return true if there are read lock holders.
     *
     * @return true if there are read lock holders
     */
    public boolean isReadLocked()
        {
        return hasReaders();
        }

    /**
     * Return true if the lock is empty; no read or write locks
     *
     * @return true if the lock is empty; no read or write locks
     */
    public boolean isEmpty()
        {
        return f_laHolders.isEmpty();
        }

    /**
     * Return a string representation of the state of this lock.
     *
     * @return a string representation of the state of this lock
     */
    public String getDescription()
        {
        int cReaders = f_atomicReaders.get();
        int cWriters = countWriters();

        return cReaders > 0
                ? "READ(" + cReaders + ')' +
                    (cWriters > 0 ? " | PENDING_WRITE(" + cWriters + ')' : "")
                : cWriters > 0 ? "WRITE(" + cWriters + ')' : "";
        }

    // ----- object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "LockReferenceCounter{" + getDescription() + '}';
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the number of pending writers.
     *
     * @return the number of pending writers
     */
    protected int countWriters()
        {
        int cWriters = 0;
        for (Iterator<LockHolder> iter = f_laHolders.iterator(); iter.hasNext(); )
            {
            if (iter.next().isWriteHolder())
                {
                cWriters++;
                }
            }
        return cWriters;
        }

    /**
     * Return true if there is a pending write lock.
     *
     * @return true if there is a pending write lock
     */
    protected boolean hasWriters()
        {
        for (Iterator<LockHolder> iter = f_laHolders.iterator(); iter.hasNext(); )
            {
            if (iter.next().isWriteHolder())
                {
                return true;
                }
            }
        return false;
        }

    /**
     * Return true if there are acquired read locks.
     *
     * @return true if there are acquired read locks
     */
    protected boolean hasReaders()
        {
        return f_atomicReaders.get() > 0;
        }

    // ----- inner class: LockHolder ----------------------------------------

    /**
     * A class that represents the lock holder
     */
    public class LockHolder
            implements Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default no-arg constructor.
         */
        public LockHolder()
            {
            }

        /**
         * Create a lock holder based on the provided member id, lock holder
         * and whether it is a write lock.
         *
         * @param nMemberId    the member id
         * @param lLockHolder  the lock holder id; general thread id
         * @param fWrite       true if this is a write lock; false for read lock
         */
        public LockHolder(int nMemberId, long lLockHolder, boolean fWrite)
            {
            m_nMemberId   = nMemberId;
            m_lLockHolder = lLockHolder;
            m_fWrite      = fWrite;
            }

        // ----- public api -------------------------------------------------

        /**
         * Return the member id.
         *
         * @return the member id
         */
        public int getMemberId()
            {
            return m_nMemberId;
            }

        /**
         * Return the lock holder.
         *
         * @return the lock holder
         */
        public long getLockHolder()
            {
            return m_lLockHolder;
            }

        /**
         * Return true if this lock holder represents a read lock.
         *
         * @return true if this lock holder represents a read lock
         */
        public boolean isReadHolder()
            {
            return !isWriteHolder();
            }

        /**
         * Return true if this lock holder represents a write lock.
         *
         * @return true if this lock holder represents a write lock
         */
        public boolean isWriteHolder()
            {
            return m_fWrite;
            }

        // ----- object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }

            if (o == null || getClass() != o.getClass())
                {
                return false;
                }

            LockHolder that = (LockHolder) o;
            return m_nMemberId == that.m_nMemberId &&
                    m_lLockHolder == that.m_lLockHolder;
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_nMemberId, m_lLockHolder);
            }

        @Override
        public String toString()
            {
            return "LockHolder{" +
                    "member=" + m_nMemberId +
                    ", threadId=" + m_lLockHolder +
                    '}';
            }

        // ----- data members -----------------------------------------------

        /**
         * The member id.
         */
        protected int m_nMemberId;

        /**
         * The lock holder.
         */
        protected long m_lLockHolder;

        /**
         * Whether this is write or read lock.
         */
        protected boolean m_fWrite;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Monotonically increasing long given to each request for lock.
     */
    protected AtomicLong f_atomicTicketor = new AtomicLong();

    /**
     * A reference of ticket to LockHolder.
     */
    protected LongArray<LockHolder> f_laHolders = new SparseArray<>();

    /**
     * A count of read locks.
     */
    protected AtomicInteger f_atomicReaders = new AtomicInteger();
    }
