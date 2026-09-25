/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.internal.net.ContinuousAggregationBound;

import com.tangosol.internal.util.aggregator.BigDecimalSerializationWrapper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import java.math.BigDecimal;


/**
* Calculates a maximum of numeric values extracted from a set of entries in a
* Map in a form of a {@link java.math.BigDecimal} value. All the extracted
* objects will be treated as {@link java.math.BigDecimal},
* {@link java.math.BigInteger} or Java <tt>double</tt> values.
* If the set of entries is empty, a <tt>null</tt> result is returned.
*
* @param <T>  the type of the value to extract from
*
* @author gg  2006.07.18
* @since Coherence 3.2
*/
public class BigDecimalMax<T>
        extends AbstractBigDecimalAggregator<T>
        implements ContinuousAggregationBound
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public BigDecimalMax()
        {
        super();
        }

    /**
    * Construct a BigDecimalMax aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public BigDecimalMax(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct a BigDecimalMax aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public BigDecimalMax(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, BigDecimal> supply()
        {
        BigDecimalMax bigDecimalMax = new BigDecimalMax(getValueExtractor());
        bigDecimalMax.setScale(this.getScale());
        bigDecimalMax.setRoundingMode(this.getRoundingMode());
        bigDecimalMax.setMathContext(this.getMathContext());
        bigDecimalMax.setStripTrailingZeros(this.isStripTrailingZeros());
        return bigDecimalMax;
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
        return new Object[] {Integer.valueOf(m_count), m_decResult,
                Integer.valueOf(m_cSupport)};
        }

    @Override
    public void restoreState(Object state)
        {
        if (!(state instanceof Object[]) || ((Object[]) state).length != 3)
            {
            throw new IllegalArgumentException("invalid BigDecimal maximum state snapshot");
            }
        Object[] aoState  = (Object[]) state;
        int      cValues  = ((Number) aoState[0]).intValue();
        int      cSupport = ((Number) aoState[2]).intValue();
        if (cValues < 0 || cSupport < 0 || cSupport > cValues
                || cValues > 0 && !(aoState[1] instanceof BigDecimal))
            {
            throw new IllegalArgumentException("invalid BigDecimal maximum state values");
            }
        ensureInitialized(false);
        m_count     = cValues;
        m_decResult = (BigDecimal) aoState[1];
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
        return new BigDecimalSerializationWrapper(m_decResult);
        }

    @Override
    public int compareContinuousAggregationBounds(Object left, Object right)
        {
        return asBigDecimal(right).compareTo(asBigDecimal(left));
        }

    @Override
    public boolean isContinuousAggregationBoundDominated(Object bound, Object exactPartial)
        {
        return asBigDecimal(exactPartial).compareTo(asBigDecimal(bound)) >= 0;
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void process(Object o, boolean fFinal)
        {
        if (o != null)
            {
            BigDecimal dec;
            if (fFinal)
                {
                dec = ((BigDecimalSerializationWrapper) o).getBigDecimal();
                }
            else
                {
                dec = ensureBigDecimal((Number) o);
                }

            BigDecimal decResult = m_decResult;
            if (decResult == null || decResult.compareTo(dec) < 0)
                {
                m_decResult = dec;
                m_cSupport  = 1;
                }
            else if (decResult.compareTo(dec) == 0)
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

        BigDecimal dec = ensureBigDecimal((Number) o);
        if (m_decResult.compareTo(dec) == 0 && m_cSupport > 0)
            {
            m_cSupport--;
            }
        if (--m_count == 0)
            {
            m_decResult = null;
            m_cSupport  = 0;
            }
        return getMaintenanceStatus();
        }

    /**
     * Convert a partial result or value to a BigDecimal.
     */
    private static BigDecimal asBigDecimal(Object value)
        {
        return value instanceof BigDecimalSerializationWrapper
               ? ((BigDecimalSerializationWrapper) value).getBigDecimal()
               : ensureBigDecimal((Number) value);
        }

    // ----- data members ---------------------------------------------------

    /** The number of current entries equal to the retained maximum. */
    protected transient int m_cSupport;
    }
