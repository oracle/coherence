/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import http.HttpServerStub;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class HttpServerTest
    {
    @Test
    public void shouldCreateDefaultHttpServer()
        {
        HttpServer server = HttpServer.create();
        assertThat(server, is(notNullValue()));
        assertThat(server, is(instanceOf(DefaultHttpServer.class)));
        }

    @Test
    public void shoulDiscoverHttpServer() throws Exception
        {
        // Create a ClassLoader that has the META-INF/services folder from under
        // resources/http so that this folder will be on the classpath when we get the server
        URL url = HttpServerStub.class.getProtectionDomain().getCodeSource().getLocation();
        File fileTestClasses = new File(url.toURI());
        File folder = new File(fileTestClasses, "http");

        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        ClassLoader loader = new URLClassLoader(new URL[]{folder.toURI().toURL()});

        try
            {
            Thread.currentThread().setContextClassLoader(loader);
            HttpServer server = HttpServer.create();
            assertThat(server, is(notNullValue()));
            assertThat(server, is(instanceOf(HttpServerStub.class)));
            }
        finally
            {
            Thread.currentThread().setContextClassLoader(parent);
            }
        }
    }
