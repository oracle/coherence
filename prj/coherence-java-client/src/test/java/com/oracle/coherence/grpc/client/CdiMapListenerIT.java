/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;
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
import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

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

        TestListener listener = getListener();

        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_INSERTED), is(5));
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_UPDATED), is(2));
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_DELETED), is(4));

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

        @Synchronous
        @WhereFilter("firstName = 'Bart' and lastName = 'Simpson'")
        private void onBart(@Observes @Remote @MapName("people") MapEvent<String, Person> event)
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
