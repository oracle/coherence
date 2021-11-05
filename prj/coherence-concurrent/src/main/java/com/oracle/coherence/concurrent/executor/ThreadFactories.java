/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import java.util.concurrent.ThreadFactory;

/**
 * The {@link ThreadFactories} class provides a simple parameterized mechanism to create {@link ThreadFactory}s.
 *
 * @author bo
 * @since 21.12
 */
public final class ThreadFactories
    {
    // ----- constructors ---------------------------------------------------

    /**
     * New instances not allowed.
     */
    private ThreadFactories()
        {
        }

    // ----- public API -----------------------------------------------------

    /**
     * Create a {@link ThreadFactory} based on the specified parameters.
     *
     * @param fIsDaemon    should the {@link ThreadFactory} produce daemon threads
     * @param sThreadName  the threadName of the produced threads
     * @param threadGroup  the {@link ThreadGroup} for the produced threads
     *
     * @return A {@link ThreadFactory}
     */
    public static ThreadFactory createThreadFactory(final boolean fIsDaemon, final String sThreadName,
            final ThreadGroup threadGroup)
        {
        return r ->
            {
            Thread thread = new Thread(threadGroup, r);

            thread.setDaemon(fIsDaemon);
            thread.setName(sThreadName + ':' + thread.getName());

            return thread;
            };
        }
    }
