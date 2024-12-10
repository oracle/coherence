/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.util;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadFactory;


/**
 * DaemonThreadFactory is a ThreadFactory which produces daemon threads.
 *
 * @author mf  2010.05.12
 */
public class DaemonThreadFactory
        implements ThreadFactory
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new DaemonThreadFactory.
     */
    public DaemonThreadFactory()
        {
        this(null);
        }

    /**
     * Construct a new DaemonThreadFactory.
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
     * The prefix to use for un-named threads produced by the factory.
     */
    protected final String m_sNamePrefix;

    /**
     * The thread name counter.
     */
    protected final AtomicInteger m_cNameSuffix = new AtomicInteger();


    // ----- constants ------------------------------------------------------

    /**
     * A reusable DaemonThreadFactory instance.
     */
    public static final DaemonThreadFactory INSTANCE =
            new DaemonThreadFactory("DaemonThread-");
    }
