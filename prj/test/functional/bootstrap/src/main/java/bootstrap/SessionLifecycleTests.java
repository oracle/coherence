/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

/**
 * @author Jonathan Knight  2020.12.17
 */
public class SessionLifecycleTests
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "CoherenceBootstrapTests");
        }

    @AfterEach
    void cleanup()
        {
        Coherence.closeAll();
        CacheFactory.getCacheFactoryBuilder().releaseAll(null);
        CacheFactory.shutdown();
        }

    @Test
    void shouldReceiveSystemAndDefaultSessionEventsWhenClusterMember()
        {
        Listener               listener      = new Listener();
        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                        .withEventInterceptors(listener)
                                                        .build();
        Coherence              coherence     = Coherence.clusterMember(configuration);

        coherence.start().join();

        assertThat(listener.f_events.size(), is(2));

        List<SessionLifecycleEvent.Type> systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        List<SessionLifecycleEvent.Type> defaultEvents = listener.f_events.get(Coherence.DEFAULT_NAME);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        listener.f_events.clear();
        CompletableFuture<Void> future = coherence.whenClosed();

        // should only close the default session
        coherence.close();
        future.join();

        assertThat(listener.f_events.size(), is(1));

        defaultEvents = listener.f_events.get(Coherence.DEFAULT_NAME);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));

        // should close the System session
        listener.f_events.clear();
        Coherence.closeAll();

        assertThat(listener.f_events.size(), is(1));

        systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));
        }

    @Test
    void shouldReceiveOnlyDefaultSessionEventsWhenClient()
        {
        Listener               listener      = new Listener();
        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                        .withEventInterceptors(listener)
                                                        .build();
        Coherence              coherence     = Coherence.client(configuration);

        coherence.start().join();

        assertThat(listener.f_events.size(), is(1));

        List<SessionLifecycleEvent.Type> defaultEvents = listener.f_events.get(Coherence.DEFAULT_NAME);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        listener.f_events.clear();
        CompletableFuture<Void> future = coherence.whenClosed();

        // should only close the default session
        coherence.close();
        future.join();

        assertThat(listener.f_events.size(), is(1));

        defaultEvents = listener.f_events.get(Coherence.DEFAULT_NAME);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));

        // should close the System session
        listener.f_events.clear();
        Coherence.closeAll();

        assertThat(listener.f_events.size(), is(0));
        }

    @Test
    void shouldReceiveSystemAndConfiguredSessionEventsWhenClusterMember()
        {
        Listener               listener             = new Listener();
        String                 sessionName          = "Test";
        SessionConfiguration   sessionConfiguration = SessionConfiguration.builder()
                                                            .named(sessionName)
                                                            .withScopeName(sessionName)
                                                            .build();
        CoherenceConfiguration configuration        = CoherenceConfiguration.builder()
                                                            .withSession(sessionConfiguration)
                                                            .withEventInterceptors(listener)
                                                            .build();
        Coherence              coherence            = Coherence.clusterMember(configuration);

        coherence.start().join();

        assertThat(listener.f_events.size(), is(2));

        List<SessionLifecycleEvent.Type> systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        List<SessionLifecycleEvent.Type> defaultEvents = listener.f_events.get(sessionName);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        listener.f_events.clear();
        CompletableFuture<Void> future = coherence.whenClosed();

        // should only close the test session
        coherence.close();
        future.join();

        assertThat(listener.f_events.size(), is(1));

        defaultEvents = listener.f_events.get(sessionName);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));

        // should close the System session
        listener.f_events.clear();
        Coherence.closeAll();

        assertThat(listener.f_events.size(), is(1));

        systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));
        }

    @Test
    void shouldReceiveConfiguredSessionEventsWhenClient()
        {
        Listener               listener             = new Listener();
        String                 sessionName          = "Test";
        SessionConfiguration   sessionConfiguration = SessionConfiguration.builder()
                                                            .named(sessionName)
                                                            .withScopeName(sessionName)
                                                            .build();
        CoherenceConfiguration configuration        = CoherenceConfiguration.builder()
                                                            .withSession(sessionConfiguration)
                                                            .withEventInterceptors(listener)
                                                            .build();
        Coherence              coherence            = Coherence.client(configuration);

        coherence.start().join();

        assertThat(listener.f_events.size(), is(1));

        List<SessionLifecycleEvent.Type> defaultEvents = listener.f_events.get(sessionName);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        listener.f_events.clear();
        CompletableFuture<Void> future = coherence.whenClosed();

        // should close the test session
        coherence.close();
        future.join();

        assertThat(listener.f_events.size(), is(1));

        defaultEvents = listener.f_events.get(sessionName);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));

        // should close the System session
        listener.f_events.clear();
        Coherence.closeAll();

        assertThat(listener.f_events.size(), is(0));
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
