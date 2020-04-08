/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import com.oracle.coherence.common.base.Blocking;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;


/**
 * A wrapper lock which supports AutoCloseable via {@link #acquire} and thus supports try with resources.
 * Additionally the AutoLock and Sentry can contain knowledge of the entity which they protect, this is
 * obtainable via {@link #getResource()}.
 * <p>
 * Example usage:
 * <p><code> try (Sentry&lt;Foo&gt; sentry = f_lock.acquire()) { Foo foo = sentry.getResource(); ... } </code></p>
 *
 * Note: the AutoLock is {@link com.oracle.coherence.common.base.Timeout Timeout} compatible.
 *
 * @author mf 2014.10.02
 */
public class AutoLock<R>
    implements Lock
    {
    /**
     * Construct an AutoLock around the specified delegate.
     *
     * The lock status of the delegate will not be changed as part of this operation.
     *
     * @param delegate the delegate
     */
    public AutoLock(Lock delegate)
        {
        this(delegate, null);
        }

    /**
     * Construct an AutoLock around the specified delegate.
     *
     * The lock status of the delegate will not be changed as part of this operation.
     *
     * @param delegate the delegate
     * @param resource the associated resource
     */
    public AutoLock(Lock delegate, R resource)
        {
        f_lockDelegate = delegate;
        f_resource     = resource;
        }

    /**
     * Acquire the lock and return the auto-closeable Sentry.
     *
     * @return the auto-closeable Sentry which will unlock on close
     */
    public Sentry<R> acquire()
        {
        f_lockDelegate.lock();
        return f_sentry;
        }

    /**
     * Return the resource associated with the lock.
     *
     * @return the resource
     */
    public R getResource()
        {
        return f_resource;
        }

    @Override
    public void lock()
        {
        f_lockDelegate.lock();
        }

    @Override
    public void lockInterruptibly()
            throws InterruptedException
        {
        Blocking.lockInterruptibly(f_lockDelegate);
        }

    @Override
    public boolean tryLock()
        {
        return f_lockDelegate.tryLock();
        }

    @Override
    public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException
        {
        return Blocking.tryLock(f_lockDelegate, time, unit);
        }

    @Override
    public void unlock()
        {
        f_lockDelegate.unlock();
        }

    @Override
    public Condition newCondition()
        {
        return f_lockDelegate.newCondition();
        }

    // ----- inner class: Sentry ------------------------------------------

    /**
     * AutoCloseable which unlocks this lock.
     */
    public static class Sentry<V>
            implements com.oracle.coherence.common.util.Sentry<V>
        {
        /**
         * Construct a Sentry for a given AutoLock.
         *
         * @param parent the parent
         */
        protected Sentry(AutoLock<V> parent)
            {
            f_parent = parent;
            }

        @Override
        public void close()
            {
            f_parent.unlock();
            }

        /**
         * Return the resource associated with the lock.
         *
         * @return the resource.
         */
        public V getResource()
            {
            return f_parent.getResource();
            }

        /**
         * The associated lock.
         */
        protected final AutoLock<V> f_parent;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The delegate lock.
     */
    protected Lock f_lockDelegate;

    /**
     * The associated resource.
     */
    protected final R f_resource;

    /**
     * The associated Sentry.
     */
    protected final Sentry<R> f_sentry = new Sentry<>(this);
    }
