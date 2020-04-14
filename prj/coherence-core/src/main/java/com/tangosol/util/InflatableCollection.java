/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


/**
* A Collection implementation which optimizes memory consumption for
* collections that often contain just a single value. This implementation also
* reduces contention for read operations (e.g. contains, iterator, etc.)
*
* @author ch 2009.11.22
* @since Coherence 3.6
*/
public abstract class InflatableCollection
        extends AbstractCollection
    {
    // ----- Object interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean equals(Object o)
        {
        Object oValue = m_oValue;

        return oValue == NO_VALUE
                ? NullImplementation.getSet().equals(o)
                : oValue instanceof InflatedCollection
                            ? ((Collection) oValue).equals(o)
                            : Collections.singleton(oValue).equals(o);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public int hashCode()
        {
        Object oValue = m_oValue;

        return oValue == NO_VALUE
                ? NullImplementation.getSet().hashCode()
                : oValue instanceof InflatedCollection
                            ? ((Collection) oValue).hashCode()
                            : Collections.singleton(oValue).hashCode();
        }


    // ----- Collection interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean add(Object o)
        {
        while (true)
            {
            Object oValue = m_oValue;

            if (oValue instanceof InflatedCollection)
                {
                // already inflated
                return ((Collection) oValue).add(o);
                }
            else if (VALUE_UPDATER.compareAndSet(this, NO_VALUE, o))
                {
                // collection was empty
                return true;
                }
            else
                {
                if (Base.equals(o, oValue))
                    {
                    return false;
                    }

                // singleton collection; inflate
                Collection col = instantiateCollection();

                col.add(oValue);
                col.add(o);

                if (VALUE_UPDATER.compareAndSet(this, oValue, col))
                    {
                    return true;
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean remove(Object o)
        {
        while (true)
            {
            Object oValue = m_oValue;

            if (oValue instanceof InflatedCollection)
                {
                return ((Collection) oValue).remove(o);
                }

            if (Base.equals(o, oValue))
                {
                if (VALUE_UPDATER.compareAndSet(this, oValue, NO_VALUE))
                    {
                    return true;
                    }
                }
            else
                {
                return false;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean removeAll(Collection c)
        {
        while (true)
            {
            Object oValue = m_oValue;

            if ((oValue instanceof InflatedCollection))
                {
                return ((Collection) oValue).removeAll(c);
                }

            if (c.contains(oValue))
                {
                if (VALUE_UPDATER.compareAndSet(this, oValue, NO_VALUE))
                    {
                    return true;
                    }
                }
            else
                {
                return false;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean retainAll(Collection c)
        {
        while (true)
            {
            Object oValue = m_oValue;

            if ((oValue instanceof InflatedCollection))
                {
                return ((Collection) oValue).retainAll(c);
                }

            if (!c.contains(oValue))
                {
                if (VALUE_UPDATER.compareAndSet(this, oValue, NO_VALUE))
                    {
                    return true;
                    }
                }
            else
                {
                return false;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public void clear()
        {
        while (true)
            {
            Object oValue = m_oValue;

            if (oValue instanceof InflatedCollection)
                {
                ((Collection) oValue).clear();
                return;
                }
            else if (VALUE_UPDATER.compareAndSet(this, oValue, NO_VALUE))
                {
                return;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean contains(Object o)
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
                ? ((Collection) oValue).contains(o)
                : Base.equals(oValue, o);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Iterator iterator()
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
            ? ((Collection) oValue).iterator()
            : oValue == NO_VALUE
                    ? NullImplementation.getIterator()
                    : Collections.singleton(oValue).iterator();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public int size()
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
                ? ((Collection) oValue).size()
                : oValue == NO_VALUE ? 0 : 1;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Object[] toArray()
        {
        return toArray(new Object[0]);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public Object[] toArray(Object[] ao)
        {
        Object oValue = m_oValue;

        return oValue instanceof InflatedCollection
                ? ((Collection) oValue).toArray(ao)
                : oValue == NO_VALUE ? NullImplementation.getSet().toArray(ao)
                                     : Collections.singleton(oValue).toArray(ao);
        }


    // ----- helpers -----------------------------------------------------

    /**
    * Factory method used to create a new Collection.
    * The returned Collection must provide a "safe" iterator.
    *
    * @return a "real" implementation to use if this collection is expanded
    */
    protected abstract InflatedCollection instantiateCollection();


    // ----- inner types -------------------------------------------------

    /**
    * A marker interface which is used to identify internally inflated
    * Collections.
    */
    protected interface InflatedCollection
            extends Collection
        {
        }


    // ----- data members -----------------------------------------------

    /**
    * A marker value indicating that the single value has not been initialized.
    */
    protected static final Object NO_VALUE = new Object();

    /**
    * Atomic updater for collection values.
    * Static atomic field updater rather than {@link java.util.concurrent.atomic.AtomicReference}
    * is shared to reduce memory footprint of each InflatableCollection instance. (COH-9262)
    */
    protected static final AtomicReferenceFieldUpdater<InflatableCollection, Object>
                VALUE_UPDATER = AtomicReferenceFieldUpdater.newUpdater(
                    InflatableCollection.class, Object.class, "m_oValue");

    /**
    * Holds NO_VALUE, a single value, or an InflatedCollection of values.
    */
    protected volatile Object m_oValue = NO_VALUE;
    }