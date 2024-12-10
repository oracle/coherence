/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.oracle.coherence.common.base.Continuation;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheService;

import com.tangosol.net.Coherence;
import com.tangosol.net.events.EventDispatcher;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEventDispatcher;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.Event;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap.EntryProcessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link PartitionedCacheDispatcher} used by the
 * PartitionedCache.
 *
 * @author rhl/hr 2011.07.20
 * @since Coherence 12.1.2
 */
public class StorageDispatcher
        extends AbstractEventDispatcher
        implements PartitionedCacheDispatcher
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a dispatcher for the specified {@link BackingMapContext cache}.
     *
     * @param ctx  the cache associated with this dispatcher
     */
    public StorageDispatcher(BackingMapContext ctx)
        {
        super(EVENT_TYPES_STORAGE);

        f_ctxBM      = ctx;
        f_sCacheName = ctx.getCacheName();
        f_service    = ctx.getManagerContext().getCacheService();
        }

    /**
     * Construct a dispatcher for the specified cache name and service.
     *
     * @param sCacheName  the name of the cache dispatching events
     * @param service     the name of the associated service
     */
    public StorageDispatcher(String sCacheName, CacheService service)
        {
        super(EVENT_TYPES_CACHE);

        f_ctxBM      = null;
        f_sCacheName = sCacheName;
        f_service    = service;
        }

    // ----- PartitionedCacheDispatcher interface ---------------------------

    /**
     * {@inheritDoc}
     */
    public BackingMapContext getBackingMapContext()
        {
        return f_ctxBM;
        }

    @Override
    public String getCacheName()
        {
        return f_sCacheName;
        }

    @Override
    public String getServiceName()
        {
        return f_service.getInfo().getServiceName();
        }

    @Override
    public String getScopeName()
        {
        return f_service.getBackingMapManager().getCacheFactory().getScopeName();
        }

    // ----- StorageDispatcher methods --------------------------------------

    /**
     * Return a continuation whose completion will cause an {@link EntryEvent}
     * to be dispatched.
     *
     * @param eventType     the {@link EntryEvent.Type} to raise
     * @param setBinEntry   the set of entries being inserted
     * @param continuation  the continuation to complete after dispatching
     *
     * @return a continuation whose completion will post an entry event
     */
    public Continuation getEntryEventContinuation(
            com.tangosol.net.events.partition.cache.EntryEvent.Type eventType,
            Set<BinaryEntry> setBinEntry, Continuation continuation)
        {
        return getDispatchContinuation(
                new PartitionedCacheEntryEvent(this, eventType, setBinEntry), continuation);
        }

    /**
     * Return a continuation whose completion will cause an {@link EntryProcessorEvent}
     * to be dispatched.
     *
     * @param eventType     the {@link EntryProcessorEvent.Type } to raise
     * @param agent         the entry processor
     * @param setBinEntry   the set of entries being inserted
     * @param continuation  the continuation to complete after dispatching
     *
     * @return a continuation whose completion will post an invocation event
     */
    public Continuation getEntryProcessorEventContinuation(
            EntryProcessorEvent.Type eventType,
            EntryProcessor agent, Set<BinaryEntry> setBinEntry,
            Continuation continuation)
        {
        return getDispatchContinuation(
                new PartitionedCacheInvocationEvent(this, eventType, agent, setBinEntry), continuation);
        }

    /**
     * Return a continuation whose completion will cause a {@link
     * CacheLifecycleEvent} to be dispatched.
     *
     * @param eventType     the {@link CacheLifecycleEvent.Type} to raise
     * @param continuation  the continuation to complete after dispatching
     *
     * @return a continuation whose completion will post an invocation event
     */
    public Continuation getCacheLifecycleEventContinuation(
            CacheLifecycleEvent.Type eventType, Continuation continuation)
        {
        return getDispatchContinuation(
                new StorageLifecycleEvent(this, eventType), continuation);
        }


    // ----- inner class: AbstractEvent -------------------------------------

    /**
     * A {@link Event} implementation providing
     * access to the dispatcher.
     */
    protected abstract static class AbstractEvent<T extends Enum<T>>
            extends com.tangosol.net.events.internal.AbstractEvent<T>
            implements Event<T>
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct an AbstractEvent with the provided dispatcher
         * and event type.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         */
        public AbstractEvent(EventDispatcher dispatcher, T eventType)
            {
            super(dispatcher, eventType);
            }

        // ----- Event interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override public PartitionedCacheDispatcher getDispatcher()
            {
            return (PartitionedCacheDispatcher) m_dispatcher;
            }
        }

    // ----- inner class: PartitionedCacheEntryEvent ------------------------

    /**
     * {@link EntryEvent} implementation raised by this dispatcher.
     */
    protected static class PartitionedCacheEntryEvent<K, V>
            extends AbstractEvent<EntryEvent.Type>
            implements EntryEvent<K, V>
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a transfer event.
         *
         * @param dispatcher    the dispatcher that raised this event
         * @param eventType     the event type
         * @param setBinEntry   the set of entries being transferred
         */
        protected PartitionedCacheEntryEvent(EventDispatcher dispatcher, Type eventType, Set<BinaryEntry<K, V>> setBinEntry)
            {
            super(dispatcher, eventType);

            m_binEntry = setBinEntry.iterator().next();
            }

        // ----- AbstractEvent methods --------------------------------------

        /**
         * {@inheritDoc}
         */
        protected boolean isMutableEvent()
            {
            switch (getType())
                {
                case INSERTING:
                case UPDATING:
                case REMOVING:
                    return true;

                default:
                    return false;
                }
            }

        /**
         * {@inheritDoc}
         */
        protected String getDescription()
            {
            return super.getDescription() +
                   ", Service=" + getService().getInfo().getServiceName() +
                   ", Cache=" + getCacheName();
            }

        // ----- EntryEvent interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        public Set<BinaryEntry<K, V>> getEntrySet()
            {
            return Collections.singleton(m_binEntry);
            }

        /**
         * {@inheritDoc}
         */
        public BinaryEntry<K, V> getEntry()
            {
            return m_binEntry;
            }

        // ----- data members -----------------------------------------------

        /**
         * The set of binary entries associated with this storage transfer event.
         */
        protected final BinaryEntry<K, V> m_binEntry;
        }


    // ----- inner class: EntryProcessorEvent -------------------------------

    /**
     * {@link EntryProcessorEvent} implementation raised by this dispatcher.
     */
    protected static class PartitionedCacheInvocationEvent
            extends AbstractEvent<EntryProcessorEvent.Type>
            implements EntryProcessorEvent
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a transfer event.
         *
         * @param dispatcher   the dispatcher that raised this event
         * @param eventType    the event type
         * @param agent        the entry processor
         * @param setBinEntry  the set of entries being transferred
         */
        protected PartitionedCacheInvocationEvent(EventDispatcher dispatcher, Type eventType, EntryProcessor agent,
                Set<BinaryEntry> setBinEntry)
            {
            super(dispatcher, eventType);

            m_processor   = agent;
            m_setBinEntry = setBinEntry;
            }

        // ----- AbstractEvent methods --------------------------------------

        /**
         * {@inheritDoc}
         */
        protected boolean isMutableEvent()
            {
            return getType() == Type.EXECUTING;
            }

        /**
         * {@inheritDoc}
         */
        protected String getDescription()
            {
            return super.getDescription() +
                   ", Service=" + getService().getInfo().getServiceName() +
                   ", Cache=" + getCacheName();
            }

        // ----- EntryProcessorEvent interface ------------------------------

        /**
         * {@inheritDoc}
         */
        public Set<BinaryEntry> getEntrySet()
            {
            return m_setBinEntry;
            }

        /**
         * {@inheritDoc}
         */
        public EntryProcessor getProcessor()
            {
            return m_processor;
            }

       // ----- data members ------------------------------------------------

        /**
         * The set of binary entries associated with this storage transfer event.
         */
        protected final Set<BinaryEntry> m_setBinEntry;

        /**
         * The entry processor associated with this event.
         */
        protected final EntryProcessor m_processor;
        }


    // ----- inner class: StorageLifecycleEvent -----------------------------

    /**
     * {@link CacheLifecycleEvent} implementation raised by this dispatcher.
     */
    protected static class StorageLifecycleEvent
            extends AbstractEvent<CacheLifecycleEvent.Type>
            implements CacheLifecycleEvent
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a cache truncate event.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         */
        protected StorageLifecycleEvent(StorageDispatcher dispatcher, Type eventType)
            {
            super(dispatcher, eventType);
            }

        // ----- AbstractEvent methods --------------------------------------

        /**
         * {@inheritDoc}
         */
        protected boolean isMutableEvent()
            {
            return false;
            }

        /**
         * {@inheritDoc}
         */
        protected String getDescription()
            {
            return super.getDescription() +
                   ", Service=" + getServiceName() +
                   ", Cache=" + getCacheName();
            }

        @Override
        public CacheLifecycleEventDispatcher getEventDispatcher()
            {
            return (CacheLifecycleEventDispatcher) m_dispatcher;
            }

        @Override
        public String getCacheName()
            {
            return getDispatcher().getCacheName();
            }

        @Override
        public CacheService getService()
            {
            return ((StorageDispatcher) getDispatcher()).f_service;
            }

        @Override
        public String getServiceName()
            {
            return getDispatcher().getServiceName();
            }

        @Override
        public String getScopeName()
            {
            return getDispatcher().getScopeName();
            }

        @Override
        public String getSessionName()
            {
            String sName = getService()
                    .getBackingMapManager()
                    .getCacheFactory()
                    .getResourceRegistry()
                    .getResource(String.class, ConfigurableCacheFactorySession.SESSION_NAME);

            return sName == null ? Coherence.DEFAULT_NAME : sName;
            }
        }


    // ----- constants and data members -------------------------------------

    /**
     * The event types raised by this dispatcher when it is storage enabled.
     */
    protected static final Set<Enum> EVENT_TYPES_STORAGE = new HashSet<Enum>();

    /**
     * The event types raised by this dispatcher when it is storage disabled.
     */
    protected static final Set<Enum> EVENT_TYPES_CACHE = new HashSet<Enum>();

    /**
     * Service context; can be null if storage disabled
     */
    protected final BackingMapContext f_ctxBM;

    /**
     * The name of the cache.
     */
    protected final String f_sCacheName;

    /**
     * The service this dispatcher is associated with.
     */
    protected final CacheService f_service;


    // ----- static initializer ---------------------------------------------

    static
        {
        EVENT_TYPES_STORAGE.addAll(Arrays.asList(EntryEvent.Type.values()));
        EVENT_TYPES_STORAGE.addAll(Arrays.asList(EntryProcessorEvent.Type.values()));
        EVENT_TYPES_STORAGE.addAll(Arrays.asList(CacheLifecycleEvent.Type.values()));

        EVENT_TYPES_CACHE.addAll(Arrays.asList(CacheLifecycleEvent.Type.values()));
        }
    }