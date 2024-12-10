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
 * A {@link Task} that calls a {@link Runnable}.
 *
 * @author bo, lh
 * @since 21.12
 */
public class RunnableTask
        implements Task<Boolean>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link RunnableTask} (required for serialization).
     */
    public RunnableTask()
        {
        }

    /**
     * Constructs a {@link RunnableTask}.
     *
     * @param runnable  the runnable
     */
    public RunnableTask(Runnable runnable)
        {
        m_runnable = runnable;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the {@link Runnable}.
     *
     * @return the {@link Runnable}
     */
    public Runnable getRunnable()
        {
        return m_runnable;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public Boolean execute(Context<Boolean> context) throws Exception
        {
        m_runnable.run();

        return true;
        }

    // ----- ExternalizableLite interface -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_runnable = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_runnable);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_runnable = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_runnable);
        }

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        return "RunnableTask{" +
               "runnable=" + m_runnable +
               '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The runnable for the {@link Task}.
     */
    protected Runnable m_runnable;
    }
