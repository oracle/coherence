/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.management;

import com.tangosol.net.management.annotation.Description;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.management.annotation.MetricsTag;
import com.tangosol.net.management.annotation.MetricsValue;

import com.tangosol.net.metrics.MBeanMetric;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * ExecutorMBean provides a monitor interface for the {@link Executor} statistics.
 *
 * @author bo, lh 2016.10.13
 * @since 21.12
 */
@SuppressWarnings("unused")
@MetricsScope(MBeanMetric.Scope.VENDOR)
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
    @MetricsTag("memberId")
    String getMemberId();

    /**
     * Return the logical name of the registered {@link ExecutorService}.
     *
     * @return the logical name of the registered {@link ExecutorService}
     */
    @Description("The logical name of this executor.")
    String getName();

    /**
     * Return the unique ID of the registered {@link ExecutorService}.
     *
     * @return the unique ID of the registered {@link ExecutorService}
     */
    @Description("The unique ID of this executor.")
    String getId();

    /**
     * Return the description of the registered {@link ExecutorService}.
     *
     * @return the unique description of the registered {@link ExecutorService}
     */
    @Description("The description of this executor.")
    String getDescription();

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
     * Get the state of the executor as an integer.
     *
     * @return the executor state as an integer
     *
     * @since 22.06
     */
    @Description("The State of the executor. "
                 + "The value of 1 (JOINING) indicates executor is joining the orchestration."
                 + "The value of 2 (RUNNING) indicates executor is accepting and executing tasks."
                 + "The value of 3 (CLOSING_GRACEFULLY) has commenced graceful closing. No new tasks will be accepted, but existing ones will run to completion."
                 + "The value of 4 (CLOSING) indicates Executor has commenced closing."
                 + "The value of 5 indicates CLOSED."
                 + "The value of 6 indicates executor is REJECTING tasks."
    )
    @MetricsValue
    int getStateCode();

    /**
     * Get the completed tasks count for the executor.
     *
     * @return the completed tasks count for the executor
     */
    @Description("The completed tasks count.")
    @MetricsValue
    long getTasksCompletedCount();

    /**
     * Get the number of tasks rejected by the executor.
     *
     * @return the rejected tasks count for the executor
     */
    @Description("The tasks rejected count.")
    @MetricsValue
    long getTasksRejectedCount();

    /**
     * Get the in progress tasks count for the executor.
     *
     * @return the in progress tasks count for the executor
     */
    @Description("The in progress tasks count.")
    @MetricsValue
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

    // ----- constants ------------------------------------------------------

    /**
     * A string representing the "type" part of <code>ObjectName</code> for the ExecutorMBean.
     */
    String EXECUTOR_TYPE = "type=Executor";

    /**
     * A string representing the "name" part of <code>ObjectName</code> for the ExecutorMBean.
     */
    String EXECUTOR_NAME = ",name=";
    }
