/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.collections.NullableConcurrentMap;

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
import java.util.Collections;
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
 * @deprecated use {@link NullableConcurrentMap} instead.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Deprecated
public class SafeSortedMap
        extends ConcurrentSkipListMap
    {
    // ----- constructors -------------------------------------------------

    /**
     * Construct a new SafeSortedMap using the natural ordering of the
     * Comparable keys in this map.
     */
    public SafeSortedMap()
        {
        this((Comparator) null);
        }

    /**
     * Construct a new SafeSortedMap copying the contents of the specified map.
     *
     * @param that  the map copied
     */
    public SafeSortedMap(SortedMap that)
        {
        this(that.comparator());
        this.putAll(that);
        }

    /**
     * Construct a new SafeSortedMap with the specified Comparator.
     *
     * @param comparator  the comparator used to sort this map
     */
    public SafeSortedMap(Comparator comparator)
        {
        super((Comparator & Serializable)(o1, o2) -> {
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

            return ((Comparable) o1).compareTo(o2);
            });
        }

    // ----- Map interface ------------------------------------------------

    @Override
    public Object get(Object oKey)
        {
        return ensureReturnValue(super.get(oKey == null ? NULL : oKey));
        }

    @Override
    public Object put(Object oKey, Object oValue)
        {
        return ensureReturnValue(super.put(oKey == null ? NULL : oKey,
                                           oValue == null ? NULL : oValue));
        }

    @Override
    public Object remove(Object oKey)
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

        for (Map.Entry entry : ((Map<?, ?>) oMap).entrySet())
            {
            Object oKey   = entry.getKey();
            Object oValue = entry.getValue();
            // support null values
            if (!Objects.equals(oValue, get(oKey)))
                {
                return false;
                }
            }

        return true;
        }

    @Override
    public SafeSortedMap clone()
        {
        return new SafeSortedMap(this);
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
    public Set<Entry> entrySet()
        {
        // too costly to check if any value is NULL as map size grows
        return new EntrySet(super.entrySet());
        }

    @Override
    public NavigableSet keySet()
        {
        // optimization when the map does not contain a null key which would
        // be the first key in the map, so weak consistency is respected
        return super.containsKey(NULL)
               ? new KeySet(super.keySet())
               : super.keySet();
        }

    @Override
    public Collection values()
        {
        return new Values(super.values());
        }

    @Override
    public ConcurrentNavigableMap descendingMap()
        {
        return new SubMap(super.descendingMap());
        }

    @Override
    public NavigableSet descendingKeySet()
        {
        return new KeySet(super.descendingKeySet());
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
    public Entry getEntry(Object oKey)
        {
        oKey = oKey == null ? NULL : oKey;

        ConcurrentNavigableMap mapSub = super.subMap(oKey, true, oKey, true);

        return mapSub.isEmpty() ? null : ensureReturnEntry(mapSub.firstEntry());
        }

    /* ------ SortedMap API methods ------ */

    @Override
    public Object firstKey()
        {
        return ensureReturnKey(super.firstKey());
        }

    @Override
    public Object lastKey()
        {
        return ensureReturnKey(super.lastKey());
        }

    @Override
    public ConcurrentNavigableMap subMap(Object oFromKey,
                                                         boolean fFromInclusive,
                                                         Object oToKey,
                                                         boolean fToInclusive)
        {
        return new SubMap(super.subMap(oFromKey == null ? NULL : oFromKey, fFromInclusive,
                                         oToKey == null ? NULL : oToKey, fToInclusive));
        }

   @Override
    public ConcurrentNavigableMap headMap(Object oToKey, boolean fInclusive)
        {
        return new SubMap(super.headMap(oToKey == null ? NULL : oToKey, fInclusive));
        }

    @Override
    public ConcurrentNavigableMap tailMap(Object oFromKey, boolean fInclusive)
        {
        return new SubMap(super.tailMap(oFromKey == null ? NULL : oFromKey, fInclusive));
        }

    @Override
    public ConcurrentNavigableMap subMap(Object oFromKey, Object oToKey)
        {
        return subMap(oFromKey, true, oToKey, false);
        }

    @Override
    public ConcurrentNavigableMap headMap(Object oToKey)
        {
        return headMap(oToKey, false);
        }

    @Override
    public ConcurrentNavigableMap tailMap(Object fromKey)
        {
        return tailMap(fromKey, true);
        }

    //----- ConcurrentMap methods ----------------------------------------

    @Override
    public Object putIfAbsent(Object oKey, Object oValue)
        {
        return ensureReturnValue(super.putIfAbsent(oKey == null ? NULL : oKey,
                                                   oValue == null ? NULL : oValue));
        }

    @Override
    public Object getOrDefault(Object oKey, Object oDefaultValue)
        {
        return ensureReturnValue(super.getOrDefault(oKey == null ? NULL : oKey,
                                                    oDefaultValue == null ? NULL : oDefaultValue));
        }

    @Override
    public void forEach(BiConsumer action)
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
    public boolean replace(Object oKey, Object oOldValue, Object oNewValue)
        {
        return super.replace(oKey == null ? NULL : oKey,
                             oOldValue == null ? NULL : oOldValue,
                             oNewValue == null ? NULL : oNewValue);
        }

    @Override
    public Object replace(Object oKey, Object oValue)
        {
        return ensureReturnValue(super.replace(oKey == null ? NULL : oKey,
                                               oValue == null ? NULL : oValue));
        }

    @Override
    public void replaceAll(BiFunction function)
        {
        super.replaceAll(function);
        }

    @Override
    public Object computeIfAbsent(Object oKey, Function mappingFunction)
        {
        return ensureReturnValue(super.computeIfAbsent(oKey == null ? NULL : oKey, mappingFunction));
        }

    @Override
    public Object computeIfPresent(Object oKey, BiFunction remappingFunction)
        {
        return ensureReturnValue(super.computeIfPresent(oKey == null ? NULL : oKey, remappingFunction));
        }

    @Override
    public Object compute(Object oKey, BiFunction remappingFunction)
        {
        return ensureReturnValue(super.compute(oKey == null ? NULL : oKey, remappingFunction));
        }

    @Override
    public Object merge(Object oKey, Object oValue, BiFunction remappingFunction)
        {
        return ensureReturnValue(super.merge(oKey == null ? NULL : oKey,
                                             oValue == null ? NULL : oValue,
                                             remappingFunction));
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
    protected static class NullableEntry
        implements Entry
        {
        // ----- constructor ----------------------------------------------

        /**
         * Constructor for a {@code NullableEntry} wrapping the original entry.
         *
         * @param entry  delegated to entry
         */
        NullableEntry(Entry entry)
            {
            m_entry = entry;
            }

        // ----- Map.Entry interface--------------------------------------

        @Override
        public Object getKey()
            {
            return m_entry == null
                   ? null
                   : ensureReturnKey(m_entry.getKey());
            }

        @Override
        public Object getValue()
            {
            return m_entry == null
                   ? null
                   : ensureReturnValue(m_entry.getValue());
            }

        @Override
        public Object setValue(Object oValue)
            {
            if (m_entry == null)
                {
                throw new NullPointerException();
                }

            return m_entry.setValue(oValue == null ? NULL : oValue);
            }

        // ----- data member ---------------------------------------------

        /**
         * The delegated to entry.
         */
        private final Entry m_entry;
        }

    // ----- inner class: EntrySet ----------------------------------------

    /**
     * Entry set delegation of the super map implementation.
     */
    protected static class EntrySet
            extends AbstractSet<Entry>
        {
        // ----- constructor ----------------------------------------------

        /**
         * Constructor taking the delegated entry set.
         *
         * @param s  delegated to entry set
         */
        EntrySet(Set<Entry> s)
            {
            m_set = s;
            }

        @Override
        public int size()
            {
            return m_set.size();
            }

        @Override
        public Iterator<Entry> iterator()
            {
            return new EntryIterator(m_set.iterator());
            }


        // ----- data member ---------------------------------------------

        /**
         * The delegated to set.
         */
        private final Set<Entry> m_set;
        }

    // ----- inner class: KeySet ----------------------------------------

    /**
     * Key set delegation of the super map implementation.
     */
    protected static class KeySet
            extends AbstractSet
            implements NavigableSet
        {
        // ----- constructor ---------------------------------------------

        /**
         * Constructor taking the delegated key set.
         *
         * @param s  delegated to key set
         */
        KeySet(NavigableSet s)
            {
            m_set = s;
            }

        @Override
        public int size()
            {
            return m_set.size();
            }

        @Override
        public Object lower(Object e)
            {
            return ensureReturnKey(m_set.lower(e));
            }

        @Override
        public Object floor(Object e)
            {
            return ensureReturnKey(m_set.floor(e));
            }

        @Override
        public Object ceiling(Object e)
            {
            return ensureReturnKey(m_set.ceiling(e));
            }

        @Override
        public Object higher(Object e)
            {
            return ensureReturnKey(m_set.higher(e));
            }

        @Override
        public Object pollFirst()
            {
            return ensureReturnKey(m_set.pollFirst());
            }

        @Override
        public Object pollLast()
            {
            return ensureReturnKey(m_set.pollLast());
            }

        @Override
        public NavigableSet descendingSet()
            {
            return new KeySet(m_set.descendingSet());
            }

        @Override
        public Iterator descendingIterator()
            {
            return new SortedIterator(m_set.descendingIterator());
            }

        @Override
        public NavigableSet subSet(Object oFromElement, Object oToElement)
            {
            return subSet(oFromElement, true, oToElement, false);
            }

        @Override
        public NavigableSet subSet(Object oFromElement, boolean fFromInclusive, Object oToElement, boolean fToInclusive)
            {
            return new KeySet(m_set.subSet(oFromElement == null ? NULL : oFromElement, fFromInclusive, 
                                           oToElement == null ? NULL : oToElement, fToInclusive));
            }

        @Override
        public NavigableSet headSet(Object oToElement, boolean fInclusive)
            {
            return new KeySet(m_set.headSet(oToElement == null ? NULL : oToElement, fInclusive));
            }

        @Override
        public NavigableSet headSet(Object oToElement)
            {
            return headSet(oToElement, false);
            }

        @Override
        public NavigableSet tailSet(Object oFromElement, boolean fInclusive)
            {
            return new KeySet(m_set.tailSet(oFromElement == null ? NULL : oFromElement, fInclusive));
            }

        @Override
        public NavigableSet tailSet(Object oFromElement)
            {
            return tailSet(oFromElement == null ? NULL : oFromElement, true);
            }

        @Override
        public Comparator<? super Object> comparator()
            {
            return m_set.comparator();
            }

        @Override
        public Object first()
            {
            return ensureReturnKey(m_set.first());
            }

        @Override
        public Object last()
            {
            return ensureReturnKey(m_set.last());
            }

        @Override
        public Iterator iterator()
            {
            return new SortedIterator<>(m_set.iterator());
            }

        // ----- data member ---------------------------------------------

        /**
         * The delegated to set.
         */
        final NavigableSet m_set;
        }

    // ----- inner class: Values -----------------------------------------

    /**
     * Values delegation of the super map implementation.
     */
    protected static class Values extends AbstractCollection
        {
        Values(Collection colValues)
            {
            m_colValues = colValues;
            }

        @Override
        public Iterator iterator()
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

        private List toList()
            {
            ArrayList list = new ArrayList<>(m_colValues.size());
            for (Object v : m_colValues)
                {
                list.add(ensureReturnValue(v));
                }
            return list;
            }

        public Spliterator spliterator()
            {
            return (Spliterator) iterator();
            }

        private final Collection m_colValues;
        }

    /**
     * SubMap delegation to manage {@link #NULL} in entry key and/or value.
     */
    @SuppressWarnings("NullableProblems")
    protected static class SubMap
            extends AbstractMap
            implements ConcurrentNavigableMap, Cloneable, Serializable
        {
        /**
         * Create a submap wrapper
         */
        SubMap(ConcurrentNavigableMap oMap)
            {
            m_map = oMap;
            }

        // ----- Clone methods -----------------------------------------------

        @Override
        public SubMap clone()
            {
            return new SubMap(this);
            }

        // -----  Map methods ------------------------------------------------

        @Override
        public boolean containsKey(Object oKey)
            {
            return m_map.containsKey(oKey == null ? NULL : oKey);
            }

        @Override
        public Object get(Object oKey)
            {
            return ensureReturnValue(m_map.get(oKey == null ? NULL : oKey));
            }

        @Override
        public Object put(Object oKey, Object oValue)
            {
            return ensureReturnValue(m_map.put(oKey == null ? NULL : oKey,
                                               oValue == null ? NULL : oValue));
            }

        @Override
        public Object remove(Object oKey)
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
        public ConcurrentNavigableMap subMap(Object oFromKey, boolean fFromInclusive,
                                             Object oToKey, boolean fToInclusive)
            {
            return new SubMap(m_map.subMap(oFromKey == null ? NULL : oFromKey, fFromInclusive,
                                             oToKey == null ? NULL : oToKey, fToInclusive));
            }

        @Override
        public ConcurrentNavigableMap headMap(Object oToKey, boolean fInclusive)
            {
            return new SubMap(m_map.subMap(NULL, false,
                                             oToKey == null ? NULL : oToKey, fInclusive));
            }

        @Override
        public ConcurrentNavigableMap tailMap(Object oFromKey, boolean fInclusive)
            {
            return new SubMap(m_map.tailMap(oFromKey == null ? NULL : oFromKey, fInclusive));
            }

        @Override
        public Comparator comparator()
            {
            return m_map.comparator();
            }

        @Override
        public ConcurrentNavigableMap subMap(Object oFromKey, Object oToKey)
            {
            return subMap(oFromKey,true, oToKey, false);
            }

        @Override
        public ConcurrentNavigableMap headMap(Object oToKey)
            {
            return headMap(oToKey, false);
            }

        @Override
        public ConcurrentNavigableMap tailMap(Object oFromKey)
            {
            return tailMap(oFromKey, true);
            }

        @Override
        public Object firstKey()
            {
            return ensureReturnKey(m_map.firstKey());
            }

        @Override
        public Object lastKey()
            {
            return ensureReturnKey(m_map.lastKey());
            }

        @Override
        public Entry lowerEntry(Object oKey)
            {
            return ensureReturnEntry(m_map.lowerEntry(oKey == null ? NULL : oKey));
            }

        @Override
        public Object lowerKey(Object oKey)
            {
            return ensureReturnKey(m_map.lowerKey(oKey == null ? NULL : oKey));
            }

        @Override
        public Entry floorEntry(Object oKey)
            {
            return ensureReturnEntry(m_map.floorEntry(oKey == null ? NULL : oKey));
            }

        @Override
        public Object floorKey(Object oKey)
            {
            return ensureReturnKey(m_map.floorKey(oKey == null ? NULL : oKey));
            }

        @Override
        public Entry ceilingEntry(Object oKey)
            {
            return ensureReturnEntry(m_map.ceilingEntry(oKey == null ? NULL : oKey));
            }

        @Override
        public Object ceilingKey(Object oKey)
            {
            return ensureReturnValue(m_map.ceilingKey(oKey == null ? NULL : oKey));
            }

        @Override
        public Entry higherEntry(Object oKey)
            {
            return ensureReturnEntry(m_map.higherEntry(oKey == null ? NULL : oKey));
            }

        @Override
        public Object higherKey(Object oKey)
            {
            return ensureReturnKey(m_map.higherKey(oKey == null ? NULL : oKey));
            }

        @Override
        public Entry firstEntry()
            {
            return ensureReturnEntry(m_map.firstEntry());
            }

        @Override
        public Entry lastEntry()
            {
            return ensureReturnEntry(m_map.lastEntry());
            }

        @Override
        public Entry pollFirstEntry()
            {
            return ensureReturnEntry(m_map.pollFirstEntry());
            }

        @Override
        public Entry pollLastEntry()
            {
            return ensureReturnEntry(m_map.pollLastEntry());
            }

        @Override
        public ConcurrentNavigableMap descendingMap()
            {
            return new SubMap(m_map.descendingMap());
            }

       //----- Submap Views --------------------------------------------------

        @Override
        public NavigableSet keySet()
            {
            return new KeySet(m_map.keySet());
            }

        @Override
        public NavigableSet navigableKeySet()
            {
            return keySet();
            }

        @Override
        public Collection values()
            {
            return new Values(m_map.values());
            }

        @Override
        public Set<Map.Entry> entrySet()
            {
            return new EntrySet(m_map.entrySet());
            }

        @Override
        public NavigableSet descendingKeySet()
            {
            return descendingMap().navigableKeySet();
            }

        //----- ConcurrentMap methods ----------------------------------------

        @Override
        public Object putIfAbsent(Object oKey, Object oValue)
            {
            return ensureReturnValue(m_map.putIfAbsent(oKey == null ? NULL : oKey,
                                                       oValue == null ? NULL : oValue));
            }

        @Override
        public Object getOrDefault(Object oKey, Object oDefaultValue)
            {
            return ensureReturnValue(m_map.getOrDefault(oKey == null ? NULL : oKey,
                                                        oDefaultValue == null ? NULL : oDefaultValue));
            }

        @Override
        public void forEach(BiConsumer action)
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
        public boolean replace(Object oKey, Object oOldValue, Object oNewValue)
            {
            return m_map.replace(oKey == null ? NULL : oKey,
                                 oOldValue == null ? NULL : oOldValue,
                                 oNewValue == null ? NULL : oNewValue);
            }

        @Override
        public Object replace(Object oKey, Object oValue)
            {
            return ensureReturnValue(m_map.replace(oKey == null ? NULL : oKey,
                                                   oValue == null ? NULL : oValue));
            }

        @Override
        public void replaceAll(BiFunction function)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Object computeIfAbsent(Object oKey, Function mappingFunction)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Object computeIfPresent(Object oKey, BiFunction remappingFunction)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Object compute(Object oKey, BiFunction remappingFunction)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Object merge(Object oKey, Object oValue, BiFunction remappingFunction)
            {
            throw new UnsupportedOperationException();
            }

        // ----- data members ------------------------------------------------

        /**
         * Underlying map
         */
        private final ConcurrentNavigableMap m_map;
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
    private static class EntryIterator
            extends SortedIterator<Entry>
        {
        EntryIterator(Iterator<Entry> iter)
            {
            super(iter);
            }

        @Override
        public Entry next()
            {
            return new NullableEntry(m_iter.next());
            }
        }

    // ----- helpers --------------------------------------------------

    /**
     * Ensure that if entry has a {@link #NULL} key or value, return NullableEntry;
     *
     * @param e  an entry
     * @return an entry that can contain a null key or null value
     */
    private static Entry ensureReturnEntry(Entry e)
        {
        return e.getKey() == NULL || e.getValue() == NULL
               ? new NullableEntry(e)
               : e;
        }

    /**
     * Ensure if {@code oKey} equals {@link #NULL}, return null;
     * otherwise, return {@code oKey}.
     *
     * @param oKey  key
     * @return Ensure if {@code oKey} equals {@link #NULL}, return null;
     *         otherwise, return {@code oKey}.
     */
    private static Object ensureReturnKey(Object oKey)
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
     */
    private static Object ensureReturnValue(Object oValue)
        {
        return oValue == NULL ? null : oValue;
        }

    // ----- constant -------------------------------------------------

    /**
     * An empty, immutable SafeSortedMap instance.
     */
    public static final SortedMap<?, ?> EMPTY = Collections.unmodifiableSortedMap(new SafeSortedMap());

    /**
     * Placeholder for a {@code null} key or value.
     */
    public static final Object NULL = new Null();

    public static class Null {}
    }
