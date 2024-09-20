/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import com.tangosol.util.Base;
import com.tangosol.util.SubSet;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.WrapperCollections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * The UnsafeSubSet is a SubSet which assumes that the backing set
 * already contains all of the keys that would be retained or removed.
 * <p>
 * This could be used for indexes (as an example), since all indexed keys also must
 * be contained in the backing map.
 *
 * @author coh 2011.06.02
 */
public class UnsafeSubSet<E>
        extends SubSet<E>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct this set based on the assigned set.
     *
     * @param set the WrapperSet to base this StableSubSet on
     * @param ctx the {@link BackingMapManagerContext} used to filter the keys in
     *            <tt>set</tt> by partition
     * @param parts a {@link PartitionSet} which will be used to filter all the
     *              contained keys from <tt>set</tt>, or <tt>null</tt> if
     *              all the keys apply
     */
    public UnsafeSubSet(Set<E> set, BackingMapManagerContext ctx, PartitionSet parts)
        {
        super(new WrapperSet<E>(set));
        m_ctx   = ctx;
        m_parts = parts;
        m_cOrig = set.size();
        }

    // ----- SubSet interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public WrapperSet<E> getOriginal()
        {
        return (WrapperSet<E>) super.getOriginal();
        }

    /**
     * {@inheritDoc}
     */
    public void resolve()
        {
        throw new UnsupportedOperationException("Resolve is not supported by UnsafeSubSet.");
        }

    /**
     * {@inheritDoc}
     */
    public boolean isModified()
        {
        return getOriginal().isInitialized() && (m_fModified || super.isModified());
        }

    /**
     * {@inheritDoc}
     */
    public void reset()
        {
        throw new UnsupportedOperationException("Reset is not supported by UnsafeSubSet.");
        }

    // ----- Set interface --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean removeAll(Collection col)
        {
        // if the first operation is a removeAll, we avoid the initial check against
        // the base set and assign the collection to the "modified" set directly.
        WrapperSet<E> setOrig = getOriginal();
        if (isModified())
            {
            return super.removeAll(col);
            }
        else
            {
            m_fModified = true;
            resetState(setOrig, collectSet(col), false);

            // important: this initializes the original set
            setOrig.initialize();
            return !col.isEmpty();
            }
        }

    /**
     * {@inheritDoc}
     */
    public boolean retainAll(Collection col)
        {
        // if the first operation is a retainAll, there is no need to
        // collect the base set; rather replace it with the passed in
        // collection. All other set operations will initialize the original set
        if (isModified())
            {
            return super.retainAll(col);
            }
        else
            {
            m_fModified = true;
            Set<E> setRetained = new WrapperSet<E>(collectSet(col));
            resetState(setRetained, null, false);

            // important: this "initializes" the set
            return !setRetained.isEmpty();
            }
        }

    @Override
    public int size()
        {
        Set setMod  = m_setMod;
        int cMod    = setMod == null ? 0 : setMod.size();
        Set setOrig = m_setOrig;

        if (m_fRetained) // retained has all elements
            {
            return cMod;
            }

        if (setMod == null || setMod.isEmpty()) // no elements were removed
            {
            return setOrig.size();
            }

        // setOrig can concurrently change therefore we must evaluate whether
        // the removed element still exists in the original set
        // Note: UnsafeSubSet is predominantly used during query evaluation and
        //       concurrent changes are handled by PartitionedCache

        int cSize = 0;
        for (Object o : setOrig)
            {
            if (!setMod.contains(o))
                {
                ++cSize;
                }
            }

        return cSize;
        }

    @Override
    public Object[] toArray()
        {
        // short-circuit if this set is empty, which will be the case frequently with small
        // caches, now that we are querying (and collecting keys) by partition
        if (isEmpty())
            {
            return EMPTY_ARRAY;
            }

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

        // the original set may have changed concurrently(after this Subset was created).
        // So the only way to correctly convert this SubSet into an array,
        // is to traverse the original set, and collect the elements which were not removed
        // by the iterator. Also calculate a best guess size for the resultant array.
        // If we assume that there were no concurrent modifications, then the size
        // of the resulting list will be original set size minus number of elements
        // which were deleted by an iterator of this set.

        int cOrig = setOrig.size();
        if (cOrig > 0)
            {
            int cEstimate = Math.max(cOrig - setMod.size(), 0);
            List listObjects = new ArrayList<>(cEstimate);

            for (Object o : setOrig)
                {
                if (!setMod.contains(o))
                    {
                    listObjects.add(o);
                    }
                }

            return listObjects.toArray();
            }

        return EMPTY_ARRAY;
        }

    // ----- protected members ----------------------------------------------

    /**
     * Extracts all keys in <tt>col</tt> where the key is contained by the PartitionSet or
     * null is no PartitionSet is defined return all entries. The entries in the new set
     * will be identical to the ones in <tt>col</tt>, however no other state is shared.
     *
     * @param col the collection that should be collected as a {@link Set}
     *
     * @return a new {@link Set} which contains identical entries to <tt>col</tt> but
     *         doesn't share any other state
     */
    protected Set<E> collectSet(Collection<E> col)
        {
        PartitionSet             parts = m_parts;
        BackingMapManagerContext ctx   = m_ctx;

        // superficially it looks like that we could just take a shortcut if the
        // partitions requested (parts) and the service owned partitions equals
        // each other. However, since there might be a transfer in-flight, the
        // collection could contain keys from "unowned" partitions
        Set<E> set = Base.newHashSet(col.size());
        for (E key : col)
            {
            if (parts.contains(ctx.getKeyPartition(key)))
                {
                set.add(key);
                }
            }

        return set;
        }

    @Override
    protected Set ensureRemoved()
        {
        try
            {
            return super.ensureRemoved();
            }
        finally
            {
            m_fModified = true;
            }
        }

    @Override
    protected Set ensureRetained()
        {
        try
            {
            return super.ensureRetained();
            }
        finally
            {
            m_fModified = true;
            }
        }

    // ----- inner classes --------------------------------------------------

    /**
     * The WrapperSet wraps a {@link Set} to provide a hook for initialization.
     */
    protected static class WrapperSet<E>
            extends WrapperCollections.AbstractWrapperSet<E>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a new WrapperSet based upon the specified set.
         *
         * @param set  the Set to realize when calling the delegate
         */
        protected WrapperSet(Set<E> set)
            {
            super(set);
            }

        // ----- AbstractWrapperSet interface -------------------------------

        /**
         * {@inheritDoc}
         */
        protected Set<E> getDelegate()
            {
            if (!isInitialized())
                {
                initialize();
                }

            return super.getDelegate();
            }

        // ----- WrapperSet methods -----------------------------------------

        /**
         * Initialize the underlying set.
         */
        protected void initialize()
            {
            m_fInitialized = true;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Check if the underlying set has been initialized.
         *
         * @return true iff the underlying set has been initialized
         */
        public boolean isInitialized()
            {
            return m_fInitialized;
            }

        // ----- data members -----------------------------------------------

        /**
         * True iff the underlying set is initialized.
         */
        boolean m_fInitialized;
        }

    // ----- data members -----------------------------------------------

    /**
     * True iff the set has been modified.
     */
    private boolean m_fModified;

    /**
     * The {@link BackingMapManagerContext} used to filter the base set keys by
     * partitions.
     */
    protected final BackingMapManagerContext m_ctx;

    /**
     * The {@link PartitionSet} which will be used to extract all the contained
     * keys from the base set.
     */
    protected final PartitionSet m_parts;

    /**
     * The size of the original set, when this {@link UnsafeSubSet} was created.
     */
    protected int m_cOrig;
    }
