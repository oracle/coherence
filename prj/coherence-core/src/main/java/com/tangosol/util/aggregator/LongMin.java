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
* Calculates a minimum of numeric values extracted from a set of entries in a
* Map. All the extracted Number objects will be treated as Java <tt>long</tt>
* values.
*
* @param <T>  the type of the value to extract from
*
* @author gg  2005.09.05
* @since Coherence 3.1
*/
public class LongMin<T>
        extends AbstractLongAggregator<T>
        implements ContinuousAggregationBound
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public LongMin()
        {
        super();
        }

    /**
    * Construct a LongMin aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public LongMin(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct a LongMin aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public LongMin(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, Long> supply()
        {
        return new LongMin<>(getValueExtractor());
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
        return new Object[] {Integer.valueOf(m_count), Long.valueOf(m_lResult),
                Integer.valueOf(m_cSupport)};
        }

    @Override
    public void restoreState(Object state)
        {
        if (!(state instanceof Object[]) || ((Object[]) state).length != 3)
            {
            throw new IllegalArgumentException("invalid long minimum state snapshot");
            }
        Object[] aoState  = (Object[]) state;
        int      cValues  = ((Number) aoState[0]).intValue();
        int      cSupport = ((Number) aoState[2]).intValue();
        if (cValues < 0 || cSupport < 0 || cSupport > cValues)
            {
            throw new IllegalArgumentException("invalid long minimum state values");
            }
        ensureInitialized(false);
        m_count    = cValues;
        m_lResult  = ((Number) aoState[1]).longValue();
        m_cSupport = cSupport;
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
        return Long.valueOf(m_lResult);
        }

    @Override
    public int compareContinuousAggregationBounds(Object left, Object right)
        {
        return Long.compare(((Number) left).longValue(), ((Number) right).longValue());
        }

    @Override
    public boolean isContinuousAggregationBoundDominated(Object bound, Object exactPartial)
        {
        return ((Number) exactPartial).longValue() <= ((Number) bound).longValue();
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        super.init(fFinal);

        m_lResult = Long.MAX_VALUE;
        m_cSupport = 0;
        }

    /**
    * {@inheritDoc}
    */
    protected void process(Object o, boolean fFinal)
        {
        if (o != null)
            {
            long lValue = ((Number) o).longValue();
            if (lValue < m_lResult)
                {
                m_lResult  = lValue;
                m_cSupport = 1;
                }
            else if (lValue == m_lResult)
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

        if (((Number) o).longValue() == m_lResult && m_cSupport > 0)
            {
            m_cSupport--;
            }
        if (--m_count == 0)
            {
            m_lResult  = Long.MAX_VALUE;
            m_cSupport = 0;
            }
        return getMaintenanceStatus();
        }

    // ----- data members ---------------------------------------------------

    /** The number of current entries equal to the retained minimum. */
    protected transient int m_cSupport;
    }
