/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Objects;
import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;
import com.tangosol.application.LifecycleListener;
import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.net.Coherence;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ProxyService;
import com.tangosol.net.ServiceMonitor;
import com.tangosol.net.SimpleServiceMonitor;
import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.ResourceRegistry;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A controller class that starts and stops the default gRPC server
 * by responding to {@link DefaultCacheServer}
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
        this(null);
        }

    /**
     * Private constructor for a domain specific server.
     *
     * @param context  an optional {@link Context} for the server
     */
    private GrpcServerController(Context context)
        {
        f_context        = context;
        f_serviceMonitor = new SimpleServiceMonitor();
        }

    // ----- GrpcServerController methods -----------------------------------

    /**
     * Start the gRPC server.
     * <p>
     * If the server is already running this method is a no-op.
     */
    public void start()
        {
        if (isRunning() || !m_fEnabled)
            {
            return;
            }

        f_startLock.lock();
        try
            {
            if (isRunning() || !m_fEnabled)
                {
                return;
                }

            ContainerContext containerContext = f_context == null ? null : f_context.getContainerContext();
            String           sAppName         = f_context == null ? null : f_context.getApplicationName();

            Runnable r = () ->
                {
                ClassLoader loader    = Classes.getContextClassLoader();
                XmlElement  xmlConfig = XmlHelper.loadFileOrResourceOrDefault(GrpcDependencies.GRPC_PROXY_CACHE_CONFIG,
                        "gRPC Proxy Cache Configuration", loader);

                String sCcfScope;
                if (sAppName != null && !Coherence.SYSTEM_SCOPE.equals(sAppName))
                    {
                    sCcfScope = sAppName + GrpcDependencies.PROXY_SERVICE_SCOPE_NAME;
                    }
                else
                    {
                    sCcfScope = GrpcDependencies.PROXY_SERVICE_SCOPE_NAME;
                    }

                ExtensibleConfigurableCacheFactory.Dependencies deps
                        = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(
                                xmlConfig, loader, null, sCcfScope, f_context);

                m_ccf = new ExtensibleConfigurableCacheFactory(deps);
                m_ccf.activate();

                ServiceMonitor monitor = f_serviceMonitor;
                monitor.setConfigurableCacheFactory(m_ccf);
                monitor.registerServices(m_ccf.getServiceMap());
                if (monitor.isMonitoring())
                    {
                    String sScope = ServiceScheme.getScopePrefix(sAppName, containerContext);
                    String sName  = ServiceScheme.getScopedServiceName(sScope, monitor.getThread().getName());
                    monitor.getThread().setName(sName);
                    }
                };

            if (containerContext == null)
                {
                r.run();
                }
            else
                {
                containerContext.runInDomainPartitionContext(r);
                }

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
        finally
            {
            f_startLock.unlock();
            }
        }

    /**
     * Stop the server.
     * <p>
     * If the server is not running this method is a no-op.
     */
    public void stop()
        {
        if (!isRunning())
            {
            return;
            }

        f_startLock.lock();
        try
            {
            if (isRunning())
                {
                if (f_serviceMonitor.isMonitoring())
                    {
                    f_serviceMonitor.stopMonitoring();
                    f_serviceMonitor.unregisterServices(m_ccf.getServiceMap().keySet());
                    m_ccf.dispose();
                    m_ccf = null;
                    }
                m_startFuture = new CompletableFuture<>();
                }
            }
        finally
            {
            f_startLock.unlock();
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
        return f_serviceMonitor.isMonitoring();
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
            return controller.getInProcessName();
            }
        throw new IllegalStateException("The gRPC server is not running");
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
     * {@link DefaultCacheServer} lifecycle events.
     */
    public static class Listener
            implements LifecycleListener
        {
        // ----- DCS LifecycleListener methods ------------------------------

        @Override
        public void preStart(Context ctx)
            {
            }

        @Override
        public void postStart(Context ctx)
            {
            if (Config.getBoolean(GrpcDependencies.PROP_ENABLED, true))
                {
                f_lock.lock();
                try
                    {
                    ContainerContext     containerContext = ctx.getContainerContext();
                    String               sScopePrefix     = ServiceScheme.getScopePrefix(ctx.getApplicationName(), containerContext);
                    GrpcServerController controller       = f_mapDomainController
                            .computeIfAbsent(sScopePrefix, k ->
                                    Coherence.DEFAULT_SCOPE.equals(sScopePrefix) && Coherence.DEFAULT_NAME.equals(ctx.getApplicationName())
                                        ? INSTANCE : new GrpcServerController(ctx));
                    controller.start();
                    }
                finally
                    {
                    f_lock.unlock();
                    }
                }
            }

        @Override
        public void preStop(Context ctx)
            {
            f_lock.lock();
            try
                {
                ContainerContext     containerContext = ctx.getContainerContext();
                String               sScopePrefix     = ServiceScheme.getScopePrefix(ctx.getApplicationName(), containerContext);
                GrpcServerController controller       = f_mapDomainController.get(sScopePrefix);
                if (controller != null && Objects.equals(ctx, controller.f_context))
                    {
                    f_mapDomainController.remove(sScopePrefix);
                    controller.stop();
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }

        @Override
        public void postStop(Context ctx)
            {
            }

        // ----- data members ---------------------------------------------------

        private static final Map<String, GrpcServerController> f_mapDomainController = new ConcurrentHashMap<>();

        private static final Lock f_lock = new ReentrantLock();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of the {@link GrpcServerController}.
     */
    public static final GrpcServerController INSTANCE = new GrpcServerController();

    // ----- data members ---------------------------------------------------

    private ExtensibleConfigurableCacheFactory m_ccf;

    /**
     * The service monitor to monitor the proxy.
     */
    private final ServiceMonitor f_serviceMonitor;

    /**
     * A flag indicating whether this controller is enabled.
     */
    private boolean m_fEnabled = true;

    /**
     * The optional {@link Context} for the server.
     */
    private final Context f_context;

    /**
     * A {@link CompletableFuture} that will be completed when the server has started.
     */
    private CompletableFuture<Void> m_startFuture = new CompletableFuture<>();

    /**
     * The lock to synchronize starting and stopping the service.
     */
    private final Lock f_startLock = new ReentrantLock();
    }
