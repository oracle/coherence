/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.tangosol.util.Base;

/**
 * Http Handler to update HttpServer metrics.
 *
 * @author tam 2022.04.29
 * @since 22.06
 */
class BasicStatisticsHandler
        implements HttpHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct BasicStatisticsHandler instance.
     *
     * @param handler  request handler to delegate to before recording statistics
     * @param server   the parent http server
     */
    public BasicStatisticsHandler(HttpHandler handler, AbstractGenericHttpServer<?> server)
        {
        f_handler = handler;
        f_server  = server;
        }

    // ----- HttpHandler methods --------------------------------------------

    /**
     * Record statistics for the handled request.
     *
     * @param exchange  HTTP exchange for the request
     *
     * @throws IOException  if an error occurs
     */
    public void handle(final HttpExchange exchange)
            throws IOException
        {
        long ldtStart = Base.getLastSafeTimeMillis();

        // handle the original request
        f_handler.handle(exchange);

        int nResponseCode = exchange.getResponseCode();

        f_server.logRequestTime(ldtStart);
        f_server.incrementRequestCount();
        f_server.logStatusCount(nResponseCode);

        if (nResponseCode == 500)
            {
            f_server.incrementErrors();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Request handler to wrap and update statistics.
     */
    private final HttpHandler f_handler;

    /**
     * The parent http server.
     */
    private final AbstractGenericHttpServer<?> f_server;
    }
