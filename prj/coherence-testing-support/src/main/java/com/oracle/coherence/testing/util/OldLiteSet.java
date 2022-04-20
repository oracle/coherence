/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.util;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
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
* <p>
* <ol>
* <li>  "empty set" - a set that contains no data;
* <li>  "single entry" - a reference directly to an item is used to represent
*       a set with exactly one item in it;
* <li>  "Object[]" - a reference is held to an array of Objects that store
*       the contents of the Set; the item limit for this implementation is
*       determined by the THRESHOLD constant;
* <li>  "delegation" - for more than THRESHOLD items, a set is created to
*       delegate the set management to; sub-classes can override the default
*       delegation class (java.util.HashSet) by overriding the factory method
*       instantiateSet.
* </ol>
* <p>
* The LiteSet implementation supports the null value.
*
* @author cp 06/02/99
*/
public class OldLiteSet
        extends AbstractSet
        implements Cloneable, ExternalizableLite, Externalizable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a OldLiteSet
    */
    public OldLiteSet()
        {
        }

    /**
    * Construct a OldLiteSet containing the elements of the passed Collection.
    *
    * @param collection  a Collection
    */
    public OldLiteSet(Collection collection)
        {
        addAll(collection);
        }


    // ----- Set interface --------------------------------------------------

    /**
    * Returns the number of elements in this Collection (its cardinality).  If this
    * Collection contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
    * <tt>Integer.MAX_VALUE</tt>.
    *
    * @return the number of elements in this Collection (its cardinality).
    */
    public int size()
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return 0;

            case I_SINGLE:
                return 1;

            case I_ARRAY:
                return ((Object[]) m_oContents).length;

            case I_OTHER:
                return ((Set) m_oContents).size();

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns <tt>true</tt> if this Collection contains the specified element.  More
    * formally, returns <tt>true</tt> if and only if this Collection contains an
    * element <code>e</code> such that <code>(o==null ? e==null :
    * o.equals(e))</code>.
    *
    * @return <tt>true</tt> if this Collection contains the specified element.
    */
    public boolean contains(Object o)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_SINGLE:
                {
                Object oContents = m_oContents;
                return (o == null ? oContents == null : o.equals(oContents));
                }

            case I_ARRAY:
                {
                // "Object[]" implementation
                Object[] a = (Object[]) m_oContents;
                int      c = a.length;
                for (int i = 0; i < c; ++i)
                    {
                    if (o == null ? a[i] == null : o.equals(a[i]))
                        {
                        return true;
                        }
                    }
                }
                return false;

            case I_OTHER:
                return ((Set) m_oContents).contains(o);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns an iterator over the elements in this Collection.  The elements are
    * returned in no particular order (unless this Collection is an instance of some
    * class that provides a guarantee).
    *
    * @return an iterator over the elements in this Collection.
    */
    public Iterator iterator()
        {
        if (isEmpty())
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
                return (iPrev + 1 < aVals.length);
                }

            /**
            * Returns the next element in the iteration.
            *
            * @return the next element in the iteration
            *
            * @exception NoSuchElementException iteration has no more elements
            */
            public Object next()
                {
                if (iPrev + 1 >= aVals.length)
                    {
                    throw new NoSuchElementException();
                    }


                fCanRemove = true;
                return aVals[++iPrev];
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
                OldLiteSet.this.remove(aVals[iPrev]);
                }

            Object[] aVals      = toArray();
            int      iPrev      = -1;
            boolean  fCanRemove = false;
            };
        }

    /**
    * Returns an enumerator over the elements in this Collection.  The elements are
    * returned in no particular order (unless this Collection is an instance of some
    * class that provides a guarantee).
    *
    * @return an enumerator over the elements in this Collection.
    */
    public Enumeration elements()
        {
        if (isEmpty())
            {
            return NullImplementation.getEnumeration();
            }

        return new Enumeration()
            {
            /**
            * Returns <tt>true</tt> if the enumeration has more elements. (In other
            * words, returns <tt>true</tt> if <tt>nextElement</tt> would return an element
            * rather than throwing an exception.)
            *
            * @return <tt>true</tt> if the enumeration has more elements
            */
            public boolean hasMoreElements()
                {
                return (iNext < aVals.length);
                }

            /**
            * Returns the next element in the enumeration.
            *
            * @return the next element in the enumeration
            *
            * @exception NoSuchElementException enumeration has no more elements
            */
            public Object nextElement()
                {
                if (iNext >= aVals.length)
                    {
                    throw new NoSuchElementException();
                    }

                return aVals[iNext++];
                }

            Object[] aVals = toArray();
            int      iNext = 0;
            };
        }

    /**
    * Returns an array containing all of the elements in this Collection.
    * Obeys the general contract of the <tt>Collection.toArray</tt> method.
    *
    * @return an array containing all of the elements in this Collection.
    */
    public Object[] toArray()
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return NO_OBJECTS;

            case I_SINGLE:
                return new Object[] {m_oContents};

            case I_ARRAY:
                return (Object[]) ((Object[]) m_oContents).clone();

            case I_OTHER:
                return ((Set) m_oContents).toArray();

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns an array containing all of the elements in this Collection whose
    * runtime type is that of the specified array.  Obeys the general
    * contract of the <tt>Collection.toArray(Object[])</tt> method.
    *
    * @param aDest  the array into which the elements of this Collection are to
    *		be stored, if it is big enough; otherwise, a new array of the
    * 		same runtime type is allocated for this purpose.
    * @return   an array containing the elements of this Collection.
    * @throws   ArrayStoreException the runtime type of a is not a supertype
    *           of the runtime type of every element in this Collection.
    */
    public Object[] toArray(Object aDest[])
        {
        Object[] aSrc  = toArray();     // not optimal, but easy
        int      cSrc  = aSrc.length;
        int      cDest = aDest.length;

        if (cDest < cSrc)
            {
            cDest = cSrc;
            aDest = (Object[]) Array.newInstance(aDest.getClass().getComponentType(), cDest);
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
    * @param o element to be added to this Collection.
    * @return <tt>true</tt> if this Collection did not already contain the specified
    *         element.
    *
    * @throws UnsupportedOperationException if the <tt>add</tt> method is not
    * 	       supported by this Collection.
    */
    public boolean add(Object o)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                // growing from an empty set to the "single entry" implementation
                m_nImpl     = I_SINGLE;
                m_oContents = o;
                return true;

            case I_SINGLE:
                {
                // check if this set already contains the object
                Object oContents = m_oContents;
                if (o == null ? oContents == null : o.equals(oContents))
                    {
                    return false;
                    }

                // growing from a "single entry" set to an "Object[]"
                // implementation
                Object[] a = new Object[2];
                a[0] = oContents;
                a[1] = o;
                m_nImpl     = I_ARRAY;
                m_oContents = a;
                return true;
                }

            case I_ARRAY:
                {
                // "Object[]" implementation
                Object[] a = (Object[]) m_oContents;
                int      c = a.length;

                // check if this set already contains the object
                for (int i = 0; i < c; ++i)
                    {
                    if (o == null ? a[i] == null : o.equals(a[i]))
                        {
                        return false;
                        }
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
                    // grow the array
                    Object[] aNew = new Object[c+1];
                    System.arraycopy(a, 0, aNew, 0, c);
                    aNew[c] = o;
                    m_oContents = aNew;
                    }

                return true;
                }

            case I_OTHER:
                return ((Set) m_oContents).add(o);

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Removes the specified element from this Collection if it is present (optional
    * operation).  More formally, removes an element <code>e</code> such that
    * <code>(o==null ?  e==null : o.equals(e))</code>, if the Collection contains
    * such an element.  Returns <tt>true</tt> if the Collection contained the
    * specified element (or equivalently, if the Collection changed as a result of
    * the call).  (The Collection will not contain the specified element once the
    * call returns.)
    *
    * @param o object to be removed from this Collection, if present.
    * @return true if the Collection contained the specified element.
    *
    * @throws UnsupportedOperationException if the <tt>remove</tt> method is
    *         not supported by this Collection.
    */
    public boolean remove(Object o)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_SINGLE:
                {
                Object oContents = m_oContents;
                if (o == null ? oContents == null : o.equals(oContents))
                    {
                    // shrink to an "empty set"
                    m_nImpl     = I_EMPTY;
                    m_oContents = null;
                    return true;
                    }
                }
                return false;

            case I_ARRAY:
                {
                Object[] a = (Object[]) m_oContents;
                int      c = a.length;

                // find the object
                for (int i = 0; i < c; ++i)
                    {
                    if (o == null ? a[i] == null : o.equals(a[i]))
                        {
                        if (c == 2)
                            {
                            // shrink to "single entry" implementation
                            m_nImpl     = I_SINGLE;
                            m_oContents = a[i^1];
                            }
                        else
                            {
                            // shrink the array
                            Object[] aNew = new Object[c-1];
                            System.arraycopy(a, 0, aNew, 0, i);
                            System.arraycopy(a, i + 1, aNew, i, c - i - 1);
                            m_oContents = aNew;
                            }

                        return true;
                        }
                    }
                }
                return false;

            case I_OTHER:
                {
                Set     set      = (Set) m_oContents;
                boolean fRemoved = set.remove(o);

                // check if the set is now below the "lite" threshold
                if (fRemoved && set.size() < THRESHOLD)
                    {
                    // shrink to "Object[]" implementation
                    m_nImpl     = I_ARRAY;
                    m_oContents = set.toArray();
                    }

                return fRemoved;
                }

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Returns <tt>true</tt> if this collection contains all of the elements
    * in the specified collection. <p>
    *
    * This implementation iterates over the specified collection, checking
    * each element returned by the iterator in turn to see if it's
    * contained in this collection.  If all elements are so contained
    * <tt>true</tt> is returned, otherwise <tt>false</tt>.
    *
    * @param collection collection to be checked for containment in this
    *                   collection.
    * @return <tt>true</tt> if this collection contains all of the elements
    * 	       in the specified collection.
    *
    * @see #contains(Object)
    */
    public boolean containsAll(Collection collection)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                // since this set is empty, so must the other be
                return collection.isEmpty();

            case I_OTHER:
                // (assume the delegatee is more efficient)
                return ((Set) m_oContents).containsAll(collection);

            default:
                return super.containsAll(collection);
            }
        }

    /**
    * Adds all of the elements in the specified collection to this Collection if
    * they're not already present (optional operation).  If the specified
    * collection is also a Collection, the <tt>addAll</tt> operation effectively
    * modifies this Collection so that its value is the <i>union</i> of the two
    * Collections.  The behavior of this operation is unspecified if the specified
    * collection is modified while the operation is in progress.
    *
    * @param collection collection whose elements are to be added to this
    *                   Collection.
    * @return <tt>true</tt> if this Collection changed as a result of the call.
    *
    * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
    * 		  not supported by this Collection.
    */
    public boolean addAll(Collection collection)
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
                        // growing from an empty set to the "single entry" implementation
                        m_nImpl     = I_SINGLE;
                        m_oContents = collection.iterator().next();
                        }
                        return true;

                    default:
                        {
                        // the passed collection needs to be turned into a Set
                        // (to ensure no duplicates if the "Object[]" implementation
                        // will be used or because it will be delegated to)
                        Set set;
                        if (c <= THRESHOLD && collection instanceof Set)
                            {
                            // the passed collection is already a Set and since
                            // it is small enough to use the "Object[]"
                            // implementation, it will be discarded after using
                            // its toArray method
                            set = (Set) collection;
                            }
                        else
                            {
                            set = instantiateSet();
                            set.addAll(collection);
                            }

                        if (c <= THRESHOLD)
                            {
                            // use the "Object[]" implementation
                            m_nImpl     = I_ARRAY;
                            m_oContents = set.toArray();
                            }
                        else
                            {
                            // use the "delegation" implementation
                            m_nImpl     = I_OTHER;
                            m_oContents = set;
                            }
                        }
                        return true;
                    }
                }

            case I_OTHER:
                // (assume the delegatee is more efficient)
                return ((Set) m_oContents).addAll(collection);

            default:
                return super.addAll(collection);
            }
        }

    /**
    * Retains only the elements in this Collection that are contained in the
    * specified collection (optional operation).  In other words, removes
    * from this Collection all of its elements that are not contained in the
    * specified collection.  If the specified collection is also a Collection, this
    * operation effectively modifies this Collection so that its value is the
    * <i>intersection</i> of the two Collections.
    *
    * @param collection collection that defines which elements this Collection
    *                   will retain.
    * @return <tt>true</tt> if this collection changed as a result of the
    *         call.
    *
    * @throws UnsupportedOperationException if the <tt>retainAll</tt> method
    * 		  is not supported by this Collection.
    */
    public boolean retainAll(Collection collection)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_OTHER:
                // (assume the delegatee is more efficient)
                return ((Set) m_oContents).retainAll(collection);

            default:
                return super.retainAll(collection);
            }
        }

    /**
    * Removes from this Collection all of its elements that are contained in the
    * specified collection (optional operation).  If the specified
    * collection is also a Collection, this operation effectively modifies this
    * Collection so that its value is the <i>asymmetric Collection difference</i> of
    * the two Collections.
    *
    * @param collection collection that defines which elements will be removed
    *                   from this Collection.
    * @return <tt>true</tt> if this Collection changed as a result of the call.
    *
    * @throws UnsupportedOperationException if the <tt>removeAll</tt>
    * 		  method is not supported by this Collection.
    */
    public boolean removeAll(Collection collection)
        {
        switch (m_nImpl)
            {
            case I_EMPTY:
                return false;

            case I_OTHER:
                // (assume the delegatee is more efficient)
                return ((Set) m_oContents).removeAll(collection);

            default:
                return super.removeAll(collection);
            }
        }

    /**
    * Removes all of the elements from this Collection (optional operation).
    * This Collection will be empty after this call returns (unless it throws an
    * exception).
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
            throws CloneNotSupportedException
        {
        try
            {
            OldLiteSet that = (OldLiteSet) super.clone();

            switch (this.m_nImpl)
                {
                case I_EMPTY:
                case I_SINGLE:
                    // nothing to do
                    break;

                case I_ARRAY:
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
        Set set = instantiateSet();
        ExternalizableHelper.readCollection(in, set, null);
        addAll(set);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeCollection(out, this);
        }


    // ----- Externalizable interface ---------------------------------------

    /**
    * @see Externalizable#readExternal(ObjectInput)
    */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException
        {
        readExternal((DataInput) in);
        }

    /**
    * @see Externalizable#writeExternal(ObjectOutput)
    */
    public void writeExternal(ObjectOutput out)
            throws IOException
        {
        writeExternal((DataOutput) out);
        }


    // ----- inner methods --------------------------------------------------

    /**
    * (Factory pattern) Instantiate a Set object to store items in once
    * the "lite" threshold has been exceeded. This method permits inheriting
    * classes to easily override the choice of the Set object.
    *
    * @return an instance of Set
    */
    protected Set instantiateSet()
        {
        return new HashSet();
        }


    // ----- constants ------------------------------------------------------

    /**
    * A constant array of zero size.  (This saves having to allocate what
    * should be a constant.)
    */
    private static final Object[] NO_OBJECTS = new Object[0];

    /**
    * The default point above which the OldLiteSet delegates to another set
    * implementation.
    */
    private static final int THRESHOLD = 7;

    /**
    * Implementation:  Empty set.
    */
    private static final int I_EMPTY = 0;
    /**
    * Implementation:  Single-item set.
    */
    private static final int I_SINGLE = 1;
    /**
    * Implementation:  Array set.
    */
    private static final int I_ARRAY = 2;
    /**
    * Implementation:  Delegation.
    */
    private static final int I_OTHER = 3;


    // ----- data members ---------------------------------------------------

    /**
    * Implementation, one of I_EMPTY, I_SINGLE, I_ARRAY or I_OTHER.
    */
    private int m_nImpl;

    /**
    * The set contents, based on the implementation being used.
    */
    private Object m_oContents;
    }
