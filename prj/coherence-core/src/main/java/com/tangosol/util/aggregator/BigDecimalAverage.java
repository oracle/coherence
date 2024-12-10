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
import java.math.RoundingMode;


/**
* Calculates an average for values of any numeric type extracted from a set
* of entries in a Map in a form of a {@link java.math.BigDecimal} value.  All
* the extracted objects will be treated as {@link java.math.BigDecimal},
* {@link java.math.BigInteger} or Java <tt>double</tt> values.
* If the set of entries is empty, a <tt>null</tt> result is returned.
*
* @param <T>  the type of the value to extract from
*
* @author gg  2006.07.18
* @since Coherence 3.2
*/
public class BigDecimalAverage<T>
        extends AbstractBigDecimalAggregator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public BigDecimalAverage()
        {
        super();
        }

    /**
    * Construct a BigDecimalAverage aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public BigDecimalAverage(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct an BigDecimalAverage object.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public BigDecimalAverage(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, BigDecimal> supply()
        {
        BigDecimalAverage bigDecimalAverage = new BigDecimalAverage<>(getValueExtractor());
        bigDecimalAverage.setScale(this.getScale());
        bigDecimalAverage.setMathContext(this.getMathContext());
        bigDecimalAverage.setRoundingMode(this.getRoundingMode());
        bigDecimalAverage.setStripTrailingZeros(this.isStripTrailingZeros());
        return bigDecimalAverage;
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
            BigDecimal decResult = m_decResult;

            if (fFinal)
                {
                BigDecimalSerializationWrapper wrapper = (BigDecimalSerializationWrapper) o;
                int c = wrapper.getCount();
                if (c > 0)
                    {
                    m_count += c;
                    BigDecimal partialResult = wrapper.getBigDecimal();
                    m_decResult = decResult == null ? partialResult : decResult.add(partialResult);
                    }
                }
            else
                {
                BigDecimal dec = ensureBigDecimal((Number) o);

                // collect partial results
                m_count++;
                m_decResult = decResult == null ? dec : decResult.add(dec);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    protected Object finalizeResult(boolean fFinal)
        {
        if (m_count == 0)
            {
                return null;
            }
        if (fFinal)
            {
            if (this.m_mathContext != null)
                {
                m_decResult = m_decResult
                        .divide(BigDecimal.valueOf(m_count), m_mathContext);
                }
            else if (this.m_roundingMode != null && this.m_scale != null)
                {
                m_decResult = m_decResult
                        .divide(BigDecimal.valueOf(m_count), m_scale, m_roundingMode);
                }
            else if (this.m_roundingMode != null && this.m_scale == null)
                {
                m_decResult = m_decResult
                        .divide(BigDecimal.valueOf(m_count), m_roundingMode);
                }
            else if (m_roundingMode == null && m_scale != null)
                {
                throw new IllegalArgumentException("If scale is specified, the rounding mode must be specified as well");
                }
            else
                {
                m_decResult = m_decResult.divide(BigDecimal.valueOf(m_count),
                        m_decResult.scale() + 8, // 'cause Cam said so
                        RoundingMode.HALF_UP);
                }

            if (isStripTrailingZeros())
                {
                m_decResult = m_decResult.stripTrailingZeros();
                }
            // return the final aggregated result
            return m_decResult;
            }
        else
            {
            // return partial aggregation data wrapped in SerializationWrapper to ensure
            // there's enough context for all serialization formats to function as expected.
            return new BigDecimalSerializationWrapper(m_count, m_decResult);
            }
        }
    }