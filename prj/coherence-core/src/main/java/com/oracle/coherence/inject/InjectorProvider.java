/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.inject;

import jakarta.annotation.Priority;

import java.util.ServiceLoader;

/**
 * A {@link Injector} provider used by {@link Injectable}.
 *
 * @author Jonathan Knight  2020.11.19
 * @since 20.12
 */
class InjectorProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor for utility class.
     */
    private InjectorProvider()
        {
        }

    // ----- InjectorProvider methods ---------------------------------------

    /**
     * Obtain the {@link Injector} to use.
     *
     * @return  the {@link Injector} to use
     */
    public static Injector getInstance()
        {
        return LazyHolder.SINGLETON;
        }

    // ----- helper methods -------------------------------------------------

    static Injector getInjector()
        {
        Injector injector  = INSTANCE;
        int      nPriority = Integer.MIN_VALUE;

        for (Injector instance : ServiceLoader.load(Injector.class))
            {
            Priority priority = instance.getClass().getAnnotation(Priority.class);
            if (priority != null && priority.value() > nPriority)
                {
                injector  = instance;
                nPriority = priority.value();
                }
            else if (nPriority == Integer.MIN_VALUE)
                {
                injector = instance;
                }
            }
        return injector;
        }

    // ----- inner class: LazyHolder ----------------------------------------

    /**
     * A holder for the singleton {@link Injector} instance.
     */
    private static class LazyHolder
        {
        static final Injector SINGLETON = InjectorProvider.getInjector();
        }

    // ----- constants ------------------------------------------------------

    /**
     * A no-op {@link Injector}.
     */
    private static final Injector INSTANCE = (target) -> {};
    }
