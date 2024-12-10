/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import javax.net.ssl.HostnameVerifier;

/**
 * An {@link ElementProcessor} that will parse and produce a
 * {@link HostnameVerifier} based on hostname-verifier configuration element.
 *
 * @author jf  2015.11.11
 * @since Coherence 12.2.1.1
 */
@XmlSimpleName("hostname-verifier")
public class SSLHostnameVerifierProcessor
        implements ElementProcessor<HostnameVerifierBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public HostnameVerifierBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        if (XmlHelper.hasElement(xmlElement, "hostname-verifier"))
            {
            xmlElement = xmlElement.getElement("hostname-verifier");
            }
        HostnameVerifierBuilder builder = new HostnameVerifierBuilder();
        context.inject(builder, xmlElement);
        builder.setBuilder((ParameterizedBuilder<HostnameVerifier>)
                                   ElementProcessorHelper.processParameterizedBuilder(context, xmlElement));
        return builder;
        }
    }
