/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.Predicate;
import com.oracle.coherence.common.collections.PredicateIterator;

import java.lang.reflect.Array;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Implements a set which is based on another set, which is assumed to be
 * immutable. The SubSet is assumed to be a subset of the underlying set, and
 * optimizes for both remove and retain operations. If the underlying (original)
 * set is sorted, the SubSet's {@link #iterator} will iterate items in the
 * ascending order.
 * <p>
 * This implementation is not thread-safe.
 *
 * @author cp 2003.01.29
 */
public class SubSet<E>
        extends AbstractSet<E>
        implements Cloneable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct this set based on an existing set.
     *
     * @param set  the set to base this subset on
     */
    public SubSet(Set<? extends E> set)
        {
        m_setOrig = (Set<E>) set;
        }


    // ----- SubSet accessors ---------------------------------------------

    /**
     * Determine what items were in the original set.
     *
     * @return the set used to construct this SubSet
     */
    public Set<? extends E> getOriginal()
        {
        return m_setOrig;
        }

    /**
     * Determine if the set has been modified.
     *
     * @return  true if any items have been removed
     */
    public boolean isModified()
        {
        Set setMod = m_setMod;
        return m_fRetained ? setMod == null || setMod.size() != m_setOrig.size()
                           : setMod != null && !setMod.isEmpty();
        }

    /**
     * Determine if the SubSet is tracking retained items versus removed
     * items.
     *
     * @return true if the SubSet is tracking just the retained items,
     *         false if the SubSet is tracking just the removed items
     */
    public boolean isTrackingRetained()
        {
        return m_fRetained;
        }

    /**
     * Determine what items were added to the subset. Do not modify the
     * returned set.
     *
     * @return a set of retained items
     */
    public Set<E> getRetained()
        {
        Set setMod = m_setMod;
        if (m_fRetained)
            {
            return setMod == null ? EMPTY_SET : setMod;
            }
        else
            {
            // the set of modifications represents removed items
            if (setMod == null || setMod.isEmpty())
                {
                return m_setOrig;
                }

            Set setOrig = m_setOrig;
            if (setMod.size() == setOrig.size())
                {
                return EMPTY_SET;
                }

            Set setRemain = instantiateModificationSet(setOrig.size());
            setRemain.addAll(setOrig);
            setRemain.removeAll(setMod);
            return setRemain;
            }
        }

    /**
     * Determine what items were removed from the subset.
     *
     * @return an immutable set of removed items
     */
    public Set<E> getRemoved()
        {
        Set setMod = m_setMod;
        if (m_fRetained)
            {
            // the set of modifications represents retained items
            if (setMod == null || setMod.isEmpty())
                {
                return m_setOrig;
                }

            Set setOrig = m_setOrig;
            if (setMod.size() == setOrig.size())
                {
                return EMPTY_SET;
                }

            // removed set doesn't have to be ordered
            Set setRemove = new OpenHashSet(setOrig);
            setRemove.removeAll(setMod);
            return setRemove;
            }
        else
            {
            return setMod == null ? EMPTY_SET : setMod;
            }
        }

    /**
     * Instantiate a new modification set containing either removed or
     * retained items.
     *
     * @param cSize an initial size of the modification set
     */
    protected Set<E> instantiateModificationSet(int cSize)
        {
        return m_setOrig instanceof SortedSet
            ? new TreeSet(((SortedSet) m_setOrig).comparator())
            : cSize == 0 ? new OpenHashSet() : new OpenHashSet(cSize);
        }

    /**
     * Get a mutable set of items that are retained in the subset.
     *
     * @return a mutable set of retained items
     */
    protected Set<E> ensureRetained()
        {
        Set<E> setMod = m_setMod;
        if (m_fRetained)
            {
            if (setMod == null)
                {
                m_setMod = setMod = instantiateModificationSet(0);
                }
            }
        else
            {
            Set<E> setOrig    = m_setOrig;
            Set<E> setRemoved = setMod;

            // new set to manage retained items
            m_setMod    = setMod = instantiateModificationSet(0);
            m_fRetained = true;

            if (setRemoved == null || setRemoved.isEmpty())
                {
                // nothing has been removed; implies everything is retained
                setMod.addAll(setOrig);
                }
            else
                {
                // some items may have been removed
                int cOrig    = setOrig.size();
                int cRemoved = setRemoved.size();
                if (cOrig != cRemoved)
                    {
                    // if more than 25% have been removed, then filter them
                    // out while building the retained set, otherwise make
                    // the retained set by copying the original set and
                    // removing just those items that have been removed from
                    // the SubSet
                    if (cRemoved << 2 > cOrig)
                        {
                        for (E e : setOrig)
                            {
                            if (!setRemoved.contains(e))
                                {
                                setMod.add(e);
                                }
                            }
                        }
                    else
                        {
                        setMod.addAll(setOrig);
                        setMod.removeAll(setRemoved);
                        }
                    }
                }
            }

        return setMod;
        }

    /**
     * Get a mutable set of items that are removed in the subset.
     *
     * @return a mutable set of removed items
     */
    protected Set<E> ensureRemoved()
        {
        if (m_fRetained)
            {
            throw new IllegalStateException();
            }

        Set setMod = m_setMod;
        if (setMod == null)
            {
            // removed set doesn't have to be ordered
            m_setMod = setMod = new OpenHashSet();
            }
        return setMod;
        }

    /**
     * Apply the changes to the underlying set ("commit").
     */
    public void resolve()
        {
        if (isModified())
            {
            Set setMod  = m_setMod;
            Set setOrig = m_setOrig;
            if (m_fRetained)
                {
                if (setMod == null || setMod.isEmpty())
                    {
                    setOrig.clear();
                    }
                else
                    {
                    setOrig.retainAll(setMod);
                    }
                }
            else
                {
                if (setMod != null && !setMod.isEmpty())
                    {
                    setOrig.removeAll(setMod);
                    }
                }
            }

        reset();
        }

    /**
     * Discard the changes to the set ("rollback").
     */
    public void reset()
        {
        m_setMod    = null;
        m_fRetained = false;
        }

    /**
     * Reset the state of the subset according to the specified parameters.
     *
     * @param setOrig    the new original set
     * @param setMod     the set of removed or retained entries, depending on
     *                   the <code>fRetained</code> flag
     * @param fRetained  if true <code>setMod</code> contains the retained entries;
     *                   otherwise the removed entries
     */
    protected void resetState(Set setOrig, Set setMod, boolean fRetained)
        {
        m_setOrig   = setOrig;
        m_setMod    = setMod;
        m_fRetained = fRetained;
        }


    // ----- Set interface --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator iterator()
        {
        return isEmpty()   ? EMPTY_ITERATOR :
               m_fRetained ? m_setMod.iterator() : new SubSetIterator();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
        {
        Set setMod  = m_setMod;
        Set setOrig = m_setOrig;

        // if the original set is empty, then the subset must be empty
        if (setOrig.isEmpty())
            {
            return true;
            }

        // if this set is retaining a subset, then this set is empty
        // only if nothing has been retained
        if (m_fRetained)
            {
            return setMod == null || setMod.isEmpty();
            }

        // this set is tracking removed items; if nothing has been removed
        // then this subset definitely is NOT empty
        if (setMod == null || setMod.isEmpty())
            {
            return false;
            }

        // otherwise, this set is empty only if all items have been removed
        return setMod.size() == setOrig.size();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
        {
        Set setMod = m_setMod;
        int cMod   = setMod == null ? 0 : setMod.size();

        return m_fRetained ? cMod : m_setOrig.size() - cMod;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o)
        {
        Set     setMod = m_setMod;
        boolean fInMod = setMod != null && setMod.contains(o);

        return m_fRetained ? fInMod : !fInMod && m_setOrig.contains(o);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> col)
        {
        Set setMod = m_setMod;
        if (m_fRetained)
            {
            // if the passed in set is empty we should return true even if
            // setMod is null
            return (setMod == null ? EMPTY_SET : setMod).containsAll(col);
            }
        else
            {
            // determine if there is any intersection between the removed
            // items managed by this SubSet and the passed collection
            if (setMod != null)
                {
                Collection colSmaller;
                Collection colLarger;
                if (col.size() > setMod.size())
                    {
                    colLarger  = col;
                    colSmaller = setMod;
                    }
                else
                    {
                    colLarger  = setMod;
                    colSmaller = col;
                    }

                for (Object o : colSmaller)
                    {
                    if (colLarger.contains(o))
                        {
                        return false;
                        }
                    }
                }

            return m_setOrig.containsAll(col);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(E e)
        {
        if (!m_setOrig.contains(e))
            {
            throw new UnsupportedOperationException("attempt to add an item to the SubSet"
                    + " that was not in the original set; item=\"" + e + "\"");
            }

        return m_fRetained ? ensureRetained().add(e) : ensureRemoved().remove(e);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(Collection<? extends E> col)
        {
        if (!m_setOrig.containsAll(col))
            {
            throw new UnsupportedOperationException("attempt to add items to the SubSet"
                    + " that were not in the original set; item collection=\"" + col + "\"");
            }

        return m_fRetained ? ensureRetained().addAll(col) : ensureRemoved().removeAll(col);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object o)
        {
        if (m_fRetained)
            {
            Set setMod = m_setMod;
            return setMod != null && setMod.remove(o);
            }
        else
            {
            return m_setOrig.contains(o) && ensureRemoved().add((E) o);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(Collection<?> col)
        {
        Set setMod = m_setMod;
        if (m_fRetained)
            {
            return setMod != null && !setMod.isEmpty() && setMod.removeAll(col);
            }
        else
            {
            // determine if this should be switched to a retain model
            Set setOrig  = m_setOrig;
            int cOrig    = setOrig.size();
            int cRemoved = setMod == null ? 0 : setMod.size();
            int cRemove  = col.size();

            if (cRemove == 0)
                {
                return false;
                }

            // if the percentage of removed is high enough (25% threshold)
            // switch to "retained"
            if (cOrig > 64 && (cRemove + cRemoved) > (cOrig >>> 2))
                {
                return ensureRetained().removeAll(col);
                }

            // relatively small number of removes; stick with "removing"
            setMod = ensureRemoved();

            boolean fMod = false;
            for (Object o : col)
                {
                if (!setMod.contains(o) && setOrig.contains(o))
                    {
                    setMod.add(o);
                    fMod = true;
                    }
                }
            return fMod;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(Collection<?> col)
        {
        assert col != null;

        // the execution of this method may call col.contains() thus an efficient
        // contains implementation is expected, i.e. col implements Set

        Set setMod  = m_setMod;
        int cPassed = col.size();

        if (m_fRetained)
            {
            setMod = ensureRetained();

            int cPrevSize = setMod.size();
            if (cPrevSize >= cPassed * 2)
                {
                // as the original set is at least twice the size of the passed
                // set it is cheaper to create a new retained set than remove
                // from the original
                retainAllInternal(col, setMod, null);
                return cPrevSize != m_setMod.size();
                }
            else
                {
                for (Iterator iter = setMod.iterator(); iter.hasNext(); )
                    {
                    if (!col.contains(iter.next()))
                        {
                        iter.remove();
                        }
                    }
                return cPrevSize != setMod.size();
                }
            }

        // switch to retain regardless
        m_fRetained = true;

        Set setOrig    = m_setOrig;
        int cOrig      = setOrig.size();
        Set setRemoved = null;
        int cRemoved   = 0;

        if (setMod != null && !setMod.isEmpty())
            {
            setRemoved = setMod;
            cRemoved   = setRemoved.size();
            }

        // optimization: decide on the cheaper walk (original or passed)
        if (cOrig >= cPassed)
            {
            retainAllInternal(col, setOrig, setRemoved);
            }
        else
            {
            retainAllInternal(setOrig, col, setRemoved);
            }

        return cOrig - cRemoved != m_setMod.size();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
        {
        m_fRetained = true;
        m_setMod    = null;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray()
        {
        // if this SubSet is tracking retained items, then simply get an
        // array of the retained items
        Set setMod = m_setMod;
        if (m_fRetained)
            {
            return setMod == null || setMod.isEmpty() ? EMPTY_ARRAY : setMod.toArray();
            }

        // this SubSet is tracking removed items; if nothing has been
        // removed, then simply get an array of the original items
        Set setOrig = m_setOrig;
        if (setMod == null || setMod.isEmpty())
            {
            return setOrig.toArray();
            }

        // otherwise it is necessary to get all original items that have NOT
        // been removed
        int      i  = 0;
        int      c  = Math.max(setOrig.size() - setMod.size(), 0);
        Object[] ao = new Object[c];

        if (c == 0)
            {
            return ao;
            }

        for (Object o : setOrig)
            {
            if (!setMod.contains(o))
                {
                ao[i++] = o;

                // an AIOBE on ao would happen if setOrig grows and the iterator
                // reflects the growth, break the iteration before it happens
                if (i == c)
                    {
                    break;
                    }
                }
            }

        // if setOrig would shrink during the iteration the returned array must
        // be resized accordingly
        return i < c ?  Arrays.copyOf(ao, i) : ao;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray(Object ao[])
        {
        Object[] aoActual = toArray();
        int      cActual  = aoActual.length;

        int c = ao.length;
        if (c < cActual)
            {
            // if it is not big enough, a new array of the same runtime
            // type is allocated
            ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), cActual);
            }
        else if (c > cActual)
            {
            // if the collection fits in the specified array with room to
            // spare, the element in the array immediately following the
            // end of the collection is set to null
            ao[cActual] = null;
            }

        // copy the data into the destination array
        System.arraycopy(aoActual, 0, ao, 0, cActual);

        return ao;
        }


    // ----- Object methods -------------------------------------------------

    /**
     * Clone the subset.
     *
     * @return a clone of this subset
     */
    @Override
    public Object clone()
        {
        SubSet setNew;
        try
            {
            setNew = (SubSet) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw new IllegalStateException();
            }

        Set setMod = setNew.m_setMod;
        if (setMod != null)
            {
            setNew.m_setMod = instantiateModificationSet(setMod.size());
            setNew.m_setMod.addAll(setMod);
            }

        return setNew;
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Instantiate a new retained set with all elements in the specified
     * collection that also exist in the provided {@code colMatch} collection
     * and are not excluded.
     *
     * @param colOuter    collection to iterate
     * @param colMatch    each element in colOuter should be present in this
     *                    collection such that {@code (colOuter ∩ colMatch)}
     * @param setExclude  optional set of excluded elements
     */
    protected void retainAllInternal(Collection colOuter, Collection colMatch, Set setExclude)
        {
        Set setMod = m_setMod = instantiateModificationSet(colOuter.size());

        for (Object o : colOuter)
            {
            if (setExclude == null || !setExclude.contains(o))
                {
                if (colMatch.contains(o))
                    {
                    setMod.add(o);
                    }
                }
            }
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Iterator for the contents of a subset in the "removed" mode.
     */
    protected class SubSetIterator
            extends PredicateIterator<E>
        {
        // ----- constructors -------------------------------------

        /**
         * Construct an iterator for a subset.
         */
        protected SubSetIterator()
            {
            super(m_setOrig.iterator(), NOT_REMOVED);
            }

        @Override
        public void remove()
            {
            if (m_fRetained)
                {
                throw new ConcurrentModificationException();
                }

            if (m_fPrev)
                {
                m_fPrev = false;
                ensureRemoved().add(m_next);
                }
            else
                {
                throw new IllegalStateException();
                }
            }
        }


    // ----- constants ------------------------------------------------------

    /**
     * An empty immutable set.
     */
    private static final Set EMPTY_SET = Collections.emptySet();

    /**
     * An empty immutable iterator.
     */
    private static final Iterator EMPTY_ITERATOR = EMPTY_SET.iterator();

    /**
     * An empty immutable array.
     */
    protected static final Object[] EMPTY_ARRAY = EMPTY_SET.toArray();

    /**
     * A Predicate evaluating the existence of an element in the "removed" mode.
     */
    private Predicate NOT_REMOVED = new Predicate()
        {
        public boolean evaluate(Object o)
            {
            if (m_fRetained)
                {
                throw new ConcurrentModificationException();
                }
            Set setMod = m_setMod;
            return setMod == null || !setMod.contains(o);
            }
        };

    // ----- data members ---------------------------------------------------

    /**
     * The underlying set (assumed immutable).
     */
    protected Set<E> m_setOrig;

    /**
     * The removed or retained items.
     */
    protected Set<E> m_setMod;

    /**
     * Toggles between whether the modifications are removed or retained.
     */
    protected boolean m_fRetained;
    }
