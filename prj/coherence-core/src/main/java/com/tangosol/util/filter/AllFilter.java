/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
* Filter which returns the logical "and" of a filter array.
*
* @author cp/gg 2002.11.01
*/
public class AllFilter
        extends ArrayFilter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public AllFilter()
        {
        }

    /**
    * Construct an "all" filter. The result is defined as:
    * <blockquote>
    *   afilter[0] &amp;&amp; afilter[1] ... &amp;&amp; afilter[n]
    * </blockquote>
    *
    * @param afilter   an array of filters
    */
    public AllFilter(Filter[] afilter)
        {
        super(afilter);
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(Object o)
        {
        Filter[] afilter = m_aFilter;
        for (int i = 0, c = afilter.length; i < c; i++)
            {
            if (!afilter[i].evaluate(o))
                {
                return false;
                }
            }
        return true;
        }

    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        optimizeFilterOrder(mapIndexes, setKeys);

        Filter[] aFilter  = m_aFilter;
        int      cFilters = aFilter.length;

        if (cFilters > 0)
            {
            Filter filter0 = aFilter[0];

            return filter0 instanceof IndexAwareFilter
                ? ((IndexAwareFilter) filter0)
                    .calculateEffectiveness(mapIndexes, setKeys)
                : setKeys.size() * ExtractorFilter.EVAL_COST;
            }

        return 1;
        }


    // ----- ArrayFilter methods --------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateEntry(Map.Entry entry, QueryContext ctx,
                                    QueryRecord.PartialResult.TraceStep step)
        {
        for (Filter filter : m_aFilter)
            {
            if (!evaluateFilter(filter, entry, ctx,
                    step == null ? null : step.ensureStep(filter)))
                {
                return false;
                }
            }
        return true;
        }

    @Override
    protected Set<Filter<?>> simplifyFilters()
        {
        Set<Filter<?>> setFilters = new HashSet<>();
        for (Filter<?> filter : m_aFilter)
            {
            if (filter instanceof AllFilter)
                {
                // pull nested AND/ALL filters to top level
                setFilters.addAll(((AllFilter) filter).simplifyFilters());
                }
            else
                {
                // remove duplicates
                setFilters.add(filter);
                }
            }
        return setFilters;
        }

    @Override
    protected Filter applyIndex(Map mapIndexes, Set setKeys,
                                   QueryContext ctx, QueryRecord.PartialResult.TraceStep step)
        {
        optimizeFilterOrder(mapIndexes, setKeys);

        Filter[] aFilter    = m_aFilter;
        int      cFilters   = aFilter.length;
        List     listFilter = new ArrayList(cFilters);

        // listFilter is an array of filters that will have to be re-applied

        for (int i = 0; i < cFilters; i++)
            {
            Filter filter = aFilter[i];
            if (filter instanceof IndexAwareFilter)
                {
                Filter filterNew = applyFilter(filter, i, mapIndexes, setKeys, ctx, step);

                if (setKeys.isEmpty())
                    {
                    return null;
                    }

                if (filterNew != null)
                    {
                    listFilter.add(filterNew);
                    }
                }
            else if (filter != null)
                {
                listFilter.add(filter);
                }
            }

        cFilters = listFilter.size();
        if (cFilters == 0)
            {
            return null;
            }
        else if (cFilters == 1)
            {
            return (Filter) listFilter.get(0);
            }
        else
            {
            return new AllFilter((Filter[])
                listFilter.toArray(new Filter[cFilters]));
            }
        }

    protected String getName()
        {
        switch (m_aFilter.length)
            {
            case 1:
                return m_aFilter[0].getClass().getSimpleName();
            case 2:
                return "AndFilter";
            default:
                return super.getName();
            }
        }

    protected String getOperator()
        {
        return "AND";
        }
    }
