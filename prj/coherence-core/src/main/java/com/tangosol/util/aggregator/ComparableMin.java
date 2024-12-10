/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;

import java.util.Comparator;


/**
* Calculates a minimum among values extracted from a set of entries in a Map.
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
public class ComparableMin<T, R>
        extends AbstractComparableAggregator<T, R>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ComparableMin()
        {
        super();
        }

    /**
    * Construct a ComparableMin aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any object that implements {@link Comparable}
    *                   interface
    */
    public <E extends Comparable<? super E>> ComparableMin(ValueExtractor<? super T, ? extends E> extractor)
        {
        super(extractor);
        }

    /**
    * Construct a ComparableMin aggregator.
    *
    * @param extractor  the extractor that provides an object to be compared
    * @param comparator the comparator used to compare the extracted object
    */
    public ComparableMin(ValueExtractor<? super T, ? extends R> extractor, Comparator<? super R> comparator)
        {
        super(extractor, comparator);
        }

    /**
    * Construct a ComparableMin aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any object that implements {@link Comparable}
    *                 interface
    */
    public ComparableMin(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, R> supply()
        {
        return new ComparableMin<>(getValueExtractor(), m_comparator);
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY;
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
            if (oResult == null ||
                SafeComparator.compareSafe(m_comparator, oResult, o) > 0)
                {
                m_oResult = (R) o;
                }
            m_count++;
            }
        }
    }