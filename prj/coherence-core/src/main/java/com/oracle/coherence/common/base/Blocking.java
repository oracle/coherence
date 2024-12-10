/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * Blocking provides a set of helper methods related to blocking a thread.
 *
 * @author  mf 2015.02.24
 */
public class Blocking
    {
    // Note: the blocking helpers are written to minimize their expense when
    // they complete without timing out.  As such they all take the basic
    // approach of only checking for timeout *before* blocking, and truncating
    // the blocking time such that the blocking operation will complete when
    // timed out.  In such a case the blocking operation will not throw an
    // InterruptedException, but any subsequent blocking helper would immediately
    // detect the timeout and interrupt the thread, which would then cause its
    // blocking operation to throw InterruptedException (if appropriate). The
    // benefit of this approach is that it avoids both unnecessary conditional
    // logic, and testing/clearing the Thread's interrupt flag.  Deferring the
    // interrupt until the subsequent blocking operation is also legal as the
    // original blocking operation simply appears to have completed spuriously.

    /**
     * Return true if the thread is interrupted or {@link Timeout timed out}.
     *
     * Note as with Thread.interrupted this will clear the interrupted flag if
     * it is set, it will not however clear the timeout.
     *
     * @return true if the thread is interrupted or {@link Timeout timed out}
     */
    public static boolean interrupted()
        {
        // Note: we must check both the timeout and the interrupt, as checking for timeout
        // sets the interrupt, we don't actually need to check the timeout result, just invoking
        // isTimedOut() followed by an unconditional call to Thread.interrupted is sufficient.
        Timeout.isTimedOut(); // will interrupt the thread if timed out
        return Thread.interrupted();
        }

    /**
     * Wait on the specified monitor while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oMonitor  the monitor to wait on
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void wait(Object oMonitor)
            throws InterruptedException
        {
        wait(oMonitor, 0, 0);
        }

    /**
     * Wait on the specified monitor while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oMonitor  the monitor to wait on
     * @param cMillis   the maximum number of milliseconds to wait
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void wait(Object oMonitor, long cMillis)
            throws InterruptedException

        {
        wait(oMonitor, cMillis, 0);
        }

    /**
     * Wait on the specified monitor while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oMonitor  the monitor to wait on
     * @param cMillis   the maximum number of milliseconds to wait
     * @param cNanos    the additional number of nanoseconds to wait
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void wait(Object oMonitor, long cMillis, int cNanos)
            throws InterruptedException
        {
        long cMillisBlock = Math.min(Timeout.remainingTimeoutMillis(), cMillis == 0 ? Long.MAX_VALUE : cMillis);
        oMonitor.wait(cMillisBlock == Long.MAX_VALUE ? 0 : cMillisBlock, cNanos);
        }

    /**
     * Invoke Thread.sleep() while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param cMillis  the maximum number of milliseconds to sleep
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void sleep(long cMillis)
            throws InterruptedException
        {
        sleep(cMillis, 0);
        }

    /**
     * Invoke Thread.sleep() while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param cMillis  the maximum number of milliseconds to sleep
     * @param cNanos   the additional number of nanoseconds to sleep
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void sleep(long cMillis, int cNanos)
            throws InterruptedException
        {
        Thread.sleep(Math.min(Timeout.remainingTimeoutMillis(), cMillis), cNanos);
        }

    /**
     * Invoke LockSupport.park() while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oBlocker  the blocker
     */
    public static void park(Object oBlocker)
        {
        parkNanos(oBlocker, Long.MAX_VALUE); // park of 0 is a no-op
        }

    /**
     * Invoke LockSupport.parkNanos() while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oBlocker  the blocker
     * @param cNanos    the maximum number of nanoseconds to park for
     */
    public static void parkNanos(Object oBlocker, long cNanos)
        {
        long cMillisTimeout = Timeout.remainingTimeoutMillis();
        long cNanosTimeout  = cMillisTimeout >= Long.MAX_VALUE / 1000000 ? Long.MAX_VALUE : cMillisTimeout * 1000000;
        if (cMillisTimeout == Long.MAX_VALUE && cNanos == Long.MAX_VALUE)
            {
            LockSupport.park(oBlocker); // common case no timeout
            }
        else
            {
            LockSupport.parkNanos(oBlocker, Math.min(cNanos, Math.min(cNanos, cNanosTimeout)));
            }
        }

    /**
     * Acquire a lock while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param lock  the lock to acquire
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void lockInterruptibly(Lock lock)
            throws InterruptedException
        {
        while (!tryLock(lock, Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {}
        }

    /**
     * Attempt to acquire a lock while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param lock  the lock to acquire
     * @param time  the maximum amount of time to try for
     * @param unit  the unit which time represents
     *
     * @return true iff the lock was acquired
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static boolean tryLock(Lock lock, long time, TimeUnit unit)
            throws InterruptedException
        {
        long cMillisTimeout = Timeout.remainingTimeoutMillis();
        long cMillis        = unit.toMillis(time);
        if (cMillis == 0)
            {
            // handle a non-zero timeout which is less then a milli
            return lock.tryLock(time, unit);
            }

        return lock.tryLock(Math.min(cMillisTimeout, cMillis), TimeUnit.MILLISECONDS);
        }

    /**
     * Await for the Condition to be signaled while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param cond  the condition to wait on
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void await(Condition cond)
            throws InterruptedException
        {
        await(cond, Long.MAX_VALUE);
        }

    /**
     * Await for the Condition to be signaled while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param cond    the condition to wait on
     * @param cNanos  the maximum amount of time to wait
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void await(Condition cond, long cNanos)
            throws InterruptedException
        {
        await(cond, cNanos, TimeUnit.NANOSECONDS);
        }

    /**
     * Await for the Condition to be signaled while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param cond  the condition to wait on
     * @param time  the maximum amount of time to wait
     * @param unit  the unit which time represents
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void await(Condition cond, long time, TimeUnit unit)
            throws InterruptedException
        {
        long cMillisTimeout = Timeout.remainingTimeoutMillis();
        long cMillis        = unit.toMillis(time);
        if (cMillis == 0)
            {
            // handle a non-zero timeout which is less then a milli
            cond.await(time, unit);
            }
        else
            {
            cond.await(Math.min(cMillisTimeout, cMillis), TimeUnit.MILLISECONDS);
            }
        }

    /**
     * Wait on the Selector while still respecting the calling thread's {@link Timeout timeout}.
     *
     * If the thread performing the select is interrupted, this method will return immediately and that thread's
     * interrupted status will be set.
     *
     * @param selector  the selector to wait on
     *
     * @return the number of keys, possibly zero, whose ready-operation sets were updated
     *
     * @throws IOException             if an I/O error occurs
     */
    public static int select(Selector selector)
            throws IOException
        {
        return select(selector, 0);
        }

    /**
     * Wait on the Selector while still respecting the calling thread's {@link Timeout timeout}.
     *
     * If the thread performing the select is interrupted, this method will return immediately and that thread's
     * interrupted status will be set.
     *
     * @param selector  the selector to wait on
     * @param cMillis   the maximum amount of time to wait
     *
     * @return the number of keys, possibly zero, whose ready-operation sets were updated
     *
     * @throws IOException             if an I/O error occurs
     */
    public static int select(Selector selector, long cMillis)
            throws IOException
        {
        long cMillisBlock = Math.min(Timeout.remainingTimeoutMillis(), cMillis == 0 ? Long.MAX_VALUE : cMillis);
        return selector.select(cMillisBlock == Long.MAX_VALUE ? 0 : cMillisBlock);
        }

    /**
     * Connect a socket while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param socket  the socket to connect
     * @param addr    the address to connect to
     *
     * @throws IOException in an IO error occurs
     */
    public static void connect(Socket socket, SocketAddress addr)
            throws IOException
        {
        connect(socket, addr, 0);
        }

    /**
     * Connect a socket within a given timeout while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param socket   the socket to connect
     * @param addr     the address to connect to
     * @param cMillis  the caller specified connect timeout
     *
     * @throws IOException in an IO error occurs
     */
    public static void connect(Socket socket, SocketAddress addr, int cMillis)
            throws IOException
        {
        long cMillisBlock = Math.min(Timeout.remainingTimeoutMillis(), cMillis == 0 ? Long.MAX_VALUE : cMillis);
        socket.connect(addr, cMillisBlock >= Integer.MAX_VALUE ? 0 : (int) cMillisBlock);
        }
    }
