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

import com.tangosol.config.expression.ParameterResolver;

import java.util.concurrent.Executors;

/**
 * A {@link ParameterizedBuilder} for constructing a {@link NamedExecutorService}
 * wrapper that will construct a cached thread pool.
 *
 * @author rl  11.26.21
 * @since 21.12
 */
public class CachedBuilder
        extends AbstractExecutorWithFactoryBuilder<NamedExecutorService>
    {
    // ----- ParameterizedBuilder interface ---------------------------------

    @Override
    public NamedExecutorService realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        NamedExecutorService service;

        if (m_bldr == null)
            {
            service = new NamedExecutorService(m_sName, "CachedThreadPool(ThreadFactory=false)", Executors::newCachedThreadPool);
            }
        else
            {
            service = new NamedExecutorService(m_sName, "CachedThreadPool(ThreadFactory=true)", () ->
                    Executors.newCachedThreadPool(m_bldr.realize(resolver, loader, listParameters)));
            }

        register(service);

        return service;
        }
    }
