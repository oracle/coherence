/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import java.io.ObjectInputFilter;

/**
 * Coherence default {@link ObjectInputFilter}.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.04
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
            }
        return fAllowed ? Status.ALLOWED : Status.REJECTED;
        }

    // ----- inner class: IntersectingFilter --------------------------------

    /**
     * Filter that rejects when either delegate rejects.
     */
    private static class IntersectingFilter
            implements ObjectInputFilter
        {
        IntersectingFilter(ObjectInputFilter defaultFilter, ObjectInputFilter userFilter)
            {
            f_defaultFilter = defaultFilter;
            f_userFilter    = userFilter;
            }

        @Override
        public Status checkInput(FilterInfo filterInfo)
            {
            Status statusDefault = f_defaultFilter.checkInput(filterInfo);
            if (statusDefault == Status.REJECTED)
                {
                return Status.REJECTED;
                }

            Status statusUser = f_userFilter.checkInput(filterInfo);
            if (statusUser == Status.REJECTED)
                {
                return Status.REJECTED;
                }

            return statusDefault == Status.ALLOWED || statusUser == Status.ALLOWED
                    ? Status.ALLOWED
                    : Status.UNDECIDED;
            }

        private final ObjectInputFilter f_defaultFilter;
        private final ObjectInputFilter f_userFilter;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton default filter.
     */
    private static final DefaultObjectInputFilter INSTANCE = new DefaultObjectInputFilter();
    }
