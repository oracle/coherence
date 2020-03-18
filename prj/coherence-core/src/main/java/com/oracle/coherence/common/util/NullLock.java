/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;


/**
 * NullLock is a lock no-op Lock implementation.
 *
 * @author mf  2014.10.02
 */
public class NullLock<R>
        extends AutoLock<R>
    {
    /**
     * Construct a NullLock with no resource.
     */
    public NullLock()
        {
        this(null);
        }

    /**
     * Construct a NullLock with the specified resource.
     *
     * @param resource the resource
     */
    public NullLock(R resource)
        {
        super(null, resource);
        }

    @Override
    public Sentry<R> acquire()
        {
        return f_sentry;
        }

    @Override
    public void lock()
        {
        }

    @Override
    public void lockInterruptibly()
            throws InterruptedException
        {
        }

    @Override
    public boolean tryLock()
        {
        return true;
        }

    @Override
    public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException
        {
        return true;
        }

    @Override
    public void unlock()
        {
        }

    @Override
    public Condition newCondition()
        {
        return new Condition()
            {
            @Override
            public void await()
                    throws InterruptedException
                {
                throw new UnsupportedOperationException();
                }

            @Override
            public void awaitUninterruptibly()
                {
                throw new UnsupportedOperationException();
                }

            @Override
            public long awaitNanos(long nanosTimeout)
                    throws InterruptedException
                {
                throw new UnsupportedOperationException();
                }

            @Override
            public boolean await(long time, TimeUnit unit)
                    throws InterruptedException
                {
                throw new UnsupportedOperationException();
                }

            @Override
            public boolean awaitUntil(Date deadline)
                    throws InterruptedException
                {
                throw new UnsupportedOperationException();
                }

            @Override
            public void signal()
                {
                }

            @Override
            public void signalAll()
                {
                }
            };
        }

    /**
     * Singleton instance.
     */
    public static final NullLock INSTANCE = new NullLock();
    }
