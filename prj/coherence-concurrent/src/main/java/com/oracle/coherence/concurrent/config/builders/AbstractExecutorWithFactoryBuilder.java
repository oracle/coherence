/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.builders;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;

import java.util.concurrent.ThreadFactory;

/**
 * Base {@link ParameterizedBuilder} for named executors that support
 * {@link ThreadFactory thread factories}.
 *
 * @param <T>  the builder type
 *
 * @author rl  11.26.21
 * @since 21.12
 */
public abstract class AbstractExecutorWithFactoryBuilder<T>
        extends AbstractExecutorBuilder<T>
    {
    // ----- setters --------------------------------------------------------

    /**
     * Sets the {@link ThreadFactory} instance builder.
     *
     * @param bldr  the {@link ThreadFactory} instance builder
     */
    @SuppressWarnings("unused")
    @Injectable("thread-factory")
    public void setInstanceBuilder(ParameterizedBuilder<ThreadFactory> bldr)
        {
        m_bldr = bldr;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A {@link ParameterizedBuilder} that creates a {@link ThreadFactory}.
     */
    protected ParameterizedBuilder<ThreadFactory> m_bldr;
    }
