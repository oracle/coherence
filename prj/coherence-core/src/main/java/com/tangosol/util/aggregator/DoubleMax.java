/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.internal.net.ContinuousAggregationBound;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;


/**
* Calculates a maximum of numeric values extracted from a set of entries in a
* Map. All the extracted Number objects will be treated as Java
* <tt>double</tt> values.
*
* @param <T>  the type of the value to extract from
*
* @author gg  2005.09.05
* @since Coherence 3.1
*/
public class DoubleMax<T>
        extends AbstractDoubleAggregator<T>
        implements ContinuousAggregationBound
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public DoubleMax()
        {
        super();
        }

    /**
    * Construct a DoubleMax aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public DoubleMax(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct a DoubleMax aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public DoubleMax(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, Double> supply()
        {
        return new DoubleMax<>(getValueExtractor());
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY | CONTINUOUS | STATE_CHECKPOINTABLE;
        }

    @Override
    public Object snapshotState()
        {
        ensureInitialized(false);
        return new Object[] {Integer.valueOf(m_count), Double.valueOf(m_dflResult),
                Integer.valueOf(m_cSupport)};
        }

    @Override
    public void restoreState(Object state)
        {
        if (!(state instanceof Object[]) || ((Object[]) state).length != 3)
            {
            throw new IllegalArgumentException("invalid double maximum state snapshot");
            }
        Object[] aoState  = (Object[]) state;
        int      cValues  = ((Number) aoState[0]).intValue();
        int      cSupport = ((Number) aoState[2]).intValue();
        if (cValues < 0 || cSupport < 0 || cSupport > cValues)
            {
            throw new IllegalArgumentException("invalid double maximum state values");
            }
        ensureInitialized(false);
        m_count     = cValues;
        m_dflResult = ((Number) aoState[1]).doubleValue();
        m_cSupport  = cSupport;
        }

    @Override
    public RetractionResult getMaintenanceStatus()
        {
        return m_count == 0 || m_cSupport > 0
               ? RetractionResult.UPDATED
               : RetractionResult.STALE_BOUND;
        }

    @Override
    public Object getContinuousAggregationBound()
        {
        return Double.valueOf(m_dflResult);
        }

    @Override
    public int compareContinuousAggregationBounds(Object left, Object right)
        {
        return Double.compare(((Number) right).doubleValue(), ((Number) left).doubleValue());
        }

    @Override
    public boolean isContinuousAggregationBoundDominated(Object bound, Object exactPartial)
        {
        return Double.compare(((Number) exactPartial).doubleValue(),
                ((Number) bound).doubleValue()) >= 0;
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        super.init(fFinal);

        m_dflResult = -Double.MAX_VALUE;
        m_cSupport  = 0;
        }

    /**
    * {@inheritDoc}
    */
    protected void process(Object o, boolean fFinal)
        {
        if (o != null)
            {
            double dflValue  = ((Number) o).doubleValue();
            double dflResult = m_count == 0 ? dflValue : Math.max(m_dflResult, dflValue);
            if (m_count == 0 || Double.compare(dflResult, m_dflResult) != 0)
                {
                m_dflResult = dflResult;
                m_cSupport  = 1;
                }
            else if (Double.compare(dflValue, m_dflResult) == 0)
                {
                m_cSupport++;
                }
            m_count++;
            }
        }

    @Override
    protected RetractionResult remove(Object o)
        {
        if (o == null)
            {
            return getMaintenanceStatus();
            }
        if (m_count == 0)
            {
            return RetractionResult.REBUILD_REQUIRED;
            }

        double dflValue = ((Number) o).doubleValue();
        if (Double.compare(dflValue, m_dflResult) == 0 && m_cSupport > 0)
            {
            m_cSupport--;
            }
        if (--m_count == 0)
            {
            m_dflResult = -Double.MAX_VALUE;
            m_cSupport  = 0;
            }
        else if (m_cSupport == 0 && Double.isNaN(m_dflResult))
            {
            return RetractionResult.REBUILD_REQUIRED;
            }
        return getMaintenanceStatus();
        }

    // ----- data members ---------------------------------------------------

    /** The number of current entries equal to the retained maximum. */
    protected transient int m_cSupport;
    }
