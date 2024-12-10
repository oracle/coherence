/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.builders;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.ParameterResolver;

import java.util.concurrent.ThreadFactory;

/**
 * A {@link ParameterizedBuilder} for constructing a {@link ThreadFactory}.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public class ThreadFactoryBuilder
        implements ParameterizedBuilder<ThreadFactory>
    {
    // ----- ParameterizedBuilder interface ---------------------------------

    @Override
    public ThreadFactory realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        return m_bldr.realize(resolver, loader, listParameters);
        }

    // ----- setters --------------------------------------------------------

    /**
     * Set the {@link ParameterizedBuilder} that will be used to construct
     * the {@link ThreadFactory}.
     *
     * @param bldr  the {@link ParameterizedBuilder} that will be used to construct
     *              the {@link ThreadFactory}
     */
    @SuppressWarnings("unused")
    @Injectable("instance")
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
