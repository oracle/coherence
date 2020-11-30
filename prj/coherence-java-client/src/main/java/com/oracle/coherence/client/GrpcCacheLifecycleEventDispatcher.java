/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.net.NamedCache;

import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.internal.AbstractEventDispatcher;
import com.tangosol.net.events.internal.NamedEventInterceptor;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEventDispatcher;
import com.tangosol.net.events.partition.cache.Event;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jonathan Knight  2020.11.16
 */
public class GrpcCacheLifecycleEventDispatcher
        extends AbstractEventDispatcher
        implements CacheLifecycleEventDispatcher
    {
    // ----- constructors ---------------------------------------------------

    public GrpcCacheLifecycleEventDispatcher(String sCacheName, GrpcRemoteSession session)
        {
        super(EVENT_TYPES);
        f_sCacheName = sCacheName;
        f_session    = session;
        }

    // ----- GrpcCacheLifecycleEventDispatcher methods ----------------------

    /**
     * Returns the session owning the cache.
     *
     * @return  the session owning the cache
     */
    public GrpcRemoteSession getSession()
        {
        return f_session;
        }

    // ----- CacheLifecycleEventDispatcher methods --------------------------

    @Override
    public String getCacheName()
        {
        return f_sCacheName;
        }

    @Override
    public String getServiceName()
        {
        return "";
        }

    @Override
    public String getScopeName()
        {
        return f_session.getScopeName();
        }

    // ----- RemoteSessionDispatcher methods --------------------------------

    public void dispatchCacheCreated(NamedCache cache)
        {
        dispatchCacheEvent(CacheLifecycleEvent.Type.CREATED, cache);
        }

    public void dispatchCacheDestroyed(NamedCache cache)
        {
        dispatchCacheEvent(CacheLifecycleEvent.Type.DESTROYED, cache);
        }

    public void dispatchCacheTruncated(NamedCache cache)
        {
        dispatchCacheEvent(CacheLifecycleEvent.Type.TRUNCATED, cache);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Helper to perform the dispatch of a {@link CacheLifecycleEvent}
     * being given its type
     *
     * @param eventType  the enum representing the event type
     * @param cache      the related cache
     */
    protected void dispatchCacheEvent(CacheLifecycleEvent.Type eventType, NamedCache cache)
        {
        List<NamedEventInterceptor<?>> list = getInterceptorMap().get(eventType);
        if (list != null)
            {
            new GrpcCacheLifecycleEvent(this, eventType, cache).dispatch(list);
            }
        }

    // ----- inner class: AbstractEvent -------------------------------------

    /**
     * A {@link Event} implementation providing
     * access to the dispatcher.
     */
    protected abstract static class AbstractEvent<T extends Enum<T>>
            extends com.tangosol.net.events.internal.AbstractEvent<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an AbstractEvent with the provided dispatcher
         * and event type.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         */
        public AbstractEvent(GrpcCacheLifecycleEventDispatcher dispatcher, T eventType)
            {
            super(dispatcher, eventType);
            }
        }

    // ----- inner class: CacheLifecycleEvent -------------------------------

    /**
     * {@link CacheLifecycleEvent} implementation raised by this dispatcher.
     */
    protected static class GrpcCacheLifecycleEvent
            extends AbstractEvent<CacheLifecycleEvent.Type>
            implements CacheLifecycleEvent
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a cache truncate event.
         *
         * @param dispatcher   the dispatcher that raised this event
         * @param eventType    the event type
         */
        protected GrpcCacheLifecycleEvent(GrpcCacheLifecycleEventDispatcher dispatcher, Type eventType, NamedCache cache)
            {
            super(dispatcher, eventType);

            f_cache = cache;
            }

        // ----- AbstractEvent methods --------------------------------------

        @Override
        protected boolean isMutableEvent()
            {
            return false;
            }

        @Override
        protected String getDescription()
            {
            return super.getDescription() +
                   ", Session=" + getSessionName() +
                   ", Scope=" + getScopeName() +
                   ", Cache=" + getCacheName();
            }

        @Override
        public String getCacheName()
            {
            return f_cache.getCacheName();
            }

        @Override
        public String getServiceName()
            {
            return null;
            }

        @Override
        public String getScopeName()
            {
            return getEventDispatcher().getScopeName();
            }

        @Override
        public String getSessionName()
            {
            return ((GrpcCacheLifecycleEventDispatcher) getEventDispatcher()).getSession().getName();
            }

        @Override
        public CacheLifecycleEventDispatcher getEventDispatcher()
            {
            return (CacheLifecycleEventDispatcher) m_dispatcher;
            }

        @Override
        public PartitionedCacheDispatcher getDispatcher()
            {
            return null;
            }

        // overridden to make the method accessible from this dispatcher class
        @Override
        protected void dispatch(Collection<? extends EventInterceptor<?>> colIter)
            {
            super.dispatch(colIter);
            }

        // ----- data members -----------------------------------------------

        /**
         * The cache that the event is associated with.
         */
        private final NamedCache<?, ?> f_cache;
        }
    
    // ----- constants ------------------------------------------------------

    /**
     * The event types raised by this dispatcher.
     */
    protected static final Set<Enum> EVENT_TYPES = new HashSet<>();

    // ----- data members ---------------------------------------------------

    /**
     * The name of the cache.
     */
    private final String f_sCacheName;

    /**
     * The session owning the cache.
     */
    private GrpcRemoteSession f_session;

    // ----- static initializer ---------------------------------------------

    static
        {
        EVENT_TYPES.addAll(Arrays.asList(CacheLifecycleEvent.Type.values()));
        }
    }
