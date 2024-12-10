/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.builders;

import com.oracle.coherence.concurrent.config.NamedExecutorService;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.function.Supplier;

/**
 * A {@link ParameterizedBuilder} for constructing a {@link NamedExecutorService}
 * wrapper that will construct a work stealing pool executor.
 *
 * @author rl  11.26.21
 * @since 21.12
 */
public class WorkStealingBuilder
        extends AbstractExecutorBuilder<NamedExecutorService>
    {
    public NamedExecutorService realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        String                    sName        = m_name.evaluate(resolver);
        int                       nParallelism = m_parallelism == null
                                                     ? Runtime.getRuntime().availableProcessors()
                                                     : m_parallelism.evaluate(resolver);
        Supplier<ExecutorService> supplier     = () -> Executors.newWorkStealingPool(nParallelism);
        NamedExecutorService      service      = new NamedExecutorService(sName, description(nParallelism), supplier);

        register(service);

        return service;
        }

    // ----- setters --------------------------------------------------------

    /**
     * Set the number of threads to be used by this executor.
     *
     * @param parallelism  the parallelism of this executor
     */
    @SuppressWarnings("unused")
    @Injectable("parallelism")
    public void setParallelism(Expression<Integer> parallelism)
        {
        m_parallelism = parallelism;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Creates the description for this executor.
     *
     * @param nParallelism  the parallelism for the executor
     *
     * @return the description for this executor
     */
    protected String description(int nParallelism)
        {
        return String.format("WorkStealingThreadPool(Parallelism=%s)", nParallelism);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The parallelism of the executor.
     */
    protected Expression<Integer> m_parallelism;
    }
