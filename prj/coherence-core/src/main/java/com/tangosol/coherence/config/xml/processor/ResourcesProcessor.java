/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.base.Assertions;

import com.tangosol.coherence.config.builder.NamedResourceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import java.util.Map;

/**
 * An {@link ElementProcessor} for the &lt;resources%gt; element of
 * Coherence Operational Configuration files.
 *
 * @author Jonathan Knight  2022.03.01
 * @since 22.06
 */
@XmlSimpleName("resources")
public class ResourcesProcessor
        implements ElementProcessor<Void>
    {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked", "ConstantConditions"})
    public Void process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // add all the ParameterizedBuilders to the ParameterizedBuilderRegistry as NamedResourceBuilders
        ParameterizedBuilderRegistry  registry = context.getCookie(ParameterizedBuilderRegistry.class);
        Assertions.azzert(registry != null);

        for (Map.Entry<String, ?> entry : context.processElementsOf(xmlElement).entrySet())
            {
            Object oBuilder = entry.getValue();

            if (oBuilder instanceof NamedResourceBuilder)
                {
                NamedResourceBuilder builder = (NamedResourceBuilder<?>) oBuilder;
                String               sName   = builder.getName();

                registry.registerBuilder(NamedResourceBuilder.class, sName, builder);
                }
            else
                {
                throw new ConfigurationException("The <resource> [" + entry.getKey() + "] is not a ParameterizedBuilder",
                        "Use <instance> element to specify a ParameterizedBuilder implementation");
                }
            }

        return null;
        }
    }
