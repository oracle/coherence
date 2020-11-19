/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition.cache;

import com.tangosol.net.events.EventDispatcher;

/**
 * A CacheLifecycleEventDispatcher raises {@link CacheLifecycleEvent}s.
 *
 * @author Jonathan Knight  2020.11.16
 * @since 20.12
 */
public interface CacheLifecycleEventDispatcher
        extends EventDispatcher
    {
    /**
     * Return the name of the {@link com.tangosol.net.NamedCache cache}
     * that this dispatcher is associated with.
     *
     * @return  the cache name
     */
    public String getCacheName();

    /**
     * Return the optional name of the {@link com.tangosol.net.CacheService service}
     * that this dispatcher is associated with.
     *
     * @return  the service name that this dispatcher is associated with or {@code null}
     *          if this dispatcher is not associated with a cache service.
     */
    public String getServiceName();

    /**
     * Return the optional scope name that this dispatcher is associated with.
     *
     * @return  the scope name that this dispatcher is associated with or {@code null}
     *          if this dispatcher is not associated with a scope.
     */
    public String getScopeName();
    }
