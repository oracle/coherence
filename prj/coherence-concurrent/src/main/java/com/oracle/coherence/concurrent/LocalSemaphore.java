/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Local implementation of {@link com.oracle.coherence.concurrent.Semaphore}
 * interface, that simply wraps {@code java.util.concurrent.Semaphore} instance.
 *
 * @author Aleks Seovic  2021.12.05
 */
public class LocalSemaphore
        implements com.oracle.coherence.concurrent.Semaphore
    {
    /**
     * Create a {@code LocalSemaphore} with the given number of permits.
     *
     * @param cPermits  the initial number of permits available;
     *                  this value may be negative, in which case releases
     *                  must occur before any acquires will be granted
     */
    LocalSemaphore(int cPermits)
        {
        f_semaphore = new Semaphore(cPermits);
        }

    // ---- Semaphore interface ---------------------------------------------

    @Override
    public void acquire() throws InterruptedException
        {
        f_semaphore.acquire();
        }

    @Override
    public void acquireUninterruptibly()
        {
        f_semaphore.acquireUninterruptibly();
        }

    @Override
    public boolean tryAcquire()
        {
        return f_semaphore.tryAcquire();
        }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException
        {
        return f_semaphore.tryAcquire(timeout, unit);
        }

    @Override
    public void release()
        {
        f_semaphore.release();
        }

    @Override
    public void acquire(int permits) throws InterruptedException
        {
        f_semaphore.acquire(permits);
        }

    @Override
    public void acquireUninterruptibly(int permits)
        {
        f_semaphore.acquireUninterruptibly(permits);
        }

    @Override
    public boolean tryAcquire(int permits)
        {
        return f_semaphore.tryAcquire(permits);
        }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException
        {
        return f_semaphore.tryAcquire(permits, timeout, unit);
        }

    @Override
    public void release(int permits)
        {
        f_semaphore.release(permits);
        }

    @Override
    public int availablePermits()
        {
        return f_semaphore.availablePermits();
        }

    @Override
    public int drainPermits()
        {
        return f_semaphore.drainPermits();
        }

    // ---- Object methods --------------------------------------------------

    /**
     * Returns a string identifying this semaphore, as well as its state.
     * The state, in brackets, includes the String {@code "Permits ="}
     * followed by the number of permits.
     *
     * @return a string identifying this semaphore, as well as its state
     */
    public String toString()
        {
        return f_semaphore.toString();
        }

    // ---- data members ----------------------------------------------------

    /**
     * The {@code Semaphore} to delegate to.
     */
    private final Semaphore f_semaphore;
    }
