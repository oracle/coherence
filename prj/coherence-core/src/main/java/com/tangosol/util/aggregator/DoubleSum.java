/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;


/**
* Sums up numeric values extracted from a set of entries in a Map. All the
* extracted Number objects will be treated as Java <tt>double</tt> values.
*
* @param <T>  the type of the value to extract from
*
* @author gg  2005.09.05
* @since Coherence 3.1
*/
public class DoubleSum<T>
        extends AbstractDoubleAggregator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public DoubleSum()
        {
        super();
        }

    /**
    * Construct a DoubleSum aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public DoubleSum(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct a DoubleSum aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public DoubleSum(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, Double> supply()
        {
        return new DoubleSum<>(getValueExtractor());
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY | CONTINUOUS | STATE_CHECKPOINTABLE;
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        super.init(fFinal);

        m_dflResult = 0.0;
        }

    /**
    * {@inheritDoc}
    */
    protected void process(Object o, boolean fFinal)
        {
        if (o != null)
            {
            m_dflResult += ((Number) o).doubleValue();
            m_count++;
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected InvocableMap.StreamingAggregator.RetractionResult remove(Object o)
        {
        if (o != null)
            {
            if (m_count == 0)
                {
                return InvocableMap.StreamingAggregator.RetractionResult.REBUILD_REQUIRED;
                }

            double dfl = ((Number) o).doubleValue();
            if (m_count > 1 && !Double.isFinite(dfl))
                {
                return InvocableMap.StreamingAggregator.RetractionResult.REBUILD_REQUIRED;
                }

            m_dflResult -= dfl;
            if (--m_count == 0)
                {
                m_dflResult = 0.0;
                }
            }

        return InvocableMap.StreamingAggregator.RetractionResult.UPDATED;
        }

    @Override
    protected InvocableMap.StreamingAggregator.RetractionResult replace(
            Object oOriginal, Object oCurrent)
        {
        if (oOriginal == null || oCurrent == null)
            {
            return super.replace(oOriginal, oCurrent);
            }
        if (m_count == 0)
            {
            return InvocableMap.StreamingAggregator.RetractionResult.REBUILD_REQUIRED;
            }

        double dflOriginal = ((Number) oOriginal).doubleValue();
        if (m_count > 1 && !Double.isFinite(dflOriginal))
            {
            return InvocableMap.StreamingAggregator.RetractionResult.REBUILD_REQUIRED;
            }

        m_dflResult -= dflOriginal;
        m_dflResult += ((Number) oCurrent).doubleValue();
        return InvocableMap.StreamingAggregator.RetractionResult.UPDATED;
        }

    @Override
    protected boolean isReplacementRequired(Object oOriginal, Object oCurrent)
        {
        return true;
        }
    }
