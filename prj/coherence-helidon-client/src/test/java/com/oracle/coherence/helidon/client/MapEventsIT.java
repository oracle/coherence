/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.client.AsyncNamedCacheClient;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.inject.PropertyExtractor;
import com.oracle.coherence.inject.Scope;
import com.oracle.coherence.inject.SessionName;
import com.oracle.coherence.inject.WhereFilter;

import com.oracle.coherence.event.CacheName;
import com.oracle.coherence.event.Deleted;
import com.oracle.coherence.event.Inserted;
import com.oracle.coherence.event.MapName;
import com.oracle.coherence.event.ScopeName;
import com.oracle.coherence.event.Synchronous;
import com.oracle.coherence.event.Updated;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.oracle.coherence.grpc.proxy.GrpcServerController;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.Coherence;
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
        System.setProperty("coherence.pof.config",       "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled",      "true");
        System.setProperty("coherence.log.level",        "9");

        // The CDI server will start DCS which will in turn cause the gRPC server to start
        s_server = Server.create().start();

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
            holder.getDefaultClientSession().close();
            holder.getTestClientSession().close();
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
        NamedCache<String, Person> underlying = getUnderlying(CACHE_NAME_PEOPLE);
        Session                    session    = ensureSession(Coherence.DEFAULT_NAME);
        NamedCache<String, Person> cache      = session.getCache(CACHE_NAME_PEOPLE);

        // Wait for the listeners to be registered as it happens async
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(underlying), is(greaterThanOrEqualTo(2)));

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
        Session                    sessionOne   = ensureSession(Coherence.DEFAULT_NAME);
        NamedCache<String, Person> cacheOne     = sessionOne.getCache(CACHE_NAME_NAMED_PEOPLE);
        Session                    sessionTwo   = ensureSession(SessionConfigurations.SERVER_TEST);
        NamedCache<String, Person> cacheTwo     = sessionTwo.getCache(CACHE_NAME_NAMED_PEOPLE);
        Session                    sessionThree = ensureSession(SessionConfigurations.CLIENT_TEST);
        NamedCache<String, Person> cacheThree   = sessionThree.getCache(CACHE_NAME_NAMED_PEOPLE);

        // Wait for the listeners to be registered as it happens async
        Eventually.assertDeferred(() -> ((AsyncNamedCacheClient<?, ?>) cacheOne.async()).getListenerCount(),
                                        is(greaterThanOrEqualTo(1)));
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(cacheTwo),
                                        is(greaterThanOrEqualTo(1)));
        Eventually.assertDeferred(() -> ((AsyncNamedCacheClient<?, ?>) cacheThree.async()).getListenerCount(),
                                  is(greaterThanOrEqualTo(1)));

        // both of these client side caches are the same server side cache
        cacheOne.put("homer", new Person("Homer", "Simpson", 45, "male"));
        cacheTwo.put("marge", new Person("Marge", "Simpson", 43, "female"));

        NamedSessionListener listener = CDI.current().select(NamedSessionListener.class).get();
        Eventually.assertDeferred(() -> listener.getAnyEvents().size(), is(4));
        Eventually.assertDeferred(() -> listener.getDefaultEvents().size(), is(1));
        Eventually.assertDeferred(() -> listener.getTestEvents().size(), is(1));
        }

    @Test
    void testScopedNamedEvents()
        {
        Session                    sessionOne = ensureSession(Coherence.DEFAULT_NAME);
        NamedCache<String, Person> cacheOne   = sessionOne.getCache(CACHE_NAME_SCOPED_PEOPLE);
        Session                    sessionTwo = ensureSession(SessionConfigurations.SERVER_TEST);
        NamedCache<String, Person> cacheTwo   = sessionTwo.getCache(CACHE_NAME_SCOPED_PEOPLE);

        // Wait for the listeners to be registered as it happens async
        Eventually.assertDeferred(() -> ((AsyncNamedCacheClient<?, ?>) cacheOne.async()).getListenerCount(),
                                        is(greaterThanOrEqualTo(1)));
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(cacheTwo),
                                        is(greaterThanOrEqualTo(1)));

        // we need to get the client side caches otherwise the listener will only get server side events
        // as the listeners are for "any" session the CDI extension cannot ensure the caches automatically
        SessionHolder holder = CDI.current().select(SessionHolder.class).get();
        holder.getDefaultClientSession().getCache(CACHE_NAME_SCOPED_PEOPLE);
        holder.getTestClientSession().getCache(CACHE_NAME_SCOPED_PEOPLE);

        // both of these client side caches are the same server side cache
        cacheOne.put("homer", new Person("Homer", "Simpson", 45, "male"));
        cacheTwo.put("marge", new Person("Marge", "Simpson", 43, "female"));

        // Because the server side cache is actually running inside the test JVM the listener will receive
        // events for the server side caches as well as the client side caches.
        NamedScopedSessionListener listener = CDI.current().select(NamedScopedSessionListener.class).get();
        Eventually.assertDeferred(() -> listener.getAnyEvents().size(), is(4));
        Eventually.assertDeferred(() -> listener.getDefaultEvents().size(), is(2));
        Eventually.assertDeferred(() -> listener.getTestEvents().size(), is(2));
        }

    // ----- helper methods -------------------------------------------------

    private Session ensureSession(String sName)
        {
        return Coherence.findSession(sName)
                        .orElseThrow(() -> new AssertionError("Could not find Session"));
        }

    private <K, V> NamedCache<K, V> getUnderlying(String sCacheName)
        {
        ClassLoader              loader = Classes.getContextClassLoader();
        ConfigurableCacheFactory ccf    = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(CacheFactoryBuilder.URI_DEFAULT, loader);
        return ccf.ensureCache(sCacheName, loader);
        }

    // ---- helper classes --------------------------------------------------

    @ApplicationScoped
    public static class SessionHolder
        {
        @Inject
        @SessionName(SessionConfigurations.CLIENT_DEFAULT)
        Session m_sessionOne;

        @Inject
        @SessionName(SessionConfigurations.CLIENT_TEST)
        Session m_sessionTwo;

        public Session getDefaultClientSession()
            {
            return m_sessionOne;
            }

        public Session getTestClientSession()
            {
            return m_sessionTwo;
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
        private void onPersonInserted(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @Inserted @MapName(CACHE_NAME_PEOPLE) MapEvent<String, Person> event)
            {
            record(event);
            }

        @Synchronous
        private void onPersonUpdated(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @Updated @MapName(CACHE_NAME_PEOPLE) MapEvent<String, Person> event)
            {
            record(event);
            }

        @Synchronous
        private void onPersonDeleted(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @Deleted @CacheName(CACHE_NAME_PEOPLE) MapEvent<String, Person> event)
            {
            record(event);
            }

        @WhereFilter("firstName = 'Bart' and lastName = 'Simpson'")
        private void onBart(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @MapName(CACHE_NAME_PEOPLE) MapEvent<String, Person> event)
            {
            f_listFilteredEvents.add(event);
            }

        @PropertyExtractor("firstName")
        private void onPersonInsertedTransformed(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @Inserted @MapName(CACHE_NAME_PEOPLE) MapEvent<String, String> event)
            {
            f_listTransformedEvents.add(event);
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
        private void onDefaultPerson(@Observes
                                     @SessionName(SessionConfigurations.CLIENT_DEFAULT)
                                     @MapName(CACHE_NAME_NAMED_PEOPLE) MapEvent<String, Person> event)
            {
            String sClass = event.getMap().getClass().getSimpleName();
            System.out.println("Received event (default): class=" + sClass + " event=" + event);
            f_listDefaultEvents.add(event);
            }

        @Synchronous
        private void onTestPerson(@Observes
                                  @SessionName(SessionConfigurations.CLIENT_TEST)
                                  @MapName(CACHE_NAME_NAMED_PEOPLE) MapEvent<String, Person> event)
            {
            String sClass = event.getMap().getClass().getSimpleName();
            System.out.println("Received event (test): class=" + sClass + " event=" + event);
            f_listTestEvents.add(event);
            }

        @Synchronous
        private void onAnyPerson(@Observes
                                 @MapName(CACHE_NAME_NAMED_PEOPLE) MapEvent<String, Person> event)
            {
            String sClass = event.getMap().getClass().getSimpleName();
            System.out.println("Received event (any): class=" + sClass + " event=" + event);
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
        private void onDefaultPerson(@Observes @ScopeName(Scope.DEFAULT) @MapName(CACHE_NAME_SCOPED_PEOPLE) MapEvent<String, Person> event)
            {
            String sClass = event.getMap().getClass().getSimpleName();
            System.out.println("Received event (default): class=" + sClass + " event=" + event);
            f_listDefaultEvents.add(event);
            }

        @Synchronous
        private void onTestPerson(@Observes @ScopeName(SessionConfigurations.TEST_SCOPE) @MapName(CACHE_NAME_SCOPED_PEOPLE) MapEvent<String, Person> event)
            {
            String sClass = event.getMap().getClass().getSimpleName();
            System.out.println("Received event (test): class=" + sClass + " event=" + event);
            f_listTestEvents.add(event);
            }

        @Synchronous
        private void onAnyPerson(@Observes @MapName(CACHE_NAME_SCOPED_PEOPLE) MapEvent<String, Person> event)
            {
            String sClass = event.getMap().getClass().getSimpleName();
            System.out.println("Received event (any): class=" + sClass + " event=" + event);
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

    // ----- constants ------------------------------------------------------
    
    public static final String CACHE_NAME_PEOPLE = "people";
    public static final String CACHE_NAME_NAMED_PEOPLE = "named-people";
    public static final String CACHE_NAME_SCOPED_PEOPLE = "named-scoped-people";
    
    // ----- data members ---------------------------------------------------

    private static Server s_server;
    }
