/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.builders;

import com.oracle.coherence.concurrent.config.NamedExecutorService;

import com.oracle.coherence.concurrent.executor.util.NamedThreadFactory;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import java.util.function.Supplier;

/**
 * A {@link ParameterizedBuilder} for constructing a {@link NamedExecutorService}
 * wrapper that will construct a fixed thread pool executor.
 *
 * @author rl  11.26.21
 * @since 21.12
 */
public class FixedBuilder
        extends AbstractExecutorWithFactoryBuilder<NamedExecutorService>
    {
    // ----- ParameterizedBuilder interface ---------------------------------

    @Override
    public NamedExecutorService realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        String                    sName        = m_name.evaluate(resolver);
        int                       cThreadCount = m_threadCount.evaluate(resolver);
        ThreadFactory             factory      = instantiateThreadFactory(sName, resolver,
                                                     loader, listParameters);
        Supplier<ExecutorService> supplier     = factory == null
                                                     ? () -> Executors.newFixedThreadPool(cThreadCount)
                                                     : () -> Executors.newFixedThreadPool(cThreadCount, factory);
        NamedExecutorService      service      = new NamedExecutorService(sName, description(cThreadCount, factory), supplier);

        register(service);

        return service;
        }

    // ----- setters --------------------------------------------------------

    /**
     * Set the number of threads to be used by this executor.
     *
     * @param threadCount  the number of threads to be used by this executor
     */
    @SuppressWarnings("unused")
    @Injectable("thread-count")
    public void setThreadCount(Expression<Integer> threadCount)
        {
        m_threadCount = threadCount;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Creates the description for this executor.
     *
     * @param cThreadCount  the configured thread count
     * @param factory       the {@link ThreadFactory}, if any
     *
     * @return the description for this executor
     */
    protected String description(int cThreadCount, ThreadFactory factory)
        {
        String sFactory = factory == null || NamedThreadFactory.class.equals(factory.getClass())
                          ? "default"
                          : factory.getClass().getName();

        return String.format("FixedThreadPool(ThreadCount=%s, ThreadFactory=%s)", cThreadCount, sFactory);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of threads.
     */
    protected Expression<Integer> m_threadCount;
    }
