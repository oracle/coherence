/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.internal;

import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal utility class to allow components that can't easily be
 * hooked into the ECCF/Session lifecycle to register {@link Runnable}s that
 * will be invoked when the executor service runtime is being terminated.
 *
 * @author rl 11.27.23
 * @since 22.06.7
 */
public class Hook
    {
    /**
     * Add the {@link Runnable} using the specified {@link ResourceRegistry}.
     *
     * @param registry  the {@link ResourceRegistry}
     * @param r         the {@link Runnable}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addShutdownHook(ResourceRegistry registry, Runnable r)
        {
        registry.registerResource(List.class, NAME, ArrayList::new,
                                  RegistrationBehavior.IGNORE, null);

        List listRunnables = registry.getResource(List.class, NAME);
        listRunnables.add(r);
        }

    /**
     * Run the shutdown hooks associated with the specified
     * {@link ResourceRegistry}.
     *
     * @param registry  the {@link ResourceRegistry}
     */
    @SuppressWarnings("unchecked")
    public static void runShutdownHooks(ResourceRegistry registry)
        {
        List<Runnable> runnables = registry.getResource(List.class, NAME);
        if (runnables != null && !runnables.isEmpty())
            {
            runnables.forEach(Runnable::run);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name to which the registered {@link Runnable}s will be associated
     * within the {@link ResourceRegistry}
     */
    private static final String NAME = "concurrent.closing.runnables";
    }