/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.oracle.coherence.concurrent.locks.internal.ReadWriteLockHolder;

import com.tangosol.net.Member;
import com.tangosol.net.NamedMap;

import com.tangosol.util.MapEvent;
import com.tangosol.util.Processors;

import com.tangosol.util.listener.SimpleMapListener;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An implementation of {@link ReadWriteLock} supporting similar semantics to
 * {@link ReentrantReadWriteLock}, but with support for access synchronization
 * across multiple cluster members.
 *
 * <p>This class has the following properties:
 *
 * <ul>
 * <li><b>Acquisition order</b>
 *
 * <p>This class does not impose a reader or writer preference
 * ordering for lock access, and unlike {@link ReentrantReadWriteLock},
 * it does NOT support an optional <em>fairness</em> policy. It supports
 * only the non-fair mode, where the order of entry
 * to the read and write lock is unspecified, subject to reentrancy
 * constraints.  A non-fair lock that is continuously contended may
 * indefinitely postpone one or more reader or writer threads.
 *
 * <li><b>Reentrancy</b>
 *
 * <p>This lock allows both readers and writers to reacquire read or
 * write locks in the style of a {@link ReentrantReadWriteLock}. Non-reentrant
 * readers are not allowed until all write locks held by the writing
 * thread have been released.
 *
 * <p>Additionally, a writer can acquire the read lock, but not
 * vice-versa.  Among other applications, reentrancy can be useful
 * when write locks are held during calls or callbacks to methods that
 * perform reads under read locks.  If a reader tries to acquire the
 * write lock it will never succeed.
 *
 * <li><b>Lock downgrading</b>
 * <p>Reentrancy also allows downgrading from the write lock to a read lock,
 * by acquiring the write lock, then the read lock and then releasing the
 * write lock. However, upgrading from a read lock to the write lock is
 * <b>not</b> possible.
 *
 * <li><b>Interruption of lock acquisition</b>
 * <p>The read lock and write lock both support interruption during lock
 * acquisition.
 *
 * <li><b>{@link Condition} support</b>
 *
 * <p>Neither the read lock nor the write lock support a {@link Condition} and
 * both {@code readLock().newCondition()} and {@code writeLock().newCondition()}
 * throw {@code UnsupportedOperationException}.
 *
 * <li><b>Instrumentation</b>
 * <p>This class supports methods to determine whether locks
 * are held or contended. These methods are designed for monitoring
 * system state, not for synchronization control.
 * </ul>
 *
 * <p>Unlike {@link ReentrantReadWriteLock}, this class does not support
 * serialization.
 *
 * <p><b>Sample usages</b>. Here is a code sketch showing how to perform
 * lock downgrading after updating a cache (exception handling is
 * particularly tricky when handling multiple locks in a non-nested
 * fashion):
 *
 * <pre> {@code
 * class CachedData {
 *   Object data;
 *   boolean cacheValid;
 *   final DistributedReadWriteLock rwl = Locks.remoteReadWriteLock("myRWLock");
 *
 *   void processCachedData() {
 *     rwl.readLock().lock();
 *     if (!cacheValid) {
 *       // Must release read lock before acquiring write lock
 *       rwl.readLock().unlock();
 *       rwl.writeLock().lock();
 *       try {
 *         // Recheck state because another thread might have
 *         // acquired write lock and changed state before we did.
 *         if (!cacheValid) {
 *           data = ...
 *           cacheValid = true;
 *         }
 *         // Downgrade by acquiring read lock before releasing write lock
 *         rwl.readLock().lock();
 *       } finally {
 *         rwl.writeLock().unlock(); // Unlock write, still hold read
 *       }
 *     }
 *
 *     try {
 *       use(data);
 *     } finally {
 *       rwl.readLock().unlock();
 *     }
 *   }
 * }}</pre>
 *
 * <h2>Implementation Notes</h2>
 *
 * <p>This lock supports a maximum of 65535 recursive write locks
 * and 65535 read locks. Attempts to exceed these limits result in
 * {@link Error} throws from locking methods.
 *
 * @author Aleks Seovic  2021.10.27
 */
