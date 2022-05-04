/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.Task;

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
 * A {@link Task} that runs for at least a specified {@link Duration}.
 *
 * @since 21.12
 */
public class LongRunningTask
        implements Task<String>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /*
     * Constructs a {@link LongRunningTask} (required for Serializable)
     */
    @SuppressWarnings("unused")
    public LongRunningTask()
        {
        }

    /**
     * Constructs a {@link LongRunningTask}.
     *
     * @param duration the {@link Duration} to run the {@link Task}
     */
    public LongRunningTask(Duration duration)
        {
        m_duration = duration;
        m_nId      = 0;
        }

    /**
     * Constructs a {@link LongRunningTask}.
     *
     * @param duration the {@link Duration} to run the {@link Task}
     * @param id       the {@link Task} ID
     */
    public LongRunningTask(Duration duration, int id)
        {
        m_duration = duration;
        m_nId      = id;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public String execute(Context<String> context)
        {
        Logger.info("LongRunningTask, " + m_nId + ": Resuming? : " + context.isResuming());
        Logger.info("LongRunningTask, " + m_nId + ": Done?     : " + context.isDone());

        // the instant we started the task
        Instant startedInstant = Instant.now();
        Integer cStart         = context.getProperties().get("count");
        Instant offsetInstance = startedInstant.plus(m_duration);

        int cIterationCount = cStart == null ? 0 : cStart;

        while (Instant.now().isBefore(offsetInstance) && !Thread.currentThread().isInterrupted())
            {
            cIterationCount++;

            context.getProperties().put("count", cIterationCount);

            Logger.info("LongRunningTask, " + m_nId + ": Iteration : " + cIterationCount);

            try
                {
                Logger.info("LongRunningTask: Sleeping");
                //noinspection BusyWait
                Thread.sleep(1000);
                }
            catch (InterruptedException e)
                {
                Logger.info("LongRunningTask: Interrupted");

                e.printStackTrace();
                break;
                }
            }
        Logger.info("LongRunningTask: Completed; returning DONE");
        return "DONE";
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_duration = Duration.ofSeconds(ExternalizableHelper.readLong(in));
        m_nId      = ExternalizableHelper.readInt(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeLong(out, m_duration.getSeconds());
        ExternalizableHelper.writeInt(out, m_nId);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_duration = Duration.ofSeconds(in.readLong(0));
        m_nId      = in.readInt(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_duration.getSeconds());
        out.writeInt(1, m_nId);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Duration} for which to run the {@link Task}.
     */
    protected Duration m_duration;

    /**
     * The {@link Task} ID.
     */
    protected int m_nId;
    }
