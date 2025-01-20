/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor;
import com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;

import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.net.messaging.ConnectionAcceptor;
import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

import io.grpc.health.v1.HealthCheckResponse;

import io.grpc.protobuf.services.ChannelzService;
import io.grpc.protobuf.services.HealthStatusManager;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A base implementation of a {@link GrpcAcceptorController}.
 */
public abstract class BaseGrpcAcceptorController
        implements GrpcAcceptorController
    {
    @Override
    public final void start()
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

            m_healthStatusManager = new HealthStatusManager();

            GrpcServiceDependencies.DefaultDependencies defaultDeps = new GrpcServiceDependencies.DefaultDependencies();
            defaultDeps.setAcceptor(m_acceptor);

            GrpcAcceptorDependencies      dependencies = getDependencies();
            GrpcServiceDependencies       serviceDeps  = createServiceDeps(defaultDeps);
            List<ServerServiceDefinition> listService  = ensureServices(serviceDeps);
            List<String>                  listName     = listService.stream().map(s -> s.getServiceDescriptor().getName()).toList();
            List<BindableService>         listBindable = new ArrayList<>();

            listBindable.add(ChannelzService.newInstance(dependencies.getChannelzPageSize()));
            listBindable.add(m_healthStatusManager.getHealthService());

            startInternal(listService, listBindable);

            m_healthStatusManager.setStatus(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME, HealthCheckResponse.ServingStatus.SERVING);
            listName.forEach(s -> m_healthStatusManager.setStatus(s, HealthCheckResponse.ServingStatus.SERVING));
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

    protected abstract GrpcServiceDependencies createServiceDeps(GrpcServiceDependencies defaultDeps);

    protected abstract void startInternal(List<ServerServiceDefinition> listService, List<BindableService> listBindable) throws IOException;

    @Override
    public final void stop()
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
                    List<BindableGrpcProxyService> list = m_listServices;
                    if (list != null)
                        {
                        for (BindableGrpcProxyService service : list)
                            {
                            try
                                {
                                service.close();
                                }
                            catch (Exception e)
                                {
                                Logger.err(e);
                                }
                            }
                        }
                    stopInternal();
                    m_listServices = null;
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        }

    protected abstract void stopInternal();

    @Override
    public boolean isRunning()
        {
        return m_fRunning;
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
    public String getLocalAddress()
        {
        return m_dependencies.getLocalAddress();
        }

    @Override
    public void setAcceptor(ConnectionAcceptor acceptor)
        {
        m_acceptor = (GrpcAcceptor) acceptor;
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

    protected List<ServerServiceDefinition> ensureServices(GrpcServiceDependencies serviceDeps)
        {
        List<BindableGrpcProxyService> list = loadServices(serviceDeps);
        return list.stream()
                .map(this::applyInterceptors)
                .toList();
        }

    private List<BindableGrpcProxyService> loadServices(GrpcServiceDependencies serviceDeps)
        {
        List<BindableGrpcProxyService> list = m_listServices;
        if (list == null)
            {
            f_lock.lock();
            try
                {
                list = m_listServices;
                if (list == null)
                    {
                    list = m_listServices = BindableServiceFactory.discoverServices(serviceDeps);
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return list;
        }

    /**
     * Return the {@link ProxyServiceGrpcImpl}.
     *
     * @return the {@link ProxyServiceGrpcImpl}
     */
    public ProxyServiceGrpcImpl getProxyService()
        {
        List<BindableGrpcProxyService> list = m_listServices;
        if (list == null)
            {
            throw new IllegalStateException("gRPC services not yet initialized");
            }
        return list.stream()
                .filter(s -> s instanceof ProxyServiceGrpcImpl)
                .map(ProxyServiceGrpcImpl.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ProxyServiceGrpcImpl not found"));
        }

    // ----- helper methods -------------------------------------------------

    protected ServerServiceDefinition applyInterceptors(BindableGrpcProxyService service)
        {
        GrpcMetricsInterceptor  metricsInterceptor = new GrpcMetricsInterceptor(service.getMetrics());
        ProxyServiceInterceptor proxyInterceptor   = new ProxyServiceInterceptor();
        return ServerInterceptors.intercept(service, metricsInterceptor, proxyInterceptor);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The dependencies to use to configure the server.
     */
    protected GrpcAcceptorDependencies m_dependencies;

    /**
     * The list of {@link BindableGrpcProxyService services} served by this controller.
     */
    private List<BindableGrpcProxyService> m_listServices;

    /**
     * The gRPC health check service manager.
     */
    private HealthStatusManager m_healthStatusManager;

    /**
     * Whether the server is running.
     */
    private volatile boolean m_fRunning;

    /**
     * The {@link GrpcAcceptor}.
     */
    private GrpcAcceptor m_acceptor;

    /**
     * The lock to synchronize starting and stopping the controller.
     */
    private final Lock f_lock = new ReentrantLock();
    }
