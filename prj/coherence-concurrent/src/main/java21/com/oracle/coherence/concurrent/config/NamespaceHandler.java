/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config;

import com.oracle.coherence.concurrent.config.processors.CachedProcessor;
import com.oracle.coherence.concurrent.config.processors.CustomExecutorProcessor;
import com.oracle.coherence.concurrent.config.processors.FixedProcessor;
import com.oracle.coherence.concurrent.config.processors.SingleProcessor;
import com.oracle.coherence.concurrent.config.processors.ThreadFactoryProcessor;
import com.oracle.coherence.concurrent.config.processors.WorkStealingProcessor;
import com.oracle.coherence.concurrent.config.processors.VirtualPerTaskProcessor;

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
        registerProcessor(CachedProcessor.class);
        registerProcessor(FixedProcessor.class);
        registerProcessor(SingleProcessor.class);
        registerProcessor(ThreadFactoryProcessor.class);
        registerProcessor(WorkStealingProcessor.class);
        registerProcessor(VirtualPerTaskProcessor.class);
        registerProcessor(CustomExecutorProcessor.class);
        }
    }
