/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.health.HealthCheckDependencies;
import com.tangosol.io.SerializerFactory;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

/**
 * A base implementation of ServiceDependencies.
 *
 * @author bko 2013.05.15
 * @since Coherence 12.1.3
 */
public class DefaultServiceDependencies
        implements ServiceDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultServiceDependencies object.
     */
    public DefaultServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultServiceDependencies object, copying the values from the
     * specified ServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultServiceDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        if (deps != null)
            {
            m_nEventDispatcherThreadPriority = deps.getEventDispatcherThreadPriority();
            m_cRequestTimeoutMillis          = deps.getRequestTimeoutMillis();
            m_serializerFactory              = deps.getSerializerFactory();
            m_cTaskHungThresholdMillis       = deps.getTaskHungThresholdMillis();
            m_cTaskTimeoutMillis             = deps.getTaskTimeoutMillis();
            m_nThreadPriority                = deps.getThreadPriority();
            m_cWorkerThreads                 = deps.getWorkerThreadCount();
            m_cWorkerThreadsMax              = deps.getWorkerThreadCountMax();
            m_cWorkerThreadsMin              = deps.getWorkerThreadCountMin();
            m_nWorkerPriority                = deps.getWorkerThreadPriority();
            m_healthCheckDependencies        = deps.getHealthCheckDependencies();
            if (deps instanceof DefaultServiceDependencies)
                {
                DefaultServiceDependencies sDeps = (DefaultServiceDependencies) deps;
                m_fDeprecatedThreadConfiguration = sDeps.m_fDeprecatedThreadConfiguration;
                m_fThreadConfiguration           = sDeps.m_fThreadConfiguration;
                }
            }
        }

    // ----- ServiceDependencies interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getEventDispatcherThreadPriority()
        {
        return m_nEventDispatcherThreadPriority;
        }

    /**
     * Set the event dispatcher's thread priority.
     *
     * @param  nPriority  the event dispatcher's thread priority
     */
    @Injectable("event-dispatcher-priority")
    public void setEventDispatcherThreadPriority(int nPriority)
        {
        m_nEventDispatcherThreadPriority = nPriority;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRequestTimeoutMillis()
        {
        return m_cRequestTimeoutMillis;
        }

    /**
     * Set the request timeout.
     *
     * @param cMillis  the request timeout
     */
    @Injectable("request-timeout")
    public void setRequestTimeoutMillis(long cMillis)
        {
        m_cRequestTimeoutMillis = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SerializerFactory getSerializerFactory()
        {
        return m_serializerFactory;
        }

    /**
     * Set the SerializerFactory
     *
     * @param factory  the SerializerFactory
     */
    @Injectable("serializer")
    public void setSerializerFactory(SerializerFactory factory)
        {
        m_serializerFactory = factory;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTaskHungThresholdMillis()
        {
        return m_cTaskHungThresholdMillis;
        }

    /**
     * Set the task hung threshold.
     *
     * @param cMillis  the task hung threshold
     */
    @Injectable("task-hung-threshold")
    public void setTaskHungThresholdMillis(long cMillis)
        {
        m_cTaskHungThresholdMillis = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTaskTimeoutMillis()
        {
        return m_cTaskTimeoutMillis;
        }

    /**
     * Set the task timeout.
     *
     * @param cMillis  the task timeout
     */
    @Injectable("task-timeout")
    public void setTaskTimeoutMillis(long cMillis)
        {
        m_cTaskTimeoutMillis = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadPriority()
        {
        return m_nThreadPriority;
        }

    /**
     * Set the thread priority for the Service thread.
     *
     * @param nPriority  the thread priority
     */
    @Injectable("service-priority")
    public void setThreadPriority(int nPriority)
        {
        m_nThreadPriority = nPriority;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWorkerThreadCount()
        {
        return m_cWorkerThreads;
        }

    /**
     * Set the worker thread count.
     *
     * @param cThreads  the thread count
     *
     * @deprecated Since 12.2.1, replaced by {@link #setWorkerThreadCountMax(int)}()}
     * and {@link #setWorkerThreadCountMin(int)}.
     */
    @Injectable("thread-count")
    public void setWorkerThreadCount(int cThreads)
        {
        m_fDeprecatedThreadConfiguration = true;
        m_cWorkerThreads                 = cThreads;
        }

    @Override
    public int getWorkerThreadCountMax()
        {
        return m_cWorkerThreadsMax;
        }

    /**
     * Set the maximum worker thread count.
     *
     * @param cThreads  the maximum thread count
     */
    @Injectable("thread-count-max")
    public void setWorkerThreadCountMax(int cThreads)
        {
        m_fThreadConfiguration = true;
        m_cWorkerThreadsMax    = cThreads;
        }

    @Override
    public int getWorkerThreadCountMin()
        {
        return m_cWorkerThreadsMin;
        }

    /**
     * Set the minimum worker thread count.
     *
     * @param cThreads  the minimum thread count
     */
    @Injectable("thread-count-min")
    public void setWorkerThreadCountMin(int cThreads)
        {
        m_fThreadConfiguration = true;
        m_cWorkerThreadsMin    = cThreads;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWorkerThreadPriority()
        {
        return m_nWorkerPriority;
        }

    /**
     * Set the thread priority for the worker threads.
     *
     * @param nPriority  the thread priority
     */
    @Injectable("worker-priority")
    public void setWorkerThreadPriority(int nPriority)
        {
        m_nWorkerPriority = nPriority;
        }

    /**
     * Returns the service's {@link HealthCheckDependencies}.
     *
     * @return  the service's {@link HealthCheckDependencies}
     */
    public HealthCheckDependencies getHealthCheckDependencies()
        {
        return m_healthCheckDependencies;
        }

    /**
     * Set the service's {@link HealthCheckDependencies}.
     *
     * @param deps  the service's {@link HealthCheckDependencies}
     */
    @Injectable("health")
    public void setHealthCheckDependencies(HealthCheckDependencies deps)
        {
        m_healthCheckDependencies = deps;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
                + "{EventDispatcherThreadPriority=" + getEventDispatcherThreadPriority()
                + ", RequestTimeoutMillis=" + getRequestTimeoutMillis()
                + ", SerializerFactory=" + getSerializerFactory()
                + ", TaskHungThresholdMillis=" + getTaskHungThresholdMillis()
                + ", TaskTimeoutMillis=" + getTaskTimeoutMillis()
                + ", ThreadPriority=" + getThreadPriority()
                + ", WorkerThreadCount=" + getWorkerThreadCount()
                + ", WorkerThreadCountMax=" + getWorkerThreadCountMax()
                + ", WorkerThreadCountMin=" + getWorkerThreadCountMin()
                + ", WorkerThreadPriority=" + getWorkerThreadPriority() + "}";
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate the dependencies.
     *
     * @return this object
     */
    public DefaultServiceDependencies validate()
        {
        Base.azzert(getEventDispatcherThreadPriority() >= Thread.MIN_PRIORITY,
                "EventDispatcherThreadPriority cannot be less than " + Thread.MIN_PRIORITY);
        Base.azzert(getEventDispatcherThreadPriority() <= Thread.MAX_PRIORITY,
                "EventDispatcherThreadPriority cannot be more than " + Thread.MAX_PRIORITY);
        Base.azzert(getRequestTimeoutMillis() >= 0, "RequestTimeout cannot be less than 0");
        Base.azzert(getTaskHungThresholdMillis() >= 0, "TaskHungThreshold cannot be less than 0");
        Base.azzert(getTaskTimeoutMillis() >= 0, "TaskTimeoutMillis cannot be less than 0");
        Base.azzert(getThreadPriority() >= Thread.MIN_PRIORITY,
                "ThreadPriority cannot be less than " + Thread.MIN_PRIORITY);
        Base.azzert(getThreadPriority() <= Thread.MAX_PRIORITY,
                "ThreadPriority cannot be more than " + Thread.MAX_PRIORITY);
        Base.azzert(getWorkerThreadPriority() >= Thread.MIN_PRIORITY,
                "WorkerThreadPriority cannot be less than " + Thread.MIN_PRIORITY);
        Base.azzert(getWorkerThreadPriority() <= Thread.MAX_PRIORITY,
                "WorkerThreadPriority cannot be more than " + Thread.MAX_PRIORITY);

        // if deprecated option is explicitly set, it takes precedence
        // of new settings
        if (m_fDeprecatedThreadConfiguration)
            {
            int cThreadCount = getWorkerThreadCount();

            setWorkerThreadCountMin(cThreadCount);
            setWorkerThreadCountMax(cThreadCount);
            }
        else
            {
            int cWorkerMin = getWorkerThreadCountMin();
            int cWorkerMax = getWorkerThreadCountMax();

            if (cWorkerMin > cWorkerMax)
                {
                // set max == min
                setWorkerThreadCountMax(cWorkerMin);
                }
            }

        Base.azzert(getWorkerThreadCountMax() >= getWorkerThreadCountMin(),
                    "WorkerThreadCountMax value " + getWorkerThreadCountMax() + " must be greater than or equal to WorkerThreadCountMin value " + getWorkerThreadCountMin());

        if (getWorkerThreadCountMin() < 0)
            {
            Base.azzert(getWorkerThreadCountMax() == Integer.MAX_VALUE ||
                        getWorkerThreadCountMax() == -1,
                        "Inconsistent WorkerThreadCountMax and WorkerThreadCountMin");
            }

        return this;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The event dispatcher thread priority.
     */
    private int m_nEventDispatcherThreadPriority = Thread.MAX_PRIORITY;

    /**
     * The request timeout.
     */
    private long m_cRequestTimeoutMillis;

    /**
     * The serializer factory.
     */
    private SerializerFactory m_serializerFactory;

    /**
     * The task hung threshold.
     */
    private long m_cTaskHungThresholdMillis;

    /**
     * The task timeout.
     */
    private long m_cTaskTimeoutMillis;

    /**
     * The Service thread priority.
     */
    private int m_nThreadPriority = Thread.MAX_PRIORITY;

    /**
     * The worker thread count or negative for pre-processing.
     */
    private int m_cWorkerThreads;

    /**
     * The maximum worker thread count or negative for pre-processing.
     */
    private int m_cWorkerThreadsMax = Integer.MAX_VALUE;

    /**
     * The minimum worker thread count or negative for pre-processing.
     */
    private int m_cWorkerThreadsMin;

    /**
     * The worker threads priority.
     */
    private int m_nWorkerPriority = Thread.NORM_PRIORITY;

    /**
     * Flag indicating deprecated {@code thread-config} used.
     *
     * @since 14.1.1.2206.5
     */
    private boolean m_fDeprecatedThreadConfiguration;

    /**
     * Flag indicating min/max {@code thread-config} used.
     *
     * @since 14.1.1.2206.5
     */
    private boolean m_fThreadConfiguration;

    /**
     * The service health check dependencies.
     */
    private HealthCheckDependencies m_healthCheckDependencies;
    }
