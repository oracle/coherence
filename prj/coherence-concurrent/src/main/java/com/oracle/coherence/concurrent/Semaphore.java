/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent;


import java.util.concurrent.TimeUnit;


/**
 * A counting semaphore. Conceptually, a semaphore maintains a set of permits.
 * Each {@link #acquire()} blocks if necessary until a permit is available,
 * and then takes it. Each {@link #release()} adds a permit, potentially
 * releasing a blocking acquirer. However, no actual permit objects are used;
 * the Semaphore just keeps a count of the number available and acts accordingly.
 *
 * @author Aleks Seovic  2021.12.05
 * @since 21.12
 */
public interface Semaphore
    {
    /**
     * Acquires a permit from this semaphore, blocking until one is available,
     * or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until one of two
     * things happens:
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
    void acquire() throws InterruptedException;

    /**
     * Acquires a permit from this semaphore, blocking until one is available.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until some other
     * thread invokes the {@link #release} method for this semaphore and the
     * current thread is next to be assigned a permit.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it will continue to wait, but the time at
     * which the thread is assigned a permit may change compared to the time it
     * would have received the permit had no interruption occurred.  When the
     * thread does return from this method its interrupt status will be set.
     */
    void acquireUninterruptibly();

    /**
     * Acquires a permit from this semaphore, only if one is available at the
     * time of invocation.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true}, reducing the number of available permits by
     * one.
     *
     * <p>If no permit is available then this method will return
     * immediately with the value {@code false}.
     *
     * <p>Even when this semaphore has been set to use a
     * fair ordering policy, a call to {@code tryAcquire()} <em>will</em>
     * immediately acquire a permit if one is available, whether or not other
     * threads are currently waiting. This &quot;barging&quot; behavior can be
     * useful in certain circumstances, even though it breaks fairness. If you
     * want to honor the fairness setting, then use {@link #tryAcquire(long,
     * TimeUnit) tryAcquire(0, TimeUnit.SECONDS)} which is almost equivalent (it
     * also detects interruption).
     *
     * @return {@code true} if a permit was acquired and {@code false} otherwise
     */
    boolean tryAcquire();

    /**
     * Acquires a permit from this semaphore, if one becomes available within
     * the given waiting time and the current thread has not been {@linkplain
     * Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true}, reducing the number of available permits by
     * one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until one of
     * three things happens:
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
     * @param unit    the time unit of the {@code timeout} argument
     *
     * @return {@code true} if a permit was acquired and {@code false} if the
     * if the waiting time elapsed before a permit was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Releases a permit, returning it to the semaphore.
     *
     * <p>Releases a permit, increasing the number of available permits by
     * one.  If any threads are trying to acquire a permit, then one is selected
     * and given the permit that was just released.  That thread is (re)enabled
     * for thread scheduling purposes.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link #acquire}. Correct usage of a
     * semaphore is established by programming convention in the application.
     */
    void release();

    /**
     * Acquires the given number of permits from this semaphore, blocking until
     * all are available, or the thread is {@linkplain Thread#interrupt
     * interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits by the
     * given amount. This method has the same effect as the loop {@code for (int
     * i = 0; i < permits; ++i) acquire();} except that it atomically acquires
     * the permits all at once:
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until one of two
     * things happens:
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
     *
     * @throws InterruptedException     if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    void acquire(int permits) throws InterruptedException;

    /**
     * Acquires the given number of permits from this semaphore, blocking until
     * all are available.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits by the
     * given amount. This method has the same effect as the loop {@code for (int
     * i = 0; i < permits; ++i) acquireUninterruptibly();} except that it
     * atomically acquires the permits all at once:
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until some other
     * thread invokes one of the {@link #release() release} methods for this
     * semaphore and the current thread is next to be assigned permits and the
     * number of available permits satisfies this request.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for permits then it will continue to wait and its position
     * in the queue is not affected.  When the thread does return from this
     * method its interrupt status will be set.
     *
     * @param permits the number of permits to acquire
     *
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    void acquireUninterruptibly(int permits);

    /**
     * Acquires the given number of permits from this semaphore, only if all are
     * available at the time of invocation.
     *
     * <p>Acquires the given number of permits, if they are available, and
     * returns immediately, with the value {@code true}, reducing the number of
     * available permits by the given amount.
     *
     * <p>If insufficient permits are available then this method will return
     * immediately with the value {@code false} and the number of available
     * permits is unchanged.
     *
     * <p>Even when this semaphore has been set to use a fair ordering
     * policy, a call to {@code tryAcquire} <em>will</em> immediately acquire a
     * permit if one is available, whether or not other threads are currently
     * waiting.  This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor the
     * fairness setting, then use {@link #tryAcquire(int, long, TimeUnit)
     * tryAcquire(permits, 0, TimeUnit.SECONDS)} which is almost equivalent (it
     * also detects interruption).
     *
     * @param permits the number of permits to acquire
     *
     * @return {@code true} if the permits were acquired and {@code false}
     * otherwise
     *
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    boolean tryAcquire(int permits);

    /**
     * Acquires the given number of permits from this semaphore, if all become
     * available within the given waiting time and the current thread has not
     * been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available and
     * returns immediately, with the value {@code true}, reducing the number of
     * available permits by the given amount.
     *
     * <p>If insufficient permits are available then
     * the current thread becomes disabled for thread scheduling purposes and
     * lies dormant until one of three things happens:
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
     * @param unit    the time unit of the {@code timeout} argument
     *
     * @return {@code true} if all permits were acquired and {@code false} if
     * if the waiting time elapsed before all permits were acquired
     *
     * @throws InterruptedException     if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Releases the given number of permits, returning them to the semaphore.
     *
     * <p>Releases the given number of permits, increasing the number of
     * available permits by that amount. If any threads are trying to acquire
     * permits, then one thread is selected and given the permits that were just
     * released. If the number of available permits satisfies that thread's
     * request then that thread is (re)enabled for thread scheduling purposes;
     * otherwise the thread will wait until sufficient permits are available. If
     * there are still permits available after this thread's request has been
     * satisfied, then those permits are assigned in turn to other threads
     * trying to acquire permits.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link java.util.concurrent.Semaphore#acquire
     * acquire}. Correct usage of a semaphore is established by programming
     * convention in the application.
     *
     * @param permits the number of permits to release
     *
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    void release(int permits);

    /**
     * Returns the current number of permits available in this semaphore.
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the number of permits available in this semaphore
     */
    int availablePermits();

    /**
     * Acquires and returns all permits that are immediately available, or if
     * negative permits are available, releases them. Upon return, zero permits
     * are available.
     *
     * @return the number of permits acquired or, if negative, the number
     * released
     */
    int drainPermits();
    }
