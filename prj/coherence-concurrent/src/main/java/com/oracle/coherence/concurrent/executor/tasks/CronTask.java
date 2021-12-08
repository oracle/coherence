/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.tasks;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.util.CronPattern;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
     * Constructs a {@link CronTask} (required for Serializable).
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
        sCronPattern             = sPattern;
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
    public String getCronPattern()
        {
        return sCronPattern;
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
        m_ldtNextExecutionMillis = new CronPattern(sCronPattern).getNextExecuteTime(cMillis);

        return m_ldtNextExecutionMillis;
        }

    // ----- Task interface -------------------------------------------------

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
        catch (Yield yield)
            {
            throw yield;
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

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_origTask               = in.readObject(0);
        m_task                   = in.readObject(1);
        sCronPattern             = in.readString(2);
        m_ldtNextExecutionMillis = in.readLong(3);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_origTask);
        out.writeObject(1, m_task);
        out.writeString(2, sCronPattern);
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

        return sCronPattern.equals(cronTask.sCronPattern);
        }

    @Override
    public int hashCode()
        {
        int result = m_task.hashCode();

        result = 31 * result + sCronPattern.hashCode();

        return result;
        }

    @Override
    public String toString()
        {
        return "CronTask{" + "task=" + m_task + "cron pattern=" + sCronPattern + '}';
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
    protected String sCronPattern;

    /**
     * The next execution time.
     */
    protected long m_ldtNextExecutionMillis;
    }
