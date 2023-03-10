/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;

import com.tangosol.config.expression.SystemPropertyParameterResolver;

import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.internal.net.grpc.RemoteGrpcCacheServiceDependencies;

import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.ServiceInfo;

import com.tangosol.net.events.EventDispatcherRegistry;

import com.tangosol.net.grpc.GrpcChannelDependencies;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.IteratorEnumerator;
import com.tangosol.util.Listeners;
import com.tangosol.util.MapEvent;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ServiceListener;
import com.tangosol.util.SimpleResourceRegistry;

import io.grpc.Channel;

import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;

import io.opentracing.Tracer;

import io.opentracing.contrib.grpc.TracingClientInterceptor;

import io.opentracing.util.GlobalTracer;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.Executor;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Collectors;

/**
 * A remote cache service that accesses caches via a remote gRPC proxy.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
@SuppressWarnings("rawtypes")
public class GrpcRemoteCacheService
        implements CacheService, ServiceInfo
    {
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
    public BackingMapManager getBackingMapManager()
        {
        return m_backingMapManager;
        }

    @Override
    public void setBackingMapManager(BackingMapManager manager)
        {
        m_backingMapManager = manager;
        }

    @Override
    public NamedCache ensureCache(String sName, ClassLoader ignored)
        {
        ClassLoader loader = getContextClassLoader();
        if (loader == null)
            {
            throw new IllegalStateException("ContextClassLoader is missing");
            }

        AsyncNamedCacheClient<?, ?> cache = m_scopedCacheStore.get(sName, loader);
        if (cache == null || !cache.isActiveInternal())
            {
            // this is one of the few places that acquiring a distinct lock per cache
            // is beneficial due to the potential cost of createRemoteNamedCache
            long cWait = getDependencies().getRequestTimeoutMillis();
            if (cWait <= 0)
                {
                cWait = -1;
                }
            if (!m_scopedCacheStore.lock(sName, cWait))
                {
                throw new RequestTimeoutException("Failed to get a reference to cache '" +
                    sName + "' after " + cWait + "ms");
                }
            try
                {
                cache = (AsyncNamedCacheClient) m_scopedCacheStore.get(sName, loader);
                if (cache == null || !cache.isActiveInternal())
                    {
                    cache = ensureAsyncCache(sName);
                    m_scopedCacheStore.put(cache, loader);
                    }
                }
            finally
                {
                m_scopedCacheStore.unlock(sName);
                }
            }

        return cache.getNamedCache();
        }

    @Override
    public Enumeration getCacheNames()
        {
        // instead of synchronizing on the Map and blocking all
        // the "put" and "remove" operations, we just catch any
        // ConcurrentModificationException and try again
        return new IteratorEnumerator(Arrays.asList(m_scopedCacheStore.getNames().toArray()).iterator());
        }

    @Override
    public void releaseCache(NamedCache map)
        {
        if (!(map instanceof NamedCacheClient<?,?>))
            {
            throw new IllegalArgumentException("illegal map: " + map);
            }
        map.release();
        NamedCacheClient<?,?> cache = (NamedCacheClient<?,?>) map;
        m_scopedCacheStore.release(cache.getAsyncClient());
        }

    @Override
    public void destroyCache(NamedCache map)
        {
        if (!(map instanceof NamedCacheClient<?,?>))
            {
            throw new IllegalArgumentException("illegal map: " + map);
            }
        map.destroy();
        NamedCacheClient<?,?> cache = (NamedCacheClient<?,?>) map;
        m_scopedCacheStore.release(cache.getAsyncClient());
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
    public void setDependencies(ServiceDependencies deps)
        {
        m_dependencies = (RemoteGrpcCacheServiceDependencies) deps;
        }

    @Override
    public RemoteGrpcCacheServiceDependencies getDependencies()
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
    public synchronized void start()
        {
        setChannel(instantiateChannel());
        setSerializer(instantiateSerializer(m_classLoader));
        setTracingInterceptor(instantiateTracingInterceptor());

        SimpleDaemonPoolExecutor executor = instantiateExecutor();
        setExecutor(executor);
        executor.start();

        m_fRunning = true;
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
    @SuppressWarnings("unchecked")
    public void stop()
        {
        if (m_fRunning)
            {
            synchronized (this)
                {
                if (m_fRunning)
                    {
                    for (AsyncNamedCacheClient<?, ?> cache : m_scopedCacheStore.getAll())
                        {
                        cache.removeDeactivationListener(f_deactivationListener);
                        try
                            {
                            cache.release();
                            }
                        catch (Throwable e)
                            {
                            e.printStackTrace();
                            }
                        }
                    m_scopedCacheStore.clear();

                    SimpleDaemonPoolExecutor executor = getExecutor();
                    executor.stop();

                    m_fRunning = false;
                    }
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
        return CacheService.TYPE_REMOTE_GRPC;
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
     * Creates a {@link AsyncNamedCacheClient} based on the provided arguments.
     *
     * @param sCacheName  the cache name
     * @param <K>         the key type
     * @param <V>         the value type
     *
     * @return a new {@link AsyncNamedCacheClient}
     */
    @SuppressWarnings("unchecked")
    protected <K, V> AsyncNamedCacheClient<K, V> ensureAsyncCache(String sCacheName)
        {
        Channel channel = m_tracingInterceptor == null
                ? m_channel
                : ClientInterceptors.intercept(m_channel, m_tracingInterceptor);

        RemoteGrpcCacheServiceDependencies dependencies = getDependencies();
        String                             sScopeName   = dependencies.getRemoteScopeName();
        GrpcCacheLifecycleEventDispatcher  dispatcher   = new GrpcCacheLifecycleEventDispatcher(sCacheName, this);


        AsyncNamedCacheClient.DefaultDependencies deps
                = new  AsyncNamedCacheClient.DefaultDependencies(sCacheName, channel, dispatcher);

        deps.setScope(sScopeName);
        deps.setSerializer(m_serializer, m_serializer.getName());
        deps.setExecutor(m_executor);

        AsyncNamedCacheClient<?, ?> client = new AsyncNamedCacheClient<>(deps);

        EventDispatcherRegistry dispatcherReg = getEventDispatcherRegistry();
        if (dispatcherReg != null)
            {
            dispatcherReg.registerEventDispatcher(dispatcher);
            }

        client.setCacheService(this);
        client.addDeactivationListener(f_truncateListener);
        client.addDeactivationListener(f_deactivationListener);

        // We must dispatch the created event async
        m_executor.execute(() -> dispatcher.dispatchCacheCreated(client.getNamedCache()));

        return (AsyncNamedCacheClient<K, V>) client;
        }

    protected EventDispatcherRegistry getEventDispatcherRegistry() {
        EventDispatcherRegistry reg = m_EventDispatcherRegistry;
        if (reg == null)
            {
            ResourceRegistry registry = getBackingMapManager().getCacheFactory().getResourceRegistry();
            setEventDispatcherRegistry(reg = registry.getResource(EventDispatcherRegistry.class));
            }
        return reg;
    }

    protected void setEventDispatcherRegistry(EventDispatcherRegistry registryInterceptor) {
        m_EventDispatcherRegistry = registryInterceptor;
    }

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
        RemoteGrpcCacheServiceDependencies deps        = getDependencies();
        GrpcChannelDependencies            depsChannel = deps.getChannelDependencies();
        Optional<ChannelProvider>          optional    = depsChannel.getChannelProvider();

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

    // ----- inner class: ClientDeactivationListener ------------------------

    /**
     * A {@link DeactivationListener} that cleans up the internal caches map
     * as clients are released or destroyed.
     */
    private class ClientDeactivationListener<K, V>
            implements DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>>
        {
        // ----- DeactivationListener interface -----------------------------

        @Override
        public void released(AsyncNamedCacheClient<? super K, ? super V> client)
            {
            GrpcRemoteCacheService.this.m_scopedCacheStore.remove(client.getCacheName());
            }

        @Override
        public void destroyed(AsyncNamedCacheClient<? super K, ? super V> client)
            {
            GrpcRemoteCacheService service = GrpcRemoteCacheService.this;
            boolean                removed = service.m_scopedCacheStore.release(client);
            if (removed)
                {
                GrpcCacheLifecycleEventDispatcher dispatcher    = client.getEventDispatcher();
                EventDispatcherRegistry           dispatcherReg = service.getEventDispatcherRegistry();
                dispatcher.dispatchCacheDestroyed(client.getNamedCache());
                dispatcherReg.unregisterEventDispatcher(client.getEventDispatcher());
                }
            }
        }

    // ----- inner class: TruncateListener ----------------------------------

    /**
     * A {@link DeactivationListener} that cleans up the internal caches map
     * as clients are released or destroyed.
     */
    @SuppressWarnings("rawtypes")
    private class TruncateListener
            extends AbstractMapListener
            implements NamedCacheDeactivationListener

        {
        // ----- NamedCacheDeactivationListener interface -------------------

        @Override
        public void entryUpdated(MapEvent evt)
            {
            NamedCache                  cache  = (NamedCache) evt.getMap();
            AsyncNamedCacheClient<?, ?> client = m_scopedCacheStore.get(cache.getCacheName(), getContextClassLoader());
            if (client != null)
                {
                client.getEventDispatcher().dispatchCacheTruncated((NamedCache) evt.getMap());
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * A counter to use for the daemon pool name suffix.
     */
    private static final AtomicInteger f_cPool = new AtomicInteger();

    // ----- data members ---------------------------------------------------

    private final Listeners f_memberListeners = new Listeners();

    private final Listeners f_serviceListeners = new Listeners();

    private final ResourceRegistry f_resourceRegistry = new SimpleResourceRegistry();

    /**
     * The {@link DeactivationListener} that will clean up client instances that
     * are destroyed or released.
     */
    @SuppressWarnings("rawtypes")
    private final ClientDeactivationListener f_deactivationListener = new ClientDeactivationListener<>();

    /**
     * The {@link TruncateListener} that will raise lifecycle events when
     * a map is truncated.
     */
    private final TruncateListener f_truncateListener = new TruncateListener();

//    /**
//     * The {@link AsyncNamedCacheClient} instances managed by this session.
//     */
//    private final Map<String, AsyncNamedCacheClient<?, ?>> f_mapCaches = new ConcurrentHashMap<>();

    private Cluster m_cluster;

    private ClassLoader m_classLoader;

    private Object m_oUserContext;

    private String m_sServiceName;

    private BackingMapManager m_backingMapManager;

    private volatile boolean m_fRunning;

    private RemoteGrpcCacheServiceDependencies m_dependencies;

    private String m_sScopeName;

    /**
     * The gRPC {@link Channel} used by this session.
     */
    private Channel m_channel;

    /**
     * The {@link Serializer} used by this session.
     */
    private Serializer m_serializer;

    /**
     * The optional client tracer to use.
     */
    private ClientInterceptor m_tracingInterceptor;

    /**
     * The {@link Executor} to use to dispatch events.
     */
    private SimpleDaemonPoolExecutor m_executor;

    /**
     * The {@link EventDispatcherRegistry}.
     */
    private EventDispatcherRegistry m_EventDispatcherRegistry;

    /**
     * The store of cache references, optionally scoped by Subject.
     */
    private final ScopedGrpcAsyncCacheReferenceStore m_scopedCacheStore = new ScopedGrpcAsyncCacheReferenceStore();
    }
