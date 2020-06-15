/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.ServiceSchemeRegistry;

import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.util.Map;

/**
 * A {@link SchemesProcessor} is an {@link ElementProcessor} for the
 * &lt;caching-schemes%gt; element of Coherence Cache Configuration files.
 *
 * @author jk  2015.05.28
 * @since Coherence 14.1.1
 */
@XmlSimpleName("schemes")
public class SchemesProcessor
        implements ElementProcessor<ServiceSchemeRegistry>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceSchemeRegistry process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // process the children of the <schemes>
        Map<String, ?> mapProcessedChildren = context.processElementsOf(xmlElement);

        // add all of the ServiceSchemes to the ServiceSchemeRegistry
        CacheConfig cacheConfig = context.getCookie(CacheConfig.class);
        Base.azzert(cacheConfig != null);

        ServiceSchemeRegistry registry = cacheConfig.getServiceSchemeRegistry();
        Base.azzert(registry != null);

        for (Object oChild : mapProcessedChildren.values())
            {
            if (oChild instanceof ServiceScheme)
                {
                registry.register((ServiceScheme) oChild);
                }
            }

        // process all of the foreign elements
        // (this allows the elements to modify the configuration)
        context.processForeignElementsOf(xmlElement);

        return registry;
        }
    }
