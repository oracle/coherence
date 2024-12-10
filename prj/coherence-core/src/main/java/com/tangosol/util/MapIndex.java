/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
* MapIndex is used to correlate values stored in an <i>indexed Map</i> (or
* attributes of those values) to the corresponding keys in the indexed Map.
*
* @param <K>  the key type
* @param <V>  the type of the value from which an extracted value is obtained
* @param <E>  the type of the extracted value that is being indexed
*
* @author cp/gg 2002.10.31
*/
public interface MapIndex<K, V, E>
    {
    /**
    * Obtain the ValueExtractor object that the MapIndex uses to extract
    * an indexable Object from a value stored in the indexed Map. This
    * property is never null.
    *
    * @return a ValueExtractor object, never null
    */
    public ValueExtractor<V, E> getValueExtractor();

    /**
    * Determine if the MapIndex orders the contents of the indexed
    * information. To determine in which way the contents are ordered,
    * get the Comparator from the <i>index contents</i> SortedMap object.
    *
    * @return true if the index contents are ordered, false otherwise
    */
    public boolean isOrdered();

    /**
    * Determine if indexed information for any entry in the indexed Map has
    * been excluded from this index.  This information is used for
    * {@link com.tangosol.util.filter.IndexAwareFilter} implementations to
    * determine the most optimal way to apply the index.
    * <p>
    * Note: Queries that use a partial index are allowed not to return entries
    * that are not indexed even though they would match the corresponding filter
    * were they evaluated during the full scan (if there were no index).
    * However, it's not allowable for a query to return entries that do not
    * match the corresponding filter, regardless of their presence in the index.
    *
    * @return true if any entry of the indexed Map has been excluded from
    *         the index, false otherwise
    * @since Coherence 3.6
    */
    public boolean isPartial();

    /**
    * Get the Map that contains the <i>index contents</i>.
    * <p>
    * The keys of the Map are the return values from the ValueExtractor
    * operating against the indexed Map's values, and for each key, the
    * corresponding value stored in the Map is a Set of keys to the
    * indexed Map.
    * <p>
    * If the MapIndex is known to be ordered, then the returned Map object
    * will be an instance of {@link SortedMap}. The SortedMap may or may
    * not have a {@link Comparator} object associated with it; see
    * {@link SortedMap#comparator()}.
    * <p>
    * A client should assume that the returned Map object is read-only and
    * must not attempt to modify it.
    *
    * @return a Map (or a SortedMap) of the index contents
    */
    public Map<E, Set<K>> getIndexContents();

    /**
    * Using the index information if possible, get the value associated with
    * the specified key. This is expected to be more efficient than using
    * the ValueExtractor against an object containing the value, because the
    * index should already have the necessary information at hand.
    *
    * @param key  the key that specifies the object to extract the value from
    *
    * @return  the value that would be extracted by this MapIndex's
    *          ValueExtractor from the object specified by the passed key;
    *          NO_VALUE if the index does not have the necessary information
    */
    public Object get(K key);

    /**
    * Get the Comparator used to sort the index.
    *
    * @return the comparator
    *
    * @since Coherence 3.5
    */
    public Comparator<E> getComparator();

    /**
    * Update this index in response to a insert operation on a cache.
    *
    * @param entry  the entry representing the object being inserted
    *
    * @since Coherence 3.5
    */
    public void insert(Map.Entry<? extends K, ? extends V> entry);

    /**
    * Update this index in response to an update operation on a cache.
    *
    * @param entry  the entry representing the object being updated
    *
    * @since Coherence 3.5
    */
    public void update(Map.Entry<? extends K, ? extends V> entry);

    /**
    * Update this index in response to a remove operation on a cache.
    *
    * @param entry  the entry representing the object being removed
    *
    * @since Coherence 3.5
    */
    public void delete(Map.Entry<? extends K, ? extends V> entry);

    default long getUnits()
        {
        return 0;
        }

    /**
    * Constant used to indicate that the index does not contain requested
    * value.
    */
    public static final Object NO_VALUE = new Object();
    }
