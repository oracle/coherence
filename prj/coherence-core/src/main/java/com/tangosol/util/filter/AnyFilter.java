/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.SubSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
* Filter which returns the logical "or" of a filter array.
*
* @author cp/gg 2002.11.01
*/
public class AnyFilter
        extends ArrayFilter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public AnyFilter()
        {
        }

    /**
    * Construct an "any" filter. The result is defined as:
    * <blockquote>
    *   afilter[0] || afilter[1] ... || afilter[n]
    * </blockquote>
    *
    * @param afilter  an array of filters
    */
    public AnyFilter(Filter[] afilter)
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
            if (afilter[i].evaluate(o))
                {
                return true;
                }
            }
        return false;
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
            Filter filterN = aFilter[aFilter.length - 1];

            return filterN instanceof IndexAwareFilter
                ? ((IndexAwareFilter) filterN)
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
        Filter[] afilter = m_aFilter;
        for (int i = 0, c = afilter.length; i < c; i++)
            {
            Filter filter = afilter[i];

            if (evaluateFilter(filter, entry, ctx,
                    step == null ? null : step.ensureStep(filter)))
                {
                return true;
                }
            }
        return false;
        }

    /**
    * {@inheritDoc}
    */
    protected Filter applyIndex(Map mapIndexes, Set setKeys, QueryContext ctx,
            QueryRecord.PartialResult.TraceStep step)
        {
        optimizeFilterOrder(mapIndexes, setKeys);

        Filter[] aFilter    = m_aFilter;
        int      cFilters   = aFilter.length;
        List     listFilter = new ArrayList(cFilters);
        Set      setMatch   = new HashSet(setKeys.size());

        // listFilter is an array of filters that will have to be re-applied
        // setMatch   is an accumulating set of already matching keys

        for (int i = 0; i < cFilters; i++)
            {
            Filter filter = aFilter[i];
            if (filter instanceof IndexAwareFilter)
                {
                SubSet setRemain = new SubSet(setKeys);
                if (!setMatch.isEmpty())
                    {
                    setRemain.removeAll(setMatch);
                    }

                Filter filterDefer = applyFilter(filter, i, mapIndexes, setRemain, ctx, step);

                if (filterDefer == null)
                    {
                    // these are definitely "in"
                    setMatch.addAll(setRemain);
                    }
                else
                    {
                    int cKeys   = setKeys.size();
                    int cRemain = setRemain.size();
                    if (cRemain < cKeys)
                        {
                        // some keys are definitely "out" for this filter;
                        // we need to incorporate this knowledge into a deferred
                        // filter
                        if (cRemain > 0)
                            {
                            KeyFilter filterKey = new KeyFilter(setRemain);
                            listFilter.add(new AndFilter(filterDefer, filterKey));
                            }
                        else
                            {
                            // though a filter was returned, the key set was
                            // fully reduced; this should have the same effect
                            // as a fully resolved filter without any matches
                            }
                        }
                    else
                        {
                        listFilter.add(filterDefer);
                        }
                    }
                }
            else
                {
                listFilter.add(filter);
                }
            }

        int cMatches = setMatch.size();

        cFilters = listFilter.size();
        if (cFilters == 0)
            {
            if (cMatches > 0)
                {
                setKeys.retainAll(setMatch);
                }
            else
                {
                setKeys.clear();
                }
            return null;
            }
        else if (cFilters == 1 && cMatches == 0)
            {
            return (Filter) listFilter.get(0);
            }
        else
            {
            if (cMatches > 0)
                {
                // the keys that have been matched are definitely "in";
                // the remaining keys each need to be evaluated later
                KeyFilter filterKey = new KeyFilter(setMatch);
                listFilter.add(0, filterKey);
                cFilters++;
                }

            return new AnyFilter((Filter[]) listFilter.toArray(new Filter[cFilters]));
            }
        }
    }