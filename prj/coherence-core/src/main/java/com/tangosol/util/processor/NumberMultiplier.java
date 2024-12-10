/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.processor;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueManipulator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.bind.annotation.JsonbProperty;


/**
* The NumberMultiplier entry processor is used to multiply a property value
* of a {@link Number} type.  Supported types are: Byte, Short, Integer, Long,
* Float, Double, BigInteger and BigDecimal.
*
* @author gg  2005.10.31
* @since Coherence 3.1
*/
public class NumberMultiplier<K, V, N extends Number>
        extends PropertyProcessor<K, V, N>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public NumberMultiplier()
        {
        }

    /**
    * Construct an NumberMultiplier processor that will multiply a property
    * value by a specified factor, returning either the old or the new value
    * as specified.  The Java type of the original property value will
    * dictate the way the specified factor is interpreted. For example,
    * applying a factor of Double(0.5) to a property value of Integer(4) will
    * result in a new property value of Integer(2).
    * <br>
    * If the original property value is null, the Java type of the numFactor
    * parameter will dictate the Java type of the new value.
    *
    * @param sName        the property name or null if the target object is
    *                      an instance of a {@link Number}
    * @param numFactor    the Number representing the magnitude and sign of
    *                      the multiplier
    * @param fPostFactor  pass true to return the value as it was before
    *                      it was multiplied, or pass false to return the
    *                      value as it is after it is multiplied
    */
    public NumberMultiplier(String sName, N numFactor, boolean fPostFactor)
        {
        super(sName);

        azzert(numFactor != null);
        m_numFactor   = numFactor;
        m_fPostFactor = fPostFactor;
        }

    /**
    * Construct an NumberMultiplier processor that will multiply a property
    * value by a specified factor, returning either the old or the new value
    * as specified.  The Java type of the original property value will
    * dictate the way the specified factor is interpreted. For example,
    * applying a factor of Double(0.5) to a property value of Integer(4) will
    * result in a new property value of Integer(2).
    * <br>
    * If the original property value is null, the Java type of the numFactor
    * parameter will dictate the Java type of the new value.
    *
    * @param manipulator  the ValueManipulator; could be null if the target
    *                      object is an instance of a {@link Number}
    * @param numFactor    the Number representing the magnitude and sign of
    *                      the multiplier
    * @param fPostFactor  pass true to return the value as it was before
    *                      it was multiplied, or pass false to return the
    *                      value as it is after it is multiplied
    */
    public NumberMultiplier(ValueManipulator<V, N> manipulator,
                            N numFactor, boolean fPostFactor)
        {
        super(manipulator);

        azzert(numFactor != null);
        m_numFactor   = numFactor;
        m_fPostFactor = fPostFactor;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public N process(InvocableMap.Entry<K, V> entry)
        {
        if (!entry.isPresent())
            {
            return null;
            }

        Number numFactor = m_numFactor;

        Number numOld = get(entry);
        if (numOld == null)
            {
            numOld = numFactor instanceof Integer    ? NumberIncrementor.INTEGER_ZERO
                   : numFactor instanceof Long       ? NumberIncrementor.LONG_ZERO
                   : numFactor instanceof Double     ? NumberIncrementor.DOUBLE_ZERO
                   : numFactor instanceof Float      ? NumberIncrementor.FLOAT_ZERO
                   : numFactor instanceof BigInteger ? NumberIncrementor.BIGINTEGER_ZERO
                   : numFactor instanceof BigDecimal ? NumberIncrementor.BIGDECIMAL_ZERO
                   : numFactor instanceof Short      ? NumberIncrementor.SHORT_ZERO
                   : numFactor instanceof Byte       ? NumberIncrementor.BYTE_ZERO
                   : null;
            }

        Number numNew;
        if (numOld instanceof Integer)
            {
            int iNew = numOld.intValue();
            if (numFactor instanceof Double || numFactor instanceof Float)
                {
                iNew = (int) (iNew * numFactor.doubleValue());
                }
            else
                {
                iNew *= numFactor.intValue();
                }
            numNew = iNew;
            }
        else if (numOld instanceof Long)
            {
            long lNew = numOld.longValue();
            if (numFactor instanceof Double || numFactor instanceof Float)
                {
                lNew = (long) (lNew * numFactor.doubleValue());
                }
            else
                {
                lNew *= numFactor.longValue();
                }
            numNew = lNew;
            }
        else if (numOld instanceof Double)
            {
            numNew = numOld.doubleValue() * numFactor.doubleValue();
            }
        else if (numOld instanceof Float)
            {
            numNew = numOld.floatValue() * numFactor.floatValue();
            }
        else if (numOld instanceof BigInteger)
            {
            numNew = ((BigInteger) numOld).multiply((BigInteger) numFactor);
            }
        else if (numOld instanceof BigDecimal)
            {
            numNew = ((BigDecimal) numOld).multiply((BigDecimal) numFactor);
            }
        else if (numOld instanceof Short)
            {
            short iNew = numOld.shortValue();
            if (numFactor instanceof Double || numFactor instanceof Float)
                {
                iNew = (short) (iNew * numFactor.doubleValue());
                }
            else
                {
                iNew *= numFactor.shortValue();
                }
            numNew = iNew;
            }
        else if (numOld instanceof Byte)
            {
            byte bNew = numOld.byteValue();
            if (numFactor instanceof Double || numFactor instanceof Float)
                {
                bNew = (byte) (bNew * numFactor.doubleValue());
                }
            else
                {
                bNew *= numFactor.byteValue();
                }
            numNew = bNew;
            }
        else
            {
            throw new RuntimeException("Unsupported type:" + (numOld == null ?
                numFactor.getClass().getName() : numOld.getClass().getName()));
            }

        if (!numNew.equals(numOld))
            {
            set(entry, (N) numNew);
            }

        return (N) (m_fPostFactor ? numOld : numNew);
        }


    // ----- helpers --------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected String getDescription()
        {
        return (m_fPostFactor ? ", post" : ", pre") + "-factor=" + m_numFactor;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_numFactor   = (N) ExternalizableHelper.readObject(in);
        m_fPostFactor = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        ExternalizableHelper.writeObject(out, m_numFactor);
        out.writeBoolean(m_fPostFactor);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_numFactor   = (N) in.readObject(1);
        m_fPostFactor = in.readBoolean(2);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeObject(1, m_numFactor);
        out.writeBoolean(2, m_fPostFactor);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the number to multiply by.
     * @return the number to multiply by
     */
    public N getNumFactor()
        {
        return m_numFactor;
        }

    /**
     * Returns whether to return the value before it was multiplied.
     *
     * @return whether to return the value before it was multiplied
     */
    public boolean getPostFactor()
        {
        return m_fPostFactor;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The number to multiply by.
    */
    @JsonbProperty("multiplier")
    private N m_numFactor;

    /**
    * Whether to return the value before it was multiplied ("post-factor") or
    * after it is multiplied ("pre-factor").
    */
    @JsonbProperty("postMultiplication")
    private boolean m_fPostFactor;
    }
