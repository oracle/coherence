/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.internal.util.aggregator.BigDecimalSerializationWrapper;

import com.tangosol.util.ValueExtractor;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
* Abstract aggregator that processes {@link Number} values extracted from
* a set of entries in a Map and returns a result in a form of a
* {@link java.math.BigDecimal} value. All the extracted objects will be
* treated as {@link java.math.BigDecimal}, {@link java.math.BigInteger} or
* Java <tt>double</tt> values.
* If the set of entries is empty, a <tt>null</tt> result is returned.
*
* @param <T>  the type of the value to extract from
*
* @author gg  2006.02.13
* @since Coherence 3.2
*/
public abstract class AbstractBigDecimalAggregator<T>
        extends AbstractAggregator<Object, Object, T, Number, BigDecimal>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public AbstractBigDecimalAggregator()
        {
        super();
        }

    /**
    * Construct an AbstractBigDecimalAggregator object.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public AbstractBigDecimalAggregator(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct an AbstractBigDecimalAggregator object.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                  of any Java object that is a {@link Number}
    */
    public AbstractBigDecimalAggregator(String sMethod)
        {
        super(sMethod);
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        m_count     = 0;
        m_decResult = null;
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
            return m_decResult;
            }
        else
            {
            return new BigDecimalSerializationWrapper(m_decResult);
            }
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Ensure the specified Number is a BigDecimal value or convert it into a
    * new BigDecimal object.
    *
    * @param num  a Number object
    *
    * @return a BigDecimal object that is equal to the passed in Number
    */
    public static BigDecimal ensureBigDecimal(Number num)
        {
        return num instanceof BigDecimal ? (BigDecimal) num :
               num instanceof BigInteger ? new BigDecimal((BigInteger) num) :
                                           new BigDecimal(num.doubleValue());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The count of processed entries.
    */
    protected transient int m_count;

    /**
    * The running result value.
    */
    protected transient BigDecimal m_decResult;
    }