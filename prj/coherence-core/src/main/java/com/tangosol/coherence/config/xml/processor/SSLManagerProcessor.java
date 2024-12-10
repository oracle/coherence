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

import com.tangosol.internal.net.ssl.DefaultManagerDependencies;
import com.tangosol.internal.net.ssl.ManagerDependencies;

import com.tangosol.net.ssl.CertificateLoader;
import com.tangosol.net.ssl.PrivateKeyLoader;

import com.tangosol.run.xml.XmlElement;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An {@link ElementProcessor} that will parse and produce a
 * DefaultManagerDependencies based on a identity-manager/trust-manager configuration element.
 *
 * @author jf  2015.11.11
 * @author Jonathan Knight  2022.03.16
 * @since 12.2.1.1
 */
public class SSLManagerProcessor implements ElementProcessor<ManagerDependencies>
    {
    @Override
    public ManagerDependencies process(ProcessingContext context, XmlElement xmlElement) throws ConfigurationException
        {
        String                     sManagerKind = xmlElement.getQualifiedName().getLocalName();
        DefaultManagerDependencies dependencies = new DefaultManagerDependencies(sManagerKind);

        context.inject(dependencies, xmlElement);

        // process any xml that was not injected
        Map<String, ?> mapComponent = context.processRemainingElementsOf(xmlElement);
        Collection<?>  colComponent = mapComponent.values()
                                                  .stream()
                                                  .map(this::buildIfInstanceBuilder)
                                                  .collect(Collectors.toList());

        // Set any PrivateKeyLoaders found (last one wins)
        colComponent.stream()
                .filter(o -> o instanceof PrivateKeyLoader)
                .map(PrivateKeyLoader.class::cast)
                .filter(PrivateKeyLoader::isEnabled)
                .forEach(dependencies::setPrivateKeyLoader);

        // Set any CertificateLoaders found
        CertificateLoader[] aCertLoader = colComponent.stream()
                .filter(o -> o instanceof CertificateLoader)
                .map(CertificateLoader.class::cast)
                .filter(CertificateLoader::isEnabled)
                .toArray(CertificateLoader[]::new);

        if (aCertLoader.length > 0)
            {
            dependencies.setCertificateLoaders(aCertLoader);
            }

        return dependencies;
        }

    @SuppressWarnings("rawtypes")
    private Object buildIfInstanceBuilder(Object o)
        {
        if (o instanceof ParameterizedBuilder)
            {
            o = ((ParameterizedBuilder) o).realize(null, null, null);
            }
        return o;
        }
    }
