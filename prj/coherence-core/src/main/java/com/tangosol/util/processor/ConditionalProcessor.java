/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.processor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapContext;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.LiteMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;


/**
* Conditional entry processor represents a processor that is invoked
* conditionally based on the result of an entry evaluation.
* <p>
* If the underlying filter expects to evaluate existent entries only (i.e.
* entries for which
* {@link com.tangosol.util.InvocableMap.Entry#isPresent() isPresent()} is
* true), it should be combined with a
* {@link com.tangosol.util.filter.PresentFilter} as follows:
* <pre>
* Filter filterPresent = new AndFilter(PresentFilter.INSTANCE, filter);
* </pre>
*
* @author gg/jh 2005.10.31
*
* @see com.tangosol.util.filter.PresentFilter
*/
public class ConditionalProcessor<K, V, T>
        extends    AbstractProcessor<K, V, T>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ConditionalProcessor()
        {
        }

    /**
    * Construct a ConditionalProcessor for the specified filter and the
    * processor.
    * <p>
    * The specified entry processor gets invoked if and only if the filter
    * applied to the InvocableMap entry evaluates to true; otherwise the
    * result of the {@link #process} invocation will return <tt>null</tt>.
    *
    * @param filter     the filter
    * @param processor  the entry processor
    */
    public ConditionalProcessor(Filter<V> filter, InvocableMap.EntryProcessor<K, V, T> processor)
        {
        azzert(filter != null && processor != null,
            "Both filter and processor must be specified");
        m_filter    = filter;
        m_processor = processor;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * Process a Map.Entry object if it satisfies the underlying filter.
    * <p>
    * Note: if this method throws an exception, all modifications to the supplied
    * entry or any other entries retrieved via the {@link BackingMapContext#getBackingMapEntry}
    * API will be rolled back leaving all underlying values unchanged.
    *
    * @param entry  the Entry to process
    *
    * @return the result of the processing, if any
    */
    public T process(InvocableMap.Entry<K, V> entry)
        {
        return (InvocableMapHelper.evaluateEntry(m_filter, entry)) ?
            m_processor.process(entry) : null;
        }

    /**
    * Process a Set of InvocableMap.Entry objects. This method is
    * semantically equivalent to:
    * <pre>
    *   Map mapResults = new ListMap();
    *   for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
    *       {
    *       Entry entry = (Entry) iter.next();
    *
    *      if (InvocableMapHelper.evaluateEntry(filter, entry))
    *          {
    *          mapResults.put(entry.getKey(), processor.process(entry));
    *          }
    *       }
    *   return mapResults;
    * </pre>
    * <p>
    * Note: if processAll() call throws an exception, only the entries that
    * were removed from the setEntries would be considered successfully
    * processed and the corresponding changes made to the underlying Map;
    * changes made to the remaining entries or any other entries obtained
    * from {@link BackingMapContext#getBackingMapEntry} will not be
    * processed.
    *
    * @param setEntries  a Set of InvocableMap.Entry objects to process
    *
    * @return a Map containing the results of the processing, up to one
    *         entry for each InvocableMap.Entry that was processed, keyed
    *         by the keys of the Map that were processed, with a corresponding
    *         value being the result of the processing for each key
    */
    public Map<K, T> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        Map                         mapResult = new LiteMap();
        Filter                      filter    = m_filter;
        InvocableMap.EntryProcessor processor = m_processor;

        for (Iterator iter = setEntries.iterator(); iter.hasNext();)
            {
            InvocableMap.Entry entry = (InvocableMap.Entry) iter.next();

            if (InvocableMapHelper.evaluateEntry(filter, entry))
                {
                mapResult.put(entry.getKey(), processor.process(entry));
                }
            }

        return mapResult;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ConditionalProcessor with another object to determine
    * equality.
    *
    * @return true iff this ConditionalProcessor and the passed object are
    *         equivalent ConditionalProcessors
    */
    public boolean equals(Object o)
        {
        if (o instanceof ConditionalProcessor)
            {
            ConditionalProcessor that = (ConditionalProcessor) o;
            return equals(this.m_filter,    that.m_filter)
                && equals(this.m_processor, that.m_processor);
            }

        return false;
        }

    /**
    * Determine a hash value for the ConditionalProcessor object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ConditionalProcessor object
    */
    public int hashCode()
        {
        return m_filter.hashCode() + m_processor.hashCode();
        }

    /**
    * Return a human-readable description for this ConditionalProcessor.
    *
    * @return a String description of the ConditionalProcessor
    */
    public String toString()
        {
        return "ConditionalProcessor(" + m_filter + ", " + m_processor + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter    = ExternalizableHelper.readObject(in);
        m_processor = ExternalizableHelper.readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_filter);
        ExternalizableHelper.writeObject(out, m_processor);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter    = in.readObject(0);
        m_processor = in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeObject(1, m_processor);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying filter.
    */
    @JsonbProperty("filter")
    protected Filter<V> m_filter;

    /**
    * The underlying entry processor.
    */
    @JsonbProperty("processor")
    protected InvocableMap.EntryProcessor<K, V, T> m_processor;
    }
