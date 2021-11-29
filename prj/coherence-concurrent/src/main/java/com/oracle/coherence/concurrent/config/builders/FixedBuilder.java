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
        int nThreadCount = m_threadCount;

        NamedExecutorService service;

        if (m_bldr == null)
            {
            service = new NamedExecutorService(m_sName,
                    "FixedThreadPool(ThreadCount=" + nThreadCount + ", ThreadFactory=false)",
                    () -> Executors.newFixedThreadPool(nThreadCount));
            }
        else
            {
            service = new NamedExecutorService(m_sName,
                    "FixedThreadPool(ThreadCount=" + nThreadCount + ", ThreadFactory=true)",
                    () -> Executors.newFixedThreadPool(
                            m_threadCount, m_bldr.realize(resolver, loader, listParameters)));
            }

        register(service);

        return service;
        }

    // ----- setters --------------------------------------------------------

    /**
     * Set the number of threads to be used by this executor.
     *
     * @param cThreadCount  the number of threads to be used by this executor
     */
    @SuppressWarnings("unused")
    @Injectable("thread-count")
    public void setThreadCount(int cThreadCount)
        {
        if (cThreadCount <= 0)
            {
            throw new IllegalArgumentException("thread count must be greater than or equal to zero");
            }
        m_threadCount = cThreadCount;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of threads;
     */
    protected int m_threadCount;
    }
