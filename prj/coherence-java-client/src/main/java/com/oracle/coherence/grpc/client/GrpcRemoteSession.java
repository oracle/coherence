/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client;

import com.oracle.coherence.cdi.SerializerProducer;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedMap;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.Base;

import io.grpc.Channel;
import io.grpc.ManagedChannel;

import io.helidon.config.Config;

import io.helidon.grpc.client.ClientTracingInterceptor;
import io.helidon.grpc.client.GrpcChannelsProvider;

import io.opentracing.Tracer;

import io.opentracing.util.GlobalTracer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.Instance;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

/**
 * A client {@link Session} that connects to a remote gRPC proxy.
 *
 * @author Jonathan Knight  2019.11.28
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
     * @param beanManager  the {@link BeanManager} for CDI support
     */
    protected GrpcRemoteSession(Builder builder, BeanManager beanManager)
        {
        this.f_beanManager          = beanManager;
        this.f_sName                = builder.name();
        this.f_channel              = builder.ensureChannel();
        this.f_sSerializerFormat    = builder.ensureSerializerFormat();
        this.f_serializer           = builder.ensureSerializer(this.f_sSerializerFormat);
        this.f_tracing              = builder.ensureTracing();
        this.f_mapCaches            = new ConcurrentHashMap<>();
        this.f_deactivationListener = new ClientDeactivationListener<>();
        }

    // ----- public methods -------------------------------------------------

    /**
     * Create a default {@link GrpcRemoteSession}.
     *
     * @return a default {@link GrpcRemoteSession}
     */
    public static GrpcRemoteSession create()
        {
        return builder().build();
        }

    /**
     * Create a {@link GrpcRemoteSession} {@link Builder}.
     *
     * @return a default {@link GrpcRemoteSession} {@link Builder}
     */
    public static Builder builder()
        {
        return new Builder(Config.empty());
        }

    /**
     * Create a {@link GrpcRemoteSession} {@link Builder}.
     * <p>
     * The {@link Config} passed to this method should be the root {@link Config}
     * as the builder requires access to the configuration keys {@code grpc} and
     * {@code coherence}.
     *
     * @param config  the {@link Config} to use to configure the session.
     *
     * @return a default {@link GrpcRemoteSession} {@link Builder}
     */
    public static Builder builder(Config config)
        {
        if (config == null)
            {
            config = Config.empty();
            }
        return new Builder(config);
        }

    /**
     * Obtain the name of this session.
     *
     * @return the name of this session
     */
    public String name()
        {
        return f_sName;
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

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "RemoteSession{"
               + "name: \"" + f_sName + '"'
               + ", serializerFormat \"" + f_sSerializerFormat + '"'
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
    protected Serializer getSerializer()
        {
        return f_serializer;
        }

    /**
     * Return the serialization format.
     *
     * @return the serialization format
     */
    protected String getSerializerFormat()
        {
        return f_sSerializerFormat;
        }

    /**
     * Return the {@link BeanManager}.
     *
     * @return the {@link BeanManager}
     */
    protected BeanManager getBeanManager()
        {
        return f_beanManager;
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
    protected synchronized <K, V> AsyncNamedCacheClient<K, V> getAsyncCache(String sCacheName,
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
        Channel tracedChannel = f_tracing.map(t -> t.intercept(f_channel))
                .orElse(f_channel);

        AsyncNamedCacheClient<?, ?> client = AsyncNamedCacheClient.builder(sCacheName)
                .channel(tracedChannel)
                .serializer(f_serializer, f_sSerializerFormat)
                .beanManager(f_beanManager)
                .build();

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
            GrpcRemoteSession.this.f_mapCaches.remove(client.getCacheName());
            }
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder of {@link GrpcRemoteSession} instances.
     */
    public static class Builder
            implements io.helidon.common.Builder<GrpcRemoteSession>
        {
        // ----- constructors -----------------------------------------------

        private Builder(Config config)
            {
            this.f_config = config;
            this.m_sName = DEFAULT_NAME;
            this.m_sChannelName = GrpcChannelsProvider.DEFAULT_CHANNEL_NAME;
            this.m_beanManager = ensureBeanManager();
            }

        // ----- public methods ---------------------------------------------

        /**
         * Set the name of the session.
         *
         * @param name  the name of the session
         *
         * @return this {@link Builder}
         */
        public Builder name(String name)
            {
            this.m_sName = name;
            return this;
            }

        /**
         * Set the name of the {@link io.grpc.Channel} that the session will use.
         *
         * @param name  the name of the {@link io.grpc.Channel} that the session will use
         *
         * @return this {@link Builder}
         */
        public Builder channelName(String name)
            {
            this.m_sChannelName = name;
            this.m_channel = null;
            return this;
            }

        /**
         * Set the {@link io.grpc.ManagedChannel} that the session will use.
         *
         * @param channel  the {@link io.grpc.ManagedChannel} that the
         *                 session will use
         *
         * @return this {@link Builder}
         */
        public Builder channel(ManagedChannel channel)
            {
            this.m_channel = channel;
            this.m_sChannelName = null;
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
            this.m_serializer = serializer;
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
         * Set the {@link javax.enterprise.inject.spi.BeanManager} to use.
         * <p>
         * In a typical CDI environment the {@link javax.enterprise.inject.spi.BeanManager}
         * will be discovered automatically.
         *
         * @param beanManager  the {@link javax.enterprise.inject.spi.BeanManager} to use
         *
         * @return this {@link Builder}
         */
        public Builder beanManager(BeanManager beanManager)
            {
            this.m_beanManager = beanManager;
            return this;
            }

        // ----- Builder interface ------------------------------------------

        @Override
        public GrpcRemoteSession build()
            {
            return new GrpcRemoteSession(this, m_beanManager);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return this {@link Session}'s {@link Config configuration}.
         *
         * @return this {@link Session}'s {@link Config configuration}
         */
        protected Config sessionConfig()
            {
            return this.f_config.get(CFG_KEY_COHERENCE)
                    .get("sessions")
                    .get(name());
            }

        /**
         * Return the name of the session, or {@value DEFAULT_NAME} of one hasn't been provided.
         *
         * @return the name of the session, or {@value DEFAULT_NAME} of one hasn't been provided
         */
        protected String name()
            {
            return m_sName == null || m_sName.isEmpty() ? DEFAULT_NAME : m_sName;
            }

        /**
         * Creates a new {@link ManagedChannel}, if necessary.
         *
         * @return a new {@link ManagedChannel}, if necessary
         */
        protected ManagedChannel ensureChannel()
            {
            Config sessionConfig = sessionConfig();

            if (m_channel != null)
                {
                return m_channel;
                }

            String channel = m_sChannelName;
            if (m_sChannelName == null || m_sChannelName.isEmpty())
                {
                channel = sessionConfig.get("channel")
                        .asString()
                        .orElse(GrpcChannelsProvider.DEFAULT_CHANNEL_NAME);
                }

            if (m_beanManager != null)
                {
                Instance<Channel> instance = m_beanManager.createInstance()
                        .select(Channel.class, GrpcChannelLiteral.of(channel));
                if (instance.isResolvable())
                    {
                    return (ManagedChannel) instance.get();
                    }
                else if (instance.isAmbiguous())
                    {
                    throw new IllegalStateException("Cannot discover Channel for name '" + channel
                                                    + " bean lookup results in ambiguous bean instances");
                    }
                else
                    {
                    throw new IllegalStateException("Cannot discover Channel for name '" + channel
                                                    + " name is unresolvable");
                    }
                }
            else
                {
                GrpcChannelsProvider provider = GrpcChannelsProvider.builder(f_config.get("grpc")).build();
                return (ManagedChannel) provider.channel(channel);
                }
            }

        /**
         * Return the {@link ClientTracingInterceptor}, if any.
         *
         * @return the {@link ClientTracingInterceptor}, if any
         */
        protected Optional<ClientTracingInterceptor> ensureTracing()
            {
            Config sessionConfig = sessionConfig();

            Config config = sessionConfig.get("tracing");
            if (config.get("enabled").asBoolean().orElse(false))
                {
                Tracer tracer = GlobalTracer.get();
                ClientTracingInterceptor.Builder builder = ClientTracingInterceptor.builder(tracer);

                if (config.get("verbose").asBoolean().orElse(true))
                    {
                    builder.withVerbosity();
                    }

                if (config.get("streaming").asBoolean().orElse(true))
                    {
                    builder.withStreaming();
                    }

                return Optional.of(builder.build());
                }
            else
                {
                return Optional.empty();
                }
            }

        /**
         * Ensure the serialization format is initialized and returned.
         *
         * @return the serialization format
         */
        protected String ensureSerializerFormat()
            {
            Config sessionConfig = sessionConfig();

            if (m_sSerializerFormat != null && !m_sSerializerFormat.isEmpty())
                {
                return m_sSerializerFormat;
                }

            return sessionConfig.get("serializer")
                    .asString()
                    .orElseGet(() -> Boolean.getBoolean("coherence.pof.enabled") ? "pof" : "java");
            }

        /**
         * Ensure the {@link Serializer} is initialized and returned.
         *
         * @param format  the serialization format
         *
         * @return the {@link Serializer}
         */
        protected Serializer ensureSerializer(String format)
            {
            if (m_serializer != null)
                {
                return m_serializer;
                }
            else
                {
                return SerializerProducer.builder()
                        .context((OperationalContext) CacheFactory.getCluster())
                        .beanManager(m_beanManager)
                        .build()
                        .getNamedSerializer(format, Base.getContextClassLoader());
                }
            }

        /**
         * Ensure the {@link BeanManager} is initialized and returned.
         *
         * @return the {@link BeanManager}
         */
        @SuppressWarnings("OptionalAssignedToNull")
        protected BeanManager ensureBeanManager()
            {
            if (defaultBeanManager == null)
                {
                try
                    {
                    BeanManager bm = CDI.current().getBeanManager();
                    defaultBeanManager = Optional.ofNullable(bm);
                    }
                catch (Exception e)
                    {
                    defaultBeanManager = Optional.empty();
                    }
                }
            return defaultBeanManager.orElse(null);
            }

        // ----- data members -----------------------------------------------

        /**
         * The optional {@link BeanManager}.
         */
        protected static Optional<BeanManager> defaultBeanManager;

        /**
         * The session name.
         */
        protected String m_sName;

        /**
         * The channel name.
         */
        protected String m_sChannelName;

        /**
         * The communications channel.
         */
        protected ManagedChannel m_channel;

        /**
         * The serializer.
         */
        protected Serializer m_serializer;

        /**
         * The serialization format.
         */
        protected String m_sSerializerFormat;

        /**
         * The {@link BeanManager}.
         */
        protected BeanManager m_beanManager;

        /**
         * The session configuration.
         */
        protected final Config f_config;
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

    // ----- constants ------------------------------------------------------

    /**
     * The configuration key used to obtain the Coherence configuration.
     */
    public static final String CFG_KEY_COHERENCE = "coherence";

    /**
     * The name for the default session and channel.
     */
    public static final String DEFAULT_NAME = "default";

    // ----- data members ---------------------------------------------------

    /**
     * The name of this session.
     */
    private final String f_sName;

    /**
     * The gRPC {@link Channel} used by this session.
     */
    private final ManagedChannel f_channel;

    /**
     * The {@link Serializer} used by this session.
     */
    private final Serializer f_serializer;

    /**
     * The name of the {@link Serializer} format.
     */
    private final String f_sSerializerFormat;

    /**
     * The {@link BeanManager} used by this session when running in a CDI environment.
     */
    private final BeanManager f_beanManager;

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
     * A list of {@link DeactivationListener} instances to be notified if this
     * session is closed.
     */
    protected final List<DeactivationListener<GrpcRemoteSession>> f_listDeactivationListeners = new ArrayList<>();

    /**
     * The optional client tracer to use.
     */
    protected final Optional<ClientTracingInterceptor> f_tracing;

    /**
     * A flag indicating whether this session has been closed.
     */
    protected boolean m_fClosed;
    }
