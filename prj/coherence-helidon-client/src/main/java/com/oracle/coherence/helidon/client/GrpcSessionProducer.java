/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.helidon.client;

import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.RemoteMapLifecycleEvent;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SerializerProducer;

import com.oracle.coherence.client.GrpcRemoteSession;
import com.oracle.coherence.client.GrpcSessions;

import com.oracle.coherence.grpc.Requests;

import com.tangosol.io.Serializer;

import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.Base;

import io.grpc.Channel;

import io.helidon.config.Config;
import io.helidon.grpc.client.ClientTracingInterceptor;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import javax.inject.Inject;

/**
 * A CDI producer for {@link GrpcRemoteSession} instances.
 *
 * @author Jonathan Knight  2019.11.28
 * @since 20.06
 */
@ApplicationScoped
public class GrpcSessionProducer
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link GrpcSessionProducer}.
     *
     * @param beanManager the {@link javax.enterprise.inject.spi.BeanManager} to use
     */
    @Inject
    protected GrpcSessionProducer(BeanManager beanManager,
                                  Config      config)
        {
        f_beanManager = beanManager;
        f_config      = config == null ? Config.empty() : config;

        Instance<RemoteMapLifecycleEvent.Dispatcher> instance = null;
        if (beanManager != null)
            {
            Instance<Object> in = beanManager.createInstance();
            instance = in == null ? null : in.select(RemoteMapLifecycleEvent.Dispatcher.class);
            }

        RemoteMapLifecycleEvent.Dispatcher dispatcher;
        if (instance != null && instance.isResolvable())
            {
            dispatcher = instance.get();
            }
        else
            {
            dispatcher = RemoteMapLifecycleEvent.Dispatcher.nullImplementation();
            }

        f_remoteMapLifecycleListener = new RemoteMapLifecycleListener(dispatcher);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Produces a remote {@link GrpcRemoteSession}.
     * <p>
     * If the value of the scope qualifier is blank or empty String the default
     * {@link Session} will be returned.
     *
     * @param injectionPoint the {@link InjectionPoint} that the cache factory it to be injected into
     *
     * @return the named {@link GrpcRemoteSession}
     */
    @Produces
    @Remote
    @Scope
    protected GrpcRemoteSession getSession(InjectionPoint injectionPoint)
        {
        String sName = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(Remote.class))
                .map(q -> ((Remote) q).value().trim())
                .findFirst()
                .orElse(Remote.DEFAULT_NAME);

        String sScope = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(Scope.class))
                .map(q -> ((Scope) q).value().trim())
                .findFirst()
                .orElse(Scope.DEFAULT);

        return ensureSession(sName, sScope);
        }

    /**
     * Obtain a {@link GrpcRemoteSession}, creating a new instance if required.
     *
     *
     * @param sScope the scope name of the session
     *
     * @return a {@link GrpcRemoteSession} instance.
     */
    GrpcRemoteSession ensureSession(String sName, String sScope)
        {
        Config            config     = sessionConfig(sName);
        Channel           channel    = ensureChannel(config);
        String            sFormat    = ensureSerializerFormat(config);
        Serializer        serializer = ensureSerializer(sFormat);
        Session.Option    optTrace   = ensureTracing(config)
                                                .map(GrpcSessions::tracing)
                                                .orElse(GrpcSessions.tracing(false));

        GrpcRemoteSession session = (GrpcRemoteSession) Session.create(GrpcSessions.channel(channel),
                                                                       GrpcSessions.scope(sScope),
                                                                       GrpcSessions.named(sName),
                                                                       GrpcSessions.serializer(serializer, sFormat),
                                                                       optTrace);

        session.addMapLifecycleListener(f_remoteMapLifecycleListener);
        return session;
        }

    /**
     * Return a named {@link Session}'s {@link Config configuration}.
     *
     * @param sName the name of the session
     *
     * @return the named {@link Session}'s {@link Config configuration}
     */
    protected Config sessionConfig(String sName)
        {
        Config cfgSessions = f_config.get(CFG_KEY_COHERENCE).get("sessions");

        if (!cfgSessions.exists())
            {
            return Config.empty();
            }

        return cfgSessions.get(sName);
        }

    protected Channel ensureChannel(Config config)
        {
        String sChannelName = config.get("channel")
                                .asString()
                                .orElse(Requests.DEFAULT_CHANNEL_NAME);

        Instance<Channel> instance;

        // try named Channel producer first
        instance = f_beanManager.createInstance()
                .select(Channel.class, NamedLiteral.of(sChannelName));

        if (!instance.isResolvable())
            {
            // try Helidon
            try
                {
                instance = f_beanManager.createInstance()
                        .select(Channel.class, GrpcChannelLiteral.of(sChannelName));
                }
            catch (ClassNotFoundException ignored)
                {
                // Helidon not on classpath
                instance = null;
                }
            }

        if (instance != null && instance.isResolvable())
            {
            return instance.get();
            }
        else if (instance != null && instance.isAmbiguous())
            {
            throw new IllegalStateException("Cannot discover Channel for name '" + sChannelName
                                            + " bean lookup results in ambiguous bean instances");
            }
        else
            {
            throw new IllegalStateException("Cannot discover Channel for name '" + sChannelName
                                            + " name is unresolvable");
            }
        }

    /**
     * Ensure the serialization format is initialized and returned.
     *
     * @return the serialization format
     */
    protected String ensureSerializerFormat(Config sessionConfig)
        {
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
        return f_beanManager.createInstance()
                            .select(SerializerProducer.class)
                            .get()
                            .getNamedSerializer(format, Base.getContextClassLoader());
        }

    /**
     * Return the {@link ClientTracingInterceptor}, if any.
     *
     * @return the {@link ClientTracingInterceptor}, if any
     */
    protected Optional<ClientTracingInterceptor> ensureTracing(Config sessionConfig)
        {
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

    // ----- inner class RemoteMapLifecycleListener -------------------------

    private static class RemoteMapLifecycleListener
            implements GrpcRemoteSession.RemoteMapLifecycleListener
        {
        private RemoteMapLifecycleListener(RemoteMapLifecycleEvent.Dispatcher dispatcher)
            {
            f_dispatcher = dispatcher;
            }

        @Override
        public void onCreate(NamedMap<?, ?> map, String sScope, Session session)
            {
            f_dispatcher.dispatch(map, sScope, ((GrpcRemoteSession) session).getName(),
                                  null, RemoteMapLifecycleEvent.Type.Created);
            }

        @Override
        public void onDestroy(NamedMap<?, ?> map, String sScope, Session session)
            {
            f_dispatcher.dispatch(map, sScope, ((GrpcRemoteSession) session).getName(),
                                  null, RemoteMapLifecycleEvent.Type.Destroyed);
            }

        @Override
        public void onTruncate(NamedMap<?, ?> map, String sScope, Session session)
            {
            f_dispatcher.dispatch(map, sScope, ((GrpcRemoteSession) session).getName(),
                                  null, RemoteMapLifecycleEvent.Type.Truncated);
            }

        // ----- data members ---------------------------------------------------

        private final RemoteMapLifecycleEvent.Dispatcher f_dispatcher;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The configuration key used to obtain the Coherence configuration.
     */
    public static final String CFG_KEY_COHERENCE = "coherence";

    // ----- data members ---------------------------------------------------

    /**
     * The CDI {@link javax.enterprise.inject.spi.BeanManager}.
     */
    protected final BeanManager f_beanManager;

    /**
     * The default {@link Config} to use.
     */
    protected final Config f_config;

    /**
     * The RemoteMapLifecycleListener to dispatch map lifecycle events.
     */
    private final RemoteMapLifecycleListener f_remoteMapLifecycleListener;
    }
