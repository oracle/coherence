/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.util.OptionsByType;

import com.tangosol.util.function.Remote.Predicate;

import java.io.Serializable;

import java.time.Duration;

import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.Executor;

import java.util.function.Supplier;

/**
 * A base implementation of a {@link Task.Orchestration}.
 *
 * @param <T>  the type of results produced by a {@link Task}
 * @param <C>  the {@link TaskExecutorService} that created the {@link Task.Orchestration}
 *
 * @author bo
 * @since 21.12
 */
public abstract class AbstractOrchestration<T, C extends TaskExecutorService>
        implements Task.Orchestration<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Task.Orchestration}.
     *
     * @param taskExecutorService  the {@link TaskExecutorService} that created the {@link Task.Orchestration}
     * @param task                 the {@link Task} to be orchestrated
     */
    public AbstractOrchestration(C taskExecutorService, Task<T> task)
        {
        m_taskExecutorService = taskExecutorService;
        m_task                = task;

        // establish default orchestration properties
        m_sTaskId         = null;
        m_strategyBuilder = new ExecutionStrategyBuilder();
        m_strategy        = null;
        m_optionsByType   = OptionsByType.empty();
        m_properties      = null;
        m_retainDuration  = null;
        m_setSubscribers  = new HashSet<>();
    }

    // ----- Task.Orchestration interface -----------------------------------

    @Override
    public Task.Orchestration<T> concurrently()
        {
        m_strategyBuilder.concurrently();

        return this;
        }

    @Override
    public Task.Orchestration<T> sequentially()
        {
        m_strategyBuilder.sequentially();

        return this;
        }

    @Override
    public Task.Orchestration<T> filter(Predicate<? super TaskExecutorService.ExecutorInfo> predicate)
        {
        m_strategyBuilder.filter(predicate);

        return this;
        }

    @Override
    public Task.Orchestration<T> limit(int cLimit)
        {
        m_strategyBuilder.limit(cLimit);

        return this;
        }

    @Override
    public Task.Orchestration<T> as(String taskId)
        {
        m_sTaskId = taskId == null ? null : taskId.trim();

        return this;
        }

    @Override
    public Task.Orchestration<T> with(Task.Option... options)
        {
        m_optionsByType = OptionsByType.from(Task.Option.class, options);

        return this;
        }

    @Override
    public Task.Orchestration<T> retain(Duration duration)
        {
        m_retainDuration = duration;

        return this;
        }

    @Override
    public Task.SubscribedOrchestration<T> subscribe(Task.Subscriber<? super T> subscriber)
        {
        m_setSubscribers.add(subscriber);

        return this;
        }

    @Override
    public Task.Coordinator<T> submit()
        {
        Task.Collectable<T, T> collectable = collect();

        for (Task.Subscriber<? super T> subscriber : m_setSubscribers)
            {
            collectable.subscribe(subscriber);
            }

        return collectable.submit();
        }

    @Override
    public <V extends Serializable> Task.Orchestration<T> define(String sName, V value)
        {
        if (m_properties == null)
            {
            m_properties = getPropertiesSupplier().get();
            }
        m_properties.put(sName, value);

        return this;
        }

    // ----- abstract methods ------------------------------------------------

    /**
     * Obtain a {@link Task.Collectable} using the default collection mechanism.
     *
     * @return a {@link Task.Collectable}
     */
    protected abstract Task.Collectable<T, T> collect();

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the {@link Task.Orchestration} that created the {@link Task.Orchestration}.
     *
     * @return the {@link Task.Orchestration}
     */
    public C getTaskExecutorService()
        {
        return m_taskExecutorService;
        }

    /**
     * Obtains the {@link Task} to orchestrate.
     *
     * @return the {@link Task}
     */
    public Task<T> getTask()
        {
        return m_task;
        }

    /**
     * Obtains the {@link Task} identity.
     *
     * @return the {@link Task} identity
     */
    public String getTaskId()
        {
        return m_sTaskId;
        }

    /**
     * Obtains the {@link ExecutionStrategy} for the {@link Task} orchestration.
     *
     * @return the {@link ExecutionStrategy}
     */
    public ExecutionStrategy getAssignmentStrategy()
        {
        if (m_strategy == null)
            {
            m_strategy = m_strategyBuilder.build();
            }
        return m_strategy;
        }

    /**
     * Obtains the {@link OptionsByType} for the {@link Task} orchestration.
     *
     * @return the {@link OptionsByType}
     */
    public OptionsByType<Task.Option> getOptionsByType()
        {
        return m_optionsByType;
        }

    /**
     * Obtains the {@link Task.Properties} for the {@link Task} orchestration.
     *
     * @return the {@link Task.Properties}
     */
    public Task.Properties getProperties()
        {
        return m_properties;
        }

    /**
     * Obtains the {@link Duration} the {@link Task} and it's results will live for once completed or an error occurs.
     *
     * @return the {@link Duration}
     */
    public Duration getRetainDuration()
        {
        return m_retainDuration;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a {@link Supplier} of a {@link Task.Properties} instance.
     *
     * @return a {@link Supplier} of a {@link Task.Properties} instance
     */
    protected Supplier<Task.Properties> getPropertiesSupplier()
        {
        return TaskProperties::new;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link TaskExecutorService} that created the {@link Task.Orchestration}.
     */
    protected C m_taskExecutorService;

    /**
     * The {@link Task} to execute.
     */
    protected Task<T> m_task;

    /**
     * The {@link Task} identity.
     */
    protected String m_sTaskId;

    /**
     * The {@link ExecutionStrategyBuilder} used to build the {@link ExecutionStrategy}.
     */
    protected ExecutionStrategyBuilder m_strategyBuilder;

    /**
     * The {@link ExecutionStrategy} to determine the {@link Executor}s to use for executing the {@link Task}.
     */
    protected ExecutionStrategy m_strategy;

    /**
     * The {@link Task.Option}s for {@link Task.Orchestration}.
     */
    protected OptionsByType<Task.Option> m_optionsByType;

    /**
     * The {@link Task.Properties} for the orchestrated {@link Task}.
     */
    protected Task.Properties m_properties;

    /**
     * The optional {@link Duration} to retain the task after it is completed.
     * <code>null</code> when not required (to retain the task)
     */
    protected Duration m_retainDuration;

    /**
     * The set of {@link Task.Subscriber}s for the orchestrated {@link Task}.
     */
    protected Set<Task.Subscriber<? super T>> m_setSubscribers;
    }
