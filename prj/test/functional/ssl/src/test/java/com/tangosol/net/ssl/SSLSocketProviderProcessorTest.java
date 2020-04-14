/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.oracle.coherence.common.internal.net.WrapperSocket;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
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
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


/**
* Unit test of the SSLSocketProviderProcessor class.
*
* @author jf  2015.11.10
*/
public class SSLSocketProviderProcessorTest
    {

    @Test
    public void testConstructor()
        {
        new SSLSocketProvider();
        }


    @Test
    public void testSimpleClientConfiguration()
            throws IOException
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-client.xml", null);

        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        DocumentProcessor.DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        // establish the cluster-config processing context
        DefaultProcessingContext sslContext = new DefaultProcessingContext(dep, xml);

        // add the default namespace handler
        NamespaceHandler handler = dep.getDefaultNamespaceHandler();
        if (handler != null)
            {
            sslContext.ensureNamespaceHandler("", handler);
            }

        XmlElement xmlSSL = xml.getSafeElement("ssl");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(sslContext, xmlSSL);

        SSLSocketProviderDefaultDependencies depsSSL = new SSLSocketProviderDefaultDependencies(null);

        ctxSocketProviders.addCookie(SSLSocketProviderDefaultDependencies.class, depsSSL);
        ctxSocketProviders.addCookie(DefaultClusterDependencies.class, new DefaultClusterDependencies());

        SSLSocketProviderDependenciesBuilder bldr = new SSLProcessor().process(ctxSocketProviders, xmlSSL);

        SSLSocketProviderDefaultDependencies sslDeps  = bldr.realize();
        SSLSocketProvider                    provider = new SSLSocketProvider(sslDeps);

        SSLContext ctx = sslDeps.getSSLContext();
        assertNotNull(ctx);
        assertEquals(ctx.getProtocol(), SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL);
        assertNotNull(sslDeps.getExecutor());
        assertNull(sslDeps.getHostnameVerifier());

        try
            {
            provider.ensureSessionValidity(
                    sslDeps.getSSLContext().createSSLEngine().getSession(),
                    new WrapperSocket(provider.openSocket())
                        {
                        public InetAddress getInetAddress()
                            {
                            try
                                {
                                return InetAddress.getLocalHost();
                                }
                            catch (UnknownHostException e)
                                {
                                throw new IllegalStateException();
                                }
                            }
                        });
            }
        catch (SSLException sse)
            {
            fail("SSLException: "+sse);
            }
        }

    @Test
    public void testSimpleServerConfiguration()
            throws IOException
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-server.xml", null);

        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        DocumentProcessor.DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        // establish the cluster-config processing context
        DefaultProcessingContext sslContext = new DefaultProcessingContext(dep, xml);

        // add the default namespace handler
        NamespaceHandler handler = dep.getDefaultNamespaceHandler();
        if (handler != null)
            {
            sslContext.ensureNamespaceHandler("", handler);
            }


        XmlElement xmlSSL = xml.getSafeElement("ssl");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(sslContext, xmlSSL);

        SSLSocketProviderDefaultDependencies depsSSL = new SSLSocketProviderDefaultDependencies(null);

        ctxSocketProviders.addCookie(SSLSocketProviderDefaultDependencies.class, depsSSL);
        ctxSocketProviders.addCookie(DefaultClusterDependencies.class, new DefaultClusterDependencies());

        SSLSocketProviderDependenciesBuilder bldr = new SSLProcessor().process(ctxSocketProviders, xmlSSL);
        SSLSocketProviderDefaultDependencies sslDeps = bldr.realize();
        SSLSocketProvider                    provider = new SSLSocketProvider(sslDeps);

        SSLContext ctx = sslDeps.getSSLContext();
        assertNotNull(ctx);
        assertEquals(ctx.getProtocol(), SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL);
        assertNotNull(ctx.getProvider());

        assertNotNull(sslDeps.getExecutor());
        assertNull(sslDeps.getHostnameVerifier());
        assertNull(sslDeps.getEnabledCipherSuites());
        assertNull(sslDeps.getEnabledProtocolVersions());
        assertFalse(sslDeps.isClientAuthenticationRequired());

        try
            {
            provider.ensureSessionValidity(
                    sslDeps.getSSLContext().createSSLEngine().getSession(),
                    new WrapperSocket(provider.openSocket())
                        {
                        public InetAddress getInetAddress()
                            {
                            try
                                {
                                return InetAddress.getLocalHost();
                                }
                            catch (UnknownHostException e)
                                {
                                throw new IllegalStateException();
                                }
                            }
                        });
            }
        catch (SSLException sse)
            {
            fail("SSLException: "+sse);
            }
        }

    @Test
    public void testServerConfigurationUsingBlacklist()
            throws IOException
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-server-blacklist.xml", null);

        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        DocumentProcessor.DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        // establish the cluster-config processing context
        DefaultProcessingContext sslContext = new DefaultProcessingContext(dep, xml);

        // add the default namespace handler
        NamespaceHandler handler = dep.getDefaultNamespaceHandler();
        if (handler != null)
            {
            sslContext.ensureNamespaceHandler("", handler);
            }

        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(sslContext, xml);

        SSLSocketProviderDefaultDependencies depsSSL = new SSLSocketProviderDefaultDependencies(null);

        ctxSocketProviders.addCookie(SSLSocketProviderDefaultDependencies.class, depsSSL);
        ctxSocketProviders.addCookie(DefaultClusterDependencies.class, new DefaultClusterDependencies());

        SSLSocketProviderDependenciesBuilder bldr    = new SSLProcessor().process(ctxSocketProviders, xml);
        SSLSocketProviderDefaultDependencies sslDeps = bldr.realize();
        SSLContext                           ctx     = sslDeps.getSSLContext();

        assertNotNull(ctx);
        assertEquals(ctx.getProtocol(), SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL);
        assertNotNull(sslDeps.getExecutor());
        assertNull(sslDeps.getHostnameVerifier());
        String[] versions = sslDeps.getEnabledProtocolVersions();
        assertNotNull(versions);
        assertFalse(Arrays.asList(versions).contains("SSLv3"));

        List ciphers = Arrays.asList(sslDeps.getEnabledCipherSuites());
        assertNotNull(ciphers);
        assertTrue(ciphers.size() > 0);
        String BLACK_LISTED_CIPHER = "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256";
        assertFalse(ciphers.contains(BLACK_LISTED_CIPHER));

        SSLSocketProvider provider = new SSLSocketProvider(sslDeps);
        try
            {
            provider.ensureSessionValidity(
                    sslDeps.getSSLContext().createSSLEngine().getSession(),
                    new WrapperSocket(provider.openSocket())
                        {
                        public InetAddress getInetAddress()
                            {
                            try
                                {
                                return InetAddress.getLocalHost();
                                }
                            catch (UnknownHostException e)
                                {
                                throw new IllegalStateException();
                                }
                            }
                        });
            }
        catch (SSLException sse)
            {
            fail("SSLException: "+sse);
            }
        }


    @Test
    public void testCustomConfiguration()
            throws IOException
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-custom.xml", null);

        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        DocumentProcessor.DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        // establish the cluster-config processing context
        DefaultProcessingContext sslContext = new DefaultProcessingContext(dep, xml);

        // add the default namespace handler
        NamespaceHandler handler = dep.getDefaultNamespaceHandler();
        if (handler != null)
            {
            sslContext.ensureNamespaceHandler("", handler);
            }

        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(sslContext, xml);
        SSLSocketProviderDefaultDependencies depsSSL = new SSLSocketProviderDefaultDependencies(null);

        ctxSocketProviders.addCookie(SSLSocketProviderDefaultDependencies.class, depsSSL);


        SSLSocketProviderDependenciesBuilder bldr = new SSLProcessor().process(ctxSocketProviders, xml);

        SSLSocketProviderDefaultDependencies sslDeps  = bldr.realize();
        SSLSocketProvider                    provider = new SSLSocketProvider(sslDeps);

        SSLContext ctx = sslDeps.getSSLContext();
        assertNotNull(ctx);
        assertEquals(ctx.getProtocol(), sslDeps.DEFAULT_SSL_PROTOCOL);
        assertNotNull(sslDeps.getExecutor());
        assertNotNull(sslDeps.getHostnameVerifier());

        assertEquals(CustomHostnameVerifier.class, sslDeps.getHostnameVerifier().getClass());
        CustomHostnameVerifier verifier = (CustomHostnameVerifier) sslDeps.getHostnameVerifier();
        assertTrue(verifier.isAllowed());

        List<String> protocolVersions = Arrays.asList(sslDeps.getEnabledProtocolVersions());
        assertTrue(protocolVersions.contains("knockknock"));
        assertTrue(protocolVersions.contains("slowboat"));
        assertTrue(protocolVersions.contains("jet"));

        List<String> cipherSuites     = Arrays.asList(sslDeps.getEnabledCipherSuites());
        assertTrue(cipherSuites.contains("twizzlers"));
        assertTrue(cipherSuites.contains("snickers"));

        try 
            {
            provider.ensureSessionValidity(
                sslDeps.getSSLContext().createSSLEngine().getSession(),
                new WrapperSocket(provider.openSocket())
                    {
                    public InetAddress getInetAddress()
                        {
                        try
                            {
                            return InetAddress.getLocalHost();
                            }
                        catch (UnknownHostException e)
                            {
                            throw new IllegalStateException();
                            }
                        }
                    });
            }
        catch (SSLException sse)    
            {
            fail("SSLException: "+sse);
            }
        }
    }
