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

import com.tangosol.net.AbstractPriorityTask;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;

import java.util.Map;
import java.util.Set;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* PriorityFilter is used to explicitly control the scheduling priority and
* timeouts for execution of filter-based methods.
* <p>
* For example, let's assume that there is a cache that belongs to a partitioned
* cache service configured with a <i>request-timeout</i> and <i>task-timeout</i>
* of 5 seconds.
* Also assume that we are willing to wait longer for a particular rarely
* executed parallel query that does not employ any indexes. Then we could
* override the default timeout values by using the PriorityFilter as follows:
* <pre>
*   LikeFilter     filterStandard = new LikeFilter("getComments", "%fail%");
*   PriorityFilter filterPriority = new PriorityFilter(filterStandard);
*   filterPriority.setExecutionTimeoutMillis(PriorityTask.TIMEOUT_NONE);
*   filterPriority.setRequestTimeoutMillis(PriorityTask.TIMEOUT_NONE);
*   Set setEntries = cache.entrySet(filterPriority);
* </pre>
* <p>
* This is an advanced feature which should be used judiciously.
*
* @author gg 2007.03.20
* @since Coherence 3.3
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class PriorityFilter<T>
        extends    AbstractPriorityTask
        implements Filter<T>, EntryFilter<Object, T>, IndexAwareFilter<Object, T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public PriorityFilter()
        {
        }

    /**
    * Construct a PriorityFilter.
    *
    * @param filter  the filter wrapped by this PriorityFilter
    */
    public PriorityFilter(IndexAwareFilter filter)
        {
        m_filter = filter;
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(T o)
        {
        return ((Filter<T>) m_filter).evaluate(o);
        }


    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return m_filter.evaluateEntry(entry);
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        return m_filter.calculateEffectiveness(mapIndexes, setKeys);
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        return m_filter.applyIndex(mapIndexes, setKeys);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying filter.
    *
    * @return the filter wrapped by this PriorityFilter
    */
    public IndexAwareFilter getFilter()
        {
        return m_filter;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '(' + m_filter + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_filter = (IndexAwareFilter) readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        writeObject(out, m_filter);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * This implementation reserves property index 10.
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_filter = (IndexAwareFilter) in.readObject(10);
        }

    /**
    * {@inheritDoc}
    * <p>
    * This implementation reserves property index 10.
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeObject(10, m_filter);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The wrapped filter.
    */
    @JsonbProperty("filter")
    private IndexAwareFilter m_filter;
    }
