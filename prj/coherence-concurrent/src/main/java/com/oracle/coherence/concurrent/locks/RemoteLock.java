/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.oracle.coherence.concurrent.locks.internal.ExclusiveLockHolder;

import com.tangosol.net.Member;
import com.tangosol.net.NamedMap;

import com.tangosol.util.MapEvent;
import com.tangosol.util.Processors;

import com.tangosol.util.listener.SimpleMapListener;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A reentrant mutual exclusion distributed {@link Lock} with the same basic
 * behavior and semantics as the implicit monitor lock accessed using {@code
 * synchronized} methods and statements, and the {@link ReentrantLock} class,
 * but with support for access synchronization across multiple cluster members.
 *
 * <p>A {@code DistributedLock} is <em>owned</em> by the member and thread
 * last successfully locking, but not yet unlocking it. A thread invoking
 * {@code lock} will return, successfully acquiring the lock, when the lock is not
 * owned by another thread. The method will return immediately if the current
 * thread already owns the lock. This can be checked using methods {@link
 * #isHeldByCurrentThread}, and {@link #getHoldCount}.
 *
 * <p>It is recommended practice to <em>always</em> immediately
 * follow a call to {@code lock} with a {@code try} block, most typically in a
 * before/after construction such as:
 *
 * <pre> {@code
 * class X {
 *   private final DistributedLock lock = Locks.remoteLock("myLock");
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}</pre>
 *
 * <p>In addition to implementing the {@link Lock} interface, this
 * class defines a number of {@code public} and {@code protected} methods for
 * inspecting the state of the lock.  Some of these methods are only useful for
 * instrumentation and monitoring.
 *
 * <p>Unlike {@link ReentrantLock}, this class does not support serialization,
 * or {@link Condition}s.
 *
 * <p>This lock supports a maximum of {@code Long#MAX_VALUE} recursive locks by
 * the same thread. Attempts to exceed this limit result in {@link Error} throws
 * from locking methods.
 *
 * @author Aleks Seovic  2021.10.19
 * @since 21.12
 */
public class RemoteLock
        implements Lock
    {
    /**
     * Create an instance of {@code DistributedLock}.
     * 
     * @param sName  the name of the lock
     * @param locks  the {@link NamedMap} that stores this lock's state
     */
    RemoteLock(String sName, NamedMap<String, ExclusiveLockHolder> locks)
        {
        f_sync = new Sync(sName, locks);
        locks.addMapListener(new SimpleMapListener<String, ExclusiveLockHolder>()
                                     .addInsertHandler(f_sync::onHolderChange)
                                     .addUpdateHandler(f_sync::onHolderChange),
                             sName, false);
        }

    // ---- Lock interface ---------------------------------------------------
    
    /**
     * Acquires the lock.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *
     * <p>If the current thread already holds the lock then the hold
     * count is incremented by one and the method returns immediately.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling purposes and lies
     * dormant until the lock has been acquired, at which time the lock hold
     * count is set to one.
     */
    public void lock()
        {
        f_sync.acquire(1);
        }

    /**
     * Acquires the lock unless the current thread is {@linkplain
     * Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *
     * <p>If the current thread already holds this lock then the hold count
     * is incremented by one and the method returns immediately.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happens:
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread.
     *
     * </ul>
     *
     * <p>If the lock is acquired by the current thread then the lock hold
     * count is set to one.
     *
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock,
     *
     * </ul>
     * <p>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void lockInterruptibly() throws InterruptedException
        {
        f_sync.acquireInterruptibly(1);
        }

    /**
     * Acquires the lock only if it is not held by another thread at the time of
     * invocation.
     *
     * <p>Acquires the lock if it is not held by another thread and
     * returns immediately with the value {@code true}, setting the lock hold
     * count to one.
     *
     * <p>If the current thread already holds this lock then the hold
     * count is incremented by one and the method returns {@code true}.
     *
     * <p>If the lock is held by another thread then this method will return
     * immediately with the value {@code false}.
     *
     * @return {@code true} if the lock was free and was acquired by the current
     * thread, or the lock was already held by the current thread; and {@code
     * false} otherwise
     */
    public boolean tryLock()
        {
        return f_sync.tryAcquire(1);
        }

    /**
     * Acquires the lock if it is not held by another thread within the given
     * waiting time and the current thread has not been {@linkplain
     * Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately with the value {@code true}, setting the lock hold count to
     * one.
     *
     * <p>If the current thread
     * already holds this lock then the hold count is incremented by one and the
     * method returns {@code true}.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happens:
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses
     *
     * </ul>
     *
     * <p>If the lock is acquired then the value {@code true} is returned and
     * the lock hold count is set to one.
     *
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while
     * acquiring the lock,
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock, and
     * over reporting the elapse of the waiting time.
     *
     * @param timeout the time to wait for the lock
     * @param unit    the time unit of the timeout argument
     *
     * @return {@code true} if the lock was free and was acquired by the current
     * current thread, or the lock was already held by the current thread; and
     * {@code false} if the waiting time elapsed before the lock could be
     * acquired
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
     * <p>If the current thread is the holder of this lock then the hold
     * count is decremented.  If the hold count is now zero then the lock is
     * released.  If the current thread is not the holder of this lock then
     * {@link IllegalMonitorStateException} is thrown.
     *
     * @throws IllegalMonitorStateException if the current thread does not hold
     *                                      this lock
     */
    public void unlock()
        {
        f_sync.release(1);
        }

    @Override
    public final Condition newCondition()
        {
        throw new UnsupportedOperationException();
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Queries the number of holds on this lock by the current thread.
     *
     * <p>A thread has a hold on a lock for each lock action that is not
     * matched by an unlock action.
     *
     * <p>The hold count information is typically only used for testing and
     * debugging purposes. For example, if a certain section of code should not
     * be entered with the lock already held then we can assert that fact:
     *
     * <pre> {@code
     * class X {
     *   DistributedLock lock = Locks.remoteLock("myLock");
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0;
     *     lock.lock();
     *     try {
     *       // ... method body
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}</pre>
     *
     * @return the number of holds on this lock by the current thread, or zero
     * if this lock is not held by the current thread
     */
    public long getHoldCount()
        {
        return f_sync.getHoldCount();
        }

    /**
     * Queries if this lock is held by the current thread.
     *
     * <p>Analogous to the {@link Thread#holdsLock(Object)} method for
     * built-in monitor locks, this method is typically used for debugging and
     * testing. For example, a method that should only be called while a lock is
     * held can assert that this is the case:
     *
     * <pre> {@code
     * class X {
     *   DistributedLock lock = Locks.remoteLock("myLock");
     *   // ...
     *
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... method body
     *   }
     * }}</pre>
     *
     * <p>It can also be used to ensure that a reentrant lock is used
     * in a non-reentrant manner, for example:
     *
     * <pre> {@code
     * class X {
     *   DistributedLock lock = Locks.remoteLock("myLock");
     *   // ...
     *
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread();
     *       lock.lock();
     *       try {
     *           // ... method body
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}</pre>
     *
     * @return {@code true} if current thread holds this lock and {@code false}
     * otherwise
     */
    public boolean isHeldByCurrentThread()
        {
        return f_sync.isHeldExclusively();
        }

    /**
     * Queries if this lock is held by any thread. This method is designed for
     * use in monitoring of the system state, not for synchronization control.
     *
     * @return {@code true} if any thread holds this lock and {@code false}
     * otherwise
     */
    public boolean isLocked()
        {
        return f_sync.isLocked();
        }

    /**
     * Returns the identity of the process and thread that currently owns this
     * lock, or {@code null} if not owned.
     *
     * @return the owner, or {@code null} if not owned
     */
    public LockOwner getOwner()
        {
        return f_sync.getOwner();
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
     * state, in brackets, includes either the String {@code "Unlocked"} or the
     * String {@code "Locked by"} followed by the {@linkplain Thread#getName
     * name} of the owning thread.
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

    // ---- inner class: Sync -----------------------------------------------

    /**
     * Synchronization control for this lock.
     *
     * Uses AQS state to represent the number of holds on the lock, in order
     * to avoid network calls when incrementing holds.
     */
    static class Sync
            extends AbstractQueuedLongSynchronizer
        {
        /**
         * Construct a {@code Sync} instance.
         *
         * @param sName  the name of the lock
         * @param locks  the {@link NamedMap} that stores this lock's state
         */
        Sync(String sName, NamedMap<String, ExclusiveLockHolder> locks)
            {
            Member localMember = locks.getService().getCluster().getLocalMember();

            f_sName       = sName;
            f_locks       = locks;
            f_localMember = localMember;
            }

        // ---- AbstractQueuedLongSynchronizer methods ----------------------

        @Override
        protected final boolean tryAcquire(long acquires)
            {
            final Thread thread = Thread.currentThread();
            if (acquires == -1)
                {
                // we are acquiring this special lock from an event dispatcher thread
                // because another member has acquired the lock already
                setState(-1);
                setExclusiveOwnerThread(thread);
                return true;
                }

            long c = getState();
            if (c == 0)
                {
                // no thread in this process owns the lock; try to obtain it
                final LockOwner owner = new LockOwner(f_localMember, thread.getId());
                boolean fLocked = f_locks.invoke(f_sName, entry ->
                        {
                        ExclusiveLockHolder lock = entry.getValue(ExclusiveLockHolder::new);
                        boolean fRes = lock.lock(owner);
                        entry.setValue(lock);
                        return fRes;
                        });

                if (fLocked && compareAndSetState(0, acquires))
                    {
                    setExclusiveOwnerThread(thread);
                    return true;
                    }
                }
            else if (thread == getExclusiveOwnerThread())
                {
                // this thread already owns the lock, so no need to make network
                // call to the server; simply increment local state and return true
                long cNext = c + acquires;
                if (cNext < 0) // overflow
                    {
                    throw new Error("Maximum lock count exceeded");
                    }
                setState(cNext);
                return true;
                }
            return false;
            }

        @Override
        protected final boolean tryRelease(long releases)
            {
            final Thread thread = Thread.currentThread();
            if (releases == -1)
                {
                // we are releasing this special lock from an event dispatcher thread
                // because another member has released the lock
                if (thread == getExclusiveOwnerThread())
                    {
                    setState(0);
                    setExclusiveOwnerThread(null);
                    }
                return true;
                }

            long c = getState() - releases;
            if (thread != getExclusiveOwnerThread())
                {
                throw new IllegalMonitorStateException(thread + " != " + getExclusiveOwnerThread());
                }
            boolean fFree = false;
            if (c == 0)
                {
                // final release of the lock; we should release the lock on the server
                final LockOwner owner = new LockOwner(f_localMember, thread.getId());
                boolean fUnlocked = f_locks.invoke(f_sName, entry ->
                        {
                        ExclusiveLockHolder lock = entry.getValue();
                        if (lock != null && lock.unlock(owner))
                            {
                            entry.setValue(lock);
                            return true;
                            }
                        return false;
                        });
                if (fUnlocked)
                    {
                    fFree = true;
                    setExclusiveOwnerThread(null);
                    }
                else
                    {
                    throw new IllegalMonitorStateException();
                    }
                }
            setState(c);
            return fFree;
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
         * a lock is acquired or released on the server by another member.
         *
         * @param event  a change event to process
         */
        private void onHolderChange(MapEvent<? extends String, ? extends ExclusiveLockHolder> event)
            {
            ExclusiveLockHolder holder = event.getNewValue();
            if (holder.isLocked() && !holder.isLockedByMember(f_localMember.getUuid()))
                {
                acquire(-1);
                }
            else if (!holder.isLocked())
                {
                release(-1);
                }
            }

        final LockOwner getOwner()
            {
            return f_locks.invoke(f_sName, Processors.extract(ExclusiveLockHolder::getOwner));
            }

        final long getHoldCount()
            {
            return isHeldExclusively() ? getState() : 0;
            }

        final boolean isLocked()
            {
            return f_locks.invoke(f_sName, Processors.extract(ExclusiveLockHolder::isLocked));
            }

        // ---- data members ------------------------------------------------

        /**
         * Local member/current process identifier.
         */
        private final Member f_localMember;

        /**
         * The name of the remote lock; used as a key in the NamedMap containing the locks.
         */
        private final String f_sName;

        /**
         * The NamedMap containing the remote locks.
         */
        private final NamedMap<String, ExclusiveLockHolder> f_locks;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Synchronizer providing all implementation mechanics
     */
    private final Sync f_sync;
    }
