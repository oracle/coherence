/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.util.comparator.SafeComparator;

import java.io.Serializable;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;

import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Implementation of a {@link java.util.SortedMap} extending {@link java.util.concurrent.ConcurrentSkipListMap}
 * to support null keys and null values.
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
        V oValue = super.get(oKey == null ? NULL : oKey);

        return oValue == NULL ? null : oValue;
        }

    @Override
    public V put(K oKey, V oValue)
        {
        return super.put(oKey == null ? (K) NULL : oKey, oValue == null ? (V) NULL : oValue);
        }

    @Override
    public V remove(Object oKey)
        {
        return super.remove(oKey == null ? (K) NULL : oKey);
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

        Iterator<Map.Entry<K, V>> it = ((Map<K, V>) oMap).entrySet().iterator();

        while (it.hasNext())
            {
            Map.Entry<K, V> entry  = it.next();
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
        return new SafeSortedMap<K, V>(this);
        }

    @Override
    public Set<Entry<K, V>> entrySet()
        {
        // optimization when the map does not contain any null value
        return this.containsValue(NULL)
                        ? new EntrySet(super.entrySet())
                        : super.entrySet();
        }

    @Override
    public NavigableSet<K> keySet()
        {
        // optimization when the map does not contain a null key which would
        // be the first key in the map, so weak consistency is respected
        return this.containsKey(NULL)
                      ? new KeySet(super.keySet())
                      : super.keySet();
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
        // call get??

        for (Entry<K, V> e : entrySet())
            {
            if ((e.getKey().equals(oKey == null ? NULL : oKey)))
                {
                if (e.getValue() == NULL)
                    {
                    return new NullableEntry<>(e);
                    }
                return e;
                }
            }

        return null;
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
            return m_entry == null ?
                   null :
                   m_entry.getKey() == NULL ? null : m_entry.getKey();
            }

        @Override
        public V getValue()
            {
            return m_entry == null ?
                   null :
                   m_entry.getValue() == NULL ? null : m_entry.getValue();
            }

        @Override
        public V setValue(V oValue)
            {
            if (m_entry == null)
                {
                throw new NullPointerException();
                }

            return m_entry.setValue(oValue);
            }

        // ----- data member ---------------------------------------------

        /**
         * The delegated to entry.
         */
        private final transient Entry<K, V> m_entry;
        }

    // ----- inner class: EntrySet ----------------------------------------

    /**
     * Entry set delegation of the super map implementation.
     */
    protected class EntrySet
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
            return new EntryIterator(m_set.iterator());
            }


        // ----- data member ---------------------------------------------

        /**
         * The delegated to set.
         */
        private final transient Set<Entry<K, V>> m_set;
        }

    // ----- inner class: KeySet ----------------------------------------

    /**
     * Key set delegation of the super map implementation.
     */
    protected class KeySet
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
            return m_set.lower(e);
            }

        @Override
        public K floor(K e)
            {
            return m_set.floor(e);
            }

        @Override
        public K ceiling(K e)
            {
            return m_set.ceiling(e);
            }

        @Override
        public K higher(K e)
            {
            return m_set.higher(e);
            }

        @Override
        public K pollFirst()
            {
            return m_set.pollFirst();
            }

        @Override
        public K pollLast()
            {
            return m_set.pollLast();
            }

        @Override
        public NavigableSet<K> descendingSet()
            {
            return m_set.descendingSet();
            }

        @Override
        public Iterator<K> descendingIterator()
            {
            return m_set.descendingIterator();
            }

        @Override
        public NavigableSet<K> subSet(K fromElement, K toElement)
            {
            return (NavigableSet<K>) m_set.subSet(fromElement, toElement);
            }

        @Override
        public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive)
            {
            return m_set.subSet(fromElement, fromInclusive, toElement, toInclusive);
            }

        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive)
            {
            return m_set.headSet(toElement, inclusive);
            }

        @Override
        public NavigableSet<K> headSet(K toElement)
            {
            return m_set.headSet(toElement, false);
            }

        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive)
            {
            return m_set.tailSet(fromElement, inclusive);
            }

        @Override
        public NavigableSet<K> tailSet(K fromElement)
            {
            return m_set.tailSet(fromElement, true);
            }

        @Override
        public Comparator<? super K> comparator()
            {
            return m_set.comparator();
            }

        @Override
        public K first()
            {
            return m_set.first();
            }

        @Override
        public K last()
            {
            return m_set.last();
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
        final transient NavigableSet<K> m_set;
        }

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

        protected final transient Iterator<T> m_iter;
        }

    /**
     * Entry iterator that supports null values.
     */
    private class EntryIterator
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

    // ----- constant -------------------------------------------------

    /**
     * Placeholder for a {@code null} key or value.
     */
    protected static NullSerializable NULL = new NullSerializable();

    static class NullSerializable
            implements Serializable
        {
        protected Object readResolve()
            {
            return NULL;
            }
        }
    }
