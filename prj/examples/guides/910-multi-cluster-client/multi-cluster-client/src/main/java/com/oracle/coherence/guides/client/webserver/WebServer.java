/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client.webserver;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.io.json.internal.GensonMapJsonBodyHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tangosol.internal.http.BaseHttpHandler;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.net.Coherence;
import com.tangosol.util.HealthCheck;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

public class WebServer
        extends BaseHttpHandler
        implements HealthCheck {
    public static final String HEALTH_CHECK_NAME = "webserver";

    private HttpServer server;

    private volatile boolean running;

    public WebServer(RequestRouter router) {
        super(router, new GensonMapJsonBodyHandler());
    }

    @Override
    protected void beforeRouting(HttpRequest request) {
    }

    public void start() {
        try {
            int port = Integer.getInteger("webserver.port", 8080);

            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", this);

            server.start();
            running = true;

            Logger.info("Multi-tenant web-server listening on " + server.getAddress());
        }
        catch (IOException e) {
            throw Exceptions.ensureRuntimeException(e, "Failed to start web-server");
        }
    }

    public void stop() {
        if (server != null) {
            running = false;
            server.stop(0);
        }
    }

    @Override
    public String getName() {
        return HEALTH_CHECK_NAME;
    }

    @Override
    public boolean isReady() {
        return running;
    }

    @Override
    public boolean isLive() {
        return running;
    }

    @Override
    public boolean isStarted() {
        return running;
    }

    @Override
    public boolean isSafe() {
        return running;
    }
}
