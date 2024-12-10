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

import com.tangosol.internal.net.service.extend.proxy.DefaultInvocationServiceProxyDependencies;
import com.tangosol.net.InvocationService;
import com.tangosol.run.xml.XmlElement;

/**
 * An {@link com.tangosol.config.xml.ElementProcessor} that will parse invocation-service-proxy configuration
 * element tp produce a {@link DefaultInvocationServiceProxyDependencies} object.
 *
 * @author pfm  2013.08.22
 * @since Coherence 12.1.3
 */
@XmlSimpleName("invocation-service-proxy")
public class InvocationServiceProxyProcessor
        implements ElementProcessor<DefaultInvocationServiceProxyDependencies>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultInvocationServiceProxyDependencies process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        DefaultInvocationServiceProxyDependencies deps = new DefaultInvocationServiceProxyDependencies();
        context.inject(deps, xmlElement);

        // assume a custom builder has been provided
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr != null)
            {
            try
                {
                deps.setServiceBuilder((ParameterizedBuilder<InvocationService>) bldr);
                }
            catch (Exception e)
                {
                throw new ConfigurationException("Expected a ParameterizedBuilder<InvocationService>, but found ["
                                                 + bldr + "] after parsing [" + xmlElement
                                                 + "]", "Please specify a class that is a InvocationService", e);
                }
            }

        return deps;
        }
    }
