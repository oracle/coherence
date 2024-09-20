/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.io.ExternalizableLite;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * An {@link ExecutorService} that orchestrates execution of submitted executables, for example {@link Runnable}s,
 * {@link Callable}s and {@link Task}s, across zero or more registered {@link ExecutorService}s. Specifically, {@link
 * TaskExecutorService}s don't execute executables. They simply orchestrate submissions to other registered {@link
 * ExecutorService}s for execution.
 *
 * @author bo
 * @since 21.06
 */
public interface TaskExecutorService
        extends RemoteExecutor
    {
    /**
     * Registers an {@link ExecutorService} to commence execution of orchestrated tasks.
     *
     * @param executor  the {@link ExecutorService} to register
     * @param options   the {@link Registration.Option}s for the {@link ExecutorService}
     *
     * @return the {@link Registration} created for the registered {@link ExecutorService},
     *         or the existing {@link Registration} when the {@link ExecutorService} is
     *         already registered
     */
    Registration register(ExecutorService executor, Registration.Option... options);

    /**
     * De-registers a previously registered {@link ExecutorService}.
     * <p>
     * Upon execution, all allocated executables are deemed to be suspended.   Any
     * results made towards completing said executables will be ignored. Executables
     * specifically allocated to the identified {@link ExecutorService} will be lost, unless
     * they were submitted as being retained.   All other executables will be
     * re-allocated and submitted to other available and appropriate subscribed
     * {@link ExecutorService}s.
     *
     * @param executor  the {@link ExecutorService} to deregister
     *
     * @return the {@link Registration} that was originally created for the registered
     *         {@link ExecutorService}, or <code>null</code> if the {@link ExecutorService}
     *         is unknown/not registered
     */
    Registration deregister(ExecutorService executor);

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
     * Provides access to the currently available information for a registered
     * {@link ExecutorService}.
     */
    interface ExecutorInfo
            extends ExternalizableLite
        {
        /**
         * Obtains the unique identity for the {@link ExecutorService}.
         *
         * @return the unique identity
         */
        String getId();

        /**
         * Obtains the current {@link State} of the {@link ExecutorService}.
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
        @SuppressWarnings("unused")
        long getLastUpdateTime();

        /**
         * Returns the time since the epoch when the associated executor
         * joined the service.
         *
         * @return the time since the epoch when the associated executor
         *         joined the service
         *
         * @since 22.06.7
         */
        long getJoinTime();

        /**
         * The last reported maximum memory by {@link Runtime#maxMemory()} available to
         * the {@link ExecutorService}.
         *
         * @return the maximum memory (in bytes)
         */
        @SuppressWarnings("unused")
        long getMaxMemory();

        /**
         * The last reported total memory by {@link Runtime#totalMemory()} available to
         * the {@link ExecutorService}.
         *
         * @return the total memory (in bytes)
         */
        @SuppressWarnings("unused")
        long getTotalMemory();

        /**
         * The last reported free memory by {@link Runtime#freeMemory()} available to
         * the {@link ExecutorService}.
         *
         * @return the free memory (in bytes)
         */
        long getFreeMemory();

        /**
         * Obtains the {@link Registration.Option} of the specified class when the
         * {@link ExecutorService} was registered, or the default value if not found.
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
         * The state of an {@link ExecutorService}.
         */
        enum State
            {
            /**
             * The {@link ExecutorService} is the process of joining the
             * orchestration, including introducing itself to the current
             * {@link Task}s for assignment.
             */
            JOINING,

            /**
             * The {@link ExecutorService} is accepting and executing
             * {@link Task}s.
             */
            RUNNING,

            /**
             * The {@link ExecutorService} has commenced graceful closing.
             * No new {@link Task}s will be accepted, but existing
             * {@link Task}s will run to completion.
             * <p>
             * Once all assigned {@link Task}s are completed, the
             * {@link ExecutorService} will go to {@link State#CLOSING}.
             * </p>
             */
            CLOSING_GRACEFULLY,

            /**
             * The {@link ExecutorService} has commenced closing, including
             * cleaning up resources and allocated {@link Task}s.
             */
            CLOSING,

            /**
             * The {@link ExecutorService} has been closed and no longer has
             * any allocated {@link Task}s.
             */
            CLOSED,

            /**
             * The {@link ExecutorService} is rejecting {@link Task}s.
             */
            REJECTING;

            /**
             * Return an integer value for the enum to be used as {@code MetricValue}.
             *
             * @return integer value for the enum {@link State}
             *
             * @since 22.06
             */
            public int getCode()
                {
                // optimization to return ordinal() as enum's constant
                // Since this is intended to be used as MetricsValue,
                // start at 1 since a value of zero would be considered not set
                // and the metric would not be returned.
                return ordinal() + 1;
                }
            }
        }

    /**
     * Provides registration information about an {@link ExecutorService} registration.
     */
    interface Registration
        {
        /**
         * Obtains the unique identity for the registered {@link ExecutorService}.
         *
         * @return the unique identity
         */
        String getId();

        /**
         * Obtains the {@link Registration.Option} of the specified class when the
         * {@link ExecutorService} was registered, or the default value if not found.
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
         * TODO(rl): flesh out contract
         * Close this registration.
         */
        void close();

        /**
         * An {@link Option} for an {@link ExecutorService} when registered.
         */
        interface Option
            extends ExternalizableLite
            {
            }
        }
    }
