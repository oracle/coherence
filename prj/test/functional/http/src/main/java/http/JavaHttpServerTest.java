/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package http;

import com.tangosol.coherence.http.JavaHttpServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URI;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link JavaHttpServer}.
 *
 * @author jk  2026.05.14
 * @since 26.04
 */
public class JavaHttpServerTest
    {
    @After
    public void cleanup()
            throws IOException
        {
        if (m_server != null)
            {
            m_server.stop();
            }
        }

    @Test
    public void shouldServePlainHttp()
            throws IOException
        {
        start("none");

        HttpURLConnection con = (HttpURLConnection) URI.create("http://"
                + m_server.getListenAddress() + ":" + m_server.getListenPort() + "/test").toURL().openConnection();
        assertThat(con.getResponseCode(), is(200));
        }

    @Test
    public void shouldRejectCertWithoutSsl()
            throws IOException
        {
        assertCertAuthRequiresSsl("cert");
        }

    @Test
    public void shouldRejectCertBasicWithoutSsl()
            throws IOException
        {
        assertCertAuthRequiresSsl("cert+basic");
        }

    private void assertCertAuthRequiresSsl(String sAuth)
            throws IOException
        {
        try
            {
            start(sAuth);
            fail("Expected certificate authentication without SSL to fail");
            }
        catch (IllegalStateException expected)
            {
            // expected
            }
        }

    private void start(String sAuth)
            throws IOException
        {
        JavaHttpServer server = m_server = new JavaHttpServer();
        server.setLocalAddress(System.getProperty("test.extend.address.local", "127.0.0.1"));
        server.setResourceConfig(Collections.singletonMap("/test", new OkHandler()));
        server.setAuthMethod(sAuth);
        server.start();
        }

    // ----- inner class: OkHandler ----------------------------------------

    static class OkHandler
            implements HttpHandler
        {
        @Override
        public void handle(HttpExchange exchange)
                throws IOException
            {
            byte[] ab = "ok".getBytes();
            exchange.sendResponseHeaders(200, ab.length);
            try (OutputStream out = exchange.getResponseBody())
                {
                out.write(ab);
                }
            }
        }

    // ----- data members --------------------------------------------------

    private JavaHttpServer m_server;
    }
