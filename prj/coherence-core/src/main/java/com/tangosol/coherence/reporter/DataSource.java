/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.SafeHashMap;

import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.GroupAggregator;

import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.processor.ExtractorProcessor;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;


/**
* Class to abstract the contract of the Locators to the MBean.
*
* @author ew 2008.03.19
* @since Coherence 3.4
*/
public class DataSource
    {
    /**
    * Construct a new DataSource
    */
    public DataSource()
        {
        m_listVE     = new LinkedList();
        m_listAgg    = new LinkedList();
        m_listGroup  = new LinkedList();
        m_listScalar = new LinkedList();
        }

    /**
    * Add a extractor to the DataSource.
    *
    * @param extractor the ValueExtractor to execute
    *
    * @return  the position in the result set of the extractor.  This value
    *          is used to access the result via getValue
    */
    public int addExtractor(ValueExtractor extractor)
        {
        int cSize = m_listVE.size();
        m_listVE.add(new Collection(extractor));
        return cSize;
        }

    /**
    * Add a scalar transformation to the DataSource.
    *
    * @param extractor the ValueExtractor to execute
    *
    * @return  the position in the result set of the extractor.  This value
    *          is used to access the result via {@link #getValue}
    */
    public int addScalar(ValueExtractor extractor)
        {
        List listScalar = m_listScalar;
        int  cSize      = listScalar.size();

        listScalar.add(extractor);

        return cSize;
        }

    /**
    * Add an aggregator to the DataSource.
    *
    * @param aggregator  the aggregator to execute
    *
    * @return  the position in the result set of the aggregator.   This value
    *          is used to access the result via {@link #getAggValue}
    */
    public int addAggregator(InvocableMap.EntryAggregator aggregator)
        {
        List list  = m_fGroupBy ? m_listVE : m_listAgg;
        int  cSize = list.size();

        list.add(aggregator);

        return cSize;
        }

    /**
    * Add an extractor to the group by.
    *
    * @param extractor  the extractor to group by
    *
    * @return  the position in the result set of the aggregator.  This value
    *          is used to access the result via {@link #getAggValue}
    */
    public int addGroupBy(ValueExtractor extractor)
        {
        List listGroup = m_listGroup;
        int  cGroups   = listGroup.size();

        listGroup.add(extractor);

        return cGroups;
        }

    /**
    * Execute the aggregators and the extractors
    *
    * @param mBeanQuery  the mBeanQuery to execute against
    * @param setMBeans   the set of keys to execute against, a subset of keys in the map
    */
    public void execute(MBeanQuery mBeanQuery, Set setMBeans)
        {
        if (m_listGroup.size() == 0)
            {
            m_listGroup.add(new IdentityExtractor());
            }

        m_ialAgg               = getAggregates(mBeanQuery, setMBeans, m_listAgg);
        m_mapGroup             = getDetail(mBeanQuery, setMBeans, m_listGroup);
        m_mapExtractionResults = getAggregates(mBeanQuery, setMBeans, m_listVE, m_listGroup);
        m_mapScalar            = getScalar(m_mapExtractionResults, m_listScalar);
        }

    /**
    * Apply Scalar functions to the aggregate result set
    *
    * @param mapResults  the Result aggregate Map
    * @param listVE      the list of Scalar ValueExtractors
    *
    * @return a Map containing the results
    */
    protected Map getScalar(Map mapResults, List listVE)
        {
        int cVE      = listVE.size();
        int cResults = mapResults.size();

        if (cVE > 0 && cResults > 0)
            {
            ValueExtractor[] aVE = (ValueExtractor[])
                listVE.toArray(new ValueExtractor[cVE]);

            MultiExtractor extractor  = new MultiExtractor(aVE);
            Map            mapScalars = new SafeHashMap();

            for (Iterator iter = mapResults.keySet().iterator(); iter.hasNext(); )
                {
                Object oKey = iter.next();
                mapScalars.put(oKey, extractor.extract(oKey));
                }

            return mapScalars;
            }

        return null;
        }

    /**
    * Execute the extractors designated "group-by" on the given mBeans.
    *
    * @param mBeanQuery  the MBeanQuery to execute against
    * @param setMBeans   the keys to be extracted
    * @param listGBE     the list of ValueExtractors designated "group-by" to be executed
    *
    * @return a Map of mBean to result of extraction on that mBean
    */
    protected Map getDetail(MBeanQuery mBeanQuery, Set setMBeans, List listGBE)
        {
        int cSize = listGBE.size();
        if (cSize >  0)
            {
            ValueExtractor[] aVE = (ValueExtractor[])
                listGBE.toArray(new ValueExtractor[cSize]);

            return mBeanQuery.invokeAll(setMBeans, new ExtractorProcessor(new MultiExtractor(aVE)));
            }

        return null;
        }

    /**
    * Execute the aggregate extractors against the set of beans.
    *
    * @param mBeanQuery  the MBeanQuery to execute against
    * @param setMBeans   the keys to be aggregate
    * @param listAgg     the list of aggregators
    *
    * @return the result of the aggregation
    */
    protected ImmutableArrayList getAggregates(MBeanQuery mBeanQuery, Set setMBeans, List listAgg)
        {
        int cSize = listAgg.size();
        if (cSize > 0)
            {
            InvocableMap.EntryAggregator[] aVE = (InvocableMap.EntryAggregator[])
                    listAgg.toArray(new InvocableMap.EntryAggregator[cSize]);

            CompositeAggregator aggComp = CompositeAggregator.createInstance(aVE);

            return (ImmutableArrayList) mBeanQuery.aggregate(setMBeans, aggComp);
            }

        return null;
        }

    /**
    * Execute the aggregates.
    *
    * @param mBeanQuery  the MBeanQuery to execute against
    * @param setMBeans   the keys to be aggregate
    * @param listAgg     the list of aggregators
    * @param listBy      the list of value extractors
    *
    * @return the result of the aggregation
    */
    protected Map getAggregates(MBeanQuery mBeanQuery, Set setMBeans, List listAgg, List listBy)
        {
        int cAgg = listAgg.size();
        int cBy  = listBy.size();

        if (cAgg > 0 && cBy > 0)
            {
            InvocableMap.EntryAggregator[] aEA = (InvocableMap.EntryAggregator[])
                listAgg.toArray(new InvocableMap.EntryAggregator[cAgg]);

            ValueExtractor[]    aVE = (ValueExtractor[]) listBy.toArray(new ValueExtractor[cBy]);
            CompositeAggregator CA  = CompositeAggregator.createInstance(aEA);
            MultiExtractor      ME  = new MultiExtractor(aVE);

            GroupAggregator ga = GroupAggregator.createInstance(ME, CA);

            return (Map) mBeanQuery.aggregate(setMBeans, ga);
            }

        return Collections.emptyMap();
        }

    /**
    * The get an aggregate value.
    *
    * @param oKey  the key for the aggregate
    * @param nPos  the position of the aggregate (returned from addAggregate)
    *
    * @return the aggregated value
    */
    public Object getAggValue(Object oKey, int nPos)
        {
        return m_fGroupBy ? getValue(oKey, nPos) : m_ialAgg.get(nPos);
        }

    /**
    * Convert an query key into a group by key.
    *
    * @param oKey  an ObjectName key
    *
    * @return a group by key based on the select key
    */
    protected ImmutableArrayList convertObjectName(ObjectName oKey)
        {
        int      nSize = m_listGroup.size();
        Object[] ao     = new Object[nSize];
        for (int i = 0; i < nSize; i++)
            {
            ao[i] = ((ImmutableArrayList) m_mapGroup.get(oKey)).get(i);
            }

        return new ImmutableArrayList(ao);
        }

    /**
    * The get an extracted value.
    *
    * @param oKey  the key to extract
    * @param nPos  the position of the Extractor (returned from addExtractor
    *
    * @return the extracted value
    */
    public Object getValue(Object oKey, int nPos)
        {
        Object oValKey = oKey instanceof ObjectName
                       ? convertObjectName((ObjectName) oKey)
                       : oKey;

        return ((ImmutableArrayList) m_mapExtractionResults.get(oValKey)).get(nPos);
        }

    /**
    * The get an extracted value.
    *
    * @param oKey  the key to extract
    * @param nPos  the position of the Extractor (returned from addExtractor)
    *
    * @return the extracted value
    */
    public Object getScalarValue(Object oKey, int nPos)
        {
        Object oValKey = oKey instanceof ObjectName
                       ? convertObjectName((ObjectName) oKey)
                       : oKey;

        return ((ImmutableArrayList) m_mapScalar.get(oValKey)).get(nPos);
        }

    /**
    * The get an extracted value.
    *
    * @param oKey  the key to extract
    * @param nPos  the position of the Extractor.  (returned from addExtractor
    *
    * @return the extracted Aggregate value
    */
    public Object getGroupValue(Object oKey, int nPos)
        {
        return getAggValue(oKey, nPos);
        }

    /**
    * Set the query to use a group by clause on the query.
    *
    * @param  fGroupBy true to apply a group by
    */
    public void setGroupBy(boolean fGroupBy)
        {
        m_fGroupBy = fGroupBy;
        }

    /**
    * Determine if the query uses a group by clause.
    *
    * @return true iff the query uses a group by
    */
    public boolean isGroupBy()
        {
        return m_fGroupBy;
        }

    /**
    * Get the results keyset for the group by query.
    *
    * @return a set of ImmutableArrayList objects which are the result keys
    */
    public Set getGroupKeys()
        {
        Map setResults = new TreeMap(new Comparator()
            {
            public int compare(Object o, Object o1)
                {
                ImmutableArrayList ial  = (ImmutableArrayList) o;
                ImmutableArrayList ial1 = (ImmutableArrayList) o1;
                int cThis = ial.size();
                int cThat = ial1.size();

                for (int i = 0, c = Math.min(cThis, cThat); i < c; i++)
                    {
                    Object oThis = ial.get(i);
                    Object oThat = ial1.get(i);
                    int    nResult;

                    if (oThis == null || oThat == null)
                        {
                        nResult = oThis == null ? (oThat == null ? 0 : -1) : 1;
                        }
                    else if (oThis instanceof ObjectName && oThat instanceof ObjectName)
                        {
                        String s  = ((ObjectName) oThis).getKeyPropertyListString();
                        String s1 = ((ObjectName) oThat).getKeyPropertyListString();
                        nResult = s.compareTo(s1);
                        }
                    else
                        {
                        nResult = ((Comparable) oThis).compareTo(oThat);
                        }

                    if (nResult != 0)
                        {
                        return nResult;
                        }
                    }

                return cThis - cThat;
                }
            });

        for (Iterator iter = m_mapExtractionResults.keySet().iterator(); iter.hasNext();)
            {
            setResults.put(iter.next(), null);
            }

        return setResults.keySet();
        }

    /**
    * Reset all attributes for next execution.
    */
    public void postProcess()
        {
        m_listVE.clear();
        m_listAgg.clear();
        m_listGroup.clear();
        m_listScalar.clear();
        if (m_mapExtractionResults != null)
            {
            m_mapExtractionResults.clear();
            }
        if (m_mapGroup != null)
            {
            m_mapGroup.clear();
            }
        if (m_mapIndex != null)
            {
            m_mapIndex.clear();
            }
        if (m_mapScalar != null)
            {
            m_mapScalar.clear();
            }
        }

    /**
    * Return an appropriate {@link MBeanServer} to use.
    *
    * @return an appropriate MBeanServer to use
    */
    public MBeanServer getMBeanServer()
        {
        MBeanServer mbs = m_mbs;
        if (mbs == null)
            {
            mbs = m_mbs = MBeanHelper.findMBeanServer();
            }
        return mbs;
        }


    /**
    * Begin wildcard join resolution for one report execution. The scope must
    * remain open through delta post-processing and be closed even on failure.
    * This DataSource, like the report pipeline, supports one execution at a time.
    *
    * @return the scope to close when execution ends
    *
    * @throws IllegalStateException if a scope is already active
    * @since 26.10
    */
    public JoinResolution beginJoinResolution()
        {
        if (m_joinResolution != null)
            {
            throw new IllegalStateException("Join resolution is already active");
            }
        return m_joinResolution = new JoinResolution(getMBeanServer());
        }

    /**
    * Resolve a wildcard join to its unique target, or retain the pattern when
    * no unique target exists. Within an active scope, the first result for each
    * pattern is reused, including missing and ambiguous results. Only names
    * are cached; the joined extractor continues to read attribute values.
    * Calls outside the scope or against a different server query directly.
    * Query exceptions propagate without being cached.
    *
    * @param server   the server used by the joined attribute extractor
    * @param pattern  the fully expanded join pattern
    *
    * @return the unique target or the unresolved pattern
    * @since 26.10
    */
    public ObjectName resolveJoinTarget(MBeanServer server, ObjectName pattern)
        {
        JoinResolution scope = m_joinResolution;
        Map<ObjectName, ObjectName> mapTargets = scope != null && scope.f_server == server
                ? scope.f_mapTargets : null;
        ObjectName target = mapTargets == null ? null : mapTargets.get(pattern);
        if (target == null)
            {
            Set<ObjectName> setNames = server.queryNames(pattern, null);
            target = setNames.size() == 1 ? setNames.iterator().next() : pattern;
            if (mapTargets != null)
                {
                mapTargets.put(pattern, target);
                }
            }
        return target;
        }

    // ----- inner class: JoinResolution ------------------------------------

    /**
    * Name resolution state for one execution. Closing releases all entries
    * without advancing delta baselines or performing report post-processing.
    *
    * @since 26.10
    */
    public class JoinResolution
            implements AutoCloseable
        {
        private JoinResolution(MBeanServer server)
            {
            f_server = server;
            }

        @Override
        public void close()
            {
            f_mapTargets.clear();
            if (m_joinResolution == this)
                {
                m_joinResolution = null;
                }
            }

        private final MBeanServer f_server;

        private final Map<ObjectName, ObjectName> f_mapTargets = new HashMap<>();
        }

    // ----- data members ---------------------------------------------------

    /**
    * Active report execution's wildcard join resolution state.
    */
    protected JoinResolution m_joinResolution;

    /**
    * The working list of detail ValueExtractors.
    */
    protected List m_listVE;

    /**
    * The working list of PostProcess Scalar Transformations.
    */
    protected List m_listScalar;

    /**
    * The working list of Aggregate ValueExtractors.
    */
    protected List m_listAgg;

    /**
    * The results of the detail extraction (content of rows and columns).
    */
    protected Map m_mapExtractionResults;

    /**
    * The map holding mBeans to the composite keys made by "group-by" extractors.
    */
    protected Map m_mapGroup;

    /**
    * The results from running the aggregate extractors against the keyset of beans.
    */
    protected ImmutableArrayList m_ialAgg;

    /**
    * The results from a scalar post process.
    */
    protected Map m_mapScalar;

    /**
    * The working list of ValueExtractors flagged as "group-by" that together are
    * used as the composite key for each row.
    */
    protected List m_listGroup;

    /**
    * Flag that is true iff the query contains a "group-by" clause.
    */
    protected boolean m_fGroupBy;

    /**
    * Map keyed by the group-by keys (ImmutableArrayList) values query keys
    * ObjectName.
    */
    protected Map m_mapIndex;

    /**
    * The {@link MBeanServer} this DataSource operates against.
    */
    protected MBeanServer m_mbs;
    }
