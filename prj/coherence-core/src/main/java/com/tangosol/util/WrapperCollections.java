/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import javax.json.bind.annotation.JsonbProperty;

import java.io.Serializable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import java.util.stream.Stream;

/**
* A collection of abstract Collection implementation classes for wrapping
* Collection types.
*
* @author mf  2007.07.05
* @author rhl 2010.01.26
*/
public class WrapperCollections
    {
    // ----- inner class: AbstractWrapperIterator ---------------------------

    /**
    * Iterator implementation which delegates all calls to another Iterator.
    */
    public static abstract class AbstractWrapperIterator<E>
            implements Iterator<E>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create an AbstractWrapperIterator which delegates to the specified
        * Iterator.
        *
        * @param iter  the Iterator to delegate all calls to
        */
        protected AbstractWrapperIterator(Iterator<E> iter)
            {
            m_iterDelegate = iter;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Iterator to which all operations should be delegated to.
        *
        * @return the wrapped Iterator
        */
        protected Iterator<E> getDelegate()
            {
            return m_iterDelegate;
            }

        // ----- Iterator interface ---------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean hasNext()
            {
            return getDelegate().hasNext();
            }

        /**
        * {@inheritDoc}
        */
        public E next()
            {
            return getDelegate().next();
            }

        /**
        * {@inheritDoc}
        */
        public void remove()
            {
            getDelegate().remove();
            }

        // ----- Object methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            return getDelegate().equals(o);
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return getDelegate().hashCode();
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return getDelegate().toString();
            }

        // ----- data members ---------------------------------------------

        /**
        * The Iterator to which this wrapper delegates.
        */
        private final Iterator<E> m_iterDelegate;
        }


    // ----- inner class: AbstractWrapperListIterator -----------------------

    /**
    * ListIterator implementation which delegates all calls to another
    * ListIterator.
    */
    public abstract class AbstractWrapperListIterator<E>
            extends AbstractWrapperIterator<E>
            implements ListIterator<E>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create an AbstractWrapperListIterator which delegates to the specified
        * ListIterator.
        *
        * @param iter  the ListIterator to delegate all calls to
        */
        protected AbstractWrapperListIterator(ListIterator<E> iter)
            {
            super(iter);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Iterator to which all operations should be delegated to.
        *
        * @return the wrapped Iterator
        */
        protected ListIterator<E> getDelegate()
            {
            return (ListIterator<E>) super.getDelegate();
            }

        // ----- ListIterator interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public void add(E o)
            {
            getDelegate().add(o);
            }

        /**
        * {@inheritDoc}
        */
        public boolean hasPrevious()
            {
            return getDelegate().hasPrevious();
            }

        /**
        * {@inheritDoc}
        */
        public int nextIndex()
            {
            return getDelegate().nextIndex();
            }

        /**
        * {@inheritDoc}
        */
        public E previous()
            {
            return getDelegate().previous();
            }

        /**
        * {@inheritDoc}
        */
        public int previousIndex()
            {
            return getDelegate().previousIndex();
            }

        /**
        * {@inheritDoc}
        */
        public void set(E o)
            {
            getDelegate().set(o);
            }
        }


    // ----- inner class: AbstractWrapperCollection -------------------------

    /**
    * Collection implementation which delegates all calls to another Collection.
    */
    public static abstract class AbstractWrapperCollection<E>
            implements Collection<E>, Serializable
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor
        */
        protected AbstractWrapperCollection()
            {
            }

        /**
        * Create an AbstractWrapperCollection which delegates to the specified
        * Collection.
        *
        * @param col  the Collection to delegate all calls to
        */
        protected AbstractWrapperCollection(Collection<E> col)
            {
            m_colDelegate = col;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Collection to which all operations should be delegated to.
        *
        * @return the wrapped Collection
        */
        protected Collection<E> getDelegate()
            {
            return m_colDelegate;
            }

        // ----- Collection interface -------------------------------------

        /**
        * {@inheritDoc}
        */
        public int size()
            {
            return getDelegate().size();
            }

        /**
        * {@inheritDoc}
        */
        public boolean isEmpty()
            {
            return getDelegate().isEmpty();
            }

        /**
        * {@inheritDoc}
        */
        public boolean contains(Object o)
            {
            return getDelegate().contains(o);
            }

        /**
        * {@inheritDoc}
        */
        public Iterator<E> iterator()
            {
            return getDelegate().iterator();
            }

        /**
        * {@inheritDoc}
        */
        public Object[] toArray()
            {
            return getDelegate().toArray();
            }

        /**
        * {@inheritDoc}
        */
        public <T> T[] toArray(T[] a)
            {
            return getDelegate().toArray(a);
            }

        /**
        * {@inheritDoc}
        */
        public boolean add(E o)
            {
            return getDelegate().add(o);
            }

        /**
        * {@inheritDoc}
        */
        public boolean remove(Object o)
            {
            return getDelegate().remove(o);
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsAll(Collection<?> c)
            {
            return getDelegate().containsAll(c);
            }

        /**
        * {@inheritDoc}
        */
        public boolean addAll(Collection<? extends E> c)
            {
            return getDelegate().addAll(c);
            }

        /**
        * {@inheritDoc}
        */
        public boolean retainAll(Collection<?> c)
            {
            return getDelegate().retainAll(c);
            }

        /**
        * {@inheritDoc}
        */
        public boolean removeAll(Collection<?> c)
            {
            return getDelegate().removeAll(c);
            }

        /**
        * {@inheritDoc}
        */
        public void clear()
            {
            getDelegate().clear();
            }

        /**
        * {@inheritDoc}
        */
        public Spliterator<E> spliterator()
            {
            return getDelegate().spliterator();
            }

        /**
        * {@inheritDoc}
        */
        public boolean removeIf(Predicate<? super E> filter)
            {
            return getDelegate().removeIf(filter);
            }

        /**
        * {@inheritDoc}
        */
        public Stream<E> stream()
            {
            return getDelegate().stream();
            }

        /**
        * {@inheritDoc}
        */
        public Stream<E> parallelStream()
            {
            return getDelegate().parallelStream();
            }

        // ---- Iterable implementation -----------------------------------

        public void forEach(Consumer<? super E> action)
            {
            getDelegate().forEach(action);
            }

        // ----- Object methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            return getDelegate().equals(o);
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return getDelegate().hashCode();
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return getDelegate().toString();
            }

        // ----- data members ---------------------------------------------

        /**
        * The Collection to which this wrapper delegates.
        */
        @JsonbProperty("valuesDelegate")
        protected Collection<E> m_colDelegate;
        }


    // ----- inner class: AbstractWrapperSet --------------------------------

    /**
    * Set implementation which delegates all calls to another Set.
    */
    public static abstract class AbstractWrapperSet<E>
            extends AbstractWrapperCollection<E>
            implements Set<E>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        protected AbstractWrapperSet()
            {
            }

        /**
        * Create an AbstractWrapperSet which delegates to the specified Set.
        *
        * @param set  the Set to delegate all calls to
        */
        protected AbstractWrapperSet(Set<E> set)
            {
            super(set);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Set to which all operations should be delegated to.
        *
        * @return the wrapped Set
        */
        protected Set<E> getDelegate()
            {
            return (Set<E>) super.getDelegate();
            }
        }


    // ----- inner class: AbstractWrapperList -----------------------------

    /**
    * List implementation which delegates all calls to another List.
    */
    public static abstract class AbstractWrapperList<E>
            extends AbstractWrapperCollection<E>
            implements List<E>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        protected AbstractWrapperList()
            {
            }

        /**
        * Create an AbstractWrapperList which delegates to the specified List.
        *
        * @param list  the List to delegate all calls to
        */
        protected AbstractWrapperList(List<E> list)
            {
            super(list);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the List to which all operations should be delegated to.
        *
        * @return the wrapped List
        */
        protected List<E> getDelegate()
            {
            return (List<E>) super.getDelegate();
            }

        // ----- List interface -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean addAll(int index, Collection<? extends E> c)
            {
            return getDelegate().addAll(index, c);
            }

        /**
        * {@inheritDoc}
        */
        public E get(int index)
            {
            return getDelegate().get(index);
            }

        /**
        * {@inheritDoc}
        */
        public E set(int index, E element)
            {
            return getDelegate().set(index, element);
            }

        /**
        * {@inheritDoc}
        */
        public void add(int index, E element)
            {
            getDelegate().add(index, element);
            }

        /**
        * {@inheritDoc}
        */
        public E remove(int index)
            {
            return getDelegate().remove(index);
            }

        /**
        * {@inheritDoc}
        */
        public int indexOf(Object o)
            {
            return getDelegate().indexOf(o);
            }

        /**
        * {@inheritDoc}
        */
        public int lastIndexOf(Object o)
            {
            return getDelegate().lastIndexOf(o);
            }

        /**
        * {@inheritDoc}
        */
        public ListIterator<E> listIterator()
            {
            return getDelegate().listIterator();
            }

        /**
        * {@inheritDoc}
        */
        public ListIterator<E> listIterator(int index)
            {
            return getDelegate().listIterator(index);
            }

        /**
        * {@inheritDoc}
        */
        public void replaceAll(UnaryOperator<E> operator)
            {
            getDelegate().replaceAll(operator);
            }

        /**
        * {@inheritDoc}
        */
        public void sort(Comparator<? super E> c)
            {
            getDelegate().sort(c);
            }

        /**
        * {@inheritDoc}
        */
        public List<E> subList(int fromIndex, int toIndex)
            {
            return getDelegate().subList(fromIndex, toIndex);
            }
        }


    // ----- inner class: AbstractWrapperMap --------------------------------

    /**
    * Map implementation which delegates all calls to another Map.
    */
    public static abstract class AbstractWrapperMap<K, V>
            implements Map<K, V>, Serializable
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor
        */
        protected AbstractWrapperMap()
            {
            }

        /**
        * Create an AbstractWrapperMap which delegates to the specified Map.
        *
        * @param map  the Map to delegate all calls to
        */
        protected AbstractWrapperMap(Map<K, V> map)
            {
            m_mapDelegate = map;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Map to which all operations should be delegated to.
        *
        * @return the wrapped Map
        */
        protected Map<K, V> getDelegate()
            {
            return m_mapDelegate;
            }

        // ----- Map interface --------------------------------------------

        /**
        * {@inheritDoc}
        */
        public void clear()
            {
            getDelegate().clear();
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsKey(Object oKey)
            {
            return getDelegate().containsKey(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsValue(Object oValue)
            {
            return getDelegate().containsValue(oValue);
            }

        /**
        * {@inheritDoc}
        */
        public Set<Map.Entry<K, V>> entrySet()
            {
            return getDelegate().entrySet();
            }

        /**
        * {@inheritDoc}
        */
        public V get(Object oKey)
            {
            return getDelegate().get(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public boolean isEmpty()
            {
            return getDelegate().isEmpty();
            }

        /**
        * {@inheritDoc}
        */
        public Set<K> keySet()
            {
            return getDelegate().keySet();
            }

        /**
        * {@inheritDoc}
        */
        public V put(K oKey, V oValue)
            {
            return getDelegate().put(oKey, oValue);
            }

        /**
        * {@inheritDoc}
        */
        public void putAll(Map<? extends K, ? extends V> map)
            {
            getDelegate().putAll(map);
            }

        /**
        * {@inheritDoc}
        */
        public V remove(Object oKey)
            {
            return getDelegate().remove(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public int size()
            {
            return getDelegate().size();
            }

        /**
        * {@inheritDoc}
        */
        public Collection<V> values()
            {
            return getDelegate().values();
            }

        /**
        * {@inheritDoc}
        */
        public V getOrDefault(Object key, V defaultValue)
            {
            return getDelegate().getOrDefault(key, defaultValue);
            }

        /**
        * {@inheritDoc}
        */
        public void forEach(BiConsumer<? super K, ? super V> action)
            {
            getDelegate().forEach(action);
            }

        /**
        * {@inheritDoc}
        */
        public boolean replace(K key, V oldValue, V newValue)
            {
            return getDelegate().replace(key, oldValue, newValue);
            }

        /**
        * {@inheritDoc}
        */
        public V replace(K key, V value)
            {
            return getDelegate().replace(key, value);
            }

        /**
        * {@inheritDoc}
        */
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
            {
            getDelegate().replaceAll(function);
            }

        /**
        * {@inheritDoc}
        */
        public V putIfAbsent(K key, V value)
            {
            return getDelegate().putIfAbsent(key, value);
            }

        /**
        * {@inheritDoc}
        */
        public boolean remove(Object key, Object value)
            {
            return getDelegate().remove(key, value);
            }

        /**
        * {@inheritDoc}
        */
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
            {
            return getDelegate().computeIfAbsent(key, mappingFunction);
            }

        /**
        * {@inheritDoc}
        */
        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
            {
            return getDelegate().computeIfPresent(key, remappingFunction);
            }

        /**
        * {@inheritDoc}
        */
        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
            {
            return getDelegate().compute(key, remappingFunction);
            }

        /**
        * {@inheritDoc}
        */
        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
            {
            return getDelegate().merge(key, value, remappingFunction);
            }

        // ----- Object methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object oOther)
            {
            return getDelegate().equals(oOther);
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return getDelegate().hashCode();
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return getDelegate().toString();
            }

        // ----- data members ---------------------------------------------

        /**
        * The Map to which this wrapper delegates.
        */
        @JsonbProperty("entriesDelegate")
        protected Map<K, V> m_mapDelegate;
        }


    // ----- inner class: AbstractWrapperEntry ------------------------------

    /**
    * Map.Entry implementation which delegates all calls to another Map.Entry.
    */
    public static abstract class AbstractWrapperEntry<K, V>
            implements Entry<K, V>, Serializable
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create an AbstractWrapperEntry which delegates to the specified Entry.
        *
        * @param entry  the Entry to delegate all calls to
        */
        protected AbstractWrapperEntry(Entry<K, V> entry)
            {
            m_entryDelegate = entry;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Map to which all operations should be delegated to.
        *
        * @return the wrapped Map
        */
        protected Entry<K, V> getDelegate()
            {
            return m_entryDelegate;
            }

        // ----- Entry interface ------------------------------------------

        /**
        * {@inheritDoc}
        */
        public K getKey()
            {
            return getDelegate().getKey();
            }

        /**
        * {@inheritDoc}
        */
        public V getValue()
            {
            return getDelegate().getValue();
            }

        /**
        * {@inheritDoc}
        */
        public V setValue(V oValue)
            {
            return getDelegate().setValue(oValue);
            }

        // ----- Object methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            return getDelegate().equals(o);
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return getDelegate().hashCode();
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return getDelegate().toString();
            }

        // ----- data members ---------------------------------------------

        /**
        * The Entry to which this wrapper delegates.
        */
        private final Entry<K, V> m_entryDelegate;
        }


    // ----- inner class: AbstractWrapperSortedSet ------------------------

    /**
    * SortedSet implementation which delegates all calls to another SortedSet.
    */
    public static abstract class AbstractWrapperSortedSet<E>
            extends AbstractWrapperSet<E>
            implements SortedSet<E>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor
        */
        protected AbstractWrapperSortedSet()
            {
            }

        /**
        * Create an AbstractWrapperSortedSet which delegates to the specified
        * SortedSet.
        *
        * @param set  the SortedSet to delegate all calls to
        */
        protected AbstractWrapperSortedSet(SortedSet<E> set)
            {
            super(set);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the SortedSet to which all operations should be delegated to.
        */
        protected SortedSet<E> getDelegate()
            {
            return (SortedSet<E>) super.getDelegate();
            }

        // ----- SortedSet interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public Comparator<? super E> comparator()
            {
            return getDelegate().comparator();
            }

        /**
        * {@inheritDoc}
        */
        public SortedSet<E> subSet(E oFrom, E oTo)
            {
            return getDelegate().subSet(oFrom, oTo);
            }

        /**
        * {@inheritDoc}
        */
        public SortedSet<E> headSet(E oTo)
            {
            return getDelegate().headSet(oTo);
            }

        /**
        * {@inheritDoc}
        */
        public SortedSet<E> tailSet(E oFrom)
            {
            return getDelegate().tailSet(oFrom);
            }

        /**
        * {@inheritDoc}
        */
        public E first()
            {
            return getDelegate().first();
            }

        /**
        * {@inheritDoc}
        */
        public E last()
            {
            return getDelegate().last();
            }
        }


    // ----- inner class: AbstractWrapperSortedMap --------------------------

    @SuppressWarnings({"NullableProblems"})
    public class AbstractWrapperSortedMap<K, V>
            extends AbstractWrapperMap<K, V>
            implements SortedMap<K, V>
        {
        // ---- constructors ----------------------------------------------

        /**
         * Construct instance which wraps an instance of a {@link SortedMap}.
         */
        public AbstractWrapperSortedMap()
            {
            }

        /**
        * Create an AbstractWrapperMap which delegates to the specified Map.
        *
        * @param map  the Map to delegate all calls to
        */
        protected AbstractWrapperSortedMap(SortedMap<K, V> map)
            {
            m_mapDelegate = map;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Map to which all operations should be delegated to.
        *
        * @return the wrapped Map
        */
        @Override
        protected SortedMap<K, V> getDelegate()
            {
            return (SortedMap<K, V>) super.getDelegate();
            }

        // ---- SortedMap implementation ----------------------------------

        /**
        * {@inheritDoc}
        */
        public Comparator<? super K> comparator()
            {
            return ((SortedMap<K, V>) getDelegate()).comparator();
            }

        /**
        * {@inheritDoc}
        */
        public SortedMap<K, V> subMap(K fromKey, K toKey)
            {
            return getDelegate().subMap(fromKey, toKey);
            }

        /**
        * {@inheritDoc}
        */
        public SortedMap<K, V> headMap(K toKey)
            {
            return getDelegate().headMap(toKey);
            }

        /**
        * {@inheritDoc}
        */
        public SortedMap<K, V> tailMap(K fromKey)
            {
            return getDelegate().tailMap(fromKey);
            }

        /**
        * {@inheritDoc}
        */
        public K firstKey()
            {
            return getDelegate().firstKey();
            }

        /**
        * {@inheritDoc}
        */
        public K lastKey()
            {
            return getDelegate().lastKey();
            }
        }


    // ----- inner class: ConcurrentWrapperMap ------------------------------

    /**
    * Map implementation which uses a ReadWriteLock to manage concurrent
    * access to an underlying Map.
    *
    * @since Coherence 3.7
    */
    public static class ConcurrentWrapperMap<K, V>
            implements ConcurrentMap<K, V>
        {
        // ----- constructors ---------------------------------------------

        protected ConcurrentWrapperMap()
            {
            ReadWriteLock lock = new ReentrantReadWriteLock();
            m_lock = lock;
            m_lockShared = lock.readLock();
            m_lockExclusive = lock.writeLock();
            m_fStrict = false;
            }

        /**
        * Create an ConcurrentWrapperMap which delegates to the specified Map.
        *
        * @param map  the Map to delegate all calls to
        */
        public ConcurrentWrapperMap(Map<K, V> map)
            {
            this(map, new ReentrantReadWriteLock());
            }

        /**
        * Create an ConcurrentWrapperMap which delegates to the specified Map.
        *
        * @param map   the Map to delegate all calls to
        * @param lock  a read/write lock for concurrency management
        */
        protected ConcurrentWrapperMap(Map<K, V> map, ReadWriteLock lock)
            {
            this(map, lock, false);
            }

        /**
        * Create an ConcurrentWrapperMap which delegates to the specified Map.
        *
        * @param map      the Map to delegate all calls to
        * @param lock     a read/write lock for concurrency management
        * @param fStrict  pass true to protect all Entry methods; false to
        *                 protect only the <tt>setValue()</tt> method
        */
        protected ConcurrentWrapperMap(Map<K, V> map, ReadWriteLock lock, boolean fStrict)
            {
            m_mapDelegate = map;
            m_lock = lock;
            m_lockShared = lock.readLock();
            m_lockExclusive = lock.writeLock();
            m_fStrict = fStrict;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Map to which all operations should be delegated to.
        *
        * @return the wrapped Map
        */
        protected Map<K, V> getDelegate()
            {
            return m_mapDelegate;
            }

        /**
        * Return the ReadWriteLock that is used to manage concurrent access
        * and modifications to the underlying map.
        *
        * @return the ReadWriteLock
        */
        protected ReadWriteLock getLock()
            {
            return m_lock;
            }

        // ----- Map interface --------------------------------------------

        /**
        * {@inheritDoc}
        */
        public void clear()
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                getDelegate().clear();
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsKey(Object oKey)
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().containsKey(oKey);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsValue(Object oValue)
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().containsValue(oValue);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public Set<Map.Entry<K, V>> entrySet()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                Set<Entry<K, V>> set = m_setEntries;
                if (set == null)
                    {
                    m_setEntries = set = new ConcurrentWrapperEntrySet<K, V>(getDelegate().entrySet(), m_lock,
                            m_fStrict);
                    }
                return set;
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public V get(Object oKey)
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().get(oKey);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean isEmpty()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().isEmpty();
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public Set<K> keySet()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                Set<K> set = m_setKeys;
                if (set == null)
                    {
                    m_setKeys = set = new ConcurrentWrapperSet<K>(getDelegate().keySet(), m_lock);
                    }
                return set;
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public V put(K oKey, V oValue)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                return getDelegate().put(oKey, oValue);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void putAll(Map<? extends K, ? extends V> map)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                getDelegate().putAll(map);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public V putIfAbsent(K key, V value)
            {
            return getDelegate().putIfAbsent(key, value);
            }

        /**
        * {@inheritDoc}
        */
        public V remove(Object oKey)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                return getDelegate().remove(oKey);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean remove(Object key, Object value)
            {
            return getDelegate().remove(key, value);
            }

        /**
        * {@inheritDoc}
         */
        public V replace(K key, V value)
            {
            return getDelegate().replace(key, value);
            }

        /**
        * {@inheritDoc}
        */
        public boolean replace(K key, V oldValue, V newValue)
            {
            return getDelegate().replace(key, oldValue, newValue);
            }

        /**
        * {@inheritDoc}
        */
        public int size()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().size();
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public Collection<V> values()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                Collection<V> coll = m_collValues;
                if (coll == null)
                    {
                    m_collValues = coll = new ConcurrentWrapperCollection<V>(getDelegate().values(), m_lock);
                    }
                return coll;
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public V getOrDefault(Object key, V defaultValue)
            {
            return getDelegate().getOrDefault(key, defaultValue);
            }

        /**
        * {@inheritDoc}
        */
        public void forEach(BiConsumer<? super K, ? super V> action)
            {
            getDelegate().forEach(action);
            }

        /**
        * {@inheritDoc}
        */
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
            {
            getDelegate().replaceAll(function);
            }

        /**
        * {@inheritDoc}
        */
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
            {
            return getDelegate().computeIfAbsent(key, mappingFunction);
            }

        /**
        * {@inheritDoc}
        */
        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
            {
            return getDelegate().computeIfPresent(key, remappingFunction);
            }

        /**
        * {@inheritDoc}
        */
        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
            {
            return getDelegate().compute(key, remappingFunction);
            }

        /**
        * {@inheritDoc}
        */
        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
            {
            return getDelegate().merge(key, value, remappingFunction);
            }

        // ----- Object methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object oOther)
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().equals(oOther);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().hashCode();
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().toString();
                }
            finally
                {
                lock.unlock();
                }
            }

        // ----- data members ---------------------------------------------

        /**
        * The Map to which this wrapper delegates.
        */
        protected Map<K, V> m_mapDelegate;

        /**
        * The read/write lock for concurrency control.
        */
        protected final ReadWriteLock m_lock;

        /**
        * The shared lock for performing read operations.
        */
        protected final Lock m_lockShared;

        /**
        * The exclusive lock for performing read/write operations.
        */
        protected final Lock m_lockExclusive;

        /**
        * The key set.
        */
        protected Set<K> m_setKeys;

        /**
        * The entry set.
        */
        protected Set<Entry<K, V>> m_setEntries;

        /**
        * The values collection.
        */
        protected Collection<V> m_collValues;

        /**
        * Determines whether or not all of the Entry methods are protected;
        * if set to true, then all Entry methods are protected.
        */
        protected final boolean m_fStrict;
        }


    // ----- inner class: ConcurrentWrapperCollection -----------------------

    /**
    * Collection implementation which uses a ReadWriteLock to manage
    * concurrent access to an underlying Collection.
    *
    * @since Coherence 3.7
    */
    public static class ConcurrentWrapperCollection<E>
            implements Collection<E>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create an ConcurrentWrapperCollection which delegates to the
        * specified Collection.
        *
        * @param col  the Collection to delegate all calls to
        */
        public ConcurrentWrapperCollection(Collection<E> col)
            {
            this(col, new ReentrantReadWriteLock());
            }

        /**
        * Create an ConcurrentWrapperCollection which delegates to the
        * specified Collection.
        *
        * @param col   the Collection to delegate all calls to
        * @param lock  a read/write lock for concurrency management
        */
        protected ConcurrentWrapperCollection(Collection<E> col, ReadWriteLock lock)
            {
            m_colDelegate = col;
            m_lock = lock;
            m_lockShared = lock.readLock();
            m_lockExclusive = lock.writeLock();
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Collection to which all operations should be delegated to.
        *
        * @return the wrapped Collection
        */
        protected Collection<E> getDelegate()
            {
            return m_colDelegate;
            }

        /**
        * Return the ReadWriteLock that is used to manage concurrent access
        * and modifications to the underlying map.
        *
        * @return the ReadWriteLock
        */
        protected ReadWriteLock getLock()
            {
            return m_lock;
            }

        // ----- Collection interface -------------------------------------

        /**
        * {@inheritDoc}
        */
        public int size()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().size();
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean isEmpty()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().isEmpty();
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean contains(Object o)
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().contains(o);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public Iterator<E> iterator()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return new AbstractWrapperIterator<E>(getDelegate().iterator())
                {
                    public void remove()
                        {
                        Lock lock = m_lockExclusive;
                        lock.lock();
                        try
                            {
                            getDelegate().remove();
                            }
                        finally
                            {
                            lock.unlock();
                            }
                        }
                };
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object[] toArray()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().toArray();
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public <T> T[] toArray(T[] a)
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().toArray(a);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean add(E o)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                return getDelegate().add(o);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean remove(Object o)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                return getDelegate().remove(o);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsAll(Collection<?> c)
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().containsAll(c);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean addAll(Collection<? extends E> c)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                return getDelegate().addAll(c);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean retainAll(Collection<?> c)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                return getDelegate().retainAll(c);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean removeAll(Collection<?> c)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                return getDelegate().removeAll(c);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void clear()
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                getDelegate().clear();
                }
            finally
                {
                lock.unlock();
                }
            }

        // ----- Object methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object oOther)
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().equals(oOther);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().hashCode();
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return getDelegate().toString();
                }
            finally
                {
                lock.unlock();
                }
            }

        // ----- data members ---------------------------------------------

        /**
        * The Collection to which this wrapper delegates.
        */
        protected final Collection<E> m_colDelegate;

        /**
        * The read/write lock for concurrency control.
        */
        protected final ReadWriteLock m_lock;

        /**
        * The shared lock for performing read operations.
        */
        protected final Lock m_lockShared;

        /**
        * The exclusive lock for performing read/write operations.
        */
        protected final Lock m_lockExclusive;
        }


    // ----- inner class: ConcurrentWrapperSet ------------------------------

    /**
    * Set implementation which uses a ReadWriteLock to manage concurrent
    * access to an underlying Set.
    *
    * @since Coherence 3.7
    */
    public static class ConcurrentWrapperSet<E>
            extends ConcurrentWrapperCollection<E>
            implements Set<E>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create an ConcurrentWrapperSet which delegates to the specified Set.
        *
        * @param set  the Set to delegate all calls to
        */
        public ConcurrentWrapperSet(Set<E> set)
            {
            super(set);
            }

        /**
        * Create an ConcurrentWrapperSet which delegates to the specified Set.
        *
        * @param set   the Set to delegate all calls to
        * @param lock  a read/write lock for concurrency management
        */
        protected ConcurrentWrapperSet(Set<E> set, ReadWriteLock lock)
            {
            super(set, lock);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Set to which all operations should be delegated to.
        *
        * @return the wrapped Set
        */
        protected Set<E> getDelegate()
            {
            return (Set<E>) super.getDelegate();
            }
        }


    // ----- inner class: ConcurrentWrapperEntrySet -------------------------

    /**
    * Map Entry Set implementation which uses a ReadWriteLock to manage
    * concurrent access to the underlying Entry objects.
    *
    * @since Coherence 3.7
    */
    public static class ConcurrentWrapperEntrySet<K, V>
            extends ConcurrentWrapperSet<Entry<K, V>>
            implements Set<Map.Entry<K, V>>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create an ConcurrentWrapperSet which delegates to the specified Set.
        *
        * @param set      the Set to delegate all calls to
        * @param lock     a read/write lock for concurrency management
        * @param fStrict  pass true to protect all Entry methods; false to
        *                 protect only the <tt>setValue()</tt> method
        */
        protected ConcurrentWrapperEntrySet(Set<Map.Entry<K, V>> set, ReadWriteLock lock, boolean fStrict)
            {
            super(set, lock);
            m_fStrict = fStrict;
            }

        // ----- Entry Set interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public Iterator<Map.Entry<K, V>> iterator()
            {
            Lock lock = m_lockShared;
            lock.lock();
            try
                {
                return new AbstractWrapperIterator<Entry<K, V>>(getDelegate().iterator())
                {
                    public Map.Entry<K, V> next()
                        {
                        return new ConcurrentWrapperEntry<K, V>(getDelegate().next(), m_lock, m_fStrict);
                        }

                    public void remove()
                        {
                        Lock lock = m_lockExclusive;
                        lock.lock();
                        try
                            {
                            getDelegate().remove();
                            }
                        finally
                            {
                            lock.unlock();
                            }
                        }
                };
                }
            finally
                {
                lock.unlock();
                }
            }

        // ----- data members ---------------------------------------------

        /**
        * Determines whether or not all of the Entry methods are protected;
        * if set to true, then all Entry methods are protected.
        */
        protected final boolean m_fStrict;
        }


    // ----- inner class: ConcurrentWrapperEntry ----------------------------

    /**
    * Map Entry implementation which uses a ReadWriteLock to manage concurrent
    * access to an underlying Map Entry.
    *
    * @since Coherence 3.7
    */
    public static class ConcurrentWrapperEntry<K, V>
            implements Map.Entry<K, V>
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create an ConcurrentWrapperEntry which delegates to the
        * specified Map Entry.
        *
        * @param entry  the Map Entry to delegate all calls to
        */
        public ConcurrentWrapperEntry(Map.Entry<K, V> entry)
            {
            this(entry, new ReentrantReadWriteLock());
            }

        /**
        * Create an ConcurrentWrapperEntry which delegates to the
        * specified Map Entry.
        *
        * @param entry  the Map Entry to delegate all calls to
        * @param lock   a read/write lock for concurrency management
        */
        protected ConcurrentWrapperEntry(Map.Entry<K, V> entry, ReadWriteLock lock)
            {
            this(entry, lock, false);
            }

        /**
        * Create an ConcurrentWrapperEntry which delegates to the
        * specified Map Entry.
        *
        * @param entry    the Map Entry to delegate all calls to
        * @param lock     a read/write lock for concurrency management
        * @param fStrict  pass true to protect all Entry methods; false to
        *                 protect only the <tt>setValue()</tt> method
        */
        protected ConcurrentWrapperEntry(Map.Entry<K, V> entry, ReadWriteLock lock, boolean fStrict)
            {
            m_entryDelegate = entry;
            m_lock = lock;
            m_lockShared = fStrict ? lock.readLock() : null;
            m_lockExclusive = lock.writeLock();
            }

        // ----- Map Entry interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public K getKey()
            {
            Lock lock = m_lockShared;
            if (lock != null)
                {
                lock.lock();
                }
            try
                {
                return getDelegate().getKey();
                }
            finally
                {
                if (lock != null)
                    {
                    lock.unlock();
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public V getValue()
            {
            Lock lock = m_lockShared;
            if (lock != null)
                {
                lock.lock();
                }
            try
                {
                return getDelegate().getValue();
                }
            finally
                {
                if (lock != null)
                    {
                    lock.unlock();
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public V setValue(V value)
            {
            Lock lock = m_lockExclusive;
            lock.lock();
            try
                {
                return getDelegate().setValue(value);
                }
            finally
                {
                lock.unlock();
                }
            }

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            Lock lock = m_lockShared;
            if (lock != null)
                {
                lock.lock();
                }
            try
                {
                return getDelegate().equals(o);
                }
            finally
                {
                if (lock != null)
                    {
                    lock.unlock();
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            Lock lock = m_lockShared;
            if (lock != null)
                {
                lock.lock();
                }
            try
                {
                return getDelegate().hashCode();
                }
            finally
                {
                if (lock != null)
                    {
                    lock.unlock();
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            Lock lock = m_lockShared;
            if (lock != null)
                {
                lock.lock();
                }
            try
                {
                return getDelegate().toString();
                }
            finally
                {
                if (lock != null)
                    {
                    lock.unlock();
                    }
                }
            }


        // ----- accessors ------------------------------------------------

        /**
        * Return the Collection to which all operations should be delegated to.
        *
        * @return the wrapped Collection
        */
        protected Entry<K, V> getDelegate()
            {
            return m_entryDelegate;
            }

        /**
        * Return the ReadWriteLock that is used to manage concurrent access
        * and modifications to the underlying map.
        *
        * @return the ReadWriteLock
        */
        protected ReadWriteLock getLock()
            {
            return m_lock;
            }

        // ----- data members ---------------------------------------------

        /**
        * The Collection to which this wrapper delegates.
        */
        private final Map.Entry<K, V> m_entryDelegate;

        /**
        * The read/write lock for concurrency control.
        */
        protected final ReadWriteLock m_lock;

        /**
        * The shared lock for performing read operations.
        * <p>
        * Note: This field is null if read-only operations are not supposed
        * to be protected, i.e. if the "strict" option is not used.
        */
        protected final Lock m_lockShared;

        /**
        * The exclusive lock for performing read/write operations.
        */
        protected final Lock m_lockExclusive;
        }

    // ----- inner class: AbstractWrapperLongArray -------------------------

    /**
     * Abstract wrapper implementation for LongArrays.
     */
    public static abstract class AbstractWrapperLongArray<V>
            implements LongArray<V>
        {
        @Override
        public abstract AbstractWrapperLongArray<V> clone();

        @Override
        public V get(long lIndex)
            {
            return delegate().get(lIndex);
            }

        @Override
        public long floorIndex(long lIndex)
            {
            return delegate().floorIndex(lIndex);
            }

        @Override
        public V floor(long lIndex)
            {
            return delegate().floor(lIndex);
            }

        @Override
        public long ceilingIndex(long lIndex)
            {
            return delegate().ceilingIndex(lIndex);
            }

        @Override
        public V ceiling(long lIndex)
            {
            return delegate().ceiling(lIndex);
            }

        @Override
        public V set(long lIndex, V oValue)
            {
            return delegate().set(lIndex, oValue);
            }

        @Override
        public long add(V oValue)
            {
            return delegate().add(oValue);
            }

        @Override
        public boolean exists(long lIndex)
            {
            return delegate().exists(lIndex);
            }

        @Override
        public V remove(long lIndex)
            {
            return delegate().remove(lIndex);
            }

        @Override
        public void remove(long lIndexFrom, long lIndexTo)
            {
            delegate().remove(lIndexFrom, lIndexTo);
            }

        @Override
        public boolean contains(V oValue)
            {
            return delegate().contains(oValue);
            }

        @Override
        public void clear()
            {
            delegate().clear();
            }

        @Override
        public boolean isEmpty()
            {
            return delegate().isEmpty();
            }

        @Override
        public int getSize()
            {
            return delegate().getSize();
            }

        @Override
        public Iterator<V> iterator()
            {
            return delegate().iterator();
            }

        @Override
        public Iterator<V> iterator(long lIndex)
            {
            return delegate().iterator(lIndex);
            }

        @Override
        public Iterator<V> reverseIterator()
            {
            return delegate().reverseIterator();
            }

        @Override
        public Iterator<V> reverseIterator(long lIndex)
            {
            return delegate().reverseIterator(lIndex);
            }

        @Override
        public long getFirstIndex()
            {
            return delegate().getFirstIndex();
            }

        @Override
        public long getLastIndex()
            {
            return delegate().getLastIndex();
            }

        @Override
        public long indexOf(V oValue)
            {
            return delegate().indexOf(oValue);
            }

        @Override
        public long indexOf(V oValue, long lIndex)
            {
            return delegate().indexOf(oValue, lIndex);
            }

        @Override
        public long lastIndexOf(V oValue)
            {
            return delegate().lastIndexOf(oValue);
            }

        @Override
        public long lastIndexOf(V oValue, long lIndex)
            {
            return delegate().lastIndexOf(oValue, lIndex);
            }

        // -----  ---------------------------------------------------

        /**
         * Return the delegate LongArray.
         *
         * @return the delegate
         */
        protected abstract LongArray<V> delegate();
        }
    }
