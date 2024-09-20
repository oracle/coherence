/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import com.google.protobuf.Message;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;

import com.oracle.coherence.grpc.client.common.v0.GrpcConnectionV0;
import com.oracle.coherence.grpc.client.common.v1.GrpcConnectionV1;
import com.oracle.coherence.grpc.internal.GrpcTracingInterceptors;

import com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies;

import com.tangosol.internal.tracing.LegacyXmlTracingHelper;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.internal.tracing.TracingShim;

import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.CacheFactory;
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

import java.util.Objects;
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

    /**
     * Set the {@link ClientInterceptor} to use for tracing.
     *
     * @param interceptor the {@link ClientInterceptor} to use for tracing
     */
    public void setTracingInterceptor(ClientInterceptor interceptor)
        {
        m_tracingInterceptor = interceptor;
        }

    /**
     * Return the scope name to use on the server.
     *
     * @return the scope name to use on the server
     */
    public String getScopeName()
        {
        return m_sScopeName;
        }

    @SuppressWarnings("unused")
    public void setScopeName(String sScopeName)
        {
        m_sScopeName = sScopeName;
        }

    /**
     * Create a {@link GrpcConnection}.
     *
     * @param sProtocol    the name of the requested protocol
     * @param nVersion     the requested protocol version
     * @param nVersionMin  the minimum supported protocol version
     *
     * @return a {@link GrpcConnection} corresponding to the version supported
     *         by the gRPC proxy service on the server
     */
    protected GrpcConnection connect(String sProtocol, int nVersion, int nVersionMin)
        {
        GrpcConnection.Dependencies deps = new GrpcConnection.DefaultDependencies(sProtocol, m_dependencies, m_channel,
                nVersion, nVersionMin, m_serializer);
        return GrpcRemoteService.connect(deps, getResponseType());
        }

    /**
     * Create a {@link GrpcConnection}.
     *
     * @param dependencies  the connection dependencies
     * @param responseType  the type of the expected response message
     *
     * @return a {@link GrpcConnection} corresponding to the version supported
     *         by the gRPC proxy service on the server
     *
     * @throws NullPointerException if the expected response type is {@code null}
     */
    public static GrpcConnection connect(GrpcConnection.Dependencies dependencies,
            Class<? extends Message> responseType)
        {
        try
            {
            Class<? extends Message> type       = Objects.requireNonNull(responseType);
            GrpcConnection           connection = new GrpcConnectionV1(dependencies, type);
            connection.connect();
            return connection;
            }
        catch (Exception e)
            {
            Logger.finer("Could not instantiate V1 gRPC connector. " + e.getMessage());
            // fall back to the version zero client
            return new GrpcConnectionV0(dependencies.getChannel());
            }
        }

    /**
     * Return the expected response message type.
     * <p/>
     * This method must not return {@code null}
     *
     * @return the expected response message type
     */
    protected abstract Class<? extends Message> getResponseType();

    // ----- Service methods ------------------------------------------------

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

            m_executor = instantiateExecutor();
            m_executor.start();

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
                    m_executor.stop();
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

    /**
     * Return the {@link EventDispatcherRegistry} to use to register interceptors.
     *
     * @return the {@link EventDispatcherRegistry}
     */
    protected EventDispatcherRegistry getEventDispatcherRegistry() {
        EventDispatcherRegistry reg = m_EventDispatcherRegistry;
        if (reg == null)
            {
            setEventDispatcherRegistry(reg = getDefaultEventDispatcherRegistry());
            }
        return reg;
    }

    /**
     * Set the {@link EventDispatcherRegistry}.
     *
     * @param registryInterceptor the {@link EventDispatcherRegistry}
     */
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

    /**
     * Instantiate the gRPC {@link Channel}.
     *
     * @return  the gRPC {@link Channel}
     */
    protected Channel instantiateChannel()
        {
        D                         deps        = getDependencies();
        GrpcChannelDependencies   depsChannel = deps.getChannelDependencies();
        Optional<ChannelProvider> optional    = depsChannel.getChannelProvider();

        return optional.flatMap(p -> p.getChannel(m_sServiceName))
                .orElse(GrpcChannelFactory.singleton().getChannel(this));
        }

    /**
     * Create and return the {@link ClientInterceptor} for tracing.
     *
     * @return Create and return the {@link ClientInterceptor} for tracing, or
     *         {@code null} if tracing is disabled, or the dependent tracing
     *         libraries aren't available on the classpath.
     */
    protected ClientInterceptor instantiateTracingInterceptor()
        {
        if (!TracingHelper.isEnabled())
            {
            XmlElement xmlTracing = CacheFactory.getServiceConfig("$Tracing");
            if (xmlTracing != null)
                {
                TracingShim.Dependencies depsTracing =
                        LegacyXmlTracingHelper.fromXml(xmlTracing, TracingHelper.defaultDependencies());
                TracingHelper.initialize(depsTracing);
                }
            }

        return TracingHelper.isEnabled() ? GrpcTracingInterceptors.getClientInterceptor() : null;
        }

    /**
     * Instantiate the {@link SimpleDaemonPoolExecutor} executor.
     *
     * @return  the executor to use to execute tasks
     */
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

    /**
     * The registered member listeners.
     */
    private final Listeners f_memberListeners = new Listeners();

    /**
     * The registered service listeners.
     */
    private final Listeners f_serviceListeners = new Listeners();

    /**
     * The service's {@link ResourceRegistry}.
     */
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
