/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.cache.SimpleMemoryCalculator;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.Base;
import com.tangosol.util.ChainedSet;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.SafeSortedMap;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A composite view over partition indices for the specified partition set.
 *
 * @param <K>  the type of cache keys
 * @param <V>  the type of cache values
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PartitionedIndexMap<K, V> extends AbstractKeyBasedMap<ValueExtractor<V, ?>, MapIndex<K, V, ?>>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PartitionedIndexMap} instance.
     *
     * @param ctx             the {@link BackingMapContext context} associated with the indexed cache
     * @param mapPartitioned  the map of partition indices, keyed by partition number
     * @param partitions      the set of partitions to return an index map for, or {@code null}
     *                        to return an index for all owned partitions
     */
    public PartitionedIndexMap(BackingMapContext ctx,
                               Map<Integer, Map<ValueExtractor<V, ?>, MapIndex<K, V, ?>>> mapPartitioned,
                               PartitionSet partitions)
        {
        f_ctx            = ctx;
        f_mapPartitioned = mapPartitioned;
        f_partitions     = partitions;
        }

    // ---- public API ------------------------------------------------------

    /**
     * Return a {@link MapIndex} for the specified extractor.
     *
     * @param extractor  the value extractor to get the {@code MapIndex} for
     *
     * @return a {@link MapIndex} for the specified extractor
     */
    public <E> MapIndex<K, V, E> get(ValueExtractor<V, E> extractor)
        {
        PartitionSet partitions = f_partitions;

        if (hasIndex(extractor))
            {
            return partitions != null && partitions.cardinality() == 1
                   // optimize for single partition
                   ? (MapIndex<K, V, E>) f_mapPartitioned.get(partitions.next(0)).get(extractor)
                   : new PartitionedIndex<>(extractor);
            }

        return null;
        }

    private <E> boolean hasIndex(ValueExtractor<V, E> extractor)
        {
        return f_mapPartitioned.values().stream().anyMatch(indexMap -> indexMap.containsKey(extractor));
        }

    /**
     * Return the set of partitions this index view is for.
     *
     * @return the set of partitions this index view is for
     */
    public PartitionSet getPartitions()
        {
        return f_partitions;
        }

    // ---- AbstractKeyBasedMap interface -----------------------------------

    @Override
    public MapIndex<K, V, Object> get(Object oKey)
        {
        return get((ValueExtractor) oKey);
        }

    @Override
    protected Iterator<ValueExtractor<V, ?>> iterateKeys()
        {
        return f_mapPartitioned.values().stream()
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .iterator();
        }

    // ---- data members ----------------------------------------------------

    /**
     * Provides unified view over multiple partition-level MapIndex instances
     * for a specific index.
     *
     * @param <E>  the type of indexed attribute
     */
    public class PartitionedIndex<E>
            implements MapIndex<K, V, E>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@link PartitionedIndex} instance.
         *
         * @param extractor  the {@code ValueExtractor} for this index
         */
        PartitionedIndex(ValueExtractor<V, E> extractor)
            {
            f_extractor = extractor;
            f_mapIndex  = new ConcurrentHashMap<>();

            
            // create a stable "snapshot" of indices for the specified partition set,
            // to avoid returning null from the index map if the partition transfers out
            for (Map.Entry<Integer, Map<ValueExtractor<V, ?>, MapIndex<K, V, ?>>> entry : f_mapPartitioned.entrySet())
                {
                int nPart = entry.getKey();
                if (f_partitions == null || f_partitions.contains(nPart))
                    {
                    Map<ValueExtractor<V, ?>, MapIndex<K, V, ?>> indexMap = entry.getValue();
                    MapIndex<K, V, E>                            mapIndex = (MapIndex<K, V, E>) indexMap.get(extractor);

                    if (mapIndex != null)
                        {
                        f_mapIndex.put(nPart, mapIndex);
                        }
                    }
                }
            }

        // ---- MapIndex interface ------------------------------------------

        @Override
        public ValueExtractor<V, E> getValueExtractor()
            {
            return f_extractor;
            }

        @Override
        public boolean isOrdered()
            {
            return f_mapIndex.values().stream().anyMatch(MapIndex::isOrdered);
            }

        @Override
        public boolean isPartial()
            {
            return f_mapIndex.values().stream().anyMatch(MapIndex::isPartial);
            }

        @Override
        public Map<E, Set<K>> getIndexContents()
            {
            return isOrdered()
                   ? new SortedIndexContents(f_mapIndex.values().stream().map(map -> (SortedMap<E, Set<K>>) map.getIndexContents()).collect(Collectors.toList()), getComparator())
                   : new IndexContents(f_mapIndex.values().stream().map(MapIndex::getIndexContents).collect(Collectors.toList()));
            }

        @Override
        public Object get(K key)
            {
            int               nPart    = f_ctx.getManagerContext().getKeyPartition(key);
            MapIndex<K, V, E> mapIndex = f_mapIndex.get(nPart);

            return mapIndex == null ? NO_VALUE : mapIndex.get(key);
            }

        @Override
        public Comparator<E> getComparator()
            {
            Optional<MapIndex<K, V, E>> mapIndex = f_mapIndex.values().stream().findAny();
            return mapIndex.map(MapIndex::getComparator).orElse(null);
            }

        @Override
        public void insert(Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException("PartitionedIndex is read-only");
            }

        @Override
        public void update(Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException("PartitionedIndex is read-only");
            }

        @Override
        public void delete(Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException("PartitionedIndex is read-only");
            }

        @Override
        public long getUnits()
            {
            return Math.round(new UnitCalculator().calculateIndexOverhead() +
                              f_mapIndex.values().stream().mapToLong(MapIndex::getUnits).sum() * 1.1);  // we seem to be under-reporting by 10% or so
            }

        // ---- Object methods ----------------------------------------------

        @Override
        public String toString()
            {
            return toString(false);
            }

        /**
        * Returns a string representation of this PartitionedIndex.  If called in
        * verbose mode, include the contents of the index (the inverse
        * index). Otherwise, just print the number of entries in the index.
        *
        * @param fVerbose  if true then print the content of the index otherwise
        *                  print the number of entries
        *
        * @return a String representation of this SimpleMapIndex
        */
        public String toString(boolean fVerbose)
            {
            return ClassHelper.getSimpleName(getClass())
                   + ": Extractor=" + getValueExtractor()
                   + ", Ordered=" + isOrdered()
                   + ", Footprint=" + Base.toMemorySizeString(getUnits(), false)
                   + ", Content="
                   + (fVerbose ? getIndexContents().keySet() : getIndexContents().size());
            }

        // ---- inner class: IndexCalculator --------------------------------

        private class UnitCalculator
                extends SimpleMapIndex.IndexCalculator
            {
            /**
             * Construct an IndexCalculator which allows for conversion of items into a
             * serialized format. The calculator may use the size of the serialized value
             * representation to approximate the size of indexed values.
             */
            public UnitCalculator()
                {
                super(f_ctx, null);
                }

            public long calculateIndexOverhead()
                {
                int cPart  = f_mapIndex.size();
                return calculateShallowSize(ConcurrentHashMap.class)
                       + (long) (ENTRY_OVERHEAD + calculateShallowSize(ValueExtractor.class) + MAP_OVERHEAD + ENTRY_OVERHEAD) * cPart;
                }
            }

        // ---- inner class: IndexContents ----------------------------------

        /**
         * Provides composite view over the contents/inverse maps of unordered
         * partitioned indices.
         *
         * @param <T>  the type of inverse index Map
         */
        private class IndexContents<T extends Map<E, Set<K>>>
                implements Map<E, Set<K>>
            {
            // ---- constructors --------------------------------------------

            /**
             * Construct an instance of {@code IndexContents}.
             *
             * @param colContents  a collection of partition indices covered by
             *                     this view
             */
            IndexContents(Collection<T> colContents)
                {
                f_colContents = colContents;
                f_mapContents = new HashMap<>();
                }

            // ---- Map interface -------------------------------------------

            @Override
            public int size()
                {
                return (int) f_colContents.stream().flatMap(map -> map.keySet().stream()).distinct().count();
                }

            @Override
            public boolean isEmpty()
                {
                return f_colContents.stream().allMatch(Map::isEmpty);
                }

            @Override
            public boolean containsKey(Object oKey)
                {
                return !f_colContents.isEmpty() && f_colContents.stream().anyMatch(map -> map.containsKey(oKey));
                }

            @Override
            public boolean containsValue(Object oValue)
                {
                return values().stream().anyMatch(set -> set.equals(oValue));
                }

            @Override
            public Set<K> get(Object oKey)
                {
                return f_mapContents.computeIfAbsent((E) oKey, k ->
                    {
                    Set[] sets = f_colContents.stream()
                            .map(map -> map.get(k))
                            .filter(Objects::nonNull)
                            .toArray(Set[]::new);
                    
                    return new ChainedSet<K>(sets);
                    });
                }

            @Override
            public Set<K> put(E key, Set<K> value)
                {
                throw new UnsupportedOperationException("PartitionedIndex is read-only");
                }

            @Override
            public Set<K> remove(Object key)
                {
                throw new UnsupportedOperationException("PartitionedIndex is read-only");
                }

            @Override
            @SuppressWarnings("NullableProblems")
            public void putAll(Map<? extends E, ? extends Set<K>> m)
                {
                throw new UnsupportedOperationException("PartitionedIndex is read-only");
                }

            @Override
            public void clear()
                {
                throw new UnsupportedOperationException("PartitionedIndex is read-only");
                }

            @Override
            public Set<E> keySet()
                {
                Set<E> setKeys = m_setKeys;
                if (setKeys == null)
                    {
                    setKeys = m_setKeys = f_colContents.stream().flatMap(map -> map.keySet().stream()).collect(Collectors.toSet());
                    }
                return setKeys;
                }

            @Override
            @SuppressWarnings("SimplifyStreamApiCallChains")
            public Collection<Set<K>> values()
                {
                return entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet());
                }

            @Override
            public Set<Map.Entry<E, Set<K>>> entrySet()
                {
                return keySet().stream()
                        .map(Entry::new)
                        .collect(Collectors.toSet());
                }

            // ---- inner class: Entry --------------------------------------

            /**
             * Virtual inverse index Entry.
             */
            class Entry implements Map.Entry<E, Set<K>>
                {
                /**
                 * Construct an {@code Entry}.
                 *
                 * @param key  the key for this entry
                 */
                Entry(E key)
                    {
                    f_key = key;
                    }

                @Override
                public E getKey()
                    {
                    return f_key;
                    }

                @Override
                public Set<K> getValue()
                    {
                    return get(f_key);
                    }

                @Override
                public Set<K> setValue(Set<K> value)
                    {
                    throw new UnsupportedOperationException("PartitionedIndex is read-only");
                    }

                // ---- data members ----------------------------------------

                /**
                 * The key for this Entry.
                 */
                private final E f_key;
                }

            // ---- data members --------------------------------------------

            /**
             * The collection of partition-level inverse indices covered by
             * this view.
             */
            protected final Collection<T> f_colContents;

            /**
             * Cached key set.
             */
            protected Set<E> m_setKeys;

            /**
             * Cached index contents.
             */
            private final Map<E, Set<K>> f_mapContents;
            }

        // ---- inner class: SortedIndexContents ----------------------------

        /**
         * Provides composite view over the contents/inverse maps of ordered
         * partitioned indices.
         */
        @SuppressWarnings("Convert2MethodRef")
        class SortedIndexContents
                extends IndexContents<SortedMap<E, Set<K>>>
                implements SortedMap<E, Set<K>>
            {
            // ---- constructors --------------------------------------------

            /**
             * Construct an instance of {@code SortedIndexContents}.
             *
             * @param colContents  the collection of partition indices covered by
             *                     this view
             * @param comparator   the Comparator to use for sorting, or
             *                     {@code null} if natural ordering should be used
             */
            SortedIndexContents(Collection<SortedMap<E, Set<K>>> colContents, Comparator<E> comparator)
                {
                super(colContents);

                f_comparator = comparator;
                }

            // ---- SortedMap interface -------------------------------------

            @Override
            public Comparator<? super E> comparator()
                {
                return f_comparator;
                }

            @Override
            public SortedMap<E, Set<K>> subMap(E fromKey, E toKey)
                {
                return (SortedMap<E, Set<K>>) f_colContents.stream()
                        .flatMap(map -> map.subMap(fromKey, toKey).entrySet().stream())
                        .collect(Collectors.toMap((Map.Entry<E, Set<K>> e) -> e.getKey(),
                                                  (Map.Entry<E, Set<K>> e) -> e.getValue(),
                                                  this::chainSets,
                                                  () -> new SafeSortedMap(f_comparator)));
                }

            @Override
            public SortedMap<E, Set<K>> headMap(E toKey)
                {
                return (SortedMap<E, Set<K>>) f_colContents.stream()
                        .flatMap(map -> map.headMap(toKey).entrySet().stream())
                        .collect(Collectors.toMap((Map.Entry<E, Set<K>> e) -> e.getKey(),
                                                  (Map.Entry<E, Set<K>> e) -> e.getValue(),
                                                  this::chainSets,
                                                  () -> new SafeSortedMap(f_comparator)));
                }

            @Override
            public SortedMap<E, Set<K>> tailMap(E fromKey)
                {
                return (SortedMap<E, Set<K>>) f_colContents.stream()
                        .flatMap(map -> map.tailMap(fromKey).entrySet().stream())
                        .collect(Collectors.toMap((Map.Entry<E, Set<K>> e) -> e.getKey(),
                                                  (Map.Entry<E, Set<K>> e) -> e.getValue(),
                                                  this::chainSets,
                                                  () -> new SafeSortedMap(f_comparator)));
                }

            @Override
            public Set<E> keySet()
                {
                Set<E> setKeys = m_setKeys;
                if (setKeys == null)
                    {
                    setKeys = m_setKeys = f_colContents.stream()
                            .flatMap(map -> map.keySet().stream())
                            .collect(Collectors.toCollection(() -> new TreeSet<>(f_comparator)));
                    }
                return setKeys;
                }

            @Override
            public E firstKey()
                {
                return ((SortedSet<E>) keySet()).first();
                }

            @Override
            public E lastKey()
                {
                return ((SortedSet<E>) keySet()).last();
                }

            // ---- helpers -------------------------------------------------

            /**
             * Create {@link ChainedSet} from two {@code Set}s or an existing
             * {@code ChainedSet} and another {@code Set}.
             *
             * @param setFirst   the first Set; could be a ChainedSet
             * @param setSecond  the second Set
             *
             * @return a ChainedSet view over specified Sets
             */
            private Set<K> chainSets(Set<K> setFirst, Set<K> setSecond)
                {
                return setFirst instanceof ChainedSet
                       ? new ChainedSet<>((ChainedSet<K>) setFirst, setSecond)
                       : new ChainedSet<>(setFirst, setSecond);
                }

            // ---- data members --------------------------------------------

            /**
             * The Comparator to use for sorting, or  {@code null} if natural
             * ordering should be used.
             */
            private final Comparator<E> f_comparator;
            }

        // ---- data members ------------------------------------------------

        /**
         * The extractor for this index.
         */
        private final ValueExtractor<V, E> f_extractor;

        /**
         * The stable view of a subset of partition indices, keyed by partition number.
         */
        private final Map<Integer, MapIndex<K, V, E>> f_mapIndex;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The {@link BackingMapContext context} associated with the indexed cache.
     */
    protected final BackingMapContext f_ctx;

    /**
     * The map of partition indices, keyed by partition number.
     */
    protected final Map<Integer, Map<ValueExtractor<V, ?>, MapIndex<K, V, ?>>> f_mapPartitioned;

    /**
     * The set of partitions to return an index map for.
     */
    protected final PartitionSet f_partitions;
    }
