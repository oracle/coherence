/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * An unmodifiable Set that provides access to many sets in the
 * given order.
 * <p>
 * Note that this implementation does not ensure that the elements are
 * unique across all chained sets. It is up to the user to provide that
 * guarantee.
 *
 * @param <E>  the type of Set elements
 *
 * @author as  2023.03.03
 * @since Coherence 23.03
 */
@SuppressWarnings("unchecked")
public class ChainedSet<E>
        extends AbstractSet<E>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ChainedSet with the provided Collection of Set
     * objects.
     *
     * @param col  a Collection of Set objects
     */
    public ChainedSet(Collection<Set<E>> col)
        {
        this(col.toArray(Set[]::new));
        }

    /**
     * Construct a ChainedSet from the existing ChainedSet and an additional Set
     * object.
     *
     * @param original  the original ChainedSet
     * @param set       a Set object to append
     */
    public ChainedSet(ChainedSet<E> original, Set<E> set)
        {
        f_aSets = Arrays.copyOf(original.f_aSets, original.f_aSets.length + 1);
        f_aSets[original.f_aSets.length] = set;
        }

    /**
     * Construct a ChainedSet from the existing ChainedSet and an array of Set
     * objects.
     *
     * @param original  the original ChainedSet
     * @param aSets     an array of Set objects
     */
    public ChainedSet(ChainedSet<E> original, Set<E>... aSets)
        {
        f_aSets = Arrays.copyOf(original.f_aSets, original.f_aSets.length + aSets.length);
        System.arraycopy(aSets, 0, f_aSets, original.f_aSets.length, aSets.length);
        }

    /**
     * Construct a ChainedSet with the provided array of Set
     * objects.
     *
     * @param aSets  an array of Set objects
     */
    public ChainedSet(Set<E>... aSets)
        {
        f_aSets = aSets;
        }

    // ----- Set interface --------------------------------------------------

    @Override
    public int size()
        {
        int cSize = 0;
        for (Set<E> set : f_aSets)
            {
            cSize += set.size();
            }
        return cSize;
        }

    @Override
    public boolean isEmpty()
        {
        for (Set<E> set : f_aSets)
            {
            if (!set.isEmpty())
                {
                return false;
                }
            }
        return true;
        }

    @Override
    public boolean contains(Object o)
        {
        for (Set<E> set : f_aSets)
            {
            if (set.contains(o))
                {
                return true;
                }
            }
        return false;
        }

    @Override
    public boolean containsAll(Collection<?> col)
        {
        for (Object o : col)
            {
            if (!contains(o))
                {
                return false;
                }
            }
        return true;
        }

    @Override
    public Iterator<E> iterator()
        {
        return new Iterator<>()
            {
            @Override
            public boolean hasNext()
                {
                Iterator<E> iter = m_iter;
                int         iSet = m_iSet;
                int         cSet = f_aSets.length;

                while ((iter == null || !iter.hasNext()) && ++iSet < cSet)
                    {
                    iter   = m_iter = f_aSets[iSet].iterator();
                    m_iSet = iSet;
                    }

                if (iSet >= cSet)
                    {
                    m_iter = null;
                    return false;
                    }
                return true;
                }

            @Override
            public E next()
                {
                if (hasNext())
                    {
                    return m_iter.next();
                    }
                throw new NoSuchElementException();
                }

            @Override
            public void remove()
                {
                if (m_iter == null)
                    {
                    throw new NoSuchElementException();
                    }
                m_iter.remove();
                }

            Iterator<E> m_iter;
            int m_iSet = -1;
            };
        }

    @Override
    public Object[] toArray()
        {
        return toArray((Object[]) null);
        }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator)
        {
        return toArray(generator.apply(size()));
        }

    @Override
    public <T> T[] toArray(T[] a)
        {
        int cSize = size();
        if (a == null || cSize > a.length)
            {
            a = (T[]) new Object[cSize];
            }

        for (int i = 0, of = 0, cArray = f_aSets.length; i < cArray; ++i)
            {
            Object[] aoCol = f_aSets[i].toArray();
            System.arraycopy(aoCol, 0, a, of, aoCol.length);
            of += aoCol.length;
            }

        return a;
        }

    // ----- unsupported methods --------------------------------------------

    @Override
    public boolean add(E e)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public boolean addAll(Collection<? extends E> c)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public boolean remove(Object o)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public boolean removeAll(Collection<?> c)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public boolean retainAll(Collection<?> c)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public void clear()
        {
        throw new UnsupportedOperationException();
        }

    // ----- data members ---------------------------------------------------

    /**
     * An array of Sets to enumerate.
     */
    protected final Set<E>[] f_aSets;
    }
