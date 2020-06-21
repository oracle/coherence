/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.ExtractorProducer;
import com.oracle.coherence.cdi.FilterProducer;
import com.oracle.coherence.cdi.MapEventTransformerProducer;
import com.oracle.coherence.cdi.PropertyExtractor;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.WhereFilter;

import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Deleted;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.ScopeName;
import com.oracle.coherence.cdi.events.ServiceName;
import com.oracle.coherence.cdi.events.Synchronous;
import com.oracle.coherence.cdi.events.Updated;

import com.oracle.coherence.cdi.server.data.Person;
import com.oracle.coherence.cdi.server.data.PhoneNumber;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link CdiInterceptorSupport} using the Weld JUnit
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
                                                          .addExtension(new CoherenceExtension())
                                                          .addExtension(new CoherenceServerExtension())
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addBeanClass(EventsScope.class)
                                                          .addBeanClass(CdiMapListenerManager.class)
                                                          .addBeanClass(ExtractorProducer.class)
                                                          .addBeanClass(ExtractorProducer.UniversalExtractorSupplier.class)
                                                          .addBeanClass(ExtractorProducer.UniversalExtractorsSupplier.class)
                                                          .addBeanClass(FilterProducer.class)
                                                          .addBeanClass(FilterProducer.WhereFilterSupplier.class)
                                                          .addBeanClass(MapEventTransformerProducer.class)
                                                          .addBeanClass(TestListener.class));

    @ApplicationScoped
    @Named("client-events")
    @ConfigUri("cdi-events-config.xml")
    private static class EventsScope
            implements ScopeInitializer
        {}

    @Inject
    @Scope("client-events")
    private ConfigurableCacheFactory ccf;

    @Inject
    private TestListener listener;

    @Test
    void testEvents()
        {
        NamedCache<String, Person> people = ccf.ensureCache("people", null);

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

        assertThat(listener.getEvents(MapEvent.ENTRY_INSERTED), is(5));
        assertThat(listener.getEvents(MapEvent.ENTRY_UPDATED), is(2));
        assertThat(listener.getEvents(MapEvent.ENTRY_DELETED), is(4));

        /*
          as: commented out until we add transformer support that was
              removed from the server extension back in where appropriate
              
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

         */
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
            System.out.println(event);
            events.compute(event.getId(), (k, v) -> v == null ? 1 : v + 1);
            }

        @Synchronous
        private void onPersonInserted(@Observes @Inserted @ScopeName("client-events") @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getNewValue().getLastName(), is("Simpson"));
            }

        @Synchronous
        private void onPersonUpdated(@Observes @Updated @ServiceName("People") @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getOldValue().getLastName(), is("Simpson"));
            assertThat(event.getNewValue().getLastName(), is("SIMPSON"));
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
