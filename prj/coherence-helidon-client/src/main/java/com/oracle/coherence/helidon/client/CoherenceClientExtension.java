/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.client;

import com.oracle.coherence.cdi.CdiMapListenerManager;
import com.oracle.coherence.cdi.events.AnnotatedMapListener;

import com.oracle.coherence.cdi.SessionName;

import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.net.Session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;

import jakarta.enterprise.event.Observes;

import jakarta.enterprise.inject.Instance;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A gRPC client side CDI {@link Extension}.
 *
 * @author Jonathan Knight  2020.01.10
 * @since 20.06
 */
public class CoherenceClientExtension
        implements Extension
    {
    public CoherenceClientExtension()
        {
        initStart();
        }

    /**
     * Initialize any {@link com.tangosol.util.MapEvent} observers for specific caches (annotated with
     * {@link MapName} after CDI {@link ApplicationScoped} context has
     * started.
     *
     * @param event        the CDI context initialized event
     * @param beanManager  the CDI {@link BeanManager}
     * @param manager      the {@link CdiMapListenerManager}
     */
    synchronized void initMapEventObservers(@Observes
                                            @Initialized(ApplicationScoped.class) Object event,
                                            BeanManager                                  beanManager,
                                            CdiMapListenerManager                        manager)
        {
        // If we're in an environment where the server is starting too then
        // we need to wait for it to start so we wait for the future to complete
        checkStart().thenRun(() ->
            {
            Instance<Object>          instance  = beanManager.createInstance();
            Map<String, Session>      sessions  = new HashMap<>();

            // Ensure caches required for CDI MapEvent observer methods
            Collection<AnnotatedMapListener<?, ?>> listeners = manager.getNonWildcardMapListeners();
            for (AnnotatedMapListener<?, ?> listener : listeners)
                {
                String               sSession = listener.getSessionName();
                Session              session  = sessions.computeIfAbsent(sSession,
                                                       k -> instance.select(Session.class,
                                                                            SessionName.Literal.of(sSession)).get());

                session.getCache(listener.getCacheName());
                }
            });
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    private void initStart()
        {
        if (m_fInit)
            {
            synchronized (this)
                {
                m_fInit = false;
                try
                    {
                    Class<?>              cls    = Class.forName("com.oracle.coherence.grpc.proxy.GrpcServerController");
                    Field                 field  = cls.getField("INSTANCE");
                    Method                method = cls.getMethod("whenStarted");
                    CompletionStage<Void> stage  = (CompletionStage<Void>) method.invoke(field.get(null));
                    stage.thenRun(CoherenceClientExtension::setStarted);
                    }
                catch (ClassNotFoundException e)
                    {
                    //
                    setStarted();
                    }
                catch (Exception e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                }
            }
        }

    /**
     * If the GrpcMpExtension is on the classpath then there is a server present
     * so return the {@link #s_futureStarted} future, otherwise return a completed
     * future.
     *
     * @return  the future that may wait for a gRPC server start.
     */
    private CompletableFuture<Void> checkStart()
        {
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

    private volatile boolean m_fInit = true;

    /**
     * A {@link CompletableFuture} that will be completed by either the gRPC server if it is present
     * or immediatley by the init method.
     */
    private static final CompletableFuture<Void> s_futureStarted = new CompletableFuture<>();
    }