public class RemoteReadWriteLock
        implements ReadWriteLock
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create an instance of {@code DistributedReadWriteLock}.
     *
     * @param sName the name of the lock
     * @param locks the {@link NamedMap} that stores this lock's state
     */
    RemoteReadWriteLock(String sName, NamedMap<String, ReadWriteLockHolder> locks)
        {
        f_sync       = new Sync(sName, locks);
        f_readerLock = new ReadLock(this);
        f_writerLock = new WriteLock(this);

        locks.addMapListener(new SimpleMapListener<String, ReadWriteLockHolder>()
                                     .addInsertHandler(f_sync::onHolderChange)
                                     .addUpdateHandler(f_sync::onHolderChange),
                             sName, false);
        }

    // ---- ReadWriteLock interface -----------------------------------------

    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading
     */
    public Lock readLock()
        {
        return f_readerLock;
        }

    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing
     */
    public Lock writeLock()
        {
        return f_writerLock;
        }

    // ---- inner class: Sync -----------------------------------------------

    /**
     * Synchronization implementation for {@link RemoteReadWriteLock}.
     */
    static class Sync
            extends AbstractQueuedSynchronizer
        {
        // ---- constructor -------------------------------------------------

        /**
         * Create an instance of {@code Sync}.
         *
         * @param sName  the name of the lock
         * @param locks  the {@link NamedMap} that stores this lock's state
         */
        Sync(String sName, NamedMap<String, ReadWriteLockHolder> locks)
            {
            Member localMember = locks.getService().getCluster().getLocalMember();

            f_sName       = sName;
            f_locks       = locks;
            f_localMember = localMember;
            readHolds     = new Sync.ThreadLocalHoldCounter();

            setState(getState()); // ensures visibility of readHolds
            }

        @Override
        protected final boolean tryRelease(int releases)
            {
            final Thread thread = Thread.currentThread();
            if (releases == -1)
                {
                // we are releasing this special lock from an event dispatcher thread
                // because another member has released the lock
                if (thread == getExclusiveOwnerThread())
                    {
                    setExclusiveOwnerThread(null);
                    }
                return true;
                }

            if (thread != getExclusiveOwnerThread())
                {
                throw new IllegalMonitorStateException(thread + " != " + getExclusiveOwnerThread());
                }

            boolean fUnlocked = false;
            int     c         = getState() - releases;
            if (exclusiveCount(c) == 0)
                {
                // final release of the lock; we should release the lock on the server
                final LockOwner owner = new LockOwner(f_localMember, thread.getId());
                fUnlocked = f_locks.invoke(f_sName, entry ->
                        {
                        ReadWriteLockHolder lock = entry.getValue();
                        if (lock != null && lock.unlockWrite(owner))
                            {
                            entry.setValue(lock);
                            return true;
                            }
                        return false;
                        });
                if (fUnlocked)
                    {
                    setExclusiveOwnerThread(null);
                    }
                else
                    {
                    throw new IllegalMonitorStateException();
                    }
                }

            setState(c);
            return fUnlocked;
            }

        @Override
        protected final boolean tryAcquire(int acquires)
            {
            Thread thread = Thread.currentThread();
            if (acquires == -1)
                {
                // we are acquiring this special lock from an event dispatcher thread
                // because another member has acquired the lock already
                setExclusiveOwnerThread(thread);
                return true;
                }

            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0)
                {
                // re-entrant lock acquisition attempt; no need to make server call
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || thread != getExclusiveOwnerThread())
                    {
                    return false;
                    }
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    {
                    throw new Error("Maximum lock count exceeded");
                    }

                // success: re-entrant acquire of the exclusive lock
                setState(c + acquires);
                return true;
                }

            // no thread in this process owns the exclusive lock and there are no readers
            // try to obtain lock from the server
            if (!writerShouldBlock())
                {
                final LockOwner owner = new LockOwner(f_localMember, thread.getId());
                boolean fLocked = f_locks.invoke(f_sName, entry ->
                {
                ReadWriteLockHolder lock = entry.getValue(ReadWriteLockHolder::new);
                boolean fRes = lock.lockWrite(owner);
                entry.setValue(lock);
                return fRes;
                });

                if (fLocked && compareAndSetState(c, c + acquires))
                    {
                    setExclusiveOwnerThread(thread);
                    return true;
                    }
                }

            return false;
            }

        @Override
        protected final boolean tryReleaseShared(int releases)
            {
            Sync.HoldCounter rh = readHolds.get();
            int count = rh.count;
            if (count <= 0)
                {
                throw new IllegalMonitorStateException("attempt to unlock read lock, not locked by current thread");
                }
            if (count <= 1)
                {
                readHolds.remove();

                // no more holds on this member; we should release the lock on the server
                final Thread    thread = Thread.currentThread();
                final LockOwner owner  = new LockOwner(f_localMember, thread.getId());
                boolean fUnlocked = f_locks.invoke(f_sName, entry ->
                        {
                        ReadWriteLockHolder lock = entry.getValue();
                        if (lock != null && lock.unlockRead(owner))
                            {
                            entry.setValue(lock);
                            return true;
                            }
                        return false;
                        });

                // the above should always return true if this thread owned the read lock on the server
                // if it didn't, something is wrong, and we need to bail out
                if (!fUnlocked)
                    {
                    throw new IllegalMonitorStateException();
                    }
                }
            --rh.count;

            for (; ; )
                {
                int c = getState();
                int cNext = c - SHARED_UNIT;
                if (compareAndSetState(c, cNext))
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    {
                    return cNext == 0;
                    }
                }
            }

        @Override
        protected final int tryAcquireShared(int unused)
            {
            return tryReadLock() ? 1 : -1;
            }

        /**
         * Performs tryLock for read.
         */
        final boolean tryReadLock()
            {
            Thread thread = Thread.currentThread();
            final LockOwner owner = new LockOwner(f_localMember, thread.getId());
            boolean fLocked = false;

            for (; ; )
                {
                int c = getState();
                if (getExclusiveOwnerThread() != thread)
                    {
                    if (exclusiveCount(c) != 0 || readerShouldBlock())
                        {
                        return false;
                        }
                    }

                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    {
                    throw new Error("Maximum lock count exceeded");
                    }

                Sync.HoldCounter rh = readHolds.get();
                if (rh.count == 0 && !fLocked)
                    {
                    // first read lock attempt from this thread
                    // attempt to obtain read lock on the server
                    fLocked = f_locks.invoke(f_sName, entry ->
                            {
                            ReadWriteLockHolder lock = entry.getValue(ReadWriteLockHolder::new);
                            boolean fRes = lock.lockRead(owner);
                            entry.setValue(lock);
                            return fRes;
                            });

                    if (!fLocked)
                        {
                        // unable to obtain read lock on the server; fail
                        readHolds.remove();
                        return false;
                        };
                    }

                if (compareAndSetState(c, c + SHARED_UNIT))
                    {
                    rh.count++;
                    return true;
                    }
                }
            }

        /**
         * Returns true if the current thread, when trying to acquire
         * the read lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        boolean readerShouldBlock()
            {
            return hasQueuedPredecessors();
            }

        /**
         * Returns true if the current thread, when trying to acquire
         * the write lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        boolean writerShouldBlock()
            {
            return false;
            }

        @Override
        protected final boolean isHeldExclusively()
            {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
            }

        // ---- helper methods ----------------------------------------------

        /**
         * Map event handler that changes the status of this sync object when
         * write lock is acquired or released on the server by another member.
         *
         * @param event  a change event to process
         */
        private void onHolderChange(MapEvent<? extends String, ? extends ReadWriteLockHolder> event)
            {
            ReadWriteLockHolder holder = event.getNewValue();
            if (holder.isWriteLocked() && !holder.isWriteLockedByMember(f_localMember.getUuid()))
                {
                acquire(-1);
                }
            else if (!holder.isWriteLocked())
                {
                release(-1);
                }
            }

        final ConditionObject newCondition()
            {
            throw new UnsupportedOperationException();
            }

        final LockOwner getOwner()
            {
            return f_locks.invoke(f_sName, Processors.extract(ReadWriteLockHolder::getWriteLock));
            }

        final int getReadLockCount()
            {
            return f_locks.invoke(f_sName, Processors.extract(ReadWriteLockHolder::getReadLockCount));
            }

        final boolean isReadLocked()
            {
            return f_locks.invoke(f_sName, Processors.extract(ReadWriteLockHolder::isReadLocked));
            }

        final boolean isWriteLocked()
            {
            return f_locks.invoke(f_sName, Processors.extract(ReadWriteLockHolder::isWriteLocked));
            }

        final int getWriteHoldCount()
            {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
            }

        final int getReadHoldCount()
            {
            if (getReadLockCount() == 0)
                {
                return 0;
                }

            int count = readHolds.get().count;
            if (count == 0)
                {
                readHolds.remove();
                }
            return count;
            }

        final int getCount()
            {
            return getState();
            }

        // ---- inner class: HoldCounter ------------------------------------

        /**
         * A counter for per-thread read hold counts. Maintained as a
         * ThreadLocal; cached in cachedHoldCounter.
         */
        static final class HoldCounter
            {
            int count;          // initially 0
            final long tid = Thread.currentThread().getId();
            }

        // ---- inner class: ThreadLocalHoldCounter -------------------------

        /**
         * ThreadLocal subclass.
         */
        static final class ThreadLocalHoldCounter
                extends ThreadLocal<Sync.HoldCounter>
            {
            public Sync.HoldCounter initialValue()
                {
                return new Sync.HoldCounter();
                }
            }

        // ---- static helpers ----------------------------------------------

        /**
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */
        static final int SHARED_SHIFT   = 16;
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        /**
         * Returns the number of shared holds represented in count.
         */
        static int sharedCount(int c)
            {
            return c >>> SHARED_SHIFT;
            }

        /**
         * Returns the number of exclusive holds represented in count.
         */
        static int exclusiveCount(int c)
            {
            return c & EXCLUSIVE_MASK;
            }

        // ---- data members ------------------------------------------------

        /**
         * Local member/current process identifier.
         */
        private final Member f_localMember;

        /**
         * The name of the remote lock; used as a key in the NamedMap containing
         * the locks.
         */
        private final String f_sName;

        /**
         * The NamedMap containing the remote locks.
         */
        private final NamedMap<String, ReadWriteLockHolder> f_locks;

        /**
         * The number of reentrant read locks held by current thread.
         * Removed whenever a thread's read hold count drops to 0.
         */
        private final transient Sync.ThreadLocalHoldCounter readHolds;
        }

    // ---- inner class: ReadLock -------------------------------------------

    /**
     * The lock returned by method {@link RemoteReadWriteLock#readLock}.
     */
    public static class ReadLock
            implements Lock
        {
        private final Sync f_sync;

        /**
         * Constructor for use by subclasses.
         *
         * @param lock the outer lock object
         *
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(RemoteReadWriteLock lock)
            {
            f_sync = lock.f_sync;
            }

        /**
         * Acquires the read lock.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately.
         *
         * <p>If the write lock is held by another thread then
         * the current thread becomes disabled for thread scheduling purposes
         * and lies dormant until the read lock has been acquired.
         */
        public void lock()
            {
            f_sync.acquireShared(1);
            }

        /**
         * Acquires the read lock unless the current thread is {@linkplain
         * Thread#interrupt interrupted}.
         *
         * <p>Acquires the read lock if the write lock is not held
         * by another thread and returns immediately.
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul>
         * <p>
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException
            {
            f_sync.acquireSharedInterruptibly(1);
            }

        /**
         * Acquires the read lock only if the write lock is not held by another
         * thread at the time of invocation.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value {@code true}.
         *
         * <p>If the write lock is held by another thread then
         * this method will return immediately with the value {@code false}.
         *
         * @return {@code true} if the read lock was acquired
         */
        public boolean tryLock()
            {
            return f_sync.tryReadLock();
            }

        /**
         * Acquires the read lock if the write lock is not held by another
         * thread within the given waiting time and the current thread has not
         * been {@linkplain Thread#interrupt interrupted}.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value {@code true}.
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses.
         *
         * </ul>
         *
         * <p>If the read lock is acquired then the value {@code true} is
         * returned.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul> then {@link InterruptedException} is thrown and the
         * current thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the read lock
         * @param unit    the time unit of the timeout argument
         *
         * @return {@code true} if the read lock was acquired
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException
            {
            return f_sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
            }

        /**
         * Attempts to release this lock.
         *
         * <p>If the number of readers is now zero then the lock
         * is made available for write lock attempts. If the current thread does
         * not hold this lock then {@link IllegalMonitorStateException} is
         * thrown.
         *
         * @throws IllegalMonitorStateException if the current thread does not
         *                                      hold this lock
         */
        public void unlock()
            {
            f_sync.releaseShared(1);
            }

        /**
         * Throws {@code UnsupportedOperationException} because {@code
         * ReadLocks} do not support conditions.
         *
         * @throws UnsupportedOperationException always
         */
        public Condition newCondition()
            {
            throw new UnsupportedOperationException();
            }

        /**
         * Returns a string identifying this lock, as well as its lock state.
         * The state, in brackets, includes the String {@code "Read locks ="}
         * followed by the number of held read locks.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString()
            {
            int r = f_sync.getReadLockCount();
            return super.toString() +
                   "[Read locks = " + r + "]";
            }
        }

    // ---- inner class: WriteLock ------------------------------------------

    /**
     * The lock returned by method {@link RemoteReadWriteLock#writeLock}.
     */
    public static class WriteLock
            implements Lock
        {
        private final Sync f_sync;

        /**
         * Constructor for use by subclasses.
         *
         * @param lock the outer lock object
         *
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(RemoteReadWriteLock lock)
            {
            f_sync = lock.f_sync;
            }

        /**
         * Acquires the write lock.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread and returns immediately, setting the write
         * lock hold count to one.
         *
         * <p>If the current thread already holds the write lock then the
         * hold count is incremented by one and the method returns immediately.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and lies
         * dormant until the write lock has been acquired, at which time the
         * write lock hold count is set to one.
         */
        public void lock()
            {
            f_sync.acquire(1);
            }

        /**
         * Acquires the write lock unless the current thread is {@linkplain
         * Thread#interrupt interrupted}.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread and returns immediately, setting the write
         * lock hold count to one.
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns immediately.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and lies
         * dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the write lock is acquired by the current thread then the
         * lock hold count is set to one.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         * <p>
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException
            {
            f_sync.acquireInterruptibly(1);
            }

        /**
         * Acquires the write lock only if it is not held by another thread at
         * the time of invocation.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread and returns immediately with the value
         * {@code true}, setting the write lock hold count to one.
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns {@code
         * true}.
         *
         * <p>If the lock is held by another thread then this method
         * will return immediately with the value {@code false}.
         *
         * @return {@code true} if the lock was free and was acquired by the
         * current thread, or the write lock was already held by the current
         * thread; and {@code false} otherwise.
         */
        public boolean tryLock()
            {
            return f_sync.tryAcquire(1);
            }

        /**
         * Acquires the write lock if it is not held by another thread within
         * the given waiting time and the current thread has not been
         * {@linkplain Thread#interrupt interrupted}.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread and returns immediately with the value
         * {@code true}, setting the write lock hold count to one.
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns {@code
         * true}.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and lies
         * dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses
         *
         * </ul>
         *
         * <p>If the write lock is acquired then the value {@code true} is
         * returned and the write lock hold count is set to one.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         * <p>
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the write lock
         * @param unit    the time unit of the timeout argument
         *
         * @return {@code true} if the lock was free and was acquired by the
         * current thread, or the write lock was already held by the current
         * thread; and {@code false} if the waiting time elapsed before the lock
         * could be acquired.
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException
            {
            return f_sync.tryAcquireNanos(1, unit.toNanos(timeout));
            }

        /**
         * Attempts to release this lock.
         *
         * <p>If the current thread is the holder of this lock then
         * the hold count is decremented. If the hold count is now zero then the
         * lock is released.  If the current thread is not the holder of this
         * lock then {@link IllegalMonitorStateException} is thrown.
         *
         * @throws IllegalMonitorStateException if the current thread does not
         *                                      hold this lock
         */
        public void unlock()
            {
            f_sync.release(1);
            }

        /**
         * Returns a {@link Condition} instance for use with this {@link Lock}
         * instance.
         * <p>The returned {@link Condition} instance supports the same
         * usages as do the {@link Object} monitor methods ({@link Object#wait()
         * wait}, {@link Object#notify notify}, and {@link Object#notifyAll
         * notifyAll}) when used with the built-in monitor lock.
         *
         * <ul>
         *
         * <li>If this write lock is not held when any {@link
         * Condition} method is called then an {@link
         * IllegalMonitorStateException} is thrown.  (Read locks are
         * held independently of write locks, so are not checked or
         * affected. However it is essentially always an error to
         * invoke a condition waiting method when the current thread
         * has also acquired read locks, since other threads that
         * could unblock it will not be able to acquire the write
         * lock.)
         *
         * <li>When the condition {@linkplain Condition#await() waiting}
         * methods are called the write lock is released and, before
         * they return, the write lock is reacquired and the lock hold
         * count restored to what it was when the method was called.
         *
         * <li>If a thread is {@linkplain Thread#interrupt interrupted} while
         * waiting then the wait will terminate, an {@link
         * InterruptedException} will be thrown, and the thread's
         * interrupted status will be cleared.
         *
         * <li>Waiting threads are signalled in FIFO order.
         *
         * <li>The ordering of lock reacquisition for threads returning
         * from waiting methods is the same as for threads initially
         * acquiring the lock, which is in the default case not specified,
         * but for <em>fair</em> locks favors those threads that have been
         * waiting the longest.
         *
         * </ul>
         *
         * @return the Condition object
         */
        public Condition newCondition()
            {
            return f_sync.newCondition();
            }

        /**
         * Returns a string identifying this lock, as well as its lock state.
         * The state, in brackets includes either the String {@code "Unlocked"}
         * or the String {@code "Locked by"} followed by the {@linkplain
         * Thread#getName name} of the owning thread.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString()
            {
            LockOwner o = f_sync.getOwner();
            return super.toString() + ((o == null) ?
                                       "[Unlocked]" :
                                       "[Locked by " + o + "]");
            }

        /**
         * Queries if this write lock is held by the current thread. Identical
         * in effect to {@link ReentrantReadWriteLock#isWriteLockedByCurrentThread}.
         *
         * @return {@code true} if the current thread holds this lock and {@code
         * false} otherwise
         */
        public boolean isHeldByCurrentThread()
            {
            return f_sync.isHeldExclusively();
            }

        /**
         * Queries the number of holds on this write lock by the current thread.
         *  A thread has a hold on a lock for each lock action that is not
         * matched by an unlock action.  Identical in effect to {@link
         * ReentrantReadWriteLock#getWriteHoldCount}.
         *
         * @return the number of holds on this lock by the current thread, or
         * zero if this lock is not held by the current thread
         *
         * @since 1.6
         */
        public int getHoldCount()
            {
            return f_sync.getWriteHoldCount();
            }
        }

    // Instrumentation and status

    /**
     * Returns the thread that currently owns the write lock, or {@code null} if
     * not owned. When this method is called by a thread that is not the owner,
     * the return value reflects a best-effort approximation of current lock
     * status. For example, the owner may be momentarily {@code null} even if
     * there are threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of subclasses that
     * provide more extensive lock monitoring facilities.
     *
     * @return the owner, or {@code null} if not owned
     */
    public LockOwner getOwner()
        {
        return f_sync.getOwner();
        }

    /**
     * Queries the number of read locks held for this lock. This method is
     * designed for use in monitoring system state, not for synchronization
     * control.
     *
     * @return the number of read locks held
     */
    public int getReadLockCount()
        {
        return f_sync.getReadLockCount();
        }

    /**
     * Queries if the read lock is held by any thread. This method is designed
     * for use in monitoring system state, not for synchronization control.
     *
     * @return {@code true} if any thread holds the read lock and {@code false}
     * otherwise
     */
    public boolean isReadLocked()
        {
        return f_sync.isReadLocked();
        }

    /**
     * Queries if the write lock is held by any thread. This method is designed
     * for use in monitoring system state, not for synchronization control.
     *
     * @return {@code true} if any thread holds the write lock and {@code false}
     * otherwise
     */
    public boolean isWriteLocked()
        {
        return f_sync.isWriteLocked();
        }

    /**
     * Queries if the write lock is held by the current thread.
     *
     * @return {@code true} if the current thread holds the write lock and
     * {@code false} otherwise
     */
    public boolean isWriteLockedByCurrentThread()
        {
        return f_sync.isHeldExclusively();
        }

    /**
     * Queries the number of reentrant write holds on this lock by the current
     * thread.  A writer thread has a hold on a lock for each lock action that
     * is not matched by an unlock action.
     *
     * @return the number of holds on the write lock by the current thread, or
     * zero if the write lock is not held by the current thread
     */
    public int getWriteHoldCount()
        {
        return f_sync.getWriteHoldCount();
        }

    /**
     * Queries the number of reentrant read holds on this lock by the current
     * thread.  A reader thread has a hold on a lock for each lock action that
     * is not matched by an unlock action.
     *
     * @return the number of holds on the read lock by the current thread, or
     * zero if the read lock is not held by the current thread
     *
     * @since 1.6
     */
    public int getReadHoldCount()
        {
        return f_sync.getReadHoldCount();
        }

    /**
     * Queries whether any threads are waiting to acquire this lock. Note that
     * because cancellations may occur at any time, a {@code true} return does
     * not guarantee that any other thread will ever acquire this lock.  This
     * method is designed primarily for use in monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to acquire the
     * lock
     */
    public final boolean hasQueuedThreads()
        {
        return f_sync.hasQueuedThreads();
        }

    /**
     * Queries whether the given thread is waiting to acquire this lock. Note
     * that because cancellations may occur at any time, a {@code true} return
     * does not guarantee that this thread will ever acquire this lock.  This
     * method is designed primarily for use in monitoring of the system state.
     *
     * @param thread the thread
     *
     * @return {@code true} if the given thread is queued waiting for this lock
     *
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread)
        {
        return f_sync.isQueued(thread);
        }

    /**
     * Returns an estimate of the number of threads waiting to acquire this
     * lock.  The value is only an estimate because the number of threads may
     * change dynamically while this method traverses internal data structures.
     * This method is designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength()
        {
        return f_sync.getQueueLength();
        }

    /**
     * Returns a string identifying this lock, as well as its lock state. The
     * state, in brackets, includes the String {@code "Write locks ="} followed
     * by the number of reentrantly held write locks, and the String {@code
     * "Read locks ="} followed by the number of held read locks.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString()
        {
        int c = f_sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
               "[Write locks = " + w + ", Read locks = " + r + "]";
        }

    // ---- data members ----------------------------------------------------

    /**
     * A read lock instance.
     */
    private final ReadLock f_readerLock;

    /**
     * A write lock instance.
     */
    private final WriteLock f_writerLock;

    /**
     * Performs all synchronization mechanics
     */
    final Sync f_sync;
    }
