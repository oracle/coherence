/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.builders.ExecutorServiceBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ElementProcessor} responsible for handling {@code executor-services}
 * element processing.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
@XmlSimpleName("executor-services")
public class ExecutorServicesProcessor
        implements ElementProcessor<List<ExecutorServiceBuilder>>
    {
    // ----- ElementProcessor interface -------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public List<ExecutorServiceBuilder> process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        List<ExecutorServiceBuilder> listBldrs = new ArrayList<>();

        for (XmlElement elementChild : ((List<XmlElement>) xmlElement.getElementList()))
            {
            Object oValue = context.processElement(elementChild);

            if (oValue instanceof ExecutorServiceBuilder)
                {
                listBldrs.add((ExecutorServiceBuilder) oValue);
                }
            else
                {
                throw new ConfigurationException(String.format("Invalid executor-services definition [%s] in [%s]",
                                                               elementChild, xmlElement),
                                                 "Please ensure the executor-service is correctly defined");
                }
            }

        return listBldrs;
        }
    }
