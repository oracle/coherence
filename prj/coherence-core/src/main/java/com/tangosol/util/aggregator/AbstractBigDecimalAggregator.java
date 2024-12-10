/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;

import com.tangosol.internal.util.aggregator.BigDecimalSerializationWrapper;

import com.tangosol.util.ValueExtractor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import java.util.Objects;

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
* @author gg              2006.02.13
* @author Gunnar Hillert  2022.06.01
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
            if (this.getScale() != null)
                {
                if (this.getRoundingMode() != null)
                    {
                    m_decResult = m_decResult.setScale(this.getScale(), this.getRoundingMode());
                    }
                else
                    {
                    m_decResult = m_decResult.setScale(this.getScale());
                    }
                }

            if (this.isStripTrailingZeros())
                {
                m_decResult = m_decResult.stripTrailingZeros();
                }
            return m_decResult;
            }
        else
            {
            return new BigDecimalSerializationWrapper(m_decResult);
            }
        }

    // ----- Object methods -------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof AbstractBigDecimalAggregator<?>))
            {
            return false;
            }
        if (!super.equals(o))
            {
            return false;
            }
        AbstractBigDecimalAggregator<?> that = (AbstractBigDecimalAggregator<?>) o;
        return isStripTrailingZeros() == that.isStripTrailingZeros()
               && Objects.equals(getScale(), that.getScale())
               && Objects.equals(getMathContext(), that.getMathContext())
               && getRoundingMode() == that.getRoundingMode();
        }

    public int hashCode()
        {
        return Objects.hash(super.hashCode(), getScale(), getMathContext(),
                getRoundingMode(), isStripTrailingZeros());
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

    /**
     * The scale used for the aggregated calculation. Is null by default, in which case the defaults of the underlying
     * {@link BigDecimal} are being used.
     */
    protected Integer m_scale;

    /**
     * The {@link MathContext} to provide the precision. Is null by default, in which case the defaults of the underlying
     * {@link BigDecimal} are being used.
     */
    protected MathContext m_mathContext;

    /**
     * The {@link RoundingMode} used for the aggregated calculation. Is null by default, in which case the defaults of
     * the underlying {@link BigDecimal} are being used.
     */
    protected RoundingMode m_roundingMode;

    /**
     * Shall trailing zeros be removed from the aggregation result? Defaults to {@code false}.
     */
    protected boolean m_fStripTrailingZeros;

    /**
     * Returns the specified scale. Can be null.
     * @return the scale to return. Can be null.
     */
    public Integer getScale()
        {
        return m_scale;
        }

    /**
     * Specifies the scale to be applied to the aggregated result. Typically, scale is set together with
     * {@link #setRoundingMode(RoundingMode)}. However, if the specified scaling operation would require rounding then
     * a ArithmeticException will be thrown. If {@link #setMathContext(MathContext)} is specified and the operation
     * supports the {@link MathContext} then the {@link #setScale(Integer)} property is ignored.
     * @param scale the scale to set.
     */
    public void setScale(Integer scale)
        {
        m_scale = scale;
        }

    /**
     * Returns the specified {@link MathContext} or null.
     * @return the MathContext. Can be null.
     */
    public MathContext getMathContext()
        {
        return m_mathContext;
        }

    /**
     * Sets the MathContext (allowing you to work with precision instead of scale).
     * If a {@link BigDecimal} operation supports both {@link MathContext} or scale and both properties are specified,
     * then the {@link MathContext} is used and the scale is ignored.
     * @param mathContext the MathContext to set.
     */
    public void setMathContext(MathContext mathContext)
        {
        m_mathContext = mathContext;
        }

    /**
     * Returns the {@link RoundingMode} that is applied to aggregation results.
     * @return The RoundingMode. Can be null.
     */
    public RoundingMode getRoundingMode()
        {
        return m_roundingMode;
        }

    /**
     * Sets the {@link RoundingMode} for the results, e.g. if scale is applied to the aggregation result.
     * @param roundingMode the RoundingMode to set. Can be null.
     */
    public void setRoundingMode(RoundingMode roundingMode)
        {
        m_roundingMode = roundingMode;
        }

    /**
     * Shall trailing zeros be removed from the aggregation result?
     * @return true if trailing zeros are removed.
     */
    public boolean isStripTrailingZeros()
        {
        return m_fStripTrailingZeros;
        }

    /**
     * Allows you to set the property to {@code true} to remove trailing zeros from the aggregation result.
     * @param fStripTrailingZeros Defaults to false if not set.
     */
    public void setStripTrailingZeros(boolean fStripTrailingZeros)
        {
        m_fStripTrailingZeros = fStripTrailingZeros;
        }
    }
