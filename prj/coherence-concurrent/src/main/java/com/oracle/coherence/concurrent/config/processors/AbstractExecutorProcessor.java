/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.NamedExecutorService;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * Base class for {@link ElementProcessor}s producing {@link NamedExecutorService}
 * instances.
 *
 * @param <T> the {@link ParameterizedBuilder} type this processor supports
 *
 * @author rl  11.26.21
 * @since 21.12
 */
public abstract class AbstractExecutorProcessor<T extends ParameterizedBuilder<NamedExecutorService>>
        implements ElementProcessor<NamedExecutorService>
    {
    // ----- ElementProcessor interface -------------------------------------

    public NamedExecutorService process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        T builder = context.inject(builder(), xmlElement);
        return builder.realize(context.getDefaultParameterResolver(), context.getContextClassLoader(), null);
        }

    // ----- protected methods ----------------------------------------------

    protected abstract T builder();
    }
