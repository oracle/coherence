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
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link Task} that calls a runnable.
 *
 * @param <T>  the type of result produced by the {@link Task}
 *
 * @author bo, lh
 * @since 21.12
 */
public class RunnableWithResultTask<T>
        implements Task<T>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link RunnableWithResultTask} (required for serialization).
     */
    @SuppressWarnings("unused")
    public RunnableWithResultTask()
        {
        }

    /**
     * Constructs a {@link RunnableWithResultTask}.
     *
     * @param runnable  the runnable
     * @param result    the result to return
     */
    public RunnableWithResultTask(Runnable runnable, T result)
        {
        m_runnable = runnable;
        m_result   = result;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public T execute(Context<T> context) throws Exception
        {
        m_runnable.run();

        return m_result;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_runnable = ExternalizableHelper.readObject(in);
        m_result   = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_runnable);
        ExternalizableHelper.writeObject(out, m_result);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_runnable = in.readObject(0);
        m_result   = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_runnable);
        out.writeObject(1, m_result);
        }

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        return "RunnableWithResultTask{" +
               "runnable=" + m_runnable +
               ", return-result=" + m_result +
               '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The runnable for the {@link Task}.
     */
    protected Runnable m_runnable;

    /**
     * The runnable for the {@link Task}.
     */
    protected T m_result;
    }
