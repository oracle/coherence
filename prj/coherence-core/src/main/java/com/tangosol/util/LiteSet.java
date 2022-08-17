/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ExternalizableLite;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.lang.reflect.Array;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
* An implementation of java.util.Set that is optimal (in terms of both size
* and speed) for very small sets of data but still works excellently with
* large sets of data.  This implementation is not thread-safe.
* <p>
* The LiteSet implementation switches at runtime between several different
* sub-implementations for storing the set of objects, described here:
* <ol>
* <li>"empty set" - a set that contains no data;
* <li>"single entry" - a reference directly to an item is used to represent
*     a set with exactly one item in it;
* <li>"Object[]" - a reference is held to an array of Objects that store
*     the contents of the Set; the item limit for this implementation is
*     determined by the THRESHOLD constant;
* <li>"delegation" - for more than THRESHOLD items, a set is created to
*     delegate the set management to; sub-classes can override the default
*     delegation class (java.util.HashSet) by overriding the factory method
*     {@link #instantiateSet() instantiateSet()}.
* </ol>
* <p>
* The LiteSet implementation supports the null value.
*
* @author cp 06/02/99
*/
public class LiteSet<E>
        extends AbstractSet<E>
        implements Cloneable, Externalizable, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a LiteSet
    */
    public LiteSet()
        {
        }

    /**
    * Construct a LiteSet containing the elements of the passed Collection.
    *
    * @param collection  a Collection
    */
    public LiteSet(Collection<? extends E> collection)
        {
        addAll(collection);
        }


    // ----- Set interface --------------------------------------------------

    /**
    * Determine if this Set is empty.
    *
    * @return true iff this Set is empty
    */
    public boolean isEmpty()
        {
        return m_nImpl == I_EMPTY;
        }

    /**
    * Returns the number of elements in this Set (its cardinality).
    *
    * @return the number of elements in this Set
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
                return ((Set<E>) m_oContents).size();

            default:
                throw new IllegalStateException();
            }
        }


    /**
    * Returns <tt>true</tt> if this Set contains the specified element.  More
    * formally, returns <tt>true</tt> if and only if this Set contains an
    * element <code>e</code> such that
    * <code>(o==null ? e==null : o.equals(e))</code>.
    *
    * @param o  the object to check for
    *
    * @return <tt>true</tt> if this Set contains the specified element
    */
    public boolean contains(Object o)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_SINGLE:
                return Base.equals(o, m_oContents);

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                // "Object[]" implementation
                Object[] ao = (Object[]) m_oContents;
                int      c  = m_nImpl - I_ARRAY_1 + 1;
                return indexOf(ao, c, o) >= 0;
                }

            case I_OTHER:
                return ((Set<E>) m_oContents).contains(o);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns an Iterator over the elements in this Set.  The elements are
    * returned in an arbitrary order.
    *
    * @return an iterator over the elements in this Set
    */
    public Iterator<E> iterator()
        {
        return isEmpty()
                ? NullImplementation.getIterator()
                : new Iterator<E>()
            {
            /**
            * Returns <tt>true</tt> if the iteration has more elements. (In
            * other words, returns <tt>true</tt> if <tt>next</tt> would
            * return an element rather than throwing an exception.)
            *
            * @return <tt>true</tt> if the iterator has more elements
            */
            public boolean hasNext()
                {
                return (m_iPrev + 1 < m_aVals.length);
                }

            /**
            * Returns the next element in the iteration.
            *
            * @return the next element in the iteration
            *
            * @exception NoSuchElementException iteration has no more
            *            elements
            */
            public E next()
                {
                int iNext = m_iPrev + 1;
                if (iNext < m_aVals.length)
                    {
                    m_iPrev      = iNext;
                    m_fCanRemove = true;
                    return (E) m_aVals[iNext];
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
                    LiteSet.this.remove(m_aVals[m_iPrev]);
                    }
                else
                    {
                    throw new IllegalStateException();
                    }
                }

            Object[] m_aVals      = LiteSet.this.toArray();
            int      m_iPrev      = -1;
            boolean  m_fCanRemove = false;
            };
        }

    /**
    * Returns an Enumerator over the elements in this Set.  The elements are
    * returned in an arbitrary order.
    *
    * @return an Enumerator over the elements in this Set
    */
    public Enumeration<E> elements()
        {
        return isEmpty()
                ? NullImplementation.getEnumeration()
                : new Enumeration<E>()
            {
            /**
            * Returns <tt>true</tt> if the Enumeration has more elements. (In
            * other words, returns <tt>true</tt> if <tt>nextElement</tt>
            * would return an element rather than throwing an exception.)
            *
            * @return <tt>true</tt> if the Enumeration has more elements
            */
            public boolean hasMoreElements()
                {
                return (m_iNext < m_aVals.length);
                }

            /**
            * Returns the next element in the Enumeration.
            *
            * @return the next element in the Enumeration
            *
            * @exception NoSuchElementException Enumeration has no more
            *            elements
            */
            public E nextElement()
                {
                if (m_iNext < m_aVals.length)
                    {
                    return (E) m_aVals[m_iNext++];
                    }
                else
                    {
                    throw new NoSuchElementException();
                    }
                }

            Object[] m_aVals = LiteSet.this.toArray();
            int      m_iNext = 0;
            };
        }

    /**
    * Returns an array containing all of the elements in this Set. Obeys the
    * general contract of the <tt>Set.toArray</tt> method.
    *
    * @return an array containing all of the elements in this Set
    */
    public Object[] toArray()
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return NO_OBJECTS;

            case I_SINGLE:
                return new Object[] {m_oContents};

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7:
                {
                // "Object[]" implementation
                Object[] ao = (Object[]) m_oContents;
                int      c  = m_nImpl - I_ARRAY_1 + 1;
                Object[] aoResult = new Object[c];
                System.arraycopy(ao, 0, aoResult, 0, c);
                return aoResult;
                }

            case I_ARRAY_8:
                return (Object[]) ((Object[]) m_oContents).clone();

            case I_OTHER:
                return ((Set) m_oContents).toArray();

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns an array (whose runtime type is that of the specified array)
    * containing all of the elements in this Set. Obeys the general contract
    * of the <tt>Set.toArray(Object[])</tt> method.
    *
    * @param aDest  the array into which the elements of this Set are to be
    *               stored, if it is big enough; otherwise, a new array of
    *               the same runtime type is allocated for this purpose
    *
    * @return an array containing the elements of this Set
    *
    * @throws  ArrayStoreException if the component type of <tt>aDest</tt> is
    *          not a supertype of the type of every element in this Set
    */
    public Object[] toArray(Object aDest[])
        {
        if (m_nImpl == I_OTHER)
            {
            return ((Set) m_oContents).toArray(aDest);
            }

        Object[] aSrc  = toArray();     // not optimal, but easy
        int      cSrc  = aSrc.length;
        int      cDest = aDest.length;

        if (cDest < cSrc)
            {
            cDest = cSrc;
            aDest = (Object[]) Array.newInstance(
                    aDest.getClass().getComponentType(), cDest);
            }

        if (cSrc > 0)
            {
            System.arraycopy(aSrc, 0, aDest, 0, cSrc);
            }

        if (cDest > cSrc)
            {
            aDest[cSrc] = null;
            }

        return aDest;
        }

    /**
    * Ensures that this Set contains the specified element. Returns
    * <tt>true</tt> if the Set changed as a result of the call. (Returns
    * <tt>false</tt> if this Set already contains the specified element.)
    *
    * @param o element to be added to this Set
    *
    * @return <tt>true</tt> if this Set did not already contain the
    *         specified element
    */
    public boolean add(E o)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                // growing from an empty set to the "single entry"
                // implementation
                m_nImpl     = I_SINGLE;
                m_oContents = o;
                return true;

            case I_SINGLE:
                {
                // check if this set already contains the object
                Object oContents = m_oContents;
                if (Base.equals(o, oContents))
                    {
                    return false;
                    }

                // growing from a "single entry" set to an "Object[]"
                // implementation
                Object[] ao = new Object[THRESHOLD];
                ao[0] = oContents;
                ao[1] = o;
                m_nImpl     = I_ARRAY_2;
                m_oContents = ao;
                return true;
                }

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                // "Object[]" implementation
                int      nImpl = m_nImpl;
                Object[] ao    = (Object[]) m_oContents;
                int      c     = nImpl - I_ARRAY_1 + 1;
                if (indexOf(ao, c, o) >= 0)
                    {
                    return false;
                    }

                // check if adding the object exceeds the "lite" threshold
                if (c >= THRESHOLD)
                    {
                    // time to switch to a different set implementation
                    Set set = instantiateSet();
                    set.addAll(this);
                    set.add(o);
                    m_nImpl     = I_OTHER;
                    m_oContents = set;
                    }
                else
                    {
                    // use the next available element in the array
                    ao[c]   = o;
                    m_nImpl = (byte) (nImpl + 1);
                    }

                return true;
                }

            case I_OTHER:
                return ((Set<E>) m_oContents).add(o);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Removes the specified element from this Set if it is present. More
    * formally, removes an element <code>e</code> such that
    * <code>(o==null ?  e==null : o.equals(e))</code>, if the Set contains
    * such an element. Returns <tt>true</tt> if the Set contained the
    * specified element (or equivalently, if the Set changed as a result of
    * the call). The Set will not contain the specified element once the call
    * returns.
    *
    * @param o  object to be removed from this Set, if present
    *
    * @return true if the Set contained the specified element
    */
    public boolean remove(Object o)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_SINGLE:
                {
                if (Base.equals(o, m_oContents))
                    {
                    // shrink to an "empty set"
                    m_nImpl     = I_EMPTY;
                    m_oContents = null;
                    return true;
                    }
                }
                return false;

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                {
                // "Object[]" implementation
                int      nImpl = m_nImpl;
                Object[] ao    = (Object[]) m_oContents;
                int      c     = nImpl - I_ARRAY_1 + 1;
                int      i     = indexOf(ao, c, o);
                if (i < 0)
                    {
                    return false;
                    }

                if (c == 1)
                    {
                    m_nImpl     = I_EMPTY;
                    m_oContents = null;
                    }
                else
                    {
                    System.arraycopy(ao, i + 1, ao, i, c - i - 1);
                    ao[c-1] = null;
                    m_nImpl = (byte) --nImpl;
                    }

                return true;
                }

            case I_OTHER:
                {
                Set     set      = (Set) m_oContents;
                boolean fRemoved = set.remove(o);
                if (fRemoved)
                    {
                    checkShrinkFromOther();
                    }
                return fRemoved;
                }

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns <tt>true</tt> if this Set contains all of the elements in the
    * specified Collection.
    *
    * @param collection  Collection to be checked for containment in this
    *                    Set
    *
    * @return <tt>true</tt> if this Set contains all of the elements in the
    *         specified Collection
    */
    public boolean containsAll(Collection<?> collection)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                // since this set is empty, so must the other be
                return collection.isEmpty();

            case I_OTHER:
                // (assume the delegatee is more efficient)
                return ((Set<E>) m_oContents).containsAll(collection);

            default:
                return super.containsAll(collection);
            }
        }

    /**
    * Adds all of the elements in the specified Collection to this Set
    * if they are not already present. If the specified Collection is also a
    * Set, the <tt>addAll</tt> operation effectively modifies this Set so
    * that its value is the <i>union</i> of the two Sets.
    *
    * @param collection  Collection whose elements are to be added to this
    *                    Set
    *
    * @return <tt>true</tt> if this Set changed as a result of the call
    */
    public boolean addAll(Collection<? extends E> collection)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                // "empty set" implementation (adding all to nothing is easy)
                {
                int c = collection.size();
                switch (c)
                    {
                    case 0:
                        return false;

                    case 1:
                        {
                        // growing from an empty set to the "single entry"
                        // implementation
                        m_nImpl     = I_SINGLE;
                        m_oContents = collection.iterator().next();
                        }
                        return true;

                    default:
                        return super.addAll(collection);
                    }
                }

            case I_OTHER:
                // (assume the delegatee is more efficient)
                return ((Set<E>) m_oContents).addAll(collection);

            default:
                return super.addAll(collection);
            }
        }

    /**
    * Retains only the elements in this Set that are contained in the
    * specified Collection. In other words, removes from this Set all of its
    * elements that are not contained in the specified Collection. If the
    * specified Collection is also a Set, this operation effectively modifies
    * this Set so that its value is the <i>intersection</i> of the two Sets.
    *
    * @param collection  collection that defines which elements this Set will
    *                    retain
    *
    * @return <tt>true</tt> if this Set changed as a result of the call
    */
    public boolean retainAll(Collection<?> collection)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_OTHER:
                // (assume the delegatee is more efficient)
                {
                boolean fChanged = ((Set<E>) m_oContents).retainAll(collection);
                if (fChanged)
                    {
                    checkShrinkFromOther();
                    }
                return fChanged;
                }

            default:
                return super.retainAll(collection);
            }
        }

    /**
    * Removes from this Set all of its elements that are contained in the
    * specified Collection. If the specified Collection is also a Set, this
    * operation effectively modifies this Set so that its value is the
    * <i>asymmetric set difference</i> of the two Sets.
    *
    * @param collection  Collection that defines which elements will be
    *                    removed from this Set
    *
    * @return <tt>true</tt> if this Set changed as a result of the call
    */
    public boolean removeAll(Collection<?> collection)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_OTHER:
                // (assume the delegatee is more efficient)
                {
                boolean fChanged = ((Set<E>) m_oContents).removeAll(collection);
                if (fChanged)
                    {
                    checkShrinkFromOther();
                    }
                return fChanged;
                }

            default:
                return super.removeAll(collection);
            }
        }

    /**
    * Removes all of the elements from this Set. This Set will be empty after
    * this call returns.
    */
    public void clear()
        {
        m_nImpl     = I_EMPTY;
        m_oContents = null;
        }


    // ----- Cloneable interface --------------------------------------------

    /**
    * Create a clone of this Set.
    *
    * @return a clone of this Set
    */
    public Object clone()
        {
        LiteSet that;
        try
            {
            that = (LiteSet) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        switch (this.m_nImpl)
            {
            case I_EMPTY:
            case I_SINGLE:
                // nothing to do
                break;

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                that.m_oContents = ((Object[]) this.m_oContents).clone();
                break;

            case I_OTHER:
                Set setThis = (Set) this.m_oContents;
                Set setThat = that.instantiateSet();
                setThat.addAll(setThis);
                that.m_oContents = setThat;
                break;

            default:
                throw new IllegalStateException();
            }

        return that;
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
        if (c > 0)
            {
            initFromArray((Object[]) in.readObject(), c);
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
        // format is int size followed by (if size > 0) an array of values;
        // note that the array size does not have to equal the Set size
        int nImpl = m_nImpl;
        switch (nImpl)
            {
            case I_EMPTY:
                out.writeInt(0);
                break;

            case I_SINGLE:
                out.writeInt(1);
                out.writeObject(new Object[] {m_oContents});
                break;

            case I_ARRAY_1: case I_ARRAY_2: case I_ARRAY_3: case I_ARRAY_4:
            case I_ARRAY_5: case I_ARRAY_6: case I_ARRAY_7: case I_ARRAY_8:
                out.writeInt(nImpl - I_ARRAY_1 + 1);
                out.writeObject((Object[]) m_oContents);
                break;

            case I_OTHER:
                Object[] ao = ((Set) m_oContents).toArray();
                out.writeInt(ao.length);
                out.writeObject(ao);
                break;

            default:
                throw new IllegalStateException();
            }
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        if (!isEmpty())
            {
            throw new NotActiveException();
            }

        boolean fLite = in.readBoolean();
        if (fLite)
            {
            readAndInitObjectArray(in);
            }
        else
            {
            Object[] ao = (Object[]) ExternalizableHelper.readObject(in);
            initFromArray(ao, ao.length);
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void writeExternal(DataOutput out)
            throws IOException
        {
        // scan through the contents searching for anything that cannot be
        // streamed to a DataOutput (i.e. anything that requires Java Object
        // serialization); note that the toArray() also resolves concerns
        // related to the synchronization of the data structure itself during
        // serialization
        boolean  fLite = true;
        Object[] ao    = toArray();
        int      c     = ao.length;
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
            ExternalizableHelper.writeInt(out, c);
            for (int i = 0; i < c; ++i)
                {
                ExternalizableHelper.writeObject(out, ao[i]);
                }
            }
        else
            {
            ExternalizableHelper.writeObject(out, ao);
            }
        }


    // ----- internal methods -----------------------------------------------

    /**
     * Read an array of objects from a DataInput stream and initialize
     * the internal structures of this set.
     *
     * @param in  a ObjectInputStream stream to read from
     *
     * @throws IOException if an I/O exception occurs
     *
     * @since 22.09
     */
    private void readAndInitObjectArray(DataInput in)
            throws IOException
        {
        int cLength = ExternalizableHelper.readInt(in);

        int cCap = cLength <= 1 || cLength > THRESHOLD ? cLength : THRESHOLD;

        // JEP-290 - ensure we can allocate this array
        ExternalizableHelper.validateLoadArray(Object[].class, cCap, in);

        Object[] oa = cCap <= 0
                          ? new Object[0]
                          : cCap < ExternalizableHelper.CHUNK_THRESHOLD >> 4
                              ? readObjectArray(in, cCap, cLength)
                              : readLargeObjectArray(in, cCap);

        initFromArray(oa, cLength);
        }

    /**
     * Read an array of the specified number of objects from a DataInput stream.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     * @param cRead    the number of elements to read
     *
     * @return an array of objects
     *
     * @throws IOException if an I/O exception occurs
     *
     * @since 22.09
     */
    private static Object[] readObjectArray(DataInput in, int cLength, int cRead)
            throws IOException
        {
        Object[] ao = new Object[cLength];
        for (int i = 0; i < cRead; i++)
            {
            ao[i] = ExternalizableHelper.readObject(in);
            }

        return ao;
        }

    /**
     * Read an array of objects with length larger than {@link ExternalizableHelper#CHUNK_THRESHOLD} {@literal >>} 4.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of objects
     *
     * @throws IOException if an I/O exception occurs
     *
     * @since 22.09
     */
    private static Object[] readLargeObjectArray(DataInput in, int cLength)
            throws IOException
        {
        int      cBatchMax = ExternalizableHelper.CHUNK_SIZE >> 4;
        int      cBatch    = cLength / cBatchMax + 1;
        Object[] aMerged   = null;
        int      cRead     = 0;
        int      cAllocate = cBatchMax;

        Object[] ao;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            ao      = readObjectArray(in, cAllocate, cAllocate);
            aMerged = ExternalizableHelper.mergeArray(aMerged, ao);
            cRead  += ao.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }


    /**
    * (Factory pattern) Instantiate a Set object to store items in once
    * the "lite" threshold has been exceeded. This method permits inheriting
    * classes to easily override the choice of the Set object.
    *
    * @return an instance of Set
    */
    protected Set<E> instantiateSet()
        {
        return new HashSet<>();
        }

    /**
    * Scan up to the first <tt>c</tt> elements of the passed array
    * <tt>ao</tt> looking for the specified Object <tt>o</tt>. If it is
    * found, return its position <tt>i</tt> in the array such that
    * <tt>(0 &lt;= i &lt; c)</tt>. If it is not found, return <tt>-1</tt>.
    *
    * @param ao  the array of objects to search
    * @param c   the number of elements in the array to search
    * @param o   the object to look for
    *
    * @return the index of the object, if found; otherwise -1
    */
    private int indexOf(Object[] ao, int c, Object o)
        {
        // first quick-scan by reference
        for (int i = 0; i < c; ++i)
            {
            if (o == ao[i])
                {
                return i;
                }
            }

        // slow scan by equals()
        if (o != null)
            {
            for (int i = 0; i < c; ++i)
                {
                if (o.equals(ao[i]))
                    {
                    return i;
                    }
                }
            }

        return -1;
        }

    /**
    * Initialize the contents of this Set from the passed array <tt>ao</tt>
    * containing <tt>c</tt> values.
    *
    * @param ao  the array that contains the values to place in this Set
    * @param c   the number of values that will be placed into this Set
    */
    protected void initFromArray(Object[] ao, int c)
        {
        switch (c)
            {
            case 0:
                m_oContents = null;
                m_nImpl     = I_EMPTY;
                break;

            case 1:
                m_oContents = ao[0];
                m_nImpl     = I_SINGLE;
                break;

            case 2: case 3: case 4: case 5: case 6: case 7: case 8:
                if (ao.length != THRESHOLD)
                    {
                    Object[] aoPresize = new Object[THRESHOLD];
                    System.arraycopy(ao, 0, aoPresize, 0, c);
                    ao = aoPresize;
                    }
                m_oContents = ao;
                m_nImpl     = (byte) (I_ARRAY_1 + c - 1);
                break;

            default:
                {
                Set set = instantiateSet();
                for (int i = 0; i < c; ++i)
                    {
                    set.add(ao[i]);
                    }
                m_oContents = set;
                m_nImpl     = I_OTHER;
                }
                break;
            }

        assert size() == c;
        }

    /**
    * After a mutation operation has reduced the size of an underlying Set,
    * check if the delegation model should be replaced with a more size-
    * efficient storage approach, and switch accordingly.
    */
    protected void checkShrinkFromOther()
        {
        assert m_nImpl == I_OTHER;

        // check if the set is now significantly below the "lite"
        // threshold
        Set set = (Set) m_oContents;
        int c = set.size();
        switch (c)
            {
            case 0:
                m_nImpl     = I_EMPTY;
                m_oContents = null;
                break;

            case 1:
                m_nImpl     = I_SINGLE;
                m_oContents = set.toArray()[0];
                break;

            case 2: case 3: case 4:
                {
                // shrink to "Object[]" implementation
                Object[] ao = set.toArray(new Object[THRESHOLD]);
                m_nImpl     = (byte) (I_ARRAY_1 + c - 1);
                m_oContents = ao;
                }
                break;
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * A constant array of zero size.  (This saves having to allocate what
    * should be a constant.)
    */
    private static final Object[] NO_OBJECTS = new Object[0];

    /**
    * The default point above which the LiteSet delegates to another set
    * implementation.
    */
    private static final int THRESHOLD = 8;

    /**
    * Implementation:  Empty set.
    */
    private static final int I_EMPTY = 0;
    /**
    * Implementation:  Single-item set.
    */
    private static final int I_SINGLE = 1;
    /**
    * Implementation:  Array set of 1 item.
    */
    private static final int I_ARRAY_1 = 2;
    /**
    * Implementation:  Array set of 2 items.
    */
    private static final int I_ARRAY_2 = 3;
    /**
    * Implementation:  Array set of 3 items.
    */
    private static final int I_ARRAY_3 = 4;
    /**
    * Implementation:  Array set of 4 items.
    */
    private static final int I_ARRAY_4 = 5;
    /**
    * Implementation:  Array set of 5 items.
    */
    private static final int I_ARRAY_5 = 6;
    /**
    * Implementation:  Array set of 6 items.
    */
    private static final int I_ARRAY_6 = 7;
    /**
    * Implementation:  Array set of 7 items.
    */
    private static final int I_ARRAY_7 = 8;
    /**
    * Implementation:  Array set of 8 items.
    */
    private static final int I_ARRAY_8 = 9;
    /**
    * Implementation:  Delegation.
    */
    private static final int I_OTHER = 10;


    // ----- data members ---------------------------------------------------

    /**
    * Implementation, one of I_EMPTY, I_SINGLE, I_ARRAY_* or I_OTHER.
    */
    private byte m_nImpl;

    /**
    * The set contents, based on the implementation being used.
    */
    private Object m_oContents;
    }
