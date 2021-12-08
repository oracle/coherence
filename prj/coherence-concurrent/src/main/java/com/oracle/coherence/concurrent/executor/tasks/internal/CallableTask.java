/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.tasks.internal;

import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

import java.util.concurrent.Callable;

/**
 * A {@link Task} that calls a {@link Callable}.
 *
 * @param <T>  the type of result produced by the {@link Task}
 *
 * @author bo, lh
 * @since 21.12
 */
public class CallableTask<T>
        implements Task<T>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CallableTask} (required for Serializable).
     */
    public CallableTask()
        {
        }

    /**
     * Constructs a {@link CallableTask}.
     *
     * @param callable  the callable
     */
    public CallableTask(Callable callable)
        {
        m_callable = callable;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the {@link Callable}.
     *
     * @return the {@link Callable}
     */
    public Callable<T> getCallable()
        {
        return m_callable;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public T execute(Context<T> context) throws Exception
        {
        T result = m_callable.call();

        return result;
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_callable = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_callable);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The value for the {@link Task}.
     */
    protected Callable<T> m_callable;
    }
