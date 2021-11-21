/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.builders.ConcurrentConfigurationBuilder;
import com.oracle.coherence.concurrent.config.builders.ExecutorServiceBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * An {@link ElementProcessor} responsible for handling {@code concurrent-config}
 * element processing.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
@XmlSimpleName("concurrent-config")
public class ConcurrentConfigurationProcessor
        implements ElementProcessor<ConcurrentConfigurationBuilder>
    {
    // ----- ElementProcessor interface -------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public ConcurrentConfigurationBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        context.processForeignElementsOf(xmlElement);

        List<XmlElement>             elements     = xmlElement.getElementList();
        List<ExecutorServiceBuilder> listBuilders = null;

        Optional<XmlElement> exServices = elements.stream().filter(e -> "executor-services".equals(e.getQualifiedName().getLocalName())).findFirst();
        if (exServices.isPresent())
            {
            //noinspection unchecked
            listBuilders = (List<ExecutorServiceBuilder>) context.processElement(exServices.get());
            }

        return context.inject(new ConcurrentConfigurationBuilder(listBuilders == null ? Collections.emptyList() : listBuilders), xmlElement);
        }
    }
