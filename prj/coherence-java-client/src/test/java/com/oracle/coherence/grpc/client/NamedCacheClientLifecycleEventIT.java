/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.RemoteMapLifecycleEvent;
import com.oracle.coherence.cdi.events.Created;
import com.oracle.coherence.cdi.events.Destroyed;
import com.oracle.coherence.cdi.events.Truncated;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import io.helidon.microprofile.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for {@link RemoteMapLifecycleEvent} observers.
 *
 * @author Jonathan Knight  2019.11.07
 * @since 20.06
 */
class NamedCacheClientLifecycleEventIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "NamedCacheClientLifecycleEventIT");
        System.setProperty("coherence.cache.config",  "coherence-config.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled", "true");

        s_server = Server.create().start();

        s_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("coherence-config.xml", null);
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        s_listClients.forEach(client -> {
        if (client.isActive())
            {
            client.destroy();
            }
        });
        s_server.stop();
        }

    @Test
    public void shouldObserveEvents()
        {
        String                           sName  = "foo";
        NamedCache<String, String>       cache  = ensureCache(sName);
        NamedCacheClient<String, String> client = createClient(sName);
        RemoteMapLifecycleEvent          event;

        Listener listener = getListener();

        Eventually.assertDeferred(listener::getEventCount, is(1));
        Eventually.assertDeferred(listener::getAsyncEventCount, is(1));

        event = listener.getEvents().get(0);
        assertThat(event, is(notNullValue()));
        assertThat(event.getMap(), is(client));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Created));

        event = listener.getAsyncEvents().get(0);
        assertThat(event, is(notNullValue()));
        assertThat(event.getMap(), is(client));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Created));

        listener.clear();

        cache.put("foo", "bar");
        cache.truncate();

        Eventually.assertDeferred(listener::getEventCount, is(1));
        Eventually.assertDeferred(listener::getAsyncEventCount, is(1));

        event = listener.getEvents().get(0);
        assertThat(event, is(notNullValue()));
        assertThat(event.getMap(), is(client));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Truncated));

        event = listener.getAsyncEvents().get(0);
        assertThat(event, is(notNullValue()));
        assertThat(event.getMap(), is(client));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Truncated));

        listener.clear();
        cache.destroy();

        Eventually.assertDeferred(listener::getEventCount, is(1));
        Eventually.assertDeferred(listener::getAsyncEventCount, is(1));

        event = listener.getEvents().get(0);
        assertThat(event, is(notNullValue()));
        assertThat(event.getMap(), is(client));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Destroyed));

        event = listener.getAsyncEvents().get(0);
        assertThat(event, is(notNullValue()));
        assertThat(event.getMap(), is(client));
        assertThat(event.getType(), is(RemoteMapLifecycleEvent.Type.Destroyed));
        }

    // ----- helper methods -------------------------------------------------

    Listener getListener()
        {
        return CDI.current().select(Listener.class).get();
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <K, V> NamedCacheClient<K, V> createClient(String sCacheName)
        {
        Instance<NamedCacheClient> cacheInstance = CDI.current().getBeanManager()
                .createInstance()
                .select(NamedCacheClient.class,
                        Name.Literal.of(sCacheName),
                        Remote.Literal.of("test"));

        assertThat(cacheInstance.isResolvable(), is(true));

        NamedCacheClient<K, V> client = cacheInstance.get();

        s_listClients.add(client);
        return client;
        }

    protected <K, V> NamedCache<K, V> ensureCache(String sName)
        {
        return s_ccf.ensureCache(sName, null);
        }

    // ----- inner class: Listener ------------------------------------------

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

        int getEventCount()
            {
            return m_listEvent.size();
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

    // ----- data members ---------------------------------------------------

    protected static ConfigurableCacheFactory s_ccf;

    private static Server s_server;

    protected static List<NamedCacheClient<?, ?>> s_listClients = new ArrayList<>();
    }
