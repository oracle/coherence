/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.CacheConfig;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;

/**
 * A {@link ConfigurationProcessor} is responsible for processing a
 * configuration {@link XmlElement} to produce a {@link CacheConfig} object.
 *
 * @author jk  2015.05.28
 * @since Coherence 14.1.1
 */
@XmlSimpleName(CacheConfig.TOP_LEVEL_ELEMENT_NAME)
public class ConfigurationProcessor
        implements ElementProcessor<CacheConfig>
    {
    // ----- ElementProcessor interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheConfig process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // get the CacheConfig into which we'll inject configuration
        // (this cookie is added by the CacheConfigNamespaceHandler)
        CacheConfig cacheConfig = context.getCookie(CacheConfig.class);

        Base.azzert(cacheConfig != null);

        // process the <defaults> to establish the ResourceRegistry defaults
        XmlElement xmlDefaults = element.getElement("defaults");

        if (xmlDefaults != null)
            {
            context.processElement(xmlDefaults);
            }

        // inject the configuration in the cache config
        context.inject(cacheConfig, element);

        // process the foreign elements to allow custom configuration
        context.processForeignElementsOf(element);

        // finally add the processed element as resource that can be used
        // by legacy parts of coherence
        context.getResourceRegistry().registerResource(XmlElement.class, "legacy-cache-config", element);

        return cacheConfig;
        }
    }
