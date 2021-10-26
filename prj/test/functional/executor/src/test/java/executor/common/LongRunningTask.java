/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.coherence.concurrent.executor.PortableTask;
import com.oracle.coherence.concurrent.executor.Task;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import java.time.Duration;
import java.time.Instant;

/**
 * A {@link PortableTask} that runs for at least a specified {@link Duration}.
 *
 * @since 21.12
 */
public class LongRunningTask
        implements PortableTask<String>
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

    // ----- PortableTask interface -----------------------------------------

    @Override
    public String execute(Context<String> context)
        {
        System.out.println("LongRunningTask, " + m_nId + ": Resuming? : " + context.isResuming());
        System.out.println("LongRunningTask, " + m_nId + ": Done?     : " + context.isDone());

        // the instant we started the task
        Instant startedInstant = Instant.now();
        Integer cStart         = context.getProperties().get("count");

        int cIterationCount = cStart == null ? 0 : cStart;

        while (Instant.now().isBefore(startedInstant.plus(m_duration)) && !Thread.currentThread().isInterrupted())
            {
            cIterationCount++;

            context.getProperties().put("count", cIterationCount);

            System.out.println("LongRunningTask, " + m_nId + ": Iteration : " + cIterationCount);

            try
                {
                System.out.println("LongRunningTask: Sleeping");
                //noinspection BusyWait
                Thread.sleep(1000);
                }
            catch (InterruptedException e)
                {
                System.out.println("LongRunningTask: Interrupted");

                e.printStackTrace();
                break;
                }
            }

        return "DONE";
        }

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
     * The {@link PortableTask} ID.
     */
    protected int m_nId;
    }
