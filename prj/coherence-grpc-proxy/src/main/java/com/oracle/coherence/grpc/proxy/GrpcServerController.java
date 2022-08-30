/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.application.Context;
import com.tangosol.application.LifecycleListener;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.Coherence;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ProxyService;

import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.ResourceRegistry;

import java.util.Collections;
import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A controller class that starts and stops the default gRPC server
 * by responding to {@link com.tangosol.net.DefaultCacheServer}
 * lifecycle events.
 *
 * @author Jonathan Knight  2020.09.24
 */
public class GrpcServerController
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
            ExtensibleConfigurableCacheFactory.Dependencies depsEccf
                    = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(GrpcDependencies.GRPC_PROXY_CACHE_CONFIG);

            m_ccf = new ExtensibleConfigurableCacheFactory(depsEccf);
            m_dcs = new DefaultCacheServer(m_ccf);

            m_dcs.startDaemon(DefaultCacheServer.DEFAULT_WAIT_MILLIS);
            markStarted();
            }
        catch (Throwable e)
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
            if (m_dcs != null)
                {
                m_dcs.stop();
                m_ccf = null;
                m_dcs = null;
                }
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
        return m_dcs != null && m_dcs.isMonitoringServices();
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
            ProxyService           proxyService = (ProxyService) m_ccf.ensureService(GrpcDependencies.PROXY_SERVICE_NAME);
            ResourceRegistry       registry     = proxyService.getResourceRegistry();
            GrpcAcceptorController controller   = registry.getResource(GrpcAcceptorController.class);
            return controller.getLocalPort();
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
            ProxyService           proxyService = (ProxyService) m_ccf.ensureService(GrpcDependencies.PROXY_SERVICE_NAME);
            ResourceRegistry       registry     = proxyService.getResourceRegistry();
            GrpcAcceptorController controller   = registry.getResource(GrpcAcceptorController.class);
            return controller.getLocalAddress();
            }
        throw new IllegalStateException("The gRPC server is not running");
        }

    /**
     * Obtain the list of gRPC proxy services to bind to a gRPC server.
     *
     * @return  the list of gRPC proxy services to bind to a gRPC server
     *
     * @deprecated use {@link DefaultGrpcAcceptorController#createGrpcServices()}
     */
    @Deprecated(since = "22.06.2")
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

    // ----- inner class: Listener ------------------------------------------

    /**
     * A listener that will start the gRPC server base on {@link Coherence} or
     * {@link com.tangosol.net.DefaultCacheServer} lifecycle events.
     */
    public static class Listener
            implements LifecycleListener
        {
        // ----- DCS LifecycleListener methods ------------------------------

        @Override
        public void preStart(Context ctx)
            {
            if (!m_fOwningInstance)
                {
                m_fOwningInstance = true;
                }
            }

        @Override
        public void postStart(Context ctx)
            {
            if (m_fOwningInstance && Config.getBoolean(GrpcDependencies.PROP_ENABLED, true))
                {
                INSTANCE.start();
                }
            }

        @Override
        public void preStop(Context ctx)
            {
            if (m_fOwningInstance && ctx.getConfigurableCacheFactory() != INSTANCE.m_ccf)
                {
                INSTANCE.stop();
                m_fOwningInstance = false;
                }
            }

        @Override
        public void postStop(Context ctx)
            {
            }

        // ----- data members ---------------------------------------------------

        private boolean m_fOwningInstance;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of the {@link GrpcServerController}.
     */
    public static final GrpcServerController INSTANCE = new GrpcServerController();

    // ----- data members ---------------------------------------------------

    private ExtensibleConfigurableCacheFactory m_ccf;

    private DefaultCacheServer m_dcs;

    /**
     * A flag indicating whether this controller is enabled.
     */
    private boolean m_fEnabled = true;

    /**
     * A {@link CompletableFuture} that will be completed when the server has started.
     */
    private CompletableFuture<Void> m_startFuture = new CompletableFuture<>();
    }
