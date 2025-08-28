/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.security.SecurityHelper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

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
     * @param group     (ignored) the thread's thread group
     * @param runnable  the thread's runnable
     * @param sName     (optional) the thread's name
     *
     * @return a new thread using the specified parameters
     */
    public static Thread makeThread(ThreadGroup group, Runnable runnable, String sName)
        {
        Thread.Builder.OfVirtual builder = Thread.ofVirtual();
        if (sName != null)
            {
            builder.name(sName);
            }
        return builder.unstarted(runnable);
        }

    /**
     * Return {@code true} if the current runtime supports virtual threads.
     *
     * @return {@code true} if the current runtime supports virtual threads;
     *         {@code false} otherwise
     */
    public static boolean isSupported()
        {
        // NOTE:  virtual threads will not be used if the security manager
        //        is enabled.  The following from the javadocs of
        //        java.lang.Thread explains why:
        //              Creating a platform thread captures the caller
        //              context to limit the permissions of the new thread
        //              when it executes code that performs a privileged
        //              action. The captured caller context is the new
        //              thread's "Inherited AccessControlContext".
        //              Creating a virtual thread does not capture the
        //              caller context; virtual threads have no permissions
        //              when executing code that performs a privileged
        //              action.
        return !SecurityHelper.hasSecurityManager();
        }

    /**
     * Return {@code true} if virtual threads are enabled.
     *
     * @return {@code true} if the virtual threads are enabled;
     *         {@code false} otherwise
     */
    public static boolean isEnabled()
        {
        return CacheFactory.getCluster().getDependencies().isVirtualThreadsEnabled();
        }

    /**
     * Return {@code true} if virtual threads are enabled for the specified service.
     *
     * @param sServiceName  the name of the service to check
     *
     * @return {@code true} if the virtual threads are enabled for the specified service;
     *         {@code false} otherwise
     */
    public static boolean isEnabled(String sServiceName)
        {
        return sServiceName == null
               ? isEnabled()
               : Config.getBoolean(PROPERTY_SERVICE_ENABLED.apply(sServiceName), isEnabled());
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
        return Executors.newVirtualThreadPerTaskExecutor();
        }

    // ---- constants -------------------------------------------------------

    /**
     * Config property used to globally enable or disable virtual threads.
     */
    public static final String PROPERTY_ENABLED = "coherence.virtualthreads.enabled";

    /**
     * Config property used to selectively enable or disable virtual threads for
     * a specific service.
     */
    public static final Function<String, String> PROPERTY_SERVICE_ENABLED =
            (sServiceName) -> String.format("coherence.service.%s.virtualthreads.enabled", sServiceName);
    }
