/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Implements a list which is based on another list, represents a sub-list of the underlying list.
 *
 * @author as  2010.12.04
 */
public class SubList<T>
        implements List<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a <tt>SubList</tt> from provided list.
     *
     * @param list    underlying list
     * @param nStart  the index of first list element to include in sub-list
     * @param nCount  the max size of created sub-list
     */
    public SubList(List<T> list, int nStart, int nCount)
        {
        if (nCount < 0)
            {
            m_subList = list.subList(nStart, list.size());
            }
        else
            {
            int nEnd = Math.min(nStart + nCount, list.size());
            m_subList = nStart < nEnd
                        ? list.subList(nStart, nEnd)
                        : new ArrayList<T>();
            }
        }

    // --- List implementation ----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public int size()
        {
        return m_subList.size();
        }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
        {
        return m_subList.isEmpty();
        }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o)
        {
        return m_subList.contains(o);
        }

    /**
     * {@inheritDoc}
     */
    public Iterator<T> iterator()
        {
        return m_subList.iterator();
        }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray()
        {
        return m_subList.toArray();
        }

    /**
     * {@inheritDoc}
     */
    public <T> T[] toArray(T[] a)
        {
        return m_subList.toArray(a);
        }

    /**
     * {@inheritDoc}
     */
    public boolean add(T o)
        {
        return m_subList.add(o);
        }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o)
        {
        return m_subList.remove(o);
        }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(Collection<?> c)
        {
        return m_subList.containsAll(c);
        }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(Collection<? extends T> c)
        {
        return m_subList.addAll(c);
        }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(int index, Collection<? extends T> c)
        {
        return m_subList.addAll(index, c);
        }

    /**
     * {@inheritDoc}
     */
    public boolean removeAll(Collection<?> c)
        {
        return m_subList.removeAll(c);
        }

    /**
     * {@inheritDoc}
     */
    public boolean retainAll(Collection<?> c)
        {
        return m_subList.retainAll(c);
        }

    /**
     * {@inheritDoc}
     */
    public void clear()
        {
        m_subList.clear();
        }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o)
        {
        return m_subList.equals(o);
        }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
        {
        return m_subList.hashCode();
        }

    /**
     * {@inheritDoc}
     */
    public T get(int index)
        {
        return m_subList.get(index);
        }

    /**
     * {@inheritDoc}
     */
    public T set(int index, T element)
        {
        return m_subList.set(index, element);
        }

    /**
     * {@inheritDoc}
     */
    public void add(int index, T element)
        {
        m_subList.add(index, element);
        }

    /**
     * {@inheritDoc}
     */
    public T remove(int index)
        {
        return m_subList.remove(index);
        }

    /**
     * {@inheritDoc}
     */
    public int indexOf(Object o)
        {
        return m_subList.indexOf(o);
        }

    /**
     * {@inheritDoc}
     */
    public int lastIndexOf(Object o)
        {
        return m_subList.lastIndexOf(o);
        }

    /**
     * {@inheritDoc}
     */
    public ListIterator<T> listIterator()
        {
        return m_subList.listIterator();
        }

    /**
     * {@inheritDoc}
     */
    public ListIterator<T> listIterator(int index)
        {
        return m_subList.listIterator(index);
        }

    /**
     * {@inheritDoc}
     */
    public List<T> subList(int fromIndex, int toIndex)
        {
        return m_subList.subList(fromIndex, toIndex);
        }

    // ---- data members ----------------------------------------------------

    /**
     * Underlying list.
     */
    private List<T> m_subList;
    }
