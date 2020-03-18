/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;
import java.security.Provider;

/**
 * An {@link ElementProcessor} that will parse and produce a
 * ProviderBuilder based on a provider configuration element.
 *
 * @author jf  2015.11.11
 * @since Coherence 12.2.1.1
 */
@XmlSimpleName("provider")
public class ProviderProcessor
        implements ElementProcessor<SSLSocketProviderDependenciesBuilder.ProviderBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public SSLSocketProviderDependenciesBuilder.ProviderBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        SSLSocketProviderDependenciesBuilder.ProviderBuilder bldr = new SSLSocketProviderDependenciesBuilder.ProviderBuilder();
        context.inject(bldr, xmlElement);
        bldr.setBuilder((ParameterizedBuilder<Provider>)ElementProcessorHelper.processParameterizedBuilder(context, xmlElement));
        return bldr;
        }
    }
