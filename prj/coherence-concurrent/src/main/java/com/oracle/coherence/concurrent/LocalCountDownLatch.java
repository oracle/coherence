/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Local implementation of {@link com.oracle.coherence.concurrent.CountDownLatch}
 * interface, that simply wraps {@code java.util.concurrent.CountDownLatch} instance.
 *
 * @author Aleks Seovic  2021.12.05
 */
public class LocalCountDownLatch
        implements com.oracle.coherence.concurrent.CountDownLatch
    {
    /**
     * Construct a {@code LocalCountDownLatch} initialized with the given count.
     *
     * @param count  the number of times {@link #countDown} must be invoked
     *               before threads can pass through {@link #await}
     *
     * @throws IllegalArgumentException if {@code count} is negative
     */
    LocalCountDownLatch(int count)
        {
        f_latch = new CountDownLatch(count);
        }

    // ---- CountDownLatch interface ----------------------------------------

    @Override
    public void await() throws InterruptedException
        {
        f_latch.await();
        }

    @Override
    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException
        {
        return f_latch.await(timeout, unit);
        }

    @Override
    public void countDown()
        {
        f_latch.countDown();
        }

    @Override
    public long getCount()
        {
        return f_latch.getCount();
        }

    // ---- Object methods --------------------------------------------------

    /**
     * Returns a string identifying this latch, as well as its state.
     * The state, in brackets, includes the String {@code "Count ="}
     * followed by the current count.
     *
     * @return a string identifying this latch, as well as its state
     */
    @Override
    public String toString()
        {
        return f_latch.toString();
        }

    // ---- data members ----------------------------------------------------

    /**
     * The {@code CountDownLatch} to delegate to.
     */
    private final CountDownLatch f_latch;
    }
