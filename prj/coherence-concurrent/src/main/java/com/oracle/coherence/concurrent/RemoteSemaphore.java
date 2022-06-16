/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

import com.oracle.coherence.concurrent.internal.SemaphoreStatus;

import com.tangosol.net.Member;
import com.tangosol.net.NamedMap;

import com.tangosol.util.MapEvent;
import com.tangosol.util.Processors;
import com.tangosol.util.function.Remote;
import com.tangosol.util.listener.SimpleMapListener;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A distributed counting semaphore.  Conceptually, a semaphore maintains a set of
 * permits.  Each {@link #acquire} blocks if necessary until a permit is
 * available, and then takes it.  Each {@link #release} adds a permit,
 * potentially releasing a blocking acquirer.
 * However, no actual permit objects are used; the {@code RemoteSemaphore}
 * just keeps a count of the number available and acts accordingly.
 *
 * @author Vaso Putica  2021.12.01
 * @since 21.12
 */
public class RemoteSemaphore
        implements com.oracle.coherence.concurrent.Semaphore
    {
    /**
     * Create an instance of {@code RemoteSemaphore}
     *
     * @param sName      the name of the semaphore
     * @param permits    the initial number of permits available
     * @param semaphores the {@link NamedMap} that stores this semaphore's state
     */
    public RemoteSemaphore(String sName, int permits, NamedMap<String, SemaphoreStatus> semaphores)
        {
        f_sync = new Sync(sName, permits, semaphores);
        semaphores.addMapListener(new SimpleMapListener<String, SemaphoreStatus>()
                                          .addUpdateHandler(f_sync::onSemaphoreStatusChange),
                                  sName, false);
        }

    /**
     * Acquires a permit from this semaphore, blocking until one is
     * available, or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void acquire() throws InterruptedException
        {
        f_sync.acquireSharedInterruptibly(1);
        }

    /**
     * Acquires a permit from this semaphore, blocking until one is
     * available.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it will continue to wait, but the
     * time at which the thread is assigned a permit may change compared to
     * the time it would have received the permit had no interruption
     * occurred.  When the thread does return from this method its interrupt
     * status will be set.
     */
    public void acquireUninterruptibly()
        {
        f_sync.acquireShared(1);
        }

    /**
     * Acquires a permit from this semaphore, only if one is available at the
     * time of invocation.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then this method will return
     * immediately with the value {@code false}.
     *
     * @return {@code true} if a permit was acquired and {@code false}
     *         otherwise
     */
    public boolean tryAcquire()
        {
        return f_sync.tryAcquireShared(1) >= 0;
        }

    /**
     * Acquires a permit from this semaphore, if one becomes available
     * within the given waiting time and the current thread has not
     * been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If a permit is acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     *         if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException
        {
        return f_sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

    /**
     * Releases a permit, returning it to the semaphore.
     *
     * <p>Releases a permit, increasing the number of available permits by
     * one.  If any threads are trying to acquire a permit, then one is
     * selected and given the permit that was just released.  That thread
     * is (re)enabled for thread scheduling purposes.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link #acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     */
    public void release()
        {
        f_sync.releaseShared(1);
        }

    /**
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available,
     * or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount. This method has the same effect as the
     * loop {@code for (int i = 0; i < permits; ++i) acquire();} except
     * that it atomically acquires the permits all at once:
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore and the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread are instead
     * assigned to other threads trying to acquire permits, as if
     * permits had been made available by a call to {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquire(int permits) throws InterruptedException
        {
        if (permits < 0)
            {
            throw new IllegalArgumentException();
            }
        f_sync.acquireSharedInterruptibly(permits);
        }

    /**
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount. This method has the same effect as the
     * loop {@code for (int i = 0; i < permits; ++i) acquireUninterruptibly();}
     * except that it atomically acquires the permits all at once:
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes one of the {@link #release() release}
     * methods for this semaphore and the current thread is next to be assigned
     * permits and the number of available permits satisfies this request.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for permits then it will continue to wait and its
     * position in the queue is not affected.  When the thread does return
     * from this method its interrupt status will be set.
     *
     * @param permits the number of permits to acquire
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquireUninterruptibly(int permits)
        {
        if (permits < 0)
            {
            throw new IllegalArgumentException();
            }
        f_sync.acquireShared(permits);
        }

    /**
     * Acquires the given number of permits from this semaphore, only
     * if all are available at the time of invocation.
     *
     * <p>Acquires the given number of permits, if they are available, and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then this method will return
     * immediately with the value {@code false} and the number of available
     * permits is unchanged.
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired and
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits)
        {
        if (permits < 0)
            {
            throw new IllegalArgumentException();
            }
        return f_sync.tryAcquireShared(permits) >= 0;
        }

    /**
     * Acquires the given number of permits from this semaphore, if all
     * become available within the given waiting time and the current
     * thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore and the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the permits are acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire the permits,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread, are instead
     * assigned to other threads trying to acquire permits, as if
     * the permits had been made available by a call to {@link #release()}.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.  Any permits that were to be assigned to this
     * thread, are instead assigned to other threads trying to acquire
     * permits, as if the permits had been made available by a call to
     * {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if all permits were acquired and {@code false}
     *         if the waiting time elapsed before all permits were acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException
        {
        if (permits < 0)
            {
            throw new IllegalArgumentException();
            }
        return f_sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
        }

    /**
     * Releases the given number of permits, returning them to the semaphore.
     *
     * <p>Releases the given number of permits, increasing the number of
     * available permits by that amount.
     * If any threads are trying to acquire permits, then one thread
     * is selected and given the permits that were just released.
     * If the number of available permits satisfies that thread's request
     * then that thread is (re)enabled for thread scheduling purposes;
     * otherwise the thread will wait until sufficient permits are available.
     * If there are still permits available
     * after this thread's request has been satisfied, then those permits
     * are assigned in turn to other threads trying to acquire permits.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link Semaphore#acquire acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     *
     * @param permits the number of permits to release
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void release(int permits)
        {
        if (permits < 0)
            {
            throw new IllegalArgumentException();
            }
        f_sync.releaseShared(permits);
        }

    /**
     * Returns the current number of permits available in this semaphore.
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the number of permits available in this semaphore
     */
    public int availablePermits()
        {
        return f_sync.getPermits();
        }

    /**
     * Acquires and returns all permits that are immediately
     * available, or if negative permits are available, releases them.
     * Upon return, zero permits are available.
     *
     * @return the number of permits acquired or, if negative, the
     * number released
     */
    public int drainPermits()
        {
        return f_sync.drainPermits();
        }

    /**
     * Shrinks the number of available permits by the indicated
     * reduction. This method can be useful in subclasses that use
     * semaphores to track resources that become unavailable. This
     * method differs from {@code acquire} in that it does not block
     * waiting for permits to become available.
     *
     * @param reduction the number of permits to remove
     * @throws IllegalArgumentException if {@code reduction} is negative
     */
    protected void reducePermits(int reduction)
        {
        if (reduction < 0)
            {
            throw new IllegalArgumentException();
            }
        f_sync.reducePermits(reduction);
        }

    /**
     * Queries if an permit is acquired by the current thread.
     *
     * @return {@code true} if current thread acquired permit and {@code false}
     * otherwise
     */
    public boolean isAcquiredByCurrentThread()
        {
        return f_sync.isAcquiredByThread(Thread.currentThread());
        }

    /**
     * Returns a string identifying this semaphore, as well as its state.
     * The state, in brackets, includes the String {@code "Permits ="}
     * followed by the number of permits.
     *
     * @return a string identifying this semaphore, as well as its state
     */
    @Override
    public String toString()
        {
        return super.toString() + "[Permits = " + f_sync.getPermits() + "]";
        }

    /**
     * Returns number of permits that were used to initialise this semaphore.
     *
     * @return initial number of permits that were used to initialise this semaphore.
     */
    public int getInitialPermits()
        {
        return f_sync.getInitialPermits();
        }

    // ----- inner class Sync -----------------------------------------------

    /**
     * Synchronization control for {@link RemoteSemaphore}.
     */
    static class Sync extends AbstractQueuedSynchronizer
        {
        Sync(String sName, int permits, NamedMap<String, SemaphoreStatus> semaphores)
            {
            Member localMember = semaphores.getService().getCluster().getLocalMember();

            f_sName          = sName;
            f_semaphores     = semaphores;
            f_localMember    = localMember;
            f_initialPermits = permits;

            setState(permits);
            }

        // ---- AbstractQueuedLongSynchronizer methods ----------------------

        @Override
        protected int tryAcquireShared(final int acquires)
            {
            if (acquires == -1)
                {
                return 0;
                }

            final Thread thread = Thread.currentThread();
            final int initialPermits = f_initialPermits;
            final PermitAcquirer acquirer = new PermitAcquirer(f_localMember, thread.getId());
            final Remote.Supplier<SemaphoreStatus> supplier = () -> new SemaphoreStatus(initialPermits);
            Integer acquired = f_semaphores.invoke(f_sName, entry ->
                {
                SemaphoreStatus semaphoreStatus = entry.getValue(supplier);
                boolean success = semaphoreStatus.acquire(acquirer, acquires);
                entry.setValue(semaphoreStatus);
                if (success)
                    {
                    return semaphoreStatus.getPermits();
                    }
                return -1;
                });
            if (acquired >= 0)
                {
                setState(acquired);
                }
            return acquired;
            }

        @Override
        protected final boolean tryReleaseShared(int releases)
            {
            if (releases == -1)
                {
                return true;
                }
            final Thread thread = Thread.currentThread();

            final PermitAcquirer acquirer = new PermitAcquirer(f_localMember, thread.getId());
            Integer state = f_semaphores.invoke(f_sName, entry ->
                {
                SemaphoreStatus status = entry.getValue();
                if (status.release(acquirer, releases))
                    {
                    entry.setValue(status);
                    }
                return status.getPermits();
                });
            if (state == null)
                {
                throw new IllegalMonitorStateException();
                }
            setState(state);
            return state > 0;
            }


        // ---- helper methods ----------------------------------------------

        final int drainPermits()
            {
            final Thread thread = Thread.currentThread();
            final PermitAcquirer acquirer = new PermitAcquirer(f_localMember, thread.getId());
            Integer cur = f_semaphores.invoke(f_sName, entry ->
                {
                SemaphoreStatus status = entry.getValue();
                int drained = status.drainPermits(acquirer);
                entry.setValue(status);
                return drained;
                });
            setState(0);
            return cur;
            }

        final void reducePermits(int reductions)
            {
            final Thread thread = Thread.currentThread();
            final PermitAcquirer acquirer = new PermitAcquirer(f_localMember, thread.getId());
            Integer nextState = f_semaphores.invoke(f_sName, entry ->
                {
                SemaphoreStatus status = entry.getValue();
                int availablePermits = status.reducePermits(acquirer, reductions);
                entry.setValue(status);
                return availablePermits;
                });
            setState(nextState);
            }

        /**
         * Returns the current number of permits available.
         *
         * @return the number of permits available
         */
        final int getPermits()
            {
            return f_semaphores.invoke(f_sName, Processors.extract(SemaphoreStatus::getPermits));
            }

        /**
         * Returns number of permits that were used to initialise this semaphore.
         *
         * @return initial number of permits that were used to initialise this semaphore.
         */
        public int getInitialPermits()
            {
            return f_initialPermits;
            }

        final boolean isAcquiredByThread(Thread thread)
            {
            final PermitAcquirer acquirer = new PermitAcquirer(f_localMember, thread.getId());
            return f_semaphores.invoke(f_sName, entry ->
                {
                SemaphoreStatus status = entry.getValue();
                return status != null && status.isAcquiredBy(acquirer);
                });
            }

        /**
         * Map event handler that changes the status of this sync object when
         * a semaphore is acquired or released on the server by another member.
         *
         * @param event  a change event to process
         */
        private void onSemaphoreStatusChange(MapEvent<? extends String, ? extends SemaphoreStatus> event)
            {
            SemaphoreStatus oldStatus = event.getOldValue();
            SemaphoreStatus status = event.getNewValue();
            // no state change
            if (oldStatus.getPermits() == status.getPermits())
                {
                return;
                }
            if (status.getPermits() <= 0 && !f_localMember.getUuid().equals(status.getMember()))
                {
                // other node acquired last permit; prevent anyone local from acquiring permit
                acquireShared(-1);
                }
            else if (status.getPermits() > 0
                     && status.getPermits() > oldStatus.getPermits()
                     && !f_localMember.getUuid().equals(status.getMember()))
                {
                // other node released permits; put local semaphore back in business
                releaseShared(-1);
                }
            }

        // ---- data members ----------------------------------------------------

        /**
         * Local member/current process identifier.
         */
        private final Member f_localMember;

        /**
         * The name of the remote semaphore; used as a key in the NamedMap containing the semaphores.
         */
        private final String f_sName;

        /**
         * The NamedMap containing the remote semaphores.
         */
        private final NamedMap<String, SemaphoreStatus> f_semaphores;

        /**
         * The initial number of permits available.
         */
        private final int f_initialPermits;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Synchronizer providing all implementation mechanics
     */
    private final Sync f_sync;

    }
