/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.time.Duration;
import java.time.Instant;

/**
 * A {@link Task} that runs for at least a specified {@link Duration},
 * returning the resuming / recovering status when completed.
 *
 * @since 21.12
 */
public class RecoveringTask
        implements Task<Boolean>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link RecoveringTask} (required for serialization).
     */
    @SuppressWarnings("unused")
    public RecoveringTask()
        {
        }

    /**
     * Constructs a {@link RecoveringTask}.
     *
     * @param duration  the {@link Duration} to run the {@link Task}
     */
    public RecoveringTask(Duration duration)
        {
        m_duration = duration;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public Boolean execute(Context<Boolean> context)
            throws Exception
        {
        // the instant we started the task
        Instant startedInstant = Instant.now();

        Integer nStart          = context.getProperties().get("count");
        int     cIterationCount = nStart == null ? 0 : nStart;

        while (Instant.now().isBefore(startedInstant.plus(m_duration)) &&
               !Thread.currentThread().isInterrupted())
            {
            cIterationCount++;

            context.getProperties().put("count", cIterationCount);

            // TODO: Remove several System.out.printlns here; review for logging
            try
                {
                Blocking.sleep(1000);
                }
            catch (InterruptedException e)
                {
                e.printStackTrace();
                break;
                }
            }

        return context.isResuming();
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_duration = Duration.ofSeconds(ExternalizableHelper.readLong(in));
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeLong(out, m_duration.getSeconds());
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_duration = Duration.ofSeconds(in.readLong(0));
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_duration.getSeconds());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Duration} for which to run the {@link Task}.
     */
    protected Duration m_duration;
    }
