/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.QueryContext;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which is a logical operator of a filter array.
*
* @author cp/gg 2002.11.01
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class ArrayFilter
        extends ExternalizableHelper
        implements EntryFilter, IndexAwareFilter, QueryRecorderFilter, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ArrayFilter()
        {
        }

    /**
    * Construct a logical filter that applies a binary operator to a
    * filter array. The result is defined as:
    * <blockquote>
    *   aFilter[0] &lt;op&gt; aFilter[1] ... &lt;op&gt; aFilter[n]
    * </blockquote>
    *
    * @param aFilter  the filter array
    *
    * @exception NullPointerException thrown if the array or any member of the array is null
    */
    public ArrayFilter(Filter<?>[] aFilter)
        {
        Objects.requireNonNull(aFilter);

        for (int i = 0, c = aFilter.length; i < c; i++)
            {
            int j = i;
            Objects.requireNonNull(aFilter[i], () -> String.format("Null element %d: %s", j, Arrays.toString(aFilter)));
            }

        m_aFilter = simplifyFilters(aFilter);
        }


    // ----- Filter interface -----------------------------------------------

    public String toExpression()
        {
        String        sOperator = getOperator();
        StringBuilder sb        = new StringBuilder();

        sb.append('(');

        Filter[] aFilter = m_aFilter;
        for (int i = 0, c = aFilter.length; i < c; i++)
            {
            if (i > 0)
                {
                sb.append(' ').append(sOperator).append(' ');
                }
            sb.append(aFilter[i] == null ? null : aFilter[i].toExpression());
            }

        sb.append(')');

        return sb.toString();
        }

    protected String getOperator()
        {
        throw new UnsupportedOperationException();
        }

    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return evaluateEntry(entry, null, null);
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        return applyIndex(mapIndexes, setKeys, null, null);
        }


    // ----- QueryRecorderFilter interface ----------------------------------

    /**
    * {@inheritDoc}
    */
    public void explain(QueryContext ctx, QueryRecord.PartialResult.ExplainStep step, Set setKeys)
        {
        optimizeFilterOrder(ctx.getBackingMapContext().getIndexMap(), setKeys);

        Filter<?>[] aFilter = getFilters();
        for (Filter filter : aFilter)
            {
            QueryRecord.PartialResult.ExplainStep subStep = step.ensureStep(filter);

            QueryRecorderFilter filterRecorder = filter instanceof QueryRecorderFilter
                    ? (QueryRecorderFilter) filter
                    : new WrapperQueryRecorderFilter(filter);

            filterRecorder.explain(ctx, subStep, setKeys);
            }
        }

    /**
    * {@inheritDoc}
    */
    public Filter trace(QueryContext ctx, QueryRecord.PartialResult.TraceStep step, Set setKeys)
        {
        step.recordPreFilterKeys(setKeys.size());

        long ldtStart = System.currentTimeMillis();

        Filter filterRemaining = applyIndex(ctx.getBackingMapContext().getIndexMap(), setKeys, ctx, step);

        long ldtEnd = System.currentTimeMillis();

        step.recordPostFilterKeys(setKeys.size());
        step.recordDuration(ldtEnd - ldtStart);

        return filterRemaining;
        }

    /**
    * {@inheritDoc}
    */
    public boolean trace(QueryContext ctx, QueryRecord.PartialResult.TraceStep step, Map.Entry entry)
        {
        step.recordPreFilterKeys(1);

        long ldtStart = System.currentTimeMillis();
        boolean fResult = evaluateEntry(entry, ctx, step);

        long ldtEnd = System.currentTimeMillis();

        step.recordPostFilterKeys(fResult ? 1 : 0);
        step.recordDuration(ldtEnd - ldtStart);

        return fResult;
        }


    // ----- QueryRecorderFilter support ------------------------------------

    /**
    * Apply the specified IndexAwareFilter to the specified keySet.  Record
    * the actual cost of execution for each of the participating filters if a
    * query context is provided.
    *
    * @param mapIndexes  the available MapIndex objects keyed by
    *                    the related ValueExtractor; read-only
    * @param setKeys     the set of keys that would be filtered
    * @param ctx         the query ctx; may be null
    * @param step        the step used to record the execution cost
    *
    * @return a Filter object that can be used to process the remaining
    *         keys, or null if no additional filter processing is necessary
    */
    protected abstract Filter applyIndex(Map mapIndexes, Set setKeys,
            QueryContext ctx, QueryRecord.PartialResult.TraceStep step);

    /**
    * Check if the given entry passes the filter's evaluation.  Record the
    * actual cost of execution for each of the participating filters if a
    * query context is provided.
    *
    * @param entry  a key value pair to filter
    * @param ctx    the query ctx; may be null
    * @param step   the step used to record the execution cost
    *
    * @return true   if the entry passes the filter, false otherwise
    */
    protected abstract boolean evaluateEntry(Map.Entry entry,
            QueryContext ctx, QueryRecord.PartialResult.TraceStep step);


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the Filter array.
    *
    * @return the Filter array
    */
    public Filter<?>[] getFilters()
        {
        Filter<?>[] filters = f_aFilterOptimized.get();
        return filters == null ? m_aFilter : filters;
        }

    /**
    * Ensure that the order of underlying filters is preserved by the
    * {@link #applyIndex} and {@link #evaluateEntry} implementations.
    */
    @Deprecated(forRemoval = true)
    public void honorOrder()
        {
        m_fOptimized = true;
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Sort all the participating filters according to their effectiveness.
    *
    * @param mapIndexes  the available MapIndex objects keyed by
    *                    the related ValueExtractor; read-only
    * @param setKeys     the set of keys that will be filtered; read-only
    */
    protected void optimizeFilterOrder(Map mapIndexes, Set setKeys)
        {
        if (m_fOptimized)
            {
            return;
            }

        int              cFilters  = m_aFilter.length;
        WeightedFilter[] aWeighted = new WeightedFilter[cFilters];
        Filter<?>[]      aFilter   = new Filter[cFilters];
        int              nMax      = setKeys.size() * ExtractorFilter.EVAL_COST;
        int              nPos      = 0;

        for (Filter<?> filter : m_aFilter)
            {
            int nEffect = filter instanceof IndexAwareFilter
                ? ((IndexAwareFilter) filter).calculateEffectiveness(mapIndexes, setKeys)
                : nMax;

            if (nEffect < 0)   // there is no index to apply
                {
                nEffect = nMax;
                }

            aWeighted[nPos++] = new WeightedFilter(filter, nEffect);
            }

        Arrays.sort(aWeighted);
        for (int i = 0; i < cFilters; i++)
            {
            aFilter[i] = aWeighted[i].getFilter();
            }

        f_aFilterOptimized.set(aFilter);
        m_fOptimized = true;
        }

    /**
     * Simplify internal filter array by merging and replacing filters if possible to reduce
     * the overall number and nesting of the filters.
     *
     * @return the simplified filter array
     */
    protected Filter<?>[] simplifyFilters(Filter<?>[] aFilters)
        {
        return aFilters;
        }


    /**
    * Apply the specified IndexAwareFilter to the specified keySet.  Record
    * the actual cost of execution if a query context is provided.
    *
    * @param filter      the IndexAwareFilter to apply an index to
    * @param iFilter     the index of the given filter in this filter's array
    *                    of indexes
    * @param mapIndexes  the available MapIndex objects keyed by
    *                    the related ValueExtractor; read-only
    * @param setKeys     the mutable set of keys that remain to be filtered
    * @param ctx         the query ctx; may be null
    * @param step        the step used to record the execution cost
    *
    * @return a Filter object that can be used to process the remaining
    *         keys, or null if no additional filter processing is necessary
    */
    protected Filter applyFilter(Filter filter, int iFilter, Map mapIndexes, Set setKeys,
                                    QueryContext ctx, QueryRecord.PartialResult.TraceStep step)
        {
        if (ctx == null)
            {
            return ((IndexAwareFilter) filter).applyIndex(mapIndexes, setKeys);
            }

        QueryRecord.PartialResult.TraceStep subStep = step.ensureStep(filter);

        QueryRecorderFilter filterRecorder = filter instanceof QueryRecorderFilter
                ? (QueryRecorderFilter) filter
                : new WrapperQueryRecorderFilter(filter);

        return filterRecorder.trace(ctx, subStep, setKeys);
        }

    /**
    * Check if the given entry passes the given filter's evaluation.  Record
    * the actual cost of execution if a query context is provided.
    *
    * @param filter  the Filter to use to evaluate the entry
    * @param entry   a key value pair to filter
    * @param ctx     the query ctx; may be null
    * @param step    the step used to record the execution cost
    *
    * @return true if the entry passes the filter, false otherwise
    */
    protected boolean evaluateFilter(Filter filter, Map.Entry entry,
            QueryContext ctx, QueryRecord.PartialResult.TraceStep step)
        {
        if (ctx == null)
            {
            return InvocableMapHelper.evaluateEntry(filter, entry);
            }

        QueryRecorderFilter filterRecorder = filter instanceof QueryRecorderFilter
                ? (QueryRecorderFilter) filter
                : new WrapperQueryRecorderFilter(filter);

        return filterRecorder.trace(ctx, step, entry);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ArrayFilter with another object to determine equality.
    * Two ArrayFilter objects are considered equal iff they belong to
    * exactly the same class and their filter arrays are equal.
    *
    * @return true iff this ArrayFilter and the passed object are
    *         equivalent ArrayFilters
    */
    public boolean equals(Object o)
        {
        if (o instanceof ArrayFilter)
            {
            ArrayFilter that = (ArrayFilter) o;
            return this.getClass() == that.getClass()
                && equalsDeep(this.m_aFilter, that.m_aFilter);
            }

        return false;
        }

    /**
    * Determine a hash value for the ArrayFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ArrayFilter object
    */
    public int hashCode()
        {
        int      iHash   = 0;
        Filter[] aFilter = m_aFilter;
        for (int i = 0, c = aFilter.length; i < c; i++)
            {
            Filter filter = aFilter[i];
            iHash += filter == null ? 0 : filter.hashCode();
            }
        return iHash;
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        String        sName = getName();
        StringBuilder sb    = new StringBuilder(sName);

        sb.append('(');

        Filter[] aFilter = m_aFilter;
        for (int i = 0, c = aFilter.length; i < c; i++)
            {
            if (i > 0)
                {
                sb.append(", ");
                }
            sb.append(aFilter[i]);
            }

        sb.append(')');

        return sb.toString();
        }

    protected String getName()
        {
        return getClass().getSimpleName();
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        int cFilters = readInt(in);
        azzert(cFilters < 16384, "Unexpected number of filters.");

        Filter[] aFilter  = new Filter[cFilters];

        for (int i = 0; i < cFilters; i++)
            {
            aFilter[i] = readObject(in);
            }
        m_aFilter = aFilter;

        m_fOptimized = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        Filter[] aFilter  = m_aFilter;
        int      cFilters = aFilter.length;

        writeInt(out, cFilters);
        for (Filter filter : aFilter)
            {
            writeObject(out, filter);
            }

        out.writeBoolean(m_fOptimized);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aFilter = in.readArray(0, Filter[]::new);

        // if we read an old version of the filter that didn't have this field,
        // it would result in maintaining the old behavior
        m_fOptimized = in.readBoolean(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObjectArray(0, m_aFilter);
        out.writeBoolean(1, m_fOptimized);
        }


    // ----- inner class: WeightedFilter ------------------------------------

    /**
    * A thin wrapper around a Filter allowing for sorting the filters
    * according to their effectiveness.
    */
    protected static class WeightedFilter
            implements Comparable<WeightedFilter>
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct the WeightedFilter.
        *
        * @param filter   the wrapped filter
        * @param nEffect  the filter's effectiveness
        */
        protected WeightedFilter(Filter filter, int nEffect)
            {
            m_filter  = filter;
            m_nEffect = nEffect;
            }

        // ----- Comparable interface ---------------------------------------

        /**
        * Compares this WeightedFilter with the specified WeightedFilter for
        * order.  Returns a negative integer, zero, or a positive integer as
        * this WeightedFilter's effectiveness is less than, equal to, or
        * greater than the effectiveness of the specified WeightedFilter
        * object.
        *
        * @param that  the Object to be compared
        *
        * @return a negative integer, zero, or a positive integer as this
        *         object is less than, equal to, or greater than the
        *         specified object
        *
        * @throws ClassCastException if the specified object's type prevents
        *         it from being compared to this WeightedFilter
        */
        public int compareTo(WeightedFilter that)
            {
            return Integer.compare(m_nEffect, that.m_nEffect);
            }

        // ----- accessors --------------------------------------------------

        /**
        * Get the wrapped filter.
        *
        * @return the wrapped filter
        */
        public Filter getFilter()
            {
            return m_filter;
            }

        // ----- data members -----------------------------------------------

        /**
        * The wrapped filter.
        */
        private final Filter m_filter;

        /**
        * The effectiveness of the wrapped filter.
        */
        private final int m_nEffect;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The Filter array.
    */
    @JsonbProperty("filters")
    private Filter<?>[] m_aFilter;

    /**
    * Flag indicating whether the filter has been optimized.
    */
    @JsonbProperty("optimized")
    private volatile boolean m_fOptimized;

    /**
     * The (thread-local) array of filters optimized/reordered based on index effectiveness.
     *
     * @implNote Note that we cannot reuse internal filter array for this, as the optimal
     *           filter order may be different for each partition, based on the index contents,
     *           so we have to perform filter reordering per partition, on different FJP threads
     *           (which is why we need this to be a thread-local, and not just a normal field).
     */
    private final transient ThreadLocal<Filter<?>[]> f_aFilterOptimized = new ThreadLocal<>();
    }
