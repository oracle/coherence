/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.util.Options;

import com.oracle.coherence.grpc.Requests;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.io.Serializer;

import com.tangosol.net.Session;
import com.tangosol.net.SessionProvider;

import com.tangosol.net.options.WithName;
import com.tangosol.net.options.WithScopeName;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;

import io.grpc.inprocess.InProcessChannelBuilder;

import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.util.GlobalTracer;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link SessionProvider} to provide {@link GrpcRemoteSession} instances.
 *
 * @author Jonathan Knight  2020.09.22
 * @since 20.06
 */
public class GrpcSessions
        implements SessionProvider
    {
    // ----- SessionProvider interface --------------------------------------

    @Override
    public GrpcRemoteSession createSession(Session.Option... options)
        {
        Options<Session.Option> sessionOptions = Options.from(Session.Option.class, options);

        if (!sessionOptions.contains(ChannelOption.class))
            {
            // the request is not for a gRPC session
            return null;
            }

        return ensureSession(sessionOptions);
        }

    /**
     * Close all {@link GrpcRemoteSession} instances created
     * by the {@link GrpcSessions} factory.
     */
    public synchronized void close()
        {
        f_sessions.shutdown();
        }

    // ----- public methods -------------------------------------------------

    /**
     * Obtain a {@link Session.Option} to specify the scope of the
     * required {@link GrpcRemoteSession}.
     * <p>
     * If the scope name parameter is {@code null} the default scope
     * name {@link Requests#DEFAULT_SCOPE} will be used.
     *
     * @param sScope  the scope name of the {@link GrpcRemoteSession}
     *
     * @return a {@link Session.Option} to specify the scope of the {@link GrpcRemoteSession}
     */
    public static Session.Option scope(String sScope)
        {
        return sScope == null ? WithScopeName.defaultScope() : WithScopeName.of(sScope);
        }

    /**
     * Obtain a {@link Session.Option} to specify that the required {@link GrpcRemoteSession} should
     * use an in-process gRPC channel.
     * <p>
     * The in-process channel will use the {@link Requests#DEFAULT_CHANNEL_NAME} for the in-process
     * channel name unless a name options is also specified.
     *
     * @return a {@link Session.Option} to specify the {@link GrpcRemoteSession} should use an
     *         in-process channel
     */
    public static Session.Option inProcessChannel()
        {
        return channel(s_inProcessChannel);
        }

    /**
     * Obtain a {@link Session.Option} to specify an optional name for the session.
     * <p>
     * If the name parameter is {@code null} the default name {@link Requests#DEFAULT_SESSION_NAME}
     * will be used.
     *
     * @param sName the name to use for the {@link GrpcRemoteSession}
     *
     * @return  a {@link Session.Option} to specify an optional name for the session
     */
    public static Session.Option named(String sName)
        {
        return WithName.of(sName);
        }

    /**
     * Obtain a {@link Session.Option} to specify the {@link Channel} of the required {@link GrpcRemoteSession}.
     * <p>
     * A channel of {@code null} will use the default in-process channel connecting using the session's name
     * as the channel name.
     *
     * @param channel  the gRPC {@link Channel} to use
     *
     * @return a {@link Session.Option} to specify the gRPC {@link Channel} of the {@link GrpcRemoteSession}
     *
     * @throws NullPointerException if the {@link Channel} is {@code null}
     */
    public static Session.Option channel(Channel channel)
        {
        return channel == null
                ? inProcessChannel()
                : new ChannelOption(channel);
        }

    /**
     * Obtain a {@link Session.Option} to specify that the required {@link GrpcRemoteSession} should
     * use an gRPC channel that connects to a gRPC server on localhost using plain text transport.
     * <p>
     * This is useful in a test environment where a test server is running on localhost.
     * <p>
     * The port used will be taken from the value of the {@code coherence.grpc.port} system property,
     * or 1408 if the property is not set.
     *
     * @return a {@link Session.Option} to specify the {@link GrpcRemoteSession} should use a channel
     *         that connects to localhost
     */
    public static Session.Option localChannel()
        {
        int     port    = Config.getInteger(Requests.PROP_PORT, Requests.DEFAULT_PORT);
        Channel channel = ManagedChannelBuilder.forAddress("localhost", port)
                                               .usePlaintext()
                                               .build();
        return channel(channel);
        }

    /**
     * Obtain a {@link Session.Option} to specify the {@link Serializer} of the required {@link GrpcRemoteSession}.
     * <p>
     * A serializer of {@code null} will use the default {@link Serializer}, which will be Java unless
     * POF has been enabled with the {@code coherence.pof.enabled} System property.
     *
     * @param serializer  the {@link Serializer} the session should use
     *
     * @return a {@link Session.Option} to specify the {@link Serializer} of the {@link GrpcRemoteSession}
     */
    public static Session.Option serializer(Serializer serializer)
        {
        String sName = serializer == null ? null : serializer.getName();
        return serializer(serializer, sName);
        }

    /**
     * Obtain a {@link Session.Option} to specify the {@link Serializer} and serialization format name
     * of the required {@link GrpcRemoteSession}.
     *
     * @param serializer  the {@link Serializer} the session should use
     * @param sFormat     the serialization format name
     *
     * @return a {@link Session.Option} to specify the {@link Serializer} and serialization
     *         format name of the {@link GrpcRemoteSession}
     */
    public static Session.Option serializer(Serializer serializer, String sFormat)
        {
        return new SerializerOption(serializer, sFormat);
        }

    /**
     * Obtain a {@link Session.Option} to specify the serialization
     * format of the required {@link GrpcRemoteSession}.
     *
     * @param sFormat  the serialization format name
     *
     * @return a {@link Session.Option} to specify the serialization
     *         format of the {@link GrpcRemoteSession}
     */
    public static Session.Option serializerFormat(String sFormat)
        {
        return serializer(null, sFormat);
        }

    /**
     * Obtain a {@link Session.Option} to enable or disable tracing for the session.
     *
     * @param enabled  {@code true} to enable tracing
     *
     * @return a {@link Session.Option} to enable or disable tracing
     */
    public static Session.Option tracing(boolean enabled)
        {
        return new TracingOption(enabled);
        }

    /**
     * Obtain a {@link Session.Option} setting the tracing interceptor to use.
     *
     * @param interceptor  the {@link TracingClientInterceptor} to use
     *
     * @return a {@link Session.Option} setting the tracing interceptor to use.
     */
    public static Session.Option tracing(ClientInterceptor interceptor)
        {
        return new TracingOption(interceptor);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain a {@link GrpcRemoteSession}, creating a new instance if required.
     * <p>
     * Requests for a session with the same {@link Channel}, scope name, serialization
     * format and {@link Serializer} will return the same {@link GrpcRemoteSession}.
     *
     * @param options the options to use to build the {@link GrpcRemoteSession}
     *
     * @return a {@link GrpcRemoteSession} instance
     */
    private synchronized GrpcRemoteSession ensureSession(Options<Session.Option> options)
        {
        Channel          channel          = options.get(ChannelOption.class).getChannel();
        String           sName            = options.get(WithName.class, WithName.defaultName()).getName();
        String           sScope           = options.get(WithScopeName.class, WithScopeName.defaultScope()).getScopeName();
        SerializerOption serializerOption = options.get(SerializerOption.class, SerializerOption.DEFAULT);
        Serializer       serializer       = serializerOption.getSerializer();
        String           sFormat          = serializerOption.getFormat();

        GrpcRemoteSession.Builder builder = GrpcRemoteSession.builder(channel)
                .setName(sName)
                .setScope(sScope)
                .serializer(serializer, sFormat);

        options.get(TracingOption.class, TracingOption.DEFAULT)
                .getInterceptor()
                .ifPresent(builder::tracing);

        return f_sessions.get(sName)
                         .get(channel)
                         .get(builder.getScope())
                         .get(builder.ensureSerializerFormat())
                         .get(builder.ensureSerializer(), builder);
        }

    // ----- inner class: ChannelOption -------------------------------------

    /**
     * A {@link Session.Option} to use to specify the gRPC {@link Channel}.
     */
    protected static class ChannelOption
            implements Session.Option
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link ChannelOption} with the specified {@link Channel}.
         *
         * @param channel  the {@link Channel} that should be used by
         *                 the {@link GrpcRemoteSession}
         *
         * @throws NullPointerException if the {@link Channel} is {@code null}
         */
        protected ChannelOption(Channel channel)
            {
            f_channel = Objects.requireNonNull(channel);
            }

        /**
         * Return the gRPC {@link Channel}.
         *
         * @return the gRPC {@link Channel}
         */
        protected Channel getChannel()
            {
            return f_channel;
            }

        // ----- data members -----------------------------------------------

        /**
         * The gRPC {@link Channel}.
         */
        protected final Channel f_channel;
        }

    // ----- inner class: ChannelOption -------------------------------------

    /**
     * A {@link Session.Option} to use to enable or disable tracing.
     */
    protected static class TracingOption
            implements Session.Option
        {
        // ----- constructors -----------------------------------------------

        protected TracingOption(boolean fEnabled)
            {
            this(fEnabled, null);
            }

        protected TracingOption(ClientInterceptor interceptor)
            {
            this(interceptor != null, interceptor);
            }

        protected TracingOption(boolean fEnabled, ClientInterceptor interceptor)
            {
            f_fEnabled    = fEnabled;
            f_interceptor = interceptor;
            }

        /**
         * Return whether tracing should be enabled.
         *
         * @return {@code true} if tracing should be enabled
         */
        protected boolean isEnabled()
            {
            return f_fEnabled;
            }

        /**
         * Return the {@link ClientInterceptor} to use.
         *
         * @return the {@link ClientInterceptor} to use
         */
        public Optional<ClientInterceptor> getInterceptor()
            {
            if (f_interceptor != null)
                {
                return Optional.of(f_interceptor);
                }
            else if (f_fEnabled)
                {
                Tracer tracer = GlobalTracer.get();
                TracingClientInterceptor interceptor = TracingClientInterceptor.newBuilder()
                        .withTracer(tracer)
                        .build();
                return Optional.of(interceptor);
                }
            else
                {
                return Optional.empty();
                }
            }

        // ----- constants --------------------------------------------------

        /**
         * Default tracing option.
         */
        protected static final TracingOption DEFAULT = new TracingOption(TracingHelper.isEnabled());

        // ----- data members -----------------------------------------------

        /**
         * The tracing flag.
         */
        protected final boolean f_fEnabled;

        /**
         * The {@link ClientInterceptor} to use.
         */
        protected final ClientInterceptor f_interceptor;
        }

    // ----- inner class: ChannelOption -------------------------------------

    /**
     * A {@link Session.Option} to use to specify the {@link Serializer}
     * and format name to use to serialize request and response payloads.
     */
    protected static class SerializerOption
            implements Session.Option
        {
        // ----- constructors -----------------------------------------------

        protected SerializerOption(Serializer serializer, String sFormat)
            {
            f_serializer = serializer;
            f_sFormat    = sFormat;
            }

        /**
         * Return the {@link Serializer}.
         *
         * @return the gRPC {@link Serializer}
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
        protected String getFormat()
            {
            return f_sFormat;
            }

        // ----- constants --------------------------------------------------

        /**
         * Default serializer option.
         */
        protected static final SerializerOption DEFAULT = new SerializerOption(null, null);

        // ----- data members -----------------------------------------------

        /**
         * The {@link Serializer}.
         */
        protected final Serializer f_serializer;

        /**
         * The serialization format.
         */
        protected final String f_sFormat;
        }

    // ----- inner class: SessionsByName ------------------------------------

    private static class SessionsByName
        {
        SessionsByChannel get(String sName)
            {
            return f_map.computeIfAbsent(sName, k -> new SessionsByChannel());
            }

        void shutdown()
            {
            f_map.values().forEach(SessionsByChannel::shutdown);
            }

        // ----- data members -----------------------------------------------

        private final ConcurrentHashMap<String, SessionsByChannel> f_map = new ConcurrentHashMap<>();
        }

    // ----- inner class: SessionsByChannel ------------------------------------

    private static class SessionsByChannel
        {
        SessionsByScope get(Channel channel)
            {
            return f_map.computeIfAbsent(channel, k -> new SessionsByScope());
            }

        void shutdown()
            {
            f_map.values().forEach(SessionsByScope::shutdown);
            }

        // ----- data members -----------------------------------------------

        private final ConcurrentHashMap<Channel, SessionsByScope> f_map = new ConcurrentHashMap<>();
        }

    // ----- inner class: SessionsByScope ------------------------------------

    private static class SessionsByScope
        {
        SessionsBySerializerFormat get(String sScope)
            {
            return f_map.computeIfAbsent(sScope, k -> new SessionsBySerializerFormat());
            }

        void shutdown()
            {
            f_map.values().forEach(SessionsBySerializerFormat::shutdown);
            }

        // ----- data members -----------------------------------------------

        private final ConcurrentHashMap<String, SessionsBySerializerFormat> f_map = new ConcurrentHashMap<>();
        }

    // ----- inner class: SessionsBySerializerFormat ------------------------------------

    private static class SessionsBySerializerFormat
        {
        SessionsBySerializer get(String sFormat)
            {
            return f_map.computeIfAbsent(sFormat, k -> new SessionsBySerializer());
            }

        void shutdown()
            {
            f_map.values().forEach(SessionsBySerializer::shutdown);
            }

        // ----- data members -----------------------------------------------

        private final ConcurrentHashMap<String, SessionsBySerializer> f_map = new ConcurrentHashMap<>();
        }

    // ----- inner class: SessionsByChannel ------------------------------------

    private static class SessionsBySerializer
        {
        GrpcRemoteSession get(Serializer serializer, GrpcRemoteSession.Builder builder)
            {
            return f_map.compute(serializer, (key, current) ->
                        {
                        if (current != null && !current.isClosed())
                            {
                            return current;
                            }
                        else
                            {
                            return builder.build();
                            }
                        });
            }

        void shutdown()
            {
            f_map.values().forEach(session ->
                {
                try
                    {
                    session.close();
                    }
                catch (Throwable t)
                    {
                    Logger.err(t);
                    }
                });
            f_map.clear();
            }

        // ----- data members -----------------------------------------------

        private final ConcurrentHashMap<Serializer, GrpcRemoteSession> f_map = new ConcurrentHashMap<>();
        }

    // ----- data members ---------------------------------------------------

    /**
     * A holder for the Sessions.
     */
    private final SessionsByName f_sessions = new SessionsByName();

    /**
     * The default in-process {@link Channel}.
     */
    private static final Channel s_inProcessChannel = InProcessChannelBuilder.forName(Requests.DEFAULT_CHANNEL_NAME)
            .usePlaintext()
            .build();
    }
