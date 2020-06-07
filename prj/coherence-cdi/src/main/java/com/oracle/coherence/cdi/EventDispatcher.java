/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.Activated;
import com.oracle.coherence.cdi.events.Activating;
import com.oracle.coherence.cdi.events.Arrived;
import com.oracle.coherence.cdi.events.Assigned;
import com.oracle.coherence.cdi.events.Cache;
import com.oracle.coherence.cdi.events.Committed;
import com.oracle.coherence.cdi.events.Committing;
import com.oracle.coherence.cdi.events.Created;
import com.oracle.coherence.cdi.events.Departed;
import com.oracle.coherence.cdi.events.Departing;
import com.oracle.coherence.cdi.events.Destroyed;
import com.oracle.coherence.cdi.events.Disposing;
import com.oracle.coherence.cdi.events.Executed;
import com.oracle.coherence.cdi.events.Executing;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.Inserting;
import com.oracle.coherence.cdi.events.Lost;
import com.oracle.coherence.cdi.events.Processor;
import com.oracle.coherence.cdi.events.Recovered;
import com.oracle.coherence.cdi.events.Removed;
import com.oracle.coherence.cdi.events.Removing;
import com.oracle.coherence.cdi.events.Rollback;
import com.oracle.coherence.cdi.events.Service;
import com.oracle.coherence.cdi.events.Truncated;
import com.oracle.coherence.cdi.events.Updated;
import com.oracle.coherence.cdi.events.Updating;

import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.application.LifecycleEvent;

import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * EventDispatcher is an {@link com.tangosol.net.events.EventInterceptor}
 * implementation that listens to all Coherence server-side events and publishes
 * them to registered CDI observers.
 *
 * @author Aleks Seovic  2020.04.03
 */
