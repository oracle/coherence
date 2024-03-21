/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.collections;

import com.oracle.coherence.common.base.InverseComparator;
import com.oracle.coherence.common.base.Nullable;

import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.comparator.SafeComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import java.util.stream.Collectors;

/**
 * An implementation of {@link ConcurrentNavigableMap} that supports
 * {@code null} keys and values.
 * <p>
 * This class wraps {@link ConcurrentSkipListMap} and ensures that all keys and values
 * are converted to {@link Nullable} representations on the way in, and back to
 * their original representation on the way out.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.02.10
 */
public class NullableSortedMap<K, V>
        implements ConcurrentNavigableMap<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructs a new, empty map, sorted according to the
     * {@linkplain Comparable natural ordering} of the keys.
     */
    public NullableSortedMap()
        {
        f_map        = new ConcurrentSkipListMap<>(new NullableComparator());
        f_comparator = null;
        }

    /**
     * Constructs a new, empty map, sorted according to the specified
     * comparator.
     *
     * @param comparator the comparator that will be used to order this map. If
     *                   {@code null}, the
     *                   {@linkplain Comparable natural ordering} of the keys
     *                   will be used.
     */
    public NullableSortedMap(Comparator<? super K> comparator)
        {
        f_map        = new ConcurrentSkipListMap<>(new NullableComparator(comparator));
        f_comparator = comparator;
        }

    /**
     * Constructs a new map containing the same mappings as the given map,
     * sorted according to the {@linkplain Comparable natural ordering} of the
     * keys.
     *
     * @param m the map whose mappings are to be placed in this map
     *
     * @throws ClassCastException   if the keys in {@code m} are not
     *                              {@link Comparable}, or are not mutually
     *                              comparable
     * @throws NullPointerException if the specified map or any of its keys or
     *                              values are null
     */
    public NullableSortedMap(Map<? extends K, ? extends V> m)
        {
        this();
        putAll(m);
        }

    /**
     * Constructs a new map containing the same mappings and using the same
     * ordering as the specified sorted map.
     *
     * @param m the sorted map whose mappings are to be placed in this map, and
     *          whose comparator is to be used to sort this map
     *
     * @throws NullPointerException if the specified sorted map or any of its
     *                              keys or values are null
     */
    public NullableSortedMap(SortedMap<K, ? extends V> m)
        {
        this(m.comparator());
        putAll(m);
        }

    /**
     * Private constructor that is used to construct a view of this map.
     *
     * @param subMap  the portion of this map to create a view over
     */
    private NullableSortedMap(ConcurrentNavigableMap<Nullable<K>, Nullable<V>> subMap, Comparator<? super K> comparator)
        {
        f_map = subMap;
        f_comparator = comparator;
        }

    // ---- internal API ----------------------------------------------------

    /**
     * Get entry for the specified key.
     *
     * @param key  the key to get the entry for
     *
     * @return the entry for the specified key, or {@code null} if the entry doesn't exist
     */
    public Entry<K, V> getEntry(K key)
        {
        Nullable<K> nullableKey = Nullable.of(key);
        Nullable<V> value = f_map.get(nullableKey);
        return value == null ? null : new NullableEntry(new SimpleMapEntry<>(nullableKey, value));
        }

    // ---- Map interface ---------------------------------------------------

    public int size()
        {
        return f_map.size();
        }

    public boolean isEmpty()
        {
        return f_map.isEmpty();
        }

    public V get(Object key)
        {
        return Nullable.get(f_map.get(Nullable.of(key)));
        }

    public boolean containsKey(Object key)
        {
        return f_map.containsKey(Nullable.of(key));
        }

    public boolean containsValue(Object value)
        {
        return f_map.containsValue(Nullable.of(value));
        }

    public V put(K key, V value)
        {
        return Nullable.get(f_map.put(Nullable.of(key), Nullable.of(value)));
        }

    public void putAll(Map<? extends K, ? extends V> map)
        {
        this.f_map.putAll(convertMap(map));
        }

    public V remove(Object key)
        {
        return Nullable.get(f_map.remove(Nullable.of(key)));
        }

    public void clear()
        {
        f_map.clear();
        }

    public NavigableSet<K> keySet()
        {
        return new NullableSortedSet(f_map.keySet());
        }

    public Collection<V> values()
        {
        return new NullableCollection<>(f_map.values());
        }

    public Set<Entry<K, V>> entrySet()
        {
        return new NullableEntrySet(f_map.entrySet());
        }

    // ---- ConcurrentMap interface -----------------------------------------

    public V putIfAbsent(K key, V value)
        {
        return Nullable.get(f_map.putIfAbsent(Nullable.of(key), Nullable.of(value)));
        }

    public boolean remove(Object key, Object value)
        {
        return f_map.remove(Nullable.of(key), Nullable.of(value));
        }

    public boolean replace(K key, V oldValue, V newValue)
        {
        return f_map.replace(Nullable.of(key), Nullable.of(oldValue), Nullable.of(newValue));
        }

    public V replace(K key, V value)
        {
        return Nullable.get(f_map.replace(Nullable.of(key), Nullable.of(value)));
        }

    public V getOrDefault(Object key, V defaultValue)
        {
        return Nullable.get(f_map.getOrDefault(Nullable.of(key), Nullable.of(defaultValue)));
        }

    public void forEach(BiConsumer<? super K, ? super V> action)
        {
        f_map.forEach((key, value) -> action.accept(Nullable.get(key), Nullable.get(value)));
        }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
        {
        f_map.replaceAll((key, value) -> Nullable.of(function.apply(Nullable.get(key), Nullable.get(value))));
        }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        {
        return Nullable.get(f_map.computeIfAbsent(Nullable.of(key), k ->
            {
            V value = mappingFunction.apply(key);
            return value == null ? null : Nullable.of(value);
            }));
        }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return Nullable.get(f_map.computeIfPresent(Nullable.of(key), (k, v) ->
            {
            V value = remappingFunction.apply(key, Nullable.get(v));
            return value == null ? null : Nullable.of(value);
            }));
        }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return Nullable.get(f_map.compute(Nullable.of(key), (k, v) ->
            {
            V value = remappingFunction.apply(key, Nullable.get(v));
            return value == null ? null : Nullable.of(value);
            }));
        }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);

        return Nullable.get(f_map.merge(Nullable.of(key), Nullable.of(value), (oldValue, ignore) ->
            {
            V newValue = remappingFunction.apply(Nullable.get(oldValue), value);
            return newValue == null ? null : Nullable.of(newValue);
            }));
        }

    // ---- SortedMap interface ---------------------------------------------

    public Comparator<? super K> comparator()
        {
        return f_comparator;
        }

    public K firstKey()
        {
        return Nullable.get(f_map.firstKey());
        }

    public K lastKey()
        {
        return Nullable.get(f_map.lastKey());
        }

    // ---- NavigableMap interface ------------------------------------------

    public K lowerKey(K key)
        {
        return Nullable.get(f_map.lowerKey(Nullable.of(key)));
        }

    public K floorKey(K key)
        {
        return Nullable.get(f_map.floorKey(Nullable.of(key)));
        }

    public K ceilingKey(K key)
        {
        return Nullable.get(f_map.ceilingKey(Nullable.of(key)));
        }

    public K higherKey(K key)
        {
        return Nullable.get(f_map.higherKey(Nullable.of(key)));
        }

    public Entry<K, V> lowerEntry(K key)
        {
        Entry<Nullable<K>, Nullable<V>> entry = f_map.lowerEntry(Nullable.of(key));
        return entry == null ? null : new NullableEntry(entry);
        }

    public Entry<K, V> floorEntry(K key)
        {
        Entry<Nullable<K>, Nullable<V>> entry = f_map.floorEntry(Nullable.of(key));
        return entry == null ? null : new NullableEntry(entry);
        }

    public Entry<K, V> ceilingEntry(K key)
        {
        Entry<Nullable<K>, Nullable<V>> entry = f_map.ceilingEntry(Nullable.of(key));
        return entry == null ? null : new NullableEntry(entry);
        }

    public Entry<K, V> higherEntry(K key)
        {
        Entry<Nullable<K>, Nullable<V>> entry = f_map.higherEntry(Nullable.of(key));
        return entry == null ? null : new NullableEntry(entry);
        }

    public Entry<K, V> firstEntry()
        {
        Entry<Nullable<K>, Nullable<V>> entry = f_map.firstEntry();
        return entry == null ? null : new NullableEntry(entry);
        }

    public Entry<K, V> lastEntry()
        {
        Entry<Nullable<K>, Nullable<V>> entry = f_map.lastEntry();
        return entry == null ? null : new NullableEntry(entry);
        }

    public Entry<K, V> pollFirstEntry()
        {
        Entry<Nullable<K>, Nullable<V>> entry = f_map.pollFirstEntry();
        return entry == null ? null : new NullableEntry(entry);
        }

    public Entry<K, V> pollLastEntry()
        {
        Entry<Nullable<K>, Nullable<V>> entry = f_map.pollLastEntry();
        return entry == null ? null : new NullableEntry(entry);
        }

    // ---- ConcurrentNavigableMap interface --------------------------------

    public ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive)
        {
        return new NullableSortedMap<>(f_map.subMap(Nullable.of(fromKey), fromInclusive, Nullable.of(toKey), toInclusive), f_comparator);
        }

    public ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive)
        {
        return new NullableSortedMap<>(f_map.headMap(Nullable.of(toKey), inclusive), f_comparator);
        }

    public ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive)
        {
        return new NullableSortedMap<>(f_map.tailMap(Nullable.of(fromKey), inclusive), f_comparator);
        }

    public ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey)
        {
        return new NullableSortedMap<>(f_map.subMap(Nullable.of(fromKey), Nullable.of(toKey)), f_comparator);
        }

    public ConcurrentNavigableMap<K, V> headMap(K toKey)
        {
        return new NullableSortedMap<>(f_map.headMap(Nullable.of(toKey)), f_comparator);
        }

    public ConcurrentNavigableMap<K, V> tailMap(K fromKey)
        {
        return new NullableSortedMap<>(f_map.tailMap(Nullable.of(fromKey)), f_comparator);
        }

    public ConcurrentNavigableMap<K, V> descendingMap()
        {
        Comparator<? super K> comparator = new InverseComparator<>(SafeComparator.ensureSafe(f_comparator));
        return new NullableSortedMap<>(f_map.descendingMap(), comparator);
        }

    public NavigableSet<K> navigableKeySet()
        {
        return keySet();
        }

    public NavigableSet<K> descendingKeySet()
        {
        return keySet().descendingSet();
        }

    // ---- Object methods --------------------------------------------------

    public int hashCode()
        {
        return f_map.hashCode();
        }

    public String toString()
        {
        return f_map.toString();
        }

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof Map<?, ?>))
            {
            return false;
            }
        Map<?, ?> that = (Map<?, ?>) o;
        return Objects.equals(f_map, convertMap(that));
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Convert each key and value in the specified map into a {@link Nullable}.
     *
     * @param map  the map to convert
     *
     * @return the converted map
     *
     * @param <K>  the type of map keys
     * @param <V>  the type of map values
     */
    private static <K, V> Map<Nullable<K>, Nullable<V>> convertMap(Map<? extends K, ? extends V> map)
        {
        Map<Nullable<K>, Nullable<V>> mapConv = new HashMap<>(map.size());
        map.forEach((key, value) -> mapConv.put(Nullable.of(key), Nullable.of(value)));
        return mapConv;
        }

    // ---- inner class: NullableComparator ---------------------------------

    /**
     * NullableComparator implementation.
     */
    private class NullableComparator implements Comparator<Nullable<K>>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code NullableComparator} instance that can be used to
         * compare {@link Nullable} instances that wrap {@link Comparable}
         * objects using their natural ordering.
         */
        public NullableComparator()
            {
            f_comparator = null;
            }

        /**
         * Construct {@code NullableComparator} instance that will use specified
         * {@code comparator} to compare objects wrapped by {@link Nullable}.
         *
         * @param comparator  a {@link Comparator} to use to compare wrapped values
         */
        public NullableComparator(Comparator<? super K> comparator)
            {
            f_comparator = comparator;
            }

        // ---- Comparator interface ----------------------------------------

        public int compare(Nullable<K> o1, Nullable<K> o2)
            {
            return SafeComparator.compareSafe(f_comparator, Nullable.get(o1), Nullable.get(o2));
            }

        // ---- data members ------------------------------------------------

        /**
         * A {@link Comparator} to use to compare wrapped values; can be {@code null}.
         */
        private final Comparator<? super K> f_comparator;
        }

    // ---- inner class: NullableIterator -----------------------------------

    /**
     * NullableIterator implementation.
     *
     * @param <T> the type of values to iterate over
     */
    private static class NullableIterator<T> implements Iterator<T>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct NullableIterator instance.
         *
         * @param iterator  the underlying iterator over {@link Nullable} values
         */
        NullableIterator(Iterator<Nullable<T>> iterator)
            {
            f_iterator = iterator;
            }

        // ---- Iterator interface ------------------------------------------

        public boolean hasNext()
            {
            return f_iterator.hasNext();
            }

        public T next()
            {
            return Nullable.get(f_iterator.next());
            }

        public void remove()
            {
            f_iterator.remove();
            }

        // ---- data members ------------------------------------------------

        private final Iterator<Nullable<T>> f_iterator;
        }

    // ---- inner class: NullableEntryIterator ------------------------------

    /**
     * NullableEntryIterator implementation.
     */
    private class NullableEntryIterator implements Iterator<Entry<K, V>>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct NullableEntryIterator instance.
         *
         * @param iterator  the underlying iterator over {@link Nullable} entries
         */
        NullableEntryIterator(Iterator<Entry<Nullable<K>, Nullable<V>>> iterator)
            {
            this.f_iterator = iterator;
            }

        // ---- Iterator interface ------------------------------------------

        public boolean hasNext()
            {
            return f_iterator.hasNext();
            }

        public Entry<K, V> next()
            {
            return new NullableEntry(f_iterator.next());
            }

        public void remove()
            {
            f_iterator.remove();
            }

        // ---- data members ------------------------------------------------

        private final Iterator<Entry<Nullable<K>, Nullable<V>>> f_iterator;
        }

    // ---- inner class: NullableEntry --------------------------------------

    /**
     * NullableEntry implementation.
     */
    private class NullableEntry
            implements Entry<K, V>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct NullableEntry instance.
         *
         * @param entry  the underlying entry 
         */
        NullableEntry(Entry<Nullable<K>, Nullable<V>> entry)
            {
            Objects.requireNonNull(entry);

            this.entry = entry;
            }

        // ---- Entry interface ---------------------------------------------

        public K getKey()
            {
            return Nullable.get(entry.getKey());
            }

        public V getValue()
            {
            return Nullable.get(entry.getValue());
            }

        public V setValue(V value)
            {
            return Nullable.get(entry.setValue(Nullable.of(value)));
            }

        // ---- Object methods ----------------------------------------------

        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (!(o instanceof Map.Entry<?,?>))
                {
                return false;
                }
            Entry<?,?> that = (Entry<?,?>) o;
            return Objects.equals(entry, new SimpleMapEntry<>(Nullable.of(that.getKey()), Nullable.of(that.getValue())));
            }

        public int hashCode()
            {
            return entry.hashCode();
            }

        public String toString()
            {
            return entry.toString();
            }

        // ---- data members ------------------------------------------------

        private final Entry<Nullable<K>, Nullable<V>> entry;
        }

    // ---- inner class: NullableEntrySet -----------------------------------

    /**
     * NullableEntrySet implementation.
     */
    private class NullableEntrySet
            implements Set<Entry<K, V>>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct NullableEntrySet instance.
         *
         * @param setEntries  the underlying entry set
         */
        NullableEntrySet(Set<Entry<Nullable<K>, Nullable<V>>> setEntries)
            {
            f_setEntries = setEntries;
            }

        // ---- Set interface -----------------------------------------------

        public int size()
            {
            return f_setEntries.size();
            }

        public boolean isEmpty()
            {
            return f_setEntries.isEmpty();
            }

        public boolean contains(Object o)
            {
            if (o instanceof Entry<?, ?>)
                {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                return f_setEntries.contains(new SimpleMapEntry<>(Nullable.of(entry.getKey()), Nullable.of(entry.getValue())));
                }
            return false;
            }

        public Iterator<Entry<K, V>> iterator()
            {
            return new NullableEntryIterator(f_setEntries.iterator());
            }

        public Object[] toArray()
            {
            return f_setEntries.stream().map(NullableEntry::new).toArray();
            }

        public <T> T[] toArray(T[] a)
            {
            return f_setEntries.stream().map(NullableEntry::new).toList().toArray(a);
            }

        public boolean add(Entry<K, V> entry)
            {
            return f_setEntries.add(new SimpleMapEntry<>(Nullable.of(entry.getKey()), Nullable.of(entry.getValue())));
            }

        public boolean remove(Object o)
            {
            if (o instanceof Entry<?, ?>)
                {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                return f_setEntries.remove(new SimpleMapEntry<>(Nullable.of(entry.getKey()), Nullable.of(entry.getValue())));
                }
            return false;
            }

        public boolean containsAll(Collection<?> c)
            {
            if (c != this)
                {
                for (Object e : c)
                    {
                    if (e == null || !contains(e))
                        {
                        return false;
                        }
                    }
                }
            return true;
            }

        public boolean addAll(Collection<? extends Entry<K, V>> c)
            {
            Objects.requireNonNull(c);
            List<Entry<Nullable<K>, Nullable<V>>> listAdd = new ArrayList<>(c.size());
            for (Entry<K, V> e : c)
                {
                if (e != null)
                    {
                    listAdd.add(new SimpleMapEntry<>(Nullable.of(e.getKey()), Nullable.of(e.getValue())));
                    }
                }
            return f_setEntries.addAll(listAdd);
            }

        @SuppressWarnings("unchecked")
        public boolean removeAll(Collection<?> c)
            {
            Objects.requireNonNull(c);
            List<Entry<Nullable<K>, Nullable<V>>> listRemove = new ArrayList<>(c.size());
            for (Entry<K, V> e : (Collection<Entry<K, V>>) c)
                {
                if (e != null)
                    {
                    listRemove.add(new SimpleMapEntry<>(Nullable.of(e.getKey()), Nullable.of(e.getValue())));
                    }
                }
            return f_setEntries.removeAll(listRemove);
            }

        @SuppressWarnings("unchecked")
        public boolean retainAll(Collection<?> c)
            {
            Objects.requireNonNull(c);
            List<Entry<Nullable<K>, Nullable<V>>> listRetain = new ArrayList<>(c.size());
            for (Entry<K, V> e : (Collection<Entry<K, V>>) c)
                {
                if (e != null)
                    {
                    listRetain.add(new SimpleMapEntry<>(Nullable.of(e.getKey()), Nullable.of(e.getValue())));
                    }
                }
            return f_setEntries.retainAll(listRetain);
            }

        public void clear()
            {
            f_setEntries.clear();
            }

        // ---- Object methods ----------------------------------------------

        @SuppressWarnings("unchecked")
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (!(o instanceof Set))
                {
                return false;
                }
            Set<Entry<?, ?>> that = (Set<Entry<?, ?>>) o;
            Set<Entry<Nullable<?>, Nullable<?>>> setComp = new HashSet<>(that.size());
            for (Entry<?, ?> e : that)
                {
                if (e != null)
                    {
                    setComp.add(new SimpleMapEntry<>(Nullable.of(e.getKey()), Nullable.of(e.getValue())));
                    }
                }
            return Objects.equals(f_setEntries, setComp);
            }

        public int hashCode()
            {
            return f_setEntries.hashCode();
            }

        public String toString()
            {
            return f_setEntries.toString();
            }

        // ---- data members ------------------------------------------------

        private final Set<Entry<Nullable<K>, Nullable<V>>> f_setEntries;
        }

    // ---- inner class: NullableSet ----------------------------------------

    /**
     * NullableSortedSet implementation.
     */
    private class NullableSortedSet
            extends NullableCollection<K>
            implements NavigableSet<K>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct NullableSet instance.
         *
         * @param set  the underlying set of {@link Nullable} values
         */
        NullableSortedSet(NavigableSet<Nullable<K>> set, boolean fDescending)
            {
            super(set);
            f_set = set;
            f_fDescending = fDescending;
            }


        /**
         * Construct NullableSet instance.
         *
         * @param set  the underlying set of {@link Nullable} values
         */
        NullableSortedSet(NavigableSet<Nullable<K>> set)
            {
            this(set, false);
            }

        /**
         * Construct NullableSet instance.
         *
         * @param set  the underlying set of {@link Nullable} values
         */
        NullableSortedSet(SortedSet<Nullable<K>> set)
            {
            this((NavigableSet<Nullable<K>>) set);
            }

        // ---- Collection methods ------------------------------------------

        public Iterator<K> iterator()
            {
            return new NullableIterator<>(f_set.iterator());
            }

        // ---- NavigableSet methods ----------------------------------------

        public K lower(K t)
            {
            return Nullable.get(f_set.lower(Nullable.of(t)));
            }

        public K floor(K t)
            {
            return Nullable.get(f_set.floor(Nullable.of(t)));
            }

        public K ceiling(K t)
            {
            return Nullable.get(f_set.ceiling(Nullable.of(t)));
            }

        public K higher(K t)
            {
            return Nullable.get(f_set.higher(Nullable.of(t)));
            }

        public K pollFirst()
            {
            return Nullable.get(f_set.pollFirst());
            }

        public K pollLast()
            {
            return Nullable.get(f_set.pollLast());
            }

        public NavigableSet<K> descendingSet()
            {
            return new NullableSortedSet(f_set.descendingSet(), true);
            }

        public Iterator<K> descendingIterator()
            {
            return descendingSet().iterator();
            }

        public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive)
            {
            return new NullableSortedSet(f_set.subSet(Nullable.of(fromElement), fromInclusive, Nullable.of(toElement), toInclusive));
            }

        public NavigableSet<K> headSet(K toElement, boolean inclusive)
            {
            return new NullableSortedSet(f_set.headSet(Nullable.of(toElement), inclusive));
            }

        public NavigableSet<K> tailSet(K fromElement, boolean inclusive)
            {
            return new NullableSortedSet(f_set.tailSet(Nullable.of(fromElement), inclusive));
            }

        // ---- SortedSet interface -----------------------------------------

        public SortedSet<K> subSet(K fromElement, K toElement)
            {
            return new NullableSortedSet(f_set.subSet(Nullable.of(fromElement), Nullable.of(toElement)));
            }

        public SortedSet<K> headSet(K toElement)
            {
            return new NullableSortedSet(f_set.headSet(Nullable.of(toElement)));
            }

        public SortedSet<K> tailSet(K fromElement)
            {
            return new NullableSortedSet(f_set.tailSet(Nullable.of(fromElement)));
            }

        public Comparator<? super K> comparator()
            {
            return f_fDescending ? new InverseComparator<>(SafeComparator.ensureSafe(f_comparator)) : f_comparator;
            }

        public K first()
            {
            return Nullable.get(f_set.first());
            }

        public K last()
            {
            return Nullable.get(f_set.last());
            }

        // ---- Object methods ----------------------------------------------

        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (!(o instanceof Set<?>))
                {
                return false;
                }
            Set<?> that = (Set<?>) o;
            return Objects.equals(f_set, that.stream().map(Nullable::of).collect(Collectors.toUnmodifiableSet()));
            }

        // ---- data members ------------------------------------------------

        private final NavigableSet<Nullable<K>> f_set;

        private final boolean f_fDescending;
        }

    // ---- inner class: NullableCollection ---------------------------------

    /**
     * NullableCollection implementation.
     *
     * @param <T> the type of collection elements
     */
    private static class NullableCollection<T>
            implements Collection<T>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct NullableCollection instance.
         *
         * @param col  the underlying collection of {@link Nullable} values
         */
        NullableCollection(Collection<Nullable<T>> col)
            {
            f_col = col;
            }

        // ---- Collection interface ----------------------------------------

        public int size()
            {
            return f_col.size();
            }

        public boolean isEmpty()
            {
            return f_col.isEmpty();
            }

        public boolean contains(Object o)
            {
            return f_col.contains(Nullable.of(o));
            }

        public Iterator<T> iterator()
            {
            return new NullableIterator<>(f_col.iterator());
            }

        public Object[] toArray()
            {
            return f_col.stream().map(k -> Nullable.get(k)).toArray();
            }

        public <T> T[] toArray(T[] a)
            {
            return f_col.stream().map(k -> Nullable.get(k)).toList().toArray(a);
            }

        public boolean add(T key)
            {
            return f_col.add(Nullable.of(key));
            }

        public boolean remove(Object o)
            {
            return f_col.remove(Nullable.of(o));
            }

        public boolean containsAll(Collection<?> c)
            {
            return f_col.containsAll(c.stream().map(Nullable::of).toList());
            }

        public boolean addAll(Collection<? extends T> c)
            {
            List<Nullable<T>> listAdd = new ArrayList<>(c.size());
            c.forEach(e -> listAdd.add(Nullable.of(e)));
            return f_col.addAll(listAdd);
            }

        public boolean retainAll(Collection<?> c)
            {
            return f_col.retainAll(c.stream().map(Nullable::of).toList());
            }

        public boolean removeAll(Collection<?> c)
            {
            return f_col.removeAll(c.stream().map(Nullable::of).toList());
            }

        public void clear()
            {
            f_col.clear();
            }

        // ---- Object methods ----------------------------------------------

        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (!(o instanceof Collection<?>))
                {
                return false;
                }
            Collection<?> that = (Collection<?>) o;
            return Objects.equals(f_col, that.stream().map(Nullable::of).toList());
            }

        public int hashCode()
            {
            return f_col.hashCode();
            }

        public String toString()
            {
            return f_col.toString();
            }

        // ---- data members ------------------------------------------------

        private final Collection<Nullable<T>> f_col;
        }

    // ---- constants -------------------------------------------------------

    /**
     * An empty, immutable NullableSortedMap instance.
     */
    public static final NavigableMap<?, ?> EMPTY = Collections.unmodifiableNavigableMap(new NullableSortedMap<>());

    // ---- data members ----------------------------------------------------

    private final ConcurrentNavigableMap<Nullable<K>, Nullable<V>> f_map;
    private final Comparator<? super K> f_comparator;
    }
