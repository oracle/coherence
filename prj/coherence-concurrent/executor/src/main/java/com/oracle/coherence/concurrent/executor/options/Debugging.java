/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.options;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

import java.util.Objects;

/**
 * An {@link Task.Option} to turn on the {@link Debugging} of a {@link Task}.
 *
 * @author lh
 * @since 21.12
 */
public class Debugging
        implements Task.Option, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Debugging} (required for Serializable).
     */
    @SuppressWarnings("unused")
    public Debugging()
        {
        this(Logger.FINEST);
        }

    /**
     * Constructs a {@link Debugging} option.
     *
     * @param nLogLevel  the {@link Debugging} log level.
     *
     * @see Logger
     */
    protected Debugging(int nLogLevel)
        {
        m_nLogLevel = nLogLevel;
        }

    // ----- accessors ------------------------------------------------------

    ///**
    // * Sets the Coherence ExecutorService log {@link Level}.
    // *
    // * @param  logLevel  the Coherence ExecutorService log {@link Level}
    // */
    //public void setExecutorServiceLogLevel(Level logLevel)
    //    {
    //    m_executorServiceLogLevel = logLevel;
    //    }
    //
    ///**
    // * Obtains the Coherence ExecutorService log level.
    // *
    // * @return the Coherence ExecutorService log {@link Level}
    // */
    //public Level getExecutorServiceLogLevel()
    //    {
    //    if (m_executorServiceLogLevel == null)
    //        {
    //        m_executorServiceLogLevel = Level.INFO;
    //        }
    //
    //    return m_executorServiceLogLevel;
    //    }

    /**
     * Obtains the log level of the {@link Debugging} option.
     *
     * @return the log level of the {@link Debugging} option
     */
    public int getLogLevel()
        {
        return m_nLogLevel;
        }

    /**
     * Obtains a {@link Debugging} with a specific log level.
     *
     * @param nLogLevel  the log level of the {@link Debugging}
     *
     * @return the {@link Debugging} option
     */
    public static Debugging of(int nLogLevel)
        {
        return new Debugging(nLogLevel);
        }

    /**
     * Obtains the log level of the {@link Debugging} option.  If there is no log level,
     * or the supplied one is smaller than the debug one, return the supplied one.
     *
     * @param nLogLevel  the given log level
     *
     * @return the appropriate log level for the log
     */
    public int getPreferredLevel(int nLogLevel)
        {
        if (m_nLogLevel > nLogLevel)
            {
            return nLogLevel;
            }
        return m_nLogLevel;
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

        Debugging debugging = (Debugging) o;

        return Objects.equals(m_nLogLevel, debugging.m_nLogLevel);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_nLogLevel);
        }

    @Override
    public String toString()
        {
        return "Debugging{"
               + "logLevel=" + m_nLogLevel
               + '}';
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nLogLevel = in.readInt(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nLogLevel);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The log level of the {@link Debugging} option.
     */
    protected int m_nLogLevel;
    }
