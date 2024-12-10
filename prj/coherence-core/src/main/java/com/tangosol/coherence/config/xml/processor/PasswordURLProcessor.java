/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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

import com.tangosol.net.PasswordProvider;
import com.tangosol.net.URLPasswordProvider;

import com.tangosol.run.xml.XmlElement;

/**
 * The {@link PasswordURLProcessor} is responsible for processing the &lt;password-url&gt;
 * {@link XmlElement} in a Coherence configuration file.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
@XmlSimpleName("password-url")
public class PasswordURLProcessor
        implements ElementProcessor<ParameterizedBuilder<PasswordProvider>>
    {
    @Override
    public ParameterizedBuilder<PasswordProvider> process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        String  sURL       = element.getString();
        boolean fFirstOnly = element.getSafeAttribute("first-line-only").getBoolean(false);

        PasswordProvider provider = sURL == null
                ? PasswordProvider.NullImplementation
                : new URLPasswordProvider(sURL, fFirstOnly);

        return (resolver, loader, listParameters) -> provider;
        }
    }
