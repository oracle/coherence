/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.internal.health.HealthCheckDependencies;
import com.tangosol.io.SerializerFactory;

/**
 * The ServiceDependencies interface defines externally provided dependencies
 * for {@link Service clustered services}.
 *
 * @author pfm  2011.05.12
 * @since Coherence 12.1.3
 */
public interface ServiceDependencies
    {
    /**
     * Obtain the priority of the event dispatcher thread. The returned value
     * must be an integer between {@link Thread#MIN_PRIORITY} and
     * {@link Thread#MAX_PRIORITY}.
     *
     * @return the event dispatcher thread priority
     */
    public int getEventDispatcherThreadPriority();

    /**
     * Obtain the {@link SerializerFactory} used by this service.
     *
     * @return the SerializerFactory
     */
    public SerializerFactory getSerializerFactory();

    /**
     * Obtain a default request timeout value. This is also a default value for
     * {@link PriorityTask#getRequestTimeoutMillis() PriorityTasks}.
     *
     * @return the default request timeout
     */
    public long getRequestTimeoutMillis();

    /**
     * Obtain the amount of time that a task can execute before it's considered "hung".
     *
     * @return the task hung threshold
     */
    public long getTaskHungThresholdMillis();

    /**
     * Obtain the default task timeout value. This value is used as a default
     * for {@link PriorityTask#getExecutionTimeoutMillis() PriorityTasks}.
     *
     * @return the default task timeout
     */
    public long getTaskTimeoutMillis();

    /**
     * Obtain the priority of the service thread. The returned value must be an
     * integer between {@link Thread#MIN_PRIORITY} and {@link Thread#MAX_PRIORITY}.
     *
     * @return the service thread priority
     */
    public int getThreadPriority();

    /**
     * Obtain the number of background worker threads that will be created when
     * the service is started.
     * <ul>
     *   <li>A value of zero indicates that all requests should be processed by
     *       the service thread.</li>
     *   <li>A positive value indicates that a fixed-size thread pool of the
     *       specified size should be used to process service requests.</li>
     *   <li>A negative value indicates that requests should be processed either
     *       on the service thread or transport threads where possible.</li>
     * </ul>
     *
     * @return the worker thread count
     *
     * @deprecated Since 12.2.1, replaced by returning the same value from
     * {@link #getWorkerThreadCountMin()} and {@link #getWorkerThreadCountMax()}.
     */
    public int getWorkerThreadCount();

    /**
     * Obtain the maximum number of background worker threads.
     * <ul>
     *   <li>A value of zero indicates that all requests should be processed by
     *       the service thread.</li>
     *   <li>A positive value indicates that a dynamic thread pool of the
     *       maximum specified size should be used to process service requests.</li>
     *   <li>A negative value indicates that requests should be processed either
     *       on the service thread or transport threads where possible.</li>
     * </ul>
     *
     * @return the maximum worker thread count
     */
    public int getWorkerThreadCountMax();

    /**
     * Obtain the minimum number of background worker threads.
     * <ul>
     *   <li>A value of zero indicates that all requests should be processed by
     *       the service thread.</li>
     *   <li>A positive value indicates that a dynamic thread pool of the
     *       minimum specified size should be used to process service requests.</li>
     *   <li>A negative value indicates that requests should be processed either
     *       on the service thread or transport threads where possible.</li>
     * </ul>
     *
     * @return the minimum worker thread count
     */
    public int getWorkerThreadCountMin();

    /**
     * Obtain the priority of the worker threads. The returned value must be an
     * integer between {@link Thread#MIN_PRIORITY} and {@link Thread#MAX_PRIORITY}.
     *
     * @return the worker thread priority
     */
    public int getWorkerThreadPriority();

    /**
     * Returns the service's {@link HealthCheckDependencies}.
     *
     * @return  the service's {@link HealthCheckDependencies}
     */
    public HealthCheckDependencies getHealthCheckDependencies();
    }
