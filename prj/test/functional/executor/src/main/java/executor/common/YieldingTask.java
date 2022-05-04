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

import java.time.Duration;

/**
 * A {@link Task} that yields a specified number of times, returning the number of times the {@link Task} was resumed
 * ie: {@link Context#isResuming()} returned <code>true</code>.
 *
 * @author Brian Oliver
 * @since 21.12
 */
public class YieldingTask
        implements Task<Integer>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link YieldingTask} (required for Serializable)
     */
    @SuppressWarnings("unused")
    public YieldingTask()
        {
        }

    /**
     * Constructs a {@link YieldingTask}
     *
     * @param yieldCount  the number of times to {@link Yield}
     */
    public YieldingTask(int yieldCount)
        {
        this(yieldCount, -1);
        }

    /**
     * Constructs a {@link YieldingTask}
     *
     * @param yieldCount    the number of times to {@link Yield}
     * @param yieldSeconds  the yield time in seconds
     */
    public YieldingTask(int yieldCount, long yieldSeconds)
        {
        this.m_cYield = Math.max(0, yieldCount);
        this.m_cResumed = 0;

        if (yieldSeconds >= 0)
            {
            this.m_yieldDuration = Duration.ofSeconds(yieldSeconds);
            }
        else
            {
            this.m_yieldDuration = Duration.ofSeconds(2);
            }
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public Integer execute(Context<Integer> context) throws Exception
        {
        if (context.isResuming())
            {
            m_cResumed++;
            }

        System.out.println("YieldingTask: Resuming? " + context.isResuming());

        if (m_cYield > 0)
            {
            m_cYield--;

            System.out.println("YieldingTask: Yielding (remaining yield count of " + m_cYield + ")");

            throw Yield.atLeast(m_yieldDuration);
            }
        else
            {
            System.out.println("YieldingTask: Completed (returning " + m_cResumed + ")");

            return m_cResumed;
            }
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_cYield        = ExternalizableHelper.readInt(in);
        m_yieldDuration = Duration.ofSeconds(ExternalizableHelper.readLong(in));
        m_cResumed      = ExternalizableHelper.readInt(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt(out, m_cYield);
        ExternalizableHelper.writeLong(out, m_yieldDuration.getSeconds());
        ExternalizableHelper.writeInt(out, m_cResumed);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_cYield        = in.readInt(0);
        m_yieldDuration = Duration.ofSeconds(in.readLong(1));
        m_cResumed      = in.readInt(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_cYield);
        out.writeLong(1, m_yieldDuration.getSeconds());
        out.writeInt(2, m_cResumed);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of times to yield;
     */
    protected int m_cYield;

    /**
     * The number of times the {@link YieldingTask} was resumed.
     */
    protected int m_cResumed;

    /**
     * The yield duration.
     */
    protected Duration m_yieldDuration;
    }
