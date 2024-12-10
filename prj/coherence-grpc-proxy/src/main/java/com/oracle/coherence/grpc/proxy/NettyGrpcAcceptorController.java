/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.internal.GrpcTracingInterceptors;
import com.oracle.coherence.grpc.proxy.common.BaseGrpcAcceptorController;
import com.oracle.coherence.grpc.proxy.common.DaemonPoolExecutor;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;
import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.net.Coherence;
import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;
import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCredentials;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The default {@link GrpcAcceptorController} implementation.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class NettyGrpcAcceptorController
        extends BaseGrpcAcceptorController
    {
    @Override
    public int getPriority()
        {
        return PRIORITY_NORMAL;
        }

    @Override
    public void setDaemonPool(DaemonPool pool)
        {
        m_daemonPool = pool;
        }

    @Override
    protected GrpcServiceDependencies createServiceDeps()
        {
        GrpcAcceptorDependencies deps    = getDependencies();
        Context                  context = deps.getContext();

        GrpcServiceDependencies.DefaultDependencies serviceDeps
                = new GrpcServiceDependencies.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);

        serviceDeps.setContext(context);

        if (m_daemonPool != null)
            {
            serviceDeps.setExecutor(new DaemonPoolExecutor(m_daemonPool));
            }

        return serviceDeps;
        }

    @Override
    protected void startInternal(List<ServerServiceDefinition> listServices, List<BindableService> listBindable) throws IOException
        {
        GrpcAcceptorDependencies deps             = getDependencies();
        ServerBuilder<?>         serverBuilder    = createServerBuilder(deps);
        InProcessServerBuilder   inProcessBuilder = createInProcessServerBuilder(deps);

        for (ServerServiceDefinition definition : listServices)
            {
            serverBuilder.addService(definition);
            inProcessBuilder.addService(definition);
            }

        listBindable.forEach(serverBuilder::addService);

        configure(serverBuilder, inProcessBuilder);

        ServerInterceptor grpcTracingInterceptor = GrpcTracingInterceptors.getServerInterceptor();
        if (grpcTracingInterceptor != null)
            {
            serverBuilder.intercept(grpcTracingInterceptor);
            }

        Server server          = serverBuilder.build();
        Server inProcessServer = inProcessBuilder.build();

        server.start();
        inProcessServer.start();

        for (SocketAddress address : inProcessServer.getListenSockets())
            {
            Logger.info(() -> "In-Process GrpcAcceptor is now listening for connections using name \""
                    + address + "\"");
            }

        m_server          = server;
        m_inProcessServer = inProcessServer;
        }

    @Override
    protected void stopInternal()
        {
        stopServer(m_inProcessServer, "in-process server");
        m_inProcessServer = null;
        stopServer(m_server, "server");
        m_server = null;
        }

    @Override
    public int getLocalPort()
        {
        Server server = m_server;
        if (server == null)
            {
            throw new IllegalStateException("The gRPC server is not started");
            }
        return server.getPort();
        }

    @Override
    public String getInProcessName()
        {
        Server server = m_inProcessServer;
        if (server != null)
            {
            return server.getListenSockets()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .findAny()
                    .orElse(null);
            }
        return null;
        }

    @Override
    public GrpcDependencies.ServerType getServerType()
        {
        return GrpcDependencies.ServerType.Asynchronous;
        }

    // ----- helper methods -------------------------------------------------

    protected ServerBuilder<?> createServerBuilder(GrpcAcceptorDependencies deps)
        {
        ServerCredentials credentials = NettyCredentialsHelper.createServerCredentials(deps.getSocketProviderBuilder());
        return Grpc.newServerBuilderForPort(deps.getLocalPort(), credentials);
        }

    protected InProcessServerBuilder createInProcessServerBuilder(GrpcAcceptorDependencies deps)
        {
        Context          ctx          = deps.getContext();
        ContainerContext ctxContainer = ctx == null ? null : ctx.getContainerContext();
        String           sPrefix      = ctx == null ? Coherence.DEFAULT_SCOPE : ctx.getDefaultScope();
        String           sScope       = ServiceScheme.getScopePrefix(sPrefix + GrpcDependencies.PROXY_SERVICE_SCOPE_NAME, ctxContainer);
        String           sName        = ServiceScheme.getScopedServiceName(sScope, deps.getInProcessName());
        return InProcessServerBuilder.forName(sName);
        }

    protected void configure(ServerBuilder<?> serverBuilder, InProcessServerBuilder inProcessServerBuilder)
        {
        for (GrpcServerConfiguration cfg : ServiceLoader.load(GrpcServerConfiguration.class))
            {
            try
                {
                cfg.configure(serverBuilder, inProcessServerBuilder);
                }
            catch (Throwable t)
                {
                Logger.err("Caught exception calling GrpcServerConfiguration " + cfg, t);
                }
            }
        }

    private void stopServer(Server server, String sName)
        {
        if (server == null)
            {
            return;
            }
        server.shutdownNow();
        Logger.fine("Stopped Coherence gRPC proxy " + sName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The gPRC server.
     */
    private Server m_server;

    /**
     * The in-process gPRC server.
     */
    private Server m_inProcessServer;

    /**
     * The {@link DaemonPool} the services may use.
     */
    private DaemonPool m_daemonPool;

    /**
     * The lock to control start and stop state.
     */
    private final Lock f_lock = new ReentrantLock();
    }
