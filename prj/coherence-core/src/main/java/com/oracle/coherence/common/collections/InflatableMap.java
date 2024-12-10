/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;

import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.lang.reflect.Array;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;


/**
* An implementation of java.util.Map that is optimal (in terms of both size
* and speed) for very small sets of data but still works excellently with
* large sets of data.  This implementation is not thread-safe.
* <p>
* The InflatableMap implementation switches at runtime between several different
* sub-implementations for storing the Map of objects, described here:
*
* <ol>
* <li>"empty map" - a map that contains no data;
* <li>"single entry" - a reference directly to a single map entry
* <li>"Object[]" - a reference to an array of entries; the item limit for
*     this implementation is determined by the THRESHOLD constant;
* <li>"delegation" - for more than THRESHOLD items, a map is created to
*     delegate the map management to; sub-classes can override the default
*     delegation class (java.util.HashMap) by overriding the factory method
*     instantiateMap.
* </ol>
* <p>
* The InflatableMap implementation supports the null key value.
*
* @author cp 06/29/99
*/
public class InflatableMap<K, V>
        extends AbstractMap<K, V>
        implements Cloneable, Externalizable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a InflatableMap.
    */
    public InflatableMap()
        {
        }

    /**
    * Construct a InflatableMap with the same mappings as the given map.
    *
    * @param map the map whose mappings are to be placed in this map.
    */
    public InflatableMap(Map<? extends K, ? extends V> map)
        {
        putAll(map);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Returns <tt>true</tt> if this map contains no key-value mappings.
    *
    * @return <tt>true</tt> if this map contains no key-value mappings
    */
    public boolean isEmpty()
        {
        return m_nImpl == I_EMPTY;
        }

    /**
    * Returns the number of key-value mappings in this map.
    *
    * @return the number of key-value mappings in this map
    */
    public int size()
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return 0;

            case I_SINGLE:
                return 1;

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                return m_nImpl - I_ARRAY_1 + 1;

            case I_OTHER:
                return ((Map) m_oContents).size();

            default:
                throw new IllegalStateException();
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
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_SINGLE:
                {
                Object oKeyEntry = ((Map.Entry) m_oContents).getKey();
                return Objects.equals(oKey, oKeyEntry);
                }

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                // "Entry[]" implementation
                Map.Entry<K, V>[] aEntry = (Map.Entry[]) m_oContents;
                int         c      = m_nImpl - I_ARRAY_1 + 1;
                return indexOf(aEntry, c, oKey) >= 0;
                }

            case I_OTHER:
                return ((Map) m_oContents).containsKey(oKey);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns the value to which this map maps the specified key.
    *
    * @param oKey  the key object
    *
    * @return the value to which this map maps the specified key,
    *         or null if the map contains no mapping for this key
    */
    public V get(Object oKey)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return null;

            case I_SINGLE:
                {
                Map.Entry<K, V> entry = (Map.Entry) m_oContents;
                K entryKey = entry.getKey();
                return Objects.equals(oKey, entryKey) ? entry.getValue() : null;
                }

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                // "Entry[]" implementation
                Map.Entry<K, V>[] aEntry = (Map.Entry<K, V>[]) m_oContents;
                int         c      = m_nImpl - I_ARRAY_1 + 1;
                int         i      = indexOf(aEntry, c, oKey);
                return i < 0 ? null : aEntry[i].getValue();
                }

            case I_OTHER:
                return ((Map<K, V>) m_oContents).get(oKey);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Associates the specified value with the specified key in this map.
    *
    * @param key    key with which the specified value is to be associated
    * @param value  value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key
    */
    public V put(K key, V value)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                m_oContents = instantiateEntry(key, value);
                m_nImpl     = I_SINGLE;
                return null;

            case I_SINGLE:
                {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) m_oContents;
                K entryKey = entry.getKey();
                V prevValue = null;
                if (Objects.equals(key, entryKey))
                    {
                    prevValue = entry.getValue();
                    entry.setValue(value);
                    }
                else
                    {
                    // grow to array implementation
                    Map.Entry<K, V>[] aEntry = new Map.Entry[THRESHOLD];
                    aEntry[0] = entry;
                    aEntry[1] = instantiateEntry(key, value);

                    m_nImpl     = I_ARRAY_2;
                    m_oContents = aEntry;
                    }

                return prevValue;
                }

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                // "Entry[]" implementation
                int               nImpl  = m_nImpl;
                Map.Entry<K, V>[] aEntry = (Map.Entry[]) m_oContents;
                int               c      = nImpl - I_ARRAY_1 + 1;
                int               i      = indexOf(aEntry, c, key);
                if (i >= 0)
                    {
                    Map.Entry<K, V> entry = aEntry[i];
                    V prevValue = entry.getValue();
                    entry.setValue(value);
                    return prevValue;
                    }

                // check if adding the object exceeds the "lite" threshold
                if (c >= THRESHOLD)
                    {
                    // time to switch to a different map implementation
                    Map<K, V> map = instantiateMap();
                    for (i = 0; i < c; ++i)
                        {
                        Map.Entry<K, V> entry = aEntry[i];
                        map.put(entry.getKey(), entry.getValue());
                        }
                    map.put(key, value);

                    m_nImpl     = I_OTHER;
                    m_oContents = map;
                    }
                else
                    {
                    // use the next available element in the array
                    aEntry[c] = instantiateEntry(key, value);
                    m_nImpl   = (byte) (nImpl + 1);
                    }

                return null;
                }

            case I_OTHER:
                return ((Map<K, V>) m_oContents).put(key, value);

            default:
                throw new IllegalStateException();
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
        switch (m_nImpl)
            {
            case I_EMPTY:
                return null;

            case I_SINGLE:
                {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) m_oContents;
                K entryKey = entry.getKey();
                V prevValue = null;
                if (Objects.equals(oKey, entryKey))
                    {
                    prevValue   = entry.getValue();
                    m_nImpl     = I_EMPTY;
                    m_oContents = null;
                    }
                return prevValue;
                }

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                // "Entry[]" implementation
                int               nImpl  = m_nImpl;
                Map.Entry<K, V>[] aEntry = (Map.Entry[]) m_oContents;
                int               c      = nImpl - I_ARRAY_1 + 1;
                int               i      = indexOf(aEntry, c, oKey);
                if (i < 0)
                    {
                    return null;
                    }

                V prevValue = aEntry[i].getValue();
                if (c == 1)
                    {
                    m_nImpl     = I_EMPTY;
                    m_oContents = null;
                    }
                else
                    {
                    System.arraycopy(aEntry, i + 1, aEntry, i, c - i - 1);
                    aEntry[c-1] = null;
                    m_nImpl = (byte) --nImpl;
                    }
                return prevValue;
                }

            case I_OTHER:
                {
                Map<K, V> map = (Map<K, V>) m_oContents;
                V prevValue = map.remove(oKey);
                checkShrinkFromOther();
                return prevValue;
                }

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Clear all key/value mappings.
    */
    public void clear()
        {
        m_nImpl     = I_EMPTY;
        m_oContents = null;
        }


    // ----- Cloneable interface --------------------------------------------

    /**
    * Create a clone of the ImmutableArrayList.
    *
    * @return a clone of this list
    */
    public Object clone()
        {
        InflatableMap that;
        try
            {
            that = (InflatableMap) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw ensureRuntimeException(e);
            }

        switch (this.m_nImpl)
            {
            case I_EMPTY:
                // nothing to do
                break;

            case I_SINGLE:
                {
                Map.Entry<K, V> entry = (Map.Entry) m_oContents;
                that.m_oContents = that.instantiateEntry(entry.getKey(), entry.getValue());
                }
                break;

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                Map.Entry<K, V>[] aEntryThis = (Map.Entry[]) this.m_oContents;
                Map.Entry<K, V>[] aEntryThat = new Map.Entry[THRESHOLD];
                for (int i = 0, c = m_nImpl - I_ARRAY_1 + 1; i < c; ++i)
                    {
                    Map.Entry<K, V> entryThis = aEntryThis[i];
                    aEntryThat[i] = that.instantiateEntry(
                            entryThis.getKey(), entryThis.getValue());
                    }
                that.m_oContents = aEntryThat;
                }
                break;

            case I_OTHER:
                Map<K, V> mapThis = (Map<K, V>) this.m_oContents;
                Map<K, V> mapThat = that.instantiateMap();
                mapThat.putAll(mapThis);
                that.m_oContents = mapThat;
                break;

            default:
                throw new IllegalStateException();
            }

        return that;
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * Returns a set view of the mappings contained in this map.  Each element
    * in the returned set is an {@link java.util.Map.Entry Map Entry}. The set is backed by the
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
    public Set<Entry<K,V>> entrySet()
        {
        return instantiateEntrySet();
        }

    /**
    * A Set of entries backed by this Map.
    */
    protected class EntrySet
            extends AbstractSet<Entry<K, V>>
            implements Serializable
        {
        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection
        */
        public Iterator<Entry<K, V>> iterator()
            {
            InflatableMap<K, V> map = InflatableMap.this;
            int           c   = map.size();
            return c == 0
                    ? NULL_ITERATOR
                    : new EntryIterator<>(map, (Map.Entry<K, V>[]) toArray(new Map.Entry[c]));
            }

        /**
        * Returns <tt>true</tt> if this Set is empty.
        *
        * @return <tt>true</tt> if this Set is empty
        */
        public boolean isEmpty()
            {
            return InflatableMap.this.isEmpty();
            }

        /**
        * Returns the number of elements in this collection.
        *
        * @return the number of elements in this collection
        */
        public int size()
            {
            return InflatableMap.this.size();
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
                Map.Entry entry   = (Map.Entry) o;
                Object    oKey    = entry.getKey();
                Object    oValue  = entry.getValue();
                InflatableMap map     = InflatableMap.this;
                Object    oActual = map.get(oKey);
                return oActual == null
                        ? oValue == null && map.containsKey(oKey)
                        : Objects.equals(oValue, oActual);
                }

            return false;
            }

        /**
        * Returns an array containing all of the elements in this collection.
        *
        * @return an array containing all of the elements in this collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the elements in this collection.  If the
        * collection fits in the specified array, it is returned therein.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.
        * <p>
        * If the collection fits in the specified array with room to spare
        * (i.e. the array has more elements than the collection), the element
        * in the array immediately following the end of the collection is set
        * to <tt>null</tt>.  This is useful in determining the length of the
        * collection <i>only</i> if the caller knows that the collection does
        * not contain any <tt>null</tt> elements.)
        *
        * @param ao  the array into which the elements of the collection are
        *            to be stored, if it is big enough; otherwise, a new
        *            array of the same runtime type is allocated for this
        *            purpose
        *
        * @return an array containing the elements of the collection
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *           array is not a supertype of the runtime type of every
        *           element in this collection
        */
        public Object[] toArray(Object ao[])
            {
            InflatableMap map = InflatableMap.this;

            // create the array to store the map contents
            int c = map.size();
            if (ao == null)
                {
                ao = c == 0 ? NO_OBJECTS : new Object[c];
                }
            else if (ao.length < c)
                {
                // if it is not big enough, a new array of the same runtime
                // type is allocated
                ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), c);
                }
            else if (ao.length > c)
                {
                // if the collection fits in the specified array with room to
                // spare, the element in the array immediately following the
                // end of the collection is set to null
                ao[c] = null;
                }

            switch (map.m_nImpl)
                {
                case I_EMPTY:
                    break;

                case I_SINGLE:
                    ao[0] = map.m_oContents;
                    break;

                case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
                case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                    System.arraycopy((Map.Entry[]) m_oContents, 0, ao, 0, c);
                    break;

                case I_OTHER:
                    ao = ((Map) m_oContents).entrySet().toArray(ao);
                    break;

                default:
                    throw new IllegalStateException();
                }

            return ao;
            }
        }


    // ----- Externalizable interface ---------------------------------------

    /**
    * Initialize this object from the data in the passed ObjectInput stream.
    *
    * @param in  the stream to read data from in order to restore the object
    *
    * @exception IOException  if an I/O exception occurs
    */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException
        {
        if (!isEmpty())
            {
            throw new NotActiveException();
            }

        int c = in.readInt();
        switch (c)
            {
            case 0:
                break;

            case 1:
                m_nImpl = I_SINGLE;
                m_oContents = instantiateEntry((K) in.readObject(), (V) in.readObject());
                break;

            case 2: case 3: case 4: case 5: case 6: case 7: case 8:
                {
                Map.Entry<K, V>[] aEntry = new Map.Entry[THRESHOLD];
                for (int i = 0; i < c; ++i)
                    {
                    aEntry[i] = instantiateEntry((K) in.readObject(), (V) in.readObject());
                    }
                m_nImpl     = (byte) (I_ARRAY_1 + c - 1);
                m_oContents = aEntry;
                }
                break;

            default:
                {
                Map<K, V> map = instantiateMap();
                for (int i = 0; i < c; ++i)
                    {
                    map.put((K) in.readObject(), (V) in.readObject());
                    }
                m_nImpl     = I_OTHER;
                m_oContents = map;
                }
                break;
            }
        }

    /**
    * Write this object's data to the passed ObjectOutput stream.
    *
    * @param out  the stream to write the object to
    *
    * @exception IOException if an I/O exception occurs
    */
    public synchronized void writeExternal(ObjectOutput out)
            throws IOException
        {
        // format is int size followed by that many key/value pairs
        int nImpl = m_nImpl;
        switch (nImpl)
            {
            case I_EMPTY:
                out.writeInt(0);
                break;

            case I_SINGLE:
                {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) m_oContents;
                out.writeInt(1);
                out.writeObject(entry.getKey());
                out.writeObject(entry.getValue());
                }
                break;

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                Map.Entry<K, V>[] aEntry = (Map.Entry[]) m_oContents;
                int               c      = nImpl - I_ARRAY_1 + 1;
                out.writeInt(c);
                for (int i = 0; i < c; ++i)
                    {
                    Map.Entry<K, V> entry = aEntry[i];
                    out.writeObject(entry.getKey());
                    out.writeObject(entry.getValue());
                    }
                }
                break;

            case I_OTHER:
                {
                Map<K, V> map = ((Map<K, V>) m_oContents);
                int       c   = map.size();
                Map.Entry<K, V>[] aEntry = (Map.Entry[]) map.entrySet().toArray(new Map.Entry[c]);
                out.writeInt(c);
                for (int i = 0; i < c; ++i)
                    {
                    Map.Entry<K, V> entry = aEntry[i];
                    out.writeObject(entry.getKey());
                    out.writeObject(entry.getValue());
                    }
                }
                break;

            default:
                throw new IllegalStateException();
            }
        }

    // ----- internal methods -----------------------------------------------

    /**
    * (Factory pattern) Instantiate a Map Entry.
    * This method permits inheriting classes to easily override the
    * implementation of the Entry object.
    *
    * @param key    the key
    * @param value  the value
    *
    * @return an instance of a Map Entry
    */
    protected Map.Entry<K, V> instantiateEntry(K key, V value)
        {
        return new SimpleEntry<K, V>(key, value);
        }

    /**
    * (Factory pattern) Instantiate an Entry Set.
    * This method permits inheriting classes to easily override the
    * implementation of the EntrySet object.
    *
    * @return an instance of Entry Set
    */
    protected Set<Entry<K, V>> instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * (Factory pattern) Instantiate a Map object to store entries in once
    * the "lite" threshold has been exceeded. This method permits inheriting
    * classes to easily override the choice of the Map object.
    *
    * @return an instance of Map
    */
    protected Map<K, V> instantiateMap()
        {
        return new HashMap<>();
        }

    /**
    * Scan up to the first <tt>c</tt> elements of the passed Entry array
    * <tt>aEntry</tt> looking for the specified key <tt>key</tt>. If it is
    * found, return its position <tt>i</tt> in the array such that
    * <tt>(0 &lt;= i &lt; c)</tt>. If it is not found, return <tt>-1</tt>.
    *
    * @param aEntry  the array of objects to search
    * @param c       the number of Entry objects in the array to search
    * @param oKey    the key to look for
    *
    * @return the index of the object, if found; otherwise -1
    */
    private int indexOf(Map.Entry<K, V>[] aEntry, int c, Object oKey)
        {
        // first quick-scan by reference
        for (int i = 0; i < c; ++i)
            {
            if (oKey == aEntry[i].getKey())
                {
                return i;
                }
            }

        // slow scan by equals()
        if (oKey != null)
            {
            for (int i = 0; i < c; ++i)
                {
                if (oKey.equals(aEntry[i].getKey()))
                    {
                    return i;
                    }
                }
            }

        return -1;
        }

    /**
     * Return the specified exception as a RuntimeException, wrapping it if necessary.
     *
     * @param t  the exception
     *
     * @return the RuntimeException
     */
    protected RuntimeException ensureRuntimeException(Throwable t)
        {
        return t instanceof RuntimeException
                ? (RuntimeException) t
                : new RuntimeException(t);
        }

    /**
    * After a mutation operation has reduced the size of an underlying Map,
    * check if the delegation model should be replaced with a more size-
    * efficient storage approach, and switch accordingly.
    */
    protected void checkShrinkFromOther()
        {
        assert m_nImpl == I_OTHER;

        // check if the Map is now significantly below the "lite"
        // threshold
        Map<K, V> map = (Map) m_oContents;
        int c = map.size();
        switch (c)
            {
            case 0:
                m_nImpl     = I_EMPTY;
                m_oContents = null;
                break;

            case 1:
                {
                Map.Entry<K, V> entry = (Map.Entry) map.entrySet().toArray()[0];
                m_oContents = instantiateEntry(entry.getKey(), entry.getValue());
                m_nImpl     = I_SINGLE;
                }
                break;

            case 2: case 3: case 4:
                {
                // shrink to "Entry[]" implementation
                Map.Entry<K, V>[] aEntry = new Map.Entry[THRESHOLD];
                int i = 0;
                for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                    {
                    Map.Entry<K, V> entry = (Map.Entry) iter.next();
                    aEntry[i++] = instantiateEntry(entry.getKey(), entry.getValue());
                    }
                assert i == c;

                m_nImpl     = (byte) (I_ARRAY_1 + i - 1);
                m_oContents = aEntry;
                }
                break;
            }
        }


    // ----- inner class: EntryIterator -------------------------------------

    /**
    * A simple Iterator for InflatableMap Entry objects. This class is static in
    * order to allow the EntrySet to be quickly garbage-collected.
    */
    public static class EntryIterator<K, V>
            implements Iterator<Entry<K, V>>
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct an EntryIterator.
        *
        * @param map     the InflatableMap to delegate <tt>remove()</tt> calls to
        * @param aEntry  the array of Map Entry objects to iterate
        */
        public EntryIterator(InflatableMap<K, V> map, Map.Entry<K, V>[] aEntry)
            {
            m_map    = map;
            m_aEntry = aEntry;
            }

        // ----- Iterator interface -------------------------------------

        /**
        * Returns <tt>true</tt> if the iteration has more elements.
        * (In other words, returns <tt>true</tt> if <tt>next</tt>
        * would return an element rather than throwing an exception.)
        *
        * @return <tt>true</tt> if the iterator has more elements
        */
        public boolean hasNext()
            {
            return m_iPrev + 1 < m_aEntry.length;
            }

        /**
        * Returns the next element in the iteration.
        *
        * @return the next element in the iteration
        *
        * @exception NoSuchElementException iteration has no more
        *            elements
        */
        public Entry<K, V> next()
            {
            int iNext = m_iPrev + 1;
            if (iNext < m_aEntry.length)
                {
                m_iPrev      = iNext;
                m_fCanRemove = true;
                return m_aEntry[iNext];
                }
            else
                {
                throw new NoSuchElementException();
                }
            }

        /**
        * Removes from the underlying set the last element
        * returned by the iterator.  This method can be called only once
        * per call to <tt>next</tt>.  The behavior of an iterator is
        * unspecified if the underlying set is modified while the
        * iteration is in progress in any way other than by calling this
        * method.
        *
        * @exception IllegalStateException if the <tt>next</tt> method
        *            has not yet been called, or the <tt>remove</tt>
        *            method has already been called after the last call
        *            to the <tt>next</tt> method
        */
        public void remove()
            {
            if (m_fCanRemove)
                {
                m_fCanRemove = false;
                m_map.remove(m_aEntry[m_iPrev].getKey());
                }
            else
                {
                throw new IllegalStateException();
                }
            }

        // ----- data members -------------------------------------------

        /**
        * The InflatableMap. Having this field allows the EntrySet to be
        * quickly collected by a GC, because the EntryIterator does not
        * have to maintain a reference to it in order to get to the InflatableMap.
        */
        Map<K, V>      m_map;

        /**
        * The entries to iterate.
        */
        Map.Entry<K, V>[] m_aEntry;

        /**
        * The previous index iterated.
        */
        int         m_iPrev      = -1;

        /**
        * True if the previously iterated element can be removed.
        */
        boolean     m_fCanRemove = false;
        }


    // ----- constants ------------------------------------------------------

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -4252475545246687924L;

    /**
    * A constant array of zero size.  (This saves having to allocate what
    * should be a constant.)
    */
    private static final Object[] NO_OBJECTS = new Object[0];

    /**
    * The default point above which the InflatableMap delegates to another Map
    * implementation.
    */
    protected static final int THRESHOLD = 8;

    /**
    * Implementation:  Empty Map.
    */
    private static final int I_EMPTY = 0;
    /**
    * Implementation:  Single-item Map.
    */
    private static final int I_SINGLE = 1;
    /**
    * Implementation:  Array Map of 1 item.
    */
    private static final int I_ARRAY_1 = 2;
    /**
    * Implementation:  Array Map of 2 items.
    */
    private static final int I_ARRAY_2 = 3;
    /**
    * Implementation:  Array Map of 3 items.
    */
    private static final int I_ARRAY_3 = 4;
    /**
    * Implementation:  Array Map of 4 items.
    */
    private static final int I_ARRAY_4 = 5;
    /**
    * Implementation:  Array Map of 5 items.
    */
    private static final int I_ARRAY_5 = 6;
    /**
    * Implementation:  Array Map of 6 items.
    */
    private static final int I_ARRAY_6 = 7;
    /**
    * Implementation:  Array Map of 7 items.
    */
    private static final int I_ARRAY_7 = 8;
    /**
    * Implementation:  Array Map of 8 items.
    */
    private static final int I_ARRAY_8 = 9;
    /**
    * Implementation:  Delegation.
    */
    protected static final int I_OTHER = 10;

    /**
     * An empty iterator.
     */
    private static final Iterator NULL_ITERATOR = new Iterator()
        {
        @Override
        public boolean hasNext()
            {
            return false;
            }

        @Override
        public Object next()
            {
            throw new NoSuchElementException();
            }
        };

    // ----- data members ---------------------------------------------------

    /**
    * Implementation, one of I_EMPTY, I_SINGLE, I_ARRAY_* or I_OTHER.
    */
    protected byte m_nImpl;

    /**
    * The Map contents, based on the implementation being used.
    */
    protected Object m_oContents;
    }
