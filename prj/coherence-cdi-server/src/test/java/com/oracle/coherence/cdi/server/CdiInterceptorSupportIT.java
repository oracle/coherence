/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.event.EventObserverSupport;
import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.inject.ConfigUri;
import com.oracle.coherence.inject.Name;
import com.oracle.coherence.inject.Scope;
import com.oracle.coherence.inject.SessionInitializer;
import com.oracle.coherence.cdi.SessionProducer;
import com.oracle.coherence.event.MapName;
import com.oracle.coherence.event.ScopeName;
import com.oracle.coherence.event.ServiceName;
import com.oracle.coherence.event.CacheName;
import com.oracle.coherence.event.Created;
import com.oracle.coherence.event.Destroyed;
import com.oracle.coherence.event.Executed;
import com.oracle.coherence.event.Executing;
import com.oracle.coherence.event.Inserted;
import com.oracle.coherence.event.Processor;
import com.oracle.coherence.event.Removed;
import com.oracle.coherence.event.Updated;

import com.oracle.coherence.cdi.server.data.Person;
import com.oracle.coherence.cdi.server.data.PhoneNumber;

import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.Event;

import com.tangosol.net.events.application.LifecycleEvent;

import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import com.tangosol.util.InvocableMap;

import java.time.LocalDate;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link EventObserverSupport} using the Weld JUnit
 * extension.
 *
 * @author Aleks Seovic  2020.04.03
*/
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CdiInterceptorSupportIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addExtension(new CoherenceServerExtension())
                                                          .addBeanClass(SessionProducer.class)
                                                          .addBeanClass(EventsSession.class)
                                                          .addBeanClass(TestObservers.class));

    @ApplicationScoped
    @Named("Events")
    @Scope("server-events")
    @ConfigUri("cdi-events-config.xml")
    private static class EventsSession
            implements SessionInitializer
        {}

    @Inject
    @Name("Events")
    private Session session;

    @Inject
    private TestObservers observers;

    @Test
    void testEvents()
        {
        NamedCache<String, Person> people = session.getCache("people");
        people.put("homer", new Person("Homer", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("marge", new Person("Marge", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("bart", new Person("Bart", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("lisa", new Person("Lisa", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("maggie", new Person("Maggie", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));

        people.invokeAll(new Uppercase());

        people.clear();
        people.truncate();
        people.destroy();

        Coherence.getInstances().forEach(Coherence::close);

        Set<Enum<?>> events = observers.getEvents();

        assertThat(events, hasItem(LifecycleEvent.Type.ACTIVATING));
        assertThat(events, hasItem(LifecycleEvent.Type.ACTIVATED));
        assertThat(events, hasItem(LifecycleEvent.Type.DISPOSING));
        assertThat(events, hasItem(CacheLifecycleEvent.Type.CREATED));
        assertThat(events, hasItem(CacheLifecycleEvent.Type.DESTROYED));
        assertThat(events, hasItem(CacheLifecycleEvent.Type.TRUNCATED));
        assertThat(events, hasItem(TransferEvent.Type.ASSIGNED));
        assertThat(events, hasItem(TransactionEvent.Type.COMMITTING));
        assertThat(events, hasItem(TransactionEvent.Type.COMMITTED));
        assertThat(events, hasItem(EntryProcessorEvent.Type.EXECUTING));
        assertThat(events, hasItem(EntryProcessorEvent.Type.EXECUTED));
        assertThat(events, hasItem(EntryEvent.Type.INSERTING));
        assertThat(events, hasItem(EntryEvent.Type.INSERTED));
        assertThat(events, hasItem(EntryEvent.Type.UPDATING));
        assertThat(events, hasItem(EntryEvent.Type.UPDATED));
        assertThat(events, hasItem(EntryEvent.Type.REMOVING));
        assertThat(events, hasItem(EntryEvent.Type.REMOVED));
        assertThat(events, hasItem(CoherenceLifecycleEvent.Type.STARTED));
        assertThat(events, hasItem(CoherenceLifecycleEvent.Type.STARTING));
        assertThat(events, hasItem(CoherenceLifecycleEvent.Type.STOPPED));
        assertThat(events, hasItem(CoherenceLifecycleEvent.Type.STOPPING));
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
    public static class TestObservers
        {
        private final Map<Enum<?>, Boolean> events = new ConcurrentHashMap<>();

        Set<Enum<?>> getEvents()
            {
            Set<Enum<?>> set = new TreeSet<>(Comparator.comparing(Enum::name));
            set.addAll(events.keySet());
            return set;
            }

        void record(Event<?> event)
            {
            events.put(event.getType(), true);
            }

        // lifecycle events
        private void onCoherenceLifecycleEvent(@Observes CoherenceLifecycleEvent event)
            {
            record(event);
            }

        // lifecycle events
        private void onLifecycleEvent(@Observes LifecycleEvent event)
            {
            record(event);
            }

        // transfer events
        private void onTransferEvent(@Observes @ScopeName("server-events") @ServiceName("People") TransferEvent event)
            {
            record(event);
            }

        // transaction events
        private void onTransactionEvent(@Observes TransactionEvent event)
            {
            record(event);
            }

        // cache lifecycle events
        private void onCacheLifecycleEvent(@Observes @ServiceName("People") CacheLifecycleEvent event)
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
            assertThat(event.getValue().getLastName(), is("Simpson"));
            }

        private void onPersonUpdated(@Observes @Updated @CacheName("people") EntryEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getValue().getLastName(), is("SIMPSON"));
            }

        private void onPersonRemoved(@Observes @Removed @CacheName("people") EntryEvent<String, Person> event)
            {
            record(event);
            assertThat(event.getOriginalValue().getLastName(), is("SIMPSON"));
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
