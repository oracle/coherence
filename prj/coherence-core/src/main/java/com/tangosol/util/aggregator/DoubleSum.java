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
        return PARALLEL | PRESENT_ONLY;
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
    }