/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.SubSet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which negates the results of another filter.
*
* @author cp/gg 2002.10.26
*/
public class NotFilter<T>
        extends    AbstractQueryRecorderFilter<T>
        implements EntryFilter<Object, T>, IndexAwareFilter<Object, T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public NotFilter()
        {
        }

    /**
    * Construct a negation filter.
    *
    * @param filter  the filter whose results this Filter negates
    */
    public NotFilter(Filter<T> filter)
        {
        m_filter = filter;
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(T o)
        {
        return !m_filter.evaluate(o);
        }

    public String toExpression()
        {
        return "!(" + m_filter.toExpression() + ")";
        }

    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return !InvocableMapHelper.evaluateEntry(m_filter, entry);
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        Filter filter = m_filter;
        if (filter instanceof IndexAwareFilter)
            {
            IndexAwareFilter ixFilter = (IndexAwareFilter) filter;
            int nEffectiveness = ixFilter.calculateEffectiveness(getNonPartialIndexes(mapIndexes), setKeys);
            return nEffectiveness < 0 ? -1 : setKeys.size() - nEffectiveness;
            }
        
        return -1;
        }

    /**
    * {@inheritDoc}
    */
    @SuppressWarnings("unchecked")
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        Filter filter = m_filter;
        if (filter instanceof IndexAwareFilter)
            {
            // create delta set
            SubSet setDelta = new SubSet(setKeys);

            // delegate to the not-ed filter, but only use the non-partial
            // indexes, since for a partial index the fact that it contains keys
            // for entries that fit the underlying filter does not mean that it
            // contains them all. As a result, the "negating" operation may
            // produce invalid result
            Filter filterNew = ((IndexAwareFilter) filter).applyIndex(
                    getNonPartialIndexes(mapIndexes), setDelta);

            // see if any keys were filtered out
            Set setRemoved = setDelta.getRemoved();

            if (filterNew == null || setDelta.isEmpty())
                {
                // invert the key selection by the delegated-to filter
                if (setRemoved.isEmpty())
                    {
                    // no keys were removed; therefore the result of the
                    // "not" is to remove all keys (clear)
                    setKeys.clear();
                    }
                else if (setDelta.isEmpty())
                    {
                    // all keys were removed; therefore the result of the
                    // "not" is to retain all keys (remove none)
                    }
                else
                    {
                    // some keys were removed; therefore the result of the
                    // "not" is to retain only those removed keys
                    setKeys.retainAll(setRemoved);
                    }

                // nothing left to do; the index fully resolved the filter
                return null;
                }
            else if (setRemoved.isEmpty())
                {
                // no obvious effect from the index application
                return filterNew == filter ? this : new NotFilter(filterNew);
                }
            else
                {
                // some keys have been removed; those are definitely "in";
                // the remaining keys each need to be evaluated later
                KeyFilter filterKey = new KeyFilter(setRemoved);
                NotFilter filterNot = filterNew == filter ? this : new NotFilter(filterNew);
                return new OrFilter(filterKey, filterNot);
                }
            }
        else
            {
            return this;
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Get a Map of the available non-partial indexes from the given Map of all
    * available indexes.
    *
    * @param mapIndexes  the available {@link MapIndex} objects keyed by the
    *                    related ValueExtractor; read-only
    *
    * @return a Map of the available non-partial {@link MapIndex} objects
    */
    protected Map getNonPartialIndexes(Map mapIndexes)
        {
        Map mapNonPartialIndexes = new HashMap();
        for (Iterator iter = mapIndexes.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            if (!((MapIndex) entry.getValue()).isPartial())
                {
                mapNonPartialIndexes.put(entry.getKey(), entry.getValue());
                }
            }
        return mapNonPartialIndexes;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the Filter whose results are negated by this filter.
    *
    * @return the filter whose results are negated by this filter
    */
    public Filter<T> getFilter()
        {
        return m_filter;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the NotFilter with another object to determine equality.
    *
    * @return true iff this NotFilter and the passed object are equivalent
    *         NotFilter objects
    */
    public boolean equals(Object o)
        {
        if (o instanceof NotFilter)
            {
            NotFilter that = (NotFilter) o;
            return equals(this.m_filter, that.m_filter);
            }

        return false;
        }

    /**
    * Determine a hash value for the NotFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this NotFilter object
    */
    public int hashCode()
        {
        return hashCode(m_filter);
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return "NotFilter: !(" + m_filter + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter = (Filter<T>) readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_filter);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter = (Filter<T>) in.readObject(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Filter whose results are negated by this filter.
    */
    @JsonbProperty("filter")
    private Filter<T> m_filter;
    }
