/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.coherence.concurrent.executor.PortableTask;
import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * A {@link Task} that throws an exception.
 *
 * @author lh
 * @since 21.12
 */
public class ExceptionTask<T>
        implements PortableTask<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ExceptionTask} (required for Serializable)
     */
    @SuppressWarnings("unused")
    public ExceptionTask()
        {
        }

    /**
     * Constructs a {@link ExceptionTask}.
     *
     * @param value  the value
     */
    public ExceptionTask(T value)
        {
        m_value = value;
        }

    // ----- public methods -------------------------------------------------

    public RuntimeException getThrowable()
        {
        return new RuntimeException("Exception Task [" + m_value + "]");
        }

    // ----- PortableTask interface -----------------------------------------

    @Override
    public T execute(Context<T> context)
        {
        context.setResult(m_value);

        throw getThrowable();
        }

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

    // ----- data members ---------------------------------------------------

    /**
     * The value for the {@link Task}.
     */
    protected T m_value;
    }
