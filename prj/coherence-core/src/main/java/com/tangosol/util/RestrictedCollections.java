/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.lang.reflect.Array;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;


/**
* A collection of Collection implementation classes that limit the data type.
*
* @author cp  2001.10.09
*/
public class RestrictedCollections
        extends Base
    {
    /**
    * Default constructor.
    */
    private RestrictedCollections()
        {
        }


    // ----- factory methods ------------------------------------------------

    /**
    * Returns a restricted instance of Collection.
    *
    * @param col  the underlying Collection
    * @param clz  the class of objects that may be stored in the Collection
    *
    * @return a restricted Collection that requires its contents to be
    *         of the specified class
    */
    public static Collection getCollection(Collection col, Class clz)
        {
        return new RestrictedCollection(col, clz);
        }

    /**
    * Returns a restricted instance of Set.
    *
    * @param set  the underlying Set
    * @param clz  the class of objects that may be stored in the Set
    *
    * @return a restricted Set that requires its contents to be
    *         of the specified class
    */
    public static Set getSet(Set set, Class clz)
        {
        return new RestrictedSet(set, clz);
        }

    /**
    * Returns a restricted instance of SortedSet.
    *
    * @param set  the underlying SortedSet
    * @param clz  the class of objects that may be stored in the SortedSet
    *
    * @return a restricted SortedSet that requires its contents to be
    *         of the specified class
    */
    public static SortedSet getSortedSet(SortedSet set, Class clz)
        {
        return new RestrictedSortedSet(set, clz);
        }

    /**
    * Returns a restricted instance of List.
    *
    * @param list  the underlying List
    * @param clz   the class of objects that may be stored in the List
    *
    * @return a restricted List that requires its contents to be
    *         of the specified class
    */
    public static List getList(List list, Class clz)
        {
        return new RestrictedList(list, clz);
        }

    /**
    * Returns a restricted instance of ListIterator.
    *
    * @param iter  the underlying ListIterator
    * @param clz   the class of objects that may be stored in the List
    *
    * @return a restricted ListIterator that requires its contents to be
    *         of the specified class
    */
    public static ListIterator getListIterator(ListIterator iter, Class clz)
        {
        return new RestrictedListIterator(iter, clz);
        }

    /**
    * Returns a restricted instance of Map.
    *
    * @param map     the underlying Map
    * @param clzKey  the class of keys that may be stored in the Map
    * @param clzVal  the class of values that may be stored in the Map
    *
    * @return a restricted Map that requires its keys and values to be
    *         of the specified classes
    */
    public static Map getMap(Map map, Class clzKey, Class clzVal)
        {
        return new RestrictedMap(map, clzKey, clzVal);
        }

    /**
    * Returns a restricted instance of SortedMap.
    *
    * @param map     the underlying SortedMap
    * @param clzKey  the class of keys that may be stored in the SortedMap
    * @param clzVal  the class of values that may be stored in the SortedMap
    *
    * @return a restricted SortedMap that requires its keys and values to be
    *         of the specified classes
    */
    public static SortedMap getSortedMap(SortedMap map, Class clzKey, Class clzVal)
        {
        return new RestrictedSortedMap(map, clzKey, clzVal);
        }

    /**
    * Returns a restricted instance of Set that holds Entry objects for a
    * RestrictedMap.
    *
    * @param set     the underlying Entry Set
    * @param clzKey  the class of keys that may be stored in the Map
    * @param clzVal  the class of values that may be stored in the Map
    *
    * @return a restricted Set that requires its contents to be Entry
    *         objects with the specified key and value restrictions
    */
    public static Set getEntrySet(Set set, Class clzKey, Class clzVal)
        {
        return new RestrictedEntrySet(set, clzKey, clzVal);
        }


    // ----- inner class: Restricted Collection -----------------------------

    /**
    * A restricted Collection that requires its contents to be of a
    * specified class.
    */
    public static class RestrictedCollection
            extends Base
            implements Collection, Serializable
        {
        /**
        * Constructor.
        *
        * @param col  the underlying Collection
        * @param clz  the class of objects that may be stored in the
        *             Collection
        */
        public RestrictedCollection(Collection col, Class clz)
            {
            azzert(col != null);
            azzert(clz != null && !clz.isPrimitive());

            m_col = col;
            m_clz = clz;
            }


        // ----- Collection interface -----------------------------------

        /**
        * Returns the number of elements in this Collection.
        *
        * @return the number of elements in this Collection
        */
        public int size()
            {
            return m_col.size();
            }

        /**
        * Returns <tt>true</tt> if this Collection contains no elements.
        *
        * @return <tt>true</tt> if this Collection contains no elements
        */
        public boolean isEmpty()
            {
            return m_col.isEmpty();
            }

        /**
        * Returns true if this Collection contains the specified element.  More
        * formally, returns true if and only if this Collection contains at least
        * one element <code>e</code> such that <code>(o==null ? e==null :
        * o.equals(e))</code>.
        *
        * @param o  the object to search for in the Collection
        *
        * @return true if this Collection contains the specified object
        */
        public boolean contains(Object o)
            {
            return m_col.contains(o);
            }

        /**
        * Returns an Iterator over the elements contained in this Collection.
        *
        * @return an Iterator over the elements contained in this Collection
        */
        public Iterator iterator()
            {
            return m_col.iterator();
            }

        /**
        * Returns an array containing all of the elements in this Collection.
        * Obeys the general contract of Collection.toArray.
        *
        * @return an array, whose component type is the class of objects
        *         that may be stored in the Collection containing all of
        *         the elements in this Collection
        */
        public Object[] toArray()
            {
            return m_col.toArray((Object[]) Array.newInstance(m_clz, m_col.size()));
            }

        /**
        * Returns an array containing all of the elements in this Collection
        * whose runtime type is that of the specified array.
        * Obeys the general contract of Collection.toArray.
        *
        * @param ao  the array into which the elements of this Collection
        *            are to be stored, if it is big enough; otherwise, a
        *            new array of the same runtime type is allocated for
        *            this purpose
        *
        * @return an array containing the elements of this Collection
        */
        public Object[] toArray(Object ao[])
            {
            return m_col.toArray(ao);
            }

        /**
        * Ensures that this Collection contains the specified element.
        *
        * @param o element whose presence in this Collection is to be ensured
        *
        * @return true if the Collection changed as a result of the call
        *
        * @throws ClassCastException class of the specified element prevents it
        *         from being added to this Collection
        */
        public boolean add(Object o)
            {
            checkObject(o);
            return m_col.add(o);
            }

        /**
        * Removes a single instance of the specified element from this Collection,
        * if it is present (optional operation).  More formally, removes an
        * element <code>e</code> such that <code>(o==null ? e==null :
        * o.equals(e))</code>, if the Collection contains one or more such
        * elements.  Returns true if the Collection contained the specified
        * element (or equivalently, if the Collection changed as a result of the
        * call).
        *
        * @param o  element to be removed from this Collection, if present
        *
        * @return true if the Collection contained the specified element
        */
        public boolean remove(Object o)
            {
            return m_col.remove(o);
            }

        /**
        * Returns <tt>true</tt> if this Collection contains all of the elements
        * in the specified Collection.
        *
        * @param col  Collection to be checked for containment in this
        *             Collection
        *
        * @return <tt>true</tt> if this Collection contains all of the elements
        *	       in the specified Collection
        */
        public boolean containsAll(Collection col)
            {
            return m_col.containsAll(col);
            }

        /**
        * Adds all of the elements in the specified Collection to this Collection
        * (optional operation).  The behavior of this operation is undefined if
        * the specified Collection is modified while the operation is in progress.
        * (This implies that the behavior of this call is undefined if the
        * specified Collection is this Collection, and this Collection is
        * nonempty.)
        *
        * @param col  elements to be inserted into this Collection
        *
        * @return <tt>true</tt> if this Collection changed as a result of the
        *         call
        * 
        * @throws ClassCastException  if the class of an element of the
        * 	                          specified Collection prevents it from
        *                             being added to this Collection
        */
        public boolean addAll(Collection col)
            {
            Object[] ao = col.toArray();
            for (int i = 0, c = ao.length; i < c; ++i)
                {
                checkObject(ao[i]);
                }

            // use ImmutableArrayList to ensure that the contents being added
            // are the same that were just checked (besides, it is efficient)
            return m_col.addAll(new ImmutableArrayList(ao));
            }

        /**
        * Removes all this Collection's elements that are also contained in the
        * specified Collection (optional operation).  After this call returns,
        * this Collection will contain no elements in common with the specified
        * Collection.
        *
        * @param col  elements to be removed from this Collection
        *
        * @return <tt>true</tt> if this Collection changed as a result of the
        *         call
        */
        public boolean removeAll(Collection col)
            {
            return m_col.removeAll(col);
            }

        /**
        * Retains only the elements in this Collection that are contained in
        * the specified Collection (optional operation).  In other words,
        * removes from this Collection all of its elements that are not
        * contained in the specified Collection.
        *
        * @param col  elements to be retained in this Collection
        *
        * @return <tt>true</tt> if this Collection changed as a result of the
        *         call
        */
        public boolean retainAll(Collection col)
            {
            return m_col.retainAll(col);
            }

        /**
        * Removes all of the elements from this Collection.
        */
        public void clear()
            {
            m_col.clear();
            }


        // ----- Object methods -----------------------------------------

        /**
        * Compares the specified object with this collection for equality. <p>
        * Obeys the general contract of Collection.equals.
        *
        * @param o  Object to be compared for equality with this Collection
        *
        * @return <tt>true</tt> if the specified object is equal to this
        *         Collection
        */
        public boolean equals(Object o)
            {
            if (o instanceof RestrictedCollection)
                {
                o = ((RestrictedCollection) o).m_col;
                }
            return m_col.equals(o);
            }

        /**
        * Returns the hash code value for this collection.
        * Obeys the general contract of Collection.hashCode.
        *
        * @return the hash code value for this collection
        */
        public int hashCode()
            {
            return m_col.hashCode();
            }

        /**
        * Return a String description for this collection.
        *
        * @return a String description of the Collection
        */
        public String toString()
            {
            return m_col.toString();
            }


        // ----- internal helpers ---------------------------------------

        /**
        * Check the passed object to verify that it passes the restriction of
        * this Collection.
        *
        * @param o  the Object to check
        * 
        * @throws ClassCastException  if the class of the passed Object
        * 	                          prevents it from being stored in this
        *                             Collection
        */
        protected void checkObject(Object o)
            {
            if (!(o == null || m_clz.isInstance(o)))
                {
                throw new ClassCastException("Unable to cast an object of class "
                    + o.getClass().getName() + " to class " + m_clz.getName());
                }
            }


        // ----- data members -------------------------------------------

        /**
        * The underlying Collection.
        */
        protected Collection m_col;

        /**
        * The class of Objects stored in the Collection.
        */
        protected Class      m_clz;
        }


    // ----- inner class: Restricted Set ------------------------------------

    /**
    * A restricted Set that requires its contents to be of a
    * specified class.
    */
    public static class RestrictedSet
            extends RestrictedCollection
            implements Set, Serializable
        {
        /**
        * Constructor.
        *
        * @param set  the underlying Set
        * @param clz  the class of objects that may be stored in the
        *             Set
        */
        public RestrictedSet(Set set, Class clz)
            {
            super(set, clz);
            }
        }


    // ----- inner class: Restricted Sorted Set -----------------------------

    /**
    * A restricted Set that requires its contents to be of a
    * specified class.
    */
    public static class RestrictedSortedSet
            extends RestrictedSet
            implements SortedSet, Serializable
        {
        /**
        * Constructor.
        *
        * @param set  the underlying SortedSet
        * @param clz  the class of objects that may be stored in the
        *             SortedSet
        */
        public RestrictedSortedSet(SortedSet set, Class clz)
            {
            super(set, clz);
            }

        // ----- SortedSet interface ------------------------------------

        /**
        * Returns the comparator associated with this sorted set, or
        * <tt>null</tt> if it uses its elements' natural ordering.
        *
        * @return the comparator associated with this sorted set, or
        * 	       <tt>null</tt> if it uses its elements' natural ordering
        */
        public Comparator comparator()
            {
            return ((SortedSet) m_col).comparator();
            }

        /**
        * Returns a view of the portion of this sorted set whose elements range
        * from <tt>fromElement</tt>, inclusive, to <tt>toElement</tt>, exclusive.
        * (If <tt>fromElement</tt> and <tt>toElement</tt> are equal, the returned
        * sorted set is empty.)  The returned sorted set is backed by this sorted
        * set, so changes in the returned sorted set are reflected in this sorted
        * set, and vice-versa.  The returned sorted set supports all optional set
        * operations that this sorted set supports.<p>
        * Obeys the general contract of SortedSet.subSet.
        *
        * @param fromElement  low endpoint (inclusive) of the subSet
        * @param toElement    high endpoint (exclusive) of the subSet
        *
        * @return a view of the specified range within this sorted set
        */
        public SortedSet subSet(Object fromElement, Object toElement)
            {
            SortedSet subset = ((SortedSet) m_col).subSet(fromElement, toElement);
            return getSortedSet(subset, m_clz);
            }

        /**
        * Returns a view of the portion of this sorted set whose elements are
        * strictly less than <tt>toElement</tt>.  The returned sorted set is
        * backed by this sorted set, so changes in the returned sorted set are
        * reflected in this sorted set, and vice-versa.  The returned sorted set
        * supports all optional set operations.<p>
        * Obeys the general contract of SortedSet.headSet.
        *
        * @param toElement  high endpoint (exclusive) of the headSet
        *
        * @return a view of the specified initial range of this sorted set
        */
        public SortedSet headSet(Object toElement)
            {
            SortedSet subset = ((SortedSet) m_col).headSet(toElement);
            return getSortedSet(subset, m_clz);
            }

        /**
        * Returns a view of the portion of this sorted set whose elements are
        * greater than or equal to <tt>fromElement</tt>.  The returned sorted set
        * is backed by this sorted set, so changes in the returned sorted set are
        * reflected in this sorted set, and vice-versa.  The returned sorted set
        * supports all optional set operations.<p>
        * Obeys the general contract of SortedSet.tailSet.
        *
        * @param fromElement  low endpoint (inclusive) of the tailSet
        *
        * @return a view of the specified final range of this sorted set
        */
        public SortedSet tailSet(Object fromElement)
            {
            SortedSet subset = ((SortedSet) m_col).tailSet(fromElement);
            return getSortedSet(subset, m_clz);
            }

        /**
        * Returns the first (lowest) element currently in this sorted set.
        *
        * @return the first (lowest) element currently in this sorted set
        *
        * @throws NoSuchElementException  sorted set is empty
        */
        public Object first()
            {
            return ((SortedSet) m_col).first();
            }

        /**
        * Returns the last (highest) element currently in this sorted set.
        *
        * @return the last (highest) element currently in this sorted set
        *
        * @throws NoSuchElementException  sorted set is empty
        */
        public Object last()
            {
            return ((SortedSet) m_col).last();
            }
        }


    // ----- inner class: Restricted List -----------------------------------

    /**
    * A restricted List that requires its contents to be of a
    * specified class.
    */
    public static class RestrictedList
            extends RestrictedCollection
            implements List, Serializable
        {
        /**
        * Constructor.
        *
        * @param list  the underlying List
        * @param clz   the class of objects that may be stored in the
        *              List
        */
        public RestrictedList(List list, Class clz)
            {
            super(list, clz);
            }


        // ----- List interface -----------------------------------------

        /**
        * Returns the element at the specified position in this list.
        *
        * @param index index of element to return
        *
        * @return the element at the specified position in this list
        */
        public Object get(int index)
            {
            return ((List) m_col).get(index);
            }

        /**
        * Replaces the element at the specified position in this list with the
        * specified element (optional operation).
        *
        * @param index index of element to replace.
        * @param element element to be stored at the specified position.
        * @return the element previously at the specified position.
        * 
        * @throws  ClassCastException if the class of the specified element
        * 		   prevents it from being added to this list
        */
        public Object set(int index, Object element)
            {
            checkObject(element);
            return ((List) m_col).set(index, element);
            }

        /**
        * Inserts the specified element at the specified position in this list
        * (optional operation).  Shifts the element currently at that position
        * (if any) and any subsequent elements to the right (adds one to their
        * indices).
        *
        * @param index    index at which the specified element is to be
        *                 inserted
        * @param element  element to be inserted
        * 
        * @throws  ClassCastException if the class of the specified element
        * 		   prevents it from being added to this list
        */
        public void add(int index, Object element)
            {
            checkObject(element);
            ((List) m_col).add(index, element);
            }

        /**
        * Inserts all of the elements in the specified collection into this
        * list at the specified position (optional operation).
        *
        * @param index  index at which to insert first element from the
        *               specified collection
        * @param col    elements to be inserted into this list
        *
        * @return <tt>true</tt> if this list changed as a result of the call
        *
        * @throws ClassCastException if the class of one of elements of the
        * 		  specified collection prevents it from being added to this
        * 		  list
        */
        public boolean addAll(int index, Collection col)
            {
            Object[] ao = col.toArray();
            for (int i = 0, c = ao.length; i < c; ++i)
                {
                checkObject(ao[i]);
                }

            // use ImmutableArrayList to ensure that the contents being added
            // are the same that were just checked (besides, it is efficient)
            return ((List) m_col).addAll(index, new ImmutableArrayList(ao));
            }

        /**
        * Removes the element at the specified position in this list (optional
        * operation).  Shifts any subsequent elements to the left (subtracts one
        * from their indices).  Returns the element that was removed from the
        * list.
        *
        * @param index  the index of the element to removed
        *
        * @return the element previously at the specified position
        */
        public Object remove(int index)
            {
            return ((List) m_col).remove(index);
            }

        /**
        * Returns the index in this list of the first occurrence of the
        * specified element, or -1 if this list does not contain this
        * element.
        *
        * More formally, returns the lowest index <tt>i</tt> such that
        * <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>,
        * or -1 if there is no such index.
        *
        * @param o element to search for
        *
        * @return the index in this list of the first occurrence of the
        *         specified element, or -1 if this list does not contain
        *         this element
        */
        public int indexOf(Object o)
            {
            return ((List) m_col).indexOf(o);
            }

        /**
        * Returns the index in this list of the last occurrence of the
        * specified element, or -1 if this list does not contain this
        * element.
        *
        * More formally, returns the highest index <tt>i</tt> such that
        * <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>,
        * or -1 if there is no such index.
        *
        * @param o  element to search for
        *
        * @return the index in this list of the last occurrence of the
        *         specified element, or -1 if this list does not contain
        *         this element.
        */
        public int lastIndexOf(Object o)
            {
            return ((List) m_col).lastIndexOf(o);
            }

        /**
        * Returns a list iterator of the elements in this list (in proper
        * sequence).
        *
        * @return a list iterator of the elements in this list (in proper
        * 	      sequence)
        */
        public ListIterator listIterator()
            {
            ListIterator iter = ((List) m_col).listIterator();
            return getListIterator(iter, m_clz);
            }

        /**
        * Returns a list iterator of the elements in this list (in proper
        * sequence), starting at the specified position in this list.  The
        * specified index indicates the first element that would be returned by
        * an initial call to the <tt>next</tt> method.  An initial call to
        * the <tt>previous</tt> method would return the element with the
        * specified index minus one.
        *
        * @param index  index of first element to be returned from the
        *		        list iterator (by a call to the <tt>next</tt> method)
        *
        * @return a list iterator of the elements in this list (in proper
        * 	       sequence), starting at the specified position in this list
        */
        public ListIterator listIterator(int index)
            {
            ListIterator iter = ((List) m_col).listIterator(index);
            return getListIterator(iter, m_clz);
            }

        /**
        * Returns a view of the portion of this list between the specified
        * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.
        * Obeys the general contract of List.subList.
        *
        * @param fromIndex  low endpoint (inclusive) of the subList
        * @param toIndex    high endpoint (exclusive) of the subList
        *
        * @return a view of the specified range within this list
        */
        public List subList(int fromIndex, int toIndex)
            {
            List sublist = ((List) m_col).subList(fromIndex, toIndex);
            return getList(sublist, m_clz);
            }
        }


    // ----- inner class: Restricted ListIterator ---------------------------

    /**
    * A restricted ListIterator that requires its contents to be of a
    * specified class.
    */
    public static class RestrictedListIterator
            extends Base
            implements ListIterator
        {
        /**
        * Constructor.
        *
        * @param iter  the underlying ListIterator
        * @param clz   the class of objects that may be stored in the
        *              ListIterator
        */
        public RestrictedListIterator(ListIterator iter, Class clz)
            {
            azzert(iter != null);
            azzert(clz != null && !clz.isPrimitive());

            m_iter = iter;
            m_clz  = clz;
            }


        // ----- ListIterator interface ---------------------------------

        /**
        * Returns <tt>true</tt> if this list iterator has more elements when
        * traversing the list in the forward direction.
        * Obeys the general contract of ListIterator.hasNext.
        *
        * @return <tt>true</tt> if the list iterator has more elements when
        *		  traversing the list in the forward direction
        */
        public boolean hasNext()
            {
            return m_iter.hasNext();
            }

        /**
        * Returns the next element in the list.
        * Obeys the general contract of ListIterator.next.
        *
        * @return the next element in the list
        */
        public Object next()
            {
            return m_iter.next();
            }

        /**
        * Returns <tt>true</tt> if this list iterator has more elements when
        * traversing the list in the reverse direction.
        * Obeys the general contract of ListIterator.hasPrevious.
        *
        * @return <tt>true</tt> if the list iterator has more elements when
        *	      traversing the list in the reverse direction
        */
        public boolean hasPrevious()
            {
            return m_iter.hasPrevious();
            }

        /**
        * Returns the previous element in the list.
        * Obeys the general contract of ListIterator.previous.
        *
        * @return the previous element in the list
        * 
        * @exception NoSuchElementException if the iteration has no previous
        *            element
        */
        public Object previous()
            {
            return m_iter.previous();
            }

        /**
        * Returns the index of the element that would be returned by a
        * subsequent call to <tt>next</tt>.
        * Obeys the general contract of ListIterator.nextIndex.
        *
        * @return the index of the element that would be returned by a
        *         subsequent call to <tt>next</tt>, or list size if list
        *         iterator is at end of list
        */
        public int nextIndex()
            {
            return m_iter.nextIndex();
            }

        /**
        * Returns the index of the element that would be returned by a
        * subsequent call to <tt>previous</tt>.
        * Obeys the general contract of ListIterator.previousIndex.
        *
        * @return the index of the element that would be returned by a
        *         subsequent call to <tt>previous</tt>, or -1 if list
        *         iterator is at beginning of list
        */ 
        public int previousIndex()
            {
            return m_iter.previousIndex();
            }

        /**
        * Removes from the list the last element that was returned by
        * <tt>next</tt> or <tt>previous</tt>.
        * Obeys the general contract of ListIterator.remove.
        */
        public void remove()
            {
            m_iter.remove();
            }

        /**
        * Replaces the last element returned by <tt>next</tt> or
        * <tt>previous</tt> with the specified element.
        * Obeys the general contract of ListIterator.set.
        *
        * @param o  the element with which to replace the last element
        *           returned by <tt>next</tt> or <tt>previous</tt>
        *
        * @exception ClassCastException if the class of the specified element
        * 		     prevents it from being added to this list
        */
        public void set(Object o)
            {
            checkObject(o);
            m_iter.set(o);
            }

        /**
        * Inserts the specified element into the list.
        * Obeys the general contract of ListIterator.add.
        *
        * @param o  the element to insert
        *
        * @exception ClassCastException if the class of the specified element
        * 		     prevents it from being added to this ListIterator
        */
        public void add(Object o)
            {
            checkObject(o);
            m_iter.add(o);
            }


        // ----- internal helpers ---------------------------------------

        /**
        * Check the passed object to verify that it passes the restriction of
        * this ListIterator.
        *
        * @param o  the Object to check
        * 
        * @throws ClassCastException  if the class of the passed Object
        * 	                          prevents it from being stored in this
        *                             ListIterator
        */
        protected void checkObject(Object o)
            {
            if (!(o == null || m_clz.isInstance(o)))
                {
                throw new ClassCastException("Unable to cast an object of class "
                    + o.getClass().getName() + " to class " + m_clz.getName());
                }
            }


        // ----- data members -------------------------------------------

        /**
        * The underlying ListIterator.
        */
        protected ListIterator m_iter;

        /**
        * The class of Objects stored in the ListIterator.
        */
        protected Class        m_clz;
        }


    // ----- inner class: Restricted Map ------------------------------------

    /**
    * A restricted Map that requires its keys and values to be of
    * specified classes.
    */
    public static class RestrictedMap
            extends Base
            implements Map, Serializable
        {
        /**
        * Constructor.
        *
        * @param map     the underlying Map
        * @param clzKey  the class of keys that may be stored in the Map
        * @param clzVal  the class of values that may be stored in the Map
        */
        public RestrictedMap(Map map, Class clzKey, Class clzVal)
            {
            azzert(map != null);
            azzert(clzKey != null && !clzKey.isPrimitive());
            azzert(clzVal != null && !clzVal.isPrimitive());

            m_map    = map;
            m_clzKey = clzKey;
            m_clzVal = clzVal;
            }


        // ----- Map interface ------------------------------------------

        /**
        * Returns the number of key-value mappings in this map.
        *
        * @return the number of key-value mappings in this map
        */
        public int size()
            {
            return m_map.size();
            }

        /**
        * Returns <tt>true</tt> if this map contains no key-value mappings.
        *
        * @return <tt>true</tt> if this map contains no key-value mappings
        */
        public boolean isEmpty()
            {
            return m_map.isEmpty();
            }

        /**
        * Returns <tt>true</tt> if this map contains a mapping for the
        * specified key.
        *
        * @param key  key whose presence in this map is to be tested
        *
        * @return <tt>true</tt> if this map contains a mapping for the
        *         specified key
        */
        public boolean containsKey(Object key)
            {
            return m_map.containsKey(key);
            }

        /**
        * Returns <tt>true</tt> if this map maps one or more keys to the
        * specified value.
        *
        * @param value  value whose presence in this map is to be tested
        *
        * @return <tt>true</tt> if this map maps one or more keys to the
        *         specified value.
        */
        public boolean containsValue(Object value)
            {
            return m_map.containsValue(value);
            }

        /**
        * Returns the value to which this map maps the specified key.
        * Returns <tt>null</tt> if the map contains no mapping for this key.
        *
        * @param key  key whose associated value is to be returned
        *
        * @return the value to which this map maps the specified key, or
        *	      <tt>null</tt> if the map contains no mapping for this key.
        */
        public Object get(Object key)
            {
            return m_map.get(key);
            }

        /**
        * Associates the specified value with the specified key in this map.
        *
        * @param key    key with which the specified value is to be associated
        * @param value  value to be associated with the specified key
        *
        * @return previous value associated with specified key, or
        *         <tt>null</tt> if there was no mapping for key
        * 
        * @throws ClassCastException if the class of the specified key or value
        * 	      prevents it from being stored in this map.
        */
        public Object put(Object key, Object value)
            {
            checkKey(key);
            checkValue(value);
            return m_map.put(key, value);
            }

        /**
        * Removes the mapping for this key from this map if present.
        *
        * @param key  key whose mapping is to be removed from the map
        *
        * @return previous value associated with specified key, or
        *         <tt>null</tt> if there was no mapping for key
        */
        public Object remove(Object key)
            {
            return m_map.remove(key);
            }

        /**
        * Copies all of the mappings from the specified map to this map.
        *
        * @param map  Mappings to be stored in this map
        * 
        * @throws ClassCastException if the class of a key or value in the
        * 	      specified map prevents it from being stored in this map
        */
        public void putAll(Map map)
            {
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry  = (Map.Entry) iter.next();
                Object    oKey   = entry.getKey();
                Object    oValue = entry.getValue();
                checkKey(oKey);
                checkValue(oValue);
                put(oKey, oValue);
                }
            }

        /**
        * Removes all mappings from this map.
        */
        public void clear()
            {
            m_map.clear();
            }

        /**
        * Returns a set view of the keys contained in this map.  The set is
        * backed by the map, so changes to the map are reflected in the set,
        * and vice-versa.
        *
        * @return a set view of the keys contained in this map
        */
        public Set keySet()
            {
            Set set = m_map.keySet();
            return getSet(set, m_clzKey);
            }

        /**
        * Returns a collection view of the values contained in this map.
        * The collection is backed by the map, so changes to the map are
        * reflected in the collection, and vice-versa.
        *
        * @return a collection view of the values contained in this map
        */
        public Collection values()
            {
            Collection col = m_map.values();
            return getCollection(col, m_clzVal);
            }

        /**
        * Returns a set view of the mappings contained in this map.  Each
        * element in the returned set is a <tt>Map.Entry</tt>.  The set is
        * backed by the map, so changes to the map are reflected in the set,
        * and vice-versa.
        *
        * @return a set view of the mappings contained in this map
        */
        public Set entrySet()
            {
            if (m_set == null)
                {
                Set set = m_map.entrySet();
                m_set = getEntrySet(set, m_clzKey, m_clzVal);
                }
            return m_set;
            }


        // ----- Object methods -----------------------------------------

        /**
        * Compares the specified object with this map for equality.
        * Obeys the general contract of Map.equals.
        *
        * @param o  object to be compared for equality with this map
        *
        * @return <tt>true</tt> if the specified object is equal to this map
        */
        public boolean equals(Object o)
            {
            if (o instanceof RestrictedMap)
                {
                o = ((RestrictedMap) o).m_map;
                }
            return m_map.equals(o);
            }

        /**
        * Returns the hash code value for this map.
        * Obeys the general contract of Map.hashCode.
        *
        * @return the hash code value for this map
        */
        public int hashCode()
            {
            return m_map.hashCode();
            }

        /**
        * Return a String description for this Map.
        *
        * @return a String description of the Map
        */
        public String toString()
            {
            return m_map.toString();
            }


        // ----- internal helpers ---------------------------------------

        /**
        * Check the passed object to verify that it passes the "key"
        * restriction of this Map.
        *
        * @param o  the Object to check
        * 
        * @throws ClassCastException  if the class of the passed Object
        *         prevents it from being used as a key in this Map
        */
        protected void checkKey(Object o)
            {
            if (!(o == null || m_clzKey.isInstance(o)))
                {
                throw new ClassCastException("Unable to cast an object of class "
                    + o.getClass().getName() + " to class " + m_clzKey.getName());
                }
            }

        /**
        * Check the passed object to verify that it passes the "value"
        * restriction of this Map.
        *
        * @param o  the Object to check
        * 
        * @throws ClassCastException  if the class of the passed Object
        *         prevents it from being used as a value in this Map
        */
        protected void checkValue(Object o)
            {
            if (!(o == null || m_clzVal.isInstance(o)))
                {
                throw new ClassCastException("Unable to cast an object of class "
                    + o.getClass().getName() + " to class " + m_clzVal.getName());
                }
            }


        // ----- data members -------------------------------------------

        /**
        * The underlying Map.
        */
        protected Map   m_map;

        /**
        * The class of key stored in the Map.
        */
        protected Class m_clzKey;

        /**
        * The class of value stored in the Map.
        */
        protected Class m_clzVal;

        /**
        * The Entry Set.
        */
        protected Set   m_set;
        }


    // ----- inner class: Restricted Sorted Map -----------------------------

    /**
    * A restricted SortedMap that requires its keys and values to be of
    * specified classes.
    */
    public static class RestrictedSortedMap
            extends RestrictedMap
            implements SortedMap, Serializable
        {
        /**
        * Constructor.
        *
        * @param map     the underlying SortedMap
        * @param clzKey  the class of keys that may be stored in the Map
        * @param clzVal  the class of values that may be stored in the Map
        */
        public RestrictedSortedMap(Map map, Class clzKey, Class clzVal)
            {
            super(map, clzKey, clzVal);
            }


        // ----- SortedMap interface ------------------------------------

        /**
        * Returns the comparator associated with this sorted map, or
        * <tt>null</tt> if it uses its keys' natural ordering.
        *
        * @return the comparator associated with this sorted map, or
        * 	       <tt>null</tt> if it uses its keys' natural ordering
        */
        public Comparator comparator()
            {
            return ((SortedMap) m_map).comparator();
            }

        /**
        * Returns a view of the portion of this sorted map whose keys range
        * from <tt>fromKey</tt>, inclusive, to <tt>toKey</tt>, exclusive.
        * Obeys the general contract of SortedMap.subMap.
        *
        * @param fromKey  low endpoint (inclusive) of the subMap
        * @param toKey    high endpoint (exclusive) of the subMap
        *
        * @return a view of the specified range within this sorted map
        */
        public SortedMap subMap(Object fromKey, Object toKey)
            {
            SortedMap submap = ((SortedMap) m_map).subMap(fromKey, toKey);
            return getSortedMap(submap, m_clzKey, m_clzVal);
            }

        /**
        * Returns a view of the portion of this sorted map whose keys are
        * strictly less than toKey.
        *
        * @param toKey  high endpoint (exclusive) of the subMap
        *
        * @return a view of the specified initial range of this sorted map
        */
        public SortedMap headMap(Object toKey)
            {
            SortedMap submap = ((SortedMap) m_map).headMap(toKey);
            return getSortedMap(submap, m_clzKey, m_clzVal);
            }

        /**
        * Returns a view of the portion of this sorted map whose keys are
        * greater than or equal to <tt>fromKey</tt>.
        *
        * @param fromKey low endpoint (inclusive) of the tailMap
        *
        * @return a view of the specified final range of this sorted map
        */
        public SortedMap tailMap(Object fromKey)
            {
            SortedMap submap = ((SortedMap) m_map).tailMap(fromKey);
            return getSortedMap(submap, m_clzKey, m_clzVal);
            }

        /**
        * Returns the first (lowest) key currently in this sorted map.
        *
        * @return the first (lowest) key currently in this sorted map
        */
        public Object firstKey()
            {
            return ((SortedMap) m_map).firstKey();
            }

        /**
        * Returns the last (highest) key currently in this sorted map.
        *
        * @return the last (highest) key currently in this sorted map
        */
        public Object lastKey()
            {
            return ((SortedMap) m_map).lastKey();
            }
        }


    // ----- inner class: Restricted Entry Set ------------------------------

    /**
    * A restricted Collection that requires its contents to be of a
    * specified class.
    */
    public static class RestrictedEntrySet
            extends Base
            implements Set, Serializable
        {
        /**
        * Constructor.
        *
        * @param set     the underlying Entry Set
        * @param clzKey  the class of keys that may be stored in the Map
        * @param clzVal  the class of values that may be stored in the Map
        */
        public RestrictedEntrySet(Set set, Class clzKey, Class clzVal)
            {
            azzert(set != null);
            azzert(clzKey != null && !clzKey.isPrimitive());
            azzert(clzVal != null && !clzVal.isPrimitive());

            m_set    = set;
            m_clzVal = clzVal;
            }


        // ----- Collection interface -----------------------------------

        /**
        * Returns the number of elements in this Collection.
        *
        * @return the number of elements in this Collection
        */
        public int size()
            {
            return m_set.size();
            }

        /**
        * Returns <tt>true</tt> if this Collection contains no elements.
        *
        * @return <tt>true</tt> if this Collection contains no elements
        */
        public boolean isEmpty()
            {
            return m_set.isEmpty();
            }

        /**
        * Returns true if this Collection contains the specified element.  More
        * formally, returns true if and only if this Collection contains at least
        * one element <code>e</code> such that <code>(o==null ? e==null :
        * o.equals(e))</code>.
        *
        * @param o  the object to search for in the Collection
        *
        * @return true if this Collection contains the specified object
        */
        public boolean contains(Object o)
            {
            return m_set.contains(o);
            }

        /**
        * Returns an Iterator over the elements contained in this Collection.
        *
        * @return an Iterator over the elements contained in this Collection
        */
        public Iterator iterator()
            {
            Iterator iter = m_set.iterator();
            return wrapIterator(iter);
            }

        /**
        * Returns an array containing all of the elements in this Collection.
        * Obeys the general contract of Collection.toArray.
        *
        * @return an array, whose component type is the class of objects
        *         that may be stored in the Collection containing all of
        *         the elements in this Collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array containing all of the elements in this Collection
        * whose runtime type is that of the specified array.
        * Obeys the general contract of Collection.toArray.
        *
        * @param ao  the array into which the elements of this Collection
        *            are to be stored, if it is big enough; otherwise, a
        *            new array of the same runtime type is allocated for
        *            this purpose
        *
        * @return an array containing the elements of this Collection
        */
        public Object[] toArray(Object ao[])
            {
            Object[] aoRaw = m_set.toArray();
            int      c     = aoRaw.length;

            // create the array to store the wrapped entries
            if (ao == null)
                {
                // implied default type, see toArray()
                ao = new Map.Entry[c];
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

            for (int i = 0; i < c; ++i)
                {
                ao[i] = wrapEntry((Map.Entry) aoRaw[i]);
                }

            return ao;
            }

        /**
        * Ensures that this Collection contains the specified element.
        *
        * @param o element whose presence in this Collection is to be ensured
        *
        * @return true if the Collection changed as a result of the call
        */
        public boolean add(Object o)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * Removes a single instance of the specified element from this Collection,
        * if it is present (optional operation).  More formally, removes an
        * element <code>e</code> such that <code>(o==null ? e==null :
        * o.equals(e))</code>, if the Collection contains one or more such
        * elements.  Returns true if the Collection contained the specified
        * element (or equivalently, if the Collection changed as a result of the
        * call).
        *
        * @param o  element to be removed from this Collection, if present
        *
        * @return true if the Collection contained the specified element
        */
        public boolean remove(Object o)
            {
            return m_set.remove(o);
            }

        /**
        * Returns <tt>true</tt> if this Collection contains all of the elements
        * in the specified Collection.
        *
        * @param col  Collection to be checked for containment in this
        *             Collection
        *
        * @return <tt>true</tt> if this Collection contains all of the elements
        *	       in the specified Collection
        */
        public boolean containsAll(Collection col)
            {
            return m_set.containsAll(col);
            }

        /**
        * Adds all of the elements in the specified Collection to this Collection
        * (optional operation).  The behavior of this operation is undefined if
        * the specified Collection is modified while the operation is in progress.
        * (This implies that the behavior of this call is undefined if the
        * specified Collection is this Collection, and this Collection is
        * nonempty.)
        *
        * @param col  elements to be inserted into this Collection
        *
        * @return <tt>true</tt> if this Collection changed as a result of the
        *         call
        */
        public boolean addAll(Collection col)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * Removes all this Collection's elements that are also contained in the
        * specified Collection (optional operation).  After this call returns,
        * this Collection will contain no elements in common with the specified
        * Collection.
        *
        * @param col  elements to be removed from this Collection
        *
        * @return <tt>true</tt> if this Collection changed as a result of the
        *         call
        */
        public boolean removeAll(Collection col)
            {
            return m_set.removeAll(col);
            }

        /**
        * Retains only the elements in this Collection that are contained in
        * the specified Collection (optional operation).  In other words,
        * removes from this Collection all of its elements that are not
        * contained in the specified Collection.
        *
        * @param col  elements to be retained in this Collection
        *
        * @return <tt>true</tt> if this Collection changed as a result of the
        *         call
        */
        public boolean retainAll(Collection col)
            {
            return m_set.retainAll(col);
            }

        /**
        * Removes all of the elements from this Collection.
        */
        public void clear()
            {
            m_set.clear();
            }


        // ----- Object methods -----------------------------------------

        /**
        * Compares the specified object with this collection for equality. <p>
        * Obeys the general contract of Collection.equals.
        *
        * @param o  Object to be compared for equality with this Collection
        *
        * @return <tt>true</tt> if the specified object is equal to this
        *         Collection
        */
        public boolean equals(Object o)
            {
            if (o instanceof RestrictedEntrySet)
                {
                o = ((RestrictedEntrySet) o).m_set;
                }
            return m_set.equals(o);
            }

        /**
        * Returns the hash code value for this collection.
        * Obeys the general contract of Collection.hashCode.
        *
        * @return the hash code value for this collection
        */
        public int hashCode()
            {
            return m_set.hashCode();
            }

        /**
        * Return a String description for this collection.
        *
        * @return a String description of the Collection
        */
        public String toString()
            {
            return m_set.toString();
            }


        // ----- internal helpers ---------------------------------------

        /**
        * Check the passed object to verify that it passes the "value"
        * restriction of this Map.
        *
        * @param o  the Object to check
        * 
        * @throws ClassCastException  if the class of the passed Object
        *         prevents it from being used as a value in this Map
        */
        protected void checkValue(Object o)
            {
            if (!(o == null || m_clzVal.isInstance(o)))
                {
                throw new ClassCastException("Unable to cast an object of class "
                    + o.getClass().getName() + " to class " + m_clzVal.getName());
                }
            }

        /**
        * Wrap an Entry from the Entry Set to make a Restricted Entry.
        *
        * @param entry  a Map Entry to wrap
        *
        * @return a Map Entry that restricts its type
        */
        protected Map.Entry wrapEntry(Map.Entry entry)
            {
            return new RestrictedEntry(entry);
            }

        /**
        * Wrap an Iterator from the Entry Set to make a Restricted Iterator.
        *
        * @param iter  a Iterator to wrap
        *
        * @return a Restricted Iterator
        */
        protected Iterator wrapIterator(Iterator iter)
            {
            return new RestrictedIterator(iter);
            }


        // ----- inner class: Restricted Entry ----------------------

        /**
        * A Map Entry that restricts the key and value types.
        */
        protected class RestrictedEntry
                extends Base
                implements Map.Entry, Serializable
            {
            /**
            * Constructor.
            *
            * @param entry  the Entry to wrap
            */
            public RestrictedEntry(Map.Entry entry)
                {
                m_entry = entry;
                }

            // ----- Map Entry interface ------------------------

            /**
            * Returns the key corresponding to this entry.
            *
            * @return the key corresponding to this entry
            */
            public Object getKey()
                {
                return m_entry.getKey();
                }

            /**
            * Returns the value corresponding to this entry.
            *
            * @return the value corresponding to this entry
            */
            public Object getValue()
                {
                return m_entry.getValue();
                }

            /**
            * Replaces the value corresponding to this entry
            * with the specified value.
            *
            * @param value  new value to be stored in this entry
            *
            * @return old value corresponding to the entry
            *
            * @throws ClassCastException if the class of the
            * 	      specified value prevents it from being
            *         stored in the backing map
            */
            public Object setValue(Object value)
                {
                checkValue(value);
                return m_entry.setValue(value);
                }

            // ----- Object methods -----------------------------

            /**
            * Compares the specified object with this entry
            * for equality.
            *
            * @param o  object to be compared for equality with
            *           this map entry
            *
            * @return <tt>true</tt> if the specified object is
            *         equal to this map entry
            */
            public boolean equals(Object o)
                {
                if (o instanceof RestrictedEntry)
                    {
                    o = ((RestrictedEntry) o).m_entry;
                    }
                return m_entry.equals(o);
                }

            /**
            * Returns the hash code value for this map entry.
            *
            * @return the hash code value for this map entry
            */
            public int hashCode()
                {
                return m_entry.hashCode();
                }

            /**
            * Return a String description for this Entry.
            *
            * @return a String description of the Entry
            */
            public String toString()
                {
                return m_entry.toString();
                }

            // ----- data members -------------------------------

            protected Map.Entry m_entry;
            }


        // ----- inner class: Restricted Iterator -------------------

        /**
        * A Map Entry Iterator that restricts the key and value types.
        */
        protected class RestrictedIterator
                extends Base
                implements Iterator
            {
            /**
            * Constructor.
            *
            * @param iter  the Iterator to wrap
            */
            public RestrictedIterator(Iterator iter)
                {
                m_iter = iter;
                }

            // ----- Iterator interface -------------------------

            /**
            * Returns <tt>true</tt> if the iteration has more elements.
            *
            * @return <tt>true</tt> if the iterator has more elements
            */
            public boolean hasNext()
                {
                return m_iter.hasNext();
                }

            /**
            * Returns the next element in the interation.
            *
            * @return the next element in the iteration
            */
            public Object next()
                {
                return wrapEntry((Map.Entry) m_iter.next());
                }

            /**
            * Removes from the underlying collection the last element
            * returned by the iterator.
            */
            public void remove()
                {
                m_iter.remove();
                }

            // ----- Object methods -----------------------------

            /**
            * Compares the specified object with this Iterator
            * for equality.
            *
            * @param o  object to be compared for equality with
            *           this Iterator
            *
            * @return <tt>true</tt> if the specified object is
            *         equal to this Iterator
            */
            public boolean equals(Object o)
                {
                if (o instanceof RestrictedIterator)
                    {
                    o = ((RestrictedIterator) o).m_iter;
                    }
                return m_iter.equals(o);
                }

            /**
            * Returns the hash code value for this Iterator.
            *
            * @return the hash code value for this Iterator
            */
            public int hashCode()
                {
                return m_iter.hashCode();
                }

            /**
            * Return a String description for this Iterator.
            *
            * @return a String description of the Iterator
            */
            public String toString()
                {
                return m_iter.toString();
                }

            // ----- data members -------------------------------

            protected Iterator m_iter;
            }


        // ----- data members -------------------------------------------

        /**
        * The underlying Entry Set.
        */
        protected Set   m_set;

        /**
        * The class of value stored in the Map.
        */
        protected Class m_clzVal;
        }
    }
