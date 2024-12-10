/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.concurrent.Executor;

/**
 * An {@link ElementProcessor} that will parse and produce a
 * Executor based on an ssl/executor configuration element.
 *
 * @author jf  2015.11.11
 * @since Coherence 12.2.1.1
 */
@XmlSimpleName("executor")
public class ExecutorProcessor
        implements ElementProcessor<ParameterizedBuilder<Executor>>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<Executor> process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        if (XmlHelper.hasElement(xmlElement, "executor"))
            {
            xmlElement = xmlElement.getElement("executor");
            }

        return (ParameterizedBuilder<Executor>) ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);
        }
    }
