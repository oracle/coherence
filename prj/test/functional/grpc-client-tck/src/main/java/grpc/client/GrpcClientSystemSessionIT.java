/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.Session;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.SessionLifecycleEvent;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.annotation.SessionLifecycleEvents;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

/**
 * Client side system session tests.
 *
 * @author Jonathan Knight  2020.12.16
 */
@SuppressWarnings("resource")
public class GrpcClientSystemSessionIT
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.cluster", "GrpcClientSystemSessionIT");
        }

    @AfterEach
    void cleanup()
        {
        Coherence.closeAll();
        CacheFactory.getCacheFactoryBuilder().releaseAll(null);
        CacheFactory.shutdown();
        }

    @Test
    void shouldHaveSystemSessionOnClusterMember() throws Exception
        {
        Listener               listener      = new Listener();
        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                        .withEventInterceptors(listener)
                                                        .build();
        Coherence              coherence     = Coherence.clusterMember(configuration);

        assertThat(coherence.getMode(), is(Coherence.Mode.ClusterMember));

        coherence.start().get(5, TimeUnit.MINUTES);

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(true));
        Session session = coherence.getSession(Coherence.SYSTEM_SESSION);
        assertThat(session, is(instanceOf(ConfigurableCacheFactorySession.class)));

        assertThat(listener.f_events.size(), is(2));
        List<SessionLifecycleEvent.Type> systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));
        }

    @Test
    void shouldHaveSystemSessionOnClient() throws Exception
        {
        Listener               listener      = new Listener();
        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                        .withEventInterceptors(listener)
                                                        .build();
        Coherence              coherence     = Coherence.create(configuration, Coherence.Mode.Grpc);

        assertThat(coherence.getMode(), is(Coherence.Mode.Grpc));

        coherence.start().get(5, TimeUnit.MINUTES);

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(true));
        Session session = coherence.getSession(Coherence.SYSTEM_SESSION);
        assertThat(session, is(instanceOf(ConfigurableCacheFactorySession.class)));

        assertThat(Coherence.SYSTEM_SESSION, is(in(listener.f_events.keySet())));
        List<SessionLifecycleEvent.Type> systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));
        }


    @Interceptor
    @SessionLifecycleEvents
    public static class Listener
            implements EventInterceptor<SessionLifecycleEvent>
        {
        @Override
        public void onEvent(SessionLifecycleEvent event)
            {
            String                           sName = event.getSession().getName();
            List<SessionLifecycleEvent.Type> list  = f_events.computeIfAbsent(sName, k -> new ArrayList<>());
            list.add(event.getType());
            }

        final Map<String, List<SessionLifecycleEvent.Type>> f_events = new ConcurrentHashMap<>();
        }
    }
