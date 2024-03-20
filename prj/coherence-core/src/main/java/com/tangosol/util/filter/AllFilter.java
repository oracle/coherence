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
import java.util.LinkedHashSet;
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
        Filter[] afilter = getFilters();
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

        Filter[] aFilter  = getFilters();
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
        Filter<?>[] aFilter = getFilters();
        for (Filter<?> filter : aFilter)
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
    protected Filter<?>[] simplifyFilters(Filter<?>[] aFilter)
        {
        Set<Filter<?>> setFilters = new LinkedHashSet<>();
        for (Filter<?> filter : aFilter)
            {
            if (filter instanceof AllFilter && !(filter instanceof BetweenFilter))
                {
                // pull nested AND/ALL filters to top level
                setFilters.addAll(List.of(((AllFilter) filter).getFilters()));
                }
            else
                {
                // remove duplicates
                setFilters.add(filter);
                }
            }
        return setFilters.toArray(Filter[]::new);
        }

    @Override
    protected Filter<?> applyIndex(Map mapIndexes, Set setKeys,
                                   QueryContext ctx, QueryRecord.PartialResult.TraceStep step)
        {
        optimizeFilterOrder(mapIndexes, setKeys);

        Filter<?>[]     aFilter    = getFilters();
        int             cFilters   = aFilter.length;
        List<Filter<?>> listFilter = new ArrayList<>(cFilters);

        // listFilter is an array of filters that will have to be re-applied

        for (int i = 0; i < cFilters; i++)
            {
            Filter<?> filter = aFilter[i];
            if (filter instanceof IndexAwareFilter)
                {
                Filter<?> filterNew = applyFilter(filter, i, mapIndexes, setKeys, ctx, step);

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
            return listFilter.get(0);
            }
        else
            {
            return new AllFilter(listFilter.toArray(new Filter[cFilters]));
            }
        }

    protected String getName()
        {
        switch (getFilters().length)
            {
            case 1:
                return getFilters()[0].getClass().getSimpleName();
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
