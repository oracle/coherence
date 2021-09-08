/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.security.auth.Subject;
import java.io.IOException;

/**
 * HTTP basic authenticator.
 *
 * @author Jonathan Knight  2021.08.02
 * @since 21.12
 */
class BasicAuthenticationHandler
        implements HttpHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct BasicAuthenticationHandler instance.
     *
     * @param handler  request handler to delegate to
     * @param server   the parent http server
     */
    public BasicAuthenticationHandler(HttpHandler handler, AbstractGenericHttpServer<?> server)
        {
        f_handler = handler;
        f_server  = server;
        }

    // ----- HttpHandler methods --------------------------------------------

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
        String  sAuth   = exchange.getRequestHeaders().getFirst(AbstractGenericHttpServer.HEADER_AUTHORIZATION);
        Subject subject = f_server.authenticate(sAuth);
        if (subject == null)
            {
            exchange.getResponseHeaders().set(AbstractGenericHttpServer.HEADER_WWW_AUTHENTICATE,
                                              AbstractGenericHttpServer.DEFAULT_BASIC_AUTH_HEADER_VALUE);
            exchange.sendResponseHeaders(401, -1);
            }
        else
            {
            exchange.setAttribute(AbstractGenericHttpServer.ATTR_SUBJECT, subject);
            f_handler.handle(exchange);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Request handler to delegate to when authentication is successful.
     */
    private final HttpHandler f_handler;

    /**
     * The parent http server.
     */
    private final AbstractGenericHttpServer<?> f_server;
    }
