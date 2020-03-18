/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} that will parse and produce a
 * DefaultManagerDependencies based on a identity-manager/trust-manager configuration element.
 *
 * @author jf  2015.11.11
 * @since Coherence 12.2.1.1
 */
public class SSLManagerProcessor implements ElementProcessor<SSLSocketProviderDependenciesBuilder.DefaultManagerDependencies>
    {

    @Override
    public SSLSocketProviderDependenciesBuilder.DefaultManagerDependencies process(ProcessingContext context, XmlElement xmlElement) throws ConfigurationException
        {
        String sManagerKind = xmlElement.getQualifiedName().getLocalName();
        SSLSocketProviderDependenciesBuilder.DefaultManagerDependencies deps = new SSLSocketProviderDependenciesBuilder.DefaultManagerDependencies(sManagerKind);
        context.inject(deps, xmlElement);
        return deps;
        }
    }
