/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;

import com.tangosol.config.expression.SystemPropertyParameterResolver;

import com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies;

import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.MemberListener;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceInfo;

import com.tangosol.net.events.EventDispatcherRegistry;

import com.tangosol.net.grpc.GrpcChannelDependencies;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Listeners;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ServiceListener;
import com.tangosol.util.SimpleResourceRegistry;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.util.GlobalTracer;

import java.util.Optional;
import java.util.Set;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A remote service that accesses caches via a remote gRPC proxy.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 23.03
 */
@SuppressWarnings("rawtypes")
public abstract class GrpcRemoteService<D extends RemoteGrpcServiceDependencies>
        implements Service, ServiceInfo
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link GrpcRemoteService}.
     *
     * @param sServiceType  the type of the service
     */
    public GrpcRemoteService(String sServiceType)
        {
        f_sServiceType = sServiceType;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Set the {@link Cluster}.
     * @param cluster the {@link Cluster}
     */
    public void setCluster(Cluster cluster)
        {
        m_cluster = cluster;
        }

    /**
     * Set the service name.
     *
     * @param sName  the service name
     */
    public void setServiceName(String sName)
        {
        m_sServiceName = sName;
        }

    /**
     * Returns the {@link Channel} to use to connect to the server.
     *
     * @return the {@link Channel} to use to connect to the server
     */
    public Channel getChannel()
        {
        return m_channel;
        }

    /**
     * Set the {@link Channel} to use to connect to the server.
     *
     * @param channel  the {@link Channel} to use to connect to the server
     */
    public void setChannel(Channel channel)
        {
        m_channel = channel;
        }

    public void setTracingInterceptor(ClientInterceptor tracingInterceptor)
        {
        m_tracingInterceptor = tracingInterceptor;
        }

    public SimpleDaemonPoolExecutor getExecutor()
        {
        return m_executor;
        }

    public void setExecutor(SimpleDaemonPoolExecutor executor)
        {
        m_executor = executor;
        }

    public String getScopeName()
        {
        return m_sScopeName;
        }

    public void setScopeName(String sScopeName)
        {
        m_sScopeName = sScopeName;
        }

    // ----- CacheService methods -------------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        return m_classLoader;
        }

    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        if (getContextClassLoader() != loader)
            {
            m_classLoader = loader;

            if (getSerializer() != null)
                {
                // re-initialize the service Serializer
                setSerializer(instantiateSerializer(loader));
                }
            }
        }

    @Override
    public Cluster getCluster()
        {
        return m_cluster;
        }

    @Override
    public ServiceInfo getInfo()
        {
        return this;
        }

    @Override
    public void addMemberListener(MemberListener listener)
        {
        f_memberListeners.add(listener);
        }

    @Override
    public void removeMemberListener(MemberListener listener)
        {
        f_memberListeners.remove(listener);
        }

    @Override
    public Object getUserContext()
        {
        return m_oUserContext;
        }

    @Override
    public void setUserContext(Object oCtx)
        {
        m_oUserContext = oCtx;
        }

    @Override
    public Serializer getSerializer()
        {
        return m_serializer;
        }

    protected void setSerializer(Serializer serializer)
        {
        m_serializer = serializer;
        }

    @Override
    @SuppressWarnings("unchecked")
    public void setDependencies(ServiceDependencies deps)
        {
        m_dependencies = (D) deps;
        }

    @Override
    public D getDependencies()
        {
        return m_dependencies;
        }

    @Override
    public ResourceRegistry getResourceRegistry()
        {
        return f_resourceRegistry;
        }

    @Override
    public boolean isSuspended()
        {
        return false;
        }

    @Override
    public void configure(XmlElement xml)
        {
        }

    @Override
    public void start()
        {
        f_lock.lock();
        try
            {
            setChannel(instantiateChannel());
            setSerializer(instantiateSerializer(m_classLoader));
            setTracingInterceptor(instantiateTracingInterceptor());

            SimpleDaemonPoolExecutor executor = instantiateExecutor();
            setExecutor(executor);
            executor.start();

            m_fRunning = true;
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    public boolean isRunning()
        {
        return m_fRunning;
        }

    @Override
    public void shutdown()
        {
        stop();
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
                    stopInternal();
                    if (m_channel instanceof ManagedChannel)
                        {
                        try
                            {
                            ManagedChannel managedChannel = (ManagedChannel) m_channel;
                            managedChannel.shutdownNow();
                            managedChannel.awaitTermination(1, TimeUnit.MINUTES);
                            }
                        catch (InterruptedException e)
                            {
                            Logger.err(e);
                            }
                        }
                    SimpleDaemonPoolExecutor executor = getExecutor();
                    executor.stop();
                    m_fRunning = false;
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        }

    @Override
    public void addServiceListener(ServiceListener listener)
        {
        f_serviceListeners.add(listener);
        }

    @Override
    public void removeServiceListener(ServiceListener listener)
        {
        f_serviceListeners.remove(listener);
        }

    // ----- ServiceInfo methods --------------------------------------------

    @Override
    public String getServiceName()
        {
        return m_sServiceName;
        }

    @Override
    public String getServiceType()
        {
        return f_sServiceType;
        }

    @Override
    public Set getServiceMembers()
        {
        return NullImplementation.getSet();
        }

    @Override
    public String getServiceVersion(Member member)
        {
        return "1";
        }

    @Override
    public Member getOldestMember()
        {
        return null;
        }

    @Override
    public Member getServiceMember(int nId)
        {
        return null;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Perform any stop tasks required by subclasses.
     */
    protected abstract void stopInternal();

    protected EventDispatcherRegistry getEventDispatcherRegistry() {
        EventDispatcherRegistry reg = m_EventDispatcherRegistry;
        if (reg == null)
            {
            setEventDispatcherRegistry(reg = getDefaultEventDispatcherRegistry());
            }
        return reg;
    }

    protected void setEventDispatcherRegistry(EventDispatcherRegistry registryInterceptor) {
        m_EventDispatcherRegistry = registryInterceptor;
    }

    /**
     * Return the default {@link EventDispatcherRegistry} to use.
     *
     * @return the default {@link EventDispatcherRegistry} to use
     */
    protected abstract EventDispatcherRegistry getDefaultEventDispatcherRegistry();

    /**
     * Return the {@link ClientInterceptor} to use for tracing.
     *
     * @return the {@link ClientInterceptor} to use for tracing
     */
    private ClientInterceptor createTracingInterceptor()
        {
        Tracer tracer = GlobalTracer.get();
        return TracingClientInterceptor.newBuilder()
                .withTracer(tracer)
                .build();
        }

    /**
     * Instantiate a Serializer and optionally configure it with the specified
     * ClassLoader.
     * 
     * @return the serializer
     */
    protected Serializer instantiateSerializer(ClassLoader loader)
        {
        SerializerFactory factory = m_dependencies.getSerializerFactory();
        return factory == null
                ? ExternalizableHelper.ensureSerializer(loader)
                : factory.createSerializer(loader);
        }

    protected Channel instantiateChannel()
        {
        D                         deps        = getDependencies();
        GrpcChannelDependencies   depsChannel = deps.getChannelDependencies();
        Optional<ChannelProvider> optional    = depsChannel.getChannelProvider();

        return optional.flatMap(p -> p.getChannel(m_sServiceName))
                .orElse(GrpcChannelFactory.singleton().getChannel(this));
        }

    protected ClientInterceptor instantiateTracingInterceptor()
        {
        boolean fTracing = m_dependencies.isTracingEnabled()
                .evaluate(new SystemPropertyParameterResolver());

        return fTracing ? createTracingInterceptor() : null;
        }

    protected SimpleDaemonPoolExecutor instantiateExecutor()
        {
        String                        sPoolName    = getServiceName() + "-pool-" + f_cPool.getAndIncrement();
        DefaultDaemonPoolDependencies dependencies = new DefaultDaemonPoolDependencies(m_dependencies.getDaemonPoolDependencies());

        dependencies.setName(sPoolName);
        dependencies.setThreadCount(Math.max(1, dependencies.getThreadCount()));

        return new SimpleDaemonPoolExecutor(dependencies);
        }

    // ----- constants ------------------------------------------------------

    /**
     * A counter to use for the daemon pool name suffix.
     */
    private static final AtomicInteger f_cPool = new AtomicInteger();

    // ----- data members ---------------------------------------------------

    /**
     * The type of this service;
     */
    private final String f_sServiceType;

    private final Listeners f_memberListeners = new Listeners();

    private final Listeners f_serviceListeners = new Listeners();

    private final ResourceRegistry f_resourceRegistry = new SimpleResourceRegistry();

    /**
     * The cluster that this service runs in.
     */
    private Cluster m_cluster;

    /**
     * This service's {@link ClassLoader}.
     */
    private ClassLoader m_classLoader;

    /**
     * The user context.
     */
    private Object m_oUserContext;

    /**
     * The name of the service.
     */
    private String m_sServiceName;

    /**
     * A flag indicating whether the service is running.
     */
    private volatile boolean m_fRunning;

    /**
     * The service dependencies.
     */
    protected D m_dependencies;

    /**
     * The scope of the service.
     */
    protected String m_sScopeName;

    /**
     * The gRPC {@link Channel} used by this session.
     */
    protected Channel m_channel;

    /**
     * The {@link Serializer} used by this session.
     */
    protected Serializer m_serializer;

    /**
     * The optional client tracer to use.
     */
    protected ClientInterceptor m_tracingInterceptor;

    /**
     * The {@link Executor} to use to dispatch events.
     */
    protected SimpleDaemonPoolExecutor m_executor;

    /**
     * The {@link EventDispatcherRegistry}.
     */
    protected EventDispatcherRegistry m_EventDispatcherRegistry;

    /**
     * The lock to synchronize access to internal state
     */
    protected final Lock f_lock = new ReentrantLock();
    }
