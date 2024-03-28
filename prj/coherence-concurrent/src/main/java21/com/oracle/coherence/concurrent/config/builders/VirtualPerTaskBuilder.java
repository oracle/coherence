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

import com.tangosol.config.expression.ParameterResolver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import java.util.function.Supplier;

/**
 * A {@link ParameterizedBuilder} for constructing a {@link NamedExecutorService}
 * wrapper that will construct a virtual-thread-per-task executor.
 *
 * @author rl  3.26.2024
 * @since 15.1.1.0
 */
public class VirtualPerTaskBuilder
        extends AbstractExecutorWithFactoryBuilder<NamedExecutorService>
    {
    // ----- ParameterizedBuilder interface ---------------------------------

    @Override
    public NamedExecutorService realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        String                    sName    = m_name.evaluate(resolver);
        ThreadFactory             factory  = instantiateThreadFactory(sName, resolver,
                                                                      loader, listParameters);
        Supplier<ExecutorService> supplier = factory == null
                                                 ? Executors::newVirtualThreadPerTaskExecutor
                                                 : () -> Executors.newThreadPerTaskExecutor(factory);
        NamedExecutorService      service  = new NamedExecutorService(sName, description(factory), supplier);

        register(service);

        return service;
        }

    // ----- helper methods -------------------------------------------------

    @Override
    protected ThreadFactory instantiateNamedThreadFactory(String sName)
        {
        return new VirtualNamedThreadFactory(sName);
        }

    // ----- inner class: VirtualNamedThreadFactory -------------------------

    /**
     * A {@link ThreadFactory} for virtual threads.
     */
    protected static class VirtualNamedThreadFactory
            extends NamedThreadFactory
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code VirtualNamedThreadFactory}.
         *
         * @param f_sName  the base name that will be used when naming
         *                 threads
         */
        public VirtualNamedThreadFactory(String f_sName)
            {
            super(f_sName);
            }

        // ----- NamedThreadFactory methods ---------------------------------

        @Override
        public Thread newThread(Runnable r, String sName)
            {
            Thread.Builder.OfVirtual builder = Thread.ofVirtual();
            if (sName != null)
                {
                builder.name(sName);
                }
            return builder.unstarted(r);
            }
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
        String sFactory = factory == null || VirtualNamedThreadFactory.class.equals(factory.getClass())
                          ? "default"
                          : factory.getClass().getName();

        return String.format("VirtualThreadPerTask(ThreadFactory=%s)", sFactory);
        }
    }
