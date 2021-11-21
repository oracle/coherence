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

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;


/**
 * A {@link ParameterizedBuilder} for constructing {@link ExecutorService}
 * instances.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public class ExecutorServiceBuilder
        implements ParameterizedBuilder<NamedExecutorService>,
                   ParameterizedBuilder.ReflectionSupport
    {
    // ----- constructors ---------------------------------------------------

    public ExecutorServiceBuilder(String sName, ParameterizedBuilder<ExecutorServiceBuilder> executorServiceBuilder)
        {
        f_sName                  = sName;
        f_executorServiceBuilder = executorServiceBuilder;
        }

    // ----- ParameterizedBuilder interface ---------------------------------

    @Override
    public NamedExecutorService realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        Supplier<ExecutorService> supplier = () -> (ExecutorService) f_executorServiceBuilder.realize(resolver, loader, listParameters);

        return new NamedExecutorService(f_sName, supplier);
        }

    // ----- ParameterizedBuilder.ReflectionSupport interface ---------------

    @Override
    public boolean realizes(Class<?> clzClass, ParameterResolver resolver, ClassLoader loader)
        {
        return false;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The logical {@link ExecutorService} name.
     */
    protected String f_sName;

    /**
     * The {@link ParameterizedBuilder<ExecutorService>} that will construct
     * the {@link ExecutorService}.
     */
    protected ParameterizedBuilder<ExecutorServiceBuilder> f_executorServiceBuilder;
    }
