/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.ValueExtractor;

import java.util.Map;
import java.util.Set;


/**
 * Abstract base class implementation of {@link QueryRecorderFilter}.
 *
 * @param <T>  the type of the input argument to the filter
 *
 * @since Coherence 3.7.1
 *
 * @author tb 2011.05.26
 */
public abstract class AbstractQueryRecorderFilter<T>
        extends ExternalizableHelper
        implements QueryRecorderFilter<T>
    {
    // ----- QueryRecorderFilter interface ----------------------------------

    /**
    * {@inheritDoc}
    */
    public void explain(QueryContext ctx, QueryRecord.PartialResult.ExplainStep step, Set setKeys)
        {
        explain(this, ctx.getBackingMapContext().getIndexMap(), setKeys, step);
        }

    /**
    * {@inheritDoc}
    */
    public Filter trace(QueryContext ctx, QueryRecord.PartialResult.TraceStep step, Set setKeys)
        {
        return trace(this, ctx.getBackingMapContext().getIndexMap(), setKeys, step);
        }

    /**
    * {@inheritDoc}
    */
    public boolean trace(QueryContext ctx, QueryRecord.PartialResult.TraceStep step, Map.Entry entry)
        {
        return trace(this, entry, step);
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Record an estimated cost of query execution for a given filter.
     *
     * @param <T>         the type of the input argument to the filter
     * @param filter      the filter
     * @param mapIndexes  a read-only map of available MapIndex objects, keyed
     *                    by the associated ValueExtractor
     * @param setKeys     the mutable set of keys that remain to be filtered
     * @param step        the step used to record the execution cost
     */
    protected static <T> void explain(Filter<T> filter, Map mapIndexes,
                                  Set setKeys, QueryRecord.PartialResult.ExplainStep step)
        {
        ValueExtractor extractor = filter instanceof ExtractorFilter
                                 ? ((ExtractorFilter) filter).getValueExtractor()
                                 : null;

        explain(filter, mapIndexes, setKeys, step, extractor);
        }

    /**
     * Record an estimated cost of query execution for a given filter.
     *
     * @param <T>         the type of the input argument to the filter
     * @param filter      the filter
     * @param mapIndexes  a read-only map of available MapIndex objects, keyed
     *                    by the associated ValueExtractor
     * @param setKeys     the mutable set of keys that remain to be filtered
     * @param step        the step used to record the execution cost
     * @param extractor   an optional ValueExtractor used by the query
     */
    protected static <T> void explain(Filter<T> filter, Map mapIndexes,
                                  Set setKeys, QueryRecord.PartialResult.ExplainStep step,
                                  ValueExtractor extractor)
        {
        step.recordPreFilterKeys(setKeys.size());

        if (filter instanceof IndexAwareFilter)
            {
            if (extractor != null)
                {
                step.recordExtractor(extractor);
                }

            int nCost = ((IndexAwareFilter) filter).calculateEffectiveness(mapIndexes, setKeys);
            step.recordEfficiency(nCost < 0 ? setKeys.size() * ExtractorFilter.EVAL_COST : nCost);
            }
        }

    /**
     * Record the actual cost of applying the specified filter to the
     * specified keySet.
     *
     * @param <T>         the type of the input argument to the filter
     * @param filter      the filter
     * @param mapIndexes  a read-only map of available MapIndex objects, keyed
     *                    by the associated ValueExtractor
     * @param setKeys     the mutable set of keys that remain to be filtered
     * @param step        the step used to record the execution cost
     *
     * @return a {@link Filter} object (which may be an {@link
     *         EntryFilter}) that can be used to process the remaining
     *         keys, or null if no additional filter processing is
     *         necessary
     */
    protected static <T> Filter<T> trace(Filter<T> filter, Map mapIndexes,
                               Set setKeys, QueryRecord.PartialResult.TraceStep step)
        {
        ValueExtractor extractor = filter instanceof ExtractorFilter
                                 ? ((ExtractorFilter) filter).getValueExtractor()
                                 : null;

        return trace(filter, mapIndexes, setKeys, step, extractor);
        }

    /**
     * Record the actual cost of applying the specified filter to the
     * specified keySet.
     *
     * @param <T>         the type of the input argument to the filter
     * @param filter      the filter
     * @param mapIndexes  a read-only map of available MapIndex objects, keyed
     *                    by the associated ValueExtractor
     * @param setKeys     the mutable set of keys that remain to be filtered
     * @param step        the step used to record the execution cost
     * @param extractor   an optional ValueExtractor used by the query
     *
     * @return a {@link Filter} object (which may be an {@link
     *         EntryFilter}) that can be used to process the remaining
     *         keys, or null if no additional filter processing is
     *         necessary
     */
    protected static <T> Filter<T> trace(Filter<T> filter, Map mapIndexes,
                               Set setKeys, QueryRecord.PartialResult.TraceStep step,
                               ValueExtractor extractor)
        {
        step.recordPreFilterKeys(setKeys.size());

        if (filter instanceof IndexAwareFilter)
            {
            if (extractor != null)
                {
                step.recordExtractor(extractor);
                }

            long ldtStart = Base.getSafeTimeMillis();

            filter = ((IndexAwareFilter) filter).applyIndex(mapIndexes, setKeys);

            long ldtEnd = Base.getSafeTimeMillis();

            step.recordDuration(ldtEnd - ldtStart);
            }

        step.recordPostFilterKeys(setKeys.size());

        return filter;
        }

    /**
     * Record the actual cost of query execution for a given filter.
     *
     * @param <T>     the type of the input argument to the filter
     * @param filter  the filter
     * @param entry   the entry to be evaluated
     * @param step    the step used to record the execution cost
     *
     * @return true if the entry passes the filter, false otherwise
     */
    protected static <T> boolean trace(Filter<T> filter, Map.Entry entry,
                                QueryRecord.PartialResult.TraceStep step)
        {
        step.recordPreFilterKeys(1);

        long ldtStart = Base.getSafeTimeMillis();

        boolean fResult = InvocableMapHelper.evaluateEntry(filter, entry);

        long ldtEnd = Base.getSafeTimeMillis();

        step.recordDuration(ldtEnd - ldtStart);
        step.recordPostFilterKeys(fResult ? 1 : 0);

        return fResult;
        }
    }
