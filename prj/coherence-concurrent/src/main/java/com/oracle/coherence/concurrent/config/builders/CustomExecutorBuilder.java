/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
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

import com.tangosol.config.expression.ParameterResolver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import java.util.function.Supplier;

/**
 * A {@link ParameterizedBuilder} for constructing an {@link ExecutorService}.
 *
 * @author rl  5.16.24
 * @since 14.1.2.0.0
 */
public class CustomExecutorBuilder
        extends AbstractExecutorBuilder<NamedExecutorService>
    {
    // ----- ParameterizedBuilder interface ---------------------------------

    @Override
    public NamedExecutorService realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        String                    sName    = m_name.evaluate(resolver);
        Supplier<ExecutorService> supplier = () -> m_bldr.realize(resolver, loader, listParameters);
        NamedExecutorService      service  = new NamedExecutorService(sName, "CustomExecutorService", supplier);

        register(service);

        return service;
        }

    // ----- setters --------------------------------------------------------

    /**
     * Set the {@link ParameterizedBuilder} that will be used to construct
     * the {@link ExecutorService}.
     *
     * @param bldr  the {@link ParameterizedBuilder} that will be used to construct
     *              the {@link ExecutorService}
     */
    @SuppressWarnings("unused")
    @Injectable("instance")
    public void setInstanceBuilder(ParameterizedBuilder<ExecutorService> bldr)
        {
        m_bldr = bldr;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Creates the description for this executor.
     *
     * @param factory  the {@link ThreadFactory}, if any
     *
     * @return the description for this executor
     */
    protected String description(ThreadFactory factory)
        {
        String sFactory = factory == null || NamedThreadFactory.class.equals(factory.getClass())
                          ? "default"
                          : factory.getClass().getName();

        return String.format("CachedThreadPool(ThreadFactory=%s)", sFactory);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A {@link ParameterizedBuilder} that creates a {@link ExecutorService}.
     */
    protected ParameterizedBuilder<ExecutorService> m_bldr;
    }
