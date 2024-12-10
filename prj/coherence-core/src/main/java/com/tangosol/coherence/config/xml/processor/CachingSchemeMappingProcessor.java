/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ResourceMappingRegistry;

import com.tangosol.coherence.config.SchemeMappingRegistry;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import java.util.Map;

/**
 * An {@link CachingSchemeMappingProcessor} is responsible for processing &lt;caching-scheme-mapping&gt; {@link XmlElement}s
 * to update the {@link ResourceMappingRegistry} with {@link CacheMapping}s.
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
@XmlSimpleName("caching-scheme-mapping")
public class CachingSchemeMappingProcessor
        implements ElementProcessor<ResourceMappingRegistry>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceMappingRegistry process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // process the children of the <caching-scheme-mapping>
        Map<String, ?> mapProcessedChildren = context.processElementsOf(element);

        // add all of the ResourceMappings to the ResourceMappingRegistry
        CacheConfig             cacheConfig = context.getCookie(CacheConfig.class);
        ResourceMappingRegistry registry    = cacheConfig == null
                                        ? new SchemeMappingRegistry() : cacheConfig.getMappingRegistry();

        for (Object oChild : mapProcessedChildren.values())
            {
            if (oChild instanceof ResourceMapping)
                {
                // FUTURE: handle the case when the cache mapping is a duplicate or weaker pattern?
                registry.register((ResourceMapping) oChild);
                }
            }

        // process all of the foreign elements
        // (this allows the elements to modify the configuration)
        context.processForeignElementsOf(element);

        return registry;
        }
    }
