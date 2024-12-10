/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.net.SSLSettings;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.SdpSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;
import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.SimpleResourceRegistry;
import com.oracle.coherence.testing.SystemPropertyResource;
import org.junit.Test;
import org.junit.Before;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* Unit test of the {@link SocketProviderProcessor} class.
*
* @author jf  2015.11.13
*/
public class SocketProviderProcessorTest
    {
    @Before
    public void init()
        {
        m_deps = new DefaultClusterDependencies();

        m_deps.setSocketProviderFactory(new SocketProviderFactory());

        DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        m_ctxClusterConfig = new DefaultProcessingContext(dependencies, null);

        // add the default namespace handler
        NamespaceHandler handler = dependencies.getDefaultNamespaceHandler();

        if (handler != null)
            {
            m_ctxClusterConfig.ensureNamespaceHandler("", handler);
            }

        dependencies.setResourceRegistry(new SimpleResourceRegistry());

        // add the ParameterizedBuilderRegistry as a Cookie so we can look it up
        m_ctxClusterConfig.addCookie(ParameterizedBuilderRegistry.class, m_deps.getBuilderRegistry());
        m_ctxClusterConfig.addCookie(DefaultClusterDependencies.class, m_deps);
        }

    @Test
    public void testDefaultProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider/>");;

        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);

            assertEquals(builder.realize(null, null, null), SocketProviderFactory.DEFAULT_SOCKET_PROVIDER);
            }
        }

    @Test
    public void testSSLProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder  = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
            SocketProvider provider = builder.realize(null, null, null);
            assertTrue(provider instanceof SSLSocketProvider);
            SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
            assertTrue(delegate instanceof MultiplexedSocketProvider);
            assertTrue(((MultiplexedSocketProvider) delegate).getDependencies().getDelegateProvider() instanceof TcpSocketProvider);
            }
        }

    @Test
    public void testSystemProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><system/></socket-provider>");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder  = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
            SocketProvider        provider = builder.realize(null, null, null);

            assertTrue(provider instanceof MultiplexedSocketProvider);
            assertTrue(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider() instanceof TcpSocketProvider);
            }
        }

    @Test
    public void testTcpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><tcp/></socket-provider>");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
            SocketProvider        provider = builder.realize(null, null, null);

            assertTrue(provider instanceof MultiplexedSocketProvider);
            assertTrue(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider() instanceof TcpSocketProvider);
            }
        }

    @Test
    public void testSDPProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><sdp/></socket-provider>");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder  = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
            SocketProvider        provider = builder.realize(null, null, null);

            assertTrue(provider instanceof MultiplexedSocketProvider);
            assertTrue(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider() instanceof SdpSocketProvider);
            }
        }

    @Test
    public void testSdpsProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl><socket-provider>sdp</socket-provider></ssl></socket-provider>");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder  = (SocketProviderBuilder)ctxSocketProviders.processDocument(xml);
            SocketProvider        provider = builder.realize(null, null, null);

            assertTrue(provider instanceof SSLSocketProvider);
            SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
            assertTrue(delegate instanceof MultiplexedSocketProvider);
            assertTrue(((MultiplexedSocketProvider) delegate).getDependencies().getDelegateProvider() instanceof SdpSocketProvider);
            }
        }

    @Test
    public void testDemultiplexedSSLProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder  = (SocketProviderBuilder)ctxSocketProviders.processDocument(xml);
            SocketProvider        provider = builder.getDemultiplexedSocketProvider(1);

            assertTrue(provider instanceof SSLSocketProvider);
            SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
            assertTrue(delegate instanceof DemultiplexedSocketProvider);
            }
        }

    @Test
    public void testDemultiplexedSystemProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><system/></socket-provider>");

        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder  = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
            SocketProvider        provider = builder.getDemultiplexedSocketProvider(1);

            assertTrue(provider instanceof DemultiplexedSocketProvider);
            }
        }

    @Test
    public void testDemultiplexedTcpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><tcp/></socket-provider>");

        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder  = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
            SocketProvider        provider = builder.getDemultiplexedSocketProvider(1);

            assertTrue(provider instanceof DemultiplexedSocketProvider);
            }
        }

    @Test
    public void testDemultiplexedSDPProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><sdp/></socket-provider>");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder  = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
            SocketProvider        provider = builder.getDemultiplexedSocketProvider(1);

            assertTrue(provider instanceof DemultiplexedSocketProvider);
            }
        }

    @Test
    public void testDemultiplexedSdpsProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl><socket-provider>sdp</socket-provider></ssl></socket-provider>");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.security.keystore", "file:internal/keystore.jks");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.security.password", "password"))
            {
            SocketProviderBuilder builder = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
            assertNotNull(builder);
            assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(builder.getId()));
            SocketProvider provider = builder.getDemultiplexedSocketProvider(1);
            SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
            assertTrue(delegate instanceof DemultiplexedSocketProvider);

            }
        }

    @Test
    public void testInlinedSSLProvider_COH14879()
        {
        XmlElement xml = XmlHelper.loadXml(
                "<socket-provider>" +
                " <ssl>" +
                "  <protocol>TLS</protocol>" +
                "  <identity-manager>" +
                "    <algorithm>SunX509</algorithm>" +
                "    <key-store>" +
                "      <url>file:internal/testkeystore.jks</url>" +
                "      <password>storepassword</password>" +
                "      <type>JKS</type>" +
                "    </key-store>" +
                "    <password>keypassword</password>" +
                "  </identity-manager>" +
                "  <trust-manager>" +
                "    <algorithm>SunX509</algorithm>" +
                "    <key-store>" +
                "      <url>file:internal/testkeystore.jks</url>" +
                "      <password>storepassword</password>" +
                "    </key-store>" +
                "  </trust-manager>" +
                "  <socket-provider>tcp</socket-provider>" +
                " </ssl>" +
                "</socket-provider>");

        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        SocketProviderBuilder builder = (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
        assertNotNull(builder);
        assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(builder.getId()));
        SSLSettings sslSettings = builder.getSSLSettings();
        assertThat(sslSettings.getClientAuth(), is(SSLSocketProvider.ClientAuthMode.required));
        assertNull(sslSettings.getEnabledProtocolVersions());
        assertNull(sslSettings.getHostnameVerifier());
        SocketProvider provider = builder.realize(new NullParameterResolver(), null, null);
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertTrue(delegate.getDelegate() instanceof TcpSocketProvider);
        }

    public static final SocketProviderFactory FACTORY = new SocketProviderFactory();

    private DefaultClusterDependencies m_deps;
    private DefaultProcessingContext   m_ctxClusterConfig;
    }
