/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.util;

import java.util.concurrent.ThreadFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} implementation for used with {@code named}
 * {@code Executors}.  Each new thread's name will be composed by using
 * the name of the executor service as the prefix and a monotonically
 * increasing integer as the suffix.
 *
 * @author rl 3.8.2024
 * @since 15.1.1.0
 */
public class NamedThreadFactory
        implements ThreadFactory
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code NamedThreadFactory} for the specified
     * executor name.
     *
     * @param f_sName  the executor name
     */
    public NamedThreadFactory(String f_sName)
        {
        if (f_sName == null || f_sName.isEmpty())
            {
            throw new IllegalArgumentException("A name must be specified");
            }

        this.f_sName = f_sName;
        }

    // ----- ThreadFactory interface ----------------------------------------

    @Override
    public Thread newThread(Runnable r)
        {
        return newThread(r, "CES:" + f_sName + '-' + f_counter.incrementAndGet());
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Creates a new thread for the given runnable and using the given name.
     *
     * @param r      the {@link Runnable}
     * @param sName  the name of the thread
     *
     * @return a new thread for the given runnable and using the given name
     */
    public Thread newThread(Runnable r, String sName)
        {
        return new Thread(r, sName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The thread counter.
     */
    protected final AtomicInteger f_counter = new AtomicInteger();

    /**
     * The thread name.
     */
    protected final String f_sName;
    }
