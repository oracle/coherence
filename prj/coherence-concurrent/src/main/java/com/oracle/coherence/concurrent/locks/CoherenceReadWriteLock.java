/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.oracle.coherence.concurrent.locks.internal.LockReferenceCounter;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.ServiceInfo;
import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.CopyOnWriteLongArray;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MultiplexingMapListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;

/**
 * A distributed {@link ReadWriteLock} implementation that uses the {@link NamedCache}
 * it is initialized with to acquire and release read or write locks and ensure
 * mutual exclusivity.
 * <p>
 * {@link ReadWriteLock}'s provide benefit when running on multi-processor cpu
 * architectures avoiding mutual exclusions when shared state needs to be modified,
 * ultimately reducing the cost for 'readers'. As this is a distributed implementation
 * the cost of acquiring the first read/write lock will involve communication over
 * the network and therefore is far from cheap. However, there are valid reasons to
 * want distributed mutual exclusion (a Coherence basked {@link Lock} implementation)
 * regardless of the cost. Generally this is due to the resource cost of performing
 * some operation is high enough that many processes (or threads) should not perform
 * said task in parallel, or the operation itself is not side effect free and
 * can not be undone thus must only be attempted once or at least serially.
 * <p>
 * The same general semantics hold such that many readers may acquire a read lock
 * until the point a write lock is requested. Once a write lock is requested
 * this implementation will wait for the readers to drain and the requested write
 * lock to be promoted from pending to acquired. Any read locks attempted once
 * a write lock is requested will be denied (blocked until they reach their timeout),
 * and additional write locks will be queued. Once the write lock is acquired
 * and subsequently released, there is a race for the next lock acquisition.
 * Currently, there is no bias towards writers over readers but instead the bias
 * is towards the first to (re-)request.
 *
 * @author hr  2021.06.15
 */
