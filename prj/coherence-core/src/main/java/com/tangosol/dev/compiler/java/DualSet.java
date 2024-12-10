/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.util.DeltaSet;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.NullImplementation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.NoSuchElementException;


/**
* Implements a set which can be treated as two different sets or as a single
* set.  This implementation is intended to be used for determination of
* definite assignment of variables in Java source code.
*
* From the Java Language Specification (16):
*
*   In order to precisely specify all the cases of definite assignment, the
*   rules in this section define two technical terms:
*       - whether a local variable is definitely assigned before a statement
*         or expression, and
*       - whether a local variable is definitely assigned after a statement
*         or expression.
*   In order to specify boolean-valued expressions, the latter notion is
*   refined into two cases:
*       - whether a local variable is definitely assigned after the
*         expression when true, and
*       - whether a local variable is definitely assigned after the
*         expression when false.
*
* The dual set implementation is typically used as a single set.  However,
* when dealing with those language constructs requiring "when true" vs.
* "when false" functionality, the dual set maintains two separate sets
* internally.  When the two sets are maintained, the dual set represents
* the union of the two sets; for example, if the dual set is used to track
* unassigned variables, then a variable is considered unassigned ("not
* definitely assigned") if it appears in either set.
*
* @version 1.00, 11/30/98
* @author Cameron Purdy
*/
public class DualSet extends AbstractSet
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct this set based on an existing set.
    *
    * @param set  the set to base this delta set on
    */
    public DualSet(Set set)
        {
        dsetTrue = new DeltaSet(set);
        }


    // ----- DualSet accessors ---------------------------------------------

    /**
    * Determine if any items were added or removed.
    *
    * @return  true if the set has been modified
    */
    public boolean isModified()
        {
        return dsetTrue.isModified() || dsetFalse != null && dsetFalse.isModified();
        }

    /**
    * Determine what items were added to the dual set.
    *
    * @return an immutable set of added items
    */
    public Set getAdded()
        {
        DeltaSet dsetTrue  = this.dsetTrue;
        DeltaSet dsetFalse = this.dsetFalse;

        if (dsetFalse == null)
            {
            return dsetTrue.getAdded();
            }

        // if one of the sets (when true/when false) is empty, then return
        // the items added to the other
        Set setAddedTrue  = dsetTrue .getAdded();
        Set setAddedFalse = dsetFalse.getAdded();
        if (setAddedTrue.isEmpty())
            {
            return setAddedFalse;
            }
        if (setAddedFalse.isEmpty())
            {
            return setAddedTrue;
            }

        // determine what items have been added to either set (union)
        HashSet setAdded = new HashSet(setAddedTrue);
        setAdded.addAll(setAddedFalse);
        return setAdded;
        }

    /**
    * Determine what items were removed from the dual set.
    *
    * @return an immutable set of removed items
    */
    public Set getRemoved()
        {
        DeltaSet dsetTrue  = this.dsetTrue;
        DeltaSet dsetFalse = this.dsetFalse;

        if (dsetFalse == null)
            {
            return dsetTrue.getRemoved();
            }

        // if one of the sets (when true/when false) is empty, then no
        // items have been removed from both
        Set setRemovedTrue  = dsetTrue .getRemoved();
        Set setRemovedFalse = dsetFalse.getRemoved();
        if (setRemovedTrue.isEmpty() || setRemovedFalse.isEmpty())
            {
            return EMPTY_SET;
            }

        // determine what items have been removed from both sets
        // (intersection)
        HashSet setRemoved = new HashSet(setRemovedTrue);
        setRemoved.retainAll(setRemovedFalse);
        return setRemoved;
        }

    /**
    * Determine what items are in the "when true" set.
    *
    * @return the "when true" DeltaSet
    */
    public DeltaSet getTrueSet()
        {
        if (dsetFalse == null)
            {
            dsetFalse = (DeltaSet) dsetTrue.clone();
            }

        return dsetTrue;
        }

    /**
    * Determine what items are in the "when false" set.
    *
    * @return the "when false" DeltaSet
    */
    public DeltaSet getFalseSet()
        {
        DeltaSet dset = dsetFalse;

        if (dset == null)
            {
            dsetFalse = dset = (DeltaSet) dsetTrue.clone();
            }

        return dset;
        }

    /**
    * Swap the "when true" and "when false" sets.
    */
    public void negate()
        {
        if (dsetFalse != null)
            {
            DeltaSet dset = dsetFalse;
            dsetFalse = dsetTrue;
            dsetTrue  = dset;
            }
        }

    /**
    * Determine if the dual set has not yet needed to be backed by two sets.
    *
    * @return  true if the true and false sets are one and the same
    */
    public boolean isSingle()
        {
        return dsetFalse == null;
        }

    /**
    * Merge the information in the "when true" and "when false" sets.
    */
    public void merge()
        {
        if (dsetFalse != null)
            {
            DeltaSet dsetTrue  = this.dsetTrue;
            DeltaSet dsetFalse = this.dsetFalse;

            // any items added to dsetFalse must be added to dsetTrue
            Set setAdded = dsetFalse.getAdded();
            if (!setAdded.isEmpty())
                {
                dsetTrue.addAll(setAdded);
                }

            // any items removed from dsetTrue but not from dsetFalse must
            // be un-removed from (added to) dsetTrue
            Set setRemovedTrue  = dsetTrue .getRemoved();
            Set setRemovedFalse = dsetFalse.getRemoved();
            if (!setRemovedTrue.equals(setRemovedFalse))
                {
                HashSet set = new HashSet(setRemovedTrue);
                set.removeAll(setRemovedFalse);
                dsetTrue.addAll(set);
                }

            // all changes are resolved; discard the second set
            dsetFalse = null;
            }
        }

    /**
    * Apply the changes to the underlying set ("commit").
    */
    public void resolve()
        {
        merge();
        dsetTrue.resolve();
        }

    /**
    * Discard the changes to the set.
    */
    public void reset()
        {
        dsetTrue.reset();
        dsetFalse = null;
        }


    // ----- Set interface --------------------------------------------------

    /**
    * Returns an Iterator over the elements contained in this Collection.
    *
    * @return an Iterator over the elements contained in this Collection
    */
    public Iterator iterator()
        {
        if (dsetFalse == null)
            {
            return dsetTrue.iterator();
            }
        else
            {
            return new DualIterator();
            }
        }

    /**
    * Returns the number of elements in this Collection.
    *
    * @return the number of elements in this Collection
    */
    public int size()
        {
        DeltaSet dsetTrue  = this.dsetTrue;
        DeltaSet dsetFalse = this.dsetFalse;

        int c = dsetTrue.size();
        if (dsetFalse == null)
            {
            return c;
            }

        Set setAddedFalse = dsetFalse.getAdded();
        if (setAddedFalse.isEmpty())
            {
            return c;
            }

        // since both delta sets are based on the same underlying set,
        // just check which items were added to the false set which were
        // not already counted in the "when true" delta set
        Set setAddedTrue = dsetTrue.getAdded();
        for (Iterator iter = setAddedFalse.iterator(); iter.hasNext(); )
            {
            if (!setAddedTrue.contains(iter.next()))
                {
                ++c;
                }
            }

        return c;
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
        if (dsetFalse == null)
            {
            return dsetTrue.contains(o);
            }

        return dsetTrue.contains(o) || dsetFalse.contains(o);
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
        boolean f = dsetTrue.add(o);

        if (dsetFalse != null)
            {
            f &= dsetFalse.add(o);
            }

        return f;
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
        boolean f = dsetTrue.remove(o);

        if (dsetFalse != null)
            {
            f &= dsetFalse.remove(o);
            }

        return f;
        }

    /**
    * Removes all of the elements from this Collection.
    */
    public void clear()
        {
        dsetTrue.clear();
        dsetFalse = null;
        }


    /**
    * Returns an array containing all of the elements in this Set.
    * Obeys the general contract of Collection.toArray.
    *
    * @return an Object array containing all of the elements in this Set
    */
    public Object[] toArray()
        {
        DeltaSet dsetTrue  = this.dsetTrue;
        DeltaSet dsetFalse = this.dsetFalse;

        if (dsetFalse == null || dsetFalse.getAdded().isEmpty())
            {
            return dsetTrue.toArray();
            }

        int c = size();
        Object[] ao = new Object[c];

        // get the items from the "when true" set
        c = dsetTrue.size();
        if (c > 0)
            {
            dsetTrue.toArray(ao);
            }

        // add any missing items from the "when false" set
        Set setAddedTrue  = dsetTrue .getAdded();
        Set setAddedFalse = dsetFalse.getAdded();
        for (Iterator iter = setAddedFalse.iterator(); iter.hasNext(); )
            {
            Object o = iter.next();
            if (!setAddedTrue.contains(o))
                {
                ao[c++] = o;
                }
            }

        return ao;
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Iterator for the contents of a dual set.
    */
    protected class DualIterator implements Iterator
        {
        /**
        * Construct an iterator for a dual set.
        */
        protected DualIterator()
            {
            ao = toArray();
            }

        /**
        * Returns true if the iteration has more elements.
        */
        public boolean hasNext()
            {
            return i < ao.length;
            }

        /**
        * Returns the next element in the interation.
        *
        * @exception NoSuchElementException iteration has no more elements.
        */
        public Object next()
            {
            if (i < ao.length)
                {
                fRemovable = true;
                return ao[i++];
                }

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
            if (!fRemovable)
                {
                throw new IllegalStateException();
                }

            DualSet.this.remove(ao[i-1]);
            fRemovable = false;
            }

        /**
        * Array of objects to iterate.
        */
        private Object[] ao;

        /**
        * The next object to return.
        */
        private int i = 0;

        /**
        * True if last iterated item can be removed.
        */
        private boolean fRemovable = false;
        }


    // ----- data members ---------------------------------------------------

    /**
    *
    */
    private DeltaSet dsetTrue;

    /**
    *
    */
    private DeltaSet dsetFalse;

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
    }
