/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.builders.ThreadFactoryBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} for {@code thread-factory} elements.
 *
 * @author rl  11.26.21
 * @since 21.12
 */
@XmlSimpleName("thread-factory")
public class ThreadFactoryProcessor
        implements ElementProcessor<ThreadFactoryBuilder>
    {
    // ----- ElementProcessor interface -------------------------------------

    public ThreadFactoryBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        return context.inject(new ThreadFactoryBuilder(), xmlElement);
        }
    }
