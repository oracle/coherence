/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.tasks.internal;

import com.oracle.coherence.concurrent.executor.PortableTask;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * A {@link PortableTask} that calls a {@link Runnable}.
 *
 * @author bo, lh
 * @since 21.12
 */
public class RunnableTask
        implements PortableTask<Boolean>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link RunnableTask} (required for Serializable).
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

    // ----- data members ---------------------------------------------------

    /**
     * The runnable for the {@link PortableTask}.
     */
    protected Runnable m_runnable;
    }