public class CoherenceReadWriteLock
        implements ReadWriteLock
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CoherenceReadWriteLock with the given name and {@link NamedCache cache}.
     *
     * @param sName  the name of this lock; used as the key for an entry in the
     *               provided map
     * @param map    the map to store associated state
     */
    public CoherenceReadWriteLock(String sName, NamedMap<String, LockReferenceCounter> map)
        {
        f_sName = sName;
        f_map   = map;
        }

    // ----- ReadWriteLock api ----------------------------------------------

    @Override
    public Lock readLock()
        {
        return f_lockRead;
        }

    @Override
    public Lock writeLock()
        {
        return f_lockWrite;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Return the name of this lock.
     * <p>
     * This name is used as the key for the entry this distributed read write
     * lock stores its state in.
     *
     * @return the name of this lock
     */
    public String getName()
        {
        return f_sName;
        }

    /**
     * Return the map the associated lock state will be held in.
     *
     * @return the map the associated lock state will be held in
     */
    public NamedMap<String, LockReferenceCounter> getMap()
        {
        return f_map;
        }

    /**
     * A convenience method that allows a caller to influence the lock holder.
     *
     * @param lLockHolder  the lock holder
     */
    public void withThreadId(long lLockHolder)
        {
        f_tloThreadId.set(lLockHolder);
        }

    /**
     * Reset the lock holder to use for un/locking.
     */
    public void resetThreadId()
        {
        f_tloThreadId.remove();
        }

    // ----- helper methods -------------------------------------------------
    /**
     * Block the current thread until reads are likely to go through.
     */
    protected void waitForRead()
        {
        doWait(LockReferenceCounter::isWriteLocked);
        }

    /**
     * Block the current thread until writes are likely to go through.
     */
    protected void waitForWrite()
        {
        int  nMemberId   = f_map.getService().getCluster().getLocalMember().getId();
        long lLockHolder = f_tloThreadId.get();

        doWait(lockReferenceCounter -> lockReferenceCounter.isReadLocked() || lockReferenceCounter.isWriteLocked(holder ->
                holder.getMemberId()   != nMemberId &&
                holder.getLockHolder() != lLockHolder));
        }

    /**
     * Block the current thread if the provided {@link Predicate} passes.
     *
     * @param condition  the condition that must pass if this thread is to block
     */
    protected void doWait(Predicate<LockReferenceCounter> condition)
        {
        ensureRegistered();

        while (m_lockRefCounter != null && condition.test(m_lockRefCounter))
            {
            synchronized (f_localRefMonitor)
                {
                try
                    {
                    f_localRefMonitor.wait();
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        }

    /**
     * Ensure this CRW has a MapListener registered.
     */
    protected void ensureRegistered()
        {
        if (!m_fRegistered)
            {
            synchronized (this)
                {
                if (m_fRegistered)
                    {
                    return;
                    }

                f_map.addMapListener(new LockStateListener(), f_sName, false);
                m_fRegistered = true;
                }
            }
        }

    // ----- inner class: LockStateListener ---------------------------------

    /**
     * A listener that holds a local up to date capy of the {@link LockReferenceCounter}.
     * This allows the client to make certain decisions without having to go
     * to the storage node.
     */
    protected class LockStateListener
            extends MultiplexingMapListener
            implements MapListenerSupport.PrimingListener
        {
        // ----- MultiplexingMapListener methods ----------------------------

        @Override
        protected void onMapEvent(MapEvent evt)
            {
            LockReferenceCounter lockRefCounter = (LockReferenceCounter) evt.getNewValue();

            synchronized (f_localRefMonitor)
                {
                m_lockRefCounter = lockRefCounter;

                f_localRefMonitor.notify();
                }
            }
        }

    // ----- inner class: ReadLock ------------------------------------------

    /**
     * A {@link Lock} implementation used for readers.
     */
    public class ReadLock
            implements Lock
        {
        // ----- Lock methods -----------------------------------------------

        @Override
        public void lock()
            {
            int  nMemberId   = f_map.getService().getCluster().getLocalMember().getId();
            long lLockHolder = f_tloThreadId.get();

            AtomicInteger atomicReaders = f_laReaders.get(lLockHolder);
            if (atomicReaders != null && atomicReaders.get() > 0)
                {
                // re-entrant lock
                atomicReaders.incrementAndGet();
                return;
                }

            while (true)
                {
                boolean fLocked = f_map.invoke(f_sName, entry ->
                    {
                    LockReferenceCounter counter = entry.getValue();
                    if (counter == null)
                        {
                        counter = new LockReferenceCounter();
                        }

                    if (counter.tryReadLock(nMemberId, lLockHolder))
                        {
                        entry.setValue(counter);
                        return true;
                        }
                    return false;
                    });

                if (fLocked)
                    {
                    atomicReaders = f_laReaders.get(lLockHolder);
                    if (atomicReaders == null)
                        {
                        f_laReaders.set(lLockHolder, atomicReaders = new AtomicInteger());
                        }
                    atomicReaders.incrementAndGet();
                    return;
                    }
                else
                    {
                    // we could not acquire a read lock therefore wait until the
                    // the writer releases the lock
                    waitForRead();
                    }
                }
            }


        @Override
        public void lockInterruptibly()
                throws InterruptedException
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean tryLock()
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void unlock()
            {
            int  nMemberId   = f_map.getService().getCluster().getLocalMember().getId();
            long lLockHolder = f_tloThreadId.get();

            assert f_laReaders.exists(lLockHolder);

            if (f_laReaders.get(lLockHolder).decrementAndGet() != 0)
                {
                return;
                }

            f_map.invoke(f_sName, entry ->
                {
                if (!entry.isPresent())
                    {
                    throw new IllegalMonitorStateException("Lock was not acquired for " + ((BinaryEntry) entry).getBackingMapContext().getCacheName());
                    }

                LockReferenceCounter counter = entry.getValue();
                if (counter.readUnlock(nMemberId, lLockHolder))
                    {
                    entry.remove(false);
                    }
                else
                    {
                    entry.setValue(counter);
                    }
                return true;
                });
            }

        @Override
        public Condition newCondition()
            {
            throw new UnsupportedOperationException();
            }

        // ----- data members -----------------------------------------------

        /**
         * A {@link LongArray} of readers.
         */
        protected final LongArray<AtomicInteger> f_laReaders = new CopyOnWriteLongArray<>();
        }

    // ----- inner class: WriteLock ---------------------------------------------------

    /**
     * A {@link Lock} implementation for writers.
     */
    public class WriteLock
            implements Lock
        {
        // ----- Lock methods -----------------------------------------------

        @Override
        public void lock()
            {
            int  nMemberId   = f_map.getService().getCluster().getLocalMember().getId();
            long lLockHolder = f_tloThreadId.get();

            if (lLockHolder == m_lLockHolder)
                {
                // reentrant
                f_atomicWriters.incrementAndGet();
                return;
                }

            while (true)
                {
                boolean fLocked = f_map.invoke(f_sName, entry ->
                    {
                    LockReferenceCounter counter = entry.getValue();
                    if (counter == null)
                        {
                        counter = new LockReferenceCounter();
                        }

                    try
                        {
                        return counter.tryWriteLock(nMemberId, lLockHolder, /*fWait*/ true);
                        }
                    finally
                        {
                        entry.setValue(counter);
                        }
                    });

                if (fLocked)
                    {
                    m_lLockHolder = lLockHolder;
                    f_atomicWriters.incrementAndGet();
                    return;
                    }
                else
                    {
                    // we could not acquire a read lock therefore wait until the
                    // writer releases the lock
                    waitForWrite();
                    }
                }
            }

        @Override
        public void lockInterruptibly()
                throws InterruptedException
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean tryLock()
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void unlock()
            {
            int  nMemberId   = f_map.getService().getCluster().getLocalMember().getId();
            long lLockHolder = f_tloThreadId.get();

            assert lLockHolder == m_lLockHolder;

            if (f_atomicWriters.decrementAndGet() != 0)
                {
                return;
                }

            f_map.invoke(f_sName, entry ->
                {
                if (!entry.isPresent())
                    {
                    throw new IllegalMonitorStateException("Lock was not acquired for " + ((BinaryEntry) entry).getBackingMapContext().getCacheName());
                    }

                LockReferenceCounter counter = entry.getValue();
                if (counter.writeUnlock(nMemberId, lLockHolder))
                    {
                    entry.remove(false);
                    }
                else
                    {
                    entry.setValue(counter);
                    }
                return true;
                });

            m_lLockHolder = -1L;
            }

        @Override
        public Condition newCondition()
            {
            throw new UnsupportedOperationException();
            }

        // ----- data members -----------------------------------------------

        /**
         * A count of successful re-entrant lock acquirers.
         */
        protected final AtomicInteger f_atomicWriters = new AtomicInteger();

        /**
         * The lock holder's 'id' or -1L;
         */
        protected volatile long m_lLockHolder = -1L;
        }

    // ----- inner class: RemoveHoldersProcessor ----------------------------

    /**
     * An EntryProcessor that will remove locks for the provided member id. If
     * the member id is {@code -1L}, this will instruct the processor to check
     * all LockHolders to ensure the associated member id is still valid.
     */
    public static class RemoveHoldersProcessor
            implements InvocableMap.EntryProcessor<String, LockReferenceCounter, Void>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default no-arg constructor.
         */
        public RemoveHoldersProcessor()
            {
            }

        /**
         * Create a RemoveHoldersProcessor with the given member id.
         *
         * @param nMemberId  remove all LockHolders that have this member id
         *                   or -1L to ensure all LockHolders have valid members
         */
        public RemoveHoldersProcessor(int nMemberId)
            {
            m_nMemberId = nMemberId;
            }

        @Override
        public Void process(InvocableMap.Entry<String, LockReferenceCounter> entry)
            {
            LockReferenceCounter counter = entry.getValue();

            ServiceInfo info = ((BinaryEntry) entry).getContext().getCacheService().getInfo();

            Predicate<Integer> condition = nHolderId -> info.getServiceMember(nHolderId) == null;

            if (m_nMemberId > 0 ? counter.onMemberLeft(m_nMemberId) : counter.checkLockHolders(condition))
                {
                if (counter.isEmpty())
                    {
                    entry.remove(false);
                    }
                else
                    {
                    entry.setValue(counter);
                    }
                }

            return null;
            }

        // ----- data members -----------------------------------------------

        /**
         * The member id to remove all LockHolders that have this member id
         * or -1L to ensure all LockHolders have valid members
         */
        protected int m_nMemberId;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of this lock. Used as a key in the provided map.
     */
    protected final String f_sName;

    /**
     * The Map to store associated lock state in.
     */
    protected final NamedMap<String, LockReferenceCounter> f_map;

    /**
     * A Lock for readers.
     */
    protected final ReadLock f_lockRead = new ReadLock();

    /**
     * A Lock for writers.
     */
    protected final WriteLock f_lockWrite = new WriteLock();

    /**
     * A semaphore for waiting readers and/or writers and the local lock state
     * updated.
     */
    protected final Object f_localRefMonitor = new Object();

    /**
     * The thread id to use for the lock holder; defaults to current thread's id.
     */
    protected final ThreadLocal<Long> f_tloThreadId = ThreadLocal.withInitial(() -> Thread.currentThread().getId());

    /**
     * Whether the MapListener was registered.
     */
    protected volatile boolean m_fRegistered;

    /**
     * A local copy of the lock state.
     */
    protected volatile LockReferenceCounter m_lockRefCounter;
    }

