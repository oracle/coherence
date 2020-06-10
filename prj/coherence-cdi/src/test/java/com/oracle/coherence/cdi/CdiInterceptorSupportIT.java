/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.MapName;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.oracle.coherence.cdi.data.Person;
import com.oracle.coherence.cdi.data.PhoneNumber;
import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Created;
import com.oracle.coherence.cdi.events.Destroyed;
import com.oracle.coherence.cdi.events.Executed;
import com.oracle.coherence.cdi.events.Executing;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.Processor;
import com.oracle.coherence.cdi.events.Removed;
import com.oracle.coherence.cdi.events.Updated;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.events.Event;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;
import com.tangosol.util.InvocableMap;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link CdiInterceptorSupport} using the Weld JUnit
 * extension.
 *
 * @author as  2020.04.03
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CdiInterceptorSupportIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addBeanClass(TestObservers.class));

    @Inject
    @Name("cdi-events-cache-config.xml")
    private ConfigurableCacheFactory ccf;

    @Inject
    private TestObservers observers;

    @Test
    void testEvents()
        {
        ccf.activate();

        NamedCache<String, Person> people = ccf.ensureCache("people", null);
        people.put("homer", new Person("Homer", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("marge", new Person("Marge", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("bart", new Person("Bart", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("lisa", new Person("Lisa", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("maggie", new Person("Maggie", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));

        people.invokeAll(new Uppercase());

        people.clear();
        people.truncate();
        people.destroy();

        ccf.dispose();

        assertThat(observers.getEvents(), hasItem(LifecycleEvent.Type.ACTIVATING));
        assertThat(observers.getEvents(), hasItem(LifecycleEvent.Type.ACTIVATED));
        assertThat(observers.getEvents(), hasItem(LifecycleEvent.Type.DISPOSING));
        assertThat(observers.getEvents(), hasItem(CacheLifecycleEvent.Type.CREATED));
        assertThat(observers.getEvents(), hasItem(CacheLifecycleEvent.Type.DESTROYED));
        assertThat(observers.getEvents(), hasItem(CacheLifecycleEvent.Type.TRUNCATED));
        assertThat(observers.getEvents(), hasItem(TransferEvent.Type.ASSIGNED));
        assertThat(observers.getEvents(), hasItem(TransactionEvent.Type.COMMITTING));
        assertThat(observers.getEvents(), hasItem(TransactionEvent.Type.COMMITTED));
        assertThat(observers.getEvents(), hasItem(EntryProcessorEvent.Type.EXECUTING));
        assertThat(observers.getEvents(), hasItem(EntryProcessorEvent.Type.EXECUTED));
        assertThat(observers.getEvents(), hasItem(EntryEvent.Type.INSERTING));
        assertThat(observers.getEvents(), hasItem(EntryEvent.Type.INSERTED));
        assertThat(observers.getEvents(), hasItem(EntryEvent.Type.UPDATING));
        assertThat(observers.getEvents(), hasItem(EntryEvent.Type.UPDATED));
        assertThat(observers.getEvents(), hasItem(EntryEvent.Type.REMOVING));
        assertThat(observers.getEvents(), hasItem(EntryEvent.Type.REMOVED));
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
    public static class TestObservers
        {
        private Set<Enum> events = new TreeSet<>(Comparator.comparing(Enum::name));

        Set<Enum> getEvents()
            {
            return events;
            }

        private void record(Event<?> event)
            {
            events.add(event.getType());
            }

        // lifecycle events
        private void onLifecycleEvent(@Observes LifecycleEvent event)
            {
            record(event);
            }

        // transfer events
        private void onTransferEvent(@Observes TransferEvent event)
            {
            record(event);
            }

        // transaction events
        private void onTransactionEvent(@Observes TransactionEvent event)
            {
            record(event);
            }

        // cache lifecycle events
        private void onCacheLifecycleEvent(@Observes CacheLifecycleEvent event)
            {
            record(event);
            }

        private void onCreatedPeople(@Observes @Created @MapName("people") CacheLifecycleEvent event)
            {
            record(event);
            assertThat(event.getCacheName(), is("people"));
            }

        private void onDestroyedPeople(@Observes @Destroyed @CacheName("people") CacheLifecycleEvent event)
            {
            record(event);
            assertThat(event.getCacheName(), is("people"));
            }

        // entry events
        private void onEntryEvent(@Observes @MapName("people") EntryEvent<String, Person> event)
            {
            record(event);
            }

        private void onPersonInserted(@Observes @Inserted @CacheName("people") EntryEvent<String, Person> event)
            {
            record(event);
            event.forEach(entry -> assertThat(entry.getValue().getLastName(), is("Simpson")));
            }

        private void onPersonUpdated(@Observes @Updated @CacheName("people") EntryEvent<String, Person> event)
            {
            record(event);
            event.forEach(entry -> assertThat(entry.getValue().getLastName(), is("SIMPSON")));
            }

        private void onPersonRemoved(@Observes @Removed @CacheName("people") EntryEvent<String, Person> event)
            {
            record(event);
            event.forEach(entry -> assertThat(entry.getOriginalValue().getLastName(), is("SIMPSON")));
            }

        private void onExecuting(@Observes @Executing @CacheName("people") @Processor(Uppercase.class) EntryProcessorEvent event)
            {
            record(event);
            assertThat(event.getProcessor(), is(instanceOf(Uppercase.class)));
            assertThat(event.getEntrySet().size(), is(5));
            }

        private void onExecuted(@Observes @Executed @CacheName("people") @Processor(Uppercase.class) EntryProcessorEvent event)
            {
            record(event);
            assertThat(event.getProcessor(), is(instanceOf(Uppercase.class)));
            assertThat(event.getEntrySet().size(), is(0));
            }
        }
    }
