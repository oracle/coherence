/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.PropertyExtractor;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.WhereFilter;
import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Deleted;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.Synchronous;
import com.oracle.coherence.cdi.events.Updated;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.MapEvent;
import io.helidon.microprofile.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.EventsHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test CDI remote map events.
 *
 * @author Jonathan Knight  2020.06.22
 * @since 20.06
 */
class CdiMapListenerIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "CdiMapListenerIT");
        System.setProperty("coherence.cache.config",  "coherence-config.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled", "true");

        s_server = Server.create().start();
        s_ccf    = CacheFactory.getCacheFactoryBuilder()
                               .getConfigurableCacheFactory("coherence-config.xml", null);
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        s_server.stop();
        }

    @Test
    void testEvents() throws Exception
        {
        NamedCache<Object, Object> cache = ensureCache("people");
        int count = EventsHelper.getTotalListenerCount(cache);

        NamedCacheClient<String, Person> people = createClient("people");

        // Wait for the listeners
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(cache), is(count + 3));

        people.put("homer", new Person("Homer", "Simpson", 45, "male"));
        people.put("marge", new Person("Marge", "Simpson", 43, "female"));
        people.put("bart", new Person("Bart", "Simpson", 12, "male"));
        people.put("lisa", new Person("Lisa", "Simpson", 10, "female"));
        people.put("maggie", new Person("Maggie", "Simpson", 2, "female"));

        people.invoke("homer", new Uppercase());
        people.invoke("bart", new Uppercase());

        people.remove("bart");
        people.remove("marge");
        people.remove("lisa");
        people.remove("maggie");

        TestListener listener = getListener();

        assertThat(listener.getEvents(MapEvent.ENTRY_INSERTED), is(5));
        assertThat(listener.getEvents(MapEvent.ENTRY_UPDATED), is(2));
        assertThat(listener.getEvents(MapEvent.ENTRY_DELETED), is(4));

        // There should be an insert and an update for Bart.
        // The delete for Bart does not match the filter because the lastName
        // had been changed to uppercase.
        List<MapEvent<String, Person>> filteredEvents = listener.getFilteredEvents();
        assertThat(filteredEvents.size(), is(2));
        MapEvent<String, Person> eventOne = filteredEvents.get(0);
        MapEvent<String, Person> eventTwo = filteredEvents.get(1);
        assertThat(eventOne.getId(), is(MapEvent.ENTRY_INSERTED));
        assertThat(eventOne.getKey(), is("bart"));
        assertThat(eventTwo.getId(), is(MapEvent.ENTRY_UPDATED));
        assertThat(eventTwo.getKey(), is("bart"));
        assertThat(eventTwo.getNewValue().getLastName(), is("SIMPSON"));

        // Transformed events should just be inserts with the person's firstName as the new value
        List<MapEvent<String, String>> transformedEvents = listener.getTransformedEvents();
        assertThat(transformedEvents.size(), is(5));
        assertThat(transformedEvents.get(0).getNewValue(), is("Homer"));
        assertThat(transformedEvents.get(1).getNewValue(), is("Marge"));
        assertThat(transformedEvents.get(2).getNewValue(), is("Bart"));
        assertThat(transformedEvents.get(3).getNewValue(), is("Lisa"));
        assertThat(transformedEvents.get(4).getNewValue(), is("Maggie"));
        }

    // ----- helper methods -------------------------------------------------

    TestListener getListener()
        {
        return CDI.current().select(TestListener.class).get();
        }


    int getEvents(int id)
        {
        TestListener l = getListener();
        return l == null ? 0 : l.getEvents(id);
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

        return (NamedCacheClient<K, V>) cacheInstance.get();
        }

    protected <K, V> NamedCache<K, V> ensureCache(String sName)
        {
        return s_ccf.ensureCache(sName, null);
        }

    // ---- helper classes --------------------------------------------------

    @ApplicationScoped
    public static class TestListener
        {
        private final Map<Integer, Integer> events = new HashMap<>();

        private final List<MapEvent<String, Person>> filteredEvents = new ArrayList<>();

        private final List<MapEvent<String, String>> transformedEvents = new ArrayList<>();

        synchronized Integer getEvents(int id)
            {
            return events.get(id);
            }

        synchronized public List<MapEvent<String, Person>> getFilteredEvents()
            {
            return filteredEvents;
            }

        synchronized public List<MapEvent<String, String>> getTransformedEvents()
            {
            return transformedEvents;
            }

        synchronized private void record(MapEvent<String, Person> event)
            {
            System.out.println("Received event: " + event);
            events.compute(event.getId(), (k, v) -> v == null ? 1 : v + 1);
            }

        @Synchronous
        private void onPersonInserted(@Observes @Remote("test") @Inserted @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getNewValue().getLastName(), is("Simpson"));
            }

        @Synchronous
        private void onPersonUpdated(@Observes @Remote @Updated @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getOldValue().getLastName(), is("Simpson"));
            assertThat(event.getNewValue().getLastName(), is("SIMPSON"));
            }

        @Synchronous
        private void onPersonDeleted(@Observes @Remote @Deleted @CacheName("people") MapEvent<String, Person> event)
            {
            record(event);
            }

        @Synchronous
        @WhereFilter("firstName = 'Bart' and lastName = 'Simpson'")
        private void onHomer(@Observes @Remote @CacheName("people") MapEvent<String, Person> event)
            {
            filteredEvents.add(event);
            }

        @Synchronous
        @PropertyExtractor("firstName")
        private void onPersonInsertedTransformed(@Observes @Remote @Inserted @MapName("people") MapEvent<String, String> event)
            {
            transformedEvents.add(event);
            }
        }


    // ----- data members ---------------------------------------------------

    protected static ConfigurableCacheFactory s_ccf;

    private static Server s_server;
    }
