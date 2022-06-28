/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.events;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.SessionName;

import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.events.CoherenceDispatcher;
import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.SessionDispatcher;
import com.tangosol.net.events.SessionLifecycleEvent;

import com.tangosol.net.events.application.EventDispatcher;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEventDispatcher;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import java.lang.annotation.Annotation;

import java.util.EnumSet;
import java.util.Set;

import java.util.concurrent.CompletableFuture;

/**
 * Manages registration of CDI observers with {@link InterceptorRegistry}
 * upon {@link ConfigurableCacheFactory} activation, and their subsequent
 * un-registration on deactivation.
 *
 * @author Aleks Seovic  2020.04.03
 * @since 20.06
 */
public class EventObserverSupport
    {
    // ----- Factory Methods ------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends com.tangosol.net.events.Event<T>, T extends Enum<T>>
            EventHandler<E, T> createObserver(Class<E> type, EventObserver<E> observer)
        {
        if (CacheLifecycleEvent.class.equals(type))
            {
            return (EventHandler<E, T>) new CacheLifecycleEventHandler(((EventObserver<CacheLifecycleEvent>) observer));
            }
        if (CoherenceLifecycleEvent.class.equals(type))
            {
            return (EventHandler<E, T>) new CoherenceLifecycleEventHandler(((EventObserver<CoherenceLifecycleEvent>) observer));
            }
        if (EntryEvent.class.equals(type))
            {
            return (EventHandler<E, T>) new EntryEventHandler(observer);
            }
        if (EntryProcessorEvent.class.equals(type))
            {
            return (EventHandler<E, T>) new EntryProcessorEventHandler((EventObserver<EntryProcessorEvent>) observer);
            }
        if (LifecycleEvent.class.equals(type))
            {
            return (EventHandler<E, T>) new LifecycleEventHandler((EventObserver<LifecycleEvent>) observer);
            }
        if (TransactionEvent.class.equals(type))
            {
            return (EventHandler<E, T>) new TransactionEventHandler((EventObserver<TransactionEvent>) observer);
            }
        if (TransferEvent.class.equals(type))
            {
            return (EventHandler<E, T>) new TransferEventHandler((EventObserver<TransferEvent>) observer);
            }
        if (UnsolicitedCommitEvent.class.equals(type))
            {
            return (EventHandler<E, T>) new UnsolicitedCommitEventHandler((EventObserver<UnsolicitedCommitEvent>) observer);
            }
        throw new IllegalArgumentException("Unsupported event type: " + type);
        }

    // ----- inner interface: EventObserver ---------------------------------

    /**
     * An observer of a specific event type.
     *
     * @param <E>  event type
     */
    @SuppressWarnings("rawtypes")
    public interface EventObserver<E extends com.tangosol.net.events.Event>
        {
        /**
         * Return the unique identifier for this observer.
         * <p>
         * This value will be used as the identifier when registering
         * an {@link com.tangosol.net.events.EventInterceptor}.
         *
         * @return  the unique identifier for this observer
         */
        String getId();

        /**
         * Process an event.
         *
         * @param event  the event
         */
        void notify(E event);

        /**
         * Return {@code true} if this observer should be async.
         *
         * @return  {@code true} if this observer should be async
         */
        boolean isAsync();

        /**
         * Return the qualifiers for the observer that wil be
         * used to further qualify which events are received.
         *
         * @return  the qualifiers for the observer
         */
        Set<Annotation> getObservedQualifiers();
        }

    // ---- inner class: EventHandler ---------------------------------------

    /**
     * Abstract base class for all observer-based interceptors.
     *
     * @param <E>  the type of {@link com.tangosol.net.events.Event} this interceptor accepts
     * @param <T>  the enumeration of event types E supports
     */
    public static abstract class EventHandler<E extends com.tangosol.net.events.Event<T>, T extends Enum<T>>
            implements EventDispatcherAwareInterceptor<E>
        {
        // ---- constructor -------------------------------------------------

        /**
         * Construct {@code EventHandler} instance.
         *
         * @param observer        the observer method to delegate events to
         * @param classEventType  the class of event type enumeration
         */
        protected EventHandler(EventObserver<E> observer, Class<T> classEventType)
            {
            m_observer = observer;
            m_setTypes = EnumSet.noneOf(classEventType);

            String sScope = null;

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof ScopeName)
                    {
                    sScope = ((ScopeName) a).value();
                    }
                }

            m_sScopeName = sScope;
            }

        // ---- EventDispatcherAwareInterceptor interface -------------------

        @Override
        public void introduceEventDispatcher(String sIdentifier, com.tangosol.net.events.EventDispatcher dispatcher)
            {
            if (isApplicable(dispatcher, m_sScopeName))
                {
                dispatcher.addEventInterceptor(getId(), this, eventTypes(), false);
                }
            }

        @Override
        public void onEvent(E event)
            {
            if (shouldFire(event))
                {
                String sObserverScope = m_sScopeName;
                String sEventScope    = getEventScope(event);

                if (sObserverScope == null || sEventScope == null || sObserverScope.equals(sEventScope))
                    {
                    if (m_observer.isAsync())
                        {
                        CompletableFuture.supplyAsync(() ->
                                                      {
                                                      m_observer.notify(event);
                                                      return event;
                                                      });
                        }
                    else
                        {
                        m_observer.notify(event);
                        }
                    }
                }
            }

        // ---- helpers -----------------------------------------------------

        /**
         * Return a unique identifier for this interceptor.
         *
         * @return a unique identifier for this interceptor
         */
        public String getId()
            {
            return m_observer.getId();
            }

        /**
         * Return {@code true} if this interceptor should be registered with
         * a specified dispatcher.
         *
         * @param dispatcher  a dispatcher to register this interceptor with
         * @param sScopeName  a scope name the observer is interested in,
         *                    or {@code null} for all scopes
         *
         * @return {@code true} if this interceptor should be registered with
         *                      a specified dispatcher; {@code false} otherwise
         */
        protected abstract boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher, String sScopeName);

        /**
         * Return {@code true} if the event should fire.
         * <p>
         * This allows sub-classes to provide additional filtering logic and
         * prevent the observer method notification from happening even after
         * the Coherence server-side event is fired.
         *
         * @param event  the event to check
         *
         * @return {@code true} if the event should fire
         */
        protected boolean shouldFire(E event)
            {
            return true;
            }

        /**
         * Return the scope name of the {@link ConfigurableCacheFactory} the
         * specified event was raised from.
         *
         * @param event  the event to extract scope name from
         *
         * @return the scope name
         */
        protected String getEventScope(E event)
            {
            return null;
            }

        /**
         * Add specified event type to a set of types this interceptor should handle.
         *
         * @param type  the event type to add
         */
        protected void addType(T type)
            {
            m_setTypes.add(type);
            }

        /**
         * Create a final set of event types to register this interceptor for.
         *
         * @return a final set of event types to register this interceptor for
         */
        protected EnumSet<T> eventTypes()
            {
            return m_setTypes.isEmpty() ? EnumSet.complementOf(m_setTypes) : m_setTypes;
            }

        /**
         * Return the name of the scope this interceptor should be registered with.
         *
         * @return the name of the scope this interceptor should be registered with
         */
        public String getScopeName()
            {
            return m_sScopeName;
            }

        /**
         * Remove the scope prefix from a specified service name.
         *
         * @param sServiceName  the service name to remove scope prefix from
         *
         * @return service name with scope prefix removed
         */
        protected String removeScope(String sServiceName)
            {
            int nIndex = sServiceName.indexOf(':');
            return nIndex > -1 ? sServiceName.substring(nIndex + 1) : sServiceName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The observer method to delegate events to.
         */
        protected final EventObserver<E> m_observer;

        /**
         * A set of event types the observer is interested in.
         */
        protected final EnumSet<T> m_setTypes;

        /**
         * The scope name for a {@link ConfigurableCacheFactory} this
         * interceptor is interested in.
         */
        private final String m_sScopeName;
        }

    // ---- inner class: CoherenceLifecycleEventHandler ---------------------

    /**
     * Handler for {@link CoherenceLifecycleEvent}s.
     */
    public static class CoherenceLifecycleEventHandler
            extends EventHandler<CoherenceLifecycleEvent, CoherenceLifecycleEvent.Type>
        {
        public CoherenceLifecycleEventHandler(EventObserver<CoherenceLifecycleEvent> observer)
            {
            super(observer, CoherenceLifecycleEvent.Type.class);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Starting)
                    {
                    addType(CoherenceLifecycleEvent.Type.STARTING);
                    }
                else if (a instanceof Started)
                    {
                    addType(CoherenceLifecycleEvent.Type.STARTED);
                    }
                else if (a instanceof Stopping)
                    {
                    addType(CoherenceLifecycleEvent.Type.STOPPING);
                    }
                else if (a instanceof Stopped)
                    {
                    addType(CoherenceLifecycleEvent.Type.STOPPED);
                    }
                else if (a instanceof Name)
                    {
                    m_sName = ((Name) a).value();
                    }
                }
            }

        @Override
        protected boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher, String sScopeName)
            {
            return dispatcher instanceof CoherenceDispatcher
                    && (m_sName == null || ((CoherenceDispatcher) dispatcher).getName().equals(m_sName));
            }

        // ----- data members -----------------------------------------------

        private String m_sName;
        }

    // ---- inner class: SessionLifecycleEventHandler -----------------------

    /**
     * Handler for {@link SessionLifecycleEvent}s.
     */
    public static class SessionLifecycleEventHandler
            extends EventHandler<SessionLifecycleEvent, SessionLifecycleEvent.Type>
        {
        public SessionLifecycleEventHandler(EventObserver<SessionLifecycleEvent> observer)
            {
            super(observer, SessionLifecycleEvent.Type.class);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Starting)
                    {
                    addType(SessionLifecycleEvent.Type.STARTING);
                    }
                else if (a instanceof Started)
                    {
                    addType(SessionLifecycleEvent.Type.STARTED);
                    }
                else if (a instanceof Stopping)
                    {
                    addType(SessionLifecycleEvent.Type.STOPPING);
                    }
                else if (a instanceof Stopped)
                    {
                    addType(SessionLifecycleEvent.Type.STOPPED);
                    }
                else if (a instanceof Name)
                    {
                    m_sName = ((Name) a).value();
                    }
                else if (a instanceof SessionName)
                    {
                    m_sName = ((SessionName) a).value();
                    }
                }
            }

        @Override
        protected boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher, String sScopeName)
            {
            return dispatcher instanceof SessionDispatcher
                    && (m_sName == null || ((SessionDispatcher) dispatcher).getName().equals(m_sName));
            }

        // ----- data members -----------------------------------------------

        private String m_sName;
        }

    // ---- inner class: LifecycleEventHandler ------------------------------

    /**
     * Handler for {@link LifecycleEvent}s.
     */
    public static class LifecycleEventHandler
            extends EventHandler<LifecycleEvent, LifecycleEvent.Type>
        {
        public LifecycleEventHandler(EventObserver<LifecycleEvent> observer)
            {
            super(observer, LifecycleEvent.Type.class);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Activating)
                    {
                    addType(LifecycleEvent.Type.ACTIVATING);
                    }
                else if (a instanceof Activated)
                    {
                    addType(LifecycleEvent.Type.ACTIVATED);
                    }
                else if (a instanceof Disposing)
                    {
                    addType(LifecycleEvent.Type.DISPOSING);
                    }
                }
            }

        @Override
        protected boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher, String sScopeName)
            {
            return dispatcher instanceof EventDispatcher;
            }

        @Override
        protected String getEventScope(LifecycleEvent event)
            {
            return event.getConfigurableCacheFactory().getScopeName();
            }
        }

    // ---- inner class: CacheEventHandler ----------------------------------

   /**
    * Abstract base class for all observer-based cache interceptors.
    *
    * @param <E>  the type of {@link com.tangosol.net.events.Event} this interceptor accepts
    * @param <T>  the enumeration of event types E supports
    */
   static abstract class CacheEventHandler<E extends com.tangosol.net.events.Event<T>, T extends Enum<T>>
           extends EventHandler<E, T>
       {
       public CacheEventHandler(EventObserver<E> observer, Class<T> classType)
           {
           super(observer, classType);

           String cache   = null;
           String service = null;
           String session = null;

           for (Annotation a : observer.getObservedQualifiers())
               {
               if (a instanceof CacheName)
                   {
                   cache = ((CacheName) a).value();
                   }
               else if (a instanceof MapName)
                   {
                   cache = ((MapName) a).value();
                   }
               else if (a instanceof ServiceName)
                   {
                   service = ((ServiceName) a).value();
                   }
               else if (a instanceof SessionName)
                   {
                   session = ((SessionName) a).value();
                   }
               }

           m_cacheName   = cache;
           m_serviceName = service;
           m_sessionName = session;
           }

       @Override
       protected boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher, String sScopeName)
           {
           if (dispatcher instanceof CacheLifecycleEventDispatcher)
               {
               CacheLifecycleEventDispatcher cacheDispatcher = (CacheLifecycleEventDispatcher) dispatcher;

               if (sScopeName == null || sScopeName.equals(cacheDispatcher.getScopeName()))
                   {
                   return ((m_cacheName == null || m_cacheName.equals(cacheDispatcher.getCacheName())) &&
                           (m_serviceName == null || m_serviceName.equals(removeScope(cacheDispatcher.getServiceName()))));
                   }
               }

           return false;
           }

       // ---- data members ----------------------------------------------------

       protected final String m_cacheName;
       protected final String m_serviceName;
       protected final String m_sessionName;
       }

    // ---- inner class: CacheLifecycleEventHandler -------------------------

    /**
     * Handler for {@link CacheLifecycleEvent}s.
     */
    public static class CacheLifecycleEventHandler
            extends CacheEventHandler<CacheLifecycleEvent, CacheLifecycleEvent.Type>
        {
        public CacheLifecycleEventHandler(EventObserver<CacheLifecycleEvent> observer)
            {
            super(observer, CacheLifecycleEvent.Type.class);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Created)
                    {
                    addType(CacheLifecycleEvent.Type.CREATED);
                    }
                else if (a instanceof Destroyed)
                    {
                    addType(CacheLifecycleEvent.Type.DESTROYED);
                    }
                else if (a instanceof Truncated)
                    {
                    addType(CacheLifecycleEvent.Type.TRUNCATED);
                    }
                }
            }
        }

    // ---- inner class: EntryEventHandler ----------------------------------

    /**
     * Handler for {@link EntryEvent}s.
     */
    public static class EntryEventHandler<K, V>
            extends CacheEventHandler<EntryEvent<K, V>, EntryEvent.Type>
        {
        public EntryEventHandler(EventObserver<EntryEvent<K,V>> observer)
            {
            super(observer, EntryEvent.Type.class);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Inserting)
                    {
                    addType(EntryEvent.Type.INSERTING);
                    }
                else if (a instanceof Inserted)
                    {
                    addType(EntryEvent.Type.INSERTED);
                    }
                else if (a instanceof Updating)
                    {
                    addType(EntryEvent.Type.UPDATING);
                    }
                else if (a instanceof Updated)
                    {
                    addType(EntryEvent.Type.UPDATED);
                    }
                else if (a instanceof Removing)
                    {
                    addType(EntryEvent.Type.REMOVING);
                    }
                else if (a instanceof Removed)
                    {
                    addType(EntryEvent.Type.REMOVED);
                    }
                }
            }
        }

    // ---- inner class: EntryProcessorEventHandler -------------------------

    /**
     * Handler for {@link EntryProcessorEvent}s.
     */
    public static class EntryProcessorEventHandler
            extends CacheEventHandler<EntryProcessorEvent, EntryProcessorEvent.Type>
        {
        public EntryProcessorEventHandler(EventObserver<EntryProcessorEvent> observer)
            {
            super(observer, EntryProcessorEvent.Type.class);

            Class<?> classProcessor = null;

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Processor)
                    {
                    classProcessor = ((Processor) a).value();
                    }
                else if (a instanceof Executing)
                    {
                    addType(EntryProcessorEvent.Type.EXECUTING);
                    }
                else if (a instanceof Executed)
                    {
                    addType(EntryProcessorEvent.Type.EXECUTED);
                    }
                }

            m_classProcessor = classProcessor;
            }

        protected boolean shouldFire(EntryProcessorEvent event)
            {
            return m_classProcessor == null || m_classProcessor.equals(event.getProcessor().getClass());
            }

        // ---- data members ------------------------------------------------

        private final Class<?> m_classProcessor;
        }

    // ---- inner class: ServiceEventHandler --------------------------------

   /**
    * Abstract base class for all observer-based service interceptors.
    *
    * @param <E>  the type of {@link com.tangosol.net.events.Event} this interceptor accepts
    * @param <T>  the enumeration of event types E supports
    */
   public static abstract class ServiceEventHandler<E extends com.tangosol.net.events.Event<T>, T extends Enum<T>>
           extends EventHandler<E, T>
       {
       protected ServiceEventHandler(EventObserver<E> observer, Class<T> classType)
           {
           super(observer, classType);

           String service = null;

           for (Annotation a : observer.getObservedQualifiers())
               {
               if (a instanceof ServiceName)
                   {
                   service = ((ServiceName) a).value();
                   }
               }

           m_serviceName = service;
           }

       @Override
       protected boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher, String sScopeName)
           {
           if (dispatcher instanceof PartitionedServiceDispatcher)
               {
               PartitionedServiceDispatcher psd = (PartitionedServiceDispatcher) dispatcher;
               ConfigurableCacheFactory     ccf = getConfigurableCacheFactory(psd.getService());

               if (ccf == null || sScopeName == null || sScopeName.equals(ccf.getScopeName()))
                   {
                   return m_serviceName == null || m_serviceName.equals(removeScope(psd.getServiceName()));
                   }
               }

           return false;
           }

       protected ConfigurableCacheFactory getConfigurableCacheFactory(PartitionedService service)
           {
           // a bit of a hack, but it should do the job
           if (service instanceof CacheService)
               {
               CacheService pc = (CacheService) service;
               return pc.getBackingMapManager().getCacheFactory();
               }
           return null;
           }

       // ---- data members ----------------------------------------------------

       protected final String m_serviceName;
       }

    // ---- inner class: TransactionEventHandler ----------------------------

    /**
     * Handler for {@link TransactionEvent}s.
     */
    public static class TransactionEventHandler
            extends ServiceEventHandler<TransactionEvent, TransactionEvent.Type>
        {
        public TransactionEventHandler(EventObserver<TransactionEvent> observer)
            {
            super(observer, TransactionEvent.Type.class);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Committing)
                    {
                    addType(TransactionEvent.Type.COMMITTING);
                    }
                else if (a instanceof Committed)
                    {
                    addType(TransactionEvent.Type.COMMITTED);
                    }
                }
            }
        }

    // ---- inner class: TransferEventHandler -------------------------------

    /**
     * Handler for {@link TransactionEvent}s.
     */
    public static class TransferEventHandler
            extends ServiceEventHandler<TransferEvent, TransferEvent.Type>
        {
        public TransferEventHandler(EventObserver<TransferEvent> observer)
            {
            super(observer, TransferEvent.Type.class);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Assigned)
                    {
                    addType(TransferEvent.Type.ASSIGNED);
                    }
                else if (a instanceof Arrived)
                    {
                    addType(TransferEvent.Type.ARRIVED);
                    }
                else if (a instanceof Departing)
                    {
                    addType(TransferEvent.Type.DEPARTING);
                    }
                else if (a instanceof Departed)
                    {
                    addType(TransferEvent.Type.DEPARTED);
                    }
                else if (a instanceof Lost)
                    {
                    addType(TransferEvent.Type.LOST);
                    }
                else if (a instanceof Recovered)
                    {
                    addType(TransferEvent.Type.RECOVERED);
                    }
                else if (a instanceof Rollback)
                    {
                    addType(TransferEvent.Type.ROLLBACK);
                    }
                }
            }
        }

    // ---- inner class: UnsolicitedCommitEventHandler ----------------------

    /**
     * Handler for {@link UnsolicitedCommitEvent}s.
     */
    public static class UnsolicitedCommitEventHandler
            extends ServiceEventHandler<UnsolicitedCommitEvent, UnsolicitedCommitEvent.Type>
        {
        public UnsolicitedCommitEventHandler(EventObserver<UnsolicitedCommitEvent> observer)
            {
            super(observer, UnsolicitedCommitEvent.Type.class);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Committed)
                    {
                    addType(UnsolicitedCommitEvent.Type.COMMITTED);
                    }
                }
            }
        }
    }
