/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.oracle.coherence.common.net.SdpSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;

import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


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
        m_factory = new SocketProviderFactory();
        SocketProviderFactory.setGlobalSocketProvider(null);
        }

    @Test
    public void testDefaultProvider()
        {
        assertThat(m_factory.getSocketProvider((XmlElement)null),
                   is(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER));

        XmlElement xml = XmlHelper.loadXml("<socket-provider/>");
        assertThat(m_factory.getSocketProvider(xml),
                   is(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER));
        }

    @Test
    public void testDefaultProviderWithGlobalProvider()
        {
        SocketProviderBuilder bldrGlobal     = mock(SocketProviderBuilder.class);
        SocketProvider        providerGlobal = mock(SocketProvider.class);

        when(bldrGlobal.realize(any(), any(), any())).thenReturn(providerGlobal);

        SocketProviderFactory.setGlobalSocketProviderBuilder(bldrGlobal);

        XmlElement xml = XmlHelper.loadXml("<socket-provider/>");
        assertThat(m_factory.getSocketProvider(xml),
                   is(providerGlobal));
        }

    @Test
    public void testSSLProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        SocketProvider provider = m_factory.getSocketProvider(xml);
        assertThat(provider, is(instanceOf(SSLSocketProvider.class)));
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertThat(delegate, is(instanceOf(MultiplexedSocketProvider.class)));
        assertThat(((MultiplexedSocketProvider) delegate).getDependencies().getDelegateProvider(), is(instanceOf(TcpSocketProvider.class)));
        }

    @Test
    public void testSSLProviderWithGlobalProvider()
        {
        SocketProviderFactory.setGlobalSocketProviderBuilder(mock(SocketProviderBuilder.class));
        
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        SocketProvider provider = m_factory.getSocketProvider(xml);
        assertThat(provider, is(instanceOf(SSLSocketProvider.class)));
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertThat(delegate, is(instanceOf(MultiplexedSocketProvider.class)));
        assertThat(((MultiplexedSocketProvider) delegate).getDependencies().getDelegateProvider(), is(instanceOf(TcpSocketProvider.class)));
        }

    @Test
    public void testSystemProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><system/></socket-provider>");
        SocketProvider provider = m_factory.getSocketProvider(xml);
        assertThat(provider, is(instanceOf(MultiplexedSocketProvider.class)));
        assertThat(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider(), is(instanceOf(TcpSocketProvider.class)));
        }

    @Test
    public void testTcpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><tcp/></socket-provider>");
        SocketProvider provider = m_factory.getSocketProvider(xml);
        assertThat(provider, is(instanceOf(MultiplexedSocketProvider.class)));
        assertThat(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider(), is(instanceOf(TcpSocketProvider.class)));
        }

    @Test
    public void testSDPProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><sdp/></socket-provider>");
        SocketProvider provider = m_factory.getSocketProvider(xml);
        assertThat(provider, is(instanceOf(MultiplexedSocketProvider.class)));
        assertThat(((MultiplexedSocketProvider) provider).getDependencies().getDelegateProvider(), is(instanceOf(SdpSocketProvider.class)));
        }

    @Test
    public void testSdpsProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl><socket-provider>sdp</socket-provider></ssl></socket-provider>");
        SocketProvider provider = m_factory.getSocketProvider(xml);
        assertThat(provider, is(instanceOf(SSLSocketProvider.class)));
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertThat(delegate, is(instanceOf(MultiplexedSocketProvider.class)));
        assertThat(((MultiplexedSocketProvider) delegate).getDependencies().getDelegateProvider(), is(instanceOf(SdpSocketProvider.class)));
        }

    @Test
    public void testDemultiplexedSSLProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        SocketProvider provider = m_factory.getDemultiplexedSocketProvider(xml, 1);
        assertThat(provider, is(instanceOf(SSLSocketProvider.class)));
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertThat(delegate, is(instanceOf(DemultiplexedSocketProvider.class)));
        }

    @Test
    public void testDemultiplexedSystemProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><system/></socket-provider>");
        SocketProvider provider = m_factory.getDemultiplexedSocketProvider(xml, 1);
        assertThat(provider, is(instanceOf(DemultiplexedSocketProvider.class)));
        }

    @Test
    public void testDemultiplexedTcpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><tcp/></socket-provider>");
        SocketProvider provider = m_factory.getDemultiplexedSocketProvider(xml, 1);
        assertThat(provider, is(instanceOf(DemultiplexedSocketProvider.class)));
        }

    @Test
    public void testDemultiplexedSDPProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><sdp/></socket-provider>");
        SocketProvider provider = m_factory.getDemultiplexedSocketProvider(xml, 1);
        assertThat(provider, is(instanceOf(DemultiplexedSocketProvider.class)));
        }

    @Test
    public void testDemultiplexedSdpsProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl><socket-provider>sdp</socket-provider></ssl></socket-provider>");
        SocketProvider provider = m_factory.getDemultiplexedSocketProvider(xml, 1);
        assertThat(provider, is(instanceOf(SSLSocketProvider.class)));
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertThat(delegate, is(instanceOf(DemultiplexedSocketProvider.class)));
        }

    @Test
    public void testLegacyDefaultProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider/>");
        assertThat(m_factory.getLegacySocketProvider(xml), is(SocketProviderFactory.DEFAULT_LEGACY_SOCKET_PROVIDER));
        }

    @Test
    public void testLegacySSLProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl/></socket-provider>");
        SocketProvider provider = m_factory.getLegacySocketProvider(xml);
        assertThat(provider, is(instanceOf(SSLSocketProvider.class)));
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertThat(delegate, is(instanceOf(DemultiplexedSocketProvider.class)));
        delegate = ((DemultiplexedSocketProvider) delegate).getDelegate().getDependencies().getDelegateProvider();
        assertThat(delegate, is(instanceOf(TcpSocketProvider.class)));
        }

    @Test
    public void testLegacySystemProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><system/></socket-provider>");
        SocketProvider providerSystem = m_factory.getLegacySocketProvider(xml);
        SocketProvider providerDelegate = ((DemultiplexedSocketProvider) providerSystem).getDelegate()
                .getDependencies().getDelegateProvider();
        assertThat(providerDelegate, is(TcpSocketProvider.INSTANCE));
        }

    @Test
    public void testLegacyTcpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><tcp/></socket-provider>");
        SocketProvider provider = m_factory.getLegacySocketProvider(xml);
        SocketProvider providerDelegate = ((DemultiplexedSocketProvider) provider).getDelegate()
                .getDependencies().getDelegateProvider();
        assertThat(providerDelegate, is(TcpSocketProvider.INSTANCE));
        }

    @Test
    public void testLegacySdpProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><sdp/></socket-provider>");
        SocketProvider provider = m_factory.getLegacySocketProvider(xml);
        SocketProvider providerDelegate = ((DemultiplexedSocketProvider) provider).getDelegate()
                .getDependencies().getDelegateProvider();
        assertThat(providerDelegate, is(SdpSocketProvider.INSTANCE));
        }

    @Test
    public void testLegacySdpsProvider()
        {
        XmlElement xml = XmlHelper.loadXml("<socket-provider><ssl><socket-provider>sdp</socket-provider></ssl></socket-provider>");
        SocketProvider provider = m_factory.getLegacySocketProvider(xml);
        assertThat(provider, is(instanceOf(SSLSocketProvider.class)));
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertThat(delegate, is(instanceOf(DemultiplexedSocketProvider.class)));
        delegate = ((DemultiplexedSocketProvider) delegate).getDelegate().getDependencies().getDelegateProvider();
        assertThat(delegate, is(instanceOf(SdpSocketProvider.class)));
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

    public SocketProviderFactory m_factory;
    }
