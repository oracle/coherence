/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.oracle.coherence.common.net.SdpSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
* Unit test of the SocketProviderFactory class.
*
* @author jh  2010.04.26
*/
public class SocketProviderFactoryTest
    {
    @Before
    public void init()
        {
        FACTORY = new SocketProviderFactory();
        }

    @Test
    public void testDefaultProvider()
        {
        assertEquals(FACTORY.getSocketProvider((XmlElement)null),
                SocketProviderFactory.DEFAULT_SOCKET_PROVIDER);

        XmlElement xml = XmlHelper.loadXml("<socket-provider/>");
        assertEquals(FACTORY.getSocketProvider(xml),
                SocketProviderFactory.DEFAULT_SOCKET_PROVIDER);
        }

    @Test
    public void testSSLProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        SocketProvider provider = FACTORY.getSocketProvider(xml);
        assertTrue(provider instanceof SSLSocketProvider);
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertTrue(delegate instanceof MultiplexedSocketProvider);
        assertTrue(((MultiplexedSocketProvider) delegate).getDependencies().getDelegateProvider() instanceof TcpSocketProvider);
        }

    @Test
    public void testSystemProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><system/></socket-provider>");
        SocketProvider provider = FACTORY.getSocketProvider(xml);
        assertTrue(provider instanceof MultiplexedSocketProvider);
        assertTrue(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider() instanceof TcpSocketProvider);
        }

    @Test
    public void testTcpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><tcp/></socket-provider>");
        SocketProvider provider = FACTORY.getSocketProvider(xml);
        assertTrue(provider instanceof MultiplexedSocketProvider);
        assertTrue(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider() instanceof TcpSocketProvider);
        }

    @Test
    public void testSDPProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><sdp/></socket-provider>");
        SocketProvider provider = FACTORY.getSocketProvider(xml);
        assertTrue(provider instanceof MultiplexedSocketProvider);
        assertTrue(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider() instanceof SdpSocketProvider);
        }

    @Test
    public void testSdpsProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl><socket-provider>sdp</socket-provider></ssl></socket-provider>");
        SocketProvider provider = FACTORY.getSocketProvider(xml);
        assertTrue(provider instanceof SSLSocketProvider);
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertTrue(delegate instanceof MultiplexedSocketProvider);
        assertTrue(((MultiplexedSocketProvider) delegate).getDependencies().getDelegateProvider() instanceof SdpSocketProvider);
        }

    @Test
    public void testDemultiplexedSSLProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        SocketProvider provider = FACTORY.getDemultiplexedSocketProvider(xml, 1);
        assertTrue(provider instanceof SSLSocketProvider);
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertTrue(delegate instanceof DemultiplexedSocketProvider);
        }

    @Test
    public void testDemultiplexedSystemProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><system/></socket-provider>");
        SocketProvider provider = FACTORY.getDemultiplexedSocketProvider(xml, 1);
        assertTrue(provider instanceof DemultiplexedSocketProvider);
        }

    @Test
    public void testDemultiplexedTcpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><tcp/></socket-provider>");
        SocketProvider provider = FACTORY.getDemultiplexedSocketProvider(xml, 1);
        assertTrue(provider instanceof DemultiplexedSocketProvider);
        }

    @Test
    public void testDemultiplexedSDPProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><sdp/></socket-provider>");
        SocketProvider provider = FACTORY.getDemultiplexedSocketProvider(xml, 1);
        assertTrue(provider instanceof DemultiplexedSocketProvider);
        }

    @Test
    public void testDemultiplexedSdpsProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl><socket-provider>sdp</socket-provider></ssl></socket-provider>");
        SocketProvider provider = FACTORY.getDemultiplexedSocketProvider(xml, 1);
        assertTrue(provider instanceof SSLSocketProvider);
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertTrue(delegate instanceof DemultiplexedSocketProvider);
        }

    @Test
    public void testLegacyDefaultProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider/>");
        assertEquals(FACTORY.getLegacySocketProvider(xml),SocketProviderFactory.DEFAULT_LEGACY_SOCKET_PROVIDER);
        }

    @Test
    public void testLegacySSLProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        SocketProvider provider = FACTORY.getLegacySocketProvider(xml);
        assertTrue(provider instanceof SSLSocketProvider);
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertTrue(delegate instanceof DemultiplexedSocketProvider);
        delegate = ((DemultiplexedSocketProvider) delegate).getDelegate().getDependencies().getDelegateProvider();
        assertTrue(delegate instanceof TcpSocketProvider);
        }

    @Test
    public void testLegacySystemProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><system/></socket-provider>");
        SocketProvider providerSystem = FACTORY.getLegacySocketProvider(xml);
        SocketProvider providerDelegate = ((DemultiplexedSocketProvider) providerSystem).getDelegate()
                .getDependencies().getDelegateProvider();
        assertEquals(providerDelegate, TcpSocketProvider.INSTANCE);
        }

    @Test
    public void testLegacyTcpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><tcp/></socket-provider>");
        SocketProvider provider = FACTORY.getLegacySocketProvider(xml);
        SocketProvider providerDelegate = ((DemultiplexedSocketProvider) provider).getDelegate()
                .getDependencies().getDelegateProvider();
        assertEquals(providerDelegate, TcpSocketProvider.INSTANCE);
        }

    @Test
    public void testLegacySdpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><sdp/></socket-provider>");
        SocketProvider provider = FACTORY.getLegacySocketProvider(xml);
        SocketProvider providerDelegate = ((DemultiplexedSocketProvider) provider).getDelegate()
                .getDependencies().getDelegateProvider();
        assertEquals(providerDelegate, SdpSocketProvider.INSTANCE);
        }

    @Test
    public void testLegacySdpsProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl><socket-provider>sdp</socket-provider></ssl></socket-provider>");
        SocketProvider provider = FACTORY.getLegacySocketProvider(xml);
        assertTrue(provider instanceof SSLSocketProvider);
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertTrue(delegate instanceof DemultiplexedSocketProvider);
        delegate = ((DemultiplexedSocketProvider) delegate).getDelegate().getDependencies().getDelegateProvider();
        assertTrue(delegate instanceof SdpSocketProvider);
        }

    /*
    Commented out. We will not support custom socket provider going forward.
    @Test
    public void testCustomProvider()
            throws IOException
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><instance><class-name>" +
                "com.tangosol.net.CustomSocketProvider</class-name><init-params>" +
                "<init-param><param-type>int</param-type><param-value>7</param-value>" +
                "</init-param></init-params></instance></socket-provider>");

        SocketProvider provider = FACTORY.getSocketProvider(xml);
        assertEquals(provider.toString(), "CustomSocketProvider");
        }
        */

    public SocketProviderFactory FACTORY;
    }
