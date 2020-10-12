/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;
import com.oracle.coherence.grpc.Requests;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.MapEvent;

import io.grpc.Channel;

import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;

import io.opentracing.contrib.grpc.TracingClientInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A client {@link Session} that connects to a remote gRPC proxy.
 *
 * @author Jonathan Knight  2020.09.22
 * @since 20.06
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class GrpcRemoteSession
        implements Session
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code GrpcRemoteSession}.
     *
     * @param builder      the {@link Builder}
     */
    protected GrpcRemoteSession(Builder builder)
        {
        f_sName                    = builder.getName().orElse(Requests.DEFAULT_SESSION_NAME);
        f_sScopeName               = builder.getScope();
        f_channel                  = builder.getChannel();
        f_sSerializerFormat        = builder.ensureSerializerFormat();
        f_serializer               = builder.ensureSerializer();
        m_executor                 = builder.ensureExecutor();
        f_tracing                  = builder.ensureTracing();
        f_mapCaches                = new ConcurrentHashMap<>();
        f_deactivationListener     = new ClientDeactivationListener<>();
        f_truncateListener         = new TruncateListener();
        f_listMapLifecycleListener = Collections.synchronizedList(new ArrayList<>());
        }

    // ----- public methods -------------------------------------------------

    /**
     * Obtain the scope name used to link this {@link GrpcRemoteSession}
     * to the {@link com.tangosol.net.ConfigurableCacheFactory} on the server
     * that has the corresponding scope.
     *
     * @return the scope name for this {@link GrpcRemoteSession}
     */
    public String getScope()
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
     * Create a default {@link GrpcRemoteSession}.
     *
     * @param channel  the gRPC {@link Channel} to use
     *
     * @return a default {@link GrpcRemoteSession}
     *
     * @throws NullPointerException if the {@code channel} parameter is {@code null}
     */
    public static GrpcRemoteSession create(Channel channel)
        {
        return builder(channel).build();
        }

    /**
     * Create a {@link GrpcRemoteSession} {@link Builder}.
     *
     * @param channel  the gRPC {@link Channel} to use
     *
     * @return a default {@link GrpcRemoteSession} {@link Builder}
     *
     * @throws NullPointerException if the {@code channel} parameter is {@code null}
     */
    public static Builder builder(Channel channel)
        {
        return new Builder(channel);
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

    /**
     * Add a {@link DeactivationListener} that will be notified when this
     * {@link GrpcRemoteSession} is closed.
     * <p>
     * If this {@link GrpcRemoteSession} is already closed then the listener's
     * {@link DeactivationListener#destroyed(Object)} method will be called
     * immediately on the calling thread.
     *
     * @param listener  the {@link DeactivationListener} to add
     *
     * @throws NullPointerException if the listener is null
     */
    public synchronized void addDeactivationListener(DeactivationListener<GrpcRemoteSession> listener)
        {
        Objects.requireNonNull(listener);
        if (!m_fClosed)
            {
            f_listDeactivationListeners.add(listener);
            }
        else
            {
            listener.destroyed(this);
            }
        }

    /**
     * Remove a {@link DeactivationListener} that will be notified when this
     * {@link GrpcRemoteSession} is closed.
     *
     * @param listener  the {@link DeactivationListener} to remove
     */
    public synchronized void removeDeactivationListener(DeactivationListener<GrpcRemoteSession> listener)
        {
        f_listDeactivationListeners.remove(listener);
        }

    /**
     * Add a {@link RemoteMapLifecycleListener} to this session.
     * <p>
     * If the current session already contains the listener (based on Object equality)
     * then this method is a no-op.
     *
     * @param listener  the {@link RemoteMapLifecycleListener} to add
     */
    public synchronized void addMapLifecycleListener(RemoteMapLifecycleListener listener)
        {
        if (f_listMapLifecycleListener.contains(listener))
            {
            return;
            }
        f_listMapLifecycleListener.add(listener);
        }

    /**
     * Remove a {@link RemoteMapLifecycleListener} from this session.
     *
     * @param listener  the {@link RemoteMapLifecycleListener} to remove
     */
    public synchronized void removeMapLifecycleListener(RemoteMapLifecycleListener listener)
        {
        f_listMapLifecycleListener.remove(listener);
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

        for (DeactivationListener<GrpcRemoteSession> listener : f_listDeactivationListeners)
            {
            try
                {
                listener.destroyed(this);
                }
            catch (Throwable e)
                {
                e.printStackTrace();
                }
            }
        f_listDeactivationListeners.clear();
        f_listMapLifecycleListener.clear();
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Returns {@code true} if this session has been closed, otherwise returns {@link false}.
     *
     * @return {@code true} if this session has been closed, otherwise {@link false}
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
        Channel channel = f_tracing
                .map(t -> ClientInterceptors.intercept(f_channel, t))
                .orElse(f_channel);

        AsyncNamedCacheClient.DefaultDependencies deps
                = new  AsyncNamedCacheClient.DefaultDependencies(sCacheName, channel);

        deps.setScope(f_sScopeName);
        deps.setSerializer(f_serializer, f_sSerializerFormat);
        deps.setExecutor(m_executor);

        AsyncNamedCacheClient<?, ?> client = new AsyncNamedCacheClient<>(deps);

        for (RemoteMapLifecycleListener listener : f_listMapLifecycleListener)
            {
            try
                {
                listener.onCreate(client.getNamedMap(), f_sScopeName, this);
                }
            catch (Throwable t)
                {
                Logger.err(t);
                }
            }

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
                for (RemoteMapLifecycleListener listener : f_listMapLifecycleListener)
                    {
                    try
                        {
                        listener.onDestroy(removed.getNamedMap(), f_sScopeName, session);
                        }
                    catch (Throwable t)
                        {
                        Logger.err(t);
                        }
                    }
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
            GrpcRemoteSession session = GrpcRemoteSession.this;
            for (RemoteMapLifecycleListener listener : f_listMapLifecycleListener)
                {
                try
                    {
                    listener.onTruncate((NamedMap<?, ?>) evt.getMap(), f_sScopeName, session);
                    }
                catch (Throwable t)
                    {
                    Logger.err(t);
                    }
                }
            }
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder of {@link GrpcRemoteSession} instances.
     */
    public static class Builder
        {
        // ----- constructors -----------------------------------------------

        private Builder(Channel channel)
            {
            f_channel = Objects.requireNonNull(channel);
            }

        // ----- public methods ---------------------------------------------

        /**
         * Set the scope name of the session.
         *
         * @param sScopeName  the scope name of the session
         *
         * @return this {@link Builder}
         */
        public Builder setScope(String sScopeName)
            {
            m_sScopeName = sScopeName;
            return this;
            }

        /**
         * Set the name of the session.
         *
         * @param sName  the name of the session
         *
         * @return this {@link Builder}
         */
        public Builder setName(String sName)
            {
            m_sName = sName == null ? Requests.DEFAULT_SESSION_NAME : sName;
            return this;
            }

        /**
         * Set the {@link Serializer} that the session will use.
         *
         * @param serializer  the {@link Serializer} that the session will use
         *
         * @return this {@link Builder}
         */
        public Builder serializer(Serializer serializer)
            {
            return serializer(serializer, serializer.getName());
            }

        /**
         * Set the {@link Serializer} format that the session will use.
         *
         * @param format  the optional name of the serializer format
         *
         * @return this {@link Builder}
         */
        public Builder serializer(String format)
            {
            return serializer(null, format);
            }

        /**
         * Set the {@link Serializer} that the session will use.
         * <p>
         * If the serializer format is {@code null} the {@link Serializer#getName()}
         * method will be used to obtain the format name.
         *
         * @param serializer  the {@link Serializer} that the session will use
         * @param format      the optional name of the serializer format
         *
         * @return this {@link Builder}
         */
        public Builder serializer(Serializer serializer, String format)
            {
            m_serializer = serializer;
            m_sSerializerFormat = format;
            if (m_sSerializerFormat == null && serializer != null)
                {
                m_sSerializerFormat = serializer.getName();
                if (m_sSerializerFormat == null)
                    {
                    if (serializer instanceof DefaultSerializer)
                        {
                        m_sSerializerFormat = "java";
                        }
                    else if (serializer instanceof ConfigurablePofContext)
                        {
                        m_sSerializerFormat = "pof";
                        }
                    }
                }

            return this;
            }

        /**
         * Set the {@link TracingClientInterceptor} to use for the session.
         *
         * @param interceptor the {@link TracingClientInterceptor} to use for the session
         *
         * @return this {@link Builder}
         */
        public Builder tracing(ClientInterceptor interceptor)
            {
            m_tracingClientInterceptor = interceptor;
            return this;
            }

        // ----- Builder interface ------------------------------------------

        public GrpcRemoteSession build()
            {
            return new GrpcRemoteSession(this);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Returns the optional session name.
         *
         * @return the session name or {@code null} if the session has no name
         */
        public Optional<String> getName()
            {
            return Optional.ofNullable(m_sName);
            }

        /**
         * Returns the {@link Channel} to use.
         *
         * @return the {@link Channel} to use
         */
        public Channel getChannel()
            {
            return f_channel;
            }

        /**
         * Return the scope name of the session, or {@link Requests#DEFAULT_SCOPE}
         * if one hasn't been provided
         * either to this builder of in config.
         *
         * @return the scope name of the session, or {@link Requests#DEFAULT_SCOPE}
         * if one hasn't been provided
         */
        protected String getScope()
            {
            if (m_sScopeName == null)
                {
                return Requests.DEFAULT_SCOPE;
                }
            return m_sScopeName;
            }

        /**
         * Return the tracing {@link ClientInterceptor}, if any.
         *
         * @return the tracing {@link ClientInterceptor}, if any
         */
        protected Optional<ClientInterceptor> ensureTracing()
            {
            return Optional.ofNullable(m_tracingClientInterceptor);
            }

        /**
         * Ensure the serialization format is initialized and returned.
         *
         * @return the serialization format
         */
        protected String ensureSerializerFormat()
            {
            if (m_sSerializerFormat != null && !m_sSerializerFormat.isEmpty())
                {
                return m_sSerializerFormat;
                }

            return Config.getBoolean("coherence.pof.enabled") ? "pof" : "java";
            }

        /**
         * Ensure the {@link Serializer} is initialized and returned.
         *
         * @return the {@link Serializer}
         */
        protected Serializer ensureSerializer()
            {
            if (m_serializer != null)
                {
                return m_serializer;
                }

            String sFormat = ensureSerializerFormat();

            if (sFormat.equals("pof"))
                {
                return f_defaultPofSerializer;
                }
            else if (sFormat.equals("java"))
                {
                return f_defaultJavaSerializer;
                }
            else
                {
                throw new IllegalArgumentException("Unknown serializer format " + sFormat
                        + " and no Serializer has been specified");
                }
            }

        /**
         * Obtain the {@link Executor} to be used by the session.
         *
         * @return  the {@link Executor} to be used by the session
         */
        Executor ensureExecutor()
            {
            if (m_executor == null)
                {
                String                   sName = "Grpc-Daemon-Pool-" + f_cPool.getAndIncrement();
                SimpleDaemonPoolExecutor pool  = new SimpleDaemonPoolExecutor(sName);
                pool.start();
                m_executor = pool;
                }
            return m_executor;
            }

        // ----- data members -----------------------------------------------

        /**
         * The optional session name.
         */
        protected String m_sName;

        /**
         * The session scope name.
         */
        protected String m_sScopeName;

        /**
         * The communications channel.
         */
        protected final Channel f_channel;

        /**
         * The serializer.
         */
        protected Serializer m_serializer;

        /**
         * The serialization format.
         */
        protected String m_sSerializerFormat;

        /**
         * The {@link Executor} to be used by the session.
         */
        protected Executor m_executor;

        /**
         * An optional {@link ClientInterceptor} to apply to the session.
         */
        protected ClientInterceptor m_tracingClientInterceptor;

        private static final Serializer f_defaultJavaSerializer = new DefaultSerializer();

        private static final Serializer f_defaultPofSerializer = new ConfigurablePofContext();
        }

    // ----- inner class: SessionDeactivationListener -----------------------

    /**
     * A listener that will be called when a session is closed.
     */
    public interface SessionDeactivationListener
            extends DeactivationListener<GrpcRemoteSession>
        {
        // ----- DeactivationListener interface -----------------------------

        @Override
        default void released(GrpcRemoteSession resource)
            {
            }
        }

    // ----- inner class: SessionDeactivationListener -----------------------

    /**
     * A listener that will be called for lifecycle events on remote {@link NamedMap}
     * instances controlled by this session.
     */
    public interface RemoteMapLifecycleListener
        {
        /**
         * Indicates that a {@link NamedMap} has been created.
         *
         * @param map      the {@link NamedMap} that the event is for
         * @param sScope   the name of the scope that the map is in
         * @param session  the {@link com.tangosol.net.Session} that owns the map
         */
        void onCreate(NamedMap<?, ?> map, String sScope, Session session);

        /**
         * Indicates that a {@link NamedMap} has been destroyed.
         *
         * @param map      the {@link NamedMap} that the event is for
         * @param sScope   the name of the scope that the map is in
         * @param session  the {@link com.tangosol.net.Session} that owns the map
         */
        void onDestroy(NamedMap<?, ?> map, String sScope, Session session);

        /**
         * Indicates that a {@link NamedMap} has been truncated.
         *
         * @param map      the {@link NamedMap} that the event is for
         * @param sScope   the name of the scope that the map is in
         * @param session  the {@link com.tangosol.net.Session} that owns the map
         */
        void onTruncate(NamedMap<?, ?> map, String sScope, Session session);
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
     * A list of {@link DeactivationListener} instances to be notified if this
     * session is closed.
     */
    protected final List<DeactivationListener<GrpcRemoteSession>> f_listDeactivationListeners = new ArrayList<>();

    /**
     * The optional client tracer to use.
     */
    protected final Optional<ClientInterceptor> f_tracing;

    /**
     * A flag indicating whether this session has been closed.
     */
    protected boolean m_fClosed;

    /**
     * The {@link Executor} to use to dispatch events.
     */
    protected Executor m_executor;

    /**
     * The list of remote map lifecycle listeners.
     */
    protected final List<RemoteMapLifecycleListener> f_listMapLifecycleListener;
    }
