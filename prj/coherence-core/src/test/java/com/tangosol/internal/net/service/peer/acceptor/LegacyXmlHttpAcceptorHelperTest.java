/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.oracle.coherence.common.net.SocketProvider;
import com.tangosol.coherence.http.DefaultHttpServer;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import http.HttpServerStub;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LegacyXmlHttpAcceptorHelperTest
    {
    @Test
    public void shouldUseDefaultHttpServer()
        {
        String                          sXmlFile     = "default-http-acceptor.xml";
        XmlDocument                     xml          = XmlHelper.loadFileOrResource(sXmlFile, "test");
        DefaultHttpAcceptorDependencies dependencies = getDependencies(xml);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getHttpServer(), is(instanceOf(DefaultHttpServer.class)));
        }

    @Test
    public void shouldUseSpecificHttpServer()
        {
        String                          sXmlFile     = "specified-server-http-acceptor.xml";
        XmlDocument                     xml          = XmlHelper.loadFileOrResource(sXmlFile, "test");
        DefaultHttpAcceptorDependencies dependencies = getDependencies(xml);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getHttpServer(), is(instanceOf(HttpServerStub.class)));
        }

    @Test
    public void shouldUseDiscoveredHttpServer() throws Exception
        {
        String      sXmlFile = "default-http-acceptor.xml";
        XmlDocument xml      = XmlHelper.loadFileOrResource(sXmlFile, "test");

        // Create a ClassLoader that has the META-INF/services folder from under
        // resources/http so that this folder will be on the classpath when we get the server
        URL         url             = HttpServerStub.class.getProtectionDomain().getCodeSource().getLocation();
        File        fileTestClasses = new File(url.toURI());
        File        folder          = new File(fileTestClasses, "http");
        ClassLoader parent          = Thread.currentThread().getContextClassLoader();
        ClassLoader loader          = new URLClassLoader(new URL[]{folder.toURI().toURL()});

        try
            {
            Thread.currentThread().setContextClassLoader(loader);
            DefaultHttpAcceptorDependencies dependencies = getDependencies(xml);

            assertThat(dependencies, is(notNullValue()));
            assertThat(dependencies.getHttpServer(), is(instanceOf(HttpServerStub.class)));
            }
        finally
            {
            Thread.currentThread().setContextClassLoader(parent);
            }
        }

    private DefaultHttpAcceptorDependencies getDependencies(XmlElement xml)
        {
        DefaultHttpAcceptorDependencies defaults = new DefaultHttpAcceptorDependencies();
        ClassLoader                     loader   = Base.getContextClassLoader();
        OperationalContext              ctx      = mock(OperationalContext.class);
        SocketProviderFactory           factory  = mock(SocketProviderFactory.class);
        SocketProvider                  provider = mock(SocketProvider.class);

        when(ctx.getSocketProviderFactory()).thenReturn(factory);
        when(factory.getSocketProvider(anyString())).thenReturn(provider);

        return LegacyXmlHttpAcceptorHelper.fromXml(xml, defaults, ctx, loader);
        }
    }
