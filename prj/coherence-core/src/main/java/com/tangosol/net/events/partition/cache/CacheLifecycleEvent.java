/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition.cache;

/**
 * A CacheLifecycleEvent allows subscribers to capture events pertaining to
 * the lifecycle of a cache.
 *
 * @author bbc 2015.09.01
 * @since 12.2.1
 */
public interface CacheLifecycleEvent
        extends com.tangosol.net.events.Event<CacheLifecycleEvent.Type>
    {
    /**
     * Return the {@link PartitionedCacheDispatcher} this event was
     * raised by or {@code null} if the dispatcher is not a
     * {@link PartitionedCacheDispatcher}.
     *
     * @return the {@code PartitionedCacheDispatcher} this event was raised by
     *         or {@code null} if the dispatcher is not a
     *         {@link PartitionedCacheDispatcher}.
     *
     * @deprecated use {@link #getEventDispatcher()}
     */
    @Deprecated
    @Override
    public PartitionedCacheDispatcher getDispatcher();

    /**
     * Return the {@link CacheLifecycleEventDispatcher} this event was raised by.
     *
     * @return the CacheLifecycleEventDispatcher this event was raised by
     */
    public CacheLifecycleEventDispatcher getEventDispatcher();

    /**
     * The name of the cache that the event is associated with.
     *
     * @return the name of the cache that the event is associated with
     */
    public String getCacheName();

    /**
     * The name of the service that the event is associated with.
     *
     * @return the name of the service that the event is associated with {@code null}
     *         if this event is not associated with a service
     */
    public String getServiceName();

    /**
     * The scope name that this event is associated with.
     *
     * @return the scope name that this event is associated with or {@code null}
     *         if this event is not associated with a scope
     */
    public String getScopeName();

    /**
     * The optional Session name that this event is associated with.
     *
     * @return the optional Session name that this event is associated with or
     *         {@code null} if this event is not associated with a Session
     */
    public String getSessionName();

    // ----- constants ------------------------------------------------------

    /**
     * The emitted event types for a {@link CacheLifecycleEvent}.
     */
    public static enum Type
        {
        /**
         * {@link CacheLifecycleEvent}s of the type {@code CREATED} are raised
         * when the relevant data structures to support a cache are created locally.
         * <p>
         * This event can be raised on both ownership enabled and disabled members.
         * <p>
         * The event may be raised based on a "natural" call to {@link
         * com.tangosol.net.CacheService#ensureCache(String, ClassLoader) ensureCache},
         * or for synthetic reasons such as a member joining an existing service
         * with pre-existing caches.
         */
        CREATED,

        /**
         * {@link CacheLifecycleEvent}s of the type {@code DESTROYED} are raised
         *  when a storage for a given cache is destroyed (usually as a result
         *  of a call to {@link com.tangosol.net.NamedCache#destroy destroy}).
         */
        DESTROYED,

        /**
         * {@link CacheLifecycleEvent}s of the type {@code TRUNCATED} are raised
         * when a storage for a given cache is truncated as a result of a call
         * to {@link com.tangosol.net.NamedCache#truncate truncate}.
         * <p>
         * Truncate provides unobservable removal of all data associated to a
         * cache thus this event notifies subscribers of the execution of a
         * truncate operation, intentionally, without the associated entries.
         */
        TRUNCATED
        }
    }