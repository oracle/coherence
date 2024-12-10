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

import com.tangosol.util.Filter;

/**
 * Responsible for processing {@code view-filter} elements.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
@XmlSimpleName("transformer")
public class TransformerProcessor
        implements ElementProcessor<ParameterizedBuilder<Filter>>
    {
    // ----- ElementProcessor methods ---------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public ParameterizedBuilder<Filter> process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // fetch the builder defined in the "transformer" element
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        return (ParameterizedBuilder<Filter>) bldr;
        }
    }
