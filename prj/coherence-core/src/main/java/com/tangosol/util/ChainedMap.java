/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.lang.reflect.Array;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.AbstractMap;
import java.util.AbstractSet;


/**
* Chains two maps into one virtual map.
* <p>
* The contents of the first map take precedence over the contents in the
* second map. If there are already entries in the two maps when this map
* is constructed, then the keys in the second map that are also in the first
* map will be removed from the second map.
*
* @since Coherence 2.4
* @author cp 2004.03.24
*/
public class ChainedMap
        extends AbstractMap
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ChainedMap out of two maps.
    *
    * @param mapFirst  the first map whose mappings are to be placed in this map.
    * @param mapSecond the second map whose mappings are to be placed in this map.
    */
    public ChainedMap(Map mapFirst, Map mapSecond)
        {
        m_mapFirst  = mapFirst;
        m_mapSecond = mapSecond;

        // avoid duplicates
        if (!mapFirst.isEmpty() && !mapSecond.isEmpty())
            {
            mapSecond.keySet().removeAll(mapFirst.keySet());
            }
        }

    
    // ----- accessors ------------------------------------------------------
    
    /**
    * Get the first (front) map.
    * <p>
    * <b>Note: direct modifications of the returned map may cause
    *  an unpredictable behavior of the ChainedMap.</b>
    *
    * @return the first Map
    */
    public Map getFirstMap()
        {
        return m_mapFirst;
        }

    /**
    * Get the second (back) map.
    * <p>
    * <b>Note: direct modifications of the returned map may cause
    *  an unpredictable behavior of the ChainedMap.</b>
    *
    * @return the second Map
    */
    public Map getSecondMap()
        {
        return m_mapSecond;
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
        return m_mapFirst.size() + m_mapSecond.size();
        }

    /**
    * Returns <tt>true</tt> if this map contains a mapping for the specified
    * key.
    * 
    * @param oKey  key whose presence in this map is to be tested
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key
    */
    public boolean containsKey(Object oKey)
        {
        return m_mapFirst.containsKey(oKey) || m_mapSecond.containsKey(oKey);
        }

    /**
    * Returns the value to which this map maps the specified key.  Returns
    * <tt>null</tt> if the map contains no mapping for this key.  A return
    * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
    * map contains no mapping for the key; it's also possible that the map
    * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
    * operation may be used to distinguish these two cases.
    *
    * @param oKey  key whose associated value is to be returned
    *
    * @return the value to which this map maps the specified key, or
    *         <tt>null</tt> if the map contains no mapping for this key
    */
    public Object get(Object oKey)
        {
        Object oValue = null;

        Map mapFirst  = m_mapFirst;
        Map mapSecond = m_mapSecond;
        if (mapFirst.containsKey(oKey))
            {
            oValue = mapFirst.get(oKey);
            }
        else if (mapSecond.containsKey(oKey))
            {
            oValue = mapSecond.get(oKey);
            }

        return oValue;
        }

    /**
    * Associates the specified value with the specified key in this map.
    * If the map previously contained a mapping for this key, the old
    * value is replaced.
    *
    * @param oKey    key with which the specified value is to be associated
    * @param oValue  value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values
    */
    public Object put(Object oKey, Object oValue)
        {
        Map mapFirst  = m_mapFirst;
        if (mapFirst.containsKey(oKey))
            {
            return mapFirst.put(oKey, oValue);
            }

        Map mapSecond = m_mapSecond;
        if (mapSecond.containsKey(oKey))
            {
            return mapSecond.put(oKey, oValue);
            }

        return mapFirst.put(oKey, oValue);
        }

    /**
    * Removes the mapping for this key from this map if present.
    *
    * @param oKey  key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values
    */
    public Object remove(Object oKey)
        {
        Object oValue = null;

        Map mapSecond = m_mapSecond;
        if (mapSecond.containsKey(oKey))
            {
            oValue = mapSecond.remove(oKey);
            }

        Map mapFirst  = m_mapFirst;
        if (mapFirst.containsKey(oKey))
            {
            oValue = mapFirst.remove(oKey);
            }

        return oValue;
        }

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
        EntrySet setEntries = m_setEntries;
        if (setEntries == null)
            {
            m_setEntries = setEntries = instantiateEntrySet();
            }
        return setEntries;
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * (Factory pattern) Instantiate an Entry Set or subclass thereof.
    * This method permits inheriting classes to easily override the
    * implementation of the EntrySet object.
    *
    * @return an instance of EntrySet
    */
    protected EntrySet instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries backed by this map.
    */
    public class EntrySet
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
            return new ChainedEnumerator(m_mapFirst .entrySet().iterator(),
                                         m_mapSecond.entrySet().iterator());
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
            return ChainedMap.this.size();
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.<p>
        *
        * @param o  object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified element
        */
        public boolean contains(Object o)
            {
            if (o instanceof Map.Entry)
                {
                Map       map   = ChainedMap.this;
                Map.Entry entry = (Map.Entry) o;
                Object    oKey  = entry.getKey();
                if (map.containsKey(oKey))
                    {
                    Object oThis = map.get(oKey);
                    Object oThat = entry.getValue();
                    return oThat == null ? oThis == null
                                         : (oThat == oThis || oThat.equals(oThis));
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
            Object[] aoResult;

            Map mapFirst  = m_mapFirst;
            Map mapSecond = m_mapSecond;
            if (mapFirst.isEmpty())
                {
                aoResult = mapSecond.entrySet().toArray();
                }
            else if (mapSecond.isEmpty())
                {
                aoResult = mapFirst.entrySet().toArray();
                }
            else
                {
                Object[] aoFirst  = mapFirst.entrySet().toArray();
                int      coFirst  = aoFirst.length;
                Object[] aoSecond = mapSecond.entrySet().toArray();
                int      coSecond = aoSecond.length;
                int      coResult = coFirst + coSecond;

                aoResult = new Object[coResult];
                System.arraycopy(aoFirst, 0, aoResult, 0, coFirst);
                System.arraycopy(aoSecond, 0, aoResult, coFirst, coSecond);
                }

            return aoResult;
            }

        /**
        * Returns an array with a runtime type is that of the specified
        * array and that contains all of the elements in this collection.
        * If the collection fits in the specified array, it is returned
        * therein. Otherwise, a new array is allocated with the runtime
        * type of the specified array and the size of this collection.
        * <p>
        * If the collection fits in the specified array with room to spare
        * (i.e. the array has more elements than the collection), the element
        * in the array immediately following the end of the collection is set
        * to <tt>null</tt>.  This is useful in determining the length of the
        * collection <i>only</i> if the caller knows that the collection does
        * not contain any <tt>null</tt> elements.)
        *
        * @param  aoDest  the array into which the elements of the collection
        *                 are to be stored, if it is big enough; otherwise, a
        *                 new array of the same runtime type is allocated for
        *                 this purpose
        *
        * @return an array containing the elements of the collection
        * 
        * @throws ArrayStoreException if the runtime type of the specified
        *           array is not a supertype of the runtime type of every
        *           element in this collection
        */
        public Object[] toArray(Object aoDest[])
            {
            Object[] aoResult;

            Map mapFirst  = m_mapFirst;
            Map mapSecond = m_mapSecond;
            if (mapFirst.isEmpty())
                {
                aoResult = mapSecond.entrySet().toArray(aoDest);
                }
            else if (mapSecond.isEmpty())
                {
                aoResult = mapFirst.entrySet().toArray(aoDest);
                }
            else
                {
                Object[] aoFirst  = mapFirst.entrySet().toArray();
                int      coFirst  = aoFirst.length;
                Object[] aoSecond = mapSecond.entrySet().toArray();
                int      coSecond = aoSecond.length;
                int      coResult = coFirst + coSecond;
                int      coDest   = aoDest.length;
                if (coDest < coResult)
                    {
                    // if it is not big enough, a new array of the same runtime
                    // type is allocated
                    aoResult = (Object[]) Array.newInstance(aoDest.getClass().getComponentType(), coResult);
                    }
                else if (coDest > coResult)
                    {
                    // if the collection fits in the specified array with room to
                    // spare, the element in the array immediately following the
                    // end of the collection is set to null
                    aoResult = aoDest;
                    aoResult[coResult] = null;
                    }
                else
                    {
                    // perfect fit
                    aoResult = aoDest;
                    }

                // merge the array contents from the individual maps
                System.arraycopy(aoFirst, 0, aoResult, 0, coFirst);
                System.arraycopy(aoSecond, 0, aoResult, coFirst, coSecond);
                }

            return aoResult;
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
        * restrictions on what elements may be added.
        *
        * @param o element whose presence in this collection is to be ensured
        *
        * @return <tt>true</tt> if the collection changed as a result of the call
        * 
        * @throws ClassCastException if the class of the specified element
        *         prevents it from being added to this collection.
        */
        public boolean add(Object o)
            {
            Map.Entry entry = (Map.Entry) o;
            if (contains(entry))
                {
                return false;
                }

            ChainedMap.this.put(entry.getKey(), entry.getValue());
            return true;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The first map.
    */
    protected Map m_mapFirst;

    /**
    * The second map.
    */
    protected Map m_mapSecond;

    /**
    * The entry set.
    */
    protected transient EntrySet m_setEntries;
    }
