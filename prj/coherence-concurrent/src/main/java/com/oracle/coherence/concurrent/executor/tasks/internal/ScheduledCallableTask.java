/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.tasks.internal;

import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.time.Duration;

import java.util.concurrent.Callable;

/**
 * A {@link Task} that executes a {@link Callable} at scheduled time.
 *
 * @param <T>  the type of result produced by the {@link Task}
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
     * Constructs a {@link ScheduledCallableTask} (required for serialization).
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
    public ScheduledCallableTask(Callable<T> callable, Duration initialDelay)
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

    // ----- Callable interface ---------------------------------------------

    @Override
    public T call()
        {
        throw new UnsupportedOperationException();
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        super.readExternal(in);
        m_ldtSubmitNanos = ExternalizableHelper.readLong(in);
        long seconds     = ExternalizableHelper.readLong(in);
        if (seconds > 0)
            {
            m_ltdInitialDelay = Duration.ofSeconds(seconds);
            }
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        super.writeExternal(out);
        ExternalizableHelper.writeLong(out, m_ldtSubmitNanos);
        ExternalizableHelper.writeLong(out, m_ltdInitialDelay == null ? 0 : m_ltdInitialDelay.getSeconds());
        }

    // ----- PortableObject interface ---------------------------------------

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

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        return "ScheduledCallableTask{" +
               "callable=" + m_callable +
               ", execution-time-nanos=" + m_ldtSubmitNanos +
               ", initial-delay=" + m_ltdInitialDelay +
               '}';
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