@ApplicationScoped
@Named("com.oracle.coherence.cdi.EventDispatcher")
public class EventDispatcher
        implements EventDispatcherAwareInterceptor<com.tangosol.net.events.Event<?>>
    {
    // ---- EventDispatcherAwareInterceptor interface -----------------------

    @Override
    public void onEvent(com.tangosol.net.events.Event<?> event)
        {
        // no-op, as EventDispatcher never actually registers itself as an
        // interceptor. Its only purpose is to discover and register all
        // available EventHandlers with standard Coherence event dispatchers.
        }

    @Override
    public void introduceEventDispatcher(String id, com.tangosol.net.events.EventDispatcher eventDispatcher)
        {
        m_eventHandlers.stream()
                .map(EventHandler::getEventInterceptor)
                .forEach(eventDispatcher::addEventInterceptor);
        }

    // ---- inner interface: EventHandler -----------------------------------

    /**
     * Marker interface that must be implemented by event interceptors
     * responsible for adapting Coherence events to CDI events.
     * <p>
     * This interface is used for runtime discovery of available event
     * interceptors, so each concrete class that implements this interface
     * must also implement {@link EventInterceptor} interface.
     */
    interface EventHandler
        {
        @SuppressWarnings("rawtypes")
        default EventInterceptor getEventInterceptor()
            {
            return (EventInterceptor) this;
            }
        }

    // ---- inner class: LifecycleEventHandler ------------------------------

    /**
     * Handler for {@link LifecycleEvent}s.
     */
    @ApplicationScoped
    static class LifecycleEventHandler
            implements EventHandler, EventInterceptor<LifecycleEvent>
        {
        @Override
        public void onEvent(LifecycleEvent event)
            {
            Event<LifecycleEvent> e = m_lifecycleEvent;
            
            switch (event.getType())
                {
                case ACTIVATING:
                    e = e.select(Activating.Literal.INSTANCE);
                    break;
                case ACTIVATED:
                    e = e.select(Activated.Literal.INSTANCE);
                    break;
                case DISPOSING:
                    e = e.select(Disposing.Literal.INSTANCE);
                    break;
                }

            e.fireAsync(event);
            e.fire(event);
            }

        // ---- data members ----------------------------------------------------

        @Inject
        private Event<LifecycleEvent> m_lifecycleEvent;
        }

    // ---- inner class: CacheLifecycleEventHandler -------------------------

    /**
     * Handler for {@link CacheLifecycleEvent}s.
     */
    @ApplicationScoped
    static class CacheLifecycleEventHandler
            implements EventHandler, EventInterceptor<CacheLifecycleEvent>
        {
        @Override
        public void onEvent(CacheLifecycleEvent event)
            {
            Cache cache   = Cache.Literal.of(event.getCacheName());
            Service service = Service.Literal.of(event.getService().getInfo().getServiceName());

            Event<CacheLifecycleEvent> e = m_cacheLifecycleEvent.select(cache, service);
                    
            switch (event.getType())
                {
                case CREATED:
                    e = e.select(Created.Literal.INSTANCE);
                    break;
                case DESTROYED:
                    e = e.select(Destroyed.Literal.INSTANCE);
                    break;
                case TRUNCATED:
                    e = e.select(Truncated.Literal.INSTANCE);
                    break;
                }

            e.fireAsync(event);
            e.fire(event);
            }

        // ---- data members ----------------------------------------------------

        @Inject
        private Event<CacheLifecycleEvent> m_cacheLifecycleEvent;
        }

    // ---- inner class: EntryEventHandler ----------------------------------

    /**
     * Handler for {@link EntryEvent}s.
     */
    @ApplicationScoped
    static class EntryEventHandler
            implements EventHandler, EventInterceptor<EntryEvent<?, ?>>
        {
        @Override
        public void onEvent(EntryEvent<?, ?> event)
            {
            Cache cache   = Cache.Literal.of(event.getCacheName());
            Service service = Service.Literal.of(event.getService().getInfo().getServiceName());

            Event<EntryEvent<?, ?>> e = m_entryEvent.select(cache, service);
            
            switch (event.getType())
                {
                case INSERTING:
                    e = e.select(Inserting.Literal.INSTANCE);
                    break;
                case INSERTED:
                    e = e.select(Inserted.Literal.INSTANCE);
                    break;
                case UPDATING:
                    e = e.select(Updating.Literal.INSTANCE);
                    break;
                case UPDATED:
                    e = e.select(Updated.Literal.INSTANCE);
                    break;
                case REMOVING:
                    e = e.select(Removing.Literal.INSTANCE);
                    break;
                case REMOVED:
                    e = e.select(Removed.Literal.INSTANCE);
                    break;
                }

            e.fireAsync(event);
            e.fire(event);
            }

        // ---- data members ----------------------------------------------------

        @Inject
        private Event<EntryEvent<?, ?>> m_entryEvent;
        }

    // ---- inner class: EntryProcessorEventHandler -------------------------

    /**
     * Handler for {@link EntryProcessorEvent}s.
     */
    @ApplicationScoped
    static class EntryProcessorEventHandler
            implements EventHandler, EventInterceptor<EntryProcessorEvent>
        {
        @Override
        public void onEvent(EntryProcessorEvent event)
            {
            Cache cache     = Cache.Literal.of(event.getCacheName());
            Service service   = Service.Literal.of(event.getService().getInfo().getServiceName());
            Processor   processor = Processor.Literal.of(event.getProcessor().getClass());

            Event<EntryProcessorEvent> e = m_entryProcessorEvent.select(cache, service, processor);

            switch (event.getType())
                {
                case EXECUTING:
                    e = e.select(Executing.Literal.INSTANCE);
                    break;
                case EXECUTED:
                    e = e.select(Executed.Literal.INSTANCE);
                    break;
                }

            e.fireAsync(event);
            e.fire(event);
            }

        // ---- data members ----------------------------------------------------

        @Inject
        private Event<EntryProcessorEvent> m_entryProcessorEvent;
        }

    // ---- inner class: TransactionEventHandler ----------------------------

    /**
     * Handler for {@link TransactionEvent}s.
     */
    @ApplicationScoped
    static class TransactionEventHandler
            implements EventHandler, EventInterceptor<TransactionEvent>
        {
        @Override
        public void onEvent(TransactionEvent event)
            {
            Service service = Service.Literal.of(event.getService().getInfo().getServiceName());

            Event<TransactionEvent> e = m_transactionEvent.select(service);

            switch (event.getType())
                {
                case COMMITTING:
                    e = e.select(Committing.Literal.INSTANCE);
                    break;
                case COMMITTED:
                    e = e.select(Committed.Literal.INSTANCE);
                    break;
                }

            e.fireAsync(event);
            e.fire(event);
            }

        // ---- data members ----------------------------------------------------

        @Inject
        private Event<TransactionEvent> m_transactionEvent;
        }

    // ---- inner class: TransferEventHandler -------------------------------

    /**
     * Handler for {@link com.tangosol.net.events.partition.TransferEvent}s.
     */
    @ApplicationScoped
    static class TransferEventHandler
            implements EventHandler, EventInterceptor<TransferEvent>
        {
        @Override
        public void onEvent(TransferEvent event)
            {
            Service service = Service.Literal.of(event.getService().getInfo().getServiceName());

            Event<TransferEvent> e = m_transferEvent.select(service);

            switch (event.getType())
                {
                case ASSIGNED:
                    e = e.select(Assigned.Literal.INSTANCE);
                    break;
                case ARRIVED:
                    e = e.select(Arrived.Literal.INSTANCE);
                    break;
                case DEPARTING:
                    e = e.select(Departing.Literal.INSTANCE);
                    break;
                case DEPARTED:
                    e = e.select(Departed.Literal.INSTANCE);
                    break;
                case LOST:
                    e = e.select(Lost.Literal.INSTANCE);
                    break;
                case RECOVERED:
                    e = e.select(Recovered.Literal.INSTANCE);
                    break;
                case ROLLBACK:
                    e = e.select(Rollback.Literal.INSTANCE);
                    break;
                }

            e.fireAsync(event);
            e.fire(event);
            }

        // ---- data members ----------------------------------------------------

        @Inject
        private Event<TransferEvent> m_transferEvent;
        }

    // ---- inner class: UnsolicitedCommitEventHandler ----------------------

    /**
     * Handler for {@link com.tangosol.net.events.partition.UnsolicitedCommitEvent}s.
     */
    @ApplicationScoped
    static class UnsolicitedCommitEventHandler
            implements EventHandler, EventInterceptor<UnsolicitedCommitEvent>
        {
        @Override
        public void onEvent(UnsolicitedCommitEvent event)
            {
            Service service = Service.Literal.of(event.getService().getInfo().getServiceName());

            Event<UnsolicitedCommitEvent> e = m_unsolicitedCommitEvent.select(service);

            if (event.getType() == UnsolicitedCommitEvent.Type.COMMITTED)
                {
                e = e.select(Committed.Literal.INSTANCE);
                }

            e.fireAsync(event);
            e.fire(event);
            }

        // ---- data members ----------------------------------------------------

        @Inject
        private Event<UnsolicitedCommitEvent> m_unsolicitedCommitEvent;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Discovered {@link EventHandler}s.
     */
    @Inject
    private Instance<EventHandler> m_eventHandlers;
    }
