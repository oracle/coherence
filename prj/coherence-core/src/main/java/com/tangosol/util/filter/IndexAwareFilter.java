/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.Map;
import java.util.Set;


/**
* IndexAwareFilter is an extension to the EntryFilter interface that allows
* a filter to use a Map index to fully or partially evaluate itself.
*
* @author cp/gg 2002.10.31
*/
public interface IndexAwareFilter<K, V>
        extends EntryFilter<K, V>
    {
    /**
    * Given a Map of available indexes, determine if this IndexAwareFilter
    * can use any of the indexes to assist in its processing, and if so,
    * determine how effective the use of that index would be.
    * <p>
    * The returned value is an effectiveness estimate of how well this filter
    * can use the specified indexes to filter the specified keys.
    * An operation that requires no more than a single access to the index
    * content (i.e. Equals, NotEquals) has an effectiveness of <b>one</b>.
    * Evaluation of a single entry is assumed to have an effectiveness that
    * depends on the index implementation and is usually measured as a
    * constant number of the single operations.  This number is referred to
    * as <i>evaluation cost</i>.
    * <p>
    * If the effectiveness of a filter evaluates to a number larger than the
    * <tt>keySet.size() * &lt;evaluation cost&gt;</tt> then a user could
    * avoid using the index and iterate through the keySet calling
    * <tt>evaluate</tt> rather then <tt>applyIndex</tt>.
    *
    * @param mapIndexes  the available {@link MapIndex} objects keyed by the
    *                    related ValueExtractor; read-only
    * @param setKeys     the set of keys that will be filtered; read-only
    *
    * @param <RK> the raw key type
    *
    * @return an effectiveness estimate of how well this filter can use the
    *         specified indexes to filter the specified keys
    */
    public <RK> int calculateEffectiveness(
        Map<? extends ValueExtractor<? extends V, Object>, ? extends MapIndex<? extends RK, ? extends V, Object>> mapIndexes,
        Set<? extends RK> setKeys);

    /**
    * Filter remaining keys using a Map of available indexes.
    * <p>
    * The filter is responsible for removing all keys from the passed set of
    * keys that the applicable indexes can prove should be filtered. If the
    * filter does not fully evaluate the remaining keys using just the index
    * information, it must return a filter (which may be an
    * {@link EntryFilter}) that can complete the task using an iterating
    * implementation. If, on the other hand, the filter does fully evaluate
    * the remaining keys using just the index information, then it should
    * return null to indicate that no further filtering is necessary.
    *
    * @param mapIndexes  the available {@link MapIndex} objects keyed by the
    *                    related ValueExtractor; read-only
    * @param setKeys     the mutable set of keys that remain to be filtered
    *
    * @param <RK> the raw key type
    *
    * @return a {@link Filter} object (which may be an {@link EntryFilter})
    *         that can be used to process the remaining keys, or null if no
    *         additional filter processing is necessary
    */
    public <RK> Filter<V> applyIndex(
        Map<? extends ValueExtractor<? extends V, Object>, ? extends MapIndex<? extends RK, ? extends V, Object>> mapIndexes,
        Set<? extends RK> setKeys);
    }