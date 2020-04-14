/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;


import com.tangosol.run.xml.XmlElement;

/**
 * An {@link CacheMappingProcessor} is responsible for processing &lt;cache-mapping&gt; {@link XmlElement}s to produce
 * a {@link CacheMapping}.
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
@XmlSimpleName("cache-mapping")
public class CacheMappingProcessor
        implements ElementProcessor<CacheMapping>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public CacheMapping process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // construct the CacheMapping with the required properties
        String       sCacheNamePattern = context.getMandatoryProperty("cache-name", String.class, element);
        String       sSchemeName       = context.getMandatoryProperty("scheme-name", String.class, element);
        CacheMapping mapping           = new CacheMapping(sCacheNamePattern, sSchemeName);

        // now inject any other (optional) properties it may require
        context.inject(mapping, element);

        // add the cache-mapping as a cookie so that child processors may access it (mainly to add resources if necessary)
        context.addCookie(CacheMapping.class, mapping);

        // process all of the foreign elements
        // (this allows the elements to modify the configuration)
        context.processForeignElementsOf(element);

        return mapping;
        }
    }
