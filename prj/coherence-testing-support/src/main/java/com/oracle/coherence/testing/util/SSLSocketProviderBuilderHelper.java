/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.oracle.coherence.common.net.SocketProvider;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.processor.SSLProcessor;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;
import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import static org.junit.Assert.assertNotNull;

public class SSLSocketProviderBuilderHelper
    {
    /**
     * Load the {@link SSLSocketProviderDefaultDependencies} from the specified XML file.
     * <p>
     * The XML root element should be the {@code ssl} element.
     *
     * @param sConfig  the XML configuration file
     *
     * @return the loaded {@link SSLSocketProviderDefaultDependencies}.
     */
    public static SSLSocketProviderDefaultDependencies loadDependencies(String sConfig)
        {
        XmlDocument xml = XmlHelper.loadFileOrResource(sConfig, null);
        return loadDependencies(xml);
        }

    /**
     * Load the {@link SSLSocketProviderDefaultDependencies} from the specified XML file.
     * <p>
     * The XML root element should be the {@code ssl} element.
     *
     * @param xml  the XML configuration
     *
     * @return the loaded {@link SSLSocketProviderDefaultDependencies}.
     */
    public static SSLSocketProviderDefaultDependencies loadDependencies(XmlElement xml)
        {
        DefaultProcessingContext             context = createDefaultProcessingContext(xml);
        SSLSocketProviderDefaultDependencies depsSSL = new SSLSocketProviderDefaultDependencies(null);

        context.addCookie(SSLSocketProviderDefaultDependencies.class, depsSSL);
        context.addCookie(DefaultClusterDependencies.class, new DefaultClusterDependencies());

        SSLSocketProviderDependenciesBuilder bldr = new SSLProcessor().process(context, xml);

        return bldr.realize();
        }

    /**
     * Create a {@link DefaultProcessingContext} for the specified xml.
     *
     * @param xml  the {@link XmlElement} to process
     *
     * @return a {@link DefaultProcessingContext} for the specified xml
     */
    public static DefaultProcessingContext createDefaultProcessingContext(XmlElement xml)
        {
        ResourceRegistry                      registry = new SimpleResourceRegistry();
        DocumentProcessor.DefaultDependencies dep      = new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(registry);

        // establish the cluster-config processing context
        DefaultProcessingContext sslContext = new DefaultProcessingContext(dep, xml);

        // add the default namespace handler
        NamespaceHandler handler = dep.getDefaultNamespaceHandler();
        if (handler != null)
            {
            sslContext.ensureNamespaceHandler("", handler);
            }

        return new DefaultProcessingContext(sslContext, xml);
        }

    /**
     * Load the {@link SocketProvider} from the specified XML file.
     * <p>
     * The XML root element should be the {@code ssl} element.
     *
     * @param sConfig  the XML configuration file
     *
     * @return the loaded {@link SocketProvider}.
     */
    public static SocketProvider loadSocketProvider(String sConfig)
        {
        XmlDocument xml = XmlHelper.loadFileOrResource(sConfig, null);
        return loadSocketProvider(xml);
        }

    /**
     * Load the {@link SocketProvider} from the specified XML file.
     * <p>
     * The XML root element should be the {@code ssl} element.
     *
     * @param xml  the XML configuration
     *
     * @return the loaded {@link SocketProvider}.
     */
    public static SocketProvider loadSocketProvider(XmlElement xml)
        {
        ResourceRegistry registry = new SimpleResourceRegistry();
        DocumentProcessor.DefaultDependencies dep      = new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(registry);

        // establish the cluster-config processing context
        DefaultProcessingContext sslContext = new DefaultProcessingContext(dep, xml);

        // add the default namespace handler
        NamespaceHandler handler = dep.getDefaultNamespaceHandler();
        if (handler != null)
            {
            sslContext.ensureNamespaceHandler("", handler);
            }

        DefaultProcessingContext context = new DefaultProcessingContext(sslContext, xml);
        SSLSocketProviderDefaultDependencies depsSSL = new SSLSocketProviderDefaultDependencies(null);

        context.addCookie(SSLSocketProviderDefaultDependencies.class, depsSSL);
        context.addCookie(DefaultClusterDependencies.class, new DefaultClusterDependencies());

        SocketProviderBuilder bldr = (SocketProviderBuilder) context.processDocument(xml);

        return bldr.realize(null, null, null);
        }
    }
