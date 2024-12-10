/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;

import com.tangosol.internal.net.cluster.DefaultClusterDependencies;

import com.tangosol.net.SocketProviderFactory;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.SimpleResourceRegistry;

import com.oracle.coherence.testing.SystemPropertyResource;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.net.URISyntaxException;

/**
 * Unit Tests for {@link SocketProviderProcessor}s
 *
 * @author jf  2015.11.09
 * @since Coherence 12.2.1
 */
public class SocketProvidersProcessorTest
    {
    /**
     * Process default tangosol coherence socket-providers fragment.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testDefinitionsProcessing()
            throws URISyntaxException, ConfigurationException
        {
        XmlDocument                xml  = XmlHelper.loadFileOrResource("com/tangosol/coherence/config/xml/processor/tangosol-coherence-socket-providers.xml" , null);
        DefaultClusterDependencies deps = new DefaultClusterDependencies();

        deps.setSocketProviderFactory(new SocketProviderFactory());

        DocumentProcessor.DefaultDependencies dependencies =
            new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());
        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(dependencies, xml);

        // add the default namespace handler
        NamespaceHandler handler = dependencies.getDefaultNamespaceHandler();

        if (handler != null)
            {
            ctxClusterConfig.ensureNamespaceHandler("", handler);
            }

        dependencies.setResourceRegistry(new SimpleResourceRegistry());
        dependencies.getResourceRegistry().registerResource(DefaultClusterDependencies.class, deps);

        // add the ParameterizedBuilderRegistry as a Cookie so we can look it up
        ctxClusterConfig.addCookie(ParameterizedBuilderRegistry.class, deps.getBuilderRegistry());
        ctxClusterConfig.addCookie(DefaultClusterDependencies.class, deps);

        // process the <socket-providers> definitions
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            ctxSocketProviders.processDocument(xml);
            }

        SocketProviderFactory factory = deps.getSocketProviderFactory();
        assertNotNull(factory);

        // validate pre-defined SocketProvider types
        for (SocketProviderFactory.Dependencies.ProviderType type : SocketProviderFactory.Dependencies.ProviderType.values())
            {
            System.out.println("Validating SocketProvider " + type.getName());
            SocketProvider provider = factory.getSocketProvider(type.getName());
            assertNotNull(provider);
            System.out.println("socketProvider=" + provider);

            if (provider instanceof SSLSocketProvider)
                {
                SSLSocketProvider sslProvider = (SSLSocketProvider) provider;
                SSLSocketProvider.Dependencies sslDeps = sslProvider.getDependencies();
                assertThat(sslDeps.getClientAuth(), is(SSLSocketProvider.ClientAuthMode.required));
                assertNotNull(sslDeps.getDelegateSocketProvider());
                }

            // validate SocketProviderBuilders in BuilderRegistry . defined in <socket-providers> processor
            ParameterizedBuilder<SocketProvider> builder = deps.getBuilderRegistry().getBuilder(SocketProvider.class, type.getName());
            assertNotNull(builder);
            SocketProvider providerFromBuilder = builder.realize(null, null, null);
            assertEquals("validating SocketProviderFactory vs SocketProviderBuilder from builder registry. SocketProvider id=" + type.getName(), provider, providerFromBuilder);
            }
        }
    }
