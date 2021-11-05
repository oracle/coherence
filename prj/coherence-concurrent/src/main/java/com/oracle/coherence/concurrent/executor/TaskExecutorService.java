/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import java.io.Serializable;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An {@link ExecutorService} that orchestrates execution of submitted executables, for example {@link Runnable}s,
 * {@link Callable}s and {@link Task}s, across zero or more registered {@link Executor}s. Specifically, {@link
 * TaskExecutorService}s don't execute executables. They simply orchestrate submissions to other registered {@link
 * Executor}s for execution.
 *
 * @author bo
 * @since 21.06
 */
public interface TaskExecutorService
        extends ScheduledExecutorService
    {
    /**
     * Registers an {@link Executor} to commence execution of orchestrated tasks.
     *
     * @param executor  the {@link Executor}
     * @param options   the {@link Registration.Option}s for the {@link Executor}
     *
     * @return the {@link Registration} created for the registered {@link Executor},
     *         or the existing {@link Registration} when the {@link Executor} is
     *         already registered
     */
    Registration register(Executor executor, Registration.Option... options);

    /**
     * De-registers a previously registered {@link Executor}.
     * <p>
     * Upon execution, all allocated executables are deemed to be suspended.   Any
     * results made towards completing said executables will be ignored. Executables
     * specifically allocated to the identified {@link Executor} will be lost, unless
     * they were submitted as being retained.   All other executables will be
     * re-allocated and submitted to other available and appropriate subscribed
     * {@link Executor}s.
     *
     * @param executor  the {@link Executor} to deregister
     *
     * @return the {@link Registration} that was originally created for the registered
     *         {@link Executor}, or <code>null</code> if the {@link Executor} is
     *         unknown/not registered
     */
    Registration deregister(Executor executor);

    /**
     * Creates a pending {@link Task.Orchestration} for a {@link Task}.
     *
     * @param task  the {@link Task}
     * @param <T>   the type result produced by the {@link Task}
     *
     * @return an {@link Task.Orchestration}
     */
    <T> Task.Orchestration<T> orchestrate(Task<T> task);

    /**
     * Submits the {@link Task} for execution by the {@link TaskExecutorService}.
     * <p>
     * The default implementation is <code>orchestrate(task).submit()</code>
     * </p>
     *
     * @param task  the {@link Task}
     * @param <T>   the type result produced by the {@link Task}
     *
     * @return a {@link Task.Coordinator} for the {@link Task}
     *
     * @see Task.Orchestration#submit()
     * @see Task.Collectable#submit()
     */
    default <T> Task.Coordinator<T> submit(Task<T> task)
        {
        return orchestrate(task).submit();
        }

    /**
     * Attempts to acquire the {@link Task.Coordinator} for a previously submitted
     * {@link Task}.
     *
     * @param taskId  the unique identity originally allocated to the {@link Task}
     *                (available by calling {@link Task.Coordinator#getTaskId()})
     * @param <R>     the collected result type published by the {@link Task.Coordinator}
     *
     * @return the {@link Task.Coordinator} for the specified {@link Task} or
     *         <code>null</code> if the {@link Task} is unknown
     */
    <R> Task.Coordinator<R> acquire(String taskId);

    // ----- inner interface: ExecutorInfo ----------------------------------

    /**
     * Provides access to the currently available information for a registered {@link Executor}.
     */
    interface ExecutorInfo
        extends Serializable
        {
        /**
         * Obtains the unique identity for the {@link Executor}.
         *
         * @return the unique identity
         */
        String getId();

        /**
         * Obtains the current {@link State} of the {@link Executor}.
         *
         * @return the current {@link State}
         */
        State getState();

        /**
         * Obtains the time since the epoch when the {@link ExecutorInfo} was last
         * updated.
         *
         * @return the time since the epoch for the last update
         */
        long getLastUpdateTime();

        /**
         * The last reported maximum memory by {@link Runtime#maxMemory()} available to
         * the {@link Executor}.
         *
         * @return the maximum memory (in bytes)
         */
        long getMaxMemory();

        /**
         * The last reported total memory by {@link Runtime#totalMemory()} available to
         * the {@link Executor}.
         *
         * @return the total memory (in bytes)
         */
        long getTotalMemory();

        /**
         * The last reported free memory by {@link Runtime#freeMemory()} available to
         * the {@link Executor}.
         *
         * @return the free memory (in bytes)
         */
        long getFreeMemory();

        /**
         * Obtains the {@link Registration.Option} of the specified class when the
         * {@link Executor} was registered, or the default value if not found.
         *
         * @param clzOfOption        the class of {@link Registration.Option}
         * @param defaultIfNotFound  the value to return if not found
         * @param <T>                the type of the {@link Registration.Option}
         *
         * @return a {@link Registration.Option}
         */
        <T extends Registration.Option> T getOption(Class<T> clzOfOption, T defaultIfNotFound);

        // ----- enum: State ------------------------------------------------
        /**
         * The state of an {@link Executor}.
         */
        enum State
            {
            /**
             * The {@link Executor} is the process of joining the orchestration,
             * including introducing itself to the current {@link Task}s for
             * assignment.
             */
            JOINING,

            /**
             * The {@link Executor} is accepting and executing {@link Task}s.
             */
            RUNNING,

            /**
             * The {@link Executor} has commenced graceful closing.  No new
             * {@link Task}s will be accepted, but existing {@link Task}s will
             * run to completion.
             * <p>
             * Once all assigned {@link Task}s are completed, the {@link Executor}
             * will go to {@link State#CLOSING}.
             * </p>
             */
            CLOSING_GRACEFULLY,

            /**
             * The {@link Executor} has commenced closing, including cleaning up
             * resources and allocated {@link Task}s.
             */
            CLOSING,

            /**
             * The {@link Executor} has been closed and no longer has any allocated
             * {@link Task}s.
             */
            CLOSED,

            /**
             * The {@link Executor} is rejecting {@link Task}s.
             */
            REJECTING
            }
        }

    /**
     * Provides registration information about an {@link Executor} that was registered with an {@link
     * TaskExecutorService} using {@link TaskExecutorService#register(Executor, Option...)}.
     */
    interface Registration
        {
        /**
         * Obtains the unique identity for the registered {@link Executor}.
         *
         * @return the unique identity
         */
        String getId();

        /**
         * Obtains the {@link Registration.Option} of the specified class when the {@link Executor} was registered, or
         * the default value if not found.
         *
         * @param classOfOption      the class of {@link Registration.Option}
         * @param defaultIfNotFound  the value to return if not found
         * @param <T>                the type of the {@link Registration.Option}
         *
         * @return a {@link Registration.Option}
         */
        <T extends Registration.Option> T getOption(Class<T> classOfOption,
                                                    T defaultIfNotFound);

        /**
         * An {@link Option} for an {@link Executor} when registered.
         */
        interface Option
            extends Serializable
            {
            }
        }
    }
