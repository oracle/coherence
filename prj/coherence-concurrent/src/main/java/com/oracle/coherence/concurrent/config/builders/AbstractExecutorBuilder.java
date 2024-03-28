/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.builders;

import com.oracle.coherence.concurrent.config.ConcurrentConfiguration;
import com.oracle.coherence.concurrent.config.NamedExecutorService;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;

/**
 * Base {@link ParameterizedBuilder} fpr named executors.
 *
 * @param <T>  the builder type
 *
 * @author rl  11.26.21
 * @since 21.12
 */
public abstract class AbstractExecutorBuilder<T>
        implements ParameterizedBuilder<T>
    {
    // ----- setters --------------------------------------------------------

    /**
     * Sets the logical name of this executor.
     *
     * @param name  the logical name of this executor
     */
    @Injectable("name")
    public void setName(Expression<String> name)
        {
        m_name = name;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Register the {@link NamedExecutorService} with the local {@link ClusteredExecutorService}.
     *
     * @param namedExecutorService  the {@link NamedExecutorService} to be registered
     */
    protected void register(NamedExecutorService namedExecutorService)
        {
        ConcurrentConfiguration.get().addNamedExecutorService(namedExecutorService);
        }

    // ----- data members ---------------------------------------------------
    /**
     * The name of the executor.
     */
    protected Expression<String> m_name;
    }
