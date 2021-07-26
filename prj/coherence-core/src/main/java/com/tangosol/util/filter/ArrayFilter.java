/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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
    */
    public ArrayFilter(Filter[] aFilter)
        {
        azzert(aFilter != null);
        for (int i = 0, c = aFilter.length; i < c; i++)
            {
            azzert(aFilter[i] != null, "Null element");
            }
        m_aFilter = aFilter;
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

        for (Filter filter : m_aFilter)
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
    public Filter[] getFilters()
        {
        return m_aFilter;
        }

    /**
    * Ensure that the order of underlying filters is preserved by the
    * {@link #applyIndex} and {@link #evaluateEntry} implementations.
    */
    public void honorOrder()
        {
        m_fPreserveOrder = true;
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
        if (m_fPreserveOrder)
            {
            return;
            }

        Filter[]         aFilter  = m_aFilter;
        int              cFilters = aFilter.length;
        WeightedFilter[] awf      = new WeightedFilter[cFilters];
        int              nMax     = setKeys.size()*ExtractorFilter.EVAL_COST;
        boolean          fSort    = false;
        int              nEffect0 = -1;

        for (int i = 0; i < cFilters; i++)
            {
            Filter filter  = aFilter[i];
            int    nEffect = filter instanceof IndexAwareFilter
                ? ((IndexAwareFilter) filter)
                    .calculateEffectiveness(mapIndexes, setKeys)
                : nMax;

            awf[i] = new WeightedFilter(filter, nEffect);

            if (i == 0)
                {
                nEffect0 = nEffect;
                }
            else
                {
                // only need to sort if the weights are different
                fSort |= (nEffect != nEffect0);
                }
            }

        if (fSort)
            {
            Arrays.sort(awf);
            for (int i = 0; i < cFilters; i++)
                {
                aFilter[i] = awf[i].getFilter();
                }
            }
        m_fPreserveOrder = true;
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
        String        sClass = getClass().getName();
        StringBuilder sb     = new StringBuilder(
            sClass.substring(sClass.lastIndexOf('.') + 1));

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


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        int cFilters = readInt(in);
        azzert(cFilters < 16384, "Unexpected number of filters.");

        Filter<Object>[] aFilter  = new Filter[cFilters];

        for (int i = 0; i < cFilters; i++)
            {
            aFilter[i] = (Filter) readObject(in);
            }
        m_aFilter = aFilter;

        m_fPreserveOrder = in.readBoolean();
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
        for (int i = 0; i < cFilters; i++)
            {
            writeObject(out, aFilter[i]);
            }

        out.writeBoolean(m_fPreserveOrder);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aFilter = (Filter<Object>[]) in.readObjectArray(0, EMPTY_FILTER_ARRAY);

        // if we read an old version of the filter that didn't have this field,
        // it would result in maintaining the old behavior
        m_fPreserveOrder = in.readBoolean(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObjectArray(0, m_aFilter);
        out.writeBoolean(1, m_fPreserveOrder);
        }


    // ----- inner class: WeightedFilter ------------------------------------

    /**
    * A thin wrapper around a Filter allowing for sorting the filters
    * according to their effectiveness.
    */
    protected static class WeightedFilter
            implements Comparable
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
        * @param o  the Object to be compared
        *
        * @return a negative integer, zero, or a positive integer as this
        *         object is less than, equal to, or greater than the
        *         specified object
        *
        * @throws ClassCastException if the specified object's type prevents
        *         it from being compared to this WeightedFilter
        */
        public int compareTo(Object o)
            {
            int nThis = m_nEffect;
            int nThat = ((WeightedFilter) o).m_nEffect;

            return (nThis < nThat ? -1 : (nThis > nThat ? +1 : 0));
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
        private Filter m_filter;

        /**
        * The effectiveness of the wrapped filter.
        */
        private int m_nEffect;
        }


    // ----- constants ------------------------------------------------------

    /**
    * A zero-length array of Filter objects.
    */
    private static final Filter[] EMPTY_FILTER_ARRAY = new Filter[0];


    // ----- data members ---------------------------------------------------

    /**
    * The Filter array.
    */
    @JsonbProperty("filters")
    protected Filter[] m_aFilter;

    /**
    * Flag indicating whether or not the filter order should be preserved.
    *
    * @since Coherence 12.2.1
    */
    @JsonbProperty("preserveOrder")
    protected boolean m_fPreserveOrder;
    }
