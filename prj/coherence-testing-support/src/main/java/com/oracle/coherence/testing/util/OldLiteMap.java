/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.util;


import com.tangosol.util.Base;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleMapEntry;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;


/**
* An implementation of java.util.Map that is optimal (in terms of both size
* and speed) for very small sets of data but still works excellently with
* large sets of data.  This implementation is not thread-safe.
* <p>
* The LiteMap implementation switches at runtime between several different
* sub-implementations for storing the map of objects, described here:
* <p>
* <ol>
* <li>  "empty map" - a map that contains no data;
* <li>  "single entry" - a to a single map entry
* <li>  "Object[]" - a reference to an array of entries; the item limit for
*       this implementation is determined by the THRESHOLD constant;
* <li>  "delegation" - for more than THRESHOLD items, a map is created to
*       delegate the map management to; sub-classes can override the default
*       delegation class (java.util.HashMap) by overriding the factory method
*       instantiateMap.
* </ol>
* <p>
* The LiteMap implementation supports the null key value.
*
* @author cp 06/29/99
*/
public class OldLiteMap
        extends AbstractMap
        implements Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a LiteMap.
    */
    public OldLiteMap()
        {
        }

    /**
    * Construct a LiteMap with the same mappings as the given map.
    *
    * @param map the map whose mappings are to be placed in this map.
    */
    public OldLiteMap(Map map)
        {
        putAll(map);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Returns the number of key-value mappings in this map.  If the
    * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
    * <tt>Integer.MAX_VALUE</tt>.
    *
    * @return the number of key-value mappings in this map
    */
    public int size()
        {
        switch (m_nImpl)
            {
            case OldLiteMap.I_EMPTY:
                return 0;

            case OldLiteMap.I_SINGLE:
                return 1;

            case OldLiteMap.I_ARRAY:
                return ((Entry[]) m_oContents).length;

            case OldLiteMap.I_OTHER:
                return ((Map) m_oContents).size();

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns <tt>true</tt> if this map contains a mapping for the specified
    * key.
    *
    * @param key key whose presence in this map is to be tested
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key
    */
    public boolean containsKey(Object key)
        {
        switch (m_nImpl)
            {
            case OldLiteMap.I_EMPTY:
                return false;

            case OldLiteMap.I_SINGLE:
                {
                Object keyEntry = ((Entry) m_oContents).getKey();
                return (key == null ? keyEntry == null : key.equals(keyEntry));
                }

            case OldLiteMap.I_ARRAY:
                {
                // "Object[]" implementation
                Entry[] aEntry = (Entry[]) m_oContents;
                int cEntries = aEntry.length;
                for (int i = 0; i < cEntries; ++i)
                    {
                    Object keyEntry = aEntry[i].getKey();
                    if (key == null ? keyEntry == null : key.equals(keyEntry))
                        {
                        return true;
                        }
                    }
                }
                return false;

            case OldLiteMap.I_OTHER:
                return ((Map) m_oContents).containsKey(key);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns the value to which this map maps the specified key.  Returns
    * <tt>null</tt> if the map contains no mapping for this key.  A return
    * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
    * map contains no mapping for the key; it's also possible that the map
    * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
    * operation may be used to distinguish these two cases.
    *
    * @param key key whose associated value is to be returned
    *
    * @return the value to which this map maps the specified key, or
    *         <tt>null</tt> if the map contains no mapping for this key
    */
    public Object get(Object key)
        {
        switch (m_nImpl)
            {
            case OldLiteMap.I_EMPTY:
                return null;

            case OldLiteMap.I_SINGLE:
                {
                Entry entry    = (Entry) m_oContents;
                Object    keyEntry = entry.getKey();
                if (key == null ? keyEntry == null : key.equals(keyEntry))
                    {
                    return entry.getValue();
                    }
                }
                return null;

            case OldLiteMap.I_ARRAY:
                {
                // "Object[]" implementation
                Entry[] aEntry = (Entry[]) m_oContents;
                int cEntries = aEntry.length;
                for (int i = 0; i < cEntries; ++i)
                    {
                    Object keyEntry = aEntry[i].getKey();
                    if (key == null ? keyEntry == null : key.equals(keyEntry))
                        {
                        return aEntry[i].getValue();
                        }
                    }
                }
                return null;

            case OldLiteMap.I_OTHER:
                return ((Map) m_oContents).get(key);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Associates the specified value with the specified key in this map.
    * If the map previously contained a mapping for this key, the old
    * value is replaced.
    *
    * @param key key with which the specified value is to be associated
    *
    * @param value value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *	       if there was no mapping for key.  A <tt>null</tt> return can
    *	       also indicate that the map previously associated <tt>null</tt>
    *	       with the specified key, if the implementation supports
    *	       <tt>null</tt> values
    */
    public Object put(Object key, Object value)
        {
        switch (m_nImpl)
            {
            case OldLiteMap.I_EMPTY:
                m_nImpl     = OldLiteMap.I_SINGLE;
                m_oContents = instantiateEntry(key, value);
                return null;

            case OldLiteMap.I_SINGLE:
                {
                Entry entry    = (Entry) m_oContents;
                Object    keyEntry = entry.getKey();
                if (key == null ? keyEntry == null : key.equals(keyEntry))
                    {
                    Object oPrev = entry.getValue();
                    entry.setValue(value);
                    return oPrev;
                    }

                // grow to array implementation
                Entry[] aEntry = new Entry[2];
                aEntry[0] = entry;
                aEntry[1] = instantiateEntry(key, value);

                m_nImpl     = OldLiteMap.I_ARRAY;
                m_oContents = aEntry;
                return null;
                }

            case OldLiteMap.I_ARRAY:
                {
                // "Object[]" implementation
                Entry[] aEntry = (Entry[]) m_oContents;
                int cEntries = aEntry.length;
                for (int i = 0; i < cEntries; ++i)
                    {
                    Entry entry    = aEntry[i];
                    Object    keyEntry = entry.getKey();
                    if (key == null ? keyEntry == null : key.equals(keyEntry))
                        {
                        Object oPrev = entry.getValue();
                        entry.setValue(value);
                        return oPrev;
                        }
                    }

                // check if adding the object exceeds the "lite" threshold
                if (cEntries >= OldLiteMap.THRESHOLD)
                    {
                    // time to switch to a different map implementation
                    Map map = instantiateMap();
                    // map.entrySet().addAll(entrySet());
                    for (int i = 0; i < cEntries; ++i)
                        {
                        Entry entry = aEntry[i];
                        map.put(entry.getKey(), entry.getValue());
                        }
                    map.put(key, value);

                    m_nImpl     = OldLiteMap.I_OTHER;
                    m_oContents = map;
                    }
                else
                    {
                    // grow the array
                    Entry[] aNew = new Entry[cEntries+1];
                    System.arraycopy(aEntry, 0, aNew, 0, cEntries);
                    aNew[cEntries] = instantiateEntry(key, value);
                    m_oContents = aNew;
                    }
                return null;
                }

            case OldLiteMap.I_OTHER:
                return ((Map) m_oContents).put(key, value);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Removes the mapping for this key from this map if present.
    *
    * @param key key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *	       if there was no mapping for key.  A <tt>null</tt> return can
    *	       also indicate that the map previously associated <tt>null</tt>
    *	       with the specified key, if the implementation supports
    *	       <tt>null</tt> values
    */
    public Object remove(Object key)
        {
        switch (m_nImpl)
            {
            case OldLiteMap.I_EMPTY:
                return null;

            case OldLiteMap.I_SINGLE:
                {
                Entry entry = (Entry) m_oContents;
                Object keyEntry = entry.getKey();
                if (key == null ? keyEntry == null : key.equals(keyEntry))
                    {
                    // shrink to empty
                    Object oPrev = entry.getValue();
                    m_nImpl      = OldLiteMap.I_EMPTY;
                    m_oContents  = null;
                    return oPrev;
                    }
                return null;
                }

            case OldLiteMap.I_ARRAY:
                {
                Entry[] aEntry = (Entry[]) m_oContents;
                int cEntries = aEntry.length;

                // find the key
                for (int i = 0; i < cEntries; ++i)
                    {
                    Entry entry    = aEntry[i];
                    Object    keyEntry = entry.getKey();
                    if (key == null ? keyEntry == null : key.equals(keyEntry))
                        {
                        if (cEntries == 2)
                            {
                            // shrink to "single entry" implementation
                            m_nImpl     = OldLiteMap.I_SINGLE;
                            m_oContents = aEntry[i^1];
                            }
                        else
                            {
                            // shrink the array
                            Entry[] aNew = new Entry[cEntries-1];
                            System.arraycopy(aEntry, 0, aNew, 0, i);
                            System.arraycopy(aEntry, i + 1, aNew, i, cEntries - i - 1);
                            m_oContents = aNew;
                            }
                        return entry.getValue();
                        }
                    }
                return null;
                }

            case OldLiteMap.I_OTHER:
                {
                Map    map   = (Map) m_oContents;
                Object oPrev = map.remove(key);
                int cEntries = map.size();

                // check if the map is now below the "lite" threshold
                if (cEntries < OldLiteMap.THRESHOLD)
                    {
                    // shrink to "Object[]" implementation
                    m_nImpl     = OldLiteMap.I_ARRAY;
                    m_oContents = map.entrySet().toArray(new Entry[cEntries]);
                    }

                return oPrev;
                }

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Removes all mappings from this map.
    */
    public void clear()
        {
        m_nImpl     = OldLiteMap.I_EMPTY;
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
        try
            {
            OldLiteMap that = (OldLiteMap) super.clone();

            switch (this.m_nImpl)
                {
                case OldLiteMap.I_EMPTY:
                    // nothing to do
                    break;

                case OldLiteMap.I_SINGLE:
                    that.m_oContents = ((SimpleMapEntry) this.m_oContents).clone();
                    break;

                case OldLiteMap.I_ARRAY:
                    Entry[] aEntry = (Entry[]) ((Object[]) this.m_oContents).clone();
                    for (int i = 0, c = aEntry.length; i < c; ++i)
                        {
                        aEntry[i] = (SimpleMapEntry) ((SimpleMapEntry) aEntry[i]).clone();
                        }
                    that.m_oContents = aEntry;
                    break;

                case OldLiteMap.I_OTHER:
                    Map mapThis = (Map) this.m_oContents;
                    Map mapThat = that.instantiateMap();
                    mapThat.putAll(mapThis);
                    that.m_oContents = mapThat;
                    break;

                default:
                    throw new IllegalStateException();
                }

            that.m_set = null;

            return that;
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * Returns a set view of the mappings contained in this map.  Each element
    * in the returned set is a <tt>Map.Entry</tt>.  The set is backed by the
    * map, so changes to the map are reflected in the set, and vice-versa.
    * If the map is modified while an iteration over the set is in progress,
    * the results of the iteration are undefined.  The set supports element
    * removal, which removes the corresponding mapping from the map, via the
    * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
    * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not support
    * the <tt>add</tt> or <tt>addAll</tt> operations.
    *
    * @return a set view of the mappings contained in this map.
    */
    public Set entrySet()
        {
        OldLiteMap.EntrySet set = m_set;
        if (set == null)
            {
            m_set = set = instantiateEntrySet();
            }
        return set;
        }

    /**
    * A set of entries backed by this map.
    */
    protected class EntrySet
            extends AbstractSet
            implements Serializable
        {
        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection.
        */
        public Iterator iterator()
            {
            if (OldLiteMap.this.isEmpty())
                {
                return NullImplementation.getIterator();
                }

            return new Iterator()
                {
                /**
                * Returns <tt>true</tt> if the iteration has more elements. (In other
                * words, returns <tt>true</tt> if <tt>next</tt> would return an element
                * rather than throwing an exception.)
                *
                * @return <tt>true</tt> if the iterator has more elements
                */
                public boolean hasNext()
                    {
                    return (iPrev + 1 < aEntry.length);
                    }

                /**
                * Returns the next element in the iteration.
                *
                * @return the next element in the iteration
                *
                * @exception java.util.NoSuchElementException iteration has no more elements
                */
                public Object next()
                    {
                    if (iPrev + 1 >= aEntry.length)
                        {
                        throw new NoSuchElementException();
                        }

                    fCanRemove = true;
                    return aEntry[++iPrev];
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
                *		 has not yet been called, or the <tt>remove</tt>
                *		 method has already been called after the last call
                *                to the <tt>next</tt> method
                */
                public void remove()
                    {
                    if (!fCanRemove)
                        {
                        throw new IllegalStateException();
                        }

                    fCanRemove = false;
                    OldLiteMap.this.remove(aEntry[iPrev].getKey());
                    }

                Entry[] aEntry     = (Entry[]) toArray(new Entry[OldLiteMap.this.size()]);
                int         iPrev      = -1;
                boolean     fCanRemove = false;
                };
            }

        /**
        * Returns the number of elements in this collection.  If the collection
        * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
        * <tt>Integer.MAX_VALUE</tt>.
        *
        * @return the number of elements in this collection.
        */
        public int size()
            {
            return OldLiteMap.this.size();
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.<p>
        *
        * @param o object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified element
        */
        public boolean contains(Object o)
            {
            if (o instanceof Entry)
                {
                Map       map   = OldLiteMap.this;
                Entry entry = (Entry) o;
                Object    key   = entry.getKey();
                if (map.containsKey(key))
                    {
                    Object oThis = map.get(key);
                    Object oThat = entry.getValue();
                    return (oThat == null ? oThis == null : oThat.equals(oThis));
                    }
                }

            return false;
            }

        /**
        * Returns an array containing all of the elements in this collection.  If
        * the collection makes any guarantees as to what order its elements are
        * returned by its iterator, this method must return the elements in the
        * same order.  The returned array will be "safe" in that no references to
        * it are maintained by the collection.  (In other words, this method must
        * allocate a new array even if the collection is backed by an Array).
        * The caller is thus free to modify the returned array.<p>
        *
        * @return an array containing all of the elements in this collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array and
        * that contains all of the elements in this collection.  If the
        * collection fits in the specified array, it is returned therein.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.<p>
        *
        * If the collection fits in the specified array with room to spare (i.e.,
        * the array has more elements than the collection), the element in the
        * array immediately following the end of the collection is set to
        * <tt>null</tt>.  This is useful in determining the length of the
        * collection <i>only</i> if the caller knows that the collection does
        * not contain any <tt>null</tt> elements.)<p>
        *
        * @param  a  the array into which the elements of the collection are to
        * 	     be stored, if it is big enough; otherwise, a new array of the
        * 	     same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the collection
        *
        * @throws ArrayStoreException if the runtime type of the specified array
        *         is not a supertype of the runtime type of every element in this
        *         collection
        */
        public Object[] toArray(Object a[])
            {
            OldLiteMap map = OldLiteMap.this;

            // create the array to store the map contents
            int c = map.size();
            if (a == null)
                {
                a = new Entry[c];
                }
            else if (a.length < c)
                {
                // if it is not big enough, a new array of the same runtime
                // type is allocated
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), c);
                }
            else if (a.length > c)
                {
                // if the collection fits in the specified array with room to
                // spare, the element in the array immediately following the
                // end of the collection is set to null
                a[c] = null;
                }

            switch (map.m_nImpl)
                {
                case OldLiteMap.I_EMPTY:
                    break;

                case OldLiteMap.I_SINGLE:
                    a[0] = map.m_oContents;
                    break;

                case OldLiteMap.I_ARRAY:
                    System.arraycopy((Entry[]) m_oContents, 0, a, 0, c);
                    break;

                case OldLiteMap.I_OTHER:
                    a = ((Map) m_oContents).entrySet().toArray(a);
                    break;

                default:
                    throw new IllegalStateException();
                }

            return a;
            }

        /**
        * Ensures that this collection contains the specified element (optional
        * operation).  Returns <tt>true</tt> if the collection changed as a
        * result of the call.  (Returns <tt>false</tt> if this collection does
        * not permit duplicates and already contains the specified element.)
        * Collections that support this operation may place limitations on what
        * elements may be added to the collection.  In particular, some
        * collections will refuse to add <tt>null</tt> elements, and others will
        * impose restrictions on the type of elements that may be added.
        * Collection classes should clearly specify in their documentation any
        * restrictions on what elements may be added.<p>
        *
        * @param o element whose presence in this collection is to be ensured
        *
        * @return <tt>true</tt> if the collection changed as a result of the call
        *
        * @throws ClassCastException if the class of the specified element
        * 		  prevents it from being added to this collection.
        */
        public boolean add(Object o)
            {
            Entry entry = (Entry) o;
            if (contains(entry))
                {
                return false;
                }

            OldLiteMap.this.put(entry.getKey(), entry.getValue());
            return true;
            }
        }


    // ----- Serializable interface -----------------------------------------

    /**
    * Write this object to an ObjectOutputStream.
    *
    * @param out  the ObjectOutputStream to write this object to
    *
    * @throws java.io.IOException  thrown if an exception occurs writing this object
    */
    private synchronized void writeObject(ObjectOutputStream out)
            throws IOException
        {
        out.writeInt(m_nImpl);
        out.writeObject(m_oContents);
        }

    /**
    * Read this object from an ObjectInputStream.
    *
    * @param in  the ObjectInputStream to read this object from
    *
    * @throws java.io.IOException  if an exception occurs reading this object
    * @throws ClassNotFoundException  if the class for an object being
    *         read cannot be found
    */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        m_nImpl     = in.readInt();
        m_oContents = in.readObject();
        }


    // ----- inner methods --------------------------------------------------

    /**
    * (Factory pattern) Instantiate a SimpleMapEntry or subclass thereof.
    * This method permits inheriting classes to easily override the
    * implementation of the Entry object.
    *
    * @return an instance of SimpleMapEntry
    */
    protected SimpleMapEntry instantiateEntry(Object key, Object value)
        {
        return new SimpleMapEntry(key, value);
        }

    /**
    * (Factory pattern) Instantiate an Entry Set or subclass thereof.
    * This method permits inheriting classes to easily override the
    * implementation of the EntrySet object.
    *
    * @return an instance of EntrySet
    */
    protected OldLiteMap.EntrySet instantiateEntrySet()
        {
        return new OldLiteMap.EntrySet();
        }

    /**
    * (Factory pattern) Instantiate a Map object to store entries in once
    * the "lite" threshold has been exceeded. This method permits inheriting
    * classes to easily override the choice of the Map object.
    *
    * @return an instance of Map
    */
    protected Map instantiateMap()
        {
        return new HashMap();
        }


    // ----- constants ------------------------------------------------------

    /**
    * A constant array of zero size.  (This saves having to allocate what
    * should be a constant.)
    */
    private static final Object[] NO_OBJECTS = new Object[0];

    /**
    * The default point above which the LiteMap delegates to another map
    * implementation.
    */
    private static final int THRESHOLD = 7;

    /**
    * Implementation:  Empty map.
    */
    private static final int I_EMPTY = 0;
    /**
    * Implementation:  Single-entry map.
    */
    private static final int I_SINGLE = 1;
    /**
    * Implementation:  Array map.
    */
    private static final int I_ARRAY = 2;
    /**
    * Implementation:  Delegation.
    */
    private static final int I_OTHER = 3;


    // ----- data members ---------------------------------------------------

    /**
    * Implementation. One of I_EMPTY, I_SINGLE, I_ARRAY or I_OTHER.
    */
    private int m_nImpl;

    /**
    * The map contents, based on the implementation being used.
    */
    private Object m_oContents;

    /**
    * Cached entry set (lazy instantiator).
    */
    private transient OldLiteMap.EntrySet m_set;
    }
