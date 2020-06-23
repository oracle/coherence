/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.oracle.coherence.cdi.CdiMapListener;
import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.Scope;
import com.tangosol.net.Session;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;

import javax.enterprise.inject.spi.Extension;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A gRPC client side CDI {@link Extension}.
 *
 * @author Jonathan Knight  2020.01.10
 * @since 20.06
 */
public class CoherenceClientExtension
        implements Extension
    {

    /**
     * Initialize any {@link com.tangosol.util.MapEvent} observers for specific caches (annotated with
     * {@link com.oracle.coherence.cdi.events.MapName} after CDI {@link ApplicationScoped} context has
     * started.
     *
     * @param event        the CDI context initialized event
     * @param beanManager  the CDI {@link BeanManager}
     * @param extension    the {@link CoherenceExtension}
     */
    synchronized void initMapEventObservers(@Observes
                                            @Initialized(ApplicationScoped.class) Object event,
                                            BeanManager        beanManager,
                                            CoherenceExtension extension)
        {
        // If we're in an environment where the server is starting too then
        // we need to wait for it to start so we wait for the future to complete
        checkStart().thenRun(() ->
            {
            Instance<Object>                  instance  = beanManager.createInstance();
            Map<String, Map<String, Session>> sessions  = new HashMap<>();
            Set<CdiMapListener<?, ?>>         listeners = extension.getRemoteMapListeners();

            for (CdiMapListener<?, ?> listener : listeners)
                {
                String               sSession = listener.getRemoteSessionName();
                String               sScope   = listener.getScopeName();
                Map<String, Session> map      = sessions.computeIfAbsent(sScope, k -> new HashMap<>());
                Session              session  = map.computeIfAbsent(sSession,
                        k -> instance.select(Session.class,
                                             Remote.Literal.of(sSession),
                                             Scope.Literal.of(sScope)).get());

                session.getCache(listener.getCacheName());
                }
            });
        }

    /**
     * Clean-up any client side resources.
     * <p>
     * This observer has a priority so that it executes before other
     * observers, such as the gRPC server shutdown.
     *
     * @param event  the {@link BeforeShutdown} event (ignored)
     */
    protected void shutdown(@Observes @Priority(1) BeforeShutdown event)
        {
        RemoteSessions.instance().shutdown();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * If the GrpcMpExtension is on the classpath then there is a server present
     * so return the {@link #s_futureStarted} future, otherwise return a completed
     * future.
     *
     * @return  the future that may wait for a gRPC server start.
     */
    private CompletableFuture<Void> checkStart()
        {
        try
            {
            Class.forName("io.helidon.microprofile.grpc.server.spi.GrpcMpExtension");
            // the gRPC server is on the classpath
            }
        catch (ClassNotFoundException e)
            {
            // the gRPC server is not on the classpath
            setStarted();
            }
        return s_futureStarted;
        }

    /**
     * Notify the extension that is can start.
     */
    static void setStarted()
        {
        s_futureStarted.complete(null);
        }

    // ----- data members ---------------------------------------------------

    /**
     *
     */
    private static final CompletableFuture<Void> s_futureStarted = new CompletableFuture<>();
    }
