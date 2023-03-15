/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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

/**
 * A {@link Task} that repeatedly sets a constant value as result
 * for the specified duration.
 *
 * @author lh
 * @since 21.12
 */
public class RepeatedTask<T>
        implements Task<T>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /*
     * Constructs a {@link RepeatedTask} (required for Serializable)
     */
    @SuppressWarnings("unused")
    public RepeatedTask()
        {
        }

    /**
     * Constructs a {@link RepeatedTask}.
     *
     * @param value            the value
     * @param cDurationMillis  the duration to repeat the task
     */
    public RepeatedTask(T value, long cDurationMillis)
        {
        m_value           = value;
        m_cDurationMillis = cDurationMillis;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public T execute(Context<T> context)
        {
        try
            {
            long cEndMillis = System.currentTimeMillis() + m_cDurationMillis;

            while (System.currentTimeMillis() < cEndMillis)
                {
                //noinspection BusyWait
                Thread.sleep(1000);
                context.setResult(m_value);
                }

            return m_value;
            }
        catch (InterruptedException e)
            {
            throw new RuntimeException(e);
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_value           = ExternalizableHelper.readObject(in);
        m_cDurationMillis = ExternalizableHelper.readLong(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_value);
        ExternalizableHelper.writeLong(out, m_cDurationMillis);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_value           = in.readObject(0);
        m_cDurationMillis = in.readLong(1);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_value);
        out.writeLong(1, m_cDurationMillis);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The value for the {@link Task}.
     */
    protected T m_value;

    /**
     * The duration for repeating the {@link Task}.
     */
    protected long m_cDurationMillis;
    }
