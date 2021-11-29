/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.management;

import java.util.concurrent.Executor;

import com.tangosol.net.management.annotation.Description;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;


/**
 * ExecutorMBean provides a monitor interface for the {@link Executor} statistics.
 *
 * @author bo, lh 2016.10.13
 * @since 21.12
 */
@SuppressWarnings("unused")
@Description("Provides Executor statistics.")
public interface ExecutorMBean
    {
    // ----- operations -----------------------------------------------------

    /**
     * Reset the statistics.
     */
    @Description("Reset the statistics.")
    void resetStatistics();

    // ----- statistics -----------------------------------------------------

    /**
     * Get the member id where the executor is running.
     *
     * @return the member id where the executor is running
     */
    @Description("The member id where the executor is running.")
    int getMemberId();

    /**
     * Return the logical name of the registered {@link ExecutorService}.
     *
     * @return the logical name of the registered {@link ExecutorService}
     */
    @Description("The logical name of this executor.")
    String getExecutorName();

    /**
     * Return the unique ID of the registered {@link ExecutorService}.
     *
     * @return the unique ID of the registered {@link ExecutorService}
     */
    @Description("The unique ID of this executor.")
    String getExecutorId();

    /**
     * Return the details of the registered {@link ExecutorService}.
     * Details will include the type of {@link ExecutorService}, how many
     * threads (if relevant), and if a {@link ThreadFactory} was defined.
     *
     * @return the unique ID of the registered {@link ExecutorService}
     */
    @Description("The details of this executor.")
    String getExecutorDetails();

    /**
     * Get the location where the executor is running.
     *
     * @return the location where the executor is running
     */
    @Description("The location where the executor is running.")
    String getLocation();

    /**
     * Get the state of the executor.
     *
     * @return the executor state
     */
    @Description("The state of the executor.")
    String getState();

    /**
     * Get the completed tasks count for the executor.
     *
     * @return the completed tasks count for the executor
     */
    @Description("The completed tasks count.")
    long getTasksCompletedCount();

    /**
     * Get the number of tasks rejected by the executor.
     *
     * @return the rejected tasks count for the executor
     */
    @Description("The tasks rejected count.")
    long getTasksRejectedCount();

    /**
     * Get the in progress tasks count for the executor.
     *
     * @return the in progress tasks count for the executor
     */
    @Description("The in progress tasks count.")
    long getTasksInProgressCount();

    /**
     * Return a boolean to indicate whether the executor trace logging
     * is enabled (true) or not (false).
     *
     * By default, the executor trace logging is disabled. You can enable
     * it by either setting the
     * "coherence.executor.trace.logging" system property or the "TraceLogging"
     * attribute in the ExecutorMBean through JMX or management over REST.
     *
     * @return whether executor trace logging is enabled (true) or not (false)
     */
    @Description("Indicate the executor traceLogging is enabled (true) or not (false).")
    boolean isTraceLogging();

    /**
     * Set the trace to true to enable executor trace logging; false to
     * disable executor trace logging.
     *
     * @param fTrace  a flag to indicate whether to enable (true) executor
     *                trace logging or not (false)
     */
    @Description("Set the trace to true to enable executor trace logging; false to disable executor trace logging.")
    void setTraceLogging(boolean fTrace);
    }
