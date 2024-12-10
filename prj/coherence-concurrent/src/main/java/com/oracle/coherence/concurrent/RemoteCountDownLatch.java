/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

import com.oracle.coherence.concurrent.internal.LatchCounter;

import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;

import com.tangosol.util.MapEvent;

import com.tangosol.util.Processors;

import com.tangosol.util.listener.SimpleMapListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A distributed count down latch with the same basic behavior and semantics
 * as the Java {@link CountDownLatch} class, but uses the {@link NamedCache}
 * to support for synchronization aid that allows one or more threads across
 * multiple cluster members to wait until a set of operations being performed
 * in other threads/members completes.
 *
 * @author as, lh  2021.11.16
 * @since 21.12
 */
public class RemoteCountDownLatch
        implements com.oracle.coherence.concurrent.CountDownLatch
    {
    /**
     * Constructs a {@code RemoteCountDownLatch} initialized with the
     * given count.
     *
     * @param sName    the name of the latch
     * @param count    the number of times {@link #countDown} must be invoked
     *                 before threads can pass through {@link #await}
     * @param latches  the {@link NamedMap} that stores this latch's state
     *
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public RemoteCountDownLatch(String sName, int count, NamedMap<String, LatchCounter> latches)
        {
        if (count < 0)
            {
            throw new IllegalArgumentException("count < 0");
            }

        f_sync         = new RemoteCountDownLatch.Sync(sName, latches);
        f_initialCount = count;
        latches.addMapListener(new SimpleMapListener<String, LatchCounter>()
                        .addDeleteHandler(this::onDelete), sName, false);
        }

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>If the current count is zero then this method returns immediately.
     *
     * <p>If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the
     * {@link #countDown} method; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void await() throws InterruptedException
        {
        f_sync.acquireSharedInterruptibly(1);
        }

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is {@linkplain Thread#interrupt interrupted},
     * or the specified waiting time elapses.
     *
     * <p>If the current count is zero then this method returns immediately
     * with the value {@code true}.
     *
     * <p>If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the
     * {@link #countDown} method; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the count reaches zero then the method returns with the
     * value {@code true}.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout  the maximum time to wait
     * @param unit     the time unit of the {@code timeout} argument
     *
     * @return {@code true} if the count reached zero and {@code false}
     *         if the waiting time elapsed before the count reached zero
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException
        {
        return f_sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

    /**
     * Decrements the latch count, releasing all waiting threads if
     * the count reaches zero.
     *
     * <p>If the current count is greater than zero then it is decremented.
     * If the new count is zero then all waiting threads are re-enabled for
     * thread scheduling purposes.
     *
     * <p>If the current count equals zero then nothing happens.
     */
    public void countDown()
        {
        f_sync.releaseShared(1);
        }

    /**
     * Returns the current count.
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the current count
     */
    public long getCount()
        {
        return f_sync.getCount();
        }

    /**
     * Returns the initial count of the latch.
     *
     * @return the initial count
     */
    public long getInitialCount()
        {
        return f_initialCount;
        }

    /**
     * Returns a string identifying this latch, as well as its state.
     *
     * @return a string identifying this latch, as well as its state
     */
    public String toString()
        {
        return "RemoteCountDownLatch{count = " + f_sync.getCount()
                + ", initialCount = " + f_initialCount + '}';
        }

    /**
     * It is called by the delete event handler to release all waiting threads
     * on the local member if the count reaches zero.  If the current count is
     * greater than zero then nothing happens.
     *
     * It passes 0 for releases argument to indicate no countdown on the latch.
     */
    protected void signalLocalThreads()
        {
        f_sync.releaseShared(0);
        }

    /**
     * Event handler for delete event.  If the local latch map contains
     * the latch being deleted, then delete it and signal the local latch
     * to release all waiting threads on this member.
     *
     * @param event  the delete event
     */
    private void onDelete(MapEvent<? extends String, ? extends LatchCounter> event)
        {
        String sName = event.getKey();

        Latches.removeCountDownLatch(sName);
        this.signalLocalThreads();
        }

    // ----- inner class Sync -----------------------------------------------

    /**
     * Synchronization control For RemoteCountDownLatch.
     */
    private static final class Sync
            extends AbstractQueuedSynchronizer
        {
        /**
         * Construct a {@code Sync} instance.
         *
         * @param sName    the name of the countdown latch
         * @param latches  the {@link NamedMap} that stores this latch's state
         */
        Sync(String sName, NamedMap<String, LatchCounter> latches)
            {
            f_sName   = sName;
            f_latches = latches;
            }

        /**
         * Return the current latch count.  For debug and testing.
         *
         * @return  the current latch count
         */
        public long getCount()
            {
            return f_latches.invoke(f_sName, e -> e.isPresent() ? e.getValue().getCount() : 0L);
            }

        @Override
        protected int tryAcquireShared(int acquires)
            {
            if (f_latches.compute(f_sName, (k, v) ->
                    {
                    if (v == null || v.getCount() == 0L)
                        {
                        return null;
                        }

                    return v;
                    }) == null)
                {
                return 1;
                }

            return -1;
            }

        @Override
        protected boolean tryReleaseShared(int releases)
            {
            int count = f_latches.invoke(f_sName, entry ->
                {
                if (entry.isPresent())
                    {
                    LatchCounter value        = entry.getValue();
                    long         currentCount = value.getCount();

                    if (releases == 0)
                        {
                        // release only; no countdown
                        return currentCount;
                        }

                    if (currentCount == 1)
                        {
                        entry.remove(false);
                        }
                    else
                        {
                        value.countDown();
                        entry.setValue(value);
                        }

                    return currentCount;
                    }

                return 0;
                }).intValue();

            if (releases == 0)
                {
                // release only
                return count == 0;
                }

            return count == 1;
            }

        /**
         * The name of the distributed countdown latch; used as a key in the
         * NamedMap containing the latches.
         */
        private final String f_sName;

        /**
         * The NamedMap containing the remote countdown latches.
         */
        private final NamedMap<String, LatchCounter> f_latches;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Synchronizer providing all implementation mechanics
     */
    private final Sync f_sync;

    /**
     * Initial count.
     */
    private final long f_initialCount;
    }