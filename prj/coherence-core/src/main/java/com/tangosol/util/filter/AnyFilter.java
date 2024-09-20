/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.ChainedCollection;
import com.tangosol.util.Filter;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.SubSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
* Filter which returns the logical "or" of a filter array.
*
* @author cp/gg 2002.11.01
*/
@SuppressWarnings({"unchecked", "rawtypes"})
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
        Filter[] afilter = getFilters();
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

        Filter[] aFilter  = getFilters();
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
    @Override
    protected boolean evaluateEntry(Map.Entry entry, QueryContext ctx,
                                    QueryRecord.PartialResult.TraceStep step)
        {
        Filter[] afilter = getFilters();
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
    @Override
    protected Filter<?>[] simplifyFilters(Filter<?>[] aFilter)
        {
        Set<Filter<?>> setFilters = new LinkedHashSet<>();
        for (Filter<?> filter : aFilter)
            {
            if (filter instanceof AnyFilter)
                {
                // pull nested OR/ANY filters to top level
                setFilters.addAll(List.of(((AnyFilter) filter).getFilters()));
                }
            else
                {
                // remove duplicates
                setFilters.add(filter);
                }
            }
        return setFilters.toArray(Filter[]::new);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected Filter applyIndex(Map mapIndexes, Set setKeys, QueryContext ctx,
            QueryRecord.PartialResult.TraceStep step)
        {
        optimizeFilterOrder(mapIndexes, setKeys);

        Filter[]        aFilter    = getFilters();
        int             cFilters   = aFilter.length;

        // a list of filters that will have to be re-applied
        List<Filter<?>> listFilter = new ArrayList<>(cFilters);

        // a list of sets of matching keys, which will be subsequently merged
        // into a set of keys that should be retained; doing it this way, instead
        // of simply creating a set and calling addAll on it for each filter
        // avoids set resizing and rehashing of the keys
        List<Set<?>>    listMatch  = new ArrayList<>(cFilters);

        // iterate backwards, from the least selective filter to the most selective one,
        // to ensure that the largest sets of matching keys are at the beginning
        // of a ChainedCollection, making subsequent retainAll call faster
        for (int i = cFilters - 1; i >= 0; i--)
            {
            Filter filter = aFilter[i];
            if (filter instanceof IndexAwareFilter)
                {
                SubSet setRemain   = new SubSet(setKeys);
                Filter filterDefer = applyFilter(filter, i, mapIndexes, setRemain, ctx, step);
                Set    setRetained = setRemain.getRetained();
                
                if (filterDefer == null)
                    {
                    if (!setRetained.isEmpty())
                        {
                        // these are definitely "in"
                        listMatch.add(setRetained);
                        }
                    }
                else
                    {
                    if (!setRemain.getRemoved().isEmpty())
                        {
                        // some keys are definitely "out" for this filter;
                        // we need to incorporate this knowledge into a deferred
                        // filter
                        if (!setRemain.isEmpty())
                            {
                            KeyFilter filterKey = new KeyFilter(setRetained);
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
            else if (filter != null)
                {
                listFilter.add(filter);
                }
            }

        // create a set containing all the matches identified by individual filters
        Set<?> setMatches = new HashSet<>(new ChainedCollection<>(listMatch.toArray(Set[]::new)));

        cFilters = listFilter.size();
        if (cFilters == 0)
            {
            if (setMatches.isEmpty())
                {
                setKeys.clear();
                }
            else
                {
                setKeys.retainAll(setMatches);
                }
            return null;
            }
        else if (cFilters == 1 && setMatches.isEmpty())
            {
            return listFilter.get(0);
            }
        else
            {
            if (!setMatches.isEmpty())
                {
                // the keys that have been matched are definitely "in";
                // the remaining keys each need to be evaluated later
                KeyFilter filterKey = new KeyFilter(setMatches);
                listFilter.add(0, filterKey);
                cFilters++;
                }

            return new AnyFilter(listFilter.toArray(new Filter[cFilters]));
            }
        }

    protected String getName()
        {
        switch (getFilters().length)
            {
            case 1:
                return getFilters()[0].getClass().getSimpleName();
            case 2:
                return "OrFilter";
            default:
                return super.getName();
            }
        }

    protected String getOperator()
        {
        return "OR";
        }
    }
