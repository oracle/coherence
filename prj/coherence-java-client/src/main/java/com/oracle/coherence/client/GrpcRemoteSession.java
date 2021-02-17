/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;
import com.oracle.coherence.grpc.Requests;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.io.Serializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.net.events.internal.Registry;

import com.tangosol.net.events.internal.SessionEventDispatcher;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.MapEvent;

import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import io.grpc.Channel;

import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;

import java.util.Map;
import java.util.Objects;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A client {@link Session} that connects to a remote gRPC proxy.
 *
 * @author Jonathan Knight  2020.09.22
 * @since 20.06
 */
public class GrpcRemoteSession
        implements Session
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code GrpcRemoteSession}.
     *
     * @param channel             the gRPC {@link Channel} to use
     * @param sName               the name of the session
     * @param sScopeName          the scope name of the session
     * @param serializer          the serializer to use for message payloads
     * @param sFormat             the name of the serializer format
     * @param tracingInterceptor  an optional client tracing interceptor
     * @param interceptors        optional interceptors to add to the session
     */
    protected GrpcRemoteSession(Channel           channel,
                                String            sName,
                                String            sScopeName,
                                Serializer        serializer,
                                String            sFormat,
                                ClientInterceptor tracingInterceptor,
                                Iterable<? extends EventInterceptor<?>> interceptors)
        {
        f_channel              = Objects.requireNonNull(channel);
        f_sName                = sName == null ? Coherence.DEFAULT_NAME : sName;
        f_sScopeName           = sScopeName == null ? Requests.DEFAULT_SCOPE : sScopeName;
        f_serializer           = Objects.requireNonNull(serializer);
        f_sSerializerFormat    = Objects.requireNonNull(sFormat);
        f_tracingInterceptor   = tracingInterceptor;
        f_mapCaches            = new ConcurrentHashMap<>();
        f_deactivationListener = new ClientDeactivationListener<>();
        f_truncateListener     = new TruncateListener();

        String                   sPoolName = "Grpc-Daemon-Pool-" + f_cPool.getAndIncrement();
        SimpleDaemonPoolExecutor pool      = new SimpleDaemonPoolExecutor(sPoolName);
        pool.start();
        f_executor = pool;

        Registry eventRegistry = new Registry();
        f_registry = new SimpleResourceRegistry();
        f_registry.registerResource(InterceptorRegistry.class, eventRegistry);
        f_registry.registerResource(EventDispatcherRegistry.class, eventRegistry);

        f_eventDispatcher = new SessionEventDispatcher(this);
        eventRegistry.registerEventDispatcher(f_eventDispatcher);

        if (interceptors != null)
            {
            for (EventInterceptor<?> interceptor : interceptors)
                {
                eventRegistry.registerEventInterceptor(interceptor, RegistrationBehavior.FAIL);
                }
            }
        }

    // ----- public methods -------------------------------------------------

    @Override
    public synchronized void activate()
        {
        if (m_fActivated)
            {
            return;
            }

        f_eventDispatcher.dispatchStarting();
        m_fActivated = true;
        f_eventDispatcher.dispatchStarted();
        }

    @Override
    public boolean isActive()
        {
        return m_fActivated && !isClosed();
        }

    /**
     * Obtain the scope name used to link this {@link GrpcRemoteSession}
     * to the {@link com.tangosol.net.ConfigurableCacheFactory} on the server
     * that has the corresponding scope.
     *
     * @return the scope name for this {@link GrpcRemoteSession}
     */
    public String getScopeName()
        {
        return f_sScopeName;
        }

    /**
     * Obtain the optional name of this session.
     *
     * @return  the name of this session or {@code null} if
     *          this session has no name
     */
    public String getName()
        {
        return f_sName;
        }

    /**
     * Obtain the scope name of this session.
     * <p>
     * The scope name is used to identify the ConfigurableCacheFactory
     * on the server that owns the resources that this session connects
     * to, for example maps, caches and topics.
     * <p>
     * In most use cases the default scope name is used, but when the
     * server has multiple scoped ConfigurableCacheFactory instances
     * the scope name can be specified to link the session to a
     * ConfigurableCacheFactory.
     *
     * @return the scope name of this session
     */
    public String scope()
        {
        return f_sScopeName;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        GrpcRemoteSession that = (GrpcRemoteSession) o;
        return Objects.equals(f_sScopeName, that.f_sScopeName) &&
                Objects.equals(f_channel, that.f_channel) &&
                Objects.equals(f_serializer, that.f_serializer) &&
                Objects.equals(f_sSerializerFormat, that.f_sSerializerFormat);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_sScopeName, f_channel, f_serializer, f_sSerializerFormat);
        }

    @Override
    public String toString()
        {
        return "RemoteSession{"
               + "scope: \"" + f_sScopeName + '"'
               + ", serializerFormat \"" + f_sSerializerFormat + '"'
               + ", serializer \"" + f_serializer + '"'
               + ", closed: " + m_fClosed
               + '}';
        }

    // ----- Session interface ----------------------------------------------

    @Override
    public <K, V> NamedMap<K, V> getMap(String sName, NamedMap.Option... options)
        {
        return getCache(sName, options);
        }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> NamedCache<K, V> getCache(String cacheName, NamedCache.Option... options)
        {
        if (m_fClosed)
            {
            throw new IllegalStateException("this session has been closed");
            }
        return (NamedCache<K, V>) getAsyncCache(cacheName, options).getNamedCache();
        }

    @Override
    public <V> NamedTopic<V> getTopic(String sName, NamedCollection.Option... options)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void close()
        {
        Logger.info("Closing Session " + getName());
        f_eventDispatcher.dispatchStopping();

        m_fActivated = false;
        m_fClosed = true;

        for (AsyncNamedCacheClient<?, ?> client : f_mapCaches.values())
            {
            client.removeDeactivationListener(f_deactivationListener);
            try
                {
                client.release();
                }
            catch (Throwable e)
                {
                e.printStackTrace();
                }
            }
        f_mapCaches.clear();
        f_registry.dispose();

        f_eventDispatcher.dispatchStopped();
        Logger.info("Closed Session " + getName());
        }

    @Override
    public ResourceRegistry getResourceRegistry()
        {
        return f_registry;
        }

    @Override
    public InterceptorRegistry getInterceptorRegistry()
        {
        return getResourceRegistry().getResource(InterceptorRegistry.class);
        }

    @Override
    public boolean isCacheActive(String sCacheName, ClassLoader loader)
        {
        AsyncNamedCacheClient<?, ?> cache = f_mapCaches.get(sCacheName);
        return cache != null && cache.isActiveInternal();
        }

    @Override
    public boolean isMapActive(String sMapName, ClassLoader loader)
        {
        return isCacheActive(sMapName, loader);
        }

    @Override
    public boolean isTopicActive(String sTopicName, ClassLoader loader)
        {
        // ToDo: Implement this when we add topic support
        return false;
        }

    @Override
    public Service getService(String sServiceName)
        {
        return null;
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Returns the set of names of currently active caches that have been
     * previously created by this session.
     *
     * @return the set of cache names
     */
    public Set<String> getCacheNames()
        {
        return f_mapCaches.entrySet()
                          .stream()
                          .filter(e -> e.getValue().getNamedCacheClient().isActive())
                          .map(Map.Entry::getKey)
                          .collect(Collectors.toSet());
        }

    /**
     * Returns {@code true} if this session has been closed, otherwise returns {@code false}.
     *
     * @return {@code true} if this session has been closed, otherwise {@code false}
     */
    public boolean isClosed()
        {
        return m_fClosed;
        }

    /**
     * Return the communication channel.
     *
     * @return the communication channel
     */
    protected Channel getChannel()
        {
        return f_channel;
        }

    /**
     * Return the {@link Serializer}.
     *
     * @return the {@link Serializer}
     */
    public Serializer getSerializer()
        {
        return f_serializer;
        }

    /**
     * Return the serialization format.
     *
     * @return the serialization format
     */
    public String getSerializerFormat()
        {
        return f_sSerializerFormat;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the {@link AsyncNamedCacheClient} based on the provided arguments.
     *
     * @param sCacheName  the cache name
     * @param options     the cache options
     * @param <K>         the key type
     * @param <V>         the value type
     *
     * @return  the {@link AsyncNamedCacheClient}
     */
    @SuppressWarnings("unchecked")
    public synchronized <K, V> AsyncNamedCacheClient<K, V> getAsyncCache(String sCacheName,
                                                                         NamedCache.Option... options)
        {
        AsyncNamedCacheClient<K, V> client = (AsyncNamedCacheClient<K, V>)
                f_mapCaches.computeIfAbsent(sCacheName, k -> ensureCache(sCacheName));
        if (client.isActiveInternal())
            {
            return client;
            }
        else
            {
            f_mapCaches.remove(sCacheName);
            return getAsyncCache(sCacheName, options);
            }
        }

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
    protected <K, V> AsyncNamedCacheClient<K, V> ensureCache(String sCacheName)
        {
        Channel channel = f_tracingInterceptor == null
                ? f_channel
                : ClientInterceptors.intercept(f_channel, f_tracingInterceptor);

        GrpcCacheLifecycleEventDispatcher dispatcher = new GrpcCacheLifecycleEventDispatcher(sCacheName, this);

        AsyncNamedCacheClient.DefaultDependencies deps
                = new  AsyncNamedCacheClient.DefaultDependencies(sCacheName, channel, dispatcher);

        deps.setScope(f_sScopeName);
        deps.setSerializer(f_serializer, f_sSerializerFormat);
        deps.setExecutor(f_executor);

        AsyncNamedCacheClient<?, ?> client = new AsyncNamedCacheClient<>(deps);

        EventDispatcherRegistry dispatcherReg = f_registry.getResource(EventDispatcherRegistry.class);
        if (dispatcherReg != null)
            {
            dispatcherReg.registerEventDispatcher(dispatcher);
            }

        // We must dispatch the created event async
        f_executor.execute(() -> dispatcher.dispatchCacheCreated(client.getNamedCache()));

        client.addDeactivationListener(f_truncateListener);
        client.addDeactivationListener(f_deactivationListener);
        return (AsyncNamedCacheClient<K, V>) client;
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
            GrpcRemoteSession.this.f_mapCaches.remove(client.getCacheName());
            }

        @Override
        public void destroyed(AsyncNamedCacheClient<? super K, ? super V> client)
            {
            GrpcRemoteSession           session = GrpcRemoteSession.this;
            AsyncNamedCacheClient<?, ?> removed = session.f_mapCaches.remove(client.getCacheName());
            if (removed != null)
                {
                GrpcCacheLifecycleEventDispatcher dispatcher    = removed.getEventDispatcher();
                EventDispatcherRegistry           dispatcherReg = f_registry.getResource(EventDispatcherRegistry.class);
                dispatcher.dispatchCacheDestroyed(removed.getNamedCache());
                dispatcherReg.unregisterEventDispatcher(removed.getEventDispatcher());
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
            NamedCache                  cacheEvent = (NamedCache) evt.getMap();
            AsyncNamedCacheClient<?, ?> client     = f_mapCaches.get(cacheEvent.getCacheName());
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

    /**
     * The scope name to link this session to a
     * {@link com.tangosol.net.ConfigurableCacheFactory}
     * on the server.
     */
    private final String f_sScopeName;

    /**
     * The optional name for this session.
     */
    private final String f_sName;

    /**
     * The gRPC {@link Channel} used by this session.
     */
    private final Channel f_channel;

    /**
     * The {@link Serializer} used by this session.
     */
    private final Serializer f_serializer;

    /**
     * The name of the {@link Serializer} format.
     */
    private final String f_sSerializerFormat;

    /**
     * The {@link AsyncNamedCacheClient} instances managed by this session.
     */
    private final Map<String, AsyncNamedCacheClient<?, ?>> f_mapCaches;

    /**
     * The {@link DeactivationListener} that will clean-up client instances that
     * are destroyed or released.
     */
    @SuppressWarnings("rawtypes")
    private final ClientDeactivationListener f_deactivationListener;

    /**
     * The {@link TruncateListener} that will raise lifecycle events when
     * a map is truncated.
     */
    private final TruncateListener f_truncateListener;

    /**
     * The dispatcher for lifecycle events.
     */
    private final SessionEventDispatcher f_eventDispatcher;

    /**
     * The optional client tracer to use.
     */
    protected final ClientInterceptor f_tracingInterceptor;

    /**
     * A flag indicating whether this session has been activated.
     */
    protected boolean m_fActivated;

    /**
     * A flag indicating whether this session has been closed.
     */
    protected boolean m_fClosed;

    /**
     * The {@link Executor} to use to dispatch events.
     */
    protected final Executor f_executor;

    /**
     * This session's {@link ResourceRegistry}.
     */
    protected final ResourceRegistry f_registry;
    }
