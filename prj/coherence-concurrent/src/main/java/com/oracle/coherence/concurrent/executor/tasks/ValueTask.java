/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.tasks;

import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

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
     * Constructs a {@link ValueTask} (required for Serializable).
     */
    @SuppressWarnings("unused")
    public ValueTask()
        {
        }

    /**
     * Constructs a {@link ValueTask}.
     *
     * @param value the value
     */
    @SuppressWarnings("unused")
    public ValueTask(T value)
        {
        m_value = value;
        }

    // ----- Task interface -------------------------------------------------

    @Override
    public T execute(Context<T> context) throws Exception
        {
        return m_value;
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_value = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_value);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "ValueTask{"
               + "value=" + m_value
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

        return Objects.equals(m_value, valueTask.m_value);
        }

    @Override
    public int hashCode()
        {
        return m_value != null ? m_value.hashCode() : 0;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The value for the {@link Task}.
     */
    protected T m_value;
    }
