/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;

import java.util.Objects;

/**
 * The semantic definition of a continuously maintained aggregation.
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public final class ContinuousAggregationDefinition
    {
    /**
     * Construct a continuous aggregation definition.
     *
     * @param filter      the filter selecting entries to aggregate
     * @param aggregator  the streaming aggregator definition
     */
    public ContinuousAggregationDefinition(
            Filter<?> filter,
            InvocableMap.StreamingAggregator<?, ?, ?, ?> aggregator)
        {
        Objects.requireNonNull(aggregator, "aggregator cannot be null");

        if (!aggregator.isContinuous())
            {
            throw new IllegalArgumentException("aggregator does not support continuous aggregation: "
                    + aggregator.getClass().getName());
            }

        if (aggregator.isRetainsEntries())
            {
            throw new IllegalArgumentException("continuous aggregator cannot retain entries: "
                    + aggregator.getClass().getName());
            }

        InvocableMap.StreamingAggregator<?, ?, ?, ?> prototype = aggregator.supply();
        if (prototype == null)
            {
            throw new IllegalArgumentException("aggregator supply() returned null: "
                    + aggregator.getClass().getName());
            }

        if (!prototype.isContinuous() || prototype.isRetainsEntries())
            {
            throw new IllegalArgumentException("supplied aggregator does not satisfy the continuous contract: "
                    + prototype.getClass().getName());
            }

        f_filter     = filter == null ? AlwaysFilter.INSTANCE() : filter;
        f_aggregator = prototype;
        }

    /**
     * Return the filter selecting entries for this definition.
     *
     * @return the definition filter
     */
    public Filter<?> getFilter()
        {
        return f_filter;
        }

    /**
     * Return the filter to evaluate on a storage member.
     * <p>
     * {@link KeyAssociatedFilter} is a client-side routing filter. Its host
     * key remains part of this definition's identity, but only its wrapped
     * filter is evaluated against entries in the associated partitions.
     *
     * @return the storage-side evaluation filter
     */
    public Filter<?> getEvaluationFilter()
        {
        return f_filter instanceof KeyAssociatedFilter
               ? ((KeyAssociatedFilter<?>) f_filter).getFilter()
               : f_filter;
        }

    /**
     * Return whether this definition is scoped by key association.
     *
     * @return {@code true} if this definition has an associated host key
     */
    public boolean isKeyAssociated()
        {
        return f_filter instanceof KeyAssociatedFilter;
        }

    /**
     * Return the host key that scopes this definition.
     *
     * @return the associated host key, or {@code null} if this definition is
     *         cache-wide
     */
    public Object getHostKey()
        {
        return isKeyAssociated()
               ? ((KeyAssociatedFilter<?>) f_filter).getHostKey()
               : null;
        }

    /**
     * Return the pristine streaming aggregator prototype.
     *
     * @return the aggregator prototype
     */
    public InvocableMap.StreamingAggregator<?, ?, ?, ?> getAggregator()
        {
        return f_aggregator;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof ContinuousAggregationDefinition))
            {
            return false;
            }

        ContinuousAggregationDefinition that = (ContinuousAggregationDefinition) o;
        return f_filter.equals(that.f_filter)
                && f_aggregator.equals(that.f_aggregator);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_filter, f_aggregator);
        }

    @Override
    public String toString()
        {
        return "ContinuousAggregationDefinition{filter=" + f_filter
                + ", aggregator=" + f_aggregator + '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The normalized definition filter.
     */
    private final Filter<?> f_filter;

    /**
     * The pristine streaming aggregator prototype.
     */
    private final InvocableMap.StreamingAggregator<?, ?, ?, ?> f_aggregator;
    }
