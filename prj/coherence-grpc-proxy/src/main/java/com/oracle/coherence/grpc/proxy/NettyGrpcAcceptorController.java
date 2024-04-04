/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.internal.GrpcTracingInterceptors;

import com.oracle.coherence.grpc.proxy.common.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.BindableServiceFactory;
import com.oracle.coherence.grpc.proxy.common.DaemonPoolExecutor;
import com.oracle.coherence.grpc.proxy.common.GrpcMetricsInterceptor;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;

import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;

import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.net.Coherence;

import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;

import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCredentials;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

import io.grpc.health.v1.HealthCheckResponse;

import io.grpc.inprocess.InProcessServerBuilder;

import io.grpc.protobuf.services.ChannelzService;
import io.grpc.protobuf.services.HealthStatusManager;

import java.io.IOException;

import java.net.SocketAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The default {@link GrpcAcceptorController} implementation.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class NettyGrpcAcceptorController
        implements GrpcAcceptorController
    {
    @Override
    public int getPriority()
        {
        return PRIORITY_NORMAL;
        }

    @Override
    public void setDependencies(GrpcAcceptorDependencies deps)
        {
        m_dependencies = deps;
        }

    @Override
    public GrpcAcceptorDependencies getDependencies()
        {
        GrpcAcceptorDependencies deps = m_dependencies;
        if (deps == null)
            {
            deps = m_dependencies = new DefaultGrpcAcceptorDependencies();
            }
        return deps;
        }

    @Override
    public void setDaemonPool(DaemonPool pool)
        {
        m_daemonPool = pool;
        }

    @Override
    public void start()
        {
        if (m_fRunning)
            {
            return;
            }

        f_lock.lock();
        try
            {
            if (m_fRunning)
                {
                return;
                }

            GrpcAcceptorDependencies deps             = getDependencies();
            ServerBuilder<?>         serverBuilder    = createServerBuilder(deps);
            InProcessServerBuilder   inProcessBuilder = createInProcessServerBuilder(deps);
            Context                  context          = deps.getContext();

            GrpcServiceDependencies.DefaultDependencies serviceDeps
                    = new GrpcServiceDependencies.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);

            serviceDeps.setContext(context);

            if (m_daemonPool != null)
                {
                serviceDeps.setExecutor(new DaemonPoolExecutor(m_daemonPool));
                }

            m_listServices = BindableServiceFactory.discoverServices(serviceDeps);
            List<String> listServiceNames = new ArrayList<>();
            for (BindableGrpcProxyService service : m_listServices)
                {
                GrpcMetricsInterceptor  interceptor = new GrpcMetricsInterceptor(service.getMetrics());
                ServerServiceDefinition definition  = ServerInterceptors.intercept(service, interceptor);
                serverBuilder.addService(definition);
                inProcessBuilder.addService(definition);
                listServiceNames.add(definition.getServiceDescriptor().getName());
                }

            m_healthStatusManager = new HealthStatusManager();
            serverBuilder.addService(m_healthStatusManager.getHealthService());
            serverBuilder.addService(ChannelzService.newInstance(deps.getChannelzPageSize()));
//            serverBuilder.intercept(new ServerLoggingInterceptor());

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
            m_healthStatusManager.setStatus(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME, HealthCheckResponse.ServingStatus.SERVING);
            listServiceNames.forEach(s -> m_healthStatusManager.setStatus(s, HealthCheckResponse.ServingStatus.SERVING));
            m_fRunning = true;
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e, "Failed to start gRPC server");
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    public void stop()
        {
        if (m_fRunning)
            {
            f_lock.lock();
            try
                {
                if (m_fRunning)
                    {
                    m_fRunning = false;
                    m_healthStatusManager.enterTerminalState();
                    m_healthStatusManager = null;
                    stopServer(m_inProcessServer, "in-process server");
                    m_inProcessServer = null;
                    stopServer(m_server, "server");
                    m_server = null;
                    m_listServices = null;
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        }

    @Override
    public boolean isRunning()
        {
        return m_fRunning;
        }

    @Override
    public String getLocalAddress()
        {
        return m_dependencies.getLocalAddress();
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

    /**
     * Return the list of services this controller is serving.
     *
     * @return the list of services this controller is serving
     */
    public List<BindableGrpcProxyService> getBindableServices()
        {
        return m_listServices;
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
        boolean fStopped = false;
        if (server == null)
            {
            return;
            }

        server.shutdown();
        Logger.finest("Awaiting termination of Coherence gRPC proxy " + sName);
        try
            {
            fStopped = server.awaitTermination(1, TimeUnit.MINUTES);
            }
        catch (InterruptedException ignored)
            {
            // ignored
            }
        if (!fStopped)
            {
            Logger.finest("Forcing termination of Coherence gRPC proxy " + sName);
            server.shutdownNow();
            }
        Logger.fine("Stopped Coherence gRPC proxy " + sName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The dependencies to use to configure the server.
     */
    private GrpcAcceptorDependencies m_dependencies;

    /**
     * Whether the server is running.
     */
    private volatile boolean m_fRunning;

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
     * The gRPC health check service manager.
     */
    private HealthStatusManager m_healthStatusManager;

    /**
     * The list of {@link BindableGrpcProxyService services} served by this controller.
     */
    private List<BindableGrpcProxyService> m_listServices;

    /**
     * The lock to control start and stop state.
     */
    private final Lock f_lock = new ReentrantLock();
    }
