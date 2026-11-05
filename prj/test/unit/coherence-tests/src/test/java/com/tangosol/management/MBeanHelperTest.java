/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.management;

import com.oracle.coherence.testing.SystemPropertyResource;

import com.tangosol.internal.net.management.GatewayDependencies;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanServerFinder;

import java.io.IOException;
import java.io.Serializable;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import java.util.ArrayList;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk 2014.09.29
 */
public class MBeanHelperTest
    {
    @Test
    public void shouldUseFirstExistingMBeanServer()
            throws Exception
        {
        MBeanServerFactory.createMBeanServer("Server0");

        GatewayDependencies deps = mock(GatewayDependencies.class);

        when(deps.getDefaultDomain()).thenReturn(null);

        ArrayList<MBeanServer> listServers = MBeanServerFactory.findMBeanServer(null);
        MBeanServer            mBeanServer = MBeanHelper.findMBeanServer("", deps);

        assertThat(mBeanServer, is(sameInstance(listServers.get(0))));
        }

    @Test
    public void shouldUseNamedPlatformMBeanServer()
            throws Exception
        {
        GatewayDependencies deps = mock(GatewayDependencies.class);

        when(deps.getDefaultDomain()).thenReturn("Server1");

        MBeanServer expectedMBeanServer = MBeanServerFactory.createMBeanServer("Server1");
        MBeanServer mBeanServer         = MBeanHelper.findMBeanServer("", deps);

        assertThat(mBeanServer, is(sameInstance(expectedMBeanServer)));
        }

    @Test
    public void shouldUseProvidedMBeanServer()
            throws Exception
        {
        GatewayDependencies deps                = mock(GatewayDependencies.class);
        MBeanServerFinder   finder              = mock(MBeanServerFinder.class);
        MBeanServer         expectedMBeanServer = MBeanServerFactory.createMBeanServer("Server2");

        when(deps.getDefaultDomain()).thenReturn("Server2");
        when(deps.getMBeanServerFinder()).thenReturn(finder);
        when(finder.findMBeanServer("Server2")).thenReturn(expectedMBeanServer);

        MBeanServer mBeanServer = MBeanHelper.findMBeanServer("", deps);

        assertThat(mBeanServer, is(sameInstance(expectedMBeanServer)));
        }

    @Test
    public void shouldCreateMBeanServer()
            throws Exception
        {
        GatewayDependencies deps = mock(GatewayDependencies.class);

        when(deps.getDefaultDomain()).thenReturn("Server3");

        MBeanServer            mBeanServer  = MBeanHelper.findMBeanServer("", deps);

        ArrayList<MBeanServer> listServers  = MBeanServerFactory.findMBeanServer(null);
        MBeanServer            serverResult = null;

        for (MBeanServer server : listServers)
            {
            if (server.getDefaultDomain().equals("Server3"))
                {
                serverResult = server;
                break;
                }
            }

        assertThat(serverResult, is(notNullValue()));
        assertThat(mBeanServer, is(sameInstance(serverResult)));
        }

    @Test
    public void testSafeUnquote()
        {
        String name   = "Test APP";
        String quoted = "\"Test APP\"";

        String newStr = MBeanHelper.safeUnquote(quoted);
        assertThat(newStr, is(name));

        newStr = MBeanHelper.safeUnquote(name);
        assertThat(newStr, is(name));
        }

    @Test
    public void shouldAllowWildcardPropertyValue()
            throws MalformedObjectNameException
        {
        ObjectName objectName = new ObjectName(MBeanHelper.quoteCanonical("Coherence:type=*HttpSessionManager,appId=" + "testapp"));
        assertThat(objectName.isPropertyValuePattern("type"), is(true));
        }

    @Test
    public void shouldAllowAnyCharacterPropertyValue()
            throws MalformedObjectNameException
        {
        ObjectName objectName = new ObjectName(MBeanHelper.quoteCanonical("Coherence:type=?HttpSessionManager,appId=" + "testapp"));
        assertThat(objectName.isPropertyValuePattern("type"), is(true));
        }

    @Test
    public void shouldEscapeWildcardAnyCharInKeyName()
            throws MalformedObjectNameException
        {
        String sNameCanonical = MBeanHelper.quoteCanonical("Coherence:type*?=*HttpSessionManager,appId=" + "testapp");
        assertThat(sNameCanonical, is("Coherence:\"type\\*\\?\"=*HttpSessionManager,appId=testapp"));
        }

    @Test
    public void shouldQuoteScopedServiceName()
            throws MalformedObjectNameException
        {
        String sNameCanonical = MBeanHelper.quoteCanonical("Coherence:service=SYS:Config");
        assertThat(sNameCanonical, is("Coherence:service=\"SYS:Config\""));
        }

    @Test
    public void shouldNotQuoteAlreadyQuotedScopedServiceName()
            throws MalformedObjectNameException
        {
        String sNameCanonical = MBeanHelper.quoteCanonical("Coherence:service=\"SYS:Config\"");
        assertThat(sNameCanonical, is("Coherence:service=\"SYS:Config\""));
        }
    @Test
    public void shouldPreserveTheCallerSocketFactoryForAWildcardBind()
            throws Exception
        {
        assertCallerSocketFactory("0.0.0.0");
        }

    @Test
    public void shouldPreserveTheCallerSocketFactoryForAConcreteBind()
            throws Exception
        {
        assertCallerSocketFactory("127.0.0.1");
        }

    private void assertCallerSocketFactory(String host)
            throws Exception
        {
        try (SystemPropertyResource auth = new SystemPropertyResource("com.sun.management.jmxremote.authenticate", "false");
             SystemPropertyResource ssl = new SystemPropertyResource("com.sun.management.jmxremote.ssl", "false");
             SystemPropertyResource hostname = new SystemPropertyResource("java.rmi.server.hostname", "127.0.0.1"))
            {
            MBeanServer server = MBeanServerFactory.createMBeanServer("CustomSocketFactoryTest");
            AtomicInteger calls = new AtomicInteger();
            RMIServerSocketFactory factory = port ->
                {
                calls.incrementAndGet();
                return new ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"));
                };
            JMXConnectorServer connector = null;
            try
                {
                connector = MBeanHelper.startRmiConnector(host, 0, 0, server,
                        Map.of("jmx.remote.rmi.server.socket.factory", factory));
                try (JMXConnector client = JMXConnectorFactory.connect(connector.getAddress()))
                    {
                    assertEquals("CustomSocketFactoryTest", client.getMBeanServerConnection().getDefaultDomain());
                    }
                assertTrue("Caller-supplied server socket factory was ignored", calls.get() > 0);
                }
            finally
                {
                if (connector != null)
                    {
                    connector.stop();
                    }
                MBeanServerFactory.releaseMBeanServer(server);
                }
            }
        }

    @Test
    public void shouldPreserveTheExplicitExportedRmiHostname()
            throws Exception
        {
        try (SystemPropertyResource auth = new SystemPropertyResource("com.sun.management.jmxremote.authenticate", "false");
             SystemPropertyResource ssl = new SystemPropertyResource("com.sun.management.jmxremote.ssl", "false");
             SystemPropertyResource hostname = new SystemPropertyResource("java.rmi.server.hostname", "localhost"))
            {
            MBeanServer server = MBeanServerFactory.createMBeanServer("ExplicitRmiHostnameTest");
            JMXConnectorServer connector = null;
            RecordingClientSocketFactory.s_sHost = null;
            try
                {
                connector = MBeanHelper.startRmiConnector("127.0.0.1", 0, 0, server,
                        Map.of("jmx.remote.rmi.client.socket.factory", new RecordingClientSocketFactory()));
                try (JMXConnector client = JMXConnectorFactory.connect(connector.getAddress()))
                    {
                    assertEquals("ExplicitRmiHostnameTest", client.getMBeanServerConnection().getDefaultDomain());
                    }
                assertEquals("localhost", RecordingClientSocketFactory.s_sHost);
                }
            finally
                {
                if (connector != null)
                    {
                    connector.stop();
                    }
                MBeanServerFactory.releaseMBeanServer(server);
                }
            }
        }

    private static class RecordingClientSocketFactory
            implements RMIClientSocketFactory, Serializable
        {
        @Override
        public Socket createSocket(String host, int port)
                throws IOException
            {
            s_sHost = host;
            return new Socket(host, port);
            }

        private static volatile String s_sHost;
        private static final long serialVersionUID = 1L;
        }
    }
