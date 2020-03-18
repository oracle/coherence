/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


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
        return new BigDecimalMax<>(getValueExtractor());
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

            m_decResult = decResult == null ? dec : decResult.max(dec);
            m_count++;
            }
        }
    }