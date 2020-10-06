/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.cdi.PropertyExtractor;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.WhereFilter;

import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Deleted;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.ScopeName;
import com.oracle.coherence.cdi.events.Synchronous;
import com.oracle.coherence.cdi.events.Updated;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.oracle.coherence.grpc.proxy.GrpcServerController;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.Session;
import com.tangosol.util.MapEvent;

import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import util.EventsHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

/**
 * Test CDI cache events support.
 *
 * @author Jonathan Knight  2020.09.29
 */
public class MapEventsIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest() throws Exception
        {
        System.setProperty("coherence.ttl",              "0");
        System.setProperty("coherence.clustername",      "MapEventsIT");
        System.setProperty("coherence.cacheconfig",      "coherence-config.xml");
        System.setProperty("coherence.cacheconfig.test", "coherence-config-two.xml");
        System.setProperty("coherence.pof.config",       "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled",      "true");
        System.setProperty("coherence.log.level",        "9");

        // The CDI server will start DCS which will in turn cause the gRPC server to start
        s_server = Server.create().start();
        s_ccf    = CacheFactory.getCacheFactoryBuilder()
                               .getConfigurableCacheFactory("coherence-config.xml", null);

        // wait at most 1 minute for the gRPC Server
        GrpcServerController.INSTANCE.whenStarted()
                .toCompletableFuture()
                .get(1, TimeUnit.MINUTES);
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        try
            {
            SessionHolder holder = CDI.current().select(SessionHolder.class).get();
            holder.getSessionOne().close();
            holder.getSessionTwo().close();
            holder.getSessionThree().close();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }

        if (s_server != null)
            {
            s_server.stop();
            }
        }

    @Test
    void testEvents()
        {
        NamedCache<String, Person> cache = s_ccf.ensureCache("people", null);

        // Wait for the listeners to be registered as it happens async
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(cache), is(greaterThanOrEqualTo(2)));

        cache.put("homer", new Person("Homer", "Simpson", 45, "male"));
        cache.put("marge", new Person("Marge", "Simpson", 43, "female"));
        cache.put("bart", new Person("Bart", "Simpson", 12, "male"));
        cache.put("lisa", new Person("Lisa", "Simpson", 10, "female"));
        cache.put("maggie", new Person("Maggie", "Simpson", 2, "female"));

        cache.invoke("homer", new Uppercase());
        cache.invoke("bart", new Uppercase());

        cache.remove("bart");
        cache.remove("marge");
        cache.remove("lisa");
        cache.remove("maggie");

        TestListener listener = CDI.current().select(TestListener.class).get();
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_INSERTED), is(5));
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_UPDATED), is(2));
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_DELETED), is(4));

        // There should be an insert and an update for Bart.
        // The delete for Bart does not match the filter because the lastName
        // had been changed to uppercase.
        List<MapEvent<String, Person>> filteredEvents = listener.getFilteredEvents();
        Eventually.assertDeferred(filteredEvents::size, is(2));
        MapEvent<String, Person> eventOne = filteredEvents.get(0);
        MapEvent<String, Person> eventTwo = filteredEvents.get(1);
        assertThat(eventOne.getId(), is(MapEvent.ENTRY_INSERTED));
        assertThat(eventOne.getKey(), is("bart"));
        assertThat(eventTwo.getId(), is(MapEvent.ENTRY_UPDATED));
        assertThat(eventTwo.getKey(), is("bart"));
        assertThat(eventTwo.getNewValue().getLastName(), is("SIMPSON"));

        // Transformed events should just be inserts with the person's firstName as the new value
        List<MapEvent<String, String>> transformedEvents = listener.getTransformedEvents();
        Eventually.assertDeferred(transformedEvents::size, is(5));
        assertThat(transformedEvents.get(0).getNewValue(), is("Homer"));
        assertThat(transformedEvents.get(1).getNewValue(), is("Marge"));
        assertThat(transformedEvents.get(2).getNewValue(), is("Bart"));
        assertThat(transformedEvents.get(3).getNewValue(), is("Lisa"));
        assertThat(transformedEvents.get(4).getNewValue(), is("Maggie"));
        }

    @Test
    void testNamedSessionEvents()
        {
        SessionHolder              holder   = CDI.current().select(SessionHolder.class).get();
        NamedCache<String, Person> cacheOne = holder.getSessionOne().getCache("named-people");
        NamedCache<String, Person> cacheTwo = holder.getSessionTwo().getCache("named-people");

        // both of these client side caches are the same server side cache
        cacheOne.put("homer", new Person("Homer", "Simpson", 45, "male"));
        cacheTwo.put("marge", new Person("Marge", "Simpson", 43, "female"));

        NamedSessionListener listener = CDI.current().select(NamedSessionListener.class).get();
        // any events listener will get six events as there a three Sessions, the server side Session
        // and two remote sessions and two inserts to the cache making six
        Eventually.assertDeferred(() -> listener.getAnyEvents().size(), is(6));
        // default events will get two events from the two inserts into the cache
        Eventually.assertDeferred(() -> listener.getDefaultEvents().size(), is(2));
        // test events will get two events from the two inserts into the cache
        Eventually.assertDeferred(() -> listener.getTestEvents().size(), is(2));
        }

    @Test
    void testNamedScopedSessionEvents()
        {
        SessionHolder              holder       = CDI.current().select(SessionHolder.class).get();
        Session                    sessionTwo   = holder.getSessionTwo();
        NamedCache<String, Person> cacheOne     = sessionTwo.getCache("named-scoped-people");
        Session                    sessionThree = holder.getSessionThree();
        NamedCache<String, Person> cacheTwo     = sessionThree.getCache("named-scoped-people");

        // both of these client side caches are the same server side cache
        cacheOne.put("homer", new Person("Homer", "Simpson", 45, "male"));
        cacheTwo.put("marge", new Person("Marge", "Simpson", 43, "female"));

        NamedScopedSessionListener listener = CDI.current().select(NamedScopedSessionListener.class).get();
        // any events listener will get six events as there a three Sessions, the server side Session
        // and two remote sessions and two inserts to the cache making six
        Eventually.assertDeferred(() -> listener.getAnyEvents().size(), is(2));
        // default events will get two events from the two inserts into the cache
        Eventually.assertDeferred(() -> listener.getDefaultEvents().size(), is(1));
        // test events will get two events from the two inserts into the cache
        Eventually.assertDeferred(() -> listener.getTestEvents().size(), is(1));
        }

    // ---- helper classes --------------------------------------------------

    @ApplicationScoped
    public static class SessionHolder
        {
        @Inject
        @Remote
        Session m_sessionOne;

        @Inject
        @Remote("test")
        Session m_sessionTwo;

        @Inject
        @Remote("test")
        @Scope("test")
        Session m_sessionThree;

        public Session getSessionOne()
            {
            return m_sessionOne;
            }

        public Session getSessionTwo()
            {
            return m_sessionTwo;
            }

        public Session getSessionThree()
            {
            return m_sessionThree;
            }
        }

    @ApplicationScoped
    public static class TestListener
        {
        private void record(MapEvent<String, Person> event)
            {
            System.out.println("Received event: " + event);
            f_listEvents.compute(event.getId(), (k, v) -> v == null ? 1 : v + 1);
            }

        @Synchronous
        private void onPersonInserted(@Observes @Remote @Inserted @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            }

        @Synchronous
        private void onPersonUpdated(@Observes @Remote @Updated @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            }

        @Synchronous
        private void onPersonDeleted(@Observes @Remote @Deleted @CacheName("people") MapEvent<String, Person> event)
            {
            record(event);
            }

        @WhereFilter("firstName = 'Bart' and lastName = 'Simpson'")
        private void onBart(@Observes @Remote @MapName("people") MapEvent<String, Person> event)
            {
            f_listFilteredEvents.add(event);
            }

        @PropertyExtractor("firstName")
        private void onPersonInsertedTransformed(@Observes @Remote @Inserted @MapName("people") MapEvent<String, String> event)
            {
            f_listTransformedEvents.add(event);
            }

        void clear()
            {
            f_listEvents.clear();
            f_listFilteredEvents.clear();
            f_listTransformedEvents.clear();
            }

        public List<MapEvent<String, Person>> getFilteredEvents()
            {
            return f_listFilteredEvents;
            }

        public List<MapEvent<String, String>> getTransformedEvents()
            {
            return f_listTransformedEvents;
            }

        // ----- data members -----------------------------------------------

        private final Map<Integer, Integer> f_listEvents = new ConcurrentHashMap<>();

        private final List<MapEvent<String, Person>> f_listFilteredEvents = Collections.synchronizedList(new ArrayList<>());

        private final List<MapEvent<String, String>> f_listTransformedEvents = Collections.synchronizedList(new ArrayList<>());

        Integer getEvents(int id)
            {
            return f_listEvents.get(id);
            }
        }

    @ApplicationScoped
    public static class NamedSessionListener
        {
        @Synchronous
        private void onDefaultPerson(@Observes @Remote @MapName("named-people") MapEvent<String, Person> event)
            {
            System.out.println("Received event (default): " + event);
            f_listDefaultEvents.add(event);
            }

        @Synchronous
        private void onTestPerson(@Observes @Remote("test") @MapName("named-people") MapEvent<String, Person> event)
            {
            System.out.println("Received event (test): " + event);
            f_listTestEvents.add(event);
            }

        @Synchronous
        private void onAnyPerson(@Observes @MapName("named-people") MapEvent<String, Person> event)
            {
            System.out.println("Received event (any): " + event);
            f_listAnyEvents.add(event);
            }

        public List<MapEvent<String, Person>> getDefaultEvents()
            {
            return f_listDefaultEvents;
            }

        public List<MapEvent<String, Person>> getAnyEvents()
            {
            return f_listAnyEvents;
            }

        public List<MapEvent<String, Person>> getTestEvents()
            {
            return f_listTestEvents;
            }

        // ----- data members -----------------------------------------------

        private final List<MapEvent<String, Person>> f_listDefaultEvents = new CopyOnWriteArrayList<>();
        private final List<MapEvent<String, Person>> f_listAnyEvents = new CopyOnWriteArrayList<>();
        private final List<MapEvent<String, Person>> f_listTestEvents = new CopyOnWriteArrayList<>();
        }

    @ApplicationScoped
    public static class NamedScopedSessionListener
        {
        @Synchronous
        private void onDefaultPerson(@Observes @Remote("test") @ScopeName(Scope.DEFAULT) @MapName("named-scoped-people") MapEvent<String, Person> event)
            {
            System.out.println("Received event (default): " + event);
            f_listDefaultEvents.add(event);
            }

        @Synchronous
        private void onTestPerson(@Observes @Remote("test") @ScopeName("test") @MapName("named-scoped-people") MapEvent<String, Person> event)
            {
            System.out.println("Received event (test): " + event);
            f_listTestEvents.add(event);
            }

        @Synchronous
        private void onAnyPerson(@Observes @Remote("test") @MapName("named-scoped-people") MapEvent<String, Person> event)
            {
            System.out.println("Received event (any): " + event);
            f_listAnyEvents.add(event);
            }

        public List<MapEvent<String, Person>> getDefaultEvents()
            {
            return f_listDefaultEvents;
            }

        public List<MapEvent<String, Person>> getAnyEvents()
            {
            return f_listAnyEvents;
            }

        public List<MapEvent<String, Person>> getTestEvents()
            {
            return f_listTestEvents;
            }

        // ----- data members -----------------------------------------------

        private final List<MapEvent<String, Person>> f_listDefaultEvents = new CopyOnWriteArrayList<>();
        private final List<MapEvent<String, Person>> f_listAnyEvents = new CopyOnWriteArrayList<>();
        private final List<MapEvent<String, Person>> f_listTestEvents = new CopyOnWriteArrayList<>();
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccf;

    private static Server s_server;
    }
