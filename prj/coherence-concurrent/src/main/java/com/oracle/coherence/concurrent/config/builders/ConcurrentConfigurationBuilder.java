/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.builders;

import com.oracle.coherence.concurrent.config.ConcurrentConfiguration;
import com.oracle.coherence.concurrent.config.NamedExecutorService;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.ParameterResolver;

import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * A simple holder for {@code coherence-concurrent} artifact configuration
 * from captured from configuration files.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public class ConcurrentConfigurationBuilder
        implements ParameterizedBuilder<ConcurrentConfiguration>
    {
    public ConcurrentConfigurationBuilder(List<ExecutorServiceBuilder> listExecutorServiceBuilders)
        {
        f_listExecutorServiceBuilders = listExecutorServiceBuilders;
        }

    // ----- ParameterizedBuilder interface ---------------------------------

    @Override
    public ConcurrentConfiguration realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        List<NamedExecutorService> l = f_listExecutorServiceBuilders.stream()
                .map(executorServiceBuilder ->
                             executorServiceBuilder.realize(resolver, loader, listParameters)).collect(Collectors.toList());

        return new ConcurrentConfiguration(l);
        }

    // ----- data members ---------------------------------------------------

    /**
     * {@link List} of parsed {@link ExecutorServiceBuilder}s.
     */
    List<ExecutorServiceBuilder> f_listExecutorServiceBuilders;
    }
