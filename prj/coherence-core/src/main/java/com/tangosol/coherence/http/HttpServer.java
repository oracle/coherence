/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.net.Session;
import com.tangosol.net.Service;

import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.Application;

/**
 * An interface implemented by embedded HTTP servers.
 *
 * @author as  2011.06.03
 */
public interface HttpServer
    {
    // ----- factory methods ------------------------------------------------

    /**
     * Return a {@link HttpServer} implementation to use.
     * <p>
     * The {@link HttpServer} instance will be discovered by the
     * {@link ServiceLoader}. This assumes that there is a single
     * {@link HttpServer} service on the classpath. If multiple
     * implementations are available then the first instance
     * discovered will be returned.
     * <p>
     * If the service loader is unable to discover any {@link HttpServer}
     * implementations then an instance of {@link DefaultHttpServer} will
     * be returned.
     * <p>
     * After creating the {@link HttpServer} this method will use the {@link ServiceLoader}
     * to discover instances of {@link Application JAX-RS Applications} and
     * add them to the {@link HttpServer}.
     *
     * @return  the {@link HttpServer} to use
     */
    public static HttpServer create()
        {
        ServiceLoader<HttpServer> loaderServer = ServiceLoader.load(HttpServer.class);
        Iterator<HttpServer>      itServer     = loaderServer.iterator();

        return itServer.hasNext() ? itServer.next() : new DefaultHttpServer();
        }

    // ----- configuration methods ------------------------------------------

    /**
     * Set the client authentication method to use.
     * <p>
     * Valid values <b>basic</b> for HTTP basic authentication, <b>cert</b>
     * for client certificate authentication, <b>cert+basic</b> for both
     * client certificate and HTTP basic authentication, and <b>none</b> for
     * no authentication.
     *
     * @param sMethod  the authentication method to use
     */
    public void setAuthMethod(String sMethod);

    /**
     * Set Coherence session to use.
     *
     * @param session  the Coherence session
     */
    public void setSession(Session session);

    /**
     * Set the address server should listen on.
     *
     * @param sAddr  the address
     */
    public void setLocalAddress(String sAddr);

    /**
     * Set the port number server should listen on.
     *
     * @param nPort  the port number
     */
    public void setLocalPort(int nPort);

    /**
     * Set the Service that is embedding this HttpServer.
     *
     * @param service  the parent service
     */
    public void setParentService(Service service);

    /**
     * Set the Jersey ResourceConfig to use.
     * <p>
     * This method will register specified application under the root context,
     * which is equivalent to:
     * <code>
     *     setResourceConfig(Collections.singletonMap("/", config));
     * </code>
     *
     * @param config  the resource config for a Jersey web application
     */
    public void setResourceConfig(ResourceConfig config);

    /**
     * Set the map of Jersey ResourceConfig to use.
     *
     * @param mapConfig  the map of context names to corresponding Jersey
     *                   resource configs to use
     */
    public void setResourceConfig(Map<String, ResourceConfig> mapConfig);

    /**
     * Set the SocketProvider to use.
     *
     * @param provider  the SocketProvider
     */
    public void setSocketProvider(SocketProvider provider);

    // ----- lifecycle methods ----------------------------------------------

    /**
     * Start the server.
     *
     * @throws IOException if an error occurs
     */
    public void start()
            throws IOException;

    /**
     * Stop the server.
     *
     * @throws IOException if an error occurs
     */
    public void stop()
            throws IOException;

    // ----- runtime information --------------------------------------------

    /**
     * Get the server's listen address.
     *
     * @return the server's listen address
     *
     * @since 12.2.1.4.0
     */
    String getListenAddress();

    /**
     * Get the server's listen port
     *
     * @return the listen port
     *
     * @since 12.2.1.4.0
     */
    int getListenPort();
    }
