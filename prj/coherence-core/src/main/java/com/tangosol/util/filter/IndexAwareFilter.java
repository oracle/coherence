/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;

import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.Map;
import java.util.Set;

/**
 * IndexAwareFilter is an extension to the EntryFilter interface that allows a
 * filter to use a Map index to fully or partially evaluate itself.
 *
 * @author cp/gg 2002.10.31
 */
public interface IndexAwareFilter<K, V>
        extends EntryFilter<K, V>
    {
    /**
     * Given a Map of available indexes, determine if this IndexAwareFilter can
     * use any of the indexes to assist in its processing, and if so, determine
     * how effective the use of that index would be.
     * <p>
     * The returned value is an effectiveness estimate of how many keys will
     * remain in the set after the index is applied. If no keys will remain
     * in the set after the index is applied, this method should return 0. If
     * all the keys will remain in the set, implying that no entries would be
     * filtered out based on this filter, this method should return
     * {@code setKeys.size()}. Otherwise, it should return the value between 0
     * and {@code setKeys.size()}. If there is no index in the specified index
     * map that can be used by this filter, this method should return a negative
     * integer.
     * <p>
     * The effectiveness returned will be used by the composite filters to
     * reorder nested filters from most to least effective, in order to optimize
     * query execution.
     *
     * @param mapIndexes the available {@link MapIndex} objects keyed by the
     *                   related ValueExtractor; read-only
     * @param setKeys    the set of keys that will be filtered; read-only
     *
     * @return an effectiveness estimate of how well this filter can use the
     * specified indexes to filter the specified keys
     */
    int calculateEffectiveness(
            Map<? extends ValueExtractor<? extends V, ?>, ? extends MapIndex<? extends K, ? extends V, ?>> mapIndexes,
            Set<? extends K> setKeys);

    /**
     * Filter remaining keys using a Map of available indexes.
     * <p>
     * The filter is responsible for removing all keys from the passed set of
     * keys that the applicable indexes can prove should be filtered. If the
     * filter does not fully evaluate the remaining keys using just the index
     * information, it must return a filter (which may be an
     * {@link EntryFilter}) that can complete the task using an iterating
     * implementation. If, on the other hand, the filter does fully evaluate the
     * remaining keys using just the index information, then it should return
     * null to indicate that no further filtering is necessary.
     *
     * @param mapIndexes the available {@link MapIndex} objects keyed by the
     *                   related ValueExtractor; read-only
     * @param setKeys    the mutable set of keys that remain to be filtered
     *
     * @return a {@link Filter} object (which may be an {@link EntryFilter})
     * that can be used to process the remaining keys, or null if no additional
     * filter processing is necessary
     */
    public Filter<?> applyIndex(
            Map<? extends ValueExtractor<? extends V, ?>, ? extends MapIndex<? extends K, ? extends V, ?>> mapIndexes,
            Set<? extends K> setKeys);
    }
