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
* ConditionalPut is an EntryProcessor that performs an {@link
* com.tangosol.util.InvocableMap.Entry#setValue(Object) Entry.setValue}
* operation if the specified condition is satisfied.
* <p>
* While the ConditionalPut processing could be implemented via direct
* key-based QueryMap operations, it is more efficient and enforces
* concurrency control without explicit locking.
* <p>
* For example, the following operations are functionally equivalent to
* methods of the
* <a href="http://download.oracle.com/javase/6/docs/api/java/util/concurrent/ConcurrentMap.html">ConcurrentMap</a>
* interface (available with JDK 1.5).
* <table border>
*   <caption>Compare InvocableMap and ConcurrentMap</caption>
*   <tr>
*     <th>InvocableMap</th>
*     <th>ConcurrentMap</th>
*   </tr>
*   <tr>
*       <td>filter = PresentFilter.INSTANCE;
*cache.invoke(key, new ConditionalPut(filter, value);</td>
*       <td>cache.replace(key, value);</td>
*   </tr>
*   <tr>
*       <td>filter = new NotFilter(PresentFilter.INSTANCE);
*cache.invoke(key, new ConditionalPut(filter, value));</td>
*       <td>cache.putIfAbsent(key, value);</td>
*   </tr>
*   <tr>
*       <td>filter = new EqualsFilter(IdentityExtractor.INSTANCE, valueOld);
*cache.invoke(key, new ConditionalPut(filter, valueNew));</td>
*       <td>cache.replace(key, valueOld, valueNew);</td>
*   </tr>
* </table>
* <p>
* Obviously, using more specific, fine-tuned filters (rather than ones based
* on the IdentityExtractor) may provide additional flexibility and efficiency
* allowing the put operation to be performed conditionally on values of
* specific attributes (or even calculations) instead of the entire object.
*
* @author gg 2006.03.15
* @since Coherence 3.2
*/
public class ConditionalPut<K, V>
        extends    AbstractProcessor<K, V, V>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ConditionalPut()
        {
        }

    /**
    * Construct a ConditionalPut that updates an entry with a new value if
    * and only if the filter applied to the entry evaluates to true.
    * The result of the {@link #process} invocation does not return any
    * result.
    *
    * @param filter  the filter to evaluate an entry
    * @param value   a value to update an entry with
    */
    public ConditionalPut(Filter filter, V value)
        {
        this(filter, value, false);
        }

    /**
    * Construct a ConditionalPut that updates an entry with a new value if
    * and only if the filter applied to the entry evaluates to true. This
    * processor optionally returns the current value as a result of the
    * invocation if it has not been updated (the filter evaluated to false).
    *
    * @param filter   the filter to evaluate an entry
    * @param value    a value to update an entry with
    * @param fReturn  specifies whether or not the processor should return
    *                 the current value in case it has not been updated
    */
    public ConditionalPut(Filter filter, V value, boolean fReturn)
        {
        azzert(filter != null, "Filter is null");

        m_filter  = filter;
        m_value   = value;
        m_fReturn = fReturn;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public V process(InvocableMap.Entry<K, V> entry)
        {
        if (InvocableMapHelper.evaluateEntry(m_filter, entry))
            {
            entry.setValue(m_value, false);
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
        Object  oValue    = m_value;
        boolean fReturn   = m_fReturn;

        for (Iterator iter = setEntries.iterator(); iter.hasNext();)
            {
            InvocableMap.Entry entry = (InvocableMap.Entry) iter.next();

            if (InvocableMapHelper.evaluateEntry(filter, entry))
                {
                entry.setValue(oValue, false);
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
    * Compare the ConditionalPut with another object to determine equality.
    *
    * @return true iff this ConditionalPut and the passed object are
    *         equivalent ConditionalPut processors
    */
    public boolean equals(Object o)
        {
        if (o instanceof ConditionalPut)
            {
            ConditionalPut that = (ConditionalPut) o;
            return equals(this.m_filter, that.m_filter)
                && equals(this.m_value,  that.m_value)
                &&        this.m_fReturn == that.m_fReturn;
            }

        return false;
        }

    /**
    * Determine a hash value for the ConditionalPut object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ConditionalPut object
    */
    public int hashCode()
        {
        Object oValue = m_value;
        int    nHash  = oValue == null ? 0 : oValue.hashCode();
        return nHash + m_filter.hashCode() + (m_fReturn ? -1 : 1);
        }

    /**
    * Return a human-readable description for this ConditionalPut.
    *
    * @return a String description of the ConditionalPut
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            "{Filter = "+ m_filter + ", Value=" + m_value +
            ", ReturnRequired= " + m_fReturn + '}';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter  = (Filter) ExternalizableHelper.readObject(in);
        m_value   = (V) ExternalizableHelper.readObject(in);
        m_fReturn = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_filter);
        ExternalizableHelper.writeObject(out, m_value);
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
        m_value   = (V) in.readObject(1);
        m_fReturn = in.readBoolean(2);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeObject(1, m_value);
        out.writeBoolean(2, m_fReturn);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying filter.
    */
    @JsonbProperty("filter")
    protected Filter m_filter;

    /**
    * Specifies the new value to update an entry with.
    */
    @JsonbProperty("value")
    protected V m_value;

    /**
    * Specifies whether or not a return value is required.
    */
    @JsonbProperty("return")
    protected boolean m_fReturn;
    }
