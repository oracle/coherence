/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.NamedEventInterceptor;

import com.tangosol.net.events.internal.AbstractEventDispatcher;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEventDispatcher;
import com.tangosol.net.events.partition.cache.Event;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link CacheLifecycleEventDispatcher} used by a {@link GrpcRemoteCacheService}
 * to dispatch cache lifecycle events.
 *
 * @author Jonathan Knight  2020.11.16
 */
public class GrpcCacheLifecycleEventDispatcher
        extends AbstractEventDispatcher
        implements CacheLifecycleEventDispatcher
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link GrpcCacheLifecycleEventDispatcher}.
     *
     * @param sCacheName  the name of the cache to dispatch lifecycle events for
     * @param service     the {@link GrpcRemoteCacheService} that owns the cache
     */
    GrpcCacheLifecycleEventDispatcher(String sCacheName, GrpcRemoteCacheService service)
        {
        super(EVENT_TYPES);
        f_sCacheName = sCacheName;
        f_service    = Objects.requireNonNull(service);
        f_session    = null;
        }

    /**
     * Create a {@link GrpcCacheLifecycleEventDispatcher}.
     *
     * @param sCacheName  the name of the cache to dispatch lifecycle events for
     * @param session     the {@link GrpcRemoteSession} that owns the cache
     *
     * @deprecated this method will be removed when {@link GrpcRemoteSession} is removed.
     */
    @Deprecated(since = "22.06.2")
    GrpcCacheLifecycleEventDispatcher(String sCacheName, GrpcRemoteSession session)
        {
        super(EVENT_TYPES);
        f_sCacheName = sCacheName;
        f_session    = Objects.requireNonNull(session);
        f_service    = null;
        }

    // ----- GrpcCacheLifecycleEventDispatcher methods ----------------------

    /**
     * Returns the session owning the cache.
     *
     * @return  the session owning the cache
     */
    @Deprecated(since = "22.06.2")
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
        return f_service == null ? "" : f_service.getInfo().getServiceName();
        }

    @Override
    public String getScopeName()
        {
        return f_service == null ? f_session.getScopeName() : f_service.getScopeName();
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


    public GrpcRemoteCacheService getService()
        {
        return f_service;
        }

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
            GrpcCacheLifecycleEventDispatcher dispatcher = (GrpcCacheLifecycleEventDispatcher) getEventDispatcher();

            return dispatcher.getService()
                    .getBackingMapManager()
                    .getCacheFactory()
                    .getResourceRegistry()
                    .getResource(String.class, ConfigurableCacheFactorySession.SESSION_NAME);
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
    @SuppressWarnings("rawtypes")
    protected static final Set<Enum> EVENT_TYPES = new HashSet<>();

    // ----- data members ---------------------------------------------------

    /**
     * The name of the cache.
     */
    private final String f_sCacheName;

    /**
     * The {@link GrpcRemoteCacheService} owning the cache.
     */
     private final GrpcRemoteCacheService f_service;

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
