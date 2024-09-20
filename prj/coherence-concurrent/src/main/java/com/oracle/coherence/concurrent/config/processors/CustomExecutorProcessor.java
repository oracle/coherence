/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.NamedExecutorService;
import com.oracle.coherence.concurrent.config.builders.AbstractExecutorBuilder;
import com.oracle.coherence.concurrent.config.builders.CachedBuilder;
import com.oracle.coherence.concurrent.config.builders.CustomExecutorBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} for {@code custom-executor} elements.
 *
 * @author rl  5.16.24
 * @since 14.1.2.0.0
 */
@XmlSimpleName("custom-executor")
public class CustomExecutorProcessor
        extends AbstractExecutorProcessor<CustomExecutorBuilder>
    {
    // ----- AbstractExecutorProcessor methods ------------------------------

    protected CustomExecutorBuilder builder()
        {
        return new CustomExecutorBuilder();
        }
    }
