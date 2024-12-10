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

import com.tangosol.internal.net.service.extend.proxy.DefaultCacheServiceProxyDependencies;
import com.tangosol.net.CacheService;
import com.tangosol.run.xml.XmlElement;

/**
 * An {@link com.tangosol.config.xml.ElementProcessor} that will parse cache-service-proxy configuration
 * element tp produce a {@link DefaultCacheServiceProxyDependencies} object.
 *
 * @author pfm  2013.08.15
 * @since Coherence 12.1.3
 */
@XmlSimpleName("cache-service-proxy")
public class CacheServiceProxyProcessor
        implements ElementProcessor<DefaultCacheServiceProxyDependencies>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultCacheServiceProxyDependencies process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        DefaultCacheServiceProxyDependencies deps = new DefaultCacheServiceProxyDependencies();
        context.inject(deps, xmlElement);

        // assume a custom builder has been provided
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr != null)
            {
                deps.setServiceBuilder((ParameterizedBuilder<CacheService>) bldr);
            }

        return deps;
        }
    }
