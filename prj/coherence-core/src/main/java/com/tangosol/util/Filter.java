/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.OrFilter;
import com.tangosol.util.filter.PartitionedFilter;
import com.tangosol.util.filter.XorFilter;

import java.io.Serializable;

import java.util.Set;
import java.util.Objects;


/**
 * Provide for "pluggable" conditional behavior.
 *
 * @param <T> the type of the input argument to the filter
 *
 * @author cp  1997.09.05
 * @author as  2014.06.15
 *
 * @since 1.0
 */
@FunctionalInterface
public interface Filter<T>
        extends Serializable
    {
    /**
     * Apply the test to the input argument.
     *
     * @param o  the input argument to evaluate
     *
     * @return {@code true} if the input argument matches the filter,
     *         otherwise {@code false}
     */
    public boolean evaluate(T o);

    // ---- Filter methods --------------------------------------------------

    /**
     * Return a string expression for this filter.
     *
     * @return a string expression for this filter
     */
    public default String toExpression()
        {
        return toString();
        }

    /**
     * Return a composed filter that represents a short-circuiting logical
     * AND of this filter and another.  When evaluating the composed
     * filter, if this filter is {@code false}, then the {@code other}
     * filter is not evaluated.
     * <p>
     * Any exceptions thrown during evaluation of either filter are
     * relayed to the caller; if evaluation of this filter throws an
     * exception, the {@code other} filter will not be evaluated.
     *
     * @param other  a filter that will be logically-ANDed with this filter
     *
     * @return a composed filter that represents the short-circuiting logical
     *         AND of this filter and the {@code other} filter
     */
    public default Filter and(Filter other)
        {
        Objects.requireNonNull(other);
        return new AndFilter(this, other);
        }

    /**
     * Return a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     * <p>
     * Any exceptions thrown during evaluation of either predicate are
     * relayed to the caller; if evaluation of this predicate throws an
     * exception, the {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     *
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    public default Filter or(Filter other)
        {
        Objects.requireNonNull(other);
        return new OrFilter(this, other);
        }

    /**
     * Return a composed predicate that represents a logical XOR of this
     * predicate and another.
     * <p>
     * Any exceptions thrown during evaluation of either predicate are
     * relayed to the caller; if evaluation of this predicate throws an
     * exception, the {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-XORed with this predicate
     *
     * @return a composed predicate that represents the logical XOR of this
     *         predicate and the {@code other} predicate
     */
    public default Filter xor(Filter other)
        {
        Objects.requireNonNull(other);
        return new XorFilter(this, other);
        }

    /**
     * Return a key associated filter based on this filter and a specified key.
     *
     * @param <K>  the key type
     * @param key  associated key
     *
     * @return a key associated filter
     */
    public default <K> KeyAssociatedFilter<T> associatedWith(K key)
        {
        return new KeyAssociatedFilter<>(this, key);
        }

    /**
     * Return a partitioned filter for a specified partition set.
     *
     * @param partitions  the set of partitions the filter should run against
     *
     * @return a partitioned filter
     */
    public default PartitionedFilter<T> forPartitions(PartitionSet partitions)
        {
        return new PartitionedFilter<>(this, partitions);
        }

    /**
     * Return a filter that will only be evaluated within specified key set.
     *
     * @param <K>      the key type
     * @param setKeys  the set of keys to limit the filter evaluation to
     *
     * @return a key set-limited filter
     */
    public default <K> InKeySetFilter<T> forKeys(Set<K> setKeys)
        {
        return new InKeySetFilter<>(this, setKeys);
        }

    /**
     * Return a limit filter based on this filter.
     *
     * @param cPageSize  the number of entries per page
     *
     * @return a limit filter
     */
    public default LimitFilter<T> asLimitFilter(int cPageSize)
        {
        return new LimitFilter<>(this, cPageSize);
        }
    }

