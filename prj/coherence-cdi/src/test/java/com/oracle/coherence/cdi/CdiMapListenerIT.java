/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.data.Person;
import com.oracle.coherence.cdi.data.PhoneNumber;

import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Deleted;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.Synchronous;
import com.oracle.coherence.cdi.events.Updated;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import javax.inject.Inject;

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
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addBeanClass(CdiMapListenerManager.class)
                                                          .addBeanClass(TestListener.class));

    @Inject
    @Scope("cdi-events-config.xml")
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

        people.remove("marge");
        people.remove("lisa");
        people.remove("maggie");

        assertThat(listener.getEvents(MapEvent.ENTRY_INSERTED), is(5));
        assertThat(listener.getEvents(MapEvent.ENTRY_UPDATED), is(2));
        assertThat(listener.getEvents(MapEvent.ENTRY_DELETED), is(3));
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

    @SuppressWarnings("unchecked")
    @ApplicationScoped
    public static class TestListener
        {
        private Map<Integer, Integer> events = new HashMap<>();

        Integer getEvents(int id)
            {
            return events.get(id);
            }

        private void record(MapEvent<String, Person> event)
            {
            System.out.println(event);
            events.compute(event.getId(), (k, v) -> v == null ? 1 : v + 1);
            }

        @Synchronous
        private void onPersonInserted(@Observes @Inserted @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getNewValue().getLastName(), is("Simpson"));
            }

        @Synchronous
        private void onPersonUpdated(@Observes @Updated @MapName("people") MapEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getOldValue().getLastName(), is("Simpson"));
            assertThat(event.getNewValue().getLastName(), is("SIMPSON"));
            }

        @Synchronous
        private void onPersonDeleted(@Observes @Deleted @CacheName("people") MapEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getOldValue().getLastName(), is("Simpson"));
            }
        }
    }
