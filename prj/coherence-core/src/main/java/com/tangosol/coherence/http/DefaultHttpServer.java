/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.DaemonThreadFactory;

import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import javax.security.auth.Subject;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.jersey.internal.MapPropertiesDelegate;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

/**
 * Implementation of {@link HttpServer} that uses Sun's lightweight HTTP
 * server to handle requests.
 * <p>
 * This implementation is not recommended for production environments.
 *
 * @author as  2011.06.08
 */
public class DefaultHttpServer
        extends AbstractHttpServer
    {

    // ----- AbstractHttpServer implementation ------------------------------

    /**
    * {@inheritDoc}
    */
    protected void startInternal()
            throws IOException
        {
        System.setProperty("sun.net.httpserver.nodelay", "true");
        HttpServer server = createHttpServer();
        server.start();
        m_server = server;
        resetStats();
        }

    /**
    * {@inheritDoc}
    */
    protected void stopInternal()
            throws IOException
        {
        m_server.stop(0);
        m_server = null;
        }

    /**
    * {@inheritDoc}
    */
    public String getListenAddress()
        {
        if (m_sListenAddress == null)
            {
            m_sListenAddress = m_server.getAddress().getHostString();
            }
        return m_sListenAddress;
        }

    /**
    * {@inheritDoc}
    */
    public int getListenPort()
        {
        if (m_nListenPort == 0)
            {
            m_nListenPort = m_server.getAddress().getPort();
            }
        return m_nListenPort;
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

        for (Map.Entry<String, ResourceConfig> entry : getResourceConfig().entrySet())
            {
            HttpHandler handler = (HttpHandler) createContainer(entry.getValue());
            if (isAuthMethodBasic())
                {
                handler = new BasicAuthenticationHandler(handler);
                }

            server.createContext(entry.getKey(), handler);
            }

        return server;
        }

    // ----- inner class: BasicAuthenticationHandler ------------------------

    /**
     * HTTP basic authenticator.
     */
    private class BasicAuthenticationHandler
            implements HttpHandler
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct BasicAuthenticationHandler instance.
         *
         * @param handler  request handler to delegate to
         */
        public BasicAuthenticationHandler(HttpHandler handler)
            {
            m_handler = handler;
            }

        // ----- HttpHandler methods ----------------------------------------

        /**
         * Authenticate user based on the credentials specified in the
         * Authorization request header and delegate request processing to
         * wrapped HttpHandler if authentication is successful.
         *
         * @param exchange  HTTP exchange for the request
         *
         * @throws IOException  if an error occurs
         */
        public void handle(final HttpExchange exchange)
                throws IOException
            {
            String  sAuth   = exchange.getRequestHeaders().getFirst(HEADER_AUTHORIZATION);
            Subject subject = authenticate(sAuth);
            if (subject == null)
                {
                exchange.getResponseHeaders().set(HEADER_WWW_AUTHENTICATE, DEFAULT_BASIC_AUTH_HEADER_VALUE);
                exchange.sendResponseHeaders(401, -1);
                }
            else
                {
                exchange.setAttribute(ATTR_SUBJECT, subject);
                m_handler.handle(exchange);
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * Request handler to delegate to when authentication is successful.
         */
        protected final HttpHandler m_handler;
        }

    // ----- inner class: HttpServerContainer -------------------------------

    @Override
    protected Object instantiateContainer(ResourceConfig config, ServiceLocator locator)
        {
        return new HttpServerContainer(config.getApplication(), locator);
        }

    /**
     * Class copied from jersey-server Jersey module and modified to
     * create security context for the request when HTTP basic or client
     * certificate authentication is enabled.
     */
    private class HttpServerContainer
            implements HttpHandler, Container
        {

        // ----- constructors -----------------------------------------------

        /**
         * Create new lightweight Java SE HTTP server container.
         *
         * @param application    JAX-RS / Jersey application to be deployed on the container.
         * @param parentLocator  parent HK2 service locator.
         */
        HttpServerContainer(final Application application, final ServiceLocator parentLocator)
            {
            this.m_hApplication = new ApplicationHandler(application, null, parentLocator);
            }

        // ----- HttpHandler interface --------------------------------------

        /**
         * Handle the given request and generate an appropriate response. See
         * HttpExchange for a description of the steps involved in handling
         * an exchange.
         *
         * @param exchange  the exchange containing the request from the
         *                  client and used to send the response
         *
         * @throws IOException on I/O error
         */
        public void handle(HttpExchange exchange)
                throws IOException
            {
            Writer responseWriter = null;
            incrementRequestCount();
            long ldtStart = Base.getLastSafeTimeMillis();

            try
                {
                // this is a URI that contains the path, query and fragment
                // components
                URI uriExchange = exchange.getRequestURI();

                // the base path specified by the HTTP context of the HTTP
                // handler in decoded form
                String sDecodedBasePath = exchange.getHttpContext().getPath();

                // ensure that the base path ends with a '/'
                if (!sDecodedBasePath.endsWith(SLASH))
                    {
                    if (sDecodedBasePath.equals(uriExchange.getPath()))
                        {
                        // This is an edge case where the request path does not
                        // end in a '/' and is equal to the context path of the
                        // HTTP handler. Both the request path and base path need
                        // to end in a '/'. Currently the request path is modified.
                        uriExchange = UriBuilder.fromUri(uriExchange).
                                path(SLASH).build();
                        }
                    sDecodedBasePath += SLASH;
                    }

                // the following is madness - there is no easy way to get the
                // complete URI of the HTTP request!
                String sScheme = (exchange instanceof HttpsExchange) ? "https" : "http";

                URI uriBase;
                List<String> listHostHeader = exchange.getRequestHeaders().get("Host");
                if (listHostHeader == null)
                    {
                    InetSocketAddress addr = exchange.getLocalAddress();
                    uriBase = new URI(sScheme, null, addr.getHostName(),
                            addr.getPort(), sDecodedBasePath, null, null);
                    }
                else
                    {
                    uriBase = new URI(sScheme + "://" + listHostHeader.get(0) + sDecodedBasePath);
                    }

                Subject subject = null;
                String  sAuth   = null;
                if (isAuthMethodBasic())
                    {
                    sAuth   = SecurityContext.BASIC_AUTH;
                    subject = (Subject) exchange.getAttribute("__SUBJECT");
                    }
                else if (isSecure() && isAuthMethodCert())
                    {
                    sAuth = SecurityContext.CLIENT_CERT_AUTH;

                    SSLSession session = ((HttpsExchange) exchange).getSSLSession();
                    subject = getSubjectFromSession(session);
                    }

                ContainerRequest request = new ContainerRequest(
                        uriBase,
                        uriBase.resolve(uriExchange),
                        exchange.getRequestMethod(),
                        new SimpleSecurityContext(sAuth,
                            subject == null ? exchange.getPrincipal() : subject.getPrincipals().iterator().next(), isSecure()),
                        new MapPropertiesDelegate());
                request.setEntityStream(exchange.getRequestBody());
                request.getHeaders().putAll(exchange.getRequestHeaders());
                responseWriter = new Writer(exchange);
                request.setWriter(responseWriter);
                handleRequest(m_hApplication, request, subject);
                }
            catch (Exception e)
                {
                CacheFactory.log(
                        "Caught unhandled exception while processing an HTTP request: "
                        + Base.printStackTrace(e)
                );
                exchange.getResponseHeaders().clear();
                exchange.sendResponseHeaders(500, -1);
                incrementErrors();
                }
            finally
                {
                if (responseWriter != null)
                    {
                    responseWriter.commit();
                    }
                }

            logRequestTime(ldtStart);
            logStatusCount(exchange.getResponseCode());
            }

        // ----- Container interface --------------------------------

        @Override
        public ResourceConfig getConfiguration()
            {
            return m_hApplication.getConfiguration();
            }

        @Override
        public ApplicationHandler getApplicationHandler()
            {
            return m_hApplication;
            }

        /**
         * Called when reloading of the container is requested.
         */
        @Override
        public void reload()
            {
            reload(getConfiguration());
            }

        @Override
        public void reload(ResourceConfig config)
            {
            m_hApplication.onShutdown(this);
            m_hApplication = new ApplicationHandler(config);
            m_hApplication.onReload(this);
            m_hApplication.onStartup(this);
            }

        /**
         * Inform this container that the server has been started.
         *
         * This method must be implicitly called after the server containing this container is started.
         */
        void onServerStart()
            {
            this.m_hApplication.onStartup(this);
            }

        /**
         * Inform this container that the server is being stopped.
         *
         * This method must be implicitly called before the server containing this container is stopped.
         */
        void onServerStop()
            {
            this.m_hApplication.onShutdown(this);
            }

        // ----- inner class: Writer ----------------------------------------

        /**
         * ContainerResponseWriter implementation for this container.
         */
        private class Writer
                implements ContainerResponseWriter
            {

            // ----- constructors --------------------------------------------

            /**
             * Create a new Writer for the given HTTP exchange.
             *
             * @param exchange  the exchange containing the request from the
             *                  client and used to send the response
             */
            public Writer(HttpExchange exchange)
                {
                this.m_exchange = exchange;
                this.m_closed   = new AtomicBoolean(false);
                }

            // ----- ContainerResponseWriter interface ----------------------

            /**
             * Write the status and headers of the response and return an
             * output stream for the web application to write the entity of
             * the response.
             *
             * @param cbContent  >=0 if the content length in bytes of the
             *                    entity to be written is known, otherwise -1.
             *                    Containers may use this value to determine
             *                    whether the "Content-Length" header can be
             *                    set or utilize chunked transfer encoding.
             * @param response    the container response. The status and
             *                    headers are obtained from the response.
             *
             * @return the output stream to write the entity (if any)
             *
             * @throws IOException on I/O error
             */
            public OutputStream writeResponseStatusAndHeaders(long cbContent,
                    ContainerResponse response)
                    throws ContainerException
                {
                HttpExchange exchange = m_exchange;

                Headers headers = exchange.getResponseHeaders();
                for (Map.Entry<String, List<String>> entry :
                        response.getStringHeaders().entrySet())
                    {
                    List<String> list = new ArrayList<String>();
                    for (String o : entry.getValue())
                        {
                        list.add(o);
                        }
                    headers.put(entry.getKey(), list);
                    }

                try
                    {
                    if (response.getStatus() == 204)
                        {
                        // work around bug in LW HTTP server:
                        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6886436
                        exchange.sendResponseHeaders(response.getStatus(), -1);
                        }
                    else
                        {
                        exchange.sendResponseHeaders(response.getStatus(),
                                getResponseLength(cbContent));
                        }
                    }
                catch (IOException e)
                    {
                    throw new ContainerException("Error during writing out the response headers.", e);
                    }
                return exchange.getResponseBody();
                }

            /**
             * Finish writing the response. This enables the container
             * response writer to clean up any state or flush any streams.
             *
             * @throws IOException on I/O error
             */
            public void finish()
                    throws IOException
                {
                }

            @Override
            public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler hTimeout)
                {
                throw new UnsupportedOperationException("Method suspend is not support by the container.");
                }

            @Override
            public void setSuspendTimeout(long timeout, TimeUnit timeUnit)
                throws IllegalStateException
                {
                throw new UnsupportedOperationException("Method setSuspendTimeout is not support by the container.");
                }

            @Override
            public void failure(Throwable error)
                {
                try
                    {
                    m_exchange.sendResponseHeaders(500, getResponseLength(0));
                    }
                catch (IOException e)
                    {
                    CacheFactory.log("Unable to send a failure response: "
                            + Base.printStackTrace(e), CacheFactory.LOG_WARN);

                    }
                finally
                    {
                    commit();
                    rethrow(error);
                    }
                }

            @Override
            public boolean enableResponseBuffering()
                {
                return true;
                }

            @Override
            public void commit()
                {
                if (m_closed.compareAndSet(false, true))
                    {
                    m_exchange.close();
                    }
                }

            // ----- helper methods -----------------------------------------

            /**
             * Return the response length for the specified number of bytes.
             *
             * @param cb  the number of bytes in the reponse
             *
             * @return the response length
             */
            private long getResponseLength(long cb)
                {
                return cb == 0 ? -1 : cb < 0 ? 0 : cb;
                }

            private void rethrow(Throwable error)
                {
                if (error instanceof RuntimeException)
                    {
                    throw (RuntimeException) error;
                    }
                else
                    {
                    throw new ContainerException(error);
                    }
                }

            // ----- data members -------------------------------------------

            /**
             * The HTTP exchange.
             */
            final HttpExchange  m_exchange;

            /**
             * A flag to indicate whether the writer is closed.
             */
            final AtomicBoolean m_closed;
            }

        // ----- data members -----------------------------------------------

        /**
         * The application.
         */
        private volatile ApplicationHandler m_hApplication;

        /**
         * The container listener.
         */
        private volatile ContainerLifecycleListener m_containerListener;
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
