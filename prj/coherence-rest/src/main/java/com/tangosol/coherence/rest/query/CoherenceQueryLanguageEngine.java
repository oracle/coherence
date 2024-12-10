/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExtractorBuilder;
import com.tangosol.coherence.dslquery.UniversalExtractorBuilder;

import com.tangosol.coherence.rest.util.ComparatorHelper;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.QueryHelper;
import com.tangosol.util.SubList;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coherence QL-based implementation of {@link QueryEngine}.
 * <p>
 * The query expression consumed by this implementation is the "where"
 * predicate of the Coherence Query Language.
 * <p>
 * Note: Although this query engine supports paging, the current implementation
 * still handles entire result set on the client. That can lead to massive
 * memory consumption (and possible <tt>OutOfMemoryError</tt>s) if invoked on
 * large caches with an unrestricted query.
 *
 * @author ic  2011.12.04
 */
@SuppressWarnings({"unchecked"})
public class CoherenceQueryLanguageEngine
        extends AbstractQueryEngine
    {
    // ----- constructors ---------------------------------------------------

    public CoherenceQueryLanguageEngine()
        {
        f_language = new CoherenceQueryLanguage();
        f_language.setExtractorBuilder(EXTRACTOR_BUILDER);
        }


    // ----- QueryEngine interface ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public Query prepareQuery(String sQuery, Map<String, Object> mapParams)
        {
        if (sQuery == null)
            {
            return new CoherenceQueryLanguageQuery(AlwaysFilter.INSTANCE);
            }
        if (mapParams == null)
            {
            mapParams = Collections.emptyMap();
            }

        ParsedQuery         parsedQuery = parseQueryString(sQuery);
        Map<String, Object> mapBindings = createBindings(mapParams,
                parsedQuery.getParameterTypes());

        Filter filter = QueryHelper.createFilter(
                parsedQuery.getQuery(),
                new Object[0],
                mapBindings,
                f_language
                );

        return new CoherenceQueryLanguageQuery(filter);
        }

    // ----- inner class: CoherenceQueryLanguageQuery -----------------------

    /**
     * CohQL implementation of Query interface.
     */
    private static class CoherenceQueryLanguageQuery
            implements Query
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a CoherenceQueryLanguageQuery instance.
         *
         * @param filter  a Filter that this Query wraps
         */
        private CoherenceQueryLanguageQuery(Filter filter)
            {
            m_filter = filter;
            }

        // ----- Query implementation ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public <E> Collection<E> execute(NamedCache cache, ValueExtractor<Map.Entry, ? extends E> extractor, String sOrder, int nStart, int cResults)
            {
            Comparator     comparator = null;
            Set<Map.Entry> setEntries;
            try
                {
                if (sOrder != null && sOrder.length() > 0)
                    {
                    comparator = ComparatorHelper.createComparator(sOrder);
                    }

                setEntries = comparator == null
                        ? cache.entrySet(m_filter)
                        : cache.entrySet(m_filter, comparator);
                }
            catch (Exception e)
                {
                throw new QueryException(e);
                }

            List listValues = new ArrayList(setEntries.size());
            for (Map.Entry entry : setEntries)
                {
                listValues.add(extractor.extract(entry));
                }
            listValues = new SubList(listValues, nStart, cResults);

            return listValues;
            }

        /**
         * {@inheritDoc}
         */
        public Set keySet(NamedCache cache)
            {
            return cache.keySet(m_filter);
            }

        // ----- data members -----------------------------------------------

        private final Filter m_filter;
        }

    // ----- constants ------------------------------------------------------



    /**
     * ExtractorBuilder to use {@link UniversalExtractorBuilder}.
     */
    public static final ExtractorBuilder EXTRACTOR_BUILDER = new UniversalExtractorBuilder();

    // ----- data members ---------------------------------------------------

    protected final CoherenceQueryLanguage f_language;
    }
