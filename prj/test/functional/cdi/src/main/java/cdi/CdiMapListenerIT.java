/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cdi;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.PropertyExtractor;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionInitializer;
import com.oracle.coherence.cdi.WhereFilter;
import cdi.data.Person;
import cdi.data.PhoneNumber;

import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Deleted;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.ScopeName;
import com.oracle.coherence.cdi.events.ServiceName;
import com.oracle.coherence.cdi.events.Synchronous;
import com.oracle.coherence.cdi.events.Updated;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.oracle.coherence.cdi.events.EventObserverSupport;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.event.Observes;

import javax.inject.Inject;
import javax.inject.Named;

import org.hamcrest.MatcherAssert;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.junit.jupiter.api.extension.ExtendWith;

import com.oracle.coherence.testing.util.EventsHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

/**
 * Integration test for the {@link EventObserverSupport} using the Weld JUnit
 * extension.
 *
 * @author Aleks Seovic  2020.04.03
*/
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CdiMapListenerIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                        .addPackages(CoherenceExtension.class)
                                                        .addExtension(new CoherenceExtension())
                                                        .addBeanClass(TestServerCoherenceProducer.class)
                                                        .addBeanClass(EventsSession.class)
                                                        .addBeanClass(TestListener.class));

    @ApplicationScoped
    @Named("ClientEvents")
    @Scope("client-events")
    @ConfigUri("cdi-events-config.xml")
    private static class EventsSession
            implements SessionInitializer
        {}

    @Inject
    @Name("ClientEvents")
    private Session m_session;

    @Inject
    private TestListener listener;

    @Test
    void testEvents() throws Exception
        {
        NamedCache<String, Person> people = m_session.getCache("people");

        // Wait for the listener registration as it is async
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(people), is(greaterThanOrEqualTo(3)));

        people.put("homer", new Person("Homer", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("marge", new Person("Marge", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("bart", new Person("Bart", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("lisa", new Person("Lisa", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("maggie", new Person("Maggie", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));

        people.invoke("homer", new Uppercase());
        people.invoke("bart", new Uppercase());

        people.remove("bart");
        people.remove("marge");
        people.remove("lisa");
        people.remove("maggie");

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
        MatcherAssert.assertThat(eventTwo.getNewValue().getLastName(), is("SIMPSON"));

        // Transformed events should just be inserts with the person's firstName as the new value
        List<MapEvent<String, String>> transformedEvents = listener.getTransformedEvents();
        Eventually.assertDeferred(transformedEvents::size, is(5));
        assertThat(transformedEvents.get(0).getNewValue(), is("Homer"));
        assertThat(transformedEvents.get(1).getNewValue(), is("Marge"));
        assertThat(transformedEvents.get(2).getNewValue(), is("Bart"));
        assertThat(transformedEvents.get(3).getNewValue(), is("Lisa"));
        assertThat(transformedEvents.get(4).getNewValue(), is("Maggie"));
        }

    // ---- helper classes --------------------------------------------------

    public static class Uppercase
            implements InvocableMap.EntryProcessor<String, Person, Object>
        {
        @Override
        public Object process(InvocableMap.Entry<String, Person> entry)
            {
            Person p = entry.getValue();
            p.setLastName(p.getLastName().toUpperCase());
            entry.setValue(p);
            return null;
            }
        }

    @ApplicationScoped
    public static class TestListener
        {
        private final Map<Integer, Integer> events = new ConcurrentHashMap<>();

        private final List<MapEvent<String, Person>> filteredEvents = Collections.synchronizedList(new ArrayList<>());

        private final List<MapEvent<String, String>> transformedEvents = Collections.synchronizedList(new ArrayList<>());

        Integer getEvents(int id)
            {
            return events.get(id);
            }

        public List<MapEvent<String, Person>> getFilteredEvents()
            {
            return filteredEvents;
            }

        public List<MapEvent<String, String>> getTransformedEvents()
            {
            return transformedEvents;
            }

        private void record(MapEvent<String, Person> event)
            {
            System.out.println("Received event: " + event);
            events.compute(event.getId(), (k, v) -> v == null ? 1 : v + 1);
            }

        @Synchronous
        private void onPersonInserted(@Observes @Inserted @ScopeName("client-events") @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            MatcherAssert.assertThat(event.getNewValue().getLastName(), is("Simpson"));
            }

        @Synchronous
        private void onPersonUpdated(@Observes @Updated @ServiceName("People") @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            MatcherAssert.assertThat(event.getOldValue().getLastName(), is("Simpson"));
            MatcherAssert.assertThat(event.getNewValue().getLastName(), is("SIMPSON"));
            }

        @Synchronous
        private void onPersonDeleted(@Observes @Deleted @CacheName("people") MapEvent<String, Person> event)
            {
            record(event);
            }

        @Synchronous
        @WhereFilter("firstName = 'Bart' and lastName = 'Simpson'")
        private void onHomer(@Observes  @CacheName("people") MapEvent<String, Person> event)
            {
            filteredEvents.add(event);
            }

        @Synchronous
        @PropertyExtractor("firstName")
        private void onPersonInsertedTransformed(@Observes @Inserted @MapName("people") MapEvent<String, String> event)
            {
            transformedEvents.add(event);
            }
        }
    }
