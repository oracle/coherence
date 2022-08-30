/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package helidon.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.client.AsyncNamedCacheClient;

import com.oracle.coherence.cdi.PropertyExtractor;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionName;
import com.oracle.coherence.cdi.WhereFilter;

import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Deleted;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.ScopeName;
import com.oracle.coherence.cdi.events.Synchronous;
import com.oracle.coherence.cdi.events.Updated;

import com.oracle.coherence.grpc.proxy.GrpcServerController;

import com.tangosol.coherence.component.util.SafeAsyncNamedCache;
import com.tangosol.coherence.component.util.SafeNamedCache;
import com.tangosol.internal.net.SessionNamedCache;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;

import com.tangosol.net.Session;
import com.tangosol.util.MapEvent;

import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.oracle.coherence.testing.util.EventsHelper;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.enterprise.event.Observes;

import jakarta.enterprise.inject.spi.CDI;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
        System.setProperty("coherence.profile",          "thin");
        System.setProperty("coherence.pof.config",       "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled",      "true");
        System.setProperty("coherence.log.level",        "9");
        System.setProperty("coherence.grpc.server.port", "1408");

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
        Session                    session       = Coherence.getInstance().getSession();
        NamedCache<String, Person> underlying    = session.getCache(CACHE_NAME_PEOPLE);
        Session                    sessionClient = ensureSession(SessionConfigurations.CLIENT_DEFAULT);
        NamedCache<String, Person> cacheDefault  = sessionClient.getCache(CACHE_NAME_PEOPLE);

        // Wait for the listeners to be registered as it happens async
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(underlying), is(greaterThanOrEqualTo(2)));
        Eventually.assertDeferred(() -> getListenerCount(cacheDefault), is(greaterThanOrEqualTo(4)));

        underlying.put("homer", new Person("Homer", "Simpson", 45, "male"));
        underlying.put("marge", new Person("Marge", "Simpson", 43, "female"));
        underlying.put("bart", new Person("Bart", "Simpson", 12, "male"));
        underlying.put("lisa", new Person("Lisa", "Simpson", 10, "female"));
        underlying.put("maggie", new Person("Maggie", "Simpson", 2, "female"));

        underlying.invoke("homer", new Uppercase());
        underlying.invoke("bart", new Uppercase());

        underlying.remove("bart");
        underlying.remove("marge");
        underlying.remove("lisa");
        underlying.remove("maggie");

        TestListener listener = CDI.current().select(TestListener.class).get();
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_INSERTED), is(5));
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_UPDATED), is(2));
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_DELETED), is(4));

        // There should be an insert and an update for Bart.
        // The delete event for Bart does not match the filter because the lastName
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
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(cacheOne),
                                        is(greaterThanOrEqualTo(1)));
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(cacheTwo),
                                        is(greaterThanOrEqualTo(1)));
        Eventually.assertDeferred(() -> getListenerCount(cacheThree), is(greaterThanOrEqualTo(2)));

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

        // we need to get the client side caches otherwise the listener will only get server side events
        // as the listeners are for "any" session the CDI extension cannot ensure the caches automatically
        SessionHolder holder = CDI.current().select(SessionHolder.class).get();
        NamedCache<Object, Object> cacheClientOne = holder.getDefaultClientSession().getCache(CACHE_NAME_SCOPED_PEOPLE);
        NamedCache<Object, Object> cacheClientTwo = holder.getTestClientSession().getCache(CACHE_NAME_SCOPED_PEOPLE);

        // Wait for the listeners to be registered as it happens async
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(cacheOne),
                                        is(greaterThanOrEqualTo(1)));
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(cacheTwo),
                                        is(greaterThanOrEqualTo(1)));
        Eventually.assertDeferred(() -> getListenerCount(cacheClientOne), is(greaterThanOrEqualTo(2)));
        Eventually.assertDeferred(() -> getListenerCount(cacheClientTwo), is(greaterThanOrEqualTo(2)));

        // both of these client side caches are the same server side cache
        cacheOne.put("homer", new Person("Homer", "Simpson", 45, "male"));
        cacheTwo.put("marge", new Person("Marge", "Simpson", 43, "female"));

        // Because the server side cache is actually running inside the test JVM the listener will receive
        // events for the server side caches as well as the client side caches.
        NamedScopedSessionListener listener = CDI.current().select(NamedScopedSessionListener.class).get();
        Eventually.assertDeferred(() -> listener.getAnyEvents().size(), is(4));
        Eventually.assertDeferred(() -> listener.getDefaultEvents().size(), is(0));
        Eventually.assertDeferred(() -> listener.getTestEvents().size(), is(1));
        }

    // ----- helper methods -------------------------------------------------

    private Session ensureSession(String sName)
        {
        return Coherence.findSession(sName)
                        .orElseThrow(() -> new AssertionError("Could not find Session " + sName));
        }

    private <K, V> NamedCache<K, V> getUnderlying(String sSession, String sCacheName)
        {
        Coherence coherence = Coherence.getInstance();
        Session   session   = coherence.getSession(sSession);
        return session.getCache(sCacheName);
        }

    private int getListenerCount(NamedCache cache)
        {
        if (cache instanceof SessionNamedCache)
            {
            cache = ((SessionNamedCache) cache).getInternalNamedCache();
            }
        if (cache instanceof SafeNamedCache)
            {
            cache = ((SafeNamedCache) cache).getNamedCache();
            }

        AsyncNamedCache async = cache.async();
        if (async instanceof SafeAsyncNamedCache)
            {
            async = ((SafeAsyncNamedCache) async).getRunningNamedCache();
            }
        return ((AsyncNamedCacheClient) async).getListenerCount();
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
        private int record(MapEvent<String, Person> event)
            {
            switch (event.getId())
                {
                case MapEvent.ENTRY_INSERTED:
                    return m_cInsert.incrementAndGet();
                case MapEvent.ENTRY_UPDATED:
                    return m_cUpdate.incrementAndGet();
                case MapEvent.ENTRY_DELETED:
                    return m_cDelete.incrementAndGet();
                default:
                    throw new IllegalArgumentException("Bad event type: " + event);
                }
            }

        @Synchronous
        private void onPersonInserted(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @Inserted @MapName(CACHE_NAME_PEOPLE) MapEvent<String, Person> event)
            {
            int n = record(event);
            System.out.println("Received event: (CLIENT_DEFAULT, @Inserted CACHE_NAME_PEOPLE) (" + n + ") " + event);
            }

        @Synchronous
        private void onPersonUpdated(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @Updated @MapName(CACHE_NAME_PEOPLE) MapEvent<String, Person> event)
            {
            int n = record(event);
            System.out.println("Received event: (CLIENT_DEFAULT, @Updated CACHE_NAME_PEOPLE) (" + n + ") " + event);
            }

        @Synchronous
        private void onPersonDeleted(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @Deleted @CacheName(CACHE_NAME_PEOPLE) MapEvent<String, Person> event)
            {
            int n = record(event);
            System.out.println("Received event: (CLIENT_DEFAULT, @Deleted CACHE_NAME_PEOPLE) " + n + ") " + event);
            }

        @WhereFilter("firstName = 'Bart' and lastName = 'Simpson'")
        private void onBart(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @MapName(CACHE_NAME_PEOPLE) MapEvent<String, Person> event)
            {
            f_listFilteredEvents.add(event);
            int n = f_listFilteredEvents.size();
            System.out.println("Received event: (CLIENT_DEFAULT, Filtered CACHE_NAME_PEOPLE) (" + n + ") " + event);
            }

        @PropertyExtractor("firstName")
        private void onPersonInsertedTransformed(@Observes @SessionName(SessionConfigurations.CLIENT_DEFAULT) @Inserted @MapName(CACHE_NAME_PEOPLE) MapEvent<String, String> event)
            {
            f_listTransformedEvents.add(event);
            int n = f_listFilteredEvents.size();
            System.out.println("Received event: (CLIENT_DEFAULT, @Inserted CACHE_NAME_PEOPLE Transformed) (" + n + ") " + event);
            }

        public List<MapEvent<String, Person>> getFilteredEvents()
            {
            return f_listFilteredEvents;
            }

        public List<MapEvent<String, String>> getTransformedEvents()
            {
            return f_listTransformedEvents;
            }

        Integer getEvents(int id)
            {
            switch (id)
                {
                case MapEvent.ENTRY_INSERTED:
                    return m_cInsert.get();
                case MapEvent.ENTRY_UPDATED:
                    return m_cUpdate.get();
                case MapEvent.ENTRY_DELETED:
                    return m_cDelete.get();
                default:
                    throw new IllegalArgumentException("Bad event type: " + id);
                }
            }

        // ----- data members -----------------------------------------------

        private final AtomicInteger m_cInsert = new AtomicInteger(0);

        private final AtomicInteger m_cUpdate = new AtomicInteger(0);

        private final AtomicInteger m_cDelete = new AtomicInteger(0);

        private final List<MapEvent<String, Person>> f_listFilteredEvents = Collections.synchronizedList(new ArrayList<>());

        private final List<MapEvent<String, String>> f_listTransformedEvents = Collections.synchronizedList(new ArrayList<>());
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
