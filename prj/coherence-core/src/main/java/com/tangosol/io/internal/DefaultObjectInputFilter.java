/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import java.io.ObjectInputFilter;

/**
 * Coherence global default {@link ObjectInputFilter} baseline. Product-owned
 * Java serialization bridge fields use expected-type filters from
 * {@link BridgeObjectInputFilter} or {@link SerializationBridgeFilters}; those
 * filters must intersect with this baseline through
 * {@link #create(ObjectInputFilter)} or the local helper equivalent, so they
 * narrow but never widen it.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.07
 */
public final class DefaultObjectInputFilter
        implements ObjectInputFilter
    {
    /**
     * Return a filter that intersects the Coherence default filter with any
     * JVM-wide user-configured serial filter.
     *
     * @return the effective serial filter
     */
    public static ObjectInputFilter create()
        {
        ObjectInputFilter filterUser = ObjectInputFilter.Config.getSerialFilter();
        return filterUser == null ? INSTANCE : new IntersectingFilter(INSTANCE, filterUser);
        }

    /**
     * Return a filter that intersects the Coherence default filter, any
     * JVM-wide user-configured serial filter, and the specified bridge filter.
     *
     * @param filterBridge  the bridge-local filter
     *
     * @return the effective serial filter
     */
    public static ObjectInputFilter create(ObjectInputFilter filterBridge)
        {
        ObjectInputFilter filterDefault = create();
        return filterBridge == null ? filterDefault : new IntersectingFilter(filterDefault, filterBridge);
        }

    /**
     * Apply a bridge-local filter to the current thread.
     *
     * @param filterBridge  the bridge-local filter
     *
     * @return a scope that restores the previous bridge filter when closed
     */
    public static Scope bridge(ObjectInputFilter filterBridge)
        {
        ObjectInputFilter filterPrevious = s_filterBridge.get();
        ObjectInputFilter filterCurrent  = filterBridge == null || filterPrevious == null
                ? filterBridge
                : new IntersectingFilter(filterPrevious, filterBridge);

        s_filterBridge.set(filterCurrent);
        return () ->
            {
            if (filterPrevious == null)
                {
                s_filterBridge.remove();
                }
            else
                {
                s_filterBridge.set(filterPrevious);
                }
            };
        }

    /**
     * Return a filter that intersects the Coherence default filter with the
     * specified user filter.
     *
     * @param filterUser  the user filter
     *
     * @return the effective serial filter
     */
    static ObjectInputFilter forTesting(ObjectInputFilter filterUser)
        {
        return filterUser == null ? INSTANCE : new IntersectingFilter(INSTANCE, filterUser);
        }

    @Override
    public Status checkInput(FilterInfo filterInfo)
        {
        Class<?> clz = filterInfo.serialClass();
        boolean fAllowed = SerializationAllowlist.isAllowed(clz);
        if (!fAllowed)
            {
            SerializationTelemetry.recordFilterCheck("rejected", "serialization-allowlist-rejected", clz, null);
            return Status.REJECTED;
            }

        ObjectInputFilter filterBridge = s_filterBridge.get();
        if (filterBridge == null)
            {
            return Status.ALLOWED;
            }

        Status statusBridge = filterBridge.checkInput(filterInfo);
        return statusBridge == Status.REJECTED ? Status.REJECTED : Status.ALLOWED;
        }

    // ----- inner interface: Scope ----------------------------------------

    /**
     * A bridge-filter scope.
     */
    public interface Scope
            extends AutoCloseable
        {
        @Override
        void close();
        }

    // ----- inner class: IntersectingFilter --------------------------------

    /**
     * Filter that rejects when either delegate rejects.
     */
    private record IntersectingFilter(ObjectInputFilter defaultFilter, ObjectInputFilter userFilter)
            implements ObjectInputFilter
        {
        @Override
        public Status checkInput(FilterInfo filterInfo)
            {
            Status statusDefault = defaultFilter.checkInput(filterInfo);
            if (statusDefault == Status.REJECTED)
                {
                return Status.REJECTED;
                }

            Status statusUser = userFilter.checkInput(filterInfo);
            if (statusUser == Status.REJECTED)
                {
                return Status.REJECTED;
                }

            return statusDefault == Status.ALLOWED || statusUser == Status.ALLOWED
                    ? Status.ALLOWED
                    : Status.UNDECIDED;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton default filter.
     */
    private static final DefaultObjectInputFilter INSTANCE = new DefaultObjectInputFilter();

    /**
     * Bridge filter scoped to the current deserialization operation.
     */
    private static final ThreadLocal<ObjectInputFilter> s_filterBridge = new ThreadLocal<>();
    }
