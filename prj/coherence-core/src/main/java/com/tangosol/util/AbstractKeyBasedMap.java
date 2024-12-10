/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.cache.CacheEvent;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
* AbstractKeyBasedMap is a base class for Map implementations. The primary
* difference between the {@link java.util.AbstractMap} abstract class and
* this abstract class is this: that AbstractMap requires a sub-class to
* provide an Entry Set implementation, while AbstractKeyBasedMap requires a
* read-only sub-class to implement only get() and iterateKeys(), and a
* read-write sub-class to additionally implement only put() and remove().
* <p>
* Read-only implementations must implement {@link #iterateKeys()} and
* {@link #get(Object)}. Read/write implementations must additionally
* implement {@link #put(Object, Object)} and {@link #remove(Object)}. A
* number of the methods have implementations provided, but are extremely
* inefficient for Maps that contain large amounts of data, including
* {@link #clear()}, {@link #containsKey(Object)}, {@link #size()} (and by
* extension {@link #isEmpty()}). Furthermore, if any of a number of method
* implementations has any cost of returning an "old value", such as is done
* by the {@link #put} and {@link #remove(Object)} methods, then the
* {@link #putAll(java.util.Map)} and {@link #removeBlind(Object)}
* methods should also be implemented.
*
* @author 2005.07.13  cp
*/
public abstract class AbstractKeyBasedMap<K, V>
        extends Base
        implements Map<K, V>
    {
    // ----- Map interface --------------------------------------------------

    /**
    * Clear all key/value mappings.
    */
    public void clear()
        {
        // this begs for sub-class optimization
        for (Iterator iter = iterateKeys(); iter.hasNext(); )
            {
            iter.next();
            iter.remove();
            }
        }

    /**
    * Returns <tt>true</tt> if this map contains a mapping for the specified
    * key.
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key, <tt>false</tt> otherwise.
    */
    public boolean containsKey(Object oKey)
        {
        // this begs for sub-class optimization
        for (Iterator iter = iterateKeys(); iter.hasNext(); )
            {
            if (equals(oKey, iter.next()))
                {
                return true;
                }
            }
        return false;
        }

    /**
    * Returns <tt>true</tt> if this Map maps one or more keys to the
    * specified value.
    *
    * @return <tt>true</tt> if this Map maps one or more keys to the
    *         specified value, <tt>false</tt> otherwise
    */
    public boolean containsValue(Object oValue)
        {
        return values().contains(oValue);
        }

    /**
    * Returns a set view of the mappings contained in this map.  Each element
    * in the returned set is an {@link Entry}. The set is backed by the
    * map, so changes to the map are reflected in the set, and vice-versa.
    * If the map is modified while an iteration over the set is in progress
    * (except by the iterator's own <tt>remove</tt> operation, or by the
    * <tt>setValue</tt> operation on a map entry returned by the iterator)
    * the results of the iteration are undefined. The set supports element
    * removal, which removes the corresponding mapping from the map, via the
    * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
    * <tt>retainAll</tt> and <tt>clear</tt> operations. It is not expected to
    * support the <tt>add</tt> or <tt>addAll</tt> operations.
    *
    * @return a set view of the mappings contained in this map
    */
    public Set<Map.Entry<K, V>> entrySet()
        {
        // no need to synchronize; it is acceptable that two threads would
        // instantiate an entry set
        Set<Map.Entry<K, V>> set = m_setEntries;
        if (set == null)
            {
            m_setEntries = set = instantiateEntrySet();
            }
        return set;
        }

    /**
    * Returns the value to which this map maps the specified key.
    *
    * @param oKey  the key object
    *
    * @return the value to which this map maps the specified key,
    *         or null if the map contains no mapping for this key
    */
    public abstract V get(Object oKey);

    /**
    * Returns <tt>true</tt> if this map contains no key-value mappings.
    *
    * @return <tt>true</tt> if this map contains no key-value mappings
    */
    public boolean isEmpty()
        {
        return size() == 0;
        }

    /**
    * Returns a set view of the keys contained in this map.  The set is
    * backed by the map, so changes to the map are reflected in the set, and
    * vice-versa. If the map is modified while an iteration over the set is
    * in progress (except through the iterator's own <tt>remove</tt>
    * operation), the results of the iteration are undefined. The set
    * supports element removal, which removes the corresponding mapping from
    * the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
    * <tt>removeAll</tt> <tt>retainAll</tt>, and <tt>clear</tt> operations.
    * It is not expected to support the add or <tt>addAll</tt> operations.
    *
    * @return a set view of the keys contained in this map
    */
    public Set<K> keySet()
        {
        // no need to synchronize; it is acceptable that two threads would
        // instantiate a key set
        Set<K> set = m_setKeys;
        if (set == null)
            {
            m_setKeys = set = instantiateKeySet();
            }
        return set;
        }

    /**
    * Associates the specified value with the specified key in this map.
    *
    * @param oKey    key with which the specified value is to be associated
    * @param oValue  value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key
    */
    public V put(K oKey, V oValue)
        {
        throw new UnsupportedOperationException();
        }

    /**
    * Copies all of the mappings from the specified map to this map. The
    * effect of this call is equivalent to that of calling {@link #put}
    * on this map once for each mapping in the passed map. The behavior of
    * this operation is unspecified if the passed map is modified while the
    * operation is in progress.
    *
    * @param map  the Map containing the key/value pairings to put into this
    *             Map
    */
    public void putAll(Map<? extends K, ? extends V> map)
        {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            {
            put(entry.getKey(), entry.getValue());
            }
        }

    /**
    * Removes the mapping for this key from this map if present.
    * Expensive: updates both the underlying cache and the local cache.
    *
    * @param oKey key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values.
    */
    public V remove(Object oKey)
        {
        throw new UnsupportedOperationException();
        }

    /**
    * Returns the number of key-value mappings in this map.
    *
    * @return the number of key-value mappings in this map
    */
    public int size()
        {
        // this begs for sub-class optimization
        int c = 0;
        for (Iterator iter = iterateKeys(); iter.hasNext(); )
            {
            iter.next();
            ++c;
            }
        return c;
        }

    /**
    * Returns a collection view of the values contained in this map. The
    * collection is backed by the map, so changes to the map are reflected in
    * the collection, and vice-versa. If the map is modified while an
    * iteration over the collection is in progress (except through the
    * iterator's own <tt>remove</tt> operation), the results of the
    * iteration are undefined.  The collection supports element removal,
    * which removes the corresponding mapping from the map, via the
    * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
    * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations.
    * It is not expected to support the add or <tt>addAll</tt> operations.
    *
    * @return a Collection view of the values contained in this map
    */
    public Collection<V> values()
        {
        // no need to synchronize; it is acceptable that two threads would
        // instantiate a key set
        Collection<V> coll = m_collValues;
        if (coll == null)
            {
            m_collValues = coll = instantiateValues();
            }
        return coll;
        }


    // ----- CacheMap methods -----------------------------------------------

    /**
    * Get all the specified keys, if they are in the Map. For each key
    * that is in the cache, that key and its corresponding value will be
    * placed in the map that is returned by this method. The absence of
    * a key in the returned map indicates that it was not in the cache,
    * which may imply (for caches that can load behind the scenes) that
    * the requested data could not be loaded.
    *
    * @param colKeys  a collection of keys that may be in the named cache
    *
    * @return a Map of keys to values for the specified keys passed in
    *         <tt>colKeys</tt>
    */
    public Map<K, V> getAll(Collection<? extends K> colKeys)
        {
        Map<K, V> map = new ListMap<>();
        for (K key : colKeys)
            {
            V val = get(key);
            if (val != null || containsKey(key))
                {
                map.put(key, val);
                }
            }
        return map;
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Create an iterator over the keys in this Map. The Iterator must
    * support remove() if the Map supports removal.
    *
    * @return a new instance of an Iterator over the keys in this Map
    */
    protected abstract Iterator<K> iterateKeys();

    /**
    * Removes the mapping for this key from this map if present. This method
    * exists to allow sub-classes to optimize remove functionality for
    * situations in which the original value is not required.
    *
    * @param oKey key whose mapping is to be removed from the map
    *
    * @return true iff the Map changed as the result of this operation
    */
    protected boolean removeBlind(Object oKey)
        {
        if (containsKey(oKey))
            {
            remove(oKey);
            return true;
            }
        else
            {
            return false;
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compares the specified object with this map for equality.  Returns
    * <tt>true</tt> if the given object is also a map and the two maps
    * represent the same mappings.  More formally, two maps <tt>t1</tt> and
    * <tt>t2</tt> represent the same mappings if
    * <tt>t1.keySet().equals(t2.keySet())</tt> and for every key <tt>k</tt>
    * in <tt>t1.keySet()</tt>, <tt> (t1.get(k)==null ? t2.get(k)==null :
    * t1.get(k).equals(t2.get(k))) </tt>.  This ensures that the
    * <tt>equals</tt> method works properly across different implementations
    * of the map interface.
    *
    * @param o object to be compared for equality with this Map
    *
    * @return <tt>true</tt> if the specified object is equal to this Map
    */
    public boolean equals(Object o)
        {
        if (o instanceof Map)
            {
            Map that = (Map) o;
            if (this == that)
                {
                return true;
                }

            if (this.size() == that.size())
                {
                for (Entry<K, V> entry : entrySet())
                    {
                    Object oKey       = entry.getKey();
                    Object oThisValue = entry.getValue();
                    Object oThatValue;
                    try
                        {
                        oThatValue = that.get(oKey);
                        }
                    catch (ClassCastException e)
                        {
                        // to be compatible with java.util.AbstractMap
                        return false;
                        }
                    catch (NullPointerException e)
                        {
                        // to be compatible with java.util.AbstractMap
                        return false;
                        }

                    if (oThisValue == this || oThisValue == that)
                        {
                        // this could be infinite recursion
                        if (oThatValue != this && oThatValue != that)
                            {
                            // it is not safe to call equals(); it would
                            // likely lead to infinite recursion
                            return false;
                            }
                        }
                    else if (!equals(oThisValue, oThatValue)
                            || oThatValue == null && !that.containsKey(oKey))
                        {
                        return false;
                        }
                    }

                // size is identical and all entries match
                return true;
                }
            }

        return false;
        }

    /**
    * Returns the hash code value for this Map. The hash code of a Map is
    * defined to be the sum of the hash codes of each entry in the Map's
    * <tt>entrySet()</tt> view.  This ensures that <tt>t1.equals(t2)</tt>
    * implies that <tt>t1.hashCode()==t2.hashCode()</tt> for any two maps
    * <tt>t1</tt> and <tt>t2</tt>, as required by the general contract of
    * Object.hashCode.
    *
    * @return the hash code value for this Map
    */
    public int hashCode()
        {
        int nHash = 0;
        for (Entry<K, V> entry : entrySet())
            {
            nHash += entry.hashCode();
            }
        return nHash;
        }

    /**
    * Returns a string representation of this Map.  The string representation
    * consists of a list of key-value mappings in the order returned by the
    * Map's <tt>entrySet</tt> view's iterator, enclosed in braces
    * (<tt>"{}"</tt>).  Adjacent mappings are separated by the characters
    * <tt>", "</tt> (comma and space).  Each key-value mapping is rendered as
    * the key followed by an equals sign (<tt>"="</tt>) followed by the
    * associated value.  Keys and values are converted to strings as by
    * <tt>String.valueOf(Object)</tt>.
    *
    * @return a String representation of this Map
    */
    public String toString()
        {
        StringBuilder sb = new StringBuilder(100 + (size() << 3));
        sb.append('{');

        boolean fFirst = true;
        for (Entry<K, V> entry : entrySet())
            {
            if (fFirst)
                {
                fFirst = false;
                }
            else
                {
                sb.append(", ");
                }

            K key   = entry.getKey();
            V value = entry.getValue();

            // as per AbstractMap, detect the condition in which case this
            // map is used as a key and/or a value inside itself, which
            // would result in infinite recursion
            sb.append(key == this ? "(this Map)" : String.valueOf(key))
              .append('=')
              .append(value == this ? "(this Map)" : String.valueOf(value));
            }

        sb.append('}');
        return sb.toString();
        }

    /**
    * Returns a shallow copy of this <tt>AbstractKeyBasedMap</tt> instance;
    * the keySet, entrySet and values collections are not cloned or copied
    * to (shared by) the clone.
    *
    * @return a shallow copy of this map
    */
    protected Object clone()
            throws CloneNotSupportedException
        {
        AbstractKeyBasedMap that = (AbstractKeyBasedMap) super.clone();
        that.m_setKeys    = null;
        that.m_setEntries = null;
        that.m_collValues = null;
        return that;
        }


    // ----- inner class: DeferredCacheEvent --------------------------------

    /**
    * A DeferredCacheEvent is a {@link CacheEvent} object that defers the loading
    * of the {@link #getOldValue() old value}.
    * <p>
    * This event has two predominant states; active and inactive. The event is
    * active from incarnation and can transition to inactive (via {@link #deactivate()})
    * but not vice-versa. Being active allows the getOldValue implementation
    * to load the value from underlying map, thus all consumers of this
    * event must ensure they call getOldValue prior to returning control when
    * this event is dispatched.
    * <p>
    * Once inactive the correct value may no longer be available in the map
    * thus in this state a getOldValue will return either null or the cached old value.
    */
    protected abstract static class DeferredCacheEvent<K, V>
            extends CacheEvent<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructs a new DeferredCacheEvent.
        *
        * @param map         the ObservableMap
        * @param nId         this event's id, one of {@link #ENTRY_INSERTED},
        *                    {@link #ENTRY_UPDATED} or {@link #ENTRY_DELETED}
        * @param oKey        the key into the map
        * @param oValueOld   the old value (for update and delete events)
        * @param oValueNew   the new value (for insert and update events)
        * @param fSynthetic  true iff the event is caused by the cache
        *                    internal processing such as eviction or loading
        */
        public DeferredCacheEvent(ObservableMap<K, V> map, int nId, K oKey,
                                  V oValueOld, V oValueNew, boolean fSynthetic)
            {
            super(map, nId, oKey,
                  oValueOld == null ? (V) NO_VALUE : oValueOld, oValueNew, fSynthetic);
            }


        // ----- helper methods ---------------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        public V getOldValue()
            {
            // since m_oValueOld is not volatile we double read the volatile
            // m_fActive to ensure the event was not deactivated whilst we were
            // loading the old value
            V oValueOld = m_valueOld;
            if (oValueOld == NO_VALUE && m_fActive)
                {
                oValueOld = readOldValue();
                if (m_fActive)
                    {
                    return m_valueOld = oValueOld;
                    }
                }
            return oValueOld == NO_VALUE ? null : oValueOld;
            }

        /**
        * Perform a deferred read for the old value in a usage-specific way.
        *
        * @return the old value
        */
        protected abstract V readOldValue();

        /**
        * Deactivate this DeferredCacheEvent instance. This is used to prevent
        * future {@link #getOldValue()} calls using the underlying map *after*
        * the event dispatching code returned and the content of the map had
        * been changed to a new value.
        * <p>
        * The contract between the DeferredCacheEvent and consumers of
        * the event states that consumers must call {@code getOldValue} prior to
        * returning from event dispatching logic.
        */
        public void deactivate()
            {
            if (m_valueOld == NO_VALUE)
                {
                m_valueOld = null;
                }
            m_fActive = false;
            }

        /**
        * Whether the DeferredCacheEvent is in an active or inactive state.
        */
        protected volatile boolean m_fActive = true;
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
    * Factory pattern: Create a Set that represents the keys in the Map
    *
    * @return a new instance of Set that represents the keys in the Map
    */
    protected Set<K> instantiateKeySet()
        {
        return new KeySet();
        }

    /**
    * A set of keys backed by this map.
    */
    protected class KeySet
            extends AbstractSet<K>
        {
        // ----- Set interface ------------------------------------------

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            AbstractKeyBasedMap.this.clear();
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.<p>
        *
        * @param o object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified
        *         element
        */
        public boolean contains(Object o)
            {
            return AbstractKeyBasedMap.this.containsKey(o);
            }

        /**
        * Returns <tt>true</tt> if this Set is empty.
        *
        * @return <tt>true</tt> if this Set is empty
        */
        public boolean isEmpty()
            {
            return AbstractKeyBasedMap.this.isEmpty();
            }

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection
        */
        public Iterator<K> iterator()
            {
            if (isEmpty())
                {
                return NullImplementation.getIterator();
                }

            return AbstractKeyBasedMap.this.iterateKeys();
            }

        /**
        * Removes the specified element from this Set of keys if it is
        * present by removing the associated key from the underlying
        * Map.
        *
        * @param o  object to be removed from this set, if present
        *
        * @return true if the set contained the specified element
        */
        public boolean remove(Object o)
            {
            return AbstractKeyBasedMap.this.removeBlind(o);
            }

        /**
        * Returns the number of elements in this collection.
        *
        * @return the number of elements in this collection
        */
        public int size()
            {
            return AbstractKeyBasedMap.this.size();
            }
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * Factory pattern: Create a Set that represents the entries in the Map.
    *
    * @return a new instance of Set that represents the entries in the Map
    */
    protected Set<Map.Entry<K, V>> instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries backed by this map.
    */
    public class EntrySet
            extends AbstractSet<Map.Entry<K, V>>
        {
        // ----- Set interface ------------------------------------------

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            AbstractKeyBasedMap.this.clear();
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.<p>
        *
        * @param o object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified
        *         element
        */
        public boolean contains(Object o)
            {
            if (o instanceof Map.Entry)
                {
                Map.Entry entry  = (Map.Entry) o;
                Object    oKey   = entry.getKey();
                Object    oValue = entry.getValue();
                Map       map    = AbstractKeyBasedMap.this;
                return Base.equals(oValue, map.get(oKey))
                    && (oValue != null || map.containsKey(oKey));
                }

            return false;
            }

        /**
        * Returns <tt>true</tt> if this Set is empty.
        *
        * @return <tt>true</tt> if this Set is empty
        */
        public boolean isEmpty()
            {
            return AbstractKeyBasedMap.this.isEmpty();
            }

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection
        */
        public Iterator<Map.Entry<K, V>> iterator()
            {
            if (isEmpty())
                {
                return NullImplementation.getIterator();
                }

            return instantiateIterator();
            }

        /**
        * Removes the specified element from this Set of entries if it is
        * present by removing the associated entry from the underlying Map.
        *
        * @param o  object to be removed from this set, if present
        *
        * @return true if the set contained the specified element
        */
        public boolean remove(Object o)
            {
            if (contains(o))
                {
                AbstractKeyBasedMap.this.remove(((Map.Entry) o).getKey());
                return true;
                }
            else
                {
                return false;
                }
            }

        /**
        * Returns the number of elements in this collection.
        *
        * @return the number of elements in this collection
        */
        public int size()
            {
            return AbstractKeyBasedMap.this.size();
            }


        // ----- inner class: Entry -------------------------------------

        /**
        * Factory pattern. Create a Map Entry.
        *
        * @param oKey    the Entry key (required)
        * @param oValue  the Entry value (optional; lazy loaded if necessary)
        *
        * @return a new instance of an Entry with the specified key and
        *         value (if one is provided)
        */
        protected Map.Entry<K, V> instantiateEntry(K oKey, V oValue)
            {
            return new Entry(oKey, oValue);
            }

        /**
        * A Map Entry implementation that defers its value acquisition from
        * the containing map (via {@link Map#get(Object)}) if the Entry is
        * constructed with a null value.
        */
        protected class Entry
                extends SimpleMapEntry<K, V>
            {
            /**
            * Construct an Entry.
            *
            * @param oKey    the Entry key
            * @param oValue  the Entry value (optional)
            */
            public Entry(K oKey, V oValue)
                {
                super(oKey, oValue);
                }

            /**
            * Returns the value corresponding to this entry.
            *
            * @return the value corresponding to this entry
            */
            public V getValue()
                {
                V oValue = super.getValue();
                if (oValue == null)
                    {
                    oValue = AbstractKeyBasedMap.this.get(getKey());
                    super.setValue(oValue);
                    }
                return oValue;
                }

            /**
            * Replaces the value corresponding to this entry with the
            * specified value (optional operation).  (Writes through
            * to the map.)
            *
            * @param oValue  new value to be stored in this entry
            *
            * @return old value corresponding to the entry
            */
            public V setValue(V oValue)
                {
                V oValueOrig = AbstractKeyBasedMap.this.put(getKey(), oValue);
                super.setValue(oValue);
                return oValueOrig;
                }

            /**
            * Returns the hash code value for this map entry.  The
            * hash code of a map entry <tt>e</tt> is defined to be:
            * <pre>
            *     (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
            *     (e.getValue()==null ? 0 : e.getValue().hashCode())
            * </pre>
            * This ensures that <tt>e1.equals(e2)</tt> implies that
            * <tt>e1.hashCode()==e2.hashCode()</tt> for any two
            * Entries <tt>e1</tt> and <tt>e2</tt>, as required by the
            * general contract of <tt>Object.hashCode</tt>.
            *
            * @return the hash code value for this map entry.
            */
            public int hashCode()
                {
                K oKey   = getKey();
                V oValue = getValue();

                AbstractKeyBasedMap map = AbstractKeyBasedMap.this;
                return (oKey   == null || oKey   == map ? 0 : oKey  .hashCode()) ^
                       (oValue == null || oValue == map ? 0 : oValue.hashCode());
                }
            }

        // ----- inner class: Entry Set Iterator ------------------------

        /**
        * Factory pattern.
        *
        * @return a new instance of an Iterator over the EntrySet
        */
        protected Iterator<Map.Entry<K, V>> instantiateIterator()
            {
            return new EntrySetIterator();
            }

        /**
        * An Iterator over the EntrySet that is backed by the Map.
        */
        protected class EntrySetIterator
                implements Iterator<Map.Entry<K, V>>
            {
            // ----- Iterator interface -----------------------------

            /**
            * Returns <tt>true</tt> if the iteration has more elements. (In
            * other words, returns <tt>true</tt> if <tt>next</tt> would
            * return an element rather than throwing an exception.)
            *
            * @return <tt>true</tt> if the iterator has more elements
            */
            public boolean hasNext()
                {
                return m_iterKeys.hasNext();
                }

            /**
            * Returns the next element in the iteration.
            *
            * @return the next element in the iteration
            *
            * @exception NoSuchElementException iteration has no more elements
            */
            public Map.Entry<K, V> next()
                {
                return instantiateEntry(m_iterKeys.next(), /*value*/ null);
                }

            /**
            * Removes from the underlying collection the last element
            * returned by the iterator.  This method can be called only once
            * per call to <tt>next</tt>.  The behavior of an iterator is
            * unspecified if the underlying collection is modified while the
            * iteration is in progress in any way other than by calling this
            * method.
            *
            * @exception IllegalStateException if the <tt>next</tt> method
            *         has not yet been called, or the <tt>remove</tt>
            *         method has already been called after the last call
            *         to the <tt>next</tt> method
            */
            public void remove()
                {
                m_iterKeys.remove();
                }

            // ----- data members -----------------------------------

            /**
            * Key iterator.
            */
            protected Iterator<K> m_iterKeys = AbstractKeyBasedMap.this.iterateKeys();
            }
        }


    // ----- inner class: ValuesCollection ----------------------------------

    /**
    * Factory pattern: Instantiate the values Collection.
    *
    * @return a new instance of Collection that represents this Map's values
    */
    protected Collection<V> instantiateValues()
        {
        return new ValuesCollection();
        }

    /**
    * A Collection of values backed by this map.
    */
    protected class ValuesCollection
            extends AbstractCollection<V>
        {
        // ----- Set interface ------------------------------------------

        /**
        * Removes all of the elements from this Collection by clearing the
        * underlying Map.
        */
        public void clear()
            {
            AbstractKeyBasedMap.this.clear();
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.<p>
        *
        * @param o object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified
        *         element
        */
        public boolean contains(Object o)
            {
            // this is hardly optimal, and sub-classes that have better
            // means to implement this should do so
            if (o == null)
                {
                for (Iterator iter = iterator(); iter.hasNext(); )
                    {
                    if (iter.next() == null)
                        {
                        return true;
                        }
                    }
                }
            else
                {
                for (Iterator iter = iterator(); iter.hasNext(); )
                    {
                    if (Base.equals(o, iter.next()))
                        {
                        return true;
                        }
                    }
                }
            return false;
            }

        /**
        * Returns <tt>true</tt> if this Set is empty.
        *
        * @return <tt>true</tt> if this Set is empty
        */
        public boolean isEmpty()
            {
            return AbstractKeyBasedMap.this.isEmpty();
            }

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection
        */
        public Iterator<V> iterator()
            {
            if (isEmpty())
                {
                return NullImplementation.getIterator();
                }

            return instantiateIterator();
            }

        /**
        * Removes the specified element from this Collection of values if it
        * is present by removing the associated key/value mapping from the
        * underlying Map.
        *
        * @param o  object to be removed from this Collection, if present
        *
        * @return true if the Collection contained the specified element
        */
        public boolean remove(Object o)
            {
            // this is hardly optimal, and sub-classes that have better
            // means to implement this should do so
            if (o == null)
                {
                for (Iterator iter = iterator(); iter.hasNext(); )
                    {
                    if (iter.next() == null)
                        {
                        iter.remove();
                        return true;
                        }
                    }
                }
            else
                {
                for (Iterator iter = iterator(); iter.hasNext(); )
                    {
                    if (Base.equals(o, iter.next()))
                        {
                        iter.remove();
                        return true;
                        }
                    }
                }
            return false;
            }

        /**
        * Returns the number of elements in this collection.
        *
        * @return the number of elements in this collection
        */
        public int size()
            {
            return AbstractKeyBasedMap.this.size();
            }


        // ----- inner class: Values Collection Iterator ----------------

        /**
        * Factory pattern: Create a values Iterator.
        *
        * @return a new instance of an Iterator over the values Collection
        */
        protected Iterator<V> instantiateIterator()
            {
            return new ValuesIterator();
            }

        /**
        * An Iterator over the values Collection that is backed by the
        * AbstractKeyBasedMap.
        */
        protected class ValuesIterator
                implements Iterator<V>
            {
            // ----- Iterator interface -----------------------------

            /**
            * Returns <tt>true</tt> if the iteration has more elements. (In
            * other words, returns <tt>true</tt> if <tt>next</tt> would
            * return an element rather than throwing an exception.)
            *
            * @return <tt>true</tt> if the iterator has more elements
            */
            public boolean hasNext()
                {
                return m_iterKeys.hasNext();
                }

            /**
            * Returns the next element in the iteration.
            *
            * @return the next element in the iteration
            *
            * @exception NoSuchElementException iteration has no more elements
            */
            public V next()
                {
                return AbstractKeyBasedMap.this.get(m_iterKeys.next());
                }

            /**
            * Removes from the underlying collection the last element
            * returned by the iterator.  This method can be called only once
            * per call to <tt>next</tt>.  The behavior of an iterator is
            * unspecified if the underlying collection is modified while the
            * iteration is in progress in any way other than by calling this
            * method.
            *
            * @exception IllegalStateException if the <tt>next</tt> method
            *         has not yet been called, or the <tt>remove</tt>
            *         method has already been called after the last call
            *         to the <tt>next</tt> method
            */
            public void remove()
                {
                m_iterKeys.remove();
                }

            // ----- data members -----------------------------------

            /**
            * A key iterator.
            */
            protected Iterator<K> m_iterKeys = AbstractKeyBasedMap.this.iterateKeys();
            }
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The key Set for this Map; lazily instantiated.
    */
    private transient Set<K> m_setKeys;

    /**
    * The entry Set for this Map; lazily instantiated.
    */
    private transient Set<Map.Entry<K, V>> m_setEntries;

    /**
    * The values Collection for this Map; lazily instantiated.
    */
    private transient Collection<V> m_collValues;

    /**
    * Constant to indicate the old value in a DeferredCacheEvent has not been
    * populated.
    */
    private static final Object NO_VALUE = new Object();
    }
