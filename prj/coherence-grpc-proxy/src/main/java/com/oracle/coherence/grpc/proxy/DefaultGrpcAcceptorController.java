/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.CredentialsHelper;
import com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.net.grpc.GrpcAcceptorController;
import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCredentials;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessServerBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * The default {@link GrpcAcceptorController} implementation.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class DefaultGrpcAcceptorController
        implements GrpcAcceptorController
    {
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
        try
            {
            GrpcAcceptorDependencies deps             = getDependencies();
            ServerBuilder<?>         serverBuilder    = createServerBuilder(deps);
            InProcessServerBuilder   inProcessBuilder = createInProcessServerBuilder(deps);

            GrpcServiceDependencies.DefaultDependencies serviceDeps = new GrpcServiceDependencies.DefaultDependencies();
            if (m_daemonPool != null)
                {
                serviceDeps.setExecutor(new DaemonPoolExecutor(m_daemonPool));
                }

            for (BindableGrpcProxyService service : createGrpcServices(serviceDeps))
                {
                GrpcMetricsInterceptor  interceptor = new GrpcMetricsInterceptor(service.getMetrics());
                ServerServiceDefinition definition  = ServerInterceptors.intercept(service, interceptor);
                serverBuilder.addService(definition);
                inProcessBuilder.addService(definition);
                }

            configure(serverBuilder, inProcessBuilder);

            Server server          = serverBuilder.build();
            Server inProcessServer = inProcessBuilder.build();

            server.start();
            inProcessServer.start();

            Logger.info(() -> "In-Process GrpcAcceptor is now listening for connections using name \""
                    + deps.getInProcessName() + "\"");

            m_server = server;
            m_inProcessServer = inProcessServer;
            m_fRunning = true;
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e, "Failed to start gRPC server");
            }
        }

    @Override
    public void stop()
        {
        if (isRunning())
            {
            stopServer(m_inProcessServer, "in-process server");
            m_inProcessServer = null;
            stopServer(m_server, "server");
            m_server = null;
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
        if (m_server == null)
            {
            throw new IllegalStateException("The gRPC server is not started");
            }
        return m_server.getPort();
        }

    @Override
    public String getInProcessName()
        {
        return getDependencies().getInProcessName();
        }

    // ----- helper methods -------------------------------------------------

    protected ServerBuilder<?> createServerBuilder(GrpcAcceptorDependencies deps)
        {
        ServerCredentials credentials = CredentialsHelper.createServerCredentials(deps.getSocketProviderBuilder());
        return Grpc.newServerBuilderForPort(deps.getLocalPort(), credentials);
        }

    protected InProcessServerBuilder createInProcessServerBuilder(GrpcAcceptorDependencies deps)
        {
        return InProcessServerBuilder.forName(deps.getInProcessName());
        }

    /**
     * Obtain the list of gRPC proxy services to bind to a gRPC server.
     *
     * @return  the list of gRPC proxy services to bind to a gRPC server
     */
    public static List<BindableGrpcProxyService> createGrpcServices()
        {
        return createGrpcServices(null);
        }

    /**
     * Obtain the list of gRPC proxy services to bind to a gRPC server.
     *
     * @param deps  the {@link GrpcServiceDependencies} to use
     *
     * @return  the list of gRPC proxy services to bind to a gRPC server
     */
    public static List<BindableGrpcProxyService> createGrpcServices(GrpcServiceDependencies deps)
        {
        BindableGrpcProxyService cacheService
                = new NamedCacheServiceGrpcImpl(new NamedCacheServiceImpl.DefaultDependencies(deps));

        return Collections.singletonList(cacheService);
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
    }
