/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config;

import com.oracle.coherence.concurrent.config.processors.ConcurrentConfigurationProcessor;
import com.oracle.coherence.concurrent.config.processors.ExecutorServiceProcessor;
import com.oracle.coherence.concurrent.config.processors.ExecutorServicesProcessor;

import com.tangosol.config.xml.AbstractNamespaceHandler;

/**
 * {@link com.tangosol.config.xml.NamespaceHandler} for processing
 * {@code coherence-concurrent}-related artifacts defined in a Coherence
 * configuration file.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public class NamespaceHandler
        extends AbstractNamespaceHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code NamespaceHandler} instance.
     */
    public NamespaceHandler()
        {
        registerProcessor(ConcurrentConfigurationProcessor.class);
        registerProcessor(ExecutorServicesProcessor.class);
        registerProcessor(ExecutorServiceProcessor.class);
        }
    }
