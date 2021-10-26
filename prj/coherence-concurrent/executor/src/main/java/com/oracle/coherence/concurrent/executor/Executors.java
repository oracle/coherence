/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import com.tangosol.util.ResourceRegistry;

/**
 * TODO.
 *
 * @author rl  8.10.2021
 * @since 21.12
 */
public final class Executors
    {
    // ----- constructors ---------------------------------------------------

    /**
     * No instances allowed.
     */
    private Executors()
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns the locally registered {@link TaskExecutorService}, if any.
     *
     * @return the locally registered {@link TaskExecutorService} or
     *         {@code null} if not registered
     *
     * @throws IllegalStateException if no session is found; most likely means
     *                               this API was called without initializing
     *                               Coherence using the Bootstrap API
     */
    public static TaskExecutorService getLocalExecutorService()
        {
        Session          session  = getExecutorSession();
        ResourceRegistry registry = session.getResourceRegistry();

        return registry.getResource(TaskExecutorService.class, TaskExecutorService.class.getSimpleName());
        }

    /**
     * Return the Coherence {@link Session session} for the {@code Executors}
     * module.
     *
     * @return the Coherence {@link Session session} for the {@code Executors}
     *         module
     *
     * @throws IllegalStateException if no session is found; most likely means
     *                               this API was called without initializing
     *                               Coherence using the Bootstrap API
     */
    public static Session getExecutorSession()
        {
        return Coherence.findSession(SESSION_NAME)
                .orElseThrow(() -> new IllegalStateException(
                                             String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Coherence Executor {@link Session session} name.
     */
    public static final String SESSION_NAME = "coherence-executor";

    /**
     * The default Executor configuration.
     */
    public static final String EXECUTOR_CONFIG_URI = "coherence-executor-cache-config.xml";

    /**
     * System property that may be set to override the default executor configuration
     * with the configuration specified by the value of the property.
     */
    public static final String EXECUTOR_CONFIG_OVERRIDE = "coherence.executor.config.override";
    }
