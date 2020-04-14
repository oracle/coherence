/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.ResourceMappingRegistry;
import com.tangosol.coherence.config.SchemeMappingRegistry;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import java.util.Map;

/**
 * An {@link TopicSchemeMappingProcessor} is responsible for processing
 * &lt;topic-scheme-mapping&gt; {@link XmlElement}s to update the
 * {@link ResourceMappingRegistry} with {@link ResourceMapping}s.
 *
 * @author jk  2015.05.28
 * @since Coherence 14.1.1
 */
@XmlSimpleName("topic-scheme-mapping")
public class TopicSchemeMappingProcessor
        implements ElementProcessor<ResourceMappingRegistry>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceMappingRegistry process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // process the children of the <topic-scheme-mapping>
        Map<String, ?> mapProcessedChildren = context.processElementsOf(element);

        // add all of the Mappings to the ResourceMappingRegistry
        CacheConfig             cacheConfig = context.getCookie(CacheConfig.class);
        ResourceMappingRegistry registry    = cacheConfig == null
                                                  ? new SchemeMappingRegistry()
                                                  : cacheConfig.getMappingRegistry();

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
