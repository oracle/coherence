/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.util.extractor.KeyExtractor;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiConsumer;

import java.util.stream.Collectors;

/**
 * Map with additional query features.
 *
 * @param <K> the type of the Map entry keys
 * @param <V> the type of the Map entry values
 *
 * @author gg  2002.09.24
 * @author as  2014.08.05
 */
public interface QueryMap<K, V>
        extends Map<K, V>
    {
    /**
     * Return a set view of the keys contained in this map for entries that
     * satisfy the criteria expressed by the filter.
     * <p>
     * Unlike the {@link #keySet()} method, the set returned by this method may
     * not be backed by the map, so changes to the set may not reflected in the
     * map, and vice-versa.
     * <p>
     * <b>Note: When using the Coherence Enterprise Edition or Grid Edition, the
     * Partitioned Cache implements the QueryMap interface using the Parallel
     * Query feature. When using Coherence Standard Edition, the Parallel Query
     * feature is not available, resulting in lower performance for most
     * queries, and particularly when querying large data sets.</b>
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return a set of keys for entries that satisfy the specified criteria
     */
    public Set<K> keySet(Filter filter);

    /**
     * Return a set view of the entries contained in this map that satisfy the
     * criteria expressed by the filter.  Each element in the returned set is a
     * {@link java.util.Map.Entry}.
     * <p>
     * Unlike the {@link #entrySet()} method, the set returned by this method
     * may not be backed by the map, so changes to the set may not be reflected
     * in the map, and vice-versa.
     * <p>
     * <b>Note: When using the Coherence Enterprise Edition or Grid Edition, the
     * Partitioned Cache implements the QueryMap interface using the Parallel
     * Query feature. When using Coherence Standard Edition, the Parallel Query
     * feature is not available, resulting in lower performance for most
     * queries, and particularly when querying large data sets.</b>
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return a set of entries that satisfy the specified criteria
     */
    public Set<Map.Entry<K, V>> entrySet(Filter filter);

    /**
     * Return a set view of the entries contained in this map that satisfy the
     * criteria expressed by the filter.  Each element in the returned set is a
     * {@link java.util.Map.Entry}.  It is further guaranteed that its iterator
     * will traverse the set in such a way that the entry values come up in
     * ascending order, sorted by the specified Comparator or according to the
     * <i>natural ordering</i> (see {@link Comparable}).
     * <p>
     * Unlike the {@link #entrySet()} method, the set returned by this method
     * may not be backed by the map, so changes to the set may not be reflected
     * in the map, and vice-versa.
     * <p>
     * <b>Note: When using the Coherence Enterprise Edition or Grid Edition, the
     * Partitioned Cache implements the QueryMap interface using the Parallel
     * Query feature. When using Coherence Standard Edition, the Parallel Query
     * feature is not available, resulting in lower performance for most
     * queries, and particularly when querying large data sets.</b>
     *
     * @param filter     the Filter object representing the criteria that the
     *                   entries of this map should satisfy
     * @param comparator the Comparator object which imposes an ordering on
     *                   entries in the resulting set; or <tt>null</tt> if the
     *                   entries' values natural ordering should be used
     *
     * @return a set of entries that satisfy the specified criteria
     *
     * @see com.tangosol.util.comparator.ChainedComparator
     */
    public Set<Map.Entry<K, V>> entrySet(Filter filter, Comparator comparator);

   /**
     * Return a collection of the values contained in this map that satisfy the
     * criteria expressed by the filter.
     * <p>
     * Unlike the {@link #values()} method, the collection returned by this
     * method may not be backed by the map, so changes to the collection may not
     * be reflected in the map, and vice-versa.
     * <p>
     * <b>Note: When using the Coherence Enterprise Edition or Grid Edition, the
     * Partitioned Cache implements the QueryMap interface using the Parallel
     * Query feature. When using Coherence Standard Edition, the Parallel Query
     * feature is not available, resulting in lower performance for most
     * queries, and particularly when querying large data sets.</b>
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return a collection of values for entries that satisfy the specified
     *         criteria
     */
    public default Collection<V> values(Filter filter)
        {
        return entrySet(filter)
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        }


    /**
     * Return a collection of the values contained in this map that satisfy the
     * criteria expressed by the filter.
     * <p>
     * Unlike the {@link #values()} method, the collection returned by this
     * method may not be backed by the map, so changes to the collection may not
     * be reflected in the map, and vice-versa.
     * <p>
     * <b>Note: When using the Coherence Enterprise Edition or Grid Edition, the
     * Partitioned Cache implements the QueryMap interface using the Parallel
     * Query feature. When using Coherence Standard Edition, the Parallel Query
     * feature is not available, resulting in lower performance for most
     * queries, and particularly when querying large data sets.</b>
     *
     * @param filter     the Filter object representing the criteria that the
     *                   entries of this map should satisfy
     * @param comparator the Comparator object which imposes an ordering on
     *                   entries in the resulting set; or <tt>null</tt> if the
     *                   entries' values natural ordering should be used
     *
     * @return a collection of values for entries that satisfy the specified
     *         criteria
     *
     * @see com.tangosol.util.comparator.ChainedComparator
     */
    public default Collection<V> values(Filter filter, Comparator comparator)
        {
        return entrySet(filter, comparator)
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        }

    /**
     * Add an index to this QueryMap. This allows to correlate values stored in
     * this <i>indexed Map</i> (or attributes of those values) to the
     * corresponding keys in the indexed Map and increase the performance of
     * methods that use {@link Filter Filters}.
     * <p>
     * The ordering maintained by this map (as determined by either the
     * specified Comparator or the natural ordering of the indexed values) must
     * be <i>consistent with equals</i> (see {@link Comparable} or {@link
     * Comparator} for a precise definition of <i>consistent with equals</i>.)
     * <p>
     * This method is only intended as a hint to the map implementation, and
     * as such it may be ignored by the map if indexes are not supported or if
     * the desired index (or a similar index) already exists. It is expected
     * that an application will call this method to suggest an index even if the
     * index may already exist, just so that the application is certain that
     * index has been suggested. For example in a distributed environment, each
     * server will likely suggest the same set of indexes when it starts, and
     * there is no downside to the application blindly requesting those indexes
     * regardless of whether another server has already requested the same
     * indexes.
     * <p>
     * <b>Note: Indexes are a feature of Coherence Enterprise Edition and
     * Coherence Grid Edition. This method will have no effect when using
     * Coherence Standard Edition.</b>
     *
     * @param <T>        the type of the value to extract from
     * @param <E>        the type of value that will be extracted
     * @param extractor  the ValueExtractor object that is used to extract an
     *                   indexable Object from a value stored in the indexed
     *                   Map.  Must not be null.
     * @param fOrdered   true iff the contents of the indexed information should
     *                   be ordered; false otherwise
     * @param comparator the Comparator object which imposes an ordering on
     *                   entries in the indexed map; or <tt>null</tt> if the
     *                   entries' values natural ordering should be used
     *
     * @see com.tangosol.util.extractor.ReflectionExtractor
     * @see com.tangosol.util.comparator.ChainedComparator
     */
    public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor, boolean fOrdered, Comparator<? super E> comparator);

    /**
     * Add an unordered index to this QueryMap. This allows to correlate values
     * stored in this <i>indexed Map</i> (or attributes of those values) to the
     * corresponding keys in the indexed Map and increase the performance of
     * methods that use {@link Filter Filters}.
     * <p>
     * This method is only intended as a hint to the map implementation, and
     * as such it may be ignored by the map if indexes are not supported or if
     * the desired index (or a similar index) already exists. It is expected
     * that an application will call this method to suggest an index even if the
     * index may already exist, just so that the application is certain that
     * index has been suggested. For example in a distributed environment, each
     * server will likely suggest the same set of indexes when it starts, and
     * there is no downside to the application blindly requesting those indexes
     * regardless of whether another server has already requested the same
     * indexes.
     * <p>
     * <b>Note: This method will have no effect when using Coherence Standard
     * Edition.</b>
     *
     * @param <T>        the type of the value to extract from
     * @param <E>        the type of value that will be extracted
     * @param extractor  the ValueExtractor object that is used to extract an
     *                   indexable Object from a value stored in the indexed
     *                   Map.  Must not be null.
     *
     * @see com.tangosol.util.extractor.ReflectionExtractor
     * @see com.tangosol.util.comparator.ChainedComparator
     */
    public default <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor)
        {
        addIndex(extractor, false, null);
        }

    /**
     * Remove an index from this QueryMap.
     *
     * @param <T>        the type of the value to extract from
     * @param <E>        the type of value that will be extracted
     * @param extractor  the ValueExtractor object that is used to extract an
     *                   indexable Object from a value stored in the Map.
     */
    public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> extractor);

    // ----- Map interface --------------------------------------------------

    /**
     * Perform the given action for each entry in this map until all entries
     * have been processed or the action throws an exception.
     * <p>
     * Exceptions thrown by the action are relayed to the caller.
     * <p>
     * The implementation processes each entry on the client and should only be
     * used for read-only client-side operations (such as adding map entries to
     * a UI widget, for example).
     * <p>
     * Any entry mutation caused by the specified action will not be propagated
     * to the server when this method is called on a distributed map, so it
     * should be avoided. The mutating operations on a subset of entries
     * should be implemented using one of {@link InvocableMap#invokeAll},
     * {@link #replaceAll}, {@link #compute}, or {@link #merge} methods instead.
     *
     * @param action  the action to be performed for each entry
     *
     * @since 12.2.1
     */
    @Override
    public default void forEach(BiConsumer<? super K, ? super V> action)
        {
        Objects.requireNonNull(action);
        entrySet().forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
        }

    /**
     * Perform the given action for each entry selected by the specified filter
     * until all entries have been processed or the action throws an exception.
     * <p>
     * Exceptions thrown by the action are relayed to the caller.
     * <p>
     * The implementation processes each entry on the client and should only be
     * used for read-only client-side operations (such as adding map entries to
     * a UI widget, for example).
     * <p>
     * Any entry mutation caused by the specified action will not be propagated
     * to the server when this method is called on a distributed map, so it
     * should be avoided. The mutating operations on a subset of entries
     * should be implemented using one of {@link InvocableMap#invokeAll},
     * {@link #replaceAll}, {@link #compute}, or {@link #merge} methods instead.
     *
     * @param filter  the filter that should be used to select entries
     * @param action  the action to be performed for each entry
     *
     * @since 12.2.1
     */
    public default void forEach(Filter filter, BiConsumer<? super K, ? super V> action)
        {
        Objects.requireNonNull(action);
        entrySet(filter).forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
        }


    // ----- QueryMap.Entry interface ---------------------------------------

    /**
     * A QueryMap Entry exposes additional index-related operation that the
     * basic Map Entry does not.
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     *
     * @since Coherence 3.2
     */
    public interface Entry<K, V>
            extends Map.Entry<K, V>
        {
        /**
         * Extract a value out of the Entry's key or value. Calling this method is
         * semantically equivalent to <tt>extractor.extract(entry.getValue())</tt>,
         * but this method may be significantly less expensive. For example, the
         * resultant value may be obtained from a forward index, avoiding a
         * potential object de-serialization.
         *
         * @param <T>        the type of the value to extract from
         * @param <E>        the type of value that will be extracted
         * @param extractor  a ValueExtractor to apply to the Entry's key or value
         *
         * @return the extracted value
         */
        public <T, E> E extract(ValueExtractor<T, E> extractor);

        /**
         * Extract a value out of the Entry's key. Calling this method is
         * semantically equivalent to <tt>extractor.extract(entry.getKey())</tt>,
         * but this method may be significantly less expensive. For example, the
         * resultant value may be obtained from a forward index, avoiding a
         * potential object de-serialization.
         *
         * @param <E>        the type of value that will be extracted
         * @param extractor  a ValueExtractor to apply to the Entry's key
         *
         * @return the extracted value
         */
        public default <E> E extractFromKey(ValueExtractor<? super K, E> extractor)
            {
            return extract(new KeyExtractor<>(extractor));
            }

        /**
         * Extract a value out of the Entry's value. Calling this method is
         * semantically equivalent to <tt>extractor.extract(entry.getValue())</tt>,
         * but this method may be significantly less expensive. For example, the
         * resultant value may be obtained from a forward index, avoiding a
         * potential object de-serialization.
         *
         * @param <E>        the type of value that will be extracted
         * @param extractor  a ValueExtractor to apply to the Entry's value
         *
         * @return the extracted value
         */
        public default <E> E extractFromValue(ValueExtractor<? super V, E> extractor)
            {
            return extract(extractor);
            }
        }
    }