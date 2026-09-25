/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A streaming aggregator marker used to query continuously maintained
 * partition results through the normal distributed aggregation pipeline.
 *
 * @param <R>  the final result type
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public class ContinuousAggregationQuery<R>
        implements InvocableMap.StreamingAggregator<Object, Object, Object, R>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an empty query for serialization.
     */
    public ContinuousAggregationQuery()
        {
        }

    /**
     * Construct a query for an unfiltered aggregator definition.
     *
     * @param aggregator  the pristine aggregator prototype
     */
    public ContinuousAggregationQuery(
            InvocableMap.StreamingAggregator<?, ?, ?, R> aggregator)
        {
        this(new ContinuousAggregationDefinition(null, aggregator));
        }

    /**
     * Construct a query for the specified continuous aggregation definition.
     *
     * @param definition  the complete registration definition
     */
    @SuppressWarnings("unchecked")
    public ContinuousAggregationQuery(ContinuousAggregationDefinition definition)
        {
        m_filter     = definition.getFilter();
        m_aggregator = (InvocableMap.StreamingAggregator<?, ?, ?, R>)
                definition.getAggregator();
        }

    // ----- StreamingAggregator methods -----------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, R> supply()
        {
        return new ContinuousAggregationQuery<>(getDefinition());
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<?, ?> entry)
        {
        throw new IllegalStateException(
                "continuous aggregation query was not intercepted by Storage");
        }

    @Override
    @SuppressWarnings("unchecked")
    public boolean combine(Object partialResult)
        {
        return ensureCombiner().combine(partialResult);
        }

    @Override
    public Object getPartialResult()
        {
        return ensureCombiner().getPartialResult();
        }

    @Override
    @SuppressWarnings("unchecked")
    public R finalizeResult()
        {
        return (R) ensureCombiner().finalizeResult();
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | BY_MEMBER;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the pristine aggregator prototype identifying the registration.
     *
     * @return the aggregator prototype
     */
    public InvocableMap.StreamingAggregator<?, ?, ?, R> getAggregator()
        {
        return m_aggregator;
        }

    /**
     * Return the complete definition identifying the registration.
     *
     * @return the continuous aggregation definition
     */
    public ContinuousAggregationDefinition getDefinition()
        {
        ContinuousAggregationDefinition definition = m_definition;
        if (definition == null)
            {
            m_definition = definition =
                    new ContinuousAggregationDefinition(m_filter, m_aggregator);
            }
        return definition;
        }

    // ----- ExternalizableLite methods ------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in)
            throws IOException
        {
        m_aggregator = ExternalizableHelper.readObject(in);
        m_filter     = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_aggregator);
        ExternalizableHelper.writeObject(out, m_filter);
        }

    // ----- PortableObject methods ----------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aggregator = in.readObject(0);
        m_filter     = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_aggregator);
        out.writeObject(1, m_filter);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the request-local result combiner.
     */
    @SuppressWarnings("unchecked")
    private InvocableMap.StreamingAggregator<Object, Object, Object, R> ensureCombiner()
        {
        InvocableMap.StreamingAggregator<Object, Object, Object, R> combiner = m_combiner;
        if (combiner == null)
            {
            if (m_aggregator == null)
                {
                throw new IllegalStateException("continuous aggregator definition is missing");
                }
            m_combiner = combiner =
                    (InvocableMap.StreamingAggregator<Object, Object, Object, R>) m_aggregator.supply();
            }
        return combiner;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The pristine aggregator prototype.
     */
    private InvocableMap.StreamingAggregator<?, ?, ?, R> m_aggregator;

    /**
     * The complete registration filter, including any client-side affinity
     * wrapper.
     */
    private Filter<?> m_filter;

    /**
     * The lazily reconstructed semantic definition.
     */
    private transient ContinuousAggregationDefinition m_definition;

    /**
     * The request-local combiner.
     */
    private transient InvocableMap.StreamingAggregator<Object, Object, Object, R> m_combiner;
    }
