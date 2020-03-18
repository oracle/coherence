/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.http.DefaultHttpServer;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;
import http.HttpServerStub;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class HttpAcceptorDependenciesProcessorTest
    {
    @Test
    public void shouldUseDefaultHttpServer()
        {
        String                            sXmlFile  = "default-http-acceptor.xml";
        XmlDocument                       xml       = XmlHelper.loadFileOrResource(sXmlFile, "test");
        ProcessingContext                 context   = new DefaultProcessingContext();
        HttpAcceptorDependenciesProcessor processor = new HttpAcceptorDependenciesProcessor();
        HttpAcceptorDependencies          dependencies = processor.process(context, xml.getElement("http-acceptor"));

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getHttpServer(), is(instanceOf(DefaultHttpServer.class)));
        }

    @Test
    public void shouldUseSpecificHttpServer()
        {
        String                                sXmlFile  = "specified-server-http-acceptor.xml";
        XmlDocument                           xml       = XmlHelper.loadFileOrResource(sXmlFile, "test");
        DocumentProcessor.DefaultDependencies deps = new DocumentProcessor.DefaultDependencies();

        deps.setExpressionParser(new ParameterMacroExpressionParser());

        DefaultProcessingContext          context   = new DefaultProcessingContext(deps);
        HttpAcceptorDependenciesProcessor processor = new HttpAcceptorDependenciesProcessor();
        HttpAcceptorDependencies          dependencies = processor.process(context, xml.getElement("http-acceptor"));

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
            ProcessingContext                 context   = new DefaultProcessingContext();
            HttpAcceptorDependenciesProcessor processor = new HttpAcceptorDependenciesProcessor();
            HttpAcceptorDependencies          dependencies = processor.process(context, xml.getElement("http-acceptor"));

            assertThat(dependencies, is(notNullValue()));
            assertThat(dependencies.getHttpServer(), is(instanceOf(HttpServerStub.class)));
            }
        finally
            {
            Thread.currentThread().setContextClassLoader(parent);
            }
        }
    }
