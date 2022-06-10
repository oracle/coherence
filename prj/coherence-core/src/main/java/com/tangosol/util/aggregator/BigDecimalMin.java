/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.internal.util.aggregator.BigDecimalSerializationWrapper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import java.math.BigDecimal;


/**
* Calculates a minimum of numeric values extracted from a set of entries in a
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
public class BigDecimalMin<T>
        extends AbstractBigDecimalAggregator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public BigDecimalMin()
        {
        super();
        }

    /**
    * Construct a BigDecimalMin aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public BigDecimalMin(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct a BigDecimalMin aggregator.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public BigDecimalMin(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, BigDecimal> supply()
        {
        BigDecimalMin bigDecimalMin = new BigDecimalMin(getValueExtractor());
        bigDecimalMin.setScale(this.getScale());
        bigDecimalMin.setRoundingMode(this.getRoundingMode());
        bigDecimalMin.setMathContext(this.getMathContext());
        bigDecimalMin.setStripTrailingZeros(this.isStripTrailingZeros());
        return bigDecimalMin;
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

            m_decResult = decResult == null ? dec : decResult.min(dec);
            m_count++;
            }
        }
    }