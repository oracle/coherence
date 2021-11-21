/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.builders.ExecutorServiceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.xml.processor.ElementProcessorHelper;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;


/**
 * An {@link ElementProcessor} responsible for handling {@code executor-service}
 * element processing.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
@XmlSimpleName("executor-service")
public class ExecutorServiceProcessor
        implements ElementProcessor<ExecutorServiceBuilder>
    {
    // ----- ElementProcessor interface -------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public ExecutorServiceBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        String                                       sExecutorName = xmlElement.getElement("name").getString();
        ParameterizedBuilder<ExecutorServiceBuilder> builder       = (ParameterizedBuilder<ExecutorServiceBuilder>)
                ElementProcessorHelper.processParameterizedBuilder(context, xmlElement.getElement("instance"));

        return context.inject(new ExecutorServiceBuilder(sExecutorName, builder), xmlElement);
        }
    }
