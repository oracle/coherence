/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.net.PasswordProvider;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import java.util.Map;

/**
 * A {@link PasswordProvidersProcessor} is responsible for processing &lt;password-providers&gt;
 * {@link XmlElement} of Coherence Operational Configuration files
 *
 * @author spuneet
 * @since Coherence 12.2.1.4
 */
@XmlSimpleName("password-providers")
public class PasswordProvidersProcessor
        implements ElementProcessor<Void>
    {
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Void process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // add all of the ParameterizedBuilders to the ParameterizedBuilderRegistry
        ParameterizedBuilderRegistry registry = context.getCookie(ParameterizedBuilderRegistry.class);
        Base.azzert(registry != null);

        Map<String, ?> mapProcessedChildren = context.processElementsOf(element);

        for (Map.Entry<String, ?> entry : mapProcessedChildren.entrySet())
            {
            String sName    = entry.getKey();
            Object oBuilder = entry.getValue();

            if (oBuilder instanceof ParameterizedBuilder)
                {
                ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) oBuilder;

                registry.registerBuilder(PasswordProvider.class, sName,
                                        (ParameterizedBuilder<PasswordProvider>) builder);
                }
            else
                {
                throw new ConfigurationException("The specified <password-provider> [" + sName
                        + "] is not a ParameterizedBuilder<PasswordProvider>",
                        "Use <class-name> element to specify a PasswordProvider implementation");
                }
            }

        return null;
        }
    }
