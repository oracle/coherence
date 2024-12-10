/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.function.IntFunction;

/**
 * An unmodifiable Collection that provides access to many collections in the
 * given order.
 *
 * @author hr  2014.02.19
 * @since Coherence 12.1.2
 */
@SuppressWarnings("unchecked")
public class ChainedCollection<E>
        extends AbstractCollection<E>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ChainedCollection with the provided Collection of Collection
     * objects.
     *
     * @param col  a Collection of Collection objects
     */
    public ChainedCollection(Collection<Collection<E>> col)
        {
        this(col.toArray(Collection[]::new));
        }

    /**
     * Construct a ChainedCollection from the existing ChainedCollection and
     * an additional Collection object.
     *
     * @param original  the original ChainedCollection
     * @param col       a Collection object to append
     */
    public ChainedCollection(ChainedCollection<E> original, Collection<E> col)
        {
        f_aCol = Arrays.copyOf(original.f_aCol, original.f_aCol.length + 1);
        f_aCol[original.f_aCol.length] = col;
        }

    /**
     * Construct a ChainedCollection from the existing ChainedCollection and
     * an array of Collection objects.
     *
     * @param original  the original ChainedSet
     * @param aCol      an array of Collection objects
     */
    @SafeVarargs
    public ChainedCollection(ChainedCollection<E> original, Collection<E>... aCol)
        {
        f_aCol = Arrays.copyOf(original.f_aCol, original.f_aCol.length + aCol.length);
        System.arraycopy(aCol, 0, f_aCol, original.f_aCol.length, aCol.length);
        }

    /**
     * Construct a ChainedCollection with the provided array of Collection
     * objects.
     *
     * @param aCol  an array of Collection objects
     */
    public ChainedCollection(Collection<E>[] aCol)
        {
        f_aCol = aCol;
        }

    // ----- Collection interface -------------------------------------------

    @Override
    public int size()
        {
        int cSize = 0;
        for (Collection<E> col : f_aCol)
            {
            cSize += col.size();
            }
        return cSize;
        }

    @Override
    public boolean isEmpty()
        {
        for (Collection<E> col : f_aCol)
            {
            if (!col.isEmpty())
                {
                return false;
                }
            }
        return true;
        }

    @Override
    public boolean contains(Object o)
        {
        for (Collection<E> col : f_aCol)
            {
            if (col.contains(o))
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
        return new Iterator<E>()
            {
            @Override
            public boolean hasNext()
                {
                Iterator<E> iter = m_iter;
                int         iCol = m_iCol;
                int         cCol = f_aCol.length;

                while ((iter == null || !iter.hasNext()) && ++iCol < cCol)
                    {
                    iter   = m_iter = f_aCol[iCol].iterator();
                    m_iCol = iCol;
                    }

                if (iCol >= cCol)
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
            int         m_iCol = -1;
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

        for (int i = 0, of = 0, cArray = f_aCol.length; i < cArray; ++i)
            {
            Object[] aoCol = f_aCol[i].toArray();
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
     * An array of Collections to enumerate.
     */
    protected final Collection<E>[] f_aCol;
    }
