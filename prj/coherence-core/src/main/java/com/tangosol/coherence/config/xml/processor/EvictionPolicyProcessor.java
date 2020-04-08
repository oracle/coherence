/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.EvictionPolicyBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;


import com.tangosol.net.cache.ConfigurableCacheMap.EvictionPolicy;

import com.tangosol.run.xml.XmlElement;

import java.text.ParseException;

/**
 * A {@link EvictionPolicyProcessor} is responsible for processing an
 * eviction-policy {@link XmlElement} to produce an {@link EvictionPolicyBuilder}.
 *
 * @author pfm  2011.12.02
 * @since Coherence 12.1.2
 */
@XmlSimpleName("eviction-policy")
public class EvictionPolicyProcessor
        implements ElementProcessor<EvictionPolicyBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public EvictionPolicyBuilder process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        EvictionPolicyBuilder bldr = new EvictionPolicyBuilder();

        if (element.getElementList().isEmpty())
            {
            try
                {
                String sValue = element.getString().trim();

                bldr.setEvictionType(context.getExpressionParser().parse(sValue, String.class));
                }
            catch (ParseException e)
                {
                throw new ConfigurationException("Failed to parse the specifie eviction-policy",
                                                 "Please ensure a correct eviction-policy is specified", e);
                }
            }
        else
            {
            bldr.setCustomBuilder((ParameterizedBuilder<EvictionPolicy>) context.processOnlyElementOf(element));
            }

        return bldr;
        }
    }
