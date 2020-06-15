/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.ssl;

import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.SimpleDocument;

import com.oracle.coherence.common.internal.net.WrapperSocket;
import com.oracle.coherence.common.net.SSLSocketProvider;

import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import static org.junit.Assert.*;

import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
* Unit test of the SSLSocketProvider class.
*
* @author jh  2010.04.26
*/
public class SSLSocketProviderTest
    {
    @Test
    public void testConstructor()
        {
        new SSLSocketProvider();
        }

    @Test
    public void testInvalidConfiguration()
        {
        try
            {
            LegacyXmlSSLSocketProviderDependencies sslDeps = new LegacyXmlSSLSocketProviderDependencies(null);
            fail("Expected IllegalArgumentException");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }

        SimpleDocument xml = new SimpleDocument("foo");
        try
            {
            LegacyXmlSSLSocketProviderDependencies sslDeps = new LegacyXmlSSLSocketProviderDependencies(null);
            SSLSocketProvider provider = new SSLSocketProvider(sslDeps);
            
            fail("Expected IllegalArgumentException");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testSimpleClientConfiguration()
            throws IOException
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-client.xml", null);
        LegacyXmlSSLSocketProviderDependencies sslDeps = new LegacyXmlSSLSocketProviderDependencies(xml);
        SSLContext ctx = sslDeps.getSSLContext();
        assertNotNull(ctx);
        assertEquals(ctx.getProtocol(), LegacyXmlSSLSocketProviderDependencies.DEFAULT_SSL_PROTOCOL);
        assertNotNull(sslDeps.getExecutor());
        assertNull(sslDeps.getHostnameVerifier());
        }

    @Test
    public void testSimpleServerConfiguration()
            throws IOException
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-server.xml", null);
        LegacyXmlSSLSocketProviderDependencies sslDeps = new LegacyXmlSSLSocketProviderDependencies(xml);
        SSLContext ctx = sslDeps.getSSLContext();
        assertNotNull(ctx);
        assertEquals(ctx.getProtocol(), LegacyXmlSSLSocketProviderDependencies.DEFAULT_SSL_PROTOCOL);
        assertNotNull(sslDeps.getExecutor());
        assertNull(sslDeps.getHostnameVerifier());
        SSLSocketProvider provider = new SSLSocketProvider();
        }

    @Test
    public void testCustomConfiguration()
            throws IOException
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-custom.xml", null);
        LegacyXmlSSLSocketProviderDependencies sslDeps = new LegacyXmlSSLSocketProviderDependencies(xml);
        SSLSocketProvider provider = new SSLSocketProvider(sslDeps);
        SSLContext ctx = sslDeps.getSSLContext();
        assertNotNull(ctx);
        assertEquals(ctx.getProtocol(), sslDeps.DEFAULT_SSL_PROTOCOL);
        assertNotNull(sslDeps.getExecutor());
        assertNotNull(sslDeps.getHostnameVerifier());

        assertEquals(CustomHostnameVerifier.class, sslDeps.getHostnameVerifier().getClass());
        CustomHostnameVerifier verifier = (CustomHostnameVerifier) sslDeps.getHostnameVerifier();
        assertTrue(verifier.isAllowed());
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
