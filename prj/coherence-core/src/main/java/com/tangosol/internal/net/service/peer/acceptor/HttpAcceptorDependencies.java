/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import java.util.Map;

/**
* The HttpAcceptorDependencies interface provides a HttpAcceptor object with its external
* dependencies.
*
* @author pfm  2011.08.25
* @since Coherence 12.1.2
*/
public interface HttpAcceptorDependencies
        extends AcceptorDependencies
    {
    /**
     * Return the HttpServer.
     *
     * @return the HttpServer
     */
    Object getHttpServer();

    /**
     * Return the SocketProviderBuilder that may be used by the HttpAcceptor to
     * create a SocketProvider to open ServerSocketChannels.
     *
     * @return the socket provider builder
     */
    SocketProviderBuilder getSocketProviderBuilder();

    /**
     * Return the local address string.
     *
     * @return the local address string
     */
    String getLocalAddress();

    /**
     * Return the local port.
     *
     * @return the local port
     */
    int getLocalPort();

    /**
     * Return the Jersey ResourceConfig.
     *
     * @return the Jersey resource configuration
     */
    Map<String, Object> getResourceConfig();

    /**
     * Return the authentication mechanism used by the HTTP server.
     * <p>
     * Valid values <b>basic</b> for HTTP basic authentication, <b>cert</b>
     * for client certificate authentication, <b>cert+basic</b> for both
     * client certificate and HTTP basic authentication, and <b>none</b> for
     * no authentication.
     *
     * @return the authentication mechanism
     */
    String getAuthMethod();
    }
