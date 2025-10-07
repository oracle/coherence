/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import com.tangosol.util.Base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Helper class for virtual threads functionality.
 * <p>
 * The main purpose of this class is to isolate the code that uses virtual threads
 * (Loom) APIs, in order to simplify multi-release JAR creation.
 *
 * @author Aleks Seovic  2023.06.08
 * @since 23.09
 */
public class VirtualThreads
    {
    /**
     * Create a virtual thread with the specified runnable, and name.
     *
     * @param group     (optional) the thread's thread group
     * @param runnable  (optional) the thread's runnable
     * @param sName     (optional) the thread's name
     *
     * @return a new thread using the specified parameters
     */
    public static Thread makeThread(ThreadGroup group, Runnable runnable, String sName)
        {
        return Base.makeThread(group, runnable, sName);
        }

    /**
     * Return {@code true} if the current runtime supports virtual threads.
     *
     * @return {@code true} if the current runtime supports virtual threads;
     *         {@code false} otherwise
     */
    public static boolean isSupported()
        {
        return false;
        }

    /**
     * Return {@code true} if virtual threads are enabled.
     *
     * @return {@code true} if the virtual threads are enabled;
     *         {@code false} otherwise
     */
    public static boolean isEnabled()
        {
        return false;
        }

    /**
     * Return {@code true} if virtual threads are enabled for the specified service.
     *
     * @param serviceName  the name of the service to check
     *
     * @return {@code true} if the virtual threads are enabled for the specified service;
     *         {@code false} otherwise
     */
    public static boolean isEnabled(String serviceName)
        {
        return false;
        }

    /**
     * Returns either a new virtual thread per-task executor on Java 21 or higher
     * or a single threaded executor if lower than Java 21.
     *
     * @param factory  the {@link ThreadFactory} to use if not on Java 21
     *
     * @return either a new virtual thread per-task executor on Java 21 or higher
     *         or a single threaded executor if lower than Java 21.
     */
    public static Executor newMaybeVirtualThreadExecutor(ThreadFactory factory)
        {
        return Executors.newSingleThreadExecutor(factory);
        }
    }
