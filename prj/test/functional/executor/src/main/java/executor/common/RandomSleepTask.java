/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Random;

/**
 * A {@link Task} that will sleep a random number of seconds (up to some maximum) before returning the current
 * system time (in milliseconds)
 *
 * @since 21.12
 */
public class RandomSleepTask
        implements Task<Long>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /*
     * Constructs a {@link RandomSleepTask} (required for Serializable)
     */
    @SuppressWarnings("unused")
    public RandomSleepTask()
        {
        }

    /**
     * Constructs a {@link RandomSleepTask}.
     *
     * @param maxSleepDurationInSeconds  the maximum duration to sleep in seconds
     */
    public RandomSleepTask(int maxSleepDurationInSeconds)
        {
        m_cMaxSleepDurationInSeconds = maxSleepDurationInSeconds;
        }

    // ----- Task interface -------------------------------------------------

    @SuppressWarnings({"finally", "ReturnInsideFinallyBlock"})
    @Override
    public Long execute(Context<Long> context)
        {
        Random random = new Random();

        try
            {
            int cDuration = random.nextInt(m_cMaxSleepDurationInSeconds) + 1;

            Thread.sleep(cDuration * 1000L);
            }
        finally
            {
            return System.currentTimeMillis();
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_cMaxSleepDurationInSeconds = ExternalizableHelper.readInt(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt(out, m_cMaxSleepDurationInSeconds);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_cMaxSleepDurationInSeconds = in.readInt(0);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_cMaxSleepDurationInSeconds);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The maximum sleep duration in seconds.
     */
    protected int m_cMaxSleepDurationInSeconds;
    }
