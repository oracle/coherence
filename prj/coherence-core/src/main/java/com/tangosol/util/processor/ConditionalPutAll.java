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
import com.tangosol.util.NullImplementation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;


/**
* ConditionalPutAll is an EntryProcessor that performs a
* {@link com.tangosol.util.InvocableMap.Entry#setValue(Object)
* Entry.setValue} operation for multiple entries that satisfy the specified
* condition.
* <p>
* Consider such methods of the
* <a href="http://download.oracle.com/javase/6/docs/api/java/util/concurrent/ConcurrentMap.html">ConcurrentMap</a>
* interface as <i>replace(key, value)</i> and <i>putIfAbsent(key, value)</i>.
* While the regular <i>put(key, value)</i> method has the "multi-put" flavor
* <i>putAll(map)</i>, there are no analogous "bulk" operations for those
* ConcurrentMap methods. The ConditionalPutAll processor provides the
* corresponding functionality when used in conjunction with the {@link
* InvocableMap#invokeAll(java.util.Collection, InvocableMap.EntryProcessor)
* invokeAll(keys, processor)} API. For example, the
* <i>replaceAll(map)</i> method could be implemented as:
* <pre>
*   filter = PresentFilter.INSTANCE;
*   cache.invokeAll(map.keySet(), new ConditionalPutAll(filter, map));
* </pre>
* and the <i>putAllIfAbsent(map)</i> could be done by inverting the filter:
* <pre>
*   filter = new NotFilter(PresentFilter.INSTANCE);
* </pre>
* <p>
* Obviously, using more specific, fine-tuned filters may provide additional
* flexibility and efficiency allowing the multi-put operations to be
* performed conditionally on values of specific attributes (or even
* calculations) instead of a simple existence check.
*
* @author gg 2006.04.28
* @since Coherence 3.2
*/
public class ConditionalPutAll<K, V>
        extends    AbstractProcessor<K, V, V>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ConditionalPutAll()
        {
        }

    /**
    * Construct a ConditionalPutAll processor that updates an entry with a
    * new value if and only if the filter applied to the entry evaluates to
    * true. The new value is extracted from the specified map based on the
    * entry's key.
    *
    * @param filter  the filter to evaluate all supplied entries
    * @param map     a map of values to update entries with
    */
    public ConditionalPutAll(Filter filter, Map<? extends K, ? extends V> map)
        {
        azzert(filter != null, "Filter is null");
        azzert(map    != null, "Map is null");

        m_filter = filter;
        m_map    = new LiteMap(map);
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public V process(InvocableMap.Entry<K, V> entry)
        {
        Map<? extends K, ? extends V> map = m_map;
        K key                             = entry.getKey();

        if (map.containsKey(key) &&
                InvocableMapHelper.evaluateEntry(m_filter, entry))
            {
            entry.setValue(map.get(key), false);
            }
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public Map<K, V> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        Map<? extends K, ? extends V> map = m_map;
        Filter filter                     = m_filter;

        for (Iterator iter = setEntries.iterator(); iter.hasNext();)
            {
            InvocableMap.Entry entry = (InvocableMap.Entry) iter.next();

            Object oKey = entry.getKey();
            if (map.containsKey(oKey) &&
                    InvocableMapHelper.evaluateEntry(filter, entry))
                {
                entry.setValue(map.get(oKey), false);
                }
            }

        return NullImplementation.getMap();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ConditionalPutAll with another object to determine
    * equality.
    *
    * @return true iff this ConditionalPutAll and the passed object are
    *         equivalent ConditionalPutAll
    */
    public boolean equals(Object o)
        {
        if (o instanceof ConditionalPutAll)
            {
            ConditionalPutAll that = (ConditionalPutAll) o;
            return equals(this.m_filter, that.m_filter)
                && equals(this.m_map,    that.m_map);
            }

        return false;
        }

    /**
    * Determine a hash value for the ConditionalPutAll object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ConditionalPutAll object
    */
    public int hashCode()
        {
        return m_filter.hashCode() + m_map.hashCode();
        }

    /**
    * Return a human-readable description for this ConditionalPutAll.
    *
    * @return a String description of the ConditionalPutAll
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            "{Filter = "+ m_filter + ", Map=" + m_map + '}';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter = (Filter) ExternalizableHelper.readObject(in);

        Map map = m_map = new LiteMap();
        ExternalizableHelper.readMap(in, map, null);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_filter);
        ExternalizableHelper.writeMap(out, m_map);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter  = (Filter) in.readObject(0);
        m_map     = (Map) in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        // note: not writeMap(), just in case the map is a POF object
        out.writeObject(1, m_map);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying filter.
    */
    @JsonbProperty("filter")
    protected Filter m_filter;

    /**
    * Specifies the map of new values.
    */
    @JsonbProperty("entries")
    protected Map<? extends K, ? extends V> m_map;
    }
