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

/**
 * A {@link Task} that throws an exception.
 *
 * @author lh
 * @since 21.12
 */
public class ExceptionTask<T>
        implements Task<T>, PortableObject
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

    // ----- Task interface -------------------------------------------------

    @Override
    public T execute(Context<T> context)
        {
        context.setResult(m_value);

        throw getThrowable();
        }

    // ----- ExternalizableLite interface -----------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        m_value = ExternalizableHelper.readObject(in);
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_value);
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

    // ----- data members ---------------------------------------------------

    /**
     * The value for the {@link Task}.
     */
    protected T m_value;
    }
