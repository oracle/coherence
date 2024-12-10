/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * UnmodifiableSetCollection is a wrapper set that provides a read-only view of
 * the underlying Sets. The underlying sets are assumed to be disjoint.
 * 
 * @author bb 2011.06.30
 */
public class UnmodifiableSetCollection<E>
        implements Set<E>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a UnmodifiableSetCollection
     *
     * @param sets  Array of sets to wrap
     */
    public UnmodifiableSetCollection(Set<E>... sets)
        {
        m_aSet = sets;
        }
    
    // ----- Collection interface -------------------------------------------
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
        {
        int i = 0;
        for (Set<E> set : m_aSet)
            {
            i += set.size();
            }
        return i;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
        {
        for (Set<E> set : m_aSet)
            {
            if (!set.isEmpty())
                {
                return false;
                }
            }
        return true;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o)
        {
        for (Set<E> set : m_aSet)
            {
            if (set.contains(o))
                {
                return true;
                }
            }
        return false;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator()
        {
        Iterator<E>[] aItr = new Iterator[m_aSet.length];
        int i = 0;
        for (Set<E> set : m_aSet)
            {
            aItr[i++] = set.iterator();
            }
        return new ChainedIterator<E>(aItr);
        }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray()
        {
        ArrayList<E> list = new ArrayList<E>(this.size());
        for (Set<E> set : m_aSet)
            {
            list.addAll(set);
            }
        return list.toArray();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(T[] a)
        {
        ArrayList<E> list = new ArrayList<E>(this.size());
        for (Set<E> set : m_aSet)
            {
            list.addAll(set);
            }
        return (T[]) list.toArray(a);
        }

    // ----- Set interface --------------------------------------------------
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(E e)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object o)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> c)
        {
        for (Object obj : c)
            {
            if (!contains(obj))
                {
                return false;
                }
            }
        return true;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(Collection<? extends E> c)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(Collection<?> c)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(Collection<?> c)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
        {
        throw new UnsupportedOperationException();
        }
    
    // ----- Object methods -------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }

        if (o instanceof Set)
            {
            Set set = (Set) o;
            return set.size() == this.size() && this.containsAll(set);
            }
        return false;
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        int nHash = 0;
        for (Set<E> set : m_aSet)
            {
            nHash += set.hashCode();
            }
        return nHash;
        }
    
    // ----- data members -------------------------------------------
    
    /**
     * Wrapped sets
     */
    protected Set<E>[] m_aSet;

    }
