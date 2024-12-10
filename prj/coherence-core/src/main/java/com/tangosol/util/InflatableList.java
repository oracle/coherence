/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
* A List specialization of InflatableCollection.
*
* @author ch 2009.11.22
* @since Coherence 3.6
*/
public class InflatableList
        extends InflatableCollection
        implements List
    {
    // ----- Object interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean equals(Object o)
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
            ? ((List) oValue).equals(o)
            :  oValue == NO_VALUE
                    ? new ImmutableArrayList(new Object[0]).getList().equals(o)
                    : Collections.singletonList(oValue).equals(o);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public int hashCode()
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
            ? ((List) oValue).hashCode()
                    : oValue == NO_VALUE
                        ? new ImmutableArrayList(new Object[0]).getList().hashCode()
                        : Collections.singletonList(oValue).hashCode();
        }


    // ----- List interface ------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void add(int i, Object o)
        {
        while (true)
            {
            Object oValue = m_oValue;

            if (oValue instanceof InflatedCollection)
                {
                ((List) oValue).add(i, o);
                return;
                }
            else if (oValue == NO_VALUE)
                {
                if (i != 0)
                    {
                    throw new IndexOutOfBoundsException();
                    }
                if (VALUE_UPDATER.compareAndSet(this, NO_VALUE, o))
                    {
                    return;
                    }
                }
            else
                {
                List list = (List) instantiateCollection();
                list.add(0, oValue);
                list.add(i, o);

                if (VALUE_UPDATER.compareAndSet(this, oValue, list))
                    {
                    return;
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public Object remove(int i)
        {
        while (true)
            {
            Object oValue = m_oValue;

            if (oValue instanceof InflatedCollection)
                {
                return ((List) oValue).remove(i);
                }

            // no value or trying to remove from an index that doesn't exist
            if (oValue == NO_VALUE || i != 0)
                {
                throw new IndexOutOfBoundsException();
                }

            // check so that correct value is removed, if not retry...
            // the only way that oValue may have changed is that it became
            // NO_VALUE or an InflatedCollection
            if (VALUE_UPDATER.compareAndSet(this, oValue, NO_VALUE))
                {
                return oValue;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean addAll(int i, Collection col)
        {
        // Note: strange semantics; see java.util.LinkedList
        Iterator iter = col.iterator();
        boolean  fRet = iter.hasNext();
        while (iter.hasNext())
            {
            add(i++, iter.next());
            }
        return fRet;
        }

    /**
    * {@inheritDoc}
    */
    public Object get(int i)
        {
        Object oValue = m_oValue;

        if (oValue instanceof InflatedCollection)
            {
            return ((List) oValue).get(i);
            }

        if (i != 0 || oValue == NO_VALUE)
            {
            throw new IndexOutOfBoundsException();
            }
        return oValue;
        }

    /**
    * {@inheritDoc}
    */
    public int indexOf(Object o)
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
            ? ((List) oValue).indexOf(o)
            : Base.equals(oValue, o)
                ? 0 : -1;
        }

    /**
    * {@inheritDoc}
    */
    public int lastIndexOf(Object o)
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
            ? ((List) oValue).lastIndexOf(o)
            : Base.equals(oValue, o)
                ? 0 : -1;
        }

    /**
    * {@inheritDoc}
    */
    public ListIterator listIterator()
        {
        return listIterator(0);
        }

    /**
    * {@inheritDoc}
    */
    public ListIterator listIterator(int i)
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
                ? ((List) oValue).listIterator(i)
                : oValue == NO_VALUE
                    ? new ImmutableArrayList(new Object[0]).getList().listIterator(i)
                    : Collections.singletonList(oValue).listIterator(i);
        }

    /**
    * {@inheritDoc}
    */
    public Object set(int i, Object o)
        {
        while (true)
            {
            Object oValue = m_oValue;

            if (oValue instanceof InflatedCollection)
                {
                return ((List) oValue).set(i, o);
                }
            if (oValue == NO_VALUE || i != 0)
                {
                throw new IndexOutOfBoundsException();
                }
            if (VALUE_UPDATER.compareAndSet(this, oValue, o))
                {
                return oValue;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public List subList(int iFrom, int iTo)
        {
        Object oValue = m_oValue;

        if (oValue instanceof InflatedCollection)
            {
            return ((List) oValue).subList(iFrom, iTo);
            }

        if (oValue == NO_VALUE)
            {
            throw new IndexOutOfBoundsException();
            }

        return Collections.singletonList(oValue).subList(iFrom, iTo);
        }


    // ----- factory methods -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected InflatedCollection instantiateCollection()
        {
        return new InflatedList();
        }


    // -----inner types ---------------------------------------------------

    /**
    * Inflated list implementation. Uses LinkedList and marks the implementation
    * with {@link InflatedCollection}
    */
    private static class InflatedList
            extends    LinkedList
            implements InflatedCollection
        {
        }
    }