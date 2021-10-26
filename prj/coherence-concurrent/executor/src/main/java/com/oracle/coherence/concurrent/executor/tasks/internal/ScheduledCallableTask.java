/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.tasks.internal;

import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import java.time.Duration;

import java.util.concurrent.Callable;

/**
 * A {@link Task} that executes a {@link Callable} at scheduled time.
 *
 * @param <T> {@inheritDoc}
 *
 * @author lh
 * @since 21.12
 */
public class ScheduledCallableTask<T>
        extends CallableTask<T>
        implements Callable<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ScheduledCallableTask} (required for Serializable).
     */
    @SuppressWarnings("unused")
    public ScheduledCallableTask()
        {
        }

    /**
     * Constructs a {@link ScheduledCallableTask}.
     *
     * @param callable      the callable
     * @param initialDelay  the initial delay to execute the task
     */
    public ScheduledCallableTask(Callable callable, Duration initialDelay)
        {
        super(callable);

        m_ldtSubmitNanos  = System.nanoTime();
        m_ltdInitialDelay = initialDelay;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the initial delay.
     *
     * @return the initial delay
     */
    public Duration getInitialDelay()
        {
        return m_ltdInitialDelay;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public T execute(Context<T> context) throws Exception
        {
        long currentNanos = System.nanoTime();
        long executeNanos = m_ldtSubmitNanos + (m_ltdInitialDelay == null ? 0L : m_ltdInitialDelay.toNanos());

        if (executeNanos > currentNanos)
            {
            throw Yield.atLeast(Duration.ofNanos(executeNanos - currentNanos));
            }

        return getCallable().call();
        }

    // ------- Callable interface -------------------------------------

    @Override
    public T call()
        {
        throw new UnsupportedOperationException();
        }

    // ------- PortableObject interface -------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        super.readExternal(in);
        m_ldtSubmitNanos = in.readLong(1);
        long seconds = in.readLong(2);
        if (seconds > 0)
            {
            m_ltdInitialDelay = Duration.ofSeconds(seconds);
            }
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        super.writeExternal(out);
        out.writeLong(1, m_ldtSubmitNanos);
        out.writeLong(2, m_ltdInitialDelay == null ? 0 : m_ltdInitialDelay.getSeconds());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The time in nanoseconds when the task is submitted.
     */
    protected long m_ldtSubmitNanos;

    /**
     * The initial delay to execute the task.
     */
    protected Duration m_ltdInitialDelay;
    }
