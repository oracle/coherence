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
     * Constructs a {@link CallableTask} (required for serialization).
     */
    public CallableTask()
        {
        }

    /**
     * Constructs a {@link CallableTask}.
     *
     * @param callable  the callable
     */
    public CallableTask(Callable<T> callable)
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
        return m_callable.call();
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_callable = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_callable);
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

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        return "CallableTask{" +
               "callable=" + m_callable +
               '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The value for the {@link Task}.
     */
    protected Callable<T> m_callable;
    }
