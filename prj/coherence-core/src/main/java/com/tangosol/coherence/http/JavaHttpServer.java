/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import com.tangosol.internal.http.ServiceAwareHandler;

import com.tangosol.util.DaemonThreadFactory;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.util.Map;

import java.util.concurrent.Executors;

import javax.net.ssl.SSLParameters;

/**
 * An implementation of {@link GenericHttpServer} that uses Java's lightweight HTTP
 * server to handle requests.
 * <p>
 * This class uses {@link HttpHandler} instances to implement the actual http endpoints to be served.
 *
 * @author Jonathan Knight  2021.08.31
 * @since 21.12
 */
public class JavaHttpServer
        extends AbstractGenericHttpServer<HttpHandler>
    {
    // ----- GenericHttpServer implementation -------------------------------

    @Override
    public Class<HttpHandler> getResourceType()
        {
        return HttpHandler.class;
        }

    @Override
    protected void startInternal()
            throws IOException
        {
        m_mapResourceConfig.values()
                .stream()
                .filter(h -> h instanceof ServiceAwareHandler)
                .map(ServiceAwareHandler.class::cast)
                .forEach(h -> h.setService(m_serviceParent));

        System.setProperty("sun.net.httpserver.nodelay", "true");
        HttpServer server = createHttpServer();
        server.start();
        m_server = server;
        resetStats();
        }

    @Override
    protected void stopInternal()
            throws IOException
        {
        m_server.stop(0);
        m_server = null;
        }

    @Override
    public String getListenAddress()
        {
        if (m_sListenAddress == null)
            {
            m_sListenAddress = m_server.getAddress().getHostString();
            }
        return m_sListenAddress;
        }

    @Override
    public int getListenPort()
        {
        if (m_nListenPort == 0)
            {
            m_nListenPort = m_server.getAddress().getPort();
            }
        return m_nListenPort;
        }

    @Override
    protected Object createContainer(HttpHandler resourceConfig)
        {
        return resourceConfig;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Creates a new HttpServer which will manage all root resource and
     * provider classes declared by the resource configuration.
     *
     * @return the new HttpServer instance
     *
     * @throws IOException if an error occurs while creating the container
     */
    protected HttpServer createHttpServer()
            throws IOException
        {
        InetSocketAddress addr = new InetSocketAddress(getLocalAddress(), getLocalPort());

        HttpServer server;
        if (isSecure())
            {
            HttpsServer sslServer = HttpsServer.create(addr, 0);

            final SSLParameters sslParams = getSSLParameters();
            sslServer.setHttpsConfigurator(new HttpsConfigurator(getSSLContext())
                {
                @Override
                public void configure(HttpsParameters params)
                    {
                    params.setSSLParameters(sslParams);
                    params.setNeedClientAuth(isAuthMethodCert());
                    }
                });

            server = sslServer;
            }
        else
            {
            server = HttpServer.create(addr, 0);
            }

        server.setExecutor(Executors.newCachedThreadPool(
                new DaemonThreadFactory("DefaultHttpServerThread-")));

        for (Map.Entry<String, HttpHandler> entry : getResourceConfig().entrySet())
            {
            HttpHandler handler = (HttpHandler) createContainer(entry.getValue());
            if (isAuthMethodBasic())
                {
                handler = new BasicAuthenticationHandler(handler, this);
                }

            // wrap the handler to update statistics
            handler = new BasicStatisticsHandler(handler, this);

            server.createContext(entry.getKey(), handler);
            }

        return server;
        }

    // ----- data members ---------------------------------------------------

    /**
     * HTTP server instance.
     */
    protected HttpServer m_server;

    /**
     * The cached listen address of this server.
     */
    protected String m_sListenAddress;

    /**
     * The cached listen port of this server.
     */
    protected int m_nListenPort;
    }
