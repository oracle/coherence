/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.collections;

import com.oracle.coherence.common.base.Nullable;

import com.tangosol.util.SimpleMapEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import java.util.stream.Collectors;

/**
 * An implementation of {@link ConcurrentMap} that supports {@code null} keys
 * and values.
 * <p>
 * This class wraps {@link ConcurrentHashMap} and ensures that all keys and values
 * are converted to {@link Nullable} representations on the way in, and back to
 * their original representation on the way out.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.12
 */
public class NullableConcurrentMap<K, V>
        implements ConcurrentMap<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a new, empty map with the default initial table size (16).
     */
    public NullableConcurrentMap()
        {
        this(new ConcurrentHashMap<>());
        }

    /**
     * Construct a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param cInitialCapacity  the initial capacity
     *
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public NullableConcurrentMap(int cInitialCapacity)
        {
        this(new ConcurrentHashMap<>(cInitialCapacity));
        }

    /**
     * Construct a new, empty map with an initial table size based on
     * the given number of elements ({@code cInitialCapacity}) and
     * initial table density ({@code flLoadFactor}).
     *
     * @param cInitialCapacity  the initial capacity
     * @param flLoadFactor      the load factor (table density)
     *
     * @throws IllegalArgumentException  if the initial capacity is negative
     *                                   or the load factor is non-positive
     */
    public NullableConcurrentMap(int cInitialCapacity, float flLoadFactor)
        {
        this(new ConcurrentHashMap<>(cInitialCapacity, flLoadFactor));
        }

    /**
     * Construct a new map with the same mappings as the given map.
     *
     * @param map  the map to construct this map from
     */
    public NullableConcurrentMap(Map<? extends K, ? extends V> map)
        {
        f_map = new ConcurrentHashMap<>(map.size());
        putAll(map);
        }

    // ---- internal API ----------------------------------------------------

    /**
     * Return the underlying {@link ConcurrentHashMap}.
     *
     * @return the underlying {@link ConcurrentHashMap}
     */
    ConcurrentHashMap<Nullable<K>, Nullable<V>> getMap()
        {
        return f_map;
        }

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

    public Set<K> keySet()
        {
        return new NullableSet<>(f_map.keySet(Nullable.empty()));
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
     * NullableSet implementation.
     *
     * @param <T> the type of set elements
     */
    private static class NullableSet<T>
            extends NullableCollection<T>
            implements Set<T>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct NullableSet instance.
         *
         * @param set  the underlying set of {@link Nullable} values
         */
        NullableSet(Set<Nullable<T>> set)
            {
            super(set);
            f_set = set;
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

        private final Set<Nullable<T>> f_set;
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
     * An empty, immutable NullableConcurrentMap instance.
     */
    public static final Map<?, ?> EMPTY = Collections.unmodifiableMap(new NullableConcurrentMap<>(1, 1.0f));

    // ---- data members ----------------------------------------------------

    private final ConcurrentHashMap<Nullable<K>, Nullable<V>> f_map;
    }
