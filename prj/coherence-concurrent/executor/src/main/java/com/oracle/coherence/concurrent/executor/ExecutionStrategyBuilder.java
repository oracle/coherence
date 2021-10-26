/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.tangosol.util.function.Remote.Predicate;

import java.util.concurrent.Executor;

/**
 * A builder of {@link ExecutionStrategy}s.
 *
 * @since 21.12
 */
public class ExecutionStrategyBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a default {@link ExecutionStrategyBuilder}, that will execute
     * {@link Task}s concurrently on all available {@link Executor}s.
     */
    public ExecutionStrategyBuilder()
        {
        m_cLimit               = -1;    // -1 means all available executors
        m_predicate            = Predicates.always();
        m_fConcurrentExecution = true;
        }

    /**
     * Build an {@link ExecutionStrategy}.
     *
     * @return the {@link ExecutionStrategy}
     */
    public ExecutionStrategy build()
        {
        return new StandardExecutionStrategy(m_cLimit, m_predicate, m_fConcurrentExecution);
        }

    /**
     * Limit the {@link ExecutionStrategy} to use only the specified number of
     * {@link Executor}s.
     *
     * @param n  the number of {@link Executor}s to use
     *
     * @return the {@link ExecutionStrategyBuilder} to permit fluent-style method calls
     */
    public ExecutionStrategyBuilder limit(int n)
        {
        m_cLimit = n < 0 ? -1 : n;

        return this;
        }

    /**
     * Limit the {@link ExecutionStrategy} to use only those {@link Executor}s that
     * satisfy the specified {@link Predicate}.
     *
     * @param predicate  the {@link TaskExecutorService.ExecutorInfo} predicate
     *
     * @return the {@link ExecutionStrategyBuilder} to permit fluent-style method calls
     */
    public ExecutionStrategyBuilder filter(Predicate<? super TaskExecutorService.ExecutorInfo> predicate)
        {
        m_predicate = predicate == null ? Predicates.always() : predicate;

        return this;
        }

    /**
     * Set this {@link ExecutionStrategyBuilder} so that created
     * {@link ExecutionStrategy}s will execute {@link Task}s in a sequential
     * nature.
     *
     * @return the {@link ExecutionStrategyBuilder} to permit fluent-style method calls
     */
    public ExecutionStrategyBuilder sequentially()
        {
        m_fConcurrentExecution = false;

        return this;
        }

    /**
     * Set this {@link ExecutionStrategyBuilder} so that created
     * {@link ExecutionStrategy}s will execute {@link Task}s in a concurrent nature.
     *
     * @return the {@link ExecutionStrategyBuilder} to permit fluent-style method calls
     */
    public ExecutionStrategyBuilder concurrently()
        {
        m_fConcurrentExecution = true;

        return this;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of {@link Executor}s to execute a {@link Task} (-1 means all).
     */
    protected int m_cLimit;

    /**
     * The {@link Predicate} for choosing candidate {@link Executor}s to execute
     * a {@link Task}.
     */
    protected Predicate<? super TaskExecutorService.ExecutorInfo> m_predicate;

    /**
     * Whether a {@link Task} should be executed concurrently or sequentially on
     * the {@link Executor}s.
     */
    protected boolean m_fConcurrentExecution;
    }
