/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An unmodifiable Collection that provides access to many collections in the
 * given order.
 *
 * @author hr  2014.02.19
 * @since Coherence 12.1.2
 */
public class ChainedCollection<E>
        implements Collection<E>
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
        this(col.toArray(new Collection[col.size()]));
        }

    /**
     * Construct a ChainedCollection with the provided array of Collection
     * objects.
     *
     * @param acol  an array of Collection objects
     */
    public ChainedCollection(Collection<E>...acol)
        {
        f_acol = acol;
        }

    // ----- Collection interface -------------------------------------------

    @Override
    public int size()
        {
        int cSize = 0;
        for (Collection<E> col : f_acol)
            {
            cSize += col.size();
            }
        return cSize;
        }

    @Override
    public boolean isEmpty()
        {
        for (Collection<E> col : f_acol)
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
        for (Collection<E> col : f_acol)
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
        for (Object o : f_acol)
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
                int         cCol = f_acol.length;

                while ((iter == null || !iter.hasNext()) && ++iCol < cCol)
                    {
                    iter   = m_iter = f_acol[iCol].iterator();
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
    public <T> T[] toArray(T[] a)
        {
        int cSize = size();
        if (a == null || cSize > a.length)
            {
            a = (T[]) new Object[cSize];
            }

        for (int i = 0, of = 0, cArray = f_acol.length; i < cArray; ++i)
            {
            Object[] aoCol = f_acol[i].toArray();
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
    protected final Collection<E>[] f_acol;
    }
