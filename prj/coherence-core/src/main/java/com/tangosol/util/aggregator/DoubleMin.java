/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;


/**
* Calculates a minimum of numeric values extracted from a set of entries in
* a Map. All the extracted Number objects will be treated as Java
* <tt>double</tt> values.
*
* @param <T>  the type of the value to extract from
*
* @author gg  2005.09.05
* @since Coherence 3.1
*/
public class DoubleMin<T>
        extends AbstractDoubleAggregator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public DoubleMin()
        {
        super();
        }

    /**
    * Construct a DoubleMin aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public DoubleMin(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct a DoubleMin aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public DoubleMin(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, Double> supply()
        {
        return new DoubleMin<>(getValueExtractor());
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
    protected void init(boolean fFinal)
        {
        super.init(fFinal);

        m_dflResult = Double.MAX_VALUE;
        }

    /**
    * {@inheritDoc}
    */
    protected void process(Object o, boolean fFinal)
        {
        if (o != null)
            {
            m_dflResult = Math.min(m_dflResult, ((Number) o).doubleValue());
            m_count++;
            }
        }
    }