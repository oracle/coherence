/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;

import com.tangosol.net.NamedCache;
import com.tangosol.net.management.annotation.Description;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

@Description("Provides View Cache statistics.")
public interface ViewMBean
    {
    /**
     * Returns name of this cache.
     *
     * @return cache name
     */
    @Description("The cache name.")
    String getViewName();

    /**
     * Determine if this {@code ContinuousQueryCache} disallows data modification
     * operations.
     *
     * @return {@code true} if this {@code ContinuousQueryCache} has been configured as
     *         read-only
     */
    @Description("Indicates if the view cache is read only.")
    boolean isReadOnly();

    /**
     * Determine if this {@code ContinuousQueryCache} transforms values.
     *
     * @return {@code true} if this {@code ContinuousQueryCache} has been configured to transform
     *         values
     */
    @Description("Indicates if the cache transforms values.")
    boolean isTransformed();

    /**
     * Obtain the {@link Filter} that this {@code ContinuousQueryCache} is using to query the
     * underlying {@link NamedCache}.
     *
     * @return the {@link Filter} that this cache uses to select its contents
     *         from the underlying {@link NamedCache}
     */
    @Description("The implementation of a com.tangosol.util.Filter, used by the associated view-scheme.")
    String getFilter();

    /**
     * Obtain the transformer that this {@code ContinuousQueryCache} is using to transform the results from
     * the underlying cache prior to storing them locally.
     *
     * @return the {@link ValueExtractor} that this cache uses to transform entries from the underlying cache
     */
    @Description("The implementation of a com.tangosol.util.ValueExtractor used to transform values retrieved from the underlying cache, before storing them locally. If specified, this view can be set to read-only.")
    String getTransformer();

    /**
     * Return the reconnection interval (in milliseconds). This value indicates the period
     * in which re-synchronization with the underlying cache will be delayed in the case the
     * connection is severed.  During this time period, local content can be accessed without
     * triggering re-synchronization of the local content.
     *
     * @return a reconnection interval (in milliseconds)
     */
    @Description("The reconnect-interval indicates the period in which re-synchronization with the underlying cache will be delayed in the case the connection is severed.")
    long getReconnectInterval();

    /**
     * Determine if this {@code ContinuousQueryCache} caches values locally.
     *
     * @return {@code true} if this object caches values locally, and {@code false} if it
     *         relies on the underlying {@link NamedCache}
     */
    @Description("Determines whether cache should cache values or only keys.")
    boolean isCacheValues();

    /**
     * Returns the number of key-value mappings in this cache.
     *
     * @return the number of key-value mappings in this cache
     */
    @Description("The number of entries in the cache.")
    long getSize();
    }
