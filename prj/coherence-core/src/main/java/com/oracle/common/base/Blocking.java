/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.Socket;
import java.net.SocketAddress;

import java.nio.channels.Selector;

/**
 * Blocking provides a set of helper methods related to blocking a thread.
 *
 * @author  mf 2015.02.24
 * @deprecated use {@link com.oracle.coherence.common.base.Blocking} instead
 */
@Deprecated
public class Blocking
        extends com.oracle.coherence.common.base.Blocking
    {
    /**
     * Return true if the thread is interrupted or {@link Timeout timed out}.
     *
     * Note as with Thread.interrupted this will clear the interrupted flag if
     * it is set, it will not however clear the timeout.
     *
     * @return true if the thread is interrupted or {@link Timeout timed out}
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#interrupted()}
     *             instead
     */
    public static boolean interrupted()
        {
        return com.oracle.coherence.common.base.Blocking.interrupted();
        }

    /**
     * Wait on the the specified monitor while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oMonitor  the monitor to wait on
     *
     * @throws InterruptedException if the thread is interrupted
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#wait(Object)}
     *             instead
     */
    public static void wait(Object oMonitor)
            throws InterruptedException
        {
        com.oracle.coherence.common.base.Blocking.wait(oMonitor);
        }

    /**
     * Wait on the specified monitor while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oMonitor  the monitor to wait on
     * @param cMillis   the maximum number of milliseconds to wait
     *
     * @throws InterruptedException if the thread is interrupted
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#wait(Object, long)}
     *             instead
     */
    public static void wait(Object oMonitor, long cMillis)
            throws InterruptedException

        {
        com.oracle.coherence.common.base.Blocking.wait(oMonitor, cMillis);
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
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#wait(Object, long, int)}
     *             instead
     */
    public static void wait(Object oMonitor, long cMillis, int cNanos)
            throws InterruptedException
        {
        com.oracle.coherence.common.base.Blocking.wait(oMonitor, cMillis, cNanos);
        }

    /**
     * Invoke Thread.sleep() while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param cMillis  the maximum number of milliseconds to sleep
     *
     * @throws InterruptedException if the thread is interrupted
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#sleep(long)}
     *             instead
     */
    public static void sleep(long cMillis)
            throws InterruptedException
        {
        com.oracle.coherence.common.base.Blocking.sleep(cMillis);
        }

    /**
     * Invoke Thread.sleep() while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param cMillis  the maximum number of milliseconds to sleep
     * @param cNanos   the additional number of nanoseconds to sleep
     *
     * @throws InterruptedException if the thread is interrupted
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#sleep(long, int)}
     *             instead
     */
    public static void sleep(long cMillis, int cNanos)
            throws InterruptedException
        {
        com.oracle.coherence.common.base.Blocking.sleep(cMillis, cNanos);
        }

    /**
     * Invoke LockSupport.park() while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oBlocker  the blocker
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#park(Object)}
     *             instead
     */
    public static void park(Object oBlocker)
        {
        com.oracle.coherence.common.base.Blocking.park(oBlocker);
        }

    /**
     * Invoke LockSupport.parkNanos() while still respecting the calling
     * thread's {@link Timeout timeout}.
     *
     * @param oBlocker  the blocker
     * @param cNanos    the maximum number of nanoseconds to park for
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#parkNanos(Object, long)}
     *             instead
     */
    public static void parkNanos(Object oBlocker, long cNanos)
        {
        com.oracle.coherence.common.base.Blocking.parkNanos(oBlocker, cNanos);
        }

    /**
     * Acquire a lock while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param lock  the lock to acquire
     *
     * @throws InterruptedException if the thread is interrupted
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#lockInterruptibly(Lock)}
     *             instead
     */
    public static void lockInterruptibly(Lock lock)
            throws InterruptedException
        {
        com.oracle.coherence.common.base.Blocking.lockInterruptibly(lock);
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
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#tryLock(Lock, long, TimeUnit)}
     *             instead
     */
    public static boolean tryLock(Lock lock, long time, TimeUnit unit)
            throws InterruptedException
        {
        return com.oracle.coherence.common.base.Blocking.tryLock(lock, time, unit);
        }

    /**
     * Await for the Condition to be signaled while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param cond  the condition to wait on
     *
     * @throws InterruptedException if the thread is interrupted
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#await(Condition)}
     *             instead
     */
    public static void await(Condition cond)
            throws InterruptedException
        {
        com.oracle.coherence.common.base.Blocking.await(cond);
        }

    /**
     * Await for the Condition to be signaled while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param cond    the condition to wait on
     * @param cNanos  the maximum amount of time to wait
     *
     * @throws InterruptedException if the thread is interrupted
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#await(Condition, long)}
     *             instead
     */
    public static void await(Condition cond, long cNanos)
            throws InterruptedException
        {
        com.oracle.coherence.common.base.Blocking.await(cond, cNanos);
        }

    /**
     * Await for the Condition to be signaled while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param cond  the condition to wait on
     * @param time  the maximum amount of time to wait
     * @param unit  the unit which time represents
     *
     * @throws InterruptedException if the thread is interrupted
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#await(Condition, long, TimeUnit)}
     *             instead
     */
    public static void await(Condition cond, long time, TimeUnit unit)
            throws InterruptedException
        {
        com.oracle.coherence.common.base.Blocking.await(cond, time, unit);
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
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#select(Selector)}
     *             instead
     */
    public static int select(Selector selector)
            throws IOException
        {
        return com.oracle.coherence.common.base.Blocking.select(selector);
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
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#select(Selector, long)}
     *             instead
     */
    public static int select(Selector selector, long cMillis)
            throws IOException
        {
        return com.oracle.coherence.common.base.Blocking.select(selector, cMillis);
        }

    /**
     * Connect a socket while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param socket  the socket to connect
     * @param addr    the address to connect to
     *
     * @throws IOException in an IO error occurs
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#connect(Socket, SocketAddress)}
     *             instead
     */
    public static void connect(Socket socket, SocketAddress addr)
            throws IOException
        {
        com.oracle.coherence.common.base.Blocking.connect(socket, addr);
        }

    /**
     * Connect a socket within a given timeout while still respecting the calling thread's {@link Timeout timeout}.
     *
     * @param socket   the socket to connect
     * @param addr     the address to connect to
     * @param cMillis  the caller specified connect timeout
     *
     * @throws IOException in an IO error occurs
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Blocking#connect(Socket, SocketAddress, int)}
     *             instead
     */
    public static void connect(Socket socket, SocketAddress addr, int cMillis)
            throws IOException
        {
        com.oracle.coherence.common.base.Blocking.connect(socket, addr, cMillis);
        }
    }
