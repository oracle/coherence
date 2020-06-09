/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cache.grpc.client;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.common.util.Options;

import com.tangosol.net.Session;
import com.tangosol.net.SessionProvider;

import io.helidon.config.Config;

import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;

import javax.inject.Inject;

/**
 * A CDI producer for {@link GrpcRemoteSession} instances.
 *
 * @author Jonathan Knight  2019.11.28
 * @since 14.1.2
 */
@ApplicationScoped
public class RemoteSessions
        implements SessionProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a non-CDI {@link RemoteSessions}.
     */
    protected RemoteSessions()
        {
        this(null, Config.create());
        }

    /**
     * Create a {@link RemoteSessions}.
     *
     * @param beanManager the {@link javax.enterprise.inject.spi.BeanManager} to use
     */
    @Inject
    protected RemoteSessions(BeanManager beanManager, Config config)
        {
        this.f_beanManager = beanManager;
        this.f_config      = config == null ? Config.empty() : config;
        }

    // ----- SessionProvider interface --------------------------------------

    @Override
    public GrpcRemoteSession createSession(Session.Option... options)
        {
        Options<Session.Option> sessionOptions = Options.from(Session.Option.class, options);
        String name = sessionOptions.get(NameOption.class, NameOption.DEFAULT).getName();
        return ensureSession(name);
        }

    // ----- public methods -------------------------------------------------

    /**
     * Obtain the singleton {@link RemoteSessions} instance.
     *
     * @return the singleton {@link RemoteSessions} instance
     */
    public static synchronized RemoteSessions instance()
        {
        if (s_instance == null)
            {
            CDI<Object> current = CDI.current();
            if (current == null)
                {
                s_instance = new RemoteSessions();
                }
            else
                {
                s_instance = CDI.current()
                        .getBeanManager()
                        .createInstance()
                        .select(RemoteSessions.class)
                        .get();
                }
            }
        return s_instance;
        }

    /**
     * Close all {@link GrpcRemoteSession} instances created
     * by the {@link RemoteSessions} factory.
     */
    @PreDestroy
    public void shutdown()
        {
        Iterator<Map.Entry<String, GrpcRemoteSession>> iterator = f_mapSessions.entrySet().iterator();
        while (iterator.hasNext())
            {
            Map.Entry<String, GrpcRemoteSession> entry = iterator.next();
            try
                {
                entry.getValue().close();
                }
            catch (Throwable t)
                {
                t.printStackTrace();
                }
            iterator.remove();
            }
        }

    /**
     * Obtain a {@link Session.Option} to specify the name of the required {@link GrpcRemoteSession}.
     *
     * @param name the name of the {@link GrpcRemoteSession}
     *
     * @return a {@link Session.Option} to specify the name of the {@link GrpcRemoteSession}
     */
    public static Session.Option name(String name)
        {
        return new NameOption(name);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Produces a remote {@link GrpcRemoteSession}.
     * <p>
     * If the value of the name qualifier is blank or empty String the default
     * {@link Session} will be returned.
     *
     * @param injectionPoint the {@link InjectionPoint} that the cache factory it to be injected into
     *
     * @return the named {@link GrpcRemoteSession}
     */
    @Produces
    @Remote
    @Name("")
    protected GrpcRemoteSession getSession(InjectionPoint injectionPoint)
        {
        String sName = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(Name.class))
                .map(q -> ((Name) q).value())
                .findFirst()
                .orElse(GrpcRemoteSession.DEFAULT_NAME);

        return ensureSession(sName);
        }

    /**
     * Obtain a {@link GrpcRemoteSession}, creating a new instance if required.
     *
     * @param name the name of the session
     *
     * @return a {@link GrpcRemoteSession} instance.
     */
    GrpcRemoteSession ensureSession(String name)
        {
        GrpcRemoteSession session = f_mapSessions.computeIfAbsent(name, k -> GrpcRemoteSession.builder(f_config)
                .name(name)
                .beanManager(f_beanManager)
                .build());

        if (session.isClosed())
            {
            // if the cached session has been closed then create a new session
            f_mapSessions.remove(name);
            return ensureSession(name);
            }

        return session;
        }

    // ----- inner class: NameOption ----------------------------------------

    /**
     * A {@link Session.Option} to use to specify the gRPC
     * channel name for a session.
     */
    protected static class NameOption
            implements Session.Option
        {
        // ----- constructors -----------------------------------------------

        protected NameOption(String sName)
            {
            this.f_sName = sName;
            }

        /**
         * Return the name, or {@value GrpcRemoteSession#DEFAULT_NAME} if {@code null}.
         *
         * @return the name, or {@value GrpcRemoteSession#DEFAULT_NAME} if {@code null}.
         */
        protected String getName()
            {
            return f_sName == null || f_sName.isEmpty() ? DEFAULT.getName() : f_sName;
            }

        // ----- constants --------------------------------------------------

        /**
         * Default session name.
         */
        protected static final NameOption DEFAULT = new NameOption(GrpcRemoteSession.DEFAULT_NAME);

        // ----- data members -----------------------------------------------

        /**
         * The remote session name.
         */
        protected final String f_sName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A map of sessions stored by name.
     */
    protected final Map<String, GrpcRemoteSession> f_mapSessions = new ConcurrentHashMap<>();

    /**
     * The CDI {@link javax.enterprise.inject.spi.BeanManager}.
     */
    protected final BeanManager f_beanManager;

    /**
     * The default {@link Config} to use.
     */
    protected final Config f_config;

    /**
     * The singleton {@link RemoteSessions}.
     */
    protected static RemoteSessions s_instance;
    }
