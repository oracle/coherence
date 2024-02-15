/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.helidon;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.proxy.common.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.GrpcMetricsInterceptor;
import com.oracle.coherence.grpc.proxy.common.GrpcServerConfiguration;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;
import com.oracle.coherence.grpc.proxy.common.NamedCacheService;
import com.oracle.coherence.grpc.proxy.common.NamedCacheServiceGrpcImpl;
import com.tangosol.application.Context;
import com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.ChannelzService;
import io.grpc.protobuf.services.HealthStatusManager;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.grpc.GrpcRouting;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link GrpcAcceptorController} that starts a Helidon gRPC server.
 */
public class HelidonGrpcAcceptorController
        implements GrpcAcceptorController
    {
    @Override
    public int getPriority()
        {
        return PRIORITY_NORMAL + 1;
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
            GrpcAcceptorDependencies deps          = getDependencies();
            WebServerConfig.Builder  serverBuilder = WebServerConfig.builder();
            Context                  context       = deps.getContext();

            GrpcServiceDependencies.DefaultDependencies serviceDeps = new GrpcServiceDependencies.DefaultDependencies();

            serviceDeps.setContext(context);
            serviceDeps.setExecutor(Runnable::run);

            m_listServices = createGrpcServices(serviceDeps);

            List<String>        listServiceNames = new ArrayList<>();
            GrpcRouting.Builder routingBuilder   = GrpcRouting.builder();
            for (BindableGrpcProxyService service : m_listServices)
                {
                GrpcMetricsInterceptor  interceptor = new GrpcMetricsInterceptor(service.getMetrics());
                ServerServiceDefinition definition  = ServerInterceptors.intercept(service, interceptor);
                routingBuilder.service(definition).build();
                listServiceNames.add(definition.getServiceDescriptor().getName());
                }

            m_healthStatusManager = new HealthStatusManager();
            routingBuilder.service(null, m_healthStatusManager.getHealthService()).build();
            routingBuilder.service(null, ChannelzService.newInstance(deps.getChannelzPageSize())).build();

            HelidonCredentialsHelper.createTlsConfig(deps.getSocketProviderBuilder())
                    .ifPresent(serverBuilder::tls);

            WebServer server = serverBuilder
                    .port(deps.getLocalPort())
                    .addRouting(routingBuilder)
                    .build()
                    .start();

            m_server = server;
            m_healthStatusManager.setStatus(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME, HealthCheckResponse.ServingStatus.SERVING);
            listServiceNames.forEach(s -> m_healthStatusManager.setStatus(s, HealthCheckResponse.ServingStatus.SERVING));
            m_fRunning = true;
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

    /**
     * Return the list of services this controller is serving.
     *
     * @return the list of services this controller is serving
     */
    public List<BindableGrpcProxyService> getBindableServices()
        {
        return m_listServices;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the list of gRPC proxy services to bind to a gRPC server.
     *
     * @param depsService  the {@link GrpcServiceDependencies} to use
     *
     * @return  the list of gRPC proxy services to bind to a gRPC server
     */
    public static List<BindableGrpcProxyService> createGrpcServices(GrpcServiceDependencies depsService)
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(depsService);
        deps.setExecutor(Runnable::run);

        BindableGrpcProxyService cacheService = new NamedCacheServiceGrpcImpl(HelidonNamedCacheService.newInstance(deps));
//        BindableGrpcProxyService topicService
//                = new RemoteTopicServiceGrpcImpl(new RemoteTopicService.DefaultDependencies(deps), true);
//
//        return List.of(cacheService, topicService);
        return List.of(cacheService);
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

    private void stopServer(WebServer server, String sName)
        {
        if (server == null)
            {
            return;
            }
        server.stop();
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
    private WebServer m_server;

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
