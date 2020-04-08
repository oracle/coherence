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

import com.tangosol.util.ClassHelper;
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
* ConditionalRemove is an EntryProcessor that performs an
* {@link com.tangosol.util.InvocableMap.Entry#remove(boolean) Entry.remove}
* operation if the specified condition is satisfied.
* <p>
* While the ConditionalRemove processing could be implemented via direct
* key-based QueryMap operations, it is more efficient and enforces
* concurrency control without explicit locking.
* <p>
* For example, the following operations are functionally similar, but the
* InvocableMap versions (a) perform significantly better for partitioned
* caches; (b) provide all necessary concurrency control (which is ommited
* from the QueryMap examples):
* <table border>
*   <caption>Compare InvocableMap and QueryMap</caption>
*   <tr>
*     <th>InvocableMap</th>
*     <th>QueryMap</th>
*   </tr>
*   <tr>
*       <td>cache.invoke(key, new ConditionalRemove(filter));<sup>&nbsp;(*)</sup></td>
*       <td>if (filter.evaluate(cache.get(key))
*  cache.remove(key);</td>
*   </tr>
*   <tr>
*       <td>cache.invokeAll(setKeys, new ConditionalRemove(filter));</td>
*       <td>for (Object key : setKeys)
*  if (filter.evaluate(cache.get(key))
*    cache.remove(key);</td>
*   </tr>
*   <tr>
*       <td>cache.invokeAll(filter1, new ConditionalRemove(filter2);</td>
*       <td>for (Object key : cache.setKeys(filter1))
*  if (filter2.evaluate(cache.get(key))
*    cache.remove(key);</td>
*   </tr>
*   <tr>
*       <td>cache.invokeAll(filter, new ConditionalRemove(AlwaysFilter.INSTANCE));</td>
*       <td>Set setKeys = cache.keySet(filter);
*cache.keySet().removeAll(setKeys);</td>
*   </tr>
* </table>
*
* <sup>(*)</sup> <font style="font-size:small">If the filter is assigned as the following:
* <tt>filter = new EqualsFilter(IdentityExtractor.INSTANCE, oValue);</tt>
* <br>this operation also becomes functionally equivalent to the
* <a href="http://download.oracle.com/javase/6/docs/api/java/util/concurrent/ConcurrentMap.html#remove(java.lang.Object,%20java.lang.Object)">remove</a>
* method of the ConcurrentMap interface (available with JDK 1.5).
* </font>
*
* @author gg 2006.03.15
* @since Coherence 3.2
*/
public class ConditionalRemove<K, V>
        extends    AbstractProcessor<K, V, V>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ConditionalRemove()
        {
        }

    /**
    * Construct a ConditionalRemove processor that removes an InvocableMap
    * entry if and only if the filter applied to the entry evaluates to true.
    * The result of the {@link #process} invocation does not return any
    * result.
    *
    * @param filter  the filter to evaluate an entry
    */
    public ConditionalRemove(Filter filter)
        {
        this(filter, false);
        }

    /**
    * Construct a ConditionalRemove processor that removes an InvocableMap
    * entry if and only if the filter applied to the entry evaluates to true.
    * This processor may optionally return the current value as a result of
    * the invocation if it has not been removed (the filter evaluated to
    * false).
    *
    * @param filter   the filter to evaluate an entry
    * @param fReturn  specifies whether or not the processor should return
    *                  the current value if it has not been removed
    */
    public ConditionalRemove(Filter filter, boolean fReturn)
        {
        azzert(filter != null, "Filter is null");
        m_filter  = filter;
        m_fReturn = fReturn;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public V process(InvocableMap.Entry<K, V> entry)
        {
        if (entry.isPresent() &&
                InvocableMapHelper.evaluateEntry(m_filter, entry))
            {
            entry.remove(false);
            return null;
            }
        return m_fReturn ? entry.getValue() : null;
        }

    /**
    * {@inheritDoc}
    */
    public Map<K, V> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        Map     mapResult = new LiteMap();
        Filter  filter    = m_filter;
        boolean fReturn   = m_fReturn;

        for (Iterator iter = setEntries.iterator(); iter.hasNext();)
            {
            InvocableMap.Entry entry = (InvocableMap.Entry) iter.next();

            if (entry.isPresent() &&
                    InvocableMapHelper.evaluateEntry(filter, entry))
                {
                entry.remove(false);
                }
            else if (fReturn)
                {
                mapResult.put(entry.getKey(), entry.getValue());
                }
            }

        return mapResult;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ConditionalRemove with another object to determine
    * equality.
    *
    * @return true iff this ConditionalRemove and the passed object are
    *         equivalent ConditionalRemove
    */
    public boolean equals(Object o)
        {
        if (o instanceof ConditionalRemove)
            {
            ConditionalRemove that = (ConditionalRemove) o;
            return equals(this.m_filter, that.m_filter)
                       && this.m_fReturn == that.m_fReturn;
            }

        return false;
        }

    /**
    * Determine a hash value for the ConditionalRemove object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ConditionalRemove object
    */
    public int hashCode()
        {
        return m_filter.hashCode() + (m_fReturn ? -1 : 1);
        }

    /**
    * Return a human-readable description for this ConditionalRemove.
    *
    * @return a String description of the ConditionalRemove processor
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            "{Filter= " + m_filter + ", ReturnRequired= " + m_fReturn + '}';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter  = (Filter) ExternalizableHelper.readObject(in);
        m_fReturn = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_filter);
        out.writeBoolean(m_fReturn);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter  = (Filter) in.readObject(0);
        m_fReturn = in.readBoolean(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeBoolean(1, m_fReturn);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying filter.
    */
    @JsonbProperty("filter")
    protected Filter m_filter;

    /**
    * Specifies whether or not a return value is required.
    */
    @JsonbProperty("return")
    protected boolean m_fReturn;
    }
