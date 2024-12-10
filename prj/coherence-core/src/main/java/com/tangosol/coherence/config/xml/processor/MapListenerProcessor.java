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


import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.MapListener;

/**
 * A {@link MapListenerProcessor} is responsible for processing a listener
 * {@link XmlElement}s to produce a {@link ParameterizedBuilder} for a
 * {@link MapListener}.
 *
 * @author pfm  2011.12.02
 * @since Coherence 12.1.2
 */
@XmlSimpleName("listener")
public class MapListenerProcessor
        implements ElementProcessor<ParameterizedBuilder<MapListener>>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<MapListener> process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // fetch the builder defined in the "listener" element
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, element);

        return (ParameterizedBuilder<MapListener>) bldr;
        }
    }
