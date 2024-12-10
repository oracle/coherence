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

import java.util.Map;
import java.util.Set;


/**
 * QueryRecorderFilter is an extension of EntryFilter that allows the
 * projected or actual costs of query execution to be recorded.
 * <p>
 * During a query execution each filter performs one or more logical operations
 * called "steps". Filters that implement this interface are expected
 * to produce an estimated cost of execution by the
 * {@link #explain(QueryContext, com.tangosol.util.QueryRecord.PartialResult.ExplainStep, Set) explain} method
 * and the actual execution cost by the
 * {@link #trace(QueryContext, com.tangosol.util.QueryRecord.PartialResult.TraceStep, java.util.Set)} and
 * {@link #trace(QueryContext, com.tangosol.util.QueryRecord.PartialResult.TraceStep, java.util.Map.Entry)}
 * methods.
 *
 * @since Coherence 3.7.1
 * @author tb 2011.05.26
 */
public interface QueryRecorderFilter<T>
    extends Filter<T>
    {
    /**
     * Record the projected query execution cost by this filter.
     * <p>
     * This method is expected to record the order of execution and estimated
     * cost of applying corresponding indexes in the given
     * {@link com.tangosol.util.QueryRecord.PartialResult.ExplainStep step} without actually applying any
     * indexes or evaluating entries.
     *
     * @param ctx      the query context
     * @param step     the step used to record the estimated execution cost
     * @param setKeys  the set of keys that would be filtered
     */
    public void explain(QueryContext ctx, QueryRecord.PartialResult.ExplainStep step, Set setKeys);

    /**
     * Filter the given keys using available indexes and record the cost
     * of execution on the given step of the {@link com.tangosol.util.QueryRecord}.
     * <p>
     * This method should record the size of the given key set before and
     * after applying corresponding indexes using
     * {@link com.tangosol.util.QueryRecord.PartialResult.TraceStep#recordPreFilterKeys(int)} and
     * {@link com.tangosol.util.QueryRecord.PartialResult.TraceStep#recordPostFilterKeys(int)}
     * as well as the corresponding execution time using the
     * {@link com.tangosol.util.QueryRecord.PartialResult.TraceStep#recordDuration(long)} method.
     * <p>
     * This method is only called if the filter is an {@link IndexAwareFilter}
     * and its implementations should explicitly call {@link
     * IndexAwareFilter#applyIndex(Map, Set) applyIndex()} to actually perform
     * the query. Additionally, this method should return the filter object (if
     * any) returned by the <i>applyIndex()</i> call.
     *
     * @param ctx      the query context
     * @param step     the step used to record the execution cost
     * @param setKeys  the mutable set of keys that remain to be filtered
     *
     * @return the filter returned from {@link IndexAwareFilter#applyIndex(Map, Set)}
     */
    public Filter trace(QueryContext ctx, QueryRecord.PartialResult.TraceStep step, Set setKeys);

    /**
     * Evaluate the specified entry against this filter and record the evaluation
     * cost on the given step of the {@link com.tangosol.util.QueryRecord}.
     * <p>
     * This method should record the corresponding latencies using
     * {@link com.tangosol.util.QueryRecord.PartialResult.TraceStep#recordDuration(long)}.
     * <p>
     * Implementations are responsible for explicitly calling {@link
     * EntryFilter#evaluateEntry(Map.Entry) evaluateEntry()} method to perform
     * the actual entry evaluation. Additionally, this method should return the
     * result of the <i>evaluateEntry</i> call.
     *
     * @param ctx    the context
     * @param step   the step used to record the evaluation cost
     * @param entry  the entry to evaluate
     *
     * @return the result returned from {@link EntryFilter#evaluateEntry(Map.Entry)}
     */
    public boolean trace(QueryContext ctx, QueryRecord.PartialResult.TraceStep step, Map.Entry entry);
    }
