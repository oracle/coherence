/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.Requests;

import com.tangosol.application.Context;
import com.tangosol.application.LifecycleListener;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.NameService;
import com.tangosol.net.events.CoherenceLifecycleEvent;

import com.tangosol.util.HealthCheck;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessServerBuilder;

import javax.naming.NamingException;

import java.io.IOException;

import java.net.InetAddress;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

/**
 * A controller class that starts and stops the default gRPC server
 * by responding to {@link com.tangosol.net.DefaultCacheServer}
 * lifecycle events.
 *
 * @author Jonathan Knight  2020.09.24
 */
public class GrpcServerController
        implements HealthCheck, NameService.Resolvable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor as this is a singleton.
     */
    private GrpcServerController()
        {
        }

    // ----- GrpcServerController methods -----------------------------------

    /**
     * Start the gRPC server.
     * <p>
     * If the server is already running this method is a no-op.
     */
    public synchronized void start()
        {
        if (isRunning() || !m_fEnabled)
            {
            return;
            }

        try
            {
            m_inProcessName = Config.getProperty(Requests.PROP_IN_PROCESS_NAME, Requests.DEFAULT_CHANNEL_NAME);

            int                       port     = Config.getInteger(Requests.PROP_PORT, Requests.DEFAULT_PORT);
            GrpcServerBuilderProvider provider =
                    StreamSupport.stream(ServiceLoader.load(GrpcServerBuilderProvider.class).spliterator(), false)
                                .sorted()
                                .findFirst()
                                .orElse(GrpcServerBuilderProvider.INSTANCE);

            ServerBuilder<?>       serverBuilder = provider.getServerBuilder(port);
            InProcessServerBuilder inProcBuilder = provider.getInProcessServerBuilder(m_inProcessName);

            if (serverBuilder == null)
                {
                serverBuilder = GrpcServerBuilderProvider.INSTANCE.getServerBuilder(port);
                }

            if (inProcBuilder == null)
                {
                inProcBuilder = GrpcServerBuilderProvider.INSTANCE.getInProcessServerBuilder(m_inProcessName);
                }

            for (BindableGrpcProxyService service : createGrpcServices())
                {
                GrpcMetricsInterceptor  interceptor = new GrpcMetricsInterceptor(service.getMetrics());
                ServerServiceDefinition definition  = ServerInterceptors.intercept(service, interceptor);
                serverBuilder.addService(definition);
                inProcBuilder.addService(definition);
                }

            configure(serverBuilder, inProcBuilder);

            Server server          = serverBuilder.build();
            Server inProcessServer = inProcBuilder.build();

            server.start();
            Logger.info(() -> "Coherence gRPC proxy is now listening for connections on 0.0.0.0:" + port);
            inProcessServer.start();
            Logger.info(() -> "Coherence gRPC in-process proxy '"
                    + m_inProcessName + "' is now listening for connections");

            m_server          = server;
            m_inProcessServer = inProcessServer;
            markStarted();
            }
        catch (IOException e)
            {
            if (!m_startFuture.isDone())
                {
                m_startFuture.completeExceptionally(e);
                }
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Stop the server.
     * <p>
     * If the server is not running this method is a no-op.
     */
    public synchronized void stop()
        {
        if (isRunning())
            {
            stopServer(m_server, "server");
            stopServer(m_inProcessServer, "in-process server");
            m_inProcessServer = null;
            m_server = null;
            m_startFuture = new CompletableFuture<>();
            }
        }

    /**
     * Mark the server as started.
     * <p>
     * This will complete the start-up {@link CompletionStage}
     * if not already completed.
     */
    public void markStarted()
        {
        if (!m_startFuture.isDone())
            {
            m_startFuture.complete(null);
            }
        }

    /**
     * Obtain a {@link CompletionStage} that will be completed when
     * the gRPC server has started.
     *
     * @return a {@link CompletionStage} that will be completed when
     *         the gRPC server has started
     */
    public CompletionStage<Void> whenStarted()
        {
        return m_startFuture;
        }

    /**
     * Returns {@code true} if the server is running.
     *
     * @return {@code true} if the server is running
     */
    public boolean isRunning()
        {
        return m_server != null && !m_server.isShutdown();
        }

    /**
     * Returns the port that the gRPC server has bound to.
     *
     * @return the port that the gRPC server has bound to
     *
     * @throws IllegalStateException if the server is not running
     */
    public int getPort()
        {
        if (isRunning())
            {
            return m_server.getPort();
            }
        throw new IllegalStateException("The gRPC server is not running");
        }

    /**
     * Returns the name of the in-process gRPC server.
     *
     * @return the name of the in-process gRPC server
     *
     * @throws IllegalStateException if the server is not running
     */
    public String getInProcessName()
        {
        if (isRunning())
            {
            return m_inProcessName;
            }
        throw new IllegalStateException("The gRPC server is not running");
        }

    /**
     * Obtain the list of gRPC proxy services to bind to a gRPC server.
     *
     * @return  the list of gRPC proxy services to bind to a gRPC server
     */
    public List<BindableGrpcProxyService> createGrpcServices()
        {
        return Collections.singletonList(new NamedCacheServiceGrpcImpl());
        }

    /**
     * Enable or disable this controller.
     * <p>
     * If disabled then the gRPC proxy will not be started.
     * this method can be used in applications where the
     * gRPC services are being deployed manually or by some
     * other mechanism such as CDI.
     *
     * @param fEnabled {@code false} to disable the controller.
     */
    public void setEnabled(boolean fEnabled)
        {
        m_fEnabled = fEnabled;
        }

    /**
     * Set the flag indicating whether this server's health check forms
     * part of the local member's overall health check.
     *
     * @param fIsMemberHealth  {@code true} if this server's health check forms
     *                         part of the local member's overall health check
     */
    public void setIsMemberHealth(boolean fIsMemberHealth)
        {
        m_fIsMemberHealth = fIsMemberHealth;
        }

    // ----- NameService.Resolvable API -------------------------------------

    @Override
    public Object resolve(NameService.RequestContext ctx)
        {
        if (m_cluster != null)
            {
            InetAddress address  = ctx.getMember().getAddress();
            String      sAddress = null;

            if (InetAddressHelper.isLocalAddress(address))
                {
                sAddress = address.getHostAddress();
                }
            else
                {
                Collection<InetAddress> colAddress = InetAddressHelper.getRoutableAddresses(null, false, Collections.singletonList(address), false);
                if (colAddress != null)
                    {
                    sAddress = colAddress.stream()
                            .map(InetAddress::getHostAddress)
                            .findFirst()
                            .orElse(null);

                    }
                }
            return new Object[]{sAddress, m_server.getPort()};
            }
        return null;
        }

    // ----- HealthCheck API ------------------------------------------------

    @Override
    public String getName()
        {
        return "GrpcServer";
        }

    @Override
    public boolean isReady()
        {
        return isRunning();
        }

    @Override
    public boolean isLive()
        {
        return isRunning();
        }

    @Override
    public boolean isStarted()
        {
        return isRunning();
        }

    @Override
    public boolean isSafe()
        {
        return isRunning();
        }

    @Override
    public boolean isMemberHealthCheck()
        {
        return m_fIsMemberHealth;
        }

    // ----- helper methods -------------------------------------------------

    private synchronized void ensureRegistered(Cluster cluster)
        {
        if (cluster != null && m_cluster == null)
            {
            cluster.getManagement().register(this);

            m_fIsMemberHealth = Config.getBoolean(PROP_HEALTH_ENABLED, true);
            
            NameService nameService = cluster.getResourceRegistry().getResource(NameService.class);
            if (nameService != null)
                {
                try
                    {
                    nameService.bind(NAME_SERVICE_NAME, this);
                    }
                catch (NamingException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                }

            m_cluster = cluster;
            }
        }

    private void configure(ServerBuilder<?> serverBuilder, InProcessServerBuilder inProcessServerBuilder)
        {
        ServiceLoader<GrpcServerConfiguration> loader = ServiceLoader.load(GrpcServerConfiguration.class);
        for (GrpcServerConfiguration cfg : loader)
            {
            try
                {
                cfg.configure(serverBuilder, inProcessServerBuilder);
                }
            catch (Throwable t)
                {
                Logger.err("Caught exception calling GrpcServerConfiguration " + cfg);
                Logger.err(t);
                }
            }
        }

    private void stopServer(Server server, String sName)
        {
        boolean fStopped = false;
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

    // ----- inner class: Listener ------------------------------------------

    /**
     * A listener that will start the gRPC server base on {@link Coherence} or
     * {@link com.tangosol.net.DefaultCacheServer} lifecycle events.
     */
    public static class Listener
            implements LifecycleListener, Coherence.LifecycleListener
        {
        // ----- Coherence.LifecycleListener methods ------------------------

        @Override
        public void onEvent(CoherenceLifecycleEvent event)
            {
            switch (event.getType())
                {
                case STARTED:
                    if (Config.getBoolean(PROP_ENABLED, true))
                        {
                        Coherence coherence = event.getCoherence();
                        Cluster   cluster   = coherence == null ? null : coherence.getCluster();
                        INSTANCE.ensureRegistered(cluster);
                        INSTANCE.start();
                        }
                    break;
                case STOPPED:
                    if (Coherence.getInstances().isEmpty())
                        {
                        INSTANCE.stop();
                        }
                    break;
                }
            }

        // ----- DCS LifecycleListener methods ------------------------------

        @Override
        public void preStart(Context ctx)
            {
            }

        @Override
        public void postStart(Context ctx)
            {
            if (Config.getBoolean(PROP_ENABLED, true))
                {
                INSTANCE.start();
                }
            }

        @Override
        public void preStop(Context ctx)
            {
            INSTANCE.stop();
            }

        @Override
        public void postStop(Context ctx)
            {
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of the {@link GrpcServerController}.
     */
    public static final GrpcServerController INSTANCE = new GrpcServerController();

    /**
     * The system property that enables or disables running the gRPC server.
     */
    public static final String PROP_ENABLED = "coherence.grpc.enabled";

    /**
     * The name used to bind to the name service.
     */
    public static final String NAME_SERVICE_NAME = "$SYS:GRPC";

    /**
     * The System property to determine whether this server's health check is part of the
     * local member's overall health.
     */
    public static final String PROP_HEALTH_ENABLED = "coherence.grpc.health.enabled";

    // ----- data members ---------------------------------------------------

    /**
     * The gRPC Server.
     */
    private Server m_server;

    /**
     * The gRPC In-Process Server.
     */
    private Server m_inProcessServer;

    /**
     * The name of the in-process server.
     */
    private String m_inProcessName;

    /**
     * A flag indicating whether this controller is enabled.
     */
    private boolean m_fEnabled = true;

    /**
     * A {@link CompletableFuture} that will be completed when the server has started.
     */
    private CompletableFuture<Void> m_startFuture = new CompletableFuture<>();

    /**
     * A flag that is {@code true} if this server is part of the local member's health check.
     */
    private boolean m_fIsMemberHealth = true;

    /**
     * The {@link Cluster} this server is running in.
     */
    private Cluster m_cluster;
    }
