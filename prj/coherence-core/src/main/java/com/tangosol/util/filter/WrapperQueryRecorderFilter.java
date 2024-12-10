/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryRecord;

import java.util.Map;
import java.util.Set;


/**
 * QueryRecorderFilter wrapper class.
 *
 * @since  Coherence 3.7.1
 * @author tb 2011.06.05
 */
public class WrapperQueryRecorderFilter<T>
        extends AbstractQueryRecorderFilter<T>
        implements EntryFilter<Object, T>
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Construct a WrapperQueryRecorderFilter.
     *
     * @param filter  the filter to wrap
     */
    public WrapperQueryRecorderFilter(Filter<T> filter)
        {
        m_filter = filter;
        }


    // ----- Filter interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(T o)
        {
        return m_filter.evaluate(o);
        }


    // ----- EntryFilter interface ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return InvocableMapHelper.evaluateEntry(m_filter, entry);
        }


    // ----- QueryRecorderFilter interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    public void explain(QueryContext ctx,
                        QueryRecord.PartialResult.ExplainStep step,
                        Set setKeys)
        {
        explain(m_filter,
                ctx.getBackingMapContext().getIndexMap(), setKeys, step);
        }

    /**
     * {@inheritDoc}
     */
    public Filter trace(QueryContext ctx,
                        QueryRecord.PartialResult.TraceStep step,
                        Set setKeys)
        {
        return trace(m_filter,
                ctx.getBackingMapContext().getIndexMap(), setKeys, step);
        }

    /**
     * {@inheritDoc}
     */
    public boolean trace(QueryContext ctx,
                         QueryRecord.PartialResult.TraceStep step,
                         Map.Entry entry)
        {
        return trace(m_filter, entry, step);
        }


    // ----- data members ---------------------------------------------------

    /**
     * The wrapped filter.
     */
    private final Filter<T> m_filter;
    }
