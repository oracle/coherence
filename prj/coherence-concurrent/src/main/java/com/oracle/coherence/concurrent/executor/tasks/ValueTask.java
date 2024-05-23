/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.tasks;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

/**
 * A {@link Task} that provides a constant value as a result.
 *
 * @param <T>  the type of result produced by the {@link Task}
 *
 * @author bo
 * @since 21.12
 */
public class ValueTask<T>
        implements Task<T>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ValueTask} (required for serialization).
     */
    @SuppressWarnings("unused")
    public ValueTask()
        {
        }

    /**
     * Constructs a {@link ValueTask}.
     *
     * @param value the value to return
     */
    @SuppressWarnings("unused")
    public ValueTask(T value)
        {
        this(value, -1);
        }

    /**
     * Constructs a {@link ValueTask} that returns its value after
     * the given delay.
     *
     * @param value          the value to return
     * @param cInitialDelay  the delay, in millis, the task should wait
     *                       before returning the value
     */
    public ValueTask(T value, long cInitialDelay)
        {
        m_value         = value;
        m_cInitialDelay = cInitialDelay;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public T execute(Context<T> context) throws Exception
        {
        long cInitialDelay = m_cInitialDelay;

        if (cInitialDelay > 0)
            {
            Blocking.sleep(cInitialDelay);
            }

        return m_value;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_value         = ExternalizableHelper.readObject(in);
        m_cInitialDelay = ExternalizableHelper.readLong(in);

        }

    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_value);
        ExternalizableHelper.writeLong(out, m_cInitialDelay);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_value         = in.readObject(0);
        m_cInitialDelay = in.readLong(1);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_value);
        out.writeLong(1, m_cInitialDelay);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "ValueTask{"
               + "value=" + m_value
               + ", initial-delay=" + m_cInitialDelay
               + '}';
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        ValueTask<?> valueTask = (ValueTask<?>) o;

        return Objects.equals(m_value, valueTask.m_value)
               && m_cInitialDelay == valueTask.m_cInitialDelay;
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_value, m_cInitialDelay);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The value for the {@link Task}.
     */
    protected T m_value;

    /**
     * The initial delay before returning the result.
     */
    protected long m_cInitialDelay;
    }
