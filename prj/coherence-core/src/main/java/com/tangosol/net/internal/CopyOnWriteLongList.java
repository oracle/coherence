/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import java.util.Arrays;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * CopyOnWriteLongList provides a list of sorted longs that can be efficiently
 * stored and accessed avoiding boxing / unboxing.
 * <p>
 * Note: this list of longs does not permit {@code 0L} as a value.
 *
 * @author hr  2016.11.28
 * @since 12.2.3
 */
public class CopyOnWriteLongList
    {
    // ----- public methods -------------------------------------------------

    /**
     * Add the given long to this list.
     *
     * @param lValue  the value to add
     *
     * @return true if the value was added
     */
    public boolean add(long lValue)
        {
        Holder  holder, holderNew;
        boolean fAdded;
        do
            {
            holder    = m_holder;
            holderNew = cloneAndAdd(lValue, holder);
            }
        while ((fAdded = holderNew != holder) &&
                !f_atomicHolder.compareAndSet(this, holder, holderNew));

        return fAdded;
        }

    /**
     * Return true if the provided long is present in this data structure.
     *
     * @param lValue  the long value to check
     *
     * @return true if the provided long is present
     */
    public boolean contains(long lValue)
        {
        return Arrays.binarySearch(m_holder.m_alValues, lValue) >= 0;
        }

    /**
     * Remove the provided value from this data structure.
     *
     * @param lValue  the long value to remove
     *
     * @return true if the provided long was removed
     */
    public boolean remove(long lValue)
        {
        Holder  holder, holderNew;
        boolean fRemoved;
        do
            {
            holder    = m_holder;
            holderNew = cloneAndRemove(lValue, holder);
            }
        while ((fRemoved = holderNew != holder) &&
                !f_atomicHolder.compareAndSet(this, holder, holderNew));

        return fRemoved;
        }

    /**
     * Return the number of stored longs.
     * 
     * @return the number of stored longs
     */
    public int size()
        {
        return m_holder.m_cSize;
        }

    // ----- object methods -------------------------------------------------
    
    @Override
    public String toString()
        {
        return "CopyOnWriteLongList" + Arrays.toString(m_holder.m_alValues);
        }
    
    // ----- static helpers -------------------------------------------------

    /**
     * Return a clone of the {@link Holder} based on the provided Holder
     * with the removal of the provided long value.
     *
     * @param lValue  the long value to remove from the returned Holder
     * @param holder  the holder to base the returned Holder upon
     *
     * @return a new Holder with the provided long removed
     */
    private static Holder cloneAndAdd(long lValue, Holder holder)
        {
        boolean fForward = lValue < 0;
        boolean fGrow    = holder.isFull();
        long[]  al       = holder.m_alValues;
        int     iInsert  = Arrays.binarySearch(al, lValue);

        if (iInsert >= 0) // already exists
            {
            return holder;
            }

        long[] alNew = null;
        int    cSize = holder.m_cSize + 1;
        if (fGrow)
            {
            alNew = new long[al.length + 8];
            System.arraycopy(al, 0, alNew, 0, al.length);
            alNew[al.length] = lValue;
            Arrays.sort(alNew);
            }
        else
            {
            iInsert = Math.min(Math.max(-(iInsert + 1), 0), al.length);

            alNew = Arrays.copyOf(al, al.length);

            if (fForward)
                {
                alNew[iInsert] = lValue;
                for (int i = iInsert, c = al.length; al[i] != 0L && i < c; ++i)
                    {
                    alNew[i + 1] = al[i];
                    }
                }
            else
                {
                alNew[iInsert - 1] = lValue;
                for (int i = iInsert - 1; al[i] != 0L && i > 0; --i)
                    {
                    alNew[i - 1] = al[i];
                    }
                }
            }

        return new Holder(alNew, cSize);
        }

    /**
     * Return a clone of the {@link Holder} based on the provided Holder
     * with the addition of the provided long value.
     *
     * @param lValue  the long value to add to the returned Holder
     * @param holder  the holder to base the returned Holder upon
     *
     * @return a new Holder with the provided long included
     */
    private static Holder cloneAndRemove(long lValue, Holder holder)
        {
        long[] al     = holder.m_alValues;
        int    iValue = Arrays.binarySearch(al, lValue);

        if (iValue < 0)
            {
            return holder; // not present
            }

        if (holder.m_cSize == 1)
            {
            return EMPTY_HOLDER;
            }

        int    cCurrent = al.length;
        long[] alNew    = new long[cCurrent - 1];// = Arrays.copyOf(al, al.length);

        if (iValue > 0)
            {
            System.arraycopy(al, 0, alNew, 0, iValue);
            }

        if (iValue >= 0 && iValue < cCurrent - 1)
            {
            // there is a tail to copy
            System.arraycopy(al, iValue + 1, alNew, iValue, alNew.length - iValue);
            }

        return new Holder(alNew, holder.m_cSize - 1);
        }

    // ----- inner class: Holder --------------------------------------------

    /**
     * A class that holds the state that should be atomically consistent for
     * any changes to CopyOnWriteLongList.
     */
    protected static class Holder
        {
        // ----- constructors -----------------------------------------------

        /**
         * Contruct a Holder based on the provided long values and the number
         * of non-empty values (0L).
         *
         * @param alValues  the long values
         * @param cSize     the number of non-empty long values
         */
        protected Holder(long[] alValues, int cSize)
            {
            m_alValues = alValues;
            m_cSize    = cSize;
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Return true if this Holder
         *
         * @return
         */
        protected boolean isFull()
            {
            return m_alValues.length == m_cSize;
            }


        // ----- data members -----------------------------------------------

        /**
         * The long values.
         */
        protected long[] m_alValues;

        /**
         * The number of non-empty long values.
         */
        protected int m_cSize;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An empty Holder.
     */
    protected static final Holder EMPTY_HOLDER = new Holder(new long[0], 0);

    /**
     * Helper to CAS a Holder.
     */
    protected static final AtomicReferenceFieldUpdater f_atomicHolder =
                AtomicReferenceFieldUpdater.newUpdater(CopyOnWriteLongList.class, Holder.class, "m_holder");

    /**
     * The 'live' Holder.
     */
    protected volatile Holder m_holder = EMPTY_HOLDER;
    }
