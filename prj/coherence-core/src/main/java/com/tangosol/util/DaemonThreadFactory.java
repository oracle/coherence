/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.concurrent.atomic.AtomicInteger;


/**
* DaemonThreadFactory is a ThreadFactory which produces daemon threads.
*
* @author mf  2010.05.12
* @since Coherence 3.6
*/
public class DaemonThreadFactory
        implements ThreadFactory
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new DameonThreadFacotry.
    */
    public DaemonThreadFactory()
        {
        this(null);
        }

    /**
    * Construct a new DameonThreadFacotry.
    *
    * @param sPrefix  the prefix for unnamed threads
    */
    public DaemonThreadFactory(String sPrefix)
        {
        m_sNamePrefix = sPrefix;
        }


    // ----- ThreadFactory methods ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Thread makeThread(ThreadGroup group, Runnable runnable,
            String sName)
        {
        String sPrefix = m_sNamePrefix;
        Thread thread  = sName == null && sPrefix == null
            ? new Thread(group, runnable)
            : new Thread(group, runnable, sPrefix + m_cNameSuffix.incrementAndGet());
        thread.setDaemon(true);
        return thread;
        }

    /**
    * {@inheritDoc}
    */
    public Thread newThread(Runnable r)
        {
        String sPrefix = m_sNamePrefix;
        Thread thread  = sPrefix == null
            ? new Thread(r)
            : new Thread(r, sPrefix + m_cNameSuffix.incrementAndGet());
        thread.setDaemon(true);
        return thread;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The prefix to use for unnammed threads produced by the factory.
    */
    protected final String m_sNamePrefix;

    /**
    * The thread name counter.
    */
    protected final AtomicInteger m_cNameSuffix = new AtomicInteger();


    // ----- constants ------------------------------------------------------

    /**
    * A reuseable DaemonThreadFactory instance.
    */
    public static final DaemonThreadFactory INSTANCE =
            new DaemonThreadFactory("DaemonThread-");
    }
