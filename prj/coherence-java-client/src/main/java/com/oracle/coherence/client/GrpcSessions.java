/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

import com.tangosol.internal.net.DefaultSessionProvider;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.util.RegistrationBehavior;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;

import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.util.GlobalTracer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
    public int getPriority()
        {
        // Make sure that client providers get to handle configurations ahead
        // of any default providers.
        return DefaultSessionProvider.DEFAULT_PRIORITY + 1;
        }

    @Override
    public Context createSession(SessionConfiguration configuration, Context context)
        {
        Context grpcContext = new DefaultContext(context.getMode(), DefaultProvider.INSTANCE, context.getInterceptors());
        for (SessionProvider provider : ensureProviders())
            {
            Context result = provider.createSession(configuration, grpcContext);
            if (result.isComplete())
                {
                return result;
                }
            }

        // none of the providers created a session, try the default
        return ensureSession(configuration, context);
        }

    /**
     * This method will throw {@link UnsupportedOperationException}.
     *
     * @param options  the {@link Session.Option}s for creating the {@link Session}
     *
     * @return this method will throw {@link UnsupportedOperationException}.
     */
    @Override
    @Deprecated
    public Session createSession(Session.Option... options)
        {
        throw new UnsupportedOperationException("Cannot create a gRPC session using optiona");
        }

    /**
     * Close all {@link GrpcRemoteSession} instances created
     * by the {@link GrpcSessions} factory.
     */
    public synchronized void close()
        {
        f_sessions.shutdown();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain a {@link GrpcRemoteSession}, creating a new instance if required.
     * <p>
     * Requests for a session with the same {@link Channel}, scope name, serialization
     * format and {@link Serializer} will return the same {@link GrpcRemoteSession}.
     *
     * @param configuration the {@link SessionConfiguration} to configure the session
     * @param context       the {@link com.tangosol.net.SessionProvider.Context} to use
     *
     * @return the result context
     */
    synchronized Context ensureSession(SessionConfiguration configuration, Context context)
        {
        if (configuration instanceof GrpcSessionConfiguration)
            {
            GrpcSessionConfiguration grpcConfig  = (GrpcSessionConfiguration) configuration;
            Channel                  channel     = Objects.requireNonNull(grpcConfig.getChannel());
            String                   sName       = grpcConfig.getName();
            String                   sScope      = grpcConfig.getScopeName();
            ClientInterceptor        interceptor = grpcConfig.enableTracing() ? createTracingInterceptor() : null;
            String                   sFormat     = ensureSerializerFormat(grpcConfig.getFormat().orElse(null));
            Serializer               serializer  = grpcConfig.getSerializer().orElseGet(() -> ensureSerializer(sFormat));

            Supplier<GrpcRemoteSession> supplier = () ->
                    new GrpcRemoteSession(channel, sName, sScope, serializer,
                                          sFormat, interceptor, context.getInterceptors());

            GrpcRemoteSession session = f_sessions.get(sName)
                    .get(channel)
                    .get(sScope)
                    .get(sFormat)
                    .get(serializer, supplier);

            if (session != null)
                {
                session.activate(null);
                return context.complete(session);
                }
            }
        return context;
        }

    /**
     * Ensure the serialization format is initialized and returned.
     *
     * @param sFormat the configured format
     *
     * @return the serialization format
     */
    private String ensureSerializerFormat(String sFormat)
        {
        if (sFormat != null && !sFormat.isEmpty())
            {
            return sFormat;
            }

        return Config.getBoolean("coherence.pof.enabled") ? "pof" : "java";
        }

    /**
     * Ensure the {@link Serializer} is initialized and returned.
     *
     * @param sFormat  the configured format
     *
     * @return the {@link Serializer}
     */
    private Serializer ensureSerializer(String sFormat)
        {
        String sSerializerName = ensureSerializerFormat(sFormat);

        if (sSerializerName.equals("pof"))
            {
            return DEFAULT_POF_SERIALIZER;
            }
        else if (sSerializerName.equals("java"))
            {
            return DEFAULT_JAVA_SERIALIZER;
            }
        else
            {
            throw new IllegalArgumentException("Unknown serializer format " + sSerializerName
                    + " and no Serializer has been specified");
            }
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
     * Obtain the list of discovered {@link GrpcSessionProvider} instances.
     *
     * @return the list of discovered {@link GrpcSessionProvider} instances
     */
    private synchronized List<GrpcSessionProvider> ensureProviders()
        {
        if (m_listProvider == null)
            {
            List<GrpcSessionProvider>          list   = new ArrayList<>();
            ServiceLoader<GrpcSessionProvider> loader = ServiceLoader.load(GrpcSessionProvider.class);
            for (GrpcSessionProvider provider : loader)
                {
                list.add(provider);
                }
            list.sort(Comparator.reverseOrder());
            m_listProvider = list;
            }
        return m_listProvider;
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
        GrpcRemoteSession get(Serializer serializer, Supplier<GrpcRemoteSession> builder)
            {
            return f_map.compute(serializer, (key, current) ->
                        {
                        if (current != null && !current.isClosed())
                            {
                            return current;
                            }
                        else
                            {
                            return builder.get();
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

    // ----- inner class: DefaultProvider -----------------------------------

    /**
     * The default gRPC session provider that does not delegate to any other provider.
     */
    private static class DefaultProvider
            extends GrpcSessions
        {
        @Override
        public Context createSession(SessionConfiguration configuration, Context context)
            {
            if (configuration instanceof GrpcSessionConfiguration)
                {
                return ensureSession(configuration, context);
                }
            return context;
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton {@link DefaultProvider} instance.
         */
        static DefaultProvider INSTANCE = new DefaultProvider();
        }

    // ----- constants ------------------------------------------------------

    private static final Serializer DEFAULT_JAVA_SERIALIZER = new DefaultSerializer();

    private static final Serializer DEFAULT_POF_SERIALIZER = new ConfigurablePofContext();

    // ----- data members ---------------------------------------------------

    /**
     * A holder for the Sessions.
     */
    private final SessionsByName f_sessions = new SessionsByName();

    /**
     * The list of {@link GrpcSessionProvider} instances to use to provide gRPC sessions.
     */
    private List<GrpcSessionProvider> m_listProvider;
    }
