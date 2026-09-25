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

import com.tangosol.util.comparator.SafeComparator;

import java.util.Comparator;

/**
* Calculates a maximum among values extracted from a set of entries in a Map.
* This aggregator is most commonly used with objects that implement
* {@link Comparable} such as {@link String String} or
* {@link java.util.Date Date}; a {@link Comparator} can also be supplied to
* perform the comparisons.
*
* @param <T>  the type of the value to extract from
* @param <R>  the type of the result
*
* @author gg  2006.02.13
* @since Coherence 3.2
*/
public class ComparableMax<T, R>
        extends AbstractComparableAggregator<T, R>
        implements ContinuousAggregationBound
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ComparableMax()
        {
        super();
        }

    /**
    * Construct a ComparableMax aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any object that implements {@link Comparable}
    *                   interface
    */
    public <E extends Comparable<? super E>> ComparableMax(ValueExtractor<? super T, ? extends E> extractor)
        {
        super(extractor);
        }

    /**
    * Construct a ComparableMax aggregator.
    *
    * @param extractor  the extractor that provides an object to be compared
    * @param comparator the comparator used to compare the extracted object
    */
    public ComparableMax(ValueExtractor<? super T, ? extends R> extractor,
                         Comparator<? super R> comparator)
        {
        super(extractor, comparator);
        }

    /**
    * Construct a ComparableMax aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any object that implements {@link Comparable}
    *                 interface
    */
    public ComparableMax(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, R> supply()
        {
        return new ComparableMax<>(getValueExtractor(), m_comparator);
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY | CONTINUOUS | STATE_CHECKPOINTABLE;
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
        return m_oResult;
        }

    @Override
    public int compareContinuousAggregationBounds(Object left, Object right)
        {
        return SafeComparator.compareSafe(m_comparator, right, left);
        }

    @Override
    public boolean isContinuousAggregationBoundDominated(Object bound, Object exactPartial)
        {
        return SafeComparator.compareSafe(m_comparator, exactPartial, bound) >= 0;
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void process(Object o, boolean fFinal)
        {
        if (o != null)
            {
            R oResult = m_oResult;
            int nCompare = oResult == null ? -1
                    : SafeComparator.compareSafe(m_comparator, oResult, o);
            if (nCompare < 0)
                {
                m_oResult = (R) o;
                m_cSupport = 1;
                }
            else if (nCompare == 0)
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

        if (SafeComparator.compareSafe(m_comparator, m_oResult, o) == 0
                && m_cSupport > 0)
            {
            m_cSupport--;
            }

        if (--m_count == 0)
            {
            m_oResult  = null;
            m_cSupport = 0;
            }
        return getMaintenanceStatus();
        }
    }
