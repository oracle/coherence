/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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
import com.tangosol.net.events.annotation.LifecycleEvents;
import com.tangosol.net.events.annotation.SessionLifecycleEvents;
import com.tangosol.net.events.application.LifecycleEvent;
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
@SuppressWarnings("resource")
public class SessionLifecycleTests
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
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
        Listener               listener          = new Listener();
        LifecycleListener      lifecycleListener = new LifecycleListener();
        CoherenceConfiguration configuration     = CoherenceConfiguration.builder()
                                                        .withEventInterceptors(listener, lifecycleListener)
                                                        .build();
        Coherence              coherence         = Coherence.clusterMember(configuration);

        coherence.start().join();

        assertThat(listener.f_events.size(), is(2));
        assertThat(lifecycleListener.f_events.size(), is(2));

        List<SessionLifecycleEvent.Type> systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        List<SessionLifecycleEvent.Type> defaultEvents = listener.f_events.get(Coherence.DEFAULT_NAME);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        List<LifecycleEvent.Type> lifecycleSystemEvents = lifecycleListener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(lifecycleSystemEvents, contains(LifecycleEvent.Type.ACTIVATING, LifecycleEvent.Type.ACTIVATED));

        List<LifecycleEvent.Type> lifecycleDefaultEvents = lifecycleListener.f_events.get(Coherence.DEFAULT_NAME);
        assertThat(lifecycleDefaultEvents, contains(LifecycleEvent.Type.ACTIVATING, LifecycleEvent.Type.ACTIVATED));

        listener.f_events.clear();
        lifecycleListener.f_events.clear();
        CompletableFuture<Void> future = coherence.whenClosed();

        // should only close the default session
        coherence.close();
        future.join();

        assertThat(listener.f_events.size(), is(1));
        assertThat(lifecycleListener.f_events.size(), is(1));

        defaultEvents = listener.f_events.get(Coherence.DEFAULT_NAME);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));

        lifecycleDefaultEvents = lifecycleListener.f_events.get(Coherence.DEFAULT_NAME);
        assertThat(lifecycleDefaultEvents, contains(LifecycleEvent.Type.DISPOSING));

        // should close the System session
        listener.f_events.clear();
        lifecycleListener.f_events.clear();
        Coherence.closeAll();

        assertThat(listener.f_events.size(), is(1));
        assertThat(lifecycleListener.f_events.size(), is(1));

        systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));

        lifecycleSystemEvents = lifecycleListener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(lifecycleSystemEvents, contains(LifecycleEvent.Type.DISPOSING));
        }

    @Test
    void shouldReceiveSystemAndDefaultSessionEventsWhenClient()
        {
        Listener               listener      = new Listener();
        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                        .withEventInterceptors(listener)
                                                        .build();
        Coherence              coherence     = Coherence.client(configuration);

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
        }

    @Test
    void shouldReceiveSystemAndConfiguredSessionEventsWhenClusterMember()
        {
        Listener               listener             = new Listener();
        LifecycleListener      lifecycleListener    = new LifecycleListener();
        String                 sessionName          = "Test";
        SessionConfiguration   sessionConfiguration = SessionConfiguration.builder()
                                                            .named(sessionName)
                                                            .withScopeName(sessionName)
                                                            .build();
        CoherenceConfiguration configuration        = CoherenceConfiguration.builder()
                                                            .withSession(sessionConfiguration)
                                                            .withEventInterceptors(listener, lifecycleListener)
                                                            .build();
        Coherence              coherence            = Coherence.clusterMember(configuration);

        coherence.start().join();

        assertThat(listener.f_events.size(), is(2));
        assertThat(lifecycleListener.f_events.size(), is(2));

        List<SessionLifecycleEvent.Type> systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        List<SessionLifecycleEvent.Type> defaultEvents = listener.f_events.get(sessionName);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

        List<LifecycleEvent.Type> lifecycleSystemEvents = lifecycleListener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(lifecycleSystemEvents, contains(LifecycleEvent.Type.ACTIVATING, LifecycleEvent.Type.ACTIVATED));

        List<LifecycleEvent.Type> lifecycleDefaultEvents = lifecycleListener.f_events.get(sessionName);
        assertThat(lifecycleDefaultEvents, contains(LifecycleEvent.Type.ACTIVATING, LifecycleEvent.Type.ACTIVATED));

        listener.f_events.clear();
        lifecycleListener.f_events.clear();
        CompletableFuture<Void> future = coherence.whenClosed();

        // should only close the test session
        coherence.close();
        future.join();

        assertThat(listener.f_events.size(), is(1));
        assertThat(lifecycleListener.f_events.size(), is(1));

        defaultEvents = listener.f_events.get(sessionName);
        assertThat(defaultEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));

        lifecycleDefaultEvents = lifecycleListener.f_events.get(sessionName);
        assertThat(lifecycleDefaultEvents, contains(LifecycleEvent.Type.DISPOSING));

        // should close the System session
        listener.f_events.clear();
        lifecycleListener.f_events.clear();
        Coherence.closeAll();

        assertThat(listener.f_events.size(), is(1));
        assertThat(lifecycleListener.f_events.size(), is(1));

        systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STOPPING, SessionLifecycleEvent.Type.STOPPED));

        lifecycleSystemEvents = lifecycleListener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(lifecycleSystemEvents, contains(LifecycleEvent.Type.DISPOSING));
        }

    @Test
    void shouldReceiveSystemAndConfiguredSessionEventsWhenClient()
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

        assertThat(listener.f_events.size(), is(2));

        List<SessionLifecycleEvent.Type> systemEvents = listener.f_events.get(Coherence.SYSTEM_SESSION);
        assertThat(systemEvents, contains(SessionLifecycleEvent.Type.STARTING, SessionLifecycleEvent.Type.STARTED));

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

        assertThat(listener.f_events.size(), is(1));
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

    @Interceptor(identifier="LEInterceptor")
    @LifecycleEvents
    public static class LifecycleListener
            implements EventInterceptor<LifecycleEvent>
        {
        @Override
        public void onEvent(LifecycleEvent event)
            {
            String                    sName = event.getConfigurableCacheFactory().getScopeName();
            List<LifecycleEvent.Type> list  = f_events.computeIfAbsent(sName, k -> new ArrayList<>());
            list.add(event.getType());
            }

        final Map<String, List<LifecycleEvent.Type>> f_events = new ConcurrentHashMap<>();
        }
    }
