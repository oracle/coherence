/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.cdi.events.Created;
import com.oracle.coherence.cdi.events.Destroyed;
import com.oracle.coherence.cdi.events.Truncated;
import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.WrapperNamedCache;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A test for RemoteMapLifecycleEventProducer.
 *
 * @author Jonathan Knight  2020.06.22
 * @since 20.06
 */
@ExtendWith(WeldJunit5Extension.class)
public class RemoteMapLifecycleEventProducerIT
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                  .addBeanClass(RemoteMapLifecycleEventProducerIT.class)
                                                  .addBeanClass(RemoteMapLifecycleEventProducerIT.Listener.class)
                                                  .addBeanClass(RemoteMapLifecycleEventProducer.class));


    @Inject
    private Listener m_listener;

    @Inject
    private RemoteMapLifecycleEvent.Dispatcher m_dispatcher;

    @BeforeEach
    void setup()
        {
        m_listener.clear();
        }

    @Test
    public void shouldObserveCreateEvents() throws Exception
        {
        NamedMap<?, ?> map = new WrapperNamedCache<>("test");
        m_dispatcher.dispatch(map, "test-scope", "test-session", "test-service", RemoteMapLifecycleEvent.Type.Created);

        List<RemoteMapLifecycleEvent> events = m_listener.getEvents();
        assertThat(events, is(notNullValue()));
        assertThat(events.size(), is(1));
        RemoteMapLifecycleEvent event = events.get(0);
        assertThat(event.getMap(), is(sameInstance(map)));
        assertThat(event.getScope(), is("test-scope"));
        assertThat(event.getServiceName(), is("test-service"));
        assertThat(event.getMapName(), is("test"));
        assertThat(event.getSessionName(), is("test-session"));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Created));

        Eventually.assertDeferred(() -> m_listener.getAsyncEventCount(), is(1));

        events = m_listener.getAsyncEvents();
        assertThat(events, is(notNullValue()));
        assertThat(events.size(), is(1));
        event = events.get(0);
        assertThat(event.getMap(), is(sameInstance(map)));
        assertThat(event.getScope(), is("test-scope"));
        assertThat(event.getServiceName(), is("test-service"));
        assertThat(event.getMapName(), is("test"));
        assertThat(event.getSessionName(), is("test-session"));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Created));
        }

    @Test
    public void shouldObserveDestroyEvents() throws Exception
        {
        NamedMap<?, ?> map = new WrapperNamedCache<>("test");
        m_dispatcher.dispatch(map, "test-scope", "test-session", "test-service", RemoteMapLifecycleEvent.Type.Destroyed);

        List<RemoteMapLifecycleEvent> events = m_listener.getEvents();
        assertThat(events, is(notNullValue()));
        assertThat(events.size(), is(1));
        RemoteMapLifecycleEvent event = events.get(0);
        assertThat(event.getMap(), is(sameInstance(map)));
        assertThat(event.getScope(), is("test-scope"));
        assertThat(event.getServiceName(), is("test-service"));
        assertThat(event.getMapName(), is("test"));
        assertThat(event.getSessionName(), is("test-session"));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Destroyed));

        Eventually.assertDeferred(() -> m_listener.getAsyncEventCount(), is(1));

        events = m_listener.getAsyncEvents();
        assertThat(events, is(notNullValue()));
        assertThat(events.size(), is(1));
        event = events.get(0);
        assertThat(event.getMap(), is(sameInstance(map)));
        assertThat(event.getScope(), is("test-scope"));
        assertThat(event.getServiceName(), is("test-service"));
        assertThat(event.getMapName(), is("test"));
        assertThat(event.getSessionName(), is("test-session"));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Destroyed));
        }

    @Test
    public void shouldObserveTruncateEvents() throws Exception
        {
        NamedMap<?, ?> map = new WrapperNamedCache<>("test");
        m_dispatcher.dispatch(map, "test-scope", "test-session", "test-service", RemoteMapLifecycleEvent.Type.Truncated);

        List<RemoteMapLifecycleEvent> events = m_listener.getEvents();
        assertThat(events, is(notNullValue()));
        assertThat(events.size(), is(1));
        RemoteMapLifecycleEvent event = events.get(0);
        assertThat(event.getMap(), is(sameInstance(map)));
        assertThat(event.getScope(), is("test-scope"));
        assertThat(event.getServiceName(), is("test-service"));
        assertThat(event.getMapName(), is("test"));
        assertThat(event.getSessionName(), is("test-session"));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Truncated));

        Eventually.assertDeferred(() -> m_listener.getAsyncEventCount(), is(1));

        events = m_listener.getAsyncEvents();
        assertThat(events, is(notNullValue()));
        assertThat(events.size(), is(1));
        event = events.get(0);
        assertThat(event.getMap(), is(sameInstance(map)));
        assertThat(event.getScope(), is("test-scope"));
        assertThat(event.getServiceName(), is("test-service"));
        assertThat(event.getMapName(), is("test"));
        assertThat(event.getSessionName(), is("test-session"));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Truncated));
        }


    @ApplicationScoped
    static class Listener
        {
        void onCreate(@Observes @Created RemoteMapLifecycleEvent event)
            {
            m_listEvent.add(event);
            }

        void onDestroy(@Observes @Destroyed RemoteMapLifecycleEvent event)
            {
            m_listEvent.add(event);
            }

        void onTruncate(@Observes @Truncated RemoteMapLifecycleEvent event)
            {
            m_listEvent.add(event);
            }

        void onCreateAsync(@ObservesAsync @Created RemoteMapLifecycleEvent event)
            {
            m_listAsyncEvent.add(event);
            }

        void onDestroyAsync(@ObservesAsync @Destroyed RemoteMapLifecycleEvent event)
            {
            m_listAsyncEvent.add(event);
            }

        void onTruncateAsync(@ObservesAsync @Truncated RemoteMapLifecycleEvent event)
            {
            m_listAsyncEvent.add(event);
            }

        List<RemoteMapLifecycleEvent> getEvents()
            {
            return m_listEvent;
            }

        List<RemoteMapLifecycleEvent> getAsyncEvents()
            {
            return m_listAsyncEvent;
            }

        int getAsyncEventCount()
            {
            return m_listAsyncEvent.size();
            }

        void clear()
            {
            m_listEvent.clear();
            m_listAsyncEvent.clear();
            }

        private final List<RemoteMapLifecycleEvent> m_listEvent = new ArrayList<>();

        private final List<RemoteMapLifecycleEvent> m_listAsyncEvent = new ArrayList<>();
        }
    }
