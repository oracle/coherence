/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

/**
 * Optional contract implemented by a continuous aggregator whose stale
 * result remains a certified bound. The contract allows storage to avoid a
 * partition scan when an exact result from another partition dominates that
 * bound.
 *
 * @author Aleks Seovic  2026.09.08
 * @since 26.10
 */
public interface ContinuousAggregationBound
    {
    /**
     * Return the retained bound in the aggregator's partial-result format.
     *
     * @return the retained bound
     */
    Object getContinuousAggregationBound();

    /**
     * Compare two bounds in the order they should be considered. A negative
     * result means that {@code left} can affect the result before
     * {@code right}.
     *
     * @param left   the first bound
     * @param right  the second bound
     *
     * @return a negative, zero, or positive value
     */
    int compareContinuousAggregationBounds(Object left, Object right);

    /**
     * Return whether an exact partial result dominates the specified bound.
     *
     * @param bound         the stale partition bound
     * @param exactPartial  the exact partial result accumulated so far
     *
     * @return {@code true} if the stale partition cannot change the result
     */
    boolean isContinuousAggregationBoundDominated(Object bound, Object exactPartial);
    }
