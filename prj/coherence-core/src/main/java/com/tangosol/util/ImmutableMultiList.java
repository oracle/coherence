/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.AbstractList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import java.lang.reflect.Array;
import java.util.Spliterator;


/**
* Implementation of the List interface in a read-only fashion based on a
* collection of arrays.
* <p>
* This class also implements the Set interface, although the contents are
* not checked to determine whether each element is unique. It is the
* responsibility of the user to ensure that the elements are unique if the
* object is used as a Set.
* <p>
* Note: while preserved for backward compatibility, as of Coherence 3.6, use of
*       this class specifically as a List or a Set is deprecated.  Instead, the
*       {@link #getList}, {@link #getSet} methods should be used.
*
* @author gg, mf  2009.1.20
* @see ImmutableArrayList
*/
public class ImmutableMultiList
        extends    AbstractList
        implements List, Set
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a List containing the elements of the specified array of
    * Object arrays.
    *
    * @param aao  the array of arrays backing the MultiList
    */
    public ImmutableMultiList(Object[][] aao)
        {
        m_cTotal = calculateTotalLength(aao);
        m_aao    = aao;
        }

    /**
    * Construct a List containing the elements of the specified Collection of
    * Object arrays.
    *
    * @param collection  the Collection of arrays to fill this MultiList from
    */
    public ImmutableMultiList(Collection collection)
        {
        this((Object[][]) collection.toArray(new Object[collection.size()][]));
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return a java.util.List view of this ImmutableMultiList.
    *
    * @return a List view of this ImmutableMultiList
    */
    public List getList()
        {
        return new ListView();
        }

    /**
    * Return a java.util.Set view of this ImmutableMultiList.
    * <p>
    * Note: this method does not ensure that the underlying ImmutableMultiList
    *       adheres to the Set contract.  It is the responsibility of the
    *       user to ensure that the elements are unique if the object is used
    *       as a SortedSet.
    *
    * @return a Set view of this ImmutableMultiList
    */
    public Set getSet()
        {
        return new SetView();
        }


    // ----- List interface -------------------------------------------------

    @Override
    public Spliterator spliterator()
        {
        return super.spliterator();
        }

    /**
    * Returns the number of elements in this List.
    *
    * @return the number of elements in this List
    */
    public int size()
        {
        return m_cTotal;
        }

    /**
    * Returns the element at the specified position in this List.
    *
    * @param i  the index of the element to return
    *
    * @return the element at the specified position in this List
    *
    * @throws IndexOutOfBoundsException if the index is out of range (index
    *         &lt; 0 || index &gt;= size()).
    */
    public Object get(int i)
        {
        Object[][] aao = m_aao;
        for (int iaa = 0, caa = aao.length; iaa < caa; ++iaa)
            {
            int c = aao[iaa].length;
            if (i < c)
                {
                return aao[iaa][i];
                }
            i -= c;
            }

        throw new IndexOutOfBoundsException();
        }

    /**
    * Returns the index in this List of the first occurrence of the specified
    * element, or -1 if this List does not contain this element.  More
    * formally, returns the lowest index <tt>i</tt> such that
    *   <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>,
    * or -1 if there is no such index.
    *
    * @param o  element to search for.
    *
    * @return the index in this List of the first occurrence of the specified
    *         element, or -1 if this List does not contain this element.
    */
    public int indexOf(Object o)
        {
        int        i   = 0;
        Object[][] aao = m_aao;
        for (int iaa = 0, caa = aao.length; iaa < caa; ++iaa)
            {
            Object[] ao = aao[iaa];
            for (int ia = 0, ca = ao.length; ia < ca; ++ia, ++i)
                {
                if (Base.equals(ao[ia], o))
                    {
                    return i;
                    }
                }
            }
        return -1;
        }

    /**
    * Returns the index in this List of the last occurrence of the specified
    * element, or -1 if this List does not contain this element.  More
    * formally, returns the highest index <tt>i</tt> such that
    *   <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>,
    * or -1 if there is no such index.
    *
    * @param o  element to search for.
    *
    * @return the index in this List of the last occurrence of the specified
    *         element, or -1 if this List does not contain this element.
    */
    public int lastIndexOf(Object o)
        {
        int        i   = m_cTotal - 1;
        Object[][] aao = m_aao;
        for (int iaa = aao.length - 1; iaa >= 0; --iaa)
            {
            Object[] ao = aao[iaa];
            for (int ia = ao.length - 1; ia >= 0; --ia, --i)
                {
                if (Base.equals(ao[ia], o))
                    {
                    return i;
                    }
                }
            }
        return -1;
        }

    /**
    * Returns <tt>true</tt> if this List contains the specified element.
    * More formally, returns <tt>true</tt> if and only if this List contains
    * at least one element <tt>e</tt> such that
    * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
    *
    * @param o  element whose presence in this List is to be tested
    *
    * @return <tt>true</tt> if this List contains the specified element
    */
    public boolean contains(Object o)
        {
        Set set = m_set;
        if (set == null)
            {
            if (m_cTotal < 32)
                {
                return indexOf(o) >= 0;
                }
            // We have a decent number of elements and it appears that we're
            // being accessed as a Set. The ImmutableMultiList data-structure
            // is sub-optimal for Set based operations, and thus for large
            // sets we inflate and delegate to a real Set implementation.
            m_set = set = new HashSet(this);
            }
        return set.contains(o);
        }

    /**
    * Returns an array containing all of the elements in this List in the
    * order that the elements occur in the List.  The returned array will
    * be "safe" in that no references to it are maintained by the List.
    * The caller is thus free to modify the returned array.<p>
    *
    * @return an array containing all of the elements in this List
    */
    public Object[] toArray()
        {
        return toArray((Object[]) null);
        }

    /**
    * Returns an array with ao runtime type is that of the specified array and
    * that contains all of the elements in this List.  If the elements all
    * fit in the specified array, it is returned therein.  Otherwise, a new
    * array is allocated with the runtime type of the specified array and the
    * size of this List.
    * <p>
    * If the elements of the MultiList fit in the specified array with room to
    * spare (i.e., the array has more elements than the MultuList), the element
    * in the array immediately following the end of the last element is set to
    * <tt>null</tt>.
    *
    * @param ao  the array into which the elements of the MultiList are to be
    *            stored, if it is big enough; otherwise, ao new array of the
    *            same runtime type is allocated for this purpose
    *
    * @return an array containing all the elements of the MultiList
    *
    * @throws ArrayStoreException if the runtime type of the specified array
    *         is not ao supertype of the runtime type of every element in this
    *         MultiList
    */
    public Object[] toArray(Object ao[])
        {
        return flatten(m_aao, m_cTotal, ao);
        }

    /**
    * {@inheritDoc}
    */
    public ListIterator listIterator()
        {
        return new MultiIterator(0);
        }

    /**
    * {@inheritDoc}
    */
    public ListIterator listIterator(int i)
        {
        return new MultiIterator(i);
        }

    /**
    * {@inheritDoc}
    */
    public List subList(int iFrom, int iTo)
        {
        int cTotal = m_cTotal;
        if (iTo == -1)
            {
            iTo = cTotal - 1;
            }
        if (iFrom > iTo)
            {
            throw new IllegalArgumentException("iFrom > iTo");
            }
        if (iFrom >= cTotal)
            {
            throw new IllegalArgumentException("iFrom >= cTotal");
            }
        if (iTo > cTotal)
            {
            throw new IllegalArgumentException("iTo > cTotal");
            }

        Object[][] aao = m_aao;

        // find the array for the "from" element
        int      iaaFrom = 0;
        Object[] aFrom   = aao[iaaFrom];
        for (; iFrom >= aFrom.length; aFrom = aao[++iaaFrom])
            {
            iFrom -= aFrom.length;
            }

        // find the array for the "to" element
        int      iaaTo = iaaFrom;
        Object[] aTo   = aFrom;
        for (; iTo > aTo.length; aTo = aao[++iaaTo])
            {
            iTo -= aTo.length;
            }

        // construct multi-array sub-list; and trim the edge arrays
        int        caaSub = iaaTo - iaaFrom + 1;
        Object[][] aaSub  = new Object[caaSub][];
        System.arraycopy(aao, iaaFrom, aaSub, 0, caaSub);
        if (iFrom != 0)
            {
            int c = aFrom.length - iFrom;
            System.arraycopy(aFrom, iFrom, aaSub[0] = new Object[c], 0, c);
            }
        if (iTo != aTo.length)
            {
            int c = iTo + 1;
            System.arraycopy(aFrom, iFrom, aaSub[caaSub - 1] = new Object[c], 0, c);
            }
        return new ImmutableMultiList(aaSub);
        }


    /**
    * Returns an iterator over the elements in this list in proper
    * sequence.
    *
    * @return an iterator over the elements in this list in proper sequence.
    */
    public Iterator iterator()
        {
        return listIterator();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this Collection / List / Set with some other Object and determine
    * if the caller would believe this Object to equal that other Object.
    *
    * @param o  some other Object that is likely to be a Collection or some
    *           more specific type (with its related overloaded definition of
    *           what it thinks that equals() means)
    *
    * @return true iff this Object believes that it can make a defensible
    *         case that this Object is equal to the passed Object
    */
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }
        else if (o instanceof List)
            {
            return super.equals(o);
            }
        else if (o instanceof Collection)
            {
            Collection that = (Collection) o;
            return this.size() == that.size()
                && this.containsAll(that);
            }
        else
            {
            return false;
            }
        }


    // ----- inner class: MultiIterator -------------------------------------

    /**
    * ListIterator implementation based on the ImmutableMultiList.
    */
    protected class MultiIterator
        implements ListIterator
        {
        // ----- constructors -------------------------------------------

        public MultiIterator(int i)
            {
            // advance array as needed
            if (i == -1)
                {
                i = ImmutableMultiList.this.m_cTotal;
                }
            while (i-- > 0)
                {
                next();
                }
            }

        // ----- Iterator interface -----------------------------------------

        public void remove()
            {
            throw new UnsupportedOperationException("collection is read-only");
            }

        public void add(Object o)
            {
            throw new UnsupportedOperationException("collection is read-only");
            }

        public void set(Object o)
            {
            throw new UnsupportedOperationException("collection is read-only");
            }

        public boolean hasNext()
            {
            return m_i < ImmutableMultiList.this.m_cTotal;
            }

        public Object next()
            {
            if (m_i == m_cTotal)
                {
                throw new NoSuchElementException();
                }
            ++m_i;

            Object[][] aao = ImmutableMultiList.this.m_aao;
            int        ia  = m_ia;
            Object[]   ao  = aao[m_iaa];

            while (ia == ao.length)
                {
                // no more elements in this array; move on to the next
                // populated array
                ao = aao[++m_iaa];
                ia = 0;
                }

            m_ia = ia + 1; // prepare for the next iteration
            return ao[ia];
            }

        public int nextIndex()
                {
                return m_i;
                }

        public int previousIndex()
                {
                return m_i - 1;
                }

        public boolean hasPrevious()
                {
                return m_i > 0;
                }

        public Object previous()
                {
                if (m_i == 0)
                    {
                    throw new NoSuchElementException();
                    }
                --m_i;

                Object[][] aao = ImmutableMultiList.this.m_aao;
                int        ia  = m_ia;
                Object[]   ao  = aao[m_iaa];

                while (ia == 0)
                    {
                    // no more elements in this array; move on to the previous
                    // populated array
                    ao = aao[--m_iaa];
                    ia = ao.length;
                    }

                m_ia = --ia; // prepare for the next iteration
                return ao[ia];
                }


        private int m_i;   // index of next to be returned
        private int m_ia;  // index into current array
        private int m_iaa; // index into array of arrays
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Calculate the total number of element in the array of arrays.
    *
    * @param aao  an array of arrays
    *
    * @return the total number of elements
    */
    public static int calculateTotalLength(Object[][] aao)
        {
        int cnt = 0;
        for (int i = 0, c = aao.length; i < c; ++i)
            {
            cnt += aao[i].length;
            }
        return cnt;
        }

    /**
    * Create a single dimensional array containing all elements of the specified
    * array of arrays.
    *
    * @param aaoFrom  an array of arrays to copy from
    * @param cTotal   the total length of the flattened array; pass -1 for it
    *                 to be calculated
    * @param aoTo     an array to copy the elements into (optional)
    *
    * @return an array containing all the elements of the array of arrays
    *
    * @throws ArrayIndexOutOfBoundsException if the total length parameter
    *         was not sufficient to hold the flattened array
    */
    public static Object[] flatten(Object[][] aaoFrom, int cTotal, Object[] aoTo)
        {
        if (cTotal < 0)
            {
            cTotal = calculateTotalLength(aaoFrom);
            }

        if (aoTo == null)
            {
            // implied Object[] type
            aoTo = new Object[cTotal];
            }
        else if (aoTo.length < cTotal)
            {
            // if it is not big enough, ao new array of the same runtime
            // type is allocated
            aoTo = (Object[]) Array.newInstance(aoTo.getClass().getComponentType(), cTotal);
            }
        else if (aoTo.length > cTotal)
            {
            // if the collection fits in the specified array with room to
            // spare, the element in the array immediately following the
            // end of the collection is set to null
            aoTo[cTotal] = null;
            }

        for (int i = 0, of = 0, c = aaoFrom.length; i < c; ++i)
            {
            Object[] aoNext = aaoFrom[i];
            int      cNext  = aoNext.length;
            System.arraycopy(aoNext, 0, aoTo, of, cNext);
            of += cNext;
            }
        return aoTo;
        }


    // ----- inner class: ListView -------------------------------------------

    /**
    * ListView exposes the underlying ImmutableMultiList through the {@link
    * java.util.List} interface, maintaining correct <tt>equals()</tt> and
    * <tt>hashCode()</tt> semantics
    */
    protected class ListView
            extends WrapperCollections.AbstractWrapperList
        {
        // ----- constructors -----------------------------------------------
        /**
        * Create a ListView over this ImmutableMultiList.
        */
        protected ListView()
            {
            super(ImmutableMultiList.this);
            }


        // ----- Object methods ---------------------------------------------

        /**
        * Compares the specified object with this list for equality.  Returns
        * <tt>true</tt> if and only if the specified object is also a list, both
        * lists have the same size, and all corresponding pairs of elements in
        * the two lists are <i>equal</i>.  (Two elements <tt>e1</tt> and
        * <tt>e2</tt> are <i>equal</i> if <tt>(e1==null ? e2==null :
        * e1.equals(e2))</tt>.)  In other words, two lists are defined to be
        * equal if they contain the same elements in the same order.  This
        * definition ensures that the equals method works properly across
        * different implementations of the <tt>List</tt> interface.
        *
        * @param o the object to be compared for equality with this list.
        *
        * @return <tt>true</tt> if the specified object is equal to this list.
        */
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }
            if (!(o instanceof List))
                {
                return false;
                }

            Iterator iterThis = iterator();
            Iterator iterThat = ((List) o).iterator();
            while(iterThis.hasNext() && iterThat.hasNext())
                {
                if (!Base.equals(iterThis.next(), iterThat.next()))
                    {
                    return false;
                    }
                }
            return !(iterThis.hasNext() || iterThat.hasNext());
            }

        /**
        * Returns the hash code value for this list.  The hash code of a list
        * is defined to be the result of the following calculation:
        * <pre>
        *  hashCode = 1;
        *  Iterator i = list.iterator();
        *  while (i.hasNext()) {
        *      Object obj = i.next();
        *      hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
        *  }
        * </pre>
        * This ensures that <tt>list1.equals(list2)</tt> implies that
        * <tt>list1.hashCode()==list2.hashCode()</tt> for any two lists,
        * <tt>list1</tt> and <tt>list2</tt>, as required by the general
        * contract of <tt>Object.hashCode</tt>.
        *
        * @return the hash code value for this list.
        * @see Object#hashCode()
        * @see Object#equals(Object)
        * @see #equals(Object)
        */
        public int hashCode()
            {
            int nHash = 1;
            for (Iterator iter = iterator(); iter.hasNext(); )
                {
                nHash = 31 * nHash + Base.hashCode(iter.next());
                }
            return nHash;
            }
        }


    // ----- inner class: SetView -------------------------------------------

    /**
    * SetView exposes the underlying ImmutableMultiList through the {@link
    * java.util.Set} interface, maintaining correct <tt>equals()</tt> and
    * <tt>hashCode()</tt> semantics
    */
    protected class SetView
            extends WrapperCollections.AbstractWrapperSet
        {
        // ----- constructor ------------------------------------------------
        /**
        * Create a SetView over this ImmutableMultiList.
        */
        protected SetView()
            {
            super(ImmutableMultiList.this);
            }


        // ----- Object methods ---------------------------------------------

        /**
        * Compares the specified object with this set for equality.  Returns
        * <tt>true</tt> if the specified object is also a set, the two sets
        * have the same size, and every member of the specified set is
        * contained in this set (or equivalently, every member of this set is
        * contained in the specified set).  This definition ensures that the
        * equals method works properly across different implementations of the
        * set interface.
        *
        * @param o Object to be compared for equality with this set.
        * @return <tt>true</tt> if the specified Object is equal to this set.
        */
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }
            if (!(o instanceof Set))
                {
                return false;
                }

            Set setOther = (Set) o;
            return setOther.size() == size() &&
                setOther.containsAll(this);
            }

        /**
        * Returns the hash code value for this set.  The hash code of a set is
        * defined to be the sum of the hash codes of the elements in the set,
        * where the hashcode of a <tt>null</tt> element is defined to be zero.
        * This ensures that <code>s1.equals(s2)</code> implies that
        * <code>s1.hashCode()==s2.hashCode()</code> for any two sets
        * <code>s1</code> and <code>s2</code>, as required by the general
        * contract of the <tt>Object.hashCode</tt> method.
        *
        * @return the hash code value for this set.
        * @see Object#hashCode()
        * @see Object#equals(Object)
        * @see Set#equals(Object)
        */
        public int hashCode()
            {
            int nHash = 0;
            for (Iterator iter = iterator(); iter.hasNext(); )
                {
                nHash += Base.hashCode(iter.next());
                }
            return nHash;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The array of Object arrays.
    */
    private Object[][] m_aao;

    /**
    * A fully realized HashSet of this collections contents. This is inflated
    * and used for doing Set based operations if it is detected that this
    * collection is large and being accessed as a Set.
    */
    private Set m_set;

    /**
    * The total number of items.
    */
    private int m_cTotal;
    }
