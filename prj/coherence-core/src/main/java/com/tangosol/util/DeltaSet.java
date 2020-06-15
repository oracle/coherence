/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
* Implements a set which is based on another set, which is assumed to be
* immutable.
*
* @version 1.00, 11/30/98
* @author Cameron Purdy
*/
public class DeltaSet
        extends AbstractSet
        implements Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct this set based on an existing set.
    *
    * @param set  the set to base this delta set on
    */
    public DeltaSet(Set set)
        {
        m_setOrig = set;
        reset();
        }


    // ----- DeltaSet accessors ---------------------------------------------

    /**
    * Determine what items were in the original set.
    *
    * @return the set used to construct this DeltaSet
    */
    public Set getOriginal()
        {
        return m_setOrig;
        }

    /**
    * Determine if any items were added or removed.
    *
    * @return  true if the set has been modified
    */
    public boolean isModified()
        {
        return m_setAdded != null || m_setRemoved != null;
        }

    /**
    * Determine what items were added to the delta set.
    *
    * @return an immutable set of added items
    */
    public Set getAdded()
        {
        Set set = m_setAdded;
        return set == null ? EMPTY_SET : set;
        }

    /**
    * Determine what items were removed from the delta set.
    *
    * @return an immutable set of removed items
    */
    public Set getRemoved()
        {
        Set set = m_setRemoved;
        return set == null ? EMPTY_SET : set;
        }

    /**
    * Get a mutable set of items that were added to the delta set.
    *
    * @return a mutable set of added items
    */
    protected Set ensureAdded()
        {
        HashSet set = m_setAdded;
        if (set == null)
            {
            m_setAdded = set = new HashSet();
            }
        return set;
        }

    /**
    * Get a mutable set of items that were removed from the delta set.
    *
    * @return a mutable set of removed items
    */
    protected Set ensureRemoved()
        {
        HashSet set = m_setRemoved;
        if (set == null)
            {
            m_setRemoved = set = new HashSet();
            }
        return set;
        }

    /**
    * Apply the changes to the underlying set ("commit").
    */
    public void resolve()
        {
        Set setAdded   = getAdded();
        if (!setAdded.isEmpty())
            {
            m_setOrig.addAll(setAdded);
            }

        Set setRemoved = getRemoved();
        if (!setRemoved.isEmpty())
            {
            m_setOrig.removeAll(setRemoved);
            }

        reset();
        }

    /**
    * Discard the changes to the set ("rollback").
    */
    public void reset()
        {
        m_setAdded   = null;
        m_setRemoved = null;
        }


    // ----- Set interface --------------------------------------------------

    /**
    * Returns an Iterator over the elements contained in this Collection.
    *
    * @return an Iterator over the elements contained in this Collection
    */
    public Iterator iterator()
        {
        return isEmpty() ? EMPTY_ITERATOR : new DeltaIterator();
        }

    /**
    * Returns the number of elements in this Collection.
    *
    * @return the number of elements in this Collection
    */
    public int size()
        {
        return m_setOrig.size() + getAdded().size() - getRemoved().size();
        }

    /**
    * Returns true if this Collection contains the specified element.  More
    * formally, returns true if and only if this Collection contains at least
    * one element <code>e</code> such that <code>(o==null ? e==null :
    * o.equals(e))</code>.
    *
    * @param o  the object to search for in the set
    *
    * @return true if this set contains the specified object
    */
    public boolean contains(Object o)
        {
        return getAdded().contains(o) || (!getRemoved().contains(o) && m_setOrig.contains(o));
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
        if (getRemoved().contains(o))
            {
            ensureRemoved().remove(o);
            return true;
            }

        if (getAdded().contains(o) || m_setOrig.contains(o))
            {
            return false;
            }

        ensureAdded().add(o);
        return true;
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
    * @param o element to be removed from this Collection, if present
    *
    * @return true if the Collection contained the specified element
    */
    public boolean remove(Object o)
        {
        if (getRemoved().contains(o))
            {
            return false;
            }

        if (getAdded().contains(o))
            {
            ensureAdded().remove(o);
            return true;
            }

        if (m_setOrig.contains(o))
            {
            ensureRemoved().add(o);
            return true;
            }

        return false;
        }

    /**
    * Removes all of the elements from this Collection.
    */
    public void clear()
        {
        reset();
        ensureRemoved().addAll(m_setOrig);
        }


    /**
    * Returns an array containing all of the elements in this Set.
    * Obeys the general contract of Collection.toArray.
    *
    * @return an Object array containing all of the elements in this Set
    */
    public Object[] toArray()
        {
        int c = size();
        if (c == 0)
            {
            return EMPTY_ARRAY;
            }

        Object[] ao = new Object[c];

        Set setAdded = getAdded();
        c = setAdded.size();
        if (c > 0)
            {
            setAdded.toArray(ao);
            }

        Set setRemoved = getRemoved();
        for (Iterator iter = m_setOrig.iterator(); iter.hasNext(); )
            {
            Object o = iter.next();
            if (!setRemoved.contains(o))
                {
                ao[c++] = o;
                }
            }

        return ao;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Clone the delta set.
    *
    * @return a clone of this delta set
    */
    public Object clone()
        {
        DeltaSet dset;
        try
            {
            dset = (DeltaSet) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw new IllegalStateException();
            }

        HashSet setAdded = dset.m_setAdded;
        if (setAdded != null)
            {
            dset.m_setAdded = (HashSet) setAdded.clone();
            }

        HashSet setRemoved = dset.m_setRemoved;
        if (setRemoved != null)
            {
            dset.m_setRemoved = (HashSet) setRemoved.clone();
            }

        return dset;
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Iterator for the contents of a delta set.
    */
    protected class DeltaIterator implements Iterator
        {
        // ----- constructors -------------------------------------

        /**
        * Construct an iterator for a delta set.
        */
        protected DeltaIterator()
            {
            m_aObject = DeltaSet.this.toArray();
            }


        // ----- Iterator interface -----------------------------------------

        /**
        * Returns true if the iteration has more elements.
        */
        public boolean hasNext()
            {
            return m_index < m_aObject.length;
            }

        /**
        * Returns the next element in the interation.
        *
        * @exception NoSuchElementException iteration has no more elements.
        */
        public Object next()
            {
            if (m_index < m_aObject.length)
                {
                m_fRemovable = true;
                return m_aObject[m_index++];
                }

            m_fRemovable = false;
            throw new NoSuchElementException();
            }

        /**
        * Removes from the underlying Collection the last element returned by the
        * Iterator .  This method can be called only once per call to next  The
        * behavior of an Iterator is unspecified if the underlying Collection is
        * modified while the iteration is in progress in any way other than by
        * calling this method.  Optional operation.
        *
        * @exception IllegalStateException next has not yet been called,
        *            or remove has already been called after the last call
        *            to next.
        */
        public void remove()
            {
            if (!m_fRemovable)
                {
                throw new IllegalStateException();
                }

            m_fRemovable = false;
            DeltaSet.this.remove(m_aObject[m_index-1]);
            }


        // ----- data members -------------------------------------

        /**
        * Array of objects to iterate.
        */
        private Object[] m_aObject;

        /**
        * The next object to return.
        */
        private int m_index = 0;

        /**
        * True if last iterated item can be removed.
        */
        private boolean m_fRemovable = false;
        }


    // ----- constants ------------------------------------------------------

    /**
    * An empty immutable set.
    */
    private static final Set EMPTY_SET = NullImplementation.getSet();

    /**
    * An empty immutable iterator.
    */
    private static final Iterator EMPTY_ITERATOR = NullImplementation.getIterator();

    /**
    * An empty immutable array.
    */
    private static final Object[] EMPTY_ARRAY = new Object[0];


    // ----- data members ---------------------------------------------------

    /**
    * The underlying set (assumed immutable).
    */
    private Set m_setOrig;

    /**
    * The added items.
    */
    private HashSet m_setAdded;

    /**
    * The removed items.
    */
    private HashSet m_setRemoved;
    }
