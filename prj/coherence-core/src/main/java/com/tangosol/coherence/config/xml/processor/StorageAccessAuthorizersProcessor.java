/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.ServiceSchemeRegistry;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.util.Map;

/**
 * An {@link ElementProcessor} for the &lt;storage-authorizers%gt; element of
 * Coherence Operational Configuration files.
 *
 * @author bo  2014.10.28
 * @since Coherence 12.1.3
 */
@XmlSimpleName("storage-authorizers")
public class StorageAccessAuthorizersProcessor
        implements ElementProcessor<Void>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public Void process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // process the children of the <storage-authorizers>
        Map<String, ?> mapProcessedChildren = context.processElementsOf(xmlElement);

        // add all of the ParameterizedBuilders to the ParameterizedBuilderRegistry
        ParameterizedBuilderRegistry registry = context.getCookie(ParameterizedBuilderRegistry.class);

        Base.azzert(registry != null);

        for (Map.Entry<String, ?> entry : mapProcessedChildren.entrySet())
            {
            String sName    = entry.getKey();
            Object oBuilder = entry.getValue();

            if (oBuilder instanceof ParameterizedBuilder)
                {
                ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) oBuilder;

                registry.registerBuilder(StorageAccessAuthorizer.class, sName,
                                         (ParameterizedBuilder<StorageAccessAuthorizer>) builder);
                }
            else
                {
                throw new ConfigurationException("The specified <storage-authorizer> [" + sName
                    + "] is not a ParameterizedBuilder<StorageAccessAuthorizer>", "Use <instance> element to specify an StorageAccessAuthorizer implementation");

                }
            }

        return null;
        }
    }
