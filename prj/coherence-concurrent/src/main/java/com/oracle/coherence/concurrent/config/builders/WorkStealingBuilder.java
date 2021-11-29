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

import com.tangosol.config.expression.ParameterResolver;

import java.util.concurrent.Executors;

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
        NamedExecutorService service;

        int nParallelism = m_nParallelism;

        service = new NamedExecutorService(m_sName, "WorkStealingThreadPool(Parallelism=" + nParallelism + ')',
                    () -> Executors.newWorkStealingPool(nParallelism));

        register(service);

        return service;
        }

    // ----- setters --------------------------------------------------------

    /**
     * Set the number of threads to be used by this executor.
     *
     * @param nParallelism  the parallelism of this executor
     */
    @SuppressWarnings("unused")
    @Injectable("parallelism")
    public void setParallelism(int nParallelism)
        {
        if (nParallelism <= 0)
            {
            throw new IllegalArgumentException("parallelism must be greater than or equal to zero");
            }
        m_nParallelism = nParallelism;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The parallelism of the executor.
     */
    protected int m_nParallelism = Runtime.getRuntime().availableProcessors();
    }
