/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition.cache;


import com.tangosol.net.NamedCache;


/**
 * A CacheLifecycleEvent allows subscribers to capture events pertaining to
 * the lifecycle of a cache.
 *
 * @author bbc 2015.09.01
 * @since 12.2.1
 */
public interface CacheLifecycleEvent
        extends Event<CacheLifecycleEvent.Type>
    {
    /**
     * The name of the cache that the event is associated with.
     *
     * @return the name of the cache that the event is associated with
     */
    public String getCacheName();

    /**
     * The name of the service that the event is associated with.
     *
     * @return the name of the service that the event is associated with
     */
    public default String getServiceName()
        {
        return getService().getInfo().getServiceName();
        }

    /**
     * The name cache that thi event is associated with.
     *
     * @return the cache that this event is associated with
     */
    public default NamedCache getCache()
        {
        return getService().ensureCache(getCacheName(), null);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The emitted event types for a {@link CacheLifecycleEvent}.
     */
    public static enum Type
        {
        /**
         * {@link CacheLifecycleEvent}s of the type {@code CREATED} are raised
         * when a storage for a given cache is created.
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