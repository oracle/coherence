/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.tasks;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.util.CronPattern;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.time.Duration;

/**
 * A {@link Task} that can run repeatedly at scheduled time, like a crontab job.
 *
 * @param <T>  the type of the {@link Task}
 *
 * @author lh, bo
 * @since 21.12
 */
public class CronTask<T>
        implements Task<T>, PortableObject, TaskExecutorService.Registration.Option
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CronTask} (required for serialization).
     */
    @SuppressWarnings("unused")
    public CronTask()
        {
        }

    /**
     * Constructs a {@link CronTask}.
     *
     * @param task      the task
     * @param sPattern  the crontab scheduling pattern
     */
    @SuppressWarnings("unchecked")
    public CronTask(Task<T> task, String sPattern)
        {
        if (task == null)
            {
            throw new IllegalArgumentException("Task must be specified");
            }

        if (sPattern == null)
            {
            throw new IllegalArgumentException("Crontab pattern must be specified");
            }

        m_origTask               = task;
        m_task                   = (Task<T>) clone(task);
        m_sCronPattern           = sPattern;
        m_ldtNextExecutionMillis = 0;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the {@link CronTask}.
     *
     * @return the {@link CronTask}
     */
    public Task<T> getTask()
        {
        return m_task;
        }

    /**
     * Returns the crontab schedule pattern.
     *
     * @return the crontab schedule pattern
     */
    @SuppressWarnings("unused")
    public String getCronPattern()
        {
        return m_sCronPattern;
        }

    /**
     * Returns the next execution time in milliseconds.
     *
     * @param cMillis The timestamp, as a UNIX-era millis value.
     *
     * @return the next execution time in milliseconds
     */
    public long getNextExecutionMillis(long cMillis)
        {
        m_ldtNextExecutionMillis = new CronPattern(m_sCronPattern).getNextExecuteTime(cMillis);

        return m_ldtNextExecutionMillis;
        }

    // ----- Task interface -------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public T execute(Context<T> context) throws Exception
        {
        long ldtNextMillis    = m_ldtNextExecutionMillis;
        long ldtCurrentMillis = System.currentTimeMillis();

        // first time
        if (ldtNextMillis == 0)
            {
            ldtNextMillis = getNextExecutionMillis(ldtCurrentMillis);

            throw Yield.atLeast(Duration.ofMillis(ldtNextMillis - ldtCurrentMillis));
            }

        try
            {
            T result = m_task.execute(context);

            context.setResult(result);

            m_task = (Task<T>) clone(m_origTask);

            }
        finally
            {
            if (ldtNextMillis < ldtCurrentMillis)
                {
                ldtNextMillis = getNextExecutionMillis(ldtCurrentMillis);
                }
            }

        throw Yield.atLeast(Duration.ofMillis(ldtNextMillis - ldtCurrentMillis));
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_origTask               = ExternalizableHelper.readObject(in);
        m_task                   = ExternalizableHelper.readObject(in);
        m_sCronPattern           = ExternalizableHelper.readUTF(in);
        m_ldtNextExecutionMillis = ExternalizableHelper.readLong(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_origTask);
        ExternalizableHelper.writeObject(out, m_task);
        ExternalizableHelper.writeUTF(out, m_sCronPattern);
        ExternalizableHelper.writeLong(out, m_ldtNextExecutionMillis);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_origTask               = in.readObject(0);
        m_task                   = in.readObject(1);
        m_sCronPattern           = in.readString(2);
        m_ldtNextExecutionMillis = in.readLong(3);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_origTask);
        out.writeObject(1, m_task);
        out.writeString(2, m_sCronPattern);
        out.writeLong(3,   m_ldtNextExecutionMillis);
        }

    // ----- Object methods -------------------------------------------------

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

        CronTask<?> cronTask = (CronTask<?>) o;

        if (!m_origTask.equals(cronTask.m_origTask))
            {
            return false;
            }

        return m_sCronPattern.equals(cronTask.m_sCronPattern);
        }

    @Override
    public int hashCode()
        {
        int result = m_task.hashCode();

        result = 31 * result + m_sCronPattern.hashCode();

        return result;
        }

    @Override
    public String toString()
        {
        return "CronTask{task=" + m_task + "cron-pattern=" + m_sCronPattern + '}';
        }

    /**
     * Creates a clone of the given object.
     *
     * @param object  the object to clone
     *
     * @return the cloned object
     */
    public static Object clone(Object object)
        {
        try
            {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeObject(object);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);

            return ois.readObject();
            }
        catch (Exception e)
            {
            Logger.finer("Unable to clone object", e);

            return null;
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtains a {@link CronTask}.
     *
     * @param task          the task to be scheduled
     * @param sCronPattern  the task schedule pattern
     * @param <T>           the type of the {@link Task}
     *
     * @return a {@link CronTask}
     */
    public static <T> CronTask<T> of(Task<T> task, String sCronPattern)
        {
        return new CronTask<>(task, sCronPattern);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The original {@link Task}.
     */
    protected Task<T> m_origTask;

    /**
     * The actual {@link Task}.
     */
    protected Task<T> m_task;

    /**
     * The crontab scheduling pattern for the {@link Task}.
     */
    protected String m_sCronPattern;

    /**
     * The next execution time.
     */
    protected long m_ldtNextExecutionMillis;
    }
