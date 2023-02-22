/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.util.comparator.SafeComparator;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;

import java.util.SortedMap;
import java.util.Spliterator;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementation of a {@link java.util.SortedMap} extending {@link java.util.concurrent.ConcurrentSkipListMap}
 * to support null keys and null values. Note that unlike its super class, this class is not serializable.
 *
 * @since 23.03
 * @author mg
 */
@SuppressWarnings("unchecked")
public class SafeSortedMap<K, V>
        extends ConcurrentSkipListMap<K, V>
    {
    // ----- constructors -------------------------------------------------

    /**
     * Construct a new SafeSortedMap using the natural ordering of the
     * Comparable keys in this map.
     */
    public SafeSortedMap()
        {
        this((Comparator<K>) null);
        }

    /**
     * Construct a new SafeSortedMap copying the contents of the specified map.
     *
     * @param that  the map copied
     */
    public SafeSortedMap(SortedMap<K, V> that)
        {
        this((Comparator<K>) that.comparator());
        this.putAll(that);
        }

    /**
     * Construct a new SafeSortedMap with the specified Comparator.
     *
     * @param comparator  the comparator used to sort this map
     */
    public SafeSortedMap(Comparator<K> comparator)
        {
        super((Comparator<K> & Serializable)(o1, o2) -> {
            if (comparator != null && !(comparator instanceof SafeComparator))
                {
                try
                    {
                    return comparator.compare(o1, o2);
                    }
                catch (NullPointerException | ClassCastException ignored)
                    {
                    // handle mixed NULL/non-NULL sets below
                    }
                }

            if (o1 == NULL)
                {
                return o2 == NULL ? 0 : -1;
                }

            if (o2 == NULL)
                {
                return +1;
                }

            return ((Comparable<K>) o1).compareTo(o2);
            });
        }

    // ----- Map interface ------------------------------------------------

    @Override
    public V get(Object oKey)
        {
        return ensureReturnValue(super.get(oKey == null ? NULL : oKey));
        }

    @Override
    public V put(K oKey, V oValue)
        {
        return ensureReturnValue(super.put(oKey == null ? (K) NULL : oKey,
                                           oValue == null ? (V) NULL : oValue));
        }

    @Override
    public V remove(Object oKey)
        {
        return ensureReturnValue(super.remove(oKey == null ? NULL : oKey));
        }

    @Override
    public boolean equals(Object oMap)
        {
        if (oMap == this)
            {
            return true;
            }

        if (!(oMap instanceof Map))
            {
            return false;
            }

        for (Map.Entry<K, V> entry : ((Map<K, V>) oMap).entrySet())
            {
            Object          oKey   = entry.getKey();
            Object          oValue = entry.getValue();
            // support null values
            if (!Objects.equals(oValue, get(oKey)))
                {
                return false;
                }
            }

        return true;
        }

    @Override
    public SafeSortedMap<K, V> clone()
        {
        return new SafeSortedMap<>(this);
        }

    @Override
    public boolean containsKey(Object oKey)
        {
        return super.containsKey(oKey == null ? NULL : oKey);
        }

    @Override
    public boolean containsValue(Object oValue)
        {
        return super.containsValue(oValue == null ? NULL : oValue);
        }

    @Override
    public Set<Entry<K, V>> entrySet()
        {
        // too costly to check if any value is NULL as map size grows
        return new EntrySet<>(super.entrySet());
        }

    @Override
    public NavigableSet<K> keySet()
        {
        // optimization when the map does not contain a null key which would
        // be the first key in the map, so weak consistency is respected
        return super.containsKey(NULL)
               ? new KeySet(super.keySet())
               : super.keySet();
        }

    @Override
    public Collection<V> values()
        {
        return new Values<>(super.values());
        }

    @Override
    public ConcurrentNavigableMap<K,V> descendingMap()
        {
        return new SubMap<>(super.descendingMap());
        }

    @Override
    public NavigableSet<K> descendingKeySet()
        {
        return new KeySet<>(super.descendingKeySet());
        }

    /**
     * Locate a Map.Entry in this map based on its key.
     * <p>
     * Note: the behaviour of {#setValue} on the returned Entry is undefined in
     *       the presence of concurrent modifications
     *
     * @param oKey  the key to return an Entry for
     *
     * @return an Entry corresponding to the specified key, or null if none exists
     */
    public Entry<K, V> getEntry(K oKey)
        {
        oKey = oKey == null ? (K) NULL : oKey;

        ConcurrentNavigableMap<K,V> mapSub = super.subMap(oKey, true, oKey, true);

        return mapSub.isEmpty() ? null : ensureReturnEntry(mapSub.firstEntry());
        }

    /* ------ SortedMap API methods ------ */

    @Override
    public K firstKey()
        {
        return ensureReturnKey(super.firstKey());
        }

    @Override
    public K lastKey()
        {
        return ensureReturnKey(super.lastKey());
        }

    @Override
    public ConcurrentNavigableMap<K,V> subMap(K oFromKey,
                                              boolean fFromInclusive,
                                              K oToKey,
                                              boolean fToInclusive)
        {
        return new SubMap<>(super.subMap(oFromKey == null ? (K) NULL : oFromKey, fFromInclusive,
                                         oToKey == null ? (K) NULL : oToKey, fToInclusive));
        }

   @Override
    public ConcurrentNavigableMap<K,V> headMap(K oToKey, boolean fInclusive)
        {
        return new SubMap<>(super.headMap(oToKey == null ? (K) NULL : oToKey, fInclusive));
        }

    @Override
    public ConcurrentNavigableMap<K,V> tailMap(K oFromKey, boolean fInclusive)
        {
        return new SubMap<>(super.tailMap(oFromKey == null ? (K) NULL : oFromKey, fInclusive));
        }

    @Override
    public ConcurrentNavigableMap<K,V> subMap(K oFromKey, K oToKey)
        {
        return subMap(oFromKey, true, oToKey, false);
        }

    @Override
    public ConcurrentNavigableMap<K,V> headMap(K oToKey)
        {
        return headMap(oToKey, false);
        }

    @Override
    public ConcurrentNavigableMap<K,V> tailMap(K fromKey)
        {
        return tailMap(fromKey, true);
        }

    //----- ConcurrentMap methods ----------------------------------------

    @Override
    public V putIfAbsent(K oKey, V oValue)
        {
        return ensureReturnValue(super.putIfAbsent(oKey == null ? (K) NULL : oKey,
                                                   oValue == null ? (V) NULL : oValue));
        }

    @Override
    public V getOrDefault(Object oKey, V oDefaultValue)
        {
        return ensureReturnValue(super.getOrDefault(oKey == null ? NULL : oKey,
                                                    oDefaultValue == null ? (V) NULL : oDefaultValue));
        }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action)
        {
        super.forEach(action);
        }

    @Override
    public boolean remove(Object oKey, Object oValue)
        {
        return super.remove(oKey == null ? NULL : oKey,
                            oValue == null ? NULL : oValue);
        }

    @Override
    public boolean replace(K oKey, V oOldValue, V oNewValue)
        {
        return super.replace(oKey == null ? (K) NULL : oKey,
                             oOldValue == null ? (V) NULL : oOldValue,
                             oNewValue == null ? (V) NULL : oNewValue);
        }

    @Override
    public V replace(K oKey, V oValue)
        {
        return ensureReturnValue(super.replace(oKey == null ? (K) NULL : oKey,
                                               oValue == null ? (V) NULL : oValue));
        }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public V computeIfAbsent(K oKey, Function<? super K, ? extends V> mappingFunction)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public V computeIfPresent(K oKey, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public V compute(K oKey, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public V merge(K oKey, V oValue, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        throw new UnsupportedOperationException();
        }
    
    // ----- Java Serialization methods -----------------------------------

    private void writeObject(ObjectOutputStream out) throws IOException
        {
        throw new NotSerializableException("SafeSortedMap is not serializable");
        }

    private void readObject(ObjectInputStream in) throws IOException
        {
        throw new NotSerializableException("SafeSortedMap is not serializable");
        }

    // ----- inner class: NullableEntry -----------------------------------

    /**
     * Map.Entry implementation that supports null key/value placeholders.
     */
    protected static class NullableEntry<K, V>
        implements Entry<K, V>
        {
        // ----- constructor ----------------------------------------------

        /**
         * Constructor for a {@code NullableEntry} wrapping the original entry.
         *
         * @param entry  delegated to entry
         */
        NullableEntry(Entry<K, V> entry)
            {
            m_entry = entry;
            }

        // ----- Map.Entry interface--------------------------------------

        @Override
        public K getKey()
            {
            return m_entry == null
                   ? null
                   : ensureReturnKey(m_entry.getKey());
            }

        @Override
        public V getValue()
            {
            return m_entry == null
                   ? null
                   : ensureReturnValue(m_entry.getValue());
            }

        @Override
        public V setValue(V oValue)
            {
            if (m_entry == null)
                {
                throw new NullPointerException();
                }

            return m_entry.setValue(oValue == null ? (V) NULL : oValue);
            }

        // ----- data member ---------------------------------------------

        /**
         * The delegated to entry.
         */
        private final Entry<K, V> m_entry;
        }

    // ----- inner class: EntrySet ----------------------------------------

    /**
     * Entry set delegation of the super map implementation.
     */
    protected static class EntrySet<K,V>
            extends AbstractSet<Entry<K,V>>
        {
        // ----- constructor ----------------------------------------------

        /**
         * Constructor taking the delegated entry set.
         *
         * @param s  delegated to entry set
         */
        EntrySet(Set<Entry<K, V>> s)
            {
            m_set = s;
            }

        @Override
        public int size()
            {
            return m_set.size();
            }

        @Override
        public Iterator<Entry<K, V>> iterator()
            {
            return new EntryIterator<>(m_set.iterator());
            }


        // ----- data member ---------------------------------------------

        /**
         * The delegated to set.
         */
        private final Set<Entry<K, V>> m_set;
        }

    // ----- inner class: KeySet ----------------------------------------

    /**
     * Key set delegation of the super map implementation.
     */
    protected static class KeySet<K>
            extends AbstractSet<K>
            implements NavigableSet<K>
        {
        // ----- constructor ---------------------------------------------

        /**
         * Constructor taking the delegated key set.
         *
         * @param s  delegated to key set
         */
        KeySet(NavigableSet<K> s)
            {
            m_set = s;
            }

        @Override
        public int size()
            {
            return m_set.size();
            }

        @Override
        public K lower(K e)
            {
            return ensureReturnKey(m_set.lower(e));
            }

        @Override
        public K floor(K e)
            {
            return ensureReturnKey(m_set.floor(e));
            }

        @Override
        public K ceiling(K e)
            {
            return ensureReturnKey(m_set.ceiling(e));
            }

        @Override
        public K higher(K e)
            {
            return ensureReturnKey(m_set.higher(e));
            }

        @Override
        public K pollFirst()
            {
            return ensureReturnKey(m_set.pollFirst());
            }

        @Override
        public K pollLast()
            {
            return ensureReturnKey(m_set.pollLast());
            }

        @Override
        public NavigableSet<K> descendingSet()
            {
            return new KeySet<>(m_set.descendingSet());
            }

        @Override
        public Iterator<K> descendingIterator()
            {
            return new SortedIterator<>(m_set.descendingIterator());
            }

        @Override
        public NavigableSet<K> subSet(K oFromElement, K oToElement)
            {
            return subSet(oFromElement, true, oToElement, false);
            }

        @Override
        public NavigableSet<K> subSet(K oFromElement, boolean fFromInclusive, K oToElement, boolean fToInclusive)
            {
            return new KeySet<>(m_set.subSet(oFromElement == null ? (K) NULL : oFromElement, fFromInclusive,
                                             oToElement == null ? (K) NULL : oToElement, fToInclusive));
            }

        @Override
        public NavigableSet<K> headSet(K oToElement, boolean fInclusive)
            {
            return new KeySet<>(m_set.headSet(oToElement == null ? (K) NULL : oToElement, fInclusive));
            }

        @Override
        public NavigableSet<K> headSet(K oToElement)
            {
            return headSet(oToElement, false);
            }

        @Override
        public NavigableSet<K> tailSet(K oFromElement, boolean fInclusive)
            {
            return new KeySet<>(m_set.tailSet(oFromElement == null ? (K) NULL : oFromElement, fInclusive));
            }

        @Override
        public NavigableSet<K> tailSet(K oFromElement)
            {
            return tailSet(oFromElement == null ? (K) NULL : oFromElement, true);
            }

        @Override
        public Comparator<? super K> comparator()
            {
            return m_set.comparator();
            }

        @Override
        public K first()
            {
            return ensureReturnKey(m_set.first());
            }

        @Override
        public K last()
            {
            return ensureReturnKey(m_set.last());
            }

        @Override
        public Iterator<K> iterator()
            {
            return new SortedIterator<>(m_set.iterator());
            }

        // ----- data member ---------------------------------------------

        /**
         * The delegated to set.
         */
        final NavigableSet<K> m_set;
        }

    // ----- inner class: Values -----------------------------------------

    /**
     * Values delegation of the super map implementation.
     */
    protected static class Values<V> extends AbstractCollection<V>
        {
        Values(Collection<V> colValues)
            {
            m_colValues = colValues;
            }

        @Override
        public Iterator<V> iterator()
            {
            return new SortedIterator<>(m_colValues.iterator());
            }

        @Override
        public boolean isEmpty()
            {
            return m_colValues.isEmpty();
            }

        @Override
        public int size()
            {
            return m_colValues.size();
            }

        @Override
        public boolean contains(Object o)
            {
            return m_colValues.contains(o == null ? NULL : o);
            }

        public void clear()
            {
            m_colValues.clear();
            }

        public Object[] toArray()
            {
            return toList().toArray();
            }

        private List<V> toList()
            {
            ArrayList<V> list = new ArrayList<>(m_colValues.size());
            for (V v : m_colValues)
                {
                list.add(ensureReturnValue(v));
                }
            return list;
            }

        @SuppressWarnings("unchecked")
        public Spliterator<V> spliterator()
            {
            return (Spliterator<V>)iterator();
            }

        private final Collection<V> m_colValues;
        }

    /**
     * SubMap delegation to manage {@link #NULL} in entry key and/or value.
     *
     * @param <K> key type
     * @param <V> value type
     */
    protected static class SubMap<K, V>
            extends AbstractMap<K, V>
            implements ConcurrentNavigableMap<K, V>, Cloneable, Serializable
        {
        /**
         * Create a submap wrapper
         */
        SubMap(ConcurrentNavigableMap<K, V> oMap)
            {
            m_map = oMap;
            }

        // ----- Clone methods -----------------------------------------------

        @Override
        public SubMap<K, V> clone()
            {
            return new SubMap<>(this);
            }

        // -----  Map methods ------------------------------------------------

        @Override
        public boolean containsKey(Object oKey)
            {
            return m_map.containsKey(oKey == null ? NULL : oKey);
            }

        @Override
        public V get(Object oKey)
            {
            return ensureReturnValue(m_map.get(oKey == null ? NULL : oKey));
            }

        @Override
        public V put(K oKey, V oValue)
            {
            return ensureReturnValue(m_map.put(oKey == null ? (K) NULL : oKey,
                                               oValue == null ? (V) NULL : oValue));
            }

        @Override
        public V remove(Object oKey)
            {
            return ensureReturnValue(m_map.remove(oKey == null ? NULL : oKey));
            }

        @Override
        public int size()
            {
            return m_map.size();
            }

        @Override
        public boolean isEmpty()
            {
            return m_map.isEmpty();
            }

        @Override
        public boolean containsValue(Object oValue)
            {
            return m_map.containsValue(oValue == null ? NULL : oValue);
            }

        @Override
        public void clear()
            {
            m_map.clear();
            }

        @Override
        public ConcurrentNavigableMap<K, V> subMap(K oFromKey, boolean fFromInclusive,
                                                   K oToKey, boolean fToInclusive)
            {
            return new SubMap<>(m_map.subMap(oFromKey == null ? (K) NULL : oFromKey, fFromInclusive,
                                             oToKey == null ? (K) NULL : oToKey, fToInclusive));
            }

        @Override
        public ConcurrentNavigableMap<K, V> headMap(K oToKey, boolean fInclusive)
            {
            return new SubMap<>(m_map.subMap((K) NULL, false,
                                             oToKey == null ? (K) NULL : oToKey, fInclusive));
            }

        @Override
        public ConcurrentNavigableMap<K, V> tailMap(K oFromKey, boolean fInclusive)
            {
            return new SubMap<>(m_map.tailMap(oFromKey == null ? (K) NULL : oFromKey, fInclusive));
            }

        @Override
        public Comparator<? super K> comparator()
            {
            return m_map.comparator();
            }

        @Override
        public ConcurrentNavigableMap<K, V> subMap(K oFromKey, K oToKey)
            {
            return subMap(oFromKey,true, oToKey, false);
            }

        @Override
        public ConcurrentNavigableMap<K, V> headMap(K oToKey)
            {
            return headMap(oToKey, false);
            }

        @Override
        public ConcurrentNavigableMap<K, V> tailMap(K oFromKey)
            {
            return tailMap(oFromKey, true);
            }

        @Override
        public K firstKey()
            {
            return ensureReturnKey(m_map.firstKey());
            }

        @Override
        public K lastKey()
            {
            return ensureReturnKey(m_map.lastKey());
            }

        @Override
        public Entry<K, V> lowerEntry(K oKey)
            {
            return ensureReturnEntry(m_map.lowerEntry(oKey == null ? (K) NULL : oKey));
            }

        @Override
        public K lowerKey(K oKey)
            {
            return ensureReturnKey(m_map.lowerKey(oKey == null ? (K) NULL : oKey));
            }

        @Override
        public Entry<K, V> floorEntry(K oKey)
            {
            return ensureReturnEntry(m_map.floorEntry(oKey == null ? (K) NULL : oKey));
            }

        @Override
        public K floorKey(K oKey)
            {
            return ensureReturnKey(m_map.floorKey(oKey == null ? (K) NULL : oKey));
            }

        @Override
        public Entry<K, V> ceilingEntry(K oKey)
            {
            return ensureReturnEntry(m_map.ceilingEntry(oKey == null ? (K) NULL : oKey));
            }

        @Override
        public K ceilingKey(K oKey)
            {
            return ensureReturnValue(m_map.ceilingKey(oKey == null ? (K) NULL : oKey));
            }

        @Override
        public Entry<K, V> higherEntry(K oKey)
            {
            return ensureReturnEntry(m_map.higherEntry(oKey == null ? (K) NULL : oKey));
            }

        @Override
        public K higherKey(K oKey)
            {
            return ensureReturnKey(m_map.higherKey(oKey == null ? (K) NULL : oKey));
            }

        @Override
        public Entry<K, V> firstEntry()
            {
            return ensureReturnEntry(m_map.firstEntry());
            }

        @Override
        public Entry<K, V> lastEntry()
            {
            return ensureReturnEntry(m_map.lastEntry());
            }

        @Override
        public Entry<K, V> pollFirstEntry()
            {
            return ensureReturnEntry(m_map.pollFirstEntry());
            }

        @Override
        public Entry<K, V> pollLastEntry()
            {
            return ensureReturnEntry(m_map.pollLastEntry());
            }

        @Override
        public ConcurrentNavigableMap<K, V> descendingMap()
            {
            return new SubMap<>(m_map.descendingMap());
            }

       //----- Submap Views --------------------------------------------------

        @Override
        public NavigableSet<K> keySet()
            {
            return new KeySet<>(m_map.keySet());
            }

        @Override
        public NavigableSet<K> navigableKeySet()
            {
            return keySet();
            }

        @Override
        public Collection<V> values()
            {
            return new Values<>(m_map.values());
            }

        @Override
        public Set<Map.Entry<K, V>> entrySet()
            {
            return new EntrySet<>(m_map.entrySet());
            }

        @Override
        public NavigableSet<K> descendingKeySet()
            {
            return descendingMap().navigableKeySet();
            }

        //----- ConcurrentMap methods ----------------------------------------

        @Override
        public V putIfAbsent(K oKey, V oValue)
            {
            return ensureReturnValue(m_map.putIfAbsent(oKey == null ? (K) NULL : oKey,
                                                       oValue == null ? (V) NULL : oValue));
            }

        @Override
        public V getOrDefault(Object oKey, V oDefaultValue)
            {
            return ensureReturnValue(m_map.getOrDefault(oKey == null ? NULL : oKey,
                                                        oDefaultValue == null ? (V) NULL : oDefaultValue));
            }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action)
            {
            ConcurrentNavigableMap.super.forEach(action);
            }

        @Override
        public boolean remove(Object oKey, Object oValue)
            {
            return m_map.remove(oKey == null ? NULL : oKey,
                                oValue == null ? NULL : oValue);
            }

        @Override
        public boolean replace(K oKey, V oOldValue, V oNewValue)
            {
            return m_map.replace(oKey == null ? (K) NULL : oKey,
                                 oOldValue == null ? (V) NULL : oOldValue,
                                 oNewValue == null ? (V) NULL : oNewValue);
            }

        @Override
        public V replace(K oKey, V oValue)
            {
            return ensureReturnValue(m_map.replace(oKey == null ? (K) NULL : oKey,
                                                   oValue == null ? (V) NULL : oValue));
            }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public V computeIfAbsent(K oKey, Function<? super K, ? extends V> mappingFunction)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public V computeIfPresent(K oKey, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public V compute(K oKey, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public V merge(K oKey, V oValue, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
            {
            throw new UnsupportedOperationException();
            }

        // ----- data members ------------------------------------------------

        /**
         * Underlying map
         */
        private final ConcurrentNavigableMap<K, V> m_map;
        }

    // ----- inner class: SortedIterator -------------------------------------

    /**
     * Base iterator that supports null values.
     */
    private static class SortedIterator<T>
            implements Iterator<T>
        {
        SortedIterator(Iterator<T> iter)
            {
            m_iter = iter;
            }

        @Override
        public boolean hasNext()
            {
            return m_iter.hasNext();
            }

        @Override
        public T next()
            {
            T oObj = m_iter.next();

            return oObj == NULL ? null : oObj;
            }

        @Override
        public void remove()
            {
            m_iter.remove();
            }

        protected final Iterator<T> m_iter;
        }

    // ----- inner class: EntryIterator --------------------------------------

    /**
     * Entry iterator that supports null values.
     */
    private static class EntryIterator<K,V>
            extends SortedIterator<Entry<K, V>>
        {
        EntryIterator(Iterator<Entry<K, V>> iter)
            {
            super(iter);
            }

        @Override
        public Entry<K, V> next()
            {
            return new NullableEntry<>(m_iter.next());
            }
        }

    // ----- helpers --------------------------------------------------

    /**
     * Ensure that if entry has a {@link #NULL} key or value, return NullableEntry;
     *
     * @param e  an entry
     * @return an entry that can contain a null key or null value
     */
    private static <K, V> Entry<K,V> ensureReturnEntry(Entry<K,V> e)
        {
        return e.getKey() == NULL || e.getValue() == NULL
               ? new NullableEntry<>(e)
               : e;
        }

    /**
     * Ensure if {@code oKey} equals {@link #NULL}, return null;
     * otherwise, return {@code oKey}.
     *
     * @param oKey  key
     * @return Ensure if {@code oKey} equals {@link #NULL}, return null;
     *         otherwise, return {@code oKey}.
     * @param <K> key type
     */
    private static <K> K ensureReturnKey(K oKey)
        {
        return oKey == NULL ? null : oKey;
        }

    /**
     * Ensure if {@code oValue} equals {@link #NULL}, return null;
     * otherwise, return {@code oValue}.
     *
     * @param oValue  value
     *
     * @return Ensure if {@code oValue} equals {@link #NULL}, return null;
     *         otherwise, return {@code oValue}.
     * @param <V> value type
     */
    private static <V> V ensureReturnValue(V oValue)
        {
        return oValue == NULL ? null : oValue;
        }

    // ----- constant -------------------------------------------------

    /**
     * Placeholder for a {@code null} key or value.
     */
    public static final Object NULL = new Null();

    public static class Null {}
    }
