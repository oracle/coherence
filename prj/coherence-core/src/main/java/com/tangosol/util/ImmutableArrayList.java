/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ExternalizableLite;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import java.lang.reflect.Array;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;


/**
* Implementation of the Collection Framework interface "List" in a read-
* only fashion on top of an array data structure.
* <p>
* This class also implements the Set interface, although the contents are
* not checked to determine whether each element is unique. It is the
* responsibility of the user to ensure that the elements are unique if the
* object is used as a Set.
* <p>
* Note: while preserved for backward compatibility, as of Coherence 3.6, use of
*       this class specifically as a List or a Set is deprecated.  Instead, the
*       {@link #getList}, {@link #getSet}, and {@link #getSortedSet} methods
*       should be used.
*
* @author cp  2001.10.09
*/
public class ImmutableArrayList
        extends AbstractList
        implements Collection, List, Set, SortedSet,
                   Comparable, Cloneable, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a List from a specified number of items in an array starting at
    * the specified offset.
    *
    * @param ao  a non-null array of Objects
    * @param of  an offset of the first item in the array
    * @param c   the number of items to use
    */
    public ImmutableArrayList(Object[] ao, int of, int c)
        {
        Base.azzert(ao != null);
        Base.azzert(ao.length >= of + c);
        m_ao = ao;
        m_of = of;
        m_c  = c;
        }

    /**
    * Construct a List from an array.
    *
    * @param ao  a non-null array of Objects
    */
    public ImmutableArrayList(Object[] ao)
        {
        this(ao, 0, ao == null ? 0 : ao.length);
        }

    /**
    * Construct a List containing the elements of the specified Collection.
    *
    * @param collection  the Collection to fill this List from
    */
    public ImmutableArrayList(Collection collection)
        {
        this(collection.toArray());
        }

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ImmutableArrayList()
        {
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return a java.util.List view of this ImmutableArrayList.
    *
    * @return a List view of this ImmutableArrayList
    */
    public List getList()
        {
        return new ListView();
        }

    /**
    * Return a java.util.Set view of this ImmutableArrayList.
    * <p>
    * Note: this method does not ensure that the underlying ImmutableArrayList
    *       adheres to the Set contract.  It is the responsibility of the
    *       user to ensure that the elements are unique if the object is used
    *       as a Set.
    *
    * @return a Set view of this ImmutableArrayList
    */
    public Set getSet()
        {
        return new SetView();
        }

    /**
    * Return a java.util.SortedSet view of this ImmutableArrayList.
    * <p>
    * Note: this method does not ensure that the underlying ImmutableArrayList
    *       adheres to the SortedSet contract.  It is the responsibility of the
    *       user to ensure that the elements are unique and ordered if the
    *       object is used as a SortedSet.
    *
    * @return a SortedSet view of this ImmutableArrayList
    *
    * @deprecated As of Coherence 14.1.2/24.09, this method and the related
    *             SortedSet implementation are deprecated and will be removed
    *             in a future release. Any code that uses ImmutableArrayList
    *             as a SortedSet should be changed to use a proper SortedSet
    *             implementation.
    */
    @Deprecated(forRemoval = true)
    public SortedSet getSortedSet()
        {
        return new SortedSetView();
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
        return m_c;
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
        int of = m_of + i;
        if (of >= m_c)
            {
            throw new IndexOutOfBoundsException(m_of + "+" + i + ">=" + m_c);
            }
        return m_ao[of];
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
        // check index
        Map map = getValueIndex();
        if (map != null)
            {
            Integer I = (Integer) map.get(o);
            return I == null ? -1 : I.intValue();
            }

        // sequential search
        Object[] ao = m_ao;
        for (int i = 0, of = m_of, c = m_c; i < c; ++i)
            {
            if (Base.equals(ao[of + i], o))
                {
                return i;
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
        // check index
        Map map = getValueIndex();
        if (map != null)
            {
            Integer I = (Integer) map.get(o);
            // three possibilities:
            // 1) the value is not in the index, so it is not in the List
            // 2) the value is in the index, so the answer is known
            // 3) the value is in the index, but the index size is not the
            //    same as the List size because the values in the List are
            //    not all unique
            if (I == null)
                {
                return -1;
                }
            else if (size() == map.size())
                {
                return I.intValue();
                }
            // since there are duplicates, fall through and do a sequential
            // search
            }

        // sequential search
        Object[] ao = m_ao;
        int      of = m_of;
        for (int i = m_c - 1; i >= 0; --i)
            {
            if (Base.equals(ao[of + i], o))
                {
                return i;
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
        return indexOf(o) != -1;
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
        Object[] ao = m_ao;
        int      c  = m_c;

        if (c == ao.length)
            {
            return ao.clone();
            }
        else
            {
            Object[] aoTemp = new Object[c];
            System.arraycopy(ao, m_of, aoTemp, 0, c);
            return aoTemp;
            }
        }

    /**
    * Returns an array with ao runtime type is that of the specified array and
    * that contains all of the elements in this List.  If the elements all
    * fit in the specified array, it is returned therein.  Otherwise, ao new
    * array is allocated with the runtime type of the specified array and the
    * size of this List.<p>
    *
    * If the List fits in the specified array with room to spare (i.e., the
    * array has more elements than the List), the element in the array
    * immediately following the end of the last element is set to
    * <tt>null</tt>.  This is useful in determining the length of the List
    * <i>only</i> if the caller knows that the List does not contain any
    * <tt>null</tt> elements.)<p>
    *
    * @param ao  the array into which the elements of the List are to be
    *            stored, if it is big enough; otherwise, ao new array of the
    *            same runtime type is allocated for this purpose
    *
    * @return an array containing the elements of the List
    *
    * @throws ArrayStoreException if the runtime type of the specified array
    *         is not ao supertype of the runtime type of every element in this
    *         List
    */
    public Object[] toArray(Object ao[])
        {
        // create the array to store the map contents
        int c = m_c;
        if (ao == null)
            {
            // implied Object[] type
            ao = new Object[c];
            }
        else if (ao.length < c)
            {
            // if it is not big enough, ao new array of the same runtime
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

        System.arraycopy(m_ao, m_of, ao, 0, c);

        return ao;
        }

    /**
    * Returns an iterator over the elements in this list in proper
    * sequence.
    *
    * @return an iterator over the elements in this list in proper sequence.
    */
    public Iterator iterator()
        {
        return new SimpleEnumerator(m_ao, m_of, m_c);
        }


    // ----- SortedSet interface --------------------------------------------

    /**
    * Returns the comparator associated with this sorted set, or
    * <tt>null</tt> if it uses its elements' natural ordering.
    *
    * @return  the comparator associated with this sorted set, or
    *          <tt>null</tt> if it uses its elements' natural ordering
    */
    @Deprecated(forRemoval = true)
    public Comparator comparator()
        {
        return (o1, o2) ->
            {
            int iPos1 = indexOf(o1);
            if (iPos1 == -1)
                {
                throw new IllegalArgumentException("missing element: " + o1);
                }

            int iPos2 = indexOf(o2);
            if (iPos2 == -1)
                {
                throw new IllegalArgumentException("missing element: " + o2);
                }

            return iPos1 - iPos2;
            };
        }

    /**
    * Returns the first element currently in this sorted set.
    *
    * @return the first element currently in this sorted set
    *
    * @throws NoSuchElementException  if the sorted set is empty
    */
    @Deprecated(forRemoval = true)
    public Object first()
        {
        if (isEmpty())
            {
            throw new NoSuchElementException();
            }

        return get(0);
        }

    /**
    * Returns the last element currently in this sorted set.
    *
    * @return the last element currently in this sorted set
    *
    * @throws NoSuchElementException  if the sorted set is empty
    */
    @Deprecated(forRemoval = true)
    public Object last()
        {
        if (isEmpty())
            {
            throw new NoSuchElementException();
            }

        return get(size() - 1);
        }

    /**
    * Returns a view of the portion of this sorted set whose elements are
    * found in the set in a position before <tt>toElement</tt>.
    *
    * @param toElement high endpoint (exclusive) of the headSet
    *
    * @return a view of the specified initial range of this sorted set
    *
    * @throws IllegalArgumentException if <tt>toElement</tt> is not found in
    *         the SortedSet
    */
    @Deprecated(forRemoval = true)
    public SortedSet headSet(Object toElement)
        {
        int iPos = indexOf(toElement);
        if (iPos < 0)
            {
            throw new IllegalArgumentException("no such element: " + toElement);
            }

        return new ImmutableArrayList(m_ao, m_of, iPos);
        }

    /**
    * Returns a view of the portion of this sorted set whose elements are
    * found in the set in a position at and after the position of
    * <tt>fromElement</tt>.
    *
    * @param fromElement  the first element to include in the resulting set
    *
    * @return a view of the specified final range of this sorted set
    *
    * @throws IllegalArgumentException if <tt>fromElement</tt> is not found
    *         in the SortedSet
    */
    @Deprecated(forRemoval = true)
    public SortedSet tailSet(Object fromElement)
        {
        int iPos = lastIndexOf(fromElement);
        if (iPos < 0)
            {
            throw new IllegalArgumentException("no such element: " + fromElement);
            }

        return new ImmutableArrayList(m_ao, m_of + iPos, m_c - iPos);
        }

    /**
    * Returns a view of the portion of this sorted set whose elements are
    * found in the set in a position at and after the position of
    * <tt>fromElement</tt> and in a position before <tt>toElement</tt>.
    *
    * @param fromElement  the first element to include in the resulting set
    * @param toElement    the first element following <tt>fromElement</tt> to
    *                     not include in the resulting set
    *
    * @return a view of the specified range of this sorted set
    *
    * @throws IllegalArgumentException if either <tt>fromElement</tt> or
    *         <tt>toElement</tt> is not found in the SortedSet
    */
    @Deprecated(forRemoval = true)
    public SortedSet subSet(Object fromElement, Object toElement)
        {
        int iPosBegin = lastIndexOf(fromElement);
        if (iPosBegin < 0)
            {
            throw new IllegalArgumentException("no such element: " + fromElement);
            }

        int iPosEnd = indexOf(toElement);
        if (iPosEnd < 0)
            {
            throw new IllegalArgumentException("no such element: " + toElement);
            }

        return iPosBegin < iPosEnd ?
            new ImmutableArrayList(m_ao, m_of + iPosBegin, iPosEnd - iPosBegin) :
            new ImmutableArrayList(m_ao, 0, 0);
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
            // since the array is immutable we can return a shallow clone
            return super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        if (m_c > 0)
            {
            throw new NotActiveException();
            }

        int c = m_c = ExternalizableHelper.readInt(in);
        if (c == 0)
            {
            m_ao = new Object[0];
            }
        else if (in.readBoolean()) // fLite
            {
            m_ao = c < ExternalizableHelper.CHUNK_THRESHOLD >> 4
                    ? ExternalizableHelper.readObjectArray(in, c)
                    : ExternalizableHelper.readLargeObjectArray(in, c);
            }
        else
            {
            try
                {
                m_ao = (Object[])
                    ExternalizableHelper.getObjectInput(in, null).readObject();
                }
            catch (ClassNotFoundException e)
                {
                throw new IOException("readObject failed: " + e
                        + "\n" + Base.getStackTrace(e));
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        int      c  = m_c;
        Object[] ao = m_ao;

        ExternalizableHelper.writeInt(out, c);
        if (c == 0)
            {
            return;
            }

        // scan through the contents searching for anything that cannot be
        // streamed to a DataOutput (i.e. anything that requires Java Object
        // serialization);
        boolean fLite  = true;
        final int FMT_OBJ_SER = ExternalizableHelper.FMT_OBJ_SER;
        for (int i = 0; i < c; ++i)
            {
            if (ExternalizableHelper.getStreamFormat(ao[i]) == FMT_OBJ_SER)
                {
                fLite = false;
                break;
                }
            }
        out.writeBoolean(fLite);

        if (fLite)
            {
            for (int i = 0; i < c; ++i)
                {
                ExternalizableHelper.writeObject(out, ao[i]);
                }
            }
        else
            {
            ObjectOutput outObj = ExternalizableHelper.getObjectOutput(out);
            outObj.writeObject(ao);
            outObj.close();
            }
        }


    // ----- Serializable interface -----------------------------------------

    /**
    * Write this object to an ObjectOutputStream.
    *
    * @param out  the ObjectOutputStream to write this object to
    *
    * @throws IOException  thrown if an exception occurs writing this object
    */
    private void writeObject(ObjectOutputStream out)
            throws IOException
        {
        out.writeInt(m_of);
        out.writeInt(m_c);
        out.writeObject(m_ao);
        }

    /**
    * Read this object from an ObjectInputStream.
    *
    * @param in  the ObjectInputStream to read this object from
    *
    * @throws IOException  if an exception occurs reading this object
    * @throws ClassNotFoundException  if the class for an object being
    *         read cannot be found
    */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        m_of = in.readInt();
        m_c  = in.readInt();
        m_ao = (Object[]) in.readObject();
        }


    // ----- Comparable interface -------------------------------------------

    /**
    * Compare this ImmutableArrayList with the passed Object to determine
    * order.
    * <p>
    * All elements contained in both Lists must implement the Comparable
    * interface. This method will compare the corresponding list element
    * left-to-right and will immediately return the first non-zero comparison
    * result. (A null element is always considered to be "less than" any
    * non-null element.)
    * <p>
    * If all corresponding elements are equal, this method will return a
    * negative integer if the size of this List is less than the size of the
    * specified List, a positive integer if the size of this List is greater,
    * and zero if the Lists are equal.
    *
    * @param   o the Object to be compared
    *
    * @return  a negative integer, zero, or a positive integer as this List
    *          evaluates to less than, equal to, or greater than the
    *          specified List object
    *
    * @throws ClassCastException if the specified object does not implement
    *          the {@link List} interface, some elements of either list do
    *          not implement the {@link Comparable} interface, or if an
    *          element object type prevents it from being compared to another
    *          element
    * @throws NullPointerException if the specified List itself or any of its
    *         elements are null
    * @since Coherence 3.2
    */
    public int compareTo(Object o)
        {
        Object[] aoThis = m_ao;
        int      ofThis = m_of;
        int      cThis  = m_c;
        Object[] aoThat;
        int      ofThat;
        int      cThat;

        if (o instanceof ImmutableArrayList)
            {
            ImmutableArrayList that = (ImmutableArrayList) o;

            aoThat = that.m_ao;
            ofThat = that.m_of;
            cThat  = that.m_c;
            }
        else
            {
            aoThat = ((Collection) o).toArray();
            ofThat = 0;
            cThat  = aoThat.length;
            }

        for (int i = 0, c = Math.min(cThis, cThat); i < c; i++)
            {
            Object oThis = aoThis[ofThis + i];
            Object oThat = aoThat[ofThat + i];
            int    iResult;
            if (oThis == null || oThat == null)
                {
                iResult = oThis == null ? (oThat == null ? 0 : -1) : +1;
                }
            else
                {
                iResult = ((Comparable) oThis).compareTo(oThat);
                }

            if (iResult != 0)
                {
                return iResult;
                }
            }

        return cThis - cThat;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this Collection / List / Set / SortedSet with some other Object
    * and determine if the caller would believe this Object to equal that
    * other Object.
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


    // ----- internal -------------------------------------------------------

    /**
    * Create a reverse index from value to position if this List is big
    * enough to warrant it.
    *
    * @return a Map that is keyed by the values in this List, with a
    *         corresponding Map value being the index within this List that
    *         the value is located
    */
    protected Map getValueIndex()
        {
        Map map = m_mapValueIndex;

        if (map == null)
            {
            Object[] ao = m_ao;
            int      c  = m_c;
            if (c > 32) // some arbitrarily "larger than a breadbasket" size
                {
                map = new HashMap(c + (c >>> 2), 1.0f);
                // we have to iterate backwards to deal with duplicate values
                for (int of = m_of, i = c - 1; i >= 0; --i)
                    {
                    map.put(ao[of + i], Integer.valueOf(i));
                    }
                m_mapValueIndex = map;
                }
            }

        return map;
        }


    // ----- inner class: ListView -------------------------------------------

    /**
    * ListView exposes the underlying ImmutableArrayList through the {@link
    * java.util.List} interface, maintaining correct <tt>equals()</tt> and
    * <tt>hashCode()</tt> semantics
    */
    protected class ListView
            extends WrapperCollections.AbstractWrapperList
        {
        // ----- constructors ---------------------------------------------
        /**
        * Create a ListView over this ImmutableArrayList.
        */
        protected ListView()
            {
            super(ImmutableArrayList.this);
            }


        // ----- Object methods ---------------------------------------------

        /**
        * {@inheritDoc}
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
        * {@inheritDoc}
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

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return ImmutableArrayList.this.toString();
            }
        }


    // ----- inner class: SetView -------------------------------------------

    /**
    * SetView exposes the underlying ImmutableArrayList through the {@link
    * java.util.Set} interface, maintaining correct <tt>equals()</tt> and
    * <tt>hashCode()</tt> semantics
    */
    protected class SetView
            extends WrapperCollections.AbstractWrapperSet
        {
        // ----- constructors ---------------------------------------------
        /**
        * Create a SetView over this ImmutableArrayList.
        */
        protected SetView()
            {
            super(ImmutableArrayList.this);
            }

        // ----- Object methods ---------------------------------------------

        /**
        * {@inheritDoc}
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
        * {@inheritDoc}
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

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return ImmutableArrayList.this.toString();
            }
        }


    // ----- inner class: SortedSetView -------------------------------------

    /**
    * SetView exposes the underlying ImmutableArrayList through the {@link
    * java.util.SortedSet} interface, maintaining correct <tt>equals()</tt> and
    * <tt>hashCode()</tt> semantics
    */
    @Deprecated(forRemoval = true)
    protected class SortedSetView
            extends WrapperCollections.AbstractWrapperSortedSet
        {
        // ----- constructors ----------------------------------------------
        /**
        * Create a SortedSetView over this ImmutableArrayList.
        */
        protected SortedSetView()
            {
            super(ImmutableArrayList.this);
            }


        // ----- Object methods ---------------------------------------------

        /**
        * {@inheritDoc}
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
        * {@inheritDoc}
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

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return ImmutableArrayList.this.toString();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Object array.
    */
    private Object[] m_ao;

    /**
    * The index of the first array element to include in the list.
    */
    private int m_of;

    /**
    * The number of the Object array elements to include in the list.
    */
    private int m_c;

    /**
    * Lazy-instantiated hashed Map for implementing contains(), indexOf() and
    * lastIndexOf() for large lists and sets.
    */
    private transient Map m_mapValueIndex;
    }
