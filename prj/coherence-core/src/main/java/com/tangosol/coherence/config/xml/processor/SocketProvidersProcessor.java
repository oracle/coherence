/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An {@link SocketProvidersProcessor} is responsible for processing &lt;socket-provider&gt;
 * {@link XmlElement}s SocketProvider definitions.
 *
 * @author pfm  2013.03.18
 * @since Coherence 12.2.1
 */
@XmlSimpleName("socket-providers")
public class SocketProvidersProcessor implements ElementProcessor<Void>
    {
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Void process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        List<SocketProvider> listSocketProvider = new ArrayList<>();

        // add all of the ParameterizedBuilders to the ParameterizedBuilderRegistry
        ParameterizedBuilderRegistry registry = context.getCookie(ParameterizedBuilderRegistry.class);

        Map<String, ?> mapProcessedChildren = context.processElementsOf(element);

        for (Map.Entry<String, ?> entry : mapProcessedChildren.entrySet())
            {
            registry.registerBuilder(SocketProvider.class, entry.getKey(), (SocketProviderBuilder) entry.getValue());
            }

        return null;
        }

    }
