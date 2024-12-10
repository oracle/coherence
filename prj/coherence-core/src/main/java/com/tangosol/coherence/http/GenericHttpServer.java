/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.net.Service;
import com.tangosol.net.Session;

import java.io.IOException;

import java.util.Map;

/**
 * An interface implemented by embedded HTTP servers.
 *
 * @param <R> the type of the resource configuration classes that handle the http endpoints.
 *
 * @author Jonathan Knight  31.08.2021
 * @since 21.12
 */
public interface GenericHttpServer<R>
    {
    // ----- configuration methods ------------------------------------------

    /**
     * Returns the class that this server expects as a resource configuration type.
     *
     * @return the class that this server expects as a resource configuration type
     */
    public Class<R> getResourceType();

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
     * Set the resource configuration to use to service an endpoint.
     *
     * @param config  the resource configuration for the endpoint
     */
    public void setResourceConfig(R config);

    /**
     * Set a map of resource configurations to service multiple endpoints.
     *
     * @param mapConfig  the map of context names to corresponding resource configurations
     */
    public void setResourceConfig(Map<String, R> mapConfig);

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
     */
    String getListenAddress();

    /**
     * Get the server's listen port
     *
     * @return the listen port
     */
    int getListenPort();
    }
