/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Comparator;
import java.util.TreeSet;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanServer;

/**
* A "map" class wrapper for the MBeanServer.
*
* @author ew 2008.02.13
* @since  Coherence 3.4
*/
public class MBeanQuery
    {
    // ----- Constructors ----------------------------------------------------
    /**
    * Construct a MBeanQuery with the specified pattern.
    *
    * @param sPattern the JMX Query pattern
    */
    public MBeanQuery(String sPattern)
        {
        this(sPattern, MBeanHelper.findMBeanServer());
        }

    /**
    * Construct a MBeanQuery with the specified pattern and {@link MBeanServer}.
    *
    * @param sPattern  the JMX Query pattern
    * @param server    the {@link MBeanServer} to query against
    */
    public MBeanQuery(String sPattern, MBeanServer server)
        {
        setPattern(sPattern);
        f_mbs = server;
        }

    /**
    * Refresh the cached keyset with the filter.
    *
    * @param filter  a Filter object to limit the results
    */
    protected void refreshKeys(Filter filter)
        {
        // COH-24823 - There used to be an if check here to determine if it was necessary to recalculate the set of
        //             MBeans. It was determined that, for all cases, a recalculation is necessary to pick up that
        //             MBeans had been removed or added (e.g. Coherence members entering and leaving the cluster).
        try
            {
            Set setResults = new TreeSet((o, o1) ->
                {
                ObjectName on  = (ObjectName) ((Entry) o).getKey();
                ObjectName on1 = (ObjectName) ((Entry) o1).getKey();
                String     s   = on.getKeyPropertyListString();
                String     s1  = on1.getKeyPropertyListString();

                return s.compareTo(s1);
                });

            Set setMBeans = f_mbs.queryNames(new ObjectName(m_sPattern), new QueryExpFilter(filter));
            for (Iterator iter = setMBeans.iterator(); iter.hasNext(); )
                {
                setResults.add(new Entry(iter.next()));
                }
            m_setResults = setResults;
            m_filter     = filter;
            }
        catch (MalformedObjectNameException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * The JMX Query pattern.
    *
    * @param sPattern  The query pattern.
    */
    public void setPattern(String sPattern)
        {
        m_sPattern = sPattern;
        }

    /**
    * Returns the number of key-value mappings in this query.  If the
    * query contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
    * <tt>Integer.MAX_VALUE</tt>.
    *
    * @return the number of key-value mappings in this query.
    */
    public int size()
        {
        return  m_setResults == null ? 0 : m_setResults.size();
        }

    /**
    * Removes all mappings from this map (optional operation).
    */
    public void clear()
        {
        m_setResults.clear();
        }

    /**
    * Determine if the query results are empty.
    *
    * @return true if results are empty.
    */
    public boolean isEmpty()
        {
        return m_setResults.isEmpty();
        }

    /**
    * Returns a set view of the keys contained in this map.  The set is
    * backed by the map, so changes to the map are reflected in the set, and
    * vice-versa.  If the map is modified while an iteration over the set is
    * in progress, the results of the iteration are undefined.  The set
    * supports element removal, which removes the corresponding mapping from
    * the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
    * <tt>removeAll</tt> <tt>retainAll</tt>, and <tt>clear</tt> operations.
    * It does not support the add or <tt>addAll</tt> operations.
    *
    * @return a set view of the keys contained in this query
    */
    public Set entrySet()
        {
        return keySet();
        }

    /**
    * Returns a set view of the keys contained in this map.  The set is
    * backed by the map, so changes to the map are reflected in the set, and
    * vice-versa.  If the map is modified while an iteration over the set is
    * in progress, the results of the iteration are undefined.  The set
    * supports element removal, which removes the corresponding mapping from
    * the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
    * <tt>removeAll</tt> <tt>retainAll</tt>, and <tt>clear</tt> operations.
    * It does not support the add or <tt>addAll</tt> operations.
    *
    * @return a set view of the keys contained in this query
    */
    public Set keySet()
        {
        refreshKeys(null);
        return m_setResults;
        }

    /**
    * Return a set view of the keys contained in this map for entries that
    * satisfy the criteria expressed by the filter.
    * <p>
    * Unlike the {@link #keySet()} method, the set returned by this method
    * may not be backed by the map, so changes to the set may not reflected
    * in the map, and vice-versa.
    * <p>
    * <b>Note: The Partitioned Cache implements the QueryMap interface using the
    * Parallel Query feature.</b>
    *
    * @param filter  the Filter object representing the criteria that
    *                the entries of this map should satisfy
    *
    * @return a set of keys for entries that satisfy the specified criteria
    */
    public Set keySet(Filter filter)
        {
        refreshKeys(filter);
        return m_setResults;
        }

    /**
    * Return a set view of the entries contained in this map that satisfy
    * the criteria expressed by the filter.  Each element in the returned set
    * is a {@link java.util.Map.Entry}.
    * <p>
    * Unlike the {@link #entrySet()} method, the set returned by this method
    * may not be backed by the map, so changes to the set may not be reflected
    * in the map, and vice-versa.
    * <p>
    * <b>Note: The Partitioned Cache implements the QueryMap interface using the
    * Parallel Query feature.</b>
    *
    * @param filter  the Filter object representing the criteria that
    *                the entries of this map should satisfy
    *
    * @return a set of entries that satisfy the specified criteria
    */
    public Set entrySet(Filter filter)
        {
        return keySet(filter);
        }

    /**
    * Invoke the passed EntryProcessor against the entries specified by the
    * passed keys, returning the result of the invocation for each.
    *
    * @param collKeys the keys to process; these keys are not required to
    *                 exist within the Query
    * @param agent    the EntryProcessor to use to process the specified keys
    *
    * @return a Map containing the results of invoking the EntryProcessor
    *         against each of the specified keys
    */
    public Map invokeAll(Collection collKeys, InvocableMap.EntryProcessor agent)
        {
        Map mRet = new HashMap();
        for (Iterator i = collKeys.iterator(); i.hasNext(); )
            {
            Entry oKey = (Entry)i.next();
              mRet.put(oKey.getKey(), agent.process(oKey));
            }
        return mRet;
        }

    /**
    * Perform an aggregating operation against the entries specified by the
    * passed keys.
    *
    * @param collKeys  the Collection of keys that specify the entries within
    *                  this Map to aggregate across
    * @param agent     the EntryAggregator that is used to aggregate across
    *                  the specified entries of this Query
    *
    * @return the result of the aggregation
    */
    public Object aggregate(Collection collKeys,
            InvocableMap.EntryAggregator agent)
        {
        return agent.aggregate((Set) collKeys);
        }

    /**
    * Perform an aggregating operation against the set of entries that are
    * selected by the given Filter.
    *
    * @param filter  the Filter that is used to select entries within this
    *                Map to aggregate across
    * @param agent   the EntryAggregator that is used to aggregate across
    *                the selected entries of this Query
    *
    * @return the result of the aggregation
    */
    public Object aggregate(Filter filter, InvocableMap.EntryAggregator agent)
        {
        return agent.aggregate(keySet(filter));
        }

    // ----- InvocableMap.Entry interface -----------------------------------

    /**
    * An InvocableMap.Entry contains additional information and exposes
    * additional operations that the basic Map.Entry does not. It allows
    * non-existent entries to be represented, thus allowing their optional
    * creation. It allows existent entries to be removed from the Map. It
    * supports a number of optimizations that can ultimately be mapped
    * through to indexes and other data structures of the underlying Map.
    */
    public static class Entry
        implements InvocableMap.Entry, SortedMap.Entry
        {
        Entry(Object Key)
            {
            m_Key = Key;
            }

        // ----- Map.Entry interface ------------------------------------

        /**
        * Return the key corresponding to this entry. The resultant key does
        * not necessarily exist within the containing Map, which is to say
        * that <tt>InvocableMap.this.containsKey(getKey)</tt> could return
        * false. To test for the presence of this key within the Query, use
        * {@link #isPresent}, and to create the entry for the key, use
        * {@link #setValue}.
         *
        * @return the key corresponding to this entry; may be null if the
        *         underlying Map supports null keys
        */
        public Object getKey()
            {
            return m_Key;
            }

        /**
        * Return the value corresponding to this entry. If the entry does
        * not exist, then the value will be null. To differentiate between
        * a null value and a non-existent entry, use {@link #isPresent}.
        * <p>
        * <b>Note:</b> any modifications to the value retrieved using this
        * method are not guaranteed to persist unless followed by a
        * {@link #setValue} or {@link #update} call.
        *
        * @return the value corresponding to this entry; may be null if the
        *         value is null or if the Entry does not exist in the Map
        */
        public Object getValue()
            {
            return m_Key;
            }

        /**
        * Unsupported Operation
        *
        * @param oValue ignored
        * @return null
        */
        public Object setValue(Object oValue)
            {
            throw(new UnsupportedOperationException());
            }

        /**
        * Unsupported Operation
        *
        * @param oValue ignored
        * @return null
        */
        public void setValue(Object oValue, boolean fSynthetic)
            {
            throw(new UnsupportedOperationException());
            }

        /**
        * Unsupported Operation
        *
        * @param oValue ignored
        * @return null
        */
        public void update(ValueUpdater updater, Object oValue)
            {
            throw(new UnsupportedOperationException());
            }

        /**
        * Determine if this Entry exists in the Query. If the Entry is not
        * present, it can not be added
        *
        * @return true iff this Entry is existent in the containing Query
        */
        public boolean isPresent()
            {
            return true;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isSynthetic()
            {
            return false;
            }

        /**
        * Unsupported Operation
        *
        * @param fSynthetic ignored
        * @return null
        */
        public void remove(boolean fSynthetic)
            {
            throw(new UnsupportedOperationException());
            }


        /**
        * Extract a value out of the Entry's value. Calling this method is
        * semantically equivalent to
        * <tt>extractor.extract(entry.getValue())</tt>, but this method may
        * be significantly less expensive. For example, the resultant value may
        * be obtained from a forward index, avoiding a potential object
        * de-serialization.
        *
        * @param extractor  a ValueExtractor to apply to the Entry's value
        *
        * @return the extracted value
        */
        public Object extract(ValueExtractor extractor)
            {
            return extractor.extract(m_Key);
            }

        /**
        * The key and value for this entry.
        */
        protected Object m_Key;
        }

    // ----- data members ----------------------------------------------------


    /**
    * The query pattern wrapped by the "map".
    */
    protected String m_sPattern;

    /**
    * The results from the MBean pattern and the filter.
    */
    protected Set m_setResults;

    /**
    * The filter for the MBean query.
    */
    protected Filter m_filter;

    /**
    * The {@link MBeanServer} this MBeanQuery operates against.
    */
    protected MBeanServer f_mbs;
    }
