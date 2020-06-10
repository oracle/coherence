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
import com.oracle.coherence.cdi.events.CacheName;
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
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.Processor;
import com.oracle.coherence.cdi.events.Recovered;
import com.oracle.coherence.cdi.events.Removed;
import com.oracle.coherence.cdi.events.Removing;
import com.oracle.coherence.cdi.events.Rollback;
import com.oracle.coherence.cdi.events.ServiceName;
import com.oracle.coherence.cdi.events.Truncated;
import com.oracle.coherence.cdi.events.Updated;
import com.oracle.coherence.cdi.events.Updating;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.internal.ConfigurableCacheFactoryDispatcher;
import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import java.lang.annotation.Annotation;
import java.util.EnumSet;

import javax.enterprise.inject.spi.ObserverMethod;


/**
 * Manages registration of CDI observers with {@link InterceptorRegistry}
 * upon {@link ConfigurableCacheFactory} activation, and their subsequent
 * unregistration on deactivation.
 *
 * @author Aleks Seovic  2020.04.03
 *
 * @since 14.1.2
 */
class CdiInterceptorSupport
    {
    // ---- inner class: EventHandler ---------------------------------------

    /**
     * Abstract base class for all observer-based interceptors.
     *
     * @param <E>  the type of {@link com.tangosol.net.events.Event} this interceptor accepts
     * @param <T>  the enumeration of event types E supports
     */
    static abstract class EventHandler<E extends com.tangosol.net.events.Event<T>, T extends Enum<T>>
            implements EventDispatcherAwareInterceptor<E>
        {
        // ---- constructor -------------------------------------------------

        /**
         * Construct {@code EventHandler} instance.
         * 
         * @param observer        the observer method to delegate events to
         * @param classEventType  the class of event type enumeration
         */
        protected EventHandler(ObserverMethod<E> observer, Class<T> classEventType)
            {
            m_observer = observer;
            m_setTypes = EnumSet.noneOf(classEventType);
            }

        // ---- EventDispatcherAwareInterceptor interface -------------------
        
        @Override
        public void introduceEventDispatcher(String sIdentifier, com.tangosol.net.events.EventDispatcher dispatcher)
            {
            if (isApplicable(dispatcher))
                {
                dispatcher.addEventInterceptor(getId(), this, eventTypes(), false);
                }
            }

        @Override
        public void onEvent(E event)
            {
            m_observer.notify(event);
            }

        // ---- helpers -----------------------------------------------------

        /**
         * Return a unique identifier for this interceptor.
         * 
         * @return a unique identifier for this interceptor
         */
        protected String getId()
            {
            return m_observer.toString();
            }

        /**
         * Return {@code true} if this interceptor should be registered with
         * a specified dispatcher.
         *
         * @param dispatcher  a dispatcher to register this interceptor with
         *
         * @return {@code true} if this interceptor should be registered with
         *                      a specified dispatcher; {@code false} otherwise
         */
        protected abstract boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher);

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

        // ---- data members ------------------------------------------------

        protected final ObserverMethod<E> m_observer;
        protected final EnumSet<T> m_setTypes;
        }
    
    // ---- inner class: LifecycleEventHandler ------------------------------

    /**
     * Handler for {@link LifecycleEvent}s.
     */
    static class LifecycleEventHandler
            extends EventHandler<LifecycleEvent, LifecycleEvent.Type>
        {
        LifecycleEventHandler(ObserverMethod<LifecycleEvent> observer)
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
        protected boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher)
            {
            return dispatcher instanceof ConfigurableCacheFactoryDispatcher;
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
       protected CacheEventHandler(ObserverMethod<E> observer, Class<T> classType)
           {
           super(observer, classType);
           
           String cache = null;
           String service = null;

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
               }
           
           m_cacheName   = cache;
           m_serviceName = service;
           }

       @Override
       protected boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher)
           {
           if (dispatcher instanceof PartitionedCacheDispatcher)
               {
               PartitionedCacheDispatcher pcd = (PartitionedCacheDispatcher) dispatcher;
               return ((m_cacheName == null || m_cacheName.equals(pcd.getCacheName())) &&
                       (m_serviceName == null || m_serviceName.equals(pcd.getServiceName())));
               }
           
           return false;
           }

       // ---- data members ----------------------------------------------------

       protected final String m_cacheName;
       protected final String m_serviceName;
       }
    
    // ---- inner class: CacheLifecycleEventHandler -------------------------

    /**
     * Handler for {@link CacheLifecycleEvent}s.
     */
    static class CacheLifecycleEventHandler
            extends CacheEventHandler<CacheLifecycleEvent, CacheLifecycleEvent.Type>
        {
        CacheLifecycleEventHandler(ObserverMethod<CacheLifecycleEvent> observer)
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
    static class EntryEventHandler<K, V>
            extends CacheEventHandler<EntryEvent<K, V>, EntryEvent.Type>
        {
        EntryEventHandler(ObserverMethod<EntryEvent<K,V>> observer)
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
    static class EntryProcessorEventHandler
            extends CacheEventHandler<EntryProcessorEvent, EntryProcessorEvent.Type>
        {
        EntryProcessorEventHandler(ObserverMethod<EntryProcessorEvent> observer)
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

        @Override
        public void onEvent(EntryProcessorEvent event)
            {
            if (m_classProcessor == null || m_classProcessor.equals(event.getProcessor().getClass()))
                {
                m_observer.notify(event);
                }
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
   static abstract class ServiceEventHandler<E extends com.tangosol.net.events.Event<T>, T extends Enum<T>>
           extends EventHandler<E, T>
       {
       protected ServiceEventHandler(ObserverMethod<E> observer, Class<T> classType)
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
       protected boolean isApplicable(com.tangosol.net.events.EventDispatcher dispatcher)
           {
           if (dispatcher instanceof PartitionedServiceDispatcher)
               {
               PartitionedServiceDispatcher pcd = (PartitionedServiceDispatcher) dispatcher;
               return m_serviceName == null || m_serviceName.equals(pcd.getServiceName());
               }

           return false;
           }

       // ---- data members ----------------------------------------------------

       protected final String m_serviceName;
       }

    // ---- inner class: TransactionEventHandler ----------------------------

    /**
     * Handler for {@link TransactionEvent}s.
     */
    static class TransactionEventHandler
            extends ServiceEventHandler<TransactionEvent, TransactionEvent.Type>
        {
        TransactionEventHandler(ObserverMethod<TransactionEvent> observer)
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
    static class TransferEventHandler
            extends ServiceEventHandler<TransferEvent, TransferEvent.Type>
        {
        TransferEventHandler(ObserverMethod<TransferEvent> observer)
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
    static class UnsolicitedCommitEventHandler
            extends ServiceEventHandler<UnsolicitedCommitEvent, UnsolicitedCommitEvent.Type>
        {
        UnsolicitedCommitEventHandler(ObserverMethod<UnsolicitedCommitEvent> observer)
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
