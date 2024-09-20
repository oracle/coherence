/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.helidon;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.proxy.common.BaseGrpcAcceptorController;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;

import com.tangosol.application.Context;

import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;

import io.helidon.webserver.grpc.GrpcRouting;

import java.io.IOException;
import java.util.List;

/**
 * A {@link GrpcAcceptorController} that starts a Helidon gRPC server.
 */
public class HelidonGrpcAcceptorController
        extends BaseGrpcAcceptorController
    {
    @Override
    public int getPriority()
        {
        return PRIORITY_NORMAL + 1;
        }

    @Override
    public void setDaemonPool(DaemonPool pool)
        {
        }

    @Override
    protected GrpcServiceDependencies createServiceDeps()
        {
        GrpcAcceptorDependencies deps    = getDependencies();
        Context                  context = deps.getContext();

        GrpcServiceDependencies.DefaultDependencies serviceDeps
                = new GrpcServiceDependencies.DefaultDependencies(GrpcDependencies.ServerType.Synchronous);

        serviceDeps.setContext(context);
        serviceDeps.setExecutor(Runnable::run);
        return serviceDeps;
        }

    @Override
    protected void startInternal(List<ServerServiceDefinition> listServices, List<BindableService> listBindable) throws IOException
        {
        GrpcAcceptorDependencies dependencies   = getDependencies();
        WebServerConfig.Builder  serverBuilder  = WebServerConfig.builder();
        GrpcRouting.Builder      routingBuilder = GrpcRouting.builder();

        for (ServerServiceDefinition definition : listServices)
            {
            routingBuilder.service(definition).build();
            }

        listBindable.forEach(s -> routingBuilder.service(null, s));

        HelidonCredentialsHelper.createTlsConfig(dependencies.getSocketProviderBuilder())
                .ifPresent(serverBuilder::tls);

        m_server = serverBuilder
                .port(dependencies.getLocalPort())
                .addRouting(routingBuilder)
                .build()
                .start();
        }

    @Override
    protected void stopInternal()
        {
        stopServer(m_server);
        }

    @Override
    public int getLocalPort()
        {
        if (m_server == null || !m_server.isRunning())
            {
            throw new IllegalStateException("The gRPC server is not started");
            }
        return m_server.port();
        }

    @Override
    public String getInProcessName()
        {
        return null;
        }

    @Override
    public GrpcDependencies.ServerType getServerType()
        {
        return GrpcDependencies.ServerType.Synchronous;
        }

    // ----- helper methods -------------------------------------------------

    private void stopServer(WebServer server)
        {
        if (server == null)
            {
            return;
            }
        server.stop();
        Logger.fine("Stopped Coherence gRPC proxy");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The gPRC server.
     */
    private WebServer m_server;
    }
