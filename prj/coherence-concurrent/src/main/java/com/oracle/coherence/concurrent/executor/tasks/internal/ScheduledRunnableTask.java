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

/**
 * A {@link Task} that calls a {@link Runnable} at scheduled time.
 *
 * @author lh
 * @since 21.12
 */
public class ScheduledRunnableTask
        extends RunnableTask
        implements Runnable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ScheduledRunnableTask} (required for serialization).
     */
    @SuppressWarnings("unused")
    public ScheduledRunnableTask()
        {
        }

    /**
     * Constructs a {@link RunnableTask}.
     *
     * @param runnable      the runnable
     * @param initialDelay  the initial delay
     * @param period        the period between successive execution
     * @param delay         the delay to start the next execution after the completion
     *                      of the current execution
     */
    public ScheduledRunnableTask(Runnable runnable, Duration initialDelay, Duration period, Duration delay)
        {
        super(runnable);

        m_ldtExecuteNanos = System.nanoTime() + (initialDelay == null ? 0 : initialDelay.toNanos());
        m_initialDelay    = initialDelay;
        m_period          = period;
        m_delay           = delay;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the initial delay.
     *
     * @return the initial delay
     */
    public Duration getInitialDelay()
        {
        return m_initialDelay;
        }

    /**
     * Returns the period.
     *
     * @return the period
     */
    public Duration getPeriod()
        {
        return m_period;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public Boolean execute(Context<Boolean> context) throws Exception
        {
        long currentNanos = System.nanoTime();
        if (m_ldtExecuteNanos > currentNanos)
            {
            throw Yield.atLeast(Duration.ofNanos(m_ldtExecuteNanos - currentNanos));
            }
        else if (m_period != null)
            {
            m_ldtExecuteNanos = currentNanos + m_period.toNanos();
            }

        getRunnable().run();

        if (m_period != null)
            {
            long cWaitNanos = m_ldtExecuteNanos - System.nanoTime();
            if (cWaitNanos > 0)
                {
                throw Yield.atLeast(Duration.ofNanos(cWaitNanos));
                }
            else
                {
                throw Yield.atLeast(Duration.ZERO);
                }
            }
        else if (m_delay != null)
            {
            m_ldtExecuteNanos = System.nanoTime() + m_delay.toNanos();
            throw Yield.atLeast(m_delay);
            }

        return true;
        }

    // ----- Runnable interface ---------------------------------------------

    @Override
    public void run()
        {
        throw new UnsupportedOperationException();
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        super.readExternal(in);
        long seconds = ExternalizableHelper.readLong(in);
        if (seconds > 0)
            {
            m_delay = Duration.ofSeconds(seconds);
            }

        m_ldtExecuteNanos = ExternalizableHelper.readLong(in);
        seconds           = ExternalizableHelper.readLong(in);
        if (seconds > 0)
            {
            m_initialDelay = Duration.ofSeconds(seconds);
            }

        seconds = ExternalizableHelper.readLong(in);
        if (seconds > 0)
            {
            m_period = Duration.ofSeconds(seconds);
            }
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        super.writeExternal(out);
        ExternalizableHelper.writeLong(out, m_delay == null ? 0 : m_delay.getSeconds());
        ExternalizableHelper.writeLong(out, m_ldtExecuteNanos);
        ExternalizableHelper.writeLong(out, m_initialDelay == null ? 0 : m_initialDelay.getSeconds());
        ExternalizableHelper.writeLong(out, m_period == null ? 0 : m_period.getSeconds());
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        super.readExternal(in);
        long seconds = in.readLong(1);
        if (seconds > 0)
            {
            m_delay = Duration.ofSeconds(seconds);
            }

        m_ldtExecuteNanos = in.readLong(2);
        seconds           = in.readLong(3);
        if (seconds > 0)
            {
            m_initialDelay = Duration.ofSeconds(seconds);
            }

        seconds = in.readLong(4);
        if (seconds > 0)
            {
            m_period = Duration.ofSeconds(seconds);
            }
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        super.writeExternal(out);
        out.writeLong(1, m_delay == null ? 0 : m_delay.getSeconds());
        out.writeLong(2, m_ldtExecuteNanos);
        out.writeLong(3, m_initialDelay == null ? 0 : m_initialDelay.getSeconds());
        out.writeLong(4, m_period == null ? 0 : m_period.getSeconds());
        }

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        return "ScheduledRunnableTask{" +
               "runnable=" + m_runnable +
               ", initial-delay=" + m_initialDelay +
               ", next-execution-time-nanos=" + m_ldtExecuteNanos +
               ", execution-period=" + m_period +
               ", execution-delay=" + m_delay +
               '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The initial delay to execute the task.
     */
    protected Duration m_initialDelay;

    /**
     * The time in nanoseconds to execute the task.
     */
    protected long m_ldtExecuteNanos;

    /**
     * The period between successive executions.
     */
    protected Duration m_period;

    /**
     * The delay to start the next execution after the completion of the current execution.
     */
    protected Duration m_delay;
    }
